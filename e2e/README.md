# Arang E2E (Playwright + DB verification)

End-to-end tests that drive the **real UI** (Chromium via Playwright) and then **verify the rows
in MySQL** so we know the UI actually persisted the right data across tables.

## Prerequisites
- **MySQL** running with the `arang` DB (host creds `root/root` by default).
- **The app** running on `http://localhost:8080` — for deterministic tests run it with LLM **off**:
  ```bash
  cd /Applications/Repos/arang
  export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/temurin-25/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
  ARANG_WHATSAPP_VERIFY_TOKEN=demo-token ARANG_LLM_ENABLED=false \
    java -jar build/libs/intake-0.0.1-SNAPSHOT.jar
  ```
- **Node 20+** (Playwright requirement). This machine's default is Node 16; a prebuilt Node 20 was
  placed at `/tmp/node20` — put it on PATH: `export PATH="/tmp/node20/bin:$PATH"`.

## Run
```bash
cd e2e
npm install                       # first time
npx playwright install chromium   # first time
npx playwright test               # run all specs
npx playwright show-report        # open the HTML report
```
Config/overrides: `ARANG_BASE_URL`, `ARANG_DB_HOST/PORT/USER/PASSWORD/NAME`.

## What the specs verify (UI → DB)
- **01-text-intake** — typing an event in the console creates a `draft` with correct
  `source_type`/`origin_type`/`raw_message`/`edit_token`/`submitted_by`; the submitter `user` row
  exists; the URL lands in `event_metadata` (kind=url, verbatim). Review page renders for a valid
  token; an invalid token → 404.
- **02-review-publish-delete** — editing fields persists; publishing requires a postal code then
  sets `status=published` and derives `latitude`/`longitude` (geo); deleting removes the event +
  its metadata and the token stops resolving (404).
- **03-web-form** — the `/submit` form creates a `user`-origin draft (`submitted_by` null, no
  WhatsApp id), redirects to the review page, and captures the details URL as metadata.

## Coverage notes (honest scope)
These tables are **verified** because P1 writes them: `event`, `event_metadata`, `user`.
These are **not** exercised and here's why:
- `event_field_confidence` — only written by LLM enrichment (run with `ARANG_LLM_ENABLED=true` +
  a Gemini/Ollama provider to populate it; left off here for determinism).
- `user_role` — P1 does not assign roles (by design; future auth phase).
- `business`, `business_metadata`, `ingestion_source`, `api_client`, `category`, `tag`,
  `event_category`, `event_tag`, `interest`, `feedback`, `conversation`, `message` — **structure-
  ahead P2/P3 tables. No P1 code path writes to them yet**, so there is nothing to drive from the
  UI. They cannot be E2E-verified until those features exist.
