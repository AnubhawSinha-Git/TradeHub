package com.tradehub.wishlist.dto;

import jakarta.validation.constraints.NotNull;

public record AddWishlistItemRequest(
        @NotNull(message = "productId is required")
        Long productId
) {
}