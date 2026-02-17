import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/store/authStore';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;

  connect(onConnect?: () => void, onError?: (error: Error) => void): void {
    const token = useAuthStore.getState().accessToken;

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log('[WS]', str);
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('WebSocket connected');
        this.reconnectAttempts = 0;
        onConnect?.();
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
        onError?.(new Error(frame.headers['message']));
      },
      onWebSocketClose: () => {
        console.log('WebSocket closed');
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
        }
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    this.client?.deactivate();
    this.client = null;
  }

  subscribe<T>(destination: string, callback: (message: T) => void): string {
    if (!this.client?.connected) {
      console.warn('WebSocket not connected');
      return '';
    }

    const subscriptionId = `sub-${Date.now()}`;
    const subscription = this.client.subscribe(destination, (message) => {
      try {
        const data = JSON.parse(message.body) as T;
        callback(data);
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    });

    this.subscriptions.set(subscriptionId, subscription);
    return subscriptionId;
  }

  unsubscribe(subscriptionId: string): void {
    const subscription = this.subscriptions.get(subscriptionId);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(subscriptionId);
    }
  }

  send(destination: string, body: unknown): void {
    if (!this.client?.connected) {
      console.warn('WebSocket not connected');
      return;
    }
    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }

  isConnected(): boolean {
    return this.client?.connected ?? false;
  }
}

export const websocketService = new WebSocketService();
export default websocketService;
