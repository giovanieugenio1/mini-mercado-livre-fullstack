package com.miniml.notification.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String channel = "EMAIL";

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 20)
    private String status = "SENT";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Notification() {}

    @PrePersist
    void prePersist() { createdAt = OffsetDateTime.now(); }

    public static Notification of(UUID orderId, UUID customerId,
                                   String eventType, String subject, String body) {
        var n = new Notification();
        n.orderId    = orderId;
        n.customerId = customerId;
        n.eventType  = eventType;
        n.subject    = subject;
        n.body       = body;
        return n;
    }

    public UUID getId()                  { return id; }
    public UUID getOrderId()             { return orderId; }
    public UUID getCustomerId()          { return customerId; }
    public String getEventType()         { return eventType; }
    public String getChannel()           { return channel; }
    public String getSubject()           { return subject; }
    public String getBody()              { return body; }
    public String getStatus()            { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
