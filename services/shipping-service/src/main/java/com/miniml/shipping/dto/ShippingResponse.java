package com.miniml.shipping.dto;

import com.miniml.shipping.domain.Shipping;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ShippingResponse(
        UUID shippingId,
        UUID orderId,
        UUID customerId,
        String status,
        String trackingCode,
        String failReason,
        OffsetDateTime createdAt
) {
    public static ShippingResponse from(Shipping s) {
        return new ShippingResponse(
                s.getId(),
                s.getOrderId(),
                s.getCustomerId(),
                s.getStatus(),
                s.getTrackingCode(),
                s.getFailReason(),
                s.getCreatedAt());
    }
}
