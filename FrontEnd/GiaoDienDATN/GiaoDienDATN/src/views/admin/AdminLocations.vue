<template>
  <div class="admin-locations">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>📍 Quản lý địa chỉ</h2>
        <p class="subtitle">Quản lý tỉnh/thành phố và phường/xã theo thông tư mới</p>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tabs-container">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'province' }"
        @click="activeTab = 'province'; fetchProvinces()"
      >
        <i class="fas fa-city"></i>
        Tỉnh/Thành phố
        <span class="tab-badge">{{ provinces.length }}</span>
      </button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'ward' }"
        @click="activeTab = 'ward'; fetchWards()"
      >
        <i class="fas fa-map-pin"></i>
        Phường/Xã
        <span class="tab-badge">{{ wards.length }}</span>
      </button>
    </div>

    <!-- ============================================= -->
    <!-- TAB 1: TỈNH/THÀNH PHỐ -->
    <!-- ============================================= -->
    <div v-show="activeTab === 'province'" class="tab-content">
      <!-- Filter Bar -->
      <div class="filter-bar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            v-model="provinceSearch"
            @keyup.enter="searchProvinces"
            placeholder="Tìm kiếm tỉnh/thành phố..."
            autocomplete="off"
          />
          <button v-if="provinceSearch" class="clear-search" @click="provinceSearch = ''; fetchProvinces()">✕</button>
        </div>
        <select v-model="provinceStatusFilter" @change="fetchProvinces" class="filter-select">
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="INACTIVE">Tạm dừng</option>
          <option value="DELETED">Đã xóa</option>
        </select>
        <button class="btn-search" @click="searchProvinces">Tìm kiếm</button>
        <button class="btn-add" @click="openAddProvince">
          <span class="btn-icon">＋</span> Thêm tỉnh/thành phố
        </button>
      </div>

      <!-- Loading -->
      <div v-if="provinceLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>Đang tải dữ liệu...</span>
      </div>

      <!-- Table -->
      <div v-else class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên tỉnh/thành phố</th>
              <th>Mã</th>
              <th>Loại</th>
              <th>Số phường/xã</th>
              <th>Thứ tự</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="province in paginatedProvinces" :key="province.provinceId" class="table-row" :class="{ 'row-deleted': province.status === 'DELETED' }">
              <td class="id-cell">#{{ province.provinceId }}</td>
              <td>
                <span class="name-text" :class="{ 'deleted-text': province.status === 'DELETED' }">
                  {{ province.provinceName }}
                </span>
              </td>
              <td><span class="code-badge">{{ province.provinceCode }}</span></td>
              <td>{{ province.provinceType || '—' }}</td>
              <td class="text-center">{{ province.wards?.length || 0 }}</td>
              <td class="text-center">{{ province.displayOrder || 0 }}</td>
              <td>
                <span v-if="province.status === 'DELETED'" class="status-badge status-deleted">
                  Đã xóa
                </span>
                <span v-else :class="['status-badge', province.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
                  {{ province.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng' }}
                </span>
              </td>
              <td class="actions">
                <div class="action-buttons">
                  <template v-if="province.status !== 'DELETED'">
                    <button class="action-btn edit-btn" @click="openEditProvince(province)" title="Sửa">
                      ✏️
                    </button>
                    <button class="action-btn delete-btn" @click="confirmDeleteProvince(province)" title="Xóa">
                      🗑️
                    </button>
                  </template>
                  <template v-else>
                    <span class="readonly-label">Đã xóa</span>
                    <button class="action-btn restore-btn" @click="confirmRestoreProvince(province)" title="Khôi phục">
                      ↩️
                    </button>
                  </template>
                </div>
              </td>
            </tr>
            <tr v-if="paginatedProvinces.length === 0">
              <td colspan="8" class="empty-row">
                <div class="empty-state">
                  <div class="empty-icon">📭</div>
                  <p>Không có dữ liệu tỉnh/thành phố</p>
                  <button class="btn-retry" @click="fetchProvinces">Tải lại</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div class="pagination" v-if="provinceTotalPages > 1">
          <span class="page-total">Tổng: {{ provinces.length }} tỉnh/thành phố</span>
          <div class="page-controls">
            <button @click="provincePage = 0" :disabled="provincePage === 0">«</button>
            <button @click="provincePage--" :disabled="provincePage === 0">‹</button>
            <span class="page-info">{{ provincePage + 1 }} / {{ provinceTotalPages }}</span>
            <button @click="provincePage++" :disabled="provincePage >= provinceTotalPages - 1">›</button>
            <button @click="provincePage = provinceTotalPages - 1" :disabled="provincePage >= provinceTotalPages - 1">»</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ============================================= -->
    <!-- TAB 2: PHƯỜNG/XÃ -->
    <!-- ============================================= -->
    <div v-show="activeTab === 'ward'" class="tab-content">
      <!-- Filter Bar -->
      <div class="filter-bar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            v-model="wardSearch"
            @keyup.enter="searchWards"
            placeholder="Tìm kiếm phường/xã..."
            autocomplete="off"
          />
          <button v-if="wardSearch" class="clear-search" @click="wardSearch = ''; fetchWards()">✕</button>
        </div>
        <select v-model="wardProvinceFilter" @change="fetchWards" class="filter-select">
          <option :value="null">Tất cả tỉnh/thành phố</option>
          <option 
            v-for="province in provinces" 
            :key="province.provinceId" 
            :value="province.provinceId"
          >
            {{ province.provinceName }}
          </option>
        </select>
        <select v-model="wardStatusFilter" @change="fetchWards" class="filter-select">
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="INACTIVE">Tạm dừng</option>
          <option value="DELETED">Đã xóa</option>
        </select>
        <button class="btn-search" @click="searchWards">Tìm kiếm</button>
        <button class="btn-add" @click="openAddWard">
          <span class="btn-icon">＋</span> Thêm phường/xã
        </button>
      </div>

      <!-- Loading -->
      <div v-if="wardLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>Đang tải dữ liệu...</span>
      </div>

      <!-- Table -->
      <div v-else class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên phường/xã</th>
              <th>Mã</th>
              <th>Loại</th>
              <th>Thuộc tỉnh</th>
              <th>Thứ tự</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ward in paginatedWards" :key="ward.wardId" class="table-row" :class="{ 'row-deleted': ward.status === 'DELETED' }">
              <td class="id-cell">#{{ ward.wardId }}</td>
              <td>
                <span class="name-text" :class="{ 'deleted-text': ward.status === 'DELETED' }">
                  {{ ward.wardName }}
                </span>
              </td>
              <td><span class="code-badge">{{ ward.wardCode }}</span></td>
              <td>{{ ward.wardType || '—' }}</td>
              <td>
                <span class="province-tag">{{ ward.provinceName || '—' }}</span>
              </td>
              <td class="text-center">{{ ward.displayOrder || 0 }}</td>
              <td>
                <span v-if="ward.status === 'DELETED'" class="status-badge status-deleted">
                  Đã xóa
                </span>
                <span v-else :class="['status-badge', ward.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
                  {{ ward.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng' }}
                </span>
              </td>
              <td class="actions">
                <div class="action-buttons">
                  <template v-if="ward.status !== 'DELETED'">
                    <button class="action-btn edit-btn" @click="openEditWard(ward)" title="Sửa">
                      ✏️
                    </button>
                    <button class="action-btn delete-btn" @click="confirmDeleteWard(ward)" title="Xóa">
                      🗑️
                    </button>
                  </template>
                  <template v-else>
                    <span class="readonly-label">Đã xóa</span>
                    <button class="action-btn restore-btn" @click="confirmRestoreWard(ward)" title="Khôi phục">
                      ↩️
                    </button>
                  </template>
                </div>
              </td>
            </tr>
            <tr v-if="paginatedWards.length === 0">
              <td colspan="8" class="empty-row">
                <div class="empty-state">
                  <div class="empty-icon">📭</div>
                  <p>Không có dữ liệu phường/xã</p>
                  <button class="btn-retry" @click="fetchWards">Tải lại</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div class="pagination" v-if="wardTotalPages > 1">
          <span class="page-total">Tổng: {{ wards.length }} phường/xã</span>
          <div class="page-controls">
            <button @click="wardPage = 0" :disabled="wardPage === 0">«</button>
            <button @click="wardPage--" :disabled="wardPage === 0">‹</button>
            <span class="page-info">{{ wardPage + 1 }} / {{ wardTotalPages }}</span>
            <button @click="wardPage++" :disabled="wardPage >= wardTotalPages - 1">›</button>
            <button @click="wardPage = wardTotalPages - 1" :disabled="wardPage >= wardTotalPages - 1">»</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ============================================= -->
    <!-- MODALS -->
    <!-- ============================================= -->

    <!-- Province Modal -->
    <ProvinceModal
      :visible="showProvinceModal"
      :is-edit="isEditProvince"
      :initial-data="editingProvince"
      @close="closeProvinceModal"
      @submit="handleProvinceSubmit"
    />

    <!-- Ward Modal -->
    <WardModal
      :visible="showWardModal"
      :is-edit="isEditWard"
      :initial-data="editingWard"
      :provinces="provinces"
      @close="closeWardModal"
      @submit="handleWardSubmit"
    />

    <!-- Confirm Dialog -->
    <div v-if="showConfirmDialog" class="modal-overlay" @click.self="closeConfirmDialog">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>{{ confirmTitle }}</h3>
          <button class="close-btn" @click="closeConfirmDialog">✕</button>
        </div>
        <div class="modal-body">
          <p>{{ confirmMessage }}</p>
          <p v-if="confirmWarning" class="warning-text">{{ confirmWarning }}</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeConfirmDialog">Hủy</button>
          <button class="btn-danger" @click="handleConfirmAction" :disabled="confirmLoading">
            {{ confirmLoading ? 'Đang xử lý...' : confirmButtonText }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div :class="['toast', { 'toast-error': toastType === 'error', show: toastShow }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { provinceService, wardService } from '../../services/locationService'
import ProvinceModal from '../../components/admin/ProvinceModal.vue'
import WardModal from '../../components/admin/WardModal.vue'

export default {
  name: 'AdminLocations',
  components: {
    ProvinceModal,
    WardModal
  },
  data() {
    return {
      // ===== TABS =====
      activeTab: 'province',

      // ===== PROVINCE STATE =====
      provinces: [],
      provinceLoading: false,
      provinceSearch: '',
      provinceStatusFilter: '',
      provincePage: 0,
      provincePageSize: 10,

      // ===== WARD STATE =====
      wards: [],
      wardLoading: false,
      wardSearch: '',
      wardProvinceFilter: null,
      wardStatusFilter: '',
      wardPage: 0,
      wardPageSize: 10,

      // ===== MODAL STATE =====
      showProvinceModal: false,
      showWardModal: false,
      showConfirmDialog: false,
      isEditProvince: false,
      isEditWard: false,
      editingProvince: null,
      editingWard: null,

      // ===== CONFIRM STATE =====
      confirmAction: '',
      confirmLoading: false,
      confirmTitle: '',
      confirmMessage: '',
      confirmWarning: '',
      confirmButtonText: '',
      confirmData: null,

      // ===== TOAST =====
      toastShow: false,
      toastMessage: '',
      toastType: 'success',
      toastTimer: null
    }
  },
  computed: {
    // ===== PROVINCE PAGINATION =====
    provinceTotalPages() {
      return Math.ceil(this.provinces.length / this.provincePageSize)
    },
    paginatedProvinces() {
      const start = this.provincePage * this.provincePageSize
      return this.provinces.slice(start, start + this.provincePageSize)
    },

    // ===== WARD PAGINATION =====
    wardTotalPages() {
      return Math.ceil(this.wards.length / this.wardPageSize)
    },
    paginatedWards() {
      const start = this.wardPage * this.wardPageSize
      return this.wards.slice(start, start + this.wardPageSize)
    }
  },
  mounted() {
    this.fetchProvinces()
  },
  methods: {
    // =============================================
    // TOAST
    // =============================================
    showToast(message, type = 'success') {
      if (this.toastTimer) clearTimeout(this.toastTimer)
      this.toastMessage = message
      this.toastType = type
      this.toastShow = true
      this.toastTimer = setTimeout(() => {
        this.toastShow = false
      }, 3000)
    },

    // =============================================
    // PROVINCE API
    // =============================================
    async fetchProvinces() {
      this.provinceLoading = true
      try {
        const response = await provinceService.getAll()
        let data = response.data || response || []
        // Filter theo status
        if (this.provinceStatusFilter) {
          data = data.filter(p => p.status === this.provinceStatusFilter)
        }
        this.provinces = data
        this.provincePage = 0
      } catch (error) {
        console.error('Error fetching provinces:', error)
        this.showToast('Không thể tải danh sách tỉnh/thành phố', 'error')
        this.provinces = []
      } finally {
        this.provinceLoading = false
      }
    },

    async searchProvinces() {
      if (!this.provinceSearch.trim()) {
        this.fetchProvinces()
        return
      }
      this.provinceLoading = true
      try {
        const response = await provinceService.search(this.provinceSearch)
        this.provinces = response.data || response || []
        this.provincePage = 0
      } catch (error) {
        console.error('Error searching provinces:', error)
        this.showToast('Không thể tìm kiếm', 'error')
      } finally {
        this.provinceLoading = false
      }
    },

    // =============================================
    // WARD API
    // =============================================
    async fetchWards() {
      this.wardLoading = true
      try {
        let data = []
        if (this.wardProvinceFilter) {
          const response = await wardService.getByProvince(this.wardProvinceFilter)
          data = response.data || response || []
        } else {
          // Nếu không chọn tỉnh, lấy tất cả từ API
          const response = await axios.get('http://localhost:8080/api/admin/dia-chi/wards', {
            headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
          })
          data = response.data || response || []
        }
        // Filter theo status
        if (this.wardStatusFilter) {
          data = data.filter(w => w.status === this.wardStatusFilter)
        }
        // Filter theo search
        if (this.wardSearch.trim()) {
          const kw = this.wardSearch.toLowerCase()
          data = data.filter(w => 
            w.wardName?.toLowerCase().includes(kw) ||
            w.wardCode?.toLowerCase().includes(kw)
          )
        }
        this.wards = data
        this.wardPage = 0
      } catch (error) {
        console.error('Error fetching wards:', error)
        this.showToast('Không thể tải danh sách phường/xã', 'error')
        this.wards = []
      } finally {
        this.wardLoading = false
      }
    },

    async searchWards() {
      if (!this.wardSearch.trim()) {
        this.fetchWards()
        return
      }
      this.wardLoading = true
      try {
        const response = await wardService.search(this.wardSearch)
        this.wards = response.data || response || []
        this.wardPage = 0
      } catch (error) {
        console.error('Error searching wards:', error)
        this.showToast('Không thể tìm kiếm', 'error')
      } finally {
        this.wardLoading = false
      }
    },

    // =============================================
    // PROVINCE CRUD
    // =============================================
    openAddProvince() {
      this.isEditProvince = false
      this.editingProvince = null
      this.showProvinceModal = true
    },

    openEditProvince(province) {
      this.isEditProvince = true
      this.editingProvince = { ...province }
      this.showProvinceModal = true
    },

    closeProvinceModal() {
      this.showProvinceModal = false
      this.editingProvince = null
    },

    async handleProvinceSubmit(data) {
      try {
        if (this.isEditProvince) {
          await provinceService.update(this.editingProvince.provinceId, data)
          this.showToast('Cập nhật tỉnh/thành phố thành công!', 'success')
        } else {
          await provinceService.create(data)
          this.showToast('Thêm tỉnh/thành phố thành công!', 'success')
        }
        this.fetchProvinces()
        this.closeProvinceModal()
      } catch (error) {
        console.error('Error saving province:', error)
        this.showToast(error.response?.data?.message || 'Có lỗi xảy ra!', 'error')
        throw error
      }
    },

    confirmDeleteProvince(province) {
      const wardCount = province.wards?.length || 0
      this.confirmAction = 'deleteProvince'
      this.confirmTitle = 'Xác nhận xóa'
      this.confirmMessage = `Bạn có chắc muốn xóa tỉnh/thành phố "${province.provinceName}"?`
      this.confirmWarning = wardCount > 0 
        ? `⚠️ Tỉnh/thành phố này đang có ${wardCount} phường/xã. Sau khi xóa, các phường/xã cũng sẽ bị chuyển sang trạng thái đã xóa!`
        : ''
      this.confirmButtonText = 'Xóa'
      this.confirmData = province
      this.showConfirmDialog = true
    },

    confirmRestoreProvince(province) {
      this.confirmAction = 'restoreProvince'
      this.confirmTitle = 'Xác nhận khôi phục'
      this.confirmMessage = `Bạn có chắc muốn khôi phục tỉnh/thành phố "${province.provinceName}"?`
      this.confirmButtonText = 'Khôi phục'
      this.confirmData = province
      this.showConfirmDialog = true
    },

    // =============================================
    // WARD CRUD
    // =============================================
    openAddWard() {
      this.isEditWard = false
      this.editingWard = null
      this.showWardModal = true
    },

    openEditWard(ward) {
      this.isEditWard = true
      this.editingWard = { ...ward }
      this.showWardModal = true
    },

    closeWardModal() {
      this.showWardModal = false
      this.editingWard = null
    },

    async handleWardSubmit(data) {
      try {
        if (this.isEditWard) {
          await wardService.update(this.editingWard.wardId, data)
          this.showToast('Cập nhật phường/xã thành công!', 'success')
        } else {
          await wardService.create(data)
          this.showToast('Thêm phường/xã thành công!', 'success')
        }
        this.fetchWards()
        // Cập nhật lại danh sách tỉnh để cập nhật số lượng
        this.fetchProvinces()
        this.closeWardModal()
      } catch (error) {
        console.error('Error saving ward:', error)
        this.showToast(error.response?.data?.message || 'Có lỗi xảy ra!', 'error')
        throw error
      }
    },

    confirmDeleteWard(ward) {
      this.confirmAction = 'deleteWard'
      this.confirmTitle = 'Xác nhận xóa'
      this.confirmMessage = `Bạn có chắc muốn xóa phường/xã "${ward.wardName}"?`
      this.confirmWarning = ''
      this.confirmButtonText = 'Xóa'
      this.confirmData = ward
      this.showConfirmDialog = true
    },

    confirmRestoreWard(ward) {
      this.confirmAction = 'restoreWard'
      this.confirmTitle = 'Xác nhận khôi phục'
      this.confirmMessage = `Bạn có chắc muốn khôi phục phường/xã "${ward.wardName}"?`
      this.confirmButtonText = 'Khôi phục'
      this.confirmData = ward
      this.showConfirmDialog = true
    },

    // =============================================
    // CONFIRM DIALOG
    // =============================================
    closeConfirmDialog() {
      this.showConfirmDialog = false
      this.confirmAction = ''
      this.confirmLoading = false
      this.confirmData = null
    },

    async handleConfirmAction() {
      this.confirmLoading = true
      try {
        switch (this.confirmAction) {
          case 'deleteProvince':
            await provinceService.delete(this.confirmData.provinceId)
            this.showToast(`Đã xóa tỉnh/thành phố "${this.confirmData.provinceName}"!`, 'success')
            this.fetchProvinces()
            break

          case 'restoreProvince':
            // Sử dụng API restore (nếu có) hoặc update status
            await provinceService.update(this.confirmData.provinceId, { 
              ...this.confirmData,
              status: 'ACTIVE' 
            })
            this.showToast(`Đã khôi phục tỉnh/thành phố "${this.confirmData.provinceName}"!`, 'success')
            this.fetchProvinces()
            break

          case 'deleteWard':
            await wardService.delete(this.confirmData.wardId)
            this.showToast(`Đã xóa phường/xã "${this.confirmData.wardName}"!`, 'success')
            this.fetchWards()
            this.fetchProvinces()
            break

          case 'restoreWard':
            await wardService.update(this.confirmData.wardId, {
              ...this.confirmData,
              status: 'ACTIVE'
            })
            this.showToast(`Đã khôi phục phường/xã "${this.confirmData.wardName}"!`, 'success')
            this.fetchWards()
            this.fetchProvinces()
            break

          default:
            break
        }
        this.closeConfirmDialog()
      } catch (error) {
        console.error('Error in confirm action:', error)
        this.showToast(error.response?.data?.message || 'Có lỗi xảy ra!', 'error')
        this.confirmLoading = false
      }
    }
  }
}
</script>

<style scoped>
.admin-locations {
  padding: 20px;
  background-color: #f8f9fa;
  min-height: calc(100vh - 60px);
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== HEADER ===== */
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

/* ===== TABS ===== */
.tabs-container {
  display: flex;
  gap: 10px;
  margin-bottom: 24px;
  background: white;
  padding: 10px 15px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.tab-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  font-size: 15px;
  color: #666;
  background: transparent;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-btn:hover {
  background: #f0f0f0;
}

.tab-btn.active {
  background: #e63946;
  color: white;
  box-shadow: 0 4px 12px rgba(230, 57, 70, 0.3);
}

.tab-btn .tab-badge {
  background: rgba(255, 255, 255, 0.2);
  padding: 1px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.tab-btn.active .tab-badge {
  background: rgba(255, 255, 255, 0.2);
}

/* ===== FILTER BAR ===== */
.filter-bar {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  background: white;
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
  align-items: center;
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

.filter-select {
  padding: 10px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  outline: none;
  background-color: white;
  cursor: pointer;
  min-width: 160px;
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

.btn-add {
  padding: 10px 20px;
  background-color: #3498db;
  color: white;
  border: none;
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

.btn-icon {
  font-size: 16px;
  font-weight: 700;
}

/* ===== TABLE ===== */
.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid #f1f4f8;
}

.data-table th {
  background-color: #f8f9fa;
  color: #2c3e50;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
}

.table-row:hover {
  background: #f9fafb;
}

.row-deleted {
  opacity: 0.6;
  background-color: #fafafa;
}

.row-deleted td {
  color: #9ca3af;
}

.row-deleted .name-text {
  color: #9ca3af;
}

.row-deleted .code-badge {
  opacity: 0.5;
}

.id-cell {
  font-weight: 600;
  color: #3498db;
}

.name-text {
  font-weight: 500;
  color: #2c3e50;
}

.deleted-text {
  text-decoration: line-through;
  color: #9ca3af;
}

.code-badge {
  background: #f0f0f0;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  color: #667eea;
}

.province-tag {
  background: #e8f5e9;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: #2e7d32;
}

.text-center {
  text-align: center;
}

/* ===== STATUS BADGE ===== */
.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.status-active {
  background-color: #e8f8f5;
  color: #27ae60;
}

.status-inactive {
  background-color: #fdf2e9;
  color: #f39c12;
}

.status-deleted {
  background-color: #f3f4f6;
  color: #6b7280;
}

/* ===== ACTION BUTTONS ===== */
.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  align-items: center;
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

.edit-btn:hover {
  background-color: #fef3c7;
}

.delete-btn:hover {
  background-color: #fde8e8;
}

.restore-btn {
  background: #10b981;
  color: white;
  border-radius: 6px;
  padding: 6px 10px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.restore-btn:hover {
  background: #059669;
  transform: scale(1.05);
}

.readonly-label {
  font-size: 12px;
  color: #9ca3af;
  font-style: italic;
  padding: 4px 8px;
  background: #f3f4f6;
  border-radius: 6px;
  white-space: nowrap;
}

/* ===== LOADING ===== */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #3498db;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e5e7eb;
  border-top-color: #3498db;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== EMPTY ===== */
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

.btn-retry:hover {
  background: #2980b9;
}

/* ===== PAGINATION ===== */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
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

/* ===== CONFIRM MODAL ===== */
.modal-container.modal-sm {
  width: 400px;
}

.warning-text {
  color: #dc3545;
  margin-top: 10px;
  font-size: 13px;
  background: #fde8e8;
  padding: 8px 12px;
  border-radius: 6px;
}

.btn-danger {
  background: #dc3545;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-danger:hover:not(:disabled) {
  background: #c82333;
}

.btn-danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

/* ===== RESPONSIVE ===== */
@media (max-width: 768px) {
  .admin-locations { padding: 15px; }
  .page-header { flex-direction: column; align-items: stretch; gap: 12px; }
  .btn-add { justify-content: center; }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .search-box { min-width: auto; }
  .btn-search, .btn-add { width: 100%; justify-content: center; }
  .table-container { overflow-x: auto; }
  .data-table { min-width: 700px; }
  .pagination { flex-direction: column; gap: 12px; }
}
</style>