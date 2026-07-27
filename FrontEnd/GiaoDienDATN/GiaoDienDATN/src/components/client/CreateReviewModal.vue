<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal-container">
      <div class="modal-header">
        <h3>Đánh giá sản phẩm</h3>
        <button class="close-btn" @click="close">✕</button>
      </div>

      <div class="modal-body">
        <!-- Product Info -->
        <div class="product-info">
          <div class="product-image">
            <img v-if="product?.imageUrl" :src="getImageUrl(product.imageUrl)" :alt="product.productName" />
            <i v-else class="fas fa-tshirt"></i>
          </div>
          <div class="product-details">
            <h4>{{ product?.productName }}</h4>
            <p class="product-variant">
              {{ product?.sizeName || '' }} {{ product?.colorName || '' }}
            </p>
          </div>
        </div>

        <!-- Rating Stars -->
        <div class="rating-section">
          <label>Đánh giá của bạn <span class="required">*</span></label>
          <div class="stars">
            <i 
              v-for="star in 5" 
              :key="star"
              :class="[
                star <= rating ? 'fas fa-star' : 'far fa-star',
                'star-icon',
                { active: star <= rating }
              ]"
              @click="rating = star"
            ></i>
          </div>
          <span class="rating-text">{{ getRatingText(rating) }}</span>
        </div>

        <!-- Comment -->
        <div class="comment-section">
          <label>Nhận xét</label>
          <textarea 
            v-model="comment" 
            rows="4" 
            placeholder="Chia sẻ cảm nhận của bạn về sản phẩm..."
          ></textarea>
        </div>

        <!-- Upload Images -->
        <div class="image-upload-section">
          <label>Hình ảnh sản phẩm</label>
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

        <!-- Messages -->
        <div v-if="alreadyReviewed" class="reviewed-message">
          <i class="fas fa-check-circle"></i>
          Bạn đã đánh giá sản phẩm này rồi!
        </div>

        <div v-if="!canReview && !alreadyReviewed && !checking" class="warning-message">
          <i class="fas fa-clock"></i>
          Bạn chỉ có thể đánh giá sau khi đơn hàng đã hoàn thành!
        </div>

        <div v-if="checking" class="loading-message">
          <i class="fas fa-spinner fa-spin"></i>
          Đang kiểm tra...
        </div>

        <div v-if="error" class="error-message">
          <i class="fas fa-exclamation-circle"></i>
          {{ error }}
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-outline" @click="close">Đóng</button>
        <button 
          class="btn btn-primary" 
          @click="submitReview" 
          :disabled="submitting || rating === 0 || alreadyReviewed || !canReview || checking || isUploading"
        >
          <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
          {{ submitting ? 'Đang gửi...' : 'Gửi đánh giá' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import api from "@/api/axios";

const props = defineProps({
  visible: Boolean,
  product: Object,
  orderId: Number
})

const emit = defineEmits(['close', 'submitted'])

const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");

const rating = ref(0)
const comment = ref('')
const submitting = ref(false)
const error = ref('')
const alreadyReviewed = ref(false)
const canReview = ref(false)
const checking = ref(false)
const isUploading = ref(false)
const uploadError = ref('')
const isDragOver = ref(false)

// State cho upload ảnh
const uploadedImages = ref([])
const fileInput = ref(null)

const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

const getRatingText = (value) => {
  const texts = {
    0: 'Chọn đánh giá',
    1: 'Rất tệ',
    2: 'Tệ',
    3: 'Bình thường',
    4: 'Tốt',
    5: 'Rất tốt'
  }
  return texts[value] || ''
}

// Upload ảnh lên server
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

// Xử lý chọn file
const handleFileSelect = async (event) => {
  const files = event.target.files
  if (files.length === 0) return

  // Reset lỗi
  uploadError.value = ''
  isUploading.value = true

  // Giới hạn số lượng ảnh (tối đa 5 ảnh)
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
    
    // Kiểm tra kích thước (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      uploadError.value = `File ${file.name} vượt quá 5MB`
      continue
    }

    // Kiểm tra loại file
    if (!file.type.startsWith('image/')) {
      uploadError.value = `File ${file.name} không phải là hình ảnh`
      continue
    }

    // Thêm vào danh sách với trạng thái đang upload
    const imageItem = {
      file: file,
      preview: URL.createObjectURL(file),
      uploading: true,
      url: null
    }
    uploadedImages.value.push(imageItem)

    try {
      // Upload lên server
      const url = await uploadImage(file)
      imageItem.url = url
      imageItem.uploading = false
      console.log('✅ Uploaded image:', url)
    } catch (err) {
      imageItem.uploading = false
      uploadError.value = `Không thể tải ảnh ${file.name}`
      // Xóa ảnh lỗi khỏi danh sách
      const index = uploadedImages.value.indexOf(imageItem)
      if (index > -1) {
        uploadedImages.value.splice(index, 1)
      }
    }
  }

  isUploading.value = false
  // Reset input
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// Xử lý drag and drop
const handleDrop = async (event) => {
  isDragOver.value = false
  const files = event.dataTransfer.files
  if (files.length === 0) return

  // Gọi lại handleFileSelect với files
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

// Kiểm tra đã đánh giá chưa và có thể đánh giá không
const checkReviewStatus = async () => {
  if (!props.product?.productDetailId || !props.orderId) {
    console.log('⚠️ Missing productDetailId or orderId')
    return
  }
  
  checking.value = true
  error.value = ''
  
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      error.value = 'Vui lòng đăng nhập để đánh giá'
      checking.value = false
      return
    }
    
    console.log('🔍 Checking review status for order:', props.orderId, 'product:', props.product.productDetailId)
    
    // 1. Kiểm tra đã đánh giá chưa
    const checkResponse = await api.get(`/reviews/check`, {
      params: {
        orderId: props.orderId,
        productDetailId: props.product.productDetailId
      } })
    alreadyReviewed.value = checkResponse.data.reviewed || false
    console.log('✅ alreadyReviewed:', alreadyReviewed.value)
    
    // 2. Nếu chưa đánh giá, kiểm tra có thể đánh giá không
    if (!alreadyReviewed.value) {
      try {
        const canReviewResponse = await api.get(`/reviews/can-review`, {
          params: {
            orderId: props.orderId,
            productDetailId: props.product.productDetailId
          } })
        canReview.value = canReviewResponse.data.canReview || false
        console.log('✅ canReview:', canReview.value)
      } catch (err) {
        console.error('❌ canReview API error:', err)
        canReview.value = true // Tạm thời cho phép để test
      }
      
      if (!canReview.value) {
        error.value = 'Bạn chỉ có thể đánh giá sau khi đơn hàng đã hoàn thành!'
      }
    } else {
      error.value = 'Bạn đã đánh giá sản phẩm này rồi!'
    }
  } catch (err) {
    console.error('❌ Check review status error:', err)
    if (err.response?.status === 404) {
      console.warn('⚠️ API not found, allowing review for testing')
      canReview.value = true
      error.value = ''
    } else {
      error.value = err.response?.data?.message || 'Không thể kiểm tra trạng thái đánh giá'
    }
  } finally {
    checking.value = false
  }
}

