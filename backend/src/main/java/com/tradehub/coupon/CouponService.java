package com.tradehub.coupon;

import com.tradehub.coupon.dto.CouponResponse;
import com.tradehub.coupon.dto.CreateCouponRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);

        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Coupon code already exists");
        }

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setType(request.type());
        coupon.setValue(request.value());
        coupon.setMaxDiscount(request.maxDiscount());
        coupon.setMinOrderAmount(request.minOrderAmount());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidUntil(request.validUntil());
        coupon.setMaxUses(request.maxUses());
        coupon.setActive(request.active());
        couponRepository.save(coupon);

        return toResponse(coupon);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getCoupons() {
        return couponRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void validateAndConsume(String code, BigDecimal orderSubtotal) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));

        if (!coupon.isActive()) {
            throw new IllegalArgumentException("Coupon is inactive");
        }

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            throw new IllegalArgumentException("Coupon is not yet valid");
        }

        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
            throw new IllegalArgumentException("Coupon has expired");
        }

        if (coupon.getMinOrderAmount() != null
                && orderSubtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Minimum order amount not met for this coupon");
        }

        if (coupon.getMaxUses() != null
                && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new IllegalArgumentException("Coupon usage limit reached");
        }

        coupon.setUsedCount(coupon.getUsedCount() + 1);
    }

    public BigDecimal computeDiscount(
            String code,
            BigDecimal orderSubtotal) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));

        BigDecimal discount;
        if (coupon.getType() == CouponType.PERCENT) {
            discount = orderSubtotal
                    .multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null
                    && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getValue();
        }

        return discount.min(orderSubtotal);
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getType(),
                coupon.getValue(),
                coupon.getMaxDiscount(),
                coupon.getMinOrderAmount(),
                coupon.isActive(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getMaxUses(),
                coupon.getUsedCount(),
                coupon.getCreatedAt());
    }
}