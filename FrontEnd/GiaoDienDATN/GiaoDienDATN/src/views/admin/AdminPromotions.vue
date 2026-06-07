<template>
  <div class="admin-promotions">
    <div class="admin-header">
      <h1 class="page-title">Quản lý khuyến mãi</h1>
      <button class="btn btn-primary" @click="openAddModal">
        <i class="fas fa-plus"></i> Thêm khuyến mãi
      </button>
    </div>

    <!-- Statistics Cards -->
    <div class="stats-grid">
      <StatsCard
        title="Tổng khuyến mãi"
        :value="statistics?.totalPromotions || 0"
        icon="fas fa-tags"
        color="#e63946"
      />
      <StatsCard
        title="Đang hoạt động"
        :value="statistics?.activePromotions || 0"
        icon="fas fa-check-circle"
        color="#28a745"
      />
      <StatsCard
        title="Đã dừng"
        :value="statistics?.inactivePromotions || 0"
        icon="fas fa-stop-circle"
        color="#dc3545"
      />
    </div>

    <!-- Tab buttons -->
    <div class="tab-buttons">
      <button
        class="tab-btn"
        :class="{ active: !promotionStore.showDeleted }"
        @click="switchTab(false)"
      >
        <i class="fas fa-list"></i> Danh sách khuyến mãi
      </button>
      <button
        class="tab-btn"
        :class="{ active: promotionStore.showDeleted }"
        @click="switchTab(true)"
      >
        <i class="fas fa-trash-alt"></i> Đã xóa
      </button>
    </div>

    <!-- Filters (chỉ hiển thị khi ở tab danh sách chính) -->
    <div class="filters-bar" v-if="!promotionStore.showDeleted">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="keyword"
          placeholder="Tìm kiếm theo tên..."
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
    <div v-if="promotionStore.loading" class="loading-container">
      <div class="spinner"></div>
      <p>Đang tải dữ liệu...</p>
    </div>

    <!-- Table -->
    <div v-else class="table-container">
      <table class="promotion-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên khuyến mãi</th>
            <th>Mô tả</th>
            <th>Giảm giá</th>
            <th>Ngày bắt đầu</th>
            <th>Ngày kết thúc</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <!-- Hiển thị danh sách theo tab -->
          <tr v-for="promo in displayList" :key="promo.promotionId">
            <td>#{{ promo.promotionId }}</td>
            <td>{{ promo.promotionName }}</td>
            <td>{{ promo.promotionDescription || "---" }}</td>
            <td>
              <span class="discount-badge"
                >{{ promo.promotionDiscountPercent }}%</span
              >
            </td>
            <td>{{ formatDate(promo.promotionStartDate) }}</td>
            <td>{{ formatDate(promo.promotionEndDate) }}</td>
            <td>
              <span
                class="status-badge"
                :class="
                  promo.promotionStatus === 'ACTIVE'
                    ? 'status-active'
                    : 'status-inactive'
                "
              >
                {{
                  promo.promotionStatus === "ACTIVE" ? "Hoạt động" : "Đã dừng"
                }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <!-- Tab ĐÃ XÓA: chỉ hiển thị nút khôi phục -->
                <template v-if="promotionStore.showDeleted">
                  <button
                    class="action-btn restore-btn"
                    @click="confirmRestore(promo.promotionId)"
                    title="Khôi phục"
                  >
                    <i class="fas fa-trash-restore"></i>
                  </button>
                </template>

                <!-- Tab DANH SÁCH CHÍNH: hiển thị đầy đủ -->
                <template v-else>
                  <button
                    class="action-btn edit-btn"
                    @click="openEditModal(promo)"
                    title="Sửa"
                  >
                    <i class="fas fa-edit"></i>
                  </button>
                  <button
                    class="action-btn status-btn"
                    :class="
                      promo.promotionStatus === 'ACTIVE'
                        ? 'status-inactive-btn'
                        : 'status-active-btn'
                    "
                    @click="confirmToggleStatus(promo)"
                    :title="
                      promo.promotionStatus === 'ACTIVE' ? 'Dừng' : 'Kích hoạt'
                    "
                  >
                    <i
                      :class="
                        promo.promotionStatus === 'ACTIVE'
                          ? 'fas fa-stop'
                          : 'fas fa-play'
                      "
                    ></i>
                  </button>
                  <button
                    class="action-btn delete-btn"
                    @click="confirmDelete(promo.promotionId)"
                    title="Xóa"
                  >
                    <i class="fas fa-trash"></i>
                  </button>
                </template>
              </div>
            </td>
          </tr>
          <tr v-if="displayList.length === 0">
            <td colspan="8" class="no-data">
              {{
                promotionStore.showDeleted
                  ? "Không có khuyến mãi nào đã xóa"
                  : "Không tìm thấy khuyến mãi nào"
              }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="promotionStore.totalPages > 0">
      <button
        class="page-btn"
        :disabled="promotionStore.currentPage === 0"
        @click="goToPage(promotionStore.currentPage - 1)"
      >
        <i class="fas fa-chevron-left"></i>
      </button>
      <button
        v-for="page in displayedPages"
        :key="page"
        class="page-btn"
        :class="{ active: promotionStore.currentPage === page }"
        @click="goToPage(page)"
      >
        {{ page + 1 }}
      </button>
      <button
        class="page-btn"
        :disabled="promotionStore.currentPage === promotionStore.totalPages - 1"
        @click="goToPage(promotionStore.currentPage + 1)"
      >
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- Modals -->
    <PromotionModal
      :visible="showModal"
      :is-edit="isEditMode"
      :initial-data="editingPromotion"
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
import { usePromotionStore } from "../../stores/promotionStore";
import { useNotification } from "../../composables/useNotification";
import StatsCard from "../../components/admin/StatsCard.vue";
import PromotionModal from "../../components/admin/PromotionModal.vue";
import ConfirmDialog from "../../components/admin/ConfirmDialog.vue";

const promotionStore = usePromotionStore();
const { statistics } = storeToRefs(promotionStore);
const { showNotification } = useNotification();

const keyword = ref("");
const statusFilter = ref("");
const showModal = ref(false);
const showConfirmDialog = ref(false);
const isEditMode = ref(false);
const editingPromotion = ref(null);
const confirmAction = ref("");
const confirmTitle = ref("");
const confirmMessage = ref("");
const confirmButtonText = ref("");
let pendingPromotionId = null;

// Danh sách hiển thị theo tab
const displayList = computed(() => {
  return promotionStore.showDeleted
    ? promotionStore.deletedPromotions
    : promotionStore.promotions;
});

const displayedPages = computed(() => {
  const total = promotionStore.totalPages;
  const current = promotionStore.currentPage;
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
    if (promotionStore.showDeleted) {
      await promotionStore.fetchDeletedPromotions(0, 10);
    } else {
      await promotionStore.fetchPromotions(
        0,
        10,
        statusFilter.value,
        keyword.value,
      );
    }
    await promotionStore.fetchStatistics();
  } catch (error) {
    showNotification("Lỗi khi tải dữ liệu", "error");
  }
};

const switchTab = (showDeleted) => {
  promotionStore.toggleShowDeleted(showDeleted);
  keyword.value = "";
  statusFilter.value = "";
  promotionStore.fetchPromotions(0, 10, "", "");
  promotionStore.fetchDeletedPromotions(0, 10);
  promotionStore.fetchStatistics();
};

const onSearch = () => {
  promotionStore.fetchPromotions(0, 10, statusFilter.value, keyword.value);
};

const onFilterChange = () => {
  promotionStore.fetchPromotions(0, 10, statusFilter.value, keyword.value);
};

const refreshData = () => {
  keyword.value = "";
  statusFilter.value = "";
  loadData();
};

const goToPage = (page) => {
  if (promotionStore.showDeleted) {
    promotionStore.fetchDeletedPromotions(page, 10);
  } else {
    promotionStore.fetchPromotions(page, 10, statusFilter.value, keyword.value);
  }
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
  editingPromotion.value = null;
  showModal.value = true;
};

const openEditModal = (promo) => {
  isEditMode.value = true;
  editingPromotion.value = { ...promo };
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  editingPromotion.value = null;
};

const handleSubmit = async (data) => {
  try {
    if (isEditMode.value) {
      await promotionStore.editPromotion(
        editingPromotion.value.promotionId,
        data,
      );
      showNotification("Cập nhật khuyến mãi thành công!", "success");
    } else {
      await promotionStore.addPromotion(data);
      showNotification("Thêm khuyến mãi thành công!", "success");
    }
  } catch (error) {
    showNotification(error.response?.data?.message || "Có lỗi xảy ra", "error");
  }
};

const confirmToggleStatus = (promo) => {
  pendingPromotionId = promo.promotionId;
  const isActivating = promo.promotionStatus !== "ACTIVE";
  confirmTitle.value = isActivating ? "Xác nhận kích hoạt" : "Xác nhận dừng";
  confirmMessage.value = isActivating
    ? `Kích hoạt khuyến mãi "${promo.promotionName}"?`
    : `Dừng khuyến mãi "${promo.promotionName}"?`;
  confirmButtonText.value = isActivating ? "Kích hoạt" : "Dừng";
  confirmAction.value = "toggleStatus";
  showConfirmDialog.value = true;
};

const confirmDelete = (id) => {
  pendingPromotionId = id;
  confirmTitle.value = "Xác nhận xóa";
  confirmMessage.value =
    "Bạn có chắc chắn muốn xóa khuyến mãi này? Khuyến mãi sẽ được chuyển vào thùng rác.";
  confirmButtonText.value = "Xóa";
  confirmAction.value = "delete";
  showConfirmDialog.value = true;
};

const confirmRestore = (id) => {
  pendingPromotionId = id;
  confirmTitle.value = "Xác nhận khôi phục";
  confirmMessage.value = "Bạn có chắc chắn muốn khôi phục khuyến mãi này?";
  confirmButtonText.value = "Khôi phục";
  confirmAction.value = "restore";
  showConfirmDialog.value = true;
};

const closeConfirmDialog = () => {
  showConfirmDialog.value = false;
  pendingPromotionId = null;
  confirmAction.value = "";
};

const handleConfirm = async () => {
  try {
    if (confirmAction.value === "toggleStatus") {
      await promotionStore.toggleStatus(pendingPromotionId);
      showNotification("Thay đổi trạng thái thành công!", "success");
    } else if (confirmAction.value === "delete") {
      await promotionStore.removePromotion(pendingPromotionId);
      showNotification("Xóa khuyến mãi thành công!", "success");
    } else if (confirmAction.value === "restore") {
      await promotionStore.restorePromotionItem(pendingPromotionId);
      showNotification("Khôi phục khuyến mãi thành công!", "success");
    }
  } catch (error) {
    showNotification(error.response?.data?.message || "Có lỗi xảy ra", "error");
  } finally {
    closeConfirmDialog();
  }
};

watch([keyword, statusFilter], () => {
  if (!promotionStore.showDeleted) {
    promotionStore.fetchPromotions(0, 10, statusFilter.value, keyword.value);
  }
});

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.admin-promotions {
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
.promotion-table {
  width: 100%;
  border-collapse: collapse;
}
.promotion-table th {
  background: #f8f9fa;
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: #555;
  border-bottom: 2px solid #eee;
}
.promotion-table td {
  padding: 15px;
  border-bottom: 1px solid #eee;
  color: #666;
}
.promotion-table tr:hover {
  background: #f8f9fa;
}
.discount-badge {
  background: #e63946;
  color: white;
  padding: 4px 8px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
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
.edit-btn {
  background: #ffc107;
  color: #333;
}
.edit-btn:hover {
  background: #e0a800;
}
.status-active-btn {
  background: #28a745;
  color: white;
}
.status-active-btn:hover {
  background: #218838;
}
.status-inactive-btn {
  background: #dc3545;
  color: white;
}
.status-inactive-btn:hover {
  background: #c82333;
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
</style>
