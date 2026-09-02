<template>
  <div class="admin-customers">
    <div class="admin-header">
      <h1 class="page-title">Quản lý khách hàng</h1>
    </div>

    <!-- Statistics Cards -->
    <div class="stats-grid">
      <StatsCard title="Tổng khách hàng" :value="statistics?.totalCustomers || 0" icon="fas fa-users" color="#e63946" />
      <StatsCard title="Đang hoạt động" :value="statistics?.activeCustomers || 0" icon="fas fa-user-check"
        color="#28a745" />
      <StatsCard title="Tạm khóa" :value="statistics?.lockedCustomers || 0" icon="fas fa-user-lock" color="#dc3545" />
      <StatsCard title="Đã xóa" :value="statistics?.deletedCustomers || 0" icon="fas fa-user-slash" color="#6c757d" />
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input type="text" v-model="keyword" placeholder="Tìm kiếm theo tên, email, SĐT..." @input="onSearch" />
      </div>
      <select v-model="filterStatus" @change="onFilterStatus" class="filter-select">
        <option value="">Tất cả trạng thái</option>
        <option value="ACTIVE">Hoạt động</option>
        <option value="LOCKED">Tạm khóa</option>
        <option value="DELETED">Đã xóa</option>
      </select>
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
          <tr v-for="customer in customerStore.customers" :key="customer.customerId" class="table-row"
            :class="{ 'row-deleted': customer.isActive === 0 || customer.isActive === false }">
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
              <span class="status-badge" :class="customer.isLocked
                  ? 'status-locked'
                  : (customer.isActive === 0 || customer.isActive === false)
                    ? 'status-deleted'
                    : 'status-active'
                ">
                {{
                  customer.isLocked
                    ? "Tạm khóa"
                    : (customer.isActive === 0 || customer.isActive === false)
                      ? "Đã xóa"
                      : "Hoạt động"
                }}
              </span>
            </td>
            <td>{{ formatDate(customer.createdAt) }}</td>
            <td>
              <div class="action-buttons">
                <!-- Xem chi tiết - Luôn hiển thị -->
                <button class="action-btn view-btn" @click="viewCustomerDetail(customer)" title="Xem chi tiết">
                  <i class="fas fa-eye"></i>
                </button>
                
                <!-- Đổi trạng thái: HIỂN THỊ CHO TẤT CẢ -->
                <button class="action-btn edit-btn" @click="openEditModal(customer)" title="Đổi trạng thái">
                  <i class="fas fa-exchange-alt"></i>
                </button>
                
                <!-- Nút Mở khóa: Chỉ hiện khi đang bị tạm khóa -->
                <template v-if="customer.isLocked === true">
                  <button class="action-btn unlock-btn" @click="confirmUnlock(customer.customerId)" title="Mở khóa tài khoản">
                    <i class="fas fa-unlock"></i>
                  </button>
                </template>
                
                <!-- Nút Xóa: Chỉ hiện khi chưa bị xóa -->
                <template v-if="customer.isActive !== 0 && customer.isActive !== false">
                  <button class="action-btn delete-btn" @click="confirmDelete(customer.customerId)" title="Xóa">
                    <i class="fas fa-trash"></i>
                  </button>
                </template>
                
                <!-- Nút Khôi phục: Chỉ hiện khi đã bị xóa -->
                <template v-if="customer.isActive === 0 || customer.isActive === false">
                  <button class="action-btn restore-btn" @click="confirmRestore(customer.customerId)" title="Khôi phục">
                    <i class="fas fa-undo"></i>
                  </button>
                </template>
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
      <button class="page-btn" :disabled="customerStore.currentPage === 0"
        @click="goToPage(customerStore.currentPage - 1)">
        <i class="fas fa-chevron-left"></i>
      </button>
      <button v-for="page in displayedPages" :key="page" class="page-btn"
        :class="{ active: customerStore.currentPage === page }" @click="goToPage(page)">
        {{ page + 1 }}
      </button>
      <button class="page-btn" :disabled="customerStore.currentPage === customerStore.totalPages - 1"
        @click="goToPage(customerStore.currentPage + 1)">
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- Detail Modal -->
    <div v-if="showDetailModal" class="modal">
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
            {{
              selectedCustomer?.isLocked
                ? "Tạm khóa"
                : (selectedCustomer?.isActive === 0 || selectedCustomer?.isActive === false)
                  ? "Đã xóa"
                  : "Hoạt động"
            }}
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

    <!-- Edit Modal -->
    <CustomerModal :visible="showCustomerModal" :initial-data="editingCustomer" @close="closeCustomerModal"
      @submit="handleCustomerSubmit" />

    <!-- Confirm Dialog -->
    <ConfirmDialog :visible="showConfirmDialog" :title="confirmTitle" :message="confirmMessage"
      :confirm-text="confirmButtonText" @confirm="handleConfirm" @cancel="closeConfirmDialog" />
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
const { statistics, filterStatus } = storeToRefs(customerStore);
const { showNotification } = useNotification();

