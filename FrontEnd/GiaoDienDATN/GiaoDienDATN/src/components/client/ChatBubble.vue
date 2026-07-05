<template>
  <div 
    class="chat-bubble-container" 
    ref="containerRef"
    :style="containerStyle"
  >
    <!-- Chat Button -->
    <button 
      class="chat-toggle-btn" 
      @click="toggleChat"
      @mousedown="startDrag"
      @touchstart="startDragTouch"
      :class="{ 'is-open': isOpen, 'dragging': isDragging }"
    >
      <i v-if="!isOpen" class="fas fa-comment-dots"></i>
      <i v-else class="fas fa-times"></i>
      <span v-if="unreadCount > 0" class="unread-badge">
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>

    <!-- Chat Window -->
    <div v-if="isOpen" class="chat-window" :class="{ 'is-minimized': isMinimized }">
      <!-- Header -->
      <div class="chat-header">
        <div class="chat-header-info">
          <div class="chat-avatar">
            <i class="fas fa-store"></i>
          </div>
          <div>
            <h4>Hỗ trợ khách hàng</h4>
            <p v-if="isConnected" class="online-status">
              <span class="status-dot"></span> Nhân viên đang hoạt động
            </p>
            <p v-else class="offline-status">
              <span class="status-dot offline"></span> Đang kết nối...
            </p>
          </div>
        </div>
        <div class="header-actions">
          <button class="minimize-btn" @click="isMinimized = !isMinimized">
            <i :class="isMinimized ? 'fas fa-window-maximize' : 'fas fa-window-minimize'"></i>
          </button>
          <button class="close-btn" @click="closeChat">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>

      <!-- Messages -->
      <div v-if="!isMinimized" class="chat-body">
        <div class="chat-messages" ref="messagesContainer">
          <div v-if="loadingMessages" class="loading-state">
            <i class="fas fa-spinner fa-spin"></i>
            <p>Đang tải tin nhắn...</p>
          </div>

          <div v-else-if="messages.length === 0" class="welcome-message">
            <div class="welcome-icon">💬</div>
            <h4>Chào bạn! 👋</h4>
            <p>Chúng tôi ở đây để hỗ trợ bạn. Hãy cho chúng tôi biết bạn cần gì nhé!</p>
            <div class="suggest-questions">
              <button class="suggest-btn" @click="sendQuickQuestion('Tôi muốn xem sản phẩm mới')">
                🥰 Sản phẩm mới
              </button>
              <button class="suggest-btn" @click="sendQuickQuestion('Tôi muốn kiểm tra đơn hàng')">
                📦 Kiểm tra đơn hàng
              </button>
              <button class="suggest-btn" @click="sendQuickQuestion('Tôi cần hỗ trợ đổi trả')">
                🔄 Đổi trả
              </button>
            </div>
          </div>

          <!-- Messages -->
          <div 
            v-for="message in messages" 
            :key="message.messageId"
            class="message-item"
            :class="{ 
              'user-message': message.isFromUser || message.userRole === 'CUSTOMER',
              'staff-message': message.userRole === 'STAFF' || message.userRole === 'ADMIN'
            }"
          >
            <div class="message-avatar">
              <i v-if="message.isFromUser || message.userRole === 'CUSTOMER'" class="fas fa-user"></i>
              <i v-else class="fas fa-headset"></i>
            </div>
            <div class="message-content-wrapper">
              <div class="message-content">
                <p>{{ message.messageContent || message.content }}</p>
              </div>
              <span class="message-time">{{ formatTime(message.sentDate) }}</span>
            </div>
          </div>
        </div>

        <!-- Input -->
        <div class="chat-input">
          <input 
            v-model="newMessage" 
            @keyup.enter="sendMessage"
            @input="checkProfanity"
            placeholder="Nhập tin nhắn..."
            :disabled="sending || !isConnected"
            ref="messageInput"
          />
          <span v-if="profanityWarning" class="profanity-warning">
            ⚠️ {{ profanityWarning }}
          </span>
          <button 
            class="send-btn" 
            @click="sendMessage"
            :disabled="sending || !newMessage.trim() || !isConnected || profanityWarning"
          >
            <i v-if="!sending" class="fas fa-paper-plane"></i>
            <i v-else class="fas fa-spinner fa-spin"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<!-- components/client/ChatBubble.vue -->
<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch, computed, reactive } from 'vue';
import { useChatStore } from '../../stores/chatStore';
import { useAuthStore } from '../../stores/authStore';

