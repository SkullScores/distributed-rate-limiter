import http from 'k6/http';
import { Counter } from 'k6/metrics';

const allowed = new Counter('allowed');
export const options = { vus: 50, duration: '30s' };

http.setResponseCallback(http.expectedStatuses(200, 429));

export default function () {
  const host = __ITER % 2 ? 'app1':'app2';
  const r = http.get(`http://${host}:8080/api/bucket?user=hot1`, { tags: {name: 'hot'} });

  if (r.status === 200) allowed.add(1);
}
