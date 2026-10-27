---
title: Arang — Consumer Discovery (P2)
type: PRD
artifact_id: ARANG-PRD-P2
parent: ARANG-INIT-001
status: draft
version: 0.4
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
  - docs/prd.md (initiative overview — Epic 3 / P2)
  - docs/producer-intake/prd.md (P1 — produces the published events + postal-centroid geo this feature reads)
  - docs/producer-intake/sdd.md (schema: event, category, tag, event_category, event_tag, interest already exist)
  - docs/mockups/direction-c-interactive-map.html (committed map-first UX direction)
  - docs/mockups/direction-b-calendar.html (candidate secondary calendar view)
origin: Promoted from the P2 placeholder stub; carries its committed UX direction (map-first, postal-centroid pins, stated interests)
tags: [discovery, interests, consumer, map, weekend, category]
---

# Arang — Consumer Discovery (P2)

> **One coherent intention:** *let a consumer discover this weekend's published events near them,
> filtered to their interests.* Reads the events P1 already captures/publishes; surfaces them on a
> **map-first** view with category/location filters. Interests are **explicitly stated** (not
> learned) for the MVP. **Conversational multi-turn intake is OUT** — that is a producer-side
> capability (initiative Epic 2) and gets its own PRD; mixing it here would be two intentions.

## 1. CONTEXT
P1 fills the datastore with **published** events that carry typed-core fields (title, start/end,
location, `postal_code` → approximate `latitude`/`longitude` via postal centroid) and a kind
(`event_type`). Nothing yet lets a **consumer** read them. P2 is the consumer side: choose a
location and a time window (default **this week**), filter by category, and see events on a lively
map + list. The schema already has the discovery vocabulary (`category`, `tag`, `event_category`,
`event_tag`) and `interest` — unused until now.

## 2. PROBLEM STATEMENT
Captured events are invisible to the people they're for. A consumer has no way to find "weekend
events near me that match what I like." Without discovery, the producer side has no payoff and the
data has no consumer value.

## 3. GOALS & SUCCESS METRICS
- **G1 — Weekend discovery:** a consumer gets this weekend's published events for a chosen area.
  *Metric: a query for a location returns the published events whose start falls in the weekend window.*
- **G2 — Interest/category filtering:** results narrow to selected categories.
  *Metric: selecting a category filters both the map and the list to matching events.*
- **G3 — Map-first presentation:** events appear as pins (postal-centroid) with a synced list.
  *Metric: each result with a postal code shows a pin at its centroid; list and map stay in sync.*
- **G4 — Stated interests personalize:** a consumer can tell Arang what they like and see it applied.
  *Metric: stored interests pre-filter the default results.*
- **Counter-metric:** never imply precise geocoding. *Metric: placement is labeled "approximate."*

## 4. USER STORIES / USE CASES
Tag legend: `[core]` P2 must-have · `[stretch]` if cheap.

### Group A — Discovery
- **UF-P2-1 (events near a location, by time window)** `[core]` — As a consumer, I choose a
  location and see published events for that area in a time window, defaulting to **this week**.
- **UF-P2-2 (filter by category)** `[core]` — As a consumer, I tap category filters that narrow
  both the map and the list.
- **UF-P2-3 (map + list, synced)** `[core]` — As a consumer, I see events as map pins and a
  scrollable list, synced (hover/click highlights the other), with click-to-detail.
- **UF-P2-4 (event detail)** `[core]` — As a consumer, I open an event to see its title, time,
  location, links, and the business/organizer when present.

### Group B — Interests
- **UF-P2-5 (state interests)** `[core]` — As a consumer, I tell Arang the categories/locations I
  care about and they're remembered.
- **UF-P2-6 (personalized default)** `[core]` — As a returning consumer, my stated interests
  pre-filter the default view.

### Group C — Channel / secondary views
- **UF-P2-7 (WhatsApp ask)** `[stretch]` — As a consumer, I ask "what's on this weekend?" over
  WhatsApp and get a summarized list + a link to the map view. *(May defer to a later slice.)*
- **UF-P2-8 (calendar toggle)** `[stretch]` — As a consumer, I switch to a weekend calendar view
  (same data/filters, by time slot).

