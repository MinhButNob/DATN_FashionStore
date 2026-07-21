<template>
  <div class="address-list-wrapper">
    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <i class="fas fa-spinner fa-spin"></i>
      <p>Đang tải địa chỉ...</p>
    </div>

    <!-- Empty -->
    <div v-else-if="filteredAddresses.length === 0" class="empty-state">
      <i class="fas fa-map-marker-alt"></i>
      <h3>Chưa có địa chỉ nào</h3>
      <p>{{ emptyMessage }}</p>
      <button class="btn btn-primary" @click="$emit('add-address')">
        <i class="fas fa-plus"></i> Thêm địa chỉ mới
      </button>
    </div>

    <!-- Address Cards -->
    <div v-else class="address-grid">
      <div 
        v-for="(addr, index) in filteredAddresses" 
        :key="addr.addressId || index"
        class="address-card"
        :class="{ 
          'default': addr.isDefault,
          'clickable': clickable 
        }"
        @click="clickable ? $emit('select-address', addr) : null"
      >
        <div class="address-card-header">
          <div class="address-info">
            <h4>{{ addr.fullName || 'Khách hàng' }}</h4>
            <p class="phone">
              <i class="fas fa-phone"></i>
              {{ addr.phone || '---' }}
            </p>
          </div>
          <div class="address-badges">
            <span v-if="addr.isDefault" class="badge-default">
              <i class="fas fa-star"></i> Mặc định
            </span>
          </div>
        </div>

        <div class="address-detail">
          <p class="address-line">
            <i class="fas fa-home"></i>
            {{ getFullAddress(addr) }}
          </p>
        </div>

        <div class="address-actions" v-if="!hideActions">
          <button 
            v-if="!addr.isDefault"
            class="btn btn-sm btn-outline"
            @click.stop="$emit('set-default', addr.addressId)"
          >
            <i class="fas fa-check"></i> Đặt làm mặc định
          </button>
          <button 
            class="btn btn-sm btn-outline"
            @click.stop="$emit('edit-address', addr)"
          >
            <i class="fas fa-edit"></i> Sửa
          </button>
          <button 
            class="btn btn-sm btn-danger"
            @click.stop="$emit('delete-address', addr.addressId)"
          >
            <i class="fas fa-trash"></i> Xóa
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  addresses: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  mainTab: {
    type: String,
    default: 'all'
  },
  subTab: {
    type: String,
    default: 'all'
  },
  hideActions: {
    type: Boolean,
    default: false
  },
  clickable: {
    type: Boolean,
    default: false
  },
  emptyMessage: {
    type: String,
    default: 'Bạn chưa có địa chỉ giao hàng nào'
  }
})

const emit = defineEmits([
  'add-address',
  'edit-address',
  'delete-address',
  'set-default',
  'select-address'
])

// ===== FILTER ADDRESSES =====
const filteredAddresses = computed(() => {
  let result = [...props.addresses]

  // Filter by main tab
  if (props.mainTab === 'default') {
    result = result.filter(a => a.isDefault)
  } else if (props.mainTab === 'others') {
    result = result.filter(a => !a.isDefault)
  }

  // Filter by sub tab (province)
  if (props.subTab !== 'all' && props.subTab) {
    result = result.filter(a => a.provinceName === props.subTab)
  }

  return result
})

// ===== HELPERS - Hiển thị địa chỉ đầy đủ =====
const getFullAddress = (addr) => {
  const parts = []
  
  // Số nhà, tên đường
  if (addr.streetAddress && addr.streetAddress.trim()) {
    parts.push(addr.streetAddress.trim())
  }
  
  // Phường/Xã
  if (addr.wardName && addr.wardName.trim()) {
    parts.push(addr.wardName.trim())
  }
  
  // Quận/Huyện - Lấy từ districtName
  if (addr.districtName && addr.districtName.trim()) {
    parts.push(addr.districtName.trim())
  }
  
  // Tỉnh/Thành phố
  if (addr.provinceName && addr.provinceName.trim()) {
    parts.push(addr.provinceName.trim())
  }
  
  return parts.length > 0 ? parts.join(', ') : 'Chưa có địa chỉ'
}
</script>

<style scoped>
.address-list-wrapper {
  width: 100%;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: #f8f9fa;
  border-radius: 15px;
}

.loading-state i {
  font-size: 40px;
  color: #e63946;
  margin-bottom: 15px;
}

.loading-state p {
  color: #6c757d;
  font-size: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #f8f9fa;
  border-radius: 15px;
}

.empty-state i {
  font-size: 60px;
  color: #e63946;
  margin-bottom: 15px;
  opacity: 0.7;
}

.empty-state h3 {
  color: #1a1a2e;
  font-size: 22px;
  margin-bottom: 10px;
}

.empty-state p {
  color: #6c757d;
  margin-bottom: 20px;
  font-size: 16px;
}

.address-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.address-card {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s;
  position: relative;
}

.address-card:hover {
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.address-card.default {
  border-color: #e63946;
  background: #fff8f8;
}

.address-card.default::before {
  content: 'Mặc định';
  position: absolute;
  top: -10px;
  right: 20px;
  background: #e63946;
  color: white;
  padding: 2px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.address-card.clickable {
  cursor: pointer;
}

.address-card.clickable:hover {
  transform: translateY(-3px);
  border-color: #e63946;
}

.address-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 10px;
}

.address-info h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: #1a1a2e;
}

.address-info .phone {
  margin: 0;
  font-size: 14px;
  color: #6c757d;
}

.address-info .phone i {
  margin-right: 5px;
}

.address-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badge-default {
  background: #e63946;
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.address-detail {
  margin-bottom: 15px;
}

.address-detail .address-line {
  margin: 4px 0;
  font-size: 14px;
  color: #555;
  line-height: 1.6;
}

.address-detail .address-line i {
  margin-right: 8px;
  color: #e63946;
  width: 18px;
}

.address-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding-top: 15px;
  border-top: 1px solid #e9ecef;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover {
  background: #c62828;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(230, 57, 70, 0.3);
}

.btn-outline {
  background: transparent;
  border: 1px solid #ddd;
  color: #666;
}

.btn-outline:hover {
  border-color: #e63946;
  color: #e63946;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

@media (max-width: 768px) {
  .address-card-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .address-actions {
    flex-direction: column;
  }
  
  .address-actions .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>