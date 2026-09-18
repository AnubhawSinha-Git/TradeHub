package com.tradehub.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @Valid ShipToRequest shipment,

        @Size(max = 50, message = "couponCode must not exceed 50 characters")
        String couponCode
) {
}