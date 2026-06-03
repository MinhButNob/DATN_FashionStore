// src/api/categoryService.js
import axiosClient from "./axios";

export default {
  // Lấy danh sách danh mục
  getCategories(params = {}) {
    return axiosClient.get("/categories", { params });
  },

  // Lấy chi tiết danh mục
  getCategoryById(id) {
    return axiosClient.get(`/categories/${id}`);
  },

  // Tạo danh mục mới
  createCategory(data) {
    return axiosClient.post("/categories", data);
  },

  // Cập nhật danh mục
  updateCategory(id, data) {
    return axiosClient.put(`/categories/${id}`, data);
  },

  // Xóa danh mục
  deleteCategory(id) {
    return axiosClient.delete(`/categories/${id}`);
  },

  // Lấy tất cả danh mục (không phân trang)
  getAllCategories() {
    return axiosClient.get("/categories/all");
  },
};
