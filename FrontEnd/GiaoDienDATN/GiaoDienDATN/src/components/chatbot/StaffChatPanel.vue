<!-- components/chatbot/StaffChatPanel.vue -->
<template>
  <div class="staff-chat-panel">
    <!-- ✅ HIỂN THỊ CHỌN LOẠI CHAT KHI CHƯA BẮT ĐẦU -->
    <div v-if="showTypeSelector" class="chat-type-selector-wrapper">
      <ChatTypeSelector 
        @chat-started="onChatStarted" 
        @close="onTypeSelectorClose"
      />
    </div>

    <!-- TRẠNG THÁI: WAITING — Đang chờ nhân viên -->
    <div v-else-if="chatStatus === 'WAITING' || chatStatus === 'IDLE'" class="staff-waiting-state">
      <div class="waiting-animation">
        <div class="waiting-dots">
          <span></span><span></span><span></span>
        </div>
        <p class="waiting-text">Đang tìm nhân viên phù hợp...</p>
        <p class="waiting-sub">Bạn có thể nhắn tin ngay, nhân viên sẽ trả lời bạn sớm nhất</p>
      </div>

      <!-- HIỂN THỊ TIN NHẮN -->
      <div v-if="messages.length > 0" class="waiting-messages">
        <div
          v-for="msg in messages"
          :key="msg.messageId || msg.id"
          class="message"
          :class="isMyMessage(msg) ? 'user' : 'staff'"
        >
          <div class="message-bubble">
            <template v-if="isImageMessage(msg.messageContent || msg.content)">
              <img
                :src="extractImageSrc(msg.messageContent || msg.content)"
                class="chat-image"
                @click="openImagePreview(extractImageSrc(msg.messageContent || msg.content))"
                alt="Ảnh"
              />
            </template>
            <p v-else>{{ msg.messageContent || msg.content }}</p>
            <span class="message-time">{{ formatTime(msg.sentDate || msg.timestamp) }}</span>
          </div>
        </div>
      </div>

      <!-- Nếu chưa có tin nhắn, hiển thị hướng dẫn -->
      <div v-else class="empty-chat-message">
        <i class="fas fa-comment-dots"></i>
        <p>Hãy nhập tin nhắn để bắt đầu trò chuyện</p>
      </div>

      <button v-if="chatStatus === 'WAITING'" @click="cancelWaiting" class="cancel-btn">
        <i class="fas fa-times"></i> Hủy yêu cầu
      </button>
    </div>

    <!-- TRẠNG THÁI: ACTIVE — Đang chat với nhân viên -->
    <div v-else-if="chatStatus === 'ACTIVE'" class="staff-chat-active">
      <!-- Header nhân viên -->
      <div class="staff-chat-header">
        <div class="staff-avatar">
          <i class="fas fa-user-tie"></i>
          <span class="online-dot"></span>
        </div>
        <div class="staff-info">
          <strong>{{ assignedEmployee || 'Nhân viên CSKH' }}</strong>
          <span class="status-text">Đang hoạt động</span>
        </div>
        <button @click="closeStaffChat" class="end-chat-btn" title="Kết thúc chat">
          <i class="fas fa-phone-slash"></i>
        </button>
      </div>

      <!-- Tin nhắn -->
      <div
        class="staff-messages"
        ref="staffMessagesContainer"
        @dragover.prevent="isDragging = true"
        @dragleave="isDragging = false"
        @drop.prevent="handleDrop"
        :class="{ 'drag-over': isDragging }"
      >
        <div
          v-for="msg in messages"
          :key="msg.messageId || msg.id"
          class="message"
          :class="isMyMessage(msg) ? 'user' : 'staff'"
        >
          <div class="message-content">
            <div class="message-bubble">
              <template v-if="isImageMessage(msg.messageContent || msg.content)">
                <img
                  :src="extractImageSrc(msg.messageContent || msg.content)"
                  class="chat-image"
                  @click="openImagePreview(extractImageSrc(msg.messageContent || msg.content))"
                  alt="Ảnh"
                />
              </template>
              <p v-else>{{ msg.messageContent || msg.content }}</p>
              <span class="message-time">{{ formatTime(msg.sentDate || msg.timestamp) }}</span>
            </div>
            <span v-if="!isMyMessage(msg)" class="sender-name">{{ msg.userName || assignedEmployee || 'Nhân viên' }}</span>
          </div>
        </div>
        <!-- Drag overlay -->
        <div v-if="isDragging" class="drag-overlay">
          <i class="fas fa-image"></i>
          <p>Thả ảnh vào đây để gửi</p>
        </div>
      </div>
    </div>

    <!-- TRẠNG THÁI: CONNECTING — Đang kết nối -->
    <div v-else-if="chatStatus === 'CONNECTING'" class="loading-state">
      <i class="fas fa-spinner fa-spin"></i>
      <p>Đang kết nối...</p>
    </div>

    <!-- TRẠNG THÁI: CLOSED -->
    <div v-else-if="chatStatus === 'CLOSED'" class="staff-closed-state">
      <div class="closed-content">
        <i class="fas fa-check-circle"></i>
        <h4>Cuộc trò chuyện đã kết thúc</h4>
        <p>Cảm ơn bạn đã liên hệ với chúng tôi!</p>
        <button @click="resetChat" class="new-chat-btn">
          <i class="fas fa-plus"></i> Bắt đầu cuộc trò chuyện mới
        </button>
      </div>
    </div>

    <!-- Input area (LUÔN HIỆN, kể cả khi WAITING) -->
    <div v-if="chatStatus !== 'CLOSED' && !showTypeSelector && chatStatus !== 'CONNECTING'" class="staff-input-wrapper">
      <!-- Image preview -->
      <div v-if="imagePreview" class="image-preview-bar">
        <img :src="imagePreview" class="preview-thumb" alt="preview" />
        <button class="remove-preview-btn" @click="clearImagePreview" title="Bỏ ảnh">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="staff-input-area">
        <!-- Hidden file input -->
        <input
          type="file"
          ref="fileInput"
          accept="image/*"
          style="display:none"
          @change="handleFileSelect"
        />
        <button class="attach-btn" @click="fileInput.click()" title="Gửi ảnh" :disabled="isSending">
          <i class="fas fa-image"></i>
        </button>
        <input
          v-model="currentMessage"
          @keyup.enter="sendStaffMessage"
          @paste="handlePaste"
          type="text"
          placeholder="Nhập tin nhắn hoặc dán ảnh..."
          class="staff-input"
          :disabled="isSending"
          ref="staffInput"
        />
        <button
          @click="sendStaffMessage"
          :disabled="(!currentMessage.trim() && !imagePreview) || isSending"
          class="staff-send-btn"
        >
          <i v-if="!isSending" class="fas fa-paper-plane"></i>
          <i v-else class="fas fa-spinner fa-spin"></i>
        </button>
      </div>
    </div>
  </div>

  <!-- Full-size image modal -->
  <Teleport to="body">
    <div v-if="previewModalSrc" class="image-modal-overlay" @click="closeImageModal">
      <div class="image-modal-content" @click.stop>
        <button class="image-modal-close" @click="closeImageModal">
          <i class="fas fa-times"></i>
        </button>
        <img :src="previewModalSrc" alt="Ảnh phóng to" />
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import chatService from '../../services/chatService'
import { useWebSocket } from '../../composables/useWebSocket'
import ChatTypeSelector from './ChatTypeSelector.vue'

