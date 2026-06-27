<template>
  <div class="admin-banners">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>🖼️ Quản lý Banner</h2>
        <p class="subtitle">Quản lý banner trang chủ – click để điều hướng đến đúng trang sản phẩm</p>
      </div>
      <button class="btn-add" @click="openCreate">+ Thêm Banner</button>
    </div>

    <!-- Stats -->
    <div class="stats-row">
      <div class="stat-card stat-active">
        <div class="stat-icon">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ store.activeBannerCount || 0 }}</div>
          <div class="stat-label">Đang hiển thị</div>
        </div>
      </div>
      <div class="stat-card stat-inactive">
        <div class="stat-icon">⏸️</div>
        <div class="stat-info">
          <div class="stat-value">{{ store.inactiveBannerCount || 0 }}</div>
          <div class="stat-label">Tạm ẩn</div>
        </div>
      </div>
      <div class="stat-card stat-total">
        <div class="stat-icon">🖼️</div>
        <div class="stat-info">
          <div class="stat-value">{{ store.totalVisibleCount || 0 }}</div>
          <div class="stat-label">Tổng banner</div>
        </div>
      </div>
    </div>

    <!-- Search -->
    <div class="search-bar">
      <input
        v-model="keyword"
        placeholder="🔍 Tìm theo tiêu đề banner..."
        @input="store.search(keyword)"
      />
    </div>

    <!-- Error Display -->
    <div v-if="store.error" class="error-message">
      ⚠️ {{ store.error }}
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>Đang tải banner...</span>
    </div>

    <!-- Table -->
    <div v-else class="table-container">
      <table class="banner-table">
        <thead>
          <tr>
            <th>Ảnh</th>
            <th>Tiêu đề</th>
            <th>Loại link</th>
            <th>Điều hướng đến</th>
            <th>Thứ tự</th>
            <th>Trạng thái</th>
            <th>Hành động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="banner in store.banners" :key="banner.bannerId">
            <td>
              <img :src="getImageUrl(banner.imagePath)" class="banner-thumb" alt="banner" @error="handleThumbError" />
            </td>
            <td>
              <strong>{{ banner.title }}</strong>
              <p v-if="banner.subtitle" class="subtitle-text">{{ banner.subtitle }}</p>
            </td>
            <td>
              <span :class="'link-badge link-' + (banner.linkType || '').toLowerCase()">
                {{ linkTypeLabel(banner.linkType) }}
              </span>
            </td>
            <td class="resolved-url">{{ resolvedLabel(banner) }}</td>
            <td class="center">{{ banner.displayOrder }}</td>
            <td>
              <span :class="'status-badge status-' + (banner.status || '').toLowerCase()">
                {{ statusLabel(banner.status) }}
              </span>
            </td>
            <td class="actions">
              <button class="btn-edit" @click="openEdit(banner)">✏️ Sửa</button>
              <button v-if="banner.status !== 'DELETED'" class="btn-delete" @click="confirmDelete(banner)">🗑️ Xóa</button>
              <button v-else class="btn-restore" @click="handleRestore(banner)">♻️ Khôi phục</button>
            </td>
          </tr>
          <tr v-if="!store.banners || store.banners.length === 0">
            <td colspan="7" class="empty-row">Chưa có banner nào</td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="pagination" v-if="store.totalPages > 1">
        <button :disabled="store.currentPage === 0" @click="store.setPage(store.currentPage - 1)">◀</button>
        <span>Trang {{ store.currentPage + 1 }} / {{ store.totalPages }}</span>
        <button :disabled="store.currentPage >= store.totalPages - 1" @click="store.setPage(store.currentPage + 1)">▶</button>
      </div>
    </div>

    <!-- Modal Thêm/Sửa -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-box">
        <div class="modal-header">
          <h3>{{ editingBanner ? '✏️ Sửa Banner' : '➕ Thêm Banner Mới' }}</h3>
          <button class="modal-close" @click="closeModal">✕</button>
        </div>

        <div class="modal-body">
          <!-- Title -->
          <div class="form-group">
            <label>Tiêu đề banner <span class="required">*</span></label>
            <input 
              v-model="form.title" 
              placeholder="VD: Bộ sưu tập Thu Đông 2025"
              :class="{ 'input-error': validationErrors.title }"
            />
            <span v-if="validationErrors.title" class="error-text">{{ validationErrors.title }}</span>
          </div>

          <!-- Subtitle -->
          <div class="form-group">
            <label>Phụ đề (subtitle)</label>
            <input v-model="form.subtitle" placeholder="VD: Giảm đến 50% toàn bộ sản phẩm" />
          </div>

          <!-- Image -->
          <div class="form-group">
            <label>Ảnh banner <span class="required">*</span> <span class="hint">(Khuyến nghị: 1920×600px)</span></label>
            <div 
              class="upload-area" 
              @click="$refs.fileInput.click()" 
              @dragover.prevent 
              @drop.prevent="onDrop"
              :class="{ 'upload-error': validationErrors.image }"
            >
              <div v-if="imagePreview" class="preview-wrap">
                <img :src="imagePreview" class="preview-img" />
                <span class="change-hint">Click để đổi ảnh</span>
              </div>
              <div v-else class="upload-placeholder">
                <span>☁️ Kéo thả hoặc click chọn ảnh</span>
                <small>JPG, PNG, WebP – tối đa 10MB</small>
              </div>
            </div>
            <span v-if="validationErrors.image" class="error-text">{{ validationErrors.image }}</span>
            <input 
              type="file" 
              ref="fileInput" 
              accept="image/jpeg,image/png,image/webp" 
              style="display:none" 
              @change="onFileSelect" 
            />
          </div>

          <!-- Link Type -->
          <div class="form-group">
            <label>Khi click banner, chuyển đến <span class="required">*</span></label>
            <select v-model="form.linkType" :class="{ 'input-error': validationErrors.linkType }">
              <option value="CATEGORY">📂 Danh mục sản phẩm</option>
              <option value="PRODUCT">👕 Sản phẩm cụ thể</option>
              <option value="URL">🔗 Đường dẫn tùy chỉnh</option>
            </select>
            <span v-if="validationErrors.linkType" class="error-text">{{ validationErrors.linkType }}</span>
          </div>

          <!-- Category -->
          <div v-if="form.linkType === 'CATEGORY'" class="form-group">
            <label>Chọn danh mục <span class="required">*</span></label>
            <select 
              v-model="form.categoryId" 
              :class="{ 'input-error': validationErrors.categoryId }"
            >
              <option :value="null">-- Chọn danh mục --</option>
              <option v-for="cat in categories" :key="cat.categoryId" :value="cat.categoryId">
                {{ cat.categoryName }}
              </option>
            </select>
            <span v-if="validationErrors.categoryId" class="error-text">{{ validationErrors.categoryId }}</span>
          </div>

          <!-- Product -->
          <div v-if="form.linkType === 'PRODUCT'" class="form-group">
            <label>Chọn sản phẩm <span class="required">*</span></label>
            <select 
              v-model="form.productId" 
              :class="{ 'input-error': validationErrors.productId }"
            >
              <option :value="null">-- Chọn sản phẩm --</option>
              <!-- ✅ Lấy sản phẩm từ store hoặc từ biến products -->
              <option v-for="p in products" :key="p.productId" :value="p.productId">
                {{ p.productName }}
              </option>
            </select>
            <span v-if="validationErrors.productId" class="error-text">{{ validationErrors.productId }}</span>
            <small class="hint-text" v-if="products.length === 0">⚠️ Không có sản phẩm nào. Vui lòng tạo sản phẩm trước.</small>
          </div>

          <!-- Custom URL -->
          <div v-if="form.linkType === 'URL'" class="form-group">
            <label>Đường dẫn tùy chỉnh <span class="required">*</span></label>
            <input 
              v-model="form.customUrl" 
              placeholder="VD: /shop?sale=1"
              :class="{ 'input-error': validationErrors.customUrl }"
            />
            <span v-if="validationErrors.customUrl" class="error-text">{{ validationErrors.customUrl }}</span>
          </div>

          <!-- Display Order & Status -->
          <div class="form-row">
            <div class="form-group half">
              <label>Thứ tự hiển thị</label>
              <input 
                type="number" 
                v-model.number="form.displayOrder" 
                min="0"
                name="displayOrder"
                @input="checkOrderOnInput"
                :class="{ 'input-error': validationErrors.displayOrder }"
                placeholder="Để trống tự động tăng"
              />
              <span v-if="validationErrors.displayOrder" class="error-text">{{ validationErrors.displayOrder }}</span>
              <small class="hint-text" v-else>Để trống để tự động tăng thứ tự</small>
            </div>
            <div class="form-group half">
              <label>Trạng thái</label>
              <select v-model="form.status">
                <option value="ACTIVE">✅ Hiển thị</option>
                <option value="INACTIVE">⏸️ Ẩn</option>
              </select>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="closeModal">Hủy</button>
          <button class="btn-save" @click="saveBanner" :disabled="store.saving">
            <span v-if="store.saving">⏳ Đang lưu...</span>
            <span v-else>💾 Lưu banner</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onErrorCaptured } from 'vue'
