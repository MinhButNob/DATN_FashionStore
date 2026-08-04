<template>
  <div class="shop-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <!-- Breadcrumb -->
        <div class="breadcrumb">
          <router-link to="/">Trang chủ</router-link>
          <i class="fas fa-chevron-right"></i>
          <span>{{ selectedCategoryName || 'Cửa hàng' }}</span>
        </div>

        <div class="shop-layout">
          <!-- Sidebar Filters -->
          <aside class="sidebar">
            <!-- Danh mục -->
            <div class="filter-widget">
              <h3 class="filter-header" @click="toggleFilter('category')">
                <span>📂 Danh mục</span>
                <span class="toggle-icon">{{ expandedFilters.category ? '−' : '+' }}</span>
              </h3>
              <div class="collapsible" :class="{ open: expandedFilters.category }">
                <div class="collapsible-inner category-tree">
                  <div v-for="category in categoryTree" :key="category.categoryId" class="category-node">
                    <div class="category-row">
                      <a 
                        class="category-link"
                        :class="getCategoryClass(category)"
                        @click="selectCategory(category)"
                      >
                        <span>{{ category.categoryName }}</span>
                      </a>
                      <i
                        v-if="category.children && category.children.length"
                        class="fas fa-chevron-down expand-icon"
                        :class="{ open: expandedCategoryIds.has(category.categoryId) }"
                        @click.stop="toggleExpand(category.categoryId)"
                      ></i>
                    </div>

                    <div
                      v-if="category.children && category.children.length"
                      class="collapsible children-collapsible"
                      :class="{ open: expandedCategoryIds.has(category.categoryId) }"
                    >
                      <ul class="collapsible-inner category-children">
                        <li v-for="child in category.children" :key="child.categoryId">
                          <a 
                            class="category-link child-link"
                            :class="{ active: selectedCategoryIds.includes(child.categoryId) }"
                            @click="selectCategory(child)"
                          >
                            <span>{{ child.categoryName }}</span>
                          </a>
                        </li>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Khoảng giá -->
            <div class="filter-widget">
              <h3>💰 Khoảng giá</h3>
              <div class="price-range">
                <input 
                  type="range" 
                  min="0" 
                  max="4000000" 
                  step="50000"
                  v-model="priceRange"
                  @input="applyFilters"
                >
                <div class="price-values">
                  <span>0₫</span>
                  <span>{{ formatPrice(priceRange) }}</span>
                  <span>4.000.000₫</span>
                </div>
              </div>
            </div>

            <!-- Kích thước -->
            <div class="filter-widget">
              <h3 class="filter-header" @click="toggleFilter('size')">
                <span>📏 Kích thước</span>
                <span class="toggle-icon">{{ expandedFilters.size ? '−' : '+' }}</span>
              </h3>
              <div class="collapsible" :class="{ open: expandedFilters.size }">
                <div class="collapsible-inner size-options">
                  <button
                    v-for="size in allSizes"
                    :key="size.sizeId"
                    class="size-btn"
                    :class="{ active: selectedSizeIds.includes(size.sizeId) }"
                    @click="toggleSize(size.sizeId)"
                  >
                    {{ size.sizeName }}
                  </button>
                </div>
              </div>
            </div>

            <button class="clear-filters" @click="clearAllFilters">
              🗑️ Xóa tất cả bộ lọc
            </button>
          </aside>

          <!-- Main Content -->
          <div class="main-content">
            <!-- Loading -->
            <div v-if="loading" class="loading-state">
              <div class="spinner"></div>
              <p>Đang tải sản phẩm...</p>
            </div>

            <!-- No Products -->
            <div v-else-if="filteredProducts.length === 0" class="no-products">
              <i class="fas fa-box-open"></i>
              <h3>Không tìm thấy sản phẩm</h3>
              <p>Vui lòng thử lại với bộ lọc khác</p>
              <button class="btn-primary" @click="clearAllFilters">Xóa bộ lọc</button>
            </div>

            <!-- Products -->
            <div v-else>
              <div class="product-header">
                <div class="product-count">
                  {{ selectedCategoryName || 'Tất cả sản phẩm' }}
                  <span class="product-count-number">({{ filteredProducts.length }} sản phẩm)</span>
                </div>
                <div class="sort-options">
                  <label>Sắp xếp:</label>
                  <select v-model="sortBy" @change="applyFilters" class="sort-select">
                    <option value="newest">Mới nhất</option>
                    <option value="price_asc">Giá tăng dần</option>
                    <option value="price_desc">Giá giảm dần</option>
                    <option value="name_asc">Tên A-Z</option>
                  </select>
                </div>
              </div>

              <!-- Product Grid -->
              <div class="product-container">
                <ProductCard 
                  v-for="product in paginatedProducts" 
                  :key="product.productId"
                  :product="product"
                />
              </div>

              <!-- Pagination -->
              <div class="pagination" v-if="totalPages > 1">
                <button 
                  class="page-btn"
                  :disabled="currentPage === 1"
                  @click="changePage(currentPage - 1)"
                >
                  <i class="fas fa-chevron-left"></i>
                </button>

                <button 
                  v-for="page in displayedPages" 
                  :key="page"
                  class="page-btn"
                  :class="{ active: currentPage === page }"
                  @click="changePage(page)"
                >
                  {{ page }}
                </button>

                <button 
                  class="page-btn"
                  :disabled="currentPage === totalPages"
                  @click="changePage(currentPage + 1)"
                >
                  <i class="fas fa-chevron-right"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
    <CartModal />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import CartModal from '../../components/client/CartModal.vue'
