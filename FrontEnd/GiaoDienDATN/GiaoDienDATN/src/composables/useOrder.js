import { ref } from "vue";
import { useOrderStore } from "../stores/orderStore";
import { useAdminOrderStore } from "../stores/adminOrderStore";
import { useNotification } from "./useNotification";

export const useOrder = () => {
  const orderStore = useOrderStore();
  const adminOrderStore = useAdminOrderStore();
  const { showNotification } = useNotification();

  const isCancelModalOpen = ref(false);
  const cancelReason = ref("");
  const cancelOrderId = ref(null);

  // Format price
  const formatPrice = (price) => {
    if (!price && price !== 0) return "0₫";
    const numPrice = typeof price === "string" ? parseFloat(price) : price;
    if (isNaN(numPrice) || numPrice === 0) return "0₫";
    return numPrice.toLocaleString("vi-VN") + "₫";
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return "---";
    const date = new Date(dateString);
    return (
      date.toLocaleDateString("vi-VN") +
      " " +
      date.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })
    );
  };

  // Get order status text
  const getStatusText = (status) => {
    const map = {
      PENDING_PAYMENT: "Chờ thanh toán",
      PROCESSING: "Đang xử lý",
      COMPLETED: "Hoàn thành",
      CANCELLED: "Đã hủy",
      RETURNED: "Trả hàng",
    };
    return map[status] || status;
  };

  // Get order status class
  const getStatusClass = (status) => {
    const map = {
      PENDING_PAYMENT: "pending",
      PROCESSING: "processing",
      COMPLETED: "completed",
      CANCELLED: "cancelled",
      RETURNED: "returned",
    };
    return map[status] || "";
  };

  // Get payment status text
  const getPaymentStatusText = (status) => {
    const map = {
      UNPAID: "Chưa thanh toán",
      PAID: "Đã thanh toán",
      PARTIAL: "Thanh toán một phần",
      REFUNDED: "Đã hoàn tiền",
    };
    return map[status] || status || "Chưa thanh toán";
  };

  // Open cancel modal
  const openCancelModal = (orderId) => {
    cancelOrderId.value = orderId;
    cancelReason.value = "";
    isCancelModalOpen.value = true;
  };

  // Close cancel modal
  const closeCancelModal = () => {
    isCancelModalOpen.value = false;
    cancelOrderId.value = null;
    cancelReason.value = "";
  };

  // Confirm cancel order
  const confirmCancelOrder = async () => {
    if (!cancelReason.value.trim()) {
      showNotification("Vui lòng nhập lý do hủy đơn hàng!", "error");
      return;
    }

    try {
      await orderStore.cancelOrder(cancelOrderId.value, cancelReason.value);
      showNotification("Hủy đơn hàng thành công!", "success");
      closeCancelModal();
    } catch (error) {
      showNotification(
        error.response?.data?.message || "Không thể hủy đơn hàng!",
        "error",
      );
    }
  };

  return {
    // State
    isCancelModalOpen,
    cancelReason,
    cancelOrderId,
    // Methods
    formatPrice,
    formatDate,
    getStatusText,
    getStatusClass,
    getPaymentStatusText,
    openCancelModal,
    closeCancelModal,
    confirmCancelOrder,
  };
};
