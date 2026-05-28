<template>
  <div class="product-detail-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <!-- Breadcrumb -->
        <div class="breadcrumb">
          <router-link to="/">Trang chủ</router-link>
          <i class="fas fa-chevron-right"></i>
          <router-link :to="`/shop?category=${product?.category}`">
            {{ getCategoryName(product?.category) }}
          </router-link>
          <i class="fas fa-chevron-right"></i>
          <span>{{ product?.name }}</span>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="loading">
          <div class="spinner"></div>
          <p>Đang tải thông tin sản phẩm...</p>
        </div>

        <!-- Not Found -->
        <div v-else-if="!product" class="not-found">
          <i class="fas fa-exclamation-circle"></i>
          <h3>Không tìm thấy sản phẩm</h3>
          <router-link to="/shop" class="btn btn-primary">
            Quay lại cửa hàng
          </router-link>
        </div>

        <!-- Product Detail -->
        <div v-else class="product-detail">
          <div class="product-gallery">
            <div 
              class="main-image" 
              @mousemove="handleZoom"
              @mouseleave="resetZoom"
              ref="mainImageRef"
            >
              <i :class="['fas', `fa-${product.image}`]"></i>
              <div 
                v-if="showZoom" 
                class="zoom-lens"
                :style="lensStyle"
              ></div>
            </div>
            <div class="thumbnail-list">
              <div 
                v-for="(thumb, index) in thumbnails" 
                :key="index"
                class="thumbnail"
                :class="{ active: currentImage === index }"
                @click="changeImage(index)"
              >
                <i :class="['fas', `fa-${thumb}`]"></i>
              </div>
            </div>
          </div>

          <div class="product-info">
            <div class="product-category">{{ getCategoryName(product.category) }}</div>
            <h1 class="product-title">{{ product.name }}</h1>

            <div class="product-rating">
              <div class="stars">
                <i 
                  v-for="star in 5" 
                  :key="star"
                  :class="getStarClass(star, product.rating)"
                ></i>
              </div>
              <span class="rating-count">
                {{ product.rating }} / 5 ({{ product.sold }} đã bán)
              </span>
            </div>

            <div class="product-price">
              <span class="current-price">{{ formatPrice(product.price) }}</span>
              <span v-if="product.oldPrice" class="old-price">
                {{ formatPrice(product.oldPrice) }}
              </span>
              <span v-if="product.oldPrice" class="discount">
                -{{ calculateDiscount(product.price, product.oldPrice) }}%
              </span>
            </div>

            <div class="product-description">
              <h3>Mô tả sản phẩm</h3>
              <p>{{ product.description || 'Chưa có mô tả cho sản phẩm này.' }}</p>
            </div>

            <div class="product-variants">
              <div v-if="hasSizes" class="variant-group">
                <h4>Kích thước:</h4>
                <div class="size-options">
                  <button 
                    v-for="size in sizes" 
                    :key="size"
                    class="size-btn"
                    :class="{ active: selectedSize === size }"
                    @click="selectedSize = size"
                  >
                    {{ size }}
                  </button>
                </div>
              </div>

              <div v-if="hasColors" class="variant-group">
                <h4>Màu sắc:</h4>
                <div class="color-options">
                  <button 
                    v-for="color in colors" 
                    :key="color.name"
                    class="color-btn"
                    :class="[color.class, { active: selectedColor === color.name }]"
                    @click="selectedColor = color.name"
                    :title="color.name"
                  ></button>
                </div>
              </div>
            </div>

            <div class="product-quantity">
              <h4>Số lượng:</h4>
              <div class="quantity-selector">
                <button 
                  class="quantity-btn"
                  @click="updateQuantity(-1)"
                  :disabled="quantity <= 1"
                >
                  <i class="fas fa-minus"></i>
                </button>
                <input 
                  type="number" 
                  v-model.number="quantity" 
                  min="1" 
                  max="99"
                  readonly
                >
                <button 
                  class="quantity-btn"
                  @click="updateQuantity(1)"
                  :disabled="quantity >= 99"
                >
                  <i class="fas fa-plus"></i>
                </button>
              </div>
              <span class="stock-info">Còn {{ product.stock || 100 }} sản phẩm</span>
            </div>

            <div class="product-actions">
              <button class="add-to-cart-btn" @click="addToCart">
                <i class="fas fa-shopping-cart"></i>
                THÊM VÀO GIỎ
              </button>
              <button 
                class="wishlist-btn" 
                :class="{ active: isInWishlist }"
                @click="toggleWishlist"
              >
                <i :class="[isInWishlist ? 'fas' : 'far', 'fa-heart']"></i>
                YÊU THÍCH
              </button>
            </div>

            <div class="product-meta">
              <div class="meta-item">
                <i class="fas fa-truck"></i>
                <span>Miễn phí vận chuyển cho đơn hàng từ 500.000₫</span>
              </div>
              <div class="meta-item">
                <i class="fas fa-undo-alt"></i>
                <span>Đổi trả trong 30 ngày nếu sản phẩm lỗi</span>
              </div>
              <div class="meta-item">
                <i class="fas fa-shield-alt"></i>
                <span>Bảo hành chính hãng 12 tháng</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Product Tabs -->
        <div v-if="product" class="product-tabs">
          <div class="tab-headers">
            <button 
              v-for="(tab, index) in tabs" 
              :key="index"
              class="tab-header"
              :class="{ active: activeTab === index }"
              @click="activeTab = index"
            >
              {{ tab.name }}
            </button>
          </div>

          <div class="tab-content">
            <!-- Details Tab -->
            <div v-show="activeTab === 0" class="tab-pane">
              <h3>Thông tin chi tiết</h3>
              <table>
                <tr v-for="(value, key) in specifications" :key="key">
                  <td>{{ key }}</td>
                  <td>{{ value }}</td>
                </tr>
              </table>
            </div>

            <!-- Care Instructions Tab -->
            <div v-show="activeTab === 1" class="tab-pane">
              <h3>Hướng dẫn bảo quản</h3>
              <ul>
                <li v-for="instruction in careInstructions" :key="instruction">
                  <i class="fas fa-check"></i> {{ instruction }}
                </li>
              </ul>
            </div>

            <!-- Reviews Tab -->
            <div v-show="activeTab === 2" class="tab-pane">
              <h3>Đánh giá sản phẩm</h3>
              <div class="reviews">
                <div v-for="review in reviews" :key="review.id" class="review-item">
                  <div class="review-header">
                    <strong>{{ review.user }}</strong>
                    <div class="review-rating">
                      <i 
                        v-for="star in 5" 
                        :key="star"
                        :class="getStarClass(star, review.rating)"
                      ></i>
                    </div>
                  </div>
                  <p class="review-date">{{ review.date }}</p>
                  <p class="review-content">{{ review.content }}</p>
                </div>
              </div>
            </div>

            <!-- Shipping Tab -->
            <div v-show="activeTab === 3" class="tab-pane">
              <h3>Chính sách vận chuyển</h3>
              <ul>
                <li v-for="policy in shippingPolicies" :key="policy">
                  <i class="fas fa-check"></i> {{ policy }}
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- Related Products -->
        <div v-if="relatedProducts.length > 0" class="related-products">
          <h2 class="related-title">SẢN PHẨM LIÊN QUAN</h2>
          <div class="product-grid">
            <ProductCard
              v-for="product in relatedProducts"
              :key="product.id"
              :product="product"
            />
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
    <CartModal />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProductStore } from '../../stores/productStore'
