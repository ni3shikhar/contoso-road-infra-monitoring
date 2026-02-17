import api from './api';
import type { ApiResponse, PaginatedResponse, Sensor, SensorReading, SensorType } from '@/types';

export type SensorStatus = 'ACTIVE' | 'INACTIVE' | 'FAULTY' | 'MAINTENANCE' | 'DECOMMISSIONED';

export interface CreateSensorRequest {
  sensorCode: string;
  name: string;
  type: SensorType;
  assetId: string;
  unit: string;
  minThreshold?: number;
  maxThreshold?: number;
  latitude?: number;
  longitude?: number;
  manufacturer?: string;
  model?: string;
}

export interface UpdateSensorRequest extends Partial<CreateSensorRequest> {
  status?: SensorStatus;
}

export interface SensorFilter {
  type?: SensorType;
  status?: SensorStatus;
  assetId?: string;
  search?: string;
  page?: number;
  size?: number;
}

export interface SensorStats {
  total: number;
  active: number;
  inactive: number;
  faulty: number;
  maintenance: number;
  calibrationDue: number;
  byType: Record<string, number>;
}

// Backend response structure (different from frontend Sensor type)
interface BackendSensorResponse {
  id: string;
  sensorCode: string;
  sensorType: string;
  manufacturer?: string;
  model?: string;
  installationDate?: string;
  lastCalibrationDate?: string;
  calibrationIntervalDays?: number;
  calibrationDue?: boolean;
  status: string;
  latitude?: number;
  longitude?: number;
  elevation?: number;
  assetId?: string;
  assetType?: string;
  locationDescription?: string;
  batteryLevel?: number;
  signalStrength?: number;
  firmwareVersion?: string;
  minThreshold?: number;
  maxThreshold?: number;
  unit?: string;
  currentValue?: number;
  lastDataReceivedAt?: string;
  createdBy?: string;
  updatedBy?: string;
  createdAt: string;
  updatedAt: string;
}

// Transform backend response to frontend Sensor type
function transformSensor(backend: BackendSensorResponse): Sensor {
  const statusLower = (backend.status || '').toLowerCase();
  // Backend SensorStatus uses @JsonValue returning "Active", "Inactive", etc.
  // Also handle uppercase enum names just in case
  const isActive = statusLower === 'active' || statusLower === 'online';
  
  return {
    id: backend.id,
    name: backend.sensorCode || backend.id,
    type: backend.sensorType || '',
    assetId: backend.assetId || '',
    // Use locationDescription first, fall back to assetType (e.g., "Bridge", "Tunnel")
    assetName: backend.locationDescription || backend.assetType || undefined,
    unit: backend.unit || '',
    status: backend.status || 'Offline',
    batteryLevel: backend.batteryLevel,
    latitude: backend.latitude,
    longitude: backend.longitude,
    minThreshold: backend.minThreshold,
    maxThreshold: backend.maxThreshold,
    active: isActive,
    lastReading: backend.currentValue !== undefined && backend.currentValue !== null
      ? `${backend.currentValue} ${backend.unit || ''}`
      : undefined,
    lastReadingValue: backend.currentValue !== undefined && backend.currentValue !== null
      ? {
          id: '',
          sensorId: backend.id,
          value: backend.currentValue,
          unit: backend.unit || '',
          timestamp: backend.lastDataReceivedAt || backend.updatedAt,
        }
      : undefined,
    installationDate: backend.installationDate,
    calibrationDate: backend.lastCalibrationDate,
    firmwareVersion: backend.firmwareVersion,
    model: backend.model,
    manufacturer: backend.manufacturer,
    createdAt: backend.createdAt,
    updatedAt: backend.updatedAt,
  };
}

function transformPaginatedSensors(response: PaginatedResponse<BackendSensorResponse>): PaginatedResponse<Sensor> {
  return {
    ...response,
    content: response.content.map(transformSensor),
  };
}

