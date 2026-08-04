<template>
  <div class="chat-type-selector">
    <div class="selector-header">
      <i class="fas fa-headset"></i>
      <h3>Chọn loại tư vấn</h3>
      <p>Chúng tôi sẽ kết nối bạn với nhân viên phù hợp nhất</p>
    </div>

    <div class="type-grid">
      <button
        v-for="type in chatTypes"
        :key="type.value"
        class="type-btn"
        :class="{ selected: selectedType === type.value }"
        @click="selectType(type.value)"
      >
        <i :class="getTypeIcon(type.value)"></i>
        <div class="type-info">
          <span class="type-label">{{ type.label }}</span>
          <span class="type-desc">{{ getTypeDescription(type.value) }}</span>
        </div>
        <span v-if="selectedType === type.value" class="check-mark">
          <i class="fas fa-check-circle"></i>
        </span>
      </button>
    </div>

    <div class="selector-actions">
      <button
        class="start-chat-btn"
        :disabled="!selectedType || isLoading"
        @click="startChat"
      >
        <i v-if="!isLoading" class="fas fa-paper-plane"></i>
        <i v-else class="fas fa-spinner fa-spin"></i>
        {{ isLoading ? 'Đang kết nối...' : 'Bắt đầu chat' }}
      </button>
      <button class="skip-btn" @click="$emit('close')">
        <i class="fas fa-times"></i>
      </button>
    </div>

    <div class="selector-footer">
      <p>
        <i class="fas fa-shield-alt"></i>
        Thông tin của bạn được bảo mật
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useChatStore } from '../../stores/chatStore';
import { useNotification } from '../../composables/useNotification';
import chatService from '../../services/chatService';

const emit = defineEmits(['chat-started', 'close']);

const chatStore = useChatStore();
const { showNotification } = useNotification();

const chatTypes = ref([]);
const selectedType = ref(null);
const isLoading = ref(false);

const TYPE_ICONS = {
  'RETURN': 'fas fa-undo-alt',
  'PRODUCT_CONSULTATION': 'fas fa-shopping-bag',
  'RETURN_ONLINE': 'fas fa-laptop',
  'RETURN_STORE': 'fas fa-store',
  'OTHER': 'fas fa-comment'
};

const TYPE_DESCRIPTIONS = {
  'RETURN': 'Hỗ trợ quy trình hoàn hàng, đổi trả sản phẩm',
  'PRODUCT_CONSULTATION': 'Tư vấn sản phẩm, giá cả, chính sách bán hàng',
  'RETURN_ONLINE': 'Hỗ trợ hoàn hàng cho đơn hàng mua online',
  'RETURN_STORE': 'Hỗ trợ hoàn hàng cho đơn hàng mua tại cửa hàng',
  'OTHER': 'Các vấn đề khác chưa được liệt kê'
};

const getTypeIcon = (value) => {
  return TYPE_ICONS[value] || 'fas fa-comment';
};

const getTypeDescription = (value) => {
  return TYPE_DESCRIPTIONS[value] || '';
};

const fetchChatTypes = async () => {
  try {
    const response = await chatService.getChatTypes();
    chatTypes.value = response || [];
    
    // Nếu chỉ có 1 loại, tự động chọn
    if (chatTypes.value.length === 1) {
      selectedType.value = chatTypes.value[0].value;
    }
  } catch (error) {
    console.error('Failed to fetch chat types:', error);
    showNotification('Không thể tải danh sách loại tư vấn', 'error');
  }
};

const selectType = (type) => {
  selectedType.value = type;
};

