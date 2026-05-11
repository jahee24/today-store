import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { SUMMARY_TREND_STATS, buildTags, numberEnv } from '../../lib/config.js';
import { getStoreProfile, parseJson, patchStoreProfile } from '../../lib/client.js';
import { recordStatusCounters } from '../../lib/metrics.js';
import { setupSingleStoreOwnerData } from '../../lib/setup.js';

/**
 * Probe test: race concurrent PATCH /api/v1/stores/me requests that all update the same field.
 *
 * Expected:
 * - all PATCH requests return 200
 * - final storeName is one of the submitted values
 * - no 5xx responses
 *
 * Example:
 * k6 run -e RUN_ID=store-patch-same-field-001 -e CONCURRENCY=8 k6/concurrency/profile/store-patch-same-field-race.js
 */

const FLOW = 'store-patch-same-field-race';
const CONCURRENCY = Math.max(2, Math.floor(numberEnv('CONCURRENCY', 8)));
const patchSuccessCount = new Counter('store_patch_same_field_race_patch_success_count');
const verifyLookupSuccessCount = new Counter('store_patch_same_field_race_verify_lookup_success_count');
const verifyValidStoreNameCount = new Counter('store_patch_same_field_race_verify_valid_store_name_count');
const unexpectedStatusCount = new Counter('store_patch_same_field_race_unexpected_status_count');
const serverErrorCount = new Counter('store_patch_same_field_race_server_error_count');

export const options = {
  scenarios: {
    store_patch_same_field_main: {
      executor: 'per-vu-iterations',
      vus: CONCURRENCY,
      iterations: 1,
      maxDuration: '30s',
      exec: 'main',
      tags: {
        phase: 'main',
        flow: FLOW,
        endpoint: 'store_patch',
      },
    },
    store_patch_same_field_verify: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      startTime: '2s',
      maxDuration: '30s',
      exec: 'verify',
      tags: {
        phase: 'verify',
        flow: FLOW,
        endpoint: 'store_get',
      },
    },
  },
  thresholds: {
    dropped_iterations: ['count==0'],
    [`store_patch_same_field_race_patch_success_count{endpoint:store_patch,phase:main}`]: [`count==${CONCURRENCY}`],
    'store_patch_same_field_race_verify_valid_store_name_count{endpoint:store_get,phase:verify}': ['count==1'],
    'store_patch_same_field_race_unexpected_status_count{flow:store-patch-same-field-race}': ['count==0'],
    'store_patch_same_field_race_server_error_count{flow:store-patch-same-field-race}': ['count==0'],
  },
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function setup() {
  return setupSingleStoreOwnerData('concurrency-store-same-field');
}

export function main(data) {
  const actor = exec.vu.idInTest ?? __VU;
  const tags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'store_patch',
    actor,
  });
  const response = patchStoreProfile(
    data.owner,
    { storeName: buildStoreName(actor) },
    tags,
    [200]
  );
  const body = parseJson(response);

  recordStatusCounters(
    response,
    tags,
    { 200: patchSuccessCount },
    unexpectedStatusCount,
    serverErrorCount
  );
  check(response, {
    'store patch same field returns 200': (result) => result.status === 200,
    'store patch same field returns update marker': () => Boolean(body?.id && body?.updatedAt),
  });
}

export function verify(data) {
  const tags = buildTags({
    flow: FLOW,
    phase: 'verify',
    endpoint: 'store_get',
  });
  const response = getStoreProfile(data.owner, tags, [200]);
  const body = parseJson(response);

  recordStatusCounters(
    response,
    tags,
    { 200: verifyLookupSuccessCount },
    unexpectedStatusCount,
    serverErrorCount
  );

  const allowedNames = buildAllowedStoreNames(CONCURRENCY);
  const validFinalStoreName = allowedNames.indexOf(body?.storeName) !== -1;
  if (validFinalStoreName) {
    verifyValidStoreNameCount.add(1, tags);
  } else {
    unexpectedStatusCount.add(1, {
      ...tags,
      status: 'invalid-final-store-name',
    });
  }

  check(response, {
    'store patch same field verify returns 200': (result) => result.status === 200,
    'store patch same field final storeName is one of submitted values': () => validFinalStoreName,
  });
}

function buildStoreName(actor) {
  return `Concurrent Store ${actor}`;
}

function buildAllowedStoreNames(count) {
  const names = [];

  for (let actor = 1; actor <= count; actor += 1) {
    names.push(buildStoreName(actor));
  }

  return names;
}


