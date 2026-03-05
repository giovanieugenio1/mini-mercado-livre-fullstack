package com.miniml.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Testes de integração do API Gateway:
 * - Não conecta a serviços downstream reais
 * - Verifica regras de segurança (401, 403) e health endpoint
 * - ReactiveJwtDecoder mockado (sem Keycloak)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayIT {

    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;

    @Autowired
    WebTestClient webTestClient;

    // ── Actuator ──────────────────────────────────────────────────

    @Test
    void healthEndpoint_semAuth_retorna200() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    // ── Regras de autenticação ─────────────────────────────────────

    @Test
    void getProducts_semAuth_rotaPublica_naoRetorna401() {
        // GET /api/v1/products é público; sem auth NÃO retorna 401.
        // Pode retornar 502/503 (serviço downstream offline) — apenas
        // garantimos que não é 401.
        webTestClient.get()
                .uri("/api/v1/products")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().value(status ->
                        org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }

    @Test
    void postOrder_semAuth_retorna401() {
        webTestClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"customerId\":\"00000000-0000-0000-0000-000000000000\",\"items\":[]}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getInventory_semAuth_retorna401() {
        webTestClient.get()
                .uri("/api/v1/inventory/00000000-0000-0000-0000-000000000000")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getNotifications_semAuth_retorna401() {
        webTestClient.get()
                .uri("/api/v1/notifications")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rotaDesconhecida_semAuth_retorna401() {
        webTestClient.get()
                .uri("/api/v1/unknown-resource")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void postOrder_comJwtValido_naoRetorna401() {
        // Com JWT presente, a segurança do gateway é satisfeita.
        // O upstream (offline) pode retornar 502/503, mas NÃO 401.
        webTestClient.mutateWith(mockJwt())
                .post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"customerId\":\"00000000-0000-0000-0000-000000000000\",\"items\":[]}")
                .exchange()
                .expectStatus().value(status ->
                        org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }
}
