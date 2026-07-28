<!-- components/sale-staff/SaleStaffFooter.vue -->
<template>
  <footer class="sale-staff-footer">
    <div class="footer-content">
      <div class="footer-left">
        <p>&copy; {{ currentYear }} Fashion Store. All rights reserved.</p>
      </div>
      <div class="footer-right">
        <span>Phiên bản 1.0.0</span>
        <span class="separator">|</span>
        <span>Nhân viên bán hàng</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.sale-staff-footer {
  background: white;
  padding: 15px 30px;
  border-top: 1px solid #eee;
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
  color: #999;
  font-size: 14px;
  margin: 0;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.footer-right span {
  color: #999;
  font-size: 14px;
}

.separator {
  color: #ddd;
}

@media (max-width: 768px) {
  .sale-staff-footer {
    padding: 15px 20px;
  }

  .footer-content {
    flex-direction: column;
    text-align: center;
  }
}
</style>