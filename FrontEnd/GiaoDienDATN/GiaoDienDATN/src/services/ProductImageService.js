// services/ProductImageService.js
import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api/product-images";

export default {
  // Lấy danh sách ảnh
  getImages(params) {
    return axios.get(API_BASE_URL, { params });
  },

  // Lấy tổng số trang
  getTotalPages(params) {
    return axios.get(`${API_BASE_URL}/total-pages`, { params });
  },

  // Tạo mới
  createImage(formData) {
    return axios.post(API_BASE_URL, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  // Cập nhật
  updateImage(id, formData) {
    return axios.put(`${API_BASE_URL}/${id}`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  // Xóa
  deleteImage(id) {
    return axios.delete(`${API_BASE_URL}/${id}`);
  },
};
