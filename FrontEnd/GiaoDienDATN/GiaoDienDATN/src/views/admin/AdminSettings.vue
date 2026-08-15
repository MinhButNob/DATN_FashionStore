<template>
  <div class="admin-settings">
    <h1 class="page-title">Cài đặt hệ thống</h1>

    <div class="settings-container">
      <!-- Sidebar -->
      <div class="settings-sidebar">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="settings-tab"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <i :class="tab.icon"></i>
          <span>{{ tab.name }}</span>
        </button>
      </div>

      <!-- Content -->
      <div class="settings-content">
        <!-- General Settings -->
        <div v-show="activeTab === 'general'" class="settings-panel">
          <h2>Cài đặt chung</h2>
          
          <form @submit.prevent="saveGeneralSettings">
            <div class="form-group">
              <label>Tên cửa hàng</label>
              <input type="text" v-model="general.storeName">
            </div>

            <div class="form-group">
              <label>Email liên hệ</label>
              <input type="email" v-model="general.contactEmail">
            </div>

            <div class="form-group">
              <label>Số điện thoại</label>
              <input type="tel" v-model="general.phone">
            </div>

            <div class="form-group">
              <label>Địa chỉ</label>
              <textarea v-model="general.address" rows="3"></textarea>
            </div>

            <div class="form-group">
              <label>Múi giờ</label>
              <select v-model="general.timezone">
                <option value="Asia/Ho_Chi_Minh">Asia/Ho_Chi_Minh (GMT+7)</option>
                <option value="Asia/Hanoi">Asia/Hanoi (GMT+7)</option>
              </select>
            </div>

            <div class="form-group">
              <label>Đơn vị tiền tệ</label>
              <select v-model="general.currency">
                <option value="VND">VND - Việt Nam Đồng</option>
                <option value="USD">USD - US Dollar</option>
              </select>
            </div>

            <button type="submit" class="btn btn-primary">
              Lưu thay đổi
            </button>
          </form>
        </div>

        <!-- Shipping Settings -->
        <div v-show="activeTab === 'shipping'" class="settings-panel">
          <h2>Cài đặt vận chuyển</h2>

          <div class="shipping-methods">
            <div v-for="method in shipping.methods" :key="method.id" class="shipping-method-card">
              <div class="method-header">
                <div class="method-info">
                  <i :class="method.icon"></i>
                  <h3>{{ method.name }}</h3>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="method.enabled">
                  <span class="slider"></span>
                </label>
              </div>

              <div class="method-details" v-if="method.enabled">
                <div class="form-group">
                  <label>Phí vận chuyển</label>
                  <input type="number" v-model="method.fee">
                </div>

                <div class="form-group">
                  <label>Thời gian giao hàng (ngày)</label>
                  <input type="text" v-model="method.time">
                </div>

                <div class="form-group">
                  <label>Miễn phí cho đơn từ</label>
                  <input type="number" v-model="method.freeFrom">
                </div>
              </div>
            </div>
          </div>

          <button class="btn btn-primary" @click="saveShippingSettings">
            Lưu thay đổi
          </button>
        </div>

        <!-- Payment Settings -->
        <div v-show="activeTab === 'payment'" class="settings-panel">
          <h2>Cài đặt thanh toán</h2>

          <div class="payment-methods">
            <div v-for="method in payment.methods" :key="method.id" class="payment-method-card">
              <div class="method-header">
                <div class="method-info">
                  <i :class="method.icon"></i>
                  <h3>{{ method.name }}</h3>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="method.enabled">
                  <span class="slider"></span>
                </label>
              </div>

              <div class="method-details" v-if="method.enabled">
                <template v-if="method.type === 'bank'">
                  <div class="form-group">
                    <label>Tên ngân hàng</label>
                    <input type="text" v-model="method.bankName">
                  </div>
                  <div class="form-group">
                    <label>Số tài khoản</label>
                    <input type="text" v-model="method.accountNumber">
                  </div>
                  <div class="form-group">
                    <label>Chủ tài khoản</label>
                    <input type="text" v-model="method.accountName">
                  </div>
                </template>

                <template v-if="method.type === 'cod'">
                  <div class="form-group">
                    <label>Phí thu hộ</label>
                    <input type="number" v-model="method.codFee">
                  </div>
                </template>
              </div>
            </div>
          </div>

          <button class="btn btn-primary" @click="savePaymentSettings">
            Lưu thay đổi
          </button>
        </div>

        <!-- Email Settings -->
        <div v-show="activeTab === 'email'" class="settings-panel">
          <h2>Cài đặt email</h2>

          <div class="email-templates">
            <div v-for="template in email.templates" :key="template.id" class="template-card">
              <div class="template-header" @click="toggleTemplate(template.id)">
                <div class="template-info">
                  <i :class="template.icon"></i>
                  <h3>{{ template.name }}</h3>
                </div>
                <i class="fas fa-chevron-down" :class="{ rotated: expandedTemplate === template.id }"></i>
              </div>

              <div class="template-content" v-if="expandedTemplate === template.id">
                <div class="form-group">
                  <label>Tiêu đề email</label>
                  <input type="text" v-model="template.subject">
                </div>

                <div class="form-group">
                  <label>Nội dung</label>
                  <textarea v-model="template.content" rows="6"></textarea>
                </div>

                <div class="template-variables">
  <h4>Các biến có thể sử dụng:</h4>
  <div class="variables-list">
    <span v-for="(variable, index) in template.variables" :key="index" class="variable-tag">
      {{ variable }}
    </span>
  </div>
