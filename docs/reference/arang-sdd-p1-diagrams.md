# P1 Producer-Intake — SDD Diagrams (reference copy)

> Illustrative Mermaid diagrams of the P1 design (ARANG-SDD-P1). **These now live canonically in
> the SDD itself** (`docs/producer-intake/sdd.md` §4); this file is a standalone reference copy.
> The SDD is the source of truth — if they diverge, the SDD wins.

## A. ER diagram — the P1 schema (§2)

```mermaid
erDiagram
    user ||--o{ user_role : has
    user ||--o{ event : "submitted_by"
    event_type ||--o{ event : classifies
    event ||--o{ event_metadata : "has (CASCADE)"
    event ||--o| processed_message : "dedupe ledger"

    user {
        bigint id PK
        varchar phone_number UK "WhatsApp id; null for form-only"
        varchar name
    }
    user_role {
        bigint id PK
        bigint user_id FK
        varchar role "producer|consumer (data, not schema)"
    }
    event_type {
        bigint id PK
        varchar name UK "concert|offer|workshop..."
    }
    event {
        bigint id PK
        bigint event_type_id FK "nullable in P1"
        bigint submitted_by FK
        varchar title "nullable"
        datetime starts_at
        varchar postal_code "required to PUBLISH"
        varchar source_type "text|image|link|mixed"
        decimal extracted_confidence "min roll-up"
        varchar status "draft|published"
        varchar origin_type "user|ingestion_source|api_client"
        char edit_token UK
        datetime edit_token_expires_at
    }
    event_metadata {
        bigint id PK
        bigint event_id FK
        varchar key "source label verbatim"
        text value
        varchar kind "url|text|date|number"
        decimal confidence "per-field 0-1"
    }
    processed_message {
        varchar provider_message_id PK
        bigint event_id
    }
```

## B. Sequence diagram — WhatsApp intake (REQ-01/02/03/04/05/06/07/08)

```mermaid
sequenceDiagram
    participant WA as WhatsApp Cloud API
    participant WC as WebhookController
    participant IS as IngestService
    participant PM as processed_message
    participant EX as ExtractionService
    participant OCR as OcrService (Tess4J)
    participant DB as MySQL (event, event_metadata)
    participant WAC as WhatsAppClient

    WA->>WC: POST /webhook (message payload)
    WC->>IS: handle(payload)
    IS->>PM: seen(provider_message_id)?
    alt already processed (REQ-07)
        PM-->>IS: yes
        IS-->>WC: 200 (no duplicate)
    else new message
        PM-->>IS: no
        alt image message (REQ-03)
            IS->>OCR: fetch media + OCR
            alt media fetch fails (REQ-04)
                OCR-->>IS: failure
                IS->>DB: draft from caption, media not-retrieved
                IS->>WAC: reply "couldn't fetch — please resend"
            else ok
                OCR-->>IS: extracted text
                IS->>EX: extract(text)
            end
        else text message (REQ-02)
            IS->>EX: extract(raw_message)
        end
        EX-->>IS: typed-core + metadata + per-field confidence (REQ-05/06)
        IS->>DB: INSERT event(status=draft) + event_metadata + ledger row
        IS->>WAC: reply: echo fields + low-confidence flags + review link (REQ-08)
        WAC->>WA: sendText(to, body + edit_token link)
    end
```

## C. State diagram — event lifecycle + token/cleanup (REQ-11/17/18)

```mermaid
stateDiagram-v2
    [*] --> draft : intake (WhatsApp / form)
    draft --> draft : edit within token window (REQ-10)
    draft --> published : submit, postal_code present (REQ-11)
    published --> published : edit within token window (publish does NOT lock, REQ-21a)
    draft --> [*] : delete (REQ-12) / cleanup after 2 months (REQ-18)
    published --> [*] : delete (REQ-12)

    note right of draft
        token expiry (REQ-17):
        after expiry, writes blocked,
        no WhatsApp message sent
    end note
```

## D. Component diagram — com.arang.intake (§4) + external edges

```mermaid
flowchart TB
    subgraph ext [External]
        WA[WhatsApp Cloud API]
        TESS[Tess4J OCR]
        GEO[Postal-centroid lookup]
    end

    subgraph controller [controller/]
        WC[WebhookController]
        RC[ReviewController]
        FC[FormController]
    end
    subgraph service [service/]
        IS[IngestService]
        EX[ExtractionService]
        OCR[OcrService]
        PGS[PostalGeoService]
        WAC[WhatsAppClient]
        CJ[CleanupJob @Scheduled]
    end
    subgraph repo [repo/]
        ER[EventRepository]
        EMR[EventMetadataRepository]
        UR[UserRepository]
        PMR[ProcessedMessageRepository]
    end
    DB[(MySQL)]

    WA --> WC
    WC --> IS
    RC --> ER
    FC --> IS
    IS --> EX --> OCR --> TESS
    IS --> WAC --> WA
    RC --> PGS --> GEO
    IS --> ER & EMR & PMR
    CJ --> ER
    ER & EMR & UR & PMR --> DB
```
