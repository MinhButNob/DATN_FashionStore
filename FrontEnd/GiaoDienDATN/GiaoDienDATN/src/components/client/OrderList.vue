<template>
  <div class="order-list">
    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <i class="fas fa-spinner fa-spin"></i>
      <p>Đang tải đơn hàng...</p>
    </div>

    <!-- Empty orders -->
    <div v-else-if="filteredOrders.length === 0" class="empty-state">
      <i class="fas fa-shopping-bag"></i>
      <h3>Chưa có đơn hàng nào</h3>
      <p>Bạn chưa có đơn hàng nào trong danh sách này</p>
      <router-link to="/shop" class="btn-shop-now">
        <i class="fas fa-shopping-cart"></i> Mua sắm ngay
      </router-link>
    </div>

    <!-- Orders list -->
    <div v-else class="orders-list">
      <div v-for="order in filteredOrders" :key="order.orderId" class="order-card">
        <div class="order-header">
          <div class="order-info">
            <span class="order-id">#{{ order.orderCode || order.orderId }}</span>
            <span class="order-date">{{ formatDate(order.orderDate) }}</span>
          </div>
          <span :class="['order-status', getStatusClass(order.orderStatus)]">
            {{ getStatusText(order.orderStatus) }}
          </span>
        </div>

        <div class="order-items">
          <div v-for="item in order.items" :key="item.itemId" class="order-item">
            <div class="item-image" @click="goToProduct(item.productId)">
              <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" @error="handleImageError" />
              <i v-else class="fas fa-tshirt"></i>
            </div>
            <div class="item-details">
              <h4 @click="goToProduct(item.productId)">{{ item.productName }}</h4>
              <p class="item-variant">
                {{ item.sizeName || '' }} {{ item.colorName || '' }}
              </p>
              <p class="item-quantity">x{{ item.quantity }}</p>
            </div>
            <div class="item-price">
              {{ formatPrice(item.finalAmount) }}
            </div>
            
            <!-- Nút Đánh giá cho từng sản phẩm (chỉ dùng trong Profile) -->
            <div v-if="showReviewActions && order.orderStatus === 'COMPLETED'" class="item-actions">
              <button 
                v-if="!isItemReviewed(order.orderId, item.productDetailId)"
                class="btn btn-review btn-sm"
                @click="handleReview(order, item)"
              >
                <i class="fas fa-star"></i> Đánh giá
              </button>
              <div v-else class="reviewed-group">
                <span class="reviewed-badge">
                  <i class="fas fa-check-circle"></i> Đã đánh giá
                </span>
                <button 
                  class="btn btn-sm btn-outline"
                  @click="handleViewReview(order.orderId, item.productDetailId)"
                  title="Xem đánh giá"
                >
                  <i class="fas fa-eye"></i>
                </button>
                <button 
                  class="btn btn-sm btn-danger"
                  @click="handleDeleteReview(order.orderId, item.productDetailId)"
                  title="Xóa đánh giá"
                >
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Voucher -->
        <div v-if="order.voucher" class="order-voucher">
          <i class="fas fa-ticket-alt"></i>
          <span class="voucher-code">{{ order.voucher.voucherCode }}</span>
          <span class="voucher-discount">-{{ formatPrice(order.discountAmount || 0) }}</span>
        </div>

        <div class="order-footer">
          <div class="order-total">
            <span>Tổng cộng:</span>
            <strong>{{ formatPrice(order.totalAmount) }}</strong>
          </div>
          <div class="order-actions">
            <button class="btn btn-outline" @click="handleViewDetail(order.orderId)">
              <i class="fas fa-eye"></i> Xem chi tiết
            </button>
            
            <!-- Hủy đơn - KHÔNG hiển thị thời gian -->
            <button 
              v-if="canCancelOrder(order)" 
              class="btn btn-danger" 
              @click="handleCancel(order.orderId)"
              title="Hủy đơn hàng"
            >
              <i class="fas fa-times"></i> Hủy đơn
            </button>
            
            <!-- Hoàn hàng - CHỈ KHI COMPLETED VÀ TRONG 48H -->
            <button 
              v-if="canReturnOrder(order)" 
              class="btn btn-warning" 
              @click="handleReturn(order)"
              :title="getReturnTooltip(order)"
            >
              <i class="fas fa-undo"></i> Hoàn hàng
              <span v-if="showReturnTime(order)" class="return-time-left">
                ({{ getReturnTimeLeft(order) }})
              </span>
            </button>
            
            <!-- Mua lại - Chỉ khi COMPLETED -->
            <button 
              v-if="order.orderStatus === 'COMPLETED'" 
              class="btn btn-primary" 
              @click="handleReorder(order)"
            >
              <i class="fas fa-redo"></i> Mua lại
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 1">
      <button 
        class="page-btn"
        :disabled="currentPage === 0"
        @click="changePage(currentPage - 1)"
      >
        <i class="fas fa-chevron-left"></i>
      </button>
      <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button 
        class="page-btn"
        :disabled="currentPage === totalPages - 1"
        @click="changePage(currentPage + 1)"
      >
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  orders: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  currentPage: {
    type: Number,
    default: 0
  },
  totalPages: {
    type: Number,
    default: 0
  },
  showReviewActions: {
    type: Boolean,
    default: false
  },
  reviewedItems: {
    type: Map,
    default: () => new Map()
  }
})

