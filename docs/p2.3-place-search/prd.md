---
title: Arangly — Place Search Extensions (P2.3)
type: PRD
artifact_id: ARANG-PRD-P2-3
parent: ARANG-INIT-001
status: placeholder
version: 0.1
driver: (you)
approver:
approver_role: product owner / driver
assigned_date: 2026-11-01
last_revised_date: 2026-11-01
approved_date:
review_provenance: none — placeholder that holds deferred scope and evidence. Not reviewed.
related_docs:
  - docs/p2.1-town-entry/prd.md (P2.1 PRD v1.6 — town-name search; this phase extends it)
  - docs/p2.1-town-entry/sdd.md (P2.1 SDD v0.6 — DD-P2.1-4 holds the T1 source evaluation; DD-P2.1-11 in-memory search)
origin: Created 2026-11-01 when the driver moved ZIP search and village/neighborhood names out of P2.1 ("add it later").
tags: [placeholder, place-search, zip, villages, neighborhoods, deferred]
---

# Arangly — Place Search Extensions (P2.3) — PLACEHOLDER

> **Status: placeholder.** This file keeps scope that was moved out of P2.1 so it is not lost. It is **not** a PRD yet:
> it has no user flows, acceptance criteria, or sign-off. Write the full PRD (with the `prd-author` skill) when this
> phase is scheduled.
>
> **Intention (draft):** a visitor can reach their town even when they think of it as a ZIP code or as a village or
> neighborhood name, not as the municipality name.

## 1. Why this phase exists
P2.1 searches the 351 Massachusetts municipalities **by name only** (driver decision, 2026-11-01). Two gaps remain:
1. **ZIP search.** A visitor cannot type `01721` to find Ashland.
2. **Village and neighborhood names.** A visitor who types "Saxonville" (Framingham) or "Jamaica Plain" (Boston) sees
   "not found in Massachusetts" in P2.1, which is wrong and may lose that visitor.

## 2. Candidate scope (to be confirmed when the PRD is written)
- **Village and neighborhood names that lead to their town** — for example Saxonville → Framingham, Jamaica Plain →
  Boston, Newton Centre → Newton. *Recommended first,* because it fixes a false "not found" message and needs no ZIP
  data. A curated list is enough to start; a data source can follow.
- **Search by ZIP** — typing a ZIP (or a ZIP+4) lists the town or towns that ZIP covers.
- Out of scope here: places outside Massachusetts (a region expansion is its own decision), and address search (P2.2).

## 3. Benefits and costs recorded on 2026-11-01

**Benefits of ZIP search**
- Some people type five digits faster than a long town name ("Manchester-by-the-Sea").
- People in villages or neighborhoods often know their ZIP better than the municipality name.
- A ZIP has no spelling variants ("Marlborough" / "Marlboro", "Foxborough" / "Foxboro").
- Many event and shopping sites ask for a ZIP, so some visitors expect it.

**Costs of ZIP search**
- A second data source and ZIP-to-town linking in the build.
- **165 ZIPs touch two or more towns** (many through tiny overlaps), so the build needs a minimum-overlap rule, and some
  searches return several towns.
- About four more checks and a larger build script.

**Benefit of village and neighborhood names**
- Fixes the false "not found in Massachusetts" message for people who use those names. This matters more than ZIP
  search for the live towns.

## 4. Evidence from the P2.1 T1 evaluation (2026-11-01, files were in `/tmp/arang-t1`, not committed)
- **US Census 2020 ZCTA ↔ county-subdivision relationship file**
  (`www2.census.gov/geo/docs/maps-data/data/rel2020/zcta520/tab20_zcta520_cousub20_natl.txt`): 540 Massachusetts ZIP
  areas (ZCTAs); one (`02861`) crosses into Rhode Island. Four municipalities (Amesbury, Easthampton, Methuen,
  Watertown) changed Census IDs after 2020, so the join to the 2024 town list must use names, not IDs.
- **ZCTA is not the same as ZIP:** 143 Massachusetts ZIPs in the bundled `zip-centroids.csv` are not ZCTAs (PO-box and
  single-organization ZIPs, for example `01703` Framingham, `01101` Springfield).
- **GeoNames US postal codes** (`download.geonames.org/export/zip/US.zip`): 685 Massachusetts ZIPs **with state**,
  including PO-box ZIPs; `05501` and `05544` are correctly Andover, MA. Its place names are postal city names: 488 name a
  municipality and 197 name a village or neighborhood (for example Jamaica Plain, Allston, Saxonville-type names). Those
  197 names are a ready-made **starting list for village and neighborhood search**. License: **CC BY 4.0**, credit with a
  link to geonames.org (verified in its `readme.txt`).
- **Combined result:** Census ZCTA links + GeoNames ZIPs named for a municipality give **every one of the 351
  municipalities at least one ZIP**; 636 ZIPs linked; every seeded town's ZIP links to that town.
- **Do not derive state from a ZIP prefix:** `05501`/`05544` are Massachusetts, and prefix `02` includes Rhode Island
  (`028xx`–`029xx`).
- **OpenStreetMap** has almost no ZIP data on town boundaries, so it does not help here.

## 5. Design notes for the later SDD (non-binding)
- Add ZIP and alias tables to the same offline-built, read-only SQLite place file (P2.1 SDD §3); the API server still
  only reads it. Search stays in memory unless the row count grows a lot (P2.1 DD-P2.1-11).
- An alias row maps a village or neighborhood name to its municipality slug; search shows it as
  "Saxonville (Framingham), MA" and opens the Framingham page.
- Choose a minimum land-overlap share before linking a ZIP to a town, to avoid sliver matches.
- P2.1 already returns `hint: type_town_name` for digits-only input; this phase replaces that hint with real results.

## 6. Version history
- **v0.1 (2026-11-01) — PLACEHOLDER.** Created when the driver moved ZIP search and village/neighborhood names out of
  P2.1. Holds the reasons, the benefit/cost notes, and the T1 evidence. No requirements yet.
