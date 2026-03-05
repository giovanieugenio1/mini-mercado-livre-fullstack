-- ── payment ──────────────────────────────────────────────────────────────────
-- Registro de cada tentativa de pagamento vinculada a um pedido.
-- Relação 1:1 com orders do order-service (order_id UNIQUE).
CREATE TABLE payment (
    id              UUID            NOT NULL,
    order_id        UUID            NOT NULL,
    customer_id     UUID            NOT NULL,
    amount          NUMERIC(19, 2)  NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'BRL',
    status          VARCHAR(30)     NOT NULL,       -- PENDING | AUTHORIZED | FAILED | REFUNDED
    payment_method  VARCHAR(50),                    -- CREDIT_CARD | PIX | BOLETO (mock)
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,

    CONSTRAINT pk_payment PRIMARY KEY (id),
    CONSTRAINT uq_payment_order UNIQUE (order_id)
);

CREATE INDEX idx_payment_customer ON payment (customer_id);
CREATE INDEX idx_payment_status   ON payment (status);

-- ── outbox_event ─────────────────────────────────────────────────────────────
-- Outbox Pattern: evento gravado na mesma transação do pagamento.
-- O OutboxPublisherService publica no Kafka e marca como SENT.
CREATE TABLE outbox_event (
    id           UUID         NOT NULL,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload_json TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING | SENT | FAILED
    created_at   TIMESTAMPTZ  NOT NULL,
    sent_at      TIMESTAMPTZ,

    CONSTRAINT pk_outbox_event PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_status_created ON outbox_event (status, created_at)
    WHERE status = 'PENDING';

-- ── processed_event ──────────────────────────────────────────────────────────
-- Idempotência: garante que cada eventId Kafka seja processado exatamente uma vez.
CREATE TABLE processed_event (
    event_id     UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_processed_event PRIMARY KEY (event_id)
);
