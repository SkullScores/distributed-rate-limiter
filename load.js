import http from 'k6/http';

export const options = {
  vus: 50,
  duration: '30s',
  summaryTrendStats: ['avg','med','p(95)', 'p(99)','max'],
};

http.setResponseCallback(http.expectedStatuses(200,429));

export default function () {
  const host = __ITER % 2 ? 'app1' : 'app2';
  const user = Math.floor(Math.random()*1000);

  http.get(`http://${host}:8080/api/bucket?user=u${user}`, { tags: {name: 'bucket'} });
}
