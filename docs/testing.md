# Testing and coverage

## Testing strategy

Test observable business behavior and persisted state rather than private implementation details. Use precise monetary assertions with `BigDecimal`, verify HTTP status contracts, and check that rejected operations do not leave unintended data behind. Existing regression assertions are retained; no production source changed during the JaCoCo task.

Coverage guides inspection but does not replace meaningful assertions. No artificial 100% target, coverage exclusions, disabled tests, or empty tests were added.

## Types of tests used

| Suite | Tests | Approach |
| --- | ---: | --- |
| `ApiIntegrationTest` | 19 | Full Spring context, MockMvc, and real H2 persistence; verifies CRUD, validation, uniqueness, transactions, and stock behavior. |
| `OrderServiceTest` | 4 | JUnit 5 and Mockito for focused service calculations and missing dependencies; also verifies item price snapshots. |
| `OrderRegressionTest` | 5 | Embedded Tomcat on a random port, TestRestTemplate, and a dedicated H2 database; verifies real HTTP error dispatch, totals, stock rejection, exact stock, and repeated product lines. |
| **Total** | **28** | **All passed; no failures, errors, or skipped tests.** |

Counts include parameterized invocations. Database cleanup isolates integration scenarios. No mocks were added to the new integration tests.

## Regression testing approach

The original three regressions first failed against controlled defects, then passed after minimal production corrections. Their expected-behavior assertions remain unchanged:

- Insufficient stock: HTTP 409 and zero persisted orders.
- Order replacement: PUT and subsequent GET both return total 50.00 for five units at 10.00.
- Missing order: a real HTTP request returns 404, not 500.

See [regression history](regression-tests.md) and [bug reports](bugs/) for the before/after executions. The 26-test result in the final bug-fix report is a historical milestone; this coverage task adds two tests for a current total of 28.

## JaCoCo configuration

[The Maven configuration](../pom.xml) uses `org.jacoco:jacoco-maven-plugin:0.8.13`, compatible with Java 21.

- `prepare-agent` runs at its default `initialize` phase and supplies the test JVM agent through Maven's `argLine`. Surefire's normal forked JVM runs all three test classes with instrumentation.
- `report` is bound to the `test` phase, after Surefire. A successful `mvn test` therefore produces the report; `mvn verify` also executes this phase and packages the application.
- Default HTML, XML, and CSV reports are generated. Execution data is written to `target/jacoco.exec`.
- No custom source/package exclusions or coverage thresholds are configured. Standard JaCoCo bytecode filters still apply, including filters for compiler-generated record code. Abstract repository interfaces have no executable implementation to measure; their database behavior is tested through Spring Data JPA.
- Reports cover application classes exercised by Maven tests. They do not measure dependency internals, manual HTTP runs, or separately launched application processes.

Use a clean run for published evidence so coverage data cannot come from a previous run. If tests fail, do not treat an older report as a fresh successful result; the test-phase report goal may not execute after a failed Surefire goal.

## Commands and report location

```bash
mvn clean test
mvn verify
```

For a reproducible final report and full build:

```bash
mvn clean verify
```

Open `target/site/jacoco/index.html` in a browser. In the managed workspace, its absolute path is `/workspace/spring-boot-bug-fix-lab/target/site/jacoco/index.html`.

Other generated outputs:

- `target/site/jacoco/jacoco.xml`: precise counters used for the summary below.
- `target/site/jacoco/jacoco.csv`: tabular counters.
- `target/surefire-reports/`: test results.
- `target/spring-boot-bug-fix-lab-0.0.1-SNAPSHOT.jar`: packaged application.

All are generated under ignored `target/`; they are not committed. No screenshots were captured.

## Actual verification results

Executed on October 5, 2026 with Java 21 and Maven 3.9.11:

