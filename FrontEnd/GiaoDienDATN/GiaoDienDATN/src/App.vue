<template>
  <div id="app">
    <router-view />
    <NotificationContainer />
    <AuthModal />
    <ConfirmDialog
      :visible="confirm.visible.value"
      :title="confirm.title.value"
      :message="confirm.message.value"
      :confirm-text="confirm.confirmText.value"
      :loading="confirm.loading.value"
      @confirm="confirm.accept"
      @cancel="confirm.cancel"
    />
    
    <!-- Haru AI Chatbot -->
    <HaruChatbot v-if="!authStore.isAdmin && !authStore.isStaff" />
  </div>
</template>

<script setup>
import NotificationContainer from "./components/NotificationContainer.vue";
import AuthModal from "./components/client/AuthModal.vue";
import HaruChatbot from "./components/chatbot/HaruChatbot.vue";
import { useAuthStore } from "./stores/authStore";
import ConfirmDialog from "./components/admin/ConfirmDialog.vue";
import { useConfirm } from "./composables/useConfirm";

const authStore = useAuthStore();
const confirm = useConfirm();
// Legacy handlers use this async facade while migrating to the shared dialog.
// The facade keeps all confirmation UI in ConfirmDialog and prevents double submits.
window.appConfirm = (text, options = {}) => confirm.confirm({
  message: text,
  ...options
}).then((accepted) => {
  if (accepted) confirm.close();
  return accepted;
});
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  min-height: 100vh;
  background: #f5f6fa;
}
</style>
