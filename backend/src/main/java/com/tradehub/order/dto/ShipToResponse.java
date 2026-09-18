package com.tradehub.order.dto;

public record ShipToResponse(
        String recipientName,
        String addressLine,
        String addressLine2,
        String city,
        String state,
        String zipCode,
        String country,
        String phone
) {
}