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
      <!-- ===== TAB BAR ===== -->
      <div class="main-tabs">
        <button
          v-for="tab in mainTabs"
          :key="tab.value"
          :class="['main-tab-btn', { active: activeMainTab === tab.value }]"
          @click="onMainTabChange(tab.value)"
        >
          {{ tab.icon }} {{ tab.label }}
        </button>
      </div>

      <!-- ===== TAB: TỔNG QUAN ===== -->
      <template v-if="activeMainTab === 'overview'">
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
            <div class="stat-label">Chờ xác nhận</div>
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
          <div class="revenue-card" :title="formatPriceFull(orderStats?.todayRevenue || 0)">
            <div class="revenue-label">Hôm nay</div>
            <div class="revenue-value">{{ formatPrice(orderStats?.todayRevenue || 0) }}</div>
          </div>
          <div class="revenue-card" :title="formatPriceFull(orderStats?.monthlyRevenue || 0)">
            <div class="revenue-label">Tháng này</div>
            <div class="revenue-value">{{ formatPrice(orderStats?.monthlyRevenue || 0) }}</div>
          </div>
          <div
            class="revenue-card highlight"
            :title="`📊 Doanh thu thực tế (đã trừ hoàn tiền): ${formatPriceFull(orderStats?.netRevenue || 0)}`"
          >
            <div class="revenue-label">💰 Tổng doanh thu</div>
            <div class="revenue-value total">{{ formatPrice(orderStats?.totalRevenue || 0) }}</div>
          </div>
          <div class="revenue-card" :title="formatPriceFull(orderStats?.onlineRevenue || 0)">
            <div class="revenue-label">🛒 Doanh thu online</div>
            <div class="revenue-value">{{ formatPrice(orderStats?.onlineRevenue || 0) }}</div>
          </div>
          <div class="revenue-card" :title="formatPriceFull(posStats?.posRevenue || 0)">
            <div class="revenue-label">🏪 Doanh thu POS</div>
            <div class="revenue-value">{{ formatPrice(posStats?.posRevenue || 0) }}</div>
          </div>
          <div class="revenue-card" :title="formatPriceFull(orderStats?.returnRefundAmount || 0)">
            <div class="revenue-label">↩️ Tiền hoàn trả</div>
            <div class="revenue-value return">{{ formatPrice(orderStats?.returnRefundAmount || 0) }}</div>
          </div>
        </div>
      </div>

      <!-- ===== 3. THỐNG KÊ BÁN TẠI QUẦY (POS) ===== -->
      <div class="pos-section">
        <div class="section-header-row">
          <h3>🏪 Thống kê bán tại quầy (POS)</h3>
          <div class="revenue-time-controls">
            <select v-model="posTimeType" @change="onPosTimeTypeChange" class="time-select">
              <option value="day">Theo ngày</option>
              <option value="month">Theo tháng</option>
              <option value="year">Theo năm</option>
            </select>

            <!-- Chọn Ngày -->
            <input
              v-if="posTimeType === 'day'"
              type="date"
              v-model="posDayValue"
              @change="fetchPosStats"
              class="time-select"
            />

            <!-- Chọn Tháng & Năm -->
            <template v-if="posTimeType === 'month'">
              <select v-model.number="posMonthValue" @change="fetchPosStats" class="time-select">
                <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
              </select>
              <select v-model.number="posYearValue" @change="fetchPosStats" class="time-select">
                <option v-for="y in availableYears" :key="y" :value="y">Năm {{ y }}</option>
              </select>
            </template>

            <!-- Chọn Năm -->
            <select
              v-if="posTimeType === 'year'"
              v-model.number="posYearValue"
              @change="fetchPosStats"
              class="time-select"
            >
              <option v-for="y in availableYears" :key="y" :value="y">Năm {{ y }}</option>
            </select>

            <button class="btn-refresh btn-small" @click="fetchPosStats" :disabled="posLoading">
              <i class="fas fa-sync-alt" :class="{ spinning: posLoading }"></i> Xem
            </button>
          </div>
        </div>

        <div v-if="posLoading" class="chart-loading" style="height: 120px;">Đang tải dữ liệu...</div>
        <template v-else>
          <div class="revenue-grid pos-grid">
            <div class="revenue-card highlight" :title="formatPriceFull(posStats.posRevenue)">
              <div class="revenue-label">🏪 Doanh thu tại quầy</div>
              <div class="revenue-value total">{{ formatPrice(posStats.posRevenue) }}</div>
            </div>
            <div class="revenue-card">
              <div class="revenue-label">🧾 Số đơn tại quầy</div>
              <div class="revenue-value">{{ posStats.posOrders || 0 }}</div>
            </div>
          </div>
        </template>
      </div>

      <!-- ===== 4. THỐNG KÊ HOÀN HÀNG ===== -->
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

      <!-- ===== 5. THỐNG KÊ HỦY ĐƠN ===== -->
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

      <!-- ===== 6. THỐNG KÊ DOANH THU THEO THỜI GIAN ===== -->
      <div class="revenue-time-section">
        <div class="section-header-row">
          <h3>📈 Thống kê doanh thu theo thời gian</h3>
          <div class="revenue-time-controls">
            <select v-model="revenueTimeType" @change="fetchRevenueByTime" class="time-select">
              <option value="monthly">Theo tháng (trong năm)</option>
              <option value="daily">Theo ngày (trong tháng)</option>
            </select>
            <select v-model.number="revenueYear" @change="fetchRevenueByTime" class="time-select">
              <option v-for="y in availableYears" :key="y" :value="y">Năm {{ y }}</option>
            </select>
            <select v-if="revenueTimeType === 'daily'" v-model.number="revenueMonth" @change="fetchRevenueByTime" class="time-select">
              <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
            </select>
          </div>
        </div>
        <div class="chart-wrapper">
          <div v-if="revenueTimeLoading" class="chart-loading">Đang tải biểu đồ...</div>
          <Line v-else-if="revenueChartData" :data="revenueChartData" :options="revenueChartOptions" />
        </div>
      </div>

      <!-- ===== 7. THỐNG KÊ THEO TRẠNG THÁI ĐƠN HÀNG ===== -->
      <div class="status-stats-section">
        <h3>📊 Thống kê theo trạng thái đơn hàng</h3>
        <div class="status-stats-body">
          <div class="chart-wrapper status-chart-wrapper">
            <Doughnut v-if="statusChartData" :data="statusChartData" :options="statusChartOptions" />
          </div>
          <div class="status-legend">
            <div
              v-for="(count, status) in statusCounts"
              :key="status"
              class="status-legend-item"
              @click="navigateToOrders(status)"
            >
              <span :class="['status-dot', getStatusClass(status)]"></span>
              <span class="status-legend-label">{{ getStatusText(status) }}</span>
              <span class="status-legend-count">{{ count }}</span>
            </div>
          </div>
        </div>
      </div>
      </template>

      <!-- ===== TAB: NHÂN VIÊN (xếp hạng theo doanh thu) ===== -->
      <template v-else-if="activeMainTab === 'staff'">
        <div class="revenue-time-section">
          <div class="section-header-row">
            <h3>👤 Xếp hạng nhân viên theo doanh thu</h3>
            <div class="revenue-time-controls">
              <select v-model="staffTimeFilter" class="time-select" @change="onStaffTimeFilterChange">
                <option value="all">Tất cả thời gian</option>
                <option value="today">Hôm nay</option>
                <option value="month">Tháng này</option>
                <option value="year">Năm nay</option>
              </select>
            </div>
          </div>

          <div v-if="staffLoading" class="chart-loading" style="height: 120px;">Đang tải dữ liệu...</div>
          <div v-else-if="!topStaff.length" class="chart-loading" style="height: 120px;">Không có dữ liệu trong khoảng thời gian này</div>
          <template v-else>
            <table class="staff-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Nhân viên</th>
                  <th>Vai trò</th>
                  <th>Số đơn</th>
                  <th>Doanh thu</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(staff, idx) in paginatedStaff" :key="staff.staffId">
                  <td>
                    <span class="rank-badge" :class="{ 'rank-top': (staffCurrentPage - 1) * staffPageSize + idx < 3 }">
                      {{ (staffCurrentPage - 1) * staffPageSize + idx + 1 }}
                    </span>
                  </td>
                  <td class="staff-name-cell">
                    {{ staff.staffName }}
                  </td>
                  <td>{{ staff.staffRole }}</td>
                  <td>{{ staff.totalOrders }}</td>
                  <td class="staff-revenue-cell" :title="formatPriceFull(staff.totalRevenue)">{{ formatPrice(staff.totalRevenue) }}</td>
                </tr>
              </tbody>
            </table>

            <!-- ===== PHÂN TRANG NHÂN VIÊN ===== -->
            <div v-if="topStaff.length > 0" class="pagination-wrapper">
              <div class="pagination-info">
                Hiển thị {{ (staffCurrentPage - 1) * staffPageSize + 1 }} - {{ Math.min(staffCurrentPage * staffPageSize, topStaff.length) }} trên tổng số {{ topStaff.length }} nhân viên
              </div>
              <div class="pagination-controls">
                <div class="page-size-selector">
                  <label>Hiển thị:</label>
                  <select v-model.number="staffPageSize" @change="staffCurrentPage = 1" class="page-size-select">
                    <option :value="5">5 / trang</option>
                    <option :value="10">10 / trang</option>
                    <option :value="20">20 / trang</option>
                    <option :value="50">50 / trang</option>
                  </select>
                </div>

                <div class="page-buttons">
                  <button class="page-btn" :disabled="staffCurrentPage === 1" @click="staffCurrentPage--" title="Trang trước">
                    <i class="fas fa-chevron-left"></i>
                  </button>
                  <button
                    v-for="p in visibleStaffPages"
                    :key="p"
                    :class="['page-btn', { active: staffCurrentPage === p }]"
                    @click="staffCurrentPage = p"
                  >
                    {{ p }}
                  </button>
                  <button class="page-btn" :disabled="staffCurrentPage === totalStaffPages" @click="staffCurrentPage++" title="Trang sau">
                    <i class="fas fa-chevron-right"></i>
                  </button>
                </div>
              </div>
            </div>
          </template>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import api from "@/api/axios";