import ProductCard from '../../components/client/ProductCard.vue'
import { useCartStore } from '../../stores/cartStore'
import { useNotification } from '../../composables/useNotification'
import { useWishlistStore } from '../../stores/wishlistStore'
import { useAuthStore } from '../../stores/authStore'
import api from "@/api/axios"

// ==================== Composables ====================
const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const authStore = useAuthStore()
const wishlistStore = useWishlistStore()
const { showNotification } = useNotification()

// ==================== State ====================
const loading = ref(false)
const allProducts = ref([])
const categories = ref([])
const allSizes = ref([])
const currentPage = ref(1)
const priceRange = ref(4000000)
const sortBy = ref('newest')
const itemsPerPage = 12

// Filter states
const selectedCategoryIds = ref([])
const selectedSizeIds = ref([])
const expandedFilters = ref({ category: true, size: true })
const expandedCategoryIds = ref(new Set())

// ==================== Constants ====================
const SALE_CATEGORY_ID = 'sale'
const NEW_CATEGORY_ID = 'new'
const LOCKED_PARENT_NAMES = ['áo', 'quần', 'phụ kiện']

// ==================== Computed ====================
const selectedCategoryName = computed(() => {
  if (selectedCategoryIds.value.length === 1) {
    const id = selectedCategoryIds.value[0]
    if (id === SALE_CATEGORY_ID) return 'Khuyến mãi'
    if (id === NEW_CATEGORY_ID) return 'Sản phẩm mới'
    const cat = categories.value.find(c => c.categoryId === id)
    return cat?.categoryName || 'Cửa hàng'
  }
  return 'Cửa hàng'
})

const categoryTree = computed(() => {
  const all = categories.value

  // Danh mục ảo: Sản phẩm mới
  const newArrivals = {
    categoryId: NEW_CATEGORY_ID,
    categoryName: 'Sản phẩm mới',
    productCount: allProducts.value.length,
    children: []
  }

  // Danh mục ảo: Khuyến mãi - CHỈ lấy sản phẩm có khuyến mãi thực sự
  const saleProducts = allProducts.value.filter(p => p.discountedPrice && p.discountedPrice < p.displayPrice)
  const promoNode = {
    categoryId: SALE_CATEGORY_ID,
    categoryName: 'Khuyến mãi',
    productCount: saleProducts.length,
    children: []
  }

  // Danh mục thật từ DB
  const realCategories = LOCKED_PARENT_NAMES
    .map(name => all.find(c => !c.parentCategoryId && normalizeName(c.categoryName) === name))
    .filter(Boolean)
    .map(parent => {
      const children = all.filter(c => c.parentCategoryId === parent.categoryId)
      const childrenTotal = children.reduce((sum, c) => sum + (c.productCount || 0), 0)
      return {
        ...parent,
        productCount: (parent.productCount || 0) + childrenTotal,
        children
      }
    })

  return [newArrivals, promoNode, ...realCategories]
})

