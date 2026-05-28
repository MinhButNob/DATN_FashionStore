<template>
  <div class="admin-login-page">
    <div class="login-container">
      <div class="login-card">
        <!-- Logo -->
        <div class="login-logo">
          <h2>FASHION STORE</h2>
          <p>Admin Panel</p>
        </div>

        <h1 class="login-title">Đăng nhập quản trị</h1>

        <!-- Error Alert -->
        <div v-if="error" class="alert alert-error">
          <i class="fas fa-exclamation-circle"></i>
          {{ error }}
        </div>

        <!-- Login Form -->
        <form @submit.prevent="handleLogin" class="login-form">
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
            <label>Mật khẩu</label>
            <div class="input-wrapper">
              <i class="fas fa-lock"></i>
              <input 
                :type="showPassword ? 'text' : 'password'" 
                v-model="form.password" 
                placeholder="Nhập mật khẩu"
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

          <div class="form-options">
            <label class="checkbox">
              <input type="checkbox" v-model="form.remember">
              <span>Ghi nhớ đăng nhập</span>
            </label>
            <router-link to="/forgot-password" class="forgot-link">
              Quên mật khẩu?
            </router-link>
          </div>

          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="!loading">ĐĂNG NHẬP</span>
            <span v-else><i class="fas fa-spinner fa-spin"></i> Đang xử lý...</span>
          </button>
        </form>

        <!-- Demo Accounts -->
        <div class="demo-accounts">
          <p class="demo-title">Tài khoản demo:</p>
          <div class="demo-account" @click="fillAdminAccount">
            <strong>Admin:</strong> admin@fashionstore.vn / admin123
          </div>
          <div class="demo-account" @click="fillManagerAccount">
            <strong>Manager:</strong> manager@fashionstore.vn / manager123
          </div>
        </div>

        <!-- Back to Home -->
        <div class="back-home">
          <router-link to="/">
            <i class="fas fa-arrow-left"></i>
            Quay lại trang chủ
          </router-link>
        </div>
      </div>

      <!-- Features -->
      <div class="login-features">
        <div class="feature-item">
          <i class="fas fa-chart-line"></i>
          <div>
            <h3>Quản lý doanh thu</h3>
            <p>Theo dõi doanh thu theo thời gian thực</p>
          </div>
        </div>
        <div class="feature-item">
          <i class="fas fa-boxes"></i>
          <div>
            <h3>Quản lý sản phẩm</h3>
            <p>Thêm, sửa, xóa sản phẩm dễ dàng</p>
          </div>
        </div>
        <div class="feature-item">
          <i class="fas fa-shopping-cart"></i>
          <div>
            <h3>Quản lý đơn hàng</h3>
            <p>Theo dõi và xử lý đơn hàng nhanh chóng</p>
          </div>
        </div>
        <div class="feature-item">
          <i class="fas fa-users"></i>
          <div>
            <h3>Quản lý khách hàng</h3>
            <p>Xem thông tin và lịch sử mua hàng</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/authStore'
import { useNotification } from '../../composables/useNotification'

const router = useRouter()
const authStore = useAuthStore()
const { showNotification } = useNotification()

const form = ref({
  email: '',
  password: '',
  remember: false
})

const loading = ref(false)
const error = ref('')
const showPassword = ref(false)

const handleLogin = async () => {
  error.value = ''
  loading.value = true

  try {
    await authStore.login(form.value.email, form.value.password)
    
    // Check if user is admin
    if (!authStore.isAdmin) {
      authStore.logout()
      throw new Error('Tài khoản không có quyền truy cập admin')
    }

    // Save remember me preference
    if (form.value.remember) {
      localStorage.setItem('adminRememberMe', 'true')
    }

    showNotification('Đăng nhập thành công!', 'success')
    
    // Redirect to admin dashboard
    const redirect = router.currentRoute.value.query.redirect || '/admin'
    router.push(redirect)

  } catch (err) {
    error.value = err.message || 'Đăng nhập thất bại'
    showNotification(error.value, 'error')
  } finally {
    loading.value = false
  }
}

const fillAdminAccount = () => {
  form.value.email = 'admin@fashionstore.vn'
  form.value.password = 'admin123'
}

const fillManagerAccount = () => {
  form.value.email = 'manager@fashionstore.vn'
  form.value.password = 'manager123'
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-container {
  display: grid;
  grid-template-columns: 400px 300px;
  gap: 30px;
  max-width: 800px;
  width: 100%;
}

.login-card {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.2);
}

.login-logo {
  text-align: center;
  margin-bottom: 30px;
}

.login-logo h2 {
  font-size: 28px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 5px;
}

.login-logo p {
  color: var(--medium-gray);
  font-size: 14px;
}

.login-title {
  text-align: center;
  font-size: 20px;
  color: var(--dark-gray);
  margin-bottom: 25px;
}

/* Alert */
.alert {
  padding: 12px 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.alert-error {
  background: #fee;
  color: #dc3545;
  border: 1px solid #fcc;
}

.alert i {
  font-size: 16px;
}

/* Form */
.login-form {
  margin-bottom: 25px;
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
  padding: 12px 12px 12px 45px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  font-size: 14px;
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

/* Form Options */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
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

.forgot-link {
  color: var(--primary-red);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.forgot-link:hover {
  text-decoration: underline;
}

/* Login Button */
.login-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.login-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--dark-red), var(--primary-red));
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(230, 57, 70, 0.3);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Demo Accounts */
.demo-accounts {
  background: var(--light-gray);
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.demo-title {
  color: var(--dark-gray);
  font-weight: 600;
  margin-bottom: 10px;
  font-size: 14px;
}

.demo-account {
  padding: 8px 12px;
  background: white;
  border: 1px solid var(--border-color);
  border-radius: 5px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--medium-gray);
  cursor: pointer;
  transition: all 0.3s;
}

.demo-account:last-child {
  margin-bottom: 0;
}

.demo-account:hover {
  border-color: var(--primary-red);
  background: rgba(230, 57, 70, 0.05);
}

.demo-account strong {
  color: var(--primary-red);
}

/* Back Home */
.back-home {
  text-align: center;
}

.back-home a {
  color: var(--medium-gray);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.back-home a:hover {
  color: var(--primary-red);
}

/* Features */
.login-features {
  background: white;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.2);
  height: fit-content;
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
@media (max-width: 768px) {
  .login-container {
    grid-template-columns: 1fr;
    max-width: 400px;
  }

  .login-card {
    padding: 30px 20px;
  }

  .form-options {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .demo-accounts {
    padding: 12px;
  }

  .login-features {
    display: none;
  }
}
</style>