import { Line, Doughnut } from 'vue-chartjs';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  Tooltip,
  Legend,
  Filler
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, ArcElement, Tooltip, Legend, Filler);

const router = useRouter();
// ===== STATE =====
const loading = ref(false);
const error = ref(null);
const orderStats = ref(null);
const statusCounts = ref({});

// ===== TAB =====
const mainTabs = [
  { value: 'overview', label: 'Tổng quan', icon: '📊' },
  { value: 'staff', label: 'Nhân viên', icon: '👤' },
];
const activeMainTab = ref('overview');
const onMainTabChange = (tab) => {
  activeMainTab.value = tab;
  if (tab === 'staff' && topStaff.value.length === 0) fetchTopStaff();
};

// ===== NHÂN VIÊN (xếp hạng theo doanh thu & phân trang) =====
const staffLoading = ref(false);
const topStaff = ref([]);
const staffTimeFilter = ref('all'); // 'all' | 'today' | 'month' | 'year'
const staffFromDate = ref('2000-01-01');
const staffToDate = ref('2099-12-31');

// State phân trang
const staffCurrentPage = ref(1);
const staffPageSize = ref(10);

const totalStaffPages = computed(() => {
  if (!topStaff.value || topStaff.value.length === 0) return 1;
  return Math.ceil(topStaff.value.length / staffPageSize.value);
});