const close = () => {
  rating.value = 0
  comment.value = ''
  error.value = ''
  uploadError.value = ''
  alreadyReviewed.value = false
  canReview.value = false
  checking.value = false
  isUploading.value = false
  
  // Xóa ảnh khi đóng
  uploadedImages.value.forEach(img => {
    if (img.preview) {
      URL.revokeObjectURL(img.preview)
    }
  })
  uploadedImages.value = []
  emit('close')
}

const submitReview = async () => {
  if (rating.value === 0) {
    error.value = 'Vui lòng chọn số sao đánh giá'
    return
  }

  if (alreadyReviewed.value) {
    error.value = 'Bạn đã đánh giá sản phẩm này rồi!'
    return
  }

  if (!canReview.value) {
    error.value = 'Bạn chỉ có thể đánh giá sau khi đơn hàng đã hoàn thành!'
    return
  }

  // Kiểm tra ảnh đã upload xong chưa
  const uploadingImages = uploadedImages.value.filter(img => img.uploading)
  if (uploadingImages.length > 0) {
    error.value = 'Đang tải ảnh lên, vui lòng đợi...'
    return
  }

  submitting.value = true
  error.value = ''

  try {
    const token = localStorage.getItem('token')
    
    // Lấy danh sách URL ảnh đã upload
    const imageUrls = uploadedImages.value
      .filter(img => img.url)
      .map(img => img.url)

    const payload = {
      productDetailId: props.product?.productDetailId,
      rating: rating.value,
      comment: comment.value,
      orderId: props.orderId,
      images: imageUrls
    }

    console.log('📤 Submitting review:', payload)

    const response = await api.post(`/reviews/create`, payload, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })

    console.log('✅ Review submitted:', response.data)

    if (response.data) {
      emit('submitted', response.data)
      close()
    }
  } catch (err) {
    console.error('❌ Submit review error:', err)
    console.error('❌ Error response:', err.response?.data)
    
    const message = err.response?.data?.message || 'Gửi đánh giá thất bại'
    error.value = message
    
    if (message.includes('đánh giá') || message.includes('reviewed')) {
      alreadyReviewed.value = true
    }
  } finally {
    submitting.value = false
  }
}

