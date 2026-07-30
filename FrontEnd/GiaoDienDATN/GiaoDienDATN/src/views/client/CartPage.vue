<template>
  <div class="cart-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <div class="page-header">
          <h1 class="page-title">GIỎ HÀNG CỦA BẠN</h1>
          <p class="page-subtitle" v-if="cartStore.items.length > 0">
            Bạn có <strong>{{ cartStore.totalItems }}</strong> sản phẩm trong giỏ hàng
          </p>
        </div>

        <div v-if="cartStore.loading" class="loading-state">
          <div class="spinner"></div>
          <p>Đang tải giỏ hàng...</p>
        </div>

        <div v-else-if="cartStore.items.length === 0" class="empty-cart">
          <div class="empty-icon">
            <i class="fas fa-shopping-cart"></i>
          </div>
          <h2>Giỏ hàng của bạn đang trống</h2>
          <p>Khám phá những sản phẩm mới nhất và thêm vào giỏ hàng ngay!</p>
          <div class="empty-actions">
            <router-link to="/shop" class="btn-primary">
              <i class="fas fa-arrow-left"></i> Tiếp tục mua sắm
            </router-link>
            <router-link to="/shop?tag=sale" class="btn-secondary">
              <i class="fas fa-tags"></i> Xem khuyến mãi
            </router-link>
          </div>
        </div>

        <div v-else class="cart-content">
          <div class="cart-items-wrapper">
            <div class="cart-items-header">
              <div class="col-product">Sản phẩm</div>
              <div class="col-price">Đơn giá</div>
              <div class="col-quantity">Số lượng</div>
              <div class="col-total">Thành tiền</div>
              <div class="col-action">Thao tác</div>
            </div>

            <div 
              v-for="item in cartStore.items" 
              :key="item.cartItemId || item.id" 
              class="cart-item"
            >
              <div class="col-product">
                <div class="product-info">
                  <div class="product-image" @click="goToProduct(item.productId || item.id)">
                    <img 
                      v-if="item.image && item.image !== 'tshirt' && !item.image.includes('placeholder')" 
                      :src="getImageUrl(item.image)" 
                      :alt="item.name"
                      @error="handleImageError"
                    />
                    <i v-else class="fas fa-tshirt"></i>
                  </div>
                  <div class="product-details">
                    <h3 @click="goToProduct(item.productId || item.id)">{{ item.name }}</h3>
                    <div class="product-variants">
                      <span v-if="item.color" class="variant-badge">
                        <span class="color-dot" :style="{ backgroundColor: getColorHex(item.color) }"></span>
                        Màu: {{ item.color }}
                      </span>
                      <span v-if="item.size" class="variant-badge">
                        <i class="fas fa-ruler"></i> Size: {{ item.size }}
                      </span>
                      <span v-if="!item.color && !item.size" class="variant-badge">
                        <i class="fas fa-box"></i> Sản phẩm cơ bản
                      </span>
                    </div>
                    <div class="product-stock" :class="{ 'low-stock': item.stock <= 5 }">
                      <i class="fas fa-boxes"></i>
                      Còn lại: <span :class="{ 'text-danger': item.stock <= 5 }">{{ item.stock || 0 }}</span> sản phẩm
                    </div>
                  </div>
                </div>
              </div>

              <div class="col-price">
                <div class="price-wrapper">
                  <template v-if="item.discountedPrice && item.discountedPrice < item.price">
                    <span class="original-price">{{ formatPrice(item.price) }}</span>
                    <span class="current-price">{{ formatPrice(item.discountedPrice) }}</span>
                    <span class="discount-badge">-{{ calculateDiscount(item.price, item.discountedPrice) }}%</span>
                  </template>
                  <template v-else>
                    <span class="current-price">{{ formatPrice(item.price) }}</span>
                  </template>
                </div>
              </div>

              <div class="col-quantity">
                <div class="quantity-control">
                  <button 
                    @click="updateQuantity(item, -1)"
                    :disabled="item.quantity <= 1 || updatingItems.has(item.cartItemId || item.id)"
                  >
                    <i class="fas fa-minus"></i>
                  </button>
                  <input 
                    type="number" 
                    v-model.number="item.quantity"
                    @blur="handleQuantityChange(item)"
                    @keyup.enter="handleQuantityChange(item)"
                    min="1"
                    :max="item.stock || 99"
                    class="quantity-input"
                    :disabled="updatingItems.has(item.cartItemId || item.id)"
                  />
                  <button 
                    @click="updateQuantity(item, 1)"
                    :disabled="item.quantity >= (item.stock || 99) || updatingItems.has(item.cartItemId || item.id)"
                  >
                    <i class="fas fa-plus"></i>
                  </button>
                </div>
                <div v-if="quantityErrors[item.cartItemId || item.id]" class="quantity-error">
                  <i class="fas fa-exclamation-circle"></i>
                  {{ quantityErrors[item.cartItemId || item.id] }}
                </div>
              </div>

              <div class="col-total">
                <span class="total-price">{{ formatPrice((item.discountedPrice || item.price || 0) * (item.quantity || 0)) }}</span>
              </div>

              <div class="col-action">
                <button class="remove-btn" @click="removeItem(item)" title="Xóa sản phẩm">
                  <i class="fas fa-trash-alt"></i>
                </button>
              </div>
            </div>

            <!-- ✅ PHẦN VOUCHER - ĐỔI THÀNH CHECKBOX -->
            <div class="cart-coupon">
              <div class="coupon-wrapper">
                <div class="voucher-section">
                  <div class="voucher-header">
                    <i class="fas fa-ticket-alt"></i>
                    <span class="voucher-title">Mã giảm giá</span>
                    <span v-if="availableVouchers.length > 0" class="voucher-count">
                      Có {{ availableVouchers.length }} mã có thể áp dụng
                    </span>
                    <button 
                      class="refresh-btn" 
                      @click="fetchAvailableVouchers"
                      :disabled="loadingVouchers"
                    >
                      <i class="fas fa-sync-alt" :class="{ 'fa-spin': loadingVouchers }"></i>
                    </button>
                  </div>

                  <!-- Loading vouchers -->
                  <div v-if="loadingVouchers" class="loading-vouchers">
                    <i class="fas fa-spinner fa-spin"></i>
                    <span>Đang tải mã giảm giá...</span>
                  </div>

                  <!-- Danh sách voucher dạng checkbox -->
                  <div v-else-if="availableVouchers.length > 0" class="voucher-list">
                    <div 
                      v-for="voucher in availableVouchers" 
                      :key="voucher.voucherId"
                      class="voucher-item"
                      :class="{ 
                        'selected': selectedVoucher && selectedVoucher.voucherId === voucher.voucherId,
                        'disabled': !canApplyVoucher(voucher)
                      }"
                      @click="toggleVoucher(voucher)"
                    >
                      <div class="voucher-checkbox">
                        <input 
                          type="checkbox" 
                          :id="'voucher-' + voucher.voucherId"
                          :checked="selectedVoucher && selectedVoucher.voucherId === voucher.voucherId"
                          :disabled="!canApplyVoucher(voucher)"
                          @change="toggleVoucher(voucher)"
                        />
                        <label :for="'voucher-' + voucher.voucherId">
                          <span class="voucher-code">{{ voucher.voucherCode }}</span>
                          <span class="voucher-discount">
                            {{ voucher.voucherDiscountPercent ? voucher.voucherDiscountPercent + '%' : formatPrice(voucher.voucherDiscountAmount) }}
                          </span>
                          <span v-if="voucher.voucherMinOrderValue > 0" class="voucher-min-order">
                            Đơn tối thiểu {{ formatPrice(voucher.voucherMinOrderValue) }}
                          </span>
                          <span class="voucher-expiry">
                            <i class="fas fa-clock"></i>
                            {{ formatDate(voucher.voucherEndDate) }}
                          </span>
                          <span v-if="!canApplyVoucher(voucher)" class="voucher-unavailable">
                            <i class="fas fa-exclamation-circle"></i>
                            Chưa đạt điều kiện
                          </span>
                        </label>
                      </div>
                    </div>
                  </div>

                  <!-- Không có voucher -->
                  <div v-else class="no-voucher">
                    <i class="fas fa-ticket-alt"></i>
                    <span>Hiện chưa có mã giảm giá nào</span>
                  </div>

                  <!-- Thông báo lỗi -->
                  <div v-if="voucherError" class="voucher-error">
                    <i class="fas fa-exclamation-circle"></i>
                    {{ voucherError }}
                  </div>
                </div>
              </div>

              <div class="cart-actions-extra">
                <button class="clear-cart-btn" @click="clearAllCart">
                  <i class="fas fa-trash-alt"></i> Xóa toàn bộ
                </button>
                <button class="update-cart-btn" @click="updateAllCart">
                  <i class="fas fa-sync-alt"></i> Cập nhật giỏ hàng
                </button>
              </div>
            </div>
          </div>

          <div class="cart-summary">
            <h2>Thông tin đơn hàng</h2>
            
            <div class="summary-items">
              <div class="summary-row">
                <span>Số lượng sản phẩm:</span>
                <span class="highlight">{{ cartStore.totalItems }}</span>
              </div>

              <div class="summary-row">
                <span>Tạm tính:</span>
                <span>{{ formatPrice(cartStore.subtotal) }}</span>
              </div>

              <div class="summary-row">
                <span>Phí vận chuyển:</span>
                <span>{{ formatPrice(cartStore.shippingFee || 30000) }}</span>
              </div>

              <div v-if="cartStore.voucherDiscount > 0" class="summary-row discount">
                <span>Giảm giá (Voucher):</span>
                <span>-{{ formatPrice(cartStore.voucherDiscount) }}</span>
              </div>

              <div class="summary-divider"></div>

              <div class="summary-row total">
                <span>Tổng cộng:</span>
                <span class="total-amount">{{ formatPrice(cartStore.total) }}</span>
              </div>
            </div>

            <div class="summary-note" v-if="cartStore.subtotal < 500000">
              <i class="fas fa-truck"></i> 
              Mua thêm <strong>{{ formatPrice(500000 - cartStore.subtotal) }}</strong> để được miễn phí vận chuyển!
            </div>

            <div class="summary-note success" v-else>
              <i class="fas fa-check-circle"></i> 
              Bạn đã được miễn phí vận chuyển!
            </div>

            <router-link 
              to="/checkout" 
              class="checkout-btn"
              :class="{ disabled: cartStore.items.length === 0 }"
            >
              <i class="fas fa-credit-card"></i> 
              TIẾN HÀNH THANH TOÁN
              <span class="btn-badge">{{ cartStore.totalItems }}</span>
            </router-link>

            <router-link to="/shop" class="continue-shopping">
              <i class="fas fa-arrow-left"></i> Tiếp tục mua sắm
            </router-link>
          </div>
        </div>

        <div v-if="suggestedProducts.length > 0" class="suggested-products">
          <div class="section-header">
            <h2 class="section-title">CÓ THỂ BẠN CŨNG THÍCH</h2>
            <router-link to="/shop" class="view-all">Xem tất cả <i class="fas fa-arrow-right"></i></router-link>
          </div>
          <div class="product-grid">
            <ProductCard
              v-for="product in suggestedProducts"
              :key="product.productId"
              :product="product"
            />
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import ProductCard from './ProductCard.vue'
import { cartService } from '../../services/cartService'
import api from "@/api/axios";

