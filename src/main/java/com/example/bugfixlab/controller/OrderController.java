package com.example.bugfixlab.controller;

import com.example.bugfixlab.dto.OrderRequest;
import com.example.bugfixlab.dto.OrderResponse;
import com.example.bugfixlab.service.OrderService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@Tag(name = "Orders", description = "Orders with price snapshots, server-calculated totals, and transactional writes.")
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @Operation(summary = "List orders", description = "Return all orders sorted by ID. This endpoint is not paginated.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class)),
                    examples = @ExampleObject(value = "[{\"id\":1,\"customerId\":1,\"createdAt\":\"2026-10-05T12:00:00Z\",\"items\":[{\"id\":1,\"productId\":1,\"quantity\":2,\"unitPrice\":10.0,\"subtotal\":20.0}],\"total\":20.0}]")))
    })
    @GetMapping
    public List<OrderResponse> findAll() { return service.findAll(); }

    @Operation(summary = "Get an order", description = "Return the order identified by its ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"customerId\":1,\"createdAt\":\"2026-10-05T12:00:00Z\",\"items\":[{\"id\":1,\"productId\":1,\"quantity\":2,\"unitPrice\":10.0,\"subtotal\":20.0}],\"total\":20.0}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Invalid ID format\"}"))),
        @ApiResponse(responseCode = "404", description = "Resource or referenced customer/product not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Order not found: 999999\",\"instance\":\"/api/orders/999999\"}")))
    })
    @GetMapping("/{id}")
    public OrderResponse findById(@Parameter(description = "Resource ID", example = "1", required = true) @PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Create an order", description = "Create an order using existing customer and product IDs. Quantities are summed per product for stock validation. Insufficient stock rejects the entire request; no order is saved. Prices and totals are calculated by the server; stock is not reserved or depleted.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Customer and product IDs must refer to existing resources. Do not submit unit prices or totals.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderRequest.class),
                            examples = @ExampleObject(value = "{\"customerId\":1,\"items\":[{\"productId\":1,\"quantity\":2}]}"))))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Resource created; Location identifies the new resource",
            headers = @Header(name = "Location", description = "Relative URI of the created resource", schema = @Schema(type = "string", example = "/api/orders/1")),
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"customerId\":1,\"createdAt\":\"2026-10-05T12:00:00Z\",\"items\":[{\"id\":1,\"productId\":1,\"quantity\":2,\"unitPrice\":10.0,\"subtotal\":20.0}],\"total\":20.0}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Request validation failed\",\"errors\":[{\"field\":\"items\",\"message\":\"At least one item is required\"}]}"))),
        @ApiResponse(responseCode = "404", description = "Resource or referenced customer/product not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Customer not found: 999999\",\"instance\":\"/api/orders\"}"))),
        @ApiResponse(responseCode = "409", description = "Insufficient stock",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Conflict\",\"status\":409,\"detail\":\"Insufficient stock for product: 1\",\"instance\":\"/api/orders\"}")))
    })
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/orders/" + response.id())).body(response);
    }

    @Operation(summary = "Replace an order", description = "Replace customer and all items, retaining creation time. New items use current product prices and the total is rebuilt. Any missing reference rolls back the update. This endpoint does not validate or reserve stock.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Customer and product IDs must refer to existing resources. Do not submit unit prices or totals.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderRequest.class),
                            examples = @ExampleObject(value = "{\"customerId\":1,\"items\":[{\"productId\":1,\"quantity\":2}]}"))))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"customerId\":1,\"createdAt\":\"2026-10-05T12:00:00Z\",\"items\":[{\"id\":1,\"productId\":1,\"quantity\":2,\"unitPrice\":10.0,\"subtotal\":20.0}],\"total\":20.0}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Request validation failed\",\"errors\":[{\"field\":\"items\",\"message\":\"At least one item is required\"}]}"))),
        @ApiResponse(responseCode = "404", description = "Resource or referenced customer/product not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Order not found: 999999\",\"instance\":\"/api/orders/999999\"}")))
    })
    @PutMapping("/{id}")
    public OrderResponse update(@Parameter(description = "Resource ID", example = "1", required = true) @PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete an order", description = "Delete the order. Linked order items are deleted as well.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Resource deleted", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Invalid ID format\"}"))),
        @ApiResponse(responseCode = "404", description = "Resource or referenced customer/product not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Order not found: 999999\",\"instance\":\"/api/orders/999999\"}")))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Resource ID", example = "1", required = true) @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
