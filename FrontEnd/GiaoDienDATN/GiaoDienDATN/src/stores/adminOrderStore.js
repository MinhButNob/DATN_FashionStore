import { defineStore } from "pinia";
import { ref } from "vue";
import { adminOrderService } from "../services/orderService";

export const useAdminOrderStore = defineStore("adminOrder", () => {
  // State
  const orders = ref([]);
  const currentOrder = ref(null);
  const loading = ref(false);
  const totalElements = ref(0);
  const totalPages = ref(0);
  const currentPage = ref(0);
  const pageSize = ref(10);
  const statistics = ref({
    totalOrders: 0,
    completedOrders: 0,
    pendingOrders: 0,
    cancelledOrders: 0,
    todayRevenue: 0,
    monthlyRevenue: 0,
  });
  const statusCounts = ref({
    PENDING_PAYMENT: 0,
    PROCESSING: 0,
    COMPLETED: 0,
    CANCELLED: 0,
    RETURNED: 0,
  });

  // Actions
  const fetchOrders = async (
    status = null,
    fromDate = null,
    toDate = null,
    keyword = null,
    page = 0,
    size = 10,
  ) => {
    loading.value = true;
    try {
      const response = await adminOrderService.getAllOrders(
        status,
        fromDate,
        toDate,
        keyword,
        page,
        size,
      );
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
      const response = await adminOrderService.getOrderDetail(orderId);
      currentOrder.value = response.data;
      return response.data;
    } catch (error) {
      console.error("Lỗi tải chi tiết đơn hàng:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const updateOrderStatus = async (orderId, data) => {
    loading.value = true;
    try {
      const response = await adminOrderService.updateOrderStatus(orderId, data);

      // Cập nhật trong danh sách
      const index = orders.value.findIndex((o) => o.orderId === orderId);
      if (index !== -1) {
        orders.value[index] = response.data;
      }
      if (currentOrder.value?.orderId === orderId) {
        currentOrder.value = response.data;
      }

      await fetchStatistics();
      await fetchStatusCounts();

      return response.data;
    } catch (error) {
      console.error("Lỗi cập nhật trạng thái:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const updatePaymentStatus = async (orderId, paymentStatus) => {
    loading.value = true;
    try {
      const response = await adminOrderService.updatePaymentStatus(
        orderId,
        paymentStatus,
      );

      const index = orders.value.findIndex((o) => o.orderId === orderId);
      if (index !== -1) {
        orders.value[index] = response.data;
      }
      if (currentOrder.value?.orderId === orderId) {
        currentOrder.value = response.data;
      }

      return response.data;
    } catch (error) {
      console.error("Lỗi cập nhật trạng thái thanh toán:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const fetchStatistics = async () => {
    try {
      const response = await adminOrderService.getStatistics();
      statistics.value = response.data;
      return response.data;
    } catch (error) {
      console.error("Lỗi tải thống kê:", error);
      throw error;
    }
  };

  const fetchStatusCounts = async () => {
    try {
      const response = await adminOrderService.getStatusCounts();
      statusCounts.value = response.data;
      return response.data;
    } catch (error) {
      console.error("Lỗi tải thống kê trạng thái:", error);
      throw error;
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
    statistics,
    statusCounts,
    // Actions
    fetchOrders,
    fetchOrderDetail,
    updateOrderStatus,
    updatePaymentStatus,
    fetchStatistics,
    fetchStatusCounts,
    clearCurrentOrder,
  };
});
