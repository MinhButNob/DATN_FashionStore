<!-- src/components/admin/dashboard/DashboardRevenueByDay.vue -->
<template>
  <div class="chart-card shadow-sm">
    <div class="local-filter">
      <div class="filter-group">
        <label>Chọn tháng</label>
        <div class="date-picker-wrapper">
          <MonthPicker v-model="selectedMonth" />
        </div>
      </div>
    </div>
    
    <div class="chart-header text-center">
      <h3 class="chart-title">Biểu đồ doanh thu theo từng ngày trong tháng</h3>
    </div>
    
    <div class="chart-body">
      <div v-if="loading || isFetching" class="chart-loading">
        <div class="spinner-border text-primary" role="status"></div>
      </div>
      <div v-else class="chart-container">
        <Bar :data="chartData" :options="chartOptions" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { Bar } from 'vue-chartjs'
import MonthPicker from './MonthPicker.vue'
import DashboardService from '@/services/DashboardService'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

// Format của MonthPicker là MM/YYYY
const selectedMonth = ref('08/2026')
const isFetching = ref(false)

const labelsArray = ref([])
const dataArray = ref([])

const fetchDailyRevenue = async () => {
  if (!selectedMonth.value) return;
  const parts = selectedMonth.value.split('/');
  if (parts.length !== 2) return;
  const m = parseInt(parts[0]);
  const y = parseInt(parts[1]);

  isFetching.value = true;
  try {
    const res = await DashboardService.getDailyRevenue(y, m);
    const dataObj = res.data; // { "1": 0, "2": 50000, ... }
    
    labelsArray.value = Object.keys(dataObj);
    dataArray.value = Object.values(dataObj);
  } catch (error) {
    console.error('Lỗi khi lấy doanh thu theo ngày:', error);
  } finally {
    isFetching.value = false;
  }
}

watch(selectedMonth, () => {
  fetchDailyRevenue()
})

onMounted(() => {
  fetchDailyRevenue()
})

const chartData = computed(() => {
  return {
    labels: labelsArray.value.length ? labelsArray.value : Array.from({length: 31}, (_, i) => `${i + 1}`),
    datasets: [
      {
        label: 'Doanh thu',
        data: dataArray.value.length ? dataArray.value : Array.from({length: 31}, () => 0),
        backgroundColor: '#ffb3c6', // Pinkish color from user request
        barPercentage: 0.5
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
      grid: { display: true, color: '#f1f5f9' }
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

.date-picker-wrapper {
  width: 150px;
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
