# Bug 1: Insufficient stock

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
