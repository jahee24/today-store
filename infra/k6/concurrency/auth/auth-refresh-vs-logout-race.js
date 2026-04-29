import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { SUMMARY_TREND_STATS, BASE_URL, buildTags } from '../../lib/config.js';
import { buildRequestParams, parseJson, refreshSession } from '../../lib/client.js';
import { recordStatusCounters } from '../../lib/metrics.js';
import { buildScenarioCode, setupAuthSessionsForCode } from '../../lib/setup.js';

/**
 * Probe test: race refresh and logout for the same session, then verify whether refresh remains usable.
 *
 * Expected:
 * - no 5xx responses
 * - final refresh state is recorded as either 200 or 401 for analysis
 *
 * Example:
 * k6 run -e RUN_ID=auth-refresh-logout-001 k6/concurrency/auth/auth-refresh-vs-logout-race.js
 */

const FLOW = 'auth-refresh-vs-logout-race';
const refreshSuccessCount = new Counter('auth_refresh_logout_race_refresh_success_count');
const refreshUnauthorizedCount = new Counter('auth_refresh_logout_race_refresh_unauthorized_count');
const logoutSuccessCount = new Counter('auth_refresh_logout_race_logout_success_count');
const finalRefreshSuccessCount = new Counter('auth_refresh_logout_race_final_refresh_success_count');
const finalRefreshUnauthorizedCount = new Counter('auth_refresh_logout_race_final_refresh_unauthorized_count');
const unexpectedStatusCount = new Counter('auth_refresh_logout_race_unexpected_status_count');
const serverErrorCount = new Counter('auth_refresh_logout_race_server_error_count');

export const options = {
  scenarios: {
    refresh_logout_race: {
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
    'auth_refresh_logout_race_unexpected_status_count{phase:main}': ['count==0'],
    'auth_refresh_logout_race_server_error_count{phase:main}': ['count==0'],
  },
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function setup() {
  const code = buildScenarioCode('auth-refresh-logout');
  return setupAuthSessionsForCode(code, 1, FLOW);
}

export function main(data) {
  const session = data.sessions[0];
  const refreshTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'refresh_batch',
  });
  const logoutTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'logout_batch',
  });
  const responses = http.batch([
    [
      'POST',
      `${BASE_URL}/api/v1/auth/refresh`,
      JSON.stringify({ refreshToken: session.refreshToken }),
      buildRequestParams({
        tags: refreshTags,
        expectedStatuses: [200, 401],
        isJson: true,
      }),
    ],
    [
      'POST',
      `${BASE_URL}/api/v1/auth/logout`,
      null,
      buildRequestParams({
        session,
        tags: logoutTags,
        expectedStatuses: [204],
      }),
    ],
  ]);

  const refreshResponse = responses[0];
  const refreshBody = parseJson(refreshResponse);
  recordStatusCounters(
    refreshResponse,
    refreshTags,
    {
      200: refreshSuccessCount,
      401: refreshUnauthorizedCount,
    },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(refreshResponse, {
    'refresh race response is allowed': (response) => response.status === 200 || response.status === 401,
    'refresh race success returns tokens': () =>
      refreshResponse.status !== 200 || Boolean(refreshBody?.accessToken && refreshBody?.refreshToken),
  });

  const logoutResponse = responses[1];
  recordStatusCounters(
    logoutResponse,
    logoutTags,
    { 204: logoutSuccessCount },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(logoutResponse, {
    'logout race returns 204': (response) => response.status === 204,
  });

  const verifyToken = refreshBody?.refreshToken || session.refreshToken;
  const verifyTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'refresh_verify',
  });
  const verifyResponse = refreshSession(verifyToken, verifyTags, [200, 401]);
  const verifyBody = parseJson(verifyResponse);

  recordStatusCounters(
    verifyResponse,
    verifyTags,
    {
      200: finalRefreshSuccessCount,
      401: finalRefreshUnauthorizedCount,
    },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(verifyResponse, {
    'final refresh status is observable': (response) => response.status === 200 || response.status === 401,
    'final refresh success returns tokens': () =>
      verifyResponse.status !== 200 || Boolean(verifyBody?.accessToken && verifyBody?.refreshToken),
  });
}


