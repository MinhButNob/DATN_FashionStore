<template>
  <div class="auth-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <div class="reset-container">
          <div class="auth-card">
            <div class="auth-logo">
              <router-link to="/">FASHION STORE</router-link>
            </div>

            <h1 class="auth-title">Đặt lại mật khẩu</h1>
            <p class="auth-subtitle">Nhập mật khẩu mới của bạn</p>

            <div v-if="error" class="alert alert-error">
              <i class="fas fa-exclamation-circle"></i>
              {{ error }}
            </div>

            <div v-if="success" class="alert alert-success">
              <i class="fas fa-check-circle"></i>
              {{ success }}
            </div>

            <form @submit.prevent="handleResetPassword" class="auth-form">
              <div class="form-group">
                <label>Mật khẩu mới</label>
                <div class="input-wrapper">
                  <i class="fas fa-lock"></i>
                  <input 
                    :type="showPassword ? 'text' : 'password'" 
                    v-model="form.password" 
                    placeholder="Nhập mật khẩu mới"
                    required
                    :disabled="loading"
                  >
                  <i 
                    class="fas fa-eye toggle-password" 
                    :class="{ 'fa-eye-slash': showPassword }"
                    @click="showPassword = !showPassword"
                  ></i>
                </div>
              </div>

              <div class="form-group">
                <label>Xác nhận mật khẩu</label>
                <div class="input-wrapper">
                  <i class="fas fa-lock"></i>
                  <input 
                    :type="showConfirmPassword ? 'text' : 'password'" 
                    v-model="form.confirmPassword" 
                    placeholder="Xác nhận mật khẩu mới"
                    required
                    :disabled="loading"
                  >
                  <i 
                    class="fas fa-eye toggle-password" 
                    :class="{ 'fa-eye-slash': showConfirmPassword }"
                    @click="showConfirmPassword = !showConfirmPassword"
                  ></i>
                </div>
              </div>

              <div class="password-requirements">
                <p class="requirements-title">Mật khẩu phải có:</p>
                <ul>
                  <li :class="{ valid: form.password.length >= 6 }">
                    <i :class="form.password.length >= 6 ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Ít nhất 6 ký tự
                  </li>
                  <li :class="{ valid: /[A-Z]/.test(form.password) }">
                    <i :class="/[A-Z]/.test(form.password) ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Ít nhất 1 chữ hoa
                  </li>
                  <li :class="{ valid: /[0-9]/.test(form.password) }">
                    <i :class="/[0-9]/.test(form.password) ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Ít nhất 1 số
                  </li>
                  <li :class="{ valid: form.password && form.confirmPassword && form.password === form.confirmPassword }">
                    <i :class="form.password && form.confirmPassword && form.password === form.confirmPassword ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Mật khẩu xác nhận khớp
                  </li>
                </ul>
              </div>

              <button type="submit" class="auth-btn" :disabled="loading || !isFormValid">
                <span v-if="!loading">ĐẶT LẠI MẬT KHẨU</span>
                <span v-else><i class="fas fa-spinner fa-spin"></i> Đang xử lý...</span>
              </button>
            </form>

            <div class="back-to-login">
              <router-link to="/login">
                <i class="fas fa-arrow-left"></i>
                Quay lại đăng nhập
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'

const route = useRoute()
const router = useRouter()
const { showNotification } = useNotification()

const form = ref({
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const error = ref('')
const success = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)

const isFormValid = computed(() => {
  return form.value.password.length >= 6 &&
         /[A-Z]/.test(form.value.password) &&
         /[0-9]/.test(form.value.password) &&
         form.value.password === form.value.confirmPassword
})

const handleResetPassword = async () => {
  error.value = ''
  loading.value = true

  try {
    const token = route.params.token
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1500))

    success.value = 'Đặt lại mật khẩu thành công!'
    showNotification('Mật khẩu đã được cập nhật!', 'success')

    setTimeout(() => {
      router.push('/login')
    }, 2000)

  } catch (err) {
    error.value = 'Có lỗi xảy ra. Vui lòng thử lại.'
    showNotification(error.value, 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.reset-container {
  max-width: 450px;
  margin: 0 auto;
}

/* Reuse styles from ForgotPasswordPage */
</style>