<template>
  <div class="haru-chatbot">
    <!-- Live2D Avatar với drag support -->
    <Live2DAvatar
      ref="live2dRef"
      @position-change="onHaruPositionChange"
    />

    <!-- Chat Toggle Bubble -->
    <button
      v-if="!isOpen && isHaruVisible"
      @click="toggleChat"
      class="chat-bubble-toggle"
      :class="{ 'has-notification': hasUnreadMessages, 'side-right': !chatOnRight }"
      :style="chatToggleStyle"
      title="Chat với Haru"
    >
      <i class="fas fa-comment-dots"></i>
      <span>Trò chuyện</span>
      <div v-if="hasUnreadMessages" class="notification-badge">{{ unreadCount }}</div>
    </button>

    <!-- Button to Show/Hide Haru -->
    <button class="haru-visibility-btn" @click="toggleHaru" title="Ẩn/Hiện Haru">
      <i :class="isHaruVisible ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
    </button>

    <!-- Chat Window -->
    <transition name="chat-slide">
      <div
        v-if="isOpen"
        class="chat-window speech-bubble"
        :class="{ 'side-right': !chatOnRight }"
        :style="chatWindowStyle"
      >
        <!-- Thêm Tabs -->
        <div class="chat-tabs-header">
          <button 
            class="tab-btn" 
            :class="{ active: activeTab === 'haru' }" 
            @click="activeTab = 'haru'"
          >
            <i class="fas fa-robot"></i> AI Haru
          </button>
          <button 
            class="tab-btn" 
            :class="{ active: activeTab === 'staff' }" 
            @click="activeTab = 'staff'"
          >
            <i class="fas fa-headset"></i> Nhân viên tư vấn
          </button>
          <button @click="toggleChat" class="close-btn" title="Đóng">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <!-- AI Haru Tab -->
        <div v-show="activeTab === 'haru'" class="chat-tab-content">
          <div class="chat-messages" ref="messagesContainer">
            <div v-if="messages.length === 0" class="welcome-message">
              <div class="welcome-content">
                <h3>Xin chào! Tôi là Haru</h3>
                <p>Tôi có thể giúp bạn:</p>
                <ul>
                  <li>🛍️ Tìm kiếm sản phẩm</li>
                  <li>📝 Hỗ trợ đặt hàng</li>
                  <li>❓ Trả lời thắc mắc</li>
                </ul>
              </div>
            </div>

            <div
              v-for="message in messages"
              :key="message.id"
              class="message"
              :class="{ 'user': message.type === 'user', 'bot': message.type === 'bot' }"
            >
              <div class="message-content">
                <div class="message-bubble">
                  <p v-html="message.content"></p>
                  <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                </div>

                <!-- Card sản phẩm gắn theo tin nhắn này (nếu có) -->
                <div v-if="message.products && message.products.length > 0" class="product-recommendations">
                  <div class="product-list">
                    <div
                      v-for="product in message.products"
                      :key="product.id"
                      class="product-card"
                      @click="navigateToProduct(product.id)"
                      title="Xem chi tiết sản phẩm"
                    >
                      <img
                        v-if="product.link"
                        :src="resolveImageUrl(product.link)"
                        :alt="product.name"
                        class="product-img"
                        @error="(e) => e.target.style.display='none'"
                      />
                      <div class="product-info">
                        <p class="product-name">{{ product.name }}</p>
                        <p class="product-price">{{ formatPrice(product.price) }}đ</p>
                        <p v-if="product.color" class="product-meta">
                          {{ product.color }}<span v-if="product.size"> · {{ product.size }}</span>
                        </p>
                        <span class="product-view-btn">Xem chi tiết →</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="isTyping" class="message bot">
              <div class="message-content">
                <div class="message-bubble typing">
                  <div class="typing-indicator">
                    <span></span><span></span><span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Input Area -->
          <div class="chat-bottom">
            <div class="quick-actions">
              <button
                v-for="action in quickActions"
                :key="action.id"
                @click="sendQuickMessage(action.text)"
                class="quick-btn"
              >
                <i :class="action.icon"></i>
                {{ action.text }}
              </button>
            </div>

            <div class="input-row">
              <input
                v-model="currentMessage"
                @keyup.enter="sendMessage"
                @input="handleTyping"
                type="text"
                placeholder="Nhập câu hỏi cho Haru..."
                class="message-input"
                :disabled="isTyping"
              />
              <button
                @click="sendMessage"
                :disabled="!currentMessage.trim() || isTyping"
                class="send-btn"
              >
                <i class="fas fa-paper-plane"></i>
              </button>
            </div>
          </div>
        </div>

        <!-- Staff Tab -->
        <div v-show="activeTab === 'staff'" class="chat-tab-content">
          <StaffChatPanel ref="staffChatRef" />
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import Live2DAvatar from './Live2DAvatar.vue'
import StaffChatPanel from './StaffChatPanel.vue'

