package com.miniml.inventory.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consome payment.authorized.v1 e reserva estoque.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment.authorized.v1", groupId = "inventory-service")
    public void onPaymentAuthorized(String message) {
        try {
            JsonNode envelope = objectMapper.readTree(message);

            UUID eventId = UUID.fromString(envelope.get("eventId").asText());
            UUID correlationId = UUID.fromString(envelope.get("correlationId").asText());
            JsonNode payload = envelope.get("payload");

            UUID orderId = UUID.fromString(payload.get("orderId").asText());
            UUID customerId = UUID.fromString(payload.get("customerId").asText());
            String itemsJson = objectMapper.writeValueAsString(payload.get("items"));

            log.info("Evento recebido: payment.authorized.v1 eventId={} orderId={}",
                    eventId, orderId);

            inventoryService.reserveStock(eventId, orderId, customerId, itemsJson, correlationId);

        } catch (Exception e) {
            log.error("Erro ao processar payment.authorized.v1: {} | mensagem={}",
                    e.getMessage(), message, e);
            throw new RuntimeException("Falha ao processar payment.authorized.v1", e);
        }
    }
}
