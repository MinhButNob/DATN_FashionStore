// src/services/adminOrderService.js
import api from "@/api/axios";

export const adminOrderService = {
  // Lấy tất cả đơn hàng (Admin)
  getAllOrders(params = {}) {
    const queryParams = {};

    // ✅ Nếu có status và khác 'all', xử lý chuỗi status
    if (params.status && params.status !== "all") {
      queryParams.status = params.status;
    }
    if (params.keyword) {
      queryParams.keyword = params.keyword;
    }
    queryParams.page = params.page || 0;
    queryParams.size = params.size || 10;

    console.log("📤 Calling API with params:", queryParams);

    return api.get("/admin/orders", {
      params: queryParams });
  },

  // Lấy chi tiết đơn hàng (Admin)
  getOrderDetail(orderId) {
    return api.get(`/admin/orders/${orderId}`);
  },

  getReturnRequestByOrderId(orderId) {
    return api.get(`/admin/orders/return-requests/order/${orderId}`);
  },

  // Cập nhật trạng thái đơn hàng (Admin)
  updateOrderStatus(orderId, data) {
    const requestData = {
      orderStatus: data.orderStatus || data,
      notes: data.notes || "" };
    return api.put(`/admin/orders/${orderId}/status`, requestData);
  },

  // Cập nhật trạng thái thanh toán (Admin)
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

  // ✅ Admin hủy đơn hàng (thêm mới)
  adminCancelOrder(orderId, data) {
    return api.put(`/admin/orders/${orderId}/cancel`, data);
  },

  // ✅ Khôi phục đơn hàng (thêm mới)
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
  },

  inspectReturn(returnRequestId, data) {
    return api.post(`/admin/returns/${returnRequestId}/inspect`, data);
  } };
