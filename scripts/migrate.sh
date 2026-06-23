#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AUTHZ_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

BREW_PREFIX="$(brew --prefix)"
PG_BIN="$BREW_PREFIX/opt/postgresql@17/bin"

PG_HOST=localhost
PG_PORT=5432
PG_USER="$(whoami)"
PG_DB=rbac

VAULT_ADDR="http://127.0.0.1:8200"
VAULT_TOKEN="dev-root-token"

RBAC_CHANGELOG_DIR="$AUTHZ_DIR/rbac/rbac-postgres/src/main/resources"
MON_CHANGELOG_DIR="$AUTHZ_DIR/monetization/monetization-postgres/src/main/resources"
CHANGELOG="db/changelog/postgres/db.changelog-master.xml"

# ---- helpers ----------------------------------------------------------------

wait_for_postgres() {
    echo "Waiting for postgres..."
    local retries=20
    until "$PG_BIN/pg_isready" -h "$PG_HOST" -p "$PG_PORT" -q; do
        retries=$((retries - 1))
        if [[ $retries -eq 0 ]]; then
            echo "Postgres did not become ready in time." >&2
            exit 1
        fi
        sleep 1
    done
    echo "  postgres: ready"
}

ensure_database() {
    if "$PG_BIN/psql" -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" \
            -lqt | cut -d'|' -f1 | grep -qw "$PG_DB"; then
        echo "  database '$PG_DB': already exists"
    else
        "$PG_BIN/createdb" -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" "$PG_DB"
        echo "  database '$PG_DB': created"
    fi
}

run_liquibase() {
    local label="$1"
    local changelog_dir="$2"
    echo "Running Liquibase migrations ($label)..."
    liquibase \
        --url="jdbc:postgresql://$PG_HOST:$PG_PORT/$PG_DB" \
        --username="$PG_USER" \
        --search-path="$changelog_dir" \
        --changelog-file="$CHANGELOG" \
        update
    echo "  $label migrations: done"
}

seed_vault() {
    echo "Seeding vault dev secrets..."
    export VAULT_ADDR VAULT_TOKEN

    vault kv put secret/local/rbac/database \
        url="jdbc:postgresql://$PG_HOST:$PG_PORT/$PG_DB" \
        username="$PG_USER" \
        password=""

    vault kv put secret/local/rbac/redis \
        host="localhost" \
        port="6379"

    echo "  vault: secrets written"
}

# ---- main -------------------------------------------------------------------

wait_for_postgres
ensure_database
run_liquibase "rbac" "$RBAC_CHANGELOG_DIR"
run_liquibase "monetization" "$MON_CHANGELOG_DIR"
seed_vault
