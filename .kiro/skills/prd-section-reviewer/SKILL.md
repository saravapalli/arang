---
name: prd-section-reviewer
description: Review a PRD section, user flow, or acceptance criterion for completeness, testability, and consistency — and surface gaps, risks, or ideas NOT yet captured. Use when the user asks to review the PRD, a section, a flow, or a decision in docs/prd.md (or any spec document). Works section-by-section and collaboratively.
---

# PRD Section Reviewer

A repeatable procedure for reviewing a PRD collaboratively, one section/flow/criterion
at a time. The goal is not to rubber-stamp: actively find gaps, contradictions,
untestable statements, and **ideas or edge cases the author has not yet considered**.

## How to run a review
1. **Confirm the target.** Ask what to review (a numbered section, a user flow U#, an
   acceptance criterion AC#, or "the whole PRD"). Default to the section the user names.
2. **Read it fresh.** Re-read the target in `docs/prd.md` (don't rely on memory of prior
   turns). Also skim the sections it depends on (e.g. an AC depends on CONCEPTS/VALIDATIONS).
3. **Apply the rubric** (below) to the target.
4. **Report findings** in the fixed output format (below). Keep it tight.
5. **Fix collaboratively.** Propose concrete edits; apply only what the user approves.
   Record decisions and bump the PRD version/history as usual.

## Review rubric — check each dimension
For the target under review, check:

- **Testable / observable:** Is every claim phrased as behavior you could verify?
  Flag vague words ("fast", "simple", "intuitive", "robust") with no measurable check.
- **Completeness of the flow:** For a user flow, are the empty, error, and boundary
  states covered — not just the happy path? What happens on bad/missing/duplicate input?
- **Consistency:** Does it contradict another section, a resolved decision (D#), or the
  steering principles (generality, intake-source-is-data, typed-core-vs-metadata)?
- **Coverage mapping:** Does each user story map to at least one acceptance criterion,
  and vice versa? Flag orphans (a story with no AC, an AC with no story).
- **Scope & priority fit:** Is the item in the right priority bucket (P1/P2/P3)? Does it
  smuggle later-slice work into an early slice?
- **Provenance/edge for Arang specifically:** events can originate from user / ingestion
  source / api_client; is the right origin handled? Is the typed-core vs. metadata split
  honored (no new narrow columns for unpredictable data)?
- **Feasibility & compliance:** Any legal/ToS, cost, or platform limits understated?
  (e.g. messaging/social scraping, WhatsApp 24-hour window, OCR quality on newsprint.)

## Also do this (the part the user explicitly asked for)
- **Surface what's NOT there.** Beyond critiquing what's written, propose 1–5 things the
  section/flow is missing or should consider that the author hasn't mentioned — new edge
  cases, a missing user story, an unstated assumption, a failure mode, a metric. Mark
  these clearly as *suggestions* so the user can accept or reject each.

## Output format (use consistently)
```
REVIEW: <section / flow / AC under review>

Strengths:
- <what is already solid>

Issues (should fix):
- [severity: high|med|low] <problem> → <suggested fix>

Gaps / not-yet-captured (suggestions — accept or reject each):
- <S1> <idea or edge case not currently in the PRD>
- <S2> ...

Coverage check:
- <story↔AC mapping result, orphans, contradictions>

Verdict: <ready | ready-with-fixes | needs-rework>
```

## Boundaries
- Review and suggest; **do not** edit the PRD until the user approves specific changes.
- One section/flow at a time unless asked for a full pass.
- Distinguish clearly between *defects* (must fix) and *suggestions* (optional ideas).
- For a truly independent cold-eyes audit (no conversation context), recommend running a
  separate reviewer **agent** instead — this skill is for collaborative, in-session review.
