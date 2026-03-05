package com.miniml.shipping.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.shipping.domain.*;
import com.miniml.shipping.dto.PageResponse;
import com.miniml.shipping.dto.ShippingResponse;
import com.miniml.shipping.exception.ShippingNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    private final ShippingRepository shippingRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public ShippingService(ShippingRepository shippingRepository,
                           OutboxEventRepository outboxEventRepository,
                           ProcessedEventRepository processedEventRepository,
                           ObjectMapper objectMapper) {
        this.shippingRepository      = shippingRepository;
        this.outboxEventRepository   = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper            = objectMapper;
    }

    // ── Processamento do evento inventory.reserved.v1 ─────────────────────────

    /**
     * Cria o envio (mock — sempre bem-sucedido) e publica shipping.created.v1 via Outbox.
     * Idempotente: verifica processedEventRepository antes de agir.
     */
    @Transactional
    public void processInventoryReserved(UUID eventId, UUID orderId,
                                         UUID customerId, UUID correlationId) {
        if (processedEventRepository.existsById(eventId)) {
            log.info("Evento {} já processado — ignorando (idempotência)", eventId);
            return;
        }

        String trackingCode = generateTrackingCode();
        var shipping = Shipping.createShipping(orderId, customerId, trackingCode);
        shippingRepository.save(shipping);

        String payloadJson = buildCreatedPayload(shipping, correlationId);
        outboxEventRepository.save(
                OutboxEvent.create(shipping.getId(), "shipping.created.v1", payloadJson));
        processedEventRepository.save(
                new ProcessedEvent(eventId, "inventory.reserved.v1"));

        log.info("Envio CRIADO: orderId={} trackingCode={}", orderId, trackingCode);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public ShippingResponse findByOrderId(UUID orderId) {
        return shippingRepository.findByOrderId(orderId)
                .map(ShippingResponse::from)
                .orElseThrow(() -> new ShippingNotFoundException(orderId));
    }

    public PageResponse<ShippingResponse> listShippings(int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by("createdAt").descending());
        return PageResponse.from(
                shippingRepository.findAll(pageable), ShippingResponse::from);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateTrackingCode() {
        return "MINIML" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }

    private String buildCreatedPayload(Shipping shipping, UUID correlationId) {
        try {
            var envelope = Map.of(
                    "eventId",       UUID.randomUUID().toString(),
                    "eventType",     "shipping.created.v1",
                    "occurredAt",    OffsetDateTime.now().toString(),
                    "correlationId", correlationId.toString(),
                    "payload", Map.of(
                            "shippingId",            shipping.getId().toString(),
                            "orderId",               shipping.getOrderId().toString(),
                            "customerId",            shipping.getCustomerId().toString(),
                            "trackingCode",          shipping.getTrackingCode(),
                            "estimatedDeliveryDays", 5));
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar shipping.created.v1", e);
        }
    }
}
