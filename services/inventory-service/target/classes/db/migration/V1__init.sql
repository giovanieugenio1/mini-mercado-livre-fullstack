-- ── product_stock ─────────────────────────────────────────────────────────────
-- Estoque disponível e reservado por produto.
-- PK = product_id (UUID do catalog-service — sem FK real, microsserviços independentes).
CREATE TABLE product_stock (
    product_id    UUID           NOT NULL,
    available_qty INT            NOT NULL DEFAULT 0,
    reserved_qty  INT            NOT NULL DEFAULT 0,
    version       BIGINT         NOT NULL DEFAULT 0,   -- optimistic locking

    CONSTRAINT pk_product_stock PRIMARY KEY (product_id),
    CONSTRAINT chk_available_qty CHECK (available_qty >= 0),
    CONSTRAINT chk_reserved_qty  CHECK (reserved_qty  >= 0)
);

-- ── reservation ───────────────────────────────────────────────────────────────
-- Reserva de estoque para um pedido. Relação 1:1 com orders.
CREATE TABLE reservation (
    id          UUID         NOT NULL,
    order_id    UUID         NOT NULL,
    status      VARCHAR(20)  NOT NULL,   -- RESERVED | FAILED
    fail_reason TEXT,
    created_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_reservation    PRIMARY KEY (id),
    CONSTRAINT uq_reservation_order UNIQUE (order_id)
);

CREATE INDEX idx_reservation_order ON reservation (order_id);

-- ── reservation_item ──────────────────────────────────────────────────────────
-- Itens de cada reserva com a quantidade reservada.
CREATE TABLE reservation_item (
    id                UUID  NOT NULL,
    reservation_id    UUID  NOT NULL,
    product_id        UUID  NOT NULL,
    quantity_reserved INT   NOT NULL,

    CONSTRAINT pk_reservation_item    PRIMARY KEY (id),
    CONSTRAINT fk_reservation_item    FOREIGN KEY (reservation_id) REFERENCES reservation(id)
);

-- ── outbox_event ──────────────────────────────────────────────────────────────
CREATE TABLE outbox_event (
    id           UUID         NOT NULL,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload_json TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ  NOT NULL,
    sent_at      TIMESTAMPTZ,

    CONSTRAINT pk_outbox_event PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_status_created ON outbox_event (status, created_at)
    WHERE status = 'PENDING';

-- ── processed_event ───────────────────────────────────────────────────────────
CREATE TABLE processed_event (
    event_id     UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_processed_event PRIMARY KEY (event_id)
);