const paginatedStaff = computed(() => {
  if (!topStaff.value) return [];
  const start = (staffCurrentPage.value - 1) * staffPageSize.value;
  const end = start + staffPageSize.value;
  return topStaff.value.slice(start, end);
});

const visibleStaffPages = computed(() => {
  const total = totalStaffPages.value;
  const current = staffCurrentPage.value;
  const pages = [];
  let start = Math.max(1, current - 2);
  let end = Math.min(total, start + 4);
  if (end - start < 4) {
    start = Math.max(1, end - 4);
  }
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  return pages;
});

const onStaffTimeFilterChange = () => {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');

  if (staffTimeFilter.value === 'today') {
    staffFromDate.value = `${y}-${m}-${d}`;
    staffToDate.value = `${y}-${m}-${d}`;
  } else if (staffTimeFilter.value === 'month') {
    const lastDay = new Date(y, now.getMonth() + 1, 0).getDate();
    const lastDayStr = String(lastDay).padStart(2, '0');
    staffFromDate.value = `${y}-${m}-01`;
    staffToDate.value = `${y}-${m}-${lastDayStr}`;
  } else if (staffTimeFilter.value === 'year') {
    staffFromDate.value = `${y}-01-01`;
    staffToDate.value = `${y}-12-31`;
  } else {
    staffFromDate.value = '2000-01-01';
    staffToDate.value = '2099-12-31';
  }
  staffCurrentPage.value = 1;
  fetchTopStaff();
};

