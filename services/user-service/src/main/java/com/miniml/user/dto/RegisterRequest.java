package com.miniml.user.dto;

import com.miniml.user.validation.Cpf;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Nome é obrigatório") String firstName,
        @NotBlank(message = "Sobrenome é obrigatório") String lastName,
        @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
        @NotBlank(message = "CPF é obrigatório") @Cpf String cpf,
        @NotBlank(message = "Senha é obrigatória") @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String password,
        AddressRequest address  
) {}
