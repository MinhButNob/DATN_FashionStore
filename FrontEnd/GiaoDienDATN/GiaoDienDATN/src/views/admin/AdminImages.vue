<template>
  <div class="admin-images">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-title">
        <h2>Quản lý hình ảnh</h2>
        <p class="subtitle">Quản lý kho tài nguyên hình ảnh sản phẩm, banner và nội dung hệ thống</p>
      </div>
      <button class="btn btn-primary btn-upload-trigger" @click="openUploadModal">
        <i class="fas fa-cloud-upload-alt"></i> Tải ảnh lên
      </button>
    </div>

    <!-- Stats Section -->
    <div class="stats-grid">
      <div class="stats-card">
        <div class="stats-icon bg-red">
          <i class="fas fa-images"></i>
        </div>
        <div class="stats-info">
          <span class="stats-label">Tổng số ảnh</span>
          <span class="stats-value">{{ images.length }}</span>
          <span class="stats-sub text-success"><i class="fas fa-check-circle"></i> Hoạt động tốt</span>
        </div>
      </div>

      <div class="stats-card">
        <div class="stats-icon bg-blue">
          <i class="fas fa-hdd"></i>
        </div>
        <div class="stats-info">
          <span class="stats-label">Dung lượng sử dụng</span>
          <span class="stats-value">{{ formattedStorageUsed }}</span>
          <span class="stats-sub text-muted">Giới hạn lưu trữ: 100 MB</span>
        </div>
      </div>

      <div class="stats-card">
        <div class="stats-icon bg-green">
          <i class="fas fa-tshirt"></i>
        </div>
        <div class="stats-info">
          <span class="stats-label">Ảnh sản phẩm</span>
          <span class="stats-value">{{ countByType('product') }}</span>
          <span class="stats-sub text-muted">Liên kết trực tiếp với kho hàng</span>
        </div>
      </div>

      <div class="stats-card">
        <div class="stats-icon bg-yellow">
          <i class="fas fa-ad"></i>
        </div>
        <div class="stats-info">
          <span class="stats-label">Banners &amp; Khác</span>
          <span class="stats-value">{{ countByType('banner') + countByType('category') + countByType('avatar') }}</span>
          <span class="stats-sub text-muted">Chiến dịch marketing &amp; Hệ thống</span>
        </div>
      </div>
    </div>

    <!-- Filters Section -->
    <div class="filters-section">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="filters.search"
          placeholder="Tìm kiếm hình ảnh theo tên hoặc file..."
        />
      </div>

      <div class="filter-controls">
        <select v-model="filters.type">
          <option value="all">Tất cả thể loại</option>
          <option value="product">Sản phẩm</option>
          <option value="banner">Banners quảng cáo</option>
          <option value="category">Danh mục sản phẩm</option>
          <option value="avatar">Ảnh đại diện</option>
        </select>

        <select v-model="filters.sort">
          <option value="newest">Mới tải lên</option>
          <option value="oldest">Cũ nhất</option>
          <option value="largest">Dung lượng lớn nhất</option>
          <option value="smallest">Dung lượng nhỏ nhất</option>
          <option value="name-asc">Tên A - Z</option>
          <option value="name-desc">Tên Z - A</option>
        </select>

        <button 
          v-if="filters.search || filters.type !== 'all' || filters.sort !== 'newest'" 
          class="btn-reset" 
          @click="resetFilters"
          title="Xóa bộ lọc"
        >
          <i class="fas fa-undo"></i>
        </button>
      </div>
    </div>

    <!-- Bulk Actions -->
    <Transition name="slide-fade">
      <div v-if="selectedImages.length > 0" class="bulk-actions">
        <div class="bulk-info">
          <label class="checkbox-container select-all-wrapper">
            <input 
              type="checkbox" 
              :checked="selectedImages.length === filteredImages.length" 
              @change="toggleSelectAll"
            />
            <span class="checkmark"></span>
          </label>
          <span>Đã chọn <strong class="text-primary">{{ selectedImages.length }}</strong> hình ảnh</span>
        </div>
        <div class="bulk-buttons">
          <button class="btn btn-outline" @click="clearSelection">
            Hủy chọn
          </button>
          <button class="btn btn-danger" @click="confirmBulkDelete">
            <i class="fas fa-trash-alt"></i> Xóa đã chọn
          </button>
        </div>
      </div>
    </Transition>

    <!-- Image Grid Gallery -->
    <div class="gallery-wrapper">
      <div class="gallery-toolbar" v-if="filteredImages.length > 0">
        <label class="checkbox-container select-all-label">
          <input 
            type="checkbox" 
            :checked="selectedImages.length > 0 && selectedImages.length === filteredImages.length"
            @change="toggleSelectAll" 
          />
          <span class="checkmark"></span>
          <span class="label-text">Chọn tất cả ({{ filteredImages.length }} ảnh)</span>
        </label>
      </div>

      <div v-if="filteredImages.length === 0" class="empty-state">
        <div class="empty-icon">
          <i class="fas fa-folder-open"></i>
        </div>
        <h3>Không tìm thấy hình ảnh nào</h3>
        <p>Thử thay đổi bộ lọc tìm kiếm hoặc tải thêm hình ảnh mới lên hệ thống.</p>
        <button class="btn btn-primary mt-3" @click="openUploadModal">
          <i class="fas fa-cloud-upload-alt"></i> Tải ảnh lên ngay
        </button>
      </div>

      <div v-else class="image-grid">
        <div 
          v-for="(img, index) in filteredImages" 
          :key="img.id" 
          class="image-card"
          :class="{ selected: isSelected(img.id) }"
        >
          <!-- Card Checkbox -->
          <label class="checkbox-container card-checkbox">
            <input 
              type="checkbox" 
              :value="img.id" 
              v-model="selectedImages"
            />
            <span class="checkmark"></span>
          </label>

          <!-- Card Preview -->
          <div class="image-preview" @click="openLightbox(index)">
            <img :src="img.url" :alt="img.name" loading="lazy" />
            <div class="hover-overlay">
              <span class="btn-preview-icon"><i class="fas fa-expand-alt"></i> Preview</span>
            </div>
          </div>

          <!-- Card Body -->
          <div class="image-card-body">
            <div class="image-tags">
              <span :class="['badge', getTypeBadgeClass(img.type)]">
                {{ getTypeName(img.type) }}
              </span>
            </div>
            <h4 class="image-title" :title="img.name">{{ img.name }}</h4>
            <p class="image-filename" :title="img.filename">{{ img.filename }}</p>
          </div>

          <!-- Card Footer (Actions & Metadata) -->
          <div class="image-card-footer">
            <div class="meta-info">
              <span><i class="far fa-image"></i> {{ img.dimensions }}</span>
              <span><i class="fas fa-weight-hanging"></i> {{ img.size }}</span>
            </div>
            <div class="card-actions">
              <button 
                class="action-btn copy" 
                @click="copyImageUrl(img.url)" 
                title="Sao chép đường dẫn hình ảnh"
              >
                <i class="fas fa-link"></i>
              </button>
              <button 
                class="action-btn delete" 
                @click="confirmDelete(img.id)" 
                title="Xóa hình ảnh"
              >
                <i class="fas fa-trash-alt"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Lightbox/Details Modal (Slider supported!) -->
    <div v-if="showLightbox" class="lightbox-overlay" @click.self="closeLightbox">
      <button class="lightbox-nav prev" @click="prevImage" title="Ảnh trước">
        <i class="fas fa-chevron-left"></i>
      </button>
      
      <div class="lightbox-container">
        <!-- Close Button -->
        <button class="lightbox-close" @click="closeLightbox">&times;</button>
        
        <!-- Image Area -->
        <div class="lightbox-main">
          <img :src="currentLightboxImage.url" :alt="currentLightboxImage.name" />
        </div>

        <!-- Info Sidebar -->
        <div class="lightbox-sidebar">
          <div class="sidebar-header">
            <h3>Chi tiết hình ảnh</h3>
            <span :class="['badge', getTypeBadgeClass(currentLightboxImage.type)]">
              {{ getTypeName(currentLightboxImage.type) }}
            </span>
          </div>

          <div class="sidebar-body">
            <div class="info-group">
              <label>Tên hiển thị</label>
              <p class="value font-bold">{{ currentLightboxImage.name }}</p>
            </div>

            <div class="info-group">
              <label>Tên tệp tin (File name)</label>
              <p class="value code">{{ currentLightboxImage.filename }}</p>
            </div>

            <div class="info-group-row">
              <div class="info-group">
                <label>Độ phân giải</label>
                <p class="value"><i class="far fa-image"></i> {{ currentLightboxImage.dimensions }}</p>
              </div>
              <div class="info-group">
                <label>Dung lượng</label>
                <p class="value"><i class="fas fa-hdd"></i> {{ currentLightboxImage.size }}</p>
              </div>
            </div>

            <div class="info-group">
              <label>Ngày tải lên</label>
              <p class="value"><i class="far fa-calendar-alt"></i> {{ formatDate(currentLightboxImage.uploadedAt) }}</p>
            </div>

            <div class="info-group" v-if="currentLightboxImage.productsAssoc?.length">
              <label>Đang được sử dụng bởi</label>
              <div class="assoc-tags">
                <span v-for="assoc in currentLightboxImage.productsAssoc" :key="assoc" class="assoc-tag">
                  <i class="fas fa-link"></i> {{ assoc }}
                </span>
              </div>
            </div>
            
            <div class="info-group url-group">
              <label>Đường dẫn URL</label>
              <div class="url-input-wrapper">
                <input type="text" readonly :value="currentLightboxImage.url" ref="urlInput" @click="selectUrlInput" />
                <button class="btn btn-primary btn-sm" @click="copyImageUrl(currentLightboxImage.url)">
                  <i class="far fa-copy"></i>
                </button>
              </div>
            </div>
          </div>

          <div class="sidebar-footer">
            <button class="btn btn-outline" @click="copyImageUrl(currentLightboxImage.url)">
              <i class="fas fa-share-alt"></i> Chia sẻ
            </button>
            <button class="btn btn-danger" @click="confirmDelete(currentLightboxImage.id)">
              <i class="fas fa-trash-alt"></i> Xóa ảnh này
            </button>
          </div>
        </div>
      </div>

      <button class="lightbox-nav next" @click="nextImage" title="Ảnh tiếp theo">
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- Upload Image Modal (FileReader enabled!) -->
    <div v-if="showUploadModal" class="modal-overlay" @click.self="closeUploadModal">
      <div class="modal-content upload-modal">
        <div class="modal-header">
          <h3><i class="fas fa-cloud-upload-alt text-primary"></i> Tải lên hình ảnh mới</h3>
          <button class="btn-close" @click="closeUploadModal">&times;</button>
        </div>
        
        <div class="modal-body">
          <form @submit.prevent="startUpload">
            <!-- Grid for preview and form -->
            <div class="upload-modal-grid">
              
              <!-- Left side: Drag and Drop -->
              <div class="upload-dropzone-wrapper">
                <div 
                  class="dropzone"
                  :class="{ dragover: isDragOver, 'has-file': uploadFile }"
                  @dragover.prevent="onDragOver"
                  @dragleave="onDragLeave"
                  @drop.prevent="onDrop"
                  @click="triggerFileInput"
                >
                  <input 
                    type="file" 
                    ref="fileInput" 
                    class="hidden-file-input" 
                    accept="image/*" 
                    @change="onFileSelected"
                  />
                  
                  <div class="dropzone-content" v-if="!uploadFilePreview">
                    <div class="upload-icon">
                      <i class="fas fa-cloud-upload-alt"></i>
                    </div>
                    <h4>Kéo và thả file ảnh vào đây</h4>
                    <p>Hoặc nhấp vào để duyệt file từ máy tính</p>
                    <span class="file-limits">Định dạng hỗ trợ: JPG, PNG, GIF, WEBP (Tối đa 5MB)</span>
                  </div>

                  <div class="dropzone-preview" v-else>
                    <img :src="uploadFilePreview" alt="Upload Preview" />
                    <button type="button" class="btn-remove-file" @click.stop="clearUploadFile" title="Hủy ảnh này">
                      <i class="fas fa-times-circle"></i>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Right side: Metadata Form -->
              <div class="upload-meta-form">
                <div class="form-group">
                  <label>Tên hiển thị <span class="required">*</span></label>
                  <input 
                    type="text" 
                    v-model="uploadFormData.name" 
                    placeholder="Nhập tên mô tả cho ảnh..." 
                    required
                  />
                </div>

                <div class="form-group">
                  <label>Thể loại hình ảnh <span class="required">*</span></label>
                  <select v-model="uploadFormData.type" required>
                    <option value="product">Sản phẩm (Product)</option>
                    <option value="banner">Banner Quảng Cáo</option>
                    <option value="category">Danh mục sản phẩm</option>
                    <option value="avatar">Ảnh đại diện</option>
                  </select>
                </div>

                <div class="form-group">
                  <label>Sản phẩm/Đối tượng liên kết (Không bắt buộc)</label>
                  <input 
                    type="text" 
                    v-model="uploadFormData.assocText" 
                    placeholder="Ví dụ: Áo thun nam, Banner Trang chủ..." 
                  />
                  <small class="form-help">Giúp dễ dàng quản lý hình ảnh được dùng ở trang nào.</small>
                </div>
              </div>
            </div>

            <!-- Upload Progress Bar (Animated) -->
            <div v-if="isUploading" class="upload-progress-container">
              <div class="progress-info">
                <span><i class="fas fa-spinner fa-spin"></i> Đang tải lên hệ thống...</span>
                <span>{{ uploadProgress }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
              </div>
            </div>

            <!-- Actions Footer -->
            <div class="modal-footer">
              <button 
                type="button" 
                class="btn btn-secondary" 
                @click="closeUploadModal" 
                :disabled="isUploading"
              >
                Hủy
              </button>
              <button 
                type="submit" 
                class="btn btn-primary" 
                :disabled="!uploadFile || isUploading"
              >
                <span v-if="!isUploading"><i class="fas fa-cloud-upload-alt"></i> Tải ảnh lên</span>
                <span v-else>Đang xử lý...</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useNotification } from '../../composables/useNotification'
