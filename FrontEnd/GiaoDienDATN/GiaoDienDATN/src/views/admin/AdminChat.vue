<!-- views/admin/AdminChat.vue -->
<template>
  <div class="admin-chat">
    <aside class="conversation-list">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input 
          v-model="search" 
          type="text" 
          placeholder="Tìm cuộc trò chuyện..." 
          @input="handleSearch"
        />
      </div>
      <div class="filter-tabs">
        <button 
          class="filter-tab" 
          :class="{ active: filterStatus === 'all' }"
          @click="filterStatus = 'all'"
        >
          Tất cả <span class="count">{{ conversations.length }}</span>
        </button>
        <button 
          class="filter-tab" 
          :class="{ active: filterStatus === 'ACTIVE' }"
          @click="filterStatus = 'ACTIVE'"
        >
          Đang hoạt động <span class="count">{{ activeCount }}</span>
        </button>
        <button 
          class="filter-tab" 
          :class="{ active: filterStatus === 'WAITING' }"
          @click="filterStatus = 'WAITING'"
        >
          Chờ xử lý <span class="count">{{ waitingCount }}</span>
        </button>
        <button 
          class="filter-tab" 
          :class="{ active: filterStatus === 'CLOSED' }"
          @click="filterStatus = 'CLOSED'"
        >
          Đã đóng <span class="count">{{ closedCount }}</span>
        </button>
      </div>

      <!-- ✅ Lọc theo loại chat -->
      <div class="filter-type" v-if="chatTypes.length > 0">
        <select v-model="filterType" class="filter-select" @change="applyTypeFilter">
          <option value="all">Tất cả loại</option>
          <option 
            v-for="type in chatTypes" 
            :key="type.value" 
            :value="type.value"
          >
            <i :class="getTypeIcon(type.value)"></i>
            {{ type.label }}
          </option>
        </select>
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
          waiting: conversation.status === 'WAITING',
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
          <!-- ✅ Hiển thị loại chat -->
          <span v-if="conversation.chatTypeLabel" class="chat-type-tag" :class="getTypeBadgeClass(conversation.chatType)">
            <i :class="getTypeIcon(conversation.chatType)"></i>
            {{ conversation.chatTypeLabel }}
          </span>
          <p v-if="conversation.status === 'ACTIVE' && conversation.adminName" class="assigned-label">
            👤 {{ conversation.adminName }} đang phụ trách
          </p>
          <p v-if="conversation.status === 'WAITING'" class="waiting-label">
            ⏳ Đang chờ nhân viên tiếp nhận
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
            <span class="status-dot" :class="selectedConversation.status === 'ACTIVE' ? 'online' : selectedConversation.status === 'WAITING' ? 'waiting' : 'offline'">
              {{ selectedConversation.status === 'ACTIVE' ? '🟢 Đang hoạt động' : selectedConversation.status === 'WAITING' ? '🟡 Chờ xử lý' : '🔴 Đã đóng' }}
            </span>
            <span v-if="selectedConversation.adminName" class="admin-name">
              - Nhân viên: {{ selectedConversation.adminName }}
            </span>
          </p>
          <!-- ✅ Hiển thị loại chat -->
          <span v-if="selectedConversation.chatTypeLabel" class="chat-type-badge" :class="getTypeBadgeClass(selectedConversation.chatType)">
            <i :class="getTypeIcon(selectedConversation.chatType)"></i>
            {{ selectedConversation.chatTypeLabel }}
          </span>
          <!-- ✅ Hiển thị lịch sử chuyển -->
          <div v-if="selectedConversation.transferHistory && selectedConversation.transferHistory.length > 0" class="transfer-history">
            <small>
              <i class="fas fa-exchange-alt"></i>
              Đã chuyển: {{ selectedConversation.transferHistory.length }} lần
            </small>
          </div>
        </div>
        <div class="header-actions">
          <button 
            v-if="selectedConversation.status === 'WAITING'" 
            class="accept-chat-btn" 
            @click="acceptChat"
            title="Nhận hỗ trợ khách hàng"
          >
            <i class="fas fa-check-circle"></i> Nhận chat
          </button>
          <!-- ✅ Nút chuyển chat -->
          <button 
            v-if="selectedConversation.status === 'ACTIVE'"
            class="transfer-chat-btn" 
            @click="openTransferModal"
            title="Chuyển cho nhân viên khác"
          >
            <i class="fas fa-user-exchange"></i>
          </button>
          <button 
            v-if="selectedConversation.status === 'ACTIVE'" 
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

      <div
        class="messages"
        ref="messagesContainer"
        @dragover.prevent="isDragging = true"
        @dragleave="isDragging = false"
        @drop.prevent="handleDrop"
        :class="{ 'drag-over': isDragging }"
      >
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
              <template v-if="isImageMessage(message.messageContent)">
                <img
                  :src="extractImageSrc(message.messageContent)"
                  class="chat-image"
                  @click="openImageModal(extractImageSrc(message.messageContent))"
                  alt="Ảnh"
                />
              </template>
              <p v-else>{{ message.messageContent }}</p>
            </div>
            <div class="message-footer">
              <span>{{ formatTime(message.sentDate) }}</span>
              <i v-if="message.userRole === 'STAFF' && message.isRead" class="fas fa-check-double"></i>
            </div>
          </div>
        </div>
        <div v-if="isDragging" class="drag-overlay">
          <i class="fas fa-image"></i>
          <p>Thả ảnh vào đây để gửi</p>
        </div>
      </div>

      <div v-if="imagePreview && selectedConversation.status === 'ACTIVE'" class="image-preview-bar">
        <img :src="imagePreview" class="preview-thumb" alt="preview" />
        <span class="preview-label">Ảnh sẽ được gửi cùng tin nhắn</span>
        <button class="remove-preview-btn" @click="clearImagePreview" title="Bỏ ảnh">
          <i class="fas fa-times"></i>
        </button>
      </div>

      <div class="reply-box" v-if="selectedConversation.status === 'ACTIVE'">
        <input
          type="file"
          ref="fileInputRef"
          accept="image/*"
          style="display:none"
          @change="handleFileSelect"
        />
        <button class="attach-btn" @click="fileInputRef.click()" title="Gửi ảnh" :disabled="sending">
          <i class="fas fa-image"></i>
        </button>
        <input 
          v-model="reply" 
          type="text" 
          placeholder="Nhập tin nhắn hoặc dán ảnh (Ctrl+V)..." 
          @keyup.enter="sendReply"
          @paste="handlePaste"
          :disabled="sending"
          ref="replyInput"
        />
        <button 
          class="send-btn" 
          :disabled="sending || (!reply.trim() && !imagePreview)" 
          @click="sendReply"
        >
          <i v-if="!sending" class="fas fa-paper-plane"></i>
          <i v-else class="fas fa-spinner fa-spin"></i>
        </button>
      </div>

      <div v-else-if="selectedConversation.status === 'WAITING'" class="chat-waiting-message">
        <i class="fas fa-clock"></i>
        <p>Cuộc trò chuyện đang chờ nhân viên tiếp nhận</p>
        <button class="accept-btn" @click="acceptChat">
          <i class="fas fa-check-circle"></i> Nhận hỗ trợ
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
        <div class="info-row" v-if="selectedConversation.adminName">
          <span><i class="fas fa-user-tie"></i> Nhân viên</span>
          <strong>{{ selectedConversation.adminName }}</strong>
        </div>
        <div class="info-row" v-if="selectedConversation.chatTypeLabel">
          <span><i class="fas fa-tag"></i> Loại</span>
          <strong>
            <span :class="['badge-type', getTypeBadgeClass(selectedConversation.chatType)]">
              <i :class="getTypeIcon(selectedConversation.chatType)"></i>
              {{ selectedConversation.chatTypeLabel }}
            </span>
          </strong>
        </div>
        <!-- ✅ Hiển thị lịch sử chuyển -->
        <div v-if="selectedConversation.transferHistory && selectedConversation.transferHistory.length > 0" class="transfer-history-detail">
          <h4>Lịch sử chuyển</h4>
          <div v-for="(item, index) in selectedConversation.transferHistory" :key="index" class="transfer-item">
            <span>{{ item.fromUser }} → {{ item.toUser }}</span>
            <small>{{ formatTime(item.transferredAt) }}</small>
            <p v-if="item.reason">{{ item.reason }}</p>
          </div>
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

  <!-- ✅ Modal chuyển chat -->
  <Teleport to="body">
    <div v-if="showTransferModal" class="modal-overlay" @click.self="closeTransferModal">
      <div class="modal-content transfer-modal">
        <div class="modal-header">
          <h3><i class="fas fa-user-exchange"></i> Chuyển cuộc trò chuyện</h3>
          <button class="close-btn" @click="closeTransferModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Chọn nhân viên tiếp nhận <span class="required">*</span></label>
            <select v-model="transferToUserId" class="form-control">
              <option value="">-- Chọn nhân viên --</option>
              <option 
                v-for="staff in availableStaffList" 
                :key="staff.userId" 
                :value="staff.userId"
                :disabled="staff.userId === currentStaffId"
              >
                {{ staff.fullName || staff.username || 'Nhân viên ' + staff.userId }}
                <span v-if="staff.userId === currentStaffId" class="text-muted">(Bạn)</span>
                <span v-if="staff.role" class="badge-role">{{ staff.role }}</span>
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Lý do chuyển</label>
            <input v-model="transferReason" class="form-control" placeholder="Nhập lý do chuyển chat..." />
          </div>
          <div class="warning-text" v-if="transferToUserId">
            <i class="fas fa-info-circle"></i>
            <span>Bạn sẽ chuyển cuộc trò chuyện này cho nhân viên được chọn. Khách hàng sẽ thấy thông báo.</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeTransferModal">Hủy</button>
          <button 
            class="btn btn-primary" 
            @click="confirmTransfer" 
            :disabled="!transferToUserId || transferring"
          >
            <i v-if="transferring" class="fas fa-spinner fa-spin"></i>
            {{ transferring ? 'Đang chuyển...' : 'Xác nhận chuyển' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- Full-size image modal -->
  <Teleport to="body">
    <div v-if="imageModalSrc" class="admin-image-modal-overlay" @click="closeImageModal">
      <div class="admin-image-modal-content" @click.stop>
        <button class="admin-image-modal-close" @click="closeImageModal">
          <i class="fas fa-times"></i>
        </button>
        <img :src="imageModalSrc" alt="Ảnh phóng to" />
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useChatStore } from '../../stores/chatStore';
import chatStaffService from '../../services/chatStaffService';
import { useNotification } from '../../composables/useNotification';
import { useAuthStore } from '../../stores/authStore';

const chatStore = useChatStore();
const authStore = useAuthStore();
const { showNotification } = useNotification();

const {
  conversations,
  selectedConversation,
  messages,
  loading,
  searchKeyword,
  unreadCount,
  waitingCount
} = storeToRefs(chatStore);

// ===== STATE =====
const reply = ref('');
const search = ref('');
const filterStatus = ref('all');
const filterType = ref('all');
const messagesContainer = ref(null);
const replyInput = ref(null);
const fileInputRef = ref(null);
const refreshing = ref(false);
const sending = ref(false);
const loadingConversations = ref(false);
const loadingMessages = ref(false);
const isDragging = ref(false);
const imagePreview = ref(null);
const imageModalSrc = ref(null);
const chatTypes = ref([]);

// ===== TRANSFER MODAL STATE =====
const showTransferModal = ref(false);
const transferToUserId = ref(null);
const transferReason = ref('');
const transferring = ref(false);
const availableStaffList = ref([]);

// ===== CHAT TYPE HELPERS =====
const TYPE_ICONS = {
  'RETURN': 'fas fa-undo-alt',
  'PRODUCT_CONSULTATION': 'fas fa-shopping-bag',
  'RETURN_ONLINE': 'fas fa-laptop',
  'RETURN_STORE': 'fas fa-store',
  'OTHER': 'fas fa-comment'
};

const TYPE_BADGE_CLASSES = {
  'RETURN': 'badge-return',
  'PRODUCT_CONSULTATION': 'badge-product',
  'RETURN_ONLINE': 'badge-online',
  'RETURN_STORE': 'badge-store',
  'OTHER': 'badge-other'
};

const getTypeIcon = (type) => {
  return TYPE_ICONS[type] || 'fas fa-comment';
};

const getTypeBadgeClass = (type) => {
  return TYPE_BADGE_CLASSES[type] || 'badge-other';
};

// ===== COMPUTED =====
const currentStaffId = computed(() => {
  return authStore.user?.userId || 
         JSON.parse(localStorage.getItem('userInfo') || '{}').userId ||
         JSON.parse(localStorage.getItem('user') || '{}').userId;
});

const activeCount = computed(() => {
  return conversations.value.filter(c => c.status === 'ACTIVE').length;
});

const waitingCountVal = computed(() => {
  return conversations.value.filter(c => c.status === 'WAITING').length;
});

const closedCount = computed(() => {
  return conversations.value.filter(c => c.status === 'CLOSED').length;
});

const filteredConversations = computed(() => {
  let filtered = conversations.value;
  
  if (filterStatus.value !== 'all') {
    filtered = filtered.filter(c => c.status === filterStatus.value);
  }
  
  if (filterType.value !== 'all') {
    filtered = filtered.filter(c => c.chatType === filterType.value);
  }
  
  const keyword = search.value.toLowerCase().trim();
  if (keyword) {
    filtered = filtered.filter(item => 
      item.customerName?.toLowerCase().includes(keyword) ||
      item.phone?.includes(keyword)
    );
  }
  
  return filtered;
});

// ===== TEMPLATES =====
const templates = [
  "Dạ shop đang kiểm tra và phản hồi mình ngay ạ.",
  "Dạ sản phẩm này hiện còn hàng tại cửa hàng.",
  "Dạ mình cho shop xin mã đơn hàng để kiểm tra nhanh hơn ạ.",
  "Dạ shop xin lỗi vì sự chậm trễ ạ.",
  "Dạ mình vui lòng cung cấp thêm thông tin để shop hỗ trợ tốt hơn ạ.",
];

// ===== HELPER FUNCTIONS =====
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

// ===== IMAGE HELPERS =====
const IMAGE_PREFIX = '[IMAGE]';

const isImageMessage = (content) => {
  return content && content.startsWith(IMAGE_PREFIX);
};

const extractImageSrc = (content) => {
  return content ? content.slice(IMAGE_PREFIX.length) : '';
};

const openImageModal = (src) => {
  imageModalSrc.value = src;
};

const closeImageModal = () => {
  imageModalSrc.value = null;
};

const clearImagePreview = () => {
  imagePreview.value = null;
  if (fileInputRef.value) fileInputRef.value.value = '';
};

const processImageFile = (file) => {
  return new Promise((resolve, reject) => {
    if (!file || !file.type.startsWith('image/')) {
      reject(new Error('Not an image file'));
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const maxSize = 800;
        let { width, height } = img;
        if (width > maxSize || height > maxSize) {
          if (width > height) {
            height = Math.round((height * maxSize) / width);
            width = maxSize;
          } else {
            width = Math.round((width * maxSize) / height);
            height = maxSize;
          }
        }
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);
        resolve(canvas.toDataURL('image/jpeg', 0.75));
      };
      img.onerror = reject;
      img.src = e.target.result;
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
};

const handleFileSelect = async (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  try {
    imagePreview.value = await processImageFile(file);
  } catch (err) {
    showNotification('Không thể đọc file ảnh!', 'error');
  }
};

const handlePaste = async (e) => {
  const items = e.clipboardData?.items;
  if (!items) return;
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault();
      const file = item.getAsFile();
      if (file) {
        try {
          imagePreview.value = await processImageFile(file);
        } catch (err) {
          showNotification('Không thể đọc ảnh từ clipboard!', 'error');
        }
      }
      break;
    }
  }
};