const fetchTopStaff = async () => {
  staffLoading.value = true;
  try {
    const response = await api.get('/admin/statistics/top-staff', {
      params: { fromDate: staffFromDate.value, toDate: staffToDate.value, limit: 1000 }
    });
    topStaff.value = response.data?.data || [];
    staffCurrentPage.value = 1;
  } catch (err) {
    console.error('❌ Error fetching top staff:', err);
    topStaff.value = [];
  } finally {
    staffLoading.value = false;
  }
};

// ===== BÁN TẠI QUẦY (POS) =====
const posLoading = ref(false);
const posStats = ref({ posRevenue: 0, posOrders: 0 });
const posDailyStats = ref([]);
const posTopStaff = ref([]);
const posTimeType = ref('month'); // 'day' | 'month' | 'year'
const todayStr = new Date().toISOString().slice(0, 10);
const posDayValue = ref(todayStr);
const posMonthValue = ref(new Date().getMonth() + 1);
const posYearValue = ref(new Date().getFullYear());

const getPosDateRange = () => {
  let fromDate = '';
  let toDate = '';

  if (posTimeType.value === 'day') {
    fromDate = posDayValue.value || todayStr;
    toDate = posDayValue.value || todayStr;
  } else if (posTimeType.value === 'month') {
    const y = posYearValue.value || new Date().getFullYear();
    const m = posMonthValue.value || (new Date().getMonth() + 1);
    const mStr = String(m).padStart(2, '0');
    const lastDay = new Date(y, m, 0).getDate();
    const lastDayStr = String(lastDay).padStart(2, '0');
    fromDate = `${y}-${mStr}-01`;
    toDate = `${y}-${mStr}-${lastDayStr}`;
  } else if (posTimeType.value === 'year') {
    const y = posYearValue.value || new Date().getFullYear();
    fromDate = `${y}-01-01`;
    toDate = `${y}-12-31`;
  }

  return { fromDate, toDate };
};

const onPosTimeTypeChange = () => {
  fetchPosStats();
};

const fetchPosStats = async () => {
  posLoading.value = true;
  try {
    const { fromDate, toDate } = getPosDateRange();
    const [resSummary, resDaily, resTopStaff] = await Promise.allSettled([
      api.get('/admin/statistics/revenue', {
        params: { fromDate, toDate }
      }),
      api.get('/admin/statistics/daily-revenue', {
        params: { fromDate, toDate }
      }),
      api.get('/admin/statistics/top-staff', {
        params: { fromDate, toDate, limit: 10 }
      })
    ]);
    
    posStats.value = resSummary.status === 'fulfilled' && resSummary.value.data?.data
      ? resSummary.value.data.data
      : { posRevenue: 0, posOrders: 0 };

    posDailyStats.value = resDaily.status === 'fulfilled' && resDaily.value.data?.data
      ? resDaily.value.data.data
      : [];

    posTopStaff.value = resTopStaff.status === 'fulfilled' && resTopStaff.value.data?.data
      ? resTopStaff.value.data.data
      : [];
  } catch (err) {
    console.error('❌ Error fetching POS stats:', err);
    posStats.value = { posRevenue: 0, posOrders: 0 };
    posDailyStats.value = [];
    posTopStaff.value = [];
  } finally {
    posLoading.value = false;
  }
};

