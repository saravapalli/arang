---
title: Arang — Initiative Overview (WhatsApp Event Intake & Weekend Discovery)
type: Initiative
artifact_id: ARANG-INIT-001
status: draft
version: 0.10
driver: (you)
approver:
approver_role:
assigned_date: 2026-10-11
first_draft_target:
last_revised_date: 2026-10-12
jira:
multifeature: true
related_docs:
  - docs/producer-intake/prd.md (child PRD — P1, the build-first feature)
  - docs/prd/consumer-discovery.md (child PRD — P2, planned)
  - docs/prd/ingestion-framework.md (child PRD — P3, planned)
  - docs/reference/how-spec-driven-ai-works.md (methodology)
origin: Resumed product-spec workflow; split into an initiative + child PRDs per right-sizing rule
informed:
review_files:
tags: [whatsapp, event-intake, ocr, discovery, spring-boot, initiative]
---

# Arang — Initiative Overview

> **This is the INITIATIVE-level overview**, not a buildable PRD. Per the spec-driven
> right-sizing rule (one PRD = one coherent intention), Arang's scope is split into
> child feature PRDs under `docs/prd/`. This document holds the shared vision, product
> shape, generality principles, schema intent, and the cross-feature priority ordering.
> The buildable, right-sized specs live in the child PRDs — start with
> `docs/producer-intake/prd.md` (P1).
>
> Section structure follows the FT-1 PRD template. SERVICE BOUNDARY and VALIDATIONS ON
> INCOMING DATA are intentional additions kept because Arang is a service with
> OCR/extraction quality concerns.

## 1. CONTEXT
Arang is for people who want to know what activities they can do on a weekend. It is **two-sided**:
- **Producer side:** events enter Arang through one or more **intake sources**:
  - *(human)* a user submits an event over WhatsApp — free text, a photo of a flyer/poster, a link, or a combination.
  - *(automated, later)* pipelines ingest events from published local sources — newspapers, town bulletins, community boards, listings pages — by OCR'ing/parsing them into the same event records.
  Arang extracts structured event information (including OCR on images) regardless of which source it came from.
- **Consumer side:** another user discovers weekend events matched to their interests/choices (category, location, etc.).

The **intake source is data, not schema** (see `.kiro/steering/design-philosophy.md`): WhatsApp and newspaper-pipeline are two sources of the same `event` entity. v1 is built one usable slice at a time — WhatsApp producer intake first; automated local-source ingestion and interest-based discovery are later slices. The schema is designed up front to cover the full vision (see Section 10).

## 2. PROBLEM STATEMENT
**Today:** there is no low-friction way for ordinary people to contribute local weekend events, and no simple personalized way to find them. Flyers, posters, and links live in chat threads and get lost.
**Gap:** capturing an event today means manual data entry; discovering one means scrolling scattered sources with no personalization.
**Desired future state:** anyone can forward an event to a WhatsApp number and have it become a structured, correctable record; anyone can ask Arang what to do this weekend and get results filtered to their interests.

## 3. GOALS & SUCCESS METRICS
- **G1 — Frictionless intake:** a user can submit an event via WhatsApp (text, image, or link) and get a confirmation with the extracted fields. *Metric: a submitted message produces a structured record + a confirmation reply.*
- **G2 — Usable extraction:** structured fields (title, time, location, purchase link) are extracted from text and from flyer images via OCR. *Metric: for a clear flyer, title + at least one of date/location is populated.*
- **G3 — Correctability:** the submitter can fix mistakes without accounts. *Metric: the submitter can open a private edit link and save a correction that persists.*
- **G4 — Durable, broad schema:** the data model covers the full vision (producer + consumer + interests) without needing migrations for foreseeable use cases.
- **G5 (later slice) — Interest-based discovery:** a consumer can ask for weekend events and receive results filtered by stated interests.
- **G6 (later slice, essential for scale) — Multi-source ingestion framework:** pluggable connectors (inbound push API, outbound pull API, structured/unstructured web, documents/PDF, images, compliance-gated messaging/social) feed one shared normalize→extract→dedupe→store pipeline producing `event` records with provenance. *Metric: two different connector shapes can each produce event records attributed to their source through the same pipeline.*
- **G7 (later slice) — Inbound events API:** external systems can POST structured events to Arang and have them stored. *Metric: a valid POST creates an event record attributed to the calling client.*

