package com.example.bugfixlab.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.List;

@Schema(description = "Order customer and replacement items. Prices and totals are server-controlled.")
public record OrderRequest(@NotNull(message = "Customer ID is required") @Positive(message = "Customer ID must be positive") @Schema(description = "ID of an existing customer.", example = "1", minimum = "1") Long customerId,
        @NotEmpty(message = "At least one item is required") @Size(max = 100, message = "An order must not exceed 100 items") @ArraySchema(minItems = 1, maxItems = 100, arraySchema = @Schema(description = "One to 100 non-null order lines.")) List<@NotNull(message = "Item is required") @Valid OrderItemRequest> items) {}
