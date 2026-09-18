package com.tradehub.coupon.dto;

import com.tradehub.coupon.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCouponRequest(
        @NotBlank(message = "code is required")
        @Size(max = 50)
        String code,

        @NotNull(message = "type is required")
        CouponType type,

        @NotNull(message = "value is required")
        @DecimalMin(value = "0.01", message = "value must be positive")
        BigDecimal value,

        @DecimalMin(value = "0.01", message = "maxDiscount must be positive")
        BigDecimal maxDiscount,

        @DecimalMin(value = "0.01", message = "minOrderAmount must be positive")
        BigDecimal minOrderAmount,

        LocalDateTime validFrom,

        @NotNull(message = "validUntil is required")
        LocalDateTime validUntil,

        @Min(value = 1, message = "maxUses must be at least 1")
        Integer maxUses,

        boolean active
) {
}