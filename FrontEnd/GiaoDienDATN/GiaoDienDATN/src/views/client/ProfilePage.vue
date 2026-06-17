<template>
  <div class="profile-page">
    <ClientHeader />

    <main>
      <div class="container">
        <div class="profile-container">
          <!-- Sidebar -->
          <aside class="profile-sidebar">
            <div class="profile-avatar">
              <div class="avatar-image" @click="changeAvatar">
                <i class="fas fa-user-circle"></i>
              </div>
              <h3>{{ displayName }}</h3>
              <p>{{ displayEmail }}</p>
            </div>

            <ul class="profile-menu">
              <li>
                <a
                  href="#"
                  :class="{ active: activeTab === 'info' }"
                  @click.prevent="activeTab = 'info'"
                >
                  <i class="fas fa-user"></i>
                  Thông tin tài khoản
                </a>
              </li>
              <li>
                <a
                  href="#"
                  :class="{ active: activeTab === 'orders' }"
                  @click.prevent="activeTab = 'orders'"
                >
                  <i class="fas fa-shopping-bag"></i>
                  Đơn hàng của tôi
                </a>
              </li>
              <li>
                <a
                  href="#"
                  :class="{ active: activeTab === 'wishlist' }"
                  @click.prevent="activeTab = 'wishlist'"
                >
                  <i class="fas fa-heart"></i>
                  Sản phẩm yêu thích
                </a>
              </li>
              <li>
                <a
                  href="#"
                  :class="{ active: activeTab === 'password' }"
                  @click.prevent="activeTab = 'password'"
                >
                  <i class="fas fa-lock"></i>
                  Đổi mật khẩu
                </a>
              </li>
            </ul>

            <button class="logout-btn" @click="logout">
              <i class="fas fa-sign-out-alt"></i>
              Đăng xuất
            </button>
          </aside>

          <!-- Main Content -->
          <div class="profile-content">
            <!-- Loading State -->
            <div v-if="pageLoading" class="loading-state">
              <i class="fas fa-spinner fa-spin"></i>
              <p>Đang tải thông tin...</p>
            </div>

            <!-- Content -->
            <template v-else>
              <!-- ⚠️ Thông báo chưa đăng nhập -->
              <div v-if="!authStore.isAuthenticated" class="auth-required">
                <i class="fas fa-lock"></i>
                <h2>Vui lòng đăng nhập</h2>
                <p>Bạn cần đăng nhập để xem thông tin tài khoản</p>
                <router-link to="/login" class="btn btn-primary">
                  <i class="fas fa-sign-in-alt"></i>
                  Đăng nhập ngay
                </router-link>
              </div>

              <!-- Content khi đã đăng nhập -->
              <template v-else>
                <!-- Banner thông báo sau đăng ký -->
                <div v-if="showWelcomeBanner" class="welcome-banner">
                  <div class="banner-content">
                    <i class="fas fa-hand-wave"></i>
                    <div>
                      <h3>Chào mừng bạn đến với Fashion Store!</h3>
                      <p>Vui lòng cập nhật thông tin cá nhân để hoàn tất đăng ký.</p>
                    </div>
                    <button class="btn btn-primary btn-sm" @click="startEditing">
                      <i class="fas fa-edit"></i>
                      Cập nhật ngay
                    </button>
                  </div>
                  <button class="banner-close" @click="showWelcomeBanner = false">
                    <i class="fas fa-times"></i>
                  </button>
                </div>

                <!-- Thông tin tài khoản -->
                <div v-show="activeTab === 'info'" class="profile-tab">
                  <h2>Thông tin tài khoản</h2>

                  <div v-if="!editing" class="info-display">
                    <div class="info-grid">
                      <div class="info-item">
                        <label>Họ và tên</label>
                        <p>{{ displayName }}</p>
                      </div>
                      <div class="info-item">
                        <label>Email</label>
                        <p>{{ displayEmail }}</p>
                      </div>
                      <div class="info-item">
                        <label>Số điện thoại</label>
                        <p>{{ profile.phone || "Chưa cập nhật" }}</p>
                      </div>
                      <div class="info-item">
                        <label>Địa chỉ</label>
                        <p>{{ profile.address || "Chưa cập nhật" }}</p>
                      </div>
                    </div>
                    <button class="btn btn-primary" @click="startEditing">
                      <i class="fas fa-edit"></i>
                      Chỉnh sửa thông tin
                    </button>
                  </div>

                  <form v-else @submit.prevent="saveProfile" class="info-edit">
                    <div class="form-group">
                      <label>Họ và tên <span class="required">*</span></label>
                      <input 
                        type="text" 
                        v-model="editForm.fullName" 
                        required 
                        placeholder="Nhập họ và tên của bạn"
                      />
                    </div>
                    <div class="form-group">
                      <label>Email <span class="required">*</span></label>
                      <input 
                        type="email" 
                        v-model="editForm.email" 
                        required 
                        disabled 
                        class="disabled-input"
                      />
                      <small class="form-hint">Email không thể thay đổi</small>
                    </div>
                    <div class="form-group">
                      <label>Số điện thoại</label>
                      <input 
                        type="tel" 
                        v-model="editForm.phone" 
                        placeholder="Ví dụ: 0912345678"
                      />
                    </div>
                    <div class="form-group">
                      <label>Địa chỉ</label>
                      <input 
                        type="text" 
                        v-model="editForm.address" 
                        placeholder="Ví dụ: 123 Nguyễn Huệ, Quận 1, TP.HCM"
                      />
                    </div>
                    <div class="form-actions">
                      <button type="submit" class="btn btn-primary" :disabled="loading">
                        <i v-if="loading" class="fas fa-spinner fa-spin"></i>
                        {{ loading ? "Đang lưu..." : "Lưu thay đổi" }}
                      </button>
                      <button
                        type="button"
                        class="btn btn-outline"
                        @click="cancelEditing"
                      >
                        Hủy
                      </button>
                    </div>
                  </form>
                </div>

                <!-- Đơn hàng -->
                <div v-show="activeTab === 'orders'" class="profile-tab">
                  <h2>Đơn hàng của tôi</h2>

                  <div v-if="ordersLoading" class="loading-state">
                    <i class="fas fa-spinner fa-spin"></i>
                    <p>Đang tải đơn hàng...</p>
                  </div>

                  <div v-else-if="orders.length === 0" class="empty-state">
                    <i class="fas fa-box-open"></i>
                    <p>Bạn chưa có đơn hàng nào</p>
                    <router-link to="/shop" class="btn btn-primary">
                      Mua sắm ngay
                    </router-link>
                  </div>

                  <div v-else class="orders-list">
                    <div v-for="order in orders" :key="order.id" class="order-card">
                      <div class="order-header">
                        <div>
                          <span class="order-id">Mã đơn: {{ order.id }}</span>
                          <span class="order-date">{{ order.date }}</span>
                        </div>
                        <span :class="['order-status', `status-${order.status}`]">
                          {{ getOrderStatus(order.status) }}
                        </span>
                      </div>

                      <div class="order-items">
                        <div
                          v-for="item in order.items"
                          :key="item.id"
                          class="order-item"
                        >
                          <div class="item-image">
                            <i :class="['fas', `fa-${item.image || 'tshirt'}`]"></i>
                          </div>
                          <div class="item-details">
                            <h4>{{ item.name }}</h4>
                            <p class="item-variant">
                              {{ item.size }} {{ item.color }}
                            </p>
                            <p class="item-quantity">x{{ item.quantity }}</p>
                          </div>
                          <div class="item-price">
                            {{ formatPrice(item.price * item.quantity) }}
                          </div>
                        </div>
                      </div>

                      <div class="order-footer">
                        <div class="order-total">
                          <span>Tổng cộng:</span>
                          <strong>{{ formatPrice(order.total) }}</strong>
                        </div>
                        <div class="order-actions">
                          <button
                            class="btn btn-outline"
                            @click="viewOrderDetail(order.id)"
                          >
                            Xem chi tiết
                          </button>
                          <button
                            v-if="order.status === 'delivered'"
                            class="btn btn-outline"
                            @click="reorder(order.id)"
                          >
                            Mua lại
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Sản phẩm yêu thích -->
                <div v-show="activeTab === 'wishlist'" class="profile-tab">
                  <h2>Sản phẩm yêu thích</h2>

                  <div v-if="wishlistLoading" class="loading-state">
                    <i class="fas fa-spinner fa-spin"></i>
                    <p>Đang tải sản phẩm yêu thích...</p>
                  </div>

                  <div v-else-if="wishlist.length === 0" class="empty-state">
                    <i class="fas fa-heart"></i>
                    <p>Bạn chưa có sản phẩm yêu thích nào</p>
                    <router-link to="/shop" class="btn btn-primary">
                      Khám phá sản phẩm
                    </router-link>
                  </div>

                  <div v-else class="wishlist-grid">
                    <div
                      v-for="product in wishlist"
                      :key="product.id"
                      class="wishlist-card"
                    >
                      <div class="product-image" @click="goToProduct(product.id)">
                        <i :class="['fas', `fa-${product.image || 'tshirt'}`]"></i>
                        <button
                          class="remove-btn"
                          @click.stop="removeFromWishlist(product.id)"
                        >
                          <i class="fas fa-times"></i>
                        </button>
                      </div>
                      <div class="product-info">
                        <h3 @click="goToProduct(product.id)">{{ product.name }}</h3>
                        <p class="product-price">
                          {{ formatPrice(product.price) }}
                        </p>
                        <button class="add-to-cart" @click="addToCart(product)">
                          <i class="fas fa-shopping-cart"></i>
                          Thêm vào giỏ
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Đổi mật khẩu -->
                <div v-show="activeTab === 'password'" class="profile-tab">
                  <h2>Đổi mật khẩu</h2>

                  <form @submit.prevent="changePassword" class="password-form">
                    <div class="form-group">
                      <label>Mật khẩu hiện tại <span class="required">*</span></label>
                      <div class="password-input">
                        <input
                          :type="showCurrentPassword ? 'text' : 'password'"
                          v-model="passwordForm.currentPassword"
                          required
                          placeholder="Nhập mật khẩu hiện tại"
                        />
                        <i
                          :class="
                            showCurrentPassword ? 'fas fa-eye-slash' : 'fas fa-eye'
                          "
                          @click="showCurrentPassword = !showCurrentPassword"
                        ></i>
                      </div>
                    </div>

                    <div class="form-group">
                      <label>Mật khẩu mới <span class="required">*</span></label>
                      <div class="password-input">
                        <input
                          :type="showNewPassword ? 'text' : 'password'"
                          v-model="passwordForm.newPassword"
                          required
                          minlength="6"
                          placeholder="Nhập mật khẩu mới"
                        />
                        <i
                          :class="
                            showNewPassword ? 'fas fa-eye-slash' : 'fas fa-eye'
                          "
                          @click="showNewPassword = !showNewPassword"
                        ></i>
                      </div>
                    </div>

                    <div class="form-group">
                      <label>Xác nhận mật khẩu mới <span class="required">*</span></label>
                      <div class="password-input">
                        <input
                          :type="showConfirmPassword ? 'text' : 'password'"
                          v-model="passwordForm.confirmPassword"
                          required
                          placeholder="Xác nhận mật khẩu mới"
                        />
                        <i
                          :class="
                            showConfirmPassword ? 'fas fa-eye-slash' : 'fas fa-eye'
                          "
                          @click="showConfirmPassword = !showConfirmPassword"
                        ></i>
                      </div>
                    </div>

                    <div class="password-requirements">
                      <p>Mật khẩu phải có:</p>
                      <ul>
                        <li
                          :class="{ valid: passwordForm.newPassword.length >= 6 }"
                        >
                          <i
                            :class="
                              passwordForm.newPassword.length >= 6
                                ? 'fas fa-check-circle'
                                : 'far fa-circle'
                            "
                          ></i>
                          Ít nhất 6 ký tự
                        </li>
                        <li
                          :class="{ valid: /[A-Z]/.test(passwordForm.newPassword) }"
                        >
                          <i
                            :class="
                              /[A-Z]/.test(passwordForm.newPassword)
                                ? 'fas fa-check-circle'
                                : 'far fa-circle'
                            "
                          ></i>
                          Ít nhất 1 chữ hoa
                        </li>
                        <li
                          :class="{ valid: /[0-9]/.test(passwordForm.newPassword) }"
                        >
                          <i
                            :class="
                              /[0-9]/.test(passwordForm.newPassword)
                                ? 'fas fa-check-circle'
                                : 'far fa-circle'
                            "
                          ></i>
                          Ít nhất 1 số
                        </li>
                      </ul>
                    </div>

                    <button
                      type="submit"
                      class="btn btn-primary"
                      :disabled="!isPasswordValid || passwordLoading"
                    >
                      <i v-if="passwordLoading" class="fas fa-spinner fa-spin"></i>
                      {{ passwordLoading ? "Đang xử lý..." : "Đổi mật khẩu" }}
                    </button>
                  </form>
                </div>
              </template>
            </template>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "../../stores/authStore";
