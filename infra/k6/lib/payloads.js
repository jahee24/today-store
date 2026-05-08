const PROFILE_STYLES = ['PLAIN', 'CLEAN', 'FRIENDLY', 'MEME'];
const BUSINESS_TYPES = ['CAFE', 'RESTAURANT', 'BAKERY', 'FLOWER', 'SALON', 'BOOKS'];

export function buildUserPatchPayload(seed) {
  return {
    name: `Profile User ${seed + 1}`,
  };
}

export function buildStoreCreatePayload(seed) {
  return {
    storeName: `Profile Store ${seed + 1}`,
    businessType: BUSINESS_TYPES[seed % BUSINESS_TYPES.length],
    address: `Seoul Test-ro ${100 + (seed % 80)}-${Math.floor(seed / 80) + 1}`,
    latitude: buildLatitude(seed),
    longitude: buildLongitude(seed),
    preferredStyle: PROFILE_STYLES[seed % PROFILE_STYLES.length],
    sns: {
      instagram: `@profile_${seed + 1}`,
      naver: `https://example.test/naver/profile/${seed + 1}`,
      karrot: `https://example.test/karrot/profile/${seed + 1}`,
    },
  };
}

export function buildStorePatchPayload(seed) {
  return {
    storeName: `Profile Store Update ${seed + 1}`,
    preferredStyle: PROFILE_STYLES[(seed + 1) % PROFILE_STYLES.length],
    snsInstagram: `@profile_patch_${seed + 1}`,
    address: `Seoul Profile-gil ${200 + (seed % 60)}-${Math.floor(seed / 60) + 1}`,
    latitude: buildLatitude(seed + 1000),
    longitude: buildLongitude(seed + 1000),
  };
}

function buildLatitude(seed) {
  return Number((37.45 + ((seed % 500) * 0.0001)).toFixed(8));
}

function buildLongitude(seed) {
  return Number((127.01 + ((seed % 500) * 0.0001)).toFixed(8));
}

