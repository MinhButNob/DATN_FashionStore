<template>
  <div class="checkout-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <h1 class="page-title">THANH TOÁN</h1>

        <div v-if="cartStore.items.length === 0" class="empty-cart">
          <i class="fas fa-shopping-cart"></i>
          <h2>Giỏ hàng trống</h2>
          <p>Vui lòng thêm sản phẩm vào giỏ hàng trước khi thanh toán</p>
          <router-link to="/shop" class="btn-primary">
            <i class="fas fa-arrow-left"></i> Tiếp tục mua sắm
          </router-link>
        </div>

        <div v-else class="checkout-content">
          <!-- Form thông tin giao hàng -->
          <div class="checkout-form">
            <h2>Thông tin giao hàng</h2>
            
            <!-- Hiển thị thông tin từ profile -->
            <div class="profile-info">
              <div class="info-row">
                <span class="label">Họ tên:</span>
                <span class="value">{{ profile.fullName || profile.customerName || 'Chưa cập nhật' }}</span>
                <button class="edit-btn" @click="toggleEdit">✏️</button>
              </div>
              <div class="info-row">
                <span class="label">Số điện thoại:</span>
                <span class="value">{{ profile.phone || 'Chưa cập nhật' }}</span>
              </div>
              <div class="info-row">
                <span class="label">Địa chỉ:</span>
                <span class="value">{{ profile.address || 'Chưa cập nhật' }}</span>
              </div>
              <!-- ✅ Hiển thị voucher đang áp dụng -->
              <div v-if="cartStore.voucherCode" class="info-row voucher-info">
                <span class="label">Mã giảm giá:</span>
                <span class="value voucher-code">{{ cartStore.voucherCode }}</span>
                <span class="voucher-discount-amount">-{{ formatPrice(cartStore.voucherDiscount || 0) }}</span>
              </div>
            </div>

            <!-- Form chỉnh sửa thông tin -->
            <div v-if="isEditing" class="edit-form">
              <h3>Chỉnh sửa thông tin giao hàng</h3>
              <div class="form-group">
                <label>Họ và tên <span class="required">*</span></label>
                <input type="text" v-model="editForm.fullName" required placeholder="Nhập họ và tên" />
              </div>
              <div class="form-group">
                <label>Số điện thoại <span class="required">*</span></label>
                <input type="tel" v-model="editForm.phone" required placeholder="Nhập số điện thoại" />
              </div>
              <div class="form-group">
                <label>Địa chỉ <span class="required">*</span></label>
                <input type="text" v-model="editForm.address" required placeholder="Số nhà, tên đường" />
              </div>
              <div class="form-group">
                <label>Thành phố/Tỉnh</label>
                <input type="text" v-model="editForm.city" placeholder="Thành phố" />
              </div>
              <div class="form-group">
                <label>Quận/Huyện</label>
                <input type="text" v-model="editForm.district" placeholder="Quận/Huyện" />
              </div>
              <div class="form-actions">
                <button type="button" class="btn-save-info" @click="saveProfileInfo" :disabled="saving">
                  <i v-if="saving" class="fas fa-spinner fa-spin"></i>
                  {{ saving ? 'Đang lưu...' : 'Lưu thông tin' }}
                </button>
                <button type="button" class="btn-cancel" @click="toggleEdit">
                  Hủy
                </button>
              </div>
            </div>

            <!-- Form đặt hàng -->
            <form @submit.prevent="placeOrder">
              <!-- Phương thức thanh toán -->
              <div class="form-group payment-methods">
                <label>Phương thức thanh toán <span class="required">*</span></label>
                <div class="payment-options">
                  <label class="payment-option">
                    <input type="radio" v-model="orderData.paymentMethod" value="COD" />
                    <span class="option-content">
                      <i class="fas fa-money-bill-wave"></i> Thanh toán khi nhận hàng (COD)
                    </span>
                  </label>
                  <label class="payment-option">
                    <input type="radio" v-model="orderData.paymentMethod" value="VNPAY" />
                    <span class="option-content">
                      <i class="fas fa-credit-card"></i> Thanh toán qua VNPay
                    </span>
                  </label>
                </div>
              </div>

              <div class="form-group">
                <label>Ghi chú cho đơn hàng</label>
                <textarea v-model="orderData.notes" rows="3" placeholder="Ghi chú (không bắt buộc)..."></textarea>
              </div>

              <button type="submit" class="btn-place-order" :disabled="placingOrder">
                <i class="fas fa-credit-card"></i>
                {{ placingOrder ? 'Đang xử lý...' : 'ĐẶT HÀNG' }}
              </button>
            </form>
          </div>

          <!-- Order Summary -->
          <div class="order-summary">
            <h2>Đơn hàng của bạn</h2>
            
            <div class="summary-items">
              <div v-for="item in cartStore.items" :key="item.id" class="summary-item">
                <div class="item-image">
                  <img v-if="item.image && item.image !== 'tshirt'" :src="getImageUrl(item.image)" :alt="item.name" />
                  <i v-else class="fas fa-tshirt"></i>
                </div>
                <div class="item-details">
                  <div class="item-name">{{ item.name }}</div>
                  <div class="item-variant" v-if="item.size || item.color">
                    {{ item.size || '' }} {{ item.color || '' }}
                  </div>
                  <div class="item-price">
                    {{ formatPrice(item.price) }} x {{ item.quantity }}
                  </div>
                </div>
                <div class="item-total">
                  {{ formatPrice(item.price * item.quantity) }}
                </div>
              </div>
            </div>

            <!-- ✅ Summary với voucher -->
            <div class="summary-totals">
              <div class="summary-row">
                <span>Tạm tính:</span>
                <span>{{ formatPrice(cartStore.subtotal || 0) }}</span>
              </div>
              
              <!-- ✅ Hiển thị giảm giá từ voucher -->
              <div v-if="cartStore.voucherDiscount && cartStore.voucherDiscount > 0" class="summary-row discount">
                <span>Giảm giá ({{ cartStore.voucherCode || 'Voucher' }}):</span>
                <span>-{{ formatPrice(cartStore.voucherDiscount) }}</span>
              </div>
              
              <div class="summary-row">
                <span>Phí vận chuyển:</span>
                <span>{{ formatPrice(shippingFee) }}</span>
              </div>
              
              <div class="summary-divider"></div>
              
              <div class="summary-row total">
                <span>Tổng cộng:</span>
                <span class="total-amount">{{ formatPrice(totalAmount) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />

    <!-- Toast Notification -->
    <div :class="['toast', { show: showToast, 'toast-error': toastType === 'error' }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { useAuthStore } from '../../stores/authStore'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import { orderService } from '../../services/orderService'
import axios from 'axios'

const router = useRouter()
const cartStore = useCartStore()
const authStore = useAuthStore()

const API_BASE = "http://localhost:8080/api"
const IMAGE_BASE = "http://localhost:8080"
const SHIPPING_FEE = 30000

const placingOrder = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

const profile = reactive({
  fullName: '',
  customerName: '',
  phone: '',
  address: '',
  city: '',
  district: '',
  ward: '',
  email: ''
})

const editForm = reactive({
  fullName: '',
  phone: '',
  address: '',
  city: '',
  district: '',
  ward: ''
})

const orderData = ref({
  notes: '',
  paymentMethod: 'COD'
})

// ✅ HÀM LẤY URL ẢNH ĐẦY ĐỦ
const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

// ✅ Computed: Tính tổng tiền với voucher
const shippingFee = computed(() => {
  const subtotal = cartStore.subtotal || 0
  return subtotal >= 500000 ? 0 : SHIPPING_FEE
})

const totalAmount = computed(() => {
  const subtotal = cartStore.subtotal || 0
  const discount = cartStore.voucherDiscount || 0
  const shipping = shippingFee.value
  return subtotal - discount + shipping
})

const formatPrice = (price) => {
  if (!price && price !== 0) return '0₫'
  const numPrice = typeof price === 'string' ? parseFloat(price) : price
  if (isNaN(numPrice) || numPrice === 0) return '0₫'
  return numPrice.toLocaleString('vi-VN') + '₫'
}

const showNotification = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 3000)
}

