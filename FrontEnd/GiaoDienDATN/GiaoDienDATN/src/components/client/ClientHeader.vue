<template>
  <header :class="{ 'header-hidden': isHeaderHidden }">
    <div class="header-top">
      <div class="container header-container">
        <div class="logo" @click="goToHome">FASHION STORE</div>

        <div class="search-box">
          <input
            type="text"
            v-model="searchQuery"
            @keyup.enter="handleSearch"
            placeholder="Tìm kiếm sản phẩm..."
          />
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

          <!-- ✅ Click vào cart icon để chuyển đến trang cart -->
          <div class="icon-wrapper" @click="goToCartPage">
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
        <li v-for="item in menuItemsBefore" :key="item.path">
          <router-link :to="item.path" :class="{ active: isActive(item.path) }" @click="closeDropdowns">
            <i :class="item.icon"></i>
            {{ item.name }}
          </router-link>
        </li>
        <CategoryMegaMenu />
        <li v-for="item in menuItemsAfter" :key="item.path">
          <router-link :to="item.path" :class="{ active: isActive(item.path) }" @click="closeDropdowns">
            <i :class="item.icon"></i>
            {{ item.name }}
          </router-link>
        </li>
      </ul>
      </div>
    </nav>
  </header>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useProductStore } from "../../stores/productStore";
import { useCartStore } from "../../stores/cartStore";
import { useAuthStore } from "../../stores/authStore";
import CategoryMegaMenu from "../../components/client/Categorymegamenu.vue";

const router = useRouter();
const route = useRoute();
const productStore = useProductStore();
const cartStore = useCartStore();
const authStore = useAuthStore();

const searchQuery = ref("");
const showUserMenu = ref(false);
const userMenu = ref(null);
const wishlistCount = ref(0);
const isHeaderHidden = ref(false);
let lastScrollY = 0;
const SCROLL_HIDE_THRESHOLD = 80; // px cuộn xuống từ đỉnh trang mới bắt đầu ẩn
const SCROLL_DELTA = 6; // ngưỡng chống rung khi cuộn nhẹ

// Thứ tự menu mong muốn: Trang chủ, Sản phẩm, Khuyến mãi, [3 danh mục mặc định: Áo/Quần/Phụ kiện], Về chúng tôi
const menuItemsBefore = [
  { path: "/home", name: "Trang chủ", icon: "fas fa-home" },
  { path: "/shop", name: "Sản phẩm", icon: "fas fa-store" },
  { path: "/shop?categoryId=sale", name: "Khuyến mãi", icon: "fas fa-tags" },
];
const menuItemsAfter = [
  { path: "/about", name: "Về chúng tôi", icon: "fas fa-info-circle" },
];

const isActive = (path) => {
  if (path.includes("?")) {
    const [base, query] = path.split("?");
    const params = new URLSearchParams(query);

    // Link "Khuyến mãi": active khi categoryId=sale hoặc categoryName=Khuyến Mãi
    const categoryId = params.get("categoryId");
    if (categoryId === "sale") {
      const currentCatId = (route.query.categoryId || "").toString().trim().toLowerCase();
      const currentCatName = (route.query.categoryName || "").toString().trim().toLowerCase();
      return route.path === base && (currentCatId === "sale" || currentCatName === "khuyến mãi");
    }

    const categoryName = params.get("categoryName");
    if (categoryName) {
      const current = (route.query.categoryName || "").toString().trim().toLowerCase();
      return route.path === base && current === categoryName.trim().toLowerCase();
    }

    const tag = params.get("tag");
    if (tag === "sale") {
      return route.path === "/shop" && route.query.tag === "sale";
    }
    return route.path === base;
  }

  // Link "Sản phẩm" (/shop không kèm query): chỉ active khi KHÔNG đang lọc
  // theo "Khuyến mãi" (categoryId=sale) - để tránh 2 mục cùng sáng 1 lúc.
  if (path === "/shop") {
    const currentCatId = (route.query.categoryId || "").toString().trim().toLowerCase();
    const currentCatName = (route.query.categoryName || "").toString().trim().toLowerCase();
    return route.path === "/shop" && currentCatId !== "sale" && currentCatName !== "khuyến mãi";
  }

  return route.path === path;
};

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value;
};

const closeUserMenu = () => {
  showUserMenu.value = false;
};

const closeDropdowns = () => {
  closeUserMenu();
};

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    productStore.setFilter("search", searchQuery.value);
    router.push(`/shop?search=${searchQuery.value}`);
  }
};

const goToHome = () => {
  router.push("/home");
  closeDropdowns();
};

const goToWishlist = () => {
  if (authStore.isAuthenticated) {
    router.push("/wishlist");
  } else {
    showLoginModal();
  }
  closeDropdowns();
};

// ✅ Chuyển đến trang cart
const goToCartPage = () => {
  closeDropdowns();
  router.push("/cart");
};

const showLoginModal = () => {
  window.dispatchEvent(new CustomEvent("open-auth-modal", { detail: "login" }));
  closeUserMenu();
};

const showRegisterModal = () => {
  window.dispatchEvent(
    new CustomEvent("open-auth-modal", { detail: "register" })
  );
  closeUserMenu();
};

const logout = () => {
  authStore.logout();
  closeUserMenu();
  router.push("/home");
};

// Handle click outside
const handleClickOutside = (event) => {
  if (userMenu.value && !userMenu.value.contains(event.target)) {
    showUserMenu.value = false;
  }
};

