import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { SUMMARY_TREND_STATS, BASE_URL, buildTags, numberEnv } from '../../lib/config.js';
import { buildRequestParams, getStoreProfile, parseJson } from '../../lib/client.js';
import { recordStatusCounters } from '../../lib/metrics.js';
import { setupSingleStoreOwnerData } from '../../lib/setup.js';

/**
 * Probe test: race two disjoint PATCH /api/v1/stores/me updates and record whether both survive.
 *
 * Expected:
 * - both PATCH requests return 200
 * - final state is recorded as merged or lost-update
 * - no 5xx responses
 *
 * Example:
 * k6 run -e RUN_ID=store-patch-disjoint-001 -e ROUNDS=5 k6/concurrency/profile/store-patch-disjoint-field-race.js
 */

const FLOW = 'store-patch-disjoint-field-race';
const ROUNDS = Math.max(1, Math.floor(numberEnv('ROUNDS', 5)));
const patchSuccessCount = new Counter('store_patch_disjoint_field_race_patch_success_count');
const verifyLookupSuccessCount = new Counter('store_patch_disjoint_field_race_verify_lookup_success_count');
const mergeSuccessCount = new Counter('store_patch_disjoint_field_race_merge_success_count');
const lostUpdateCount = new Counter('store_patch_disjoint_field_race_lost_update_count');
const unexpectedStatusCount = new Counter('store_patch_disjoint_field_race_unexpected_status_count');
const serverErrorCount = new Counter('store_patch_disjoint_field_race_server_error_count');

export const options = {
  scenarios: {
    store_patch_disjoint_rounds: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: ROUNDS,
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
    'store_patch_disjoint_field_race_unexpected_status_count{flow:store-patch-disjoint-field-race}': ['count==0'],
    'store_patch_disjoint_field_race_server_error_count{flow:store-patch-disjoint-field-race}': ['count==0'],
  },
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function setup() {
  return setupSingleStoreOwnerData('concurrency-store-disjoint');
}

export function main(data) {
  const round = (exec.scenario.iterationInTest ?? __ITER) + 1;
  const namePayload = {
    storeName: `Disjoint Store ${round}`,
    preferredStyle: 'MEME',
  };
  const addressPayload = {
    address: `Seoul Disjoint-gil ${round}`,
    snsInstagram: `@disjoint_${round}`,
  };

  const firstTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'store_patch_name',
    round,
  });
  const secondTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'store_patch_address',
    round,
  });
  const responses = http.batch([
    [
      'PATCH',
      `${BASE_URL}/api/v1/stores/me`,
      JSON.stringify(namePayload),
      buildRequestParams({
        session: data.owner,
        tags: firstTags,
        expectedStatuses: [200],
        isJson: true,
      }),
    ],
    [
      'PATCH',
      `${BASE_URL}/api/v1/stores/me`,
      JSON.stringify(addressPayload),
      buildRequestParams({
        session: data.owner,
        tags: secondTags,
        expectedStatuses: [200],
        isJson: true,
      }),
    ],
  ]);

  recordStatusCounters(
    responses[0],
    firstTags,
    { 200: patchSuccessCount },
    unexpectedStatusCount,
    serverErrorCount
  );
  recordStatusCounters(
    responses[1],
    secondTags,
    { 200: patchSuccessCount },
    unexpectedStatusCount,
    serverErrorCount
  );

  check(responses[0], {
    'disjoint patch first request returns 200': (response) => response.status === 200,
  });
  check(responses[1], {
    'disjoint patch second request returns 200': (response) => response.status === 200,
  });

  const verifyTags = buildTags({
    flow: FLOW,
    phase: 'main',
    endpoint: 'store_get_verify',
    round,
  });
  const verifyResponse = getStoreProfile(data.owner, verifyTags, [200]);
  const verifyBody = parseJson(verifyResponse);

  recordStatusCounters(
    verifyResponse,
    verifyTags,
    { 200: verifyLookupSuccessCount },
    unexpectedStatusCount,
    serverErrorCount
  );

  const merged =
    verifyBody?.storeName === namePayload.storeName &&
    verifyBody?.preferredStyle === namePayload.preferredStyle &&
    verifyBody?.address === addressPayload.address &&
    verifyBody?.sns?.instagram === addressPayload.snsInstagram;

  if (merged) {
    mergeSuccessCount.add(1, verifyTags);
  } else {
    lostUpdateCount.add(1, verifyTags);
  }

  check(verifyResponse, {
    'disjoint patch verify returns 200': (response) => response.status === 200,
    'disjoint patch final state is observable': () => Boolean(verifyBody?.id && verifyBody?.storeName),
  });
}


