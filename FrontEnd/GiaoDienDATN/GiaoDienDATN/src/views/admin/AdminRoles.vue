<template>
  <div class="admin-roles">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>Quản lý vai trò (Role)</h2>
        <p class="subtitle">Quản lý danh sách vai trò hệ thống</p>
      </div>
      <button class="btn-add" @click="openCreateModal">
        <i class="fas fa-plus"></i> Thêm vai trò
      </button>
    </div>

    <!-- Hiển thị lỗi nếu có -->
    <div v-if="roleStore.error" class="error-alert">
      <span class="error-icon"><i class="fas fa-exclamation-triangle"></i></span>
      <span class="error-message">{{ roleStore.error }}</span>
      <button class="error-close" @click="roleStore.clearError()"><i class="fas fa-times"></i></button>
    </div>

    <!-- Tìm kiếm và lọc -->
    <div class="filter-bar">
      <div class="search-box">
        <i class="fas fa-search search-icon"></i>
        <input
          v-model="searchKeyword"
          @keyup.enter="handleSearch"
          placeholder="Tìm theo tên vai trò..."
          autocomplete="off"
        />
        <button
          v-if="searchKeyword"
          class="clear-search"
          @click="
            searchKeyword = '';
            handleSearch();
          "
        >
          <i class="fas fa-times"></i>
        </button>
      </div>
      <select
        v-model="filterStatus"
        @change="handleSearch"
        class="filter-select"
      >
        <option value="">Tất cả trạng thái</option>
        <option :value="true">Hoạt động</option>
        <option :value="false">Không hoạt động</option>
      </select>
      <select
        v-model="pageSize"
        @change="handleSearch"
        class="page-size-select"
      >
        <option :value="5">5 / trang</option>
        <option :value="10">10 / trang</option>
        <option :value="20">20 / trang</option>
      </select>
      <button class="btn-search" @click="handleSearch">
        <i class="fas fa-search"></i> Tìm kiếm
      </button>
    </div>

    <!-- Bảng dữ liệu -->
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên vai trò</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody v-if="!roleStore.loading">
          <tr
            v-for="role in roleStore.roles"
            :key="role.idRole || role.id"
            class="table-row"
          >
            <td>
              <span class="id-badge">#{{ role.idRole || role.id }}</span>
            </td>
            <td>
              <span class="fw-bold">{{ role.roleName }}</span>
            </td>
            <td>
              <div
                class="status-badge"
                :class="{
                  active: role.status === 'ACTIVE',
                  inactive: role.status === 'INACTIVE',
                  deleted: role.status === 'DELETED' }"
              >
                {{
                  role.status === "ACTIVE"
                    ? "Hoạt động"
                    : role.status === "INACTIVE"
                      ? "Không hoạt động"
                      : role.status === "DELETED"
                        ? "Đã xóa"
                        : "Không xác định"
                }}
              </div>
            </td>
            <td class="actions">
              <button class="action-btn edit-btn" @click="openEditModal(role)" title="Sửa">
                <i class="fas fa-edit"></i>
              </button>
              <button
                class="action-btn delete-btn"
                @click="confirmDelete(role)"
                title="Xoá"
              >
                <i class="fas fa-trash-alt"></i>
              </button>
            </td>
          </tr>
          <tr v-if="roleStore.roles.length === 0 && !roleStore.loading">
            <td colspan="4" class="empty-row">
              <div class="empty-state">
                <div class="empty-icon"><i class="fas fa-box-open"></i></div>
                <p>Không có dữ liệu vai trò</p>
                <button class="btn-retry" @click="fetchData">Tải lại</button>
              </div>
            </td>
          </tr>
        </tbody>
        <tbody v-if="roleStore.loading">
          <tr>
            <td colspan="4" class="loading-row">
              <div class="loading-spinner"></div>
              <span>Đang tải dữ liệu...</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Phân trang -->
    <div class="pagination" v-if="roleStore.pagination.totalPages > 0">
      <span class="page-total"
        >Tổng: {{ roleStore.pagination.totalElements }} vai trò</span
      >
      <div class="page-controls">
        <button
          @click="changePage(0)"
          :disabled="roleStore.pagination.currentPage === 0"
          title="Trang đầu"
        >
          «
        </button>
        <button
          @click="changePage(roleStore.pagination.currentPage - 1)"
          :disabled="roleStore.pagination.currentPage === 0"
          title="Trang trước"
        >
          ‹
        </button>
        <span class="page-info"
          >{{ roleStore.pagination.currentPage + 1 }} /
          {{ roleStore.pagination.totalPages }}</span
        >
        <button
          @click="changePage(roleStore.pagination.currentPage + 1)"
          :disabled="
            roleStore.pagination.currentPage >=
            roleStore.pagination.totalPages - 1
          "
          title="Trang sau"
        >
          ›
        </button>
        <button
          @click="changePage(roleStore.pagination.totalPages - 1)"
          :disabled="
            roleStore.pagination.currentPage >=
            roleStore.pagination.totalPages - 1
          "
          title="Trang cuối"
        >
          »
        </button>
      </div>
    </div>

    <!-- Modal -->
    <RoleModal
      v-if="showModal"
      :isEdit="isEdit"
      :initialData="selectedRole"
      @close="closeModal"
      @submit="handleSubmit"
    />

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
import { ref, onMounted } from "vue";
import { useRoleStore } from "@/stores/roleStore";
import RoleModal from "@/components/admin/RoleModal.vue";

