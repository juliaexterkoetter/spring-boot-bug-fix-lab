package com.example.bugfixlab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

@Schema(description = "An order line. Repeated product IDs are allowed; creation validates their combined quantity.")
public record OrderItemRequest(@NotNull(message = "Product ID is required") @Positive(message = "Product ID must be positive") @Schema(description = "ID of an existing product.", example = "1", minimum = "1") Long productId,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") @Max(value = 1000000, message = "Quantity must not exceed 1000000") @Schema(description = "Number of units, from 1 to 1000000.", example = "2") Integer quantity) {}
