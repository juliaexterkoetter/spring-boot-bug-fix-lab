# Running the application

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

The API is published on loopback only at `http://localhost:8080`:

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

## Scope and limitations

The H2 database is in memory: all data is lost when the application stops. Schema creation is automatic for this development baseline. There is no authentication, frontend, payment processing, inventory reservation, or order status workflow. Docker Compose provides a single-container H2 demonstration. List endpoints are unpaginated and intended for small portfolio datasets. Production persistence and deployment hardening are outside this initial version's scope.

