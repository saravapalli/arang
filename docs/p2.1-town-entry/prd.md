---
title: Arang — Town Entry & Coverage (P2.1)
type: PRD
artifact_id: ARANG-PRD-P2-1
parent: ARANG-INIT-001
status: approved
version: 1.0
driver: (you)
approver: (you)
approver_role: product owner / driver
assigned_date: 2026-10-31
first_draft_target: 2026-10-31
last_revised_date: 2026-10-31
approved_date: 2026-10-31
review_provenance: first draft (Fast path), NO independent spec-reviewer gate and NO collaborative drill run. Driver approved directly on 2026-10-31 and asked to proceed; the recommended defaults for OQ-P2.1-1..3 were adopted by that approval (see §12, §15).
jira:
multifeature: false
related_docs:
  - docs/prd.md (initiative overview — Epic 3 / P2; this is a follow-on to P2, phase tag P2.1)
  - docs/p2-consumer-discovery/prd.md (P2 — approved + built; this PRD extends its "choose a location" entry point, UF-P2-1)
  - docs/p2-consumer-discovery/sdd.md (DD-P2-3 deferred real town geocoding; DD-P2-9 two-screen landing flow; DD-P2-11 "any town, any size" positioning — all three are inputs here)
  - docs/p1-producer-intake/sdd.md (schema: user, user_metadata, event — reused, not changed, unless OQ-P2.1-1 decides otherwise)
  - /tmp/arang-mockups/landing-v3.html, landing-v3-typing.html, coming-soon.html, maintenance.html (the design source for the landing states; copy into docs/p2.1-town-entry/ui-reference/ at sign-off so the reference is durable)
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
of ~28 seeded New England towns**. A visitor who types any other town (e.g. "Worcester") has no real
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
- **G1 — Find any in-region town fast:** a visitor can reach their town by typing a town name or
  ZIP, or by tapping near-me, in a few seconds.
  *Metric: for any place in the supported region (see Concepts), typing its name or ZIP yields a
  matching suggestion; selecting it opens that town's events.*
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
  window, I see an honest "nothing on" state — distinct from "coming soon" — not a dead end.

### Group C — Being found
- **UF-P2.1-6 (search engine discovery)** `[core]` — As a search engine crawler, I can read a sitemap
  of live-town pages and a robots policy that excludes write/admin routes.

*Coverage: every UF has ≥1 AC in §14; every AC traces to a UF. Deferred items (sending
notification emails, sharing, accounts) appear only in §5 "Out of scope".*

## 5. SCOPE
**In scope (P2.1):**
- Town/ZIP search with suggestions over the supported region (UF-P2.1-1).
- Near-me → nearest live town, with a defined distance limit (UF-P2.1-2).
- A stable URL per live town that opens it directly; unknown/invalid town URLs handled (UF-P2.1-3).
- A defined, data-driven notion of a "live" town, so coverage is not hard-coded in the front end.
- Coming-soon state with email capture and server-side persistence (UF-P2.1-4).
- Live-town-with-no-events empty state (UF-P2.1-5).
- Sitemap and robots policy (UF-P2.1-6), with the production domain supplied by configuration.
- Maintenance page asset (already designed) — exists as a static page only.

**Out of scope (explicit):**
- **Sending any email** (the "we'll tell you when it's live" notification). P2.1 only *captures and
  stores* the signal. Sending needs its own consent, unsubscribe, and delivery decisions.
- Accounts/authentication, cross-device recognition (P2 deferred; still deferred).
- Any change to discovery queries, categories, interests, map, or event detail (P2 behavior).
- Places outside the supported region (see Concepts and OQ-P2.1-2); a visitor outside it gets the
  same coming-soon capture, but Arang does not claim to cover or resolve them as towns.
- Admin UI to mark towns live (data change only for now — see OQ-P2.1-2).
- Automatic switching of the app into the maintenance page during releases.
- Full security hardening of the public write endpoint (rate limits, bot defense) — the baseline in
  §7 applies; the rest is P4.
