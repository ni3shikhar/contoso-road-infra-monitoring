import api from './api';
import type { ApiResponse, PaginatedResponse, Kpi } from '@/types';

export type KpiCategory = 'CONSTRUCTION' | 'HEALTH' | 'SENSOR' | 'ALERT' | 'SAFETY' | 'ALL';
export type TimeRange = '7d' | '30d' | '90d' | '1y';

export interface KpiHistoryPoint {
  timestamp: string;
  value: number;
}

export interface KpiWithHistory extends Kpi {
  history: KpiHistoryPoint[];
}

export interface WeeklyReport {
  weekStart: string;
  weekEnd: string;
  kpis: {
    metricName: string;
    displayName: string;
    currentValue: number;
    previousValue: number;
    change: number;
    changePercent: number;
    trend: 'up' | 'down' | 'stable';
    onTarget: boolean;
  }[];
  summary: string;
  highlights: string[];
  concerns: string[];
}

export interface DashboardKpis {
  constructionProgress: Kpi;
  assetHealthIndex: Kpi;
  activeSensors: Kpi;
  openAlerts: Kpi;
}

export const analyticsService = {
  // KPI Retrieval
  getAllKpis: async (): Promise<Kpi[]> => {
    const response = await api.get<ApiResponse<Kpi[]>>('/v1/analytics/kpis');
    return response.data.data;
  },

  getKpiById: async (id: string): Promise<Kpi> => {
    const response = await api.get<ApiResponse<Kpi>>(`/v1/analytics/kpis/${id}`);
    return response.data.data;
  },

  getLatestKpi: async (metricName: string): Promise<Kpi> => {
    const response = await api.get<ApiResponse<Kpi>>(`/v1/analytics/kpis/latest/${metricName}`);
    return response.data.data;
  },

  getKpisByCategory: async (category: KpiCategory): Promise<Kpi[]> => {
    if (category === 'ALL') {
      return analyticsService.getAllKpis();
    }
    const response = await api.get<ApiResponse<Kpi[]>>(`/v1/analytics/kpis/category/${category}`);
    return response.data.data;
  },

  getKpisByAsset: async (assetId: string, page = 0, size = 20): Promise<PaginatedResponse<Kpi>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<Kpi>>>(`/v1/analytics/kpis/asset/${assetId}`, {
      params: { page, size },
    });
    return response.data.data;
  },

  getOffTargetKpis: async (): Promise<Kpi[]> => {
    const response = await api.get<ApiResponse<Kpi[]>>('/v1/analytics/kpis/off-target');
    return response.data.data;
  },

  // KPI with History
  getKpiWithHistory: async (metricName: string, timeRange: TimeRange): Promise<KpiWithHistory> => {
    const response = await api.get<ApiResponse<KpiWithHistory>>(
      `/v1/analytics/kpis/${metricName}/history`,
      { params: { range: timeRange } }
    );
    return response.data.data;
  },

  getKpiHistory: async (
    metricName: string, 
    startDate: string, 
    endDate: string
  ): Promise<KpiHistoryPoint[]> => {
    const response = await api.get<ApiResponse<KpiHistoryPoint[]>>(
      `/v1/analytics/kpis/${metricName}/history`,
      { params: { startDate, endDate } }
    );
    return response.data.data;
  },

  // Dashboard KPIs
  getDashboardKpis: async (): Promise<DashboardKpis> => {
    const response = await api.get<ApiResponse<DashboardKpis>>('/v1/analytics/dashboard');
    return response.data.data;
  },

  // Weekly Report
  getWeeklyReport: async (weekOffset = 0): Promise<WeeklyReport> => {
    const response = await api.get<ApiResponse<WeeklyReport>>(
      '/v1/analytics/reports/weekly',
      { params: { weekOffset } }
    );
    return response.data.data;
  },

  // Metadata
  getAllMetricNames: async (): Promise<string[]> => {
    const response = await api.get<ApiResponse<string[]>>('/v1/analytics/metrics');
    return response.data.data;
  },

  getAllCategories: async (): Promise<string[]> => {
    const response = await api.get<ApiResponse<string[]>>('/v1/analytics/categories');
    return response.data.data;
  },

  // Refresh
  refreshKpis: async (): Promise<void> => {
    await api.post('/v1/analytics/refresh');
  },

  refreshKpiByMetric: async (metricName: string): Promise<Kpi> => {
    const response = await api.post<ApiResponse<Kpi>>(`/v1/analytics/refresh/${metricName}`);
    return response.data.data;
  },

  // Export
  exportKpis: async (format: 'csv' | 'pdf' = 'csv'): Promise<Blob> => {
    const response = await api.get(`/v1/analytics/export?format=${format}`, {
      responseType: 'blob',
    });
    return response.data;
  },

  exportKpiReport: async (timeRange: TimeRange, format: 'csv' | 'pdf' = 'pdf'): Promise<Blob> => {
    const response = await api.get(
      `/v1/analytics/export/report?range=${timeRange}&format=${format}`,
      { responseType: 'blob' }
    );
    return response.data;
  },
};
