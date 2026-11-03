---
title: Arang — Town Entry & Coverage (P2.1)
type: PRD
artifact_id: ARANG-PRD-P2-1
parent: ARANG-INIT-001
status: approved
version: 1.5
driver: (you)
approver: (you)
approver_role: product owner / driver
assigned_date: 2026-10-31
first_draft_target: 2026-10-31
last_revised_date: 2026-11-01
approved_date: 2026-11-01
review_provenance: v1.0 approved by the driver 2026-10-31 without a review gate (approved_date refers to v1.0 only). v1.1–v1.4 are DRAFT additions. Independent spec-reviewer gate run 2026-11-01 on PRD v1.3 + TRD v0.4 + SDD v0.3 → NEEDS-CHANGES (7 HIGH); v1.4 applied the fixes. Second gate on PRD v1.4 / TRD v0.5 / SDD v0.4 → PASS-WITH-FIXES (1 new HIGH); v1.5 applies those fixes..
jira:
multifeature: false
related_docs:
  - ~/Downloads/Arangly_Enhancement_Checklist.md (2026-11-01 site review — source of the v1.1 additions; items outside P2.1 went to docs/p2.2-event-detail-trust/prd.md)
  - docs/prd.md (initiative overview — Epic 3 / P2; this is a follow-on to P2, phase tag P2.1)
  - docs/p2-consumer-discovery/prd.md (P2 — approved + built; this PRD extends its "choose a location" entry point, UF-P2-1)
  - docs/p2-consumer-discovery/sdd.md (DD-P2-3 deferred real town geocoding; DD-P2-9 two-screen landing flow; DD-P2-11 "any town, any size" positioning — all three are inputs here)
  - docs/p1-producer-intake/sdd.md (schema: user, user_metadata, event — reused, not changed, unless OQ-P2.1-1 decides otherwise)
  - docs/p2.1-town-entry/ui-reference/landing-v3.html, landing-v3-typing.html, coming-soon.html, maintenance.html (the design source for the landing states; copied from /tmp/arang-mockups on 2026-11-01 so the reference is durable)
origin: Promoted from the "next steps" analysis after P2 shipped. The landing UI (v3, coming-soon) was prototyped in the SPA BEFORE this spec existed; this PRD retroactively specifies it and the backend it needs.
tags: [landing, town-search, geocoding, waitlist, demand-capture, seo, sitemap, consumer]
---

# Arang — Town Entry & Coverage (P2.1)

> **One coherent intention:** *let a visitor get from "arrived at Arang" to "looking at events in
> my own town" — and, when Arang is not live in their town yet, turn that dead end into a captured,
> honest signal of demand.* This covers the landing screen's town entry (search, near-me, direct
> links), the definition of which towns are "covered", the "coming soon" demand capture, and making
> town pages discoverable by search engines. It does **not** change how events are queried, filtered,
> or displayed once a town is chosen (that is P2, already built).

## 1. CONTEXT
P2 shipped a two-screen consumer flow (DD-P2-9): a landing screen whose one job is "choose your
town", then a discovery screen (list + map). That landing is currently backed by a **hard-coded list
of 28 seeded Massachusetts towns**. A visitor who types any other town (e.g. "Worcester") has no real
path: the product voice promises "any town, any size" (DD-P2-11), but the system can only resolve
the seeded towns (DD-P2-3 explicitly deferred real town lookup). Separately:

- A town has **no URL of its own** — the chosen town lives only in browser memory, so a town cannot
  be linked, bookmarked, shared, or indexed by a search engine.
- There is **no place to record demand** from visitors in towns Arang does not cover yet, so that
  signal is lost.
- A new landing design (v3 with a near-me button and type-ahead; a coming-soon state with email
  capture; a maintenance page) was built in the SPA ahead of any spec. Its email capture is not
  backed by a server endpoint — it currently always reports a failure to save (deliberately, so it
  never claims a save that did not happen).

## 2. PROBLEM STATEMENT
Visitors outside the seeded towns hit a dead end, and Arang learns nothing from them. Visitors inside
a covered town cannot share a link to "my town's events". And the search-engine surface is a single
URL, so organic discovery — a main way "your own town" users would find a hyper-local product —
cannot work. The marketing promise ("any town, any size") exceeds what the system can resolve.

## 3. GOALS & SUCCESS METRICS
- **G1 — Find any in-region town fast:** a visitor can reach their town by typing the start of a town name or
  a ZIP, or by tapping near-me, in a few seconds.
  *Metric: for any place in the supported region (see Concepts), typing its name or ZIP yields a
  matching suggestion; selecting it opens that town's events (or, for a town Arangly does not serve yet, its coming-soon state).*
- **G2 — Honest coverage:** the product states truthfully whether Arang is live in the chosen town.
  *Metric: a town is shown as "live" only if it meets the coverage definition (OQ-P2.1-2); every
  other resolvable town shows "coming soon". No town is ever shown as live without events being
  queryable for it, and none is shown as coming-soon while live.*
- **G3 — Capture demand:** a visitor in an uncovered town can leave an email and it is durably saved
  against that town.
  *Metric: submitting a valid email for an uncovered town creates a persisted record linking the
  email to that town; the visitor sees a "saved" confirmation only after the save is confirmed.*
- **G4 — Shareable town pages:** every live town has a stable, human-readable URL that opens that
  town directly.
  *Metric: loading a town URL in a fresh browser shows that town's events without visiting the
  landing screen first.*
- **G5 — Crawlable surface:** search engines can discover live-town pages and are told to stay out
  of write/admin routes.
  *Metric: the sitemap lists exactly the live-town URLs and the landing page; it lists none of the
  excluded routes (add, submit, console, review, API, webhook).*
