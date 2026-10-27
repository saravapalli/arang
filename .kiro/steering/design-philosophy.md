# Arang — Design Philosophy (Generality First)

Arang is intended to be a broad, long-lived event platform. Design every data model
and feature so that **new use cases become new data, not new schema**. When proposing
ideas, schemas, or code, default to the general shape over the narrow one, and surface
the generalization explicitly so the user can confirm or narrow it.

## Core principle: the event is the universal unit
- `event` is the central primary entity: "a thing that can appear in a user's feed."
- An event's **kind is data, not schema**: an `event_type` lookup row distinguishes
  concert / offer / workshop / festival / class / whatever comes next. Adding a new
  kind = INSERT a row, never a migration.
- Events **may or may not** be tied to other entities (e.g. a `business`), and *whether*
  they are can depend on the event's type. Model such links as optional (nullable FKs
  or join tables), never as assumptions baked into the core.
- **Intake source is data, not schema.** An event can arrive from many origins — a human
  over WhatsApp, an automated pipeline (newspaper/bulletin/listings/PDF/API/web), or an
  inbound push-API client. Model origin generically: `origin_type`
  (`user` | `ingestion_source` | `api_client`) + the appropriate FK. Adding a new intake
  path must not require changing the `event` table.
- **Ingestion is a pluggable connector framework.** Many connectors (by source *shape*:
  push API, pull API, structured web, unstructured web, document/PDF, image,
  messaging/social) feed one shared pipeline: acquire → normalize → extract → dedupe →
  store. Only the acquire/normalize front half differs per connector; the back half and
  the `event`/provenance model are shared. New source = new connector behind a stable
  interface, never a pipeline or schema change. Messaging/social connectors are
  **compliance-gated** (official APIs or legal review; scraping assumed non-viable).
- We do not know all future use cases. Prefer models that can absorb unknown ones
  without altering primary tables.

## Three storage strategies — put each datum in the right one
1. **Typed columns on primary entities** — only for data we filter/sort/display on
   nearly every request (e.g. event title, start/end time, location). Fast, indexed,
   correctness-enforced.
2. **Per-entity key/value metadata tables** (`event_metadata`, `user_metadata`,
   `business_metadata`, …) — the escape hatch for anything unpredictable, including any
   field found in OCR text. Shape: `(entity_id FK, key, value, kind)`. `kind` is a light
   type hint (url/text/date/number). Duplicate keys allowed. New fields = new rows.
3. **Lookup + join tables** (`event_type`, `category`, `tag`, `user_role`) — shared
   controlled vocabulary used consistently across many entities, especially to drive
   discovery and authorization.

## User identity vs. role (plan for many user types)
- `user` is **identity only** (id, phone, name, timestamps) — never a `user_type` column.
- A user's **role is data, not schema**: model it as a many-to-many `user_role`
  `(user_id, role)` so one person can be producer AND consumer (and later operator,
  business_owner, api_client, moderator, …). New user kinds = new role *values*, never a
  migration. Role is the column that future **authentication/authorization** gates on.
- Design the role table into the schema up front; populate/enforce it only when a feature
  needs it (P1 has producers only and need not read roles yet).

## What NOT to do
- Do **not** collapse everything into generic EAV soup. The *core* of a primary entity
  stays typed and queryable; only the unpredictable extras go to metadata.
- Do **not** create a new primary table for every variation (offer, deal, promo…) when
  it is really a *type* of event. Reach for `event_type` first.
- Do **not** hardcode narrow fields (e.g. a single `purchase_url` column) when the datum
  is really one of many possible labeled values — capture it as metadata with its
  source label verbatim; defer interpretation (often to an LLM at serve time).

## How to work on Arang
- The **schema is the deliberate, long-lived artifact.** Treat schema changes as rare
  and significant; design to avoid future migrations.
- When unsure between general and specific, present the general option, name the
  tradeoff, and ask. Record the user's product decisions in `docs/prd.md`.
- Prior repos `samro` and `artha` are **idea sources only** — no code is reused.
- Confirmed stack/tooling: Java + Spring Boot, **MySQL**, **Gradle**, WhatsApp Business
  Cloud API (test mode), local hosting via a tunnel for the prototype.