const roleStore = useRoleStore();

// State
const searchKeyword = ref("");
const filterStatus = ref("");
const pageSize = ref(10);
const currentPage = ref(0);

const showModal = ref(false);
const isEdit = ref(false);
const selectedRole = ref(null);

const toastShow = ref(false);
const toastMessage = ref("");
const toastType = ref("success");

// Lifecycle
onMounted(() => {
  fetchData();
});

// Methods
const fetchData = () => {
  if (searchKeyword.value.trim() !== "") {
    roleStore.searchRoles(
      searchKeyword.value,
      currentPage.value,
      pageSize.value,
    );
  } else {
    roleStore.fetchRoles(
      currentPage.value,
      pageSize.value,
      filterStatus.value !== "" ? filterStatus.value : null,
    );
  }
};

const handleSearch = () => {
  currentPage.value = 0;
  fetchData();
};

const changePage = (page) => {
  currentPage.value = page;
  fetchData();
};

const showToast = (message, type = "success") => {
  toastMessage.value = message;
  toastType.value = type;
  toastShow.value = true;
  setTimeout(() => {
    toastShow.value = false;
  }, 3000);
};

const openCreateModal = () => {
  selectedRole.value = null;
  isEdit.value = false;
  showModal.value = true;
};

const openEditModal = (role) => {
  const roleId = role.roleId || role.id;
  if (!roleId) {
    showToast("Không tìm thấy ID vai trò", "error");
    return;
  }
  
  selectedRole.value = { 
    ...role,
    id: roleId
  };
  isEdit.value = true;
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  selectedRole.value = null;
};

const handleSubmit = async (roleData) => {
  try {
    if (isEdit.value) {
      const roleId = selectedRole.value.roleId || selectedRole.value.id;
      if (!roleId) {
        throw new Error("ID không hợp lệ");
      }
      await roleStore.updateRole(roleId, roleData);
      showToast("Cập nhật vai trò thành công!");
      fetchData();
    } else {
      await roleStore.createRole(roleData);
      showToast("Thêm vai trò mới thành công!");
    }
    closeModal();
  } catch (error) {
    showToast(error.message || "Có lỗi xảy ra", "error");
  }
};

const confirmDelete = async (role) => {
  // ✅ Lấy ID từ role object trực tiếp
  const id = role.roleId || role.id;
  
  if (!id) {
    showToast("Không tìm thấy ID vai trò", "error");
    return;
  }
  
  // ✅ Kiểm tra nếu role đã bị xóa thì không xóa nữa
  if (role.status === 'DELETED') {
    showToast("Vai trò đã bị xóa trước đó", "error");
    return;
  }
  
  if (await window.appConfirm(`Bạn có chắc chắn muốn xóa vai trò "${role.roleName}" không?`)) {
    try {
      await roleStore.deleteRole(id);
      showToast(`Đã chuyển vai trò "${role.roleName}" sang trạng thái đã xóa!`);
      fetchData();
    } catch (error) {
      showToast(error.message || "Không thể xóa vai trò này", "error");
    }
  }
};
</script>

<style scoped>
.admin-roles {
  padding: 20px;
  background-color: #f8f9fa;
  min-height: calc(100vh - 60px);
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

.error-alert {
  background-color: #fde8e8;
  border: 1px solid #fad2d2;
  color: #e74c3c;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.error-close {
  margin-left: auto;
  background: none;
  border: none;
  color: #e74c3c;
  cursor: pointer;
}

.filter-bar {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  background: white;
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.search-box {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
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
}

.btn-search {
  padding: 10px 20px;
  background-color: #2c3e50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

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

.actions {
  display: flex;
  gap: 8px;
}

.btn-edit,
.btn-delete {
  background: none;
  border: none;
  padding: 6px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-edit:hover {
  background-color: #e8f4f8;
}

.btn-delete:hover {
  background-color: #fde8e8;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #7f8c8d;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.loading-row td {
  text-align: center;
  padding: 40px;
  color: #7f8c8d;
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
}

.page-controls button:disabled {
  background: #f8f9fa;
  color: #bdc3c7;
  cursor: not-allowed;
}

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
  z-index: 1000;
}

.toast.show {
  transform: translateY(0);
  opacity: 1;
}

.toast.toast-error {
  background-color: #e74c3c;
}

/* Action buttons */
.actions {
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

.delete-btn:hover {
  background: #c82333;
  transform: translateY(-2px);
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
</style>