const handleDrop = async (e) => {
  isDragging.value = false;
  const file = e.dataTransfer?.files?.[0];
  if (!file || !file.type.startsWith('image/')) return;
  try {
    imagePreview.value = await processImageFile(file);
  } catch (err) {
    showNotification('Không thể đọc ảnh!', 'error');
  }
};

// ===== GET STAFF ID =====
const getStaffId = () => {
  // ✅ Lấy từ nhiều nguồn
  let staffId = authStore.user?.userId || 
                authStore.user?.id || 
                authStore.user?.user_id;
  
  if (!staffId) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    staffId = userInfo.userId || userInfo.id || userInfo.user_id || userInfo.staffId;
  }
  
  if (!staffId) {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    staffId = user.userId || user.id || user.user_id;
  }
  
  console.log('👤 getStaffId result:', staffId);
  return staffId ? Number(staffId) : null;
};

// ===== LOAD CONVERSATIONS =====
const loadConversations = async () => {
  loadingConversations.value = true;
  try {
    const staffId = getStaffId();
    if (!staffId) {
      console.warn('No staffId found, skipping loadConversations');
      conversations.value = [];
      return;
    }
    const response = await chatStaffService.getChatsByStaff(staffId);
    conversations.value = response || [];
    
    conversations.value.sort((a, b) => {
      if (a.unreadCount !== b.unreadCount) return b.unreadCount - a.unreadCount;
      if (a.status === 'WAITING' && b.status !== 'WAITING') return -1;
      if (b.status === 'WAITING' && a.status !== 'WAITING') return 1;
      return new Date(b.createdAt) - new Date(a.createdAt);
    });
    
    await chatStore.updateUnreadCount();
    
    if (conversations.value.length > 0 && !selectedConversation.value) {
      const priorityConv = conversations.value.find(c => c.status === 'WAITING') ||
                          conversations.value.find(c => c.status === 'ACTIVE') ||
                          conversations.value[0];
      await selectConversation(priorityConv);
    }
  } catch (error) {
    console.error('loadConversations error:', error);
    showNotification('Không thể tải danh sách cuộc trò chuyện', 'error');
  } finally {
    loadingConversations.value = false;
  }
};

