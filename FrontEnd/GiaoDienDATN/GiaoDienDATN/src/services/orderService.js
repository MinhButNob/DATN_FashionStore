// src/services/orderService.js
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

// Get auth token
const getAuthHeader = () => {
  const token = localStorage.getItem("token");
  return token ? { Authorization: `Bearer ${token}` } : {};
};

// ============= CUSTOMER ORDER =============
export const orderService = {
  // Tạo đơn hàng mới
  createOrder(orderData) {
    return axios.post(`${API_BASE}/orders/create`, orderData, {
      headers: getAuthHeader(),
    });
  },

  // Lấy danh sách đơn hàng của tôi
  getMyOrders(status = null, page = 0, size = 10) {
    const params = { page, size };
    if (status && status !== "all") {
      params.status = status;
    }
    return axios.get(`${API_BASE}/orders`, {
      params,
      headers: getAuthHeader(),
    });
  },

  // Lấy chi tiết đơn hàng
  getOrderDetail(orderId) {
    return axios.get(`${API_BASE}/orders/${orderId}`, {
      headers: getAuthHeader(),
    });
  },

  // Hủy đơn hàng
  cancelOrder(orderId, reason) {
    return axios.put(`${API_BASE}/orders/${orderId}/cancel`, null, {
      params: { reason },
      headers: getAuthHeader(),
    });
  },

  // Yêu cầu hoàn hàng (có ảnh)
  requestReturn(orderId, data) {
    return axios.post(`${API_BASE}/orders/${orderId}/return-request`, data, {
      headers: {
        ...getAuthHeader(),
        "Content-Type": "application/json",
      },
    });
  },

  // Lấy danh sách yêu cầu hoàn hàng của tôi
  getMyReturnRequests(page = 0, size = 10) {
    return axios.get(`${API_BASE}/orders/return-requests`, {
      params: { page, size },
      headers: getAuthHeader(),
    });
  },
};

// ============= ADMIN ORDER =============
export const adminOrderService = {
  // Lấy tất cả đơn hàng
  getAllOrders(params = {}) {
    const queryParams = {
      page: params.page || 0,
      size: params.size || 10,
    };
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
    return axios.get(`${API_BASE}/admin/orders`, {
      params: queryParams,
      headers: getAuthHeader(),
    });
  },

  // Lấy chi tiết đơn hàng
  getOrderDetail(orderId) {
    return axios.get(`${API_BASE}/admin/orders/${orderId}`, {
      headers: getAuthHeader(),
    });
  },

  // Cập nhật trạng thái đơn hàng
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

  // Cập nhật trạng thái thanh toán
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

  // Admin hủy đơn hàng
  adminCancelOrder(orderId, data) {
    return axios.put(`${API_BASE}/admin/orders/${orderId}/cancel`, data, {
      headers: {
        ...getAuthHeader(),
        "Content-Type": "application/json",
      },
    });
  },

  // Khôi phục đơn hàng
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