// Lấy thông tin profile
const fetchProfile = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return

    const response = await axios.get(`${API_BASE}/customers/profile`, {
      headers: { Authorization: `Bearer ${token}` }
    })

    const data = response.data
    console.log('✅ Profile loaded:', data)
    
    profile.fullName = data.fullName || data.customerName || ''
    profile.customerName = data.customerName || ''
    profile.phone = data.phone || ''
    profile.address = data.address || ''
    profile.city = data.city || ''
    profile.district = data.district || ''
    profile.ward = data.ward || ''
    profile.email = data.email || ''

    editForm.fullName = profile.fullName
    editForm.phone = profile.phone
    editForm.address = profile.address
    editForm.city = profile.city
    editForm.district = profile.district
    editForm.ward = profile.ward

  } catch (error) {
    console.error('Error fetching profile:', error)
  }
}

const toggleEdit = () => {
  if (isEditing.value) {
    editForm.fullName = profile.fullName
    editForm.phone = profile.phone
    editForm.address = profile.address
    editForm.city = profile.city
    editForm.district = profile.district
    editForm.ward = profile.ward
  }
  isEditing.value = !isEditing.value
}

const saveProfileInfo = async () => {
  if (!editForm.fullName || editForm.fullName.trim() === '') {
    showNotification('Vui lòng nhập họ và tên', 'error')
    return
  }
  if (!editForm.phone || editForm.phone.trim() === '') {
    showNotification('Vui lòng nhập số điện thoại', 'error')
    return
  }
  if (!editForm.address || editForm.address.trim() === '') {
    showNotification('Vui lòng nhập địa chỉ', 'error')
    return
  }

  saving.value = true

  try {
    const token = localStorage.getItem('token')
    if (!token) {
      showNotification('Vui lòng đăng nhập để lưu thông tin', 'error')
      return
    }

    const payload = {
      fullName: editForm.fullName.trim(),
      phone: editForm.phone.trim(),
      address: editForm.address.trim(),
      city: editForm.city?.trim() || '',
      district: editForm.district?.trim() || '',
      ward: editForm.ward?.trim() || ''
    }

    const response = await axios.put(`${API_BASE}/customers/profile`, payload, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })

    const data = response.data
    profile.fullName = data.fullName || data.customerName || editForm.fullName
    profile.customerName = data.customerName || ''
    profile.phone = data.phone || editForm.phone
    profile.address = data.address || editForm.address
    profile.city = data.city || editForm.city
    profile.district = data.district || editForm.district
    profile.ward = data.ward || editForm.ward

    if (authStore.user) {
      authStore.user.username = profile.fullName
      localStorage.setItem('user', JSON.stringify(authStore.user))
    }

    isEditing.value = false
    showNotification('Cập nhật thông tin thành công!', 'success')

  } catch (error) {
    console.error('Error saving profile:', error)
    showNotification(error.response?.data?.message || 'Cập nhật thất bại!', 'error')
  } finally {
    saving.value = false
  }
}

