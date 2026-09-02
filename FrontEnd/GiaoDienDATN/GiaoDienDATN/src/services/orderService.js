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
  // Lấy danh sách đơn hàng cho Admin có phân trang và bộ lọc
  getAllOrders(params = {}) {
    const queryParams = {};
    if (params.status && params.status !== "all") {
      queryParams.status = params.status;
    }
    if (params.keyword) {
      queryParams.keyword = params.keyword;
    }
    queryParams.page = params.page || 0;
    queryParams.size = params.size || 10;

    return api.get("/admin/orders", {
      params: queryParams
    });
  },

  // Lấy chi tiết đơn hàng
  getOrderDetail(orderId) {
    return api.get(`/admin/orders/${orderId}`);
  },

  // Cập nhật trạng thái đơn hàng
  updateOrderStatus(orderId, data) {
    const userInfo = getLoggedInUserInfo();
    const statusVal = typeof data === "string" ? data : (data?.orderStatus || data);
    const notesVal = typeof data === "object" ? (data?.notes || "") : "";

    const requestData = {
      orderStatus: statusVal,
      notes: notesVal,
      employeeId: data?.employeeId || userInfo.employeeId,
      userId: data?.userId || userInfo.userId,
      confirmedBy: data?.confirmedBy || userInfo.confirmedBy,
      processedBy: data?.processedBy || userInfo.processedBy,
      processedByEmail: data?.processedByEmail || userInfo.processedByEmail,
      updatedBy: data?.updatedBy || userInfo.updatedBy,
      username: userInfo.username,
      employeeName: userInfo.employeeName,
      ...(typeof data === "object" ? data : {})
    };
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