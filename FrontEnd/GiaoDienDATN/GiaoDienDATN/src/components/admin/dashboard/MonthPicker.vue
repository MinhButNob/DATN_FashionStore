<!-- src/components/admin/dashboard/MonthPicker.vue -->
<template>
  <div class="month-picker-container" ref="pickerContainer">
    <!-- Input Box -->
    <div class="picker-input" @click="togglePicker" :class="{ 'is-open': isOpen }">
      <span>{{ formattedValue }}</span>
    </div>

    <!-- Dropdown -->
    <div class="picker-dropdown shadow-sm" v-if="isOpen">
      <div class="picker-header">
        <button class="nav-btn" @click.stop="prevYear"><i class="fas fa-chevron-left"></i></button>
        <span class="current-year">{{ currentViewYear }}</span>
        <button class="nav-btn" @click.stop="nextYear"><i class="fas fa-chevron-right"></i></button>
      </div>
      
      <div class="picker-body">
        <div 
          v-for="(month, index) in months" 
          :key="index"
          class="month-cell"
          :class="{ 
            'active': isSelected(index),
            'current': isCurrentMonth(index)
          }"
          @click.stop="selectMonth(index)"
        >
          {{ month }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: String, // Format: "MM/YYYY" or "YYYY-MM"
    default: '08/2026'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isOpen = ref(false)
const pickerContainer = ref(null)

// Parse initial value (assuming MM/YYYY)
const parseInitial = () => {
  if (props.modelValue && props.modelValue.includes('/')) {
    const parts = props.modelValue.split('/')
    return { month: parseInt(parts[0]) - 1, year: parseInt(parts[1]) }
  }
  const d = new Date()
  return { month: d.getMonth(), year: d.getFullYear() }
}

const selectedDate = ref(parseInitial())
const currentViewYear = ref(selectedDate.value.year)

const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

const formattedValue = computed(() => {
  const m = selectedDate.value.month + 1
  const mm = m < 10 ? '0' + m : m
  return `${mm}/${selectedDate.value.year}`
})

const togglePicker = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    currentViewYear.value = selectedDate.value.year
  }
}

const closePicker = (e) => {
  if (pickerContainer.value && !pickerContainer.value.contains(e.target)) {
    isOpen.value = false
  }
}

const prevYear = () => {
  currentViewYear.value--
}

const nextYear = () => {
  currentViewYear.value++
}

const selectMonth = (index) => {
  selectedDate.value = { month: index, year: currentViewYear.value }
  isOpen.value = false
  emit('update:modelValue', formattedValue.value)
  emit('change', formattedValue.value)
}

const isSelected = (index) => {
  return selectedDate.value.month === index && selectedDate.value.year === currentViewYear.value
}

const isCurrentMonth = (index) => {
  const d = new Date()
  return index === d.getMonth() && currentViewYear.value === d.getFullYear()
}

onMounted(() => {
  document.addEventListener('click', closePicker)
})

onUnmounted(() => {
  document.removeEventListener('click', closePicker)
})

watch(() => props.modelValue, (newVal) => {
  if (newVal !== formattedValue.value) {
    selectedDate.value = parseInitial()
  }
})
</script>

<style scoped>
.month-picker-container {
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

.current-year {
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
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
}

.month-cell {
  text-align: center;
  padding: 8px 4px;
  font-size: 13px;
  border-radius: 4px;
  cursor: pointer;
  color: #334155;
  transition: all 0.2s;
}

.month-cell:hover {
  background: #f1f5f9;
}

.month-cell.current {
  color: #3b82f6;
  font-weight: 600;
}

.month-cell.active {
  background: #2563eb;
  color: #fff;
  font-weight: 600;
}
</style>
