<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="isOpen" class="auth-modal">
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
                  @input="errors.name = ''"
                />
                <span v-if="errors.name" class="error-text">{{ errors.name }}</span>
              </div>

              <div class="form-group">
                <i class="fas fa-envelope"></i>
                <input
                  type="email"
                  v-model="registerForm.email"
                  placeholder="Email"
                  required
                  @input="errors.email = ''"
                />
                <span v-if="errors.email" class="error-text">{{ errors.email }}</span>
              </div>

              <div class="form-group">
                <i class="fas fa-phone"></i>
                <input
                  type="tel"
                  v-model="registerForm.phone"
                  placeholder="Số điện thoại"
                  required
                  @input="errors.phone = ''"
                />
                <span v-if="errors.phone" class="error-text">{{ errors.phone }}</span>
              </div>

              <div class="form-group">
                <i class="fas fa-lock"></i>
                <input
                  :type="showRegisterPassword ? 'text' : 'password'"
                  v-model="registerForm.password"
                  placeholder="Mật khẩu"
                  required
                  @input="errors.password = ''"
                />
                <i
                  class="fas fa-eye toggle-password"
                  :class="{ 'fa-eye-slash': showRegisterPassword }"
                  @click="toggleRegisterPassword"
                ></i>
                <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
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
       
      >
        <div class="auth-container forgot-container">
          <button class="close-btn" @click="closeForgotModal">
            <i class="fas fa-times"></i>
          </button>

          <!-- Bước 1: Nhập email -->
          <div v-if="!otpSent" class="forgot-content">
            <div class="forgot-header">
              <i class="fas fa-key"></i>
              <h2>Quên mật khẩu?</h2>
              <p>Nhập email để nhận mã OTP đặt lại mật khẩu</p>
            </div>

            <div v-if="forgotError" class="alert alert-error">
              <i class="fas fa-exclamation-circle"></i>
              {{ forgotError }}
            </div>

            <form @submit.prevent="handleForgotPassword">
              <div class="form-group">
                <i class="fas fa-envelope"></i>
                <input
                  type="email"
                  v-model="forgotEmail"
                  placeholder="Email của bạn"
                  required
                  :disabled="forgotLoading"
                />
              </div>

              <button type="submit" class="auth-btn" :disabled="forgotLoading">
                <span v-if="!forgotLoading">GỬI MÃ OTP</span>
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

          <!-- Bước 2: Nhập OTP và mật khẩu mới -->
          <div v-else class="forgot-content">
            <div class="forgot-header">
              <i class="fas fa-key"></i>
              <h2>Đặt lại mật khẩu</h2>
              <p>Nhập mã OTP và mật khẩu mới của bạn</p>
            </div>

            <div v-if="resetError" class="alert alert-error">
              <i class="fas fa-exclamation-circle"></i>
              {{ resetError }}
            </div>

            <div v-if="resetSuccess" class="alert alert-success">
              <i class="fas fa-check-circle"></i>
              {{ resetSuccess }}
            </div>

            <form @submit.prevent="handleResetPassword">
              <div class="form-group">
                <i class="fas fa-shield-alt"></i>
                <input
                  type="text"
                  v-model="resetForm.otp"
                  placeholder="Mã OTP (6 số)"
                  required
                  maxlength="6"
                  :disabled="resetLoading"
                />
              </div>

              <div class="form-group">
                <i class="fas fa-lock"></i>
                <input
                  :type="showResetPassword ? 'text' : 'password'"
                  v-model="resetForm.newPassword"
                  placeholder="Mật khẩu mới"
                  required
                  :disabled="resetLoading"
                />
                <i
                  class="fas fa-eye toggle-password"
                  :class="{ 'fa-eye-slash': showResetPassword }"
                  @click="showResetPassword = !showResetPassword"
                ></i>
              </div>

              <div class="form-group">
                <i class="fas fa-lock"></i>
                <input
                  :type="showResetConfirmPassword ? 'text' : 'password'"
                  v-model="resetForm.confirmPassword"
                  placeholder="Xác nhận mật khẩu mới"
                  required
                  :disabled="resetLoading"
                />
                <i
                  class="fas fa-eye toggle-password"
                  :class="{ 'fa-eye-slash': showResetConfirmPassword }"
                  @click="showResetConfirmPassword = !showResetConfirmPassword"
                ></i>
              </div>

              <div class="password-requirements">
                <p class="requirements-title">Mật khẩu phải có:</p>
                <ul>
                  <li :class="{ valid: resetForm.newPassword.length >= 6 }">
                    <i :class="resetForm.newPassword.length >= 6 ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Ít nhất 6 ký tự
                  </li>
                  <li :class="{ valid: /[A-Z]/.test(resetForm.newPassword) }">
                    <i :class="/[A-Z]/.test(resetForm.newPassword) ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Ít nhất 1 chữ hoa
                  </li>
                  <li :class="{ valid: /[0-9]/.test(resetForm.newPassword) }">
                    <i :class="/[0-9]/.test(resetForm.newPassword) ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Ít nhất 1 số
                  </li>
                  <li :class="{ valid: resetForm.newPassword && resetForm.confirmPassword && resetForm.newPassword === resetForm.confirmPassword }">
                    <i :class="resetForm.newPassword && resetForm.confirmPassword && resetForm.newPassword === resetForm.confirmPassword ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    Mật khẩu xác nhận khớp
                  </li>
                </ul>
              </div>

              <button type="submit" class="auth-btn" :disabled="resetLoading || !isResetFormValid">
                <span v-if="!resetLoading">ĐẶT LẠI MẬT KHẨU</span>
                <span v-else><i class="fas fa-spinner fa-spin"></i> Đang xử lý...</span>
              </button>
            </form>

            <div class="back-to-login">
              <a href="#" @click.prevent="backToLogin">
                <i class="fas fa-arrow-left"></i> Quay lại đăng nhập
              </a>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useAuthStore } from "../../stores/authStore";
