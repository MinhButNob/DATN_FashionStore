<!-- StaffDamagedProducts.vue -->
<template>
  <div class="staff-page">
    <div class="page-header">
      <h2><i class="fas fa-box-open"></i> Quản lý sản phẩm hỏng</h2>
      <div class="header-actions">
        <button class="btn btn-outline" @click="refreshData" :disabled="loading">
          <i class="fas fa-sync" :class="{ 'fa-spin': loading }"></i>
          Làm mới
        </button>
      </div>
    </div>

    <!-- Thống kê -->
    <div class="stats-grid" v-if="stats">
      <div class="stat-card">
        <div class="stat-icon" style="background: #e63946;">
          <i class="fas fa-box"></i>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalDamagedProducts }}</span>
          <span class="stat-label">Tổng sản phẩm hỏng</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #ffc107;">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.pendingDamagedProducts }}</span>
          <span class="stat-label">Đang chờ xử lý</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #28a745;">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.processedDamagedProducts }}</span>
          <span class="stat-label">Đã xử lý</span>
        </div>
      </div>
    </div>

    <!-- Bộ lọc -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input v-model="searchKeyword" type="text" placeholder="Tìm theo mã sản phẩm, tên, SKU..." />
      </div>
      <select v-model="statusFilter" @change="loadData">
        <option value="all">Tất cả trạng thái</option>
        <option value="PENDING">Chờ xử lý</option>
        <option value="PROCESSED">Đã xử lý</option>
        <option value="DISCARDED">Đã hủy</option>
        <option value="REPAIRED">Đã sửa chữa</option>
      </select>
      <button class="btn btn-primary" @click="showBatchProcess = true" :disabled="selectedIds.length === 0">
        <i class="fas fa-tasks"></i>
        Xử lý hàng loạt ({{ selectedIds.length }})
      </button>
    </div>

    <!-- Bảng danh sách -->
    <div class="table-container">
      <div v-if="loading" class="loading-text">Đang tải dữ liệu...</div>
      
      <table v-else>
        <thead>
          <tr>
            <th style="width: 40px;">
              <input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
            </th>
            <th>Mã SP</th>
            <th>Tên sản phẩm</th>
            <th>Phân loại</th>
            <th>Số lượng</th>
            <th>Đơn giá</th>
            <th>Thành tiền</th>
            <th>Trạng thái</th>
            <th>Ngày tạo</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredItems" :key="item.damagedProductId">
            <td>
              <input type="checkbox" :value="item.damagedProductId" v-model="selectedIds" />
            </td>
            <td>
              <span class="sku-code">{{ item.productDetailId }}</span>
            </td>
            <td>
              <div class="product-name">{{ item.productName || 'Sản phẩm' }}</div>
              <div class="sub-text">SKU: {{ item.sku || 'N/A' }}</div>
            </td>
            <td>
              <span class="variant-label">{{ item.colorName || 'N/A' }} / {{ item.sizeName || 'N/A' }}</span>
            </td>
            <td>{{ item.quantity }}</td>
            <td class="price">{{ formatPrice(item.unitPrice) }}</td>
            <td class="price">{{ formatPrice(item.totalAmount) }}</td>
            <td>
              <span :class="['badge', getStatusBadge(item.status)]">
                {{ getStatusText(item.status) }}
              </span>
            </td>
            <td>{{ formatDate(item.createdAt) }}</td>
            <td>
              <div class="action-buttons">
                <button class="action-btn view-btn" title="Xem chi tiết" @click="viewDetail(item.damagedProductId)">
                  <i class="fas fa-eye"></i>
                </button>
                <button 
                  v-if="item.status === 'PENDING'"
                  class="action-btn process-btn" 
                  title="Xử lý" 
                  @click="openProcessModal(item)"
                >
                  <i class="fas fa-check"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredItems.length === 0">
            <td colspan="10" style="text-align: center; padding: 30px; color: #999;">
              Không tìm thấy sản phẩm hỏng nào.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal xử lý sản phẩm hỏng -->
    <transition name="modal-fade">
      <div class="modal-overlay" v-if="showProcessModal || showBatchProcess">
        <div class="modal-wrapper" @click.self="closeModals">
          <div class="modal-container">
            <div class="modal-header">
              <h3>
                <i class="fas fa-tools"></i>
                {{ showBatchProcess ? 'Xử lý hàng loạt' : 'Xử lý sản phẩm hỏng' }}
              </h3>
              <button class="close-btn" @click="closeModals">&times;</button>
            </div>
            <div class="modal-body">
              <div v-if="showBatchProcess" class="batch-info">
                <p>Đang chọn <strong>{{ selectedIds.length }}</strong> sản phẩm để xử lý:</p>
                <div class="batch-list">
                  <span v-for="id in selectedIds.slice(0, 10)" :key="id" class="batch-item">#{{ id }}</span>
                  <span v-if="selectedIds.length > 10" class="batch-item-more">+{{ selectedIds.length - 10 }} sản phẩm khác</span>
                </div>
              </div>

              <div class="form-group">
                <label>Trạng thái mới <span class="required">*</span></label>
                <select v-model="processStatus">
                  <option value="PROCESSED">Đã xử lý</option>
                  <option value="DISCARDED">Đã hủy</option>
                  <option value="REPAIRED">Đã sửa chữa</option>
                </select>
              </div>
              <div class="form-group">
                <label>Ghi chú</label>
                <textarea v-model="processNotes" rows="3" placeholder="Nhập ghi chú xử lý..."></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="closeModals">Hủy</button>
              <button class="btn btn-primary" @click="submitProcess" :disabled="processing">
                <i v-if="!processing" class="fas fa-check"></i>
                <i v-else class="fas fa-spinner fa-spin"></i>
                {{ processing ? 'Đang xử lý...' : 'Xác nhận' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- Modal chi tiết -->
    <transition name="modal-fade">
      <div class="modal-overlay" v-if="showDetailModal">
        <div class="modal-wrapper" @click.self="closeDetailModal">
          <div class="modal-container modal-lg">
            <div class="modal-header">
              <h3><i class="fas fa-box"></i> Chi tiết sản phẩm hỏng</h3>
              <button class="close-btn" @click="closeDetailModal">&times;</button>
            </div>
            <div class="modal-body" v-if="selectedItem">
              <div class="detail-grid">
                <div class="detail-row">
                  <label>Mã sản phẩm:</label>
                  <span>{{ selectedItem.productDetailId }}</span>
                </div>
                <div class="detail-row">
                  <label>Tên sản phẩm:</label>
                  <span>{{ selectedItem.productName || 'Sản phẩm' }}</span>
                </div>
                <div class="detail-row">
                  <label>Phân loại:</label>
                  <span>{{ selectedItem.colorName || 'N/A' }} - {{ selectedItem.sizeName || 'N/A' }}</span>
                </div>
                <div class="detail-row">
                  <label>SKU:</label>
                  <span>{{ selectedItem.sku || 'N/A' }}</span>
                </div>
                <div class="detail-row">
                  <label>Số lượng:</label>
                  <span>{{ selectedItem.quantity }}</span>
                </div>
                <div class="detail-row">
                  <label>Đơn giá:</label>
                  <span class="price">{{ formatPrice(selectedItem.unitPrice) }}</span>
                </div>
                <div class="detail-row">
                  <label>Thành tiền:</label>
                  <span class="price">{{ formatPrice(selectedItem.totalAmount) }}</span>
                </div>
                <div class="detail-row">
                  <label>Trạng thái:</label>
                  <span :class="['badge', getStatusBadge(selectedItem.status)]">
                    {{ getStatusText(selectedItem.status) }}
                  </span>
                </div>
                <div class="detail-row">
                  <label>Mã phiếu đổi/trả:</label>
                  <span>{{ selectedItem.returnExchangeId || 'N/A' }}</span>
                </div>
                <div class="detail-row">
                  <label>Lý do:</label>
                  <span>{{ selectedItem.reason || 'N/A' }}</span>
                </div>
                <div class="detail-row">
                  <label>Ghi chú:</label>
                  <span>{{ selectedItem.notes || 'Không có' }}</span>
                </div>
                <div class="detail-row">
                  <label>Ngày tạo:</label>
                  <span>{{ formatDate(selectedItem.createdAt) }}</span>
                </div>
                <div class="detail-row" v-if="selectedItem.processedAt">
                  <label>Ngày xử lý:</label>
                  <span>{{ formatDate(selectedItem.processedAt) }}</span>
                </div>
                <div class="detail-row" v-if="selectedItem.processedByName">
                  <label>Người xử lý:</label>
                  <span>{{ selectedItem.processedByName }}</span>
                </div>
              </div>

              <div class="action-buttons" v-if="selectedItem.status === 'PENDING'">
                <button class="btn btn-success" @click="processSingleItem(selectedItem.damagedProductId)">
                  <i class="fas fa-check"></i> Xử lý ngay
                </button>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="closeDetailModal">Đóng</button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useNotification } from '../../composables/useNotification';
import { formatPrice, formatDate } from '../../utils/helpers';
import damagedProductService from '../../services/damagedProductService';

const router = useRouter();
const { showNotification } = useNotification();

// State
const loading = ref(false);
const processing = ref(false);
const searchKeyword = ref("");
const statusFilter = ref("all");
const damagedProducts = ref([]);
const stats = ref(null);
const selectedIds = ref([]);
const selectAll = ref(false);

// Modal states
const showProcessModal = ref(false);
const showBatchProcess = ref(false);
const showDetailModal = ref(false);
const selectedItem = ref(null);
const processStatus = ref("PROCESSED");
const processNotes = ref("");

// Computed
const filteredItems = computed(() => {
  let items = damagedProducts.value;
  
  // Filter by status
  if (statusFilter.value !== "all") {
    items = items.filter(item => item.status === statusFilter.value);
  }
  
  // Filter by search keyword
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase().trim();
    items = items.filter(item => 
      String(item.productDetailId).includes(keyword) ||
      (item.productName && item.productName.toLowerCase().includes(keyword)) ||
      (item.sku && item.sku.toLowerCase().includes(keyword))
    );
  }
  
  return items;
});