// ===== SELECT CONVERSATION =====
const selectConversation = async (conversation) => {
  if (!conversation) return;
  loadingMessages.value = true;
  
  try {
    const response = await chatStaffService.getAllActiveChats();
    const updatedConv = response.find(c => c.chatId === conversation.chatId);
    const convToUse = updatedConv || conversation;
    
    chatStore.selectedConversation = convToUse;
    
    const staffId = getStaffId();
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
    showNotification('Không thể tải tin nhắn', 'error');
  } finally {
    loadingMessages.value = false;
  }
};

// ===== FETCH CHAT TYPES =====
const fetchChatTypes = async () => {
  try {
    const response = await chatStaffService.getChatTypes();
    chatTypes.value = response || [];
  } catch (error) {
    console.error('fetchChatTypes error:', error);
  }
};

// ===== APPLY TYPE FILTER =====
const applyTypeFilter = () => {
  // Filter sẽ tự động áp dụng qua computed filteredConversations
};

// ===== ACCEPT CHAT =====
const acceptChat = async () => {
  if (!selectedConversation.value) return;
  const staffId = getStaffId();
  if (!staffId) {
    showNotification('Không tìm thấy thông tin nhân viên!', 'error');
    return;
  }
  
  try {
    await chatStaffService.acceptChat(selectedConversation.value.chatId, staffId);
    showNotification('Đã nhận hỗ trợ khách hàng!', 'success');
    await loadConversations();
  } catch (error) {
    console.error('acceptChat error:', error);
    showNotification(error.response?.data?.message || 'Không thể nhận chat', 'error');
  }
};

