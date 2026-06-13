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

        <!-- Filter Bar -->
        <FilterBar />

        <div class="shop-layout">
          <!-- Sidebar Filters -->
          <aside class="sidebar">
            <div class="filter-widget">
              <h3>Danh mục</h3>
              <ul>
                <li v-for="category in categories" :key="category.categoryId">
                  <label>
                    <input 
                      type="checkbox" 
                      :value="category.categoryId"
                      v-model="selectedCategoryIds"
                      @change="applyFilters"
                    >
                    {{ category.categoryName }} 
                    <span class="category-count">({{ category.productCount || 0 }})</span>
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
                  max="10000000" 
                  step="50000"
                  v-model="priceRange"
                  @input="applyFilters"
                >
                <div class="price-values">
                  <span>0₫</span>
                  <span>{{ formatPrice(priceRange) }}</span>
                  <span>10.000.000₫</span>
                </div>
              </div>
            </div>

            <div class="filter-widget">
              <h3>Màu sắc</h3>
              <ul>
                <li v-for="color in colors" :key="color.colorId">
                  <label>
                    <input 
                      type="checkbox" 
                      :value="color.colorId"
                      v-model="selectedColorIds"
                      @change="applyFilters"
                    >
                    <span 
                      class="color-dot" 
                      :style="{ backgroundColor: color.hexCode || '#ccc' }"
                    ></span>
                    {{ color.colorName }}
                  </label>
                </li>
              </ul>
            </div>

            <div class="filter-widget">
              <h3>Kích thước</h3>
              <ul>
                <li v-for="size in sizes" :key="size.sizeId">
                  <label>
                    <input 
                      type="checkbox" 
                      :value="size.sizeId"
                      v-model="selectedSizeIds"
                      @change="applyFilters"
                    >
                    {{ size.sizeName }}
                  </label>
                </li>
              </ul>
            </div>

            <button class="clear-filters" @click="clearFilters">
              Xóa bộ lọc
            </button>
          </aside>

          <!-- Main Content -->
          <div class="main-content">
            <!-- Active Filters -->
            <div v-if="hasActiveFilters" class="active-filters">
              <span>Bộ lọc đang chọn:</span>
              <div class="filter-tags">
                <span 
                  v-for="filter in activeFilters" 
                  :key="filter.key"
                  class="filter-tag"
                >
                  {{ filter.name }}
                  <i class="fas fa-times" @click="removeFilter(filter)"></i>
                </span>
              </div>
            </div>

            <!-- Loading State -->
            <div v-if="loading" class="loading-state">
              <div class="spinner"></div>
              <p>Đang tải sản phẩm...</p>
            </div>

            <!-- No Products -->
            <div v-else-if="filteredProducts.length === 0" class="no-products">
              <i class="fas fa-box-open"></i>
              <h3>Không tìm thấy sản phẩm</h3>
              <p>Vui lòng thử lại với bộ lọc khác</p>
              <button class="btn-primary" @click="clearFilters">
                Xóa bộ lọc
              </button>
            </div>

            <!-- Product Grid -->
            <div v-else>
              <div class="product-header">
                <div class="product-count">
                  Hiển thị {{ paginatedProducts.length }} / {{ filteredProducts.length }} sản phẩm
                </div>
                <div class="sort-options">
                  <select v-model="sortBy" @change="applyFilters" class="sort-select">
                    <option value="newest">Mới nhất</option>
                    <option value="price_asc">Giá tăng dần</option>
                    <option value="price_desc">Giá giảm dần</option>
                    <option value="name_asc">Tên A-Z</option>
                  </select>
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
                <div 
                  v-for="product in paginatedProducts" 
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
                        {{ formatPriceVND(product.displayPrice) }}
                      </span>
                    </div>
                    <div class="product-stock-direct" :class="{ 'out-of-stock': (product.totalStock || 0) === 0 }">
                      <i class="fas" :class="(product.totalStock || 0) > 0 ? 'fa-check-circle' : 'fa-times-circle'"></i>
                      {{ (product.totalStock || 0) > 0 ? `Còn ${product.totalStock} sản phẩm` : 'Hết hàng' }}
                    </div>
                    <button class="add-to-cart-direct" @click.stop="addToCart(product)" :disabled="(product.totalStock || 0) === 0">
                      <i class="fas fa-shopping-cart"></i> Thêm vào giỏ
                    </button>
                  </div>
                </div>
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import FilterBar from '../../components/client/FilterBar.vue'
import CartModal from '../../components/client/CartModal.vue'
import { useCartStore } from '../../stores/cartStore'
import { useNotification } from '../../composables/useNotification'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const { showNotification } = useNotification()
const API_BASE = "http://localhost:8080/api"

