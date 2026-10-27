---
name: spec-section-reviewer
description: Collaboratively review any spec artifact section — PRD, TRD (REQs), SDD/design, or TTP — for completeness, testability, traceability, and consistency, and surface gaps not yet captured. Use when the user asks to review a TRD/REQ, a design/SDD, or a test plan section. (For PRDs specifically, prd-section-reviewer is the tuned variant.)
---

# Spec Section Reviewer (TRD / SDD / TTP / general)

The interactive, in-session review drill, generalized across artifacts. Finds gaps and
contradictions; does not rubber-stamp. Complements the independent `spec-reviewer` agent (the
autonomous gate) — drill first with the human, then run the agent before sign-off.

## Procedure
1. Confirm the target (which artifact + section) and read it fresh (don't rely on memory).
   Skim the upstream artifact it must trace to (SDD→TRD→PRD).
2. Apply the rubric below. 3. Report in the fixed format. 4. Fix collaboratively — propose
   edits, apply only what the human approves, bump version/history.

## Rubric (by artifact)
- **TRD (REQs):** each REQ single + testable ("MUST" with a pass/fail check)? Every REQ traces
  to an AC; every AC covered by a REQ? No design detail leaking in? ~5–15 REQs?
- **SDD/design:** does each REQ have an implementing component? Schema follows the generality
  steering (typed core + metadata + lookup; no EAV soup; no narrow hardcoded fields)? Tasks
  dependency-ordered, within D5 caps, each tracing to a REQ? DD-# choices surfaced not buried?
- **TTP:** does every REQ/AC have ≥1 test case? Are empty/error/boundary cases covered? Is each
  test observable (clear pass/fail)?
- **All:** internal consistency (no contradictions / stale cross-refs / vocabulary drift);
  honesty about the unverified; right-sizing; coverage/cardinality (flag orphans).

## Also (the explicit ask)
Surface 1–5 things NOT yet captured — missing REQ, uncovered edge case, unstated assumption,
a risky DD, a missing test — marked clearly as *suggestions* to accept or reject.

## Output format
```
REVIEW: <artifact / section>
Strengths:
- …
Issues (should fix):
- [high|med|low] <problem> → <fix>
Gaps / not-yet-captured (suggestions):
- <S1> …
Coverage check:
- <trace result; orphans; contradictions>
Verdict: <ready | ready-with-fixes | needs-rework>
```

## Guardrails
Review and suggest; don't edit until approved. Distinguish defects (must-fix) from suggestions.
For a cold-eyes independent audit, recommend the `spec-reviewer` agent instead.
