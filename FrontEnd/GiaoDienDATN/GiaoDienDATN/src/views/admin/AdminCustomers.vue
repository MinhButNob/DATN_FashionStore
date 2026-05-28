<template>
  <div class="admin-customers">
    <div class="admin-header">
      <h1 class="page-title">Quản lý khách hàng</h1>
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="filters.search"
          placeholder="Tìm kiếm theo tên, email, SĐT..."
          @input="applyFilters"
        >
      </div>

      <select v-model="filters.status" @change="applyFilters">
        <option value="all">Tất cả trạng thái</option>
        <option value="active">Hoạt động</option>
        <option value="inactive">Không hoạt động</option>
      </select>

      <select v-model="filters.sort" @change="applyFilters">
        <option value="newest">Mới nhất</option>
        <option value="oldest">Cũ nhất</option>
        <option value="most-orders">Nhiều đơn nhất</option>
        <option value="highest-spent">Chi tiêu cao nhất</option>
      </select>
    </div>

    <!-- Customers Table -->
    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Khách hàng</th>
            <th>Liên hệ</th>
            <th>Địa chỉ</th>
            <th>Tổng đơn</th>
            <th>Tổng chi tiêu</th>
            <th>Lần cuối mua</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="customer in paginatedCustomers" :key="customer.id">
            <td>#{{ customer.id }}</td>
            <td>
              <div class="customer-info">
                <div class="customer-avatar">
                  <i class="fas fa-user-circle"></i>
                </div>
                <div>
                  <div class="customer-name">{{ customer.name }}</div>
                  <div class="customer-date">Đã tham gia: {{ customer.joinedDate }}</div>
                </div>
              </div>
            </td>
            <td>
              <div class="contact-info">
                <div><i class="fas fa-envelope"></i> {{ customer.email }}</div>
                <div><i class="fas fa-phone"></i> {{ customer.phone }}</div>
              </div>
            </td>
            <td>{{ customer.address || 'Chưa cập nhật' }}</td>
            <td class="text-center">{{ customer.totalOrders }}</td>
            <td>
              <span class="total-spent">{{ formatPrice(customer.totalSpent) }}</span>
            </td>
            <td>{{ customer.lastPurchase || 'Chưa mua' }}</td>
            <td>
              <span 
                class="status-badge" 
                :class="customer.status === 'active' ? 'status-active' : 'status-inactive'"
              >
                {{ customer.status === 'active' ? 'Hoạt động' : 'Không hoạt động' }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button 
                  class="action-btn view-btn" 
                  @click="viewCustomerDetail(customer)"
                  title="Xem chi tiết"
                >
                  <i class="fas fa-eye"></i>
                </button>
                <button 
                  class="action-btn edit-btn" 
                  @click="editCustomer(customer)"
                  title="Sửa"
                >
                  <i class="fas fa-edit"></i>
                </button>
                <button 
                  class="action-btn delete-btn" 
                  @click="confirmDelete(customer.id)"
                  title="Xóa"
                >
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredCustomers.length === 0">
            <td colspan="9" class="no-data">
              Không tìm thấy khách hàng nào
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

    <!-- Customer Detail Modal -->
    <div v-if="showDetailModal" class="modal">
      <div class="modal-content modal-lg">
        <div class="modal-header">
          <h2>Chi tiết khách hàng</h2>
          <button class="close-btn" @click="closeDetailModal">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="modal-body">
          <div class="customer-profile">
            <div class="profile-header">
              <div class="profile-avatar">
                <i class="fas fa-user-circle"></i>
              </div>
              <div class="profile-info">
                <h3>{{ selectedCustomer?.name }}</h3>
                <p>ID: #{{ selectedCustomer?.id }}</p>
                <p>Đã tham gia: {{ selectedCustomer?.joinedDate }}</p>
              </div>
              <span 
                class="status-badge" 
                :class="selectedCustomer?.status === 'active' ? 'status-active' : 'status-inactive'"
              >
                {{ selectedCustomer?.status === 'active' ? 'Hoạt động' : 'Không hoạt động' }}
              </span>
            </div>

            <div class="profile-stats">
              <div class="stat-card">
                <div class="stat-icon">
                  <i class="fas fa-shopping-bag"></i>
                </div>
                <div class="stat-info">
                  <span class="stat-label">Tổng đơn hàng</span>
                  <span class="stat-value">{{ selectedCustomer?.totalOrders }}</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon">
                  <i class="fas fa-dollar-sign"></i>
                </div>
                <div class="stat-info">
                  <span class="stat-label">Tổng chi tiêu</span>
                  <span class="stat-value">{{ formatPrice(selectedCustomer?.totalSpent) }}</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon">
                  <i class="fas fa-calendar-check"></i>
                </div>
                <div class="stat-info">
                  <span class="stat-label">Lần cuối mua</span>
                  <span class="stat-value">{{ selectedCustomer?.lastPurchase || 'Chưa mua' }}</span>
                </div>
              </div>
            </div>

            <div class="profile-details">
              <div class="details-section">
                <h4>Thông tin liên hệ</h4>
                <p><strong>Email:</strong> {{ selectedCustomer?.email }}</p>
                <p><strong>Số điện thoại:</strong> {{ selectedCustomer?.phone }}</p>
                <p><strong>Địa chỉ:</strong> {{ selectedCustomer?.address || 'Chưa cập nhật' }}</p>
              </div>

              <div class="details-section">
                <h4>Đơn hàng gần đây</h4>
                <div v-if="recentOrders.length === 0" class="no-orders">
                  Khách hàng chưa có đơn hàng nào
                </div>
                <div v-else class="recent-orders">
                  <div v-for="order in recentOrders" :key="order.id" class="recent-order">
                    <div class="order-header">
                      <span class="order-id">Mã đơn: {{ order.id }}</span>
                      <span class="order-date">{{ order.date }}</span>
                    </div>
                    <div class="order-products">
                      {{ order.items.length }} sản phẩm
                    </div>
                    <div class="order-footer">
                      <span class="order-total">{{ formatPrice(order.total) }}</span>
                      <span class="order-status" :class="`status-${order.status}`">
                        {{ getStatusText(order.status) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-primary" @click="sendEmail">
            <i class="fas fa-envelope"></i> Gửi email
          </button>
          <button class="btn btn-outline" @click="closeDetailModal">
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
          <p>Bạn có chắc chắn muốn xóa khách hàng này?</p>
          <p class="warning-text">Hành động này không thể hoàn tác!</p>
        </div>

        <div class="modal-footer">
          <button class="btn btn-danger" @click="deleteCustomer">
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
const customers = ref([
  {
    id: 1,
    name: 'Nguyễn Văn A',
    email: 'nguyenvana@email.com',
    phone: '0123456789',
    address: '123 Nguyễn Huệ, Quận 1, Hồ Chí Minh',
    joinedDate: '01/01/2024',
    totalOrders: 5,
    totalSpent: 2450000,
    lastPurchase: '15/03/2024',
    status: 'active'
  },
  {
    id: 2,
    name: 'Trần Thị B',
    email: 'tranthib@email.com',
    phone: '0987654321',
    address: '456 Lê Lợi, Quận 1, Hồ Chí Minh',
    joinedDate: '15/02/2024',
    totalOrders: 3,
    totalSpent: 1250000,
    lastPurchase: '10/03/2024',
    status: 'active'
  },
  {
    id: 3,
    name: 'Lê Văn C',
    email: 'levanc@email.com',
    phone: '0369852147',
    address: '789 Điện Biên Phủ, Quận 3, Hồ Chí Minh',
    joinedDate: '20/02/2024',
    totalOrders: 2,
    totalSpent: 850000,
    lastPurchase: '05/03/2024',
    status: 'inactive'
  },
  {
    id: 4,
    name: 'Phạm Thị D',
    email: 'phamthid@email.com',
    phone: '0912345678',
    address: '321 Hoàng Diệu, Quận 4, Hồ Chí Minh',
    joinedDate: '01/03/2024',
    totalOrders: 4,
    totalSpent: 1890000,
    lastPurchase: '12/03/2024',
    status: 'active'
  },
  {
    id: 5,
    name: 'Hoàng Văn E',
    email: 'hoangvane@email.com',
    phone: '0975314682',
    address: '654 Võ Văn Kiệt, Quận 5, Hồ Chí Minh',
    joinedDate: '10/03/2024',
    totalOrders: 1,
    totalSpent: 299000,
    lastPurchase: '10/03/2024',
    status: 'active'
  }
])

// Mock orders for customer detail
const recentOrders = ref([
  {
    id: 'DH001',
    date: '15/03/2024',
    items: [{ id: 1, name: 'Áo thun nam', quantity: 2 }],
    total: 398000,
    status: 'delivered'
  },
  {
    id: 'DH002',
    date: '10/03/2024',
    items: [{ id: 2, name: 'Đầm nữ', quantity: 1 }],
    total: 399000,
    status: 'delivered'
  }
])

// State
const filters = ref({
  search: '',
  status: 'all',
  sort: 'newest'
})

const currentPage = ref(1)
const itemsPerPage = 10
const showDetailModal = ref(false)
const showDeleteModal = ref(false)
const selectedCustomer = ref(null)
const deleteId = ref(null)

// Computed
const filteredCustomers = computed(() => {
  let filtered = [...customers.value]

  // Search filter
  if (filters.value.search) {
    const searchTerm = filters.value.search.toLowerCase()
    filtered = filtered.filter(customer =>
      customer.name.toLowerCase().includes(searchTerm) ||
      customer.email.toLowerCase().includes(searchTerm) ||
      customer.phone.includes(searchTerm)
    )
  }

  // Status filter
  if (filters.value.status !== 'all') {
    filtered = filtered.filter(customer => customer.status === filters.value.status)
  }

  // Sort
  switch (filters.value.sort) {
    case 'newest':
      filtered.sort((a, b) => new Date(b.joinedDate.split('/').reverse().join('-')) - 
                                new Date(a.joinedDate.split('/').reverse().join('-')))
      break
    case 'oldest':
      filtered.sort((a, b) => new Date(a.joinedDate.split('/').reverse().join('-')) - 
                                new Date(b.joinedDate.split('/').reverse().join('-')))
      break
    case 'most-orders':
      filtered.sort((a, b) => b.totalOrders - a.totalOrders)
      break
    case 'highest-spent':
      filtered.sort((a, b) => b.totalSpent - a.totalSpent)
      break
  }

  return filtered
})

const paginatedCustomers = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredCustomers.value.slice(start, end)
})

const totalPages = computed(() => {
  return Math.ceil(filteredCustomers.value.length / itemsPerPage)
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

// Methods
const applyFilters = () => {
  currentPage.value = 1
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

const viewCustomerDetail = (customer) => {
  selectedCustomer.value = customer
  showDetailModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedCustomer.value = null
}

const editCustomer = (customer) => {
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

const deleteCustomer = () => {
  const index = customers.value.findIndex(c => c.id === deleteId.value)
  if (index !== -1) {
    customers.value.splice(index, 1)
    showNotification('Xóa khách hàng thành công!', 'success')
  }
  closeDeleteModal()
}

const sendEmail = () => {
  showNotification('Chức năng gửi email đang phát triển', 'info')
}

// Watch for filter changes
watch(filters, () => {
  applyFilters()
}, { deep: true })
</script>

<style scoped>
.admin-customers {
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

.text-center {
  text-align: center;
}

/* Customer Info */
.customer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.customer-avatar {
  width: 40px;
  height: 40px;
  background: var(--light-gray);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.customer-avatar i {
  font-size: 24px;
  color: var(--primary-red);
}

.customer-name {
  font-weight: 600;
  color: var(--dark-gray);
  margin-bottom: 3px;
}

.customer-date {
  font-size: 11px;
  color: var(--medium-gray);
}

/* Contact Info */
.contact-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.contact-info div {
  font-size: 13px;
}

.contact-info i {
  width: 18px;
  color: var(--primary-red);
  font-size: 12px;
}

/* Total Spent */
.total-spent {
  font-weight: 600;
  color: var(--primary-red);
}

/* Status Badge */
.status-badge {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-active {
  background: #d4edda;
  color: #155724;
}

.status-inactive {
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
  max-width: 800px;
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

/* Customer Profile */
.customer-profile {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  position: relative;
  flex-wrap: wrap;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  background: var(--light-gray);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-avatar i {
  font-size: 50px;
  color: var(--primary-red);
}

.profile-info h3 {
  color: var(--dark-gray);
  font-size: 20px;
  margin-bottom: 5px;
}

.profile-info p {
  color: var(--medium-gray);
  font-size: 14px;
}

/* Profile Stats */
.profile-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-card {
  background: var(--light-gray);
  padding: 15px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon i {
  font-size: 24px;
  color: var(--primary-red);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 13px;
  color: var(--medium-gray);
  margin-bottom: 3px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--dark-gray);
}

/* Profile Details */
.profile-details {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 25px;
}

.details-section h4 {
  color: var(--dark-gray);
  margin-bottom: 15px;
  font-size: 16px;
}

.details-section p {
  color: var(--medium-gray);
  margin-bottom: 8px;
  font-size: 14px;
}

.details-section p strong {
  color: var(--dark-gray);
  margin-right: 5px;
}

/* Recent Orders */
.recent-orders {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recent-order {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
  transition: all 0.3s;
}

.recent-order:hover {
  box-shadow: var(--shadow);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.order-id {
  font-weight: 600;
  color: var(--primary-red);
}

.order-date {
  font-size: 12px;
  color: var(--medium-gray);
}

.order-products {
  font-size: 13px;
  color: var(--medium-gray);
  margin-bottom: 8px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-total {
  font-weight: 600;
  color: var(--dark-gray);
}

.order-status {
  padding: 3px 8px;
  border-radius: 15px;
  font-size: 11px;
  font-weight: 600;
}

.order-status.status-pending {
  background: #fff3cd;
  color: #856404;
}

.order-status.status-processing {
  background: #cce5ff;
  color: #004085;
}

.order-status.status-shipped {
  background: #d1ecf1;
  color: #0c5460;
}

.order-status.status-delivered {
  background: #d4edda;
  color: #155724;
}

.order-status.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}

.no-orders {
  color: var(--medium-gray);
  font-size: 14px;
  font-style: italic;
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

/* Responsive */
@media (max-width: 1024px) {
  .profile-stats {
    grid-template-columns: 1fr;
  }

  .profile-details {
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

  .profile-header {
    flex-direction: column;
    text-align: center;
  }

  .profile-header .status-badge {
    align-self: center;
  }

  .modal-content {
    width: 95%;
  }
}
</style>