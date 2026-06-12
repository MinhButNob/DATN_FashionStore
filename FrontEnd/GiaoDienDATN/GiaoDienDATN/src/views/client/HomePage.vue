<template>
  <div class="home-page">
    <ClientHeader />

    <main>
      <!-- Banner Slider -->
      <BannerSlider />

      <!-- Category Section -->
      <section class="category-section">
        <div class="container">
          <div class="section-title">
            <h2>DANH MỤC NỔI BẬT</h2>
          </div>
          <div class="category-grid">
            <div
              v-for="category in categories"
              :key="category.categoryId"
              class="category-card"
              @click="goToCategory(category.categoryId)"
            >
              <div class="category-icon">
                <i :class="getCategoryIcon(category.categoryName)"></i>
              </div>
              <h3>{{ category.categoryName }}</h3>
              <p>{{ category.productCount || 0 }} sản phẩm</p>
            </div>
          </div>
        </div>
      </section>

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
            <ProductCard
              v-for="product in latestProducts.slice(0, 9)"
              :key="product.productId"
              :product="product"
            />
          </div>
        </div>
      </section>

      <!-- Empty State -->
      <div v-else class="empty-state">
        <div class="empty-icon">📦</div>
        <h3>Chưa có sản phẩm nào</h3>
        <p>Hãy thêm sản phẩm đầu tiên trong Admin Panel</p>
        <router-link to="/admin/products" class="btn-primary">
          Đến Admin
        </router-link>
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
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useNotification } from "../../composables/useNotification";
import ClientHeader from "../../components/client/ClientHeader.vue";
import ClientFooter from "../../components/client/ClientFooter.vue";
import ProductCard from "../../components/client/ProductCard.vue";
import BannerSlider from "../../components/client/BannerSlider.vue";
import CartModal from "../../components/client/CartModal.vue";
import axios from "axios";

const router = useRouter();
const { showNotification } = useNotification();
const API_BASE = "http://localhost:8080/api";
const IMAGE_BASE = "http://localhost:8080";

const loading = ref(true);
const latestProducts = ref([]);
const categories = ref([]);
const newsletterEmail = ref("");

const getCategoryIcon = (categoryName) => {
  const icons = {
    "Áo thun": "fas fa-tshirt",
    "Áo Polo": "fas fa-tshirt",
    "Quần Jeans": "fas fa-vest",
    "Áo Sơ Mi": "fas fa-shirt",
    "Đầm": "fas fa-dress",
    "Phụ kiện": "fas fa-glasses",
  };
  return icons[categoryName] || "fas fa-tag";
};

const goToCategory = (categoryId) => {
  router.push(`/shop?categoryId=${categoryId}`);
};

const loadCategories = async () => {
  try {
    const response = await axios.get(`${API_BASE}/categories/active`);
    categories.value = response.data || [];
    console.log("✅ Categories loaded:", categories.value.length);
  } catch (error) {
    console.error("Lỗi tải danh mục:", error);
    categories.value = [];
  }
};

const loadProducts = async () => {
  loading.value = true;
  try {
    console.log("🟡 Đang gọi API lấy sản phẩm...");
    
    const response = await axios.get(`${API_BASE}/products`, {
      params: {
        page: 0,
        size: 20,
        sortBy: "productId",
        sortDir: "DESC"
      }
    });
    
    // ✅ IN RA CẤU TRÚC DỮ LIỆU CHI TIẾT
    console.log("🔍 FULL RESPONSE:", JSON.stringify(response.data, null, 2));
    
    let products = [];
    if (response.data?.data?.content) {
      products = response.data.data.content;
    } else if (response.data?.content) {
      products = response.data.content;
    }
    
    console.log("📦 Số lượng sản phẩm:", products.length);
    
    // ✅ IN CHI TIẾT TỪNG SẢN PHẨM
    products.forEach((product, index) => {
      console.log(`\n========== SẢN PHẨM ${index + 1} ==========`);
      console.log("productId:", product.productId);
      console.log("productName:", product.productName);
      console.log("description:", product.description);
      console.log("status:", product.status);
      console.log("totalStock:", product.totalStock);
      console.log("category:", product.category);
      console.log("variants:", product.variants);
      console.log("images:", product.images);
      console.log("createdAt:", product.createdAt);
      console.log("=====================================\n");
    });
    
    // Thử hiển thị trực tiếp trên giao diện để debug
    latestProducts.value = products;
    
  } catch (error) {
    console.error("❌ Lỗi:", error);
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

.category-section {
  padding: 40px 0;
}

.section-title {
  text-align: center;
  margin-bottom: 40px;
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

.category-card h3 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.category-card p {
  color: #7f8c8d;
  font-size: 14px;
}

.loading-state {
  text-align: center;
  padding: 60px;
}

.spinner {
  width: 40px;
  height: 40px;
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

.product-section {
  padding: 40px 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
}

.empty-state {
  text-align: center;
  padding: 60px;
  background: white;
  border-radius: 15px;
  margin: 20px;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 20px;
  color: #ccc;
}

.empty-state h3 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.empty-state p {
  color: #7f8c8d;
  margin-bottom: 20px;
}

.empty-state .btn-primary {
  display: inline-block;
  padding: 12px 24px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 600;
}

.newsletter {
  background: white;
  padding: 60px 0;
  text-align: center;
  margin-top: 40px;
}

.newsletter h2 {
  font-size: 32px;
  color: #2c3e50;
  margin-bottom: 15px;
}

.newsletter p {
  color: #7f8c8d;
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
  border: 1px solid #ddd;
  border-radius: 8px;
  outline: none;
  font-size: 16px;
}

.newsletter-form input:focus {
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
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
}

.btn-primary:hover {
  background: linear-gradient(135deg, #c82333, #e63946);
  transform: translateY(-2px);
}

@media (max-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .section-title h2 {
    font-size: 28px;
  }

  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .product-grid {
    grid-template-columns: 1fr;
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
}
</style>