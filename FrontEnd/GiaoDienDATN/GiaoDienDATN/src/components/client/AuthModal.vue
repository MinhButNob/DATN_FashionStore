<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="isOpen" class="auth-modal" @click.self="closeModal">
        <div class="auth-container">
          <button class="close-btn" @click="closeModal">
            <i class="fas fa-times"></i>
          </button>

          <!-- Tabs -->
          <div class="auth-tabs">
            <button
              class="tab-btn"
              :class="{ active: activeTab === 'login' }"
              @click="activeTab = 'login'"
            >
              ĐĂNG NHẬP
            </button>
            <button
              class="tab-btn"
              :class="{ active: activeTab === 'register' }"
              @click="activeTab = 'register'"
            >
              ĐĂNG KÝ
            </button>
          </div>

          <!-- Login Form -->
          <div v-if="activeTab === 'login'" class="auth-form">
            <h2>Chào mừng trở lại!</h2>
            <p class="subtitle">Đăng nhập để tiếp tục mua sắm</p>

            <form @submit.prevent="handleLogin">
              <div class="form-group">
                <i class="fas fa-envelope"></i>
                <input
                  type="email"
                  v-model="loginForm.email"
                  placeholder="Email"
                  required
                />
              </div>

              <div class="form-group">
                <i class="fas fa-lock"></i>
                <input
                  :type="showPassword ? 'text' : 'password'"
                  v-model="loginForm.password"
                  placeholder="Mật khẩu"
                  required
                />
                <i
                  class="fas fa-eye toggle-password"
                  :class="{ 'fa-eye-slash': showPassword }"
                  @click="togglePassword"
                ></i>
              </div>

              <div class="form-options">
                <label class="remember-me">
                  <input type="checkbox" v-model="loginForm.remember" />
                  Ghi nhớ đăng nhập
                </label>
                <a
                  href="#"
                  class="forgot-password"
                  @click.prevent="showForgotPassword"
                >
                  Quên mật khẩu?
                </a>
              </div>

              <button type="submit" class="auth-btn" :disabled="loading">
                <span v-if="!loading">ĐĂNG NHẬP</span>
                <span v-else
                  ><i class="fas fa-spinner fa-spin"></i> Đang xử lý...</span
                >
              </button>
            </form>

            <div class="social-login">
              <p>Hoặc đăng nhập với</p>
              <div class="social-icons">
                <button
                  class="social-btn facebook"
                  @click="socialLogin('facebook')"
                >
                  <i class="fab fa-facebook-f"></i>
                </button>
                <button
                  class="social-btn google"
                  @click="socialLogin('google')"
                >
                  <i class="fab fa-google"></i>
                </button>
                <button class="social-btn apple" @click="socialLogin('apple')">
                  <i class="fab fa-apple"></i>
                </button>
              </div>
            </div>
          </div>

          <!-- Register Form -->
          <div v-else class="auth-form">
            <h2>Tạo tài khoản mới</h2>
            <p class="subtitle">Đăng ký để nhận nhiều ưu đãi hấp dẫn</p>

            <form @submit.prevent="handleRegister">
              <div class="form-group">
                <i class="fas fa-user"></i>
                <input
                  type="text"
                  v-model="registerForm.name"
                  placeholder="Họ và tên"
                  required
                />
              </div>

              <div class="form-group">
                <i class="fas fa-envelope"></i>
                <input
                  type="email"
                  v-model="registerForm.email"
                  placeholder="Email"
                  required
                />
              </div>

              <div class="form-group">
                <i class="fas fa-phone"></i>
                <input
                  type="tel"
                  v-model="registerForm.phone"
                  placeholder="Số điện thoại"
                  required
                />
              </div>

              <div class="form-group">
                <i class="fas fa-lock"></i>
                <input
                  :type="showRegisterPassword ? 'text' : 'password'"
                  v-model="registerForm.password"
                  placeholder="Mật khẩu"
                  required
                />
                <i
                  class="fas fa-eye toggle-password"
                  :class="{ 'fa-eye-slash': showRegisterPassword }"
                  @click="toggleRegisterPassword"
                ></i>
              </div>

              <div class="form-group">
                <i class="fas fa-lock"></i>
                <input
                  :type="showConfirmPassword ? 'text' : 'password'"
                  v-model="registerForm.confirmPassword"
                  placeholder="Xác nhận mật khẩu"
                  required
                />
                <i
                  class="fas fa-eye toggle-password"
                  :class="{ 'fa-eye-slash': showConfirmPassword }"
                  @click="toggleConfirmPassword"
                ></i>
              </div>

              <div class="form-options">
                <label class="terms">
                  <input type="checkbox" v-model="registerForm.agreeTerms" />
                  Tôi đồng ý với
                  <a href="#" @click.prevent="showTerms">điều khoản</a>
                  và
                  <a href="#" @click.prevent="showPrivacy"
                    >chính sách bảo mật</a
                  >
                </label>
              </div>

              <button
                type="submit"
                class="auth-btn"
                :disabled="loading || !registerForm.agreeTerms"
              >
                <span v-if="!loading">ĐĂNG KÝ</span>
                <span v-else
                  ><i class="fas fa-spinner fa-spin"></i> Đang xử lý...</span
                >
              </button>
            </form>

            <div class="social-login">
              <p>Hoặc đăng ký với</p>
              <div class="social-icons">
                <button
                  class="social-btn facebook"
                  @click="socialLogin('facebook')"
                >
                  <i class="fab fa-facebook-f"></i>
                </button>
                <button
                  class="social-btn google"
                  @click="socialLogin('google')"
                >
                  <i class="fab fa-google"></i>
                </button>
                <button class="social-btn apple" @click="socialLogin('apple')">
                  <i class="fab fa-apple"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Forgot Password Modal -->
    <Transition name="modal">
      <div
        v-if="showForgotModal"
        class="auth-modal"
        @click.self="closeForgotModal"
      >
        <div class="auth-container forgot-container">
          <button class="close-btn" @click="closeForgotModal">
            <i class="fas fa-times"></i>
          </button>

          <div class="forgot-header">
            <i class="fas fa-key"></i>
            <h2>Quên mật khẩu?</h2>
            <p>Nhập email để nhận hướng dẫn đặt lại mật khẩu</p>
          </div>

          <form @submit.prevent="handleForgotPassword">
            <div class="form-group">
              <i class="fas fa-envelope"></i>
              <input
                type="email"
                v-model="forgotEmail"
                placeholder="Email của bạn"
                required
              />
            </div>

            <button type="submit" class="auth-btn" :disabled="forgotLoading">
              <span v-if="!forgotLoading">GỬI YÊU CẦU</span>
              <span v-else
                ><i class="fas fa-spinner fa-spin"></i> Đang gửi...</span
              >
            </button>
          </form>

          <div class="back-to-login">
            <a href="#" @click.prevent="backToLogin">
              <i class="fas fa-arrow-left"></i> Quay lại đăng nhập
            </a>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { useAuthStore } from "../../stores/authStore";
