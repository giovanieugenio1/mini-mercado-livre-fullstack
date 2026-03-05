package com.miniml.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "Título é obrigatório") String title,
        String description,
        @NotNull @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero") BigDecimal price,
        @NotNull @Min(value = 0, message = "Estoque não pode ser negativo") Integer stock,
        @NotBlank(message = "Categoria é obrigatória") String category,
        String imageUrl
) {}
