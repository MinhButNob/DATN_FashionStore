<template>
  <div class="admin-products">
    <div class="admin-toolbar">
      <h1 class="page-title">Quản lý sản phẩm</h1>
      
      <button class="btn btn-primary" @click="openProductModal()">
        <i class="fas fa-plus"></i> Thêm sản phẩm
      </button>
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="filters.search"
          placeholder="Tìm kiếm sản phẩm..."
          @input="applyFilters"
        >
      </div>

      <select v-model="filters.category" @change="applyFilters">
        <option value="all">Tất cả danh mục</option>
        <option value="men">Thời trang nam</option>
        <option value="women">Thời trang nữ</option>
        <option value="kids">Trẻ em</option>
        <option value="accessories">Phụ kiện</option>
      </select>

      <select v-model="filters.status" @change="applyFilters">
        <option value="all">Tất cả trạng thái</option>
        <option value="active">Đang bán</option>
        <option value="inactive">Ngừng bán</option>
      </select>

      <select v-model="filters.tag" @change="applyFilters">
        <option value="all">Tất cả tags</option>
        <option value="Hot">Hot</option>
        <option value="Sale">Sale</option>
        <option value="Mới">Mới</option>
      </select>
    </div>

    <!-- Bulk Actions -->
    <Transition>
      <div v-if="selectedProducts.length > 0" class="bulk-actions">
        <span>Đã chọn <strong>{{ selectedProducts.length }}</strong> sản phẩm</span>
        
        <select v-model="bulkAction">
          <option value="">Chọn thao tác</option>
          <option value="delete">Xóa</option>
          <option value="active">Kích hoạt</option>
          <option value="inactive">Vô hiệu hóa</option>
        </select>

        <button class="btn btn-primary" @click="executeBulkAction">
          Áp dụng
        </button>

        <button class="btn btn-outline" @click="clearSelection">
          Bỏ chọn
        </button>
      </div>
    </Transition>

    <!-- Products Table -->
    <DataTable
      :columns="columns"
      :data="paginatedProducts"
      :selectable="true"
      v-model:selected="selectedProducts"
    >
      <template #cell-image="{ row }">
        <div class="product-image">
          <img v-if="isUrl(row.image)" :src="row.image" :alt="row.name" class="thumbnail-img-fit" />
          <i v-else :class="['fas', `fa-${row.image}`]"></i>
        </div>
      </template>

      <template #cell-price="{ row }">
        {{ formatPrice(row.price) }}
      </template>

      <template #cell-oldPrice="{ row }">
        <span v-if="row.oldPrice">{{ formatPrice(row.oldPrice) }}</span>
        <span v-else>-</span>
      </template>

      <template #cell-rating="{ row }">
        <div class="rating">
          {{ row.rating }} <i class="fas fa-star" style="color: #ffc107;"></i>
        </div>
      </template>

      <template #cell-tag="{ row }">
        <span v-if="row.tag" :class="['badge', row.tag === 'Hot' ? 'badge-success' : 'badge-warning']">
          {{ row.tag }}
        </span>
        <span v-else>-</span>
      </template>

      <template #cell-status="{ row }">
        <label class="status-toggle">
          <input
            type="checkbox"
            :checked="row.status === 'active'"
            @change="toggleProductStatus(row.id)"
          >
          <span class="slider"></span>
        </label>
      </template>

      <template #cell-actions="{ row }">
        <div class="action-buttons">
          <button class="action-btn view-btn" @click="viewProduct(row.id)">
            <i class="fas fa-eye"></i>
          </button>
          <button class="action-btn edit-btn" @click="openProductModal(row)">
            <i class="fas fa-edit"></i>
          </button>
          <button class="action-btn delete-btn" @click="confirmDelete(row.id)">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      </template>
    </DataTable>

    <!-- Pagination -->
    <div class="pagination">
      <button
        class="page-btn"
        :disabled="currentPage === 1"
        @click="currentPage--"
      >
        <i class="fas fa-chevron-left"></i>
      </button>

      <button
        v-for="page in displayedPages"
        :key="page"
        class="page-btn"
        :class="{ active: currentPage === page }"
        @click="currentPage = page"
      >
        {{ page }}
      </button>

      <button
        class="page-btn"
        :disabled="currentPage === totalPages"
        @click="currentPage++"
      >
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- Product Modal -->
    <ProductModal
      v-if="showModal"
      :product="editingProduct"
      @close="closeProductModal"
      @save="saveProduct"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useProductStore } from '../../stores/productStore'
import { useNotification } from '../../composables/useNotification'
import { formatPrice } from '../../utils/helpers'
import DataTable from '../../components/admin/DataTable.vue'
import ProductModal from '../../components/admin/ProductModal.vue'

const productStore = useProductStore()
const { showNotification } = useNotification()

// State
const filters = ref({
  search: '',
  category: 'all',
  status: 'all',
  tag: 'all'
})

const currentPage = ref(1)
const itemsPerPage = 10
const selectedProducts = ref([])
const bulkAction = ref('')
const showModal = ref(false)
const editingProduct = ref(null)

// Table columns
const columns = [
  { key: 'id', label: 'ID' },
  { key: 'image', label: 'Hình ảnh' },
  { key: 'name', label: 'Tên sản phẩm' },
  { key: 'category', label: 'Danh mục', format: (value) => getCategoryName(value) },
  { key: 'price', label: 'Giá' },
  { key: 'oldPrice', label: 'Giá cũ' },
  { key: 'stock', label: 'Kho' },
  { key: 'sold', label: 'Đã bán' },
  { key: 'rating', label: 'Đánh giá' },
  { key: 'tag', label: 'Tag' },
  { key: 'status', label: 'Trạng thái' },
  { key: 'actions', label: 'Thao tác' }
]

