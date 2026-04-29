import { Counter } from 'k6/metrics';
import { buildTags } from './config.js';

export const profileUnexpectedStatusCount = new Counter('profile_unexpected_status_count');
export const profileServerErrorCount = new Counter('profile_server_error_count');
export const duplicateCreateCreatedCount = new Counter('profile_duplicate_create_created_count');
export const duplicateCreateConflictCount = new Counter('profile_duplicate_create_conflict_count');
export const duplicateCreateUnexpectedStatusCount = new Counter('profile_duplicate_create_unexpected_status_count');
export const duplicateCreateServerErrorCount = new Counter('profile_duplicate_create_server_error_count');

export function recordProfileResponse(response, expectedStatuses, tags) {
  const normalizedTags = buildTags(tags);
  const isExpected = expectedStatuses.indexOf(response.status) !== -1;

  if (!isExpected) {
    profileUnexpectedStatusCount.add(1, {
      ...normalizedTags,
      status: String(response.status),
    });
  }

  if (response.status >= 500) {
    profileServerErrorCount.add(1, normalizedTags);
  }

  return isExpected;
}

export function recordDuplicateCreateResponse(response, tags) {
  const normalizedTags = buildTags(tags);

  if (response.status === 201) {
    duplicateCreateCreatedCount.add(1, normalizedTags);
    return true;
  }

  if (response.status === 409) {
    duplicateCreateConflictCount.add(1, normalizedTags);
    return true;
  }

  if (response.status >= 500) {
    duplicateCreateServerErrorCount.add(1, normalizedTags);
  }

  duplicateCreateUnexpectedStatusCount.add(1, {
    ...normalizedTags,
    status: String(response.status),
  });
  return false;
}

export function recordStatusCounters(
  response,
  tags,
  statusCounters = {},
  unexpectedCounter = null,
  serverErrorCounter = null
) {
  const normalizedTags = buildTags(tags);
  const statusCounter = statusCounters[response.status];

  if (statusCounter) {
    statusCounter.add(1, normalizedTags);
  } else if (unexpectedCounter) {
    unexpectedCounter.add(1, {
      ...normalizedTags,
      status: String(response.status),
    });
  }

  if (response.status >= 500 && serverErrorCounter) {
    serverErrorCounter.add(1, normalizedTags);
  }

  return Boolean(statusCounter);
}

