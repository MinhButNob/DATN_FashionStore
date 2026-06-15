import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { orderService } from "../services/orderService";

export const useOrderStore = defineStore("order", () => {
  // State
  const orders = ref([]);
  const currentOrder = ref(null);
  const loading = ref(false);
  const totalElements = ref(0);
  const totalPages = ref(0);
  const currentPage = ref(0);
  const pageSize = ref(10);

  // Getters
  const hasOrders = computed(() => orders.value.length > 0);
  const getOrderStatusText = (status) => {
    const statusMap = {
      PENDING_PAYMENT: "Chờ thanh toán",
      PROCESSING: "Đang xử lý",
      COMPLETED: "Hoàn thành",
      CANCELLED: "Đã hủy",
      RETURNED: "Trả hàng",
    };
    return statusMap[status] || status;
  };

  const getOrderStatusClass = (status) => {
    const classMap = {
      PENDING_PAYMENT: "status-pending",
      PROCESSING: "status-processing",
      COMPLETED: "status-completed",
      CANCELLED: "status-cancelled",
      RETURNED: "status-returned",
    };
    return classMap[status] || "";
  };

  // Actions
  const fetchMyOrders = async (status = null, page = 0, size = 10) => {
    loading.value = true;
    try {
      const response = await orderService.getMyOrders(status, page, size);
      const data = response.data;

      orders.value = data.content || [];
      totalElements.value = data.totalElements || 0;
      totalPages.value = data.totalPages || 0;
      currentPage.value = data.number || 0;
      pageSize.value = data.size || 10;

      return data;
    } catch (error) {
      console.error("Lỗi tải đơn hàng:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const fetchOrderDetail = async (orderId) => {
    loading.value = true;
    try {
      const response = await orderService.getOrderDetail(orderId);
      currentOrder.value = response.data;
      return response.data;
    } catch (error) {
      console.error("Lỗi tải chi tiết đơn hàng:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const createOrder = async (orderData) => {
    loading.value = true;
    try {
      const response = await orderService.createOrder(orderData);
      return response.data;
    } catch (error) {
      console.error("Lỗi tạo đơn hàng:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const cancelOrder = async (orderId, reason) => {
    loading.value = true;
    try {
      const response = await orderService.cancelOrder(orderId, reason);
      // Cập nhật đơn hàng trong danh sách
      const index = orders.value.findIndex((o) => o.orderId === orderId);
      if (index !== -1) {
        orders.value[index] = response.data;
      }
      if (currentOrder.value?.orderId === orderId) {
        currentOrder.value = response.data;
      }
      return response.data;
    } catch (error) {
      console.error("Lỗi hủy đơn hàng:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const clearCurrentOrder = () => {
    currentOrder.value = null;
  };

  return {
    // State
    orders,
    currentOrder,
    loading,
    totalElements,
    totalPages,
    currentPage,
    pageSize,
    // Getters
    hasOrders,
    getOrderStatusText,
    getOrderStatusClass,
    // Actions
    fetchMyOrders,
    fetchOrderDetail,
    createOrder,
    cancelOrder,
    clearCurrentOrder,
  };
});
