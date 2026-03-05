-- Remove os registros com UUIDs fictícios do seed inicial
DELETE FROM product_stock
WHERE product_id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444',
    '55555555-5555-5555-5555-555555555555'
);

-- Insere o estoque real usando os UUIDs gerados pelo catalog-service
INSERT INTO product_stock (product_id, available_qty, reserved_qty) VALUES
    ('5409a33b-b9a1-4c8e-8b2c-2cc83828c31b', 60,  0),  -- Air Fryer Philips Walita 4,1L
    ('bf790f31-426e-409c-9f09-162a0271d0b2', 45,  0),  -- Cafeteira Elétrica Nespresso Vertuo
    ('5417421d-e039-4646-9fc5-aa6570758529', 200, 0),  -- Camiseta Reserva Classic
    ('38f9165c-10be-4c8a-9df3-e3a394c6060d', 40,  0),  -- Headphone Sony WH-1000XM5
    ('71f7015a-5095-4487-808b-60f0c1dcb000', 300, 0),  -- Livro: Clean Code
    ('220cfbfd-3977-4868-a211-b2739130863b', 150, 0),  -- Livro: Domain-Driven Design
    ('cffe8495-6a9e-4d92-87d9-e3ca2caf446c', 25,  0),  -- Monitor LG UltraWide 34"
    ('e0d446ff-651d-4c6d-b05a-f9092707b852', 30,  0),  -- Notebook Dell Inspiron 15
    ('fcd4839e-b46e-4cfe-8789-99de3785e1b7', 80,  0),  -- Samsung Galaxy S24
    ('6bb55fa2-bfce-4391-9127-b56872d8f411', 40,  0),  -- Smart TV 4K 55 Polegadas
    ('3bdf6dc5-99d3-49c0-93b1-6de00736f2d2', 35,  0),  -- Tablet Samsung Galaxy Tab S9
    ('323dda87-a50e-4d33-816d-d0703b0823b8', 120, 0),  -- Tênis Nike Air Max 270
    ('fe767507-9812-46cd-aea5-fbea810ac54d', 50,  0)   -- iPhone 15 Pro
ON CONFLICT (product_id) DO UPDATE
    SET available_qty = EXCLUDED.available_qty,
        reserved_qty  = EXCLUDED.reserved_qty;
