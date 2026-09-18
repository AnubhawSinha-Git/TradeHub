package com.tradehub.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String slug,
        String sku,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        Integer lowStockThreshold,
        boolean active,
        Long categoryId,
        String categoryName,
        String categorySlug,
        LocalDateTime createdAt
) {
}