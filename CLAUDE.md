# Agent Context Boundary Policy

## Purpose
This file defines how context boundaries are managed in this project.
At the start of each new phase of work, the agent must follow the
procedure below before taking any action.

## Context Boundary Procedure
At the start of each new phase, before doing any editing or analysis:

1. Restate the current task goal in one sentence.
2. List the rules currently in effect (verbatim, not paraphrased).
3. State explicitly which prior rules are no longer in effect, if any.
4. Identify the specific artifact being worked on in this phase.
5. Then proceed with the requested work.

## Why This Matters
Rules and requirements change during long sessions. This procedure ensures the agent is always operating from the current version of the rules, not a prior version buried in conversation history.

## Memory Configuration

At the start of every session, read .memory/project/MEMORY_INDEX.md
to orient yourself. Then read any active entries listed there that
are relevant to the current task.

Before making any significant decision or observing something worth
remembering across sessions, check the index for an existing entry
on the same topic. Update existing entries rather than creating
duplicates.

### Memory layers

- .memory/project/ — Read on startup via MEMORY_INDEX.md. You may
  write new entries here when a significant decision is made or
  project state changes.

- .memory/knowledge/ — Read-only. Consult before making any decision
  that touches coding standards or architectural constraints. Never
  attempt to write to this directory.

- .memory/reference/ — Read-only. Query by keyword for relevant
  excerpts when you need background context. Do not read the entire
  directory.

### Write policy

Before writing any content to a memory file, classify it:
- If it is Public or Internal: proceed with writing
- If it is Confidential: do not write it to memory. Note in your
  response that the information was not stored and explain where
  it should be retrieved from instead.
- If it is Secret (credential, token, API key, PII): do not write
  it anywhere. Use it for the immediate task only. If you find a
  secret already written in a memory file, flag it immediately and
  do not proceed until a human removes it.

Before writing a new memory entry, check MEMORY_INDEX.md for an
existing entry on the same topic. Update existing entries rather
than creating new ones. Never write anything classified as
Confidential or Secret to any memory layer.

### Stale memory policy

If a memory entry's review date has passed, flag it in your session
output and ask for human confirmation before acting on it.

### Scope verification

Read SCOPE.md at the root of .memory/ on startup. If it does not
match this project, halt and report the mismatch before doing
anything else.

## Orchestrator Instructions

When a task involves planning a code change and then implementing it (not a simple one-step edit), delegate rather than doing both yourself in one pass.

### When to invoke each subagent

- Invoke the `planner` subagent first, whenever a task requires deciding *how* to make a change, not just making it -- e.g. distinguishing between failure types, choosing where logic should live, or any change where more than one reasonable approach exists.
- Do not invoke `planner` for trivial, single-interpretation changes (a typo fix, a one-line config change) -- delegating those adds overhead without adding safety.
- Invoke the `implementer` subagent only after a plan from `planner` has been evaluated and judged adequate (see below). Never pass an unevaluated or inadequate plan straight through.

### What to hand each subagent

- `planner` receives: the task brief (what needs to change and why) and the repo path. Nothing else -- it should form its own understanding of the current code via `file_read`/`codebase_search`, not be handed a pre-digested summary.
- `implementer` receives: the planner's full plan and file list, verbatim. Do not paraphrase or summarize the plan before handing it off -- paraphrasing risks dropping a constraint the planner specified.

### Expected output format

- `planner` returns a numbered plan and an explicit file list, no code.
- `implementer` returns a summary of exactly what changed, in which files, and any judgment call it had to make that the plan didn't explicitly cover.

### Evaluating results

- After `planner` returns: check that the plan is concrete (names exact files/methods, not vague intentions), correctly scoped (touches only what the task requires), and stays within its role (no code, no attempt to write). If the plan proposes a behavior change visible outside the codebase (e.g. a change to API responses), flag it to the human for explicit approval before proceeding, even if the plan itself is sound.
- After `implementer` returns: never trust its own summary as verification. Independently confirm the change compiles and passes tests by running the actual build/test command yourself -- do not delegate this to the implementer, which does not have test-running access by design. Also independently check the diff (`git diff`) matches what was claimed, scoped to only the intended files.

### What to do if a result is incomplete or doesn't meet requirements

- If `planner`'s plan is vague, incorrectly scoped, or missing something the task requires: send it back with specific feedback naming the gap, and request a revision. Do not pass an inadequate plan to `implementer` "to save time."
- If `implementer`'s change fails to build or fails tests: send the actual failure output back to `implementer` and request a fix. Do not attempt to fix it yourself in place of `implementer`, and do not silently work around a failure.
- If either subagent attempts to use a tool outside its granted set, or returns output outside its defined responsibility (e.g. `planner` proposing code instead of a plan): stop, do not proceed to the next phase, and report this to the human rather than compensating for it automatically.

### Storage and Retrieval Access

- `planner` retrieves relevant prior lessons (via `mcp__retrieval__retrieve`, `project_id="proj-lessons"`, `classification_ceiling="internal"`) before proposing an approach. Check its plan cites what it found, or explicitly notes nothing relevant existed -- a plan that skips this step should be sent back.
- `implementer` may record one new lesson learned (via `mcp__storage__write_entry`) when the work surfaces something a future session couldn't reconstruct from the code alone. It has write-only access -- it cannot read, list, or edit existing entries. Not every run needs a new entry; a plan that ends with "recorded a lesson" for a routine, unsurprising change is over-recording, not a sign of thoroughness.
- The Orchestrator itself retrieves relevant standards or prior lessons while evaluating the Implementer's result, standing in for a dedicated Reviewer role (same pattern as the Orchestrator's own `./mvnw test` run standing in for Tester). This is a policy followed by the top-level session, not a technically enforced boundary the way a subagent's `tools:` restriction is -- worth remembering that distinction rather than treating it as an equivalent guarantee.

**Classification ceiling is self-declared, not server-enforced per role.** `classification_ceiling` is just a parameter any caller passes to `retrieve` -- nothing on the server ties a specific role's identity to a maximum ceiling it's allowed to request. `planner`'s instructions tell it to always pass `"internal"`, but that is a policy the agent is expected to follow, not a technical guarantee the way the storage server's own `write_classifications` check is (which genuinely rejects a `secret`-classified write server-side, regardless of what the caller claims). Do not describe retrieval's classification ceiling as enforced in the same sense as storage's write classification -- they are different strengths of guarantee.

### Human checkpoint

Before any implemented change is committed, present a run summary (plan, diff, test result) to the human for approval. This applies regardless of whether the run appeared to go smoothly -- approval is not conditional on anything going wrong.
