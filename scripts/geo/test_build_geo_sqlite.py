#!/usr/bin/env python3
"""
Tests for build_geo_sqlite.py — TC-P2.1-001..006 (ARANG-TTP-P2-1). Standard library unittest, no network.
Run: python3 scripts/geo/test_build_geo_sqlite.py
"""
import json
import os
import shutil
import sqlite3
import stat
import tempfile
import unittest

import build_geo_sqlite as b

HEADER = "USPS\tGEOID\tANSICODE\tNAME\tFUNCSTAT\tALAND\tAWATER\tALAND_SQMI\tAWATER_SQMI\tINTPTLAT\tINTPTLONG"


def row(usps, geoid, name, lat, lng):
    return "\t".join([usps, geoid, "0", name, "A", "0", "0", "0", "0", str(lat), str(lng)])


BASE_ROWS = [
    row("MA", "2501701605", "Ashland town", 42.2578, -71.4735),
    row("MA", "2502507000", "Boston city", 42.3386, -71.0183),
    row("MA", "2501300660", "Agawam Town city", 42.0696, -72.6155),
    row("MA", "2500107665", "Bourne town", 41.7234, -70.5939),
    row("MA", "2500900000", "County subdivisions not defined", 42.5, -70.8),
    row("RI", "4400100000", "Barrington town", 41.73, -71.31),
]
SEED_SQL = "INSERT INTO town (slug, name, state) VALUES\n  ('ashland-ma', 'Ashland', 'MA', '01721'),\n  ('boston-ma', 'Boston', 'MA', '02108');\n"