</div>
              </div>
            </div>
          </div>

          <button class="btn btn-primary" @click="saveEmailSettings">
            Lưu thay đổi
          </button>
        </div>

        <!-- SEO Settings -->
        <div v-show="activeTab === 'seo'" class="settings-panel">
          <h2>Cài đặt SEO</h2>

          <form @submit.prevent="saveSEOSettings">
            <div class="form-group">
              <label>Meta Title mặc định</label>
              <input type="text" v-model="seo.defaultTitle">
            </div>

            <div class="form-group">
              <label>Meta Description mặc định</label>
              <textarea v-model="seo.defaultDescription" rows="3"></textarea>
            </div>

            <div class="form-group">
              <label>Meta Keywords</label>
              <input type="text" v-model="seo.keywords" placeholder="thời trang, quần áo, phụ kiện">
            </div>

            <div class="form-group">
              <label>Google Analytics ID</label>
              <input type="text" v-model="seo.gaId">
            </div>

            <div class="form-group">
              <label>Facebook Pixel ID</label>
              <input type="text" v-model="seo.fbPixel">
            </div>

            <button type="submit" class="btn btn-primary">
              Lưu thay đổi
            </button>
          </form>
        </div>

        <!-- Backup & Security -->
        <div v-show="activeTab === 'backup'" class="settings-panel">
          <h2>Sao lưu & Bảo mật</h2>

          <div class="backup-section">
            <h3>Sao lưu dữ liệu</h3>
            <div class="backup-info">
              <p><strong>Lần sao lưu cuối:</strong> 15/03/2024 14:30</p>
              <p><strong>Dung lượng:</strong> 256 MB</p>
            </div>

            <div class="backup-actions">
              <button class="btn btn-primary" @click="createBackup">
                <i class="fas fa-database"></i> Sao lưu ngay
              </button>
              <button class="btn btn-outline" @click="restoreBackup">
                <i class="fas fa-undo"></i> Khôi phục
              </button>
            </div>
          </div>

          <div class="security-section">
            <h3>Bảo mật</h3>

            <div class="form-group">
              <label>
                <input type="checkbox" v-model="security.twoFactor">
                Bật xác thực 2 lớp
              </label>
            </div>

            <div class="form-group">
              <label>
                <input type="checkbox" v-model="security.loginNotification">
                Gửi thông báo khi có đăng nhập lạ
              </label>
            </div>

            <div class="form-group">
              <label>Thời gian timeout (phút)</label>
              <input type="number" v-model="security.sessionTimeout">
            </div>

            <div class="form-group">
              <label>Giới hạn đăng nhập sai</label>
              <input type="number" v-model="security.maxLoginAttempts">
            </div>
          </div>

          <button class="btn btn-primary" @click="saveSecuritySettings">
            Lưu thay đổi
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useNotification } from '../../composables/useNotification'

const { showNotification } = useNotification()

// State
const activeTab = ref('general')
const expandedTemplate = ref(null)

const tabs = [
  { id: 'general', name: 'Cài đặt chung', icon: 'fas fa-cog' },
  { id: 'shipping', name: 'Vận chuyển', icon: 'fas fa-truck' },
  { id: 'payment', name: 'Thanh toán', icon: 'fas fa-credit-card' },
  { id: 'email', name: 'Email', icon: 'fas fa-envelope' },
  { id: 'seo', name: 'SEO', icon: 'fas fa-chart-line' },
  { id: 'backup', name: 'Sao lưu & Bảo mật', icon: 'fas fa-shield-alt' }
]

// General settings
const general = ref({
  storeName: 'Fashion Store',
  contactEmail: 'contact@fashionstore.vn',
  phone: '1900 1234',
  address: '123 Nguyễn Huệ, Quận 1, TP.HCM',
  timezone: 'Asia/Ho_Chi_Minh',
  currency: 'VND'
})