// Ẩn header khi cuộn xuống, hiện lại ngay khi cuộn lên (giữ thao tác vuốt lên là "muốn xem lại menu")
const handleHeaderScroll = () => {
  const currentY = window.scrollY;

  if (currentY < SCROLL_HIDE_THRESHOLD) {
    isHeaderHidden.value = false;
  } else if (currentY - lastScrollY > SCROLL_DELTA) {
    isHeaderHidden.value = true;
    closeDropdowns();
  } else if (lastScrollY - currentY > SCROLL_DELTA) {
    isHeaderHidden.value = false;
  }

  lastScrollY = currentY;
};

// Đồng bộ khoảng trống dành cho header (margin-top của <main>, top của sidebar sticky...)
// với trạng thái ẩn/hiện thực tế của header, để khi header trượt lên ẩn đi thì phần
// nội dung phía dưới cũng "kéo lên" theo, không để lại khoảng trắng/xám cố định.
watch(isHeaderHidden, (hidden) => {
  document.documentElement.style.setProperty(
    "--header-offset",
    hidden ? "20px" : "140px"
  );
}, { immediate: true });

onMounted(() => {
  document.addEventListener("click", handleClickOutside);
  window.addEventListener("scroll", handleHeaderScroll, { passive: true });
});

onUnmounted(() => {
  document.removeEventListener("click", handleClickOutside);
  window.removeEventListener("scroll", handleHeaderScroll);
});
</script>

<style scoped>
header {
  position: fixed;
  top: 0;
  width: 100%;
  z-index: 1000;
  background: white;
  box-shadow: 0 2px 15px rgba(0, 0, 0, 0.08);
  transition: transform 0.32s cubic-bezier(0.4, 0, 0.2, 1);
}

header.header-hidden {
  transform: translateY(-100%);
}

.header-top {
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
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
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  cursor: pointer;
  letter-spacing: 1px;
}

.search-box {
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 30px;
  padding: 5px 5px 5px 20px;
  width: 400px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.search-box:focus-within {
  box-shadow: 0 2px 15px rgba(230, 57, 70, 0.15);
  background: white;
}

.search-box input {
  border: none;
  background: none;
  padding: 12px 0;
  width: 100%;
  outline: none;
  font-size: 14px;
  color: #333;
}

.search-box input::placeholder {
  color: #999;
}

.search-box button {
  background: #e63946;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 16px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-box button:hover {
  background: #c62828;
  transform: scale(1.05);
}

.nav-icons {
  display: flex;
  gap: 25px;
  align-items: center;
}

.icon-wrapper {
  position: relative;
  cursor: pointer;
  padding: 5px;
}

.icon-wrapper i {
  font-size: 22px;
  color: #333;
  transition: all 0.3s ease;
}

.icon-wrapper:hover i {
  color: #e63946;
  transform: translateY(-3px);
}

.badge {
  position: absolute;
  top: -5px;
  right: -8px;
  background: #e63946;
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 50%;
  min-width: 18px;
  text-align: center;
  font-weight: 600;
  border: 2px solid white;
}

.user-menu {
  position: relative;
}

.user-menu i {
  font-size: 22px;
  color: #333;
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-menu i:hover {
  color: #e63946;
}

.user-dropdown {
  position: absolute;
  top: 45px;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  width: 200px;
  z-index: 1001;
  overflow: hidden;
  animation: slideDown 0.3s ease;
}

.user-dropdown a {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: #333;
  text-decoration: none;
  transition: all 0.2s ease;
  font-size: 14px;
}

.user-dropdown a:hover {
  background: #f8f9fa;
  color: #e63946;
}

.user-dropdown a i {
  width: 20px;
  font-size: 16px;
  color: #666;
}

.user-dropdown a:hover i {
  color: #e63946;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.3s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.nav-menu {
  background: white;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.nav-menu ul {
  display: flex;
  list-style: none;
  gap: 5px;
  justify-content: center;
  flex-wrap: wrap;
  margin: 0;
  padding: 0;
}

.nav-menu ul li a {
  text-decoration: none;
  color: #555;
  font-weight: 500;
  transition: all 0.3s ease;
  padding: 10px 20px;
  border-radius: 25px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.nav-menu ul li a i {
  font-size: 15px;
}

.nav-menu ul li a:hover {
  color: #e63946;
  background: rgba(230, 57, 70, 0.08);
}

.nav-menu ul li a.active {
  color: #e63946;
  background: rgba(230, 57, 70, 0.12);
  font-weight: 600;
}

@media (max-width: 1024px) {
  .search-box {
    width: 300px;
  }
}

@media (max-width: 768px) {
  .header-container {
    flex-direction: column;
    gap: 10px;
  }

  .logo {
    font-size: 24px;
    order: 1;
  }

  .nav-icons {
    order: 2;
    gap: 15px;
  }

  .search-box {
    width: 100%;
    order: 3;
  }

  .nav-menu ul {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 5px;
  }

  .nav-menu ul li a {
    padding: 8px 15px;
    font-size: 13px;
  }

  .nav-menu ul li a i {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .nav-menu ul li a {
    padding: 6px 12px;
    font-size: 12px;
  }

  .nav-menu ul li a i {
    font-size: 12px;
  }

  .user-dropdown {
    right: -20px;
    width: 180px;
  }
}
</style>