package com.miniml.notification.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;

    protected ProcessedEvent() {}

    public ProcessedEvent(UUID eventId, String eventType) {
        this.eventId     = eventId;
        this.eventType   = eventType;
        this.processedAt = OffsetDateTime.now();
    }

    public UUID getEventId() { return eventId; }
}
