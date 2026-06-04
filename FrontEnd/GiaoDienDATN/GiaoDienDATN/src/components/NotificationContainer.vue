<template>
  <Teleport to="body">
    <TransitionGroup name="notification">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        :class="['notification', `notification-${notification.type}`]"
      >
        {{ notification.message }}
        <button class="close-notification" @click="removeNotification(notification.id)">
          <i class="fas fa-times"></i>
        </button>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup>
import { useNotification } from '../composables/useNotification'

const { notifications, removeNotification } = useNotification()
</script>

<style scoped>
.notification {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
}

.close-notification {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 16px;
  opacity: 0.8;
  transition: opacity 0.3s;
}

.close-notification:hover {
  opacity: 1;
}

.notification-enter-active,
.notification-leave-active {
  transition: all 0.3s;
}

.notification-enter-from,
.notification-leave-to {
  opacity: 0;
  transform: translateX(50px);
}
</style>