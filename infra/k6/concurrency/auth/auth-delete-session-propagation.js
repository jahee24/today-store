import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { SUMMARY_TREND_STATS, buildTags } from '../../lib/config.js';
import { createStore, deleteMyAccount, getStoreProfile, getUserProfile, parseJson } from '../../lib/client.js';
import { recordStatusCounters } from '../../lib/metrics.js';
import { buildStoreCreatePayload } from '../../lib/payloads.js';
import { buildScenarioCode, setupAuthSessionsForCode } from '../../lib/setup.js';

/**
 * Probe test: create two sessions for the same user, delete the account with one session,
 * then observe whether the second access token still reaches protected resources.
 *
 * Expected:
 * - delete request returns 204
 * - follow-up statuses are recorded for analysis
 * - no 5xx responses
 *
 * Example:
 * k6 run -e RUN_ID=auth-delete-propagation-001 k6/concurrency/auth/auth-delete-session-propagation.js
 */

const FLOW = 'auth-delete-session-propagation';
const deleteSuccessCount = new Counter('auth_delete_session_propagation_delete_success_count');
const survivorUserAccessibleCount = new Counter('auth_delete_session_propagation_user_accessible_count');
const survivorUserBlockedCount = new Counter('auth_delete_session_propagation_user_blocked_count');
const survivorStoreAccessibleCount = new Counter('auth_delete_session_propagation_store_accessible_count');
const survivorStoreBlockedCount = new Counter('auth_delete_session_propagation_store_blocked_count');
const survivorStoreMissingCount = new Counter('auth_delete_session_propagation_store_missing_count');
const unexpectedStatusCount = new Counter('auth_delete_session_propagation_unexpected_status_count');
const serverErrorCount = new Counter('auth_delete_session_propagation_server_error_count');

export const options = {
  scenarios: {
    delete_session_probe: {
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
    'auth_delete_session_propagation_delete_success_count{endpoint:delete_account,phase:main}': ['count==1'],
    'auth_delete_session_propagation_unexpected_status_count{phase:main}': ['count==0'],
    'auth_delete_session_propagation_server_error_count{phase:main}': ['count==0'],
  },
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function setup() {
  const code = buildScenarioCode('auth-delete-session');
  const data = setupAuthSessionsForCode(code, 2, FLOW);
  const seedResponse = createStore(
    data.sessions[0],
    buildStoreCreatePayload(1),
    buildTags({
      flow: FLOW,
      phase: 'setup',
      endpoint: 'store_seed',
    }),
    [201, 409]
  );

  if (seedResponse.status !== 201 && seedResponse.status !== 409) {
    throw new Error(`Store seed failed. Status: ${seedResponse.status}`);
  }

  return data;
}

export function main(data) {
  const deleteSession = data.sessions[0];
  const survivorSession = data.sessions[1];

  const deleteTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'delete_account',
  });
  const deleteResponse = deleteMyAccount(deleteSession, deleteTags, [204]);

  recordStatusCounters(
    deleteResponse,
    deleteTags,
    { 204: deleteSuccessCount },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(deleteResponse, {
    'delete account returns 204': (response) => response.status === 204,
  });

  const userTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'survivor_user_profile',
  });
  const userResponse = getUserProfile(survivorSession, userTags, [200, 401, 403]);
  const userBody = parseJson(userResponse);

  recordStatusCounters(
    userResponse,
    userTags,
    {
      200: survivorUserAccessibleCount,
      401: survivorUserBlockedCount,
      403: survivorUserBlockedCount,
    },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(userResponse, {
    'survivor profile status is observable': (response) =>
      response.status === 200 || response.status === 401 || response.status === 403,
    'survivor profile body exists when accessible': () =>
      userResponse.status !== 200 || Boolean(userBody?.id && userBody?.email),
  });

  const storeTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'survivor_store_profile',
  });
  const storeResponse = getStoreProfile(survivorSession, storeTags, [200, 401, 403, 404]);
  const storeBody = parseJson(storeResponse);

  recordStatusCounters(
    storeResponse,
    storeTags,
    {
      200: survivorStoreAccessibleCount,
      401: survivorStoreBlockedCount,
      403: survivorStoreBlockedCount,
      404: survivorStoreMissingCount,
    },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(storeResponse, {
    'survivor store status is observable': (response) =>
      response.status === 200 ||
      response.status === 401 ||
      response.status === 403 ||
      response.status === 404,
    'survivor store body exists when accessible': () =>
      storeResponse.status !== 200 || Boolean(storeBody?.id && storeBody?.storeName),
  });
}