## 4. USER STORIES / USE CASES
> At the initiative level these are grouped into **epics by feature/priority**, each
> mapping to a child PRD where the detailed, right-sized `UF-###`/`AC-###` flows live.
> Tag legend: `[P1]` build-first · `[P2]` later · `[P3]` lowest priority · `[P4]` hardening (NFRs), after a working P1 · `[P5]` business & venue linking.

### Epic 1 — Producer intake `[P1]` → `docs/producer-intake/prd.md`
- As a submitter, I send an event over WhatsApp (text / flyer image / link) and get a confirmation echoing what was captured.
- As the submitter, I open a private link to review, correct, submit (publish), or delete the event.
- As a submitter, I can type an event into a web form instead of WhatsApp. `[P1 stretch]`
- As an operator, every event records its origin and creation time.
*(Detailed flows UF-001…UF-009 are specified in the child PRD.)*

### Epic 2 — Conversational intake `[P2]` → (future child PRD)
- As a submitter, when a field is missing Arang asks a follow-up question and merges my answer into the pending event. *(Requires conversation state.)*

### Epic 3 — Consumer discovery & interests `[P2]` → `docs/prd/consumer-discovery.md`
- As a consumer, I ask "what's on this weekend?" and get events filtered to my interests (category/location).
- As a consumer, I tell Arang what I like and it personalizes results.

### Epic 4 — Multi-source ingestion & provenance `[P3]` → `docs/prd/ingestion-framework.md`
- As an operator, I configure a source connector (newspaper listings, platform API, PDF feed, …) and Arang periodically ingests published items into event records attributed to that source.
- As an external integrator, I POST structured events to Arang's inbound API and they are stored, attributed to my client.
- As an operator/consumer, I can tell where an event came from (user / ingestion source / api client, and when) and how confident the extraction was.

### Epic 5 — Hardening / non-functional requirements `[P4]` → (future child PRD)
> Quantitative NFRs are **deliberately deferred** out of the P1 prototype (which captures only
> the correctness-critical operational NFRs: durability, webhook verification, idempotency,
> token expiry, cleanup). This epic is where measurable targets and their tests are set **once
> there is real usage to measure against** — not guessed for a single-user tunnel prototype.
- As an operator, the system meets stated **performance** targets (e.g. reply/review-page/webhook
  latency, OCR processing time) under expected load.
- As an operator, the system meets **availability** and **throughput/scalability** targets
  (uptime, concurrent users, events/day).
- As an operator, I have **observability** — structured logging, metrics, tracing, and alerting.
- As an operator, the system is **hardened**: token-entropy/rate-limiting, PII handling of phone
  numbers, media-storage security, and public (non-network-restricted) exposure — superseding the
  P1 deferrals OQ-P1-3 (anti-abuse) and OQ-P1-7 (real authn/authz).
*(Detailed NFR targets + `AC-###` are specified when this child PRD is drafted.)*

### Epic 6 — Business & venue linking `[P5]` → `docs/business-linking/prd.md`
> The `business`/`business_metadata` tables and the nullable `event.business_id` FK already exist
> (created in the P1 SDD per the generality steering) but no phase populates them. This epic gives
> that entity a feature home: derive/create a business from a submission, link events to it, dedupe
> repeats, and capture business details — so events can be grouped by who runs them.
- As a producer submitting an event that names a venue/organizer, my event is associated with a
  business so it's grouped with that business's other events.
- As the platform, repeated submissions naming the same venue link to one business (dedupe), not
  duplicates.
- As a producer on the review page, I can see and correct the linked business.
*(Detailed flows UF-P5-1…UF-P5-6 + `AC-###` are in the child PRD. Business-owner accounts/auth and
consumer venue pages are explicitly out — they ride on the auth work and P2 discovery.)*

## 5. SCOPE
**Priority ordering (explicit — decided by the user):**
1. **P1 — WhatsApp one-shot intake (slice 1):** a WhatsApp user sends a message → extract → store → confirmation reply + private edit link. This is the first running app and the top priority.
2. **P1 — Edit UI (slice 2):** the token-authorized edit page.
3. **P1 — Direct-entry UI (slice 3):** a simple web form as a *second producer channel* — a user types an event directly. Same create/extract back-end; origin is still `user` (no WhatsApp).
4. **P2 — Conversational intake (later):** stateful multi-turn capture where Arang asks follow-up questions to fill missing fields, merging answers into a pending event. Distinct from one-shot capture; requires conversation state.
5. **P2 — Consumer discovery + interests (later).**
6. **P3 — Multi-source ingestion framework (lowest priority).** Everything in the connector list below. Essential for scale, but after the producer and consumer loops work.