## 5. SCOPE
**In:** a consumer-facing discovery surface (map-first + synced list, category filters, event
detail); a `from`/`to` datetime-window query (default "this week") by location/category over
**published** events; stated-interest capture + applying them; postal-centroid pin placement
(reusing P1's geo); **event categorization — extending the existing LLM enrichment to assign
categories + seeding the category vocabulary (P2-owned, since P1 is already built/approved) and
review-page correction of categories.** A minimal web view is the primary P2 channel.
**Out (later/other phases):** conversational multi-turn producer intake (Epic 2, own PRD);
learned/ML personalization (MVP is stated interests only); precise geocoding; consumer accounts &
auth (rides on the platform auth work; MVP identifies a consumer lightly, e.g. by WhatsApp id or a
session); ticketing/RSVP transactions; business/venue pages (depends on P5 linking).

## 6. SERVICE BOUNDARY
P2 adds a read/query service over the existing `event` data plus an interests store; it reuses the
P1 schema and the `PostalGeoService`. New: a `DiscoveryService` (weekend/area/category query) and
an `InterestService`. No change to how events are produced.

## 7. VALIDATIONS ON INCOMING DATA (consumer inputs)
| # | Rule | When | Outcome |
|---|---|---|---|
| V1 | Only `status=published` events are discoverable | on-query | exclude drafts |
| V2 | Discovery filters events by a `from`/`to` datetime window on `starts_at`; the default preset is "this week" (Mon–Sun local, configurable) | on-query | filter by starts_at range |
| V3 | Location query resolves to an area (postal/locality); events matched by postal/centroid proximity | on-query | area filter |
| V4 | Events with no postal code are listed but not pinned (honestly "location approximate/unknown") | on-render | list-only |
| V5 | Interest values must map to known categories/tags | on-save | reject unknown |

## 8. DEPENDENCIES & INTEGRATIONS
- P1 published events + their postal-centroid `latitude`/`longitude`.
- The `category`/`tag`/`event_category`/`event_tag` vocabulary and `interest` table (schema exists).
  **Categorization (resolved, OQ-P2-3):** the LLM assigns categories, and the producer can correct
  them on the review page; P2 reads `event_category`. **Scope note:** P1 is already built and its
  PRD approved, so this categorization work is **owned by P2** — P2 extends the existing enrichment
  path (and seeds the category vocabulary), rather than reopening the P1 spec. Dependency: category
  filtering needs LLM enrichment enabled (uncategorized events still appear, unfiltered).
- A map rendering approach for the web view (library TBD at SDD time).

## 9. CONSTRAINTS
Stack unchanged (Java/Spring Boot, MySQL, JdbcTemplate, Thymeleaf or a minimal JSON+JS map view —
decide at SDD). Approximate placement only (postal centroid). Stated interests only (no ML).

## 10. CONCEPTS
- **Discovery window:** a general `from`/`to` datetime range the query filters `starts_at` on.
  The default preset is **"this week"** (Monday 00:00 → Sunday 23:59, local, configurable); other
  presets (this weekend / tonight / today / next 7 days / custom) use the same engine.
- **Area query:** a location (postal/locality) → the set of events whose centroid is in/near it.
- **Category/tag:** the shared controlled vocabulary used to classify events and express interests.
  Categories are assigned by the LLM during P1 enrichment (correctable on the review page).
- **Interest:** a consumer's stated preference (category/tag/location, weighted) in the `interest` table.
- **Consumer:** a `user` in the `consumer` role (role model from the initiative; auth deferred).

## 11. ASSUMPTIONS
- **A1:** Events are already published with usable `postal_code` (P1 enforces postal to publish).
- **A2:** The MVP consumer is identified lightly (WhatsApp id / session), not via full auth.
- **A3:** Categories are a small controlled set seeded for the MVP.

## 12. OPEN QUESTIONS
| # | Question | Status | Note / default | Owner |
|---|---|---|---|---|
| OQ-P2-1 | Primary channel: web map view, WhatsApp, or both for the MVP? | RESOLVED (2026-10-26) | **Web map view is primary** for P2; a WhatsApp "what's on?" reply (summary + link) stays `[stretch]` (UF-P2-7) | driver |
| OQ-P2-2 | Discovery time filter: fixed "weekend" or general window? | RESOLVED (2026-10-26) | **General `from`/`to` datetime window** is the engine (`starts_at` in range). **Default preset = "this week"** (Monday 00:00 → Sunday 23:59, local, configurable). Other presets (this weekend / tonight / today / next 7 days / custom) are `[stretch]`, same engine | driver |
| OQ-P2-3 | How do events get categorized? (P1 doesn't set event_type/category) | RESOLVED (2026-10-26) | **P2 extends the existing LLM enrichment to assign one or more categories** (writes `event_category`) + the producer can **correct on the review page**. Owned by P2 (P1 is already built/approved — not reopened). Uncategorized events still appear, not category-filtered. Dependency: category filtering needs LLM enrichment enabled | driver |
| OQ-P2-4 | Consumer identity for the MVP (WhatsApp id vs. anonymous session vs. wait for auth) | RESOLVED (2026-10-26) | **Light identity, no auth:** a WhatsApp id when the consumer arrives via WhatsApp, else an anonymous browser session. Interests attach to whichever id is present. Full authn/authz stays deferred (platform-wide) | driver |
| OQ-P2-5 | Map rendering tech (Leaflet/MapLibre/static) | DEFERRED → SDD | Implementation choice, not a product decision; does not block PRD sign-off. Decide when the SDD is authored | driver |

## 13. PRE-SIGN-OFF VERIFICATION
- [ ] One coherent intention (consumer discovery) — conversational producer intake kept OUT.
- [ ] Every UF has ≥1 AC; every AC traces to a UF.
- [ ] The "approximate placement" counter-metric is covered (V4/AC).
- [x] The categorization approach (OQ-P2-3) is resolved: LLM at enrichment + review correction.
- [x] The discovery time filter (OQ-P2-2) is resolved: general window, default "this week".
- [x] Consumer-identity decision (OQ-P2-4) made: light identity (WhatsApp id / session), no auth.
- [ ] Scope defers ML personalization, precise geocoding, accounts/auth, ticketing, venue pages.

## 14. ACCEPTANCE CRITERIA
- **AC-P2-01** ⟵ UF-P2-1: A location query returns only `published` events whose `starts_at` falls
  in the requested `from`/`to` window for that area; with no window given, it defaults to this week
  (Mon–Sun local).
- **AC-P2-02** ⟵ UF-P2-2: Selecting one or more categories narrows results (map + list) to events
  in those categories.
- **AC-P2-03** ⟵ UF-P2-3: Each result with a postal code renders a pin at its centroid; hovering/
  clicking a list item highlights its pin and vice-versa.
- **AC-P2-04** ⟵ UF-P2-4: Opening an event shows title, time, location, any link metadata, and the
  linked business when present.
- **AC-P2-05** ⟵ UF-P2-1 (counter): placement is labeled approximate; an event without a postal
  code appears in the list but is not pinned.
- **AC-P2-06** ⟵ UF-P2-5: A consumer can save interests (categories/locations) and they persist in
  the `interest` table.
- **AC-P2-07** ⟵ UF-P2-6: A returning consumer's saved interests pre-filter the default results.
- **AC-P2-08** ⟵ UF-P2-7 `[stretch]`: A WhatsApp "what's on this weekend?" returns a summarized
  list + a link to the map view.
- **AC-P2-09** ⟵ UF-P2-8 `[stretch]`: A calendar toggle shows the same filtered results by time slot.

**UF↔AC coverage:** UF-P2-1→AC-01/05; UF-P2-2→AC-02; UF-P2-3→AC-03; UF-P2-4→AC-04; UF-P2-5→AC-06;
UF-P2-6→AC-07; UF-P2-7→AC-08; UF-P2-8→AC-09. Every UF has ≥1 AC; every AC traces to a UF.

## 15. VERSION HISTORY
- **v0.4 (2026-10-26):** Scope clarification (driver): since P1 is already built and its PRD
  approved, the event-categorization work (extending the LLM enrichment to assign categories +
  seeding the vocabulary + review-page correction) is **owned by P2**, not a retroactive P1
  change. Updated Scope (In), Dependencies, and the OQ-P2-3 row to state this boundary. No change
  to user flows or acceptance criteria.
- **v0.3 (2026-10-26):** Resolved the last two open questions. **OQ-P2-4 (consumer identity):**
  light identity — WhatsApp id or anonymous session, no auth; interests attach to whichever id is
  present. **OQ-P2-5 (map tech):** deferred to the SDD as an implementation choice. All
  product-level open questions are now resolved; the PRD is ready for the review gate and sign-off.
- **v0.2 (2026-10-26):** Resolved OQ-P2-1/2/3 with the driver. **Channel:** web map primary
  (WhatsApp ask stretch). **Time filter:** generalized from a fixed "weekend" to a `from`/`to`
  datetime window engine, with **default preset "this week" (Mon–Sun, local, configurable)**;
  other presets are stretch. **Categorization:** LLM assigns categories during P1 enrichment,
  producer-correctable on the review page (adds a dependency: category filtering needs LLM
  enrichment on). Updated context, UF-P2-1, V2, concepts, dependencies, AC-P2-01, and the
  pre-sign-off checklist accordingly. OQ-P2-4 (consumer identity) and OQ-P2-5 (map tech) remain
  open for the SDD.
- **v0.1 (2026-10-26):** Promoted from placeholder to a full draft PRD (prd-author). Scoped to
  **consumer discovery only** (map-first + list, category filters, detail, stated interests,
  weekend/area query); carried over the committed map-first / postal-centroid UX direction from the
  stub. **Conversational producer intake explicitly excluded** (own PRD, initiative Epic 2) to keep
  one intention. Flagged the key dependency gap in OQ-P2-3: P1 does not categorize events, so
  discovery filtering needs a categorization step decided before the TRD. Not yet reviewed —
  pending the collaborative drill + the independent spec-reviewer gate before sign-off.