// Methods
const loadData = async () => {
  loading.value = true;
  try {
    const [productsRes, statsRes] = await Promise.all([
      statusFilter.value === "all" 
        ? damagedProductService.getAllDamagedProducts()
        : damagedProductService.getDamagedProductsByStatus(statusFilter.value),
      damagedProductService.getDamagedProductStats()
    ]);
    
    damagedProducts.value = productsRes?.data?.data || productsRes?.data || [];
    stats.value = statsRes?.data?.data || statsRes?.data;
    
    // Reset selection
    selectedIds.value = [];
    selectAll.value = false;
  } catch (error) {
    console.error("Load damaged products error:", error);
    showNotification("Không thể tải dữ liệu sản phẩm hỏng", "error");
  } finally {
    loading.value = false;
  }
};

const refreshData = () => {
  loadData();
};

const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedIds.value = filteredItems.map(item => item.damagedProductId);
  } else {
    selectedIds.value = [];
  }
};

const viewDetail = async (id) => {
  try {
    const response = await damagedProductService.getDamagedProductById(id);
    selectedItem.value = response?.data?.data || response?.data;
    showDetailModal.value = true;
  } catch (error) {
    showNotification("Không thể tải chi tiết sản phẩm", "error");
  }
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedItem.value = null;
};

const openProcessModal = (item) => {
  selectedItem.value = item;
  processStatus.value = "PROCESSED";
  processNotes.value = "";
  showProcessModal.value = true;
};