import { useCartStore } from "../../stores/cartStore";
import { useNotification } from "../../composables/useNotification";
import ClientHeader from "../../components/client/ClientHeader.vue";
import ClientFooter from "../../components/client/ClientFooter.vue";
import { formatPrice } from "../../utils/helpers";
import axios from "axios";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const cartStore = useCartStore();
const { showNotification } = useNotification();

// State
const activeTab = ref("info");
const editing = ref(false);
const loading = ref(false);
const passwordLoading = ref(false);
const pageLoading = ref(true);
const showWelcomeBanner = ref(false);
const ordersLoading = ref(false);
const wishlistLoading = ref(false);

// Profile data
const profile = reactive({
  fullName: authStore.user?.username || "",
  email: authStore.user?.email || "",
  phone: "",
  address: ""
});

const editForm = reactive({
  fullName: profile.fullName,
  email: profile.email,
  phone: "",
  address: ""
});

// Computed để hiển thị
const displayName = computed(() => {
  return profile.fullName || "Người dùng";
});

const displayEmail = computed(() => {
  return profile.email || "email@example.com";
});

// Orders data
const orders = ref([]);

// Wishlist data
const wishlist = ref([]);

// Password change
const showCurrentPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);

const passwordForm = reactive({
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const isPasswordValid = computed(() => {
  return (
    passwordForm.newPassword.length >= 6 &&
    /[A-Z]/.test(passwordForm.newPassword) &&
    /[0-9]/.test(passwordForm.newPassword) &&
    passwordForm.newPassword === passwordForm.confirmPassword &&
    passwordForm.currentPassword.length > 0
  );
});

// ✅ Lấy profile từ API
const fetchProfile = async () => {
  try {
    const token = authStore.token || localStorage.getItem("token");
    console.log("🔍 Token:", token);
    
    // Nếu có token, gọi API lấy thông tin mới nhất
    if (token) {
      const response = await axios.get("http://localhost:8080/api/customers/profile", {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json"
        }
      });

      console.log("✅ Profile from API:", response.data);

      if (response.data) {
        const data = response.data;
        
        // ✅ Cập nhật profile
        profile.fullName = data.fullName || data.customerName || "";
        profile.email = data.email || "";
        profile.phone = data.phone || "";
        profile.address = data.address || "";

        // ✅ Cập nhật authStore
        if (authStore.user) {
          authStore.user.username = profile.fullName;
          authStore.user.email = profile.email;
          // Lưu lại localStorage
          localStorage.setItem("user", JSON.stringify(authStore.user));
        }

        console.log("📋 Profile loaded:", {
          fullName: profile.fullName,
          email: profile.email,
          phone: profile.phone,
          address: profile.address
        });
      }
    } else {
      // Không có token, lấy từ authStore
      profile.fullName = authStore.user?.username || "";
      profile.email = authStore.user?.email || "";
      console.log("📋 Profile from authStore:", {
        fullName: profile.fullName,
        email: profile.email
      });
    }

    // Kiểm tra user mới
    if (route.query.newUser === 'true') {
      showWelcomeBanner.value = true;
      if (!profile.phone && !profile.address) {
        setTimeout(() => startEditing(), 500);
      }
    }
  } catch (error) {
    console.error("❌ Error fetching profile:", error);
    // Nếu API lỗi, vẫn hiển thị thông tin từ authStore
    profile.fullName = authStore.user?.username || "";
    profile.email = authStore.user?.email || "";
  } finally {
    pageLoading.value = false;
  }
};

const startEditing = () => {
  Object.assign(editForm, {
    fullName: profile.fullName,
    email: profile.email,
    phone: profile.phone || "",
    address: profile.address || ""
  });
  editing.value = true;
  showWelcomeBanner.value = false;
};

const cancelEditing = () => {
  editing.value = false;
};

// ✅ Lưu profile và reload dữ liệu
const saveProfile = async () => {
  if (!editForm.fullName || editForm.fullName.trim().length === 0) {
    showNotification("Vui lòng nhập họ và tên", "error");
    return;
  }

  loading.value = true;
  try {
    const token = authStore.token || localStorage.getItem("token");
    
    const payload = {
      fullName: editForm.fullName.trim(),
      email: editForm.email,
      phone: editForm.phone ? editForm.phone.trim() : null,
      address: editForm.address ? editForm.address.trim() : null
    };

    console.log("📤 Sending payload:", payload);

    if (token) {
      const response = await axios.put(
        "http://localhost:8080/api/customers/profile",
        payload,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json"
          }
        }
      );
      
      console.log("✅ Update response:", response.data);

      if (response.data) {
        const data = response.data;
        
        // ✅ Cập nhật profile
        profile.fullName = data.fullName || data.customerName || editForm.fullName;
        profile.email = data.email || editForm.email;
        profile.phone = data.phone || editForm.phone || "";
        profile.address = data.address || editForm.address || "";

        // ✅ Cập nhật authStore
        if (authStore.user) {
          authStore.user.username = profile.fullName;
          authStore.user.email = profile.email;
          localStorage.setItem("user", JSON.stringify(authStore.user));
        }

        editing.value = false;
        showNotification("Cập nhật thông tin thành công!", "success");
        
        // ✅ Gọi lại API để lấy dữ liệu mới nhất (đồng bộ)
        await fetchProfile();
      }
    } else {
      // Không có token, chỉ cập nhật local
      showNotification("Vui lòng đăng nhập để lưu thông tin", "warning");
      profile.fullName = editForm.fullName;
      profile.email = editForm.email;
      profile.phone = editForm.phone || "";
      profile.address = editForm.address || "";
      editing.value = false;
    }
  } catch (error) {
    console.error("❌ Error saving profile:", error);
    const message = error.response?.data?.message || "Có lỗi xảy ra khi cập nhật";
    showNotification(message, "error");
  } finally {
    loading.value = false;
  }
};

