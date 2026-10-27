---
title: Arang — Producer Intake (P1) — TRD
type: TRD
artifact_id: ARANG-TRD-P1
parent: ARANG-PRD-P1
status: draft
version: 1.0
driver: (you)
approver:
approver_role: product owner / driver
assigned_date: 2026-10-14
last_revised_date: 2026-10-20
approved_date:
review_provenance:
related_docs:
  - docs/producer-intake/prd.md (approved PRD v1.0 — source of ACs; the "what & why")
  - docs/producer-intake/sdd.md (SDD — the "how"; design builds on these REQs)
  - docs/producer-intake/ttp.md (TTP — proves each REQ via TC-###)
origin: Extracted from the former folded design.md §1 when the project moved to one-file-per-stage (2026-10-20)
tags: [trd, requirements, req, p1, producer-intake]
---

# Arang — Producer Intake (P1) — TRD

> **Verifiable technical requirements** for the approved P1 PRD (v1.0). Each `REQ-###` is a
> single testable "the system MUST…" statement tracing to a PRD acceptance criterion. This doc
> answers *what the system must do*; the *how* is the SDD (`docs/producer-intake/sdd.md`) and the
> *proof* is the TTP (`docs/producer-intake/ttp.md`).
>
> Chain: `UF → AC (PRD) → REQ (here) → T# (SDD tasks) → TC (TTP)`.

## Intake
- **REQ-01** — The system MUST expose a WhatsApp webhook: `GET /webhook` echoes the challenge iff `hub.verify_token` matches the configured token; `POST /webhook` accepts the Cloud API message payload. ⟵ AC-013
- **REQ-02** — On an inbound text message, the system MUST create one `event` with `raw_message` set and `status=draft`. ⟵ AC-001
- **REQ-03** — On an inbound image message, the system MUST fetch the media, run OCR, store the extracted text, and set `source_type=image` with a `media_ref`. ⟵ AC-002
- **REQ-04** — If media download fails, the system MUST still create a draft from any text/caption, mark media not-retrieved, and reply asking for a resend. ⟵ AC-003
- **REQ-05** — The system MUST extract URLs and store each as `event_metadata(key,value,kind='url',confidence)` with the value verbatim. ⟵ AC-004
- **REQ-06** — The system MUST store a per-field `confidence` (0–1 float) for every extracted field (typed-core + metadata) and compute `event.extracted_confidence` as the minimum. ⟵ AC-017
- **REQ-07** — The system MUST be idempotent on the WhatsApp provider message id (a redelivery creates no duplicate event). ⟵ AC-014

## Reply
- **REQ-08** — After creating a draft, the system MUST send one WhatsApp reply echoing title (or "couldn't read title") + any start/location + URL labels, flagging low-confidence fields, and containing the review link with the `edit_token`. ⟵ AC-005, AC-006

## Review / edit / submit / delete
- **REQ-09** — `GET /review/{id}?token=…` MUST render the event's current fields prefilled iff the token is valid, matches the event, and is non-expired; low-confidence fields MUST be visibly flagged. ⟵ AC-007, AC-018
- **REQ-10** — The system MUST accept field edits (saving sets `updated_at`) and reject any edit/submit/delete with a missing/invalid/expired token or unknown event. ⟵ AC-008, AC-009, AC-020
- **REQ-11** — Submit MUST set `status=published` only if `postal_code` is present; publishing MUST NOT lock editing within the token window. ⟵ AC-015, AC-019, AC-021a
- **REQ-12** — Delete MUST remove the event and its metadata; the token MUST stop resolving afterward. ⟵ AC-016
- **REQ-13** — Publishing MUST require `postal_code` (+ `country_code`, default `US`); the system MUST derive approximate `latitude`/`longitude` from a postal-centroid lookup. ⟵ AC-019

## Alternate channel
- **REQ-14** — The system MUST accept a direct-entry web-form submission that creates a draft of the same shape (`origin_type=user`, no WhatsApp id). ⟵ AC-010

## Provenance, durability, lifecycle (operator/NFR)
- **REQ-15** — Every event MUST record `origin_type`, origin reference (`submitted_by` or form), and `created_at`. ⟵ AC-011
- **REQ-16** — Events MUST persist across restart (MySQL, not memory). ⟵ AC-012
- **REQ-17** — Expired tokens MUST block writes; after expiry the system sends no WhatsApp message (user resubmits). ⟵ AC-021b
- **REQ-18** — A scheduled job MUST delete drafts unsubmitted after 2 months; published events unaffected. ⟵ AC-022

## REQ ↔ AC coverage
Every PRD AC (001–022, incl. 021a/b) maps to ≥1 REQ above; every REQ traces to ≥1 AC. 18 REQs —
within the 5–15 guideline's spirit for a prototype; grouped, no duplication of AC prose.

## Version history
- **v1.0 (2026-10-20):** Extracted verbatim from the former folded `design.md` §1 (SDD v0.2) into
  this standalone TRD when the project adopted one-file-per-stage. Content unchanged (REQ-01..18
  and the REQ↔AC coverage note); only relocated and given its own frontmatter. Status carried as
  `draft` pending the SDD/TRD review gate.
