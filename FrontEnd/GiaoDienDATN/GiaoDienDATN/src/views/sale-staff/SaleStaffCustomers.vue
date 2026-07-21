<!-- views/sale-staff/SaleStaffCustomers.vue -->
<template>
  <div class="sale-staff-customers">
    <div class="page-header">
      <div class="header-left">
        <h2>Quản lý khách hàng</h2>
        <p class="subtitle">Xem và tìm kiếm thông tin khách hàng</p>
      </div>
      <div class="header-right">
        <span class="total-customers">Tổng: {{ totalElements }} khách hàng</span>
      </div>
    </div>

    <!-- Stats -->
    <div class="stats-row">
      <div class="stat-card stat-total">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalElements }}</div>
          <div class="stat-label">Tổng khách hàng</div>
        </div>
      </div>
      <div class="stat-card stat-active">
        <div class="stat-icon">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ activeCount }}</div>
          <div class="stat-label">Hoạt động</div>
        </div>
      </div>
      <div class="stat-card stat-locked">
        <div class="stat-icon">🔒</div>
        <div class="stat-info">
          <div class="stat-value">{{ lockedCount }}</div>
          <div class="stat-label">Tạm khóa</div>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="filters.keyword"
          placeholder="Tìm kiếm theo tên, email, SĐT..."
          @input="onSearch"
        />
      </div>

      <select v-model="filters.status" @change="onFilterChange" class="filter-select">
        <option value="">Tất cả trạng thái</option>
        <option value="ACTIVE">Hoạt động</option>
        <option value="LOCKED">Tạm khóa</option>
        <option value="DELETED">Đã xóa</option>
      </select>

      <button class="btn btn-outline" @click="refreshData">
        <i class="fas fa-sync-alt"></i> Làm mới
      </button>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="loading-container">
      <div class="spinner"></div>
      <p>Đang tải dữ liệu...</p>
    </div>

    <!-- Table -->
    <div v-else class="table-container">
      <table class="customer-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Khách hàng</th>
            <th>Email</th>
            <th>Số điện thoại</th>
            <th>Địa chỉ</th>
            <th>Trạng thái</th>
            <th>Ngày tạo</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="customer in store.customers"
            :key="customer.customerId"
            class="table-row"
            :class="{ 'row-deleted': !customer.isActive && !customer.isLocked }"
          >
            <td>#{{ customer.customerId }}</td>
            <td>
              <div class="customer-info">
                <div class="customer-avatar">
                  <i class="fas fa-user-circle"></i>
                </div>
                <div class="customer-name">{{ customer.customerName }}</div>
              </div>
            </td>
            <td>{{ customer.email }}</td>
            <td>{{ customer.phone || '---' }}</td>
            <td>{{ customer.address || '---' }}</td>
            <td>
              <span
                class="status-badge"
                :class="
                  customer.isLocked
                    ? 'status-locked'
                    : (!customer.isActive && !customer.isLocked)
                    ? 'status-deleted'
                    : 'status-active'
                "
              >
                {{
                  customer.isLocked
                    ? 'Tạm khóa'
                    : (!customer.isActive && !customer.isLocked)
                    ? 'Đã xóa'
                    : 'Hoạt động'
                }}
              </span>
            </td>
            <td>{{ formatDate(customer.createdAt) }}</td>
          </tr>
          <tr v-if="store.customers.length === 0">
            <td colspan="7" class="no-data">Không tìm thấy khách hàng nào</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="store.pagination.totalPages > 0">
      <button
        class="page-btn"
        :disabled="store.pagination.currentPage === 0"
        @click="goToPage(store.pagination.currentPage - 1)"
      >
        <i class="fas fa-chevron-left"></i>
      </button>
      <span class="page-info">
        Trang {{ store.pagination.currentPage + 1 }} / {{ store.pagination.totalPages }}
      </span>
      <button
        class="page-btn"
        :disabled="store.pagination.currentPage === store.pagination.totalPages - 1"
        @click="goToPage(store.pagination.currentPage + 1)"
      >
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- Toast -->
    <div :class="['toast', { show: showToast, 'toast-error': toastType === 'error' }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useSaleStaffStore } from '../../stores/saleStaffStore'
