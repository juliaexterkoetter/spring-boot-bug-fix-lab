# Bug 1: Insufficient stock

**Status: fixed for order creation on October 5, 2026.** Historical reproduction is retained below. Bugs 2 and 3 remain intentionally unfixed.

## Fix evidence

### Root cause and correction

`OrderService.create` delegated to `replaceItems`, which resolved products and added order items without reading available stock. The corrected creation path sums requested quantities by product ID, compares the cumulative quantity with `Product.getStock()`, and throws the existing `ConflictException` before `saveAndFlush` when stock is insufficient. The existing global handler already maps that exception to HTTP 409; no new exception or handler was necessary.

Repeated lines for the same product are included in the availability check. An order requesting exactly the available stock remains valid. Validation runs inside the existing transaction, before any order is persisted. Inventory is not decremented or reserved, and the update path is unchanged; those workflows are outside this correction.

### Before and after

Associated regression test: `OrderRegressionTest.rejectsOrderWhenQuantityExceedsAvailableStock`.

- Before correction: the targeted test failed; expected HTTP 409 / zero orders, actual HTTP 201 / one order.
- After correction: the same assertions passed (1 test, 0 failures/errors/skips).
- Added boundary tests pass: `acceptsOrderWhenQuantityEqualsAvailableStock` and `rejectsOrderWhenRepeatedProductLinesExceedAvailableStock`.
- Existing calculation and rollback test fixtures now explicitly provide sufficient stock. Their assertions were not modified, removed, or disabled.
- `mvn test`: 26 tests, 22 passes, 4 failures, 0 errors/skips.
- `mvn package`: fails at the test phase with the same four failures from bugs 2 and 3. Compilation succeeds, but the normal build is not green.
- `mvn -DskipTests package`: succeeds solely to produce the runnable verification JAR; this is not a successful full build.

### Actual HTTP verification after the fix

The newly packaged application started on port 8081 using `java -jar target/spring-boot-bug-fix-lab-0.0.1-SNAPSHOT.jar --server.port=8081`. Port 8081 separated this run from the earlier demonstration server. On a fresh database, create customer ID 1 and product ID 1 priced at 10.00 with stock 2, then send:

```bash
curl -i -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":1,"items":[{"productId":1,"quantity":5}]}'
```

Recorded response: **HTTP 409 Conflict**.

```json
{"type":"about:blank","title":"Conflict","status":409,"detail":"Insufficient stock for product: 1","instance":"/api/orders"}
```

Immediately afterward, `GET /api/orders` returned **HTTP 200** with `[]`, proving that the rejected request did not persist an order. A request containing repeated product lines with quantities 1 and 2 also returned 409 and left the list empty. Quantity 2 then returned 201 with total 20.00.

The same HTTP session confirmed bugs 2 and 3 remain: after increasing available stock to 100, changing order quantity to 5 still returned total 70.00; retrieving order 999999 still returned 500.

### Pending screenshots

No screenshot was captured in this task. Capture `09-stock-validation-error.png` later from a genuine execution showing stock 2, quantity 5, HTTP 409, and no persisted order. Preserve the historical before-fix capture plan for `02-insufficient-stock-bug.png` by using a pre-fix commit. Do not present the currently failing full suite as all-green.

## Historical evidence before the fix

The remaining sections describe the original defect and its pre-fix HTTP responses. To reproduce that behavior, use bug-introduction commit `071ae40da3ab173ad6b2e7b68dd2d281ae03d55c`; the current creation path rejects it.

## Affected endpoint

`POST /api/orders`

## Expected behavior

Reject an order when the requested quantity exceeds the product's available stock, return `409 Conflict`, and do not persist the order.

## Actual behavior

A product with stock `2` accepts an order for `5` units. The API returns `201 Created`, and a subsequent GET confirms that the order was persisted.

`stock` was added to products to make this scenario explicit. It accepts non-negative integers, defaults to zero on creation when omitted, and remains unchanged on product updates when omitted. The exercise concerns the availability check; inventory reservation, depletion, and restocking workflows are outside this version's scope.

## Reproduction setup

Run this intentionally faulty version from the repository root:

```bash
mvn -DskipTests package
java -jar target/spring-boot-bug-fix-lab-0.0.1-SNAPSHOT.jar
```

Tests are skipped **only to obtain a runnable demonstration JAR**. `mvn test` is expected to fail; no tests have been removed, disabled, or changed. Stop any previous server before starting this one. H2 is in memory, so restarting resets the data.

Use the IDs returned by your requests. IDs below are from the actual recorded session; they are not universal constants. JSON responses below are formatted from the captured HTTP responses. Timestamps and item IDs will differ on later runs.

## Reproduction steps and sample requests

1. Create a customer:

```bash
curl -i -X POST http://localhost:8080/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Debug Customer","email":"debug@example.com"}'
```

2. Create a product with only two available units:

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Limited Keyboard","price":10.0,"stock":2}'
```

3. Use the returned IDs to request five units:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":1,"items":[{"productId":1,"quantity":5}]}'
```

4. Retrieve the returned order ID to confirm persistence:

```bash
curl -i -X GET http://localhost:8080/api/orders/1
```

## Sample responses and actual reproduction results

Recorded on October 5, 2026 against the packaged application on port 8080.

Product creation response:

HTTP status: `201`; Content-Type: `application/json`.

```json
{
  "id": 1,
  "name": "Limited Keyboard",
  "price": 10.0,
  "stock": 2
}
```
Order creation response:

HTTP status: `201`; Content-Type: `application/json`.

```json
{
  "id": 1,
  "customerId": 1,
  "createdAt": "2026-10-05T20:32:11.099872771Z",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "quantity": 5,
      "unitPrice": 10.0,
      "subtotal": 50.0
    }
  ],
  "total": 50.0
}
```
Follow-up order retrieval:

HTTP status: `200`; Content-Type: `application/json`.

```json
{
  "id": 1,
  "customerId": 1,
  "createdAt": "2026-10-05T20:32:11.099873Z",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "quantity": 5,
      "unitPrice": 10.0,
      "subtotal": 50.0
    }
  ],
  "total": 50.0
}
```

## Suspected area of the code

- `src/main/java/com/example/bugfixlab/service/OrderService.java`: `create` and `replaceItems` resolve products and create items without checking `Product.getStock()`.
- `src/main/java/com/example/bugfixlab/entity/Product.java`: available stock is stored but not enforced during order creation.

## Existing test impact

No existing test covers stock availability, since the baseline had no stock field. This bug is therefore uncovered by the existing suite. The full unchanged suite reports 19 passes and 2 failures caused by bugs 2 and 3. A future regression test should assert rejection and verify that no order was persisted.
