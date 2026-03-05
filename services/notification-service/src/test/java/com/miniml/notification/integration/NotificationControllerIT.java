package com.miniml.notification.integration;

import com.miniml.notification.domain.Notification;
import com.miniml.notification.domain.NotificationRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@EmbeddedKafka(
        partitions = 1,
        topics = {
            "order.created.v1",
            "order.completed.v1",
            "payment.authorized.v1",
            "payment.failed.v1",
            "inventory.reserved.v1",
            "inventory.failed.v1",
            "shipping.created.v1",
            "shipping.failed.v1"
        }
)
class NotificationControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired MockMvc mockMvc;
    @Autowired NotificationRepository notificationRepository;

    static final UUID ORDER_ID    = UUID.randomUUID();
    static final UUID CUSTOMER_ID = UUID.randomUUID();

    // ── GET /notifications ────────────────────────────────────────────────────

    @Test
    void deveRetornarListaVaziaInicialmente() throws Exception {
        mockMvc.perform(get("/notifications")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void deveRetornarNotificacoesExistentes() throws Exception {
        notificationRepository.save(
                Notification.of(ORDER_ID, CUSTOMER_ID, "order.created.v1",
                        "Pedido criado!", "Corpo da notificação"));

        mockMvc.perform(get("/notifications")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ── GET /notifications/order/{orderId} ────────────────────────────────────

    @Test
    void deveRetornarNotificacoesDoPedido() throws Exception {
        var orderId = UUID.randomUUID();
        notificationRepository.save(
                Notification.of(orderId, CUSTOMER_ID, "payment.authorized.v1",
                        "Pagamento aprovado!", "Seu pagamento foi aprovado."));

        mockMvc.perform(get("/notifications/order/{orderId}", orderId)
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.content[0].eventType").value("payment.authorized.v1"));
    }

    @Test
    void deveRetornarListaVaziaParaPedidoSemNotificacoes() throws Exception {
        mockMvc.perform(get("/notifications/order/{orderId}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── GET /notifications/customer/{customerId} ──────────────────────────────

    @Test
    void deveRetornarNotificacoesDoCliente() throws Exception {
        var customerId = UUID.randomUUID();
        notificationRepository.save(
                Notification.of(UUID.randomUUID(), customerId, "order.completed.v1",
                        "Pedido concluído!", "Parabéns!"));

        mockMvc.perform(get("/notifications/customer/{customerId}", customerId)
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerId").value(customerId.toString()));
    }

    // ── Autenticação ──────────────────────────────────────────────────────────

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/notifications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