import { useCartStore } from '../../stores/cartStore'
import { useAuthStore } from '../../stores/authStore'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import ProductCard from '../../components/client/ProductCard.vue'
import CartModal from '../../components/client/CartModal.vue'
import { formatPrice, getCategoryName, getStarClass, calculateDiscount } from '../../utils/helpers'

const route = useRoute()
const router = useRouter()
const productStore = useProductStore()
const cartStore = useCartStore()
const authStore = useAuthStore()
const { showNotification } = useNotification()

// State
const loading = ref(true)
const product = ref(null)
const currentImage = ref(0)
const quantity = ref(1)
const selectedSize = ref('M')
const selectedColor = ref('Đen')
const activeTab = ref(0)
const showZoom = ref(false)
const lensStyle = ref({})
const mainImageRef = ref(null)

const thumbnails = ['tshirt', 'tshirt', 'tshirt', 'tshirt']

const sizes = ['S', 'M', 'L', 'XL', 'XXL']
const colors = [
  { name: 'Đỏ', class: 'red' },
  { name: 'Xanh', class: 'blue' },
  { name: 'Đen', class: 'black' },
  { name: 'Trắng', class: 'white' },
  { name: 'Xám', class: 'gray' }
]

const tabs = [
  { name: 'CHI TIẾT' },
  { name: 'BẢO QUẢN' },
  { name: 'ĐÁNH GIÁ' },
  { name: 'VẬN CHUYỂN' }
]

