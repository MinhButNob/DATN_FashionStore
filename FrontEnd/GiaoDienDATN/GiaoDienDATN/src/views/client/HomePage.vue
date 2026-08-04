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

      <!-- Sản phẩm khuyến mãi -->
      <section v-else-if="saleProducts.length > 0" class="product-section home-flow-section">
        <div class="container">
          <div class="section-header">
            <div class="section-heading-group">
              <span class="section-eyebrow">Ưu đãi hôm nay</span>
              <h2 class="section-title-line no-click">SẢN PHẨM SALE</h2>
            </div>
            <div class="scroll-arrows">
              <button
                class="arrow-btn"
                @click="scrollProducts(-1)"
                aria-label="Trước"
              >
                <i class="fas fa-chevron-left"></i>
              </button>
              <button
                class="arrow-btn"
                @click="scrollProducts(1)"
                aria-label="Sau"
              >
                <i class="fas fa-chevron-right"></i>
              </button>
            </div>
          </div>

          <div class="product-scroll-wrapper">
            <div class="product-scroll-row" ref="scrollRow" @scroll="updateScrollState">
              <div
                v-for="product in saleProducts"
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
                  <div v-if="product.discountPercent > 0" class="discount-flag">
                    -{{ product.discountPercent }}%
                  </div>
                </div>
                <div class="product-info-direct">
                  <div class="product-options">
                    <span v-if="product.colorOptions && product.colorOptions.length > 0" class="option-tag">
                      +{{ product.colorOptions.length }} Màu sắc
                    </span>
                    <span class="option-tag">+{{ product.sizeIds?.length || 0 }} Kích thước</span>
                  </div>
                  <h3 class="product-name-direct">{{ product.productName }}</h3>

                  <div class="product-price-direct">
                    <template v-if="product.discountedPrice && product.discountedPrice < product.minPrice">
                      <span class="current-price-direct">{{ formatPrice(product.discountedPrice) }}</span>
                      <span class="original-price-direct">{{ formatPrice(product.minPrice) }}</span>
                    </template>
                    <template v-else>
                      <span class="current-price-direct">{{ formatPrice(product.minPrice) }}</span>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="view-all-container">
            <button class="view-all-btn" @click="goToShopWithPromotion">
              XEM TẤT CẢ SẢN PHẨM KHUYẾN MÃI
            </button>
          </div>
        </div>
      </section>

      <!-- Empty State -->
      <div v-else-if="!loading && saleProducts.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <h3>Hiện chưa có sản phẩm nào đang khuyến mãi</h3>
        <p>Các sản phẩm được áp dụng giảm giá sẽ xuất hiện tại đây.</p>
        <button class="btn-reload" @click="reloadData">
          <i class="fas fa-sync-alt"></i> Tải lại trang
        </button>
      </div>

      <!-- Dòng hàng nổi bật -->
      <section v-if="featuredCategories.length > 0" class="featured-section home-flow-section">
        <div class="container">
          <div class="section-header">
            <div class="section-heading-group">
              <span class="section-eyebrow">Danh mục</span>
              <h2 class="section-title-line no-click">DÒNG HÀNG NỔI BẬT</h2>
            </div>
            <div class="scroll-arrows">
              <button class="arrow-btn" @click="scrollFeatured(-1)" aria-label="Trước">
                <i class="fas fa-chevron-left"></i>
              </button>
              <button class="arrow-btn" @click="scrollFeatured(1)" aria-label="Sau">
                <i class="fas fa-chevron-right"></i>
              </button>
            </div>
          </div>

          <div class="featured-scroll-wrapper">
            <div
              class="featured-scroll-row"
              ref="featuredScrollRow"
              @scroll="updateFeaturedProgress"
            >
              <div
                v-for="cat in featuredCategories"
                :key="cat.id"
                class="featured-card"
                @click="goToFeaturedCategory(cat)"
              >
                <img
                  :src="cat.imagePath"
                  :alt="cat.title"
                  class="featured-img"
                  loading="lazy"
                />
                <div class="featured-overlay">
                  <span class="featured-title">{{ cat.title }}</span>
                </div>
              </div>
            </div>
          </div>

          <div
            class="featured-progress-track"
            ref="featuredProgressTrack"
            @pointerdown="onTrackClick"
          >
            <div
              class="featured-progress-thumb"
              :class="{ dragging: isDraggingThumb }"
              :style="{ width: featuredThumbWidth + '%', left: featuredThumbLeft + '%' }"
              @pointerdown.stop="startDragThumb"
            ></div>
          </div>
        </div>
      </section>

      <!-- Bộ Sưu Tập Mới -->
      <section class="new-collection-section home-flow-section" v-if="newCollectionBanner || newCollectionProducts.length > 0 || newCollectionLoading">
        <div class="container">
          <div class="section-header">
            <div class="section-heading-group">
              <span class="section-eyebrow">Mới về</span>
              <h2 class="section-title-line no-click">BỘ SƯU TẬP MỚI</h2>
            </div>
          </div>
        </div>

        <div class="container">
          <div
            v-if="newCollectionBanner"
            class="new-collection-banner"
            @click="goToNewCollectionShop"
            role="button"
            tabindex="0"
          >
            <img
              :src="getBannerImageUrl(newCollectionBanner.imagePath)"
              :alt="newCollectionBanner.title || 'Bộ Sưu Tập Mới'"
              class="new-collection-banner-img"
              @error="handleImageError"
            />
          </div>
        </div>

        <div class="container">
          <div v-if="newCollectionLoading" class="loading-state">
            <div class="spinner"></div>
          </div>

          <div v-else-if="newCollectionProducts.length > 0" class="product-scroll-wrapper new-collection-scroll-wrapper">
            <button class="row-arrow row-arrow-left" @click="scrollNewCollection(-1)" aria-label="Trước">
              <i class="fas fa-chevron-left"></i>
            </button>
            <div
              class="product-scroll-row"
              ref="newCollectionScrollRow"
              @scroll="updateNewCollectionScrollState"
            >
              <div
                v-for="product in newCollectionProducts"
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
                  <div v-if="product.discountPercent > 0" class="discount-flag">
                    -{{ product.discountPercent }}%
                  </div>
                </div>
                <div class="product-info-direct">
                  <div class="product-options">
                    <span v-if="product.colorOptions && product.colorOptions.length > 0" class="option-tag">
                      +{{ product.colorOptions.length }} Màu sắc
                    </span>
                    <span class="option-tag">+{{ product.sizeIds?.length || 0 }} Kích thước</span>
                  </div>
                  <h3 class="product-name-direct">{{ product.productName }}</h3>

                  <div class="product-price-direct">
                    <template v-if="product.discountedPrice && product.discountedPrice < product.minPrice">
                      <span class="current-price-direct">{{ formatPrice(product.discountedPrice) }}</span>
                      <span class="original-price-direct">{{ formatPrice(product.minPrice) }}</span>
                    </template>
                    <template v-else>
                      <span class="current-price-direct">{{ formatPrice(product.minPrice) }}</span>
                    </template>
                  </div>
                </div>
              </div>
            </div>
            <button class="row-arrow row-arrow-right" @click="scrollNewCollection(1)" aria-label="Sau">
              <i class="fas fa-chevron-right"></i>
            </button>
          </div>

          <div v-else class="empty-tab-state">
            <p>Chưa có sản phẩm nào trong danh mục "Bộ Sưu Tập Mới". Hãy vào trang quản lý sản phẩm và gán sản phẩm vào danh mục này nhé!</p>
          </div>

          <div class="view-all-container">
            <button class="view-all-btn" @click="goToNewCollectionShop">
              XEM TẤT CẢ BỘ SƯU TẬP
            </button>
          </div>
        </div>
      </section>

      <!-- Sản phẩm nổi bật theo tab -->
      <section class="tabs-featured-section home-flow-section">
        <div class="container">
          <div class="section-heading-group tabs-heading-group">
            <span class="section-eyebrow">Gợi ý cho bạn</span>
            <h2 class="section-title-line no-click">CHỌN THEO PHONG CÁCH</h2>
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
            <div
              v-for="product in tabProducts"
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
                <div v-if="product.discountPercent > 0" class="discount-flag">
                  -{{ product.discountPercent }}%
                </div>
              </div>
              <div class="product-info-direct">
                <div class="product-options">
                  <span v-if="product.colorOptions && product.colorOptions.length > 0" class="option-tag">
                    +{{ product.colorOptions.length }} Màu sắc
                  </span>
                  <span class="option-tag">+{{ product.sizeIds?.length || 0 }} Kích thước</span>
                </div>
                <h3 class="product-name-direct">{{ product.productName }}</h3>

                <div class="product-price-direct">
                  <template v-if="product.discountedPrice && product.discountedPrice < product.minPrice">
                    <span class="current-price-direct">{{ formatPrice(product.discountedPrice) }}</span>
                    <span class="original-price-direct">{{ formatPrice(product.minPrice) }}</span>
                  </template>
                  <template v-else>
                    <span class="current-price-direct">{{ formatPrice(product.minPrice) }}</span>
                  </template>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-tab-state">
            <p>Chưa có sản phẩm nào trong danh mục "{{ currentTabLabel }}". Hãy vào trang quản lý sản phẩm và gán sản phẩm vào danh mục này nhé!</p>
          </div>

          <div class="view-all-container">
            <button class="view-all-btn" @click="goToTabShop">
              XEM TẤT CẢ {{ currentTabLabel }}
            </button>
          </div>
        </div>
      </section>

      <!-- Slogan Banner - Banner quảng cáo thương hiệu -->
      <section class="slogan-banner-section">
        <div class="container">
          <div class="slogan-banner-container">
            <!-- ✅ Ảnh lấy từ banner quản lý trong Admin > Quản lý Banner (đánh dấu "Banner Slogan cuối trang").
                 Nếu admin chưa upload banner nào cho khu vực này, dùng ảnh mặc định trong /public/images làm fallback. -->
            <img
              :src="sloganImgSrc"
              :alt="footerSloganBanner?.title || 'Fashion Stores - Thời trang cho mọi phong cách'"
              class="slogan-banner-img"
              @error="handleSloganImageError"
            />
            <div class="slogan-shine-effect"></div>
          </div>
        </div>
      </section>
    </main>

    <ClientFooter />
    <CartModal />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, watch } from "vue"
