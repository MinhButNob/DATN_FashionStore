CusModal
<template>
  <div v-if="visible" class="modal-overlay">
    <div class="modal-container">
      <div class="modal-header">
        <h3>{{ isEdit ? 'Cập nhật trạng thái khách hàng' : 'Thêm khách hàng mới' }}</h3>
        <button class="close-btn" @click="close">&times;</button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <!-- Khi ở chế độ SỬA: Chỉ hiển thị thông tin và trạng thái -->
          <template v-if="isEdit">
            <!-- Hiển thị thông tin khách hàng (chỉ đọc) -->
            <div class="customer-info-box">
              <div class="info-row">
                <span class="info-label">Tên khách hàng:</span>
                <span class="info-value">{{ form.customerName || '---' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">Email:</span>
                <span class="info-value">{{ form.email || '---' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">Số điện thoại:</span>
                <span class="info-value">{{ form.phone || '---' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">Địa chỉ:</span>
                <span class="info-value">{{ form.address || '---' }}</span>
              </div>
              <div class="info-divider"></div>
            </div>

            <!-- Chỉ có trạng thái để chỉnh sửa -->
            <div class="form-group">
              <label>Trạng thái <span class="required">*</span></label>
              <select v-model="form.isLocked" class="form-control" required>
                <option :value="false">Hoạt động</option>
                <option :value="true">Tạm khóa</option>
              </select>
              <small class="hint-text">
                {{ form.isLocked ? '🔒 Tài khoản sẽ bị khóa, khách hàng không thể đăng nhập' : '✅ Tài khoản hoạt động bình thường' }}
              </small>
            </div>
          </template>

          <!-- Khi ở chế độ THÊM MỚI: Hiển thị tất cả trường -->
          <template v-else>
            <!-- Tên khách hàng -->
            <div class="form-group">
              <label>Tên khách hàng <span class="required">*</span></label>
              <input 
                type="text" 
                v-model="form.customerName" 
                class="form-control" 
                placeholder="Nhập tên khách hàng"
                required
              />
            </div>

            <!-- Email -->
            <div class="form-group">
              <label>Email <span class="required">*</span></label>
              <input 
                type="email" 
                v-model="form.email" 
                class="form-control" 
                placeholder="Nhập email"
                required
              />
            </div>

            <!-- Mật khẩu -->
            <div class="form-group">
              <label>Mật khẩu <span class="required">*</span></label>
              <div class="password-input-wrapper">
                <input 
                  :type="showPassword ? 'text' : 'password'" 
                  v-model="form.password" 
                  class="form-control" 
                  placeholder="Nhập mật khẩu (ít nhất 6 ký tự)"
                  required
                  minlength="6"
                />
                <i 
                  :class="showPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"
                  @click="showPassword = !showPassword"
                  class="toggle-password"
                ></i>
              </div>
              <small class="hint-text">Mật khẩu phải có ít nhất 6 ký tự</small>
            </div>

            <!-- Số điện thoại -->
            <div class="form-group">
              <label>Số điện thoại</label>
              <input 
                type="tel" 
                v-model="form.phone" 
                class="form-control" 
                placeholder="Nhập số điện thoại"
              />
            </div>

            <!-- Tỉnh/Thành phố -->
            <div class="form-group">
              <label>Tỉnh/Thành phố</label>
              <select v-model="form.provinceId" class="form-control" @change="onProvinceChange">
                <option :value="null">-- Chọn tỉnh/thành phố --</option>
                <option v-for="province in provinces" :key="province.code" :value="province.code">
                  {{ province.name }}
                </option>
              </select>
            </div>

            <!-- Phường/Xã -->
            <div class="form-group">
              <label>Phường/Xã</label>
              <select v-model="form.wardId" class="form-control" :disabled="!form.provinceId">
                <option :value="null">-- Chọn phường/xã --</option>
                <option v-for="ward in wards" :key="ward.code" :value="ward.code">
                  {{ ward.name }}
                </option>
              </select>
            </div>

            <!-- Địa chỉ cụ thể -->
            <div class="form-group">
              <label>Số nhà, tên đường (Địa chỉ cụ thể)</label>
              <input 
                type="text" 
                v-model="form.streetAddress" 
                class="form-control" 
                placeholder="VD: 123 Trịnh Văn Bô"
              />
            </div>
          </template>

          <!-- Footer -->
          <div class="modal-footer">
            <button type="button" class="btn-secondary" @click="close">Hủy</button>
            <button type="submit" class="btn-primary" :disabled="submitting">
              <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
              {{ submitting ? 'Đang xử lý...' : (isEdit ? 'Cập nhật trạng thái' : 'Thêm mới') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { useVietnamAddress } from '../../composables/useVietnamAddress'

const props = defineProps({
  visible: Boolean,
  initialData: Object
})

const emit = defineEmits(['close', 'submit'])

const submitting = ref(false)
const showPassword = ref(false)

const { provinces, wards, fetchProvinces, fetchWards } = useVietnamAddress()

// Load provinces khi modal mở
watch(() => props.visible, async (newVal) => {
  if (newVal && provinces.value.length === 0) {
    await fetchProvinces()
  }
})

// Kiểm tra là edit hay add mới
const isEdit = computed(() => !!props.initialData?.customerId)

// Form data
const form = ref({
  customerId: null,
  customerName: '',
  email: '',
  password: '',
  phone: '',
  address: '',
  streetAddress: '',
  provinceId: null,
  wardId: null,
  isLocked: false
})

// Reset form
const resetForm = () => {
  form.value = {
    customerId: null,
    customerName: '',
    email: '',
    password: '',
    phone: '',
    address: '',
    streetAddress: '',
    provinceId: null,
    wardId: null,
    isLocked: false
  }
  submitting.value = false
  showPassword.value = false
  wards.value = []
}

// Watch initialData để populate form khi edit
watch(() => props.initialData, (newData) => {
  if (newData) {
    form.value = {
      customerId: newData.customerId || null,
      customerName: newData.customerName || '',
      email: newData.email || '',
      password: '',
      phone: newData.phone || '',
      address: newData.address || '',
      streetAddress: newData.address || '', // Fallback address vào ô street
      provinceId: null, // Không có ID từ backend cho Customer
      wardId: null,
      isLocked: !!newData.isLocked
    }
    wards.value = []
  } else {
    resetForm()
  }
}, { immediate: true })

const onProvinceChange = async () => {
  form.value.wardId = null
  if (form.value.provinceId) {
    await fetchWards(form.value.provinceId)
  } else {
    wards.value = []
  }
}

// Watch visible để reset khi đóng modal
watch(() => props.visible, (newVal) => {
  if (!newVal) {
    resetForm()
  }
})

const close = () => {
  resetForm()
  emit('close')
}

const handleSubmit = async () => {
  // Validate
  if (!isEdit.value) {
    // Validate cho thêm mới
    if (!form.value.customerName || form.value.customerName.trim() === '') {
      alert('Vui lòng nhập tên khách hàng')
      return
    }

    if (!form.value.email || form.value.email.trim() === '') {
      alert('Vui lòng nhập email')
      return
    }

    if (!form.value.password || form.value.password.length < 6) {
      alert('Mật khẩu phải có ít nhất 6 ký tự')
      return
    }
  } else {
    // Validate cho sửa (chỉ kiểm tra trạng thái)
    if (form.value.isLocked === undefined || form.value.isLocked === null) {
      alert('Vui lòng chọn trạng thái')
      return
    }
  }

  submitting.value = true
  try {
    let submitData = {}

    if (!isEdit.value) {
      // Dữ liệu thêm mới
      // Ghép chuỗi địa chỉ
      let fullAddress = form.value.streetAddress ? form.value.streetAddress.trim() : ''
      if (form.value.wardId) {
        const w = wards.value.find(w => w.code === form.value.wardId)
        if (w) fullAddress += `, ${w.name}`
      }
      if (form.value.provinceId) {
        const p = provinces.value.find(p => p.code === form.value.provinceId)
        if (p) fullAddress += `, ${p.name}`
      }

      submitData = {
        customerName: form.value.customerName.trim(),
        email: form.value.email.trim(),
        password: form.value.password,
        phone: form.value.phone ? form.value.phone.trim() : null,
        address: fullAddress || null
      }
    } else {
      // Dữ liệu cập nhật trạng thái
      submitData = {
        customerId: form.value.customerId,
        isLocked: form.value.isLocked
      }
    }

    await emit('submit', submitData)
    // Không reset ở đây, để parent xử lý
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
  animation: fadeIn 0.3s ease;
}

.modal-container {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #999;
  transition: color 0.3s;
  padding: 0 5px;
  line-height: 1;
}

.close-btn:hover {
  color: #e63946;
}

.modal-body {
  padding: 25px;
}

/* Customer Info Box - Chỉ hiển thị khi edit */
.customer-info-box {
  background: #f8f9fa;
  padding: 16px 18px;
  border-radius: 10px;
  margin-bottom: 24px;
  border-left: 4px solid #e63946;
}

.info-row {
  display: flex;
  padding: 4px 0;
  font-size: 14px;
}

.info-label {
  font-weight: 600;
  color: #555;
  min-width: 120px;
  flex-shrink: 0;
}

.info-value {
  color: #333;
  word-break: break-word;
}

.info-divider {
  height: 1px;
  background: #e0e6ed;
  margin: 10px 0;
}

/* Form Group */
.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #555;
  font-size: 14px;
}

.required {
  color: #e63946;
}

.form-control {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s, box-shadow 0.3s;
  outline: none;
  box-sizing: border-box;
}

.form-control:focus {
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.form-control:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
  opacity: 0.7;
}

.password-input-wrapper {
  position: relative;
}

.password-input-wrapper .form-control {
  padding-right: 40px;
}

.toggle-password {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: #999;
  font-size: 18px;
  transition: color 0.3s;
}

.toggle-password:hover {
  color: #333;
}

.hint-text {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #6c757d;
}

.hint-text i {
  margin-right: 4px;
}

.modal-footer {
  padding-top: 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-primary,
.btn-secondary {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c62828;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(230, 57, 70, 0.3);
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
  transform: none !important;
}

.btn-primary i {
  margin-right: 6px;
}

/* Animations */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* Responsive */
@media (max-width: 600px) {
  .modal-container {
    width: 95%;
    margin: 10px;
  }
  
  .modal-header {
    padding: 15px 20px;
  }
  
  .modal-body {
    padding: 20px;
  }
  
  .modal-footer {
    flex-direction: column;
  }
  
  .modal-footer .btn-primary,
  .modal-footer .btn-secondary {
    width: 100%;
  }

  .info-row {
    flex-direction: column;
    padding: 2px 0;
  }

  .info-label {
    min-width: auto;
    font-size: 12px;
    color: #888;
  }
}
</style>