// ===== TRANSFER CHAT =====
const openTransferModal = async () => {
  if (!selectedConversation.value) return;
  
  try {
    const staffMap = new Map();
    
    conversations.value.forEach(c => {
      if (c.adminId && c.adminName && !staffMap.has(c.adminId)) {
        staffMap.set(c.adminId, {
          userId: c.adminId,
          username: c.adminName,
          fullName: c.adminName,
          role: c.adminRole || 'STAFF'
        });
      }
    });
    
    const currentId = getStaffId();
    if (currentId && !staffMap.has(currentId)) {
      staffMap.set(currentId, {
        userId: currentId,
        username: 'Bạn',
        fullName: 'Bạn',
        role: 'ADMIN'
      });
    }
    
    availableStaffList.value = Array.from(staffMap.values());
    
    if (availableStaffList.value.length === 0) {
      const defaultStaff = [
        { userId: 2, username: 'Nhân viên 1', fullName: 'Nhân viên 1', role: 'STAFF_ONLINE' },
        { userId: 3, username: 'Nhân viên 2', fullName: 'Nhân viên 2', role: 'STAFF_STORE' },
        { userId: 4, username: 'Nhân viên 3', fullName: 'Nhân viên 3', role: 'STAFF' }
      ];
      const filtered = defaultStaff.filter(s => s.userId !== currentStaffId.value);
      availableStaffList.value = filtered;
    }
  } catch (error) {
    console.error('Load staff list error:', error);
    availableStaffList.value = [
      { userId: 2, username: 'Nhân viên 1', fullName: 'Nhân viên 1', role: 'STAFF' },
      { userId: 3, username: 'Nhân viên 2', fullName: 'Nhân viên 2', role: 'STAFF' }
    ];
  }
  
  transferToUserId.value = null;
  transferReason.value = '';
  showTransferModal.value = true;
};