import { useBannerStore } from '../../stores/bannerStore'
import axios from 'axios'

onErrorCaptured((err, instance, info) => {
  console.error('❌ AdminBanners Error:', err)
  console.error('Error info:', info)
  return false
})

const store = useBannerStore()
const API_BASE = 'http://localhost:8080/api'

// ========== STATE ==========
const keyword = ref('')
const showModal = ref(false)
const editingBanner = ref(null)
const imagePreview = ref(null)
const selectedFile = ref(null)
const categories = ref([])
const products = ref([])
const validationErrors = reactive({})
let orderCheckTimeout = null

const form = reactive({
  title: '',
  subtitle: '',
  linkType: 'CATEGORY',
  categoryId: null,
  productId: null,
  customUrl: '',
  displayOrder: null,
  status: 'ACTIVE',
})

// ========== LIFECYCLE ==========
onMounted(async () => {
  try {
    console.log('✅ AdminBanners mounted')
    await store.fetchBanners()
    
    // ✅ Gọi API lấy danh sách danh mục
    const catRes = await axios.get(`${API_BASE}/categories/active`)
    categories.value = catRes.data?.content || catRes.data || []
    console.log('✅ Loaded categories:', categories.value.length)
    
    // ✅ Gọi API lấy sản phẩm - SỬ DỤNG API ĐÃ CÓ SẴN /api/products
    // Với tham số size lớn để lấy tất cả
    const prodRes = await axios.get(`${API_BASE}/products`, {
      params: {
        page: 0,
        size: 1000, // Lấy tối đa 1000 sản phẩm
        sortBy: 'productId',
        sortDir: 'desc'
      }
    })
    
    // ✅ Xử lý response từ ApiResponse wrapper
    if (prodRes.data?.data?.content) {
      products.value = prodRes.data.data.content
    } else if (prodRes.data?.content) {
      products.value = prodRes.data.content
    } else if (Array.isArray(prodRes.data)) {
      products.value = prodRes.data
    } else if (prodRes.data?.data && Array.isArray(prodRes.data.data)) {
      products.value = prodRes.data.data
    } else {
      products.value = []
    }
    
    console.log('✅ Loaded products:', products.value.length)
    if (products.value.length > 0) {
      console.log('📦 Sample product:', products.value[0])
    }
    
  } catch (e) {
    console.error('❌ Failed to load data:', e)
  }
})

