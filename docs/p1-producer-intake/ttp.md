---
title: Arang — Producer Intake (P1) — TTP
type: TTP
artifact_id: ARANG-TTP-P1
parent: ARANG-SDD-P1
status: draft
version: 0.1
driver: (you)
approver:
approver_role: product owner / driver
assigned_date: 2026-10-20
last_revised_date: 2026-10-20
approved_date:
review_provenance:
related_docs:
  - docs/p1-producer-intake/prd.md (approved PRD v1.0 — ACs)
  - docs/p1-producer-intake/trd.md (TRD — REQ-01..18 these TCs prove)
  - docs/p1-producer-intake/sdd.md (SDD — §5 Checks + §6 task slices this plan follows)
origin: Initial P1 test plan authored alongside the one-file-per-stage split (2026-10-20)
tags: [ttp, test-plan, tc, p1, producer-intake]
---

# Arang — Producer Intake (P1) — TTP

> The **test plan** that proves P1. Each `TC-###` has a precondition, action, and observable
> expected result, and traces to ≥1 REQ (and through it to an AC). Final link in the chain
> `UF → AC → REQ → T# → TC`. Grouped by the SDD's task slices (§6) so each build task has its
> proving tests. Prefer automatable (unit/integration); manual cases are marked `[manual]`.
> **Draft skeleton** — cases are specified but not yet implemented; author alongside the build.

---

## Slice 1 — T2: Webhook verify + idempotency (REQ-01, REQ-07)
- **TC-001** *(happy)* — GET `/webhook?hub.verify_token=<valid>&hub.challenge=X` → responds `X`. ⟵ REQ-01
- **TC-002** *(error)* — GET `/webhook` with a wrong/absent verify_token → rejected (no echo). ⟵ REQ-01
- **TC-003** *(happy)* — POST `/webhook` with a valid message payload → accepted (2xx). ⟵ REQ-01
- **TC-004** *(boundary/idempotency)* — POST the same `provider_message_id` twice → exactly one `event` created; ledger row prevents the duplicate. ⟵ REQ-07

## Slice 1 — T3: Text intake → extraction → persist (REQ-02, REQ-05, REQ-06, REQ-15)
- **TC-005** *(happy)* — inbound text message → one `event` with `raw_message` set, `status=draft`. ⟵ REQ-02
- **TC-006** *(happy)* — text containing a URL → an `event_metadata(kind='url')` row with the value verbatim. ⟵ REQ-05
- **TC-007** *(empty)* — text with no extractable fields → draft still created; no false metadata rows. ⟵ REQ-02
- **TC-008** *(boundary)* — every extracted field gets a `confidence` in [0,1]; `event.extracted_confidence` equals the minimum of them. ⟵ REQ-06
- **TC-009** *(provenance)* — created event records `origin_type`, origin reference (`submitted_by`), and `created_at`. ⟵ REQ-15

## Slice 1 — T4: WhatsApp reply (REQ-08)
- **TC-010** *(happy)* — after a draft is created, exactly one reply is sent echoing title/start/location/URL labels + the review link with `edit_token`. ⟵ REQ-08
- **TC-011** *(error/empty)* — title unreadable → reply says "couldn't read title" and still includes the review link; low-confidence fields are flagged. ⟵ REQ-08

## T5: Review page — token-gated GET/edit/save (REQ-09, REQ-10)
- **TC-012** *(happy)* — GET `/review/{id}?token=<valid,unexpired>` → fields prefilled; low-confidence fields visibly flagged. ⟵ REQ-09
- **TC-013** *(error)* — GET/POST with a missing/invalid token, or a token for a different event, or an unknown event → rejected. ⟵ REQ-10
- **TC-014** *(boundary)* — GET/POST with an expired token → rejected (write blocked). ⟵ REQ-10, REQ-17
- **TC-015** *(happy)* — valid edit saves the field and sets `updated_at`. ⟵ REQ-10