const emit = defineEmits(['status-change'])

// ===== STATE =====
const chatStatus = ref('IDLE')
const currentChatId = ref(null)
const assignedEmployee = ref('')
const messages = ref([])
const currentMessage = ref('')
const isConnecting = ref(false)
const isSending = ref(false)
const staffTyping = ref(false)
const userId = ref(null)
const isDragging = ref(false)
const imagePreview = ref(null)
const previewModalSrc = ref(null)

// ✅ Hiển thị selector loại chat
const showTypeSelector = ref(true)

// ===== REFS =====
const staffMessagesContainer = ref(null)
const staffInput = ref(null)
const fileInput = ref(null)

// ===== WEBSOCKET =====
let wsHandle = null
let wsCleanups = []

// ===== CHAT TYPE SELECTOR HANDLERS =====
const onChatStarted = (chat) => {
  console.log('[StaffChatPanel] Chat started event received:', chat)
  
  if (chat && chat.chatId) {
    // ✅ Ẩn selector
    showTypeSelector.value = false
    // ✅ Set trạng thái CONNECTING
    chatStatus.value = 'CONNECTING'
    emit('status-change', 'CONNECTING')
    
    currentChatId.value = chat.chatId
    
    // ✅ Sử dụng nextTick để đảm bảo UI cập nhật
    nextTick(() => {
      // ✅ Xác định trạng thái chat
      if (chat.status === 'ACTIVE' || chat.status === 'ASSIGNED') {
        chatStatus.value = 'ACTIVE'
        assignedEmployee.value = chat.staffName || chat.employeeName || 'Nhân viên CSKH'
        loadMessages()
      } else {
        chatStatus.value = 'WAITING'
      }
      
      setupWebSocket()
      emit('status-change', chatStatus.value)
      saveChatState()
      console.log('[StaffChatPanel] Chat started, status:', chatStatus.value)
    })
    
  } else {
    console.error('[StaffChatPanel] No chat data received')
    showTypeSelector.value = true
    chatStatus.value = 'IDLE'
    emit('status-change', 'IDLE')
  }
}

