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
              :key="category.id"
              class="category-card"
              @click="goToCategory(category.slug)"
            >
              <div class="category-icon">
                <i :class="category.icon"></i>
              </div>
              <h3>{{ category.name }}</h3>
              <p>{{ category.count }} sản phẩm</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Filter Section -->
      <div class="container">
        <FilterBar />
      </div>

      <!-- Product Grid -->
      <section class="product-section">
        <div class="container">
          <div class="product-container" :class="{ 'list-view': !gridView }">
            <ProductCard
              v-for="product in productStore.filteredProducts"
              :key="product.id"
              :product="product"
            />
          </div>
        </div>
      </section>

      <!-- Featured Products -->
      <section class="featured-section">
        <div class="container">
          <div class="featured-title">
            <h2>SẢN PHẨM NỔI BẬT</h2>
            <p>Những sản phẩm được yêu thích nhất trong tháng</p>
          </div>
          <div class="featured-grid">
            <ProductCard
              v-for="product in productStore.featuredProducts"
              :key="product.id"
              :product="product"
            />
          </div>
        </div>
      </section>

      <!-- Blog Section -->
      <section class="blog-section">
        <div class="container">
          <div class="section-title">
            <h2>TIN TỨC THỜI TRANG</h2>
          </div>
          <div class="blog-grid">
            <div v-for="post in blogPosts" :key="post.id" class="blog-card">
              <div class="blog-image">
                <i :class="post.icon"></i>
              </div>
              <div class="blog-content">
                <div class="blog-date">
                  <i class="far fa-calendar-alt"></i> {{ post.date }}
                </div>
                <div class="blog-title">{{ post.title }}</div>
                <div class="blog-excerpt">{{ post.excerpt }}</div>
                <a href="#" class="read-more" @click.prevent="showBlog(post.id)">
                  Đọc tiếp <i class="fas fa-arrow-right"></i>
                </a>
              </div>
            </div>
          </div>
        </div>
      </section>

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
            >
            <button class="btn btn-primary" @click="subscribeNewsletter">
              Đăng ký
            </button>
          </div>
        </div>
      </section>
    </main>

    <ClientFooter />
    <CartModal />
    <AuthModal />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProductStore } from '../../stores/productStore'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import ProductCard from '../../components/client/ProductCard.vue'
import BannerSlider from '../../components/client/BannerSlider.vue'
import FilterBar from '../../components/client/FilterBar.vue'
import CartModal from '../../components/client/CartModal.vue'
import AuthModal from '../../components/client/AuthModal.vue'

const router = useRouter()
const productStore = useProductStore()
const { showNotification } = useNotification()

const gridView = ref(true)
const newsletterEmail = ref('')

const categories = [
  { id: 1, name: 'Thời trang nam', slug: 'men', icon: 'fas fa-user-tie', count: 234 },
  { id: 2, name: 'Thời trang nữ', slug: 'women', icon: 'fas fa-female', count: 456 },
  { id: 3, name: 'Trẻ em', slug: 'kids', icon: 'fas fa-child', count: 123 },
  { id: 4, name: 'Phụ kiện', slug: 'accessories', icon: 'fas fa-glasses', count: 345 },
  { id: 5, name: 'Khuyến mãi', slug: 'sale', icon: 'fas fa-tags', count: 89 }
]

const blogPosts = [
  {
    id: 1,
    title: 'Xu hướng thời trang hè 2024',
    excerpt: 'Những xu hướng thời trang nổi bật nhất trong mùa hè năm nay...',
    date: '15/03/2024',
    icon: 'fas fa-tshirt'
  },
  {
    id: 2,
    title: 'Cách phối đồ với giày thể thao',
    excerpt: 'Bí quyết phối đồ với giày thể thao cho nam và nữ...',
    date: '12/03/2024',
    icon: 'fas fa-shoe-prints'
  },
  {
    id: 3,
    title: 'Túi xách công sở cho nữ',
    excerpt: 'Lựa chọn túi xách phù hợp cho môi trường công sở...',
    date: '10/03/2024',
    icon: 'fas fa-handbag'
  }
]

const goToCategory = (slug) => {
  router.push(`/shop?category=${slug}`)
}

const showBlog = (id) => {
  router.push(`/blog/${id}`)
}

const subscribeNewsletter = () => {
  if (newsletterEmail.value) {
    showNotification('Cảm ơn bạn đã đăng ký nhận tin!', 'success')
    newsletterEmail.value = ''
  } else {
    showNotification('Vui lòng nhập email!', 'warning')
  }
}
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
  color: var(--dark-gray);
  display: inline-block;
  padding: 0 20px;
  background: var(--light-gray);
  position: relative;
  z-index: 1;
}

.section-title::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--primary-red), transparent);
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
  transition: var(--transition);
  box-shadow: var(--shadow);
}

.category-card:hover {
  transform: translateY(-10px);
  box-shadow: var(--shadow-hover);
}

.category-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
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
  color: var(--dark-gray);
  margin-bottom: 10px;
}

.category-card p {
  color: var(--medium-gray);
  font-size: 14px;
}

.product-section {
  padding: 40px 0;
}

.product-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
}

.product-container.list-view {
  grid-template-columns: 1fr;
}

.featured-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 0;
  margin: 40px 0;
}

.featured-title {
  text-align: center;
  color: white;
  margin-bottom: 40px;
}

.featured-title h2 {
  font-size: 36px;
  margin-bottom: 10px;
}

.featured-title p {
  font-size: 18px;
  opacity: 0.9;
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 25px;
}

.blog-section {
  padding: 40px 0;
}

.blog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
}

.blog-card {
  background: white;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: var(--shadow);
  transition: var(--transition);
}

.blog-card:hover {
  transform: translateY(-10px);
  box-shadow: var(--shadow-hover);
}

.blog-image {
  height: 200px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  display: flex;
  align-items: center;
  justify-content: center;
}

.blog-image i {
  font-size: 60px;
  color: white;
}

.blog-content {
  padding: 20px;
}

.blog-date {
  color: var(--primary-red);
  font-size: 13px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.blog-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 10px;
  color: var(--dark-gray);
}

.blog-excerpt {
  color: var(--medium-gray);
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 15px;
}

.read-more {
  color: var(--primary-red);
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  transition: var(--transition);
}

.read-more:hover {
  gap: 10px;
}

.newsletter {
  background: white;
  padding: 60px 0;
  text-align: center;
}

.newsletter h2 {
  font-size: 32px;
  color: var(--dark-gray);
  margin-bottom: 15px;
}

.newsletter p {
  color: var(--medium-gray);
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
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  font-size: 16px;
}

.newsletter-form input:focus {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

@media (max-width: 1024px) {
  .featured-grid {
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
  
  .product-container {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 20px;
  }
  
  .featured-grid {
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
  
  .product-container {
    grid-template-columns: 1fr;
  }
}
</style>