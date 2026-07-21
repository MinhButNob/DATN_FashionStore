// src/utils/helpers.js

// Format price to VND
export function formatPrice(price) {
  if (!price && price !== 0) return "0₫";
  const num = Number(price);
  if (isNaN(num)) return "0₫";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(num);
}

// Get category name in Vietnamese
export function getCategoryName(category) {
  const categories = {
    men: "Thời trang nam",
    women: "Thời trang nữ",
    kids: "Trẻ em",
    accessories: "Phụ kiện",
  };
  return categories[category] || category;
}

// Generate star rating HTML classes
export function getStarClass(star, rating) {
  if (star <= Math.floor(rating)) {
    return "fas fa-star";
  } else if (star - rating < 1 && star - rating > 0) {
    return "fas fa-star-half-alt";
  } else {
    return "far fa-star";
  }
}

// Get star rating array
export function getStarRating(rating) {
  const stars = [];
  for (let i = 1; i <= 5; i++) {
    if (i <= Math.floor(rating)) {
      stars.push("fas fa-star");
    } else if (i - rating < 1 && i - rating > 0) {
      stars.push("fas fa-star-half-alt");
    } else {
      stars.push("far fa-star");
    }
  }
  return stars;
}

// Calculate discount percentage
export function calculateDiscount(price, oldPrice) {
  if (!oldPrice) return 0;
  return Math.round(((oldPrice - price) / oldPrice) * 100);
}

// =============================================
// ===== ORDER STATUS FUNCTIONS =====
// =============================================

// Get order status text in Vietnamese
export function getOrderStatusText(status) {
  const statuses = {
    pending: "Chờ xác nhận",
    processing: "Đang xử lý",
    shipped: "Đang giao",
    delivered: "Đã giao",
    cancelled: "Đã hủy",
  };
  return statuses[status] || status;
}

// Get order status class
export function getOrderStatusClass(status) {
  const classes = {
    pending: "warning",
    processing: "info",
    shipped: "primary",
    delivered: "success",
    cancelled: "danger",
  };
  return classes[status] || "secondary";
}

// =============================================
// ===== NEW FUNCTIONS FOR SALE STAFF =====
// =============================================

/**
 * Format date to Vietnamese format
 * @param {string} dateString - Date string
 * @returns {string} Formatted date
 */
export function formatDate(dateString) {
  if (!dateString) return "---";
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return "---";
  return (
    date.toLocaleDateString("vi-VN") +
    " " +
    date.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })
  );
}

/**
 * Get status text for order (chi tiết) - ✅ ĐÃ SỬA "Đã xác nhận"
 * @param {string} status - Status code
 * @returns {string} Status text in Vietnamese
 */
export function getStatusText(status) {
  const map = {
    PENDING_PAYMENT: "Chờ thanh toán",
    CONFIRMED: "Đã xác nhận",
    PROCESSING: "Đang xử lý",
    SHIPPING: "Đang giao hàng",
    DELIVERED: "Đã giao hàng",
    COMPLETED: "Hoàn thành",
    CANCELLED: "Đã hủy",
    RETURN_REQUESTED: "Yêu cầu hoàn hàng",
    RETURN_APPROVED: "Đã duyệt hoàn hàng",
    RETURN_RECEIVED: "Đã nhận hàng trả",
    RETURN_PENDING: "Đang thu hồi hàng",
    RETURNED: "Đã hoàn trả",
    REJECTED: "Từ chối hoàn trả",
    PENDING: "Chờ duyệt",
    APPROVED: "Đã duyệt",
    RECEIVED: "Đã nhận hàng",
  };
  return map[status] || status || "---";
}

/**
 * Get CSS class for order status
 * @param {string} status - Status code
 * @returns {string} CSS class name
 */
export function getStatusClass(status) {
  const map = {
    PENDING_PAYMENT: "status-pending",
    CONFIRMED: "status-confirmed",
    PROCESSING: "status-processing",
    SHIPPING: "status-shipping",
    DELIVERED: "status-delivered",
    COMPLETED: "status-completed",
    CANCELLED: "status-cancelled",
    RETURN_REQUESTED: "status-return-requested",
    RETURN_APPROVED: "status-return-approved",
    RETURN_RECEIVED: "status-return-received",
    RETURN_PENDING: "status-return-pending",
    RETURNED: "status-returned",
    REJECTED: "status-rejected",
    PENDING: "status-pending-review",
    APPROVED: "status-approved",
    RECEIVED: "status-return-received",
  };
  return map[status] || "";
}

/**
 * Get payment status text in Vietnamese
 * @param {string} status - Payment status code
 * @returns {string} Payment status text
 */
export function getPaymentStatusText(status) {
  const map = {
    PAID: "Đã thanh toán",
    UNPAID: "Chưa thanh toán",
    PENDING: "Đang xử lý",
    FAILED: "Thất bại",
    REFUNDED: "Đã hoàn tiền",
    CANCELLED: "Đã hủy",
    SUCCESS: "Thành công",
  };
  return map[status] || status || "---";
}

/**
 * Get CSS class for payment status
 * @param {string} status - Payment status code
 * @returns {string} CSS class name
 */
export function getPaymentClass(status) {
  const map = {
    PAID: "payment-paid",
    UNPAID: "payment-unpaid",
    PENDING: "payment-pending",
    FAILED: "payment-failed",
    REFUNDED: "payment-refunded",
    CANCELLED: "payment-cancelled",
    SUCCESS: "payment-success",
  };
  return map[status] || "";
}

