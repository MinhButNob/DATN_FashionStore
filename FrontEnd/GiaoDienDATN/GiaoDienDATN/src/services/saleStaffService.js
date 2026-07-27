// src/services/saleStaffService.js
import api from "@/api/axios";

export const saleStaffService = {
  // ===== ORDERS =====
  getAllOrders(params = {}) {
    const queryParams = {
      page: params.page || 0,
      size: params.size || 10,
      keyword: params.keyword || "",
      status: params.status || "" };
    return api.get("/sale-staff/orders", {
      params: queryParams });
  },

  getOrderDetail(orderId) {
    return api.get(`/sale-staff/orders/${orderId}`);
  },

  updateOrderStatus(orderId, data) {
    return api.put(`/sale-staff/orders/${orderId}/status`, data);
  },

  updatePaymentStatus(orderId, paymentStatus) {
    return api.put(
      `/sale-staff/orders/${orderId}/payment-status`,
      null,
      { params: { paymentStatus } },
    );
  },

  cancelOrder(orderId, reason) {
    return api.put(`/sale-staff/orders/${orderId}/cancel`, { reason });
  },

  getStatistics() {
    return api.get("/sale-staff/orders/statistics");
  },

  getStatusCounts() {
    return api.get("/sale-staff/orders/status/counts");
  },

  getInvoice(orderId) {
    return api.get(`/sale-staff/orders/${orderId}/invoice`);
  },

  // ===== RETURN REQUESTS =====
  getReturnRequestByOrderId(orderId) {
    return api.get(`/sale-staff/orders/return-requests/order/${orderId}`);
  },

  processReturnRequest(orderId, data) {
    return api.put(
      `/sale-staff/orders/return-requests/${orderId}/process`,
      data,
    );
  },

  receiveReturnedItems(orderId, note) {
    return api.put(
      `/sale-staff/orders/return-requests/${orderId}/receive`,
      { note },
    );
  },

  getAllReturnRequests(params = {}) {
    return api.get("/sale-staff/orders/return-requests", {
      params: {
        page: params.page || 0,
        size: params.size || 10,
        status: params.status || "" } });
  },

  // ===== PRODUCTS (STOCK ONLY) =====
  updateStock(productDetailId, quantity) {
    return api.put(
      `/sale-staff/products/stock/${productDetailId}`,
      { quantity },
    );
  },

  increaseStock(productDetailId, quantity) {
    return api.post(
      `/sale-staff/products/stock/${productDetailId}/increase`,
      { quantity },
    );
  },

  decreaseStock(productDetailId, quantity) {
    return api.post(
      `/sale-staff/products/stock/${productDetailId}/decrease`,
      { quantity },
    );
  },

  getProductDetail(productDetailId) {
    return api.get(`/sale-staff/products/details/${productDetailId}`);
  },

  // ===== CUSTOMERS =====
  getCustomers(params = {}) {
    return api.get("/sale-staff/customers", {
      params: {
        page: params.page || 0,
        size: params.size || 10,
        keyword: params.keyword || "",
        status: params.status || "" } });
  },

  getCustomerById(id) {
    return api.get(`/sale-staff/customers/${id}`);
  } };