const closeTransferModal = () => {
  showTransferModal.value = false;
  transferToUserId.value = null;
  transferReason.value = '';
  transferring.value = false;
};

const confirmTransfer = async () => {
  if (!transferToUserId.value) {
    showNotification('Vui lòng chọn nhân viên tiếp nhận', 'warning');
    return;
  }
  
  const fromUserId = getStaffId();
  if (!fromUserId) {
    showNotification('Không tìm thấy thông tin nhân viên!', 'error');
    return;
  }
  
  transferring.value = true;
  try {
    await chatStaffService.transferChat(
      selectedConversation.value.chatId,
      fromUserId,
      transferToUserId.value,
      transferReason.value || 'Chuyển chat'
    );
    showNotification('Đã chuyển cuộc trò chuyện thành công!', 'success');
    closeTransferModal();
    await loadConversations();
  } catch (error) {
    console.error('transferChat error:', error);
    showNotification(error.response?.data?.message || 'Không thể chuyển chat', 'error');
  } finally {
    transferring.value = false;
  }
};

// ===== CLOSE CONVERSATION =====
const closeConversation = async () => {
  if (!selectedConversation.value) return;
  const staffId = getStaffId();
  if (!staffId) {
    showNotification('Không tìm thấy thông tin nhân viên!', 'error');
    return;
  }
  
  if (confirm('Bạn có chắc muốn đóng cuộc trò chuyện này?')) {
    try {
      await chatStaffService.closeChat(selectedConversation.value.chatId, staffId);
      showNotification('Đã đóng cuộc trò chuyện', 'success');
      await loadConversations();
    } catch (error) {
      console.error('closeConversation error:', error);
      showNotification(error.response?.data?.message || 'Không thể đóng cuộc trò chuyện', 'error');
    }
  }
};

