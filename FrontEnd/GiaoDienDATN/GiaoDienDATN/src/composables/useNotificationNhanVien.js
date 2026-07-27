import { ref, readonly } from 'vue';

// =============================================
// STATE - Quản lý notifications cho nhân viên
// =============================================
const notifications = ref([]);
let notificationId = 0;

// =============================================
// CONSTANTS
// =============================================
export const NOTIFICATION_TYPES = {
  SUCCESS: 'success',
  ERROR: 'error',
  WARNING: 'warning',
  INFO: 'info' };

export const NOTIFICATION_DURATION = {
  SHORT: 2000,
  MEDIUM: 3000,
  LONG: 5000,
  EXTRA_LONG: 8000 };

const DEFAULT_DURATION = 3000;
const MAX_NOTIFICATIONS = 5;

// =============================================
// MAIN COMPOSABLE
// =============================================
export function useNotificationNhanVien() {
  // === CORE FUNCTIONS ===
  
  // Thêm notification mới
  const showNotification = (message, type = NOTIFICATION_TYPES.INFO, duration = DEFAULT_DURATION) => {
    const id = ++notificationId;
    const notification = {
      id,
      message,
      type,
      duration,
      timestamp: Date.now(),
      visible: true };

    notifications.value.push(notification);

    // Giới hạn số lượng notification
    if (notifications.value.length > MAX_NOTIFICATIONS) {
      notifications.value.shift();
    }

    // Tự động ẩn sau duration
    if (duration > 0) {
      setTimeout(() => {
        removeNotification(id);
      }, duration);
    }

    return id;
  };

  // Xóa notification theo id
  const removeNotification = (id) => {
    const index = notifications.value.findIndex(n => n.id === id);
    if (index !== -1) {
      // Animation fade out
      notifications.value[index].visible = false;
      setTimeout(() => {
        const currentIndex = notifications.value.findIndex(n => n.id === id);
        if (currentIndex !== -1) {
          notifications.value.splice(currentIndex, 1);
        }
      }, 300);
    }
  };

  // Xóa tất cả notifications
  const clearNotifications = () => {
    notifications.value = [];
  };

  // === HELPER FUNCTIONS ===
  
  // Success
  const success = (message, duration = DEFAULT_DURATION) => {
    return showNotification(message, NOTIFICATION_TYPES.SUCCESS, duration);
  };

  // Error
  const error = (message, duration = DEFAULT_DURATION) => {
    return showNotification(message, NOTIFICATION_TYPES.ERROR, duration);
  };

  // Warning
  const warning = (message, duration = DEFAULT_DURATION) => {
    return showNotification(message, NOTIFICATION_TYPES.WARNING, duration);
  };

  // Info
  const info = (message, duration = DEFAULT_DURATION) => {
    return showNotification(message, NOTIFICATION_TYPES.INFO, duration);
  };

  // === ADVANCED FUNCTIONS ===
  
  // Hiển thị lỗi từ API response
  const showApiError = (error, defaultMessage = 'Có lỗi xảy ra') => {
    const message = error?.response?.data?.message || 
                   error?.response?.data?.error ||
                   error?.message || 
                   defaultMessage;
    return error(message, NOTIFICATION_DURATION.LONG);
  };

  // Hiển thị thành công với icon
  const successWithIcon = (message, icon = '✅') => {
    return success(`${icon} ${message}`);
  };

  // Hiển thị lỗi với icon
  const errorWithIcon = (message, icon = '❌') => {
    return error(`${icon} ${message}`);
  };

  // Hiển thị warning với icon
  const warningWithIcon = (message, icon = '⚠️') => {
    return warning(`${icon} ${message}`);
  };

  // Hiển thị info với icon
  const infoWithIcon = (message, icon = 'ℹ️') => {
    return info(`${icon} ${message}`);
  };

  // === BATCH FUNCTIONS ===
  
  // Hiển thị nhiều notifications cùng lúc
  const showBatch = (messages, type = NOTIFICATION_TYPES.INFO, duration = DEFAULT_DURATION) => {
    const ids = [];
    messages.forEach(msg => {
      ids.push(showNotification(msg, type, duration));
    });
    return ids;
  };

  // === RETURN ===
  return {
    // State (readonly để không bị thay đổi từ bên ngoài)
    notifications: readonly(notifications),
    
    // Core functions
    showNotification,
    removeNotification,
    clearNotifications,
    
    // Helper functions
    success,
    error,
    warning,
    info,
    
    // Advanced functions
    showApiError,
    successWithIcon,
    errorWithIcon,
    warningWithIcon,
    infoWithIcon,
    
    // Batch functions
    showBatch,
    
    // Constants
    NOTIFICATION_TYPES,
    NOTIFICATION_DURATION };
}

