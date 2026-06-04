<template>
  <div class="admin-orders">
    <div class="admin-header">
      <h1 class="page-title">Quản lý đơn hàng</h1>
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
        <option value="pending">Chờ xác nhận</option>
        <option value="processing">Đang xử lý</option>
        <option value="shipped">Đang giao</option>
        <option value="delivered">Đã giao</option>
        <option value="cancelled">Đã hủy</option>
      </select>

      <select v-model="filters.payment" @change="applyFilters">
        <option value="all">Tất cả thanh toán</option>
        <option value="paid">Đã thanh toán</option>
        <option value="pending">Chờ thanh toán</option>
        <option value="failed">Thất bại</option>
      </select>

      <select v-model="filters.dateRange" @change="applyFilters">
        <option value="all">Tất cả thời gian</option>
        <option value="today">Hôm nay</option>
        <option value="week">Tuần này</option>
        <option value="month">Tháng này</option>
        <option value="year">Năm nay</option>
      </select>
    </div>

    <!-- Bulk Actions -->
    <Transition>
      <div v-if="selectedOrders.length > 0" class="bulk-actions">
        <span>Đã chọn <strong>{{ selectedOrders.length }}</strong> đơn hàng</span>
        
        <select v-model="bulkAction">
          <option value="">Chọn thao tác</option>
          <option value="processing">Chuyển sang Đang xử lý</option>
          <option value="shipped">Chuyển sang Đang giao</option>
          <option value="delivered">Đánh dấu Đã giao</option>
          <option value="cancelled">Hủy đơn hàng</option>
        </select>

        <button class="btn btn-primary" @click="executeBulkAction">
          Áp dụng
        </button>

        <button class="btn btn-outline" @click="clearSelection">
          Bỏ chọn
        </button>
      </div>
    </Transition>

    <!-- Orders Table -->
    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th width="40">
              <input 
                type="checkbox" 
                :checked="isAllSelected"
                @change="toggleSelectAll"
              >
            </th>
            <th>Mã đơn</th>
            <th>Khách hàng</th>
            <th>Sản phẩm</th>
            <th>Tổng tiền</th>
            <th>Ngày đặt</th>
            <th>Trạng thái</th>
            <th>Thanh toán</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in paginatedOrders" :key="order.id">
            <td>
              <input 
                type="checkbox" 
                :value="order.id"
                v-model="selectedOrders"
              >
            </td>
            <td>
              <strong>{{ order.id }}</strong>
            </td>
            <td>
              <div class="customer-info">
                <div class="customer-name">{{ order.customer.name }}</div>
                <div class="customer-phone">{{ order.customer.phone }}</div>
              </div>
            </td>
            <td>
              <div class="product-count">{{ order.items.length }} sản phẩm</div>
              <button 
                class="view-items-btn" 
                @click="viewOrderItems(order)"
                title="Xem chi tiết sản phẩm"
              >
                <i class="fas fa-eye"></i>
              </button>
            </td>
            <td>
              <span class="order-total">{{ formatPrice(order.total) }}</span>
            </td>
            <td>{{ order.date }}</td>
            <td>
              <select 
                class="status-select" 
                :class="`status-${order.status}`"
                v-model="order.status"
                @change="updateOrderStatus(order)"
              >
                <option value="pending">Chờ xác nhận</option>
                <option value="processing">Đang xử lý</option>
                <option value="shipped">Đang giao</option>
                <option value="delivered">Đã giao</option>
                <option value="cancelled">Đã hủy</option>
              </select>
            </td>
            <td>
              <span 
                class="payment-status" 
                :class="`payment-${order.payment.status}`"
              >
                {{ getPaymentStatus(order.payment.status) }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button 
                  class="action-btn view-btn" 
                  @click="viewOrderDetail(order)"
                  title="Xem chi tiết"
                >
                  <i class="fas fa-eye"></i>
                </button>
                <button 
                  class="action-btn edit-btn" 
                  @click="editOrder(order)"
                  title="Sửa"
                >
                  <i class="fas fa-edit"></i>
                </button>
                <button 
                  class="action-btn delete-btn" 
                  @click="confirmDelete(order.id)"
                  title="Xóa"
                >
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredOrders.length === 0">
            <td colspan="9" class="no-data">
              Không tìm thấy đơn hàng nào
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination">
      <button
        class="page-btn"
        :disabled="currentPage === 1"
        @click="currentPage--"
      >
        <i class="fas fa-chevron-left"></i>
      </button>

      <button
        v-for="page in displayedPages"
        :key="page"
        class="page-btn"
        :class="{ active: currentPage === page }"
        @click="currentPage = page"
      >
        {{ page }}
      </button>

      <button
        class="page-btn"
        :disabled="currentPage === totalPages"
        @click="currentPage++"
      >
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- Order Detail Modal -->
    <div v-if="showDetailModal" class="modal">
      <div class="modal-content modal-lg">
        <div class="modal-header">
          <h2>Chi tiết đơn hàng #{{ selectedOrder?.id }}</h2>
          <button class="close-btn" @click="closeDetailModal">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="modal-body">
          <div class="order-info-grid">
            <div class="info-section">
              <h3>Thông tin khách hàng</h3>
              <p><strong>Họ tên:</strong> {{ selectedOrder?.customer.name }}</p>
              <p><strong>SĐT:</strong> {{ selectedOrder?.customer.phone }}</p>
              <p><strong>Email:</strong> {{ selectedOrder?.customer.email }}</p>
            </div>

            <div class="info-section">
              <h3>Địa chỉ giao hàng</h3>
              <p>{{ selectedOrder?.shippingAddress }}</p>
            </div>

            <div class="info-section">
              <h3>Thông tin đơn hàng</h3>
              <p><strong>Ngày đặt:</strong> {{ selectedOrder?.date }}</p>
              <p><strong>Phương thức thanh toán:</strong> {{ selectedOrder?.payment.method }}</p>
              <p><strong>Phương thức vận chuyển:</strong> {{ selectedOrder?.shipping.method }}</p>
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
                <tr v-for="item in selectedOrder?.items" :key="item.id">
                  <td>
                    <div class="item-info">
                      <div class="item-image">
                        <i :class="['fas', `fa-${item.image}`]"></i>
                      </div>
                      <div>
                        <div class="item-name">{{ item.name }}</div>
                        <div class="item-variant">
                          {{ item.size }} {{ item.color }}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td>{{ formatPrice(item.price) }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ formatPrice(item.price * item.quantity) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="3" class="text-right">Tạm tính:</td>
                  <td>{{ formatPrice(selectedOrder?.subtotal) }}</td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right">Phí vận chuyển:</td>
                  <td>{{ formatPrice(selectedOrder?.shipping.fee) }}</td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right">Giảm giá:</td>
                  <td>-{{ formatPrice(selectedOrder?.discount || 0) }}</td>
                </tr>
                <tr class="total-row">
                  <td colspan="3" class="text-right">Tổng cộng:</td>
                  <td class="total-price">{{ formatPrice(selectedOrder?.total) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div class="order-notes" v-if="selectedOrder?.notes">
            <h3>Ghi chú</h3>
            <p>{{ selectedOrder.notes }}</p>
          </div>

          <div class="order-timeline">
            <h3>Lịch sử đơn hàng</h3>
            <div class="timeline">
              <div 
                v-for="(event, index) in selectedOrder?.timeline" 
                :key="index"
                class="timeline-item"
              >
                <div class="timeline-icon">
                  <i :class="event.icon"></i>
                </div>
                <div class="timeline-content">
                  <div class="timeline-date">{{ event.date }}</div>
                  <div class="timeline-text">{{ event.text }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-primary" @click="printOrder">
            <i class="fas fa-print"></i> In đơn hàng
          </button>
          <button class="btn btn-outline" @click="closeDetailModal">
            Đóng
          </button>
        </div>
      </div>
    </div>

    <!-- Order Items Modal -->
    <div v-if="showItemsModal" class="modal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Sản phẩm trong đơn hàng #{{ selectedOrder?.id }}</h2>
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
              <tr v-for="item in selectedOrder?.items" :key="item.id">
                <td>
                  <div class="item-info">
                    <div class="item-image">
                      <i :class="['fas', `fa-${item.image}`]"></i>
                    </div>
                    <div>
                      <div class="item-name">{{ item.name }}</div>
                      <div class="item-variant">
                        {{ item.size }} {{ item.color }}
                      </div>
                    </div>
                  </div>
                </td>
                <td>{{ formatPrice(item.price) }}</td>
                <td>{{ item.quantity }}</td>
                <td>{{ formatPrice(item.price * item.quantity) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="modal-footer">
          <button class="btn btn-primary" @click="showItemsModal = false">
            Đóng
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="showDeleteModal" class="modal">
      <div class="modal-content modal-sm">
        <div class="modal-header">
          <h2>Xác nhận xóa</h2>
          <button class="close-btn" @click="closeDeleteModal">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="modal-body">
          <p>Bạn có chắc chắn muốn xóa đơn hàng này?</p>
          <p class="warning-text">Hành động này không thể hoàn tác!</p>
        </div>

        <div class="modal-footer">
          <button class="btn btn-danger" @click="deleteOrder">
            Xóa
          </button>
          <button class="btn btn-outline" @click="closeDeleteModal">
            Hủy
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useNotification } from '../../composables/useNotification'
import { formatPrice } from '../../utils/helpers'

const { showNotification } = useNotification()

// Mock data
const orders = ref([
  {
    id: 'DH001',
    customer: {
      name: 'Nguyễn Văn A',
      phone: '0123456789',
      email: 'nguyenvana@email.com'
    },
    items: [
      { id: 1, name: 'Áo thun nam', image: 'tshirt', price: 199000, quantity: 2, size: 'M', color: 'Đen' },
      { id: 3, name: 'Quần jean nam', image: 'jeans', price: 499000, quantity: 1, size: 'L', color: 'Xanh' }
    ],
    subtotal: 897000,
    shipping: {
      method: 'Giao hàng tiêu chuẩn',
      fee: 30000
    },
    discount: 0,
    total: 927000,
    date: '15/03/2024 14:30',
    status: 'delivered',
    payment: {
      method: 'COD',
      status: 'paid'
    },
    shippingAddress: '123 Nguyễn Huệ, Quận 1, Hồ Chí Minh',
    notes: 'Gọi trước khi giao',
    timeline: [
      { date: '15/03/2024 14:30', text: 'Đơn hàng được tạo', icon: 'fas fa-plus-circle' },
      { date: '15/03/2024 15:45', text: 'Đã xác nhận đơn hàng', icon: 'fas fa-check-circle' },
      { date: '16/03/2024 08:20', text: 'Đang giao hàng', icon: 'fas fa-truck' },
      { date: '16/03/2024 14:15', text: 'Giao hàng thành công', icon: 'fas fa-check-circle' }
    ]
  },
  {
    id: 'DH002',
    customer: {
      name: 'Trần Thị B',
      phone: '0987654321',
      email: 'tranthib@email.com'
    },
    items: [
      { id: 2, name: 'Đầm nữ công sở', image: 'dress', price: 399000, quantity: 1, size: 'M', color: 'Đỏ' }
    ],
    subtotal: 399000,
    shipping: {
      method: 'Giao hàng nhanh',
      fee: 50000
    },
    discount: 0,
    total: 449000,
    date: '16/03/2024 09:15',
    status: 'processing',
    payment: {
      method: 'Chuyển khoản',
      status: 'pending'
    },
    shippingAddress: '456 Lê Lợi, Quận 1, Hồ Chí Minh',
    notes: '',
    timeline: [
      { date: '16/03/2024 09:15', text: 'Đơn hàng được tạo', icon: 'fas fa-plus-circle' },
      { date: '16/03/2024 10:30', text: 'Đã xác nhận đơn hàng', icon: 'fas fa-check-circle' }
    ]
  },
  {
    id: 'DH003',
    customer: {
      name: 'Lê Văn C',
      phone: '0369852147',
      email: 'levanc@email.com'
    },
    items: [
      { id: 4, name: 'Áo khoác nữ', image: 'jacket', price: 599000, quantity: 1, size: 'L', color: 'Đen' },
      { id: 6, name: 'Túi xách nữ', image: 'bag', price: 299000, quantity: 1, size: '', color: 'Nâu' }
    ],
    subtotal: 898000,
    shipping: {
      method: 'Giao hàng tiêu chuẩn',
      fee: 30000
    },
    discount: 50000,
    total: 878000,
    date: '16/03/2024 16:20',
    status: 'pending',
    payment: {
      method: 'COD',
      status: 'pending'
    },
    shippingAddress: '789 Điện Biên Phủ, Quận 3, Hồ Chí Minh',
    notes: 'Giao giờ hành chính',
    timeline: [
      { date: '16/03/2024 16:20', text: 'Đơn hàng được tạo', icon: 'fas fa-plus-circle' }
    ]
  },
  {
    id: 'DH004',
    customer: {
      name: 'Phạm Thị D',
      phone: '0912345678',
      email: 'phamthid@email.com'
    },
    items: [
      { id: 5, name: 'Quần short trẻ em', image: 'shorts', price: 149000, quantity: 3, size: 'S', color: 'Xanh' }
    ],
    subtotal: 447000,
    shipping: {
      method: 'Giao hàng tiêu chuẩn',
      fee: 30000
    },
    discount: 0,
    total: 477000,
    date: '17/03/2024 10:05',
    status: 'shipped',
    payment: {
      method: 'COD',
      status: 'pending'
    },
    shippingAddress: '321 Hoàng Diệu, Quận 4, Hồ Chí Minh',
    notes: '',
    timeline: [
      { date: '17/03/2024 10:05', text: 'Đơn hàng được tạo', icon: 'fas fa-plus-circle' },
      { date: '17/03/2024 11:20', text: 'Đã xác nhận đơn hàng', icon: 'fas fa-check-circle' },
      { date: '18/03/2024 09:00', text: 'Đang giao hàng', icon: 'fas fa-truck' }
    ]
  },
  {
    id: 'DH005',
    customer: {
      name: 'Hoàng Văn E',
      phone: '0975314682',
      email: 'hoangvane@email.com'
    },
    items: [
      { id: 7, name: 'Áo sơ mi nam', image: 'shirt', price: 299000, quantity: 2, size: 'L', color: 'Trắng' }
    ],
    subtotal: 598000,
    shipping: {
      method: 'Giao hàng nhanh',
      fee: 50000
    },
    discount: 0,
    total: 648000,
    date: '17/03/2024 14:30',
    status: 'cancelled',
    payment: {
      method: 'Chuyển khoản',
      status: 'failed'
    },
    shippingAddress: '654 Võ Văn Kiệt, Quận 5, Hồ Chí Minh',
    notes: 'Khách yêu cầu hủy',
    timeline: [
      { date: '17/03/2024 14:30', text: 'Đơn hàng được tạo', icon: 'fas fa-plus-circle' },
      { date: '17/03/2024 15:45', text: 'Đã xác nhận đơn hàng', icon: 'fas fa-check-circle' },
      { date: '17/03/2024 16:20', text: 'Khách yêu cầu hủy đơn', icon: 'fas fa-times-circle' },
      { date: '17/03/2024 17:00', text: 'Đơn hàng đã hủy', icon: 'fas fa-ban' }
    ]
  }
])

// State
const filters = ref({
  search: '',
  status: 'all',
  payment: 'all',
  dateRange: 'all'
})

const currentPage = ref(1)
const itemsPerPage = 10
const selectedOrders = ref([])
const bulkAction = ref('')
const showDetailModal = ref(false)
const showItemsModal = ref(false)
const showDeleteModal = ref(false)
const selectedOrder = ref(null)
const deleteId = ref(null)

// Computed
const filteredOrders = computed(() => {
  let filtered = [...orders.value]

  // Search filter
  if (filters.value.search) {
    const searchTerm = filters.value.search.toLowerCase()
    filtered = filtered.filter(order =>
      order.id.toLowerCase().includes(searchTerm) ||
      order.customer.name.toLowerCase().includes(searchTerm) ||
      order.customer.phone.includes(searchTerm)
    )
  }

  // Status filter
  if (filters.value.status !== 'all') {
    filtered = filtered.filter(order => order.status === filters.value.status)
  }

  // Payment filter
  if (filters.value.payment !== 'all') {
    filtered = filtered.filter(order => order.payment.status === filters.value.payment)
  }

  // Date range filter
  if (filters.value.dateRange !== 'all') {
    const today = new Date()
    const todayStr = today.toLocaleDateString('vi-VN')
    
    filtered = filtered.filter(order => {
      const orderDate = order.date.split(' ')[0]
      
      switch (filters.value.dateRange) {
        case 'today':
          return orderDate === todayStr
        case 'week':
          // Simple week filter - last 7 days
          const orderDateTime = new Date(orderDate.split('/').reverse().join('-')).getTime()
          const weekAgo = today.getTime() - 7 * 24 * 60 * 60 * 1000
          return orderDateTime >= weekAgo
        default:
          return true
      }
    })
  }

  return filtered
})

const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredOrders.value.slice(start, end)
})

const totalPages = computed(() => {
  return Math.ceil(filteredOrders.value.length / itemsPerPage)
})

const displayedPages = computed(() => {
  const delta = 2
  const range = []
  const rangeWithDots = []
  let l

  for (let i = 1; i <= totalPages.value; i++) {
    if (i === 1 || i === totalPages.value || (i >= currentPage.value - delta && i <= currentPage.value + delta)) {
      range.push(i)
    }
  }

  range.forEach((i) => {
    if (l) {
      if (i - l === 2) {
        rangeWithDots.push(l + 1)
      } else if (i - l !== 1) {
        rangeWithDots.push('...')
      }
    }
    rangeWithDots.push(i)
    l = i
  })

  return rangeWithDots
})

const isAllSelected = computed(() => {
  return paginatedOrders.value.length > 0 && 
         paginatedOrders.value.every(order => selectedOrders.value.includes(order.id))
})

// Methods
const applyFilters = () => {
  currentPage.value = 1
}

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedOrders.value = selectedOrders.value.filter(
      id => !paginatedOrders.value.some(order => order.id === id)
    )
  } else {
    const newIds = paginatedOrders.value.map(order => order.id)
    selectedOrders.value = [...new Set([...selectedOrders.value, ...newIds])]
  }
}

const clearSelection = () => {
  selectedOrders.value = []
  bulkAction.value = ''
}

const executeBulkAction = () => {
  if (!bulkAction.value) return

  selectedOrders.value.forEach(id => {
    const order = orders.value.find(o => o.id === id)
    if (order) {
      order.status = bulkAction.value
      
      // Add to timeline
      order.timeline.push({
        date: new Date().toLocaleString('vi-VN'),
        text: `Cập nhật trạng thái: ${getStatusText(bulkAction.value)}`,
        icon: 'fas fa-sync-alt'
      })
    }
  })

  showNotification(`Đã cập nhật ${selectedOrders.value.length} đơn hàng`, 'success')
  clearSelection()
}

const getPaymentStatus = (status) => {
  const statuses = {
    'paid': 'Đã thanh toán',
    'pending': 'Chờ thanh toán',
    'failed': 'Thất bại'
  }
  return statuses[status] || status
}

const getStatusText = (status) => {
  const statuses = {
    'pending': 'Chờ xác nhận',
    'processing': 'Đang xử lý',
    'shipped': 'Đang giao',
    'delivered': 'Đã giao',
    'cancelled': 'Đã hủy'
  }
  return statuses[status] || status
}

const updateOrderStatus = (order) => {
  // Add to timeline
  order.timeline.push({
    date: new Date().toLocaleString('vi-VN'),
    text: `Cập nhật trạng thái: ${getStatusText(order.status)}`,
    icon: 'fas fa-sync-alt'
  })
  
  showNotification('Cập nhật trạng thái thành công!', 'success')
}

const viewOrderDetail = (order) => {
  selectedOrder.value = order
  showDetailModal.value = true
}

const viewOrderItems = (order) => {
  selectedOrder.value = order
  showItemsModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
}

const editOrder = (order) => {
  showNotification('Chức năng đang phát triển', 'info')
}

const confirmDelete = (id) => {
  deleteId.value = id
  showDeleteModal.value = true
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
  deleteId.value = null
}

const deleteOrder = () => {
  const index = orders.value.findIndex(o => o.id === deleteId.value)
  if (index !== -1) {
    orders.value.splice(index, 1)
    showNotification('Xóa đơn hàng thành công!', 'success')
  }
  closeDeleteModal()
}

const printOrder = () => {
  window.print()
}

// Watch for filter changes
watch(filters, () => {
  applyFilters()
}, { deep: true })
</script>

<style scoped>
.admin-orders {
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
  color: var(--dark-gray);
}

.filters-bar {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: var(--shadow);
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.search-box {
  flex: 1;
  min-width: 250px;
  display: flex;
  align-items: center;
  background: var(--light-gray);
  border-radius: 8px;
  padding: 0 15px;
}

.search-box i {
  color: var(--medium-gray);
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
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  min-width: 150px;
  cursor: pointer;
  background: white;
}

.filters-bar select:focus {
  border-color: var(--primary-red);
}

/* Bulk Actions */
.bulk-actions {
  background: white;
  padding: 15px 20px;
  border-radius: 10px;
  box-shadow: var(--shadow);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
  animation: slideDown 0.3s;
}

.bulk-actions select {
  padding: 10px 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  min-width: 200px;
}

/* Table */
.table-container {
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
  overflow-x: auto;
  margin-bottom: 20px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background: var(--light-gray);
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: var(--dark-gray);
  border-bottom: 2px solid var(--border-color);
}

td {
  padding: 15px;
  border-bottom: 1px solid var(--border-color);
  color: var(--medium-gray);
}

tr:hover {
  background: var(--light-gray);
}

/* Customer Info */
.customer-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.customer-name {
  font-weight: 600;
  color: var(--dark-gray);
}

.customer-phone {
  font-size: 12px;
  color: var(--medium-gray);
}

/* Product Count */
.product-count {
  font-weight: 500;
  color: var(--dark-gray);
  margin-bottom: 5px;
}

.view-items-btn {
  background: none;
  border: none;
  color: var(--primary-red);
  cursor: pointer;
  font-size: 14px;
  padding: 5px;
  transition: all 0.3s;
}

.view-items-btn:hover {
  transform: scale(1.1);
}

/* Order Total */
.order-total {
  font-weight: 700;
  color: var(--primary-red);
}

/* Status Select */
.status-select {
  padding: 8px 12px;
  border-radius: 20px;
  border: none;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  outline: none;
}

.status-select.status-pending {
  background: #fff3cd;
  color: #856404;
}

.status-select.status-processing {
  background: #cce5ff;
  color: #004085;
}

.status-select.status-shipped {
  background: #d1ecf1;
  color: #0c5460;
}

.status-select.status-delivered {
  background: #d4edda;
  color: #155724;
}

.status-select.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}

/* Payment Status */
.payment-status {
  display: inline-block;
  padding: 5px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.payment-status.payment-paid {
  background: #d4edda;
  color: #155724;
}

.payment-status.payment-pending {
  background: #fff3cd;
  color: #856404;
}

.payment-status.payment-failed {
  background: #f8d7da;
  color: #721c24;
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

.view-btn {
  background: #17a2b8;
  color: white;
}

.view-btn:hover {
  background: #138496;
  transform: translateY(-2px);
}

.edit-btn {
  background: #ffc107;
  color: var(--dark-gray);
}

.edit-btn:hover {
  background: #e0a800;
  transform: translateY(-2px);
}

.delete-btn {
  background: #dc3545;
  color: white;
}

.delete-btn:hover {
  background: #c82333;
  transform: translateY(-2px);
}

/* No Data */
.no-data {
  text-align: center;
  padding: 40px;
  color: var(--medium-gray);
}

/* Modal */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
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

.modal-content.modal-sm {
  max-width: 400px;
}

.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  background: white;
  border-radius: 15px 15px 0 0;
}

.modal-header h2 {
  color: var(--dark-gray);
  font-size: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: var(--medium-gray);
  transition: all 0.3s;
}

.close-btn:hover {
  color: var(--primary-red);
  transform: rotate(90deg);
}

.modal-body {
  padding: 25px;
}

.modal-footer {
  padding: 20px 25px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  background: var(--light-gray);
  border-radius: 0 0 15px 15px;
}

/* Order Info Grid */
.order-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.info-section {
  background: var(--light-gray);
  padding: 15px;
  border-radius: 8px;
}

.info-section h3 {
  color: var(--dark-gray);
  font-size: 16px;
  margin-bottom: 10px;
  padding-bottom: 5px;
  border-bottom: 1px solid var(--border-color);
}

.info-section p {
  color: var(--medium-gray);
  margin-bottom: 5px;
  font-size: 14px;
}

.info-section p strong {
  color: var(--dark-gray);
  margin-right: 5px;
}

/* Order Items Section */
.order-items-section {
  margin-bottom: 30px;
}

.order-items-section h3 {
  color: var(--dark-gray);
  margin-bottom: 15px;
  font-size: 16px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
}

.items-table th {
  background: var(--light-gray);
  padding: 12px;
  text-align: left;
  font-size: 14px;
}

.items-table td {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
}

.item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-image {
  width: 40px;
  height: 40px;
  background: var(--light-gray);
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-image i {
  font-size: 20px;
  color: var(--medium-gray);
}

.item-name {
  font-weight: 600;
  color: var(--dark-gray);
  margin-bottom: 3px;
}

.item-variant {
  font-size: 12px;
  color: var(--medium-gray);
}

.text-right {
  text-align: right;
}

.total-row {
  font-weight: 700;
}

.total-price {
  color: var(--primary-red);
  font-size: 18px;
}

/* Order Notes */
.order-notes {
  background: var(--light-gray);
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.order-notes h3 {
  color: var(--dark-gray);
  margin-bottom: 10px;
  font-size: 16px;
}

.order-notes p {
  color: var(--medium-gray);
  line-height: 1.6;
}

/* Timeline */
.order-timeline h3 {
  color: var(--dark-gray);
  margin-bottom: 15px;
  font-size: 16px;
}

.timeline {
  position: relative;
  padding-left: 30px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--border-color);
}

.timeline-item {
  position: relative;
  margin-bottom: 20px;
  display: flex;
  gap: 15px;
}

.timeline-icon {
  position: absolute;
  left: -30px;
  width: 30px;
  height: 30px;
  background: white;
  border: 2px solid var(--primary-red);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-red);
  font-size: 12px;
}

.timeline-content {
  flex: 1;
  background: var(--light-gray);
  padding: 12px 15px;
  border-radius: 8px;
}

.timeline-date {
  font-size: 12px;
  color: var(--medium-gray);
  margin-bottom: 3px;
}

.timeline-text {
  color: var(--dark-gray);
  font-weight: 500;
}

/* Warning Text */
.warning-text {
  color: #dc3545;
  font-size: 14px;
  margin-top: 10px;
}

/* Animations */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(50px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.v-enter-active,
.v-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* Responsive */
@media (max-width: 1024px) {
  .order-info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 24px;
  }

  .filters-bar {
    flex-direction: column;
  }

  .filters-bar select {
    width: 100%;
  }

  .bulk-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .bulk-actions select {
    width: 100%;
  }

  .order-info-grid {
    grid-template-columns: 1fr;
  }

  .modal-content {
    width: 95%;
  }

  .action-buttons {
    flex-wrap: wrap;
  }
}

@media print {
  .admin-header,
  .filters-bar,
  .bulk-actions,
  .pagination,
  .action-buttons,
  .modal-footer {
    display: none;
  }

  .modal-content {
    box-shadow: none;
    padding: 0;
  }

  table {
    border: 1px solid #000;
  }
}
</style>