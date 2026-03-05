package com.miniml.payment.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Consome order.created.v1 e aciona o processamento de pagamento.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order.created.v1", groupId = "payment-service")
    public void onOrderCreated(String message) {
        try {
            JsonNode envelope = objectMapper.readTree(message);

            UUID eventId = UUID.fromString(envelope.get("eventId").asText());
            UUID correlationId = UUID.fromString(envelope.get("correlationId").asText());
            JsonNode payload = envelope.get("payload");

            UUID orderId = UUID.fromString(payload.get("orderId").asText());
            UUID customerId = UUID.fromString(payload.get("customerId").asText());
            BigDecimal totalAmount = new BigDecimal(payload.get("totalAmount").asText());

            // Extrai items para repassar ao inventory-service via payment.authorized.v1
            String itemsJson = objectMapper.writeValueAsString(payload.get("items"));

            log.info("Evento recebido: order.created.v1 eventId={} orderId={} amount={}",
                    eventId, orderId, totalAmount);

            paymentService.processOrderCreated(eventId, orderId, customerId,
                    totalAmount, itemsJson, correlationId);

        } catch (Exception e) {
            // Log e deixa o Kafka reprocessar (sem commit do offset em caso de erro não
            // tratado)
            log.error("Erro ao processar order.created.v1: {} | mensagem={}",
                    e.getMessage(), message, e);
            throw new RuntimeException("Falha ao processar order.created.v1", e);
        }
    }
}