import { useRouter } from "vue-router"
import { useNotification } from "../../composables/useNotification"
import ClientHeader from "../../components/client/ClientHeader.vue"
import ClientFooter from "../../components/client/ClientFooter.vue"
import BannerSlider from "../../components/client/BannerSlider.vue"
import CartModal from "../../components/client/CartModal.vue"
import { useCartStore } from "../../stores/cartStore"
import { useAuthStore } from "../../stores/authStore"
import { useWishlistStore } from "../../stores/wishlistStore"
import { useBannerStore } from "../../stores/bannerStore"
import api from "@/api/axios"

// ==================== Composables ====================
const router = useRouter()
const { showNotification } = useNotification()
const cartStore = useCartStore()
const authStore = useAuthStore()
const wishlistStore = useWishlistStore()
const bannerStore = useBannerStore()

// ==================== State ====================
const loading = ref(false)
const saleProducts = ref([])
const categories = ref([])
// ✅ Marker nhận diện banner "Slogan cuối trang" được tạo/sửa trong Admin > Quản lý Banner
// (xem AdminBanners.vue / BannerSlider.vue - cùng dùng chung 1 marker này).
const FOOTER_BANNER_MARKER = "__FOOTER_SLOGAN_BANNER__"
const DEFAULT_SLOGAN_IMAGE = "/images/brown-modern-fashion-banner.jpg"
const SLOGAN_FALLBACK_IMAGE = "/images/slogan-banner-fallback.jpg"
const sloganImageLoadFailCount = ref(0)

