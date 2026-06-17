<template>
  <div class="orders-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <h1 class="page-title">ĐƠN HÀNG CỦA TÔI</h1>

        <!-- Tab filters -->
        <div class="order-tabs">
          <button 
            v-for="tab in tabs" 
            :key="tab.value"
            :class="['tab-btn', { active: activeTab === tab.value }]"
            @click="changeTab(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>Đang tải đơn hàng...</p>
        </div>

        <!-- Empty orders -->
        <div v-else-if="orders.length === 0" class="empty-state">
          <i class="fas fa-box-open"></i>
          <h3>Chưa có đơn hàng nào</h3>
          <p>Bạn chưa có đơn hàng nào trong danh sách này</p>
          <router-link to="/shop" class="btn-primary">
            <i class="fas fa-shopping-cart"></i> Mua sắm ngay
          </router-link>
        </div>

        <!-- Orders list -->
        <div v-else>
          <div v-for="order in orders" :key="order.orderId" class="order-card">
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
                <div class="item-image">
                  <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.productName" />
                  <i v-else class="fas fa-tshirt"></i>
                </div>
                <div class="item-details">
                  <h4>{{ item.productName }}</h4>
                  <p class="item-variant">
                    {{ item.sizeName || '' }} {{ item.colorName || '' }}
                  </p>
                  <p class="item-quantity">x{{ item.quantity }}</p>
                </div>
                <div class="item-price">
                  {{ formatPrice(item.finalAmount) }}
                </div>
              </div>
            </div>

            <!-- ✅ HIỂN THỊ VOUCHER TRONG ĐƠN HÀNG -->
            <div v-if="order.voucher" class="order-voucher">
              <i class="fas fa-ticket-alt"></i>
              <span class="voucher-code">{{ order.voucher.voucherCode }}</span>
              <span class="voucher-discount">-{{ formatPrice(order.voucher.discountAmount || order.discountAmount || 0) }}</span>
            </div>

            <div class="order-footer">
              <div class="order-total">
                <span>Tổng cộng:</span>
                <strong>{{ formatPrice(order.totalAmount) }}</strong>
              </div>
              <div class="order-actions">
                <button class="btn btn-outline" @click="viewOrderDetail(order.orderId)">
                  <i class="fas fa-eye"></i> Xem chi tiết
                </button>
                <button 
                  v-if="order.orderStatus === 'PENDING_PAYMENT'" 
                  class="btn btn-danger" 
                  @click="handleCancelOrder(order.orderId)"
                >
                  <i class="fas fa-times"></i> Hủy đơn
                </button>
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
            @click="goToPage(currentPage - 1)"
          >
            <i class="fas fa-chevron-left"></i>
          </button>
          <button 
            v-for="page in displayedPages" 
            :key="page"
            :class="['page-btn', { active: currentPage === page }]"
            @click="goToPage(page)"
          >
            {{ page + 1 }}
          </button>
          <button 
            class="page-btn"
            :disabled="currentPage === totalPages - 1"
            @click="goToPage(currentPage + 1)"
          >
            <i class="fas fa-chevron-right"></i>
          </button>
        </div>
      </div>
    </main>

    <ClientFooter />

    <!-- Order Detail Modal -->
    <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-container modal-lg">
        <div class="modal-header">
          <h3>Chi tiết đơn hàng #{{ selectedOrder?.orderCode || selectedOrder?.orderId }}</h3>
          <button class="close-btn" @click="closeDetailModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="order-info-grid">
            <div class="info-section">
              <h4>Thông tin khách hàng</h4>
              <p><strong>Họ tên:</strong> {{ selectedOrder?.customer?.customerName || 'Khách hàng' }}</p>
              <p><strong>SĐT:</strong> {{ selectedOrder?.customer?.phone || '---' }}</p>
              <p><strong>Email:</strong> {{ selectedOrder?.customer?.email || '---' }}</p>
            </div>
            <div class="info-section">
              <h4>Thông tin đơn hàng</h4>
              <p><strong>Ngày đặt:</strong> {{ formatDate(selectedOrder?.orderDate) }}</p>
              <p><strong>Trạng thái:</strong> {{ getStatusText(selectedOrder?.orderStatus) }}</p>
              <p><strong>Thanh toán:</strong> {{ selectedOrder?.paymentStatus || 'UNPAID' }}</p>
            </div>
          </div>

          <div class="order-items-section">
            <h4>Sản phẩm</h4>
            <table class="items-table">
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Đơn giá</th>
                  <th>Số lượng</th>
                  <th>Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in selectedOrder?.items" :key="item.itemId">
                  <td>
                    <div class="item-info">
                      <div class="item-image">
                        <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.productName" />
                        <i v-else class="fas fa-tshirt"></i>
                      </div>
                      <div>
                        <div class="item-name">{{ item.productName }}</div>
                        <div class="item-variant">{{ item.sizeName || '' }} {{ item.colorName || '' }}</div>
                      </div>
                    </div>
                  </td>
                  <td>{{ formatPrice(item.unitPrice) }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ formatPrice(item.finalAmount) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="3" class="text-right">Tạm tính:</td>
                  <td>{{ formatPrice(selectedOrder?.subtotalAmount) }}</td>
                </tr>
                <!-- ✅ HIỂN THỊ GIẢM GIÁ TỪ VOUCHER -->
                <tr v-if="selectedOrder?.discountAmount && selectedOrder?.discountAmount > 0" class="discount-row">
                  <td colspan="3" class="text-right">
                    <span class="voucher-label">
                      <i class="fas fa-ticket-alt"></i> Giảm giá 
                      ({{ selectedOrder?.voucher?.voucherCode || 'Voucher' }})
                    </span>
                  </td>
                  <td class="discount-amount">-{{ formatPrice(selectedOrder?.discountAmount) }}</td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right">Phí vận chuyển:</td>
                  <td>{{ formatPrice(selectedOrder?.shippingFee || 0) }}</td>
                </tr>
                <tr class="total-row">
                  <td colspan="3" class="text-right">Tổng cộng:</td>
                  <td class="grand-total">{{ formatPrice(selectedOrder?.totalAmount) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>

          <!-- ✅ HIỂN THỊ THÔNG TIN VOUCHER RIÊNG -->
          <div v-if="selectedOrder?.voucher" class="voucher-info-section">
            <h4><i class="fas fa-ticket-alt"></i> Mã giảm giá đã áp dụng</h4>
            <div class="voucher-detail">
              <span class="voucher-code">{{ selectedOrder.voucher.voucherCode }}</span>
              <span class="voucher-savings">Tiết kiệm: {{ formatPrice(selectedOrder.discountAmount || 0) }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeDetailModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Cancel Reason Modal -->
    <div v-if="showCancelModal" class="modal-overlay" @click.self="closeCancelModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Hủy đơn hàng</h3>
          <button class="close-btn" @click="closeCancelModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Lý do hủy</label>
            <textarea v-model="cancelReason" rows="3" placeholder="Nhập lý do hủy đơn hàng..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeCancelModal">Đóng</button>
          <button class="btn btn-danger" @click="confirmCancelOrder" :disabled="cancelLoading">
            {{ cancelLoading ? 'Đang xử lý...' : 'Xác nhận hủy' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div :class="['toast', { show: showToast, 'toast-error': toastType === 'error' }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import { useCartStore } from '../../stores/cartStore'
import { orderService } from '../../services/orderService'

const router = useRouter()
const cartStore = useCartStore()

const loading = ref(false)
const cancelLoading = ref(false)
const orders = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const activeTab = ref('all')

const showDetailModal = ref(false)
const showCancelModal = ref(false)
const selectedOrder = ref(null)
const cancelOrderId = ref(null)
const cancelReason = ref('')

const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

const tabs = [
  { value: 'all', label: 'Tất cả' },
  { value: 'PENDING_PAYMENT', label: 'Chờ thanh toán' },
  { value: 'PROCESSING', label: 'Đang xử lý' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'CANCELLED', label: 'Đã hủy' }
]

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
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy'
  }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    'PENDING_PAYMENT': 'status-pending',
    'PROCESSING': 'status-processing',
    'COMPLETED': 'status-completed',
    'CANCELLED': 'status-cancelled'
  }
  return map[status] || ''
}

const showNotification = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 3000)
}

// Fetch orders
const fetchOrders = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      showNotification('Vui lòng đăng nhập để xem đơn hàng', 'error')
      router.push('/login')
      return
    }

    const status = activeTab.value === 'all' ? null : activeTab.value
    const response = await orderService.getMyOrders(status, currentPage.value, 10)
    
    console.log('📦 Orders response:', response.data)
    
    if (response.data) {
      orders.value = response.data.content || []
      totalPages.value = response.data.totalPages || 0
      totalElements.value = response.data.totalElements || 0
      currentPage.value = response.data.number || 0
      console.log('✅ Loaded', orders.value.length, 'orders')
    }
  } catch (error) {
    console.error('Error fetching orders:', error)
    showNotification(error.response?.data?.message || 'Không thể tải đơn hàng', 'error')
    orders.value = []
  } finally {
    loading.value = false
  }
}