const router = useRouter()
const CHAT_WIDTH = 380
const CHAT_GAP = 16     // khoảng cách ngang giữa Haru và chat window

// ---- Reactive state ----
const activeTab = ref('haru')
const staffChatRef = ref(null)
const isOpen = ref(false)
const isHaruVisible = ref(true)
const currentMessage = ref('')
const messages = ref([])
const isTyping = ref(false)
const messagesContainer = ref(null)
const hasUnreadMessages = ref(false)
const live2dRef = ref(null)

// Vị trí Haru (từ emit của Live2DAvatar)
const haruPos = ref({ x: 20, centerX: 120, width: 200, bottom: 0 })

const quickActions = ref([
  { id: 1, text: 'Sản phẩm mới', icon: 'fas fa-star' },
  { id: 2, text: 'Khuyến mãi', icon: 'fas fa-percent' },
  { id: 3, text: 'Hỗ trợ', icon: 'fas fa-headset' }
])

const unreadCount = computed(() =>
  messages.value.filter(m => m.type === 'bot' && !m.read).length
)

// ---- Vị trí chat: tính toán dựa trên vị trí Haru ----
// Chat nằm bên PHẢI Haru khi Haru ở nửa trái màn hình, và ngược lại
const chatOnRight = computed(() => {
  return haruPos.value.centerX <= window.innerWidth / 2
})

// Bottom của chat bám theo bottom của Haru (canh đáy canvas)
// haruPos.bottom = window.innerHeight - rect.bottom (khoảng cách từ đáy canvas đến đáy viewport)
// Chat bottom = bottom của Haru (canh thẳng hàng với đáy Haru)
const chatBottom = computed(() => {
  // bottom của oml2d element tính từ đáy viewport
  // rect.bottom = window.innerHeight - haruPos.bottom
  // Ta muốn đáy chat ngang đáy Haru → bottom của chat = haruPos.bottom
  return Math.max(10, haruPos.value.bottom)
})

// Tính left/right của chat window theo vị trí Haru
const chatWindowStyle = computed(() => {
  const haruRight = haruPos.value.x + haruPos.value.width

  if (chatOnRight.value) {
    // Haru bên trái → chat nằm bên phải Haru
    const left = haruRight + CHAT_GAP
    const safeLeft = Math.min(left, window.innerWidth - CHAT_WIDTH - 10)
    return {
      left: safeLeft + 'px',
      right: 'auto',
      bottom: chatBottom.value + 'px' }
  } else {
    // Haru bên phải → chat nằm bên trái Haru
    const left = haruPos.value.x - CHAT_WIDTH - CHAT_GAP
    const safeLeft = Math.max(10, left)
    return {
      left: safeLeft + 'px',
      right: 'auto',
      bottom: chatBottom.value + 'px' }
  }
})

// Style cho nút toggle (bong bóng nhỏ khi chat đóng) — cũng bám theo Haru
const chatToggleStyle = computed(() => {
  const haruRight = haruPos.value.x + haruPos.value.width
  // Toggle hiện giữa chiều cao Haru, cách đáy Haru một chút
  const toggleBottom = chatBottom.value + 60

  if (chatOnRight.value) {
    const left = haruRight + CHAT_GAP
    const safeLeft = Math.min(left, window.innerWidth - 180)
    return { left: safeLeft + 'px', right: 'auto', bottom: toggleBottom + 'px' }
  } else {
    const left = haruPos.value.x - 180 - CHAT_GAP
    const safeLeft = Math.max(10, left)
    return { left: safeLeft + 'px', right: 'auto', bottom: toggleBottom + 'px' }
  }
})

