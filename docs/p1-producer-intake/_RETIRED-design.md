---
title: Arang — Producer Intake (P1) — Design (RETIRED / SPLIT)
type: pointer
status: retired
superseded_by:
  - docs/p1-producer-intake/trd.md (TRD — REQ-01..18)
  - docs/p1-producer-intake/sdd.md (SDD — schema, behavior, diagrams, tasks)
  - docs/p1-producer-intake/ttp.md (TTP — TC-### test plan)
last_revised_date: 2026-10-20
---

# Retired — this file was split into one-file-per-stage (2026-10-20)

This document previously folded the **TRD** (REQ section) and the **SDD** (design + tasks) into
a single `design.md`, per an earlier prototype decision. That decision was **reversed**: each
lifecycle stage now has its own file with its own frontmatter and format.

**Go to:**
- **TRD** (verifiable requirements, REQ-01..18): `docs/p1-producer-intake/trd.md`
- **SDD** (schema, behavior→impl, diagrams, dependency-ordered tasks, design decisions): `docs/p1-producer-intake/sdd.md`
- **TTP** (test plan, TC-### tracing to REQs): `docs/p1-producer-intake/ttp.md`

## Pre-split version history (preserved for the record)
- **v0.1 (2026-10-14):** Initial SDD for approved P1 PRD v1.0. Folded TRD (REQ-01..18 mapped
  to ACs), MySQL schema, behavior→impl, module structure, checks, 9 dependency-ordered tasks
  (slice 1 = T1–T4), and 5 flagged design decisions (DD-1..5). No code written.
- **v0.2 (2026-10-20):** Added a Diagrams section (ER, sequence, state, component as Mermaid).
- **split (2026-10-20):** Content moved verbatim into the three files above; this stub retained
  so the former path and history resolve. Safe to delete once no external links point here.
