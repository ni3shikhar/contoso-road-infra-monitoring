import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useAsset } from '@/hooks/useAssets';
import { useSensorsByAsset } from '@/hooks/useSensors';
import { useAlertsByAsset } from '@/hooks/useAlerts';
import { formatDate, formatDateTime, getHealthStatusColor, getAlertSeverityColor } from '@/lib/utils';
import { ArrowLeft, MapPin, Calendar, Radio, AlertTriangle } from 'lucide-react';

export default function AssetDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: asset, isLoading: assetLoading } = useAsset(id!);
  const { data: sensors, isLoading: sensorsLoading } = useSensorsByAsset(id!);
  const { data: alerts, isLoading: alertsLoading } = useAlertsByAsset(id!);

  if (assetLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-[200px]" />
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-[300px]" />
          <Skeleton className="h-[300px]" />
        </div>
      </div>
    );
  }

  if (!asset) {
    return (
      <div className="flex flex-col items-center justify-center h-96">
        <p className="text-muted-foreground">Asset not found</p>
        <Button variant="link" onClick={() => navigate('/assets')}>
          Back to Assets
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate('/assets')}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h2 className="text-3xl font-bold tracking-tight">{asset.name}</h2>
          <p className="text-muted-foreground">{asset.description}</p>
        </div>
      </div>

      {/* Overview Card */}
      <Card>
        <CardHeader>
          <CardTitle>Asset Overview</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
            <div>
              <p className="text-sm text-muted-foreground">Type</p>
              <Badge variant="outline" className="mt-1">
                {asset.type}
              </Badge>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Health Status</p>
              <div className="flex items-center gap-2 mt-1">
                <div
                  className={`h-3 w-3 rounded-full ${getHealthStatusColor(
                    asset.healthStatus
                  )}`}
                />
                <Badge
                  variant={
                    asset.healthStatus === 'HEALTHY'
                      ? 'success'
                      : asset.healthStatus === 'CRITICAL'
                      ? 'destructive'
                      : 'warning'
                  }
                >
                  {asset.healthStatus}
                </Badge>
              </div>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Location</p>
              <div className="flex items-center gap-2 mt-1">
                <MapPin className="h-4 w-4 text-muted-foreground" />
                <span>{asset.location?.address || 'Not specified'}</span>
              </div>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Construction Year</p>
              <div className="flex items-center gap-2 mt-1">
                <Calendar className="h-4 w-4 text-muted-foreground" />
                <span>{asset.constructionYear || 'Unknown'}</span>
              </div>
            </div>
          </div>
          <div className="grid gap-6 md:grid-cols-2 mt-6">
            <div>
              <p className="text-sm text-muted-foreground">Last Inspection</p>
              <p className="font-medium">
                {asset.lastInspectionDate
                  ? formatDate(asset.lastInspectionDate)
                  : 'Never'}
              </p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Next Inspection</p>
              <p className="font-medium">
                {asset.nextInspectionDate
                  ? formatDate(asset.nextInspectionDate)
                  : 'Not scheduled'}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Sensors */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Radio className="h-5 w-5" />
              Sensors
            </CardTitle>
            <CardDescription>
              {sensors?.length || 0} sensors attached to this asset
            </CardDescription>
          </CardHeader>
          <CardContent>
            {sensorsLoading ? (
              <div className="space-y-4">
                {[...Array(3)].map((_, i) => (
                  <Skeleton key={i} className="h-16" />
                ))}
              </div>
            ) : sensors?.length === 0 ? (
              <p className="text-muted-foreground">No sensors attached</p>
            ) : (
              <div className="space-y-4 max-h-[300px] overflow-auto">
                {sensors?.map((sensor) => (
                  <div
                    key={sensor.id}
                    className="flex items-center justify-between rounded-lg border p-4"
                  >
                    <div>
                      <p className="font-medium">{sensor.name}</p>
                      <p className="text-sm text-muted-foreground">
                        {sensor.type} • {sensor.unit}
                      </p>
                    </div>
                    <Badge variant={sensor.active ? 'success' : 'secondary'}>
                      {sensor.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Alerts */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <AlertTriangle className="h-5 w-5" />
              Recent Alerts
            </CardTitle>
            <CardDescription>Alerts for this asset</CardDescription>
          </CardHeader>
          <CardContent>
            {alertsLoading ? (
              <div className="space-y-4">
                {[...Array(3)].map((_, i) => (
                  <Skeleton key={i} className="h-16" />
                ))}
              </div>
            ) : !alerts?.content?.length ? (
              <p className="text-muted-foreground">No alerts</p>
            ) : (
              <div className="space-y-4 max-h-[300px] overflow-auto">
                {alerts?.content?.map((alert) => (
                  <div
                    key={alert.id}
                    className="flex items-start gap-3 rounded-lg border p-4"
                  >
                    <div
                      className={`h-2 w-2 mt-2 rounded-full ${getAlertSeverityColor(
                        alert.severity
                      )}`}
                    />
                    <div className="flex-1">
                      <p className="font-medium">{alert.title}</p>
                      <p className="text-sm text-muted-foreground">
                        {formatDateTime(alert.createdAt)}
                      </p>
                    </div>
                    <Badge
                      variant={
                        alert.severity === 'CRITICAL' ? 'destructive' : 'secondary'
                      }
                    >
                      {alert.severity}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
