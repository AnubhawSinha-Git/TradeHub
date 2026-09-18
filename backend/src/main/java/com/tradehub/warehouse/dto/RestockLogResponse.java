package com.tradehub.warehouse.dto;

import java.time.LocalDateTime;

public record RestockLogResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        String restockedBy,
        LocalDateTime restockedAt
) {
}