import { useProductStore } from '../../stores/productStore'

const { showNotification } = useNotification()
const productStore = useProductStore()

// Reference Pinia store images list
const images = computed(() => productStore.images)

// Filtering and Sorting States
const filters = ref({
  search: '',
  type: 'all',
  sort: 'newest'
})

// Selection for Bulk Actions
const selectedImages = ref([])

// Lightbox States
const showLightbox = ref(false)
const lightboxIndex = ref(0)

// Upload Modal States
const showUploadModal = ref(false)
const isUploading = ref(false)
const uploadProgress = ref(0)
const isDragOver = ref(false)

const fileInput = ref(null)
const uploadFile = ref(null)
const uploadFilePreview = ref(null)

const defaultFormData = {
  name: '',
  type: 'product',
  assocText: ''
}
const uploadFormData = ref({ ...defaultFormData })

// COMPUTED PROPERTIES
// Calculate storage space (Sum up files size in KB)
const formattedStorageUsed = computed(() => {
  let totalKB = 0
  images.value.forEach(img => {
    const numericSize = parseFloat(img.size)
    if (img.size.includes('KB')) {
      totalKB += numericSize
    } else if (img.size.includes('MB')) {
      totalKB += numericSize * 1024
    }
  })
  if (totalKB >= 1024) {
    return (totalKB / 1024).toFixed(1) + ' MB'
  }
  return totalKB.toFixed(0) + ' KB'
})

