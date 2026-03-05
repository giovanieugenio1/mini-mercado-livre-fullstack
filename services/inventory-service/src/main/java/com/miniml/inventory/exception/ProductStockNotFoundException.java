package com.miniml.inventory.exception;

import java.util.UUID;

public class ProductStockNotFoundException extends RuntimeException {
    public ProductStockNotFoundException(UUID productId) {
        super("Produto não encontrado no inventário: " + productId);
    }
}
