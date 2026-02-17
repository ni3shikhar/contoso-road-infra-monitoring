import api from './api';
import type { ApiResponse, PaginatedResponse, Alert, AlertSeverity, AlertStatus } from '@/types';

export interface AlertFilter {
  severity?: AlertSeverity[];
  status?: AlertStatus[];
  category?: string;
  assetId?: string;
  sensorId?: string;
  startDate?: string;
  endDate?: string;
  search?: string;
  page?: number;
  size?: number;
}

export interface AlertStats {
  total: number;
  active: number;
  acknowledged: number;
  resolved: number;
  bySeverity: Record<AlertSeverity, number>;
  byCategory: Record<string, number>;
}

export interface AlertRule {
  id: string;
  name: string;
  description?: string;
  condition: string;
  severity: AlertSeverity;
  enabled: boolean;
  notifyOnTrigger: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAlertRuleRequest {
  name: string;
  description?: string;
  condition: string;
  severity: AlertSeverity;
  enabled: boolean;
  notifyOnTrigger: boolean;
}

export interface AssignAlertRequest {
  userId: string;
}

export interface ResolveAlertRequest {
  notes?: string;
  resolution?: string;
}

export const alertService = {
  getAll: async (page = 0, size = 20): Promise<PaginatedResponse<Alert>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<Alert>>>('/v1/alerts', {
      params: { page, size },
    });
    return response.data.data;
  },

  getFiltered: async (filter: AlertFilter): Promise<PaginatedResponse<Alert>> => {
    const params = new URLSearchParams();
    if (filter.severity?.length) params.append('severity', filter.severity.join(','));
    if (filter.status?.length) params.append('status', filter.status.join(','));
    if (filter.category) params.append('category', filter.category);
    if (filter.assetId) params.append('assetId', filter.assetId);
    if (filter.sensorId) params.append('sensorId', filter.sensorId);
    if (filter.startDate) params.append('startDate', filter.startDate);
    if (filter.endDate) params.append('endDate', filter.endDate);
    if (filter.search) params.append('search', filter.search);
    if (filter.page !== undefined) params.append('page', String(filter.page));
    if (filter.size !== undefined) params.append('size', String(filter.size));

    const response = await api.get<ApiResponse<PaginatedResponse<Alert>>>(
      `/v1/alerts?${params.toString()}`
    );
    return response.data.data;
  },

  getById: async (id: string): Promise<Alert> => {
    const response = await api.get<ApiResponse<Alert>>(`/v1/alerts/${id}`);
    return response.data.data;
  },

  getActive: async (): Promise<Alert[]> => {
    const response = await api.get<ApiResponse<Alert[]>>('/v1/alerts/active');
    return response.data.data;
  },

  getUnacknowledged: async (): Promise<Alert[]> => {
    const response = await api.get<ApiResponse<Alert[]>>('/v1/alerts/unacknowledged');
    return response.data.data;
  },

  getRecent: async (limit = 10): Promise<Alert[]> => {
    const response = await api.get<ApiResponse<Alert[]>>(`/v1/alerts/recent?limit=${limit}`);
    return response.data.data;
  },

  getBySeverity: async (severity: AlertSeverity): Promise<Alert[]> => {
    const response = await api.get<ApiResponse<Alert[]>>(`/v1/alerts/severity/${severity}`);
    return response.data.data;
  },

  getByAsset: async (assetId: string, page = 0, size = 20): Promise<PaginatedResponse<Alert>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<Alert>>>(`/v1/alerts/asset/${assetId}`, {
      params: { page, size },
    });
    return response.data.data;
  },

  getBySensor: async (sensorId: string, page = 0, size = 20): Promise<PaginatedResponse<Alert>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<Alert>>>(`/v1/alerts/sensor/${sensorId}`, {
      params: { page, size },
    });
    return response.data.data;
  },

  // Actions
  acknowledge: async (id: string): Promise<Alert> => {
    const response = await api.post<ApiResponse<Alert>>(`/v1/alerts/${id}/acknowledge`);
    return response.data.data;
  },

  assign: async (id: string, data: AssignAlertRequest): Promise<Alert> => {
    const response = await api.post<ApiResponse<Alert>>(`/v1/alerts/${id}/assign`, data);
    return response.data.data;
  },

  resolve: async (id: string, data?: ResolveAlertRequest): Promise<Alert> => {
    const response = await api.post<ApiResponse<Alert>>(`/v1/alerts/${id}/resolve`, data);
    return response.data.data;
  },

  // Stats
  getStats: async (): Promise<AlertStats> => {
    const response = await api.get<ApiResponse<AlertStats>>('/v1/alerts/stats');
    return response.data.data;
  },

  getCount: async (): Promise<number> => {
    const response = await api.get<ApiResponse<{ count: number }>>('/v1/alerts/count');
    return response.data.data.count;
  },

  // Alert Rules
  getRules: async (): Promise<AlertRule[]> => {
    const response = await api.get<ApiResponse<AlertRule[]>>('/v1/alerts/rules');
    return response.data.data;
  },

  createRule: async (data: CreateAlertRuleRequest): Promise<AlertRule> => {
    const response = await api.post<ApiResponse<AlertRule>>('/v1/alerts/rules', data);
    return response.data.data;
  },

  updateRule: async (id: string, data: Partial<CreateAlertRuleRequest>): Promise<AlertRule> => {
    const response = await api.put<ApiResponse<AlertRule>>(`/v1/alerts/rules/${id}`, data);
    return response.data.data;
  },

  deleteRule: async (id: string): Promise<void> => {
    await api.delete(`/v1/alerts/rules/${id}`);
  },

  toggleRule: async (id: string, enabled: boolean): Promise<AlertRule> => {
    const response = await api.put<ApiResponse<AlertRule>>(`/v1/alerts/rules/${id}/toggle`, { enabled });
    return response.data.data;
  },

  // Categories
  getCategories: async (): Promise<string[]> => {
    const response = await api.get<ApiResponse<string[]>>('/v1/alerts/categories');
    return response.data.data;
  },
};
