<template>
  <header>
    <div class="header-top">
      <div class="container header-container">
        <div class="logo" @click="goToHome">
          FASHION STORE
        </div>

        <div class="search-box">
          <input
            type="text"
            v-model="searchQuery"
            @keyup.enter="handleSearch"
            placeholder="Tìm kiếm sản phẩm..."
          >
          <button @click="handleSearch">
            <i class="fas fa-search"></i>
          </button>
        </div>

        <div class="nav-icons">
          <div class="icon-wrapper" @click="goToWishlist">
            <i class="far fa-heart"></i>
            <span v-if="wishlistCount > 0" class="badge">{{ wishlistCount }}</span>
          </div>

          <div class="user-menu" ref="userMenu">
            <i class="far fa-user" @click="toggleUserMenu"></i>
            <Transition name="dropdown">
              <div v-if="showUserMenu" class="user-dropdown">
                <template v-if="!authStore.isAuthenticated">
                  <a href="#" @click.prevent="showLoginModal">
                    <i class="fas fa-sign-in-alt"></i> Đăng nhập
                  </a>
                  <a href="#" @click.prevent="showRegisterModal">
                    <i class="fas fa-user-plus"></i> Đăng ký
                  </a>
                </template>
                <template v-else>
                  <router-link to="/profile" @click="closeUserMenu">
                    <i class="fas fa-user-circle"></i> Tài khoản
                  </router-link>
                  <router-link to="/profile?tab=orders" @click="closeUserMenu">
                    <i class="fas fa-shopping-bag"></i> Đơn hàng
                  </router-link>
                  <a href="#" @click.prevent="logout">
                    <i class="fas fa-sign-out-alt"></i> Đăng xuất
                  </a>
                </template>
              </div>
            </Transition>
          </div>

          <div class="icon-wrapper" @click="cartStore.toggleCart">
            <i class="fas fa-shopping-cart"></i>
            <span v-if="cartStore.totalItems > 0" class="badge">
              {{ cartStore.totalItems }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <nav class="nav-menu">
      <div class="container">
        <ul>
          <li v-for="item in menuItems" :key="item.path">
            <router-link
              :to="item.path"
              :class="{ active: isActive(item.path) }"
              @click="closeDropdowns"
            >
              <i :class="item.icon"></i>
              {{ item.name }}
              <i v-if="item.children" class="fas fa-chevron-down"></i>
            </router-link>
            
            <Transition v-if="item.children" name="dropdown">
              <div v-if="isDropdownOpen(item.path)" class="dropdown-menu">
                <router-link
                  v-for="child in item.children"
                  :key="child.path"
                  :to="child.path"
                  @click="closeDropdowns"
                >
                  <i :class="child.icon"></i>
                  {{ child.name }}
                </router-link>
              </div>
            </Transition>
          </li>
        </ul>
      </div>
    </nav>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useProductStore } from '../../stores/productStore'
import { useCartStore } from '../../stores/cartStore'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const route = useRoute()
const productStore = useProductStore()
const cartStore = useCartStore()
const authStore = useAuthStore()

const searchQuery = ref('')
const showUserMenu = ref(false)
const openDropdown = ref(null)
const userMenu = ref(null)

const wishlistCount = ref(0)

const menuItems = [
  { path: '/', name: 'Trang chủ', icon: 'fas fa-home' },
  {
    path: '/shop?category=men',
    name: 'Nam',
    icon: 'fas fa-male',
    children: [
      { path: '/shop?category=men&subcategory=ao', name: 'Áo nam', icon: 'fas fa-tshirt' },
      { path: '/shop?category=men&subcategory=quan', name: 'Quần nam', icon: 'fas fa-vest' },
      { path: '/shop?category=men&subcategory=giay', name: 'Giày nam', icon: 'fas fa-shoe-prints' },
      { path: '/shop?category=men&subcategory=phukien', name: 'Phụ kiện nam', icon: 'fas fa-glasses' }
    ]
  },
  {
    path: '/shop?category=women',
    name: 'Nữ',
    icon: 'fas fa-female',
    children: [
      { path: '/shop?category=women&subcategory=dam', name: 'Đầm nữ', icon: 'fas fa-dress' },
      { path: '/shop?category=women&subcategory=ao', name: 'Áo nữ', icon: 'fas fa-tshirt' },
      { path: '/shop?category=women&subcategory=quan', name: 'Quần nữ', icon: 'fas fa-vest' },
      { path: '/shop?category=women&subcategory=phukien', name: 'Phụ kiện nữ', icon: 'fas fa-handbag' }
    ]
  },
  {
    path: '/shop?category=kids',
    name: 'Trẻ em',
    icon: 'fas fa-child',
    children: [
      { path: '/shop?category=kids&subcategory=be-trai', name: 'Bé trai', icon: 'fas fa-boy' },
      { path: '/shop?category=kids&subcategory=be-gai', name: 'Bé gái', icon: 'fas fa-girl' },
      { path: '/shop?category=kids&subcategory=so-sinh', name: 'Sơ sinh', icon: 'fas fa-baby' }
    ]
  },
  {
    path: '/shop?category=accessories',
    name: 'Phụ kiện',
    icon: 'fas fa-bag-shopping',
    children: [
      { path: '/shop?category=accessories&subcategory=tui', name: 'Túi xách', icon: 'fas fa-bag-shopping' },
      { path: '/shop?category=accessories&subcategory=giay', name: 'Giày dép', icon: 'fas fa-shoe-prints' },
      { path: '/shop?category=accessories&subcategory=dongho', name: 'Đồng hồ', icon: 'fas fa-clock' },
      { path: '/shop?category=accessories&subcategory=kinh', name: 'Kính mát', icon: 'fas fa-glasses' },
      { path: '/shop?category=accessories&subcategory=trangsuc', name: 'Trang sức', icon: 'fas fa-gem' }
    ]
  },
  { path: '/shop?tag=sale', name: 'Khuyến mãi', icon: 'fas fa-tags' },
  { path: '/blog', name: 'Blog', icon: 'fas fa-blog' },
  { path: '/about', name: 'Về chúng tôi', icon: 'fas fa-info-circle' }
]

