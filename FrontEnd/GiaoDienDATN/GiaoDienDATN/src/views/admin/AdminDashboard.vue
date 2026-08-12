<!-- AdminDashboard.vue -->
<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <h1 class="page-title">📊 Tổng quan</h1>
      <div class="header-actions">
        <button class="btn-refresh" @click="refreshAllData" :disabled="loading">
          <i class="fas fa-sync-alt" :class="{ spinning: loading }"></i>
          {{ loading ? 'Đang tải...' : 'Làm mới' }}
        </button>
        <span class="last-update" v-if="lastUpdated">
          Cập nhật: {{ formatDate(lastUpdated) }}
        </span>
      </div>
    </div>

    <!-- ===== LOADING ===== -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Đang tải dữ liệu thống kê...</p>
    </div>

    <!-- ===== DASHBOARD CONTENT ===== -->
    <template v-else>
      <!-- ===== 1. STATS CARDS ===== -->
      <div class="stats-grid">
        <StatsCard
          title="Tổng sản phẩm"
          :value="productStats?.totalProducts || 0"
          icon="fas fa-tshirt"
          color="#e63946"
          :loading="loadingProduct"
        />
        <StatsCard
          title="Tổng khách hàng"
          :value="customerStats?.totalCustomers || 0"
          icon="fas fa-users"
          color="#3498db"
          :loading="loadingCustomer"
        />
        <StatsCard
          title="Tổng đơn hàng"
          :value="orderStats?.totalOrders || 0"
          icon="fas fa-shopping-bag"
          color="#6c5ce7"
          :loading="loadingOrder"
        />
        <StatsCard
          title="Doanh thu online"
          :value="formatPrice(orderStats?.onlineRevenue || 0)"
          icon="fas fa-chart-line"
          color="#00b894"
          :loading="loadingOrder"
          :trend="orderStats?.onlineRevenueGrowth"
        />
        <StatsCard
          title="Doanh thu thực tế"
          :value="formatPrice(orderStats?.netRevenue || 0)"
          icon="fas fa-hand-holding-usd"
          color="#f39c12"
          :loading="loadingOrder"
          :trend="orderStats?.netRevenueGrowth"
        />
        <StatsCard
          title="Đơn hoàn thành"
          :value="orderStats?.completedOrders || 0"
          icon="fas fa-check-circle"
          color="#2ecc71"
          :loading="loadingOrder"
        />
      </div>

      <!-- ===== 2. DOANH THU & TOP SẢN PHẨM ===== -->
      <div class="charts-row">
        <div class="chart-card">
          <div class="chart-header">
            <h3>📈 Doanh thu theo tháng</h3>
            <select v-model="selectedYear" @change="fetchMonthlyRevenue">
              <option v-for="year in availableYears" :key="year" :value="year">
                {{ year }}
              </option>
            </select>
          </div>
          <RevenueChart :data="monthlyRevenueData" :loading="loadingRevenue" />
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <h3>🏆 Top sản phẩm bán chạy</h3>
          </div>
          <div class="top-products">
            <div
              v-for="(product, index) in topProducts"
              :key="product.productId"
              class="top-product-item"
            >
              <div class="product-rank" :class="{ 'rank-1': index === 0 }">
                {{ index + 1 }}
              </div>
              <div class="product-info">
                <h4>{{ product.productName }}</h4>
                <p>Đã bán: {{ product.totalSold || 0 }}</p>
              </div>
              <div class="product-revenue">{{ formatPrice(product.revenue || 0) }}</div>
            </div>
            <div v-if="topProducts.length === 0" class="no-data-text">
              Chưa có dữ liệu bán hàng
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 3. PHÂN BỔ TRẠNG THÁI ===== -->
      <div class="charts-row two-col">
        <div class="chart-card">
          <div class="chart-header">
            <h3>📦 Phân bổ đơn hàng theo trạng thái</h3>
          </div>
          <div class="status-distribution">
            <div
              v-for="(count, status) in orderStatusCounts"
              :key="status"
              class="status-item"
            >
              <span class="status-label">
                <span :class="['status-dot', getStatusClass(status)]"></span>
                {{ getStatusText(status) }}
              </span>
              <div class="status-bar-wrapper">
                <div
                  class="status-bar"
                  :style="{ width: getStatusPercent(status) + '%' }"
                  :class="getStatusClass(status)"
                ></div>
              </div>
              <span class="status-count">{{ count }}</span>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <h3>👥 Phân bổ khách hàng</h3>
          </div>
          <div class="customer-distribution">
            <div class="customer-stat-item">
              <span class="stat-label">🟢 Đang hoạt động</span>
              <span class="stat-value">{{ customerStats?.activeCustomers || 0 }}</span>
              <span class="stat-percent">
                {{ getCustomerPercent(customerStats?.activeCustomers) }}%
              </span>
            </div>
            <div class="customer-stat-item">
              <span class="stat-label">🔴 Tạm khóa</span>
              <span class="stat-value">{{ customerStats?.lockedCustomers || 0 }}</span>
              <span class="stat-percent">
                {{ getCustomerPercent(customerStats?.lockedCustomers) }}%
              </span>
            </div>
            <div class="customer-stat-item">
              <span class="stat-label">⚫ Đã xóa</span>
              <span class="stat-value">{{ customerStats?.deletedCustomers || 0 }}</span>
              <span class="stat-percent">
                {{ getCustomerPercent(customerStats?.deletedCustomers) }}%
              </span>
            </div>
            <div class="customer-total">
              <span class="stat-label">Tổng cộng</span>
              <span class="stat-value total">{{ customerStats?.totalCustomers || 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 4. ĐƠN HÀNG GẦN ĐÂY ===== -->
      <div class="recent-orders">
        <div class="section-header">
          <h3>🕐 Đơn hàng gần đây</h3>
          <router-link to="/admin/orders" class="view-all">
            Xem tất cả <i class="fas fa-arrow-right"></i>
          </router-link>
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Khách hàng</th>
                <th>Tổng tiền</th>
                <th>Ngày đặt</th>
                <th>Trạng thái</th>
                <th>Thanh toán</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in recentOrders" :key="order.orderId">
                <td><strong>#{{ order.orderCode || order.orderId }}</strong></td>
                <td>{{ order.customer?.customerName || 'Khách vãng lai' }}</td>
                <td class="order-total">{{ formatPrice(order.totalAmount) }}</td>
                <td>{{ formatDate(order.orderDate) }}</td>
                <td>
                  <span :class="['status-badge', getStatusClass(order.orderStatus)]">
                    {{ getStatusText(order.orderStatus) }}
                  </span>
                </td>
                <td>
                  <span :class="['payment-status', getPaymentClass(order.paymentStatus)]">
                    {{ getPaymentStatusText(order.paymentStatus) }}
                  </span>
                </td>
                <td>
                  <button class="action-btn view-btn" @click="viewOrderDetail(order.orderId)" title="Xem chi tiết">
                    <i class="fas fa-eye"></i>
                  </button>
                </td>
              </tr>
              <tr v-if="recentOrders.length === 0">
                <td colspan="7" class="no-data">
                  <i class="fas fa-box-open"></i>
                  <p>Chưa có đơn hàng nào</p>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ===== ORDER DETAIL MODAL ===== -->
    <div v-if="showDetailModal" class="modal-overlay">
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
              <p><strong>Thanh toán:</strong> {{ getPaymentStatusText(selectedOrder?.paymentStatus) }}</p>
            </div>
          </div>

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
              <div class="address-row">
                <span class="address-label">🏠 Địa chỉ:</span>
                <span class="address-value">{{ getFullAddress(selectedOrder?.shippingAddress) }}</span>
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
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeDetailModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- ===== TOAST ===== -->
    <div :class="['toast', { show: showToast, 'toast-error': toastType === 'error' }]">
      <i :class="toastType === 'error' ? 'fas fa-exclamation-circle' : 'fas fa-check-circle'"></i>
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from "@/api/axios";
import StatsCard from '../../components/admin/StatsCard.vue'
import RevenueChart from '../../components/admin/RevenueChart.vue'

const router = useRouter()
const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");

// ===== STATE =====
const loading = ref(false)
const loadingProduct = ref(false)
const loadingCustomer = ref(false)
const loadingOrder = ref(false)
const loadingRevenue = ref(false)
const lastUpdated = ref(null)

// ===== STATS DATA =====
const productStats = ref(null)
const customerStats = ref(null)
const orderStats = ref(null)
const orderStatusCounts = ref({})
const monthlyRevenueData = ref([])
const topProducts = ref([])
const recentOrders = ref([])

// ===== FILTERS =====
const selectedYear = ref(new Date().getFullYear())
const availableYears = computed(() => {
  const current = new Date().getFullYear()
  return Array.from({ length: 5 }, (_, i) => current - i)
})

// ===== MODAL STATE =====
const showDetailModal = ref(false)
const selectedOrder = ref(null)

// ===== TOAST =====
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

// ===== HELPERS =====
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

const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

const getFullAddress = (shippingAddress) => {
  if (!shippingAddress) return '---'
  const parts = []
  if (shippingAddress.addressLine) parts.push(shippingAddress.addressLine)
  if (shippingAddress.ward) parts.push(shippingAddress.ward)
  if (shippingAddress.district) parts.push(shippingAddress.district)
  if (shippingAddress.city) parts.push(shippingAddress.city)
  return parts.length > 0 ? parts.join(', ') : '---'
}

const getStatusText = (status) => {
  const map = {
    'PENDING_PAYMENT': 'Chờ xác nhận',
    'CONFIRMED': 'Đã xác nhận',
    'PROCESSING': 'Đang xử lý',
    'SHIPPING': 'Đang giao',
    'DELIVERED': 'Đã giao',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy',
    'RETURN_REQUESTED': 'Yêu cầu hoàn hàng',
    'RETURN_APPROVED': 'Đã duyệt hoàn hàng',
    'RETURN_RECEIVED': 'Đã nhận hàng trả',
    'RETURN_PENDING': 'Đang thu hồi hàng',
    'RETURNED': 'Đã hoàn hàng',
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

const getStatusPercent = (status) => {
  const total = orderStats.value?.totalOrders || 1
  const count = orderStatusCounts.value[status] || 0
  return Math.round((count / total) * 100)
}

const getCustomerPercent = (count) => {
  const total = customerStats.value?.totalCustomers || 1
  return Math.round((count / total) * 100)
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

// ===== API CALLS =====
const fetchProductStats = async () => {
  loadingProduct.value = true
  try {
    const response = await api.get(`/products/statistics`)
    if (response.data?.data) {
      productStats.value = response.data.data
    }
  } catch (error) {
    console.error('❌ Error fetching product stats:', error)
  } finally {
    loadingProduct.value = false
  }
}

const fetchCustomerStats = async () => {
  loadingCustomer.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(`/admin/customers/statistics`, {})
    if (response.data) {
      customerStats.value = response.data
    }
  } catch (error) {
    console.error('❌ Error fetching customer stats:', error)
  } finally {
    loadingCustomer.value = false
  }
}

const fetchOrderStats = async () => {
  loadingOrder.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(`/admin/orders/statistics`, {})
    if (response.data) {
      orderStats.value = response.data
    }
  } catch (error) {
    console.error('❌ Error fetching order stats:', error)
  } finally {
    loadingOrder.value = false
  }
}

const fetchOrderStatusCounts = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(`/admin/orders/status/counts`, {})
    if (response.data) {
      orderStatusCounts.value = response.data
    }
  } catch (error) {
    console.error('❌ Error fetching order status counts:', error)
  }
}

const fetchMonthlyRevenue = async () => {
  loadingRevenue.value = true
  try {
    // Gọi API để lấy doanh thu theo tháng
    // Backend chưa có API riêng, dùng dữ liệu từ orderStats
    // Nếu cần chi tiết theo tháng, cần thêm API ở backend
    const monthlyData = []
    const now = new Date()
    const year = selectedYear.value

    for (let month = 1; month <= 12; month++) {
      monthlyData.push({
        month: month,
        revenue: 0
      })
    }

    // Giả lập dữ liệu từ orderStats
    if (orderStats.value?.monthlyRevenue) {
      const monthNum = now.getMonth() + 1
      const yearNum = now.getFullYear()
      if (yearNum === year) {
        monthlyData[monthNum - 1].revenue = orderStats.value.monthlyRevenue || 0
      }
    }

    monthlyRevenueData.value = monthlyData
  } catch (error) {
    console.error('❌ Error fetching monthly revenue:', error)
  } finally {
    loadingRevenue.value = false
  }
}

const fetchTopProducts = async () => {
  try {
    // Gọi API để lấy top sản phẩm bán chạy
    // Sử dụng OrderDetailRepository.getTopSellingProducts
    const response = await api.get(`/orders/top-products`, {
      params: { limit: 5 }
    })
    if (response.data) {
      topProducts.value = response.data
    }
  } catch (error) {
    console.error('❌ Error fetching top products:', error)
    // Dữ liệu mẫu nếu API chưa có
    topProducts.value = []
  }
}

const fetchRecentOrders = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(`/admin/orders`, {
      params: { page: 0, size: 10 } })
    if (response.data?.content) {
      recentOrders.value = response.data.content.slice(0, 5)
    }
  } catch (error) {
    console.error('❌ Error fetching recent orders:', error)
    recentOrders.value = []
  }
}