// Filter and Sort Images
const filteredImages = computed(() => {
  let result = [...images.value]

  // Filter by Type
  if (filters.value.type !== 'all') {
    result = result.filter(img => img.type === filters.value.type)
  }

  // Filter by Search Query
  if (filters.value.search) {
    const query = filters.value.search.toLowerCase()
    result = result.filter(img => 
      img.name.toLowerCase().includes(query) ||
      img.filename.toLowerCase().includes(query)
    )
  }

  // Sorting
  switch (filters.value.sort) {
    case 'newest':
      result.sort((a, b) => new Date(b.uploadedAt) - new Date(a.uploadedAt))
      break
    case 'oldest':
      result.sort((a, b) => new Date(a.uploadedAt) - new Date(b.uploadedAt))
      break
    case 'largest':
      result.sort((a, b) => parseSizeInKB(b.size) - parseSizeInKB(a.size))
      break
    case 'smallest':
      result.sort((a, b) => parseSizeInKB(a.size) - parseSizeInKB(b.size))
      break
    case 'name-asc':
      result.sort((a, b) => a.name.localeCompare(b.name, 'vi'))
      break
    case 'name-desc':
      result.sort((a, b) => b.name.localeCompare(a.name, 'vi'))
      break
  }

  return result
})

// Get Current Lightbox Image
const currentLightboxImage = computed(() => {
  return filteredImages.value[lightboxIndex.value] || null
})

