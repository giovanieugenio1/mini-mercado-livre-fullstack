package com.miniml.order.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração order-service:
 * - PostgreSQL via Testcontainers (@ServiceConnection)
 * - Flyway roda V1 (sem seed)
 * - Kafka embarcado via @EmbeddedKafka (sem Docker Kafka)
 * - JwtDecoder mockado
 * - Endpoints autenticados com jwt() do spring-security-test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@EmbeddedKafka(
        partitions = 1,
        topics = {
            "order.created.v1", "order.completed.v1",
            "payment.authorized.v1", "payment.failed.v1",
            "inventory.reserved.v1", "inventory.failed.v1",
            "shipping.created.v1", "shipping.failed.v1"
        },
        brokerProperties = {
            "listeners=PLAINTEXT://localhost:${spring.embedded.kafka.brokers}",
            "log.dir=/tmp/kafka-order-it"
        }
)
class OrderControllerIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mockMvc;

    // ── POST /orders ──────────────────────────────────────────

    @Test
    void criarPedido_payloadValido_retorna201() throws Exception {
        String body = """
                {
                  "customerId": "%s",
                  "items": [
                    {
                      "productId": "%s",
                      "productTitle": "iPhone 15 Pro",
                      "unitPrice": 7999.99,
                      "quantity": 2
                    }
                  ]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/orders")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(15999.98))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productTitle").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.items[0].lineTotal").value(15999.98));
    }

    @Test
    void criarPedido_semAutenticacao_retorna401() throws Exception {
        String body = """
                {"customerId":"%s","items":[{"productId":"%s",
                "productTitle":"X","unitPrice":10,"quantity":1}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criarPedido_itensvazios_retorna400ComProblemDetail() throws Exception {
        String body = """
                {"customerId": "%s", "items": []}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/orders")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"));
    }

    @Test
    void criarPedido_quantidadeZero_retorna400() throws Exception {
        String body = """
                {
                  "customerId": "%s",
                  "items": [{"productId":"%s","productTitle":"X","unitPrice":10,"quantity":0}]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/orders")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── GET /orders/{id} ──────────────────────────────────────

    @Test
    void buscarPedidoPorId_idInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/orders/00000000-0000-0000-0000-000000000000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pedido não encontrado"));
    }

    @Test
    void buscarPedidoPorId_pedidoCriado_retornaDetalhe() throws Exception {
        // Cria pedido
        String customerId = UUID.randomUUID().toString();
        String createBody = """
                {
                  "customerId": "%s",
                  "items": [{"productId":"%s","productTitle":"Galaxy S24",
                              "unitPrice":4599.90,"quantity":1}]
                }
                """.formatted(customerId, UUID.randomUUID());

        String location = mockMvc.perform(post("/orders")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        // Consulta pelo ID extraído do Location header
        String id = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/orders/" + id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items[0].productTitle").value("Galaxy S24"));
    }

    // ── GET /orders ───────────────────────────────────────────

    @Test
    void listarPedidos_retornaOkComPaginacao() throws Exception {
        mockMvc.perform(get("/orders").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    // ── PATCH /orders/{id}/cancel ─────────────────────────────

    @Test
    void cancelarPedido_statusCreated_retorna200Cancelado() throws Exception {
        // Cria pedido
        String createBody = """
                {
                  "customerId": "%s",
                  "items": [{"productId":"%s","productTitle":"Produto X",
                              "unitPrice":100.00,"quantity":1}]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        String location = mockMvc.perform(post("/orders")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        String id = location.substring(location.lastIndexOf('/') + 1);

        // Cancela o pedido
        mockMvc.perform(patch("/orders/{id}/cancel", id)
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelarPedido_idInexistente_retorna404() throws Exception {
        mockMvc.perform(patch("/orders/{id}/cancel", UUID.randomUUID())
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pedido não encontrado"));
    }

    @Test
    void cancelarPedido_jaFoiCancelado_retorna409() throws Exception {
        // Cria e cancela o pedido uma vez
        String createBody = """
                {
                  "customerId": "%s",
                  "items": [{"productId":"%s","productTitle":"Produto Y",
                              "unitPrice":50.00,"quantity":1}]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        String location = mockMvc.perform(post("/orders")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        String id = location.substring(location.lastIndexOf('/') + 1);

        // Primeiro cancel → OK
        mockMvc.perform(patch("/orders/{id}/cancel", id)
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Segundo cancel → 409 Conflict
        mockMvc.perform(patch("/orders/{id}/cancel", id)
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Operação inválida"));
    }
}