**In scope — P1 (v1 first slices):**
- Producer intake over **WhatsApp**: text (slice 1), then flyer image + OCR, link capture.
- Extraction of structured event fields + confirmation reply with a private edit link.
- A token-authorized **edit page** (edit only the event tied to your link).
- A **direct-entry web form** (second producer channel; same back-end).

**In scope — P2 (schema-covered now, built later):**
- **Conversational intake** (follow-up questions to complete an event). Requires per-conversation state (the `conversation`/`message` trail in the schema supports this).
- Interest-based weekend discovery for consumers.

**In scope — P3 (lowest priority; schema-covered now, built later):**
- **Multi-source ingestion framework** — a pluggable set of **connectors** feeding one shared **normalize → extract → dedupe → store** pipeline that produces the same `event` records with source provenance. Defined by **source shape**, not brand, so new sources are new connectors (no schema/pipeline change). Connector shapes:
  - **Inbound Push API** — external systems POST structured events to Arang's API. *(Lowest effort, no scraping, no ToS risk; the recommended first ingestion connector when P3 begins.)*
  - **Outbound Pull API** — Arang calls an external platform's API on a schedule.
  - **Structured web** — pages with schema.org/JSON-LD event markup; fetch + parse.
  - **Unstructured web / JS calendars** — fetch (possibly headless) + parse or LLM-assist.
  - **Documents** — newspaper PDFs, newsletters; fetch/receive + OCR + parse.
  - **Images** — flyers/posters; OCR + parse (same extractor as WhatsApp image intake).
  - **Messaging/social** — WhatsApp groups, Facebook, Instagram. **Compliance-gated:** official APIs expose limited data with app review; scraping these violates platform terms and is **not assumed buildable**. Requires legal review and a platform-compliant connector, or is dropped.
