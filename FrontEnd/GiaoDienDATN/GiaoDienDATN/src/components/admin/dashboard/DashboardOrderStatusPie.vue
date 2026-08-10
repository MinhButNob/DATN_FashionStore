<!-- src/components/admin/dashboard/DashboardOrderStatusPie.vue -->
<template>
  <div class="chart-card shadow-sm">
    
    <!-- Filter -->
    <div class="local-filter">
      <div class="filter-group w-100">
        <label>Loại thống kê</label>
        <select class="form-control mb-3" v-model="filterType">
          <option value="Ngày">Ngày</option>
          <option value="Tháng">Tháng</option>
          <option value="Năm">Năm</option>
        </select>
        
        <!-- Dynamic Input based on selection -->
        <template v-if="filterType === 'Ngày'">
          <label>Từ ngày - Đến ngày</label>
          <div class="date-picker-wrapper mb-3">
            <DateRangePicker v-model="selectedDateRange" />
          </div>
        </template>
        
        <template v-if="filterType === 'Tháng'">
          <label>Chọn tháng</label>
          <div class="date-picker-wrapper mb-3">
            <MonthPicker v-model="selectedMonth" />
          </div>
        </template>
        
        <template v-if="filterType === 'Năm'">
          <label>Chọn năm</label>
          <div class="date-picker-wrapper mb-3">
            <YearPicker v-model="selectedYear" />
          </div>
        </template>
        
        <button class="btn-filter" @click="fetchOrderStatus">Lọc</button>
      </div>
    </div>
    
    <div class="chart-header text-center">
      <h3 class="chart-title">Thống kê trạng thái đơn hàng</h3>
    </div>
    
    <div class="chart-body">
      <div v-if="loading || isFetching" class="chart-loading">
        <div class="spinner-border text-primary" role="status"></div>
      </div>
      <div v-else-if="isDataEmpty" class="empty-data-msg">
        <i class="fas fa-inbox mb-2" style="font-size: 24px; color: #94a3b8;"></i>
        <div>Không có dữ liệu trong khoảng thời gian này</div>
      </div>
      <div v-else class="chart-layout">
        <div class="chart-wrapper">
          <Doughnut :key="chartKey" :data="chartData" :options="chartOptions" />
          <div class="doughnut-center">
            <span class="total-label">Tổng đơn</span>
            <span class="total-value">{{ totalOrders }}</span>
          </div>
        </div>
        <div class="custom-legend">
          <div v-for="item in legendItems" :key="item.label" class="legend-item">
            <span class="legend-color" :style="{ backgroundColor: item.color }"></span>
            <span class="legend-label">{{ item.label }}</span>
            <span class="legend-count">{{ item.count }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Doughnut } from 'vue-chartjs'
import MonthPicker from './MonthPicker.vue'
import YearPicker from './YearPicker.vue'
import DateRangePicker from './DateRangePicker.vue'
import DashboardService from '@/services/DashboardService'
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend
} from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend)

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const filterType = ref('Ngày')
const selectedDateRange = ref('')
const selectedMonth = ref('08/2026')
const selectedYear = ref(new Date().getFullYear())

const isFetching = ref(false)
const statsData = ref({})
const chartKey = ref(0)

const fetchOrderStatus = async () => {
  isFetching.value = true;
  try {
    const res = await DashboardService.getOrderStatus(
      filterType.value,
      selectedDateRange.value,
      selectedMonth.value,
      selectedYear.value
    )
    statsData.value = res.data
    chartKey.value++ // Kích hoạt lại animation khi có data mới
  } catch (error) {
    console.error('Lỗi khi lấy trạng thái đơn hàng:', error)
  } finally {
    isFetching.value = false;
  }
}

onMounted(() => {
  fetchOrderStatus()
})

