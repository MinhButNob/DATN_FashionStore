<template>
  <div class="admin-sizes">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>Quản lý Size</h2>
        <p class="subtitle">Quản lý danh sách kích thước sản phẩm</p>
      </div>
      <button class="btn-add" @click="openAddModal">
        <span class="btn-icon-add">＋</span> Thêm Size mới
      </button>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchQuery"
          placeholder="Tìm kiếm size..."
          autocomplete="off"
        />
        <button
          v-if="searchQuery"
          class="clear-search"
          @click="
            searchQuery = '';
            currentPage = 0;
            fetchSizes();
          "
        >
          ✕
        </button>
      </div>
      <select
        v-model="filterStatus"
        @change="
          currentPage = 0;
          fetchSizes();
        "
        class="filter-select"
      >
        <option value="">Tất cả trạng thái</option>
        <option value="ACTIVE">Hoạt động</option>
        <option value="INACTIVE">Không hoạt động</option>
        <option value="DELETED">Đã xóa</option>
      </select>
      <select
        v-model="pageSize"
        @change="
          currentPage = 0;
          fetchSizes();
        "
        class="page-size-select"
      >
        <option :value="5">5 / trang</option>
        <option :value="10">10 / trang</option>
        <option :value="20">20 / trang</option>
      </select>
      <button class="btn-search" @click="currentPage = 0; fetchSizes()">Tìm kiếm</button>
    </div>

    <!-- Data Table -->
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Mã Size</th>
            <th>Tên Size</th>
            <th>Mô tả</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="sizes.length === 0">
            <td colspan="6" class="empty-row">
              <div class="empty-state">
                <div class="empty-icon">📭</div>
                <p>Không có dữ liệu size</p>
                <button class="btn-retry" @click="fetchSizes">Tải lại</button>
              </div>
            </td>
          </tr>
          <tr
            v-for="size in sizes"
            :key="size.sizeId"
            class="table-row"
            :class="{ 'row-deleted': size.status === 'DELETED' }"
          >
            <td>
              <span class="id-badge">#{{ size.sizeId }}</span>
            </td>
            <td>
              <span class="fw-bold">{{ size.sizeCode }}</span>
            </td>
            <td>
              <span class="fw-bold">{{ size.sizeName }}</span>
            </td>
            <td>{{ size.description }}</td>
            <td>
              <div
                class="status-badge"
                :class="{
                  active: size.status === 'ACTIVE',
                  inactive: size.status === 'INACTIVE',
                  deleted: size.status === 'DELETED',
                }"
              >
                {{
                  size.status === "ACTIVE"
                    ? "Hoạt động"
                    : size.status === "INACTIVE"
                      ? "Không hoạt động"
                      : size.status === "DELETED"
                        ? "Đã xóa"
                        : "Không xác định"
                }}
              </div>
            </td>
            <td class="actions">
              <!-- Nút Sửa & Xóa: chỉ hiện với ACTIVE và INACTIVE -->
              <template v-if="size.status !== 'DELETED'">
                <button
                  class="btn-edit"
                  @click="openEditModal(size)"
                  title="Sửa"
                >
                  ✏️
                </button>
                <button
                  class="btn-delete"
                  @click="confirmDelete(size.sizeId, size.sizeName)"
                  title="Xóa"
                >
                  🗑️
                </button>
              </template>
              <!-- DELETED: chỉ xem, không có thao tác -->
              <span v-else class="readonly-label">Chỉ xem</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Phân trang -->
    <div class="pagination" v-if="totalPages > 0">
      <span class="page-total">Tổng: {{ totalElements }} bản ghi</span>
      <div class="page-controls">
        <button
          @click="changePage(0)"
          :disabled="currentPage === 0"
          title="Trang đầu"
        >
          «
        </button>
        <button
          @click="changePage(currentPage - 1)"
          :disabled="currentPage === 0"
          title="Trang trước"
        >
          ‹
        </button>
        <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button
          @click="changePage(currentPage + 1)"
          :disabled="currentPage >= totalPages - 1"
          title="Trang sau"
        >
          ›
        </button>
        <button
          @click="changePage(totalPages - 1)"
          :disabled="currentPage >= totalPages - 1"
          title="Trang cuối"
        >
          »
        </button>
      </div>
    </div>

    <!-- Modal Thêm/Sửa Size -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ isEditing ? "Cập nhật Size" : "Thêm Size mới" }}</h3>
          <button class="btn-close" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveSize">
            <div class="form-group">
              <label>Mã Size <span class="required">*</span></label>
              <input
                type="text"
                v-model="formData.sizeCode"
                required
                placeholder="Ví dụ: S, M, L, XL..."
              />
            </div>

            <div class="form-group">
              <label>Tên Size <span class="required">*</span></label>
              <input
                type="text"
                v-model="formData.sizeName"
                required
                placeholder="Ví dụ: Small, Medium, Large..."
              />
            </div>

            <div class="form-group">
              <label>Mô tả</label>
              <textarea
                v-model="formData.description"
                rows="3"
                placeholder="Mô tả hoặc chi tiết kích thước..."
              ></textarea>
            </div>

            <div class="form-group">
              <label>Trạng thái</label>
              <select v-model="formData.status" class="status-select">
                <option value="ACTIVE">Hoạt động</option>
                <option value="INACTIVE">Không hoạt động</option>
              </select>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn-secondary" @click="closeModal">
                Hủy
              </button>
              <button type="submit" class="btn-primary">Lưu lại</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div
      :class="[
        'toast',
        { 'toast-error': toastType === 'error', show: toastShow },
      ]"
    >
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from "vue";
import { sizeService } from "../../services/sizeService";

