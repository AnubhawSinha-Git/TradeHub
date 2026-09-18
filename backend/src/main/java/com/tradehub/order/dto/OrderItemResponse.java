package com.tradehub.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String productName,
        String productSlug,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {
}