<template>
  <div class="shop-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <!-- Breadcrumb -->
        <div class="breadcrumb">
          <router-link to="/">Trang chủ</router-link>
          <i class="fas fa-chevron-right"></i>
          <span>Cửa hàng</span>
        </div>

        <!-- Filter Bar -->
        <FilterBar />

        <div class="shop-layout">
          <!-- Sidebar Filters -->
          <aside class="sidebar">
            <div class="filter-widget">
              <h3>Danh mục</h3>
              <ul>
                <li v-for="category in categories" :key="category.id">
                  <label>
                    <input 
                      type="checkbox" 
                      :value="category.slug"
                      v-model="selectedCategories"
                      @change="applyFilters"
                    >
                    {{ category.name }} ({{ category.count }})
                  </label>
                </li>
              </ul>
            </div>

            <div class="filter-widget">
              <h3>Khoảng giá</h3>
              <div class="price-range">
                <input 
                  type="range" 
                  min="0" 
                  max="1000000" 
                  step="10000"
                  v-model="priceRange"
                  @input="applyFilters"
                >
                <div class="price-values">
                  <span>0₫</span>
                  <span>{{ formatPrice(priceRange) }}</span>
                  <span>1.000.000₫</span>
                </div>
              </div>
            </div>

            <div class="filter-widget">
              <h3>Đánh giá</h3>
              <ul>
                <li v-for="rating in 5" :key="rating">
                  <label>
                    <input 
                      type="checkbox" 
                      :value="rating"
                      v-model="selectedRatings"
                      @change="applyFilters"
                    >
                    <span class="stars">
                      <i v-for="i in rating" :key="i" class="fas fa-star"></i>
                      <i v-for="i in (5-rating)" :key="'empty'+i" class="far fa-star"></i>
                    </span>
                    {{ rating }} sao trở lên
                  </label>
                </li>
              </ul>
            </div>

            <div class="filter-widget">
              <h3>Tags</h3>
              <div class="tags">
                <button 
                  v-for="tag in tags" 
                  :key="tag"
                  :class="{ active: selectedTags.includes(tag) }"
                  @click="toggleTag(tag)"
                >
                  {{ tag }}
                </button>
              </div>
            </div>

            <button class="btn btn-outline clear-filters" @click="clearFilters">
              Xóa bộ lọc
            </button>
          </aside>

          <!-- Main Content -->
          <main class="main-content">
            <!-- Active Filters -->
            <div v-if="hasActiveFilters" class="active-filters">
              <span>Bộ lọc đang chọn:</span>
              <div class="filter-tags">
                <span 
                  v-for="filter in activeFilters" 
                  :key="filter"
                  class="filter-tag"
                >
                  {{ filter }}
                  <i class="fas fa-times" @click="removeFilter(filter)"></i>
                </span>
              </div>
            </div>

            <!-- Loading State -->
            <div v-if="loading" class="loading">
              <div class="spinner"></div>
              <p>Đang tải sản phẩm...</p>
            </div>

            <!-- No Products -->
            <div v-else-if="filteredProducts.length === 0" class="no-products">
              <i class="fas fa-box-open"></i>
              <h3>Không tìm thấy sản phẩm</h3>
              <p>Vui lòng thử lại với bộ lọc khác</p>
              <button class="btn btn-primary" @click="clearFilters">
                Xóa bộ lọc
              </button>
            </div>

            <!-- Product Grid -->
            <div v-else>
              <div class="product-header">
                <div class="product-count">
                  Hiển thị {{ paginatedProducts.length }} / {{ filteredProducts.length }} sản phẩm
                </div>
                <div class="view-options">
                  <button 
                    :class="{ active: gridView }"
                    @click="gridView = true"
                  >
                    <i class="fas fa-th"></i>
                  </button>
                  <button 
                    :class="{ active: !gridView }"
                    @click="gridView = false"
                  >
                    <i class="fas fa-list"></i>
                  </button>
                </div>
              </div>

              <div 
                class="product-container" 
                :class="{ 'list-view': !gridView }"
              >
                <ProductCard
                  v-for="product in paginatedProducts"
                  :key="product.id"
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
          </main>
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
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import ProductCard from '../../components/client/ProductCard.vue'
import FilterBar from '../../components/client/FilterBar.vue'
import CartModal from '../../components/client/CartModal.vue'
import { formatPrice } from '../../utils/helpers'

const route = useRoute()
const router = useRouter()
const productStore = useProductStore()

// State
const loading = ref(false)
const gridView = ref(true)
const currentPage = ref(1)
const itemsPerPage = 12
const priceRange = ref(500000)

const selectedCategories = ref([])
const selectedRatings = ref([])
const selectedTags = ref([])

const categories = [
  { id: 1, name: 'Thời trang nam', slug: 'men', count: 45 },
  { id: 2, name: 'Thời trang nữ', slug: 'women', count: 67 },
  { id: 3, name: 'Trẻ em', slug: 'kids', count: 23 },
  { id: 4, name: 'Phụ kiện', slug: 'accessories', count: 34 }
]

const tags = ['Hot', 'Sale', 'Mới', 'Giảm giá']

// Computed
const filteredProducts = computed(() => {
  let filtered = productStore.products

  // Category filter
  if (selectedCategories.value.length > 0) {
    filtered = filtered.filter(p => selectedCategories.value.includes(p.category))
  }

  // Price filter
  filtered = filtered.filter(p => p.price <= priceRange.value)

  // Rating filter
  if (selectedRatings.value.length > 0) {
    filtered = filtered.filter(p => 
      selectedRatings.value.some(r => p.rating >= r)
    )
  }

  // Tags filter
  if (selectedTags.value.length > 0) {
    filtered = filtered.filter(p => 
      p.tag && selectedTags.value.includes(p.tag)
    )
  }

  // Search filter
  if (route.query.search) {
    const searchTerm = route.query.search.toLowerCase()
    filtered = filtered.filter(p => 
      p.name.toLowerCase().includes(searchTerm)
    )
  }

  return filtered
})