// State
const loading = ref(false)
const allProducts = ref([])
const categories = ref([])
const colors = ref([])
const sizes = ref([])
const gridView = ref(true)
const currentPage = ref(1)
const itemsPerPage = 12
const priceRange = ref(10000000)
const sortBy = ref('newest')

// Filter states
const selectedCategoryIds = ref([])
const selectedColorIds = ref([])
const selectedSizeIds = ref([])

// Computed
const selectedCategoryName = computed(() => {
  if (selectedCategoryIds.value.length === 1) {
    const cat = categories.value.find(c => c.categoryId === selectedCategoryIds.value[0])
    return cat?.categoryName || 'Cửa hàng'
  }
  return 'Cửa hàng'
})

const formatPrice = (price) => {
  if (!price && price !== 0) return '0₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
}

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

// Lấy danh sách màu sắc thực tế có trong sản phẩm (để lọc chính xác)
const availableColors = computed(() => {
  const colorMap = new Map()
  allProducts.value.forEach(product => {
    if (product.variants && product.variants.length > 0) {
      product.variants.forEach(variant => {
        if (variant.colorId && variant.colorName) {
          if (!colorMap.has(variant.colorId)) {
            colorMap.set(variant.colorId, {
              colorId: variant.colorId,
              colorName: variant.colorName,
              hexCode: variant.hexCode
            })
          }
        }
      })
    }
  })
  return Array.from(colorMap.values())
})

// Lấy danh sách kích thước thực tế có trong sản phẩm
const availableSizes = computed(() => {
  const sizeMap = new Map()
  allProducts.value.forEach(product => {
    if (product.variants && product.variants.length > 0) {
      product.variants.forEach(variant => {
        if (variant.sizeId && variant.sizeName) {
          if (!sizeMap.has(variant.sizeId)) {
            sizeMap.set(variant.sizeId, {
              sizeId: variant.sizeId,
              sizeName: variant.sizeName
            })
          }
        }
      })
    }
  })
  return Array.from(sizeMap.values())
})

// Filter products
const filteredProducts = computed(() => {
  let filtered = [...allProducts.value]

  console.log('🟡 Lọc sản phẩm...')
  console.log('  - Tổng số SP:', filtered.length)
  console.log('  - Danh mục đã chọn:', selectedCategoryIds.value)
  console.log('  - Màu đã chọn:', selectedColorIds.value)
  console.log('  - Size đã chọn:', selectedSizeIds.value)
  console.log('  - Giá <=:', priceRange.value)

  // Lọc theo danh mục
  if (selectedCategoryIds.value.length > 0) {
    filtered = filtered.filter(p => 
      selectedCategoryIds.value.includes(p.categoryId)
    )
    console.log('  - Sau lọc danh mục:', filtered.length)
  }

  // Lọc theo giá
  filtered = filtered.filter(p => (p.displayPrice || 0) <= priceRange.value)
  console.log('  - Sau lọc giá:', filtered.length)

  // Lọc theo màu sắc
  if (selectedColorIds.value.length > 0) {
    filtered = filtered.filter(p => {
      if (!p.variants || p.variants.length === 0) return false
      // Kiểm tra xem sản phẩm có variant nào có màu được chọn không
      return p.variants.some(v => selectedColorIds.value.includes(v.colorId))
    })
    console.log('  - Sau lọc màu:', filtered.length)
  }

  // Lọc theo kích thước
  if (selectedSizeIds.value.length > 0) {
    filtered = filtered.filter(p => {
      if (!p.variants || p.variants.length === 0) return false
      // Kiểm tra xem sản phẩm có variant nào có size được chọn không
      return p.variants.some(v => selectedSizeIds.value.includes(v.sizeId))
    })
    console.log('  - Sau lọc size:', filtered.length)
  }

  // Sort
  filtered = [...filtered].sort((a, b) => {
    switch (sortBy.value) {
      case 'price_asc':
        return (a.displayPrice || 0) - (b.displayPrice || 0)
      case 'price_desc':
        return (b.displayPrice || 0) - (a.displayPrice || 0)
      case 'name_asc':
        return (a.productName || '').localeCompare(b.productName || '')
      case 'newest':
      default:
        return (b.productId || 0) - (a.productId || 0)
    }
  })

  console.log('  - Kết quả cuối:', filtered.length)
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
  return selectedCategoryIds.value.length > 0 ||
         selectedColorIds.value.length > 0 ||
         selectedSizeIds.value.length > 0 ||
         priceRange.value < 10000000
})

