#!/usr/bin/env bash
# Publish the current working tree to the stable app on :8080.
# Builds the SPA + a jar, copies the jar to .release/, then restarts :8080 from that jar.
# The stable app therefore never runs from build/ and is not disturbed by dev builds.
# Optional secrets: put `export ARANG_...=...` lines in .release/env (git-ignored).
set -euo pipefail
if [ "${1:-}" != "--prod" ]; then
  echo "Refusing: this restarts PROD on :8080. Re-run with --prod only when the owner asked for a prod release." >&2
  exit 64
fi
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-$HOME/Library/Java/JavaVirtualMachines/temurin-25/Contents/Home}"
export PATH="$JAVA_HOME/bin:${NODE_BIN:-/tmp/node20/bin}:$PATH"
PORT="${STABLE_PORT:-8080}"
REL="$ROOT/.release"
mkdir -p "$REL"

echo "==> 1/5 build SPA"
( cd web && node node_modules/vite/bin/vite.js build )

echo "==> 2/5 copy SPA into static/ (removing stale bundles)"
rm -rf src/main/resources/static/assets
cp -R web/dist/. src/main/resources/static/

echo "==> 3/5 run backend tests, then build jar"
./gradlew test bootJar
NEW_JAR="$(ls -t build/libs/*.jar | grep -v plain | head -1)"
[ -f "$REL/arang.jar" ] && cp "$REL/arang.jar" "$REL/arang.previous.jar"
cp "$NEW_JAR" "$REL/arang.jar"

echo "==> 4/5 stop whatever listens on :$PORT"
OLD="$(lsof -tiTCP:"$PORT" -sTCP:LISTEN || true)"
if [ -n "$OLD" ]; then kill $OLD; for _ in $(seq 1 20); do kill -0 $OLD 2>/dev/null || break; sleep 0.5; done; fi

echo "==> 5/5 start stable app from .release/arang.jar"
[ -f "$REL/env" ] && source "$REL/env"
export SERVER_PORT="$PORT"
nohup java -jar "$REL/arang.jar" >> "$REL/arang.log" 2>&1 &
for _ in $(seq 1 60); do
  curl -fs "http://localhost:$PORT/health" >/dev/null && { echo "OK: stable app is up on :$PORT (log: .release/arang.log)"; exit 0; }
  sleep 1
done
echo "FAILED to become healthy. Roll back: cp .release/arang.previous.jar .release/arang.jar and rerun the java -jar line. See .release/arang.log" >&2
exit 1
