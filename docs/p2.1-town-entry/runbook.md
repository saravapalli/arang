# P2.1 operator runbook

Short operator procedures for Town Entry & Coverage (ARANG-SDD-P2-1 task T19). All SQL runs against the
target database: `arang_dev` on dev, `arang` on prod. Make a prod change only during an explicit prod release.

## Make a town live
A town shows events and appears in search and the sitemap only when it has a `live` row in the `town` table.
Take the values from the place file so they match search.

```sql
-- Look up the town's data in the place file (read-only), or copy from a seeded row as a template.
INSERT INTO town (slug, name, state, postal_code, latitude, longitude, map_zoom, status)
VALUES ('gloucester-ma', 'Gloucester', 'MA', '01930', 42.6159, -70.6620, 13, 'live');
```
The change takes effect within ~60 seconds (the page/sitemap cache window). A startup warning is logged if a
live `town` row's slug is missing from the place file or its state disagrees.

## Hide a town
```sql
UPDATE town SET status = 'hidden' WHERE slug = 'some-town-ma';
```
Its page becomes the coming-soon state, and it leaves search and the sitemap (within the cache window).

## Rename a town (keep old links working)
```sql
-- Record the old slug as an alias BEFORE changing the slug, so /towns/<old> redirects to the new one.
INSERT INTO town_slug_alias (alias, town_id) SELECT 'old-slug-ma', id FROM town WHERE slug = 'new-slug-ma';
```
Slugs are pinned across place-file rebuilds, so a routine rebuild will not change an existing slug.

## Remove a person's waitlist data (on request)
Deleting the user cascades to their demand signals.
```sql
DELETE FROM `user` WHERE email = 'person@example.com';
```
A waitlist-only user (no phone, no web cookie id, no interests) is also removed automatically 24 months after
their last signal by `WaitlistRetentionJob`.

## See waitlist demand for a town
```sql
SELECT place_label, COUNT(*) AS wishes
FROM demand_signal
WHERE kind = 'town_waitlist'
GROUP BY place_label ORDER BY wishes DESC;
```
Never copy email addresses into logs, tickets, or screenshots.

## Rebuild / refresh the place file
```bash
python3 scripts/geo/build_geo_sqlite.py --previous .release/geo/current.sqlite   # pins existing slugs
```
`scripts/release.sh --prod` does this automatically and points `ARANG_GEO_FILE` at `.release/geo/current.sqlite`.
If the place file is missing at startup, the app still runs: town search returns "unavailable" and the landing
offers the built-in live towns (`/health` shows `geo.status = UNAVAILABLE`).
