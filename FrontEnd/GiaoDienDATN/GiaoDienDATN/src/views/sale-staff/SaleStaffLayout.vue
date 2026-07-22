<!-- views/sale-staff/SaleStaffLayout.vue -->
<template>
  <div class="sale-staff-layout">
    <SaleStaffSidebar />
    
    <div class="sale-staff-main">
      <SaleStaffHeader @toggle-menu="toggleSidebar" />
      
      <div class="sale-staff-content">
        <router-view />
      </div>
      
      <SaleStaffFooter />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SaleStaffSidebar from '../../components/sale-staff/SaleStaffSidebar.vue'
import SaleStaffHeader from '../../components/sale-staff/SaleStaffHeader.vue'
import SaleStaffFooter from '../../components/sale-staff/SaleStaffFooter.vue'

const sidebarCollapsed = ref(false)

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<style scoped>
.sale-staff-layout {
  display: flex;
  min-height: 100vh;
  background: #f8f9fa;
}

.sale-staff-main {
  flex: 1;
  margin-left: 280px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.sale-staff-content {
  flex: 1;
  padding: 30px;
}

@media (max-width: 768px) {
  .sale-staff-main {
    margin-left: 80px;
  }

  .sale-staff-content {
    padding: 20px;
  }
}
</style>