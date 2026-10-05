# Portfolio evidence

This is the continuing evidence register for Spring Boot Bug Fix Lab. It distinguishes verified historical results, the latest recorded state, and work that has not been completed. All three documented defects are fixed and verified.

Last documentation review: October 5, 2026. The latest OpenAPI step ran `mvn clean verify` with 29 passing tests and verified 11 real requests through Swagger UI. Only documentation configuration/annotations and one contract test were added; business logic is unchanged. Historical results retain their milestone context.

When adding evidence, record the date, source commit, command or HTTP request, expected and actual result, and a link to the report or genuine screenshot. Preserve historical results instead of replacing them with later results. A planned screenshot is not evidence of execution.

## 1. Project overview

A Java 21 and Spring Boot REST API for demonstrating reproducible debugging and regression testing in an order management application. This is a controlled portfolio exercise, not a client production incident.

- Domain entities: `Customer`, `Product`, `Order`, and `OrderItem`.
- CRUD endpoints: `/api/customers`, `/api/products`, and `/api/orders`.
- Stack: Maven, Spring Web, Spring Data JPA, Bean Validation, H2, JUnit 5, and Mockito.
- Structure: controllers, services, repositories, entities, DTOs, and global exception handling.
- Scope: a small API with no authentication or frontend. H2 data is temporary.

Sources: [project README](../../README.md), [build configuration](../../pom.xml), and [production sources](../../src/main/java/com/example/bugfixlab/).

## 2. Initial functional baseline

Verified baseline commit: `e04266c917465b881984f27783fcc8f43bc3a72d`.

| Evidence | Recorded result |
| --- | --- |
| Functional REST API | Customer, product, and order CRUD, including order items, validation, and exception handling. |
| Automated tests | 21 passed: 4 unit tests and 17 integration test invocations; no failures, errors, or skipped tests. |
| Maven build | `mvn -B test` and `mvn -B package` both reported BUILD SUCCESS; executable JAR generated. |
| Application startup | The packaged application started successfully with embedded Tomcat on port 8080. |
| Real HTTP validation | 24 checks passed, covering CRUD, totals, price snapshots, validation, missing resources, and deletion protection. |

Source: the historical baseline section of [validation history](../VALIDATION.md). These are results from the clean baseline, distinct from the final 26-test verification recorded below.

## 3. Controlled bugs introduced

Introduction commit: `071ae40da3ab173ad6b2e7b68dd2d281ae03d55c`.

| Scenario | Controlled defect | Report |
| --- | --- | --- |
| Insufficient stock | Product stock is available in the API, but order creation does not enforce it. | [Bug 1](../bugs/01-insufficient-stock.md) |
| Order total recalculation | Replacing items accumulates the previous stored total instead of recalculating from current items. | [Bug 2](../bugs/02-order-total-recalculation.md) |
| Missing order error handling | A GET for an unknown order throws an unmapped exception and returns HTTP 500 instead of 404. | [Bug 3](../bugs/03-missing-order-error-handling.md) |

At this stage, the original suite had **19 passing tests and 2 failing tests**, and **15 unrelated HTTP validations passed**. This is the pre-regression-test milestone recorded in [validation history](../VALIDATION.md), even though that document labels it "Current debugging version". The newer regression milestone below supersedes that test count.

## 4. Bug reproduction evidence

All three bugs were reproduced against the packaged application through real HTTP requests on October 5, 2026. Requests and captured response bodies are preserved in the linked reports.

| Request/scenario | Expected | Recorded actual result |
| --- | --- | --- |
| `POST /api/orders`, stock 2, quantity 5 | Reject the order without persisting it. | HTTP 201; five units were accepted, and a later GET confirmed persistence. |
| `PUT /api/orders/{id}`, change quantity 2 to 5 at unit price 10.00 | Total 50.00. | Item subtotal 50.00 but total 70.00; a later GET also returned 70.00. |
| `GET /api/orders/999999` | HTTP 404. | HTTP 500 with the default Internal Server Error response. |

Sources: [stock reproduction](../bugs/01-insufficient-stock.md), [total reproduction](../bugs/02-order-total-recalculation.md), and [missing-order reproduction](../bugs/03-missing-order-error-handling.md).

The 15 additional HTTP checks covered lists, missing customers/products, missing-order update/delete, invalid stock and prices, duplicate email, omitted stock compatibility, and deletion protection/cleanup. They passed in the bug-introduction session. They were not rerun as part of this documentation task.

