// composables/useWebSocket.js
import { ref, onUnmounted } from 'vue';

/**
 * WebSocket Composable
 * Kết nối đến ws://localhost:8080/ws/chat
 */
export function useWebSocket() {
  const socket = ref(null);
  const isConnected = ref(false);
  const reconnectAttempts = ref(0);
  const listeners = new Map();
  let reconnectTimer = null;
  let pingInterval = null;

  const connect = (path = '/ws/chat') => {
    if (socket.value && socket.value.readyState === WebSocket.OPEN) {
      return socket.value;
    }

    const wsUrl = `ws://localhost:8080${path}`;
    console.log('🔗 Connecting to WebSocket:', wsUrl);

    try {
      socket.value = new WebSocket(wsUrl);

      socket.value.onopen = () => {
        isConnected.value = true;
        reconnectAttempts.value = 0;
        console.log('✅ WebSocket connected');

        // Gửi tin nhắn xác nhận
        socket.value.send(JSON.stringify({
          type: 'connection',
          message: 'Client connected'
        }));

        // Bắt đầu ping để giữ kết nối
        if (pingInterval) clearInterval(pingInterval);
        pingInterval = setInterval(() => {
          if (socket.value?.readyState === WebSocket.OPEN) {
            socket.value.send(JSON.stringify({ type: 'ping' }));
          }
        }, 30000);
      };

      socket.value.onclose = (event) => {
        isConnected.value = false;
        console.log('❌ WebSocket disconnected:', event.code, event.reason);

        if (pingInterval) {
          clearInterval(pingInterval);
          pingInterval = null;
        }

        if (reconnectAttempts.value < 5) {
          const delay = 3000 * (reconnectAttempts.value + 1);
          console.log(`🔄 Reconnecting in ${delay}ms (attempt ${reconnectAttempts.value + 1})`);
          reconnectTimer = setTimeout(() => {
            reconnectAttempts.value++;
            connect(path);
          }, delay);
        }
      };

      socket.value.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log('📨 WebSocket message:', data);

          const eventType = data.type || 'message';
          const callbacks = listeners.get(eventType) || [];
          callbacks.forEach(callback => callback(data));
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
        }
      };

      socket.value.onerror = (error) => {
        console.error('⚠️ WebSocket error:', error);
      };

      return socket.value;
    } catch (error) {
      console.error('❌ Failed to create WebSocket:', error);
      return null;
    }
  };

  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (pingInterval) {
      clearInterval(pingInterval);
      pingInterval = null;
    }
    if (socket.value) {
      socket.value.close();
      socket.value = null;
    }
    listeners.clear();
  };

  const send = (type, payload) => {
    if (socket.value?.readyState === WebSocket.OPEN) {
      const message = JSON.stringify({ type, payload });
      socket.value.send(message);
      console.log('📤 WebSocket sent:', { type, payload });
    } else {
      console.warn('⚠️ WebSocket is not connected');
    }
  };

  const onMessage = (eventType, callback) => {
    if (!listeners.has(eventType)) {
      listeners.set(eventType, []);
    }
    listeners.get(eventType).push(callback);

    return () => {
      const callbacks = listeners.get(eventType);
      if (callbacks) {
        const index = callbacks.indexOf(callback);
        if (index > -1) {
          callbacks.splice(index, 1);
        }
      }
    };
  };

  onUnmounted(() => {
    disconnect();
  });

  return {
    socket,
    isConnected,
    connect,
    disconnect,
    send,
    onMessage,
  };
}