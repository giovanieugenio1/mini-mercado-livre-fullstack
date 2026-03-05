package com.miniml.order.dto;

import com.miniml.order.domain.Order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        String status,
        BigDecimal totalAmount,
        String currency,
        List<OrderItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
