<template>
  <div class="checkout-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <h1 class="page-title">THANH TOÁN</h1>

        <!-- Empty Cart -->
        <div v-if="cartStore.items.length === 0" class="empty-cart">
          <i class="fas fa-shopping-cart"></i>
          <h2>Giỏ hàng trống</h2>
          <p>Vui lòng thêm sản phẩm vào giỏ hàng trước khi thanh toán</p>
          <router-link to="/shop" class="btn btn-primary">
            Tiếp tục mua sắm
          </router-link>
        </div>

        <!-- Checkout Content -->
        <div v-else class="checkout-content">
          <form @submit.prevent="placeOrder" class="checkout-form">
            <!-- Thông tin giao hàng -->
            <div class="form-section">
              <h2>Thông tin giao hàng</h2>
              
              <div class="form-row">
                <div class="form-group">
                  <label>Họ và tên *</label>
                  <input 
                    type="text" 
                    v-model="form.fullName" 
                    required
                    placeholder="Nhập họ và tên"
                  >
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>Số điện thoại *</label>
                  <input 
                    type="tel" 
                    v-model="form.phone" 
                    required
                    placeholder="Nhập số điện thoại"
                  >
                </div>
                <div class="form-group">
                  <label>Email</label>
                  <input 
                    type="email" 
                    v-model="form.email" 
                    placeholder="Nhập email"
                  >
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>Tỉnh/Thành phố *</label>
                  <select v-model="form.city" required>
                    <option value="">Chọn tỉnh/thành phố</option>
                    <option v-for="city in cities" :key="city" :value="city">
                      {{ city }}
                    </option>
                  </select>
                </div>
                <div class="form-group">
                  <label>Quận/Huyện *</label>
                  <select v-model="form.district" required>
                    <option value="">Chọn quận/huyện</option>
                    <option v-for="district in districts" :key="district" :value="district">
                      {{ district }}
                    </option>
                  </select>
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>Địa chỉ cụ thể *</label>
                  <input 
                    type="text" 
                    v-model="form.address" 
                    required
                    placeholder="Số nhà, tên đường"
                  >
                </div>
              </div>
            </div>

            <!-- Phương thức vận chuyển -->
            <div class="form-section">
              <h2>Phương thức vận chuyển</h2>
              
              <div class="shipping-methods">
                <label 
                  v-for="method in shippingMethods" 
                  :key="method.id"
                  class="shipping-method"
                  :class="{ active: form.shippingMethod === method.id }"
                >
                  <input 
                    type="radio" 
                    :value="method.id" 
                    v-model="form.shippingMethod"
                  >
                  <div class="method-content">
                    <i :class="method.icon"></i>
                    <div>
                      <h3>{{ method.name }}</h3>
                      <p>{{ method.time }}</p>
                    </div>
                    <span class="method-price">{{ formatPrice(method.price) }}</span>
                  </div>
                </label>
              </div>
            </div>

            <!-- Phương thức thanh toán -->
            <div class="form-section">
              <h2>Phương thức thanh toán</h2>
              
              <div class="payment-methods">
                <label 
                  v-for="method in paymentMethods" 
                  :key="method.id"
                  class="payment-method"
                  :class="{ active: form.paymentMethod === method.id }"
                >
                  <input 
                    type="radio" 
                    :value="method.id" 
                    v-model="form.paymentMethod"
                  >
                  <div class="method-content">
                    <i :class="method.icon"></i>
                    <div>
                      <h3>{{ method.name }}</h3>
                      <p>{{ method.description }}</p>
                    </div>
                  </div>
                </label>
              </div>
            </div>

            <!-- Ghi chú đơn hàng -->
            <div class="form-section">
              <h2>Ghi chú đơn hàng</h2>
              <textarea 
                v-model="form.note" 
                placeholder="Ghi chú về đơn hàng, ví dụ: thời gian giao hàng..."
                rows="4"
              ></textarea>
            </div>

            <button type="submit" class="btn btn-primary place-order-btn">
              ĐẶT HÀNG
            </button>
          </form>

          <!-- Tóm tắt đơn hàng -->
          <div class="order-summary">
            <h2>Đơn hàng của bạn</h2>
            
            <div class="order-items">
              <div v-for="item in cartStore.items" :key="item.id" class="order-item">
                <div class="item-info">
                  <div class="item-image">
                    <i :class="['fas', `fa-${item.image}`]"></i>
                  </div>
                  <div class="item-details">
                    <h4>{{ item.name }}</h4>
                    <p v-if="item.size || item.color" class="item-variants">
                      {{ item.size }} {{ item.color }}
                    </p>
                    <p class="item-quantity">Số lượng: {{ item.quantity }}</p>
                  </div>
                </div>
                <div class="item-price">
                  {{ formatPrice(item.price * item.quantity) }}
                </div>
              </div>
            </div>

            <div class="summary-totals">
              <div class="summary-row">
                <span>Tạm tính:</span>
                <span>{{ formatPrice(cartStore.subtotal) }}</span>
              </div>
              <div class="summary-row">
                <span>Phí vận chuyển:</span>
                <span>{{ formatPrice(shippingCost) }}</span>
              </div>
              <div class="summary-row total">
                <span>Tổng cộng:</span>
                <span>{{ formatPrice(cartStore.subtotal + shippingCost) }}</span>
              </div>
            </div>

            <div class="payment-instructions" v-if="form.paymentMethod === 'bank'">
              <h3>Thông tin chuyển khoản</h3>
              <p>Ngân hàng: Vietcombank</p>
              <p>Số tài khoản: 1234567890</p>
              <p>Chủ tài khoản: FASHION STORE</p>
              <p>Nội dung: Mã đơn hàng + Số điện thoại</p>
            </div>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { useAuthStore } from '../../stores/authStore'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import { formatPrice } from '../../utils/helpers'

