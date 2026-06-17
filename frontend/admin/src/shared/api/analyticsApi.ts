import { apiClient } from './client';
import type {
  AnalyticsMenuData,
  AnalyticsOperations,
  AnalyticsSummary,
  AnalyticsTrends,
  MenuInsights,
  RealtimeAnalytics,
} from '../types';

export const analyticsApi = {
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

  getTrends: async (days: number): Promise<AnalyticsTrends> => {
    const response = await apiClient.get<AnalyticsTrends>('/api/admin/analytics/trends', {
      params: { days },
    });
    return response.data;
  },

  getMenuInsights: async (days: number): Promise<MenuInsights> => {
    const response = await apiClient.get<MenuInsights>('/api/admin/analytics/menu-insights', {
      params: { days },
    });
    return response.data;
  },

  exportMenuInsightsCsv: async (days: number): Promise<Blob> => {
    const response = await apiClient.get('/api/admin/analytics/menu-insights/export', {
      params: { days },
      responseType: 'blob',
    });
    return response.data as Blob;
  },
};
