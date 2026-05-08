import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { SUMMARY_TREND_STATS, buildTags, numberEnv } from '../../lib/config.js';
import { parseJson, refreshSession } from '../../lib/client.js';
import { recordStatusCounters } from '../../lib/metrics.js';

/**
 * Gate test: invalid refresh tokens should be rejected with 401 A004 and never surface as 5xx.
 *
 * Example:
 * k6 run -e RUN_ID=auth-invalid-refresh-001 -e ITERATIONS=10 k6/concurrency/auth/auth-invalid-refresh.js
 */

const FLOW = 'auth-invalid-refresh';
const ITERATIONS = Math.max(1, Math.floor(numberEnv('ITERATIONS', 5)));
const invalidRefreshUnauthorizedCount = new Counter('auth_invalid_refresh_unauthorized_count');
const unexpectedStatusCount = new Counter('auth_invalid_refresh_unexpected_status_count');
const serverErrorCount = new Counter('auth_invalid_refresh_server_error_count');

export const options = {
  scenarios: {
    invalid_refresh_gate: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: ITERATIONS,
      maxDuration: '30s',
      exec: 'main',
      tags: {
        phase: 'main',
        flow: FLOW,
        endpoint: 'invalid_refresh',
      },
    },
  },
  thresholds: {
    dropped_iterations: ['count==0'],
    [`auth_invalid_refresh_unauthorized_count{endpoint:invalid_refresh,phase:main}`]: [`count==${ITERATIONS}`],
    'auth_invalid_refresh_unexpected_status_count{endpoint:invalid_refresh,phase:main}': ['count==0'],
    'auth_invalid_refresh_server_error_count{endpoint:invalid_refresh,phase:main}': ['count==0'],
  },
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function main() {
  const iteration = (exec.scenario.iterationInTest ?? __ITER) + 1;
  const tags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'invalid_refresh',
    iteration,
  });
  const response = refreshSession(`invalid-refresh-${iteration}`, tags, [401]);
  const body = parseJson(response);

  recordStatusCounters(
    response,
    tags,
    { 401: invalidRefreshUnauthorizedCount },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(response, {
    'invalid refresh returns 401': (result) => result.status === 401,
    'invalid refresh returns A004': () => body?.code === 'A004',
  });
}