- **Counter-metrics (must not regress):**
  - *Never claim a save that did not happen.* Metric: the "you're on the list" state appears only
    after the server confirms persistence; any failure shows an error and keeps the form.
  - *Never imply precision we lack.* P2's "approximate placement" rule (AC-P2-05) is unchanged.
  - *Do not leak private data through the new surface.* Metric: the waitlist endpoint never returns
    another person's email, and no stored email appears in the sitemap, logs at INFO, or URLs.

## 4. USER STORIES / USE CASES
Tag legend: `[core]` P2.1 must-have · `[stretch]` if cheap.

### Group A — Reaching a town
- **UF-P2.1-1 (search by town name or ZIP)** `[core]` — As a visitor, I type part of a town name or
  a ZIP code and see matching places (with their ZIP and state), pick one, and land on that town.
- **UF-P2.1-2 (near me)** `[core]` — As a visitor, I tap "Find events near me", allow location, and
  am taken to the nearest live town; if none is near, I am told so and offered the search instead.
- **UF-P2.1-3 (open a town directly)** `[core]` — As a visitor with a link or bookmark to a town, I
  open it and see that town's events immediately, and I can still change town from there.

### Group B — When Arang is not there yet
- **UF-P2.1-4 (coming soon + notify me)** `[core]` — As a visitor whose town is real but not live,
  I am told Arang is coming soon to it and can leave my email to be told when it is live.
- **UF-P2.1-5 (live town, nothing on)** `[core]` — As a visitor in a live town with no events in the
  window, I see an honest "nothing on" state — distinct from "coming soon" — with nearby live towns
  that do have events and a way to submit an event, so it is not a dead end. *(v1.1: nearby towns and
  submit option added from the site review.)*

### Group A2 — Coming back
- **UF-P2.1-7 (remember my town)** `[core]` — As a returning visitor, my last chosen town is remembered
  on this browser, so I do not have to choose it again, and I can still switch town. No account is
  needed. *(v1.1, from the site review.)*

### Group C — Being found
- **UF-P2.1-6 (search engine discovery)** `[core]` — As a search engine crawler, I can read a sitemap
  of live-town pages and a robots policy that excludes write/admin routes.

*Coverage: every UF has ≥1 AC in §14; every AC traces to a UF (v1.4: 20 ACs, 7 UFs). Deferred items (sending
notification emails, sharing, accounts) appear only in §5 "Out of scope".*

## 5. SCOPE
**In scope (P2.1):**
- Town/ZIP search with suggestions over the supported region — the Massachusetts municipalities (UF-P2.1-1).
  *(v1.4)* Villages and neighborhoods (for example Saxonville, Newton Centre) are **not** searchable in P2.1;
  they are a later phase.
- Near-me → nearest live town, with a defined distance limit (UF-P2.1-2).
- A stable URL per live town that opens it directly; unknown/invalid town URLs handled (UF-P2.1-3).
- A defined, data-driven notion of a "live" town, so coverage is not hard-coded in the front end.
- Coming-soon state with email capture and server-side persistence (UF-P2.1-4).
- Live-town-with-no-events empty state (UF-P2.1-5).
- Sitemap and robots policy (UF-P2.1-6), with the production domain supplied by configuration.
- Maintenance page asset (already designed) — exists as a static page only.
- *(v1.1)* Remembering the last chosen town on the browser (UF-P2.1-7).
- *(v1.1)* Town pages carry their own page title, canonical URL, and descriptive metadata, and the
  product name shown to users is consistently **Arangly**.
- *(v1.1)* Nearby-towns suggestions and a "submit an event" link on the nothing-on state.
- *(v1.4)* Automatic deletion of waitlist records after the retention period (OQ-P2.1-3), and marking every
  waitlist record as unverified so a later email sender must confirm the address first.

**Out of scope (explicit):**
- **Sending any email** (the "we'll tell you when it's live" notification). P2.1 only *captures and
  stores* the signal. Sending needs its own consent, unsubscribe, and delivery decisions.
- Accounts/authentication, cross-device recognition (P2 deferred; still deferred).
- Any change to discovery queries, categories, interests, map, or event detail (P2 behavior).
- Places outside Massachusetts, even within 100 miles of Ashland (driver decision, 2026-11-01). They are not
  resolved as places. A visitor who types one gets the "only in Massachusetts for now" message with the email
  form (AC-P2.1-07); the typed text is stored as given.
- Verifying email ownership (double opt-in). P2.1 marks records unverified instead; verification is required
  before any notification is sent (later phase).
- Admin UI to mark towns live (data change only for now — see OQ-P2.1-2).
- Automatic switching of the app into the maintenance page during releases.
- Full security hardening of the public write endpoint (rate limits, bot defense) — the baseline in
  §7 applies; the rest is P4.
- *(v1.1)* The brand-name decision itself is made: **Arangly** (see §15). Only its consistent use on P2.1 screens is a requirement here (AC-P2.1-17).

## 6. SERVICE BOUNDARY
- **Owns (P2.1):** town resolution (name/ZIP → place), the coverage definition and its data, the
  demand-capture record and its endpoint, per-town URL routing, and sitemap/robots content.
- **Reads (does not change):** published events and the discovery API (P2); ZIP centroid data (P1/P2).
- **Does not own:** event ingestion (P1/P3), categories/interests (P2), email delivery (future).
- Consistent with the platform generality rule: demand signals and coverage are modeled as **data**,
  so a new town or a new kind of signal is a new row, not a code or schema change.

