-- Tabela principal de envios
CREATE TABLE shipping (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID         NOT NULL UNIQUE,
    customer_id     UUID         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    tracking_code   VARCHAR(50),
    fail_reason     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_shipping_order_id    ON shipping(order_id);
CREATE INDEX idx_shipping_customer_id ON shipping(customer_id);
CREATE INDEX idx_shipping_status      ON shipping(status);

-- Outbox: eventos a publicar no Kafka (Outbox Pattern)
CREATE TABLE outbox_event (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payload_json TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at      TIMESTAMPTZ,
    version      BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_status ON outbox_event(status, created_at);

-- Idempotência: eventos Kafka já processados
CREATE TABLE processed_event (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
