---
title: Arang — Consumer Discovery (P2) — TRD
type: TRD
artifact_id: ARANG-TRD-P2
parent: ARANG-PRD-P2
status: draft
version: 1.1
driver: (you)
approver:
approver_role: product owner / driver
assigned_date: 2026-10-30
last_revised_date: 2026-10-30
approved_date:
review_provenance:
related_docs:
  - docs/p2-consumer-discovery/prd.md (approved PRD v1.0 — source of ACs)
  - docs/p2-consumer-discovery/sdd.md (SDD — the "how"; written after this TRD)
  - docs/p1-producer-intake/sdd.md (schema + the LLM enrichment path P2 extends)
  - docs/p1-producer-intake/prd.md (P1 — produces the published events P2 reads)
origin: Authored from the approved P2 PRD; each REQ traces to a PRD acceptance criterion
tags: [trd, requirements, req, p2, consumer-discovery]
---

# Arang — Consumer Discovery (P2) — TRD

> **Verifiable technical requirements** for the approved P2 PRD (v1.0). Each `REQ-###` is a single
> testable "the system MUST…" statement tracing to a PRD acceptance criterion. This doc states
> *what the system must do*; the *how* (React SPA, API shape, library choices, radius tuning) is
> the SDD (`docs/p2-consumer-discovery/sdd.md`).
>
> Chain: `UF → AC (PRD) → REQ (here) → T# (SDD tasks) → TC (TTP)`.

## Discovery query (window + area)
- **REQ-01** — The system MUST provide a discovery query that accepts a location and an optional
  `from`/`to` datetime window, and MUST return only events with `status='published'` whose
  `starts_at` is within the window. ⟵ AC-P2-01
- **REQ-02** — When no window is supplied, the system MUST default to "this week" (Monday 00:00 to
  Sunday 23:59), computed in the configured application timezone. ⟵ AC-P2-01
- **REQ-03** — The system MUST match an event to the queried area when the event's `postal_code`
  equals the query postal code, OR (when the query resolves to a centroid) when the event's
  centroid is within a configurable radius (default 15 miles) of the query centroid. ⟵ AC-P2-01
- **REQ-04** — The system MUST reject a query where `from` is after `to` with an error, and MUST
  NOT silently swap the bounds. ⟵ AC-P2-01 (V6)
- **REQ-05** — When the queried location resolves to no area, the system MUST return an empty
  result set (not an error). ⟵ AC-P2-01 (V7)

## Presentation (map + list + detail)
- **REQ-06** — For each result that has a derived centroid, the system MUST expose its
  approximate `latitude`/`longitude` so the client can place a pin. ⟵ AC-P2-03
- **REQ-07** — The system MUST mark results as approximately placed, and a published event that has
  a postal code but no derived centroid MUST be returned as list-only (not pinnable). ⟵ AC-P2-05
- **REQ-08** — The system MUST support selecting one or more categories so that results (the
  returned set the client renders on map and list) include only events whose `event_category`
  contains any selected category (OR semantics). ⟵ AC-P2-02
- **REQ-09** — For a single event, the system MUST expose its title, start time, location, link
  metadata, and the linked business when present. ⟵ AC-P2-04

## Interests (capture + personalized default)
- **REQ-10** — The system MUST let a consumer save interests (one or more categories, and optional
  locations) persisted to the `interest` table, keyed to the consumer's recognized identity. ⟵ AC-P2-06
- **REQ-11** — The system MUST reject an interest whose category/tag value is not in the known
  vocabulary. ⟵ AC-P2-06 (V5)
- **REQ-12** — The system MUST recognize a returning consumer by a stable id — the WhatsApp phone
  for WhatsApp arrivals, or a persistent browser cookie id for web — and associate interests with
  the matching `user` row. Recognition is best-effort for web (same browser/device). ⟵ AC-P2-07
