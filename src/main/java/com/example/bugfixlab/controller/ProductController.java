package com.example.bugfixlab.controller;

import com.example.bugfixlab.dto.ProductRequest;
import com.example.bugfixlab.dto.ProductResponse;
import com.example.bugfixlab.service.ProductService;
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

@Tag(name = "Products", description = "Product prices and available stock. Stock is not automatically depleted.")
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @Operation(summary = "List products", description = "Return all products sorted by ID. This endpoint is not paginated.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductResponse.class)),
                    examples = @ExampleObject(value = "[{\"id\":1,\"name\":\"Keyboard\",\"price\":10.0,\"stock\":100}]")))
    })
    @GetMapping
    public List<ProductResponse> findAll() { return service.findAll(); }

    @Operation(summary = "Get a product", description = "Return the product identified by its ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"name\":\"Keyboard\",\"price\":10.0,\"stock\":100}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Invalid ID format\"}"))),
        @ApiResponse(responseCode = "404", description = "Resource or referenced customer/product not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Product not found: 999999\",\"instance\":\"/api/products/999999\"}")))
    })
    @GetMapping("/{id}")
    public ProductResponse findById(@Parameter(description = "Resource ID", example = "1", required = true) @PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Create a product", description = "Create a product. Prices require at most two decimal places. Omitted or null stock defaults to zero.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Product fields to save.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Keyboard\",\"price\":10.0,\"stock\":100}"))))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Resource created; Location identifies the new resource",
            headers = @Header(name = "Location", description = "Relative URI of the created resource", schema = @Schema(type = "string", example = "/api/products/1")),
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"name\":\"Keyboard\",\"price\":10.0,\"stock\":100}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Request validation failed\",\"errors\":[{\"field\":\"name\",\"message\":\"Name is required\"}]}")))
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + response.id())).body(response);
    }

    @Operation(summary = "Replace a product", description = "Replace the product name and price. Supplied stock replaces the current value; omitted or null stock preserves it. Existing order price snapshots are unchanged.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Product fields to save.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Keyboard\",\"price\":10.0,\"stock\":100}"))))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"name\":\"Keyboard\",\"price\":10.0,\"stock\":100}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Request validation failed\",\"errors\":[{\"field\":\"name\",\"message\":\"Name is required\"}]}"))),
        @ApiResponse(responseCode = "404", description = "Resource or referenced customer/product not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Product not found: 999999\",\"instance\":\"/api/products/999999\"}")))
    })
    @PutMapping("/{id}")
    public ProductResponse update(@Parameter(description = "Resource ID", example = "1", required = true) @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete a product", description = "Delete the product. Deletion is rejected when referenced by an order.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Resource deleted", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Invalid ID format\"}"))),
        @ApiResponse(responseCode = "404", description = "Resource or referenced customer/product not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Product not found: 999999\",\"instance\":\"/api/products/999999\"}"))),
        @ApiResponse(responseCode = "409", description = "Conflict with existing data",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Conflict\",\"status\":409,\"detail\":\"The operation conflicts with existing data. Referenced resources cannot be deleted and unique values cannot be duplicated.\",\"instance\":\"/api/products\"}")))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Resource ID", example = "1", required = true) @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
