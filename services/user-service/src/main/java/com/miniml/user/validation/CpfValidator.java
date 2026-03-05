package com.miniml.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida CPF usando o algoritmo oficial dos dígitos verificadores.
 * Rejeita sequências uniformes (111.111.111-11, etc.).
 */
public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return false;

        String digits = value.replaceAll("[^0-9]", "");

        if (digits.length() != 11) return false;

        // Rejeita sequências uniformes
        if (digits.chars().distinct().count() == 1) return false;

        return checkDigit(digits, 9) && checkDigit(digits, 10);
    }

    private boolean checkDigit(String digits, int position) {
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * (position + 1 - i);
        }
        int remainder = (sum * 10) % 11;
        if (remainder == 10) remainder = 0;
        return remainder == Character.getNumericValue(digits.charAt(position));
    }
}