const posChartTitle = computed(() => {
  if (posTimeType.value === 'day') {
    const parts = (posDayValue.value || '').split('-');
    return parts.length === 3 ? `📈 Doanh thu POS ngày ${parts[2]}/${parts[1]}/${parts[0]}` : '📈 Doanh thu POS ngày';
  } else if (posTimeType.value === 'month') {
    return `📈 Biểu đồ doanh thu POS - Tháng ${posMonthValue.value}/${posYearValue.value}`;
  } else {
    return `📈 Biểu đồ doanh thu POS - Năm ${posYearValue.value}`;
  }
});

const posChartData = computed(() => {
  if (!posDailyStats.value || posDailyStats.value.length === 0) return null;
  return {
    labels: posDailyStats.value.map(d => {
      if (!d.date) return '';
      const parts = d.date.split('-');
      return parts.length === 3 ? `${parts[2]}/${parts[1]}` : d.date;
    }),
    datasets: [
      {
        label: 'Doanh thu POS (tại quầy)',
        data: posDailyStats.value.map(d => Number(d.posRevenue) || 0),
        borderColor: '#e63946',
        backgroundColor: 'rgba(230, 57, 70, 0.15)',
        tension: 0.35,
        fill: true,
        pointRadius: 4,
        pointBackgroundColor: '#e63946'
      }
    ]
  };
});

const posChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: true, position: 'top' },
    tooltip: {
      callbacks: {
        label: (ctx) => `${ctx.dataset.label}: ${Number(ctx.raw).toLocaleString('vi-VN')}₫`
      }
    }
  },
  scales: {
    y: {
      beginAtZero: true,
      ticks: {
        callback: (value) => Number(value).toLocaleString('vi-VN') + '₫'
      }
    }
  }
};

// ===== DOANH THU THEO THỜI GIAN =====
const revenueTimeLoading = ref(false);
const revenueTimeType = ref('monthly'); // 'monthly' | 'daily'
const currentYear = new Date().getFullYear();
const revenueYear = ref(currentYear);
const revenueMonth = ref(new Date().getMonth() + 1);
const availableYears = computed(() => {
  const years = [];
  for (let y = currentYear; y >= currentYear - 4; y--) years.push(y);
  return years;
});
const revenueByTime = ref([]);

// ===== HELPERS =====
const formatPriceFull = (value) => {
  if (!value && value !== 0) return '0₫';
  const num = Number(value);
  if (isNaN(num)) return '0₫';
  return num.toLocaleString('vi-VN') + '₫';
};

const formatPrice = (value) => {
  if (!value && value !== 0) return '0₫';
  const num = Number(value);
  if (isNaN(num)) return '0₫';
  if (Math.abs(num) >= 1_000_000_000) {
    const bill = num / 1_000_000_000;
    return bill.toLocaleString('vi-VN', { maximumFractionDigits: 2 }) + ' tỷ ₫';
  }
  return num.toLocaleString('vi-VN') + '₫';
};