// ---- Callback từ Live2DAvatar khi Haru di chuyển ----
const onHaruPositionChange = (pos) => {
  haruPos.value = pos
}

// ---- Chat logic ----
const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    hasUnreadMessages.value = false
    markAllAsRead()
    nextTick(() => {
      scrollToBottom()
      if (live2dRef.value) live2dRef.value.triggerMotion('idle')
    })
  }
}

const toggleHaru = () => {
  isHaruVisible.value = !isHaruVisible.value
  if (live2dRef.value) live2dRef.value.toggleDisplay(isHaruVisible.value)
  if (!isHaruVisible.value && isOpen.value) isOpen.value = false
}

const sendQuickMessage = (text) => {
  currentMessage.value = text
  sendMessage()
}

const sendMessage = async () => {
  if (!currentMessage.value.trim()) return

  const messageText = currentMessage.value.trim()

  const userMessage = {
    id: Date.now(),
    type: 'user',
    content: messageText,
    timestamp: new Date(),
    read: true
  }

  messages.value.push(userMessage)
  currentMessage.value = ''

  await nextTick()
  scrollToBottom()

  // Haru bắt đầu "đang suy nghĩ" khi nhận tin
  if (live2dRef.value) live2dRef.value.playEmotion('thinking')
  // Reset idle timer vì đang có tương tác
  scheduleIdleEmotion()

  isTyping.value = true

  try {
    const result = await callAIAPI(messageText)

    const botMessage = {
      id: Date.now() + 1,
      type: 'bot',
      content: result.text,
      timestamp: new Date(),
      read: isOpen.value,
      products: result.products || []
    }

    messages.value.push(botMessage)

    // Phân tích nội dung response để chọn emotion phù hợp
    if (live2dRef.value) {
      const emotion = detectEmotion(result.text)
      live2dRef.value.playEmotion(emotion)
    }
    // Lưu ý: không tự động điều hướng nữa — Haru chỉ hiển thị card sản phẩm,
    // người dùng bấm vào card mới chuyển sang trang chi tiết.
  } catch (error) {
    console.error('API Error:', error)
    messages.value.push({
      id: Date.now() + 1,
      type: 'bot',
      content: 'Xin lỗi, Haru đang gặp vấn đề kết nối. Bạn vui lòng thử lại sau nhé! 🙏',
      timestamp: new Date(),
      read: isOpen.value
    })
    if (live2dRef.value) live2dRef.value.playEmotion('sorry')
  } finally {
    isTyping.value = false
    if (!isOpen.value) hasUnreadMessages.value = true
    await nextTick()
    scrollToBottom()
  }
}

const callAIAPI = async (message) => {
  let userId = null
  try {
    const userObj = JSON.parse(localStorage.getItem('user') || 'null')
    userId = userObj?.userId ?? userObj?.id ?? userObj?.user_id ?? userObj?.customerId ?? null
  } catch (e) {
    console.warn('Could not parse user from localStorage', e)
  }

  // Also try direct key
  if (!userId) {
    userId = localStorage.getItem('userId') || localStorage.getItem('user_id')
    if (userId) userId = Number(userId)
  }

  if (!userId) {
    return {
      text: 'Bạn cần đăng nhập để trò chuyện với Haru nhé! 🔐',
      products: []
    }
  }

  const token = localStorage.getItem('token') || localStorage.getItem('accessToken')
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const response = await fetch('/ai/chat', {
    method: 'POST',
    headers,
    body: JSON.stringify({ message, userId: Number(userId) })
  })

  if (!response.ok) throw new Error(`API error: ${response.status}`)

  const data = await response.json()
  if (data.success && data.data) {
    const { response: aiText, products, navigateToProductId, intent } = data.data
    // Debug log - xoá khi production
    console.log('[Haru] intent:', intent, '| products:', products)
    return {
      text: aiText || 'Xin lỗi, Haru không hiểu. Bạn có thể hỏi lại không?',
      products: Array.isArray(products) ? products : [],
      navigateToProductId: navigateToProductId || null
    }
  }
  throw new Error('Invalid response format')
}

