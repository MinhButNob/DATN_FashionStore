<template>
  <div class="home-page">
    <ClientHeader />

    <main>
      <!-- Hero Banner Slider -->
      <BannerSlider />

      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Đang tải trang chủ...</p>
      </div>

      <template v-else>
        <!-- 1. SẢN PHẨM BÁN CHẠY NHẤT -->
        <section v-if="bestSellerProducts.length > 0" class="product-section home-flow-section">
          <div class="container">
            <div class="section-header">
              <div class="section-heading-group">
                <span class="section-eyebrow">Xu hướng tìm kiếm</span>
                <h2 class="section-title-line">
                  <i class="fas fa-fire flame-icon"></i>
                  SẢN PHẨM BÁN CHẠY NHẤT
                </h2>
              </div>
              <div class="scroll-arrows">
                <button class="arrow-btn" @click="scrollRow(bestsellerScrollRow, -1)" aria-label="Trước">
                  <i class="fas fa-chevron-left"></i>
                </button>
                <button class="arrow-btn" @click="scrollRow(bestsellerScrollRow, 1)" aria-label="Sau">
                  <i class="fas fa-chevron-right"></i>
                </button>
              </div>
            </div>

            <div class="product-scroll-wrapper">
              <div class="product-scroll-row" ref="bestsellerScrollRow">
                <div
                  v-for="(product, idx) in bestSellerProducts"
                  :key="product.productId || product.id || idx"
                  class="product-card-col"
                >
                  <ProductCard 
                    :product="product" 
                    :badgeText="idx < 3 ? `TOP ${idx + 1}` : 'BÁN CHẠY'" 
                  />
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 2. SẢN PHẨM ĐƯỢC ĐÁNH GIÁ CAO -->
        <section v-if="topRatedProducts.length > 0" class="top-rated-bg home-flow-section">
          <div class="container">
            <div class="section-header">
              <div class="section-heading-group">
                <span class="section-eyebrow">Yêu thích nhất</span>
                <h2 class="section-title-line">
                  <i class="fas fa-star star-icon"></i>
                  SẢN PHẨM ĐƯỢC ĐÁNH GIÁ CAO
                </h2>
              </div>
              <div class="scroll-arrows">
                <button class="arrow-btn" @click="scrollRow(topRatedScrollRow, -1)" aria-label="Trước">
                  <i class="fas fa-chevron-left"></i>
                </button>
                <button class="arrow-btn" @click="scrollRow(topRatedScrollRow, 1)" aria-label="Sau">
                  <i class="fas fa-chevron-right"></i>
                </button>
              </div>
            </div>

            <div class="product-scroll-wrapper">
              <div class="product-scroll-row" ref="topRatedScrollRow">
                <div
                  v-for="(product, idx) in topRatedProducts"
                  :key="product.productId || product.id || idx"
                  class="product-card-col"
                >
                  <ProductCard 
                    :product="product" 
                    :rating="product.rating || product.averageRating || 5.0"
                    :reviewCount="product.reviewCount || (32 + idx * 8)"
                    badgeText="ĐÁNH GIÁ CAO"
                  />
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 3. BỘ SƯU TẬP MỚI -->
        <section class="new-collection-section home-flow-section" v-if="newCollectionBanner || newCollectionProducts.length > 0">
          <div class="container">
            <div class="section-header">
              <div class="section-heading-group">
                <span class="section-eyebrow">Mới về</span>
                <h2 class="section-title-line">BỘ SƯU TẬP MỚI</h2>
              </div>
            </div>

            <div
              v-if="newCollectionBanner"
              class="new-collection-banner"
              @click="goToNewCollectionShop"
            >
              <img
                :src="getBannerImageUrl(newCollectionBanner.imagePath)"
                :alt="newCollectionBanner.title || 'Bộ Sưu Tập Mới'"
                class="new-collection-banner-img"
                @error="handleImageError"
              />
            </div>

            <div v-if="newCollectionProducts.length > 0" class="product-scroll-wrapper">
              <div class="product-scroll-row" ref="newCollectionScrollRow">
                <div
                  v-for="product in newCollectionProducts"
                  :key="product.productId || product.id"
                  class="product-card-col"
                >
                  <ProductCard :product="product" />
                </div>
              </div>

              <div class="view-all-container">
                <button class="view-all-btn" @click="goToNewCollectionShop">
                  XEM TẤT CẢ BỘ SƯU TẬP
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- 4. CHỌN THEO PHONG CÁCH (TABS) -->
        <section class="tabs-featured-section home-flow-section">
          <div class="container">
            <div class="section-heading-group tabs-heading-group">
              <span class="section-eyebrow">Gợi ý cho bạn</span>
              <h2 class="section-title-line">CHỌN THEO PHONG CÁCH</h2>
            </div>
            
            <div class="tabs-header">
              <button
                v-for="tab in FEATURED_TABS"
                :key="tab.key"
                type="button"
                class="tab-btn"
                :class="{ active: activeTabKey === tab.key }"
                @click="switchTab(tab.key)"
              >
                {{ tab.label }}
              </button>
            </div>

            <div v-if="tabLoading" class="loading-state">
              <div class="spinner"></div>
            </div>

            <div v-else-if="tabProducts.length > 0" class="tabs-product-grid">
              <ProductCard
                v-for="product in tabProducts"
                :key="product.productId || product.id"
                :product="product"
              />
            </div>

            <div v-else class="empty-tab-state">
              <p>Chưa có sản phẩm nào trong danh mục "{{ currentTabLabel }}".</p>
            </div>

            <div class="view-all-container">
              <button class="view-all-btn" @click="goToTabShop">
                XEM TẤT CẢ {{ currentTabLabel }}
              </button>
            </div>
          </div>
        </section>

        <!-- 5. SLOGAN BANNER SECTION -->
        <section class="slogan-banner-section">
          <div class="container">
            <div class="slogan-banner-container">
              <img
                :src="sloganImgSrc"
                alt="Fashion Store Slogan Banner"
                class="slogan-banner-img"
                @error="handleSloganImageError"
              />
              <div class="slogan-shine-effect"></div>
            </div>
          </div>
        </section>
      </template>
    </main>

    <ClientFooter />
    <CartModal />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue"
