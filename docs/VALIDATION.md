# Initial baseline validation

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
