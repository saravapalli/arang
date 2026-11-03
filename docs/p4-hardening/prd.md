---
title: Arangly — Hardening / Non-Functional Requirements (P4)
type: PRD
artifact_id: ARANG-PRD-P4
parent: ARANG-INIT-001
status: placeholder
version: 0.1
driver: (you)
approver:
approver_role: product owner / driver
assigned_date: 2026-11-01
last_revised_date: 2026-11-01
approved_date:
review_provenance: none — placeholder that collects launch-blocking items. Not reviewed.
related_docs:
  - docs/prd.md (initiative — Epic 5 / P4, "future hardening/NFR PRD")
  - docs/p2-consumer-discovery/sdd.md (DD-P2-4: OSM tiles; move to a keyed or self-hosted provider before real traffic)
  - docs/p2.1-town-entry/sdd.md (DD-P2.1-7 throttle per instance; open item: production client identity)
tags: [placeholder, hardening, nfr, scalability, tiles, launch-blocking]
---

# Arangly — Hardening / Non-Functional Requirements (P4) — PLACEHOLDER

> **Status: placeholder.** This file collects items that must be solved before real traffic. It is **not** a PRD yet:
> it has no user flows, acceptance criteria, measurable targets, or sign-off. Write the full PRD when P4 starts.

## 1. Launch-blocking items

### H-1 — Map tile source (recorded 2026-11-01)
**Today (checked in the code):** the map shows OpenStreetMap tiles through our proxy `/tiles/{z}/{x}/{y}.png`
(`TileProxyController`). Each tile is fetched once from `tile.openstreetmap.org` with an identifying User-Agent, stored on
local disk, and returned with a 30-day public cache header. The map shows "© OpenStreetMap contributors".

**Why it does not scale yet:**
1. The cache folder defaults to the system temp folder (`/tmp/arang-tiles`), so a restart or a new container starts empty.
   *(Fixed for dev on 2026-11-01: `scripts/dev.sh` sets `ARANG_TILES_CACHE_DIR` to `data/tiles`. Prod still uses the
   temp folder until the next explicit prod release sets it.)*
2. Each server keeps its own cache, so several servers fetch the same tiles again.
3. The OSM license FAQ (read on osmfoundation.org, 2026-11-01) says OSM's tile servers are not intended for commercial
   use, and heavy use breaks the tile usage policy.

**Options (to decide in P4, or earlier if traffic grows):**
- A pre-filled Massachusetts tile cache in object storage behind a CDN. *Must be checked against OSM's bulk-download
  rules first (not checked).*
- Self-hosted tiles built from OSM data (for example one vector-tile file in object storage behind a CDN). No outside
  calls at run time; most build effort; ODbL attribution still applies.
- A paid tile provider (ongoing cost; they handle scale and policy).
- In all cases, an edge cache (Cloudflare) for `/tiles/` reduces calls, because responses already carry long cache
  headers.

**Done when (draft):** no visitor request causes a call to `tile.openstreetmap.org`, and the tile source has a written
usage basis (own data, a contract, or a policy that allows our volume).

### H-2 — Production client identity for throttling (from P2.1)
The waitlist throttle needs a trusted client header per deployment (P2.1 SDD DD-P2.1-7, open item 3). The AWS front door
is not decided. Settle it with the AWS deployment design.

## 2. Other P4 themes (from the initiative overview, not yet detailed)
Performance, availability, scalability, observability, security hardening, backups and restore tests, uptime monitoring
and automatic restart (also listed in the 2026-11-01 site-review checklist, section 8).

## 3. Version history
- **v0.1 (2026-11-01) — PLACEHOLDER.** Created to record the map-tile scaling item (H-1) and the throttle client-identity
  item (H-2) as launch-blocking. No requirements yet.
