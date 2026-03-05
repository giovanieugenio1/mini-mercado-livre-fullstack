package com.miniml.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    protected Order() {}

    // ── Factory ───────────────────────────────────────────────

    public static Order create(UUID customerId, List<OrderItemData> itemDataList) {
        var order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.status = OrderStatus.CREATED;
        order.currency = "BRL";
        order.createdAt = OffsetDateTime.now();
        order.updatedAt = OffsetDateTime.now();

        // Cria itens referenciando este pedido
        for (OrderItemData data : itemDataList) {
            var item = OrderItem.create(order, data.productId(), data.productTitle(),
                                        data.unitPrice(), data.quantity());
            order.items.add(item);
        }

        order.totalAmount = order.items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Registra criação no histórico
        order.statusHistory.add(
                OrderStatusHistory.record(order, null, OrderStatus.CREATED, "Pedido criado"));

        return order;
    }

    // ── Business methods ──────────────────────────────────────

    public void transition(OrderStatus newStatus, String reason) {
        statusHistory.add(OrderStatusHistory.record(this, this.status, newStatus, reason));
        this.status = newStatus;
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────

    public UUID getId()                         { return id; }
    public UUID getCustomerId()                 { return customerId; }
    public OrderStatus getStatus()              { return status; }
    public BigDecimal getTotalAmount()           { return totalAmount; }
    public String getCurrency()                 { return currency; }
    public OffsetDateTime getCreatedAt()        { return createdAt; }
    public OffsetDateTime getUpdatedAt()        { return updatedAt; }
    public List<OrderItem> getItems()           { return Collections.unmodifiableList(items); }
    public List<OrderStatusHistory> getStatusHistory() { return Collections.unmodifiableList(statusHistory); }

    // ─── Value object para criação de itens ──────────────────
    public record OrderItemData(
            UUID productId,
            String productTitle,
            BigDecimal unitPrice,
            int quantity) {}
}
