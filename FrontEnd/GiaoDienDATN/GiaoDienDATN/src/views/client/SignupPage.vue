<template>
  <div class="auth-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <div class="auth-container">
          <div class="auth-card">
            <!-- Logo -->
            <div class="auth-logo">
              <router-link to="/">FASHION STORE</router-link>
            </div>

            <h1 class="auth-title">Đăng ký tài khoản</h1>
            <p class="auth-subtitle">Tham gia để nhận nhiều ưu đãi</p>

            <!-- Error Alert -->
            <div v-if="error" class="alert alert-error">
              <i class="fas fa-exclamation-circle"></i>
              {{ error }}
            </div>

            <!-- Success Alert -->
            <div v-if="success" class="alert alert-success">
              <i class="fas fa-check-circle"></i>
              {{ success }}
            </div>

            <!-- Register Form -->
            <form @submit.prevent="handleRegister" class="auth-form">
              <div class="form-row">
                <div class="form-group">
                  <label>Họ</label>
                  <div class="input-wrapper">
                    <i class="fas fa-user"></i>
                    <input 
                      type="text" 
                      v-model="form.firstName" 
                      placeholder="Họ"
                      required
                      :disabled="loading"
                    >
                  </div>
                </div>

                <div class="form-group">
                  <label>Tên</label>
                  <div class="input-wrapper">
                    <i class="fas fa-user"></i>
                    <input 
                      type="text" 
                      v-model="form.lastName" 
                      placeholder="Tên"
                      required
                      :disabled="loading"
                    >
                  </div>
                </div>
              </div>

              <div class="form-group">
                <label>Email</label>
                <div class="input-wrapper">
                  <i class="fas fa-envelope"></i>
                  <input 
                    type="email" 
                    v-model="form.email" 
                    placeholder="Nhập email của bạn"
                    required
                    :disabled="loading"
                  >
                </div>
              </div>

              <div class="form-group">
                <label>Số điện thoại</label>
                <div class="input-wrapper">
                  <i class="fas fa-phone"></i>
                  <input 
                    type="tel" 
                    v-model="form.phone" 
                    placeholder="Nhập số điện thoại"
                    required
                    :disabled="loading"
                  >
                </div>
              </div>

              <div class="form-group">
                <label>Mật khẩu</label>
                <div class="input-wrapper">
                  <i class="fas fa-lock"></i>
                  <input 
                    :type="showPassword ? 'text' : 'password'" 
                    v-model="form.password" 
                    placeholder="Tạo mật khẩu"
                    required
                    :disabled="loading"
                    @input="validatePassword"
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
                    placeholder="Nhập lại mật khẩu"
                    required
                    :disabled="loading"
                    @input="validatePassword"
                  >
                  <i 
                    class="fas fa-eye toggle-password" 
                    :class="{ 'fa-eye-slash': showConfirmPassword }"
                    @click="showConfirmPassword = !showConfirmPassword"
                  ></i>
                </div>
              </div>

              <!-- Password Strength -->
              <div v-if="form.password" class="password-strength">
                <div class="strength-bar">
                  <div 
                    class="strength-progress" 
                    :style="{ width: passwordStrength + '%' }"
                    :class="strengthClass"
                  ></div>
                </div>
                <p class="strength-text" :class="strengthClass">
                  {{ strengthText }}
                </p>
              </div>

              <!-- Password Requirements -->
              <div class="password-requirements">
                <p class="requirements-title">Mật khẩu phải có:</p>
                <ul>
                  <li :class="{ valid: hasMinLength }">
                    <i :class="hasMinLength ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Ít nhất 6 ký tự
                  </li>
                  <li :class="{ valid: hasUpperCase }">
                    <i :class="hasUpperCase ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Ít nhất 1 chữ hoa
                  </li>
                  <li :class="{ valid: hasLowerCase }">
                    <i :class="hasLowerCase ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Ít nhất 1 chữ thường
                  </li>
                  <li :class="{ valid: hasNumber }">
                    <i :class="hasNumber ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Ít nhất 1 số
                  </li>
                  <li :class="{ valid: hasMatch }">
                    <i :class="hasMatch ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Mật khẩu xác nhận khớp
                  </li>
                </ul>
              </div>

              <!-- Terms -->
              <div class="terms">
                <label class="checkbox">
                  <input type="checkbox" v-model="form.agreeTerms" required>
                  <span>
                    Tôi đồng ý với 
                    <a href="#" @click.prevent="showTerms">điều khoản</a> 
                    và 
                    <a href="#" @click.prevent="showPrivacy">chính sách bảo mật</a>
                  </span>
                </label>
              </div>

              <button 
                type="submit" 
                class="auth-btn" 
                :disabled="loading || !isFormValid"
              >
                <span v-if="!loading">ĐĂNG KÝ</span>
                <span v-else><i class="fas fa-spinner fa-spin"></i> Đang xử lý...</span>
              </button>
            </form>

            <!-- Social Register -->
            <div class="social-login">
              <p class="or-text">Hoặc đăng ký với</p>
              <div class="social-buttons">
                <button class="social-btn facebook" @click="socialLogin('facebook')" :disabled="loading">
                  <i class="fab fa-facebook-f"></i>
                  <span>Facebook</span>
                </button>
                <button class="social-btn google" @click="socialLogin('google')" :disabled="loading">
                  <i class="fab fa-google"></i>
                  <span>Google</span>
                </button>
              </div>
            </div>

            <!-- Login Link -->
            <div class="auth-footer">
              <p>Đã có tài khoản? 
                <router-link to="/login">Đăng nhập</router-link>
              </p>
            </div>
          </div>

          <!-- Benefits -->
          <div class="auth-features">
            <h3 class="benefits-title">Lợi ích khi đăng ký</h3>
            <div class="feature-item">
              <i class="fas fa-gift"></i>
              <div>
                <h3>Quà tặng chào mừng</h3>
                <p>Giảm 10% cho đơn hàng đầu tiên</p>
              </div>
            </div>
            <div class="feature-item">
              <i class="fas fa-star"></i>
              <div>
                <h3>Tích điểm thành viên</h3>
                <p>Nhận ưu đãi độc quyền</p>
              </div>
            </div>
            <div class="feature-item">
              <i class="fas fa-bell"></i>
              <div>
                <h3>Thông báo khuyến mãi</h3>
                <p>Cập nhật ưu đãi mới nhất</p>
              </div>
            </div>
            <div class="feature-item">
              <i class="fas fa-clock"></i>
              <div>
                <h3>Lịch sử mua hàng</h3>
                <p>Theo dõi đơn hàng dễ dàng</p>
              </div>
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
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/authStore'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'

