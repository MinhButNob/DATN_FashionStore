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
          <div 
            v-for="order in orders" 
            :key="order.orderId"
            class="order-card"
          >
            <div class="order-header">
              <div class="order-info">
                <span class="order-id">Mã đơn: {{ order.orderCode || order.orderId }}</span>
                <span class="order-date">{{ formatDate(order.orderDate) }}</span>
              </div>
              <div class="order-status">
                <span :class="['status-badge', getStatusClass(order.orderStatus)]">
                  {{ getStatusText(order.orderStatus) }}
                </span>
              </div>
            </div>

            <div class="order-items">
              <div 
                v-for="item in order.items" 
                :key="item.itemId"
                class="order-item"
              >
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
                  {{ formatPrice(item.finalAmount || (item.unitPrice * item.quantity)) }}
                </div>
              </div>
            </div>

            <div class="order-footer">
              <div class="order-total">
                <span>Tổng cộng:</span>
                <strong>{{ formatPrice(order.totalAmount) }}</strong>
              </div>
              <div class="order-actions">
                <button class="btn-outline" @click="viewOrderDetail(order.orderId)">
                  Xem chi tiết
                </button>
                <button 
                  v-if="order.orderStatus === 'COMPLETED'" 
                  class="btn-outline" 
                  @click="reorder(order)"
                >
                  Mua lại
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
    <OrderDetailModal 
      :visible="isDetailModalOpen"
      :order="selectedOrder"
      @close="closeDetailModal"
    />

    <!-- Toast -->
    <div :class="['toast', { 'toast-error': toastType === 'error', show: showToast }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
import OrderDetailModal from '../../components/client/OrderDetailModal.vue'
import { useCartStore } from '../../stores/cartStore'
import axios from 'axios'

const router = useRouter()
const cartStore = useCartStore()

const API_BASE = "http://localhost:8080/api"

// State
const loading = ref(false)
const orders = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const activeTab = ref('all')
const isDetailModalOpen = ref(false)
const selectedOrder = ref(null)

// Toast state
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

// Tabs
const tabs = [
  { value: 'all', label: 'Tất cả' },
  { value: 'PENDING_PAYMENT', label: 'Chờ thanh toán' },
  { value: 'PROCESSING', label: 'Đang xử lý' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'CANCELLED', label: 'Đã hủy' }
]

// Helper functions
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
    'CANCELLED': 'Đã hủy',
    'RETURNED': 'Trả hàng'
  }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    'PENDING_PAYMENT': 'pending',
    'PROCESSING': 'processing',
    'COMPLETED': 'completed',
    'CANCELLED': 'cancelled',
    'RETURNED': 'returned'
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

// ✅ GỌI API TRỰC TIẾP - KHÔNG QUA STORE
const fetchOrders = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      console.warn('Chưa đăng nhập')
      loading.value = false
      return
    }

    const status = activeTab.value === 'all' ? null : activeTab.value
    
    // Gọi API trực tiếp bằng axios
    const response = await axios.get(`${API_BASE}/orders`, {
      params: {
        page: currentPage.value,
        size: 10,
        status: status
      },
      headers: { 
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })

    console.log('✅ API Response:', response.data)

    // Xử lý response
    if (response.data && response.data.content) {
      orders.value = response.data.content
      totalPages.value = response.data.totalPages || 0
      totalElements.value = response.data.totalElements || 0
      currentPage.value = response.data.number || 0
    } else if (Array.isArray(response.data)) {
      orders.value = response.data
      totalPages.value = 1
    } else {
      orders.value = []
    }

    console.log(`✅ Đã tải ${orders.value.length} đơn hàng`)
    
  } catch (error) {
    console.error('❌ Lỗi tải đơn hàng:', error)
    const errorMsg = error.response?.data?.message || error.message || 'Không thể tải danh sách đơn hàng'
    showNotification(errorMsg, 'error')
    orders.value = []
  } finally {
    loading.value = false
  }
}

// ✅ GỌI API CHI TIẾT ĐƠN HÀNG TRỰC TIẾP
const viewOrderDetail = async (orderId) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`${API_BASE}/orders/${orderId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    selectedOrder.value = response.data
    isDetailModalOpen.value = true
  } catch (error) {
    console.error('Lỗi tải chi tiết đơn hàng:', error)
    showNotification('Không thể tải chi tiết đơn hàng', 'error')
  }
}

const closeDetailModal = () => {
  isDetailModalOpen.value = false
  selectedOrder.value = null
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

const reorder = (order) => {
  order.items.forEach(item => {
    cartStore.addToCart({
      productId: item.productDetailId,
      productName: item.productName,
      price: item.unitPrice,
      quantity: item.quantity,
      image: item.imageUrl,
      size: item.sizeName,
      color: item.colorName
    })
  })
  showNotification('Đã thêm sản phẩm vào giỏ hàng!', 'success')
  router.push('/cart')
}

// Computed
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

// Lifecycle
onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
/* Giữ nguyên style cũ */
.orders-page {
  min-height: 100vh;
}

main {
  margin-top: 140px;
  padding: 20px 0 50px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-title {
  font-size: 32px;
  color: #2c3e50;
  margin-bottom: 30px;
}

/* Tabs */
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

/* Empty State */
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

/* Order Card */
.order-card {
  background: white;
  border-radius: 15px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
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

.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.pending { background: #fff3cd; color: #856404; }
.status-badge.processing { background: #cce5ff; color: #004085; }
.status-badge.completed { background: #d4edda; color: #155724; }
.status-badge.cancelled { background: #f8d7da; color: #721c24; }
.status-badge.returned { background: #fef3c7; color: #92400e; }

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
}

.item-variant {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.item-quantity {
  font-size: 12px;
  color: #999;
}

.item-price {
  font-weight: 600;
  color: #e63946;
  font-size: 15px;
  min-width: 100px;
  text-align: right;
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
}

.btn-outline {
  padding: 8px 16px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-outline:hover {
  border-color: #e63946;
  color: #e63946;
}

/* Pagination */
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

/* Toast */
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

/* Responsive */
@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .order-tabs {
    overflow-x: auto;
    white-space: nowrap;
    flex-wrap: nowrap;
  }
  
  .order-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .item-image {
    width: 100%;
    height: 150px;
  }
  
  .item-price {
    text-align: left;
  }
  
  .order-footer {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .order-actions {
    width: 100%;
  }
  
  .order-actions button {
    flex: 1;
  }
}
</style>