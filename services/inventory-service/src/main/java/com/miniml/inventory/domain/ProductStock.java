package com.miniml.inventory.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "product_stock")
public class ProductStock {

    @Id
    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProductStock() {
    }

    public static ProductStock of(UUID productId, int availableQty) {
        var s = new ProductStock();
        s.productId = productId;
        s.availableQty = availableQty;
        s.reservedQty = 0;
        return s;
    }

    public void reserve(int qty) {
        if (availableQty < qty) {
            throw new IllegalStateException(
                    "Estoque insuficiente para produto " + productId +
                            ": disponível=" + availableQty + " solicitado=" + qty);
        }
        this.availableQty -= qty;
        this.reservedQty += qty;
    }

    public void release(int qty) {
        this.reservedQty -= qty;
        this.availableQty += qty;
    }

    public boolean hasStock(int qty) {
        return availableQty >= qty;
    }

    public void restock(int newAvailableQty) {
        if (newAvailableQty < 0)
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        this.availableQty = newAvailableQty;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getAvailableQty() {
        return availableQty;
    }

    public int getReservedQty() {
        return reservedQty;
    }

    public long getVersion() {
        return version;
    }
}
