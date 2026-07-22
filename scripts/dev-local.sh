#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/muyun-web"
DEMO_BOOTSTRAP_ENABLED="${MUYUN_DEMO_BOOTSTRAP_ENABLED:-true}"
BACKEND_PORT="${MUYUN_BACKEND_PORT:-8080}"
FRONTEND_PORT="${MUYUN_FRONTEND_PORT:-5173}"
FORCE_RESTART=false

PROCESS_PIDS=()
PROCESS_STATUS_DIR=""

usage() {
  cat <<USAGE
Usage: $0 [-f|--force]

Starts the local development stack:
  - PostgreSQL via docker compose
  - Spring Boot backend with continuous recompilation on http://127.0.0.1:${BACKEND_PORT}
  - Vite frontend on http://127.0.0.1:${FRONTEND_PORT}/

Options:
  -f, --force  Stop existing processes listening on backend/frontend ports before startup.

Environment:
  MUYUN_DEMO_BOOTSTRAP_ENABLED=false  Disable demo tenant bootstrap.
  MUYUN_BACKEND_PORT=8080             Backend port to clean and display.
  MUYUN_FRONTEND_PORT=5173            Frontend port to clean and display.
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

listen_pids_on_port() {
  local port="$1"
  if ! command -v lsof >/dev/null 2>&1; then
    return 0
  fi
  lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | sort -u
}

wait_until_stopped() {
  local pid="$1"
  local attempts=20
  while ((attempts > 0)); do
    if ! kill -0 "$pid" 2>/dev/null; then
      return 0
    fi
    sleep 0.2
    attempts=$((attempts - 1))
  done
  return 1
}

stop_pid() {
  local pid="$1"
  if ! kill -0 "$pid" 2>/dev/null; then
    return
  fi
  kill "$pid" 2>/dev/null || true
  if wait_until_stopped "$pid"; then
    return
  fi
  echo "Process $pid did not stop after SIGTERM; sending SIGKILL."
  kill -9 "$pid" 2>/dev/null || true
  wait_until_stopped "$pid" || true
}

force_stop_port() {
  local port="$1"
  local label="$2"
  local pids=()
  local pid
  while IFS= read -r pid; do
    if [[ -n "$pid" ]]; then
      pids+=("$pid")
    fi
  done < <(listen_pids_on_port "$port")
  if ((${#pids[@]} == 0)); then
    return
  fi
  echo "Stopping existing $label process(es) on port $port: ${pids[*]}"
  for pid in "${pids[@]}"; do
    stop_pid "$pid"
  done
}

force_stop_existing_processes() {
  if [[ "$FORCE_RESTART" != "true" ]]; then
    return
  fi
  if ! command -v lsof >/dev/null 2>&1; then
    echo "Cannot force stop existing processes because lsof is not available." >&2
    exit 1
  fi
  force_stop_port "$BACKEND_PORT" "backend"
  force_stop_port "$FRONTEND_PORT" "frontend"
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
  args+=" --server.port=$BACKEND_PORT"
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

watch_backend_classes() {
  cd "$ROOT_DIR"
  ./gradlew :muyun-boot:classes --continuous
}

start_frontend() {
  cd "$ROOT_DIR"
  npm run dev:backend --prefix muyun-web -- --port "$FRONTEND_PORT"
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

while (($# > 0)); do
  case "$1" in
    -f|--force)
      FORCE_RESTART=true
      ;;
    -h|--help|help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      exit 1
      ;;
  esac
  shift
done

trap cleanup INT TERM EXIT

cd "$ROOT_DIR"
PROCESS_STATUS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/muyun-dev.XXXXXX")"
force_stop_existing_processes
echo "Starting PostgreSQL..."
docker compose up -d
ensure_frontend_dependencies

echo "Starting backend, continuous compilation and frontend..."
start_process backend-compiler watch_backend_classes
start_process backend start_backend
start_process frontend start_frontend

echo
echo "Backend:  http://127.0.0.1:${BACKEND_PORT}"
echo "Frontend: http://127.0.0.1:${FRONTEND_PORT}/"
echo "Press Ctrl-C to stop backend and frontend."

exit "$(wait_for_children)"
