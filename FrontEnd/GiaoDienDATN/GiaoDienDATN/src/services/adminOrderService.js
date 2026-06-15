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
    return axios.get(`${API_BASE}/admin/orders`, {
      params,
      headers: getAuthHeader(),
    });
  },

  // Lấy chi tiết đơn hàng (Admin)
  getOrderDetail(orderId) {
    return axios.get(`${API_BASE}/admin/orders/${orderId}`, {
      headers: getAuthHeader(),
    });
  },

  // ✅ SỬA: Cập nhật trạng thái đơn hàng (Admin)
  updateOrderStatus(orderId, data) {
    // Đảm bảo dữ liệu gửi lên đúng format backend yêu cầu
    const requestData = {
      orderStatus: data.orderStatus || data,
      notes: data.notes || "",
    };

    console.log("📤 Updating order status:", orderId, requestData);

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

  // Xóa đơn hàng (soft delete)
  deleteOrder(orderId) {
    return axios.delete(`${API_BASE}/admin/orders/${orderId}`, {
      headers: getAuthHeader(),
    });
  },
};
