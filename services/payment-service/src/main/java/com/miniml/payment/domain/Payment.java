package com.miniml.payment.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // JSON dos itens do pedido — repassado ao inventory-service via evento
    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Payment() {}

    public static Payment create(UUID orderId, UUID customerId, BigDecimal amount, String itemsJson) {
        var p = new Payment();
        p.id = UUID.randomUUID();
        p.orderId = orderId;
        p.customerId = customerId;
        p.amount = amount;
        p.currency = "BRL";
        p.status = PaymentStatus.PENDING;
        p.paymentMethod = "CREDIT_CARD"; // mock
        p.itemsJson = itemsJson;
        p.createdAt = OffsetDateTime.now();
        p.updatedAt = p.createdAt;
        return p;
    }

    public void authorize() {
        this.status = PaymentStatus.AUTHORIZED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId()              { return id; }
    public UUID getOrderId()         { return orderId; }
    public UUID getCustomerId()      { return customerId; }
    public BigDecimal getAmount()    { return amount; }
    public String getCurrency()      { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getFailureReason() { return failureReason; }
    public String getItemsJson()     { return itemsJson; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
