<template>
  <div class="admin-employees">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>Quản lý nhân viên</h2>
        <p class="subtitle">Quản lý toàn bộ thông tin nhân viên</p>
      </div>
      <button class="btn-add" @click="openCreateModal">
        <span class="btn-icon">＋</span> Thêm nhân viên
      </button>
    </div>

    <!-- Thống kê -->
    <div class="stats-row">
      <div class="stat-card stat-total">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalEmployees || 0 }}</div>
          <div class="stat-label">Tổng nhân viên</div>
        </div>
      </div>
      <div class="stat-card stat-active">
        <div class="stat-icon">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.activeEmployees || 0 }}</div>
          <div class="stat-label">Đang hoạt động</div>
        </div>
      </div>
      <div class="stat-card stat-locked">
        <div class="stat-icon">🔒</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.lockedEmployees || 0 }}</div>
          <div class="stat-label">Tạm khóa</div>
        </div>
      </div>
      <div class="stat-card stat-deleted">
        <div class="stat-icon">🗑️</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.deletedEmployees || 0 }}</div>
          <div class="stat-label">Đã xóa</div>
        </div>
      </div>
    </div>

    <!-- Hiển thị lỗi nếu có -->
    <div v-if="error" class="error-alert">
      <span class="error-icon">⚠️</span>
      <span class="error-message">{{ error }}</span>
      <button class="error-close" @click="clearError">✕</button>
    </div>

    <!-- Tìm kiếm và lọc -->
    <div class="filter-bar">
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchKeyword"
          @keyup.enter="handleSearch"
          placeholder="Tìm theo tên, số điện thoại, email, chức vụ..."
          autocomplete="off"
        />
        <button v-if="searchKeyword" class="clear-search" @click="searchKeyword = ''; handleSearch()">✕</button>
      </div>
      <select v-model="filterStatus" @change="handleSearch" class="filter-select">
        <option value="">Tất cả trạng thái</option>
        <option value="ACTIVE">Hoạt động</option>
        <option value="INACTIVE">Không hoạt động</option>
        <option value="BANNED">Tạm khóa</option>
        <option value="DELETED">Đã xóa</option>
      </select>
      <select v-model="pageSize" @change="handleSearch" class="page-size-select">
        <option :value="5">5 / trang</option>
        <option :value="10">10 / trang</option>
        <option :value="20">20 / trang</option>
        <option :value="50">50 / trang</option>
      </select>
      <button class="btn-search" @click="handleSearch">Tìm kiếm</button>
    </div>

    <!-- Bảng nhân viên -->
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th @click="sortBy('employeeId')" class="sortable">
              ID
              <span class="sort-icon" v-if="sortByField === 'employeeId'">
                {{ sortDirection === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th @click="sortBy('employeeFullname')" class="sortable">
              Họ tên
              <span class="sort-icon" v-if="sortByField === 'employeeFullname'">
                {{ sortDirection === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
            <th>Giới tính</th>
            <th>SĐT</th>
            <th>Email</th>
            <th>Chức vụ</th>
            <th>Vai trò</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody v-if="!loading">
          <tr v-for="emp in employees" :key="emp.employeeId" class="table-row" :class="{ 'row-deleted': emp.userStatus === 'DELETED' }">
            <td><span class="id-badge">#{{ emp.employeeId }}</span></td>
            <td>
              <div class="employee-name">
                <div class="avatar">{{ getInitials(emp.employeeFullname) }}</div>
                <span>{{ emp.employeeFullname }}</span>
              </div>
            </td>
            <td>
              <span class="gender-badge" :class="emp.employeeGender ? 'male' : 'female'">
                {{ emp.employeeGender ? '♂ Nam' : '♀ Nữ' }}
              </span>
            </td>
            <td>{{ emp.employeePhone }}</td>
            <td>{{ emp.employeeEmail }}</td>
            <td>{{ emp.employeePosition }}</td>
            <td>
              <span class="role-badge" :class="emp.roleName === 'ADMIN' ? 'role-admin' : 'role-staff'">
                {{ emp.roleName === 'ADMIN' ? 'Quản trị viên' : 'Nhân viên' }}
              </span>
            </td>
            <td>
              <span class="status-badge" :class="getStatusClass(emp)">
                {{ getStatusText(emp) }}
              </span>
            </td>
            <td class="actions">
              <div class="action-buttons">
                <!-- Xem chi tiết -->
                <button class="action-btn view-btn" @click="viewDetail(emp)" title="Xem chi tiết">👁️</button>

                <!-- Sửa & Xóa - chỉ hiện khi chưa xóa -->
                <template v-if="emp.userStatus !== 'DELETED'">
                  <button 
                    class="action-btn edit-btn" 
                    @click="openEditModal(emp)" 
                    title="Sửa">
                    ✏️
                  </button>
                  
                  <!-- Xóa mềm -->
                  <button class="action-btn delete-btn" @click="confirmDelete(emp)" title="Xóa">🗑️</button>
                </template>

                <!-- Đã xóa: chỉ xem + nút khôi phục -->
                <template v-else>
                  <span class="readonly-label">Chỉ xem</span>
                  <button class="action-btn restore-btn" @click="confirmRestore(emp)" title="Khôi phục">↩️</button>
                </template>
              </div>
            </td>
          </tr>
          <tr v-if="employees.length === 0 && !loading">
            <td colspan="10" class="empty-row">
              <div class="empty-state">
                <div class="empty-icon">📭</div>
                <p>Không có dữ liệu nhân viên</p>
                <button class="btn-retry" @click="handleSearch">Tải lại</button>
              </div>
            </td>
          </tr>
        </tbody>
        <tbody v-if="loading">
          <tr>
            <td colspan="10" class="loading-row">
              <div class="loading-spinner"></div>
              <span>Đang tải dữ liệu...</span>
            </td>
          </tr>
        </tbody>
       </table>
    </div>

    <!-- Phân trang -->
    <div class="pagination" v-if="pagination.totalPages > 0">
      <span class="page-total">Tổng: {{ pagination.totalElements }} nhân viên</span>
      <div class="page-controls">
        <button @click="changePage(0)" :disabled="pagination.currentPage === 0" title="Trang đầu">«</button>
        <button @click="changePage(pagination.currentPage - 1)" :disabled="pagination.currentPage === 0" title="Trang trước">‹</button>
        <span class="page-info">{{ pagination.currentPage + 1 }} / {{ pagination.totalPages }}</span>
        <button @click="changePage(pagination.currentPage + 1)" :disabled="pagination.currentPage >= pagination.totalPages - 1" title="Trang sau">›</button>
        <button @click="changePage(pagination.totalPages - 1)" :disabled="pagination.currentPage >= pagination.totalPages - 1" title="Trang cuối">»</button>
      </div>
    </div>

    <!-- Modal -->
    <EmployeeModal
      v-if="showModal"
      :isEdit="isEdit"
      :initialData="selectedEmployee"
      :isSubmitting="isSubmitting"
      @close="closeModal"
      @submit="handleSubmit"
    />

    <!-- Detail Modal -->
    <div v-if="showDetailModal" class="modal" @click.self="closeDetailModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Chi tiết nhân viên</h2>
          <button class="close-btn" @click="closeDetailModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="detail-item"><strong>ID:</strong> {{ selectedEmployeeDetail?.employeeId }}</div>
          <div class="detail-item"><strong>Họ tên:</strong> {{ selectedEmployeeDetail?.employeeFullname }}</div>
          <div class="detail-item"><strong>Email:</strong> {{ selectedEmployeeDetail?.email }}</div>
          <div class="detail-item"><strong>SĐT:</strong> {{ selectedEmployeeDetail?.employeePhone }}</div>
          <div class="detail-item"><strong>Giới tính:</strong> {{ selectedEmployeeDetail?.employeeGender ? 'Nam' : 'Nữ' }}</div>
          <div class="detail-item"><strong>Chức vụ:</strong> {{ selectedEmployeeDetail?.employeePosition }}</div>
          <div class="detail-item"><strong>Vai trò:</strong> {{ selectedEmployeeDetail?.roleName === 'ADMIN' ? 'Quản trị viên' : 'Nhân viên' }}</div>
          <div class="detail-item"><strong>Trạng thái:</strong> {{ getStatusText(selectedEmployeeDetail) }}</div>
          <div class="detail-item"><strong>Ngày tạo:</strong> {{ formatDate(selectedEmployeeDetail?.createdAt) }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeDetailModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Confirm Dialog -->
    <ConfirmDialog
      v-if="showConfirmDialog"
      :visible="showConfirmDialog"
      :title="confirmTitle"
      :message="confirmMessage"
      :confirm-text="confirmButtonText"
      @confirm="handleConfirm"
      @cancel="closeConfirmDialog"
    />

    <!-- Toast -->
    <div :class="['toast', { 'toast-error': toastType === 'error', show: toastShow }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script>
import { nextTick } from 'vue'
import { useEmployeeStore } from '@/stores/employeeStore'
import EmployeeModal from '@/components/admin/EmployeeModal.vue'
import ConfirmDialog from '@/components/admin/ConfirmDialog.vue'

export default {
  name: 'AdminEmployees',
  components: { EmployeeModal, ConfirmDialog },
  data() {
    return {
      searchKeyword: '',
      filterStatus: '',
      sortByField: 'employeeId',
      sortDirection: 'desc',
      showModal: false,
      isSubmitting: false,
      showDetailModal: false,
      showConfirmDialog: false,
      isEdit: false,
      selectedEmployee: null,
      selectedEmployeeDetail: null,
      currentPage: 0,
      pageSize: 10,
      toastShow: false,
      toastMessage: '',
      toastType: 'success',
      toastTimer: null,
      confirmTitle: '',
      confirmMessage: '',
      confirmButtonText: '',
      confirmAction: '',
      pendingEmployee: null
    }
  },
  computed: {
    employees() {
      return useEmployeeStore().getEmployees
    },
    statistics() {
      return useEmployeeStore().getStatistics
    },
    pagination() {
      return useEmployeeStore().pagination
    },
    loading() {
      return useEmployeeStore().isLoading
    },
    error() {
      return useEmployeeStore().error
    }
  },
  mounted() {
    this.loadData(0)
    this.loadStatistics()
  },
  methods: {
    getInitials(name) {
      if (!name) return '?'
      const parts = name.trim().split(' ')
      return parts[parts.length - 1].charAt(0).toUpperCase()
    },

    getStatusClass(emp) {
      if (emp.userStatus === 'DELETED') return 'status-deleted'
      if (emp.userStatus === 'INACTIVE') return 'status-inactive'
      if (emp.isLocked) return 'status-locked'
      return 'status-active'
    },

    getStatusText(emp) {
      if (emp.userStatus === 'DELETED') return 'Đã xóa'
      if (emp.userStatus === 'INACTIVE') return 'Không hoạt động'
      if (emp.isLocked) return 'Tạm khóa'
      return 'Hoạt động'
    },

    formatDate(dateString) {
      if (!dateString) return '---'
      return new Date(dateString).toLocaleDateString('vi-VN')
    },

    showToast(message, isError = false) {
      if (this.toastTimer) clearTimeout(this.toastTimer)
      this.toastMessage = message
      this.toastType = isError ? 'error' : 'success'
      this.toastShow = true
      this.toastTimer = setTimeout(() => {
        this.toastShow = false
      }, 3000)
    },

    clearError() {
      useEmployeeStore().error = null
    },

    async loadData(page = 0) {
      this.currentPage = page
      try {
        await useEmployeeStore().searchEmployees({
          keyword: this.searchKeyword,
          status: this.filterStatus,
          page: page,
          size: this.pageSize,
          sortBy: this.sortByField,
          direction: this.sortDirection
        })
      } catch (error) {
        console.error('Load data error:', error)
        this.showToast('Không thể tải dữ liệu. Vui lòng kiểm tra kết nối server!', true)
      }
    },

    async loadStatistics() {
      try {
        await useEmployeeStore().loadStatistics()
      } catch (error) {
        console.error('Load statistics error:', error)
      }
    },

    async handleSearch() {
      await this.loadData(0)
    },

    async sortBy(field) {
      if (this.sortByField === field) {
        this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc'
      } else {
        this.sortByField = field
        this.sortDirection = 'asc'
      }
      await this.loadData(0)
    },

    async changePage(page) {
      await this.loadData(page)
    },

    openCreateModal() {
      useEmployeeStore().error = null // ✅ Xoá banner lỗi cũ (vd: từ lần tìm kiếm/tải danh sách trước đó)
      this.isEdit = false
      this.selectedEmployee = null
      this.showModal = true
    },

    // openEditModal(employee) {
    //   this.isEdit = true
    //   this.selectedEmployee = {
    //     employeeId: employee.employeeId,
    //     userId: employee.userId,
    //     employeeFullname: employee.employeeFullname,
    //     employeeGender: employee.employeeGender,
    //     employeePhone: employee.employeePhone,
    //     employeeEmail: employee.employeeEmail,
    //     employeePosition: employee.employeePosition,
    //     roleName: employee.roleName
    //   }
    //   this.showModal = true
    // },

    
    // Trong AdminEmployees.vue, sửa method openEditModal
openEditModal(employee) {
  this.isEdit = true
  // ✅ Load đầy đủ dữ liệu từ employee hiện tại
  this.selectedEmployee = {
    employeeId: employee.employeeId,
    userId: employee.userId,
    employeeFullname: employee.employeeFullname,                                                                                                                                                                                                                                        
    employeeGender: employee.employeeGender,
    employeePhone: employee.employeePhone,
    employeeEmail: employee.employeeEmail,
    employeePosition: employee.employeePosition,
    roleName: employee.roleName,
    userStatus: employee.userStatus || (employee.isLocked ? 'BANNED' : 'ACTIVE')
  }
  this.showModal = true
},


    viewDetail(employee) {
      this.selectedEmployeeDetail = employee
      this.showDetailModal = true
    },

    closeDetailModal() {
      this.showDetailModal = false
      this.selectedEmployeeDetail = null
    },

    closeModal() {
      this.showModal = false
      this.selectedEmployee = null
    },

    async handleSubmit(formData) {
      if (this.isSubmitting) return // ✅ Chặn bấm submit nhiều lần liên tiếp
      this.isSubmitting = true
      useEmployeeStore().error = null
      const wasEdit = this.isEdit

      let result
      try {
        if (wasEdit) {
          const id = this.selectedEmployee?.employeeId
          if (!id) return
          result = await useEmployeeStore().updateEmployee(id, formData)
        } else {
          result = await useEmployeeStore().createEmployee(formData)
        }
      } finally {
        this.isSubmitting = false
      }

      if (result.success) {
        this.closeModal()
        await this.loadData(0)
        await this.loadStatistics()
        await nextTick()
        this.showToast(wasEdit ? 'Cập nhật thành công!' : 'Thêm nhân viên thành công!')
      } else {
        this.showToast(result.error || 'Có lỗi xảy ra!', true)
      }
    },

    confirmToggleLock(employee) {
      this.pendingEmployee = employee
      const isLocking = !employee.isLocked
      this.confirmTitle = isLocking ? 'Xác nhận khóa tài khoản' : 'Xác nhận mở khóa'
      this.confirmMessage = isLocking 
        ? `Khóa tài khoản của "${employee.employeeFullname}"?` 
        : `Mở khóa tài khoản của "${employee.employeeFullname}"?`
      this.confirmButtonText = isLocking ? 'Khóa' : 'Mở khóa'
      this.confirmAction = 'toggleLock'
      this.showConfirmDialog = true
    },

    confirmDelete(employee) {
      this.pendingEmployee = employee
      this.confirmTitle = 'Xác nhận xóa'
      this.confirmMessage = `Bạn có chắc chắn muốn xóa nhân viên "${employee.employeeFullname}"?`
      this.confirmButtonText = 'Xóa'
      this.confirmAction = 'delete'
      this.showConfirmDialog = true
    },

    confirmRestore(employee) {
      this.pendingEmployee = employee
      this.confirmTitle = 'Xác nhận khôi phục'
      this.confirmMessage = `Khôi phục nhân viên "${employee.employeeFullname}"?`
      this.confirmButtonText = 'Khôi phục'
      this.confirmAction = 'restore'
      this.showConfirmDialog = true
    },

    closeConfirmDialog() {
      this.showConfirmDialog = false
      this.pendingEmployee = null
      this.confirmAction = ''
    },

    async handleConfirm() {
      try {
        if (this.confirmAction === 'toggleLock') {
          await useEmployeeStore().toggleLockEmployee(this.pendingEmployee.employeeId)
          this.showToast(`Đã ${this.confirmButtonText} tài khoản "${this.pendingEmployee.employeeFullname}"!`)
        } else if (this.confirmAction === 'delete') {
          await useEmployeeStore().softDeleteEmployee(this.pendingEmployee.employeeId)
          this.showToast(`Đã xóa nhân viên "${this.pendingEmployee.employeeFullname}"!`)
        } else if (this.confirmAction === 'restore') {
          await useEmployeeStore().restoreEmployee(this.pendingEmployee.employeeId)
          this.showToast(`Đã khôi phục nhân viên "${this.pendingEmployee.employeeFullname}"!`)
        }
        await this.loadData(this.currentPage)
        await this.loadStatistics()
      } catch (error) {
        this.showToast(error.message || 'Có lỗi xảy ra!', true)
      } finally {
        this.closeConfirmDialog()
      }
    }
  }
}
</script>

<style scoped>
/* Giữ nguyên style cũ và thêm các style mới theo phong cách AdminSizes */

.admin-employees {
  padding: 20px;
  background-color: #f8f9fa;
  min-height: calc(100vh - 60px);
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h2 {
  margin: 0 0 5px 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #7f8c8d;
  font-size: 14px;
}

.btn-add {
  background-color: #3498db;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-add:hover {
  background-color: #2980b9;
  transform: translateY(-2px);
}

/* Stats Row */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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

.stat-total { border-left-color: #3b82f6; }
.stat-active { border-left-color: #10b981; }
.stat-locked { border-left-color: #f59e0b; }
.stat-deleted { border-left-color: #ef4444; }

.stat-icon { font-size: 32px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a2e; }
.stat-label { font-size: 12px; color: #6b7280; margin-top: 4px; }

/* Filter Bar */
.filter-bar {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  background: white;
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  min-width: 250px;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #95a5a6;
}

.search-box input {
  width: 100%;
  padding: 10px 35px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s;
  font-size: 14px;
}

.search-box input:focus {
  border-color: #3498db;
}

.clear-search {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  color: #95a5a6;
  cursor: pointer;
}

.filter-select, .page-size-select {
  padding: 10px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  outline: none;
  background-color: white;
  cursor: pointer;
}

.btn-search {
  padding: 10px 20px;
  background-color: #2c3e50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-search:hover {
  background-color: #34495e;
}

/* Table */
.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th, .data-table td {
  padding: 15px;
  text-align: left;
  border-bottom: 1px solid #f1f4f8;
}

.data-table th {
  background-color: #f8f9fa;
  color: #2c3e50;
  font-weight: 600;
  font-size: 14px;
  text-transform: uppercase;
}

.sortable {
  cursor: pointer;
  user-select: none;
}

.sort-icon {
  margin-left: 4px;
}

.table-row:hover {
  background: #f9fafb;
}

.row-deleted {
  opacity: 0.55;
  background-color: #fafafa;
}

.row-deleted td {
  color: #9ca3af;
}

.id-badge {
  background: #f1f4f8;
  padding: 4px 8px;
  border-radius: 6px;
  font-family: monospace;
  font-size: 13px;
  color: #7f8c8d;
}

.employee-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  color: #2c3e50;
}

.avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.gender-badge {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 12px;
  font-weight: 500;
}
.gender-badge.male { background: #dbeafe; color: #1d4ed8; }
.gender-badge.female { background: #fce7f3; color: #be185d; }

.role-badge {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 12px;
  font-weight: 500;
}
.role-badge.role-admin { background: #fef3c7; color: #92400e; }
.role-badge.role-staff { background: #e0e7ff; color: #4338ca; }

.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}
.status-active { background-color: #e8f8f5; color: #27ae60; }
.status-inactive { background-color: #fdf2e9; color: #f39c12; }
.status-locked { background-color: #fee2e2; color: #e74c3c; }
.status-deleted { background-color: #f3f4f6; color: #9ca3af; }

.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.action-btn {
  background: none;
  border: none;
  padding: 6px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.view-btn:hover { background-color: #e8f4f8; }
.edit-btn:hover { background-color: #fef3c7; }
.lock-btn:hover { background-color: #fde8e8; }
.unlock-btn:hover { background-color: #e8f8f5; }
.delete-btn:hover { background-color: #fde8e8; }
.restore-btn:hover { background-color: #e8f4f8; }

.readonly-label {
  font-size: 12px;
  color: #9ca3af;
  font-style: italic;
  padding: 4px 8px;
  background: #f3f4f6;
  border-radius: 6px;
  white-space: nowrap;
}

/* Pagination */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 15px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.page-total {
  font-size: 14px;
  color: #6b7280;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-controls button {
  padding: 5px 12px;
  border: 1px solid #e0e6ed;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-controls button:hover:not(:disabled) {
  background: #3498db;
  color: white;
  border-color: #3498db;
}

.page-controls button:disabled {
  background: #f8f9fa;
  color: #bdc3c7;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #4b5563;
  font-weight: 500;
  min-width: 70px;
  text-align: center;
}

/* Modal details */
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
  animation: fadeIn 0.2s ease-in-out;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  overflow-y: auto;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
  color: #1f2937;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #9ca3af;
  cursor: pointer;
}

.close-btn:hover {
  color: #1f2937;
}

.modal-body {
  padding: 20px;
}

.detail-item {
  margin-bottom: 10px;
  color: #374151;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #f3f4f6;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-outline {
  padding: 8px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: #4b5563;
  transition: background 0.2s;
}

.btn-outline:hover {
  background: #e5e7eb;
}

/* Error/Empty/Loading */
.error-alert {
  background: #fee2e2;
  border-left: 4px solid #ef4444;
  padding: 12px 16px;
  margin-bottom: 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.loading-row {
  text-align: center;
  padding: 40px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  display: inline-block;
  margin-right: 8px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-row {
  text-align: center;
  padding: 40px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
}

.empty-icon {
  font-size: 40px;
}

.btn-retry {
  margin-top: 10px;
  padding: 8px 16px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
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
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}
</style>