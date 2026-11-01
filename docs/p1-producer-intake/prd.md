---
title: Arang — Producer Intake (P1)
type: PRD
artifact_id: ARANG-PRD-P1
parent: ARANG-INIT-001
status: approved
version: 1.1
driver: (you)
approver: (you)
approver_role: product owner / driver
assigned_date: 2026-10-12
first_draft_target:
last_revised_date: 2026-10-14
approved_date: 2026-10-14
review_provenance: collaborative drill (prd-section-reviewer skill) + independent gate (spec-reviewer agent) PASS on v0.8
jira:
multifeature: false
related_docs:
  - docs/prd.md (initiative overview)
  - docs/p1-producer-intake/trd.md (TRD — verifiable REQ-01..18 derived from these ACs)
  - docs/p1-producer-intake/sdd.md (SDD — the design; written after this PRD was approved)
  - docs/p1-producer-intake/ttp.md (TTP — test plan proving the REQs)
origin: Carved from the Arang initiative as the right-sized, build-first feature PRD
tags: [whatsapp, event-intake, ocr, edit-ui, producer]
---

# Arang — Producer Intake (P1)

> One coherent intention: **let a producer get an event into Arang and correct it.**
> Scope is the producer loop only — WhatsApp intake (text/image/link), extraction,
> storage with provenance, a confirmation reply, a token-authorized edit page, and a
> direct-entry web form. Consumer discovery (P2) and the ingestion framework (P3) are
> separate child PRDs. Inherits all principles from the initiative overview and the
> steering files (generality, spec-driven workflow).

## 1. CONTEXT
Producers need a low-friction way to put a weekend event into Arang. P1 delivers the
producer half end-to-end: a user submits over WhatsApp (free text, a flyer image, a link,
or a mix), Arang extracts structured fields and stores an `event` with user provenance,
replies confirming what it understood with a private edit link, and offers a web form as
an alternative entry channel. No consumer-facing discovery in this PRD.

## 2. PROBLEM STATEMENT
Capturing an event today means manual data entry; flyers and links get lost in chats.
P1's gap: there is no path to turn a WhatsApp message or a typed form into a clean,
correctable event record. Desired state: send it (or type it), get it captured and
confirmed, fix it via a private link if needed.

## 3. GOALS & SUCCESS METRICS
- **G1 — Frictionless WhatsApp intake:** a submitted message yields a stored `event` + a
  confirmation reply. *Metric: for a text/image/link message, a record exists and a reply
  is sent.*
- **G2 — Usable extraction:** title/time/location + labeled URL/attributes extracted from
  text and from flyer images (OCR). *Metric: for a clear flyer, title + ≥1 of start/location
  populated.*
- **G3 — Correctability without accounts:** submitter fixes fields via a private edit link.
  *Metric: a correction saved via the link persists on reload.*
- **G4 — Second entry channel:** a user can type an event into a web form with the same
  result as WhatsApp. *Metric: a valid form submission creates the same shape of record.*

## 4. USER STORIES / USE CASES
**Primary actor:** the **submitter** (a producer identified by WhatsApp phone number, or an anonymous web-form user). Secondary actor: the **operator** (you).

**Build-type tags (from-scratch adaptation).** The reference PRD tags each flow as reused/adapt/new against a prior codebase. Arang is built from scratch, so there is nothing to reuse; instead each flow is tagged by *priority within P1*:
`[core]` = required for the first running slice · `[stretch]` = in P1 but can follow the core slice.
Each flow lists **Owned by** (the service component) and **Flow** (primary / alternate / edge).

### Group A — WhatsApp intake
- **UF-001 (text intake)** `[core]` — As a submitter, I text an event and get a confirmation echoing what was captured.
  - Owned by: webhook + extraction service · Flow: primary
- **UF-002 (image intake)** `[core]` — As a submitter, I send a flyer photo; Arang OCRs it and stores the event.
  - Owned by: webhook + OCR/extraction service · Flow: primary (edge: media-fetch failure, see V2b)
- **UF-003 (link intake)** `[core]` — As a submitter, I include URLs; each is captured as a labeled attribute.
  - Owned by: extraction service · Flow: primary
