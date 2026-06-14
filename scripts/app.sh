#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RBAC_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
INFRA_DIR="${INFRA_DIR:-$(cd "$SCRIPT_DIR/../../.." && pwd)/infra}"

LOGS_DIR="$INFRA_DIR/local/logs"
ARCHIVE_DIR="$LOGS_DIR/archive"
PIDS_DIR="$INFRA_DIR/local/pids"
CONFIG="$INFRA_DIR/local/configs/rbac-config.yaml"
JAR="$RBAC_DIR/rbac-app/target/rbac-app.jar"

rotate_log() {
    local log="$LOGS_DIR/rbac.log"
    if [[ -f "$log" ]]; then
        mkdir -p "$ARCHIVE_DIR"
        mv "$log" "$ARCHIVE_DIR/rbac-$(date +%Y%m%dT%H%M%S).log"
    fi
}

is_running() {
    local file="$PIDS_DIR/rbac.pid"
    [[ -f "$file" ]] && kill -0 "$(cat "$file")" 2>/dev/null
}

start() {
    if [[ ! -f "$JAR" ]]; then
        echo "jar not found at $JAR — run 'mvn package' first" >&2
        exit 1
    fi
    if is_running; then
        echo "rbac: already running"
        return
    fi
    mkdir -p "$LOGS_DIR" "$PIDS_DIR"
    rotate_log
    java -Drbac.config="$CONFIG" \
        -jar "$JAR" \
        > "$LOGS_DIR/rbac.log" 2>&1 &
    echo $! > "$PIDS_DIR/rbac.pid"
    echo "rbac: started (log: $LOGS_DIR/rbac.log)"
}

stop() {
    if ! is_running; then
        echo "rbac: not running"
        rm -f "$PIDS_DIR/rbac.pid"
        return
    fi
    local pid
    pid="$(cat "$PIDS_DIR/rbac.pid")"
    kill "$pid"
    echo "rbac: waiting for graceful shutdown (pid $pid)..."
    for i in $(seq 1 35); do
        if ! kill -0 "$pid" 2>/dev/null; then break; fi
        sleep 1
    done
    if kill -0 "$pid" 2>/dev/null; then
        echo "rbac: timed out — force killing"
        kill -9 "$pid"
    fi
    rm -f "$PIDS_DIR/rbac.pid"
    echo "rbac: stopped"
}

status() {
    if is_running; then
        echo "rbac: running (pid $(cat "$PIDS_DIR/rbac.pid"))"
    else
        echo "rbac: stopped"
    fi
}

case "${1:-}" in
    start)   start ;;
    stop)    stop ;;
    restart) stop; start ;;
    status)  status ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}" >&2
        exit 1
        ;;
esac
