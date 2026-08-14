<!-- components/client/OrderTabs.vue -->
<template>
  <div class="order-tabs-wrapper">
    <!-- ===== MAIN TABS ===== -->
    <div class="main-tabs">
      <button
        v-for="mainTab in mainTabs"
        :key="mainTab.value"
        class="main-tab-btn"
        :class="{ active: activeMainTab === mainTab.value }"
        @click="changeMainTab(mainTab.value)"
      >
        <i :class="mainTab.icon"></i>
        {{ mainTab.label }}
        <span class="tab-badge" :class="mainTab.badgeClass">
          {{ getMainTabCount(mainTab.value) }}
        </span>
      </button>
    </div>

    <!-- ===== SUB TABS ===== -->
    <div class="sub-tabs" v-if="subTabs.length > 0">
      <button
        v-for="subTab in subTabs"
        :key="subTab.value"
        class="sub-tab-btn"
        :class="{ active: activeSubTab === subTab.value }"
        @click="changeSubTab(subTab.value)"
      >
        <i :class="subTab.icon"></i>
        {{ subTab.label }}
        <span class="tab-count">{{ getSubTabCount(subTab.value) }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  orders: {
    type: Array,
    default: () => []
  },
  initialMainTab: {
    type: String,
    default: 'all'
  },
  initialSubTab: {
    type: String,
    default: 'all'
  }
})

const emit = defineEmits(['change-tab'])

// ===== TABS CONFIG =====
const mainTabs = [
  { value: 'all', label: 'Tất cả', icon: 'fas fa-list', badgeClass: 'badge-blue' },
  { value: 'orders', label: 'Đơn hàng', icon: 'fas fa-shopping-bag', badgeClass: 'badge-green' },
  { value: 'returns', label: 'Hoàn hàng', icon: 'fas fa-undo-alt', badgeClass: 'badge-orange' },
  { value: 'cancelled', label: 'Đã hủy', icon: 'fas fa-times-circle', badgeClass: 'badge-red' },
]

const subTabsMap = {
  orders: [
    { value: 'all', label: 'Tất cả', icon: 'fas fa-list' },
    { value: 'PENDING_PAYMENT', label: 'Chờ xác nhận', icon: 'fas fa-clock' },
    { value: 'PROCESSING', label: 'Đang xử lý', icon: 'fas fa-spinner' },
    { value: 'SHIPPING', label: 'Đang giao', icon: 'fas fa-truck' },
    { value: 'DELIVERED', label: 'Đã giao', icon: 'fas fa-box' },
    { value: 'COMPLETED', label: 'Hoàn thành', icon: 'fas fa-check-circle' },
  ],
  returns: [
    { value: 'all', label: 'Tất cả', icon: 'fas fa-list' },
    { value: 'RETURN_REQUESTED', label: 'Yêu cầu hoàn hàng', icon: 'fas fa-clock' },
    { value: 'RETURN_APPROVED', label: 'Đã duyệt', icon: 'fas fa-check' },
    { value: 'RETURN_RECEIVED', label: 'Đã nhận hàng trả', icon: 'fas fa-box' },
    { value: 'RETURNED', label: 'Đã hoàn hàng', icon: 'fas fa-check-circle' },
    { value: 'REJECTED', label: 'Từ chối', icon: 'fas fa-times-circle' },
  ],
  cancelled: [
    { value: 'all', label: 'Tất cả', icon: 'fas fa-list' },
    { value: 'CANCELLED', label: 'Đã hủy', icon: 'fas fa-times-circle' },
  ]
}

// ===== STATE =====
const activeMainTab = ref(props.initialMainTab || 'all')
const activeSubTab = ref(props.initialSubTab || 'all')

// ===== COMPUTED =====
const subTabs = computed(() => {
  if (activeMainTab.value === 'all') return []
  return subTabsMap[activeMainTab.value] || []
})

// ===== METHODS =====
const getMainTabCount = (tab) => {
  if (tab === 'all') return props.orders.length
  if (tab === 'orders') {
    return props.orders.filter(o => 
      !['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'REJECTED', 'CANCELLED'].includes(o.orderStatus)
    ).length
  }
  if (tab === 'returns') {
    return props.orders.filter(o => 
      ['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'REJECTED'].includes(o.orderStatus)
    ).length
  }
  if (tab === 'cancelled') {
    return props.orders.filter(o => o.orderStatus === 'CANCELLED').length
  }
  return 0
}

const getSubTabCount = (status) => {
  if (status === 'all') {
    return getMainTabCount(activeMainTab.value)
  }
  return props.orders.filter(o => o.orderStatus === status).length
}

const changeMainTab = (tab) => {
  activeMainTab.value = tab
  activeSubTab.value = 'all'
  emit('change-tab', {
    mainTab: tab,
    subTab: 'all'
  })
}

const changeSubTab = (tab) => {
  activeSubTab.value = tab
  emit('change-tab', {
    mainTab: activeMainTab.value,
    subTab: tab
  })
}

// ===== EXPOSE FOR PARENT =====
defineExpose({
  activeMainTab,
  activeSubTab,
  resetTabs: () => {
    activeMainTab.value = 'all'
    activeSubTab.value = 'all'
  }
})

// Watch for prop changes
watch(() => props.orders, () => {
  // Recalculate counts when orders change
}, { deep: true })
</script>

<style scoped>
.main-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
  background: #f8f9fa;
  padding: 8px 12px;
  border-radius: 10px;
  flex-wrap: wrap;
  border: 1px solid #e9ecef;
}

.main-tab-btn {
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  font-size: 14px;
  color: #666;
  background: transparent;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.main-tab-btn:hover {
  background: #e9ecef;
}

.main-tab-btn.active {
  background: #e63946;
  color: white;
  box-shadow: 0 2px 8px rgba(230, 57, 70, 0.3);
}

.main-tab-btn .tab-badge {
  padding: 0 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.sub-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 15px;
  padding: 6px 10px;
  background: white;
  border-radius: 8px;
  flex-wrap: wrap;
  border: 1px solid #e9ecef;
}

.sub-tab-btn {
  padding: 5px 14px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  font-size: 13px;
  color: #666;
  background: transparent;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.sub-tab-btn:hover {
  background: #f0f0f0;
}

.sub-tab-btn.active {
  background: #2c3e50;
  color: white;
}

.sub-tab-btn .tab-count {
  font-size: 10px;
  background: rgba(0, 0, 0, 0.08);
  padding: 0 6px;
  border-radius: 10px;
  font-weight: 600;
}

.sub-tab-btn.active .tab-count {
  background: rgba(255, 255, 255, 0.2);
}
</style>