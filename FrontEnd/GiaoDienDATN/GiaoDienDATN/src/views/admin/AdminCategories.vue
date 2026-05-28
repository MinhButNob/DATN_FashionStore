<template>
  <div class="admin-categories">
    <div class="admin-header">
      <h1 class="page-title">Quản lý danh mục</h1>
      <button class="btn btn-primary" @click="openCategoryModal()">
        <i class="fas fa-plus"></i> Thêm danh mục
      </button>
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="filters.search"
          placeholder="Tìm kiếm danh mục..."
          @input="applyFilters"
        >
      </div>

      <select v-model="filters.status" @change="applyFilters">
        <option value="all">Tất cả trạng thái</option>
        <option value="active">Hiển thị</option>
        <option value="inactive">Ẩn</option>
      </select>
    </div>

    <!-- Categories Table -->
    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Icon</th>
            <th>Tên danh mục</th>
            <th>Slug</th>
            <th>Số sản phẩm</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="category in paginatedCategories" :key="category.id">
            <td>#{{ category.id }}</td>
            <td>
              <div class="category-icon">
                <i :class="category.icon"></i>
              </div>
            </td>
            <td>
              <strong>{{ category.name }}</strong>
            </td>
            <td>{{ category.slug }}</td>
            <td>{{ category.productCount }} sản phẩm</td>
            <td>
              <label class="status-toggle">
                <input 
                  type="checkbox" 
                  :checked="category.status === 'active'"
                  @change="toggleCategoryStatus(category.id)"
                >
                <span class="slider"></span>
              </label>
            </td>
            <td>
              <div class="action-buttons">
                <button 
                  class="action-btn edit-btn" 
                  @click="openCategoryModal(category)"
                  title="Sửa"
                >
                  <i class="fas fa-edit"></i>
                </button>
                <button 
                  class="action-btn delete-btn" 
                  @click="confirmDelete(category.id)"
                  title="Xóa"
                  :disabled="category.productCount > 0"
                >
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredCategories.length === 0">
            <td colspan="7" class="no-data">
              Không tìm thấy danh mục nào
            </td>
          </tr>
        </tbody>
      </table>
    </div>

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

    <!-- Category Modal -->
    <div v-if="showModal" class="modal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ editingCategory ? 'Sửa danh mục' : 'Thêm danh mục mới' }}</h2>
          <button class="close-btn" @click="closeModal">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="modal-body">
          <form @submit.prevent="saveCategory">
            <div class="form-group">
              <label>Tên danh mục *</label>
              <input 
                type="text" 
                v-model="form.name" 
                required
                @input="generateSlug"
              >
            </div>

            <div class="form-group">
              <label>Slug *</label>
              <input 
                type="text" 
                v-model="form.slug" 
                required
                placeholder="ten-danh-muc"
              >
            </div>

            <div class="form-group">
              <label>Icon</label>
              <div class="icon-selector">
                <button 
                  v-for="icon in icons" 
                  :key="icon"
                  type="button"
                  class="icon-btn"
                  :class="{ active: form.icon === icon }"
                  @click="form.icon = icon"
                >
                  <i :class="icon"></i>
                </button>
              </div>
            </div>

            <div class="form-group">
              <label>Mô tả</label>
              <textarea 
                v-model="form.description" 
                rows="3"
                placeholder="Nhập mô tả cho danh mục"
              ></textarea>
            </div>

            <div class="form-group">
              <label>
                <input type="checkbox" v-model="form.status">
                Hiển thị danh mục
              </label>
            </div>

            <div class="form-actions">
              <button type="submit" class="btn btn-primary">
                {{ editingCategory ? 'Cập nhật' : 'Thêm mới' }}
              </button>
              <button type="button" class="btn btn-outline" @click="closeModal">
                Hủy
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="showDeleteModal" class="modal">
      <div class="modal-content modal-sm">
        <div class="modal-header">
          <h2>Xác nhận xóa</h2>
          <button class="close-btn" @click="closeDeleteModal">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="modal-body">
          <p>Bạn có chắc chắn muốn xóa danh mục này?</p>
          <p class="warning-text">Hành động này không thể hoàn tác!</p>
        </div>

        <div class="modal-footer">
          <button class="btn btn-danger" @click="deleteCategory">
            Xóa
          </button>
          <button class="btn btn-outline" @click="closeDeleteModal">
            Hủy
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useNotification } from '../../composables/useNotification'
import { useProductStore } from '../../stores/productStore'

