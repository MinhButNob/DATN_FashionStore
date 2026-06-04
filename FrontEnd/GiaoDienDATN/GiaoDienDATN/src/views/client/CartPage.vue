<template>
  <div class="cart-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <h1 class="page-title">GIỎ HÀNG</h1>

        <!-- Empty Cart -->
        <div v-if="cartStore.items.length === 0" class="empty-cart">
          <i class="fas fa-shopping-cart"></i>
          <h2>Giỏ hàng trống</h2>
          <p>Bạn chưa có sản phẩm nào trong giỏ hàng</p>
          <router-link to="/shop" class="btn btn-primary">
            Tiếp tục mua sắm
          </router-link>
        </div>

        <!-- Cart Content -->
        <div v-else class="cart-content">
          <div class="cart-items">
            <div class="cart-header">
              <div class="col-product">Sản phẩm</div>
              <div class="col-price">Đơn giá</div>
              <div class="col-quantity">Số lượng</div>
              <div class="col-total">Thành tiền</div>
              <div class="col-action"></div>
            </div>

            <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
              <div class="col-product">
                <div class="product-info">
                  <div class="product-image" @click="goToProduct(item.id)">
                    <i :class="['fas', `fa-${item.image}`]"></i>
                  </div>
                  <div class="product-details">
                    <h3 @click="goToProduct(item.id)">{{ item.name }}</h3>
                    <div v-if="item.size || item.color" class="product-variants">
                      <span v-if="item.size">Size: {{ item.size }}</span>
                      <span v-if="item.color">Màu: {{ item.color }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="col-price">
                {{ formatPrice(item.price) }}
              </div>

              <div class="col-quantity">
                <div class="quantity-control">
                  <button 
                    @click="updateQuantity(item, -1)"
                    :disabled="item.quantity <= 1"
                  >
                    <i class="fas fa-minus"></i>
                  </button>
                  <span>{{ item.quantity }}</span>
                  <button 
                    @click="updateQuantity(item, 1)"
                    :disabled="item.quantity >= 99"
                  >
                    <i class="fas fa-plus"></i>
                  </button>
                </div>
              </div>

              <div class="col-total">
                {{ formatPrice(item.price * item.quantity) }}
              </div>

              <div class="col-action">
                <button class="remove-btn" @click="removeItem(item)">
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </div>

            <!-- Coupon -->
            <div class="cart-coupon">
              <input 
                type="text" 
                v-model="couponCode" 
                placeholder="Mã giảm giá"
              >
              <button 
                class="btn btn-outline"
                @click="applyCoupon"
                :disabled="!couponCode"
              >
                Áp dụng
              </button>
            </div>
          </div>

          <!-- Cart Summary -->
          <div class="cart-summary">
            <h2>Tổng giỏ hàng</h2>

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

            <div class="summary-row total">
              <span>Tổng cộng:</span>
              <span>{{ formatPrice(cartStore.total - discount) }}</span>
            </div>

            <router-link 
              to="/checkout" 
              class="btn btn-primary checkout-btn"
              :class="{ disabled: cartStore.items.length === 0 }"
            >
              TIẾN HÀNH THANH TOÁN
            </router-link>

            <router-link to="/shop" class="continue-shopping">
              <i class="fas fa-arrow-left"></i>
              Tiếp tục mua sắm
            </router-link>
          </div>
        </div>

        <!-- Suggested Products -->
        <div v-if="suggestedProducts.length > 0" class="suggested-products">
          <h2 class="section-title">CÓ THỂ BẠN CŨNG THÍCH</h2>
          <div class="product-grid">
            <ProductCard
              v-for="product in suggestedProducts"
              :key="product.id"
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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { useProductStore } from '../../stores/productStore'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import ProductCard from '../../components/client/ProductCard.vue'
import { formatPrice } from '../../utils/helpers'

const router = useRouter()
const cartStore = useCartStore()
const productStore = useProductStore()
const { showNotification } = useNotification()

const couponCode = ref('')
const discount = ref(0)

const suggestedProducts = computed(() => {
  const cartIds = cartStore.items.map(item => item.id)
  return productStore.products
    .filter(p => !cartIds.includes(p.id))
    .slice(0, 4)
})

const updateQuantity = (item, change) => {
  const newQuantity = item.quantity + change
  if (newQuantity >= 1 && newQuantity <= 99) {
    cartStore.updateQuantity(item.id, newQuantity, item.size, item.color)
  }
}

const removeItem = (item) => {
  if (confirm('Bạn có chắc muốn xóa sản phẩm này khỏi giỏ hàng?')) {
    cartStore.removeFromCart(item.id, item.size, item.color)
    showNotification('Đã xóa sản phẩm khỏi giỏ hàng', 'info')
  }
}

const applyCoupon = () => {
  if (couponCode.value.toUpperCase() === 'SALE20') {
    discount.value = cartStore.subtotal * 0.2
    showNotification('Áp dụng mã giảm giá thành công!', 'success')
  } else if (couponCode.value.toUpperCase() === 'FREESHIP') {
    discount.value = cartStore.shippingFee
    showNotification('Áp dụng mã giảm giá thành công!', 'success')
  } else {
    showNotification('Mã giảm giá không hợp lệ!', 'error')
  }
}

const goToProduct = (id) => {
  router.push(`/product/${id}`)
}
</script>

<style scoped>
.cart-page {
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

/* Empty Cart */
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

/* Cart Content */
.cart-content {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: 30px;
}

/* Cart Items */
.cart-items {
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
  overflow: hidden;
}

.cart-header {
  display: grid;
  grid-template-columns: 3fr 1fr 1fr 1fr 50px;
  padding: 15px 20px;
  background: var(--light-gray);
  font-weight: 600;
  color: var(--dark-gray);
  border-bottom: 2px solid var(--border-color);
}

.cart-item {
  display: grid;
  grid-template-columns: 3fr 1fr 1fr 1fr 50px;
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
  align-items: center;
}

.cart-item:last-child {
  border-bottom: none;
}

/* Product Column */
.product-info {
  display: flex;
  gap: 15px;
  align-items: center;
}

.product-image {
  width: 80px;
  height: 80px;
  background: var(--light-gray);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.3s;
}

.product-image:hover {
  transform: scale(1.05);
}

.product-image i {
  font-size: 40px;
  color: var(--medium-gray);
}

.product-details {
  flex: 1;
}

.product-details h3 {
  color: var(--dark-gray);
  margin-bottom: 5px;
  font-size: 16px;
  cursor: pointer;
  transition: color 0.3s;
}

.product-details h3:hover {
  color: var(--primary-red);
}

.product-variants {
  display: flex;
  gap: 10px;
  font-size: 13px;
  color: var(--medium-gray);
}

.product-variants span {
  background: var(--light-gray);
  padding: 3px 8px;
  border-radius: 3px;
}

/* Quantity Control */
.quantity-control {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
}

.quantity-control button {
  width: 35px;
  height: 35px;
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 600;
}

.quantity-control button:hover:not(:disabled) {
  background: var(--primary-red);
  color: white;
}

.quantity-control button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quantity-control span {
  width: 40px;
  text-align: center;
  font-weight: 600;
}

/* Remove Button */
.remove-btn {
  width: 35px;
  height: 35px;
  background: none;
  border: none;
  color: var(--medium-gray);
  cursor: pointer;
  transition: all 0.3s;
  font-size: 16px;
}

.remove-btn:hover {
  color: var(--primary-red);
  transform: scale(1.1);
}

/* Coupon */
.cart-coupon {
  padding: 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  gap: 10px;
}

.cart-coupon input {
  flex: 1;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
}

.cart-coupon input:focus {
  border-color: var(--primary-red);
}

/* Cart Summary */
.cart-summary {
  background: white;
  border-radius: 15px;
  padding: 25px;
  box-shadow: var(--shadow);
  height: fit-content;
}

.cart-summary h2 {
  color: var(--dark-gray);
  margin-bottom: 20px;
  font-size: 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  color: var(--medium-gray);
}

.summary-row.discount {
  color: #28a745;
}

.summary-row.total {
  font-size: 20px;
  font-weight: 700;
  color: var(--dark-gray);
  margin-top: 15px;
  padding-top: 15px;
  border-top: 2px solid var(--border-color);
}

.checkout-btn {
  width: 100%;
  margin: 20px 0 15px;
  padding: 15px;
  font-size: 16px;
}

.checkout-btn.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.continue-shopping {
  display: block;
  text-align: center;
  color: var(--medium-gray);
  text-decoration: none;
  transition: color 0.3s;
}

.continue-shopping:hover {
  color: var(--primary-red);
}

/* Suggested Products */
.suggested-products {
  margin-top: 50px;
}

.section-title {
  font-size: 24px;
  color: var(--dark-gray);
  margin-bottom: 20px;
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
  background: var(--primary-red);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

/* Responsive */
@media (max-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .cart-content {
    grid-template-columns: 1fr;
  }

  .cart-header {
    display: none;
  }

  .cart-item {
    grid-template-columns: 1fr;
    gap: 15px;
    position: relative;
    padding: 20px;
  }

  .product-info {
    grid-column: 1 / -1;
  }

  .col-price::before {
    content: 'Đơn giá: ';
    font-weight: normal;
    color: var(--medium-gray);
  }

  .col-total::before {
    content: 'Thành tiền: ';
    font-weight: normal;
    color: var(--medium-gray);
  }

  .col-price,
  .col-total {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .col-action {
    position: absolute;
    top: 20px;
    right: 20px;
  }

  .cart-coupon {
    flex-direction: column;
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
}
</style>