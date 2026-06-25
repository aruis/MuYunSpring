#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<USAGE
Usage: $0 [all|backend|frontend]

Runs the same checks as the GitHub CI jobs for the selected area.

Commands:
  all       Run backend and frontend checks. This is the default.
  backend   Run backend Gradle tests, including boot integration tests.
  frontend  Run frontend lint, format check, tests, and build.
USAGE
}

run_backend() {
  cd "$ROOT_DIR"
  ./gradlew test :muyun-boot:integrationTest --no-daemon --stacktrace
}

run_frontend() {
  cd "$ROOT_DIR"
  npm run check --prefix muyun-web
}

case "${1:-all}" in
  all)
    run_backend
    run_frontend
    ;;
  backend)
    run_backend
    ;;
  frontend)
    run_frontend
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