// METHODS
// Count files by type
const countByType = (type) => {
  return images.value.filter(img => img.type === type).length
}

// Convert sizes "124 KB" or "2 MB" into raw KBs for sorting
const parseSizeInKB = (sizeStr) => {
  const num = parseFloat(sizeStr)
  if (sizeStr.includes('MB')) return num * 1024
  return num
}

// Format date into friendly string
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

// Reset filters
const resetFilters = () => {
  filters.value.search = ''
  filters.value.type = 'all'
  filters.value.sort = 'newest'
}

// Get display label and CSS badge class for Type
const getTypeName = (type) => {
  const names = {
    product: 'Sản phẩm',
    banner: 'Banner QC',
    category: 'Danh mục',
    avatar: 'Ảnh đại diện',
    other: 'Khác'
  }
  return names[type] || 'Khác'
}

const getTypeBadgeClass = (type) => {
  const classes = {
    product: 'badge-success',
    banner: 'badge-warning',
    category: 'badge-info',
    avatar: 'badge-danger',
    other: 'badge-secondary'
  }
  return classes[type] || 'badge-secondary'
}

// Selection handling
const isSelected = (id) => {
  return selectedImages.value.includes(id)
}

const toggleSelectAll = (e) => {
  if (e.target.checked) {
    selectedImages.value = filteredImages.value.map(img => img.id)
  } else {
    selectedImages.value = []
  }
}

