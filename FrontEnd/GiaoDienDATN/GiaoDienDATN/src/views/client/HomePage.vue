<template>
  <div class="home-page">
    <ClientHeader />

    <main>
      <BannerSlider />

      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Đang tải sản phẩm...</p>
      </div>

      <!-- Product Grid -->
      <section v-else-if="latestProducts.length > 0" class="product-section">
        <div class="container">
          <div class="section-title">
            <h2>SẢN PHẨM MỚI</h2>
          </div>
          
          <div class="product-grid">
            <div 
              v-for="product in latestProducts" 
              :key="product.productId"
              class="product-card-direct"
              @click="goToProductDetail(product.productId)"
            >
              <div class="product-image-direct">
                <img 
                  v-if="product.mainImageUrl" 
                  :src="product.mainImageUrl" 
                  :alt="product.productName" 
                  class="product-img-direct"
                  @error="handleImageError"
                />
                <div v-else class="no-image-placeholder">
                  <i class="fas fa-tshirt"></i>
                </div>
              </div>
              <div class="product-info-direct">
                <div class="product-category-direct">{{ product.categoryName || 'Sản phẩm' }}</div>
                <h3 class="product-name-direct">{{ product.productName }}</h3>
                <div class="product-price-direct">
                  <span class="current-price-direct">
                    {{ formatPriceVND(product.minPrice) }}
                  </span>
                  <span v-if="product.maxPrice && product.maxPrice !== product.minPrice" class="price-range-direct">
                    - {{ formatPriceVND(product.maxPrice) }}
                  </span>
                </div>
                <div class="product-stock-direct" :class="{ 'out-of-stock': (product.totalStock || 0) === 0 }">
                  <i class="fas" :class="(product.totalStock || 0) > 0 ? 'fa-check-circle' : 'fa-times-circle'"></i>
                  {{ (product.totalStock || 0) > 0 ? `Còn ${product.totalStock} sản phẩm` : 'Hết hàng' }}
                </div>
                <button class="add-to-cart-direct" @click.stop="addToCartDirect(product)" :disabled="(product.totalStock || 0) === 0">
                  <i class="fas fa-shopping-cart"></i> Thêm vào giỏ
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Empty State -->
      <div v-else-if="!loading && latestProducts.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <h3>Chưa có sản phẩm nào</h3>
        <p>Hiện tại cửa hàng chưa có sản phẩm. Vui lòng quay lại sau!</p>
        <button class="btn-reload" @click="reloadPage">
          <i class="fas fa-sync-alt"></i> Tải lại trang
        </button>
      </div>

      <!-- Newsletter -->
      <section class="newsletter">
        <div class="container">
          <h2>ĐĂNG KÝ NHẬN TIN</h2>
          <p>Nhận thông tin về sản phẩm mới và khuyến mãi đặc biệt</p>
          <div class="newsletter-form">
            <input
              type="email"
              v-model="newsletterEmail"
              placeholder="Nhập email của bạn"
            />
            <button class="btn btn-primary" @click="subscribeNewsletter">
              Đăng ký
            </button>
          </div>
        </div>
      </section>
    </main>

    <ClientFooter />
    <CartModal />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { useNotification } from "../../composables/useNotification";
import ClientHeader from "../../components/client/ClientHeader.vue";
import ClientFooter from "../../components/client/ClientFooter.vue";
import BannerSlider from "../../components/client/BannerSlider.vue";
import CartModal from "../../components/client/CartModal.vue";
import { useCartStore } from "../../stores/cartStore";
import { useAuthStore } from "../../stores/authStore";
import api from "@/api/axios";

const router = useRouter();
const { showNotification } = useNotification();
const cartStore = useCartStore();
const authStore = useAuthStore();
const loading = ref(true);
const latestProducts = ref([]);
const categories = ref([]);
const newsletterEmail = ref("");

// Lấy customerId từ auth store
const customerId = computed(() => {
  const user = authStore.user || JSON.parse(localStorage.getItem('userInfo') || '{}');
  return user.customerId || user.userId || null;
});

const formatPriceVND = (price) => {
  if (price === undefined || price === null) return '0₫'
  const numPrice = typeof price === 'number' ? price : Number(price)
  if (isNaN(numPrice) || numPrice === 0) return '0₫'
  return numPrice.toLocaleString('vi-VN') + '₫'
}

const handleImageError = (event) => {
  event.target.style.display = 'none'
  const placeholder = event.target.parentElement.querySelector('.no-image-placeholder')
  if (placeholder) placeholder.style.display = 'flex'
}

