package com.tradehub.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long cartItemId,
        Long productId,
        String productName,
        String productSlug,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {
}