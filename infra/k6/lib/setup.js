import { DEFAULT_PROFILE_USER_COUNT, RUN_ID } from './config.js';
import { createStore, getStoreProfile, loginWithMock, parseJson } from './client.js';
import { buildStoreCreatePayload } from './payloads.js';

export function setupUserProfileData(count = DEFAULT_PROFILE_USER_COUNT) {
  return {
    users: loginUsers(count, 'user'),
  };
}

export function setupStoreOwnerData(count = DEFAULT_PROFILE_USER_COUNT) {
  const owners = loginUsers(count, 'owner');
  seedOwnerStores(owners);

  return { owners };
}

export function setupMixedData(count = DEFAULT_PROFILE_USER_COUNT) {
  const owners = loginUsers(count, 'mixed-owner');
  seedOwnerStores(owners);

  return { owners };
}

export function setupStoreCreateData(count) {
  validatePositiveCount(count, 'store-create');

  return {
    createUsers: loginUsers(count, 'store-create'),
  };
}

export function setupDuplicateCreateData(roundCount, roundStartDelaySeconds) {
  validatePositiveCount(roundCount, 'duplicate-create');

  return {
    duplicateUsers: loginUsers(roundCount, 'duplicate-create'),
    roundZeroAt: Date.now() + (roundStartDelaySeconds * 1000),
  };
}

export function setupSingleUserProfileData(poolName = 'single-user') {
  const users = loginUsers(1, poolName);
  return {
    user: users[0],
    users,
  };
}

export function setupSingleStoreOwnerData(poolName = 'single-owner') {
  const owners = loginUsers(1, poolName);
  seedOwnerStores(owners);

  return {
    owner: owners[0],
    owners,
  };
}

export function setupAuthSessionsForCode(code, count, flow = 'auth') {
  validatePositiveCount(count, flow);

  return {
    code,
    sessions: loginSessionsForCode(code, count, flow),
  };
}

export function loginSessionForCode(code, flow = 'auth', index = 1) {
  const response = loginWithMock(code, {
    phase: 'setup',
    flow,
    endpoint: 'oauth_login_setup',
    user_number: index,
  });
  const body = parseJson(response);

  if (response.status !== 200 || !body?.accessToken || !body?.refreshToken) {
    throw new Error(`Setup login failed for ${flow} code ${code}. Status: ${response.status}`);
  }

  return {
    userNumber: index,
    code,
    email: body?.user?.email || `${code}@loadtest.example.test`,
    accessToken: body.accessToken,
    refreshToken: body.refreshToken,
  };
}

export function loginSessionsForCode(code, count, flow = 'auth') {
  validatePositiveCount(count, flow);

  const sessions = [];

  for (let index = 1; index <= count; index += 1) {
    sessions.push(loginSessionForCode(code, flow, index));
  }

  return sessions;
}

export function buildScenarioCode(scope, index = 1) {
  return buildMockCode('concurrency', scope, index);
}

function loginUsers(count, poolName) {
  const sessions = [];

  for (let userNumber = 1; userNumber <= count; userNumber += 1) {
    const code = buildUserCode(poolName, userNumber);
    const response = loginWithMock(code, {
      phase: 'setup',
      flow: poolName,
      endpoint: 'oauth_login_setup',
    });
    const body = parseJson(response);

    if (response.status !== 200 || !body?.accessToken || !body?.refreshToken) {
      throw new Error(`Setup login failed for ${poolName} user ${userNumber}. Status: ${response.status}`);
    }

    sessions.push({
      userNumber,
      code,
      email: body?.user?.email || `${code}@loadtest.example.test`,
      accessToken: body.accessToken,
      refreshToken: body.refreshToken,
    });
  }

  return sessions;
}

function seedOwnerStores(owners) {
  owners.forEach((session) => {
    const createResponse = createStore(
      session,
      buildStoreCreatePayload(session.userNumber),
      {
        phase: 'setup',
        flow: 'owner-seed',
        endpoint: 'store_seed',
        user_number: session.userNumber,
      },
      [201, 409]
    );

    if (createResponse.status === 201) {
      return;
    }

    const verifyResponse = getStoreProfile(
      session,
      {
        phase: 'setup',
        flow: 'owner-seed',
        endpoint: 'store_seed_verify',
        user_number: session.userNumber,
      },
      [200, 404]
    );

    if (createResponse.status === 409 && verifyResponse.status === 200) {
      return;
    }

    throw new Error(`Owner store seeding failed for user ${session.userNumber}. Status: ${createResponse.status}`);
  });
}

function buildUserCode(poolName, userNumber) {
  return buildMockCode('profile', poolName, userNumber);
}

function buildMockCode(prefix, scope, index) {
  const stablePrefix = `${prefix}-${scope}-${index}`;
  const runFragment = compactRunId(RUN_ID);
  const code = `${stablePrefix}-${runFragment}`;

  // MockOAuth2UserInfoResolver truncates codes to 80 characters before using
  // them as provider ids/emails. Keep generated codes within that limit and
  // put the per-user/per-scenario index before RUN_ID so long run ids do not
  // collapse multiple setup users into the same mock account.
  return code.length > 80 ? code.substring(0, 80) : code;
}

function compactRunId(runId) {
  const normalized = String(runId || 'profile-default');

  if (normalized.length <= 32) {
    return normalized;
  }

  return `${normalized.substring(0, 16)}-${normalized.substring(normalized.length - 15)}`;
}

function validatePositiveCount(count, label) {
  if (!Number.isInteger(count) || count < 1) {
    throw new Error(`${label} setup requires a positive integer count.`);
  }
}

