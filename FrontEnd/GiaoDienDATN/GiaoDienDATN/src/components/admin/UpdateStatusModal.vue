<!-- components/admin/UpdateStatusModal.vue - Full file đã sửa -->
<template>
  <div v-if="visible" class="modal-overlay">
    <div class="modal-container">
      <div class="modal-header">
        <h3>Cập nhật trạng thái đơn hàng</h3>
        <button class="close-btn" @click="close">✕</button>
      </div>

      <div class="modal-body">
        <!-- ✅ Hiển thị thông tin người xác nhận -->
        <div v-if="order?.processedBy" class="processed-warning" :class="{ 'is-other': isConfirmedByOther }">
          <div class="warning-header">
            <i class="fas fa-check-circle" :class="{ 'text-success': !isConfirmedByOther, 'text-warning': isConfirmedByOther }"></i>
            <span>{{ isConfirmedByOther ? '⚠️ Đơn hàng đã được xác nhận bởi người khác' : '✅ Đơn hàng đã được xác nhận' }}</span>
          </div>
          <div class="warning-body">
            <p><strong>Người xác nhận:</strong> {{ order.processedBy }}</p>
            <p><strong>Email:</strong> {{ order.processedByEmail || 'N/A' }}</p>
            <p><strong>Vai trò:</strong> {{ order.processedByRole || 'N/A' }}</p>
            <p><strong>Thời gian:</strong> {{ formatDate(order.processedAt) }}</p>
            <p v-if="isConfirmedByOther" class="warning-text">
              <i class="fas fa-exclamation-triangle"></i>
              Đơn hàng này đã được xác nhận bởi <strong>{{ order.processedBy }}</strong> ({{ order.processedByEmail }}).
              Bạn không thể cập nhật trạng thái đơn hàng này.
            </p>
            <p v-else class="info-text">
              <i class="fas fa-info-circle"></i>
              Bạn là người đã xác nhận đơn hàng này. Bạn có thể tiếp tục cập nhật các trạng thái tiếp theo.
            </p>
          </div>
        </div>

        <div class="order-info">
          <p><strong>Mã đơn hàng:</strong> #{{ order?.orderCode || order?.orderId }}</p>
          <p><strong>Trạng thái hiện tại:</strong> 
            <span :class="['status-badge', getStatusClass(order?.orderStatus)]">
              {{ getStatusText(order?.orderStatus) }}
            </span>
          </p>
        </div>

        <div class="form-group">
          <label>Chọn trạng thái mới <span class="required">*</span></label>
          <select 
            v-model="selectedStatus" 
            class="form-control"
            :disabled="isConfirmedByOther || availableStatuses.length === 0"
          >
            <option value="" disabled>-- Chọn trạng thái --</option>
            <option 
              v-for="status in availableStatuses" 
              :key="status.value"
              :value="status.value"
            >
              {{ status.label }}
            </option>
          </select>
          <small v-if="isConfirmedByOther" class="text-danger">
            ⚠️ Đơn hàng đã được xác nhận bởi người khác, bạn không thể thay đổi trạng thái.
          </small>
          <small v-else-if="availableStatuses.length === 0" class="text-muted">
            Không thể cập nhật trạng thái này
          </small>
        </div>

        <div class="form-group">
          <label>Ghi chú (tùy chọn)</label>
          <textarea 
            v-model="notes" 
            rows="3" 
            placeholder="Nhập ghi chú cho khách hàng..."
            class="form-control"
            :disabled="isConfirmedByOther || availableStatuses.length === 0"
          ></textarea>
        </div>

        <!-- ✅ Thêm thông báo khi chọn DELIVERED -->
        <div v-if="selectedStatus === 'DELIVERED'" class="info-message">
          <i class="fas fa-info-circle"></i>
          <span>Khi chuyển sang trạng thái <strong>Đã giao hàng</strong>, thanh toán sẽ tự động được cập nhật thành <strong>THÀNH CÔNG</strong>.</span>
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
          @click="updateStatus" 
          :disabled="loading || !selectedStatus || isConfirmedByOther || availableStatuses.length === 0"
        >
          <i v-if="loading" class="fas fa-spinner fa-spin"></i>
          {{ loading ? 'Đang cập nhật...' : 'Cập nhật' }}
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

const emit = defineEmits(['close', 'updated'])

const loading = ref(false)
const error = ref('')
const notes = ref('')
const selectedStatus = ref('')

