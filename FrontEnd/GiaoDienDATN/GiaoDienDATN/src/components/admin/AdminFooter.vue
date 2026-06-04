<template>
  <footer class="admin-footer">
    <div class="footer-content">
      <div class="footer-left">
        <p>&copy; {{ currentYear }} Fashion Store. All rights reserved.</p>
      </div>
      <div class="footer-right">
        <span>Phiên bản 1.0.0</span>
        <span class="separator">|</span>
        <span>Cập nhật lần cuối: 20/03/2024</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.admin-footer {
  background: white;
  padding: 15px 30px;
  border-top: 1px solid var(--border-color);
  margin-top: auto;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.footer-left p {
  color: var(--medium-gray);
  font-size: 14px;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.footer-right span {
  color: var(--medium-gray);
  font-size: 14px;
}

.separator {
  color: var(--border-color);
}

@media (max-width: 768px) {
  .admin-footer {
    padding: 15px 20px;
  }

  .footer-content {
    flex-direction: column;
    text-align: center;
  }

  .footer-right {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>