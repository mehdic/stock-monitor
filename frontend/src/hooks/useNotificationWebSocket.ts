import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Notification } from '../types';

interface UseNotificationWebSocketOptions {
  userId: string;
  onNotificationReceived?: (notification: Notification) => void;
  autoConnect?: boolean;
}

interface UseNotificationWebSocketReturn {
  notifications: Notification[];
  isConnected: boolean;
  error: string | null;
  connect: () => void;
  disconnect: () => void;
  acknowledgeNotification: (notificationId: string) => void;
  clearNotifications: () => void;
}

export default function useNotificationWebSocket({
  userId,
  onNotificationReceived,
  autoConnect = true,
}: UseNotificationWebSocketOptions): UseNotificationWebSocketReturn {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);

  const connect = useCallback(() => {
    if (clientRef.current?.connected) {
      console.log('WebSocket already connected');
      return;
    }

    console.log('Connecting to notification WebSocket...');
    setError(null);

    const client = new Client({
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'),
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log('STOMP:', str);
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        console.log('Notification WebSocket connected');
        setIsConnected(true);
        setError(null);

        // Subscribe to user's notification channel
        client.subscribe(`/user/${userId}/notifications`, (message) => {
          try {
            const notification: Notification = JSON.parse(message.body);
            console.log('Received notification:', notification);

            setNotifications((prev) => [notification, ...prev]);

            if (onNotificationReceived) {
              onNotificationReceived(notification);
            }
          } catch (err) {
            console.error('Failed to parse notification message:', err);
            setError('Failed to parse notification');
          }
        });

        // Send subscription message to backend
        client.publish({
          destination: '/app/notifications/subscribe',
          body: userId,
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        setError(`Connection error: ${frame.headers['message'] || 'Unknown error'}`);
        setIsConnected(false);
      },
      onWebSocketClose: () => {
        console.log('Notification WebSocket disconnected');
        setIsConnected(false);
      },
      onDisconnect: () => {
        console.log('Notification WebSocket disconnected');
        setIsConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;
  }, [userId, onNotificationReceived]);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      console.log('Disconnecting notification WebSocket...');
      clientRef.current.deactivate();
      clientRef.current = null;
      setIsConnected(false);
    }
  }, []);

  const acknowledgeNotification = useCallback(
    (notificationId: string) => {
      if (clientRef.current?.connected) {
        clientRef.current.publish({
          destination: '/app/notifications/ack',
          body: notificationId,
        });
      }
    },
    []
  );

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  // Auto-connect on mount if enabled
  useEffect(() => {
    if (autoConnect) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [autoConnect, connect, disconnect]);

  return {
    notifications,
    isConnected,
    error,
    connect,
    disconnect,
    acknowledgeNotification,
    clearNotifications,
  };
}
