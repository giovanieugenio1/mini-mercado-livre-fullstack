package com.miniml.shipping.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.shipping.service.ShippingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consome inventory.reserved.v1 e cria o envio.
 */
@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final ShippingService shippingService;
    private final ObjectMapper objectMapper;

    public InventoryEventConsumer(ShippingService shippingService, ObjectMapper objectMapper) {
        this.shippingService = shippingService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "inventory.reserved.v1", groupId = "shipping-service")
    public void onInventoryReserved(String message) {
        try {
            JsonNode envelope = objectMapper.readTree(message);
            UUID eventId = UUID.fromString(envelope.get("eventId").asText());
            UUID correlationId = UUID.fromString(envelope.get("correlationId").asText());
            JsonNode payload = envelope.get("payload");
            UUID orderId = UUID.fromString(payload.get("orderId").asText());
            UUID customerId = UUID.fromString(payload.get("customerId").asText());

            log.info("Evento recebido: inventory.reserved.v1 eventId={} orderId={}",
                    eventId, orderId);

            shippingService.processInventoryReserved(eventId, orderId, customerId, correlationId);

        } catch (Exception e) {
            log.error("Erro ao processar inventory.reserved.v1: {} | msg={}",
                    e.getMessage(), message, e);
            throw new RuntimeException("Falha ao processar inventory.reserved.v1", e);
        }
    }
}
