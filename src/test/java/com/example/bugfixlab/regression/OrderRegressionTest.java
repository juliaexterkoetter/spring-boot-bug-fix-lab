package com.example.bugfixlab.regression;

import com.example.bugfixlab.dto.OrderItemRequest;
import com.example.bugfixlab.dto.OrderRequest;
import com.example.bugfixlab.dto.OrderResponse;
import com.example.bugfixlab.entity.Customer;
import com.example.bugfixlab.entity.Product;
import com.example.bugfixlab.repository.CustomerRepository;
import com.example.bugfixlab.repository.OrderRepository;
import com.example.bugfixlab.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:order-regression;DB_CLOSE_DELAY=-1")
class OrderRegressionTest {
    @Autowired TestRestTemplate http;
    @Autowired OrderRepository orders;
    @Autowired CustomerRepository customers;
    @Autowired ProductRepository products;

    @BeforeEach
    void clearDatabase() {
        orders.deleteAll();
        customers.deleteAll();
        products.deleteAll();
    }

    @Test
    @DisplayName("Bug 1: reject orders exceeding available stock without persisting them")
    void rejectsOrderWhenQuantityExceedsAvailableStock() {
        Customer customer = createCustomer();
        Product product = createProductWithStock(2);
        OrderRequest request = orderRequest(customer, product, 5);

        var response = http.postForEntity("/api/orders", request, String.class);

        assertAll("Insufficient stock must reject the order atomically",
                () -> assertThat(response.getStatusCode())
                        .as("Order creation status; response body: %s", response.getBody())
                        .isEqualTo(HttpStatus.CONFLICT),
                () -> assertThat(orders.count()).as("Persisted orders after rejection").isZero());
    }

    @Test
    @DisplayName("Stock validation: allow an order requesting exactly the available stock")
    void acceptsOrderWhenQuantityEqualsAvailableStock() {
        Customer customer = createCustomer();
        Product product = createProductWithStock(2);

        var response = http.postForEntity("/api/orders", orderRequest(customer, product, 2), OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().total()).isEqualByComparingTo("20.00");
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Stock validation: sum repeated product lines before accepting an order")
    void rejectsOrderWhenRepeatedProductLinesExceedAvailableStock() {
        Customer customer = createCustomer();
        Product product = createProductWithStock(2);
        OrderRequest request = new OrderRequest(customer.getId(), List.of(
                new OrderItemRequest(product.getId(), 1), new OrderItemRequest(product.getId(), 2)));

        var response = http.postForEntity("/api/orders", request, String.class);

        assertAll("Repeated lines must not bypass stock validation or partially persist the order",
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT),
                () -> assertThat(orders.count()).isZero());
    }

    @Test
    @DisplayName("Bug 2: replacing quantity recalculates and persists the order total")
    void recalculatesOrderTotalWhenItemQuantityChanges() {
        Customer customer = createCustomer();
        Product product = createProductWithStock(100);
        var created = http.postForEntity("/api/orders", orderRequest(customer, product, 2), OrderResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().total()).isEqualByComparingTo("20.00");
        String orderPath = "/api/orders/" + created.getBody().id();

        var updated = http.exchange(orderPath, HttpMethod.PUT,
                new HttpEntity<>(orderRequest(customer, product, 5)), OrderResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isNotNull();

        // A separate HTTP request verifies the committed state, not just the update response.
        var retrieved = http.getForEntity(orderPath, OrderResponse.class);
        assertThat(retrieved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retrieved.getBody()).isNotNull();
        assertThat(retrieved.getBody().items()).hasSize(1);
        assertThat(retrieved.getBody().items().getFirst().quantity()).isEqualTo(5);
        assertThat(retrieved.getBody().items().getFirst().unitPrice()).isEqualByComparingTo("10.00");
        assertThat(retrieved.getBody().items().getFirst().subtotal()).isEqualByComparingTo("50.00");

        assertAll("Updated and persisted totals must equal the sum of current item subtotals",
                () -> assertThat(updated.getBody().total()).as("Total returned by PUT")
                        .isEqualByComparingTo("50.00"),
                () -> assertThat(retrieved.getBody().total()).as("Persisted total returned by GET")
                        .isEqualByComparingTo("50.00"));
    }

    @Test
    @DisplayName("Bug 3: retrieving a nonexistent order returns HTTP 404")
    void returnsNotFoundWhenOrderDoesNotExist() {
        long missingOrderId = 999999L;
        assertThat(orders.existsById(missingOrderId)).isFalse();

        // Real HTTP exercises Tomcat's error dispatch for unhandled service exceptions.
        var response = http.getForEntity("/api/orders/" + missingOrderId, String.class);

        assertThat(response.getStatusCode())
                .as("Missing order status; response body: %s", response.getBody())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Customer createCustomer() {
        return customers.saveAndFlush(new Customer("Regression Customer", "regression@example.com"));
    }

    private Product createProductWithStock(int stock) {
        Product product = new Product("Regression Product", new BigDecimal("10.00"));
        product.setStock(stock);
        return products.saveAndFlush(product);
    }

    private OrderRequest orderRequest(Customer customer, Product product, int quantity) {
        return new OrderRequest(customer.getId(), List.of(new OrderItemRequest(product.getId(), quantity)));
    }
}
