package com.example.bugfixlab.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.List;

public record OrderRequest(@NotNull(message = "Customer ID is required") @Positive(message = "Customer ID must be positive") Long customerId,
        @NotEmpty(message = "At least one item is required") @Size(max = 100, message = "An order must not exceed 100 items") List<@NotNull(message = "Item is required") @Valid OrderItemRequest> items) {}