// ========== MODAL FUNCTIONS ==========
function openCreate() {
  editingBanner.value = null
  resetForm()
  imagePreview.value = null
  selectedFile.value = null
  showModal.value = true
  clearValidationErrors()
}

function openEdit(banner) {
  editingBanner.value = banner
  Object.assign(form, {
    title: banner.title || '',
    subtitle: banner.subtitle || '',
    linkType: banner.linkType || 'URL',
    categoryId: banner.categoryId || null,
    productId: banner.productId || null,
    customUrl: banner.customUrl || '',
    displayOrder: banner.displayOrder !== null && banner.displayOrder !== undefined ? banner.displayOrder : null,
    status: banner.status || 'ACTIVE',
  })
  imagePreview.value = getImageUrl(banner.imagePath)
  selectedFile.value = null
  showModal.value = true
  clearValidationErrors()
}

function closeModal() {
  showModal.value = false
  resetForm()
  clearValidationErrors()
  if (imagePreview.value && imagePreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(imagePreview.value)
  }
  if (orderCheckTimeout) {
    clearTimeout(orderCheckTimeout)
    orderCheckTimeout = null
  }
}

function resetForm() {
  Object.assign(form, {
    title: '',
    subtitle: '',
    linkType: 'CATEGORY',
    categoryId: null,
    productId: null,
    customUrl: '',
    displayOrder: null,
    status: 'ACTIVE',
  })
}