// Watch authStore.user để cập nhật profile
watch(
  () => authStore.user,
  (newUser) => {
    if (newUser) {
      profile.fullName = newUser.username || profile.fullName;
      profile.email = newUser.email || profile.email;
    }
  },
  { deep: true }
);

const getOrderStatus = (status) => {
  const statuses = {
    pending: "Chờ xác nhận",
    processing: "Đang xử lý",
    shipped: "Đang giao",
    delivered: "Đã giao",
    cancelled: "Đã hủy",
  };
  return statuses[status] || status;
};

const viewOrderDetail = (orderId) => {
  showNotification(`Xem chi tiết đơn hàng ${orderId}`, "info");
};

const reorder = (orderId) => {
  showNotification("Đã thêm vào giỏ hàng", "success");
  router.push("/cart");
};

const goToProduct = (id) => {
  router.push(`/product/${id}`);
};

const addToCart = (product) => {
  cartStore.addToCart(product);
  showNotification("Đã thêm vào giỏ hàng", "success");
};

const removeFromWishlist = (id) => {
  wishlist.value = wishlist.value.filter((p) => p.id !== id);
  showNotification("Đã xóa khỏi danh sách yêu thích", "info");
};

const changePassword = async () => {
  if (!isPasswordValid.value) {
    showNotification("Vui lòng kiểm tra lại mật khẩu", "error");
    return;
  }

  passwordLoading.value = true;
  try {
    await new Promise(resolve => setTimeout(resolve, 1000));
    showNotification("Đổi mật khẩu thành công!", "success");
    passwordForm.currentPassword = "";
    passwordForm.newPassword = "";
    passwordForm.confirmPassword = "";
  } catch (error) {
    console.error("Error changing password:", error);
    showNotification("Có lỗi xảy ra", "error");
  } finally {
    passwordLoading.value = false;
  }
};