- **REQ-13** — For a recognized returning consumer, the system MUST pre-filter the default results
  by their saved interests using OR across saved categories, and MUST NOT use the interest `weight`
  column for ranking in the MVP. ⟵ AC-P2-07
- **REQ-14** — When a recognized consumer's interests yield zero results in the window+area, the
  system MUST return the unfiltered in-window, in-area results and indicate that the interest filter
  matched nothing. ⟵ AC-P2-07

## Categorization (P2-owned enrichment extension)
- **REQ-15** — During enrichment, the system MUST map an event to zero or more categories drawn
  only from the curated category vocabulary, writing each as an `event_category` row. ⟵ AC-P2-10
- **REQ-16** — During enrichment, the system MAY attach finer free-form tags to an event, writing
  each as an `event_tag` row; a tag value need not pre-exist. ⟵ AC-P2-10
- **REQ-17** — The system MUST seed and maintain the curated category vocabulary as a controlled,
  fixed-for-the-MVP set (changed only by a deliberate, versioned edit, not by consumer input). ⟵ AC-P2-10
- **REQ-18** — On the review page, the system MUST let a producer view the event's assigned
  categories and add or remove them, persisting the change to `event_category`. ⟵ AC-P2-11

## Access / channel (API)
- **REQ-19** — The system MUST expose the discovery query and the interest capture/read operations
  as JSON endpoints suitable for a browser SPA client. ⟵ AC-P2-01, AC-P2-06 (OQ-P2-5 decision)

## Stretch (not required for the P2 core)
- **REQ-S1** — The system SHOULD answer a consumer's WhatsApp "what's on?" message with a
  summarized list plus a link to the discovery view. ⟵ AC-P2-08 `[stretch]`
- **REQ-S2** — The system SHOULD support a calendar presentation of the same filtered results by
  time slot. ⟵ AC-P2-09 `[stretch]`

## REQ ↔ AC coverage
| AC | REQs |
|---|---|
| AC-P2-01 | REQ-01, REQ-02, REQ-03, REQ-04, REQ-05, REQ-19 |
| AC-P2-02 | REQ-08 |
| AC-P2-03 | REQ-06 |
| AC-P2-04 | REQ-09 |
| AC-P2-05 | REQ-07 |
| AC-P2-06 | REQ-10, REQ-11, REQ-19 |
| AC-P2-07 | REQ-12, REQ-13, REQ-14 |
| AC-P2-08 (stretch) | REQ-S1 |
| AC-P2-09 (stretch) | REQ-S2 |
| AC-P2-10 | REQ-15, REQ-16, REQ-17 |
| AC-P2-11 | REQ-18 |

**Coverage:** every core AC (01–07, 10, 11) maps to ≥1 REQ; every REQ traces to ≥1 AC. Stretch ACs
(08/09) map to stretch REQs (S1/S2). Core REQ count = 19 — above the 5–15 guideline because P2
bundles three concerns (discovery query, interests/identity, and the categorization enrichment it
owns). This is flagged, not hidden. If the build needs slicing, the categorization REQs (15–18) are
the natural first slice, since discovery filtering depends on them.

## Notes carried from the PRD gate (for the SDD)
- N3: define the area/interest interaction on an empty interest result (REQ-14 covers the window
  +area fallback; the SDD confirms ordering).
- N4: define P2 behavior when LLM enrichment is disabled — categories are absent, so category
  filters return everything; discovery by window+area still works.

## Version history
- **v1.1 (2026-10-30):** REQ-03 radius default changed from 25 km to **15 miles** (driver decision
  during the P2 SDD gate, for a clearer US-prototype unit). Aligns the TRD with SDD DD-P2-3. No
  change to the rule itself (same-postal OR within-radius) or to coverage.
- **v1.0 (2026-10-30):** Initial TRD from the approved PRD v1.0. 19 core REQs + 2 stretch, each
  traced to an AC; full REQ↔AC coverage table. REQ count flagged as above-guideline (three bundled
  concerns) with a suggested first slice (categorization). No design detail (that is the SDD).
