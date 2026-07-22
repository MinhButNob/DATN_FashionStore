// src/services/locationService.js
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

// Lấy token từ localStorage
const getToken = () => localStorage.getItem("token");

// Headers cho request
const getHeaders = () => ({
  headers: {
    Authorization: `Bearer ${getToken()}`,
    "Content-Type": "application/json",
  },
});

// =============================================
// PROVINCE API
// =============================================

export const provinceService = {
  // Lấy danh sách tỉnh/thành phố
  getAll: async () => {
    const response = await axios.get(
      `${API_BASE}/admin/dia-chi/provinces`,
      getHeaders(),
    );
    return response.data;
  },

  // Lấy chi tiết tỉnh/thành phố
  getById: async (id) => {
    const response = await axios.get(
      `${API_BASE}/admin/dia-chi/provinces/${id}`,
      getHeaders(),
    );
    return response.data;
  },

  // Tạo mới tỉnh/thành phố
  create: async (data) => {
    const response = await axios.post(
      `${API_BASE}/admin/dia-chi/provinces`,
      data,
      getHeaders(),
    );
    return response.data;
  },

  // Cập nhật tỉnh/thành phố
  update: async (id, data) => {
    const response = await axios.put(
      `${API_BASE}/admin/dia-chi/provinces/${id}`,
      data,
      getHeaders(),
    );
    return response.data;
  },

  // Xóa tỉnh/thành phố (soft delete)
  delete: async (id) => {
    const response = await axios.delete(
      `${API_BASE}/admin/dia-chi/provinces/${id}`,
      getHeaders(),
    );
    return response.data;
  },

  // Tìm kiếm tỉnh/thành phố
  search: async (keyword) => {
    const response = await axios.get(
      `${API_BASE}/admin/dia-chi/provinces/search?keyword=${keyword}`,
      getHeaders(),
    );
    return response.data;
  },
};

// =============================================
// WARD API
// =============================================

export const wardService = {
  // Lấy danh sách phường/xã theo tỉnh
  getByProvince: async (provinceId) => {
    const response = await axios.get(
      `${API_BASE}/admin/dia-chi/provinces/${provinceId}/wards`,
      getHeaders(),
    );
    return response.data;
  },

  // Lấy chi tiết phường/xã
  getById: async (id) => {
    const response = await axios.get(
      `${API_BASE}/admin/dia-chi/wards/${id}`,
      getHeaders(),
    );
    return response.data;
  },

  // Tạo mới phường/xã
  create: async (data) => {
    const response = await axios.post(
      `${API_BASE}/admin/dia-chi/wards`,
      data,
      getHeaders(),
    );
    return response.data;
  },

  // Cập nhật phường/xã
  update: async (id, data) => {
    const response = await axios.put(
      `${API_BASE}/admin/dia-chi/wards/${id}`,
      data,
      getHeaders(),
    );
    return response.data;
  },

  // Xóa phường/xã (soft delete)
  delete: async (id) => {
    const response = await axios.delete(
      `${API_BASE}/admin/dia-chi/wards/${id}`,
      getHeaders(),
    );
    return response.data;
  },

  // Tìm kiếm phường/xã
  search: async (keyword) => {
    const response = await axios.get(
      `${API_BASE}/admin/dia-chi/wards/search?keyword=${keyword}`,
      getHeaders(),
    );
    return response.data;
  },
};