/**
 * Get full status text with description
 * @param {string} status - Status code
 * @returns {string} Full status description
 */
export function getFullStatusText(status) {
  const map = {
    PENDING_PAYMENT: "Đơn hàng đang chờ thanh toán",
    CONFIRMED: "Đơn hàng đã được xác nhận",
    PROCESSING: "Đơn hàng đang được xử lý",
    SHIPPING: "Đơn hàng đang trên đường giao",
    DELIVERED: "Đơn hàng đã được giao",
    COMPLETED: "Đơn hàng đã hoàn thành",
    CANCELLED: "Đơn hàng đã bị hủy",
    RETURN_REQUESTED: "Khách hàng yêu cầu hoàn hàng",
    RETURN_APPROVED: "Yêu cầu hoàn hàng đã được duyệt",
    RETURN_RECEIVED: "Đã nhận hàng trả từ khách",
    RETURN_PENDING: "Đang thu hồi hàng",
    RETURNED: "Đã hoàn trả hàng",
    REJECTED: "Yêu cầu hoàn hàng bị từ chối",
  };
  return map[status] || status || "---";
}

/**
 * Get color hex from color name
 * @param {string} colorName - Color name
 * @returns {string} Hex color code
 */
export function getColorHex(colorName) {
  const colorMap = {
    Đỏ: "#FF0000",
    Đen: "#000000",
    Trắng: "#FFFFFF",
    Xanh: "#0000FF",
    "Xanh dương": "#0000FF",
    Navy: "#1B2951",
    Vàng: "#FFFF00",
    Hồng: "#FFC0CB",
    Xám: "#808080",
    Nâu: "#8B4513",
    Tím: "#800080",
    Cam: "#FFA500",
    "Xanh lá": "#00FF00",
    "Xanh lục": "#00FF00",
    Be: "#F5F5DC",
    Ghi: "#C0C0C0",
    Bạc: "#C0C0C0",
    "Vàng kim": "#FFD700",
    Đồng: "#CD7F32",
  };
  return colorMap[colorName] || "#CCCCCC";
}

/**
 * Format quantity with thousand separators
 * @param {number} quantity - Quantity number
 * @returns {string} Formatted quantity
 */
export function formatQuantity(quantity) {
  if (!quantity && quantity !== 0) return "0";
  return Number(quantity).toLocaleString("vi-VN");
}

/**
 * Calculate discount percentage
 * @param {number} originalPrice - Original price
 * @param {number} discountedPrice - Discounted price
 * @returns {number} Discount percentage
 */
export function calculateDiscountPercent(originalPrice, discountedPrice) {
  if (!originalPrice || !discountedPrice || originalPrice <= discountedPrice)
    return 0;
  const discount = ((originalPrice - discountedPrice) / originalPrice) * 100;
  return Math.round(discount);
}

/**
 * Truncate text with ellipsis
 * @param {string} text - Text to truncate
 * @param {number} length - Maximum length
 * @returns {string} Truncated text
 */
export function truncateText(text, length = 50) {
  if (!text) return "";
  if (text.length <= length) return text;
  return text.substring(0, length) + "...";
}

/**
 * Check if string is empty or whitespace
 * @param {string} str - String to check
 * @returns {boolean} True if empty
 */
export function isEmpty(str) {
  return !str || str.trim() === "";
}

/**
 * Get initials from name
 * @param {string} name - Full name
 * @returns {string} Initials (max 2 characters)
 */
export function getInitials(name) {
  if (!name) return "?";
  const parts = name.trim().split(" ");
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

/**
 * Generate random ID
 * @param {number} length - Length of ID
 * @returns {string} Random ID
 */
export function generateId(length = 8) {
  const chars =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  let result = "";
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

/**
 * Debounce function for search input
 * @param {Function} fn - Function to debounce
 * @param {number} delay - Delay in milliseconds
 * @returns {Function} Debounced function
 */
export function debounce(fn, delay = 300) {
  let timer = null;
  return function (...args) {
    clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
}

// =============================================
// ===== NEW: GET ORDER STATUS COLOR =====
// =============================================

/**
 * Get color for order status (for UI)
 * @param {string} status - Status code
 * @returns {string} Color hex
 */
export function getStatusColor(status) {
  const map = {
    PENDING_PAYMENT: "#f39c12",
    CONFIRMED: "#3498db",
    PROCESSING: "#3498db",
    SHIPPING: "#17a2b8",
    DELIVERED: "#28a745",
    COMPLETED: "#28a745",
    CANCELLED: "#dc3545",
    RETURN_REQUESTED: "#f39c12",
    RETURN_APPROVED: "#3498db",
    RETURN_RECEIVED: "#3498db",
    RETURN_PENDING: "#17a2b8",
    RETURNED: "#28a745",
    REJECTED: "#dc3545",
  };
  return map[status] || "#6c757d";
}

// =============================================
// ===== EXPORT ALL FUNCTIONS =====
// =============================================

export default {
  formatPrice,
  formatDate,
  getCategoryName,
  getStarClass,
  getStarRating,
  calculateDiscount,
  getOrderStatusText,
  getOrderStatusClass,
  getStatusText,
  getStatusClass,
  getPaymentStatusText,
  getPaymentClass,
  getFullStatusText,
  getColorHex,
  formatQuantity,
  calculateDiscountPercent,
  truncateText,
  isEmpty,
  getInitials,
  generateId,
  debounce,
  getStatusColor,
};
