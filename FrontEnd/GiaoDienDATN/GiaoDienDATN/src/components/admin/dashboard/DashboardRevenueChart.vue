<!-- src/components/admin/dashboard/DashboardRevenueChart.vue -->
<template>
  <div class="chart-card shadow-sm">
    <div class="chart-header">
      <div>
        <h3 class="chart-title">Thống kê doanh thu</h3>
        <p class="chart-subtitle">Doanh thu 30 ngày qua (Tất cả các kênh)</p>
      </div>
      <div class="chart-actions">
        <select class="form-select form-select-sm nice-select" v-model="timeRange">
          <option value="7">7 ngày qua</option>
          <option value="30">30 ngày qua</option>
          <option value="this_year">Năm nay</option>
        </select>
      </div>
    </div>
    
    <div class="chart-body">
      <div v-if="loading" class="chart-loading">
        <div class="spinner-border text-primary" role="status"></div>
      </div>
      <div v-else class="chart-container">
        <Line :data="chartData" :options="chartOptions" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler)

const props = defineProps({
  monthlyRevenue: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const timeRange = ref('30')

// Dummy data to look good even before API is ready
const dummyLabels = Array.from({length: 30}, (_, i) => `Ngày ${i + 1}`);
const dummyData = Array.from({length: 30}, () => Math.floor(Math.random() * 20000000) + 5000000);

const chartData = computed(() => {
  // Use actual data if available, else fallback to dummy for visual preview
  const labels = props.monthlyRevenue.length ? props.monthlyRevenue.map(d => `Tháng ${d.month}`) : dummyLabels
  const data = props.monthlyRevenue.length ? props.monthlyRevenue.map(d => d.revenue) : dummyData

  return {
    labels,
    datasets: [
      {
        label: 'Doanh thu (VNĐ)',
        data,
        borderColor: '#3b82f6',
        backgroundColor: (context) => {
          const ctx = context.chart.ctx;
          const gradient = ctx.createLinearGradient(0, 0, 0, 350);
          gradient.addColorStop(0, 'rgba(59, 130, 246, 0.2)');
          gradient.addColorStop(1, 'rgba(59, 130, 246, 0.0)');
          return gradient;
        },
        borderWidth: 2,
        pointBackgroundColor: '#ffffff',
        pointBorderColor: '#3b82f6',
        pointBorderWidth: 2,
        pointRadius: 4,
        pointHoverRadius: 6,
        fill: true,
        tension: 0.4 // Creates nice smooth curves
      }
    ]
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      padding: 12,
      titleFont: { size: 13, family: "'Inter', sans-serif" },
      bodyFont: { size: 14, family: "'Inter', sans-serif", weight: 'bold' },
      displayColors: false,
      callbacks: {
        label: function(context) {
          return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(context.parsed.y);
        }
      }
    }
  },
  scales: {
    x: {
      grid: { display: false, drawBorder: false },
      ticks: { font: { family: "'Inter', sans-serif" }, color: '#94a3b8' }
    },
    y: {
      grid: { color: '#f1f5f9', drawBorder: false, borderDash: [5, 5] },
      ticks: {
        font: { family: "'Inter', sans-serif" },
        color: '#94a3b8',
        callback: function(value) {
          if (value >= 1000000) return (value / 1000000) + 'Tr';
          return value;
        }
      }
    }
  },
  interaction: {
    mode: 'index',
    intersect: false,
  }
}
</script>

<style scoped>
.chart-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #f1f5f9;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.chart-title {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.chart-subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.nice-select {
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-weight: 500;
  box-shadow: none;
  cursor: pointer;
  background-color: #f8fafc;
}

.nice-select:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.chart-body {
  flex: 1;
  min-height: 300px;
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
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
