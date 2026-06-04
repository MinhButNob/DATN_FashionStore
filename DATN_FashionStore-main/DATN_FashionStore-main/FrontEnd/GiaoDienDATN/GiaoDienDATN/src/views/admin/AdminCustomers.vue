<template>
  <div class="admin-customers">
    <div class="admin-header">
      <h1 class="page-title">Quản lý khách hàng</h1>
      <button class="btn btn-primary" @click="openAddModal">
        <i class="fas fa-plus"></i> Thêm khách hàng
      </button>
    </div>

    <!-- Statistics Cards -->
    <div class="stats-grid">
      <StatsCard
        title="Tổng khách hàng"
        :value="statistics?.totalCustomers || 0"
        icon="fas fa-users"
        color="#e63946"
      />
      <StatsCard
        title="Đang hoạt động"
        :value="statistics?.activeCustomers || 0"
        icon="fas fa-user-check"
        color="#28a745"
      />
      <StatsCard
        title="Tạm khóa"
        :value="statistics?.lockedCustomers || 0"
        icon="fas fa-user-lock"
        color="#dc3545"
      />
      <StatsCard
        title="Danh mục có SP"
        :value="statistics?.categoriesWithProducts || 0"
        icon="fas fa-boxes"
        color="#17a2b8"
      />
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="keyword"
          placeholder="Tìm kiếm theo tên, email, SĐT..."
          @input="onSearch"
        />
      </div>
      <button class="btn btn-outline" @click="refreshData">
        <i class="fas fa-sync-alt"></i> Làm mới
      </button>
    </div>

    <!-- Loading -->
    <div v-if="customerStore.loading" class="loading-container">
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
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="customer in customerStore.customers"
            :key="customer.customerId"
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
            <td>{{ customer.phone || "---" }}</td>
            <td>{{ customer.address || "---" }}</td>
            <td>
              <span
                class="status-badge"
                :class="customer.isLocked ? 'status-locked' : 'status-active'"
              >
                {{ customer.isLocked ? "Tạm khóa" : "Hoạt động" }}
              </span>
            </td>
            <td>{{ formatDate(customer.createdAt) }}</td>
            <td>
              <div class="action-buttons">
                <button
                  class="action-btn view-btn"
                  @click="viewCustomerDetail(customer)"
                  title="Xem chi tiết"
                >
                  <i class="fas fa-eye"></i>
                </button>
                <button
                  class="action-btn edit-btn"
                  @click="openEditModal(customer)"
                  title="Sửa"
                >
                  <i class="fas fa-edit"></i>
                </button>
                <button
                  class="action-btn"
                  :class="customer.isLocked ? 'unlock-btn' : 'lock-btn'"
                  @click="confirmToggleLock(customer)"
                  :title="customer.isLocked ? 'Mở khóa' : 'Khóa'"
                >
                  <i
                    :class="
                      customer.isLocked ? 'fas fa-unlock-alt' : 'fas fa-lock'
                    "
                  ></i>
                </button>
                <button
                  class="action-btn delete-btn"
                  @click="confirmDelete(customer.customerId)"
                  title="Xóa"
                >
                  <i class="fas fa-trash"></i>
                </button>
                <!-- Nút khôi phục - chỉ hiển thị khi tài khoản không hoạt động -->
                <button
                  v-if="!customer.isActive"
                  class="action-btn restore-btn"
                  @click="confirmRestore(customer.customerId)"
                  title="Khôi phục"
                >
                  <i class="fas fa-undo-alt"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="customerStore.customers.length === 0">
            <td colspan="8" class="no-data">Không tìm thấy khách hàng nào</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="customerStore.totalPages > 0">
      <button
        class="page-btn"
        :disabled="customerStore.currentPage === 0"
        @click="goToPage(customerStore.currentPage - 1)"
      >
        <i class="fas fa-chevron-left"></i>
      </button>
      <button
        v-for="page in displayedPages"
        :key="page"
        class="page-btn"
        :class="{ active: customerStore.currentPage === page }"
        @click="goToPage(page)"
      >
        {{ page + 1 }}
      </button>
      <button
        class="page-btn"
        :disabled="customerStore.currentPage === customerStore.totalPages - 1"
        @click="goToPage(customerStore.currentPage + 1)"
      >
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- Detail Modal -->
    <div v-if="showDetailModal" class="modal" @click.self="closeDetailModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Chi tiết khách hàng</h2>
          <button class="close-btn" @click="closeDetailModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="detail-item">
            <strong>ID:</strong> {{ selectedCustomer?.customerId }}
          </div>
          <div class="detail-item">
            <strong>Tên:</strong> {{ selectedCustomer?.customerName }}
          </div>
          <div class="detail-item">
            <strong>Email:</strong> {{ selectedCustomer?.email }}
          </div>
          <div class="detail-item">
            <strong>SĐT:</strong> {{ selectedCustomer?.phone || "---" }}
          </div>
          <div class="detail-item">
            <strong>Địa chỉ:</strong> {{ selectedCustomer?.address || "---" }}
          </div>
          <div class="detail-item">
            <strong>Trạng thái:</strong>
            {{ selectedCustomer?.isLocked ? "Tạm khóa" : "Hoạt động" }}
          </div>
          <div class="detail-item">
            <strong>Ngày tạo:</strong>
            {{ formatDate(selectedCustomer?.createdAt) }}
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeDetailModal">
            Đóng
          </button>
        </div>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <CustomerModal
      :visible="showCustomerModal"
      :is-edit="isEditMode"
      :initial-data="editingCustomer"
      @close="closeCustomerModal"
      @submit="handleCustomerSubmit"
    />

    <!-- Confirm Dialog -->
    <ConfirmDialog
      :visible="showConfirmDialog"
      :title="confirmTitle"
      :message="confirmMessage"
      :confirm-text="confirmButtonText"
      @confirm="handleConfirm"
      @cancel="closeConfirmDialog"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useCustomerStore } from "../../stores/customerStore";
