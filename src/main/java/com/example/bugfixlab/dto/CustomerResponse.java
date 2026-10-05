package com.example.bugfixlab.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Saved customer.")
public record CustomerResponse(@Schema(description = "Customer ID.", example = "1") Long id, @Schema(description = "Customer name.", example = "Alex Smith") String name, @Schema(description = "Normalized email.", example = "alex@example.com") String email) {}
