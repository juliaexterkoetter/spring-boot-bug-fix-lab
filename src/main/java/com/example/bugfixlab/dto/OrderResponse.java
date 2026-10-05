package com.example.bugfixlab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;

@Schema(description = "Saved order with server-calculated totals.")
public record OrderResponse(@Schema(description = "Order ID.", example = "1") Long id, @Schema(description = "Referenced customer ID.", example = "1") Long customerId, @Schema(description = "Creation timestamp in UTC; retained on replacement.", example = "2026-10-05T12:00:00Z") Instant createdAt, @Schema(description = "Saved order lines.") List<OrderItemResponse> items, @Schema(description = "Sum of current item subtotals.", example = "20.00") BigDecimal total) {}
