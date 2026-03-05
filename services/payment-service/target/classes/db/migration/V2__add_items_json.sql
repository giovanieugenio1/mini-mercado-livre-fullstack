-- Adiciona items_json para repassar a lista de produtos ao inventory-service
-- via payment.authorized.v1 (saga coreografada sem necessidade de chamada HTTP)
ALTER TABLE payment ADD COLUMN items_json TEXT;
