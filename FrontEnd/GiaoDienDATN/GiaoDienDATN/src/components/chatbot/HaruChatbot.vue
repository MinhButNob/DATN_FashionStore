<template>
  <div class="haru-chatbot">
    <!-- Live2D Avatar is now mounted globally, but anchored here -->
    <Live2DAvatar ref="live2dRef" />

    <!-- Chat Toggle Bubble (Shows when chat is closed & Haru is visible) -->
    <button 
      v-if="!isOpen && isHaruVisible" 
      @click="toggleChat" 
      class="chat-bubble-toggle"
      :class="{ 'has-notification': hasUnreadMessages }"
      title="Chat với Haru"
    >
      <i class="fas fa-comment-dots"></i>
      <span>Trò chuyện</span>
      <div v-if="hasUnreadMessages" class="notification-badge">{{ unreadCount }}</div>
    </button>

    <!-- Button to Show/Hide Haru (Bottom Right) -->
    <button class="haru-visibility-btn" @click="toggleHaru" title="Ẩn/Hiện Haru">
      <i :class="isHaruVisible ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
    </button>

    <!-- Chat Window styled as a speech bubble -->
    <div v-if="isOpen" class="chat-window speech-bubble">
      <!-- Chat Messages -->
      <div class="chat-messages" ref="messagesContainer">
        <!-- Close Button -->
        <button @click="toggleChat" class="close-btn" title="Đóng">
          <i class="fas fa-times"></i>
        </button>
        
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
          </div>
        </div>

        <div v-if="isTyping" class="message bot">
          <div class="message-content">
            <div class="message-bubble typing">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Input Area & Prompts -->
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import Live2DAvatar from './Live2DAvatar.vue'

// Reactive data
const isOpen = ref(false)
const isHaruVisible = ref(true)
const currentMessage = ref('')
const messages = ref([])
const isTyping = ref(false)
const messagesContainer = ref(null)
const hasUnreadMessages = ref(false)
const live2dRef = ref(null)

// Quick actions
const quickActions = ref([
  { id: 1, text: 'Sản phẩm mới', icon: 'fas fa-star' },
  { id: 2, text: 'Khuyến mãi', icon: 'fas fa-percent' },
  { id: 3, text: 'Hỗ trợ', icon: 'fas fa-headset' }
])

// Computed
const unreadCount = computed(() => {
  return messages.value.filter(m => m.type === 'bot' && !m.read).length
})

// Methods
const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    hasUnreadMessages.value = false
    markAllAsRead()
    nextTick(() => {
      scrollToBottom()
      if (live2dRef.value) {
        live2dRef.value.triggerMotion('idle')
      }
    })
  }
}

const toggleHaru = () => {
  isHaruVisible.value = !isHaruVisible.value
  if (live2dRef.value) {
    live2dRef.value.toggleDisplay(isHaruVisible.value)
  }
  // Nếu ẩn Haru thì cũng nên tắt chat window
  if (!isHaruVisible.value && isOpen.value) {
    isOpen.value = false
  }
}

const sendQuickMessage = (text) => {
  currentMessage.value = text
  sendMessage()
}

