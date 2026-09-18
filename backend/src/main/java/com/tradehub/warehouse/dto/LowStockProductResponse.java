package com.tradehub.warehouse.dto;

public record LowStockProductResponse(
        Long id,
        String name,
        String sku,
        Integer stockQuantity,
        Integer lowStockThreshold,
        Integer suggestedQuantity
) {
}