// Banner khu vực slogan cuối trang: lấy từ banner đang ACTIVE có customUrl = FOOTER_BANNER_MARKER
// (được tạo/sửa trong Admin > Quản lý Banner, tick "Đây là banner Slogan cuối trang").
const footerSloganBanner = computed(() =>
  bannerStore.activeBanners.find(b => b.customUrl === FOOTER_BANNER_MARKER) || null
)

// Danh sách nguồn ảnh theo thứ tự ưu tiên: banner admin upload -> ảnh mặc định -> ảnh dự phòng cuối cùng
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
    // Đã thử hết mọi nguồn ảnh (banner admin + 2 ảnh fallback) đều lỗi -> ẩn hẳn để tránh vòng lặp lỗi liên tục
    e.target.style.display = 'none'
  }
}

// Reset trạng thái lỗi mỗi khi banner cuối trang từ store thay đổi (vd: admin vừa cập nhật ảnh mới)
watch(() => footerSloganBanner.value?.imagePath, () => {
  sloganImageLoadFailCount.value = 0
})
const scrollRow = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(true)

// Featured categories
const featuredCategories = ref([])
const featuredScrollRow = ref(null)
const featuredThumbWidth = ref(25)
const featuredThumbLeft = ref(0)
const featuredProgressTrack = ref(null)
const isDraggingThumb = ref(false)

// New Collection
const newCollectionCategoryId = ref(null)
const newCollectionProducts = ref([])
const newCollectionLoading = ref(false)
const newCollectionScrollRow = ref(null)
const canScrollLeftNewCollection = ref(false)
const canScrollRightNewCollection = ref(true)

// Tab Products
const activeTabKey = ref('noi-bat')
const tabProducts = ref([])
const tabLoading = ref(false)
const tabCategoryMap = ref({})

// ==================== Constants ====================
const NEW_COLLECTION_CATEGORY_NAME = "Bộ Sưu Tập Mới"
const SALE_CATEGORY_NAME = "Khuyến mãi"

const FEATURED_TABS = [
  { key: "noi-bat", label: "SẢN PHẨM NỔI BẬT", categoryName: "Sản Phẩm Nổi Bật" },
  { key: "thu-dong", label: "ĐỒ THU ĐÔNG", categoryName: "Đồ Thu Đông" },
  { key: "cong-so", label: "ĐỒ CÔNG SỞ", categoryName: "Đồ Công Sở" },
  { key: "the-thao", label: "ĐỒ THỂ THAO", categoryName: "Đồ Thể Thao" },
]

const FEATURED_ORDER = ['Áo Phông', 'Quần', 'Áo Polo', 'Áo Sơ Mi', 'Phụ kiện', 'Homewear', 'Active']
const FEATURED_IMAGE_MAP = {
  'Áo Phông': 'https://picsum.photos/seed/feat-aophong/500/650',
  'Quần': 'https://picsum.photos/seed/feat-quan/500/650',
  'Áo Polo': 'https://picsum.photos/seed/feat-aopolo/500/650',
  'Áo Sơ Mi': 'https://picsum.photos/seed/feat-aosomi/500/650',
  'Phụ kiện': 'https://picsum.photos/seed/feat-phukien/500/650',
  'Homewear': 'https://picsum.photos/seed/feat-homewear/500/650',
  'Active': 'https://picsum.photos/seed/feat-active/500/650',
}

