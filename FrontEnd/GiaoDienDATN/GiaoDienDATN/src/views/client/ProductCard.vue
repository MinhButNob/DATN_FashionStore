<template>
  <div class="product-card" @click="goToDetail">
    <div class="product-image">
      <img 
        v-if="product.mainImageUrl" 
        :src="product.mainImageUrl" 
        :alt="product.productName" 
        class="card-img" 
      />
      <i v-else class="fas fa-tshirt"></i>
      <div v-if="product.discountPercent" class="product-tag">
        -{{ product.discountPercent }}%
      </div>
      <div class="product-wishlist" @click.stop="toggleWishlist">
        <i :class="[isInWishlist ? 'fas' : 'far', 'fa-heart']"></i>
      </div>
    </div>

    <div class="product-info">
      <div class="product-category">{{ product.categoryName || 'Sản phẩm' }}</div>
      <h3 class="product-name">{{ product.productName }}</h3>

      <div class="product-price">
        <span class="current-price">{{ formatPrice(product.minPrice) }}</span>
        <span v-if="product.maxPrice !== product.minPrice" class="price-range">
          - {{ formatPrice(product.maxPrice) }}
        </span>
      </div>

      <div class="product-colors" v-if="product.colorOptions && product.colorOptions.length > 0">
        <span 
          v-for="color in product.colorOptions.slice(0, 4)" 
          :key="color"
          class="color-dot"
          :style="{ backgroundColor: getColorHex(color) }"
          :title="color"
        ></span>
        <span v-if="product.colorOptions.length > 4" class="more-colors">
          +{{ product.colorOptions.length - 4 }}
        </span>
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

const formatPrice = (price) => {
  if (!price) return '0đ'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
}

const getColorHex = (colorName) => {
  const colorMap = {
    'Đỏ': '#FF0000',
    'Đen': '#000000',
    'Trắng': '#FFFFFF',
    'Xanh': '#0000FF',
    'Navy': '#1B2951',
    'Vàng': '#FFFF00',
    'Hồng': '#FFC0CB'
  }
  return colorMap[colorName] || '#CCCCCC'
}

const isInWishlist = computed(() => false)

const goToDetail = () => {
  // ✅ Lấy productId từ props
  const productId = props.product.productId
  console.log('🟡 Chuyển đến chi tiết sản phẩm ID:', productId)
  console.log('🟡 Product data:', props.product)
  
  if (productId && productId !== 'undefined') {
    router.push(`/product/${productId}`)
  } else {
    console.error('❌ Không có productId hợp lệ:', props.product)
    showNotification('Không thể xem chi tiết sản phẩm!', 'error')
  }
}

const addToCart = (event) => {
  event.stopPropagation()
  cartStore.addToCart({
    productId: props.product.productId,
    productName: props.product.productName,
    price: props.product.minPrice,
    quantity: 1,
    image: props.product.mainImageUrl
  })
  showNotification('Đã thêm sản phẩm vào giỏ hàng!', 'success')
}

const toggleWishlist = (event) => {
  event.stopPropagation()
  if (!authStore.isAuthenticated) {
    window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'login' }))
    return
  }
  showNotification('Chức năng đang phát triển', 'info')
}
</script>

<style scoped>
/* Giữ nguyên style cũ */
.product-card {
  background: white;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.product-image {
  height: 280px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.product-image i {
  font-size: 80px;
  color: #ccc;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-card:hover .card-img {
  transform: scale(1.05);
}

.product-tag {
  position: absolute;
  top: 15px;
  left: 15px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
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
  transition: all 0.3s;
}

.product-wishlist:hover {
  background: #e63946;
  color: white;
}

.product-info {
  padding: 20px;
}

.product-category {
  color: #e63946;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  margin-bottom: 8px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 10px;
  line-height: 1.4;
  height: 44px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price {
  margin-bottom: 12px;
}

.current-price {
  font-size: 18px;
  font-weight: 700;
  color: #e63946;
}

.price-range {
  font-size: 14px;
  color: #999;
}

.product-colors {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.color-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid #ddd;
}

.more-colors {
  font-size: 12px;
  color: #999;
  line-height: 20px;
}

.add-to-cart {
  width: 100%;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border: none;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.add-to-cart:hover {
  background: linear-gradient(135deg, #c82333, #e63946);
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .product-image {
    height: 220px;
  }
  
  .product-name {
    font-size: 14px;
    height: 38px;
  }
  
  .current-price {
    font-size: 16px;
  }
}
</style>