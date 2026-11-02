---
title: Arang — Town Entry & Coverage (P2.1) — TRD
type: TRD
artifact_id: ARANG-TRD-P2-1
parent: ARANG-PRD-P2-1
status: draft
version: 0.3
driver: (you)
approver:
approver_role: product owner / driver
assigned_date: 2026-10-31
last_revised_date: 2026-11-01
approved_date:
review_provenance: none yet — first draft. Next: spec-section-reviewer drill, then the spec-reviewer gate, then driver sign-off.
related_docs:
  - docs/p2.1-town-entry/prd.md (PRD v1.1 (v1.0 approved; v1.1 delta pending read-through) — source of the ACs)
  - docs/p2.1-town-entry/sdd.md (SDD — the "how"; written after this TRD)
  - docs/p2-consumer-discovery/trd.md (P2 TRD — REQ-01..19 there are unchanged by this feature)
  - docs/p2-consumer-discovery/sdd.md (DD-P2-3, DD-P2-9, DD-P2-11 — inputs)
origin: Authored from the approved P2.1 PRD; each REQ traces to a PRD acceptance criterion
tags: [trd, requirements, req, p2.1, town-entry, landing, waitlist, sitemap]
---

# Arang — Town Entry & Coverage (P2.1) — TRD

> **Verifiable technical requirements** for the approved P2.1 PRD (v1.0). Each `REQ-P2.1-##` is a
> single testable "the system MUST…" statement tracing to a PRD acceptance criterion. This document
> states *what must be true*. It does not name tables, libraries, route handlers, or data sources —
> that is the SDD (`docs/p2.1-town-entry/sdd.md`).
>
> Chain: `UF → AC (PRD) → REQ (here) → T# (SDD tasks) → TC (TTP)`.
>
> Numbering: REQs here use the `REQ-P2.1-##` prefix so they never collide with the P2 TRD's
> `REQ-01..19`, which remain unchanged.

## Town search and selection
- **REQ-P2.1-01** — The system MUST, for search text of 1–80 characters, return places whose name
  contains the text (case-insensitive), and for a 1–5 digit numeric text, return places having a ZIP
  that begins with it. Each returned place MUST include its name, state, and ZIP. Selecting a
  returned live place MUST open that place's events. Search text longer than 80 characters MUST be
  rejected with a validation error (HTTP 400). ⟵ AC-P2.1-01
- **REQ-P2.1-02** — For every place in the supported region, searching its exact name, or any one
  of its ZIPs, MUST return that place. When two or more places share a name in different states,
  the system MUST return all of them, each identified by state. ⟵ AC-P2.1-02

## Near me
- **REQ-P2.1-03** — Given valid coordinates from the visitor, the system MUST open the nearest live
  town if its distance is at most the configured near-me limit (default 100 miles). If the nearest
  live town is farther, the system MUST open no town, tell the visitor that Arang is not near them
  yet, and leave the search available. The system MUST NOT persist the coordinates. Coordinates
  outside latitude −90…90 or longitude −180…180, or non-numeric, MUST be rejected. ⟵ AC-P2.1-03
- **REQ-P2.1-04** — When the visitor denies location permission, the location request times out, or
  location is unavailable, the system MUST show a message that states the location could not be
  used, MUST keep the search input usable, and MUST NOT navigate to an error page. ⟵ AC-P2.1-04

## Town pages and URLs
- **REQ-P2.1-05** — Each live town MUST have a stable, human-readable URL of the form
  `/towns/<town>-<state>` (slug: lowercase letters, digits, hyphens; unique across states). Query
  parameters MUST NOT change the canonical URL. A renamed or merged town's old URL MUST redirect
  permanently to the new one. Opening that URL in a
  fresh browser MUST display that town's events with no landing step. A "change town" action from
  that page MUST return the visitor to the landing screen. ⟵ AC-P2.1-05
- **REQ-P2.1-06** — A malformed or unknown town URL MUST display the landing screen (never a blank
  page or a server error page). The URL of a resolvable place that is not live MUST display that
  place's coming-soon state. ⟵ AC-P2.1-06
- **REQ-P2.1-07** — Whether a town is live MUST be determined from stored data that can change
  without a code release. Changing a town from not-live to live (or the reverse) MUST change how the
  town's page, the search result, and the sitemap treat it, with no other change. ⟵ AC-P2.1-05,
  AC-P2.1-06, AC-P2.1-12
- **REQ-P2.1-08** — A live town with no published events in the displayed window MUST display a
  "nothing on" state with an action to widen the window, an action to change town, a link to submit
  an event, and up to 3 other live towns within the near-me limit that have at least one published
  event in the window (nearest first; omitted if none). It MUST NOT display the coming-soon state.
  ⟵ AC-P2.1-11

## Coming soon and demand capture
- **REQ-P2.1-09** — When the search text resolves to a place in the supported region that is not
  live, the system MUST display "Coming soon to <place>" with an email field. When the text resolves
  to no place in the supported region, the system MUST display a "place not found" message with a way
  to retry, and MUST NOT display a coming-soon claim for it. ⟵ AC-P2.1-07
- **REQ-P2.1-10** — On a valid email for a coming-soon place, the system MUST durably record a demand
  signal containing the normalized email, the place, the time of submission, and the consent wording
  that was shown. The system MUST display the "you're on the list" state only after that record is
  confirmed stored. ⟵ AC-P2.1-08
