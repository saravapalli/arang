---
title: Arang — Business & Venue Linking (P5)
type: PRD
artifact_id: ARANG-PRD-P5
parent: ARANG-INIT-001
status: draft
version: 0.1
driver: (you)
approver:
approver_role: product owner / driver
assigned_date: 2026-10-26
first_draft_target:
last_revised_date: 2026-10-26
review_provenance: not yet reviewed (drafted by prd-author; pending collaborative drill + spec-reviewer gate)
jira:
multifeature: false
related_docs:
  - docs/prd.md (initiative overview — Epic 6 / P5)
  - docs/producer-intake/prd.md (P1 — where events + the nullable event.business_id originate)
  - docs/producer-intake/sdd.md (schema: business / business_metadata already created, unused)
  - .kiro/steering/design-philosophy.md (business as an optional, type-driven link; metadata escape hatch)
origin: Carved from the initiative to give the already-modeled `business` entity a real feature home; surfaced by E2E showing business/* tables never populate (no P1–P4 code path writes them)
tags: [business, venue, organizer, linking, dedupe, p5]
---

# Arang — Business & Venue Linking (P5)

> **One coherent intention:** *let an event be associated with the business/venue/organizer behind
> it, so events can be grouped, trusted, and discovered by who runs them.* The `business` and
> `business_metadata` tables and the nullable `event.business_id` FK already exist (created in the
> P1 SDD per the generality steering) but **no phase populates them yet** — this PRD defines the
> feature that does. Scope is the business entity + its link to events; it does not build
> business-owner accounts/auth (that rides on the future role/auth work).

## 1. CONTEXT
Arang's model treats a **business** (a venue, shop, or organizer) as a first-class entity that an
event may optionally belong to — e.g. a shop publishing a weekend offer, or a venue hosting a
concert. The initiative CONCEPTS define it; the schema reserves it. But P1 (producer intake) only
creates bare events; P2 (discovery), P3 (ingestion), and P4 (hardening) don't touch businesses
either. As a result `event.business_id` is always null and the `business*` tables are always empty.
P5 closes that gap: deriving/creating a business from a submission and linking events to it.

## 2. PROBLEM STATEMENT
Events today float free of the organization behind them. We cannot answer "show me everything at
The Cellar" or "all offers from this shop," cannot de-duplicate the same venue across many events,
and cannot later attribute events to a business owner. Producers already name venues/organizers
in their messages and flyers (the LLM extracts them as free text), but that signal is discarded
into a `location` string or a metadata attribute instead of a reusable `business` record.

## 3. GOALS & SUCCESS METRICS
- **G1** — An event can be linked to a business; `event.business_id` resolves to a real row.
  *Metric: a published event with a named venue/organizer has a non-null `business_id`.*
- **G2** — The same venue/organizer across multiple events links to **one** business row (dedupe).
  *Metric: two events naming the same venue share one `business_id`.*
- **G3** — Business details beyond the typed core (phone, website, socials) are captured as
  `business_metadata`, not lost. *Metric: extracted contact details appear as business_metadata rows.*
- **Counter-metric:** we must not fabricate businesses for events that have none (a user-submitted
  gig with no venue). *Metric: events with no identifiable business keep `business_id` null.*

## 4. USER STORIES / USE CASES
Tag legend: `[core]` P5 must-have · `[stretch]` if cheap.

### Group A — Linking at intake
- **UF-P5-1 (auto-link on submit)** `[core]` — As a producer submitting an event that names a
  venue/organizer, the system associates my event with a business so it's grouped with that
  business's other events.
- **UF-P5-2 (dedupe)** `[core]` — As the platform, when an event names a business that already
  exists, the system links to the existing business rather than creating a duplicate.
- **UF-P5-3 (capture business details)** `[core]` — As a producer whose flyer lists the
  organizer's phone/website/socials, those details are stored on the business for reuse.

### Group B — Review & correction
- **UF-P5-4 (review shows the business)** `[core]` — As a producer on the review page, I can see
  the business my event was linked to and correct it (change the name, detach, or set none).
- **UF-P5-5 (manual business entry)** `[stretch]` — As a producer, I can type a business/venue
  name on the web form and have it linked/created.

### Group C — Grouping (read)
- **UF-P5-6 (events by business)** `[stretch]` — As an operator, I can list all events linked to
  a given business. *(Full consumer-facing "venue page" is P2 discovery's concern; P5 provides the
  link + a minimal operator lookup.)*

## 5. SCOPE
**In:** creating/linking a `business` from an event's extracted venue/organizer; dedupe on a
normalized business key; storing business details as `business_metadata`; showing/editing the link
on the review page; a minimal "events for business" lookup.
**Out (later/other phases):** business-owner **accounts & auth** (rides on the role/auth work),
consumer-facing venue pages (P2 discovery), business analytics, claiming/verifying a business,
merging businesses by a human operator (beyond automatic dedupe).

## 6. SERVICE BOUNDARY
P5 extends the existing intake/enrichment service and the review surface; it introduces a
`BusinessService`/`BusinessDao` but reuses the event pipeline, the LLM `EventExtractor`
(to also emit a business), and the review controller. No new external integrations.

## 7. VALIDATIONS ON INCOMING DATA
| # | Rule | When | Outcome |
|---|---|---|---|
| V1 | A business must have a non-blank `name` to be created | on-link | reject (leave business_id null) |
| V2 | Dedupe key = normalized(name [+ postal/locality when present]); match → reuse | on-link | link to existing |
| V3 | No identifiable business in the submission | on-link | store none; business_id stays null |
| V4 | Business detail values (phone/url/handle) captured verbatim as business_metadata | on-link | store |
| V5 | Producer-set business name on the review page overrides the auto-derived one | on-edit | relink/create |

## 8. DEPENDENCIES & INTEGRATIONS
- The LLM extraction (P1 enrichment) must additionally return a candidate business
  (name + details) — an extension of the existing `ExtractedFields`.
- The `business` / `business_metadata` tables + `event.business_id` FK (already in the schema).
- The review page (P1) to display/edit the link.

## 9. CONSTRAINTS
Stack unchanged (Java/Spring Boot, MySQL, JdbcTemplate). Dedupe is best-effort/normalized-string
for P5 (no fuzzy/entity-resolution service). Business linking is **advisory** — the producer can
always correct it, consistent with the "capture losslessly, confirm at review" ethos.

## 10. CONCEPTS
- **Business:** venue/shop/organizer; typed core (name, location, postal, geo) + `business_metadata`
  for the unpredictable extras (phone, website, instagram, hours).
- **Business link:** `event.business_id` (nullable) — set when a business is identified.
- **Dedupe key:** a normalized name (lowercased, trimmed, punctuation-stripped), optionally
  qualified by postal/locality, used to find-or-create.
- **Owner (deferred):** `business.owner_user_id` exists but P5 does not populate it (needs auth).

## 11. ASSUMPTIONS
- **A1:** Most business signal comes from the LLM extraction already running on P1 submissions.
- **A2:** Normalized-string dedupe is "good enough" for the prototype; false-merges are rare and
  correctable on review.
- **A3:** A business with no address still links by name alone (name-only dedupe).

## 12. OPEN QUESTIONS
| # | Question | Status | Note / default | Owner |
|---|---|---|---|---|
| OQ-P5-1 | Dedupe scope — name-only vs name+postal? | OPEN | Default: name+postal when postal present, else name-only | driver |
| OQ-P5-2 | Should an event's `location` string also become the business name when no explicit organizer? | OPEN | Default: yes, treat a named venue as the business | driver |
| OQ-P5-3 | Auto-link confidence threshold — only link above some LLM confidence? | OPEN | Default: link if a name is present; flag low-confidence on review | driver |
| OQ-P5-4 | Operator "events by business" surface — API only or a page? | OPEN (deferred) | Minimal JSON lookup in P5; page is P2 | driver |

## 13. PRE-SIGN-OFF VERIFICATION
- [ ] One coherent intention (business↔event linking) — not conflating business-owner auth.
- [ ] Every UF has ≥1 AC below; every AC traces to a UF.
- [ ] Dedupe behavior (V2) and the "don't fabricate" counter-metric are both covered by ACs.
- [ ] Scope line explicitly defers accounts/auth, consumer venue pages, and human merge.
- [ ] OQ-P5-1..4 resolved or consciously deferred before the TRD.

## 14. ACCEPTANCE CRITERIA
- **AC-P5-01** ⟵ UF-P5-1: Submitting an event naming a venue/organizer results in a `business`
  row and `event.business_id` set to it.
- **AC-P5-02** ⟵ UF-P5-1 (counter): Submitting an event with no identifiable business leaves
  `business_id` null and creates no business row.
- **AC-P5-03** ⟵ UF-P5-2: Two events naming the same business (per the dedupe key) link to the
  **same** `business.id`; no duplicate business row is created.
- **AC-P5-04** ⟵ UF-P5-3: Extracted business details (phone/website/socials) are stored as
  `business_metadata` rows on the linked business.
- **AC-P5-05** ⟵ UF-P5-4: The review page shows the linked business name; editing it to a new
  name relinks/creates accordingly; clearing it detaches (business_id → null).
- **AC-P5-06** ⟵ UF-P5-5 `[stretch]`: The web form accepts a business/venue name that links or
  creates a business on submit.
- **AC-P5-07** ⟵ UF-P5-6 `[stretch]`: An operator lookup returns all events for a given
  `business_id`.

**UF↔AC coverage:** UF-P5-1→AC-01/02; UF-P5-2→AC-03; UF-P5-3→AC-04; UF-P5-4→AC-05;
UF-P5-5→AC-06; UF-P5-6→AC-07. Every UF has ≥1 AC; every AC traces to a UF. (Stretch UFs 5/6 may
be dropped without affecting the core.)

## 15. VERSION HISTORY
- **v0.1 (2026-10-26):** Initial draft (prd-author). Gives the pre-existing `business` schema a
  feature home as **Epic 6 / P5**, after P1–P4. Scope limited to business↔event linking + dedupe
  + detail capture + review editing; business-owner accounts/auth and consumer venue pages are
  explicitly out (deferred to the auth work and P2 discovery). Not yet reviewed — pending the
  collaborative drill (prd-section-reviewer) and the independent spec-reviewer gate before sign-off.
