---
title: Arangly — Event Detail, Accuracy & Trust (P2.2)
type: PRD
artifact_id: ARANG-PRD-P2-2
parent: ARANG-INIT-001
status: draft
version: 0.2
driver: (you)
approver: (you)
approver_role: product owner / driver
assigned_date: 2026-11-01
first_draft_target: 2026-11-01
last_revised_date: 2026-11-01
approved_date:
review_provenance: none yet — first draft (Fast path). Next: collaborative drill (prd-section-reviewer), then independent gate (spec-reviewer agent), then driver sign-off.
jira:
multifeature: false
related_docs:
  - docs/prd.md (initiative overview — Epic 3 / P2; this is a follow-on to P2, phase tag P2.2)
  - docs/p2-consumer-discovery/prd.md (P2 — the list, map, and detail this PRD corrects and completes)
  - docs/p2.1-town-entry/prd.md (P2.1 — town entry; runs before or alongside this one, no dependency in either direction except the shared event URL base)
  - docs/p1-producer-intake/prd.md (P1 — the publish gate that decides which events become visible)
  - ~/Downloads/Arangly_Enhancement_Checklist.md (2026-11-01 review of https://arangly.com — source of §1 and part of §3 and §5)
origin: Derived from the 2026-11-01 site review. The review found the displayed event details wrong, incomplete, or unactionable for a resident; this PRD fixes the display and the data behind it. Includes the user-reported mismatch between pin coordinates and the event address.
tags: [event-detail, accuracy, geocoding, trust, share, calendar, directions, report-error, consumer]
---

# Arangly — Event Detail, Accuracy & Trust (P2.2)

> **One coherent intention:** *when a resident looks at an event, what they see must be correct,
> complete enough to act on, and honest about how reliable it is.* It covers how an event's date,
> distance, place, and details are shown and validated; the actions available on an event (share,
> add to calendar, directions, register, report a problem); and a stable link for each event. It
> does **not** cover finding or collecting events (ingestion, P3), approving them (curation), the
> organizer's side (intake), or new ways to filter and personalize (a later P2.3).

## 1. CONTEXT
P2 shipped discovery: a list, a map with pins, a detail view, and a Google Maps link. The 2026-11-01
review of the live site, plus a direct report from the driver, found problems that make an event
hard to trust or act on:

- **Dates are ambiguous.** Cards showed "FRI 30" or "SUN 8" with no month or year.
- **Distance is blank.** Cards and details showed "away" with no number.
- **Place is wrong.** An event titled "Multiple A2B Locations (IL, MA, NJ, NC)" appeared under Ashland.
  More generally, a pin is a ZIP-code centre, not the event's address. Events in the same ZIP stack at
  one point, and the directions link used those coordinates, so it sent people to the ZIP centre.
  (Verified against the dev database on 2026-11-01: events 36 and 48 share one point; event 8's
  coordinates were set by hand; event 35 sits in a different town's ZIP.)
- **Details are thin.** Description, price, eligibility, registration deadline, organizer, original
  source, and an accessible flyer were not reliably present.
- **Some controls did nothing visible** ("Get details", "See flyer", "Filters", "Profile" — as
  reported by the review; not yet reproduced in our own build).
- **No stable address.** Opening an event left the browser address at the homepage, so an event
  cannot be shared or bookmarked.
- **An unqualified trust claim.** The detail view says "Details are verified" with no stated source or
  date.

## 2. PROBLEM STATEMENT
A resident cannot tell *when* an event is, *where* it really is, or *how far* it is, and often cannot
act on it (register, add to calendar, share). Some listings are wrong in a way that costs trust
immediately, and the product claims a verification it has not defined. For a hyper-local product that
lives on word of mouth, one wrong pin or date does more harm than a missing event.

## 3. GOALS & SUCCESS METRICS
- **G1 — Unambiguous time:** every displayed event time identifies the calendar date fully.
  *Metric: no event card or detail shows a date without its day, month, and (when not the current
  year) year; start time is always shown or explicitly labeled "time not provided".*
- **G2 — Honest distance:** a distance appears only when it is known.
  *Metric: no card or detail shows a distance label without a number and unit.*
- **G3 — The place is a place:** an event appears on the map and in directions only when it has a
  specific, resolvable location; otherwise it is not pinned.
  *Metric: every pinned event has a single venue or address that resolves to a point; no event whose
  location text names several places is pinned; pins for different addresses in one ZIP do not
  coincide.*
- **G4 — Enough to act:** each event detail shows the facts a resident needs, or says they are missing.
  *Metric: price, eligibility, registration deadline, organizer, original source link, and flyer are
  each either shown or explicitly marked "not provided"; none is silently blank.*
- **G5 — Shareable and actionable:** each event has its own link and the standard actions.
  *Metric: every event opens at a unique URL in a fresh browser; share, add-to-calendar, directions,
  and register (when a registration link exists) each work.*
- **G6 — Trustworthy claims and correction:** any reliability statement is specific, and residents can
  report errors.
  *Metric: the detail view states the source and the last-checked date instead of a bare "verified";
  a resident can submit a correction report, and each report is stored.*
- **Counter-metrics (must not regress):**
  - *Never imply precision we lack.* Where a pin is only approximate, it is labeled approximate (P2's
    AC-P2-05 stays).
  - *Never show a dead control.* Metric: every visible button or link has an observable effect or is
    not shown.
  - *Never publish a location we could not resolve as if we had.* Metric: an unresolved location is
    shown as text only, without a pin or directions.

## 4. USER STORIES / USE CASES
Tag legend: `[core]` P2.2 must-have · `[stretch]` if cheap.

### Group A — Correct display
- **UF-P2.2-1 (clear date and time)** `[core]` — As a resident, I see each event's full date and time
  without guessing the month or year.
- **UF-P2.2-2 (honest distance)** `[core]` — As a resident, I see how far an event is only when Arangly
  actually knows.
- **UF-P2.2-3 (accurate place)** `[core]` — As a resident, I see an event on the map at its real venue
  or address, and "directions" takes me there; events with no single specific place are not pinned.
- **UF-P2.2-4 (clear counts)** `[core]` — As a resident, the result count reads correctly ("1 event",
  "2 events") and names what it counts.

### Group B — Detail and action
- **UF-P2.2-5 (everything I need to decide)** `[core]` — As a resident, I open an event and see its
  description, price, eligibility, registration deadline, organizer, original source, and flyer — or a
  clear note that an item was not provided.
- **UF-P2.2-6 (act on it)** `[core]` — As a resident, I can share an event on WhatsApp, add it to my
  calendar, get directions, and follow the registration link when there is one.
- **UF-P2.2-7 (a link for each event)** `[core]` — As a resident, I can copy, bookmark, or send the
  link to a single event and the recipient sees that event.
- **UF-P2.2-8 (every control works)** `[core]` — As a resident, every button I can see does something
  useful; unfinished ones are not shown.

### Group C — Trust
- **UF-P2.2-9 (honest reliability)** `[core]` — As a resident, I see where an event's details came from
  and when they were last checked, instead of an unexplained "verified".
- **UF-P2.2-10 (report a problem)** `[core]` — As a resident, I can tell Arangly that an event's details
  are wrong, so it can be fixed.

*Coverage: every UF has ≥1 AC in §14; every AC traces to a UF.*

## 5. SCOPE
**In scope (P2.2):**
- Date/time display rules and timezone clarity (UF-P2.2-1).
- Distance display rules (UF-P2.2-2).
- Location validation before mapping, and address-level placement for events that qualify
  (UF-P2.2-3). Includes the directions link using the address.
- Result-count wording (UF-P2.2-4).
- Event detail content and the "not provided" convention (UF-P2.2-5).
- Share on WhatsApp, add to calendar, directions, register (UF-P2.2-6).
- A unique, stable URL per event (UF-P2.2-7).
- Removing or fixing every control that has no effect (UF-P2.2-8). The review named Get details,
  See flyer, Filters, and Profile; the first two get fixed, the last two are fixed or hidden (OQ-P2.2-2).
- A specific reliability statement (source + last-checked) replacing the bare "verified" claim
  (UF-P2.2-9).
- A resident "report incorrect information" action and its storage (UF-P2.2-10).

**Out of scope (explicit):**
- **New filters** (free/paid, children's ages, registration-required) and **saved-events** improvements
  → a later P2.3 PRD.
- **Business offers as a separate kind** from events, with expiry and redemption conditions → later.
- **Finding and collecting events** (source registry, connectors, deduplication, change detection) → P3.
- **Moderation workflow** (review queues, edit history, restore) → a later curation phase. P2.2 only
  *stores* the correction reports; it does not build the screen to work them.
- **Organizer submission and self-service updates** → organizer-intake follow-up to P1.
- **Recurring events, rescheduling, cancellation status** → P3/curation (needs the data model for
  occurrences).
- Per-event timezones. A single configured application timezone stays for now (A3).
- WhatsApp conversational queries (P2 stretch), analytics, revenue, security hardening (P4).

## 6. SERVICE BOUNDARY
- **Owns (P2.2):** how event time, distance, place, and details are shown and validated for display;
  the location-resolution rule that decides whether an event is pinned; the event URL; the share /
  calendar / directions / register actions; the correction-report record and endpoint.
- **Reads:** published events and their stored metadata (P1/P2). Reads the town context from P2.1 only
  where an event URL needs it.
- **Does not own:** how events get into the system or become published (P1/P3), moderation of reports,
  or any notification to residents.
- Generality rule: extra detail fields (eligibility, deadline, organizer, source) and correction
  reports are modeled as **data** on the existing generic mechanisms where they fit, so a new detail
  kind is a new row, not new schema.

## 7. VALIDATIONS ON INCOMING DATA
Outcome legend: **ACCEPT** = store/act as given · **NORMALIZE** = accept after cleaning ·
**REJECT** = refuse with a visible error · **IGNORE** = accept silently without effect ·
**HOLD** = do not show on the map/directions (text only).

| # | Input | Rule | Outcome |
|---|-------|------|---------|
| V1 | Event location text | Names one venue or one street address that resolves to a point within the supported region | ACCEPT → pinned |
| V2 | Event location text naming several places ("Multiple…", a list of states or towns) or no place | Does not resolve to exactly one point | HOLD: shown as text, not pinned, no directions; flagged for curation |
| V3 | Event whose resolved point lies in a different town than its postal code or the town being browsed | Mismatch beyond the area rule | HOLD (as V2) and flag |
| V4 | Event start/end | Stored time must be a valid date-time; end not before start | Display per UF-1; invalid end is ignored for display (NORMALIZE) |
| V5 | Event price text | Free text, ≤200 chars | ACCEPT; shown verbatim; absent → "not provided" |
| V6 | Registration / source link | Absolute `http`/`https` URL, ≤2048 chars, no credentials in the URL | ACCEPT; otherwise treated as absent; opens in a new tab with no referrer leak |
| V7 | Flyer reference | Resolves to an image the app serves | ACCEPT; absent → "not provided" |
| V8 | Correction report | Event id exists and is published; reason from a fixed list; free text ≤1000 chars; optional contact email valid if given | REJECT invalid (400); NORMALIZE (trim); a report on a non-published event → REJECT (404) |
| V9 | Correction report rate | Baseline per-client limit; full hardening is P4 | REJECT excess with 429 |
| V10 | Event URL identifier | Must identify a published event | Unknown, unpublished, or malformed → a "this event isn't available" page (404 behavior), never another event |
| V11 | Calendar export content | Title, start, end (if known), location text, and event link only | Free text from the event is escaped; no personal data |

## 8. DEPENDENCIES & INTEGRATIONS
- **P2 (built):** list, map, detail, `/api/events/{id}`, the dev database with real events.
- **Address geocoding (NEW, undecided):** turning a venue or address into a point needs a geocoding
  source. Candidates and their terms are **not yet verified**: a hosted geocoding service (cost, usage
  limits, whether results may be stored/cached), or a self-hosted dataset. Public OpenStreetMap
  geocoding has a restrictive usage policy that discourages application use. See OQ-P2.2-1 and A5.
- **Existing ZIP centroid data:** remains the fallback, labeled approximate.
- **P1 publish gate:** location validation (V2/V3) may need to change what the publish step accepts;
  how a HOLD event is flagged is a curation question (OQ-P2.2-2).
- **Calendar and share:** standard calendar file format and a WhatsApp share link; no third-party SDK
  required.
- **Event page addressing:** follows the P2.1 rule: a path names the resource, so an event URL is a path under a fixed prefix (for example `/events/<id>`) and view options stay as query parameters. The SDD decides the exact form.

## 9. CONSTRAINTS
- **Honesty:** nothing is displayed as verified, precise, or complete unless it is, and the screen says
  so when it is not. This is the through-line of the whole PRD.
- **Privacy:** a correction report may carry an optional contact email — personal data. Same care as
  P2.1 (not logged at INFO, not public). The report endpoint never returns another person's data.
- **Generality:** detail fields, reliability statements, and reports are data; adding a field kind
  must not need a new table (see §6).
- **Accessibility:** flyers have alt text derived from the event title and date; actions are keyboard
  operable and labeled; success/error messages are announced.
- **Mobile first:** every action works on a phone-width screen (the review recommended mobile testing).
- **Environments:** developed and tested on dev (:8081, `arang_dev`); prod (:8080) only by explicit
  release (see `.kiro/steering/environments.md`).

## 10. CONCEPTS
- **Specific location:** exactly one venue or street address that can be resolved to a point.
- **Pinned event:** an event shown on the map. Only events with a specific location are pinned.
- **Approximate pin:** a pin placed at a ZIP centre because no address point exists; labeled
  approximate. Whether approximate pins stay allowed once address placement exists is OQ-P2.2-1.
- **Held event (HOLD):** a published-or-publishable event whose location is not specific: visible as
  text, never pinned, never given directions.
- **Reliability statement:** "Source: <name or domain> · Last checked: <date>" — what Arangly actually
  knows about where a detail came from and how recent it is.
- **Not provided:** the explicit label shown for a missing detail, instead of a blank.
- **Correction report:** a resident's note that an event's details are wrong, with a reason and
  optional text and contact.
- **Event URL:** the stable address of one event.

## 11. ASSUMPTIONS
- **A1 [ASSUMPTION]:** The review's observations are accurate for the live site, but they are a
  snapshot and not all reproduced on our dev build (the dead-control report especially). The SDD's
  first task is to reproduce each before fixing it.
- **A2 [ASSUMPTION]:** "Last checked" can be derived from data we already keep (the event's last
  update or enrichment time) until P3 records real fetch times. Slightly weaker than the checklist's
  "last successful fetch".
- **A3 [ASSUMPTION]:** A single application timezone (America/New_York) continues to apply; event times
  are shown in it, with the zone abbreviation (e.g. "EDT") on the detail view. Per-event timezones
  and daylight-saving edge tests move to P3/curation.
- **A4 [ASSUMPTION]:** Description, eligibility, deadline, organizer, and source are stored on the
  existing generic per-event key/value mechanism; the extraction step is asked to populate them where
  it can. Existing events will mostly read "not provided" until re-extracted or edited.
- **A5 [ASSUMPTION — unverified]:** A geocoding source that permits storing results exists at
  acceptable cost for this volume (tens of events, not thousands). Not yet checked.
- **A6 [ASSUMPTION]:** Share-on-WhatsApp is a link that opens WhatsApp with prefilled text (no API
  integration), and "add to calendar" is a downloadable standard calendar file.
- **A7 [ASSUMPTION]:** The "Profile" and "Filters" controls named in the review are not part of P2's
  shipped scope; hiding them is acceptable until their feature exists.

## 12. OPEN QUESTIONS
| ID | Question | Status | Recommended default | Owner |
|----|----------|--------|---------------------|-------|
| OQ-P2.2-1 | **How do we place events precisely?** (a) geocode the venue/address to a point with a hosted service and store the result; (b) keep ZIP-centre pins but fix directions to use the address and spread overlapping pins; (c) both, with (b) as the fallback when (a) fails. And: do approximate pins stay allowed, or is an unresolved event simply unpinned? | OPEN | (c): geocode and store the result (cached forever, re-done only on address change), fall back to an *approximate*-labeled centre pin when geocoding fails, and never pin an event that fails V2/V3. Provider choice waits for the terms check (A5) in the SDD. Needs your call because it adds a paid/third-party dependency. | driver |
| OQ-P2.2-2 | **What happens to an event that fails the location rule (V2/V3), and to controls we cannot finish?** Hide it from residents, show it as text with no pin, or hold it for review? And for dead controls: fix, or hide until the feature exists? | OPEN | Show as text, no pin, no directions (the HOLD outcome), and flag it for curation. Hide Filters and Profile until their features exist; fix Get details and See flyer. The review queue itself waits for the curation phase. | driver |
| OQ-P2.2-3 | **What may we say about reliability?** Allow "verified" only after a defined check exists, and until then show the source and last-checked date only? | OPEN | Remove "verified" now; show "Source · Last checked". Reintroduce a verification mark only when curation can back it (moderation phase). | driver |

## 13. PRE-SIGN-OFF VERIFICATION
*(Unchecked — ticking is the driver's act at sign-off.)*
- [ ] One coherent intention (event correctness, completeness, and trust); filters, ingestion,
      moderation, organizer intake, and offers are kept out.
- [ ] Every UF has ≥1 AC and every AC traces to a UF.
- [ ] OQ-P2.2-1..3 are resolved or deferred with an owner.
- [ ] The geocoding source's terms (storage, cost, limits) are verified before the SDD commits (A5).
- [ ] Each review observation is reproduced on our build before a fix is scoped (A1).
- [ ] The honesty counter-metrics (no false precision, no dead control, no false pin) each have an AC.
- [ ] The boundary with P3 (provenance, recurring events) and the curation phase is acceptable.

## 14. ACCEPTANCE CRITERIA
- **AC-P2.2-01** ⟵ UF-P2.2-1: Every event card and detail shows the weekday, day, and month; the year is
  shown when it is not the current year. An event with a start time shows it with the timezone
  abbreviation on the detail view; one without shows "time not provided". An end time is shown only
  when valid (V4).
- **AC-P2.2-02** ⟵ UF-P2.2-1: Events on the first and last day of the displayed window, events starting
  at 00:00 and 23:59 local time, and events on a daylight-saving change day each appear in the correct
  window and on the correct date.
- **AC-P2.2-03** ⟵ UF-P2.2-2: A distance label appears only when a distance is known and always includes
  a number and unit. When unknown, no distance label or empty "away" text appears on cards or details.
- **AC-P2.2-04** ⟵ UF-P2.2-3: An event whose location text resolves to exactly one point (V1) is
  pinned at that point. Two events at different addresses in the same ZIP are pinned at different
  points. An event whose pin is only approximate is labeled approximate.
- **AC-P2.2-05** ⟵ UF-P2.2-3: An event whose location text names several places, names none, or whose
  point falls in a different town than expected (V2, V3) is shown as text only: no pin and no
  directions action. It is not shown under a town it does not belong to.
- **AC-P2.2-06** ⟵ UF-P2.2-3: The directions action opens a maps search for the event's address or
  venue text when it exists, and uses coordinates only when no address text exists.
- **AC-P2.2-07** ⟵ UF-P2.2-4: The result summary uses the correct singular and plural ("1 event",
  "2 events") and states what it counts; a count of zero reads as "no events".
- **AC-P2.2-08** ⟵ UF-P2.2-5: The event detail shows description, price, eligibility, registration
  deadline, organizer, source link, and flyer. Each is either shown or labeled "not provided"; none
  is blank. A flyer image has descriptive alternative text.
- **AC-P2.2-09** ⟵ UF-P2.2-6: "Share on WhatsApp" opens WhatsApp with a message containing the event
  title, date, and the event's own URL. "Add to calendar" downloads a calendar file that opens in a
  standard calendar with the right title, start, end (if known), location text, and event URL (V11).
- **AC-P2.2-10** ⟵ UF-P2.2-6: When the event has a valid registration link (V6), a "Register" action
  opens it in a new tab. When it has none, no register action is shown.
- **AC-P2.2-11** ⟵ UF-P2.2-7: Each published event is available at its own URL. Opening it in a fresh
  browser shows that event. The browser address changes when an event is opened from the list. An
  unknown, unpublished, or malformed event URL shows an "event isn't available" page (V10) and never
  another event.
- **AC-P2.2-12** ⟵ UF-P2.2-8: Every visible button and link on the list, detail, and top bar produces an
  observable result when activated (navigation, a panel, a download, or a message). Any control that
  cannot yet do so is not displayed. This is checked on a phone-width and a desktop-width screen.
- **AC-P2.2-13** ⟵ UF-P2.2-9: The event detail shows "Source: <source> · Last checked: <date>" and does
  not show the word "verified" unless a verification record exists for the event (OQ-P2.2-3).
- **AC-P2.2-14** ⟵ UF-P2.2-10: A "Report incorrect information" action on the detail view accepts a
  reason from a fixed list, optional text, and an optional contact email, and stores a report linked to
  the event with the time. The visitor sees a confirmation only after the report is stored.
- **AC-P2.2-15** ⟵ UF-P2.2-10: An invalid report (V8) shows an error and keeps the entered text; a
  report on an unpublished or unknown event is refused; reports over the rate limit get a 429 and a
  "try again shortly" message. A stored contact email never appears in a public response, a URL, or an
  application log line at INFO level or above.

## 15. VERSION HISTORY
- **v0.1 (2026-11-01) — DRAFT (Fast path).** First draft. Origin: the 2026-11-01 site-review checklist
  plus the driver's report that coordinates do not match addresses. Verified before drafting, against
  the dev database copy of prod: pins are ZIP-centre (events 36 and 48 coincide), event 8's coordinates
  were set by hand, and event 35's location text names several states while its ZIP is another town's.
  Also verified: the directions link preferred coordinates over the address (fixed in code on
  2026-11-01, uncommitted at the time of writing). Not verified: the review's dead-control reports and
  its live-site observations on our own build (A1). Scope deliberately excludes filters, offers,
  ingestion, moderation, and organizer intake (§5). Three open questions (OQ-P2.2-1..3) need the driver.
  No code written beyond the directions-link fix.
- **v0.2 (2026-11-01) — DRAFT.** Added the "Notes for the SDD" appendix below (non-binding) to keep the
  geocoding design discussion. No change to requirements.

---

## Appendix — Notes for the SDD (NON-BINDING hand-off; not requirements)
> **Why this is here:** these notes keep a design discussion from 2026-11-01 so it is not lost. They are
> recommendations for the SDD to confirm or change. They do not change any acceptance criterion or open
> question. The P2.1 PRD has the full version of the place-data notes; read it first
> (`docs/p2.1-town-entry/prd.md`, "Appendix — Notes for the SDD").

**Populate process.** The step-by-step build, validation, publish, deploy, and rollback process for the SQLite file is written once, in the P2.1 PRD appendix ("How the SQLite file is populated"). P2.2 reuses it and only adds the address and venue tables, plus the address checks in step 5.

**What P2.2 adds beyond P2.1:** address and venue placement (OQ-P2.2-1). The same offline-built, read-only
**SQLite** file approach can carry it, with extra tables built from the OpenStreetMap extract:
street addresses (street, house number, ZIP, point) and named venues (temples, libraries, halls, parks).

**Recommendations to confirm in the SDD:**
1. **Geocode once, when an event is published or its location text changes.** Store the resulting point on
   the event in the main database (MySQL). Do not look it up on each page view. Never write to the SQLite file.
2. **Matching logic is ours to write:** normalize street types ("St" / "Street"), numbers, and ZIPs; try an
   exact address match first, then a venue-name match inside the event's town. If neither matches, do not
   pin the event, or use the ZIP-centre pin labeled approximate (OQ-P2.2-1 decides which).
3. **Reject, do not guess:** location text that names several places ("Multiple locations", a list of
   states) must fail the "specific location" rule (validations V2 and V3) before any lookup.
4. **Coverage test before commitment:** run real events through the lookup (for example the Sri Lakshmi
   Temple at 117 Waverly St, Ashland) and record the hit rate. OSM house-number and venue coverage varies.
5. **Escalation path:** if hit rate is too low on messy flyer text, move to a self-hosted Nominatim or a
   paid geocoder behind the same small interface. Keep the geocoder swappable.
6. **License:** OSM data is ODbL. Attribution is required. Share-alike duties for a derived database and
   for stored results are not yet confirmed. Read the license before launch.
7. **Public Nominatim is not suitable for ingestion-scale or type-ahead use** (1 request per second cap,
   caching required, autocomplete forbidden). It could serve a very small, cached, user-triggered volume
   only if the policy is followed.
