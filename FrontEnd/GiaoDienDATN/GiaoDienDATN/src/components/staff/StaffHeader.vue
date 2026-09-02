<template>
  <header class="staff-header">
    <div class="header-left">
      <button class="menu-toggle" @click="$emit('toggle-menu')">
        <i class="fas fa-bars"></i>
      </button>
      <h1 class="page-title">{{ pageTitle }}</h1>
    </div>

    <div class="header-right">
      <button class="quick-btn" title="Tạo đơn tại quầy" @click="router.push('/staff/pos')">
        <i class="fas fa-plus"></i>
      </button>

      <div class="profile-dropdown" ref="profileRef">
        <button class="profile-btn" @click="showProfile = !showProfile">
          <div class="profile-avatar">{{ userInitials }}</div>
          <span class="profile-name">{{ authStore.user?.name || "Staff" }}</span>
          <i class="fas fa-chevron-down"></i>
        </button>

        <Transition name="dropdown">
          <div v-if="showProfile" class="dropdown-menu">
            <div class="profile-header">
              <div class="profile-avatar large">{{ userInitials }}</div>
              <div>
                <h4>{{ authStore.user?.name || "Staff" }}</h4>
                <p>{{ authStore.user?.email || "staff@fashionstore.vn" }}</p>
              </div>
            </div>
            <button @click="handleLogout">
              <i class="fas fa-sign-out-alt"></i>
              Đăng xuất
            </button>
          </div>
        </Transition>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../../stores/authStore";
import { useNotification } from "../../composables/useNotification";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { showNotification } = useNotification();
const showProfile = ref(false);
const profileRef = ref(null);

const pageTitle = computed(() => {
  if (route.path.includes("/pos")) return "Bán hàng tại quầy";
  if (route.path.includes("/orders")) return "Quản lý đơn hàng";
  if (route.path.includes("/returns")) return "Quản lý hoàn hàng";
  if (route.path.includes("/chat")) return "Chat khách hàng";
  return "Thống kê hôm nay";
});

const userInitials = computed(() => {
  const name = authStore.user?.name || "Staff";
  return name
    .split(" ")
    .map((part) => part[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
});

const handleLogout = () => {
  authStore.logout();
  showNotification("Đã đăng xuất thành công!", "success");
  router.push("/login");
};

const handleClickOutside = (event) => {
  if (profileRef.value && !profileRef.value.contains(event.target)) {
    showProfile.value = false;
  }
};

onMounted(() => document.addEventListener("click", handleClickOutside));
onUnmounted(() => document.removeEventListener("click", handleClickOutside));
</script>

<style scoped>
.staff-header {
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

.header-left,
.header-right,
.profile-btn {
  display: flex;
  align-items: center;
}

.header-left {
  gap: 20px;
}

.header-right {
  gap: 14px;
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

.quick-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 8px;
  color: white;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  cursor: pointer;
}

.profile-dropdown {
  position: relative;
}

.profile-btn {
  gap: 10px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 8px;
  transition: var(--transition);
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

.profile-avatar.large {
  width: 50px;
  height: 50px;
  font-size: 18px;
  flex-shrink: 0;
}

.profile-name {
  color: var(--dark-gray);
  font-weight: 500;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 10px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
  width: 260px;
  z-index: 1000;
  overflow: hidden;
}

.profile-header {
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  border-bottom: 1px solid var(--border-color);
}

.profile-header h4 {
  color: var(--dark-gray);
  font-size: 16px;
  margin-bottom: 3px;
}

.profile-header p {
  color: var(--medium-gray);
  font-size: 12px;
}

.dropdown-menu button {
  width: 100%;
  border: none;
  background: white;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  cursor: pointer;
  color: var(--dark-gray);
}

.dropdown-menu button:hover {
  background: var(--light-gray);
  color: var(--primary-red);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 768px) {
  .staff-header {
    padding: 15px 20px;
  }

  .menu-toggle {
    display: block;
  }

  .page-title {
    font-size: 18px;
  }

  .profile-name,
  .profile-btn .fa-chevron-down {
    display: none;
  }
}
</style>
