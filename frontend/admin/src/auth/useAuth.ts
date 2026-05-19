import { useState, useCallback, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';
import { authApi, type RegisterRequest } from '@/shared/api/authApi';
import type { SessionResponse } from '@/shared/types';
import { signOutEverywhere } from './cognito';

const STORAGE_KEY_TOKEN = 'md_token';
const STORAGE_KEY_TENANT_ID = 'md_tenant_id';
const STORAGE_KEY_RESTAURANT_NAME = 'md_restaurant_name';

interface AccessTokenClaims {
  exp: number;
}

interface AuthState {
  token: string | null;
  tenantId: string | null;
  restaurantName: string | null;
  isAuthenticated: boolean;
}

function isTokenLive(token: string): boolean {
  try {
    const { exp } = jwtDecode<AccessTokenClaims>(token);
    return exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

function readPersistedState(): AuthState {
  const token = localStorage.getItem(STORAGE_KEY_TOKEN);
  const tenantId = localStorage.getItem(STORAGE_KEY_TENANT_ID);
  const restaurantName = localStorage.getItem(STORAGE_KEY_RESTAURANT_NAME);

  if (token && tenantId && restaurantName && isTokenLive(token)) {
    return { token, tenantId, restaurantName, isAuthenticated: true };
  }

  if (token && !isTokenLive(token)) {
    localStorage.removeItem(STORAGE_KEY_TOKEN);
  }

  return { token: null, tenantId: null, restaurantName: null, isAuthenticated: false };
}

function persistSession(accessToken: string, session: SessionResponse) {
  localStorage.setItem(STORAGE_KEY_TOKEN, accessToken);
  localStorage.setItem(STORAGE_KEY_TENANT_ID, session.tenantId);
  localStorage.setItem(STORAGE_KEY_RESTAURANT_NAME, session.restaurantName);
}

function clearSession() {
  localStorage.removeItem(STORAGE_KEY_TOKEN);
  localStorage.removeItem(STORAGE_KEY_TENANT_ID);
  localStorage.removeItem(STORAGE_KEY_RESTAURANT_NAME);
}

export function useAuth() {
  const [authState, setAuthState] = useState<AuthState>(readPersistedState);

  const establishSession = useCallback((accessToken: string, session: SessionResponse) => {
    persistSession(accessToken, session);
    setAuthState({
      token: accessToken,
      tenantId: session.tenantId,
      restaurantName: session.restaurantName,
      isAuthenticated: true,
    });
  }, []);

  const register = useCallback(async (data: RegisterRequest, idToken: string, accessToken: string) => {
    const session = await authApi.register(data, idToken);
    establishSession(accessToken, session);
    return session;
  }, [establishSession]);

  const logout = useCallback(async () => {
    clearSession();
    setAuthState({
      token: null,
      tenantId: null,
      restaurantName: null,
      isAuthenticated: false,
    });

    try {
      await signOutEverywhere();
    } catch {
      // Swallow — we've already cleared local state.
    }
    window.location.assign('/login');
  }, []);

  useEffect(() => {
    const checkTokenExpiry = () => {
      const token = localStorage.getItem(STORAGE_KEY_TOKEN);
      if (token && !isTokenLive(token)) {
        logout();
      }
    };

    const interval = setInterval(checkTokenExpiry, 60000);
    return () => clearInterval(interval);
  }, [logout]);

  return {
    ...authState,
    establishSession,
    register,
    logout,
  };
}