// ===== DANH SÁCH TỪ CẤM =====
const PROFANITY_LIST = [
  'địt', 'đụ', 'đéo', 'vãi', 'lồn', 'cặc', 'buồi', 'đĩ', 'đồ con', 'con mẹ',
  'mẹ kiếp', 'đồ khốn', 'khốn nạn', 'chó đẻ', 'chết tiệt', 'đồ ngu', 'ngu như',
  'óc chó', 'đồ điên', 'điên rồ', 'đồ rác', 'đồ bỏ', 'cút', 'cút đi',
  'mày', 'đồ chó', 'thằng chó', 'con chó', 'đồ lợn',
  'fuck', 'shit', 'damn', 'bitch', 'bastard', 'dick', 'pussy', 'asshole',
  'motherfucker', 'cunt', 'whore', 'slut', 'retard', 'stupid', 'idiot',
  'dumb', 'moron', 'trash', 'loser', 'suck', 'piss', 'crap',
  'chửi', 'thề', 'bậy', 'tục', 'thô tục',
];

const props = defineProps({
  customerId: {
    type: Number,
    default: null
  }
});

const emit = defineEmits(['message-sent', 'message-received']);

const chatStore = useChatStore();
const authStore = useAuthStore();

// State
const containerRef = ref(null);
const isOpen = ref(false);
const isMinimized = ref(false);
const messages = ref([]);
const newMessage = ref('');
const sending = ref(false);
const loadingMessages = ref(false);
const isConnected = ref(false);
const unreadCount = ref(0);
const chatId = ref(null);
const messagesContainer = ref(null);
const messageInput = ref(null);
const profanityWarning = ref('');

// Drag State
const isDragging = ref(false);
const dragStartX = ref(0);
const dragStartY = ref(0);
const position = reactive({ x: 0, y: 0 });

// Computed
const customerId = computed(() => {
  if (props.customerId) return props.customerId;
  const user = authStore.user || JSON.parse(localStorage.getItem('userInfo') || '{}');
  return user.customerId || user.userId || user.id || null;
});

const containerStyle = computed(() => {
  return {
    transform: `translate(${position.x}px, ${position.y}px)`,
    cursor: isDragging.value ? 'grabbing' : 'grab',
  };
});

// ===== PROFANITY CHECK =====
const checkProfanity = () => {
  const text = newMessage.value.toLowerCase();
  const foundWords = [];
  
  for (const word of PROFANITY_LIST) {
    if (text.includes(word.toLowerCase())) {
      foundWords.push(word);
    }
  }
  
  if (foundWords.length > 0) {
    profanityWarning.value = `Vui lòng không sử dụng từ ngữ không phù hợp: ${foundWords.join(', ')}`;
  } else {
    profanityWarning.value = '';
  }
};

const validateMessage = (text) => {
  const lowerText = text.toLowerCase();
  for (const word of PROFANITY_LIST) {
    if (lowerText.includes(word.toLowerCase())) {
      return { valid: false, word: word };
    }
  }
  return { valid: true };
};

// ===== DRAG FUNCTIONS =====
const startDrag = (e) => {
  if (e.button !== 0) return;
  if (e.target.closest('.chat-window')) return;
  
  isDragging.value = true;
  dragStartX.value = e.clientX - position.x;
  dragStartY.value = e.clientY - position.y;
  
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
  e.preventDefault();
};

const startDragTouch = (e) => {
  const touch = e.touches[0];
  if (e.target.closest('.chat-window')) return;
  
  isDragging.value = true;
  dragStartX.value = touch.clientX - position.x;
  dragStartY.value = touch.clientY - position.y;
  
  document.addEventListener('touchmove', onDragTouch, { passive: false });
  document.addEventListener('touchend', stopDragTouch);
  e.preventDefault();
};

const onDrag = (e) => {
  if (!isDragging.value) return;
  
  let newX = e.clientX - dragStartX.value;
  let newY = e.clientY - dragStartY.value;
  
  const container = containerRef.value;
  if (container) {
    const rect = container.getBoundingClientRect();
    const minX = -(rect.width * 0.5);
    const minY = -(rect.height * 0.5);
    const maxX = window.innerWidth - (rect.width * 0.5);
    const maxY = window.innerHeight - (rect.height * 0.5);
    
    newX = Math.max(minX, Math.min(newX, maxX));
    newY = Math.max(minY, Math.min(newY, maxY));
  }
  
  position.x = newX;
  position.y = newY;
};

const onDragTouch = (e) => {
  if (!isDragging.value) return;
  e.preventDefault();
  
  const touch = e.touches[0];
  let newX = touch.clientX - dragStartX.value;
  let newY = touch.clientY - dragStartY.value;
  
  const container = containerRef.value;
  if (container) {
    const rect = container.getBoundingClientRect();
    const minX = -(rect.width * 0.5);
    const minY = -(rect.height * 0.5);
    const maxX = window.innerWidth - (rect.width * 0.5);
    const maxY = window.innerHeight - (rect.height * 0.5);
    
    newX = Math.max(minX, Math.min(newX, maxX));
    newY = Math.max(minY, Math.min(newY, maxY));
  }
  
  position.x = newX;
  position.y = newY;
};