const emit = defineEmits([
  'view-detail',
  'cancel-order',
  'return-order',
  'reorder',
  'review',
  'view-review',
  'delete-review',
  'change-page'
])

const router = useRouter()

// ===== CONSTANTS =====
const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");
const RETURN_HOURS_LIMIT = 48

// ===== HELPERS =====
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

// ===== HỦY ĐƠN HÀNG =====
const canCancelOrder = (order) => {
  return ['PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING'].includes(order.orderStatus)
}

// ===== HOÀN HÀNG =====
// ✅ CHỈ CHO PHÉP HOÀN KHI ĐƠN HÀNG ĐÃ COMPLETED VÀ TRONG VÒNG 48H
const canReturnOrder = (order) => {
  // Chỉ cho phép hoàn khi đơn hàng đã hoàn thành
  if (order.orderStatus !== 'COMPLETED') {
    return false
  }
  
  // Kiểm tra thời gian trong vòng 48h kể từ khi COMPLETED
  return isWithin48HoursFromCompleted(order)
}

// ✅ Kiểm tra đơn hàng có trong vòng 48h kể từ khi COMPLETED không
const isWithin48HoursFromCompleted = (order) => {
  // Lấy thời điểm hoàn thành (updatedAt - thời điểm cập nhật lần cuối khi chuyển sang COMPLETED)
  const completedDate = order.updatedAt || order.orderDate || order.createdAt
  if (!completedDate) return false
  
  const date = new Date(completedDate)
  const now = new Date()
  const hoursDiff = (now - date) / (1000 * 60 * 60)
  
  console.log(`📊 Order #${order.orderId} - Completed at: ${date.toLocaleString()}, Hours passed: ${hoursDiff.toFixed(2)}/${RETURN_HOURS_LIMIT}`)
  
  return hoursDiff <= RETURN_HOURS_LIMIT
}

// ✅ Kiểm tra có nên hiển thị thời gian trên nút hoàn hàng không
const showReturnTime = (order) => {
  // Chỉ hiển thị thời gian khi đơn hàng đã COMPLETED và trong 48h
  return order.orderStatus === 'COMPLETED' && isWithin48HoursFromCompleted(order)
}

// ✅ Lấy thời gian còn lại để hoàn hàng (tính từ lúc COMPLETED)
const getReturnTimeLeft = (order) => {
  const completedDate = order.updatedAt || order.orderDate || order.createdAt
  if (!completedDate) return null
  
  const date = new Date(completedDate)
  const now = new Date()
  const hoursPassed = (now - date) / (1000 * 60 * 60)
  const hoursLeft = Math.ceil(RETURN_HOURS_LIMIT - hoursPassed)
  
  console.log(`⏰ Order #${order.orderId} - Hours left: ${hoursLeft}`)
  
  if (hoursLeft <= 0) return null
  
  if (hoursLeft < 1) {
    const minutesLeft = Math.ceil(hoursLeft * 60)
    return `${minutesLeft}p`
  }
  
  return `${hoursLeft}h`
}

// ✅ Tooltip cho nút hoàn hàng
const getReturnTooltip = (order) => {
  if (order.orderStatus !== 'COMPLETED') {
    return 'Chỉ có thể hoàn hàng khi đơn hàng đã hoàn thành'
  }
  
  if (isWithin48HoursFromCompleted(order)) {
    const timeLeft = getReturnTimeLeft(order)
    return `Còn ${timeLeft} để yêu cầu hoàn hàng (kể từ khi hoàn thành)`
  }
  
  return 'Đã quá 48h kể từ khi hoàn thành, không thể yêu cầu hoàn hàng'
}

// ===== STATUS =====
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
    'REJECTED': 'Từ chối hoàn trả'
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

const isItemReviewed = (orderId, productDetailId) => {
  if (!orderId || !productDetailId) return false
  const reviewKey = `${orderId}_${productDetailId}`
  const data = props.reviewedItems.get(reviewKey)
  return data && data.reviewId !== null && data.reviewId !== undefined
}

// ===== FILTERED ORDERS =====
const filteredOrders = computed(() => {
  return props.orders
})

// ===== HANDLERS =====
const goToProduct = (id) => {
  if (id) {
    router.push(`/product/${id}`)
  }
}

const handleViewDetail = (orderId) => {
  emit('view-detail', orderId)
}

const handleCancel = (orderId) => {
  emit('cancel-order', orderId)
}

const handleReturn = (order) => {
  emit('return-order', order)
}

const handleReorder = (order) => {
  emit('reorder', order)
}

const handleReview = (order, item) => {
  emit('review', { order, item })
}