// ===== REFRESH ALL DATA =====
const refreshAllData = async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchProductStats(),
      fetchCustomerStats(),
      fetchOrderStats(),
      fetchOrderStatusCounts(),
      fetchMonthlyRevenue(),
      fetchTopProducts(),
      fetchRecentOrders()
    ])
    lastUpdated.value = new Date()
    showNotification('Đã cập nhật dữ liệu thành công!', 'success')
  } catch (error) {
    console.error('❌ Error refreshing data:', error)
    showNotification('Có lỗi xảy ra khi tải dữ liệu', 'error')
  } finally {
    loading.value = false
  }
}

// ===== VIEW ORDER DETAIL =====
const viewOrderDetail = async (orderId) => {
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(`/admin/orders/${orderId}`, {})
    if (response.data) {
      selectedOrder.value = response.data
      showDetailModal.value = true
    }
  } catch (error) {
    console.error('❌ Error fetching order detail:', error)
    showNotification('Không thể tải chi tiết đơn hàng', 'error')
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
}

// ===== WATCH =====
watch(selectedYear, () => {
  fetchMonthlyRevenue()
})

// ===== LIFECYCLE =====
onMounted(() => {
  refreshAllData()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
  animation: fadeIn 0.3s ease;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 28px;
  color: #2c3e50;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-refresh {
  padding: 10px 20px;
  background: #2c3e50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-refresh:hover:not(:disabled) {
  background: #1a252f;
  transform: translateY(-2px);
}

.btn-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-refresh .spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.last-update {
  font-size: 13px;
  color: #999;
}

/* ===== STATS GRID ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

/* ===== CHARTS ROW ===== */
.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 25px;
  margin-bottom: 30px;
}

