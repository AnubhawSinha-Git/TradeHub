package com.tradehub.order.dto;

import com.tradehub.order.OrderStatus;
import com.tradehub.payment.dto.PaymentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String couponCode,
        ShipToResponse shippingAddress,
        PaymentResponse payment,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
}