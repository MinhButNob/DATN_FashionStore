<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal-container modal-lg">
      <div class="modal-header">
        <h3>Yêu cầu hoàn hàng</h3>
        <button class="close-btn" @click="close">✕</button>
      </div>

      <div class="modal-body">
        <!-- Thông tin đơn hàng -->
        <div class="order-summary" v-if="order">
          <h4>Đơn hàng #{{ order.orderCode || order.orderId }}</h4>
          <p>Ngày đặt: {{ formatDate(order.orderDate) }}</p>
          <p>Tổng tiền: {{ formatPrice(order.totalAmount) }}</p>
        </div>

        <!-- ✅ Chọn sản phẩm cần hoàn -->
        <div class="form-group">
          <div class="selection-actions">
            <label>Chọn sản phẩm cần hoàn <span class="required">*</span></label>
            <div class="action-buttons">
              <button type="button" class="btn btn-sm btn-outline" @click="selectAll">
                <i class="fas fa-check-double"></i> Chọn tất cả
              </button>
              <button type="button" class="btn btn-sm btn-outline" @click="deselectAll">
                <i class="fas fa-times"></i> Bỏ chọn tất cả
              </button>
            </div>
          </div>
          
          <div class="product-select-list">
            <div v-for="item in order?.items" :key="item.productDetailId" class="product-select-item">
              <label class="checkbox-label">
                <input 
                  type="checkbox" 
                  :value="item.productDetailId" 
                  v-model="selectedProductIds"
                  @change="onProductSelect(item)"
                />
                <div class="product-info">
                  <div class="product-image">
                    <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" />
                    <i v-else class="fas fa-tshirt"></i>
                  </div>
                  <div class="product-details">
                    <span class="product-name">{{ item.productName }}</span>
                    <span class="product-variant">{{ item.colorName || '' }} {{ item.sizeName || '' }}</span>
                    <span class="product-price">{{ formatPrice(item.unitPrice) }}</span>
                    <span class="product-quantity">Đã mua: {{ item.quantity }}</span>
                  </div>
                </div>
              </label>
              <div class="quantity-select" v-if="selectedProductIds.includes(item.productDetailId)">
                <label>Số lượng hoàn:</label>
                <input 
                  type="number" 
                  :min="1" 
                  :max="item.quantity" 
                  v-model.number="quantities[item.productDetailId]" 
                  @input="validateQuantity(item.productDetailId, item.quantity)"
                />
                <span class="max-quantity">(Tối đa: {{ item.quantity }})</span>
              </div>
            </div>
          </div>
          
          <!-- Hiển thị số lượng sản phẩm đã chọn -->
          <div class="selection-summary" v-if="selectedProductIds.length > 0">
            <span class="selected-count">
              <i class="fas fa-check-circle"></i>
              Đã chọn <strong>{{ selectedProductIds.length }}</strong> sản phẩm
            </span>
            <button type="button" class="btn btn-sm btn-danger" @click="deselectAll">
              <i class="fas fa-times"></i> Bỏ chọn tất cả
            </button>
          </div>
          
          <small v-if="selectedProductIds.length === 0" class="error-text">Vui lòng chọn ít nhất 1 sản phẩm</small>
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

        <!-- Thông tin tổng quan -->
        <div class="return-summary" v-if="selectedProductIds.length > 0">
          <h4>Thông tin hoàn hàng</h4>
          <div class="summary-row">
            <span>Số sản phẩm hoàn:</span>
            <span><strong>{{ selectedProductIds.length }}</strong> sản phẩm</span>
          </div>
          <div class="summary-row">
            <span>Tổng số lượng hoàn:</span>
            <span><strong>{{ totalReturnQuantity }}</strong> sản phẩm</span>
          </div>
          <div class="summary-row total">
            <span>Tổng tiền hoàn:</span>
            <span class="total-amount">{{ formatPrice(totalRefundAmount) }}</span>
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
          :disabled="submitting || selectedProductIds.length === 0 || !form.reason || !form.description || isUploading"
        >
          <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
          {{ submitting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import api from "@/api/axios";

const props = defineProps({
  visible: Boolean,
  order: Object
})

const emit = defineEmits(['close', 'submitted'])

const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");

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

// ✅ State cho chọn sản phẩm
const selectedProductIds = ref([])
const quantities = ref({})

// ===== COMPUTED =====
const totalReturnQuantity = computed(() => {
  let total = 0
  for (const pdId of selectedProductIds.value) {
    total += quantities.value[pdId] || 0
  }
  return total
})

const totalRefundAmount = computed(() => {
  let total = 0
  if (!props.order?.items) return 0
  
  for (const pdId of selectedProductIds.value) {
    const qty = quantities.value[pdId] || 0
    const item = props.order.items.find(i => i.productDetailId === pdId)
    if (item) {
      total += item.unitPrice * qty
    }
  }
  return total
})

// ===== HELPER =====
const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

const formatPrice = (price) => {
  if (!price && price !== 0) return '0₫'
  const num = Number(price)
  if (isNaN(num)) return '0₫'
  return num.toLocaleString('vi-VN') + '₫'
}

const formatDate = (dateString) => {
  if (!dateString) return '---'
  const date = new Date(dateString)
  if (isNaN(date.getTime())) return '---'
  return date.toLocaleDateString('vi-VN') + ' ' + 
         date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

// ===== PRODUCT SELECTION =====
const onProductSelect = (item) => {
  if (selectedProductIds.value.includes(item.productDetailId)) {
    // Nếu chọn, set số lượng mặc định = 1 (hoặc max nếu quantity = 1)
    const defaultQty = Math.min(item.quantity, 1)
    quantities.value[item.productDetailId] = defaultQty > 0 ? defaultQty : 1
  } else {
    // Nếu bỏ chọn, xóa khỏi quantities
    delete quantities.value[item.productDetailId]
  }
}

const validateQuantity = (productDetailId, maxQty) => {
  if (quantities.value[productDetailId] > maxQty) {
    quantities.value[productDetailId] = maxQty
  }
  if (quantities.value[productDetailId] < 1) {
    quantities.value[productDetailId] = 1
  }
}

// ✅ Chọn tất cả sản phẩm
const selectAll = () => {
  if (!props.order?.items) return
  
  selectedProductIds.value = props.order.items.map(item => item.productDetailId)
  
  // Set số lượng mặc định cho từng sản phẩm
  props.order.items.forEach(item => {
    quantities.value[item.productDetailId] = item.quantity
  })
}

// ✅ Bỏ chọn tất cả
const deselectAll = () => {
  selectedProductIds.value = []
  quantities.value = {}
}

// ===== UPLOAD IMAGES =====
const uploadImage = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const token = localStorage.getItem('token')
    const response = await api.post(`/reviews/upload-image`, formData, {
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

const handleDrop = async (event) => {
  isDragOver.value = false
  const files = event.dataTransfer.files
  if (files.length === 0) return

  const fakeEvent = { target: { files: files } }
  await handleFileSelect(fakeEvent)
}

const removeImage = (index) => {
  const image = uploadedImages.value[index]
  if (image.preview) {
    URL.revokeObjectURL(image.preview)
  }
  uploadedImages.value.splice(index, 1)
  uploadError.value = ''
}

// ===== SUBMIT =====
const submitReturnRequest = async () => {
  if (selectedProductIds.value.length === 0) {
    error.value = 'Vui lòng chọn ít nhất 1 sản phẩm để hoàn'
    return
  }

  // Kiểm tra tất cả sản phẩm đều có số lượng hợp lệ
  for (const pdId of selectedProductIds.value) {
    const qty = quantities.value[pdId]
    if (!qty || qty <= 0) {
      error.value = 'Vui lòng nhập số lượng hoàn cho tất cả sản phẩm đã chọn'
      return
    }
  }

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

    // ✅ Gửi productDetailIds và quantities
    const payload = {
      reason: form.value.reason,
      description: form.value.description.trim(),
      images: imageUrls,
      productDetailIds: selectedProductIds.value,
      quantities: quantities.value
    }

    console.log('📤 Submitting return request:', payload)

    const response = await api.post(
      `/orders/${props.order.orderId}/return-request`,
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
  selectedProductIds.value = []
  quantities.value = {}
  
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
    selectedProductIds.value = []
    quantities.value = {}
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
  max-width: 650px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideIn 0.3s;
}

.modal-container.modal-lg {
  max-width: 700px;
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

.order-summary {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 10px;
  margin-bottom: 20px;
}

.order-summary h4 {
  margin: 0 0 5px 0;
  color: #1a1a2e;
}

.order-summary p {
  margin: 3px 0;
  color: #6c757d;
  font-size: 14px;
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

/* ✅ Selection Actions */
.selection-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.selection-actions label {
  margin-bottom: 0;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.btn-sm {
  padding: 4px 12px;
  font-size: 12px;
}

/* ✅ Product Select List */
.product-select-list {
  border: 1px solid #e9ecef;
  border-radius: 10px;
  overflow: hidden;
}

.product-select-item {
  padding: 12px 15px;
  border-bottom: 1px solid #f1f3f5;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-select-item:last-child {
  border-bottom: none;
}

.product-select-item .checkbox-label {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 5px 0;
}

.product-select-item .checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: #e63946;
  cursor: pointer;
  flex-shrink: 0;
}

.product-select-item .product-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.product-select-item .product-image {
  width: 50px;
  height: 50px;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.product-select-item .product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-select-item .product-image i {
  font-size: 24px;
  color: #ccc;
}

.product-select-item .product-details {
  flex: 1;
}

.product-select-item .product-name {
  font-weight: 600;
  color: #1a1a2e;
  display: block;
}

.product-select-item .product-variant {
  font-size: 12px;
  color: #999;
  display: block;
}

.product-select-item .product-price {
  font-weight: 600;
  color: #e63946;
  font-size: 14px;
  display: block;
}

.product-select-item .product-quantity {
  font-size: 12px;
  color: #6c757d;
  display: block;
}

.quantity-select {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 30px;
}

.quantity-select label {
  font-size: 13px;
  color: #6c757d;
  margin: 0;
  font-weight: 500;
}

.quantity-select input[type="number"] {
  width: 70px;
  padding: 6px 10px;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  text-align: center;
}

.quantity-select .max-quantity {
  font-size: 12px;
  color: #6c757d;
}

/* ✅ Selection Summary */
.selection-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #e8f5e9;
  border-radius: 8px;
  margin-top: 10px;
}

.selection-summary .selected-count {
  color: #2e7d32;
  font-size: 14px;
}

.selection-summary .selected-count i {
  margin-right: 6px;
}

.error-text {
  color: #e63946;
  font-size: 13px;
  margin-top: 5px;
  display: block;
}

/* ✅ Return Summary */
.return-summary {
  background: #fff3cd;
  padding: 15px 20px;
  border-radius: 10px;
  border-left: 4px solid #ffc107;
  margin-top: 10px;
}

.return-summary h4 {
  margin: 0 0 12px 0;
  color: #856404;
  font-size: 15px;
}

.return-summary .summary-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 14px;
  color: #555;
}

.return-summary .summary-row.total {
  border-top: 1px solid #e8d5a0;
  margin-top: 8px;
  padding-top: 10px;
  font-size: 16px;
  font-weight: 600;
}

.return-summary .summary-row .total-amount {
  color: #e63946;
  font-size: 18px;
}

/* Upload Area */
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

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

@media (max-width: 576px) {
  .modal-container {
    max-width: 100%;
    margin: 10px;
  }
  
  .product-select-item .product-info {
    flex-wrap: wrap;
  }
  
  .quantity-select {
    flex-wrap: wrap;
  }
  
  .selection-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .action-buttons {
    justify-content: stretch;
  }
  
  .action-buttons .btn {
    flex: 1;
  }
  
  .selection-summary {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
}
</style>