<!-- components/sale-staff/ReturnRequestDetailModal.vue -->
<template>
  <div v-if="visible" class="modal-overlay">
    <div class="modal-container modal-lg">
      <div class="modal-header">
        <h2>Chi tiết yêu cầu hoàn hàng</h2>
        <button class="close-btn" @click="close">✕</button>
      </div>
      <div class="modal-body">
        <div v-if="returnRequest" class="return-detail">
          <div class="return-info-grid">
            <div class="info-section">
              <h3>Thông tin khách hàng</h3>
              <p><strong>Khách hàng:</strong> {{ returnRequest.customerName || 'N/A' }}</p>
              <p><strong>SĐT:</strong> {{ returnRequest.customerPhone || 'N/A' }}</p>
              <p><strong>Email:</strong> {{ returnRequest.customerEmail || 'N/A' }}</p>
            </div>
            <div class="info-section">
              <h3>Thông tin đơn hàng</h3>
              <p><strong>Mã đơn:</strong> #{{ returnRequest.orderCode || returnRequest.orderId }}</p>
              <p><strong>Trạng thái:</strong> 
                <span :class="['status-badge', getStatusClass(returnRequest.status)]">
                  {{ getStatusText(returnRequest.status) }}
                </span>
              </p>
              <p><strong>Ngày yêu cầu:</strong> {{ formatDate(returnRequest.createdAt) }}</p>
            </div>
          </div>

          <div class="return-reason">
            <h3>Lý do hoàn hàng</h3>
            <div class="reason-content">
              <p><strong>Lý do:</strong> {{ returnRequest.reason }}</p>
              <p><strong>Mô tả:</strong> {{ returnRequest.description }}</p>
              <p v-if="returnRequest.adminNote"><strong>Phản hồi Admin:</strong> {{ returnRequest.adminNote }}</p>
            </div>
          </div>

          <div v-if="returnRequest.images && returnRequest.images.length > 0" class="return-images">
            <h3>Hình ảnh minh họa</h3>
            <div class="image-list">
              <img 
                v-for="(img, index) in returnRequest.images" 
                :key="index"
                :src="getImageUrl(img)"
                class="return-image"
                @click="viewImage(img)"
              />
            </div>
          </div>

          <!-- Admin Actions -->
          <div class="admin-actions">
            <h3>Xử lý yêu cầu</h3>
            
            <template v-if="returnRequest.status === 'PENDING'">
              <div class="action-group">
                <div class="form-group">
                  <label>Phản hồi (tùy chọn)</label>
                  <textarea v-model="adminNote" rows="2" placeholder="Nhập phản hồi cho khách hàng..." class="form-control"></textarea>
                </div>
                <div class="button-group">
                  <button class="btn btn-success" @click="processReturn('APPROVED')" :disabled="processing">
                    <i v-if="processing" class="fas fa-spinner fa-spin"></i>
                    {{ processing ? 'Đang xử lý...' : 'Duyệt hoàn hàng' }}
                  </button>
                  <button class="btn btn-danger" @click="processReturn('REJECTED')" :disabled="processing">
                    <i v-if="processing" class="fas fa-spinner fa-spin"></i>
                    {{ processing ? 'Đang xử lý...' : 'Từ chối' }}
                  </button>
                </div>
              </div>
            </template>

            <template v-if="returnRequest.status === 'APPROVED'">
              <div class="action-group">
                <div class="form-group">
                  <label>Ghi chú khi nhận hàng (tùy chọn)</label>
                  <textarea v-model="adminNote" rows="2" placeholder="Nhập ghi chú khi nhận hàng trả..." class="form-control"></textarea>
                </div>
                <div class="button-group">
                  <button class="btn btn-primary" @click="receiveReturn" :disabled="receiving">
                    <i v-if="receiving" class="fas fa-spinner fa-spin"></i>
                    {{ receiving ? 'Đang xử lý...' : 'Đã nhận hàng trả' }}
                  </button>
                </div>
              </div>
            </template>

            <template v-if="returnRequest.status === 'RECEIVED'">
              <div class="status-info-box status-success">
                <i class="fas fa-check-circle"></i>
                <span>Đã nhận hàng trả. Chờ hoàn tất.</span>
              </div>
            </template>

            <template v-if="returnRequest.status === 'COMPLETED'">
              <div class="status-info-box status-success">
                <i class="fas fa-check-circle"></i>
                <span>Đã hoàn tất hoàn hàng!</span>
              </div>
            </template>

            <template v-if="returnRequest.status === 'REJECTED'">
              <div class="status-info-box status-danger">
                <i class="fas fa-times-circle"></i>
                <span>Đã từ chối yêu cầu hoàn hàng.</span>
              </div>
            </template>
          </div>

          <div class="return-status">
            <h3>Trạng thái hiện tại</h3>
            <div class="status-info">
              <span :class="['status-badge', getStatusClass(returnRequest.status)]">
                {{ getStatusText(returnRequest.status) }}
              </span>
              <span v-if="returnRequest.updatedAt" class="update-time">
                Cập nhật: {{ formatDate(returnRequest.updatedAt) }}
              </span>
            </div>
          </div>
        </div>
        <div v-else class="loading-state">
          <div class="spinner"></div>
          <p>Đang tải thông tin...</p>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="close">Đóng</button>
      </div>
    </div>

    <!-- Image Viewer -->
    <div v-if="showImageViewer" class="modal-overlay">
      <div class="image-viewer-container">
        <img :src="viewingImage" alt="Return image" />
        <button class="close-btn" @click="closeImageViewer">✕</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { formatDate, getStatusText, getStatusClass } from '../../utils/helpers'

