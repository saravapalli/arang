---
name: trd-author
description: Author the TRD (verifiable technical requirements) for an Arang feature as its own file, each REQ-### tracing to a PRD acceptance criterion. One folder per feature, stage-named files (docs/<feature>/trd.md). Use after a PRD is approved, before the SDD.
---

# TRD Author

Turn an approved PRD's acceptance criteria into verifiable system requirements.
The testability rigor (good/bad examples) and the self-validation loop below are adapted from
GitHub Spec Kit's success-criteria guidance (`templates/commands/specify.md`, MIT) — reused and
rewritten for Arang; see `docs/reference/sdd-frameworks-research.md`.

## When to use
After a PRD is approved, before the SDD. The TRD is its own file: `docs/<feature>/trd.md`
(one folder per feature, stage-named files — see steering).

## Procedure
1. **Read the approved PRD.** Work only from approved ACs — don't invent scope.
2. **Write `REQ-###` statements** — each a single, testable "the system MUST…" requirement.
   Group them (e.g. intake / processing / UI / security / operational).
3. **Trace every REQ to its AC(s)** with `⟵ AC-###`, and ensure every AC is covered by ≥1 REQ.
   Add a REQ↔AC coverage note; flag any AC with no REQ (gap) or REQ with no AC (scope creep).
4. **Right-size:** aim ~5–15 REQs; if more, the feature is probably too big — flag it.
5. **No design detail.** REQs say *what must be true*, not *how* (that's the SDD). Keep tables,
   schemas, and tech choices out of the TRD section.
6. **Version-log** the change.

## What makes a REQ verifiable (reject vague ones)
Each REQ must be something a test could pass or fail on. Two traps to avoid:
- **Unmeasurable verbs** — "handles", "supports well", "is fast", "is user-friendly". Replace
  with an observable condition and, where the AC implies one, a number.
- **Implementation leaking in** — naming a framework, table, or algorithm inside the REQ. The
  REQ states the required *behavior/outcome*; the mechanism is the SDD's job.

Good (verifiable, outcome-stated):
- "The system MUST reject an event whose start time is absent, returning a validation error that
  names the missing field." ⟵ AC-012
- "The system MUST persist every ingested event's origin (`user` | `ingestion_source` |
  `api_client`) so provenance is queryable." ⟵ AC-004
- "The system MUST return feed results for a valid query within a bounded, stated latency."

Bad (not verifiable as written → rewrite):
- "The system handles bad input gracefully." (which input? what response? → make it observable)
- "Ingestion is fast and reliable." (no measurable condition → state the threshold/outcome)
- "Store events in a MySQL `event` table with a JPA entity." (implementation — belongs in SDD)

## Self-validation loop (before hand-off)
After drafting, check the REQ set against this list and fix what fails, iterating at most ~3
times, then surface anything still open rather than looping:
- Every AC has ≥1 REQ; every REQ traces to ≥1 AC (no orphans, no scope creep).
- Every REQ is independently testable (passes the "verifiable" bar above).
- No design/implementation detail leaked into any REQ.
- REQ count is within ~5–15, or the oversize is flagged.
This is a cheap self-gate — it does not replace the `spec-section-reviewer` drill or the
`spec-reviewer` gate.

## Hand-off
Recommend a review (the `spec-section-reviewer` skill or the `spec-reviewer` agent) before the
SDD builds on the REQs. Do not write code.

## Guardrails
- Each REQ must be independently verifiable (a test could pass/fail on it). Reject vague verbs.
- Humans decide tradeoffs; surface, don't resolve.
