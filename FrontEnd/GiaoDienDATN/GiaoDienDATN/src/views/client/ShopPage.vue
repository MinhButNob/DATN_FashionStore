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
          <!-- Sidebar Filters - BÊN TRÁI -->
          <aside class="sidebar">
            <div class="filter-widget">
              <h3><i class="fas fa-tshirt"></i> Danh mục sản phẩm</h3>
              <ul class="category-tree">
                <!-- Gắn thêm 2 mục giống menu ở ClientHeader: Sản phẩm (tất cả) và Khuyến mãi -->
                <li class="category-node">
                  <div class="category-row">
                    <a
                      href="#"
                      class="category-link"
                      :class="{ active: selectedCategoryIds.length === 0 }"
                      @click.prevent="selectAllProducts"
                    >
                      Sản phẩm
                    </a>
                  </div>
                </li>
                <li class="category-node" v-if="promoCategoryId">
                  <div class="category-row">
                    <a
                      href="#"
                      class="category-link"
                      :class="{ active: selectedCategoryIds.includes(promoCategoryId) }"
                      @click.prevent="selectCategory(promoCategoryId)"
                    >
                      Khuyến mãi
                    </a>
                  </div>
                </li>
                <li v-for="cat in categories" :key="cat.categoryId" class="category-node">
                  <div class="category-row">
                    <a
                      href="#"
                      class="category-link"
                      :class="{ active: selectedCategoryIds.includes(cat.categoryId) }"
                      @click.prevent="selectCategory(cat.categoryId)"
                    >
                      {{ cat.categoryName }}
                    </a>
                    <button
                      v-if="cat.children && cat.children.length"
                      type="button"
                      class="expand-btn"
                      @click="toggleExpand(cat.categoryId)"
                    >
                      {{ expandedCategoryIds.includes(cat.categoryId) ? '−' : '+' }}
                    </button>
                  </div>
                  <ul
                    v-if="cat.children && cat.children.length && expandedCategoryIds.includes(cat.categoryId)"
                    class="category-children"
                  >
                    <li v-for="child in cat.children" :key="child.categoryId">
                      <a
                        href="#"
                        class="category-link child"
                        :class="{ active: selectedCategoryIds.includes(child.categoryId) }"
                        @click.prevent="selectCategory(child.categoryId)"
                      >
                        {{ child.categoryName }}
                      </a>
                    </li>
                  </ul>
                </li>
              </ul>
            </div>

            <div class="filter-widget">
              <h3>💰 Khoảng giá</h3>
              <div class="price-range">
                <input 
                  type="range" 
                  min="0" 
                  max="3000000" 
                  step="50000"
                  v-model="priceRange"
                  @input="applyFilters"
                >
                <div class="price-values">
                  <span>0₫</span>
                  <span class="price-current">Đến {{ formatPriceVND(priceRange) }}</span>
                </div>
              </div>
            </div>

            <!-- ✅ BỘ LỌC KÍCH THƯỚC - dạng nút chọn, không checkbox -->
            <div class="filter-widget">
              <h3>📏 Kích thước</h3>
              <ul class="size-chip-list">
                <li v-for="size in allSizes" :key="size.sizeId">
                  <button
                    type="button"
                    class="size-chip"
                    :class="{ active: selectedSizeIds.includes(size.sizeId) }"
                    @click="toggleSize(size.sizeId)"
                  >
                    {{ size.sizeName }}
                    <span class="count" v-if="size.productCount > 0">({{ size.productCount }})</span>
                  </button>
                </li>
              </ul>
            </div>

            <button class="clear-filters" @click="clearFilters">
              🗑️ Xóa tất cả bộ lọc
            </button>
          </aside>

          <!-- Main Content - BÊN PHẢI -->
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
              <button class="clear-all-btn" @click="clearFilters">Xóa tất cả</button>
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
                  Hiển thị {{ filteredProducts.length }} sản phẩm
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
                <div class="view-options">
                  <button 
                    :class="{ active: gridView }"
                    @click="gridView = true"
                    title="Lưới"
                  >
                    <i class="fas fa-th"></i>
                  </button>
                  <button 
                    :class="{ active: !gridView }"
                    @click="gridView = false"
                    title="Danh sách"
                  >
                    <i class="fas fa-list"></i>
                  </button>
                </div>
              </div>

              <!-- Product Grid -->
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
                    <div v-if="product.discountPercent > 0" class="discount-flag">
                      -{{ product.discountPercent }}%
                    </div>
                  </div>
                  <div class="product-info-direct">
                    <div class="product-category-direct">{{ product.categoryName || 'Sản phẩm' }}</div>
                    <h3 class="product-name-direct">{{ product.productName }}</h3>
                    <div class="product-price-direct">
                      <template v-if="product.discountedPrice && product.discountedPrice < product.displayPrice">
                        <span class="original-price-direct">{{ formatPriceVND(product.displayPrice) }}</span>
                        <span class="current-price-direct">{{ formatPriceVND(product.discountedPrice) }}</span>
                      </template>
                      <template v-else>
                        <span class="current-price-direct">{{ formatPriceVND(product.displayPrice) }}</span>
                      </template>
                    </div>
                    <div class="product-colors-direct" v-if="product.colorOptions && product.colorOptions.length > 0">
                      <span 
                        v-for="color in product.colorOptions.slice(0, 3)" 
                        :key="color"
                        class="color-dot-direct"
                        :style="{ backgroundColor: getColorHex(color) }"
                        :title="color"
                      ></span>
                      <span v-if="product.colorOptions.length > 3" class="more-colors-direct">
                        +{{ product.colorOptions.length - 3 }}
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
const expandedCategoryIds = ref([])
const allSizes = ref([])
const gridView = ref(true)
const currentPage = ref(1)
const itemsPerPage = 12
const priceRange = ref(3000000)
const sortBy = ref('newest')

