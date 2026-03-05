package com.miniml.user.service;

import com.miniml.user.domain.User;
import com.miniml.user.domain.UserCredential;
import com.miniml.user.domain.UserCredentialRepository;
import com.miniml.user.domain.UserRepository;
import com.miniml.user.dto.RegisterRequest;
import com.miniml.user.dto.RegisterResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserRegistrationService")
class UserRegistrationServiceTest {

    // ── Dependências mockadas ────────────────────────────────────────────────
    @Mock private UserRepository           userRepository;
    @Mock private UserCredentialRepository credentialRepository;
    @Mock private BCryptPasswordEncoder    passwordEncoder;
    @Mock private RestClient               restClient;

    // RETURNS_SELF: body(), contentType(), header(), uri() retornam o próprio mock automaticamente
    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.RequestBodyUriSpec postSpec;

    @Mock(answer = Answers.RETURNS_SELF)
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec getSpec;

    @Mock private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private UserRegistrationService service;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private static final String FAKE_KEYCLOAK_ID = "kc-" + UUID.randomUUID();

    private RegisterRequest validRequest() {
        return new RegisterRequest(
                "João", "Silva",
                "joao@exemplo.com", "529.982.247-25",
                "senha123", null);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "keycloakUrl",   "http://fake-kc:8080");
        ReflectionTestUtils.setField(service, "realm",         "test-realm");
        ReflectionTestUtils.setField(service, "adminUsername", "admin");
        ReflectionTestUtils.setField(service, "adminPassword", "admin");

        // Stubs comuns: RETURNS_SELF cuida de body/contentType/header/uri automaticamente
        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.retrieve()).thenReturn(responseSpec);
    }

    // ── Caminhos felizes ──────────────────────────────────────────────────────

    @Test
    @DisplayName("register() deve persistir usuário e retornar RegisterResponse quando dados são válidos")
    @SuppressWarnings("unchecked")
    void register_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });
        when(credentialRepository.save(any(UserCredential.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        when(responseSpec.body(Map.class))
                .thenReturn(Map.of("access_token", "fake-admin-token"))
                .thenReturn(Map.of("id", "role-id", "name", "ROLE_USER"));

        HttpHeaders locationHeaders = new HttpHeaders();
        locationHeaders.set(HttpHeaders.LOCATION,
                "http://fake-kc/admin/realms/test/users/" + FAKE_KEYCLOAK_ID);
        when(responseSpec.toBodilessEntity())
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).headers(locationHeaders).build())
                .thenReturn(ResponseEntity.noContent().build());

        RegisterResponse result = service.register(validRequest());

        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("João");
        assertThat(result.lastName()).isEqualTo("Silva");
        assertThat(result.email()).isEqualTo("joao@exemplo.com");
        assertThat(result.id()).isNotNull();
        assertThat(result.message()).isNotBlank();

        verify(userRepository, times(2)).save(any(User.class));
        verify(credentialRepository).save(any(UserCredential.class));
        verify(passwordEncoder).encode("senha123");
    }

    @Test
    @DisplayName("register() deve persistir usuário com endereço quando address é fornecido")
    @SuppressWarnings("unchecked")
    void register_success_with_address() {
        var requestWithAddress = new RegisterRequest(
                "Maria", "Santos",
                "maria@exemplo.com", "111.444.777-35",
                "senha456",
                new com.miniml.user.dto.AddressRequest(
                        "Rua A", "10", null, "Centro", "São Paulo", "SP", "01310-100"));

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });
        when(credentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        when(responseSpec.body(Map.class))
                .thenReturn(Map.of("access_token", "token"))
                .thenReturn(Map.of("id", "r", "name", "ROLE_USER"));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, "http://kc/users/" + FAKE_KEYCLOAK_ID);
        when(responseSpec.toBodilessEntity())
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).headers(headers).build())
                .thenReturn(ResponseEntity.noContent().build());

        RegisterResponse result = service.register(requestWithAddress);

        assertThat(result.email()).isEqualTo("maria@exemplo.com");
        assertThat(result.id()).isNotNull();
    }

    // ── Caminhos de falha ─────────────────────────────────────────────────────

    @Test
    @DisplayName("register() deve lançar IllegalArgumentException quando e-mail já está cadastrado")
    void register_fails_when_email_already_exists() {
        when(userRepository.existsByEmail("joao@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joao@exemplo.com");

        verify(userRepository, never()).save(any());
        verify(credentialRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() deve lançar IllegalArgumentException quando CPF já está cadastrado")
    void register_fails_when_cpf_already_exists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf("529.982.247-25")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() deve lançar IllegalArgumentException quando Keycloak retorna 409 Conflict")
    @SuppressWarnings("unchecked")
    void register_fails_when_keycloak_returns_conflict() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });
        when(credentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        when(responseSpec.body(Map.class)).thenReturn(Map.of("access_token", "token"));

        HttpClientErrorException conflict = HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY, null, null);
        when(responseSpec.toBodilessEntity()).thenThrow(conflict);

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provedor de identidade");
    }

    @Test
    @DisplayName("register() deve lançar IllegalStateException quando Keycloak não retorna header Location")
    @SuppressWarnings("unchecked")
    void register_fails_when_keycloak_returns_no_location_header() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });
        when(credentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        when(responseSpec.body(Map.class)).thenReturn(Map.of("access_token", "token"));
        when(responseSpec.toBodilessEntity())
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build()); // sem Location header

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Location");
    }
}
