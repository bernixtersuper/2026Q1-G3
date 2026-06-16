const LEGACY_API_MEDIA_PREFIX = '/api/media/';
const MENUS_KEY_PREFIX = 'menus/';

/** Extrae clave S3 desde URL prefirmada, legacy /api/media/... o clave menus/... */
export function extractStorageKey(imageUrl: string): string | null {
  let value = imageUrl.trim();
  if (!value) {
    return null;
  }
  if (value.startsWith(LEGACY_API_MEDIA_PREFIX)) {
    value = value.slice(LEGACY_API_MEDIA_PREFIX.length);
  }
  const idx = value.indexOf(MENUS_KEY_PREFIX);
  if (idx < 0) {
    return null;
  }
  let key = value.substring(idx);
  const q = key.indexOf('?');
  if (q >= 0) {
    key = key.substring(0, q);
  }
  return key.startsWith(MENUS_KEY_PREFIX) ? key : null;
}

/** URL para <img src>: presigned URL devuelta por el API. */
export function imageSrc(imageUrl: string | undefined | null): string {
  if (!imageUrl) {
    return '';
  }
  const trimmed = imageUrl.trim();
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return trimmed;
  }
  return '';
}
