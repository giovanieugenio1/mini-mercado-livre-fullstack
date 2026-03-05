package com.miniml.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.order.domain.*;
import com.miniml.order.dto.*;
import com.miniml.order.exception.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        OutboxEventRepository outboxEventRepository,
                        ProcessedEventRepository processedEventRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    // ── Criação de pedido ─────────────────────────────────────

    /**
     * Cria o pedido e grava o OutboxEvent NA MESMA TRANSAÇÃO.
     * Garantia: ambos são persistidos ou nenhum (atomicidade).
     * O OutboxPublisherService publica o evento no Kafka assincronamente.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        var itemDataList = request.items().stream()
                .map(i -> new Order.OrderItemData(
                        i.productId(), i.productTitle(), i.unitPrice(), i.quantity()))
                .toList();

        var order = Order.create(request.customerId(), itemDataList);
        orderRepository.save(order);

        // Outbox: grava evento na mesma transação
        var outboxEvent = OutboxEvent.create(
                order.getId(),
                "order.created.v1",
                buildOrderCreatedPayload(order));
        outboxEventRepository.save(outboxEvent);

        log.info("Pedido criado: id={} customer={} total={}",
                order.getId(), order.getCustomerId(), order.getTotalAmount());

        return OrderResponse.from(order);
    }

    // ── Consultas ─────────────────────────────────────────────

    public OrderResponse findById(UUID id) {
        var order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return OrderResponse.from(order);
    }

    public PageResponse<OrderResponse> list(UUID customerId, OrderStatus status,
                                             int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by("createdAt").descending());

        final Page<Order> result;
        if (customerId != null && status != null) {
            result = orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        } else if (customerId != null) {
            result = orderRepository.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            result = orderRepository.findByStatus(status, pageable);
        } else {
            result = orderRepository.findAll(pageable);
        }

        return PageResponse.from(result, OrderResponse::from);
    }

    // ── Cancelamento (chamado pelo controller) ────────────────

    /**
     * Cancela um pedido. Permitido apenas no status CREATED (antes do pagamento).
     * Retorna 409 Conflict via GlobalExceptionHandler se já estiver em processamento.
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        var order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Pedido só pode ser cancelado no status CREATED. Status atual: " + order.getStatus());
        }

        order.transition(OrderStatus.CANCELLED, "Cancelado pelo cliente");
        orderRepository.save(order);

        log.info("Pedido cancelado: id={}", orderId);
        return OrderResponse.from(order);
    }

    // ── Atualização de status (chamado pelo consumer Kafka) ───

    /**
     * Idempotente: verifica se o eventId já foi processado antes de agir.
     * Salva ProcessedEvent e atualiza status em UMA transação.
     */
    @Transactional
    public void handleExternalEvent(UUID eventId, UUID orderId,
                                     OrderStatus newStatus, String reason) {
        if (processedEventRepository.existsById(eventId)) {
            log.info("Evento {} já processado — ignorando (idempotência)", eventId);
            return;
        }

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.transition(newStatus, reason);
        orderRepository.save(order);

        // Se o pedido foi concluído via shipping.created → publicar order.completed.v1
        if (newStatus == OrderStatus.SHIPPING_CREATED) {
            order.transition(OrderStatus.COMPLETED, "Envio criado — pedido concluído");
            orderRepository.save(order);

            var outbox = OutboxEvent.create(
                    order.getId(), "order.completed.v1",
                    buildOrderCompletedPayload(order));
            outboxEventRepository.save(outbox);

            log.info("Pedido concluído: id={}", order.getId());
        }

        processedEventRepository.save(new ProcessedEvent(eventId));
        log.info("Pedido {} → {} (evento={})", orderId, newStatus, eventId);
    }

    // ── Builders de payload JSON ──────────────────────────────

    private String buildOrderCreatedPayload(Order order) {
        try {
            var itemsList = order.getItems().stream()
                    .map(i -> Map.of(
                            "productId", i.getProductId().toString(),
                            "productTitle", i.getProductTitle(),
                            "unitPrice", i.getUnitPrice(),
                            "quantity", i.getQuantity()))
                    .toList();

            var envelope = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "eventType", "order.created.v1",
                    "occurredAt", OffsetDateTime.now().toString(),
                    "correlationId", order.getId().toString(),
                    "payload", Map.of(
                            "orderId", order.getId().toString(),
                            "customerId", order.getCustomerId().toString(),
                            "totalAmount", order.getTotalAmount(),
                            "currency", order.getCurrency(),
                            "items", itemsList));

            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar order.created.v1", e);
        }
    }

    private String buildOrderCompletedPayload(Order order) {
        try {
            var envelope = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "eventType", "order.completed.v1",
                    "occurredAt", OffsetDateTime.now().toString(),
                    "correlationId", order.getId().toString(),
                    "payload", Map.of(
                            "orderId", order.getId().toString(),
                            "customerId", order.getCustomerId().toString(),
                            "totalAmount", order.getTotalAmount()));

            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar order.completed.v1", e);
        }
    }
}
