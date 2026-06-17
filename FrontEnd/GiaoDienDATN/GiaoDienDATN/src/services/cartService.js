// src/services/cartService.js
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

const getAuthHeader = () => {
  const token =
    localStorage.getItem("token") || localStorage.getItem("accessToken");
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const cartService = {
  // Áp dụng voucher
  applyVoucher(voucherCode) {
    return axios.post(`${API_BASE}/cart/apply-voucher`, null, {
      params: { voucherCode },
      headers: getAuthHeader(),
    });
  },

  // Xóa voucher
  removeVoucher() {
    return axios.delete(`${API_BASE}/cart/remove-voucher`, {
      headers: getAuthHeader(),
    });
  },

  // Lấy giỏ hàng
  getCart() {
    return axios.get(`${API_BASE}/cart`, {
      headers: getAuthHeader(),
    });
  },

  // Thêm vào giỏ
  addToCart(data) {
    return axios.post(`${API_BASE}/cart/add`, data, {
      headers: getAuthHeader(),
    });
  },

  // Cập nhật số lượng
  updateCartItem(cartItemId, quantity) {
    return axios.put(`${API_BASE}/cart/items/${cartItemId}`, null, {
      params: { quantity },
      headers: getAuthHeader(),
    });
  },

  // Xóa khỏi giỏ
  removeFromCart(cartItemId) {
    return axios.delete(`${API_BASE}/cart/items/${cartItemId}`, {
      headers: getAuthHeader(),
    });
  },

  // Xóa toàn bộ giỏ
  clearCart() {
    return axios.delete(`${API_BASE}/cart/clear`, {
      headers: getAuthHeader(),
    });
  },
};
