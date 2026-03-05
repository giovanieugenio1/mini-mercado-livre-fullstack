package com.miniml.shipping.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipping")
public class Shipping {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "tracking_code", length = 50)
    private String trackingCode;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    protected Shipping() {}

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public static Shipping createShipping(UUID orderId, UUID customerId, String trackingCode) {
        var s = new Shipping();
        s.orderId = orderId;
        s.customerId = customerId;
        s.status = "CREATED";
        s.trackingCode = trackingCode;
        return s;
    }

    public static Shipping createFailed(UUID orderId, UUID customerId, String reason) {
        var s = new Shipping();
        s.orderId = orderId;
        s.customerId = customerId;
        s.status = "FAILED";
        s.failReason = reason;
        return s;
    }

    public UUID getId()                  { return id; }
    public UUID getOrderId()             { return orderId; }
    public UUID getCustomerId()          { return customerId; }
    public String getStatus()            { return status; }
    public String getTrackingCode()      { return trackingCode; }
    public String getFailReason()        { return failReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion()             { return version; }
}
