package com.miniml.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddStockRequest(
        @NotNull(message = "productId é obrigatório") UUID productId,
        @NotNull @Min(value = 0, message = "Quantidade não pode ser negativa") Integer availableQty
) {}
