import { useState } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Skeleton } from '@/components/ui/skeleton';
import { useSensors } from '@/hooks/useSensors';
import { formatDate } from '@/lib/utils';
import { Plus, Search, Radio } from 'lucide-react';

export default function Sensors() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const { data, isLoading } = useSensors(page, 20);

  const filteredSensors = data?.content?.filter((sensor) => {
    const searchLower = search.toLowerCase();
    const statusText = sensor.active ? 'active' : 'inactive';
    const lastReadingText = sensor.lastReadingValue 
      ? `${sensor.lastReadingValue.value} ${sensor.unit}` 
      : (sensor.lastReading || '');
    const calibrationText = sensor.calibrationDate 
      ? formatDate(sensor.calibrationDate) 
      : 'never';
    
    return (
      (sensor?.name?.toLowerCase() || '').includes(searchLower) ||
      (sensor?.type?.toLowerCase() || '').includes(searchLower) ||
      (sensor?.assetName?.toLowerCase() || '').includes(searchLower) ||
      (sensor?.status?.toLowerCase() || '').includes(searchLower) ||
      statusText.includes(searchLower) ||
      lastReadingText.toLowerCase().includes(searchLower) ||
      calibrationText.toLowerCase().includes(searchLower)
    );
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Sensors</h2>
          <p className="text-muted-foreground">
            Manage your monitoring sensors
          </p>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          Add Sensor
        </Button>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search sensors..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16" />
              ))}
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Asset</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Last Reading</TableHead>
                    <TableHead>Calibration</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredSensors?.map((sensor) => (
                    <TableRow key={sensor.id}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Radio className="h-4 w-4 text-muted-foreground" />
                          <span className="font-medium">{sensor.name}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline">{sensor.type}</Badge>
                      </TableCell>
                      <TableCell>{sensor.assetName || '-'}</TableCell>
                      <TableCell>
                        <Badge variant={sensor.active ? 'success' : 'secondary'}>
                          {sensor.active ? 'Active' : 'Inactive'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {sensor.lastReadingValue
                          ? `${sensor.lastReadingValue.value} ${sensor.unit}`
                          : sensor.lastReading || '-'}
                      </TableCell>
                      <TableCell>
                        {sensor.calibrationDate
                          ? formatDate(sensor.calibrationDate)
                          : 'Never'}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              <div className="mt-4 flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  Showing {(data?.number || 0) * 20 + 1} to{' '}
                  {Math.min((data?.number || 0) * 20 + 20, data?.totalElements || 0)} of{' '}
                  {data?.totalElements} sensors
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data?.first}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data?.last}
                    onClick={() => setPage(page + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
