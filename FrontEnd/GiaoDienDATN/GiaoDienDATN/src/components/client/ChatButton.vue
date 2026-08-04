<!-- components/client/ChatButton.vue -->
<template>
  <div class="chat-button-wrapper">
    <button class="chat-btn" @click="openChatModal">
      <i class="fas fa-comment-dots"></i>
      <span>Chat hỗ trợ</span>
    </button>

    <ChatCategoryModal
      :visible="showModal"
      @close="showModal = false"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../../stores/authStore'
import { useRouter } from 'vue-router'
import ChatCategoryModal from './ChatCategoryModal.vue'

const authStore = useAuthStore()
const router = useRouter()
const showModal = ref(false)

const openChatModal = () => {
  if (!authStore.isAuthenticated) {
    router.push({ 
      name: 'login', 
      query: { redirect: '/chat' } 
    })
    return
  }
  showModal.value = true
}
</script>

<style scoped>
.chat-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #e63946;
  color: white;
  border: none;
  border-radius: 50px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(230, 57, 70, 0.3);
}

.chat-btn:hover {
  background: #c62828;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(230, 57, 70, 0.4);
}

.chat-btn i {
  font-size: 18px;
}
</style>