- **UF-005 (incomplete input)** `[core]` — As a submitter, when a field can't be extracted, the record is still stored (as a draft) with the gap flagged and the reply tells me what's missing.
  - Owned by: extraction service + reply composer · Flow: edge

### Group B — Review, correct, publish
- **UF-004 (review & edit)** `[core]` — As the submitter, I open the private link to a review page showing the parsed fields, and I can correct any field; changes persist.
  - Owned by: edit/review web page · Flow: primary
- **UF-008 (review & submit)** `[core]` — As the submitter, on the review page I click an explicit Submit/Confirm action that promotes the event from draft to published; only then is it live.
  - Owned by: edit/review web page · Flow: primary
- **UF-009 (delete)** `[core]` — As the submitter, on the review page I can delete the event; afterward the link no longer resolves.
  - Owned by: edit/review web page · Flow: alternate

### Group C — Alternate entry channel
- **UF-006 (direct-entry UI)** `[stretch]` — As a submitter, I type an event into a web form instead of WhatsApp and it is stored the same way (as a draft).
  - Owned by: direct-entry web form · Flow: primary

### Group D — Provenance
- **UF-007 (provenance)** `[core]` — As an operator, each stored event records its origin (user + WhatsApp id, or web form) and creation time.
  - Owned by: persistence layer · Flow: primary (cross-cutting)

### Group E — Operator / non-functional (NFR)
- **UF-OPS (platform correctness)** `[core]` — As an operator, the service is correct and durable under real conditions: events persist across restarts, the webhook is verified and idempotent, extraction records per-field confidence, expired tokens are enforced, and abandoned drafts are cleaned up. (Umbrella flow giving the infra/background acceptance criteria a traceability home: AC-012, AC-013, AC-014, AC-017, AC-021b, AC-022.)
  - Owned by: webhook / persistence / scheduler · Flow: cross-cutting

## 5. SCOPE
**In scope — overview.** The producer loop end-to-end: get an event in (WhatsApp or web form), extract it, store it as a draft with provenance, let the submitter review/correct/submit/delete it via a private link.

**In scope — detail (built under this feature):**
- WhatsApp webhook intake: text, image + OCR, link capture (Group A).
- Extraction to typed core (`title`, `starts_at`, `ends_at`, `location`) + labeled `event_metadata` (incl. all URLs).
- `event` persistence with `origin_type=user`, `status=draft`, `edit_token`, timestamps.
- Confirmation reply with the private review link.
- Token-authorized review page: view, edit, submit (draft→published), delete (Group B).
- Direct-entry web form as an alternate channel (Group C, `[stretch]`).
- Webhook verification (verify-token) and idempotency (dedupe on provider message id).

**Reused unchanged:** none — Arang is built from scratch; there is no prior codebase to cite. (This subsection exists in the reference template to mark reused components; recorded here as N/A.)

**Out of scope (explicit exclusions):**
- Consumer discovery / interests (P2); conversational multi-turn intake (P2).
- Any automated ingestion / connectors (P3).
- Accounts / passwords / OTP; de-duplication of events; recurring events.
- Multi-language extraction (English-only); fetching/reading link page contents.
- Paid WhatsApp template / marketing messages.

**Geography & localisation:** English-only for P1; event content stored as submitted. Timestamps stored in UTC. Localisation is out of scope.

## 6. SERVICE BOUNDARY
**Service:** `arang-intake` (single Spring Boot app for P1). One-directional dependency on the WhatsApp Cloud API: Arang receives from and sends to WhatsApp; WhatsApp never reads Arang.

**Surfaces Arang exposes/owns:**
1. **Inbound webhook** (`GET` verify + `POST` messages) — receives WhatsApp events; owns verification and idempotency.
2. **Outbound reply** — sends the confirmation + review link via the Cloud API (within the 24h window).
3. **Review/edit web page** (token-authorized) — view / edit / submit / delete a draft.
4. **Direct-entry web form** — alternate producer channel (`[stretch]`).
5. **Persistence** — the `event` / `event_metadata` / `user` store (MySQL).

**External (not owned):** WhatsApp Business Cloud API (message transport); the tunnel (ngrok/cloudflared) exposing the webhook over HTTPS during local hosting.

**Paths:**
- Write (WhatsApp): `WhatsApp → webhook (verify + dedupe) → extract → store as DRAFT → reply with review link`.
- Write (form): `form POST → extract/validate → store as DRAFT`.
- Review/edit: `review link (token) → review page → edit | submit (draft→published) | delete`.

