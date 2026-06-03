// src/api/colorService.js
import axiosClient from "./axios";

export default {
  // Lấy danh sách màu (phân trang)
  getColors(params = {}) {
    return axiosClient.get("/colors", { params });
  },

  // Lấy tất cả màu (không phân trang) - Dùng cho dropdown
  getAllColors() {
    return axiosClient.get("/colors?page=0&size=1000");
  },

  // Lấy tất cả màu đang hoạt động - Dùng cho dropdown sản phẩm
  getActiveColors() {
    return axiosClient.get("/colors/active");
  },

  // Lấy chi tiết màu theo ID
  getColorById(id) {
    if (!id || id === "undefined") {
      return Promise.reject(new Error("ID không hợp lệ"));
    }
    return axiosClient.get(`/colors/${id}`);
  },

  // Tạo màu mới
  createColor(data) {
    return axiosClient.post("/colors", data);
  },

  // Cập nhật màu
  updateColor(id, data) {
    if (!id || id === "undefined") {
      return Promise.reject(new Error("ID không hợp lệ"));
    }
    return axiosClient.put(`/colors/${id}`, data);
  },

  // Xóa màu
  deleteColor(id) {
    if (!id || id === "undefined") {
      return Promise.reject(new Error("ID không hợp lệ"));
    }
    return axiosClient.delete(`/colors/${id}`);
  },

  // Tìm kiếm màu
  searchColors(keyword, params = {}) {
    return axiosClient.get("/colors/search", {
      params: { keyword, ...params },
    });
  },

  // Đếm số lượng màu
  countColors() {
    return axiosClient.get("/colors/count");
  },
};
