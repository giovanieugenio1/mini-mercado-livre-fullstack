package com.miniml.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("UserRepository — IT")
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    UserRepository userRepository;

    // ── Fixture ───────────────────────────────────────────────────────────────

    private User buildUser(String email, String cpf) {
        return new User("João", "Silva", email, cpf, null);
    }

    // ── Caminhos felizes ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deve persistir e recuperar usuário pelo ID")
    void save_and_find_by_id() {
        User user = userRepository.save(buildUser("joao@exemplo.com", "529.982.247-25"));

        assertThat(user.getId()).isNotNull();
        var found = userRepository.findById(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("joao@exemplo.com");
        assertThat(found.get().getCpf()).isEqualTo("529.982.247-25");
        assertThat(found.get().getFirstName()).isEqualTo("João");
        assertThat(found.get().getLastName()).isEqualTo("Silva");
        assertThat(found.get().isActive()).isTrue();
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("existsByEmail deve retornar true quando e-mail existe")
    void exists_by_email_returns_true() {
        userRepository.save(buildUser("maria@exemplo.com", "046.434.160-60"));

        assertThat(userRepository.existsByEmail("maria@exemplo.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail deve retornar false quando e-mail não existe")
    void exists_by_email_returns_false() {
        assertThat(userRepository.existsByEmail("nao-existe@exemplo.com")).isFalse();
    }

    @Test
    @DisplayName("existsByCpf deve retornar true quando CPF existe")
    void exists_by_cpf_returns_true() {
        userRepository.save(buildUser("pedro@exemplo.com", "529.982.247-25"));

        assertThat(userRepository.existsByCpf("529.982.247-25")).isTrue();
    }

    @Test
    @DisplayName("existsByCpf deve retornar false quando CPF não existe")
    void exists_by_cpf_returns_false() {
        assertThat(userRepository.existsByCpf("000.000.000-00")).isFalse();
    }

    // ── Caminhos de falha ─────────────────────────────────────────────────────

    @Test
    @DisplayName("deve retornar Optional vazio quando ID não existe")
    void find_by_id_returns_empty_when_not_found() {
        var result = userRepository.findById(java.util.UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deve persistir usuário com keycloakId após assignKeycloakId")
    void assign_keycloak_id() {
        User user = userRepository.save(buildUser("ana@exemplo.com", "046.434.160-60"));
        user.assignKeycloakId("kc-uuid-xyz");
        userRepository.save(user);

        var found = userRepository.findById(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getKeycloakId()).isEqualTo("kc-uuid-xyz");
    }

    @Test
    @DisplayName("deve persistir usuário com endereço embutido")
    void save_user_with_address() {
        var address = new AddressData(
                "Av. Paulista", "1000", "Apto 5",
                "Bela Vista", "São Paulo", "SP", "01310-100");
        User user = new User("Carlos", "Oliveira", "carlos@exemplo.com", "529.982.247-25", address);
        userRepository.save(user);

        var found = userRepository.findById(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAddress()).isNotNull();
        assertThat(found.get().getAddress().getCidade()).isEqualTo("São Paulo");
        assertThat(found.get().getAddress().getEstado()).isEqualTo("SP");
    }
}
