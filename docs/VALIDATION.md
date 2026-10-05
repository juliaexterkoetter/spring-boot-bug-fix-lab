# Validation history

Current validation: **29 passing tests**; see the [final audit](portfolio/final-audit.md). The following results are historical.

## Historical controlled-bug milestone

Validated on October 5, 2026. All original test files remain unchanged.

- `mvn -B test`: BUILD FAILURE; 21 tests, 19 passed, 2 failures, 0 errors, 0 skipped.
- `ApiIntegrationTest.orderCrudPreservesPricesAndProtectsReferences`: expected total 89.97, actual 129.95 (bug 2).
- `OrderServiceTest.reportsMissingOrder`: expected ResourceNotFoundException, actual NoSuchElementException (bug 3).
- Stock availability has no coverage in the original test suite (bug 1).
- `mvn -B -DskipTests package`: BUILD SUCCESS, used solely to obtain the executable demonstration JAR; tests were explicitly skipped for this command. Normal packaging remains blocked by the failing tests.
- The packaged application started on port 8080. All three bugs were reproduced over HTTP; exact requests and formatted captured responses are recorded in [docs/bugs](bugs/).
- Fifteen additional HTTP checks passed: resource lists, missing customer/product lookups, missing-order update/delete, negative stock and price validation, duplicate email rejection, backward-compatible omitted stock, and deletion protection/cleanup.
- The first sandboxed test attempt stalled during Mockito JVM attachment and was stopped. The reported test results come from the subsequent run with the necessary execution permissions.

## Historical clean baseline

The results below apply to commit `e04266c917465b881984f27783fcc8f43bc3a72d`, not to the intentionally faulty version above.

Validated on October 5, 2026 in the managed project workspace.

## Environment

- Existing repository: `/workspace/spring-boot-bug-fix-lab`
- Java runtime was already available; a complete Java 21 JDK and Maven were missing.
- Installed Oracle JDK 21.0.12.1 and Apache Maven 3.9.11 under `/workspace/.tools`, outside the repository.
- Git 2.52.0 was already available.
- Maven dependencies were resolved through the environment's configured proxy with TLS verification enabled.

## Automated verification

| Command | Result |
| --- | --- |
| `mvn -B test` | BUILD SUCCESS; 21 tests, 0 failures, 0 errors, 0 skipped |
| `mvn -B package` | BUILD SUCCESS; the same 21 tests passed; executable JAR generated |
| `git diff --check` | Passed |

`-B` selects Maven batch mode without changing the test or package lifecycle.

The suite contains 4 unit tests and 17 integration test invocations, including parameterized cases. Integration tests verify persisted state across separate HTTP requests, including rollback after a partially processed order update.

## Live application verification

Started the packaged artifact with:

```bash
java -jar target/spring-boot-bug-fix-lab-0.0.1-SNAPSHOT.jar
```

Tomcat started on port 8080 and Spring reported successful application startup. An HTTP client then performed 24 successful checks against the running server:

- Create, list, retrieve, replace, and delete customers, products, and orders.
- Check that an order with two units at 19.99 totals 39.98.
- Change the product price to 29.99 and verify that the existing order still totals 39.98.
- Replace the order with three units and verify the new total is 89.97.
- Verify duplicate email and deletion of referenced resources return 409.
- Verify an invalid price returns 400 and missing resources return 404.
- Delete the test order, product, and customer and verify they are no longer found.

The managed command sandbox initially blocked network sockets. Running the same packaged application with the approved execution permissions resolved that environment restriction; no application change was needed.

## Notes

H2 data is temporary and disappears when the application stops. Mockito emits a dynamic-agent warning on Java 21; it does not affect this build or the tests. No authentication, Docker, frontend, or intentional defects were added.