const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredProducts.value.slice(start, end)
})

const totalPages = computed(() => {
  return Math.ceil(filteredProducts.value.length / itemsPerPage)
})

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

const hasActiveFilters = computed(() => {
  return selectedCategories.value.length > 0 ||
         selectedRatings.value.length > 0 ||
         selectedTags.value.length > 0 ||
         priceRange.value < 1000000
})

const activeFilters = computed(() => {
  const filters = []

  selectedCategories.value.forEach(cat => {
    const category = categories.find(c => c.slug === cat)
    if (category) filters.push(category.name)
  })

  selectedTags.value.forEach(tag => filters.push(`Tag: ${tag}`))
  selectedRatings.value.forEach(r => filters.push(`${r} sao trở lên`))
  
  if (priceRange.value < 1000000) {
    filters.push(`Dưới ${formatPrice(priceRange.value)}`)
  }

  return filters
})

// Methods
const applyFilters = () => {
  currentPage.value = 1
}

const toggleTag = (tag) => {
  const index = selectedTags.value.indexOf(tag)
  if (index === -1) {
    selectedTags.value.push(tag)
  } else {
    selectedTags.value.splice(index, 1)
  }
  applyFilters()
}

const removeFilter = (filter) => {
  if (filter.includes('Dưới')) {
    priceRange.value = 1000000
  } else if (filter.includes('Tag:')) {
    const tag = filter.replace('Tag: ', '')
    selectedTags.value = selectedTags.value.filter(t => t !== tag)
  } else if (filter.includes('sao')) {
    const rating = parseInt(filter.split(' ')[0])
    selectedRatings.value = selectedRatings.value.filter(r => r !== rating)
  } else {
    const category = categories.find(c => c.name === filter)
    if (category) {
      selectedCategories.value = selectedCategories.value.filter(c => c !== category.slug)
    }
  }
  applyFilters()
}

const clearFilters = () => {
  selectedCategories.value = []
  selectedRatings.value = []
  selectedTags.value = []
  priceRange.value = 1000000
  currentPage.value = 1
}

const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// Lifecycle
onMounted(() => {
  if (route.query.category) {
    selectedCategories.value = [route.query.category]
  }
  if (route.query.search) {
    productStore.setFilter('search', route.query.search)
  }
})
</script>

<style scoped>
.shop-page {
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
  margin-bottom: 20px;
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

.shop-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 30px;
  margin-top: 20px;
}

/* Sidebar */
.sidebar {
  background: white;
  border-radius: 15px;
  padding: 25px;
  box-shadow: var(--shadow);
  height: fit-content;
}

.filter-widget {
  margin-bottom: 25px;
  padding-bottom: 25px;
  border-bottom: 1px solid var(--border-color);
}

.filter-widget:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.filter-widget h3 {
  color: var(--dark-gray);
  font-size: 16px;
  margin-bottom: 15px;
}

.filter-widget ul {
  list-style: none;
}

.filter-widget ul li {
  margin-bottom: 10px;
}

.filter-widget ul li label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--medium-gray);
  font-size: 14px;
}

.filter-widget ul li input[type="checkbox"] {
  cursor: pointer;
  accent-color: var(--primary-red);
}

.price-range {
  padding: 0 5px;
}

.price-range input[type="range"] {
  width: 100%;
  margin-bottom: 10px;
  accent-color: var(--primary-red);
}

.price-values {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--medium-gray);
}

.stars {
  color: #ffc107;
  margin-right: 5px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tags button {
  padding: 6px 12px;
  background: var(--light-gray);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 12px;
}

.tags button:hover,
.tags button.active {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
}

.clear-filters {
  width: 100%;
}

/* Main Content */
.main-content {
  min-height: 500px;
}

.product-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 15px 20px;
  border-radius: 10px;
  box-shadow: var(--shadow);
}

.product-count {
  color: var(--medium-gray);
  font-size: 14px;
}

.view-options {
  display: flex;
  gap: 5px;
}

.view-options button {
  width: 35px;
  height: 35px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.3s;
}

.view-options button:hover,
.view-options button.active {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
}

/* Active Filters */
.active-filters {
  background: white;
  padding: 15px 20px;
  border-radius: 10px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
  box-shadow: var(--shadow);
}

.active-filters span {
  color: var(--medium-gray);
  font-size: 14px;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1;
}

.filter-tag {
  background: var(--light-gray);
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.filter-tag i {
  cursor: pointer;
  font-size: 12px;
  transition: color 0.3s;
}

.filter-tag i:hover {
  color: var(--primary-red);
}

/* Loading */
.loading {
  text-align: center;
  padding: 60px;
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
}

.loading p {
  color: var(--medium-gray);
  margin-top: 15px;
}

/* No Products */
.no-products {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
}

.no-products i {
  font-size: 60px;
  color: var(--medium-gray);
  margin-bottom: 20px;
}

.no-products h3 {
  color: var(--dark-gray);
  margin-bottom: 10px;
}

.no-products p {
  color: var(--medium-gray);
  margin-bottom: 20px;
}

/* Product Container */
.product-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
  margin-bottom: 30px;
}

.product-container.list-view {
  grid-template-columns: 1fr;
}

/* Responsive */
@media (max-width: 992px) {
  .shop-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .product-container {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 20px;
  }

  .active-filters {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 480px) {
  .product-container {
    grid-template-columns: 1fr;
  }

  .product-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }

  .filter-tags {
    width: 100%;
  }
}
</style>