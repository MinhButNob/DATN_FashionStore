<!-- views/sale-staff/SaleStaffChat.vue -->
<template>
  <div class="sale-staff-chat">
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
      <div class="filter-type" v-if="availableFilterTypes.length > 1">
        <select v-model="filterType" class="filter-select" @change="applyTypeFilter">
          <option value="all">Tất cả loại</option>
          <option 
            v-for="type in availableFilterTypes" 
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
          <!-- ✅ Hiển thị loại chat trong header -->
          <span v-if="selectedConversation.chatTypeLabel" class="chat-type-badge" :class="getTypeBadgeClass(selectedConversation.chatType)">
            <i :class="getTypeIcon(selectedConversation.chatType)"></i>
            {{ selectedConversation.chatTypeLabel }}
          </span>
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
              <template v-else-if="isReturnFormMessage(message.messageContent)">
                <div class="return-form-card staff-view">
                  <div class="return-card-header">
                    <i class="fas fa-box-open"></i>
                    <strong>Form Yêu Cầu Hoàn Hàng</strong>
                  </div>
                  <p class="return-card-desc">{{ extractReturnFormText(message.messageContent) }}</p>
                  <span class="return-sent-badge"><i class="fas fa-check-circle"></i> Đã gửi liên kết đơn hàng</span>
                </div>
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
        <!-- ✅ Hiển thị loại chat -->
        <div class="info-row" v-if="selectedConversation.chatTypeLabel">
          <span><i class="fas fa-tag"></i> Loại tư vấn</span>
          <strong>
            <span :class="['badge-type', getTypeBadgeClass(selectedConversation.chatType)]">
              <i :class="getTypeIcon(selectedConversation.chatType)"></i>
              {{ selectedConversation.chatTypeLabel }}
            </span>
          </strong>
        </div>
      </div>

      <div class="return-form-section">
        <h3>Hỗ trợ hoàn hàng</h3>
        <p class="return-section-desc">Gửi nút liên kết chuyển hướng đến trang Đơn hàng của tôi cho khách hàng.</p>
        <button 
          class="send-return-form-btn" 
          @click="sendReturnForm"
          :disabled="selectedConversation.status !== 'ACTIVE'"
        >
          <i class="fas fa-box-open"></i> Gửi form hoàn hàng
        </button>
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

  <!-- Full-size image modal -->
  <Teleport to="body">
    <div v-if="imageModalSrc" class="sale-staff-image-modal-overlay" @click="closeImageModal">
      <div class="sale-staff-image-modal-content" @click.stop>
        <button class="sale-staff-image-modal-close" @click="closeImageModal">
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

// ===== XÁC ĐỊNH VAI TRÒ NHÂN VIÊN =====
const getUserRoles = () => {
  const user = authStore.user || JSON.parse(localStorage.getItem('user') || '{}');
  return user.roles || user.role || [];
};

const isStaff = computed(() => {
  const roles = getUserRoles();
  return roles.includes('STAFF') || roles.includes('SALE_STAFF') || roles.includes('STAFF_STORE') || roles.includes('STAFF_ONLINE');
});

const isOnlineStaff = computed(() => {
  const roles = getUserRoles();
  return roles.includes('STAFF_ONLINE') || roles.includes('SALE_STAFF');
});

const isStoreStaff = computed(() => {
  const roles = getUserRoles();
  return roles.includes('STAFF_STORE') || roles.includes('SALE_STAFF_STORE');
});

const isAdmin = computed(() => {
  const roles = getUserRoles();
  return roles.includes('ADMIN') || roles.includes('ROLE_ADMIN');
});

// ===== LẤY DANH SÁCH LOẠI CHAT PHÙ HỢP VỚI VAI TRÒ =====
const getAvailableChatTypes = () => {
  // Admin: xem tất cả
  if (isAdmin.value) {
    return chatTypes.value;
  }
  
  // Nhân viên online: chỉ thấy các loại liên quan đến online
  if (isOnlineStaff.value && !isStoreStaff.value) {
    return chatTypes.value.filter(t => 
      ['PRODUCT_CONSULTATION', 'RETURN_ONLINE'].includes(t.value)
    );
  }
  
  // Nhân viên tại quầy: chỉ thấy các loại liên quan đến cửa hàng
  if (isStoreStaff.value && !isOnlineStaff.value) {
    return chatTypes.value.filter(t => 
      ['RETURN', 'RETURN_STORE'].includes(t.value)
    );
  }
  
  // Nếu có cả 2 vai trò hoặc không xác định, hiển thị tất cả trừ OTHER
  return chatTypes.value.filter(t => t.value !== 'OTHER');
};

const availableFilterTypes = computed(() => {
  return getAvailableChatTypes();
});

// ===== TEMPLATES =====
const templates = [
  "Dạ shop đang kiểm tra và phản hồi mình ngay ạ.",
  "Dạ sản phẩm này hiện còn hàng tại cửa hàng.",
  "Dạ mình cho shop xin mã đơn hàng để kiểm tra nhanh hơn ạ.",
  "Dạ shop xin lỗi vì sự chậm trễ ạ.",
  "Dạ mình vui lòng cung cấp thêm thông tin để shop hỗ trợ tốt hơn ạ.",
];

// ===== COMPUTED =====
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

// ===== IMAGE & RETURN FORM HELPERS =====
const IMAGE_PREFIX = '[IMAGE]';
const RETURN_FORM_PREFIX = '[RETURN_FORM]';

const isImageMessage = (content) => {
  return content && content.startsWith(IMAGE_PREFIX);
};

const extractImageSrc = (content) => {
  return content ? content.slice(IMAGE_PREFIX.length) : '';
};