const activeFilters = computed(() => {
  const filters = []

  selectedCategoryIds.value.forEach(catId => {
    const category = categories.value.find(c => c.categoryId === catId)
    if (category) filters.push({ key: `cat_${catId}`, name: category.categoryName, type: 'category', value: catId })
  })

  selectedColorIds.value.forEach(colorId => {
    const color = colors.value.find(c => c.colorId === colorId)
    if (color) filters.push({ key: `color_${colorId}`, name: color.colorName, type: 'color', value: colorId })
  })

  selectedSizeIds.value.forEach(sizeId => {
    const size = sizes.value.find(s => s.sizeId === sizeId)
    if (size) filters.push({ key: `size_${sizeId}`, name: size.sizeName, type: 'size', value: sizeId })
  })
  
  if (priceRange.value < 10000000) {
    filters.push({ key: 'price', name: `Dưới ${formatPrice(priceRange.value)}`, type: 'price' })
  }

  return filters
})

// Methods
const goToProductDetail = (productId) => {
  router.push(`/product/${productId}`)
}

const addToCart = (product) => {
  const stock = product.totalStock || 0
  if (stock === 0) {
    showNotification('Sản phẩm đã hết hàng!', 'error')
    return
  }
  
  const price = product.displayPrice || 0
  cartStore.addToCart({
    productId: product.productId,
    productName: product.productName,
    price: price,
    quantity: 1,
    image: product.mainImageUrl
  })
  showNotification('Đã thêm sản phẩm vào giỏ hàng!', 'success')
}

const applyFilters = () => {
  currentPage.value = 1
  if (selectedCategoryIds.value.length === 1) {
    router.replace({ query: { ...route.query, categoryId: selectedCategoryIds.value[0] } })
  } else if (selectedCategoryIds.value.length === 0 && route.query.categoryId) {
    const { categoryId, ...rest } = route.query
    router.replace({ query: rest })
  }
}

const removeFilter = (filter) => {
  switch (filter.type) {
    case 'category':
      selectedCategoryIds.value = selectedCategoryIds.value.filter(id => id !== filter.value)
      break
    case 'color':
      selectedColorIds.value = selectedColorIds.value.filter(id => id !== filter.value)
      break
    case 'size':
      selectedSizeIds.value = selectedSizeIds.value.filter(id => id !== filter.value)
      break
    case 'price':
      priceRange.value = 10000000
      break
  }
  applyFilters()
}

const clearFilters = () => {
  selectedCategoryIds.value = []
  selectedColorIds.value = []
  selectedSizeIds.value = []
  priceRange.value = 10000000
  sortBy.value = 'newest'
  currentPage.value = 1
  showNotification('Đã xóa tất cả bộ lọc', 'success')
}

