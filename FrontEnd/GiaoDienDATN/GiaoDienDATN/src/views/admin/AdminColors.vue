<template>
  <div class="admin-colors">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>Quản lý màu sắc</h2>
        <p class="subtitle">Quản lý các màu sắc sản phẩm và mã HEX tương ứng</p>
      </div>
      <button class="btn-add" @click="openCreateModal">
        <span class="btn-icon">＋</span> Thêm màu mới
      </button>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <div class="stat-card stat-total">
        <div class="stat-icon">🎨</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalColors }}</div>
          <div class="stat-label">Tổng số màu</div>
        </div>
      </div>
      <div class="stat-card stat-active">
        <div class="stat-icon">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ activeCount }}</div>
          <div class="stat-label">Đang hoạt động</div>
        </div>
      </div>
      <div class="stat-card stat-inactive">
        <div class="stat-icon">⏸️</div>
        <div class="stat-info">
          <div class="stat-value">{{ inactiveCount }}</div>
          <div class="stat-label">Tạm dừng</div>
        </div>
      </div>
      <div class="stat-card stat-deleted">
        <div class="stat-icon">🗑️</div>
        <div class="stat-info">
          <div class="stat-value">{{ deletedCount }}</div>
          <div class="stat-label">Đã xóa</div>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchKeyword"
          @keyup.enter="searchColors"
          placeholder="Tìm kiếm theo mã hoặc tên màu..."
          autocomplete="off"
        />
        <button v-if="searchKeyword" class="clear-search" @click="searchKeyword = ''; searchColors()">✕</button>
      </div>
      <select v-model="statusFilter" @change="filterColors" class="filter-select">
        <option value="all">Tất cả trạng thái</option>
        <option value="ACTIVE">Đang hoạt động</option>
        <option value="INACTIVE">Tạm dừng</option>
        <option value="DELETED">Đã xóa</option>
      </select>
      <select v-model="sortBy" @change="filterColors" class="filter-select">
        <option value="stt_asc">STT tăng dần</option>
        <option value="stt_desc">STT giảm dần</option>
        <option value="newest">Mới nhất</option>
        <option value="oldest">Cũ nhất</option>
      </select>
      <button class="btn-search" @click="searchColors">Tìm kiếm</button>
      <button v-if="searchKeyword || statusFilter !== 'all' || sortBy !== 'stt_asc'" class="btn-reset" @click="resetFilters">
        <span class="btn-icon">↺</span> Reset
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>Đang tải dữ liệu...</span>
    </div>

    <!-- Table -->
    <div v-else class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>STT</th>
            <th>Màu</th>
            <th>Mã màu</th>
            <th>Tên màu</th>
            <th>Mã HEX</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(color, index) in paginatedColors" :key="color.colorId" class="table-row" :class="{ 'row-deleted': color._deletedAt }">
            <td class="stt-cell">{{ getSTT(index) }}</td>
            <td>
              <div class="color-display" :style="{ backgroundColor: color.hexCode || '#ccc' }">
                <span class="color-tooltip">{{ color.hexCode || '?' }}</span>
              </div>
            </td>
            <td>
              <span class="color-code-badge">{{ color.colorCode }}</span>
            </td>
            <td>
              <strong :class="{ 'deleted-text': color._deletedAt }">{{ color.colorName }}</strong>
            </td>
            <td>
              <span class="hex-code" :style="{ color: color.hexCode }">{{ color.hexCode || '—' }}</span>
            </td>
            <td>
              <span v-if="color._deletedAt" class="status-badge status-deleted">
                Đã xóa
              </span>
              <span v-else :class="['status-badge', color.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
                {{ color.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng' }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <template v-if="color._deletedAt">
                  <span class="readonly-label">Đã xóa</span>
                  <button class="action-btn restore-btn" @click="confirmRestore(color)" title="Khôi phục">
                    ↩️
                  </button>
                </template>
                <template v-else>
                  <button class="action-btn edit-btn" @click="openEditModal(color)" title="Sửa">
                    ✏️
                  </button>
                  <button class="action-btn toggle-btn" @click="toggleColorStatus(color)" :title="color.status === 'ACTIVE' ? 'Tạm dừng' : 'Kích hoạt'">
                    {{ color.status === 'ACTIVE' ? '⏸️' : '▶️' }}
                  </button>
                  <button class="action-btn delete-btn" @click="confirmDelete(color)" title="Xóa">
                    🗑️
                  </button>
                </template>
              </div>
            </td>
          </tr>
          <tr v-if="paginatedColors.length === 0">
            <td colspan="7" class="empty-row">
              <div class="empty-state">
                <div class="empty-icon">📭</div>
                <p>Không có dữ liệu màu sắc</p>
                <button class="btn-retry" @click="loadColors">Tải lại</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="pagination" v-if="totalPages > 0">
        <span class="page-total">Tổng: {{ totalColors }} màu</span>
        <div class="page-controls">
          <button @click="goToPage(0)" :disabled="currentPage === 0">«</button>
          <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 0">‹</button>
          <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button @click="goToPage(currentPage + 1)" :disabled="currentPage + 1 >= totalPages">›</button>
          <button @click="goToPage(totalPages - 1)" :disabled="currentPage + 1 >= totalPages">»</button>
        </div>
      </div>
    </div>

    <!-- Modal Thêm/Sửa -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3>{{ isEdit ? 'Sửa màu' : 'Thêm màu mới' }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>

        <form @submit.prevent="saveColor">
          <div class="modal-body">
            <div class="form-group">
              <label>Mã màu (Code) *</label>
              <input v-model="formData.colorCode" required placeholder="VD: RED, BLUE, BLACK" />
              <small class="form-help">Mã duy nhất, không trùng lặp</small>
            </div>

            <div class="form-group">
              <label>Tên màu *</label>
              <input v-model="formData.colorName" required placeholder="VD: Đỏ, Xanh dương, Đen" />
            </div>

            <div class="form-group">
              <label>Mã HEX</label>
              <div class="hex-input-group">
                <input 
                  v-model="formData.hexCode" 
                  placeholder="#FF0000"
                  @input="validateHexCode"
                />
                <input 
                  type="color" 
                  v-model="formData.hexCode"
                  class="color-picker"
                  title="Chọn màu trực tiếp"
                />
                <div class="color-preview-small" :style="{ backgroundColor: formData.hexCode || '#fff' }"></div>
              </div>
              <small class="form-help">Click vào ô màu để chọn màu trực quan hoặc nhập mã HEX (VD: #FF0000)</small>
            </div>

            <div class="form-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="formData.status" />
                <span>Kích hoạt</span>
              </label>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="closeModal" :disabled="saving">
              Hủy
            </button>
            <button type="submit" class="btn-save" :disabled="saving">
              <span v-if="saving" class="loading-spinner-small"></span>
              <span v-else>💾</span>
              {{ saving ? 'Đang xử lý...' : (isEdit ? 'Cập nhật' : 'Thêm mới') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete Modal -->
    <div v-if="showDeleteModal" class="modal-overlay">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận xóa</h3>
          <button class="close-btn" @click="closeDeleteModal">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc chắn muốn xóa màu <strong>{{ deleteColorName }}</strong>?</p>
          <p class="warning-text">Sau khi xóa, màu sẽ chuyển sang trạng thái đã xóa và có thể khôi phục lại!</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeDeleteModal">Hủy</button>
          <button class="btn-danger" @click="deleteColor">Xóa</button>
        </div>
      </div>
    </div>

    <!-- Restore Modal -->
    <div v-if="showRestoreModal" class="modal-overlay">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận khôi phục</h3>
          <button class="close-btn" @click="closeRestoreModal">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc muốn khôi phục màu <strong>{{ restoreColorName }}</strong>?</p>
          <p>Sau khi khôi phục, màu sẽ trở lại trạng thái ban đầu và có thể chỉnh sửa.</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeRestoreModal">Hủy</button>
          <button class="btn-save" @click="restoreColor">Khôi phục</button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div :class="['toast', { 'toast-error': toastType === 'error', show: showToast }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script>
import api from "@/api/axios";

export default {
  name: 'AdminColors',
  data() {
    return {
      allColors: [],
      filteredColors: [],
      loading: false,
      saving: false,
      currentPage: 0,
      pageSize: 5,
      totalPages: 0,
      totalColors: 0,
      searchKeyword: '',
      statusFilter: 'all',
      sortBy: 'stt_asc',
      showModal: false,
      showDeleteModal: false,
      showRestoreModal: false,
      isEdit: false,
      selectedColor: null,
      deleteColorId: null,
      deleteColorName: '',
      restoreColorId: null,
      restoreColorName: '',
      formData: {
        colorCode: '',
        colorName: '',
        hexCode: '#000000',
        status: true
      },
      showToast: false,
      toastMessage: '',
      toastType: 'success',
      toastTimer: null
    };
  },
  computed: {
    activeCount() {
      return this.filteredColors.filter(c => !c._deletedAt && c.status === 'ACTIVE').length;
    },
    inactiveCount() {
      return this.filteredColors.filter(c => !c._deletedAt && c.status === 'INACTIVE').length;
    },
    deletedCount() {
      return this.filteredColors.filter(c => c._deletedAt).length;
    },
    totalFilteredColors() {
      return this.filteredColors.length;
    },
    paginatedColors() {
      const sorted = this.sortColors(this.filteredColors, this.sortBy);
      const start = this.currentPage * this.pageSize;
      const end = start + this.pageSize;
      return sorted.slice(start, end);
    }
  },
  methods: {
    showNotification(message, type = 'success') {
      if (this.toastTimer) clearTimeout(this.toastTimer);
      this.toastMessage = message;
      this.toastType = type;
      this.showToast = true;
      this.toastTimer = setTimeout(() => {
        this.showToast = false;
      }, 3000);
    },

    getSTT(index) {
      const currentIndex = (this.currentPage * this.pageSize) + index + 1;
      if (this.sortBy === 'stt_desc') {
        return this.totalFilteredColors - currentIndex + 1;
      }
      return currentIndex;
    },

    sortColors(colors, sortType) {
      const sorted = [...colors];
      switch (sortType) {
        case 'stt_asc':
          return sorted.sort((a, b) => (a.colorId || 0) - (b.colorId || 0));
        case 'stt_desc':
          return sorted.sort((a, b) => (b.colorId || 0) - (a.colorId || 0));
        case 'newest':
          return sorted.sort((a, b) => {
            const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
            const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
            return dateB - dateA;
          });
        case 'oldest':
          return sorted.sort((a, b) => {
            const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
            const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
            return dateA - dateB;
          });
        default:
          return sorted;
      }
    },

    validateHexCode() {
      let hex = this.formData.hexCode;
      if (hex && !hex.startsWith('#')) {
        this.formData.hexCode = '#' + hex;
      }
    },

    async loadColors() {
      this.loading = true;
      try {
        const response = await api.get(`/colors`, {
          params: { page: 0, size: 100 }
        });
        
        let colors = [];
        if (response.data?.data?.content) {
          colors = response.data.data.content;
        } else if (response.data?.content) {
          colors = response.data.content;
        } else if (Array.isArray(response.data)) {
          colors = response.data;
        }
        
        this.allColors = colors.map(color => ({
          ...color,
          _deletedAt: color.status === 'DELETED' ? new Date().toISOString() : null
        }));
        
        this.filterColors();
      } catch (error) {
        console.error('Lỗi tải màu:', error);
        this.showNotification('Không thể tải danh sách màu', 'error');
        this.allColors = [];
        this.filteredColors = [];
      } finally {
        this.loading = false;
      }
    },

    filterColors() {
      let filtered = [...this.allColors];
      
      if (this.searchKeyword && this.searchKeyword.trim()) {
        const keyword = this.searchKeyword.toLowerCase().trim();
        filtered = filtered.filter(color => 
          color.colorCode?.toLowerCase().includes(keyword) || 
          color.colorName?.toLowerCase().includes(keyword)
        );
      }
      
      if (this.statusFilter !== 'all') {
        if (this.statusFilter === 'DELETED') {
          filtered = filtered.filter(color => color._deletedAt !== null);
        } else {
          filtered = filtered.filter(color => !color._deletedAt && color.status === this.statusFilter);
        }
      }
      
      this.filteredColors = filtered;
      this.currentPage = 0;
      this.totalPages = Math.ceil(filtered.length / this.pageSize);
      this.totalColors = filtered.length;
    },

    searchColors() {
      this.filterColors();
    },

    resetFilters() {
      this.searchKeyword = '';
      this.statusFilter = 'all';
      this.sortBy = 'stt_asc';
      this.filterColors();
    },

    goToPage(page) {
      if (page >= 0 && page < this.totalPages) {
        this.currentPage = page;
      }
    },

    openCreateModal() {
      this.isEdit = false;
      this.selectedColor = null;
      this.formData = {
        colorCode: '',
        colorName: '',
        hexCode: '#000000',
        status: true
      };
      this.showModal = true;
    },

    openEditModal(color) {
      if (color._deletedAt) {
        this.showNotification('Không thể sửa màu đã xóa! Vui lòng khôi phục trước.', 'error');
        return;
      }
      
      this.isEdit = true;
      this.selectedColor = color;
      this.formData = {
        colorCode: color.colorCode,
        colorName: color.colorName,
        hexCode: color.hexCode || '#000000',
        status: color.status === 'ACTIVE'
      };
      this.showModal = true;
    },

    closeModal() {
      this.showModal = false;
      this.selectedColor = null;
    },

    async saveColor() {
      const code = this.formData.colorCode ? this.formData.colorCode.trim().toUpperCase() : "";
      const name = this.formData.colorName ? this.formData.colorName.trim() : "";
      const hex = this.formData.hexCode ? this.formData.hexCode.trim() : "";

      if (!code) {
        this.showNotification('Mã màu không được để trống hoặc chỉ chứa khoảng trắng', 'error');
        return;
      }
      if (code.length > 30) {
        this.showNotification('Mã màu không được vượt quá 30 ký tự', 'error');
        return;
      }

      if (!name) {
        this.showNotification('Tên màu không được để trống hoặc chỉ chứa khoảng trắng', 'error');
        return;
      }
      if (name.length > 50) {
        this.showNotification('Tên màu không được vượt quá 50 ký tự', 'error');
        return;
      }

      if (hex) {
        const hexRegex = /^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6})$/;
        if (!hexRegex.test(hex)) {
          this.showNotification('Mã HEX không hợp lệ (Ví dụ hợp lệ: #FF0000 hoặc #FFF)', 'error');
          return;
        }
      }

      if (!(await window.appConfirm(this.isEdit ? 'Bạn có chắc chắn muốn lưu các thay đổi này không?' : 'Bạn có chắc chắn muốn thêm dữ liệu này không?'))) return;
      this.saving = true;
      try {
        const submitData = {
          colorCode: code,
          colorName: name,
          hexCode: hex,
          status: this.formData.status ? 'ACTIVE' : 'INACTIVE'
        };

        if (this.isEdit) {
          await api.put(`/colors/${this.selectedColor.colorId}`, submitData);
          this.showNotification('Cập nhật màu thành công!', 'success');
        } else {
          await api.post(`/colors`, submitData);
          this.showNotification('Thêm màu mới thành công!', 'success');
        }
        
        this.closeModal();
        await this.loadColors();
      } catch (error) {
        console.error('Lỗi lưu màu:', error);
        const errorMsg = error.response?.data?.message || error.response?.data?.error || 'Có lỗi xảy ra';
        this.showNotification(errorMsg, 'error');
      } finally {
        this.saving = false;
      }
    },

    async toggleColorStatus(color) {
      if (color._deletedAt) {
        this.showNotification('Không thể thay đổi trạng thái màu đã xóa!', 'error');
        return;
      }
      
      try {
      const newStatus = color.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        if (!(await window.appConfirm('Bạn có chắc chắn muốn chuyển sang trạng thái này không?'))) return;
        const submitData = {
          colorCode: color.colorCode,
          colorName: color.colorName,
          hexCode: color.hexCode,
          status: newStatus
        };
        await api.put(`/colors/${color.colorId}`, submitData);
        
        const index = this.allColors.findIndex(c => c.colorId === color.colorId);
        if (index !== -1) {
          this.allColors[index].status = newStatus;
          this.allColors[index]._deletedAt = null;
        }
        
        this.filterColors();
        this.showNotification(`Đã ${newStatus === 'ACTIVE' ? 'kích hoạt' : 'tạm dừng'} màu!`, 'success');
      } catch (error) {
        console.error('Lỗi cập nhật trạng thái:', error);
        this.showNotification('Không thể cập nhật trạng thái!', 'error');
      }
    },

    confirmDelete(color) {
      this.deleteColorId = color.colorId;
      this.deleteColorName = color.colorName;
      this.showDeleteModal = true;
    },

    closeDeleteModal() {
      this.showDeleteModal = false;
      this.deleteColorId = null;
      this.deleteColorName = '';
    },

    async deleteColor() {
      try {
        await api.delete(`/colors/${this.deleteColorId}`);
        
        const index = this.allColors.findIndex(c => c.colorId === this.deleteColorId);
        if (index !== -1) {
          this.allColors[index] = {
            ...this.allColors[index],
            _deletedAt: new Date().toISOString(),
            status: 'DELETED'
          };
        }
        
        this.filterColors();
        this.showNotification('Xóa màu thành công! Màu đã chuyển sang trạng thái đã xóa.', 'success');
        this.closeDeleteModal();
      } catch (error) {
        console.error('Lỗi xóa màu:', error);
        this.showNotification('Không thể xóa màu này!', 'error');
      }
    },

    confirmRestore(color) {
      this.restoreColorId = color.colorId;
      this.restoreColorName = color.colorName;
      this.showRestoreModal = true;
    },

    closeRestoreModal() {
      this.showRestoreModal = false;
      this.restoreColorId = null;
      this.restoreColorName = '';
    },

    async restoreColor() {
      try {
        await api.patch(`/colors/${this.restoreColorId}/restore`);
        
        const index = this.allColors.findIndex(c => c.colorId === this.restoreColorId);
        if (index !== -1) {
          this.allColors[index] = {
            ...this.allColors[index],
            _deletedAt: null,
            status: 'ACTIVE'
          };
        }
        
        this.filterColors();
        this.showNotification('Khôi phục màu thành công!', 'success');
        this.closeRestoreModal();
      } catch (error) {
        console.error('Lỗi khôi phục màu:', error);
        this.showNotification('Không thể khôi phục màu này!', 'error');
      }
    }
  },
  mounted() {
    this.loadColors();
  }
};
</script>

<style scoped>
.admin-colors {
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

/* ===== STATS ===== */
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
.stat-inactive { border-left-color: #f59e0b; }
.stat-deleted { border-left-color: #ef4444; }

.stat-icon { font-size: 32px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a2e; }
.stat-label { font-size: 12px; color: #6b7280; margin-top: 4px; }

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

.btn-reset {
  padding: 10px 20px;
  background-color: #f1f2f6;
  color: #2c3e50;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.btn-reset:hover {
  background-color: #e0e0e0;
}

/* ===== TABLE ===== */
.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
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

.row-deleted .deleted-text {
  text-decoration: line-through;
  color: #9ca3af;
}

.stt-cell {
  font-weight: 600;
  color: #3498db;
  width: 60px;
}

/* ===== COLOR DISPLAY ===== */
.color-display {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  position: relative;
  cursor: pointer;
  border: 2px solid #ddd;
  transition: transform 0.2s;
}

.color-display:hover {
  transform: scale(1.1);
}

.color-display:hover .color-tooltip {
  visibility: visible;
  opacity: 1;
}

.color-tooltip {
  visibility: hidden;
  opacity: 0;
  position: absolute;
  bottom: -30px;
  left: 50%;
  transform: translateX(-50%);
  background: #2c3e50;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  white-space: nowrap;
  transition: all 0.3s;
  z-index: 10;
}

.color-code-badge {
  background: #f0f0f0;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  color: #667eea;
}

.hex-code {
  font-family: monospace;
  font-size: 13px;
  font-weight: 500;
}

/* ===== STATUS BADGES ===== */
.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.status-active { background-color: #e8f8f5; color: #27ae60; }
.status-inactive { background-color: #fdf2e9; color: #f39c12; }
.status-deleted { background-color: #f3f4f6; color: #6b7280; }

/* ===== ACTION BUTTONS ===== */
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

.edit-btn:hover { background-color: #fef3c7; }
.toggle-btn:hover { background-color: #e8f4f8; }
.delete-btn:hover { background-color: #fde8e8; }

.restore-btn {
  background: #10b981;
  color: white;
  border-radius: 6px;
  padding: 6px 10px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
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

/* ===== PAGINATION ===== */
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

.loading-spinner-small {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid white;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  margin-right: 6px;
}

/* ===== EMPTY STATE ===== */
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
  width: 500px;
  max-width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideIn 0.3s;
}

.modal-container.modal-sm {
  width: 400px;
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
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 12px 12px 0 0;
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
}

/* ===== FORM ===== */
.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #2c3e50;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.hex-input-group {
  display: flex;
  gap: 10px;
  align-items: center;
}

.hex-input-group input[type="text"] {
  flex: 1;
}

.color-picker {
  width: 50px;
  height: 40px;
  padding: 2px;
  cursor: pointer;
  border: 1px solid #ddd;
  border-radius: 6px;
}

.color-preview-small {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  border: 1px solid #ddd;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-label input {
  width: auto;
}

.form-help {
  font-size: 11px;
  color: #7f8c8d;
  margin-top: 4px;
  display: block;
}

.required {
  color: #e74c3c;
}

.warning-text {
  color: #dc3545;
  margin-top: 10px;
  font-size: 13px;
}

/* ===== BUTTONS ===== */
.btn-cancel, .btn-save, .btn-danger {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-cancel {
  background: #95a5a6;
  color: white;
}

.btn-cancel:hover {
  background: #7f8c8d;
}

.btn-save {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
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
  .admin-colors { padding: 15px; }
  .page-header { flex-direction: column; align-items: stretch; gap: 12px; }
  .btn-add { justify-content: center; }
  .stats-row { grid-template-columns: 1fr 1fr; gap: 12px; }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .search-box { min-width: auto; }
  .btn-search, .btn-reset { justify-content: center; }
  .table-container { overflow-x: auto; }
  .data-table { min-width: 700px; }
  .pagination { flex-direction: column; gap: 12px; }
}
</style>
