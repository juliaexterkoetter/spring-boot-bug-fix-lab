package com.example.bugfixlab;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "Spring Boot Bug Fix Lab API", version = "0.0.1",
        description = "Order management API and debugging portfolio. Manage customers, products, and orders. "
                + "Orders use price snapshots; replacement uses current product prices. "
                + "Stock is checked on creation without reservation or depletion. H2 data resets on restart."))
@SpringBootApplication
public class BugFixLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(BugFixLabApplication.class, args);
    }
}