const isReturnFormMessage = (content) => {
  return content && content.startsWith(RETURN_FORM_PREFIX);
};

const extractReturnFormText = (content) => {
  if (!content) return '';
  return content.slice(RETURN_FORM_PREFIX.length).trim();
};

const sendReturnForm = async () => {
  if (sending.value || selectedConversation.value?.status !== 'ACTIVE') return;

  const staffId = getStaffId();
  if (!staffId) {
    showNotification('Không tìm thấy thông tin nhân viên!', 'error');
    return;
  }

  const content = RETURN_FORM_PREFIX + 'Quý khách vui lòng nhấn vào nút bên dưới để chuyển sang trang Đơn hàng của tôi và gửi yêu cầu hoàn hàng.';
  sending.value = true;
  try {
    await chatStaffService.sendMessage(selectedConversation.value.chatId, Number(staffId), content);
    await chatStore.fetchMessages(selectedConversation.value.chatId);
    messages.value = chatStore.messages;
    scrollToBottom();
    showNotification('Đã gửi form hoàn hàng cho khách hàng!', 'success');
  } catch (err) {
    console.error('sendReturnForm error:', err);
    showNotification('Không thể gửi form hoàn hàng', 'error');
  } finally {
    sending.value = false;
  }
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

// ===== LOAD CONVERSATIONS =====
const loadConversations = async () => {
  loadingConversations.value = true;
  try {
    const staffId = getStaffId();
    if (!staffId) {
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
    
    // Tự động chọn filter type phù hợp với vai trò
    const available = getAvailableChatTypes();
    if (available.length === 1) {
      filterType.value = available[0].value;
    }
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
  
  // ✅ Kiểm tra xem nhân viên có được phép nhận loại chat này không
  const chatType = selectedConversation.value.chatType;
  const availableTypes = getAvailableChatTypes().map(t => t.value);
  
  if (!availableTypes.includes(chatType) && !isAdmin.value) {
    showNotification('Bạn không được phân công hỗ trợ loại tư vấn này!', 'warning');
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

// ===== CLOSE CONVERSATION =====
const closeConversation = async () => {
  if (!selectedConversation.value) return;
  const staffId = getStaffId();
  if (!staffId) {
    showNotification('Không tìm thấy thông tin nhân viên!', 'error');
    return;
  }
  
  if (await window.appConfirm('Bạn có chắc muốn đóng cuộc trò chuyện này?')) {
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
  console.log('🔄 SaleStaffChat mounted');
  console.log('👤 User roles:', getUserRoles());
  console.log('👤 Is online staff:', isOnlineStaff.value);
  console.log('👤 Is store staff:', isStoreStaff.value);
  
  const staffId = getStaffId();
  if (!staffId) {
    showNotification('⚠️ Không tìm thấy thông tin nhân viên. Vui lòng đăng nhập lại!', 'error');
  }
  chatStore.initWebSocket(staffId);
  await Promise.all([
    loadConversations(),
    fetchChatTypes()
  ]);
  showNotification('Chào mừng đến với Chat hỗ trợ khách hàng', 'success');
});

onUnmounted(() => {
  console.log('🔄 SaleStaffChat unmounted');
  chatStore.disconnectWebSocket();
});
</script>

<style scoped>
.sale-staff-chat {
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

.filter-select option i {
  margin-right: 6px;
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

.header-actions {
  display: flex;
  gap: 8px;
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

/* Sale Staff Image Modal */
.sale-staff-image-modal-overlay {
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

.sale-staff-image-modal-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.sale-staff-image-modal-content img {
  max-width: 100%;
  max-height: 90vh;
  border-radius: 10px;
  object-fit: contain;
  display: block;
}

.sale-staff-image-modal-close {
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

.sale-staff-image-modal-close:hover {
  background: #c62828;
}

.return-form-section {
  margin-top: 24px;
  padding: 16px;
  background: #fffaf0;
  border: 1px dashed #feebc8;
  border-radius: 12px;
}
.return-form-section h3 {
  color: #dd6b20;
  font-size: 15px;
  margin-bottom: 6px;
}
.return-section-desc {
  font-size: 12.5px;
  color: #718096;
  margin: 0 0 14px 0;
  line-height: 1.4;
}
.send-return-form-btn {
  width: 100%;
  border: none;
  background: linear-gradient(135deg, #dd6b20 0%, #c05621 100%);
  color: white;
  border-radius: 8px;
  padding: 11px 14px;
  text-align: center;
  cursor: pointer;
  font-size: 13.5px;
  font-weight: 600;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 2px 6px rgba(221, 107, 32, 0.25);
}
.send-return-form-btn i {
  font-size: 15px;
}
.send-return-form-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #c05621 0%, #9c4221 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(221, 107, 32, 0.35);
}
.send-return-form-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* Return form card in staff view */
.return-form-card.staff-view {
  background: #fffaf0;
  border: 1.5px solid #feebc8;
  border-radius: 10px;
  padding: 12px;
  max-width: 300px;
}
.return-form-card.staff-view .return-card-header {
  color: #dd6b20;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  margin-bottom: 6px;
}
.return-form-card.staff-view .return-card-desc {
  font-size: 12.5px;
  color: #4a5568;
  margin: 0 0 8px 0;
}
.return-sent-badge {
  font-size: 11.5px;
  color: #38a169;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Responsive */
@media (max-width: 1200px) {
  .sale-staff-chat {
    grid-template-columns: 280px 1fr;
  }
  .customer-panel {
    display: none;
  }
}

@media (max-width: 850px) {
  .sale-staff-chat {
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