// Filter states
const selectedCategoryIds = ref([])
const selectedColorIds = ref([])
const selectedSizeIds = ref([])

// Computed
// Tìm 1 danh mục (cha hoặc con) theo ID trong cây categories
const findCategoryById = (id) => {
  for (const cat of categories.value) {
    if (cat.categoryId === id) return cat
    const child = (cat.children || []).find(c => c.categoryId === id)
    if (child) return child
  }
  return null
}

const selectedCategoryName = computed(() => {
  if (selectedCategoryIds.value.length === 1) {
    const cat = findCategoryById(selectedCategoryIds.value[0])
    return cat?.categoryName || promoCategoryName.value || 'Cửa hàng'
  }
  return 'Cửa hàng'
})

const formatPriceVND = (price) => {
  if (price === undefined || price === null) return '0₫'
  const numPrice = typeof price === 'number' ? price : Number(price)
  if (isNaN(numPrice) || numPrice === 0) return '0₫'
  return numPrice.toLocaleString('vi-VN') + '₫'
}

const calculateDiscountPercent = (originalPrice, discountedPrice) => {
  if (!originalPrice || !discountedPrice || originalPrice <= discountedPrice) return 0
  const discount = ((originalPrice - discountedPrice) / originalPrice) * 100
  return Math.round(discount)
}

const getColorHex = (colorName) => {
  const colorMap = {
    'Đỏ': '#FF0000',
    'Đen': '#000000',
    'Trắng': '#FFFFFF',
    'Xanh': '#0000FF',
    'Navy': '#1B2951',
    'Vàng': '#FFFF00',
    'Hồng': '#FFC0CB',
    'Xám': '#808080',
    'Nâu': '#8B4513',
    'Tím': '#800080',
    'Cam': '#FFA500'
  }
  return colorMap[colorName] || '#CCCCCC'
}

const handleImageError = (event) => {
  event.target.style.display = 'none'
  const placeholder = event.target.parentElement.querySelector('.no-image-placeholder')
  if (placeholder) placeholder.style.display = 'flex'
}

// Lấy danh sách màu sắc từ sản phẩm
const availableColors = computed(() => {
  const colorMap = new Map()
  allProducts.value.forEach(product => {
    if (product.colorOptions && product.colorOptions.length > 0) {
      product.colorOptions.forEach(colorName => {
        if (!colorMap.has(colorName)) {
          colorMap.set(colorName, {
            colorId: colorName,
            colorName: colorName,
            hexCode: getColorHex(colorName)
          })
        }
      })
    }
  })
  return Array.from(colorMap.values())
})

