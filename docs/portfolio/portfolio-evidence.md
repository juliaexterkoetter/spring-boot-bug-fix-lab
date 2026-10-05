# Portfolio evidence

This is the continuing evidence register for Spring Boot Bug Fix Lab. It distinguishes verified historical results, the latest recorded state, and work that has not been completed. The stock defect is now fixed for order creation; the total and missing-order defects remain.

Last documentation review: October 5, 2026. The latest stock-fix step reran the targeted test, full suite, packaging, and real HTTP verification. Historical results retain their milestone context.

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

Source: the historical baseline section of [validation history](../VALIDATION.md). These are results from the clean baseline, not claims that the current intentionally faulty version passes.

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

Latest stock-fix run: **26 tests, 22 passes, 4 failures, 0 errors/skips**. The unchanged stock regression now passes, together with two new stock boundary tests. Only failures for bugs 2 and 3 remain. See the latest section of the [regression report](../regression-tests.md).

### Historical regression introduction

Regression-test commit: `09537ba423bfebced365ce38f853276fefb9fcf3`.

Three independent integration tests use a real HTTP server on a random port and a dedicated H2 database, with fresh fixtures for each test and no mocks.

| Test in `OrderRegressionTest` | Recorded failure |
| --- | --- |
| `rejectsOrderWhenQuantityExceedsAvailableStock` | Expected HTTP 409 and zero persisted orders; received HTTP 201 and one persisted order. |
| `recalculatesOrderTotalWhenItemQuantityChanges` | Expected 50.00 in both PUT and follow-up GET responses; both returned 70.00. |
| `returnsNotFoundWhenOrderDoesNotExist` | Expected HTTP 404; received HTTP 500. |

At regression introduction, `mvn test` reported **24 executed, 19 passed, 5 failed, 0 errors, 0 skipped; BUILD FAILURE**. The five failures are the three new regression tests plus the two original failures. No unrelated failures were introduced. Existing tests and production logic were unchanged during that step.

Sources: [regression report](../regression-tests.md) and [regression test source](../../src/test/java/com/example/bugfixlab/regression/OrderRegressionTest.java). The 19-pass/2-failure count belongs to the earlier 21-test milestone, not the current 24-test suite.

## 6. Bug fixes

**Bug 1 fixed; bugs 2 and 3 pending.** On October 5, 2026, the stock regression failed before correction and passed afterward without assertion changes. Root cause: order creation ignored stock. The minimal service change validates cumulative requested quantity per product before persistence and reuses the existing HTTP 409 exception mapping.

Real HTTP on the newly packaged application (port 8081) returned 409 for stock 2 / quantity 5; an immediate GET returned an empty order list. Repeated product lines totaling 3 were rejected against stock 2, while quantity 2 succeeded. Bugs 2 and 3 were reconfirmed through HTTP: total 70.00 instead of 50.00 and missing-order HTTP 500.

Both `mvn test` and `mvn package` failed only on the four remaining bug-2/bug-3 assertions. `mvn -DskipTests package` succeeded solely for the runnable verification artifact. This is not a green full build. Source revision: the dedicated stock-fix commit containing this entry, titled `fix: reject order creation when requested quantity exceeds stock`. See [root cause, correction, and before/after evidence](../bugs/01-insufficient-stock.md#fix-evidence).

For each future fix, add the diagnosis, minimal code change, commit, passing regression result, and real before/after HTTP evidence. Do not mark a bug fixed based only on a planned change.

## 7. Final test results

**Pending.** No final, all-green result exists for the expanded regression suite. The latest recorded result remains 22 passes and 4 failures out of 26 tests.

After the bugs are fixed, record the actual full-suite counts, Maven build result, source commit, and verification date. The historical 21-test baseline must not be presented as the post-fix result.

## 8. Code coverage

**Pending.** JaCoCo is not configured, and no coverage report or percentage has been measured. Passing-test counts are not coverage measurements.

After coverage tooling is added in a later task, link the generated report and record the measured scope and percentages. Screenshot `07-jacoco-coverage.png` is planned, not available.

## 9. API documentation

Available now: the [README endpoint table and curl examples](../../README.md#main-endpoints), request/response details in the [bug reports](../bugs/), and executable HTTP regression tests.

**Swagger/OpenAPI documentation is pending.** No Swagger UI or generated OpenAPI specification is currently configured. Screenshot `08-swagger-overview.png` cannot be captured yet.

## 10. Docker execution

**Pending.** No Docker configuration or verified container execution exists. No container screenshot or deployment claim is available. Docker was not added in this documentation step.

When implemented in a later task, record the image/build command, startup logs, mapped port, successful API request, and source commit before marking this section verified.

## 11. Portfolio screenshots

**Captured: 0. Pending: 11.** See the [screenshot plan](screenshot-plan.md) for filenames, required content, service relevance, and capture timing.

The current environment has no configured X11 or Wayland display and exposes no screen-capture tool for the terminal or running API. Although an ImageMagick capture binary is installed, there is no graphical session to capture. No artificial terminal image, rendered log image, generated screenshot, or placeholder PNG was created.

The [screenshots directory](screenshots/) contains only `.gitkeep` so Git retains the empty directory. That file is not evidence. Scenarios 01–05 have historical results but need genuine captures; scenario 09 now has verified stock-fix evidence ready for later capture. Scenarios 06–08 and 10–11 still depend on future fixes or tooling. No screenshot was captured during the stock-fix task.

When a real image is added, record its filename, capture date, source commit, command/request, and a factual caption here. If the baseline is rerun later, label the image as a rerun of the baseline commit, not an original historical capture.

## 12. Upwork portfolio material

Available supporting material: a functional baseline history, three documented intentional defects with real HTTP reproduction, and three independent regression tests proving those defects. No material has been published to Upwork during this task.

Services this evidence can support:

- Java/Spring Boot REST API development: entity modeling, CRUD, validation, and exception handling from the baseline.
- Spring Boot debugging and diagnosis: reproducible business-rule and HTTP error-handling defects.
- API integration and regression testing: real HTTP requests, persistence checks, isolated data, and explicit expected behavior.

Suggested factual project description based on current evidence:

> Built a Java 21 and Spring Boot order management API, verified a 21-test functional baseline and 24 HTTP checks, then introduced three controlled debugging scenarios. Documented their real HTTP reproduction and added independent regression tests that expose stock validation, total recalculation, and missing-resource error-handling defects. Fixed the stock-validation defect and verified rejection without persistence. The current 26-test suite has four failures from the two remaining controlled defects.

Only the stock fix is currently verified. Claims about completing all bug fixes, a final all-green suite, coverage percentages, Swagger, Docker, client outcomes, or production deployment are not yet supported. Add those only after the corresponding evidence exists.
