package com.miniml.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Idempotência — registra eventos Kafka já processados.
 * Antes de processar qualquer evento consumido, verifica se o eventId já está aqui.
 */
@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    protected ProcessedEvent() {}

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = OffsetDateTime.now();
    }

    public UUID getEventId() { return eventId; }
}
