package com.miniml.notification.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consome todos os eventos de domínio relevantes e registra notificações
 * (mock).
 */
@Component
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public DomainEventConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    // ── Pedidos ───────────────────────────────────────────────────────────────

    @KafkaListener(topics = "order.created.v1", groupId = "notification-service")
    public void onOrderCreated(String message) {
        process(message, "order.created.v1",
                "Pedido criado com sucesso!",
                (payload, orderId) -> "Seu pedido %s foi recebido e está sendo processado.".formatted(orderId));
    }

    @KafkaListener(topics = "order.completed.v1", groupId = "notification-service")
    public void onOrderCompleted(String message) {
        process(message, "order.completed.v1",
                "Pedido concluído!",
                (payload, orderId) -> "Parabéns! Seu pedido %s foi concluído com sucesso.".formatted(orderId));
    }

    // ── Pagamentos ────────────────────────────────────────────────────────────

    @KafkaListener(topics = "payment.authorized.v1", groupId = "notification-service")
    public void onPaymentAuthorized(String message) {
        process(message, "payment.authorized.v1",
                "Pagamento aprovado!",
                (payload, orderId) -> "O pagamento do seu pedido %s foi aprovado.".formatted(orderId));
    }

    @KafkaListener(topics = "payment.failed.v1", groupId = "notification-service")
    public void onPaymentFailed(String message) {
        process(message, "payment.failed.v1",
                "Pagamento recusado",
                (payload, orderId) -> {
                    String reason = payload.path("reason").asText("Motivo não informado");
                    return "O pagamento do pedido %s foi recusado. Motivo: %s".formatted(orderId, reason);
                });
    }

    // ── Estoque ───────────────────────────────────────────────────────────────

    @KafkaListener(topics = "inventory.reserved.v1", groupId = "notification-service")
    public void onInventoryReserved(String message) {
        process(message, "inventory.reserved.v1",
                "Itens reservados",
                (payload, orderId) -> "Os itens do pedido %s foram reservados no estoque.".formatted(orderId));
    }

    @KafkaListener(topics = "inventory.failed.v1", groupId = "notification-service")
    public void onInventoryFailed(String message) {
        process(message, "inventory.failed.v1",
                "Estoque insuficiente",
                (payload, orderId) -> {
                    String reason = payload.path("reason").asText("Produto indisponível");
                    return "Não foi possível reservar os itens do pedido %s. Motivo: %s".formatted(orderId, reason);
                });
    }

    // ── Envio ─────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "shipping.created.v1", groupId = "notification-service")
    public void onShippingCreated(String message) {
        process(message, "shipping.created.v1",
                "Pedido enviado!",
                (payload, orderId) -> {
                    String code = payload.path("trackingCode").asText("N/A");
                    return "Seu pedido %s foi enviado! Código de rastreio: %s. Previsão: 5 dias úteis."
                            .formatted(orderId, code);
                });
    }

    @KafkaListener(topics = "shipping.failed.v1", groupId = "notification-service")
    public void onShippingFailed(String message) {
        process(message, "shipping.failed.v1",
                "Problema no envio",
                (payload, orderId) -> "Houve um problema ao enviar o pedido %s. Nossa equipe entrará em contato."
                        .formatted(orderId));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    @FunctionalInterface
    interface BodyBuilder {
        String build(JsonNode payload, String orderId);
    }

    private void process(String message, String eventType, String subject, BodyBuilder bodyBuilder) {
        try {
            JsonNode envelope = objectMapper.readTree(message);
            UUID eventId = UUID.fromString(envelope.get("eventId").asText());
            JsonNode payload = envelope.get("payload");
            String orderIdStr = payload.path("orderId").asText(null);
            UUID orderId = orderIdStr != null ? UUID.fromString(orderIdStr) : null;
            UUID customerId = UUID.fromString(payload.get("customerId").asText());
            String body = bodyBuilder.build(payload, orderIdStr != null ? orderIdStr : "N/A");

            log.info("Evento recebido: {} orderId={} customerId={}", eventType, orderId, customerId);

            notificationService.notify(eventId, eventType, orderId, customerId, subject, body);

        } catch (Exception e) {
            log.error("Erro ao processar evento {}: {} | msg={}", eventType, e.getMessage(), message, e);
            throw new RuntimeException("Falha ao processar " + eventType, e);
        }
    }
}
