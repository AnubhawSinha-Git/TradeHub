package com.tradehub.coupon.dto;

import com.tradehub.coupon.CouponType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String code,
        CouponType type,
        BigDecimal value,
        BigDecimal maxDiscount,
        BigDecimal minOrderAmount,
        boolean active,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Integer maxUses,
        Integer usedCount,
        LocalDateTime createdAt
) {
}