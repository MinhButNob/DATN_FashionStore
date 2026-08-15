<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay">
        <div class="modal-container modal-lg">
          <div class="modal-header">
            <h3>Chi tiết đơn hàng #{{ order?.orderCode || order?.orderId }}</h3>
            <button class="close-btn" @click="close">✕</button>
          </div>

          <div class="modal-body" v-if="order">
            <!-- Customer Info -->
            <div class="info-section">
              <h4>Thông tin khách hàng</h4>
              <div class="info-grid">
                <div class="info-item">
                  <span class="label">Họ tên:</span>
                  <span class="value">{{ order.customer?.customerName || order.customer?.fullName || '---' }}</span>
                </div>
                <div class="info-item">
                  <span class="label">SĐT:</span>
                  <span class="value">{{ order.customer?.phone || '---' }}</span>
                </div>
                <div class="info-item">
                  <span class="label">Email:</span>
                  <span class="value">{{ order.customer?.email || '---' }}</span>
                </div>
              </div>
            </div>

            <!-- Shipping Address -->
            <div class="info-section" v-if="order.shippingAddress">
              <h4>Địa chỉ giao hàng</h4>
              <div class="info-item">
                <span class="label">Địa chỉ:</span>
                <span class="value">{{ getFullAddress(order.shippingAddress) }}</span>
              </div>
            </div>

            <!-- Order Info -->
            <div class="info-section">
              <h4>Thông tin đơn hàng</h4>
              <div class="info-grid">
                <div class="info-item">
                  <span class="label">Ngày đặt:</span>
                  <span class="value">{{ formatDate(order.orderDate) }}</span>
                </div>
                <div class="info-item">
                  <span class="label">Trạng thái:</span>
                  <span :class="['status-badge', statusClass]">{{ getStatusText(order.orderStatus) }}</span>
                </div>
                <div class="info-item">
                  <span class="label">Thanh toán:</span>
                  <span :class="['payment-badge', paymentClass]">{{ getPaymentStatusText(order.paymentStatus) }}</span>
                </div>
              </div>
            </div>

            <!-- Order Items -->
            <div class="items-section">
              <h4>Sản phẩm đã đặt</h4>
              <table class="items-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th>Phân loại</th>
                    <th>Giá gốc</th>
                    <th>Voucher</th>
                    <th>Thực trả</th>
                    <th>Trạng thái</th>
                    <th>Số lượng</th>
                    <th>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in order.orderItems" :key="idx">
                    <td class="product-cell">
                      <div class="product-info">
                        <div class="product-image">
                          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.productName" @error="handleImageError" />
                          <i v-else class="fas fa-tshirt"></i>
                        </div>
                        <span class="product-name">{{ item.productName }}</span>
                      </div>
                    </td>
                    <td class="variant-cell">
                      <div class="variants">
                        <span v-if="item.colorName" class="color-dot" :style="{ backgroundColor: getColorHex(item.colorName) }"></span>
                        <span>{{ item.colorName || '' }} {{ item.sizeName || '' }}</span>
                      </div>
                    </td>
                    <td class="price-cell">{{ formatPrice(item.unitPrice) }}</td>
                    <td class="price-cell">-{{ formatPrice(item.discount || 0) }}</td>
                    <td class="price-cell">{{ formatPrice((Number(item.finalAmount || 0) / Number(item.quantity || 1))) }}</td>
                    <td><span class="status-badge" :class="getStatusClass(item.status)">{{ getItemStatusText(item.status) }}</span></td>
                    <td class="quantity-cell">{{ item.quantity }}</td>
                    <td class="total-cell">{{ formatPrice(item.finalAmount || (item.unitPrice * item.quantity)) }}</td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr class="subtotal-row">
                    <td colspan="7" class="text-right">Tạm tính:</td>
                    <td class="subtotal-value">{{ formatPrice(order.subtotalAmount || order.orderSubtotalAmount) }}</td>
                  </tr>
                  <tr class="discount-row" v-if="(order.discountAmount || order.orderDiscountAmount) > 0">
                    <td colspan="7" class="text-right">Giảm giá:</td>
                    <td class="discount-value">-{{ formatPrice(order.discountAmount || order.orderDiscountAmount) }}</td>
                  </tr>
                  <tr class="shipping-row">
                    <td colspan="7" class="text-right">Phí vận chuyển:</td>
                    <td class="shipping-value">{{ formatPrice(order.shippingFee || order.orderShippingFee || 0) }}</td>
                  </tr>
                  <tr class="total-row">
                    <td colspan="7" class="text-right">Tổng cộng:</td>
                    <td class="total-value">{{ formatPrice(order.totalAmount) }}</td>
                  </tr>
                </tfoot>
              </table>
            </div>

            <!-- Notes -->
            <div class="notes-section" v-if="order.notes">
              <h4>Ghi chú</h4>
              <p>{{ order.notes }}</p>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-outline" @click="close">Đóng</button>
            <button class="btn-primary" @click="printOrder">In đơn hàng</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

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

const emit = defineEmits(['close'])

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

const getFullAddress = (addr) => {
  if (!addr) return '---'
  if (typeof addr === 'object') {
    const parts = [addr.addressLine, addr.ward, addr.district, addr.city].filter(p => p)
    return parts.join(', ') || '---'
  }
  return addr
}

const getStatusText = (status) => {
  const map = {
    'PENDING_PAYMENT': 'Chờ xác nhận',
    'PROCESSING': 'Đang xử lý',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy',
    'RETURNED': 'Trả hàng',
    'PARTIALLY_RETURNED': 'Hoàn trả một phần'
  }
  return map[status] || status || 'Chưa xác định'
}

