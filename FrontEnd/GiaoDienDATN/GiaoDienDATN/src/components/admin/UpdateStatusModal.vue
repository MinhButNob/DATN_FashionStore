<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="close">
        <div class="modal-container">
          <div class="modal-header">
            <h3>
              <i class="fas fa-exchange-alt"></i>
              Cập nhật trạng thái đơn hàng
            </h3>
            <button class="close-btn" @click="close">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="modal-body" v-if="order">
            <!-- Thông tin đơn hàng -->
            <div class="order-info">
              <div class="info-row">
                <span class="label">Mã đơn hàng:</span>
                <span class="value">#{{ order.orderCode || order.orderId }}</span>
              </div>
              <div class="info-row">
                <span class="label">Ngày đặt:</span>
                <span class="value">{{ formatDate(order.orderDate) }}</span>
              </div>
              <div class="info-row">
                <span class="label">Khách hàng:</span>
                <span class="value">{{ order.customer?.customerName || 'Khách vãng lai' }}</span>
              </div>
              <div class="info-row">
                <span class="label">Số điện thoại:</span>
                <span class="value">{{ order.customer?.phone || '---' }}</span>
              </div>
              <div class="info-row">
                <span class="label">Email:</span>
                <span class="value">{{ order.customer?.email || '---' }}</span>
              </div>
              <div class="info-row address-row">
                <span class="label">Địa chỉ giao hàng:</span>
                <span class="value">
                  <i class="fas fa-map-marker-alt"></i>
                  {{ getFullAddress(order.shippingAddress) }}
                </span>
              </div>
              <div class="info-row">
                <span class="label">Tổng tiền:</span>
                <span class="value total-amount">{{ formatPrice(order.totalAmount) }}</span>
              </div>
            </div>

            <!-- Trạng thái hiện tại -->
            <div class="current-status">
              <span class="status-label">Trạng thái hiện tại:</span>
              <span :class="['status-badge', getStatusClass(order.orderStatus)]">
                {{ getStatusText(order.orderStatus) }}
              </span>
            </div>

            <!-- Chọn trạng thái mới -->
            <div class="status-select-group">
              <label class="form-label">Chọn trạng thái mới</label>
              <select v-model="selectedStatus" class="status-select-input">
                <option 
                  v-for="status in availableStatuses" 
                  :key="status.value"
                  :value="status.value"
                  :disabled="status.value === order.orderStatus"
                >
                  {{ status.label }}
                  <span v-if="status.value === order.orderStatus">(Hiện tại)</span>
                </option>
              </select>
            </div>

            <!-- Ghi chú -->
            <div class="notes-group">
              <label class="form-label">Ghi chú (không bắt buộc)</label>
              <textarea 
                v-model="notes" 
                rows="3" 
                placeholder="Nhập ghi chú cho việc cập nhật trạng thái..."
                class="notes-input"
              ></textarea>
            </div>

            <!-- Thông báo lỗi -->
            <div v-if="errorMessage" class="error-message">
              <i class="fas fa-exclamation-circle"></i>
              {{ errorMessage }}
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-cancel" @click="close">
              <i class="fas fa-times"></i> Hủy
            </button>
            <button 
              class="btn btn-update" 
              @click="updateStatus"
              :disabled="loading || selectedStatus === order?.orderStatus"
            >
              <i v-if="loading" class="fas fa-spinner fa-spin"></i>
              <i v-else class="fas fa-save"></i>
              {{ loading ? 'Đang cập nhật...' : 'Cập nhật trạng thái' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { adminOrderService } from '../../services/adminOrderService'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  order: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'updated'])

const loading = ref(false)
const selectedStatus = ref('')
const notes = ref('')
const errorMessage = ref('')

// Danh sách trạng thái có thể chọn
const availableStatuses = [
  { value: 'PENDING_PAYMENT', label: 'Chờ thanh toán' },
  { value: 'PROCESSING', label: 'Đang xử lý' },
  { value: 'SHIPPING', label: 'Đang giao hàng' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'CANCELLED', label: 'Đã hủy' }
]

// Watch để reset khi mở modal
watch(() => props.visible, (newVal) => {
  if (newVal && props.order) {
    selectedStatus.value = props.order.orderStatus || ''
    notes.value = ''
    errorMessage.value = ''
  }
})

// Lấy địa chỉ đầy đủ
const getFullAddress = (address) => {
  if (!address) return 'Không có địa chỉ'
  
  const parts = []
  if (address.addressLine) parts.push(address.addressLine)
  if (address.ward) parts.push(address.ward)
  if (address.district) parts.push(address.district)
  if (address.city) parts.push(address.city)
  
  return parts.length > 0 ? parts.join(', ') : 'Không có địa chỉ'
}

// Format functions
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

