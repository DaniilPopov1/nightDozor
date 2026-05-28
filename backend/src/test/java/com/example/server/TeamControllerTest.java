package com.example.server;

import com.example.server.auth.entity.Role;
import com.example.server.team.repository.TeamRepository;
import com.example.server.team.repository.TeamMembershipRepository;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TeamControllerTest extends BaseIntegrationTest {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    // TC-07-01: Создание команды и сохранение в БД
    @Test
    void TC_07_01() throws Exception {
        String token = registerAndLogin("captain@test.com", "password123", Role.PARTICIPANT);

        String response = mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearerHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"Команда А","city":"Казань"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Команда А"))
                .andExpect(jsonPath("$.city").value("Казань"))
                .andExpect(jsonPath("$.inviteCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Проверка БД
        assertTrue(teamRepository.findAll().stream()
                        .anyMatch(t -> t.getName().equals("Команда А")),
                "Команда должна быть сохранена в БД");

        Long teamId = objectMapper.readTree(response).get("id").asLong();
        var memberships = teamMembershipRepository.findAll().stream()
                .filter(m -> m.getTeam().getId().equals(teamId))
                .toList();
        assertFalse(memberships.isEmpty(), "Запись в team_memberships должна существовать");
        assertEquals("CAPTAIN", memberships.get(0).getRole().name());
        assertEquals("ACTIVE", memberships.get(0).getStatus().name());
    }

    // TC-07-02: GET /api/teams/me — получение текущей команды
    @Test
    void TC_07_02() throws Exception {
        String token = registerAndLogin("member@test.com", "password123", Role.PARTICIPANT);

        // Создаём команду
        mockMvc.perform(post("/api/teams")
                .header("Authorization", bearerHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Моя Команда\",\"city\":\"Москва\"}"));

        // Получаем текущую команду
        mockMvc.perform(get("/api/teams/me")
                        .header("Authorization", bearerHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Моя Команда"))
                .andExpect(jsonPath("$.city").value("Москва"))
                .andExpect(jsonPath("$.inviteCode").isNotEmpty())
                .andExpect(jsonPath("$.members").isArray());
    }

    // TC-07-03: Вступление по коду приглашения
    @Test
    void TC_07_03() throws Exception {
        // Капитан создаёт команду
        String captainToken = registerAndLogin("captain2@test.com", "password123", Role.PARTICIPANT);
        String teamJson = mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearerHeader(captainToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Команда Б\",\"city\":\"Казань\"}"))
                .andReturn().getResponse().getContentAsString();

        String inviteCode = objectMapper.readTree(teamJson).get("inviteCode").asText();

        // Второй пользователь вступает по коду
        String memberToken = registerAndLogin("joiner@test.com", "password123", Role.PARTICIPANT);
        mockMvc.perform(post("/api/teams/join-by-code")
                        .header("Authorization", bearerHeader(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"%s\"}".formatted(inviteCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Команда Б"));

        // Проверка БД
        var user = userRepository.findByEmail("joiner@test.com").orElseThrow();
        boolean isMember = teamMembershipRepository.findAll().stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId())
                        && m.getStatus().name().equals("ACTIVE"));
        assertTrue(isMember, "Пользователь должен быть активным участником команды");
    }

    // TC-07-04: Отправка заявки и подтверждение капитаном
    @Test
    void TC_07_04() throws Exception {
        // Капитан создаёт команду
        String captainToken = registerAndLogin("captain3@test.com", "password123", Role.PARTICIPANT);
        String teamJson = mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearerHeader(captainToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Команда В\",\"city\":\"Казань\"}"))
                .andReturn().getResponse().getContentAsString();

        Long teamId = objectMapper.readTree(teamJson).get("id").asLong();

        // Второй пользователь отправляет заявку
        String applicantToken = registerAndLogin("applicant@test.com", "password123", Role.PARTICIPANT);
        String requestJson = mockMvc.perform(post("/api/teams/{teamId}/join-requests", teamId)
                        .header("Authorization", bearerHeader(applicantToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Статус заявки — PENDING
        String status = objectMapper.readTree(requestJson).get("status").asText();
        assertEquals("PENDING", status);

        // Капитан принимает заявку
        Long applicantId = userRepository.findByEmail("applicant@test.com").orElseThrow().getId();
        mockMvc.perform(post("/api/teams/{teamId}/join-requests/{userId}/approve",
                        teamId, applicantId)
                        .header("Authorization", bearerHeader(captainToken)))
                .andExpect(status().isOk());

        // Проверка БД — участник добавлен со статусом ACTIVE
        boolean isActive = teamMembershipRepository.findAll().stream()
                .anyMatch(m -> m.getUser().getId().equals(applicantId)
                        && m.getStatus().name().equals("ACTIVE"));
        assertTrue(isActive, "После подтверждения участник должен иметь статус ACTIVE");
    }
}