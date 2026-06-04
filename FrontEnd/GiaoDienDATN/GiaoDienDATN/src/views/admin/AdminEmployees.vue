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
        <option value="ACTIVE">Đang hoạt động</option>
        <option value="INACTIVE">Không hoạt động</option>
        <option value="ON_LEAVE">Nghỉ phép</option>
        <!-- ✅ Thêm TERMINATED để có thể lọc nhân viên đã nghỉ việc -->
        <option value="TERMINATED">Đã nghỉ việc</option>
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
            <th>Tuổi</th>
            <th>Số điện thoại</th>
            <th>Email</th>
            <th>Chức vụ</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody v-if="!loading">
          <tr v-for="emp in employees" :key="emp.employeeId" class="table-row">
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
            <td>{{ emp.employeeAge }}</td>
            <td>{{ emp.employeePhone }}</td>
            <td>{{ emp.employeeEmail }}</td>
            <td>{{ emp.employeePosition }}</td>
            <td>
              <select
                :value="emp.employeeStatus"
                @change="updateStatus(emp.employeeId, $event.target.value)"
                class="status-select"
                :class="'status-' + emp.employeeStatus"
              >
                <option value="ACTIVE">Đang hoạt động</option>
                <option value="INACTIVE">Không hoạt động</option>
                <option value="ON_LEAVE">Nghỉ phép</option>
                <!-- ✅ Thêm TERMINATED -->
                <option value="TERMINATED">Đã nghỉ việc</option>
              </select>
            </td>
            <td class="actions">
              <button class="btn-edit" @click="openEditModal(emp)" title="Sửa">✏️</button>
              <button class="btn-delete" @click="confirmDelete(emp.employeeId)" title="Xoá">🗑️</button>
            </td>
          </tr>
          <tr v-if="employees.length === 0 && !loading">
            <td colspan="9" class="empty-row">
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
            <td colspan="9" class="loading-row">
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
      @close="closeModal"
      @submit="handleSubmit"
    />

    <!-- ✅ Toast dùng class động thay vì document.querySelector -->
    <div :class="['toast', { 'toast-error': toastType === 'error', show: toastShow }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script>
import { nextTick } from 'vue'
import { useEmployeeStore } from '@/stores/employeeStore'
import EmployeeModal from '@/components/admin/EmployeeModal.vue'

export default {
  name: 'AdminEmployees',
  components: { EmployeeModal },
  data() {
    return {
      searchKeyword: '',
      filterStatus: '',
      sortByField: 'employeeId',
      sortDirection: 'desc',
      showModal: false,
      isEdit: false,
      selectedEmployee: null,
      currentPage: 0,
      pageSize: 5,
      toastShow: false,
      toastMessage: '',
      toastType: 'success', // ✅ 'success' | 'error' — dùng reactive thay vì DOM manipulation
      toastTimer: null
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

    // ✅ Dùng toastType reactive, không dùng document.querySelector
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
      this.isEdit = false
      this.selectedEmployee = null
      this.showModal = true
    },

    openEditModal(employee) {
      this.isEdit = true
      this.selectedEmployee = {
        employeeId: employee.employeeId,
        userId: employee.userId,
        employeeFullname: employee.employeeFullname,
        employeeGender: employee.employeeGender,
        employeeAge: employee.employeeAge,
        employeePhone: employee.employeePhone,
        employeeEmail: employee.employeeEmail,
        employeePosition: employee.employeePosition,
        employeeStatus: employee.employeeStatus
      }
      this.showModal = true
    },

    closeModal() {
      this.showModal = false
      this.selectedEmployee = null
    },

    async handleSubmit(formData) {
      useEmployeeStore().error = null

      // ✅ Lưu lại isEdit TRƯỚC KHI closeModal reset nó
      const wasEdit = this.isEdit

      let result
      if (wasEdit) {
        const id = this.selectedEmployee?.employeeId
        if (!id) return
        result = await useEmployeeStore().updateEmployee(id, formData)
      } else {
        result = await useEmployeeStore().createEmployee(formData)
      }

      if (result.success) {
        this.closeModal()
        await this.loadData(0)
        await this.loadStatistics()
        await nextTick()
        // ✅ Dùng wasEdit thay vì this.isEdit (đã bị reset bởi closeModal)
        this.showToast(wasEdit ? 'Cập nhật thành công!' : 'Thêm nhân viên thành công!')
      } else {
        this.showToast(result.error || 'Có lỗi xảy ra!', true)
      }
    },

    async updateStatus(id, status) {
      const result = await useEmployeeStore().updateEmployeeStatus(id, status)
      if (result.success) {
        await this.loadData(this.currentPage)
        await this.loadStatistics()
        await nextTick()
        this.showToast('Cập nhật trạng thái thành công!')
      } else {
        this.showToast(result.error || 'Cập nhật thất bại!', true)
        // Reload lại để đưa dropdown về đúng giá trị cũ
        await this.loadData(this.currentPage)
      }
    },

    async confirmDelete(id) {
      if (confirm('Bạn có chắc muốn xoá nhân viên này?')) {
        const result = await useEmployeeStore().deleteEmployee(id)
        if (result.success) {
          await this.loadData(0)
          await this.loadStatistics()
          await nextTick()
          this.showToast('Xoá nhân viên thành công!')
        } else {
          this.showToast(result.error || 'Xoá thất bại!', true)
        }
      }
    }
  }
}
</script>

