<template>
  <div class="admin-orders">
    <div class="admin-header">
      <h1 class="page-title">Quản lý đơn hàng</h1>
      <div class="header-actions">
        <button class="btn-refresh" @click="fetchOrders">
          <i class="fas fa-sync-alt"></i> Làm mới
        </button>
      </div>
    </div>

    <!-- Thống kê nhanh -->
    <div class="stats-grid" v-if="statistics">
      <div class="stat-card">
        <div class="stat-icon blue">
          <i class="fas fa-shopping-bag"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.totalOrders || 0 }}</span>
          <span class="stat-label">Tổng đơn hàng</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon yellow">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.pendingOrders || 0 }}</span>
          <span class="stat-label">Chờ xử lý</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.completedOrders || 0 }}</span>
          <span class="stat-label">Hoàn thành</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">
          <i class="fas fa-times-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.cancelledOrders || 0 }}</span>
          <span class="stat-label">Đã hủy</span>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="filters.search"
          placeholder="Tìm kiếm theo mã đơn, tên khách hàng..."
          @input="applyFilters"
        >
      </div>

      <select v-model="filters.status" @change="applyFilters">
        <option value="all">Tất cả trạng thái</option>
        <option value="PENDING_PAYMENT">Chờ thanh toán</option>
        <option value="PROCESSING">Đang xử lý</option>
        <option value="SHIPPING">Đang giao</option>
        <option value="COMPLETED">Hoàn thành</option>
        <option value="CANCELLED">Đã hủy</option>
      </select>

      <button class="btn-search" @click="fetchOrders">
        <i class="fas fa-search"></i> Tìm kiếm
      </button>
      <button class="btn-reset" @click="resetFilters">
        <i class="fas fa-sync-alt"></i> Làm mới
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Đang tải đơn hàng...</p>
    </div>

    <!-- Orders Table -->
    <div v-else class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>Mã đơn</th>
            <th>Khách hàng</th>
            <th>SĐT</th>
            <th>Sản phẩm</th>
            <th>Tạm tính</th>
            <th>Giảm giá</th>
            <th>Tổng tiền</th>
            <th>Ngày đặt</th>
            <th>Trạng thái</th>
            <th>Thanh toán</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.orderId">
            <td>
              <strong>#{{ order.orderCode || order.orderId }}</strong>
            </td>
            <td>
              <div class="customer-info">
                <div class="customer-name">{{ order.customer?.customerName || 'Khách vãng lai' }}</div>
              </div>
            </td>
            <td>{{ order.customer?.phone || '---' }}</td>
            <td>
              <div class="product-count">{{ order.items?.length || 0 }} sản phẩm</div>
              <button class="view-items-btn" @click="viewOrderItems(order)" title="Xem chi tiết">
                <i class="fas fa-eye"></i>
              </button>
            </td>
            <td class="price-cell">{{ formatPrice(order.subtotalAmount) }}</td>
            <td class="discount-cell">
              <span v-if="order.discountAmount && order.discountAmount > 0" class="discount-amount">
                -{{ formatPrice(order.discountAmount) }}
              </span>
              <span v-else class="no-discount">0₫</span>
              <div v-if="order.voucher" class="order-voucher-tag">
                <i class="fas fa-ticket-alt"></i>
                <span>{{ order.voucher.voucherCode }}</span>
              </div>
            </td>
            <td>
              <span class="order-total">{{ formatPrice(order.totalAmount) }}</span>
            </td>
            <td>{{ formatDate(order.orderDate) }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(order.orderStatus)]">
                {{ getStatusText(order.orderStatus) }}
              </span>
            </td>
            <td>
              <span class="payment-status" :class="getPaymentClass(order.paymentStatus)">
                {{ getPaymentStatusText(order.paymentStatus) }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button 
                  class="action-btn update-status-btn" 
                  @click="openUpdateStatusModal(order)"
                  :disabled="updatingOrderIds.has(order.orderId)"
                  title="Cập nhật trạng thái"
                >
                  <i class="fas fa-edit"></i>
                </button>
                <button class="action-btn view-btn" @click="viewOrderDetail(order)" title="Xem chi tiết">
                  <i class="fas fa-eye"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="orders.length === 0 && !loading">
            <td colspan="11" class="no-data">
              <i class="fas fa-box-open"></i>
              <p>Không tìm thấy đơn hàng nào</p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 1">
      <button class="page-btn" :disabled="currentPage === 0" @click="goToPage(currentPage - 1)">
        <i class="fas fa-chevron-left"></i>
      </button>
      <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button class="page-btn" :disabled="currentPage === totalPages - 1" @click="goToPage(currentPage + 1)">
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- Order Detail Modal -->
    <div v-if="showDetailModal" class="modal" @click.self="closeDetailModal">
      <div class="modal-content modal-lg">
        <div class="modal-header">
          <h2>Chi tiết đơn hàng #{{ selectedOrder?.orderCode || selectedOrder?.orderId }}</h2>
          <button class="close-btn" @click="closeDetailModal">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="modal-body" v-if="selectedOrder">
          <div class="order-info-grid">
            <div class="info-section">
              <h3>Thông tin khách hàng</h3>
              <p><strong>Họ tên:</strong> {{ selectedOrder.customer?.customerName || 'Khách vãng lai' }}</p>
              <p><strong>SĐT:</strong> {{ selectedOrder.customer?.phone || '---' }}</p>
              <p><strong>Email:</strong> {{ selectedOrder.customer?.email || '---' }}</p>
            </div>

            <div class="info-section">
              <h3>Địa chỉ giao hàng</h3>
              <p>{{ selectedOrder.shippingAddress?.addressLine || '---' }}</p>
            </div>

            <div class="info-section">
              <h3>Thông tin đơn hàng</h3>
              <p><strong>Ngày đặt:</strong> {{ formatDate(selectedOrder.orderDate) }}</p>
              <p><strong>Trạng thái:</strong> {{ getStatusText(selectedOrder.orderStatus) }}</p>
              <p><strong>Thanh toán:</strong> {{ getPaymentStatusText(selectedOrder.paymentStatus) }}</p>
              <p v-if="selectedOrder.voucher">
                <strong>Voucher:</strong> 
                <span class="voucher-code-admin">{{ selectedOrder.voucher.voucherCode }}</span>
                <span class="voucher-discount-admin">(-{{ formatPrice(selectedOrder.discountAmount || 0) }})</span>
              </p>
            </div>
          </div>

          <div class="order-items-section">
            <h3>Sản phẩm đã đặt</h3>
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
                <tr v-for="item in selectedOrder.items" :key="item.itemId">
                  <td>
                    <div class="item-info">
                      <div class="item-image">
                        <img v-if="item.imageUrl" :src="item.imageUrl" alt="product" />
                        <i v-else class="fas fa-tshirt"></i>
                      </div>
                      <div>
                        <div class="item-name">{{ item.productName }}</div>
                        <div class="item-variant">
                          {{ item.sizeName || '' }} {{ item.colorName || '' }}
                        </div>
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
                  <td class="total-price">{{ formatPrice(selectedOrder.subtotalAmount) }}</td>
                </tr>
                <tr v-if="selectedOrder.discountAmount && selectedOrder.discountAmount > 0" class="discount-row-admin">
                  <td colspan="3" class="text-right">
                    <span class="voucher-label-admin">
                      <i class="fas fa-ticket-alt"></i> Giảm giá 
                      ({{ selectedOrder.voucher?.voucherCode || 'Voucher' }})
                    </span>
                  </td>
                  <td class="discount-amount-admin">-{{ formatPrice(selectedOrder.discountAmount) }}</td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right">Phí vận chuyển:</td>
                  <td class="total-price">{{ formatPrice(selectedOrder.shippingFee || 0) }}</td>
                </tr>
                <tr class="total-row">
                  <td colspan="3" class="text-right">Tổng cộng:</td>
                  <td class="total-price grand-total">{{ formatPrice(selectedOrder.totalAmount) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div class="order-notes" v-if="selectedOrder.notes">
            <h3>Ghi chú</h3>
            <p>{{ selectedOrder.notes }}</p>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeDetailModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Order Items Modal -->
    <div v-if="showItemsModal" class="modal" @click.self="showItemsModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Sản phẩm trong đơn hàng #{{ selectedOrder?.orderCode || selectedOrder?.orderId }}</h2>
          <button class="close-btn" @click="showItemsModal = false">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="modal-body">
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
                      <img v-if="item.imageUrl" :src="item.imageUrl" alt="product" />
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
          </table>
        </div>

        <div class="modal-footer">
          <button class="btn btn-primary" @click="showItemsModal = false">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Update Status Modal -->
    <UpdateStatusModal
      :visible="showUpdateModal"
      :order="selectedOrderForUpdate"
      @close="closeUpdateStatusModal"
      @updated="onStatusUpdated"
    />

    <!-- Toast Notification -->
    <div :class="['toast', { show: showToast, 'toast-error': toastType === 'error' }]">
      <i :class="toastType === 'error' ? 'fas fa-exclamation-circle' : 'fas fa-check-circle'"></i>
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminOrderService } from '../../services/adminOrderService'
import UpdateStatusModal from '../../components/admin/UpdateStatusModal.vue'

// ===== STATE =====
const loading = ref(false)
const orders = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = ref(10)
const statistics = ref(null)
const updatingOrderIds = ref(new Set())

// Filters
const filters = ref({
  search: '',
  status: 'all'
})

// Modal state
const showDetailModal = ref(false)
const showItemsModal = ref(false)
const selectedOrder = ref(null)

// Update Status Modal
const showUpdateModal = ref(false)
const selectedOrderForUpdate = ref(null)

// Toast
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

// ===== UTILITY FUNCTIONS =====
const showNotification = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 3000)
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

const getPaymentStatusText = (status) => {
  const map = {
    'PAID': 'Đã thanh toán',
    'UNPAID': 'Chưa thanh toán'
  }
  return map[status] || status
}

const getPaymentClass = (status) => {
  const map = {
    'PAID': 'payment-paid',
    'UNPAID': 'payment-unpaid'
  }
  return map[status] || ''
}

// ===== API CALLS =====
const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    
    if (filters.value.search) {
      params.keyword = filters.value.search
    }
    if (filters.value.status !== 'all') {
      params.status = filters.value.status
    }
    
    console.log('📤 Fetching orders with params:', params)
    
    const response = await adminOrderService.getAllOrders(params)
    
    console.log('📦 Orders response:', response.data)
    
    if (response.data && response.data.content) {
      orders.value = response.data.content
      
      orders.value.forEach(order => {
        console.log(`📋 Order #${order.orderId}:`, {
          subtotal: order.subtotalAmount,
          discount: order.discountAmount,
          total: order.totalAmount,
          voucher: order.voucher?.voucherCode || 'Không có'
        })
      })
      
      totalPages.value = response.data.totalPages || 0
      totalElements.value = response.data.totalElements || 0
      currentPage.value = response.data.number || 0
    } else if (Array.isArray(response.data)) {
      orders.value = response.data
      totalPages.value = 1
    } else {
      orders.value = []
    }
  } catch (error) {
    console.error('❌ Error fetching orders:', error)
    showNotification(error.response?.data?.message || 'Không thể tải danh sách đơn hàng', 'error')
    orders.value = []
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const response = await adminOrderService.getStatistics()
    if (response.data) {
      statistics.value = response.data
    }
  } catch (error) {
    console.error('❌ Error fetching statistics:', error)
  }
}

