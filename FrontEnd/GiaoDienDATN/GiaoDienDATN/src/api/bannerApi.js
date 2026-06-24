import axios from 'axios';

// ========== CONFIGURATION ==========
const API_BASE = 'http://localhost:8080/api';

// ========== CREATE AXIOS INSTANCE ==========
const bannerApi = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ========== REQUEST INTERCEPTOR ==========
bannerApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token') ||
                  sessionStorage.getItem('token');

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // ✅ Nếu là FormData, xóa Content-Type để browser tự set
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
      console.log('📤 Sending FormData request to:', config.url);
    }

    if (import.meta.env.DEV && config.url?.includes('/banners')) {
      console.log('📤 API Request:', {
        url: config.url,
        method: config.method?.toUpperCase(),
        hasToken: !!token,
        data: config.data instanceof FormData ? 'FormData' : config.data,
      });
    }

    return config;
  },
  (error) => {
    console.error('❌ Request Error:', error);
    return Promise.reject(error);
  }
);

// ========== RESPONSE INTERCEPTOR ==========
bannerApi.interceptors.response.use(
  (response) => {
    if (import.meta.env.DEV && response.config.url?.includes('/banners')) {
      console.log('✅ API Response:', {
        url: response.config.url,
        status: response.status,
        data: response.data,
      });
    }
    return response;
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response;

      if (status === 401 || status === 403) {
        console.warn(`🔒 ${status} - Unauthorized/Forbidden`);
        localStorage.removeItem('token');
        sessionStorage.removeItem('token');
        localStorage.removeItem('user');

        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login';
        }
      }

      console.error('❌ API Error:', {
        url: error.config?.url,
        status: status,
        data: data,
        message: error.message,
      });
    }

    return Promise.reject(error);
  }
);

// ========== API METHODS ==========
export const bannerApiService = {
  // ========== CLIENT ==========
  async getActiveBanners() {
    try {
      const response = await bannerApi.get('/banners/active');
      return response.data;
    } catch (error) {
      console.error('❌ Failed to fetch active banners:', error);
      throw error;
    }
  },

  // ========== ADMIN ==========
  async getBanners(params = {}) {
    try {
      const response = await bannerApi.get('/admin/banners', { params });
      return response.data;
    } catch (error) {
      console.error('❌ Failed to fetch banners:', error);
      throw error;
    }
  },

  async createBanner(formData) {
    try {
      // ✅ Log FormData để debug
      console.log('📤 Creating banner with FormData:');
      for (let [key, value] of formData.entries()) {
        console.log(`  ${key}:`, value instanceof File ? `File: ${value.name} (${value.size} bytes)` : value);
      }
      const response = await bannerApi.post('/admin/banners', formData);
      console.log('✅ Create banner response:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Failed to create banner:', error);
      throw error;
    }
  },

  async updateBanner(id, formData) {
    try {
      console.log(`📤 Updating banner ${id} with FormData:`);
      for (let [key, value] of formData.entries()) {
        console.log(`  ${key}:`, value instanceof File ? `File: ${value.name} (${value.size} bytes)` : value);
      }
      const response = await bannerApi.put(`/admin/banners/${id}`, formData);
      console.log('✅ Update banner response:', response.data);
      return response.data;
    } catch (error) {
      console.error(`❌ Failed to update banner ${id}:`, error);
      throw error;
    }
  },

  async deleteBanner(id) {
    try {
      const response = await bannerApi.delete(`/admin/banners/${id}`);
      return response.data;
    } catch (error) {
      console.error(`❌ Failed to delete banner ${id}:`, error);
      throw error;
    }
  },

  async restoreBanner(id) {
    try {
      const response = await bannerApi.patch(`/admin/banners/${id}/restore`);
      return response.data;
    } catch (error) {
      console.error(`❌ Failed to restore banner ${id}:`, error);
      throw error;
    }
  },
};

export default bannerApi;