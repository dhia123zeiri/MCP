#!/usr/bin/env bash
# Register all Kafka Connect source/sink connectors.
# Run AFTER `docker compose up -d` and after Connect is healthy.
#
# Requires: curl, jq

set -e
CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"
CFG_DIR="$(cd "$(dirname "$0")" && pwd)/configs"

echo "Waiting for Kafka Connect at $CONNECT_URL ..."
until curl -fs "$CONNECT_URL/" >/dev/null 2>&1; do sleep 2; done
echo "Connect is up."

register() {
    local file=$1
    local name
    name=$(jq -r .name "$file")
    echo ">>> Registering $name"
    curl -fsS -X PUT \
        -H "Content-Type: application/json" \
        --data "$(jq .config "$file")" \
        "$CONNECT_URL/connectors/$name/config" >/dev/null
    echo "    OK"
}

# --- Sources ---
register "$CFG_DIR/source-pettypes.json"
register "$CFG_DIR/source-countries.json"

# --- Sinks (one per result topic) ---
for sink in \
    sink-revenue-per-item \
    sink-expenses-per-item \
    sink-profit-per-item \
    sink-total-revenue \
    sink-total-expenses \
    sink-total-profit \
    sink-avg-by-item \
    sink-avg-all \
    sink-top-profit \
    sink-windowed-revenue \
    sink-windowed-expenses \
    sink-windowed-profit \
    sink-top-country
do
    register "$CFG_DIR/${sink}.json"
done

echo ""
echo "All connectors registered. Status:"
curl -fsS "$CONNECT_URL/connectors?expand=status" | jq 'to_entries | map({name: .key, state: .value.status.connector.state, tasks: [.value.status.tasks[].state]})'