// ========== VALIDATION ==========
async function validateForm() {
  clearValidationErrors()
  let isValid = true

  if (!form.title?.trim()) {
    validationErrors.title = 'Vui lòng nhập tiêu đề banner'
    isValid = false
  } else if (form.title.trim().length < 3) {
    validationErrors.title = 'Tiêu đề phải có ít nhất 3 ký tự'
    isValid = false
  }

  if (!editingBanner.value && !selectedFile.value) {
    validationErrors.image = 'Vui lòng chọn ảnh banner'
    isValid = false
  }

  if (form.linkType === 'CATEGORY' && !form.categoryId) {
    validationErrors.categoryId = 'Vui lòng chọn danh mục'
    isValid = false
  }

  if (form.linkType === 'PRODUCT' && !form.productId) {
    validationErrors.productId = 'Vui lòng chọn sản phẩm'
    isValid = false
  }

  if (form.linkType === 'URL' && !form.customUrl?.trim()) {
    validationErrors.customUrl = 'Vui lòng nhập đường dẫn'
    isValid = false
  }

  if (form.displayOrder !== null && form.displayOrder !== '' && Number(form.displayOrder) < 0) {
    validationErrors.displayOrder = 'Thứ tự hiển thị phải lớn hơn hoặc bằng 0'
    isValid = false
  }

  // ✅ KIỂM TRA THỨ TỰ TRÙNG LẶP
  if (form.displayOrder !== null && form.displayOrder !== '' && Number(form.displayOrder) >= 0) {
    const order = Number(form.displayOrder)
    const excludeId = editingBanner.value?.bannerId || null
    
    try {
      const check = await store.checkOrderExists(order, excludeId)
      if (check.exists) {
        validationErrors.displayOrder = 
          `Thứ tự ${order} đã được sử dụng${editingBanner.value ? ' bởi banner khác' : ''}. Vui lòng chọn thứ tự khác.`
        isValid = false
      }
    } catch (e) {
      console.warn('Check order failed:', e)
    }
  }

  return isValid
}

function clearValidationErrors() {
  Object.keys(validationErrors).forEach(key => delete validationErrors[key])
}

// ========== CHECK ORDER REALTIME ==========
async function checkOrderOnInput() {
  const order = form.displayOrder
  
  if (order === null || order === '' || order === undefined) {
    delete validationErrors.displayOrder
    return
  }

  const numOrder = Number(order)
  if (isNaN(numOrder) || numOrder < 0) {
    validationErrors.displayOrder = 'Thứ tự phải là số không âm'
    return
  }

  if (orderCheckTimeout) {
    clearTimeout(orderCheckTimeout)
  }

  orderCheckTimeout = setTimeout(async () => {
    try {
      const excludeId = editingBanner.value?.bannerId || null
      const check = await store.checkOrderExists(numOrder, excludeId)
      
      if (check.exists) {
        validationErrors.displayOrder = 
          `Thứ tự ${numOrder} đã được sử dụng${editingBanner.value ? ' bởi banner khác' : ''}. Vui lòng chọn thứ tự khác.`
      } else {
        delete validationErrors.displayOrder
      }
    } catch (e) {
      console.warn('Check order realtime failed:', e)
    }
  }, 500)
}