const stopDrag = () => {
  isDragging.value = false;
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
};

const stopDragTouch = () => {
  isDragging.value = false;
  document.removeEventListener('touchmove', onDragTouch);
  document.removeEventListener('touchend', stopDragTouch);
};

// ===== CHAT FUNCTIONS =====
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const dateObj = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  
  if (dateObj.getTime() === today.getTime()) {
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

const toggleChat = () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) {
    isMinimized.value = false;
    loadConversation();
    scrollToBottom();
  }
};

const closeChat = () => {
  isOpen.value = false;
  isMinimized.value = false;
};

// ===== LOAD CONVERSATION =====
const loadConversation = async () => {
  const id = customerId.value;
  if (!id) {
    alert('Vui lòng đăng nhập để chat với nhân viên');
    return;
  }

  try {
    loadingMessages.value = true;
    
    const conversations = await chatStore.fetchConversations(id);
    const conv = conversations.find(c => c.customerId === id);
    
    if (conv) {
      chatId.value = conv.chatId;
      await chatStore.selectConversation(conv);
      messages.value = chatStore.messages;
    } else {
      const response = await chatStore.createConversation(id);
      if (response) {
        chatId.value = response.chatId;
        await chatStore.fetchMessages(chatId.value);
        messages.value = chatStore.messages;
      }
    }
    scrollToBottom();
  } catch (error) {
    console.error('loadConversation error:', error);
    alert('Không thể tải cuộc trò chuyện. Vui lòng thử lại!');
  } finally {
    loadingMessages.value = false;
  }
};

// ===== SEND MESSAGE =====
const sendMessage = async () => {
  if (!newMessage.value.trim() || sending.value) return;
  
  const validation = validateMessage(newMessage.value);
  if (!validation.valid) {
    profanityWarning.value = `⚠️ Vui lòng không sử dụng từ ngữ không phù hợp: "${validation.word}"`;
    setTimeout(() => {
      profanityWarning.value = '';
    }, 3000);
    return;
  }

  const id = customerId.value;
  if (!id) {
    alert('Vui lòng đăng nhập để chat!');
    return;
  }

  if (!chatId.value) {
    await loadConversation();
    if (!chatId.value) {
      alert('Không thể tạo cuộc trò chuyện. Vui lòng thử lại!');
      return;
    }
  }

  const content = newMessage.value.trim();
  newMessage.value = '';
  profanityWarning.value = '';
  sending.value = true;

  try {
    const response = await chatStore.sendMessage(content);
    
    await chatStore.fetchMessages(chatId.value);
    messages.value = chatStore.messages;
    
    scrollToBottom();
    emit('message-sent', response);
  } catch (error) {
    console.error('sendMessage error:', error);
    alert(error.message || 'Không thể gửi tin nhắn');
  } finally {
    sending.value = false;
  }
};

const sendQuickQuestion = (question) => {
  newMessage.value = question;
  nextTick(() => {
    sendMessage();
  });
};

// ===== WATCHES =====
watch(
  () => chatStore.messages,
  (newMessages) => {
    if (newMessages.length >= 0) {
      messages.value = newMessages;
      scrollToBottom();
    }
  },
  { deep: true, immediate: true }
);

watch(
  () => chatStore.unreadCount,
  (newCount) => {
    unreadCount.value = newCount;
  }
);

watch(
  () => chatStore.isWebSocketConnected,
  (connected) => {
    isConnected.value = connected;
  }
);

// ===== LIFECYCLE =====
onMounted(() => {
  chatStore.initWebSocket();
  isConnected.value = chatStore.isWebSocketConnected;
  
  const savedPosition = localStorage.getItem('chatBubblePosition');
  if (savedPosition) {
    try {
      const pos = JSON.parse(savedPosition);
      position.x = pos.x || 0;
      position.y = pos.y || 0;
    } catch (e) {
      console.warn('Failed to load position:', e);
    }
  }
  
  const savedMessages = localStorage.getItem('chatMessages');
  if (savedMessages) {
    try {
      const parsed = JSON.parse(savedMessages);
      if (parsed && parsed.length > 0) {
        messages.value = parsed;
      }
    } catch (e) {
      console.warn('Failed to load messages:', e);
    }
  }
});

onUnmounted(() => {
  localStorage.setItem('chatBubblePosition', JSON.stringify({
    x: position.x,
    y: position.y
  }));
  
  if (messages.value.length > 0) {
    localStorage.setItem('chatMessages', JSON.stringify(messages.value));
  }
  
  chatStore.disconnectWebSocket();
});
</script>