// Trong ChatTypeSelector.vue - method startChat
const startChat = async () => {
  if (!selectedType.value) {
    showNotification('Vui lòng chọn loại tư vấn', 'warning');
    return;
  }

  let userId = chatStore.currentCustomerId;
  if (!userId) {
    const user = JSON.parse(localStorage.getItem('userInfo') || '{}');
    userId = user?.customerId || user?.userId || user?.id;
  }
  if (!userId) {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    userId = user?.customerId || user?.userId || user?.id;
  }
  if (!userId) {
    showNotification('Vui lòng đăng nhập để chat', 'warning');
    return;
  }

  isLoading.value = true;

  try {
    console.log('📤 [ChatTypeSelector] Creating chat with type:', selectedType.value, 'userId:', userId);
    
    const chat = await chatService.requestStaffChat(userId, null, selectedType.value);
    
    console.log('📥 [ChatTypeSelector] Chat response:', chat);
    
    if (chat && chat.chatId) {
      // Cập nhật store
      const existingIdx = chatStore.conversations.findIndex(c => c.chatId === chat.chatId);
      if (existingIdx >= 0) {
        chatStore.conversations[existingIdx] = chat;
      } else {
        chatStore.conversations.unshift(chat);
      }
      chatStore.selectedConversation = chat;
      chatStore.currentCustomerId = userId;
      
      console.log('✅ [ChatTypeSelector] Emitting chat-started with chatId:', chat.chatId);
      emit('chat-started', chat);
      showNotification('Đã kết nối với nhân viên hỗ trợ', 'success');
    } else {
      console.error('❌ [ChatTypeSelector] No chat data received');
      throw new Error('Không nhận được dữ liệu chat từ server');
    }
  } catch (error) {
    console.error('❌ [ChatTypeSelector] Failed to start chat:', error);
    showNotification(error.message || 'Không thể bắt đầu chat. Vui lòng thử lại!', 'error');
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchChatTypes();
});
</script>

<style scoped>
.chat-type-selector {
  padding: 24px;
  background: white;
  border-radius: 16px;
  max-width: 480px;
  width: 100%;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.12);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.selector-header {
  text-align: center;
  margin-bottom: 24px;
}

.selector-header i {
  font-size: 40px;
  color: #667eea;
  display: block;
  margin-bottom: 10px;
  background: #eef0ff;
  padding: 12px;
  border-radius: 50%;
  width: 64px;
  height: 64px;
  margin-left: auto;
  margin-right: auto;
}

.selector-header h3 {
  margin: 0 0 4px 0;
  color: #2d3748;
  font-size: 20px;
  font-weight: 700;
}

.selector-header p {
  margin: 0;
  color: #718096;
  font-size: 14px;
}

.type-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.type-grid::-webkit-scrollbar {
  width: 4px;
}

.type-grid::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 10px;
}

.type-btn {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  position: relative;
}

.type-btn:hover {
  border-color: #667eea;
  background: #f7f8fc;
  transform: translateX(4px);
}

.type-btn.selected {
  border-color: #667eea;
  background: #eef0ff;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.type-btn i:first-child {
  font-size: 22px;
  color: #667eea;
  width: 36px;
  text-align: center;
  flex-shrink: 0;
}

.type-info {
  flex: 1;
  min-width: 0;
}

.type-label {
  font-weight: 600;
  color: #2d3748;
  font-size: 14px;
  display: block;
}

.type-desc {
  font-size: 12px;
  color: #718096;
  margin-top: 2px;
  display: block;
}

.check-mark {
  color: #667eea;
  font-size: 20px;
  flex-shrink: 0;
  animation: popIn 0.3s ease;
}

@keyframes popIn {
  from { transform: scale(0.5); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.selector-actions {
  display: flex;
  gap: 12px;
}

.start-chat-btn {
  flex: 1;
  padding: 14px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.start-chat-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
}

.start-chat-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.skip-btn {
  padding: 14px 18px;
  background: #f5f5f5;
  color: #999;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 16px;
}

.skip-btn:hover {
  background: #eee;
  color: #666;
}

.selector-footer {
  text-align: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.selector-footer p {
  margin: 0;
  color: #a0aec0;
  font-size: 12px;
}

.selector-footer p i {
  margin-right: 6px;
  color: #48bb78;
}

/* Responsive */
@media (max-width: 480px) {
  .chat-type-selector {
    padding: 16px;
    max-width: 100%;
  }
  
  .selector-header i {
    font-size: 32px;
    width: 56px;
    height: 56px;
    padding: 10px;
  }
  
  .selector-header h3 {
    font-size: 18px;
  }
  
  .type-btn {
    padding: 12px 14px;
  }
  
  .type-btn i:first-child {
    font-size: 18px;
    width: 30px;
  }
}
</style>