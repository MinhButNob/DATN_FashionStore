<!-- src/components/admin/dashboard/DashboardRevenueByMonth.vue -->
<template>
  <div class="chart-card shadow-sm">
    <!-- Filter for this chart -->
    <div class="local-filter">
      <div class="filter-group">
        <label>Chọn năm</label>
        <div style="width: 150px;">
          <YearPicker v-model="selectedYear" />
        </div>
      </div>
    </div>
    
    <div class="chart-header text-center">
      <h3 class="chart-title">Biểu đồ doanh thu theo từng tháng trong năm</h3>
    </div>
    
    <div class="chart-body">
      <div v-if="loading || isFetching" class="chart-loading">
        <div class="spinner-border text-primary" role="status"></div>
      </div>
      <div v-else class="chart-container">
        <Line :data="chartData" :options="chartOptions" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import YearPicker from './YearPicker.vue'
import DashboardService from '@/services/DashboardService'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const selectedYear = ref(new Date().getFullYear())
const isFetching = ref(false)

const labels = ['Th 1', 'Th 2', 'Th 3', 'Th 4', 'Th 5', 'Th 6', 'Th 7', 'Th 8', 'Th 9', 'Th 10', 'Th 11', 'Th 12']
const revenueData = ref(new Array(12).fill(0))

const fetchMonthlyRevenue = async () => {
  isFetching.value = true
  try {
    const res = await DashboardService.getMonthlyRevenue(selectedYear.value)
    // res.data is a map: { "1": 15000000, "2": 0, ... }
    const dataObj = res.data
    const newRevenue = []
    for (let i = 1; i <= 12; i++) {
      newRevenue.push(dataObj[i] || 0)
    }
    revenueData.value = newRevenue
  } catch (error) {
    console.error('Lỗi khi lấy doanh thu theo tháng:', error)
  } finally {
    isFetching.value = false
  }
}

watch(selectedYear, () => {
  fetchMonthlyRevenue()
})

onMounted(() => {
  fetchMonthlyRevenue()
})

const chartData = computed(() => {
  return {
    labels: labels,
    datasets: [
      {
        label: 'Doanh thu',
        data: revenueData.value,
        borderColor: '#3b82f6',
        backgroundColor: '#3b82f6',
        borderWidth: 2,
        pointBackgroundColor: '#3b82f6',
        pointRadius: 4,
        tension: 0
      }
    ]
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { 
      position: 'top',
      labels: {
        usePointStyle: false,
        boxWidth: 20
      }
    },
    tooltip: {
      callbacks: {
        label: function(context) {
          return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(context.parsed.y);
        }
      }
    }
  },
  scales: {
    x: {
      grid: { display: true, color: '#f1f5f9' },
    },
    y: {
      beginAtZero: true,
      grid: { display: true, color: '#f1f5f9' },
      ticks: {
        callback: function(value) {
          return new Intl.NumberFormat('vi-VN').format(value);
        }
      }
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
  gap: 8px;
}

.filter-group label {
  font-size: 14px;
  color: #334155;
  margin: 0;
}

.chart-header {
  margin-bottom: 20px;
}

.chart-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.chart-body {
  flex: 1;
  min-height: 400px;
  position: relative;
}

.chart-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.chart-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
</style>
