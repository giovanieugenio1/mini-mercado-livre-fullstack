-- Notificações geradas para cada evento de domínio relevante
CREATE TABLE notification (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID,
    customer_id  UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    channel      VARCHAR(20)  NOT NULL DEFAULT 'EMAIL',
    subject      VARCHAR(255) NOT NULL,
    body         TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'SENT',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_order_id    ON notification(order_id);
CREATE INDEX idx_notification_customer_id ON notification(customer_id);
CREATE INDEX idx_notification_event_type  ON notification(event_type);
CREATE INDEX idx_notification_created_at  ON notification(created_at DESC);

-- Idempotência: eventos Kafka já processados
CREATE TABLE processed_event (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