import { useNotification } from "../../composables/useNotification";
import api from "@/api/axios";

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
const forgotError = ref("");
const forgotEmail = ref("");
const otpSent = ref(false);
const resetLoading = ref(false);
const resetError = ref("");
const resetSuccess = ref("");
const showResetPassword = ref(false);
const showResetConfirmPassword = ref(false);

// Reset form
const resetForm = ref({
  otp: "",
  newPassword: "",
  confirmPassword: "" });

// Login form
const loginForm = ref({
  email: "",
  password: "",
  remember: false });

// Register form
const registerForm = ref({
  name: "",
  email: "",
  phone: "",
  password: "",
  confirmPassword: "",
  agreeTerms: false });

const errors = ref({
  name: "",
  email: "",
  phone: "",
  password: ""
});

// Computed
const isResetFormValid = computed(() => {
  return resetForm.value.otp.length === 6 &&
         resetForm.value.newPassword.length >= 6 &&
         /[A-Z]/.test(resetForm.value.newPassword) &&
         /[0-9]/.test(resetForm.value.newPassword) &&
         resetForm.value.newPassword === resetForm.value.confirmPassword;
});

// ===== GIỮ NGUYÊN CÁC HÀM HIỆN CÓ =====
const handleRegister = async () => {
  const form = registerForm.value;
  errors.value = { name: "", email: "", phone: "", password: "" };

  let hasError = false;

  if (!form.name.trim()) {
    errors.value.name = "Họ và tên không được để trống";
    hasError = true;
  } else if (form.name.trim().length < 2) {
    errors.value.name = "Họ và tên phải có ít nhất 2 ký tự";
    hasError = true;
  }

  if (!form.email.trim()) {
    errors.value.email = "Email không được để trống";
    hasError = true;
  } else if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(form.email.trim())) {
    errors.value.email = "Email không đúng định dạng";
    hasError = true;
  }

  if (!form.phone.trim()) {
    errors.value.phone = "Số điện thoại không được để trống";
    hasError = true;
  } else if (!/^0[0-9]{9}$/.test(form.phone.trim())) {
    errors.value.phone = "Số điện thoại không hợp lệ (phải gồm 10 số và bắt đầu bằng 0)";
    hasError = true;
  }

  if (!form.password) {
    errors.value.password = "Mật khẩu không được để trống";
    hasError = true;
  } else if (form.password.length < 6) {
    errors.value.password = "Mật khẩu phải ít nhất 6 ký tự";
    hasError = true;
  } else if (form.password !== form.confirmPassword) {
    errors.value.password = "Mật khẩu xác nhận không khớp";
    hasError = true;
  }

  if (!form.agreeTerms) {
    showNotification("Vui lòng đồng ý điều khoản", "error");
    hasError = true;
  }

  if (hasError) {
    showNotification("Vui lòng kiểm tra lại các thông tin nhập vào", "error");
    return;
  }

  loading.value = true;

  try {
    await authStore.register({
      name: form.name,
      email: form.email,
      phone: form.phone,
      password: form.password });

    showNotification("Đăng ký thành công!", "success");

    resetForms();
    activeTab.value = "login";
  } catch (error) {
    const data = error?.response?.data;
    if (data?.errors && Object.keys(data.errors).length > 0) {
      if (data.errors.name) errors.value.name = data.errors.name;
      if (data.errors.email) errors.value.email = data.errors.email;
      if (data.errors.phone) errors.value.phone = data.errors.phone;
      if (data.errors.password) errors.value.password = data.errors.password;
      showNotification("Dữ liệu không hợp lệ, vui lòng sửa lại!", "error");
    } else {
      const message = data?.message || "Email đã tồn tại hoặc đăng ký thất bại";
      if (message.includes('Số điện thoại')) errors.value.phone = message;
      if (message.includes('Email') || message.includes('email')) errors.value.email = "Email đã được sử dụng bởi tài khoản khác";
      showNotification(message, "error");
    }
  } finally {
    loading.value = false;
  }
};

