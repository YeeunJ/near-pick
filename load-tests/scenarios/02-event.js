import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, defaultThresholds } from '../common/config.js';

export const options = {
  scenarios: {
    event_load: {
      executor: 'ramping-arrival-rate',
      startRate: 100,
      timeUnit: '1s',
      preAllocatedVUs: 500,
      maxVUs: 3000,
      stages: [
        { target: 3000, duration: '30s' },
        { target: 3000, duration: '30s' },
      ],
    },
  },
  thresholds: {
    ...defaultThresholds,
    http_req_duration: ['p(95)<500'],
    http_req_failed:   ['rate<0.02'],
  },
};

export default function () {
  const res = http.get(
    `${BASE_URL}/api/products/nearby?lat=37.5563&lng=126.9236&radius=5&sort=POPULARITY`
  );
  check(res, { 'status 200': (r) => r.status === 200 });
}