const changeTab = (tab) => {
  activeTab.value = tab
  currentPage.value = 0
  fetchOrders()
}

const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    fetchOrders()
  }
}

const viewOrderDetail = async (orderId) => {
  try {
    const response = await orderService.getOrderDetail(orderId)
    selectedOrder.value = response.data
    showDetailModal.value = true
  } catch (error) {
    console.error('Error fetching order detail:', error)
    showNotification('Không thể tải chi tiết đơn hàng', 'error')
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
}

const handleCancelOrder = (orderId) => {
  cancelOrderId.value = orderId
  cancelReason.value = ''
  showCancelModal.value = true
}

const closeCancelModal = () => {
  showCancelModal.value = false
  cancelOrderId.value = null
  cancelReason.value = ''
}

const confirmCancelOrder = async () => {
  if (!cancelReason.value.trim()) {
    showNotification('Vui lòng nhập lý do hủy đơn hàng', 'error')
    return
  }
  
  cancelLoading.value = true
  try {
    await orderService.cancelOrder(cancelOrderId.value, cancelReason.value)
    showNotification('Hủy đơn hàng thành công!', 'success')
    closeCancelModal()
    await fetchOrders()
  } catch (error) {
    console.error('Error canceling order:', error)
    showNotification(error.response?.data?.message || 'Không thể hủy đơn hàng', 'error')
  } finally {
    cancelLoading.value = false
  }
}

const handleReorder = (order) => {
  if (order.items && order.items.length > 0) {
    order.items.forEach(item => {
      cartStore.addToCart({
        productDetailId: item.productDetailId,
        productId: item.productId,
        productName: item.productName,
        price: item.unitPrice,
        quantity: item.quantity,
        imageUrl: item.imageUrl,
        sizeName: item.sizeName,
        colorName: item.colorName
      })
    })
    showNotification('Đã thêm sản phẩm vào giỏ hàng!', 'success')
    router.push('/cart')
  }
}

const displayedPages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const delta = 2
  const range = []
  const rangeWithDots = []
  let l

  for (let i = 0; i < total; i++) {
    if (i === 0 || i === total - 1 || (i >= current - delta && i <= current + delta)) {
      range.push(i)
    }
  }

  range.forEach((i) => {
    if (l) {
      if (i - l === 2) rangeWithDots.push(l + 1)
      else if (i - l !== 1) rangeWithDots.push('...')
    }
    rangeWithDots.push(i)
    l = i
  })

  return rangeWithDots
})

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: #f8f9fa;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

