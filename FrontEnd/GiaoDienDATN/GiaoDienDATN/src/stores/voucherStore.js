import { defineStore } from "pinia";
import { ref } from "vue";
import {
  getVouchers,
  getDeletedVouchers,
  createVoucher,
  updateVoucher,
  softDeleteVoucher,
  restoreVoucher,
  toggleVoucherStatus,
  getVoucherStatistics } from "../services/api";

export const useVoucherStore = defineStore("voucher", () => {
  const vouchers = ref([]);
  const deletedVouchers = ref([]);
  const totalElements = ref(0);
  const totalPages = ref(0);
  const currentPage = ref(0);
  const pageSize = ref(10);
  const loading = ref(false);
  const showDeleted = ref(false);
  const statistics = ref({
    totalVouchers: 0,
    activeVouchers: 0,
    inactiveVouchers: 0,
    totalUsedCount: 0 });

  const fetchVouchers = async (
    page = 0,
    size = 10,
    status = "",
    keyword = "",
  ) => {
    loading.value = true;
    try {
      const res = await getVouchers(page, size, status, keyword);
      vouchers.value = res.content || [];
      totalElements.value = res.totalElements || 0;
      totalPages.value = res.totalPages || 0;
      currentPage.value = res.number || 0;
      pageSize.value = res.size || 10;
      return res;
    } catch (error) {
      console.error("Lỗi khi lấy danh sách voucher:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const fetchDeletedVouchers = async (page = 0, size = 10) => {
    loading.value = true;
    try {
      const res = await getDeletedVouchers(page, size);
      deletedVouchers.value = res.content || [];
      totalElements.value = res.totalElements || 0;
      totalPages.value = res.totalPages || 0;
      currentPage.value = res.number || 0;
      pageSize.value = res.size || 10;
      return res;
    } catch (error) {
      console.error("Lỗi khi lấy danh sách voucher đã xóa:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const fetchStatistics = async () => {
    try {
      const res = await getVoucherStatistics();
      statistics.value = res;
      return res;
    } catch (error) {
      console.error("Lỗi khi lấy thống kê voucher:", error);
      throw error;
    }
  };

  const addVoucher = async (data) => {
    try {
      const res = await createVoucher(data);
      await fetchVouchers(currentPage.value, pageSize.value);
      await fetchStatistics();
      return res;
    } catch (error) {
      console.error("Lỗi khi thêm voucher:", error);
      throw error;
    }
  };

  const editVoucher = async (id, data) => {
    try {
      const res = await updateVoucher(id, data);
      await fetchVouchers(currentPage.value, pageSize.value);
      return res;
    } catch (error) {
      console.error("Lỗi khi cập nhật voucher:", error);
      throw error;
    }
  };

  const removeVoucher = async (id) => {
    try {
      await softDeleteVoucher(id);
      if (showDeleted.value) {
        await fetchDeletedVouchers(currentPage.value, pageSize.value);
      } else {
        await fetchVouchers(currentPage.value, pageSize.value);
      }
      await fetchStatistics();
    } catch (error) {
      console.error("Lỗi khi xóa voucher:", error);
      throw error;
    }
  };

  const restoreVoucherItem = async (id) => {
    try {
      await restoreVoucher(id);
      if (showDeleted.value) {
        await fetchDeletedVouchers(currentPage.value, pageSize.value);
      } else {
        await fetchVouchers(currentPage.value, pageSize.value);
      }
      await fetchStatistics();
    } catch (error) {
      console.error("Lỗi khi khôi phục voucher:", error);
      throw error;
    }
  };

  const toggleStatus = async (id) => {
    try {
      await toggleVoucherStatus(id);
      await fetchVouchers(currentPage.value, pageSize.value);
      await fetchStatistics();
    } catch (error) {
      console.error("Lỗi khi đổi trạng thái voucher:", error);
      throw error;
    }
  };

  const toggleShowDeleted = (value) => {
    showDeleted.value = value;
  };

  return {
    vouchers,
    deletedVouchers,
    totalElements,
    totalPages,
    currentPage,
    pageSize,
    loading,
    showDeleted,
    statistics,
    fetchVouchers,
    fetchDeletedVouchers,
    fetchStatistics,
    addVoucher,
    editVoucher,
    removeVoucher,
    restoreVoucherItem,
    toggleStatus,
    toggleShowDeleted };
});