To reproduce the intentionally broken application, its JAR was packaged with tests explicitly skipped. That packaging result is not evidence of a passing test suite.

## 5. Regression test evidence

Current OpenAPI milestone: **29 tests, 29 passes, 0 failures, 0 errors, 0 skipped**, verified by `mvn clean verify`. The prior coverage milestone had 28 passing tests. The final missing-order fix milestone had **26 tests, 26 passes, 0 failures, 0 errors, 0 skipped**. All three original regression scenarios, stock boundary tests, and the previously blocked GET-after-delete check pass. Both `mvn test` and `mvn package` report BUILD SUCCESS. See the latest section of the [regression report](../regression-tests.md).

### Historical regression introduction

Regression-test commit: `09537ba423bfebced365ce38f853276fefb9fcf3`.

Three independent integration tests use a real HTTP server on a random port and a dedicated H2 database, with fresh fixtures for each test and no mocks.

| Test in `OrderRegressionTest` | Recorded failure |
| --- | --- |
| `rejectsOrderWhenQuantityExceedsAvailableStock` | Expected HTTP 409 and zero persisted orders; received HTTP 201 and one persisted order. |
| `recalculatesOrderTotalWhenItemQuantityChanges` | Expected 50.00 in both PUT and follow-up GET responses; both returned 70.00. |
| `returnsNotFoundWhenOrderDoesNotExist` | Expected HTTP 404; received HTTP 500. |

At regression introduction, `mvn test` reported **24 executed, 19 passed, 5 failed, 0 errors, 0 skipped; BUILD FAILURE**. The five failures are the three new regression tests plus the two original failures. No unrelated failures were introduced. Existing tests and production logic were unchanged during that step.

Sources: [regression report](../regression-tests.md) and [regression test source](../../src/test/java/com/example/bugfixlab/regression/OrderRegressionTest.java). The 19-pass/2-failure count belongs to the earlier 21-test milestone, not the subsequent 24-test or current 26-test suites.

## 6. Bug fixes

### Historical stock fix

**Bug 1 fixed at this milestone; bugs 2 and 3 were pending.** On October 5, 2026, the stock regression failed before correction and passed afterward without assertion changes. Root cause: order creation ignored stock. The minimal service change validates cumulative requested quantity per product before persistence and reuses the existing HTTP 409 exception mapping.

Real HTTP on the newly packaged application (port 8081) returned 409 for stock 2 / quantity 5; an immediate GET returned an empty order list. Repeated product lines totaling 3 were rejected against stock 2, while quantity 2 succeeded. Bugs 2 and 3 were reconfirmed through HTTP: total 70.00 instead of 50.00 and missing-order HTTP 500.

