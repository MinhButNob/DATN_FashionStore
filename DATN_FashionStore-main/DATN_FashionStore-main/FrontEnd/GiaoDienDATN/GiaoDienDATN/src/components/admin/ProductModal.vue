<template>
  <div class="modal">
    <div class="modal-content">
      <div class="modal-header">
        <h2>{{ product ? 'Sửa sản phẩm' : 'Thêm sản phẩm mới' }}</h2>
        <button class="close-btn" @click="$emit('close')">
          <i class="fas fa-times"></i>
        </button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <!-- Tên sản phẩm -->
          <div class="form-group">
            <label>Tên sản phẩm *</label>
            <input 
              type="text" 
              v-model="form.name" 
              required
              placeholder="Nhập tên sản phẩm"
            >
          </div>

          <!-- Danh mục và Icon -->
          <div class="form-row">
            <div class="form-group">
              <label>Danh mục *</label>
              <select v-model="form.category" required>
                <option value="men">Thời trang nam</option>
                <option value="women">Thời trang nữ</option>
                <option value="kids">Trẻ em</option>
                <option value="accessories">Phụ kiện</option>
              </select>
            </div>

            <div class="form-group image-select-group">
              <label>Hình ảnh / Biểu tượng hiển thị *</label>
              <div class="image-select-control">
                <div class="preview-thumbnail">
                  <img v-if="isUrl(form.image)" :src="form.image" alt="Preview" class="thumbnail-img" />
                  <i v-else :class="['fas', `fa-${form.image}`, 'thumbnail-icon']"></i>
                </div>
                <div class="select-actions">
                  <select v-model="form.image" class="icon-selector-dropdown">
                    <option disabled value="">Chọn biểu tượng mặc định</option>
                    <option value="tshirt">Áo thun</option>
                    <option value="dress">Đầm</option>
                    <option value="jeans">Quần jean</option>
                    <option value="jacket">Áo khoác</option>
                    <option value="shorts">Quần short</option>
                    <option value="bag">Túi xách</option>
                    <option value="shirt">Sơ mi</option>
                    <option value="skirt">Chân váy</option>
                    <option value="hat">Mũ</option>
                    <option value="sweater">Áo len</option>
                    <option value="pants">Quần tây</option>
                  </select>
                  <button type="button" class="btn btn-outline btn-choose-gallery" @click="openLibraryModal">
                    <i class="fas fa-images"></i> Chọn từ thư viện ảnh
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Giá -->
          <div class="form-row">
            <div class="form-group">
              <label>Giá *</label>
              <input 
                type="number" 
                v-model="form.price" 
                required
                min="0"
                placeholder="0"
              >
            </div>

            <div class="form-group">
              <label>Giá cũ</label>
              <input 
                type="number" 
                v-model="form.oldPrice" 
                min="0"
                placeholder="0"
              >
            </div>
          </div>

          <!-- Kho và Đã bán -->
          <div class="form-row">
            <div class="form-group">
              <label>Số lượng kho</label>
              <input 
                type="number" 
                v-model="form.stock" 
                min="0"
                placeholder="100"
              >
            </div>

            <div class="form-group">
              <label>Đã bán</label>
              <input 
                type="number" 
                v-model="form.sold" 
                min="0"
                placeholder="0"
              >
            </div>
          </div>

          <!-- Đánh giá và Tag -->
          <div class="form-row">
            <div class="form-group">
              <label>Đánh giá</label>
              <input 
                type="number" 
                v-model="form.rating" 
                step="0.1"
                min="0"
                max="5"
                placeholder="4.5"
              >
            </div>

            <div class="form-group">
              <label>Tag</label>
              <select v-model="form.tag">
                <option value="">Không có</option>
                <option value="Hot">Hot</option>
                <option value="Sale">Sale</option>
                <option value="Mới">Mới</option>
              </select>
            </div>
          </div>

          <!-- Mô tả -->
          <div class="form-group">
            <label>Mô tả</label>
            <textarea 
              v-model="form.description" 
              rows="4"
              placeholder="Nhập mô tả sản phẩm"
            ></textarea>
          </div>

          <!-- Trạng thái -->
          <div class="form-group">
            <label>
              <input type="checkbox" v-model="form.status">
              Đang bán
            </label>
          </div>

          <!-- Upload hình ảnh -->
          <div class="form-group">
            <label>Hình ảnh sản phẩm</label>
            <div class="image-upload" @click="triggerFileInput">
              <i class="fas fa-cloud-upload-alt"></i>
              <p>Click để upload hình ảnh</p>
              <p class="upload-hint">Hỗ trợ JPG, PNG (tối đa 5MB)</p>
            </div>
            <input 
              type="file" 
              ref="fileInput" 
              style="display: none"
              multiple
              accept="image/*"
              @change="handleFileUpload"
            >
            
            <div class="image-preview" v-if="imagePreviews.length > 0">
              <div v-for="(preview, index) in imagePreviews" :key="index" class="preview-item">
                <img :src="preview" :alt="`Preview ${index + 1}`">
                <button class="remove-image" @click="removeImage(index)">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary">
              {{ product ? 'Cập nhật' : 'Thêm mới' }}
            </button>
            <button type="button" class="btn btn-outline" @click="$emit('close')">
              Hủy
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- System Image Library Modal -->
    <div v-if="showLibraryModal" class="library-modal-overlay" @click.self="closeLibraryModal">
      <div class="library-modal-content">
        <div class="library-modal-header">
          <h3>Chọn hình ảnh từ Thư viện</h3>
          <button type="button" class="btn-close" @click="closeLibraryModal">&times;</button>
        </div>
        <div class="library-modal-body">
          <div class="library-search">
            <i class="fas fa-search"></i>
            <input 
              type="text" 
              v-model="searchQuery" 
              placeholder="Tìm kiếm hình ảnh trong thư viện..." 
            />
          </div>
          
          <div class="library-grid">
            <div 
              v-for="img in filteredLibraryImages" 
              :key="img.id" 
              class="library-card"
              :class="{ active: form.image === img.url }"
              @click="selectImageFromLibrary(img.url)"
            >
              <div class="library-card-preview">
                <img :src="img.url" :alt="img.name" />
                <div class="active-badge" v-if="form.image === img.url">
                  <i class="fas fa-check-circle"></i>
                </div>
              </div>
              <div class="library-card-info">
                <h4 class="title" :title="img.name">{{ img.name }}</h4>
                <span class="meta">{{ img.dimensions }} | {{ img.size }}</span>
              </div>
            </div>
            
            <div v-if="filteredLibraryImages.length === 0" class="empty-library">
              <i class="fas fa-images"></i>
              <p>Không có hình ảnh sản phẩm nào trong thư viện.</p>
              <router-link to="/admin/images" class="btn btn-primary btn-sm mt-2" @click="closeLibraryModal">
                Quản lý thư viện ảnh
              </router-link>
            </div>
          </div>
        </div>
        <div class="library-modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeLibraryModal">Đóng</button>
          <button type="button" class="btn btn-primary" @click="closeLibraryModal">
            Xác nhận
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useProductStore } from '../../stores/productStore'

