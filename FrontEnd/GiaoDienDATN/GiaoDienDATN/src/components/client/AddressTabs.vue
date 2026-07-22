<template>
  <div class="address-tabs-wrapper">
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
import { ref, computed } from 'vue'

const props = defineProps({
  addresses: {
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
  { value: 'default', label: 'Mặc định', icon: 'fas fa-star', badgeClass: 'badge-gold' },
  { value: 'others', label: 'Khác', icon: 'fas fa-map-marker-alt', badgeClass: 'badge-gray' },
]

// ===== STATE =====
const activeMainTab = ref(props.initialMainTab || 'all')
const activeSubTab = ref(props.initialSubTab || 'all')

// ===== COMPUTED - Lấy danh sách tỉnh/thành phố từ addresses =====
const subTabs = computed(() => {
  // Lấy danh sách các tỉnh/thành phố duy nhất từ addresses
  const provinces = new Set()
  props.addresses.forEach(addr => {
    if (addr.provinceName) {
      provinces.add(addr.provinceName)
    }
  })
  
  // Tạo tab "Tất cả" + các tỉnh/thành phố
  const tabs = [
    { value: 'all', label: 'Tất cả', icon: 'fas fa-list' }
  ]
  
  // Thêm các tỉnh/thành phố vào tabs
  Array.from(provinces).sort().forEach(province => {
    tabs.push({
      value: province,
      label: province,
      icon: 'fas fa-city'
    })
  })
  
  return tabs
})

// ===== METHODS =====
const getMainTabCount = (tab) => {
  if (tab === 'all') return props.addresses.length
  if (tab === 'default') {
    return props.addresses.filter(a => a.isDefault).length
  }
  if (tab === 'others') {
    return props.addresses.filter(a => !a.isDefault).length
  }
  return 0
}

const getSubTabCount = (province) => {
  if (province === 'all') {
    return getMainTabCount(activeMainTab.value)
  }
  // Lọc theo tỉnh/thành phố
  let result = props.addresses.filter(a => a.provinceName === province)
  
  // Lọc thêm theo main tab
  if (activeMainTab.value === 'default') {
    result = result.filter(a => a.isDefault)
  } else if (activeMainTab.value === 'others') {
    result = result.filter(a => !a.isDefault)
  }
  
  return result.length
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

// ===== EXPOSE =====
defineExpose({
  activeMainTab,
  activeSubTab,
  resetTabs: () => {
    activeMainTab.value = 'all'
    activeSubTab.value = 'all'
  }
})
</script>

<style scoped>
.address-tabs-wrapper {
  margin-bottom: 20px;
}

.main-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  background: white;
  padding: 10px 15px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
}

.main-tab-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  font-size: 15px;
  color: #666;
  background: transparent;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.main-tab-btn:hover {
  background: #f0f0f0;
}

.main-tab-btn.active {
  background: #e63946;
  color: white;
  box-shadow: 0 4px 12px rgba(230, 57, 70, 0.3);
}

.main-tab-btn .tab-badge {
  padding: 1px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-left: 4px;
}

.main-tab-btn .tab-badge.badge-blue {
  background: rgba(52, 152, 219, 0.2);
  color: #2980b9;
}

.main-tab-btn .tab-badge.badge-gold {
  background: rgba(255, 193, 7, 0.2);
  color: #d4a017;
}

.main-tab-btn .tab-badge.badge-gray {
  background: rgba(108, 117, 125, 0.2);
  color: #6c757d;
}

.main-tab-btn.active .tab-badge {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.sub-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 15px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 10px;
  flex-wrap: wrap;
  border: 1px solid #e9ecef;
}

.sub-tab-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  font-size: 13px;
  color: #666;
  background: transparent;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sub-tab-btn:hover {
  background: #e9ecef;
}

.sub-tab-btn.active {
  background: #2c3e50;
  color: white;
}

.sub-tab-btn .tab-count {
  font-size: 11px;
  background: rgba(0, 0, 0, 0.08);
  padding: 0 8px;
  border-radius: 12px;
  font-weight: 600;
}

.sub-tab-btn.active .tab-count {
  background: rgba(255, 255, 255, 0.2);
}

@media (max-width: 768px) {
  .main-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
  }
  
  .main-tab-btn {
    white-space: nowrap;
    font-size: 13px;
    padding: 8px 16px;
  }
  
  .sub-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
  }
  
  .sub-tab-btn {
    white-space: nowrap;
    font-size: 12px;
    padding: 4px 12px;
  }
}
</style>