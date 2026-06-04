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
              <h3>{{ authStore.user?.name || "Người dùng" }}</h3>
              <p>{{ authStore.user?.email || "email@example.com" }}</p>
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
                  :class="{ active: activeTab === 'address' }"
                  @click.prevent="activeTab = 'address'"
                >
                  <i class="fas fa-map-marker-alt"></i>
                  Sổ địa chỉ
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
              <li>
                <a
                  href="#"
                  :class="{ active: activeTab === 'payment' }"
                  @click.prevent="activeTab = 'payment'"
                >
                  <i class="fas fa-credit-card"></i>
                  Phương thức thanh toán
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
            <!-- Thông tin tài khoản -->
            <div v-show="activeTab === 'info'" class="profile-tab">
              <h2>Thông tin tài khoản</h2>

              <div v-if="!editing" class="info-display">
                <div class="info-grid">
                  <div class="info-item">
                    <label>Họ và tên</label>
                    <p>{{ profile.fullName }}</p>
                  </div>
                  <div class="info-item">
                    <label>Email</label>
                    <p>{{ profile.email }}</p>
                  </div>
                  <div class="info-item">
                    <label>Số điện thoại</label>
                    <p>{{ profile.phone || "Chưa cập nhật" }}</p>
                  </div>
                  <div class="info-item">
                    <label>Ngày sinh</label>
                    <p>{{ profile.birthday || "Chưa cập nhật" }}</p>
                  </div>
                  <div class="info-item">
                    <label>Giới tính</label>
                    <p>{{ profile.gender || "Chưa cập nhật" }}</p>
                  </div>
                </div>
                <button class="btn btn-primary" @click="startEditing">
                  <i class="fas fa-edit"></i>
                  Chỉnh sửa thông tin
                </button>
              </div>

              <form v-else @submit.prevent="saveProfile" class="info-edit">
                <div class="form-group">
                  <label>Họ và tên</label>
                  <input type="text" v-model="editForm.fullName" required />
                </div>
                <div class="form-group">
                  <label>Email</label>
                  <input type="email" v-model="editForm.email" required />
                </div>
                <div class="form-group">
                  <label>Số điện thoại</label>
                  <input type="tel" v-model="editForm.phone" />
                </div>
                <div class="form-group">
                  <label>Ngày sinh</label>
                  <input type="date" v-model="editForm.birthday" />
                </div>
                <div class="form-group">
                  <label>Giới tính</label>
                  <select v-model="editForm.gender">
                    <option value="">Chọn giới tính</option>
                    <option value="Nam">Nam</option>
                    <option value="Nữ">Nữ</option>
                    <option value="Khác">Khác</option>
                  </select>
                </div>
                <div class="form-actions">
                  <button type="submit" class="btn btn-primary">
                    Lưu thay đổi
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

              <div v-if="orders.length === 0" class="empty-state">
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
                        <i :class="['fas', `fa-${item.image}`]"></i>
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

              <div v-if="wishlist.length === 0" class="empty-state">
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
                    <i :class="['fas', `fa-${product.image}`]"></i>
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

            <!-- Sổ địa chỉ -->
            <div v-show="activeTab === 'address'" class="profile-tab">
              <div class="tab-header">
                <h2>Sổ địa chỉ</h2>
                <button class="btn btn-primary" @click="showAddressForm = true">
                  <i class="fas fa-plus"></i>
                  Thêm địa chỉ mới
                </button>
              </div>

              <div v-if="addresses.length === 0" class="empty-state">
                <i class="fas fa-map-marker-alt"></i>
                <p>Bạn chưa có địa chỉ nào</p>
              </div>

              <div v-else class="addresses-list">
                <div
                  v-for="address in addresses"
                  :key="address.id"
                  class="address-card"
                >
                  <div class="address-type" v-if="address.isDefault">
                    Mặc định
                  </div>
                  <div class="address-content">
                    <h4>{{ address.name }}</h4>
                    <p>{{ address.phone }}</p>
                    <p>
                      {{ address.address }}, {{ address.district }},
                      {{ address.city }}
                    </p>
                  </div>
                  <div class="address-actions">
                    <button class="btn-icon" @click="editAddress(address)">
                      <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn-icon" @click="deleteAddress(address.id)">
                      <i class="fas fa-trash"></i>
                    </button>
                    <button
                      v-if="!address.isDefault"
                      class="btn-link"
                      @click="setDefaultAddress(address.id)"
                    >
                      Đặt làm mặc định
                    </button>
                  </div>
                </div>
              </div>

              <!-- Address Form Modal -->
              <div v-if="showAddressForm" class="modal">
                <div class="modal-content">
                  <h3>
                    {{ editingAddress ? "Sửa địa chỉ" : "Thêm địa chỉ mới" }}
                  </h3>
                  <form @submit.prevent="saveAddress">
                    <div class="form-group">
                      <label>Họ tên người nhận *</label>
                      <input type="text" v-model="addressForm.name" required />
                    </div>
                    <div class="form-group">
                      <label>Số điện thoại *</label>
                      <input type="tel" v-model="addressForm.phone" required />
                    </div>
                    <div class="form-group">
                      <label>Tỉnh/Thành phố *</label>
                      <select v-model="addressForm.city" required>
                        <option value="">Chọn tỉnh/thành phố</option>
                        <option
                          v-for="city in cities"
                          :key="city"
                          :value="city"
                        >
                          {{ city }}
                        </option>
                      </select>
                    </div>
                    <div class="form-group">
                      <label>Quận/Huyện *</label>
                      <select v-model="addressForm.district" required>
                        <option value="">Chọn quận/huyện</option>
                        <option
                          v-for="district in getDistricts(addressForm.city)"
                          :key="district"
                          :value="district"
                        >
                          {{ district }}
                        </option>
                      </select>
                    </div>
                    <div class="form-group">
                      <label>Địa chỉ cụ thể *</label>
                      <input
                        type="text"
                        v-model="addressForm.address"
                        required
                      />
                    </div>
                    <div class="form-group">
                      <label>
                        <input
                          type="checkbox"
                          v-model="addressForm.isDefault"
                        />
                        Đặt làm địa chỉ mặc định
                      </label>
                    </div>
                    <div class="form-actions">
                      <button type="submit" class="btn btn-primary">
                        {{ editingAddress ? "Cập nhật" : "Thêm mới" }}
                      </button>
                      <button
                        type="button"
                        class="btn btn-outline"
                        @click="closeAddressForm"
                      >
                        Hủy
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            </div>

            <!-- Đổi mật khẩu -->
            <div v-show="activeTab === 'password'" class="profile-tab">
              <h2>Đổi mật khẩu</h2>

              <form @submit.prevent="changePassword" class="password-form">
                <div class="form-group">
                  <label>Mật khẩu hiện tại *</label>
                  <div class="password-input">
                    <input
                      :type="showCurrentPassword ? 'text' : 'password'"
                      v-model="passwordForm.currentPassword"
                      required
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
                  <label>Mật khẩu mới *</label>
                  <div class="password-input">
                    <input
                      :type="showNewPassword ? 'text' : 'password'"
                      v-model="passwordForm.newPassword"
                      required
                      minlength="6"
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
                  <label>Xác nhận mật khẩu mới *</label>
                  <div class="password-input">
                    <input
                      :type="showConfirmPassword ? 'text' : 'password'"
                      v-model="passwordForm.confirmPassword"
                      required
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
                  :disabled="!isPasswordValid"
                >
                  Đổi mật khẩu
                </button>
              </form>
            </div>

            <!-- Phương thức thanh toán -->
            <div v-show="activeTab === 'payment'" class="profile-tab">
              <div class="tab-header">
                <h2>Phương thức thanh toán</h2>
                <button class="btn btn-primary" @click="showPaymentForm = true">
                  <i class="fas fa-plus"></i>
                  Thêm thẻ mới
                </button>
              </div>

              <div v-if="paymentMethods.length === 0" class="empty-state">
                <i class="fas fa-credit-card"></i>
                <p>Bạn chưa thêm phương thức thanh toán nào</p>
              </div>

              <div v-else class="payment-list">
                <div
                  v-for="method in paymentMethods"
                  :key="method.id"
                  class="payment-card"
                >
                  <div class="payment-icon">
                    <i :class="method.icon"></i>
                  </div>
                  <div class="payment-info">
                    <h4>
                      {{
                        method.type === "card" ? "Thẻ tín dụng" : "Ví điện tử"
                      }}
                    </h4>
                    <p>{{ method.details }}</p>
                    <p>Hết hạn: {{ method.expiry || "N/A" }}</p>
                  </div>
                  <div class="payment-actions">
                    <button
                      class="btn-icon"
                      @click="deletePaymentMethod(method.id)"
                    >
                      <i class="fas fa-trash"></i>
                    </button>
                    <span v-if="method.isDefault" class="default-badge"
                      >Mặc định</span
                    >
                  </div>
                </div>
              </div>

              <!-- Payment Form Modal -->
              <div v-if="showPaymentForm" class="modal">
                <div class="modal-content">
                  <h3>Thêm phương thức thanh toán</h3>
                  <form @submit.prevent="addPaymentMethod">
                    <div class="form-group">
                      <label>Loại thẻ</label>
                      <select v-model="paymentForm.type">
                        <option value="visa">Visa</option>
                        <option value="mastercard">Mastercard</option>
                        <option value="momo">MoMo</option>
                        <option value="zalopay">ZaloPay</option>
                      </select>
                    </div>
                    <div class="form-group">
                      <label>Số thẻ</label>
                      <input
                        type="text"
                        v-model="paymentForm.cardNumber"
                        placeholder="1234 5678 9012 3456"
                        maxlength="19"
                        @input="formatCardNumber"
                      />
                    </div>
                    <div class="form-row">
                      <div class="form-group">
                        <label>Ngày hết hạn</label>
                        <input
                          type="text"
                          v-model="paymentForm.expiry"
                          placeholder="MM/YY"
                          maxlength="5"
                          @input="formatExpiry"
                        />
                      </div>
                      <div class="form-group">
                        <label>CVV</label>
                        <input
                          type="text"
                          v-model="paymentForm.cvv"
                          placeholder="123"
                          maxlength="3"
                        />
                      </div>
                    </div>
                    <div class="form-group">
                      <label>Tên chủ thẻ</label>
                      <input type="text" v-model="paymentForm.cardholderName" />
                    </div>
                    <div class="form-group">
                      <label>
                        <input
                          type="checkbox"
                          v-model="paymentForm.isDefault"
                        />
                        Đặt làm phương thức mặc định
                      </label>
                    </div>
                    <div class="form-actions">
                      <button type="submit" class="btn btn-primary">
                        Thêm thẻ
                      </button>
                      <button
                        type="button"
                        class="btn btn-outline"
                        @click="closePaymentForm"
                      >
                        Hủy
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import { ref, reactive, computed } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../../stores/authStore";
import { useCartStore } from "../../stores/cartStore";
import { useNotification } from "../../composables/useNotification";
import ClientHeader from "../../components/client/ClientHeader.vue";
import ClientFooter from "../../components/client/ClientFooter.vue";
import { formatPrice } from "../../utils/helpers";

