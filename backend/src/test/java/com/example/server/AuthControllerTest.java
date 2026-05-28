package com.example.server;

import com.example.server.auth.entity.Role;
import com.example.server.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    // TC-06-01: Успешная регистрация и сохранение в БД
    @Test
    void TC_06_01() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"email":"newuser@test.com","password":"password123","role":"PARTICIPANT"}
                    """))
                .andExpect(status().isOk());

        var user = userRepository.findByEmail("newuser@test.com");
        assertTrue(user.isPresent(), "Пользователь должен быть сохранён в БД");
        assertEquals(Role.PARTICIPANT, user.get().getRole());
        assertNotEquals("password123", user.get().getPasswordHash(), "Пароль должен быть захэширован");
    }

    // TC-06-02: Регистрация с уже занятым email
    @Test
    void TC_06_02() throws Exception {
        // Предусловие: регистрируем первого пользователя
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"existing@test.com","password":"password123","role":"PARTICIPANT"}
                    """));

        // Повторная попытка с тем же email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"email":"existing@test.com","password":"newpassword","role":"PARTICIPANT"}
                    """))
                .andExpect(status().isConflict());

        // Проверка БД — запись одна
        long count = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals("existing@test.com"))
                .count();
        assertEquals(1, count, "В БД должна быть только одна запись с таким email");
    }

    // TC-06-03: Успешная авторизация и получение токена
    @Test
    void TC_06_03() throws Exception {
        // Предусловие: регистрируем и активируем пользователя
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"loginuser@test.com","password":"password123","role":"PARTICIPANT"}
                    """));

        var user = userRepository.findByEmail("loginuser@test.com").orElseThrow();
        user.setEnabled(true);
        userRepository.save(user);

        // Логин
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"email":"loginuser@test.com","password":"password123"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("PARTICIPANT"));
    }

    // TC-06-04: Авторизация с неверным паролем
    @Test
    void TC_06_04() throws Exception {
        // Предусловие
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"wrongpass@test.com","password":"correctpass","role":"PARTICIPANT"}
                    """));

        var user = userRepository.findByEmail("wrongpass@test.com").orElseThrow();
        user.setEnabled(true);
        userRepository.save(user);

        // Неверный пароль
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"email":"wrongpass@test.com","password":"wrongpassword"}
                    """))
                .andExpect(status().isUnauthorized());
    }
}