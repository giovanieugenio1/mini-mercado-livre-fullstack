package com.miniml.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.user.config.SecurityConfig;
import com.miniml.user.dto.RegisterRequest;
import com.miniml.user.dto.RegisterResponse;
import com.miniml.user.service.UserRegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockitoBean
    UserRegistrationService registrationService;

    // ── Fixture ───────────────────────────────────────────────────────────────

    private RegisterRequest validRequest() {
        return new RegisterRequest(
                "João", "Silva",
                "joao@exemplo.com", "529.982.247-25",
                "senha123", null);
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ── Caminhos felizes ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /users/register deve retornar 201 e RegisterResponse quando dados são válidos")
    void register_returns_201_when_valid() throws Exception {
        var response = new RegisterResponse(
                "uuid-1", "João", "Silva", "joao@exemplo.com", "Cadastro realizado com sucesso!");
        when(registrationService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("uuid-1"))
                .andExpect(jsonPath("$.firstName").value("João"))
                .andExpect(jsonPath("$.lastName").value("Silva"))
                .andExpect(jsonPath("$.email").value("joao@exemplo.com"))
                .andExpect(jsonPath("$.message").value("Cadastro realizado com sucesso!"));
    }

    @Test
    @DisplayName("POST /users/register deve retornar 201 quando endereço é fornecido")
    void register_returns_201_with_address() throws Exception {
        var requestWithAddress = new RegisterRequest(
                "Maria", "Santos",
                "maria@exemplo.com", "111.444.777-35",
                "senha456",
                new com.miniml.user.dto.AddressRequest(
                        "Rua A", "10", null, "Centro", "São Paulo", "SP", "01310-100"));

        var response = new RegisterResponse(
                "uuid-2", "Maria", "Santos", "maria@exemplo.com", "Cadastro realizado com sucesso!");
        when(registrationService.register(any())).thenReturn(response);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(requestWithAddress)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"));
    }

    // ── Caminhos de falha — validação de entrada ──────────────────────────────

    @Test
    @DisplayName("POST /users/register deve retornar 400 quando firstName está em branco")
    void register_returns_400_when_first_name_blank() throws Exception {
        var req = new RegisterRequest("", "Silva", "joao@exemplo.com", "529.982.247-25", "senha123", null);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"));
    }

    @Test
    @DisplayName("POST /users/register deve retornar 400 quando lastName está em branco")
    void register_returns_400_when_last_name_blank() throws Exception {
        var req = new RegisterRequest("João", "", "joao@exemplo.com", "529.982.247-25", "senha123", null);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"));
    }

    @Test
    @DisplayName("POST /users/register deve retornar 400 quando e-mail é inválido")
    void register_returns_400_when_email_invalid() throws Exception {
        var req = new RegisterRequest("João", "Silva", "nao-é-email", "529.982.247-25", "senha123", null);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"));
    }

    @Test
    @DisplayName("POST /users/register deve retornar 400 quando CPF é inválido")
    void register_returns_400_when_cpf_invalid() throws Exception {
        var req = new RegisterRequest("João", "Silva", "joao@exemplo.com", "111.111.111-11", "senha123", null);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"));
    }

    @Test
    @DisplayName("POST /users/register deve retornar 400 quando senha tem menos de 8 caracteres")
    void register_returns_400_when_password_too_short() throws Exception {
        var req = new RegisterRequest("João", "Silva", "joao@exemplo.com", "529.982.247-25", "abc", null);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"));
    }

    @Test
    @DisplayName("POST /users/register deve retornar 400 quando body está ausente")
    void register_returns_400_when_body_missing() throws Exception {
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── Caminhos de falha — regras de negócio ─────────────────────────────────

    @Test
    @DisplayName("POST /users/register deve retornar 409 quando e-mail ou CPF já está cadastrado")
    void register_returns_409_when_duplicate() throws Exception {
        when(registrationService.register(any()))
                .thenThrow(new IllegalArgumentException("E-mail já cadastrado: joao@exemplo.com"));

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"))
                .andExpect(jsonPath("$.detail").value("E-mail já cadastrado: joao@exemplo.com"));
    }

    @Test
    @DisplayName("POST /users/register deve retornar 500 quando ocorre erro inesperado")
    void register_returns_500_on_unexpected_error() throws Exception {
        when(registrationService.register(any()))
                .thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"));
    }
}
