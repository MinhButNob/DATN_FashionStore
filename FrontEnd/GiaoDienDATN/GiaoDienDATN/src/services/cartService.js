// src/services/cartService.js
import api from "@/api/axios";

export const cartService = {
  // Áp dụng voucher
  applyVoucher(voucherCode) {
    return api.post("/cart/apply-voucher", null, {
      params: { voucherCode } });
  },

  // Xóa voucher
  removeVoucher() {
    return api.delete("/cart/remove-voucher");
  },

  // Lấy giỏ hàng
  getCart() {
    return api.get("/cart");
  },

  // Thêm vào giỏ
  addToCart(data) {
    return api.post("/cart/add", data);
  },

  // Cập nhật số lượng
  updateCartItem(cartItemId, quantity) {
    return api.put(`/cart/items/${cartItemId}`, null, {
      params: { quantity } });
  },

  // Xóa khỏi giỏ
  removeFromCart(cartItemId) {
    return api.delete(`/cart/items/${cartItemId}`);
  },

  // Xóa toàn bộ giỏ
  clearCart() {
    return api.delete("/cart/clear");
  } };