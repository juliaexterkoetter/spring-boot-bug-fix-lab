# Spring Boot Bug Fix Lab

A small REST API for managing customers, products, and orders, used to demonstrate diagnosis, regression testing, and bug fixes in Spring Boot applications.

## Current debugging exercise

All three documented debugging scenarios are now fixed:

1. [Insufficient stock](docs/bugs/01-insufficient-stock.md): fixed: order creation rejects quantities above available stock with HTTP 409.
2. [Order total recalculation](docs/bugs/02-order-total-recalculation.md): fixed: replacing items resets and rebuilds the total from the replacement items.
3. [Missing order error handling](docs/bugs/03-missing-order-error-handling.md): fixed: retrieving an unknown order returns HTTP 404 through the existing global exception handler.

The suite has **29 passing tests, 0 failures, 0 errors, and 0 skipped tests**. Both `mvn test` and `mvn package` pass. Existing regression assertions are preserved. See [regression test results](docs/regression-tests.md) for the red-to-green history and real HTTP verification. The clean baseline remains available in commit `e04266c917465b881984f27783fcc8f43bc3a72d`; historical bug reports identify pre-fix revisions for reproducing the defects.

## Technologies

- Java 21 (JDK required)
- Spring Boot 3.5.6 and Maven 3.9+
- Spring Web, Spring Data JPA, and Bean Validation
- H2 in-memory database
- JUnit 5, Mockito, Spring Boot Test, and MockMvc

## Prerequisites

Choose either execution path:

- **Maven:** Java 21 JDK and Maven 3.9+.
- **Docker:** Docker Engine or Docker Desktop with BuildKit and Docker Compose v2. Java and Maven do not need to be installed on the host for this path.

Both paths need an available host port (8080 by default). The first build needs access to Maven Central; Docker also needs access to the base-image registry.

## Run with Maven

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

## Run with Docker

From the repository root:

```bash
docker compose up --build
```

This builds the project, runs the full test suite with JaCoCo, and starts the API. For background execution with a readiness check:

```bash
docker compose up --build -d --wait
docker compose ps
docker compose logs -f app
```

The API is exposed on `http://localhost:8080`:

