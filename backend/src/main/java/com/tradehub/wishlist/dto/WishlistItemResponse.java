package com.tradehub.wishlist.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WishlistItemResponse(
        Long wishlistItemId,
        Long productId,
        String productName,
        String productSlug,
        BigDecimal price,
        boolean active,
        LocalDateTime createdAt
) {
}