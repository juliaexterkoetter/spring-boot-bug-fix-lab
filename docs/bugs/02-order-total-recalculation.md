# Bug 2: Order total recalculation

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
