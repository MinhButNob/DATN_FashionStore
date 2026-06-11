// src/api/categoryService.js
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

const categoryService = {
  /**
   * Lấy tất cả danh mục (kể cả đã xóa - dành cho admin)
   */
  async getCategories() {
    try {
      const response = await axios.get(`${API_BASE}/categories`);
      return response;
    } catch (error) {
      console.error("Lỗi lấy danh sách danh mục:", error);
      throw error;
    }
  },

  /**
   * Lấy danh sách danh mục đang hoạt động (chỉ ACTIVE và chưa xóa)
   * Dùng cho dropdown sản phẩm
   */
  async getActiveCategories() {
    try {
      const response = await axios.get(`${API_BASE}/categories/active`);
      return response;
    } catch (error) {
      console.error("Lỗi lấy danh mục hoạt động:", error);
      throw error;
    }
  },

  /**
   * Lấy danh mục theo ID
   */
  async getCategoryById(id) {
    try {
      const response = await axios.get(`${API_BASE}/categories/${id}`);
      return response;
    } catch (error) {
      console.error(`Lỗi lấy danh mục ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Tạo danh mục mới
   * @param {Object} data - { categoryName, categoryDescription, status }
   */
  async createCategory(data) {
    try {
      const response = await axios.post(`${API_BASE}/categories`, data);
      return response;
    } catch (error) {
      console.error("Lỗi tạo danh mục:", error);
      throw error;
    }
  },

  /**
   * Cập nhật danh mục
   * @param {Number} id - ID danh mục
   * @param {Object} data - { categoryName, categoryDescription, status }
   */
  async updateCategory(id, data) {
    try {
      const response = await axios.put(`${API_BASE}/categories/${id}`, data);
      return response;
    } catch (error) {
      console.error(`Lỗi cập nhật danh mục ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Xóa mềm danh mục
   * @param {Number} id - ID danh mục
   */
  async deleteCategory(id) {
    try {
      const response = await axios.delete(`${API_BASE}/categories/${id}`);
      return response;
    } catch (error) {
      console.error(`Lỗi xóa danh mục ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Khôi phục danh mục đã xóa
   * @param {Number} id - ID danh mục
   */
  async restoreCategory(id) {
    try {
      const response = await axios.patch(
        `${API_BASE}/categories/${id}/restore`,
      );
      return response;
    } catch (error) {
      console.error(`Lỗi khôi phục danh mục ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Tìm kiếm danh mục theo từ khóa
   * @param {String} keyword - Từ khóa tìm kiếm
   * @param {Number} page - Trang (mặc định 0)
   * @param {Number} size - Số lượng (mặc định 10)
   */
  async searchCategories(keyword, page = 0, size = 10) {
    try {
      const response = await axios.get(`${API_BASE}/categories/search`, {
        params: { keyword, page, size },
      });
      return response;
    } catch (error) {
      console.error("Lỗi tìm kiếm danh mục:", error);
      throw error;
    }
  },

  /**
   * Đếm số lượng danh mục (chưa xóa)
   */
  async countCategories() {
    try {
      const response = await axios.get(`${API_BASE}/categories/count`);
      return response;
    } catch (error) {
      console.error("Lỗi đếm danh mục:", error);
      throw error;
    }
  },
};

export default categoryService;
