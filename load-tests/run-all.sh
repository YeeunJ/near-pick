#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"

# 토큰 자동 발급 (TEST_TOKEN이 없는 경우)
if [ -z "$TEST_TOKEN" ]; then
  TEST_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"consumer1@test.com","password":"test1234"}' | \
    python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null || echo "")
fi

echo "=== NearPick Load Tests ==="
echo "BASE_URL: $BASE_URL"
echo ""

echo "=== 시나리오 1: 평시 200 TPS ==="
k6 run -e BASE_URL="$BASE_URL" "$SCRIPT_DIR/scenarios/01-normal.js"

echo ""
echo "=== 시나리오 2: 이벤트 3,000 TPS ==="
k6 run -e BASE_URL="$BASE_URL" "$SCRIPT_DIR/scenarios/02-event.js"

echo ""
echo "=== 시나리오 3: 선착순 10,000 동시 요청 ==="
echo "※ 실행 전 products 테이블에서 FLASH_SALE 상품 재고 100으로 설정 필요"
echo "※ 실행 후 아래 SQL로 재고 초과 여부 검증:"
echo "  SELECT COUNT(*) FROM flash_purchases WHERE product_id = \$FLASH_PRODUCT_ID AND status = 'CONFIRMED';"
echo "  SELECT stock FROM products WHERE id = \$FLASH_PRODUCT_ID;"
k6 run \
  -e BASE_URL="$BASE_URL" \
  -e TEST_TOKEN="$TEST_TOKEN" \
  -e FLASH_PRODUCT_ID="${FLASH_PRODUCT_ID:-1}" \
  "$SCRIPT_DIR/scenarios/03-flash.js"

echo ""
echo "=== 완료 ==="
