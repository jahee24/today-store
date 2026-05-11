const rawBaseUrl = stringEnv('BASE_URL', '');

if (!rawBaseUrl) {
  throw new Error(
    'BASE_URL is required. Example: k6 run -e BASE_URL=https://<masked-target-host> -e RUN_ID=prod-concurrency-001 <script>'
  );
}

export const BASE_URL = rawBaseUrl.replace(/\/$/, '');
export const RUN_ID = stringEnv('RUN_ID', 'prod-concurrency-default');
export const ENABLE_WARMUP = boolEnv('ENABLE_WARMUP', true);
export const DEFAULT_PROFILE_USER_COUNT = 300;
export const SUMMARY_TREND_STATS = ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'];

export function buildTags(tags) {
  const normalized = {};

  Object.keys(tags || {}).forEach((key) => {
    normalized[key] = String(tags[key]);
  });

  return normalized;
}

export function buildWarmupRate(mainRate) {
  return Math.max(1, Math.ceil(mainRate * 0.2));
}

export function durationToSeconds(duration) {
  const match = /^(\d+)([smh])$/.exec(duration);
  if (!match) {
    throw new Error(`Unsupported duration format: ${duration}`);
  }

  const value = Number(match[1]);
  const unit = match[2];

  if (unit === 's') {
    return value;
  }

  if (unit === 'm') {
    return value * 60;
  }

  return value * 3600;
}

export function stringEnv(name, defaultValue) {
  const raw = __ENV[name];
  if (raw === undefined || raw === '') {
    return defaultValue;
  }

  return String(raw);
}

export function numberEnv(name, defaultValue) {
  const raw = __ENV[name];
  if (raw === undefined || raw === '') {
    return defaultValue;
  }

  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : defaultValue;
}

export function boolEnv(name, defaultValue) {
  const raw = __ENV[name];
  if (raw === undefined) {
    return defaultValue;
  }

  return ['true', '1', 'yes', 'on'].indexOf(String(raw).toLowerCase()) !== -1;
}

