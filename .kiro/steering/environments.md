# Environments: dev vs prod (always follow)

| Env | Port | Database | How it runs |
|---|---|---|---|
| **dev** | 8081 (backend), 5173 (Vite) | `arang_dev` | `scripts/dev.sh` |
| **prod** | 8080 | `arang` | `.release/arang.jar`, published by `scripts/release.sh --prod` |

## Rules
- Default target is **dev**. All code, build, test, and run work uses dev (:8081 / :5173 / `arang_dev`).
- Never stop, restart, or send test data to :8080 or the `arang` database unless the user explicitly asks for prod in that message.
- Run `scripts/release.sh --prod` ONLY when the user's current message explicitly says to push or release to prod. A past approval does not carry over.
- Before a prod release: state what will ship (git status/diff summary) and that it restarts :8080.
- Never run `./gradlew bootRun` without `-PdevBuildDir=build-dev`. Never write to `build/` while prod may use it.
- E2E tests run against dev: set `ARANG_BASE_URL=http://localhost:8081` and `ARANG_DB_NAME=arang_dev`.
- If the user says "Dell", "deb", or similar, treat it as "dev" and confirm if unsure.
- After a prod release, report the `/health` result and how to roll back.
