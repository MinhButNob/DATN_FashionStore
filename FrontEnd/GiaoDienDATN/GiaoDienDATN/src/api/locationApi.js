// src/api/locationApi.js
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

const getToken = () => localStorage.getItem("token");

const getHeaders = () => ({
  headers: {
    Authorization: `Bearer ${getToken()}`,
    "Content-Type": "application/json",
  },
});

// =============================================
// PROVINCE API
// =============================================
export const provinceApi = {
  /**
   * Lấy danh sách tất cả tỉnh/thành phố
   * GET /api/admin/dia-chi/provinces
   */
  getAll: () => axios.get(`${API_BASE}/admin/dia-chi/provinces`, getHeaders()),

  /**
   * Lấy chi tiết tỉnh/thành phố theo ID
   * GET /api/admin/dia-chi/provinces/{id}
   */
  getById: (id) =>
    axios.get(`${API_BASE}/admin/dia-chi/provinces/${id}`, getHeaders()),

  /**
   * Tạo mới tỉnh/thành phố
   * POST /api/admin/dia-chi/provinces
   */
  create: (data) =>
    axios.post(`${API_BASE}/admin/dia-chi/provinces`, data, getHeaders()),

  /**
   * Cập nhật tỉnh/thành phố
   * PUT /api/admin/dia-chi/provinces/{id}
   */
  update: (id, data) =>
    axios.put(`${API_BASE}/admin/dia-chi/provinces/${id}`, data, getHeaders()),

  /**
   * Xóa tỉnh/thành phố (soft delete)
   * DELETE /api/admin/dia-chi/provinces/{id}
   */
  delete: (id) =>
    axios.delete(`${API_BASE}/admin/dia-chi/provinces/${id}`, getHeaders()),

  /**
   * Tìm kiếm tỉnh/thành phố
   * GET /api/admin/dia-chi/provinces/search?keyword=...
   */
  search: (keyword) =>
    axios.get(
      `${API_BASE}/admin/dia-chi/provinces/search?keyword=${keyword}`,
      getHeaders(),
    ),
};

// =============================================
// WARD API
// =============================================
export const wardApi = {
  /**
   * ✅ THÊM MỚI: Lấy danh sách tất cả phường/xã
   * GET /api/admin/dia-chi/wards
   */
  getAll: () => axios.get(`${API_BASE}/admin/dia-chi/wards`, getHeaders()),

  /**
   * Lấy danh sách phường/xã theo tỉnh/thành phố
   * GET /api/admin/dia-chi/provinces/{provinceId}/wards
   */
  getByProvince: (provinceId) =>
    axios.get(
      `${API_BASE}/admin/dia-chi/provinces/${provinceId}/wards`,
      getHeaders(),
    ),

  /**
   * Lấy chi tiết phường/xã theo ID
   * GET /api/admin/dia-chi/wards/{id}
   */
  getById: (id) =>
    axios.get(`${API_BASE}/admin/dia-chi/wards/${id}`, getHeaders()),

  /**
   * Tạo mới phường/xã
   * POST /api/admin/dia-chi/wards
   */
  create: (data) =>
    axios.post(`${API_BASE}/admin/dia-chi/wards`, data, getHeaders()),

  /**
   * Cập nhật phường/xã
   * PUT /api/admin/dia-chi/wards/{id}
   */
  update: (id, data) =>
    axios.put(`${API_BASE}/admin/dia-chi/wards/${id}`, data, getHeaders()),

  /**
   * Xóa phường/xã (soft delete)
   * DELETE /api/admin/dia-chi/wards/{id}
   */
  delete: (id) =>
    axios.delete(`${API_BASE}/admin/dia-chi/wards/${id}`, getHeaders()),

  /**
   * Tìm kiếm phường/xã
   * GET /api/admin/dia-chi/wards/search?keyword=...
   */
  search: (keyword) =>
    axios.get(
      `${API_BASE}/admin/dia-chi/wards/search?keyword=${keyword}`,
      getHeaders(),
    ),
};
