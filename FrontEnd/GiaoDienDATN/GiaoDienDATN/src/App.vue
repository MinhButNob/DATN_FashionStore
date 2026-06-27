<template>
  <div id="app">
    <router-view />
    <NotificationContainer />
    <AuthModal />
    
    <!-- Haru AI Chatbot -->
    <HaruChatbot />
  </div>
</template>

<script setup>
import NotificationContainer from "./components/NotificationContainer.vue";
import AuthModal from "./components/client/AuthModal.vue";
import HaruChatbot from "./components/chatbot/HaruChatbot.vue";
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