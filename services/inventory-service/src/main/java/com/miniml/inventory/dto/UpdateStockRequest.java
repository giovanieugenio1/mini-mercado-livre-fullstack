package com.miniml.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateStockRequest(
        @NotNull @Min(value = 0, message = "Quantidade não pode ser negativa") Integer availableQty
) {}