const router = useRouter();
const authStore = useAuthStore();
const cartStore = useCartStore();
const { showNotification } = useNotification();

// State
const activeTab = ref("info");
const editing = ref(false);

// Profile data
const profile = reactive({
  fullName: authStore.user?.name || "Nguyễn Văn A",
  email: authStore.user?.email || "nguyenvana@email.com",
  phone: "0123456789",
  birthday: "1990-01-01",
  gender: "Nam",
});

const editForm = reactive({ ...profile });

// Orders data
const orders = ref([
  {
    id: "DH001",
    date: "10/03/2024",
    status: "delivered",
    total: 897000,
    items: [
      {
        id: 1,
        name: "Áo thun nam",
        image: "tshirt",
        price: 199000,
        quantity: 2,
        size: "M",
        color: "Đen",
      },
      {
        id: 3,
        name: "Quần jean nam",
        image: "jeans",
        price: 499000,
        quantity: 1,
        size: "L",
        color: "Xanh",
      },
    ],
  },
  {
    id: "DH002",
    date: "15/03/2024",
    status: "processing",
    total: 399000,
    items: [
      {
        id: 2,
        name: "Đầm nữ công sở",
        image: "dress",
        price: 399000,
        quantity: 1,
        size: "M",
        color: "Đỏ",
      },
    ],
  },
]);