import { useNotification } from "../../composables/useNotification";

const authStore = useAuthStore();
const { showNotification } = useNotification();

// State
const isOpen = ref(false);
const activeTab = ref("login");
const loading = ref(false);
const showPassword = ref(false);
const showRegisterPassword = ref(false);
const showConfirmPassword = ref(false);
const showForgotModal = ref(false);
const forgotLoading = ref(false);
const forgotEmail = ref("");

// Login form
const loginForm = ref({
  email: "",
  password: "",
  remember: false,
});

// Register form
const registerForm = ref({
  name: "",
  email: "",
  phone: "",
  password: "",
  confirmPassword: "",
  agreeTerms: false,
});

// Methods
const openModal = (tab = "login") => {
  activeTab.value = tab;
  isOpen.value = true;
  document.body.style.overflow = "hidden";
};

const closeModal = () => {
  isOpen.value = false;
  document.body.style.overflow = "auto";
  resetForms();
};

const resetForms = () => {
  loginForm.value = {
    email: "",
    password: "",
    remember: false,
  };
  registerForm.value = {
    name: "",
    email: "",
    phone: "",
    password: "",
    confirmPassword: "",
    agreeTerms: false,
  };
};

const togglePassword = () => {
  showPassword.value = !showPassword.value;
};

const toggleRegisterPassword = () => {
  showRegisterPassword.value = !showRegisterPassword.value;
};