export const sensorService = {
  getAll: async (page = 0, size = 20): Promise<PaginatedResponse<Sensor>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<BackendSensorResponse>>>('/v1/sensors', {
      params: { page, size },
    });
    return transformPaginatedSensors(response.data.data);
  },

  getFiltered: async (filter: SensorFilter): Promise<PaginatedResponse<Sensor>> => {
    const params = new URLSearchParams();
    if (filter.type) params.append('type', filter.type);
    if (filter.status) params.append('status', filter.status);
    if (filter.assetId) params.append('assetId', filter.assetId);
    if (filter.search) params.append('search', filter.search);
    if (filter.page !== undefined) params.append('page', String(filter.page));
    if (filter.size !== undefined) params.append('size', String(filter.size));

    const response = await api.get<ApiResponse<PaginatedResponse<BackendSensorResponse>>>(
      `/v1/sensors?${params.toString()}`
    );
    return transformPaginatedSensors(response.data.data);
  },

  getById: async (id: string): Promise<Sensor> => {
    const response = await api.get<ApiResponse<BackendSensorResponse>>(`/v1/sensors/${id}`);
    return transformSensor(response.data.data);
  },

  getByCode: async (code: string): Promise<Sensor> => {
    const response = await api.get<ApiResponse<BackendSensorResponse>>(`/v1/sensors/code/${code}`);
    return transformSensor(response.data.data);
  },

  create: async (data: CreateSensorRequest): Promise<Sensor> => {
    const response = await api.post<ApiResponse<BackendSensorResponse>>('/v1/sensors', data);
    return transformSensor(response.data.data);
  },

  update: async (id: string, data: UpdateSensorRequest): Promise<Sensor> => {
    const response = await api.put<ApiResponse<BackendSensorResponse>>(`/v1/sensors/${id}`, data);
    return transformSensor(response.data.data);
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/v1/sensors/${id}`);
  },

  // Status changes
  changeStatus: async (id: string, status: SensorStatus): Promise<Sensor> => {
    const response = await api.put<ApiResponse<BackendSensorResponse>>(`/v1/sensors/${id}/status`, { status });
    return transformSensor(response.data.data);
  },

  activate: async (id: string): Promise<Sensor> => {
    const response = await api.post<ApiResponse<BackendSensorResponse>>(`/v1/sensors/${id}/activate`);
    return transformSensor(response.data.data);
  },

  deactivate: async (id: string): Promise<Sensor> => {
    const response = await api.post<ApiResponse<BackendSensorResponse>>(`/v1/sensors/${id}/deactivate`);
    return transformSensor(response.data.data);
  },

  decommission: async (id: string): Promise<Sensor> => {
    const response = await api.post<ApiResponse<BackendSensorResponse>>(`/v1/sensors/${id}/decommission`);
    return transformSensor(response.data.data);
  },

  // Calibration
  recordCalibration: async (id: string): Promise<Sensor> => {
    const response = await api.post<ApiResponse<BackendSensorResponse>>(`/v1/sensors/${id}/calibrate`);
    return transformSensor(response.data.data);
  },

  // Thresholds
  updateThresholds: async (
    id: string, 
    minThreshold: number, 
    maxThreshold: number
  ): Promise<Sensor> => {
    const response = await api.put<ApiResponse<BackendSensorResponse>>(`/v1/sensors/${id}/thresholds`, {
      minThreshold,
      maxThreshold,
    });
    return transformSensor(response.data.data);
  },

  getByAsset: async (assetId: string): Promise<Sensor[]> => {
    const response = await api.get<ApiResponse<BackendSensorResponse[]>>(`/v1/sensors/asset/${assetId}`);
    return response.data.data.map(transformSensor);
  },

  getByType: async (type: string, page = 0, size = 20): Promise<PaginatedResponse<Sensor>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<BackendSensorResponse>>>(`/v1/sensors/type/${type}`, {
      params: { page, size },
    });
    return transformPaginatedSensors(response.data.data);
  },

  getByStatus: async (status: SensorStatus, page = 0, size = 20): Promise<PaginatedResponse<Sensor>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<BackendSensorResponse>>>(`/v1/sensors/status/${status}`, {
      params: { page, size },
    });
    return transformPaginatedSensors(response.data.data);
  },

  // Readings
  getReadings: async (
    sensorId: string, 
    page = 0, 
    size = 100,
    startTime?: string,
    endTime?: string
  ): Promise<PaginatedResponse<SensorReading>> => {
    const params: Record<string, string | number> = { page, size };
    if (startTime) params.startTime = startTime;
    if (endTime) params.endTime = endTime;

    const response = await api.get<ApiResponse<PaginatedResponse<SensorReading>>>(
      `/v1/sensors/${sensorId}/readings`,
      { params }
    );
    return response.data.data;
  },

  getLatestReading: async (sensorId: string): Promise<SensorReading | null> => {
    const response = await api.get<ApiResponse<SensorReading>>(`/v1/sensors/${sensorId}/readings/latest`);
    return response.data.data;
  },

  submitReading: async (sensorId: string, value: number): Promise<SensorReading> => {
    const response = await api.post<ApiResponse<SensorReading>>(`/v1/sensors/${sensorId}/readings`, { value });
    return response.data.data;
  },

  // Stats
  getStats: async (): Promise<SensorStats> => {
    const response = await api.get<ApiResponse<SensorStats>>('/v1/sensors/stats');
    return response.data.data;
  },

  // Bulk operations
  bulkChangeStatus: async (ids: string[], status: SensorStatus): Promise<Sensor[]> => {
    const response = await api.put<ApiResponse<BackendSensorResponse[]>>('/v1/sensors/bulk/status', {
      sensorIds: ids,
      status,
    });
    return response.data.data.map(transformSensor);
  },

  // Export
  exportToCsv: async (filter?: SensorFilter): Promise<Blob> => {
    const params = new URLSearchParams();
    if (filter?.type) params.append('type', filter.type);
    if (filter?.status) params.append('status', filter.status);
    if (filter?.assetId) params.append('assetId', filter.assetId);

    const response = await api.get(`/v1/sensors/export?${params.toString()}`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
