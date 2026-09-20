#!/usr/bin/env bash
#
# Load test: compare Java ray casting vs PostGIS locate performance.
# Requires the app and PostGIS to be running: docker-compose up
#
#   ./scripts/load-test-strategy.sh
#

set -u

HOST="${HOST:-http://localhost:8080}"
ITERATIONS="${ITERATIONS:-50}"
JSON='Content-Type: application/json'
TMPDIR_LOAD=$(mktemp -d)
trap 'rm -rf "$TMPDIR_LOAD"' EXIT

echo
echo "STRATEGY LOAD TEST ($ITERATIONS iterations per strategy)"
echo "========================================================"

echo
echo "Uploading test building data..."
UPLOAD_STATUS=$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "$HOST/api/buildings/upload" \
    -F "file=@inputs/test-buildings.json")
echo "  upload status: $UPLOAD_STATUS"

if [ "$UPLOAD_STATUS" != "200" ] && [ "$UPLOAD_STATUS" != "201" ]; then
    echo "  upload failed, aborting"
    exit 1
fi

run_strategy() {
    local strategy="$1"

    echo
    echo "Switching to $strategy..."
    curl -s -o /dev/null -X PUT "$HOST/api/strategy" -H "$JSON" \
        -d "{\"strategy\":\"$strategy\"}"

    echo "Running $ITERATIONS locate requests..."
    for i in $(seq 1 "$ITERATIONS"); do
        curl -s -o "$TMPDIR_LOAD/${strategy}_body_${i}.txt" \
            -w '%{time_total}' \
            -X POST "$HOST/api/locate" -H "$JSON" \
            -d '{"x":10,"y":10,"z":1}' \
            > "$TMPDIR_LOAD/${strategy}_time_${i}.txt"
    done

    local total=0
    local min=999999
    local max=0
    for i in $(seq 1 "$ITERATIONS"); do
        ms=$(awk "BEGIN {printf \"%d\", $(cat "$TMPDIR_LOAD/${strategy}_time_${i}.txt") * 1000}")
        total=$((total + ms))
        [ "$ms" -lt "$min" ] && min=$ms
        [ "$ms" -gt "$max" ] && max=$ms
    done
    local avg=$((total / ITERATIONS))

    printf '  %s: avg=%dms  min=%dms  max=%dms  total=%dms\n' \
        "$strategy" "$avg" "$min" "$max" "$total"

    cp "$TMPDIR_LOAD/${strategy}_body_1.txt" "$TMPDIR_LOAD/${strategy}_sample.txt"
}

run_strategy "JAVA"
run_strategy "POSTGIS"

echo
echo "RESULT COMPARISON"
JAVA_BODY=$(cat "$TMPDIR_LOAD/JAVA_sample.txt")
POSTGIS_BODY=$(cat "$TMPDIR_LOAD/POSTGIS_sample.txt")

if [ "$JAVA_BODY" = "$POSTGIS_BODY" ]; then
    printf '  \033[32mPASS\033[0m  both strategies returned identical results\n'
else
    printf '  \033[31mFAIL\033[0m  strategies returned different results\n'
    printf '  JAVA:    %s\n' "$JAVA_BODY"
    printf '  POSTGIS: %s\n' "$POSTGIS_BODY"
fi

echo
echo "Resetting strategy to JAVA..."
curl -s -o /dev/null -X PUT "$HOST/api/strategy" -H "$JSON" \
    -d '{"strategy":"JAVA"}'
echo "Done."
echo