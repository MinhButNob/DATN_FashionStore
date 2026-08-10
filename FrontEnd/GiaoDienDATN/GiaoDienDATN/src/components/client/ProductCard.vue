<template>
  <div class="product-card" @click="goToDetail">
    <!-- Image & Top Badges Container -->
    <div class="product-image-wrapper">
      <img 
        v-if="imageUrl" 
        :src="imageUrl" 
        :alt="product.productName || product.name" 
        class="card-img" 
        @error="handleImageError"
      />
      <div v-else class="no-image">
        <i class="fas fa-tshirt"></i>
      </div>

      <!-- Top-Left Discount Badge -->
      <div v-if="discountPercent > 0" class="discount-badge">
        -{{ discountPercent }}%
      </div>
      <div v-else-if="badgeText" class="custom-badge">
        {{ badgeText }}
      </div>

      <!-- Top-Right Wishlist Heart Button -->
      <button class="product-wishlist" @click.stop="handleToggleWishlist" :title="isInWishlist ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'">
        <i :class="[isInWishlist ? 'fas active' : 'far', 'fa-heart']"></i>
      </button>
    </div>

    <!-- Product Info Section -->
    <div class="product-info">
      <!-- Option Tags (Màu sắc & Kích thước) -->
      <div class="product-options">
        <span class="option-tag" v-if="colorCount > 0">
          +{{ colorCount }} Màu sắc
        </span>
        <span class="option-tag" v-if="sizeCount > 0">
          +{{ sizeCount }} Kích thước
        </span>
        <span class="option-tag category-tag" v-if="product.categoryName && colorCount === 0 && sizeCount === 0">
          {{ product.categoryName }}
        </span>
      </div>

      <!-- Product Name -->
      <h3 class="product-name" :title="product.productName || product.name">
        {{ product.productName || product.name }}
      </h3>

      <!-- Rating Display (nếu có) -->
      <div class="product-rating" v-if="displayRating > 0">
        <div class="stars">
          <i v-for="i in 5" :key="i" class="fas fa-star" :class="{ 'active-star': i <= Math.round(displayRating) }"></i>
        </div>
        <span class="rating-text">{{ displayRating.toFixed(1) }}</span>
        <span class="review-count" v-if="displayReviewCount > 0">({{ displayReviewCount }})</span>
      </div>

      <!-- Product Price Section -->
      <div class="product-price">
        <span class="current-price">{{ formatPriceVND(displayCurrentPrice) }}</span>
        <span v-if="hasDiscount && displayOriginalPrice > displayCurrentPrice" class="original-price">
          {{ formatPriceVND(displayOriginalPrice) }}
        </span>
      </div>

      <!-- Product Stock Status -->
      <div class="product-stock" :class="{ 'out-of-stock': isOutOfStock }">
        <i class="fas" :class="!isOutOfStock ? 'fa-check-circle' : 'fa-times-circle'"></i>
        <span>{{ !isOutOfStock ? `Còn ${stockCount} sản phẩm` : 'Hết hàng' }}</span>
      </div>

      <!-- Add to Cart Action Button -->
      <button 
        class="add-to-cart-btn" 
        @click.stop="addToCart" 
        :disabled="isOutOfStock"
      >
        <i class="fas fa-shopping-cart"></i>
        <span>{{ !isOutOfStock ? 'Thêm vào giỏ' : 'Hết hàng' }}</span>
      </button>
    </div>
  </div>

  <CartPopup 
    :visible="showCartPopup" 
    :product="lastAddedProduct"
    @close="showCartPopup = false"
  />
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { useAuthStore } from '../../stores/authStore'
import { useWishlistStore } from '../../stores/wishlistStore'
import { useNotification } from '../../composables/useNotification'
import CartPopup from './CartPopup.vue'

const props = defineProps({
  product: {
    type: Object,
    required: true
  },
  badgeText: {
    type: String,
    default: ''
  },
  rating: {
    type: Number,
    default: 0
  },
  reviewCount: {
    type: Number,
    default: 0
  }
})

const router = useRouter()
const cartStore = useCartStore()
const authStore = useAuthStore()
const wishlistStore = useWishlistStore()
const { showNotification } = useNotification()

const showCartPopup = ref(false)
const lastAddedProduct = ref({})

// Calculated Image URL
const imageUrl = computed(() => {
  const p = props.product
  return p.mainImageUrl || p.imageUrl || p.image || ''
})

// Calculated Color Count
const colorCount = computed(() => {
  const p = props.product
  if (Array.isArray(p.colorOptions) && p.colorOptions.length > 0) {
    return p.colorOptions.length
  }
  if (typeof p.colorCount === 'number') return p.colorCount
  if (Array.isArray(p.variants)) {
    const colors = new Set(p.variants.map(v => v.colorName).filter(Boolean))
    return colors.size
  }
  return 0
})