## 7. VALIDATIONS ON INCOMING DATA
**Outcome legend** (how Arang treats a validation result):
- **store+flag** — never reject; create/keep the record and mark the gap in `extracted_confidence` (the governing principle: a submission is never silently dropped).
- **reject** — refuse the action (used only for unauthorized edit/submit/delete).
- **dedupe** — ignore as a duplicate (no new record).

**Tier** = when the check runs: `on-ingest` (webhook/form intake) or `on-edit` (review page).

| # | Validation | Tier | Outcome | Notes |
|---|---|---|---|---|
| V1 | A text message with any body creates a record with `raw_message` set | on-ingest | store+flag | Baseline capture. |
| V2 | Image fetched then OCR'd; if OCR yields no usable text, record still created, affected fields get low per-field confidence, reply says image unreadable | on-ingest | store+flag | OCR failure ≠ reject. |
| V2b | If media *download* fails (distinct from unreadable OCR), store from accompanying text/caption, flag media not-retrieved, reply asks for resend | on-ingest | store+flag | Separate from V2. |
| V3 | Each URL captured as `event_metadata (key, value, kind=url, confidence)` with source label verbatim; stored not fetched | on-ingest | store+flag | No fixed `purchase_url`. |
| V4 | If no title determinable, record still stored with `title` empty + low per-field confidence on title; reply names the missing field | on-ingest | store+flag | Resolves AC1↔V4: title not required. |
| V4b | Every newly captured event stored with `status=draft`; becomes `published` only via explicit submit | on-ingest | store+flag | Draft-first. |
| V4c | An event cannot be **published** without a `postal_code`; it may be **captured** (draft) without one. The review page requires it before Submit; `country_code` defaults to `US` | on-edit | reject (publish only) | Enables map placement; internationalizable; resolves OQ-P1-1. |
| V5 | An edit/submit/delete request is accepted only with a valid, non-expired `edit_token` matching the target event | on-edit | reject | Authorization gate. |
| V5b | A delete action removes the event and its metadata; the token no longer resolves afterward | on-edit | delete | Terminal action (irreversible in P1). |
| V6 | Every record sets `origin_type`, origin reference (`submitted_by` or form), and `created_at` | on-ingest | store+flag | Provenance always set. |
| V7 | Webhook validates WhatsApp's verify-token on GET and rejects POSTs that fail payload validation | on-ingest | reject | Security on an exposed tunnel. |
| V8 | A redelivered WhatsApp message (same provider message id) does not create a duplicate event | on-ingest | dedupe | WhatsApp retries on non-200. |
| V8b | A draft never submitted is auto-deleted **2 months** after creation (scheduled cleanup) | background | — | Prevents stale-draft accumulation (OQ-P1-4). |
| V8c | The `edit_token` is valid **24h**; within it, edit/submit/delete are allowed (incl. after publish). After expiry, writes are rejected and Arang sends **no** new WhatsApp message; the user re-submits the event to get a fresh token | on-edit | reject (after expiry) | Aligns with WhatsApp 24h window; no paid renewal (B3). |
| V8d | Re-sending the same flyer/text creates a **new draft** (dedupe is only on provider message id, V8); duplicate events from re-sends are an **accepted P1 limit** | on-ingest | store+flag | Named limit (G-b); real dedupe is P3. |

## 8. DEPENDENCIES & INTEGRATIONS
- **WhatsApp Business Cloud API** (test mode): free test number, ≤5 verified recipients, $0.
- **OCR:** Tesseract via Tess4J (local, $0).
- **Stack:** Java + Spring Boot; **Gradle**; **MySQL** (local via Docker).
- **Tunnel:** ngrok or cloudflared for the HTTPS webhook.
- **Note — 24-hour window (drives the token lifetime):** WhatsApp allows free-form replies
  only within 24h of the user's message. P1 therefore sets the `edit_token` lifetime to **24h**
  so the entire review-and-submit loop (confirmation reply, any resend-ask in V2b, review, edit,
  submit) happens inside the free window. Arang sends **no** WhatsApp messages after the window
  (no token "renewal" message, no paid templates — out of scope §5). If a user returns after 24h,
  the browser link is read-only and they must resubmit the event to get a fresh token.

