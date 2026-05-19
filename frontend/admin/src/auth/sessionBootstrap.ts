import { isAxiosError } from 'axios';
import type { NavigateFunction } from 'react-router-dom';
import { authApi } from '@/shared/api/authApi';
import { apiBaseUrl } from '@/shared/api/client';
import { describeApiFailure } from '@/shared/lib/apiErrors';
import { getCurrentTokens } from './cognito';
import type { SessionResponse } from '@/shared/types';

type EstablishSession = (accessToken: string, session: SessionResponse) => void;

export async function bootstrapBackendSession(
  establishSession: EstablishSession,
  navigate: NavigateFunction,
): Promise<{ ok: true } | { ok: false; error: string }> {
  const tokens = await getCurrentTokens();
  if (!tokens) {
    return { ok: false, error: 'No se pudo leer la sesión de Cognito. Intentá iniciar sesión de nuevo.' };
  }

  try {
    const session = await authApi.bootstrapSession(tokens.idToken);
    establishSession(tokens.accessToken, session);
    navigate('/admin', { replace: true });
    return { ok: true };
  } catch (apiError) {
    if (isAxiosError(apiError) && apiError.response?.status === 401 && apiError.response?.data?.code === 'UNKNOWN_USER') {
      navigate('/register', { replace: true });
      return { ok: true };
    }
    return { ok: false, error: describeApiFailure(apiError, apiBaseUrl) };
  }
}
