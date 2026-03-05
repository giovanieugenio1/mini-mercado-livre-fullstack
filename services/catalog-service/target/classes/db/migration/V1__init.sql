-- =============================================================
-- catalog-service — V1: Schema inicial
-- =============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE product (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    title       VARCHAR(255)  NOT NULL,
    description TEXT,
    price       NUMERIC(19,2) NOT NULL,
    stock       INTEGER       NOT NULL DEFAULT 0,
    category    VARCHAR(100)  NOT NULL,
    image_url   VARCHAR(500),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_product            PRIMARY KEY (id),
    CONSTRAINT chk_product_price     CHECK (price >= 0),
    CONSTRAINT chk_product_stock     CHECK (stock >= 0)
);

CREATE INDEX idx_product_category ON product (category);
CREATE INDEX idx_product_active   ON product (active);
CREATE INDEX idx_product_title    ON product USING GIN (to_tsvector('portuguese', title));