## 9. CONSTRAINTS
- $0 prototype; local hosting; webhook reachable only while machine + tunnel are up.
- Stack/engine/build tool fixed (above). English-only. No user accounts; editing a specific event is authorized by its per-event `edit_token`.
- **No user authentication/authorization in P1 (deliberately deferred).** The review/edit surface
  is protected by **network restriction**, not in-app auth: it is **not publicly exposed** —
  kept behind a VPN/VPC (AWS) or on localhost. The operator reviews and submits. The
  `edit_token` authorizes actions on **one event** (object-level capability) and is the P1 edit
  access-control mechanism — but it is **not** user-level authentication/authorization (no login,
  roles, or cross-device identity). Those are a future platform-wide piece gating on `user_role`
  (see OQ-P1-7 and the initiative overview). Network restriction is the broader P1 guard around
  the whole surface.

## 10. CONCEPTS
Inherits the initiative's data model (event = universal unit; `event_type`; optional
`business`; typed core + per-entity key/value metadata; provenance). P1 uses:
`event` (typed core + `status`, `source_type` (text|image|link|mixed), `media_ref` (stored
image reference, if any), `origin_type=user`, `submitted_by`, `edit_token`,
timestamps), `event_metadata` (labeled attributes incl. URLs), `user` (identity only,
keyed by WhatsApp phone). **User/role:** `user` holds no type column; roles live in a
`user_role` join table (producer/consumer/…) designed into the schema now but **not read or
enforced in P1** — every P1 submitter is implicitly a producer. Role becomes the authz gate
later (see Constraints / OQ-P1-7).
- **`status` (draft | published):** every event is created as `draft`; an explicit submit
  on the review page promotes it to `published`. **Forward-looking rule for P2:** only
  `published` events are consumable/discoverable. P1 has no consumer, so this rule is
  stated now (so P2 inherits it) but has no P1 acceptance criterion.
- **`edit_token`** = unguessable per-event secret delivered **via WhatsApp** in the reply;
  grants review/edit/submit/delete on **that one event**, no login. **Expires 24 hours** after
  issue — deliberately aligned with WhatsApp's 24-hour free-form messaging window, so the whole
  review-and-submit loop happens inside the window at $0 (no paid templates). After expiry the
  link no longer permits writes; it may still render the event read-only in the browser. Scoping
  to a single event is also the anti-abuse control — the token only permits writes to its own
  event, so there is no open write surface to spam.
  - **Access-control role (B1):** within P1, the `edit_token` **is** the access-control mechanism
    for editing a specific event (a valid, non-expired token for that event is required — see V5).
    It is **not** a substitute for real **user authentication/authorization** (login, roles,
    cross-device identity), which is deferred (OQ-P1-7). "Capability link" means: it authorizes an
    action on one object, not a user session.
- **Review page** = a single token-authorized page that shows the parsed draft, allows editing
  fields, an explicit **Submit** (draft→published), and **Delete**. Editing remains allowed
  **through the token's 24-hour lifetime, including after publish** (correcting a live event —
  time changed, link broke — is a legitimate need and adds no access beyond the already-scoped,
  already-expiring token). The **token expiry (24h)** — not the act of publishing — is what
  freezes the event in P1. (Review page = edit page; one page, one token.)
- **Per-field confidence.** Extraction is uncertain per field, not per record. Each
  extracted datum carries its own `confidence` as a **0–1 float**:
  - For labeled metadata, the shape extends to `(key, value, kind, confidence)` — e.g.
    `("title","Jazz Night","text",0.95)`, `("date",null,"date",0.2)`, `("Tickets",url,"url",0.9)`.
  - Typed-core fields (`title`, `starts_at`, `ends_at`, `location`) also get a per-field
    confidence recorded alongside the event (mechanism settled at design time — likely a
    small `(event_id, field, confidence)` set or confidence columns).
  - `event.extracted_confidence` becomes a **derived roll-up** (e.g. the minimum field
    confidence), kept for coarse "needs review" sorting; the per-field values are the
    source of truth.
  - **Deferred (noted, not built in P1):** a per-field **`source`** (ocr | text | url) to
    record where each value came from — useful later to trust typed text over OCR. See OQ-P1-6.