const getCategoryIcon = (categoryName) => {
  if (!categoryName) return "fas fa-tag";
  const icons = {
    "Áo thun": "fas fa-tshirt",
    "Áo Polo": "fas fa-tshirt",
    "Quần Jeans": "fas fa-vest",
    "Áo Sơ Mi": "fas fa-shirt",
    "Đầm": "fas fa-dress",
    "Phụ kiện": "fas fa-glasses" };
  return icons[categoryName] || "fas fa-tag";
};

const goToCategory = (categoryId) => {
  router.push(`/shop?categoryId=${categoryId}`);
};

const goToProductDetail = (productId) => {
  if (productId) {
    router.push(`/product/${productId}`);
  }
};

const addToCartDirect = (product) => {
  if (!authStore.isAuthenticated) {
    window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'login' }))
    showNotification('Vui lòng đăng nhập để thêm vào giỏ hàng', 'info')
    return
  }

  const stock = product.totalStock || 0
  if (stock === 0) {
    showNotification('Sản phẩm đã hết hàng!', 'error')
    return
  }
  
  const price = product.minPrice || 0;
  cartStore.addToCart({
    productId: product.productId,
    productName: product.productName,
    price: price,
    quantity: 1,
    image: product.mainImageUrl
  });
  showNotification('Đã thêm sản phẩm vào giỏ hàng!', 'success');
};

const reloadPage = () => {
  showNotification('Đang tải lại dữ liệu...', 'info');
  loadProducts();
  loadCategories();
};

// Tên 3 danh mục lớn mặc định - dùng để lọc, khớp với logic ở
// src/stores/categoryHierarchyStores.js (DEFAULT_LOCKED_NAMES)
const DEFAULT_LOCKED_NAMES = ["Áo", "Quần", "Phụ kiện"];

const loadCategories = async () => {
  try {
    // Lấy cây danh mục (chỉ ACTIVE) từ module Phân cấp danh mục, sau đó chỉ giữ
    // lại đúng 3 danh mục lớn mặc định - KHÔNG hiển thị danh mục con hay bất kỳ
    // danh mục độc lập nào khác (được tạo ở "Quản lý danh mục" nhưng chưa gán vào 3 danh mục lớn).
    const response = await api.get(`/category-hierarchy/tree/active`);
    let categoriesData = response.data || [];

    if (!Array.isArray(categoriesData)) {
      categoriesData = [];
    }

    categoriesData = categoriesData.filter(
      (cat) => cat && cat.categoryId && DEFAULT_LOCKED_NAMES.includes(cat.categoryName)
    );

    console.log("✅ Categories loaded:", categoriesData);
    
    const productsResponse = await api.get(`/products`, {
      params: { page: 0, size: 1000 }
    });
    
    let allProducts = [];
    if (productsResponse.data?.data?.content) {
      allProducts = productsResponse.data.data.content;
    } else if (productsResponse.data?.content) {
      allProducts = productsResponse.data.content;
    } else if (Array.isArray(productsResponse.data)) {
      allProducts = productsResponse.data;
    }
    
    const productCountByCategory = {};
    allProducts.forEach(product => {
      const catId = product.categoryId || product.category?.categoryId;
      if (catId) {
        productCountByCategory[catId] = (productCountByCategory[catId] || 0) + 1;
      }
    });
    
    categories.value = categoriesData
      .filter(cat => cat && cat.categoryId)
      .map(cat => ({
        ...cat,
        productCount: productCountByCategory[cat.categoryId] || 0
      }));
    
    console.log("✅ Categories with counts:", categories.value);
  } catch (error) {
    console.error("Lỗi tải danh mục:", error);
    categories.value = [];
  }
};

const loadProducts = async () => {
  loading.value = true;
  try {
    console.log("🟡 Đang gọi API:", `/customer/home/latest-products`);
    
    const response = await api.get(`/customer/home/latest-products`, {
      params: {
        page: 0,
        size: 12
      }
    });
    
    console.log("🔍 Response:", response.data);
    
    let products = [];
    
    if (response.data?.products) {
      products = response.data.products;
    } else if (response.data?.data?.products) {
      products = response.data.data.products;
    } else if (Array.isArray(response.data)) {
      products = response.data;
    } else if (response.data?.content) {
      products = response.data.content;
    }
    
    if (!Array.isArray(products)) {
      products = [];
    }
    
    products = products.map(p => ({
      ...p,
      minPrice: Number(p.minPrice) || 0,
      maxPrice: Number(p.maxPrice) || 0,
      totalStock: Number(p.totalStock) || 0
    }));
    
    console.log(`✅ Đã tải ${products.length} sản phẩm`);
    
    latestProducts.value = products;
    
  } catch (error) {
    console.error("❌ Lỗi tải sản phẩm:", error);
    latestProducts.value = [];
  } finally {
    loading.value = false;
  }
};

