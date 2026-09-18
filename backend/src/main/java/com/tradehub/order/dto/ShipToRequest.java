package com.tradehub.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipToRequest(
        @NotBlank(message = "recipientName is required")
        @Size(max = 100)
        String recipientName,

        @NotBlank(message = "addressLine is required")
        @Size(max = 200)
        String addressLine,

        @Size(max = 200)
        String addressLine2,

        @NotBlank(message = "city is required")
        @Size(max = 100)
        String city,

        @NotBlank(message = "state is required")
        @Size(max = 100)
        String state,

        @NotBlank(message = "zipCode is required")
        @Size(max = 20)
        String zipCode,

        @NotBlank(message = "country is required")
        @Size(max = 100)
        String country,

        @Size(max = 30)
        String phone
) {
}