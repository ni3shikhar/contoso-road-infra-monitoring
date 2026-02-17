import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sensorService, CreateSensorRequest, UpdateSensorRequest } from '@/services/sensorService';
import { useToast } from '@/components/ui/use-toast';

export function useSensors(page = 0, size = 20) {
  return useQuery({
    queryKey: ['sensors', page, size],
    queryFn: () => sensorService.getAll(page, size),
  });
}

export function useSensor(id: string) {
  return useQuery({
    queryKey: ['sensor', id],
    queryFn: () => sensorService.getById(id),
    enabled: !!id,
  });
}

export function useSensorsByAsset(assetId: string) {
  return useQuery({
    queryKey: ['sensors', 'asset', assetId],
    queryFn: () => sensorService.getByAsset(assetId),
    enabled: !!assetId,
  });
}

export function useSensorReadings(sensorId: string, page = 0, size = 100) {
  return useQuery({
    queryKey: ['sensor-readings', sensorId, page, size],
    queryFn: () => sensorService.getReadings(sensorId, page, size),
    enabled: !!sensorId,
    refetchInterval: 30000, // Refetch every 30 seconds
  });
}

export function useCreateSensor() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (data: CreateSensorRequest) => sensorService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sensors'] });
      toast({
        title: 'Sensor created',
        description: 'The sensor has been created successfully.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to create sensor.',
        variant: 'destructive',
      });
    },
  });
}

export function useUpdateSensor() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateSensorRequest }) =>
      sensorService.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sensors'] });
      queryClient.invalidateQueries({ queryKey: ['sensor', variables.id] });
      toast({
        title: 'Sensor updated',
        description: 'The sensor has been updated successfully.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to update sensor.',
        variant: 'destructive',
      });
    },
  });
}

export function useDeleteSensor() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (id: string) => sensorService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sensors'] });
      toast({
        title: 'Sensor deleted',
        description: 'The sensor has been deleted successfully.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to delete sensor.',
        variant: 'destructive',
      });
    },
  });
}
