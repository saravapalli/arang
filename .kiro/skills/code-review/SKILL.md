---
name: code-review
description: Review a code change (diff, file, or function) for unhandled edge cases and test/verification gaps, before it's considered done. Use after writing or modifying code in a build slice. Adapted from BMAD-METHOD review-prompts (edge-case-hunter, verification-gap; MIT).
---

# Code Review (edge cases + verification gaps)

Two complementary passes over a code change. Adapted from BMAD-METHOD's `edge-case-hunter`
and `verification-gap` review prompts (MIT-licensed; patterns reused, wording rewritten).
Use during the build phase, after a change is written and before it's called done. Complements
the spec-level reviewers (which check docs, not code).

## Pass 1 — Edge-case enumeration
Mechanically walk **every** branch and boundary reachable from the changed lines; report ONLY
the ones with no explicit guard. Do not editorialize about code quality.
- Control flow: missing else/default, unguarded inputs, off-by-one loops, early returns, error paths.
- Domain boundaries: null/empty, overflow, type coercion, race/timeout, duplicate/idempotency.
- Implicit branches: if the change special-cases some members of a fixed set (enum, status
  codes, flags), the rest are implicit branches — check them.
- Call-site checks: for each added/changed call, verify argument count/order/types against the callee.
For each unhandled path, report: location · trigger condition · a minimal guard sketch · the
consequence. Discard handled paths silently. Empty result is valid.

## Pass 2 — Verification-gap (tests)
Find behavior the change altered that could break later with **no test failing**. File a gap only
when you can name a specific regression a test would catch. Gap shapes:
1. **Regression gap** — changed code could regress where used; no test fails.
2. **Missing-adoption gap** — a site that should use the new behavior still does its own thing.
3. **Broken-verification gap** — a test looks like it covers the change but is skipped/flaky/too weak.
4. **Stale-verification gap** — a test this change made wrong or moot (may still pass).
Evidence rules: read a test before claiming what it covers; search for the symbol before saying
no test exists; state what you checked; drop any finding you can't ground. For each gap: changed
surface · impacted consumer (file:line) · existing test evidence · missing assertion ·
demonstration of the regression · consequence · the test to add/change.

## Scope & guardrails
- Review only the provided change (diff/file/function); don't audit the whole codebase.
- Skip parts with no observable behavior change (formatting, renames, comments, type-only).
- This is for Arang's own application code during build slices (per the verification steering:
  write + run tests for new behavior). It does not replace the spec reviewers.
- Report; the human/author decides what to fix. Don't assign false-precision severity to edge
  cases — list them; let the fix decision follow.
