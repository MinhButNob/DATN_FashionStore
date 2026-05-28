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

            <div class="form-group">
              <label>Icon</label>
              <select v-model="form.image">
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'

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
</style>