const changeAvatar = () => {
  showNotification("Chức năng đang phát triển", "info");
};

const logout = () => {
  if (confirm("Bạn có chắc muốn đăng xuất?")) {
    authStore.logout();
    router.push("/home");
  }
};

// Fetch orders
const fetchOrders = async () => {
  ordersLoading.value = true;
  try {
    const token = authStore.token || localStorage.getItem("token");
    
    if (!token) {
      ordersLoading.value = false;
      return;
    }

    const response = await axios.get("http://localhost:8080/api/orders", {
      headers: {
        Authorization: `Bearer ${token}`
      },
      params: {
        page: 0,
        size: 10
      }
    });

    if (response.data && response.data.content) {
      orders.value = response.data.content.map(order => ({
        id: order.orderCode || `DH${order.orderId}`,
        date: formatDate(order.orderDate),
        status: mapOrderStatus(order.orderStatus),
        total: order.totalAmount || 0,
        items: order.items ? order.items.map(item => ({
          id: item.itemId,
          name: item.productName,
          image: "tshirt",
          price: item.unitPrice,
          quantity: item.quantity,
          size: item.sizeName || "M",
          color: item.colorName || "Đen"
        })) : []
      }));
    }
  } catch (error) {
    console.error("Error fetching orders:", error);
  } finally {
    ordersLoading.value = false;
  }
};