// Shipping settings
const shipping = ref({
  methods: [
    {
      id: 'standard',
      name: 'Giao hàng tiêu chuẩn',
      icon: 'fas fa-truck',
      enabled: true,
      fee: 0,
      time: '3-5 ngày',
      freeFrom: 500000
    },
    {
      id: 'express',
      name: 'Giao hàng nhanh',
      icon: 'fas fa-rocket',
      enabled: true,
      fee: 50000,
      time: '1-2 ngày',
      freeFrom: 1000000
    },
    {
      id: 'pickup',
      name: 'Nhận tại cửa hàng',
      icon: 'fas fa-store',
      enabled: true,
      fee: 0,
      time: 'Trong giờ hành chính',
      freeFrom: 0
    }
  ]
})

// Payment settings
const payment = ref({
  methods: [
    {
      id: 'cod',
      name: 'Thanh toán khi nhận hàng',
      icon: 'fas fa-money-bill-wave',
      type: 'cod',
      enabled: true,
      codFee: 0
    },
    {
      id: 'bank',
      name: 'Chuyển khoản ngân hàng',
      icon: 'fas fa-university',
      type: 'bank',
      enabled: true,
      bankName: 'Vietcombank',
      accountNumber: '1234567890',
      accountName: 'FASHION STORE'
    },
    {
      id: 'momo',
      name: 'Ví MoMo',
      icon: 'fas fa-mobile-alt',
      type: 'wallet',
      enabled: false
    },
    {
      id: 'vnpay',
      name: 'VNPay',
      icon: 'fas fa-credit-card',
      type: 'gateway',
      enabled: false
    }
  ]
})

// Email settings
const email = ref({
  templates: [
    {
      id: 'welcome',
      name: 'Email chào mừng',
      icon: 'fas fa-hand-peace',
      subject: 'Chào mừng bạn đến với Fashion Store!',
      content: 'Cảm ơn bạn đã đăng ký tài khoản tại Fashion Store...',
      variables: ['{{customer_name}}', '{{store_name}}', '{{login_url}}']
    },
    {
      id: 'order_confirmation',
      name: 'Xác nhận đơn hàng',
      icon: 'fas fa-shopping-bag',
      subject: 'Xác nhận đơn hàng #{{order_id}}',
      content: 'Cảm ơn bạn đã đặt hàng tại Fashion Store...',
      variables: ['{{order_id}}', '{{customer_name}}', '{{order_total}}', '{{order_items}}']
    },
    {
      id: 'shipping_confirmation',
      name: 'Thông báo giao hàng',
      icon: 'fas fa-truck',
      subject: 'Đơn hàng #{{order_id}} đang được giao',
      content: 'Đơn hàng của bạn đang trên đường giao...',
      variables: ['{{order_id}}', '{{tracking_number}}', '{{estimated_date}}']
    },
    {
      id: 'password_reset',
      name: 'Đặt lại mật khẩu',
      icon: 'fas fa-key',
      subject: 'Yêu cầu đặt lại mật khẩu',
      content: 'Chúng tôi nhận được yêu cầu đặt lại mật khẩu...',
      variables: ['{{reset_link}}', '{{expiry_hours}}']
    }
  ]
})

// SEO settings
const seo = ref({
  defaultTitle: 'Fashion Store - Thời trang nam nữ cao cấp',
  defaultDescription: 'Fashion Store cung cấp các sản phẩm thời trang nam nữ chất lượng cao với giá cả hợp lý.',
  keywords: 'thời trang, quần áo, áo thun, đầm, phụ kiện, thời trang nam, thời trang nữ',
  gaId: 'UA-12345678-1',
  fbPixel: '123456789012345'
})

// Security settings
const security = ref({
  twoFactor: false,
  loginNotification: true,
  sessionTimeout: 30,
  maxLoginAttempts: 5
})

// Methods
const toggleTemplate = (id) => {
  expandedTemplate.value = expandedTemplate.value === id ? null : id
}

const saveGeneralSettings = () => {
  showNotification('Cập nhật cài đặt chung thành công!', 'success')
}

const saveShippingSettings = () => {
  showNotification('Cập nhật cài đặt vận chuyển thành công!', 'success')
}

const savePaymentSettings = () => {
  showNotification('Cập nhật cài đặt thanh toán thành công!', 'success')
}

const saveEmailSettings = () => {
  showNotification('Cập nhật cài đặt email thành công!', 'success')
}

const saveSEOSettings = () => {
  showNotification('Cập nhật cài đặt SEO thành công!', 'success')
}

const saveSecuritySettings = () => {
  showNotification('Cập nhật cài đặt bảo mật thành công!', 'success')
}

const createBackup = () => {
  showNotification('Đang tạo bản sao lưu...', 'info')
  setTimeout(() => {
    showNotification('Tạo bản sao lưu thành công!', 'success')
  }, 2000)
}

const restoreBackup = () => {
  if (confirm('Bạn có chắc muốn khôi phục dữ liệu từ bản sao lưu?')) {
    showNotification('Đang khôi phục dữ liệu...', 'info')
    setTimeout(() => {
      showNotification('Khôi phục dữ liệu thành công!', 'success')
    }, 2000)
  }
}
</script>

