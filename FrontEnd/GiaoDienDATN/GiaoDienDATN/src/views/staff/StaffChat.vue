<template>
  <div class="staff-chat">
    <aside class="conversation-list">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input 
          v-model="search" 
          type="text" 
          placeholder="Tìm cuộc trò chuyện" 
          @input="handleSearch"
        />
      </div>
      <div v-if="loadingConversations" class="loading-state">
        <i class="fas fa-spinner fa-spin"></i>
        <p>Đang tải danh sách...</p>
      </div>
      <div v-else-if="filteredConversations.length === 0" class="empty-state">
        <i class="fas fa-comments"></i>
        <p>{{ search ? 'Không tìm thấy' : 'Không có cuộc trò chuyện nào' }}</p>
      </div>
      <button
        v-for="conversation in filteredConversations"
        :key="conversation.chatId"
        class="conversation-item"
        :class="{ 
          active: selectedConversation?.chatId === conversation.chatId,
          closed: conversation.status === 'CLOSED',
          'has-unread': conversation.unreadCount > 0
        }"
        @click="selectConversation(conversation)"
      >
        <div class="avatar">{{ getInitial(conversation.customerName) }}</div>
        <div class="conversation-info">
          <div class="conversation-top">
            <strong>{{ conversation.customerName }}</strong>
            <span>{{ formatTime(conversation.createdAt) }}</span>
          </div>
          <p>{{ conversation.lastMessage?.messageContent || 'Chưa có tin nhắn' }}</p>
          <!-- FIX: hiện rõ ai đang phụ trách - tránh nhân viên khác tưởng nhầm mình
               nhắn được vào chat này -->
          <p v-if="conversation.status === 'ACTIVE' && conversation.adminName" class="assigned-label">
            👤 {{ conversation.adminName }} đang phụ trách
          </p>
        </div>
        <span v-if="conversation.unreadCount > 0" class="unread-dot">
          {{ conversation.unreadCount > 99 ? '99+' : conversation.unreadCount }}
        </span>
      </button>
    </aside>
    <section class="chat-panel" v-if="selectedConversation">
      <div class="chat-header">
        <div class="avatar">{{ getInitial(selectedConversation.customerName) }}</div>
        <div class="header-info">
          <h3>{{ selectedConversation.customerName }}</h3>
          <p>
            <span class="status-dot" :class="selectedConversation.status === 'ACTIVE' ? 'online' : 'offline'">
              {{ selectedConversation.status === 'ACTIVE' ? '🟢 Đang hoạt động' : '🔴 Đã đóng' }}
            </span>
          </p>
        </div>
        <div class="header-actions">
          <button 
            v-if="selectedConversation.status === 'ACTIVE' && isOwnChat" 
            class="close-chat-btn" 
            @click="closeConversation"
            title="Đóng cuộc trò chuyện"
          >
            <i class="fas fa-times"></i>
          </button>
          <button 
            class="refresh-btn" 
            @click="refreshMessages"
            title="Làm mới"
            :disabled="refreshing"
          >
            <i class="fas fa-sync" :class="{ 'fa-spin': refreshing }"></i>
          </button>
        </div>
      </div>
      <div class="messages" ref="messagesContainer">
        <div v-if="loadingMessages" class="loading-state">
          <i class="fas fa-spinner fa-spin"></i>
          <p>Đang tải tin nhắn...</p>
        </div>
        
        <div v-else-if="messages.length === 0" class="empty-state">
          <i class="fas fa-comment-dots"></i>
          <p>Chưa có tin nhắn nào</p>
        </div>
        <div 
          v-for="message in messages" 
          :key="message.messageId" 
          class="message-row" 
          :class="[message.userRole === 'CUSTOMER' ? 'customer' : 'staff']"
        >
          <div class="message-bubble">
            <div class="message-content">
              <p>{{ message.messageContent }}</p>
            </div>
            <div class="message-footer">
              <span>{{ formatTime(message.sentDate) }}</span>
              <i v-if="message.userRole === 'STAFF' && message.isRead" class="fas fa-check-double"></i>
            </div>
          </div>
        </div>
      </div>
      <!-- FIX: banner cảnh báo khi chat đang được nhân viên KHÁC phụ trách -->
      <div v-if="selectedConversation.status === 'ACTIVE' && !isOwnChat" class="assigned-banner">
        🔒 Cuộc trò chuyện này đang được <strong>{{ selectedConversation.adminName || 'nhân viên khác' }}</strong> phụ trách. Bạn chỉ có thể xem, không thể trả lời.
      </div>
      <div class="reply-box" v-if="selectedConversation.status === 'ACTIVE' && isOwnChat">
        <input 
          v-model="reply" 
          type="text" 
          placeholder="Nhập tin nhắn trả lời..." 
          @keyup.enter="sendReply"
          :disabled="sending"
          ref="replyInput"
        />
        <button 
          class="send-btn" 
          :disabled="sending || !reply.trim()" 
          @click="sendReply"
        >
          <i v-if="!sending" class="fas fa-paper-plane"></i>
          <i v-else class="fas fa-spinner fa-spin"></i>
        </button>
      </div>
      
      <div v-else class="chat-closed-message">
        <i class="fas fa-lock"></i>
        <p>Cuộc trò chuyện đã được đóng</p>
        <button class="reopen-btn" @click="reopenConversation">
          <i class="fas fa-undo"></i> Mở lại
        </button>
      </div>
    </section>
    <section class="customer-panel" v-if="selectedConversation">
      <div class="customer-info">
        <h3>Thông tin khách hàng</h3>
        <div class="info-row">
          <span><i class="fas fa-user"></i> Tên</span>
          <strong>{{ selectedConversation.customerName || 'Chưa cập nhật' }}</strong>
        </div>
        <div class="info-row">
          <span><i class="fas fa-envelope"></i> Email</span>
          <strong>{{ selectedConversation.customerEmail || 'Chưa cập nhật' }}</strong>
        </div>
        <div class="info-row">
          <span><i class="fas fa-phone"></i> Số điện thoại</span>
          <strong>{{ selectedConversation.phone || 'Chưa cập nhật' }}</strong>
        </div>
        <div class="info-row">
          <span><i class="fas fa-clock"></i> Bắt đầu</span>
          <strong>{{ formatTime(selectedConversation.createdAt) }}</strong>
        </div>
      </div>
      <div class="quick-templates">
        <h3>Gợi ý phản hồi</h3>
        <button 
          v-for="template in templates" 
          :key="template" 
          class="template-btn" 
          @click="reply = template; focusReply()"
          :disabled="selectedConversation.status !== 'ACTIVE'"
        >
          <i class="fas fa-comment"></i>
          {{ template }}
        </button>
      </div>
    </section>
  </div>
