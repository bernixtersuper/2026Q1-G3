import { apiClient } from './client';
import type {
  AnalyticsDashboard,
  AnalyticsMenuData,
  AnalyticsOperations,
  AnalyticsSummary,
  RealtimeAnalytics,
} from '../types';

export const analyticsApi = {
  getDashboard: async (): Promise<AnalyticsDashboard> => {
    const response = await apiClient.get<AnalyticsDashboard>('/api/admin/analytics');
    return response.data;
  },

  getSummary: async (): Promise<AnalyticsSummary> => {
    const response = await apiClient.get<AnalyticsSummary>('/api/admin/analytics/summary');
    return response.data;
  },

  getMenu: async (): Promise<AnalyticsMenuData> => {
    const response = await apiClient.get<AnalyticsMenuData>('/api/admin/analytics/menu');
    return response.data;
  },

  getOperations: async (): Promise<AnalyticsOperations> => {
    const response = await apiClient.get<AnalyticsOperations>('/api/admin/analytics/operations');
    return response.data;
  },

  getRealtime: async (): Promise<RealtimeAnalytics> => {
    const response = await apiClient.get<RealtimeAnalytics>('/api/admin/analytics/realtime');
    return response.data;
  },
};
