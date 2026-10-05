package com.example.bugfixlab.dto;

import jakarta.validation.constraints.*;

public record CustomerRequest(@NotBlank(message = "Name is required") @Size(max = 100, message = "Name must not exceed 100 characters") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 254, message = "Email must not exceed 254 characters") String email) {}
