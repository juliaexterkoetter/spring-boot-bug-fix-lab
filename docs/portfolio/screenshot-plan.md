# Portfolio screenshot plan

Store genuine captures in `docs/portfolio/screenshots/`. **No screenshots have been captured yet.** The current execution environment has no graphical display or terminal screen-capture tool. Existing text reports document real results, but are not screenshots.

Capture the terminal or API client displaying an actual run. Do not generate images from logs, reconstruct a terminal in HTML, invent responses, or create placeholder PNGs. Keep commands, response status, and relevant values readable. Record the capture date and source commit in the [evidence register](portfolio-evidence.md#11-portfolio-screenshots). Only claim service capabilities demonstrated by the captured result.

## Planned captures

All entries below are **pending**.

| Suggested filename | What must be visible | Why it is useful | Upwork service supported | When it should be captured |
| --- | --- | --- | --- | --- |
| `01-baseline-tests-passing.png` | Real terminal run of `mvn test` on baseline commit `e04266c`, with 21 tests, zero failures/errors/skips, and BUILD SUCCESS; show the commit identity. | Establishes the clean starting point before intentional defects. | Java/Spring Boot REST API development and automated testing. | During a genuine rerun of the baseline in an isolated checkout of the same repository. Label it as a later rerun; do not present current failing tests as the baseline. |
| `02-insufficient-stock-bug.png` | Product stock 2, order POST quantity 5, HTTP 201, and the persisted order response. | Demonstrates the exact business-rule violation with input and outcome. | Spring Boot business-rule debugging. | From a genuine rerun of pre-fix commit `071ae40`, following its documented HTTP reproduction; the current version rejects this request. |
| `03-order-total-bug.png` | Initial quantity 2 and total 20.00; PUT quantity 5 at price 10.00; subtotal 50.00, incorrect total 70.00, and follow-up GET. | Shows the discrepancy and proves it survives persistence. | Java calculation and persistence bug diagnosis. | From a genuine rerun of pre-fix commit `fa7589a`, with sufficient stock and unchanged product price. |
| `04-missing-order-500.png` | GET of a confirmed nonexistent order, request URL, HTTP 500, and error body. | Shows incorrect HTTP error handling from a real server. | REST API troubleshooting and exception handling. | Before fixing bug 3; use the real HTTP endpoint, not only a unit exception trace. |
| `05-regression-tests-failing.png` | Actual full `mvn test` run, all three regression test names and expected/actual mismatches, plus summary: 24 executed, 5 failures, 0 errors/skips, BUILD FAILURE. | Links the documented bugs to automated tests and exposes the two pre-existing failures honestly. | JUnit 5 integration testing and regression test development. | Before fixes, using regression commit `09537ba` or a documentation-only descendant. Use readable additional captures if one screen cannot fit the details; retain this filename for the summary. |
| `06-tests-passing-after-fixes.png` | Full `mvn test` command, actual final counts, zero failures/errors, no hidden skipped tests, BUILD SUCCESS, and fix commit. | Demonstrates that the full suite passes after correction. | Spring Boot bug fixing with regression verification. | Only after all three fixes are implemented and the unchanged regression assertions pass; record actual counts rather than assuming them. |
| `07-jacoco-coverage.png` | Genuine generated JaCoCo HTML report with project/package names, measured line and branch coverage, and totals. | Adds measurable test coverage evidence without confusing test counts with coverage. | Java test coverage analysis and test-suite improvement. | Only after JaCoCo is configured and a real report is generated in a later task. Do not invent percentages. |
| `08-swagger-overview.png` | Actual Swagger UI with project title, base URL, and customer/product/order endpoint groups. | Shows usable API discovery and request documentation. | Spring Boot API documentation with OpenAPI/Swagger. | Only after Swagger/OpenAPI support is implemented and running. |
| `09-stock-validation-error.png` | Stock 2, POST quantity 5, the corrected HTTP 409 rejection, and evidence that no order was persisted. | Provides the successful after-fix counterpart to screenshot 02. | Business validation fixes and transactional integrity. | Only after the stock bug is fixed and its regression test passes. |
| `10-order-not-found-404.png` | GET of a confirmed nonexistent order, HTTP 404, and the actual structured error body. | Provides the corrected counterpart to screenshot 04. | REST exception handling and HTTP status corrections. | Only after the missing-order bug is fixed and its HTTP regression test passes. |
| `11-docker-running.png` | Real container status, image name, port mapping, successful Spring Boot startup logs, and a successful HTTP request to the container. | Demonstrates a working containerized API rather than only a Dockerfile. | Docker packaging and containerized Spring Boot execution. | Only after Docker support is added and the container is genuinely running in a later task. |
| `12-order-total-corrected.png` | Actual targeted regression passing; POST with two units at 10.00 totaling 20.00, PUT with five units, GET total 50.00, and repeated PUT still 50.00. | Shows the corrected calculation and absence of accumulation. | Spring Boot calculation fixes and regression verification. | The fix is now verified; capture later from the corrected version. No screenshot was captured in the fix task. |

## Capture readiness

- **Existing evidence, capture still pending:** 01–05. The baseline needs a rerun at its historical commit; the three broken HTTP scenarios and failing regression suite are available at the current development stage.
- **Stock and total fixes verified, capture pending:** 09 and 12; no screenshots were taken during either fix task.
- **Future work required:** 06 and 10 need fixes; 07 needs JaCoCo; 08 needs Swagger/OpenAPI; 11 needs Docker.
- **Captured in this documentation step:** none. No PNG files were created.

## Evidence sources

- [Validation history](../VALIDATION.md): clean baseline, successful build/startup, 24 baseline HTTP checks, and the earlier bug-introduction milestone with 19 passing/2 failing tests and 15 unrelated HTTP checks.
- [Insufficient stock report](../bugs/01-insufficient-stock.md).
- [Order total report](../bugs/02-order-total-recalculation.md).
- [Missing order report](../bugs/03-missing-order-error-handling.md).
- [Regression test report](../regression-tests.md): the regression-introduction suite had 24 tests, 19 passes, and 5 failures; after the stock fix it had 26 tests, 22 passes, and 4 failures; after the total fix it has 26 tests, 23 passes, 2 assertion failures, and 1 error, all remaining unsuccessful results caused by bug 3. Do not caption the latest suite as having only two failures.

After each genuine capture, add the image link and provenance to the evidence register and update that entry's status. Keep unimplemented capabilities marked pending.
