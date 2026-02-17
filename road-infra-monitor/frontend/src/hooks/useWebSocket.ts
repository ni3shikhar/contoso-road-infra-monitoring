import { useEffect, useRef, useCallback } from 'react';
import websocketService from '@/services/websocket';
import { useAuthStore } from '@/store/authStore';

export function useWebSocket() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const connectedRef = useRef(false);

  useEffect(() => {
    if (isAuthenticated && !connectedRef.current) {
      websocketService.connect(
        () => {
          connectedRef.current = true;
        },
        (error) => {
          console.error('WebSocket error:', error);
          connectedRef.current = false;
        }
      );
    }

    return () => {
      if (connectedRef.current) {
        websocketService.disconnect();
        connectedRef.current = false;
      }
    };
  }, [isAuthenticated]);

  const subscribe = useCallback(<T,>(destination: string, callback: (message: T) => void) => {
    return websocketService.subscribe(destination, callback);
  }, []);

  const unsubscribe = useCallback((subscriptionId: string) => {
    websocketService.unsubscribe(subscriptionId);
  }, []);

  const send = useCallback((destination: string, body: unknown) => {
    websocketService.send(destination, body);
  }, []);

  return {
    subscribe,
    unsubscribe,
    send,
    isConnected: () => websocketService.isConnected(),
  };
}
