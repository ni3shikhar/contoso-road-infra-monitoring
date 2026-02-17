import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Separator } from '@/components/ui/separator';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  AreaChart,
  Area,
} from 'recharts';
import { SensorStatusBadge, BatteryLevel, GaugeChart, ConfirmDialog } from '@/components/shared';
import { PermissionGate } from '@/components/auth';
import { useToast } from '@/hooks/use-toast';
import {
  ArrowLeft,
  Radio,
  Activity,
  Calendar,
  MapPin,
  Settings,
  RefreshCw,
  Download,
  AlertTriangle,
  CheckCircle,
  Wrench,
  History,
  Zap,
  ThermometerSun,
  Gauge,
  Edit,
  Trash2,
} from 'lucide-react';
import { sensorService } from '@/services';
import type { Sensor, SensorReading } from '@/types';
import { format } from 'date-fns';

export default function SensorDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const [calibrateDialogOpen, setCalibrateDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [timeRange, setTimeRange] = useState('24h');

  // Queries
  const { data: sensor, isLoading } = useQuery({
    queryKey: ['sensor', id],
    queryFn: () => sensorService.getById(id!),
    enabled: !!id,
  });

  const { data: readingsData } = useQuery({
    queryKey: ['sensorReadings', id, timeRange],
    queryFn: () => sensorService.getReadings(id!, 0, 100),
    enabled: !!id,
  });

  const readings = readingsData?.content || [];

  // Mutations
  const calibrateMutation = useMutation({
    mutationFn: () => sensorService.recordCalibration(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sensor', id] });
      toast({ title: 'Calibration initiated successfully' });
      setCalibrateDialogOpen(false);
    },
    onError: () => {
      toast({ title: 'Failed to initiate calibration', variant: 'destructive' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => sensorService.delete(id!),
    onSuccess: () => {
      toast({ title: 'Sensor deleted successfully' });
      navigate('/sensors');
    },
    onError: () => {
      toast({ title: 'Failed to delete sensor', variant: 'destructive' });
    },
  });

  // Generate mock readings for demo
  const mockReadings = readings?.length ? readings : Array.from({ length: 24 }, (_, i) => ({
    timestamp: new Date(Date.now() - (23 - i) * 60 * 60 * 1000).toISOString(),
    value: 20 + Math.random() * 10 + Math.sin(i / 4) * 5,
    unit: '°C',
  }));

  // Mock calibration history
  const calibrationHistory = [
    { timestamp: '2024-01-15T10:30:00Z', status: 'SUCCESS', performedBy: 'John Smith' },
    { timestamp: '2023-10-15T14:00:00Z', status: 'SUCCESS', performedBy: 'System' },
    { timestamp: '2023-07-01T09:00:00Z', status: 'SUCCESS', performedBy: 'Jane Doe' },
  ];

  const latestReading = mockReadings[mockReadings.length - 1];
  const minValue = Math.min(...mockReadings.map((r: any) => r.value));
  const maxValue = Math.max(...mockReadings.map((r: any) => r.value));
  const avgValue = mockReadings.reduce((sum: number, r: any) => sum + r.value, 0) / mockReadings.length;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-[50vh]">
        <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  const sensorData: Sensor = sensor || {
    id: id!,
    name: 'Temperature Sensor 01',
    type: 'TEMPERATURE',
    status: 'ONLINE',
    batteryLevel: 85,
    latitude: 39.7392,
    longitude: -104.9903,
    assetId: 'asset-001',
    assetName: 'Highway Bridge 101',
    unit: '°C',
    active: true,
    lastReading: new Date().toISOString(),
    installationDate: '2023-06-15',
    firmwareVersion: '2.1.4',
    model: 'TempSense Pro X200',
    manufacturer: 'SensorTech Inc.',
    serialNumber: 'ST-2023-045678',
    createdAt: '2023-06-15T00:00:00Z',
    updatedAt: new Date().toISOString(),
  };

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="flex items-start gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate('/sensors')}>
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <div className="flex items-center gap-3">
              <Radio className="h-6 w-6 text-primary" />
              <h1 className="text-2xl font-bold">{sensorData.name}</h1>
              <SensorStatusBadge status={sensorData.status as 'ACTIVE' | 'INACTIVE' | 'FAULTY' | 'MAINTENANCE' | 'DECOMMISSIONED'} />
            </div>
            <p className="text-muted-foreground mt-1">
              {sensorData.type} Sensor • {sensorData.model || 'Standard Model'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <PermissionGate permission="SENSOR_CONFIGURE">
            <Button variant="outline" onClick={() => setCalibrateDialogOpen(true)}>
              <Wrench className="h-4 w-4 mr-2" />
              Calibrate
            </Button>
          </PermissionGate>
          <PermissionGate permission="SENSOR_WRITE">
            <Button variant="outline" onClick={() => setEditDialogOpen(true)}>
              <Edit className="h-4 w-4 mr-2" />
              Edit
            </Button>
          </PermissionGate>
          <PermissionGate permission="SENSOR_DELETE">
            <Button variant="destructive" onClick={() => setDeleteDialogOpen(true)}>
              <Trash2 className="h-4 w-4 mr-2" />
              Delete
            </Button>
          </PermissionGate>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Current Reading</CardTitle>
            <ThermometerSun className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">
              {latestReading?.value.toFixed(1)}{latestReading?.unit || '°C'}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              Last updated: {format(new Date(latestReading?.timestamp || Date.now()), 'HH:mm:ss')}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">24h Range</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {minValue.toFixed(1)} - {maxValue.toFixed(1)}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              Avg: {avgValue.toFixed(1)}°C
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Battery Level</CardTitle>
            <Zap className="h-4 w-4 text-yellow-500" />
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-4">
              <BatteryLevel level={sensorData.batteryLevel || 0} showLabel size="md" />
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              Estimated: {Math.ceil((sensorData.batteryLevel || 0) / 10)} weeks remaining
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Uptime</CardTitle>
            <CheckCircle className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">99.8%</div>
            <p className="text-xs text-muted-foreground mt-1">
              Last 30 days
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Main Content */}
      <Tabs defaultValue="readings" className="space-y-4">
        <TabsList>
          <TabsTrigger value="readings">Live Readings</TabsTrigger>
          <TabsTrigger value="details">Details</TabsTrigger>
          <TabsTrigger value="calibration">Calibration History</TabsTrigger>
          <TabsTrigger value="alerts">Alerts</TabsTrigger>
        </TabsList>

        <TabsContent value="readings" className="space-y-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle>Sensor Readings</CardTitle>
                <CardDescription>Real-time and historical data</CardDescription>
              </div>
              <div className="flex items-center gap-2">
                <Select value={timeRange} onValueChange={setTimeRange}>
                  <SelectTrigger className="w-[150px]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="1h">Last Hour</SelectItem>
                    <SelectItem value="24h">Last 24 Hours</SelectItem>
                    <SelectItem value="7d">Last 7 Days</SelectItem>
                    <SelectItem value="30d">Last 30 Days</SelectItem>
                  </SelectContent>
                </Select>
                <Button variant="outline" size="icon">
                  <Download className="h-4 w-4" />
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="h-[400px]">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={mockReadings}>
                    <defs>
                      <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#1B4F72" stopOpacity={0.3} />
                        <stop offset="95%" stopColor="#1B4F72" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                    <XAxis
                      dataKey="timestamp"
                      tickFormatter={(v) => format(new Date(v), 'HH:mm')}
                      stroke="#6b7280"
                      fontSize={12}
                    />
                    <YAxis stroke="#6b7280" fontSize={12} domain={['auto', 'auto']} />
                    <Tooltip
                      labelFormatter={(v) => format(new Date(v), 'MMM d, HH:mm:ss')}
                      formatter={(v: number) => [`${v.toFixed(2)}°C`, 'Value']}
                      contentStyle={{
                        backgroundColor: '#fff',
                        border: '1px solid #e5e7eb',
                        borderRadius: '8px',
                      }}
                    />
                    <Area
                      type="monotone"
                      dataKey="value"
                      stroke="#1B4F72"
                      strokeWidth={2}
                      fill="url(#colorValue)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          {/* Real-time Gauge */}
          <div className="grid gap-4 md:grid-cols-3">
            <Card>
              <CardHeader>
                <CardTitle className="text-sm">Current Value</CardTitle>
              </CardHeader>
              <CardContent className="flex justify-center">
                <GaugeChart
                  value={latestReading?.value || 0}
                  min={0}
                  max={50}
                  label="°C"
                />
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-sm">Signal Strength</CardTitle>
              </CardHeader>
              <CardContent className="flex justify-center">
                <GaugeChart
                  value={85}
                  min={0}
                  max={100}
                  label="%"
                />
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-sm">Data Quality</CardTitle>
              </CardHeader>
              <CardContent className="flex justify-center">
                <GaugeChart
                  value={98}
                  min={0}
                  max={100}
                  label="%"
                />
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="details">
          <Card>
            <CardHeader>
              <CardTitle>Sensor Information</CardTitle>
              <CardDescription>Technical specifications and configuration</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid gap-6 md:grid-cols-2">
                <div className="space-y-4">
                  <h3 className="font-semibold">General</h3>
                  <Separator />
                  <div className="grid gap-3">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Sensor ID</span>
                      <span className="font-mono">{sensorData.id}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Type</span>
                      <Badge variant="outline">{sensorData.type}</Badge>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Status</span>
                      <SensorStatusBadge status={sensorData.status as 'ACTIVE' | 'INACTIVE' | 'FAULTY' | 'MAINTENANCE' | 'DECOMMISSIONED'} />
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Model</span>
                      <span>{sensorData.model || '-'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Manufacturer</span>
                      <span>{sensorData.manufacturer || '-'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Serial Number</span>
                      <span className="font-mono">{sensorData.serialNumber || '-'}</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <h3 className="font-semibold">Configuration</h3>
                  <Separator />
                  <div className="grid gap-3">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Firmware Version</span>
                      <span className="font-mono">{sensorData.firmwareVersion || '-'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Installation Date</span>
                      <span>
                        {sensorData.installationDate
                          ? format(new Date(sensorData.installationDate), 'MMM d, yyyy')
                          : '-'}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Last Calibration</span>
                      <span>-</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Sampling Rate</span>
                      <span>60 seconds</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <h3 className="font-semibold">Location</h3>
                  <Separator />
                  <div className="grid gap-3">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Latitude</span>
                      <span className="font-mono">{sensorData.latitude?.toFixed(6) || '-'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Longitude</span>
                      <span className="font-mono">{sensorData.longitude?.toFixed(6) || '-'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Associated Asset</span>
                      <Button
                        variant="link"
                        className="h-auto p-0"
                        onClick={() => navigate(`/assets/${sensorData.assetId}`)}
                      >
                        {sensorData.assetName || sensorData.assetId}
                      </Button>
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <h3 className="font-semibold">Thresholds</h3>
                  <Separator />
                  <div className="grid gap-3">
                    <div className="flex justify-between items-center">
                      <span className="text-muted-foreground">Warning Threshold</span>
                      <div className="flex items-center gap-2">
                        <AlertTriangle className="h-4 w-4 text-yellow-500" />
                        <span>35°C</span>
                      </div>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-muted-foreground">Critical Threshold</span>
                      <div className="flex items-center gap-2">
                        <AlertTriangle className="h-4 w-4 text-red-500" />
                        <span>45°C</span>
                      </div>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Min Valid</span>
                      <span>-20°C</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Max Valid</span>
                      <span>60°C</span>
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="calibration">
          <Card>
            <CardHeader>
              <CardTitle>Calibration History</CardTitle>
              <CardDescription>Record of all calibration events</CardDescription>
            </CardHeader>
            <CardContent>
              {calibrationHistory?.length ? (
                <div className="space-y-4">
                  {calibrationHistory.map((cal: any, i: number) => (
                    <div key={i} className="flex items-start gap-4 p-4 border rounded-lg">
                      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
                        <Wrench className="h-5 w-5 text-primary" />
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <span className="font-medium">Calibration #{calibrationHistory.length - i}</span>
                          <Badge variant={cal.status === 'SUCCESS' ? 'default' : 'destructive'}>
                            {cal.status}
                          </Badge>
                        </div>
                        <p className="text-sm text-muted-foreground mt-1">
                          {format(new Date(cal.timestamp), 'MMMM d, yyyy HH:mm')}
                        </p>
                        <p className="text-sm mt-2">
                          Performed by: {cal.performedBy || 'System'}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 text-muted-foreground">
                  <History className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>No calibration history available</p>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="alerts">
          <Card>
            <CardHeader>
              <CardTitle>Related Alerts</CardTitle>
              <CardDescription>Alerts triggered by this sensor</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8 text-muted-foreground">
                <CheckCircle className="h-12 w-12 mx-auto mb-4 text-green-500 opacity-50" />
                <p>No active alerts for this sensor</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Calibration Dialog */}
      <ConfirmDialog
        open={calibrateDialogOpen}
        onOpenChange={setCalibrateDialogOpen}
        title="Calibrate Sensor"
        description="This will initiate a calibration cycle for the sensor. The sensor may be temporarily offline during calibration."
        confirmText="Start Calibration"
        onConfirm={() => calibrateMutation.mutate()}
        isLoading={calibrateMutation.isPending}
      />

      {/* Delete Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        title="Delete Sensor"
        description={`Are you sure you want to delete "${sensorData.name}"? This action cannot be undone.`}
        confirmText="Delete"
        variant="destructive"
        onConfirm={() => deleteMutation.mutate()}
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
