package com.miniml.user.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("CpfValidator")
class CpfValidatorTest {

    private CpfValidator validator;
    private ConstraintValidatorContext ctx;

    @BeforeEach
    void setUp() {
        validator = new CpfValidator();
        ctx       = mock(ConstraintValidatorContext.class);
    }

    // ── Caminhos felizes ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deve aceitar CPF válido formatado (com pontos e traço)")
    void valid_cpf_formatted() {
        assertThat(validator.isValid("529.982.247-25", ctx)).isTrue();
    }

    @Test
    @DisplayName("deve aceitar CPF válido sem formatação (apenas dígitos)")
    void valid_cpf_unformatted() {
        assertThat(validator.isValid("52998224725", ctx)).isTrue();
    }

    @Test
    @DisplayName("deve aceitar outro CPF válido")
    void valid_cpf_another() {
        assertThat(validator.isValid("111.444.777-35", ctx)).isTrue();
    }

    // ── Caminhos de falha ─────────────────────────────────────────────────────

    @Test
    @DisplayName("deve rejeitar null")
    void null_value() {
        assertThat(validator.isValid(null, ctx)).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar string vazia")
    void blank_value() {
        assertThat(validator.isValid("", ctx)).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar string de espaços")
    void whitespace_value() {
        assertThat(validator.isValid("   ", ctx)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "111.111.111-11",
        "222.222.222-22",
        "333.333.333-33",
        "000.000.000-00",
        "99999999999"
    })
    @DisplayName("deve rejeitar sequências uniformes de dígitos")
    void uniform_sequences(String cpf) {
        assertThat(validator.isValid(cpf, ctx)).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar CPF com menos de 11 dígitos")
    void too_short() {
        assertThat(validator.isValid("123.456.789-0", ctx)).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar CPF com mais de 11 dígitos")
    void too_long() {
        assertThat(validator.isValid("529.982.247-250", ctx)).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar CPF com primeiro dígito verificador inválido")
    void invalid_first_check_digit() {
        // CPF base válido 529.982.247-25, alteramos o primeiro dígito verificador
        assertThat(validator.isValid("529.982.247-35", ctx)).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar CPF com segundo dígito verificador inválido")
    void invalid_second_check_digit() {
        // CPF base válido 529.982.247-25, alteramos o segundo dígito verificador
        assertThat(validator.isValid("529.982.247-26", ctx)).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar CPF com dígitos completamente errados")
    void completely_invalid() {
        assertThat(validator.isValid("123.456.789-10", ctx)).isFalse();
    }
}
