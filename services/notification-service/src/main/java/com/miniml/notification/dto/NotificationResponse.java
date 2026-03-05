package com.miniml.notification.dto;

import com.miniml.notification.domain.Notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationId,
        UUID orderId,
        UUID customerId,
        String eventType,
        String channel,
        String subject,
        String body,
        String status,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getOrderId(),
                n.getCustomerId(),
                n.getEventType(),
                n.getChannel(),
                n.getSubject(),
                n.getBody(),
                n.getStatus(),
                n.getCreatedAt());
    }
}
