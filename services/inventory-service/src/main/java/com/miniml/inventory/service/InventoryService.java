package com.miniml.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.inventory.domain.*;
import com.miniml.inventory.dto.PageResponse;
import com.miniml.inventory.dto.ProductStockResponse;
import com.miniml.inventory.dto.ReservationResponse;
import com.miniml.inventory.exception.ProductStockNotFoundException;
import com.miniml.inventory.exception.ReservationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductStockRepository productStockRepository;
    private final ReservationRepository reservationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.inventory.lock-ttl-seconds:30}")
    private long lockTtlSeconds;

    public InventoryService(ProductStockRepository productStockRepository,
            ReservationRepository reservationRepository,
            OutboxEventRepository outboxEventRepository,
            ProcessedEventRepository processedEventRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.productStockRepository = productStockRepository;
        this.reservationRepository = reservationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void reserveStock(UUID eventId, UUID orderId, UUID customerId,
            String itemsJson, UUID correlationId) {

        if (processedEventRepository.existsById(eventId)) {
            log.info("Evento {} já processado — ignorando (idempotência)", eventId);
            return;
        }

        List<ItemData> items = parseItems(itemsJson);
        List<String> locksAcquired = new ArrayList<>();

        try {
            // ── 1. Acquire Redis locks por produto ────────────────────────────
            for (ItemData item : items) {
                String lockKey = "inventory:lock:" + item.productId();
                String lockValue = orderId.toString();
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(lockTtlSeconds));

                if (!Boolean.TRUE.equals(acquired)) {
                    throw new IllegalStateException(
                            "Não foi possível adquirir lock para produto " + item.productId());
                }
                locksAcquired.add(lockKey);
                log.debug("Lock adquirido: {} orderId={}", lockKey, orderId);
            }

            // ── 2. Verificar disponibilidade de todos os itens ────────────────
            String failReason = null;
            for (ItemData item : items) {
                var stock = productStockRepository.findByIdForUpdate(item.productId())
                        .orElse(null);

                if (stock == null || !stock.hasStock(item.quantity())) {
                    int available = stock != null ? stock.getAvailableQty() : 0;
                    failReason = String.format(
                            "Estoque insuficiente para produto %s: disponível=%d solicitado=%d",
                            item.productId(), available, item.quantity());
                    break;
                }
            }

            // ── 3. Reservar ou falhar ─────────────────────────────────────────
            Reservation reservation;
            String outboxEventType;
            String payloadJson;

            if (failReason == null) {
                // Reserva todos os itens
                var reservedItems = new ArrayList<ReservationItem>();
                for (ItemData item : items) {
                    var stock = productStockRepository.findByIdForUpdate(item.productId()).get();
                    stock.reserve(item.quantity());
                    productStockRepository.save(stock);
                    reservedItems.add(new ReservationItem(item.productId(), item.quantity()));
                }
                reservation = Reservation.createReserved(orderId, reservedItems);
                outboxEventType = "inventory.reserved.v1";
                payloadJson = buildReservedPayload(reservation, customerId, correlationId);
                log.info("Estoque RESERVADO: orderId={} itens={}", orderId, items.size());
            } else {
                reservation = Reservation.createFailed(orderId, failReason);
                outboxEventType = "inventory.failed.v1";
                payloadJson = buildFailedPayload(orderId, customerId, failReason, correlationId);
                log.warn("Reserva FALHOU: orderId={} motivo={}", orderId, failReason);
            }

            reservationRepository.save(reservation);
            outboxEventRepository.save(
                    OutboxEvent.create(reservation.getId(), outboxEventType, payloadJson));
            processedEventRepository.save(
                    new ProcessedEvent(eventId, "payment.authorized.v1"));

        } finally {
            // ── 4. Libera todos os locks ──────────────────────────────────────
            locksAcquired.forEach(key -> {
                redisTemplate.delete(key);
                log.debug("Lock liberado: {}", key);
            });
        }
    }

    // ── Gestão de estoque (ROLE_ADMIN) ────────────────────────────────────────

    @Transactional
    public ProductStockResponse addStock(UUID productId, int availableQty) {
        if (productStockRepository.existsById(productId)) {
            throw new IllegalStateException("Estoque já cadastrado para o produto: " + productId);
        }
        var stock = ProductStock.of(productId, availableQty);
        productStockRepository.save(stock);
        log.info("Estoque criado: productId={} availableQty={}", productId, availableQty);
        return ProductStockResponse.from(stock);
    }

    @Transactional
    public ProductStockResponse updateStock(UUID productId, int newAvailableQty) {
        var stock = productStockRepository.findById(productId)
                .orElseThrow(() -> new ProductStockNotFoundException(productId));
        stock.restock(newAvailableQty);
        productStockRepository.save(stock);
        log.info("Estoque atualizado: productId={} availableQty={}", productId, newAvailableQty);
        return ProductStockResponse.from(stock);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public ProductStockResponse findStock(UUID productId) {
        return productStockRepository.findById(productId)
                .map(ProductStockResponse::from)
                .orElseThrow(() -> new ProductStockNotFoundException(productId));
    }

    public PageResponse<ProductStockResponse> listStock(int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by("productId").ascending());
        return PageResponse.from(productStockRepository.findAll(pageable),
                ProductStockResponse::from);
    }

    public ReservationResponse findReservation(UUID orderId) {
        return ReservationResponse.from(
                reservationRepository.findByOrderIdWithItems(orderId)
                        .orElseThrow(() -> new ReservationNotFoundException(orderId)));
    }

    // ── Builders de payload JSON ──────────────────────────────────────────────

    private String buildReservedPayload(Reservation reservation, UUID customerId, UUID correlationId) {
        try {
            var itemsList = reservation.getItems().stream()
                    .map(i -> Map.of(
                            "productId", i.getProductId().toString(),
                            "quantityReserved", i.getQuantityReserved()))
                    .toList();

            var envelope = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "eventType", "inventory.reserved.v1",
                    "occurredAt", OffsetDateTime.now().toString(),
                    "correlationId", correlationId.toString(),
                    "payload", Map.of(
                            "reservationId", reservation.getId().toString(),
                            "orderId", reservation.getOrderId().toString(),
                            "customerId", customerId.toString(),
                            "items", itemsList));
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar inventory.reserved.v1", e);
        }
    }

    private String buildFailedPayload(UUID orderId, UUID customerId,
            String reason, UUID correlationId) {
        try {
            var envelope = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "eventType", "inventory.failed.v1",
                    "occurredAt", OffsetDateTime.now().toString(),
                    "correlationId", correlationId.toString(),
                    "payload", Map.of(
                            "orderId", orderId.toString(),
                            "customerId", customerId.toString(),
                            "reason", reason));
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar inventory.failed.v1", e);
        }
    }

    private List<ItemData> parseItems(String itemsJson) {
        try {
            JsonNode node = objectMapper.readTree(itemsJson);
            var items = new ArrayList<ItemData>();
            node.forEach(item -> items.add(new ItemData(
                    UUID.fromString(item.get("productId").asText()),
                    item.get("quantity").asInt())));
            return items;
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato inválido de items JSON: " + itemsJson, e);
        }
    }

    public record ItemData(UUID productId, int quantity) {
    }
}