// ✅ FILTER PRODUCTS - ĐÃ SỬA LỌC SIZE
const filteredProducts = computed(() => {
  let filtered = [...allProducts.value]

  console.log('🔍 Bắt đầu lọc, tổng số sản phẩm:', filtered.length)
  console.log('  - Category selected:', selectedCategoryIds.value)
  console.log('  - Color selected:', selectedColorIds.value)
  console.log('  - Size selected:', selectedSizeIds.value)
  console.log('  - Price range:', priceRange.value)

  // Lọc theo danh mục
  if (selectedCategoryIds.value.length > 0) {
    // Nếu chọn 1 danh mục lớn (cha), lọc luôn sản phẩm thuộc các danh mục con của nó
    const effectiveIds = new Set(selectedCategoryIds.value)
    selectedCategoryIds.value.forEach(id => {
      const parent = categories.value.find(cat => cat.categoryId === id)
      if (parent && parent.children) {
        parent.children.forEach(child => effectiveIds.add(child.categoryId))
      }
    })
    filtered = filtered.filter(p => 
      effectiveIds.has(p.categoryId)
    )
    console.log('  - Sau lọc danh mục:', filtered.length)
  }

  // Lọc theo giá
  filtered = filtered.filter(p => {
    const priceToCompare = p.discountedPrice && p.discountedPrice < p.displayPrice ? p.discountedPrice : p.displayPrice
    return (priceToCompare || 0) <= priceRange.value
  })
  console.log('  - Sau lọc giá:', filtered.length)

  // Lọc theo màu sắc
  if (selectedColorIds.value.length > 0) {
    filtered = filtered.filter(p => {
      if (!p.colorOptions || p.colorOptions.length === 0) return false
      return p.colorOptions.some(color => selectedColorIds.value.includes(color))
    })
    console.log('  - Sau lọc màu:', filtered.length)
  }

  // ✅ Lọc theo kích thước - QUAN TRỌNG
  if (selectedSizeIds.value.length > 0) {
    filtered = filtered.filter(p => {
      // Kiểm tra sản phẩm có sizeIds không
      if (!p.sizeIds || p.sizeIds.length === 0) {
        console.log(`  - Sản phẩm ${p.productName} không có sizeIds`)
        return false
      }
      // Kiểm tra có ít nhất 1 size được chọn nằm trong sizeIds của sản phẩm
      const hasSize = selectedSizeIds.value.some(sizeId => p.sizeIds.includes(sizeId))
      if (!hasSize) {
        console.log(`  - Sản phẩm ${p.productName} không có size được chọn`)
      }
      return hasSize
    })
    console.log('  - Sau lọc size:', filtered.length)
  }

  // Sort
  filtered = [...filtered].sort((a, b) => {
    const priceA = a.discountedPrice && a.discountedPrice < a.displayPrice ? a.discountedPrice : a.displayPrice
    const priceB = b.discountedPrice && b.discountedPrice < b.displayPrice ? b.discountedPrice : b.displayPrice
    
    switch (sortBy.value) {
      case 'price_asc':
        return (priceA || 0) - (priceB || 0)
      case 'price_desc':
        return (priceB || 0) - (priceA || 0)
      case 'name_asc':
        return (a.productName || '').localeCompare(b.productName || '')
      case 'newest':
      default:
        return (b.productId || 0) - (a.productId || 0)
    }
  })

  console.log('🔍 Kết quả cuối:', filtered.length)
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
         priceRange.value < 3000000
})

const activeFilters = computed(() => {
  const filters = []

  selectedCategoryIds.value.forEach(catId => {
    const category = findCategoryById(catId)
    if (category) filters.push({ key: `cat_${catId}`, name: category.categoryName, type: 'category', value: catId })
  })

  selectedColorIds.value.forEach(colorName => {
    filters.push({ key: `color_${colorName}`, name: colorName, type: 'color', value: colorName })
  })

  selectedSizeIds.value.forEach(sizeId => {
    const size = allSizes.value.find(s => s.sizeId === sizeId)
    if (size) filters.push({ key: `size_${sizeId}`, name: size.sizeName, type: 'size', value: sizeId })
  })
  
  if (priceRange.value < 3000000) {
    filters.push({ key: 'price', name: `Dưới ${formatPriceVND(priceRange.value)}`, type: 'price' })
  }

  return filters
})

// Methods
const goToProductDetail = (productId) => {
  router.push(`/product/${productId}`)
}

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

const applyFilters = () => {
  currentPage.value = 1
  if (selectedCategoryIds.value.length === 1) {
    router.replace({ query: { ...route.query, categoryId: selectedCategoryIds.value[0] } })
  } else if (selectedCategoryIds.value.length === 0 && route.query.categoryId) {
    const { categoryId, ...rest } = route.query
    router.replace({ query: rest })
  }
}