const closeModals = () => {
  showProcessModal.value = false;
  showBatchProcess.value = false;
  processStatus.value = "PROCESSED";
  processNotes.value = "";
};

const processSingleItem = async (id) => {
  const item = damagedProducts.value.find(p => p.damagedProductId === id);
  if (!item) return;
  
  selectedItem.value = item;
  processStatus.value = "PROCESSED";
  processNotes.value = "";
  showDetailModal.value = false;
  showProcessModal.value = true;
};

const submitProcess = async () => {
  if (!processStatus.value) {
    showNotification("Vui lòng chọn trạng thái xử lý", "warning");
    return;
  }
  
  processing.value = true;
  try {
    let response;
    
    if (showBatchProcess.value) {
      // Batch process
      response = await damagedProductService.batchUpdateDamagedProducts(
        selectedIds.value,
        processStatus.value,
        processNotes.value
      );
      showNotification(`Đã xử lý ${selectedIds.value.length} sản phẩm hỏng thành công!`, "success");
    } else {
      // Single process
      const id = selectedItem.value.damagedProductId;
      response = await damagedProductService.updateDamagedProductStatus(
        id,
        processStatus.value,
        processNotes.value
      );
      showNotification(`Đã xử lý sản phẩm hỏng #${id} thành công!`, "success");
    }
    
    closeModals();
    await loadData();
  } catch (error) {
    console.error("Process damaged product error:", error);
    showNotification(error.response?.data?.message || "Không thể xử lý sản phẩm hỏng", "error");
  } finally {
    processing.value = false;
  }
};

