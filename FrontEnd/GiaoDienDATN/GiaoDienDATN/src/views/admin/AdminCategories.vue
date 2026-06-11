<template>
  <div class="admin-categories">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>Quản lý danh mục</h2>
        <p class="subtitle">Quản lý danh mục sản phẩm</p>
      </div>
      <button class="btn-add" @click="openCreateModal">
        <span class="btn-icon">＋</span> Thêm danh mục
      </button>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <div class="stat-card stat-total">
        <div class="stat-icon">📁</div>
        <div class="stat-info">
          <div class="stat-value">{{ totalCategories }}</div>
          <div class="stat-label">Tổng danh mục</div>
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
          @keyup.enter="searchCategories"
          placeholder="Tìm kiếm danh mục..."
          autocomplete="off"
        />
        <button v-if="searchKeyword" class="clear-search" @click="searchKeyword = ''; searchCategories()">✕</button>
      </div>
      <select v-model="statusFilter" @change="filterCategories" class="filter-select">
        <option value="all">Tất cả trạng thái</option>
        <option value="ACTIVE">Đang hoạt động</option>
        <option value="INACTIVE">Tạm dừng</option>
        <option value="DELETED">Đã xóa</option>
      </select>
      <button class="btn-search" @click="searchCategories">Tìm kiếm</button>
      <button v-if="searchKeyword || statusFilter !== 'all'" class="btn-reset" @click="resetFilters">
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
            <th>Tên danh mục</th>
            <th>Mô tả</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in paginatedCategories" :key="item.categoryId" class="table-row" :class="{ 'row-deleted': item._deletedAt }">
            <td class="stt-cell">{{ getSTT(index) }}</td>
            <td>
              <div class="category-name">
                <strong :class="{ 'deleted-text': item._deletedAt }">{{ item.categoryName }}</strong>
              </div>
            </td>
            <td class="description-cell" :class="{ 'deleted-text': item._deletedAt }">
              {{ item.categoryDescription || '—' }}
            </td>
            <td>
              <span v-if="!item._deletedAt" :class="['status-badge', item.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
                {{ item.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng' }}
              </span>
              <span v-else class="status-badge status-deleted">
                Đã xóa
              </span>
            </td>
            <td class="actions">
              <div class="action-buttons">
                <!-- Nếu chưa xóa: hiển thị đầy đủ các nút -->
                <template v-if="!item._deletedAt">
                  <button class="action-btn edit-btn" @click="openEditModal(item)" title="Sửa">
                    ✏️
                  </button>
                  <button class="action-btn toggle-btn" @click="toggleStatus(item)" :title="item.status === 'ACTIVE' ? 'Tạm dừng' : 'Kích hoạt'">
                    {{ item.status === 'ACTIVE' ? '⏸️' : '▶️' }}
                  </button>
                  <button class="action-btn delete-btn" @click="confirmDelete(item)" title="Xóa" :disabled="checkingProducts">
                    <span v-if="checkingProducts && checkingCategoryId === item.categoryId" class="loading-spinner-small"></span>
                    <span v-else>🗑️</span>
                  </button>
                </template>
                <!-- Nếu đã xóa: chỉ hiển thị nút khôi phục -->
                <template v-else>
                  <span class="readonly-label">Đã xóa</span>
                  <button class="action-btn restore-btn" @click="confirmRestore(item)" title="Khôi phục">
                    ↩️
                  </button>
                </template>
              </div>
            </td>
          </tr>
          <tr v-if="paginatedCategories.length === 0">
            <td colspan="5" class="empty-row">
              <div class="empty-state">
                <div class="empty-icon">📭</div>
                <p>Không có dữ liệu danh mục</p>
                <button class="btn-retry" @click="loadCategories">Tải lại</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="pagination" v-if="totalPages > 0">
        <span class="page-total">Tổng: {{ totalCategories }} danh mục</span>
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
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-container">
        <div class="modal-header">
          <h3>{{ isEdit ? 'Sửa danh mục' : 'Thêm danh mục' }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>
        <form @submit.prevent="saveCategory">
          <div class="modal-body">
            <div class="form-group">
              <label>Tên danh mục <span class="required">*</span></label>
              <input v-model="form.categoryName" required placeholder="Nhập tên danh mục" maxlength="20"/>
            </div>
            <div class="form-group">
              <label>Mô tả</label>
              <textarea v-model="form.categoryDescription" rows="3" placeholder="Nhập mô tả" maxlength="20"></textarea>
            </div>
            <div class="form-group">
              <label>Trạng thái</label>
              <select v-model="form.status">
                <option value="ACTIVE">Hoạt động</option>
                <option value="INACTIVE">Tạm dừng</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="closeModal">Hủy</button>
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
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận xóa</h3>
          <button class="close-btn" @click="closeDeleteModal">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc muốn xóa danh mục <strong>{{ deleteCategoryName }}</strong>?</p>
          <p class="warning-text">Sau khi xóa, danh mục sẽ không thể sửa được nữa, chỉ có thể khôi phục!</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeDeleteModal">Hủy</button>
          <button class="btn-danger" @click="deleteCategory">Xóa</button>
        </div>
      </div>
    </div>

    <!-- Restore Modal -->
    <div v-if="showRestoreModal" class="modal-overlay" @click.self="closeRestoreModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận khôi phục</h3>
          <button class="close-btn" @click="closeRestoreModal">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc muốn khôi phục danh mục <strong>{{ restoreCategoryName }}</strong>?</p>
          <p>Sau khi khôi phục, danh mục sẽ trở lại trạng thái ban đầu và có thể chỉnh sửa.</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeRestoreModal">Hủy</button>
          <button class="btn-save" @click="restoreCategory">Khôi phục</button>
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
import categoryService from '@/api/categoryService';
import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

export default {
  name: 'AdminCategories',
  data() {
    return {
      allCategories: [],
      filteredCategories: [],
      loading: false,
      saving: false,
      currentPage: 0,
      pageSize: 5,
      totalPages: 0,
      searchKeyword: '',
      statusFilter: 'all',
      showModal: false,
      showDeleteModal: false,
      showRestoreModal: false,
      isEdit: false,
      deleteCategoryId: null,
      deleteCategoryName: '',
      restoreCategoryId: null,
      restoreCategoryName: '',
      checkingProducts: false,
      checkingCategoryId: null,
      form: {
        categoryId: null,
        categoryName: '',
        categoryDescription: '',
        status: 'ACTIVE'
      },
      showToast: false,
      toastMessage: '',
      toastType: 'success',
      toastTimer: null
    };
  },
  computed: {
    totalCategories() {
      return this.filteredCategories.length;
    },
    activeCount() {
      return this.filteredCategories.filter(c => !c._deletedAt && c.status === 'ACTIVE').length;
    },
    inactiveCount() {
      return this.filteredCategories.filter(c => !c._deletedAt && c.status === 'INACTIVE').length;
    },
    deletedCount() {
      return this.filteredCategories.filter(c => c._deletedAt).length;
    },
    paginatedCategories() {
      const start = this.currentPage * this.pageSize;
      return this.filteredCategories.slice(start, start + this.pageSize);
    }
  },
  methods: {
    showNotification(msg, type = 'success') {
      if (this.toastTimer) clearTimeout(this.toastTimer);
      this.toastMessage = msg;
      this.toastType = type;
      this.showToast = true;
      this.toastTimer = setTimeout(() => {
        this.showToast = false;
      }, 3000);
    },

    getSTT(index) {
      return this.currentPage * this.pageSize + index + 1;
    },

    async loadCategories() {
      this.loading = true;
      try {
        const response = await categoryService.getCategories();
        let categories = response?.data || [];
        
        this.allCategories = categories.map(c => ({
          ...c,
          _deletedAt: c.status === 'DELETED' ? new Date().toISOString() : null
        }));
        
        console.log('📋 All categories loaded:', this.allCategories.length);
        this.filterCategories();
      } catch (error) {
        console.error('Lỗi tải:', error);
        this.showNotification('Không thể tải danh sách', 'error');
      } finally {
        this.loading = false;
      }
    },

    filterCategories() {
      let filtered = [...this.allCategories];
      
      if (this.searchKeyword) {
        const kw = this.searchKeyword.toLowerCase();
        filtered = filtered.filter(c =>
          c.categoryName?.toLowerCase().includes(kw) ||
          c.categoryDescription?.toLowerCase().includes(kw)
        );
      }
      
      if (this.statusFilter !== 'all') {
        if (this.statusFilter === 'DELETED') {
          filtered = filtered.filter(c => c._deletedAt !== null);
        } else {
          filtered = filtered.filter(c => !c._deletedAt && c.status === this.statusFilter);
        }
      }
      
      this.filteredCategories = filtered;
      this.totalPages = Math.ceil(filtered.length / this.pageSize);
      this.currentPage = 0;
    },

    searchCategories() {
      this.filterCategories();
    },

    resetFilters() {
      this.searchKeyword = '';
      this.statusFilter = 'all';
      this.filterCategories();
    },

    goToPage(page) {
      if (page >= 0 && page < this.totalPages) {
        this.currentPage = page;
      }
    },

    openCreateModal() {
      this.isEdit = false;
      this.form = {
        categoryId: null,
        categoryName: '',
        categoryDescription: '',
        status: 'ACTIVE'
      };
      this.showModal = true;
    },

    openEditModal(cat) {
      if (cat._deletedAt) {
        this.showNotification('Không thể sửa danh mục đã xóa! Vui lòng khôi phục trước.', 'error');
        return;
      }
      this.isEdit = true;
      this.form = { ...cat };
      this.showModal = true;
    },

    closeModal() {
      this.showModal = false;
    },

    async saveCategory() {
      if (!this.form.categoryName) {
        this.showNotification('Vui lòng nhập tên danh mục', 'error');
        return;
      }
      this.saving = true;
      try {
        const data = {
          categoryName: this.form.categoryName,
          categoryDescription: this.form.categoryDescription || '',
          status: this.form.status
        };
        
        if (this.isEdit) {
          // ✅ CHỈ CẬP NHẬT LOCAL STATE - KHÔNG GỌI API UPDATE
          const index = this.allCategories.findIndex(c => c.categoryId === this.form.categoryId);
          if (index !== -1) {
            this.allCategories[index] = {
              ...this.allCategories[index],
              categoryName: data.categoryName,
              categoryDescription: data.categoryDescription,
              status: data.status
            };
          }
          this.filterCategories();
          this.showNotification('Cập nhật thành công!');
        } else {
          // ✅ THÊM MỚI - GỌI API TẠO MỚI
          const response = await categoryService.createCategory(data);
          const newCategory = response.data?.data || response.data;
          if (newCategory && newCategory.categoryId) {
            this.allCategories.push({
              ...newCategory,
              _deletedAt: null
            });
            this.filterCategories();
          }
          this.showNotification('Thêm thành công!');
        }
        this.closeModal();
      } catch (error) {
        const errorMsg = error.response?.data?.message || 'Có lỗi xảy ra!';
        this.showNotification(errorMsg, 'error');
      } finally {
        this.saving = false;
      }
    },

    async toggleStatus(cat) {
      if (cat._deletedAt) {
        this.showNotification('Không thể thay đổi trạng thái danh mục đã xóa!', 'error');
        return;
      }
      
      // ✅ CHỈ CẬP NHẬT LOCAL STATE - KHÔNG GỌI API
      const newStatus = cat.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
      const index = this.allCategories.findIndex(c => c.categoryId === cat.categoryId);
      if (index !== -1) {
        this.allCategories[index] = {
          ...this.allCategories[index],
          status: newStatus
        };
      }
      this.filterCategories();
      this.showNotification(`Đã ${newStatus === 'ACTIVE' ? 'kích hoạt' : 'tạm dừng'} danh mục!`);
    },

    // ✅ KIỂM TRA SẢN PHẨM TRƯỚC KHI XÓA (GIỐNG RÀNG BUỘC)
    async confirmDelete(cat) {
      this.checkingProducts = true;
      this.checkingCategoryId = cat.categoryId;
      this.deleteCategoryId = cat.categoryId;
      this.deleteCategoryName = cat.categoryName;
      
      try {
        // Gọi API đếm số sản phẩm thuộc danh mục này
        const response = await axios.get(`${API_BASE}/products/category/${cat.categoryId}/count`);
        const productCount = response.data?.data || response.data || 0;
        
        if (productCount > 0) {
          this.showNotification(
            `❌ Không thể xóa danh mục "${cat.categoryName}" vì đang có ${productCount} sản phẩm! Vui lòng chuyển hoặc xóa sản phẩm trước.`,
            'error'
          );
          this.checkingProducts = false;
          this.checkingCategoryId = null;
          return;
        }
        
        // Nếu không có sản phẩm, hiển thị modal xác nhận xóa
        this.showDeleteModal = true;
        this.checkingProducts = false;
        this.checkingCategoryId = null;
        
      } catch (error) {
        console.error('Lỗi kiểm tra sản phẩm:', error);
        // Nếu API chưa có, cho phép xóa (hoặc thông báo lỗi)
        this.showNotification('Không thể kiểm tra sản phẩm! Vui lòng thử lại sau.', 'error');
        this.checkingProducts = false;
        this.checkingCategoryId = null;
      }
    },

    closeDeleteModal() {
      this.showDeleteModal = false;
      this.deleteCategoryId = null;
      this.deleteCategoryName = '';
    },

    async deleteCategory() {
      try {
        // ✅ KIỂM TRA LẠI LẦN NỮA TRƯỚC KHI XÓA (để an toàn)
        const response = await axios.get(`${API_BASE}/products/category/${this.deleteCategoryId}/count`);
        const productCount = response.data?.data || response.data || 0;
        
        if (productCount > 0) {
          this.showNotification(
            `❌ Không thể xóa danh mục vì đang có ${productCount} sản phẩm!`,
            'error'
          );
          this.closeDeleteModal();
          return;
        }
        
        // ✅ XÓA MỀM - CHỈ CẬP NHẬT LOCAL STATE
        const index = this.allCategories.findIndex(c => c.categoryId === this.deleteCategoryId);
        if (index !== -1) {
          this.allCategories[index] = {
            ...this.allCategories[index],
            _deletedAt: new Date().toISOString(),
            status: 'DELETED'
          };
        }
        
        this.filterCategories();
        this.showNotification('Xóa danh mục thành công!');
        this.closeDeleteModal();
      } catch (error) {
        const errorMsg = error.response?.data?.message || 'Không thể xóa danh mục này!';
        this.showNotification(errorMsg, 'error');
      }
    },

    confirmRestore(cat) {
      this.restoreCategoryId = cat.categoryId;
      this.restoreCategoryName = cat.categoryName;
      this.showRestoreModal = true;
    },

    closeRestoreModal() {
      this.showRestoreModal = false;
      this.restoreCategoryId = null;
      this.restoreCategoryName = '';
    },

    async restoreCategory() {
      try {
        // ✅ KHÔI PHỤC - CHỈ CẬP NHẬT LOCAL STATE
        const index = this.allCategories.findIndex(c => c.categoryId === this.restoreCategoryId);
        if (index !== -1) {
          this.allCategories[index] = {
            ...this.allCategories[index],
            _deletedAt: null,
            status: 'ACTIVE'
          };
        }
        
        this.filterCategories();
        this.showNotification('Khôi phục danh mục thành công!');
        this.closeRestoreModal();
      } catch (error) {
        console.error('Lỗi khôi phục:', error);
        this.showNotification('Không thể khôi phục danh mục này!', 'error');
      }
    }
  },
  mounted() {
    this.loadCategories();
  }
};
</script>

<style scoped>
.admin-categories {
  padding: 20px;
  background-color: #f8f9fa;
  min-height: calc(100vh - 60px);
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

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

.stt-cell {
  font-weight: 600;
  color: #3498db;
  width: 60px;
}

.category-name strong {
  color: #2c3e50;
}

.deleted-text {
  text-decoration: line-through;
  color: #9ca3af;
}

.description-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #666;
}

.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}
.status-active { background-color: #e8f8f5; color: #27ae60; }
.status-inactive { background-color: #fdf2e9; color: #f39c12; }
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

.edit-btn:hover { background-color: #fef3c7; }
.toggle-btn:hover { background-color: #e8f4f8; }
.delete-btn:hover { background-color: #fde8e8; }
.restore-btn:hover { background-color: #e8f4f8; }
.delete-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.readonly-label {
  font-size: 12px;
  color: #9ca3af;
  font-style: italic;
  padding: 4px 8px;
  background: #f3f4f6;
  border-radius: 6px;
  white-space: nowrap;
}

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

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #2c3e50;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
}

.required {
  color: #e74c3c;
}

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

.warning-text {
  color: #dc3545;
  margin-top: 10px;
  font-size: 13px;
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

@media (max-width: 768px) {
  .admin-categories { padding: 15px; }
  .page-header { flex-direction: column; align-items: stretch; gap: 12px; }
  .btn-add { justify-content: center; }
  .stats-row { grid-template-columns: 1fr 1fr; gap: 12px; }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .search-box { min-width: auto; }
  .btn-search, .btn-reset { justify-content: center; }
  .table-container { overflow-x: auto; }
  .data-table { min-width: 600px; }
  .pagination { flex-direction: column; gap: 12px; }
}
</style>