// Watch visible
watch(() => props.visible, (newVal) => {
  if (newVal && props.product) {
    rating.value = 0
    comment.value = ''
    error.value = ''
    uploadError.value = ''
    alreadyReviewed.value = false
    canReview.value = false
    uploadedImages.value = []
    isUploading.value = false
    checkReviewStatus()
  }
})

onMounted(() => {
  if (props.visible && props.product) {
    checkReviewStatus()
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
  z-index: 10;
  border-radius: 16px 16px 0 0;
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

.product-info {
  display: flex;
  gap: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 10px;
  margin-bottom: 20px;
}

.product-image {
  width: 60px;
  height: 60px;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-image i {
  font-size: 30px;
  color: #ccc;
}

.product-details h4 {
  font-size: 15px;
  color: #2c3e50;
  margin: 0 0 4px 0;
}

.product-variant {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.rating-section {
  margin-bottom: 20px;
}

.rating-section label {
  display: block;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 10px;
}

.stars {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.star-icon {
  font-size: 32px;
  cursor: pointer;
  color: #ddd;
  transition: all 0.2s;
}

.star-icon:hover {
  transform: scale(1.1);
}

.star-icon.active {
  color: #f5c842;
}

.rating-text {
  font-size: 14px;
  color: #666;
}

.required {
  color: #e63946;
}

.comment-section {
  margin-bottom: 20px;
}

.comment-section label {
  display: block;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.comment-section textarea {
  width: 100%;
  padding: 12px;
  border: 2px solid #e9ecef;
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  transition: all 0.3s;
}

.comment-section textarea:focus {
  outline: none;
  border-color: #e63946;
}

/* Image Upload Styles */
.image-upload-section {
  margin-bottom: 20px;
}

.image-upload-section label {
  display: block;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
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

/* Message Styles */
.reviewed-message {
  padding: 16px 20px;
  background: #e8f5e9;
  border-radius: 10px;
  color: #2e7d32;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 15px;
}

.reviewed-message i {
  font-size: 24px;
}

.warning-message {
  padding: 16px 20px;
  background: #fff3cd;
  border-radius: 10px;
  color: #856404;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 15px;
}

.warning-message i {
  font-size: 24px;
}

.loading-message {
  padding: 16px 20px;
  background: #e3f2fd;
  border-radius: 10px;
  color: #0d47a1;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 15px;
}

.loading-message i {
  font-size: 24px;
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
  border-radius: 0 0 16px 16px;
  background: white;
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

/* Responsive */
@media (max-width: 576px) {
  .modal-container {
    max-width: 100%;
    margin: 10px;
    max-height: 95vh;
  }
  
  .modal-header {
    padding: 16px 20px;
  }
  
  .modal-body {
    padding: 16px 20px;
  }
  
  .stars .star-icon {
    font-size: 28px;
  }
  
  .image-preview-item {
    width: 60px;
    height: 60px;
  }
}
</style>