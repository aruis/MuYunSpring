#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/muyun-web"
BACKEND_PORT="${MUYUN_BACKEND_PORT:-8080}"
FRONTEND_PORT="${MUYUN_FRONTEND_PORT:-5173}"
FORCE_RESTART=false

PROCESS_PIDS=()
PROCESS_NAMES=()
CLEANING_UP=false

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
  MUYUN_BACKEND_PORT=8080             Backend port to clean and display.
  MUYUN_FRONTEND_PORT=5173            Frontend port to clean and display.
USAGE
}

cleanup() {
  if [[ "$CLEANING_UP" == "true" ]]; then
    return
  fi
  CLEANING_UP=true
  if ((${#PROCESS_PIDS[@]} > 0)); then
    echo
    echo "Stopping local development processes..."
    for pid in "${PROCESS_PIDS[@]}"; do
      stop_process_tree "$pid"
    done
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

child_pids() {
  local pid="$1"
  if command -v pgrep >/dev/null 2>&1; then
    pgrep -P "$pid" 2>/dev/null || true
  fi
}

stop_process_tree() {
  local pid="$1"
  local child
  while IFS= read -r child; do
    if [[ -n "$child" ]]; then
      stop_process_tree "$child"
    fi
  done < <(child_pids "$pid")
  stop_pid "$pid"
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

force_stop_project_processes() {
  local label="$1"
  shift
  local needles=("$@")
  local pids=()
  local pid
  local command
  while IFS=$'\t' read -r pid command; do
    if [[ -n "$pid" ]]; then
      local matches=true
      local needle
      for needle in "${needles[@]}"; do
        if [[ "$command" != *"$needle"* ]]; then
          matches=false
          break
        fi
      done
      if [[ "$matches" == "true" ]]; then
        pids+=("$pid")
      fi
    fi
  done < <(ps -axo pid=,command= | awk -v root="$ROOT_DIR" '
    index($0, root) > 0 { sub(/^[[:space:]]+/, ""); pid = $1; sub(/^[^[:space:]]+[[:space:]]+/, ""); print pid "\t" $0 }
  ')
  if ((${#pids[@]} == 0)); then
    return
  fi
  echo "Stopping existing $label process(es): ${pids[*]}"
  for pid in "${pids[@]}"; do
    stop_process_tree "$pid"
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
  force_stop_project_processes "continuous compiler" "demoClasses" "--continuous"
  force_stop_project_processes "boot runner" ":muyun-boot:demoBootRun"
  force_stop_project_processes "frontend dev server" "node_modules/.bin/vite" "--port $FRONTEND_PORT"
}

start_process() {
  local name="$1"
  shift
  "$@" &
  PROCESS_PIDS+=("$!")
  PROCESS_NAMES+=("$name")
}

backend_args() {
  local args
  args="--muyun.runtime.mode=development"
  args+=" --spring.profiles.active=local"
  args+=" --server.port=$BACKEND_PORT"
  args+=" --spring.datasource.url=jdbc:postgresql://127.0.0.1:54321/muyun_spring"
  args+=" --spring.datasource.username=postgres"
  args+=" --spring.datasource.password=muyun_dev"
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
  exec ./gradlew :muyun-boot:demoBootRun --args="$(backend_args)"
}

watch_backend_classes() {
  cd "$ROOT_DIR"
  exec ./gradlew demoClasses --continuous
}

start_frontend() {
  cd "$ROOT_DIR"
  exec npm run dev:backend --prefix muyun-web -- --port "$FRONTEND_PORT"
}

wait_for_children() {
  local index
  local pid
  local status
  while true; do
    for index in "${!PROCESS_PIDS[@]}"; do
      pid="${PROCESS_PIDS[$index]}"
      if ! kill -0 "$pid" 2>/dev/null; then
        wait "$pid" || status=$?
        status="${status:-0}"
        echo "${PROCESS_NAMES[$index]} exited with status $status" >&2
        return "$status"
      fi
    done
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

wait_for_children
exit $?
