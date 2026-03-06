import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../common/config.js';

// 사전 조건: FLASH_SALE 타입 상품 ID, 재고 100개 설정 후 실행
const PRODUCT_ID = __ENV.FLASH_PRODUCT_ID || '1';

export const options = {
  scenarios: {
    flash_purchase: {
      executor: 'shared-iterations',
      vus: 10000,
      iterations: 10000,
      maxDuration: '30s',
    },
  },
  thresholds: {
    // 핵심: 재고 초과 구매 없음 → 테스트 후 DB 검증 필수
    http_req_failed:   ['rate<0.01'],  // 5xx 에러율 1% 미만 (429는 정상)
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  const payload = JSON.stringify({ productId: parseInt(PRODUCT_ID), quantity: 1 });
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${__ENV.TEST_TOKEN}`,
  };

  const res = http.post(`${BASE_URL}/api/flash-purchases`, payload, { headers });

  // 201(성공), 409(재고부족/충돌), 429(rate limit) 허용
  check(res, {
    'acceptable status': (r) => [201, 409, 429].includes(r.status),
  });
}
