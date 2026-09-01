# Project Memory Index

Last updated: 2026-09-01

## Active entries

- decisions/decision-001.md — Feign call failures converted to ResponseStatusException with appropriate HTTP status (503/404). Review by: [90 days from decision-001's date].
- decisions/decision-002.md — API connection approach using service account; API key referenced via ANTHROPIC_API_KEY env var only, never written to memory/knowledge/code. Recorded 2026-08-26. Review by 2026-11-24.
- decisions/decision-003.md — placeOrder endpoint changed from @GetMapping to @PostMapping to fix REST method semantics violation; breaking change for external GET callers bypassing the gateway. Recorded 2026-09-01. Review by 2026-12-01.

## Archived entries

(none yet)

## Pruning schedule

- Workflow-scoped entries: archived when the branch merges to main
- Project-scoped entries: reviewed every 90 days