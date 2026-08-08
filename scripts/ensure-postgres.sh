#!/usr/bin/env bash
# Makes sure a local PostgreSQL is up and has the two databases this branch needs, then execs
# whatever command was passed to it (spring-boot:run, test, ...). Idempotent — safe to run before
# every command, not just the first.
#
# Prefers whichever of Docker or Homebrew Postgres is already set up on this machine; if neither
# is, it defaults to Homebrew (lighter than Docker Desktop for a laptop that's never run either).
set -euo pipefail

HOST=localhost
PORT=5432
PG_BIN="/opt/homebrew/opt/postgresql@16/bin"

is_up() {
  if [ -x "$PG_BIN/pg_isready" ]; then
    "$PG_BIN/pg_isready" -h "$HOST" -p "$PORT" -q
  else
    pg_isready -h "$HOST" -p "$PORT" -q
  fi
}

if is_up; then
  echo "[ensure-postgres] already up on $HOST:$PORT"
elif command -v docker >/dev/null 2>&1 && [ -f "$(dirname "$0")/../docker-compose.yml" ]; then
  echo "[ensure-postgres] starting docker compose..."
  (cd "$(dirname "$0")/.." && docker compose up -d)
  for i in $(seq 1 30); do is_up && break; sleep 1; done
elif brew list --formula 2>/dev/null | grep -q '^postgresql@16$'; then
  echo "[ensure-postgres] starting postgresql@16 via brew services..."
  brew services start postgresql@16 >/dev/null
  for i in $(seq 1 30); do is_up && break; sleep 1; done
else
  echo "[ensure-postgres] no Docker and no postgresql@16 formula found. Install one:"
  echo "  brew install postgresql@16 && brew services start postgresql@16"
  echo "  # or: docker compose up -d   (repo root)"
  exit 1
fi

is_up || { echo "[ensure-postgres] still not accepting connections after 30s"; exit 1; }

PSQL="$PG_BIN/psql"
[ -x "$PSQL" ] || PSQL=psql

# Match docker-compose's credentials exactly, so the committed application.yml needs no per-machine
# edits regardless of which backend is running.
"$PSQL" -h "$HOST" -U "$(whoami)" -d postgres -tAc \
  "select 1 from pg_roles where rolname='postgres'" | grep -q 1 || \
  "$PSQL" -h "$HOST" -U "$(whoami)" -d postgres -c \
  "create role postgres with login superuser password 'postgres'" >/dev/null

for db in sandbox sandbox_test; do
  "$PSQL" -h "$HOST" -U postgres -d postgres -tAc \
    "select 1 from pg_database where datname='$db'" | grep -q 1 || \
    "$PSQL" -h "$HOST" -U postgres -d postgres -c "create database $db" >/dev/null
done

echo "[ensure-postgres] ready: sandbox, sandbox_test"

if [ "$#" -gt 0 ]; then
  exec "$@"
fi
