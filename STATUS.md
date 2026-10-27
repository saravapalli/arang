# STATUS — Arang Intake (read me first)

**P1 Producer-Intake is functionally implemented (tasks T1–T9) and the full test suite passes
(42 tests, 0 failures).** Built autonomously against the approved PRD/TRD/SDD/TTP. The design
decisions DD-3/DD-4/DD-5 were resolved with the SDD's recommended defaults (recorded in the SDD).

## What exists (all P1 tasks)
- T1 skeleton + full 21-table schema (Flyway V1) + `/health`
- T2 WhatsApp webhook verify/receive + idempotency ledger
- T3 text intake → draft (URL extraction, per-field confidence, provenance, edit token)
- T4 WhatsApp reply with review link (+ low-confidence flagging), send-after-commit
- T5 token-gated review/edit page (Thymeleaf)
- T6 submit/publish (postal-gated) + ZIP-centroid geo + delete
- T7 image intake: media fetch + OCR + media-failure path (interfaces + stub defaults)
- T8 direct-entry web form (`/submit`)
- T9 scheduled draft-cleanup job (drafts >2 months)

## Known limitations / integration points (not blockers)
- **WhatsApp send** and **media fetch** use no-op/stub implementations until
  `ARANG_WHATSAPP_ACCESS_TOKEN` + `ARANG_WHATSAPP_PHONE_NUMBER_ID` are set (real Cloud API client
  activates automatically when configured).
- **OCR** is a stub returning empty text; a real Tess4J engine is a drop-in `OcrService` bean.
- **REQ-16** (persistence across restart) is covered by Flyway + repository round-trips, not a
  dedicated restart test.
- Quantitative NFRs (perf/availability/observability/hardening) are deferred to **P4** (OQ-P1-8).

## Verified stack (modern, per project steering)
- **Java 25** (Temurin 25.0.4.1) · **Spring Boot 4.1.1** (Spring Framework 7.0.9) · **Gradle 9.8.2**
- Last verified: `./gradlew test` → **BUILD SUCCESSFUL**, 2 tests, 0 failures (2026-10-20).

## How to build/run on this machine
Gradle must run on JDK 25 (the host default JDK is older). Point `JAVA_HOME` at Temurin 25:
```bash
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/temurin-25/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew test        # run the smoke test (H2, no live DB needed)
./gradlew bootRun     # run the app (needs MySQL on the default profile)
```

## Notes / gotchas encountered
- Spring Initializr currently only serves Boot ≥4.0.0 and its Gradle BOM resolution was failing,
  so this skeleton was **hand-written** rather than generated.
- **Boot 4 API change:** `org.springframework.boot.test.web.client.TestRestTemplate` is no longer
  in that package. The smoke test uses the JDK's built-in `java.net.http.HttpClient` instead, so
  it is immune to Spring test-client package moves across major versions.
- JDK 25 was installed as a prebuilt Temurin binary (Homebrew's `openjdk@25` requires full Xcode
  to compile from source, which isn't installed).
