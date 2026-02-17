// Re-export auth types
export * from './auth';

// Import types for use in this file
import type { Role, Permission, User } from './auth';
export type { Role, Permission, User };

export interface ApiResponse<T> {
  status: 'SUCCESS' | 'ERROR';
  data: T;
  message?: string;
  timestamp: string;
  path?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// AssetType matching backend exactly
export type AssetType = 'ROAD' | 'BRIDGE' | 'TUNNEL' | 'DRAINAGE' | 'GUARDRAIL' | 'LIGHTING';

// SensorType matching backend exactly
export type SensorType = 
  | 'STRAIN_GAUGE'
  | 'ACCELEROMETER'
  | 'TEMPERATURE'
  | 'DISPLACEMENT'
  | 'CRACK_METER'
  | 'TILTMETER'
  | 'GPS'
  | 'CAMERA'
  | 'MOISTURE'
  | 'AIR_QUALITY';

// HealthStatus matching backend exactly
export type HealthStatus = 'HEALTHY' | 'WARNING' | 'CRITICAL' | 'OFFLINE' | 'UNKNOWN';

// AlertSeverity matching backend exactly
export type AlertSeverity = 'INFO' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

// AlertStatus matching backend exactly
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'RESOLVED' | 'DISMISSED' | 'ESCALATED' | 'AUTO_RESOLVED';

export interface Asset {
  id: string;
  name: string;
  type: AssetType;
  description?: string;
  location?: GeoLocation;
  // Direct lat/lng for convenience
  latitude?: number;
  longitude?: number;
  healthStatus: HealthStatus;
  healthScore?: number;
  lastInspectionDate?: string;
  nextInspectionDate?: string;
  constructionYear?: number;
  tags?: string[];
  metadata?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface GeoLocation {
  latitude: number;
  longitude: number;
  address?: string;
}

export type SensorStatus = 'ONLINE' | 'OFFLINE' | 'WARNING' | 'ERROR' | 'MAINTENANCE';

export interface Sensor {
  id: string;
  name: string;
  type: SensorType | string;
  assetId: string;
  assetName?: string;
  unit: string;
  status: SensorStatus | string;
  batteryLevel?: number;
  // Direct lat/lng for convenience
  latitude?: number;
  longitude?: number;
  location?: GeoLocation;
  minThreshold?: number;
  maxThreshold?: number;
  active: boolean;
  lastReading?: string;
  lastReadingValue?: SensorReading;
  installationDate?: string;
  calibrationDate?: string;
  firmwareVersion?: string;
  model?: string;
  manufacturer?: string;
  serialNumber?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SensorReading {
  id: string;
  sensorId: string;
  value: number;
  unit: string;
  timestamp: string;
  anomaly?: boolean;
  quality?: string;
}

export interface HealthRecord {
  id: string;
  assetId: string;
  status: HealthStatus;
  score?: number;
  details?: string;
  recommendations?: string[];
  recordedAt: string;
}

export interface Alert {
  id: string;
  title: string;
  message: string;
  severity: AlertSeverity;
  status: AlertStatus;
  assetId?: string;
  assetName?: string;
  sensorId?: string;
  sensorName?: string;
  acknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  createdAt: string;
}

export interface Kpi {
  id: string;
  metricName: string;
  displayName?: string;
  category?: string;
  value: number;
  previousValue?: number;
  targetValue?: number;
  unit?: string;
  percentageChange?: number;
  trend?: string;
  onTarget?: boolean;
  assetId?: string;
  assetName?: string;
  period?: string;
  periodStart?: string;
  periodEnd?: string;
  historicalData?: Record<string, number>;
  breakdown?: Record<string, number>;
  calculatedAt: string;
  createdAt: string;
}
