# Final quality and presentation audit

Audit date: October 5, 2026. Source: Docker milestone `0b90b29` plus the dedicated audit commit containing this report. Scope: portfolio presentation and existing application behavior; no new business features, production refactors, test changes, or screenshots.

## Findings

| Area | Review result |
| --- | --- |
| Structure and naming | English names and clear controller/service/repository/entity/DTO/exception/config responsibilities. No generated reports or unnecessary binaries tracked. The screenshots directory contains only `.gitkeep`. |
| Business code | Transactions protect writes, DTOs separate HTTP from persistence, monetary values use BigDecimal, and item prices are snapshots. All three corrections remain intact. The small explicit services do not justify a generic CRUD abstraction. |
| Validation and errors | Required fields and numeric limits are enforced. Stock creation checks aggregate repeated product lines. Existing global handlers map missing resources to 404 and conflicts to 409; database exception details are not exposed. Unknown JSON properties and fractional quantities are rejected. |
| Tests | 4 unit, 20 integration invocations, and 5 real HTTP regressions. Descriptive scenarios verify persisted state, rollback, stock boundaries, totals, and missing resources. No empty or percentage-only tests found; assertions and sources preserved. |
| Maven and dependencies | Spring Boot 3.5.6 manages framework versions; Java 21, springdoc 2.8.13, JaCoCo 0.8.13. Declared dependencies have identifiable uses. JaCoCo runs during normal tests without exclusions or skipped tests. Compatibility is verified by the build; this is not a dependency vulnerability scan. |
| Docker | Multi-stage build, digest-pinned Maven/JRE bases, tests inside the build, non-root UID/GID 10001, read-only filesystem, temporary writable `/tmp`, dropped capabilities, and no-new-privileges. Minimal build context excludes Git and local artifacts. Optional build secrets are not copied into the runtime image. One issue: the published port previously listened on every host interface. |
| OpenAPI | Consistent title/version, three resource groups, 15 operations, request/response DTO schemas, examples, and relevant 200/201/204/400/404/409 responses. A contract test protects schema constraints and flattened Problem Details extensions. |
| README | Too much operational detail obscured the portfolio story; technologies and package structure omitted newer additions. Reorganized for quick scanning and moved detailed instructions to a runbook. |
| Portfolio history | Historical 21- and 26-test results could be mistaken for current counts; old screenshot notes still described already-fixed blockers. Milestone labels and links now distinguish history from the current 29-test suite. |
| GitHub presentation | No remote settings changed. Suggested description/topics and remaining presentation work are recorded below. |

## Changes made

- Rewrote [README](../../README.md) with an upfront result summary, debugging case study, features, stack, run commands, Docker, tests/coverage, API links, endpoints, evidence, and package structure.
- Preserved detailed setup, proxy instructions, curl workflow, and behavioral limits in [running the application](../running.md).
- Bound the Compose published port to `127.0.0.1`. The unauthenticated local demo no longer publishes on every host interface; application routes and behavior are unchanged.
- Clarified historical validation/regression counts and obsolete capture blockers; replaced ambiguous milestone references with commit IDs and updated documentation links.
- Preserved production sources, Maven configuration, all tests, Dockerfile, Swagger, and JaCoCo configuration.

## Final validation results

All checks below completed successfully on October 5, 2026. Generated Maven reports remain ignored under `target/`.

| Verification | Actual result |
| --- | --- |
| `mvn clean verify` | BUILD SUCCESS; 29 tests, 0 failures, 0 errors, 0 skipped; executable JAR and JaCoCo report generated. |
| `docker compose build --no-cache` | Success; Maven ran inside the builder and independently passed all 29 tests. Maven dependency cache was allowed; Docker build layers were not reused for application build steps. |
| `APP_PORT=8086 docker compose up -d --wait --force-recreate` | Success; Spring Boot started in 5.234 seconds; container reached healthy. Host 8080 was occupied, so 8086 was selected. |
| Direct HTTP | 13 requests passed against the container: customer/product creation, stock rejection without persistence, order create/read/update/delete, missing-order errors, and repeated-update totals. |
| Swagger UI | Headless Chromium loaded the real UI and all 15 operations. Eleven Try it out requests passed, including customer/product operations, order create/read/update, and 400/404/409 responses. No JavaScript page errors; no screenshots. |
| OpenAPI and health | `/v3/api-docs` served the six paths/15 operations; the exact internal `wget` health probe exited 0. Docker inspect reported healthy. |
| Runtime restrictions | UID/GID 10001, Java 21.0.12.1, read-only root filesystem, capabilities dropped, no-new-privileges, published port `127.0.0.1:8086`. |
| Documentation | Local Markdown file links resolve; `git diff --check` passes. No production or test source files changed. |

