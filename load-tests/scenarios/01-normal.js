import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, defaultThresholds } from '../common/config.js';

export const options = {
  scenarios: {
    normal_load: {
      executor: 'constant-arrival-rate',
      rate: 200,
      timeUnit: '1s',
      duration: '1m',
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
  },
  thresholds: {
    ...defaultThresholds,
    http_req_duration: ['p(95)<200'],
  },
};

export default function () {
  // 근처 상품 조회 (캐시 히트 확인)
  const nearbyRes = http.get(
    `${BASE_URL}/api/products/nearby?lat=37.5563&lng=126.9236&radius=5&sort=POPULARITY`
  );
  check(nearbyRes, { 'nearby status 200': (r) => r.status === 200 });

  // 상품 상세 조회
  const detailRes = http.get(`${BASE_URL}/api/products/1`);
  check(detailRes, { 'detail status 200 or 404': (r) => [200, 404].includes(r.status) });

  sleep(0.5);
}