// =============================================
// COMPONENT HIỂN THỊ NOTIFICATION
// =============================================
export const NotificationContainerNhanVien = {
  name: 'NotificationContainerNhanVien',
  setup() {
    const { notifications, removeNotification } = useNotificationNhanVien();
    
    // Lấy icon theo loại
    const getIcon = (type) => {
      const icons = {
        success: 'fas fa-check-circle',
        error: 'fas fa-exclamation-circle',
        warning: 'fas fa-exclamation-triangle',
        info: 'fas fa-info-circle' };
      return icons[type] || icons.info;
    };

    // Lấy màu sắc theo loại
    const getColor = (type) => {
      const colors = {
        success: '#28a745',
        error: '#dc3545',
        warning: '#ffc107',
        info: '#17a2b8' };
      return colors[type] || colors.info;
    };

    // Lấy background color
    const getBackgroundColor = (type) => {
      const colors = {
        success: '#d4edda',
        error: '#f8d7da',
        warning: '#fff3cd',
        info: '#d1ecf1' };
      return colors[type] || colors.info;
    };

    return {
      notifications,
      removeNotification,
      getIcon,
      getColor,
      getBackgroundColor };
  },
  template: `
    <div class="notification-container-nhanvien">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        class="notification-item-nhanvien"
        :class="[
          notification.type,
          { 'fade-out': !notification.visible }
        ]"
        @click="removeNotification(notification.id)"
      >
        <div class="notification-content-nhanvien">
          <div class="notification-icon-nhanvien" :style="{ background: getBackgroundColor(notification.type) }">
            <i :class="getIcon(notification.type)" :style="{ color: getColor(notification.type) }"></i>
          </div>
          <div class="notification-message-nhanvien">
            <span>{{ notification.message }}</span>
          </div>
          <button class="notification-close-nhanvien" @click.stop="removeNotification(notification.id)">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div 
          class="notification-progress-nhanvien" 
          :style="{ animationDuration: notification.duration + 'ms' }"
          :class="notification.type"
        ></div>
      </div>
    </div>
  ` };

// =============================================
// STYLES - CSS cho notification
// =============================================
export const notificationStyles = `
  .notification-container-nhanvien {
    position: fixed;
    top: 80px;
    right: 20px;
    z-index: 9999;
    display: flex;
    flex-direction: column;
    gap: 10px;
    max-width: 420px;
    width: 100%;
    pointer-events: none;
  }

  .notification-item-nhanvien {
    pointer-events: auto;
    background: white;
    border-radius: 12px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    overflow: hidden;
    animation: slideInRightNhanVien 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
    cursor: pointer;
    border-left: 5px solid #17a2b8;
    transition: all 0.3s ease;
  }

  .notification-item-nhanvien:hover {
    transform: translateX(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  }

  .notification-item-nhanvien.success {
    border-left-color: #28a745;
  }

  .notification-item-nhanvien.error {
    border-left-color: #dc3545;
  }

  .notification-item-nhanvien.warning {
    border-left-color: #ffc107;
  }

  .notification-item-nhanvien.info {
    border-left-color: #17a2b8;
  }

  .notification-item-nhanvien.fade-out {
    animation: slideOutRightNhanVien 0.3s ease forwards;
  }

  .notification-content-nhanvien {
    padding: 16px 18px;
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .notification-icon-nhanvien {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .notification-icon-nhanvien i {
    font-size: 18px;
  }

  .notification-message-nhanvien {
    flex: 1;
  }

  .notification-message-nhanvien span {
    font-size: 14px;
    color: #1a1a2e;
    line-height: 1.5;
    font-weight: 500;
  }

  .notification-close-nhanvien {
    background: none;
    border: none;
    color: #999;
    cursor: pointer;
    padding: 6px;
    border-radius: 50%;
    transition: all 0.2s;
    font-size: 14px;
    flex-shrink: 0;
  }

  .notification-close-nhanvien:hover {
    background: #f5f5f5;
    color: #666;
  }

  .notification-progress-nhanvien {
    height: 3px;
    background: #17a2b8;
    animation: progressBarNhanVien linear forwards;
    transform-origin: left;
  }

  .notification-progress-nhanvien.success {
    background: #28a745;
  }

  .notification-progress-nhanvien.error {
    background: #dc3545;
  }

  .notification-progress-nhanvien.warning {
    background: #ffc107;
  }

  .notification-progress-nhanvien.info {
    background: #17a2b8;
  }

  @keyframes slideInRightNhanVien {
    from {
      transform: translateX(120%) scale(0.8);
      opacity: 0;
    }
    to {
      transform: translateX(0) scale(1);
      opacity: 1;
    }
  }

  @keyframes slideOutRightNhanVien {
    from {
      transform: translateX(0) scale(1);
      opacity: 1;
    }
    to {
      transform: translateX(120%) scale(0.8);
      opacity: 0;
    }
  }

  @keyframes progressBarNhanVien {
    from {
      transform: scaleX(1);
    }
    to {
      transform: scaleX(0);
    }
  }

  @media (max-width: 480px) {
    .notification-container-nhanvien {
      top: 70px;
      right: 10px;
      left: 10px;
      max-width: none;
    }
  }
`;