// ========== FILE HANDLING ==========
function onFileSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  handleFile(file)
  e.target.value = ''
}

function onDrop(e) {
  const file = e.dataTransfer.files[0]
  if (!file) return
  handleFile(file)
}

function handleFile(file) {
  if (file.size > 10 * 1024 * 1024) {
    alert('File ảnh quá lớn. Vui lòng chọn ảnh nhỏ hơn 10MB')
    return
  }

  const validTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!validTypes.includes(file.type)) {
    alert('Vui lòng chọn ảnh định dạng JPG, PNG hoặc WebP')
    return
  }

  console.log('📸 File selected:', {
    name: file.name,
    size: file.size,
    type: file.type
  })

  selectedFile.value = file
  if (imagePreview.value && imagePreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(imagePreview.value)
  }
  imagePreview.value = URL.createObjectURL(file)
  delete validationErrors.image
}

// ========== SAVE BANNER ==========
async function saveBanner() {
  const isValid = await validateForm()
  if (!isValid) {
    const firstError = Object.keys(validationErrors)[0]
    if (firstError) {
      if (firstError === 'displayOrder') {
        const input = document.querySelector('input[name="displayOrder"]')
        if (input) input.focus()
      } else {
        const el = document.querySelector(`[name="${firstError}"]`)
        if (el) el.focus()
      }
    }
    return
  }

  const fd = new FormData()
  fd.append('title', form.title.trim())
  fd.append('subtitle', form.subtitle?.trim() || '')
  fd.append('linkType', form.linkType)
  
  if (form.displayOrder !== null && form.displayOrder !== '') {
    fd.append('displayOrder', Number(form.displayOrder))
  }
  
  fd.append('status', form.status || 'ACTIVE')

  if (form.linkType === 'CATEGORY' && form.categoryId) {
    fd.append('categoryId', Number(form.categoryId))
  }
  
  if (form.linkType === 'PRODUCT' && form.productId) {
    fd.append('productId', Number(form.productId))
  }
  
  if (form.linkType === 'URL' && form.customUrl?.trim()) {
    fd.append('customUrl', form.customUrl.trim())
  }
  
  if (selectedFile.value) {
    fd.append('imageFile', selectedFile.value)
    console.log('📤 Appending file:', selectedFile.value.name)
  } else if (!editingBanner.value) {
    alert('Vui lòng chọn ảnh banner')
    return
  }

  console.log('📤 Sending FormData:')
  for (let [key, value] of fd.entries()) {
    console.log(`  ${key}: ${value instanceof File ? `File: ${value.name} (${value.size} bytes)` : value}`)
  }

  const result = editingBanner.value
    ? await store.updateBanner(editingBanner.value.bannerId, fd)
    : await store.createBanner(fd)

  if (result.success) {
    console.log('✅ Banner saved successfully:', result.data)
    closeModal()
    alert(editingBanner.value ? '✅ Cập nhật banner thành công!' : '✅ Tạo banner thành công!')
  } else {
    let errorMsg = result.message || 'Lưu thất bại'
    if (result.errors) {
      if (typeof result.errors === 'object') {
        errorMsg += '\n\nChi tiết:\n' + Object.values(result.errors).join('\n')
      } else if (Array.isArray(result.errors)) {
        errorMsg += '\n\nChi tiết:\n' + result.errors.map(e => `- ${e}`).join('\n')
      }
    }
    alert(`❌ ${errorMsg}`)
  }
}

