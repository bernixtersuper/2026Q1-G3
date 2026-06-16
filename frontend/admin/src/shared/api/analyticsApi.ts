import { apiClient } from './client';
import type {
  AnalyticsExportJob,
  AnalyticsMenuData,
  AnalyticsOperations,
  AnalyticsSummary,
  AnalyticsTrends,
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

  startExport: async (days: number): Promise<AnalyticsExportJob> => {
    const response = await apiClient.post<AnalyticsExportJob>('/api/admin/analytics/export', {
      days,
    });
    return response.data;
  },

  getExportStatus: async (jobId: string): Promise<AnalyticsExportJob> => {
    const response = await apiClient.get<AnalyticsExportJob>(
      `/api/admin/analytics/export/${jobId}`
    );
    return response.data;
  },
};
