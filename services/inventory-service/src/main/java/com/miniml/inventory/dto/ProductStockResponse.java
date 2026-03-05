package com.miniml.inventory.dto;

import com.miniml.inventory.domain.ProductStock;

import java.util.UUID;

public record ProductStockResponse(
        UUID productId,
        int availableQty,
        int reservedQty,
        long version
) {
    public static ProductStockResponse from(ProductStock s) {
        return new ProductStockResponse(
                s.getProductId(), s.getAvailableQty(), s.getReservedQty(), s.getVersion());
    }

}
