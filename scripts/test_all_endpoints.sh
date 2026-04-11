#!/bin/bash
# ============================================================
# RecruitAssist — Full Endpoint Integration Test
# Requires: running server at http://127.0.0.1:8081
# Usage:   bash scripts/test_all_endpoints.sh
# ============================================================

set -euo pipefail

BASE="http://127.0.0.1:8081"
PASS=0
FAIL=0
COOKIE_JAR=$(mktemp /tmp/ra_cookies_XXXX)
trap "rm -f $COOKIE_JAR" EXIT

green() { printf "\033[32m✅ PASS\033[0m %s\n" "$1"; PASS=$((PASS+1)); }
red()   { printf "\033[31m❌ FAIL\033[0m %s\n" "$1"; FAIL=$((FAIL+1)); }

check() {
    local desc="$1" url="$2" expected="$3"
    local body
    body=$(curl -s -L -b "$COOKIE_JAR" -c "$COOKIE_JAR" "$url" 2>/dev/null || true)
    if echo "$body" | grep -qi "$expected"; then
        green "$desc"
    else
        red "$desc (expected '$expected' in response)"
    fi
}

check_status() {
    local desc="$1" url="$2" expected_code="$3"
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" -L -b "$COOKIE_JAR" -c "$COOKIE_JAR" "$url" 2>/dev/null || echo "000")
    if [ "$code" = "$expected_code" ]; then
        green "$desc (HTTP $code)"
    else
        red "$desc (expected HTTP $expected_code, got $code)"
    fi
}

post_check() {
    local desc="$1" url="$2" data="$3" expected="$4"
    local body
    body=$(curl -s -L -b "$COOKIE_JAR" -c "$COOKIE_JAR" -d "$data" "$url" 2>/dev/null || true)
    if echo "$body" | grep -qi "$expected"; then
        green "$desc"
    else
        red "$desc (expected '$expected' in response)"
    fi
}

echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║     RecruitAssist — Full Integration Test Suite         ║"
echo "║     Server: $BASE                          ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# --------------------------------------------------
echo "━━━ 1. Public Pages (No Auth) ━━━"
# --------------------------------------------------
check_status "Home page loads" "$BASE/home" "200"
check "Home page shows system stats" "$BASE/home" "RecruitAssist"
check_status "Login page loads" "$BASE/login" "200"
check "Login page has form" "$BASE/login" "username"

# --------------------------------------------------
echo ""
echo "━━━ 2. Authentication ━━━"
# --------------------------------------------------
# Clear cookies first
rm -f "$COOKIE_JAR"; touch "$COOKIE_JAR"

post_check "Login with wrong password fails" "$BASE/login" "username=alice.ta&password=wrong" "Invalid"
post_check "Login with non-existent user fails" "$BASE/login" "username=nobody&password=demo123" "Invalid"

# Login as TA
rm -f "$COOKIE_JAR"; touch "$COOKIE_JAR"
post_check "TA login succeeds" "$BASE/login" "username=alice.ta&password=demo123" "dashboard"
check "TA dashboard loads after login" "$BASE/dashboard" "Recommended"

# --------------------------------------------------
echo ""
echo "━━━ 3. TA Features ━━━"
# --------------------------------------------------
check "TA dashboard has KPI cards" "$BASE/dashboard" "workload"
check "TA dashboard has profile section" "$BASE/dashboard" "profile"
check "TA dashboard has recommended jobs" "$BASE/dashboard" "score"

# Test job detail page
check "Job detail page loads" "$BASE/jobs/detail?id=J1" "detail"

# Logout
check_status "Logout works" "$BASE/logout" "200"

# --------------------------------------------------
echo ""
echo "━━━ 4. MO Features ━━━"
# --------------------------------------------------
rm -f "$COOKIE_JAR"; touch "$COOKIE_JAR"
post_check "MO login succeeds" "$BASE/login" "username=recruiter.01&password=demo123" "dashboard"
check "MO dashboard loads" "$BASE/dashboard" "Create"
check "MO dashboard shows owned jobs" "$BASE/dashboard" "Job"
check "MO can view job detail" "$BASE/jobs/detail?id=J1" "detail"