const handleGoogleCallback = async (response) => {
  if (!response.credential) {
    showNotification("Đăng nhập Google thất bại", "error");
    return;
  }
  loading.value = true;
  try {
    const user = await authStore.googleLogin(response.credential);
    showNotification("Đăng nhập Google thành công!", "success");
    closeModal();
    if (authStore.isAdmin) {
      window.location.href = "/admin";
    } else if (authStore.isStaff) {
      window.location.href = "/staff/pos";
    } else if (authStore.userRoles.includes('SALE_STAFF')) {
      window.location.href = "/sale-staff";
    }
  } catch (error) {
    console.error("Google Login Error:", error);
    showNotification("Đăng nhập Google thất bại", "error");
  } finally {
    loading.value = false;
  }
};

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
    remember: false };
  registerForm.value = {
    name: "",
    email: "",
    phone: "",
    password: "",
    confirmPassword: "",
    agreeTerms: false };
  errors.value = { name: "", email: "", phone: "", password: "" };
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

    if (authStore.isAdmin) {
      window.location.href = "/admin";
    } else if (authStore.isStaff) {
      window.location.href = "/staff/pos";
    } else if (authStore.userRoles.includes('SALE_STAFF')) {
      window.location.href = "/sale-staff";
    }
  } catch (error) {
    showNotification("Sai email hoặc mật khẩu", "error");
  } finally {
    loading.value = false;
  }
};

const socialLogin = async (provider) => {
  loading.value = true;

  try {
    if (provider === "google") {
      const response = await new Promise((resolve, reject) => {
        google.accounts.id.initialize({
          client_id:
            "402914017010-6lmb8tf0937e9gpmjfjh36ps6mf94lu6.apps.googleusercontent.com",
          callback: (response) => {
            if (!response.credential) {
              reject(new Error("Google login cancelled"));
              return;
            }
            resolve(response);
          } });

        google.accounts.id.prompt((notification) => {
          if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
            reject(new Error("Google login cancelled"));
          }
        });
      });

      const idToken = response.credential;
      const user = await authStore.googleLogin(idToken);

      showNotification("Đăng nhập Google thành công!", "success");
      closeModal();

      if (authStore.isAdmin) {
        window.location.href = "/admin";
      } else if (authStore.isStaff) {
        window.location.href = "/staff/pos";
      } else if (authStore.userRoles.includes('SALE_STAFF')) {
        window.location.href = "/sale-staff";
      }

      return;
    }

    if (provider === "facebook") {
      const response = await new Promise((resolve, reject) => {
        FB.login(
          (response) => {
            if (response.authResponse) {
              resolve(response);
            } else {
              reject(new Error("Facebook login cancelled"));
            }
          },
          {
            scope: "public_profile,email" },
        );
      });

      const accessToken = response.authResponse.accessToken;
      const user = await authStore.facebookLogin(accessToken);

      showNotification("Đăng nhập Facebook thành công!", "success");
      closeModal();

      if (authStore.isAdmin) {
        window.location.href = "/admin";
      } else if (authStore.isStaff) {
        window.location.href = "/staff/pos";
      } else if (authStore.userRoles.includes('SALE_STAFF')) {
        window.location.href = "/sale-staff";
      }

      return;
    }
  } catch (error) {
    console.error("Social Login Error:", error);

    if (error.message?.includes("cancelled")) {
      return;
    }

    showNotification("Đăng nhập thất bại", "error");
  } finally {
    loading.value = false;
  }
};

