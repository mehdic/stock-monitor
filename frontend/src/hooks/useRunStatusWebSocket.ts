import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { RunStatusUpdate } from '../types';

interface UseRunStatusWebSocketOptions {
  runId: string | null;
  onStatusUpdate?: (update: RunStatusUpdate) => void;
  autoConnect?: boolean;
}

interface UseRunStatusWebSocketReturn {
  statusUpdate: RunStatusUpdate | null;
  isConnected: boolean;
  error: string | null;
  connect: () => void;
  disconnect: () => void;
}

export default function useRunStatusWebSocket({
  runId,
  onStatusUpdate,
  autoConnect = true,
}: UseRunStatusWebSocketOptions): UseRunStatusWebSocketReturn {
  const [statusUpdate, setStatusUpdate] = useState<RunStatusUpdate | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);
  const subscriptionRef = useRef<any>(null);

  const disconnect = useCallback(() => {
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    if (clientRef.current) {
      console.log('Disconnecting run status WebSocket...');
      clientRef.current.deactivate();
      clientRef.current = null;
      setIsConnected(false);
    }
  }, []);

  const connect = useCallback(() => {
    if (!runId) {
      console.warn('Cannot connect run status WebSocket: no runId provided');
      return;
    }

    if (clientRef.current?.connected) {
      console.log('WebSocket already connected');
      return;
    }

    console.log(`Connecting to run status WebSocket for run ${runId}...`);
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
        console.log(`Run status WebSocket connected for run ${runId}`);
        setIsConnected(true);
        setError(null);

        // Subscribe to run's status channel
        const subscription = client.subscribe(`/topic/runs/${runId}/status`, (message) => {
          try {
            const update: RunStatusUpdate = JSON.parse(message.body);
            console.log('Received run status update:', update);

            setStatusUpdate(update);

            if (onStatusUpdate) {
              onStatusUpdate(update);
            }
          } catch (err) {
            console.error('Failed to parse run status update:', err);
            setError('Failed to parse status update');
          }
        });

        subscriptionRef.current = subscription;

        // Send subscription message to backend
        client.publish({
          destination: `/app/runs/${runId}/status/subscribe`,
          body: runId,
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        setError(`Connection error: ${frame.headers['message'] || 'Unknown error'}`);
        setIsConnected(false);
      },
      onWebSocketClose: () => {
        console.log('Run status WebSocket disconnected');
        setIsConnected(false);
      },
      onDisconnect: () => {
        console.log('Run status WebSocket disconnected');
        setIsConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;
  }, [runId, onStatusUpdate]);

  // Auto-connect on mount or when runId changes
  useEffect(() => {
    if (autoConnect && runId) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [autoConnect, runId, connect, disconnect]);

  return {
    statusUpdate,
    isConnected,
    error,
    connect,
    disconnect,
  };
}