const onTypeSelectorClose = () => {
  showTypeSelector.value = false
  if (!currentChatId.value) {
    chatStatus.value = 'IDLE'
    emit('status-change', 'IDLE')
  }
}

// ===== GET CURRENT USER ID =====
const getCurrentUserId = () => {
  try {
    const userObj = JSON.parse(localStorage.getItem('user') || 'null')
    let id = userObj?.userId ?? userObj?.id ?? userObj?.user_id ?? null
    if (!id) id = localStorage.getItem('userId') || localStorage.getItem('user_id')
    return id ? Number(id) : null
  } catch (e) { return null }
}

// ===== AUTO CONNECT =====
const autoConnect = async () => {
  const id = getCurrentUserId()
  if (!id) {
    console.warn('[StaffChat] No user ID found')
    return
  }
  userId.value = id

  const restored = await restoreChatState()
  if (restored) {
    showTypeSelector.value = false
    return
  }

  showTypeSelector.value = true
}

// ===== LOAD MESSAGES =====
const loadMessages = async () => {
  if (!currentChatId.value) return
  try {
    const msgs = await chatService.getMessages(currentChatId.value)
    messages.value = msgs || []
    await nextTick()
    scrollToBottom()
  } catch (e) {
    console.warn('[StaffChat] Load messages error:', e)
  }
}

// ===== IMAGE HELPERS =====
const IMAGE_PREFIX = '[IMAGE]'

const isImageMessage = (content) => {
  return content && content.startsWith(IMAGE_PREFIX)
}

const extractImageSrc = (content) => {
  return content ? content.slice(IMAGE_PREFIX.length) : ''
}

const openImagePreview = (src) => {
  previewModalSrc.value = src
}

const closeImageModal = () => {
  previewModalSrc.value = null
}

const clearImagePreview = () => {
  imagePreview.value = null
  if (fileInput.value) fileInput.value.value = ''
}

