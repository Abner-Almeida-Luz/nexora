package com.diversao.backend.order;

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
 * Integração de OrderController: fluxo carrinho→pedido com baixa real de
 * estoque, isolamento entre usuários, endpoints ADMIN-only.
 */
class OrderControllerTest extends AbstractIntegrationTest {

    private long createCategoryAndProduct(String adminToken, int stock, BigDecimal price) throws Exception {
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
                                "price", price,
                                "stock", stock,
                                "categoryId", catId))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(prod.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("Fluxo ponta a ponta: criar produto, adicionar, finalizar, verificar estoque e pedido")
    void fluxoCompleto() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String user = registerUserAndGetToken("senha123");
        long productId = createCategoryAndProduct(admin, 10, new BigDecimal("10.00"));

        // adiciona 3 unidades
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productId", productId, "quantity", 3))))
                .andExpect(status().isOk());

        // finaliza
        var orderMvc = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total", is(30.00)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andReturn();
        long orderId = objectMapper.readTree(orderMvc.getResponse().getContentAsString()).get("id").asLong();

        // estoque decrementado para 7
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(7)));

        // GET /api/orders do usuário inclui o pedido
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is((int) orderId)));

        // carrinho foi limpo
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("Isolamento: usuário B não vê pedido de usuário A (400)")
    void isolamentoEntreUsuarios() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String userA = registerUserAndGetToken("senha123");
        String userB = registerUserAndGetToken("senha123");
        long productId = createCategoryAndProduct(admin, 5, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + userA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isOk());

        var orderMvc = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userA))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(orderMvc.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + userB))
                .andExpect(status().isForbidden());;
    }

    @Test
    @DisplayName("GET /api/orders/admin/all: USER devolve 403, ADMIN devolve 200")
    void adminAll() throws Exception {
        String user = registerUserAndGetToken("senha123");
        String admin = createAdminAndGetToken("admin123");

        mockMvc.perform(get("/api/orders/admin/all")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders/admin/all")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status: USER devolve 403, ADMIN altera para SHIPPED")
    void patchStatus() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String user = registerUserAndGetToken("senha123");
        long productId = createCategoryAndProduct(admin, 5, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isOk());

        var orderMvc = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isCreated()).andReturn();
        long orderId = objectMapper.readTree(orderMvc.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SHIPPED"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SHIPPED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SHIPPED")));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel: dono cancela PENDING e estoque é devolvido")
    void cancelarPedidoProprio() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String user = registerUserAndGetToken("senha123");
        long productId = createCategoryAndProduct(admin, 10, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productId", productId, "quantity", 3))))
                .andExpect(status().isOk());

        var created = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Estoque foi para 7
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(jsonPath("$.stock", is(7)));

        // Cancela
        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // Estoque voltou para 10
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(jsonPath("$.stock", is(10)));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel: pedido SHIPPED devolve 400")
    void cancelarPedidoNaoPending() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String user = registerUserAndGetToken("senha123");
        long productId = createCategoryAndProduct(admin, 5, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isOk());

        var created = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // ADMIN move para SHIPPED
        mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SHIPPED"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail",
                        is("Only pending orders can be cancelled")));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel: usuário tentando cancelar pedido alheio devolve 403")
    void cancelarPedidoAlheio() throws Exception {
        String admin = createAdminAndGetToken("admin123");
        String userA = registerUserAndGetToken("senha123");
        String userB = registerUserAndGetToken("senha123");
        long productId = createCategoryAndProduct(admin, 5, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + userA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isOk());

        var created = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userA))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + userB))
                .andExpect(status().isForbidden());
    }
}