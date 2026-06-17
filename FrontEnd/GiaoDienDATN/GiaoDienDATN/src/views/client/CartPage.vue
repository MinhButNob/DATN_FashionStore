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
                      :src="item.image" 
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
                  <span class="current-price">{{ formatPrice(item.price) }}</span>
                  <span v-if="item.originalPrice" class="original-price">{{ formatPrice(item.originalPrice) }}</span>
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
                <span class="total-price">{{ formatPrice((item.price || 0) * (item.quantity || 0)) }}</span>
              </div>

              <div class="col-action">
                <button class="remove-btn" @click="removeItem(item)" title="Xóa sản phẩm">
                  <i class="fas fa-trash-alt"></i>
                </button>
              </div>
            </div>

            <div class="cart-coupon">
              <div class="coupon-wrapper">
                <div class="coupon-input-group">
                  <i class="fas fa-tag"></i>
                  <input 
                    type="text" 
                    v-model="couponCode" 
                    placeholder="Nhập mã giảm giá"
                    @keyup.enter="applyCoupon"
                  />
                  <button 
                    class="apply-coupon-btn"
                    @click="applyCoupon"
                    :disabled="!couponCode"
                  >
                    Áp dụng
                  </button>
                </div>
                <div v-if="appliedCoupon" class="applied-coupon">
                  <i class="fas fa-check-circle"></i>
                  <span>Đã áp dụng: <strong>{{ appliedCoupon }}</strong></span>
                  <i class="fas fa-times" @click="removeCoupon"></i>
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
                <span>{{ formatPrice(cartStore.shippingFee) }}</span>
              </div>

              <div v-if="discount > 0" class="summary-row discount">
                <span>Giảm giá:</span>
                <span>-{{ formatPrice(discount) }}</span>
              </div>

              <div class="summary-divider"></div>

              <div class="summary-row total">
                <span>Tổng cộng:</span>
                <span class="total-amount">{{ formatPrice(cartStore.total - discount) }}</span>
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
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import ProductCard from '../../components/client/ProductCard.vue'
import axios from 'axios'

const API_BASE = "http://localhost:8080/api"

const router = useRouter()
const cartStore = useCartStore()
const { showNotification } = useNotification()

const couponCode = ref('')
const discount = ref(0)
const appliedCoupon = ref('')
const suggestedProducts = ref([])
const updatingItems = ref(new Set())
const quantityErrors = ref({})

const formatPrice = (price) => {
  if (!price && price !== 0) return '0₫'
  const numPrice = typeof price === 'string' ? parseFloat(price) : price
  if (isNaN(numPrice) || numPrice === 0) return '0₫'
  return numPrice.toLocaleString('vi-VN') + '₫'
}

const getColorHex = (colorName) => {
  if (!colorName) return '#CCCCCC'
  const colorMap = {
    'Đỏ': '#FF0000',
    'Đen': '#000000',
    'Trắng': '#FFFFFF',
    'Xanh': '#0000FF',
    'Xanh dương': '#0000FF',
    'Xanh lá': '#00FF00',
    'Navy': '#1B2951',
    'Vàng': '#FFFF00',
    'Hồng': '#FFC0CB',
    'Xám': '#808080',
    'Nâu': '#8B4513',
    'Tím': '#800080',
    'Cam': '#FFA500',
    'Be': '#F5F5DC',
    'Bạc': '#C0C0C0',
    'Vàng kim': '#FFD700'
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
  showNotification('Đã cập nhật giỏ hàng!', 'success')
}

const removeItem = async (item) => {
  if (confirm(`Bạn có chắc muốn xóa "${item.name}" khỏi giỏ hàng?`)) {
    try {
      await cartStore.removeFromCart(item.cartItemId, item.id, item.size, item.color)
      showNotification('Đã xóa sản phẩm khỏi giỏ hàng', 'success')
      await fetchSuggestedProducts()
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
      showNotification('Đã xóa toàn bộ giỏ hàng', 'success')
    } catch (error) {
      console.error('Clear cart error:', error)
      showNotification(error.response?.data?.message || 'Xóa thất bại', 'error')
    }
  }
}

const applyCoupon = () => {
  const code = couponCode.value.toUpperCase().trim()
  
  if (code === 'SALE20') {
    discount.value = cartStore.subtotal * 0.2
    appliedCoupon.value = 'SALE20 (Giảm 20%)'
    showNotification('Áp dụng mã SALE20 thành công! Giảm 20%', 'success')
  } else if (code === 'FREESHIP') {
    discount.value = cartStore.shippingFee
    appliedCoupon.value = 'FREESHIP (Miễn phí vận chuyển)'
    showNotification('Áp dụng mã FREESHIP thành công! Miễn phí vận chuyển', 'success')
  } else if (code === 'SALE50K') {
    discount.value = 50000
    appliedCoupon.value = 'SALE50K (Giảm 50,000đ)'
    showNotification('Áp dụng mã SALE50K thành công! Giảm 50,000đ', 'success')
  } else {
    showNotification('Mã giảm giá không hợp lệ!', 'error')
  }
}

const removeCoupon = () => {
  discount.value = 0
  appliedCoupon.value = ''
  couponCode.value = ''
  showNotification('Đã xóa mã giảm giá', 'info')
}

const goToProduct = (id) => {
  if (id) {
    router.push(`/product/${id}`)
  }
}

const fetchSuggestedProducts = async () => {
  try {
    const response = await axios.get(`${API_BASE}/customer/home/latest-products`, {
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

onMounted(async () => {
  console.log("🚀 Cart page mounted")
  await cartStore.fetchCart()
  console.log("📦 Cart items after fetch:", cartStore.items)
  
  cartStore.items.forEach((item, index) => {
    console.log(`📦 Item ${index + 1}:`, {
      id: item.id,
      cartItemId: item.cartItemId,
      name: item.name,
      quantity: item.quantity,
      price: item.price,
      stock: item.stock
    })
  })
  
  await fetchSuggestedProducts()
  window.addEventListener('login-success', handleLoginSuccess)
})

onUnmounted(() => {
  window.removeEventListener('login-success', handleLoginSuccess)
})
</script>

<style scoped>
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

.cart-coupon {
  padding: 20px 25px;
  background: #f8f9fa;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.coupon-wrapper {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.coupon-input-group {
  display: flex;
  align-items: center;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 0 5px 0 15px;
  transition: border-color 0.3s;
}

.coupon-input-group:focus-within {
  border-color: #e63946;
}

.coupon-input-group i {
  color: #999;
  font-size: 16px;
}

.coupon-input-group input {
  flex: 1;
  padding: 10px 12px;
  border: none;
  outline: none;
  font-size: 14px;
  min-width: 180px;
}

.apply-coupon-btn {
  padding: 8px 20px;
  background: #e63946;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s;
  white-space: nowrap;
}

.apply-coupon-btn:hover:not(:disabled) {
  background: #c62828;
  transform: translateY(-2px);
}

.apply-coupon-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.applied-coupon {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px 6px 16px;
  background: #e8f5e9;
  border-radius: 20px;
  font-size: 13px;
  color: #2e7d32;
}

.applied-coupon i.fa-times {
  cursor: pointer;
  padding: 5px;
  transition: color 0.3s;
}

.applied-coupon i.fa-times:hover {
  color: #e63946;
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
    align-items: stretch;
  }

  .coupon-wrapper {
    flex-direction: column;
    align-items: stretch;
  }

  .coupon-input-group {
    width: 100%;
  }

  .coupon-input-group input {
    min-width: auto;
  }

  .apply-coupon-btn {
    padding: 10px 20px;
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
}
</style>