const processImageFile = (file) => {
  return new Promise((resolve, reject) => {
    if (!file || !file.type.startsWith('image/')) {
      reject(new Error('Not an image file'))
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const maxSize = 800
        let { width, height } = img
        if (width > maxSize || height > maxSize) {
          if (width > height) {
            height = Math.round((height * maxSize) / width)
            width = maxSize
          } else {
            width = Math.round((width * maxSize) / height)
            height = maxSize
          }
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)
        const base64 = canvas.toDataURL('image/jpeg', 0.75)
        resolve(base64)
      }
      img.onerror = reject
      img.src = e.target.result
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const handleFileSelect = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const base64 = await processImageFile(file)
    imagePreview.value = base64
  } catch (err) {
    alert('Không thể đọc file ảnh!')
  }
}

const handlePaste = async (e) => {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (file) {
        try {
          const base64 = await processImageFile(file)
          imagePreview.value = base64
        } catch (err) {
          alert('Không thể đọc ảnh từ clipboard!')
        }
      }
      break
    }
  }
}

const handleDrop = async (e) => {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (!file || !file.type.startsWith('image/')) return
  try {
    const base64 = await processImageFile(file)
    imagePreview.value = base64
  } catch (err) {
    alert('Không thể đọc ảnh!')
  }
}

// ===== SEND MESSAGE =====
const sendStaffMessage = async () => {
  if (imagePreview.value) {
    await sendImageMessage(imagePreview.value)
    clearImagePreview()
    if (currentMessage.value.trim()) {
      await sendTextMessage(currentMessage.value.trim())
      currentMessage.value = ''
    }
    return
  }

  if (!currentMessage.value.trim()) return
  await sendTextMessage(currentMessage.value.trim())
  currentMessage.value = ''
}

const ensureChatReady = async () => {
  const id = getCurrentUserId()
  if (!id) {
    alert('Bạn cần đăng nhập để chat!')
    return false
  }
  if (!currentChatId.value) {
    showTypeSelector.value = true
    return false
  }
  return true
}

const sendTextMessage = async (msgText) => {
  const ready = await ensureChatReady()
  if (!ready) return

  const id = getCurrentUserId()
  isSending.value = true

  const tempMsg = {
    id: Date.now(),
    messageContent: msgText,
    userId: id,
    sentDate: new Date().toISOString(),
    userRole: 'CUSTOMER',
    isAi: false
  }
  messages.value.push(tempMsg)
  await nextTick()
  scrollToBottom()

  try {
    await chatService.sendMessage(currentChatId.value, id, msgText)
    const msgs = await chatService.getMessages(currentChatId.value)
    messages.value = msgs || []
  } catch (e) {
    console.error('[StaffChat] Send text error:', e)
    messages.value = messages.value.filter(m => m.id !== tempMsg.id)
    alert('Không thể gửi tin nhắn. Vui lòng thử lại!')
  } finally {
    isSending.value = false
    await nextTick()
    scrollToBottom()
  }
}

const sendImageMessage = async (base64) => {
  const ready = await ensureChatReady()
  if (!ready) return

  const id = getCurrentUserId()
  isSending.value = true
  const content = IMAGE_PREFIX + base64

  const tempMsg = {
    id: Date.now(),
    messageContent: content,
    userId: id,
    sentDate: new Date().toISOString(),
    userRole: 'CUSTOMER',
    isAi: false
  }
  messages.value.push(tempMsg)
  await nextTick()
  scrollToBottom()

  try {
    await chatService.sendMessage(currentChatId.value, id, content)
    const msgs = await chatService.getMessages(currentChatId.value)
    messages.value = msgs || []
  } catch (e) {
    console.error('[StaffChat] Send image error:', e)
    messages.value = messages.value.filter(m => m.id !== tempMsg.id)
    alert('Không thể gửi ảnh. Vui lòng thử lại!')
  } finally {
    isSending.value = false
    await nextTick()
    scrollToBottom()
  }
}