The managed workspace requires an external Compose build override supplying the existing optional Maven settings and Java trust-store secrets. The actual build command added `-f docker-compose.yml -f /tmp/lab-docker-build-secrets.yml`; the override and secrets are not committed. Docker commands explicitly selected the local Unix-socket daemon. Ordinary workstations use the documented Compose command without these environment-specific settings. No TLS verification or tests were disabled.

Image: `spring-boot-bug-fix-lab:local`, ID `sha256:57490789dcd58a4c003fc2a65f4293c005f45b660fce9dad2354ac5d53e30c27`, size 266,082,528 bytes (253.76 MiB). Digest pinning controls base-image selection, but this audit does not claim byte-for-byte reproducible JAR builds or a vulnerability-free image.

### Real HTTP outcomes

| Scenario | Observed result |
| --- | --- |
| Create customer and two products | 201 for each; returned IDs used in later requests. |
| Stock 2, quantity 5 | 409 with `Insufficient stock for product: 2`; subsequent order list was empty. |
| Missing order 999999 | 404 with `Order not found: 999999`. |
| Create two units at 10.00 | 201, total 20.00; GET returned 200 and 20.00. |
| Replace with five units | PUT 200, total 50.00; GET and repeated PUT both remained 50.00. |
| Delete order | 204; subsequent GET 404. |
| Swagger invalid empty items | 400 with validation Problem Details. |

The Swagger session additionally created a stock-2 product, rejected five units, accepted two (20.00), and replaced them with one (10.00). All requests targeted the container's mapped port, not another Java process.

### Final coverage

| Metric | Covered / total | Percentage |
| --- | ---: | ---: |
| Instructions | 1009 / 1014 | 99.51% |
| Branches | 16 / 16 | 100.00% |
| Lines | 179 / 181 | 98.90% |
| Methods | 104 / 105 | 99.05% |
| Classes | 24 / 24 | 100.00% |

Measured from the freshly generated `target/site/jacoco/jacoco.xml`; HTML: `target/site/jacoco/index.html`. These counters are unchanged. See [testing strategy and coverage limits](../testing.md).

## Remaining recommendations

- Suggested GitHub description: **Spring Boot debugging portfolio: three reproducible bugs, regression tests, minimal fixes, JaCoCo coverage, Swagger, and Docker.**
- Suggested topics: `java`, `spring-boot`, `rest-api`, `debugging`, `regression-testing`, `junit5`, `mockito`, `jacoco`, `openapi`, `swagger`, `docker`, `portfolio`.
- Capture the 18 planned genuine images with the actual source commit and date. Historical bugs/failures require the documented historical revisions. Use the latest revision for the current 29-test suite. No images were captured or fabricated during this audit.
- The owner should choose a license before advertising reuse rights; no license was selected on their behalf. Automated CI verification would be a useful later repository improvement.
- Periodically review dependency releases and refresh pinned image digests after verification. Run dependency and container vulnerability scans before any public deployment; no CVE-free claim is made here.
- Retain the explicit demo limits: temporary H2 data, no authentication, no inventory reservation/depletion, creation-only stock validation, and unpaginated lists. Concurrency/load and other database engines are not covered by this suite. The remaining uncovered bootstrap method does not justify an artificial coverage test.

## Portfolio readiness assessment

**Ready for genuine screenshot capture and a local portfolio demonstration.** All three documented bugs are fixed, the 29-test suite and Docker build pass, and container HTTP/Swagger/health checks succeed. Remaining recommendations do not block this capture phase. The controlled exercise must be presented as such, with historical failures clearly separated from the corrected application. See the [evidence register](portfolio-evidence.md) and [screenshot plan](screenshot-plan.md).
