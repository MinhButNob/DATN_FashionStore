<template>
  <div class="order-card" :class="statusClass">
    <div class="order-header">
      <div class="order-info">
        <span class="order-id">#{{ order.orderCode || order.orderId }}</span>
        <span class="order-date">{{ formatDate(order.orderDate) }}</span>
      </div>
      <div class="order-status">
        <span :class="['status-badge', statusClass]">
          {{ getStatusText(order.orderStatus) }}
        </span>
        <span class="payment-status" :class="paymentClass">
          {{ getPaymentStatusText(order.paymentStatus) }}
        </span>
      </div>
    </div>

    <div class="order-items">
      <div v-for="item in order.items" :key="item.itemId" class="order-item">
        <div class="item-image" @click="goToProduct(item.productDetailId)">
          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.productName" />
          <i v-else class="fas fa-tshirt"></i>
        </div>
        <div class="item-details">
          <h4 @click="goToProduct(item.productDetailId)">{{ item.productName }}</h4>
          <div class="item-variants">
            <span v-if="item.colorName" class="variant">
              <span class="color-dot" :style="{ backgroundColor: getColorHex(item.colorName) }"></span>
              {{ item.colorName }}
            </span>
            <span v-if="item.sizeName" class="variant">Size: {{ item.sizeName }}</span>
          </div>
          <div class="item-price-info">
            <span class="item-price">{{ formatPrice(item.unitPrice) }}</span>
            <span class="item-quantity">x{{ item.quantity }}</span>
            <span class="item-total">{{ formatPrice(item.finalAmount) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="order-footer">
      <div class="order-total">
        <span>Tổng cộng:</span>
        <strong>{{ formatPrice(displayTotal(order)) }}</strong>
      </div>
      <div class="order-actions">
        <button class="btn-outline" @click="$emit('view-detail', order.orderId)">
          <i class="fas fa-eye"></i> Xem chi tiết
        </button>
        <button 
          v-if="canCancel" 
          class="btn-danger-outline" 
          @click="$emit('cancel', order.orderId)"
        >
          <i class="fas fa-times"></i> Hủy đơn
        </button>
        <button 
          v-if="canReorder" 
          class="btn-primary" 
          @click="$emit('reorder', order)"
        >
          <i class="fas fa-shopping-cart"></i> Mua lại
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useOrder } from '../../composables/useOrder'

const props = defineProps({
  order: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['view-detail', 'cancel', 'reorder'])
const router = useRouter()
const { formatPrice, formatDate, getStatusText, getStatusClass, getPaymentStatusText } = useOrder()

const statusClass = computed(() => getStatusClass(props.order.orderStatus))
const paymentClass = computed(() => {
  if (props.order.paymentStatus === 'PAID') return 'payment-paid'
  if (props.order.paymentStatus === 'UNPAID') return 'payment-unpaid'
  return ''
})

const canCancel = computed(() => {
  return props.order.orderStatus === 'PENDING_PAYMENT' || props.order.orderStatus === 'PROCESSING'
})

const canReorder = computed(() => {
  return props.order.orderStatus === 'COMPLETED'
})

const displayTotal = (order) => {
  if (!Array.isArray(order?.returnItems)) return order?.totalAmount

  return order.returnItems.reduce((total, item) => {
    const amount = item.refundAmount ?? item.finalAmount ?? (Number(item.unitPrice || 0) * Number(item.quantity || 0))
    return total + Number(amount || 0)
  }, 0)
}

const getColorHex = (colorName) => {
  const colorMap = {
    'Đỏ': '#FF0000', 'Đen': '#000000', 'Trắng': '#FFFFFF',
    'Xanh': '#0000FF', 'Navy': '#1B2951', 'Vàng': '#FFFF00',
    'Hồng': '#FFC0CB', 'Xám': '#808080', 'Nâu': '#8B4513',
    'Tím': '#800080', 'Cam': '#FFA500'
  }
  return colorMap[colorName] || '#CCCCCC'
}

const goToProduct = (productId) => {
  router.push(`/product/${productId}`)
}
</script>

<style scoped>
.order-card {
  background: white;
  border-radius: 15px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.order-card:hover {
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #eee;
  flex-wrap: wrap;
  gap: 10px;
}

.order-info {
  display: flex;
  gap: 15px;
  align-items: baseline;
  flex-wrap: wrap;
}

.order-id {
  font-weight: 700;
  color: #e63946;
  font-size: 16px;
}

.order-date {
  color: #999;
  font-size: 13px;
}

.order-status {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.pending {
  background: #fff3cd;
  color: #856404;
}

.status-badge.processing {
  background: #cce5ff;
  color: #004085;
}

.status-badge.completed {
  background: #d4edda;
  color: #155724;
}

.status-badge.cancelled {
  background: #f8d7da;
  color: #721c24;
}

.status-badge.returned {
  background: #fef3c7;
  color: #92400e;
}

.payment-status {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.payment-status.payment-paid {
  background: #d4edda;
  color: #155724;
}

.payment-status.payment-unpaid {
  background: #fff3cd;
  color: #856404;
}

.order-items {
  padding: 10px 20px;
}

.order-item {
  display: flex;
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
}

.order-item:last-child {
  border-bottom: none;
}

.item-image {
  width: 80px;
  height: 80px;
  background: #f5f5f5;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-image i {
  font-size: 40px;
  color: #ccc;
}

.item-details {
  flex: 1;
}

.item-details h4 {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 6px;
  cursor: pointer;
}

.item-details h4:hover {
  color: #e63946;
}

.item-variants {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.variant {
  font-size: 12px;
  color: #999;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid #ddd;
}

.item-price-info {
  display: flex;
  gap: 15px;
  align-items: baseline;
  flex-wrap: wrap;
}

.item-price {
  color: #666;
  font-size: 13px;
}

.item-quantity {
  color: #999;
  font-size: 13px;
}

.item-total {
  font-weight: 600;
  color: #e63946;
  font-size: 14px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #fafafa;
  border-top: 1px solid #eee;
  flex-wrap: wrap;
  gap: 15px;
}

.order-total {
  font-size: 16px;
}

.order-total strong {
  color: #e63946;
  margin-left: 10px;
  font-size: 18px;
}

.order-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.btn-outline, .btn-danger-outline, .btn-primary {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
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

.btn-danger-outline {
  background: white;
  border: 1px solid #e63946;
  color: #e63946;
}

.btn-danger-outline:hover {
  background: #e63946;
  color: white;
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

@media (max-width: 768px) {
  .order-header, .order-footer {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .order-status {
    width: 100%;
    justify-content: space-between;
  }
  
  .order-actions {
    width: 100%;
  }
  
  .order-actions button {
    flex: 1;
  }
  
  .order-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .item-image {
    width: 100%;
    height: 150px;
  }
}
</style>
