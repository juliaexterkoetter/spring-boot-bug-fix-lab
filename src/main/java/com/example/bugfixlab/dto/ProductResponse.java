package com.example.bugfixlab.dto;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price) {}
