package com.example.bugfixlab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Product fields. Omitted stock defaults to zero on creation and is preserved on update.")
public record ProductRequest(@NotBlank(message = "Name is required") @Size(max = 100, message = "Name must not exceed 100 characters") @Schema(description = "Product name; trimmed when saved.", example = "Keyboard") String name,
        @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be at least 0.01") @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places") @Schema(description = "Positive price, at most ten integer digits and two decimal places.", example = "10.00", maximum = "9999999999.99", multipleOf = 0.01) BigDecimal price,
        @PositiveOrZero(message = "Stock must not be negative") @Schema(description = "Available units; non-negative. No automatic reservation or depletion.", example = "100", minimum = "0") Integer stock) {}
