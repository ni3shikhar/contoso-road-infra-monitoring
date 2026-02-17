import api from './api';
import type { ApiResponse, PaginatedResponse, AuditLog, AuditLogFilter } from '@/types';

export const auditService = {
  // List audit logs with filters
  listAuditLogs: async (filter: AuditLogFilter = {}): Promise<PaginatedResponse<AuditLog>> => {
    const params = new URLSearchParams();
    if (filter.userId) params.append('userId', filter.userId);
    if (filter.action) params.append('action', filter.action);
    if (filter.resourceType) params.append('resourceType', filter.resourceType);
    if (filter.startDate) params.append('startDate', filter.startDate);
    if (filter.endDate) params.append('endDate', filter.endDate);
    if (filter.page !== undefined) params.append('page', String(filter.page));
    if (filter.size !== undefined) params.append('size', String(filter.size));

    const response = await api.get<ApiResponse<PaginatedResponse<AuditLog>>>(
      `/v1/audit?${params.toString()}`
    );
    return response.data.data;
  },

  // Get audit log by ID
  getAuditLog: async (auditId: string): Promise<AuditLog> => {
    const response = await api.get<ApiResponse<AuditLog>>(`/v1/audit/${auditId}`);
    return response.data.data;
  },

  // Get audit logs for a specific user
  getUserAuditLogs: async (
    userId: string, 
    page = 0, 
    size = 20
  ): Promise<PaginatedResponse<AuditLog>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<AuditLog>>>(
      `/v1/audit/user/${userId}?page=${page}&size=${size}`
    );
    return response.data.data;
  },

  // Get audit logs for a specific resource
  getResourceAuditLogs: async (
    resourceType: string,
    resourceId: string,
    page = 0,
    size = 20
  ): Promise<PaginatedResponse<AuditLog>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<AuditLog>>>(
      `/v1/audit/resource/${resourceType}/${resourceId}?page=${page}&size=${size}`
    );
    return response.data.data;
  },

  // Export audit logs as CSV
  exportAuditLogs: async (filter: AuditLogFilter = {}): Promise<Blob> => {
    const params = new URLSearchParams();
    if (filter.userId) params.append('userId', filter.userId);
    if (filter.action) params.append('action', filter.action);
    if (filter.resourceType) params.append('resourceType', filter.resourceType);
    if (filter.startDate) params.append('startDate', filter.startDate);
    if (filter.endDate) params.append('endDate', filter.endDate);

    const response = await api.get(`/v1/audit/export?${params.toString()}`, {
      responseType: 'blob',
    });
    return response.data;
  },

  // Get distinct action types for filtering
  getActionTypes: async (): Promise<string[]> => {
    const response = await api.get<ApiResponse<string[]>>('/v1/audit/actions');
    return response.data.data;
  },

  // Get distinct resource types for filtering
  getResourceTypes: async (): Promise<string[]> => {
    const response = await api.get<ApiResponse<string[]>>('/v1/audit/resource-types');
    return response.data.data;
  },
};