// ===== UPDATE STATUS =====
const openUpdateStatusModal = (order) => {
  selectedOrderForUpdate.value = order
  showUpdateModal.value = true
}

const closeUpdateStatusModal = () => {
  showUpdateModal.value = false
  selectedOrderForUpdate.value = null
}

const onStatusUpdated = async (data) => {
  showNotification(`✅ Đã cập nhật trạng thái đơn hàng #${data.orderId} thành "${getStatusText(data.newStatus)}"`, 'success')
  await fetchOrders()
  await fetchStatistics()
}

// ===== VIEW DETAILS =====
const viewOrderDetail = async (order) => {
  try {
    const response = await adminOrderService.getOrderDetail(order.orderId)
    selectedOrder.value = response.data
    showDetailModal.value = true
  } catch (error) {
    console.error('❌ Error fetching order detail:', error)
    showNotification('Không thể tải chi tiết đơn hàng', 'error')
  }
}

const viewOrderItems = (order) => {
  selectedOrder.value = order
  showItemsModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
}

// ===== PAGINATION =====
const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    fetchOrders()
  }
}

// ===== FILTERS =====
const applyFilters = () => {
  currentPage.value = 0
  fetchOrders()
}

const resetFilters = () => {
  filters.value = {
    search: '',
    status: 'all'
  }
  currentPage.value = 0
  fetchOrders()
}

