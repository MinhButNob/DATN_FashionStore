// src/api/productImageService.js
import axiosClient from "./axios";

export default {
  // Lấy danh sách ảnh (có lọc theo productDetailId)
  getImages(productDetailId = null, page = 0, size = 100) {
    const params = { page, size };
    if (productDetailId) {
      params.productDetailId = productDetailId;
    }
    return axiosClient.get("/product-images", { params });
  },

  // Tạo ảnh mới
  createImage(formData) {
    return axiosClient.post("/product-images", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  // Cập nhật ảnh
  updateImage(id, data) {
    if (data instanceof FormData) {
      return axiosClient.put(`/product-images/${id}`, data, {
        headers: { "Content-Type": "multipart/form-data" },
      });
    }
    return axiosClient.put(`/product-images/${id}`, data);
  },

  // Đặt ảnh chính
  setMainImage(id) {
    const formData = new FormData();
    formData.append("isMain", true);
    return axiosClient.put(`/product-images/${id}`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  // Xóa ảnh
  deleteImage(id) {
    return axiosClient.delete(`/product-images/${id}`);
  },
};
