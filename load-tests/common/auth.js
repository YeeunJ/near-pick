import http from 'k6/http';

export function getToken(baseUrl) {
  const res = http.post(`${baseUrl}/api/auth/login`, JSON.stringify({
    email: 'consumer1@test.com',
    password: 'test1234',
  }), { headers: { 'Content-Type': 'application/json' } });

  if (res.status !== 200) {
    console.error(`Login failed: ${res.status} ${res.body}`);
    return null;
  }
  return JSON.parse(res.body).data?.accessToken;
}
