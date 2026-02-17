import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useWebSocket } from '@/hooks/useWebSocket';
import { useAssets } from '@/hooks/useAssets';
import { getHealthStatusColor } from '@/lib/utils';
import { Activity, Wifi, WifiOff } from 'lucide-react';
import type { HealthRecord, SensorReading } from '@/types';

export default function Monitoring() {
  const { subscribe, unsubscribe, isConnected } = useWebSocket();
  const { data: assets, isLoading } = useAssets(0, 50);
  const [realtimeReadings, setRealtimeReadings] = useState<Map<string, SensorReading>>(new Map());
  const [healthUpdates, setHealthUpdates] = useState<HealthRecord[]>([]);

  useEffect(() => {
    if (!isConnected()) return;

    const readingsSubId = subscribe<SensorReading>('/topic/sensor-readings', (reading) => {
      setRealtimeReadings((prev) => new Map(prev).set(reading.sensorId, reading));
    });

    const healthSubId = subscribe<HealthRecord>('/topic/health-updates', (update) => {
      setHealthUpdates((prev) => [update, ...prev].slice(0, 10));
    });

    return () => {
      unsubscribe(readingsSubId);
      unsubscribe(healthSubId);
    };
  }, [subscribe, unsubscribe, isConnected]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Real-time Monitoring</h2>
          <p className="text-muted-foreground">
            Live monitoring of infrastructure health and sensor data
          </p>
        </div>
        <Badge variant={isConnected() ? 'success' : 'secondary'} className="flex items-center gap-2">
          {isConnected() ? (
            <>
              <Wifi className="h-4 w-4" />
              Connected
            </>
          ) : (
            <>
              <WifiOff className="h-4 w-4" />
              Disconnected
            </>
          )}
        </Badge>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Live Health Updates */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Activity className="h-5 w-5" />
              Live Health Updates
            </CardTitle>
            <CardDescription>Real-time health status changes</CardDescription>
          </CardHeader>
          <CardContent>
            {healthUpdates.length === 0 ? (
              <div className="flex h-[200px] items-center justify-center text-muted-foreground">
                Waiting for health updates...
              </div>
            ) : (
              <div className="space-y-4 max-h-[400px] overflow-auto">
                {healthUpdates.map((update, i) => (
                  <div
                    key={`${update.id}-${i}`}
                    className="flex items-center justify-between rounded-lg border p-4"
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`h-3 w-3 rounded-full ${getHealthStatusColor(update.status)}`}
                      />
                      <div>
                        <p className="font-medium">Asset {update.assetId.slice(0, 8)}...</p>
                        <p className="text-sm text-muted-foreground">
                          Score: {update.score?.toFixed(1) || 'N/A'}
                        </p>
                      </div>
                    </div>
                    <Badge
                      variant={
                        update.status === 'HEALTHY'
                          ? 'success'
                          : update.status === 'CRITICAL'
                          ? 'destructive'
                          : 'warning'
                      }
                    >
                      {update.status}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Asset Health Overview */}
        <Card>
          <CardHeader>
            <CardTitle>Asset Health Overview</CardTitle>
            <CardDescription>Current health status of all assets</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-4">
                {[...Array(5)].map((_, i) => (
                  <Skeleton key={i} className="h-12" />
                ))}
              </div>
            ) : (
              <div className="space-y-4 max-h-[400px] overflow-auto">
                {assets?.content.map((asset) => (
                  <div
                    key={asset.id}
                    className="flex items-center justify-between rounded-lg border p-4"
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`h-3 w-3 rounded-full ${getHealthStatusColor(
                          asset.healthStatus
                        )}`}
                      />
                      <div>
                        <p className="font-medium">{asset.name}</p>
                        <p className="text-sm text-muted-foreground">{asset.type}</p>
                      </div>
                    </div>
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
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