<style scoped>
.admin-employees {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
  font-family: 'Segoe UI', sans-serif;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.header-left h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
}
.subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #888;
}
.btn-add {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(59,130,246,0.3);
}
.btn-add:hover {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59,130,246,0.4);
}
.btn-icon { font-size: 16px; font-weight: 400; }

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
.error-icon { font-size: 18px; }
.error-message { flex: 1; color: #991b1b; font-size: 14px; }
.error-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #991b1b;
  font-size: 16px;
  padding: 0 4px;
}
.error-close:hover { opacity: 0.7; }

.stats-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  padding: 20px 24px;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  border-left: 4px solid transparent;
  transition: all 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}
.stat-total { border-left-color: #3b82f6; }
.stat-active { border-left-color: #10b981; }
.stat-icon { font-size: 28px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a2e; line-height: 1; }
.stat-label { font-size: 12px; color: #888; margin-top: 4px; }

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}
.search-box {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}
.search-icon { position: absolute; left: 12px; font-size: 14px; }
.search-box input {
  width: 100%;
  padding: 10px 36px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: all 0.2s;
  box-sizing: border-box;
}
.search-box input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.1);
}
.clear-search {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  color: #aaa;
  font-size: 12px;
  padding: 2px 6px;
}
.clear-search:hover { color: #555; }
.filter-select {
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  min-width: 180px;
}
.filter-select:focus { outline: none; border-color: #3b82f6; }
.page-size-select {
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  min-width: 120px;
}
.page-size-select:focus { outline: none; border-color: #3b82f6; }
.btn-search {
  padding: 10px 20px;
  background: #1a1a2e;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  transition: all 0.2s;
}
.btn-search:hover { background: #2d2d4e; }

.table-container {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.data-table { width: 100%; border-collapse: collapse; }
.data-table th {
  padding: 13px 16px;
  text-align: left;
  background: #f8fafc;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #e5e7eb;
}
.data-table th.sortable { cursor: pointer; user-select: none; }
.data-table th.sortable:hover { background: #f1f5f9; color: #3b82f6; }
.sort-icon { margin-left: 4px; color: #3b82f6; }
.data-table td {
  padding: 13px 16px;
  font-size: 14px;
  color: #374151;
  border-bottom: 1px solid #f3f4f6;
}
.table-row:hover td { background: #fafbff; }
.table-row:last-child td { border-bottom: none; }

.id-badge {
  background: #eff6ff;
  color: #3b82f6;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
.employee-name { display: flex; align-items: center; gap: 10px; }
.avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.gender-badge { font-size: 12px; padding: 3px 8px; border-radius: 12px; font-weight: 500; }
.gender-badge.male { background: #dbeafe; color: #1d4ed8; }
.gender-badge.female { background: #fce7f3; color: #be185d; }

.status-select {
  padding: 5px 10px;
  border: none;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  text-align: center;
}
.status-select.status-ACTIVE { background: #d1fae5; color: #065f46; }
.status-select.status-INACTIVE { background: #fee2e2; color: #991b1b; }
.status-select.status-ON_LEAVE { background: #fef3c7; color: #92400e; }
/* ✅ Thêm style cho TERMINATED */
.status-select.status-TERMINATED { background: #f3f4f6; color: #374151; }

.actions { display: flex; gap: 6px; }
.btn-edit, .btn-delete {
  padding: 6px 9px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s;
}
.btn-edit { background: #eff6ff; }
.btn-edit:hover { background: #dbeafe; transform: scale(1.1); }
.btn-delete { background: #fff5f5; }
.btn-delete:hover { background: #fee2e2; transform: scale(1.1); }
.btn-retry {
  margin-top: 12px;
  padding: 6px 16px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}
.btn-retry:hover { background: #2563eb; }

.empty-row { text-align: center; padding: 40px; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 8px; color: #9ca3af; }
.empty-icon { font-size: 40px; }
.loading-row { text-align: center; padding: 40px; display: flex; align-items: center; justify-content: center; gap: 12px; color: #9ca3af; }
.loading-spinner {
  width: 20px; height: 20px;
  border: 2px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 4px;
}
.page-total { font-size: 13px; color: #6b7280; }
.page-controls { display: flex; align-items: center; gap: 6px; }
.page-controls button {
  padding: 6px 12px;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s;
  color: #374151;
}
.page-controls button:hover:not(:disabled) {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #3b82f6;
}
.page-controls button:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { padding: 6px 14px; font-size: 13px; color: #374151; font-weight: 500; }

/* ✅ Toast dùng class động */
.toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  padding: 12px 20px;
  border-radius: 8px;
  background: #10b981;
  color: white;
  font-size: 14px;
  font-weight: 500;
  z-index: 9999;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.3s ease;
  pointer-events: none;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.toast.toast-error {
  background: #ef4444;
}
.toast.show {
  opacity: 1;
  transform: translateY(0);
}
</style>