package com.miniml.payment.dto;

import com.miniml.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        String status,
        String paymentMethod,
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getOrderId(),
                p.getCustomerId(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus().name(),
                p.getPaymentMethod(),
                p.getFailureReason(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