// ===== WEBSOCKET =====
const setupWebSocket = () => {
  try {
    const ws = useWebSocket()
    wsHandle = ws
    ws.connect('/ws/chat')
    
    const id = getCurrentUserId()
    if (id) {
      ws.identify('CUSTOMER', id, currentChatId.value)
    }

    const newMsgCleanup = ws.onMessage('new_message', (data) => {
      const msgData = data.data || data
      if (msgData.chatId === currentChatId.value) {
        const msg = msgData.message
        if (msg && msg.userId !== getCurrentUserId()) {
          if (chatStatus.value === 'WAITING') {
            chatStatus.value = 'ACTIVE'
            assignedEmployee.value = msg.userName || 'Nhân viên CSKH'
            emit('status-change', 'ACTIVE')
            saveChatState()
          }
          messages.value.push(msg)
          nextTick(() => scrollToBottom())
        }
      }
    })

    const assignedCleanup = ws.onMessage('chat_assigned', (data) => {
      const assignData = data.data || data
      if (assignData.chatId === currentChatId.value) {
        chatStatus.value = 'ACTIVE'
        assignedEmployee.value = assignData.staffName || assignData.employeeName || 'Nhân viên CSKH'
        emit('status-change', 'ACTIVE')
        saveChatState()
        loadMessages()
      }
    })

    const waitingCleanup = ws.onMessage('chat_waiting', (data) => {
      const waitData = data.data || data
      if (waitData.chatId === currentChatId.value) {
        chatStatus.value = 'WAITING'
        emit('status-change', 'WAITING')
        saveChatState()
      }
    })

    const closedCleanup = ws.onMessage('chat_closed', (data) => {
      const closeData = data.data || data
      if (closeData.chatId === currentChatId.value) {
        chatStatus.value = 'CLOSED'
        emit('status-change', 'CLOSED')
        clearChatState()
      }
    })

    wsCleanups = [newMsgCleanup, assignedCleanup, waitingCleanup, closedCleanup]
    
  } catch (e) {
    console.warn('[StaffChat] WS setup error:', e)
  }
}

const disconnectWebSocket = () => {
  wsCleanups.forEach(cleanup => {
    if (typeof cleanup === 'function') cleanup()
  })
  wsCleanups = []
  if (wsHandle) {
    wsHandle.disconnect()
    wsHandle = null
  }
}

// ===== CHAT ACTIONS =====
const cancelWaiting = async () => {
  if (!confirm('Bạn có muốn hủy yêu cầu hỗ trợ không?')) return
  resetChat()
}

const closeStaffChat = async () => {
  if (!currentChatId.value) return
  if (!confirm('Bạn có muốn kết thúc cuộc trò chuyện này không?')) return
  chatStatus.value = 'CLOSED'
  disconnectWebSocket()
  emit('status-change', 'CLOSED')
  clearChatState()
}

const resetChat = () => {
  chatStatus.value = 'IDLE'
  currentChatId.value = null
  assignedEmployee.value = ''
  messages.value = []
  currentMessage.value = ''
  disconnectWebSocket()
  clearChatState()
  emit('status-change', 'IDLE')
  showTypeSelector.value = true
}

// ===== STORAGE =====
const STORAGE_KEY = 'staff-chat-state'

const saveChatState = () => {
  if (!currentChatId.value) return
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    chatId: currentChatId.value,
    status: chatStatus.value,
    employeeName: assignedEmployee.value
  }))
}

const clearChatState = () => {
  localStorage.removeItem(STORAGE_KEY)
}

const restoreChatState = async () => {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
    if (!saved || !saved.chatId) return false

    const id = getCurrentUserId()
    if (!id) return false

    const chats = await chatService.getConversations(id)
    const found = chats.find(c => c.chatId === saved.chatId)
    if (!found) { clearChatState(); return false }

    currentChatId.value = saved.chatId
    
    if (found.status === 'ACTIVE' || found.status === 'ASSIGNED') {
      chatStatus.value = 'ACTIVE'
      assignedEmployee.value = found.staffName || found.employeeName || 'Nhân viên CSKH'
      await loadMessages()
    } else if (found.status === 'WAITING') {
      chatStatus.value = 'WAITING'
    } else {
      clearChatState()
      return false
    }
    
    setupWebSocket()
    emit('status-change', chatStatus.value)
    return true
  } catch (e) {
    clearChatState()
    return false
  }
}