// ✅ Đặt hàng - chuyển về Profile Page
const placeOrder = async () => {
  if (!profile.address || profile.address.trim() === '') {
    showNotification('Vui lòng cập nhật địa chỉ trước khi đặt hàng!', 'error')
    return
  }

  const voucherId = cartStore.voucherId || null
  
  console.log('📋 Place order with voucherId:', voucherId)
  console.log('📋 Voucher code:', cartStore.voucherCode)
  console.log('📋 Voucher discount:', cartStore.voucherDiscount)

  const orderItems = cartStore.items.map(item => ({
    productDetailId: item.productDetailId || item.id,
    quantity: item.quantity
  }))

  const orderPayload = {
    shippingFullName: profile.fullName || 'Khách hàng',
    shippingPhone: profile.phone || '',
    shippingAddress: profile.address || '',
    shippingCity: profile.city || 'Hà Nội',
    shippingDistrict: profile.district || '',
    shippingWard: profile.ward || '',
    notes: orderData.value.notes || '',
    items: orderItems,
    paymentMethod: orderData.value.paymentMethod || 'COD',
    voucherId: voucherId
  }

  console.log('📦 Order payload:', orderPayload)

  placingOrder.value = true

  try {
    const response = await orderService.createOrder(orderPayload)
    console.log('✅ Order created:', response.data)

    // ✅ Clear cart (cả voucher)
    await cartStore.clearCart()
    
    // Xử lý nếu chọn VNPay
    if (orderData.value.paymentMethod === 'VNPAY' && response.data && response.data.orderId) {
      showNotification('Đang chuyển hướng sang cổng thanh toán VNPay...', 'success')
      const token = localStorage.getItem('token')
      const paymentRes = await axios.post(`${API_BASE}/payment/vnpay/create/${response.data.orderId}`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      })
      
      if (paymentRes.data && paymentRes.data.paymentUrl) {
        window.location.href = paymentRes.data.paymentUrl
        return
      }
    }
    
    showNotification('Đặt hàng thành công!', 'success')
    
    // ✅ CHUYỂN HƯỚNG VỀ PROFILE PAGE VỚI TAB ORDERS
    setTimeout(() => {
      router.push({ 
        path: '/profile', 
        query: { tab: 'orders' } 
      })
    }, 1500)

  } catch (error) {
    console.error('❌ Order error:', error)
    showNotification(error.response?.data?.message || 'Đặt hàng thất bại!', 'error')
  } finally {
    placingOrder.value = false
  }
}

