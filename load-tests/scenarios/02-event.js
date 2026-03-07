import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../common/config.js';

// 단계별 ramping: 포화점 탐색 + Tomcat 튜닝 후 최대 처리량 확인
export const options = {
  scenarios: {
    event_load: {
      executor: 'ramping-arrival-rate',
      startRate: 200,
      timeUnit: '1s',
      preAllocatedVUs: 300,
      maxVUs: 1500,
      stages: [
        { target: 500,  duration: '20s' },  // 워밍업
        { target: 1000, duration: '20s' },  // 중간 부하
        { target: 1500, duration: '20s' },  // 고부하
        { target: 2000, duration: '20s' },  // 한계 탐색
        { target: 2000, duration: '20s' },  // 지속성 확인
      ],
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.05'],    // 5% 이내 (고부하 구간 일부 실패 허용)
    http_req_duration: ['p(95)<2000'],   // 95th percentile 2s 이내
  },
};

export default function () {
  const res = http.get(
    `${BASE_URL}/api/products/nearby?lat=37.5563&lng=126.9236&radius=5&sort=POPULARITY`
  );
  check(res, { 'status 200': (r) => r.status === 200 });
}