## 7. VALIDATIONS ON INCOMING DATA
Outcome legend: **ACCEPT** = store/act as given · **NORMALIZE** = accept after cleaning ·
**REJECT** = refuse with a visible error · **IGNORE** = accept silently without effect.

| # | Input | Rule | Outcome |
|---|-------|------|---------|
| V1 | Search text (town/ZIP) | Trimmed; case-insensitive; length 1–80 chars; punctuation removed; a trailing state word ("ma", "mass", "massachusetts") ignored, so "Ashland, MA" finds Ashland. Text: every typed word must match the **start** of a word in the place name, in any order. Digits only: 1–5 digits match the start of a ZIP; more than 5 digits or a ZIP+4 ("01721-1234") use the first 5 digits. Mixed letters and digits are treated as words | NORMALIZE; over-length → REJECT (400) |
| V2 | Search text with no match | No suggestions and ≥3 characters. (1–2 characters with no match: no message, the visitor keeps typing) | The "not found in Massachusetts" message with the email form (AC-P2.1-07). Never a coming-soon claim for a place Arangly cannot verify |
| V3 | Email (waitlist) | Single address, ≤254 chars, one `@` with non-empty local/domain parts; no whitespace/control chars | REJECT (400) with a field error if invalid; NORMALIZE (trim, lowercase the whole address) if valid |
| V4 | Town reference (waitlist) | Must be a resolvable place the server recognizes, or the free-text town the visitor typed (≤80 chars) | NORMALIZE (collapse whitespace); unresolved free text is stored as typed with a flag, not rejected |
| V5 | Duplicate (same email + same town) | Already recorded | IGNORE the duplicate row but still report success (idempotent; avoids leaking whether an email is on a list) |
| V6 | Near-me coordinates | Latitude −90…90, longitude −180…180, both finite numbers | REJECT invalid; coordinates are used transiently and **not stored** |
| V7 | Town URL slug | Lowercase letters, digits, hyphens; ≤80 chars | Mixed case or a trailing slash → permanent redirect to the lowercase form without the slash; unknown or malformed → the landing screen (never a blank page) |
| V8 | Waitlist request rate | Baseline abuse guard (OQ-P2.1-3 sets the policy); full hardening is P4 | REJECT excess with 429 |

## 8. DEPENDENCIES & INTEGRATIONS
- **P2 (built):** SPA, discovery API (`/api/discovery`), seeded location list, two-screen flow.
- **ZIP centroid data (built):** `geo/zip-centroids.csv` — ZIP → latitude/longitude only. Verified facts
  (corrected in v1.4): it has no place names and no state; it holds 1,365 ZIPs within about 100 miles of
  Ashland, including non-Massachusetts ZIPs (Rhode Island 028xx–029xx, New Hampshire, southern Maine and
  Vermont, Connecticut, and a few New York ZIPs). A two-digit prefix does **not** give the state (for example
  `05501` and `05544` are Andover, MA, and prefix `02` includes Rhode Island). **A place-name source is
  therefore a NEW dependency** (town name ↔ ZIP ↔ state), and the non-MA rows must be excluded.
- **Place-data source (to be evaluated, not committed):** **OpenStreetMap is the preferred source.** A free
  alternative replaces it only if the evaluation (SDD task) shows it is better on coverage, accuracy, license
  terms, or effort. Free candidates named for that evaluation, none verified yet: US Census gazetteer files
  (believed public domain), MassGIS town data, GeoNames (believed CC BY). See A4.
- **Browser geolocation:** the near-me button depends on the browser Geolocation API and user
  permission. Where it is denied or unavailable, the flow falls back to search.
- **Public hosting / domain:** the sitemap and robots need the production public base URL
  (configuration, not hard-coded). Today the app is served through a Cloudflare tunnel. The AWS front door
  (load balancer, tunnel, or other) is **not decided**; nothing in P2.1 may assume it (see A12).
- **Design reference:** the three landing states and the maintenance page listed in `related_docs`.

## 9. CONSTRAINTS
- **Generality first:** a new town, a new coverage status, or a new demand-signal kind must be new
  data, not new schema or code. Do not add a per-feature primary table if an existing generic
  mechanism fits (decided under OQ-P2.1-1).
- **Honesty:** no UI state may claim coverage, a save, or a notification that did not occur.
- **Privacy:** an email address is personal data. Store only what is needed (email, town, timestamp,
  consent statement shown). No third-party sharing. Retention: a waitlist record is deleted 24 months
  after it was made (AC-P2.1-20); removal on request is an operator step. Every record is unverified until a
  later phase confirms the address.
- **Resilience:** the landing must remain usable if the server is unreachable (search over a built-in
  fallback of the seeded towns is acceptable) but must not claim to save an email.
- **Place data stays on our side:** what a visitor types into the town search is not sent to any third-party service while they type or when they submit. Reasons: privacy, and the usage rules of free public geocoding services (which forbid type-ahead use). The place data comes from an open dataset whose license allows our use, with the credit each license requires shown to visitors. **OpenStreetMap is preferred** unless a free alternative proves better (see §8). *(v1.3, reworded v1.4)*
- **Accessibility:** the search, suggestions, and form are keyboard-operable and have labels; error
  and success messages are announced (`role=alert`/`status`).
- **Brand:** the user-facing product name is **Arangly** (logo, page titles, accessible labels, copy). *(v1.1)*
- **Region:** coverage claims are limited to the supported region: **Massachusetts only** for now (driver
  decision, 2026-11-01), even though the ZIP data reaches other states within 100 miles.
