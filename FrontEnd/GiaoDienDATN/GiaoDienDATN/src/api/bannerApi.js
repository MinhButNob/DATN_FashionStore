import api from "./axios";

// ========== API METHODS ==========
// Interceptor (token, FormData, refresh-token, redirect /login khi 401/403)
// đã được xử lý tập trung trong ./axios.js, không cần khai báo lại ở đây.
export const bannerApiService = {
  // ========== CLIENT ==========
  async getActiveBanners() {
    try {
      const response = await api.get("/banners/active");
      return response.data;
    } catch (error) {
      console.error("❌ Failed to fetch active banners:", error);
      throw error;
    }
  },

  // ========== ADMIN ==========
  async getBanners(params = {}) {
    try {
      const response = await api.get("/admin/banners", { params });
      return response.data;
    } catch (error) {
      console.error("❌ Failed to fetch banners:", error);
      throw error;
    }
  },

  async createBanner(formData) {
    try {
      const response = await api.post("/admin/banners", formData);
      return response.data;
    } catch (error) {
      console.error("❌ Failed to create banner:", error);
      throw error;
    }
  },

  async updateBanner(id, formData) {
    try {
      const response = await api.put(`/admin/banners/${id}`, formData);
      return response.data;
    } catch (error) {
      console.error(`❌ Failed to update banner ${id}:`, error);
      throw error;
    }
  },

  async deleteBanner(id) {
    try {
      const response = await api.delete(`/admin/banners/${id}`);
      return response.data;
    } catch (error) {
      console.error(`❌ Failed to delete banner ${id}:`, error);
      throw error;
    }
  },

  async restoreBanner(id) {
    try {
      const response = await api.patch(`/admin/banners/${id}/restore`);
      return response.data;
    } catch (error) {
      console.error(`❌ Failed to restore banner ${id}:`, error);
      throw error;
    }
  } };

export default api;