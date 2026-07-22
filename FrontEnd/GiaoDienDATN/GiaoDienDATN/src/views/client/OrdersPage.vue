<template>
  <div class="orders-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <h1 class="page-title">ĐƠN HÀNG CỦA TÔI</h1>

        <!-- ===== ORDER TABS ===== -->
        <OrderTabs 
          ref="orderTabsRef"
          :orders="orders"
          @change-tab="onTabChange"
        />

        <!-- ===== ORDER LIST ===== -->
        <OrderList
          :orders="filteredOrders"
          :loading="loading"
          :current-page="currentPage"
          :total-pages="totalPages"
          :show-review-actions="false"
          @view-detail="viewOrderDetail"
          @cancel-order="handleCancelOrder"
          @return-order="handleReturnOrder"
          @reorder="handleReorder"
          @change-page="goToPage"
        />
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

          <!-- ===== ĐỊA CHỈ GIAO HÀNG - HIỂN THỊ CHI TIẾT ===== -->
          <div class="address-section">
            <h4>📍 Địa chỉ giao hàng</h4>
            <div class="address-detail-card">
              <div class="address-row">
                <span class="address-label">👤 Người nhận:</span>
                <span class="address-value">{{ selectedOrder?.shippingAddress?.fullName || '---' }}</span>
              </div>
              <div class="address-row">
                <span class="address-label">📞 Số điện thoại:</span>
                <span class="address-value">{{ selectedOrder?.shippingAddress?.phone || '---' }}</span>
              </div>
              <div class="address-row full-address-row">
                <span class="address-label">📦 Địa chỉ đầy đủ:</span>
                <span class="address-value highlight">{{ getOrderFullAddress(selectedOrder?.shippingAddress) }}</span>
              </div>
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
                        <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" />
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
            <label>Lý do hủy <span class="required">*</span></label>
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

    <!-- Return Request Modal -->
    <ReturnRequestModal
      :visible="showReturnModal"
      :order="selectedOrderForReturn"
      @close="closeReturnModal"
      @submitted="onReturnSubmitted"
    />

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
import OrderTabs from '../../components/client/OrderTabs.vue'
import OrderList from '../../components/client/OrderList.vue'
import ReturnRequestModal from '../../components/client/ReturnRequestModal.vue'
import { useCartStore } from '../../stores/cartStore'
import { orderService } from '../../services/orderService'
import { useNotification } from '../../composables/useNotification'

const router = useRouter()
const cartStore = useCartStore()
const { showNotification } = useNotification()

// ===== CONSTANTS =====
const API_BASE = "http://localhost:8080/api"
const IMAGE_BASE = "http://localhost:8080"

// ===== STATE =====
const loading = ref(false)
const cancelLoading = ref(false)
const orders = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const orderTabsRef = ref(null)

// ===== TAB STATE =====
const activeMainTab = ref('all')
const activeSubTab = ref('all')

// ===== MODAL STATE =====
const showDetailModal = ref(false)
const showCancelModal = ref(false)
const showReturnModal = ref(false)
const selectedOrder = ref(null)
const selectedOrderForReturn = ref(null)
const cancelOrderId = ref(null)
const cancelReason = ref('')

// ===== TOAST =====
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

// ===== HELPERS =====
const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
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

// ===== GET ORDER FULL ADDRESS =====
const getOrderFullAddress = (shippingAddress) => {
  if (!shippingAddress) return '---'
  
  // Nếu đã có addressLine (backend đã gộp) thì dùng luôn
  if (shippingAddress.addressLine) {
    return shippingAddress.addressLine
  }
  
  // Nếu không, tự gộp từ các thành phần
  const parts = []
  if (shippingAddress.addressLine) parts.push(shippingAddress.addressLine)
  if (shippingAddress.ward) parts.push(shippingAddress.ward)
  if (shippingAddress.city) parts.push(shippingAddress.city)
  
  return parts.length > 0 ? parts.join(', ') : '---'
}

const showNotificationMsg = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 3000)
}