const filteredProducts = computed(() => {
  let result = [...allProducts.value]

  // 1. Lọc danh mục
  if (selectedCategoryIds.value.length > 0) {
    result = applyCategoryFilter(result)
  }

  // 2. Lọc giá
  result = result.filter(p => {
    const price = getProductPrice(p)
    return price <= priceRange.value
  })

  // 3. Lọc kích thước
  if (selectedSizeIds.value.length > 0) {
    result = result.filter(p => 
      p.sizeIds?.some(id => selectedSizeIds.value.includes(id))
    )
  }

  // 4. Sắp xếp
  result.sort((a, b) => {
    const priceA = getProductPrice(a)
    const priceB = getProductPrice(b)

    switch (sortBy.value) {
      case 'price_asc': return priceA - priceB
      case 'price_desc': return priceB - priceA
      case 'name_asc': return (a.productName || '').localeCompare(b.productName || '')
      default: return b.productId - a.productId
    }
  })

  return result
})

const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredProducts.value.slice(start, end)
})

const totalPages = computed(() => Math.ceil(filteredProducts.value.length / itemsPerPage))

const displayedPages = computed(() => {
  const delta = 2
  const range = []
  const rangeWithDots = []
  let l

  for (let i = 1; i <= totalPages.value; i++) {
    if (i === 1 || i === totalPages.value || (i >= currentPage.value - delta && i <= currentPage.value + delta)) {
      range.push(i)
    }
  }

  range.forEach((i) => {
    if (l) {
      if (i - l === 2) {
        rangeWithDots.push(l + 1)
      } else if (i - l !== 1) {
        rangeWithDots.push('...')
      }
    }
    rangeWithDots.push(i)
    l = i
  })

  return rangeWithDots
})

// ==================== Helper Functions ====================
const normalizeName = (name) => (name || '').toString().trim().toLowerCase()
const getProductPrice = (product) => 
  product.discountedPrice && product.discountedPrice < product.displayPrice 
    ? product.discountedPrice 
    : product.displayPrice

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

// ==================== Category Logic ====================
const getCategoryClass = (category) => {
  const classes = []
  if (category.children && category.children.length) classes.push('is-parent')
  if (selectedCategoryIds.value.includes(category.categoryId)) classes.push('active')
  if (expandedCategoryIds.value.has(category.categoryId)) classes.push('expanded')
  return classes.join(' ')
}

