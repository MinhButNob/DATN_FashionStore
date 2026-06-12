<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cartStore'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import CartModal from '../../components/client/CartModal.vue'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const { showNotification } = useNotification()
const API_BASE = "http://localhost:8080/api"

const loading = ref(true)
const product = ref(null)
const currentImage = ref(0)
const quantity = ref(1)
const selectedColorId = ref(null)
const selectedSizeId = ref(null)

const formatPrice = (price) => {
  if (!price) return '0đ'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
}

const productImages = computed(() => {
  if (!product.value?.images) return []
  return product.value.images.map(img => img.imageUrl)
})

const mainImage = computed(() => {
  if (!productImages.value.length) return null
  return productImages.value[currentImage.value] || productImages.value[0]
})

const uniqueColors = computed(() => {
  if (!product.value?.colorOptions) return []
  return product.value.colorOptions
})

const uniqueSizes = computed(() => {
  if (!product.value?.sizeOptions) return []
  return product.value.sizeOptions
})

const selectedVariant = computed(() => {
  if (!product.value?.variants || product.value.variants.length === 0) return null
  
  let found = product.value.variants.find(v => 
    (!selectedColorId.value || v.colorId === selectedColorId.value) &&
    (!selectedSizeId.value || v.sizeId === selectedSizeId.value)
  )
  
  if (!found) {
    found = product.value.variants[0]
  }
  
  return found
})

// ✅ Gọi API customer để lấy chi tiết sản phẩm
const loadProductDetail = async () => {
  loading.value = true
  try {
    let productId = route.params.id
    
    if (!productId || productId === 'undefined') {
      console.error("❌ Không có product ID hợp lệ")
      product.value = null
      loading.value = false
      showNotification('Không tìm thấy sản phẩm!', 'error')
      return
    }
    
    console.log("🟡 Đang gọi API customer:", `${API_BASE}/customer/home/products/${productId}`)
    
    // ✅ Gọi API customer
    const response = await axios.get(`${API_BASE}/customer/home/products/${productId}`)
    console.log("🟢 Response:", response.data)
    
    product.value = response.data
    
    // Set default selections
    if (uniqueColors.value.length > 0) {
      selectedColorId.value = uniqueColors.value[0]?.colorId
    }
    if (uniqueSizes.value.length > 0) {
      selectedSizeId.value = uniqueSizes.value[0]?.sizeId
    }
    
  } catch (error) {
    console.error("❌ Lỗi chi tiết:", error)
    product.value = null
    if (error.response?.status === 404) {
      showNotification('Không tìm thấy sản phẩm!', 'error')
    } else {
      showNotification('Không thể tải thông tin sản phẩm!', 'error')
    }
  } finally {
    loading.value = false
  }
}

const addToCart = () => {
  if (!selectedVariant.value || selectedVariant.value.quantity === 0) {
    showNotification('Sản phẩm đã hết hàng!', 'error')
    return
  }
  
  cartStore.addToCart({
    productId: product.value.productId,
    productName: product.value.productName,
    productDetailId: selectedVariant.value.productDetailId,
    price: selectedVariant.value.price,
    quantity: quantity.value,
    color: selectedVariant.value.colorName,
    size: selectedVariant.value.sizeName,
    image: mainImage.value
  })
  showNotification('Đã thêm vào giỏ hàng!', 'success')
}

const buyNow = () => {
  addToCart()
  router.push('/cart')
}