const productStore = useProductStore()

const props = defineProps({
  product: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'save'])

// Form state
const form = reactive({
  name: '',
  category: 'men',
  image: 'tshirt',
  price: '',
  oldPrice: '',
  stock: 100,
  sold: 0,
  rating: 4.5,
  tag: '',
  description: '',
  status: true
})

const fileInput = ref(null)
const imagePreviews = ref([])

// Image Library Modal States
const showLibraryModal = ref(false)
const searchQuery = ref('')

const isUrl = (str) => {
  if (!str) return false
  return str.startsWith('http') || str.startsWith('data:image')
}

const openLibraryModal = () => {
  searchQuery.value = ''
  showLibraryModal.value = true
}

const closeLibraryModal = () => {
  showLibraryModal.value = false
}

const selectImageFromLibrary = (url) => {
  form.image = url
}

const filteredLibraryImages = computed(() => {
  // Show product or category or other uploaded images
  let list = productStore.images.filter(img => img.type === 'product' || img.type === 'category' || img.type === 'other')
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(img => 
      img.name.toLowerCase().includes(q) || 
      img.filename.toLowerCase().includes(q)
    )
  }
  return list
})

// Initialize form with product data if editing
onMounted(() => {
  if (props.product) {
    Object.assign(form, props.product)
    form.status = props.product.status === 'active'
  }
})

const handleSubmit = () => {
  const productData = {
    ...form,
    price: parseInt(form.price),
    oldPrice: form.oldPrice ? parseInt(form.oldPrice) : null,
    stock: parseInt(form.stock),
    sold: parseInt(form.sold),
    rating: parseFloat(form.rating),
    status: form.status ? 'active' : 'inactive',
    images: imagePreviews.value // In real app, this would be uploaded files
  }
  
  emit('save', productData)
}

