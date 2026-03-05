package com.miniml.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Product() {}

    // ── Factory ───────────────────────────────────────────────

    public static Product create(String title, String description, BigDecimal price,
                                  int stock, String category, String imageUrl) {
        var p = new Product();
        p.id          = UUID.randomUUID();
        p.title       = title;
        p.description = description;
        p.price       = price;
        p.stock       = stock;
        p.category    = category;
        p.imageUrl    = imageUrl;
        p.active      = true;
        p.createdAt   = OffsetDateTime.now();
        p.updatedAt   = p.createdAt;
        return p;
    }

    // ── Business methods ──────────────────────────────────────

    public void update(String title, String description, BigDecimal price,
                       int stock, String category, String imageUrl) {
        this.title       = title;
        this.description = description;
        this.price       = price;
        this.stock       = stock;
        this.category    = category;
        this.imageUrl    = imageUrl;
        this.updatedAt   = OffsetDateTime.now();
    }

    public void deactivate() {
        this.active    = false;
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────

    public UUID getId()                  { return id; }
    public String getTitle()             { return title; }
    public String getDescription()       { return description; }
    public BigDecimal getPrice()         { return price; }
    public Integer getStock()            { return stock; }
    public String getCategory()          { return category; }
    public String getImageUrl()          { return imageUrl; }
    public boolean isActive()            { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