// Calculated Size Count
const sizeCount = computed(() => {
  const p = props.product
  if (Array.isArray(p.sizeIds) && p.sizeIds.length > 0) {
    return p.sizeIds.length
  }
  if (Array.isArray(p.sizeOptions) && p.sizeOptions.length > 0) {
    return p.sizeOptions.length
  }
  if (typeof p.sizeCount === 'number') return p.sizeCount
  if (Array.isArray(p.variants)) {
    const sizes = new Set(p.variants.map(v => v.sizeName || v.sizeId).filter(Boolean))
    return sizes.size
  }
  return 0
})

// Current Price
const displayCurrentPrice = computed(() => {
  const p = props.product
  if (p.discountedPrice && Number(p.discountedPrice) > 0 && Number(p.discountedPrice) < Number(p.minPrice || p.price)) {
    return Number(p.discountedPrice)
  }
  return Number(p.minPrice || p.price || 0)
})

// Original Price
const displayOriginalPrice = computed(() => {
  const p = props.product
  return Number(p.minPrice || p.price || 0)
})

// Discount Percent
const discountPercent = computed(() => {
  const p = props.product
  if (p.discountPercent && p.discountPercent > 0) {
    return p.discountPercent
  }
  const original = displayOriginalPrice.value
  const current = displayCurrentPrice.value
  if (original > current && original > 0) {
    return Math.round(((original - current) / original) * 100)
  }
  return 0
})

const hasDiscount = computed(() => discountPercent.value > 0)

// Stock Count
const stockCount = computed(() => {
  const p = props.product
  if (p.totalStock !== undefined && p.totalStock !== null) return Number(p.totalStock)
  if (p.stock !== undefined && p.stock !== null) return Number(p.stock)
  if (Array.isArray(p.variants)) {
    return p.variants.reduce((acc, v) => acc + Number(v.quantity || v.stock || 0), 0)
  }
  return 0
})

const isOutOfStock = computed(() => stockCount.value <= 0)

// Ratings
const displayRating = computed(() => {
  if (props.rating > 0) return props.rating
  if (props.product.averageRating > 0) return props.product.averageRating
  if (props.product.rating > 0) return props.product.rating
  return 0
})

const displayReviewCount = computed(() => {
  if (props.reviewCount > 0) return props.reviewCount
  if (props.product.reviewCount > 0) return props.product.reviewCount
  return 0
})

// Wishlist State
const isInWishlist = computed(() => {
  const pId = props.product.productDetailId || props.product.id || props.product.productId
  return wishlistStore.isInWishlist(pId)
})

const formatPriceVND = (price) => {
  if (price === undefined || price === null) return '0đ'
  let numPrice = 0
  if (typeof price === 'number') {
    numPrice = price
  } else if (typeof price === 'string') {
    numPrice = parseFloat(price.replace(/[^0-9.-]/g, ''))
  } else {
    numPrice = Number(price)
  }
  if (isNaN(numPrice) || numPrice === 0) return '0đ'
  return numPrice.toLocaleString('vi-VN') + 'đ'
}

const handleImageError = (event) => {
  event.target.style.display = 'none'
  const noImageDiv = event.target.parentElement.querySelector('.no-image')
  if (noImageDiv) noImageDiv.style.display = 'flex'
}

const goToDetail = () => {
  const productId = props.product.productId || props.product.id
  if (productId && productId !== 'undefined') {
    router.push(`/product/${productId}`)
  } else {
    showNotification('Không thể xem chi tiết sản phẩm!', 'error')
  }
}

const addToCart = async (event) => {
  event.stopPropagation()

  if (!authStore.isAuthenticated) {
    window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'login' }))
    showNotification('Vui lòng đăng nhập để thêm vào giỏ hàng', 'info')
    return
  }
  
  if (isOutOfStock.value) {
    showNotification('Sản phẩm đã hết hàng!', 'error')
    return
  }
  
  const price = displayCurrentPrice.value
  let productDetailId = props.product.productDetailId || props.product.id || props.product.productId
  let size = null
  let color = null
  
  if (props.product.variants && props.product.variants.length > 0) {
    const firstVariant = props.product.variants[0]
    size = firstVariant.sizeName || null
    color = firstVariant.colorName || null
    productDetailId = firstVariant.productDetailId || productDetailId
  }
  
  const productData = {
    id: productDetailId,
    productId: props.product.productId || props.product.id,
    productName: props.product.productName || props.product.name,
    name: props.product.productName || props.product.name,
    price: price,
    quantity: 1,
    image: imageUrl.value,
    imageUrl: imageUrl.value,
    size: size,
    color: color,
    stock: stockCount.value
  }
  
  try {
    await cartStore.addToCart(productData)
    
    lastAddedProduct.value = {
      productDetailId: productDetailId,
      name: props.product.productName || props.product.name,
      price: price,
      quantity: 1,
      image: imageUrl.value,
      size: size,
      color: color,
      stock: stockCount.value
    }
    
    showCartPopup.value = true
  } catch (error) {
    console.error('Add to cart error:', error)
    showNotification('Thêm vào giỏ thất bại!', 'error')
  }
}

