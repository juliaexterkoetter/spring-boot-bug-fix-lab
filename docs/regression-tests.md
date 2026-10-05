# Regression tests for the debugging lab

Current suite: **29 passing tests**, including all five order regressions. See [testing and coverage](testing.md) and the [final audit](portfolio/final-audit.md). The results below retain their original milestone counts.

## Historical bug-fix milestone: missing-order handling fixed

Validated on October 5, 2026. All three documented bugs are fixed. Production changed only in `OrderService.findById`, which now calls the existing `requireById` helper instead of throwing an unmapped `NoSuchElementException`. The global `ResourceNotFoundException` handler now produces 404 without duplicate controller logic. No test source or assertion changed.

The targeted command `mvn -Dtest=OrderRegressionTest#returnsNotFoundWhenOrderDoesNotExist test` failed before the fix (expected 404, actual 500), then passed afterward: 1 test, 0 failures/errors/skips.

Actual results from both `mvn test` and `mvn package`:

| Suite | Executed | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| `ApiIntegrationTest` | 17 | 17 | 0 | 0 | 0 |
| `OrderServiceTest` | 4 | 4 | 0 | 0 | 0 |
| `OrderRegressionTest` | 5 | 5 | 0 | 0 | 0 |
| **Total** | **26** | **26** | **0** | **0** | **0** |

Both commands exited 0 and reported BUILD SUCCESS. Packaging generated the executable JAR with all tests enabled; no `-DskipTests` flag was used. All original test methods and assertions are preserved.

The JAR started successfully on port 8083. Thirteen real HTTP requests verified missing orders return 404, existing orders return 200, creation returns 201, updates return 200, insufficient stock returns 409 without persistence, and totals change from 20.00 to 50.00 without accumulating on repeated PUT. Delete followed by GET returned 204 then 404. See [the final fix evidence](bugs/03-missing-order-error-handling.md#fix-evidence).

The three original regression scenarios now pass:

- `rejectsOrderWhenQuantityExceedsAvailableStock`: HTTP 409 and no persisted order.
- `recalculatesOrderTotalWhenItemQuantityChanges`: PUT and GET total 50.00.
- `returnsNotFoundWhenOrderDoesNotExist`: HTTP 404.

The two stock boundary tests also pass. No screenshots were captured; final passing-suite and missing-order 404 evidence are ready for later genuine capture.

## Historical result: order total recalculation fix

Validated on October 5, 2026. Bugs 1 and 2 are fixed; only the missing-order bug remains open. No test source, fixture, or assertion changed in this step.

Root cause: clearing items left the stored total intact. `Order.clearItems()` now resets that total to 0.00; the existing item-addition calculation rebuilds it from the replacement items without duplicated calculation logic. Stored item prices remain unchanged by product edits, and order replacement still uses current prices.

`mvn -Dtest=OrderRegressionTest#recalculatesOrderTotalWhenItemQuantityChanges test` failed before the fix (PUT and GET totals 70.00 instead of 50.00), then passed afterward: 1 test, 0 failures/errors/skips.

Actual results from both `mvn test` and `mvn package`:

| Suite | Executed | Passed | Assertion failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| `ApiIntegrationTest` | 17 | 16 | 0 | 1 | 0 |
| `OrderServiceTest` | 4 | 3 | 1 | 0 | 0 |
| `OrderRegressionTest` | 5 | 4 | 1 | 0 | 0 |
| **Total** | **26** | **23** | **2** | **1** | **0** |

Both commands exited 1 with BUILD FAILURE. The only remaining cause is bug 3:

- `OrderServiceTest.reportsMissingOrder`: expected ResourceNotFoundException, actual NoSuchElementException.
- `OrderRegressionTest.returnsNotFoundWhenOrderDoesNotExist`: expected HTTP 404, actual HTTP 500.
- `ApiIntegrationTest.orderCrudPreservesPricesAndProtectsReferences`: the total assertions now pass, so it reaches GET after deleting the order; the same missing-order exception escapes MockMvc as a ServletException. This is an exposed later check of the existing bug, not a new unrelated defect.

The stock tests, total regression, price snapshot unit test, and transactional rollback test pass. HTTP verification used `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082`, without a skipped-test package. It confirmed 20.00 to 50.00, repeated PUT remaining 50.00, a replacement with multiple items totaling 15.00, and preserved price snapshot/replacement behavior. The missing-order endpoint still returned 500. See [the total fix report](bugs/02-order-total-recalculation.md#fix-evidence) for captured responses.

No screenshots were captured. The corrected total and passing targeted test are pending genuine capture; final all-green suite evidence is still unavailable.

## Historical result: insufficient-stock fix

Validated on October 5, 2026. Only bug 1 has been corrected; bugs 2 and 3 remain intentionally unfixed.

Root cause: order creation never compared requested quantities with product stock. `OrderService.create` now checks cumulative quantities per product before saving and raises the existing `ConflictException`, mapped to HTTP 409. No stock reservation/decrement or order-update validation was added.

The targeted command `mvn -Dtest=OrderRegressionTest#rejectsOrderWhenQuantityExceedsAvailableStock test` first failed (201 instead of 409; one order instead of zero), then passed after the fix with its assertions unchanged. Two additional stock boundary tests verify that exact stock is accepted and repeated product lines cannot bypass the limit. Both pass.

| Suite | Executed | Passed | Failed | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| `ApiIntegrationTest` | 17 | 16 | 1 | 0 | 0 |
| `OrderServiceTest` | 4 | 3 | 1 | 0 | 0 |
| `OrderRegressionTest` | 5 | 3 | 2 | 0 | 0 |
| **Total** | **26** | **22** | **4** | **0** | **0** |

`mvn test` and `mvn package` both exited with code 1 because the same four failures remain:

- `ApiIntegrationTest.orderCrudPreservesPricesAndProtectsReferences`: 129.95 instead of 89.97 (bug 2).
- `OrderRegressionTest.recalculatesOrderTotalWhenItemQuantityChanges`: PUT and GET return 70.00 instead of 50.00 (bug 2).
- `OrderServiceTest.reportsMissingOrder`: NoSuchElementException instead of ResourceNotFoundException (bug 3).
- `OrderRegressionTest.returnsNotFoundWhenOrderDoesNotExist`: HTTP 500 instead of 404 (bug 3).

No unrelated failures were introduced. In `ApiIntegrationTest`, the two order fixtures now set stock 100; in `OrderServiceTest`, the calculation fixture sets keyboard stock 3 and cable stock 2. Previously these valid-order scenarios implicitly used stock zero. Only their setup changed: all existing assertions and test methods are retained, and the other defects still fail at their original assertions.

Added tests in `src/test/java/com/example/bugfixlab/regression/OrderRegressionTest.java`:

- `acceptsOrderWhenQuantityEqualsAvailableStock`: stock 2 / quantity 2 returns 201 and persists one order.
- `rejectsOrderWhenRepeatedProductLinesExceedAvailableStock`: stock 2 / quantities 1 + 2 returns 409 and persists no order.

For live verification only, `mvn -DskipTests package` produced a JAR. Real HTTP on port 8081 confirmed 409 and an empty order list after rejection; exact-stock creation succeeded. The total and missing-order bugs were reproduced again. See [the stock fix report](bugs/01-insufficient-stock.md#fix-evidence). No screenshots were captured; stock rejection and the passing targeted test remain pending capture.

## Historical regression introduction

The following record applies to commit `09537ba423bfebced365ce38f853276fefb9fcf3`, before the stock fix. Its 24-test counts and all-three-failing statements are historical.

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