// Computed
const filteredProducts = computed(() => {
  let filtered = [...productStore.products]

  // Search
  if (filters.value.search) {
    const searchTerm = filters.value.search.toLowerCase()
    filtered = filtered.filter(p =>
      p.name.toLowerCase().includes(searchTerm) ||
      p.id.toString().includes(searchTerm)
    )
  }

  // Category filter
  if (filters.value.category !== 'all') {
    filtered = filtered.filter(p => p.category === filters.value.category)
  }

  // Status filter
  if (filters.value.status !== 'all') {
    filtered = filtered.filter(p => p.status === filters.value.status)
  }

  // Tag filter
  if (filters.value.tag !== 'all') {
    filtered = filtered.filter(p => p.tag === filters.value.tag)
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

// Methods
const applyFilters = () => {
  currentPage.value = 1
}

const clearSelection = () => {
  selectedProducts.value = []
  bulkAction.value = ''
}

const executeBulkAction = () => {
  if (!bulkAction.value) return

  switch (bulkAction.value) {
    case 'delete':
      if (confirm(`Bạn có chắc muốn xóa ${selectedProducts.value.length} sản phẩm?`)) {
        selectedProducts.value.forEach(id => {
          productStore.deleteProduct(id)
        })
        showNotification(`Đã xóa ${selectedProducts.value.length} sản phẩm`, 'success')
      }
      break

    case 'active':
      selectedProducts.value.forEach(id => {
        const product = productStore.products.find(p => p.id === id)
        if (product) product.status = 'active'
      })
      showNotification(`Đã kích hoạt ${selectedProducts.value.length} sản phẩm`, 'success')
      break

    case 'inactive':
      selectedProducts.value.forEach(id => {
        const product = productStore.products.find(p => p.id === id)
        if (product) product.status = 'inactive'
      })
      showNotification(`Đã vô hiệu hóa ${selectedProducts.value.length} sản phẩm`, 'success')
      break
  }

  clearSelection()
}

const toggleProductStatus = (id) => {
  productStore.toggleProductStatus(id)
  showNotification('Đã cập nhật trạng thái sản phẩm', 'success')
}

const openProductModal = (product = null) => {
  editingProduct.value = product
  showModal.value = true
}

const closeProductModal = () => {
  showModal.value = false
  editingProduct.value = null
}

const saveProduct = (productData) => {
  if (editingProduct.value) {
    productStore.updateProduct({ ...editingProduct.value, ...productData })
    showNotification('Cập nhật sản phẩm thành công!', 'success')
  } else {
    productStore.addProduct(productData)
    showNotification('Thêm sản phẩm thành công!', 'success')
  }
  closeProductModal()
}

const viewProduct = (id) => {
  // Navigate to product detail or show preview
}

const confirmDelete = (id) => {
  if (confirm('Bạn có chắc muốn xóa sản phẩm này?')) {
    productStore.deleteProduct(id)
    showNotification('Xóa sản phẩm thành công!', 'success')
  }
}

const isUrl = (str) => {
  if (!str) return false
  return str.startsWith('http') || str.startsWith('data:image')
}

// Watch for filter changes
watch(filters, () => {
  applyFilters()
}, { deep: true })
</script>

<style scoped>
.admin-products {
  animation: fadeIn 0.3s;
}

.page-title {
  font-size: 28px;
  color: var(--dark-gray);
}

.admin-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  flex-wrap: wrap;
  gap: 15px;
}

.filters-bar {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: var(--shadow);
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.search-box {
  flex: 1;
  min-width: 250px;
  display: flex;
  align-items: center;
  background: var(--light-gray);
  border-radius: 8px;
  padding: 0 15px;
}

.search-box i {
  color: var(--medium-gray);
}

.search-box input {
  border: none;
  background: none;
  padding: 12px;
  width: 100%;
  outline: none;
}

.filters-bar select {
  padding: 12px 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  min-width: 150px;
  cursor: pointer;
  background: white;
}

.filters-bar select:focus {
  border-color: var(--primary-red);
}

.bulk-actions {
  background: white;
  padding: 15px 20px;
  border-radius: 10px;
  box-shadow: var(--shadow);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
  animation: slideDown 0.3s;
}

.bulk-actions select {
  padding: 10px 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  min-width: 150px;
}

.product-image {
  width: 50px;
  height: 50px;
  background: var(--light-gray);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image i {
  font-size: 25px;
  color: var(--medium-gray);
}

.thumbnail-img-fit {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.rating {
  display: flex;
  align-items: center;
  gap: 5px;
}

.status-toggle {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.status-toggle input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #28a745;
}

input:checked + .slider:before {
  transform: translateX(26px);
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 35px;
  height: 35px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}

.view-btn {
  background: #17a2b8;
  color: white;
}

.view-btn:hover {
  background: #138496;
}

.edit-btn {
  background: #ffc107;
  color: var(--dark-gray);
}

.edit-btn:hover {
  background: #e0a800;
}

.delete-btn {
  background: #dc3545;
  color: white;
}

.delete-btn:hover {
  background: #c82333;
}

.v-enter-active,
.v-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 24px;
  }
  
  .filters-bar {
    flex-direction: column;
  }
  
  .filters-bar select {
    width: 100%;
  }
  
  .bulk-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .bulk-actions select {
    width: 100%;
  }
}
</style>