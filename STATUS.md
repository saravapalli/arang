# STATUS — Arang Intake (read me first)

**P2.1 Town Entry & Coverage is built (tasks T1–T19) on dev and pushed; the full backend suite passes
(150 tests, 0 failures) plus a real-MySQL concurrency test and 6 Playwright E2E tests.** Built against the
approved PRD v1.6 / TRD v0.7 / SDD v0.6 / TTP v0.3 after three independent review gates. All on dev (:8081,
`arang_dev`); prod (:8080) is unchanged and takes P2.1 only on an explicit `scripts/release.sh --prod`.

Earlier phases: **P1 Producer-Intake** and **P2 Consumer Discovery** are built and green.

## What P2.1 added (town entry, coverage, waitlist, discoverability)
- Town coverage as data: `town`, `town_slug_alias` tables; a town is live/hidden by a row, not a release.
- Town-name search over the 351 Massachusetts municipalities (US Census list), from a read-only SQLite place
  file built offline by `scripts/geo/build_geo_sqlite.py`. Digits-only input shows a "type a town name" hint
  (ZIP search is deferred to P2.3).
- Server-rendered pages: `/` and `/towns/{slug}` with title, canonical URL, and events in the HTML (so
  crawlers see them); coming-soon + noindex for a resolvable not-live town; 404-with-landing for unknown
  slugs; case/slash redirects. `/sitemap.xml` and `/robots.txt`.
- Waitlist: `POST /api/waitlist` stores demand against `user.email` + `demand_signal` (idempotent, unverified,
  no email in logs), with a per-client + global throttle and a daily 24-month retention job.
- SPA wired to all of the above (search, near-me, coming-soon, waitlist form, nothing-on state with nearby
  towns, remember-town, "Arangly" brand). `/health` reports the place-data status and date.

## P2.1 known limitations / deferred (not blockers)
- **Place data:** Census town list + GeoNames ZIP centroids (for map pins). ZIP search and village/neighborhood
  names are deferred to **P2.3** (`docs/p2.3-place-search/prd.md`).
- **Tiles + throttle at scale:** the OSM tile source and the production client-IP header are launch-blocking
  hardening items in **P4** (`docs/p4-hardening/prd.md`). The SQLite driver was verified on macOS; a Linux-image
  run is the T19 deploy check.
- **Event detail accuracy/trust** (dates, address geocoding, actions): **P2.2** (`docs/p2.2-event-detail-trust`).

## P1 (unchanged, still true)
- T1 skeleton + full schema (Flyway V1) + `/health`; T2 webhook; T3 text intake; T4 reply; T5 review page;
  T6 submit/publish + geo + delete; T7 image intake; T8 web form (`/submit`); T9 draft-cleanup job.
- WhatsApp send, media fetch, and OCR remain stub/no-op until configured.

## Verified stack (modern, per project steering)
- **Java 25** (Temurin 25) · **Spring Boot 4.1.1** · **Gradle 9.8.2** · **sqlite-jdbc 3.54.1.0** · Jackson 3.
- Last verified: `./gradlew test -PdevBuildDir=build-dev` → **BUILD SUCCESSFUL, 150 tests, 0 failures**
  (2026-11-04); `./gradlew mysqlIntegrationTest` green; 6 P2.1 + 2 P2 E2E tests pass on dev.


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
