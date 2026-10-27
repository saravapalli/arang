---
name: sdd-author
description: Author the SDD (software design + dependency-ordered tasks) for an Arang feature from its approved PRD and TRD. Produces schema, behavior-to-implementation, module structure, checks, and D5 task breakdown. Use after the PRD is approved and the TRD/REQs exist.
---

# SDD Author

Design *how* the feature is built, tracing to the TRD's REQs, and break it into buildable tasks.
The phased flow (resolve-unknowns-before-designing, re-check steering after design) is adapted
from GitHub Spec Kit's plan workflow (`templates/commands/plan.md`, MIT); the rejected-alternatives
addendum is adapted from BMAD-METHOD (`skills/bmad-prd`, MIT) — reused and rewritten for Arang.
See `docs/reference/sdd-frameworks-research.md`.

## When to use
After the PRD is approved and REQs (TRD) exist. Produces the SDD as its own file, one folder per
feature with stage-named files: `docs/<feature>/sdd.md` (alongside `prd.md`, `trd.md`, `ttp.md`).

## Phase 0 — resolve unknowns before designing
Before writing any design, list the open technical unknowns the REQs imply (a library choice, an
integration shape, a data-modeling question). Resolve each with a one-line **Decision ·
Rationale · Alternatives considered** note. Where the choice is a product/tradeoff call the PRD
left open, it's a `DD-#` to surface — don't silently resolve it. Don't start Phase 1 with
unresolved unknowns sitting in the design; either decide them or raise them as DD-#.

## Phase 1 — design & tasks
1. **Read the approved PRD + REQs.** Design to the REQs; don't add scope.
2. **Schema** (if data changes): follow the generality steering — typed core on primary
   entities, per-entity key/value metadata, lookup/join for shared vocabulary. Create only what
   this feature needs, but design broad enough to avoid foreseeable migrations. Show DDL.
3. **Behavior → implementation:** for each REQ, name the component/flow that realizes it.
4. **Module/package structure:** the layout and which files the feature adds.
5. **Contracts (if the feature exposes an interface):** name the external surface it adds — a
   REST endpoint shape, a WhatsApp webhook payload, a push-API contract, a connector interface —
   as a contract downstream tests can pin to. Skip for purely internal changes.
6. **Diagrams:** picture what prose makes the reader assemble by hand. Use **Mermaid** fenced
   blocks (```` ```mermaid ````) embedded in the doc — it renders on GitHub/most editors, it is
   diff-able plain text (fits "the spec is the long-lived artifact"), and needs no binary files.
   Include a diagram only when the design actually has that shape — don't draw empty ones:

   | Diagram | Mermaid kind | Captures | Include when |
   |---|---|---|---|
   | **ER** | `erDiagram` | tables + FK relationships from the schema | the feature touches the schema |
   | **Sequence** | `sequenceDiagram` | multi-service request flow (who calls whom, in order), incl. alt/error branches | any flow crossing ≥2 components |
   | **State** | `stateDiagram-v2` | entity lifecycle + transitions (e.g. `draft→published`) | the feature has a status/lifecycle |
   | **Component** | `flowchart` | package/module layout + external edges | the feature adds modules or external integrations |

   Label nodes/edges with the REQ they realize where it helps traceability. Keep diagrams in
   sync with the schema (§2) and behavior (§3) — a diagram that drifts from the text is worse
   than none.
7. **Checks:** the command or manual step that proves each REQ (feeds the TTP).
8. **Dependency-ordered tasks (D5):** cap ~5–12; each = 1 concern / 1 repo / ≤3 files / 1 test
   run; order so each builds on the last; mark which tasks form the first runnable slice.
9. **Flag design decisions (DD-#):** where the PRD was silent on a technical choice, propose a
   default and ask — don't bake it in silently.
10. **Version-log.**

## Post-design steering re-check (gate)
After the design is drafted, re-read the steering files and confirm the design still complies —
design pressure often tempts a shortcut that violates a principle. Check specifically:
- **Generality:** is a new kind/variation modeled as data (`event_type`, metadata, a role value)
  rather than a new primary table or a hardcoded narrow column?
- **Intake-as-data:** does a new source go through the connector interface + generic `origin_type`,
  not a change to the `event` table or the pipeline back half?
- **Identity vs. role:** identity stays on `user`; anything role-like is a `user_role` value.
- **Stack:** Java + Spring Boot, MySQL, Gradle.
If the design violates a principle, either fix it or raise it as a `DD-#` for the human — don't
ship a design that quietly breaks steering.

## Capture rejected alternatives (don't lose them)
When you weigh an approach and set it aside, record the option and *why* it lost in a short
"Alternatives considered" note next to the decision (or a design appendix). This keeps the main
design lean while preserving the reasoning, so a later reader doesn't reopen a settled question.
Deep technical "how", options matrices, and rejected-mechanism rationale live here — not inflating
the PRD upstream.

## Self-validation loop (before hand-off)
After drafting, check the design against this list and fix what fails, iterating at most ~3
times, then surface anything still open:
- Every REQ has ≥1 task and ≥1 check; every task traces to ≥1 REQ (no orphans).
- Phase 0 unknowns are all resolved or raised as DD-#.
- The post-design steering re-check passes (or violations are raised as DD-#).
- Diagrams present for every shape the design has (schema→ER, cross-component flow→sequence,
  lifecycle→state, modules/externals→component), and each is consistent with the text.
- Task count is within ~5–12, each within the 1-concern/≤3-files bound; the first slice is marked.
This is a cheap self-gate — it does not replace the `spec-section-reviewer` drill or the
`spec-reviewer` gate.

## Hand-off
Recommend a review (the `spec-section-reviewer` skill or the `spec-reviewer` agent). Do not write
application code until the human approves the design.

## Guardrails
- Trace every task to ≥1 REQ; every REQ to ≥1 task. Flag orphans.
- Respect confirmed stack: Java + Spring Boot, MySQL, Gradle.
- Humans decide the DD-# choices; surface and recommend.
