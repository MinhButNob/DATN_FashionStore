// composables/useWebSocket.js
import { ref } from "vue";

export function useWebSocket() {
  const socket = ref(null);
  const isConnected = ref(false);
  const reconnectAttempts = ref(0);
  const listeners = new Map();
  let reconnectTimer = null;
  let pingInterval = null;
  let intentionalDisconnect = false;
  let pendingIdentity = null;

  const sendRegister = () => {
    if (!pendingIdentity) {
      console.log("[WS] No pending identity to register");
      return;
    }

    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
      console.log(
        "[WS] Cannot register - socket not open, readyState:",
        socket.value?.readyState,
      );
      return;
    }

    const msg = JSON.stringify({
      type: "register",
      role: pendingIdentity.role,
      userId: pendingIdentity.userId,
    });

    try {
      socket.value.send(msg);
      console.log(
        "📇 [WS] Registered as",
        pendingIdentity.role,
        pendingIdentity.userId,
      );
    } catch (error) {
      console.error("❌ [WS] Failed to send register:", error);
    }
  };

  const identify = (role, userId) => {
    if (!role || !userId) {
      console.warn("⚠️ [WS] identify() thiếu role hoặc userId");
      return;
    }
    console.log("[WS] identify called with role:", role, "userId:", userId);
    pendingIdentity = { role: role.toUpperCase(), userId: Number(userId) };

    // Nếu socket đã mở, gửi register ngay
    if (socket.value && socket.value.readyState === WebSocket.OPEN) {
      sendRegister();
    } else {
      console.log("[WS] Socket not open yet, will register when connected");
    }
  };

  const connect = (path = "/ws/chat") => {
    console.log("[WS] connect() called with path:", path);

    if (socket.value && socket.value.readyState === WebSocket.OPEN) {
      console.log(
        "[WS] Already connected, readyState:",
        socket.value.readyState,
      );
      return socket.value;
    }

    if (socket.value && socket.value.readyState === WebSocket.CONNECTING) {
      console.log("[WS] Connection already in progress");
      return socket.value;
    }

    // ✅ Nếu đã thử quá nhiều lần, báo lỗi rõ ràng thay vì tiếp tục retry
    if (reconnectAttempts.value >= 5) {
      console.error(
        "❌ [WS] Đã thử kết nối 5 lần thất bại. Backend có thể chưa chạy.",
        "Kiểm tra Spring Boot đang chạy trên port 8080."
      );
      return null;
    }

    intentionalDisconnect = false;

    // ✅ Sử dụng port 8080
    const wsUrl = `ws://localhost:8080${path}`;
    console.log("🔗 Connecting to WebSocket:", wsUrl);

    try {
      socket.value = new WebSocket(wsUrl);
      console.log(
        "[WS] WebSocket instance created, readyState:",
        socket.value.readyState,
      );

      socket.value.onopen = () => {
        console.log(
          "✅ WebSocket connected, readyState:",
          socket.value.readyState,
        );
        isConnected.value = true;
        reconnectAttempts.value = 0; // ✅ Reset khi kết nối thành công

        window.ws = socket.value;

        // ✅ Gửi register ngay khi kết nối mở
        console.log("[WS] Sending register on open...");
        sendRegister();

        // Ping để giữ kết nối
        if (pingInterval) clearInterval(pingInterval);
        pingInterval = setInterval(() => {
          if (socket.value?.readyState === WebSocket.OPEN) {
            socket.value.send(JSON.stringify({ type: "ping" }));
          }
        }, 30000);
      };

      socket.value.onclose = (event) => {
        console.log("❌ WebSocket disconnected:", event.code, event.reason);
        isConnected.value = false;
        window.ws = null;

        if (pingInterval) {
          clearInterval(pingInterval);
          pingInterval = null;
        }

        if (intentionalDisconnect) {
          console.log("🔌 Đóng có chủ đích - không tự reconnect");
          return;
        }

        if (reconnectAttempts.value < 5) {
          // ✅ Exponential backoff: 3s, 6s, 9s, 12s, 15s
          const delay = 3000 * (reconnectAttempts.value + 1);
          console.log(
            `🔄 Reconnecting in ${delay}ms (attempt ${reconnectAttempts.value + 1}/5)`,
          );
          reconnectTimer = setTimeout(() => {
            reconnectAttempts.value++;
            connect(path);
          }, delay);
        } else {
          console.error(
            "❌ [WS] Đã thử kết nối 5 lần, dừng lại.",
            "Hãy đảm bảo Spring Boot backend đang chạy trên port 8080."
          );
        }
      };

      socket.value.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log("📨 WebSocket message:", data.type, data);

          const eventType = data.type || "message";
          const callbacks = listeners.get(eventType) || [];
          callbacks.forEach((callback) => callback(data));
        } catch (error) {
          console.error("Error parsing WebSocket message:", error);
        }
      };

      socket.value.onerror = (error) => {
        console.error(
          "⚠️ WebSocket error - Kiểm tra backend đang chạy trên localhost:8080:",
          error.type
        );
      };

      return socket.value;
    } catch (error) {
      console.error("❌ Failed to create WebSocket:", error);
      return null;
    }
  };

  const disconnect = () => {
    console.log("[WS] disconnect() called");
    intentionalDisconnect = true;

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
    pendingIdentity = null;
    window.ws = null;
    isConnected.value = false;
  };

  const send = (type, payload) => {
    if (socket.value?.readyState === WebSocket.OPEN) {
      const message = JSON.stringify({ type, payload });
      socket.value.send(message);
      console.log("📤 WebSocket sent:", { type, payload });
    } else {
      console.warn(
        "⚠️ WebSocket is not connected, readyState:",
        socket.value?.readyState,
      );
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

  return {
    socket,
    isConnected,
    connect,
    disconnect,
    send,
    onMessage,
    identify,
  };
}
