# Bug 3: Missing order error handling

**Status: fixed on October 5, 2026. All three documented bugs are now fixed.** Historical reproduction is preserved below.

## Fix evidence

### Root cause and minimal correction

`OrderService.findById` called `Optional.orElseThrow()` without a custom exception. For an absent order this threw `NoSuchElementException`, which the global handler did not map to 404; the server returned 500.

The method now delegates to the existing `requireById(id)` helper. That helper throws `ResourceNotFoundException`, already mapped by `GlobalExceptionHandler` to HTTP 404 Problem Details. The production change is one line. No controller handling, new exception mapping, test changes, stock logic, or total logic was needed.

### Before and after

Associated regression: `OrderRegressionTest.returnsNotFoundWhenOrderDoesNotExist`.

```bash
mvn -Dtest=OrderRegressionTest#returnsNotFoundWhenOrderDoesNotExist test
```

- Before the fix: 1 test failed, expected HTTP 404, received HTTP 500.
- After the fix: 1 test passed, 0 failures/errors/skips, with the same assertions.
- `mvn test`: **26 passed, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
- `mvn package`: **26 passed, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. The executable JAR was generated without skipping tests.
- The previously failing service test and GET-after-delete integration check also pass.

### Actual HTTP verification

The JAR from the successful full build was started with:

```bash
java -jar target/spring-boot-bug-fix-lab-0.0.1-SNAPSHOT.jar --server.port=8083
```

Port 8083 isolated this run from earlier demonstration servers. Spring and Tomcat reported successful startup. Recorded request on October 5, 2026:

```bash
curl -i http://localhost:8083/api/orders/999999
```

Actual response: **HTTP 404 Not Found**, Content-Type `application/problem+json`.

```json
{"type":"about:blank","title":"Not Found","status":404,"detail":"Order not found: 999999","instance":"/api/orders/999999"}
```

Thirteen actual HTTP requests passed, including fixture creation and these checks:

| Scenario | Recorded result |
| --- | --- |
| GET nonexistent order 999999 | 404 with Problem Details. |
| POST order with stock 2 and quantity 5 | 409; subsequent GET order list returned `[]`. |
| POST valid order with two units at 10.00 | 201 and total 20.00. |
| GET that existing order | 200 and total 20.00. |
| PUT quantity five | 200 and total 50.00. |
| Follow-up GET | 200 and total 50.00. |
| Repeat the five-unit PUT | 200 and total remains 50.00. |
| DELETE the order, then GET it | 204, then 404. |

Stock rejection and total recalculation remain correct. No tests were removed, changed, disabled, or weakened.

### Pending screenshots

No screenshots were captured in this task. The real results are ready for later capture as `06-tests-passing-after-fixes.png` and `10-order-not-found-404.png`. Retain `04-missing-order-500.png` as a historical pre-fix capture to be taken from a genuine rerun of commit `7dc887d2672562c93b1f84a3dcd5dd82ad5a2193`, not the current application.

## Historical evidence before the fix

The following sections describe the original defect at a pre-fix revision. They are not the current behavior.


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
