import api from "./axios";

export const roleApi = {
  getAll: (page = 0, size = 10, status = null) => {
    let url = `/roles?page=${page}&size=${size}`;
    if (status !== null && status !== "") {
      url += `&status=${status}`;
    }
    return api.get(url);
  },

  getAllNoParams: () => {
    return api.get("/roles/all");
  },

  getById: (id) => {
    return api.get(`/roles/${id}`);
  },

  // Lấy danh sách theo status cụ thể (ACTIVE, INACTIVE, DELETED)
  getByStatus: (status, page = 0, size = 10) => {
    return api.get(`/roles?status=${status}&page=${page}&size=${size}`);
  },

  create: (roleData) => {
    return api.post("/roles", roleData);
  },

  update: (id, roleData) => {
    return api.put(`/roles/${id}`, roleData);
  },

  // Soft delete - chuyển status thành DELETED
  // Soft delete - chuyển status thành DELETED (không xóa khỏi DB)
  delete: (id) => {
    return api.delete(`/roles/${id}`);
  },

  // Khôi phục role đã xóa (chuyển từ DELETED sang INACTIVE hoặc ACTIVE)
  restore: (id) => {
    return api.patch(`/roles/${id}/restore`);
  },

  // Xóa cứng khỏi database
  permanentDelete: (id) => {
    return api.delete(`/roles/${id}/permanent`);
  },

  search: (keyword, page = 0, size = 10) => {
    return api.get(
      `/roles/search?keyword=${keyword}&page=${page}&size=${size}`,
    );
  } };
