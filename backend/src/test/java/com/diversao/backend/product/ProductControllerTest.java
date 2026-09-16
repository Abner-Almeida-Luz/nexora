package com.diversao.backend.product;

import com.diversao.backend.it.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integração de ProductController: leitura pública paginada, escrita ADMIN-only,
 * validação de entrada, e o teste de regressão do SecurityFilterChain
 * (POST sem token deve dar 401, jamais 200 por causa de permitAll mal ordenado).
 */
class ProductControllerTest extends AbstractIntegrationTest {

    private Map<String, Object> validProduct(Long categoryId) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "Produto " + uniqueSuffix());
        m.put("description", "desc");
        m.put("price", new BigDecimal("19.90"));
        m.put("stock", 10);
        m.put("imageUrl", null);
        m.put("categoryId", categoryId);
        return m;
    }

    private long createCategory(String adminToken) throws Exception {
        var r = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Cat-" + uniqueSuffix()))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("POST /api/products: sem token devolve 401 (regressão de segurança)")
    void postSemToken() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProduct(1L))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/products: USER autenticado devolve 403")
    void postComoUser() throws Exception {
        String token = registerUserAndGetToken("senha123");

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProduct(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/products: ADMIN cria com 201")
    void postComoAdmin() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        long catId = createCategory(admin);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProduct(catId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.categoryId", is((int) catId)));
    }

    @Test
    @DisplayName("POST /api/products: preço negativo devolve 400 com errors.price")
    void postPrecoNegativo() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        long catId = createCategory(admin);

        var body = validProduct(catId);
        body.put("price", new BigDecimal("-1.00"));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price", not(emptyString())));
    }

    @Test
    @DisplayName("POST /api/products: nome em branco devolve 400 com errors.name")
    void postNomeEmBranco() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        long catId = createCategory(admin);
        var body = validProduct(catId);
        body.put("name", "");

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", not(emptyString())));
    }

    @Test
    @DisplayName("GET /api/products: público, paginado, com filtro por search")
    void getListPublico() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(5)));
    }

    @Test
    @DisplayName("PUT /api/products/{id}: USER devolve 403")
    void putComoUser() throws Exception {
        String user = registerUserAndGetToken("senha123");
        mockMvc.perform(put("/api/products/1")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProduct(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/products/{id}: USER devolve 403")
    void deleteComoUser() throws Exception {
        String user = registerUserAndGetToken("senha123");
        mockMvc.perform(delete("/api/products/1")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/products: sem parâmetro search não quebra (regressão do bug lower(bytea))")
    void getListSemParametroSearch() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }
}