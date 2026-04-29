import exec from 'k6/execution';
import { check, sleep } from 'k6';
import { SUMMARY_TREND_STATS, buildTags, numberEnv } from '../../lib/config.js';
import { createStore, parseJson } from '../../lib/client.js';
import { recordDuplicateCreateResponse } from '../../lib/metrics.js';
import { buildStoreCreatePayload } from '../../lib/payloads.js';
import { setupDuplicateCreateData } from '../../lib/setup.js';

/**
 * Duplicate store creation race test.
 *
 * Each round fans out one user to N concurrent POST /api/v1/stores requests and expects:
 * 1x 201 Created + (N-1)x 409 Conflict.
 *
 * Use a unique RUN_ID for each execution. setup data is not intended to be reused.
 *
 * Examples:
 * k6 run -e RUN_ID=profile-duplicate-001 k6/concurrency/profile/store-create-duplicate.js
 * k6 run -e RUN_ID=profile-duplicate-smoke -e CONCURRENCY=10 -e ROUNDS=5 k6/concurrency/profile/store-create-duplicate.js
 */

const FLOW = 'store-create-duplicate';
const ENDPOINT = 'store_create_duplicate';
const CONCURRENCY = Math.max(1, Math.floor(numberEnv('CONCURRENCY', 50)));
const ROUNDS = Math.max(1, Math.floor(numberEnv('ROUNDS', 20)));
const ROUND_INTERVAL_SECONDS = 2;
const ROUND_START_DELAY_SECONDS = 5;
const MAX_DURATION_SECONDS = ROUND_START_DELAY_SECONDS + (ROUNDS * ROUND_INTERVAL_SECONDS) + 20;

export const options = {
  scenarios: {
    duplicate_create_main: {
      executor: 'per-vu-iterations',
      vus: CONCURRENCY,
      iterations: ROUNDS,
      maxDuration: `${MAX_DURATION_SECONDS}s`,
      exec: 'main',
      tags: {
        phase: 'main',
        flow: FLOW,
        endpoint: ENDPOINT,
      },
    },
  },
  thresholds: {
    dropped_iterations: ['count==0'],
    'http_req_failed{endpoint:store_create_duplicate,phase:main}': ['rate==0'],
    'profile_duplicate_create_created_count{endpoint:store_create_duplicate,phase:main}': [`count==${ROUNDS}`],
    'profile_duplicate_create_conflict_count{endpoint:store_create_duplicate,phase:main}': [
      `count==${ROUNDS * (CONCURRENCY - 1)}`,
    ],
    'profile_duplicate_create_unexpected_status_count{endpoint:store_create_duplicate,phase:main}': ['count==0'],
    'profile_duplicate_create_server_error_count{endpoint:store_create_duplicate,phase:main}': ['count==0'],
  },
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function setup() {
  return setupDuplicateCreateData(ROUNDS, ROUND_START_DELAY_SECONDS);
}

export function main(data) {
  const roundIndex = currentRoundIndex();
  waitForRound(data.roundZeroAt, roundIndex);

  const session = data.duplicateUsers[roundIndex % data.duplicateUsers.length];
  const tags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: ENDPOINT,
    round: roundIndex + 1,
  });
  const response = createStore(
    session,
    buildStoreCreatePayload(session.userNumber),
    tags,
    [201, 409]
  );
  const body = parseJson(response);

  recordDuplicateCreateResponse(response, tags);
  check(response, {
    'duplicate create returns 201 or 409': (res) => res.status === 201 || res.status === 409,
    'duplicate create response shape is valid': () =>
      response.status === 409 || Boolean(body?.id && body?.createdAt),
  });
}

function currentRoundIndex() {
  return exec.vu.iterationInScenario ?? __ITER;
}

function waitForRound(roundZeroAt, roundIndex) {
  const targetTime = roundZeroAt + (roundIndex * ROUND_INTERVAL_SECONDS * 1000);
  const remainingMs = targetTime - Date.now();

  if (remainingMs > 0) {
    sleep(remainingMs / 1000);
  }
}