- **Environments:** developed and tested on dev (:8081, `arang_dev`); prod (:8080) only by explicit
  release (see `.kiro/steering/environments.md`).

## 10. CONCEPTS
- **Place:** a Massachusetts municipality (city or town) with at least one ZIP and a centre point. The unit
  the visitor searches for and the URL slug identifies (e.g. "Ashland, MA"). Villages and neighborhoods
  inside a municipality are not places in P2.1. *(v1.4)*
- **Supported region:** the set of places the system can resolve: the Massachusetts municipalities. Anything
  outside is *unresolvable*. *(Corrected in v1.4; earlier versions wrongly said New England.)*
- **Live town (covered):** a place Arang has marked as served, so its discovery page is reachable by
  URL and listed in the sitemap. The exact rule (curated flag vs. "has published events") is
  **OQ-P2.1-2**; the recommended default is a curated flag stored as data.
- **Coming-soon town:** a resolvable place that is not live.
- **Demand signal:** a recorded wish — "this person wants Arang in this town" — consisting of an
  email, a town reference, a timestamp, and the consent wording that was shown.
- **Town slug:** the stable name of a place inside its URL, `<town>-<state>` in lowercase with hyphens
  (e.g. `ashland-ma`). The state suffix keeps slugs unique when the region later grows beyond Massachusetts
  (a "Springfield" exists in several states). A slug, once published, does not change; a renamed town keeps
  its old slug as a redirect.
- **Town URL:** `/towns/<town slug>` (e.g. `/towns/ashland-ma`). The path names the town. Query
  parameters carry only view options (time window, category) and are not part of the canonical URL.
  *(v1.2, decision 2026-11-01.)*
- **Empty town:** a live town with zero published events in the current window — distinct from a
  coming-soon town.

## 11. ASSUMPTIONS
- **A1 [DECISION, v1.4]:** The supported region is Massachusetts only (driver, 2026-11-01). Expanding it
  later means adding data, not code. *(v1.0–v1.3 said New England; that was wrong for the data present.)*
- **A2 [ASSUMPTION]:** The existing seeded 28 towns (all in Massachusetts) become the initial **live** set;
  every other Massachusetts municipality starts as coming-soon.
- **A3 [ASSUMPTION]:** Near-me uses a 100-mile limit to the nearest live town (it matches the
  footer text already shown on the landing page); the limit is configuration.
- **A4 [ASSUMPTION — unverified]:** A free dataset that lists the Massachusetts municipalities with ZIPs and
  centre points can be bundled under its license. OpenStreetMap is preferred (§8); the license and credit
  terms of whichever source is chosen — including the ODbL share-alike question for OpenStreetMap — are
  verified before that source is used.
- **A5 [ASSUMPTION]:** A ZIP may map to more than one municipality, and a municipality to several ZIPs;
  search returns the combination, and the slug uses the name + state. The 351 Massachusetts municipality
  names are believed unique (to be confirmed by a data check).
- **A6 [ASSUMPTION]:** The demand-capture endpoint is public and unauthenticated (like the SPA);
  abuse is controlled by validation and a baseline throttle, not by accounts.
- **A7 [ASSUMPTION]:** Sending notification email is a later phase, so P2.1 needs no email provider.
- **A8 [ASSUMPTION]:** The sitemap and robots content is generated from the live-town data so it
  cannot drift from what is actually live.
- **A9 [ASSUMPTION — from the site review]:** the production public domain is `https://arangly.com`, supplied to the sitemap and canonical URLs by configuration (never hard-coded in a page). *(v1.1)*
- **A10:** *(withdrawn on 2026-11-01; the number is not reused.)*
- **A11 [ASSUMPTION, v1.4]:** Town pages may be cached for up to 60 seconds (by the app and by any front-door
  cache). A newly published event, a town going live or hidden, and the matching sitemap change can therefore
  take up to about one minute to appear (driver agreed, 2026-11-01).
- **A12 [ASSUMPTION, v1.4]:** The production front door is not fixed. Whether the client address can be read
  from a proxy header depends on the deployment, so it is configuration, set and checked per deployment.


