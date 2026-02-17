import api from './api';
import type { ApiResponse, PaginatedResponse, HealthStatus, Asset } from '@/types';

export interface HealthScore {
  assetId: string;
  assetName: string;
  assetType: string;
  overallScore: number;
  structuralScore?: number;
  functionalScore?: number;
  safetyScore?: number;
  environmentalScore?: number;
  status: HealthStatus;
  sensorCount: number;
  alertCount: number;
  lastUpdated: string;
  trend: 'improving' | 'stable' | 'declining';
}

export interface CorridorHealth {
  corridorId: string;
  corridorName: string;
  overallHealth: number;
  status: HealthStatus;
  segments: CorridorSegment[];
  totalLength: number;
  healthyLength: number;
  warningLength: number;
  criticalLength: number;
}

export interface CorridorSegment {
  segmentId: string;
  startKm: number;
  endKm: number;
  status: HealthStatus;
  healthScore: number;
  assetIds: string[];
}

export interface MonitoringThreshold {
  id: string;
  metricName: string;
  description?: string;
  warningThreshold: number;
  criticalThreshold: number;
  unit: string;
  enabled: boolean;
  assetType?: string;
  sensorType?: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateThresholdRequest {
  warningThreshold: number;
  criticalThreshold: number;
  enabled?: boolean;
}

export interface HealthTrendPoint {
  timestamp: string;
  score: number;
  status: HealthStatus;
}

export interface AssetHealthDetail {
  asset: Asset;
  healthScore: HealthScore;
  sensorReadings: {
    sensorId: string;
    sensorName: string;
    sensorType: string;
    currentValue: number;
    unit: string;
    status: HealthStatus;
    threshold: {
      warning: number;
      critical: number;
    };
  }[];
  recentAlerts: number;
  healthTrend: HealthTrendPoint[];
}

export const monitoringService = {
  // Corridor Health
  getCorridorHealth: async (): Promise<CorridorHealth> => {
    const response = await api.get<ApiResponse<CorridorHealth>>('/v1/monitoring/corridor');
    return response.data.data;
  },

  getCorridorSegments: async (): Promise<CorridorSegment[]> => {
    const response = await api.get<ApiResponse<CorridorSegment[]>>('/v1/monitoring/corridor/segments');
    return response.data.data;
  },

  // Asset Health
  getHealthScores: async (): Promise<HealthScore[]> => {
    const response = await api.get<ApiResponse<HealthScore[]>>('/v1/monitoring/health');
    return response.data.data;
  },

  getAssetHealthScore: async (assetId: string): Promise<HealthScore> => {
    const response = await api.get<ApiResponse<HealthScore>>(`/v1/monitoring/health/${assetId}`);
    return response.data.data;
  },

  getAssetHealthDetail: async (assetId: string): Promise<AssetHealthDetail> => {
    const response = await api.get<ApiResponse<AssetHealthDetail>>(`/v1/monitoring/health/${assetId}/detail`);
    return response.data.data;
  },

  getHealthTrend: async (
    assetId: string, 
    startDate: string, 
    endDate: string
  ): Promise<HealthTrendPoint[]> => {
    const response = await api.get<ApiResponse<HealthTrendPoint[]>>(
      `/v1/monitoring/health/${assetId}/trend`,
      { params: { startDate, endDate } }
    );
    return response.data.data;
  },

  getMultipleHealthTrends: async (
    assetIds: string[], 
    startDate: string, 
    endDate: string
  ): Promise<Record<string, HealthTrendPoint[]>> => {
    const response = await api.post<ApiResponse<Record<string, HealthTrendPoint[]>>>(
      '/v1/monitoring/health/trends',
      { assetIds, startDate, endDate }
    );
    return response.data.data;
  },

  // Thresholds
  getThresholds: async (): Promise<MonitoringThreshold[]> => {
    const response = await api.get<ApiResponse<MonitoringThreshold[]>>('/v1/monitoring/thresholds');
    return response.data.data;
  },

  getThreshold: async (id: string): Promise<MonitoringThreshold> => {
    const response = await api.get<ApiResponse<MonitoringThreshold>>(`/v1/monitoring/thresholds/${id}`);
    return response.data.data;
  },

  updateThreshold: async (id: string, data: UpdateThresholdRequest): Promise<MonitoringThreshold> => {
    const response = await api.put<ApiResponse<MonitoringThreshold>>(
      `/v1/monitoring/thresholds/${id}`,
      data
    );
    return response.data.data;
  },

  createThreshold: async (data: Omit<MonitoringThreshold, 'id' | 'createdAt' | 'updatedAt'>): Promise<MonitoringThreshold> => {
    const response = await api.post<ApiResponse<MonitoringThreshold>>(
      '/v1/monitoring/thresholds',
      data
    );
    return response.data.data;
  },

  deleteThreshold: async (id: string): Promise<void> => {
    await api.delete(`/v1/monitoring/thresholds/${id}`);
  },

  // Dashboard Data
  getDashboardSummary: async (): Promise<{
    totalAssets: number;
    healthyAssets: number;
    warningAssets: number;
    criticalAssets: number;
    totalSensors: number;
    activeSensors: number;
    openAlerts: number;
    constructionProgress: number;
    healthIndex: number;
  }> => {
    const response = await api.get<ApiResponse<{
      totalAssets: number;
      healthyAssets: number;
      warningAssets: number;
      criticalAssets: number;
      totalSensors: number;
      activeSensors: number;
      openAlerts: number;
      constructionProgress: number;
      healthIndex: number;
    }>>('/v1/monitoring/dashboard/summary');
    return response.data.data;
  },

  // Real-time data
  getLiveReadings: async (assetId: string): Promise<{
    sensorId: string;
    sensorName: string;
    value: number;
    unit: string;
    timestamp: string;
    status: HealthStatus;
  }[]> => {
    const response = await api.get<ApiResponse<{
      sensorId: string;
      sensorName: string;
      value: number;
      unit: string;
      timestamp: string;
      status: HealthStatus;
    }[]>>(`/v1/monitoring/live/${assetId}`);
    return response.data.data;
  },
};
