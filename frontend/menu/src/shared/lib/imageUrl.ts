const API_BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

const MENUS_KEY_PREFIX = 'menus/';
const API_MEDIA_PREFIX = '/api/media/';

/** Extrae clave S3 desde URL legacy (S3/MinIO), ruta /api/media/... o clave menus/... */
export function extractStorageKey(imageUrl: string): string | null {
  let value = imageUrl.trim();
  if (!value) {
    return null;
  }
  if (value.startsWith(API_MEDIA_PREFIX)) {
    value = value.slice(API_MEDIA_PREFIX.length);
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

/** Valor estable para guardar en el formulario (ruta proxy relativa). */
export function normalizeImageUrlForForm(imageUrl: string): string {
  const key = extractStorageKey(imageUrl);
  if (key) {
    return `${API_MEDIA_PREFIX}${key}`;
  }
  return imageUrl.trim();
}

/** URL absoluta para <img src>: siempre vía proxy del backend si hay clave menus/... */
export function imageSrc(imageUrl: string | undefined | null): string {
  if (!imageUrl) {
    return '';
  }
  const key = extractStorageKey(imageUrl);
  if (key) {
    return `${API_BASE}${API_MEDIA_PREFIX}${key}`;
  }
  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl;
  }
  return imageUrl.startsWith('/') ? `${API_BASE}${imageUrl}` : `${API_BASE}/${imageUrl}`;
}