main {
  margin-top: 140px;
  padding: 20px 0 50px;
}

.page-title {
  font-size: 32px;
  color: #2c3e50;
  margin-bottom: 30px;
}

.order-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 25px;
  flex-wrap: wrap;
  background: white;
  padding: 10px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.tab-btn {
  padding: 10px 20px;
  background: none;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  color: #666;
  transition: all 0.3s;
}

.tab-btn:hover {
  background: #f0f0f0;
}

.tab-btn.active {
  background: #e63946;
  color: white;
}

.loading-state {
  text-align: center;
  padding: 60px;
  background: white;
  border-radius: 15px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #e63946;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 15px;
}

.empty-state i {
  font-size: 60px;
  color: #ccc;
  margin-bottom: 20px;
}

.empty-state h3 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.empty-state p {
  color: #666;
  margin-bottom: 25px;
}

.order-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
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
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
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
  color: #999;
  font-size: 14px;
}

.order-status {
  padding: 5px 15px;
  border-radius: 20px;
  font-size: 13px;
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

.status-completed {
  background: #d4edda;
  color: #155724;
}

.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}

.order-items {
  padding: 15px 0;
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
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-image i {
  font-size: 30px;
  color: #ccc;
}

.item-details {
  flex: 1;
}

.item-details h4 {
  font-size: 15px;
  color: #2c3e50;
  margin-bottom: 3px;
}

.item-variant {
  font-size: 13px;
  color: #999;
}

.item-quantity {
  font-size: 13px;
  color: #999;
}

.item-price {
  font-weight: 600;
  color: #e63946;
  font-size: 15px;
}

/* ✅ VOUCHER STYLES */
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
  padding-top: 15px;
  border-top: 1px solid #eee;
  flex-wrap: wrap;
  gap: 15px;
}