// State
const keyword = ref("");
const showDetailModal = ref(false);
const showCustomerModal = ref(false);
const showConfirmDialog = ref(false);
const selectedCustomer = ref(null);
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
    const status = filterStatus.value || null;
    await Promise.all([
      customerStore.fetchCustomers(0, 10, keyword.value, status),
      customerStore.fetchStatistics(),
    ]);
  } catch (error) {
    showNotification("Lỗi khi tải dữ liệu", "error");
  }
};

const onSearch = () => {
  const status = filterStatus.value || null;
  customerStore.fetchCustomers(0, 10, keyword.value, status);
};

const onFilterStatus = () => {
  const status = filterStatus.value || null;
  customerStore.fetchCustomers(0, 10, keyword.value, status);
};

const refreshData = () => {
  keyword.value = "";
  filterStatus.value = "";
  loadData();
};

const goToPage = (page) => {
  const status = filterStatus.value || null;
  customerStore.fetchCustomers(page, 10, keyword.value, status);
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

const openEditModal = (customer) => {
  editingCustomer.value = {
    ...customer,
    phone: customer.phone || '',
    address: customer.address || ''
  };
  showCustomerModal.value = true;
};

const closeCustomerModal = () => {
  showCustomerModal.value = false;
  editingCustomer.value = null;
};

const handleCustomerSubmit = async (data) => {
  if (!(await window.appConfirm(editingCustomer.value?.customerId ? 'Bạn có chắc chắn muốn lưu các thay đổi này không?' : 'Bạn có chắc chắn muốn thêm dữ liệu này không?'))) return;
  try {
    if (editingCustomer.value?.customerId) {
      const currentIsLocked = !!editingCustomer.value.isLocked;
      const newIsLocked = !!data.isLocked;

      if (currentIsLocked !== newIsLocked) {
        await customerStore.toggleLock(editingCustomer.value.customerId);
        showNotification("Cập nhật trạng thái thành công!", "success");
      } else {
        showNotification("Không có thay đổi để cập nhật", "warning");
      }
    } else {
      await customerStore.addCustomer(data);
      showNotification("Thêm khách hàng thành công!", "success");
    }

    await loadData();
    closeCustomerModal();
  } catch (error) {
    console.error("Submit error:", error);
    const message = error.response?.data?.message || "Có lỗi xảy ra";
    showNotification(message, "error");
  }
};

// ✅ Xác nhận mở khóa
const confirmUnlock = (id) => {
  pendingCustomerId = id;
  confirmTitle.value = "Xác nhận mở khóa";
  confirmMessage.value = "Bạn có chắc chắn muốn mở khóa tài khoản khách hàng này?";
  confirmButtonText.value = "Mở khóa";
  confirmAction.value = "unlock";
  showConfirmDialog.value = true;
};

// ✅ Xác nhận xóa (chỉ xóa mềm)
const confirmDelete = (id) => {
  pendingCustomerId = id;
  confirmTitle.value = "Xác nhận xóa";
  confirmMessage.value = "Bạn có chắc chắn muốn xóa khách hàng này? Khách hàng sẽ chuyển sang trạng thái 'Đã xóa' và có thể khôi phục sau.";
  confirmButtonText.value = "Xóa";
  confirmAction.value = "delete";
  showConfirmDialog.value = true;
};

// ✅ Xác nhận khôi phục
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

// ✅ Xử lý xác nhận từ ConfirmDialog
const handleConfirm = async () => {
  try {
    if (confirmAction.value === "delete") {
      await customerStore.removeCustomer(pendingCustomerId);
      showNotification("Xóa khách hàng thành công!", "success");
      await loadData();
    } else if (confirmAction.value === "unlock") {
      await customerStore.toggleLock(pendingCustomerId);
      showNotification("Mở khóa tài khoản thành công!", "success");
      await loadData();
    } else if (confirmAction.value === "restore") {
      await customerStore.restoreCustomer(pendingCustomerId);
      showNotification("Khôi phục khách hàng thành công!", "success");
      await loadData();
    }
  } catch (error) {
    console.error("Confirm error:", error);
    const message = error.response?.data?.message || "Có lỗi xảy ra";
    showNotification(message, "error");
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
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
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
  flex-wrap: wrap;
  align-items: center;
}

.filter-select {
  padding: 10px 14px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  outline: none;
  background-color: white;
  cursor: pointer;
  font-size: 14px;
  color: #555;
  transition: border-color 0.2s;
  min-width: 160px;
}

.filter-select:focus {
  border-color: #e63946;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 0 15px;
  min-width: 200px;
}

.search-box input {
  border: none;
  background: none;
  padding: 12px;
  width: 100%;
  outline: none;
  font-size: 14px;
}

.search-box i {
  color: #999;
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
  font-size: 14px;
}

.customer-table th {
  background: #f8f9fa;
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: #555;
  border-bottom: 2px solid #eee;
  white-space: nowrap;
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
  flex-shrink: 0;
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
  white-space: nowrap;
}

.status-active {
  background: #d4edda;
  color: #155724;
}

.status-locked {
  background: #f8d7da;
  color: #721c24;
}

.status-deleted {
  background: #f3f4f6;
  color: #9ca3af;
}

.row-deleted {
  opacity: 0.6;
  background-color: #fafafa;
}

.row-deleted td {
  color: #9ca3af;
}

.row-deleted .customer-name {
  color: #9ca3af;
}

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
  transition: all 0.3s;
  font-size: 14px;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
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
  color: #212529;
}

.edit-btn:hover {
  background: #e0a800;
}

.unlock-btn {
  background: #28a745;
  color: white;
}

.unlock-btn:hover {
  background: #218838;
}

.delete-btn {
  background: #dc3545;
  color: white;
}

.delete-btn:hover {
  background: #c82333;
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
  font-style: italic;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.page-btn {
  padding: 8px 14px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  min-width: 36px;
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
  font-weight: 500;
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
  animation: fadeIn 0.3s ease;
}

.modal-content {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
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
  font-size: 24px;
  cursor: pointer;
  color: #999;
  transition: color 0.3s;
  padding: 0 5px;
}

.close-btn:hover {
  color: #e63946;
}

.modal-body {
  padding: 25px;
}

.detail-item {
  margin-bottom: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item strong {
  display: inline-block;
  min-width: 120px;
  color: #555;
}

.modal-footer {
  padding: 20px 25px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Animations */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* Responsive */
@media (max-width: 768px) {
  .admin-customers {
    padding: 10px;
  }

  .page-title {
    font-size: 22px;
  }

  .stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 10px;
  }

  .filters-bar {
    flex-direction: column;
    padding: 15px;
  }

  .search-box {
    width: 100%;
  }

  .filter-select {
    width: 100%;
  }

  .customer-table th,
  .customer-table td {
    padding: 10px;
    font-size: 13px;
  }

  .action-buttons {
    gap: 4px;
  }

  .action-btn {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }

  .modal-content {
    width: 95%;
    margin: 10px;
  }

  .detail-item strong {
    min-width: 80px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }

  .customer-table {
    font-size: 12px;
  }

  .customer-table th,
  .customer-table td {
    padding: 8px;
  }

  .customer-avatar {
    width: 30px;
    height: 30px;
  }

  .customer-avatar i {
    font-size: 18px;
  }

  .status-badge {
    font-size: 10px;
    padding: 3px 8px;
  }

  .page-btn {
    padding: 6px 10px;
    font-size: 12px;
    min-width: 30px;
  }
}
</style>
