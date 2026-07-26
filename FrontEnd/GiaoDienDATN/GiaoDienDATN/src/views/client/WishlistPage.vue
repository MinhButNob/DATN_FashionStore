<template>
  <div class="wishlist-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <h1 class="page-title">SẢN PHẨM YÊU THÍCH</h1>

        <div v-if="wishlistStore.loading" class="empty-wishlist">
          <p>Đang tải danh sách yêu thích...</p>
        </div>

        <div v-else-if="wishlistItems.length === 0" class="empty-wishlist">
          <i class="fas fa-heart"></i>
          <h2>Danh sách yêu thích trống</h2>
          <p>Hãy thêm những sản phẩm bạn yêu thích vào danh sách này</p>
          <router-link to="/shop" class="btn btn-primary">
            Khám phá sản phẩm
          </router-link>
        </div>

        <div v-else class="wishlist-grid">
          <div v-for="item in wishlistItems" :key="item.wishlistItemId" class="wishlist-card">
            <div class="product-image" @click="goToProduct(item.productId)">
              <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.productName" class="wishlist-img" />
              <i v-else class="fas fa-tshirt"></i>
              <button class="remove-btn" @click.stop="removeFromWishlist(item.productDetailId)">
                <i class="fas fa-times"></i>
              </button>
            </div>

            <div class="product-info">
              <h3 @click="goToProduct(item.productId)">{{ item.productName }}</h3>
              <div class="product-price">
                <span class="current-price">{{ formatPrice(item.discountedPrice || item.price) }}</span>
                <span v-if="item.discountedPrice && item.discountedPrice < item.price" class="old-price">
                  {{ formatPrice(item.price) }}
                </span>
              </div>

              <div class="product-stock" :class="{ 'out-of-stock': (item.stock || 0) === 0 }">
                {{ (item.stock || 0) > 0 ? `Còn ${item.stock} sản phẩm` : 'Hết hàng' }}
              </div>

              <div class="product-actions">
                <button class="add-to-cart" @click="addToCart(item)" :disabled="(item.stock || 0) === 0">
                  <i class="fas fa-shopping-cart"></i>
                  Thêm vào giỏ
                </button>
              </div>
            </div>
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
    <CartModal />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useCartStore } from '../../stores/cartStore'
import { useWishlistStore } from '../../stores/wishlistStore'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import ProductCard from './ProductCard.vue'
import CartModal from '../../components/client/CartModal.vue'
import { formatPrice } from '../../utils/helpers'

const API_BASE = 'http://localhost:8080/api'

const router = useRouter()
const cartStore = useCartStore()
const wishlistStore = useWishlistStore()
const { showNotification } = useNotification()

const allProducts = ref([])

const fetchSuggestedProducts = async () => {
  try {
    const response = await axios.get(`${API_BASE}/customer/home/latest-products`, {
      params: { page: 0, size: 8 }
    })
    allProducts.value = response.data?.products || []
  } catch (error) {
    console.error('Lỗi tải sản phẩm gợi ý:', error)
    allProducts.value = []
  }
}

onMounted(() => {
  wishlistStore.fetchWishlist()
  fetchSuggestedProducts()
})

const wishlistItems = computed(() => wishlistStore.items)

const suggestedProducts = computed(() => {
  const wishlistProductIds = wishlistItems.value.map(item => item.productId)
  return allProducts.value
    .filter(p => !wishlistProductIds.includes(p.productId))
    .slice(0, 4)
})

const goToProduct = (productId) => {
  router.push(`/product/${productId}`)
}

const addToCart = async (item) => {
  try {
    await cartStore.addToCart({
      id: item.productDetailId,
      productDetailId: item.productDetailId,
      productId: item.productId,
      productName: item.productName,
      name: item.productName,
      price: item.price,
      discountedPrice: item.discountedPrice,
      quantity: 1,
      image: item.imageUrl,
      imageUrl: item.imageUrl,
      size: item.sizeName,
      color: item.colorName,
      stock: item.stock,
    })
    showNotification('Đã thêm vào giỏ hàng', 'success')
  } catch (error) {
    console.error('Add to cart error:', error)
    showNotification('Thêm vào giỏ thất bại!', 'error')
  }
}

const removeFromWishlist = async (productDetailId) => {
  try {
    await wishlistStore.removeFromWishlist(productDetailId)
    showNotification('Đã xóa khỏi danh sách yêu thích', 'info')
  } catch (error) {
    console.error('Remove wishlist error:', error)
    showNotification('Xóa thất bại, vui lòng thử lại!', 'error')
  }
}
</script>

<style scoped>
.wishlist-page {
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

.empty-wishlist {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
}

.empty-wishlist i {
  font-size: 80px;
  color: var(--medium-gray);
  margin-bottom: 20px;
}

.empty-wishlist h2 {
  color: var(--dark-gray);
  margin-bottom: 10px;
}

.empty-wishlist p {
  color: var(--medium-gray);
  margin-bottom: 30px;
}

.wishlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 25px;
  margin-bottom: 50px;
}

.wishlist-card {
  background: white;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: var(--shadow);
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}

.wishlist-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-hover);
}

.product-image {
  height: 200px;
  background: var(--light-gray);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.product-image i {
  font-size: 80px;
  color: var(--medium-gray);
  transition: transform 0.3s;
}

.wishlist-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.wishlist-card:hover .product-image i {
  transform: scale(1.1);
}

.remove-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;       /* Thu nhỏ từ 35px xuống 24px */
  height: 24px;      /* Thu nhỏ từ 35px xuống 24px */
  background: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
  font-size: 10px;   /* Thêm dòng này để icon nhỏ lại */
  padding: 0;
}

.remove-btn:hover {
  background: var(--primary-red);
  color: white;
  transform: rotate(90deg);
}

/* Icon bên trong nút */
.remove-btn i {
  font-size: 10px;   /* Thu nhỏ icon "✕" */
}

.product-info {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-info h3 {
  font-size: 16px;
  color: var(--dark-gray);
  margin-bottom: 10px;
  cursor: pointer;
  transition: color 0.3s;
  line-height: 1.4;
}

.product-info h3:hover {
  color: var(--primary-red);
}

.product-price {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.current-price {
  font-size: 20px;
  font-weight: 700;
  color: var(--primary-red);
}

.old-price {
  font-size: 14px;
  color: var(--medium-gray);
  text-decoration: line-through;
}

.product-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
}

.stars {
  color: #ffc107;
  font-size: 13px;
}

.product-rating span {
  color: var(--medium-gray);
  font-size: 13px;
}

.product-actions {
  display: flex;
  gap: 10px;
  margin-top: auto;
}

.add-to-cart {
  flex: 1;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  border: none;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
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

.share-btn {
  width: 45px;
  background: var(--light-gray);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 16px;
}

.share-btn:hover {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
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

  .wishlist-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .page-title {
    font-size: 24px;
  }
}

@media (max-width: 480px) {
  .wishlist-grid {
    grid-template-columns: 1fr;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }

  .product-actions {
    flex-direction: column;
  }

  .share-btn {
    width: 100%;
  }
}
</style>