package com.diversao.backend.review;

import com.diversao.backend.it.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integração de ReviewController: leitura pública, criação autenticada com
 * constraint única (product, user) refletida como 409, e DELETE por autor/ADMIN.
 */
class ReviewControllerTest extends AbstractIntegrationTest {

    private long createProduct(String adminToken) throws Exception {
        var cat = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Cat-" + uniqueSuffix()))))
                .andExpect(status().isCreated()).andReturn();
        long catId = objectMapper.readTree(cat.getResponse().getContentAsString()).get("id").asLong();

        var prod = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Prod-" + uniqueSuffix(),
                                "description", "d",
                                "price", new BigDecimal("10.00"),
                                "stock", 5,
                                "categoryId", catId))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(prod.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("GET /api/products/{id}/reviews: público")
    void listPublico() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        long productId = createProduct(admin);

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/reviews: segundo review do mesmo usuário devolve 409")
    void reviewDuplicado() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String user = registerUserAndGetToken("senha123");
        long productId = createProduct(admin);

        var body = Map.of("productId", productId, "rating", 5, "comment", "ótimo");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/reviews: rating fora de 1..5 devolve 400 com errors.rating")
    void ratingInvalido() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String user = registerUserAndGetToken("senha123");
        long productId = createProduct(admin);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productId", productId, "rating", 6, "comment", "x"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating", not(emptyString())));
    }

    @Test
    @DisplayName("DELETE /api/reviews/{id}: outro usuário sem ADMIN devolve 400; ADMIN consegue")
    void deleteAutorOuAdmin() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String author = registerUserAndGetToken("senha123");
        String intruder = registerUserAndGetToken("senha123");
        long productId = createProduct(admin);

        var created = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + author)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productId", productId, "rating", 4, "comment", "ok"))))
                .andExpect(status().isCreated()).andReturn();
        long reviewId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + intruder))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());;
    }
}