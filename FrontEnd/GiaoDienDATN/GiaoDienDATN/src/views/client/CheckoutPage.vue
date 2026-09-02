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
            
            <!-- ✅ PHẦN CHỌN ĐỊA CHỈ ĐÃ LƯU -->
            <div class="address-selection" v-if="addresses.length > 0">
              <h4>📍 Chọn địa chỉ giao hàng</h4>
              <div class="address-list">
                <div 
                  v-for="addr in addresses" 
                  :key="addr.addressId"
                  class="address-option"
                  :class="{ active: selectedAddressId === addr.addressId }"
                  @click="selectAddress(addr.addressId)"
                >
                  <div class="address-radio">
                    <input 
                      type="radio" 
                      :checked="selectedAddressId === addr.addressId"
                      @change="selectAddress(addr.addressId)"
                    />
                  </div>
                  <div class="address-content">
                    <div class="addr-name">{{ addr.fullName }}</div>
                    <div class="addr-phone">{{ addr.phone }}</div>
                    <div class="addr-detail">{{ getFullAddress(addr) }}</div>
                    <span v-if="addr.isDefault" class="default-badge">Mặc định</span>
                  </div>
                </div>
              </div>
              <button class="btn-add-address" @click="toggleEdit">
                <i class="fas fa-plus"></i> Thêm địa chỉ mới
              </button>
            </div>

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
                <span class="value">{{ profile.fullAddress || 'Chưa cập nhật' }}</span>
              </div>
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
                <label>Số nhà, tên đường <span class="required">*</span></label>
                <input type="text" v-model="editForm.streetAddress" required placeholder="VD: 123 Nguyễn Huệ" />
              </div>
              <div class="form-group">
                <label>Tỉnh/Thành phố <span class="required">*</span></label>
                <select v-model="editForm.provinceId" required @change="onProvinceChange">
                  <option :value="null">-- Chọn tỉnh/thành phố --</option>
                  <option 
                    v-for="province in provinces" 
                    :key="province.code" 
                    :value="province.code"
                  >
                    {{ province.name }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label>Quận/Huyện <span class="required">*</span></label>
                <select v-model="editForm.districtId" required :disabled="!editForm.provinceId" @change="onDistrictChange">
                  <option :value="null">-- Chọn quận/huyện --</option>
                  <option v-for="district in districts" :key="district.code" :value="district.code">
                    {{ district.name }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label>Phường/Xã <span class="required">*</span></label>
                <select v-model="editForm.wardId" required :disabled="!editForm.districtId">
                  <option :value="null">-- Chọn phường/xã --</option>
                  <option 
                    v-for="ward in wards" 
                    :key="ward.code" 
                    :value="ward.code"
                  >
                    {{ ward.name }}
                  </option>
                </select>
              </div>
              <div class="form-actions">
                <button type="button" class="btn-save-info" @click="saveProfileInfo" :disabled="saving">
                  <i v-if="saving" class="fas fa-spinner fa-spin"></i>
                  {{ saving ? 'Đang lưu...' : 'Lưu thông tin' }}
                </button>
                <button type="button" class="btn-cancel" @click="toggleEdit">Hủy</button>
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
                    {{ formatPrice(item.discountedPrice || item.price) }} x {{ item.quantity }}
                  </div>
                </div>
                <div class="item-total">
                  {{ formatPrice((item.discountedPrice || item.price) * item.quantity) }}
                </div>
              </div>
            </div>

            <!-- Summary với voucher -->
            <div class="summary-totals">
              <div class="summary-row">
                <span>Tạm tính:</span>
                <span>{{ formatPrice(cartStore.subtotal || 0) }}</span>
              </div>
              
              <div v-if="cartStore.voucherDiscount && cartStore.voucherDiscount > 0" class="summary-row discount">
                <span>Giảm giá ({{ cartStore.voucherCode || 'Voucher' }}):</span>
                <span>-{{ formatPrice(cartStore.voucherDiscount) }}</span>
              </div>
              
              <div class="summary-row">
                <span>Phí vận chuyển:</span>
                <span>
                  <i v-if="shippingLoading" class="fas fa-spinner fa-spin"></i>
                  <span v-else>{{ formatPrice(shippingFee) }}</span>
                </span>
              </div>
              <div v-if="shippingError" class="shipping-error">{{ shippingError }}</div>
              
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
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { useAuthStore } from '../../stores/authStore'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import { orderService } from '../../services/orderService'
import api from "@/api/axios";
import { useVietnamAddress } from '../../composables/useVietnamAddress'

const router = useRouter()
const cartStore = useCartStore()
const authStore = useAuthStore()

const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");
const calculateFixedShippingFee = () => 0

const placingOrder = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

// ===== PROFILE STATE =====
const profile = reactive({
  fullName: '',
  customerName: '',
  phone: '',
  address: '',
  fullAddress: '',
  city: '',
  district: '',
  ward: '',
  email: '',
  provinceId: null,
  districtId: null,
  wardId: null
})

const editForm = reactive({
  fullName: '',
  phone: '',
  streetAddress: '',
  provinceId: null,
  districtId: null,
  wardId: null,
  city: '',
  district: '',
  ward: ''
})

const orderData = ref({
  notes: '',
  paymentMethod: 'COD'
})

// ===== ADDRESS STATE =====
const addresses = ref([])
const selectedAddressId = ref(null)
const addressLoading = ref(false)
const shippingLoading = ref(false)
const shippingError = ref('')
const shippingFee = ref(0)

// ===== PROVINCE/WARD STATE =====
const {
  provinces, districts, wards,
  loadingProvinces: provinceLoading,
  fetchProvinces, fetchDistricts, fetchWards
} = useVietnamAddress()

// ===== FORMAT FUNCTIONS =====
const formatPrice = (price) => {
  if (!price && price !== 0) return '0₫'
  const numPrice = typeof price === 'string' ? parseFloat(price) : price
  if (isNaN(numPrice) || numPrice === 0) return '0₫'
  return numPrice.toLocaleString('vi-VN') + '₫'
}

const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

// ✅ Hàm lấy địa chỉ đầy đủ
const getFullAddress = (addr) => {
  if (!addr) return ''
  const parts = []
  if (addr.streetAddress) parts.push(addr.streetAddress)
  if (addr.wardName) parts.push(addr.wardName)
  if (addr.districtName) parts.push(addr.districtName)
  if (addr.provinceName) parts.push(addr.provinceName)
  return parts.join(', ') || 'Chưa có địa chỉ'
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

// ===== ON PROVINCE CHANGE =====
const onProvinceChange = async () => {
  editForm.districtId = null
  editForm.wardId = null
  editForm.ward = ''
  if (editForm.provinceId) {
    const province = provinces.value.find(p => p.code === editForm.provinceId)
    editForm.city = province?.name || ''
    await fetchDistricts(editForm.provinceId)
  } else {
    wards.value = []
    districts.value = []
    editForm.city = ''
  }
}

const onDistrictChange = async () => {
  editForm.wardId = null
  const district = districts.value.find(d => d.code === editForm.districtId)
  editForm.district = district?.name || ''
  await fetchWards(editForm.districtId)
}

// ===== LẤY DANH SÁCH ĐỊA CHỈ =====
const fetchAddresses = async () => {
  addressLoading.value = true
  try {
    // FIX: trước đây đọc localStorage.getItem('token') - SAI key, authStore
    // lưu token dưới key 'accessToken' -> luôn null -> return sớm, không
    // bao giờ load được địa chỉ đã lưu. Dùng chung trạng thái với authStore.
    if (!authStore.isAuthenticated) {
      addressLoading.value = false
      return
    }

    const response = await api.get(`/shipping-addresses`, {})
    
    addresses.value = response.data || []
    console.log('✅ Addresses loaded:', addresses.value.length)
    
    // Tìm địa chỉ mặc định
    const validAddresses = addresses.value.filter(a => a.provinceCode && a.districtCode && a.wardCode)
    const defaultAddr = validAddresses.find(a => a.isDefault)
    if (defaultAddr) {
      selectedAddressId.value = defaultAddr.addressId
      updateProfileFromAddress(defaultAddr)
    } else if (validAddresses.length > 0) {
      selectedAddressId.value = validAddresses[0].addressId
      updateProfileFromAddress(validAddresses[0])
    } else if (addresses.value.length > 0) {
      console.warn('[Address] Các địa chỉ hiện tại thiếu mã chuẩn Open API:', addresses.value)
      shippingError.value = 'Địa chỉ hiện tại cần được cập nhật lại theo tỉnh/quận/phường chuẩn.'
    }
  } catch (error) {
    console.error('❌ Error fetching addresses:', error)
  } finally {
    addressLoading.value = false
  }
}

// ===== CẬP NHẬT PROFILE TỪ ĐỊA CHỈ =====
const updateProfileFromAddress = (addr) => {
  if (!addr) return
  profile.fullName = addr.fullName || profile.fullName
  profile.phone = addr.phone || profile.phone
  profile.address = addr.streetAddress || profile.address
  profile.fullAddress = getFullAddress(addr)
  profile.provinceId = addr.provinceCode || addr.provinceId
  profile.districtId = addr.districtCode || null
  profile.wardId = addr.wardCode || addr.wardId
  profile.city = addr.provinceName || ''
  profile.district = addr.districtName || ''
  profile.ward = addr.wardName || ''
}

// ===== LẤY THÔNG TIN PROFILE =====
const fetchProfile = async () => {
  try {
    // FIX: cùng lỗi sai key 'token' như fetchAddresses ở trên
    if (!authStore.isAuthenticated) return

    const response = await api.get(`/customers/profile`, {})

    const data = response.data
    console.log('✅ Profile loaded:', data)
    
    profile.fullName = data.fullName || data.customerName || ''
    profile.customerName = data.customerName || ''
    profile.phone = data.phone || ''
    profile.address = data.address || ''
    profile.fullAddress = data.address || ''
    profile.city = data.city || ''
    profile.district = data.district || ''
    profile.ward = data.ward || ''
    profile.email = data.email || ''
    profile.provinceId = data.provinceId || null
    profile.districtId = data.districtCode || null
    profile.wardId = data.wardId || null

    editForm.fullName = profile.fullName
    editForm.phone = profile.phone
    editForm.streetAddress = profile.address
    editForm.provinceId = profile.provinceId
    editForm.districtId = profile.districtId
    editForm.wardId = profile.wardId
    editForm.city = profile.city
    editForm.district = profile.district
    editForm.ward = profile.ward
    
    if (editForm.provinceId) {
      await fetchDistricts(editForm.provinceId)
    }
    if (editForm.districtId) {
      await fetchWards(editForm.districtId)
    }
  } catch (error) {
    console.error('❌ Error fetching profile:', error)
  }
}

// ===== CHỌN ĐỊA CHỈ =====
const selectAddress = (addressId) => {
  selectedAddressId.value = addressId
  const addr = addresses.value.find(a => a.addressId === addressId)
  if (addr) {
    updateProfileFromAddress(addr)
    console.log('✅ Selected address:', addr)
    calculateShippingFee()
  }
}

const calculateShippingFee = async () => {
  // Không gọi GHTK nữa: hệ thống đang áp dụng phí vận chuyển cố định 0₫.
  shippingLoading.value = false
  shippingError.value = ''
  shippingFee.value = calculateFixedShippingFee(cartStore.subtotal)
}

watch(() => [profile.city, profile.district, profile.ward, cartStore.subtotal, cartStore.voucherDiscount], () => {
  calculateShippingFee()
})

// ===== TOGGLE EDIT =====
const toggleEdit = () => {
  if (isEditing.value) {
    editForm.fullName = profile.fullName
    editForm.phone = profile.phone
    editForm.streetAddress = profile.address
    editForm.provinceId = profile.provinceId
    editForm.districtId = profile.districtId
    editForm.wardId = profile.wardId
    editForm.city = profile.city
    editForm.district = profile.district
    editForm.ward = profile.ward
    if (editForm.provinceId) {
      fetchDistricts(editForm.provinceId)
    }
  }
  isEditing.value = !isEditing.value
}

// ===== SAVE PROFILE =====
const saveProfileInfo = async () => {
  if (!editForm.fullName || editForm.fullName.trim() === '') {
    showNotification('Vui lòng nhập họ và tên', 'error')
    return
  }
  if (!editForm.phone || editForm.phone.trim() === '') {
    showNotification('Vui lòng nhập số điện thoại', 'error')
    return
  }
  if (!editForm.streetAddress || editForm.streetAddress.trim() === '') {
    showNotification('Vui lòng nhập địa chỉ cụ thể', 'error')
    return
  }
  if (!editForm.provinceId) {
    showNotification('Vui lòng chọn tỉnh/thành phố', 'error')
    return
  }

  if (!editForm.districtId) {
    showNotification('Vui lòng chọn quận/huyện', 'error')
    return
  }

  if (!editForm.wardId) {
    showNotification('Vui lòng chọn phường/xã', 'error')
    return
  }

  saving.value = true
  try {
    // FIX: đây chính là nguyên nhân toast "Vui lòng đăng nhập để lưu thông
    // tin" hiện ra dù người dùng ĐÃ đăng nhập - do đọc sai key localStorage
    // ('token' thay vì 'accessToken'), luôn trả về null. Dùng authStore
    // (đã có sẵn, đồng bộ với toàn bộ app) thay vì tự đọc localStorage.
    if (!authStore.isAuthenticated) {
      showNotification('Vui lòng đăng nhập để lưu thông tin', 'error')
      return
    }

    const province = provinces.value.find(p => p.code === editForm.provinceId)
    const district = districts.value.find(d => d.code === editForm.districtId)
    const ward = wards.value.find(w => w.code === editForm.wardId)

    const payload = {
      fullName: editForm.fullName.trim(),
      phone: editForm.phone.trim(),
      address: editForm.streetAddress.trim(),
      provinceId: editForm.provinceId,
      wardId: editForm.wardId,
      provinceCode: province?.code || '',
      provinceName: province?.name || editForm.city,
      districtCode: district?.code || '',
      districtName: district?.name || editForm.district,
      wardCode: ward?.code || '',
      wardName: ward?.name || editForm.ward,
      city: province?.name || editForm.city,
      ward: ward?.name || editForm.ward
    }

    const response = await api.put(`/customers/profile`, payload)

    const data = response.data
    profile.fullName = data.fullName || data.customerName || editForm.fullName
    profile.customerName = data.customerName || ''
    profile.phone = data.phone || editForm.phone
    profile.address = data.address || editForm.streetAddress
    profile.fullAddress = data.address || editForm.streetAddress
    profile.provinceId = editForm.provinceId
    profile.wardId = editForm.wardId
    profile.city = data.city || editForm.city
    profile.ward = data.ward || editForm.ward

    isEditing.value = false
    showNotification('Cập nhật thông tin thành công!', 'success')
    await fetchAddresses()
  } catch (error) {
    console.error('❌ Error saving profile:', error)
    showNotification(error.response?.data?.message || 'Cập nhật thất bại!', 'error')
  } finally {
    saving.value = false
  }
}

// ===== ĐẶT HÀNG =====
const placeOrder = async () => {
  if (!selectedAddressId.value && !profile.address) {
    showNotification('Vui lòng chọn hoặc thêm địa chỉ giao hàng!', 'error')
    return
  }

  const voucherId = cartStore.voucherId || null

  const orderItems = cartStore.items.map(item => ({
    productDetailId: item.productDetailId || item.id,
    quantity: item.quantity
  }))

  let orderPayload = {
    shippingFullName: profile.fullName || 'Khách hàng',
    shippingPhone: profile.phone || '',
    shippingAddress: profile.address || '',
    shippingCity: profile.city || '',
    shippingDistrict: profile.district || '',
    shippingWard: profile.ward || '',
    notes: orderData.value.notes || '',
    items: orderItems,
    paymentMethod: orderData.value.paymentMethod || 'COD',
    voucherId: voucherId,
    shippingFee: shippingFee.value
  }

  // ✅ Nếu có shippingAddressId, gửi lên
  if (selectedAddressId.value) {
    orderPayload.shippingAddressId = selectedAddressId.value
    const selectedAddr = addresses.value.find(a => a.addressId === selectedAddressId.value)
    if (selectedAddr) {
      orderPayload.provinceId = selectedAddr.provinceId
      orderPayload.wardId = selectedAddr.wardId
      orderPayload.shippingCity = selectedAddr.provinceName || ''
      orderPayload.shippingDistrict = selectedAddr.districtName || ''
      orderPayload.shippingWard = selectedAddr.wardName || ''
    }
  } else {
    if (editForm.provinceId) {
      orderPayload.provinceId = editForm.provinceId
    }
    if (editForm.wardId) {
      orderPayload.wardId = editForm.wardId
    }
  }

  console.log('📦 Order payload:', orderPayload)

  if (!(await window.appConfirm('Bạn có chắc chắn muốn thanh toán đơn hàng này không?'))) return

  placingOrder.value = true

  try {
    // FIX: trước đây tự đọc localStorage.getItem('token') (SAI key, luôn
    // null) rồi tự set header Authorization: 'Bearer null' - header này ĐÈ
    // LÊN header đúng mà interceptor của `api` (axios instance dùng chung,
    // xem @/api/axios) đã tự động gắn sẵn từ key 'accessToken'. Kết quả:
    // request tạo đơn hàng bị gửi đi với token rỗng, có thể khiến backend
    // trả 401/xử lý sai người đặt hàng dù UI vẫn tưởng đã đăng nhập.
    // Bỏ hẳn phần tự set header - để interceptor lo, giống mọi request khác.
    const response = await api.post(`/orders/create`, orderPayload)

    console.log('✅ Order created:', response.data)

    if (orderData.value.paymentMethod === 'VNPAY' && response.data && response.data.orderId) {
      showNotification('Đang chuyển hướng sang cổng thanh toán VNPay...', 'success')
      const paymentRes = await api.post(
        `/payment/vnpay/create/${response.data.orderId}`,
        {},
        {}
      )
      
      if (paymentRes.data && paymentRes.data.paymentUrl) {
        window.location.href = paymentRes.data.paymentUrl
        return
      }
    }
    
    await cartStore.clearCart()
    showNotification('Đặt hàng thành công!', 'success')
    
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

// ===== COMPUTED =====
const totalAmount = computed(() => {
  const subtotal = cartStore.subtotal || 0
  const discount = cartStore.voucherDiscount || 0
  const shipping = shippingFee.value
  return subtotal - discount + shipping
})

// ===== LIFECYCLE =====
onMounted(async () => {
  if (!authStore.isAuthenticated) {
    showNotification('Vui lòng đăng nhập để thanh toán!', 'error')
    router.push('/login')
    return
  }
  
  await fetchProvinces()
  await fetchProfile()
  await fetchAddresses()
  await cartStore.fetchCart()
  await calculateShippingFee()
  
  if (!profile.address && addresses.value.length === 0) {
    isEditing.value = true
    showNotification('Vui lòng cập nhật thông tin giao hàng!', 'warning')
  }
})
</script>

<style scoped>
.checkout-page {
  background: #f8f9fa;
  min-height: 100vh;
}

.shipping-error {
  margin-top: 8px;
  color: #b42318;
  font-size: 13px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-title {
  text-align: center;
  font-size: 32px;
  font-weight: 700;
  color: #2c3e50;
  padding: 30px 0 20px;
  letter-spacing: 2px;
}

/* ===== EMPTY CART ===== */
.empty-cart {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}

.empty-cart i {
  font-size: 80px;
  color: #e0e0e0;
  margin-bottom: 20px;
}

.empty-cart h2 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.empty-cart p {
  color: #888;
  margin-bottom: 30px;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 30px;
  background: #e63946;
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-primary:hover {
  background: #c62828;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(230, 57, 70, 0.3);
}

/* ===== CHECKOUT CONTENT ===== */
.checkout-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 30px;
  padding: 20px 0 60px;
}

/* ===== CHECKOUT FORM ===== */
.checkout-form {
  background: white;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}

.checkout-form h2 {
  font-size: 22px;
  color: #2c3e50;
  margin-bottom: 24px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f1f4f8;
}

/* ===== ADDRESS SELECTION ===== */
.address-selection {
  margin-bottom: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
}

.address-selection h4 {
  color: #2c3e50;
  font-size: 15px;
  margin-bottom: 12px;
  font-weight: 600;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.address-option {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  background: white;
  border-radius: 10px;
  border: 2px solid #e8ecf0;
  cursor: pointer;
  transition: all 0.3s ease;
}

.address-option:hover {
  border-color: #d0d5dc;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.address-option.active {
  border-color: #e63946;
  background: #fff5f5;
  box-shadow: 0 2px 12px rgba(230, 57, 70, 0.1);
}

.address-radio {
  padding-top: 2px;
  flex-shrink: 0;
}

.address-radio input[type="radio"] {
  accent-color: #e63946;
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.address-content {
  flex: 1;
  min-width: 0;
}

.address-content .addr-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 15px;
}

.address-content .addr-phone {
  color: #666;
  font-size: 13px;
  margin-top: 2px;
}

.address-content .addr-detail {
  color: #888;
  font-size: 13px;
  margin-top: 2px;
  word-break: break-word;
}

.default-badge {
  display: inline-block;
  background: #e63946;
  color: white;
  padding: 2px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  margin-top: 4px;
}

.btn-add-address {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  color: #e63946;
  border: 1.5px dashed #e63946;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-add-address:hover {
  background: #fff5f5;
  border-style: solid;
}

/* ===== PROFILE INFO ===== */
.profile-info {
  background: #f8f9fa;
  padding: 16px 20px;
  border-radius: 10px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 6px 0;
  gap: 10px;
  flex-wrap: wrap;
}

.info-row .label {
  font-weight: 600;
  color: #666;
  font-size: 14px;
  min-width: 100px;
}

.info-row .value {
  color: #2c3e50;
  font-size: 14px;
  flex: 1;
}

.edit-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 16px;
  transition: color 0.3s;
  padding: 0 4px;
}

.edit-btn:hover {
  color: #e63946;
}

.info-row.voucher-info {
  background: #fff8e1;
  padding: 8px 12px;
  border-radius: 8px;
  margin-top: 4px;
}

.voucher-code {
  font-weight: 700;
  color: #e63946;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.voucher-discount-amount {
  color: #e63946;
  font-weight: 700;
  font-size: 15px;
}

/* ===== EDIT FORM ===== */
.edit-form {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
  margin: 16px 0;
}

.edit-form h3 {
  color: #2c3e50;
  font-size: 17px;
  margin-bottom: 16px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #555;
  font-size: 14px;
  margin-bottom: 4px;
}

.form-group .required {
  color: #e63946;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s;
  background: white;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.form-group select:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.btn-save-info {
  padding: 12px 28px;
  background: #e63946;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-save-info:hover:not(:disabled) {
  background: #c62828;
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(230, 57, 70, 0.3);
}

.btn-save-info:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel {
  padding: 12px 28px;
  background: transparent;
  color: #666;
  border: 1.5px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-cancel:hover {
  background: #f5f5f5;
  border-color: #ccc;
}

/* ===== PAYMENT METHODS ===== */
.payment-methods {
  margin-top: 8px;
}

.payment-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
}

.payment-option {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  border: 1.5px solid #e0e0e0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  background: white;
}

.payment-option:hover {
  border-color: #ccc;
}

.payment-option:has(input:checked) {
  border-color: #e63946;
  background: #fff5f5;
}

.payment-option input[type="radio"] {
  accent-color: #e63946;
  width: 18px;
  height: 18px;
  margin-right: 12px;
  flex-shrink: 0;
}

.payment-option .option-content {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #333;
}

.payment-option .option-content i {
  font-size: 20px;
  color: #666;
  width: 24px;
}

/* ===== PLACE ORDER BUTTON ===== */
.btn-place-order {
  width: 100%;
  padding: 16px;
  background: #e63946;
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 8px;
}

.btn-place-order:hover:not(:disabled) {
  background: #c62828;
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(230, 57, 70, 0.35);
}

.btn-place-order:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* ===== ORDER SUMMARY ===== */
.order-summary {
  background: white;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  height: fit-content;
  position: sticky;
  top: 20px;
}

.order-summary h2 {
  font-size: 22px;
  color: #2c3e50;
  margin-bottom: 24px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f1f4f8;
}

.summary-items {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 6px;
}

.summary-items::-webkit-scrollbar {
  width: 4px;
}

.summary-items::-webkit-scrollbar-track {
  background: #f1f4f8;
  border-radius: 4px;
}

.summary-items::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 4px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f1f4f8;
}

.summary-item:last-child {
  border-bottom: none;
}

.item-image {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: #f1f4f8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-image i {
  font-size: 28px;
  color: #ccc;
}

.item-details {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-weight: 500;
  color: #2c3e50;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-variant {
  font-size: 12px;
  color: #888;
  margin-top: 2px;
}

.item-price {
  font-size: 13px;
  color: #666;
}

.item-total {
  font-weight: 600;
  color: #e63946;
  font-size: 14px;
  white-space: nowrap;
  flex-shrink: 0;
}

/* ===== SUMMARY TOTALS ===== */
.summary-totals {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 2px solid #f1f4f8;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 15px;
  color: #555;
}

.summary-row.discount {
  color: #e63946;
  font-weight: 500;
}

.summary-row.total {
  font-size: 18px;
  font-weight: 700;
  padding-top: 12px;
}

.summary-row.total .total-amount {
  color: #e63946;
  font-size: 22px;
}

.summary-divider {
  height: 1px;
  background: #f1f4f8;
  margin: 8px 0;
}

/* ===== TOAST ===== */
.toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  background: #28a745;
  color: white;
  padding: 16px 28px;
  border-radius: 10px;
  box-shadow: 0 6px 30px rgba(0,0,0,0.15);
  transform: translateY(100px);
  opacity: 0;
  transition: all 0.4s ease;
  z-index: 9999;
  max-width: 400px;
  font-size: 14px;
}

.toast.show {
  transform: translateY(0);
  opacity: 1;
}

.toast-error {
  background: #dc3545;
}

/* ===== RESPONSIVE ===== */
@media (max-width: 1024px) {
  .checkout-content {
    grid-template-columns: 1fr 350px;
    gap: 24px;
  }
}

@media (max-width: 768px) {
  .checkout-content {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .checkout-form {
    padding: 20px;
  }
  
  .order-summary {
    padding: 20px;
    position: static;
  }
  
  .page-title {
    font-size: 24px;
    padding: 20px 0 12px;
  }
  
  .info-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }
  
  .info-row .label {
    min-width: auto;
  }
  
  .address-option {
    padding: 12px;
    gap: 10px;
  }
  
  .payment-option {
    padding: 12px 14px;
  }
  
  .btn-place-order {
    font-size: 16px;
    padding: 14px;
  }
  
  .summary-row.total .total-amount {
    font-size: 18px;
  }
  
  .toast {
    bottom: 16px;
    right: 16px;
    left: 16px;
    max-width: none;
    padding: 14px 20px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .checkout-form {
    padding: 16px;
  }
  
  .address-selection {
    padding: 14px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .btn-save-info,
  .btn-cancel {
    width: 100%;
    justify-content: center;
  }
}
</style>
