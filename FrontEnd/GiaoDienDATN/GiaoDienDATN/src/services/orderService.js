// src/services/orderService.js
import api from "@/api/axios";

// ============= CUSTOMER ORDER =============
export const orderService = {
  // Tạo đơn hàng mới
  createOrder(orderData) {
    return api.post("/orders/create", orderData);
  },

  // Lấy danh sách đơn hàng của tôi
  getMyOrders(status = null, page = 0, size = 10) {
    const params = { page, size };
    if (status && status !== "all") {
      params.status = status;
    }
    return api.get("/orders", { params });
  },

  // Lấy chi tiết đơn hàng
  getOrderDetail(orderId) {
    return api.get(`/orders/${orderId}`);
  },

  // Hủy đơn hàng
  cancelOrder(orderId, reason) {
    return api.put(`/orders/${orderId}/cancel`, null, {
      params: { reason } });
  },

  // Yêu cầu hoàn hàng (có ảnh)
  requestReturn(orderId, data) {
    return api.post(`/orders/${orderId}/return-request`, data);
  },

  // Lấy danh sách yêu cầu hoàn hàng của tôi
  getMyReturnRequests(page = 0, size = 10) {
    return api.get("/orders/return-requests", {
      params: { page, size } });
  } };

// ============= ADMIN ORDER =============
export const adminOrderService = {
  // Lấy tất cả đơn hàng
  getAllOrders(params = {}) {
    const queryParams = {
      page: params.page || 0,
      size: params.size || 10 };
    if (params.status && params.status !== "all") {
      queryParams.status = params.status;
    }
    if (params.keyword) {
      queryParams.keyword = params.keyword;
    }
    if (params.fromDate) {
      queryParams.fromDate = params.fromDate;
    }
    if (params.toDate) {
      queryParams.toDate = params.toDate;
    }
    return api.get("/admin/orders", {
      params: queryParams });
  },

  // Lấy chi tiết đơn hàng
  getOrderDetail(orderId) {
    return api.get(`/admin/orders/${orderId}`);
  },

  // Cập nhật trạng thái đơn hàng
  updateOrderStatus(orderId, data) {
    const requestData = {
      orderStatus: data.orderStatus || data,
      notes: data.notes || "" };
    return api.put(`/admin/orders/${orderId}/status`, requestData);
  },

  // Cập nhật trạng thái thanh toán
  updatePaymentStatus(orderId, paymentStatus) {
    return api.put(`/admin/orders/${orderId}/payment-status`, null, {
      params: { paymentStatus } });
  },

  // Lấy thống kê đơn hàng
  getStatistics() {
    return api.get("/admin/orders/statistics");
  },

  // Lấy số lượng theo trạng thái
  getStatusCounts() {
    return api.get("/admin/orders/status/counts");
  },

  // Admin hủy đơn hàng
  adminCancelOrder(orderId, data) {
    return api.put(`/admin/orders/${orderId}/cancel`, data);
  },

  // Khôi phục đơn hàng
  restoreOrder(orderId) {
    return api.put(`/admin/orders/${orderId}/restore`, {});
  },

  // Xóa đơn hàng (soft delete)
  deleteOrder(orderId) {
    return api.delete(`/admin/orders/${orderId}`);
  },

  // Gửi thông báo cho khách hàng
  notifyCustomer(orderId, data) {
    return api.post(`/admin/orders/${orderId}/notify`, data);
  } };