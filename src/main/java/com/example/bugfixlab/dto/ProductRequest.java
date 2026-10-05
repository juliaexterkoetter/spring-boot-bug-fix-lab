package com.example.bugfixlab.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(@NotBlank(message = "Name is required") @Size(max = 100, message = "Name must not exceed 100 characters") String name,
        @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be at least 0.01") @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places") BigDecimal price,
        @PositiveOrZero(message = "Stock must not be negative") Integer stock) {}
