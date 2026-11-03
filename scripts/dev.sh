#!/usr/bin/env bash
# Local development, isolated from the stable app on :8080.
#   backend  -> http://localhost:8081  (own database: arang_dev)
#   frontend -> http://localhost:5173  (Vite hot reload, proxies /api to :8081)
# Stop both with Ctrl-C.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-$HOME/Library/Java/JavaVirtualMachines/temurin-25/Contents/Home}"
export PATH="$JAVA_HOME/bin:${NODE_BIN:-/tmp/node20/bin}:$PATH"

DEV_PORT="${DEV_PORT:-8081}"
export SERVER_PORT="$DEV_PORT"
export ARANG_DB_URL="${ARANG_DEV_DB_URL:-jdbc:mysql://localhost:3306/arang_dev?createDatabaseIfNotExist=true}"
export ARANG_PUBLIC_BASE_URL="http://localhost:$DEV_PORT"
# WhatsApp/LLM secrets are deliberately NOT set here, so dev never answers the real webhook.
# Lasting tile cache, so a dev restart keeps the OSM tiles already fetched (git-ignored).
export ARANG_TILES_CACHE_DIR="${ARANG_TILES_CACHE_DIR:-$ROOT/data/tiles}"

# Separate build dir so dev compiles never touch files a running app may use.
./gradlew bootRun -PdevBuildDir=build-dev &
BACK=$!
trap 'kill $BACK 2>/dev/null || true' EXIT INT TERM

cd web
ARANG_API_TARGET="http://localhost:$DEV_PORT" node node_modules/vite/bin/vite.js --port 5173