// ==================== Computed ====================
const currentTabLabel = computed(() => {
  const tab = FEATURED_TABS.find(t => t.key === activeTabKey.value)
  return tab ? tab.label : ""
})

const newCollectionBanner = computed(() => {
  const normalize = (s) => (s || "").toString().trim().toLowerCase()
  return bannerStore.activeBanners.find(
    b => b.linkType === "CATEGORY" &&
    (
      (b.categoryId && newCollectionCategoryId.value && b.categoryId === newCollectionCategoryId.value) ||
      normalize(b.categoryName) === normalize(NEW_COLLECTION_CATEGORY_NAME)
    )
  ) || null
})

// ==================== Helper Functions ====================
const normalizeName = (name) => (name || '').toString().trim().toLowerCase()

const formatPrice = (price) => {
  if (!price && price !== 0) return '0₫'
  return Number(price).toLocaleString('vi-VN') + '₫'
}

const calculateDiscountPercent = (original, discounted) => {
  if (!original || !discounted || original <= discounted) return 0
  return Math.round(((original - discounted) / original) * 100)
}

const handleImageError = (event) => {
  event.target.style.display = 'none'
  const placeholder = event.target.parentElement?.querySelector('.no-image-placeholder')
  if (placeholder) placeholder.style.display = 'flex'
}

const getBannerImageUrl = (path) => {
  if (!path) return ""
  if (path.startsWith("http")) return path
  const base = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")
  if (path.startsWith("/uploads/") || path.startsWith("uploads/")) {
    return `${base}/${path.replace(/^\//, "")}`
  }
  return `${base}/uploads/${path}`
}

const shuffleArray = (arr) => {
  const shuffled = [...arr]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
  }
  return shuffled
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

// ==================== Scroll Helpers ====================
const updateScrollState = () => {
  const el = scrollRow.value
  if (!el) return
  canScrollLeft.value = el.scrollLeft > 5
  canScrollRight.value = el.scrollLeft < el.scrollWidth - el.clientWidth - 5
}

const scrollProducts = (direction) => {
  const el = scrollRow.value
  if (!el) return
  const card = el.querySelector('.product-card-direct')
  const cardWidth = card ? card.offsetWidth : 220
  const maxScroll = el.scrollWidth - el.clientWidth

  if (direction > 0 && el.scrollLeft >= maxScroll - 5) {
    el.scrollTo({ left: 0, behavior: 'smooth' })
    return
  }
  if (direction < 0 && el.scrollLeft <= 5) {
    el.scrollTo({ left: maxScroll, behavior: 'smooth' })
    return
  }
  el.scrollBy({ left: direction * (cardWidth + 16) * 2, behavior: 'smooth' })
}

// ==================== Navigation ====================
const goToProductDetail = (id) => {
  if (id) router.push(`/product/${id}`)
}

const goToShopWithPromotion = () => {
  router.push({ path: '/shop', query: { categoryId: 'sale' } })
}

const goToFeaturedCategory = (cat) => {
  if (cat.categoryName) {
    router.push({ path: '/shop', query: { categoryName: cat.categoryName } })
  }
}

const goToNewCollectionShop = () => {
  const resolvedUrl = newCollectionBanner.value?.resolvedUrl
  if (resolvedUrl && resolvedUrl !== "#") {
    if (resolvedUrl.startsWith("http")) {
      window.open(resolvedUrl, "_blank")
    } else {
      router.push(resolvedUrl)
    }
    return
  }
  router.push({ path: "/shop", query: { categoryName: NEW_COLLECTION_CATEGORY_NAME } })
}

const goToTabShop = () => {
  const tab = FEATURED_TABS.find(t => t.key === activeTabKey.value)
  if (tab) {
    router.push({ path: "/shop", query: { categoryName: tab.categoryName } })
  }
}

// ==================== Featured Categories ====================
const updateFeaturedProgress = () => {
  const el = featuredScrollRow.value
  if (!el || el.scrollWidth <= el.clientWidth) {
    featuredThumbWidth.value = 100
    featuredThumbLeft.value = 0
    return
  }
  const maxScroll = el.scrollWidth - el.clientWidth
  const ratio = el.scrollLeft / maxScroll
  featuredThumbWidth.value = Math.max(15, (el.clientWidth / el.scrollWidth) * 100)
  featuredThumbLeft.value = ratio * (100 - featuredThumbWidth.value)
}