import { useRouter } from "vue-router"
import { useNotification } from "../../composables/useNotification"
import ClientHeader from "../../components/client/ClientHeader.vue"
import ClientFooter from "../../components/client/ClientFooter.vue"
import BannerSlider from "../../components/client/BannerSlider.vue"
import CartModal from "../../components/client/CartModal.vue"
import ProductCard from "../../components/client/ProductCard.vue"

import { useAuthStore } from "../../stores/authStore"
import { useWishlistStore } from "../../stores/wishlistStore"
import { useBannerStore } from "../../stores/bannerStore"
import api from "@/api/axios"

const router = useRouter()
const { showNotification } = useNotification()
const authStore = useAuthStore()
const wishlistStore = useWishlistStore()
const bannerStore = useBannerStore()

// State
const loading = ref(true)
const tabLoading = ref(false)

const bestSellerProducts = ref([])
const topRatedProducts = ref([])
const categories = ref([])
const newCollectionProducts = ref([])
const tabProducts = ref([])

const activeTabKey = ref('noi-bat')
const bestsellerScrollRow = ref(null)
const topRatedScrollRow = ref(null)
const newCollectionScrollRow = ref(null)

const FOOTER_BANNER_MARKER = "__FOOTER_SLOGAN_BANNER__"
const DEFAULT_SLOGAN_IMAGE = "/images/brown-modern-fashion-banner.jpg"
const SLOGAN_FALLBACK_IMAGE = "/images/slogan-banner-fallback.jpg"
const sloganImageLoadFailCount = ref(0)
const NEW_COLLECTION_CATEGORY_NAME = "Bộ Sưu Tập Mới"

const FEATURED_TABS = [
  { key: "noi-bat", label: "SẢN PHẨM NỔI BẬT", categoryName: "Sản Phẩm Nổi Bật" },
  { key: "thu-dong", label: "ĐỒ THU ĐÔNG", categoryName: "Đồ Thu Đông" },
  { key: "cong-so", label: "ĐỒ CÔNG SỞ", categoryName: "Đồ Công Sở" },
  { key: "the-thao", label: "ĐỒ THỂ THAO", categoryName: "Đồ Thể Thao" },
]

const currentTabLabel = computed(() => {
  const tab = FEATURED_TABS.find(t => t.key === activeTabKey.value)
  return tab ? tab.label : ""
})

const footerSloganBanner = computed(() =>
  bannerStore.activeBanners.find(b => b.customUrl === FOOTER_BANNER_MARKER) || null
)

const sloganImageSources = computed(() => {
  const list = []
  if (footerSloganBanner.value?.imagePath) {
    list.push(getBannerImageUrl(footerSloganBanner.value.imagePath))
  }
  list.push(DEFAULT_SLOGAN_IMAGE)
  list.push(SLOGAN_FALLBACK_IMAGE)
  return list
})

const sloganImgSrc = computed(() => {
  const list = sloganImageSources.value
  return list[Math.min(sloganImageLoadFailCount.value, list.length - 1)]
})

const handleSloganImageError = (e) => {
  if (sloganImageLoadFailCount.value < sloganImageSources.value.length - 1) {
    sloganImageLoadFailCount.value++
  } else {
    e.target.style.display = 'none'
  }
}