const toggleConfirmPassword = () => {
  showConfirmPassword.value = !showConfirmPassword.value;
};

const handleLogin = async () => {
  if (!loginForm.value.email || !loginForm.value.password) {
    showNotification("Vui lòng nhập đầy đủ thông tin", "error");
    return;
  }

  loading.value = true;
  try {
    const user = await authStore.login(
      loginForm.value.email,
      loginForm.value.password,
    );
    showNotification("Đăng nhập thành công!", "success");
    closeModal();

    // Redirect based on role
    const roles = user.roles || [];
    if (roles.includes("ROLE_ADMIN") || roles.includes("ROLE_STAFF")) {
      window.location.href = "/admin"; // Redirect to admin dashboard
    } else {
      // Reload current page to update header/state for customer
      // Or do nothing if reactivity updates the UI automatically
    }
  } catch (error) {
    showNotification(
      "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.",
      "error",
    );
  } finally {
    loading.value = false;
  }
};

const handleRegister = async () => {
  // Validate
  if (
    !registerForm.value.name ||
    !registerForm.value.email ||
    !registerForm.value.phone ||
    !registerForm.value.password
  ) {
    showNotification("Vui lòng nhập đầy đủ thông tin", "error");
    return;
  }

  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    showNotification("Mật khẩu xác nhận không khớp", "error");
    return;
  }

  if (registerForm.value.password.length < 6) {
    showNotification("Mật khẩu phải có ít nhất 6 ký tự", "error");
    return;
  }

  loading.value = true;
  try {
    await authStore.register({
      name: registerForm.value.name,
      email: registerForm.value.email,
      phone: registerForm.value.phone,
      password: registerForm.value.password,
    });
    showNotification("Đăng ký thành công!", "success");
    closeModal();
  } catch (error) {
    showNotification("Đăng ký thất bại. Vui lòng thử lại sau.", "error");
  } finally {
    loading.value = false;
  }
};

const socialLogin = async (provider) => {
  try {
    loading.value = true;

    if (provider === "google") {
      // sau này lấy idToken từ Google SDK
      const idToken = "GOOGLE_ID_TOKEN";

      await authStore.googleLogin(idToken);
    }

    if (provider === "facebook") {
      // sau này lấy accessToken từ Facebook SDK
      const accessToken =
        "FACEBOOK_ACCESS_TOKEN";

      await authStore.facebookLogin(accessToken);
    }

    showNotification(
      "Đăng nhập thành công!",
      "success",
    );

    closeModal();
  } catch (error) {
    showNotification(
      "Đăng nhập thất bại",
      "error",
    );
  } finally {
    loading.value = false;
  }
};

const showForgotPassword = () => {
  showForgotModal.value = true;
};

const closeForgotModal = () => {
  showForgotModal.value = false;
  forgotEmail.value = "";
};

const backToLogin = () => {
  closeForgotModal();
};

const handleForgotPassword = async () => {
  if (!forgotEmail.value) {
    showNotification("Vui lòng nhập email", "error");
    return;
  }

  forgotLoading.value = true;
  try {
    // Simulate API call
    await new Promise((resolve) => setTimeout(resolve, 1500));
    showNotification(
      "Link đặt lại mật khẩu đã được gửi đến email của bạn!",
      "success",
    );
    closeForgotModal();
  } catch (error) {
    showNotification("Có lỗi xảy ra. Vui lòng thử lại sau.", "error");
  } finally {
    forgotLoading.value = false;
  }
};

const showTerms = () => {
  showNotification("Điều khoản sử dụng", "info");
};

const showPrivacy = () => {
  showNotification("Chính sách bảo mật", "info");
};

// Listen for open modal events
const handleOpenAuthModal = (event) => {
  openModal(event.detail);
};

onMounted(() => {
  window.addEventListener("open-auth-modal", handleOpenAuthModal);
});

onUnmounted(() => {
  window.removeEventListener("open-auth-modal", handleOpenAuthModal);
  document.body.style.overflow = "auto";
});
</script>

<style scoped>
.auth-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 3000;
  backdrop-filter: blur(5px);
}

.auth-container {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 450px;
  position: relative;
  overflow: hidden;
  max-height: 90vh; /* Ensure modal itself doesn't exceed screen height */
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s;
}

