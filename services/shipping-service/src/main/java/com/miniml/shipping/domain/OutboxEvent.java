package com.miniml.shipping.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Version
    private long version;

    protected OutboxEvent() {}

    @PrePersist
    void prePersist() { createdAt = OffsetDateTime.now(); }

    public static OutboxEvent create(UUID aggregateId, String eventType, String payloadJson) {
        var e = new OutboxEvent();
        e.aggregateId = aggregateId;
        e.eventType   = eventType;
        e.payloadJson = payloadJson;
        return e;
    }

    public void markAsSent() {
        this.status = "SENT";
        this.sentAt = OffsetDateTime.now();
    }

    public void markAsFailed() { this.status = "FAILED"; }

    public UUID getId()            { return id; }
    public UUID getAggregateId()   { return aggregateId; }
    public String getEventType()   { return eventType; }
    public String getStatus()      { return status; }
    public String getPayloadJson() { return payloadJson; }
}
