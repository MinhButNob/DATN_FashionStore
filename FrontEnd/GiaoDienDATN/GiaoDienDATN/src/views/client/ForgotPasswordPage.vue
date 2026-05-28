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

            <div v-if="!emailSent">
              <h1 class="auth-title">Quên mật khẩu?</h1>
              <p class="auth-subtitle">
                Nhập email của bạn để nhận hướng dẫn đặt lại mật khẩu
              </p>

              <!-- Error Alert -->
              <div v-if="error" class="alert alert-error">
                <i class="fas fa-exclamation-circle"></i>
                {{ error }}
              </div>

              <!-- Forgot Password Form -->
              <form @submit.prevent="handleForgotPassword" class="auth-form">
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

                <button type="submit" class="auth-btn" :disabled="loading">
                  <span v-if="!loading">GỬI YÊU CẦU</span>
                  <span v-else><i class="fas fa-spinner fa-spin"></i> Đang gửi...</span>
                </button>
              </form>

              <!-- Back to Login -->
              <div class="back-to-login">
                <router-link to="/login">
                  <i class="fas fa-arrow-left"></i>
                  Quay lại đăng nhập
                </router-link>
              </div>
            </div>

            <!-- Success State -->
            <div v-else class="success-state">
              <div class="success-icon">
                <i class="fas fa-envelope-open-text"></i>
              </div>
              <h2 class="success-title">Kiểm tra email của bạn</h2>
              <p class="success-message">
                Chúng tôi đã gửi hướng dẫn đặt lại mật khẩu đến
                <strong>{{ form.email }}</strong>
              </p>
              <div class="success-actions">
                <button class="btn btn-primary" @click="openEmailClient">
                  Mở email
                </button>
                <button class="btn btn-outline" @click="resendEmail">
                  Gửi lại
                </button>
              </div>
              <p class="resend-text">
                Không nhận được email? 
                <a href="#" @click.prevent="resendEmail">Gửi lại</a>
              </p>
              <router-link to="/login" class="back-link">
                <i class="fas fa-arrow-left"></i>
                Quay lại đăng nhập
              </router-link>
            </div>
          </div>

          <!-- Help Tips -->
          <div class="help-tips">
            <h3 class="tips-title">Mẹo hữu ích</h3>
            <div class="tip-item">
              <i class="fas fa-search"></i>
              <div>
                <h4>Kiểm tra spam</h4>
                <p>Email có thể nằm trong thư mục spam</p>
              </div>
            </div>
            <div class="tip-item">
              <i class="fas fa-clock"></i>
              <div>
                <h4>Thời gian chờ</h4>
                <p>Email thường đến trong vòng 5 phút</p>
              </div>
            </div>
            <div class="tip-item">
              <i class="fas fa-lock"></i>
              <div>
                <h4>Bảo mật</h4>
                <p>Không chia sẻ link đặt lại mật khẩu</p>
              </div>
            </div>
            <div class="tip-item">
              <i class="fas fa-headset"></i>
              <div>
                <h4>Cần hỗ trợ?</h4>
                <p>Liên hệ: support@fashionstore.vn</p>
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
import { ref } from 'vue'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'

const { showNotification } = useNotification()

const form = ref({
  email: ''
})

const loading = ref(false)
const error = ref('')
const emailSent = ref(false)

const handleForgotPassword = async () => {
  error.value = ''
  loading.value = true

  try {
    // Validate email
    if (!form.value.email) {
      throw new Error('Vui lòng nhập email')
    }

    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1500))

    // Check if email exists (mock)
    const existingEmails = ['user@example.com', 'admin@fashionstore.vn']
    if (!existingEmails.includes(form.value.email)) {
      throw new Error('Email không tồn tại trong hệ thống')
    }

    // Show success
    emailSent.value = true
    showNotification('Email đã được gửi! Vui lòng kiểm tra hộp thư.', 'success')

  } catch (err) {
    error.value = err.message || 'Có lỗi xảy ra. Vui lòng thử lại.'
    showNotification(error.value, 'error')
  } finally {
    loading.value = false
  }
}

const resendEmail = async () => {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1000))
    showNotification('Đã gửi lại email!', 'success')
  } finally {
    loading.value = false
  }
}

const openEmailClient = () => {
  // Open default email client
  window.location.href = `mailto:${form.value.email}`
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
  max-width: 900px;
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

/* Form */
.auth-form {
  margin-bottom: 30px;
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

.input-wrapper i {
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

/* Back to Login */
.back-to-login {
  text-align: center;
}

.back-to-login a {
  color: var(--medium-gray);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.back-to-login a:hover {
  color: var(--primary-red);
}

/* Success State */
.success-state {
  text-align: center;
}

.success-icon {
  font-size: 60px;
  color: #28a745;
  margin-bottom: 20px;
}

.success-title {
  color: var(--dark-gray);
  font-size: 24px;
  margin-bottom: 10px;
}

.success-message {
  color: var(--medium-gray);
  margin-bottom: 25px;
  line-height: 1.6;
}

.success-message strong {
  color: var(--primary-red);
}

.success-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-bottom: 20px;
}

.resend-text {
  color: var(--medium-gray);
  font-size: 14px;
  margin-bottom: 15px;
}

.resend-text a {
  color: var(--primary-red);
  text-decoration: none;
  font-weight: 600;
}

.resend-text a:hover {
  text-decoration: underline;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--medium-gray);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.back-link:hover {
  color: var(--primary-red);
}

/* Help Tips */
.help-tips {
  background: white;
  border-radius: 20px;
  padding: 30px;
  box-shadow: var(--shadow);
  height: fit-content;
}

.tips-title {
  color: var(--dark-gray);
  font-size: 18px;
  margin-bottom: 20px;
  text-align: center;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.tip-item:last-child {
  border-bottom: none;
}

.tip-item i {
  font-size: 24px;
  color: var(--primary-red);
  width: 40px;
  text-align: center;
}

.tip-item h4 {
  color: var(--dark-gray);
  font-size: 14px;
  margin-bottom: 3px;
}

.tip-item p {
  color: var(--medium-gray);
  font-size: 12px;
}

/* Responsive */
@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .auth-container {
    grid-template-columns: 1fr;
    max-width: 400px;
  }

  .auth-card {
    padding: 30px 20px;
  }

  .auth-title {
    font-size: 24px;
  }

  .success-actions {
    flex-direction: column;
  }

  .success-actions button {
    width: 100%;
  }
}
</style>