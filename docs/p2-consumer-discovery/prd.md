---
title: Arang — Consumer Discovery (P2)
type: PRD
artifact_id: ARANG-PRD-P2
parent: ARANG-INIT-001
status: approved
version: 1.1
driver: (you)
approver: (you)
approver_role: product owner / driver
assigned_date: 2026-10-26
first_draft_target:
last_revised_date: 2026-10-30
approved_date: 2026-10-30
review_provenance: independent spec-reviewer gate v1 (NEEDS-CHANGES, fixed in v0.5) → re-gate v2 PASS-WITH-FIXES; v0.7–v0.10 driver read-through fixes (weekend→window consistency, interest-vocabulary curation, returning-consumer recognition, React SPA + JSON API). Driver sign-off 2026-10-30.
jira:
multifeature: false
related_docs:
  - docs/prd.md (initiative overview — Epic 3 / P2)
  - docs/p1-producer-intake/prd.md (P1 — produces the published events + postal-centroid geo this feature reads)
  - docs/p1-producer-intake/sdd.md (schema: event, category, tag, event_category, event_tag, interest already exist)
  - docs/mockups/direction-c-interactive-map.html (committed map-first UX direction)
  - docs/mockups/direction-b-calendar.html (candidate secondary calendar view)
origin: Promoted from the P2 placeholder stub; carries its committed UX direction (map-first, postal-centroid pins, stated interests)
tags: [discovery, interests, consumer, map, weekend, category]
---

# Arang — Consumer Discovery (P2)

> **One coherent intention:** *let a consumer discover published events near them within a time
> window (default **this week**), filtered to their interests.* Reads the events P1 already captures/publishes; surfaces them on a
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
- **G1 — Time-window discovery:** a consumer gets published events for a chosen area within a
  datetime window, defaulting to **this week**.
  *Metric: a query for a location + `from`/`to` window returns the published events whose
  `starts_at` falls in that window; with no window given, the default is this week (Mon–Sun).*
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
- **UF-P2-5 (state interests)** `[core]` — As a consumer, I pick interests from the curated
  categories (and optionally finer tags/locations), and they're remembered.
- **UF-P2-6 (personalized default)** `[core]` — As a returning consumer, my saved interests
  pre-filter the default view. "Returning" = a stable identity signal, **not** a login: the
  **WhatsApp id (phone)** when I arrive via WhatsApp (reliable), or a **persistent browser cookie**
  on web (best-effort — same browser/device only; cleared cookies, a new device, or private mode
  look like a new consumer). True cross-device "same person" recognition needs accounts/auth, which
  is deferred (OQ-P2-4). This is recognition/identity, not authorization.

### Group C — Channel / secondary views
- **UF-P2-7 (WhatsApp ask)** `[stretch]` — As a consumer, I ask "what's on this weekend?" over
  WhatsApp and get a summarized list + a link to the map view. *(May defer to a later slice.)*
- **UF-P2-8 (calendar toggle)** `[stretch]` — As a consumer, I switch to a weekend calendar view
  (same data/filters, by time slot).

### Group D — Event categorization (P2-owned enabler for filtering)
> This is a first-class P2 build item, not a dependency. Current P1 enrichment emits only
> title/starts_at/location + free-text attributes; it does **not** assign categories, and no
> category vocabulary is seeded. P2 builds this: a **curated category spine** (fixed MVP) the LLM
> maps events to, plus **open tags** the LLM may mint (see Concepts + OQ-P2-6), so category
> filtering (UF-P2-2) has data and tags give the wider interest variety.
- **UF-P2-9 (auto-categorize at enrichment)** `[core]` — As the platform, when an event is
  ingested, the LLM maps it to one or more **curated categories** (the controlled spine, written
  to `event_category`) and may attach finer **open tags** (written to `event_tag`), so events are
  filterable by category and searchable by the wider tag variety.
- **UF-P2-10 (producer corrects category)** `[core]` — As a producer on the review page, I can
  see the assigned categories and add/remove them, so a miscategorized event can be fixed.