// Wishlist data
const wishlist = ref([
  { id: 1, name: "Áo thun nam", price: 199000, image: "tshirt" },
  { id: 2, name: "Đầm nữ công sở", price: 399000, image: "dress" },
]);

// Address data
const cities = ["Hồ Chí Minh", "Hà Nội", "Đà Nẵng"];
const districts = {
  "Hồ Chí Minh": ["Quận 1", "Quận 2", "Quận 3", "Quận 4", "Quận 5"],
  "Hà Nội": ["Ba Đình", "Hoàn Kiếm", "Hai Bà Trưng"],
  "Đà Nẵng": ["Hải Châu", "Thanh Khê", "Sơn Trà"],
};

const addresses = ref([
  {
    id: 1,
    name: "Nguyễn Văn A",
    phone: "0123456789",
    address: "123 Nguyễn Huệ",
    district: "Quận 1",
    city: "Hồ Chí Minh",
    isDefault: true,
  },
]);

const showAddressForm = ref(false);
const editingAddress = ref(null);
const addressForm = reactive({
  name: "",
  phone: "",
  city: "",
  district: "",
  address: "",
  isDefault: false,
});

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
    passwordForm.newPassword === passwordForm.confirmPassword
  );
});

// Payment methods
const paymentMethods = ref([
  {
    id: 1,
    type: "visa",
    icon: "fab fa-cc-visa",
    details: "**** **** **** 1234",
    expiry: "12/25",
    isDefault: true,
  },
]);

