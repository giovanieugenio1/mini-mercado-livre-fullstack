package com.miniml.inventory.integration;

import com.miniml.inventory.domain.ProductStock;
import com.miniml.inventory.domain.ProductStockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@EmbeddedKafka(
        partitions = 1,
        topics = {
            "payment.authorized.v1",
            "inventory.reserved.v1",
            "inventory.failed.v1"
        }
)
class InventoryControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired MockMvc mockMvc;
    @Autowired ProductStockRepository productStockRepository;

    static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    // ── GET /inventory/{productId} ────────────────────────────────────────────

    @Test
    void deveRetornar200ComEstoqueExistente() throws Exception {
        // O seed V2 já insere o produto 1111..., mas em IT usamos Testcontainers
        // que só roda V1 inicialmente — inserimos manualmente para o teste
        if (productStockRepository.findById(PRODUCT_ID).isEmpty()) {
            productStockRepository.save(ProductStock.of(PRODUCT_ID, 100));
        }

        mockMvc.perform(get("/inventory/{productId}", PRODUCT_ID)
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.availableQty").isNumber())
                .andExpect(jsonPath("$.reservedQty").isNumber());
    }

    @Test
    void deveRetornar404ParaProdutoNaoExistenteNoInventario() throws Exception {
        mockMvc.perform(get("/inventory/{productId}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Produto não encontrado no inventário"));
    }

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/inventory/{productId}", PRODUCT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /inventory ────────────────────────────────────────────────────────

    @Test
    void deveRetornarListaPaginadaDeEstoque() throws Exception {
        mockMvc.perform(get("/inventory")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    // ── GET /reservations/order/{orderId} ─────────────────────────────────────

    @Test
    void deveRetornar404QuandoReservaNaoExiste() throws Exception {
        mockMvc.perform(get("/reservations/order/{orderId}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Reserva não encontrada"));
    }

    // ── POST /inventory ───────────────────────────────────────────────────────

    @Test
    void postInventory_adminCriaEstoque_retorna200() throws Exception {
        var novoProductId = UUID.randomUUID();
        var body = """
                {"productId":"%s","availableQty":50}
                """.formatted(novoProductId);

        mockMvc.perform(post("/inventory")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(novoProductId.toString()))
                .andExpect(jsonPath("$.availableQty").value(50));
    }

    @Test
    void postInventory_semAdmin_retorna403() throws Exception {
        var body = """
                {"productId":"%s","availableQty":50}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/inventory")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void postInventory_duplicado_retorna409() throws Exception {
        var existingId = UUID.randomUUID();
        productStockRepository.save(ProductStock.of(existingId, 10));

        var body = """
                {"productId":"%s","availableQty":20}
                """.formatted(existingId);

        mockMvc.perform(post("/inventory")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // ── PUT /inventory/{productId} ────────────────────────────────────────────

    @Test
    void putInventory_adminAtualizaEstoque_retorna200() throws Exception {
        var productId = UUID.randomUUID();
        productStockRepository.save(ProductStock.of(productId, 30));

        mockMvc.perform(put("/inventory/{productId}", productId)
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availableQty\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQty").value(99));
    }

    @Test
    void putInventory_produtoNaoExiste_retorna404() throws Exception {
        mockMvc.perform(put("/inventory/{productId}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availableQty\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putInventory_semAdmin_retorna403() throws Exception {
        mockMvc.perform(put("/inventory/{productId}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availableQty\":10}"))
                .andExpect(status().isForbidden());
    }
}
