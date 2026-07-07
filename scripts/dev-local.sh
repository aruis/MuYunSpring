#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/muyun-web"
DEMO_BOOTSTRAP_ENABLED="${MUYUN_DEMO_BOOTSTRAP_ENABLED:-true}"

PROCESS_PIDS=()
PROCESS_STATUS_DIR=""

usage() {
  cat <<USAGE
Usage: $0

Starts the local development stack:
  - PostgreSQL via docker compose
  - Spring Boot backend on http://127.0.0.1:8080
  - Vite frontend on http://127.0.0.1:5173/

Environment:
  MUYUN_DEMO_BOOTSTRAP_ENABLED=false  Disable demo tenant bootstrap.
USAGE
}

cleanup() {
  if ((${#PROCESS_PIDS[@]} > 0)); then
    echo
    echo "Stopping local development processes..."
    for pid in "${PROCESS_PIDS[@]}"; do
      if kill -0 "$pid" 2>/dev/null; then
        kill "$pid" 2>/dev/null || true
      fi
    done
  fi
  if [[ -n "$PROCESS_STATUS_DIR" && -d "$PROCESS_STATUS_DIR" ]]; then
    rm -rf "$PROCESS_STATUS_DIR"
  fi
}

start_process() {
  local name="$1"
  shift
  (
    set +e
    "$@"
    local status=$?
    set -e
    printf '%s\n' "$status" >"$PROCESS_STATUS_DIR/$name.status"
    exit "$status"
  ) &
  PROCESS_PIDS+=("$!")
}

first_status_file() {
  local file
  for file in "$PROCESS_STATUS_DIR"/*.status; do
    if [[ -f "$file" ]]; then
      printf '%s\n' "$file"
      return 0
    fi
  done
  return 1
}

backend_args() {
  local args
  args="--muyun.runtime.mode=development"
  args+=" --spring.datasource.url=jdbc:postgresql://127.0.0.1:54321/muyun_spring"
  args+=" --spring.datasource.username=postgres"
  args+=" --spring.datasource.password=muyun_dev"
  if [[ "$DEMO_BOOTSTRAP_ENABLED" != "false" ]]; then
    args+=" --muyun.demo-bootstrap.enabled=true"
  fi
  printf '%s' "$args"
}

ensure_frontend_dependencies() {
  if [[ -d "$FRONTEND_DIR/node_modules" ]]; then
    return
  fi
  echo "Installing frontend dependencies..."
  npm ci --prefix "$FRONTEND_DIR"
}

start_backend() {
  cd "$ROOT_DIR"
  ./gradlew :muyun-boot:bootRun --args="$(backend_args)"
}

start_frontend() {
  cd "$ROOT_DIR"
  npm run dev:backend --prefix muyun-web
}

wait_for_children() {
  local status_file
  while true; do
    status_file="$(first_status_file || true)"
    if [[ -n "$status_file" ]]; then
      cat "$status_file"
      return
    fi
    sleep 2
  done
}

case "${1:-}" in
  -h|--help|help)
    usage
    exit 0
    ;;
  "")
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac

trap cleanup INT TERM EXIT

cd "$ROOT_DIR"
PROCESS_STATUS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/muyun-dev.XXXXXX")"
echo "Starting PostgreSQL..."
docker compose up -d
ensure_frontend_dependencies

echo "Starting backend and frontend..."
start_process backend start_backend
start_process frontend start_frontend

echo
echo "Backend:  http://127.0.0.1:8080"
echo "Frontend: http://127.0.0.1:5173/"
echo "Press Ctrl-C to stop backend and frontend."

exit "$(wait_for_children)"
