import { bannerApiService } from '../api/bannerApi';

const bannerService = {
  // ========== CLIENT ==========
  async getActiveBanners() {
    try {
      const data = await bannerApiService.getActiveBanners();
      return { data };
    } catch (error) {
      console.error('Lỗi lấy banner active:', error);
      throw error;
    }
  },

  // ========== ADMIN ==========
  async getBanners(params = {}) {
    try {
      const data = await bannerApiService.getBanners(params);
      return { data };
    } catch (error) {
      console.error('Lỗi lấy danh sách banner:', error);
      throw error;
    }
  },

  async createBanner(formData) {
    try {
      const data = await bannerApiService.createBanner(formData);
      return { data };
    } catch (error) {
      console.error('Lỗi tạo banner:', error);
      throw error;
    }
  },

  async updateBanner(id, formData) {
    try {
      const data = await bannerApiService.updateBanner(id, formData);
      return { data };
    } catch (error) {
      console.error(`Lỗi cập nhật banner ID ${id}:`, error);
      throw error;
    }
  },

  async deleteBanner(id) {
    try {
      const data = await bannerApiService.deleteBanner(id);
      return { data };
    } catch (error) {
      console.error(`Lỗi xóa banner ID ${id}:`, error);
      throw error;
    }
  },

  async restoreBanner(id) {
    try {
      const data = await bannerApiService.restoreBanner(id);
      return { data };
    } catch (error) {
      console.error(`Lỗi khôi phục banner ID ${id}:`, error);
      throw error;
    }
  },
};

export default bannerService;