<template>
  <div class="admin-sizes">
    <div class="page-header">
      <h2>Quản lý Size</h2>
      <button class="btn-primary" @click="openAddModal">
        <i class="fas fa-plus"></i> Thêm Size mới
      </button>
    </div>

    <!-- Stats or Filters placeholder -->
    <div class="filters-section">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="searchQuery"
          placeholder="Tìm kiếm size..."
        />
      </div>
    </div>

    <!-- Data Table -->
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên Size</th>
            <th>Mô tả</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="sizes.length === 0">
            <td colspan="5" class="empty-state">Không có dữ liệu</td>
          </tr>
          <tr v-for="size in sizes" :key="size.id">
            <td>#{{ size.id }}</td>
            <td class="font-bold">{{ size.name }}</td>
            <td>{{ size.description }}</td>
            <td>
              <span
                :class="['status-badge', size.isActive ? 'active' : 'inactive']"
              >
                {{ size.isActive ? "Hoạt động" : "Tạm ẩn" }}
              </span>
            </td>
            <td class="actions">
              <button
                class="btn-icon edit"
                @click="openEditModal(size)"
                title="Sửa"
              >
                <i class="fas fa-edit"></i>
              </button>
              <button
                class="btn-icon delete"
                @click="confirmDelete(size.id)"
                title="Xóa"
              >
                <i class="fas fa-trash"></i>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination-section" v-if="totalPages > 1">
      <button
        :disabled="currentPage === 0"
        @click="
          currentPage--;
          fetchSizes();
        "
        class="pagination-btn"
      >
        <i class="fas fa-chevron-left"></i>
      </button>
      <span class="pagination-info"
        >Trang {{ currentPage + 1 }} / {{ totalPages }}</span
      >
      <button
        :disabled="currentPage >= totalPages - 1"
        @click="
          currentPage++;
          fetchSizes();
        "
        class="pagination-btn"
      >
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- Modal cho Thêm/Sửa Size -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ isEditing ? "Cập nhật Size" : "Thêm Size mới" }}</h3>
          <button class="btn-close" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveSize">
            <div class="form-group">
              <label>Tên Size <span class="required">*</span></label>
              <input
                type="text"
                v-model="formData.name"
                required
                placeholder="Ví dụ: S, M, L, XL, 38, 39..."
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

            <div class="form-group checkbox-group">
              <label>
                <input type="checkbox" v-model="formData.isActive" />
                Đang hoạt động
              </label>
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
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from "vue";
import { useNotification } from "../../composables/useNotification";
import { sizeService } from "../../services/sizeService";

const { showNotification } = useNotification();

const sizes = ref([]);
const totalElements = ref(0);
const totalPages = ref(0);
const currentPage = ref(0);
const pageSize = ref(10);

const searchQuery = ref("");
const showModal = ref(false);
const isEditing = ref(false);

const defaultForm = {
  id: null,
  name: "",
  description: "",
  isActive: true,
};
const formData = ref({ ...defaultForm });

const fetchSizes = async () => {
  try {
    const data = await sizeService.getSizes(
      currentPage.value,
      pageSize.value,
      searchQuery.value,
    );
    sizes.value = data.content || [];
    totalElements.value = data.totalElements || 0;
    totalPages.value = data.totalPages || 0;
  } catch (error) {
    console.error("Lỗi khi tải danh sách size:", error);
    showNotification("Không thể tải danh sách size!", "error");
  }
};

watch(searchQuery, () => {
  currentPage.value = 0;
  fetchSizes();
});

onMounted(() => {
  fetchSizes();
});

// Methods
const openAddModal = () => {
  isEditing.value = false;
  formData.value = { ...defaultForm };
  showModal.value = true;
};

const openEditModal = (size) => {
  isEditing.value = true;
  formData.value = { ...size };
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
};

const saveSize = async () => {
  try {
    if (isEditing.value) {
      await sizeService.updateSize(formData.value.id, formData.value);
      showNotification("Cập nhật size thành công!", "success");
    } else {
      await sizeService.createSize(formData.value);
      showNotification("Thêm size mới thành công!", "success");
    }
    fetchSizes();
    closeModal();
  } catch (error) {
    console.error("Lỗi khi lưu size:", error);
    showNotification("Lỗi khi lưu size!", "error");
  }
};

const confirmDelete = async (id) => {
  if (confirm("Bạn có chắc chắn muốn xóa size này?")) {
    try {
      await sizeService.deleteSize(id);
      showNotification("Đã xóa size!", "success");
      fetchSizes();
    } catch (error) {
      console.error("Lỗi khi xóa size:", error);
      showNotification("Lỗi khi xóa size!", "error");
    }
  }
};
</script>

<style scoped>
.admin-sizes {
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  color: #1f2937;
  font-weight: 600;
}

.filters-section {
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.search-box {
  position: relative;
  max-width: 300px;
}

.search-box i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
}

.search-box input {
  width: 100%;
  padding: 10px 10px 10px 36px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  outline: none;
  transition: all 0.3s;
}

.search-box input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
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

.data-table th,
.data-table td {
  padding: 16px;
  text-align: left;
  border-bottom: 1px solid #f3f4f6;
}

.data-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #4b5563;
  font-size: 14px;
  text-transform: uppercase;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.font-bold {
  font-weight: 600;
}

.empty-state {
  text-align: center;
  padding: 40px !important;
  color: #6b7280;
}

.status-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background: #dcfce7;
  color: #166534;
}

.status-badge.inactive {
  background: #fee2e2;
  color: #991b1b;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-icon.edit {
  background: #e0f2fe;
  color: #0284c7;
}

.btn-icon.edit:hover {
  background: #bae6fd;
}

.btn-icon.delete {
  background: #fee2e2;
  color: #dc2626;
}

.btn-icon.delete:hover {
  background: #fecaca;
}

.btn-primary {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
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
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

/* Modal Styles */
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
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s;
}

.form-group input[type="text"]:focus,
.form-group textarea:focus {
  border-color: #3b82f6;
}

.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: normal;
  cursor: pointer;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f3f4f6;
}

/* Pagination Styles */
.pagination-section {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}

.pagination-btn {
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #4b5563;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  background: #f3f4f6;
  color: #1f2937;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  font-size: 14px;
  color: #4b5563;
  font-weight: 500;
}
</style>
