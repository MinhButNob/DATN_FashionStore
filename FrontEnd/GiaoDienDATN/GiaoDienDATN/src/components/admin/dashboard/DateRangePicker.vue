<!-- src/components/admin/dashboard/DateRangePicker.vue -->
<template>
  <div class="date-range-picker-container" ref="pickerContainer">
    <!-- Input Box -->
    <div class="picker-input" @click="togglePicker" :class="{ 'is-open': isOpen }">
      <span>{{ formattedValue }}</span>
    </div>

    <!-- Dropdown -->
    <div class="picker-dropdown shadow-sm" v-if="isOpen">
      <div class="picker-header">
        <button class="nav-btn" @click.stop="prevMonth"><i class="fas fa-chevron-left"></i></button>
        <div class="current-month">
          <strong>{{ monthNames[currentMonth] }}</strong> {{ currentYear }}
        </div>
        <button class="nav-btn" @click.stop="nextMonth"><i class="fas fa-chevron-right"></i></button>
      </div>
      
      <div class="picker-body">
        <div class="weekdays">
          <div class="weekday" v-for="day in weekdays" :key="day">{{ day }}</div>
        </div>
        
        <div class="days-grid">
          <div 
            v-for="(day, index) in calendarDays" 
            :key="index"
            class="day-cell"
            :class="{ 
              'empty': !day.date,
              'in-range': isInRange(day.date),
              'start-range': isStart(day.date),
              'end-range': isEnd(day.date)
            }"
            @click.stop="day.date && selectDay(day.date)"
          >
            {{ day.dayNum }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: String, // Format: "MM/DD/YYYY - MM/DD/YYYY"
    default: '01/08/2026 - 15/08/2026'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isOpen = ref(false)
const pickerContainer = ref(null)

const currentMonth = ref(7) // 0-11, August is 7
const currentYear = ref(2026)

const startDate = ref(null)
const endDate = ref(null)

const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
const weekdays = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa']

const formattedValue = computed(() => {
  if (startDate.value === null || endDate.value === null) return 'Chọn khoảng thời gian'
  
  const sm = currentMonth.value + 1
  const sDay = startDate.value
  const eDay = endDate.value
  const y = currentYear.value
  
  const f_sm = sm < 10 ? '0'+sm : sm
  const f_sDay = sDay < 10 ? '0'+sDay : sDay
  const f_eDay = eDay < 10 ? '0'+eDay : eDay
  
  return `${f_sDay}/${f_sm}/${y} - ${f_eDay}/${f_sm}/${y}`
})

// Generate a fake calendar grid for visual mockup purposes
const calendarDays = computed(() => {
  const days = []
  // Add some empty slots at beginning
  for (let i = 0; i < 4; i++) {
    days.push({ dayNum: 27 + i, date: null })
  }
  
  // Add 31 days
  for (let i = 1; i <= 31; i++) {
    days.push({ dayNum: i, date: i })
  }
  return days
})

const togglePicker = () => {
  isOpen.value = !isOpen.value
}

const closePicker = (e) => {
  if (pickerContainer.value && !pickerContainer.value.contains(e.target)) {
    isOpen.value = false
  }
}

const prevMonth = () => {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

const nextMonth = () => {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

// Simple selection logic for mockup
let step = 0
const selectDay = (date) => {
  if (step === 0) {
    startDate.value = date
    endDate.value = date
    step = 1
    emit('update:modelValue', formattedValue.value)
    emit('change', formattedValue.value)
  } else {
    if (date < startDate.value) {
      endDate.value = startDate.value
      startDate.value = date
    } else {
      endDate.value = date
    }
    step = 0
    isOpen.value = false
    emit('update:modelValue', formattedValue.value)
    emit('change', formattedValue.value)
  }
}

const isStart = (date) => date === startDate.value
const isEnd = (date) => date === endDate.value
const isInRange = (date) => date >= startDate.value && date <= endDate.value

const parseDateRange = () => {
  try {
    if (!props.modelValue) return
    const parts = props.modelValue.split(' - ')
    if (parts.length === 2) {
      const sParts = parts[0].split('/')
      const eParts = parts[1].split('/')
      
      startDate.value = parseInt(sParts[0], 10)
      currentMonth.value = parseInt(sParts[1], 10) - 1
      currentYear.value = parseInt(sParts[2], 10)
      
      endDate.value = parseInt(eParts[0], 10)
    }
  } catch (e) {
    console.error("Error parsing date range", e)
  }
}

onMounted(() => {
  parseDateRange()
  document.addEventListener('click', closePicker)
})

onUnmounted(() => {
  document.removeEventListener('click', closePicker)
})
</script>

<style scoped>
.date-range-picker-container {
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
  width: 260px;
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
}

.current-month {
  font-size: 14px;
  color: #1e293b;
  text-align: center;
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

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  row-gap: 4px;
}

.day-cell {
  text-align: center;
  padding: 6px 0;
  font-size: 13px;
  cursor: pointer;
  color: #334155;
  border-radius: 4px;
  margin: 0 2px;
}

.day-cell.empty {
  color: #cbd5e1;
  cursor: default;
}

.day-cell:not(.empty):hover {
  background: #f1f5f9;
}

.day-cell.in-range {
  background: #2563eb;
  color: #fff;
  border-radius: 0;
  margin: 0;
}

.day-cell.start-range {
  border-top-left-radius: 4px;
  border-bottom-left-radius: 4px;
  margin-left: 2px;
}

.day-cell.end-range {
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
  margin-right: 2px;
}
</style>