const router = useRouter()
const authStore = useAuthStore()
const { showNotification } = useNotification()

const form = ref({
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  agreeTerms: false
})

const loading = ref(false)
const error = ref('')
const success = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)

// Computed
const fullName = computed(() => {
  return `${form.value.firstName} ${form.value.lastName}`.trim()
})

const hasMinLength = computed(() => form.value.password.length >= 6)
const hasUpperCase = computed(() => /[A-Z]/.test(form.value.password))
const hasLowerCase = computed(() => /[a-z]/.test(form.value.password))
const hasNumber = computed(() => /[0-9]/.test(form.value.password))
const hasMatch = computed(() => 
  form.value.password && 
  form.value.confirmPassword && 
  form.value.password === form.value.confirmPassword
)

const isFormValid = computed(() => {
  return hasMinLength.value &&
         hasUpperCase.value &&
         hasLowerCase.value &&
         hasNumber.value &&
         hasMatch.value &&
         form.value.agreeTerms
})

// Password strength
const passwordStrength = computed(() => {
  let strength = 0
  if (hasMinLength.value) strength += 25
  if (hasUpperCase.value) strength += 25
  if (hasLowerCase.value) strength += 25
  if (hasNumber.value) strength += 25
  return strength
})

const strengthClass = computed(() => {
  if (passwordStrength.value <= 25) return 'weak'
  if (passwordStrength.value <= 50) return 'fair'
  if (passwordStrength.value <= 75) return 'good'
  return 'strong'
})

const strengthText = computed(() => {
  const texts = {
    weak: 'Yếu',
    fair: 'Trung bình',
    good: 'Khá',
    strong: 'Mạnh'
  }
  return texts[strengthClass.value]
})

const validatePassword = () => {
  // Auto validate on input
}

const handleRegister = async () => {
  error.value = ''
  success.value = ''
  loading.value = true

  try {
    // Validate phone number (Vietnam format)
    const phoneRegex = /(84|0[3|5|7|8|9])+([0-9]{8})\b/
    if (!phoneRegex.test(form.value.phone)) {
      throw new Error('Số điện thoại không hợp lệ')
    }

    // Register
    await authStore.register({
      name: fullName.value,
      email: form.value.email,
      phone: form.value.phone,
      password: form.value.password
    })

    success.value = 'Đăng ký thành công! Đang chuyển hướng...'
    showNotification('Đăng ký thành công!', 'success')

    // Redirect to login after 2 seconds
    setTimeout(() => {
      router.push('/login')
    }, 2000)

  } catch (err) {
    error.value = err.message || 'Đăng ký thất bại. Vui lòng thử lại.'
    showNotification(error.value, 'error')
  } finally {
    loading.value = false
  }
}

const socialLogin = (provider) => {
  showNotification(`Đăng ký với ${provider} đang phát triển`, 'info')
}

const showTerms = () => {
  showNotification('Điều khoản sử dụng', 'info')
}

const showPrivacy = () => {
  showNotification('Chính sách bảo mật', 'info')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
}

main {
  margin-top: 140px;
  padding: 40px 0 80px;
}

.auth-container {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 30px;
  max-width: 1100px;
  margin: 0 auto;
}

.auth-card {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: var(--shadow);
}

.auth-logo {
  text-align: center;
  margin-bottom: 30px;
}

.auth-logo a {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-decoration: none;
}

.auth-title {
  text-align: center;
  font-size: 28px;
  color: var(--dark-gray);
  margin-bottom: 10px;
}