.forgot-container {
  padding: 40px;
}

.close-btn {
  position: absolute;
  top: 15px;
  right: 20px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--medium-gray);
  z-index: 10;
  transition: all 0.3s;
}

.close-btn:hover {
  color: var(--primary-red);
  transform: rotate(90deg);
}

/* Tabs inside flex container */
.auth-tabs {
  flex-shrink: 0; /* Keep tabs fixed while form scrolls */
  display: flex;
  background: var(--light-gray);
  border-bottom: 1px solid var(--border-color);
}

.tab-btn {
  flex: 1;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  font-weight: 600;
  color: var(--medium-gray);
  background: none;
  border: none;
  transition: all 0.3s;
  font-size: 16px;
}

.tab-btn.active {
  color: var(--primary-red);
  background: white;
  border-bottom: 3px solid var(--primary-red);
}

/* Form */
.auth-form {
  padding: 30px;
  overflow-y: auto;
}

/* Custom scrollbar for better look */
.auth-form::-webkit-scrollbar {
  width: 6px;
}
.auth-form::-webkit-scrollbar-track {
  background: transparent;
}
.auth-form::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 10px;
}
.auth-form::-webkit-scrollbar-thumb:hover {
  background: var(--medium-gray);
}

.auth-form h2 {
  color: var(--dark-gray);
  margin-bottom: 10px;
  font-size: 24px;
}

.subtitle {
  color: var(--medium-gray);
  margin-bottom: 25px;
  font-size: 14px;
}

.form-group {
  margin-bottom: 20px;
  position: relative;
}

.form-group i:not(.toggle-password) {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--medium-gray);
  transition: all 0.3s;
  font-size: 16px;
}

.form-group input {
  width: 100%;
  padding: 15px 15px 15px 45px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  font-size: 15px;
  transition: all 0.3s;
}

.form-group input:focus {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.form-group:focus-within i:not(.toggle-password) {
  color: var(--primary-red);
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
}

.toggle-password:hover {
  color: var(--primary-red);
}

/* Form Options */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 14px;
}

.remember-me,
.terms {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--medium-gray);
  cursor: pointer;
}

.remember-me input,
.terms input {
  accent-color: var(--primary-red);
}

.terms a {
  color: var(--primary-red);
  text-decoration: none;
}

.terms a:hover {
  text-decoration: underline;
}

.forgot-password {
  color: var(--primary-red);
  text-decoration: none;
}

.forgot-password:hover {
  text-decoration: underline;
}

/* Auth Button */
.auth-btn {
  width: 100%;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  border: none;
  padding: 15px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
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
  text-align: center;
  margin-top: 20px;
}

.social-login p {
  color: var(--medium-gray);
  margin-bottom: 15px;
  position: relative;
}

.social-login p::before,
.social-login p::after {
  content: "";
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background: var(--border-color);
}

.social-login p::before {
  left: 0;
}

.social-login p::after {
  right: 0;
}

.social-icons {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.social-btn {
  width: 45px;
  height: 45px;
  border: 1px solid var(--border-color);
  border-radius: 50%;
  background: white;
  color: var(--dark-gray);
  cursor: pointer;
  transition: all 0.3s;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.social-btn:hover {
  transform: translateY(-3px);
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

.social-btn.apple:hover {
  background: #000;
  color: white;
  border-color: #000;
}

/* Forgot Password */
.forgot-header {
  text-align: center;
  margin-bottom: 30px;
}

.forgot-header i {
  font-size: 50px;
  color: var(--primary-red);
  margin-bottom: 15px;
}

.forgot-header h2 {
  font-size: 28px;
  color: var(--dark-gray);
  margin-bottom: 10px;
}

.forgot-header p {
  color: var(--medium-gray);
}

.back-to-login {
  text-align: center;
  margin-top: 20px;
}

.back-to-login a {
  color: var(--primary-red);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  transition: gap 0.3s;
}

.back-to-login a:hover {
  gap: 10px;
}

/* Animations */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(50px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 480px) {
  .auth-container {
    width: 95%;
  }

  .auth-form {
    padding: 20px;
  }

  .form-options {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .social-icons {
    flex-wrap: wrap;
  }

  .social-btn {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }

  .forgot-container {
    padding: 30px 20px;
  }
}
</style>