const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// Fetch data
const fetchProducts = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${API_BASE}/products`, {
      params: { page: 0, size: 100 }
    })
    
    let products = []
    if (response.data?.data?.content) {
      products = response.data.data.content
    } else if (response.data?.content) {
      products = response.data.content
    } else if (Array.isArray(response.data)) {
      products = response.data
    }
    
    allProducts.value = products.map(p => {
      let displayPrice = 0
      let totalStock = 0
      
      if (p.variants && p.variants.length > 0) {
        const firstVariant = p.variants[0]
        displayPrice = Number(firstVariant.price) || 0
        totalStock = p.variants.reduce((sum, v) => sum + (Number(v.quantity) || 0), 0)
      } else {
        displayPrice = Number(p.minPrice) || Number(p.price) || 0
        totalStock = Number(p.totalStock) || 0
      }
      
      let mainImageUrl = null
      if (p.mainImageUrl) {
        mainImageUrl = p.mainImageUrl
      } else if (p.images && p.images.length > 0) {
        mainImageUrl = p.images[0].imageUrl || p.images[0]
      }
      
      const categoryName = p.category?.categoryName || p.categoryName || ''
      const categoryId = p.categoryId || p.category?.categoryId || null
      
      return {
        productId: p.productId,
        productName: p.productName,
        categoryName: categoryName,
        categoryId: categoryId,
        displayPrice: displayPrice,
        originalPrice: Number(p.maxPrice) || displayPrice,
        totalStock: totalStock,
        mainImageUrl: mainImageUrl,
        variants: p.variants || []
      }
    })
    
    // Cập nhật categories với số lượng sản phẩm
    await fetchCategories()
    
  } catch (error) {
    console.error('Lỗi tải sản phẩm:', error)
    allProducts.value = []
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const response = await axios.get(`${API_BASE}/categories/active`)
    const categoriesData = response.data || []
    
    const productCountByCategory = {}
    allProducts.value.forEach(product => {
      const catId = product.categoryId
      if (catId) {
        productCountByCategory[catId] = (productCountByCategory[catId] || 0) + 1
      }
    })
    
    categories.value = categoriesData.map(cat => ({
      ...cat,
      productCount: productCountByCategory[cat.categoryId] || 0
    }))
  } catch (error) {
    console.error('Lỗi tải danh mục:', error)
    categories.value = []
  }
}

const fetchColors = async () => {
  try {
    const response = await axios.get(`${API_BASE}/colors`, { params: { page: 0, size: 100 } })
    let colorsData = []
    if (response.data?.data?.content) {
      colorsData = response.data.data.content
    } else if (response.data?.content) {
      colorsData = response.data.content
    } else if (Array.isArray(response.data)) {
      colorsData = response.data
    }
    colors.value = colorsData.filter(c => c.status === 'ACTIVE')
  } catch (error) {
    console.error('Lỗi tải màu:', error)
    colors.value = []
  }
}

const fetchSizes = async () => {
  try {
    const response = await axios.get(`${API_BASE}/sizes`, { params: { page: 0, size: 100 } })
    let sizesData = []
    if (response.data?.data?.content) {
      sizesData = response.data.data.content
    } else if (response.data?.content) {
      sizesData = response.data.content
    } else if (Array.isArray(response.data)) {
      sizesData = response.data
    }
    sizes.value = sizesData.filter(s => s.status === 'ACTIVE')
  } catch (error) {
    console.error('Lỗi tải size:', error)
    sizes.value = []
  }
}

// Watch URL categoryId
watch(() => route.query.categoryId, (newCategoryId) => {
  if (newCategoryId) {
    const id = Number(newCategoryId)
    if (!selectedCategoryIds.value.includes(id)) {
      selectedCategoryIds.value = [id]
      currentPage.value = 1
    }
  } else if (selectedCategoryIds.value.length > 0 && !route.query.categoryId) {
    selectedCategoryIds.value = []
  }
}, { immediate: true })

// Lifecycle
onMounted(async () => {
  await fetchProducts()
  await Promise.all([
    fetchColors(),
    fetchSizes()
  ])
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

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

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
  transition: color 0.3s;
}

.breadcrumb a:hover {
  color: #e63946;
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
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  height: fit-content;
}

.filter-widget {
  margin-bottom: 25px;
  padding-bottom: 25px;
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
  color: #666;
  font-size: 14px;
}

.category-count {
  color: #999;
  font-size: 12px;
}

.color-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid #ddd;
}

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

.clear-filters {
  width: 100%;
  padding: 10px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.clear-filters:hover {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

/* Product Card */
.product-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
  margin-bottom: 30px;
}

.product-container.list-view {
  grid-template-columns: 1fr;
}

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

/* Product Header */
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

.sort-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  outline: none;
  cursor: pointer;
}

.view-options {
  display: flex;
  gap: 5px;
}

.view-options button {
  width: 35px;
  height: 35px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.3s;
}

.view-options button:hover,
.view-options button.active {
  background: #e63946;
  color: white;
  border-color: #e63946;
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
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1;
}

.filter-tag {
  background: #f0f0f0;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.filter-tag i {
  cursor: pointer;
  font-size: 12px;
}

.filter-tag i:hover {
  color: #e63946;
}

/* Pagination */
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

/* Loading & Empty States */
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
  
  .product-image-direct {
    height: 220px;
  }
  
  .product-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>