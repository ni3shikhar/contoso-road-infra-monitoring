import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type { HealthStatus, AlertSeverity } from '@/types';

// Health Status Badge
const healthStatusConfig: Record<HealthStatus, { label: string; className: string }> = {
  HEALTHY: { label: 'Healthy', className: 'bg-green-500 hover:bg-green-600 text-white' },
  WARNING: { label: 'Warning', className: 'bg-yellow-500 hover:bg-yellow-600 text-white' },
  CRITICAL: { label: 'Critical', className: 'bg-red-500 hover:bg-red-600 text-white' },
  OFFLINE: { label: 'Offline', className: 'bg-gray-500 hover:bg-gray-600 text-white' },
  UNKNOWN: { label: 'Unknown', className: 'bg-gray-400 hover:bg-gray-500 text-white' },
};

interface HealthStatusBadgeProps {
  status: HealthStatus;
  className?: string;
}

export function HealthStatusBadge({ status, className }: HealthStatusBadgeProps) {
  const config = healthStatusConfig[status] || healthStatusConfig.UNKNOWN;
  return (
    <Badge className={cn(config.className, className)}>
      {config.label}
    </Badge>
  );
}

// Alert Severity Badge
const alertSeverityConfig: Record<AlertSeverity, { label: string; className: string }> = {
  INFO: { label: 'Info', className: 'bg-blue-500 hover:bg-blue-600 text-white' },
  LOW: { label: 'Low', className: 'bg-green-500 hover:bg-green-600 text-white' },
  MEDIUM: { label: 'Medium', className: 'bg-yellow-500 hover:bg-yellow-600 text-white' },
  HIGH: { label: 'High', className: 'bg-orange-500 hover:bg-orange-600 text-white' },
  CRITICAL: { label: 'Critical', className: 'bg-red-500 hover:bg-red-600 text-white' },
};

interface AlertSeverityBadgeProps {
  severity: AlertSeverity;
  className?: string;
}

export function AlertSeverityBadge({ severity, className }: AlertSeverityBadgeProps) {
  const config = alertSeverityConfig[severity] || alertSeverityConfig.INFO;
  return (
    <Badge className={cn(config.className, className)}>
      {config.label}
    </Badge>
  );
}

// Sensor Status Badge
type SensorStatus = 'ACTIVE' | 'INACTIVE' | 'FAULTY' | 'MAINTENANCE' | 'DECOMMISSIONED';

const sensorStatusConfig: Record<SensorStatus, { label: string; className: string }> = {
  ACTIVE: { label: 'Active', className: 'bg-green-500 hover:bg-green-600 text-white' },
  INACTIVE: { label: 'Inactive', className: 'bg-gray-500 hover:bg-gray-600 text-white' },
  FAULTY: { label: 'Faulty', className: 'bg-red-500 hover:bg-red-600 text-white' },
  MAINTENANCE: { label: 'Maintenance', className: 'bg-blue-500 hover:bg-blue-600 text-white' },
  DECOMMISSIONED: { label: 'Decommissioned', className: 'bg-gray-400 hover:bg-gray-500 text-white' },
};

interface SensorStatusBadgeProps {
  status: SensorStatus;
  className?: string;
}

export function SensorStatusBadge({ status, className }: SensorStatusBadgeProps) {
  const config = sensorStatusConfig[status] || sensorStatusConfig.INACTIVE;
  return (
    <Badge className={cn(config.className, className)}>
      {config.label}
    </Badge>
  );
}

// Alert Status Badge - matching backend AlertStatus enum
type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'RESOLVED' | 'DISMISSED' | 'ESCALATED' | 'AUTO_RESOLVED';

const alertStatusConfig: Record<AlertStatus, { label: string; className: string }> = {
  OPEN: { label: 'Open', className: 'bg-red-500 hover:bg-red-600 text-white' },
  ACKNOWLEDGED: { label: 'Acknowledged', className: 'bg-yellow-500 hover:bg-yellow-600 text-white' },
  IN_PROGRESS: { label: 'In Progress', className: 'bg-blue-500 hover:bg-blue-600 text-white' },
  RESOLVED: { label: 'Resolved', className: 'bg-green-500 hover:bg-green-600 text-white' },
  DISMISSED: { label: 'Dismissed', className: 'bg-gray-500 hover:bg-gray-600 text-white' },
  ESCALATED: { label: 'Escalated', className: 'bg-orange-500 hover:bg-orange-600 text-white' },
  AUTO_RESOLVED: { label: 'Auto Resolved', className: 'bg-teal-500 hover:bg-teal-600 text-white' },
};

interface AlertStatusBadgeProps {
  status: AlertStatus;
  className?: string;
}

export function AlertStatusBadge({ status, className }: AlertStatusBadgeProps) {
  const config = alertStatusConfig[status] || alertStatusConfig.OPEN;
  return (
    <Badge className={cn(config.className, className)}>
      {config.label}
    </Badge>
  );
}

// Asset Type Badge
type AssetType = 'ROAD' | 'BRIDGE' | 'TUNNEL' | 'DRAINAGE' | 'GUARDRAIL' | 'LIGHTING';

const assetTypeConfig: Record<AssetType, { label: string; className: string }> = {
  ROAD: { label: 'Road', className: 'bg-slate-600 hover:bg-slate-700 text-white' },
  BRIDGE: { label: 'Bridge', className: 'bg-amber-600 hover:bg-amber-700 text-white' },
  TUNNEL: { label: 'Tunnel', className: 'bg-purple-600 hover:bg-purple-700 text-white' },
  DRAINAGE: { label: 'Drainage', className: 'bg-cyan-600 hover:bg-cyan-700 text-white' },
  GUARDRAIL: { label: 'Guardrail', className: 'bg-zinc-600 hover:bg-zinc-700 text-white' },
  LIGHTING: { label: 'Lighting', className: 'bg-yellow-600 hover:bg-yellow-700 text-white' },
};

interface AssetTypeBadgeProps {
  type: AssetType;
  className?: string;
}

export function AssetTypeBadge({ type, className }: AssetTypeBadgeProps) {
  const config = assetTypeConfig[type] || { label: type, className: 'bg-gray-500 text-white' };
  return (
    <Badge className={cn(config.className, className)}>
      {config.label}
    </Badge>
  );
}

// Generic Status Badge
interface StatusBadgeProps {
  status: string;
  variant?: 'success' | 'warning' | 'error' | 'info' | 'default';
  className?: string;
}

const variantConfig = {
  success: 'bg-green-500 hover:bg-green-600 text-white',
  warning: 'bg-yellow-500 hover:bg-yellow-600 text-white',
  error: 'bg-red-500 hover:bg-red-600 text-white',
  info: 'bg-blue-500 hover:bg-blue-600 text-white',
  default: 'bg-gray-500 hover:bg-gray-600 text-white',
};

export function StatusBadge({ status, variant = 'default', className }: StatusBadgeProps) {
  return (
    <Badge className={cn(variantConfig[variant], className)}>
      {status}
    </Badge>
  );
}
