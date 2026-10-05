# Bug 3: Missing order error handling

## Affected endpoint

`GET /api/orders/{id}`

## Expected behavior

An unknown order ID should return `404 Not Found` with the API's Problem Details response.

## Actual behavior

The same lookup returns `500 Internal Server Error` with Spring Boot's default error JSON. This happens because an unhandled `NoSuchElementException` escapes the service.

## Reproduction setup

Run this intentionally faulty version from the repository root:

```bash
mvn -DskipTests package
java -jar target/spring-boot-bug-fix-lab-0.0.1-SNAPSHOT.jar
```

Tests are skipped **only to obtain a runnable demonstration JAR**. `mvn test` is expected to fail; no tests have been removed, disabled, or changed. Stop any previous server before starting this one. H2 is in memory, so restarting resets the data.

Use the IDs returned by your requests. IDs below are from the actual recorded session; they are not universal constants. JSON responses below are formatted from the captured HTTP responses. Timestamps and item IDs will differ on later runs.

## Reproduction steps and sample request

1. Start the application.
2. Choose an order ID that does not exist (the recorded session used `999999`). No customer or product setup is required.
3. Send the following request:

```bash
curl -i -X GET http://localhost:8080/api/orders/999999
```

## Sample response and actual reproduction result

Recorded on October 5, 2026 against the running embedded Tomcat server:

HTTP status: `500`; Content-Type: `application/json`.

```json
{
  "timestamp": "2026-10-05T20:32:11.213+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/orders/999999"
}
```
The application log recorded `java.util.NoSuchElementException: No value present` from `OrderService.findById`.

## Suspected area of the code

- `src/main/java/com/example/bugfixlab/service/OrderService.java`: `findById` uses `Optional.orElseThrow()` without providing the `ResourceNotFoundException` used by the shared `requireById` method.
- `src/main/java/com/example/bugfixlab/exception/GlobalExceptionHandler.java` (unchanged): maps `ResourceNotFoundException` to 404, but has no mapping for `NoSuchElementException`.

## Scope checks

Actual HTTP requests confirmed that nonexistent customers and products still return 404. `PUT /api/orders/999999` with a valid request body and `DELETE /api/orders/999999` also still return 404, because they retain the original `requireById` path.

## Existing test impact

`OrderServiceTest.reportsMissingOrder` fails: expected `ResourceNotFoundException`, actual `NoSuchElementException`. No existing test was edited. The existing order integration test stops at bug 2's earlier total assertion, so its later missing-order assertion is not reached; the 500 response was verified separately against the actual running server.
