package com.example.bugfixlab.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

@Schema(description = "Customer fields. Email uniqueness is case-insensitive.")
public record CustomerRequest(@NotBlank(message = "Name is required") @Size(max = 100, message = "Name must not exceed 100 characters") @Schema(description = "Customer name; trimmed when saved.", example = "Alex Smith") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 254, message = "Email must not exceed 254 characters") @Schema(description = "Valid, unique email; stored in lowercase.", example = "alex@example.com") String email) {}