const props = defineProps({
  visible: Boolean,
  order: Object,
  returnRequest: Object
})

const emit = defineEmits(['close', 'process', 'receive'])

const adminNote = ref('')
const processing = ref(false)
const receiving = ref(false)
const showImageViewer = ref(false)
const viewingImage = ref('')

const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");

const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

const close = () => {
  adminNote.value = ''
  emit('close')
}

const processReturn = (status) => {
  processing.value = true
  emit('process', {
    status: status,
    adminNote: adminNote.value || null
  })
  processing.value = false
}

const receiveReturn = () => {
  receiving.value = true
  emit('receive', adminNote.value || null)
  receiving.value = false
}

const viewImage = (imageUrl) => {
  viewingImage.value = getImageUrl(imageUrl)
  showImageViewer.value = true
}

const closeImageViewer = () => {
  showImageViewer.value = false
  viewingImage.value = ''
}

watch(() => props.visible, (newVal) => {
  if (!newVal) {
    adminNote.value = ''
    processing.value = false
    receiving.value = false
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
  border-radius: 15px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s;
}

.modal-container.modal-lg {
  max-width: 1000px;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(50px); }
  to { opacity: 1; transform: translateY(0); }
}

.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  background: white;
  border-radius: 15px 15px 0 0;
  z-index: 1;
}

.modal-header h2 {
  font-size: 20px;
  color: #2c3e50;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  transition: all 0.3s;
}

.close-btn:hover {
  color: #e63946;
  transform: rotate(90deg);
}

.modal-body {
  padding: 25px;
}

.modal-footer {
  padding: 20px 25px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.return-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.info-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
}

.info-section h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 10px;
  padding-bottom: 5px;
  border-bottom: 1px solid #eee;
}

.info-section p {
  margin-bottom: 5px;
  font-size: 14px;
  color: #666;
}

.return-reason {
  background: #f8f9fa;
  padding: 15px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.return-reason h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.return-reason .reason-content p {
  margin-bottom: 8px;
  font-size: 14px;
  color: #555;
}

.return-images {
  margin-bottom: 20px;
}

.return-images h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.return-images .image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.return-images .return-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #e9ecef;
  transition: all 0.3s;
}

.return-images .return-image:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.admin-actions {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.admin-actions h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 15px;
}

.admin-actions .action-group {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.admin-actions .form-group {
  margin-bottom: 0;
}

.admin-actions .form-group label {
  display: block;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 6px;
}

.admin-actions .form-control {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  transition: all 0.3s;
}

.admin-actions .form-control:focus {
  outline: none;
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.admin-actions .button-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.admin-actions .button-group .btn {
  min-width: 120px;
}

.status-info-box {
  padding: 12px 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
}

.status-info-box.status-success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-info-box.status-danger {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.status-info-box i {
  font-size: 20px;
}

.return-status {
  background: #f8f9fa;
  padding: 15px 20px;
  border-radius: 8px;
}

.return-status h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.return-status .status-info {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.return-status .update-time {
  font-size: 13px;
  color: #999;
}

.status-badge {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.status-pending {
  background: #fff3cd;
  color: #856404;
}
.status-badge.status-approved {
  background: #d4edda;
  color: #155724;
}
.status-badge.status-rejected {
  background: #f8d7da;
  color: #721c24;
}
.status-badge.status-return-received {
  background: #cce5ff;
  color: #004085;
}
.status-badge.status-completed {
  background: #d4edda;
  color: #155724;
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

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover {
  background: #1e7e34;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover {
  background: #c62828;
}

.btn-outline {
  background: #f8f9fa;
  color: #666;
  border: 1px solid #ddd;
}

.btn-outline:hover {
  background: #e9ecef;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-state {
  text-align: center;
  padding: 40px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #e63946;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.image-viewer-container {
  max-width: 90vw;
  max-height: 90vh;
  position: relative;
}

.image-viewer-container img {
  max-width: 100%;
  max-height: 85vh;
  object-fit: contain;
  border-radius: 8px;
}

.image-viewer-container .close-btn {
  position: absolute;
  top: -40px;
  right: -40px;
  background: white;
  color: #333;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.image-viewer-container .close-btn:hover {
  transform: rotate(90deg);
  background: #e63946;
  color: white;
}

@media (max-width: 768px) {
  .return-info-grid {
    grid-template-columns: 1fr;
  }

  .admin-actions .button-group {
    flex-direction: column;
  }

  .admin-actions .button-group .btn {
    width: 100%;
  }

  .return-images .return-image {
    width: 80px;
    height: 80px;
  }

  .image-viewer-container .close-btn {
    top: 10px;
    right: 10px;
  }
}
</style>