</template>
<!-- components/staff/StaffChat.vue -->
<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useChatStore } from '../../stores/chatStore';
import chatStaffService from '../../services/chatStaffService';
import { useNotificationNhanVien } from '../../composables/useNotificationNhanVien';
import { useAuthStore } from '../../stores/authStore';
const chatStore = useChatStore();
const authStore = useAuthStore();
const { successWithIcon, showApiError, error } = useNotificationNhanVien();
const {
  conversations,
  selectedConversation,
  messages,
  loading,
  searchKeyword,
  unreadCount,
} = storeToRefs(chatStore);
const reply = ref('');
const search = ref('');
const messagesContainer = ref(null);
const replyInput = ref(null);
const refreshing = ref(false);
const sending = ref(false);
const loadingConversations = ref(false);
const loadingMessages = ref(false);
const templates = [
  "Dạ shop đang kiểm tra và phản hồi mình ngay ạ.",
  "Dạ sản phẩm này hiện còn hàng tại cửa hàng.",
  "Dạ mình cho shop xin mã đơn hàng để kiểm tra nhanh hơn ạ.",
  "Dạ shop xin lỗi vì sự chậm trễ ạ.",
  "Dạ mình vui lòng cung cấp thêm thông tin để shop hỗ trợ tốt hơn ạ.",
];
const filteredConversations = computed(() => {
  const keyword = search.value.toLowerCase().trim();
  if (!keyword) return conversations.value;
  
  return conversations.value.filter(item => 
    item.customerName?.toLowerCase().includes(keyword) ||
    item.phone?.includes(keyword)
  );
});
const getInitial = (name) => {
  return name ? name.charAt(0).toUpperCase() : '?';
};
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const dateObj = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  
  if (dateObj.getTime() === today.getTime()) {
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
};
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};
const focusReply = () => {
  nextTick(() => {
    if (replyInput.value) {
      replyInput.value.focus();
    }
  });
};
// ===== LOAD CONVERSATIONS =====
const loadConversations = async () => {
  loadingConversations.value = true;
  try {
    const response = await chatStaffService.getAllActiveChats();
    conversations.value = response || [];
    
    await chatStore.updateUnreadCount();
    
    if (conversations.value.length > 0 && !selectedConversation.value) {
      const activeConv = conversations.value.find(c => c.status === 'ACTIVE');
      await selectConversation(activeConv || conversations.value[0]);
    }
  } catch (error) {
    console.error('loadConversations error:', error);
    showApiError(error, 'Không thể tải danh sách cuộc trò chuyện');
  } finally {
    loadingConversations.value = false;
  }
};
// ===== SELECT CONVERSATION =====
const selectConversation = async (conversation) => {
  if (!conversation) return;
  loadingMessages.value = true;
  
  try {
    // Lấy thông tin mới nhất từ server
    const response = await chatStaffService.getAllActiveChats();
    const updatedConv = response.find(c => c.chatId === conversation.chatId);
    const convToUse = updatedConv || conversation;
    
    chatStore.selectedConversation = convToUse;
    
    const staffId = authStore.user?.userId || 
                    JSON.parse(localStorage.getItem('userInfo') || '{}').userId;
    if (staffId) {
      await chatStaffService.markAsRead(convToUse.chatId, staffId);
    }
    
    convToUse.unreadCount = 0;
    
    await chatStore.fetchMessages(convToUse.chatId);
    messages.value = chatStore.messages;
    scrollToBottom();
    focusReply();
  } catch (error) {
    console.error('selectConversation error:', error);
    showApiError(error, 'Không thể tải tin nhắn');
  } finally {
    loadingMessages.value = false;
  }
};
// Helper dùng chung: lấy đúng userId của staff đang đăng nhập (user.userId)
const getStaffId = () => {
  let staffId = authStore.user?.userId || authStore.user?.id || authStore.user?.user_id;
  if (!staffId) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    staffId = userInfo.userId || userInfo.id || userInfo.user_id || userInfo.staffId;
  }
  if (!staffId) {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    staffId = user.userId || user.id || user.user_id;
  }
  return staffId ? Number(staffId) : null;
};
// FIX: chat có thuộc về staff đang đăng nhập hay không - dùng để khoá ô nhập
// và hiện banner cảnh báo NGAY trên UI, thay vì để staff gõ xong mới bị BE từ
// chối (bug "2 nhân viên tưởng nhắn được vào cùng 1 chat" trước đây).
// selectedConversation.adminId = chat.admin.userId (đã xác nhận đúng là user.userId).
const isOwnChat = computed(() => {
  const conv = selectedConversation.value;
  if (!conv) return false;
  if (conv.status !== 'ACTIVE') return true; // chat WAITING/CLOSED không áp dụng khoá này
  const staffId = getStaffId();
  return !staffId || !conv.adminId || Number(conv.adminId) === Number(staffId);
});
// ===== SEND REPLY =====
const sendReply = async () => {
  if (!reply.value.trim() || sending.value || selectedConversation.value?.status !== 'ACTIVE') return;
  // FIX: chặn gửi ngay từ UI nếu chat này không phải của mình, tránh gọi API
  // rồi mới nhận lỗi 400 khó hiểu.
  if (!isOwnChat.value) {
    error(`Chat này đang được nhân viên ${selectedConversation.value?.adminName || 'khác'} phụ trách, bạn không thể trả lời.`);
    return;
  }
  const content = reply.value.trim();
  reply.value = '';
  sending.value = true;
  try {
    const staffId = getStaffId();
    if (!staffId) {
      error('Không tìm thấy thông tin nhân viên. Vui lòng đăng nhập lại!');
      reply.value = content;
      return;
    }
    
    console.log('📤 Sending message with staffId:', staffId);
    
    const response = await chatStaffService.sendMessage(
      selectedConversation.value.chatId,
      Number(staffId),
      content
    );
    
    console.log('✅ Message sent successfully:', response);
    
    // Fetch lại messages
    await chatStore.fetchMessages(selectedConversation.value.chatId);
    messages.value = chatStore.messages;
    
    const conv = conversations.value.find(c => c.chatId === selectedConversation.value.chatId);
    if (conv) {
      conv.lastMessage = {
        messageContent: content,
        sentDate: new Date().toISOString()
      };
      conv.unreadCount = 0;
    }
    
    scrollToBottom();
    successWithIcon('Đã gửi tin nhắn thành công', '📨');
  } catch (error) {
    console.error('❌ sendReply error:', error);
    
    let errorMsg = 'Không thể gửi tin nhắn';
    if (error.response?.data?.message) {
      errorMsg = error.response.data.message;
    } else if (error.response?.data) {
      const errors = Object.values(error.response.data);
      if (errors.length > 0) {
        errorMsg = errors.join(', ');
      }
    } else if (error.message) {
      errorMsg = error.message;
    }
    
    showApiError(error, errorMsg);
    reply.value = content;
  } finally {
    sending.value = false;
  }
};
// ===== CLOSE CONVERSATION =====
const closeConversation = async () => {
  if (!selectedConversation.value) return;
  const staffId = getStaffId();
  if (!staffId) {
    error('Không tìm thấy thông tin nhân viên. Vui lòng đăng nhập lại!');
    return;
  }
  if (confirm('Bạn có chắc muốn đóng cuộc trò chuyện này?')) {
    try {
      // FIX: bản cũ thiếu staffUserId -> URL bị lỗi /close/undefined
      await chatStaffService.closeChat(selectedConversation.value.chatId, staffId);
      successWithIcon('Đã đóng cuộc trò chuyện', '🔒');
      await loadConversations();
    } catch (error) {
      console.error('closeConversation error:', error);
      showApiError(error, 'Không thể đóng cuộc trò chuyện');
    }
  }
};
// ===== REOPEN CONVERSATION =====
const reopenConversation = async () => {
  if (!selectedConversation.value) return;
  const staffId = getStaffId();
  if (!staffId) {
    error('Không tìm thấy thông tin nhân viên. Vui lòng đăng nhập lại!');
    return;
  }
  try {
    // FIX: bản cũ gọi nhầm closeChat() thay vì reopenChat(), và thiếu staffUserId
    await chatStaffService.reopenChat(selectedConversation.value.chatId, staffId);
    await loadConversations();
    successWithIcon('Đã mở lại cuộc trò chuyện', '🔓');
  } catch (error) {
    showApiError(error, 'Không thể mở lại cuộc trò chuyện');
  }
};
// ===== REFRESH MESSAGES =====
const refreshMessages = async () => {
  if (refreshing.value || !selectedConversation.value) return;
  
  refreshing.value = true;
  try {
    await chatStore.fetchMessages(selectedConversation.value.chatId);
    messages.value = chatStore.messages;
    
    // Cập nhật thông tin customer
    const response = await chatStaffService.getAllActiveChats();
    const updatedConv = response.find(c => c.chatId === selectedConversation.value.chatId);
    if (updatedConv) {
      Object.assign(selectedConversation.value, updatedConv);
    }
    
    successWithIcon('Đã làm mới tin nhắn', '🔄');
  } catch (error) {
    console.error('refreshMessages error:', error);
    showApiError(error, 'Không thể làm mới tin nhắn');
  } finally {
    refreshing.value = false;
  }
};
// ===== SEARCH =====
const handleSearch = () => {
  chatStore.searchKeyword = search.value;
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
watch(unreadCount, (newCount) => {
  console.log('📊 Unread count updated:', newCount);
});
watch(selectedConversation, (newVal) => {
  if (newVal) {
    refreshMessages();
  }
});
// ===== LIFECYCLE =====
onMounted(async () => {
  console.log('🔄 StaffChat mounted');
  const staffId = getStaffId();
  if (!staffId) {
    error('⚠️ Không tìm thấy thông tin nhân viên. Vui lòng đăng nhập lại!');
  }
  // FIX: bản cũ gọi initWebSocket() không có staffUserId -> WS connect được
  // nhưng không bao giờ register -> không nhận được sự kiện nào cả
  chatStore.initWebSocket(staffId);
  await loadConversations();
  successWithIcon('Chào mừng đến với Chat hỗ trợ khách hàng', '💬');
});
onUnmounted(() => {
  console.log('🔄 StaffChat unmounted');
  chatStore.disconnectWebSocket();
});
</script>
<style scoped>
/* ===== CSS giữ nguyên ===== */
.staff-chat {
  display: grid;
  grid-template-columns: 300px 1fr 280px;
  gap: 20px;
  min-height: calc(100vh - 130px);
  animation: fadeIn 0.3s;
}
.conversation-list,
.chat-panel,
.customer-panel {
  background: white;
  border-radius: 15px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}
.conversation-list {
  padding: 16px;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 0 12px;
  margin-bottom: 14px;
  border: 1px solid transparent;
  transition: all 0.3s;
}
.search-box:focus-within {
  border-color: #e63946;
  background: white;
}
.search-box input {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  padding: 12px 0;
  font-size: 14px;
}
.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
}
.loading-state i,
.empty-state i {
  font-size: 32px;
  margin-bottom: 12px;
}
.conversation-item {
  width: 100%;
  border: none;
  background: white;
  border-radius: 10px;
  padding: 12px;
  display: grid;
  grid-template-columns: 42px 1fr auto;
  gap: 10px;
  text-align: left;
  cursor: pointer;
  align-items: center;
  transition: all 0.2s;
  margin-bottom: 2px;
}
.conversation-item:hover {
  background: #f8f0f0;
}
.conversation-item.active {
  background: #fce8e8;
}
.conversation-item.closed {
  opacity: 0.6;
}
.conversation-item.closed .avatar {
  background: #999;
}
.conversation-item.has-unread {
  border-left: 3px solid #e63946;
}
.avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}
.conversation-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}
.conversation-top strong {
  color: #1a1a2e;
  font-size: 14px;
}
.conversation-top span {
  color: #999;
  font-size: 12px;
}
.conversation-info p {
  color: #999;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
  margin: 0;
}
.unread-dot {
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  background: #e63946;
  color: white;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}