const sendMessage = async () => {
  if (!currentMessage.value.trim()) return
  
  const userMessage = {
    id: Date.now(),
    type: 'user',
    content: currentMessage.value.trim(),
    timestamp: new Date(),
    read: true
  }
  
  messages.value.push(userMessage)
  const messageText = currentMessage.value.trim()
  currentMessage.value = ''
  
  await nextTick()
  scrollToBottom()
  
  // Trigger avatar khi user gửi tin nhắn
  if (live2dRef.value) {
    live2dRef.value.triggerMotion('speak')
    live2dRef.value.setExpression('normal')
  }
  
  isTyping.value = true
  
  try {
    // Gọi API AI thực tế
    const botResponse = await callAIAPI(messageText)
    
    const botMessage = {
      id: Date.now() + 1,
      type: 'bot',
      content: botResponse,
      timestamp: new Date(),
      read: isOpen.value
    }
    
    messages.value.push(botMessage)
    
    // Trigger avatar khi bot trả lời
    if (live2dRef.value) {
      // Phản ứng vui vẻ
      live2dRef.value.setExpression('happy')
      live2dRef.value.triggerMotion('happy')
      
      // Trở lại trạng thái idle sau khi trả lời xong (khoảng 3 giây)
      setTimeout(() => {
        if (live2dRef.value) {
          live2dRef.value.triggerMotion('idle')
          live2dRef.value.setExpression('normal')
        }
      }, 3000)
    }
    
  } catch (error) {
    console.error('API Error:', error)
    // Fallback response
    messages.value.push({
      id: Date.now() + 1,
      type: 'bot',
      content: 'Xin lỗi, tôi đang gặp vấn đề kết nối. Bạn vui lòng thử lại sau nhé! 🙏',
      timestamp: new Date(),
      read: isOpen.value
    })
  } finally {
    isTyping.value = false
    if (!isOpen.value) {
      hasUnreadMessages.value = true
    }
    await nextTick()
    scrollToBottom()
  }
}

// Hàm gọi API AI thực tế
const callAIAPI = async (message) => {
  try {
    // Gọi API của bạn (Gemini/ChatGPT)
    const response = await fetch('http://localhost:8080/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        message: message,
        userId: localStorage.getItem('userId') || 'guest'
      })
    })
    
    if (!response.ok) {
      throw new Error(`API error: ${response.status}`)
    }
    
    const data = await response.json()
    return data.response || data.message || 'Xin lỗi, tôi không hiểu. Bạn có thể hỏi lại không?'
    
  } catch (error) {
    console.error('API call failed:', error)
    // Fallback to mock response
    return getMockResponse(message)
  }
}

// Giữ lại mock response làm fallback
const getMockResponse = (message) => {
  const lowerMessage = message.toLowerCase()
  
  if (lowerMessage.includes('chào') || lowerMessage.includes('hello') || lowerMessage.includes('hi')) {
    return `Xin chào! 👋 Tôi là Haru, trợ lý AI của Fashion Store. Tôi có thể giúp bạn tìm kiếm sản phẩm, tư vấn thời trang, hoặc hỗ trợ đặt hàng. Bạn cần tôi giúp gì không?`
  }
  
  if (lowerMessage.includes('sản phẩm') || lowerMessage.includes('áo') || lowerMessage.includes('quần')) {
    return `🛍️ Chúng tôi có rất nhiều sản phẩm thời trang đa dạng! Bạn quan tâm đến loại sản phẩm nào?
    
    <br/>• 👔 Áo thun, sơ mi, hoodie
    <br/>• 👖 Quần jean, kaki, short  
    <br/>• 👗 Váy đầm các loại
    <br/>• 👟 Giày dép và phụ kiện
    
    <br/>Bạn có thể tìm kiếm trực tiếp trên trang <strong>Cửa hàng</strong> nhé!`
  }
  
  if (lowerMessage.includes('khuyến mãi') || lowerMessage.includes('giảm giá') || lowerMessage.includes('sale')) {
    return `🎉 Hiện tại chúng tôi đang có các chương trình khuyến mãi hấp dẫn:
    
    <br/>🔥 <strong>Flash Sale cuối tuần:</strong> Giảm đến 50%
    <br/>💳 <strong>Mã giảm giá:</strong> FASHION20 (giảm 20%)  
    <br/>🚚 <strong>Miễn phí ship:</strong> Đơn hàng từ 500k
    
    <br/>Hãy nhanh tay đặt hàng để không bỏ lỡ ưu đãi nhé! 💖`
  }
  
  if (lowerMessage.includes('đặt hàng') || lowerMessage.includes('mua') || lowerMessage.includes('order')) {
    return `📦 Để đặt hàng, bạn có thể:
    
    <br/>1️⃣ Thêm sản phẩm vào giỏ hàng
    <br/>2️⃣ Vào trang <strong>Giỏ hàng</strong> để kiểm tra
    <br/>3️⃣ Bấm <strong>Thanh toán</strong> và điền thông tin
    <br/>4️⃣ Chọn phương thức thanh toán (COD/VNPay)
    
    <br/>Bạn cần hỗ trợ gì thêm về quá trình đặt hàng không? 😊`
  }
  
  if (lowerMessage.includes('hỗ trợ') || lowerMessage.includes('help') || lowerMessage.includes('giúp')) {
    return `🤝 Tôi có thể hỗ trợ bạn:
    
    <br/>• 🔍 Tìm kiếm và tư vấn sản phẩm
    <br/>• 💰 Thông tin về giá cả và khuyến mãi  
    <br/>• 📋 Hướng dẫn đặt hàng và thanh toán
    <br/>• 🚚 Chính sách giao hàng và đổi trả
    <br/>• 📞 Liên hệ với bộ phận CSKH
    
    <br/>Bạn muốn biết thêm về vấn đề nào? 💡`
  }
  
  const responses = [
    `Tôi hiểu ý bạn rồi! 😊 Nhưng tôi cần thêm thông tin để có thể hỗ trợ bạn tốt nhất. Bạn có thể nói rõ hơn được không?`,
    `Thật thú vị! 🤔 Tôi đang học cách hiểu câu hỏi này tốt hơn. Trong lúc đó, bạn có thể thử hỏi về sản phẩm, khuyến mãi, hoặc cách đặt hàng nhé!`,
    `Hmm, tôi chưa hiểu rõ lắm. 💭 Bạn có thể thử các từ khóa như "sản phẩm", "khuyến mãi", "đặt hàng", hoặc "hỗ trợ" để tôi giúp bạn tốt hơn!`
  ]
  
  return responses[Math.floor(Math.random() * responses.length)]
}

