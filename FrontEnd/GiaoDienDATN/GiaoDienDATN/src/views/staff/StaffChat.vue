<template>
  <div class="staff-chat">
    <aside class="conversation-list">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input v-model="search" type="text" placeholder="Tìm cuộc trò chuyện" />
      </div>

      <button
        v-for="conversation in filteredConversations"
        :key="conversation.id"
        class="conversation-item"
        :class="{ active: selectedConversation.id === conversation.id }"
        @click="selectedConversation = conversation"
      >
        <div class="avatar">{{ conversation.customer.charAt(0) }}</div>
        <div class="conversation-info">
          <div class="conversation-top">
            <strong>{{ conversation.customer }}</strong>
            <span>{{ conversation.time }}</span>
          </div>
          <p>{{ conversation.lastMessage }}</p>
        </div>
        <span v-if="conversation.unread" class="unread-dot">{{ conversation.unread }}</span>
      </button>
    </aside>

    <section class="chat-panel">
      <div class="chat-header">
        <div class="avatar">{{ selectedConversation.customer.charAt(0) }}</div>
        <div>
          <h3>{{ selectedConversation.customer }}</h3>
          <p>{{ selectedConversation.phone }} · {{ selectedConversation.status }}</p>
        </div>
      </div>

      <div class="messages">
        <div v-for="message in selectedConversation.messages" :key="message.id" class="message-row" :class="message.from">
          <div class="message-bubble">
            <p>{{ message.text }}</p>
            <span>{{ message.time }}</span>
          </div>
        </div>
      </div>

      <div class="reply-box">
        <button title="Đính kèm"><i class="fas fa-paperclip"></i></button>
        <input v-model="reply" type="text" placeholder="Nhập tin nhắn trả lời khách hàng" @keyup.enter="sendReply" />
        <button class="send-btn" title="Gửi" @click="sendReply"><i class="fas fa-paper-plane"></i></button>
      </div>
    </section>

    <aside class="customer-panel">
      <h3>Thông tin khách</h3>
      <div class="info-row"><span>Số điện thoại</span><strong>{{ selectedConversation.phone }}</strong></div>
      <div class="info-row"><span>Đơn gần nhất</span><strong>{{ selectedConversation.order }}</strong></div>
      <div class="info-row"><span>Tổng đơn</span><strong>{{ selectedConversation.totalOrders }}</strong></div>
      <div class="info-row"><span>Hạng</span><strong>{{ selectedConversation.level }}</strong></div>

      <h3 class="mt">Gợi ý phản hồi</h3>
      <button v-for="template in templates" :key="template" class="template-btn" @click="reply = template">
        {{ template }}
      </button>
    </aside>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useNotification } from "../../composables/useNotification";

const { showNotification } = useNotification();
const search = ref("");
const reply = ref("");

const conversations = ref([
  {
    id: 1,
    customer: "Nguyễn Minh Anh",
    phone: "0901122334",
    status: "Đang online",
    order: "DH1024",
    totalOrders: 8,
    level: "VIP",
    time: "09:42",
    unread: 2,
    lastMessage: "Shop kiểm tra giúp em đơn hàng với ạ",
    messages: [
      { id: 1, from: "customer", text: "Shop kiểm tra giúp em đơn hàng với ạ", time: "09:38" },
      { id: 2, from: "staff", text: "Dạ em kiểm tra ngay cho mình nhé.", time: "09:40" },
      { id: 3, from: "customer", text: "Em cảm ơn shop.", time: "09:42" },
    ],
  },
  {
    id: 2,
    customer: "Trần Quốc Bảo",
    phone: "0919988776",
    status: "Hoạt động 12 phút trước",
    order: "DH1025",
    totalOrders: 3,
    level: "Thân thiết",
    time: "08:15",
    unread: 0,
    lastMessage: "Mẫu áo này còn size L không?",
    messages: [
      { id: 1, from: "customer", text: "Mẫu áo này còn size L không?", time: "08:15" },
    ],
  },
]);

const selectedConversation = ref(conversations.value[0]);
const templates = [
  "Dạ shop đang kiểm tra và phản hồi mình ngay ạ.",
  "Dạ sản phẩm này hiện còn hàng tại cửa hàng.",
  "Dạ mình cho shop xin mã đơn hàng để kiểm tra nhanh hơn ạ.",
];

const filteredConversations = computed(() => {
  const keyword = search.value.toLowerCase();
  return conversations.value.filter((item) => item.customer.toLowerCase().includes(keyword) || item.phone.includes(keyword));
});

const sendReply = () => {
  if (!reply.value.trim()) return;
  selectedConversation.value.messages.push({
    id: Date.now(),
    from: "staff",
    text: reply.value,
    time: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
  });
  selectedConversation.value.lastMessage = reply.value;
  reply.value = "";
  showNotification("Đã gửi tin nhắn", "success");
};
</script>

<style scoped>
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
  box-shadow: var(--shadow);
  overflow: hidden;
}

.conversation-list {
  padding: 16px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--light-gray);
  border-radius: 8px;
  padding: 0 12px;
  margin-bottom: 14px;
}

.search-box input {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  padding: 12px 0;
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
}

.conversation-item.active,
.conversation-item:hover {
  background: rgba(230, 57, 70, 0.08);
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
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
  color: var(--dark-gray);
  font-size: 14px;
}

.conversation-top span,
.conversation-info p,
.chat-header p,
.message-bubble span,
.info-row span {
  color: var(--medium-gray);
  font-size: 12px;
}

.conversation-info p {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.unread-dot {
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  background: var(--primary-red);
  color: white;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-panel {
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 18px 22px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-header h3,
.customer-panel h3 {
  color: var(--dark-gray);
  font-size: 17px;
}

.messages {
  flex: 1;
  padding: 22px;
  background: var(--light-gray);
  overflow-y: auto;
}

.message-row {
  display: flex;
  margin-bottom: 12px;
}

.message-row.staff {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 14px;
  border-radius: 12px;
  background: white;
  color: var(--dark-gray);
}

.message-row.staff .message-bubble {
  background: var(--primary-red);
  color: white;
}

.message-row.staff .message-bubble span {
  color: rgba(255, 255, 255, 0.75);
}

.reply-box {
  padding: 16px;
  display: grid;
  grid-template-columns: 40px 1fr 44px;
  gap: 10px;
  border-top: 1px solid var(--border-color);
}

.reply-box button,
.reply-box input {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: white;
}

.reply-box input {
  padding: 0 14px;
  outline: none;
}

.reply-box .send-btn {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
}

.customer-panel {
  padding: 22px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.info-row strong {
  color: var(--dark-gray);
  text-align: right;
}

.mt {
  margin-top: 24px;
  margin-bottom: 12px;
}

.template-btn {
  width: 100%;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: 8px;
  padding: 10px;
  text-align: left;
  cursor: pointer;
  margin-bottom: 10px;
  color: var(--dark-gray);
}

.template-btn:hover {
  border-color: var(--primary-red);
  color: var(--primary-red);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
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
}
</style>