## 5. SCOPE
**In:** a consumer-facing discovery surface (map-first + synced list, category filters, event
detail); a `from`/`to` datetime-window query (default "this week") by location/category over
**published** events; stated-interest capture + applying them; postal-centroid pin placement
(reusing P1's geo); **event categorization — extending the existing LLM enrichment to assign
categories + seeding the category vocabulary (P2-owned, since P1 is already built/approved) and
review-page correction of categories.** A **React SPA (calling a JSON API)** is the primary P2 channel.
**Out (later/other phases):** conversational multi-turn producer intake (Epic 2, own PRD);
learned/ML personalization (MVP is stated interests only); precise geocoding; consumer accounts &
auth (rides on the platform auth work; MVP identifies a consumer lightly, e.g. by WhatsApp id or a
session); ticketing/RSVP transactions; business/venue pages (depends on P5 linking).

## 6. SERVICE BOUNDARY
P2 adds a read/query service over the existing `event` data plus an interests store; it reuses the
P1 schema and the `PostalGeoService`. New: a `DiscoveryService` (window/area/category query) and
an `InterestService`, both exposed as **JSON endpoints** that the React SPA calls. No change to
how events are produced.

## 7. VALIDATIONS ON INCOMING DATA (consumer inputs)
| # | Rule | When | Outcome |
|---|---|---|---|
| V1 | Only `status=published` events are discoverable | on-query | exclude drafts |
| V2 | Discovery filters events by a `from`/`to` datetime window on `starts_at`; the default preset is "this week" (Mon–Sun local, configurable) | on-query | filter by starts_at range |
| V3 | Location query resolves to an area: match events by same `postal_code`, or (when a query centroid is known) within a configurable radius (default 15 miles) of it. How a free-text location resolves to a centroid is an SDD detail; an unresolved location falls to V7 | on-query | area filter |
| V4 | A published event with a postal code but **no derived centroid** (unknown ZIP, null lat/long per P1 DD-3) is listed but not pinned ("location approximate/unknown") | on-render | list-only |
| V5 | Interest values must map to known categories/tags | on-save | reject unknown |
| V6 | Malformed window where `from` > `to` | on-query | reject with an error (do not silently swap) |
| V7 | Location string that resolves to no area | on-query | empty result set (not an error), honestly labeled |

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
Backend stack unchanged (Java/Spring Boot, MySQL, JdbcTemplate). **Front end: a React SPA calling
a JSON API** (OQ-P2-5) — a step up from P1's server-rendered Thymeleaf. Map via `react-leaflet` +
OpenStreetMap (free, no key); Google Maps link for directions. Approximate placement only (postal
centroid). Stated interests only (no ML).

## 10. CONCEPTS
- **Discovery window:** a general `from`/`to` datetime range the query filters `starts_at` on.
  The default preset is **"this week"** (Monday 00:00 → Sunday 23:59). `starts_at` is stored as a
  naive local `DATETIME` (P1 schema — no timezone), so "this week" is computed in a single
  configured **app timezone** for the prototype; true per-consumer timezones are deferred. Other
  presets (this weekend / tonight / today / next 7 days / custom) use the same engine.
- **Area query:** a location (postal/locality) → the set of events whose centroid is in/near it.
- **Category (curated spine):** a **controlled, curated** vocabulary — a broad, high-quality set
  (~15–25 for the MVP, exact list finalized at SDD/build) that drives the filter pills and the
  interest-capture surface. **Fixed for the MVP** (changes are a deliberate, versioned edit, not
  user-minted). The LLM maps each event to one or more of these during enrichment.
- **Tag (open long tail):** finer, free-form labels (e.g. jazz, vegan, 5k, pottery, free,
  dog-friendly) that give the **wide variety** of interests without bloating the filter UI.
  **Growable** — the LLM may mint tags during enrichment; they are not a fixed list.
- **Interest:** a consumer's stated preference in the `interest` table. Consumers set interests
  primarily on **categories** (the curated spine); tags are optional finer interests. The
  `weight` column exists but is not used in the MVP (interests filter, not rank).
- **Consumer:** a `user` discovering events; may have interests. Identity is **light recognition,
  not authentication/authorization** (OQ-P2-4).
  - **Recognition (how we know a returning consumer):** the **WhatsApp id (phone)** for WhatsApp
    arrivals — stable and reliable; or a **persistent browser cookie** holding a generated id on
    web — best-effort, bound to that browser/device. Cleared cookies, a new device, or private mode
    present as a new consumer. Cross-device recognition of the same human needs accounts/auth
    (deferred).
  - **Identity↔storage (resolves the `interest.user_id` NOT NULL constraint):** a consumer who
    saves interests gets a lightweight `user` row — the existing P1 `user` matched by phone for
    WhatsApp arrivals, or a new row keyed to the web cookie id — and interests FK to it. A
    schema-touching decision, recorded here (not left to the SDD).

## 11. ASSUMPTIONS
- **A1:** Events are already published with usable `postal_code` (P1 enforces postal to publish).
- **A2:** The MVP recognizes a consumer lightly (WhatsApp phone, or a persistent web cookie), not
  via full auth; web recognition is best-effort and browser/device-bound.
- **A3:** Categories are a small controlled set seeded for the MVP.

## 12. OPEN QUESTIONS
| # | Question | Status | Note / default | Owner |
|---|---|---|---|---|
| OQ-P2-1 | Primary channel: web map view, WhatsApp, or both for the MVP? | RESOLVED (2026-10-26) | **Web map view is primary** for P2; a WhatsApp "what's on?" reply (summary + link) stays `[stretch]` (UF-P2-7) | driver |
| OQ-P2-2 | Discovery time filter: fixed "weekend" or general window? | RESOLVED (2026-10-26) | **General `from`/`to` datetime window** is the engine (`starts_at` in range). **Default preset = "this week"** (Monday 00:00 → Sunday 23:59, local, configurable). Other presets (this weekend / tonight / today / next 7 days / custom) are `[stretch]`, same engine | driver |
| OQ-P2-3 | How do events get categorized? (P1 doesn't set event_type/category) | RESOLVED (2026-10-26) | **P2 extends the existing LLM enrichment to assign one or more categories** (writes `event_category`) + the producer can **correct on the review page**. Owned by P2 (P1 is already built/approved — not reopened). Uncategorized events still appear, not category-filtered. Dependency: category filtering needs LLM enrichment enabled | driver |
| OQ-P2-4 | Consumer identity for the MVP + how a "returning" consumer is recognized | RESOLVED (2026-10-30) | **Light recognition, not auth.** WhatsApp arrivals → recognized by **phone (WhatsApp id)**, mapped to the P1 `user` (reliable). Web → a **persistent cookie** holding a generated id, keyed to a lightweight `user` row (best-effort: same browser/device only; cleared cookies / new device / private mode = new consumer). Interests FK to whichever `user` row applies. Cross-device "same person" and real authn/authz stay deferred (platform-wide) | driver |
| OQ-P2-5 | Front-end architecture + map rendering tech | RESOLVED (2026-10-30) | **React SPA + JSON API** (a step up from P1's server-rendered Thymeleaf, chosen for scalability + component reuse + the open-source ecosystem). Map via **`react-leaflet` + OpenStreetMap tiles** (free, no API key); a **Google Maps link** on each event detail for directions (free). Exact bundler/UI-kit/library versions deferred to the SDD. Implication: the backend must expose JSON discovery/interest endpoints (not Thymeleaf pages) for P2 | driver |
| OQ-P2-6 | How are interests/categories curated, and how wide is the set? | RESOLVED (2026-10-30) | **Two-level vocabulary.** `category` = a **curated controlled spine** (~15–25, broad high-quality set, **fixed for the MVP**) that drives filter pills + interest capture; the LLM maps each event to one or more. `tag` = **open, growable** finer labels the LLM may mint during enrichment — the "wide variety". Consumers set interests primarily on categories; tags are optional. **The exact seed category list is finalized at the SDD/build stage** | driver |

## 13. PRE-SIGN-OFF VERIFICATION
- [x] One coherent intention (consumer discovery) — conversational producer intake kept OUT.
- [x] Every UF has ≥1 AC; every AC traces to a UF (categorization enabler now has UF-P2-9/10 + AC-10/11).
- [x] The "approximate placement" counter-metric is covered (V4/AC-P2-05), fixed to no-centroid.
- [x] The categorization approach (OQ-P2-3) is resolved + scoped as first-class P2 work (Group D).
- [x] The discovery time filter (OQ-P2-2) is resolved: general window, default "this week", app-tz.
- [x] Consumer-identity (OQ-P2-4) made + the interest.user_id FK gap resolved (lightweight user row).
- [x] Area-match rule (same postal or radius) and list↔map sync are stated testably (AC-01/03).
- [x] Scope defers ML personalization, precise geocoding, accounts/auth, ticketing, venue pages.

## 14. ACCEPTANCE CRITERIA
- **AC-P2-01** ⟵ UF-P2-1: A location query returns only `published` events whose `starts_at` falls
  in the requested `from`/`to` window, **for the area** — area match = same `postal_code` as the
  query, OR (when the query resolves to a centroid) events whose centroid is within a configurable
  radius (default 25 km). With no window given, it defaults to this week (Mon 00:00 – Sun 23:59).
- **AC-P2-02** ⟵ UF-P2-2: Selecting one or more categories narrows results (map + list) to events
  whose `event_category` includes any selected category (OR semantics).
- **AC-P2-03** ⟵ UF-P2-3: Selecting a list item marks its map pin as selected (an observable
  selected state) and centers it; selecting a pin marks its list row as selected. The map/list
  rendering mechanism is an SDD choice (OQ-P2-5); this AC is stated at the behavior level only.
- **AC-P2-04** ⟵ UF-P2-4: Opening an event shows title, time, location, any link metadata, and the
  linked business when present.
- **AC-P2-05** ⟵ UF-P2-1 (counter): placement is labeled approximate; an event with a postal code
  but **no derived centroid** (unknown ZIP — P1 publishes these with null lat/long per P1 DD-3)
  appears in the list but is **not pinned**. (A published event always has a postal code — P1
  requires it to publish — so the real "not pinned" case is a missing centroid, not a missing postal.)
- **AC-P2-06** ⟵ UF-P2-5: A consumer can save interests (categories/locations) and they persist in
  the `interest` table, keyed to the consumer's recognized identity (WhatsApp phone, or a web cookie
  id — see OQ-P2-4).
- **AC-P2-07** ⟵ UF-P2-6: A recognized returning consumer's saved interests pre-filter the default
  results using OR across saved categories; "recognized" = a matching WhatsApp phone or web cookie
  id (best-effort; an unrecognized consumer sees the unfiltered default). The interest `weight`
  column is **not** used in the MVP (filter, not rank); if interests yield zero results in the
  window, the view shows all in-window **and in-area** results with a note that the interest filter
  matched nothing.
- **AC-P2-08** ⟵ UF-P2-7 `[stretch]`: A WhatsApp "what's on this weekend?" returns a summarized
  list + a link to the map view.
- **AC-P2-09** ⟵ UF-P2-8 `[stretch]`: A calendar toggle shows the same filtered results by time slot.
- **AC-P2-10** ⟵ UF-P2-9: When an event is enriched, the LLM maps it to one or more curated
  categories (each written as an `event_category` row) and may attach open tags (each written as
  an `event_tag` row). A category value must be one of the curated set; a tag may be new.
- **AC-P2-11** ⟵ UF-P2-10: On the review page, a producer can see the event's categories and
  add/remove them; the change persists to `event_category`.

**UF↔AC coverage:** UF-P2-1→AC-01/05; UF-P2-2→AC-02; UF-P2-3→AC-03; UF-P2-4→AC-04; UF-P2-5→AC-06;
UF-P2-6→AC-07; UF-P2-7→AC-08; UF-P2-8→AC-09; UF-P2-9→AC-10; UF-P2-10→AC-11. Every UF has ≥1 AC;
every AC traces to a UF (including the categorization enabler, Group D).

## 15. VERSION HISTORY
- **v1.1 (2026-10-30):** V3/AC-P2-01 radius default changed from 25 km to **15 miles** (driver
  decision during the P2 SDD gate, for a clearer US-prototype unit). Scope and the area-match rule
  are unchanged; approved baseline stands.
- **v1.0 (2026-10-30) — APPROVED.** Driver sign-off. All UFs/ACs traced, all five open questions
  resolved, two independent gate rounds (NEEDS-CHANGES → PASS-WITH-FIXES) plus driver read-through
  fixes (v0.7–v0.10). Approved baseline; the TRD is authored next.
- **v0.10 (2026-10-30):** Resolved OQ-P2-5 as **React SPA + JSON API** (driver choice, for
  scalability + component reuse + open-source ecosystem) instead of P1's Thymeleaf. Map via
  `react-leaflet` + OpenStreetMap (free, no key); Google Maps link on event detail. Backend must
  expose JSON discovery/interest endpoints. Updated Scope, Service Boundary, Constraints, OQ-P2-5.
  Bundler/UI-kit/library versions deferred to the SDD. Known cost: Node 20+ needed for React
  tooling (this machine defaults to Node 16 — a prebuilt Node 20 was used before).
- **v0.9 (2026-10-30):** Clarified **how a "returning" consumer is recognized** (gap the driver
  caught in UF-P2-6). This is **recognition/identity, not authorization**. WhatsApp arrivals →
  recognized by phone (reliable, mapped to the P1 `user`); web → a **persistent cookie** id keyed
  to a lightweight `user` row (best-effort, same-browser/device only — cleared cookies / new device
  / private mode = new consumer). Cross-device recognition + real authn/authz stay deferred.
  Updated UF-P2-6, the Consumer concept, OQ-P2-4, A2, and AC-P2-06/07. No scope change (still no
  auth in P2).
- **v0.8 (2026-10-30):** Specified interest/category **curation** (gap the driver caught — the PRD
  said "seed the vocabulary" without defining it). Resolved as **OQ-P2-6**: a two-level vocabulary —
  `category` = curated controlled spine (~15–25, **fixed MVP**, drives filters + interest capture);
  `tag` = **open/growable** LLM-minted long tail for the wide variety. Exact seed category list
  finalized at SDD. Updated Concepts, UF-P2-5, UF-P2-9, AC-P2-10 (now covers categories + tags),
  and the Group D note. No change to the core intention or scope boundaries.
- **v0.7 (2026-10-30):** Consistency fix (driver caught it): **G1** still said "weekend discovery"
  while OQ-P2-2 had generalized the engine to a `from`/`to` datetime window (default this week).
  Updated G1 + its metric, the one-line intention, and the service-boundary `DiscoveryService`
  description to the time-window framing. Kept "weekend" only where it is a legitimate named preset
  or stretch flow (UF-P2-7/8, OQ-P2-2 history). No scope or AC change.
- **v0.6 (2026-10-26) [also relocated]:** Moved from the old flat path `docs/prd/consumer-discovery.md`
  to its own feature folder `docs/p2-consumer-discovery/prd.md`, matching the one-folder-per-feature
  convention (so the forthcoming TRD/SDD/TTP are siblings). Initiative references updated; P3 moved
  likewise to `docs/p3-ingestion-framework/`. Independent `spec-reviewer` **re-gate → PASS-WITH-FIXES**: all prior
  CRITICAL/HIGH/MEDIUM findings verified genuinely resolved (checked against the live schema), no
  new blocking findings, 100% UF↔AC coverage. Cleaned 2 LOW nits — reordered ACs so stretch
  (08/09) precede Group D (10/11); noted query-location→centroid resolution as an SDD detail (V3);
  clarified the empty-interest fallback stays in-window **and** in-area (AC-P2-07). Two LOW items
  (N3 area/interest edge, N4 enrichment-disabled behavior) carried to the SDD. **Ready for driver sign-off.**
- **v0.5 (2026-10-26):** Applied the independent `spec-reviewer` gate findings (verdict was
  NEEDS-CHANGES). **CRITICAL/HIGH fixed:** (C1/E1) categorization promoted to first-class scope
  with its own flows UF-P2-9/10 + AC-P2-10/11; (B1) area-match rule defined (same postal or
  radius, default 25 km) in AC-P2-01/V3; (B2) AC-P2-03 reworded to observable selected-state sync,
  tech left to SDD; (H1) counter-metric fixed — a published event always has a postal code, so the
  "not pinned" case is a missing **centroid** (unknown ZIP, P1 DD-3), not a missing postal
  (AC-P2-05/V4). **MEDIUM fixed:** (D2) interest.user_id NOT NULL reconciled with anonymous
  identity via a lightweight user row; (B3) "this week" computed in a configured app timezone over
  naive `DATETIME`; (D1) interest combination = OR, weight unused in MVP, empty-result behavior
  defined (AC-P2-07); (G1) added V6 (from>to reject) + V7 (unresolved locality → empty); (C2)
  initiative Epic 3 noted P2 owns categorization. Verdict items addressed; ready to re-gate / sign off.
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