// Fetch wishlist
const fetchWishlist = async () => {
  wishlistLoading.value = true;
  try {
    wishlist.value = [
      { id: 1, name: "Áo thun nam", price: 199000, image: "tshirt" },
      { id: 2, name: "Đầm nữ công sở", price: 399000, image: "dress" },
    ];
  } catch (error) {
    console.error("Error fetching wishlist:", error);
  } finally {
    wishlistLoading.value = false;
  }
};

// Helper functions
const formatDate = (dateString) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleDateString("vi-VN");
};

const mapOrderStatus = (status) => {
  const map = {
    "PENDING_PAYMENT": "pending",
    "PROCESSING": "processing",
    "SHIPPING": "shipped",
    "DELIVERED": "delivered",
    "CANCELLED": "cancelled"
  };
  return map[status] || "pending";
};

// Lifecycle
onMounted(() => {
  console.log("🚀 Profile page mounted");
  console.log("🔍 Token:", authStore.token || localStorage.getItem("token"));
  console.log("🔍 Is authenticated:", authStore.isAuthenticated);
  console.log("🔍 User:", authStore.user);
  
  fetchProfile();
  fetchOrders();
  fetchWishlist();
});
</script>

<style scoped>
/* ===== RESET & CONTAINER ===== */
.profile-page {
  min-height: 100vh;
}

