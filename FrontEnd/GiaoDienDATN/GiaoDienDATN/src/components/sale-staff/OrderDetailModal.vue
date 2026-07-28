<!-- components/sale-staff/OrderDetailModal.vue -->
<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal-container modal-lg">
      <div class="modal-header">
        <h3>Chi tiết đơn hàng #{{ order?.orderCode || order?.orderId }}</h3>
        <button class="close-btn" @click="close">✕</button>
      </div>
      <div class="modal-body">
        <div class="order-info-grid">
          <div class="info-section">
            <h4>Thông tin khách hàng</h4>
            <p><strong>Họ tên:</strong> {{ order?.customer?.customerName || 'Khách hàng' }}</p>
            <p><strong>SĐT:</strong> {{ order?.customer?.phone || '---' }}</p>
            <p><strong>Email:</strong> {{ order?.customer?.email || '---' }}</p>
          </div>
          <div class="info-section">
            <h4>Thông tin đơn hàng</h4>
            <p><strong>Ngày đặt:</strong> {{ formatDate(order?.orderDate) }}</p>
            <p><strong>Trạng thái:</strong> {{ getStatusText(order?.orderStatus) }}</p>
            <p><strong>Thanh toán:</strong> {{ getPaymentStatusText(order?.paymentStatus) }}</p>
          </div>
          <div class="info-section">
            <h4>Địa chỉ giao hàng</h4>
            <p><strong>Người nhận:</strong> {{ order?.shippingAddress?.fullName || '---' }}</p>
            <p><strong>SĐT:</strong> {{ order?.shippingAddress?.phone || '---' }}</p>
            <p><strong>Địa chỉ:</strong> 
              {{ order?.shippingAddress?.addressLine || '---' }},
              {{ order?.shippingAddress?.ward || '' }},
              {{ order?.shippingAddress?.district || '' }},
              {{ order?.shippingAddress?.city || '' }}
            </p>
          </div>
        </div>

        <div class="order-items-section">
          <h4>Sản phẩm</h4>
          <table class="items-table">
            <thead>
              <tr>
                <th>Sản phẩm</th>
                <th class="price-cell">Đơn giá</th>
                <th class="quantity-cell">Số lượng</th>
                <th class="total-cell">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in order?.items" :key="item.itemId">
                <td>
                  <div class="item-info">
                    <div class="item-image">
                      <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" @error="handleImageError" />
                      <i v-else class="fas fa-tshirt"></i>
                    </div>
                    <div>
                      <div class="item-name">{{ item.productName }}</div>
                      <div class="item-variant">{{ item.sizeName || '' }} {{ item.colorName || '' }}</div>
                    </div>
                  </div>
                </td>
                <td class="price-cell">{{ formatPrice(item.unitPrice) }}</td>
                <td class="quantity-cell">{{ item.quantity }}</td>
                <td class="total-cell">{{ formatPrice(item.finalAmount) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="3" class="text-right">Tạm tính:</td>
                <td class="text-right">{{ formatPrice(order?.subtotalAmount) }}</td>
              </tr>
              <tr v-if="order?.discountAmount && order?.discountAmount > 0" class="discount-row">
                <td colspan="3" class="text-right">
                  <span class="voucher-label">
                    <i class="fas fa-ticket-alt"></i> Giảm giá 
                    ({{ order?.voucher?.voucherCode || 'Voucher' }})
                  </span>
                </td>
                <td class="text-right discount-amount">-{{ formatPrice(order?.discountAmount) }}</td>
              </tr>
              <tr>
                <td colspan="3" class="text-right">Phí vận chuyển:</td>
                <td class="text-right">{{ formatPrice(order?.shippingFee || 0) }}</td>
              </tr>
              <tr class="total-row">
                <td colspan="3" class="text-right">Tổng cộng:</td>
                <td class="text-right grand-total">{{ formatPrice(order?.totalAmount) }}</td>
              </tr>
            </tfoot>
          </table>
        </div>

        <div v-if="order?.voucher" class="voucher-info-section">
          <h4><i class="fas fa-ticket-alt"></i> Mã giảm giá đã áp dụng</h4>
          <div class="voucher-detail">
            <span class="voucher-code">{{ order.voucher.voucherCode }}</span>
            <span class="voucher-savings">Tiết kiệm: {{ formatPrice(order.discountAmount || 0) }}</span>
          </div>
        </div>

        <div v-if="order?.notes" class="order-notes">
          <h3>Ghi chú</h3>
          <p>{{ order.notes }}</p>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-primary" @click="emitPrint">
          <i class="fas fa-print"></i> In hóa đơn
        </button>
        <button class="btn btn-outline" @click="close">Đóng</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { formatPrice, formatDate, getStatusText, getStatusClass, getPaymentStatusText, getPaymentClass } from '../../utils/helpers'

const props = defineProps({
  visible: Boolean,
  order: Object
})

const emit = defineEmits(['close', 'print-invoice'])

const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");

const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

const handleImageError = (event) => {
  event.target.style.display = 'none'
}

const close = () => {
  emit('close')
}

const emitPrint = () => {
  emit('print-invoice', props.order?.orderId)
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

.modal-header h3 {
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

.order-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.info-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
}

.info-section h4 {
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

.order-items-section {
  margin-bottom: 30px;
}

.order-items-section h4 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 15px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
}

.items-table th {
  background: #f8f9fa;
  padding: 12px;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
}

.items-table td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  vertical-align: middle;
}

.items-table tfoot td {
  border-bottom: none;
  padding: 8px 10px;
}

.text-right {
  text-align: right;
}

.total-row {
  font-weight: 700;
}

.grand-total {
  font-size: 18px;
  color: #e63946;
}

.discount-row td {
  color: #28a745;
  font-weight: 600;
}

.discount-amount {
  color: #28a745;
  font-weight: 700;
}

.voucher-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.voucher-info-section {
  background: #e8f5e9;
  padding: 15px 20px;
  border-radius: 8px;
  margin-top: 20px;
  border-left: 4px solid #2e7d32;
}

.voucher-info-section h4 {
  color: #2e7d32;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.voucher-info-section .voucher-detail {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.voucher-info-section .voucher-code {
  font-weight: 700;
  color: #2e7d32;
  background: white;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 16px;
}

.voucher-info-section .voucher-savings {
  color: #e63946;
  font-weight: 700;
  font-size: 15px;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-image {
  width: 40px;
  height: 40px;
  background: #f5f5f5;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-image i {
  font-size: 20px;
  color: #ccc;
}

.item-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.item-variant {
  font-size: 12px;
  color: #999;
}

.price-cell, .quantity-cell, .total-cell {
  text-align: right;
}

.order-notes {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-top: 20px;
}

.order-notes h3 {
  font-size: 14px;
  color: #2c3e50;
  margin-bottom: 8px;
}

.order-notes p {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
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

@media (max-width: 768px) {
  .order-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>