// ===== REOPEN CONVERSATION =====
const reopenConversation = async () => {
  if (!selectedConversation.value) return;
  const staffId = getStaffId();
  if (!staffId) {
    showNotification('Không tìm thấy thông tin nhân viên!', 'error');
    return;
  }
  
  try {
    await chatStaffService.reopenChat(selectedConversation.value.chatId, staffId);
    await loadConversations();
    showNotification('Đã mở lại cuộc trò chuyện', 'success');
  } catch (error) {
    console.error('reopenConversation error:', error);
    showNotification(error.response?.data?.message || 'Không thể mở lại cuộc trò chuyện', 'error');
  }
};

// ===== SEND REPLY =====
const sendReply = async () => {
  if (sending.value || selectedConversation.value?.status !== 'ACTIVE') return;

  const staffId = getStaffId();
  if (!staffId) {
    showNotification('Không tìm thấy thông tin nhân viên!', 'error');
    return;
  }

  if (imagePreview.value) {
    const imgContent = IMAGE_PREFIX + imagePreview.value;
    clearImagePreview();
    sending.value = true;
    try {
      await chatStaffService.sendMessage(selectedConversation.value.chatId, Number(staffId), imgContent);
      await chatStore.fetchMessages(selectedConversation.value.chatId);
      messages.value = chatStore.messages;
      scrollToBottom();
      showNotification('Đã gửi ảnh thành công', 'success');
    } catch (err) {
      console.error('send image error:', err);
      showNotification(err.response?.data?.message || 'Không thể gửi ảnh', 'error');
    } finally {
      sending.value = false;
    }
  }

  if (!reply.value.trim()) return;
  const content = reply.value.trim();
  reply.value = '';
  sending.value = true;
  try {
    await chatStaffService.sendMessage(
      selectedConversation.value.chatId,
      Number(staffId),
      content
    );
    await chatStore.fetchMessages(selectedConversation.value.chatId);
    messages.value = chatStore.messages;
    const conv = conversations.value.find(c => c.chatId === selectedConversation.value.chatId);
    if (conv) {
      conv.lastMessage = { messageContent: content, sentDate: new Date().toISOString() };
      conv.unreadCount = 0;
    }
    scrollToBottom();
  } catch (err) {
    console.error('sendReply error:', err);
    showNotification(err.response?.data?.message || 'Không thể gửi tin nhắn', 'error');
    reply.value = content;
  } finally {
    sending.value = false;
  }
};

