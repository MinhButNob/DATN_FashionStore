<!-- components/sale-staff/InvoiceModal.vue -->
<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal-container modal-lg">
      <div class="modal-header">
        <h2>Hóa đơn #{{ invoiceData?.orderCode || order?.orderId }}</h2>
        <div class="header-actions">
          <button class="btn btn-primary btn-sm" @click="printInvoice">
            <i class="fas fa-print"></i> In hóa đơn
          </button>
          <button class="close-btn" @click="close">✕</button>
        </div>
      </div>
      <div class="modal-body" id="invoice-content">
        <div v-if="invoiceData" class="invoice-container">
          <div class="invoice-header">
            <div class="company-info">
              <h1>FASHION STORE</h1>
              <p>Địa chỉ: 123 Nguyễn Huệ, Quận 1, TP.HCM</p>
              <p>Điện thoại: (028) 1234 5678</p>
              <p>Email: info@fashionstore.com</p>
            </div>
            <div class="invoice-title">
              <h2>HÓA ĐƠN BÁN HÀNG</h2>
              <p>Mã đơn: #{{ invoiceData.orderCode }}</p>
              <p>Ngày: {{ formatDate(invoiceData.orderDate) }}</p>
              <p>Trạng thái: {{ getStatusText(invoiceData.orderStatus) }}</p>
            </div>
          </div>

          <div class="customer-info">
            <h3>Thông tin khách hàng</h3>
            <div class="info-row">
              <span><strong>Họ tên:</strong> {{ invoiceData.customerName || 'Khách hàng' }}</span>
              <span><strong>SĐT:</strong> {{ invoiceData.customerPhone || '---' }}</span>
              <span><strong>Email:</strong> {{ invoiceData.customerEmail || '---' }}</span>
            </div>
            <div class="info-row">
              <span><strong>Địa chỉ giao hàng:</strong> 
                {{ invoiceData.shippingFullName || '' }}
                {{ invoiceData.shippingPhone ? '- SĐT: ' + invoiceData.shippingPhone : '' }}
                {{ invoiceData.shippingAddress ? '- ' + invoiceData.shippingAddress : '' }}
                {{ invoiceData.shippingWard ? ', ' + invoiceData.shippingWard : '' }}
                {{ invoiceData.shippingDistrict ? ', ' + invoiceData.shippingDistrict : '' }}
                {{ invoiceData.shippingCity ? ', ' + invoiceData.shippingCity : '' }}
              </span>
            </div>
          </div>

          <div class="invoice-items">
            <h3>Chi tiết sản phẩm</h3>
            <table class="invoice-table">
              <thead>
                <tr>
                  <th style="width: 40%;">Sản phẩm</th>
                  <th style="width: 20%;">Đơn giá</th>
                  <th style="width: 15%;">Số lượng</th>
                  <th style="width: 25%;">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in invoiceData.items" :key="index">
                  <td>
                    <div class="product-info">
                      <div class="product-image">
                        <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" />
                        <i v-else class="fas fa-tshirt"></i>
                      </div>
                      <div>
                        <div class="product-name">{{ item.productName }}</div>
                        <div class="product-variant">{{ item.colorName || '' }} {{ item.sizeName || '' }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="text-right">{{ formatPrice(item.unitPrice) }}</td>
                  <td class="text-center">{{ item.quantity }}</td>
                  <td class="text-right">{{ formatPrice(item.finalAmount) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="3" class="text-right"><strong>Tạm tính:</strong></td>
                  <td class="text-right">{{ formatPrice(invoiceData.subtotalAmount) }}</td>
                </tr>
                <tr v-if="invoiceData.discountAmount && invoiceData.discountAmount > 0">
                  <td colspan="3" class="text-right">
                    <strong>Giảm giá</strong>
                    <span v-if="invoiceData.voucherCode" class="voucher-code">({{ invoiceData.voucherCode }})</span>
                  </td>
                  <td class="text-right discount-amount">-{{ formatPrice(invoiceData.discountAmount) }}</td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right"><strong>Phí vận chuyển:</strong></td>
                  <td class="text-right">{{ formatPrice(invoiceData.shippingFee || 0) }}</td>
                </tr>
                <tr class="total-row">
                  <td colspan="3" class="text-right"><strong>Tổng cộng:</strong></td>
                  <td class="text-right grand-total">{{ formatPrice(invoiceData.totalAmount) }}</td>
                </tr>
                <tr v-if="invoiceData.paymentStatus === 'PAID'">
                  <td colspan="4" class="text-right payment-status-paid">
                    <i class="fas fa-check-circle"></i> Đã thanh toán
                  </td>
                </tr>
                <tr v-else>
                  <td colspan="4" class="text-right payment-status-unpaid">
                    <i class="fas fa-clock"></i> Chưa thanh toán
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div v-if="invoiceData.notes" class="invoice-notes">
            <h4>Ghi chú:</h4>
            <p>{{ invoiceData.notes }}</p>
          </div>

          <div class="invoice-footer">
            <p>Cảm ơn quý khách đã mua hàng tại Fashion Store!</p>
          </div>
        </div>
        <div v-else class="loading-state">
          <div class="spinner"></div>
          <p>Đang tải hóa đơn...</p>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-primary" @click="printInvoice">
          <i class="fas fa-print"></i> In hóa đơn
        </button>
        <button class="btn btn-outline" @click="close">Đóng</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { formatPrice, formatDate, getStatusText } from '../../utils/helpers'

const props = defineProps({
  visible: Boolean,
  order: Object,
  invoiceData: Object
})

const emit = defineEmits(['close', 'print'])

const IMAGE_BASE = "http://localhost:8080"

const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

const close = () => {
  emit('close')
}

const printInvoice = () => {
  emit('print', props.order?.orderId)
  
  const content = document.getElementById('invoice-content')
  if (!content) return
  
  const printWindow = window.open('', '_blank', 'width=1000,height=800')
  if (!printWindow) return
  
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>Hóa đơn #${props.invoiceData?.orderCode}</title>
      <style>
        * { box-sizing: border-box; }
        body { font-family: Arial, sans-serif; padding: 40px; max-width: 800px; margin: 0 auto; background: white; color: #333; }
        .invoice-header { display: flex; justify-content: space-between; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 3px solid #e63946; }
        .company-info h1 { color: #e63946; margin: 0; font-size: 28px; }
        .company-info p { margin: 5px 0; color: #666; font-size: 13px; }
        .invoice-title { text-align: right; }
        .invoice-title h2 { color: #333; margin: 0; font-size: 22px; }
        .invoice-title p { margin: 5px 0; color: #666; font-size: 14px; }
        .customer-info { background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
        .customer-info h3 { margin-top: 0; color: #333; font-size: 16px; margin-bottom: 10px; }
        .info-row { margin: 5px 0; font-size: 14px; }
        .invoice-items h3 { font-size: 16px; color: #333; margin-bottom: 15px; }
        .invoice-table { width: 100%; border-collapse: collapse; margin: 15px 0; }
        .invoice-table th { background: #f8f9fa; padding: 10px; text-align: left; border-bottom: 2px solid #dee2e6; font-weight: 600; font-size: 14px; }
        .invoice-table td { padding: 10px; border-bottom: 1px solid #dee2e6; vertical-align: middle; font-size: 14px; }
        .text-right { text-align: right; }
        .text-center { text-align: center; }
        .total-row { font-weight: bold; background: #fafafa; }
        .grand-total { font-size: 18px; color: #e63946; }
        .discount-amount { color: #28a745; font-weight: bold; }
        .voucher-code { color: #28a745; font-weight: 600; background: #e8f5e9; padding: 2px 8px; border-radius: 4px; }
        .payment-status-paid { color: #28a745; font-weight: bold; padding: 8px; background: #d4edda; border-radius: 4px; display: inline-block; margin-top: 10px; }
        .payment-status-unpaid { color: #dc3545; font-weight: bold; padding: 8px; background: #f8d7da; border-radius: 4px; display: inline-block; margin-top: 10px; }
        .product-info { display: flex; align-items: center; gap: 10px; }
        .product-image { width: 40px; height: 40px; background: #f5f5f5; border-radius: 5px; display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0; }
        .product-image img { width: 100%; height: 100%; object-fit: cover; }
        .product-image i { font-size: 20px; color: #ccc; }
        .product-name { font-weight: 600; }
        .product-variant { font-size: 12px; color: #999; }
        .invoice-footer { margin-top: 30px; text-align: center; color: #666; border-top: 1px solid #eee; padding-top: 20px; font-style: italic; }
        .invoice-notes { background: #f8f9fa; padding: 15px; border-radius: 8px; margin: 15px 0; }
        .invoice-notes h4 { margin: 0 0 5px 0; color: #333; font-size: 14px; }
        .invoice-notes p { margin: 0; color: #666; }
        @media print { body { padding: 20px; } .btn { display: none; } }
      </style>
    </head>
    <body>
      ${content.innerHTML}
    </body>
    </html>
  `)
  
  printWindow.document.close()
  setTimeout(() => {
    printWindow.focus()
    printWindow.print()
  }, 500)
}
</script>

<style scoped>
/* Giống style của OrderDetailModal */
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
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

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
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

.invoice-container {
  padding: 10px;
}

.invoice-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e63946;
}

.company-info h1 {
  color: #e63946;
  margin: 0;
  font-size: 28px;
}

.company-info p {
  margin: 5px 0;
  color: #666;
}

.invoice-title {
  text-align: right;
}

.invoice-title h2 {
  color: #333;
  margin: 0;
  font-size: 24px;
}

.invoice-title p {
  margin: 5px 0;
  color: #666;
}

.customer-info {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.customer-info h3 {
  margin-top: 0;
  color: #333;
  font-size: 16px;
  margin-bottom: 10px;
}

.info-row {
  margin: 5px 0;
  font-size: 14px;
}

.invoice-items h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 15px;
}

.invoice-table {
  width: 100%;
  border-collapse: collapse;
}

.invoice-table th {
  background: #f8f9fa;
  padding: 10px;
  text-align: left;
  border-bottom: 2px solid #dee2e6;
  font-weight: 600;
  font-size: 14px;
}

.invoice-table td {
  padding: 10px;
  border-bottom: 1px solid #dee2e6;
  vertical-align: middle;
  font-size: 14px;
}

.voucher-code {
  color: #28a745;
  font-weight: 600;
  background: #e8f5e9;
  padding: 2px 8px;
  border-radius: 4px;
}

.total-row {
  font-weight: bold;
  background: #fafafa;
}

.grand-total {
  font-size: 18px;
  color: #e63946;
}

.discount-amount {
  color: #28a745;
  font-weight: bold;
}

.payment-status-paid {
  color: #28a745;
  font-weight: bold;
  padding: 8px;
  background: #d4edda;
  border-radius: 4px;
  display: inline-block;
  margin-top: 10px;
}

.payment-status-unpaid {
  color: #dc3545;
  font-weight: bold;
  padding: 8px;
  background: #f8d7da;
  border-radius: 4px;
  display: inline-block;
  margin-top: 10px;
}

.invoice-footer {
  margin-top: 30px;
  text-align: center;
  color: #666;
  border-top: 1px solid #eee;
  padding-top: 20px;
}

.invoice-notes {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin: 15px 0;
}

.invoice-notes h4 {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 14px;
}

.invoice-notes p {
  margin: 0;
  color: #666;
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

@media (max-width: 768px) {
  .invoice-header {
    flex-direction: column;
    text-align: center;
  }
  
  .invoice-title {
    text-align: center;
  }
}
</style>