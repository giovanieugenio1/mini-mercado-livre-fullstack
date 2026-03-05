package com.miniml.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: verifica que o contexto Spring carrega corretamente
 * com todas as dependências (DB via Testcontainers, Keycloak com valores fictícios).
 */
@SpringBootTest
@Testcontainers
class UserServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void contextLoads() {
        // Garante que beans, configurações e migrações Flyway estão corretos
    }
}
