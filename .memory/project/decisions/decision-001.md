# Decision 001 - Feign Failure Handling Convention

**Date:** 2026-08-13
**Review by:** 2026-11-11
**Status:** Active

**Decision:** Feign call failures in OrderServiceImpl are converted to a ResponseStatusException with an appropriate HTTP status (503 for unreachable/general failures, 404 for confirmed not-found), rather than left to propagate as unhandled exceptions or returned as a raw, manually-constructed error body.

**Rationale:** viewAllProducts() and placeOrder() originally handled the same failure category two different ways -- viewAllProducts() threw ResponseStatusException, while placeOrder() manually built a ResponseEntity<String> with matching status codes but a different mechanism. This inconsistency was flagged by spring-boot-reviewer and confirmed during a Task-Resumption verification check, which caught the mismatch by reading the actual code rather than trusting the existing (inaccurate) record of this decision. placeOrder() was updated to match viewAllProducts()'s mechanism, so both methods now handle Feign failures identically.

**Alternatives rejected:** Leaving the two methods on different mechanisms with matching status codes was rejected once identified, since it meant the "consistent" convention wasn't actually consistent -- only the client-visible outcome was, not the implementation a future change would need to follow.

**Follow-up (2026-08-14):** A code-vs-record check found that the original `placeOrder()` update left an orphaned dead branch behind: a post-call `if(product!=null)/else throw NOT_FOUND` check that became unreachable once `FeignException.NotFound` was caught and rethrown above it -- a violation of the "No orphaned code after exception-handling changes" standard in coding-standards.md. Fixed by removing the dead branch, and added `OrderServiceImplTest` (success, not-found, and service-unavailable cases for `placeOrder()`) since the method previously had no dedicated unit test. Note: these changes were written and could not be compiled/run in this session -- the environment is offline with no cached Maven dependencies, so `./mvnw test` fails on parent-POM resolution before it can even reach the new test. Verify with a build before merge.
