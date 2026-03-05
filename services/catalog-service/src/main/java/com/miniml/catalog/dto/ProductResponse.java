package com.miniml.catalog.dto;

import com.miniml.catalog.domain.Product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        Integer stock,
        String category,
        String imageUrl,
        boolean active,
        OffsetDateTime createdAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getCategory(),
                p.getImageUrl(),
                p.isActive(),
                p.getCreatedAt()
        );
    }
}
