---
title: Arang — Consumer Discovery (P2) — TTP
type: TTP
artifact_id: ARANG-TTP-P2
parent: ARANG-SDD-P2
status: draft
version: 0.1
driver: (you)
approver:
approver_role: product owner / driver
assigned_date: 2026-10-30
last_revised_date: 2026-10-30
approved_date:
review_provenance:
related_docs:
  - docs/p2-consumer-discovery/prd.md (approved PRD v1.1 — ACs)
  - docs/p2-consumer-discovery/trd.md (TRD v1.1 — REQ-01..19 these TCs prove)
  - docs/p2-consumer-discovery/sdd.md (approved SDD v1.3 — §5 Checks + §6 slices this plan follows)
origin: Lean P2 test plan authored after SDD approval; one TC per core REQ, tracing to the SDD §5 checks.
tags: [ttp, test-plan, tc, p2, consumer-discovery]
---

# Arang — Consumer Discovery (P2) — TTP

> Proves each core requirement in the approved SDD (ARANG-SDD-P2 v1.3). Lean by design (prototype):
> one `TC-###` per core REQ-01..19, tracing to the SDD §5 check and the REQ/AC chain. Stretch
> REQ-S1/S2 are deferred and out of scope here. Test type is noted per TC: **[unit]**, **[int]**
> (Spring + H2), or **[e2e]** (Playwright over the same-origin SPA).
>
> Chain: `AC (PRD) → REQ (TRD) → T# (SDD §6) → TC (here)`.

## S-A — T1: Vocabulary + schema migration (REQ-17)
- **TC-01** `[int]` After the V2 migration runs, the curated `category` table has the 15 seeded rows,
  `user.external_ref` exists as a nullable UNIQUE column, and `idx_event_pub_starts` exists.
  *(REQ-17)*

## S-B — T2/T3/T4: Discovery query + endpoints (REQ-01..09, 19)
- **TC-02** `[int]` A discovery query with no window returns only `status='published'` events whose
  `starts_at` is inside this week `[Mon 00:00, Sun 23:59]` in the configured zone. A draft in-window
  is excluded. *(REQ-01, REQ-02)*
- **TC-03** `[unit]` `WeekWindow` computes `[Mon 00:00:00, Sun 23:59:59]` in `America/New_York`,
  including a week that crosses a DST change (edges stay at local midnight). *(REQ-02)*
- **TC-04** `[int]` Area match: an event with the same `postal_code` matches; an event whose
  centroid is within 15 miles of the query centroid matches; one beyond 15 miles does not.
  *(REQ-03)*
- **TC-05** `[int]` `from > to` → HTTP 400 with body `{"error":"invalid_window",...}` and no query
  runs. *(REQ-04)*
- **TC-06** `[int]` A location with no resolvable centroid AND no postal match → HTTP 200 with an
  empty `results` array (not an error). *(REQ-05)*
- **TC-07** `[int]` Result shape: an event with both `lat` and `lng` is pinnable; an event with a
  postal code but null centroid is present in `results` but has null `lat/lng` (listed, not pinned).
  *(REQ-06, REQ-07)*
- **TC-08** `[int]` Category filter: a query with `categories=[music,food]` returns events having
  music OR food (OR semantics), excludes events with neither. *(REQ-08)*
- **TC-09** `[int]` `GET /api/events/{id}`: a published id → 200 `EventDetail`; a draft id or an
  unknown id → 404. *(REQ-09)*
- **TC-10** `[int]` The discovery envelope matches the contract: `window`, `area`, `truncated`,
  `interestFilterMatchedNothing`, `results[]`; `results` ordered `starts_at ASC`; `truncated:true`
  once the result cap is exceeded. `GET /api/categories` returns the seeded list. *(REQ-19, M2)*

## S-C — T5/T6/T7: Identity + interests (REQ-10..14)
- **TC-11** `[int]` Identity resolution: a known WhatsApp phone maps to its existing `user`; a web
  cookie id maps to a `user` by `external_ref` (created on first interest save); **no cookie →
  unrecognized and no `user` row is created**. *(REQ-12)*
