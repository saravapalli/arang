#!/usr/bin/env python3
"""
Build the read-only place file for Arangly P2.1 town search (ARANG-SDD-P2-1 section 3.2, task T2).

Offline only. The API server never runs this script. It:
  1. gets the US Census county-subdivision gazetteer (download or local file),
  2. keeps the Massachusetts municipalities and cleans their names,
  3. builds search names and slugs (pinned to the previous file's slugs),
  4. writes a SQLite file in journal_mode=DELETE with a meta table,
  5. validates the result and publishes nothing if a check fails,
  6. names the file geo-YYYY-MM-DD.sqlite, marks it read-only, and writes manifest.json.

Uploading to object storage is the deploy step (task T19), not this script.
Standard library only (Python 3.7+).
"""
import argparse
import datetime
import hashlib
import io
import json
import os
import re
import sqlite3
import sys
import tempfile
import unicodedata
import urllib.request
import zipfile

SCHEMA_VERSION = 1
DEFAULT_SOURCE_URL = (
    "https://www2.census.gov/geo/docs/maps-data/data/gazetteer/2024_Gazetteer/2024_Gaz_cousubs_national.zip")
DEFAULT_SEED_SQL = os.path.join(
    os.path.dirname(__file__), "..", "..", "src", "main", "resources", "db", "migration", "V3__p2_1_town_entry.sql")
STATE = "MA"
STATE_FIPS = "25"
EXPECTED_COUNT = 351
# Coarse Massachusetts bounding box (sanity check only; the Census state code and ID are the real test).
MA_BBOX = (41.18, -73.51, 42.89, -69.85)  # min lat, min lng, max lat, max lng
CREDITS = [
    "Town data: U.S. Census Bureau",
    "ZIP data: GeoNames (geonames.org), CC BY 4.0",
]
LICENSE_TEXT = (
    "Town data: U.S. Census Bureau, 2024 Gazetteer Files (county subdivisions), a U.S. Government work. "
    "ZIP data used for map pins: GeoNames (https://www.geonames.org), Creative Commons Attribution 4.0.")
STATE_WORDS = {"ma", "mass", "massachusetts"}
UNDEFINED_PREFIX = "County subdivisions not defined"
SLUG_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*-ma$")


class BuildError(Exception):
    """A validation failure. Nothing is published."""


# ---------- normalization (the same rules the app uses, SDD section 3.4) ----------

def normalize(text):
    """Lowercase, remove accents, punctuation to spaces, collapse spaces, drop a trailing state word."""
    t = unicodedata.normalize("NFKD", text)
    t = "".join(ch for ch in t if not unicodedata.combining(ch)).lower()
    t = re.sub(r"[^a-z0-9]+", " ", t).strip()
    words = t.split()
    if len(words) > 1 and words[-1] in STATE_WORDS:
        words = words[:-1]
    return " ".join(words)


def matches(query, search_name):
    """Word-start rule: every typed word starts some word of the name, in any order."""
    q = normalize(query).split()
    if not q:
        return False
    name_words = search_name.split()
    return all(any(w.startswith(tw) for w in name_words) for tw in q)


def clean_name(census_name):
    """'Agawam Town city' -> 'Agawam'; 'Bourne town' -> 'Bourne'; 'Boston city' -> 'Boston'."""
    n = re.sub(r"\s+(city|town)$", "", census_name.strip())
    return re.sub(r"\s+Town$", "", n)


def slug_for(name):
    return re.sub(r"\s+", "-", normalize(name)) + "-ma"


# ---------- input ----------

def read_source(source):
    """Return (text, sha256_hex, source_label) for a URL or a local .zip/.txt path."""
    if re.match(r"^https?://", source):
        req = urllib.request.Request(source, headers={"User-Agent": "arangly-geo-build/1.0"})
        with urllib.request.urlopen(req, timeout=120) as resp:
            raw = resp.read()
    else:
        with open(source, "rb") as f:
            raw = f.read()
    digest = hashlib.sha256(raw).hexdigest()
    if raw[:2] == b"PK":
        with zipfile.ZipFile(io.BytesIO(raw)) as z:
            member = next(m for m in z.namelist() if m.endswith(".txt"))
            text = z.read(member).decode("utf-8", errors="replace")
    else:
        text = raw.decode("utf-8", errors="replace")
    return text, digest, source


