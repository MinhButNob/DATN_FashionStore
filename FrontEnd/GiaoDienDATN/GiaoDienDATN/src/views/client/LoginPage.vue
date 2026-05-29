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

            <h1 class="auth-title">Đăng nhập</h1>
            <p class="auth-subtitle">Chào mừng bạn trở lại!</p>

            <!-- Error Alert -->
            <div v-if="error" class="alert alert-error">
              <i class="fas fa-exclamation-circle"></i>
              {{ error }}
            </div>

            <!-- Login Form -->
            <form @submit.prevent="handleLogin" class="auth-form">
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
                  />
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
                  />
                  <i
                    class="fas fa-eye toggle-password"
                    :class="{ 'fa-eye-slash': showPassword }"
                    @click="showPassword = !showPassword"
                  ></i>
                </div>
              </div>

              <div class="form-options">
                <label class="checkbox">
                  <input type="checkbox" v-model="form.remember" />
                  <span>Ghi nhớ đăng nhập</span>
                </label>
                <router-link to="/forgot-password" class="forgot-link">
                  Quên mật khẩu?
                </router-link>
              </div>

              <button type="submit" class="auth-btn" :disabled="loading">
                <span v-if="!loading">ĐĂNG NHẬP</span>
                <span v-else
                  ><i class="fas fa-spinner fa-spin"></i> Đang xử lý...</span
                >
              </button>
            </form>

            <!-- Social Login -->
            <div class="social-login">
              <p class="or-text">Hoặc đăng nhập với</p>
              <div class="social-buttons">
                <button
                  class="social-btn facebook"
                  @click="socialLogin('facebook')"
                  :disabled="loading"
                >
                  <i class="fab fa-facebook-f"></i>
                  <span>Facebook</span>
                </button>
                <button
                  class="social-btn google"
                  @click="socialLogin('google')"
                  :disabled="loading"
                >
                  <i class="fab fa-google"></i>
                  <span>Google</span>
                </button>
              </div>
            </div>

            <!-- Register Link -->
            <div class="auth-footer">
              <p>
                Chưa có tài khoản?
                <router-link to="/signup">Đăng ký ngay</router-link>
              </p>
            </div>
          </div>

          <!-- Features -->
          <div class="auth-features">
            <div class="feature-item">
              <i class="fas fa-truck"></i>
              <div>
                <h3>Miễn phí vận chuyển</h3>
                <p>Cho đơn hàng từ 500.000₫</p>
              </div>
            </div>
            <div class="feature-item">
              <i class="fas fa-undo-alt"></i>
              <div>
                <h3>Đổi trả dễ dàng</h3>
                <p>Trong vòng 30 ngày</p>
              </div>
            </div>
            <div class="feature-item">
              <i class="fas fa-shield-alt"></i>
              <div>
                <h3>Bảo mật thanh toán</h3>
                <p>Công nghệ mã hóa SSL</p>
              </div>
            </div>
            <div class="feature-item">
              <i class="fas fa-headset"></i>
              <div>
                <h3>Hỗ trợ 24/7</h3>
                <p>Tư vấn nhiệt tình</p>
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
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../../stores/authStore";
import { useNotification } from "../../composables/useNotification";
import ClientHeader from "../../components/client/ClientHeader.vue";
import ClientFooter from "../../components/client/ClientFooter.vue";

const router = useRouter();
const authStore = useAuthStore();
const { showNotification } = useNotification();

const form = ref({
  email: "",
  password: "",
  remember: false,
});

const loading = ref(false);
const error = ref("");
const showPassword = ref(false);

const handleLogin = async () => {
  // Reset error
  error.value = "";
  loading.value = true;

  try {
    const user = await authStore.login(form.value.email, form.value.password);

    // Check if user wants to be remembered
    if (form.value.remember) {
      localStorage.setItem("rememberMe", "true");
    }

    showNotification("Đăng nhập thành công!", "success");

    const roles = user.roles || [];
    if (roles.includes("ROLE_ADMIN") || roles.includes("ROLE_STAFF")) {
      window.location.href = "/admin";
    } else {
      // Redirect to previous page or home
      const redirect = router.currentRoute.value.query.redirect || "/home";
      router.push(redirect);
    }
  } catch (err) {
    error.value = "Email hoặc mật khẩu không đúng";
    showNotification("Đăng nhập thất bại!", "error");
  } finally {
    loading.value = false;
  }
};

const socialLogin = async (provider) => {
  try {
    loading.value = true;

    if (provider === "google") {
      const idToken = "GOOGLE_ID_TOKEN";
      await authStore.googleLogin(idToken);
    }

    if (provider === "facebook") {
      const accessToken = "FACEBOOK_ACCESS_TOKEN";
      await authStore.facebookLogin(accessToken);
    }

    showNotification("Đăng nhập thành công!", "success");

    const roles = authStore.user?.roles || [];
    if (roles.includes("ROLE_ADMIN") || roles.includes("ROLE_STAFF")) {
      window.location.href = "/admin";
    } else {
      const redirect = router.currentRoute.value.query.redirect || "/home";
      router.push(redirect);
    }
  } catch (err) {
    showNotification(`Đăng nhập với ${provider} thất bại`, "error");
  } finally {
    loading.value = false;
  }
};
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

.alert i {
  font-size: 18px;
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

.input-wrapper:focus-within i:not(.toggle-password) {
  color: var(--primary-red);
}

.input-wrapper input:disabled {
  background: var(--light-gray);
  cursor: not-allowed;
}

.toggle-password {
  position: absolute;
  left: auto !important;
  right: 15px !important;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: var(--medium-gray);
  z-index: 2;
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
  content: "";
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

/* Features */
.auth-features {
  background: white;
  border-radius: 20px;
  padding: 30px;
  box-shadow: var(--shadow);
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

  .auth-features {
    grid-template-columns: 1fr;
  }

  .social-buttons {
    grid-template-columns: 1fr;
  }

  .form-options {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