const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");

const router = useRouter()
const cartStore = useCartStore()
const { showNotification } = useNotification()

// State
const suggestedProducts = ref([])
const updatingItems = ref(new Set())
const quantityErrors = ref({})
const availableVouchers = ref([])
const selectedVoucher = ref(null)
const loadingVouchers = ref(false)
const voucherError = ref('')
const applyingVoucher = ref(false)

// ✅ HÀM LẤY URL ẢNH ĐẦY ĐỦ
const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  // Nếu là đường dẫn tương đối, thêm base URL
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

// Format functions
const formatPrice = (price) => {
  if (!price && price !== 0) return '0₫'
  const numPrice = typeof price === 'string' ? parseFloat(price) : price
  if (isNaN(numPrice) || numPrice === 0) return '0₫'
  return numPrice.toLocaleString('vi-VN') + '₫'
}

const formatDate = (dateString) => {
  if (!dateString) return '---'
  const date = new Date(dateString)
  if (isNaN(date.getTime())) return '---'
  return date.toLocaleDateString('vi-VN')
}

const calculateDiscount = (originalPrice, discountedPrice) => {
  if (!originalPrice || !discountedPrice || originalPrice <= discountedPrice) return 0
  return Math.round(((originalPrice - discountedPrice) / originalPrice) * 100)
}

