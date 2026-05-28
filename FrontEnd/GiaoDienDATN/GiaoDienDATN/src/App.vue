<template>
  <div id="app">
    <router-view />
    <NotificationContainer />
    <AuthModal />
  </div>
</template>

<script setup>
import NotificationContainer from "./components/NotificationContainer.vue";
import AuthModal from "./components/client/AuthModal.vue";
</script>
