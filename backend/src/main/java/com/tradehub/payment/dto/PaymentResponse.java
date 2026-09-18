package com.tradehub.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String transactionId,
        BigDecimal amount,
        LocalDateTime paidAt,
        boolean refunded,
        LocalDateTime refundedAt
) {
}