def parse_gazetteer(text):
    """Return Massachusetts rows as dicts. Rows marked other states are dropped."""
    lines = text.splitlines()
    header = [h.strip() for h in lines[0].split("\t")]
    idx = {h: i for i, h in enumerate(header)}
    for col in ("USPS", "GEOID", "NAME", "INTPTLAT", "INTPTLONG"):
        if col not in idx:
            raise BuildError("gazetteer header is missing column " + col)
    rows = []
    for line in lines[1:]:
        if not line.strip():
            continue
        f = [c.strip() for c in line.split("\t")]
        if f[idx["USPS"]] != STATE:
            continue
        if f[idx["NAME"]].startswith(UNDEFINED_PREFIX):
            continue
        rows.append({
            "geoid": f[idx["GEOID"]],
            "census_name": f[idx["NAME"]],
            "lat": float(f[idx["INTPTLAT"]]),
            "lng": float(f[idx["INTPTLONG"]]),
        })
    return rows


def read_seeded_slugs(seed_sql_path):
    """Slugs and states of the seeded live towns, read from the V3 migration INSERT."""
    with open(seed_sql_path) as f:
        sql = f.read()
    return re.findall(r"\(\s*'([a-z0-9-]+)',\s*'[^']*',\s*'([A-Z]{2})'", sql)


def read_previous(previous_path):
    """Return {source_id: slug} from a previously published place file, or {} if none."""
    if not previous_path:
        return {}
    con = sqlite3.connect("file:" + previous_path + "?mode=ro", uri=True)
    try:
        return dict(con.execute("SELECT source_id, slug FROM place").fetchall())
    finally:
        con.close()


# ---------- build ----------

def build_places(rows, previous):
    places = []
    for r in rows:
        name = clean_name(r["census_name"])
        slug = previous.get(r["geoid"]) or slug_for(name)   # pin: a published slug never changes
        places.append({
            "source_id": r["geoid"], "name": name, "search_name": normalize(name),
            "type": "city" if r["census_name"].endswith(" city") else "town",
            "state": STATE, "slug": slug, "lat": r["lat"], "lng": r["lng"],
        })
    return places


def validate(places, previous, seeded, expected_count):
    errors = []
    if len(places) != expected_count:
        errors.append("expected %d municipalities, found %d" % (expected_count, len(places)))
    for p in places:
        if not p["source_id"].startswith(STATE_FIPS):
            errors.append("row %s (%s) is marked MA but its Census ID is not a Massachusetts ID"
                          % (p["source_id"], p["name"]))
        if not p["name"].strip() or not p["search_name"]:
            errors.append("empty name for Census ID %s" % p["source_id"])
        if re.search(r"\s(town|city|Town)$", p["name"]):
            errors.append("administrative suffix left in name: %r" % p["name"])
        lat, lng = p["lat"], p["lng"]
        if not (MA_BBOX[0] <= lat <= MA_BBOX[2] and MA_BBOX[1] <= lng <= MA_BBOX[3]):
            errors.append("point outside Massachusetts bounds: %s (%.4f, %.4f)" % (p["name"], lat, lng))
        if not SLUG_RE.match(p["slug"]):
            errors.append("bad slug %r for %s" % (p["slug"], p["name"]))
    slugs = [p["slug"] for p in places]
    dups = sorted({s for s in slugs if slugs.count(s) > 1})
    if dups:
        errors.append("duplicate slugs: %s" % ", ".join(dups))
    missing_prev = sorted(set(previous.values()) - set(slugs))
    if missing_prev:
        errors.append("slugs from the previous file are missing: %s" % ", ".join(missing_prev))
    by_slug = {p["slug"]: p for p in places}
    for slug, state in seeded:
        if slug not in by_slug:
            errors.append("seeded town %s does not resolve" % slug)
        elif by_slug[slug]["state"] != state:
            errors.append("seeded town %s has state %s, expected %s" % (slug, by_slug[slug]["state"], state))
    # Every place returns itself by its exact name, under the app's matching rules.
    for p in places:
        exact = [o for o in places if o["search_name"] == normalize(p["name"])]
        if p not in exact or not matches(p["name"], p["search_name"]):
            errors.append("place does not find itself by exact name: %s" % p["name"])
    if not CREDITS:
        errors.append("credits list is empty")
    if errors:
        raise BuildError("\n  - " + "\n  - ".join(errors))