const showPaymentForm = ref(false);
const paymentForm = reactive({
  type: "visa",
  cardNumber: "",
  expiry: "",
  cvv: "",
  cardholderName: "",
  isDefault: false,
});

// Methods
const getDistricts = (city) => {
  return districts[city] || [];
};

const startEditing = () => {
  Object.assign(editForm, profile);
  editing.value = true;
};

const cancelEditing = () => {
  editing.value = false;
};

const saveProfile = () => {
  Object.assign(profile, editForm);
  editing.value = false;
  showNotification("Cập nhật thông tin thành công!", "success");
};

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

const editAddress = (address) => {
  editingAddress.value = address;
  Object.assign(addressForm, address);
  showAddressForm.value = true;
};

const deleteAddress = (id) => {
  if (confirm("Bạn có chắc muốn xóa địa chỉ này?")) {
    addresses.value = addresses.value.filter((a) => a.id !== id);
    showNotification("Đã xóa địa chỉ", "info");
  }
};

const setDefaultAddress = (id) => {
  addresses.value.forEach((a) => {
    a.isDefault = a.id === id;
  });
  showNotification("Đã cập nhật địa chỉ mặc định", "success");
};

const saveAddress = () => {
  if (editingAddress.value) {
    // Update existing address
    const index = addresses.value.findIndex(
      (a) => a.id === editingAddress.value.id,
    );
    if (index !== -1) {
      addresses.value[index] = { ...addressForm, id: editingAddress.value.id };
    }
    showNotification("Cập nhật địa chỉ thành công!", "success");
  } else {
    // Add new address
    addresses.value.push({
      ...addressForm,
      id: Date.now(),
    });
    showNotification("Thêm địa chỉ thành công!", "success");
  }
  closeAddressForm();
};

const closeAddressForm = () => {
  showAddressForm.value = false;
  editingAddress.value = null;
  Object.assign(addressForm, {
    name: "",
    phone: "",
    city: "",
    district: "",
    address: "",
    isDefault: false,
  });
};

