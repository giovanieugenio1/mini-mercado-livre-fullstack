package com.miniml.shipping.integration;

import com.miniml.shipping.domain.Shipping;
import com.miniml.shipping.domain.ShippingRepository;
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
            "inventory.reserved.v1",
            "shipping.created.v1",
            "shipping.failed.v1"
        }
)
class ShippingControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired MockMvc mockMvc;
    @Autowired ShippingRepository shippingRepository;

    // ── GET /shippings/order/{orderId} ────────────────────────────────────────

    @Test
    void deveRetornar200ComEnvioExistente() throws Exception {
        var orderId    = UUID.randomUUID();
        var customerId = UUID.randomUUID();
        var shipping   = Shipping.createShipping(orderId, customerId, "MINIMLTEST001");
        shippingRepository.save(shipping);

        mockMvc.perform(get("/shippings/order/{orderId}", orderId)
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.trackingCode").value("MINIMLTEST001"));
    }

    @Test
    void deveRetornar404QuandoEnvioNaoExiste() throws Exception {
        mockMvc.perform(get("/shippings/order/{orderId}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Envio não encontrado"));
    }

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/shippings/order/{orderId}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /shippings ────────────────────────────────────────────────────────

    @Test
    void deveRetornarListaPaginadaDeEnvios() throws Exception {
        mockMvc.perform(get("/shippings")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }
}
