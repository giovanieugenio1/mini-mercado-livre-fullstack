package com.miniml.order.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.order.domain.OrderStatus;
import com.miniml.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumidor de eventos Kafka.
 * Cada método é idempotente: chama OrderService.handleExternalEvent()
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    // ── Pagamento ─────────────────────────────────────────────

    @KafkaListener(topics = "payment.authorized.v1", groupId = "order-service")
    public void onPaymentAuthorized(String message) {
        process(message, OrderStatus.PAID, "Pagamento autorizado");
    }

    @KafkaListener(topics = "payment.failed.v1", groupId = "order-service")
    public void onPaymentFailed(String message) {
        process(message, OrderStatus.PAYMENT_FAILED, extractReason(message, "Pagamento recusado"));
    }

    // ── Estoque ───────────────────────────────────────────────

    @KafkaListener(topics = "inventory.reserved.v1", groupId = "order-service")
    public void onInventoryReserved(String message) {
        process(message, OrderStatus.INVENTORY_RESERVED, "Estoque reservado");
    }

    @KafkaListener(topics = "inventory.failed.v1", groupId = "order-service")
    public void onInventoryFailed(String message) {
        process(message, OrderStatus.INVENTORY_FAILED, extractReason(message, "Estoque insuficiente"));
    }

    // ── Envio ─────────────────────────────────────────────────

    @KafkaListener(topics = "shipping.created.v1", groupId = "order-service")
    public void onShippingCreated(String message) {
        process(message, OrderStatus.SHIPPING_CREATED, "Envio criado");
    }

    @KafkaListener(topics = "shipping.failed.v1", groupId = "order-service")
    public void onShippingFailed(String message) {
        process(message, OrderStatus.SHIPPING_FAILED, extractReason(message, "Falha no envio"));
    }

    // ── Helper ────────────────────────────────────────────────

    private void process(String message, OrderStatus newStatus, String reason) {
        try {
            JsonNode envelope = objectMapper.readTree(message);
            UUID eventId = UUID.fromString(envelope.get("eventId").asText());
            UUID orderId = UUID.fromString(envelope.get("payload").get("orderId").asText());

            log.info("Evento recebido: tipo={} orderId={} eventId={}",
                    newStatus, orderId, eventId);

            orderService.handleExternalEvent(eventId, orderId, newStatus, reason);

        } catch (Exception e) {
            log.error("Erro ao processar evento Kafka — status={} msg={}", newStatus, message, e);
            // Lança exceção para Spring Kafka fazer retry / encaminhar ao DLT
            throw new RuntimeException("Falha ao processar evento: " + newStatus, e);
        }
    }

    private String extractReason(String message, String defaultReason) {
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode reason = root.path("payload").path("reason");
            return reason.isMissingNode() ? defaultReason : reason.asText(defaultReason);
        } catch (Exception e) {
            return defaultReason;
        }
    }
}
