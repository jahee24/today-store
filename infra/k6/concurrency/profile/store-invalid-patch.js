import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { SUMMARY_TREND_STATS, BASE_URL, buildTags, numberEnv } from '../../lib/config.js';
import { buildRequestParams, parseJson } from '../../lib/client.js';
import { recordStatusCounters } from '../../lib/metrics.js';
import { setupSingleStoreOwnerData } from '../../lib/setup.js';

/**
 * Gate test: invalid or unauthorized PATCH /api/v1/stores/me requests should fail as 4xx and never surface as 5xx.
 *
 * Expected:
 * - invalid enum -> 400
 * - missing auth -> 401 A005
 * - invalid token -> 401 A005
 *
 * Example:
 * k6 run -e RUN_ID=store-invalid-patch-001 -e ITERATIONS=3 k6/concurrency/profile/store-invalid-patch.js
 */

const FLOW = 'store-invalid-patch';
const ITERATIONS = Math.max(1, Math.floor(numberEnv('ITERATIONS', 3)));
const invalidEnumBadRequestCount = new Counter('store_invalid_patch_invalid_enum_bad_request_count');
const missingAuthUnauthorizedCount = new Counter('store_invalid_patch_missing_auth_unauthorized_count');
const invalidTokenUnauthorizedCount = new Counter('store_invalid_patch_invalid_token_unauthorized_count');
const unexpectedStatusCount = new Counter('store_invalid_patch_unexpected_status_count');
const serverErrorCount = new Counter('store_invalid_patch_server_error_count');

export const options = {
  scenarios: {
    store_invalid_patch_enum: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: ITERATIONS,
      maxDuration: '30s',
      exec: 'invalidEnum',
      tags: {
        phase: 'main',
        flow: FLOW,
        endpoint: 'invalid_enum',
      },
    },
    store_invalid_patch_missing_auth: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: ITERATIONS,
      maxDuration: '30s',
      exec: 'missingAuth',
      tags: {
        phase: 'main',
        flow: FLOW,
        endpoint: 'missing_auth',
      },
    },
    store_invalid_patch_invalid_token: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: ITERATIONS,
      maxDuration: '30s',
      exec: 'invalidToken',
      tags: {
        phase: 'main',
        flow: FLOW,
        endpoint: 'invalid_token',
      },
    },
  },
  thresholds: {
    dropped_iterations: ['count==0'],
    [`store_invalid_patch_invalid_enum_bad_request_count{endpoint:invalid_enum,phase:main}`]: [`count==${ITERATIONS}`],
    [`store_invalid_patch_missing_auth_unauthorized_count{endpoint:missing_auth,phase:main}`]: [`count==${ITERATIONS}`],
    [`store_invalid_patch_invalid_token_unauthorized_count{endpoint:invalid_token,phase:main}`]: [`count==${ITERATIONS}`],
    'store_invalid_patch_unexpected_status_count{flow:store-invalid-patch}': ['count==0'],
    'store_invalid_patch_server_error_count{flow:store-invalid-patch}': ['count==0'],
  },
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function setup() {
  return setupSingleStoreOwnerData('concurrency-store-invalid');
}

export function invalidEnum(data) {
  const tags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'invalid_enum',
  });
  const response = http.patch(
    `${BASE_URL}/api/v1/stores/me`,
    JSON.stringify({ preferredStyle: 'INVALID_STYLE' }),
    buildRequestParams({
      session: data.owner,
      tags,
      expectedStatuses: [400],
      isJson: true,
    })
  );

  recordStatusCounters(
    response,
    tags,
    { 400: invalidEnumBadRequestCount },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(response, {
    'invalid store enum returns 400': (result) => result.status === 400,
  });
}

export function missingAuth() {
  const tags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'missing_auth',
  });
  const response = http.patch(
    `${BASE_URL}/api/v1/stores/me`,
    JSON.stringify({ storeName: 'Updated Store' }),
    buildRequestParams({
      tags,
      expectedStatuses: [401],
      isJson: true,
    })
  );
  const body = parseJson(response);

  recordStatusCounters(
    response,
    tags,
    { 401: missingAuthUnauthorizedCount },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(response, {
    'missing auth store patch returns 401': (result) => result.status === 401,
    'missing auth store patch returns A005': () => body?.code === 'A005',
  });
}

export function invalidToken() {
  const tags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'invalid_token',
  });
  const response = http.patch(
    `${BASE_URL}/api/v1/stores/me`,
    JSON.stringify({ storeName: 'Updated Store' }),
    buildRequestParams({
      tags,
      expectedStatuses: [401],
      isJson: true,
      authorization: 'Bearer invalid-token',
    })
  );
  const body = parseJson(response);

  recordStatusCounters(
    response,
    tags,
    { 401: invalidTokenUnauthorizedCount },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(response, {
    'invalid token store patch returns 401': (result) => result.status === 401,
    'invalid token store patch returns A005': () => body?.code === 'A005',
  });
}