- **REQ-P2.1-11** — The system MUST reject an invalid email (more than 254 characters, not exactly one
  `@`, empty local or domain part, or containing whitespace or control characters) with an error that
  identifies the email field. When saving fails for any reason, the system MUST show an error, MUST
  keep the entered value, and MUST NOT display the saved state. A repeat submission of the same email
  for the same place MUST report success and MUST NOT create a second record. ⟵ AC-P2.1-09
- **REQ-P2.1-12** — The system MUST limit the rate of demand-capture submissions per client. A request
  over the limit MUST receive HTTP 429, and the page MUST show a "try again shortly" message. No
  response MUST reveal whether a given email address is already stored. ⟵ AC-P2.1-10

## Discoverability
- **REQ-P2.1-13** — The system MUST serve `/sitemap.xml` listing the landing page and exactly the live
  town URLs, each as an absolute URL on the configured public domain. It MUST list none of the
  routes `/add`, `/submit`, `/console`, `/review/…`, `/api/…`, `/webhook/…`, `/media/…`. After a
  town's live status changes, the next fetch of the sitemap MUST reflect the change. ⟵ AC-P2.1-12
- **REQ-P2.1-14** — The system MUST serve `/robots.txt` that disallows the same excluded routes as
  REQ-P2.1-13 and names the sitemap URL. ⟵ AC-P2.1-13

## Privacy
- **REQ-P2.1-15** — The system MUST NOT write a visitor's email address to the application log at
  INFO level or above, MUST NOT place it in any URL or in the sitemap, and MUST NOT return any stored
  email address in any public response. ⟵ AC-P2.1-14

## Page identity and returning visitors (added in TRD v0.2 from PRD v1.1)
- **REQ-P2.1-16** — The system MUST remember a visitor's last chosen live town in that browser only,
  open it when the visitor returns to the site root, and still allow changing town. The system MUST
  NOT send the remembered choice to, or store it on, the server. Clearing the browser's site data MUST
  reset it. ⟵ AC-P2.1-16
- **REQ-P2.1-17** — Each live-town page and the landing page MUST have a unique title, a canonical URL
  on the configured public domain, and a description naming the town (landing: the product). The
  town name and events MUST be present in the page content served to a client that does not run
  scripts. On the landing, town, coming-soon, and nothing-on screens, user-visible text, page titles,
  and accessible labels MUST show the product name "Arangly" and MUST NOT show "Arang" alone.
  ⟵ AC-P2.1-15, AC-P2.1-17

## Coverage notes (REQ ↔ AC)
| AC | Covered by |
|----|-----------|
| AC-P2.1-01 | REQ-P2.1-01 |
| AC-P2.1-02 | REQ-P2.1-02 |
| AC-P2.1-03 | REQ-P2.1-03 |
| AC-P2.1-04 | REQ-P2.1-04 |
| AC-P2.1-05 | REQ-P2.1-05, REQ-P2.1-07 |
| AC-P2.1-06 | REQ-P2.1-06, REQ-P2.1-07 |
| AC-P2.1-07 | REQ-P2.1-09 |
| AC-P2.1-08 | REQ-P2.1-10 |
| AC-P2.1-09 | REQ-P2.1-11 |
| AC-P2.1-10 | REQ-P2.1-12 |
| AC-P2.1-11 | REQ-P2.1-08 |
| AC-P2.1-12 | REQ-P2.1-13, REQ-P2.1-07 |
| AC-P2.1-13 | REQ-P2.1-14 |
| AC-P2.1-14 | REQ-P2.1-15 |
| AC-P2.1-15 | REQ-P2.1-17 |
| AC-P2.1-16 | REQ-P2.1-16 |
| AC-P2.1-17 | REQ-P2.1-17 |

- **All 17 ACs are covered by at least one REQ.** 17 REQs — **2 over the 5–15 guideline.** The two
  extra REQs come from the v1.1 additions (remember town; page identity). Flagged rather than hidden:
  the alternative is to move "remember town" and "page identity" into a P2.1b, which I judge more
  overhead than value. Driver's call.
- **Gap closed:** REQ-P2.1-15 (email privacy) now traces to AC-P2.1-14 (added in PRD v1.1).
- **Not covered by any REQ, on purpose:** the maintenance page (PRD §5, a static asset with no
  behavior to verify) and the brand name (a decision, not a requirement).
- **No measurable latency or availability target** is stated, because the PRD sets none (quantitative
  non-functional targets are deferred to P4). If you want a search-latency REQ, the PRD needs a
  number first.

## Version history
- **v0.1 (2026-10-31) — DRAFT.** Authored from the approved PRD v1.0 (13 ACs → 15 REQs). Flagged one
  PRD gap (REQ-P2.1-15 / privacy counter-metric without an AC). No design detail included.
- **v0.2 (2026-11-01) — DRAFT.** Aligned to PRD v1.1: extended REQ-P2.1-08 (nearby towns, submit link);
  REQ-P2.1-15 now traces to the new AC-P2.1-14; added REQ-P2.1-16 (remember town) and REQ-P2.1-17 (page
  identity + brand). 13 → 17 ACs, 15 → 17 REQs (2 over the guideline, flagged). No design detail added.
- **v0.3 (2026-11-01) — DRAFT.** Aligned to PRD v1.2: town URL form is `/towns/<slug>`, filters are
  query-only, and old slugs redirect permanently (REQ-P2.1-05). No new REQ.