const markAllAsRead = () => {
  messages.value.forEach(m => { if (m.type === 'bot') m.read = true })
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const formatTime = (date) =>
  date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })

const formatPrice = (price) =>
  price ? new Intl.NumberFormat('vi-VN').format(price) : 'Liên hệ'

// Ảnh sản phẩm từ DB có thể là path tương đối (vd: /uploads/abc.jpg)
// => cần ghép domain backend để hiển thị đúng, nếu đã là URL đầy đủ (http...) thì giữ nguyên.
const BACKEND_BASE_URL = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")
const resolveImageUrl = (link) => {
  if (!link) return ''
  if (/^https?:\/\//i.test(link)) return link
  return `${BACKEND_BASE_URL}${link.startsWith('/') ? '' : '/'}${link}`
}

const handleTyping = () => {}

// ---- Emotion system ----
// Phân tích text response của AI để chọn emotion phù hợp
const detectEmotion = (text) => {
  const t = text.toLowerCase()
  if (/xin lỗi|không thể|lỗi|tiếc|thất bại|không tìm|không hỗ trợ/.test(t)) return 'sorry'
  if (/khuyến mãi|giảm giá|sale|ưu đãi|miễn phí|tặng|quà/.test(t)) return 'excited'
  if (/tìm thấy|có sẵn|đây là|gợi ý|sản phẩm|kết quả/.test(t)) return 'happy'
  if (/chào|xin chào|hello|hi |haru/.test(t)) return 'greeting'
  if (/không biết|không chắc|có thể|hãy thử|thêm thông tin/.test(t)) return 'thinking'
  return 'speak'
}

// Điều hướng đến trang sản phẩm
const navigateToProduct = (productId) => {
  if (!productId) return
  isOpen.value = false // đóng chat để xem trang
  router.push(`/product/${productId}`)
}

// Idle random: sau mỗi 8–15s không có tương tác, Haru tự đổi biểu cảm
let idleTimer = null
const IDLE_EMOTIONS = ['idle', 'idle', 'greeting', 'thinking', 'happy']

const scheduleIdleEmotion = () => {
  if (idleTimer) clearTimeout(idleTimer)
  const delay = 8000 + Math.random() * 7000 // 8–15s
  idleTimer = setTimeout(() => {
    if (!isTyping.value && live2dRef.value) {
      const emotion = IDLE_EMOTIONS[Math.floor(Math.random() * IDLE_EMOTIONS.length)]
      live2dRef.value.playEmotion(emotion)
    }
    scheduleIdleEmotion() // lặp lại
  }, delay)
}

onMounted(() => {
  const savedMessages = localStorage.getItem('haru-chat-messages')
  if (savedMessages) {
    try {
      const parsed = JSON.parse(savedMessages)
      messages.value = parsed.map(m => ({ ...m, timestamp: new Date(m.timestamp) }))
    } catch (e) {
      console.log('Could not load saved messages')
    }
  }
  scheduleIdleEmotion()
})

onBeforeUnmount(() => {
  if (idleTimer) clearTimeout(idleTimer)
})

const saveMessages = () => {
  localStorage.setItem('haru-chat-messages', JSON.stringify(messages.value))
}

watch(messages, saveMessages, { deep: true })
</script>

<style scoped>
.haru-chatbot {
  position: fixed;
  z-index: 9999;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

/* ---- Visibility button ---- */
.haru-visibility-btn {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  box-shadow: 0 5px 15px rgba(0,0,0,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  z-index: 9999;
  transition: all 0.3s;
}
.haru-visibility-btn:hover { transform: scale(1.1); }

/* ---- Chat Toggle Bubble ---- */
.chat-bubble-toggle {
  position: fixed;
  background: white;
  color: #667eea;
  border: 2px solid #667eea;
  padding: 12px 20px;
  /* Mặc định: Haru bên trái → chat bên phải → đuôi bong bóng bên trái */
  border-radius: 20px 20px 20px 0;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: background 0.3s, color 0.3s, box-shadow 0.3s, border-color 0.3s;
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  font-size: 14px;
}

/* Đuôi trái (Haru bên trái, chat bên phải) */
.chat-bubble-toggle:not(.side-right)::before {
  content: '';
  position: absolute;
  bottom: -2px;
  left: -18px;
  border-width: 0 16px 16px 0;
  border-style: solid;
  border-color: transparent #667eea transparent transparent;
}
.chat-bubble-toggle:not(.side-right)::after {
  content: '';
  position: absolute;
  bottom: 1px;
  left: -13px;
  border-width: 0 14px 14px 0;
  border-style: solid;
  border-color: transparent white transparent transparent;
}

/* Đuôi phải (Haru bên phải, chat bên trái) */
.chat-bubble-toggle.side-right {
  border-radius: 20px 20px 0 20px;
}
.chat-bubble-toggle.side-right::before {
  content: '';
  position: absolute;
  bottom: -2px;
  right: -18px;
  left: auto;
  border-width: 16px 16px 0 0;
  border-style: solid;
  border-color: #667eea transparent transparent transparent;
}
.chat-bubble-toggle.side-right::after {
  content: '';
  position: absolute;
  bottom: 1px;
  right: -13px;
  left: auto;
  border-width: 14px 14px 0 0;
  border-style: solid;
  border-color: white transparent transparent transparent;
}

.chat-bubble-toggle:hover {
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}
.chat-bubble-toggle:hover::before,
.chat-bubble-toggle:hover::after { display: none; }

.chat-bubble-toggle.has-notification { animation: bounce 2s infinite; }

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-10px); }
  60% { transform: translateY(-5px); }
}