.auth-subtitle {
  text-align: center;
  color: var(--medium-gray);
  margin-bottom: 30px;
}

/* Alert */
.alert {
  padding: 15px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.alert-error {
  background: #fee;
  color: #dc3545;
  border: 1px solid #fcc;
}

.alert-success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.alert i {
  font-size: 18px;
}

/* Form */
.auth-form {
  margin-bottom: 30px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: var(--dark-gray);
  font-weight: 500;
  font-size: 14px;
}

.input-wrapper {
  position: relative;
}

.input-wrapper i:not(.toggle-password) {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--medium-gray);
  font-size: 16px;
}

.input-wrapper input {
  width: 100%;
  padding: 15px 15px 15px 45px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  font-size: 15px;
  transition: all 0.3s;
}

.input-wrapper input:focus {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.input-wrapper input:focus + i {
  color: var(--primary-red);
}

.input-wrapper input:disabled {
  background: var(--light-gray);
  cursor: not-allowed;
}

.toggle-password {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: var(--medium-gray);
  z-index: 2;
}

.toggle-password:hover {
  color: var(--primary-red);
}

/* Password Strength */
.password-strength {
  margin-bottom: 15px;
}

.strength-bar {
  height: 4px;
  background: var(--light-gray);
  border-radius: 2px;
  margin-bottom: 5px;
  overflow: hidden;
}

.strength-progress {
  height: 100%;
  transition: width 0.3s, background 0.3s;
}

.strength-progress.weak {
  background: #dc3545;
}

.strength-progress.fair {
  background: #ffc107;
}

.strength-progress.good {
  background: #17a2b8;
}

.strength-progress.strong {
  background: #28a745;
}

.strength-text {
  font-size: 13px;
}

.strength-text.weak {
  color: #dc3545;
}

.strength-text.fair {
  color: #ffc107;
}

.strength-text.good {
  color: #17a2b8;
}

.strength-text.strong {
  color: #28a745;
}

/* Password Requirements */
.password-requirements {
  background: var(--light-gray);
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.requirements-title {
  color: var(--dark-gray);
  font-weight: 600;
  margin-bottom: 10px;
  font-size: 14px;
}

.password-requirements ul {
  list-style: none;
}

.password-requirements li {
  margin-bottom: 5px;
  color: var(--medium-gray);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.password-requirements li i {
  font-size: 12px;
  color: var(--medium-gray);
}

.password-requirements li.valid {
  color: #28a745;
}

.password-requirements li.valid i {
  color: #28a745;
}

/* Terms */
.terms {
  margin-bottom: 20px;
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--medium-gray);
  font-size: 14px;
}

.checkbox input {
  accent-color: var(--primary-red);
  width: 16px;
  height: 16px;
}

.checkbox a {
  color: var(--primary-red);
  text-decoration: none;
}

.checkbox a:hover {
  text-decoration: underline;
}

/* Auth Button */
.auth-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.auth-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--dark-red), var(--primary-red));
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.3);
}

.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Social Login */
.social-login {
  margin-bottom: 25px;
}

.or-text {
  text-align: center;
  color: var(--medium-gray);
  margin-bottom: 15px;
  position: relative;
}

.or-text::before,
.or-text::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background: var(--border-color);
}

.or-text::before {
  left: 0;
}

.or-text::after {
  right: 0;
}

.social-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.social-btn {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-weight: 500;
}

.social-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.social-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.social-btn.facebook:hover {
  background: #1877f2;
  color: white;
  border-color: #1877f2;
}

.social-btn.google:hover {
  background: #ea4335;
  color: white;
  border-color: #ea4335;
}

/* Auth Footer */
.auth-footer {
  text-align: center;
  color: var(--medium-gray);
  font-size: 15px;
}

.auth-footer a {
  color: var(--primary-red);
  text-decoration: none;
  font-weight: 600;
}

.auth-footer a:hover {
  text-decoration: underline;
}

/* Benefits */
.auth-features {
  background: white;
  border-radius: 20px;
  padding: 30px;
  box-shadow: var(--shadow);
  height: fit-content;
}

.benefits-title {
  color: var(--dark-gray);
  font-size: 18px;
  margin-bottom: 20px;
  text-align: center;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid var(--border-color);
}

.feature-item:last-child {
  border-bottom: none;
}

.feature-item i {
  font-size: 30px;
  color: var(--primary-red);
  width: 50px;
  text-align: center;
}

.feature-item h3 {
  color: var(--dark-gray);
  font-size: 16px;
  margin-bottom: 3px;
}

.feature-item p {
  color: var(--medium-gray);
  font-size: 13px;
}

/* Responsive */
@media (max-width: 968px) {
  .auth-container {
    grid-template-columns: 1fr;
    max-width: 500px;
  }

  .auth-features {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }

  .feature-item {
    border-bottom: none;
    padding: 10px;
  }
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .auth-card {
    padding: 30px 20px;
  }

  .auth-title {
    font-size: 24px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .auth-features {
    grid-template-columns: 1fr;
  }

  .social-buttons {
    grid-template-columns: 1fr;
  }

  .password-requirements li {
    font-size: 12px;
  }
}
</style>