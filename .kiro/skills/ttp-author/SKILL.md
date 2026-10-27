---
name: ttp-author
description: Author the TTP (test plan) for an Arang feature — the test cases that prove each REQ/AC. Produces TC-### cases tracing to REQs/ACs, covering happy/empty/error/boundary paths. Use after the SDD is approved, before or alongside the build.
---

# TTP Author (Test Plan)

Turn approved REQs/ACs into a concrete, traceable test plan — the final link in
`UF→AC→REQ→T→TC`.

## When to use
After the SDD is approved (or alongside building a slice). Lighter/optional for the prototype,
but recommended for anything safety- or data-sensitive.

## Procedure
1. **Read the REQs (TRD) and the design's "Checks" section.**
2. **Write `TC-###` test cases.** Each: a clear precondition, action, and observable expected
   result (pass/fail). Prefer automatable (unit/integration) over manual; mark manual ones.
3. **Trace every TC to ≥1 REQ/AC; ensure every REQ/AC has ≥1 TC.** Coverage note; flag gaps.
4. **Cover the unhappy paths explicitly:** empty input, bad/malformed input, boundary values,
   auth/permission failures, idempotency/duplicates — not just the happy path.
5. **Check for verification gaps** (shapes adapted from BMAD `verification-gap`, MIT) — a TC is
   worth adding only if it catches a concrete regression:
   - *Regression gap* — changed behavior could regress where used with no test failing.
   - *Missing-adoption gap* — a site that should use the new behavior still does its own thing.
   - *Broken-verification gap* — a test appears to cover it but is skipped/flaky/too weak.
   - *Stale-verification gap* — a test the change made wrong or moot (may still pass).
6. **Group by the design's task slices** so each build task has its proving tests.
7. **Version-log.**

## Hand-off
Recommend the `spec-section-reviewer` skill (TTP mode) or the `spec-reviewer` agent. Tests may be
implemented during the build per the verification steering (write + run tests for new behavior).

## Guardrails
Every TC must be observable. No test without a REQ/AC parent. Humans decide risk tradeoffs
(what's worth testing); surface and recommend.