# Logout
check_status "MO logout works" "$BASE/logout" "200"

# --------------------------------------------------
echo ""
echo "━━━ 5. Admin Features ━━━"
# --------------------------------------------------
rm -f "$COOKIE_JAR"; touch "$COOKIE_JAR"
post_check "Admin login succeeds" "$BASE/login" "username=admin.sarah&password=demo123" "dashboard"
check "Admin dashboard loads" "$BASE/dashboard" "Workload"
check "Admin dashboard has recruitment overview" "$BASE/dashboard" "Recruitment"
check "Admin dashboard has TA workload table" "$BASE/dashboard" "threshold"

# Logout
check_status "Admin logout works" "$BASE/logout" "200"

# --------------------------------------------------
echo ""
echo "━━━ 6. Access Control ━━━"
# --------------------------------------------------
rm -f "$COOKIE_JAR"; touch "$COOKIE_JAR"

# Unauthenticated access should redirect to login
check "Unauthenticated /dashboard redirects to login" "$BASE/dashboard" "login"
check "Unauthenticated /jobs/detail redirects to login" "$BASE/jobs/detail?id=J1" "login"

# --------------------------------------------------
echo ""
echo "━━━ 7. Stability — Rapid Sequential Requests ━━━"
# --------------------------------------------------
rm -f "$COOKIE_JAR"; touch "$COOKIE_JAR"
post_check "Login for stability test" "$BASE/login" "username=alice.ta&password=demo123" "dashboard"

STABLE=true
for i in $(seq 1 20); do
    code=$(curl -s -o /dev/null -w "%{http_code}" -b "$COOKIE_JAR" "$BASE/dashboard" 2>/dev/null || echo "000")
    if [ "$code" != "200" ]; then
        STABLE=false
        break
    fi
done
if $STABLE; then
    green "20 rapid dashboard requests all returned 200"
else
    red "Stability test failed — got non-200 during rapid requests"
fi

# Job detail stability
STABLE=true
for i in $(seq 1 10); do
    code=$(curl -s -o /dev/null -w "%{http_code}" -b "$COOKIE_JAR" "$BASE/jobs/detail?id=J$i" 2>/dev/null || echo "000")
    if [ "$code" != "200" ]; then
        STABLE=false
        break
    fi
done
if $STABLE; then
    green "10 rapid job-detail requests all returned 200"
else
    red "Job detail stability test failed"
fi

# --------------------------------------------------
echo ""
echo "━━━ 8. Concurrent Access Simulation ━━━"
# --------------------------------------------------
PIDS=""
TMPDIR_CONC=$(mktemp -d /tmp/ra_conc_XXXX)
for i in $(seq 1 5); do
    (
        CJ="$TMPDIR_CONC/cookie_$i"
        touch "$CJ"
        curl -s -b "$CJ" -c "$CJ" -d "username=alice.ta&password=demo123" -L "$BASE/login" > /dev/null 2>&1
        code=$(curl -s -o /dev/null -w "%{http_code}" -b "$CJ" "$BASE/dashboard" 2>/dev/null || echo "000")
        echo "$code" > "$TMPDIR_CONC/result_$i"
    ) &
    PIDS="$PIDS $!"
done
wait $PIDS 2>/dev/null || true

ALL_OK=true
for i in $(seq 1 5); do
    code=$(cat "$TMPDIR_CONC/result_$i" 2>/dev/null || echo "000")
    if [ "$code" != "200" ]; then
        ALL_OK=false
    fi
done
rm -rf "$TMPDIR_CONC"

if $ALL_OK; then
    green "5 concurrent login+dashboard sessions all succeeded"
else
    red "Concurrent access test had failures"
fi

# --------------------------------------------------
echo ""
echo "══════════════════════════════════════════════════════════"
echo "  Results: $PASS passed, $FAIL failed"
echo "══════════════════════════════════════════════════════════"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