| Stage | Command | Result |
| --- | --- | --- |
| JaCoCo added, original tests retained | `mvn clean test` | BUILD SUCCESS; 26 passed, 0 failures/errors/skips; HTML report generated. |
| Original suite verified | `mvn verify` | BUILD SUCCESS; 26 passed, 0 failures/errors/skips. |
| Two business-scenario tests added | `mvn clean verify` | BUILD SUCCESS; 28 passed, 0 failures/errors/skips; report and executable JAR regenerated. |

No skipped-test flags were used. The final run starts with `clean`, removing previous compiled classes, execution data, and reports.

## Coverage summary

Measured from the final clean run's XML report, with percentages rounded to two decimal places. JaCoCo's HTML display may show whole percentages instead.

| Counter | Covered | Total | Missed | Coverage |
| --- | ---: | ---: | ---: | ---: |
| Instructions | 949 | 954 | 5 | 99.48% |
| Branches | 16 | 16 | 0 | 100.00% |
| Lines | 167 | 169 | 2 | 98.82% |
| Methods | 101 | 102 | 1 | 99.02% |
| Classes | 23 | 23 | 0 | 100.00% |

Package review:

| Package | Covered lines | Covered branches | Interpretation |
| --- | --- | --- | --- |
| `service` | 67/67 | 16/16 | Business-service code and measured decisions executed. |
| `entity` | 54/54 | Not applicable | No JaCoCo branch counters in these classes. |
| `controller` | 24/24 | Not applicable | Endpoint delegation exercised. |
| `exception` | 13/13 | Not applicable | Application exception mapping exercised. |
| `dto` | 8/8 | Not applicable | Measured DTO code executed; this is not exhaustive input validation coverage. |
| Application bootstrap package | 1/3 | Not applicable | `BugFixLabApplication.main` is not invoked by the automated suite. |

The first 26-test report had the same counters. The two new tests improve scenario protection without increasing the percentage, illustrating why coverage alone cannot establish correctness.

## Important covered scenarios

- Customer/product/order CRUD, valid-order totals, and price snapshot behavior.
- Case-insensitive duplicate customer emails on creation and update.
- Invalid update rollback and foreign-key deletion protection.
- Stock 2 / quantity 5 rejection, exact-stock acceptance, and aggregation of repeated product lines.
- Missing references rejected without persisting an order.
- Correct total after replacing quantities, verified in both response and persisted state.
- HTTP 404 for missing orders, including GET after deletion.
- Malformed JSON, invalid paths, invalid prices/items, and rejection of fractional quantities.

Two useful additions after reviewing the initial report and test scenarios:

1. `ApiIntegrationTest.rejectsDuplicateEmailOnUpdateWithoutChangingCustomer`: attempting to use another customer's email with different casing returns 409; the original customer's name/email and the owner's email remain unchanged, and the customer count stays two.
2. `ApiIntegrationTest.preservesUpdatedStockWhenLaterProductUpdateOmitsIt`: stock is explicitly changed from five to two; a later name/price edit omitting stock preserves two in the response and database; ordering three then returns 409 and persists no order.

Both are in [ApiIntegrationTest.java](../src/test/java/com/example/bugfixlab/ApiIntegrationTest.java). Existing test methods and assertions were not removed or weakened.

## Known coverage gaps and limits

- The only measured uncovered code is the bootstrap `main` method: five instructions, two lines, and one method. The Spring context and HTTP server are exercised by integration tests, and packaged startup has separate historical HTTP evidence. No trivial main invocation test was added merely to raise the percentage.
- All 16 measured branches are covered, but combinations of input values and operation sequences are not exhaustive. Bean Validation and framework logic mostly live outside application bytecode; 100% branch coverage does not mean every boundary or invalid payload has been tested.
- H2 verification does not establish behavior on a different production database. Concurrent updates and deployment/load behavior are not covered by this small suite.
- Inventory reservation/depletion is not a feature of the current application and is not claimed as tested functionality.
- Class coverage means some executable code in a class was visited, not that every method or requirement was verified. The bootstrap class is counted as covered even though `main` is not.

The relevant business packages have no measured low-coverage area. Further tests should be justified by a concrete behavior or risk, not by filling the remaining bootstrap gap.
