package com.example.bugfixlab;

import com.example.bugfixlab.repository.CustomerRepository;
import com.example.bugfixlab.repository.OrderRepository;
import com.example.bugfixlab.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired OrderRepository orders;
    @Autowired CustomerRepository customers;
    @Autowired ProductRepository products;

    @BeforeEach
    void cleanDatabase() {
        orders.deleteAll();
        customers.deleteAll();
        products.deleteAll();
    }

    @Test
    void publishesOpenApiOperationsAndValidatedRequestSchemas() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode spec = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(spec.at("/info/title").asText()).isEqualTo("Spring Boot Bug Fix Lab API");
        assertThat(spec.at("/info/version").asText()).isEqualTo("0.0.1");
        for (String resource : new String[]{"customers", "products", "orders"}) {
            JsonNode collection = spec.get("paths").get("/api/" + resource);
            JsonNode item = spec.get("paths").get("/api/" + resource + "/{id}");
            assertThat(collection.get("get").get("responses").has("200")).isTrue();
            assertThat(collection.get("post").get("responses").has("201")).isTrue();
            assertThat(item.get("get").get("responses").has("404")).isTrue();
            assertThat(item.get("put").get("responses").has("400")).isTrue();
            assertThat(item.get("delete").get("responses").has("204")).isTrue();
        }
        JsonNode orderCreation = spec.get("paths").get("/api/orders").get("post");
        assertThat(orderCreation.get("responses").has("409")).isTrue();
        assertThat(orderCreation.at("/requestBody/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/OrderRequest");
        JsonNode schemas = spec.at("/components/schemas");
        assertThat(schemas.at("/OrderRequest/properties/items/minItems").asInt()).isEqualTo(1);
        assertThat(schemas.at("/OrderRequest/properties/items/maxItems").asInt()).isEqualTo(100);
        assertThat(schemas.at("/OrderRequest/properties/items/items/$ref").asText())
                .isEqualTo("#/components/schemas/OrderItemRequest");
        assertThat(schemas.at("/ProductRequest/properties/stock/minimum").asInt(-1)).isZero();
        assertThat(schemas.at("/ProblemDetail/properties").has("properties")).isFalse();
        assertThat(schemas.at("/ProblemDetail/properties/errors/items/properties").has("field")).isTrue();
    }

    @Test
    void customerCrudAndDuplicateEmail() throws Exception {
        long id = create("customers", """
                {"name":"Alex","email":"alex@example.com"}
                """);
        mvc.perform(get("/api/customers")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/customers/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Alex"));
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Other\",\"email\":\"ALEX@example.com\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/customers/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alex Smith\",\"email\":\"alex@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Alex Smith"));
        mvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/customers/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateEmailOnUpdateWithoutChangingCustomer() throws Exception {
        long owner = create("customers", """
                {"name":"Email Owner","email":"owner@example.com"}
                """);
        long customer = create("customers", """
                {"name":"Original Name","email":"original@example.com"}
                """);

        mvc.perform(put("/api/customers/{id}", customer).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Changed Name","email":"OWNER@example.com"}
                        """))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/customers/{id}", customer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original Name"))
                .andExpect(jsonPath("$.email").value("original@example.com"));
        mvc.perform(get("/api/customers/{id}", owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"));
        assertThat(customers.count()).isEqualTo(2);
    }

    @Test
    void preservesUpdatedStockWhenLaterProductUpdateOmitsIt() throws Exception {
        long customer = create("customers", """
                {"name":"Stock Customer","email":"stock@example.com"}
                """);
        long product = create("products", """
                {"name":"Keyboard","price":10.00,"stock":5}
                """);

        mvc.perform(put("/api/products/{id}", product).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Keyboard","price":10.00,"stock":2}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(2));
        mvc.perform(put("/api/products/{id}", product).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Updated Keyboard","price":12.00}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(2));
        mvc.perform(get("/api/products/{id}", product)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Keyboard"))
                .andExpect(jsonPath("$.price").value(12.00))
                .andExpect(jsonPath("$.stock").value(2));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(customer, product, 3)))
                .andExpect(status().isConflict());
        assertThat(orders.count()).isZero();
    }

    @Test
    void productCrud() throws Exception {
        long id = create("products", "{\"name\":\"Keyboard\",\"price\":19.99}");
        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/products/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.price").value(19.99));
        mvc.perform(put("/api/products/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mechanical Keyboard\",\"price\":29.99}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(29.99));
        mvc.perform(delete("/api/products/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void orderCrudPreservesPricesAndProtectsReferences() throws Exception {
        long customer = create("customers", "{\"name\":\"Alex\",\"email\":\"alex@example.com\"}");
        long product = create("products", "{\"name\":\"Keyboard\",\"price\":19.99,\"stock\":100}");
        long order = create("orders", orderBody(customer, product, 2));
        mvc.perform(get("/api/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/customers/{id}", customer)).andExpect(status().isConflict());
        mvc.perform(delete("/api/products/{id}", product)).andExpect(status().isConflict());
        mvc.perform(put("/api/products/{id}", product).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Keyboard\",\"price\":29.99}")).andExpect(status().isOk());
        mvc.perform(get("/api/orders/{id}", order)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(39.98)).andExpect(jsonPath("$.items[0].unitPrice").value(19.99));
        mvc.perform(put("/api/orders/{id}", order).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(customer, product, 3))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(89.97)).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/api/orders/{id}", order)).andExpect(jsonPath("$.total").value(89.97));
        mvc.perform(delete("/api/orders/{id}", order)).andExpect(status().isNoContent());
        mvc.perform(get("/api/orders/{id}", order)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/products/{id}", product)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/customers/{id}", customer)).andExpect(status().isNoContent());
    }

    @Test
    void rollsBackEntireOrderUpdateWhenAnyProductIsMissing() throws Exception {
        long customer = create("customers", "{\"name\":\"Alex\",\"email\":\"alex@example.com\"}");
        long product = create("products", "{\"name\":\"Keyboard\",\"price\":19.99,\"stock\":100}");
        long order = create("orders", orderBody(customer, product, 2));
        String invalidUpdate = "{\"customerId\":%d,\"items\":[{\"productId\":%d,\"quantity\":5},{\"productId\":999999,\"quantity\":1}]}"
                .formatted(customer, product);
        mvc.perform(put("/api/orders/{id}", order).contentType(MediaType.APPLICATION_JSON).content(invalidUpdate))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/{id}", order)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(39.98)).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void rejectsMissingReferencesWithoutPersistingOrder() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(999999, 999999, 1)))
                .andExpect(status().isNotFound());
        long customer = create("customers", "{\"name\":\"Alex\",\"email\":\"alex@example.com\"}");
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(customer, 999999, 1)))
                .andExpect(status().isNotFound());
        assertThat(orders.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"name\":\"\",\"email\":\"invalid\"}", "{}"})
    void rejectsInvalidCustomer(String body) throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.001", "10000000000", "null"})
    void rejectsInvalidPrice(String price) throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Keyboard\",\"price\":" + price + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]", "[{\"productId\":1,\"quantity\":0}]", "[{\"productId\":1,\"quantity\":null}]"})
    void rejectsInvalidItems(String items) throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":1,\"items\":" + items + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void rejectsMalformedJsonAndInvalidPath() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Request body is missing or malformed"));
        mvc.perform(get("/api/products/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":1,\"items\":[{\"productId\":1,\"quantity\":1.5}]}"))
                .andExpect(status().isBadRequest());
    }

    private long create(String resource, String body) throws Exception {
        var result = mvc.perform(post("/api/" + resource).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = mapper.readTree(result.getResponse().getContentAsString());
        long id = response.get("id").asLong();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/" + resource + "/" + id);
        return id;
    }

    private String orderBody(long customer, long product, int quantity) {
        return "{\"customerId\":%d,\"items\":[{\"productId\":%d,\"quantity\":%d}]}".formatted(customer, product, quantity);
    }
}