const triggerFileInput = () => {
  fileInput.value.click()
}

const handleFileUpload = (event) => {
  const files = Array.from(event.target.files)
  
  files.forEach(file => {
    if (file.size > 5 * 1024 * 1024) {
      alert(`File ${file.name} vượt quá 5MB`)
      return
    }
    
    const reader = new FileReader()
    reader.onload = (e) => {
      imagePreviews.value.push(e.target.result)
    }
    reader.readAsDataURL(file)
  })
}

const removeImage = (index) => {
  imagePreviews.value.splice(index, 1)
}
</script>

<style scoped>
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
  font-size: 14px;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.form-group input[type="checkbox"] {
  width: auto;
  margin-right: 8px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

/* Image Upload */
.image-upload {
  border: 2px dashed var(--border-color);
  padding: 30px;
  text-align: center;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  background: var(--light-gray);
}

.image-upload:hover {
  border-color: var(--primary-red);
  background: rgba(230, 57, 70, 0.05);
}

.image-upload i {
  font-size: 40px;
  color: var(--medium-gray);
  margin-bottom: 10px;
}

.image-upload p {
  color: var(--dark-gray);
  margin-bottom: 5px;
}

.upload-hint {
  font-size: 12px;
  color: var(--medium-gray);
}

.image-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 10px;
  margin-top: 15px;
}

.preview-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image {
  position: absolute;
  top: 3px;
  right: 3px;
  width: 20px;
  height: 20px;
  background: rgba(220, 53, 69, 0.9);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  transition: all 0.3s;
}

.remove-image:hover {
  background: #dc3545;
  transform: scale(1.1);
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.btn-primary,
.btn-outline {
  padding: 12px 25px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
}

.btn-primary:hover {
  background: linear-gradient(135deg, var(--dark-red), var(--primary-red));
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.3);
}

.btn-outline {
  background: white;
  border: 1px solid var(--border-color);
  color: var(--medium-gray);
}

.btn-outline:hover {
  background: var(--light-gray);
}

/* Animations */
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
  .form-row {
    grid-template-columns: 1fr;
  }

  .modal-content {
    width: 95%;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions button {
    width: 100%;
  }
}

/* Image Select Group Styling */
.image-select-control {
  display: flex;
  gap: 15px;
  align-items: center;
  margin-top: 5px;
}

.preview-thumbnail {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: var(--light-gray);
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-icon {
  font-size: 28px;
  color: var(--medium-gray);
}

.select-actions {
  display: flex;
  gap: 10px;
  flex: 1;
}

.icon-selector-dropdown {
  flex: 1;
  min-width: 150px;
}

.btn-choose-gallery {
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 15px !important;
  font-size: 13px !important;
}

/* Library Modal Overlay */
.library-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(5px);
  animation: fadeIn 0.2s;
}

.library-modal-content {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 700px;
  height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 25px rgba(0,0,0,0.15);
  overflow: hidden;
}

.library-modal-header {
  padding: 15px 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.library-modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--dark-gray);
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--medium-gray);
  cursor: pointer;
}

.btn-close:hover {
  color: var(--primary-red);
}

.library-modal-body {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.library-search {
  position: relative;
  display: flex;
  align-items: center;
}

.library-search i {
  position: absolute;
  left: 12px;
  color: var(--medium-gray);
}

.library-search input {
  width: 100%;
  padding: 10px 10px 10px 35px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
}

.library-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 15px;
  flex: 1;
  overflow-y: auto;
  align-content: start;
}

.library-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
}

.library-card:hover {
  border-color: var(--primary-red);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.05);
}

.library-card.active {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 2px rgba(230, 57, 70, 0.2);
}

.library-card-preview {
  height: 100px;
  position: relative;
  background: #f8f9fa;
}

.library-card-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.active-badge {
  position: absolute;
  top: 5px;
  right: 5px;
  color: var(--primary-red);
  font-size: 18px;
  background: white;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.library-card-info {
  padding: 8px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.library-card-info .title {
  font-size: 12px;
  font-weight: 600;
  margin: 0 0 3px 0;
  color: var(--dark-gray);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.library-card-info .meta {
  font-size: 9px;
  color: var(--medium-gray);
}

.empty-library {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px 20px;
  color: var(--medium-gray);
}

.empty-library i {
  font-size: 40px;
  margin-bottom: 10px;
  color: #dee2e6;
}

.library-modal-footer {
  padding: 15px 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>