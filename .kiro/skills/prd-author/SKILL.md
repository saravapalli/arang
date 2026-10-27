---
name: prd-author
description: Draft or extend a PRD (product requirements) for an Arang feature. Use when starting a new feature's PRD or adding to one. Produces the 15-section FT-1 template with UF/AC traceability. Humans decide product intent; this skill only drafts and structures.
---

# PRD Author

Draft a right-sized PRD following the project's spec-driven workflow and the FT-1 template.
Patterns for working mode, bounded clarification, informed defaults, and the self-validation
loop are adapted from GitHub Spec Kit (`templates/commands/specify.md`, MIT) and BMAD-METHOD
(`skills/bmad-prd`, MIT) — reused and rewritten for Arang; see `docs/reference/sdd-frameworks-research.md`.

## When to use
Starting a new feature spec, or materially extending one. For *reviewing* an existing PRD, use
`prd-section-reviewer` (drill) or the `spec-reviewer` agent (gate) instead.

## Pick a working mode first (ask the human)
Offer the choice up front — it sets how much you draft vs. draw out:
- **Fast path** — batch the open gaps into one or two consolidated questions, then draft the
  full PRD, tagging every inferred decision `[ASSUMPTION]`. The human reviews and iterates.
  Initial quality scales with how much context they gave. Good when intent is already clear.
- **Coaching path** — walk the PRD sections together, one coherent chunk at a time, pulling the
  human's thinking out section by section. Good when the idea is still forming.
Default to Fast path if the human is in a hurry; don't hold them hostage to upstream probing.

## Size to the stakes
Calibrate depth before drafting — hobby/throwaway vs. internal vs. a real launch. A small
feature gets a tight PRD; don't pad sections to look thorough. Detail that doesn't earn its
place in the PRD belongs downstream (the SDD) or in OPEN QUESTIONS — not in filler prose.

## Procedure
1. **Confirm intent with the human first.** One coherent user intention per PRD. If the idea
   spans multiple intentions, say so and propose splitting into an initiative + child PRDs
   (right-sizing rule). Never invent product decisions — surface options, recommend, ask.
2. **Draft the sections.** The FT-1 **essential spine** is the expected default — include every
   section unless the feature genuinely doesn't need one, and when you drop one, drop it for a
   reason a reviewer would agree with:
   CONTEXT · PROBLEM STATEMENT · GOALS & SUCCESS METRICS · USER STORIES/USE CASES ·
   SCOPE · SERVICE BOUNDARY · VALIDATIONS ON INCOMING DATA · DEPENDENCIES & INTEGRATIONS ·
   CONSTRAINTS · CONCEPTS · ASSUMPTIONS · OPEN QUESTIONS · PRE-SIGN-OFF VERIFICATION ·
   ACCEPTANCE CRITERIA · VERSION HISTORY. Plus YAML frontmatter.
   If the feature carries a **concern the spine doesn't name** (e.g. compliance for a
   messaging/social connector, operational SLAs, a public push-API contract), add a section for
   it — name it well and place it where it serves the reader. Never skip a real concern because
   no template section covered it.
3. **Carry traceability IDs:** group user flows (`UF-###`) by theme; every UF maps to ≥1
   acceptance criterion (`AC-###`) and every AC back to a UF. Add a coverage line; flag orphans.
4. **Right-size:** one intention; a handful of ACs per flow; stop at caps (promote to an
   initiative if it outgrows them).
5. **Mark the unverified:** record assumptions explicitly; put taste calls / tradeoffs in OPEN
   QUESTIONS with a recommended default and an owner — don't silently resolve them.
6. **Validations table + outcome legend** for how incoming data is treated (store/reject/etc.).
7. **Log decisions** in VERSION HISTORY; bump the frontmatter version.

## Bounded clarification (don't spam questions)
Make informed guesses from context, the steering files, and common patterns; record them in
ASSUMPTIONS. Only raise an explicit clarification when it (a) materially changes scope or user
experience, (b) has several reasonable readings with different consequences, or (c) has no
sensible default. **Cap open clarifications at ~3**; prioritize by impact —
**scope > security/privacy > user experience > technical detail**. Present them together (with
2–3 suggested answers + implications each) rather than drip-feeding.
- *Don't ask — use a sensible default and note it:* standard error-handling/messaging, ordinary
  retention, conventional validation. *Do ask:* anything that moves scope, a product tradeoff,
  or a compliance/privacy call (e.g. whether a connector is in scope — those are gated by steering).

## Elicitation, not authoring (guardrail)
In Coaching path especially: your job is to pull the human's vision out, not insert your own.
Open-ended "tell me about X" beats a tree of multiple-choice. Infer-and-confirm ("I'm assuming
X works like Y — right?") is fine. The moment you catch yourself choosing the MVP cut, naming
the wedge, or picking phases, **stop and hand the pen back** — that's a product decision.

## Self-validation loop (before hand-off)
After drafting, run the PRD against this quick checklist and fix what fails, iterating at most
~3 times before handing to the reviewers (don't loop forever — surface residual issues instead):
- No implementation detail leaking in (no stack/API/schema — that's the SDD).
- Every section that should be filled is filled; dropped sections were dropped for a reason.
- No unresolved clarification markers left (resolved, or demoted to an ASSUMPTION/OPEN QUESTION).
- Every AC is testable and unambiguous; success metrics are measurable and user-facing.
- Every UF has ≥1 AC and every AC traces to a UF; scope is bounded; assumptions are listed.
This is a cheap self-gate — it does **not** replace the collaborative drill or the independent gate.

## Hand-off
After drafting, recommend the review path: collaborative drill (`prd-section-reviewer`) then the
independent gate (`spec-reviewer` agent) before the human signs off. Do not write code.

## Guardrails
- Humans decide; this skill drafts. Never tick the PRE-SIGN-OFF boxes — that's the human's act.
- Keep the PRD consistent with the steering files (generality, intake-source-as-data, user/role).