const getColorHex = (colorName) => {
  if (!colorName) return '#CCCCCC'
  const colorMap = {
    'Đỏ': '#FF0000', 'Đen': '#000000', 'Trắng': '#FFFFFF',
    'Xanh': '#0000FF', 'Xanh dương': '#0000FF', 'Navy': '#1B2951',
    'Vàng': '#FFFF00', 'Hồng': '#FFC0CB', 'Xám': '#808080',
    'Nâu': '#8B4513', 'Tím': '#800080', 'Cam': '#FFA500'
  }
  return colorMap[colorName] || '#CCCCCC'
}

const handleImageError = (event) => {
  event.target.style.display = 'none'
  const parent = event.target.parentElement
  if (parent && !parent.querySelector('i')) {
    const icon = document.createElement('i')
    icon.className = 'fas fa-tshirt'
    parent.appendChild(icon)
  }
}

// Kiểm tra xem voucher có áp dụng được không
const canApplyVoucher = (voucher) => {
  const subtotal = cartStore.subtotal || 0
  if (voucher.voucherMinOrderValue > 0 && subtotal < voucher.voucherMinOrderValue) {
    return false
  }
  return true
}

// Lấy danh sách voucher có thể áp dụng
const fetchAvailableVouchers = async () => {
  loadingVouchers.value = true
  voucherError.value = ''
  
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      availableVouchers.value = []
      return
    }

    const response = await api.get(`/vouchers/available`, {
      params: {
        orderValue: cartStore.subtotal || 0
      }
    })

    console.log('📦 Voucher API response:', response.data)

    // ✅ Lấy data từ ApiResponse
    let vouchersData = []
    if (response.data && response.data.data) {
      vouchersData = response.data.data
    } else if (Array.isArray(response.data)) {
      vouchersData = response.data
    }

    // ✅ Lọc voucher hợp lệ
    const now = new Date()
    availableVouchers.value = vouchersData.filter(v => {
      // Kiểm tra status
      if (v.voucherStatus !== 'ACTIVE') return false
      
      // Kiểm tra thời gian
      const startDate = new Date(v.voucherStartDate)
      const endDate = new Date(v.voucherEndDate)
      if (now < startDate || now > endDate) return false
      
      // Kiểm tra số lần sử dụng
      if (v.voucherMaxUsage !== null && v.voucherUsedCount >= v.voucherMaxUsage) return false
      
      return true
    })

    console.log('✅ Available vouchers after filter:', availableVouchers.value.length)
    console.log('📋 Voucher list:', availableVouchers.value)

    // Nếu đang có voucher áp dụng, kiểm tra xem còn hợp lệ không
    if (cartStore.voucherCode) {
      const currentVoucher = availableVouchers.value.find(
        v => v.voucherCode === cartStore.voucherCode
      )
      if (!currentVoucher) {
        // Voucher không còn hợp lệ, xóa khỏi giỏ
        await removeCurrentVoucher()
      } else {
        selectedVoucher.value = currentVoucher
      }
    }

  } catch (error) {
    console.error('❌ Lỗi tải voucher:', error)
    voucherError.value = 'Không thể tải danh sách mã giảm giá'
  } finally {
    loadingVouchers.value = false
  }
}

