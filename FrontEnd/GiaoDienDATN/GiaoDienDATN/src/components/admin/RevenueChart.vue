<!-- components/admin/RevenueChart.vue -->
<template>
  <div class="revenue-chart">
    <div v-if="loading" class="chart-loading">
      <div class="skeleton-chart"></div>
    </div>
    <div v-else-if="data.length === 0" class="no-data-chart">
      <p>Chưa có dữ liệu doanh thu</p>
    </div>
    <div v-else class="chart-container">
      <div class="chart-bars">
        <div
          v-for="(item, index) in data"
          :key="index"
          class="chart-bar-wrapper"
        >
          <div
            class="chart-bar"
            :style="{
              height: getBarHeight(item.revenue) + '%',
              backgroundColor: getBarColor(index)
            }"
          >
            <span class="bar-value">{{ formatPrice(item.revenue) }}</span>
          </div>
          <span class="bar-label">Thg {{ item.month }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const maxRevenue = computed(() => {
  if (props.data.length === 0) return 0
  return Math.max(...props.data.map(d => d.revenue || 0))
})

const formatPrice = (value) => {
  if (!value || value === 0) return '0₫'
  if (value >= 1000000) {
    return (value / 1000000).toFixed(1) + 'M₫'
  }
  return value.toLocaleString('vi-VN') + '₫'
}

const getBarHeight = (value) => {
  if (maxRevenue.value === 0) return 0
  return Math.max((value / maxRevenue.value) * 100, 5)
}

const getBarColor = (index) => {
  const colors = ['#e63946', '#f39c12', '#2ecc71', '#3498db', '#6c5ce7', '#fd79a8', '#00b894', '#e17055', '#0984e3', '#fdcb6e', '#6c5ce7', '#00cec9']
  return colors[index % colors.length]
}
</script>

<style scoped>
.revenue-chart {
  width: 100%;
  min-height: 250px;
}

.chart-container {
  width: 100%;
  height: 250px;
  padding-top: 20px;
}

.chart-bars {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 100%;
  gap: 6px;
}

.chart-bar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  height: 100%;
  justify-content: flex-end;
}

.chart-bar {
  width: 100%;
  min-height: 8px;
  border-radius: 4px 4px 0 0;
  position: relative;
  transition: height 0.6s ease;
  cursor: pointer;
}

.chart-bar:hover {
  opacity: 0.85;
}

.chart-bar .bar-value {
  position: absolute;
  top: -24px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  font-weight: 600;
  color: #2c3e50;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.3s;
}

.chart-bar:hover .bar-value {
  opacity: 1;
}

.bar-label {
  font-size: 11px;
  color: #999;
  margin-top: 6px;
  text-align: center;
}

.chart-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 250px;
  width: 100%;
}

.skeleton-chart {
  width: 100%;
  height: 200px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 8px;
}

.no-data-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 250px;
  color: #999;
  font-style: italic;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@media (max-width: 480px) {
  .chart-bar .bar-value {
    font-size: 9px;
    top: -20px;
  }

  .bar-label {
    font-size: 9px;
  }
}
</style>