onMounted(() => {
  loadProductDetail()
})
</script>

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
        <div v-else class="product-detail">
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
            <p class="product-description">{{ product.description }}</p>

            <div class="product-price">
              <span class="current-price">{{ formatPrice(product.minPrice) }}</span>
              <span v-if="product.maxPrice !== product.minPrice" class="price-range">
                - {{ formatPrice(product.maxPrice) }}
              </span>
            </div>

            <!-- Color Options -->
            <div class="variant-group" v-if="uniqueColors.length > 0">
              <h4>Màu sắc:</h4>
              <div class="color-options">
                <div 
                  v-for="color in uniqueColors" 
                  :key="color.colorId"
                  class="color-btn"
                  :style="{ backgroundColor: color.hexCode || '#CCCCCC' }"
                  :class="{ active: selectedColorId === color.colorId }"
                  :title="color.colorName"
                  @click="selectedColorId = color.colorId"
                ></div>
              </div>
            </div>

            <!-- Size Options -->
            <div class="variant-group" v-if="uniqueSizes.length > 0">
              <h4>Kích thước:</h4>
              <div class="size-options">
                <button 
                  v-for="size in uniqueSizes" 
                  :key="size.sizeId"
                  class="size-btn"
                  :class="{ active: selectedSizeId === size.sizeId }"
                  @click="selectedSizeId = size.sizeId"
                >
                  {{ size.sizeName }}
                </button>
              </div>
            </div>

            <!-- Selected Variant Info -->
            <div class="selected-variant" v-if="selectedVariant">
              <div class="variant-price">
                Giá: <strong>{{ formatPrice(selectedVariant.price) }}</strong>
              </div>
              <div class="variant-stock" :class="{ 'out-of-stock': !selectedVariant.inStock }">
                <i class="fas" :class="selectedVariant.inStock ? 'fa-check-circle' : 'fa-times-circle'"></i>
                {{ selectedVariant.inStock ? `Còn ${selectedVariant.quantity} sản phẩm` : 'Hết hàng' }}
              </div>
            </div>

            <!-- Quantity -->
            <div class="product-quantity">
              <h4>Số lượng:</h4>
              <div class="quantity-selector">
                <button @click="quantity > 1 && quantity--" :disabled="quantity <= 1">-</button>
                <input type="number" v-model.number="quantity" min="1" :max="selectedVariant?.quantity || 99" />
                <button @click="quantity < (selectedVariant?.quantity || 99) && quantity++">+</button>
              </div>
            </div>

            <!-- Actions -->
            <div class="product-actions">
              <button class="add-to-cart-btn" @click="addToCart" :disabled="!selectedVariant?.inStock">
                <i class="fas fa-shopping-cart"></i> Thêm vào giỏ
              </button>
              <button class="buy-now-btn" @click="buyNow" :disabled="!selectedVariant?.inStock">
                <i class="fas fa-bolt"></i> Mua ngay
              </button>
            </div>
          </div>
        </div>

        <!-- Variants Table -->
        <div class="variants-table" v-if="product?.variants && product.variants.length > 1">
          <h3>Bảng thông số kỹ thuật</h3>
          <div class="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Màu sắc</th>
                  <th>Kích thước</th>
                  <th>SKU</th>
                  <th>Giá</th>
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
                  <td>{{ variant.sku }}</td>
                  <td>{{ formatPrice(variant.price) }}</td>
                  <td :class="{ 'out-of-stock': variant.quantity === 0 }">
                    {{ variant.quantity > 0 ? variant.quantity + ' sản phẩm' : 'Hết hàng' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
    <CartModal />
  </div>
</template>

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

.product-detail {
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

.product-description {
  color: #666;
  line-height: 1.6;
  margin-bottom: 20px;
}

.product-price {
  margin-bottom: 20px;
}

.current-price {
  font-size: 28px;
  font-weight: 700;
  color: #e63946;
}

.price-range {
  font-size: 20px;
  color: #999;
}

.variant-group {
  margin-bottom: 20px;
}

.variant-group h4 {
  color: #2c3e50;
  margin-bottom: 10px;
  font-size: 16px;
}

.color-options {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.color-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.color-btn:hover,
.color-btn.active {
  transform: scale(1.1);
  border-color: #2c3e50;
  box-shadow: 0 0 0 2px white, 0 0 0 4px #e63946;
}

.size-options {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.size-btn {
  width: 50px;
  height: 40px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 600;
}

.size-btn:hover,
.size-btn.active {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.selected-variant {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin: 15px 0;
}

.variant-price {
  font-size: 18px;
  margin-bottom: 5px;
}

.variant-price strong {
  color: #e63946;
  font-size: 22px;
}

.variant-stock {
  color: #28a745;
}

.variant-stock.out-of-stock {
  color: #e63946;
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

.variants-table {
  background: white;
  border-radius: 20px;
  padding: 30px;
  margin-top: 30px;
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

.variants-table table {
  width: 100%;
  border-collapse: collapse;
  min-width: 500px;
}

.variants-table th,
.variants-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.variants-table th {
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

.out-of-stock {
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
    font-size: 24px;
  }

  .product-actions {
    flex-direction: column;
  }
}
</style>