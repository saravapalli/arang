# Spec-Driven Workflow (always-on rules)

> This is the operating discipline for this project, from "How Spec-Driven AI Works"
> (see docs/reference/how-spec-driven-ai-works.md). These rules apply every session.
> Refine this file as the workflow is used — it is expected to evolve.

## The one rule above all
**Humans decide; AI executes.** Product intent, conflict resolution, and sign-off are the
user's. Translation between artifacts, mechanical edits, coverage audits, and consistency
checks are the assistant's. Never resolve a product/tradeoff decision silently — surface
it, recommend, and let the user decide. The artifact (not a chat message) is the record.

## Spec before code, in order
Lifecycle: **PRD → TRD → design/SDD → TTP → build → release.** Each artifact forces
precision on a different question before the next begins:
- PRD = the *what & why* (problem, goals, user flows, scope, acceptance criteria).
- TRD = *what the system must do* (verifiable `REQ-###` requirements).
- design/SDD = *how it's built* (design + dependency-ordered tasks).
- TTP = how we prove it (test plan, `TC-###`).
Do not write application code before the relevant spec is approved by the user.

**One file per stage (decision 2026-10-20, reversing the earlier "folded TRD" prototype shortcut).**
Each lifecycle stage is its own document with its own frontmatter and format — *not* folded into a
single `design.md`. This matches how Spec Kit / OpenSpec / BMAD all separate their artifacts, keeps
sign-off and traceability per-stage clean, and avoids one file conflating requirements, design, and
tests. Layout: **one folder per feature, stage-named files inside** —
`docs/<phase>-<feature>/{prd,trd,sdd,ttp}.md` (folder prefixed with its phase so the listing sorts in phase order: `p1-`, `p2-`, `p2.1-`, `p3-`, `p5-`) (e.g. `docs/p1-producer-intake/prd.md`, `…/trd.md`, `…/sdd.md`,
`…/ttp.md`). The TTP stays lighter/optional for the prototype but is still its own file when written.

## Traceability (audit at every stage)
Carry stable IDs so any item traces from intent to proof:
`UF-### (user flow) → AC-### (criterion) → REQ-### (TRD) → T# (task = 1 story) → TC-### (test)`.
Each task maps to exactly one story (the only strict 1:1). On every edit, re-check: does
each user flow have ≥1 acceptance criterion, and each criterion a home downstream? Flag
orphans and uncovered items.

## Right-sizing (caps — stop bloat AND fragmentation)
- One intention per flow: one coherent user intention, one observable outcome, a handful
  of criteria. Split if multiple outcomes/actors hide inside; demote to a note/AC if it's
  a mechanism, not a user-facing outcome.
- Guideline caps: TRD 5–15 requirements; design 5–12 tasks; a task = 1 concern / 1 repo /
  ≤3 files / 1 test run; an initiative ≤5 child features.
- One PRD maps 1:1 to one design/epic. If it outgrows the caps, promote to an initiative
  and split into child PRDs — don't hang multiple designs off one PRD.

## Honesty about the unverified
Mark the boundary between known and assumed. State what was checked vs. not. Record
assumptions explicitly (and let the user reject them). Never assert what hasn't been
verified as if it were fact.

## Consistency (conflicting docs cause hallucination)
Keep the spec internally consistent. Watch for: owner↔service-boundary mismatches,
tag↔prose contradictions, item↔backing-table drift, stale cross-references to removed/
superseded things, vocabulary drift, and coverage/cardinality gaps. A single,
internally-consistent, authoritative artifact is a correctness requirement.

## Decisions recorded where they happen
Log decisions with rationale in the artifact's version history; keep open questions with
an owner. The PRD/design docs are the source of truth, not this chat.

## Tooling that encodes this (how it's set up on this machine)
- **Steering (this file):** the always-on rules above.
- **Skills:** `prd-section-reviewer` (interactive, collaborative drill). Author skills
  (`trd-author`, `sdd-author`, `ttp-author`) added when those steps are reached.
- **Agent:** `spec-reviewer` — independent, cold-eyes adversarial gate (isolated context).
- **Registry:** the artifacts themselves (docs/*.md + their frontmatter). No dashboard.
Two review modes: the **interactive drill** (skill, with the user) and the **autonomous
gate** (agent, independent). They compose: drill first, then independent gate before sign-off.
