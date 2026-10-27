# Spec-Driven Development (SDD) Frameworks — Research & Analysis

> Captured for Arang (Oct 2026) from GitHub Spec Kit's launch post and a dev.to comparison
> (Spec Kit vs BMAD vs OpenSpec). Reporting what those sources say; per-tool details are
> pointers to verify, not independently run.

## The shared workflow (all frameworks agree on this spine)
1. **Constitution** — standards applying to every change (tests, stack, security). *(≈ Arang steering files)*
2. **Specification** — the *what & why*. *(≈ PRD)*
3. **Design/Plan** — the *how*, architecture decisions. *(≈ SDD / design.md)*
4. **Tasks** — ordered, independently testable implementation units. *(≈ our D5 tasks)*
5. **Implementation** — the agent executes; the human reviews focused changes.

Thesis: *"If the AI writes the code, the spec is the artifact. Code is the build output."*
LLMs collapsed the cost of **drafting** specs, not the cost of **quality** — catching missing
edge cases, validating assumptions, and detecting hallucinations still costs human time. The
human role shifts from *author* to *steer + verify*.

## GitHub Spec Kit (github/spec-kit · ~90K★ · greenfield-optimized)
- CLI `specify`; slash commands: `/speckit.constitution → /speckit.specify → /speckit.plan → /speckit.tasks → /speckit.implement`.
- **constitution.md** is the key idea: a rules file every subsequent spec references (testing
  strategy, security posture, stack constraints, error-handling conventions). Write once, it
  multiplies across every feature.
- Four phases with checkpoints — don't advance until the current phase is validated.
- Branch-per-spec (specs as change artifacts); brownfield is a friction point.

## BMAD-METHOD (~43K★ · multi-agent · audit/compliance-oriented)
- 12+ AI **personas** as "Agent-as-Code" Markdown files: Analyst, PM, Architect, Scrum Master,
  Developer, QA, UX Designer, …
- Pipeline: `Analyst → PM (PRD) → Architect → Scrum Master (stories) → Developer → QA`.
- Each handoff is a **versioned artifact** = audit trail; every decision traceable requirement→PR.
- **Warning (important for us):** it's a *process multiplier, not a process creator*. If you lack
  structured process it reproduces chaos across many agents; the pipeline is only as good as the
  weakest handoff; more agents = more tokens + handoff-failure debugging. Overkill for a solo prototype.

## OpenSpec (@fission-ai/openspec · lightweight · brownfield-first)
- **Change-centric** specs with `ADDED / MODIFIED / REMOVED` delta markers (forces categorizing
  every change relative to current behavior).
- Layout: `openspec/project.md`, `specs/` (current behavior), `changes/<name>/{proposal.md, design.md, tasks.md, specs/ deltas}`.
- `AGENTS.md` = "README for robots." Minimizes adoption friction.

## Pick-by-codebase (dev.to TL;DR)
- Existing codebase + features → **OpenSpec**
- New project from scratch → **Spec Kit**
- Compliance / audit / regulated → **BMAD**
- Unsure → **OpenSpec**

## Others worth knowing
- **SpecDD (specdd.ai)** — shared source of truth for intent/requirements/boundaries/completion.
- **spec-development-protocol (SDP)** — a 6-gate SDLC (no code without a spec, no incomplete spec).
- **arXiv "Consort"** — spec-first + test-driven agent framework with gates the agent *runs inside
  but cannot bypass*, and separate design-lane / build-lane role agents (enforcement in code).
- **arXiv "From Prompt to Process"** — a process taxonomy of AI-dev frameworks.
- **ianhxu/agentic-engineering-field-study** — a field study of how people actually practice SDD.

## How Arang already compares
- Arang **PRD→TRD→SDD→TTP + sign-off gates** ≈ SDP's 6 gates + Spec Kit's phases.
- Arang **author/review skills + independent reviewer agent** ≈ BMAD role agents + Consort's
  design/build role agents.
- Arang **`UF→AC→REQ→T→TC` traceability** is *more rigorous* than most ship with — a strength to keep.
- What Arang **lacks** vs. these: *enforcement-in-code* (gates the agent cannot skip) and a
  *test-driven build lane*. See Consort / SDP if we want to add that later.

## Key lessons to imbibe (not copy wholesale)
1. **Constitution = steering.** Keep Arang steering files tight and authoritative; they're the
   constitution every spec references.
2. **Skill-per-artifact** (author + reviewer) is the pattern worth adopting — it's what BMAD's
   personas and Spec Kit's phases both encode.
3. **Draft is cheap, quality is not** — the reviewer skills/agent are where the real value is.
4. **Don't over-build** (BMAD warning): a solo prototype needs a handful of good skills, not a
   7-agent mesh. Encode discipline you already follow.

## Attribution & provenance (what we adapted, from where)
Verified by reading the actual source files (not just articles), 2026-10-20:
- **GitHub Spec Kit** — `templates/commands/analyze.md`. License: **MIT**. Adapted its
  cross-artifact **detection taxonomy** (duplication/ambiguity/underspecification/constitution-
  alignment/coverage-gaps/inconsistency), **4-level severity** (CRITICAL/HIGH/MEDIUM/LOW), and
  the report/coverage/metrics table structure into `.kiro/agents/spec-reviewer.json`. Its
  "constitution is non-negotiable" rule maps to our steering files.
- **BMAD-METHOD** — `skills/bmad-build/review-prompts/edge-case-hunter.md` and
  `verification-gap.md`. License: **MIT** (BMad Code, LLC, 2025). Adapted the **edge-case path
  enumeration** and the **four verification-gap shapes** (regression / missing-adoption /
  broken-verification / stale-verification) into `.kiro/skills/code-review/SKILL.md` and the
  `ttp-author` skill.
- **GitHub Spec Kit** — `templates/commands/specify.md` + `plan.md`. License: **MIT**. Adapted
  into the **author** skills (2026-10-20): the **bounded-clarification** rule (max ~3 markers,
  priority scope > security/privacy > UX > technical, informed-defaults do/don't-ask list), the
  **testable / technology-agnostic success-criteria** rigor with good/bad examples, and the
  **self-validation checklist loop** (iterate ≤3× before hand-off) → `prd-author` + `trd-author`.
  From `plan.md`: the **phased flow** (Phase 0 resolve-unknowns-before-designing via Decision/
  Rationale/Alternatives; Phase 1 design + contracts) and the **re-check-constitution-post-design
  gate** (→ our post-design *steering* re-check) → `sdd-author`.
- **BMAD-METHOD** — `skills/bmad-prd/SKILL.md`. License: **MIT** (BMad Code, LLC, 2025; "BMad" is
  their trademark — named here only nominatively, no endorsement implied). Adapted into the author
  skills (2026-10-20): **working-mode choice** (Fast path with `[ASSUMPTION]` tags vs. Coaching
  path), **length-scales-with-stakes**, **Essential Spine vs. Adapt-In** section selection, and
  the **elicitation-not-authoring** guardrail → `prd-author`; the **addendum / rejected-alternatives
  capture** (keep the main doc lean, preserve the reasoning) → `sdd-author`.
- Method: patterns and checklists **reused and rewritten** in our own words for the Arang
  context; no verbatim copying of substantial text. Both licenses permit this; this note is the
  attribution. BMAD is also greenfield/heavy-multi-agent and Spec Kit is branch-per-spec — we did
  **not** adopt that scaffolding (hooks, scripts, memlog, branch-per-spec, multi-agent mesh), only
  the review rubrics and authoring patterns (per the "don't over-build" lesson).
