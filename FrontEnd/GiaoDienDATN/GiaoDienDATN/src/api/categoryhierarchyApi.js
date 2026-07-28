// src/api/categoryhierarchyApi.js
// LỚP API - CHỈ chứa lời gọi HTTP thuần túy tới backend.
// KHÔNG xử lý logic nghiệp vụ, KHÔNG try/catch ở đây.
// Được dùng bởi: src/service/categoryhierarchyService.js

import api from "./axios";

const categoryHierarchyApi = {
  // GET /tree - toàn bộ cây danh mục (ACTIVE + INACTIVE)
  fetchTree() {
    return api.get("/category-hierarchy/tree");
  },

  // GET /tree/active - cây danh mục chỉ ACTIVE
  fetchActiveTree() {
    return api.get("/category-hierarchy/tree/active");
  },

  // GET /parents - danh sách danh mục cấp 1
  fetchParents() {
    return api.get("/category-hierarchy/parents");
  },

  // GET /children/:parentId - danh mục con theo cha
  fetchChildren(parentId) {
    return api.get(`/category-hierarchy/children/${parentId}`);
  },

  // POST / - tạo danh mục mới
  create(payload) {
    return api.post("/category-hierarchy", payload);
  },

  // PUT /:id - cập nhật danh mục
  update(id, payload) {
    return api.put(`/category-hierarchy/${id}`, payload);
  },

  // DELETE /:id - xóa mềm danh mục
  remove(id) {
    return api.delete(`/category-hierarchy/${id}`);
  },

  // PATCH /:id/restore - khôi phục danh mục đã xóa
  restore(id) {
    return api.patch(`/category-hierarchy/${id}/restore`);
  } };

export default categoryHierarchyApi;