## 12. OPEN QUESTIONS
| ID | Question | Status | Recommended default | Owner |
|----|----------|--------|---------------------|-------|
| OQ-P2.1-1 | **Where does a demand signal live?** Reuse existing generic tables (the platform's per-entity metadata and user mechanism: the email as a lightweight user, the town as metadata) or add a dedicated table? | RESOLVED (2026-10-31, by approval — recommended default adopted; reopen if wrong) | Reuse the existing generic mechanisms if they can express email + town + timestamp + consent without a migration; otherwise one small dedicated table. The SDD decides the mechanics once you confirm the intent ("a demand signal is data"). Needs your call because it touches the long-lived schema. | driver |
| OQ-P2.1-2 | **What makes a town "live", and how big is the supported region?** (a) a curated flag on the place; (b) has at least one published event; (c) every resolvable place counts as live. And: is the region New England only for now? | RESOLVED (2026-10-31, by approval — recommended default adopted; reopen if wrong) | (a) a curated flag as data, starting with the current ~28 towns; New England only. (b) would flip towns between live and coming-soon as events appear, which is confusing; (c) would break the honesty goal.  *(Region superseded in v1.4: Massachusetts only, driver decision 2026-11-01.)* | driver |
| OQ-P2.1-3 | **Email privacy and abuse policy:** what consent wording, how long to keep an address, how can someone ask to be removed, and what baseline throttle applies? | RESOLVED (2026-10-31, by approval — recommended default adopted; reopen if wrong) | Show a one-line consent ("We'll only email you about Arang in <town>"); keep until the town goes live and a notification is sent or 24 months, whichever is first; removal by email request for now; throttle per client (e.g. 5 requests/min) with a 429 response.  *(v1.4: retention is fixed at 24 months with automatic deletion, AC-P2.1-20; the "until notified" part is dropped because P2.1 sends no email.)* | driver |
| OQ-P2.1-4 | **Crawlable content:** town pages are rendered in the browser today, so a crawler that does not run scripts sees an empty page. Prerender the live-town pages at release time, or render them on the server per request? *(v1.1)* | RESOLVED (2026-11-01, driver decision): the server adds the town's title, canonical URL, description, and current events to the page when it is requested, so a crawler that does not run scripts sees them | Prerender at release (simpler, no new runtime); the SDD confirms feasibility. Needs a product nod because it ties town data to the release process. | driver |

## 13. PRE-SIGN-OFF VERIFICATION
*(Driver approved on 2026-10-31. Boxes ticked below are satisfied by the document; the three left unticked are carried into the SDD as explicit tasks.)*
- [x] One coherent intention (town entry and coverage); no change to event discovery behavior.
- [x] Every UF has ≥1 AC and every AC traces to a UF.
- [x] OQ-P2.1-1, OQ-P2.1-2, OQ-P2.1-3 are resolved or explicitly deferred with an owner.
- [ ] (carried to SDD, task T1) A4: the place-data source is evaluated (OSM preferred) and its license verified before it is used.
- [x] The honesty counter-metrics (no false save, no false coverage) are each covered by an AC.
- [ ] (carried to SDD) The landing UI that was prototyped before this spec is reconciled with these ACs.
- [ ] (carried to SDD) P2 docs (DD-P2-3, DD-P2-9, DD-P2-11) are updated to point to this PRD where it supersedes them.

## 14. ACCEPTANCE CRITERIA
- **AC-P2.1-01** ⟵ UF-P2.1-1: Typing text (≥1 character, normalized as in V1) returns suggestions where every typed word matches
  the **start** of a word in the place name, case-insensitive, in any order (so "ash" and "north and" match,
  "land" does not match Ashland); each shows place, state, and ZIP. Typing 1–5 digits returns places with a ZIP
  that starts with them. Selecting a suggestion for a live town opens that town's events. Pressing Enter with
  exactly one suggestion opens it. At most 8 suggestions are shown besides exact matches (AC-P2.1-02). *(v1.4:
  word-start matching, driver decision.)*
- **AC-P2.1-02** ⟵ UF-P2.1-1: For every Massachusetts municipality, typing its exact name (or one of its
  ZIPs) returns a suggestion for it, **even when many other names also match** — an exact match is never
  dropped by the limit on the number of suggestions. If two places share a name, all appear, each identified.
- **AC-P2.1-03** ⟵ UF-P2.1-2: With location permission granted, "Find events near me" opens the
  nearest live town if it is within the near-me limit (A3). If the nearest live town is farther, no
  town opens; the visitor sees a message that Arang is not near them yet and the search remains
  available. Coordinates are not stored (V6).
- **AC-P2.1-04** ⟵ UF-P2.1-2: If location permission is denied, times out, or is unavailable, the
  visitor sees a clear message and the search input remains usable. No error page appears.
- **AC-P2.1-05** ⟵ UF-P2.1-3: Opening a live town's URL (e.g. `/towns/ashland-ma`) in a fresh browser
  shows that town's discovery screen directly, with no landing step. Using "change town" from there
  returns to the landing screen and clears the remembered town (AC-P2.1-16). The URL of a renamed town's old slug
  redirects permanently to the current one.
- **AC-P2.1-06** ⟵ UF-P2.1-3: Opening a malformed or unknown town URL shows the landing screen (not a
  blank page or server error). Opening the URL of a resolvable but not-live town shows that town's
  coming-soon state. If place data is unavailable, a not-live town's URL shows the landing screen. A town URL
  with uppercase letters or a trailing slash redirects permanently to its lowercase form without the slash.
- **AC-P2.1-07** ⟵ UF-P2.1-4: When the visitor selects a suggestion for a place that is not live, the visitor
  sees "Coming soon to <place>" and an email field. When text of 3 or more characters matches no place, the
  visitor sees that the place was not found in Massachusetts, that Arangly covers Massachusetts only for now,
  a way to retry, and an email field to be told when Arangly reaches them (the typed text is stored as given).
  This message never claims that Arangly is coming to that place. It appears only after the visitor pauses
  typing (about 300 ms) or presses Enter, and only for the response to the latest text. Text of 1–2 characters
  with no match shows no message. *(v1.4: driver decision on non-MA towns; timing added in v1.5.)*
- **AC-P2.1-08** ⟵ UF-P2.1-4: Submitting a valid email for a coming-soon place causes a persisted
  demand signal to exist (email, place, timestamp, consent wording shown), marked unverified. The "You're on
  the list" state appears only after the server confirms the save.
- **AC-P2.1-09** ⟵ UF-P2.1-4: Submitting an invalid email (V3), or when the server cannot save (any
  failure), shows an error, keeps the entered value, and does not show the saved state. Submitting
  the same email for the same place again reports success and creates no duplicate (V5).
- **AC-P2.1-10** ⟵ UF-P2.1-4: The waitlist endpoint exceeds its throttle with a 429 response and the
  UI shows a "try again shortly" message. No response ever reveals whether an email was already stored. When a
  deployment serves a public domain without identifying clients individually, the service warns at startup.
- **AC-P2.1-11** ⟵ UF-P2.1-5: A live town with no published events in the window shows a "nothing on
  this week" state with a way to widen the window or change town; it does not show "coming soon".
  *(v1.1)* The state also lists any other live town within the near-me limit that has at least one
  published event in the window (up to 3, nearest first, omitted if none), and a link to submit an
  event.
- **AC-P2.1-12** ⟵ UF-P2.1-6: `/sitemap.xml` lists the landing page and exactly the live-town URLs,
  each with the production domain from configuration; it lists none of `/add`, `/submit`, `/console`,
  `/review/…`, `/api/…`, `/webhook/…`, `/media/…`, `/tiles/…`, `/health`, `/actuator/…`. When a town is marked
  live or not live, the sitemap reflects it within 60 seconds (A11).
- **AC-P2.1-13** ⟵ UF-P2.1-6: `/robots.txt` disallows those same routes and names the sitemap URL.
- **AC-P2.1-14** ⟵ UF-P2.1-4 *(v1.1, closes the TRD privacy gap)*: A stored email address never appears
  in a URL, the sitemap, any public response, or an application log line at INFO level or above.
- **AC-P2.1-15** ⟵ UF-P2.1-6 *(v1.1)*: Each live-town page has a unique page title naming the town, a
  canonical URL on the configured public domain, and a description naming the town. The landing page
  has its own title, canonical URL, and description. The page content that carries the town name and
  events is available to a crawler that does not run scripts (method decided: OQ-P2.1-4). Text from event
  data on the page cannot run as script.
- **AC-P2.1-16** ⟵ UF-P2.1-7 *(v1.1)*: After a visitor chooses a live town, returning to the site root in
  the same browser opens that town's page directly. "Change town" clears the remembered town, so the landing
  screen stays. If the remembered town is no longer live or no longer exists, it is cleared and the landing is
  shown. Clearing the browser's site data also resets it. Remembering the choice makes no request of its own:
  it is not sent in any cookie or to any endpoint; the only requests are the ones that open the town.
  *(v1.5: change-town and stale-town rules added; confirmed by the driver 2026-11-01.)*
- **AC-P2.1-17** ⟵ UF-P2.1-1, UF-P2.1-3, UF-P2.1-4, UF-P2.1-5 *(v1.1; trace widened in v1.4)*: On the landing, town, coming-soon, and nothing-on screens, no
  visible text, page title, or accessible label uses the name "Arang" without "ly"; the product name
  reads "Arangly".
- **AC-P2.1-18** ⟵ UF-P2.1-1 *(v1.3, resilience)*: When the place data used for town search is missing or
  unusable, the site still starts and every other page works. Town search shows a clear
  "search is unavailable right now" message and the landing offers the live towns that are built into the
  page. The service health report states that place data is unavailable.
- **AC-P2.1-19** ⟵ UF-P2.1-1 *(v1.3, credit and data handling)*: The credit that the place-data license
  requires is visible on the landing page (for example in its footer). Text typed into the town search is
  never sent to a third-party service; this is checked by watching the network requests made while typing.
  The health report shows the date of the place data in use.
- **AC-P2.1-20** ⟵ UF-P2.1-4 *(v1.4, retention)*: A waitlist record is deleted automatically no later than one
  day after it becomes 24 months old. A person record that existed only for the waitlist (no phone, no web
  identity, no saved interests) is deleted with its last waitlist record.

## 15. VERSION HISTORY

- **v0.1 (2026-10-31) — DRAFT (Fast path).** First draft. Origin: after P2 shipped, the next-steps
  review found (a) the landing's hard-coded town list contradicts "any town, any size", (b) towns
  have no URLs, (c) the new coming-soon email capture has no backend, (d) the sitemap can only list
  one URL. Verified before drafting: `geo/zip-centroids.csv` has ZIP→lat/lng only (no place names,
  prefixes 01–06 plus strays) — hence the new place-name dependency (A4). The landing v3 /
  coming-soon UI already exists in the SPA (built ahead of this spec on 2026-10-31); its waitlist call
  is intentionally failing until the endpoint exists. Three open questions (OQ-P2.1-1..3) need the
  driver. No code written for the backend, schema, or routes.
- **v1.0 (2026-10-31) — APPROVED.** Driver sign-off ("consider the PRD approved, move ahead"). Provenance
  caveat recorded honestly: no collaborative drill and no independent spec-reviewer gate were run before
  approval. By approving, the driver adopted the recommended defaults for OQ-P2.1-1 (reuse generic
  data mechanisms for demand signals, SDD decides mechanics), OQ-P2.1-2 (curated live flag as data;
  seeded ~28 towns start live; New England only) and OQ-P2.1-3 (one-line consent; retention until
  notified or 24 months; removal on email request; baseline per-client throttle). The recommended
  defaults were assumed, not separately stated — reopen any of them in a revision if wrong. Three
  pre-sign-off items are carried into the SDD (data license, landing-UI reconciliation, P2 doc updates).
- **v1.1 (2026-11-01) — DRAFT (delta pending driver read-through).** Folded in the items from the
  2026-11-01 site-review checklist that fit this feature's single intention: remember my town (UF-7,
  AC-16), nearby towns and a submit link on the nothing-on state (UF-5, AC-11), crawlable canonical
  page metadata (AC-15), consistent "Arangly" naming (AC-17, §9), and the production domain assumption
  `arangly.com` (A9). Added AC-14 to close the TRD's privacy-without-an-AC gap. New open question
  OQ-P2.1-4 (prerender vs server render). The remaining checklist items were deliberately NOT added here
  to keep one intention; they moved to the new P2.2 PRD (`docs/p2.2-event-detail-trust/prd.md`) or to later phases.
  Decision recorded: **product name = Arangly.** v1.0's approval covers the v1.0 content only.