const statusConfig = {
  'PENDING_PAYMENT': { label: 'Chờ thanh toán', color: '#ffe4b5' },
  'CONFIRMED': { label: 'Đã xác nhận', color: '#cce0ff' },
  'PROCESSING': { label: 'Đang xử lý', color: '#ffeb99' },
  'SHIPPING': { label: 'Đang giao', color: '#b3e6ff' },
  'DELIVERED': { label: 'Đã giao', color: '#d6f5f5' },
  'COMPLETED': { label: 'Hoàn thành', color: '#b3ffb3' },
  'RETURN_REQUESTED': { label: 'Yêu cầu hoàn hàng', color: '#ffccd5' },
  'RETURN_APPROVED': { label: 'Đã duyệt hoàn hàng', color: '#e6ccff' },
  'RETURN_RECEIVED': { label: 'Đã nhận hàng trả', color: '#cc99ff' },
  'RETURN_PENDING': { label: 'Đang thu hồi hàng', color: '#ffd480' },
  'RETURNED': { label: 'Đã hoàn hàng', color: '#99ff99' },
  'REJECTED': { label: 'Từ chối hoàn trả', color: '#ff9999' },
  'CANCELLED': { label: 'Đã hủy', color: '#cccccc' }
}

const totalOrders = computed(() => {
  return Object.values(statsData.value).reduce((sum, val) => sum + (val || 0), 0)
})

const isDataEmpty = computed(() => {
  if (Object.keys(statsData.value).length === 0) return true
  return totalOrders.value === 0
})

const legendItems = computed(() => {
  const activeKeys = Object.keys(statusConfig)
  return activeKeys.map(key => ({
    label: statusConfig[key].label,
    color: statusConfig[key].color,
    count: statsData.value[key] || 0
  }))
})

const chartData = computed(() => {
  const activeKeys = Object.keys(statusConfig)
  const labels = []
  const data = []
  const backgroundColor = []

  activeKeys.forEach(key => {
    const count = statsData.value[key] || 0
    if (count > 0) {
      labels.push(statusConfig[key].label)
      data.push(count)
      backgroundColor.push(statusConfig[key].color)
    }
  })

  return {
    labels,
    datasets: [
      {
        data,
        backgroundColor,
        borderColor: '#ffffff',
        borderWidth: 2,
        hoverOffset: 8
      }
    ]
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '70%',
  animation: {
    animateRotate: true,
    animateScale: true,
    duration: 1200,
    easing: 'easeOutQuart'
  },
  plugins: {
    legend: {
      display: false
    },
    tooltip: {
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      titleColor: '#ffffff',
      bodyColor: '#e2e8f0',
      padding: 12,
      cornerRadius: 8,
      titleFont: { size: 14, family: 'Inter', weight: '600' },
      bodyFont: { size: 14, family: 'Inter' },
      displayColors: true,
      boxPadding: 4
    }
  }
}
</script>

<style scoped>
.chart-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.local-filter {
  display: flex;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  flex-direction: column;
}

.filter-group label {
  font-size: 14px;
  color: #334155;
  margin-bottom: 8px;
}

.mb-3 {
  margin-bottom: 16px;
}

.form-control {
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 14px;
  width: 100%;
  outline: none;
}

.form-control:focus {
  border-color: #3b82f6;
}

.date-picker-wrapper {
  position: relative;
  width: 100%;
}

.btn-filter {
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 24px;
  font-size: 14px;
  width: fit-content;
  cursor: pointer;
}

.btn-filter:hover {
  background: #1d4ed8;
}

.chart-header {
  margin-bottom: 10px;
}

.chart-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.chart-body {
  flex: 1;
  min-height: 350px;
  position: relative;
  display: flex;
  flex-direction: column;
}

.chart-layout {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

.chart-wrapper {
  position: relative;
  height: 250px;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.doughnut-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.total-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.total-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.custom-legend {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px 16px;
  padding: 0 10px;
  margin-top: 10px;
  max-height: 200px;
  overflow-y: auto;
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
  flex-shrink: 0;
}

.legend-label {
  color: #475569;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.legend-count {
  font-weight: 600;
  color: #1e293b;
  margin-left: 8px;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.chart-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.empty-data-msg {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #64748b;
  font-size: 15px;
  font-weight: 500;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
