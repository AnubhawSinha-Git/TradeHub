package com.tradehub.review.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long productId,
        Long userId,
        String userFullName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}