// Bấm dấu +/− để mở/đóng danh sách danh mục con của 1 trong 3 danh mục lớn
const toggleExpand = (categoryId) => {
  const idx = expandedCategoryIds.value.indexOf(categoryId)
  if (idx === -1) {
    expandedCategoryIds.value.push(categoryId)
  } else {
    expandedCategoryIds.value.splice(idx, 1)
  }
}

// Bấm "Sản phẩm" trong sidebar - hiển thị tất cả sản phẩm cửa hàng (bỏ mọi lọc danh mục)
const selectAllProducts = () => {
  selectedCategoryIds.value = []
  applyFilters()
}

// Bấm vào tên danh mục (cha hoặc con) để lọc - bấm lại lần nữa để bỏ lọc (không dùng checkbox)
const selectCategory = (categoryId) => {
  if (selectedCategoryIds.value.length === 1 && selectedCategoryIds.value[0] === categoryId) {
    selectedCategoryIds.value = []
  } else {
    selectedCategoryIds.value = [categoryId]
  }
  applyFilters()
}

// Bấm vào 1 kích thước để bật/tắt lọc (không dùng checkbox), vẫn chọn được nhiều size
const toggleSize = (sizeId) => {
  const idx = selectedSizeIds.value.indexOf(sizeId)
  if (idx === -1) {
    selectedSizeIds.value.push(sizeId)
  } else {
    selectedSizeIds.value.splice(idx, 1)
  }
  applyFilters()
}

const removeFilter = (filter) => {
  switch (filter.type) {
    case 'category':
      selectedCategoryIds.value = selectedCategoryIds.value.filter(id => id !== filter.value)
      break
    case 'color':
      selectedColorIds.value = selectedColorIds.value.filter(color => color !== filter.value)
      break
    case 'size':
      selectedSizeIds.value = selectedSizeIds.value.filter(id => id !== filter.value)
      break
    case 'price':
      priceRange.value = 3000000
      break
  }
  applyFilters()
}