const subscribeNewsletter = () => {
  if (newsletterEmail.value) {
    showNotification("Cảm ơn bạn đã đăng ký nhận tin!", "success");
    newsletterEmail.value = "";
  } else {
    showNotification("Vui lòng nhập email!", "warning");
  }
};

onMounted(() => {
  loadCategories();
  loadProducts();
});
</script>


<style scoped>
.home-page {
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* ===== CATEGORY SECTION ===== */
.category-section {
  padding: 60px 0;
  background: #f8f9fa;
}

.section-title {
  text-align: center;
  margin-bottom: 50px;
  position: relative;
}

.section-title h2 {
  font-size: 36px;
  color: #2c3e50;
  display: inline-block;
  padding: 0 20px;
  background: #f8f9fa;
  position: relative;
  z-index: 1;
}

.section-title::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #e63946, transparent);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 25px;
}

.category-card {
  background: white;
  border-radius: 15px;
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.category-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
}

.category-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.category-icon i {
  font-size: 40px;
  color: white;
}

/* ===== PRODUCT SECTION ===== */
.product-section {
  padding: 60px 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
}

/* ===== PRODUCT CARD ===== */
.product-card-direct {
  background: white;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  cursor: pointer;
}

.product-card-direct:hover {
  transform: translateY(-10px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.product-image-direct {
  height: 280px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.product-img-direct {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.no-image-placeholder i {
  font-size: 80px;
  color: #ccc;
}

.product-info-direct {
  padding: 20px;
}

.product-category-direct {
  color: #e63946;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  margin-bottom: 8px;
}

.product-name-direct {
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

.product-price-direct {
  margin-bottom: 12px;
}

.current-price-direct {
  font-size: 18px;
  font-weight: 700;
  color: #e63946;
}

.price-range-direct {
  font-size: 14px;
  color: #999;
}

.product-stock-direct {
  font-size: 12px;
  margin-bottom: 15px;
  color: #28a745;
}

.product-stock-direct.out-of-stock {
  color: #e63946;
}

.product-stock-direct i {
  margin-right: 5px;
}

.add-to-cart-direct {
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

.add-to-cart-direct:hover:not(:disabled) {
  background: linear-gradient(135deg, #c82333, #e63946);
  transform: translateY(-2px);
}

.add-to-cart-direct:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* ===== LOADING STATE ===== */
.loading-state {
  text-align: center;
  padding: 80px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #e63946;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ===== EMPTY STATE - CHỈ CÒN NÚT TẢI LẠI ===== */
.empty-state {
  text-align: center;
  padding: 80px 40px;
  background: white;
  border-radius: 20px;
  margin: 40px 0;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.05);
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 28px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.empty-state p {
  color: #666;
  font-size: 16px;
  margin-bottom: 30px;
}

.btn-reload {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-reload:hover {
  transform: translateY(-3px);
  box-shadow: 0 5px 20px rgba(230, 57, 70, 0.3);
}

.btn-reload i {
  font-size: 18px;
}

/* ===== NEWSLETTER ===== */
.newsletter {
  background: linear-gradient(135deg, #2c3e50, #1a252f);
  padding: 80px 0;
  text-align: center;
  color: white;
  margin-top: 40px;
}

.newsletter h2 {
  font-size: 32px;
  margin-bottom: 15px;
}

.newsletter p {
  opacity: 0.9;
  margin-bottom: 30px;
}

.newsletter-form {
  max-width: 500px;
  margin: 0 auto;
  display: flex;
  gap: 10px;
}

.newsletter-form input {
  flex: 1;
  padding: 15px;
  border: none;
  border-radius: 8px;
  outline: none;
  font-size: 16px;
}

.btn-primary {
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border: none;
  padding: 15px 30px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s;
  text-decoration: none;
  display: inline-block;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #c82333, #e63946);
  transform: translateY(-2px);
}

/* ===== RESPONSIVE ===== */
@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
  
  .product-image-direct {
    height: 220px;
  }
  
  .product-name-direct {
    font-size: 14px;
    height: 38px;
  }
  
  .current-price-direct {
    font-size: 16px;
  }

  .empty-state {
    padding: 40px 20px;
  }

  .empty-state h3 {
    font-size: 22px;
  }

  .btn-reload {
    width: 100%;
    justify-content: center;
  }

  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .newsletter-form {
    flex-direction: column;
    padding: 0 20px;
  }
}

@media (max-width: 480px) {
  .category-grid {
    grid-template-columns: 1fr;
  }

  .section-title h2 {
    font-size: 24px;
  }
}
</style>