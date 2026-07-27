// src/stores/saleStaffStore.js
import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { saleStaffService } from "../services/saleStaffService";

export const useSaleStaffStore = defineStore("saleStaff", () => {
  // ===== STATE =====
  const orders = ref([]);
  const customers = ref([]);
  const returnRequests = ref([]);
  const loading = ref(false);
  const statusCounts = ref({});
  const statistics = ref({
    totalOrders: 0,
    pendingOrders: 0,
    completedOrders: 0,
    cancelledOrders: 0,
    todayRevenue: 0,
    monthlyRevenue: 0 });

  const pagination = ref({
    currentPage: 0,
    totalPages: 0,
    totalElements: 0,
    pageSize: 10 });

  const filters = ref({
    keyword: "",
    status: "",
    customerStatus: "" });

  // ===== GETTERS =====
  const filteredOrders = computed(() => orders.value);
  const filteredCustomers = computed(() => customers.value);

  // ===== ACTIONS =====

  // Orders
  const fetchOrders = async (page = 0, size = 10) => {
    loading.value = true;
    try {
      const response = await saleStaffService.getAllOrders({
        page,
        size,
        keyword: filters.value.keyword,
        status: filters.value.status });

      const data = response.data?.data || response.data;
      orders.value = data?.content || [];
      pagination.value = {
        currentPage: data?.number || 0,
        totalPages: data?.totalPages || 0,
        totalElements: data?.totalElements || 0,
        pageSize: data?.size || 10 };
      return data;
    } catch (error) {
      console.error("Error fetching orders:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const getOrderDetail = async (orderId) => {
    try {
      const response = await saleStaffService.getOrderDetail(orderId);
      return response.data?.data || response.data;
    } catch (error) {
      console.error("Error fetching order detail:", error);
      throw error;
    }
  };

  const updateOrderStatus = async (orderId, data) => {
    loading.value = true;
    try {
      const response = await saleStaffService.updateOrderStatus(orderId, data);
      const result = response.data?.data || response.data;
      await fetchOrders(
        pagination.value.currentPage,
        pagination.value.pageSize,
      );
      return result;
    } catch (error) {
      console.error("Error updating order status:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const cancelOrder = async (orderId, reason) => {
    loading.value = true;
    try {
      const response = await saleStaffService.cancelOrder(orderId, reason);
      const result = response.data?.data || response.data;
      await fetchOrders(
        pagination.value.currentPage,
        pagination.value.pageSize,
      );
      return result;
    } catch (error) {
      console.error("Error cancelling order:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const getStatusCounts = async () => {
    try {
      const response = await saleStaffService.getStatusCounts();
      statusCounts.value = response.data?.data || response.data || {};
      return statusCounts.value;
    } catch (error) {
      console.error("Error fetching status counts:", error);
      return {};
    }
  };

  const getInvoice = async (orderId) => {
    try {
      const response = await saleStaffService.getInvoice(orderId);
      return response.data?.data || response.data;
    } catch (error) {
      console.error("Error fetching invoice:", error);
      throw error;
    }
  };

  // Return Requests
  const fetchReturnRequests = async (page = 0, size = 10) => {
    loading.value = true;
    try {
      const response = await saleStaffService.getAllReturnRequests({
        page,
        size });
      const data = response.data?.data || response.data;
      returnRequests.value = data?.content || [];
      return data;
    } catch (error) {
      console.error("Error fetching return requests:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const getReturnRequestByOrderId = async (orderId) => {
    try {
      const response =
        await saleStaffService.getReturnRequestByOrderId(orderId);
      return response.data?.data || response.data;
    } catch (error) {
      console.error("Error fetching return request:", error);
      throw error;
    }
  };

  const processReturnRequest = async (orderId, data) => {
    loading.value = true;
    try {
      const response = await saleStaffService.processReturnRequest(
        orderId,
        data,
      );
      const result = response.data?.data || response.data;
      await fetchReturnRequests();
      return result;
    } catch (error) {
      console.error("Error processing return request:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const receiveReturnedItems = async (orderId, note) => {
    loading.value = true;
    try {
      const response = await saleStaffService.receiveReturnedItems(
        orderId,
        note,
      );
      const result = response.data?.data || response.data;
      await fetchReturnRequests();
      return result;
    } catch (error) {
      console.error("Error receiving returned items:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // Stock Management
  const updateStock = async (productDetailId, quantity) => {
    loading.value = true;
    try {
      const response = await saleStaffService.updateStock(
        productDetailId,
        quantity,
      );
      return response.data?.data || response.data;
    } catch (error) {
      console.error("Error updating stock:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const increaseStock = async (productDetailId, quantity) => {
    loading.value = true;
    try {
      const response = await saleStaffService.increaseStock(
        productDetailId,
        quantity,
      );
      return response.data?.data || response.data;
    } catch (error) {
      console.error("Error increasing stock:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const decreaseStock = async (productDetailId, quantity) => {
    loading.value = true;
    try {
      const response = await saleStaffService.decreaseStock(
        productDetailId,
        quantity,
      );
      return response.data?.data || response.data;
    } catch (error) {
      console.error("Error decreasing stock:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // Customers
  const fetchCustomers = async (page = 0, size = 10) => {
    loading.value = true;
    try {
      const response = await saleStaffService.getCustomers({
        page,
        size,
        keyword: filters.value.keyword,
        status: filters.value.customerStatus });

      const data = response.data?.data || response.data;
      customers.value = data?.content || [];
      pagination.value = {
        currentPage: data?.number || 0,
        totalPages: data?.totalPages || 0,
        totalElements: data?.totalElements || 0,
        pageSize: data?.size || 10 };
      return data;
    } catch (error) {
      console.error("Error fetching customers:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const getCustomerById = async (id) => {
    try {
      const response = await saleStaffService.getCustomerById(id);
      return response.data?.data || response.data;
    } catch (error) {
      console.error("Error fetching customer:", error);
      throw error;
    }
  };

  // ===== RESET =====
  const resetFilters = () => {
    filters.value = {
      keyword: "",
      status: "",
      customerStatus: "" };
  };

  return {
    // State
    orders,
    customers,
    returnRequests,
    loading,
    statusCounts,
    statistics,
    pagination,
    filters,

    // Getters
    filteredOrders,
    filteredCustomers,

    // Actions
    fetchOrders,
    getOrderDetail,
    updateOrderStatus,
    cancelOrder,
    getStatusCounts,
    getInvoice,
    fetchReturnRequests,
    getReturnRequestByOrderId,
    processReturnRequest,
    receiveReturnedItems,
    updateStock,
    increaseStock,
    decreaseStock,
    fetchCustomers,
    getCustomerById,
    resetFilters };
});
