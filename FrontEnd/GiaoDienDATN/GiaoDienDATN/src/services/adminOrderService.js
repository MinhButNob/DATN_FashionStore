// src/services/adminOrderService.js
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

const getAuthHeader = () => {
  const token = localStorage.getItem("token");
  return token ? { Authorization: `Bearer ${token}` } : {};
};

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

    return axios.get(`${API_BASE}/admin/orders`, {
      params: queryParams,
      headers: {
        ...getAuthHeader(),
        "Content-Type": "application/json",
      },
    });
  },

  // Lấy chi tiết đơn hàng (Admin)
  getOrderDetail(orderId) {
    return axios.get(`${API_BASE}/admin/orders/${orderId}`, {
      headers: getAuthHeader(),
    });
  },

  // Cập nhật trạng thái đơn hàng (Admin)
  updateOrderStatus(orderId, data) {
    const requestData = {
      orderStatus: data.orderStatus || data,
      notes: data.notes || "",
    };
    return axios.put(
      `${API_BASE}/admin/orders/${orderId}/status`,
      requestData,
      {
        headers: {
          ...getAuthHeader(),
          "Content-Type": "application/json",
        },
      },
    );
  },

  // Cập nhật trạng thái thanh toán (Admin)
  updatePaymentStatus(orderId, paymentStatus) {
    return axios.put(
      `${API_BASE}/admin/orders/${orderId}/payment-status`,
      null,
      {
        params: { paymentStatus },
        headers: getAuthHeader(),
      },
    );
  },

  // Lấy thống kê đơn hàng
  getStatistics() {
    return axios.get(`${API_BASE}/admin/orders/statistics`, {
      headers: getAuthHeader(),
    });
  },

  // Lấy số lượng theo trạng thái
  getStatusCounts() {
    return axios.get(`${API_BASE}/admin/orders/status/counts`, {
      headers: getAuthHeader(),
    });
  },

  // ✅ Admin hủy đơn hàng (thêm mới)
  adminCancelOrder(orderId, data) {
    return axios.put(`${API_BASE}/admin/orders/${orderId}/cancel`, data, {
      headers: {
        ...getAuthHeader(),
        "Content-Type": "application/json",
      },
    });
  },

  // ✅ Khôi phục đơn hàng (thêm mới)
  restoreOrder(orderId) {
    return axios.put(
      `${API_BASE}/admin/orders/${orderId}/restore`,
      {},
      {
        headers: getAuthHeader(),
      },
    );
  },

  // Xóa đơn hàng (soft delete)
  deleteOrder(orderId) {
    return axios.delete(`${API_BASE}/admin/orders/${orderId}`, {
      headers: getAuthHeader(),
    });
  },

  // Gửi thông báo cho khách hàng
  notifyCustomer(orderId, data) {
    return axios.post(`${API_BASE}/admin/orders/${orderId}/notify`, data, {
      headers: {
        ...getAuthHeader(),
        "Content-Type": "application/json",
      },
    });
  },
};