- **TC-12** `[int]` A concurrent/duplicate `external_ref` save does not throw: the resolver catches
  the UNIQUE collision and re-selects the existing row (find-or-create). *(REQ-12)*
- **TC-13** `[int]` Interest save: a valid curated category is saved; an off-vocabulary category is
  rejected with 400; any tag value is accepted as-is (never rejected). *(REQ-10, REQ-11)*
- **TC-14** `[int]` A recognized consumer with saved interests gets discovery pre-filtered by those
  interest categories (OR). *(REQ-13)*
- **TC-15** `[int]` A consumer with no interests (or unrecognized) gets the in-window/in-area set
  unfiltered by category, and the envelope carries `interestFilterMatchedNothing:true`. *(REQ-14)*

## S-D — T8/T9/T10: React SPA (REQ-19, presentation) `[e2e]`
- **TC-16** `[e2e]` The SPA loads over the same origin, renders the map (OSM tiles load) and the
  event list, and selecting a list item highlights its map pin and vice-versa (list↔map sync).
  *(REQ-19, REQ-06)*
- **TC-17** `[e2e]` Filter pills narrow the list + pins to the chosen categories; the event detail
  sheet opens with a working Google Maps link; the interest picker saves to `/api/interests` and the
  next default load reflects the saved interests. *(REQ-19, REQ-08, REQ-13)*

## S-E — T11/T12: Categorization enrichment (REQ-15, REQ-16, REQ-18) `[parallel]`
- **TC-18** `[int]` Enrichment maps LLM output to **curated categories only** (writes
  `event_category`), and never invents a category outside the seeded set. *(REQ-15, REQ-17)*
- **TC-19** `[int]` Enrichment MAY mint an open tag (writes `event_tag`) for a concept outside the
  curated set. *(REQ-16)*
- **TC-20** `[int]` The review page adds and removes an event category, and the change persists to
  `event_category`. *(REQ-18)*

## S-F — T13: End-to-end (manual + Playwright)
- **TC-21** `[e2e/manual]` Seed published events across categories/postals, load the SPA, run a
  this-week discovery for a postal code, verify the list + pins, apply a filter, save an interest,
  reload and confirm the interest-filtered default. DB-verify the written `interest` row.
  *(all core REQs)*

## TC ↔ REQ coverage
| REQ | TC(s) | REQ | TC(s) |
|---|---|---|---|
| REQ-01 | TC-02 | REQ-11 | TC-13 |
| REQ-02 | TC-02, TC-03 | REQ-12 | TC-11, TC-12 |
| REQ-03 | TC-04 | REQ-13 | TC-14, TC-17 |
| REQ-04 | TC-05 | REQ-14 | TC-15 |
| REQ-05 | TC-06 | REQ-15 | TC-18 |
| REQ-06 | TC-07, TC-16 | REQ-16 | TC-19 |
| REQ-07 | TC-07 | REQ-17 | TC-01, TC-18 |
| REQ-08 | TC-08, TC-17 | REQ-18 | TC-20 |
| REQ-09 | TC-09 | REQ-19 | TC-10, TC-16, TC-17 |
| REQ-10 | TC-13 | — | — |

Every core REQ-01..19 has ≥1 TC. Stretch REQ-S1/S2: no TC (deferred, out of scope).

## Open gaps / notes
- Timezone TC (TC-03) assumes the configured zone is `America/New_York` (SDD DD-P2-7). Change the
  TC if the deployed zone differs.
- OSM tile load in TC-16 depends on the public tile server being reachable (SDD DD-P2-4); a tile-
  server outage is an environment failure, not a product defect.
- Pagination is not tested beyond the `truncated` flag (TC-10) — the MVP has no offset paging (SDD
  M2 named assumption).

## Version history
- **v0.1 (2026-10-30):** Initial lean P2 test plan from approved SDD v1.3. 21 TCs grouped by the SDD
  §6 slices (S-A..S-F); one TC per core REQ-01..19 with full TC↔REQ coverage. Stretch S1/S2 excluded
  (deferred). Draft, pending driver review.
