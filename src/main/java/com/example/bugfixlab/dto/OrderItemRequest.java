package com.example.bugfixlab.dto;

import jakarta.validation.constraints.*;

public record OrderItemRequest(@NotNull(message = "Product ID is required") @Positive(message = "Product ID must be positive") Long productId,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") @Max(value = 1000000, message = "Quantity must not exceed 1000000") Integer quantity) {}