// ===== FORGOT PASSWORD =====
const showForgotPassword = () => {
  forgotEmail.value = "";
  forgotError.value = "";
  otpSent.value = false;
  resetError.value = "";
  resetSuccess.value = "";
  resetForm.value = {
    otp: "",
    newPassword: "",
    confirmPassword: "" };
  showForgotModal.value = true;
};

const closeForgotModal = () => {
  showForgotModal.value = false;
  forgotEmail.value = "";
  forgotError.value = "";
  otpSent.value = false;
  resetError.value = "";
  resetSuccess.value = "";
  resetForm.value = {
    otp: "",
    newPassword: "",
    confirmPassword: "" };
  resetLoading.value = false;
};

const backToLogin = () => {
  closeForgotModal();
};

// Bước 1: Gửi OTP
const handleForgotPassword = async () => {
  if (!forgotEmail.value) {
    forgotError.value = "Vui lòng nhập email";
    return;
  }

  forgotError.value = "";
  forgotLoading.value = true;

  try {
    const response = await api.post(`/auth/forgot-password`, {
      email: forgotEmail.value });

    console.log("✅ Forgot password response:", response.data);
    
    otpSent.value = true;
    showNotification(
      "Mã OTP đã được gửi đến email của bạn! Vui lòng kiểm tra hộp thư.",
      "success"
    );
  } catch (error) {
    console.error("❌ Forgot password error:", error);
    forgotError.value =
      error.response?.data?.message ||
      "Email không tồn tại hoặc có lỗi xảy ra. Vui lòng thử lại.";
    showNotification(forgotError.value, "error");
  } finally {
    forgotLoading.value = false;
  }
};

// Bước 2: Đặt lại mật khẩu với OTP
const handleResetPassword = async () => {
  resetError.value = "";
  resetSuccess.value = "";

  if (resetForm.value.otp.length !== 6) {
    resetError.value = "Mã OTP phải có 6 số";
    return;
  }

  if (resetForm.value.newPassword.length < 6) {
    resetError.value = "Mật khẩu phải có ít nhất 6 ký tự";
    return;
  }

  if (resetForm.value.newPassword !== resetForm.value.confirmPassword) {
    resetError.value = "Mật khẩu xác nhận không khớp";
    return;
  }

  resetLoading.value = true;

  try {
    const response = await api.post(`/auth/reset-password`, {
      email: forgotEmail.value,
      otp: resetForm.value.otp,
      newPassword: resetForm.value.newPassword });

    console.log("✅ Reset password response:", response.data);
    
    resetSuccess.value = "Đặt lại mật khẩu thành công!";
    showNotification("Mật khẩu đã được cập nhật!", "success");

    setTimeout(() => {
      closeForgotModal();
    }, 2000);
  } catch (error) {
    console.error("❌ Reset password error:", error);
    resetError.value =
      error.response?.data?.message ||
      "Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.";
    showNotification(resetError.value, "error");
  } finally {
    resetLoading.value = false;
  }
};

// ===== TERMS =====
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
  max-height: 90vh;
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

/* Tabs */
.auth-tabs {
  flex-shrink: 0;
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

.error-text {
  color: #dc3545;
  font-size: 13px;
  margin-top: 5px;
  display: block;
  text-align: left;
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
.forgot-content {
  padding: 20px 10px;
}

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

/* ✅ Alert styles */
.alert {
  padding: 12px 16px;
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

.alert-success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.alert i {
  font-size: 18px;
}

/* ✅ Password Requirements */
.password-requirements {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.requirements-title {
  color: #2c3e50;
  font-weight: 600;
  margin-bottom: 10px;
  font-size: 14px;
}

.password-requirements ul {
  list-style: none;
  padding: 0;
}

.password-requirements li {
  margin-bottom: 5px;
  color: #999;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.password-requirements li i {
  font-size: 12px;
  color: #999;
}

.password-requirements li.valid {
  color: #28a745;
}

.password-requirements li.valid i {
  color: #28a745;
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

  .password-requirements li {
    font-size: 12px;
  }
}
</style>