// ===== HELPERS =====
const isMyMessage = (msg) => {
  const id = getCurrentUserId()
  return msg.userId === id
}

const scrollToBottom = () => {
  if (staffMessagesContainer.value) {
    staffMessagesContainer.value.scrollTop = staffMessagesContainer.value.scrollHeight
  }
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  try {
    return new Date(dateStr).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  } catch (e) { return '' }
}

// ===== WATCH =====
watch(messages, () => {
  nextTick(() => scrollToBottom())
}, { deep: true })

// ===== LIFECYCLE =====
onMounted(async () => {
  console.log('[StaffChatPanel] Mounted')
  userId.value = getCurrentUserId()
  await autoConnect()
})

onBeforeUnmount(() => {
  disconnectWebSocket()
})

defineExpose({ 
  connectToStaff: autoConnect,
  resetChat
})
</script>


<style scoped>
.staff-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  font-family: 'Segoe UI', sans-serif;
}

.staff-waiting-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  gap: 16px;
  overflow-y: auto;
}

.waiting-animation {
  text-align: center;
  margin-top: 10px;
}

.waiting-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.waiting-dots span {
  width: 12px;
  height: 12px;
  background: #667eea;
  border-radius: 50%;
  animation: waiting-bounce 1.4s infinite ease-in-out both;
}
.waiting-dots span:nth-child(1) { animation-delay: -0.32s; }
.waiting-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes waiting-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.waiting-text {
  font-size: 15px;
  color: #2d3748;
  font-weight: 600;
  margin: 0 0 4px;
}

.waiting-sub {
  font-size: 13px;
  color: #718096;
  margin: 0;
}

.empty-chat-message {
  text-align: center;
  padding: 30px 20px;
  color: #a0aec0;
}

.empty-chat-message i {
  font-size: 40px;
  margin-bottom: 12px;
  display: block;
}

.empty-chat-message p {
  font-size: 14px;
  margin: 0;
}

.waiting-messages {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.cancel-btn {
  padding: 8px 20px;
  background: white;
  color: #e53e3e;
  border: 1px solid #e53e3e;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}
.cancel-btn:hover { background: #fff5f5; }

.staff-chat-active {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.staff-chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  flex-shrink: 0;
}

.staff-avatar {
  position: relative;
  width: 36px;
  height: 36px;
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.online-dot {
  position: absolute;
  bottom: 1px;
  right: 1px;
  width: 9px;
  height: 9px;
  background: #48bb78;
  border: 2px solid white;
  border-radius: 50%;
}

.staff-info { flex: 1; }
.staff-info strong { display: block; font-size: 14px; }
.staff-info .status-text { font-size: 11px; opacity: 0.85; }

.end-chat-btn {
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.3);
  color: white;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: background 0.2s;
}
.end-chat-btn:hover { background: rgba(229, 62, 62, 0.5); }

.staff-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.staff-messages::-webkit-scrollbar { width: 4px; }
.staff-messages::-webkit-scrollbar-thumb { background: #667eea; border-radius: 2px; }

.message {
  display: flex;
  flex-direction: column;
  max-width: 80%;
}
.message.user { align-self: flex-end; }
.message.staff { align-self: flex-start; }

.message-content { display: flex; flex-direction: column; gap: 2px; }
.message.user .message-content { align-items: flex-end; }
.message.staff .message-content { align-items: flex-start; }

.message-bubble {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}
.message-bubble p { margin: 0; }
.message-time { display: block; font-size: 10px; margin-top: 3px; opacity: 0.6; }

.message.user .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.staff .message-bubble {
  background: #f1f3f5;
  color: #2d3748;
  border-bottom-left-radius: 4px;
}

.sender-name {
  font-size: 10px;
  color: #a0aec0;
  margin-top: 2px;
  padding-left: 4px;
}

.typing-indicator-wrapper { align-self: flex-start; max-width: 80px; }
.typing-bubble {
  padding: 10px 16px;
  background: #f1f3f5;
  border-radius: 16px;
  border-bottom-left-radius: 4px;
}
.typing-dots {
  display: flex;
  gap: 4px;
}
.typing-dots span {
  width: 6px;
  height: 6px;
  background: #a0aec0;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}
.typing-dots span:nth-child(1) { animation-delay: -0.32s; }
.typing-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.staff-input-wrapper {
  flex-shrink: 0;
  background: white;
  border-top: 1px solid #eee;
}

.image-preview-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: #f7f8fc;
  border-bottom: 1px solid #e2e8f0;
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-6px); }
  to   { opacity: 1; transform: translateY(0); }
}

.preview-thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid #667eea;
}

.remove-preview-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  background: #e53e3e;
  color: white;
  font-size: 11px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}
