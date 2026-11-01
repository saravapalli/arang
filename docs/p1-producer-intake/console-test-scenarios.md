# Arang — Producer Console: Manual Test Scenarios

> Hands-on scenarios you can run from the **Producer Console** (`http://localhost:8080/`) to
> exercise the P1 producer loop end to end. Each scenario lists what to type, what you should see
> in the console, and what the system does underneath — traced to the requirements
> (`docs/p1-producer-intake/trd.md` REQ-01..18) and the automated test plan (`ttp.md` TC-###).
>
> These were **verified live** against the running app + MySQL on 2026-10-22; the "expected"
> results are observed behavior, not assumptions. Where behavior reflects a P1 limitation
> (minimal extraction, stub OCR), it is called out.
>
> **Setup:** server running on `:8080` against MySQL. The console's "send" calls
> `POST /api/producer/message`, which runs the *same* ingest pipeline as the WhatsApp webhook.

---

## How to run
1. Open `http://localhost:8080/`.
2. Type the message from a scenario, press Enter (or ➤).
3. Compare the reply bubble + the "Open review page →" button against **Expected**.
4. For review/edit scenarios, click **Open review page →** and act there.
5. (Optional) Verify persistence: `mysql -uroot -proot -D arang -e "SELECT id,status,title,postal_code,source_type FROM event;"`

---

## A. Intake scenarios (console → draft + reply)

### SC-01 — Plain text, no links  ·  REQ-02, REQ-06  ·  TC-005, TC-007
- **Type:** `Poetry night next Tuesday`
- **Expected reply:** “✅ Got your event…”, **Title: ⚠️ couldn't read a title — please add one**, *no Links section*, then a review link.
- **Under the hood:** one `event` (status=draft, source_type=text), `extracted_confidence = NULL` (nothing auto-extracted). Typed-core stays empty by design (P1 extracts URLs only; you fill the rest on the review page).

### SC-02 — Message with one URL  ·  REQ-05, REQ-06  ·  TC-006, TC-008
- **Type:** `Live jazz Friday 8pm at The Cellar, tickets https://example.com/jazz`
- **Expected reply:** title “couldn't read” prompt, **Links:** lists `https://example.com/jazz`, review link.
- **Under the hood:** one `event_metadata` row (`kind=url`, value verbatim, `source=text`, confidence 1.00); `event.extracted_confidence = 1.00`.

### SC-03 — Multiple URLs  ·  REQ-05
- **Type:** `Food fest Sat, info https://example.com/food and tickets https://example.com/tix`
- **Expected reply:** **Links:** lists *both* URLs, each verbatim.
- **Under the hood:** two `event_metadata` url rows. (Verified: both captured.)

### SC-04 — URL with trailing punctuation  ·  REQ-05 (extraction robustness)
- **Type:** `See you at the gala (details: https://example.com/gala).`
- **Expected:** the captured link is `https://example.com/gala` — the trailing `)` and `.` are trimmed.

### SC-05 — Empty / whitespace message  ·  input validation
- **Type:** (nothing, or just spaces) and send.
- **Expected:** console shows **⚠️ Please type a message.**; no event is created.
- **Under the hood:** endpoint returns `{"ok":false}`; event count unchanged.

### SC-06 — Long / multi-line flyer text  ·  REQ-02
- **Type (use Shift+Enter for new lines):**
  ```
  SUMMER MARKET
  Saturday 10am–4pm, Riverside Park
  Local crafts, food trucks, live music
  Info: https://example.com/market
  ```
- **Expected:** draft created; full text stored as `raw_message`; the URL extracted; title still “couldn't read” (no title heuristics in P1).

---

## B. Review / edit scenarios (click “Open review page →”)

### SC-07 — Review page renders prefilled  ·  REQ-09  ·  TC-012
- From any SC-01..06 reply, click **Open review page →**.
- **Expected:** the review form opens (HTTP 200) with a ⚠️ banner when nothing was auto-read; your captured links/raw text are present.

### SC-08 — Edit and save fields  ·  REQ-10  ·  TC-015
- On the review page, set **Title** = `Live Jazz Night`, **Location** = `The Cellar`, **Postal code** = `10001`; click **Save changes**.
- **Expected:** “✅ Saved.” and the fields persist (reload shows them).

### SC-09 — Publish requires a postal code  ·  REQ-11  ·  TC-016, TC-017
- On a draft with **no** postal code, click **Submit / Publish**.
- **Expected:** error banner **“A postal code is required to publish.”**; status stays `draft`. (Verified.)
- Then add postal `10001` → **Submit / Publish** → **Expected:** “✅ Published!”, status `published`, and lat/long derived (`10001` → ~40.7507, −73.9966). (Verified.)

### SC-10 — Publish with an unknown postal code  ·  REQ-13, DD-3
- Use postal `99999` (not in the bundled centroid set) and publish.
- **Expected:** publishes successfully; lat/long remain empty (approximate geo is best-effort).

### SC-11 — Editing still allowed after publish  ·  AC-021a  ·  TC-018
- Publish an event, then change the Title and Save.
- **Expected:** the edit succeeds (publishing does not lock the token window).

### SC-12 — Delete an event  ·  REQ-12  ·  TC-019
- On the review page, click **Delete this event**.
- **Expected:** “Event deleted” page; the event + its metadata are gone; reopening the old review link now returns **404**.

---

## C. Security / negative scenarios (URL tampering)

### SC-13 — Invalid token  ·  REQ-10  ·  TC-013
- Take a working review URL and change the `token` value.
- **Expected:** **HTTP 404** (the page does not reveal whether the event exists). (Verified.)

### SC-14 — Unknown event id  ·  REQ-10  ·  TC-013
- Visit `/review/999999?token=anything`.
- **Expected:** **HTTP 404**.

### SC-15 — Missing token  ·  REQ-10
- Visit `/review/{id}` with no `?token=`.
- **Expected:** **HTTP 404**.

### SC-16 — Expired token  ·  REQ-17  ·  TC-014
- Not reproducible from the console in real time (24h TTL). Covered by the automated test
  `ReviewControllerTest.getWithExpiredTokenIs404`. Expected behavior: writes blocked, page 404s
  after expiry; the producer resubmits.

---

## D. Scenarios the console cannot fully exercise (noted for honesty)

| Scenario | Why not from the console | Where it's covered |
|---|---|---|
| **Image / OCR intake** (REQ-03/04) | The console sends text only; image intake needs a media id + the WhatsApp media endpoint (OCR is a stub in P1). | `ImageIngestTest` (success + REQ-04 failure path). |
| **WhatsApp verify handshake / idempotency** (REQ-01/07) | These are webhook-level, not console actions. | `WebhookControllerTest`; or `curl` the webhook directly. |
| **Real WhatsApp reply delivery** (REQ-08) | No live Cloud API credentials; the reply is composed + shown in the console but not transmitted. | `ReplyComposerTest` + `WebhookReplyWiringTest`. |
| **Scheduled draft cleanup** (REQ-18) | 2-month TTL; not real-time. | `CleanupJobTest` (parameterized cutoff). |

---

## Quick cross-check (optional)
After running A–C, inspect the data:
```bash
mysql -uroot -proot -D arang -e "SELECT id,status,source_type,title,postal_code FROM event;"
mysql -uroot -proot -D arang -e "SELECT event_id,\`key\`,\`value\`,kind,source FROM event_metadata;"
```

## Traceability summary
Intake SC-01..06 → REQ-02/05/06; Review SC-07..12 → REQ-09/10/11/12/13; Security SC-13..16 →
REQ-10/17. Image/webhook/reply/cleanup (Section D) are exercised by the automated suite
(45 tests, all green) rather than the console.