- **Behavior driven by confidence:** the review page highlights low-confidence fields for
  the user to check first; the reply flags which fields Arang was unsure about.
- **Location (internationalizable, approximate).** Instead of a US-only `zip` field, location
  is modeled generically so Arang can go international later without a migration:
  - `postal_code` — **string** (not numeric; many countries use letters/spaces).
  - `country_code` — ISO 3166-1 alpha-2 (e.g. `US`), **defaults to `US`** for the prototype.
  - `latitude` / `longitude` — **derived** from (`postal_code` + `country_code`) via a local
    lookup (postal-centroid), used only for approximate map placement — not precise geocoding.
  A US ZIP is simply `postal_code` with `country_code=US`. `location` free-text (venue/place
  as given) is kept too; the postal code is the structured, filterable, map-driving field.
  - **Required to publish, not to capture** (resolves OQ-P1-1): an event is still captured as
    a draft without a postal code (never silently dropped), but **cannot be submitted/published
    until `postal_code` is provided** — asked for on the review page when extraction doesn't
    find one. This guarantees every *published* event can be placed on the consumer map.

## 11. ASSUMPTIONS
- A1: Success = correct record + confirmation reply; verified via datastore + reply.
- A2: One message = one event.
- A3: Images are printed flyers/posters, not handwriting.
- A4: Service receives only while machine + tunnel run.
- A5: The edit-link holder is the legitimate editor (WhatsApp thread = proof).
- A6 (accepted limit): Between token expiry (24h) and draft cleanup (2 months), an unsubmitted draft is **frozen but retained** — no access path in P1; the user resubmits to edit. Accepted for the prototype.
- A7 (accepted limit): Re-sending the same event creates a duplicate draft (dedupe is only on provider message id, V8/V8d). Cross-submission de-duplication is a P3 concern, not P1.

## 12. OPEN QUESTIONS

| # | Question | Status | Default / recommendation | Owner |
|---|---|---|---|---|
| OQ-P1-1 | Required fields — must any field be mandatory? | RESOLVED | Nothing required to **capture** (always store + flag gaps); `postal_code` is required to **publish** (V4c). `country_code` defaults to `US` | driver |
| OQ-P1-2 | Edit-token lifecycle — does it expire / can it be revoked / single-use? | RESOLVED | **Expires 24h** (aligned to WhatsApp window), delivered via WhatsApp, scoped to one event; editing allowed through expiry incl. after publish; **no WhatsApp renewal** — after expiry the user resubmits | driver |
| OQ-P1-3 | Direct-entry form — any anti-abuse control (captcha/rate-limit/auth)? | RESOLVED | None in P1 (network-restricted; the edit token already scopes writes to one event). Revisit when public | driver |
| OQ-P1-4 | Abandoned drafts — what happens to drafts never submitted? | RESOLVED | Auto-delete **2 months** after creation if still draft (V8b) | driver |
| OQ-P1-5 | Confidence scale — float vs. buckets? | RESOLVED | **0–1 float** per field; `extracted_confidence` is a derived roll-up (min) | driver |
| OQ-P1-6 | Per-field source (ocr/text/url) — record provenance of each extracted value? | DEFERRED | Not in P1; noted in Concepts for later | driver |
| OQ-P1-7 | Authentication/authorization for the review/edit surface | DEFERRED | P1 relies on network restriction (not public; VPN/VPC or localhost); real authn/authz (gating on `user_role`) is a future platform-wide piece | driver |
| OQ-P1-8 | Quantitative NFRs — performance/latency, availability/uptime, throughput/scalability, observability (logging/metrics/tracing/alerting), and security hardening (token entropy, rate-limiting, PII handling, media-storage security) | DEFERRED → **P4** | Out of the P1 prototype. P1 keeps only the correctness-critical operational NFRs (durability, webhook verify, idempotency, token expiry, cleanup — AC-012/013/014/017/021b/022). Measurable targets + tests are set in the **Hardening/NFR (P4)** child PRD once real usage exists to measure against. Revisit condition: before any non-network-restricted / public deployment, or when load becomes non-trivial | driver |

*All P1 open questions are now RESOLVED (OQ-P1-1..5) or DEFERRED (OQ-P1-6 per-field source, OQ-P1-7 authn/authz, OQ-P1-8 quantitative NFRs → P4). Ready for the PRE-SIGN-OFF checklist and an independent review pass.*

