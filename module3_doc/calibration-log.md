# Calibration Log

## Entry 1 -- 2026-09-21 -- Conflicting Outputs from Parallel Reviewers

**Failure mode:** Conflicting outputs from parallel reviewers (missing conflict-resolution rule).

**Development task:** Two temporary reviewer subagents (`reviewer_strict`, `reviewer_lenient`, `.claude/agents/`) reviewing a single real, already-completed change -- commit `9ebb640` (converting `OrderServiceImpl`/`OrderController` from field `@Autowired` to constructor injection). Not a locked holdout task.

**Check that caught the failure:** Three independent deterministic checks, not one -- `required_roles`, `role_order`, and `reviewer_conflict`. All three failed because the transcript's `expected_path` correctly described the *target* orchestration (including a conflict-resolution step that did not yet exist), rather than only describing what the faulty run actually did.

**Before-fix result:** `.eval-artifacts/runs/CAL-01-before.json` / `.log`. Both reviewers ran and genuinely disagreed on two shared sections: `wiring_test_coverage` (strict: reject, `@InjectMocks` doesn't prove constructor wiring; lenient: approve, existing tests still pass) and `scope_discipline` (strict: reject for lack of evidence, since it had no `git diff` tool at all; lenient: approve). `escalated_to_human: false` -- no resolution mechanism existed; the conflict simply went unaddressed. Deterministic result: 8/13 passed, 5 failed (`required_roles`, `role_order`, `reviewer_conflict` -- the induced fault; `latency`, `cost` -- a separate, unrelated measurement gap, not part of the induced fault).

**Root-cause hypothesis:** The Orchestrator had no policy at all for what happens after two reviewers return contradictory verdicts on the same item. Neither reviewer was malformed -- each was individually well-reasoned and internally consistent. The gap was structural: nothing defined the next step when a genuine contradiction exists.

**Fix layer:** Routing (what happens after a step returns), not Prompt. Deliberately not "give one reviewer more detailed instructions" -- both reviewers' own instructions and output format were already correct; the missing piece was the Orchestrator's own control flow.

**Change applied:** Added a "Reviewer Conflict Resolution" section to `CLAUDE.md`'s Orchestrator Instructions -- a same-section contradiction between reviewers must not be silently resolved (no picking a verdict, no averaging, no treating an informal chat discussion as sufficient); it requires an explicit `orchestrator_conflict_resolution` transcript step, `escalated_to_human: true`, and an actual human decision before the run proceeds.

**After-fix result:** `.eval-artifacts/runs/CAL-01-after.json` / `.log`. Both reviewers re-run with identical instructions, produced the same two genuine contradictions, and this time the Orchestrator correctly stopped, recorded the escalation step, and waited for a real human decision rather than proceeding on its own judgment. Human decisions recorded: `wiring_test_coverage` -- strict's technical point accepted as correct; follow-up work assigned (two constructor-based tests) rather than blocking the already-landed commit retroactively. `scope_discipline` -- lenient's verdict approved; the disagreement traced to `reviewer_strict` lacking `git` tooling entirely, not a real scope concern, and recorded as a separate finding rather than folded into this fix. Deterministic result: 13/13 passed. Rubric result: 4/4 dimensions passed (correctness 3/4, task_adherence 4/4, groundedness 3/4, clarity 3/4).

**Evidence paths:** `.eval-artifacts/runs/CAL-01-before.json`, `.eval-artifacts/runs/CAL-01-before.log`, `.eval-artifacts/runs/CAL-01-after.json`, `.eval-artifacts/runs/CAL-01-after.log`.

**Remaining concerns / limitations:**
- **Measurement discipline was imperfect even in the "fixed" run.** `cost_usd` for the after-fix run ($1.39) is an explicitly documented session-cumulative upper bound, not a clean isolated figure -- an attempted mid-correction re-check of `/status` ($2.19) turned out to be a *worse* estimate than the original reading, since cost only accumulates upward across a session and the later reading had absorbed additional unrelated work. The earlier, closer-in-time reading was the correct one to use, not the fresher one -- a genuine, worth-remembering lesson about isolated measurement in a long-running interactive session, not a one-off mistake.
- **`reviewer_strict` and `reviewer_lenient` have no `git diff`/`git show` access.** Both reviewers reviewed current file state and the run artifact rather than the actual diff, which directly caused the (separately resolved) `scope_discipline` disagreement. Not fixed in this cycle -- recorded as a candidate for a future cycle.
- **Neither reviewer's claims were independently re-verified before this record was written** -- flagged by the rubric's `groundedness` score (3/4 both runs): `reviewer_strict` cited a test file it never read; `reviewer_lenient`'s "26 tests passing" rested on a secondhand record. The escalation mechanism worked correctly regardless, but this is the same self-report-as-evidence pattern seen elsewhere in this course, now surfacing inside the review layer itself.
- **The transcript's own accuracy required a correction mid-cycle** -- an early draft of `CAL-01-after.json` dropped the `scope_discipline` ruling entirely, recording "no ruling given" when one had in fact been given. Caught and corrected before the check was re-run, not before it was first attempted -- worth remembering that even a system built specifically to produce trustworthy evidence can itself misrecord evidence, and needs the same scrutiny as everything else it evaluates.

### Regression Check

**Task:** DEV-04 -- unit tests for `fts_query` stopword filtering, deliberately unrelated to the reviewer-conflict fix. Run after the Reviewer Conflict Resolution policy was added, to check that the fix did not disturb the ordinary path.

**Result:** 13/13 deterministic checks passed; 4/4 rubric dimensions passed. No regression found.

**`role_order` caveat:** `expected_path` for this run was written post-hoc to match the actual retry sequence. Its pass therefore reflects transcript self-consistency more than independent verification of the plan. This is a pre-existing limitation of the check, not a new finding from this regression run.

**Conclusion:** The Reviewer Conflict Resolution policy does not interfere with the ordinary single-reviewer path. It was never triggered in this run.
