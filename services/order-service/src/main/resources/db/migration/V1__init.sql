-- =============================================================
-- order-service — V1: Schema inicial
-- =============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── Pedidos ──────────────────────────────────────────────────
CREATE TABLE orders (
    id           UUID          NOT NULL DEFAULT gen_random_uuid(),
    customer_id  UUID          NOT NULL,
    status       VARCHAR(50)   NOT NULL DEFAULT 'CREATED',
    total_amount NUMERIC(19,2) NOT NULL,
    currency     VARCHAR(3)    NOT NULL DEFAULT 'BRL',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT chk_orders_status CHECK (status IN (
        'CREATED', 'PAYMENT_PENDING', 'PAID',
        'INVENTORY_RESERVED', 'SHIPPING_CREATED', 'COMPLETED',
        'PAYMENT_FAILED', 'INVENTORY_FAILED', 'SHIPPING_FAILED', 'CANCELLED'
    )),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount >= 0)
);

-- ─── Itens do pedido ─────────────────────────────────────────
CREATE TABLE order_item (
    id            UUID          NOT NULL DEFAULT gen_random_uuid(),
    order_id      UUID          NOT NULL,
    product_id    UUID          NOT NULL,
    product_title VARCHAR(255)  NOT NULL,
    unit_price    NUMERIC(19,2) NOT NULL,
    quantity      INTEGER       NOT NULL,
    line_total    NUMERIC(19,2) NOT NULL,

    CONSTRAINT pk_order_item              PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT chk_order_item_quantity    CHECK (quantity > 0),
    CONSTRAINT chk_order_item_unit_price  CHECK (unit_price >= 0)
);

-- ─── Histórico de transições de status ───────────────────────
CREATE TABLE order_status_history (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    order_id    UUID         NOT NULL,
    from_status VARCHAR(50),
    to_status   VARCHAR(50)  NOT NULL,
    reason      VARCHAR(500),
    changed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_order_status_history       PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- ─── Outbox (garantia de publicação no Kafka) ─────────────────
-- Gravada na mesma transação do domínio. Um scheduler publica
-- os eventos PENDING e marca como SENT.
CREATE TABLE outbox_event (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    aggregate_id UUID        NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload_json TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at      TIMESTAMPTZ,

    CONSTRAINT pk_outbox_event        PRIMARY KEY (id),
    CONSTRAINT chk_outbox_event_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- ─── Processed events (idempotência dos consumidores Kafka) ───
-- Cada event_id processado é salvo aqui para evitar reprocessamento.
CREATE TABLE processed_event (
    event_id     UUID        NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_processed_event PRIMARY KEY (event_id)
);

-- ─── Índices ─────────────────────────────────────────────────
CREATE INDEX idx_orders_customer_id      ON orders (customer_id);
CREATE INDEX idx_orders_status           ON orders (status);
CREATE INDEX idx_order_item_order_id     ON order_item (order_id);
CREATE INDEX idx_outbox_event_status     ON outbox_event (status, created_at);
CREATE INDEX idx_order_status_history_oid ON order_status_history (order_id, changed_at);
