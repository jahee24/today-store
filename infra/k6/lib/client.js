import http from 'k6/http';
import { BASE_URL, RUN_ID, buildTags } from './config.js';

export function loginWithMock(code, tags, expectedStatuses = [200]) {
  return http.post(
    `${BASE_URL}/api/v1/auth/oauth/login`,
    JSON.stringify({ provider: 'mock', code }),
    buildRequestParams({
      tags,
      expectedStatuses,
      isJson: true,
    })
  );
}

export function getUserProfile(session, tags, expectedStatuses = [200]) {
  return http.get(
    `${BASE_URL}/api/v1/users/me`,
    buildRequestParams({
      session,
      tags,
      expectedStatuses,
    })
  );
}

export function patchUserProfile(session, payload, tags, expectedStatuses = [200]) {
  return http.patch(
    `${BASE_URL}/api/v1/users/me`,
    JSON.stringify(payload),
    buildRequestParams({
      session,
      tags,
      expectedStatuses,
      isJson: true,
    })
  );
}

export function createStore(session, payload, tags, expectedStatuses = [201]) {
  return http.post(
    `${BASE_URL}/api/v1/stores`,
    JSON.stringify(payload),
    buildRequestParams({
      session,
      tags,
      expectedStatuses,
      isJson: true,
    })
  );
}

export function getStoreProfile(session, tags, expectedStatuses = [200]) {
  return http.get(
    `${BASE_URL}/api/v1/stores/me`,
    buildRequestParams({
      session,
      tags,
      expectedStatuses,
    })
  );
}

export function patchStoreProfile(session, payload, tags, expectedStatuses = [200]) {
  return http.patch(
    `${BASE_URL}/api/v1/stores/me`,
    JSON.stringify(payload),
    buildRequestParams({
      session,
      tags,
      expectedStatuses,
      isJson: true,
    })
  );
}

export function refreshSession(refreshToken, tags, expectedStatuses = [200]) {
  return http.post(
    `${BASE_URL}/api/v1/auth/refresh`,
    JSON.stringify({ refreshToken }),
    buildRequestParams({
      tags,
      expectedStatuses,
      isJson: true,
    })
  );
}

export function logoutSession(session, tags, expectedStatuses = [204]) {
  return http.post(
    `${BASE_URL}/api/v1/auth/logout`,
    null,
    buildRequestParams({
      session,
      tags,
      expectedStatuses,
    })
  );
}

export function deleteMyAccount(session, tags, expectedStatuses = [204]) {
  return http.del(
    `${BASE_URL}/api/v1/auth/me`,
    null,
    buildRequestParams({
      session,
      tags,
      expectedStatuses,
    })
  );
}

export function parseJson(response) {
  try {
    return response.json();
  } catch (_error) {
    return null;
  }
}

export function buildRequestParams({
  session = null,
  tags = {},
  expectedStatuses = [200],
  isJson = false,
  authorization = null,
  extraHeaders = {},
} = {}) {
  const headers = {
    Accept: 'application/json',
    'X-Loadtest-Run-Id': RUN_ID,
    ...extraHeaders,
  };

  if (authorization) {
    headers.Authorization = authorization;
  } else if (session) {
    headers.Authorization = `Bearer ${session.accessToken}`;
  }

  if (isJson) {
    headers['Content-Type'] = 'application/json';
  }

  return {
    headers,
    tags: buildTags(tags),
    responseCallback: http.expectedStatuses(...expectedStatuses),
  };
}

