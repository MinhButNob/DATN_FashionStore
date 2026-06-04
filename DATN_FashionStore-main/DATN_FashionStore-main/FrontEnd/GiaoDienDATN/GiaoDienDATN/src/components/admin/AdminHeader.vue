<template>
  <header class="admin-header">
    <div class="header-left">
      <button class="menu-toggle" @click="$emit('toggle-menu')">
        <i class="fas fa-bars"></i>
      </button>
      <h1 class="page-title">{{ pageTitle }}</h1>
    </div>

    <div class="header-right">
      <!-- Notifications -->
      <div class="notification-dropdown" ref="notificationRef">
        <button class="notification-btn" @click="toggleNotifications">
          <i class="far fa-bell"></i>
          <span v-if="unreadCount > 0" class="notification-badge">
            {{ unreadCount }}
          </span>
        </button>

        <Transition name="dropdown">
          <div
            v-if="showNotifications"
            class="dropdown-menu notifications-menu"
          >
            <div class="dropdown-header">
              <h3>Thông báo</h3>
              <button v-if="unreadCount > 0" @click="markAllAsRead">
                Đánh dấu đã đọc
              </button>
            </div>

            <div class="notifications-list">
              <div
                v-for="notif in notifications"
                :key="notif.id"
                class="notification-item"
                :class="{ unread: !notif.read }"
                @click="markAsRead(notif.id)"
              >
                <div
                  class="notification-icon"
                  :style="{ background: notif.color + '20' }"
                >
                  <i :class="notif.icon" :style="{ color: notif.color }"></i>
                </div>
                <div class="notification-content">
                  <p>{{ notif.message }}</p>
                  <span class="notification-time">{{ notif.time }}</span>
                </div>
              </div>

              <div v-if="notifications.length === 0" class="no-notifications">
                <i class="far fa-bell-slash"></i>
                <p>Không có thông báo</p>
              </div>
            </div>

            <div class="dropdown-footer">
              <button @click="viewAllNotifications">Xem tất cả</button>
            </div>
          </div>
        </Transition>
      </div>

      <!-- Admin Profile -->
      <div class="profile-dropdown" ref="profileRef">
        <button class="profile-btn" @click="toggleProfile">
          <div class="profile-avatar">
            {{ userInitials }}
          </div>
          <span class="profile-name">{{
            authStore.user?.name || "Admin"
          }}</span>
          <i class="fas fa-chevron-down"></i>
        </button>

        <Transition name="dropdown">
          <div v-if="showProfile" class="dropdown-menu profile-menu">
            <div class="profile-header">
              <div class="profile-avatar-large">
                {{ userInitials }}
              </div>
              <div class="profile-info">
                <h4>{{ authStore.user?.name || "Admin" }}</h4>
                <p>{{ authStore.user?.email || "admin@fashionstore.vn" }}</p>
              </div>
            </div>

            <div class="profile-links">
              <router-link to="/admin/profile" @click="closeDropdowns">
                <i class="fas fa-user-circle"></i>
                Thông tin cá nhân
              </router-link>
              <router-link to="/admin/settings" @click="closeDropdowns">
                <i class="fas fa-cog"></i>
                Cài đặt
              </router-link>
              <button @click="handleLogout">
                <i class="fas fa-sign-out-alt"></i>
                Đăng xuất
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRoute } from "vue-router";
import { useAuthStore } from "../../stores/authStore";
import { useNotification } from "../../composables/useNotification";

const route = useRoute();
const authStore = useAuthStore();
const { showNotification } = useNotification();

// State
const showNotifications = ref(false);
const showProfile = ref(false);
const notificationRef = ref(null);
const profileRef = ref(null);

// Mock notifications
const notifications = ref([
  {
    id: 1,
    message: "Đơn hàng mới #DH001 cần xác nhận",
    time: "5 phút trước",
    icon: "fas fa-shopping-cart",
    color: "#e63946",
    read: false,
  },
  {
    id: 2,
    message: 'Sản phẩm "Áo thun nam" sắp hết hàng',
    time: "1 giờ trước",
    icon: "fas fa-exclamation-triangle",
    color: "#ffc107",
    read: false,
  },
  {
    id: 3,
    message: "Có đánh giá mới từ khách hàng",
    time: "2 giờ trước",
    icon: "fas fa-star",
    color: "#17a2b8",
    read: true,
  },
]);

const unreadCount = computed(() => {
  return notifications.value.filter((n) => !n.read).length;
});

const pageTitle = computed(() => {
  const path = route.path;
  if (path.includes("products")) return "Quản lý sản phẩm";
  if (path.includes("orders")) return "Quản lý đơn hàng";
  if (path.includes("customers")) return "Quản lý khách hàng";
  if (path.includes("categories")) return "Quản lý danh mục";
  if (path.includes("settings")) return "Cài đặt hệ thống";
  return "Dashboard";
});

const userInitials = computed(() => {
  const name = authStore.user?.name || "Admin";
  return name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
});