const { showNotification } = useNotification()
const productStore = useProductStore()

// Mock data for categories
const categories = ref([
  { id: 1, name: 'Thời trang nam', slug: 'thoi-trang-nam', icon: 'fas fa-male', productCount: 45, status: 'active', description: 'Thời trang nam cao cấp' },
  { id: 2, name: 'Thời trang nữ', slug: 'thoi-trang-nu', icon: 'fas fa-female', productCount: 67, status: 'active', description: 'Thời trang nữ thời thượng' },
  { id: 3, name: 'Trẻ em', slug: 'tre-em', icon: 'fas fa-child', productCount: 23, status: 'active', description: 'Thời trang cho bé' },
  { id: 4, name: 'Phụ kiện', slug: 'phu-kien', icon: 'fas fa-bag-shopping', productCount: 34, status: 'active', description: 'Phụ kiện thời trang' },
  { id: 5, name: 'Giày dép', slug: 'giay-dep', icon: 'fas fa-shoe-prints', productCount: 28, status: 'inactive', description: 'Giày dép các loại' }
])

// State
const filters = ref({
  search: '',
  status: 'all'
})

const currentPage = ref(1)
const itemsPerPage = 10
const showModal = ref(false)
const showDeleteModal = ref(false)
const editingCategory = ref(null)
const deleteId = ref(null)

const icons = [
  'fas fa-tshirt',
  'fas fa-female',
  'fas fa-male',
  'fas fa-child',
  'fas fa-baby',
  'fas fa-bag-shopping',
  'fas fa-shoe-prints',
  'fas fa-clock',
  'fas fa-glasses',
  'fas fa-gem',
  'fas fa-hat-cowboy',
  'fas fa-vest'
]

const form = ref({
  name: '',
  slug: '',
  icon: 'fas fa-tshirt',
  description: '',
  status: true
})

// Computed
const filteredCategories = computed(() => {
  let filtered = [...categories.value]

  // Search filter
  if (filters.value.search) {
    const searchTerm = filters.value.search.toLowerCase()
    filtered = filtered.filter(cat =>
      cat.name.toLowerCase().includes(searchTerm) ||
      cat.slug.includes(searchTerm)
    )
  }

  // Status filter
  if (filters.value.status !== 'all') {
    filtered = filtered.filter(cat => cat.status === filters.value.status)
  }

  return filtered
})

const paginatedCategories = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredCategories.value.slice(start, end)
})