const handleViewReview = (orderId, productDetailId) => {
  emit('view-review', { orderId, productDetailId })
}

const handleDeleteReview = (orderId, productDetailId) => {
  emit('delete-review', { orderId, productDetailId })
}

const changePage = (page) => {
  emit('change-page', page)
}
</script>

<style scoped>
/* ... giữ nguyên style ... */
.order-list {
  width: 100%;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-state i {
  font-size: 50px;
  color: #e63946;
  margin-bottom: 20px;
}

.loading-state p {
  color: #6c757d;
  font-size: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #f8f9fa;
  border-radius: 15px;
}

.empty-state i {
  font-size: 70px;
  color: #e63946;
  margin-bottom: 15px;
  opacity: 0.7;
}

.empty-state h3 {
  color: #1a1a2e;
  font-size: 22px;
  margin-bottom: 10px;
}

.empty-state p {
  color: #6c757d;
  margin-bottom: 25px;
  font-size: 16px;
}

.btn-shop-now {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 32px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  box-shadow: 0 4px 15px rgba(230, 57, 70, 0.3);
}

.btn-shop-now:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 25px rgba(230, 57, 70, 0.4);
  background: linear-gradient(135deg, #c62828, #e63946);
}

.btn-shop-now i {
  font-size: 18px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  border: 1px solid #e9ecef;
  border-radius: 15px;
  padding: 20px;
  transition: all 0.3s;
}

.order-card:hover {
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.05);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  flex-wrap: wrap;
  gap: 10px;
}

.order-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.order-id {
  font-weight: 700;
  color: #e63946;
  font-size: 16px;
}

.order-date {
  color: #6c757d;
  font-size: 14px;
}

.order-status {
  padding: 5px 15px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}
.status-processing {
  background: #cce5ff;
  color: #004085;
}
.status-shipping {
  background: #d1ecf1;
  color: #0c5460;
}
.status-delivered {
  background: #d4edda;
  color: #155724;
}
.status-completed {
  background: #d4edda;
  color: #155724;
}
.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}
.status-return-requested {
  background: #fff3cd;
  color: #856404;
}
.status-return-approved {
  background: #cce5ff;
  color: #004085;
}
.status-return-received {
  background: #cce5ff;
  color: #004085;
}
.status-return-pending {
  background: #d1ecf1;
  color: #0c5460;
}
.status-returned {
  background: #d4edda;
  color: #155724;
}
.status-rejected {
  background: #f8d7da;
  color: #721c24;
}

.order-items {
  padding: 10px 0;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.order-item:last-child {
  border-bottom: none;
}

.item-image {
  width: 60px;
  height: 60px;
  background: #f1f3f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-image i {
  font-size: 30px;
  color: #adb5bd;
}

.item-details {
  flex: 1;
}

.item-details h4 {
  font-size: 15px;
  color: #1a1a2e;
  margin-bottom: 3px;
  cursor: pointer;
}

.item-details h4:hover {
  color: #e63946;
}

.item-variant {
  font-size: 13px;
  color: #6c757d;
}

.item-quantity {
  font-size: 13px;
  color: #6c757d;
}

.item-price {
  font-weight: 600;
  color: #e63946;
  font-size: 15px;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-left: auto;
  flex-shrink: 0;
}

.reviewed-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.reviewed-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #e8f5e9;
  color: #2e7d32;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.reviewed-badge i {
  font-size: 14px;
}

.order-voucher {
  background: #e8f5e9;
  padding: 10px 15px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 5px 0 15px;
  border-left: 4px solid #2e7d32;
}

.order-voucher i {
  color: #2e7d32;
  font-size: 18px;
}

.order-voucher .voucher-code {
  font-weight: 700;
  color: #2e7d32;
  background: white;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 14px;
}

.order-voucher .voucher-discount {
  color: #e63946;
  font-weight: 700;
  margin-left: auto;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #e9ecef;
  flex-wrap: wrap;
  gap: 15px;
}

.order-total {
  font-size: 16px;
}

.order-total strong {
  color: #e63946;
  margin-left: 10px;
}

.order-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
  min-width: 32px;
}

.btn-outline {
  background: #f0f0f0;
  color: #666;
}

.btn-outline:hover {
  background: #e0e0e0;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover:not(.disabled) {
  background: #c82333;
}

.btn-danger.disabled {
  background: #6c757d;
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-warning {
  background: #ffc107;
  color: #333;
}

.btn-warning:hover {
  background: #e0a800;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover {
  background: #c62828;
}

.btn-review {
  background: linear-gradient(135deg, #f5c842, #f0a500);
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.btn-review:hover {
  background: linear-gradient(135deg, #e6b800, #cc9900);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 200, 66, 0.4);
}

/* ✅ Style cho thời gian còn lại trên nút hoàn hàng */
.return-time-left {
  font-size: 10px;
  opacity: 0.85;
  margin-left: 2px;
  background: rgba(0, 0, 0, 0.08);
  padding: 1px 8px;
  border-radius: 12px;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #666;
}
</style>