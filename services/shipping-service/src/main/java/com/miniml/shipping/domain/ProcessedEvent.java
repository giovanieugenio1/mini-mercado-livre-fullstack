package com.miniml.shipping.domain;

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
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt = OffsetDateTime.now();

    protected ProcessedEvent() {}

    public ProcessedEvent(UUID eventId, String eventType) {
        this.eventId   = eventId;
        this.eventType = eventType;
    }
}