const changePassword = () => {
  if (!isPasswordValid.value) {
    showNotification("Mật khẩu không hợp lệ", "error");
    return;
  }

  // Simulate API call
  setTimeout(() => {
    showNotification("Đổi mật khẩu thành công!", "success");
    passwordForm.currentPassword = "";
    passwordForm.newPassword = "";
    passwordForm.confirmPassword = "";
  }, 1000);
};

const formatCardNumber = (e) => {
  let value = e.target.value.replace(/\s/g, "");
  if (value.length > 16) value = value.slice(0, 16);

  const parts = [];
  for (let i = 0; i < value.length; i += 4) {
    parts.push(value.slice(i, i + 4));
  }

  paymentForm.cardNumber = parts.join(" ");
};

const formatExpiry = (e) => {
  let value = e.target.value.replace("/", "");
  if (value.length > 4) value = value.slice(0, 4);

  if (value.length >= 2) {
    paymentForm.expiry = value.slice(0, 2) + "/" + value.slice(2);
  } else {
    paymentForm.expiry = value;
  }
};

const addPaymentMethod = () => {
  paymentMethods.value.push({
    id: Date.now(),
    type: paymentForm.type,
    icon: `fab fa-cc-${paymentForm.type}`,
    details: `**** **** **** ${paymentForm.cardNumber.slice(-4)}`,
    expiry: paymentForm.expiry,
    isDefault: paymentForm.isDefault,
  });

  closePaymentForm();
  showNotification("Thêm phương thức thanh toán thành công!", "success");
};

const closePaymentForm = () => {
  showPaymentForm.value = false;
  Object.assign(paymentForm, {
    type: "visa",
    cardNumber: "",
    expiry: "",
    cvv: "",
    cardholderName: "",
    isDefault: false,
  });
};

const deletePaymentMethod = (id) => {
  if (confirm("Bạn có chắc muốn xóa phương thức thanh toán này?")) {
    paymentMethods.value = paymentMethods.value.filter((m) => m.id !== id);
    showNotification("Đã xóa phương thức thanh toán", "info");
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
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
}

main {
  margin-top: 140px;
  padding: 20px 0 50px;
}

.profile-container {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 30px;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: var(--shadow);
}

/* Sidebar */
.profile-sidebar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

/* Main Content */
.profile-content {
  padding: 30px;
}

.profile-tab {
  animation: fadeIn 0.3s;
}

.profile-tab h2 {
  color: var(--dark-gray);
  margin-bottom: 20px;
  font-size: 24px;
}

/* Info Display */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.info-item label {
  display: block;
  font-size: 14px;
  color: var(--medium-gray);
  margin-bottom: 5px;
}

.info-item p {
  font-size: 16px;
  color: var(--dark-gray);
  font-weight: 500;
}

/* Form Styles */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: var(--dark-gray);
  font-weight: 500;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  transition: all 0.3s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.password-input {
  position: relative;
}

.password-input i {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: var(--medium-gray);
}

.form-actions {
  display: flex;
  gap: 15px;
  margin-top: 20px;
}

/* Orders */
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 20px;
  transition: all 0.3s;
}

.order-card:hover {
  box-shadow: var(--shadow);
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
  color: var(--primary-red);
  margin-right: 15px;
}

.order-date {
  color: var(--medium-gray);
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
  border-bottom: 1px dashed var(--border-color);
}

.order-item:last-child {
  border-bottom: none;
}

.item-image {
  width: 50px;
  height: 50px;
  background: var(--light-gray);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-image i {
  font-size: 25px;
  color: var(--medium-gray);
}

.item-details {
  flex: 1;
}

.item-details h4 {
  font-size: 14px;
  margin-bottom: 3px;
  color: var(--dark-gray);
}

.item-variant {
  font-size: 12px;
  color: var(--medium-gray);
}

.item-quantity {
  font-size: 12px;
  color: var(--medium-gray);
}

.item-price {
  font-weight: 600;
  color: var(--primary-red);
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid var(--border-color);
  flex-wrap: wrap;
  gap: 15px;
}

.order-total {
  font-size: 16px;
}

.order-total strong {
  color: var(--primary-red);
  margin-left: 10px;
}

.order-actions {
  display: flex;
  gap: 10px;
}

/* Wishlist */
.wishlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.wishlist-card {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.3s;
}

.wishlist-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow);
}

