# Spring Boot Bug Fix Lab

A small REST API for managing customers, products, and orders. This portfolio project provides a clean, working baseline for demonstrating diagnosis, regression testing, and bug fixes in Spring Boot applications. No intentional bugs are included in this version.

## Technologies

- Java 21 (JDK required)
- Spring Boot 3.5.6 and Maven 3.9+
- Spring Web, Spring Data JPA, and Bean Validation
- H2 in-memory database
- JUnit 5, Mockito, Spring Boot Test, and MockMvc

## Run locally

Check the prerequisites:

```bash
java -version
javac -version
mvn -version
```

From the repository root:

```bash
mvn spring-boot:run
```

The API listens at `http://localhost:8080`. Alternatively, build and run the executable JAR:

```bash
mvn package
java -jar target/spring-boot-bug-fix-lab-0.0.1-SNAPSHOT.jar
```

In the managed workspace used to initialize this project, the missing JDK and Maven were installed outside the repository under `/workspace/.tools`. Activate them for the current shell with:

```bash
source /workspace/.tools/env.sh
```

That workspace's Maven installation uses its configured outbound proxy and a dependency cache under `/workspace/.tools/m2`. These environment-specific settings are not required on a standard local installation.

## Tests and build

```bash
mvn test
mvn package
```

Both commands run the JUnit 5 suite. Unit tests use Mockito to test order calculations and missing products. Integration tests use the full Spring context, MockMvc, and a real H2 database to verify CRUD operations, validation, duplicate emails, referential integrity, price snapshots, and transaction rollback. The database is cleaned before each integration test.

## Main endpoints

All request and response bodies use JSON. Replace `{id}` with a resource ID.

| Resource | Create | List | Get | Replace | Delete |
| --- | --- | --- | --- | --- | --- |
| Customers | `POST /api/customers` | `GET /api/customers` | `GET /api/customers/{id}` | `PUT /api/customers/{id}` | `DELETE /api/customers/{id}` |
| Products | `POST /api/products` | `GET /api/products` | `GET /api/products/{id}` | `PUT /api/products/{id}` | `DELETE /api/products/{id}` |
| Orders | `POST /api/orders` | `GET /api/orders` | `GET /api/orders/{id}` | `PUT /api/orders/{id}` | `DELETE /api/orders/{id}` |

Creation returns `201 Created` with a `Location` header. Reads and replacements return `200 OK`; deletion returns `204 No Content`. Lists are sorted by ID.

### Example workflow

Create a customer and a product, then use their returned IDs to create an order. The following assumes a fresh database, where both IDs are `1`:

```bash
curl -i -X POST http://localhost:8080/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alex Smith","email":"alex@example.com"}'

curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Keyboard","price":19.99}'

curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":1,"items":[{"productId":1,"quantity":2}]}'

curl -i http://localhost:8080/api/orders/1

curl -i -X PUT http://localhost:8080/api/orders/1 \
  -H 'Content-Type: application/json' \
  -d '{"customerId":1,"items":[{"productId":1,"quantity":3}]}'

curl -i -X DELETE http://localhost:8080/api/orders/1
```

An order response includes `id`, `customerId`, `createdAt` (UTC), `items`, and `total`. Each item includes `id`, `productId`, `quantity`, `unitPrice`, and `subtotal`.

## Validation and behavior

- Customer and product names are required, trimmed, and limited to 100 characters.
- Customer emails must be valid, at most 254 characters, and unique without regard to case. Emails are stored in lowercase.
- Product prices must be at least `0.01`, with at most 10 integer digits and 2 decimal places.
- Orders require an existing customer and 1–100 non-null items. Each item requires an existing product and an integer quantity from 1 to 1,000,000.
- Monetary values use `BigDecimal`. The server calculates subtotals and totals from product prices; clients cannot submit prices or totals on orders.
- Each order item stores the product price at the time it is added. Later product price changes do not alter existing orders.
- `PUT` fully replaces the resource. Replacing an order replaces its items and uses current product prices, while retaining its creation time. Repeated product IDs are allowed as separate line items.
- Order writes are transactional. An invalid reference rolls back the entire operation.
- Customers and products referenced by orders cannot be deleted. Delete the relevant orders first. Deleting an order also deletes its items.
- Unknown JSON fields and fractional values for integer fields are rejected.

Errors use Spring's Problem Details format (`application/problem+json`): `400` for invalid input, `404` for missing resources, and `409` for conflicting data. Validation responses include an `errors` array with English `field` and `message` values. SQL details are not exposed.

## Structure

```text
src/main/java/com/example/bugfixlab/
  controller/   HTTP endpoints
  service/      Business rules and transaction boundaries
  repository/   Spring Data JPA repositories
  entity/       Customer, Product, Order, and OrderItem
  dto/          Validated requests and response records
  exception/    Exceptions and global HTTP error handling
src/main/resources/application.properties
src/test/java/com/example/bugfixlab/
```

## Scope and limitations

The H2 database is in memory: all data is lost when the application stops. Schema creation is automatic for this development baseline. There is no authentication, frontend, Docker configuration, payment processing, stock control, or order status workflow. List endpoints are unpaginated and intended for small portfolio datasets. Production persistence and deployment hardening are outside this initial version's scope.
