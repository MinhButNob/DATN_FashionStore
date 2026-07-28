import { defineStore } from "pinia";
import { ref } from "vue";
import {
  getPromotions,
  getDeletedPromotions,
  createPromotion,
  updatePromotion,
  softDeletePromotion,
  restorePromotion,
  togglePromotionStatus,
  getPromotionStatistics } from "../services/api";
import api from "../api/axios";

export const usePromotionStore = defineStore("promotion", () => {
  // ==================== STATE HIỆN TẠI (GIỮ NGUYÊN) ====================
  const promotions = ref([]);
  const deletedPromotions = ref([]);
  const totalElements = ref(0);
  const totalPages = ref(0);
  const currentPage = ref(0);
  const pageSize = ref(10);
  const loading = ref(false);
  const showDeleted = ref(false);
  const statistics = ref({
    totalPromotions: 0,
    activePromotions: 0,
    inactivePromotions: 0 });

  // ==================== CUSTOMER STATE (THÊM MỚI) ====================
  const activePromotions = ref([]);
  const promotionLoading = ref(false);

  // ==================== ADMIN ACTIONS (GIỮ NGUYÊN) ====================
  const fetchPromotions = async (
    page = 0,
    size = 10,
    status = "",
    keyword = "",
  ) => {
    loading.value = true;
    try {
      const res = await getPromotions(page, size, status, keyword);
      promotions.value = res.content || [];
      totalElements.value = res.totalElements || 0;
      totalPages.value = res.totalPages || 0;
      currentPage.value = res.number || 0;
      pageSize.value = res.size || 10;
      return res;
    } catch (error) {
      console.error("Lỗi khi lấy danh sách:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const fetchDeletedPromotions = async (page = 0, size = 10) => {
    loading.value = true;
    try {
      const res = await getDeletedPromotions(page, size);
      deletedPromotions.value = res.content || [];
      totalElements.value = res.totalElements || 0;
      totalPages.value = res.totalPages || 0;
      currentPage.value = res.number || 0;
      pageSize.value = res.size || 10;
      return res;
    } catch (error) {
      console.error("Lỗi khi lấy danh sách đã xóa:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const fetchStatistics = async () => {
    try {
      const res = await getPromotionStatistics();
      statistics.value = res;
      return res;
    } catch (error) {
      console.error("Lỗi khi lấy thống kê:", error);
      throw error;
    }
  };

  const addPromotion = async (data) => {
    try {
      const res = await createPromotion(data);
      await fetchPromotions(currentPage.value, pageSize.value);
      await fetchStatistics();
      return res;
    } catch (error) {
      console.error("Lỗi khi thêm:", error);
      throw error;
    }
  };

  const editPromotion = async (id, data) => {
    try {
      const res = await updatePromotion(id, data);
      await fetchPromotions(currentPage.value, pageSize.value);
      return res;
    } catch (error) {
      console.error("Lỗi khi cập nhật:", error);
      throw error;
    }
  };

  const removePromotion = async (id) => {
    try {
      await softDeletePromotion(id);
      if (showDeleted.value) {
        await fetchDeletedPromotions(currentPage.value, pageSize.value);
      } else {
        await fetchPromotions(currentPage.value, pageSize.value);
      }
      await fetchStatistics();
    } catch (error) {
      console.error("Lỗi khi xóa:", error);
      throw error;
    }
  };

  const restorePromotionItem = async (id) => {
    try {
      await restorePromotion(id);
      if (showDeleted.value) {
        await fetchDeletedPromotions(currentPage.value, pageSize.value);
      } else {
        await fetchPromotions(currentPage.value, pageSize.value);
      }
      await fetchStatistics();
    } catch (error) {
      console.error("Lỗi khi khôi phục:", error);
      throw error;
    }
  };

  const toggleStatus = async (id) => {
    try {
      await togglePromotionStatus(id);
      await fetchPromotions(currentPage.value, pageSize.value);
      await fetchStatistics();
    } catch (error) {
      console.error("Lỗi khi đổi trạng thái:", error);
      throw error;
    }
  };

  const toggleShowDeleted = (value) => {
    showDeleted.value = value;
  };

  // ==================== CUSTOMER ACTIONS (THÊM MỚI) ====================

  const fetchActivePromotions = async () => {
    promotionLoading.value = true;
    try {
      const response = await api.get("/promotions/active");
      let promotionsData = [];
      if (response.data?.data) {
        promotionsData = response.data.data;
      } else if (Array.isArray(response.data)) {
        promotionsData = response.data;
      }
      activePromotions.value = promotionsData.filter(
        (p) => p.promotionStatus === "ACTIVE" && !p.promotionDeletedAt,
      );
      return activePromotions.value;
    } catch (error) {
      console.error("Lỗi khi lấy danh sách khuyến mãi đang hoạt động:", error);
      activePromotions.value = [];
      return [];
    } finally {
      promotionLoading.value = false;
    }
  };

  const fetchPromotionsByProduct = async (productId) => {
    promotionLoading.value = true;
    try {
      const response = await api.get(`/products/${productId}/promotions`);
      let promotionsData = [];
      if (response.data?.data) {
        promotionsData = response.data.data;
      } else if (Array.isArray(response.data)) {
        promotionsData = response.data;
      }
      return promotionsData;
    } catch (error) {
      console.error(
        `Lỗi khi lấy khuyến mãi theo sản phẩm ${productId}:`,
        error,
      );
      return [];
    } finally {
      promotionLoading.value = false;
    }
  };

  const fetchPromotionByProductDetail = async (productDetailId) => {
    try {
      const response = await api.get(
        `/product-details/${productDetailId}/promotion`,
      );
      return response.data?.data || response.data || null;
    } catch (error) {
      console.error(
        `Lỗi khi lấy khuyến mãi theo biến thể ${productDetailId}:`,
        error,
      );
      return null;
    }
  };

  // ==================== EXPORTS ====================
  return {
    // Admin (cũ - giữ nguyên)
    promotions,
    deletedPromotions,
    totalElements,
    totalPages,
    currentPage,
    pageSize,
    loading,
    showDeleted,
    statistics,
    fetchPromotions,
    fetchDeletedPromotions,
    fetchStatistics,
    addPromotion,
    editPromotion,
    removePromotion,
    restorePromotionItem,
    toggleStatus,
    toggleShowDeleted,
    // Customer (mới)
    activePromotions,
    promotionLoading,
    fetchActivePromotions,
    fetchPromotionsByProduct,
    fetchPromotionByProductDetail };
});