.chat-panel {
  display: flex;
  flex-direction: column;
}
.chat-header {
  padding: 18px 22px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-info {
  flex: 1;
}
.header-info h3 {
  color: #1a1a2e;
  font-size: 17px;
  margin: 0;
}
.header-info p {
  margin: 2px 0 0;
}
.status-dot {
  display: inline-block;
  font-size: 13px;
}
.status-dot.online {
  color: #28a745;
}
.status-dot.offline {
  color: #dc3545;
}
/* FIX: nhãn "đang phụ trách" trong danh sách hội thoại */
.assigned-label {
  font-size: 11px;
  color: #6c5ce7;
  margin-top: 2px;
  font-style: italic;
}
/* FIX: banner cảnh báo chat thuộc nhân viên khác */
.assigned-banner {
  background: #fff3cd;
  color: #856404;
  padding: 10px 16px;
  font-size: 13px;
  border-top: 1px solid #ffeeba;
  border-bottom: 1px solid #ffeeba;
  text-align: center;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.close-chat-btn,
.refresh-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.2s;
}
.close-chat-btn:hover {
  background: #f5f5f5;
  color: #e63946;
}
.refresh-btn:hover:not(:disabled) {
  background: #f5f5f5;
  color: #17a2b8;
}
.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.messages {
  flex: 1;
  padding: 22px;
  background: #fafafa;
  overflow-y: auto;
  max-height: 500px;
}
.message-row {
  display: flex;
  margin-bottom: 16px;
  animation: slideIn 0.3s ease;
}
.message-row.staff {
  justify-content: flex-end;
}
.message-row.customer {
  justify-content: flex-start;
}
.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}
.message-row.staff .message-bubble {
  background: #e63946;
  color: white;
}
.message-row.customer .message-bubble {
  background: white;
  color: #1a1a2e;
}
.message-content p {
  margin: 0;
  word-wrap: break-word;
  line-height: 1.5;
}
.message-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}
.message-footer span {
  font-size: 11px;
  opacity: 0.7;
}
.message-row.staff .message-footer span,
.message-row.staff .message-footer i {
  color: rgba(255, 255, 255, 0.8);
}
.reply-box {
  padding: 16px;
  display: grid;
  grid-template-columns: 1fr 44px;
  gap: 10px;
  border-top: 1px solid #eee;
  background: white;
}
.reply-box input {
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  outline: none;
  font-size: 14px;
  transition: all 0.2s;
}
.reply-box input:focus {
  border-color: #e63946;
}
.reply-box .send-btn {
  background: #e63946;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.reply-box .send-btn:hover:not(:disabled) {
  background: #c62828;
}
.reply-box .send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.chat-closed-message {
  padding: 20px;
  text-align: center;
  color: #999;
  background: #f5f5f5;
}
.chat-closed-message i {
  font-size: 24px;
  display: block;
  margin-bottom: 8px;
}
.reopen-btn {
  margin-top: 12px;
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  background: #e63946;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
}
.reopen-btn:hover {
  background: #c62828;
}
.customer-panel {
  padding: 22px;
}
.customer-panel h3 {
  color: #1a1a2e;
  font-size: 16px;
  margin-bottom: 16px;
}
.customer-info {
  margin-bottom: 24px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}
.info-row span i {
  width: 20px;
  color: #e63946;
}
.info-row strong {
  color: #1a1a2e;
  text-align: right;
}
.quick-templates {
  margin-top: 24px;
}
.template-btn {
  width: 100%;
  border: 1px solid #eee;
  background: white;
  border-radius: 8px;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  margin-bottom: 10px;
  color: #1a1a2e;
  font-size: 13px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}
.template-btn i {
  color: #e63946;
  font-size: 14px;
}
.template-btn:hover:not(:disabled) {
  border-color: #e63946;
  background: #fef5f5;
}
.template-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes slideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
@media (max-width: 1200px) {
  .staff-chat {
    grid-template-columns: 280px 1fr;
  }
  .customer-panel {
    display: none;
  }
}
@media (max-width: 850px) {
  .staff-chat {
    grid-template-columns: 1fr;
  }
  .conversation-list {
    max-height: 300px;
    overflow-y: auto;
  }
  .chat-panel {
    min-height: 400px;
  }
}
</style>