main {
  margin-top: 140px;
  padding: 20px 0 50px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}

/* ===== PROFILE CONTAINER ===== */
.profile-container {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 30px;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 5px 30px rgba(0, 0, 0, 0.08);
}

/* ===== SIDEBAR ===== */
.profile-sidebar {
  background: linear-gradient(135deg, #e63946 0%, #c62828 100%);
  padding: 30px;
  color: white;
  display: flex;
  flex-direction: column;
}

.profile-avatar {
  text-align: center;
  margin-bottom: 30px;
}

.avatar-image {
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  margin: 0 auto 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid white;
  cursor: pointer;
  transition: all 0.3s;
}

.avatar-image:hover {
  transform: scale(1.05);
  background: rgba(255, 255, 255, 0.3);
}

.avatar-image i {
  font-size: 60px;
  color: white;
}

.profile-avatar h3 {
  font-size: 18px;
  margin-bottom: 5px;
}

.profile-avatar p {
  font-size: 14px;
  opacity: 0.9;
}

.profile-menu {
  list-style: none;
  flex: 1;
  padding: 0;
}

.profile-menu li {
  margin-bottom: 5px;
}

.profile-menu a {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 15px;
  color: white;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.3s;
}

.profile-menu a:hover,
.profile-menu a.active {
  background: rgba(255, 255, 255, 0.2);
  transform: translateX(5px);
}

.profile-menu a i {
  width: 20px;
}

.logout-btn {
  width: 100%;
  padding: 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s;
  margin-top: 20px;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* ===== MAIN CONTENT ===== */
.profile-content {
  padding: 30px;
  flex: 1;
}

.profile-tab {
  animation: fadeIn 0.3s;
}

.profile-tab h2 {
  color: #1a1a2e;
  margin-bottom: 20px;
  font-size: 24px;
}

/* ===== AUTH REQUIRED ===== */
.auth-required {
  text-align: center;
  padding: 80px 20px;
}

.auth-required i {
  font-size: 60px;
  color: #e63946;
  margin-bottom: 20px;
}

.auth-required h2 {
  color: #1a1a2e;
  margin-bottom: 10px;
  font-size: 28px;
}

.auth-required p {
  color: #6c757d;
  margin-bottom: 25px;
  font-size: 16px;
}

/* ===== LOADING STATE ===== */
.loading-state {
  text-align: center;
  padding: 80px 20px;
}

.loading-state i {
  font-size: 50px;
  color: #e63946;
  margin-bottom: 20px;
}

.loading-state p {
  color: #6c757d;
  font-size: 16px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px;
  background: white;
  border-radius: 15px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #e63946;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ===== WELCOME BANNER ===== */
.welcome-banner {
  background: linear-gradient(135deg, #e63946 0%, #ff6b6b 100%);
  color: white;
  padding: 20px 25px;
  border-radius: 12px;
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  animation: slideDown 0.5s ease;
}

.banner-content {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.banner-content i {
  font-size: 30px;
}

.banner-content h3 {
  font-size: 18px;
  margin-bottom: 3px;
}

.banner-content p {
  font-size: 14px;
  opacity: 0.9;
}

.banner-close {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s;
}

.banner-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* ===== INFO DISPLAY ===== */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.info-item {
  padding: 15px;
  background: #f8f9fa;
  border-radius: 10px;
}

.info-item label {
  display: block;
  font-size: 13px;
  color: #6c757d;
  margin-bottom: 5px;
  font-weight: 500;
}

.info-item p {
  font-size: 16px;
  color: #1a1a2e;
  font-weight: 500;
}

/* ===== FORM STYLES ===== */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #1a1a2e;
  font-weight: 500;
}

.required {
  color: #e63946;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #e9ecef;
  border-radius: 10px;
  outline: none;
  transition: all 0.3s;
  font-size: 14px;
}

.form-group input:focus,
.form-group select:focus {
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.disabled-input {
  background: #f1f3f5;
  cursor: not-allowed;
  opacity: 0.7;
}

.form-hint {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #6c757d;
}

.info-edit {
  max-width: 600px;
}

.password-input {
  position: relative;
}

.password-input input {
  padding-right: 45px;
}

.password-input i {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: #6c757d;
  font-size: 18px;
}

.password-input i:hover {
  color: #1a1a2e;
}

.form-actions {
  display: flex;
  gap: 15px;
  margin-top: 20px;
}

/* ===== BUTTONS ===== */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 24px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-sm {
  padding: 8px 18px;
  font-size: 13px;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c62828;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(230, 57, 70, 0.3);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-outline {
  background: transparent;
  color: #333;
  border: 2px solid #e0e0e0;
}

.btn-outline:hover {
  background: #f5f5f5;
  border-color: #c62828;
}

/* ===== ORDERS ===== */
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  border: 1px solid #e9ecef;
  border-radius: 15px;
  padding: 20px;
  transition: all 0.3s;
}

.order-card:hover {
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.05);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  flex-wrap: wrap;
  gap: 10px;
}

.order-id {
  font-weight: 600;
  color: #e63946;
  margin-right: 15px;
}

.order-date {
  color: #6c757d;
  font-size: 14px;
}

.order-status {
  padding: 5px 15px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}
.status-processing {
  background: #cce5ff;
  color: #004085;
}
.status-shipped {
  background: #d1ecf1;
  color: #0c5460;
}
.status-delivered {
  background: #d4edda;
  color: #155724;
}
.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}

.order-items {
  margin: 15px 0;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
  border-bottom: 1px dashed #e9ecef;
}

.order-item:last-child {
  border-bottom: none;
}

.item-image {
  width: 50px;
  height: 50px;
  background: #f1f3f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-image i {
  font-size: 25px;
  color: #6c757d;
}

.item-details {
  flex: 1;
}

.item-details h4 {
  font-size: 14px;
  margin-bottom: 3px;
  color: #1a1a2e;
}

.item-variant {
  font-size: 12px;
  color: #6c757d;
}

.item-quantity {
  font-size: 12px;
  color: #6c757d;
}

.item-price {
  font-weight: 600;
  color: #e63946;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #e9ecef;
  flex-wrap: wrap;
  gap: 15px;
}

.order-total {
  font-size: 16px;
}

.order-total strong {
  color: #e63946;
  margin-left: 10px;
}

.order-actions {
  display: flex;
  gap: 10px;
}

/* ===== WISHLIST ===== */
.wishlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.wishlist-card {
  border: 1px solid #e9ecef;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
}

.wishlist-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.08);
}

.product-image {
  height: 150px;
  background: #f1f3f5;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  cursor: pointer;
}

.product-image i {
  font-size: 50px;
  color: #adb5bd;
}

.remove-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 30px;
  height: 30px;
  background: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.remove-btn:hover {
  background: #e63946;
  color: white;
}

.product-info {
  padding: 15px;
}

.product-info h3 {
  font-size: 14px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: color 0.3s;
}

.product-info h3:hover {
  color: #e63946;
}

.product-price {
  font-weight: 600;
  color: #e63946;
  margin-bottom: 10px;
  font-size: 16px;
}

.add-to-cart {
  width: 100%;
  background: #e63946;
  color: white;
  border: none;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
}

.add-to-cart:hover {
  background: #c62828;
}

/* ===== PASSWORD REQUIREMENTS ===== */
.password-requirements {
  background: #f8f9fa;
  padding: 15px 20px;
  border-radius: 10px;
  margin-bottom: 20px;
}

.password-requirements p {
  color: #1a1a2e;
  margin-bottom: 10px;
  font-weight: 500;
}

.password-requirements ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.password-requirements li {
  margin-bottom: 5px;
  color: #6c757d;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.password-requirements li i {
  font-size: 14px;
}

.password-requirements li.valid {
  color: #28a745;
}

.password-requirements li.valid i {
  color: #28a745;
}

/* ===== EMPTY STATE ===== */
.empty-state {
  text-align: center;
  padding: 50px 20px;
  background: #f8f9fa;
  border-radius: 15px;
}

.empty-state i {
  font-size: 60px;
  color: #adb5bd;
  margin-bottom: 15px;
}

.empty-state p {
  color: #6c757d;
  margin-bottom: 20px;
  font-size: 16px;
}

/* ===== ANIMATIONS ===== */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== RESPONSIVE ===== */
@media (max-width: 1024px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .profile-container {
    grid-template-columns: 1fr;
    border-radius: 12px;
  }

  .profile-sidebar {
    padding: 20px;
  }

  .profile-menu {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 5px;
  }

  .profile-menu a {
    padding: 10px 12px;
    font-size: 13px;
  }

  .profile-menu a i {
    width: 16px;
  }

  .profile-content {
    padding: 20px;
  }

  .banner-content {
    flex-direction: column;
    text-align: center;
  }

  .welcome-banner {
    flex-direction: column;
    gap: 15px;
  }

  .order-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-actions {
    width: 100%;
  }

  .order-actions .btn {
    flex: 1;
  }

  .wishlist-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .filters-bar {
    flex-direction: column;
  }

  .search-box {
    width: 100%;
  }

  .filter-select {
    width: 100%;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .modal-content {
    padding: 20px;
  }
}

@media (max-width: 480px) {
  .profile-menu {
    grid-template-columns: 1fr;
  }

  .wishlist-grid {
    grid-template-columns: 1fr;
  }

  .order-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions .btn {
    width: 100%;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>