export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const defaultThresholds = {
  http_req_failed:   ['rate<0.01'],
  http_req_duration: ['p(95)<500'],
};
