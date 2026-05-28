package com.example.server;

import com.example.server.auth.entity.Role;
import com.example.server.game.entity.Game;
import com.example.server.game.repository.GameRepository;
import com.example.server.game.repository.GameTaskRepository;
import com.example.server.game.repository.GameTaskHintRepository;
import com.example.server.game.repository.GameTeamSessionRepository;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GameControllerTest extends BaseIntegrationTest {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameTaskRepository gameTaskRepository;

    @Autowired
    private GameTaskHintRepository gameTaskHintRepository;

    @Autowired
    private GameTeamSessionRepository gameTeamSessionRepository;

    // --- Вспомогательный метод: создаёт игру и возвращает её id ---
    private Long createGame(String organizerToken) throws Exception {
        String future = Instant.now().plus(7, ChronoUnit.DAYS).toString();
        String body = """
            {
              "title": "Тестовая Игра",
              "description": "Описание",
              "city": "Казань",
              "minTeamSize": 1,
              "maxTeamSize": 6,
              "taskFailurePenaltyMinutes": 5,
              "routeSlotsCount": 1,
              "startsAt": "%s"
            }
            """.formatted(future);

        String response = mockMvc.perform(post("/api/games")
                        .header("Authorization", bearerHeader(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    // --- Вспомогательный метод: создаёт задание и возвращает его id ---
    private Long createTask(String organizerToken, Long gameId, String answerKey) throws Exception {
        String body = """
            {
              "title": "Задание 1",
              "riddleText": "Текст загадки",
              "answerKey": "%s",
              "orderIndex": 1,
              "timeLimitMinutes": 30,
              "failurePenaltyMinutes": 5
            }
            """.formatted(answerKey);

        String response = mockMvc.perform(post("/api/games/{gameId}/my/{gameId}/tasks"
                        .replace("{gameId}/my/{gameId}", gameId + "/my/" + gameId), gameId)
                        .header("Authorization", bearerHeader(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    // TC-08-01: Создание игры организатором
    @Test
    void TC_08_01() throws Exception {
        String orgToken = registerAndLogin("organizer@test.com", "password123", Role.ORGANIZER);
        String future = Instant.now().plus(7, ChronoUnit.DAYS).toString();

        String response = mockMvc.perform(post("/api/games")
                        .header("Authorization", bearerHeader(orgToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "title": "Ночной Дозор 2025",
                      "description": "Описание игры",
                      "city": "Казань",
                      "minTeamSize": 2,
                      "maxTeamSize": 6,
                      "taskFailurePenaltyMinutes": 5,
                      "routeSlotsCount": 2,
                      "startsAt": "%s"
                    }
                    """.formatted(future)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Ночной Дозор 2025"))
                .andExpect(jsonPath("$.city").value("Казань"))
                .andReturn().getResponse().getContentAsString();

        Long gameId = objectMapper.readTree(response).get("id").asLong();

        // Проверка БД
        assertTrue(gameRepository.findById(gameId).isPresent(),
                "Игра должна быть сохранена в БД");
    }

    // TC-08-02: Добавление задания к игре
    @Test
    void TC_08_02() throws Exception {
        String orgToken = registerAndLogin("org2@test.com", "password123", Role.ORGANIZER);
        Long gameId = createGame(orgToken);

        String taskResponse = mockMvc.perform(post("/api/games/my/{gameId}/tasks", gameId)
                        .header("Authorization", bearerHeader(orgToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "title": "Задание 1",
                      "riddleText": "Текст загадки",
                      "answerKey": "секретный-ответ",
                      "orderIndex": 1,
                      "timeLimitMinutes": 30,
                      "failurePenaltyMinutes": 5
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Задание 1"))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

        // Проверка БД
        assertTrue(gameTaskRepository.findById(taskId).isPresent(),
                "Задание должно быть сохранено в БД");

        // Проверка GET /api/games/my/{gameId}/tasks
        mockMvc.perform(get("/api/games/my/{gameId}/tasks", gameId)
                        .header("Authorization", bearerHeader(orgToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Задание 1"));
    }

    // TC-08-03: Добавление подсказки к заданию
    @Test
    void TC_08_03() throws Exception {
        String orgToken = registerAndLogin("org3@test.com", "password123", Role.ORGANIZER);
        Long gameId = createGame(orgToken);

        // Создаём задание
        String taskResponse = mockMvc.perform(post("/api/games/my/{gameId}/tasks", gameId)
                        .header("Authorization", bearerHeader(orgToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "title": "Задание с подсказкой",
                      "riddleText": "Загадка",
                      "answerKey": "ответ",
                      "orderIndex": 1,
                      "timeLimitMinutes": 30,
                      "failurePenaltyMinutes": 5
                    }
                    """))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

        // Добавляем подсказку
        String hintResponse = mockMvc.perform(
                        post("/api/games/my/{gameId}/tasks/{taskId}/hints", gameId, taskId)
                                .header("Authorization", bearerHeader(orgToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                    {
                      "text": "Подсказка: смотри на крышу",
                      "orderIndex": 1,
                      "delayMinutesFromPreviousHint": 5
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Подсказка: смотри на крышу"))
                .andReturn().getResponse().getContentAsString();

        Long hintId = objectMapper.readTree(hintResponse).get("id").asLong();

        // Проверка БД
        assertTrue(gameTaskHintRepository.findById(hintId).isPresent(),
                "Подсказка должна быть сохранена в БД");
    }

    // TC-09-01: Отправка правильного ответа
    @Test
    void TC_09_01() throws Exception {
        String orgToken = registerAndLogin("org4@test.com", "password123", Role.ORGANIZER);
        Long gameId = createGame(orgToken);

        // Создаём задание с известным ответом
        String taskResponse = mockMvc.perform(post("/api/games/my/{gameId}/tasks", gameId)
                        .header("Authorization", bearerHeader(orgToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "title": "Задание А",
                      "riddleText": "Загадка А",
                      "answerKey": "правильный-ответ",
                      "orderIndex": 1,
                      "timeLimitMinutes": 30,
                      "failurePenaltyMinutes": 5
                    }
                    """))
                .andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

        // Создаём маршрут для команды
        String captainToken = registerAndLogin("captain4@test.com", "password123", Role.PARTICIPANT);
        String teamJson = mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearerHeader(captainToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Команда Г\",\"city\":\"Казань\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long teamId = objectMapper.readTree(teamJson).get("id").asLong();

        // Организатор создаёт маршрут для этой команды
        String routeJson = mockMvc.perform(post("/api/games/my/{gameId}/routes", gameId)
                        .header("Authorization", bearerHeader(orgToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamId\":%d,\"name\":\"Маршрут 1\",\"slotNumber\":1}".formatted(teamId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long routeId = objectMapper.readTree(routeJson).get("id").asLong();

        // Добавляем задание в маршрут
        mockMvc.perform(post("/api/games/my/{gameId}/routes/{routeId}/items", gameId, routeId)
                .header("Authorization", bearerHeader(orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":%d,\"orderIndex\":1}".formatted(taskId)));

        // Команда подаёт заявку и организатор её подтверждает
        mockMvc.perform(post("/api/games/{gameId}/registrations", gameId)
                .header("Authorization", bearerHeader(captainToken)));

        String regsJson = mockMvc.perform(get("/api/games/my/{gameId}/registrations", gameId)
                        .header("Authorization", bearerHeader(orgToken)))
                .andReturn().getResponse().getContentAsString();
        Long regId = objectMapper.readTree(regsJson).get(0).get("registrationId").asLong();

        mockMvc.perform(post("/api/games/my/{gameId}/registrations/{regId}/approve",
                gameId, regId)
                .header("Authorization", bearerHeader(orgToken)));

        // Сдвигаем startsAt в прошлое, чтобы игра могла быть запущена
        Game game = gameRepository.findById(gameId).orElseThrow();
        game.setStartsAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        gameRepository.save(game);

        // Запускаем игру
        mockMvc.perform(post("/api/games/my/{gameId}/start", gameId)
                        .header("Authorization", bearerHeader(orgToken)))
                .andExpect(status().isOk());

        // Капитан отправляет правильный ответ
        mockMvc.perform(post("/api/games/current-task/submit-key")
                        .header("Authorization", bearerHeader(captainToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"правильный-ответ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTaskId").value(taskId))
                .andExpect(jsonPath("$.totalPenaltyMinutes").value(0));
    }

    // TC-09-02: Отправка неправильного ответа
    @Test
    void TC_09_02() throws Exception {
        String orgToken = registerAndLogin("org5@test.com", "password123", Role.ORGANIZER);
        Long gameId = createGame(orgToken);

        // Создаём задание
        String taskResponse = mockMvc.perform(post("/api/games/my/{gameId}/tasks", gameId)
                        .header("Authorization", bearerHeader(orgToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "title": "Задание Б",
                      "riddleText": "Загадка Б",
                      "answerKey": "правильный-ответ",
                      "orderIndex": 1,
                      "timeLimitMinutes": 30,
                      "failurePenaltyMinutes": 5
                    }
                    """))
                .andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

        // Создаём команду и маршрут
        String captainToken = registerAndLogin("captain5@test.com", "password123", Role.PARTICIPANT);
        String teamJson = mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearerHeader(captainToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Команда Д\",\"city\":\"Казань\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long teamId = objectMapper.readTree(teamJson).get("id").asLong();

        String routeJson = mockMvc.perform(post("/api/games/my/{gameId}/routes", gameId)
                        .header("Authorization", bearerHeader(orgToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamId\":%d,\"name\":\"Маршрут 1\",\"slotNumber\":1}".formatted(teamId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long routeId = objectMapper.readTree(routeJson).get("id").asLong();

        mockMvc.perform(post("/api/games/my/{gameId}/routes/{routeId}/items", gameId, routeId)
                .header("Authorization", bearerHeader(orgToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":%d,\"orderIndex\":1}".formatted(taskId)));

        // Регистрация и подтверждение
        mockMvc.perform(post("/api/games/{gameId}/registrations", gameId)
                .header("Authorization", bearerHeader(captainToken)));

        String regsJson = mockMvc.perform(get("/api/games/my/{gameId}/registrations", gameId)
                        .header("Authorization", bearerHeader(orgToken)))
                .andReturn().getResponse().getContentAsString();
        Long regId = objectMapper.readTree(regsJson).get(0).get("registrationId").asLong();

        mockMvc.perform(post("/api/games/my/{gameId}/registrations/{regId}/approve",
                gameId, regId)
                .header("Authorization", bearerHeader(orgToken)));

        // Сдвигаем startsAt в прошлое
        Game game = gameRepository.findById(gameId).orElseThrow();
        game.setStartsAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        gameRepository.save(game);

        mockMvc.perform(post("/api/games/my/{gameId}/start", gameId)
                        .header("Authorization", bearerHeader(orgToken)))
                .andExpect(status().isOk());

        // Капитан отправляет НЕВЕРНЫЙ ответ — ожидаем 400
        mockMvc.perform(post("/api/games/current-task/submit-key")
                        .header("Authorization", bearerHeader(captainToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"неверный-ответ\"}"))
                .andExpect(status().isBadRequest());

        // Проверяем БД — сессия всё ещё IN_PROGRESS, задание не сменилось
        var sessions = gameTeamSessionRepository.findAll().stream()
                .filter(s -> s.getTeam().getId().equals(teamId))
                .toList();
        assertFalse(sessions.isEmpty());
        assertEquals("IN_PROGRESS", sessions.get(0).getStatus().name());
        assertEquals(taskId, sessions.get(0).getCurrentTask().getId());
    }
}