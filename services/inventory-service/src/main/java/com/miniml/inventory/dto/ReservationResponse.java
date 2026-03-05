package com.miniml.inventory.dto;

import com.miniml.inventory.domain.Reservation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID orderId,
        String status,
        String failReason,
        List<ReservationItemResponse> items,
        OffsetDateTime createdAt
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getOrderId(),
                r.getStatus(),
                r.getFailReason(),
                r.getItems().stream()
                        .map(i -> new ReservationItemResponse(
                                i.getProductId(), i.getQuantityReserved()))
                        .toList(),
                r.getCreatedAt());
    }

    public record ReservationItemResponse(UUID productId, int quantityReserved) {}
}