const totalPages = computed(() => {
  return Math.ceil(filteredCategories.value.length / itemsPerPage)
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

const generateSlug = () => {
  form.value.slug = form.value.name
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}

const openCategoryModal = (category = null) => {
  if (category) {
    editingCategory.value = category
    form.value = {
      name: category.name,
      slug: category.slug,
      icon: category.icon,
      description: category.description || '',
      status: category.status === 'active'
    }
  } else {
    editingCategory.value = null
    form.value = {
      name: '',
      slug: '',
      icon: 'fas fa-tshirt',
      description: '',
      status: true
    }
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingCategory.value = null
}

const saveCategory = () => {
  const categoryData = {
    id: editingCategory.value ? editingCategory.value.id : Date.now(),
    name: form.value.name,
    slug: form.value.slug,
    icon: form.value.icon,
    description: form.value.description,
    status: form.value.status ? 'active' : 'inactive',
    productCount: editingCategory.value ? editingCategory.value.productCount : 0
  }

  if (editingCategory.value) {
    // Update
    const index = categories.value.findIndex(c => c.id === editingCategory.value.id)
    if (index !== -1) {
      categories.value[index] = categoryData
    }
    showNotification('Cập nhật danh mục thành công!', 'success')
  } else {
    // Create
    categories.value.push(categoryData)
    showNotification('Thêm danh mục thành công!', 'success')
  }

  closeModal()
}

const toggleCategoryStatus = (id) => {
  const category = categories.value.find(c => c.id === id)
  if (category) {
    category.status = category.status === 'active' ? 'inactive' : 'active'
    showNotification('Cập nhật trạng thái thành công!', 'success')
  }
}

const confirmDelete = (id) => {
  deleteId.value = id
  showDeleteModal.value = true
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
  deleteId.value = null
}

const deleteCategory = () => {
  const index = categories.value.findIndex(c => c.id === deleteId.value)
  if (index !== -1) {
    categories.value.splice(index, 1)
    showNotification('Xóa danh mục thành công!', 'success')
  }
  closeDeleteModal()
}

// Watch for filter changes
watch(filters, () => {
  applyFilters()
}, { deep: true })
</script>

<style scoped>
.admin-categories {
  animation: fadeIn 0.3s;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.page-title {
  font-size: 28px;
  color: var(--dark-gray);
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

/* Table */
.table-container {
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
  overflow-x: auto;
  margin-bottom: 20px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background: var(--light-gray);
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: var(--dark-gray);
  border-bottom: 2px solid var(--border-color);
}

td {
  padding: 15px;
  border-bottom: 1px solid var(--border-color);
  color: var(--medium-gray);
}

tr:hover {
  background: var(--light-gray);
}

/* Category Icon */
.category-icon {
  width: 40px;
  height: 40px;
  background: var(--light-gray);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.category-icon i {
  font-size: 20px;
  color: var(--primary-red);
}

/* Status Toggle */
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

/* Action Buttons */
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
  transition: all 0.3s;
}

.edit-btn {
  background: #ffc107;
  color: var(--dark-gray);
}

.edit-btn:hover {
  background: #e0a800;
  transform: translateY(-2px);
}

.delete-btn {
  background: #dc3545;
  color: white;
}

.delete-btn:hover:not(:disabled) {
  background: #c82333;
  transform: translateY(-2px);
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* No Data */
.no-data {
  text-align: center;
  padding: 40px;
  color: var(--medium-gray);
}

/* Modal */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.modal-content {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s;
}

.modal-content.modal-sm {
  max-width: 400px;
}

.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  background: white;
  border-radius: 15px 15px 0 0;
}

.modal-header h2 {
  color: var(--dark-gray);
  font-size: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: var(--medium-gray);
  transition: all 0.3s;
}

.close-btn:hover {
  color: var(--primary-red);
  transform: rotate(90deg);
}

.modal-body {
  padding: 25px;
}

.modal-footer {
  padding: 20px 25px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  background: var(--light-gray);
  border-radius: 0 0 15px 15px;
}

/* Form */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: var(--dark-gray);
  font-weight: 500;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  transition: all 0.3s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

/* Icon Selector */
.icon-selector {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(50px, 1fr));
  gap: 10px;
  padding: 15px;
  background: var(--light-gray);
  border-radius: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.icon-btn {
  width: 50px;
  height: 50px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.icon-btn:hover,
.icon-btn.active {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
  transform: scale(1.05);
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

/* Warning Text */
.warning-text {
  color: #dc3545;
  font-size: 14px;
  margin-top: 10px;
}

/* Animations */
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

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(50px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
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

  .admin-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .icon-selector {
    grid-template-columns: repeat(auto-fill, minmax(40px, 1fr));
  }

  .icon-btn {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }
}
</style>