<!-- components/sale-staff/SaleStaffHeader.vue -->
<template>
  <header class="sale-staff-header">
    <div class="header-left">
      <button class="menu-toggle" @click="$emit('toggle-menu')">
        <i class="fas fa-bars"></i>
      </button>
      <h1 class="page-title">{{ pageTitle }}</h1>
    </div>

    <div class="header-right">
      <button class="refresh-btn" @click="refreshData" title="Làm mới dữ liệu">
        <i class="fas fa-sync-alt"></i>
      </button>

      <div class="profile-dropdown" ref="profileRef">
        <button class="profile-btn" @click="toggleProfile">
          <div class="profile-avatar">
            {{ userInitials }}
          </div>
          <span class="profile-name">{{ userName }}</span>
          <i class="fas fa-chevron-down"></i>
        </button>

        <Transition name="dropdown">
          <div v-if="showProfile" class="dropdown-menu profile-menu">
            <div class="profile-header">
              <div class="profile-avatar-large">
                {{ userInitials }}
              </div>
              <div class="profile-info">
                <h4>{{ userName }}</h4>
                <p>{{ userEmail }}</p>
                <span class="role-badge">Nhân viên bán hàng</span>
              </div>
            </div>

            <div class="profile-links">
              <button @click="handleLogout">
                <i class="fas fa-sign-out-alt"></i> Đăng xuất
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/authStore'

const emit = defineEmits(['toggle-menu'])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const showProfile = ref(false)
const profileRef = ref(null)

const pageTitle = computed(() => {
  const path = route.path
  if (path.includes('orders')) return 'Quản lý đơn hàng'
  if (path.includes('customers')) return 'Quản lý khách hàng'
  if (path.includes('stock')) return 'Quản lý tồn kho'
  return 'Dashboard'
})

const userName = computed(() => {
  return authStore.user?.username || 'Nhân viên'
})

const userEmail = computed(() => {
  return authStore.user?.email || 'sale@fashionstore.com'
})

const userInitials = computed(() => {
  const name = userName.value
  return name
    .split(' ')
    .map(n => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
})

const toggleProfile = () => {
  showProfile.value = !showProfile.value
}

const refreshData = () => {
  window.location.reload()
}

const handleLogout = () => {
  if (confirm('Bạn có chắc muốn đăng xuất?')) {
    authStore.logout()
    router.push('/login')
  }
}

const handleClickOutside = (event) => {
  if (profileRef.value && !profileRef.value.contains(event.target)) {
    showProfile.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.sale-staff-header {
  background: white;
  padding: 15px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.menu-toggle {
  display: none;
  background: none;
  border: none;
  font-size: 20px;
  color: #333;
  cursor: pointer;
}

.page-title {
  font-size: 24px;
  color: #1a1a2e;
  font-weight: 600;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.refresh-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #666;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.3s;
}

.refresh-btn:hover {
  background: #f5f5f5;
  color: #e63946;
  transform: rotate(180deg);
}

.profile-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 8px;
  transition: all 0.3s;
}

.profile-btn:hover {
  background: #f5f5f5;
}

.profile-avatar {
  width: 35px;
  height: 35px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.profile-name {
  color: #1a1a2e;
  font-weight: 500;
}

.profile-btn i {
  color: #999;
  font-size: 12px;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 10px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
  min-width: 250px;
  z-index: 1000;
  overflow: hidden;
}

.profile-menu {
  width: 260px;
}

.profile-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  border-bottom: 1px solid #eee;
}

.profile-avatar-large {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 18px;
}

.profile-info h4 {
  color: #1a1a2e;
  font-size: 16px;
  margin-bottom: 3px;
}

.profile-info p {
  color: #999;
  font-size: 12px;
  margin-bottom: 5px;
}

.role-badge {
  display: inline-block;
  padding: 2px 10px;
  background: #e8f5e9;
  color: #2e7d32;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.profile-links {
  padding: 10px 0;
}

.profile-links button {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: #1a1a2e;
  text-decoration: none;
  transition: all 0.3s;
  width: 100%;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.profile-links button:hover {
  background: #f5f5f5;
  color: #e63946;
}

.profile-links i {
  width: 20px;
  color: #e63946;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.3s;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (max-width: 768px) {
  .sale-staff-header {
    padding: 15px 20px;
  }

  .menu-toggle {
    display: block;
  }

  .page-title {
    font-size: 18px;
  }

  .profile-name {
    display: none;
  }

  .profile-btn i.fa-chevron-down {
    display: none;
  }
}
</style>