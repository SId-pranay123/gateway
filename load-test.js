import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const allowedRequests = new Counter('allowed_requests');
const rateLimitedRequests = new Counter('rate_limited_requests');

export const options = {
  scenarios: {
    // Scenario 1: steady load — stay within rate limit
    steady_load: {
      executor: 'constant-arrival-rate',
      rate: 3,              // 3 requests per second
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 5,
      tags: { scenario: 'steady' },
      startTime: '0s',
    },
    // Scenario 2: burst — exceed rate limit intentionally
    burst: {
      executor: 'constant-arrival-rate',
      rate: 20,             // 20 requests per second — well above the limit
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 30,
      tags: { scenario: 'burst' },
      startTime: '35s',     // starts after steady_load finishes
    },
    // Scenario 3: two tenants simultaneously — prove isolation
    multi_tenant: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 40,
      tags: { scenario: 'multi_tenant' },
      startTime: '70s',
    },
  },
  thresholds: {
    http_req_duration: ['p(99)<500'],   // 99% of requests under 500ms
    http_req_failed: ['rate<0.01'],     // less than 1% unexpected failures
                                        // (429s are expected, not failures)
  },
};

const BASE_URL = 'http://localhost:8080';
const TENANT_1_KEY = 'test-key-1';
const TENANT_2_KEY = 'test-key-2'; // create this tenant before running

export default function () {
  const scenario = __ENV.SCENARIO || 'steady';

  let apiKey;
  if (__ITER % 2 === 0 || scenario !== 'multi_tenant') {
    apiKey = TENANT_1_KEY;
  } else {
    apiKey = TENANT_2_KEY;
  }

  const res = http.get(`${BASE_URL}/api/status`, {
    headers: { 'X-API-Key': apiKey },
  });

  if (res.status === 200) {
    allowedRequests.add(1);
  } else if (res.status === 429) {
    rateLimitedRequests.add(1);
  }

  // We deliberately do NOT check for 429 as a failure —
  // rate limiting working correctly IS the expected behavior.
  check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    'no unexpected errors': (r) => r.status !== 500 && r.status !== 503,
  });
}