// ===== ADMIN STATUS TRANSITIONS =====
const ADMIN_STATUS_TRANSITIONS = {
  'PENDING_PAYMENT': ['CONFIRMED'],
  'CONFIRMED': ['PROCESSING'],
  'PROCESSING': ['SHIPPING'],
  'SHIPPING': ['DELIVERED'],
  'DELIVERED': ['COMPLETED'],
  'RETURN_APPROVED': ['RETURN_RECEIVED'],
  'RETURN_RECEIVED': ['RETURNED']
}

const FINAL_STATUSES = ['CANCELLED', 'RETURNED', 'RETURN_REQUESTED', 'RETURN_PENDING', 'REJECTED', 'COMPLETED']

// ✅ Lấy username và email người dùng hiện tại
const getCurrentUser = () => {
  return localStorage.getItem('username') || 
         localStorage.getItem('userName') || 
         ''
}

const getCurrentUserEmail = () => {
  return localStorage.getItem('userEmail') || 
         localStorage.getItem('email') || 
         ''
}

// ✅ Kiểm tra xem đơn hàng đã được xác nhận bởi người khác chưa
const isConfirmedByOther = computed(() => {
  if (!props.order || !props.order.processedBy) {
    return false
  }
  
  const currentUser = getCurrentUser()
  const currentEmail = getCurrentUserEmail()
  const status = props.order.orderStatus
  
  // ✅ Chỉ chặn khi đơn hàng ở trạng thái CONFIRMED hoặc PROCESSING
  if (status !== 'CONFIRMED' && status !== 'PROCESSING') {
    return false
  }
  
  // 🔥 QUAN TRỌNG: So sánh KHÔNG phân biệt chữ hoa/thường
  const processedByLower = (props.order.processedBy || '').toLowerCase().trim()
  const processedByEmailLower = (props.order.processedByEmail || '').toLowerCase().trim()
  const currentUserLower = (currentUser || '').toLowerCase().trim()
  const currentEmailLower = (currentEmail || '').toLowerCase().trim()
  
  // Kiểm tra xem có phải cùng người không
  const isSameUser = processedByLower === currentUserLower || 
                     processedByEmailLower === currentEmailLower ||
                     processedByLower === currentEmailLower ||
                     processedByEmailLower === currentUserLower
  
  // Nếu không cùng người -> là người khác
  return !isSameUser
})

const availableStatuses = computed(() => {
  if (!props.order || !props.order.orderStatus) {
    return []
  }
  
  const currentStatus = props.order.orderStatus
  
  if (FINAL_STATUSES.includes(currentStatus)) {
    return []
  }
  
  // Nếu đã được xác nhận bởi người khác, không cho phép chọn
  if (isConfirmedByOther.value) {
    return []
  }
  
  if (currentStatus === 'DELIVERED') {
    return [{ value: 'COMPLETED', label: 'Hoàn thành' }]
  }
  
  if (currentStatus === 'RETURN_APPROVED') {
    return [{ value: 'RETURN_RECEIVED', label: 'Đã nhận hàng trả' }]
  }
  
  if (currentStatus === 'RETURN_RECEIVED') {
    return [{ value: 'RETURNED', label: 'Đã hoàn trả' }]
  }
  
  const allowed = ADMIN_STATUS_TRANSITIONS[currentStatus] || []
  
  return allowed.map(status => ({
    value: status,
    label: getStatusText(status)
  }))
})

const getStatusText = (status) => {
  const map = {
    'PENDING_PAYMENT': 'Chờ thanh toán',
    'CONFIRMED': 'Đã xác nhận',
    'PROCESSING': 'Đang xử lý',
    'SHIPPING': 'Đang giao hàng',
    'DELIVERED': 'Đã giao hàng',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy',
    'RETURN_REQUESTED': 'Yêu cầu hoàn hàng',
    'RETURN_APPROVED': 'Đã duyệt hoàn hàng',
    'RETURN_RECEIVED': 'Đã nhận hàng trả',
    'RETURN_PENDING': 'Đang thu hồi hàng',
    'RETURNED': 'Đã hoàn trả',
    'REJECTED': 'Đã từ chối hoàn trả'
  }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    'PENDING_PAYMENT': 'status-pending',
    'CONFIRMED': 'status-confirmed',
    'PROCESSING': 'status-processing',
    'SHIPPING': 'status-shipping',
    'DELIVERED': 'status-delivered',
    'COMPLETED': 'status-completed',
    'CANCELLED': 'status-cancelled',
    'RETURN_REQUESTED': 'status-return-requested',
    'RETURN_APPROVED': 'status-return-approved',
    'RETURN_RECEIVED': 'status-return-received',
    'RETURN_PENDING': 'status-return-pending',
    'RETURNED': 'status-returned',
    'REJECTED': 'status-rejected'
  }
  return map[status] || ''
}

