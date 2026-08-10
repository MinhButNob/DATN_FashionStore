<!-- src/components/admin/dashboard/YearPicker.vue -->
<template>
  <div class="year-picker-container" ref="pickerContainer">
    <!-- Input Box -->
    <div class="picker-input" @click="togglePicker" :class="{ 'is-open': isOpen }">
      <span>{{ modelValue }}</span>
    </div>

    <!-- Dropdown -->
    <div class="picker-dropdown shadow-sm" v-if="isOpen">
      <div class="picker-header">
        <button class="nav-btn" @click.stop="prevDecade"><i class="fas fa-chevron-left"></i></button>
        <span class="current-range">{{ startYear }} - {{ endYear }}</span>
        <button class="nav-btn" @click.stop="nextDecade"><i class="fas fa-chevron-right"></i></button>
      </div>
      
      <div class="picker-body">
        <div 
          v-for="year in yearsInView" 
          :key="year"
          class="year-cell"
          :class="{ 
            'active': year === parseInt(modelValue),
            'current': year === new Date().getFullYear()
          }"
          @click.stop="selectYear(year)"
        >
          {{ year }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: 2026
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isOpen = ref(false)
const pickerContainer = ref(null)

const baseYear = ref(parseInt(props.modelValue) || new Date().getFullYear())

// Calculate start year of the current 12-year view
const startYear = computed(() => {
  return Math.floor(baseYear.value / 12) * 12
})

const endYear = computed(() => {
  return startYear.value + 11
})

const yearsInView = computed(() => {
  const years = []
  for (let i = startYear.value; i <= endYear.value; i++) {
    years.push(i)
  }
  return years
})

const togglePicker = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    baseYear.value = parseInt(props.modelValue) || new Date().getFullYear()
  }
}

const closePicker = (e) => {
  if (pickerContainer.value && !pickerContainer.value.contains(e.target)) {
    isOpen.value = false
  }
}

const prevDecade = () => {
  baseYear.value -= 12
}

const nextDecade = () => {
  baseYear.value += 12
}

const selectYear = (year) => {
  isOpen.value = false
  emit('update:modelValue', year)
  emit('change', year)
}

onMounted(() => {
  document.addEventListener('click', closePicker)
})

onUnmounted(() => {
  document.removeEventListener('click', closePicker)
})
</script>

<style scoped>
.year-picker-container {
  position: relative;
  width: 100%;
}

.picker-input {
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 14px;
  background-color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  height: 34px;
  transition: all 0.2s;
  color: #1e293b;
}

.picker-input.is-open, .picker-input:hover {
  border-color: #3b82f6;
}

.picker-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 4px;
  width: 220px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  z-index: 1000;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  padding: 12px;
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.current-range {
  font-weight: 700;
  font-size: 15px;
  color: #1e293b;
}

.nav-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.nav-btn:hover {
  background: #f1f5f9;
  color: #334155;
}

.picker-body {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.year-cell {
  text-align: center;
  padding: 8px 4px;
  font-size: 13px;
  border-radius: 4px;
  cursor: pointer;
  color: #334155;
  transition: all 0.2s;
}

.year-cell:hover {
  background: #f1f5f9;
}

.year-cell.current {
  color: #3b82f6;
  font-weight: 600;
}

.year-cell.active {
  background: #2563eb;
  color: #fff;
  font-weight: 600;
}
</style>