.notification-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #ff4757;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: bold;
  border: 2px solid white;
}

/* ---- Chat Window ---- */
.chat-window.speech-bubble {
  position: fixed;
  width: 380px;
  height: 500px;
  background: white;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  border: 2px solid #667eea;
  /* Mặc định: chat bên phải Haru → đuôi bong bóng góc dưới trái */
  border-radius: 20px 20px 20px 0;
}

/* Đuôi trái (chat bên phải Haru) */
.chat-window.speech-bubble:not(.side-right)::before {
  content: '';
  position: absolute;
  bottom: -2px;
  left: -22px;
  border-width: 0 20px 20px 0;
  border-style: solid;
  border-color: transparent #667eea transparent transparent;
}
.chat-window.speech-bubble:not(.side-right)::after {
  content: '';
  position: absolute;
  bottom: 1px;
  left: -17px;
  border-width: 0 18px 18px 0;
  border-style: solid;
  border-color: transparent white transparent transparent;
}

/* Đuôi phải (chat bên trái Haru) */
.chat-window.speech-bubble.side-right {
  border-radius: 20px 20px 0 20px;
}
.chat-window.speech-bubble.side-right::before {
  content: '';
  position: absolute;
  bottom: -2px;
  right: -22px;
  left: auto;
  border-width: 20px 20px 0 0;
  border-style: solid;
  border-color: #667eea transparent transparent transparent;
}
.chat-window.speech-bubble.side-right::after {
  content: '';
  position: absolute;
  bottom: 1px;
  right: -17px;
  left: auto;
  border-width: 18px 18px 0 0;
  border-style: solid;
  border-color: white transparent transparent transparent;
}

/* Transition animation */
.chat-slide-enter-active, .chat-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.chat-slide-enter-from, .chat-slide-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.97);
}

