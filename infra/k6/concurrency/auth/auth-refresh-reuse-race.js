import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { SUMMARY_TREND_STATS, buildTags, numberEnv } from '../../lib/config.js';
import { parseJson, refreshSession } from '../../lib/client.js';
import { recordStatusCounters } from '../../lib/metrics.js';
import { buildScenarioCode, setupAuthSessionsForCode } from '../../lib/setup.js';

/**
 * Probe test: race the same refresh token across multiple concurrent requests.
 *
 * Expected:
 * - ideal distribution is 1x 200 + (N-1)x 401
 * - no 5xx responses
 *
 * Example:
 * k6 run -e RUN_ID=auth-refresh-race-001 -e CONCURRENCY=10 k6/concurrency/auth/auth-refresh-reuse-race.js
 */

const FLOW = 'auth-refresh-reuse-race';
const CONCURRENCY = Math.max(2, Math.floor(numberEnv('CONCURRENCY', 8)));
const refreshSuccessCount = new Counter('auth_refresh_reuse_success_count');
const refreshUnauthorizedCount = new Counter('auth_refresh_reuse_unauthorized_count');
const unexpectedStatusCount = new Counter('auth_refresh_reuse_unexpected_status_count');
const serverErrorCount = new Counter('auth_refresh_reuse_server_error_count');

export const options = {
  scenarios: {
    refresh_reuse_race: {
      executor: 'per-vu-iterations',
      vus: CONCURRENCY,
      iterations: 1,
      maxDuration: '30s',
      exec: 'main',
      tags: {
        phase: 'main',
        flow: FLOW,
        endpoint: 'refresh_race',
      },
    },
  },
  thresholds: {
    dropped_iterations: ['count==0'],
    'auth_refresh_reuse_unexpected_status_count{phase:main,endpoint:refresh_race}': ['count==0'],
    'auth_refresh_reuse_server_error_count{phase:main,endpoint:refresh_race}': ['count==0'],
  },
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function setup() {
  const code = buildScenarioCode('auth-refresh-race');
  return setupAuthSessionsForCode(code, 1, FLOW);
}

export function main(data) {
  const tags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'refresh_race',
    actor: exec.vu.idInTest ?? __VU,
  });
  const response = refreshSession(data.sessions[0].refreshToken, tags, [200, 401]);
  const body = parseJson(response);

  recordStatusCounters(
    response,
    tags,
    {
      200: refreshSuccessCount,
      401: refreshUnauthorizedCount,
    },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(response, {
    'refresh reuse race returns 200 or 401': (result) => result.status === 200 || result.status === 401,
    'successful reuse race refresh returns tokens': () =>
      response.status !== 200 || Boolean(body?.accessToken && body?.refreshToken),
  });
}