// ===== FILTER ORDERS =====
const filteredOrders = computed(() => {
  let result = [...orders.value]
  
  if (activeMainTab.value === 'orders') {
    result = result.filter(o => 
      !['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'REJECTED', 'CANCELLED'].includes(o.orderStatus)
    )
  } else if (activeMainTab.value === 'returns') {
    result = result.filter(o => 
      ['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'REJECTED'].includes(o.orderStatus)
    )
  } else if (activeMainTab.value === 'cancelled') {
    result = result.filter(o => o.orderStatus === 'CANCELLED')
  }
  
  if (activeSubTab.value !== 'all' && activeMainTab.value !== 'all') {
    result = result.filter(o => o.orderStatus === activeSubTab.value)
  }
  
  return result
})

// ===== TAB HANDLER =====
const onTabChange = ({ mainTab, subTab }) => {
  activeMainTab.value = mainTab
  activeSubTab.value = subTab
  currentPage.value = 0
  fetchOrders()
}

// ===== FETCH ORDERS =====
const fetchOrders = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      showNotificationMsg('Vui lòng đăng nhập để xem đơn hàng', 'error')
      router.push('/login')
      return
    }

    let statusFilter = null
    
    if (activeMainTab.value === 'orders') {
      if (activeSubTab.value === 'all') {
        statusFilter = 'PENDING_PAYMENT,CONFIRMED,PROCESSING,SHIPPING,DELIVERED,COMPLETED'
      } else {
        statusFilter = activeSubTab.value
      }
    } else if (activeMainTab.value === 'returns') {
      if (activeSubTab.value === 'all') {
        statusFilter = 'RETURN_REQUESTED,RETURN_APPROVED,RETURN_RECEIVED,RETURN_PENDING,RETURNED,REJECTED'
      } else {
        statusFilter = activeSubTab.value
      }
    } else if (activeMainTab.value === 'cancelled') {
      statusFilter = 'CANCELLED'
    }

    const response = await orderService.getMyOrders(statusFilter, currentPage.value, 10)
    
    console.log('📦 Orders response:', response.data)
    
    if (response.data) {
      orders.value = response.data.content || []
      totalPages.value = response.data.totalPages || 0
      currentPage.value = response.data.number || 0
    }
  } catch (error) {
    console.error('Error fetching orders:', error)
    showNotificationMsg(error.response?.data?.message || 'Không thể tải đơn hàng', 'error')
    orders.value = []
  } finally {
    loading.value = false
  }
}

// ===== PAGINATION =====
const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    fetchOrders()
  }
}

// ===== ORDER DETAIL =====
const viewOrderDetail = async (orderId) => {
  try {
    const response = await orderService.getOrderDetail(orderId)
    selectedOrder.value = response.data
    showDetailModal.value = true
  } catch (error) {
    console.error('Error fetching order detail:', error)
    showNotificationMsg('Không thể tải chi tiết đơn hàng', 'error')
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
}

// ===== CANCEL ORDER =====
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
    showNotificationMsg('Vui lòng nhập lý do hủy đơn hàng', 'error')
    return
  }
  
  cancelLoading.value = true
  try {
    await orderService.cancelOrder(cancelOrderId.value, cancelReason.value)
    showNotificationMsg('Hủy đơn hàng thành công!', 'success')
    closeCancelModal()
    await fetchOrders()
  } catch (error) {
    console.error('Error canceling order:', error)
    showNotificationMsg(error.response?.data?.message || 'Không thể hủy đơn hàng', 'error')
  } finally {
    cancelLoading.value = false
  }
}

// ===== RETURN ORDER =====
const handleReturnOrder = (order) => {
  if (!order) {
    showNotificationMsg('Không tìm thấy đơn hàng', 'error')
    return
  }
  
  if (order.orderStatus !== 'COMPLETED') {
    showNotificationMsg('Chỉ có thể yêu cầu hoàn hàng khi đơn hàng đã hoàn thành', 'warning')
    return
  }
  
  selectedOrderForReturn.value = order
  showReturnModal.value = true
}

const closeReturnModal = () => {
  showReturnModal.value = false
  selectedOrderForReturn.value = null
}

const onReturnSubmitted = (data) => {
  showNotificationMsg('Yêu cầu hoàn hàng đã được gửi!', 'success')
  closeReturnModal()
  fetchOrders()
}

// ===== REORDER =====
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
    showNotificationMsg('Đã thêm sản phẩm vào giỏ hàng!', 'success')
    router.push('/cart')
  }
}

// ===== LIFECYCLE =====
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

/* ===== MODAL ===== */
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

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

/* ===== ORDER INFO GRID ===== */
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

/* ===== ADDRESS SECTION ===== */
.address-section {
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #e9ecef;
}

.address-section h4 {
  margin-top: 0;
  margin-bottom: 12px;
  color: #2c3e50;
  font-size: 15px;
}

.address-detail-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.address-row {
  display: flex;
  align-items: flex-start;
  padding: 4px 0;
  font-size: 14px;
}

.address-row .address-label {
  font-weight: 600;
  color: #2c3e50;
  min-width: 120px;
  flex-shrink: 0;
}

.address-row .address-value {
  color: #444;
  word-break: break-word;
}

.address-row.full-address-row {
  background: #e8f0fe;
  padding: 8px 12px;
  border-radius: 6px;
  margin-top: 4px;
}

.address-row.full-address-row .address-label {
  color: #1a3a6a;
}

.address-row.full-address-row .address-value.highlight {
  color: #1a3a6a;
  font-weight: 500;
}

/* ===== ORDER ITEMS ===== */
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

/* ===== FORM ===== */
.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 5px;
}

.required {
  color: #e63946;
}

.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}

.form-group textarea:focus {
  outline: none;
  border-color: #e63946;
}

/* ===== TOAST ===== */
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

/* ===== RESPONSIVE ===== */
@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .page-title {
    font-size: 24px;
  }

  .order-info-grid {
    grid-template-columns: 1fr;
  }

  .modal-container.modal-lg {
    max-width: 100%;
    margin: 10px;
  }

  .address-row {
    flex-direction: column;
    padding: 2px 0;
  }
  
  .address-row .address-label {
    min-width: auto;
    font-size: 12px;
  }
  
  .address-row .address-value {
    font-size: 13px;
    padding-left: 10px;
  }
}
</style>