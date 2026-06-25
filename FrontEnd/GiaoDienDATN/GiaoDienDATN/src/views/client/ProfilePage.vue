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
              <p class="profile-phone" v-if="profile.phone">
                <i class="fas fa-phone"></i> {{ profile.phone }}
              </p>
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
                  <div class="orders-header">
                    <h2>Đơn hàng của tôi</h2>
                    <div class="order-tabs">
                      <button 
                        v-for="tab in orderTabs" 
                        :key="tab.value"
                        :class="['tab-btn', { active: orderFilter === tab.value }]"
                        @click="changeOrderFilter(tab.value)"
                      >
                        {{ tab.label }}
                      </button>
                    </div>
                  </div>

                  <!-- Loading -->
                  <div v-if="ordersLoading" class="loading-state">
                    <i class="fas fa-spinner fa-spin"></i>
                    <p>Đang tải đơn hàng...</p>
                  </div>

                  <!-- Empty orders -->
                  <div v-else-if="orders.length === 0" class="empty-state">
                    <i class="fas fa-shopping-bag"></i>
                    <h3>Chưa có đơn hàng nào</h3>
                    <p>Bạn chưa có đơn hàng nào trong danh sách này</p>
                    <router-link to="/shop" class="btn-shop-now">
                      <i class="fas fa-shopping-cart"></i> Mua sắm ngay
                    </router-link>
                  </div>

                  <!-- Orders list -->
                  <div v-else class="orders-list">
                    <div v-for="order in orders" :key="order.orderId" class="order-card">
                      <div class="order-header">
                        <div class="order-info">
                          <span class="order-id">#{{ order.orderCode || order.orderId }}</span>
                          <span class="order-date">{{ formatDate(order.orderDate) }}</span>
                        </div>
                        <span :class="['order-status', getStatusClass(order.orderStatus)]">
                          {{ getStatusText(order.orderStatus) }}
                        </span>
                      </div>

                      <div class="order-items">
                        <div v-for="item in order.items" :key="item.itemId" class="order-item">
                          <div class="item-image">
                            <!-- ✅ SỬA: Thêm getImageUrl() -->
                            <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" @error="handleImageError" />
                            <i v-else class="fas fa-tshirt"></i>
                          </div>
                          <div class="item-details">
                            <h4>{{ item.productName }}</h4>
                            <p class="item-variant">
                              {{ item.sizeName || '' }} {{ item.colorName || '' }}
                            </p>
                            <p class="item-quantity">x{{ item.quantity }}</p>
                          </div>
                          <div class="item-price">
                            {{ formatPrice(item.finalAmount) }}
                          </div>
                        </div>
                      </div>

                      <!-- Hiển thị voucher nếu có -->
                      <div v-if="order.voucher" class="order-voucher">
                        <i class="fas fa-ticket-alt"></i>
                        <span class="voucher-code">{{ order.voucher.voucherCode }}</span>
                        <span class="voucher-discount">-{{ formatPrice(order.discountAmount || 0) }}</span>
                      </div>

                      <div class="order-footer">
                        <div class="order-total">
                          <span>Tổng cộng:</span>
                          <strong>{{ formatPrice(order.totalAmount) }}</strong>
                        </div>
                        <div class="order-actions">
                          <button class="btn btn-outline" @click="viewOrderDetail(order.orderId)">
                            <i class="fas fa-eye"></i> Xem chi tiết
                          </button>
                          <button 
                            v-if="order.orderStatus === 'PENDING_PAYMENT'" 
                            class="btn btn-danger" 
                            @click="handleCancelOrder(order.orderId)"
                          >
                            <i class="fas fa-times"></i> Hủy đơn
                          </button>
                          <button 
                            v-if="order.orderStatus === 'COMPLETED'" 
                            class="btn btn-primary" 
                            @click="handleReorder(order)"
                          >
                            <i class="fas fa-redo"></i> Mua lại
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Pagination -->
                  <div class="pagination" v-if="totalPages > 1">
                    <button 
                      class="page-btn"
                      :disabled="currentPage === 0"
                      @click="goToPage(currentPage - 1)"
                    >
                      <i class="fas fa-chevron-left"></i>
                    </button>
                    <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
                    <button 
                      class="page-btn"
                      :disabled="currentPage === totalPages - 1"
                      @click="goToPage(currentPage + 1)"
                    >
                      <i class="fas fa-chevron-right"></i>
                    </button>
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
                    <router-link to="/shop" class="btn-shop-now">
                      <i class="fas fa-shopping-cart"></i> Khám phá sản phẩm
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

    <!-- Cancel Reason Modal -->
    <div v-if="showCancelModal" class="modal-overlay" @click.self="closeCancelModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Hủy đơn hàng</h3>
          <button class="close-btn" @click="closeCancelModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Lý do hủy</label>
            <textarea v-model="cancelReason" rows="3" placeholder="Nhập lý do hủy đơn hàng..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeCancelModal">Đóng</button>
          <button class="btn btn-danger" @click="confirmCancelOrder" :disabled="cancelLoading">
            {{ cancelLoading ? 'Đang xử lý...' : 'Xác nhận hủy' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Order Detail Modal -->
    <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-container modal-lg">
        <div class="modal-header">
          <h3>Chi tiết đơn hàng #{{ selectedOrder?.orderCode || selectedOrder?.orderId }}</h3>
          <button class="close-btn" @click="closeDetailModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="order-info-grid">
            <div class="info-section">
              <h4>Thông tin khách hàng</h4>
              <p><strong>Họ tên:</strong> {{ selectedOrder?.customer?.customerName || 'Khách hàng' }}</p>
              <p><strong>SĐT:</strong> {{ selectedOrder?.customer?.phone || '---' }}</p>
              <p><strong>Email:</strong> {{ selectedOrder?.customer?.email || '---' }}</p>
            </div>
            <div class="info-section">
              <h4>Thông tin đơn hàng</h4>
              <p><strong>Ngày đặt:</strong> {{ formatDate(selectedOrder?.orderDate) }}</p>
              <p><strong>Trạng thái:</strong> {{ getStatusText(selectedOrder?.orderStatus) }}</p>
              <p><strong>Thanh toán:</strong> {{ selectedOrder?.paymentStatus || 'UNPAID' }}</p>
            </div>
          </div>

          <div class="order-items-section">
            <h4>Sản phẩm</h4>
            <table class="items-table">
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Đơn giá</th>
                  <th>Số lượng</th>
                  <th>Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in selectedOrder?.items" :key="item.itemId">
                  <td>
                    <div class="item-info">
                      <div class="item-image">
                        <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" />
                        <i v-else class="fas fa-tshirt"></i>
                      </div>
                      <div>
                        <div class="item-name">{{ item.productName }}</div>
                        <div class="item-variant">{{ item.sizeName || '' }} {{ item.colorName || '' }}</div>
                      </div>
                    </div>
                  </td>
                  <td>{{ formatPrice(item.unitPrice) }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ formatPrice(item.finalAmount) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="3" class="text-right">Tạm tính:</td>
                  <td>{{ formatPrice(selectedOrder?.subtotalAmount) }}</td>
                </tr>
                <tr v-if="selectedOrder?.discountAmount && selectedOrder?.discountAmount > 0" class="discount-row">
                  <td colspan="3" class="text-right">
                    <span class="voucher-label">
                      <i class="fas fa-ticket-alt"></i> Giảm giá 
                      ({{ selectedOrder?.voucher?.voucherCode || 'Voucher' }})
                    </span>
                  </td>
                  <td class="discount-amount">-{{ formatPrice(selectedOrder?.discountAmount) }}</td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right">Phí vận chuyển:</td>
                  <td>{{ formatPrice(selectedOrder?.shippingFee || 0) }}</td>
                </tr>
                <tr class="total-row">
                  <td colspan="3" class="text-right">Tổng cộng:</td>
                  <td class="grand-total">{{ formatPrice(selectedOrder?.totalAmount) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div v-if="selectedOrder?.voucher" class="voucher-info-section">
            <h4><i class="fas fa-ticket-alt"></i> Mã giảm giá đã áp dụng</h4>
            <div class="voucher-detail">
              <span class="voucher-code">{{ selectedOrder.voucher.voucherCode }}</span>
              <span class="voucher-savings">Tiết kiệm: {{ formatPrice(selectedOrder.discountAmount || 0) }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeDetailModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Toast Notification -->
    <div :class="['toast', { show: showToast, 'toast-error': toastType === 'error' }]">
      <i :class="toastType === 'error' ? 'fas fa-exclamation-circle' : 'fas fa-check-circle'"></i>
      {{ toastMessage }}
    </div>
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
import { orderService } from "../../services/orderService";
import axios from "axios";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const cartStore = useCartStore();
const { showNotification } = useNotification();

// ✅ THÊM BASE URL CHO ẢNH
const IMAGE_BASE = "http://localhost:8080";

// ✅ HÀM LẤY URL ẢNH ĐẦY ĐỦ
const getImageUrl = (imagePath) => {
  if (!imagePath) return '';
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath;
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath;
  return IMAGE_BASE + cleanPath;
};

// State
const activeTab = ref("info");
const editing = ref(false);
const loading = ref(false);
const passwordLoading = ref(false);
const pageLoading = ref(true);
const showWelcomeBanner = ref(false);
const ordersLoading = ref(false);
const wishlistLoading = ref(false);
const cancelLoading = ref(false);
const showCancelModal = ref(false);
const showDetailModal = ref(false);
const cancelOrderId = ref(null);
const cancelReason = ref("");
const selectedOrder = ref(null);
const currentPage = ref(0);
const totalPages = ref(0);
const orderFilter = ref("all");

// Profile data
const profile = reactive({
  fullName: "",
  email: "",
  phone: "",
  address: ""
});

const editForm = reactive({
  fullName: "",
  email: "",
  phone: "",
  address: ""
});

// Computed để hiển thị
const displayName = computed(() => {
  return profile.fullName || authStore.user?.username || "Người dùng";
});

const displayEmail = computed(() => {
  return profile.email || authStore.user?.email || "email@example.com";
});

// Orders data
const orders = ref([]);

// Wishlist data
const wishlist = ref([]);

// Toast
const showToast = ref(false);
const toastMessage = ref("");
const toastType = ref("success");
let toastTimer = null;

// Password change
const showCurrentPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);

const passwordForm = reactive({
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
});

// Order tabs
const orderTabs = [
  { value: 'all', label: 'Tất cả' },
  { value: 'PENDING_PAYMENT', label: 'Chờ thanh toán' },
  { value: 'PROCESSING', label: 'Đang xử lý' },
  { value: 'SHIPPING', label: 'Đang giao hàng' },
  { value: 'DELIVERED', label: 'Đã giao hàng' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'CANCELLED', label: 'Đã hủy' }
];

const isPasswordValid = computed(() => {
  return (
    passwordForm.newPassword.length >= 6 &&
    /[A-Z]/.test(passwordForm.newPassword) &&
    /[0-9]/.test(passwordForm.newPassword) &&
    passwordForm.newPassword === passwordForm.confirmPassword &&
    passwordForm.currentPassword.length > 0
  );
});

// ===== UTILITY FUNCTIONS =====
const showNotificationMsg = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer);
  toastMessage.value = message;
  toastType.value = type;
  showToast.value = true;
  toastTimer = setTimeout(() => {
    showToast.value = false;
  }, 3000);
};

const formatDate = (dateString) => {
  if (!dateString) return "---";
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return "---";
  return date.toLocaleDateString("vi-VN") + " " + 
         date.toLocaleTimeString("vi-VN", { hour: '2-digit', minute: '2-digit' });
};

const getStatusText = (status) => {
  const map = {
    "PENDING_PAYMENT": "Chờ thanh toán",
    "PROCESSING": "Đang xử lý",
    "SHIPPING": "Đang giao hàng",
    "DELIVERED": "Đã giao hàng",
    "COMPLETED": "Hoàn thành",
    "CANCELLED": "Đã hủy"
  };
  return map[status] || status;
};

const getStatusClass = (status) => {
  const map = {
    "PENDING_PAYMENT": "status-pending",
    "PROCESSING": "status-processing",
    "SHIPPING": "status-shipping",
    "DELIVERED": "status-delivered",
    "COMPLETED": "status-completed",
    "CANCELLED": "status-cancelled"
  };
  return map[status] || "";
};

const handleImageError = (event) => {
  event.target.style.display = 'none';
};

// ===== PROFILE FUNCTIONS =====
const fetchProfile = async () => {
  try {
    const token = authStore.token || localStorage.getItem("token");
    
    if (token) {
      const response = await axios.get("http://localhost:8080/api/customers/profile", {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json"
        }
      });

      if (response.data) {
        const data = response.data;
        profile.fullName = data.fullName || data.customerName || authStore.user?.username || "";
        profile.email = data.email || authStore.user?.email || "";
        profile.phone = data.phone || "";
        profile.address = data.address || "";

        if (authStore.user) {
          authStore.user.username = profile.fullName;
          authStore.user.email = profile.email;
          localStorage.setItem("user", JSON.stringify(authStore.user));
        }
      }
    } else {
      profile.fullName = authStore.user?.username || "";
      profile.email = authStore.user?.email || "";
    }

    if (route.query.newUser === 'true') {
      showWelcomeBanner.value = true;
      if (!profile.phone && !profile.address) {
        setTimeout(() => startEditing(), 500);
      }
    }
  } catch (error) {
    console.error("❌ Error fetching profile:", error);
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

const saveProfile = async () => {
  if (!editForm.fullName || editForm.fullName.trim().length === 0) {
    showNotificationMsg("Vui lòng nhập họ và tên", "error");
    return;
  }

  loading.value = true;
  try {
    const token = authStore.token || localStorage.getItem("token");
    
    const payload = {
      fullName: editForm.fullName.trim(),
      email: editForm.email,
      phone: editForm.phone ? editForm.phone.trim() : "",
      address: editForm.address ? editForm.address.trim() : ""
    };

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

      if (response.data) {
        const data = response.data;
        profile.fullName = data.fullName || data.customerName || editForm.fullName;
        profile.email = data.email || editForm.email;
        profile.phone = data.phone || editForm.phone || "";
        profile.address = data.address || editForm.address || "";

        if (authStore.user) {
          authStore.user.username = profile.fullName;
          authStore.user.email = profile.email;
          localStorage.setItem("user", JSON.stringify(authStore.user));
        }

        editing.value = false;
        showNotificationMsg("Cập nhật thông tin thành công!", "success");
        await fetchProfile();
      }
    } else {
      showNotificationMsg("Vui lòng đăng nhập để lưu thông tin", "warning");
      profile.fullName = editForm.fullName;
      profile.email = editForm.email;
      profile.phone = editForm.phone || "";
      profile.address = editForm.address || "";
      editing.value = false;
    }
  } catch (error) {
    console.error("❌ Error saving profile:", error);
    const message = error.response?.data?.message || "Có lỗi xảy ra khi cập nhật";
    showNotificationMsg(message, "error");
  } finally {
    loading.value = false;
  }
};

// ===== ORDERS FUNCTIONS =====
const fetchOrders = async () => {
  ordersLoading.value = true;
  try {
    const token = authStore.token || localStorage.getItem("token");
    
    if (!token) {
      ordersLoading.value = false;
      return;
    }

    const status = orderFilter.value === 'all' ? null : orderFilter.value;
    const response = await orderService.getMyOrders(status, currentPage.value, 10);
    
    console.log("📦 Orders response with filter:", orderFilter.value, response.data);
    
    if (response.data && response.data.content) {
      orders.value = response.data.content;
      totalPages.value = response.data.totalPages || 0;
      currentPage.value = response.data.number || 0;
    } else {
      orders.value = [];
    }
  } catch (error) {
    console.error("❌ Error fetching orders:", error);
    showNotificationMsg(error.response?.data?.message || "Không thể tải đơn hàng", "error");
    orders.value = [];
  } finally {
    ordersLoading.value = false;
  }
};

const changeOrderFilter = (filterValue) => {
  orderFilter.value = filterValue;
  currentPage.value = 0;
  fetchOrders();
};

const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page;
    fetchOrders();
  }
};

const viewOrderDetail = async (orderId) => {
  try {
    const response = await orderService.getOrderDetail(orderId);
    selectedOrder.value = response.data;
    showDetailModal.value = true;
  } catch (error) {
    console.error("❌ Error fetching order detail:", error);
    showNotificationMsg("Không thể tải chi tiết đơn hàng", "error");
  }
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedOrder.value = null;
};

const handleCancelOrder = (orderId) => {
  cancelOrderId.value = orderId;
  cancelReason.value = "";
  showCancelModal.value = true;
};

const closeCancelModal = () => {
  showCancelModal.value = false;
  cancelOrderId.value = null;
  cancelReason.value = "";
};

const confirmCancelOrder = async () => {
  if (!cancelReason.value.trim()) {
    showNotificationMsg("Vui lòng nhập lý do hủy đơn hàng", "error");
    return;
  }
  
  cancelLoading.value = true;
  try {
    await orderService.cancelOrder(cancelOrderId.value, cancelReason.value);
    showNotificationMsg("Hủy đơn hàng thành công!", "success");
    closeCancelModal();
    await fetchOrders();
  } catch (error) {
    console.error("❌ Error canceling order:", error);
    showNotificationMsg(error.response?.data?.message || "Không thể hủy đơn hàng", "error");
  } finally {
    cancelLoading.value = false;
  }
};

const handleReorder = (order) => {
  if (order.items && order.items.length > 0) {
    order.items.forEach(item => {
      cartStore.addToCart({
        productDetailId: item.productDetailId,
        productId: item.productId,
        productName: item.productName,
        price: item.unitPrice,
        quantity: item.quantity,
        imageUrl: item.imageUrl,
        sizeName: item.sizeName,
        colorName: item.colorName
      });
    });
    showNotificationMsg("Đã thêm sản phẩm vào giỏ hàng!", "success");
    router.push("/cart");
  }
};

// ===== WISHLIST FUNCTIONS =====
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

const goToProduct = (id) => {
  router.push(`/product/${id}`);
};

const addToCart = (product) => {
  cartStore.addToCart(product);
  showNotificationMsg("Đã thêm vào giỏ hàng", "success");
};

const removeFromWishlist = (id) => {
  wishlist.value = wishlist.value.filter((p) => p.id !== id);
  showNotificationMsg("Đã xóa khỏi danh sách yêu thích", "info");
};

// ===== PASSWORD FUNCTIONS =====
const changePassword = async () => {
  if (!isPasswordValid.value) {
    showNotificationMsg("Vui lòng kiểm tra lại mật khẩu", "error");
    return;
  }

  passwordLoading.value = true;
  try {
    const token = authStore.token || localStorage.getItem("token");
    
    if (token) {
      await axios.put(
        "http://localhost:8080/api/customers/change-password",
        {
          currentPassword: passwordForm.currentPassword,
          newPassword: passwordForm.newPassword
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json"
          }
        }
      );
      showNotificationMsg("Đổi mật khẩu thành công!", "success");
      passwordForm.currentPassword = "";
      passwordForm.newPassword = "";
      passwordForm.confirmPassword = "";
    } else {
      showNotificationMsg("Vui lòng đăng nhập để đổi mật khẩu", "warning");
    }
  } catch (error) {
    console.error("Error changing password:", error);
    const message = error.response?.data?.message || "Có lỗi xảy ra";
    showNotificationMsg(message, "error");
  } finally {
    passwordLoading.value = false;
  }
};