const toggleExpand = (id) => {
  const next = new Set(expandedCategoryIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  expandedCategoryIds.value = next
}

const toggleFilter = (key) => {
  expandedFilters.value[key] = !expandedFilters.value[key]
}

const selectCategory = (category) => {
  const isVirtual = category.categoryId === NEW_CATEGORY_ID || category.categoryId === SALE_CATEGORY_ID

  if (isVirtual) {
    // Danh mục ảo: chỉ cho chọn 1 trong 2
    const otherId = category.categoryId === NEW_CATEGORY_ID ? SALE_CATEGORY_ID : NEW_CATEGORY_ID
    
    if (selectedCategoryIds.value.includes(category.categoryId)) {
      selectedCategoryIds.value = selectedCategoryIds.value.filter(id => id !== category.categoryId)
    } else {
      selectedCategoryIds.value = selectedCategoryIds.value.filter(id => id !== otherId)
      selectedCategoryIds.value = [...selectedCategoryIds.value, category.categoryId]
    }
  } else if (category.children && category.children.length) {
    // Danh mục cha: chỉ mở/đóng
    toggleExpand(category.categoryId)
  } else {
    // Danh mục con: toggle chọn
    const index = selectedCategoryIds.value.indexOf(category.categoryId)
    if (index === -1) {
      selectedCategoryIds.value = [...selectedCategoryIds.value, category.categoryId]
    } else {
      selectedCategoryIds.value = selectedCategoryIds.value.filter(id => id !== category.categoryId)
    }
  }
  applyFilters()
}

const applyCategoryFilter = (products) => {
  const hasNew = selectedCategoryIds.value.includes(NEW_CATEGORY_ID)
  const hasSale = selectedCategoryIds.value.includes(SALE_CATEGORY_ID)
  const realIds = selectedCategoryIds.value.filter(id => id !== NEW_CATEGORY_ID && id !== SALE_CATEGORY_ID)

  let result = products

  // Lọc theo danh mục thật
  if (realIds.length > 0) {
    result = result.filter(p => realIds.includes(p.categoryId))
  }

  // Lọc "Khuyến mãi": CHỈ giữ sản phẩm đang có khuyến mãi thực sự
  // KHÔNG phụ thuộc danh mục
  if (hasSale) {
    result = result.filter(p => p.discountedPrice && p.discountedPrice < p.displayPrice)
  }

  return result
}

// ==================== Size Logic ====================
const toggleSize = (id) => {
  const index = selectedSizeIds.value.indexOf(id)
  if (index === -1) {
    selectedSizeIds.value = [...selectedSizeIds.value, id]
  } else {
    selectedSizeIds.value = selectedSizeIds.value.filter(i => i !== id)
  }
  applyFilters()
}

// ==================== Filter Actions ====================
const applyFilters = () => {
  currentPage.value = 1
  syncCategoryToUrl()
}

const clearAllFilters = () => {
  selectedCategoryIds.value = []
  selectedSizeIds.value = []
  priceRange.value = 4000000
  sortBy.value = 'newest'
  currentPage.value = 1
  
  const { categoryId, category, subcategory, categoryName, ...rest } = route.query
  router.replace({ query: rest })
  showNotification('Đã xóa tất cả bộ lọc', 'success')
}

const syncCategoryToUrl = () => {
  const { categoryId, category, subcategory, categoryName, ...rest } = route.query
  
  if (selectedCategoryIds.value.length === 1) {
    router.replace({ query: { ...rest, categoryId: selectedCategoryIds.value[0] } })
  } else if (route.query.categoryId || route.query.category || route.query.subcategory || route.query.categoryName) {
    router.replace({ query: rest })
  }
}

// ==================== Sync from URL ====================
const syncCategoryFromRoute = () => {
  const qCategoryName = route.query.categoryName
  const rawId = route.query.categoryId ?? route.query.subcategory ?? route.query.category

  // Kiểm tra danh mục ảo "Khuyến mãi" / "Sản phẩm mới" - nhận diện từ CẢ categoryId
  // (?categoryId=sale) LẪN categoryName (?categoryName=Khuyến Mãi), để link luôn hoạt động
  // đúng và không phụ thuộc vào việc sản phẩm thuộc danh mục thật nào.
  const virtualKey = normalizeName(rawId) || normalizeName(qCategoryName)

  if (virtualKey === SALE_CATEGORY_ID || virtualKey === 'khuyến mãi') {
    if (!(selectedCategoryIds.value.length === 1 && selectedCategoryIds.value[0] === SALE_CATEGORY_ID)) {
      selectedCategoryIds.value = [SALE_CATEGORY_ID]
      currentPage.value = 1
    }
    return
  }

  if (virtualKey === NEW_CATEGORY_ID || virtualKey === 'sản phẩm mới') {
    if (!(selectedCategoryIds.value.length === 1 && selectedCategoryIds.value[0] === NEW_CATEGORY_ID)) {
      selectedCategoryIds.value = [NEW_CATEGORY_ID]
      currentPage.value = 1
    }
    return
  }

  if (qCategoryName) {
    const normalized = normalizeName(qCategoryName)
    // Kiểm tra danh mục thật
    const found = categories.value.find(c => normalizeName(c.categoryName) === normalized)
    if (found) {
      if (!(selectedCategoryIds.value.length === 1 && selectedCategoryIds.value[0] === found.categoryId)) {
        selectedCategoryIds.value = [found.categoryId]
        currentPage.value = 1
      }
      return
    }
  }

  if (rawId) {
    const id = Number(rawId)
    if (!isNaN(id) && !(selectedCategoryIds.value.length === 1 && selectedCategoryIds.value[0] === id)) {
      selectedCategoryIds.value = [id]
      currentPage.value = 1
    }
    return
  }

  if (selectedCategoryIds.value.length > 0) {
    selectedCategoryIds.value = []
  }
}

// ==================== Navigation ====================
const goToProductDetail = (id) => router.push(`/product/${id}`)

const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// ==================== Cart & Wishlist ====================
const addToCartDirect = (product) => {
  const stock = product.totalStock || 0
  if (stock === 0) {
    showNotification('Sản phẩm đã hết hàng!', 'error')
    return
  }
  
  const price = product.discountedPrice && product.discountedPrice < product.displayPrice 
    ? product.discountedPrice 
    : product.displayPrice
    
  cartStore.addToCart({
    productId: product.productId,
    productName: product.productName,
    price: price,
    quantity: 1,
    image: product.mainImageUrl
  })
  showNotification('Đã thêm sản phẩm vào giỏ hàng!', 'success')
}

const handleWishlistToggle = async (productId) => {
  if (!authStore.isAuthenticated) {
    window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'login' }))
    return
  }
  
  const isAdded = !wishlistStore.isInWishlist(productId)
  const success = await wishlistStore.toggleWishlist(productId)
  
  if (success) {
    showNotification(
      isAdded ? 'Đã thêm vào danh sách yêu thích!' : 'Đã xóa khỏi danh sách yêu thích!',
      isAdded ? 'success' : 'info'
    )
  } else {
    showNotification('Có lỗi xảy ra, vui lòng thử lại!', 'error')
  }
}