const handleToggleWishlist = async (event) => {
  event.stopPropagation()
  if (!authStore.isAuthenticated) {
    window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'login' }))
    showNotification('Vui lòng đăng nhập để sử dụng danh sách yêu thích', 'info')
    return
  }
  const targetId = props.product.productDetailId || props.product.id || props.product.productId
  const success = await wishlistStore.toggleWishlist(targetId)
  if (success) {
    showNotification(isInWishlist.value ? 'Đã thêm vào yêu thích' : 'Đã xóa khỏi yêu thích', 'success')
  }
}
</script>

<style scoped>
/* Card Base Styling */
.product-card {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1ece2;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  border-color: #ff4d4f;
}

/* Image Container - Slate / Blue-Grey Background (Shorter Height) */
.product-image-wrapper {
  position: relative;
  height: 185px;
  background: #f0f4f8; /* Nền xanh xám sản phẩm */
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.product-card:hover .card-img {
  transform: scale(1.05);
}

.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f4f8;
  color: #94a3b8;
}

.no-image i {
  font-size: 48px;
}

/* Discount Badge - Red Pill Top Left */
.discount-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #ff4d4f;
  color: #ffffff;
  padding: 3px 8px;
  border-radius: 14px;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.3px;
  z-index: 2;
  box-shadow: 0 2px 6px rgba(255, 77, 79, 0.25);
}

.custom-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #1e293b;
  color: #ffffff;
  padding: 3px 8px;
  border-radius: 14px;
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  z-index: 2;
}

/* Wishlist Heart Button - White Circle Top Right */
.product-wishlist {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  background: #ffffff;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  z-index: 2;
  color: #64748b;
  font-size: 14px;
}

.product-wishlist:hover {
  transform: scale(1.08);
  color: #ff4d4f;
  background: #ffffff;
}

.product-wishlist i.active {
  color: #ff4d4f;
}

/* Product Info Container (Compact Padding) */
.product-info {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  background: #ffffff;
}

/* Option Tags (Pills) */
.product-options {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.option-tag {
  font-size: 11px;
  color: #475569;
  background: #f1f5f9;
  padding: 2px 7px;
  border-radius: 4px;
  font-weight: 500;
}

.category-tag {
  color: #ff4d4f;
  background: #fff0f0;
}

/* Product Title */
.product-name {
  font-size: 13.5px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 6px;
  line-height: 1.3;
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* Ratings */
.product-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}

.stars {
  display: flex;
  gap: 2px;
}

.stars i {
  font-size: 10px;
  color: #cbd5e1;
}

.stars i.active-star {
  color: #f59e0b;
}

.rating-text {
  font-size: 11px;
  font-weight: 700;
  color: #1e293b;
}

.review-count {
  font-size: 10px;
  color: #94a3b8;
}

/* Price Section */
.product-price {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.current-price {
  font-size: 15.5px;
  font-weight: 800;
  color: #ff4d4f;
}

.original-price {
  font-size: 12px;
  color: #94a3b8;
  text-decoration: line-through;
  font-weight: 500;
}

/* Stock Status */
.product-stock {
  font-size: 11.5px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #16a34a;
  display: flex;
  align-items: center;
  gap: 4px;
}

.product-stock.out-of-stock {
  color: #dc2626;
}

/* Add to Cart Button - Red Full Width Compact */
.add-to-cart-btn {
  width: 100%;
  background: #ff4d4f;
  color: #ffffff;
  border: none;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s ease;
  font-weight: 700;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: auto;
  box-shadow: 0 3px 8px rgba(255, 77, 79, 0.2);
}

.add-to-cart-btn:hover:not(:disabled) {
  background: #e0383a;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

.add-to-cart-btn:disabled {
  background: #cbd5e1;
  color: #64748b;
  cursor: not-allowed;
  box-shadow: none;
}

@media (max-width: 768px) {
  .product-image-wrapper {
    height: 160px;
  }
  .product-name {
    font-size: 12.5px;
    height: 32px;
  }
  .current-price {
    font-size: 14px;
  }
  .add-to-cart-btn {
    padding: 7px;
    font-size: 12px;
  }
}
</style>