const getStatusText = (status) => {
  const map = {
    'PENDING_PAYMENT': 'Chờ thanh toán',
    'PROCESSING': 'Đang xử lý',
    'SHIPPING': 'Đang giao',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy'
  }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    'PENDING_PAYMENT': 'status-pending',
    'PROCESSING': 'status-processing',
    'SHIPPING': 'status-shipping',
    'COMPLETED': 'status-completed',
    'CANCELLED': 'status-cancelled'
  }
  return map[status] || ''
}

const close = () => {
  emit('close')
}

const updateStatus = async () => {
  if (!props.order) return
  
  const oldStatus = props.order.orderStatus
  const newStatus = selectedStatus.value
  
  if (oldStatus === newStatus) {
    errorMessage.value = 'Trạng thái mới trùng với trạng thái hiện tại'
    return
  }

  // Xác nhận trước khi cập nhật
  const confirmMessage = `Bạn có chắc muốn chuyển trạng thái đơn hàng #${props.order.orderCode || props.order.orderId} từ "${getStatusText(oldStatus)}" sang "${getStatusText(newStatus)}"?`
  
  if (!confirm(confirmMessage)) {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await adminOrderService.updateOrderStatus(props.order.orderId, {
      orderStatus: newStatus,
      notes: notes.value || `Admin cập nhật trạng thái từ "${getStatusText(oldStatus)}" sang "${getStatusText(newStatus)}"`
    })

    if (response.data) {
      // Cập nhật thành công
      emit('updated', {
        orderId: props.order.orderId,
        oldStatus: oldStatus,
        newStatus: newStatus,
        data: response.data
      })
      close()
    }
  } catch (error) {
    console.error('❌ Error updating order status:', error)
    errorMessage.value = error.response?.data?.message || 'Cập nhật thất bại! Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== OVERLAY ===== */
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
  z-index: 2000;
  backdrop-filter: blur(5px);
  padding: 20px;
}

.modal-container {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 550px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

/* ===== HEADER ===== */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  border-radius: 16px 16px 0 0;
  position: sticky;
  top: 0;
  z-index: 1;
}

.modal-header h3 {
  color: white;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.modal-header h3 i {
  margin-right: 10px;
}

.close-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

/* ===== BODY ===== */
.modal-body {
  padding: 24px;
}

/* Order Info */
.order-info {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  padding: 6px 0;
  font-size: 14px;
}

.info-row .label {
  font-weight: 600;
  color: #2c3e50;
  width: 140px;
  flex-shrink: 0;
}

.info-row .value {
  color: #555;
  flex: 1;
  word-break: break-word;
}

.address-row .value {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.address-row .value i {
  color: #e63946;
  margin-top: 2px;
  flex-shrink: 0;
}

.total-amount {
  font-weight: 700;
  color: #e63946;
  font-size: 16px;
}

/* Current Status */
.current-status {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f0f0f0;
  border-radius: 8px;
  margin-bottom: 16px;
}

.status-label {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.status-badge {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.status-badge.status-pending {
  background: #fff3cd;
  color: #856404;
}
.status-badge.status-processing {
  background: #cce5ff;
  color: #004085;
}
.status-badge.status-shipping {
  background: #d1ecf1;
  color: #0c5460;
}
.status-badge.status-completed {
  background: #d4edda;
  color: #155724;
}
.status-badge.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}

/* Select Status */
.status-select-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
  font-size: 14px;
}

.status-select-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
  background: white;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 16px center;
}

.status-select-input:focus {
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.status-select-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Notes */
.notes-group {
  margin-bottom: 16px;
}

.notes-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
  resize: vertical;
  font-family: inherit;
  min-height: 80px;
}

.notes-input:focus {
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

/* Error */
.error-message {
  padding: 12px 16px;
  background: #fde8e8;
  border-radius: 8px;
  color: #e63946;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ===== FOOTER ===== */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px 24px;
  border-top: 1px solid #eee;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-cancel {
  background: #f0f0f0;
  color: #666;
}

.btn-cancel:hover {
  background: #e0e0e0;
}

.btn-update {
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
}

.btn-update:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(230, 57, 70, 0.3);
}

.btn-update:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* ===== ANIMATIONS ===== */
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(50px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

/* ===== RESPONSIVE ===== */
@media (max-width: 480px) {
  .modal-container {
    max-width: 100%;
    margin: 10px;
    border-radius: 12px;
  }

  .modal-header {
    padding: 16px 20px;
  }

  .modal-body {
    padding: 16px 20px;
  }

  .modal-footer {
    flex-direction: column;
  }

  .btn {
    width: 100%;
    justify-content: center;
  }

  .info-row {
    flex-direction: column;
    padding: 4px 0;
  }

  .info-row .label {
    width: 100%;
    margin-bottom: 2px;
  }

  .current-status {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>