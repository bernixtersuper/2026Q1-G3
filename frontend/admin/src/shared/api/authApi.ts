import { apiClient } from './client';
import type { SessionResponse } from '../types';

export interface RegisterRequest {
  restaurantName: string;
  slug: string;
}

function bearerHeader(idToken: string) {
  return { headers: { Authorization: `Bearer ${idToken}` } };
}

export const authApi = {
  register: async (data: RegisterRequest, idToken: string): Promise<SessionResponse> => {
    const response = await apiClient.post<SessionResponse>('/api/auth/register', data, bearerHeader(idToken));
    return response.data;
  },

  bootstrapSession: async (idToken: string): Promise<SessionResponse> => {
    const response = await apiClient.post<SessionResponse>('/api/auth/session', null, bearerHeader(idToken));
    return response.data;
  },
};