const changeAvatar = () => {
  showNotificationMsg("Chức năng đang phát triển", "info");
};

const logout = () => {
  if (confirm("Bạn có chắc muốn đăng xuất?")) {
    authStore.logout();
    router.push("/home");
  }
};

// ===== WATCH =====
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

watch(
  () => route.query.newUser,
  (newVal) => {
    if (newVal === 'true') {
      showWelcomeBanner.value = true;
      if (!profile.phone && !profile.address) {
        setTimeout(() => startEditing(), 500);
      }
    }
  }
);

// ✅ Lắng nghe query param tab
watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab === 'orders') {
      activeTab.value = 'orders';
      fetchOrders();
    }
  },
  { immediate: true }
);

// ===== LIFECYCLE =====
onMounted(() => {
  console.log("🚀 Profile page mounted");
  
  // ✅ Kiểm tra query param tab
  if (route.query.tab === 'orders') {
    activeTab.value = 'orders';
  }
  
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

.profile-phone {
  margin-top: 5px;
  font-size: 13px;
  opacity: 0.8;
}

.profile-phone i {
  margin-right: 5px;
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
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #e9ecef;
  border-radius: 10px;
  outline: none;
  transition: all 0.3s;
  font-size: 14px;
  font-family: inherit;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.form-group textarea {
  min-height: 80px;
  resize: vertical;
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

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

/* ===== ORDERS ===== */
.orders-header {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 15px;
}

.orders-header h2 {
  margin-bottom: 0;
}

.order-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 5px 0 10px;
}

.tab-btn {
  padding: 6px 16px;
  background: #f0f0f0;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #666;
  transition: all 0.3s;
}

.tab-btn:hover {
  background: #e0e0e0;
}

.tab-btn.active {
  background: #e63946;
  color: white;
}

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

.order-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.order-id {
  font-weight: 700;
  color: #e63946;
  font-size: 16px;
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
.status-shipping {
  background: #d1ecf1;
  color: #0c5460;
}
.status-delivered {
  background: #d4edda;
  color: #155724;
}
.status-completed {
  background: #d4edda;
  color: #155724;
}
.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}

.order-items {
  margin: 10px 0;
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
  width: 60px;
  height: 60px;
  background: #f1f3f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-image i {
  font-size: 30px;
  color: #adb5bd;
}

.item-details {
  flex: 1;
}

.item-details h4 {
  font-size: 15px;
  color: #1a1a2e;
  margin-bottom: 3px;
}

.item-variant {
  font-size: 13px;
  color: #6c757d;
}

.item-quantity {
  font-size: 13px;
  color: #6c757d;
}

.item-price {
  font-weight: 600;
  color: #e63946;
  font-size: 15px;
}

.order-voucher {
  background: #e8f5e9;
  padding: 10px 15px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 5px 0 15px;
  border-left: 4px solid #2e7d32;
}

.order-voucher i {
  color: #2e7d32;
  font-size: 18px;
}

.order-voucher .voucher-code {
  font-weight: 700;
  color: #2e7d32;
  background: white;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 14px;
}

.order-voucher .voucher-discount {
  color: #e63946;
  font-weight: 700;
  margin-left: auto;
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
  flex-wrap: wrap;
}

/* ===== PAGINATION ===== */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #666;
}

/* ===== EMPTY STATE ===== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #f8f9fa;
  border-radius: 15px;
}

.empty-state i {
  font-size: 70px;
  color: #e63946;
  margin-bottom: 15px;
  opacity: 0.7;
}

.empty-state h3 {
  color: #1a1a2e;
  font-size: 22px;
  margin-bottom: 10px;
}

.empty-state p {
  color: #6c757d;
  margin-bottom: 25px;
  font-size: 16px;
}

/* ✅ SỬA NÚT MUA SẮM NGAY */
.btn-shop-now {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 32px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  box-shadow: 0 4px 15px rgba(230, 57, 70, 0.3);
}

.btn-shop-now:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 25px rgba(230, 57, 70, 0.4);
  background: linear-gradient(135deg, #c62828, #e63946);
}

.btn-shop-now i {
  font-size: 18px;
}

/* ===== MODAL ===== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.modal-container {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideIn 0.3s;
}

.modal-container.modal-sm {
  max-width: 450px;
}

.modal-container.modal-lg {
  max-width: 800px;
}

@keyframes slideIn {
  from { transform: translateY(-30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border-radius: 12px 12px 0 0;
  position: sticky;
  top: 0;
  z-index: 1;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: white;
}

.close-btn:hover {
  opacity: 0.8;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-radius: 0 0 12px 12px;
}

.order-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.info-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
}

.info-section h4 {
  color: #2c3e50;
  margin-bottom: 10px;
  padding-bottom: 5px;
  border-bottom: 1px solid #eee;
}

.info-section p {
  margin-bottom: 5px;
  font-size: 14px;
  color: #666;
}

.order-items-section h4 {
  color: #2c3e50;
  margin-bottom: 15px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
}

.items-table th {
  background: #f8f9fa;
  padding: 10px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
}

.items-table td {
  padding: 10px;
  border-bottom: 1px solid #eee;
  vertical-align: middle;
}

.items-table tfoot td {
  border-bottom: none;
  padding: 8px 10px;
}

.text-right {
  text-align: right;
}

.total-row {
  font-weight: 700;
}

.grand-total {
  font-size: 18px;
  color: #e63946;
}

.discount-row td {
  color: #28a745;
  font-weight: 600;
}

.discount-row .voucher-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.discount-row .discount-amount {
  color: #28a745;
  font-weight: 700;
}

.voucher-info-section {
  background: #e8f5e9;
  padding: 15px 20px;
  border-radius: 8px;
  margin-top: 20px;
  border-left: 4px solid #2e7d32;
}

.voucher-info-section h4 {
  color: #2e7d32;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.voucher-info-section .voucher-detail {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.voucher-info-section .voucher-code {
  font-weight: 700;
  color: #2e7d32;
  background: white;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 16px;
}

.voucher-info-section .voucher-savings {
  color: #e63946;
  font-weight: 700;
  font-size: 15px;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-info .item-image {
  width: 40px;
  height: 40px;
}

.item-info .item-image i {
  font-size: 20px;
}

.item-info .item-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.item-info .item-variant {
  font-size: 12px;
  color: #999;
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

/* ===== TOAST ===== */
.toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  padding: 12px 24px;
  border-radius: 10px;
  background: #27ae60;
  color: white;
  font-size: 14px;
  z-index: 9999;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.4s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 10px;
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}

/* ===== ANIMATIONS ===== */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideIn {
  from { transform: translateY(-30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
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

  .order-info-grid {
    grid-template-columns: 1fr;
  }

  .modal-container.modal-lg {
    max-width: 100%;
    margin: 10px;
  }

  .order-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
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

  .order-voucher {
    flex-wrap: wrap;
  }
  
  .order-voucher .voucher-discount {
    margin-left: 0;
  }
}
</style>