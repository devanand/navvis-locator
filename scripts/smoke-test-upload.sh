#!/usr/bin/env bash
#
# Smoke test: upload validation and file size limit (10MB).
# Requires the app to be running: docker-compose up
#
#   ./scripts/smoke-test-upload.sh
#

set -u

HOST="${HOST:-http://localhost:8080}"
PASS=0
FAIL=0
TMPDIR_UPLOAD=$(mktemp -d)
trap 'rm -rf "$TMPDIR_UPLOAD"' EXIT

check() {
    local expected="$1" description="$2"
    shift 2
    local body status
    body=$(curl -s -w '\n%{http_code}' "$@")
    status=$(printf '%s' "$body" | tail -n1)
    body=$(printf '%s' "$body" | sed '$d')

    if [ "$status" = "$expected" ]; then
        printf '  \033[32mPASS\033[0m  %-3s  %s\n' "$status" "$description"
        PASS=$((PASS + 1))
    else
        printf '  \033[31mFAIL\033[0m  %-3s  %s (expected %s)\n' "$status" "$description" "$expected"
        printf '        %s\n' "$body"
        FAIL=$((FAIL + 1))
    fi
}

echo
echo "UPLOAD SMOKE TEST"
echo "================="

echo
echo "Valid upload"
check 200 "valid building file accepted" \
    -X POST "$HOST/api/buildings/upload" \
    -F "file=@inputs/test-buildings.json"

echo
echo "Missing file"
check 415 "non-multipart request rejected" \
    -X POST "$HOST/api/buildings/upload"

check 400 "missing file part rejected" \
    -X POST "$HOST/api/buildings/upload" \
    -F "notafile=dummy"

echo
echo "File size limit (10MB)"
echo "  generating 11MB file..."
dd if=/dev/zero bs=1M count=11 2>/dev/null | tr '\0' 'x' > "$TMPDIR_UPLOAD/oversized.json"
check 413 "oversized file rejected" \
    -X POST "$HOST/api/buildings/upload" \
    -F "file=@$TMPDIR_UPLOAD/oversized.json"

echo
printf '\n%d passed, %d failed\n\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]