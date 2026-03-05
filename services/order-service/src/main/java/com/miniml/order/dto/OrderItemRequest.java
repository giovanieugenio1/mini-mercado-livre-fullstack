package com.miniml.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "productId é obrigatório")
        UUID productId,

        @NotBlank(message = "productTitle é obrigatório")
        String productTitle,

        @NotNull @Positive(message = "unitPrice deve ser positivo")
        BigDecimal unitPrice,

        @Positive(message = "quantity deve ser positivo")
        int quantity
) {}