/* ---- Close Button ---- */
.close-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  background: none;
  border: none;
  font-size: 18px;
  color: #a0a0a0;
  cursor: pointer;
  z-index: 10;
}
.close-btn:hover { color: #ff4757; }

/* ---- Tabs ---- */
.chat-tabs-header {
  display: flex;
  background: #f8f9fa;
  border-bottom: 1px solid #eee;
  padding: 0 10px;
  padding-right: 45px; /* Chỗ cho close button */
  border-radius: 20px 20px 0 0;
  flex-shrink: 0;
}
.chat-window.speech-bubble.side-right .chat-tabs-header {
  border-radius: 20px 20px 0 0;
}

.tab-btn {
  flex: 1;
  background: none;
  border: none;
  padding: 15px 10px;
  font-size: 14px;
  font-weight: 600;
  color: #718096;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.tab-btn:hover {
  color: #4a5568;
}
.tab-btn.active {
  color: #667eea;
  border-bottom: 2px solid #667eea;
}

.chat-tab-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ---- Messages ---- */
.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.chat-messages::-webkit-scrollbar { width: 6px; }
.chat-messages::-webkit-scrollbar-track { background: #f1f1f1; border-radius: 3px; }
.chat-messages::-webkit-scrollbar-thumb { background: #667eea; border-radius: 3px; }

.welcome-message { display: flex; flex-direction: column; align-items: flex-start; margin-bottom: 10px; }
.welcome-content { background: #f8f9fa; padding: 15px; border-radius: 15px; border-top-left-radius: 0; color: #333; }
.welcome-content h3 { margin: 0 0 10px 0; color: #667eea; font-size: 16px; }
.welcome-content p { margin: 0 0 8px 0; font-size: 14px; }
.welcome-content ul { margin: 0; padding-left: 0; list-style: none; font-size: 13px; line-height: 1.6; }

.message { display: flex; flex-direction: column; max-width: 85%; }
.message.user { align-self: flex-end; }
.message.bot { align-self: flex-start; }
.message-content { display: flex; flex-direction: column; gap: 6px; width: 100%; }
.message.user .message-content { align-items: flex-end; }
.message.bot .message-content { align-items: flex-start; }

.message-bubble {
  padding: 12px 16px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.5;
  position: relative;
  word-break: break-word;
}
.message-bubble p { margin: 0; }

.user .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}
.bot .message-bubble { background: #f1f3f5; color: #333; border-bottom-left-radius: 4px; }
.message-time { display: block; font-size: 10px; margin-top: 4px; opacity: 0.7; text-align: right; }

.typing-indicator { display: flex; gap: 4px; padding: 4px 8px; }
.typing-indicator span {
  width: 6px; height: 6px;
  background: rgba(0,0,0,0.5);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}
.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* ---- Bottom Input ---- */
.chat-bottom { padding: 15px; background: white; border-top: 1px solid #eee; border-radius: 0 0 20px 0; }
.quick-actions { display: flex; gap: 8px; margin-bottom: 12px; overflow-x: auto; padding-bottom: 4px; }
.quick-actions::-webkit-scrollbar { height: 0; }

.quick-btn {
  white-space: nowrap;
  background: #f0f2f5;
  border: 1px solid #e4e6eb;
  color: #050505;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}
.quick-btn:hover { background: #e4e6eb; color: #667eea; }
.quick-btn i { color: #667eea; }

.input-row { display: flex; gap: 10px; align-items: center; }

.message-input {
  flex: 1;
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}
.message-input:focus { border-color: #667eea; }

.send-btn {
  background: #667eea;
  color: white;
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}
.send-btn:hover:not(:disabled) { background: #5a67d8; transform: scale(1.05); }
.send-btn:disabled { background: #ccc; cursor: not-allowed; }

/* ---- Product Recommendations ---- */
.product-recommendations { padding: 0; width: 100%; max-width: 280px; }
.product-rec-title { font-size: 12px; color: #667eea; font-weight: 600; margin: 0 0 8px 0; }
.product-list { display: flex; flex-direction: column; gap: 8px; }

.product-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.product-card:hover { border-color: #667eea; background: #f0f2ff; }
.product-card:hover .product-view-btn { opacity: 1; }

.product-view-btn {
  display: inline-block;
  margin-top: 4px;
  font-size: 11px;
  color: #667eea;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.2s;
}

.product-img { width: 64px; height: 64px; object-fit: cover; border-radius: 8px; flex-shrink: 0; background: #eee; }
.product-info { flex: 1; min-width: 0; }
.product-name { font-size: 13px; font-weight: 600; color: #222; margin: 0 0 2px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.product-price { font-size: 13px; color: #667eea; font-weight: 700; margin: 0 0 2px 0; }
.product-meta { font-size: 11px; color: #888; margin: 0; }
</style>