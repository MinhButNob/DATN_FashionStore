<template>
  <div class="product-card" @click="goToDetail">
    <div class="product-image">
      <img 
        v-if="product.mainImageUrl" 
        :src="product.mainImageUrl" 
        :alt="product.productName" 
        class="card-img" 
        @error="handleImageError"
      />
      <div v-else class="no-image">
        <i class="fas fa-tshirt"></i>
      </div>
      <div class="product-wishlist" @click.stop="toggleWishlist">
        <i :class="[isInWishlist ? 'fas' : 'far', 'fa-heart']"></i>
      </div>
    </div>

    <div class="product-info">
      <div class="product-category">{{ product.categoryName || 'Sản phẩm' }}</div>
      <h3 class="product-name">{{ product.productName }}</h3>

      <div class="product-price">
        <span class="current-price">{{ formatPriceVND(product.minPrice) }}</span>
        <span v-if="product.maxPrice && product.maxPrice !== product.minPrice" class="price-range">
          - {{ formatPriceVND(product.maxPrice) }}
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

      <div class="product-stock" :class="{ 'out-of-stock': (product.totalStock || 0) === 0 }">
        <i class="fas" :class="(product.totalStock || 0) > 0 ? 'fa-check-circle' : 'fa-times-circle'"></i>
        {{ (product.totalStock || 0) > 0 ? `Còn ${product.totalStock} sản phẩm` : 'Hết hàng' }}
      </div>

      <button class="add-to-cart" @click.stop="addToCart" :disabled="(product.totalStock || 0) === 0">
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

// ✅ HÀM FORMAT GIÁ ĐÚNG - QUAN TRỌNG
const formatPriceVND = (price) => {
  // Nếu price là undefined hoặc null
  if (price === undefined || price === null) {
    return '0₫'
  }
  
  // Chuyển đổi sang số
  let numPrice = 0
  
  if (typeof price === 'number') {
    numPrice = price
  } else if (typeof price === 'string') {
    numPrice = parseFloat(price.replace(/[^0-9.-]/g, ''))
  } else {
    numPrice = Number(price)
  }
  
  // Kiểm tra NaN
  if (isNaN(numPrice) || numPrice === 0) {
    return '0₫'
  }
  
  // Format theo VND
  return numPrice.toLocaleString('vi-VN') + '₫'
}

const getColorHex = (colorName) => {
  const colorMap = {
    'Đỏ': '#FF0000',
    'Đen': '#000000',
    'Trắ': '#FFFFFF',
    'Trắng': '#FFFFFF',
    'Xanh': '#0000FF',
    'Navy': '#1B2951',
    'Vàng': '#FFFF00',
    'Hồng': '#FFC0CB',
    'Xám': '#808080',
    'Nâu': '#8B4513',
    'Tím': '#800080',
    'Cam': '#FFA500'
  }
  return colorMap[colorName] || '#CCCCCC'
}

const handleImageError = (event) => {
  event.target.style.display = 'none'
  const noImageDiv = event.target.parentElement.querySelector('.no-image')
  if (noImageDiv) noImageDiv.style.display = 'flex'
}

const isInWishlist = computed(() => false)

const goToDetail = () => {
  const productId = props.product.productId
  console.log('🟡 Chuyển đến chi tiết sản phẩm ID:', productId)
  
  if (productId && productId !== 'undefined') {
    router.push(`/product/${productId}`)
  } else {
    console.error('❌ Không có productId hợp lệ:', props.product)
    showNotification('Không thể xem chi tiết sản phẩm!', 'error')
  }
}

const addToCart = (event) => {
  event.stopPropagation()
  
  const stock = props.product.totalStock || 0
  if (stock === 0) {
    showNotification('Sản phẩm đã hết hàng!', 'error')
    return
  }
  
  const price = props.product.minPrice || 0
  const numericPrice = typeof price === 'number' ? price : Number(price)
  
  cartStore.addToCart({
    productId: props.product.productId,
    productName: props.product.productName,
    price: numericPrice,
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

// ✅ Log để debug
console.log('ProductCard component loaded')
console.log('Product data:', props.product)
console.log('minPrice:', props.product.minPrice)
console.log('Formatted price:', formatPriceVND(props.product.minPrice))
</script>

<style scoped>
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

.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.no-image i {
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
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
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
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.color-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid #ddd;
  cursor: pointer;
  transition: transform 0.2s;
}

.color-dot:hover {
  transform: scale(1.1);
}

.more-colors {
  font-size: 12px;
  color: #999;
  line-height: 22px;
}

.product-stock {
  font-size: 12px;
  margin-bottom: 15px;
  color: #28a745;
}

.product-stock.out-of-stock {
  color: #e63946;
}

.product-stock i {
  margin-right: 5px;
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

.add-to-cart:hover:not(:disabled) {
  background: linear-gradient(135deg, #c82333, #e63946);
  transform: translateY(-2px);
}

.add-to-cart:disabled {
  background: #ccc;
  cursor: not-allowed;
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