// ========== DELETE / RESTORE ==========
async function confirmDelete(banner) {
  if (!confirm(`Bạn có chắc muốn xóa banner "${banner.title}"?`)) return
  const result = await store.deleteBanner(banner.bannerId)
  if (result.success) {
    alert('✅ Xóa banner thành công!')
  } else {
    alert(`❌ ${result.message || 'Xóa thất bại'}`)
  }
}

async function handleRestore(banner) {
  if (!confirm(`Bạn có chắc muốn khôi phục banner "${banner.title}"?`)) return
  const result = await store.restoreBanner(banner.bannerId)
  if (result.success) {
    alert('✅ Khôi phục banner thành công!')
  } else {
    alert(`❌ ${result.message || 'Khôi phục thất bại'}`)
  }
}

// ========== HELPERS ==========
function getImageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  if (path.startsWith('/uploads/')) {
    return `http://localhost:8080${path}`
  }
  if (path.startsWith('uploads/')) {
    return `http://localhost:8080/${path}`
  }
  return `http://localhost:8080/uploads/${path}`
}

function handleThumbError(event) {
  console.error('❌ Thumb image load error:', event.target.src)
  event.target.src = ''
  event.target.alt = 'No image'
  event.target.style.display = 'none'
}

function linkTypeLabel(type) {
  const map = {
    CATEGORY: '📂 Danh mục',
    PRODUCT: '👕 Sản phẩm',
    URL: '🔗 URL'
  }
  return map[type] || type
}

function statusLabel(s) {
  const map = {
    ACTIVE: 'Hiển thị',
    INACTIVE: 'Ẩn',
    DELETED: 'Đã xóa'
  }
  return map[s] || s
}

function resolvedLabel(banner) {
  if (banner.linkType === 'CATEGORY') return banner.categoryName || '—'
  if (banner.linkType === 'PRODUCT') return banner.productName || '—'
  return banner.customUrl || '—'
}
</script>


<style scoped>
.admin-banners { 
  padding: 24px; 
  font-family: 'Segoe UI', sans-serif; 
}

.page-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 20px; 
}
.page-header h2 { 
  font-size: 1.5rem; 
  margin: 0; 
}
.subtitle { 
  color: #666; 
  font-size: 0.85rem; 
  margin: 4px 0 0; 
}
.btn-add { 
  background: #1a1a2e; 
  color: #fff; 
  border: none; 
  padding: 10px 20px; 
  border-radius: 8px; 
  cursor: pointer; 
  transition: all 0.3s;
}
.btn-add:hover {
  background: #2d2d4e;
  transform: translateY(-2px);
}

.stats-row { 
  display: flex; 
  gap: 16px; 
  margin-bottom: 20px; 
}
.stat-card { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
  background: #fff; 
  border-radius: 10px; 
  padding: 16px 20px; 
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); 
  flex: 1; 
}
.stat-icon { 
  font-size: 1.8rem; 
}
.stat-value { 
  font-size: 1.4rem; 
  font-weight: 700; 
}
.stat-label { 
  color: #888; 
  font-size: 0.8rem; 
}
.stat-active { 
  border-left: 4px solid #22c55e; 
}
.stat-inactive { 
  border-left: 4px solid #f59e0b; 
}
.stat-total { 
  border-left: 4px solid #6366f1; 
}

.search-bar { 
  margin-bottom: 16px; 
}
.search-bar input { 
  width: 100%; 
  max-width: 400px; 
  padding: 10px 14px; 
  border: 1px solid #ddd; 
  border-radius: 8px; 
  transition: border-color 0.3s;
}
.search-bar input:focus {
  outline: none;
  border-color: #6366f1;
}

.error-message {
  background: #fee2e2;
  color: #b91c1c;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid #fecaca;
}

