#!/usr/bin/env bash
set -euo pipefail

BREW_PREFIX="$(brew --prefix)"
PG_BIN="$BREW_PREFIX/opt/postgresql@17/bin"

PG_HOST=localhost
PG_PORT=5432
PG_USER="$(whoami)"
PG_DB=rbac

confirm() {
    read -r -p "This will drop the rbac database. Are you sure? [y/N] " reply
    [[ "$reply" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }
}

drop_database() {
    if "$PG_BIN/psql" -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" \
            -lqt | cut -d'|' -f1 | grep -qw "$PG_DB"; then
        "$PG_BIN/dropdb" -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" "$PG_DB"
        echo "  postgres: dropped database '$PG_DB'"
    else
        echo "  postgres: database '$PG_DB' does not exist — nothing to drop"
    fi
}

confirm
echo "Cleaning up rbac data model..."
drop_database
echo "Done. Run migrate.sh to recreate the schema."