.charts-row.two-col {
  grid-template-columns: 1fr 1fr;
}

.chart-card {
  background: white;
  padding: 24px;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.chart-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 {
  font-size: 17px;
  color: #2c3e50;
  margin: 0;
}

.chart-header select {
  padding: 8px 14px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  outline: none;
  cursor: pointer;
  background: white;
  font-size: 14px;
}

/* ===== TOP PRODUCTS ===== */
.top-products {
  margin-top: 5px;
}

.top-product-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px 0;
  border-bottom: 1px solid #f1f4f8;
}

.top-product-item:last-child {
  border-bottom: none;
}

.product-rank {
  width: 30px;
  height: 30px;
  background: #f1f4f8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #999;
  font-size: 13px;
}

.product-rank.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffed4e);
  color: #2c3e50;
}

.product-info {
  flex: 1;
}

.product-info h4 {
  font-size: 14px;
  color: #2c3e50;
  margin: 0 0 2px 0;
}

.product-info p {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.product-revenue {
  font-weight: 700;
  color: #e63946;
  font-size: 14px;
}

.no-data-text {
  text-align: center;
  color: #999;
  padding: 20px 0;
  font-style: italic;
}

/* ===== STATUS DISTRIBUTION ===== */
.status-distribution {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-label {
  font-size: 13px;
  color: #555;
  min-width: 140px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.status-bar-wrapper {
  flex: 1;
  height: 8px;
  background: #f1f4f8;
  border-radius: 4px;
  overflow: hidden;
}

.status-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.status-count {
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  min-width: 30px;
  text-align: right;
}

/* ===== CUSTOMER DISTRIBUTION ===== */
.customer-distribution {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.customer-stat-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 10px;
  gap: 12px;
}

.customer-stat-item .stat-label {
  flex: 1;
  font-size: 14px;
  color: #555;
}

.customer-stat-item .stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
}

.customer-stat-item .stat-percent {
  font-size: 13px;
  color: #999;
  min-width: 40px;
  text-align: right;
}

.customer-total {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  background: #e8f0fe;
  border-radius: 10px;
  gap: 12px;
  margin-top: 4px;
}

.customer-total .stat-label {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: #1a3a6a;
}

.customer-total .stat-value.total {
  font-size: 20px;
  font-weight: 700;
  color: #1a3a6a;
}

/* ===== RECENT ORDERS ===== */
.recent-orders {
  background: white;
  padding: 24px;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 17px;
  color: #2c3e50;
  margin: 0;
}

.view-all {
  color: #e63946;
  text-decoration: none;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s;
}

.view-all:hover {
  gap: 10px;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th {
  background: #f8f9fa;
  padding: 12px 14px;
  text-align: left;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
}

.data-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f1f4f8;
}

.data-table tr:hover {
  background: #fafafa;
}

.order-total {
  font-weight: 700;
  color: #e63946;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending { background: #fff3cd; color: #856404; }
.status-confirmed { background: #cce5ff; color: #004085; }
.status-processing { background: #cce5ff; color: #004085; }
.status-shipping { background: #d1ecf1; color: #0c5460; }
.status-delivered { background: #d4edda; color: #155724; }
.status-completed { background: #d4edda; color: #155724; }
.status-cancelled { background: #f8d7da; color: #721c24; }
.status-return-requested { background: #fff3cd; color: #856404; }
.status-return-approved { background: #cce5ff; color: #004085; }
.status-return-received { background: #cce5ff; color: #004085; }
.status-return-pending { background: #d1ecf1; color: #0c5460; }
.status-returned { background: #d4edda; color: #155724; }
.status-rejected { background: #f8d7da; color: #721c24; }

.payment-status {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.payment-paid { background: #d4edda; color: #155724; }
.payment-unpaid { background: #fff3cd; color: #856404; }

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
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
  padding: 30px;
  color: #999;
}

.no-data i {
  font-size: 36px;
  display: block;
  margin-bottom: 8px;
  color: #ddd;
}

/* ===== LOADING ===== */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #e63946;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
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
  from { transform: translateY(30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border-radius: 15px 15px 0 0;
  position: sticky;
  top: 0;
  z-index: 1;
}

.modal-header h3 {
  font-size: 18px;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
}

.close-btn:hover {
  transform: rotate(90deg);
  opacity: 0.8;
}

.modal-body {
  padding: 24px;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-outline {
  background: #f0f0f0;
  color: #666;
}

.btn-outline:hover {
  background: #e0e0e0;
}

/* ===== ORDER DETAIL ===== */
.order-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.info-section {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 10px;
}

.info-section h4 {
  color: #2c3e50;
  margin: 0 0 10px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.info-section p {
  margin: 6px 0;
  font-size: 14px;
  color: #555;
}

.address-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 10px;
  border: 1px solid #e9ecef;
}

.address-section h4 {
  margin: 0 0 12px 0;
  color: #2c3e50;
}

.address-detail-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.address-row {
  display: flex;
  padding: 4px 0;
  font-size: 14px;
}

.address-label {
  font-weight: 600;
  color: #2c3e50;
  min-width: 120px;
}

.address-value {
  color: #444;
}

.order-items-section h4 {
  color: #2c3e50;
  margin: 0 0 15px 0;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.items-table th {
  background: #f8f9fa;
  padding: 10px;
  text-align: left;
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

.discount-amount {
  color: #28a745;
  font-weight: 700;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-image {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
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
}

.item-variant {
  font-size: 12px;
  color: #999;
}

/* ===== TOAST ===== */
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

/* ===== ANIMATIONS ===== */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== RESPONSIVE ===== */
@media (max-width: 1200px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
  .charts-row.two-col {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .order-info-grid {
    grid-template-columns: 1fr;
  }

  .status-item {
    flex-wrap: wrap;
  }

  .status-label {
    min-width: 100px;
  }

  .modal-container {
    width: 95%;
    max-height: 95vh;
  }

  .address-row {
    flex-direction: column;
    padding: 2px 0;
  }

  .address-label {
    min-width: auto;
    font-size: 12px;
  }

  .address-value {
    font-size: 13px;
    padding-left: 10px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .page-title {
    font-size: 22px;
  }

  .data-table {
    font-size: 12px;
  }

  .data-table th,
  .data-table td {
    padding: 8px 10px;
  }

  .items-table {
    font-size: 12px;
  }

  .items-table th,
  .items-table td {
    padding: 6px 8px;
  }
}
</style>