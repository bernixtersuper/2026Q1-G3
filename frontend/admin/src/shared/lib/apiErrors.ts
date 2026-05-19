import { isAxiosError } from 'axios';
import { TimeoutError } from './withTimeout';

export function describeApiFailure(err: unknown, apiUrl: string): string {
  if (err instanceof TimeoutError) {
    return err.message;
  }
  if (isAxiosError(err)) {
    if (err.code === 'ECONNABORTED') {
      return `El servidor no respondió a tiempo. Revisá que el backend esté activo (${apiUrl}).`;
    }
    if (!err.response) {
      return `No se pudo conectar al API (${apiUrl}). ¿VITE_API_URL correcto y el ALB accesible?`;
    }
    const code = (err.response.data as { code?: string })?.code;
    if (err.response.status === 401 && code === 'INVALID_TOKEN') {
      return 'Token inválido o Cognito mal configurado en el backend (COGNITO_ISSUER_URL / CLIENT_ID).';
    }
  }
  return 'No se pudo completar la operación. Intentá de nuevo.';
}
