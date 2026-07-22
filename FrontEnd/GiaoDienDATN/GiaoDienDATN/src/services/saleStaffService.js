// src/services/saleStaffService.js
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

const getAuthHeader = () => {
  const token = localStorage.getItem("token");
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const saleStaffService = {
  // ===== ORDERS =====
  getAllOrders(params = {}) {
    const queryParams = {
      page: params.page || 0,
      size: params.size || 10,
      keyword: params.keyword || "",
      status: params.status || "",
    };
    return axios.get(`${API_BASE}/sale-staff/orders`, {
      params: queryParams,
      headers: getAuthHeader(),
    });
  },

  getOrderDetail(orderId) {
    return axios.get(`${API_BASE}/sale-staff/orders/${orderId}`, {
      headers: getAuthHeader(),
    });
  },

  updateOrderStatus(orderId, data) {
    return axios.put(`${API_BASE}/sale-staff/orders/${orderId}/status`, data, {
      headers: getAuthHeader(),
    });
  },

  updatePaymentStatus(orderId, paymentStatus) {
    return axios.put(
      `${API_BASE}/sale-staff/orders/${orderId}/payment-status`,
      null,
      { params: { paymentStatus }, headers: getAuthHeader() },
    );
  },

  cancelOrder(orderId, reason) {
    return axios.put(
      `${API_BASE}/sale-staff/orders/${orderId}/cancel`,
      { reason },
      { headers: getAuthHeader() },
    );
  },

  getStatistics() {
    return axios.get(`${API_BASE}/sale-staff/orders/statistics`, {
      headers: getAuthHeader(),
    });
  },

  getStatusCounts() {
    return axios.get(`${API_BASE}/sale-staff/orders/status/counts`, {
      headers: getAuthHeader(),
    });
  },

  getInvoice(orderId) {
    return axios.get(`${API_BASE}/sale-staff/orders/${orderId}/invoice`, {
      headers: getAuthHeader(),
    });
  },

  // ===== RETURN REQUESTS =====
  getReturnRequestByOrderId(orderId) {
    return axios.get(
      `${API_BASE}/sale-staff/orders/return-requests/order/${orderId}`,
      {
        headers: getAuthHeader(),
      },
    );
  },

  processReturnRequest(orderId, data) {
    return axios.put(
      `${API_BASE}/sale-staff/orders/return-requests/${orderId}/process`,
      data,
      { headers: getAuthHeader() },
    );
  },

  receiveReturnedItems(orderId, note) {
    return axios.put(
      `${API_BASE}/sale-staff/orders/return-requests/${orderId}/receive`,
      { note },
      { headers: getAuthHeader() },
    );
  },

  getAllReturnRequests(params = {}) {
    return axios.get(`${API_BASE}/sale-staff/orders/return-requests`, {
      params: {
        page: params.page || 0,
        size: params.size || 10,
        status: params.status || "",
      },
      headers: getAuthHeader(),
    });
  },

  // ===== PRODUCTS (STOCK ONLY) =====
  updateStock(productDetailId, quantity) {
    return axios.put(
      `${API_BASE}/sale-staff/products/stock/${productDetailId}`,
      { quantity },
      { headers: getAuthHeader() },
    );
  },

  increaseStock(productDetailId, quantity) {
    return axios.post(
      `${API_BASE}/sale-staff/products/stock/${productDetailId}/increase`,
      { quantity },
      { headers: getAuthHeader() },
    );
  },

  decreaseStock(productDetailId, quantity) {
    return axios.post(
      `${API_BASE}/sale-staff/products/stock/${productDetailId}/decrease`,
      { quantity },
      { headers: getAuthHeader() },
    );
  },

  getProductDetail(productDetailId) {
    return axios.get(
      `${API_BASE}/sale-staff/products/details/${productDetailId}`,
      {
        headers: getAuthHeader(),
      },
    );
  },

  // ===== CUSTOMERS =====
  getCustomers(params = {}) {
    return axios.get(`${API_BASE}/sale-staff/customers`, {
      params: {
        page: params.page || 0,
        size: params.size || 10,
        keyword: params.keyword || "",
        status: params.status || "",
      },
      headers: getAuthHeader(),
    });
  },

  getCustomerById(id) {
    return axios.get(`${API_BASE}/sale-staff/customers/${id}`, {
      headers: getAuthHeader(),
    });
  },
};
