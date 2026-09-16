package com.diversao.backend.cart;

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
 * Integração de CartController: autenticação obrigatória, adicionar/atualizar
 * itens respeitando estoque, e a regra de quantity <= 0 remover o item.
 */
class CartControllerTest extends AbstractIntegrationTest {

    private long createCategoryAndProduct(String adminToken, int stock) throws Exception {
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
                                "stock", stock,
                                "categoryId", catId))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(prod.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("GET /api/cart: sem token devolve 401")
    void getSemToken() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Fluxo: adicionar item, atualizar quantidade, remover com quantity=0")
    void fluxoBasico() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String user = registerUserAndGetToken("senha123");
        long productId = createCategoryAndProduct(admin, 10);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productId", productId, "quantity", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.total", is(20.00)));

        mockMvc.perform(put("/api/cart/items/" + productId)
                        .header("Authorization", "Bearer " + user)
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity", is(5)));

        mockMvc.perform(put("/api/cart/items/" + productId)
                        .header("Authorization", "Bearer " + user)
                        .param("quantity", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        mockMvc.perform(delete("/api/cart")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/cart/items: quantidade > estoque devolve 400")
    void addItemAcimaDoEstoque() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String user = registerUserAndGetToken("senha123");
        long productId = createCategoryAndProduct(admin, 3);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productId", productId, "quantity", 4))))
                .andExpect(status().isBadRequest());
    }
}