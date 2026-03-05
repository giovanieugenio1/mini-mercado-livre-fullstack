package com.miniml.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_title", nullable = false, length = 255)
    private String productTitle;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    protected OrderItem() {}

    public static OrderItem create(Order order, UUID productId, String productTitle,
                                   BigDecimal unitPrice, int quantity) {
        var item = new OrderItem();
        item.id = UUID.randomUUID();
        item.order = order;
        item.productId = productId;
        item.productTitle = productTitle;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return item;
    }

    public UUID getId()             { return id; }
    public UUID getProductId()      { return productId; }
    public String getProductTitle() { return productTitle; }
    public BigDecimal getUnitPrice(){ return unitPrice; }
    public Integer getQuantity()    { return quantity; }
    public BigDecimal getLineTotal(){ return lineTotal; }
}
