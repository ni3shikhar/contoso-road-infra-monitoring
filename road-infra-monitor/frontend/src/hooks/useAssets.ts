import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { assetService, CreateAssetRequest, UpdateAssetRequest } from '@/services/assetService';
import { useToast } from '@/hooks/use-toast';
import type { AssetType, HealthStatus } from '@/types';

export function useAssets(page = 0, size = 20) {
  return useQuery({
    queryKey: ['assets', page, size],
    queryFn: () => assetService.getAll(page, size),
  });
}

export function useAsset(id: string) {
  return useQuery({
    queryKey: ['asset', id],
    queryFn: () => assetService.getById(id),
    enabled: !!id,
  });
}

export function useAssetsByType(type: AssetType, page = 0, size = 20) {
  return useQuery({
    queryKey: ['assets', 'type', type, page, size],
    queryFn: () => assetService.getByType(type, page, size),
    enabled: !!type,
  });
}

export function useAssetsByHealthStatus(status: HealthStatus) {
  return useQuery({
    queryKey: ['assets', 'health-status', status],
    queryFn: () => assetService.getByHealthStatus(status),
    enabled: !!status,
  });
}

export function useCreateAsset() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (data: CreateAssetRequest) => assetService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      toast({
        title: 'Asset created',
        description: 'The asset has been created successfully.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to create asset.',
        variant: 'destructive',
      });
    },
  });
}

export function useUpdateAsset() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateAssetRequest }) =>
      assetService.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      queryClient.invalidateQueries({ queryKey: ['asset', variables.id] });
      toast({
        title: 'Asset updated',
        description: 'The asset has been updated successfully.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to update asset.',
        variant: 'destructive',
      });
    },
  });
}

export function useDeleteAsset() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (id: string) => assetService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      toast({
        title: 'Asset deleted',
        description: 'The asset has been deleted successfully.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to delete asset.',
        variant: 'destructive',
      });
    },
  });
}