const getStatusBadge = (status) => {
  const map = {
    'PENDING': 'badge-warning',
    'PROCESSED': 'badge-success',
    'DISCARDED': 'badge-danger',
    'REPAIRED': 'badge-info'
  };
  return map[status] || 'badge-secondary';
};

const getStatusText = (status) => {
  const map = {
    'PENDING': 'Chờ xử lý',
    'PROCESSED': 'Đã xử lý',
    'DISCARDED': 'Đã hủy',
    'REPAIRED': 'Đã sửa chữa'
  };
  return map[status] || status;
};

// Lifecycle
onMounted(() => {
  loadData();
});
</script>

<style scoped>
.staff-page {
  animation: fadeIn 0.3s;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  color: #1a1a2e;
  font-size: 22px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header h2 i {
  color: #e63946;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.filters-bar {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: var(--shadow);
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.search-box {
  flex: 1;
  min-width: 260px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--light-gray);
  border-radius: 8px;
  padding: 0 14px;
}

.search-box input {
  border: none;
  background: transparent;
  outline: none;
  padding: 12px 0;
  width: 100%;
}

.filters-bar select {
  padding: 12px 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  background: white;
}

.btn {
  padding: 10px 22px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.25s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #e63946, #d62828);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(230, 57, 70, 0.35);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  background: transparent;
  border: 1.5px solid #ddd;
  color: #555;
}

.btn-outline:hover {
  background: #f5f5f5;
  border-color: #ccc;
}

.btn-secondary {
  background: #f0f0f2;
  color: #555;
}

.btn-secondary:hover {
  background: #e5e5e8;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover {
  background: #218838;
}

/* Stats */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

/* Table */
.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f8f9fb;
}

th, td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #eee;
  font-size: 14px;
}

th {
  color: #666;
  font-weight: 600;
}

.price {
  color: #e63946;
  font-weight: 700;
}

.sku-code {
  font-family: monospace;
  font-size: 13px;
  color: #666;
}

.product-name {
  font-weight: 600;
  color: #1a1a2e;
}

.sub-text {
  color: #999;
  font-size: 12px;
}

.variant-label {
  font-size: 12px;
  color: #666;
  background: #f0f0f2;
  padding: 2px 10px;
  border-radius: 4px;
}

.badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.badge-success { background: #d4edda; color: #155724; }
.badge-warning { background: #fff3cd; color: #856404; }
.badge-danger { background: #f8d7da; color: #721c24; }
.badge-info { background: #d1ecf1; color: #0c5460; }
.badge-secondary { background: #e2e3e5; color: #383d41; }

.action-buttons {
  display: flex;
  gap: 6px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.view-btn { background: #17a2b8; }
.process-btn { background: #28a745; }

.action-btn:hover {
  transform: scale(1.05);
}

.loading-text {
  text-align: center;
  padding: 40px;
  color: #999;
}

/* Modal */
.modal-overlay {
  position: fixed;
  z-index: 9999;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-container {
  width: 90%;
  max-width: 500px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.modal-lg {
  max-width: 700px;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  color: #1a1a2e;
  display: flex;
  align-items: center;
  gap: 10px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.close-btn:hover {
  color: #666;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #444;
  font-size: 14px;
  margin-bottom: 6px;
}

.form-group .required {
  color: #e63946;
}

.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px 14px;
  border: 2px solid #e8e8ef;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: all 0.25s ease;
}

.form-group select:focus,
.form-group textarea:focus {
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230,57,70,0.08);
}

.form-group textarea {
  resize: vertical;
}

.batch-info {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.batch-info p {
  margin: 0 0 8px 0;
  color: #555;
}

.batch-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.batch-item {
  background: #e8e8ef;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-family: monospace;
  color: #555;
}

.batch-item-more {
  font-size: 12px;
  color: #999;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.detail-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-row label {
  font-weight: 600;
  color: #666;
  font-size: 12px;
}

.detail-row span {
  color: #1a1a2e;
  font-size: 14px;
}

.detail-row .price {
  font-weight: 700;
}

/* Modal transition */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
  
  .filters-bar {
    flex-direction: column;
  }
}
</style>