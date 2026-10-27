# How Spec-Driven AI Works (reference)

> Captured from screenshots the user shared (document "How Spec-Driven AI Works",
> grounded in an Ancestry SDLC process + a "PRD-28 / FT-1" example). This is a
> methodology reference for how we run Arang's PRD → design → build workflow.
> Not authored here; transcribed for our own use.

## 1. Core thesis
Pull the "flushing out" of ambiguity to the front, into precise specs. Time spent making
the definition robust pays off as code generation that is "very close to what you expect."
*Garbage in, garbage out — amplified for AI.* Context is the fuel: every artifact is
context that feeds the next step. Better context → better and faster output.

## 2. The lifecycle
A linear chain of artifacts, each with a human sign-off gate, each forcing precision on a
different question:

`PRD → TRD → SDD → TTP → build → release`

| Artifact | Forces precision on… | Owner |
|---|---|---|
| **PRD** — Product Requirements | the *what & why* — problem, goals, user flows, scope, acceptance criteria | Product |
| **TRD** — Technical Requirements | the *what the system must do* — verifiable requirements | Engineering (SME) |
| **SDD** — Software Design | the *how it gets built* — design + dependency-ordered tasks (D5) | Engineering |
| **TTP** — Test Plan | how we prove it | QA |

## 3. The traceability chain
Stable, document-internal IDs let any consumer trace a requirement from intent to proof:

`UF-### (PRD user flow) → AC-### (PRD criterion) → REQ-### (TRD) → T# (SDD task = 1 Jira story) → TC-### (TTP test)`

Cardinality: a UF fans out to several ACs; an AC to several REQs; a REQ to several tasks;
**each task is exactly one story** (the only strict 1:1). Coverage is audited at every
stage — a broken link silently breaks everything downstream.

## 4. Human–AI division of labor (DACI)
Operating rule: **Humans decide; AI executes.**
- **Humans own:** product intent, conflict resolution, sign-off gates — the decisions a
  spec encodes (which approach, which policy, which tradeoff).
- **AI owns:** translation PRD→TRD→SDD→TTP, mechanical edits, coverage audits,
  cross-consistency checks, drafting & structuring, the review drills.
Each artifact has one accountable **Approver** (PRD→Product Director; TRD/SDD→Eng
Architect/Lead; TTP→QA Lead), a **Driver** who authors it, **Contributors**, and
**Informed** parties. The artifact itself is the authoritative sign-off record — not a
chat approval or a merged PR.

## 5. The 10 mechanisms that make it work
1. **Context is the fuel** — ground every artifact in real evidence, not guesses.
2. **Humans decide; AI executes** — intent & sign-off human; translation & verification AI.
3. **Spec before code, in order** — each artifact forces precision on a distinct question.
4. **End-to-end traceability** — `UF→AC→REQ→T#→TC`; audit coverage at every stage.
5. **Verification is a first-class step** — coverage audits + cross-consistency run continuously, not at the end.
6. **Right-sizing with explicit caps** — 5–15 REQs/TRD, 5–12 tasks/SDD, one intention per flow — stops bloat *and* fragmentation.
7. **Decisions recorded where they happen** — versioned history with rationale; round-trip `triggered_by` links; OQs with owners + dates.
8. **Honesty about the unverified** — mark the boundary between known and assumed ("this PRD does not assert what it has not verified").
9. **Adversarial review from outside the vertical** — gap-finding, not rubber-stamping; one-item-at-a-time drills surface what a skim misses.
10. **Tooling encodes the process** — author / AI-review / human-review / section-review skills + registry/dashboard make the discipline repeatable, not person-dependent.

## 6. Right-sizing & decomposition
| Artifact | Guideline | Why |
|---|---|---|
| TRD | 5–15 requirements | Above ~15, agents "assume" coverage rather than verifying each; 1-session human review |
| SDD | 5–12 D5 tasks | Atomic verifiability; fits context alongside TRD+TTP |
| D5 task | 1 concern, 1 repo, ≤3 files, 1 test run | One agent pass; reversible in isolation |
| Initiative children | ≤5 features | Reconciliation fan-out |

A single PRD maps 1:1 to one Epic/TRD. When a feature outgrows the caps, **promote to an
Initiative** and split the PRD into child feature PRDs — do not hang multiple TRDs off one
PRD. *One intention per flow:* over-fragmentation (a flow per edge case) is as harmful as
over-bloat. Test: one coherent user intention, one observable outcome, a handful of
criteria. Split if multiple outcomes/actors/owners hide inside; demote to a note/AC if
it's a mechanism, not a user-facing outcome.

## 7. Verification & consistency
- **Per-item quality:** for each flow/criterion/requirement — is it well-formed,
  observable/testable, right-sized, owned, traceable?
- **Cross-section consistency** (between-the-items defects, which humans miss by eye):

| Check | Catches |
|---|---|
| Owner ↔ service boundary | A flow owned by a system that belongs to a different feature |
| Tag ↔ note agreement | A classification tag that contradicts the item's own prose |
| Item ↔ mechanism drift | A flow/criterion whose backing table row states different behavior |
| Stale cross-references | A line referencing a removed capability or a superseded value |
| Vocabulary drift | Domain-term misuse the concepts/format section rules out |
| Coverage & cardinality | Uncovered items, orphan criteria, OQs with no owner |

Principle: **"conflicting docs cause hallucination."** A single, internally-consistent,
authoritative artifact is a correctness requirement — contradictions downstream become
wrong code.

## 8. Tooling that encodes the process
| Tool | Role |
|---|---|
| `*-author` skills | Produce a conformant artifact from upstream inputs |
| `*-ai-review` skills | Autonomous, adversarial gap-finding against the spec and real codebase |
| `*-human-review` skills | Interactive interview to build the reviewer's mental model |
| `prd-section-review` / `prd-uf-review` | One-item-at-a-time interactive drill per section, with the cross-section consistency pass |
| Registry + dashboard | Single source of truth for status |
| Jira (Initiative→Epic→Story) | Planning view; bidirectionally linked to the artifact tree |

Two review *modes*: the **autonomous gate** (fast, whole-doc, no human) and the
**interactive drill** (slow, per-item, human-paced). They compose — the drill's artifacts
feed the autonomous pass so it doesn't re-litigate settled items.

## 9. Failure modes it guards against
- Plausible-but-wrong code (skipping spec order)
- Silent coverage gaps (a flow with no AC) — caught by coverage audit
- Hidden scope — caught by honest work-type tagging and sizing
- Over-fragmentation — caught by "one intention per flow"
- Drift & contradiction — caught by the cross-consistency pass
- Unverifiable assumptions as fact — caught by the honesty principle
- Rubber-stamp sign-off — caught by adversarial review and the artifact-as-record rule

## 10. The one-sentence version
It works because precise, traceable, internally-consistent context is built up front
through human-decided / AI-executed collaboration, with verification and right-sizing
enforced at every step — so that by the time code is written, the ambiguity is already gone.

---

## Relevance to Arang (our notes)
- We already adopted the PRD's 15-section template and the `UF→AC` coverage idea.
- Natural next artifacts after PRD sign-off: a **TRD** (verifiable requirements), then an
  **SDD** (dependency-ordered D5 tasks), then a **TTP** (test plan) — each now a separate file
  per stage under `docs/trd/`, `docs/sdd/`, `docs/ttp/`.
- The `prd-section-reviewer` skill we created mirrors the "interactive drill" review mode.
- Consider an independent **AI-review agent** for the "autonomous gate" cold-eyes pass.