// ==================== API Calls ====================
const fetchSizes = async () => {
  try {
    const response = await api.get('/sizes', { params: { page: 0, size: 100 } })
    const sizes = response.data?.data?.content || response.data?.content || response.data || []
    const order = { 'S': 1, 'M': 2, 'L': 3, 'XL': 4, 'XXL': 5 }
    allSizes.value = sizes
      .filter(s => s.status === 'ACTIVE')
      .sort((a, b) => (order[a.sizeName] || 99) - (order[b.sizeName] || 99))
  } catch (error) {
    console.error('Lỗi tải kích thước:', error)
  }
}

const fetchProducts = async () => {
  loading.value = true
  try {
    const response = await api.get('/customer/home/latest-products', { params: { page: 0, size: 1000 } })
    const products = response.data?.products || response.data?.data?.products || response.data || []
    
    allProducts.value = products.map(p => {
      const sizeIds = []
      const colorOptions = []
      
      if (p.variants && p.variants.length > 0) {
        p.variants.forEach(v => {
          if (v.sizeId && !sizeIds.includes(v.sizeId)) sizeIds.push(v.sizeId)
          if (v.colorName && !colorOptions.includes(v.colorName)) colorOptions.push(v.colorName)
        })
      }
      
      let displayPrice = Number(p.minPrice) || 0
      let discountedPrice = p.discountedPrice ? Number(p.discountedPrice) : null

      if (p.variants && p.variants.length > 0) {
        const validPrices = p.variants.map(v => Number(v.price || 0)).filter(pr => pr > 0)
        const saleVariants = p.variants.filter(v => v.discountedPrice && Number(v.discountedPrice) < Number(v.price || 0))

        if (saleVariants.length > 0) {
          if (validPrices.length > 0) {
            displayPrice = Math.min(...validPrices)
          }
          discountedPrice = Math.min(...saleVariants.map(v => Number(v.discountedPrice)))
        }
      }
      
      return {
        productId: p.productId,
        productName: p.productName,
        categoryName: p.categoryName,
        categoryId: p.categoryId,
        displayPrice: displayPrice,
        discountedPrice: discountedPrice,
        discountPercent: calculateDiscountPercent(displayPrice, discountedPrice),
        maxPrice: Number(p.maxPrice) || 0,
        totalStock: Number(p.totalStock) || 0,
        mainImageUrl: p.mainImageUrl,
        colorOptions: colorOptions,
        sizeIds: sizeIds,
        variants: p.variants || []
      }
    })

    await fetchCategories()
    syncCategoryFromRoute()
  } catch (error) {
    console.error('Lỗi tải sản phẩm:', error)
    allProducts.value = []
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const response = await api.get('/categories/active')
    const data = response.data || []
    
    const productCount = {}
    allProducts.value.forEach(p => {
      if (p.categoryId) {
        productCount[p.categoryId] = (productCount[p.categoryId] || 0) + 1
      }
    })
    
    categories.value = data.map(cat => ({
      ...cat,
      productCount: productCount[cat.categoryId] || 0
    }))
  } catch (error) {
    console.error('Lỗi tải danh mục:', error)
  }
}

// ==================== Watch ====================
watch(
  () => [route.query.categoryId, route.query.category, route.query.subcategory, route.query.categoryName],
  () => syncCategoryFromRoute()
)

