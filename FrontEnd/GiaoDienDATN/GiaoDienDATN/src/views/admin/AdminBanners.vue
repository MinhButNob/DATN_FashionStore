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
    <div v-if="showModal" class="modal-overlay">
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
            <label>Ảnh banner <span class="required">*</span> <span class="hint">(Khuyến nghị: 1920×660px, tỉ lệ 16:5.5 – tối thiểu 1200×412px để không bị mờ)</span></label>
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
            <span v-if="imageQualityWarning" class="warning-text">⚠️ {{ imageQualityWarning }}</span>
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
          <div v-if="form.linkType === 'CATEGORY'" class="form-group category-search-group">
            <label>Chọn danh mục <span class="required">*</span></label>
            <div class="category-search-box" ref="categorySearchBoxRef">
              <input
                type="text"
                v-model="categorySearchQuery"
                placeholder="Gõ để tìm danh mục..."
                autocomplete="off"
                :class="{ 'input-error': validationErrors.categoryId }"
                @focus="showCategorySuggestions = true"
                @input="showCategorySuggestions = true"
              />
              <ul v-if="showCategorySuggestions" class="category-suggestions">
                <li v-if="filteredCategorySuggestions.length === 0" class="no-result">
                  Không tìm thấy danh mục phù hợp
                </li>
                <li
                  v-for="cat in filteredCategorySuggestions"
                  :key="cat.categoryId"
                  @click="selectCategorySuggestion(cat)"
                >
                  {{ cat.categoryName }}
                </li>
              </ul>
            </div>
            <span v-if="validationErrors.categoryId" class="error-text">{{ validationErrors.categoryId }}</span>
          </div>

          <!-- Product -->
          <div v-if="form.linkType === 'PRODUCT'" class="form-group category-search-group">
            <label>Chọn sản phẩm <span class="required">*</span></label>
            <div class="category-search-box" ref="productSearchBoxRef">
              <input
                type="text"
                v-model="productSearchQuery"
                placeholder="Gõ để tìm sản phẩm..."
                autocomplete="off"
                :class="{ 'input-error': validationErrors.productId }"
                @focus="onProductSearchFocus"
                @input="showProductSuggestions = true"
              />
              <ul v-if="showProductSuggestions" class="category-suggestions">
                <li v-if="loadingFeaturedProducts && filteredProductSuggestions.length === 0" class="no-result">
                  ⏳ Đang tải gợi ý...
                </li>
                <li v-else-if="filteredProductSuggestions.length === 0" class="no-result">
                  Không tìm thấy sản phẩm phù hợp
                </li>
                <template v-else>
                  <li v-if="isShowingFeaturedSuggestions" class="suggestion-header">
                    ⭐ Sản phẩm tiêu biểu
                  </li>
                  <li
                    v-for="p in filteredProductSuggestions"
                    :key="p.productId"
                    @click="selectProductSuggestion(p)"
                  >
                    {{ p.productName }}
                  </li>
                </template>
              </ul>
            </div>
            <span v-if="validationErrors.productId" class="error-text">{{ validationErrors.productId }}</span>
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
                :class="{ 'input-error': validationErrors.displayOrder }"
              />
              <span v-if="validationErrors.displayOrder" class="error-text">{{ validationErrors.displayOrder }}</span>
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
import { ref, reactive, computed, onMounted, onUnmounted, onErrorCaptured } from 'vue'
import { useBannerStore } from '../../stores/bannerStore'
import api from "@/api/axios";
import productService from '../../api/productService'

onErrorCaptured((err, instance, info) => {
  console.error('❌ AdminBanners Error:', err)
  console.error('Error info:', info)
  return false
})

const store = useBannerStore()
// ========== STATE ==========
const keyword = ref('')
const showModal = ref(false)
const editingBanner = ref(null)
const imagePreview = ref(null)
const selectedFile = ref(null)
const imageQualityWarning = ref('')
const categories = ref([])
const products = ref([])
// Danh sách sản phẩm tiêu biểu (nổi bật) - gợi ý ngay khi click vào ô tìm kiếm, chưa cần gõ gì
const featuredProducts = ref([])
const loadingFeaturedProducts = ref(false)
// Tìm kiếm/gợi ý danh mục & sản phẩm (thay cho combobox) - giống bên Quản lý sản phẩm
const categorySearchQuery = ref('')
const showCategorySuggestions = ref(false)
const categorySearchBoxRef = ref(null)
const productSearchQuery = ref('')
const showProductSuggestions = ref(false)
const productSearchBoxRef = ref(null)
const validationErrors = reactive({})

