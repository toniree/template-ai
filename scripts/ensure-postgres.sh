#!/usr/bin/env bash
# Makes sure a local PostgreSQL is up and has the two databases this branch needs, then execs
# whatever command was passed to it (spring-boot:run, test, ...). Safe to run before every
# command — but "safe" specifically means: never mutate a Postgres this script didn't start.
#
# If something is already listening on 5432, it might be someone else's — another project's
# Postgres.app, a different docker-compose stack, a colleague's laptop image. We check for the
# project's own role and databases read-only; we only ever CREATE anything on an instance this
# script just started itself. If one is already running and isn't already set up for this
# project, we stop and ask rather than guess.
#
# Password auth is not a usable ownership signal here: Homebrew's default pg_hba.conf trusts all
# local connections regardless of password, so "postgres/postgres works" proves nothing.
set -euo pipefail

HOST=localhost
PORT=5432
PG_BIN="/opt/homebrew/opt/postgresql@16/bin"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

is_up() {
  if [ -x "$PG_BIN/pg_isready" ]; then
    "$PG_BIN/pg_isready" -h "$HOST" -p "$PORT" -q
  else
    pg_isready -h "$HOST" -p "$PORT" -q
  fi
}

PSQL="$PG_BIN/psql"
[ -x "$PSQL" ] || PSQL=psql

# Read-only. Connects as the current OS user over the local trust-auth socket, which needs no
# password and works whether or not a "postgres" role exists yet.
query_as_self() {
  "$PSQL" -h "$HOST" -p "$PORT" -U "$(whoami)" -d postgres -tAc "$1" 2>/dev/null | tr -d '[:space:]'
}

is_fully_set_up() {
  [ "$(query_as_self "select 1 from pg_roles where rolname='postgres'")" = "1" ] || return 1
  [ "$(query_as_self "select 1 from pg_database where datname='sandbox'")" = "1" ] || return 1
  [ "$(query_as_self "select 1 from pg_database where datname='sandbox_test'")" = "1" ] || return 1
}

STARTED="none"   # none | docker | brew

if is_up; then
  if is_fully_set_up; then
    echo "[ensure-postgres] already up on $HOST:$PORT and already set up for this project"
  else
    cat <<EOF >&2
[ensure-postgres] something is already listening on $HOST:$PORT, but it doesn't have the
"postgres" role and sandbox/sandbox_test databases this project expects — it's probably not this
project's database. Refusing to create or modify anything on it automatically.

Options:
  - If it's yours and unrelated: stop it, or point this project at a different port.
  - If it's a half-finished setup from this project: connect and check what's there, then create
    the missing pieces by hand (see docs/CHEATSHEET.md).
  - To let this script manage its own instance instead, free port $PORT and rerun.
EOF
    exit 1
  fi
else
  if command -v docker >/dev/null 2>&1 && [ -f "$REPO_ROOT/docker-compose.yml" ]; then
    echo "[ensure-postgres] starting docker compose..."
    (cd "$REPO_ROOT" && docker compose up -d)
    STARTED="docker"
  elif brew list --formula 2>/dev/null | grep -q '^postgresql@16$'; then
    echo "[ensure-postgres] starting postgresql@16 via brew services..."
    brew services start postgresql@16 >/dev/null
    STARTED="brew"
  else
    echo "[ensure-postgres] no Docker and no postgresql@16 formula found. Install one:"
    echo "  brew install postgresql@16 && brew services start postgresql@16"
    echo "  # or: docker compose up -d   (repo root)"
    exit 1
  fi

  for i in $(seq 1 30); do is_up && break; sleep 1; done
  is_up || { echo "[ensure-postgres] still not accepting connections after 30s"; exit 1; }

  if [ "$STARTED" = "docker" ]; then
    # docker-compose.yml already creates the postgres role (POSTGRES_USER) and the sandbox
    # database (POSTGRES_DB), and docker/init-test-db.sql creates sandbox_test on first boot.
    # Nothing left to do here except wait for the healthcheck.
    for i in $(seq 1 30); do
      docker compose -f "$REPO_ROOT/docker-compose.yml" ps --format '{{.Health}}' 2>/dev/null | grep -qx healthy && break
      sleep 1
    done
  else
    # Fresh Homebrew instance we just started — nothing project-specific exists yet.
    query_as_self "select 1 from pg_roles where rolname='postgres'" | grep -q 1 || \
      "$PSQL" -h "$HOST" -p "$PORT" -U "$(whoami)" -d postgres -c \
        "create role postgres with login superuser password 'postgres'" >/dev/null

    for db in sandbox sandbox_test; do
      query_as_self "select 1 from pg_database where datname='$db'" | grep -q 1 || \
        "$PSQL" -h "$HOST" -p "$PORT" -U "$(whoami)" -d postgres -c "create database $db" >/dev/null
    done
  fi
fi

echo "[ensure-postgres] ready: sandbox, sandbox_test"

if [ "$#" -gt 0 ]; then
  exec "$@"
fi
