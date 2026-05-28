<template>
  <aside class="admin-sidebar">
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
import { useNotification } from '../../composables/useNotification'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { showNotification } = useNotification()

const menuSections = [
  {
    title: 'MAIN',
    items: [
      { name: 'Dashboard', path: '/admin', icon: 'fas fa-tachometer-alt' },
      { name: 'Sản phẩm', path: '/admin/products', icon: 'fas fa-tshirt' },
      { name: 'Đơn hàng', path: '/admin/orders', icon: 'fas fa-shopping-cart' },
      { name: 'Khách hàng', path: '/admin/customers', icon: 'fas fa-users' }
    ]
  },
  {
    title: 'MANAGE',
    items: [
      { name: 'Danh mục', path: '/admin/categories', icon: 'fas fa-tags' },
      { name: 'Quản lý hình ảnh', path: '/admin/images', icon: 'fas fa-images' },
      { name: 'Quản lý size', path: '/admin/sizes', icon: 'fas fa-ruler-combined' },
      { name: 'Kho hàng', path: '/admin/inventory', icon: 'fas fa-boxes' },
      { name: 'Đánh giá', path: '/admin/reviews', icon: 'fas fa-star' },
      { name: 'Khuyến mãi', path: '/admin/discounts', icon: 'fas fa-percent' }
    ]
  },
  {
    title: 'SYSTEM',
    items: [
      { name: 'Cài đặt', path: '/admin/settings', icon: 'fas fa-cog' },
      { name: 'Báo cáo', path: '/admin/reports', icon: 'fas fa-chart-bar' }
    ]
  },

]

const isActive = (path) => {
  return route.path === path
}

const handleLogout = () => {
  if (confirm('Bạn có chắc muốn đăng xuất?')) {
    authStore.logout()
    showNotification('Đã đăng xuất thành công!', 'success')
    router.push('/')
  }
}
</script>

<style scoped>
.admin-sidebar {
  width: 280px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: white;
  position: fixed;
  height: 100vh;
  display: flex;
  flex-direction: column;
  transition: all 0.3s;
  box-shadow: 2px 0 10px rgba(0,0,0,0.1);
}

.sidebar-header {
  padding: 30px 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.sidebar-header h2 {
  font-size: 28px;
  margin-bottom: 5px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
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
  color: rgba(255,255,255,0.8);
  text-decoration: none;
  transition: all 0.3s;
  position: relative;
}

.menu-item:hover,
.menu-item.active {
  background: linear-gradient(90deg, rgba(230, 57, 70, 0.2), transparent);
  color: #e63946;
  padding-left: 25px;
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 3px;
  background: #e63946;
}

.menu-item i {
  width: 20px;
  font-size: 16px;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255,255,255,0.1);
}

.logout-btn {
  width: 100%;
  padding: 12px;
  background: none;
  border: 1px solid rgba(255,255,255,0.2);
  color: white;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s;
}

.logout-btn:hover {
  background: rgba(230, 57, 70, 0.2);
  border-color: #e63946;
}

@media (max-width: 768px) {
  .admin-sidebar {
    width: 80px;
  }

  .sidebar-header h2,
  .sidebar-header p,
  .menu-item span,
  .menu-category {
    display: none;
  }

  .menu-item {
    justify-content: center;
    padding: 15px;
  }

  .menu-item:hover {
    padding-left: 15px;
  }

  .menu-item i {
    margin: 0;
    font-size: 20px;
  }

  .logout-btn span {
    display: none;
  }
}
</style>