.product-image {
  height: 150px;
  background: var(--light-gray);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  cursor: pointer;
}

.product-image i {
  font-size: 50px;
  color: var(--medium-gray);
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
}

.remove-btn:hover {
  background: var(--primary-red);
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
  color: var(--primary-red);
}

.product-price {
  font-weight: 600;
  color: var(--primary-red);
  margin-bottom: 10px;
}

.add-to-cart {
  width: 100%;
  background: var(--primary-red);
  color: white;
  border: none;
  padding: 8px;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.3s;
}

.add-to-cart:hover {
  background: var(--dark-red);
}

/* Addresses */
.addresses-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.address-card {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 20px;
  position: relative;
  transition: all 0.3s;
}

.address-card:hover {
  box-shadow: var(--shadow);
}

.address-type {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 10px;
  background: var(--primary-red);
  color: white;
  border-radius: 15px;
  font-size: 11px;
  font-weight: 600;
}

.address-content h4 {
  color: var(--dark-gray);
  margin-bottom: 5px;
}

.address-content p {
  color: var(--medium-gray);
  margin-bottom: 3px;
  font-size: 14px;
}

.address-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  flex-wrap: wrap;
}

.btn-icon {
  width: 35px;
  height: 35px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-icon:hover {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
}

.btn-link {
  background: none;
  border: none;
  color: var(--primary-red);
  cursor: pointer;
  font-size: 14px;
  text-decoration: underline;
}

/* Password Requirements */
.password-requirements {
  background: var(--light-gray);
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.password-requirements p {
  color: var(--dark-gray);
  margin-bottom: 10px;
  font-weight: 500;
}

.password-requirements ul {
  list-style: none;
}

.password-requirements li {
  margin-bottom: 5px;
  color: var(--medium-gray);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.password-requirements li i {
  font-size: 12px;
}

.password-requirements li.valid {
  color: #28a745;
}

.password-requirements li.valid i {
  color: #28a745;
}

/* Payment Methods */
.payment-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.payment-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 15px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  transition: all 0.3s;
}

.payment-card:hover {
  box-shadow: var(--shadow);
}

.payment-icon i {
  font-size: 40px;
  color: var(--primary-red);
}

.payment-info {
  flex: 1;
}

.payment-info h4 {
  color: var(--dark-gray);
  margin-bottom: 5px;
}

.payment-info p {
  color: var(--medium-gray);
  font-size: 14px;
  margin-bottom: 2px;
}

.payment-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.default-badge {
  padding: 3px 10px;
  background: var(--light-gray);
  border-radius: 15px;
  font-size: 11px;
  color: var(--medium-gray);
}

/* Modal */
.modal {
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

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 15px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h3 {
  color: var(--dark-gray);
  margin-bottom: 20px;
  font-size: 20px;
}

/* Tab Header */
.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 50px;
  background: var(--light-gray);
  border-radius: 10px;
}

.empty-state i {
  font-size: 60px;
  color: var(--medium-gray);
  margin-bottom: 15px;
}

.empty-state p {
  color: var(--medium-gray);
  margin-bottom: 20px;
}

/* Form Row */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

/* Animations */
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

/* Responsive */
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
  }

  .profile-sidebar {
    padding: 20px;
  }

  .profile-menu {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 5px;
  }

  .profile-menu a {
    padding: 10px;
    font-size: 13px;
  }

  .profile-menu a i {
    width: 16px;
  }

  .order-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-actions {
    width: 100%;
    justify-content: space-between;
  }

  .wishlist-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .payment-card {
    flex-direction: column;
    text-align: center;
  }

  .payment-actions {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .profile-menu {
    grid-template-columns: 1fr;
  }

  .wishlist-grid {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .order-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .tab-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .modal-content {
    padding: 20px;
  }
}
</style>
