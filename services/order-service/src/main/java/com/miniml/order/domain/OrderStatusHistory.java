package com.miniml.order.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 50)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 50)
    private OrderStatus toStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    protected OrderStatusHistory() {}

    public static OrderStatusHistory record(Order order, OrderStatus fromStatus,
                                            OrderStatus toStatus, String reason) {
        var h = new OrderStatusHistory();
        h.id = UUID.randomUUID();
        h.order = order;
        h.fromStatus = fromStatus;
        h.toStatus = toStatus;
        h.reason = reason;
        h.changedAt = OffsetDateTime.now();
        return h;
    }

    public UUID getId()               { return id; }
    public OrderStatus getFromStatus(){ return fromStatus; }
    public OrderStatus getToStatus()  { return toStatus; }
    public String getReason()         { return reason; }
    public OffsetDateTime getChangedAt() { return changedAt; }
}
