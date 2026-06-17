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
    if (status && status !== "all") params.status = status;
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
};

// ============= ADMIN ORDER =============
export const adminOrderService = {
  // Lấy tất cả đơn hàng
  getAllOrders(
    status = null,
    fromDate = null,
    toDate = null,
    keyword = null,
    page = 0,
    size = 10,
  ) {
    const params = { page, size };
    if (status) params.status = status;
    if (fromDate) params.fromDate = fromDate;
    if (toDate) params.toDate = toDate;
    if (keyword) params.keyword = keyword;
    return axios.get(`${API_BASE}/admin/orders`, {
      params,
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
};