// Chọn/Bỏ chọn voucher (checkbox)
const toggleVoucher = async (voucher) => {
  if (applyingVoucher.value) return

  // Kiểm tra điều kiện áp dụng
  if (!canApplyVoucher(voucher)) {
    showNotification(`Đơn hàng tối thiểu ${formatPrice(voucher.voucherMinOrderValue)} để sử dụng mã này`, 'warning')
    return
  }

  // Nếu đang chọn voucher khác, bỏ chọn cũ
  if (selectedVoucher.value && selectedVoucher.value.voucherId !== voucher.voucherId) {
    await removeCurrentVoucher()
  }

  // Nếu chọn cùng voucher -> bỏ chọn
  if (selectedVoucher.value && selectedVoucher.value.voucherId === voucher.voucherId) {
    await removeCurrentVoucher()
    return
  }

  // Áp dụng voucher mới
  applyingVoucher.value = true
  try {
    const response = await cartService.applyVoucher(voucher.voucherCode)
    if (response.data && response.data.success) {
      selectedVoucher.value = voucher
      await cartStore.fetchCart()
      showNotification(`Đã áp dụng mã ${voucher.voucherCode}!`, 'success')
    }
  } catch (error) {
    console.error('Lỗi áp dụng voucher:', error)
    showNotification(error.response?.data?.message || 'Không thể áp dụng mã giảm giá', 'error')
  } finally {
    applyingVoucher.value = false
  }
}

