<!-- components/client/ChatCategoryModal.vue -->
<template>
  <div v-if="visible" class="modal-overlay">
    <div class="modal-container">
      <div class="modal-header">
        <h2>💬 Chọn bộ phận hỗ trợ</h2>
        <button class="close-btn" @click="close">✕</button>
      </div>
      <div class="modal-body">
        <p class="subtitle">Vui lòng chọn lý do bạn cần hỗ trợ để chúng tôi kết nối đúng nhân viên phù hợp.</p>
        
        <div class="category-grid">
          <button
            v-for="cat in categories"
            :key="cat.value"
            class="category-card"
            @click="handleSelect(cat)"
          >
            <div class="category-icon">{{ cat.icon }}</div>
            <div class="category-info">
              <h4>{{ cat.label }}</h4>
              <p>{{ cat.description }}</p>
              <span class="category-role">👤 {{ cat.assignedTo }}</span>
            </div>
            <i class="fas fa-chevron-right arrow-icon"></i>
          </button>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="close">Hủy</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/authStore'

const props = defineProps({
  visible: Boolean
})

const emit = defineEmits(['close'])

const router = useRouter()
const authStore = useAuthStore()

const categories = [
  {
    value: 'TU_VAN_SAN_PHAM',
    label: 'Tư vấn sản phẩm',
    icon: '🛍️',
    description: 'Tư vấn về sản phẩm, size, màu sắc, chất liệu...',
    assignedTo: 'Nhân viên bán hàng tại quầy',
    // CHUYỂN ĐẾN STAFF POS
    route: '/staff/chat'
  },
  {
    value: 'HOAN_HANG_ONLINE',
    label: 'Hoàn hàng - Mua online',
    icon: '📦',
    description: 'Thắc mắc về đổi trả hàng khi mua online',
    assignedTo: 'Nhân viên bán hàng online',
    // CHUYỂN ĐẾN SALE STAFF
    route: '/sale-staff/chat'
  },
  {
    value: 'HOAN_HANG_TAI_QUAY',
    label: 'Hoàn hàng - Mua tại cửa hàng',
    icon: '🏪',
    description: 'Thắc mắc về đổi trả hàng khi mua tại cửa hàng',
    assignedTo: 'Nhân viên bán hàng tại quầy',
    // CHUYỂN ĐẾN STAFF POS
    route: '/staff/chat'
  },
  {
    value: 'KHAC',
    label: 'Lý do khác',
    icon: '❓',
    description: 'Các vấn đề khác không thuộc các mục trên',
    assignedTo: 'Quản trị viên (Admin)',
    // CHUYỂN ĐẾN ADMIN
    route: '/admin/chat'
  }
]

const handleSelect = (category) => {
  emit('close')
  
  // Kiểm tra đăng nhập
  if (!authStore.isAuthenticated) {
    router.push({ 
      name: 'login', 
      query: { redirect: category.route } 
    })
    return
  }
  
  // Chuyển hướng đến trang chat tương ứng
  router.push(category.route)
}

const close = () => {
  emit('close')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  backdrop-filter: blur(5px);
}

.modal-container {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  color: #1a1a2e;
  font-size: 22px;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  transition: all 0.3s;
}

.close-btn:hover {
  color: #e63946;
  transform: rotate(90deg);
}

.modal-body {
  padding: 24px;
}

.subtitle {
  color: #666;
  margin-bottom: 20px;
  font-size: 14px;
}

.category-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border: 2px solid #eee;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
  width: 100%;
}

.category-card:hover {
  border-color: #e63946;
  background: #fef5f5;
  transform: translateX(4px);
  box-shadow: 0 4px 15px rgba(230, 57, 70, 0.1);
}

.category-card:active {
  transform: scale(0.98);
}

.category-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.category-info {
  flex: 1;
}

.category-info h4 {
  margin: 0;
  color: #1a1a2e;
  font-size: 16px;
}

.category-info p {
  margin: 4px 0;
  color: #666;
  font-size: 13px;
}

.category-role {
  display: inline-block;
  margin-top: 4px;
  font-size: 12px;
  color: #6c5ce7;
  background: #f3f0ff;
  padding: 2px 12px;
  border-radius: 12px;
}

.arrow-icon {
  color: #ccc;
  font-size: 18px;
  transition: all 0.3s;
}

.category-card:hover .arrow-icon {
  color: #e63946;
  transform: translateX(4px);
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-outline {
  background: #f5f5f5;
  color: #666;
}

.btn-outline:hover {
  background: #eee;
}

@media (max-width: 600px) {
  .category-card {
    flex-direction: column;
    text-align: center;
  }
  
  .category-role {
    display: inline-block;
  }
  
  .arrow-icon {
    display: none;
  }
}
</style>