import { useNotification } from "../../composables/useNotification";
import StatsCard from "../../components/admin/StatsCard.vue";
import CustomerModal from "../../components/admin/CustomerModal.vue";
import ConfirmDialog from "../../components/admin/ConfirmDialog.vue";

const customerStore = useCustomerStore();
const { statistics } = storeToRefs(customerStore);
const { showNotification } = useNotification();

// State
const keyword = ref("");
const showDetailModal = ref(false);
const showCustomerModal = ref(false);
const showConfirmDialog = ref(false);
const selectedCustomer = ref(null);
const isEditMode = ref(false);
const editingCustomer = ref(null);
const confirmAction = ref("");
const confirmTitle = ref("");
const confirmMessage = ref("");
const confirmButtonText = ref("");
let pendingCustomerId = null;

// Computed
const displayedPages = computed(() => {
  const total = customerStore.totalPages;
  const current = customerStore.currentPage;
  const delta = 2;
  const range = [];
  const rangeWithDots = [];
  let l;
  for (let i = 0; i < total; i++) {
    if (
      i === 0 ||
      i === total - 1 ||
      (i >= current - delta && i <= current + delta)
    )
      range.push(i);
  }
  range.forEach((i) => {
    if (l) {
      if (i - l === 2) rangeWithDots.push(l + 1);
      else if (i - l !== 1) rangeWithDots.push("...");
    }
    rangeWithDots.push(i);
    l = i;
  });
  return rangeWithDots;
});

// Methods
const loadData = async () => {
  try {
    await Promise.all([
      customerStore.fetchCustomers(0, 10, keyword.value),
      customerStore.fetchStatistics(),
    ]);
  } catch (error) {
    showNotification("Lỗi khi tải dữ liệu", "error");
  }
};

const onSearch = () => {
  customerStore.fetchCustomers(0, 10, keyword.value);
};
const refreshData = () => {
  keyword.value = "";
  loadData();
};
const goToPage = (page) => {
  customerStore.fetchCustomers(page, 10, keyword.value);
};
const formatDate = (dateString) => {
  if (!dateString) return "---";
  return new Date(dateString).toLocaleDateString("vi-VN");
};

const viewCustomerDetail = (customer) => {
  selectedCustomer.value = customer;
  showDetailModal.value = true;
};
const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedCustomer.value = null;
};

const openAddModal = () => {
  isEditMode.value = false;
  editingCustomer.value = null;
  showCustomerModal.value = true;
};
const openEditModal = (customer) => {
  isEditMode.value = true;
  editingCustomer.value = { ...customer };
  showCustomerModal.value = true;
};
const closeCustomerModal = () => {
  showCustomerModal.value = false;
  editingCustomer.value = null;
};

