#!/usr/bin/env bash
# The one command to start the app: makes sure Postgres is up (starting it if needed, via Docker
# or Homebrew — see scripts/ensure-postgres.sh) and creates sandbox/sandbox_test if they don't
# exist yet, then runs the app. Safe to run repeatedly.
set -euo pipefail
cd "$(dirname "$0")"
./scripts/ensure-postgres.sh
cd sandbox
./mvnw -o spring-boot:run
