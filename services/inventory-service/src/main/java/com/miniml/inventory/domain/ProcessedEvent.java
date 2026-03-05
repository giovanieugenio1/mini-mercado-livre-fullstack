package com.miniml.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", updatable = false, nullable = false)
    private OffsetDateTime processedAt;

    protected ProcessedEvent() {}

    public ProcessedEvent(UUID eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = OffsetDateTime.now();
    }

    public UUID getEventId() { return eventId; }
}