const router = useRouter()
const cartStore = useCartStore()
const authStore = useAuthStore()
const { showNotification } = useNotification()

// Form state
const form = ref({
  fullName: authStore.user?.name || '',
  phone: '',
  email: authStore.user?.email || '',
  city: '',
  district: '',
  address: '',
  shippingMethod: 'standard',
  paymentMethod: 'cod',
  note: ''
})

const cities = ['Hồ Chí Minh', 'Hà Nội', 'Đà Nẵng', 'Hải Phòng', 'Cần Thơ']
const districts = {
  'Hồ Chí Minh': ['Quận 1', 'Quận 2', 'Quận 3', 'Quận 4', 'Quận 5', 'Quận 6', 'Quận 7', 'Quận 8', 'Quận 9', 'Quận 10', 'Quận 11', 'Quận 12', 'Bình Thạnh', 'Gò Vấp', 'Phú Nhuận', 'Tân Bình', 'Tân Phú', 'Thủ Đức'],
  'Hà Nội': ['Ba Đình', 'Hoàn Kiếm', 'Hai Bà Trưng', 'Đống Đa', 'Tây Hồ', 'Cầu Giấy', 'Thanh Xuân', 'Hoàng Mai', 'Long Biên', 'Bắc Từ Liêm', 'Nam Từ Liêm'],
  'Đà Nẵng': ['Hải Châu', 'Thanh Khê', 'Sơn Trà', 'Ngũ Hành Sơn', 'Liên Chiểu', 'Cẩm Lệ'],
  'Hải Phòng': ['Hồng Bàng', 'Lê Chân', 'Ngô Quyền', 'Kiến An', 'Hải An', 'Đồ Sơn', 'Dương Kinh'],
  'Cần Thơ': ['Ninh Kiều', 'Bình Thủy', 'Cái Răng', 'Ô Môn', 'Thốt Nốt']
}

const shippingMethods = [
  { 
    id: 'standard', 
    name: 'Giao hàng tiêu chuẩn', 
    time: '3-5 ngày làm việc', 
    price: 30000,
    icon: 'fas fa-truck'
  },
  { 
    id: 'express', 
    name: 'Giao hàng nhanh', 
    time: '1-2 ngày làm việc', 
    price: 50000,
    icon: 'fas fa-rocket'
  },
  { 
    id: 'pickup', 
    name: 'Nhận tại cửa hàng', 
    time: 'Miễn phí', 
    price: 0,
    icon: 'fas fa-store'
  }
]

const paymentMethods = [
  { 
    id: 'cod', 
    name: 'Thanh toán khi nhận hàng (COD)', 
    description: 'Thanh toán bằng tiền mặt khi nhận hàng',
    icon: 'fas fa-money-bill-wave'
  },
  { 
    id: 'bank', 
    name: 'Chuyển khoản ngân hàng', 
    description: 'Chuyển khoản qua tài khoản ngân hàng',
    icon: 'fas fa-university'
  },
  { 
    id: 'momo', 
    name: 'Ví MoMo', 
    description: 'Thanh toán qua ví điện tử MoMo',
    icon: 'fas fa-mobile-alt'
  },
  { 
    id: 'vnpay', 
    name: 'VNPay', 
    description: 'Thanh toán qua cổng VNPay',
    icon: 'fas fa-credit-card'
  }
]

// Computed
const shippingCost = computed(() => {
  const method = shippingMethods.find(m => m.id === form.value.shippingMethod)
  return method ? method.price : 0
})

