<template>
  <div class="admin-categories">
    <!-- ===== PHÂN CẤP DANH MỤC (gộp chung, không tách file/route riêng) ===== -->
    <div v-if="showHierarchy" class="admin-category-hierarchy">
      <div class="page-header">
        <div class="header-left">
          <h2>Phân cấp danh mục</h2>
          <p class="subtitle">
            3 danh mục lớn mặc định (Áo, Quần, Phụ kiện) là cố định, không thể sửa/xóa.
            Bạn chỉ có thể thêm/sửa/xóa danh mục con bên trong từng danh mục lớn.
          </p>
        </div>
        <button type="button" class="btn-back-categories" @click="showHierarchy = false">
          <span class="btn-icon">📋</span> Quản lý danh mục
        </button>
      </div>

      <div v-if="hLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>Đang tải dữ liệu...</span>
      </div>

      <div v-else class="tree-container">
        <div v-for="parent in hMainParents" :key="parent.categoryId" class="parent-block">
          <div class="parent-row">
            <button class="toggle-btn" @click="hToggleExpand(parent.categoryId)">
              {{ hIsExpanded(parent.categoryId) ? '▾' : '▸' }}
            </button>
            <strong class="parent-name">{{ parent.categoryName }}</strong>
            <span v-if="hIsLocked(parent)" class="locked-badge" title="Danh mục lớn mặc định, không thể sửa/xóa">
              🔒 Mặc định
            </span>
            <span :class="['status-badge', parent.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
              {{ parent.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng' }}
            </span>
            <div class="row-actions">
              <button class="action-btn" title="Thêm danh mục con (chọn từ Quản lý danh mục)" @click="hOpenPickModal(parent)">➕ Con</button>
              <template v-if="!hIsLocked(parent)">
                <button class="action-btn action-assign" title="Gán vào 1 trong 3 danh mục lớn" @click="hOpenAssignModal(parent)">🔗 Gán vào</button>
                <button class="action-btn" title="Sửa" @click="hOpenEditModal(parent)">✏️</button>
                <button class="action-btn" title="Xóa" @click="hConfirmDelete(parent)">🗑️</button>
              </template>
            </div>
          </div>

          <div v-if="hIsExpanded(parent.categoryId)" class="children-list">
            <div v-if="!parent.children || parent.children.length === 0" class="empty-children">
              Chưa có danh mục con
            </div>
            <div v-for="child in parent.children" :key="child.categoryId" class="child-row">
              <span class="child-name">— {{ child.categoryName }}</span>
              <span :class="['status-badge', child.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
                {{ child.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng' }}
              </span>
              <div class="row-actions">
                <button class="action-btn action-detach" title="Gỡ khỏi danh mục lớn (đưa về danh mục độc lập)" @click="hConfirmDetach(child)">↩️ Gỡ khỏi danh mục lớn</button>
                <button class="action-btn" title="Sửa" @click="hOpenEditModal(child)">✏️</button>
                <button class="action-btn" title="Xóa" @click="hConfirmDelete(child)">🗑️</button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="hMainParents.length === 0" class="empty-state">
          <div class="empty-icon">📭</div>
          <p>Chưa có danh mục nào</p>
        </div>
      </div>

      <!-- Modal Thêm/Sửa (phân cấp) -->
      <div v-if="hShowModal" class="modal-overlay">
        <div class="modal-container">
          <div class="modal-header">
            <h3>{{ hIsEdit ? 'Sửa danh mục' : 'Thêm danh mục con' }}</h3>
            <button class="close-btn" @click="hCloseModal">✕</button>
          </div>
          <form @submit.prevent="hSaveCategory">
            <div class="modal-body">
              <div class="form-group">
                <label>Tên danh mục <span class="required">*</span></label>
                <input v-model="hForm.categoryName" required placeholder="Nhập tên danh mục" maxlength="20" />
              </div>
              <div class="form-group">
                <label>Mô tả</label>
                <textarea v-model="hForm.categoryDescription" rows="3" placeholder="Nhập mô tả" maxlength="20"></textarea>
              </div>
              <div class="form-group">
                <label>Thuộc danh mục lớn</label>
                <select v-model="hForm.parentCategoryId" :disabled="hIsEdit && hHasChildren(hForm.categoryId)">
                  <option :value="null">-- Không thuộc danh mục lớn nào --</option>
                  <option v-for="p in hParentOptions" :key="p.categoryId" :value="p.categoryId">
                    {{ p.categoryName }}
                  </option>
                </select>
                <small v-if="hIsEdit && hHasChildren(hForm.categoryId)" class="hint-text">
                  Danh mục này đang có danh mục con nên không thể trở thành danh mục con của danh mục khác.
                </small>
              </div>
              <div class="form-group">
                <label>Trạng thái</label>
                <select v-model="hForm.status">
                  <option value="ACTIVE">Hoạt động</option>
                  <option value="INACTIVE">Tạm dừng</option>
                </select>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn-cancel" @click="hCloseModal">Hủy</button>
              <button type="submit" class="btn-save" :disabled="hSaving">
                {{ hSaving ? 'Đang xử lý...' : (hIsEdit ? 'Cập nhật' : 'Thêm mới') }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Modal Thêm danh mục con (chọn từ danh sách đã tạo ở "Quản lý danh mục") -->
      <div v-if="hShowPickModal" class="modal-overlay">
        <div class="modal-container modal-sm">
          <div class="modal-header">
            <h3>Thêm danh mục con vào "{{ hPickParentTarget?.categoryName }}"</h3>
            <button class="close-btn" @click="hClosePickModal">✕</button>
          </div>
          <div class="modal-body">
            <p v-if="hUnassignedCategories.length === 0" class="hint-text">
              Không có danh mục nào đang chưa gán để thêm. Hãy tạo danh mục mới ở trang "Quản lý danh mục" trước.
            </p>
            <div v-else class="form-group pick-category-search">
              <label>Chọn danh mục (dữ liệu lấy từ Quản lý danh mục) <span class="required">*</span></label>
              <input
                type="text"
                v-model="hPickSearchKeyword"
                placeholder="Gõ tên danh mục để tìm..."
                autocomplete="off"
                @focus="hPickDropdownOpen = true"
                @input="hPickDropdownOpen = true"
                @blur="hPickInputBlur"
              />
              <div v-if="hPickDropdownOpen" class="pick-dropdown">
                <div
                  v-for="c in hFilteredUnassignedCategories"
                  :key="c.categoryId"
                  class="pick-dropdown-item"
                  :class="{ active: c.categoryId === hPickCategoryId }"
                  @mousedown.prevent="hSelectPickCategory(c)"
                >
                  {{ c.categoryName }}
                </div>
                <div v-if="hFilteredUnassignedCategories.length === 0" class="pick-dropdown-empty">
                  Không tìm thấy danh mục phù hợp
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="hClosePickModal">Hủy</button>
            <button class="btn-save" :disabled="!hPickCategoryId || hPicking" @click="hConfirmPick">
              {{ hPicking ? 'Đang xử lý...' : 'Thêm vào' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Modal Gán vào danh mục lớn -->
      <div v-if="hShowAssignModal" class="modal-overlay">
        <div class="modal-container modal-sm">
          <div class="modal-header">
            <h3>Gán vào danh mục lớn</h3>
            <button class="close-btn" @click="hCloseAssignModal">✕</button>
          </div>
          <div class="modal-body">
            <p>
              Chọn 1 trong 3 danh mục lớn để đưa <strong>{{ hAssignTarget?.categoryName }}</strong>
              vào làm danh mục con:
            </p>
            <div class="form-group">
              <label>Danh mục lớn <span class="required">*</span></label>
              <select v-model="hAssignParentId">
                <option :value="null">-- Chọn danh mục lớn --</option>
                <option v-for="p in hMainParents" :key="p.categoryId" :value="p.categoryId">
                  {{ p.categoryName }}
                </option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="hCloseAssignModal">Hủy</button>
            <button class="btn-save" :disabled="!hAssignParentId || hAssigning" @click="hConfirmAssign">
              {{ hAssigning ? 'Đang xử lý...' : 'Gán vào' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Modal xác nhận Gỡ khỏi danh mục lớn -->
      <div v-if="hShowDetachModal" class="modal-overlay">
        <div class="modal-container modal-sm">
          <div class="modal-header">
            <h3>Xác nhận gỡ</h3>
            <button class="close-btn" @click="hShowDetachModal = false">✕</button>
          </div>
          <div class="modal-body">
            <p>
              Gỡ <strong>{{ hDetachTarget?.categoryName }}</strong> ra khỏi danh mục lớn hiện tại?
              Danh mục này sẽ trở thành danh mục độc lập (không nằm trong 3 danh mục chính),
              dữ liệu không bị xóa.
            </p>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="hShowDetachModal = false">Hủy</button>
            <button class="btn-save" :disabled="hDetaching" @click="hDoDetach">
              {{ hDetaching ? 'Đang xử lý...' : 'Gỡ khỏi' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Modal xác nhận Xóa (phân cấp) -->
      <div v-if="hShowDeleteModal" class="modal-overlay">
        <div class="modal-container modal-sm">
          <div class="modal-header">
            <h3>Xác nhận xóa</h3>
            <button class="close-btn" @click="hShowDeleteModal = false">✕</button>
          </div>
          <div class="modal-body">
            <p>Bạn có chắc muốn xóa danh mục <strong>{{ hDeleteTarget?.categoryName }}</strong>?</p>
            <p v-if="hHasChildren(hDeleteTarget?.categoryId)" class="warning-text">
              Danh mục này đang có danh mục con. Vui lòng xóa/di chuyển danh mục con trước.
            </p>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="hShowDeleteModal = false">Hủy</button>
            <button class="btn-danger" :disabled="hHasChildren(hDeleteTarget?.categoryId)" @click="hDoDelete">Xóa</button>
          </div>
        </div>
      </div>

      <div :class="['toast', { 'toast-error': hToastType === 'error', show: hShowToast }]">
        {{ hToastMessage }}
      </div>
    </div>

    <!-- ===== DANH SÁCH DANH MỤC (mặc định) ===== -->
    <template v-else>
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>Quản lý danh mục</h2>
        <p class="subtitle">Quản lý danh mục sản phẩm</p>
      </div>
      <div class="header-actions">
        <button type="button" class="btn-hierarchy" @click="showHierarchy = true">
          <i class="fas fa-sitemap"></i> Phân cấp danh mục
        </button>
        <button class="btn-add" @click="openCreateModal">
          <i class="fas fa-plus"></i> Thêm danh mục
        </button>
      </div>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <div class="stat-card stat-total">
        <div class="stat-info">
          <div class="stat-value">{{ totalCategories }}</div>
          <div class="stat-label">Tổng danh mục</div>
        </div>
      </div>
      <div class="stat-card stat-active">
        <div class="stat-info">
          <div class="stat-value">{{ activeCount }}</div>
          <div class="stat-label">Đang hoạt động</div>
        </div>
      </div>
      <div class="stat-card stat-inactive">
        <div class="stat-info">
          <div class="stat-value">{{ inactiveCount }}</div>
          <div class="stat-label">Tạm dừng</div>
        </div>
      </div>
      <div class="stat-card stat-deleted">
        <div class="stat-info">
          <div class="stat-value">{{ deletedCount }}</div>
          <div class="stat-label">Đã xóa</div>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="search-box">
        <i class="fas fa-search search-icon"></i>
        <input
          v-model="searchKeyword"
          @keyup.enter="searchCategories"
          placeholder="Tìm kiếm danh mục..."
          autocomplete="off"
        />
        <button v-if="searchKeyword" class="clear-search" @click="searchKeyword = ''; searchCategories()"><i class="fas fa-times"></i></button>
      </div>
      <select v-model="statusFilter" @change="filterCategories" class="filter-select">
        <option value="all">Tất cả trạng thái</option>
        <option value="ACTIVE">Đang hoạt động</option>
        <option value="INACTIVE">Tạm dừng</option>
        <option value="DELETED">Đã xóa</option>
      </select>
      <button class="btn-search" @click="searchCategories">
        <i class="fas fa-search"></i> Tìm kiếm
      </button>
      <button v-if="searchKeyword || statusFilter !== 'all'" class="btn-reset" @click="resetFilters">
        <i class="fas fa-sync-alt"></i> Làm mới
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
              <span v-if="item._deletedAt" class="status-badge status-deleted">
                Đã xóa
              </span>
              <span v-else :class="['status-badge', item.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
                {{ item.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng' }}
              </span>
            </td>
            <td class="actions">
              <div class="action-buttons">
                <!-- Nếu đã xóa: chỉ hiển thị nút khôi phục -->
                <template v-if="item._deletedAt">
                  <span class="readonly-label">Đã xóa</span>
                  <button class="action-btn restore-btn" @click="confirmRestore(item)" title="Khôi phục">
                    <i class="fas fa-undo"></i>
                  </button>
                </template>
                <!-- Nếu chưa xóa: hiển thị đầy đủ -->
                <template v-else>
                  <button class="action-btn edit-btn" @click="openEditModal(item)" title="Sửa">
                    <i class="fas fa-edit"></i>
                  </button>
                  <button class="action-btn toggle-btn" @click="toggleStatus(item)" :title="item.status === 'ACTIVE' ? 'Tạm dừng' : 'Kích hoạt'">
                    <i :class="item.status === 'ACTIVE' ? 'fas fa-pause' : 'fas fa-play'"></i>
                  </button>
                  <button class="action-btn delete-btn" @click="confirmDelete(item)" title="Xóa" :disabled="checkingProducts">
                    <span v-if="checkingProducts && checkingCategoryId === item.categoryId" class="loading-spinner-small"></span>
                    <i v-else class="fas fa-trash-alt"></i>
                  </button>
                </template>
              </div>
            </td>
          </tr>
          <tr v-if="paginatedCategories.length === 0">
            <td colspan="5" class="empty-row">
              <div class="empty-state">
                <div class="empty-icon"><i class="fas fa-box-open"></i></div>
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
    <div v-if="showModal" class="modal-overlay">
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
            <div class="form-group">
              <label>Thuộc danh mục lớn</label>
              <select v-model="form.parentCategoryId">
                <option :value="null">-- Không thuộc danh mục lớn nào --</option>
                <option v-for="p in parentCategoryOptions" :key="p.categoryId" :value="p.categoryId">
                  {{ p.categoryName }}
                </option>
              </select>
              <small class="hint-text">Chọn Áo / Quần / Phụ kiện nếu muốn danh mục này nằm trong 1 trong 3 danh mục lớn ở trang Phân cấp danh mục.</small>
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
    <div v-if="showDeleteModal" class="modal-overlay">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận xóa</h3>
          <button class="close-btn" @click="closeDeleteModal">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc muốn xóa danh mục <strong>{{ deleteCategoryName }}</strong>?</p>
          <p class="warning-text">Sau khi xóa, danh mục sẽ chuyển sang trạng thái đã xóa và có thể khôi phục lại!</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeDeleteModal">Hủy</button>
          <button class="btn-danger" @click="deleteCategory">Xóa</button>
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
    </template>
  </div>
</template>

<script>
import categoryService from '@/api/categoryService';
import api from "@/api/axios";
import { useCategoryHierarchyStore } from "@/stores/categoryHierarchyStores";

export default {
  name: 'AdminCategories',
  setup() {
    const categoryStore = useCategoryHierarchyStore();
    return { categoryStore };
  },
  data() {
    return {
      showHierarchy: false,
      // ===== State cho phần Phân cấp danh mục (gộp chung) =====
      hExpandedIds: new Set(),
      hShowModal: false,
      hIsEdit: false,
      hSaving: false,
      hForm: {
        categoryId: null,
        categoryName: '',
        categoryDescription: '',
        status: 'ACTIVE',
        parentCategoryId: null
      },
      hShowDeleteModal: false,
      hDeleteTarget: null,
      hShowPickModal: false,
      hPickParentTarget: null,
      hPickCategoryId: null,
      hPickSearchKeyword: '',
      hPickDropdownOpen: false,
      hPicking: false,
      hShowAssignModal: false,
      hAssignTarget: null,
      hAssignParentId: null,
      hAssigning: false,
      hShowDetachModal: false,
      hDetachTarget: null,
      hDetaching: false,
      hShowToast: false,
      hToastMessage: '',
      hToastType: 'success',
      hToastTimer: null,
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
      // (Thêm mới) danh sách 3 danh mục lớn mặc định để chọn khi thêm/sửa
      parentCategoryOptions: [],
      form: {
        categoryId: null,
        categoryName: '',
        categoryDescription: '',
        status: 'ACTIVE',
        parentCategoryId: null
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
    },
    // ===== Computed cho phần Phân cấp danh mục (gộp chung) =====
    hTree() {
      return this.categoryStore.tree;
    },
    hLoading() {
      return this.categoryStore.loading;
    },
    hParentOptions() {
      return this.hMainParents.filter((p) => !this.hIsEdit || p.categoryId !== this.hForm.categoryId);
    },
    hMainParents() {
      return this.hTree.filter((p) => this.hIsLocked(p));
    },
    hUnassignedCategories() {
      return this.hTree.filter((p) => !this.hIsLocked(p));
    },
    hFilteredUnassignedCategories() {
      const kw = (this.hPickSearchKeyword || '').trim().toLowerCase();
      if (!kw) return this.hUnassignedCategories;
      return this.hUnassignedCategories.filter((c) =>
        (c.categoryName || '').toLowerCase().includes(kw)
      );
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
        
        // ✅ GIỮ TẤT CẢ DANH MỤC, KHÔNG LỌC BỎ
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
      
      if (this.searchKeyword && this.searchKeyword.trim()) {
        const kw = this.searchKeyword.trim().toLowerCase();
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
        status: 'ACTIVE',
        parentCategoryId: null
      };
      this.showModal = true;
    },

    openEditModal(cat) {
      if (cat._deletedAt) {
        this.showNotification('Không thể sửa danh mục đã xóa! Vui lòng khôi phục trước.', 'error');
        return;
      }
      this.isEdit = true;
      this.form = { ...cat, parentCategoryId: cat.parentCategoryId ?? null };
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
      if (!(await window.appConfirm(this.isEdit ? 'Bạn có chắc chắn muốn lưu các thay đổi này không?' : 'Bạn có chắc chắn muốn thêm dữ liệu này không?'))) return;
      this.saving = true;
      try {
        const data = {
          categoryName: this.form.categoryName,
          categoryDescription: this.form.categoryDescription || '',
          status: this.form.status,
          parentCategoryId: this.form.parentCategoryId ?? null
        };
        
        if (this.isEdit) {
          const response = await categoryService.updateCategory(this.form.categoryId, data);
          const updatedCategory = response.data?.data || response.data;
          
          const index = this.allCategories.findIndex(c => c.categoryId === this.form.categoryId);
          if (index !== -1) {
            this.allCategories[index] = {
              ...updatedCategory,
              _deletedAt: null
            };
          }
          this.filterCategories();
          this.showNotification('Cập nhật danh mục thành công!');
        } else {
          const response = await categoryService.createCategory(data);
          const newCategory = response.data?.data || response.data;
          if (newCategory && newCategory.categoryId) {
            this.allCategories.push({
              ...newCategory,
              _deletedAt: null
            });
            this.filterCategories();
          }
          this.showNotification('Thêm danh mục thành công!');
        }
        this.closeModal();
      } catch (error) {
        const errorMsg = error.response?.data?.message || error.message || 'Có lỗi xảy ra!';
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
      if (!(await window.appConfirm('Bạn có chắc chắn muốn chuyển sang trạng thái này không?'))) return;
      
      this.saving = true;
      try {
        const newStatus = cat.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        
        const response = await categoryService.updateCategory(cat.categoryId, {
          categoryName: cat.categoryName,
          categoryDescription: cat.categoryDescription,
          status: newStatus,
          parentCategoryId: cat.parentCategoryId ?? null
        });
        
        const updatedCategory = response.data?.data || response.data;
        
        const index = this.allCategories.findIndex(c => c.categoryId === cat.categoryId);
        if (index !== -1) {
          this.allCategories[index] = {
            ...updatedCategory,
            _deletedAt: null
          };
        }
        this.filterCategories();
        this.showNotification(`Đã ${newStatus === 'ACTIVE' ? 'kích hoạt' : 'tạm dừng'} danh mục!`);
      } catch (error) {
        const errorMsg = error.response?.data?.message || error.message || 'Có lỗi xảy ra!';
        this.showNotification(errorMsg, 'error');
      } finally {
        this.saving = false;
      }
    },

    // ✅ XÓA MỀM - KHÔNG XÓA KHỎI DANH SÁCH
   async confirmDelete(cat) {
  this.checkingProducts = true;
  this.checkingCategoryId = cat.categoryId;
  this.deleteCategoryId = cat.categoryId;
  this.deleteCategoryName = cat.categoryName;
  
  try {
    // ✅ Gọi API kiểm tra sản phẩm trong danh mục
    const response = await api.get(`/products/category/${cat.categoryId}/count`);
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
    
    this.showDeleteModal = true;
    this.checkingProducts = false;
    this.checkingCategoryId = null;
    
  } catch (error) {
    console.error('Lỗi kiểm tra sản phẩm:', error);
    
    // ✅ Nếu API bị lỗi (500, 404) thì vẫn cho xóa (bỏ qua kiểm tra)
    if (error.response?.status === 404 || error.response?.status === 500) {
      // API chưa có hoặc lỗi, vẫn cho xóa
      console.log('⚠️ API kiểm tra sản phẩm chưa có hoặc lỗi, vẫn cho xóa danh mục');
      this.showDeleteModal = true;
      this.checkingProducts = false;
      this.checkingCategoryId = null;
    } else {
      this.showNotification('Không thể kiểm tra sản phẩm! Vui lòng thử lại sau.', 'error');
      this.checkingProducts = false;
      this.checkingCategoryId = null;
    }
  }
},

    closeDeleteModal() {
      this.showDeleteModal = false;
      this.deleteCategoryId = null;
      this.deleteCategoryName = '';
    },

    async deleteCategory() {
      this.saving = true;
      try {
        // ✅ GỌI API XÓA MỀM
        await categoryService.deleteCategory(this.deleteCategoryId);
        
        // ✅ CẬP NHẬT TRẠNG THÁI (KHÔNG XÓA KHỎI DANH SÁCH)
        const index = this.allCategories.findIndex(c => c.categoryId === this.deleteCategoryId);
        if (index !== -1) {
          this.allCategories[index] = {
            ...this.allCategories[index],
            _deletedAt: new Date().toISOString(),
            status: 'DELETED'
          };
        }
        
        this.filterCategories();
        this.showNotification('Xóa danh mục thành công! Danh mục đã chuyển sang trạng thái đã xóa.', 'success');
        this.closeDeleteModal();
      } catch (error) {
        const errorMsg = error.response?.data?.message || error.message || 'Không thể xóa danh mục này!';
        this.showNotification(errorMsg, 'error');
      } finally {
        this.saving = false;
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
      this.saving = true;
      try {
        await categoryService.restoreCategory(this.restoreCategoryId);
        
        const index = this.allCategories.findIndex(c => c.categoryId === this.restoreCategoryId);
        if (index !== -1) {
          this.allCategories[index] = {
            ...this.allCategories[index],
            _deletedAt: null,
            status: 'ACTIVE'
          };
        }
        
        this.filterCategories();
        this.showNotification('Khôi phục danh mục thành công!', 'success');
        this.closeRestoreModal();
      } catch (error) {
        console.error('Lỗi khôi phục:', error);
        const errorMsg = error.response?.data?.message || error.message || 'Không thể khôi phục danh mục này!';
        this.showNotification(errorMsg, 'error');
      } finally {
        this.saving = false;
      }
    },
    // (Thêm mới) Lấy danh sách 3 danh mục lớn mặc định từ module Phân cấp danh mục (chỉ đọc, không ảnh hưởng gì)
    async loadParentCategoryOptions() {
      try {
        const response = await api.get(`/category-hierarchy/parents`);
        this.parentCategoryOptions = response?.data || [];
      } catch (error) {
        console.error('Lỗi tải danh sách danh mục lớn:', error);
      }
    },

    // ===== Methods cho phần Phân cấp danh mục (gộp chung) =====
    async hLoadTree() {
      try {
        await this.categoryStore.loadTree();
      } catch (error) {
        this.hNotify("Không tải được cây danh mục", "error");
      }
    },
    hIsExpanded(id) {
      return this.hExpandedIds.has(id);
    },
    hToggleExpand(id) {
      if (this.hExpandedIds.has(id)) this.hExpandedIds.delete(id);
      else this.hExpandedIds.add(id);
    },
    hHasChildren(categoryId) {
      return this.categoryStore.hasChildren(categoryId);
    },
    hIsLocked(item) {
      return this.categoryStore.isLockedCategory(item);
    },
    hOpenEditModal(item) {
      if (this.hIsLocked(item)) {
        this.hNotify("Danh mục lớn mặc định không thể chỉnh sửa", "error");
        return;
      }
      this.hIsEdit = true;
      this.hForm = {
        categoryId: item.categoryId,
        categoryName: item.categoryName,
        categoryDescription: item.categoryDescription,
        status: item.status,
        parentCategoryId: item.parentCategoryId ?? null
      };
      this.hShowModal = true;
    },
    hCloseModal() {
      this.hShowModal = false;
    },
    async hSaveCategory() {
      this.hSaving = true;
      try {
        if (this.hIsEdit) {
          await this.categoryStore.updateCategory(this.hForm.categoryId, this.hForm);
          this.hNotify("Cập nhật danh mục thành công");
        } else {
          await this.categoryStore.createCategory(this.hForm);
          this.hNotify("Thêm danh mục thành công");
        }
        this.hShowModal = false;
      } catch (error) {
        const msg = error?.response?.data?.message || error?.message || "Có lỗi xảy ra, vui lòng thử lại";
        this.hNotify(msg, "error");
      } finally {
        this.hSaving = false;
      }
    },
    hOpenPickModal(parent) {
      this.hPickParentTarget = parent;
      this.hPickCategoryId = null;
      this.hPickSearchKeyword = '';
      this.hPickDropdownOpen = false;
      this.hShowPickModal = true;
    },
    hClosePickModal() {
      this.hShowPickModal = false;
      this.hPickParentTarget = null;
      this.hPickCategoryId = null;
      this.hPickSearchKeyword = '';
      this.hPickDropdownOpen = false;
    },
    hSelectPickCategory(category) {
      this.hPickCategoryId = category.categoryId;
      this.hPickSearchKeyword = category.categoryName;
      this.hPickDropdownOpen = false;
    },
    hPickInputBlur() {
      // Đóng dropdown sau 1 khoảng ngắn để kịp nhận click chọn item (đã dùng mousedown.prevent nên an toàn)
      setTimeout(() => { this.hPickDropdownOpen = false; }, 150);
    },
    async hConfirmPick() {
      if (!this.hPickCategoryId || !this.hPickParentTarget) return;
      const selected = this.hUnassignedCategories.find((c) => c.categoryId === this.hPickCategoryId);
      if (!selected) return;
      this.hPicking = true;
      try {
        await this.categoryStore.updateCategory(selected.categoryId, {
          categoryName: selected.categoryName,
          categoryDescription: selected.categoryDescription,
          status: selected.status,
          parentCategoryId: this.hPickParentTarget.categoryId
        });
        this.hNotify("Đã thêm danh mục con thành công");
        this.hClosePickModal();
      } catch (error) {
        const msg = error?.response?.data?.message || error?.message || "Không thể thêm danh mục con, vui lòng thử lại";
        this.hNotify(msg, "error");
      } finally {
        this.hPicking = false;
      }
    },
    hOpenAssignModal(item) {
      if (this.hIsLocked(item)) return;
      if (this.hHasChildren(item.categoryId)) {
        this.hNotify("Danh mục này đang có danh mục con nên không thể gán làm con của danh mục khác", "error");
        return;
      }
      this.hAssignTarget = item;
      this.hAssignParentId = null;
      this.hShowAssignModal = true;
    },
    hCloseAssignModal() {
      this.hShowAssignModal = false;
      this.hAssignTarget = null;
      this.hAssignParentId = null;
    },
    async hConfirmAssign() {
      if (!this.hAssignParentId || !this.hAssignTarget) return;
      this.hAssigning = true;
      try {
        await this.categoryStore.updateCategory(this.hAssignTarget.categoryId, {
          categoryName: this.hAssignTarget.categoryName,
          categoryDescription: this.hAssignTarget.categoryDescription,
          status: this.hAssignTarget.status,
          parentCategoryId: this.hAssignParentId
        });
        this.hNotify("Đã gán danh mục vào danh mục lớn thành công");
        this.hCloseAssignModal();
      } catch (error) {
        const msg = error?.response?.data?.message || error?.message || "Không thể gán danh mục, vui lòng thử lại";
        this.hNotify(msg, "error");
      } finally {
        this.hAssigning = false;
      }
    },
    hConfirmDetach(child) {
      this.hDetachTarget = child;
      this.hShowDetachModal = true;
    },
    async hDoDetach() {
      if (!this.hDetachTarget) return;
      this.hDetaching = true;
      try {
        await this.categoryStore.updateCategory(this.hDetachTarget.categoryId, {
          categoryName: this.hDetachTarget.categoryName,
          categoryDescription: this.hDetachTarget.categoryDescription,
          status: this.hDetachTarget.status,
          parentCategoryId: null
        });
        this.hNotify("Đã gỡ danh mục khỏi danh mục lớn");
        this.hShowDetachModal = false;
      } catch (error) {
        const msg = error?.response?.data?.message || error?.message || "Không thể gỡ danh mục, vui lòng thử lại";
        this.hNotify(msg, "error");
      } finally {
        this.hDetaching = false;
      }
    },
    hConfirmDelete(item) {
      if (this.hIsLocked(item)) {
        this.hNotify("Danh mục lớn mặc định không thể xóa", "error");
        return;
      }
      this.hDeleteTarget = item;
      this.hShowDeleteModal = true;
    },
    async hDoDelete() {
      try {
        await this.categoryStore.deleteCategory(this.hDeleteTarget.categoryId);
        this.hNotify("Xóa danh mục thành công");
        this.hShowDeleteModal = false;
      } catch (error) {
        this.hNotify("Không thể xóa danh mục", "error");
      }
    },
    hNotify(message, type = "success") {
      this.hToastMessage = message;
      this.hToastType = type;
      this.hShowToast = true;
      clearTimeout(this.hToastTimer);
      this.hToastTimer = setTimeout(() => (this.hShowToast = false), 2500);
    }
  },
  watch: {
    showHierarchy(val) {
      if (val) this.hLoadTree();
    }
  },
  mounted() {
    this.loadCategories();
    this.loadParentCategoryOptions();
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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

.btn-hierarchy {
  background-color: #ffffff;
  color: #3498db;
  border: 1px solid #3498db;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
  text-decoration: none;
}

.btn-hierarchy:hover {
  background-color: #ebf5fb;
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

.row-deleted .category-name strong {
  color: #9ca3af;
}

.deleted-text {
  text-decoration: line-through;
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
.status-deleted { background-color: #f3f4f6; color: #6b7280; }

.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
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
  font-size: 14px;
  transition: all 0.3s;
}

.edit-btn {
  background: #ffc107;
  color: #212529;
}

.edit-btn:hover {
  background: #e0a800;
  transform: translateY(-2px);
}

.delete-btn {
  background: #dc3545;
  color: white;
}

.delete-btn:hover:not(:disabled) {
  background: #c82333;
  transform: translateY(-2px);
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.view-btn {
  background: #17a2b8;
  color: white;
}

.view-btn:hover {
  background: #138496;
  transform: translateY(-2px);
}

.restore-btn {
  background: #28a745;
  color: white;
}

.restore-btn:hover {
  background: #218838;
  transform: translateY(-2px);
}

.toggle-btn {
  background: #6c757d;
  color: white;
}

.toggle-btn:hover {
  background: #5a6268;
  transform: translateY(-2px);
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

.hint-text {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #7f8c8d;
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
  .header-actions { flex-direction: column; width: 100%; }
  .btn-hierarchy { justify-content: center; }
  .stats-row { grid-template-columns: 1fr 1fr; gap: 12px; }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .search-box { min-width: auto; }
  .btn-search, .btn-reset { justify-content: center; }
  .table-container { overflow-x: auto; }
  .data-table { min-width: 600px; }
  .pagination { flex-direction: column; gap: 12px; }
}

/* ===== Style riêng cho phần Phân cấp danh mục (gộp chung trong file này) ===== */
.admin-category-hierarchy { padding: 0; }
.btn-back-categories {
  background-color: #ffffff;
  color: #3498db;
  border: 1px solid #3498db;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
  text-decoration: none;
  white-space: nowrap;
}
.btn-back-categories:hover { background-color: #ebf5fb; transform: translateY(-2px); }
.tree-container { background: white; border-radius: 12px; padding: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.parent-block { border-bottom: 1px solid #eee; }
.parent-block:last-child { border-bottom: none; }
.parent-row .toggle-btn { background: none; border: none; font-size: 16px; cursor: pointer; width: 20px; }
.parent-name { flex: 1; font-size: 16px; }
.locked-badge { font-size: 12px; color: #8e44ad; background: #f3e8ff; padding: 3px 10px; border-radius: 20px; }
.row-actions { display: flex; gap: 6px; }
.row-actions .action-btn { width: auto; height: auto; border: 1px solid #ddd; background: white; border-radius: 6px; padding: 6px 10px; cursor: pointer; font-size: 13px; }
.row-actions .action-btn:hover { background: #f5f5f5; transform: none; }
.action-assign { border-color: #667eea; color: #667eea; }
.action-assign:hover { background: #eef0ff; }
.action-detach { border-color: #e67e22; color: #e67e22; }
.action-detach:hover { background: #fdf1e6; }
.children-list { padding: 0 10px 12px 46px; display: flex; flex-direction: column; gap: 8px; }
.child-row { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: #fafafa; border-radius: 8px; }
.child-name { flex: 1; color: #34495e; }
.empty-children { color: #9ca3af; font-style: italic; padding: 6px 0; }

.pick-category-search { position: relative; }
.pick-category-search input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}
.pick-category-search input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}
.pick-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  max-height: 220px;
  overflow-y: auto;
  box-shadow: 0 6px 16px rgba(0,0,0,0.12);
  z-index: 20;
}
.pick-dropdown-item {
  padding: 10px 14px;
  cursor: pointer;
  font-size: 14px;
  color: #2c3e50;
}
.pick-dropdown-item:hover,
.pick-dropdown-item.active {
  background: #eef0ff;
  color: #667eea;
}
.pick-dropdown-empty {
  padding: 10px 14px;
  font-size: 13px;
  color: #9ca3af;
  font-style: italic;
}
</style>