const scrollFeatured = (direction) => {
  const el = featuredScrollRow.value
  if (!el) return
  const card = el.querySelector('.featured-card')
  const cardWidth = card ? card.offsetWidth : 240
  const maxScroll = el.scrollWidth - el.clientWidth

  if (direction > 0 && el.scrollLeft >= maxScroll - 5) {
    el.scrollTo({ left: 0, behavior: 'smooth' })
    return
  }
  if (direction < 0 && el.scrollLeft <= 5) {
    el.scrollTo({ left: maxScroll, behavior: 'smooth' })
    return
  }
  el.scrollBy({ left: direction * (cardWidth + 16) * 2, behavior: 'smooth' })
}

const setScrollFromRatio = (ratio) => {
  const el = featuredScrollRow.value
  if (!el) return
  const maxScroll = el.scrollWidth - el.clientWidth
  const clamped = Math.min(1, Math.max(0, ratio))
  el.scrollLeft = clamped * maxScroll
  updateFeaturedProgress()
}

const startDragThumb = (event) => {
  const track = featuredProgressTrack.value
  const el = featuredScrollRow.value
  if (!track || !el) return
  isDraggingThumb.value = true

  const trackRect = track.getBoundingClientRect()
  const thumbWidthPx = (featuredThumbWidth.value / 100) * trackRect.width
  const pointerOffsetInThumb = event.clientX - (trackRect.left + (featuredThumbLeft.value / 100) * trackRect.width)

  const onPointerMove = (moveEvent) => {
    const x = moveEvent.clientX - trackRect.left - pointerOffsetInThumb
    const usableWidth = trackRect.width - thumbWidthPx
    const ratio = usableWidth > 0 ? x / usableWidth : 0
    setScrollFromRatio(ratio)
  }

  const onPointerUp = () => {
    isDraggingThumb.value = false
    window.removeEventListener('pointermove', onPointerMove)
    window.removeEventListener('pointerup', onPointerUp)
  }

  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

const onTrackClick = (event) => {
  if (event.target !== featuredProgressTrack.value) return
  const track = featuredProgressTrack.value
  if (!track) return
  const trackRect = track.getBoundingClientRect()
  const thumbWidthPx = (featuredThumbWidth.value / 100) * trackRect.width
  const x = event.clientX - trackRect.left - thumbWidthPx / 2
  const usableWidth = trackRect.width - thumbWidthPx
  const ratio = usableWidth > 0 ? x / usableWidth : 0
  setScrollFromRatio(ratio)
}

// ==================== New Collection ====================
const updateNewCollectionScrollState = () => {
  const el = newCollectionScrollRow.value
  if (!el) return
  canScrollLeftNewCollection.value = el.scrollLeft > 5
  canScrollRightNewCollection.value = el.scrollLeft < el.scrollWidth - el.clientWidth - 5
}

const scrollNewCollection = (direction) => {
  const el = newCollectionScrollRow.value
  if (!el) return
  const card = el.querySelector('.product-card-direct')
  const cardWidth = card ? card.offsetWidth : 220
  const maxScroll = el.scrollWidth - el.clientWidth

  if (direction > 0 && el.scrollLeft >= maxScroll - 5) {
    el.scrollTo({ left: 0, behavior: 'smooth' })
    return
  }
  if (direction < 0 && el.scrollLeft <= 5) {
    el.scrollTo({ left: maxScroll, behavior: 'smooth' })
    return
  }
  el.scrollBy({ left: direction * (cardWidth + 16) * 2, behavior: 'smooth' })
}

// ==================== Tab Products ====================
const switchTab = (tabKey) => {
  if (activeTabKey.value === tabKey) return
  activeTabKey.value = tabKey
  loadTabProducts(tabKey)
}

// ==================== API Calls ====================
const loadSaleProducts = async () => {
  try {
    const response = await api.get('/customer/home/latest-products', { params: { page: 0, size: 1000 } })
    const products = response.data?.products || response.data?.data?.products || response.data || []
    
    const mapped = Array.isArray(products) ? products.map(mapProduct) : []
    const onSale = mapped.filter(p => p.discountPercent > 0 && p.discountedPrice && p.discountedPrice < p.minPrice)
    
    saleProducts.value = shuffleArray(onSale).slice(0, 10)
    
    console.log(`✅ Đã tìm thấy ${onSale.length} sản phẩm khuyến mãi, hiển thị ${saleProducts.value.length}`)
  } catch (error) {
    console.error('❌ Lỗi tải sản phẩm khuyến mãi:', error)
    saleProducts.value = []
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

const loadFeaturedCategories = async () => {
  try {
    const allCats = categories.value.length > 0 ? categories.value : []
    const normalize = (s) => (s || '').toString().trim().toLowerCase()

    featuredCategories.value = FEATURED_ORDER
      .map(name => {
        const found = allCats.find(c => normalize(c.categoryName) === normalize(name))
        if (!found) return null
        return {
          id: found.categoryId,
          title: name.toUpperCase(),
          categoryName: found.categoryName,
          imagePath: FEATURED_IMAGE_MAP[name] || 'https://picsum.photos/seed/feat-default/500/650',
        }
      })
      .filter(Boolean)
  } catch (error) {
    console.error('❌ Lỗi tải danh mục nổi bật:', error)
    featuredCategories.value = []
  }
}

const loadNewCollection = async () => {
  newCollectionLoading.value = true
  try {
    if (!bannerStore.activeBanners || bannerStore.activeBanners.length === 0) {
      await bannerStore.fetchActiveBanners()
    }

    const allCats = categories.value.length > 0 ? categories.value : []
    const normalize = (s) => (s || "").toString().trim().toLowerCase()
    const found = allCats.find(c => normalize(c.categoryName) === normalize(NEW_COLLECTION_CATEGORY_NAME))

    if (!found) {
      console.warn('⚠️ Chưa tìm thấy danh mục "Bộ Sưu Tập Mới"')
      newCollectionProducts.value = []
      return
    }
    newCollectionCategoryId.value = found.categoryId

    const response = await api.get(`/customer/home/categories/${found.categoryId}/products`, {
      params: { page: 0, size: 10 },
    })
    const products = response.data?.products || response.data?.data?.products || response.data || []
    newCollectionProducts.value = Array.isArray(products) ? products.map(mapProduct) : []
    nextTick(() => updateNewCollectionScrollState())
  } catch (error) {
    console.error('❌ Lỗi tải bộ sưu tập mới:', error)
    newCollectionProducts.value = []
  } finally {
    newCollectionLoading.value = false
  }
}

const loadTabCategoryIds = async () => {
  try {
    const allCats = categories.value.length > 0 ? categories.value : []
    const normalize = (s) => (s || "").toString().trim().toLowerCase()
    const map = {}
    FEATURED_TABS.forEach(tab => {
      const found = allCats.find(c => normalize(c.categoryName) === normalize(tab.categoryName))
      if (found) map[tab.key] = found.categoryId
    })
    tabCategoryMap.value = map
  } catch (error) {
    console.error('❌ Lỗi tải danh mục cho tabs:', error)
    tabCategoryMap.value = {}
  }
}

const loadTabProducts = async (tabKey) => {
  const categoryId = tabCategoryMap.value[tabKey]
  if (!categoryId) {
    console.warn(`⚠️ Chưa tìm thấy danh mục cho tab "${tabKey}"`)
    tabProducts.value = []
    return
  }
  tabLoading.value = true
  try {
    const response = await api.get(`/customer/home/categories/${categoryId}/products`, {
      params: { page: 0, size: 10 },
    })
    const products = response.data?.products || response.data?.data?.products || response.data || []
    tabProducts.value = Array.isArray(products) ? products.map(mapProduct) : []
  } catch (error) {
    console.error(`❌ Lỗi tải sản phẩm cho tab "${tabKey}":`, error)
    tabProducts.value = []
  } finally {
    tabLoading.value = false
  }
}

// ==================== Reload ====================
const reloadData = async () => {
  showNotification('Đang tải lại dữ liệu...', 'info')
  loading.value = true
  await loadSaleProducts()
  await loadFeaturedCategories()
  await loadNewCollection()
  await loadTabProducts(activeTabKey.value)
  loading.value = false
}

// ==================== Lifecycle ====================
onMounted(async () => {
  loading.value = true
  
  await loadCategories()
  await loadSaleProducts()
  await loadFeaturedCategories()
  await loadNewCollection()
  await loadTabCategoryIds()
  await loadTabProducts(activeTabKey.value)
  
  if (authStore.isAuthenticated) {
    await wishlistStore.fetchWishlist()
  }
  
  loading.value = false
  
  nextTick(() => {
    updateScrollState()
    updateFeaturedProgress()
    updateNewCollectionScrollState()
  })
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,500;0,600;0,700;1,500&display=swap');

.home-page {
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* ===== Loading ===== */
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

/* ===== Section Header ===== */
.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 36px;
  gap: 16px;
}

.section-heading-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.section-eyebrow {
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 3px;
  text-transform: uppercase;
  color: #b8905a;
}

.section-title-line {
  font-family: 'Playfair Display', 'Segoe UI', serif;
  font-size: 30px;
  color: #1a1a1a;
  font-weight: 600;
  letter-spacing: 0.3px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
}

.section-title-line.no-click {
  cursor: default;
}

.section-title-line.no-click:hover {
  color: #1a1a1a;
}

.scroll-arrows {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.arrow-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid #e2ddd4;
  background: transparent;
  color: #2c3e50;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
  font-size: 13px;
}

.arrow-btn:hover:not(:disabled) {
  background: #1a1a1a;
  color: white;
  border-color: #1a1a1a;
}

.arrow-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ===== Product Section ===== */
.home-flow-section {
  background: #fdfcfa;
  position: relative;
}

.home-flow-section + .home-flow-section::before {
  content: "";
  position: absolute;
  top: 0;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 64px;
  height: 1px;
  background: #ddd0bb;
}

.home-flow-section + .home-flow-section::after {
  content: "";
  position: absolute;
  top: 0;
  left: 50%;
  transform: translate(-50%, -50%) rotate(45deg);
  width: 7px;
  height: 7px;
  background: #fdfcfa;
  border: 1px solid #ddd0bb;
}

.product-section {
  padding: 64px 0 56px;
}

.product-scroll-wrapper {
  margin-bottom: 36px;
}

.product-scroll-row {
  display: flex;
  gap: 22px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding: 4px 4px 10px;
}

.product-scroll-row::-webkit-scrollbar {
  display: none;
}

/* ===== Product Card ===== */
.product-card-direct {
  background: white;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(20, 15, 5, 0.06);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid #f1ece2;
  flex: 0 0 auto;
  width: 220px;
  scroll-snap-align: start;
}

.product-card-direct:hover {
  transform: translateY(-6px);
  box-shadow: 0 18px 30px rgba(20, 15, 5, 0.1);
  border-color: #e7dfd0;
}

.product-image-direct {
  height: 220px;
  background: #f6f4f0;
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
  transition: transform 0.5s ease;
}

.product-card-direct:hover .product-img-direct {
  transform: scale(1.05);
}

.no-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.no-image-placeholder i {
  font-size: 56px;
  color: #ccc;
}

.discount-flag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #1a1a1a;
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.3px;
  z-index: 2;
}

.product-info-direct {
  padding: 14px 14px 18px;
}

.product-options {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.option-tag {
  font-size: 11px;
  color: #8a8a8a;
  background: #f6f4f0;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.product-name-direct {
  font-size: 13.5px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
  line-height: 1.4;
  height: 38px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price-direct {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.current-price-direct {
  font-size: 15px;
  font-weight: 700;
  color: #e63946;
}

.original-price-direct {
  font-size: 12.5px;
  color: #999;
  text-decoration: line-through;
}

/* ===== View All ===== */
.view-all-container {
  text-align: center;
  margin-top: 40px;
}

.view-all-btn {
  background: transparent;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  padding: 13px 38px;
  border-radius: 30px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}

.view-all-btn:hover {
  background: #1a1a1a;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(20, 15, 5, 0.18);
}

/* ===== Empty State ===== */
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

/* ===== Featured Categories ===== */
.featured-section {
  padding: 60px 0 64px;
}

.featured-scroll-wrapper {
  margin-bottom: 24px;
}

.featured-scroll-row {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding: 4px 4px 10px;
}

.featured-scroll-row::-webkit-scrollbar {
  display: none;
}

.featured-card {
  position: relative;
  flex: 0 0 auto;
  width: 240px;
  height: 320px;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  scroll-snap-align: start;
  background: #f0f0f0;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.featured-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 18px 30px rgba(20, 15, 5, 0.14);
}

.featured-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.featured-card:hover .featured-img {
  transform: scale(1.06);
}

.featured-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18px 18px;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.6) 90%);
}

.featured-title {
  color: white;
  font-family: 'Playfair Display', serif;
  font-weight: 600;
  font-size: 18px;
  letter-spacing: 0.3px;
}

.featured-progress-track {
  height: 3px;
  background: #ece6d9;
  border-radius: 4px;
  position: relative;
  cursor: pointer;
  touch-action: none;
}

.featured-progress-thumb {
  position: absolute;
  top: 0;
  height: 100%;
  background: #1a1a1a;
  border-radius: 4px;
  cursor: grab;
  transition: left 0.15s ease, width 0.15s ease, background 0.15s ease;
  touch-action: none;
}

.featured-progress-thumb:hover {
  background: #b8905a;
}

.featured-progress-thumb.dragging {
  cursor: grabbing;
  background: #b8905a;
  transition: none;
}

/* ===== New Collection ===== */
.new-collection-section {
  padding: 60px 0 56px;
}

.new-collection-banner {
  width: 100%;
  overflow: hidden;
  cursor: pointer;
  margin: 0 0 40px;
  border-radius: 12px;
  aspect-ratio: 2 / 1;
}

.new-collection-banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
  transition: transform 0.4s ease;
}

.new-collection-banner:hover .new-collection-banner-img {
  transform: scale(1.02);
}

.new-collection-scroll-wrapper {
  position: relative;
}

.row-arrow {
  position: absolute;
  top: 40%;
  transform: translateY(-50%);
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(232, 232, 232, 0.7);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 5;
  color: #1a1a1a;
  font-size: 15px;
  opacity: 0.55;
  backdrop-filter: blur(2px);
  transition: all 0.25s ease;
}

.row-arrow:hover {
  background: #1a1a2e;
  color: #fff;
  opacity: 1;
  transform: translateY(-50%) scale(1.08);
}

.row-arrow-left {
  left: -6px;
}

.row-arrow-right {
  right: -6px;
}

/* ===== Tabs Featured ===== */
.tabs-featured-section {
  padding: 60px 0 68px;
}

.tabs-heading-group {
  text-align: center;
  align-items: center;
  margin-bottom: 32px;
}

.tabs-heading-group .section-title-line {
  justify-content: center;
}

.tabs-header {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 40px;
  padding-bottom: 0;
}

.tab-btn {
  background: transparent;
  border: 1px solid #e2ddd4;
  cursor: pointer;
  padding: 10px 24px;
  border-radius: 30px;
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #6b6b6b;
  text-transform: uppercase;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.tab-btn:hover {
  border-color: #1a1a1a;
  color: #1a1a1a;
}

.tab-btn.active {
  color: #fff;
  font-weight: 600;
  background: #1a1a1a;
  border-color: #1a1a1a;
}

.tabs-product-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 28px 24px;
  margin-bottom: 8px;
}

.tabs-product-grid .product-card-direct {
  width: auto;
}

.empty-tab-state {
  text-align: center;
  padding: 60px 20px;
  background: #fafafa;
  border-radius: 12px;
  color: #666;
  font-size: 15px;
}

/* ===== Slogan Banner Section ===== */
.slogan-banner-section {
  margin: 0;
  padding: 64px 0;
  width: 100%;
  background: #ffffff;
}

.slogan-banner-container {
  position: relative;
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  background: #f8f9fa;
  display: flex;
  justify-content: center;
  align-items: center;
}

.slogan-banner-img {
  width: 100%;
  height: auto;
  display: block;
  pointer-events: none;
  user-select: none;
  animation: sloganBreathing 7s ease-in-out infinite alternate;
}

/* Dynamic Breathing Zoom Animation */
@keyframes sloganBreathing {
  0% {
    transform: scale(1);
  }
  100% {
    transform: scale(1.03);
  }
}

/* Shimmer Light Sweep Effect */
.slogan-shine-effect {
  position: absolute;
  top: 0;
  left: -150%;
  width: 60%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.15) 50%,
    transparent 100%
  );
  transform: skewX(-25deg);
  pointer-events: none;
  animation: sloganShimmerSweep 6s infinite;
}

@keyframes sloganShimmerSweep {
  0% {
    left: -150%;
  }
  30% {
    left: 150%;
  }
  100% {
    left: 150%;
  }
}

/* ===== Responsive ===== */
@media (max-width: 992px) {
  .product-card-direct {
    width: 200px;
  }
  .tabs-product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .tabs-header {
    gap: 10px;
  }
  .tab-btn {
    font-size: 12.5px;
    padding: 8px 18px;
  }
  .product-section,
  .featured-section,
  .new-collection-section,
  .tabs-featured-section {
    padding-top: 48px;
    padding-bottom: 44px;
  }
  .slogan-banner-section {
    padding: 48px 0;
  }
  .section-header {
    margin-bottom: 28px;
  }
  .view-all-container {
    margin-top: 28px;
  }
}

@media (max-width: 768px) {
  .product-card-direct {
    width: 160px;
  }
  .section-title-line {
    font-size: 20px;
  }
  .section-eyebrow {
    font-size: 11px;
    letter-spacing: 2px;
  }
  .arrow-btn {
    width: 32px;
    height: 32px;
    font-size: 12px;
  }
  .product-image-direct {
    height: 160px;
  }
  .product-section,
  .featured-section,
  .new-collection-section,
  .tabs-featured-section {
    padding-top: 36px;
    padding-bottom: 32px;
  }
  .slogan-banner-section {
    padding: 36px 0;
  }
  .section-header {
    margin-bottom: 20px;
  }
  .view-all-container {
    margin-top: 20px;
  }
  .new-collection-banner {
    margin-bottom: 24px;
  }
  .featured-card {
    width: 170px;
    height: 230px;
  }
  .featured-title {
    font-size: 13px;
  }
  .product-name-direct {
    font-size: 14px;
    height: 38px;
  }
  .current-price-direct {
    font-size: 16px;
  }
  .original-price-direct {
    font-size: 12px;
  }
  .discount-flag {
    font-size: 12px;
    padding: 4px 10px;
  }
  .view-all-btn {
    padding: 12px 24px;
    font-size: 13px;
    width: 100%;
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
  .newsletter-form {
    flex-direction: column;
    padding: 0 20px;
  }
  .row-arrow {
    width: 34px;
    height: 34px;
    font-size: 13px;
  }
  .row-arrow-left { left: -2px; }
  .row-arrow-right { right: -2px; }
  .tabs-product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }
}

@media (max-width: 480px) {
  .product-card-direct {
    width: 140px;
  }
  .product-image-direct {
    height: 140px;
  }
  .featured-card {
    width: 130px;
    height: 175px;
  }
  .featured-title {
    font-size: 11px;
  }
  .section-title-line {
    font-size: 17px;
    gap: 6px;
  }
  .product-info-direct {
    padding: 12px 14px 16px;
  }
  .product-name-direct {
    font-size: 13px;
    height: 34px;
  }
  .current-price-direct {
    font-size: 14px;
  }
  .original-price-direct {
    font-size: 11px;
  }
  .option-tag {
    font-size: 10px;
    padding: 1px 8px;
  }
  .tabs-product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
}
</style>