import { create } from 'zustand';
import type { Sensor, SensorReading } from '@/types';

interface SensorState {
  sensors: Sensor[];
  selectedSensor: Sensor | null;
  liveReadings: Map<string, SensorReading[]>;
  maxReadingsPerSensor: number;
  
  // Actions
  setSensors: (sensors: Sensor[]) => void;
  addSensor: (sensor: Sensor) => void;
  updateSensor: (id: string, updates: Partial<Sensor>) => void;
  removeSensor: (id: string) => void;
  setSelectedSensor: (sensor: Sensor | null) => void;
  addLiveReading: (sensorId: string, reading: SensorReading) => void;
  setLiveReadings: (sensorId: string, readings: SensorReading[]) => void;
  clearLiveReadings: (sensorId: string) => void;
  clearAllReadings: () => void;
}

export const useSensorStore = create<SensorState>((set, get) => ({
  sensors: [],
  selectedSensor: null,
  liveReadings: new Map(),
  maxReadingsPerSensor: 100,
  
  setSensors: (sensors) => set({ sensors }),
  
  addSensor: (sensor) => set((state) => ({
    sensors: [...state.sensors, sensor],
  })),
  
  updateSensor: (id, updates) => set((state) => ({
    sensors: state.sensors.map((s) => 
      s.id === id ? { ...s, ...updates } : s
    ),
  })),
  
  removeSensor: (id) => set((state) => ({
    sensors: state.sensors.filter((s) => s.id !== id),
  })),
  
  setSelectedSensor: (sensor) => set({ selectedSensor: sensor }),
  
  addLiveReading: (sensorId, reading) => {
    const { liveReadings, maxReadingsPerSensor } = get();
    const currentReadings = liveReadings.get(sensorId) || [];
    const newReadings = [reading, ...currentReadings].slice(0, maxReadingsPerSensor);
    
    const newMap = new Map(liveReadings);
    newMap.set(sensorId, newReadings);
    
    set({ liveReadings: newMap });
  },
  
  setLiveReadings: (sensorId, readings) => {
    const { liveReadings, maxReadingsPerSensor } = get();
    const newMap = new Map(liveReadings);
    newMap.set(sensorId, readings.slice(0, maxReadingsPerSensor));
    
    set({ liveReadings: newMap });
  },
  
  clearLiveReadings: (sensorId) => {
    const { liveReadings } = get();
    const newMap = new Map(liveReadings);
    newMap.delete(sensorId);
    
    set({ liveReadings: newMap });
  },
  
  clearAllReadings: () => set({ liveReadings: new Map() }),
}));