// ==================== Lifecycle ====================
onMounted(async () => {
  await fetchSizes()
  await fetchProducts()
  if (authStore.isAuthenticated) {
    await wishlistStore.fetchWishlist()
  }
})
</script>

<style scoped>
/* ===== Base ===== */
.shop-page {
  min-height: 100vh;
}

main {
  margin-top: var(--header-offset, 140px);
  padding: 20px 0 50px;
  transition: margin-top 0.32s cubic-bezier(0.4, 0, 0.2, 1);
}

.container {
  max-width: 1320px;
  margin: 0 auto;
  padding: 0 20px;
}

/* ===== Breadcrumb ===== */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  color: #999;
  font-size: 14px;
}

.breadcrumb a {
  color: #999;
  text-decoration: none;
}

.breadcrumb a:hover {
  color: #e63946;
}

/* ===== Layout ===== */
.shop-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
  margin-top: 20px;
}

/* ===== Sidebar ===== */
.sidebar {
  background: white;
  border-radius: 15px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  height: fit-content;
  position: sticky;
  top: calc(var(--header-offset, 140px) + 20px);
  transition: top 0.32s cubic-bezier(0.4, 0, 0.2, 1);
}

.filter-widget {
  margin-bottom: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid #eee;
}

.filter-widget:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.filter-widget h3 {
  color: #2c3e50;
  font-size: 16px;
  margin-bottom: 15px;
  font-weight: 600;
}

.filter-widget h3.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  user-select: none;
}

.filter-widget h3.filter-header .toggle-icon {
  font-size: 18px;
  font-weight: 700;
  color: #999;
  line-height: 1;
}

/* ===== Collapsible ===== */
.collapsible {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.3s ease;
}

.collapsible.open {
  grid-template-rows: 1fr;
}

.collapsible-inner {
  overflow: hidden;
}

.children-collapsible {
  transition: grid-template-rows 0.25s ease;
}

.children-collapsible .category-children {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.children-collapsible.open .category-children {
  opacity: 1;
  transition: opacity 0.25s ease 0.1s;
}

/* ===== Category Tree ===== */
.category-tree {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.category-node {
  padding: 2px 0;
}

.category-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
}

.category-link {
  display: flex;
  align-items: center;
  flex: 1;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  text-decoration: none;
  color: #2c3e50;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  background: transparent;
  border: none;
  width: 100%;
  gap: 6px;
}

.category-link:hover {
  background: white;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
  color: #e63946;
}

.category-link.active {
  background: transparent;
  color: #e63946;
  font-weight: 700;
  box-shadow: none;
}

.category-link.active:hover {
  background: white;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
  color: #c82333;
}

.category-link.is-parent.active {
  background: transparent;
  color: #2c3e50;
  font-weight: 500;
  box-shadow: none;
}

.category-link.is-parent.active:hover {
  background: white;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
  color: #e63946;
}

.category-count {
  font-size: 12px;
  color: #999;
  font-weight: 400;
}

.category-children {
  list-style: none;
  margin: 2px 0 4px 0;
  padding-left: 20px;
  border-left: 2px solid #f0f0f0;
}

.category-children li {
  margin-bottom: 2px;
}

.child-link {
  font-size: 13px;
  font-weight: 400;
  padding: 6px 12px;
  color: #666;
}

.child-link:hover {
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
  color: #e63946;
}

.child-link.active {
  background: transparent;
  color: #e63946;
  font-weight: 700;
  box-shadow: none;
}

.child-link.active:hover {
  background: white;
  color: #c82333;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.expand-icon {
  font-size: 12px;
  color: #999;
  cursor: pointer;
  padding: 8px 12px;
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.expand-icon.open {
  transform: rotate(180deg);
  color: #e63946;
}

.expand-icon:hover {
  color: #e63946;
}

/* ===== Size Options ===== */
.size-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 5px;
}

.size-btn {
  padding: 8px 20px;
  border: 2px solid #e0e0e0;
  border-radius: 25px;
  background: white;
  color: #555;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 50px;
  text-align: center;
  text-transform: uppercase;
}

.size-btn:hover {
  border-color: #e63946;
  color: #e63946;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(230, 57, 70, 0.15);
}

.size-btn.active {
  background: #e63946;
  color: white;
  border-color: #e63946;
  box-shadow: 0 4px 15px rgba(230, 57, 70, 0.3);
}

.size-btn.active:hover {
  background: #c82333;
  border-color: #c82333;
  box-shadow: 0 6px 20px rgba(230, 57, 70, 0.4);
}

/* ===== Price Range ===== */
.price-range {
  padding: 0 5px;
}

.price-range input[type="range"] {
  width: 100%;
  margin-bottom: 10px;
  accent-color: #e63946;
}

.price-values {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #999;
}

/* ===== Clear Filters ===== */
.clear-filters {
  width: 100%;
  padding: 12px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 15px;
  font-weight: 500;
}

.clear-filters:hover {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

/* ===== Main Content ===== */
.main-content {
  min-width: 0;
}

/* ===== Product Header ===== */
.product-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 15px 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
  gap: 15px;
}

.product-count {
  color: #666;
  font-size: 14px;
}

.product-count-number {
  color: #999;
  font-size: 13px;
  font-weight: 400;
  margin-left: 4px;
}

.sort-options {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-options label {
  color: #666;
  font-size: 14px;
}

.sort-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  outline: none;
  cursor: pointer;
}

/* ===== Product Grid ===== */
.product-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px 16px;
  margin-bottom: 30px;
}