def write_sqlite(path, places, meta):
    con = sqlite3.connect(path)
    try:
        con.execute("PRAGMA journal_mode=DELETE")
        con.execute("""CREATE TABLE place (
            id INTEGER PRIMARY KEY, source_id TEXT NOT NULL UNIQUE, name TEXT NOT NULL, search_name TEXT NOT NULL,
            type TEXT NOT NULL, state TEXT NOT NULL, slug TEXT NOT NULL UNIQUE, latitude REAL NOT NULL,
            longitude REAL NOT NULL)""")
        con.execute("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        con.executemany(
            "INSERT INTO place (source_id, name, search_name, type, state, slug, latitude, longitude) "
            "VALUES (?,?,?,?,?,?,?,?)",
            [(p["source_id"], p["name"], p["search_name"], p["type"], p["state"], p["slug"], p["lat"], p["lng"])
             for p in sorted(places, key=lambda x: x["name"])])
        con.executemany("INSERT INTO meta (key, value) VALUES (?, ?)", sorted(meta.items()))
        con.commit()
        con.execute("VACUUM")
    finally:
        con.close()


def check_written_file(path):
    """Open read-only as the app will, and confirm the schema version and journal mode."""
    con = sqlite3.connect("file:" + path + "?mode=ro", uri=True)
    try:
        version = con.execute("SELECT value FROM meta WHERE key='schema_version'").fetchone()
        mode = con.execute("PRAGMA journal_mode").fetchone()[0]
        count = con.execute("SELECT COUNT(*) FROM place").fetchone()[0]
    finally:
        con.close()
    if not version or version[0] != str(SCHEMA_VERSION):
        raise BuildError("written file has wrong schema_version")
    if mode.lower() != "delete":
        raise BuildError("written file journal_mode is %s, expected delete" % mode)
    return count


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def run(source, out_dir, previous_path=None, seed_sql=DEFAULT_SEED_SQL, expected_count=EXPECTED_COUNT,
        today=None):
    today = today or datetime.date.today().isoformat()
    text, source_sha, source_label = read_source(source)
    rows = parse_gazetteer(text)
    previous = read_previous(previous_path)
    places = build_places(rows, previous)
    seeded = read_seeded_slugs(seed_sql) if seed_sql else []
    validate(places, previous, seeded, expected_count)

    os.makedirs(out_dir, exist_ok=True)
    final_name = "geo-%s.sqlite" % today
    final_path = os.path.join(out_dir, final_name)
    fd, tmp_path = tempfile.mkstemp(prefix=".geo-", suffix=".sqlite", dir=out_dir)
    os.close(fd)
    os.remove(tmp_path)
    meta = {
        "schema_version": str(SCHEMA_VERSION), "data_date": today, "build_date": today,
        "source_url": source_label, "source_vintage": "2024", "source_checksum": source_sha,
        "sources": json.dumps(["US Census 2024 Gazetteer: county subdivisions"]),
        "credits": json.dumps(CREDITS), "license_text": LICENSE_TEXT,
    }
    try:
        write_sqlite(tmp_path, places, meta)
        count = check_written_file(tmp_path)
        if os.path.exists(final_path):
            os.chmod(final_path, 0o644)
        os.replace(tmp_path, final_path)
    except Exception:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
        raise
    os.chmod(final_path, 0o444)
    file_sha = sha256_file(final_path)
    manifest = {"file": final_name, "sha256": file_sha, "data_date": today, "places": count,
                "schema_version": SCHEMA_VERSION}
    with open(os.path.join(out_dir, "manifest.json"), "w") as f:
        json.dump(manifest, f, indent=2)
        f.write("\n")
    return manifest


def main(argv=None):
    ap = argparse.ArgumentParser(description="Build the Arangly read-only place file (offline).")
    ap.add_argument("--source", default=DEFAULT_SOURCE_URL, help="Census gazetteer URL or local .zip/.txt path")
    ap.add_argument("--out-dir", default=os.path.join(os.path.dirname(__file__), "..", "..", "data", "geo"))
    ap.add_argument("--previous", help="previous published geo-*.sqlite, to pin slugs")
    ap.add_argument("--seed-sql", default=DEFAULT_SEED_SQL, help="V3 migration file with the seeded live towns")
    ap.add_argument("--expected-count", type=int, default=EXPECTED_COUNT)
    args = ap.parse_args(argv)
    try:
        manifest = run(args.source, os.path.abspath(args.out_dir), args.previous, args.seed_sql,
                       args.expected_count)
    except BuildError as e:
        print("BUILD FAILED, nothing published:" + str(e), file=sys.stderr)
        return 1
    print("OK: %s (%d places, sha256 %s)" % (manifest["file"], manifest["places"], manifest["sha256"]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
