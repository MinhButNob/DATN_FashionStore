<template>
  <div class="stat-card" :style="cardStyle">
    <div class="stat-info">
      <h3>{{ title }}</h3>
      <div class="stat-number">{{ value }}</div>
      <div v-if="change" class="stat-change" :class="{ positive: isPositive, negative: !isPositive }">
        <i :class="isPositive ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
        {{ change }}
      </div>
    </div>
    <div class="stat-icon" :style="iconStyle">
      <i :class="icon"></i>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  change: {
    type: String,
    default: null
  },
  icon: {
    type: String,
    required: true
  },
  color: {
    type: String,
    default: '#e63946'
  }
})

const isPositive = computed(() => {
  if (!props.change) return true
  return !props.change.startsWith('-')
})

const cardStyle = computed(() => ({
  borderLeft: `4px solid ${props.color}`
}))

const iconStyle = computed(() => ({
  background: `linear-gradient(135deg, ${props.color}20, ${props.color}10)`,
  color: props.color
}))
</script>

<style scoped>
.stat-card {
  background: white;
  padding: 25px;
  border-radius: 15px;
  box-shadow: var(--shadow);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-hover);
}

.stat-info h3 {
  font-size: 15px;
  color: var(--medium-gray);
  margin-bottom: 8px;
  font-weight: 500;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--dark-gray);
  margin-bottom: 5px;
}

.stat-change {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 3px;
}

.stat-change.positive {
  color: #28a745;
}

.stat-change.negative {
  color: #dc3545;
}

.stat-change i {
  font-size: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}
</style>