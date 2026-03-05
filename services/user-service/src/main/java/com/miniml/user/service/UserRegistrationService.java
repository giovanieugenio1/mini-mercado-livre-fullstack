package com.miniml.user.service;

import com.miniml.user.domain.*;
import com.miniml.user.dto.RegisterRequest;
import com.miniml.user.dto.RegisterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class UserRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationService.class);

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    private final UserRepository           userRepository;
    private final UserCredentialRepository credentialRepository;
    private final BCryptPasswordEncoder    passwordEncoder;
    private final RestClient               restClient;

    public UserRegistrationService(UserRepository userRepository,
                                   UserCredentialRepository credentialRepository,
                                   BCryptPasswordEncoder passwordEncoder,
                                   RestClient restClient) {
        this.userRepository       = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder      = passwordEncoder;
        this.restClient           = restClient;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado: " + request.email());
        }
        if (userRepository.existsByCpf(request.cpf())) {
            throw new IllegalArgumentException("CPF já cadastrado.");
        }

        AddressData address = null;
        if (request.address() != null) {
            var a = request.address();
            address = new AddressData(a.logradouro(), a.numero(), a.complemento(),
                                      a.bairro(), a.cidade(), a.estado(), a.cep());
        }

        User user = new User(request.firstName(), request.lastName(),
                             request.email(), request.cpf(), address);
        userRepository.save(user);

        String hash = passwordEncoder.encode(request.password());
        UserCredential credential = new UserCredential(user, request.email(), hash);
        credentialRepository.save(credential);

        String adminToken = getAdminToken();
        String keycloakId = createKeycloakUser(request, adminToken);
        assignUserRole(keycloakId, adminToken);

        user.assignKeycloakId(keycloakId);
        userRepository.save(user);

        log.info("Usuário cadastrado: email={} localId={} keycloakId={}", request.email(), user.getId(), keycloakId);
        return new RegisterResponse(user.getId().toString(), request.firstName(),
                                    request.lastName(), request.email(), "Cadastro realizado com sucesso!");
    }

    // ── Keycloak Admin API ────────────────────────────────────────────────────

    private String getAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(keycloakUrl + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        return (String) response.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private String createKeycloakUser(RegisterRequest request, String adminToken) {
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", request.password(),
                "temporary", false);

        Map<String, Object> userRep = Map.of(
                "username", request.email(),
                "email", request.email(),
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(credential),
                "attributes", Map.of("cpf", List.of(request.cpf())));

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(keycloakUrl + "/admin/realms/" + realm + "/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .body(userRep)
                    .retrieve()
                    .toBodilessEntity();

            String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            if (location == null) throw new IllegalStateException("Keycloak não retornou Location header");
            return location.substring(location.lastIndexOf('/') + 1);

        } catch (HttpClientErrorException.Conflict e) {
            throw new IllegalArgumentException("E-mail já cadastrado no provedor de identidade: " + request.email());
        }
    }

    @SuppressWarnings("unchecked")
    private void assignUserRole(String userId, String adminToken) {
        Map<String, Object> roleObj = restClient.get()
                .uri(keycloakUrl + "/admin/realms/" + realm + "/roles/ROLE_USER")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(Map.class);

        restClient.post()
                .uri(keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .body(List.of(roleObj))
                .retrieve()
                .toBodilessEntity();
    }
}
