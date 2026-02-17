import api from './api';
import type { ApiResponse, PaginatedResponse, Asset, HealthRecord, AssetType, HealthStatus } from '@/types';

export interface CreateAssetRequest {
  assetCode: string;
  name: string;
  type: AssetType;
  description?: string;
  latitude?: number;
  longitude?: number;
  address?: string;
  constructionYear?: number;
  parentAssetId?: string;
  plannedStartDate?: string;
  plannedEndDate?: string;
  tags?: string[];
  metadata?: Record<string, unknown>;
}

export interface UpdateAssetRequest extends Partial<Omit<CreateAssetRequest, 'assetCode'>> {}

export interface AssetFilter {
  type?: AssetType;
  healthStatus?: HealthStatus;
  search?: string;
  page?: number;
  size?: number;
}

export interface AssetMilestone {
  id: string;
  assetId: string;
  name: string;
  description?: string;
  plannedDate: string;
  actualDate?: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'DELAYED';
  progress: number;
}

export interface AssetInspection {
  id: string;
  assetId: string;
  inspectorId: string;
  inspectorName: string;
  inspectionDate: string;
  type: string;
  findings: string;
  recommendations?: string;
  overallRating: 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR' | 'CRITICAL';
  attachments?: string[];
  createdAt: string;
}

export interface CreateInspectionRequest {
  type: string;
  findings: string;
  recommendations?: string;
  overallRating: 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR' | 'CRITICAL';
}

export interface GeoJsonFeature {
  type: 'Feature';
  geometry: {
    type: string;
    coordinates: number[] | number[][] | number[][][];
  };
  properties: {
    id: string;
    name: string;
    assetType: AssetType;
    healthStatus: HealthStatus;
    [key: string]: unknown;
  };
}

export interface GeoJsonCollection {
  type: 'FeatureCollection';
  features: GeoJsonFeature[];
}

export const assetService = {
  getAll: async (page = 0, size = 20): Promise<PaginatedResponse<Asset>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<Asset>>>('/v1/assets', {
      params: { page, size },
    });
    return response.data.data;
  },

  getFiltered: async (filter: AssetFilter): Promise<PaginatedResponse<Asset>> => {
    const params = new URLSearchParams();
    if (filter.type) params.append('type', filter.type);
    if (filter.healthStatus) params.append('healthStatus', filter.healthStatus);
    if (filter.search) params.append('search', filter.search);
    if (filter.page !== undefined) params.append('page', String(filter.page));
    if (filter.size !== undefined) params.append('size', String(filter.size));

    const response = await api.get<ApiResponse<PaginatedResponse<Asset>>>(
      `/v1/assets?${params.toString()}`
    );
    return response.data.data;
  },

  getById: async (id: string): Promise<Asset> => {
    const response = await api.get<ApiResponse<Asset>>(`/v1/assets/${id}`);
    return response.data.data;
  },

  getByCode: async (code: string): Promise<Asset> => {
    const response = await api.get<ApiResponse<Asset>>(`/v1/assets/code/${code}`);
    return response.data.data;
  },

  create: async (data: CreateAssetRequest): Promise<Asset> => {
    const response = await api.post<ApiResponse<Asset>>('/v1/assets', data);
    return response.data.data;
  },

  update: async (id: string, data: UpdateAssetRequest): Promise<Asset> => {
    const response = await api.put<ApiResponse<Asset>>(`/v1/assets/${id}`, data);
    return response.data.data;
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/v1/assets/${id}`);
  },

  // Progress
  updateProgress: async (id: string, progress: number): Promise<Asset> => {
    const response = await api.put<ApiResponse<Asset>>(`/v1/assets/${id}/progress`, { progress });
    return response.data.data;
  },

  // By Type and Status
  getByType: async (type: AssetType, page = 0, size = 20): Promise<PaginatedResponse<Asset>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<Asset>>>(`/v1/assets/type/${type}`, {
      params: { page, size },
    });
    return response.data.data;
  },

  getByHealthStatus: async (status: HealthStatus): Promise<Asset[]> => {
    const response = await api.get<ApiResponse<Asset[]>>(`/v1/assets/health-status/${status}`);
    return response.data.data;
  },

  // Child Assets
  getChildAssets: async (parentId: string): Promise<Asset[]> => {
    const response = await api.get<ApiResponse<Asset[]>>(`/v1/assets/${parentId}/children`);
    return response.data.data;
  },

  // Health History
  getHealthHistory: async (id: string, page = 0, size = 20): Promise<PaginatedResponse<HealthRecord>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<HealthRecord>>>(`/v1/assets/${id}/health-history`, {
      params: { page, size },
    });
    return response.data.data;
  },

  // Milestones
  getMilestones: async (assetId: string): Promise<AssetMilestone[]> => {
    const response = await api.get<ApiResponse<AssetMilestone[]>>(`/v1/assets/${assetId}/milestones`);
    return response.data.data;
  },

  createMilestone: async (assetId: string, data: Omit<AssetMilestone, 'id' | 'assetId'>): Promise<AssetMilestone> => {
    const response = await api.post<ApiResponse<AssetMilestone>>(`/v1/assets/${assetId}/milestones`, data);
    return response.data.data;
  },

  updateMilestone: async (assetId: string, milestoneId: string, data: Partial<AssetMilestone>): Promise<AssetMilestone> => {
    const response = await api.put<ApiResponse<AssetMilestone>>(
      `/v1/assets/${assetId}/milestones/${milestoneId}`,
      data
    );
    return response.data.data;
  },

  // Inspections
  getInspections: async (assetId: string, page = 0, size = 20): Promise<PaginatedResponse<AssetInspection>> => {
    const response = await api.get<ApiResponse<PaginatedResponse<AssetInspection>>>(
      `/v1/assets/${assetId}/inspections`,
      { params: { page, size } }
    );
    return response.data.data;
  },

  createInspection: async (assetId: string, data: CreateInspectionRequest): Promise<AssetInspection> => {
    const response = await api.post<ApiResponse<AssetInspection>>(`/v1/assets/${assetId}/inspections`, data);
    return response.data.data;
  },

  // GeoJSON for Map
  getGeoJson: async (): Promise<GeoJsonCollection> => {
    const response = await api.get<ApiResponse<GeoJsonCollection>>('/v1/assets/geojson');
    return response.data.data;
  },

  getAssetGeoJson: async (id: string): Promise<GeoJsonFeature> => {
    const response = await api.get<ApiResponse<GeoJsonFeature>>(`/v1/assets/${id}/geojson`);
    return response.data.data;
  },

  // Stats
  getStats: async (): Promise<{
    total: number;
    byType: Record<AssetType, number>;
    byHealth: Record<HealthStatus, number>;
    averageProgress: number;
  }> => {
    const response = await api.get<ApiResponse<{
      total: number;
      byType: Record<AssetType, number>;
      byHealth: Record<HealthStatus, number>;
      averageProgress: number;
    }>>('/v1/assets/stats');
    return response.data.data;
  },
};
