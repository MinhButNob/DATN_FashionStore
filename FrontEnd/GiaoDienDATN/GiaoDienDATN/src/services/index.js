import api from "./api";
import authApi from "../api/authApi";

export { authApi };

export const productService = {
  getAllProducts(params = {}) {
    return api.get("/products", { params });
  },

  getProductById(id) {
    return api.get(`/products/${id}`);
  },
};