- Brand/name decision ("Arang" vs "Arangly") — recorded as a decision, not a requirement here.

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
| V1 | Search text (town/ZIP) | Trimmed; case-insensitive; length 1–80 chars; ZIP is 5 digits (a 5-digit prefix of 1–4 digits matches prefix) | NORMALIZE; over-length → REJECT (400) |
| V2 | Search text with no match | No suggestions and ≥3 characters | Shows coming-soon path (UF-P2.1-4) only if the text resolves to a real place; otherwise a "couldn't find that place" message (see AC-P2.1-07) |
| V3 | Email (waitlist) | Single address, ≤254 chars, one `@` with non-empty local/domain parts; no whitespace/control chars | REJECT (400) with a field error if invalid; NORMALIZE (trim, lowercase domain) if valid |
| V4 | Town reference (waitlist) | Must be a resolvable place the server recognizes, or the free-text town the visitor typed (≤80 chars) | NORMALIZE (collapse whitespace); unresolved free text is stored as typed with a flag, not rejected |
| V5 | Duplicate (same email + same town) | Already recorded | IGNORE the duplicate row but still report success (idempotent; avoids leaking whether an email is on a list) |
| V6 | Near-me coordinates | Latitude −90…90, longitude −180…180, both finite numbers | REJECT invalid; coordinates are used transiently and **not stored** |
| V7 | Town URL slug | Lowercase letters, digits, hyphens; ≤80 chars | Unknown or malformed slug → falls back to the landing screen (never a blank page) |
| V8 | Waitlist request rate | Baseline abuse guard (OQ-P2.1-3 sets the policy); full hardening is P4 | REJECT excess with 429 |

## 8. DEPENDENCIES & INTEGRATIONS
- **P2 (built):** SPA, discovery API (`/api/discovery`), seeded location list, two-screen flow.
- **ZIP centroid data (built):** `geo/zip-centroids.csv` — ZIP → latitude/longitude only. Verified
  facts: it has no place names, and its ZIP prefixes cover 01–06 (New England) plus a few strays.
  **A place-name source is therefore a NEW dependency** (town name ↔ ZIP). Candidate: GeoNames
  postal data — its license/attribution terms are **not yet verified** (see A4).
- **Browser geolocation:** the near-me button depends on the browser Geolocation API and user
  permission. Where it is denied or unavailable, the flow falls back to search.
- **Public hosting / domain:** the sitemap and robots need the production public base URL
  (configuration, not hard-coded). Served via the existing Cloudflare tunnel; the files can also be
  uploaded at the edge (see OQ in §12 if routing differs).
- **Design reference:** the three landing states and the maintenance page listed in `related_docs`.

## 9. CONSTRAINTS
- **Generality first:** a new town, a new coverage status, or a new demand-signal kind must be new
  data, not new schema or code. Do not add a per-feature primary table if an existing generic
  mechanism fits (decided under OQ-P2.1-1).
- **Honesty:** no UI state may claim coverage, a save, or a notification that did not occur.
- **Privacy:** an email address is personal data. Store only what is needed (email, town, timestamp,
  consent statement shown). No third-party sharing. Retention and deletion handling are stated in
  OQ-P2.1-3 before sign-off.
- **Resilience:** the landing must remain usable if the server is unreachable (search over a built-in
  fallback of the seeded towns is acceptable) but must not claim to save an email.
- **Accessibility:** the search, suggestions, and form are keyboard-operable and have labels; error
  and success messages are announced (`role=alert`/`status`).
- **Region:** coverage claims are limited to the supported region (New England ZIP data is what is
  actually present today).
- **Environments:** developed and tested on dev (:8081, `arang_dev`); prod (:8080) only by explicit
  release (see `.kiro/steering/environments.md`).

## 10. CONCEPTS
- **Place:** a named locality with at least one ZIP and a centroid (lat/lng). The unit the visitor
  searches for and the URL slug identifies (e.g. "Ashland, MA").
