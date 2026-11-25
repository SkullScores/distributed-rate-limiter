# Distributed Rate Limiter

Per-user rate limiting shared across multiple app instances.
Java 17, Spring Boot, Redis (Lua scripts), Docker Compose, k6.

## Endpoints

All allow 5 requests per 60 seconds per user. Allowed requests get `200`, rejected ones `429`.

| Endpoint | Algorithm | State per user | Trade-off |
|---|---|---|---|
| `/api/check?user=x` | Fixed window (starts at first request) | one counter | bursts around the reset |
| `/api/sliding?user=x` | Sliding window log | one timestamp per allowed request | exact, memory grows with the limit |
| `/api/bucket?user=x` | Token bucket | token count and last update time | allows a burst up to capacity |

## Why Lua

Reading and updating a counter as separate commands lets two instances read the same value
and both allow a request. Redis runs a Lua script without executing other commands in between,
so check and update happen as one step. The sliding window and token bucket scripts use
Redis `TIME`, so both instances share one clock.

## Run

```powershell
.\mvnw.cmd package "-DskipTests"
docker compose up --build
curl.exe "http://localhost:8081/api/bucket?user=demo"
```

Instances listen on ports 8081 and 8082.

## Load tests (token bucket)

```powershell
Get-Content load.js -Raw | docker run --rm -i --network rate-limiter_default grafana/k6 run -
Get-Content burst.js -Raw | docker run --rm -i --network rate-limiter_default grafana/k6 run -
```

Setup: i7-1255U laptop (16 GB), 2 app instances, 1 Redis, 50 virtual users for 30 s,
k6 running on the same machine.

| Test | Requests | Throughput | p95 | p99 |
|---|---|---|---|---|
| 1,000 random users (mostly rejections) | 502,344 | 16,743 req/s | 5.47 ms | 7.75 ms |
| One hot user | 525,235 | 17,507 req/s | 5.12 ms | not recorded |

In the hot-user test exactly 7 requests were allowed, the expected maximum
(5 at the start, plus one refill at 12 s and one at 24 s).

To repeat the hot-user test within a minute, change the user ID in `burst.js`.

## Limitations

- Single Redis instance, with no handling for it being unavailable
- Limits are hardcoded
- The client is identified by a query parameter, which is easy to spoof
- No automated tests of the limiting logic
- Only the token bucket was load tested