const clearChat = () => {
  messages.value = []
  hasUnreadMessages.value = false
}

const markAllAsRead = () => {
  messages.value.forEach(m => {
    if (m.type === 'bot') m.read = true
  })
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const formatTime = (date) => {
  return date.toLocaleTimeString('vi-VN', { 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

const handleTyping = () => {
  // Could implement typing indicators or auto-suggestions here
}

// Initialize
onMounted(() => {
  // Optional: Load chat history from localStorage
  const savedMessages = localStorage.getItem('haru-chat-messages')
  if (savedMessages) {
    try {
      const parsed = JSON.parse(savedMessages)
      messages.value = parsed.map(m => ({
        ...m,
        timestamp: new Date(m.timestamp)
      }))
    } catch (e) {
      console.log('Could not load saved messages')
    }
  }
})

// Save messages to localStorage when they change
const saveMessages = () => {
  localStorage.setItem('haru-chat-messages', JSON.stringify(messages.value))
}

// Watch messages and save
import { watch } from 'vue'
watch(messages, saveMessages, { deep: true })
</script>

<style scoped>
.haru-chatbot {
  position: fixed;
  bottom: 20px;
  left: 20px;
  z-index: 9999;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  display: flex;
  align-items: flex-end;
  gap: 15px;
}

/* Round button to toggle Haru visibility */
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

.haru-visibility-btn:hover {
  transform: scale(1.1);
}

/* Chat Toggle Bubble */
.chat-bubble-toggle {
  position: fixed;
  bottom: 80px;
  left: 250px; /* Right next to Haru */
  background: white;
  color: #667eea;
  border: 2px solid #667eea;
  padding: 12px 20px;
  border-radius: 20px 20px 20px 0; /* Speech bubble tail */
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: all 0.3s;
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  font-size: 14px;
}

.chat-bubble-toggle::before {
  content: '';
  position: absolute;
  bottom: -2px;
  left: -18px;
  border-width: 0 16px 16px 0;
  border-style: solid;
  border-color: transparent #667eea transparent transparent;
}
.chat-bubble-toggle::after {
  content: '';
  position: absolute;
  bottom: 1px;
  left: -13px;
  border-width: 0 14px 14px 0;
  border-style: solid;
  border-color: transparent white transparent transparent;
}

.chat-bubble-toggle:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

.chat-bubble-toggle:hover::before {
  border-color: transparent #667eea transparent transparent;
}

.chat-bubble-toggle:hover::after {
  display: none; /* Hide white inner tail on hover */
}

.chat-bubble-toggle.has-notification {
  animation: bounce 2s infinite;
}

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

/* Chat Window as Speech Bubble */
.chat-window.speech-bubble {
  position: fixed;
  bottom: 80px;
  left: 250px; /* Right next to Haru */
  width: 380px;
  height: 500px;
  background: white;
  border-radius: 20px 20px 20px 0; /* Flat bottom-left corner for the tail */
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  border: 2px solid #667eea;
}

/* Tail for speech bubble */
.chat-window.speech-bubble::before {
  content: '';
  position: absolute;
  bottom: -2px;
  left: -22px;
  border-width: 0 20px 20px 0;
  border-style: solid;
  border-color: transparent #667eea transparent transparent;
}
.chat-window.speech-bubble::after {
  content: '';
  position: absolute;
  bottom: 1px;
  left: -17px;
  border-width: 0 18px 18px 0;
  border-style: solid;
  border-color: transparent white transparent transparent;
}

/* Close Button */
.close-btn {
  position: absolute;
  top: 10px;
  right: 15px;
  background: none;
  border: none;
  font-size: 18px;
  color: #a0a0a0;
  cursor: pointer;
  z-index: 10;
}
.close-btn:hover {
  color: #ff4757;
}

/* Chat Messages */
.chat-messages {
  flex: 1;
  padding: 20px;
  padding-top: 35px; /* space for close btn */
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

/* Custom Scrollbar */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}
.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
}
.chat-messages::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 10px;
}
.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* Welcome Message */
.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 10px;
}

.welcome-content {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 15px;
  border-top-left-radius: 0;
  color: #333;
}

.welcome-content h3 {
  margin: 0 0 10px 0;
  color: #667eea;
  font-size: 16px;
}

.welcome-content p {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.welcome-content ul {
  margin: 0;
  padding-left: 0;
  list-style: none;
  font-size: 13px;
  line-height: 1.6;
}

/* Messages */
.message {
  display: flex;
  flex-direction: column;
  max-width: 85%;
}

.message.user {
  align-self: flex-end;
}

.message.bot {
  align-self: flex-start;
}

.message-content {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.5;
  position: relative;
  word-break: break-word;
}

.message-bubble p {
  margin: 0;
}

.user .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.bot .message-bubble {
  background: #f1f3f5;
  color: #333;
  border-bottom-left-radius: 4px;
}

.message-time {
  display: block;
  font-size: 10px;
  margin-top: 4px;
  opacity: 0.7;
  text-align: right;
}

/* Typing Indicator */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 8px;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Chat Bottom Area */
.chat-bottom {
  padding: 15px;
  background: white;
  border-top: 1px solid #eee;
  border-radius: 0 0 20px 0;
}

/* Quick Actions (Prompts) */
.quick-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.quick-actions::-webkit-scrollbar {
  height: 0px;
}

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

.quick-btn:hover {
  background: #e4e6eb;
  color: #667eea;
}

.quick-btn i {
  color: #667eea;
}

/* Input Row */
.input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.message-input {
  flex: 1;
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.message-input:focus {
  border-color: #667eea;
}

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

.send-btn:hover:not(:disabled) {
  background: #5a67d8;
  transform: scale(1.05);
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* Scrollbar */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #667eea;
  border-radius: 3px;
}

/* Responsive */
@media (max-width: 480px) {
  .chat-window {
    width: calc(100vw - 20px);
    right: 10px;
  }
  
  .chat-toggle-btn {
    min-width: 60px;
    border-radius: 50%;
    padding: 15px;
  }
  
  .chat-label {
    display: none;
  }
}
</style>