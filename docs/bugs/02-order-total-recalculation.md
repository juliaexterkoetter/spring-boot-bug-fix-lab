# Bug 2: Order total recalculation

**Status: fixed on October 5, 2026.** The missing-order error-handling bug was still open at that milestone; it is now fixed, as recorded in the [final regression report](../regression-tests.md).

## Fix evidence

### Root cause and minimal correction

`Order.clearItems()` removed old items but retained their accumulated total. `OrderService.update` then added replacement items through `addItem`, adding their subtotals to that stale value. Two units at 10.00 followed by five units produced 20.00 + 50.00 = 70.00.

`clearItems()` now resets `total` to 0.00 when clearing the collection. The existing `addItem` and `OrderItem.getSubtotal` calculation is reused to build the replacement total from current items. No calculation was duplicated, no service or exception mapping changed, and no test source or assertion was modified.

The existing price contract is preserved: each item stores its unit price; changing a product price alone does not change existing orders. A PUT replaces items and uses the current product prices. No stock or missing-order behavior was changed.

### Before and after tests

Associated test: `OrderRegressionTest.recalculatesOrderTotalWhenItemQuantityChanges`.

```bash
mvn -Dtest=OrderRegressionTest#recalculatesOrderTotalWhenItemQuantityChanges test
```

Before the fix: 1 failure, because both PUT and GET totals were 70.00 instead of 50.00. After the fix: 1 test passed, 0 failures/errors/skips, with the exact same assertions.

Both `mvn test` and `mvn package` executed 26 tests: **23 passed, 2 assertion failures, 1 error, 0 skipped**. Both commands exited 1 and reported BUILD FAILURE solely because of the open missing-order bug:

- `OrderServiceTest.reportsMissingOrder`: wrong exception type.
- `OrderRegressionTest.returnsNotFoundWhenOrderDoesNotExist`: HTTP 500 instead of 404.
- `ApiIntegrationTest.orderCrudPreservesPricesAndProtectsReferences`: its total and price assertions now pass, exposing the later GET-after-delete request that throws `NoSuchElementException`. MockMvc reports this as an error rather than an assertion failure.

Compilation succeeded, but the full build did not pass. No `-DskipTests` command was used in this task. For HTTP verification, the application was started from current compiled sources with:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

This development startup is not evidence of a successful final build.

### Actual HTTP verification

Recorded on October 5, 2026 on port 8082. A fresh customer and product were created; the product had price 10.00 and stock 100. The POST for two units returned HTTP 201 and total 20.00. The following request returned HTTP 200:

```bash
curl -i -X PUT http://localhost:8082/api/orders/1 \
  -H 'Content-Type: application/json' \
  -d '{"customerId":1,"items":[{"productId":1,"quantity":5}]}'
```

Captured response (formatted):

```json
{
  "id": 1,
  "customerId": 1,
  "createdAt": "2026-10-05T20:52:46.693260Z",
  "items": [
    {
      "id": 2,
      "productId": 1,
      "quantity": 5,
      "unitPrice": 10.0,
      "subtotal": 50.0
    }
  ],
  "total": 50.0
}
```

A separate GET returned the same total 50.00. Additional real HTTP checks confirmed:

| Scenario | Actual result |
| --- | --- |
| Repeat the same five-unit PUT | Total remains 50.00; no accumulation. |
| Replace with one unit at 10.00 and two units of another product at 2.50 | PUT and GET total 15.00. |
| Change the first product price to 12.00 without changing the order | Existing order remains 15.00 with its stored unit prices. |
| Replace that order with five units of the first product | PUT and GET total 60.00, using current price 12.00. |
| Request 101 units against stock 100 | HTTP 409; the stock fix is preserved. |
| GET nonexistent order 999999 | HTTP 500; bug 3 remains open. |

### Pending screenshots

No screenshots were captured. Capture the passing targeted regression and real 20.00-to-50.00 PUT/GET result later as `12-order-total-corrected.png`; include a repeated update to show the absence of accumulation. Capture the pre-fix `03-order-total-bug.png` only from a genuine rerun of a pre-fix commit. The full-suite all-green screenshot remains blocked by bug 3.

## Historical evidence before the fix

