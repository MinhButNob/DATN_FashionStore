<!-- src/components/admin/dashboard/DashboardFilter.vue -->
<template>
  <div class="filter-card shadow-sm mb-4">
    <div class="filter-group">
      <label class="filter-label"><i class="fas fa-filter mr-1"></i> Loại thống kê:</label>
      <div class="filter-options">
        <label class="radio-label">
          <input type="radio" v-model="filterType" value="dateRange"> Theo ngày (Tùy chỉnh)
        </label>
        <label class="radio-label">
          <input type="radio" v-model="filterType" value="month"> Theo tháng
        </label>
        <label class="radio-label">
          <input type="radio" v-model="filterType" value="year"> Theo năm
        </label>
      </div>
    </div>
    
    <div class="filter-inputs">
      <!-- Date Range -->
      <div v-if="filterType === 'dateRange'" class="input-group">
        <div class="input-item">
          <span class="input-label">Từ ngày:</span>
          <input type="date" class="form-control" v-model="fromDate">
        </div>
        <div class="input-item">
          <span class="input-label">Đến ngày:</span>
          <input type="date" class="form-control" v-model="toDate">
        </div>
      </div>
      
      <!-- Month -->
      <div v-if="filterType === 'month'" class="input-group">
        <div class="input-item">
          <span class="input-label">Chọn tháng:</span>
          <input type="month" class="form-control" v-model="selectedMonth">
        </div>
      </div>
      
      <!-- Year -->
      <div v-if="filterType === 'year'" class="input-group">
        <div class="input-item">
          <span class="input-label">Chọn năm:</span>
          <select class="form-control" v-model="selectedYear">
            <option value="2024">2024</option>
            <option value="2025">2025</option>
            <option value="2026">2026</option>
          </select>
        </div>
      </div>
      
      <button class="btn-apply btn-primary">Áp dụng</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const filterType = ref('dateRange')
const fromDate = ref('2026-08-01')
const toDate = ref('2026-08-31')
const selectedMonth = ref('2026-08')
const selectedYear = ref('2026')
</script>

<style scoped>
.filter-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px 24px;
  border: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.filter-label {
  font-weight: 600;
  color: #334155;
  margin: 0;
  font-size: 14px;
}

.filter-options {
  display: flex;
  gap: 16px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #475569;
  cursor: pointer;
  margin: 0;
}

.filter-inputs {
  display: flex;
  align-items: center;
  gap: 16px;
}

.input-group {
  display: flex;
  gap: 12px;
}

.input-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-label {
  font-size: 13px;
  color: #64748b;
}

.form-control {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 14px;
  color: #1e293b;
  outline: none;
}

.form-control:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.btn-apply {
  padding: 8px 20px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  background: #3b82f6;
  color: white;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
}

.btn-apply:hover {
  background: #2563eb;
}
</style>