## T6: Submit / publish / geo / delete (REQ-11, REQ-12, REQ-13)
- **TC-016** *(happy)* — submit with `postal_code` present → `status=published`; `latitude`/`longitude` derived from the postal centroid. ⟵ REQ-11, REQ-13
- **TC-017** *(error/boundary)* — submit with `postal_code` absent → NOT published; actionable error. ⟵ REQ-11
- **TC-018** *(happy)* — after publish, an edit within the token window still succeeds (publish does not lock). ⟵ REQ-11 (AC-021a)
- **TC-019** *(happy)* — delete removes the event and its `event_metadata`; the token no longer resolves afterward. ⟵ REQ-12
- **TC-020** *(boundary)* — publish defaults `country_code` to `US` when unspecified. ⟵ REQ-13

## T7: Image intake + OCR + failure path (REQ-03, REQ-04)
- **TC-021** *(happy)* — inbound image → media fetched, OCR run, extracted text stored, `source_type=image`, `media_ref` set. ⟵ REQ-03
- **TC-022** *(error)* — media download fails → draft still created from caption/text, media marked not-retrieved, reply asks for a resend. ⟵ REQ-04
- **TC-023** *(boundary)* — image with unreadable/empty OCR output → draft created with low confidence, not rejected. ⟵ REQ-03, REQ-06

## T8: Direct-entry web form (REQ-14) `[stretch]`
- **TC-024** *(happy)* — form submission creates a draft of the same shape with `origin_type=user` and no WhatsApp id. ⟵ REQ-14

## T9: Scheduled draft cleanup (REQ-18)
- **TC-025** *(boundary)* — a draft older than 2 months is deleted by the scheduled job; a published event of the same age is untouched. ⟵ REQ-18
- **TC-026** *(boundary)* — a draft just under 2 months old is NOT deleted. ⟵ REQ-18

## Durability (REQ-16)
- **TC-027** *(happy)* `[manual or integration]` — create an event, restart the app, confirm the event persists (MySQL, not memory). ⟵ REQ-16

## End-to-end (manual, over a tunnel — SDD §5)
- **TC-028** *(happy)* `[manual]` — real WhatsApp test number: send 1 text → receive reply with link → open review → submit; send 1 flyer image → OCR draft → reply. ⟵ REQ-01..13

---

## TC ↔ REQ coverage
| REQ | Covered by |
|---|---|
| REQ-01 | TC-001, TC-002, TC-003, TC-028 |
| REQ-02 | TC-005, TC-007 |
| REQ-03 | TC-021, TC-023 |
| REQ-04 | TC-022 |
| REQ-05 | TC-006 |
| REQ-06 | TC-008, TC-023 |
| REQ-07 | TC-004 |
| REQ-08 | TC-010, TC-011 |
| REQ-09 | TC-012 |
| REQ-10 | TC-013, TC-014, TC-015 |
| REQ-11 | TC-016, TC-017, TC-018 |
| REQ-12 | TC-019 |
| REQ-13 | TC-016, TC-020 |
| REQ-14 | TC-024 |
| REQ-15 | TC-009 |
| REQ-16 | TC-027 |
| REQ-17 | TC-014 |
| REQ-18 | TC-025, TC-026 |

**Coverage:** every REQ-01..18 has ≥1 TC; every TC traces to ≥1 REQ. Unhappy paths covered —
empty (TC-007), error (TC-002/011/013/017/022), boundary (TC-004/008/014/020/023/025/026),
auth/expiry (TC-013/014), idempotency (TC-004).

## Open gaps / notes
- Quantitative NFRs (latency/throughput/availability/observability/security hardening) are
  deferred to the **Hardening/NFR phase (P4)** — see OQ-P1-8 in the PRD. No performance TCs in P1.
- The feed query itself is a **P2** (consumer-discovery) feature, so its load testing lands there.
- Exact OCR-accuracy thresholds are deferred (rule-based extraction in P1; see SDD §2).

## Version history
- **v0.1 (2026-10-20):** Initial P1 test-plan skeleton — 28 TC cases grouped by SDD task slice,
  full REQ↔TC coverage table. Cases specified, not yet implemented. Authored during the
  one-file-per-stage split.
