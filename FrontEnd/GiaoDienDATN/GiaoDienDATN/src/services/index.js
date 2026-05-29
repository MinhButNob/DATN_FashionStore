import api from "./api";

export const authService = {
  login(email, password) {
    return api.post("/auth/login", { email, password });
  },

  register(userData) {
    return api.post("/auth/register", userData);
  },

  getCurrentUser() {
    return api.get("/user/me");
  },

  updateProfile(data) {
    return api.put("/auth/profile", data);
  },
};

export const productService = {
  getAllProducts(params = {}) {
    return api.get("/products", { params });
  },

  getProductById(id) {
    return api.get(`/products/${id}`);
  },
};
