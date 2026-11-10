#!/usr/bin/env bash
set -u

: "${SUPABASE_URL:?set SUPABASE_URL}"
: "${SUPABASE_ANON_KEY:?set SUPABASE_ANON_KEY}"

BASE="${SUPABASE_URL%/}"
failed=0

call() {
  method=$1
  path=$2
  payload=${3:-}
  if [ -n "$payload" ]; then
    curl -sS -X "$method" "$BASE$path" \
      -H "apikey: $SUPABASE_ANON_KEY" \
      -H "Authorization: Bearer $SUPABASE_ANON_KEY" \
      -H "Content-Type: application/json" \
      --data "$payload" \
      -w '\n%{http_code}'
  else
    curl -sS -X "$method" "$BASE$path" \
      -H "apikey: $SUPABASE_ANON_KEY" \
      -H "Authorization: Bearer $SUPABASE_ANON_KEY" \
      -w '\n%{http_code}'
  fi
}

resp=$(call POST "/rest/v1/orders" '{"customer_id":"11111111-1111-1111-1111-111111111111","total":1}')
status=$(printf '%s\n' "$resp" | tail -n1)
body=$(printf '%s\n' "$resp" | sed '$d')
printf 'check 1: POST /rest/v1/orders -> HTTP %s\n%s\n' "$status" "$body"
case "$status" in
  401|403) echo "PASS" ;;
  *) echo "FAIL"; failed=1 ;;
esac

resp=$(call GET "/rest/v1/access_codes?select=code")
status=$(printf '%s\n' "$resp" | tail -n1)
body=$(printf '%s\n' "$resp" | sed '$d')
printf 'check 2: GET /rest/v1/access_codes -> HTTP %s\n%s\n' "$status" "$body"
if [ "$status" = "401" ] || [ "$status" = "403" ]; then
  echo "PASS"
elif [ "$status" = "200" ] && [ "$(printf '%s' "$body" | tr -d '[:space:]')" = "[]" ]; then
  echo "PASS"
else
  echo "FAIL"
  failed=1
fi

exit "$failed"