- [Swagger UI](http://localhost:8080/swagger-ui/index.html).
- [OpenAPI JSON](http://localhost:8080/v3/api-docs).
- [OpenAPI YAML](http://localhost:8080/v3/api-docs.yaml).

If port 8080 is already in use, select a different host port (the container still listens on 8080):

```bash
APP_PORT=8086 docker compose up --build -d --wait
```

Then use `http://localhost:8086` for the same paths. To stop the service and remove its container/network:

```bash
docker compose down
```

For a temporary stop without removing the container, use `docker compose stop`. H2 remains in memory, so application stops/restarts lose all data; there is no database container or persistence volume.

### Container design

- The build stage uses `maven:3.9.11-eclipse-temurin-21` and runs `mvn clean verify`, with no skipped tests. A BuildKit cache reuses downloaded Maven artifacts; unchanged Docker build layers may also be reused normally.
- The final image uses `eclipse-temurin:21-jre-alpine` and contains only the JRE and application JAR. Both base images are pinned by digest in the Dockerfile; refresh those digests deliberately for updates.
- Runtime UID/GID is `10001:10001`. Compose uses a read-only root filesystem, a writable temporary `/tmp`, dropped Linux capabilities, and `no-new-privileges`.
- The health check requests `/v3/api-docs` locally. The process receives normal Docker shutdown signals, with a 20-second stop grace period.
- `.dockerignore` includes only the build inputs (`pom.xml` and `src`) and Dockerfile, excluding Git history, local artifacts, and unrelated workspace files.
- JaCoCo is generated in the build stage, not bundled with the running API. Run `mvn clean verify` locally to inspect `target/site/jacoco/index.html` on the host.

### Builds behind a proxy

Ordinary local builds require no extra files. Restricted environments may need Maven proxy settings and an additional Java trust store. The Dockerfile accepts optional BuildKit secrets named `maven_settings` and `java_cacerts`. They are mounted only for the Maven build step; neither is copied into an image layer or the final JAR.

Example with environment-specific files outside the repository:

```bash
docker build \
  --secret id=maven_settings,src=/path/to/settings.xml \
  --secret id=java_cacerts,src=/path/to/cacerts \
  -t spring-boot-bug-fix-lab:local .
docker compose up -d --wait --no-build
```

For Compose builds in that environment, supply the same secrets through a local Compose override. Keep credentials, local proxy addresses, and trust stores out of version control, and keep TLS verification enabled. Standard users can continue to use `docker compose up --build` directly.

## Tests and build

```bash
mvn test
mvn package
```

Both commands run all 29 tests successfully, including the three original bug regression scenarios, stock boundary tests, customer/product update checks, and the generated OpenAPI contract. Unit tests use Mockito to test order calculations and missing products. Integration tests use the full Spring context, MockMvc, and a real H2 database to verify CRUD operations, validation, duplicate emails, referential integrity, price snapshots, and transaction rollback. The database is cleaned before each integration test.

## Test coverage

JaCoCo 0.8.13 generates HTML, XML, and CSV reports during the normal test phase. To regenerate coverage and verify the complete build:

```bash
mvn clean verify
```

Open `target/site/jacoco/index.html`. The latest clean run passed all 29 tests: instructions 99.51%, branches 100.00%, lines 98.90%, methods 99.05%, and classes 100.00%. No coverage exclusions or skipped tests are configured. These counters do not imply exhaustive behavior coverage.

See [testing strategy, measured counters, and known gaps](docs/testing.md). Generated reports remain under ignored `target/`.

## Interactive API documentation

The application uses `springdoc-openapi-starter-webmvc-ui` 2.8.13, compatible with Spring Boot 3.5. Start it with `mvn spring-boot:run` or the packaged JAR, then open:

- [Swagger UI](http://localhost:8080/swagger-ui/index.html) (also available through `/swagger-ui.html`).
- [OpenAPI JSON](http://localhost:8080/v3/api-docs).
- [OpenAPI YAML](http://localhost:8080/v3/api-docs.yaml).

The specification is titled **Spring Boot Bug Fix Lab API**, version **0.0.1**, and documents all 15 operations under Customers, Products, and Orders. It includes operation descriptions, path parameters, validated request schemas, examples, success responses, and relevant 400/404/409 errors. Delete operations return 204. Error schemas describe Problem Details with an optional top-level `errors` array for validation failures.

Use **Try it out** to create a customer and product first, then copy their returned IDs into the order example. For a reproducible demonstration:

1. Create a product priced at 10.00 with stock 2.
2. Request five units in `POST /api/orders`: expect 409 and no saved order.
3. Request two units: expect 201 and total 20.00; retrieve the order with GET.
4. Replace its quantity with one using PUT: expect 200 and total 10.00.
5. Request `GET /api/orders/999999`: expect 404 if that ID does not exist.

Requests made through Swagger modify the same in-memory data as any API client. Stock is checked on creation only; order replacement uses current prices, and stock is not reserved or depleted. Examples are illustrative; always use IDs returned by your session.

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
  -d '{"name":"Keyboard","price":19.99,"stock":100}'

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

## Intended behavior and current exceptions

- Customer and product names are required, trimmed, and limited to 100 characters.
- Customer emails must be valid, at most 254 characters, and unique without regard to case. Emails are stored in lowercase.
- Product stock accepts non-negative integers. Omitted or null stock defaults to zero on creation and preserves the current value on update for compatibility with existing requests. Order creation rejects quantities above stock with HTTP 409, including cumulative quantities across repeated product lines. Order updates retain their existing behavior. Inventory reservation and depletion are not implemented.
- Product prices must be at least `0.01`, with at most 10 integer digits and 2 decimal places.
- Orders require an existing customer and 1–100 non-null items. Each item requires an existing product and an integer quantity from 1 to 1,000,000.
- Monetary values use `BigDecimal`. The server calculates subtotals and initial totals from product prices; clients cannot submit prices or totals on orders. Replacing items resets the previous total and rebuilds it from the current items using their stored unit prices.
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

The H2 database is in memory: all data is lost when the application stops. Schema creation is automatic for this development baseline. There is no authentication, frontend, payment processing, inventory reservation, or order status workflow. Docker Compose provides a single-container H2 demonstration. List endpoints are unpaginated and intended for small portfolio datasets. Production persistence and deployment hardening are outside this initial version's scope.

## Portfolio Evidence

See the [portfolio evidence register](docs/portfolio/portfolio-evidence.md) for verified milestones, regression results, and the screenshot capture plan.