// Xóa voucher đang áp dụng
const removeCurrentVoucher = async () => {
  if (!selectedVoucher.value) return

  try {
    const response = await cartService.removeVoucher()
    if (response.data && response.data.success) {
      selectedVoucher.value = null
      await cartStore.fetchCart()
      showNotification('Đã xóa mã giảm giá', 'info')
    }
  } catch (error) {
    console.error('Lỗi xóa voucher:', error)
  }
}

// Update quantity
const updateQuantity = async (item, change) => {
  const newQuantity = (item.quantity || 0) + change
  const maxStock = item.stock || 99
  
  if (newQuantity < 1) {
    showNotification("Số lượng tối thiểu là 1", "warning")
    return
  }

  if (newQuantity > maxStock) {
    showNotification(`Số lượng vượt quá tồn kho (${maxStock})`, "error")
    return
  }

  await performUpdateQuantity(item, newQuantity)
}

const handleQuantityChange = async (item) => {
  const itemId = item.cartItemId || item.id
  let newQuantity = parseInt(item.quantity) || 1

  if (newQuantity < 1) {
    newQuantity = 1
    item.quantity = 1
    quantityErrors.value[itemId] = 'Số lượng tối thiểu là 1'
    setTimeout(() => {
      delete quantityErrors.value[itemId]
    }, 3000)
    return
  }

  const maxStock = item.stock || 99
  if (newQuantity > maxStock) {
    quantityErrors.value[itemId] = `Số lượng vượt quá tồn kho (${maxStock})`
    item.quantity = maxStock
    newQuantity = maxStock
    setTimeout(() => {
      delete quantityErrors.value[itemId]
    }, 3000)
    return
  }

  if (item.quantity === newQuantity) return

  await performUpdateQuantity(item, newQuantity)
}

const performUpdateQuantity = async (item, newQuantity) => {
  const itemId = item.cartItemId || item.id
  
  if (!item.cartItemId) {
    console.error("❌ Không có cartItemId:", item)
    showNotification("Lỗi: Không tìm thấy ID sản phẩm trong giỏ", "error")
    return
  }
  
  if (updatingItems.value.has(itemId)) return

  updatingItems.value.add(itemId)
  const oldQuantity = item.quantity

  try {
    item.quantity = newQuantity
    await cartStore.updateQuantity(
      item.cartItemId,
      newQuantity,
      item.productDetailId || item.id,
      item.size,
      item.color
    )
    delete quantityErrors.value[itemId]
  } catch (error) {
    console.error("❌ Update quantity error:", error)
    item.quantity = oldQuantity
    const message = error.response?.data?.message || error.message || 'Cập nhật thất bại'
    quantityErrors.value[itemId] = message
    showNotification(message, 'error')
    setTimeout(() => {
      delete quantityErrors.value[itemId]
    }, 3000)
  } finally {
    updatingItems.value.delete(itemId)
  }
}

const updateAllCart = async () => {
  showNotification('Đang cập nhật giỏ hàng...', 'info')
  await cartStore.fetchCart()
  await fetchAvailableVouchers()
  showNotification('Đã cập nhật giỏ hàng!', 'success')
}

const removeItem = async (item) => {
  if (confirm(`Bạn có chắc muốn xóa "${item.name}" khỏi giỏ hàng?`)) {
    try {
      await cartStore.removeFromCart(item.cartItemId, item.id, item.size, item.color)
      showNotification('Đã xóa sản phẩm khỏi giỏ hàng', 'success')
      if (selectedVoucher.value && !canApplyVoucher(selectedVoucher.value)) {
        await removeCurrentVoucher()
      }
      await fetchSuggestedProducts()
      await fetchAvailableVouchers()
    } catch (error) {
      console.error('Remove item error:', error)
      showNotification(error.response?.data?.message || 'Xóa thất bại', 'error')
    }
  }
}

