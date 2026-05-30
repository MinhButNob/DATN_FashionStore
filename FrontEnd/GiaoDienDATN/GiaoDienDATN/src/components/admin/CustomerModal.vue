<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal-container">
      <div class="modal-header">
        <h3>{{ isEdit ? 'Cập nhật khách hàng' : 'Thêm khách hàng mới' }}</h3>
        <button class="close-btn" @click="close">&times;</button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>Họ tên *</label>
            <input type="text" v-model="form.customerName" class="form-control" required />
          </div>
          <div class="form-group">
            <label>Email *</label>
            <input type="email" v-model="form.email" class="form-control" :disabled="isEdit" required />
            <small v-if="isEdit" class="text-muted">Email không thể thay đổi</small>
          </div>
          <div class="form-group" v-if="!isEdit">
            <label>Mật khẩu *</label>
            <input type="password" v-model="form.password" class="form-control" required />
          </div>
          <div class="form-group">
            <label>Số điện thoại</label>
            <input type="tel" v-model="form.phone" class="form-control" />
          </div>
          <div class="form-group">
            <label>Địa chỉ</label>
            <textarea v-model="form.address" class="form-control" rows="2"></textarea>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-secondary" @click="close">Hủy</button>
            <button type="submit" class="btn-primary" :disabled="submitting">
              {{ submitting ? 'Đang xử lý...' : (isEdit ? 'Cập nhật' : 'Thêm mới') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: Boolean,
  isEdit: Boolean,
  initialData: Object
})

const emit = defineEmits(['close', 'submit'])

const form = ref({
  customerName: '',
  email: '',
  password: '',
  phone: '',
  address: ''
})

const submitting = ref(false)

watch(() => props.initialData, (newData) => {
  if (newData) {
    form.value = {
      customerName: newData.customerName || '',
      email: newData.email || '',
      password: '',
      phone: newData.phone || '',
      address: newData.address || ''
    }
  }
}, { immediate: true })

watch(() => props.visible, (newVal) => {
  if (!newVal) {
    form.value = {
      customerName: '',
      email: '',
      password: '',
      phone: '',
      address: ''
    }
    submitting.value = false
  }
})

const close = () => emit('close')

const handleSubmit = async () => {
  submitting.value = true
  try {
    await emit('submit', { ...form.value })
    close()
  } catch (error) {
    console.error('Submit error:', error)
  } finally {
    submitting.value = false
  }
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
  z-index: 1000;
}
.modal-container {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.modal-header h3 {
  font-size: 20px;
  color: #333;
  margin: 0;
}
.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}
.modal-body {
  padding: 25px;
}
.form-group {
  margin-bottom: 15px;
}
.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #555;
}
.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}
.form-control:focus {
  outline: none;
  border-color: #e63946;
}
textarea.form-control {
  resize: vertical;
}
.text-muted {
  font-size: 12px;
  color: #999;
}
.modal-footer {
  padding: 15px 0 0;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.btn-primary, .btn-secondary {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}
.btn-primary {
  background: #e63946;
  color: white;
}
.btn-primary:hover {
  background: #c82333;
}
.btn-secondary {
  background: #f5f5f5;
  color: #666;
}
.btn-secondary:hover {
  background: #e0e0e0;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>