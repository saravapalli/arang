#!/usr/bin/env bash
# One-way copy: prod `arang` (read-only snapshot) -> dev `arang_dev` (dropped and recreated).
# Never writes to prod. Stop the dev backend first, because this replaces arang_dev.
set -euo pipefail
U="${ARANG_DB_USER:-arang}"; P="${ARANG_DB_PASSWORD:-arang}"
SRC=arang; DST=arang_dev
M() { mysql -u"$U" -p"$P" "$@" 2> >(grep -v 'Using a password' >&2); }
echo "==> recreating $DST"
M -e "DROP DATABASE IF EXISTS $DST; CREATE DATABASE $DST CHARACTER SET utf8mb4"
echo "==> copying $SRC -> $DST (read-only on $SRC)"
mysqldump -u"$U" -p"$P" --single-transaction --routines "$SRC" 2> >(grep -v 'Using a password' >&2) | M "$DST"
echo "==> row counts"
M -N -e "SELECT 'event', COUNT(*) FROM $SRC.event UNION ALL SELECT 'event(dev)', COUNT(*) FROM $DST.event"
