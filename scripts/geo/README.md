# Place file build (P2.1 town search)

Builds the read-only SQLite place file that the app loads at startup for town search
(`docs/p2.1-town-entry/sdd.md` §3, task T2). **Offline only. The API server never runs this.**

## What it does
1. Gets the US Census 2024 gazetteer of county subdivisions (download, or a local file).
2. Keeps the 351 Massachusetts municipalities and cleans the Census suffixes ("Agawam Town city" → "Agawam").
3. Builds search names and slugs (`ashland-ma`). Slugs from a previous file are kept, so a published URL never changes.
4. Writes `geo-YYYY-MM-DD.sqlite` (`journal_mode=DELETE`) with a `meta` table: schema version, dates, source URL and
   checksum, credits, license text.
5. Validates. **If any check fails, it publishes nothing and exits 1.**
6. Marks the file read-only and writes `manifest.json` (file name + SHA-256).

## Checks
Exactly 351 rows · every Census ID is a Massachusetts ID (prefix `25`) · every point inside the Massachusetts bounds ·
no empty names · no suffix left · slugs unique and well-formed · every slug of the previous file still exists · every
town seeded in `V3__p2_1_town_entry.sql` resolves with state MA · every place finds itself by exact name under the
search rules · credits present · the file reopens read-only with the right schema version and journal mode.

## Run
```bash
python3 scripts/geo/build_geo_sqlite.py                                  # download + build into data/geo/
python3 scripts/geo/build_geo_sqlite.py --source /path/to/2024_Gaz_cousubs_national.zip
python3 scripts/geo/build_geo_sqlite.py --previous data/geo/geo-2026-11-03.sqlite   # pin slugs from the last file
python3 scripts/geo/test_build_geo_sqlite.py                             # unit tests (no network)
```
Python 3.7+, standard library only. Output goes to `data/geo/` (git-ignored).

## Use it in dev
Point the app at the file: `ARANG_GEO_FILE=$PWD/data/geo/geo-YYYY-MM-DD.sqlite` (read by the app from task T4 on).

## Publish and deploy
Uploading the file and manifest to object storage, and copying it onto servers, is the deploy step (task T19).

## Credits (shown on the site)
- Town data: U.S. Census Bureau (U.S. Government work; the Census Bureau asks for a source citation).
- ZIP data used for event map pins: GeoNames (geonames.org), Creative Commons Attribution 4.0.

## Refresh
Census publishes gazetteer files yearly. Rebuild with `--previous` so slugs stay the same.
