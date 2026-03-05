package com.miniml.inventory.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "reservation_item")
public class ReservationItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    protected ReservationItem() {}

    public ReservationItem(UUID productId, int quantityReserved) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.quantityReserved = quantityReserved;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public UUID getId()              { return id; }
    public UUID getProductId()       { return productId; }
    public int getQuantityReserved() { return quantityReserved; }
}