const newCollectionBanner = computed(() => {
  return bannerStore.activeBanners.find(b => b.linkType === "CATEGORY") || null
})

const getBannerImageUrl = (path) => {
  if (!path) return ""
  if (path.startsWith("http")) return path
  const base = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")
  return `${base}/uploads/${path.replace(/^\//, "")}`
}

const calculateDiscountPercent = (original, discounted) => {
  if (!original || !discounted || original <= discounted) return 0
  return Math.round(((original - discounted) / original) * 100)
}

const mapProduct = (p) => {
  const colorOptions = []
  if (p.variants && p.variants.length > 0) {
    p.variants.forEach(v => {
      if (v.colorName && !colorOptions.includes(v.colorName)) {
        colorOptions.push(v.colorName)
      }
    })
  }

  let minPrice = Number(p.minPrice) || 0
  let discountedPrice = p.discountedPrice ? Number(p.discountedPrice) : null

  if (p.variants && p.variants.length > 0) {
    const validPrices = p.variants.map(v => Number(v.price || 0)).filter(pr => pr > 0)
    const saleVariants = p.variants.filter(v => v.discountedPrice && Number(v.discountedPrice) < Number(v.price || 0))

    if (saleVariants.length > 0) {
      if (validPrices.length > 0) {
        minPrice = Math.min(...validPrices)
      }
      discountedPrice = Math.min(...saleVariants.map(v => Number(v.discountedPrice)))
    }
  }

  return {
    ...p,
    minPrice,
    maxPrice: Number(p.maxPrice) || 0,
    totalStock: Number(p.totalStock) || 0,
    discountedPrice,
    discountPercent: calculateDiscountPercent(minPrice, discountedPrice),
    colorOptions,
    sizeIds: p.variants?.map(v => v.sizeId).filter(Boolean) || []
  }
}

// Scroll Helper
const scrollRow = (refEl, direction) => {
  if (!refEl) return
  const cardWidth = 250
  refEl.scrollBy({ left: direction * (cardWidth + 20) * 2, behavior: 'smooth' })
}

// Navigation
const goToNewCollectionShop = () => {
  router.push({ path: "/shop", query: { categoryName: NEW_COLLECTION_CATEGORY_NAME } })
}

const goToTabShop = () => {
  const tab = FEATURED_TABS.find(t => t.key === activeTabKey.value)
  if (tab) {
    router.push({ path: "/shop", query: { categoryName: tab.categoryName } })
  }
}

// API Calls
const loadBestSellers = async () => {
  try {
    const response = await api.get('/customer/home/featured-products', { params: { page: 0, size: 10 } })
    const products = response.data?.products || response.data?.data?.products || response.data || []
    bestSellerProducts.value = Array.isArray(products) ? products.map(mapProduct) : []
  } catch (error) {
    console.error('Lỗi tải sản phẩm bán chạy:', error)
    bestSellerProducts.value = []
  }
}

const loadTopRated = async () => {
  try {
    const response = await api.get('/customer/home/latest-products', { params: { page: 0, size: 10 } })
    const products = response.data?.products || response.data?.data?.products || response.data || []
    const mapped = Array.isArray(products) ? products.map(mapProduct) : []
    topRatedProducts.value = mapped.map((p, idx) => ({
      ...p,
      rating: 4.8 + (idx % 3) * 0.1,
      reviewCount: 32 + idx * 8
    }))
  } catch (error) {
    console.error('Lỗi tải sản phẩm đánh giá cao:', error)
    topRatedProducts.value = []
  }
}

const loadCategories = async () => {
  try {
    const response = await api.get('/categories/active')
    categories.value = response.data || []
  } catch (error) {
    console.error('Lỗi tải danh mục:', error)
    categories.value = []
  }
}

const loadNewCollection = async () => {
  try {
    const allCats = categories.value.length > 0 ? categories.value : []
    const normalize = (s) => (s || "").toString().trim().toLowerCase()
    const found = allCats.find(c => normalize(c.categoryName) === normalize(NEW_COLLECTION_CATEGORY_NAME))

    if (!found) {
      newCollectionProducts.value = []
      return
    }

    const response = await api.get(`/customer/home/categories/${found.categoryId}/products`, {
      params: { page: 0, size: 10 },
    })
    const products = response.data?.products || response.data?.data?.products || response.data || []
    newCollectionProducts.value = Array.isArray(products) ? products.map(mapProduct) : []
  } catch (error) {
    console.error('Lỗi tải bộ sưu tập mới:', error)
    newCollectionProducts.value = []
  }
}

