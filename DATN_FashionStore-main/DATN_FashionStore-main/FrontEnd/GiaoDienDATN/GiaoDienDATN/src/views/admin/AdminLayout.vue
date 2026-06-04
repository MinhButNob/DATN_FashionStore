<template>
  <div class="admin-layout">
    <AdminSidebar />
    
    <div class="admin-main">
      <AdminHeader @toggle-menu="toggleSidebar" />
      
      <div class="admin-content">
        <router-view />
      </div>

      <AdminFooter />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminHeader from '../../components/admin/AdminHeader.vue'
import AdminFooter from '../../components/admin/AdminFooter.vue'

const sidebarCollapsed = ref(false)

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  // You can add logic to collapse sidebar if needed
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--light-gray);
}

.admin-main {
  flex: 1;
  margin-left: 280px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.admin-content {
  flex: 1;
  padding: 30px;
}

@media (max-width: 768px) {
  .admin-main {
    margin-left: 80px;
  }

  .admin-content {
    padding: 20px;
  }
}
</style>