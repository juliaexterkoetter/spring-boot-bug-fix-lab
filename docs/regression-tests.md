# Regression tests for the debugging lab

These tests protect the correct behavior of the three documented bugs. They intentionally fail against the current production code. No production code, existing test, or Maven test configuration was changed, and none of the new tests accepts the faulty behavior as correct.

## Test design and isolation

All three scenarios are in:

`src/test/java/com/example/bugfixlab/regression/OrderRegressionTest.java`

- `@SpringBootTest(webEnvironment = RANDOM_PORT)` starts embedded Tomcat on an available port. `TestRestTemplate` sends real HTTP requests, including the server's error dispatch for the missing-order scenario.
- A dedicated H2 database (`order-regression`) separates these tests from the existing integration suite.
- Before each test, orders, customers, and products are removed in foreign-key-safe order. Each test creates its own fixtures and has no dependency on execution order or another test's IDs.
- Fixtures are committed through real JPA repositories before requests are sent. Tests are not wrapped in test-managed transactions because the HTTP server uses separate threads and transactions.
- No mocks are used. Repositories inspect persisted state, and a separate GET verifies the committed updated total.
- Grouped assertions report multiple symptoms of the same defect without preventing the other test scenarios from running. They do not change the test count or weaken the expected behavior.

## Bug 1: Insufficient stock

- Related bug: [Insufficient stock](bugs/01-insufficient-stock.md).
- Test: `OrderRegressionTest.rejectsOrderWhenQuantityExceedsAvailableStock`.
- File: `src/test/java/com/example/bugfixlab/regression/OrderRegressionTest.java`.
- Scenario: create a product with stock `2` and price `10.00`, then send `POST /api/orders` requesting `5` units for an existing customer.
- Expected behavior: return `409 Conflict`, as specified in the bug report, and persist no order.
- Actual behavior: return `201 Created` and persist one order containing five units.
- Reason for failure: order creation never checks requested quantity against product stock. Both the HTTP-status assertion and the database-count assertion fail.

Actual assertion results:

```text
Order creation status: expected 409 CONFLICT, actual 201 CREATED
Persisted orders after rejection: expected 0, actual 1
```

## Bug 2: Order total recalculation

- Related bug: [Order total recalculation](bugs/02-order-total-recalculation.md).
- Test: `OrderRegressionTest.recalculatesOrderTotalWhenItemQuantityChanges`.
- File: `src/test/java/com/example/bugfixlab/regression/OrderRegressionTest.java`.
- Scenario: create a product priced at `10.00` with stock `100` to avoid insufficient stock. Create an order with two units, verify its initial total is `20.00`, and replace the quantity with five through `PUT /api/orders/{id}`. Retrieve the order through a separate GET.
- Expected behavior: both the update response and the persisted order report total `50.00`.
- Actual behavior: both return `70.00`. The persisted item correctly contains quantity `5`, unit price `10.00`, and subtotal `50.00`; those assertions pass.
- Reason for failure: `clearItems` does not reset the stored total, so the new subtotal `50.00` is added to the previous total `20.00`. Both total assertions fail.

Actual assertion results:

```text
Total returned by PUT: expected 50.00, actual 70.00
Persisted total returned by GET: expected 50.00, actual 70.00
```

## Bug 3: Missing order error handling

- Related bug: [Missing order error handling](bugs/03-missing-order-error-handling.md).
- Test: `OrderRegressionTest.returnsNotFoundWhenOrderDoesNotExist`.
- File: `src/test/java/com/example/bugfixlab/regression/OrderRegressionTest.java`.
- Scenario: verify that order ID `999999` does not exist, then send `GET /api/orders/999999`.
- Expected behavior: HTTP `404 Not Found`.
- Actual behavior: HTTP `500 Internal Server Error`, with the default error body identifying `/api/orders/999999`.
- Reason for failure: `OrderService.findById` throws an unhandled `NoSuchElementException` rather than the mapped `ResourceNotFoundException`. Real HTTP verifies the returned status rather than merely asserting that a Java exception escaped a mock request.

Actual assertion result:

```text
Missing order status: expected 404 NOT_FOUND, actual 500 INTERNAL_SERVER_ERROR
```

## Actual full-suite results

Executed on October 5, 2026 with Java 21 and Maven 3.9.11 from the repository root:

```bash
mvn test
```

Result: **BUILD FAILURE**, exit code **1**, caused by the expected assertion failures.

| Test class | Executed | Passed | Failed | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| `ApiIntegrationTest` | 17 | 16 | 1 | 0 | 0 |
| `OrderServiceTest` | 4 | 3 | 1 | 0 | 0 |
| `OrderRegressionTest` | 3 | 0 | 3 | 0 | 0 |
| **Total** | **24** | **19** | **5** | **0** | **0** |

Maven's reported counts include individual parameterized test invocations. Multiple failed assertions inside one regression test count as one failed test.

The two pre-existing failures remain unchanged:

| Existing test | Failure |
| --- | --- |
| `ApiIntegrationTest.orderCrudPreservesPricesAndProtectsReferences` | Expected total `89.97`, actual `129.95` (bug 2). |
| `OrderServiceTest.reportsMissingOrder` | Expected `ResourceNotFoundException`, actual `NoSuchElementException` (bug 3). |

Before this change the suite had 21 tests, 19 passes, and these two failures. After adding the three tests, the original 19 still pass, the original two still fail, and each new test fails for its intended bug. There are no additional unrelated failures, initialization errors, or skipped tests. Production sources and the two original test files were verified unchanged with Git.

Surefire reports are generated under `target/surefire-reports/` and are not committed. All three defects remain unfixed.

## Running a specific scenario

From the repository root:

```bash
mvn -Dtest=OrderRegressionTest test
mvn -Dtest=OrderRegressionTest#rejectsOrderWhenQuantityExceedsAvailableStock test
mvn -Dtest=OrderRegressionTest#recalculatesOrderTotalWhenItemQuantityChanges test
mvn -Dtest=OrderRegressionTest#returnsNotFoundWhenOrderDoesNotExist test
```

These commands are provided for future diagnosis; the recorded results above come from the complete `mvn test` run. All three tests should stay enabled and retain their correct-behavior assertions when the bugs are fixed in a later task.
