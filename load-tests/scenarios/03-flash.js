import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../common/config.js';

// 사전 조건: FLASH_SALE 타입 상품 ID, 재고 100개 설정 후 실행
const PRODUCT_ID = __ENV.FLASH_PRODUCT_ID || '1';
const TOTAL_USERS = 100;

export const options = {
  scenarios: {
    flash_purchase: {
      executor: 'shared-iterations',
      vus: 200,        // OS 소켓 한계 내: 연결 타임아웃 제거
      iterations: 500, // 100명 × 5회 시도 → CONFIRMED는 stock 한도(100) 내로 제한됨
      maxDuration: '90s',
    },
  },
  thresholds: {
    // 핵심: 재고 초과 구매 없음 → 테스트 후 DB 검증 필수
    http_req_failed:   ['rate<0.01'],   // 5xx 에러율 1% 미만 (409/429는 정상)
    http_req_duration: ['p(95)<3000'],  // 분산 락 포함 3s 이내
  },
};

// setup(): 100명 로그인 → 토큰 배열 반환 (auth rate-limit: 200/min on local)
export function setup() {
  const tokens = [];
  for (let i = 1; i <= TOTAL_USERS; i++) {
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ email: `flashtest${i}@test.com`, password: 'test1234' }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status === 200) {
      const body = JSON.parse(res.body);
      if (body.data && body.data.accessToken) {
        tokens.push(body.data.accessToken);
      }
    }
  }
  console.log(`Loaded ${tokens.length} tokens`);
  return { tokens };
}

export default function (data) {
  // 각 VU가 고유한 사용자 토큰 사용 → 고유한 멱등성 키 → 사용자별 1회 구매 보장
  const token = data.tokens[__VU % data.tokens.length];
  const payload = JSON.stringify({ productId: parseInt(PRODUCT_ID), quantity: 1 });
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  const res = http.post(`${BASE_URL}/api/flash-purchases`, payload, { headers });

  // 201(성공), 409(재고부족/충돌), 429(rate limit) 허용
  check(res, {
    'acceptable status': (r) => [201, 409, 429].includes(r.status),
  });
}