/* ===== Product Card ===== */
.product-card-direct {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  cursor: pointer;
}

.product-card-direct:hover {
  transform: translateY(-6px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.product-image-direct {
  height: 220px;
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
  font-size: 60px;
  color: #ccc;
}

.discount-flag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #e63946;
  color: white;
  padding: 3px 8px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: bold;
}

.product-info-direct {
  padding: 12px 14px 14px;
}

.product-options {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.option-tag {
  font-size: 11px;
  color: #666;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.product-name-direct {
  font-size: 16px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 8px;
  line-height: 1.35;
  height: 44px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price-direct {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.original-price-direct {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
}

.current-price-direct {
  font-size: 15px;
  font-weight: 700;
  color: #e63946;
}

.product-stock-direct {
  font-size: 11px;
  margin-bottom: 10px;
  color: #28a745;
}

.product-stock-direct.out-of-stock {
  color: #e63946;
}

.product-stock-direct i {
  margin-right: 4px;
}

.add-to-cart-direct {
  width: 100%;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border: none;
  padding: 9px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 600;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.add-to-cart-direct:hover:not(:disabled) {
  background: linear-gradient(135deg, #c82333, #e63946);
  transform: translateY(-2px);
}

.add-to-cart-direct:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.product-wishlist-direct {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 35px;
  height: 35px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
  z-index: 10;
}

.product-wishlist-direct:hover {
  background: #e63946;
  color: white;
  transform: scale(1.1);
}

.product-wishlist-direct i {
  font-size: 16px;
  transition: color 0.3s;
}

.product-wishlist-direct .fas.fa-heart {
  color: #e63946;
}

.product-wishlist-direct:hover .fas.fa-heart {
  color: white;
}

/* ===== Pagination ===== */
.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 30px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.page-btn.active {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== Loading & Empty ===== */
.loading-state {
  text-align: center;
  padding: 60px;
  background: white;
  border-radius: 15px;
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

.no-products {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 15px;
}

.no-products i {
  font-size: 60px;
  color: #999;
  margin-bottom: 20px;
}

.no-products h3 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.no-products p {
  color: #666;
  margin-bottom: 20px;
}

.btn-primary {
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #c82333, #e63946);
}

/* ===== Responsive ===== */
@media (max-width: 992px) {
  .shop-layout {
    grid-template-columns: 1fr;
  }
  
  .sidebar {
    position: static;
  }

  .product-container {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }
  
  .product-container {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px 12px;
  }
  
  .product-image-direct {
    height: 180px;
  }
  
  .product-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .product-name-direct {
    font-size: 15px;
    height: 40px;
  }
  
  .current-price-direct {
    font-size: 14px;
  }
  
  .category-children {
    padding-left: 15px;
  }
  
  .size-btn {
    padding: 6px 16px;
    font-size: 13px;
    min-width: 40px;
  }
}

@media (max-width: 480px) {
  .product-container {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .product-image-direct {
    height: 150px;
  }

  .product-info-direct {
    padding: 8px 10px 10px;
  }
}
</style>