// ===== REFRESH MESSAGES =====
const refreshMessages = async () => {
  if (refreshing.value || !selectedConversation.value) return;
  
  refreshing.value = true;
  try {
    await chatStore.fetchMessages(selectedConversation.value.chatId);
    messages.value = chatStore.messages;
    
    const response = await chatStaffService.getAllActiveChats();
    const updatedConv = response.find(c => c.chatId === selectedConversation.value.chatId);
    if (updatedConv) {
      Object.assign(selectedConversation.value, updatedConv);
    }
    
    showNotification('Đã làm mới tin nhắn', 'success');
  } catch (error) {
    console.error('refreshMessages error:', error);
    showNotification('Không thể làm mới tin nhắn', 'error');
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

// ===== LIFECYCLE =====
onMounted(async () => {
  console.log('🔄 AdminChat mounted');
  
  // ✅ Lấy staffId từ nhiều nguồn
  const staffId = getStaffId();
  console.log('👤 Staff ID found:', staffId);
  
  if (!staffId) {
    showNotification('⚠️ Không tìm thấy thông tin nhân viên. Vui lòng đăng nhập lại!', 'error');
    return;
  }
  
  // ✅ Khởi tạo WebSocket
  const ws = chatStore.initWebSocket(staffId);
  console.log('🔌 WebSocket instance:', ws);
  console.log('🔌 WebSocket readyState:', ws?.readyState);
  
  // ✅ Đợi WebSocket kết nối
  if (ws) {
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  
  await Promise.all([
    loadConversations(),
    fetchChatTypes()
  ]);
  
  showNotification('Chào mừng đến với Chat hỗ trợ khách hàng', 'success');
});

onUnmounted(() => {
  console.log('🔄 AdminChat unmounted');
  chatStore.disconnectWebSocket();
});
</script>

<style scoped>
.admin-chat {
  display: grid;
  grid-template-columns: 320px 1fr 280px;
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
  overflow-y: auto;
  max-height: calc(100vh - 130px);
}

.conversation-list::-webkit-scrollbar {
  width: 6px;
}
.conversation-list::-webkit-scrollbar-track {
  background: transparent;
}
.conversation-list::-webkit-scrollbar-thumb {
  background-color: #ddd;
  border-radius: 10px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 0 12px;
  margin-bottom: 12px;
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

.filter-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.filter-tab {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  background: transparent;
  color: #666;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.filter-tab:hover {
  background: #f0f0f0;
}

.filter-tab.active {
  background: #e63946;
  color: white;
}

.filter-tab .count {
  background: rgba(0,0,0,0.1);
  padding: 0 6px;
  border-radius: 10px;
  font-size: 10px;
}

.filter-tab.active .count {
  background: rgba(255,255,255,0.2);
}

/* ✅ Filter type select */
.filter-type {
  margin-bottom: 12px;
}

.filter-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  background: white;
  color: #2d3748;
  outline: none;
  cursor: pointer;
}

.filter-select:focus {
  border-color: #e63946;
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

.conversation-item.waiting {
  border-left: 3px solid #ffc107;
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

/* ✅ Chat type tag */
.chat-type-tag {
  display: inline-block;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-top: 2px;
  color: white;
}

.chat-type-tag i {
  margin-right: 4px;
  font-size: 10px;
}

.badge-return { background: #e63946; }
.badge-product { background: #2d6a4f; }
.badge-online { background: #0077b6; }
.badge-store { background: #e76f51; }
.badge-other { background: #6c757d; }

.chat-type-badge {
  display: inline-block;
  font-size: 12px;
  padding: 3px 12px;
  border-radius: 12px;
  color: white;
  margin-top: 4px;
}

.chat-type-badge i {
  margin-right: 6px;
}

.badge-type {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  color: white;
  font-size: 13px;
}

.badge-type i {
  margin-right: 6px;
}

.assigned-label {
  font-size: 11px;
  color: #6c5ce7;
  margin-top: 2px;
  font-style: italic;
}

.waiting-label {
  font-size: 11px;
  color: #f59e0b;
  margin-top: 2px;
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

.status-dot.waiting {
  color: #f59e0b;
}

.status-dot.offline {
  color: #dc3545;
}

.admin-name {
  color: #6c5ce7;
  font-size: 12px;
  margin-left: 8px;
}

/* ✅ Transfer history */
.transfer-history {
  margin-top: 2px;
}

.transfer-history small {
  color: #6c5ce7;
  font-size: 11px;
  cursor: pointer;
}

.transfer-history-detail {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.transfer-history-detail h4 {
  font-size: 13px;
  color: #2d3748;
  margin: 0 0 8px 0;
}

.transfer-item {
  padding: 6px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 13px;
}

.transfer-item span {
  font-weight: 600;
  color: #2d3748;
}

.transfer-item small {
  color: #999;
  font-size: 11px;
  margin-left: 8px;
}

.transfer-item p {
  margin: 2px 0 0 0;
  color: #718096;
  font-size: 12px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.accept-chat-btn {
  padding: 8px 16px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.accept-chat-btn:hover {
  background: #1e7e34;
}

/* ✅ Transfer button */
.transfer-chat-btn {
  background: #6c5ce7;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.transfer-chat-btn:hover {
  background: #5a4bd1;
  transform: translateY(-2px);
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
  position: relative;
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

.chat-image {
  max-width: 200px;
  max-height: 200px;
  object-fit: cover;
  border-radius: 10px;
  cursor: pointer;
  display: block;
  transition: opacity 0.2s;
}

.chat-image:hover {
  opacity: 0.85;
}

.drag-over {
  outline: 2px dashed #e63946;
  outline-offset: -4px;
}

.drag-overlay {
  position: absolute;
  inset: 0;
  background: rgba(230, 57, 70, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #e63946;
  font-size: 14px;
  font-weight: 600;
  pointer-events: none;
  border-radius: 8px;
}

.drag-overlay i {
  font-size: 36px;
}

.image-preview-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: #fef5f5;
  border-top: 1px solid #fce4e4;
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-6px); }
  to { opacity: 1; transform: translateY(0); }
}

.preview-thumb {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid #e63946;
}

.preview-label {
  flex: 1;
  font-size: 12px;
  color: #e63946;
}

.remove-preview-btn {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: #e63946;
  color: white;
  font-size: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}

.remove-preview-btn:hover {
  background: #c62828;
}

.reply-box {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid #eee;
  background: white;
}

.reply-box input {
  flex: 1;
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

.attach-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: #f9f9f9;
  color: #e63946;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.attach-btn:hover:not(:disabled) {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.attach-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.send-btn {
  background: #e63946;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: #c62828;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-waiting-message {
  padding: 20px;
  text-align: center;
  color: #999;
  background: #f5f5f5;
}

.chat-waiting-message i {
  font-size: 24px;
  display: block;
  margin-bottom: 8px;
}

.accept-btn {
  margin-top: 12px;
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  background: #28a745;
  color: white;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.accept-btn:hover {
  background: #1e7e34;
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

/* ✅ Transfer Modal */
.transfer-modal .modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.transfer-modal .modal-body {
  padding: 20px;
}

.transfer-modal .form-group {
  margin-bottom: 16px;
}

.transfer-modal .form-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 6px;
  font-size: 14px;
}

.transfer-modal .form-control {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}

.transfer-modal .form-control:focus {
  outline: none;
  border-color: #6c5ce7;
}

.transfer-modal .modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.transfer-modal .badge-role {
  font-size: 10px;
  background: #e9ecef;
  padding: 1px 8px;
  border-radius: 10px;
  color: #666;
  margin-left: 6px;
}

.transfer-modal .text-muted {
  color: #999;
  font-size: 12px;
  margin-left: 4px;
}

/* ✅ Warning text */
.warning-text {
  padding: 10px 14px;
  background: #fff3cd;
  border-radius: 8px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: #856404;
  font-size: 13px;
}

.warning-text i {
  margin-top: 2px;
}

/* ✅ Required */
.required {
  color: #e63946;
}

/* Modal Overlay */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.modal-content {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-outline {
  background: transparent;
  color: #666;
  border: 1px solid #ddd;
}

.btn-outline:hover {
  background: #f5f5f5;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c62828;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  transition: all 0.3s;
}

.close-btn:hover {
  color: #e63946;
  transform: rotate(90deg);
}

/* Admin Image Modal */
.admin-image-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.82);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeInModal 0.2s ease;
}

@keyframes fadeInModal {
  from { opacity: 0; }
  to { opacity: 1; }
}

.admin-image-modal-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.admin-image-modal-content img {
  max-width: 100%;
  max-height: 90vh;
  border-radius: 10px;
  object-fit: contain;
  display: block;
}

.admin-image-modal-close {
  position: absolute;
  top: -14px;
  right: -14px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e63946;
  color: white;
  border: none;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  transition: background 0.2s;
}

.admin-image-modal-close:hover {
  background: #c62828;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(50px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Responsive */
@media (max-width: 1200px) {
  .admin-chat {
    grid-template-columns: 280px 1fr;
  }
  .customer-panel {
    display: none;
  }
}

@media (max-width: 850px) {
  .admin-chat {
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

@media (max-width: 480px) {
  .filter-tabs {
    flex-wrap: wrap;
  }
  .filter-tab {
    font-size: 11px;
    padding: 4px 10px;
  }
  .header-actions {
    flex-wrap: wrap;
  }
  .transfer-chat-btn {
    padding: 6px 10px;
    font-size: 12px;
  }
}
</style>