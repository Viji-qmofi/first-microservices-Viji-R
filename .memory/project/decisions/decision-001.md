# Decision 001 - Feign Failure Handling Convention

**Date:** 2026-08-13
**Review by:** 2026-11-11
**Status:** Active

**Decision:** Feign call failures in OrderServiceImpl are converted to a ResponseStatusException with an appropriate HTTP status (503 for unreachable/general failures, 404 for confirmed not-found), rather than left to propagate as unhandled exceptions or returned as a raw, manually-constructed error body.

**Rationale:** viewAllProducts() and placeOrder() originally handled the same failure category two different ways -- viewAllProducts() threw ResponseStatusException, while placeOrder() manually built a ResponseEntity<String> with matching status codes but a different mechanism. This inconsistency was flagged by spring-boot-reviewer and confirmed during a Task-Resumption verification check, which caught the mismatch by reading the actual code rather than trusting the existing (inaccurate) record of this decision. placeOrder() was updated to match viewAllProducts()'s mechanism, so both methods now handle Feign failures identically.

**Alternatives rejected:** Leaving the two methods on different mechanisms with matching status codes was rejected once identified, since it meant the "consistent" convention wasn't actually consistent -- only the client-visible outcome was, not the implementation a future change would need to follow.
