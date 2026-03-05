package com.miniml.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "customerId é obrigatório")
        UUID customerId,

        @NotEmpty(message = "O pedido deve ter ao menos 1 item")
        @Valid
        List<OrderItemRequest> items
) {}