The following sections describe the original defect. Reproduce them using commit `fa7589ac473499f50e385e4055f1039c5736b3ef` or an earlier bug-introduction revision, not the corrected current code.


## Affected endpoint

`PUT /api/orders/{id}`; the incorrect total is also visible through subsequent order GET requests.

## Expected behavior

After replacing a single item's quantity from `2` to `5` at a unit price of `10.00`, the item subtotal and order total should both be `50.00`.

## Actual behavior

The item quantity and subtotal update correctly, but the order total becomes `70.00`: the previous `20.00` is retained and the new `50.00` is added. A subsequent GET still returns `70.00`, confirming the incorrect total is persisted. Repeated replacements keep accumulating the previous total.

## Reproduction setup

Run this intentionally faulty version from the repository root:

```bash
mvn -DskipTests package
java -jar target/spring-boot-bug-fix-lab-0.0.1-SNAPSHOT.jar
```

Tests are skipped **only to obtain a runnable demonstration JAR**. `mvn test` is expected to fail; no tests have been removed, disabled, or changed. Stop any previous server before starting this one. H2 is in memory, so restarting resets the data.

Use the IDs returned by your requests. IDs below are from the actual recorded session; they are not universal constants. JSON responses below are formatted from the captured HTTP responses. Timestamps and item IDs will differ on later runs.

## Reproduction steps and sample requests

1. Create a customer (or reuse one from bug 1) and note its ID:

```bash
curl -i -X POST http://localhost:8080/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Debug Customer","email":"debug@example.com"}'
```

2. Create a separate product with stock 100, avoiding the insufficient-stock scenario:

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Standard Cable","price":10.0,"stock":100}'
```

3. Create an order with two units:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":1,"items":[{"productId":2,"quantity":2}]}'
```

4. Replace the quantity with five units using the returned order ID:

```bash
curl -i -X PUT http://localhost:8080/api/orders/2 \
  -H 'Content-Type: application/json' \
  -d '{"customerId":1,"items":[{"productId":2,"quantity":5}]}'
```

5. Retrieve that order again:

```bash
curl -i -X GET http://localhost:8080/api/orders/2
```

## Sample responses and actual reproduction results

Recorded on October 5, 2026. The product price remained `10.00` throughout the reproduction.

Initial order:

HTTP status: `201`; Content-Type: `application/json`.

```json
{
  "id": 2,
  "customerId": 1,
  "createdAt": "2026-10-05T20:32:11.169123047Z",
  "items": [
    {
      "id": 2,
      "productId": 2,
      "quantity": 2,
      "unitPrice": 10.0,
      "subtotal": 20.0
    }
  ],
  "total": 20.0
}
```
Update response:

HTTP status: `200`; Content-Type: `application/json`.

```json
{
  "id": 2,
  "customerId": 1,
  "createdAt": "2026-10-05T20:32:11.169123Z",
  "items": [
    {
      "id": 3,
      "productId": 2,
      "quantity": 5,
      "unitPrice": 10.0,
      "subtotal": 50.0
    }
  ],
  "total": 70.0
}
```
Follow-up retrieval:

HTTP status: `200`; Content-Type: `application/json`.

```json
{
  "id": 2,
  "customerId": 1,
  "createdAt": "2026-10-05T20:32:11.169123Z",
  "items": [
    {
      "id": 3,
      "productId": 2,
      "quantity": 5,
      "unitPrice": 10.0,
      "subtotal": 50.0
    }
  ],
  "total": 70.0
}
```

## Suspected area of the code

- `src/main/java/com/example/bugfixlab/entity/Order.java`: `total` is now persisted and incremented by `addItem`. `clearItems` removes the items but does not reset the total.
- `src/main/java/com/example/bugfixlab/service/OrderService.java`: `update` calls `replaceItems`, which clears items and adds their replacements.

## Existing test impact

`ApiIntegrationTest.orderCrudPreservesPricesAndProtectsReferences` fails at its total assertion: expected `89.97`, actual `129.95` (old total `39.98` plus new subtotal `89.97`). Assertions after that failure do not run. Initial order calculation and price snapshot unit tests still pass. No test was changed to accommodate the defect.