const clearFilters = () => {
  selectedCategoryIds.value = []
  selectedColorIds.value = []
  selectedSizeIds.value = []
  priceRange.value = 3000000
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

// Fetch danh sách kích thước từ API
const fetchSizes = async () => {
  try {
    const response = await axios.get(`${API_BASE}/sizes`, {
      params: { page: 0, size: 100 }
    })
    
    let sizes = []
    if (response.data?.data?.content) {
      sizes = response.data.data.content
    } else if (response.data?.content) {
      sizes = response.data.content
    } else if (Array.isArray(response.data)) {
      sizes = response.data
    }
    
    // Chỉ lấy size đang ACTIVE và sắp xếp
    allSizes.value = sizes
      .filter(s => s.status === 'ACTIVE')
      .sort((a, b) => {
        // Sắp xếp size theo thứ tự: S, M, L, XL, XXL
        const order = { 'S': 1, 'M': 2, 'L': 3, 'XL': 4, 'XXL': 5 }
        return (order[a.sizeName] || 99) - (order[b.sizeName] || 99)
      })
    console.log('✅ Sizes loaded:', allSizes.value)
  } catch (error) {
    console.error('Lỗi tải kích thước:', error)
    allSizes.value = []
  }
}

// Fetch products
const fetchProducts = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${API_BASE}/customer/home/latest-products`, {
      params: { page: 0, size: 100 }
    })
    
    let products = response.data?.products || []
    console.log('📦 Products from API:', products.length)
    
    // Xử lý sản phẩm - tạo sizeIds từ variants
    allProducts.value = products.map(p => {
      // Lấy danh sách sizeId từ variants
      const sizeIds = []
      const colorOptions = []
      
      if (p.variants && p.variants.length > 0) {
        p.variants.forEach(v => {
          // Lấy sizeId
          if (v.sizeId && !sizeIds.includes(v.sizeId)) {
            sizeIds.push(v.sizeId)
          }
          // Lấy màu sắc
          if (v.colorName && !colorOptions.includes(v.colorName)) {
            colorOptions.push(v.colorName)
          }
        })
      }
      
      const discountPercent = calculateDiscountPercent(p.minPrice, p.discountedPrice)
      
      return {
        productId: p.productId,
        productName: p.productName,
        categoryName: p.categoryName,
        categoryId: p.categoryId,
        displayPrice: Number(p.minPrice) || 0,
        discountedPrice: p.discountedPrice ? Number(p.discountedPrice) : null,
        discountPercent: discountPercent,
        maxPrice: Number(p.maxPrice) || 0,
        totalStock: Number(p.totalStock) || 0,
        mainImageUrl: p.mainImageUrl,
        colorOptions: colorOptions,
        sizeIds: sizeIds,
        variants: p.variants || []
      }
    })
    
    console.log('✅ Products processed:', allProducts.value.length)
    allProducts.value.forEach(p => {
      console.log(`  - ${p.productName}: sizes = [${p.sizeIds.join(', ')}]`)
    })
    
    // Cập nhật số lượng sản phẩm cho từng size
    updateSizeProductCounts()
    
    await fetchCategories()
    
  } catch (error) {
    console.error('Lỗi tải sản phẩm:', error)
    allProducts.value = []
  } finally {
    loading.value = false
  }
}

// Cập nhật số lượng sản phẩm cho từng size
const updateSizeProductCounts = () => {
  const countMap = {}
  
  allProducts.value.forEach(product => {
    if (product.sizeIds && product.sizeIds.length > 0) {
      product.sizeIds.forEach(sizeId => {
        countMap[sizeId] = (countMap[sizeId] || 0) + 1
      })
    }
  })
  
  allSizes.value = allSizes.value.map(size => ({
    ...size,
    productCount: countMap[size.sizeId] || 0
  }))
  
  console.log('📊 Size counts:', countMap)
}

// Tên 3 danh mục lớn mặc định - khớp với logic ở categoryHierarchyStores.js / các trang khác
const DEFAULT_LOCKED_NAMES = ["Áo", "Quần", "Phụ kiện"]

// Danh sách phẳng TẤT CẢ danh mục (kể cả "Khuyến Mãi", "nbvcx"...) - chỉ dùng để
// tra cứu categoryId theo tên (vd: link "Khuyến mãi" ở menu), KHÔNG dùng để hiển thị sidebar
const flatCategoriesForLookup = ref([])

// categoryId của danh mục "Khuyến Mãi" (nếu có) - dùng cho mục "Khuyến mãi" trong sidebar,
// giống hệt link "Khuyến mãi" ở menu ClientHeader
const promoCategoryId = computed(() => {
  const found = flatCategoriesForLookup.value.find(
    c => (c.categoryName || '').trim().toLowerCase() === 'khuyến mãi'
  )
  return found ? found.categoryId : null
})

const fetchCategories = async () => {
  try {
    // Lấy cây danh mục (chỉ ACTIVE) từ module Phân cấp danh mục, sau đó chỉ giữ lại
    // đúng 3 danh mục lớn mặc định (Áo/Quần/Phụ kiện) để hiển thị ở sidebar - không
    // hiện các danh mục khác (vd "Khuyến Mãi") lẫn vào như 1 danh mục chính thứ 4.
    const response = await axios.get(`${API_BASE}/category-hierarchy/tree/active`)
    const tree = Array.isArray(response.data) ? response.data : []

    const productCountByCategory = {}
    allProducts.value.forEach(product => {
      const catId = product.categoryId
      if (catId) {
        productCountByCategory[catId] = (productCountByCategory[catId] || 0) + 1
      }
    })

    categories.value = tree
      .filter(cat => DEFAULT_LOCKED_NAMES.includes(cat.categoryName))
      .map(cat => ({
        ...cat,
        productCount: productCountByCategory[cat.categoryId] || 0,
        children: (cat.children || []).map(child => ({
          ...child,
          productCount: productCountByCategory[child.categoryId] || 0
        }))
      }))

    // Lấy thêm danh sách phẳng TẤT CẢ danh mục (kể cả Khuyến Mãi) để tra cứu theo tên
    try {
      const flatRes = await axios.get(`${API_BASE}/categories/active`)
      flatCategoriesForLookup.value = Array.isArray(flatRes.data) ? flatRes.data : []
    } catch (e) {
      console.error('Lỗi tải danh sách danh mục (tra cứu tên):', e)
    }
  } catch (error) {
    console.error('Lỗi tải danh mục:', error)
    categories.value = []
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

// Link kiểu "/shop?categoryName=Khuyến Mãi" (vd menu "Khuyến mãi") - tra theo TÊN danh mục
// vì danh mục đó không nằm trong cây 3 danh mục chính hiển thị ở sidebar.
const promoCategoryName = ref(null)
watch(
  [() => route.query.categoryName, flatCategoriesForLookup],
  ([name, list]) => {
    if (!name) {
      promoCategoryName.value = null
      return
    }
    const found = (list || []).find(
      c => (c.categoryName || '').trim().toLowerCase() === String(name).trim().toLowerCase()
    )
    if (found) {
      promoCategoryName.value = found.categoryName
      if (!selectedCategoryIds.value.includes(found.categoryId)) {
        selectedCategoryIds.value = [found.categoryId]
        currentPage.value = 1
      }
    }
  },
  { immediate: true }
)

// Khi 1 danh mục con được chọn (qua URL hoặc bấm tay), tự mở rộng danh mục lớn chứa nó
watch([selectedCategoryIds, categories], () => {
  if (selectedCategoryIds.value.length !== 1) return
  const id = selectedCategoryIds.value[0]
  const parent = categories.value.find(cat => (cat.children || []).some(c => c.categoryId === id))
  if (parent && !expandedCategoryIds.value.includes(parent.categoryId)) {
    expandedCategoryIds.value.push(parent.categoryId)
  }
})

// Lifecycle
onMounted(async () => {
  await fetchSizes()
  await fetchProducts()
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
}

.breadcrumb a:hover {
  color: #e63946;
}

/* Layout 2 cột: Sidebar trái, Content phải */
.shop-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 30px;
  margin-top: 20px;
}

/* Sidebar - BÊN TRÁI */
.sidebar {
  background: white;
  border-radius: 15px;
  padding: 25px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  height: fit-content;
  position: sticky;
  top: 160px;
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
  font-weight: 600;
}

.filter-widget ul {
  list-style: none;
  padding-left: 0;
  margin: 0;
}

.filter-widget ul li {
  margin-bottom: 12px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #555;
  font-size: 14px;
}

.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #e63946;
}

.checkbox-label .count {
  color: #999;
  font-size: 12px;
}

/* Cây danh mục (giống Torano): dấu +/- để mở/đóng danh mục con */
.category-tree {
  list-style: none;
  margin: 0;
  padding: 0;
}

.category-node {
  margin-bottom: 8px;
}

.category-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.expand-btn {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 15px;
  line-height: 1;
  color: #555;
  display: flex;
  align-items: center;
  justify-content: center;
}

.expand-btn:hover {
  border-color: #e63946;
  color: #e63946;
}

.category-link {
  flex: 1;
  color: #333;
  font-size: 17px;
  text-decoration: none;
}

.category-link:hover {
  color: #e63946;
}

.category-link.active {
  color: #e63946;
  font-weight: 600;
}

.category-children {
  list-style: none;
  margin: 8px 0 0 12px;
  padding: 0;
}

.category-children li {
  margin-bottom: 10px;
}

.category-children .category-link {
  font-size: 15px;
}

/* Kích thước dạng nút chọn - không checkbox */
.size-chip-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.size-chip {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  color: #555;
  font-size: 13px;
  cursor: pointer;
}

.size-chip:hover {
  border-color: #e63946;
  color: #e63946;
}

.size-chip.active {
  border-color: #e63946;
  background: #e63946;
  color: #fff;
}

.size-chip .count {
  opacity: 0.8;
  font-size: 11px;
}

.color-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid #ddd;
  display: inline-block;
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

.price-current {
  font-weight: 600;
  color: #e63946;
}

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

/* Main Content - BÊN PHẢI */
.main-content {
  min-width: 0;
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

.clear-all-btn {
  padding: 5px 12px;
  background: none;
  border: 1px solid #ddd;
  border-radius: 20px;
  cursor: pointer;
  font-size: 12px;
}

.clear-all-btn:hover {
  background: #e63946;
  color: white;
  border-color: #e63946;
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

/* Product Card */
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

.discount-flag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #e63946;
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
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
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.original-price-direct {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.current-price-direct {
  font-size: 18px;
  font-weight: 700;
  color: #e63946;
}

.product-colors-direct {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.color-dot-direct {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid #ddd;
  cursor: pointer;
  transition: transform 0.2s;
}

.color-dot-direct:hover {
  transform: scale(1.1);
}

.more-colors-direct {
  font-size: 12px;
  color: #999;
  line-height: 22px;
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
  
  .sidebar {
    position: static;
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
  
  .product-name-direct {
    font-size: 14px;
    height: 38px;
  }
  
  .current-price-direct {
    font-size: 16px;
  }
}
</style>