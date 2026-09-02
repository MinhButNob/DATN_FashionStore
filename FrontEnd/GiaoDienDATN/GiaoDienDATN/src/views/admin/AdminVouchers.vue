<template>
  <div class="admin-vouchers">
    <div class="admin-header">
      <h1 class="page-title">Quản lý voucher</h1>
      <button class="btn btn-primary" @click="openAddModal">
        <i class="fas fa-plus"></i> Thêm voucher
      </button>
    </div>

    <!-- Statistics Cards -->
    <div class="stats-grid">
      <StatsCard
        title="Tổng voucher"
        :value="statistics?.totalVouchers || 0"
        icon="fas fa-ticket-alt"
        color="#e63946"
      />
      <StatsCard
        title="Đang hoạt động"
        :value="statistics?.activeVouchers || 0"
        icon="fas fa-check-circle"
        color="#28a745"
      />
      <StatsCard
        title="Đã dừng"
        :value="statistics?.inactiveVouchers || 0"
        icon="fas fa-stop-circle"
        color="#dc3545"
      />
      <StatsCard
        title="Đã sử dụng"
        :value="statistics?.totalUsedCount || 0"
        icon="fas fa-chart-line"
        color="#17a2b8"
      />
    </div>

    <!-- Tab buttons -->
    <div class="tab-buttons">
      <button
        class="tab-btn"
        :class="{ active: !voucherStore.showDeleted }"
        @click="switchTab(false)"
      >
        <i class="fas fa-list"></i> Danh sách voucher
      </button>
      <button
        class="tab-btn"
        :class="{ active: voucherStore.showDeleted }"
        @click="switchTab(true)"
      >
        <i class="fas fa-trash-alt"></i> Đã xóa
      </button>
    </div>

    <!-- Filters (chỉ hiển thị khi ở tab danh sách chính) -->
    <div class="filters-bar" v-if="!voucherStore.showDeleted">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="keyword"
          placeholder="Tìm kiếm theo mã voucher..."
          @input="onSearch"
        />
      </div>
      <select
        v-model="statusFilter"
        @change="onFilterChange"
        class="filter-select"
      >
        <option value="">Tất cả trạng thái</option>
        <option value="ACTIVE">Đang hoạt động</option>
        <option value="INACTIVE">Đã dừng</option>
      </select>
      <button class="btn btn-outline" @click="refreshData">
        <i class="fas fa-sync-alt"></i> Làm mới
      </button>
    </div>

    <!-- Loading -->
    <div v-if="voucherStore.loading" class="loading-container">
      <div class="spinner"></div>
      <p>Đang tải dữ liệu...</p>
    </div>

    <!-- Table -->
    <div v-else class="table-container">
      <table class="voucher-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Mã voucher</th>
            <th>Giảm giá</th>
            <th>Điều kiện</th>
            <th>Số lần dùng</th>
            <th>Ngày bắt đầu</th>
            <th>Ngày kết thúc</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="voucher in displayList" :key="voucher.voucherId">
            <td>#{{ voucher.voucherId }}</td>
            <td>
              <strong>{{ voucher.voucherCode }}</strong>
            </td>
            <td>
              <span class="discount-badge">
                {{ formatDiscount(voucher) }}
              </span>
            </td>
            <td>
              <span v-if="voucher.voucherMinOrderValue > 0">
                Đơn tối thiểu: {{ formatPrice(voucher.voucherMinOrderValue) }}
              </span>
              <span v-else class="text-muted">Không yêu cầu</span>
            </td>
            <td>
              {{ voucher.voucherUsedCount || 0 }} /
              {{ voucher.voucherMaxUsage || "∞" }}
            </td>
            <td>{{ formatDate(voucher.voucherStartDate) }}</td>
            <td>{{ formatDate(voucher.voucherEndDate) }}</td>
            <td>
              <span
                class="status-badge"
                :class="
                  voucher.voucherStatus === 'ACTIVE'
                    ? 'status-active'
                    : 'status-inactive'
                "
              >
                {{
                  voucher.voucherStatus === "ACTIVE" ? "Hoạt động" : "Đã dừng"
                }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <!-- Tab ĐÃ XÓA: chỉ hiển thị nút khôi phục -->
                <template v-if="voucherStore.showDeleted">
                  <button
                    class="action-btn restore-btn"
                    @click="confirmRestore(voucher.voucherId)"
                    title="Khôi phục"
                  >
                    <i class="fas fa-undo"></i>
                  </button>
                </template>

                <!-- Tab DANH SÁCH CHÍNH: hiển thị đầy đủ -->
                <template v-else>
                  <button
                    class="action-btn edit-btn"
                    @click="openEditModal(voucher)"
                    title="Sửa"
                  >
                    <i class="fas fa-edit"></i>
                  </button>
                  <button
                    class="action-btn status-btn"
                    :class="
                      voucher.voucherStatus === 'ACTIVE'
                        ? 'status-inactive-btn'
                        : 'status-active-btn'
                    "
                    @click="confirmToggleStatus(voucher)"
                    :title="
                      voucher.voucherStatus === 'ACTIVE' ? 'Dừng' : 'Kích hoạt'
                    "
                  >
                    <i
                      :class="
                        voucher.voucherStatus === 'ACTIVE'
                          ? 'fas fa-pause'
                          : 'fas fa-play'
                      "
                    ></i>
                  </button>
                  <button
                    class="action-btn delete-btn"
                    @click="confirmDelete(voucher.voucherId)"
                    title="Xóa"
                  >
                    <i class="fas fa-trash-alt"></i>
                  </button>
                </template>
              </div>
            </td>
          </tr>
          <tr v-if="displayList.length === 0">
            <td colspan="9" class="no-data">
              {{
                voucherStore.showDeleted
                  ? "Không có voucher nào đã xóa"
                  : "Không tìm thấy voucher nào"
              }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="voucherStore.totalPages > 0">
      <button
        class="page-btn"
        :disabled="voucherStore.currentPage === 0"
        @click="goToPage(voucherStore.currentPage - 1)"
      >
        <i class="fas fa-chevron-left"></i>
      </button>
      <button
        v-for="page in displayedPages"
        :key="page"
        class="page-btn"
        :class="{ active: voucherStore.currentPage === page }"
        @click="goToPage(page)"
      >
        {{ page + 1 }}
      </button>
      <button
        class="page-btn"
        :disabled="voucherStore.currentPage === voucherStore.totalPages - 1"
        @click="goToPage(voucherStore.currentPage + 1)"
      >
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- Modals -->
    <VoucherModal
      :visible="showModal"
      :is-edit="isEditMode"
      :initial-data="editingVoucher"
      @close="closeModal"
      @submit="handleSubmit"
    />
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
import { ref, computed, onMounted, watch } from "vue";
import { storeToRefs } from "pinia";
import { useVoucherStore } from "../../stores/voucherStore";
import { useNotification } from "../../composables/useNotification";
import StatsCard from "../../components/admin/StatsCard.vue";
import VoucherModal from "../../components/admin/VoucherModal.vue";
import ConfirmDialog from "../../components/admin/ConfirmDialog.vue";

const voucherStore = useVoucherStore();
const { statistics } = storeToRefs(voucherStore);
const { showNotification } = useNotification();

const keyword = ref("");
const statusFilter = ref("");
const showModal = ref(false);
const showConfirmDialog = ref(false);
const isEditMode = ref(false);
const editingVoucher = ref(null);
const confirmAction = ref("");
const confirmTitle = ref("");
const confirmMessage = ref("");
const confirmButtonText = ref("");
let pendingVoucherId = null;

const displayList = computed(() => {
  return voucherStore.showDeleted
    ? voucherStore.deletedVouchers
    : voucherStore.vouchers;
});

const displayedPages = computed(() => {
  const total = voucherStore.totalPages;
  const current = voucherStore.currentPage;
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

const loadData = async () => {
  try {
    if (voucherStore.showDeleted) {
      await voucherStore.fetchDeletedVouchers(0, 10);
    } else {
      await voucherStore.fetchVouchers(
        0,
        10,
        statusFilter.value,
        keyword.value,
      );
    }
    await voucherStore.fetchStatistics();
  } catch (error) {
    showNotification("Lỗi khi tải dữ liệu", "error");
  }
};

const switchTab = (showDeleted) => {
  voucherStore.toggleShowDeleted(showDeleted);
  keyword.value = "";
  statusFilter.value = "";
  loadData();
};

const onSearch = () => {
  voucherStore.fetchVouchers(0, 10, statusFilter.value, keyword.value);
};

const onFilterChange = () => {
  voucherStore.fetchVouchers(0, 10, statusFilter.value, keyword.value);
};

const refreshData = () => {
  keyword.value = "";
  statusFilter.value = "";
  loadData();
};

const goToPage = (page) => {
  if (voucherStore.showDeleted) {
    voucherStore.fetchDeletedVouchers(page, 10);
  } else {
    voucherStore.fetchVouchers(page, 10, statusFilter.value, keyword.value);
  }
};

const formatPrice = (price) => {
  if (!price) return "0₫";
  return price.toLocaleString("vi-VN") + "₫";
};

const formatDiscount = (voucher) => {
  if (voucher.voucherDiscountAmount && voucher.voucherDiscountAmount > 0) {
    return formatPrice(voucher.voucherDiscountAmount);
  }
  if (voucher.voucherDiscountPercent && voucher.voucherDiscountPercent > 0) {
    const max = voucher.voucherMaxDiscountAmount || voucher.voucherMaxDiscount || voucher.maxDiscountAmount || voucher.maxDiscount;
    if (max && max > 0) {
      return `${voucher.voucherDiscountPercent}% (Tối đa ${formatPrice(max)})`;
    }
    return voucher.voucherDiscountPercent + "%";
  }
  return "---";
};

const formatDate = (dateString) => {
  if (!dateString) return "---";
  const date = new Date(dateString);
  return (
    date.toLocaleDateString("vi-VN") +
    " " +
    date.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })
  );
};

const openAddModal = () => {
  isEditMode.value = false;
  editingVoucher.value = null;
  showModal.value = true;
};

const openEditModal = (voucher) => {
  isEditMode.value = true;
  editingVoucher.value = { ...voucher };
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  editingVoucher.value = null;
};

const handleSubmit = async (data) => {
  try {
    if (isEditMode.value) {
      await voucherStore.editVoucher(editingVoucher.value.voucherId, data);
      showNotification("Cập nhật voucher thành công!", "success");
    } else {
      await voucherStore.addVoucher(data);
      showNotification("Thêm voucher thành công!", "success");
    }
  } catch (error) {
    showNotification(error.response?.data?.message || "Có lỗi xảy ra", "error");
  }
};

const confirmToggleStatus = (voucher) => {
  pendingVoucherId = voucher.voucherId;
  const isActivating = voucher.voucherStatus !== "ACTIVE";
  confirmTitle.value = isActivating ? "Xác nhận kích hoạt" : "Xác nhận dừng";
  confirmMessage.value = isActivating
    ? `Kích hoạt voucher "${voucher.voucherCode}"?`
    : `Dừng voucher "${voucher.voucherCode}"?`;
  confirmButtonText.value = isActivating ? "Kích hoạt" : "Dừng";
  confirmAction.value = "toggleStatus";
  showConfirmDialog.value = true;
};

const confirmDelete = (id) => {
  pendingVoucherId = id;
  confirmTitle.value = "Xác nhận xóa";
  confirmMessage.value =
    "Bạn có chắc chắn muốn xóa voucher này? Voucher sẽ được chuyển vào thùng rác.";
  confirmButtonText.value = "Xóa";
  confirmAction.value = "delete";
  showConfirmDialog.value = true;
};

const confirmRestore = (id) => {
  pendingVoucherId = id;
  confirmTitle.value = "Xác nhận khôi phục";
  confirmMessage.value = "Bạn có chắc chắn muốn khôi phục voucher này?";
  confirmButtonText.value = "Khôi phục";
  confirmAction.value = "restore";
  showConfirmDialog.value = true;
};

const closeConfirmDialog = () => {
  showConfirmDialog.value = false;
  pendingVoucherId = null;
  confirmAction.value = "";
};

const handleConfirm = async () => {
  try {
    if (confirmAction.value === "toggleStatus") {
      await voucherStore.toggleStatus(pendingVoucherId);
      showNotification("Thay đổi trạng thái thành công!", "success");
    } else if (confirmAction.value === "delete") {
      await voucherStore.removeVoucher(pendingVoucherId);
      showNotification("Xóa voucher thành công!", "success");
    } else if (confirmAction.value === "restore") {
      await voucherStore.restoreVoucherItem(pendingVoucherId);
      showNotification("Khôi phục voucher thành công!", "success");
    }
  } catch (error) {
    showNotification(error.response?.data?.message || "Có lỗi xảy ra", "error");
  } finally {
    closeConfirmDialog();
  }
};

watch([keyword, statusFilter], () => {
  if (!voucherStore.showDeleted) {
    voucherStore.fetchVouchers(0, 10, statusFilter.value, keyword.value);
  }
});

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.admin-vouchers {
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
.tab-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.tab-btn {
  padding: 10px 20px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}
.tab-btn.active {
  background: #e63946;
  color: white;
  border-color: #e63946;
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
.filter-select {
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  min-width: 180px;
  cursor: pointer;
  background: white;
}
.table-container {
  background: white;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
}
.voucher-table {
  width: 100%;
  border-collapse: collapse;
}
.voucher-table th {
  background: #f8f9fa;
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: #555;
  border-bottom: 2px solid #eee;
}
.voucher-table td {
  padding: 15px;
  border-bottom: 1px solid #eee;
  color: #666;
}
.voucher-table tr:hover {
  background: #f8f9fa;
}
.discount-badge {
  background: #e63946;
  color: white;
  padding: 4px 8px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}
.text-muted {
  color: #999;
  font-style: italic;
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
.status-inactive {
  background: #f8d7da;
  color: #721c24;
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
.status-active-btn {
  background: #28a745;
  color: white;
}
.status-active-btn:hover {
  background: #218838;
  transform: translateY(-2px);
}
.status-inactive-btn {
  background: #6c757d;
  color: white;
}
.status-inactive-btn:hover {
  background: #5a6268;
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
.restore-btn {
  background: #28a745;
  color: white;
}
.restore-btn:hover {
  background: #218838;
  transform: translateY(-2px);
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
</style>