## 13. PRE-SIGN-OFF VERIFICATION
*(Boxes are the driver's to tick — this is the human sign-off act. Approved 2026-10-14.)*
- [x] Every UF (001–009) maps to ≥1 AC and vice versa (see §14 coverage).
- [x] AC1↔V4 ambiguity resolved (title not required to persist).
- [x] Draft→published model: event created as draft; explicit website Submit publishes it.
- [x] Delete action on the review page accepted (V5b/UF-009).
- [x] "Only published events are consumable" recorded as a P2-inherited rule.
- [x] Webhook idempotency (V8) and verification (V7) accepted as P1 requirements.
- [x] Media-fetch-failure path (V2b) distinct from OCR-unreadable (V2) accepted.
- [x] `postal_code`/`country_code` location model + required-to-publish (V4c) accepted.
- [x] Edit-token expires **24h** (WhatsApp-window-aligned), no WhatsApp renewal, WhatsApp-delivered; editing allowed through expiry **including after publish**; after expiry the user resubmits (OQ-P1-2 / AC-020/021a/021b).
- [x] Abandoned drafts auto-deleted after 2 months (OQ-P1-4 / V8b / AC-022).
- [x] Direct-entry form needs no extra anti-abuse in P1 (token scopes edits; network-restricted) (OQ-P1-3).
- [x] Authn/authz (OQ-P1-7) and per-field source (OQ-P1-6) accepted as deferred.
- [x] Quantitative NFRs (OQ-P1-8) accepted as deferred to the Hardening/NFR phase (P4).
- [x] Scope boundary with P2/P3 confirmed.

## 14. ACCEPTANCE CRITERIA
- **AC-001** ⟵ UF-001,UF-005: A plain-text message creates a record with `raw_message` set;
  `title` is populated if determinable, else left empty with a low per-field confidence on
  `title` (never rejected for a missing title).
- **AC-002** ⟵ UF-002: A flyer image is OCR'd; a record is created with `source_type=image`
  and a `media_ref`; extracted text is stored.
- **AC-003** ⟵ UF-002,V2b: If media download fails, a record is still created from text/
  caption with media flagged not-retrieved and the reply asks for a resend.
- **AC-004** ⟵ UF-003: Each URL in the message is stored as `event_metadata (key, value,
  kind=url, confidence)` with value equal to the URL verbatim.
- **AC-005** ⟵ UF-001,UF-002,UF-003: After storing (as a **draft**), a WhatsApp reply
  echoes `title` (or "couldn't read title") plus any extracted start/location and URL
  labels, **flags which fields it was unsure about (low confidence)**, and includes a
  private link containing the `edit_token` with copy like
  "I captured a draft — review & submit here: <link>".
- **AC-006** ⟵ UF-005: When a field can't be extracted, the reply names the missing field.
- **AC-007** ⟵ UF-004: Opening the link with a valid `edit_token` shows the review page
  pre-filled with the draft's current fields, **with low-confidence fields visibly flagged
  for the user to check first**.
- **AC-008** ⟵ UF-004: Editing a field and saving updates the record, sets `updated_at`,
  and persists on reload.
- **AC-009** ⟵ UF-004,UF-008,UF-009,V5: Any review/edit/submit/delete request with a
  missing/invalid/expired token, or for a non-existent event, is rejected.
- **AC-010** ⟵ UF-006: A valid web-form submission creates a draft record of the same shape
  as a WhatsApp submission, with `origin_type=user` and no WhatsApp id.
- **AC-011** ⟵ UF-007: Every record has `origin_type`, origin reference, and `created_at`.
- **AC-012** ⟵ (durability): Stored events survive a service restart (MySQL, not memory).
- **AC-013** ⟵ V7: The webhook returns the challenge only for a valid verify-token (GET)
  and rejects malformed POSTs.
- **AC-014** ⟵ V8: Re-delivering the same provider message id does not create a duplicate
  event.
- **AC-015** ⟵ UF-008,V4b: On the review page, an explicit Submit action changes the
  event's `status` from `draft` to `published`; the change persists on reload.
- **AC-016** ⟵ UF-009,V5b: On the review page, a Delete action removes the event and its
  metadata; afterward the link/token no longer resolves.
- **AC-017** ⟵ UF-005,V2,V4: Each extracted field (typed-core and labeled metadata) is
  stored with its own `confidence` (0–1 float); `event.extracted_confidence` equals the
  derived roll-up (minimum field confidence).
- **AC-018** ⟵ UF-004,V4c: If no `postal_code` was extracted, the review page requires one
  (with `country_code` defaulting to `US`) before Submit is allowed.
- **AC-019** ⟵ UF-008,V4c: Submitting without a `postal_code` is rejected; a published event
  always has a `postal_code` (+ `country_code`), from which an approximate `latitude`/`longitude`
  is derived for map placement.
- **AC-020** ⟵ UF-004,V5,V8c: An `edit_token` older than **24 hours** is expired; using an
  expired token rejects edit/submit/delete (the page may still render read-only).
- **AC-021a** ⟵ UF-004,UF-008: Within the 24h window, editing is allowed **after the event is
  published** — a published event can still be corrected and the change persists.
- **AC-021b** ⟵ V8c: After token expiry, Arang sends no new WhatsApp message; the user must
  re-submit the event to obtain a fresh token/link (no in-WhatsApp renewal).
- **AC-022** ⟵ V8b: A draft still unsubmitted 2 months after creation is removed by the cleanup
  job; published events are unaffected.

**Coverage check:** UF-001→AC-001,005,006; UF-002→AC-002,003,005; UF-003→AC-004,005;
UF-004→AC-007,008,009,018,020,021a; UF-005→AC-001,006,017; UF-006→AC-010; UF-007→AC-011;
UF-008→AC-015,009,019,021a; UF-009→AC-016,009. **UF-OPS (operator/NFR)** →
AC-012 durability, AC-013 webhook verify (V7), AC-014 idempotency (V8), AC-017 per-field
confidence (V2/V4), AC-021b post-expiry resubmit (V8c), AC-022 draft cleanup (V8b). No orphan
UFs; no orphan ACs.

## 15. VERSION HISTORY
- **v1.1 (2026-10-21):** Added **OQ-P1-8** recording the explicit **deferral of quantitative NFRs**
  (performance, availability, scalability, observability, security hardening) to a new
  **Hardening/NFR phase (P4)** in the initiative overview (Epic 5 / IC-5). Editorial deferral
  record only — **does not change P1's approved scope**; P1 still owns the correctness-critical
  operational NFRs (durability/idempotency/webhook-verify/token-expiry/cleanup). Pre-sign-off
  deferral note updated. Approval of v1.0 stands.
- **v1.0 (2026-10-14) — APPROVED.** Signed off by the driver after collaborative review
  (prd-section-reviewer skill) and an independent `spec-reviewer` gate returning **PASS** on
  v0.8. All PRE-SIGN-OFF boxes ticked; all open questions resolved or deferred. This is the
  approved baseline; the design/SDD (now split into `docs/trd/`, `docs/sdd/`, `docs/ttp/`
  producer-intake.md) is written next.
- **v0.8 (2026-10-14):** Fixes from the independent `spec-reviewer` audit of v0.7.
  **B3:** token lifetime changed **2 days → 24h** to match WhatsApp's free window; removed the
  WhatsApp "renewal" message (no paid templates) — after expiry the user resubmits (V8c, AC-020/
  021b). **B1:** reworded so the `edit_token` **is** P1's object-level edit access control, but
  **not** user authn/authz (removed the contradictory "not a security boundary" phrasing) in both
  this PRD and the initiative overview. **B2:** split the overloaded AC-021 into AC-021a
  (edit-after-publish within window) and AC-021b (post-expiry resubmit). Non-blocking: added
  **UF-OPS** operator/NFR flow so infra ACs (012/013/014/017/021b/022) have a traceability parent;
  added `source_type`/`media_ref` to typed-core; filled V5b outcome; added V8d + A6/A7 to name the
  accepted limits (duplicate re-sends, expired-not-deleted limbo); fixed stale initiative cross-ref
  (AC range). Coverage re-verified.
- **v0.7 (2026-10-14):** Resolved the last open questions. **Edit-token:** expires **2 days**,
  **renewable**, delivered via **WhatsApp**, scoped to one event; editing allowed through expiry
  **including after publish** (publishing no longer locks the event — the token expiry does).
  **Abandoned drafts:** auto-deleted **2 months** after creation (V8b, scheduled cleanup).
  **Form anti-abuse:** none needed in P1 (token scopes edits to one event; network-restricted).
  Added V8b/V8c, AC-020/021/022; updated Concepts (token + review page), OQ-P1-2/3/4 resolved,
  pre-sign-off checklist refreshed. All P1 OQs now resolved or deferred.
- **v0.6 (2026-10-14):** Added **internationalizable location**: generic `postal_code` (string)
  + `country_code` (ISO, defaults `US`) + **derived** approximate `latitude`/`longitude` from a
  postal-centroid lookup (for the consumer map; not precise geocoding). Modeled this way (not a
  US-only `zip`) so going international later needs no migration. **Required to publish, not to
  capture** — resolves OQ-P1-1. Added V4c, AC-018/AC-019; updated Concepts and coverage. The P2
  consumer map uses these postal-centroid coordinates for approximate-within-town placement.
- **v0.5 (2026-10-14):** Clarified **user identity vs. role** — `user` is identity-only; roles
  (producer/consumer/…) live in a `user_role` join table designed in now but not enforced in
  P1 (every submitter implicitly a producer). Recorded **deferred authn/authz**: P1 protects
  the review/edit surface by **network restriction** (not public; VPN/VPC or localhost), with
  the `edit_token` as a capability link, not a security boundary; real authn/authz (gating on
  `user_role`) is a future platform-wide piece (OQ-P1-7). Updated Constraints, Concepts;
  steering + initiative overview updated with the user/role principle. No AC/coverage change.
- **v0.4 (2026-10-14):** Added **per-field confidence** (user request). Each extracted datum
  (typed-core and labeled metadata) carries its own `confidence` as a 0–1 float; metadata
  shape extends to `(key, value, kind, confidence)`; `event.extracted_confidence` becomes a
  derived roll-up (min). Review page highlights low-confidence fields; reply flags uncertain
  fields. Updated Concepts, V2/V3/V4, AC-001/004/005/007, added AC-017; new OQ-P1-5 (scale =
  0–1 float, resolved) and OQ-P1-6 (per-field source ocr/text/url, deferred). Coverage re-checked.
- **v0.3 (2026-10-13):** Reformatted to the richer reference PRD layout (from screenshots):
  USER STORIES grouped by theme (A–D) with **Owned by**/**Flow** sub-fields and a
  from-scratch build-type tag legend (`[core]`/`[stretch]` instead of reuse/adapt/new);
  VALIDATIONS now a table with an **outcome legend** (store+flag / reject / dedupe) and a
  **Tier** column (on-ingest / on-edit); OPEN QUESTIONS now a numbered table with status +
  default; SCOPE split into overview / detail / reused(N/A) / out-of-scope / localisation;
  SERVICE BOUNDARY names the `arang-intake` service and its five surfaces. No content/decision
  changes — all UF/AC IDs and coverage preserved.
- **v0.2 (2026-10-12):** Added **Option B — draft→published review flow** via the
  `prd-section-reviewer` skill. Events are created as `draft`; the WhatsApp reply links to a
  token-authorized **review page** (= edit page) where the user edits, then explicitly
  **Submits** (draft→published) or **Deletes**. Added UF-008 (submit), UF-009 (delete),
  AC-015/AC-016, V4b/V5b, `status` to Concepts, and the P2-inherited rule "only published
  events are consumable". Reworded AC-005 (review link + draft copy) and AC-007/008/009 for
  the review page. New OQ-P1-4 (abandoned drafts persist). Coverage re-checked (no orphans).
- **v0.1 (2026-10-12):** Initial right-sized P1 PRD, carved from the Arang initiative after
  an independent spec-reviewer audit of the combined PRD (v0.6). Fixes applied from the
  audit: clean `UF-###`/`AC-###` IDs with full two-way coverage; resolved AC1↔V4 (title not
  required to persist); added webhook verification (V7/AC-013), idempotency (V8/AC-014), and
  media-fetch-failure path (V2b/AC-003); edit-token lifecycle raised as OQ-P1-2; WhatsApp
  24-hour window noted. Engine MySQL / build Gradle stated without contradiction.
