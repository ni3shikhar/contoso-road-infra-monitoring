import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import {
  FileQuestion,
  Search,
  AlertCircle,
  Plus,
  FolderOpen,
  Radio,
  Building2,
  Bell,
  Users,
  BarChart3,
} from 'lucide-react';

type EmptyStateIcon = 
  | 'file'
  | 'search'
  | 'error'
  | 'folder'
  | 'sensor'
  | 'asset'
  | 'alert'
  | 'user'
  | 'analytics';

const iconMap: Record<EmptyStateIcon, typeof FileQuestion> = {
  file: FileQuestion,
  search: Search,
  error: AlertCircle,
  folder: FolderOpen,
  sensor: Radio,
  asset: Building2,
  alert: Bell,
  user: Users,
  analytics: BarChart3,
};

interface EmptyStateProps {
  icon?: EmptyStateIcon;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}

export function EmptyState({
  icon = 'file',
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  const Icon = iconMap[icon];

  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center rounded-lg border border-dashed p-8 text-center',
        className
      )}
    >
      <div className="rounded-full bg-muted p-4">
        <Icon className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="mt-4 text-lg font-semibold">{title}</h3>
      {description && (
        <p className="mt-2 max-w-sm text-sm text-muted-foreground">
          {description}
        </p>
      )}
      {action && (
        <Button onClick={action.onClick} className="mt-4 gap-2">
          <Plus className="h-4 w-4" />
          {action.label}
        </Button>
      )}
    </div>
  );
}

// Prebuilt empty states for common scenarios
export function NoDataEmptyState({ message = 'No data available' }: { message?: string }) {
  return (
    <EmptyState
      icon="folder"
      title={message}
      description="There's no data to display at the moment."
    />
  );
}

export function NoSearchResultsEmptyState({ query }: { query: string }) {
  return (
    <EmptyState
      icon="search"
      title="No results found"
      description={`No results found for "${query}". Try adjusting your search or filters.`}
    />
  );
}

export function NoSensorsEmptyState({ onAdd }: { onAdd?: () => void }) {
  return (
    <EmptyState
      icon="sensor"
      title="No sensors registered"
      description="Get started by registering your first sensor to begin monitoring."
      action={onAdd ? { label: 'Register Sensor', onClick: onAdd } : undefined}
    />
  );
}

export function NoAssetsEmptyState({ onAdd }: { onAdd?: () => void }) {
  return (
    <EmptyState
      icon="asset"
      title="No assets found"
      description="Create your first asset to start tracking infrastructure."
      action={onAdd ? { label: 'Create Asset', onClick: onAdd } : undefined}
    />
  );
}

export function NoAlertsEmptyState() {
  return (
    <EmptyState
      icon="alert"
      title="No active alerts"
      description="Great news! There are no active alerts at this time."
    />
  );
}

export function NoUsersEmptyState({ onAdd }: { onAdd?: () => void }) {
  return (
    <EmptyState
      icon="user"
      title="No users found"
      description="Create user accounts to grant access to the system."
      action={onAdd ? { label: 'Create User', onClick: onAdd } : undefined}
    />
  );
}

export function ErrorEmptyState({ onRetry }: { onRetry?: () => void }) {
  return (
    <EmptyState
      icon="error"
      title="Something went wrong"
      description="We couldn't load the data. Please try again."
      action={onRetry ? { label: 'Retry', onClick: onRetry } : undefined}
    />
  );
}