// ===== LIFECYCLE =====
onMounted(() => {
  fetchOrders()
  fetchStatistics()
})
</script>

<style scoped>
.admin-orders {
  padding: 20px;
  animation: fadeIn 0.3s;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.page-title {
  font-size: 28px;
  color: #2c3e50;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.btn-refresh {
  padding: 10px 20px;
  background: #2c3e50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-refresh:hover {
  background: #1a252f;
}

/* Stats */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 25px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 15px;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
}

.stat-icon.blue { background: #3498db; }
.stat-icon.yellow { background: #f39c12; }
.stat-icon.green { background: #2ecc71; }
.stat-icon.red { background: #e74c3c; }

.stat-info {
  flex: 1;
}

.stat-number {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

/* Filters */
.filters-bar {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  align-items: center;
}

.search-box {
  flex: 1;
  min-width: 250px;
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 0 15px;
}

.search-box i {
  color: #999;
}

.search-box input {
  border: none;
  background: none;
  padding: 12px;
  width: 100%;
  outline: none;
}

.filters-bar select {
  padding: 12px 15px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  outline: none;
  min-width: 150px;
  cursor: pointer;
  background: white;
}

.btn-search, .btn-reset {
  padding: 12px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-search {
  background: #e63946;
  color: white;
}

.btn-search:hover {
  background: #c82333;
}

.btn-reset {
  background: #f0f0f0;
  color: #666;
}

.btn-reset:hover {
  background: #e0e0e0;
}

/* Loading */
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

/* Table */
.table-container {
  background: white;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
  margin-bottom: 20px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1100px;
}

.data-table th {
  background: #f8f9fa;
  padding: 12px 15px;
  text-align: left;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
  font-size: 13px;
}

.data-table td {
  padding: 12px 15px;
  border-bottom: 1px solid #eee;
  color: #666;
  font-size: 13px;
}

.data-table tr:hover {
  background: #fafafa;
}

.customer-name {
  font-weight: 600;
  color: #2c3e50;
}

.product-count {
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 5px;
}

.view-items-btn {
  background: none;
  border: none;
  color: #e63946;
  cursor: pointer;
  font-size: 14px;
  padding: 5px;
  transition: all 0.3s;
}

.view-items-btn:hover {
  transform: scale(1.1);
}

.order-total {
  font-weight: 700;
  color: #e63946;
}

.price-cell {
  font-weight: 500;
  color: #2c3e50;
}

.discount-cell {
  text-align: center;
}

.discount-amount {
  color: #28a745;
  font-weight: 700;
}

.no-discount {
  color: #999;
  font-size: 12px;
}

.order-voucher-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #e8f5e9;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  color: #2e7d32;
  margin-top: 4px;
  font-weight: 600;
}

.order-voucher-tag i {
  font-size: 10px;
}

/* Status Badge */
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

.payment-status {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.payment-status.payment-paid {
  background: #d4edda;
  color: #155724;
}

.payment-status.payment-unpaid {
  background: #fff3cd;
  color: #856404;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 35px;
  height: 35px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.update-status-btn {
  background: #e63946;
  color: white;
}

.update-status-btn:hover:not(:disabled) {
  background: #c82333;
  transform: translateY(-2px);
}

.update-status-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.view-btn {
  background: #17a2b8;
  color: white;
}

.view-btn:hover {
  background: #138496;
  transform: translateY(-2px);
}

.no-data {
  text-align: center;
  padding: 40px;
  color: #999;
}

.no-data i {
  font-size: 40px;
  display: block;
  margin-bottom: 10px;
  color: #ddd;
}

/* Pagination */
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

/* Modal */
.modal {
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

.modal-content {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s;
}

.modal-content.modal-lg {
  max-width: 1000px;
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

.voucher-code-admin {
  background: #e8f5e9;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
  color: #2e7d32;
}

.voucher-discount-admin {
  color: #e63946;
  font-weight: 600;
  margin-left: 4px;
}

/* Discount row admin */
.discount-row-admin td {
  color: #28a745;
  font-weight: 600;
}

.discount-row-admin .voucher-label-admin {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.discount-row-admin .discount-amount-admin {
  color: #28a745;
  font-weight: 700;
}

.order-items-section {
  margin-bottom: 30px;
}

.order-items-section h3 {
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
  margin-bottom: 3px;
}

.item-variant {
  font-size: 12px;
  color: #999;
}

.price-cell, .quantity-cell, .total-cell {
  text-align: right;
}

.text-right {
  text-align: right;
}

.total-price {
  font-weight: 600;
  color: #e63946;
}

.grand-total {
  font-size: 18px;
  font-weight: 700;
}

.total-row {
  font-weight: 700;
  background: #fafafa;
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
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover {
  background: #c82333;
}

.btn-outline {
  background: none;
  border: 1px solid #ddd;
  color: #666;
}

.btn-outline:hover {
  border-color: #e63946;
  color: #e63946;
}

/* Toast */
.toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  padding: 12px 24px;
  border-radius: 10px;
  background: #27ae60;
  color: white;
  font-size: 14px;
  z-index: 9999;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.4s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 10px;
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}

/* Animations */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(50px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Responsive */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .admin-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .page-title {
    font-size: 24px;
  }

  .filters-bar {
    flex-direction: column;
  }

  .filters-bar select,
  .btn-search,
  .btn-reset {
    width: 100%;
  }

  .order-info-grid {
    grid-template-columns: 1fr;
  }

  .modal-content {
    width: 95%;
  }

  .stats-grid {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .stat-card {
    padding: 15px;
  }

  .stat-number {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
    align-items: center;
  }
  
  .data-table {
    font-size: 12px;
  }
  
  .data-table th,
  .data-table td {
    padding: 8px 10px;
  }
}
</style>