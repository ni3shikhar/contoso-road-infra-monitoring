import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { alertService } from '@/services/alertService';
import { useToast } from '@/components/ui/use-toast';
import { useAlertStore } from '@/store/alertStore';

export function useAlerts(page = 0, size = 20) {
  const setAlerts = useAlertStore((state) => state.setAlerts);

  return useQuery({
    queryKey: ['alerts', page, size],
    queryFn: async () => {
      const result = await alertService.getAll(page, size);
      if (page === 0) {
        setAlerts(result.content);
      }
      return result;
    },
  });
}

export function useActiveAlerts() {
  const setAlerts = useAlertStore((state) => state.setAlerts);

  return useQuery({
    queryKey: ['alerts', 'active'],
    queryFn: async () => {
      const alerts = await alertService.getActive();
      setAlerts(alerts);
      return alerts;
    },
    refetchInterval: 30000,
  });
}

export function useUnacknowledgedAlerts() {
  return useQuery({
    queryKey: ['alerts', 'unacknowledged'],
    queryFn: () => alertService.getUnacknowledged(),
    refetchInterval: 30000,
  });
}

export function useAlertsByAsset(assetId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['alerts', 'asset', assetId, page, size],
    queryFn: () => alertService.getByAsset(assetId, page, size),
    enabled: !!assetId,
  });
}

export function useAcknowledgeAlert() {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const markAsRead = useAlertStore((state) => state.markAsRead);

  return useMutation({
    mutationFn: (id: string) => alertService.acknowledge(id),
    onSuccess: (alert) => {
      markAsRead(alert.id);
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      toast({
        title: 'Alert acknowledged',
        description: 'The alert has been acknowledged.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to acknowledge alert.',
        variant: 'destructive',
      });
    },
  });
}

export function useResolveAlert() {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const updateAlert = useAlertStore((state) => state.updateAlert);

  return useMutation({
    mutationFn: (id: string) => alertService.resolve(id),
    onSuccess: (alert) => {
      updateAlert(alert);
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      toast({
        title: 'Alert resolved',
        description: 'The alert has been resolved.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to resolve alert.',
        variant: 'destructive',
      });
    },
  });
}