const handleCustomerSubmit = async (data) => {
  try {
    if (isEditMode.value) {
      await customerStore.editCustomer(editingCustomer.value.customerId, {
        customerName: data.customerName,
        phone: data.phone,
        address: data.address,
      });
      showNotification("Cập nhật thành công!", "success");
    } else {
      await customerStore.addCustomer(data);
      showNotification("Thêm thành công!", "success");
    }
  } catch (error) {
    showNotification(error.response?.data?.message || "Có lỗi xảy ra", "error");
  }
};

const confirmToggleLock = (customer) => {
  pendingCustomerId = customer.customerId;
  const isLocking = !customer.isLocked;
  confirmTitle.value = isLocking
    ? "Xác nhận khóa tài khoản"
    : "Xác nhận mở khóa";
  confirmMessage.value = isLocking
    ? `Khóa tài khoản của "${customer.customerName}"?`
    : `Mở khóa tài khoản của "${customer.customerName}"?`;
  confirmButtonText.value = isLocking ? "Khóa" : "Mở khóa";
  confirmAction.value = "toggleLock";
  showConfirmDialog.value = true;
};

const confirmDelete = (id) => {
  pendingCustomerId = id;
  confirmTitle.value = "Xác nhận xóa";
  confirmMessage.value = "Bạn có chắc chắn muốn xóa khách hàng này?";
  confirmButtonText.value = "Xóa";
  confirmAction.value = "delete";
  showConfirmDialog.value = true;
};

const confirmRestore = (id) => {
  pendingCustomerId = id;
  confirmTitle.value = "Xác nhận khôi phục";
  confirmMessage.value = "Bạn có chắc chắn muốn khôi phục khách hàng này?";
  confirmButtonText.value = "Khôi phục";
  confirmAction.value = "restore";
  showConfirmDialog.value = true;
};

const closeConfirmDialog = () => {
  showConfirmDialog.value = false;
  pendingCustomerId = null;
  confirmAction.value = "";
};

const handleConfirm = async () => {
  try {
    if (confirmAction.value === "toggleLock") {
      await customerStore.toggleLock(pendingCustomerId);
      showNotification("Thay đổi trạng thái thành công!", "success");
    } else if (confirmAction.value === "delete") {
      await customerStore.removeCustomer(pendingCustomerId);
      showNotification("Xóa khách hàng thành công!", "success");
    } else if (confirmAction.value === "restore") {
      await customerStore.restoreCustomer(pendingCustomerId);
      showNotification("Khôi phục khách hàng thành công!", "success");
    }
  } catch (error) {
    showNotification(error.response?.data?.message || "Có lỗi xảy ra", "error");
  } finally {
    closeConfirmDialog();
  }
};

// Lifecycle
onMounted(() => {
  loadData();
});
</script>

<style scoped>
.admin-customers {
  padding: 20px;
}
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}
.page-title {
  font-size: 28px;
  color: #333;
  margin: 0;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 25px;
}
.filters-bar {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}
.search-box {
  flex: 1;
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
.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}
.view-btn {
  background: #17a2b8;
  color: white;
}
.view-btn:hover {
  background: #138496;
}
.edit-btn {
  background: #ffc107;
  color: #333;
}
.edit-btn:hover {
  background: #e0a800;
}
.lock-btn {
  background: #dc3545;
  color: white;
}
.lock-btn:hover {
  background: #c82333;
}
.unlock-btn {
  background: #28a745;
  color: white;
}
.unlock-btn:hover {
  background: #218838;
}
.delete-btn {
  background: #6c757d;
  color: white;
}
.delete-btn:hover {
  background: #5a6268;
}
.restore-btn {
  background: #17a2b8;
  color: white;
}
.restore-btn:hover {
  background: #138496;
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
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
.no-data {
  text-align: center;
  padding: 40px;
  color: #999;
}
.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
}
.page-btn {
  padding: 8px 12px;
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
.page-btn.active {
  background: #e63946;
  color: white;
  border-color: #e63946;
}
.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}
.btn-primary {
  background: #e63946;
  color: white;
}
.btn-primary:hover {
  background: #c82333;
}
.btn-outline {
  background: none;
  border: 1px solid #ddd;
  color: #666;
}
.btn-outline:hover {
  background: #f5f5f5;
}
.modal {
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
}
.modal-content {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.modal-header h2 {
  font-size: 20px;
  color: #333;
  margin: 0;
}
.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
}
.modal-body {
  padding: 25px;
}
.detail-item {
  margin-bottom: 12px;
}
.modal-footer {
  padding: 20px 25px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