const isActive = (path) => {
  if (path.includes('?')) {
    const [base, query] = path.split('?')
    const params = new URLSearchParams(query)
    const category = params.get('category')
    return route.path === base && route.query.category === category
  }
  return route.path === path
}

const isDropdownOpen = (path) => {
  return openDropdown.value === path
}

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const closeUserMenu = () => {
  showUserMenu.value = false
}

const closeDropdowns = () => {
  openDropdown.value = null
  closeUserMenu()
}

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    productStore.setFilter('search', searchQuery.value)
    router.push(`/shop?search=${searchQuery.value}`)
  }
}

const goToHome = () => {
  router.push('/')
  closeDropdowns()
}

const goToWishlist = () => {
  if (authStore.isAuthenticated) {
    router.push('/wishlist')
  } else {
    showLoginModal()
  }
  closeDropdowns()
}

const showLoginModal = () => {
  // Emit event to open login modal
  window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'login' }))
  closeUserMenu()
}

const showRegisterModal = () => {
  window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'register' }))
  closeUserMenu()
}

const logout = () => {
  authStore.logout()
  closeUserMenu()
  router.push('/')
}

// Handle click outside
const handleClickOutside = (event) => {
  if (userMenu.value && !userMenu.value.contains(event.target)) {
    showUserMenu.value = false
  }
  
  // Close dropdowns when clicking outside
  if (!event.target.closest('.has-dropdown')) {
    openDropdown.value = null
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
header {
  position: fixed;
  top: 0;
  width: 100%;
  z-index: 1000;
  background: white;
  box-shadow: var(--shadow);
}

.header-top {
  padding: 15px 0;
  border-bottom: 1px solid var(--border-color);
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.logo {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  cursor: pointer;
}

.search-box {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, var(--light-gray), #e9ecef);
  border-radius: 30px;
  padding: 5px 20px;
  width: 400px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.search-box input {
  border: none;
  background: none;
  padding: 12px;
  width: 100%;
  outline: none;
  font-size: 14px;
}

.search-box button {
  background: var(--primary-red);
  border: none;
  color: white;
  cursor: pointer;
  font-size: 16px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  transition: var(--transition);
}

.search-box button:hover {
  background: var(--dark-red);
  transform: scale(1.1);
}

.nav-icons {
  display: flex;
  gap: 25px;
  align-items: center;
}

.icon-wrapper {
  position: relative;
  cursor: pointer;
}

.icon-wrapper i {
  font-size: 22px;
  color: var(--dark-gray);
  transition: var(--transition);
}

.icon-wrapper:hover i {
  color: var(--primary-red);
  transform: translateY(-3px);
}

.badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: var(--primary-red);
  color: white;
  font-size: 11px;
  padding: 2px 5px;
  border-radius: 50%;
  min-width: 18px;
  text-align: center;
  font-weight: 600;
}

.user-menu {
  position: relative;
}

.user-dropdown {
  position: absolute;
  top: 40px;
  right: 0;
  background: white;
  border-radius: 10px;
  box-shadow: 0 5px 20px rgba(0,0,0,0.2);
  width: 200px;
  z-index: 1001;
  overflow: hidden;
}

.user-dropdown a {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  color: var(--dark-gray);
  text-decoration: none;
  transition: var(--transition);
}

.user-dropdown a:hover {
  background: var(--light-gray);
  color: var(--primary-red);
}

.user-dropdown a i {
  width: 20px;
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

.nav-menu {
  background: white;
  padding: 10px 0;
  position: relative;
}

.nav-menu ul {
  display: flex;
  list-style: none;
  gap: 15px;
  justify-content: center;
  flex-wrap: wrap;
}

.nav-menu ul li {
  position: relative;
}

.nav-menu ul li a {
  text-decoration: none;
  color: var(--medium-gray);
  font-weight: 600;
  transition: var(--transition);
  padding: 10px 15px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
}

.nav-menu ul li a i {
  font-size: 14px;
}

.nav-menu ul li a:hover,
.nav-menu ul li a.active {
  color: var(--primary-red);
  background: rgba(230, 57, 70, 0.1);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  background: white;
  border-radius: 10px;
  box-shadow: 0 5px 20px rgba(0,0,0,0.15);
  width: 220px;
  z-index: 1000;
  overflow: hidden;
}

.dropdown-menu a {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  border-radius: 0 !important;
}

.dropdown-menu a:first-child {
  border-radius: 10px 10px 0 0 !important;
}

.dropdown-menu a:last-child {
  border-radius: 0 0 10px 10px !important;
}

@media (max-width: 768px) {
  .header-container {
    flex-direction: column;
  }
  
  .logo {
    order: 1;
  }
  
  .nav-icons {
    order: 2;
  }
  
  .search-box {
    width: 100%;
    order: 3;
  }
  
  .nav-menu ul {
    flex-direction: column;
    align-items: center;
    gap: 5px;
  }
  
  .nav-menu ul li {
    width: 100%;
    text-align: center;
  }
  
  .dropdown-menu {
    position: static;
    width: 100%;
    box-shadow: none;
    border: 1px solid var(--border-color);
    margin-top: 5px;
  }
}
</style>