// Methods
const placeOrder = () => {
  // Validate form
  if (!form.value.fullName || !form.value.phone || !form.value.city || !form.value.district || !form.value.address) {
    showNotification('Vui lòng điền đầy đủ thông tin giao hàng', 'error')
    return
  }

  // Create order object
  const order = {
    id: 'DH' + Date.now(),
    customer: form.value.fullName,
    items: cartStore.items,
    subtotal: cartStore.subtotal,
    shipping: shippingCost.value,
    total: cartStore.subtotal + shippingCost.value,
    paymentMethod: form.value.paymentMethod,
    shippingMethod: form.value.shippingMethod,
    address: `${form.value.address}, ${form.value.district}, ${form.value.city}`,
    phone: form.value.phone,
    email: form.value.email,
    note: form.value.note,
    status: 'pending',
    date: new Date().toLocaleDateString('vi-VN')
  }

  // Simulate API call
  setTimeout(() => {
    // Clear cart
    cartStore.clearCart()
    
    // Show success message
    showNotification('Đặt hàng thành công! Cảm ơn bạn đã mua hàng.', 'success')
    
    // Redirect to success page or home
    router.push('/')
  }, 1500)
}
</script>

<style scoped>
.checkout-page {
  min-height: 100vh;
}

main {
  margin-top: 140px;
  padding: 20px 0 50px;
}

.page-title {
  font-size: 32px;
  color: var(--dark-gray);
  margin-bottom: 30px;
}

.empty-cart {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
}

.empty-cart i {
  font-size: 80px;
  color: var(--medium-gray);
  margin-bottom: 20px;
}

.empty-cart h2 {
  color: var(--dark-gray);
  margin-bottom: 10px;
}

.empty-cart p {
  color: var(--medium-gray);
  margin-bottom: 30px;
}

.checkout-content {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: 30px;
}

/* Form */
.checkout-form {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: var(--shadow);
}

.form-section {
  margin-bottom: 30px;
  padding-bottom: 30px;
  border-bottom: 1px solid var(--border-color);
}

.form-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.form-section h2 {
  color: var(--dark-gray);
  margin-bottom: 20px;
  font-size: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  margin-bottom: 8px;
  color: var(--dark-gray);
  font-weight: 500;
  font-size: 14px;
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  font-size: 14px;
  transition: all 0.3s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

/* Shipping Methods */
.shipping-methods,
.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.shipping-method,
.payment-method {
  display: flex;
  align-items: center;
  padding: 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.shipping-method:hover,
.payment-method:hover,
.shipping-method.active,
.payment-method.active {
  border-color: var(--primary-red);
  background: rgba(230, 57, 70, 0.05);
}

.shipping-method input[type="radio"],
.payment-method input[type="radio"] {
  margin-right: 15px;
  accent-color: var(--primary-red);
  width: 18px;
  height: 18px;
}

.method-content {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.method-content i {
  font-size: 24px;
  color: var(--primary-red);
  width: 40px;
}

.method-content h3 {
  color: var(--dark-gray);
  margin-bottom: 3px;
  font-size: 16px;
}

.method-content p {
  color: var(--medium-gray);
  font-size: 13px;
}

.method-price {
  font-weight: 600;
  color: var(--primary-red);
  margin-left: auto;
}

/* Order Summary */
.order-summary {
  background: white;
  border-radius: 15px;
  padding: 25px;
  box-shadow: var(--shadow);
  height: fit-content;
}

.order-summary h2 {
  color: var(--dark-gray);
  margin-bottom: 20px;
  font-size: 20px;
}

.order-items {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 20px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid var(--border-color);
}

.order-item:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  gap: 15px;
  align-items: center;
}

.item-image {
  width: 60px;
  height: 60px;
  background: var(--light-gray);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-image i {
  font-size: 30px;
  color: var(--medium-gray);
}

.item-details h4 {
  color: var(--dark-gray);
  margin-bottom: 3px;
  font-size: 14px;
}

.item-variants {
  color: var(--medium-gray);
  font-size: 12px;
  margin-bottom: 3px;
}

.item-quantity {
  color: var(--medium-gray);
  font-size: 12px;
}

.item-price {
  font-weight: 600;
  color: var(--primary-red);
}

.summary-totals {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 2px solid var(--border-color);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  color: var(--medium-gray);
}

.summary-row.total {
  font-size: 18px;
  font-weight: 700;
  color: var(--dark-gray);
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid var(--border-color);
}

.payment-instructions {
  margin-top: 20px;
  padding: 15px;
  background: var(--light-gray);
  border-radius: 8px;
}

.payment-instructions h3 {
  color: var(--dark-gray);
  margin-bottom: 10px;
  font-size: 16px;
}

.payment-instructions p {
  color: var(--medium-gray);
  margin-bottom: 5px;
  font-size: 14px;
}

.place-order-btn {
  width: 100%;
  padding: 15px;
  font-size: 16px;
}

/* Responsive */
@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .checkout-content {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .method-content {
    flex-wrap: wrap;
  }

  .method-price {
    margin-left: 0;
    width: 100%;
    text-align: right;
  }

  .page-title {
    font-size: 24px;
  }
}

@media (max-width: 480px) {
  .checkout-form,
  .order-summary {
    padding: 20px;
  }

  .shipping-method,
  .payment-method {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .shipping-method input[type="radio"],
  .payment-method input[type="radio"] {
    margin-bottom: 10px;
  }

  .method-content {
    width: 100%;
  }

  .order-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .item-price {
    align-self: flex-end;
  }
}
</style>