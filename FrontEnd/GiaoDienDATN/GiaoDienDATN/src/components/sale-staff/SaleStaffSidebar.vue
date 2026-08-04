<!-- components/sale-staff/SaleStaffSidebar.vue -->
<template>
  <aside class="sale-staff-sidebar">
    <div class="sidebar-header">
      <h2>FASHION</h2>
      <p>Nhân viên bán hàng</p>
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
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/authStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menuSections = [
  {
    title: 'MAIN',
    items: [
      { name: 'Dashboard', path: '/sale-staff', icon: 'fas fa-tachometer-alt' },
      { name: 'Quản lý đơn hàng', path: '/sale-staff/orders', icon: 'fas fa-shopping-cart' },
      { name: 'Quản lý khách hàng', path: '/sale-staff/customers', icon: 'fas fa-users' },
      { name: 'Quản lý tồn kho', path: '/sale-staff/stock', icon: 'fas fa-boxes' },
    ]
  },
  {
  title: 'SUPPORT',
  items: [
    { name: 'Chat hỗ trợ', path: '/sale-staff/chat', icon: 'fas fa-comments' },
  ]
}
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
.sale-staff-sidebar {
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

@media (max-width: 768px) {
  .sale-staff-sidebar {
    width: 80px;
  }

  .sidebar-header h2 {
    font-size: 16px;
  }

  .sidebar-header p {
    display: none;
  }

  .menu-category {
    display: none;
  }

  .menu-item span {
    display: none;
  }

  .menu-item {
    justify-content: center;
    padding: 15px;
  }

  .menu-item:hover {
    padding-left: 15px;
  }

  .menu-item.active {
    padding-left: 15px;
  }

  .logout-btn span {
    display: none;
  }
}
</style>