const clearAllCart = async () => {
  if (confirm('Bạn có chắc muốn xóa toàn bộ giỏ hàng?')) {
    try {
      await cartStore.clearCart()
      selectedVoucher.value = null
      showNotification('Đã xóa toàn bộ giỏ hàng', 'success')
      await fetchAvailableVouchers()
    } catch (error) {
      console.error('Clear cart error:', error)
      showNotification(error.response?.data?.message || 'Xóa thất bại', 'error')
    }
  }
}

const goToProduct = (id) => {
  if (id) {
    router.push(`/product/${id}`)
  }
}

const fetchSuggestedProducts = async () => {
  try {
    const response = await api.get(`/customer/home/latest-products`, {
      params: { page: 0, size: 4 }
    })
    let products = response.data?.products || []
    const cartIds = cartStore.items.map(item => item.productId || item.id)
    suggestedProducts.value = products.filter(p => !cartIds.includes(p.productId))
  } catch (error) {
    console.error('Lỗi tải sản phẩm gợi ý:', error)
  }
}

const handleLoginSuccess = () => {
  cartStore.syncCartAfterLogin()
}

watch(() => cartStore.items.length, () => {
  fetchSuggestedProducts()
})

// Watch subtotal để kiểm tra lại điều kiện voucher
watch(() => cartStore.subtotal, (newSubtotal) => {
  if (selectedVoucher.value && !canApplyVoucher(selectedVoucher.value)) {
    removeCurrentVoucher()
  }
})

