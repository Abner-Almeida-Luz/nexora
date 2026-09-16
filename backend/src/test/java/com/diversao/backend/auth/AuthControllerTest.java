package com.diversao.backend.auth;

import com.diversao.backend.it.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do fluxo de autenticação: register, login e uso do
 * token contra um endpoint autenticado (/api/cart).
 */
class AuthControllerTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("register: 201 com token, e-mail e role USER")
    void register_returnsTokenAndUser() throws Exception {
        String email = "novo-" + uniqueSuffix() + "@nexora.test";
        var body = Map.of("name", "Novo", "email", email, "password", "senha123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    @DisplayName("register: e-mail duplicado devolve 409 (ProblemDetail)")
    void register_emailDuplicado() throws Exception {
        String email = "dup-" + uniqueSuffix() + "@nexora.test";
        var body = Map.of("name", "A", "email", email, "password", "senha123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Conflict")))
                .andExpect(jsonPath("$.detail", is("Email already in use")));
    }

    @Test
    @DisplayName("register: senha curta devolve 400 com mapa errors.password")
    void register_senhaCurta() throws Exception {
        var body = Map.of("name", "A", "email", "valido@x.com", "password", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password", not(emptyString())));
    }

    @Test
    @DisplayName("login: credenciais válidas devolvem token reutilizável em /api/cart")
    void login_tokenUsavelEmEndpointAutenticado() throws Exception {
        String token = registerUserAndGetToken("senha123");

        mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()));
    }

    @Test
    @DisplayName("register: JSON malformado devolve 400 (não 500)")
    void registerJsonMalformado() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad request")))
                .andExpect(jsonPath("$.detail", is("Malformed JSON request")));
    }

    @Test
    @DisplayName("login: 6ª tentativa com senha errada devolve 429 (rate limit)")
    void loginRateLimit() throws Exception {
        String email = "rl-" + uniqueSuffix() + "@nexora.test";
        // cria o usuário para garantir que o email é válido (senão o 401 viria por outro motivo)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "RL", "email", email, "password", "senha123"))))
                .andExpect(status().isCreated());

        var badLogin = Map.of("email", email, "password", "errada");

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badLogin)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title", is("Too many requests")));
    }

    @Test
    @DisplayName("refresh: token válido gera novo par { token, refreshToken }")
    void refresh_happyPath() throws Exception {
        String email = "r-" + uniqueSuffix() + "@nexora.test";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "R", "email", email, "password", "senha123"))))
                .andExpect(status().isCreated());

        var loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "senha123"))))
                .andExpect(status().isOk())
                .andReturn();
        String refresh = objectMapper.readTree(loginResp.getResponse().getContentAsString())
                .get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.refreshToken", not(emptyString())));
    }

    @Test
    @DisplayName("refresh: access token enviado no lugar do refresh devolve 401")
    void refresh_rejeitaAccessToken() throws Exception {
        String token = registerUserAndGetToken("senha123"); // é um access token

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", token))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh: string aleatória devolve 401")
    void refresh_rejeitaLixo() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", "nao-e-um-jwt"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh: refresh token no header Bearer não autentica (é rejeitado como access)")
    void refreshTokenNaoServeComoBearer() throws Exception {
        String email = "b-" + uniqueSuffix() + "@nexora.test";
        var reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "B", "email", email, "password", "senha123"))))
                .andExpect(status().isCreated())
                .andReturn();
        String refresh = objectMapper.readTree(reg.getResponse().getContentAsString())
                .get("refreshToken").asText();

        mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }
}