const getStatusText = (status) => {
  const map = {
    'PENDING_PAYMENT': 'Chờ xác nhận',
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

const navigateToEmployees = (staffName) => {
  let query = {};
  if (staffName) {
    query.search = staffName;
  }
  router.push({ path: '/admin/employees', query });
};

const navigateToOrdersByStaff = (staffName) => {
  let query = {};
  if (staffName) {
    query.search = staffName;
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

const fetchRevenueByTime = async () => {
  revenueTimeLoading.value = true;
  try {
    const params = { type: revenueTimeType.value, year: revenueYear.value };
    if (revenueTimeType.value === 'daily') {
      params.month = revenueMonth.value;
    }
    const response = await api.get(`/admin/orders/statistics/revenue-by-time`, { params });
    revenueByTime.value = response.data || [];
  } catch (err) {
    console.error('❌ Error fetching revenue by time:', err);
    revenueByTime.value = [];
  } finally {
    revenueTimeLoading.value = false;
  }
};

const fetchAllData = async () => {
  loading.value = true;
  error.value = null;
  try {
    await Promise.all([
      fetchOrderStats(),
      fetchStatusCounts(),
      fetchRevenueByTime(),
      fetchPosStats(),
      fetchTopStaff()
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

// ===== CHART: DOANH THU THEO THỜI GIAN =====
const revenueChartData = computed(() => {
  if (!revenueByTime.value || revenueByTime.value.length === 0) return null;
  return {
    labels: revenueByTime.value.map(p => p.label),
    datasets: [
      {
        label: revenueTimeType.value === 'daily' ? `Doanh thu theo ngày - Tháng ${revenueMonth.value}/${revenueYear.value}` : `Doanh thu theo tháng - Năm ${revenueYear.value}`,
        data: revenueByTime.value.map(p => Number(p.revenue) || 0),
        borderColor: '#e63946',
        backgroundColor: 'rgba(230, 57, 70, 0.12)',
        tension: 0.35,
        fill: true,
        pointRadius: 3,
        pointBackgroundColor: '#e63946'
      }
    ]
  };
});

const revenueChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: true, position: 'top' },
    tooltip: {
      callbacks: {
        label: (ctx) => `${ctx.dataset.label}: ${Number(ctx.raw).toLocaleString('vi-VN')}₫`
      }
    }
  },
  scales: {
    y: {
      beginAtZero: true,
      ticks: {
        callback: (value) => Number(value).toLocaleString('vi-VN') + '₫'
      }
    }
  }
};

// ===== CHART: THỐNG KÊ THEO TRẠNG THÁI ĐƠN HÀNG =====
const statusColorMap = {
  'PENDING_PAYMENT': '#ffc107',
  'CONFIRMED': '#3498db',
  'PROCESSING': '#3498db',
  'SHIPPING': '#17a2b8',
  'DELIVERED': '#28a745',
  'COMPLETED': '#28a745',
  'CANCELLED': '#dc3545',
  'RETURN_REQUESTED': '#ffc107',
  'RETURN_APPROVED': '#3498db',
  'RETURN_RECEIVED': '#3498db',
  'RETURN_PENDING': '#17a2b8',
  'RETURNED': '#28a745',
  'REJECTED': '#dc3545'
};

const statusChartData = computed(() => {
  const entries = Object.entries(statusCounts.value || {}).filter(([, count]) => count > 0);
  if (entries.length === 0) return null;
  return {
    labels: entries.map(([status]) => getStatusText(status)),
    datasets: [
      {
        data: entries.map(([, count]) => count),
        backgroundColor: entries.map(([status]) => statusColorMap[status] || '#999'),
        borderWidth: 1
      }
    ]
  };
});

const statusChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (ctx) => `${ctx.label}: ${ctx.raw}`
      }
    }
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

/* ===== MAIN TABS ===== */
.main-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 2px solid #eef0f3;
  flex-wrap: wrap;
}

.main-tab-btn {
  padding: 10px 18px;
  background: transparent;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.main-tab-btn:hover {
  color: #1a1a2e;
}

.main-tab-btn.active {
  color: #e63946;
  border-bottom-color: #e63946;
}

/* ===== HOVER TOOLTIP (revenue card) ===== */
.revenue-card.hoverable {
  cursor: help;
  position: relative;
}

.hover-hint {
  margin-top: 6px;
  font-size: 11px;
  color: rgba(255,255,255,0.75);
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ===== STAFF TABLE ===== */
.btn-small {
  padding: 6px 14px;
  font-size: 13px;
}

.staff-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 12px;
}

.staff-table th {
  text-align: left;
  padding: 10px 12px;
  background: #f8f9fa;
  font-size: 13px;
  color: #6b7280;
  border-bottom: 2px solid #eef0f3;
}

.staff-table td {
  padding: 12px;
  font-size: 13px;
  border-bottom: 1px solid #f1f4f8;
  color: #333;
}

.staff-table tbody tr:hover {
  background: #fafbfc;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #eef0f3;
  color: #555;
  font-weight: 700;
  font-size: 12px;
}

.rank-badge.rank-top {
  background: #ffd166;
  color: #7a4b00;
}

.staff-name-cell {
  font-weight: 600;
  color: #1a1a2e;
}

.hoverable-staff-name {
  cursor: pointer;
  transition: color 0.2s;
}

.hoverable-staff-name:hover {
  color: #3498db;
  text-decoration: underline;
}

.staff-link-icon {
  font-size: 11px;
  margin-left: 4px;
  color: #8899a6;
}

.hoverable-staff-name:hover .staff-link-icon {
  color: #3498db;
}

.staff-revenue-cell {
  font-weight: 700;
  color: #e63946;
}

.btn-manage-staff {
  background: #34495e;
  color: white;
}

.btn-manage-staff:hover {
  background: #1a252f;
}

.sub-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 10px;
}

.action-cell {
  display: flex;
  gap: 6px;
  align-items: center;
}

.btn-action-view {
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 6px;
  border: 1px solid #3498db;
  background: #f0f7ff;
  color: #3498db;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.btn-action-view:hover {
  background: #3498db;
  color: white;
}

.btn-action-orders {
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 6px;
  border: 1px solid #2ecc71;
  background: #e8f8f5;
  color: #27ae60;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.btn-action-orders:hover {
  background: #27ae60;
  color: white;
}

/* ===== CHANNEL COMPARE (POS vs Online) ===== */
.channel-compare {
  margin-top: 20px;
}

.channel-compare-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #444;
  margin-bottom: 6px;
  font-weight: 600;
}

.channel-compare-bar {
  height: 14px;
  border-radius: 7px;
  background: #cce5ff;
  overflow: hidden;
}

.channel-compare-fill.pos {
  height: 100%;
  background: #e63946;
  border-radius: 7px 0 0 7px;
  transition: width 0.5s ease;
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
.revenue-section,
.pos-section {
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

.revenue-grid.pos-grid {
  grid-template-columns: repeat(2, 1fr);
}

.sub-section-title {
  margin: 0 0 14px 0;
  font-size: 15px;
  color: #1a1a2e;
  font-weight: 600;
}

.revenue-card {
  padding: 16px 12px;
  background: #f8f9fa;
  border-radius: 10px;
  text-align: center;
  min-width: 0;
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
  font-size: clamp(14px, 1.2vw, 19px);
  font-weight: 700;
  color: #1a1a2e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

/* ===== REVENUE BY TIME ===== */
.revenue-time-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.section-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.section-header-row h3 {
  margin: 0;
  font-size: 18px;
}

.revenue-time-controls {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.time-select {
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #e2e5ea;
  background: #f8f9fa;
  font-size: 13px;
  color: #333;
  cursor: pointer;
}

.chart-wrapper {
  position: relative;
  height: 320px;
}

.chart-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6b7280;
  font-size: 14px;
}

/* ===== STATUS STATS (chart + legend) ===== */
.status-stats-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.status-stats-section h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 18px;
}

.status-stats-body {
  display: flex;
  gap: 32px;
  align-items: center;
  flex-wrap: wrap;
}

.status-chart-wrapper {
  height: 260px;
  width: 260px;
  flex-shrink: 0;
}

.status-legend {
  flex: 1;
  min-width: 220px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.status-legend-item:hover {
  background: #f8f9fa;
}

.status-legend-label {
  flex: 1;
  font-size: 13px;
  color: #444;
}

.status-legend-count {
  font-size: 13px;
  font-weight: 700;
  color: #1a1a2e;
}

/* ===== STATUS DISTRIBUTION (legacy - không còn dùng trong template) ===== */
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

/* ===== PHÂN TRANG STYLES ===== */
.pagination-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #ffffff;
  border-top: 1px solid #f0f0f0;
  border-radius: 0 0 12px 12px;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}

.pagination-info {
  font-size: 13.5px;
  color: #64748b;
  font-weight: 500;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.page-size-select {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s ease;
}

.page-size-select:focus {
  border-color: #3b82f6;
}

.page-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  padding: 0 8px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #94a3b8;
  color: #0f172a;
}

.page-btn.active {
  background: #1e293b;
  color: #ffffff;
  border-color: #1e293b;
  font-weight: 600;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

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