Both `mvn test` and `mvn package` failed only on the four remaining bug-2/bug-3 assertions. `mvn -DskipTests package` succeeded solely for the runnable verification artifact. This is not a green full build. Source revision: `fa7589ac473499f50e385e4055f1039c5736b3ef`, titled `fix: reject order creation when requested quantity exceeds stock`. See [root cause, correction, and before/after evidence](../bugs/01-insufficient-stock.md#fix-evidence).

### Historical total recalculation fix

**Bugs 1 and 2 fixed at this milestone; bug 3 was pending.** The targeted total test first failed with 70.00 instead of 50.00, then passed without any test changes. Root cause: `clearItems()` removed items but kept the old total. The minimal entity change resets the total when clearing items; the existing subtotal accumulation then builds the replacement total without duplicated logic. Price snapshots and current-price replacement behavior remain intact.

Real HTTP on port 8082 confirmed initial total 20.00, updated and retrieved total 50.00, repeated PUT still 50.00, and a multiple-item replacement totaling 15.00. A product price change did not affect the existing order; the next order replacement used the new price. Stock rejection remains 409 and missing-order GET remains 500.

`mvn test` and `mvn package` both reported 26 tests, 23 passes, 2 assertion failures, and 1 error, all remaining problems caused by bug 3. No skipped-test packaging was used. Application startup via `mvn spring-boot:run` was used only for HTTP verification and is not a final build claim. Source revision: `7dc887d2672562c93b1f84a3dcd5dd82ad5a2193`, titled `fix: reset order total when replacing items`. See [the detailed fix evidence](../bugs/02-order-total-recalculation.md#fix-evidence).

### Missing-order handling fix

**All three bugs fixed.** Root cause: the GET path threw an unmapped `NoSuchElementException`. `OrderService.findById` now uses `requireById`, reusing `ResourceNotFoundException` and the existing global 404 handler. The production change is one line, with no changes to tests, controllers, stock validation, or total calculation.

The unchanged targeted regression failed with 500 before the fix and passed with 404 afterward. Both full Maven commands passed all 26 tests. The generated executable JAR started on port 8083, and 13 actual HTTP requests verified 404 for missing/deleted orders, 200 for existing orders, working creation/update, stock 409 with no persisted order, and correct 20.00-to-50.00 totals that do not accumulate.

Source revision: the dedicated commit containing this final fix entry, titled `fix: return not found for missing orders`. See [root cause and before/after responses](../bugs/03-missing-order-error-handling.md#fix-evidence). No screenshots were captured.

For each future fix, add the diagnosis, minimal code change, commit, passing regression result, and real before/after HTTP evidence. Do not mark a bug fixed based only on a planned change.

## 7. Final test results

**Latest OpenAPI milestone:** 29 tests passed with 0 failures/errors/skips in `mvn clean verify`. The original 28 tests remain unchanged; the additional test verifies generated documentation. The packaged application loaded Swagger UI, and 11 real Try it out requests succeeded without JavaScript page errors. No screenshots were captured.


**Historical coverage milestone:** 28 tests passed with 0 failures/errors/skips in `mvn clean verify`; the executable JAR and JaCoCo report were regenerated. The original 26 tests also passed with instrumentation in both `mvn clean test` and `mvn verify` before adding the two scenarios. No production source or existing test assertion changed. See [testing evidence](../testing.md).

### Historical final bug-fix milestone

**Verified on October 5, 2026:** `mvn test` and `mvn package` both exited 0 with BUILD SUCCESS. Each executed 26 tests with 26 passes, 0 failures, 0 errors, and 0 skipped tests. No tests were skipped during packaging.

Breakdown: 17 API integration test invocations, 4 service unit tests, and 5 HTTP regression tests. The executable JAR started successfully and passed the final HTTP checks described above. These results belong to the final missing-order fix commit containing this record; the original 21-test baseline is retained separately.

Sources: [final regression results](../regression-tests.md) and [final HTTP evidence](../bugs/03-missing-order-error-handling.md#fix-evidence). At that milestone, coverage, Swagger, and Docker were pending; the coverage result added afterward is recorded below.

## 8. Code coverage

**Verified on October 5, 2026; refreshed after OpenAPI integration.** JaCoCo 0.8.13 attaches to the test JVM and generates HTML, XML, and CSV reports in the test phase. A fresh `mvn clean verify` executed 29 tests successfully and generated `target/site/jacoco/index.html`.

| Counter | Covered / total | Coverage |
| --- | --- | --- |
| Instructions | 1009 / 1014 | 99.51% |
| Branches | 16 / 16 | 100.00% |
| Lines | 179 / 181 | 98.90% |
| Methods | 104 / 105 | 99.05% |
| Classes | 24 / 24 | 100.00% |

Values come from the final XML counters, not estimates. The report includes all application code subject to standard JaCoCo bytecode filters; no custom exclusions or arbitrary threshold was introduced. The uncovered code is the application `main` method. Business packages have full measured line coverage, but these numbers do not prove all input combinations or business scenarios are tested.

The first 26-test report and the 28-test coverage report had identical percentages (98.82% lines). The 29-test OpenAPI report additionally covers the documentation configuration and has the counters above. Two integration tests were added for meaningful scenario gaps: rejecting duplicate-email updates without changing data, and preserving stock across a later product edit that omits stock. Neither exists merely to increase coverage. Source revision: the dedicated commit containing this coverage entry, titled `test: add JaCoCo coverage and business scenario checks`.

Sources: [testing strategy and full results](../testing.md), [Maven configuration](../../pom.xml), and the locally generated report at `target/site/jacoco/index.html` (not committed). Screenshot `07-jacoco-coverage.png` is now ready for later genuine capture; no screenshot was taken.

## 9. API documentation

Available now: the [README endpoint table and curl examples](../../README.md#main-endpoints), request/response details in the [bug reports](../bugs/), and executable HTTP regression tests.

**Swagger/OpenAPI verified.** `springdoc-openapi-starter-webmvc-ui` 2.8.13 documents 15 operations under Customers, Products, and Orders, with parameters, request/response DTOs, examples, and applicable 200/201/204/400/404/409 responses. Title: Spring Boot Bug Fix Lab API; version: 0.0.1.

Default URLs:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`.
- JSON specification: `http://localhost:8080/v3/api-docs`.
- YAML specification: `http://localhost:8080/v3/api-docs.yaml`.

Final verification used the packaged application on port 8085. Headless Chromium opened the actual UI and executed 11 requests via Try it out: create customer/product, list both, reject an order with stock 2 / quantity 5 (409), verify no order was stored, create two units successfully (201, total 20.00), retrieve and replace that order (200, new total 10.00), retrieve missing order 999999 (404), and submit empty items (400). All 15 operations were visible; no JavaScript page errors occurred. No screenshots were captured.

`ApiIntegrationTest.publishesOpenApiOperationsAndValidatedRequestSchemas` verifies the generated specification, including matching minimum-item constraints and top-level validation errors. Final `mvn clean verify`: 29 passes, zero failures/errors/skips, BUILD SUCCESS. Source revision: the dedicated commit containing this entry, titled `docs: add OpenAPI specification and Swagger UI`.

Screenshots 08 and 13–15 are planned for genuine future captures. See the [README walkthrough](../../README.md#interactive-api-documentation) and [testing notes](../testing.md#openapi-contract-verification).

## 10. Docker execution

**Pending.** No Docker configuration or verified container execution exists. No container screenshot or deployment claim is available. Docker was not added in this documentation step.

When implemented in a later task, record the image/build command, startup logs, mapped port, successful API request, and source commit before marking this section verified.

## 11. Portfolio screenshots

**Captured: 0. Pending: 15.** See the [screenshot plan](screenshot-plan.md) for filenames, required content, service relevance, and capture timing.

Earlier checks found no desktop display for terminal capture. Headless Chromium was subsequently available for Swagger interaction testing, but no screenshots were taken, as requested. No artificial terminal image, rendered log image, generated screenshot, or placeholder PNG was created.

The [screenshots directory](screenshots/) contains only `.gitkeep` so Git retains the empty directory. That file is not evidence. Scenarios 01–05 have historical results but need genuine captures; scenario 09 now has verified stock-fix evidence ready for later capture. Scenarios 06 and 10 now have verified final results ready for capture, along with the corrected-total scenario 12. Scenario 07 now has a real JaCoCo report ready for later capture. Swagger evidence 08 and 13–15 is now ready for later capture. Only 11 still depends on future Docker tooling. No screenshots were captured during any fix task.

When a real image is added, record its filename, capture date, source commit, command/request, and a factual caption here. If the baseline is rerun later, label the image as a rerun of the baseline commit, not an original historical capture.

## 12. Upwork portfolio material

Available supporting material: a functional baseline history, three documented intentional defects with real HTTP reproduction, and three independent regression tests proving those defects. No material has been published to Upwork during this task.

Services this evidence can support:

- Java/Spring Boot REST API development: entity modeling, CRUD, validation, and exception handling from the baseline.
- Spring Boot debugging and diagnosis: reproducible business-rule and HTTP error-handling defects.
- API integration and regression testing: real HTTP requests, persistence checks, isolated data, and explicit expected behavior.

Suggested factual project description based on current evidence:

> Built a Java 21 and Spring Boot order management API, verified a 21-test functional baseline and 24 HTTP checks, then introduced three controlled debugging scenarios. Documented their real HTTP reproduction and added independent regression tests that expose stock validation, total recalculation, and missing-resource error-handling defects. Fixed all three defects with minimal changes, preserved regression assertions, and verified correct stock rejection, totals, and HTTP 404 behavior. The 26-test bug-fix suite and Maven package build passed without skipped tests, and the packaged application passed real HTTP checks. The subsequent OpenAPI milestone provides interactive Swagger documentation and passes 29 tests with 98.90% measured line coverage, documented limits, and no custom coverage exclusions.

All three documented fixes and the final passing suite/build are verified. Measured coverage is now documented above. Swagger is now verified above. Claims about Docker, client outcomes, or production deployment are not yet supported. Add those only after the corresponding evidence exists.
