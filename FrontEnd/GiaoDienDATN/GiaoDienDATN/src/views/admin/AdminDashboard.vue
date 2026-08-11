<!-- src/views/admin/AdminDashboard.vue -->
<template>
  <div class="admin-dashboard-premium">
    <div class="dashboard-header">
      <div class="header-content">
        <h1 class="page-title">Bảng điều khiển (Dashboard)</h1>
        <p class="page-subtitle">Chào mừng trở lại! Dưới đây là tổng quan tình hình kinh doanh của cửa hàng.</p>
      </div>
      <div class="header-actions">
        <button class="btn-premium btn-secondary">
          <i class="fas fa-download mr-2"></i> Xuất báo cáo
        </button>
        <button class="btn-premium btn-primary" @click="refreshData">
          <i class="fas fa-sync-alt mr-2" :class="{ 'fa-spin': isRefreshing }"></i> Làm mới
        </button>
      </div>
    </div>

    <!-- 1. KPI Cards (Atomic Component) -->
    <DashboardKPIs :summary="summaryData" :loading="isRefreshing" />

    <!-- 2. Charts Row 1: Revenue by Month (Line) & Order Status (Pie) -->
    <div class="dashboard-row mb-4">
      <div class="col-main">
        <DashboardRevenueByMonth :loading="isRefreshing" />
      </div>
      <div class="col-side">
        <DashboardOrderStatusPie :loading="isRefreshing" />
      </div>
    </div>

    <!-- 3. Charts Row 2: Revenue by Day (Bar) & Top Products -->
    <div class="dashboard-row mb-4">
      <div class="col-main">
        <DashboardRevenueByDay :loading="isRefreshing" />
      </div>
      <div class="col-side">
        <DashboardTopProducts :products="topProductsData" :loading="isRefreshing" />
      </div>
    </div>

    <!-- 4. Tables Row 3: Recent Orders & Alerts -->
    <div class="dashboard-row">
      <div class="col-main">
        <DashboardRecentOrders :orders="recentOrdersData" :loading="isRefreshing" />
      </div>
      <div class="col-side">
        <DashboardInventoryAlerts />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import DashboardService from '@/services/DashboardService'
import DashboardKPIs from '@/components/admin/dashboard/DashboardKPIs.vue'
import DashboardRevenueByDay from '@/components/admin/dashboard/DashboardRevenueByDay.vue'
import DashboardRevenueByMonth from '@/components/admin/dashboard/DashboardRevenueByMonth.vue'
import DashboardOrderStatusPie from '@/components/admin/dashboard/DashboardOrderStatusPie.vue'
import DashboardTopProducts from '@/components/admin/dashboard/DashboardTopProducts.vue'
import DashboardRecentOrders from '@/components/admin/dashboard/DashboardRecentOrders.vue'
import DashboardInventoryAlerts from '@/components/admin/dashboard/DashboardInventoryAlerts.vue'

const isRefreshing = ref(false)
const summaryData = ref({})
const topProductsData = ref([])
const recentOrdersData = ref([])

const fetchDashboardData = async () => {
  try {
    isRefreshing.value = true
    const response = await DashboardService.getDashboardKPIs()
    const data = response.data
    
    // Map DTO to summary object expected by DashboardKPIs
    summaryData.value = {
      todayRevenue: data.orderStatistics?.todayRevenue || 0,
      todayOrders: data.orderStatistics?.totalOrders || 0, // DTO doesn't have todayOrders specifically, use total for now or backend could add it
      pendingOrders: data.orderStatistics?.pendingOrders || 0,
      shippingOrders: data.orderStatistics?.shippingOrders || 0,
      completedOrders: data.orderStatistics?.completedOrders || 0,
      newCustomers: data.customerStatistics?.activeCustomers || 0,
      outOfStockProducts: data.productStatistics?.lowStockVariants || 0,
      returnRequests: data.orderStatistics?.returnRequested || 0
    }
    
    topProductsData.value = data.topProducts || []
    recentOrdersData.value = data.recentOrders || []
  } catch (error) {
    console.error('Lỗi khi tải dữ liệu dashboard:', error)
  } finally {
    isRefreshing.value = false
  }
}

const refreshData = () => {
  fetchDashboardData()
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

.admin-dashboard-premium {
  padding: 32px;
  background-color: #f8fafc;
  min-height: 100vh;
  font-family: 'Inter', sans-serif;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-premium {
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  display: flex;
  align-items: center;
}

.btn-primary {
  background: #3b82f6;
  color: white;
  box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.3);
}

.btn-primary:hover {
  background: #2563eb;
  transform: translateY(-1px);
}

.btn-secondary {
  background: white;
  color: #334155;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.btn-secondary:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.dashboard-row {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.mb-4 {
  margin-bottom: 24px;
}

/* Tỉ lệ 2:1 cho biểu đồ và các widget phụ */
.col-main {
  flex: 2;
  min-width: 0;
}

.col-side {
  flex: 1;
  min-width: 350px;
}

.col-full {
  flex: 1;
  width: 100%;
  min-width: 0;
}

@media (max-width: 1200px) {
  .dashboard-row {
    flex-direction: column;
  }
  .col-main, .col-side, .col-full {
    min-width: 100%;
  }
}
</style>