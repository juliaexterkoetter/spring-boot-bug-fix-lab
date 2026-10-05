# Spring Boot Bug Fix Lab

A small Java REST API demonstrating how to reproduce, diagnose, and fix Spring Boot bugs with regression tests. This is a controlled portfolio exercise, not a client production incident.

**29 passing tests · Three documented fixes · 98.90% line coverage · Docker and Swagger verified**

## What this project demonstrates

Traceable debugging: functional baseline → controlled bugs → failing regression tests → minimal fixes → green suite → coverage → Swagger → Docker. Historical commits and real HTTP results preserve the evidence behind each milestone.

## Main features

- Customer, product, and order CRUD with validated DTOs and consistent Problem Details errors.
- Transactional order writes, price snapshots, stock validation on creation, and correct total recalculation.
- Interactive API documentation and a single-container demonstration using H2.

## Debugging case study

| Controlled bug | Corrected behavior | Evidence |
| --- | --- | --- |
| Insufficient stock accepted | HTTP 409; no order persisted | [Diagnosis and fix](docs/bugs/01-insufficient-stock.md) |
| Previous total accumulated on update | Two units at 10.00 → five units: total 50.00 | [Diagnosis and fix](docs/bugs/02-order-total-recalculation.md) |
| Unknown order returned 500 | HTTP 404 through global exception handling | [Diagnosis and fix](docs/bugs/03-missing-order-error-handling.md) |

[Regression history](docs/regression-tests.md) records the failing tests and their eventual success without weakened assertions.

## Technologies

Java 21 · Spring Boot 3.5.6 · Maven 3.9+ · Spring Web/Data JPA/Validation · H2 · JUnit 5/Mockito/MockMvc · JaCoCo 0.8.13 · springdoc 2.8.13 · Docker Compose v2.

## How to run

Requires Java 21 **JDK** and Maven 3.9+, or Docker with Compose v2. Run from the repository root:

```bash
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. H2 data disappears when the application stops. See the [runbook](docs/running.md) for JAR execution, curl examples, proxy setup, and detailed API rules.

## Docker

```bash
docker compose up --build
# Stop and remove the container/network:
docker compose down
```

No host Java installation is needed. The build runs all tests. The digest-pinned Java 21 JRE image runs as a non-root user with a read-only filesystem, restricted capabilities, and an HTTP health check. The published port is loopback-only. Use `APP_PORT=8086 docker compose up --build -d --wait` if 8080 is occupied.

## Testing and coverage

```bash
mvn clean verify
```

All **29 tests pass**, with no failures, errors, or skips. Unit, integration, and real HTTP regression tests cover business rules, persistence, rollback, and the OpenAPI contract.

JaCoCo report: `target/site/jacoco/index.html`. Instructions **99.51%**, branches **100%**, lines **98.90%**, methods **99.05%**, classes **100%**. These counters do not imply exhaustive behavior coverage. See [testing strategy and known gaps](docs/testing.md).

## API documentation

- [Swagger UI](http://localhost:8080/swagger-ui/index.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)

All 15 operations include request schemas, examples, and relevant HTTP responses. In Swagger, create a customer and product first, then use their returned IDs to create an order. Stock 2 with quantity 5 demonstrates the 409 response; an absent order ID demonstrates 404.

## Important endpoints

| Resource | Collection: GET / POST | Item: GET / PUT / DELETE |
| --- | --- | --- |
| Customers | `/api/customers` | `/api/customers/{id}` |
| Products | `/api/products` | `/api/products/{id}` |
| Orders | `/api/orders` | `/api/orders/{id}` |

Success statuses: 200, 201, 204. Errors: 400 for invalid input, 404 for missing resources, 409 for conflicts.

## Portfolio evidence

[Evidence register](docs/portfolio/portfolio-evidence.md) · [Final audit](docs/portfolio/final-audit.md) · [Screenshot plan](docs/portfolio/screenshot-plan.md). Screenshots remain pending.

## Project structure

```text
src/main/java/com/example/bugfixlab/
  config/       OpenAPI configuration
  controller/   HTTP endpoints
  service/      Business rules and transactions
  repository/   JPA persistence
  entity/       Customer, Product, Order, OrderItem
  dto/          Validated requests and responses
  exception/    Global error handling
src/test/       Unit, integration, and regression tests
docs/           Debugging history, testing, and portfolio evidence
```

Scope: a local portfolio demo with temporary H2 data, no authentication or frontend, and unpaginated lists. Stock is checked only on creation, without reservation/depletion. Order replacement uses current product prices; existing orders retain their snapshots until replaced.