onMounted(async () => {
  if (!authStore.isAuthenticated) {
    showNotification('Vui lòng đăng nhập để thanh toán!', 'error')
    router.push('/login')
    return
  }
  
  await fetchProfile()
  await cartStore.fetchCart()
  
  if (!profile.address) {
    isEditing.value = true
    showNotification('Vui lòng cập nhật thông tin giao hàng!', 'warning')
  }
})
</script>

<style scoped>
.checkout-page {
  min-height: 100vh;
  background: #f8f9fa;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

main {
  margin-top: 140px;
  padding: 20px 0 50px;
}

.page-title {
  font-size: 32px;
  color: #2c3e50;
  margin-bottom: 30px;
}

.empty-cart {
  text-align: center;
  padding: 80px;
  background: white;
  border-radius: 15px;
}

.empty-cart i {
  font-size: 60px;
  color: #ccc;
  margin-bottom: 20px;
}

.checkout-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 30px;
  align-items: start;
}

.checkout-form {
  background: white;
  padding: 30px;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.checkout-form h2,
.order-summary h2 {
  font-size: 20px;
  color: #2c3e50;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e63946;
}

.profile-info {
  background: #f8f9fa;
  padding: 15px 20px;
  border-radius: 10px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 5px 0;
  font-size: 15px;
  gap: 10px;
}

.info-row .label {
  font-weight: 600;
  color: #2c3e50;
  width: 120px;
  flex-shrink: 0;
}

.info-row .value {
  color: #666;
  flex: 1;
}

/* ✅ Style cho voucher */
.info-row.voucher-info {
  background: #e8f5e9;
  padding: 8px 12px;
  border-radius: 8px;
  margin-top: 5px;
}

.info-row.voucher-info .voucher-code {
  font-weight: 700;
  color: #2e7d32;
  background: white;
  padding: 2px 10px;
  border-radius: 4px;
}

.info-row.voucher-info .voucher-discount-amount {
  color: #e63946;
  font-weight: 700;
}

.edit-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s;
}

.edit-btn:hover {
  background: #e9ecef;
}

.edit-form {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 10px;
  margin-bottom: 20px;
  border: 2px solid #e63946;
}

.edit-form h3 {
  color: #2c3e50;
  margin-bottom: 15px;
  font-size: 16px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 5px;
  font-size: 14px;
}

.required {
  color: #e63946;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #e63946;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.btn-save-info {
  padding: 10px 24px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-save-info:hover:not(:disabled) {
  background: #218838;
}

.btn-save-info:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel {
  padding: 10px 24px;
  background: #f8f9fa;
  color: #666;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-cancel:hover {
  background: #e9ecef;
}

.btn-place-order {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 15px;
}

.btn-place-order:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(230, 57, 70, 0.3);
}

.btn-place-order:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Payment Methods */
.payment-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.payment-option {
  cursor: pointer;
}

.payment-option input {
  display: none;
}

.option-content {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  transition: all 0.3s;
  color: #666;
}

.payment-option input:checked + .option-content {
  border-color: #e63946;
  background: #fff0f0;
  color: #e63946;
  font-weight: 600;
}

/* Order Summary */
.order-summary {
  background: white;
  padding: 30px;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 160px;
}

.summary-items {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 20px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.summary-item:last-child {
  border-bottom: none;
}

.item-image {
  width: 50px;
  height: 50px;
  background: #f5f5f5;
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
  font-size: 24px;
  color: #ccc;
}

.item-details {
  flex: 1;
}

.item-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.item-variant {
  font-size: 12px;
  color: #999;
}

.item-price {
  font-size: 13px;
  color: #666;
}

.item-total {
  font-weight: 700;
  color: #e63946;
}

/* ✅ Summary totals với voucher */
.summary-totals {
  border-top: 2px solid #eee;
  padding-top: 15px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  color: #666;
}

.summary-row.discount {
  color: #28a745;
  font-weight: 600;
}

.summary-divider {
  height: 1px;
  background: #e0e0e0;
  margin: 8px 0;
}

.summary-row.total {
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
  padding-top: 10px;
  border-top: 1px solid #eee;
  margin-top: 5px;
}

.total-amount {
  color: #e63946;
}

.toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  padding: 12px 24px;
  border-radius: 8px;
  background: #27ae60;
  color: white;
  font-size: 14px;
  z-index: 9999;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  padding: 12px 24px;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.3);
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .checkout-content {
    grid-template-columns: 1fr;
  }

  .order-summary {
    position: static;
  }

  .page-title {
    font-size: 24px;
  }

  .info-row {
    flex-wrap: wrap;
  }

  .info-row .label {
    width: 100%;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions button {
    width: 100%;
  }
}
</style>