const specifications = {
  'Thương hiệu': 'Fashion Store',
  'Chất liệu': 'Cotton 100%',
  'Xuất xứ': 'Việt Nam',
  'Mẫu mã': 'Cổ tròn, tay ngắn',
  'Kiểu dáng': 'Regular fit'
}

const careInstructions = [
  'Giặt ở nhiệt độ bình thường',
  'Không dùng chất tẩy mạnh',
  'Phơi trong bóng râm',
  'Là ủi ở nhiệt độ thấp',
  'Không giặt khô'
]

const reviews = [
  {
    id: 1,
    user: 'Nguyễn Văn A',
    rating: 5,
    date: '15/03/2024',
    content: 'Sản phẩm chất lượng tốt, đúng như mô tả. Sẽ ủng hộ shop lần sau.'
  },
  {
    id: 2,
    user: 'Trần Thị B',
    rating: 4,
    date: '14/03/2024',
    content: 'Áo đẹp, chất vải mát. Tuy nhiên giao hàng hơi chậm.'
  },
  {
    id: 3,
    user: 'Lê Văn C',
    rating: 5,
    date: '13/03/2024',
    content: 'Rất hài lòng với sản phẩm và dịch vụ của shop.'
  }
]

const shippingPolicies = [
  'Miễn phí vận chuyển cho đơn hàng từ 500.000₫',
  'Thời gian giao hàng từ 2-4 ngày làm việc',
  'Giao hàng nhanh trong 24h (áp dụng nội thành)',
  'Kiểm tra hàng trước khi thanh toán',
  'Hỗ trợ đổi trả trong vòng 7 ngày'
]

// Computed
const hasSizes = computed(() => product.value?.category === 'men' || product.value?.category === 'women')
const hasColors = computed(() => true)

const isInWishlist = computed(() => {
  return false
})

const relatedProducts = computed(() => {
  if (!product.value) return []
  return productStore.products
    .filter(p => p.category === product.value.category && p.id !== product.value.id)
    .slice(0, 4)
})

// Methods
const changeImage = (index) => {
  currentImage.value = index
}

const updateQuantity = (change) => {
  const newValue = quantity.value + change
  if (newValue >= 1 && newValue <= 99) {
    quantity.value = newValue
  }
}

const handleZoom = (event) => {
  showZoom.value = true
  const rect = mainImageRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  lensStyle.value = {
    left: `${x - 50}px`,
    top: `${y - 50}px`
  }
}

const resetZoom = () => {
  showZoom.value = false
}

const addToCart = () => {
  if (!product.value) return
  
  cartStore.addToCart({
    ...product.value,
    quantity: quantity.value,
    size: selectedSize.value,
    color: selectedColor.value
  })
  
  showNotification('Đã thêm sản phẩm vào giỏ hàng!', 'success')
}

