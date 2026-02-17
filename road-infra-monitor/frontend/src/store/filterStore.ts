import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { TimeRange } from '@/components/shared/TimeRangeSelector';

interface FilterState {
  // Global time range
  globalTimeRange: TimeRange;
  customStartDate: string | null;
  customEndDate: string | null;
  
  // Asset selection
  selectedAssetIds: string[];
  selectedAssetType: string | null;
  
  // Sensor selection
  selectedSensorType: string | null;
  
  // Actions
  setGlobalTimeRange: (range: TimeRange, startDate?: string, endDate?: string) => void;
  setSelectedAssets: (assetIds: string[]) => void;
  toggleAssetSelection: (assetId: string) => void;
  setSelectedAssetType: (type: string | null) => void;
  setSelectedSensorType: (type: string | null) => void;
  clearFilters: () => void;
}

export const useFilterStore = create<FilterState>()(
  persist(
    (set, get) => ({
      globalTimeRange: '7d',
      customStartDate: null,
      customEndDate: null,
      selectedAssetIds: [],
      selectedAssetType: null,
      selectedSensorType: null,
      
      setGlobalTimeRange: (range, startDate, endDate) =>
        set({
          globalTimeRange: range,
          customStartDate: startDate || null,
          customEndDate: endDate || null,
        }),
        
      setSelectedAssets: (assetIds) => set({ selectedAssetIds: assetIds }),
      
      toggleAssetSelection: (assetId) => {
        const { selectedAssetIds } = get();
        if (selectedAssetIds.includes(assetId)) {
          set({ selectedAssetIds: selectedAssetIds.filter((id) => id !== assetId) });
        } else {
          set({ selectedAssetIds: [...selectedAssetIds, assetId] });
        }
      },
      
      setSelectedAssetType: (type) => set({ selectedAssetType: type }),
      
      setSelectedSensorType: (type) => set({ selectedSensorType: type }),
      
      clearFilters: () =>
        set({
          globalTimeRange: '7d',
          customStartDate: null,
          customEndDate: null,
          selectedAssetIds: [],
          selectedAssetType: null,
          selectedSensorType: null,
        }),
    }),
    {
      name: 'filter-storage',
      partialize: (state) => ({
        globalTimeRange: state.globalTimeRange,
        selectedAssetType: state.selectedAssetType,
        selectedSensorType: state.selectedSensorType,
      }),
    }
  )
);