class BuildTests(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()
        self.out = os.path.join(self.dir, "out")
        self.seed = os.path.join(self.dir, "seed.sql")
        with open(self.seed, "w") as f:
            f.write(SEED_SQL)

    def tearDown(self):
        for root, _, files in os.walk(self.dir):
            for n in files:
                os.chmod(os.path.join(root, n), stat.S_IWUSR | stat.S_IRUSR)
        shutil.rmtree(self.dir)

    def gaz(self, rows):
        path = os.path.join(self.dir, "gaz.txt")
        with open(path, "w") as f:
            f.write(HEADER + "\n" + "\n".join(rows) + "\n")
        return path

    def build(self, rows, expected=4, previous=None, seed=None, today="2026-11-01"):
        return b.run(self.gaz(rows), self.out, previous, seed or self.seed, expected, today=today)

    def assertNothingPublished(self):
        self.assertFalse(os.path.exists(os.path.join(self.out, "manifest.json")))
        if os.path.isdir(self.out):
            self.assertEqual([n for n in os.listdir(self.out) if n.endswith(".sqlite")], [])

    # TC-001: a row marked MA with a non-Massachusetts Census ID fails; other states are dropped.
    def test_mislabeled_row_fails_and_other_states_are_dropped(self):
        bad = BASE_ROWS[:4] + [row("MA", "4400199999", "Fake town", 41.8, -71.4)]
        with self.assertRaises(b.BuildError):
            self.build(bad, expected=5)
        self.assertNothingPublished()
        m = self.build(BASE_ROWS)        # RI row and the undefined row are dropped
        self.assertEqual(m["places"], 4)

    # TC-002: wrong municipality count fails; the expected count passes.
    def test_count_must_match(self):
        with self.assertRaises(b.BuildError):
            self.build(BASE_ROWS, expected=5)
        self.assertNothingPublished()
        self.assertEqual(self.build(BASE_ROWS, expected=4)["places"], 4)

    # TC-003: every place finds itself by exact name; the file has no ZIP tables.
    def test_every_place_finds_itself_and_no_zip_tables(self):
        m = self.build(BASE_ROWS)
        con = sqlite3.connect("file:" + os.path.join(self.out, m["file"]) + "?mode=ro", uri=True)
        names = [r[0] for r in con.execute("SELECT name FROM place")]
        tables = {r[0] for r in con.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        con.close()
        self.assertEqual(tables, {"place", "meta"})
        for n in names:
            self.assertTrue(b.matches(n, b.normalize(n)), n)

    # TC-004: a previous slug that disappears fails; duplicate slugs fail; previous slugs are kept.
    def test_slug_pinning_and_duplicates(self):
        m = self.build(BASE_ROWS)
        prev = os.path.join(self.dir, "prev.sqlite")
        shutil.copy(os.path.join(self.out, m["file"]), prev)
        os.chmod(prev, stat.S_IWUSR | stat.S_IRUSR)
        con = sqlite3.connect(prev)
        con.execute("UPDATE place SET slug='old-bourne-ma' WHERE name='Bourne'")
        con.commit()
        con.close()
        m2 = self.build(BASE_ROWS, previous=prev, today="2026-11-02")
        con = sqlite3.connect("file:" + os.path.join(self.out, m2["file"]) + "?mode=ro", uri=True)
        self.assertEqual(con.execute("SELECT slug FROM place WHERE name='Bourne'").fetchone()[0], "old-bourne-ma")
        con.close()

        con = sqlite3.connect(prev)
        con.execute("INSERT INTO place (source_id,name,search_name,type,state,slug,latitude,longitude) "
                    "VALUES ('2599999999','Gone','gone','town','MA','gone-ma',42.0,-71.0)")
        con.commit()
        con.close()
        with self.assertRaises(b.BuildError):
            self.build(BASE_ROWS, previous=prev, today="2026-11-03")

        dup = BASE_ROWS[:4] + [row("MA", "2502599999", "Boston town", 42.3, -71.1)]
        with self.assertRaises(b.BuildError):
            self.build(dup, expected=5, today="2026-11-04")

    # TC-005: suffixes are removed; a point outside Massachusetts fails.
    def test_name_cleaning_and_bounds(self):
        self.assertEqual(b.clean_name("Agawam Town city"), "Agawam")
        self.assertEqual(b.clean_name("Bourne town"), "Bourne")
        self.assertEqual(b.clean_name("Boston city"), "Boston")
        self.assertEqual(b.clean_name("North Attleborough Town city"), "North Attleborough")
        self.assertEqual(b.slug_for("Manchester-by-the-Sea"), "manchester-by-the-sea-ma")
        far = BASE_ROWS[:3] + [row("MA", "2500107665", "Bourne town", 40.0, -70.59)]
        with self.assertRaises(b.BuildError):
            self.build(far)

    # TC-006: meta, credits, journal mode, read-only open, read-only file mode, manifest checksum.
    def test_meta_credits_and_read_only(self):
        m = self.build(BASE_ROWS)
        path = os.path.join(self.out, m["file"])
        con = sqlite3.connect("file:" + path + "?mode=ro", uri=True)
        meta = dict(con.execute("SELECT key, value FROM meta"))
        mode = con.execute("PRAGMA journal_mode").fetchone()[0]
        with self.assertRaises(sqlite3.OperationalError):
            con.execute("INSERT INTO meta VALUES ('x','y')")
        con.close()
        self.assertEqual(meta["schema_version"], "1")
        self.assertEqual(meta["data_date"], "2026-11-01")
        credits = json.loads(meta["credits"])
        self.assertIn("Town data: U.S. Census Bureau", credits)
        self.assertTrue(any("GeoNames" in c for c in credits))
        self.assertEqual(mode.lower(), "delete")
        self.assertFalse(os.stat(path).st_mode & stat.S_IWUSR)
        self.assertEqual(m["sha256"], b.sha256_file(path))
        self.assertEqual([n for n in os.listdir(self.out) if n.startswith(".geo-")], [])

    # Seeded towns must resolve.
    def test_seeded_town_missing_fails(self):
        with open(self.seed, "a") as f:
            f.write("INSERT INTO town (slug, name, state) VALUES ('natick-ma', 'Natick', 'MA', '01760');\n")
        with self.assertRaises(b.BuildError):
            self.build(BASE_ROWS)

    # Matching rules shared with the app (SDD section 3.4).
    def test_normalize_and_word_start_matching(self):
        self.assertEqual(b.normalize("Ashland, MA"), "ashland")
        self.assertEqual(b.normalize("ma"), "ma")
        self.assertTrue(b.matches("north and", "north andover"))
        self.assertTrue(b.matches("andover north", "north andover"))
        self.assertFalse(b.matches("land", "ashland"))
        self.assertTrue(b.matches("ASH", "ashland"))


if __name__ == "__main__":
    unittest.main()
