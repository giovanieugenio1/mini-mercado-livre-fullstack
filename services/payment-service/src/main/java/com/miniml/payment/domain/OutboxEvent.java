package com.miniml.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    protected OutboxEvent() {}

    public static OutboxEvent create(UUID aggregateId, String eventType, String payloadJson) {
        var e = new OutboxEvent();
        e.id = UUID.randomUUID();
        e.aggregateId = aggregateId;
        e.eventType = eventType;
        e.payloadJson = payloadJson;
        e.status = "PENDING";
        e.createdAt = OffsetDateTime.now();
        return e;
    }

    public void markAsSent() {
        this.status = "SENT";
        this.sentAt = OffsetDateTime.now();
    }

    public void markAsFailed() {
        this.status = "FAILED";
    }

    public UUID getId()             { return id; }
    public UUID getAggregateId()    { return aggregateId; }
    public String getEventType()    { return eventType; }
    public String getPayloadJson()  { return payloadJson; }
    public String getStatus()       { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
