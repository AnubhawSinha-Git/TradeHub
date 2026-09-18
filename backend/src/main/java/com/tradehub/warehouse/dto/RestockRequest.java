package com.tradehub.warehouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RestockRequest(

        @Valid
        @NotEmpty(message = "Restock list cannot be empty")
        List<RestockItemRequest> items
) {
}