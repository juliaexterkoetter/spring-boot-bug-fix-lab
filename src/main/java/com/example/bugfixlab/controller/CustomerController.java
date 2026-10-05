package com.example.bugfixlab.controller;

import com.example.bugfixlab.dto.CustomerRequest;
import com.example.bugfixlab.dto.CustomerResponse;
import com.example.bugfixlab.service.CustomerService;
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

@Tag(name = "Customers", description = "Customer records with case-insensitive unique email addresses.")
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService service;

    public CustomerController(CustomerService service) { this.service = service; }

    @Operation(summary = "List customers", description = "Return all customers sorted by ID. This endpoint is not paginated.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CustomerResponse.class)),
                    examples = @ExampleObject(value = "[{\"id\":1,\"name\":\"Alex Smith\",\"email\":\"alex@example.com\"}]")))
    })
    @GetMapping
    public List<CustomerResponse> findAll() { return service.findAll(); }

    @Operation(summary = "Get a customer", description = "Return the customer identified by its ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"name\":\"Alex Smith\",\"email\":\"alex@example.com\"}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Invalid ID format\"}"))),
        @ApiResponse(responseCode = "404", description = "Resource or referenced customer/product not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Customer not found: 999999\",\"instance\":\"/api/customers/999999\"}")))
    })
    @GetMapping("/{id}")
    public CustomerResponse findById(@Parameter(description = "Resource ID", example = "1", required = true) @PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Create a customer", description = "Create a customer. Names are trimmed and emails are normalized to lowercase; duplicate emails are rejected.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Customer fields to save.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CustomerRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Alex Smith\",\"email\":\"alex@example.com\"}"))))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Resource created; Location identifies the new resource",
            headers = @Header(name = "Location", description = "Relative URI of the created resource", schema = @Schema(type = "string", example = "/api/customers/1")),
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"name\":\"Alex Smith\",\"email\":\"alex@example.com\"}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Request validation failed\",\"errors\":[{\"field\":\"name\",\"message\":\"Name is required\"}]}"))),
        @ApiResponse(responseCode = "409", description = "Conflict with existing data",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Conflict\",\"status\":409,\"detail\":\"Email is already in use\",\"instance\":\"/api/customers\"}")))
    })
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/customers/" + response.id())).body(response);
    }

    @Operation(summary = "Replace a customer", description = "Replace the customer name and email. The email must not belong to another customer.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Customer fields to save.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CustomerRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Alex Smith\",\"email\":\"alex@example.com\"}"))))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"name\":\"Alex Smith\",\"email\":\"alex@example.com\"}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Request validation failed\",\"errors\":[{\"field\":\"name\",\"message\":\"Name is required\"}]}"))),
        @ApiResponse(responseCode = "404", description = "Resource or referenced customer/product not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Customer not found: 999999\",\"instance\":\"/api/customers/999999\"}"))),
        @ApiResponse(responseCode = "409", description = "Conflict with existing data",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Conflict\",\"status\":409,\"detail\":\"Email is already in use\",\"instance\":\"/api/customers\"}")))
    })
    @PutMapping("/{id}")
    public CustomerResponse update(@Parameter(description = "Resource ID", example = "1", required = true) @PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete a customer", description = "Delete the customer. Deletion is rejected when referenced by an order.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Resource deleted", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request body, validation constraints, or ID format",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Invalid ID format\"}"))),
        @ApiResponse(responseCode = "404", description = "Resource or referenced customer/product not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Customer not found: 999999\",\"instance\":\"/api/customers/999999\"}"))),
        @ApiResponse(responseCode = "409", description = "Conflict with existing data",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Conflict\",\"status\":409,\"detail\":\"The operation conflicts with existing data. Referenced resources cannot be deleted and unique values cannot be duplicated.\",\"instance\":\"/api/customers\"}")))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Resource ID", example = "1", required = true) @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