- Generalized **source/provenance** (an event's origin is a user, an ingestion source, or a push-API client), modeled in the schema from day one.

**Explicitly deferred / to decide for the P3 ingestion framework:**
- Per-connector legal/ToS review (mandatory before building any connector that reads a third-party source; blocking for messaging/social).
- Scheduling/orchestration of pipeline runs; per-connector parsers; newsprint/PDF OCR tuning; headless-browser rendering for JS calendars.
- **De-duplication** across connectors (same event seen via WhatsApp + newspaper + a platform API) — a first-class concern once multiple sources exist.
- Authentication/rate-limiting/abuse controls for the inbound Push API.

**Out of this version:**
- A public browsing/discovery web UI (discovery arrives via WhatsApp first).
- Accounts/login with passwords or OTP (v1 uses per-event edit tokens).
- De-duplication, moderation/approval, recurring events, multi-language extraction (English-only v1), fetching/reading link page contents, paid WhatsApp template/marketing messages.

## 6. SERVICE BOUNDARY
- **Arang service** owns: the WhatsApp webhook (receive), extraction (OCR + parsing), persistence (the event/record store), the confirmation reply, and the edit page.
- **External — WhatsApp Business Cloud API (Meta):** delivers inbound messages to Arang's webhook and sends Arang's outbound replies. Arang does not own message transport.
- **External — tunnel (ngrok/cloudflared):** exposes the local webhook over HTTPS during the prototype. Not part of the app; an operational dependency.
- **External — local sources (later):** newspapers/town bulletins/listings pages that automated pipelines read. Arang owns the ingestion pipeline (fetch → OCR/parse → extract → store) but not the sources themselves; access to each is subject to that source's terms.
- **Write path (human):** WhatsApp → webhook → extract → store → reply. **Write path (automated, later):** scheduled pipeline → fetch source → OCR/parse → extract → store (no reply; attributed to an ingestion source). **Read/edit path:** edit link → edit page → update store. **Consumer read path (later):** WhatsApp query → filtered event search → reply.

## 7. VALIDATIONS ON INCOMING DATA
- **V1:** A text message with any content produces a record with `raw_message` populated.
- **V2:** An image message is fetched and OCR'd; if OCR yields no usable text, the record is still created with a confidence flag and the reply says the image was unreadable.
- **V3:** Any URL present in the message is captured verbatim as a **labeled attribute** `(label, value, kind=url)`, where `label` is the raw source text near the URL as it appeared in the OCR/message (e.g. "Tickets", "RSVP", "Register", "Instagram"). URLs are stored only, not fetched. Meaning is interpreted later (by an LLM) from the raw label; Arang does not force a URL into a fixed `purchase_url` field.
- **V3b:** Other labeled data OCR'd from the source (e.g. "Price: $20", "Ages 18+", "Dress code: formal") is likewise captured as `(label, value, kind)` attributes, preserving the source label verbatim.
- **V4:** If no title can be determined, the record is stored with the gap flagged (`extracted_confidence`) rather than rejected (confirm vs. reject in D5 once schema is final).
- **V5:** Edit submissions are rejected unless the request carries a valid `edit_token` matching the target event.
- **V6:** `submitted_by` (sender WhatsApp id) and `created_at` are always set on create.

## 8. DEPENDENCIES & INTEGRATIONS
- **WhatsApp Business Cloud API** (test mode): free test number, up to 5 verified recipients, $0. Compliant path (no unofficial WhatsApp-Web libraries).
- **OCR engine:** Tesseract via Tess4J (open source, local, $0) — for slice 2.
- **Spring Boot** (Java) application; build tool and exact versions TBD at design time.
- **Database:** **MySQL** (decided). Runs locally via Docker, $0. (Postgres was considered for JSONB/enums but MySQL was chosen; MySQL 8 JSON columns cover the metadata escape-hatch need.)
- **Tunnel:** ngrok or cloudflared for the public HTTPS webhook during local hosting.
- **(Later) local LLM** (optional) for higher-quality extraction/discovery — only if we choose to upgrade from rule-based extraction.

## 9. CONSTRAINTS
- Minimal cost: prototype must run at ~$0 (local hosting + free tiers).
- Local hosting on the user's machine; webhook reachable only while machine + tunnel are up (accepted for prototype).
- Stack is **Java + Spring Boot** (decided).
- WhatsApp via official Cloud API test mode (decided).
- English-only extraction for v1 (decided).
- No accounts/passwords in v1; editing authorized by per-event token (decided).

## 10. CONCEPTS
> Generality principle (see `.kiro/steering/design-philosophy.md`): the **event is the universal unit**; its **kind is data, not schema**; links to other entities are **optional and type-driven**. New use cases become new data (rows), not new tables/columns.

- **Event (universal primary entity):** "a thing that can appear in a consumer's weekend feed." Covers concerts, offers/deals, workshops, festivals, classes, and future kinds not yet imagined. Typed core fields queried on nearly every request: `id`, `event_type_id` (FK), `business_id` (FK, **nullable**), `title`, `starts_at`, `ends_at` (also serves as a validity window for offers), `location`, `source_type` (text|image|link|mixed), `raw_message`, `media_ref`, `extracted_confidence`, **origin fields (see Provenance)**, `edit_token`, `created_at`, `updated_at`. (Required-vs-optional finalized in D5.)
- **Provenance / origin (generalized):** every event has an origin that is **a human submitter, an automated ingestion source, or an inbound API client** — the "optional, type-driven link" pattern. Model origin as: `origin_type` (`user` | `ingestion_source` | `api_client`) plus the appropriate FK (`submitted_by` → `user`, or `ingestion_source_id` → `ingestion_source`, or an API client id). WhatsApp submissions set `origin_type=user`; pipeline events set `ingestion_source`; pushed events set `api_client`. Only `user`-origin events have an `edit_token` holder.
- **Ingestion source (primary entity, later):** a configured source a connector reads. Fields (illustrative): `id`, `name`, `connector_shape` (push_api | pull_api | structured_web | unstructured_web | document | image | messaging_social), `kind`/town/`location`, `url_or_reference`, `last_run_at`, `enabled`, plus per-entity metadata for connector-specific config. New sources are new rows; new connector *shapes* are new code behind a stable interface.
- **Connector + pipeline (architecture, later):** a **connector** acquires raw candidate events from one source shape and normalizes them to candidate fields; all connectors feed one shared pipeline: **acquire → normalize → extract → de-duplicate → store (with provenance)**. The back half (extract/dedupe/store/`event`) is identical across sources; only the acquire/normalize front half differs per connector. Adding a source type = implementing the connector interface, not changing the pipeline or schema. Messaging/social connectors are **compliance-gated** (official APIs or legal review; scraping assumed non-viable).
- **Event type (lookup):** `event_type` row names the kind (`concert`, `offer`, `workshop`, …). A new kind is an INSERT, never a migration. The type may govern which optional links/fields are meaningful (e.g. an `offer` is typically tied to a `business`; a user-submitted gig may not be).
- **Business (primary entity):** a persistent entity (a venue, shop, organizer). An event may optionally belong to a business (`event.business_id` nullable) — e.g. a shop publishing a weekend deal, or a venue hosting a concert. Businesses are not events; they relate to events by type-driven optional links.
- **Event attribute (labeled, flexible):** everything else captured from the source is stored as per-entity key/value metadata `(label|key, value, kind)` attached to the event, where:
  - `label/key` = the raw text as it appeared in the OCR/message (e.g. "Tickets", "RSVP", "Price", "Ages", "Instagram"). **Not normalized at capture time.**
  - `value` = the captured content (a URL, a string, a number-as-text).
  - `kind` = a light type hint (`url` | `text` | `date` | `number` | …). Duplicate keys allowed.
  This is where **all URLs** live — there is no fixed `purchase_url` column. A "buy tickets" link is just `("Tickets", url, url)`. Capture is lossless; **interpretation is deferred** to serve time, where an LLM reads the raw labels and decides meaning per consumer need. The same per-entity metadata pattern repeats for other entities (`user_metadata`, `business_metadata`, …).
- **User identity vs. role:** `user` is **identity only** (id, phone, name, timestamps). A user's roles (**producer**, **consumer**, later operator/business_owner/api_client/moderator…) are modeled as a many-to-many **`user_role`** `(user_id, role)` — role is *data, not schema*, so one person can be producer AND consumer and new user kinds are new role values, never a migration. The `user_role` table is designed into the schema up front but only populated/enforced when a feature needs it (P1 has producers only).
- **Submitter / producer:** a `user` identified by WhatsApp phone number (`submitted_by`); "producer" is a role, not a separate table.
- **Consumer:** a `user` discovering events; may have **interests** (weighted by category/tag/location). "Consumer" is a role. The weekend feed is a blend over events of multiple types, joined to businesses when present, filtered by interests.
- **Authentication & authorization (deferred, platform-wide):** real **user** authn/authz is a known future piece, **not built in P1/early phases**. When added, it gates on `user_role`. Until then, access to privileged surfaces (e.g. the review/edit page) is additionally guarded by **network restriction** (not publicly exposed; behind VPN/VPC or localhost). The per-event `edit_token` is an **object-level capability** (it authorizes actions on one event) and serves as P1's edit access control — but it is **not** user-level authentication (no login/roles/cross-device identity).
- **Edit token:** unguessable per-event secret delivered in the WhatsApp reply; authorizes review/edit/submit/delete on one event (no login). It is P1's edit access-control mechanism, not a substitute for user authn/authz (see above).
- **Schema design intent (durable artifact):** typed relational core (users, events, event_type, businesses, per-entity metadata, categories, tags, interests, feedback, conversation/message trail). Three storage strategies — typed columns (filtered/sorted data), per-entity key/value metadata (unpredictable extras), lookup/join tables (shared vocabulary: event_type, category, tag). Broad and future-proof to avoid migrations; explicitly **not** fully-generic EAV for the *core*. Engine **MySQL**, build tool **Gradle** (decided). Reference schemas `samro`/`artha` are **idea sources only**; no code reused.

## 11. ASSUMPTIONS
- **A1:** v1 success = a correctly structured record in the datastore + a confirmation reply; verified via the datastore and the WhatsApp reply (no browsing UI needed to confirm).
- **A2:** One message = one event (not a batch).
- **A3:** Images are photos of printed flyers/posters, not handwriting.
- **A4:** It's acceptable that the service only receives messages while the machine and tunnel are running.
- **A5:** The submitter who holds the edit link is the legitimate editor (WhatsApp thread = proof of ownership).
- **A6:** Consumer interests for the MVP are explicitly stated, not learned, in the first discovery slice.

## 12. OPEN QUESTIONS

| # | Question | Status | Note / default | Owner |
|---|---|---|---|---|
| D5 | Required fields — mandatory vs. optional at the schema level | OPEN | Finalize with the schema; placeholder = store-with-gap-flagged (V4), not reject | driver |
| OQ-Ingest-1 | Is any ingestion in the first prototype? | OPEN | Default no; schema covers it. Exception candidate: inbound Push API (cheap, ToS-safe) | driver |
| OQ-Ingest-2 | Per-connector legal/ToS review before building | OPEN | Messaging/social (WhatsApp groups, FB, IG) compliance-gated — official APIs only, pending review, or dropped | driver |
| OQ-Ingest-3 | De-duplication strategy across connectors | OPEN (deferred) | First-class once >1 source exists | driver |
| OQ-Ingest-4 | Inbound Push API contract, auth, rate-limiting | OPEN (deferred) | Design when that connector is built | driver |

**Resolved decisions (log):** DB engine = **MySQL**; build tool = **Gradle**; D1 plain OCR/Tess4J; D2 corrections via review/edit UI; D3 URLs as labeled `(label,value,kind=url)` attributes (stored only, interpreted later by LLM — no fixed `purchase_url`); D4 printed-only (no handwriting); D6 English-only; D7 edit-token authorization; D8 review/edit UI in MVP; D9 official Cloud API test mode. *(Previously OQ-Engine/OQ-Build, now closed.)*

## 13. PRE-SIGN-OFF VERIFICATION (initiative-level)
Confirm the cross-feature framing (per-feature sign-off happens in each child PRD):
- [ ] Two-sided product framing (producer + consumer) is correct.
- [ ] Priority ordering P1 → P2 → P3 is correct.
- [ ] The generality model (event-as-unit, `event_type`, optional `business`, origin-as-data) is endorsed.
- [ ] Schema-first, broad-schema design intent is endorsed.
- [ ] The split into child PRDs (producer-intake / consumer-discovery / ingestion-framework) is accepted.
- [ ] Initiative open question D5 (required fields) can be settled with the schema.

## 14. ACCEPTANCE CRITERIA (initiative-level)
> Detailed, testable `AC-###` criteria live in each child PRD (e.g. `docs/producer-intake/prd.md`
> defines AC-001…AC-022). At the initiative level the success criteria are coarse-grained
> and measured by child-PRD completion:
- **IC-1 (Producer):** `producer-intake` (P1) is approved, built, and its ACs pass — an event can be submitted, reviewed, published, and persisted.
- **IC-2 (Consumer):** `consumer-discovery` (P2) delivers interest-filtered weekend discovery (criteria defined in that PRD).
- **IC-3 (Ingestion):** `ingestion-framework` (P3) delivers ≥1 connector through the shared pipeline with provenance (criteria defined when that sub-initiative is drafted).
- **IC-4 (Schema durability):** the schema absorbs P1–P3 without a migration to a primary table (validates the generality goal).
- **IC-5 (Hardening/NFRs):** `hardening` (P4) sets and meets measurable NFR targets (performance, availability, scalability, observability, security hardening) once real usage exists (criteria defined when that child PRD is drafted).
- **IC-6 (Business linking):** `business-linking` (P5) links events to businesses with dedupe and detail capture — a published event naming a venue has a non-null `business_id`, and repeats share one business row (criteria in that PRD).

*Epic→child-PRD coverage: Epic 1→producer-intake; Epic 2→(future); Epic 3→consumer-discovery; Epic 4→ingestion-framework; Epic 5→(future hardening/NFR PRD); Epic 6→business-linking. No epic without a home.*

## 15. VERSION HISTORY
- **v0.10 (2026-10-26):** Added **Epic 6 / P5 — Business & venue linking** (child PRD
  `docs/business-linking/prd.md`, status draft) + **IC-6**; updated the tag legend and the
  epic→child-PRD coverage line. Gives the already-modeled `business`/`business_metadata` tables
  and the nullable `event.business_id` FK a feature home (derive/link/dedupe a business from a
  submission) — a gap surfaced when E2E showed those tables never populate. No change to P1–P4
  scope. Business-owner accounts/auth and consumer venue pages remain out (auth work + P2).
- **v0.9 (2026-10-14):** Added the **user identity vs. role** model to Concepts — `user` is identity-only; roles (producer/consumer/operator/…) in a many-to-many `user_role` table (role = data, not schema; supports multi-role users and future types without migration; the authz gate). Recorded **authentication & authorization as a deferred, platform-wide concept** — interim control is network restriction; the `edit_token` is a capability link, not a security boundary. (Mirrors P1 v0.5 and the design-philosophy steering update.)
- **v0.8 (2026-10-13):** Applied the richer reference PRD format at initiative level: USER STORIES regrouped into **epics by feature/priority** (Epic 1–4) each mapping to a child PRD; OPEN QUESTIONS converted to a status table + resolved-decisions log; sections 13–14 made initiative-level (cross-feature sign-off; coarse **IC-1…IC-4** success criteria that delegate detailed `AC-###` to child PRDs, removing the now-stale duplicated P1 ACs). No scope change.
- **v0.7 (2026-10-12):** **Reframed this document as the Initiative overview** and split the buildable scope into child PRDs under `docs/prd/` (producer-intake P1, consumer-discovery P2 stub, ingestion-framework P3 stub), per the spec-driven right-sizing rule — driven by an independent `spec-reviewer` audit of v0.6. **Fixed audit defects:** DB-engine contradiction (now MySQL, no Postgres/undecided language) and build-tool contradiction (Gradle); removed resolved OQ-Engine/OQ-Build; set `type: Initiative`, `multifeature: true`. The first right-sized, fully-traceable spec now lives in `docs/producer-intake/prd.md` (UF/AC coverage closed, AC1↔V4 resolved, webhook idempotency/verification + media-fetch-failure added).
- **v0.6 (2026-10-12):** Added an explicit **priority ordering** (user decision): P1 = producer loop (WhatsApp one-shot intake → edit UI → direct-entry web form); P2 = conversational (stateful, follow-up-question) intake + consumer discovery/interests; P3 (lowest) = the multi-source ingestion framework. Added **direct-entry UI** (U5b) as a second P1 producer channel and **conversational intake** (U5c) as a distinct P2 capability requiring conversation state. Reorganized Scope into P1/P2/P3.
- **v0.5 (2026-10-12):** Expanded automated ingestion into a general **multi-source ingestion framework**: pluggable **connectors** by source *shape* (inbound push API, outbound pull API, structured web, unstructured web/JS calendars, documents/PDF, images, compliance-gated messaging/social) feeding one shared **acquire→normalize→extract→dedupe→store** pipeline. Added **G7 inbound events API** (recommended as the first/cheapest connector), U10, and broadened provenance to include an `api_client` origin. Flagged messaging/social (WhatsApp groups, FB, IG) as compliance-gated and not assumed scrapable. De-duplication elevated to a first-class concern. Updated Scope, Goals, User Stories, Concepts (connector+pipeline), Open Questions (OQ-Ingest-1..4). Steering file updated.
- **v0.4 (2026-10-12):** Added **automated local-source ingestion** (newspapers/town bulletins/listings → OCR/parse → events) as a later-slice, good-to-have capability, spec'd in the PRD and covered by the schema now. Generalized event **provenance/origin** to be either a human submitter or an **`ingestion_source`** entity (`origin_type` + appropriate FK), so pipeline events coexist with WhatsApp submissions without overloading `submitted_by`. Updated Context, Goals (G6), User Stories (U8/U9), Scope (incl. ToS/legal note), Service Boundary, Concepts, AC6, and Open Questions (OQ-Ingest-1..3). Reinforced "intake source is data, not schema."
- **v0.3 (2026-10-12):** Adopted **generality-first** model: `event` is the universal unit; **kind is data** via an `event_type` lookup (concert/offer/workshop/…); `business` added as an optional, type-driven link (`event.business_id` nullable); `ends_at` added to serve as an offer validity window. Captured the principle in a persistent steering file `.kiro/steering/design-philosophy.md`. Confirmed engine **MySQL**, build tool **Gradle**. Metadata is per-entity key/value with duplicate keys allowed.
- **v0.2 (2026-10-11):** Replaced the fixed `purchase_url` field with a general **labeled-attributes** model `(label, value, kind)` attached to each event. URLs (and other labeled flyer data) are captured losslessly with the source label verbatim; meaning is interpreted later by an LLM at serve time. Kept typed core columns (title/starts_at/location) for querying. Updated V3/V3b, CONCEPTS (Event core vs. Event attribute), AC3/AC4, and the D3 resolution.
- **v0.1 (2026-10-11):** Restructured into the 15-section FT-1 template with frontmatter. Consolidated prior drafting: two-sided product shape; resolved D1–D4, D6–D9; schema design intent (broad relational core + JSON escape hatch, Postgres recommended); OCR via Tess4J; WhatsApp Cloud API test mode; edit-token model. Open: D5 (required fields), DB engine, build tool. Design doc intentionally deferred.
