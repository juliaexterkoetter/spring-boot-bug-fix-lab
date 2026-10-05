package com.example.bugfixlab.dto;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;

public record OrderResponse(Long id, Long customerId, Instant createdAt, List<OrderItemResponse> items, BigDecimal total) {}