const clearSelection = () => {
  selectedImages.value = []
}

// Delete handling
const confirmDelete = (id) => {
  if (confirm('Bạn có chắc chắn muốn xóa hình ảnh này không? Thao tác này không thể hoàn tác.')) {
    // If it was in lightbox, close it first
    if (showLightbox.value && currentLightboxImage.value?.id === id) {
      closeLightbox()
    }
    
    productStore.deleteImage(id)
    selectedImages.value = selectedImages.value.filter(sId => sId !== id)
    showNotification('Đã xóa hình ảnh thành công!', 'success')
  }
}

const confirmBulkDelete = () => {
  const count = selectedImages.value.length
  if (confirm(`Bạn có chắc chắn muốn xóa ${count} hình ảnh đã chọn không?`)) {
    selectedImages.value.forEach(id => {
      productStore.deleteImage(id)
    })
    clearSelection()
    showNotification(`Đã xóa thành công ${count} hình ảnh!`, 'success')
  }
}

// Copy URL Utility with feedback
const copyImageUrl = (url) => {
  navigator.clipboard.writeText(url).then(() => {
    showNotification('Đã sao chép đường dẫn hình ảnh vào bộ nhớ tạm!', 'info')
  }).catch(() => {
    showNotification('Không thể sao chép đường dẫn hình ảnh!', 'error')
  })
}

// Lightbox controller
const openLightbox = (index) => {
  lightboxIndex.value = index
  showLightbox.value = true
}

const closeLightbox = () => {
  showLightbox.value = false
}

const prevImage = () => {
  if (filteredImages.value.length === 0) return
  if (lightboxIndex.value > 0) {
    lightboxIndex.value--
  } else {
    lightboxIndex.value = filteredImages.value.length - 1
  }
}

const nextImage = () => {
  if (filteredImages.value.length === 0) return
  if (lightboxIndex.value < filteredImages.value.length - 1) {
    lightboxIndex.value++
  } else {
    lightboxIndex.value = 0
  }
}

const selectUrlInput = (e) => {
  e.target.select()
}

// Upload handlers
const openUploadModal = () => {
  uploadFormData.value = { ...defaultFormData }
  clearUploadFile()
  showUploadModal.value = true
}

const closeUploadModal = () => {
  if (isUploading.value) return
  showUploadModal.value = false
}

const triggerFileInput = () => {
  if (fileInput.value) {
    fileInput.value.click()
  }
}

const processSelectedFile = (file) => {
  if (!file) return
  if (!file.type.startsWith('image/')) {
    showNotification('Vui lòng chỉ tải lên file định dạng hình ảnh!', 'error')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    showNotification('File ảnh không được vượt quá 5MB!', 'error')
    return
  }

  uploadFile.value = file
  
  // Set default name based on file name (without extension)
  const baseName = file.name.replace(/\.[^/.]+$/, "")
  uploadFormData.value.name = baseName.replace(/[-_]/g, ' ')

  // FileReader to show a real previews
  const reader = new FileReader()
  reader.onload = (e) => {
    uploadFilePreview.value = e.target.result
  }
  reader.readAsDataURL(file)
}

const onFileSelected = (e) => {
  const files = e.target.files
  if (files.length > 0) {
    processSelectedFile(files[0])
  }
}

