package com.miniml.inventory.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // RESERVED | FAILED

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationItem> items = new ArrayList<>();

    protected Reservation() {}

    public static Reservation createReserved(UUID orderId, List<ReservationItem> items) {
        var r = new Reservation();
        r.id = UUID.randomUUID();
        r.orderId = orderId;
        r.status = "RESERVED";
        r.createdAt = OffsetDateTime.now();
        items.forEach(i -> i.setReservation(r));
        r.items = new ArrayList<>(items);
        return r;
    }

    public static Reservation createFailed(UUID orderId, String reason) {
        var r = new Reservation();
        r.id = UUID.randomUUID();
        r.orderId = orderId;
        r.status = "FAILED";
        r.failReason = reason;
        r.createdAt = OffsetDateTime.now();
        return r;
    }

    public UUID getId()            { return id; }
    public UUID getOrderId()       { return orderId; }
    public String getStatus()      { return status; }
    public String getFailReason()  { return failReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<ReservationItem> getItems() { return items; }
}
