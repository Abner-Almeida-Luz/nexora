package com.diversao.backend.category;

import com.diversao.backend.it.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;

/**
 * Integração de CategoryController: leitura pública, escrita restrita a ADMIN
 * e validação de entrada (ProblemDetail com mapa errors).
 */
class CategoryControllerTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("GET /api/categories: público, sem token")
    void listPublico() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/categories: sem token devolve 401")
    void postSemToken() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "X"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/categories: USER autenticado devolve 403")
    void postComoUser() throws Exception {
        String token = registerUserAndGetToken("senha123");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "X"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/categories: ADMIN cria com 201")
    void postComoAdmin() throws Exception {
        String token = createAdminAndGetToken("admin123");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Livros-" + uniqueSuffix()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", startsWith("Livros-")));
    }

    @Test
    @DisplayName("POST /api/categories: nome em branco devolve 400 com errors.name")
    void postNomeEmBranco() throws Exception {
        String token = createAdminAndGetToken("admin123");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", not(emptyString())));
    }

    @Test
    @DisplayName("PUT /api/categories/{id}: USER devolve 403")
    void putComoUser() throws Exception {
        String adminToken = createAdminAndGetToken("admin123");
        String name = "Cat-" + uniqueSuffix();

        var created = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        String userToken = registerUserAndGetToken("senha123");
        mockMvc.perform(put("/api/categories/" + id)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name + "-novo"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/categories/{id}: USER devolve 403, ADMIN devolve 204")
    void deleteRoles() throws Exception {
        String adminToken = createAdminAndGetToken("admin123");
        var created = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Del-" + uniqueSuffix()))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        String userToken = registerUserAndGetToken("senha123");
        mockMvc.perform(delete("/api/categories/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/categories/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/categories/{id}: categoria com produtos devolve 409, não 500")
    void deleteCategoriaComProdutos() throws Exception {
        String admin = createAdminAndGetToken("admin123");

        // Cria categoria
        var catMvc = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "ComProd-" + uniqueSuffix()))))
                .andExpect(status().isCreated())
                .andReturn();
        long catId = objectMapper.readTree(catMvc.getResponse().getContentAsString()).get("id").asLong();

        // Cria produto associado
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "P-" + uniqueSuffix(),
                                "description", "d",
                                "price", new BigDecimal("10.00"),
                                "stock", 1,
                                "categoryId", catId))))
                .andExpect(status().isCreated());

        // DELETE deve ser 409, não 500 por FK violation
        mockMvc.perform(delete("/api/categories/" + catId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Conflict")))
                .andExpect(jsonPath("$.detail", containsString("products associated")));
    }
}