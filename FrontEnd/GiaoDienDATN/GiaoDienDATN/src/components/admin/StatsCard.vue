<!-- components/admin/StatsCard.vue -->
<template>
  <div class="stats-card" :style="{ borderLeftColor: color }">
    <div class="stats-icon" :style="{ background: color }">
      <i :class="icon"></i>
    </div>
    <div class="stats-content">
      <div class="stats-value" v-if="!loading">
        {{ value }}
        <span v-if="trend !== undefined" class="trend" :class="{ positive: trend >= 0, negative: trend < 0 }">
          {{ trend >= 0 ? '+' : '' }}{{ trend }}%
        </span>
      </div>
      <div class="stats-value" v-else>
        <div class="skeleton"></div>
      </div>
      <div class="stats-title">{{ title }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  icon: {
    type: String,
    required: true
  },
  color: {
    type: String,
    default: '#e63946'
  },
  loading: {
    type: Boolean,
    default: false
  },
  trend: {
    type: Number,
    default: undefined
  }
})
</script>

<style scoped>
.stats-card {
  background: white;
  padding: 20px;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 16px;
  border-left: 4px solid #e63946;
  transition: all 0.3s;
}

.stats-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 25px rgba(0, 0, 0, 0.08);
}

.stats-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
  flex-shrink: 0;
}

.stats-content {
  flex: 1;
  min-width: 0;
}

.stats-value {
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.trend {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
}

.trend.positive {
  background: #d4edda;
  color: #155724;
}

.trend.negative {
  background: #f8d7da;
  color: #721c24;
}

.stats-title {
  font-size: 14px;
  color: #999;
  margin-top: 2px;
}

.skeleton {
  width: 80px;
  height: 28px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@media (max-width: 480px) {
  .stats-card {
    padding: 14px 16px;
  }

  .stats-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .stats-value {
    font-size: 18px;
  }
}
</style>