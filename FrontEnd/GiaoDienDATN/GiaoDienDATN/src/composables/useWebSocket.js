// composables/useWebSocket.js
import { ref } from 'vue';

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

  // Lưu lại thông tin identify để tự động register lại sau mỗi lần
  // reconnect (server KHÔNG nhớ register cũ qua các lần connect khác nhau)
  let pendingIdentity = null; // { role, userId }

  // FIX: đánh dấu khi disconnect() được gọi CÓ CHỦ ĐÍCH (component unmount),
  // để phân biệt với việc mất kết nối ngoài ý muốn (rớt mạng). Không có cờ
  // này, mỗi lần rời trang chat, việc đóng socket sẽ tự kích hoạt onclose,
  // và onclose vô điều kiện lên lịch reconnect sau ~3s -> tạo ra 1 kết nối
  // WS "ma" vẫn sống dù component đã unmount từ lâu, dùng closure cũ (còn
  // nhớ pendingIdentity cũ) tự động register lại và tiếp tục nhận/xử lý
  // message dội vào store/component đã không còn tồn tại - đây chính là
  // nguyên nhân crash storeToRefs khi quay lại trang chat lần 2.
  let intentionalDisconnect = false;

  const sendRegister = () => {
    if (!pendingIdentity || socket.value?.readyState !== WebSocket.OPEN) return;
    socket.value.send(JSON.stringify({
      type: 'register',
      role: pendingIdentity.role,
      userId: pendingIdentity.userId
    }));
    console.log('📇 [WS] Registered as', pendingIdentity.role, pendingIdentity.userId);
  };

  /**
   * Đăng ký danh tính cho session hiện tại - BẮT BUỘC phải gọi ngay sau connect(),
   * nếu không server sẽ không gửi bất kỳ sự kiện chat nào cho session này.
   * role: 'CUSTOMER' | 'STAFF', userId: user.userId (không phải customerId/employeeId)
   */
  const identify = (role, userId) => {
    if (!role || !userId) {
      console.warn('⚠️ [WS] identify() thiếu role hoặc userId');
      return;
    }
    pendingIdentity = { role, userId: Number(userId) };
    sendRegister();
  };

  const connect = (path = '/ws/chat') => {
    if (socket.value && socket.value.readyState === WebSocket.OPEN) {
      return socket.value;
    }

    // FIX: mỗi lần chủ động connect (kể cả reconnect tự động), coi như
    // không còn ở trạng thái "đã đóng có chủ đích" nữa.
    intentionalDisconnect = false;

    const wsUrl = `ws://localhost:8080${path}`;
    console.log('🔗 Connecting to WebSocket:', wsUrl);

    try {
      socket.value = new WebSocket(wsUrl);

      socket.value.onopen = () => {
        isConnected.value = true;
        reconnectAttempts.value = 0;
        console.log('✅ WebSocket connected');

        // Nếu đã từng identify trước đó (lần connect đầu, hoặc trước khi bị
        // reconnect) thì tự động register lại ngay - server coi mỗi lần
        // connect là 1 session mới, không nhớ register của lần trước.
        sendRegister();

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

        // FIX: KHÔNG reconnect nếu đây là lần đóng có chủ đích (component
        // unmount / rời trang) - tránh tạo kết nối "ma" tự sống lại sau vài
        // giây dù không còn ai cần nó nữa.
        if (intentionalDisconnect) {
          console.log('🔌 Đóng có chủ đích - không tự reconnect');
          return;
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
    // FIX: đánh dấu TRƯỚC khi đóng socket, để onclose (sẽ chạy async ngay
    // sau đây) biết đây là chủ đích, không tự reconnect.
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

    // FIX: xoá luôn identity đã lưu - nếu không, lỡ có gì đó vẫn giữ tham
    // chiếu tới closure này và gọi connect() lại, nó sẽ tự register lại
    // bằng danh tính cũ dù không còn hợp lệ.
    pendingIdentity = null;
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

  // FIX: KHÔNG tự đăng ký onUnmounted ở đây nữa.
  //
  // Lý do: useWebSocket() được gọi từ BÊN TRONG 1 Pinia action
  // (chatStore.initWebSocket()), không phải trực tiếp trong setup() của
  // component. onUnmounted() chỉ an toàn khi gọi trực tiếp trong ngữ cảnh
  // component - gọi nó gián tiếp qua 1 tầng store action như vậy khiến hook
  // bị đăng ký nhầm vào effect scope của component đang active, và khi
  // component đó unmount, Vue dừng (stop) luôn cả các computed getter nội
  // bộ của Pinia store (bị lây nhầm scope) -> lần mount tiếp theo,
  // storeToRefs() cố dùng lại getter đã "chết" -> crash
  // "Cannot read properties of null (reading 'effect')".
  //
  // Việc cleanup (gọi disconnect()) đã được component tự làm tường minh
  // trong onUnmounted của chính nó rồi (xem chatStore.disconnectWebSocket()
  // được StaffChat.vue gọi trong onUnmounted), nên hook này thực ra thừa
  // và có hại - bỏ hẳn.

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