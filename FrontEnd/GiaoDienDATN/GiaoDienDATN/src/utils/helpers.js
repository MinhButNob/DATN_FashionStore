// Format price to VND
export function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(price);
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

// Get order status text
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
