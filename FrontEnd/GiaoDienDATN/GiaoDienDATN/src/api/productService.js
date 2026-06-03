// src/api/productService.js
import api from "./axios";
import categoryService from "./categoryService";
import colorService from "./colorService";
import sizeService from "./sizeService";

export default {
  // Products
  getProducts(params) {
    return api.get("/products", { params });
  },
  getProductById(id) {
    return api.get(`/products/${id}`);
  },
  createProduct(data) {
    return api.post("/products", data);
  },
  updateProduct(id, data) {
    return api.put(`/products/${id}`, data);
  },
  deleteProduct(id) {
    return api.delete(`/products/${id}`);
  },
  toggleProductStatus(id) {
    return api.patch(`/products/${id}/toggle-status`);
  },
  getStatistics() {
    return api.get("/products/statistics");
  },

  // Categories
  getCategories: categoryService.getAll,

  // Colors
  getColors: colorService.getAll,

  // Sizes
  getSizes: sizeService.getAll,
};
