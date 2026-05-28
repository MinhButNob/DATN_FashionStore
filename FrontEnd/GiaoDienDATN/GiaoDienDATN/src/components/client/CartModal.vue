<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="cartStore.isCartOpen" class="modal" @click.self="closeCart">
        <div class="modal-content">
          <div class="modal-header">
            <h2>GIỎ HÀNG</h2>
            <button class="close-btn" @click="closeCart">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="modal-body">
            <div v-if="cartStore.items.length === 0" class="empty-cart">
              <i class="fas fa-shopping-cart"></i>
              <p>Giỏ hàng trống</p>
              <button class="btn btn-primary" @click="continueShopping">
                Tiếp tục mua sắm
              </button>
            </div>

            <div v-else class="cart-items">
              <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
                <div class="item-image" @click="goToProduct(item.id)">
                  <i :class="['fas', `fa-${item.image}`]"></i>
                </div>
                
                <div class="item-info">
                  <h4 @click="goToProduct(item.id)">{{ item.name }}</h4>
                  <p v-if="item.size || item.color" class="item-variant">
                    {{ item.size }} {{ item.color }}
                  </p>
                  <p class="item-price">{{ formatPrice(item.price) }}</p>
                </div>

                <div class="item-quantity">
                  <button 
                    class="quantity-btn"
                    @click="updateQuantity(item, -1)"
                    :disabled="item.quantity <= 1"
                  >
                    <i class="fas fa-minus"></i>
                  </button>
                  <span>{{ item.quantity }}</span>
                  <button 
                    class="quantity-btn"
                    @click="updateQuantity(item, 1)"
                    :disabled="item.quantity >= 99"
                  >
                    <i class="fas fa-plus"></i>
                  </button>
                </div>

                <div class="item-total">
                  {{ formatPrice(item.price * item.quantity) }}
                </div>

                <button class="remove-btn" @click="removeItem(item)">
                  <i class="fas fa-trash"></i>
                </button>
              </div>

              <div class="cart-summary">
                <div class="summary-row">
                  <span>Tạm tính:</span>
                  <span>{{ formatPrice(cartStore.subtotal) }}</span>
                </div>
                <div class="summary-row">
                  <span>Phí vận chuyển:</span>
                  <span>{{ formatPrice(cartStore.shippingFee) }}</span>
                </div>
                <div class="summary-row total">
                  <span>Tổng cộng:</span>
                  <span>{{ formatPrice(cartStore.total) }}</span>
                </div>
              </div>

              <div class="cart-actions">
                <button class="btn btn-outline" @click="continueShopping">
                  <i class="fas fa-arrow-left"></i> Tiếp tục mua
                </button>
                <button class="btn btn-primary" @click="goToCheckout">
                  Thanh toán <i class="fas fa-arrow-right"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { formatPrice } from '../../utils/helpers'

const router = useRouter()
const cartStore = useCartStore()

const closeCart = () => {
  cartStore.toggleCart()
}

const continueShopping = () => {
  closeCart()
  router.push('/shop')
}

const goToCheckout = () => {
  closeCart()
  router.push('/checkout')
}

const goToProduct = (id) => {
  closeCart()
  router.push(`/product/${id}`)
}

const updateQuantity = (item, change) => {
  const newQuantity = item.quantity + change
  if (newQuantity >= 1 && newQuantity <= 99) {
    cartStore.updateQuantity(item.id, newQuantity, item.size, item.color)
  }
}

const removeItem = (item) => {
  cartStore.removeFromCart(item.id, item.size, item.color)
}
</script>

<style scoped>
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  backdrop-filter: blur(5px);
}

.modal-content {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s;
}

.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
}

.modal-header h2 {
  color: var(--primary-red);
  font-size: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: var(--medium-gray);
  transition: all 0.3s;
}

.close-btn:hover {
  color: var(--primary-red);
  transform: rotate(90deg);
}

.modal-body {
  padding: 25px;
  overflow-y: auto;
  flex: 1;
}

.empty-cart {
  text-align: center;
  padding: 40px;
}

.empty-cart i {
  font-size: 60px;
  color: var(--medium-gray);
  margin-bottom: 15px;
}

.empty-cart p {
  color: var(--medium-gray);
  margin-bottom: 20px;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid var(--border-color);
  position: relative;
}

.cart-item:last-child {
  border-bottom: none;
}

.item-image {
  width: 60px;
  height: 60px;
  background: var(--light-gray);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.3s;
}

.item-image:hover {
  transform: scale(1.05);
}

.item-image i {
  font-size: 30px;
  color: var(--medium-gray);
}

.item-info {
  flex: 2;
}

.item-info h4 {
  color: var(--dark-gray);
  margin-bottom: 3px;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.3s;
}

.item-info h4:hover {
  color: var(--primary-red);
}

.item-variant {
  color: var(--medium-gray);
  font-size: 11px;
  margin-bottom: 3px;
}

.item-price {
  color: var(--primary-red);
  font-weight: 600;
  font-size: 13px;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity-btn {
  width: 25px;
  height: 25px;
  background: var(--light-gray);
  border: 1px solid var(--border-color);
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}

.quantity-btn:hover:not(:disabled) {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
}

.quantity-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.item-quantity span {
  min-width: 20px;
  text-align: center;
  font-weight: 600;
}

.item-total {
  font-weight: 600;
  color: var(--primary-red);
  min-width: 80px;
  text-align: right;
}

.remove-btn {
  background: none;
  border: none;
  color: var(--medium-gray);
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.remove-btn:hover {
  color: #dc3545;
  transform: scale(1.1);
}

.cart-summary {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 2px solid var(--border-color);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: var(--medium-gray);
}

.summary-row.total {
  font-size: 18px;
  font-weight: 700;
  color: var(--dark-gray);
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--border-color);
}

.cart-actions {
  display: flex;
  gap: 15px;
  margin-top: 20px;
}

.cart-actions button {
  flex: 1;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(50px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 480px) {
  .cart-item {
    flex-wrap: wrap;
  }

  .item-quantity {
    order: 2;
  }

  .item-total {
    order: 3;
    text-align: left;
  }

  .remove-btn {
    position: absolute;
    top: 15px;
    right: 0;
  }

  .cart-actions {
    flex-direction: column;
  }
}
</style>