const form = reactive({
  title: '',
  subtitle: '',
  linkType: 'CATEGORY',
  categoryId: null,
  productId: null,
  customUrl: '',
  displayOrder: 0,
  status: 'ACTIVE' })

// ========== CLICK RA NGOÀI ĐỂ ĐÓNG GỢI Ý (thay cho @blur + setTimeout) ==========
// Lý do đổi cách làm cũ: @blur kết hợp setTimeout dễ bị tranh chấp (race condition)
// với sự kiện click chọn gợi ý, khiến ô input bị mất focus liên tục / "nháy" màn hình
// và không gõ được. Dùng document click để phát hiện click-ra-ngoài sẽ ổn định hơn.
function handleClickOutsideSearchBoxes(event) {
  if (categorySearchBoxRef.value && !categorySearchBoxRef.value.contains(event.target)) {
    showCategorySuggestions.value = false
    const current = categories.value.find(c => c.categoryId === form.categoryId)
    categorySearchQuery.value = current?.categoryName || ''
  }
  if (productSearchBoxRef.value && !productSearchBoxRef.value.contains(event.target)) {
    showProductSuggestions.value = false
    const current = products.value.find(p => p.productId === form.productId)
    productSearchQuery.value = current?.productName || ''
  }
}

// ========== LIFECYCLE ==========
onMounted(async () => {
  document.addEventListener('click', handleClickOutsideSearchBoxes)
  try {
    console.log('✅ AdminBanners mounted')
    await store.fetchBanners()
    
    const [catRes, prodRes, parentCatRes] = await Promise.all([
      api.get(`/categories/active`),
      api.get(`/products`, { params: { size: 200 } }),
      api.get(`/category-hierarchy/parents`).catch(() => ({ data: [] }))
    ])
    // ⚠️ Backend bọc dữ liệu 2 lớp: ApiResponse { data: PageResponse { content: [...] } }
    // Trước đây code chỉ bóc 1 lớp (res.data.content) nên products.value bị gán nhầm
    // thành cả object thay vì mảng -> gây lỗi "products.value.slice is not a function"
    // khi click vào ô "Chọn sản phẩm".
    categories.value = catRes.data?.content || catRes.data?.data?.content || catRes.data || []
    products.value = prodRes.data?.data?.content || prodRes.data?.content || []
    // Chốt an toàn: nếu backend đổi cấu trúc lần nữa mà không khớp field nào ở trên,
    // ép về mảng rỗng thay vì để lọt object vào -> tránh crash toàn trang khi gọi .slice()/.filter()
    if (!Array.isArray(categories.value)) categories.value = []
    if (!Array.isArray(products.value)) products.value = []

    // ✅ Loại bỏ 3 danh mục lớn (Áo, Quần, Phụ kiện...) khỏi danh sách gợi ý chọn danh mục
    // của banner - vì đó là danh mục cha dùng để phân cấp, không phải danh mục để gán banner.
    // Các danh mục cha được lấy từ /api/category-hierarchy/parents.
    const parentList = parentCatRes.data?.content || parentCatRes.data?.data?.content || parentCatRes.data || []
    const parentCategoryIds = new Set(
      (Array.isArray(parentList) ? parentList : []).map(p => p.categoryId)
    )
    if (parentCategoryIds.size > 0) {
      categories.value = categories.value.filter(c => !parentCategoryIds.has(c.categoryId))
    }

    console.log('✅ Loaded categories:', categories.value.length)
    console.log('✅ Loaded products:', products.value.length)
  } catch (e) {
    console.error('❌ Failed to load data:', e)
  }

  // Tải sẵn sản phẩm tiêu biểu để gợi ý ngay khi admin click vào ô "Chọn sản phẩm"
  loadFeaturedProducts()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutsideSearchBoxes)
})