const toggleWishlist = () => {
  if (!authStore.isAuthenticated) {
    window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'login' }))
    return
  }
  
  showNotification(
    isInWishlist.value ? 'Đã xóa khỏi danh sách yêu thích' : 'Đã thêm vào danh sách yêu thích',
    'success'
  )
}

// Lifecycle
onMounted(() => {
  setTimeout(() => {
    const id = parseInt(route.params.id)
    product.value = productStore.products.find(p => p.id === id)
    loading.value = false
  }, 500)
})
</script>

<style scoped>
.product-detail-page {
  min-height: 100vh;
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
  color: var(--medium-gray);
  font-size: 14px;
}

.breadcrumb a {
  color: var(--medium-gray);
  text-decoration: none;
  transition: color 0.3s;
}

.breadcrumb a:hover {
  color: var(--primary-red);
}

.breadcrumb i {
  font-size: 12px;
}

.loading,
.not-found {
  text-align: center;
  padding: 100px 20px;
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
}

.not-found i {
  font-size: 60px;
  color: var(--primary-red);
  margin-bottom: 20px;
}

.not-found h3 {
  color: var(--dark-gray);
  margin-bottom: 20px;
}

/* Product Detail */
.product-detail {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 50px;
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: var(--shadow);
  margin-bottom: 30px;
}

/* Gallery */
.product-gallery {
  position: relative;
}

.main-image {
  width: 100%;
  height: 400px;
  background: var(--light-gray);
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  cursor: zoom-in;
}

.main-image i {
  font-size: 150px;
  color: var(--medium-gray);
  transition: transform 0.3s;
}

.main-image:hover i {
  transform: scale(1.1);
}

.zoom-lens {
  position: absolute;
  width: 100px;
  height: 100px;
  border: 2px solid var(--primary-red);
  background: rgba(255,255,255,0.3);
  border-radius: 50%;
  pointer-events: none;
  transform: translate(-50%, -50%);
}