<style scoped>
.chat-bubble-container {
  position: fixed;
  bottom: 30px;
  right: 30px;
  /* ===== TĂNG Z-INDEX ĐỂ LUÔN HIỂN THỊ TRÊN BANNER ===== */
  z-index: 99999;
  touch-action: none;
  user-select: none;
  transition: none;
  pointer-events: none;
}

.chat-toggle-btn {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  font-size: 24px;
  cursor: grab;
  box-shadow: 0 4px 20px rgba(230, 57, 70, 0.4);
  transition: all 0.3s;
  position: relative;
  touch-action: none;
  pointer-events: auto;
}

.chat-toggle-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 30px rgba(230, 57, 70, 0.5);
}

.chat-toggle-btn.dragging {
  cursor: grabbing;
  transform: scale(0.95);
  opacity: 0.8;
}

.chat-toggle-btn.is-open {
  background: linear-gradient(135deg, #2c3e50, #34495e);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.chat-toggle-btn.is-open:hover {
  background: linear-gradient(135deg, #1a252f, #2c3e50);
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #e63946;
  color: white;
  border-radius: 50%;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 600;
  min-width: 20px;
  text-align: center;
  animation: pulse 2s infinite;
  pointer-events: none;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.chat-window {
  position: absolute;
  bottom: 80px;
  right: 0;
  width: 400px;
  max-height: 600px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.3s ease;
  pointer-events: auto;
}

/* ... Các style khác giữ nguyên ... */
.chat-window.is-minimized {
  max-height: 70px;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px) scale(0.9); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.chat-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, #2c3e50, #34495e);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.chat-header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.chat-header-info h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.chat-header-info p {
  margin: 2px 0 0;
  font-size: 12px;
  opacity: 0.8;
}

.online-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #28a745;
  display: inline-block;
  animation: blink 1.5s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.status-dot.offline {
  background: #ffc107;
  animation: none;
}

.header-actions {
  display: flex;
  gap: 6px;
}

.minimize-btn,
.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.minimize-btn:hover,
.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.chat-body {
  display: flex;
  flex-direction: column;
  height: 520px;
}

.chat-messages {
  flex: 1;
  padding: 16px 20px;
  overflow-y: auto;
  background: #f8f9fa;
}

.loading-state,
.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  text-align: center;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.welcome-message h4 {
  color: #2c3e50;
  margin: 0 0 8px;
}

.welcome-message p {
  color: #666;
  font-size: 14px;
  max-width: 280px;
  margin-bottom: 16px;
}

.suggest-questions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  max-width: 280px;
}

.suggest-btn {
  padding: 10px 16px;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #2c3e50;
  text-align: left;
}

.suggest-btn:hover {
  border-color: #e63946;
  background: #fef5f5;
  transform: translateX(4px);
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  animation: fadeInMessage 0.3s ease;
}

@keyframes fadeInMessage {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e9ecef;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 14px;
  color: #666;
}

.user-message .message-avatar {
  background: #e63946;
  color: white;
}

.staff-message .message-avatar {
  background: #17a2b8;
  color: white;
}

.message-item.user-message {
  flex-direction: row-reverse;
}

.message-content-wrapper {
  max-width: 70%;
}

.user-message .message-content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-content {
  padding: 10px 14px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.user-message .message-content {
  background: #e63946;
  color: white;
  border-radius: 12px 12px 4px 12px;
}

.staff-message .message-content {
  background: white;
  border-radius: 12px 12px 12px 4px;
}

.message-content p {
  margin: 0;
  word-wrap: break-word;
  line-height: 1.5;
  font-size: 14px;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.chat-input {
  padding: 12px 16px;
  border-top: 1px solid #eee;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  background: white;
  flex-shrink: 0;
}

.chat-input input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
  transition: all 0.3s;
  min-width: 120px;
}

.chat-input input:focus {
  border-color: #e63946;
}

.chat-input input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.profanity-warning {
  width: 100%;
  color: #dc3545;
  font-size: 12px;
  padding: 4px 8px;
  background: #f8d7da;
  border-radius: 6px;
  animation: shake 0.3s ease;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  background: #e63946;
  color: white;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: #c82333;
  transform: scale(1.05);
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .chat-bubble-container {
    bottom: 15px;
    right: 15px;
  }
  
  .chat-toggle-btn {
    width: 50px;
    height: 50px;
    font-size: 20px;
  }
  
  .chat-window {
    width: calc(100vw - 30px);
    right: -15px;
    max-height: 80vh;
  }
  
  .chat-body {
    height: 400px;
  }
  
  .suggest-questions {
    max-width: 100%;
  }
  
  .chat-input {
    flex-direction: column;
    align-items: stretch;
  }
  
  .chat-input input {
    min-width: unset;
  }
  
  .send-btn {
    width: 100%;
    border-radius: 20px;
    height: 44px;
  }
}
</style>