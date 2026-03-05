package com.miniml.order.domain;

public enum OrderStatus {
    // ── Happy path ────────────────────────────────────────────
    CREATED,
    PAYMENT_PENDING,
    PAID,
    INVENTORY_RESERVED,
    SHIPPING_CREATED,
    COMPLETED,

    // ── Failure states ────────────────────────────────────────
    PAYMENT_FAILED,
    INVENTORY_FAILED,
    SHIPPING_FAILED,
    CANCELLED
}
