import { useState, useEffect, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import { useNavigate } from 'react-router-dom';
import L from 'leaflet';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { SensorStatusBadge, BatteryLevel } from '@/components/shared';
import {
  Search,
  Filter,
  Layers,
  Radio,
  Building2,
  AlertTriangle,
  ZoomIn,
  ZoomOut,
  Crosshair,
  ExternalLink,
} from 'lucide-react';
import { useSensorStore } from '@/store/sensorStore';
import { sensorService, assetService } from '@/services';
import type { Sensor, Asset } from '@/types';
import 'leaflet/dist/leaflet.css';

// Fix Leaflet default marker icon
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// Custom marker icons
const createIcon = (color: string) => {
  return L.divIcon({
    className: 'custom-marker',
    html: `<div style="
      background-color: ${color};
      width: 24px;
      height: 24px;
      border-radius: 50% 50% 50% 0;
      transform: rotate(-45deg);
      border: 2px solid white;
      box-shadow: 0 2px 4px rgba(0,0,0,0.3);
    "></div>`,
    iconSize: [24, 24],
    iconAnchor: [12, 24],
    popupAnchor: [0, -24],
  });
};

const defaultSensorIcon = createIcon('#6B7280');

const sensorIcons: Record<string, L.DivIcon> = {
  Active: createIcon('#27AE60'),
  Inactive: createIcon('#6B7280'),
  Offline: createIcon('#6B7280'),
  Maintenance: createIcon('#F39C12'),
  Faulty: createIcon('#E74C3C'),
  Decommissioned: createIcon('#9CA3AF'),
};

// Helper to get sensor icon by status (case-insensitive)
const getSensorIcon = (status: string | undefined): L.DivIcon => {
  if (!status) return defaultSensorIcon;
  // Try exact match first, then capitalized version
  return sensorIcons[status] || 
         sensorIcons[status.charAt(0).toUpperCase() + status.slice(1).toLowerCase()] || 
         defaultSensorIcon;
};

const assetIcon = createIcon('#1B4F72');

// Default map center (Seattle)
const DEFAULT_CENTER: [number, number] = [47.615, -122.32];
const DEFAULT_ZOOM = 14;

// Map controls component
function MapControls() {
  const map = useMap();

  const handleZoomIn = () => map.zoomIn();
  const handleZoomOut = () => map.zoomOut();
  const handleReset = () => {
    map.setView(DEFAULT_CENTER, DEFAULT_ZOOM);
  };

  return (
    <div className="absolute bottom-4 right-4 z-[1000] flex flex-col gap-2">
      <Button size="icon" variant="secondary" onClick={handleZoomIn}>
        <ZoomIn className="h-4 w-4" />
      </Button>
      <Button size="icon" variant="secondary" onClick={handleZoomOut}>
        <ZoomOut className="h-4 w-4" />
      </Button>
      <Button size="icon" variant="secondary" onClick={handleReset}>
        <Crosshair className="h-4 w-4" />
      </Button>
    </div>
  );
}

// Corridor path (Seattle area - Highway 101)
const corridorPath: [number, number][] = [
  [47.6062, -122.3321],
  [47.6098, -122.3273],
  [47.6134, -122.3225],
  [47.617, -122.3177],
  [47.6197, -122.3141],
  [47.6230, -122.3100],
];

export default function CorridorMap() {
  const navigate = useNavigate();
  const { sensors, setSensors } = useSensorStore();
  const [assets, setAssets] = useState<Asset[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSensor, setSelectedSensor] = useState<Sensor | null>(null);
  
  // Layer visibility
  const [showSensors, setShowSensors] = useState(true);
  const [showAssets, setShowAssets] = useState(true);
  const [showCorridor, setShowCorridor] = useState(true);
  
  // Filters
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [typeFilter, setTypeFilter] = useState<string>('all');

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [sensorsRes, assetsRes] = await Promise.all([
          sensorService.getAll(0, 100),
          assetService.getAll(0, 100),
        ]);
        
        // Add mock coordinates if not present
        const sensorsWithCoords = (sensorsRes?.content || []).map((s: Sensor, i: number) => ({
          ...s,
          latitude: s.latitude || DEFAULT_CENTER[0] + (Math.random() - 0.5) * 0.02,
          longitude: s.longitude || DEFAULT_CENTER[1] + (Math.random() - 0.5) * 0.02,
        }));
        
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const assetsWithCoords = (assetsRes?.content || []).map((a: any, i: number) => ({
          ...a,
          // Backend returns startLatitude/startLongitude, map to latitude/longitude
          latitude: a.latitude || a.startLatitude || DEFAULT_CENTER[0] + (Math.random() - 0.5) * 0.02,
          longitude: a.longitude || a.startLongitude || DEFAULT_CENTER[1] + (Math.random() - 0.5) * 0.02,
        }));
        
        setSensors(sensorsWithCoords);
        setAssets(assetsWithCoords);
      } catch (error) {
        console.error('Failed to fetch map data:', error);
        // Set demo data
        setSensors([
          { id: '1', name: 'Temperature Sensor 01', type: 'TEMPERATURE', status: 'ONLINE', latitude: 39.7450, longitude: -105.0200, batteryLevel: 85 } as Sensor,
          { id: '2', name: 'Vibration Sensor 01', type: 'VIBRATION', status: 'WARNING', latitude: 39.7550, longitude: -104.9800, batteryLevel: 45 } as Sensor,
          { id: '3', name: 'Strain Gauge 01', type: 'STRAIN', status: 'ONLINE', latitude: 39.7650, longitude: -104.9400, batteryLevel: 92 } as Sensor,
          { id: '4', name: 'Weather Station 01', type: 'WEATHER', status: 'OFFLINE', latitude: 39.7750, longitude: -104.9000, batteryLevel: 0 } as Sensor,
        ]);
        setAssets([
          { id: '1', name: 'Highway Bridge 101', type: 'BRIDGE', healthScore: 87, latitude: 39.7500, longitude: -105.0000 } as Asset,
          { id: '2', name: 'Tunnel Section A', type: 'TUNNEL', healthScore: 92, latitude: 39.7700, longitude: -104.9200 } as Asset,
        ]);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [setSensors]);

  // Filter sensors
  const filteredSensors = useMemo(() => {
    return sensors.filter((sensor) => {
      const matchesSearch = (sensor?.name?.toLowerCase() || '').includes(searchQuery.toLowerCase()) ||
        (sensor?.id?.toLowerCase() || '').includes(searchQuery.toLowerCase());
      const matchesStatus = statusFilter === 'all' || sensor.status === statusFilter;
      const matchesType = typeFilter === 'all' || sensor.type === typeFilter;
      return matchesSearch && matchesStatus && matchesType;
    });
  }, [sensors, searchQuery, statusFilter, typeFilter]);

  // Get unique sensor types
  const sensorTypes = useMemo(() => {
    const types = new Set(sensors.map((s) => s.type));
    return Array.from(types);
  }, [sensors]);

  // Stats
  const stats = useMemo(() => ({
    totalSensors: sensors.length,
    activeSensors: sensors.filter((s) => s.status === 'Active' || s.active).length,
    maintenanceSensors: sensors.filter((s) => s.status === 'Maintenance' || s.status === 'Faulty').length,
    offlineSensors: sensors.filter((s) => s.status === 'Offline' || s.status === 'Inactive' || s.status === 'Decommissioned').length,
    totalAssets: assets.length,
  }), [sensors, assets]);

  return (
    <div className="h-[calc(100vh-4rem)] flex flex-col">
      {/* Header */}
      <div className="p-4 border-b bg-card">
        <div className="flex flex-col lg:flex-row lg:items-center gap-4">
          <div>
            <h1 className="text-2xl font-bold">Corridor Map</h1>
            <p className="text-sm text-muted-foreground">
              Interactive map view of all sensors and assets
            </p>
          </div>
          
          <div className="flex-1 flex flex-wrap items-center gap-4 lg:justify-end">
            {/* Search */}
            <div className="relative w-full max-w-xs">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search sensors..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
            
            {/* Filter Sheet */}
            <Sheet>
              <SheetTrigger asChild>
                <Button variant="outline" size="icon">
                  <Filter className="h-4 w-4" />
                </Button>
              </SheetTrigger>
              <SheetContent>
                <SheetHeader>
                  <SheetTitle>Map Filters</SheetTitle>
                </SheetHeader>
                <div className="mt-6 space-y-6">
                  {/* Layers */}
                  <div className="space-y-4">
                    <h3 className="font-medium flex items-center gap-2">
                      <Layers className="h-4 w-4" />
                      Layers
                    </h3>
                    <div className="space-y-3">
                      <div className="flex items-center gap-2">
                        <Checkbox
                          id="show-sensors"
                          checked={showSensors}
                          onCheckedChange={(checked: boolean | 'indeterminate') => setShowSensors(!!checked)}
                        />
                        <Label htmlFor="show-sensors">Sensors</Label>
                      </div>
                      <div className="flex items-center gap-2">
                        <Checkbox
                          id="show-assets"
                          checked={showAssets}
                          onCheckedChange={(checked: boolean | 'indeterminate') => setShowAssets(!!checked)}
                        />
                        <Label htmlFor="show-assets">Assets</Label>
                      </div>
                      <div className="flex items-center gap-2">
                        <Checkbox
                          id="show-corridor"
                          checked={showCorridor}
                          onCheckedChange={(checked: boolean | 'indeterminate') => setShowCorridor(!!checked)}
                        />
                        <Label htmlFor="show-corridor">Corridor Path</Label>
                      </div>
                    </div>
                  </div>
                  
                  {/* Status Filter */}
                  <div className="space-y-2">
                    <Label>Sensor Status</Label>
                    <Select value={statusFilter} onValueChange={setStatusFilter}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All Statuses</SelectItem>
                        <SelectItem value="Active">Active</SelectItem>
                        <SelectItem value="Inactive">Inactive</SelectItem>
                        <SelectItem value="Maintenance">Maintenance</SelectItem>
                        <SelectItem value="Faulty">Faulty</SelectItem>
                        <SelectItem value="Offline">Offline</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  {/* Type Filter */}
                  <div className="space-y-2">
                    <Label>Sensor Type</Label>
                    <Select value={typeFilter} onValueChange={setTypeFilter}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All Types</SelectItem>
                        {sensorTypes.map((type) => (
                          <SelectItem key={type} value={type}>
                            {type}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
              </SheetContent>
            </Sheet>
            
            {/* Stats */}
            <div className="hidden xl:flex items-center gap-4 text-sm">
              <div className="flex items-center gap-1">
                <Radio className="h-4 w-4 text-primary" />
                <span>{stats.totalSensors} Sensors</span>
              </div>
              <div className="flex items-center gap-1">
                <div className="h-2 w-2 rounded-full bg-green-500" />
                <span>{stats.activeSensors} Active</span>
              </div>
              <div className="flex items-center gap-1">
                <div className="h-2 w-2 rounded-full bg-yellow-500" />
                <span>{stats.maintenanceSensors} Maintenance</span>
              </div>
              <div className="flex items-center gap-1">
                <Building2 className="h-4 w-4 text-primary" />
                <span>{stats.totalAssets} Assets</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Map Container */}
      <div className="flex-1 relative">
        <MapContainer
          center={DEFAULT_CENTER}
          zoom={DEFAULT_ZOOM}
          className="h-full w-full"
          zoomControl={false}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          
          {/* Corridor Path */}
          {showCorridor && (
            <Polyline
              positions={corridorPath}
              color="#1B4F72"
              weight={4}
              opacity={0.7}
            />
          )}
          
          {/* Sensor Markers */}
          {showSensors && filteredSensors.filter(s => s.latitude && s.longitude).map((sensor) => (
            <Marker
              key={sensor.id}
              position={[sensor.latitude!, sensor.longitude!]}
              icon={getSensorIcon(sensor.status)}
              eventHandlers={{
                click: () => setSelectedSensor(sensor),
              }}
            >
              <Popup>
                <div className="min-w-[200px]">
                  <div className="font-semibold">{sensor.name}</div>
                  <div className="text-sm text-muted-foreground mb-2">{sensor.type}</div>
                  <div className="flex items-center gap-2 mb-2">
                    <SensorStatusBadge status={sensor.status as 'ACTIVE' | 'INACTIVE' | 'FAULTY' | 'MAINTENANCE' | 'DECOMMISSIONED'} />
                    {sensor.batteryLevel !== undefined && (
                      <BatteryLevel level={sensor.batteryLevel} />
                    )}
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    className="w-full"
                    onClick={() => navigate(`/sensors/${sensor.id}`)}
                  >
                    <ExternalLink className="h-3 w-3 mr-1" />
                    View Details
                  </Button>
                </div>
              </Popup>
            </Marker>
          ))}
          
          {/* Asset Markers */}
          {showAssets && assets.map((asset) => (
            <Marker
              key={asset.id}
              position={[asset.latitude || 0, asset.longitude || 0]}
              icon={assetIcon}
            >
              <Popup>
                <div className="min-w-[200px]">
                  <div className="font-semibold">{asset.name}</div>
                  <div className="text-sm text-muted-foreground mb-2">{asset.type}</div>
                  {asset.healthScore !== undefined && (
                    <div className="mb-2">
                      <Badge variant={asset.healthScore >= 80 ? 'default' : asset.healthScore >= 60 ? 'secondary' : 'destructive'}>
                        Health: {asset.healthScore}%
                      </Badge>
                    </div>
                  )}
                  <Button
                    size="sm"
                    variant="outline"
                    className="w-full"
                    onClick={() => navigate(`/assets/${asset.id}`)}
                  >
                    <ExternalLink className="h-3 w-3 mr-1" />
                    View Details
                  </Button>
                </div>
              </Popup>
            </Marker>
          ))}
          
          <MapControls />
        </MapContainer>

        {/* Legend */}
        <Card className="absolute bottom-4 left-4 z-[1000] w-48">
          <CardHeader className="py-2 px-3">
            <CardTitle className="text-sm">Legend</CardTitle>
          </CardHeader>
          <CardContent className="py-2 px-3 space-y-1 text-xs">
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-full bg-green-500" />
              <span>Active Sensor</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-full bg-yellow-500" />
              <span>Maintenance</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-full bg-red-500" />
              <span>Faulty</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-full bg-gray-500" />
              <span>Inactive/Offline</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-full bg-primary" />
              <span>Asset</span>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