.table-container { 
  background: #fff; 
  border-radius: 12px; 
  box-shadow: 0 2px 8px rgba(0,0,0,0.07); 
  overflow: hidden; 
}
.banner-table { 
  width: 100%; 
  border-collapse: collapse; 
}
.banner-table th { 
  background: #f8f9fa; 
  padding: 12px 16px; 
  text-align: left; 
  font-size: 0.85rem; 
  color: #555; 
  border-bottom: 2px solid #eee; 
}
.banner-table td { 
  padding: 12px 16px; 
  border-bottom: 1px solid #f0f0f0; 
  vertical-align: middle; 
}
.banner-thumb { 
  width: 120px; 
  height: 40px; 
  object-fit: cover; 
  border-radius: 6px; 
  border: 1px solid #eee; 
}
.subtitle-text { 
  color: #888; 
  font-size: 0.8rem; 
  margin: 2px 0 0; 
}
.center { 
  text-align: center; 
}
.resolved-url { 
  font-size: 0.82rem; 
  color: #555; 
  max-width: 180px; 
  word-break: break-all; 
}
.empty-row { 
  text-align: center; 
  color: #aaa; 
  padding: 32px; 
}

.link-badge { 
  padding: 3px 10px; 
  border-radius: 20px; 
  font-size: 0.78rem; 
  font-weight: 600; 
}
.link-category { 
  background: #dbeafe; 
  color: #1d4ed8; 
}
.link-product { 
  background: #dcfce7; 
  color: #15803d; 
}
.link-url { 
  background: #fef9c3; 
  color: #a16207; 
}
.status-badge { 
  padding: 3px 10px; 
  border-radius: 20px; 
  font-size: 0.78rem; 
  font-weight: 600; 
}
.status-active { 
  background: #dcfce7; 
  color: #15803d; 
}
.status-inactive { 
  background: #fef3c7; 
  color: #92400e; 
}
.status-deleted { 
  background: #fee2e2; 
  color: #b91c1c; 
}

.actions { 
  display: flex; 
  gap: 8px; 
  flex-wrap: wrap;
}
.btn-edit { 
  background: #3b82f6; 
  color: #fff; 
  border: none; 
  padding: 6px 12px; 
  border-radius: 6px; 
  cursor: pointer; 
  font-size: 0.8rem; 
  transition: all 0.3s;
}
.btn-edit:hover {
  background: #2563eb;
}
.btn-delete { 
  background: #ef4444; 
  color: #fff; 
  border: none; 
  padding: 6px 12px; 
  border-radius: 6px; 
  cursor: pointer; 
  font-size: 0.8rem; 
  transition: all 0.3s;
}
.btn-delete:hover {
  background: #dc2626;
}
.btn-restore { 
  background: #10b981; 
  color: #fff; 
  border: none; 
  padding: 6px 12px; 
  border-radius: 6px; 
  cursor: pointer; 
  font-size: 0.8rem; 
  transition: all 0.3s;
}
.btn-restore:hover {
  background: #059669;
}

.pagination { 
  display: flex; 
  justify-content: center; 
  align-items: center; 
  gap: 12px; 
  padding: 16px; 
}
.pagination button { 
  padding: 6px 14px; 
  border: 1px solid #ddd; 
  background: #fff; 
  border-radius: 6px; 
  cursor: pointer; 
  transition: all 0.3s;
}
.pagination button:hover:not(:disabled) {
  background: #f1f5f9;
}
.pagination button:disabled { 
  opacity: 0.4; 
  cursor: not-allowed; 
}

.modal-overlay { 
  position: fixed; 
  inset: 0; 
  background: rgba(0,0,0,0.5); 
  z-index: 1000; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
}
.modal-box { 
  background: #fff; 
  border-radius: 14px; 
  width: 600px; 
  max-width: 95vw; 
  max-height: 90vh; 
  overflow-y: auto; 
}
.modal-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  padding: 20px 24px; 
  border-bottom: 1px solid #eee; 
}
.modal-header h3 { 
  margin: 0; 
}
.modal-close { 
  background: none; 
  border: none; 
  font-size: 1.2rem; 
  cursor: pointer; 
  color: #888; 
  transition: color 0.3s;
}
.modal-close:hover {
  color: #333;
}
.modal-body { 
  padding: 20px 24px; 
  display: flex; 
  flex-direction: column; 
  gap: 14px; 
}
.modal-footer { 
  padding: 16px 24px; 
  border-top: 1px solid #eee; 
  display: flex; 
  justify-content: flex-end; 
  gap: 10px; 
}

