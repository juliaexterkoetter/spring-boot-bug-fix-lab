package com.example.bugfixlab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Saved order item with a unit price snapshot.")
public record OrderItemResponse(@Schema(description = "Order item ID.", example = "1") Long id, @Schema(description = "Referenced product ID.", example = "1") Long productId, @Schema(description = "Number of units.", example = "2") int quantity, @Schema(description = "Product price captured when the item was added.", example = "10.00") BigDecimal unitPrice, @Schema(description = "Quantity multiplied by the saved unit price.", example = "20.00") BigDecimal subtotal) {}