.thumbnail-list {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.thumbnail {
  width: 80px;
  height: 80px;
  background: var(--light-gray);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.thumbnail:hover,
.thumbnail.active {
  border-color: var(--primary-red);
  transform: translateY(-5px);
}

.thumbnail i {
  font-size: 30px;
  color: var(--medium-gray);
}

/* Product Info */
.product-info {
  padding: 20px 0;
}

.product-category {
  color: var(--primary-red);
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.product-title {
  font-size: 32px;
  color: var(--dark-gray);
  margin-bottom: 15px;
  font-weight: 700;
  line-height: 1.3;
}

.product-rating {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stars {
  color: #ffc107;
  font-size: 16px;
}

.rating-count {
  color: var(--medium-gray);
  font-size: 14px;
}

.product-price {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 25px;
  flex-wrap: wrap;
}

.current-price {
  font-size: 36px;
  font-weight: 800;
  color: var(--primary-red);
}

.old-price {
  font-size: 24px;
  color: var(--medium-gray);
  text-decoration: line-through;
}

.discount {
  background: var(--primary-red);
  color: white;
  padding: 5px 15px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: 600;
}

.product-description {
  margin-bottom: 25px;
}

.product-description h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: var(--dark-gray);
}

.product-description p {
  color: var(--medium-gray);
  line-height: 1.8;
}

/* Variants */
.variant-group {
  margin-bottom: 20px;
}

.variant-group h4 {
  color: var(--dark-gray);
  margin-bottom: 10px;
  font-size: 16px;
}

.size-options {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.size-btn {
  width: 45px;
  height: 45px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 600;
}

.size-btn:hover,
.size-btn.active {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
  transform: scale(1.05);
}

.color-options {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.color-btn {
  width: 35px;
  height: 35px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.color-btn:hover,
.color-btn.active {
  transform: scale(1.2);
  border-color: var(--dark-gray);
  box-shadow: 0 0 0 2px white, 0 0 0 4px var(--primary-red);
}

.color-btn.red { background: #dc3545; }
.color-btn.blue { background: #007bff; }
.color-btn.black { background: #343a40; }
.color-btn.white { background: #f8f9fa; border: 1px solid var(--border-color); }
.color-btn.gray { background: #6c757d; }

/* Quantity */
.product-quantity {
  margin-bottom: 25px;
}

.product-quantity h4 {
  color: var(--dark-gray);
  margin-bottom: 10px;
  font-size: 16px;
}

.quantity-selector {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.quantity-btn {
  width: 40px;
  height: 40px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 16px;
  font-weight: 600;
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

.quantity-selector input {
  width: 60px;
  height: 40px;
  text-align: center;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
}

.stock-info {
  color: var(--medium-gray);
  font-size: 13px;
}

/* Actions */
.product-actions {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
  flex-wrap: wrap;
}

.add-to-cart-btn {
  flex: 2;
  min-width: 200px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  border: none;
  padding: 15px 30px;
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

.add-to-cart-btn:hover {
  background: linear-gradient(135deg, var(--dark-red), var(--primary-red));
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.3);
}

.wishlist-btn {
  flex: 1;
  min-width: 120px;
  background: white;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.wishlist-btn:hover,
.wishlist-btn.active {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
}

/* Meta */
.product-meta {
  border-top: 1px solid var(--border-color);
  padding-top: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--medium-gray);
  font-size: 14px;
}

.meta-item i {
  width: 20px;
  color: var(--primary-red);
  font-size: 16px;
}

/* Tabs */
.product-tabs {
  background: white;
  border-radius: 20px;
  padding: 30px;
  box-shadow: var(--shadow);
  margin-bottom: 30px;
}

.tab-headers {
  display: flex;
  gap: 30px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.tab-header {
  padding: 10px 0;
  cursor: pointer;
  font-weight: 600;
  color: var(--medium-gray);
  background: none;
  border: none;
  position: relative;
  transition: color 0.3s;
}

.tab-header:hover {
  color: var(--primary-red);
}

.tab-header.active {
  color: var(--primary-red);
}

.tab-header.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--primary-red);
}

.tab-content {
  padding: 20px 0;
}

.tab-pane h3 {
  color: var(--dark-gray);
  margin-bottom: 20px;
  font-size: 18px;
}

.tab-pane table {
  width: 100%;
  border-collapse: collapse;
}

.tab-pane table tr {
  border-bottom: 1px solid var(--border-color);
}

.tab-pane table td {
  padding: 10px;
  color: var(--medium-gray);
}

.tab-pane table td:first-child {
  font-weight: 600;
  width: 30%;
}

.tab-pane ul {
  list-style: none;
  padding: 0;
}

.tab-pane ul li {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--medium-gray);
}

.tab-pane ul li i {
  color: #28a745;
  font-size: 14px;
}

/* Reviews */
.review-item {
  padding: 15px 0;
  border-bottom: 1px solid var(--border-color);
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 5px;
  flex-wrap: wrap;
}

.review-header strong {
  color: var(--dark-gray);
}

.review-rating {
  color: #ffc107;
  font-size: 13px;
}

.review-date {
  color: var(--medium-gray);
  font-size: 12px;
  margin-bottom: 8px;
}

.review-content {
  color: var(--medium-gray);
  line-height: 1.6;
}

/* Related Products */
.related-products {
  margin-top: 30px;
}

.related-title {
  font-size: 24px;
  color: var(--dark-gray);
  margin-bottom: 20px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

/* Spinner */
.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--light-gray);
  border-top: 4px solid var(--primary-red);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 20px auto;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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

  .product-detail {
    grid-template-columns: 1fr;
    padding: 20px;
  }

  .product-title {
    font-size: 24px;
  }

  .current-price {
    font-size: 28px;
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .tab-headers {
    gap: 15px;
  }
}

@media (max-width: 480px) {
  .product-actions {
    flex-direction: column;
  }

  .add-to-cart-btn,
  .wishlist-btn {
    width: 100%;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }

  .tab-headers {
    flex-direction: column;
    gap: 5px;
  }

  .tab-header {
    width: 100%;
    text-align: left;
  }
}
</style>