- **Supported region:** the set of places the system can resolve. Today, New England states (by ZIP
  prefix 01–06). Anything outside is *unresolvable*.
- **Live town (covered):** a place Arang has marked as served, so its discovery page is reachable by
  URL and listed in the sitemap. The exact rule (curated flag vs. "has published events") is
  **OQ-P2.1-2**; the recommended default is a curated flag stored as data.
- **Coming-soon town:** a resolvable place that is not live.
- **Demand signal:** a recorded wish — "this person wants Arang in this town" — consisting of an
  email, a town reference, a timestamp, and the consent wording that was shown.
- **Town slug:** the stable URL form of a place, `<town>-<state>` in lowercase with hyphens (e.g.
  `ashland-ma`), unique across states (a "Springfield" exists in several).
- **Empty town:** a live town with zero published events in the current window — distinct from a
  coming-soon town.

## 11. ASSUMPTIONS
- **A1 [ASSUMPTION]:** The first supported region is New England (ZIP prefixes 01–06) because that is
  the only ZIP data present; expanding the region means adding data, not code.
- **A2 [ASSUMPTION]:** The existing seeded ~28 towns become the initial **live** set; everything else
  in the region starts as coming-soon.
- **A3 [ASSUMPTION]:** Near-me uses a 100-mile limit to the nearest live town (it matches the
  footer text already shown on the landing page); the limit is configuration.
- **A4 [ASSUMPTION — unverified]:** A free place-name dataset (e.g. GeoNames postal codes) can be
  used and bundled under its license with attribution. Not yet checked.
- **A5 [ASSUMPTION]:** A ZIP may map to more than one place name, and a place to several ZIPs;
  search returns the combination, and the slug uses the place name + state.
- **A6 [ASSUMPTION]:** The demand-capture endpoint is public and unauthenticated (like the SPA);
  abuse is controlled by validation and a baseline throttle, not by accounts.
- **A7 [ASSUMPTION]:** Sending notification email is a later phase, so P2.1 needs no email provider.
- **A8 [ASSUMPTION]:** The sitemap and robots content is generated from the live-town data so it
  cannot drift from what is actually live.

