<!-- src/components/admin/dashboard/StatsCard.vue -->
<template>
  <div class="stat-card shadow-sm" :class="{ 'clickable': isClickable }">
    <div v-if="loading" class="stat-loading">
      <div class="skeleton skeleton-icon"></div>
      <div class="skeleton-text-group">
        <div class="skeleton skeleton-title"></div>
        <div class="skeleton skeleton-value"></div>
      </div>
    </div>
    
    <template v-else>
      <div class="stat-header">
        <div class="stat-icon-wrapper" :style="{ backgroundColor: `${color}15`, color: color }">
          <i :class="icon"></i>
        </div>
        <div v-if="trend !== undefined" class="stat-trend" :class="trend >= 0 ? 'trend-up' : 'trend-down'">
          <i :class="trend >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
          {{ Math.abs(trend) }}%
        </div>
      </div>
      <div class="stat-body">
        <h3 class="stat-title">{{ title }}</h3>
        <h2 class="stat-value" :style="{ color: isDanger ? '#ef4444' : '#0f172a' }">{{ value }}</h2>
      </div>
    </template>
  </div>
</template>

<script setup>
defineProps({
  title: String,
  value: [String, Number],
  icon: String,
  color: { type: String, default: '#3b82f6' },
  trend: Number,
  loading: Boolean,
  isClickable: { type: Boolean, default: false },
  isDanger: { type: Boolean, default: false }
})
</script>

<style scoped>
.stat-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #f1f5f9;
  position: relative;
  overflow: hidden;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.08) !important;
  border-color: #e2e8f0;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 20px;
}

.trend-up {
  background: #ecfdf5;
  color: #10b981;
}

.trend-down {
  background: #fef2f2;
  color: #ef4444;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
}

.stat-value {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

/* Skeleton Loading */
.stat-loading {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.skeleton {
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 8px;
}
.skeleton-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
}
.skeleton-text-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.skeleton-title {
  width: 60%;
  height: 16px;
}
.skeleton-value {
  width: 80%;
  height: 28px;
}
@keyframes loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