onMounted(async () => {
  console.log("🚀 Cart page mounted")

  const urlParams = new URLSearchParams(window.location.search)
  if (urlParams.get('payment') === 'failed') {
    showNotification(
      'Thanh toán VNPay thất bại. Đơn hàng đã bị hủy, sản phẩm vẫn còn trong giỏ hàng.',
      'error'
    )
    router.replace({ path: '/cart' })
  }

  await cartStore.fetchCart()
  await fetchAvailableVouchers()
  await fetchSuggestedProducts()
  
  // Kiểm tra voucher đang áp dụng
  if (cartStore.voucherCode) {
    const voucher = availableVouchers.value.find(v => v.voucherCode === cartStore.voucherCode)
    if (voucher && canApplyVoucher(voucher)) {
      selectedVoucher.value = voucher
    } else {
      await removeCurrentVoucher()
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('login-success', handleLoginSuccess)
})
</script>

<style scoped>
/* ===== GIỮ NGUYÊN CÁC STYLE CŨ ===== */
.cart-page {
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

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 36px;
  font-weight: 800;
  color: #2c3e50;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.page-subtitle {
  font-size: 16px;
  color: #666;
}

.page-subtitle strong {
  color: #e63946;
}

.loading-state {
  text-align: center;
  padding: 80px;
  background: white;
  border-radius: 15px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #e63946;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-cart {
  text-align: center;
  padding: 80px 40px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.05);
}

.empty-icon i {
  font-size: 100px;
  color: #ddd;
  margin-bottom: 20px;
}

.empty-cart h2 {
  font-size: 28px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.empty-cart p {
  color: #666;
  margin-bottom: 30px;
  font-size: 16px;
}

.empty-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn-primary, .btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s;
}

.btn-primary {
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.3);
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

.cart-content {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 30px;
  align-items: start;
}

.cart-items-wrapper {
  background: white;
  border-radius: 15px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.cart-items-header {
  display: grid;
  grid-template-columns: 4fr 1.2fr 1.2fr 1.2fr 0.8fr;
  padding: 15px 25px;
  background: #f8f9fa;
  font-weight: 700;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.cart-item {
  display: grid;
  grid-template-columns: 4fr 1.2fr 1.2fr 1.2fr 0.8fr;
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
  align-items: center;
  transition: background 0.3s;
}

.cart-item:hover {
  background: #fafafa;
}

.cart-item:last-child {
  border-bottom: none;
}

.product-info {
  display: flex;
  gap: 15px;
  align-items: center;
}

.product-image {
  width: 100px;
  height: 100px;
  background: #f5f5f5;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  overflow: hidden;
  flex-shrink: 0;
}

.product-image:hover {
  transform: scale(1.02);
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-image i {
  font-size: 40px;
  color: #ccc;
}

.product-details {
  flex: 1;
}

.product-details h3 {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 6px;
  cursor: pointer;
  transition: color 0.3s;
}

.product-details h3:hover {
  color: #e63946;
}

.product-variants {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 5px;
}

.variant-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #f0f0f0;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  color: #666;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid #ddd;
}

.product-stock {
  font-size: 13px;
  color: #28a745;
}

.product-stock.low-stock {
  color: #e63946;
}

.product-stock .text-danger {
  font-weight: 700;
}

.product-stock i {
  margin-right: 5px;
}

.col-price {
  text-align: center;
}

.price-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.current-price {
  font-weight: 700;
  color: #e63946;
  font-size: 17px;
}

.original-price {
  font-size: 13px;
  color: #999;
  text-decoration: line-through;
}

.col-quantity {
  text-align: center;
}

.quantity-control {
  display: inline-flex;
  align-items: center;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.quantity-control button {
  width: 36px;
  height: 36px;
  background: white;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.quantity-control button:hover:not(:disabled) {
  background: #e63946;
  color: white;
}

.quantity-control button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.quantity-input {
  width: 50px;
  height: 36px;
  border: none;
  border-left: 2px solid #e0e0e0;
  border-right: 2px solid #e0e0e0;
  text-align: center;
  font-weight: 600;
  font-size: 15px;
  outline: none;
  padding: 0;
  background: white;
  color: #333;
}

.quantity-input:focus {
  background: #f8f9fa;
}

.quantity-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quantity-error {
  font-size: 12px;
  color: #e63946;
  margin-top: 5px;
  animation: fadeIn 0.3s ease;
}

.col-total {
  text-align: center;
}

.total-price {
  font-weight: 700;
  color: #e63946;
  font-size: 18px;
}

.col-action {
  text-align: center;
}

.remove-btn {
  width: 40px;
  height: 40px;
  background: none;
  border: 1px solid #eee;
  color: #999;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 16px;
  border-radius: 50%;
}

.remove-btn:hover {
  color: #e63946;
  background: #fee;
  border-color: #e63946;
  transform: scale(1.1);
}

/* ===== VOUCHER SECTION - STYLE MỚI ===== */
.cart-coupon {
  padding: 20px 25px;
  background: #f8f9fa;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 15px;
}

.coupon-wrapper {
  flex: 1;
  min-width: 300px;
}

.voucher-section {
  width: 100%;
}

.voucher-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.voucher-header i {
  font-size: 20px;
  color: #e63946;
}

.voucher-title {
  font-weight: 600;
  color: #2c3e50;
  font-size: 16px;
}

.voucher-count {
  font-size: 13px;
  color: #28a745;
  background: #e8f5e9;
  padding: 2px 10px;
  border-radius: 12px;
}

.refresh-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
  font-size: 16px;
  padding: 4px 8px;
  transition: all 0.3s;
}

.refresh-btn:hover {
  color: #e63946;
}

.loading-vouchers {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  color: #666;
  font-size: 14px;
}

.voucher-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 250px;
  overflow-y: auto;
}

.voucher-item {
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  padding: 10px 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.voucher-item:hover:not(.disabled) {
  border-color: #e63946;
}

.voucher-item.selected {
  border-color: #28a745;
  background: #f0faf0;
}

.voucher-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.voucher-checkbox {
  display: flex;
  align-items: flex-start;
}

.voucher-checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
  margin-right: 12px;
  margin-top: 2px;
  cursor: pointer;
  accent-color: #e63946;
  flex-shrink: 0;
}

.voucher-checkbox input[type="checkbox"]:disabled {
  cursor: not-allowed;
}

.voucher-checkbox label {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  cursor: pointer;
  flex: 1;
  font-size: 14px;
}

.voucher-code {
  font-weight: 700;
  color: #e63946;
  font-size: 15px;
  background: #fff5f5;
  padding: 2px 10px;
  border-radius: 4px;
}

.voucher-discount {
  font-weight: 600;
  color: #28a745;
  font-size: 14px;
}

.voucher-min-order {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.voucher-expiry {
  font-size: 12px;
  color: #999;
}

.voucher-expiry i {
  margin-right: 3px;
}

.voucher-unavailable {
  font-size: 12px;
  color: #e63946;
  display: flex;
  align-items: center;
  gap: 4px;
}

.no-voucher {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  color: #999;
  font-size: 14px;
}

.voucher-error {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #e63946;
  font-size: 13px;
  padding: 8px 12px;
  background: #fde8e8;
  border-radius: 6px;
  margin-top: 8px;
}

.cart-actions-extra {
  display: flex;
  gap: 10px;
}

.clear-cart-btn, .update-cart-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.clear-cart-btn:hover {
  background: #fee;
  border-color: #e63946;
  color: #e63946;
}

.update-cart-btn:hover {
  background: #f0f0f0;
}

.cart-summary {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 160px;
}

.cart-summary h2 {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #e63946;
}

.summary-items {
  margin-bottom: 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  color: #666;
  font-size: 15px;
}

.summary-row .highlight {
  font-weight: 700;
  color: #2c3e50;
}

.summary-row.discount {
  color: #28a745;
}

.summary-divider {
  height: 1px;
  background: #eee;
  margin: 10px 0;
}

.summary-row.total {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  padding-top: 10px;
}

.total-amount {
  color: #e63946;
  font-size: 24px;
}

.summary-note {
  padding: 12px 16px;
  background: #fff3cd;
  color: #856404;
  border-radius: 8px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 15px 0;
}

.summary-note.success {
  background: #d4edda;
  color: #155724;
}

.checkout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border: none;
  padding: 16px;
  border-radius: 10px;
  font-weight: 700;
  font-size: 17px;
  cursor: pointer;
  transition: all 0.3s;
  text-decoration: none;
  position: relative;
  margin-top: 10px;
}

.checkout-btn .btn-badge {
  background: rgba(255,255,255,0.2);
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 13px;
}

.checkout-btn:hover:not(.disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(230, 57, 70, 0.3);
}

.checkout-btn.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.continue-shopping {
  display: block;
  text-align: center;
  color: #999;
  text-decoration: none;
  font-size: 14px;
  margin-top: 15px;
  transition: color 0.3s;
}

.continue-shopping:hover {
  color: #e63946;
}

.suggested-products {
  margin-top: 50px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
  position: relative;
  padding-bottom: 10px;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 50px;
  height: 3px;
  background: #e63946;
}

.view-all {
  color: #e63946;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.3s;
}

.view-all:hover {
  color: #c62828;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

/* ===== THÊM STYLE CHO DISCOUNT BADGE ===== */
.discount-badge {
  display: inline-block;
  background: #e63946;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  margin-left: 5px;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

/* ===== RESPONSIVE ===== */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .cart-content {
    grid-template-columns: 1fr;
  }
  
  .cart-summary {
    position: static;
  }
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .cart-items-header {
    display: none;
  }

  .cart-item {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 20px;
    position: relative;
  }

  .product-info {
    grid-column: 1 / -1;
  }

  .product-image {
    width: 80px;
    height: 80px;
  }

  .col-price, .col-total {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 5px 0;
  }

  .col-price::before {
    content: 'Đơn giá: ';
    font-weight: 600;
    color: #666;
  }

  .col-total::before {
    content: 'Thành tiền: ';
    font-weight: 600;
    color: #666;
  }

  .col-quantity {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 5px 0;
  }

  .col-quantity::before {
    content: 'Số lượng: ';
    font-weight: 600;
    color: #666;
  }

  .col-action {
    position: absolute;
    top: 15px;
    right: 15px;
  }

  .remove-btn {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }

  .cart-coupon {
    flex-direction: column;
  }

  .coupon-wrapper {
    flex-direction: column;
    align-items: stretch;
  }

  .cart-actions-extra {
    justify-content: center;
  }

  .page-title {
    font-size: 28px;
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .voucher-checkbox label {
    gap: 6px;
  }
  
  .voucher-code {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 24px;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }

  .product-info {
    flex-direction: column;
    text-align: center;
  }

  .product-image {
    margin: 0 auto;
  }

  .empty-actions {
    flex-direction: column;
  }

  .empty-actions a {
    width: 100%;
    justify-content: center;
  }

  .quantity-control {
    margin: 0 auto;
  }

  .col-price, .col-total {
    flex-wrap: wrap;
    justify-content: center;
  }

  .voucher-checkbox {
    flex-direction: column;
  }
  
  .voucher-checkbox label {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>