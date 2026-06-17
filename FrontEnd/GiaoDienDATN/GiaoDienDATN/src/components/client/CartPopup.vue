<template>
  <Teleport to="body">
    <Transition name="popup">
      <div v-if="visible" class="cart-popup-overlay" @click.self="close">
        <div class="cart-popup">
          <div class="popup-header">
            <div class="success-icon">
              <i class="fas fa-check-circle"></i>
            </div>
            <h3>Đã thêm vào giỏ hàng!</h3>
            <button class="close-btn" @click="close">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="popup-body">
            <div class="product-info">
              <div class="product-image">
                <img 
                  v-if="product.image && product.image !== 'tshirt' && !product.image.includes('placeholder')" 
                  :src="product.image" 
                  :alt="product.name"
                  @error="handleImageError"
                />
                <i v-else class="fas fa-tshirt"></i>
              </div>
              <div class="product-details">
                <h4>{{ product.name }}</h4>
                <p class="product-variant" v-if="product.size || product.color">
                  {{ product.size || '' }} {{ product.color || '' }}
                </p>
                <div class="product-price-wrapper">
                  <!-- Hiển thị giá gốc và giá đã giảm nếu có -->
                  <p v-if="product.originalPrice && product.originalPrice > product.price" class="product-price">
                    <span class="original-price">{{ formatPrice(product.originalPrice) }}</span>
                    <span class="discounted-price">{{ formatPrice(product.price) }}</span>
                    <span class="discount-badge">-{{ calculateDiscount(product.originalPrice, product.price) }}%</span>
                  </p>
                  <p v-else class="product-price">
                    <span class="discounted-price">{{ formatPrice(product.price) }}</span>
                  </p>
                </div>
                
                <div class="quantity-stock">
                  <div class="quantity-control">
                    <button @click="decreaseQuantity" :disabled="localQuantity <= 1">
                      <i class="fas fa-minus"></i>
                    </button>
                    <span>{{ localQuantity }}</span>
                    <button @click="increaseQuantity" :disabled="localQuantity >= (product.stock || 99)">
                      <i class="fas fa-plus"></i>
                    </button>
                  </div>
                  <span class="stock-info" :class="{ 'low-stock': (product.stock || 0) <= 5 }">
                    <i class="fas fa-boxes"></i>
                    Còn: <strong>{{ product.stock || 0 }}</strong> sản phẩm
                  </span>
                </div>
              </div>
            </div>

            <div class="cart-summary">
              <div class="summary-row">
                <span>Số lượng sản phẩm:</span>
                <span>{{ cartStore.totalItems }}</span>
              </div>
              <div class="summary-row">
                <span>Tạm tính:</span>
                <span>{{ formatPrice(cartStore.subtotal) }}</span>
              </div>
              <div v-if="cartStore.voucherDiscount > 0" class="summary-row discount">
                <span>Giảm giá (Voucher):</span>
                <span>-{{ formatPrice(cartStore.voucherDiscount) }}</span>
              </div>
              <div class="summary-row total">
                <span>Tổng tiền:</span>
                <span>{{ formatPrice(cartStore.total) }}</span>
              </div>
            </div>
          </div>

          <div class="popup-footer">
            <button class="btn btn-outline" @click="continueShopping">
              <i class="fas fa-arrow-left"></i> Tiếp tục mua
            </button>
            <button class="btn btn-primary" @click="goToCartPage">
              <i class="fas fa-shopping-cart"></i> Xem giỏ hàng
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { formatPrice } from '../../utils/helpers'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  product: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['close', 'update-quantity'])

const router = useRouter()
const cartStore = useCartStore()

const localQuantity = ref(props.product.quantity || 1)

// Tính phần trăm giảm giá
const calculateDiscount = (originalPrice, discountedPrice) => {
  if (!originalPrice || !discountedPrice || originalPrice <= discountedPrice) return 0
  return Math.round(((originalPrice - discountedPrice) / originalPrice) * 100)
}

watch(() => props.product, (newProduct) => {
  localQuantity.value = newProduct.quantity || 1
}, { deep: true })

const close = () => {
  emit('close')
}

const continueShopping = () => {
  close()
}

const goToCartPage = () => {
  close()
  cartStore.fetchCart().then(() => {
    router.push('/cart')
  })
}

const increaseQuantity = () => {
  const maxStock = props.product.stock || 99
  if (localQuantity.value < maxStock) {
    localQuantity.value++
    emit('update-quantity', localQuantity.value)
  }
}

const decreaseQuantity = () => {
  if (localQuantity.value > 1) {
    localQuantity.value--
    emit('update-quantity', localQuantity.value)
  }
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
</script>

<style scoped>
.cart-popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(5px);
  animation: fadeIn 0.3s ease;
}

.cart-popup {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 450px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

.popup-header {
  padding: 20px 25px;
  background: linear-gradient(135deg, #28a745, #20c997);
  border-radius: 16px 16px 0 0;
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.success-icon i {
  font-size: 30px;
  color: white;
}

.popup-header h3 {
  color: white;
  margin: 0;
  font-size: 18px;
  flex: 1;
}

.close-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.popup-body {
  padding: 20px 25px;
}

.product-info {
  display: flex;
  gap: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
  margin-bottom: 15px;
}

.product-image {
  width: 80px;
  height: 80px;
  background: #f5f5f5;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
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

.product-details h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #2c3e50;
}

.product-variant {
  font-size: 13px;
  color: #999;
  margin: 0 0 5px 0;
}

.product-price-wrapper {
  margin: 0 0 10px 0;
}

.product-price {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.discounted-price {
  font-size: 18px;
  font-weight: 700;
  color: #e63946;
}

.discount-badge {
  display: inline-block;
  background: #e63946;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.quantity-stock {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.quantity-control {
  display: flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
}

.quantity-control button {
  width: 30px;
  height: 30px;
  background: white;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 12px;
  color: #333;
}

.quantity-control button:hover:not(:disabled) {
  background: #e63946;
  color: white;
}

.quantity-control button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.quantity-control span {
  width: 30px;
  text-align: center;
  font-weight: 600;
  font-size: 14px;
}

.stock-info {
  font-size: 13px;
  color: #666;
}

.stock-info i {
  margin-right: 4px;
}

.stock-info.low-stock {
  color: #e63946;
  font-weight: 500;
}

.cart-summary {
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #666;
  font-size: 14px;
}

.summary-row.discount {
  color: #28a745;
}

.summary-row.total {
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 2px solid #eee;
}

.popup-footer {
  padding: 15px 25px 25px;
  display: flex;
  gap: 12px;
}

.popup-footer .btn {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  border: none;
}

.btn-outline {
  background: transparent;
  color: #666;
  border: 2px solid #ddd;
}

.btn-outline:hover {
  background: #f5f5f5;
  border-color: #ccc;
}

.btn-primary {
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.3);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(50px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.popup-enter-active,
.popup-leave-active {
  transition: all 0.3s ease;
}

.popup-enter-from,
.popup-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

@media (max-width: 480px) {
  .cart-popup {
    width: 95%;
    margin: 10px;
  }
  
  .popup-footer {
    flex-direction: column;
  }
  
  .popup-footer .btn {
    width: 100%;
  }
  
  .quantity-stock {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>