import { useNotification } from '../../composables/useNotification'

const store = useSaleStaffStore()
const { showNotification } = useNotification()

const filters = ref({
  keyword: '',
  status: ''
})

const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

const totalElements = computed(() => store.pagination.totalElements)
const activeCount = computed(() => {
  return store.customers.filter(c => !c.isLocked && c.isActive).length
})
const lockedCount = computed(() => {
  return store.customers.filter(c => c.isLocked).length
})

const showToastMessage = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 3000)
}

const formatDate = (dateString) => {
  if (!dateString) return '---'
  return new Date(dateString).toLocaleDateString('vi-VN')
}

const fetchCustomers = async () => {
  try {
    await store.fetchCustomers(store.pagination.currentPage, store.pagination.pageSize)
  } catch (error) {
    showToastMessage('Không thể tải danh sách khách hàng', 'error')
  }
}

const onSearch = () => {
  store.filters.keyword = filters.value.keyword
  fetchCustomers()
}

const onFilterChange = () => {
  store.filters.status = filters.value.status
  fetchCustomers()
}

const refreshData = () => {
  filters.value = { keyword: '', status: '' }
  store.resetFilters()
  fetchCustomers()
  showToastMessage('Đã làm mới dữ liệu', 'success')
}

const goToPage = (page) => {
  store.fetchCustomers(page, store.pagination.pageSize)
}

const loadData = async () => {
  await fetchCustomers()
}

onMounted(() => {
  loadData()
})

// Watch for filter changes
watch(() => store.filters, () => {
  fetchCustomers()
}, { deep: true })
</script>

<style scoped>
.sale-staff-customers {
  padding: 20px;
  animation: fadeIn 0.3s;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.header-left h2 {
  margin: 0 0 5px 0;
  color: #2c3e50;
  font-size: 24px;
}

.subtitle {
  margin: 0;
  color: #999;
  font-size: 14px;
}

.total-customers {
  font-size: 14px;
  color: #666;
  background: #f5f5f5;
  padding: 8px 16px;
  border-radius: 20px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  border-left: 4px solid;
}

.stat-total { border-left-color: #3498db; }
.stat-active { border-left-color: #2ecc71; }
.stat-locked { border-left-color: #e74c3c; }

.stat-icon { font-size: 32px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a2e; }
.stat-label { font-size: 12px; color: #999; }

.filters-bar {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
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

.search-box input {
  border: none;
  background: none;
  padding: 12px;
  width: 100%;
  outline: none;
}

.filter-select {
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  min-width: 180px;
  cursor: pointer;
  background: white;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-outline {
  background: none;
  border: 1px solid #ddd;
  color: #666;
}

.btn-outline:hover {
  background: #f5f5f5;
}

.table-container {
  background: white;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
}

.customer-table {
  width: 100%;
  border-collapse: collapse;
}

.customer-table th {
  background: #f8f9fa;
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: #555;
  border-bottom: 2px solid #eee;
}

.customer-table td {
  padding: 15px;
  border-bottom: 1px solid #eee;
  color: #666;
}

.customer-table tr:hover {
  background: #f8f9fa;
}

.customer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.customer-avatar {
  width: 40px;
  height: 40px;
  background: #f5f5f5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.customer-avatar i {
  font-size: 24px;
  color: #e63946;
}

.customer-name {
  font-weight: 500;
  color: #333;
}

.row-deleted {
  opacity: 0.6;
  background: #fafafa;
}

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

.status-locked {
  background: #f8d7da;
  color: #721c24;
}

.status-deleted {
  background: #f3f4f6;
  color: #9ca3af;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px;
  background: white;
  border-radius: 15px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #e63946;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.no-data {
  text-align: center;
  padding: 40px;
  color: #999;
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

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }

  .filters-bar {
    flex-direction: column;
  }

  .search-box {
    min-width: auto;
    width: 100%;
  }

  .filter-select {
    width: 100%;
  }

  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>