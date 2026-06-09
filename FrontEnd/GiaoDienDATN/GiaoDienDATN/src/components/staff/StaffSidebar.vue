<template>
  <aside class="staff-sidebar">
    <div class="sidebar-header">
      <h2>FASHION</h2>
      <p>Staff Workspace</p>
    </div>

    <nav class="sidebar-nav">
      <div v-for="section in menuSections" :key="section.title">
        <div class="menu-category">{{ section.title }}</div>
        <router-link
          v-for="item in section.items"
          :key="item.path"
          :to="item.path"
          class="menu-item"
          :class="{ active: isActive(item.path) }"
        >
          <i :class="item.icon"></i>
          <span>{{ item.name }}</span>
        </router-link>
      </div>
    </nav>

    <div class="sidebar-footer">
      <button class="logout-btn" @click="handleLogout">
        <i class="fas fa-sign-out-alt"></i>
        <span>Đăng xuất</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../../stores/authStore";
import { useNotification } from "../../composables/useNotification";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { showNotification } = useNotification();

const menuSections = [
  {
    title: "MAIN",
    items: [
      { name: "Thống kê hôm nay", path: "/staff", icon: "fas fa-chart-line" },
      { name: "Bán hàng tại quầy", path: "/staff/pos", icon: "fas fa-cash-register" },
      { name: "Đơn hàng", path: "/staff/orders", icon: "fas fa-shopping-cart" },
    ],
  },
  {
    title: "WORK",
    items: [
      { name: "Khách hàng", path: "/staff/customers", icon: "fas fa-users" },
      { name: "Đổi trả", path: "/staff/returns", icon: "fas fa-undo-alt" },
      { name: "Chat khách hàng", path: "/staff/chat", icon: "fas fa-comments" },
    ],
  },
];

const isActive = (path) => route.path === path;

const handleLogout = () => {
  if (confirm("Bạn có chắc muốn đăng xuất?")) {
    authStore.logout();
    showNotification("Đã đăng xuất thành công!", "success");
    router.push("/login");
  }
};
</script>

<style scoped>
.staff-sidebar {
  width: 280px;
  background: linear-gradient(180deg, #172033 0%, #20263a 100%);
  color: white;
  position: fixed;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
}

.sidebar-header {
  padding: 30px 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-header h2 {
  font-size: 28px;
  margin-bottom: 5px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sidebar-header p {
  font-size: 14px;
  opacity: 0.7;
}

.sidebar-nav {
  flex: 1;
  padding: 20px 0;
  overflow-y: auto;
}

.menu-category {
  padding: 10px 20px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 1px;
  opacity: 0.6;
  margin-top: 10px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: rgba(255, 255, 255, 0.82);
  text-decoration: none;
  transition: var(--transition);
  position: relative;
}

.menu-item:hover,
.menu-item.active {
  background: linear-gradient(90deg, rgba(230, 57, 70, 0.2), transparent);
  color: var(--primary-red);
  padding-left: 25px;
}

.menu-item.active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: var(--primary-red);
}

.menu-item i {
  width: 20px;
  font-size: 16px;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.logout-btn {
  width: 100%;
  padding: 12px;
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: var(--transition);
}

.logout-btn:hover {
  background: rgba(230, 57, 70, 0.2);
  border-color: var(--primary-red);
}

@media (max-width: 768px) {
  .staff-sidebar {
    width: 80px;
  }

  .sidebar-header h2,
  .sidebar-header p,
  .menu-item span,
  .menu-category,
  .logout-btn span {
    display: none;
  }

  .menu-item {
    justify-content: center;
    padding: 15px;
  }

  .menu-item:hover {
    padding-left: 15px;
  }
}
</style>