// Methods
const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value;
  if (showNotifications.value) showProfile.value = false;
};

const toggleProfile = () => {
  showProfile.value = !showProfile.value;
  if (showProfile.value) showNotifications.value = false;
};

const closeDropdowns = () => {
  showNotifications.value = false;
  showProfile.value = false;
};

const markAsRead = (id) => {
  const notif = notifications.value.find((n) => n.id === id);
  if (notif) notif.read = true;
};

const markAllAsRead = () => {
  notifications.value.forEach((n) => (n.read = true));
  showNotification("Đã đánh dấu tất cả là đã đọc", "success");
};

const viewAllNotifications = () => {
  closeDropdowns();
  // Navigate to notifications page
};

const handleLogout = () => {
  if (confirm("Bạn có chắc muốn đăng xuất?")) {
    authStore.logout();
    showNotification("Đã đăng xuất thành công!", "success");
    window.location.href = "/admin/login";
  }
};

// Handle click outside
const handleClickOutside = (event) => {
  if (notificationRef.value && !notificationRef.value.contains(event.target)) {
    showNotifications.value = false;
  }
  if (profileRef.value && !profileRef.value.contains(event.target)) {
    showProfile.value = false;
  }
};

onMounted(() => {
  document.addEventListener("click", handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener("click", handleClickOutside);
});
</script>

<style scoped>
.admin-header {
  background: white;
  padding: 15px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow);
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
  color: var(--dark-gray);
  cursor: pointer;
}

.page-title {
  font-size: 24px;
  color: var(--dark-gray);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* Notifications */
.notification-dropdown,
.profile-dropdown {
  position: relative;
}

.notification-btn {
  background: none;
  border: none;
  font-size: 20px;
  color: var(--medium-gray);
  cursor: pointer;
  position: relative;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.3s;
}

.notification-btn:hover {
  background: var(--light-gray);
  color: var(--primary-red);
}

.notification-badge {
  position: absolute;
  top: 0;
  right: 0;
  background: var(--primary-red);
  color: white;
  font-size: 10px;
  padding: 2px 5px;
  border-radius: 50%;
  min-width: 16px;
  text-align: center;
}

/* Dropdown Menu */
.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 10px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
  min-width: 300px;
  z-index: 1000;
  overflow: hidden;
}

.notifications-menu {
  width: 350px;
}

.dropdown-header {
  padding: 15px 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dropdown-header h3 {
  font-size: 16px;
  color: var(--dark-gray);
}

.dropdown-header button {
  background: none;
  border: none;
  color: var(--primary-red);
  font-size: 13px;
  cursor: pointer;
}

.notifications-list {
  max-height: 300px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 20px;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 1px solid var(--border-color);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: var(--light-gray);
}

.notification-item.unread {
  background: rgba(230, 57, 70, 0.05);
}

.notification-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
}

.notification-content p {
  color: var(--dark-gray);
  font-size: 14px;
  margin-bottom: 3px;
}

.notification-time {
  color: var(--medium-gray);
  font-size: 11px;
}

.no-notifications {
  text-align: center;
  padding: 40px 20px;
  color: var(--medium-gray);
}

.no-notifications i {
  font-size: 40px;
  margin-bottom: 10px;
}

.dropdown-footer {
  padding: 12px 20px;
  border-top: 1px solid var(--border-color);
  text-align: center;
}

.dropdown-footer button {
  background: none;
  border: none;
  color: var(--primary-red);
  font-size: 14px;
  cursor: pointer;
}

/* Profile Menu */
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
  background: var(--light-gray);
}

.profile-avatar {
  width: 35px;
  height: 35px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.profile-name {
  color: var(--dark-gray);
  font-weight: 500;
}

.profile-btn i {
  color: var(--medium-gray);
  font-size: 12px;
}

.profile-menu {
  width: 250px;
}

.profile-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  border-bottom: 1px solid var(--border-color);
}

.profile-avatar-large {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 18px;
}

.profile-info h4 {
  color: var(--dark-gray);
  font-size: 16px;
  margin-bottom: 3px;
}

.profile-info p {
  color: var(--medium-gray);
  font-size: 12px;
}

.profile-links {
  padding: 10px 0;
}

.profile-links a,
.profile-links button {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: var(--dark-gray);
  text-decoration: none;
  transition: all 0.3s;
  width: 100%;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.profile-links a:hover,
.profile-links button:hover {
  background: var(--light-gray);
  color: var(--primary-red);
}

.profile-links i {
  width: 20px;
  color: var(--primary-red);
}

/* Animations */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.3s;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Responsive */
@media (max-width: 768px) {
  .admin-header {
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

  .notifications-menu {
    width: 300px;
    right: -50px;
  }

  .profile-menu {
    width: 220px;
    right: -20px;
  }
}

@media (max-width: 480px) {
  .notifications-menu {
    width: 280px;
    right: -70px;
  }

  .profile-menu {
    width: 200px;
    right: -30px;
  }

  .notification-item {
    padding: 12px 15px;
  }
}
</style>
