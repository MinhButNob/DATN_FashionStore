<template>
  <div class="product-detail-page">
    <ClientHeader />

    <main>
      <div class="container">
        <!-- Breadcrumb -->
        <div class="breadcrumb">
          <router-link to="/">Trang chủ</router-link>
          <span>/</span>
          <router-link to="/shop">Sản phẩm</router-link>
          <span>/</span>
          <span class="current">{{ product?.productName || 'Chi tiết sản phẩm' }}</span>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="loading">
          <div class="spinner"></div>
          <p>Đang tải thông tin sản phẩm...</p>
        </div>

        <!-- Not Found -->
        <div v-else-if="!product" class="not-found">
          <i class="fas fa-box-open"></i>
          <h3>Không tìm thấy sản phẩm</h3>
          <p>Sản phẩm bạn đang tìm kiếm không tồn tại hoặc đã bị xóa.</p>
          <router-link to="/shop" class="btn-primary">Tiếp tục mua sắm</router-link>
        </div>

        <!-- Product Detail -->
        <div v-else>
          <!-- HÀNG TRÊN: ẢNH + THÔNG TIN SẢN PHẨM -->
          <div class="product-detail-top">
            <div class="product-gallery">
              <div class="main-image">
                <img v-if="mainImage" :src="mainImage" :alt="product.productName" class="detail-main-img" />
                <i v-else class="fas fa-tshirt"></i>
              </div>
              <div class="thumbnail-list" v-if="productImages.length > 1">
                <div 
                  v-for="(img, idx) in productImages" 
                  :key="idx"
                  class="thumbnail"
                  :class="{ active: currentImage === idx }"
                  @click="currentImage = idx"
                >
                  <img :src="img" :alt="'Hình ảnh ' + (idx + 1)" />
                </div>
              </div>
            </div>

            <div class="product-info">
              <div class="product-category">{{ product.categoryName }}</div>
              <h1 class="product-title">{{ product.productName }}</h1>

              <!-- Hiển thị giá theo biến thể được chọn -->
              <div class="product-price">
                <div v-if="selectedVariant">
                  <!-- Nếu có khuyến mãi -->
                  <div v-if="selectedVariant.discountedPrice && selectedVariant.discountedPrice < selectedVariant.price" class="price-wrapper">
                    <span class="original-price">{{ formatPrice(selectedVariant.price) }}</span>
                    <span class="discount-badge">-{{ calculateDiscountPercent(selectedVariant.price, selectedVariant.discountedPrice) }}%</span>
                    <div class="current-price-wrapper">
                      <span class="current-price">{{ formatPrice(selectedVariant.discountedPrice) }}</span>
                      <span class="price-label">(Giá sau khuyến mãi)</span>
                    </div>
                  </div>
                  <!-- Không có khuyến mãi -->
                  <div v-else>
                    <span class="current-price">{{ formatPrice(selectedVariant.price) }}</span>
                  </div>
                </div>
                <div v-else class="price-range-wrapper">
                  <span class="current-price">{{ formatPrice(product.minPrice) }}</span>
                  <span v-if="product.maxPrice && product.maxPrice !== product.minPrice" class="price-range">
                    - {{ formatPrice(product.maxPrice) }}
                  </span>
                </div>
              </div>

              <!-- Hiển thị thông tin khuyến mãi của sản phẩm -->
              <div v-if="product.promotion" class="promotion-info">
                <div class="promotion-badge">
                  🎁 <strong>{{ product.promotion.promotionName }}</strong>
                </div>
                <div class="promotion-discount">
                  Giảm <strong>{{ product.promotion.promotionDiscountPercent }}%</strong> cho tất cả biến thể
                </div>
              </div>

              <!-- Màu sắc -->
              <div class="variant-group" v-if="availableColors.length > 0">
                <h4>Màu sắc:</h4>
                <div class="color-options">
                  <div 
                    v-for="color in availableColors" 
                    :key="color.colorId"
                    class="color-btn"
                    :style="{ backgroundColor: color.hexCode || '#CCCCCC' }"
                    :class="{ active: selectedColorId === color.colorId }"
                    :title="color.colorName"
                    @click="selectColor(color.colorId)"
                  >
                    <span class="color-name">{{ color.colorName }}</span>
                  </div>
                </div>
              </div>

              <!-- Kích thước -->
              <div class="variant-group" v-if="availableSizes.length > 0">
                <h4>Kích thước:</h4>
                <div class="size-options">
                  <button 
                    v-for="size in availableSizes" 
                    :key="size.sizeId"
                    class="size-btn"
                    :class="{ 
                      active: selectedSizeId === size.sizeId,
                      'out-of-stock': !isSizeAvailable(size.sizeId)
                    }"
                    @click="selectSize(size.sizeId)"
                    :disabled="!isSizeAvailable(size.sizeId)"
                  >
                    {{ size.sizeName }}
                  </button>
                </div>
              </div>

              <!-- Selected Variant Info -->
              <div class="selected-variant" v-if="selectedVariant">
                <div class="variant-info">
                  <div class="variant-sku" v-if="selectedVariant.sku">
                    Mã SKU: {{ selectedVariant.sku }}
                  </div>
                  <div class="variant-stock" :class="{ 'out-of-stock': selectedVariant.quantity === 0 }">
                    <i class="fas" :class="selectedVariant.quantity > 0 ? 'fa-check-circle' : 'fa-times-circle'"></i>
                    {{ selectedVariant.quantity > 0 ? `Còn ${selectedVariant.quantity} sản phẩm` : 'Hết hàng' }}
                  </div>
                </div>
              </div>

              <!-- Quantity -->
              <div class="product-quantity" v-if="selectedVariant && selectedVariant.quantity > 0">
                <h4>Số lượng:</h4>
                <div class="quantity-selector">
                  <button @click="quantity > 1 && quantity--" :disabled="quantity <= 1">-</button>
                  <input type="number" v-model.number="quantity" min="1" :max="selectedVariant.quantity" />
                  <button @click="quantity < selectedVariant.quantity && quantity++">+</button>
                </div>
              </div>

              <!-- Actions -->
              <div class="product-actions">
                <button class="add-to-cart-btn" @click="addToCart" :disabled="!selectedVariant || selectedVariant.quantity === 0">
                  <i class="fas fa-shopping-cart"></i> Thêm vào giỏ
                </button>
                <button class="buy-now-btn" @click="buyNow" :disabled="!selectedVariant || selectedVariant.quantity === 0">
                  <i class="fas fa-bolt"></i> Mua ngay
                </button>
              </div>
            </div>
          </div>

          <!-- HÀNG DƯỚI: MÔ TẢ SẢN PHẨM -->
          <div class="product-description-section">
            <h3 class="section-title">Mô tả sản phẩm</h3>
            <div class="product-description-content">
              <p>{{ product.description || 'Chưa có mô tả cho sản phẩm này.' }}</p>
            </div>
          </div>

          <!-- ✅ PHẦN ĐÁNH GIÁ SẢN PHẨM -->
          <div class="product-reviews-section">
            <div class="reviews-header">
              <h3 class="section-title">Đánh giá sản phẩm</h3>
              <div class="rating-summary" v-if="ratingStats">
                <div class="average-rating">
                  <span class="rating-number">{{ ratingStats.averageRating?.toFixed(1) || 0 }}</span>
                  <div class="stars">
                    <i 
                      v-for="star in 5" 
                      :key="star"
                      :class="star <= Math.round(ratingStats.averageRating || 0) ? 'fas fa-star active' : 'far fa-star'"
                    ></i>
                  </div>
                  <span class="total-reviews">({{ ratingStats.totalReviews || 0 }} đánh giá)</span>
                </div>
              </div>
            </div>

            <!-- Loading reviews -->
            <div v-if="reviewsLoading" class="loading-reviews">
              <i class="fas fa-spinner fa-spin"></i>
              <span>Đang tải đánh giá...</span>
            </div>

            <!-- No reviews -->
            <div v-else-if="reviews.length === 0" class="no-reviews">
              <i class="fas fa-comment-slash"></i>
              <p>Chưa có đánh giá nào cho sản phẩm này</p>
              <p class="hint">Hãy là người đầu tiên đánh giá sản phẩm!</p>
            </div>

            <!-- Reviews list -->
            <div v-else class="reviews-list">
              <div 
                v-for="review in reviews" 
                :key="review.reviewId" 
                class="review-item"
              >
                <div class="review-header">
                  <div class="reviewer-info">
                    <div class="reviewer-avatar">
                      <i class="fas fa-user-circle"></i>
                    </div>
                    <div>
                      <div class="reviewer-name">{{ review.customerName || 'Khách hàng' }}</div>
                      <div class="reviewer-email">{{ review.customerEmail || '' }}</div>
                    </div>
                  </div>
                  <div class="review-rating">
                    <div class="stars">
                      <i 
                        v-for="star in 5" 
                        :key="star"
                        :class="star <= review.rating ? 'fas fa-star active' : 'far fa-star'"
                      ></i>
                    </div>
                    <span class="review-date">{{ formatDate(review.createdAt) }}</span>
                  </div>
                </div>
                <div class="review-body">
                  <p>{{ review.comment || 'Không có nhận xét' }}</p>
                  
                  <!-- ✅ HIỂN THỊ ẢNH REVIEW -->
                  <div v-if="review.images && review.images.length > 0" class="review-images">
                    <img 
                      v-for="(img, idx) in review.images" 
                      :key="idx"
                      :src="getImageUrl(img)"
                      :alt="'Hình ảnh đánh giá ' + (idx + 1)"
                      class="review-image-thumb"
                      @click="viewReviewImage(img)"
                    />
                  </div>
                </div>
              </div>

              <!-- Load more -->
              <div v-if="reviewsTotalPages > 1 && reviewsPage + 1 < reviewsTotalPages" class="load-more">
                <button 
                  class="btn btn-outline" 
                  @click="loadMoreReviews"
                  :disabled="reviewsLoadingMore"
                >
                  <i v-if="reviewsLoadingMore" class="fas fa-spinner fa-spin"></i>
                  {{ reviewsLoadingMore ? 'Đang tải...' : 'Xem thêm đánh giá' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Variants Table -->
          <div class="variants-table" v-if="product?.variants && product.variants.length > 0">
            <h3>Bảng thông số kỹ thuật</h3>
            <div class="table-wrapper">
              <table class="variant-table">
                <thead>
                  <tr>
                    <th>Màu sắc</th>
                    <th>Kích thước</th>
                    <th>SKU</th>
                    <th>Giá gốc</th>
                    <th>Giá khuyến mãi</th>
                    <th>Giảm giá</th>
                    <th>Tồn kho</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="variant in product.variants" :key="variant.productDetailId">
                    <td>
                      <span class="color-dot" :style="{ backgroundColor: variant.hexCode || '#CCCCCC' }"></span>
                      {{ variant.colorName }}
                    </td>
                    <td>{{ variant.sizeName }}</td>
                    <td>{{ variant.sku || '---' }}</td>
                    <td class="price-cell">
                      {{ formatPrice(variant.price) }}
                    </td>
                    <td class="discounted-price-cell" v-if="variant.discountedPrice && variant.discountedPrice < variant.price">
                      <span class="sale-price">{{ formatPrice(variant.discountedPrice) }}</span>
                    </td>
                    <td class="discounted-price-cell" v-else>
                      <span class="no-discount">—</span>
                    </td>
                    <td class="discount-percent-cell" v-if="variant.discountedPrice && variant.discountedPrice < variant.price">
                      <span class="discount-badge-table">-{{ calculateDiscountPercent(variant.price, variant.discountedPrice) }}%</span>
                    </td>
                    <td class="discount-percent-cell" v-else>
                      <span class="no-discount">0%</span>
                    </td>
                    <td :class="{ 'out-of-stock-text': variant.quantity === 0 }">
                      {{ variant.quantity > 0 ? variant.quantity + ' sản phẩm' : 'Hết hàng' }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
    <CartModal />
    
    <CartPopup 
      :visible="showCartPopup" 
      :product="lastAddedProduct"
      @close="showCartPopup = false"
    />

    <!-- ✅ Image Viewer Modal -->
    <div v-if="showImageViewer" class="modal-overlay" @click.self="closeImageViewer">
      <div class="image-viewer-container">
        <img :src="viewingImage" alt="Review image" />
        <button class="close-btn" @click="closeImageViewer">✕</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import CartModal from '../../components/client/CartModal.vue'
import CartPopup from '../../components/client/CartPopup.vue'
import api from "@/api/axios";

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const { showNotification } = useNotification()
const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");

const loading = ref(true)
const product = ref(null)
const currentImage = ref(0)
const quantity = ref(1)
const selectedColorId = ref(null)
const selectedSizeId = ref(null)

// State cho popup
const showCartPopup = ref(false)
const lastAddedProduct = ref({})

// ✅ State cho đánh giá
const reviews = ref([])
const reviewsLoading = ref(false)
const reviewsLoadingMore = ref(false)
const reviewsPage = ref(0)
const reviewsTotalPages = ref(0)
const reviewsTotalElements = ref(0)
const ratingStats = ref(null)

// ✅ State cho image viewer
const showImageViewer = ref(false)
const viewingImage = ref('')

// Format price
const formatPrice = (price) => {
  if (!price && price !== 0) return '0đ'
  const numPrice = typeof price === 'string' ? parseFloat(price) : price
  if (isNaN(numPrice) || numPrice === 0) return '0đ'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(numPrice)
}

// Format date
const formatDate = (dateString) => {
  if (!dateString) return '---'
  const date = new Date(dateString)
  if (isNaN(date.getTime())) return '---'
  return date.toLocaleDateString('vi-VN') + ' ' + 
         date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

// Tính phần trăm giảm giá
const calculateDiscountPercent = (originalPrice, discountedPrice) => {
  if (!originalPrice || !discountedPrice || originalPrice <= discountedPrice) return 0
  const discount = ((originalPrice - discountedPrice) / originalPrice) * 100
  return Math.round(discount)
}

// Lấy danh sách ảnh
const productImages = computed(() => {
  if (!product.value?.images) return []
  return product.value.images.map(img => img.imageUrl)
})

const mainImage = computed(() => {
  if (!productImages.value.length) return null
  return productImages.value[currentImage.value] || productImages.value[0]
})

// Lấy danh sách màu sắc từ variants
const availableColors = computed(() => {
  if (!product.value?.variants) return []
  const colorMap = new Map()
  product.value.variants.forEach(variant => {
    if (variant.colorId && !colorMap.has(variant.colorId)) {
      colorMap.set(variant.colorId, {
        colorId: variant.colorId,
        colorName: variant.colorName,
        hexCode: variant.hexCode || '#CCCCCC'
      })
    }
  })
  return Array.from(colorMap.values())
})

// Lấy danh sách kích thước từ variants
const availableSizes = computed(() => {
  if (!product.value?.variants) return []
  const sizeMap = new Map()
  product.value.variants.forEach(variant => {
    if (variant.sizeId && !sizeMap.has(variant.sizeId)) {
      sizeMap.set(variant.sizeId, {
        sizeId: variant.sizeId,
        sizeName: variant.sizeName
      })
    }
  })
  return Array.from(sizeMap.values())
})

// Kiểm tra size có khả dụng với màu đã chọn không
const isSizeAvailable = (sizeId) => {
  if (!product.value?.variants) return false
  if (selectedColorId.value) {
    return product.value.variants.some(v => 
      v.sizeId === sizeId && 
      v.colorId === selectedColorId.value && 
      v.quantity > 0
    )
  }
  return product.value.variants.some(v => v.sizeId === sizeId && v.quantity > 0)
}

// Tính toán biến thể được chọn
const selectedVariant = computed(() => {
  if (!product.value?.variants || product.value.variants.length === 0) return null
  
  if (selectedColorId.value && selectedSizeId.value) {
    const found = product.value.variants.find(v => 
      v.colorId === selectedColorId.value && 
      v.sizeId === selectedSizeId.value
    )
    if (found) return found
  }
  
  if (selectedColorId.value) {
    const found = product.value.variants.find(v => v.colorId === selectedColorId.value)
    if (found) return found
  }
  
  if (selectedSizeId.value) {
    const found = product.value.variants.find(v => v.sizeId === selectedSizeId.value)
    if (found) return found
  }
  
  const inStockVariant = product.value.variants.find(v => v.quantity > 0)
  if (inStockVariant) return inStockVariant
  
  return product.value.variants[0]
})

// Theo dõi selectedVariant
watch(selectedVariant, (newVariant) => {
  if (newVariant) {
    if (!selectedColorId.value && newVariant.colorId) {
      selectedColorId.value = newVariant.colorId
    }
    if (!selectedSizeId.value && newVariant.sizeId) {
      selectedSizeId.value = newVariant.sizeId
    }
    const maxQty = newVariant.quantity || 0
    if (quantity.value > maxQty && maxQty > 0) {
      quantity.value = maxQty
    } else if (maxQty === 0) {
      quantity.value = 1
    }
  }
})

// Chọn màu
const selectColor = (colorId) => {
  selectedColorId.value = colorId
  if (selectedSizeId.value) {
    const variantExists = product.value.variants.some(v => 
      v.colorId === colorId && 
      v.sizeId === selectedSizeId.value && 
      v.quantity > 0
    )
    if (!variantExists) {
      const newVariant = product.value.variants.find(v => v.colorId === colorId && v.quantity > 0)
      if (newVariant) {
        selectedSizeId.value = newVariant.sizeId
      } else {
        selectedSizeId.value = null
      }
    }
  } else {
    const firstVariant = product.value.variants.find(v => v.colorId === colorId && v.quantity > 0)
    if (firstVariant) {
      selectedSizeId.value = firstVariant.sizeId
    }
  }
}

// Chọn size
const selectSize = (sizeId) => {
  selectedSizeId.value = sizeId
  if (selectedColorId.value) {
    const variantExists = product.value.variants.some(v => 
      v.colorId === selectedColorId.value && 
      v.sizeId === sizeId && 
      v.quantity > 0
    )
    if (!variantExists) {
      const newVariant = product.value.variants.find(v => v.sizeId === sizeId && v.quantity > 0)
      if (newVariant) {
        selectedColorId.value = newVariant.colorId
      } else {
        selectedColorId.value = null
      }
    }
  } else {
    const firstVariant = product.value.variants.find(v => v.sizeId === sizeId && v.quantity > 0)
    if (firstVariant) {
      selectedColorId.value = firstVariant.colorId
    }
  }
}

// ✅ Lấy URL ảnh
const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

// ✅ View review image
const viewReviewImage = (imageUrl) => {
  viewingImage.value = getImageUrl(imageUrl)
  showImageViewer.value = true
}

// ✅ Close image viewer
const closeImageViewer = () => {
  showImageViewer.value = false
  viewingImage.value = ''
}

// ✅ Load product detail
const loadProductDetail = async () => {
  loading.value = true
  try {
    const productId = route.params.id
    if (!productId || productId === 'undefined') {
      product.value = null
      loading.value = false
      showNotification('Không tìm thấy sản phẩm!', 'error')
      return
    }
    
    const response = await api.get(`/customer/home/products/${productId}`)
    product.value = response.data
    
    const firstInStockVariant = product.value.variants?.find(v => v.quantity > 0)
    if (firstInStockVariant) {
      selectedColorId.value = firstInStockVariant.colorId
      selectedSizeId.value = firstInStockVariant.sizeId
    } else if (product.value.variants?.length > 0) {
      selectedColorId.value = product.value.variants[0].colorId
      selectedSizeId.value = product.value.variants[0].sizeId
    }
    
    // ✅ Sau khi load product, load đánh giá
    if (productId) {
      await loadReviews(productId)
      await loadRatingStats(productId)
    }
    
  } catch (error) {
    console.error("❌ Lỗi:", error)
    product.value = null
    showNotification('Không thể tải thông tin sản phẩm!', 'error')
  } finally {
    loading.value = false
  }
}

// ✅ Load đánh giá sản phẩm - SỬA LẠI ĐÚNG API
const loadReviews = async (productId, reset = true) => {
  if (reset) {
    reviewsPage.value = 0
    reviews.value = []
  }
  
  reviewsLoading.value = reset
  reviewsLoadingMore.value = !reset
  
  try {
    if (!productId) {
      console.warn('Không tìm thấy productId')
      reviewsLoading.value = false
      reviewsLoadingMore.value = false
      return
    }

    // ✅ Gọi API gộp theo productId (tất cả biến thể màu/size): /api/reviews/product-all/{productId}
    const response = await api.get(`/reviews/product-all/${productId}`, {
      params: {
        page: reviewsPage.value,
        size: 5
      }
    })
    
    console.log('✅ Reviews response:', response.data)
    
    if (response.data && response.data.content) {
      if (reset) {
        reviews.value = response.data.content || []
      } else {
        reviews.value = [...reviews.value, ...(response.data.content || [])]
      }
      reviewsTotalPages.value = response.data.totalPages || 0
      reviewsTotalElements.value = response.data.totalElements || 0
    }
  } catch (error) {
    console.error('❌ Lỗi tải đánh giá:', error)
  } finally {
    reviewsLoading.value = false
    reviewsLoadingMore.value = false
  }
}

// ✅ Load thống kê rating
const loadRatingStats = async (productId) => {
  try {
    if (!productId) return

    const response = await api.get(`/reviews/product-all/${productId}/rating`)
    ratingStats.value = response.data
  } catch (error) {
    console.error('❌ Lỗi tải thống kê rating:', error)
  }
}

// ✅ Load more reviews
const loadMoreReviews = () => {
  if (reviewsPage.value + 1 < reviewsTotalPages.value) {
    reviewsPage.value++
    loadReviews(product.value?.productId, false)
  }
}

// Add to cart
const addToCart = async () => {
  if (!selectedVariant.value) {
    showNotification('Vui lòng chọn màu sắc và kích thước!', 'error')
    return
  }
  
  if (selectedVariant.value.quantity === 0) {
    showNotification('Sản phẩm đã hết hàng!', 'error')
    return
  }
  
  if (quantity.value > selectedVariant.value.quantity) {
    showNotification(`Chỉ còn ${selectedVariant.value.quantity} sản phẩm!`, 'error')
    return
  }
  
  const finalPrice = selectedVariant.value.discountedPrice && selectedVariant.value.discountedPrice < selectedVariant.value.price
    ? selectedVariant.value.discountedPrice
    : selectedVariant.value.price
  
  const stockQuantity = selectedVariant.value.quantity || 0
  
  const productData = {
    productDetailId: selectedVariant.value.productDetailId,
    productId: product.value.productId,
    productName: product.value.productName,
    name: product.value.productName,
    price: finalPrice,
    quantity: quantity.value,
    image: mainImage.value,
    imageUrl: mainImage.value,
    size: selectedVariant.value.sizeName,
    color: selectedVariant.value.colorName,
    sku: selectedVariant.value.sku,
    discountPercent: selectedVariant.value.discountedPrice ? 
      calculateDiscountPercent(selectedVariant.value.price, selectedVariant.value.discountedPrice) : 0,
    stock: stockQuantity
  }
  
  try {
    await cartStore.addToCart(productData)
    
    lastAddedProduct.value = {
      name: product.value.productName,
      price: finalPrice,
      quantity: quantity.value,
      image: mainImage.value,
      size: selectedVariant.value.sizeName,
      color: selectedVariant.value.colorName,
      stock: stockQuantity
    }
    
    showCartPopup.value = true
    quantity.value = 1
    
  } catch (error) {
    console.error('❌ Add to cart error:', error)
    showNotification(error.response?.data?.message || 'Thêm vào giỏ hàng thất bại!', 'error')
  }
}

const buyNow = () => {
  addToCart()
}

onMounted(() => {
  loadProductDetail()
})
</script>

<style scoped>
.product-detail-page {
  min-height: 100vh;
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

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 30px;
  color: #999;
  font-size: 14px;
}

.breadcrumb a {
  color: #999;
  text-decoration: none;
  transition: color 0.3s;
}

.breadcrumb a:hover {
  color: #e63946;
}

.breadcrumb .current {
  color: #2c3e50;
}

.loading, .not-found {
  text-align: center;
  padding: 80px;
  background: white;
  border-radius: 15px;
}

.not-found i {
  font-size: 60px;
  color: #ccc;
  margin-bottom: 20px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #e63946;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* HÀNG TRÊN: ẢNH + THÔNG TIN */
.product-detail-top {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 50px;
  background: white;
  border-radius: 20px;
  padding: 40px;
  margin-bottom: 40px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.product-gallery {
  position: relative;
}

.main-image {
  width: 100%;
  height: 400px;
  background: #f5f5f5;
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  overflow: hidden;
}

.main-image i {
  font-size: 120px;
  color: #ccc;
}

.detail-main-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-list {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.thumbnail {
  width: 80px;
  height: 80px;
  background: #f5f5f5;
  border-radius: 10px;
  cursor: pointer;
  overflow: hidden;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.thumbnail.active {
  border-color: #e63946;
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  padding: 20px 0;
}

.product-category {
  color: #e63946;
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.product-title {
  font-size: 32px;
  color: #2c3e50;
  margin-bottom: 15px;
}

/* Price Styles */
.product-price {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.price-wrapper {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 12px;
}

.original-price {
  font-size: 18px;
  color: #999;
  text-decoration: line-through;
}

.discount-badge {
  display: inline-block;
  background: #e63946;
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.current-price-wrapper {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.current-price {
  font-size: 32px;
  font-weight: 700;
  color: #e63946;
}

.price-label {
  font-size: 14px;
  color: #28a745;
  font-weight: 500;
}

.price-range-wrapper .current-price {
  font-size: 28px;
}

.price-range {
  font-size: 20px;
  color: #999;
}

/* Promotion Info */
.promotion-info {
  background: linear-gradient(135deg, #fff5f5, #ffe0e0);
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 20px;
  border-left: 4px solid #e63946;
}

.promotion-badge {
  font-size: 14px;
  color: #e63946;
  margin-bottom: 5px;
}

.promotion-discount {
  font-size: 13px;
  color: #666;
}

.promotion-discount strong {
  color: #e63946;
  font-size: 16px;
}

.variant-group {
  margin-bottom: 20px;
}

.variant-group h4 {
  color: #2c3e50;
  margin-bottom: 10px;
  font-size: 16px;
}

/* Color Options - Hiển thị tên màu */
.color-options {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.color-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  min-width: 70px;
}

.color-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  background: rgba(255,255,255,0.8);
  padding: 2px 6px;
  border-radius: 20px;
}

.color-btn:hover,
.color-btn.active {
  transform: scale(1.05);
  border-color: #2c3e50;
  box-shadow: 0 0 0 2px white, 0 0 0 4px #e63946;
}

.color-btn.active .color-name {
  color: #e63946;
  font-weight: bold;
}

/* Size Options */
.size-options {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.size-btn {
  min-width: 55px;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 600;
}

.size-btn:hover:not(:disabled),
.size-btn.active {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.size-btn.out-of-stock {
  background: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
  text-decoration: line-through;
}

.size-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.selected-variant {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin: 15px 0;
}

.variant-info {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: baseline;
}

.variant-sku {
  font-size: 13px;
  color: #999;
}

.variant-stock {
  font-size: 14px;
  color: #28a745;
}

.variant-stock.out-of-stock {
  color: #e63946;
}

.variant-stock i {
  margin-right: 5px;
}

.product-quantity {
  margin-bottom: 20px;
}

.product-quantity h4 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.quantity-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quantity-selector button {
  width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 18px;
  font-weight: bold;
  transition: all 0.3s;
}

.quantity-selector button:hover:not(:disabled) {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.quantity-selector button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quantity-selector input {
  width: 60px;
  height: 36px;
  text-align: center;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
}

.product-actions {
  display: flex;
  gap: 15px;
  margin-top: 20px;
}

.add-to-cart-btn,
.buy-now-btn {
  flex: 1;
  padding: 15px;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.add-to-cart-btn {
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
}

.add-to-cart-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #c82333, #e63946);
  transform: translateY(-2px);
}

.add-to-cart-btn:disabled,
.buy-now-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.buy-now-btn {
  background: #2c3e50;
  color: white;
}

.buy-now-btn:hover:not(:disabled) {
  background: #1a252f;
  transform: translateY(-2px);
}

/* HÀNG DƯỚI: MÔ TẢ SẢN PHẨM */
.product-description-section {
  background: white;
  border-radius: 20px;
  padding: 40px;
  margin-bottom: 40px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #e63946;
  position: relative;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 50px;
  height: 2px;
  background: #e63946;
}

.product-description-content {
  line-height: 1.8;
  color: #555;
  font-size: 16px;
}

.product-description-content p {
  margin-bottom: 15px;
}

/* ✅ PHẦN ĐÁNH GIÁ SẢN PHẨM */
.product-reviews-section {
  background: white;
  border-radius: 20px;
  padding: 40px;
  margin-bottom: 40px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  flex-wrap: wrap;
  gap: 15px;
}

.rating-summary {
  display: flex;
  align-items: center;
  gap: 15px;
}

.average-rating {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-number {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
}

.average-rating .stars {
  display: flex;
  gap: 2px;
}

.average-rating .stars i {
  font-size: 18px;
  color: #ddd;
}

.average-rating .stars i.active {
  color: #f5c842;
}

.total-reviews {
  font-size: 14px;
  color: #999;
}

.loading-reviews {
  text-align: center;
  padding: 40px;
  color: #999;
}

.loading-reviews i {
  margin-right: 10px;
}

.no-reviews {
  text-align: center;
  padding: 40px;
  color: #999;
}

.no-reviews i {
  font-size: 48px;
  color: #ddd;
  margin-bottom: 15px;
}

.no-reviews .hint {
  font-size: 13px;
  color: #ccc;
  margin-top: 5px;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.review-item {
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s;
}

.review-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 10px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reviewer-avatar {
  width: 40px;
  height: 40px;
  background: #f0f0f0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 30px;
}

.reviewer-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 15px;
}

.reviewer-email {
  font-size: 13px;
  color: #999;
}

.review-rating {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.review-rating .stars {
  display: flex;
  gap: 2px;
}

.review-rating .stars i {
  font-size: 16px;
  color: #ddd;
}

.review-rating .stars i.active {
  color: #f5c842;
}

.review-date {
  font-size: 12px;
  color: #ccc;
}

.review-body {
  padding-left: 52px;
}

.review-body p {
  color: #555;
  line-height: 1.6;
  font-size: 15px;
  margin: 0;
}

/* ✅ REVIEW IMAGES */
.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.review-image-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e9ecef;
}

.review-image-thumb:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* ✅ IMAGE VIEWER MODAL */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(5px);
}

.image-viewer-container {
  max-width: 90vw;
  max-height: 90vh;
  position: relative;
  background: transparent;
}

.image-viewer-container img {
  max-width: 100%;
  max-height: 85vh;
  object-fit: contain;
  border-radius: 8px;
}

.image-viewer-container .close-btn {
  position: absolute;
  top: -40px;
  right: -40px;
  background: white;
  color: #333;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 10px rgba(0,0,0,0.2);
}

.image-viewer-container .close-btn:hover {
  transform: rotate(90deg);
  background: #e63946;
  color: white;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

.load-more .btn {
  padding: 10px 30px;
}

/* Variants Table */
.variants-table {
  background: white;
  border-radius: 20px;
  padding: 30px;
  margin-top: 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
}

.variants-table h3 {
  color: #2c3e50;
  margin-bottom: 20px;
}

.table-wrapper {
  overflow-x: auto;
}

.variant-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 700px;
}

.variant-table th,
.variant-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.variant-table th {
  background: #f8f9fa;
  font-weight: 600;
  color: #2c3e50;
}

.color-dot {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
  border: 1px solid #ddd;
}

.price-cell {
  color: #666;
}

.discounted-price-cell .sale-price {
  color: #e63946;
  font-weight: 600;
}

.discounted-price-cell .no-discount {
  color: #ccc;
}

.discount-percent-cell .discount-badge-table {
  display: inline-block;
  background: #e63946;
  color: white;
  padding: 2px 8px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.discount-percent-cell .no-discount {
  color: #ccc;
}

.out-of-stock-text {
  color: #e63946;
}

.btn-primary {
  display: inline-block;
  padding: 12px 24px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-primary:hover {
  transform: translateY(-2px);
}

.btn-outline {
  padding: 8px 20px;
  background: transparent;
  border: 2px solid #ddd;
  color: #666;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-outline:hover {
  border-color: #e63946;
  color: #e63946;
}

/* Responsive */
@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .product-detail-top {
    grid-template-columns: 1fr;
    padding: 20px;
  }

  .product-title {
    font-size: 24px;
  }

  .current-price {
    font-size: 24px;
  }

  .original-price {
    font-size: 16px;
  }

  .product-actions {
    flex-direction: column;
  }
  
  .variant-info {
    flex-direction: column;
    gap: 8px;
  }
  
  .price-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .color-btn {
    min-width: 55px;
    padding: 6px 12px;
  }
  
  .color-name {
    font-size: 11px;
  }
  
  .size-btn {
    min-width: 45px;
    height: 36px;
    font-size: 13px;
  }

  .product-description-section {
    padding: 20px;
  }

  .section-title {
    font-size: 18px;
  }

  .product-reviews-section {
    padding: 20px;
  }

  .reviews-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .review-header {
    flex-direction: column;
  }

  .review-rating {
    align-items: flex-start;
  }

  .review-body {
    padding-left: 0;
    padding-top: 10px;
  }

  .review-image-thumb {
    width: 60px;
    height: 60px;
  }

  .image-viewer-container .close-btn {
    top: 10px;
    right: 10px;
  }
}
</style>