.remove-preview-btn:hover { background: #c53030; }

.staff-input-area {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  align-items: center;
}

.attach-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid #e2e8f0;
  background: #f7f8fc;
  color: #667eea;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}
.attach-btn:hover:not(:disabled) { background: #667eea; color: white; border-color: #667eea; }
.attach-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.staff-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}
.staff-input:focus { border-color: #667eea; }

.staff-send-btn {
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s;
  flex-shrink: 0;
}
.staff-send-btn:hover:not(:disabled) { transform: scale(1.08); }
.staff-send-btn:disabled { background: #cbd5e0; cursor: not-allowed; }

/* Chat image in bubble */
.chat-image {
  max-width: 200px;
  max-height: 200px;
  object-fit: cover;
  border-radius: 10px;
  cursor: pointer;
  display: block;
  transition: opacity 0.2s;
}
.chat-image:hover { opacity: 0.88; }

/* Drag-and-drop overlay */
.staff-messages {
  position: relative;
}
.drag-over {
  outline: 2px dashed #667eea;
  outline-offset: -4px;
}
.drag-overlay {
  position: absolute;
  inset: 0;
  background: rgba(102, 126, 234, 0.12);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #667eea;
  font-size: 14px;
  font-weight: 600;
  pointer-events: none;
  border-radius: 8px;
}
.drag-overlay i { font-size: 36px; }

/* Full-size image modal */
.image-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.8);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeInModal 0.2s ease;
}
@keyframes fadeInModal {
  from { opacity: 0; }
  to   { opacity: 1; }
}
.image-modal-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}
.image-modal-content img {
  max-width: 100%;
  max-height: 90vh;
  border-radius: 10px;
  object-fit: contain;
  display: block;
}
.image-modal-close {
  position: absolute;
  top: -14px;
  right: -14px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e53e3e;
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
.image-modal-close:hover { background: #c53030; }

.staff-closed-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.closed-content {
  text-align: center;
  padding: 20px;
}

.closed-content i {
  font-size: 48px;
  color: #48bb78;
  margin-bottom: 12px;
  display: block;
}

.closed-content h4 {
  color: #2d3748;
  margin: 0 0 8px;
  font-size: 16px;
}

.closed-content p {
  color: #718096;
  font-size: 13px;
  margin: 0 0 16px;
}

.new-chat-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 auto;
  transition: all 0.2s;
}
.new-chat-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(102,126,234,0.4); }

.chat-type-selector-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: #f8f9fa;
  overflow-y: auto;
}


.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #667eea;
  background: #f8f9fa;
}

.loading-state i {
  font-size: 32px;
}

.loading-state p {
  margin: 0;
  font-size: 14px;
  color: #718096;
}

/* ✅ Style cho chat-type-selector-wrapper */
.chat-type-selector-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: #f8f9fa;
  overflow-y: auto;
  min-height: 300px;
}
</style>