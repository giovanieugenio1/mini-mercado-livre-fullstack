package com.miniml.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.payment.domain.*;
import com.miniml.payment.dto.PageResponse;
import com.miniml.payment.dto.PaymentResponse;
import com.miniml.payment.exception.PaymentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.max-authorized-amount:50000.00}")
    private BigDecimal maxAuthorizedAmount;

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxEventRepository outboxEventRepository,
                          ProcessedEventRepository processedEventRepository,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    // ── Processamento de pagamento (acionado pelo consumer Kafka) ─────────────

    /**
     * Processa o evento order.created.v1, aplica lógica de aprovação mock
     * e publica payment.authorized.v1 ou payment.failed.v1 via Outbox.
     *
     * Idempotente: se eventId já foi processado, ignora silenciosamente.
     */
    @Transactional
    public void processOrderCreated(UUID eventId, UUID orderId, UUID customerId,
                                    BigDecimal totalAmount, String itemsJson, UUID correlationId) {

        if (processedEventRepository.existsById(eventId)) {
            log.info("Evento {} já processado — ignorando (idempotência)", eventId);
            return;
        }

        var payment = Payment.create(orderId, customerId, totalAmount, itemsJson);

        // ── Lógica mock de aprovação ──────────────────────────────────────────
        // Regra: valor <= maxAuthorizedAmount → AUTHORIZED, caso contrário FAILED.
        // Em produção seria integração real com adquirente (Cielo, Stone, Stripe...).
        String outboxEventType;
        String payloadJson;

        if (totalAmount.compareTo(maxAuthorizedAmount) <= 0) {
            payment.authorize();
            outboxEventType = "payment.authorized.v1";
            payloadJson = buildAuthorizedPayload(payment, correlationId);
            log.info("Pagamento AUTORIZADO: orderId={} valor={}", orderId, totalAmount);
        } else {
            String reason = String.format(
                    "Valor R$ %.2f excede o limite de R$ %.2f",
                    totalAmount, maxAuthorizedAmount);
            payment.fail(reason);
            outboxEventType = "payment.failed.v1";
            payloadJson = buildFailedPayload(payment, reason, correlationId);
            log.warn("Pagamento RECUSADO: orderId={} valor={} motivo={}", orderId, totalAmount, reason);
        }

        paymentRepository.save(payment);

        // Outbox + idempotência gravados na mesma transação
        outboxEventRepository.save(
                OutboxEvent.create(payment.getId(), outboxEventType, payloadJson));
        processedEventRepository.save(
                new ProcessedEvent(eventId, "order.created.v1"));
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public PaymentResponse findById(UUID id) {
        return PaymentResponse.from(
                paymentRepository.findById(id)
                        .orElseThrow(() -> new PaymentNotFoundException("id", id.toString())));
    }

    public PaymentResponse findByOrderId(UUID orderId) {
        return PaymentResponse.from(
                paymentRepository.findByOrderId(orderId)
                        .orElseThrow(() -> new PaymentNotFoundException("orderId", orderId.toString())));
    }

    public PageResponse<PaymentResponse> list(UUID customerId, PaymentStatus status,
                                              int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by("createdAt").descending());

        final Page<Payment> result;
        if (customerId != null && status != null) {
            result = paymentRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        } else if (customerId != null) {
            result = paymentRepository.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            result = paymentRepository.findByStatus(status, pageable);
        } else {
            result = paymentRepository.findAll(pageable);
        }

        return PageResponse.from(result, PaymentResponse::from);
    }

    // ── Builders de payload JSON ──────────────────────────────────────────────

    private String buildAuthorizedPayload(Payment payment, UUID correlationId) {
        try {
            // items_json já é um array JSON — deserializa para incluir no envelope
            var itemsNode = objectMapper.readTree(
                    payment.getItemsJson() != null ? payment.getItemsJson() : "[]");

            var payloadNode = objectMapper.createObjectNode()
                    .put("paymentId",      payment.getId().toString())
                    .put("orderId",        payment.getOrderId().toString())
                    .put("customerId",     payment.getCustomerId().toString())
                    .put("amount",         payment.getAmount().toPlainString())
                    .put("currency",       payment.getCurrency())
                    .put("paymentMethod",  payment.getPaymentMethod());
            payloadNode.set("items", itemsNode);

            var envelope = objectMapper.createObjectNode()
                    .put("eventId",       UUID.randomUUID().toString())
                    .put("eventType",     "payment.authorized.v1")
                    .put("occurredAt",    OffsetDateTime.now().toString())
                    .put("correlationId", correlationId.toString());
            envelope.set("payload", payloadNode);

            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao serializar payment.authorized.v1", e);
        }
    }

    private String buildFailedPayload(Payment payment, String reason, UUID correlationId) {
        try {
            var envelope = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "eventType", "payment.failed.v1",
                    "occurredAt", OffsetDateTime.now().toString(),
                    "correlationId", correlationId.toString(),
                    "payload", Map.of(
                            "paymentId", payment.getId().toString(),
                            "orderId", payment.getOrderId().toString(),
                            "customerId", payment.getCustomerId().toString(),
                            "reason", reason));
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar payment.failed.v1", e);
        }
    }
}