const clearUploadFile = () => {
  uploadFile.value = null
  uploadFilePreview.value = null
  if (fileInput.value) fileInput.value.value = ''
}

// Drag & Drop
const onDragOver = () => {
  isDragOver.value = true
}

const onDragLeave = () => {
  isDragOver.value = false
}

const onDrop = (e) => {
  isDragOver.value = false
  const files = e.dataTransfer.files
  if (files.length > 0) {
    processSelectedFile(files[0])
  }
}

// Save uploaded image (Animated flow)
const startUpload = () => {
  if (!uploadFile.value) return
  
  isUploading.value = true
  uploadProgress.value = 0

  const interval = setInterval(() => {
    uploadProgress.value += 10
    if (uploadProgress.value >= 100) {
      clearInterval(interval)
      
      // Compute formatted size and fake resolution
      const sizeKB = (uploadFile.value.size / 1024).toFixed(0)
      const sizeStr = sizeKB >= 1024 
        ? (sizeKB / 1024).toFixed(1) + ' MB' 
        : sizeKB + ' KB'
      
      // Push new image into standard structure using data URL from FileReader
      const newImg = {
        id: Date.now(),
        name: uploadFormData.value.name,
        filename: uploadFile.value.name,
        url: uploadFilePreview.value,
        type: uploadFormData.value.type,
        dimensions: '1200 x 1200 px', // Mocked resolution
        size: sizeStr,
        uploadedAt: new Date().toISOString().split('T')[0],
        productsAssoc: uploadFormData.value.assocText 
          ? [uploadFormData.value.assocText] 
          : []
      }

      productStore.addImage(newImg)
      isUploading.value = false
      showUploadModal.value = false
      showNotification('Đã tải hình ảnh lên kho tài nguyên thành công!', 'success')
    }
  }, 150)
}
</script>

<style scoped>
.admin-images {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Page Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  flex-wrap: wrap;
  gap: 15px;
}

.header-title h2 {
  font-size: 28px;
  color: var(--dark-gray);
  font-weight: 600;
  margin-bottom: 5px;
}

.subtitle {
  color: var(--medium-gray);
  font-size: 14px;
}

/* Stats Section */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stats-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: var(--shadow);
  display: flex;
  align-items: center;
  gap: 18px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stats-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.05);
}

.stats-icon {
  width: 54px;
  height: 54px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
  flex-shrink: 0;
}