const getStatusClass = (status) => {
  const map = {
    'PENDING_PAYMENT': 'pending',
    'PROCESSING': 'processing',
    'COMPLETED': 'completed',
    'CANCELLED': 'cancelled',
    'RETURNED': 'returned',
    'PARTIALLY_RETURNED': 'returned'
  }
  return map[status] || ''
}

const getItemStatusText = (status) => ({
  COMPLETED: 'Hoàn thành', RETURN_REQUESTED: 'Đang yêu cầu hoàn',
  RETURN_APPROVED: 'Đã duyệt hoàn', RETURN_RECEIVED: 'Đã nhận hàng trả',
  RETURNED: 'Đã hoàn', REJECTED: 'Từ chối'
}[status] || 'Hoàn thành')

const getPaymentStatusText = (status) => {
  const map = {
    'UNPAID': 'Chưa thanh toán',
    'PAID': 'Đã thanh toán',
    'PARTIAL': 'Thanh toán một phần',
    'REFUNDED': 'Đã hoàn tiền'
  }
  return map[status] || status || 'Chưa thanh toán'
}

const statusClass = computed(() => getStatusClass(props.order?.orderStatus))
const paymentClass = computed(() => {
  const status = props.order?.paymentStatus
  if (status === 'PAID') return 'payment-paid'
  if (status === 'UNPAID') return 'payment-unpaid'
  return ''
})

const getColorHex = (colorName) => {
  if (!colorName) return '#CCCCCC'
  const colorMap = {
    'Đỏ': '#FF0000', 'Đen': '#000000', 'Trắng': '#FFFFFF',
    'Xanh': '#0000FF', 'Navy': '#1B2951', 'Vàng': '#FFFF00',
    'Hồng': '#FFC0CB', 'Xám': '#808080', 'Nâu': '#8B4513',
    'Tím': '#800080', 'Cam': '#FFA500'
  }
  return colorMap[colorName] || '#CCCCCC'
}

const handleImageError = (e) => {
  e.target.style.display = 'none'
  const parent = e.target.parentElement
  if (parent && !parent.querySelector('i')) {
    const icon = document.createElement('i')
    icon.className = 'fas fa-tshirt'
    parent.appendChild(icon)
  }
}

const close = () => {
  emit('close')
}

const printOrder = () => {
  window.print()
}
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
  border-radius: 12px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideIn 0.3s;
}

.modal-container.modal-lg {
  max-width: 1000px;
}

@keyframes slideIn {
  from { transform: translateY(-30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 12px 12px 0 0;
  position: sticky;
  top: 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: white;
}

.close-btn:hover {
  opacity: 0.8;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  position: sticky;
  bottom: 0;
  background: white;
}

.info-section {
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.info-section h4 {
  color: #2c3e50;
  margin-bottom: 12px;
  font-size: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.info-item {
  display: flex;
  gap: 10px;
  font-size: 14px;
}

.info-item .label {
  font-weight: 600;
  color: #666;
  min-width: 100px;
}

.info-item .value {
  color: #2c3e50;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}

.status-badge.pending { background: #fff3cd; color: #856404; }
.status-badge.processing { background: #cce5ff; color: #004085; }
.status-badge.completed { background: #d4edda; color: #155724; }
.status-badge.cancelled { background: #f8d7da; color: #721c24; }
.status-badge.returned { background: #fef3c7; color: #92400e; }

.payment-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}

.payment-badge.payment-paid { background: #d4edda; color: #155724; }
.payment-badge.payment-unpaid { background: #fff3cd; color: #856404; }

.items-section {
  margin-bottom: 25px;
}

.items-section h4 {
  color: #2c3e50;
  margin-bottom: 12px;
  font-size: 16px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.items-table th {
  text-align: left;
  padding: 10px;
  background: #f8f9fa;
  border-bottom: 2px solid #eee;
}

.items-table td {
  padding: 10px;
  border-bottom: 1px solid #eee;
  vertical-align: middle;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-image {
  width: 40px;
  height: 40px;
  background: #f5f5f5;
  border-radius: 6px;
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
  font-size: 20px;
  color: #ccc;
}

.product-name {
  font-weight: 500;
  color: #2c3e50;
}

.variants {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid #ddd;
  display: inline-block;
}

.price-cell, .quantity-cell, .total-cell {
  text-align: right;
}

.text-right {
  text-align: right;
}

.subtotal-row, .discount-row, .shipping-row {
  font-size: 14px;
}

.total-row {
  font-weight: 700;
  font-size: 16px;
  background: #fafafa;
}

.total-value {
  color: #e63946;
  font-size: 18px;
}

.notes-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
}

.notes-section h4 {
  margin-bottom: 8px;
  font-size: 14px;
}

.notes-section p {
  color: #666;
  font-size: 14px;
}

.btn-outline, .btn-primary {
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-outline {
  background: white;
  border: 1px solid #ddd;
  color: #666;
}

.btn-outline:hover {
  border-color: #e63946;
  color: #e63946;
}

.btn-primary {
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  border: none;
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.3);
}

.modal-enter-active, .modal-leave-active {
  transition: opacity 0.3s;
}

.modal-enter-from, .modal-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .items-table {
    font-size: 12px;
  }
  
  .product-name {
    max-width: 120px;
  }
  
  .modal-footer {
    flex-direction: column;
  }
  
  .modal-footer button {
    width: 100%;
  }
}
</style>