const sizes = ref([]);
const totalElements = ref(0);
const totalPages = ref(0);
const currentPage = ref(0);
const pageSize = ref(10);

const searchQuery = ref("");
const filterStatus = ref("");
const showModal = ref(false);
const isEditing = ref(false);

const toastShow = ref(false);
const toastMessage = ref("");
const toastType = ref("success");

const defaultForm = {
  sizeId: null,
  sizeCode: "",
  sizeName: "",
  description: "",
  status: "ACTIVE",
};
const formData = ref({ ...defaultForm });

const showToast = (message, type = "success") => {
  toastMessage.value = message;
  toastType.value = type;
  toastShow.value = true;
  setTimeout(() => {
    toastShow.value = false;
  }, 3000);
};

const fetchSizes = async () => {
  try {
    const data = await sizeService.getSizes(
      currentPage.value,
      pageSize.value,
      searchQuery.value,
      filterStatus.value || null,
    );
    sizes.value = data.content || [];
    totalElements.value = data.totalElements || 0;
    totalPages.value = data.totalPages || 0;
  } catch (error) {
    console.error("Lỗi khi tải danh sách size:", error);
    showToast("Không thể tải danh sách size!", "error");
  }
};

const changePage = (page) => {
  currentPage.value = page;
  fetchSizes();
};

watch(searchQuery, () => {
  currentPage.value = 0;
  fetchSizes();
});

onMounted(() => {
  fetchSizes();
});

// Modal
const openAddModal = () => {
  isEditing.value = false;
  formData.value = { ...defaultForm };
  showModal.value = true;
};

const openEditModal = (size) => {
  isEditing.value = true;
  formData.value = {
    sizeId: size.sizeId,
    sizeCode: size.sizeCode,
    sizeName: size.sizeName,
    description: size.description,
    status: size.status,
  };
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
};

const saveSize = async () => {
  try {
    if (isEditing.value) {
      await sizeService.updateSize(formData.value.sizeId, formData.value);
      showToast("Cập nhật size thành công!");
    } else {
      await sizeService.createSize(formData.value);
      showToast("Thêm size mới thành công!");
    }
    fetchSizes();
    closeModal();
  } catch (error) {
    console.error("Lỗi khi lưu size:", error);
    showToast(error.response?.data?.message || "Lỗi khi lưu size!", "error");
  }
};

const confirmDelete = async (id, name) => {
  if (confirm(`Bạn có chắc chắn muốn xóa size "${name || id}" không?`)) {
    try {
      await sizeService.deleteSize(id);
      showToast(`Đã chuyển size "${name}" sang trạng thái đã xóa!`);
      fetchSizes();
    } catch (error) {
      console.error("Lỗi khi xóa size:", error);
      showToast(error.response?.data?.message || "Lỗi khi xóa size!", "error");
    }
  }
};

const confirmRestore = async (id, name) => {
  if (confirm(`Bạn có chắc chắn muốn khôi phục size "${name}" không?`)) {
    try {
      await sizeService.restoreSize(id);
      showToast("Đã khôi phục size thành công!");
      fetchSizes();
    } catch (error) {
      console.error("Lỗi khi khôi phục size:", error);
      showToast(
        error.response?.data?.message || "Lỗi khi khôi phục size!",
        "error",
      );
    }
  }
};
</script>

<style scoped>
.admin-sizes {
  padding: 20px;
  background-color: #f8f9fa;
  min-height: calc(100vh - 60px);
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
  min-width: 200px;
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

.filter-select,
.page-size-select {
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
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
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

/* Deleted row style */
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

.fw-bold {
  font-weight: 600;
  color: #2c3e50;
}

/* Status Badge */
.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.status-badge.active {
  background-color: #e8f8f5;
  color: #27ae60;
}

.status-badge.inactive {
  background-color: #fdf2e9;
  color: #f39c12;
}

.status-badge.deleted {
  background-color: #f3f4f6;
  color: #9ca3af;
}

/* Actions */
.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.btn-edit,
.btn-delete {
  background: none;
  border: none;
  padding: 6px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}

.btn-edit:hover {
  background-color: #e8f4f8;
}

.btn-delete:hover {
  background-color: #fde8e8;
}

.readonly-label {
  font-size: 12px;
  color: #9ca3af;
  font-style: italic;
  padding: 4px 8px;
  background: #f3f4f6;
  border-radius: 6px;
}

/* Empty state */
.empty-state {
  text-align: center;
  padding: 40px;
  color: #7f8c8d;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 10px;
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

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-in-out;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #1f2937;
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #9ca3af;
  cursor: pointer;
}

.btn-close:hover {
  color: #1f2937;
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #374151;
}

.required {
  color: #dc2626;
}

.form-group input[type="text"],
.form-group textarea,
.status-select {
  width: 100%;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-group input[type="text"]:focus,
.form-group textarea:focus,
.status-select:focus {
  border-color: #3b82f6;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f3f4f6;
}

.btn-primary {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-secondary {
  background: #f3f4f6;
  color: #4b5563;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

/* Toast */
.toast {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background-color: #27ae60;
  color: white;
  padding: 12px 24px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(100px);
  opacity: 0;
  transition: all 0.3s;
  z-index: 9999;
}

.toast.show {
  transform: translateY(0);
  opacity: 1;
}

.toast.toast-error {
  background-color: #e74c3c;
}
</style>
