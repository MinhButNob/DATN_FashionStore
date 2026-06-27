<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal-container">
      <div class="modal-header">
        <h3>Yêu cầu hoàn hàng</h3>
        <button class="close-btn" @click="close">✕</button>
      </div>

      <div class="modal-body">
        <!-- Thông tin đơn hàng -->
        <div class="order-info">
          <p><strong>Đơn hàng:</strong> #{{ order?.orderCode || order?.orderId }}</p>
          <p><strong>Trạng thái:</strong> {{ getStatusText(order?.orderStatus) }}</p>
        </div>

        <!-- Lý do hoàn hàng -->
        <div class="form-group">
          <label>Lý do hoàn hàng <span class="required">*</span></label>
          <select v-model="form.reason" class="form-control">
            <option value="">-- Chọn lý do --</option>
            <option value="Sản phẩm bị lỗi">Sản phẩm bị lỗi</option>
            <option value="Sản phẩm không đúng mô tả">Sản phẩm không đúng mô tả</option>
            <option value="Sản phẩm bị hỏng khi vận chuyển">Sản phẩm bị hỏng khi vận chuyển</option>
            <option value="Giao sai sản phẩm">Giao sai sản phẩm</option>
            <option value="Không còn nhu cầu sử dụng">Không còn nhu cầu sử dụng</option>
            <option value="Lý do khác">Lý do khác</option>
          </select>
        </div>

        <!-- Mô tả chi tiết -->
        <div class="form-group">
          <label>Mô tả chi tiết <span class="required">*</span></label>
          <textarea 
            v-model="form.description" 
            rows="4" 
            placeholder="Vui lòng mô tả chi tiết lý do hoàn hàng..."
            class="form-control"
          ></textarea>
        </div>

        <!-- Upload hình ảnh -->
        <div class="form-group">
          <label>Hình ảnh minh họa</label>
          <div 
            class="upload-area" 
            @dragover.prevent 
            @drop.prevent="handleDrop" 
            @click="$refs.fileInput.click()"
            :class="{ 'drag-over': isDragOver }"
          >
            <i class="fas fa-cloud-upload-alt"></i>
            <p>Kéo thả hoặc nhấn để tải ảnh lên</p>
            <p class="upload-hint">Hỗ trợ: JPG, PNG, WEBP (Tối đa 5MB mỗi ảnh)</p>
            <input 
              ref="fileInput"
              type="file"
              accept="image/*"
              multiple
              @change="handleFileSelect"
              style="display: none"
            />
          </div>

          <!-- Preview ảnh -->
          <div v-if="uploadedImages.length > 0" class="image-preview-list">
            <div 
              v-for="(img, index) in uploadedImages" 
              :key="index"
              class="image-preview-item"
            >
              <img :src="img.preview" :alt="'Hình ảnh ' + (index + 1)" />
              <button class="remove-image-btn" @click.stop="removeImage(index)">
                <i class="fas fa-times"></i>
              </button>
              <div v-if="img.uploading" class="uploading-overlay">
                <i class="fas fa-spinner fa-spin"></i>
              </div>
            </div>
          </div>

          <div v-if="uploadError" class="upload-error">
            <i class="fas fa-exclamation-circle"></i>
            {{ uploadError }}
          </div>
        </div>

        <div v-if="error" class="error-message">
          <i class="fas fa-exclamation-circle"></i>
          {{ error }}
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-outline" @click="close">Hủy</button>
        <button 
          class="btn btn-primary" 
          @click="submitReturnRequest" 
          :disabled="submitting || !form.reason || !form.description || isUploading"
        >
          <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
          {{ submitting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import axios from 'axios'

const props = defineProps({
  visible: Boolean,
  order: Object
})

const emit = defineEmits(['close', 'submitted'])

const API_BASE = "http://localhost:8080/api"

const form = ref({
  reason: '',
  description: ''
})

const submitting = ref(false)
const error = ref('')
const isUploading = ref(false)
const uploadError = ref('')
const isDragOver = ref(false)
const uploadedImages = ref([])
const fileInput = ref(null)

const getStatusText = (status) => {
  const map = {
    'PENDING_PAYMENT': 'Chờ thanh toán',
    'CONFIRMED': 'Đã xác nhận',
    'PROCESSING': 'Đang xử lý',
    'SHIPPING': 'Đang giao hàng',
    'DELIVERED': 'Đã giao hàng',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy'
  }
  return map[status] || status
}

// Upload ảnh lên server
const uploadImage = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const token = localStorage.getItem('token')
    const response = await axios.post(`${API_BASE}/reviews/upload-image`, formData, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data.url
  } catch (error) {
    console.error('❌ Upload image error:', error)
    throw error
  }
}

// Xử lý chọn file
const handleFileSelect = async (event) => {
  const files = event.target.files
  if (files.length === 0) return

  uploadError.value = ''
  isUploading.value = true

  const maxImages = 5
  const currentCount = uploadedImages.value.length
  const remainingSlots = maxImages - currentCount

  if (remainingSlots <= 0) {
    uploadError.value = 'Chỉ được tải tối đa 5 ảnh'
    isUploading.value = false
    return
  }

  const filesToUpload = Array.from(files).slice(0, remainingSlots)

  for (let i = 0; i < filesToUpload.length; i++) {
    const file = filesToUpload[i]
    
    if (file.size > 5 * 1024 * 1024) {
      uploadError.value = `File ${file.name} vượt quá 5MB`
      continue
    }

    if (!file.type.startsWith('image/')) {
      uploadError.value = `File ${file.name} không phải là hình ảnh`
      continue
    }

    const imageItem = {
      file: file,
      preview: URL.createObjectURL(file),
      uploading: true,
      url: null
    }
    uploadedImages.value.push(imageItem)

    try {
      const url = await uploadImage(file)
      imageItem.url = url
      imageItem.uploading = false
      console.log('✅ Uploaded image:', url)
    } catch (err) {
      imageItem.uploading = false
      uploadError.value = `Không thể tải ảnh ${file.name}`
      const index = uploadedImages.value.indexOf(imageItem)
      if (index > -1) {
        uploadedImages.value.splice(index, 1)
      }
    }
  }

  isUploading.value = false
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// Xử lý drag and drop
const handleDrop = async (event) => {
  isDragOver.value = false
  const files = event.dataTransfer.files
  if (files.length === 0) return

  const fakeEvent = { target: { files: files } }
  await handleFileSelect(fakeEvent)
}

// Xóa ảnh
const removeImage = (index) => {
  const image = uploadedImages.value[index]
  if (image.preview) {
    URL.revokeObjectURL(image.preview)
  }
  uploadedImages.value.splice(index, 1)
  uploadError.value = ''
}

// Gửi yêu cầu hoàn hàng
const submitReturnRequest = async () => {
  if (!form.value.reason) {
    error.value = 'Vui lòng chọn lý do hoàn hàng'
    return
  }

  if (!form.value.description.trim()) {
    error.value = 'Vui lòng nhập mô tả chi tiết'
    return
  }

  const uploadingImages = uploadedImages.value.filter(img => img.uploading)
  if (uploadingImages.length > 0) {
    error.value = 'Đang tải ảnh lên, vui lòng đợi...'
    return
  }

  submitting.value = true
  error.value = ''

  try {
    const token = localStorage.getItem('token')
    
    const imageUrls = uploadedImages.value
      .filter(img => img.url)
      .map(img => img.url)

    const payload = {
      reason: form.value.reason,
      description: form.value.description.trim(),
      images: imageUrls
    }

    console.log('📤 Submitting return request:', payload)

    const response = await axios.post(
      `${API_BASE}/orders/${props.order.orderId}/return-request`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )

    console.log('✅ Return request submitted:', response.data)

    emit('submitted', response.data)
    close()
  } catch (err) {
    console.error('❌ Error submitting return request:', err)
    console.error('❌ Error response:', err.response?.data)
    
    const message = err.response?.data?.message || 'Không thể gửi yêu cầu hoàn hàng'
    error.value = message
  } finally {
    submitting.value = false
  }
}

const close = () => {
  form.value = {
    reason: '',
    description: ''
  }
  error.value = ''
  uploadError.value = ''
  uploadedImages.value.forEach(img => {
    if (img.preview) {
      URL.revokeObjectURL(img.preview)
    }
  })
  uploadedImages.value = []
  emit('close')
}

// Reset khi modal mở
watch(() => props.visible, (newVal) => {
  if (newVal && props.order) {
    form.value = {
      reason: '',
      description: ''
    }
    error.value = ''
    uploadError.value = ''
    uploadedImages.value = []
    isUploading.value = false
  }
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.modal-container {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 550px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideIn 0.3s;
}

@keyframes slideIn {
  from { transform: translateY(-30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  background: white;
  border-radius: 16px 16px 0 0;
  z-index: 1;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  color: #2c3e50;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  transition: all 0.3s;
}

.close-btn:hover {
  color: #e63946;
  transform: rotate(90deg);
}

.modal-body {
  padding: 24px;
}

.order-info {
  background: #f8f9fa;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.order-info p {
  margin: 4px 0;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.required {
  color: #e63946;
}

.form-control {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
  font-family: inherit;
}

.form-control:focus {
  outline: none;
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

textarea.form-control {
  resize: vertical;
  min-height: 100px;
}

.upload-area {
  border: 2px dashed #ddd;
  border-radius: 10px;
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
}

.upload-area:hover {
  border-color: #e63946;
  background: #f5f5f5;
}

.upload-area.drag-over {
  border-color: #e63946;
  background: #fff5f5;
}

.upload-area i {
  font-size: 40px;
  color: #ccc;
  margin-bottom: 10px;
}

.upload-area p {
  color: #666;
  margin: 5px 0;
}

.upload-area .upload-hint {
  font-size: 12px;
  color: #999;
}

.image-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 15px;
}

.image-preview-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e9ecef;
}

.image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 24px;
  height: 24px;
  background: #e63946;
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  transition: all 0.3s;
}

.remove-image-btn:hover {
  transform: scale(1.1);
}

.uploading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.upload-error {
  margin-top: 10px;
  padding: 8px 12px;
  background: #fee;
  color: #dc3545;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.error-message {
  padding: 12px 16px;
  background: #fee;
  color: #dc3545;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  margin-top: 15px;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-outline {
  background: #f8f9fa;
  color: #666;
  border: 1px solid #ddd;
}

.btn-outline:hover {
  background: #e9ecef;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c62828;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(230, 57, 70, 0.3);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

@media (max-width: 576px) {
  .modal-container {
    max-width: 100%;
    margin: 10px;
  }
}
</style>