.bg-red { background: linear-gradient(135deg, #ff6b6b, #e63946); }
.bg-blue { background: linear-gradient(135deg, #54a0ff, #2e86de); }
.bg-green { background: linear-gradient(135deg, #1dd1a1, #10ac84); }
.bg-yellow { background: linear-gradient(135deg, #ffb8b8, #ff9f43); }

.stats-info {
  display: flex;
  flex-direction: column;
}

.stats-label {
  font-size: 13px;
  color: var(--medium-gray);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stats-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--dark-gray);
  margin: 2px 0;
}

.stats-sub {
  font-size: 11px;
}

.text-success { color: #10ac84; }
.text-muted { color: var(--medium-gray); }

/* Filters Section */
.filters-section {
  background: white;
  padding: 16px 20px;
  border-radius: 10px;
  box-shadow: var(--shadow);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
}

.search-box {
  display: flex;
  align-items: center;
  background: #f1f2f6;
  border-radius: 8px;
  padding: 0 14px;
  flex: 1;
  min-width: 280px;
  max-width: 450px;
}

.search-box i {
  color: var(--medium-gray);
  margin-right: 10px;
}

.search-box input {
  border: none;
  background: transparent;
  padding: 11px 0;
  width: 100%;
  outline: none;
  font-size: 14px;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-controls select {
  padding: 11px 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: white;
  outline: none;
  font-size: 14px;
  cursor: pointer;
  min-width: 160px;
  transition: border-color 0.2s;
}

.filter-controls select:focus {
  border-color: var(--primary-red);
}

.btn-reset {
  background: #f1f2f6;
  color: var(--dark-gray);
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-reset:hover {
  background: #dee2e6;
  color: var(--primary-red);
}

/* Bulk Actions */
.bulk-actions {
  background: #fff5f5;
  border: 1px solid rgba(230, 57, 70, 0.15);
  padding: 12px 20px;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.bulk-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.text-primary {
  color: var(--primary-red);
}

.bulk-buttons {
  display: flex;
  gap: 10px;
}

.bulk-buttons .btn {
  padding: 8px 16px;
  font-size: 14px;
}

.btn-danger {
  background: var(--primary-red);
  color: white;
  border: none;
}

.btn-danger:hover {
  background: var(--dark-red);
}

/* Checkbox Styling */
.checkbox-container {
  display: block;
  position: relative;
  cursor: pointer;
  user-select: none;
  width: 18px;
  height: 18px;
}

.checkbox-container input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.checkmark {
  position: absolute;
  top: 0;
  left: 0;
  height: 18px;
  width: 18px;
  background-color: white;
  border: 2px solid var(--border-color);
  border-radius: 4px;
  transition: all 0.2s;
}

.checkbox-container:hover input ~ .checkmark {
  border-color: var(--primary-red);
}

.checkbox-container input:checked ~ .checkmark {
  background-color: var(--primary-red);
  border-color: var(--primary-red);
}

.checkmark:after {
  content: "";
  position: absolute;
  display: none;
}

.checkbox-container input:checked ~ .checkmark:after {
  display: block;
}

.checkbox-container .checkmark:after {
  left: 5px;
  top: 1px;
  width: 5px;
  height: 9px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

/* Gallery and Grid System */
.gallery-wrapper {
  background: white;
  border-radius: 12px;
  box-shadow: var(--shadow);
  padding: 20px;
}

.gallery-toolbar {
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.select-all-label {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: auto;
  height: auto;
}

.select-all-label .label-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--medium-gray);
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

/* Image Card Design */
.image-card {
  background: white;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: all 0.2s ease-in-out;
}

.image-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08);
  border-color: var(--primary-red);
}

.image-card.selected {
  border-color: var(--primary-red);
  background-color: #fffafb;
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.05);
}

/* Card Checkbox overlay */
.card-checkbox {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-card:hover .card-checkbox,
.image-card.selected .card-checkbox {
  opacity: 1;
}

.image-preview {
  height: 180px;
  background: #f1f2f6;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.image-card:hover .image-preview img {
  transform: scale(1.05);
}

.hover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-preview:hover .hover-overlay {
  opacity: 1;
}

.btn-preview-icon {
  background: white;
  color: var(--dark-gray);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 5px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.15);
}

/* Card Details */
.image-card-body {
  padding: 12px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.image-tags {
  margin-bottom: 6px;
}

.image-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dark-gray);
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.image-filename {
  font-size: 12px;
  color: var(--medium-gray);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: Consolas, monospace;
}

/* Card Footer */
.image-card-footer {
  padding: 10px 14px;
  background: #fafafa;
  border-top: 1px solid #f1f2f6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 10px;
  color: var(--medium-gray);
}

.card-actions {
  display: flex;
  gap: 6px;
}

.action-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.action-btn.copy {
  background: #e1f5fe;
  color: #0288d1;
}

.action-btn.copy:hover {
  background: #0288d1;
  color: white;
}

.action-btn.delete {
  background: #ffebee;
  color: #c62828;
}

.action-btn.delete:hover {
  background: #c62828;
  color: white;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 50px 20px;
  color: var(--medium-gray);
}

.empty-icon {
  font-size: 60px;
  color: #dcdde1;
  margin-bottom: 15px;
}

.empty-state h3 {
  font-size: 18px;
  color: var(--dark-gray);
  margin-bottom: 5px;
}

.empty-state p {
  font-size: 14px;
  max-width: 400px;
  margin: 0 auto;
}

.mt-3 {
  margin-top: 15px;
}

/* LIGHTBOX / DETAIL SLIDER VIEW */
.lightbox-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.85);
  backdrop-filter: blur(8px);
  z-index: 2500;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  animation: fadeIn 0.2s;
}

.lightbox-nav {
  background: rgba(255,255,255,0.1);
  color: white;
  border: none;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all 0.2s;
  flex-shrink: 0;
}

.lightbox-nav:hover {
  background: var(--primary-red);
  transform: scale(1.1);
}

.lightbox-container {
  display: grid;
  grid-template-columns: 1fr 340px;
  background: white;
  width: 100%;
  max-width: 1100px;
  height: 80vh;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 15px 40px rgba(0,0,0,0.3);
}

.lightbox-close {
  position: absolute;
  top: 15px;
  right: 15px;
  font-size: 28px;
  color: var(--medium-gray);
  background: white;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
  transition: all 0.2s;
  z-index: 10;
}

.lightbox-close:hover {
  color: var(--primary-red);
  transform: rotate(90deg);
}

.lightbox-main {
  background: #f5f6fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow: hidden;
}

.lightbox-main img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 4px;
}

.lightbox-sidebar {
  border-left: 1px solid var(--border-color);
  padding: 25px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.lightbox-sidebar .sidebar-header {
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 15px;
  margin-bottom: 20px;
}

.lightbox-sidebar .sidebar-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--dark-gray);
  margin-bottom: 8px;
}

.sidebar-body {
  flex: 1;
}

.info-group {
  margin-bottom: 16px;
}

.info-group label {
  display: block;
  font-size: 11px;
  text-transform: uppercase;
  color: var(--medium-gray);
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.info-group .value {
  font-size: 14px;
  color: var(--dark-gray);
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-group .value.font-bold {
  font-weight: 600;
}

.info-group .value.code {
  font-family: Consolas, monospace;
  background: #f1f2f6;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  word-break: break-all;
}

.info-group-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.assoc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.assoc-tag {
  background: #efebe9;
  color: #5d4037;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.url-group {
  margin-top: 10px;
}

.url-input-wrapper {
  display: flex;
  gap: 8px;
}

.url-input-wrapper input {
  flex: 1;
  background: #f1f2f6;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 11px;
  font-family: Consolas, monospace;
  outline: none;
  cursor: text;
}

.btn-sm {
  padding: 6px 12px !important;
  font-size: 12px !important;
}

.lightbox-sidebar .sidebar-footer {
  border-top: 1px solid var(--border-color);
  padding-top: 15px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

/* UPLOAD DIALOG MODAL */
.upload-modal {
  max-width: 780px !important;
}

.upload-modal-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.upload-dropzone-wrapper {
  display: flex;
  flex-direction: column;
}

.dropzone {
  border: 2px dashed #b2bec3;
  border-radius: 12px;
  height: 250px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  cursor: pointer;
  padding: 20px;
  transition: all 0.25s ease-in-out;
  background: #fafafa;
  position: relative;
}

.dropzone:hover,
.dropzone.dragover {
  border-color: var(--primary-red);
  background: #fff8f8;
}

.upload-icon {
  font-size: 48px;
  color: var(--medium-gray);
  margin-bottom: 12px;
  transition: transform 0.2s;
}

.dropzone:hover .upload-icon {
  transform: translateY(-5px);
  color: var(--primary-red);
}

.dropzone-content h4 {
  font-size: 16px;
  color: var(--dark-gray);
  margin-bottom: 6px;
}

.dropzone-content p {
  font-size: 13px;
  color: var(--medium-gray);
  margin-bottom: 10px;
}

.file-limits {
  font-size: 10px;
  color: var(--medium-gray);
  display: block;
}

.hidden-file-input {
  display: none;
}

.dropzone-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.dropzone-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 6px;
}

.btn-remove-file {
  position: absolute;
  top: -8px;
  right: -8px;
  font-size: 20px;
  color: var(--primary-red);
  background: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  box-shadow: 0 2px 5px rgba(0,0,0,0.2);
}

.btn-remove-file:hover {
  transform: scale(1.1);
}

.upload-meta-form {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-help {
  font-size: 11px;
  color: var(--medium-gray);
  margin-top: 4px;
  display: block;
}

/* Progress bar */
.upload-progress-container {
  margin-bottom: 15px;
  background: #f5f6fa;
  padding: 12px 15px;
  border-radius: 8px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 500;
  color: var(--dark-gray);
  margin-bottom: 8px;
}

.progress-bar {
  height: 8px;
  background: var(--border-color);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-red), var(--light-red));
  border-radius: 4px;
  transition: width 0.15s ease-in-out;
}

/* Animation */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease-out;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}

/* Responsive Media Queries */
@media (max-width: 992px) {
  .lightbox-container {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr 280px;
    height: 90vh;
  }
  
  .lightbox-sidebar {
    border-left: none;
    border-top: 1px solid var(--border-color);
    padding: 15px;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .btn-upload-trigger {
    width: 100%;
  }

  .filters-section {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    max-width: none;
  }

  .filter-controls {
    display: grid;
    grid-template-columns: 1fr 1fr auto;
    gap: 8px;
  }

  .filter-controls select {
    min-width: 0;
  }
  
  .upload-modal-grid {
    grid-template-columns: 1fr;
  }
  
  .dropzone {
    height: 180px;
  }
}
</style>
