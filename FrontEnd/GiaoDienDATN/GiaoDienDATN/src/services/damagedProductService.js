// src/services/damagedProductService.js
import api from "@/api/axios";

const damagedProductService = {
  /**
   * Lấy danh sách tất cả sản phẩm hỏng
   */
  getAllDamagedProducts() {
    return api.get("/damaged-products");
  },

  /**
   * Lấy danh sách sản phẩm hỏng theo trạng thái
   */
  getDamagedProductsByStatus(status) {
    return api.get(`/damaged-products/status/${status}`);
  },

  /**
   * Lấy danh sách sản phẩm hỏng đang chờ xử lý
   */
  getPendingDamagedProducts() {
    return api.get("/damaged-products/pending");
  },

  /**
   * Lấy chi tiết sản phẩm hỏng theo ID
   */
  getDamagedProductById(id) {
    return api.get(`/damaged-products/${id}`);
  },

  /**
   * Cập nhật trạng thái sản phẩm hỏng
   * @param {number} id - ID sản phẩm hỏng
   * @param {string} status - Trạng thái mới: PROCESSED, DISCARDED, REPAIRED
   * @param {string} notes - Ghi chú xử lý
   */
  updateDamagedProductStatus(id, status, notes = "") {
    return api.put(`/damaged-products/${id}/status`, {
      status,
      notes,
    });
  },

  /**
   * Xử lý hàng loạt sản phẩm hỏng
   * @param {number[]} damagedProductIds - Danh sách ID sản phẩm hỏng
   * @param {string} status - Trạng thái mới
   * @param {string} notes - Ghi chú xử lý
   */
  batchUpdateDamagedProducts(damagedProductIds, status, notes = "") {
    return api.put("/damaged-products/batch/status", {
      damagedProductIds,
      status,
      notes,
    });
  },

  /**
   * Lấy thống kê sản phẩm hỏng
   */
  getDamagedProductStats() {
    return api.get("/damaged-products/stats");
  },
};

export default damagedProductService;
