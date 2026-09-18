package com.tradehub.review.dto;

import java.util.List;

public record ProductReviewsResponse(
        Long productId,
        double averageRating,
        long totalReviews,
        List<ReviewResponse> reviews
) {
}