package com.example.bugfixlab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Saved product.")
public record ProductResponse(@Schema(description = "Product ID.", example = "1") Long id, @Schema(description = "Product name.", example = "Keyboard") String name, @Schema(description = "Current unit price.", example = "10.00") BigDecimal price, @Schema(description = "Available units.", example = "100") int stock) {}
