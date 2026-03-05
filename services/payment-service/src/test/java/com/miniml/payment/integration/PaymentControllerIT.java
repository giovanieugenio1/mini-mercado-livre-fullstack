package com.miniml.payment.integration;

import com.miniml.payment.domain.Payment;
import com.miniml.payment.domain.PaymentRepository;
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

import java.math.BigDecimal;
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
            "payment.authorized.v1",
            "payment.failed.v1"
        }
)
class PaymentControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired MockMvc mockMvc;
    @Autowired PaymentRepository paymentRepository;

    // ── GET /payments/{id} ────────────────────────────────────────────────────

    @Test
    void deveRetornar200AoBuscarPagamentoPorId() throws Exception {
        var payment = paymentRepository.save(buildPayment(new BigDecimal("1500.00")));

        mockMvc.perform(get("/payments/{id}", payment.getId())
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId().toString()))
                .andExpect(jsonPath("$.orderId").value(payment.getOrderId().toString()))
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void deveRetornar404QuandoPagamentoNaoExiste() throws Exception {
        mockMvc.perform(get("/payments/{id}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pagamento não encontrado"));
    }

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/payments/{id}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /payments/order/{orderId} ─────────────────────────────────────────

    @Test
    void deveRetornar200AoBuscarPagamentoPorOrderId() throws Exception {
        var payment = paymentRepository.save(buildPayment(new BigDecimal("2000.00")));

        mockMvc.perform(get("/payments/order/{orderId}", payment.getOrderId())
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(payment.getOrderId().toString()));
    }

    @Test
    void deveRetornar404QuandoOrderIdNaoTemPagamento() throws Exception {
        mockMvc.perform(get("/payments/order/{orderId}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ── GET /payments ─────────────────────────────────────────────────────────

    @Test
    void deveRetornarListaPaginada() throws Exception {
        mockMvc.perform(get("/payments")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Payment buildPayment(BigDecimal amount) {
        return Payment.create(UUID.randomUUID(), UUID.randomUUID(), amount, "[]");
    }
}