.form-group { 
  display: flex; 
  flex-direction: column; 
  gap: 6px; 
}
.form-group label { 
  font-size: 0.88rem; 
  font-weight: 600; 
  color: #333; 
}
.required {
  color: #ef4444;
  font-weight: 700;
}
.hint { 
  font-weight: 400; 
  color: #888; 
  font-size: 0.8rem; 
}
.form-group input, 
.form-group select { 
  padding: 9px 12px; 
  border: 1px solid #ddd; 
  border-radius: 8px; 
  font-size: 0.9rem; 
  transition: border-color 0.3s;
}
.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #6366f1;
}
.form-group input.input-error,
.form-group select.input-error {
  border-color: #ef4444;
}
.error-text {
  color: #ef4444;
  font-size: 0.8rem;
  margin-top: 2px;
}
.hint-text {
  color: #888;
  font-size: 0.75rem;
  margin-top: 2px;
}
.form-row { 
  display: flex; 
  gap: 14px; 
}
.half { 
  flex: 1; 
}

.upload-area { 
  border: 2px dashed #cbd5e1; 
  border-radius: 10px; 
  cursor: pointer; 
  overflow: hidden; 
  min-height: 90px; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  transition: all 0.3s;
}
.upload-area:hover { 
  border-color: #6366f1; 
}
.upload-area.upload-error {
  border-color: #ef4444;
  background: #fef2f2;
}
.upload-placeholder { 
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  gap: 6px; 
  color: #94a3b8; 
  padding: 20px; 
}
.preview-wrap { 
  position: relative; 
  width: 100%; 
}
.preview-img { 
  width: 100%; 
  height: 120px; 
  object-fit: cover; 
  display: block; 
}
.change-hint { 
  position: absolute; 
  inset: 0; 
  background: rgba(0,0,0,0.35); 
  color: #fff; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  opacity: 0; 
  transition: opacity 0.2s; 
}
.preview-wrap:hover .change-hint { 
  opacity: 1; 
}

.btn-cancel { 
  padding: 9px 20px; 
  background: #f1f5f9; 
  border: none; 
  border-radius: 8px; 
  cursor: pointer; 
  transition: all 0.3s;
}
.btn-cancel:hover {
  background: #e2e8f0;
}
.btn-save { 
  padding: 9px 20px; 
  background: #1a1a2e; 
  color: #fff; 
  border: none; 
  border-radius: 8px; 
  cursor: pointer; 
  transition: all 0.3s;
}
.btn-save:hover:not(:disabled) {
  background: #2d2d4e;
}
.btn-save:disabled { 
  opacity: 0.5; 
  cursor: not-allowed; 
}

.loading-state { 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  gap: 10px; 
  padding: 40px; 
  color: #666; 
}
.loading-spinner { 
  width: 24px; 
  height: 24px; 
  border: 3px solid #eee; 
  border-top-color: #6366f1; 
  border-radius: 50%; 
  animation: spin 0.8s linear infinite; 
}
@keyframes spin { 
  to { transform: rotate(360deg); } 
}

@media (max-width: 768px) {
  .stats-row {
    flex-direction: column;
  }
  .form-row {
    flex-direction: column;
  }
  .modal-box {
    width: 100%;
    max-width: 100vw;
    max-height: 100vh;
    border-radius: 0;
  }
  .banner-table {
    font-size: 0.8rem;
  }
  .banner-thumb {
    width: 80px;
    height: 30px;
  }
  .actions {
    flex-direction: column;
  }
}
</style>