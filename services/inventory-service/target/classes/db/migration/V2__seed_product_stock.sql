-- Seed de estoque com os UUIDs usados nas collections Postman de exemplo.
-- Esses UUIDs coincidem com os productIds usados nos requests de teste.
INSERT INTO product_stock (product_id, available_qty, reserved_qty) VALUES
    ('11111111-1111-1111-1111-111111111111', 100, 0),  -- iPhone 15 Pro
    ('22222222-2222-2222-2222-222222222222', 50,  0),  -- AirPods Pro
    ('33333333-3333-3333-3333-333333333333', 30,  0),  -- MacBook Pro
    ('44444444-4444-4444-4444-444444444444', 75,  0),  -- Produto genérico D
    ('55555555-5555-5555-5555-555555555555', 20,  0);  -- Produto genérico E
