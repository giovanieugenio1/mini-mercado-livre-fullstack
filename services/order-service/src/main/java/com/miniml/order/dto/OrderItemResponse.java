package com.miniml.order.dto;

import com.miniml.order.domain.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productTitle,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductTitle(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal()
        );
    }
}
