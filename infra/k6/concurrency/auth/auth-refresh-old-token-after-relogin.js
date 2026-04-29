import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { SUMMARY_TREND_STATS, buildTags } from '../../lib/config.js';
import { parseJson, refreshSession } from '../../lib/client.js';
import { recordStatusCounters } from '../../lib/metrics.js';
import { buildScenarioCode, loginSessionForCode } from '../../lib/setup.js';

/**
 * Gate test: after the same user logs in twice, only the latest refresh token should remain valid.
 *
 * Expected:
 * - old refresh token -> 401 A004
 * - latest refresh token -> 200 with rotated tokens
 *
 * Example:
 * k6 run -e RUN_ID=auth-relogin-001 k6/concurrency/auth/auth-refresh-old-token-after-relogin.js
 */

const FLOW = 'auth-refresh-old-token-after-relogin';
const oldRefreshUnauthorizedCount = new Counter('auth_refresh_relogin_old_unauthorized_count');
const latestRefreshSuccessCount = new Counter('auth_refresh_relogin_latest_success_count');
const unexpectedStatusCount = new Counter('auth_refresh_relogin_unexpected_status_count');
const serverErrorCount = new Counter('auth_refresh_relogin_server_error_count');

export const options = {
  scenarios: {
    refresh_relogin_gate: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '30s',
      exec: 'main',
      tags: {
        phase: 'main',
        flow: FLOW,
      },
    },
  },
  thresholds: {
    dropped_iterations: ['count==0'],
    'auth_refresh_relogin_old_unauthorized_count{endpoint:old_refresh,phase:main}': ['count==1'],
    'auth_refresh_relogin_latest_success_count{endpoint:latest_refresh,phase:main}': ['count==1'],
    'auth_refresh_relogin_unexpected_status_count{phase:main}': ['count==0'],
    'auth_refresh_relogin_server_error_count{phase:main}': ['count==0'],
  },
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function setup() {
  const code = buildScenarioCode('auth-relogin');
  const oldSession = loginSessionForCode(code, FLOW, 1);

  // Intentionally do not wait here. This regression must exercise a rapid
  // re-login path (<1s gap) so JWTs without a unique claim (for example jti)
  // can reproduce as byte-identical old/latest refresh tokens.
  const latestSession = loginSessionForCode(code, FLOW, 2);

  return {
    code,
    sessions: [oldSession, latestSession],
  };
}

export function main(data) {
  const oldTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'old_refresh',
  });
  const oldResponse = refreshSession(data.sessions[0].refreshToken, oldTags, [401]);
  const oldBody = parseJson(oldResponse);

  recordStatusCounters(
    oldResponse,
    oldTags,
    { 401: oldRefreshUnauthorizedCount },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(oldResponse, {
    'old refresh token returns 401': (response) => response.status === 401,
    'old refresh token returns A004': () => oldBody?.code === 'A004',
  });

  const latestTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'latest_refresh',
  });
  const latestResponse = refreshSession(data.sessions[1].refreshToken, latestTags, [200]);
  const latestBody = parseJson(latestResponse);

  recordStatusCounters(
    latestResponse,
    latestTags,
    { 200: latestRefreshSuccessCount },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(latestResponse, {
    'latest refresh token returns 200': (response) => response.status === 200,
    'latest refresh token rotates tokens': () =>
      Boolean(latestBody?.accessToken && latestBody?.refreshToken),
  });
}