.order-total {
  font-size: 16px;
}

.order-total strong {
  color: #e63946;
  font-size: 18px;
  margin-left: 5px;
}

.order-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-outline {
  background: #f0f0f0;
  color: #666;
}

.btn-outline:hover {
  background: #e0e0e0;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover {
  background: #c62828;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 30px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
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

.page-btn.active {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Modal Styles */
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
  max-height: 90vh;
  overflow-y: auto;
  animation: slideIn 0.3s;
}

.modal-container.modal-sm {
  max-width: 450px;
}

.modal-container.modal-lg {
  max-width: 800px;
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
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border-radius: 12px 12px 0 0;
  position: sticky;
  top: 0;
  z-index: 1;
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
  border-radius: 0 0 12px 12px;
}

.order-info-grid {
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

.info-section h4 {
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

.order-items-section h4 {
  color: #2c3e50;
  margin-bottom: 15px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
}

.items-table th {
  background: #f8f9fa;
  padding: 10px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
}

.items-table td {
  padding: 10px;
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

/* ✅ Discount row */
.discount-row td {
  color: #28a745;
  font-weight: 600;
}

.discount-row .voucher-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.discount-row .discount-amount {
  color: #28a745;
  font-weight: 700;
}

/* ✅ Voucher info section */
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

.item-info .item-image {
  width: 40px;
  height: 40px;
}

.item-info .item-image i {
  font-size: 20px;
}

.item-info .item-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.item-info .item-variant {
  font-size: 12px;
  color: #999;
}

.toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  padding: 12px 24px;
  border-radius: 8px;
  background: #27ae60;
  color: white;
  font-size: 14px;
  z-index: 9999;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  padding: 12px 24px;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.3);
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .page-title {
    font-size: 24px;
  }

  .order-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
  }

  .order-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .order-actions {
    width: 100%;
  }

  .order-actions .btn {
    flex: 1;
    justify-content: center;
  }

  .order-info-grid {
    grid-template-columns: 1fr;
  }

  .modal-container.modal-lg {
    max-width: 100%;
    margin: 10px;
  }

  .order-voucher {
    flex-wrap: wrap;
  }
  
  .order-voucher .voucher-discount {
    margin-left: 0;
  }
}
</style>