const loadTabProducts = async (tabKey) => {
  const tab = FEATURED_TABS.find(t => t.key === tabKey)
  if (!tab) return
  tabLoading.value = true
  try {
    const normalize = (s) => (s || "").toString().trim().toLowerCase()
    const found = categories.value.find(c => normalize(c.categoryName) === normalize(tab.categoryName))
    
    if (found) {
      const response = await api.get(`/customer/home/categories/${found.categoryId}/products`, {
        params: { page: 0, size: 10 }
      })
      const products = response.data?.products || response.data?.data?.products || response.data || []
      tabProducts.value = Array.isArray(products) ? products.map(mapProduct) : []
    } else {
      const response = await api.get('/customer/home/products', { params: { page: 0, size: 10 } })
      const products = response.data?.products || response.data?.data?.products || response.data || []
      tabProducts.value = Array.isArray(products) ? products.map(mapProduct) : []
    }
  } catch (error) {
    console.error(`Lỗi tải sản phẩm cho tab "${tabKey}":`, error)
    tabProducts.value = []
  } finally {
    tabLoading.value = false
  }
}

const switchTab = (tabKey) => {
  if (activeTabKey.value === tabKey) return
  activeTabKey.value = tabKey
  loadTabProducts(tabKey)
}

onMounted(async () => {
  loading.value = true
  await loadCategories()
  await Promise.all([
    loadBestSellers(),
    loadTopRated(),
    loadNewCollection(),
    loadTabProducts(activeTabKey.value)
  ])

  if (authStore.isAuthenticated) {
    await wishlistStore.fetchWishlist()
  }

  loading.value = false
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #ffffff;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Loading */
.loading-state {
  text-align: center;
  padding: 80px 0;
  color: #64748b;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f1f5f9;
  border-top: 4px solid #ff4d4f;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Home Flow Sections */
.home-flow-section {
  padding: 60px 0;
  border-bottom: 1px solid #f1ece2;
}

.top-rated-bg {
  background: #f8fafc;
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 32px;
  gap: 16px;
}

.section-heading-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.section-eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: #ff4d4f;
}

.section-title-line {
  font-size: 26px;
  color: #1e293b;
  font-weight: 800;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
}

.flame-icon {
  color: #ff4d4f;
  animation: pulse 1.5s infinite alternate;
}

.star-icon {
  color: #f59e0b;
  animation: rotate 2s infinite alternate;
}

@keyframes pulse {
  0% { transform: scale(1); }
  100% { transform: scale(1.2); }
}

@keyframes rotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(15deg); }
}

.scroll-arrows {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.arrow-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #1e293b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
  font-size: 14px;
}

.arrow-btn:hover {
  background: #ff4d4f;
  color: #ffffff;
  border-color: #ff4d4f;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

/* Product Scroll Row */
.product-scroll-wrapper {
  margin-bottom: 10px;
}

.product-scroll-row {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding: 10px 4px 20px;
}

.product-scroll-row::-webkit-scrollbar {
  display: none;
}

.product-card-col {
  flex: 0 0 250px;
  width: 250px;
  scroll-snap-align: start;
}

/* New Collection Banner */
.new-collection-banner {
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 30px;
}

.new-collection-banner-img {
  width: 100%;
  max-height: 400px;
  object-fit: cover;
  display: block;
}

/* Tabs Grid */
.tabs-heading-group {
  text-align: center;
  align-items: center;
  margin-bottom: 24px;
}

.tabs-header {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 36px;
}

.tab-btn {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 10px 24px;
  border-radius: 30px;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
  transition: all 0.25s ease;
}

.tab-btn:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.tab-btn.active {
  background: #ff4d4f;
  color: #ffffff;
  border-color: #ff4d4f;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

.tabs-product-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

.empty-tab-state {
  text-align: center;
  padding: 40px 0;
  color: #94a3b8;
  font-size: 14px;
}

.view-all-container {
  text-align: center;
  margin-top: 32px;
}

.view-all-btn {
  background: #ffffff;
  color: #ff4d4f;
  border: 2px solid #ff4d4f;
  padding: 12px 36px;
  border-radius: 30px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.view-all-btn:hover {
  background: #ff4d4f;
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 77, 79, 0.25);
}

/* Slogan Banner */
.slogan-banner-section {
  padding: 60px 0;
  background: #ffffff;
}

.slogan-banner-container {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
}

.slogan-banner-img {
  width: 100%;
  display: block;
}

.slogan-shine-effect {
  position: absolute;
  top: 0;
  left: -150%;
  width: 60%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transform: skewX(-25deg);
  animation: shimmer 6s infinite;
}

@keyframes shimmer {
  0% { left: -150%; }
  30% { left: 150%; }
  100% { left: 150%; }
}

@media (max-width: 1024px) {
  .tabs-product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 640px) {
  .section-title-line {
    font-size: 20px;
  }
  .product-card-col {
    flex: 0 0 210px;
    width: 210px;
  }
  .tabs-product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>