const formatDate = (dateString) => {
  if (!dateString) return '---'
  const date = new Date(dateString)
  if (isNaN(date.getTime())) return '---'
  return date.toLocaleDateString('vi-VN') + ' ' + 
         date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

const close = () => {
  error.value = ''
  notes.value = ''
  selectedStatus.value = ''
  emit('close')
}

const updateStatus = async () => {
  if (!selectedStatus.value) {
    error.value = 'Vui lòng chọn trạng thái mới'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const token = localStorage.getItem('token')
    const payload = {
      orderStatus: selectedStatus.value,
      notes: notes.value || null
    }

    console.log('📤 Updating order status:', payload)

    const response = await api.put(
      `/admin/orders/${props.order.orderId}/status`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )

    console.log('✅ Order status updated:', response.data)

    emit('updated', {
      orderId: props.order.orderId,
      newStatus: selectedStatus.value,
      data: response.data
    })
    
    close()
  } catch (err) {
    console.error('❌ Error updating order status:', err)
    console.error('❌ Error response:', err.response?.data)
    
    const message = err.response?.data?.message || 'Không thể cập nhật trạng thái đơn hàng'
    error.value = message
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (newVal) => {
  if (newVal && props.order) {
    selectedStatus.value = ''
    error.value = ''
    notes.value = ''
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
  max-width: 500px;
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
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
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
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.order-info p {
  margin: 5px 0;
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
}

.form-control:focus {
  outline: none;
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

textarea.form-control {
  resize: vertical;
  min-height: 80px;
}

/* ✅ Style cho info-message */
.info-message {
  padding: 12px 16px;
  background: #e3f2fd;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #0d47a1;
  font-size: 14px;
  margin-bottom: 16px;
  border-left: 4px solid #1976d2;
}

.info-message i {
  font-size: 18px;
}

.text-muted {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #999;
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

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.status-pending {
  background: #fff3cd;
  color: #856404;
}
.status-badge.status-confirmed {
  background: #cce5ff;
  color: #004085;
}
.status-badge.status-processing {
  background: #cce5ff;
  color: #004085;
}
.status-badge.status-shipping {
  background: #d1ecf1;
  color: #0c5460;
}
.status-badge.status-delivered {
  background: #d4edda;
  color: #155724;
}
.status-badge.status-completed {
  background: #d4edda;
  color: #155724;
}
.status-badge.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}
.status-badge.status-return-requested {
  background: #fff3cd;
  color: #856404;
}
.status-badge.status-return-approved {
  background: #cce5ff;
  color: #004085;
}
.status-badge.status-return-received {
  background: #cce5ff;
  color: #004085;
}
.status-badge.status-return-pending {
  background: #d1ecf1;
  color: #0c5460;
}
.status-badge.status-returned {
  background: #d4edda;
  color: #155724;
}
.status-badge.status-rejected {
  background: #f8d7da;
  color: #721c24;
}

.processed-warning {
  background: #f0fff4;
  border: 1px solid #c6e6d2;
  border-radius: 10px;
  padding: 15px;
  margin-bottom: 16px;
}

.processed-warning.is-other {
  background: #fff8e1;
  border-color: #ffd54f;
}

.warning-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 15px;
}

.warning-header .text-success {
  color: #2e7d32;
}

.warning-header .text-warning {
  color: #e65100;
}

.warning-body p {
  margin: 4px 0;
  font-size: 14px;
  color: #333;
}

.warning-text {
  color: #c62828;
  background: #ffebee;
  padding: 8px 12px;
  border-radius: 6px;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  font-size: 13px;
}

.info-text {
  color: #0d47a1;
  background: #e3f2fd;
  padding: 8px 12px;
  border-radius: 6px;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  font-size: 13px;
}

.text-danger {
  color: #dc3545;
  font-weight: 500;
  font-size: 13px;
  display: block;
  margin-top: 6px;
}

.text-muted {
  color: #999;
  font-size: 13px;
  display: block;
  margin-top: 6px;
}
</style>