<template>
  <div class="staff-layout">
    <StaffSidebar />

    <div class="staff-main">
      <StaffHeader @toggle-menu="toggleSidebar" />

      <div class="staff-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import StaffHeader from "../../components/staff/StaffHeader.vue";
import StaffSidebar from "../../components/staff/StaffSidebar.vue";

const sidebarCollapsed = ref(false);

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value;
};
</script>

<style scoped>
.staff-layout {
  display: flex;
  min-height: 100vh;
  background: var(--light-gray);
}

.staff-main {
  flex: 1;
  margin-left: 280px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.staff-content {
  flex: 1;
  padding: 30px;
}

@media (max-width: 768px) {
  .staff-main {
    margin-left: 80px;
  }

  .staff-content {
    padding: 20px;
  }
}
</style>