// ========== SẢN PHẨM TIÊU BIỂU (gợi ý khi click vào ô tìm kiếm, chưa gõ gì) ==========
async function loadFeaturedProducts() {
  loadingFeaturedProducts.value = true
  try {
    const res = await productService.getFeaturedProducts(0, 8)
    // HomeProductResponse trả field tên "products", không phải "content"
    const list = res.data?.products || res.data?.data?.products || res.data?.content || []
    featuredProducts.value = Array.isArray(list) ? list : []
    console.log('✅ Loaded featured products:', featuredProducts.value.length)
  } catch (e) {
    console.error('❌ Failed to load featured products, fallback to danh sách sản phẩm:', e)
    featuredProducts.value = []
  } finally {
    loadingFeaturedProducts.value = false
  }
}

// ========== MODAL FUNCTIONS ==========
function openCreate() {
  editingBanner.value = null
  resetForm()
  imagePreview.value = null
  selectedFile.value = null
  categorySearchQuery.value = ''
  productSearchQuery.value = ''
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
    displayOrder: banner.displayOrder || 0,
    status: banner.status || 'ACTIVE' })
  // ✅ SỬA: Hiển thị ảnh preview đúng cách
  imagePreview.value = getImageUrl(banner.imagePath)
  selectedFile.value = null

  const currentCategory = categories.value.find(c => c.categoryId === form.categoryId)
  categorySearchQuery.value = currentCategory?.categoryName || ''
  const currentProduct = products.value.find(p => p.productId === form.productId)
  productSearchQuery.value = currentProduct?.productName || ''

  showModal.value = true
  clearValidationErrors()
}

function closeModal() {
  showModal.value = false
  resetForm()
  clearValidationErrors()
  // Cleanup URL object để tránh memory leak
  if (imagePreview.value && imagePreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(imagePreview.value)
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
    displayOrder: 0,
    status: 'ACTIVE' })
  categorySearchQuery.value = ''
  productSearchQuery.value = ''
  showCategorySuggestions.value = false
  showProductSuggestions.value = false
  imageQualityWarning.value = ''
}

// ========== DANH MỤC / SẢN PHẨM - Ô TÌM KIẾM GỢI Ý ==========
const filteredCategorySuggestions = computed(() => {
  const query = categorySearchQuery.value.trim().toLowerCase()
  if (!query) return categories.value.slice(0, 8)
  return categories.value.filter(c => (c.categoryName || '').toLowerCase().includes(query))
})

function selectCategorySuggestion(cat) {
  form.categoryId = cat.categoryId
  categorySearchQuery.value = cat.categoryName
  showCategorySuggestions.value = false
}

// Khi chưa gõ gì (query rỗng): ưu tiên hiển thị sản phẩm tiêu biểu/nổi bật.
// Khi đã gõ: tìm kiếm trong toàn bộ danh sách sản phẩm như bình thường.
const isShowingFeaturedSuggestions = computed(() => {
  return !productSearchQuery.value.trim() && featuredProducts.value.length > 0
})

const filteredProductSuggestions = computed(() => {
  const query = productSearchQuery.value.trim().toLowerCase()
  if (!query) {
    return featuredProducts.value.length > 0
      ? featuredProducts.value
      : products.value.slice(0, 8)
  }
  return products.value.filter(p => (p.productName || '').toLowerCase().includes(query))
})

function onProductSearchFocus() {
  showProductSuggestions.value = true
  // Nếu chưa tải được sản phẩm tiêu biểu (ví dụ lần trước lỗi), thử tải lại khi admin click vào ô
  if (!productSearchQuery.value.trim() && featuredProducts.value.length === 0 && !loadingFeaturedProducts.value) {
    loadFeaturedProducts()
  }
}

function selectProductSuggestion(p) {
  form.productId = p.productId
  productSearchQuery.value = p.productName
  showProductSuggestions.value = false
}

// ========== VALIDATION ==========
function validateForm() {
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

  if (form.displayOrder < 0) {
    validationErrors.displayOrder = 'Thứ tự hiển thị phải lớn hơn hoặc bằng 0'
    isValid = false
  }

  return isValid
}

function clearValidationErrors() {
  Object.keys(validationErrors).forEach(key => delete validationErrors[key])
}

// ========== FILE HANDLING ==========
function onFileSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  handleFile(file)
  // Reset input để có thể chọn lại cùng file
  e.target.value = ''
}

