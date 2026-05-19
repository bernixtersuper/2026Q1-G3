import { apiClient } from './client';
import type { AnalyticsDashboard, RealtimeAnalytics } from '../types';

export const analyticsApi = {
  getDashboard: async (): Promise<AnalyticsDashboard> => {
    const response = await apiClient.get<AnalyticsDashboard>('/api/admin/analytics');
    return response.data;
  },

  getRealtime: async (): Promise<RealtimeAnalytics> => {
    const response = await apiClient.get<RealtimeAnalytics>('/api/admin/analytics/realtime');
    return response.data;
  },
};
