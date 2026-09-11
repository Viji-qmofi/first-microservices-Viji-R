## Run 001 -- 2026-09-08 -- Orchestrated Workflow: Narrow FeignException Handling (3.1 Exercise 1)

Roles: Orchestrator (top-level Claude Code session), Planner (implemented, `.claude/agents/planner.md` v1), Implementer (implemented, `.claude/agents/implementer.md` v1). Reviewer and Tester designed but not implemented this round -- the Orchestrator's own verification and human approval stand in for them.

Task: real project work -- `spring-boot-reviewer` had repeatedly flagged (Runs 001-004) that `OrderServiceImpl`'s catch-all `catch (FeignException ex)` in both `placeOrder()` and `viewAllProducts()` mislabels any non-404 failure as "unreachable," with no logging to recover the actual cause. This workflow's job: narrow that handling and add logging.

### MCP infrastructure

`coursetools` MCP server (`mcp/coursetools_server.py`) registered at project scope (`.mcp.json`, committed) so the registration is shared via the repo rather than tied to one container's ephemeral state. Required adding Python3 + `pip3 install fastmcp "mcp<2"` to the Dockerfile (course-wide bug: unpinned `mcp` installs 2.x, which renamed `FastMCP` and breaks the course script's import -- flagged to the curriculum team separately). `roles.allowlist.json` adapted to this workflow's actual roles (`planner`, `implementer`, `reviewer`, `tester` -- no `orchestrator`/`project-manager`/`researcher`), matching `routing-and-tool-grant-map.md` exactly.

### Tool-boundary verification -- two independent confirmations

**Deliberate test (Step 4, direct MCP calls from the top-level session, no subagent involved):**
- `file_write` with `role="planner"` → denied: `"role 'planner' is not on the allow-list for file_write. Allowed roles: ['implementer']."`
- `file_write` with `role="implementer"` → succeeded (positive control, confirms the mechanism isn't blanket-denying).
- `test_runner` with `role="implementer"` → denied: only `"tester"` is allowed.
- Finding: the role check is a self-declared string match, not caller-identity verification -- the *server* has no way to know which actual process is calling it, it only sees whatever role string is sent. In normal operation this is safe because each subagent's own definition hardcodes its role and never varies it. The boundary that actually matters day-to-day is Claude Code's own client-side restriction (each subagent's `tools:` field), with the MCP server's role check as a secondary, defense-in-depth layer behind it -- not a primary guarantee on its own.
- Also confirmed: MCP role-gating has zero effect on native tools. Cleaning up the test file required `rm` via native Bash from the top-level session, completely outside the role-gated boundary -- meaning the entire scoped-tools story only holds for a subagent whose `tools:` field is restricted to *exclusively* MCP identifiers, exactly how `planner.md`/`implementer.md` were built.

**Organic finding (during the real run, unscripted):** the Implementer subagent reached a point where verifying its own work would have required shell/test access it doesn't have, and correctly stopped rather than attempting a workaround: "The implementer role had no shell access, so it couldn't run the build. I ran it myself" (Orchestrator). This is stronger evidence than the deliberate test alone, since nobody staged it -- it's the boundary holding under real task pressure, not a controlled probe.

### Planner run

Given the task brief and repo path. Produced a plan that correctly identified `feign.RetryableException` as the actual Feign type for "no HTTP response received" (true unreachable: timeout, connection refused, DNS failure), distinct from `FeignException` subtypes that carry a real status code -- the technically correct way to make the requested distinction, not just "add more logging." Proposed status mapping: `RetryableException` -> WARN log -> 503; `FeignException` with 5xx -> ERROR log -> 502; `FeignException` with other non-404 4xx -> ERROR log -> 500 (without echoing the downstream reason to the caller). Correctly scoped to exactly 2 source files + 2 memory files, explicitly declined to touch `NotFound` handling or introduce new exception infrastructure, and explicitly noted the memory-recording step was the Implementer's responsibility since it requires `file_write`, which Planner doesn't have. No code written, no `file_write` attempted -- stayed fully in its read-only role. One real design decision flagged to the human for sign-off before proceeding (the 503/502/500 split, since it's an externally-visible behavior change) -- approved.

Process note: invoking a second subagent (Implementer) from within the same continuous session initially failed -- the session appeared stuck continuing to respond *as* the Planner across multiple turns rather than returning control to the top-level orchestrator. Resolved with Esc (not exiting the session) to break out of that state; the underlying cause wasn't fully diagnosed, but Esc reliably fixed it. Worth watching for on future multi-role invocations in the same session.

### Implementer run

Given the Planner's plan verbatim. Produced code changes matching the plan precisely: logger added, both catch blocks split into `RetryableException` (WARN, 503) then `FeignException` (ERROR with status code logged, 502/500 split, no downstream detail leaked to the caller). Updated `OrderServiceImplTest.java`: fixed the pre-existing 500-case test's expected status (503 -> 502, correctly following from the new mapping), added a true-unreachable test using a real `RetryableException`, added a 400->500 regression test, and added full `viewAllProducts()` coverage (none existed before). Created `decision-004.md`, updated `MEMORY_INDEX.md`. Confirmed scope discipline: only the 2 source files + 2 memory files touched, nothing else in the repo modified.

### Orchestrator verification

Did not accept the Implementer's own "tests pass" claim at face value. Ran `./mvnw test -Dtest=OrderServiceImplTest` directly: 9/9 tests pass, `BUILD SUCCESS`, log output confirmed the WARN/ERROR split fires correctly for the different failure types. `decision-004.md` updated to record the verified result rather than left flagged as unrun (contrast with `decision-001.md`'s original unverified state from Exercise 2.3/2.4). `git diff --stat` at repo root showed ~305 files / ~21k lines due to pre-existing `.metadata/` and CRLF/LF noise unrelated to this session (consistent with the same pattern seen in Module 1 and the Module 2 lab); correctly scoped the actual diff to the 4 real files before committing rather than trusting the misleading top-level stat.

Commit: `8b8bc52` -- "fix: narrow FeignException handling to distinguish unreachable from real HTTP errors, add logging, record decision-004" (5 files: `OrderServiceImpl.java`, `OrderServiceImplTest.java`, `decision-004.md`, `MEMORY_INDEX.md`, `docs/feign-narrowing-plan.md`).

Pass/Fail: **Pass.** Real, previously-flagged project issue resolved correctly, tool boundaries verified two independent ways (one deliberate, one organic), and every claimed result (tests passing, scope discipline, plan quality) was checked against actual evidence rather than accepted from any agent's self-report -- consistent with the verification discipline built up across the whole course.

Observations: The deliberate MCP-layer boundary test and the organic implementer-self-limiting moment are genuinely complementary evidence, not redundant -- the first proves the mechanism exists and works when directly probed; the second proves it actually holds under real task pressure, unprompted. Worth keeping both in any write-up rather than treating one as sufficient on its own.