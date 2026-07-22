<template>
  <aside class="admin-sidebar" :class="{ collapsed: sidebarCollapsed }">
    <div class="sidebar-header">
      <h2>FASHION</h2>
      <p>Admin Panel</p>
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
          <span v-if="item.badge" class="menu-badge">{{ item.badge }}</span>
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
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const sidebarCollapsed = ref(false)
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// Menu items cho sidebar admin
const menuSections = [
  {
    title: "MAIN",
    items: [
      { name: "Dashboard", path: "/admin", icon: "fas fa-tachometer-alt" },
      { name: "Quản lý sản phẩm", path: "/admin/products", icon: "fas fa-tshirt" },
      { name: "Quản lý đơn hàng", path: "/admin/orders", icon: "fas fa-shopping-cart" },
      { name: "Quản lý khách hàng", path: "/admin/customers", icon: "fas fa-users" },
    ],
  },
  {
    title: "MANAGE",
    items: [
      { name: "Quản lý danh mục", path: "/admin/categories", icon: "fas fa-tags" },
      { name: "Quản lý nhân viên", path: "/admin/employees", icon: "fas fa-user-tie" },
      { name: "Quản lý vai trò", path: "/admin/roles", icon: "fas fa-user-shield" },
      { name: "Quản lý hình ảnh", path: "/admin/images", icon: "fas fa-images" },
      { name: "Quản lý banner", path: "/admin/banners", icon: "fas fa-images" },
      { name: "Quản lý size", path: "/admin/sizes", icon: "fas fa-ruler-combined" },
      { name: "Quản lý màu sắc", path: "/admin/colors", icon: "fas fa-boxes" },
      { name: "Quản lý đánh giá", path: "/admin/reviews", icon: "fas fa-star" },
      { name: "Quản lý khuyến mãi", path: "/admin/promotions", icon: "fas fa-percent" },
      { name: "Voucher", path: "/admin/vouchers", icon: "fas fa-ticket-alt" },
    ],
  },
  {
    title: "REPORTS & SYSTEM",
    items: [
      { 
        name: "Báo cáo thống kê", 
        path: "/admin/statistics", 
        icon: "fas fa-chart-pie",
        badge: "Mới"
      },
      { name: "Cài đặt", path: "/admin/settings", icon: "fas fa-cog" },
    ],
  },
]

const isActive = (path) => {
  if (route.path === path) return true
  if (route.path.startsWith(path + '/')) return true
  return false
}

const handleLogout = () => {
  if (confirm('Bạn có chắc muốn đăng xuất?')) {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-sidebar {
  width: 280px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: white;
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  overflow: hidden;
}

.sidebar-header {
  padding: 30px 20px 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.sidebar-header h2 {
  font-size: 28px;
  margin: 0;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 800;
  letter-spacing: 2px;
}

.sidebar-header p {
  font-size: 13px;
  opacity: 0.5;
  margin: 4px 0 0;
  text-transform: uppercase;
  letter-spacing: 2px;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 0 20px;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.menu-category {
  padding: 12px 24px 6px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  opacity: 0.4;
  font-weight: 600;
  color: #a0aec0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 24px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  transition: all 0.25s ease;
  position: relative;
  border-left: 3px solid transparent;
  font-size: 14px;
  cursor: pointer;
}

.menu-item:hover {
  background: rgba(230, 57, 70, 0.08);
  color: #ffffff;
  padding-left: 28px;
}

.menu-item.active {
  background: rgba(230, 57, 70, 0.12);
  color: #ffffff;
  border-left-color: #e63946;
  padding-left: 28px;
}

.menu-item.active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: #e63946;
  border-radius: 0 2px 2px 0;
}

.menu-item i {
  width: 20px;
  font-size: 16px;
  text-align: center;
  flex-shrink: 0;
  color: inherit;
  opacity: 0.7;
}

.menu-item.active i {
  opacity: 1;
  color: #e63946;
}

.menu-item span {
  flex: 1;
  white-space: nowrap;
}

.menu-badge {
  background: #e63946;
  color: white;
  font-size: 10px;
  padding: 1px 8px;
  border-radius: 12px;
  font-weight: 600;
  animation: pulseBadge 2s ease-in-out infinite;
}

@keyframes pulseBadge {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

.sidebar-footer {
  padding: 16px 20px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.logout-btn {
  width: 100%;
  padding: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: all 0.3s ease;
  font-size: 14px;
}

.logout-btn:hover {
  background: rgba(230, 57, 70, 0.15);
  border-color: rgba(230, 57, 70, 0.3);
  color: #ffffff;
}

.logout-btn i {
  font-size: 16px;
}

@media (max-width: 768px) {
  .admin-sidebar {
    width: 0;
    transform: translateX(-100%);
  }

  .admin-sidebar.open {
    width: 280px;
    transform: translateX(0);
  }

  .admin-sidebar.open::after {
    content: '';
    position: fixed;
    top: 0;
    left: 280px;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: -1;
  }

  .sidebar-header h2 {
    font-size: 24px;
  }

  .menu-item {
    padding: 10px 20px;
    font-size: 13px;
  }

  .menu-item:hover {
    padding-left: 24px;
  }

  .menu-item.active {
    padding-left: 24px;
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.menu-item {
  animation: slideIn 0.3s ease forwards;
}

.menu-item:nth-child(1) { animation-delay: 0.05s; }
.menu-item:nth-child(2) { animation-delay: 0.10s; }
.menu-item:nth-child(3) { animation-delay: 0.15s; }
.menu-item:nth-child(4) { animation-delay: 0.20s; }
.menu-item:nth-child(5) { animation-delay: 0.25s; }
.menu-item:nth-child(6) { animation-delay: 0.30s; }
</style>