- **v1.2 (2026-11-01) — DRAFT.** Decision (driver): the town URL form is **`/towns/<town slug>`**, a path
  and not a query parameter. Reasons: a path names a resource and a query parameter changes the view of
  it, search engines treat paths as separate pages, links stay readable, and the `/towns/` prefix
  removes any name clash with other routes (`/add`, `/console`, future pages), so no slug naming rule
  is needed. Filters (time window, category) stay as query parameters and are not in the canonical
  URL. Old slugs of renamed or merged towns must redirect permanently (SDD task). Updated the Concepts
  entry and AC-P2.1-05.
- **v1.3 (2026-11-01) — DRAFT.** Added the product-level facts about place data that came out of the
  geocoding discussion: a constraint (place data stays on our side, license-compliant, credited), AC-P2.1-18
  (resilience when place data is unavailable) and AC-P2.1-19 (data credit shown, no third-party lookups
  while typing, data date in the health report). The implementation approach was deliberately NOT put into
  the requirements; it is kept as a non-binding hand-off note in "Appendix — Notes for the SDD" below so
  that the information is not lost. Closes the gap that the Resilience constraint had no acceptance criterion.
- **v1.3a (2026-11-01):** Marked OQ-P2.1-4 resolved (driver confirmed the SDD's server-injected page shell). No requirement changed.
- **v1.4 (2026-11-01) — DRAFT.** Applied the independent review (verdict NEEDS-CHANGES on PRD v1.3 / TRD v0.4 /
  SDD v0.3) and the driver's decisions on it. **H1:** the region was wrongly called New England; the ZIP file
  holds ZIPs within about 100 miles of Ashland across five states, and prefixes do not give the state. Driver:
  **Massachusetts only for now.** Updated §5, §8, §9, §10, A1, A2, A5. **H2:** driver chose **word-start**
  matching (AC-01, V1). **H3:** an exact match is never dropped by the suggestion limit (AC-02). **H6:** the
  place-data source is not committed; **OpenStreetMap preferred**, a better free alternative may replace it
  (§8, §9, A4). Driver decisions on the review's questions: non-MA text gets the "Massachusetts only for now"
  message with the email form (AC-07, V2); villages and neighborhoods are a later phase (§5, §10); the AWS
  front door is not assumed (§8, A12); waitlist records are auto-deleted at 24 months and marked unverified
  (AC-08, AC-20, §9); town pages may be cached for 60 seconds (A11). Review fixes without product change: the
  3-character rule for "not found" (V2), mixed-case and trailing-slash redirects (V7), whole-address
  lowercasing (V3), extra robots exclusions (AC-12), escaping of event text on server pages (AC-15), AC-17
  trace widened, OQ-P2.1-4 resolution worded as the driver's decision, mockups copied into
  `ui-reference/`. Note: after the region change, a typed out-of-state town is *not* recognized as a place
  (it is stored as typed text); recognizing nearby out-of-state towns by name is part of a later region
  expansion.
- **v1.5 (2026-11-01) — DRAFT.** Applied the second gate (PASS-WITH-FIXES). **N-H1:** "change town" now clears the
  remembered town, and a remembered town that is no longer live is cleared (AC-05, AC-16). Also: AC-16 states
  that remembering makes no request of its own; the sitemap and live-status changes may take up to 60 seconds
  (AC-12, A11); search input rules for punctuation, a trailing state word, long digit strings, and mixed text
  (V1); "not found" timing and the 1–2 character rule (AC-07); old-slug and case/slash redirects (AC-05, AC-06);
  the startup warning (AC-10); the cap of 8 named in AC-01; G1 wording; assumption order (A10 withdrawn); OQ-3
  retention note; duplicate §9 sentence removed; version entries moved above the appendix; appendix marked as
  partly superseded.
- **v1.5a (2026-11-01):** Third gate fix: AC-01 refers to the V1 normalization rules (N2). Driver confirmed the
  change-town rule.
- **v1.5 APPROVED (2026-11-01).** Driver sign-off of PRD v1.5 (incl. v1.5a), after three independent review gates (NEEDS-CHANGES → PASS-WITH-FIXES → PASS-WITH-FIXES, all fixes applied).

---

## Appendix — Notes for the SDD (NON-BINDING hand-off; not requirements)
> **Partly superseded (v1.5):** the SDD v0.4/v0.5 now governs. In particular: the region is Massachusetts only, search
> runs in memory without a full-text or spatial index (SDD DD-P2.1-11), and the place source is evaluated in SDD task
> T1 (OpenStreetMap preferred). Where these notes differ, the SDD wins.
>
> **Why this is here:** these notes capture a design discussion held on 2026-11-01 so it is not lost.
> They are **recommendations for the SDD to confirm or change**. They are not requirements and they do
> not change any acceptance criterion. If the SDD chooses differently, the PRD does not need to change.

**Context.** The town search needs place names and points. Public free geocoding (OSM Nominatim) cannot be
used for type-ahead or for listing all towns — its usage policy forbids both, and it allows only 1 request
per second per application. So the place data must be local.

**Recommended approach (to confirm in the SDD):**
1. **Source data (preferred, not committed — v1.4):** an OpenStreetMap extract for the supported region (for example Massachusetts or New
   England from Geofabrik), plus the existing ZIP-centroid file. License: ODbL — attribution is required;
   share-alike duties for a published derived database are **not yet confirmed**. Read the license first.
2. **Offline build:** a script (laptop or CI, never the API server) downloads the extract and keeps only
   what is needed — for P2.1: the Massachusetts municipalities with state, ZIPs, and point (villages and neighborhoods are a later phase, v1.4). It writes
   a **SQLite file** (a GeoPackage is a ready-made SQLite-based alternative) with a full-text table for
   prefix search and an R-tree for nearest-point queries. The script **validates** the result (row counts,
   sample lookups such as "Ashland") and refuses to publish a bad file. The file carries a metadata table
   (OSM data date, schema version, license text) and a dated name such as `geo-2026-11-01.sqlite`.
3. **Publish and deploy:** the script uploads the file to object storage (S3). A deploy step — container
   start script or image build, not application code — places it on local disk. Several servers each hold
   their own copy; the file is read-only, so no sharing or sync is needed. Replace the file as a whole,
   never in place.
4. **Application start-up (the only logic in the API server):** read the file path from configuration, open
   the file **read-only**, check the metadata schema version, expose the data date in the health report. The
   app never downloads, converts, or writes to the file. If the file is missing or the version is wrong, the
   app starts anyway and degrades as AC-P2.1-18 describes.
5. **Writes go elsewhere:** any point computed for an event is stored in the main database (MySQL), not in
   the SQLite file.

### How the SQLite file is populated (step by step; NON-BINDING)
**Who and when.** A developer runs the build script by hand for the first file. Later, a scheduled job
(for example monthly, or on demand when a new region is added) can run it. It runs offline, on a laptop or in
CI. **It never runs inside the API server.** Its output is a file, not a database change.

**Inputs.**
- An OSM extract (`.osm.pbf`) for the supported region from a public mirror such as Geofabrik. P2.1 can start
  with Massachusetts. Add other New England states when coverage grows.
- The existing ZIP-centroid file (`geo/zip-centroids.csv`).
- The list of live towns, if the build should mark them (the live flag itself is data in the main database,
  per OQ-P2.1-2, so the SQLite file does not need to own it).

**Steps.**
1. **Download** the extract. Record the source address, the download date, and a checksum.
2. **Filter** to the features needed, using an OSM tool (osmium or pyosmium). For P2.1 (v1.4): the Massachusetts municipalities and their boundaries only; villages, hamlets and neighborhoods are a later phase. For P2.2 add: addresses (house number, street, ZIP)
   and named venues (places of worship, libraries, community centres, halls, parks). The exact OSM tags are
   to be confirmed in the SDD against real data.
3. **Normalize.** Make a lowercase, accent-free search name. Set the state code. Attach ZIPs to places.
   Create the town slug `<town>-<state>` and make sure every slug is unique. Remove duplicates.
4. **Write the tables** into a new SQLite file. Expected shape (names are illustrative, the SDD decides):
   `place` (id, OSM id, name, search name, type, state, slug, latitude, longitude), `place_zip` (place, ZIP),
   `zip` (ZIP, latitude, longitude, state), a full-text index on place names for prefix search, an R-tree index
   for nearest-point queries, and a `meta` table (OSM data date, build date, schema version, license text,
   source checksum). P2.2 adds `address`, `venue`, and their full-text indexes.
5. **Validate. Stop and publish nothing if any check fails.** Checks: row counts above a set minimum; every
   one of the 28 current seeded towns resolves to the right state and ZIP; slugs are unique; every point
   lies inside the supported region; no empty names; a prefix search for "ash" returns Ashland; (P2.2) a known
   address such as 117 Waverly St, Ashland resolves to a point near the temple; the file opens read-only;
   the schema version is set.
6. **Finalize.** Compact the file, mark it read-only, name it `geo-YYYY-MM-DD.sqlite`, and compute its checksum.
7. **Publish.** Upload the file to object storage (S3). Keep the last few versions. Update a small manifest
   file that names the current version and its checksum.
8. **Deploy.** A deploy step (container start script or image build, not application code) reads the manifest,
   copies the named file to local disk, and checks the checksum. The app then reads its path from
   configuration. A restart picks up a new file.
9. **Roll back** by pointing the manifest at the previous version and restarting.

**Local development.** The same script writes the file into a git-ignored folder (for example `data/geo/`).
The app config points to it. Dev and AWS therefore use identical files.

**Refresh rhythm.** Place names change slowly. Monthly or on demand is enough. Address and venue data change
faster, so P2.2 may refresh more often.

**What must never happen.** The API server must not download OSM data, build tables, or write to the file.
The server's only duties are: open it read-only, check the schema version, report the data date, and degrade
as AC-P2.1-18 describes if the file is missing or wrong.

**Open details for the SDD:** the supported region at launch, the exact OSM tags, the full-text tokenizer
settings (prefix search, accent folding), the actual file size, where the manifest lives, and who owns the
refresh job.

**Tasks this implies for the SDD:** (a) the build-and-validate script with its README; (b) the thin read-only
reader and its health/degraded behavior; (c) the deploy step that places the file. **Checks to run first:**
the Java SQLite driver includes full-text and R-tree support; the actual size of the extract; OSM coverage
for real local places (for example the Sri Lakshmi Temple, 117 Waverly St, Ashland); the ODbL duties.

**Limits to remember:** matching logic is ours to write (street-type variants, ZIP handling); it will be weaker
on messy flyer text than a full geocoder such as a self-hosted Nominatim; coverage depends on OSM.

**Alternatives noted:** self-hosted Nominatim (better address search, needs PostgreSQL/PostGIS and several GB
of disk and RAM); a paid geocoding service (simple, ongoing cost and its own storage rules).