## 12. OPEN QUESTIONS
| ID | Question | Status | Recommended default | Owner |
|----|----------|--------|---------------------|-------|
| OQ-P2.1-1 | **Where does a demand signal live?** Reuse existing generic tables (the platform's per-entity metadata and user mechanism: the email as a lightweight user, the town as metadata) or add a dedicated table? | RESOLVED (2026-10-31, by approval — recommended default adopted; reopen if wrong) | Reuse the existing generic mechanisms if they can express email + town + timestamp + consent without a migration; otherwise one small dedicated table. The SDD decides the mechanics once you confirm the intent ("a demand signal is data"). Needs your call because it touches the long-lived schema. | driver |
| OQ-P2.1-2 | **What makes a town "live", and how big is the supported region?** (a) a curated flag on the place; (b) has at least one published event; (c) every resolvable place counts as live. And: is the region New England only for now? | RESOLVED (2026-10-31, by approval — recommended default adopted; reopen if wrong) | (a) a curated flag as data, starting with the current ~28 towns; New England only. (b) would flip towns between live and coming-soon as events appear, which is confusing; (c) would break the honesty goal. | driver |
| OQ-P2.1-3 | **Email privacy and abuse policy:** what consent wording, how long to keep an address, how can someone ask to be removed, and what baseline throttle applies? | RESOLVED (2026-10-31, by approval — recommended default adopted; reopen if wrong) | Show a one-line consent ("We'll only email you about Arang in <town>"); keep until the town goes live and a notification is sent or 24 months, whichever is first; removal by email request for now; throttle per client (e.g. 5 requests/min) with a 429 response. | driver |

## 13. PRE-SIGN-OFF VERIFICATION
*(Driver approved on 2026-10-31. Boxes ticked below are satisfied by the document; the three left unticked are carried into the SDD as explicit tasks.)*
- [x] One coherent intention (town entry and coverage); no change to event discovery behavior.
- [x] Every UF has ≥1 AC and every AC traces to a UF.
- [x] OQ-P2.1-1, OQ-P2.1-2, OQ-P2.1-3 are resolved or explicitly deferred with an owner.
- [ ] (carried to SDD) A4 place-name data license is verified before the SDD commits to a dataset.
- [x] The honesty counter-metrics (no false save, no false coverage) are each covered by an AC.
- [ ] (carried to SDD) The landing UI that was prototyped before this spec is reconciled with these ACs.
- [ ] (carried to SDD) P2 docs (DD-P2-3, DD-P2-9, DD-P2-11) are updated to point to this PRD where it supersedes them.

## 14. ACCEPTANCE CRITERIA
- **AC-P2.1-01** ⟵ UF-P2.1-1: Typing a town name fragment (≥1 character) returns suggestions whose
  place name contains the fragment, case-insensitive, each showing place, state, and ZIP; typing a
  5-digit ZIP, or a 1–4 digit prefix, returns places with a matching ZIP. Selecting a suggestion for
  a live town opens that town's events.
- **AC-P2.1-02** ⟵ UF-P2.1-1: For every place in the supported region, typing its exact name (or one
  of its ZIPs) returns a suggestion for it. Where two places share a name in different states, both
  appear with their states.
- **AC-P2.1-03** ⟵ UF-P2.1-2: With location permission granted, "Find events near me" opens the
  nearest live town if it is within the near-me limit (A3). If the nearest live town is farther, no
  town opens; the visitor sees a message that Arang is not near them yet and the search remains
  available. Coordinates are not stored (V6).
- **AC-P2.1-04** ⟵ UF-P2.1-2: If location permission is denied, times out, or is unavailable, the
  visitor sees a clear message and the search input remains usable. No error page appears.
- **AC-P2.1-05** ⟵ UF-P2.1-3: Opening a live town's URL (e.g. `/ashland-ma`) in a fresh browser
  shows that town's discovery screen directly, with no landing step. Using "change town" from there
  returns to the landing screen.
- **AC-P2.1-06** ⟵ UF-P2.1-3: Opening a malformed or unknown town URL shows the landing screen (not a
  blank page or server error). Opening the URL of a resolvable but not-live town shows that town's
  coming-soon state.
- **AC-P2.1-07** ⟵ UF-P2.1-4: When the search text resolves to a place that is not live, the visitor
  sees "Coming soon to <place>" and an email field. When the text does not resolve to any place in
  the supported region, the visitor sees a message that the place was not found, with a way to
  retry — not a coming-soon claim for a place Arang cannot verify.
- **AC-P2.1-08** ⟵ UF-P2.1-4: Submitting a valid email for a coming-soon place causes a persisted
  demand signal to exist (email, place, timestamp, consent wording shown). The "You're on the list"
  state appears only after the server confirms the save.
- **AC-P2.1-09** ⟵ UF-P2.1-4: Submitting an invalid email (V3), or when the server cannot save (any
  failure), shows an error, keeps the entered value, and does not show the saved state. Submitting
  the same email for the same place again reports success and creates no duplicate (V5).
- **AC-P2.1-10** ⟵ UF-P2.1-4: The waitlist endpoint exceeds its throttle with a 429 response and the
  UI shows a "try again shortly" message. No response ever reveals whether an email was already stored.
- **AC-P2.1-11** ⟵ UF-P2.1-5: A live town with no published events in the window shows an "nothing on
  this week" state with a way to widen the window or change town; it does not show "coming soon".
- **AC-P2.1-12** ⟵ UF-P2.1-6: `/sitemap.xml` lists the landing page and exactly the live-town URLs,
  each with the production domain from configuration; it lists none of `/add`, `/submit`, `/console`,
  `/review/…`, `/api/…`, `/webhook/…`, `/media/…`. When a town is marked live or not live, the next
  sitemap fetch reflects it.
- **AC-P2.1-13** ⟵ UF-P2.1-6: `/robots.txt` disallows those same routes and names the sitemap URL.

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
