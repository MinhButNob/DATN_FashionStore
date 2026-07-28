<template>
  <div class="admin-statistics">
    <div class="page-header">
      <h2>📊 Báo cáo thống kê</h2>
      <div class="header-actions">
        <button class="btn-refresh" @click="fetchAllData" :disabled="loading">
          <i class="fas fa-sync-alt" :class="{ spinning: loading }"></i>
          {{ loading ? 'Đang tải...' : 'Làm mới' }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>Đang tải dữ liệu...</span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">⚠️</div>
      <h3>Không thể tải dữ liệu</h3>
      <p>{{ error }}</p>
      <button class="btn-retry" @click="fetchAllData">Thử lại</button>
    </div>

    <template v-else>
      <!-- ===== 1. THỐNG KÊ TỔNG QUAN ===== -->
      <div class="dashboard-cards">
        <div class="stat-card" @click="navigateToOrders()">
          <div class="stat-icon total">📦</div>
          <div class="stat-info">
            <div class="stat-value">{{ orderStats?.totalOrders || 0 }}</div>
            <div class="stat-label">Tổng đơn hàng</div>
          </div>
        </div>
        <div class="stat-card" @click="navigateToOrders('COMPLETED')">
          <div class="stat-icon completed">✅</div>
          <div class="stat-info">
            <div class="stat-value">{{ orderStats?.completedOrders || 0 }}</div>
            <div class="stat-label">Đơn hoàn thành</div>
          </div>
        </div>
        <div class="stat-card" @click="navigateToOrders('PENDING_PAYMENT')">
          <div class="stat-icon pending">⏳</div>
          <div class="stat-info">
            <div class="stat-value">{{ orderStats?.pendingOrders || 0 }}</div>
            <div class="stat-label">Chờ thanh toán</div>
          </div>
        </div>
        <div class="stat-card" @click="navigateToOrders('CANCELLED')">
          <div class="stat-icon cancelled">❌</div>
          <div class="stat-info">
            <div class="stat-value">{{ orderStats?.cancelledOrders || 0 }}</div>
            <div class="stat-label">Đã hủy</div>
          </div>
        </div>
        <div class="stat-card" @click="navigateToOrders('PROCESSING')">
          <div class="stat-icon processing">🔄</div>
          <div class="stat-info">
            <div class="stat-value">{{ orderStats?.processingOrders || 0 }}</div>
            <div class="stat-label">Đang xử lý</div>
          </div>
        </div>
        <div class="stat-card" @click="navigateToOrders('SHIPPING')">
          <div class="stat-icon shipping">🚚</div>
          <div class="stat-info">
            <div class="stat-value">{{ orderStats?.shippingOrders || 0 }}</div>
            <div class="stat-label">Đang giao</div>
          </div>
        </div>
      </div>

      <!-- ===== 2. DOANH THU ===== -->
      <div class="revenue-section">
        <h3>💰 Doanh thu</h3>
        <div class="revenue-grid">
          <div class="revenue-card">
            <div class="revenue-label">Hôm nay</div>
            <div class="revenue-value">{{ formatPrice(orderStats?.todayRevenue || 0) }}</div>
          </div>
          <div class="revenue-card">
            <div class="revenue-label">Tháng này</div>
            <div class="revenue-value">{{ formatPrice(orderStats?.monthlyRevenue || 0) }}</div>
          </div>
          <div class="revenue-card highlight">
            <div class="revenue-label">💰 Tổng doanh thu</div>
            <div class="revenue-value total">{{ formatPrice(orderStats?.totalRevenue || 0) }}</div>
          </div>
          <div class="revenue-card">
            <div class="revenue-label">🛒 Doanh thu online</div>
            <div class="revenue-value">{{ formatPrice(orderStats?.onlineRevenue || 0) }}</div>
          </div>
          <div class="revenue-card">
            <div class="revenue-label">↩️ Tiền hoàn trả</div>
            <div class="revenue-value return">{{ formatPrice(orderStats?.returnRefundAmount || 0) }}</div>
          </div>
          <div class="revenue-card highlight">
            <div class="revenue-label">📊 Doanh thu thực tế</div>
            <div class="revenue-value net">{{ formatPrice(orderStats?.netRevenue || 0) }}</div>
          </div>
        </div>
      </div>

      <!-- ===== 3. THỐNG KÊ HOÀN HÀNG ===== -->
      <div class="return-section">
        <h3>🔄 Thống kê hoàn hàng</h3>
        <div class="return-grid">
          <div class="return-card" @click="navigateToOrders('RETURN_REQUESTED')">
            <span class="return-icon">📋</span>
            <div>
              <div class="return-value">{{ orderStats?.returnRequested || 0 }}</div>
              <div class="return-label">Yêu cầu hoàn</div>
            </div>
          </div>
          <div class="return-card" @click="navigateToOrders('RETURN_APPROVED')">
            <span class="return-icon">✅</span>
            <div>
              <div class="return-value">{{ orderStats?.returnApproved || 0 }}</div>
              <div class="return-label">Đã duyệt</div>
            </div>
          </div>
          <div class="return-card" @click="navigateToOrders('RETURN_RECEIVED')">
            <span class="return-icon">📦</span>
            <div>
              <div class="return-value">{{ orderStats?.returnReceived || 0 }}</div>
              <div class="return-label">Đã nhận hàng</div>
            </div>
          </div>
          <div class="return-card" @click="navigateToOrders('RETURN_PENDING')">
            <span class="return-icon">⏳</span>
            <div>
              <div class="return-value">{{ orderStats?.returnPending || 0 }}</div>
              <div class="return-label">Đang thu hồi</div>
            </div>
          </div>
          <div class="return-card" @click="navigateToOrders('RETURNED')">
            <span class="return-icon">✅</span>
            <div>
              <div class="return-value">{{ orderStats?.returnCompleted || 0 }}</div>
              <div class="return-label">Đã hoàn thành</div>
            </div>
          </div>
          <div class="return-card" @click="navigateToOrders('REJECTED')">
            <span class="return-icon">❌</span>
            <div>
              <div class="return-value">{{ orderStats?.returnRejected || 0 }}</div>
              <div class="return-label">Từ chối</div>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 4. THỐNG KÊ HỦY ĐƠN ===== -->
      <div class="cancel-section">
        <h3>🚫 Thống kê hủy đơn</h3>
        <div class="cancel-grid">
          <div class="cancel-card">
            <span class="cancel-icon">👤</span>
            <div>
              <div class="cancel-value">{{ orderStats?.cancelledByCustomer || 0 }}</div>
              <div class="cancel-label">Khách hàng hủy</div>
            </div>
          </div>
          <div class="cancel-card">
            <span class="cancel-icon">🛡️</span>
            <div>
              <div class="cancel-value">{{ orderStats?.cancelledByAdmin || 0 }}</div>
              <div class="cancel-label">Admin hủy</div>
            </div>
          </div>
          <div class="cancel-card">
            <span class="cancel-icon">🤖</span>
            <div>
              <div class="cancel-value">{{ orderStats?.cancelledBySystem || 0 }}</div>
              <div class="cancel-label">Hệ thống hủy</div>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 5. PHÂN BỔ TRẠNG THÁI ĐƠN HÀNG ===== -->
      <div class="status-distribution-section">
        <h3>📊 Phân bổ trạng thái đơn hàng</h3>
        <div class="status-distribution">
          <div
            v-for="(count, status) in statusCounts"
            :key="status"
            class="status-item"
            @click="navigateToOrders(status)"
          >
            <span class="status-label">
              <span :class="['status-dot', getStatusClass(status)]"></span>
              {{ getStatusText(status) }}
            </span>
            <div class="status-bar-wrapper">
              <div
                class="status-bar"
                :style="{ width: getStatusPercent(status) + '%' }"
                :class="getStatusClass(status)"
              ></div>
            </div>
            <span class="status-count">{{ count }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import api from "@/api/axios";

const router = useRouter();
// ===== STATE =====
const loading = ref(false);
const error = ref(null);
const orderStats = ref(null);
const statusCounts = ref({});

// ===== HELPERS =====
const formatPrice = (value) => {
  if (!value && value !== 0) return '0₫';
  const num = Number(value);
  if (isNaN(num)) return '0₫';
  return num.toLocaleString('vi-VN') + '₫';
};

const getStatusText = (status) => {
  const map = {
    'PENDING_PAYMENT': 'Chờ thanh toán',
    'CONFIRMED': 'Đã xác nhận',
    'PROCESSING': 'Đang xử lý',
    'SHIPPING': 'Đang giao',
    'DELIVERED': 'Đã giao',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy',
    'RETURN_REQUESTED': 'Yêu cầu hoàn hàng',
    'RETURN_APPROVED': 'Đã duyệt hoàn hàng',
    'RETURN_RECEIVED': 'Đã nhận hàng trả',
    'RETURN_PENDING': 'Đang thu hồi hàng',
    'RETURNED': 'Đã hoàn hàng',
    'REJECTED': 'Từ chối hoàn trả'
  };
  return map[status] || status;
};

const getStatusClass = (status) => {
  const map = {
    'PENDING_PAYMENT': 'status-pending',
    'CONFIRMED': 'status-confirmed',
    'PROCESSING': 'status-processing',
    'SHIPPING': 'status-shipping',
    'DELIVERED': 'status-delivered',
    'COMPLETED': 'status-completed',
    'CANCELLED': 'status-cancelled',
    'RETURN_REQUESTED': 'status-return-requested',
    'RETURN_APPROVED': 'status-return-approved',
    'RETURN_RECEIVED': 'status-return-received',
    'RETURN_PENDING': 'status-return-pending',
    'RETURNED': 'status-returned',
    'REJECTED': 'status-rejected'
  };
  return map[status] || '';
};

const getStatusPercent = (status) => {
  const total = orderStats.value?.totalOrders || 1;
  const count = statusCounts.value[status] || 0;
  return Math.min(Math.round((count / total) * 100), 100);
};

// ===== NAVIGATE =====
const navigateToOrders = (status) => {
  let query = {};
  if (status) {
    query.status = status;
  }
  router.push({ path: '/admin/orders', query });
};

// ===== FETCH DATA =====
const fetchOrderStats = async () => {
  try {
    const token = localStorage.getItem('token');
    const response = await api.get(`/admin/orders/statistics`, {});
    if (response.data) {
      orderStats.value = response.data;
      console.log('✅ Order stats loaded:', orderStats.value);
    }
  } catch (err) {
    console.error('❌ Error fetching order stats:', err);
    throw err;
  }
};

const fetchStatusCounts = async () => {
  try {
    const token = localStorage.getItem('token');
    const response = await api.get(`/admin/orders/status/counts`, {});
    if (response.data) {
      statusCounts.value = response.data;
      console.log('✅ Status counts loaded:', statusCounts.value);
    }
  } catch (err) {
    console.error('❌ Error fetching status counts:', err);
    throw err;
  }
};

const fetchAllData = async () => {
  loading.value = true;
  error.value = null;
  try {
    await Promise.all([
      fetchOrderStats(),
      fetchStatusCounts()
    ]);
  } catch (err) {
    if (err.response) {
      error.value = err.response.data?.message || 'Lỗi từ server: ' + err.response.status;
    } else if (err.request) {
      error.value = 'Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.';
    } else {
      error.value = err.message || 'Có lỗi xảy ra khi tải dữ liệu';
    }
  } finally {
    loading.value = false;
  }
};

// ===== LIFECYCLE =====
onMounted(() => {
  console.log('🚀 AdminStatistics mounted');
  fetchAllData();
});
</script>

<style scoped>
.admin-statistics {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

/* ===== HEADER ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #1a1a2e;
}

.header-actions {
  display: flex;
  gap: 15px;
  align-items: center;
  flex-wrap: wrap;
}

.btn-refresh {
  padding: 10px 20px;
  background: #2c3e50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-refresh:hover:not(:disabled) {
  background: #1a252f;
}

.btn-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== LOADING & ERROR ===== */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px;
  background: white;
  border-radius: 12px;
  margin: 20px 0;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #e5e7eb;
  border-top-color: #3498db;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.error-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-state h3 {
  margin: 0 0 8px;
  color: #dc3545;
}

.error-state p {
  color: #6b7280;
  margin-bottom: 20px;
}

.btn-retry {
  padding: 10px 30px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-retry:hover {
  background: #2980b9;
}

/* ===== STATS CARDS ===== */
.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 18px 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.stat-icon {
  font-size: 28px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  flex-shrink: 0;
}

.stat-icon.total { background: #e3f2fd; color: #1976d2; }
.stat-icon.completed { background: #e8f5e9; color: #2e7d32; }
.stat-icon.pending { background: #fff3e0; color: #e65100; }
.stat-icon.cancelled { background: #fce4ec; color: #c62828; }
.stat-icon.processing { background: #e8eaf6; color: #283593; }
.stat-icon.shipping { background: #e0f7fa; color: #00838f; }

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
}

/* ===== REVENUE ===== */
.revenue-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.revenue-section h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 18px;
}

.revenue-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.revenue-card {
  padding: 16px 18px;
  background: #f8f9fa;
  border-radius: 10px;
  text-align: center;
}

.revenue-card.highlight {
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  color: white;
}

.revenue-card.highlight .revenue-value {
  color: #ffd700;
}

.revenue-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.revenue-card.highlight .revenue-label {
  color: rgba(255,255,255,0.7);
}

.revenue-value {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.revenue-value.total {
  color: #e63946;
}

.revenue-value.return {
  color: #e67e22;
}

.revenue-value.net {
  color: #2ecc71;
}

/* ===== RETURN ===== */
.return-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.return-section h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 18px;
}

.return-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.return-card {
  padding: 14px 16px;
  background: #f8f9fa;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.return-card:hover {
  background: #eef2f7;
  transform: translateY(-2px);
}

.return-icon {
  font-size: 24px;
}

.return-value {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.return-label {
  font-size: 12px;
  color: #6b7280;
}

/* ===== CANCEL ===== */
.cancel-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.cancel-section h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 18px;
}

.cancel-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.cancel-card {
  padding: 14px 16px;
  background: #f8f9fa;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.cancel-icon {
  font-size: 24px;
}

.cancel-value {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.cancel-label {
  font-size: 12px;
  color: #6b7280;
}

/* ===== STATUS DISTRIBUTION ===== */
.status-distribution-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.status-distribution-section h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 18px;
}

.status-distribution {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.status-item:hover {
  background: #f8f9fa;
}

.status-label {
  font-size: 13px;
  color: #555;
  min-width: 140px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.status-bar-wrapper {
  flex: 1;
  height: 8px;
  background: #f1f4f8;
  border-radius: 4px;
  overflow: hidden;
}

.status-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.status-count {
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  min-width: 30px;
  text-align: right;
}

/* ===== STATUS COLORS ===== */
.status-pending { background: #fff3cd; }
.status-confirmed { background: #cce5ff; }
.status-processing { background: #cce5ff; }
.status-shipping { background: #d1ecf1; }
.status-delivered { background: #d4edda; }
.status-completed { background: #d4edda; }
.status-cancelled { background: #f8d7da; }
.status-return-requested { background: #fff3cd; }
.status-return-approved { background: #cce5ff; }
.status-return-received { background: #cce5ff; }
.status-return-pending { background: #d1ecf1; }
.status-returned { background: #d4edda; }
.status-rejected { background: #f8d7da; }

.status-dot.status-pending { background: #ffc107; }
.status-dot.status-confirmed { background: #3498db; }
.status-dot.status-processing { background: #3498db; }
.status-dot.status-shipping { background: #17a2b8; }
.status-dot.status-delivered { background: #28a745; }
.status-dot.status-completed { background: #28a745; }
.status-dot.status-cancelled { background: #dc3545; }
.status-dot.status-return-requested { background: #ffc107; }
.status-dot.status-return-approved { background: #3498db; }
.status-dot.status-return-received { background: #3498db; }
.status-dot.status-return-pending { background: #17a2b8; }
.status-dot.status-returned { background: #28a745; }
.status-dot.status-rejected { background: #dc3545; }

/* ===== RESPONSIVE ===== */
@media (max-width: 1200px) {
  .dashboard-cards {
    grid-template-columns: repeat(3, 1fr);
  }
  .revenue-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .return-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .dashboard-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .revenue-grid {
    grid-template-columns: 1fr 1fr;
  }
  .return-grid {
    grid-template-columns: 1fr 1fr;
  }
  .cancel-grid {
    grid-template-columns: 1fr;
  }
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
  .header-actions {
    width: 100%;
  }
  .btn-refresh {
    justify-content: center;
    width: 100%;
  }
  .status-label {
    min-width: 100px;
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .dashboard-cards {
    grid-template-columns: 1fr;
  }
  .revenue-grid {
    grid-template-columns: 1fr;
  }
  .return-grid {
    grid-template-columns: 1fr;
  }
  .revenue-card {
    padding: 12px 14px;
  }
  .revenue-value {
    font-size: 18px;
  }
}
</style>