<style scoped>
.admin-settings {
  padding: 20px;
  animation: fadeIn 0.3s;
}

.page-title {
  font-size: 28px;
  color: var(--dark-gray);
  margin-bottom: 25px;
}

.settings-container {
  display: grid;
  grid-template-columns: 250px 1fr;
  gap: 25px;
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
  overflow: hidden;
}

/* Sidebar */
.settings-sidebar {
  background: var(--light-gray);
  padding: 20px 0;
  border-right: 1px solid var(--border-color);
}

.settings-tab {
  width: 100%;
  padding: 15px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
  color: var(--medium-gray);
  font-weight: 500;
  text-align: left;
}

.settings-tab:hover {
  background: rgba(230, 57, 70, 0.1);
  color: var(--primary-red);
}

.settings-tab.active {
  background: linear-gradient(90deg, rgba(230, 57, 70, 0.2), transparent);
  color: var(--primary-red);
  border-left: 3px solid var(--primary-red);
}

.settings-tab i {
  width: 20px;
  font-size: 16px;
}

/* Content */
.settings-content {
  padding: 30px;
  max-height: 800px;
  overflow-y: auto;
}

.settings-panel h2 {
  color: var(--dark-gray);
  margin-bottom: 25px;
  font-size: 22px;
}

.settings-panel h3 {
  color: var(--dark-gray);
  margin: 20px 0 15px;
  font-size: 18px;
}

/* Form */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: var(--dark-gray);
  font-weight: 500;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  transition: all 0.3s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.form-group input[type="checkbox"] {
  width: auto;
  margin-right: 8px;
}

/* Switch */
.switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #28a745;
}

input:checked + .slider:before {
  transform: translateX(26px);
}

/* Shipping Methods */
.shipping-method-card,
.payment-method-card {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 15px;
  transition: all 0.3s;
}

.shipping-method-card:hover,
.payment-method-card:hover {
  box-shadow: var(--shadow);
}

.method-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.method-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.method-info i {
  font-size: 20px;
  color: var(--primary-red);
}

.method-info h3 {
  margin: 0;
  font-size: 16px;
}

.method-details {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed var(--border-color);
}

/* Email Templates */
.template-card {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  margin-bottom: 15px;
  overflow: hidden;
}

.template-header {
  padding: 15px 20px;
  background: var(--light-gray);
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
}

.template-header:hover {
  background: rgba(230, 57, 70, 0.1);
}

.template-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.template-info i {
  color: var(--primary-red);
  font-size: 16px;
}

.template-info h3 {
  margin: 0;
  font-size: 16px;
}

.template-header .fa-chevron-down {
  transition: transform 0.3s;
}

.template-header .fa-chevron-down.rotated {
  transform: rotate(180deg);
}

.template-content {
  padding: 20px;
  border-top: 1px solid var(--border-color);
}

.template-variables {
  margin-top: 15px;
  padding: 15px;
  background: var(--light-gray);
  border-radius: 8px;
}

.template-variables h4 {
  color: var(--dark-gray);
  margin-bottom: 10px;
  font-size: 14px;
}

.variables-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.variable-tag {
  background: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--primary-red);
  border: 1px solid var(--border-color);
}

/* Backup Section */
.backup-section,
.security-section {
  background: var(--light-gray);
  padding: 20px;
  border-radius: 10px;
  margin-bottom: 25px;
}

.backup-info {
  background: white;
  padding: 15px;
  border-radius: 8px;
  margin: 15px 0;
}

.backup-info p {
  margin-bottom: 5px;
  color: var(--medium-gray);
}

.backup-actions {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

/* Buttons */
.btn-primary {
  margin-top: 10px;
}

.btn-outline {
  background: white;
  border: 1px solid var(--primary-red);
  color: var(--primary-red);
  padding: 12px 25px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 600;
}

.btn-outline:hover {
  background: var(--primary-red);
  color: white;
}

/* Animations */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .settings-container {
    grid-template-columns: 1fr;
  }

  .settings-sidebar {
    border-right: none;
    border-bottom: 1px solid var(--border-color);
    padding: 10px;
    display: flex;
    overflow-x: auto;
  }

  .settings-tab {
    white-space: nowrap;
    padding: 10px 15px;
    border-left: none !important;
    border-bottom: 3px solid transparent;
  }

  .settings-tab.active {
    border-left: none;
    border-bottom-color: var(--primary-red);
  }

  .page-title {
    font-size: 24px;
  }

  .backup-actions {
    flex-direction: column;
  }

  .backup-actions button {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .settings-content {
    padding: 20px;
  }

  .method-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .method-info {
    width: 100%;
  }

  .switch {
    align-self: flex-start;
  }
}
</style>