function onDrop(e) {
  const file = e.dataTransfer.files[0]
  if (!file) return
  handleFile(file)
}

function handleFile(file) {
  // Kiểm tra kích thước
  if (file.size > 10 * 1024 * 1024) {
    alert('File ảnh quá lớn. Vui lòng chọn ảnh nhỏ hơn 10MB')
    return
  }

  // Kiểm tra định dạng
  const validTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!validTypes.includes(file.type)) {
    alert('Vui lòng chọn ảnh định dạng JPG, PNG hoặc WebP')
    return
  }

  // ✅ SỬA: Log file để debug
  console.log('📸 File selected:', {
    name: file.name,
    size: file.size,
    type: file.type
  })

  selectedFile.value = file
  // Tạo preview
  if (imagePreview.value && imagePreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(imagePreview.value)
  }
  imagePreview.value = URL.createObjectURL(file)
  delete validationErrors.image

  // ✅ Kiểm tra độ phân giải thực tế của ảnh - banner được kéo giãn full chiều rộng
  // màn hình trên trang chủ, nếu ảnh gốc quá nhỏ sẽ bị vỡ/mờ khi phóng to.
  checkImageResolution(imagePreview.value)
}

function checkImageResolution(url) {
  imageQualityWarning.value = ''
  const img = new Image()
  img.onload = () => {
    const { naturalWidth: w, naturalHeight: h } = img
    const MIN_WIDTH = 1200
    if (w < MIN_WIDTH) {
      imageQualityWarning.value = `Ảnh của bạn chỉ ${w}×${h}px – khá nhỏ so với chiều rộng màn hình. Banner sẽ được phóng to full màn hình trên trang chủ nên ảnh dễ bị mờ/vỡ. Nên dùng ảnh tối thiểu 1200×412px, lý tưởng là 1920×660px trở lên.`
    }
  }
  img.src = url
}

// ========== SAVE BANNER ==========
async function saveBanner() {
  if (!validateForm()) {
    const firstError = Object.keys(validationErrors)[0]
    if (firstError) {
      const el = document.querySelector(`[name="${firstError}"]`)
      if (el) el.focus()
    }
    return
  }

  const fd = new FormData()
  fd.append('title', form.title.trim())
  fd.append('subtitle', form.subtitle?.trim() || '')
  fd.append('linkType', form.linkType)
  fd.append('displayOrder', Number(form.displayOrder) || 0)
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
  
  // ✅ QUAN TRỌNG: Chỉ append file khi có file mới
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
    console.log('📸 Image path:', result.data?.imagePath)
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
// ✅ SỬA: Hàm getImageUrl xử lý đúng đường dẫn
function getImageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  if (path.startsWith('/uploads/')) {
    return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/${path}`
  }
  if (path.startsWith('uploads/')) {
    return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/${path}`
  }
  return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/uploads/${path}`
}

// ✅ THÊM: Xử lý lỗi ảnh thumb
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
.category-search-group {
  position: relative;
}
.category-search-box {
  position: relative;
}
.category-search-box input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.9rem;
  transition: border-color 0.3s;
}
.category-search-box input:focus {
  outline: none;
  border-color: #6366f1;
}
.category-suggestions {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 20;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  max-height: 220px;
  overflow-y: auto;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}
.category-suggestions li {
  padding: 9px 14px;
  cursor: pointer;
  font-size: 0.9rem;
  color: #333;
}
.category-suggestions li:hover {
  background: #eef0ff;
  color: #6366f1;
}
.category-suggestions li.no-result {
  color: #999;
  cursor: default;
}
.category-suggestions li.no-result:hover {
  background: transparent;
  color: #999;
}
.category-suggestions li.suggestion-header {
  padding: 6px 14px;
  font-size: 0.75rem;
  font-weight: 700;
  color: #a16207;
  background: #fffbeb;
  cursor: default;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.category-suggestions li.suggestion-header:hover {
  background: #fffbeb;
  color: #a16207;
}

.error-text {
  color: #ef4444;
  font-size: 0.8rem;
  margin-top: 2px;
}
.warning-text {
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 0.8rem;
  margin-top: 4px;
  display: block;
  line-height: 1.4;
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