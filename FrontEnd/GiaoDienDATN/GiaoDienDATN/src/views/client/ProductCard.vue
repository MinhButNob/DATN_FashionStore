<template>
  <div class="product-card" @click="goToDetail">
    <div class="product-image">
      <i :class="['fas', `fa-${product.image}`]"></i>
      <div v-if="product.tag" class="product-tag">{{ product.tag }}</div>
      <div class="product-wishlist" @click.stop="toggleWishlist">
        <i :class="[isInWishlist ? 'fas' : 'far', 'fa-heart']"></i>
      </div>
    </div>

    <div class="product-info">
      <div class="product-category">{{ getCategoryName(product.category) }}</div>
      <h3 class="product-name">{{ product.name }}</h3>

      <div class="product-price">
        <span class="current-price">{{ formatPrice(product.price) }}</span>
        <span v-if="product.oldPrice" class="old-price">
          {{ formatPrice(product.oldPrice) }}
        </span>
      </div>

      <div class="product-rating">
        <div class="stars">
          <i
            v-for="(star, index) in starRating"
            :key="index"
            :class="star"
          ></i>
        </div>
        <span>({{ product.sold }} đã bán)</span>
      </div>

      <button class="add-to-cart" @click.stop="addToCart">
        <i class="fas fa-shopping-cart"></i> Thêm vào giỏ
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { useAuthStore } from '../../stores/authStore'
import { useNotification } from '../../composables/useNotification'
import { formatPrice, getCategoryName, getStarRating } from '../../utils/helpers'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const router = useRouter()
const cartStore = useCartStore()
const authStore = useAuthStore()
const { showNotification } = useNotification()

const starRating = computed(() => {
  return getStarRating(props.product.rating)
})

const isInWishlist = computed(() => {
  // Check if product is in wishlist
  return false
})

const goToDetail = () => {
  router.push(`/product/${props.product.id}`)
}

const addToCart = (event) => {
  event.stopPropagation()
  cartStore.addToCart(props.product)
  showNotification('Đã thêm sản phẩm vào giỏ hàng!', 'success')
}

const toggleWishlist = (event) => {
  event.stopPropagation()
  
  if (!authStore.isAuthenticated) {
    window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'login' }))
    return
  }
  
  // Toggle wishlist logic here
  if (isInWishlist.value) {
    showNotification('Đã xóa khỏi danh sách yêu thích!', 'info')
  } else {
    showNotification('Đã thêm vào danh sách yêu thích!', 'success')
  }
}
</script>

<style scoped>
.product-card {
  background: white;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: var(--shadow);
  transition: var(--transition);
  cursor: pointer;
  position: relative;
}

.product-card:hover {
  transform: translateY(-10px);
  box-shadow: var(--shadow-hover);
}

.product-image {
  height: 300px;
  background: linear-gradient(135deg, var(--light-gray), #e9ecef);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.product-image i {
  font-size: 100px;
  color: var(--medium-gray);
  transition: var(--transition);
}

.product-card:hover .product-image i {
  transform: scale(1.1) rotate(5deg);
  color: var(--primary-red);
}

.product-tag {
  position: absolute;
  top: 15px;
  left: 15px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  padding: 5px 15px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
  z-index: 1;
}

.product-wishlist {
  position: absolute;
  top: 15px;
  right: 15px;
  width: 35px;
  height: 35px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--transition);
  z-index: 1;
}

.product-wishlist:hover {
  background: var(--primary-red);
  color: white;
  transform: scale(1.1);
}

.product-info {
  padding: 20px;
}

.product-category {
  color: var(--primary-red);
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.product-name {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 10px;
  color: var(--dark-gray);
  line-height: 1.4;
  height: 44px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.current-price {
  color: var(--primary-red);
  font-weight: 800;
  font-size: 20px;
}

.old-price {
  color: var(--medium-gray);
  font-size: 14px;
  text-decoration: line-through;
}

.product-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.stars {
  color: #ffc107;
  font-size: 13px;
}

.product-rating span {
  color: var(--medium-gray);
  font-size: 13px;
}

.add-to-cart {
  width: 100%;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  border: none;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: var(--transition);
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.add-to-cart:hover {
  background: linear-gradient(135deg, var(--dark-red), var(--primary-red));
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.3);
}
</style>