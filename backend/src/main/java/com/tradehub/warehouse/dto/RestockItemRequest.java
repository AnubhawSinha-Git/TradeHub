package com.tradehub.warehouse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RestockItemRequest(

        @NotNull(message = "Product ID is required")
        @Min(value = 1, message = "Product ID must be valid")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Restock quantity must be at least 1")
        Integer quantity
) {
}