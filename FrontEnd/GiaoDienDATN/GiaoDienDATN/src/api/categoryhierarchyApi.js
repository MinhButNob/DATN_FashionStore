// src/api/categoryhierarchyApi.js
// LỚP API - CHỈ chứa lời gọi HTTP thuần túy tới backend.
// KHÔNG xử lý logic nghiệp vụ, KHÔNG try/catch ở đây.
// Được dùng bởi: src/service/categoryhierarchyService.js

import axios from "axios";

const API_BASE = "http://localhost:8080/api/category-hierarchy";

const categoryHierarchyApi = {
  // GET /tree - toàn bộ cây danh mục (ACTIVE + INACTIVE)
  fetchTree() {
    return axios.get(`${API_BASE}/tree`);
  },

  // GET /tree/active - cây danh mục chỉ ACTIVE
  fetchActiveTree() {
    return axios.get(`${API_BASE}/tree/active`);
  },

  // GET /parents - danh sách danh mục cấp 1
  fetchParents() {
    return axios.get(`${API_BASE}/parents`);
  },

  // GET /children/:parentId - danh mục con theo cha
  fetchChildren(parentId) {
    return axios.get(`${API_BASE}/children/${parentId}`);
  },

  // POST / - tạo danh mục mới
  create(payload) {
    return axios.post(`${API_BASE}`, payload);
  },

  // PUT /:id - cập nhật danh mục
  update(id, payload) {
    return axios.put(`${API_BASE}/${id}`, payload);
  },

  // DELETE /:id - xóa mềm danh mục
  remove(id) {
    return axios.delete(`${API_BASE}/${id}`);
  },

  // PATCH /:id/restore - khôi phục danh mục đã xóa
  restore(id) {
    return axios.patch(`${API_BASE}/${id}/restore`);
  },
};

export default categoryHierarchyApi;