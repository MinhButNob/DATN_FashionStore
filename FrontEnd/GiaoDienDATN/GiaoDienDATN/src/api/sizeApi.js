import api from "./axios";

const API_URL = "/sizes";

export const sizeApi = {
  getAll(page = 0, size = 10, status = null) {
    let url = `${API_URL}?page=${page}&size=${size}`;
    if (status) {
      url += `&status=${status}`;
    }
    return api.get(url);
  },

  getById(id) {
    return api.get(`${API_URL}/${id}`);
  },

  create(data) {
    return api.post(API_URL, data);
  },

  update(id, data) {
    return api.put(`${API_URL}/${id}`, data);
  },

  delete(id) {
    return api.delete(`${API_URL}/${id}`);
  },

  restore(id) {
    return api.patch(`${API_URL}/${id}/restore`);
  },

  search(keyword, page = 0, size = 10) {
    return api.get(`${API_URL}/search`, {
      params: { keyword, page, size },
    });
  },

  count() {
    return api.get(`${API_URL}/count`);
  },
};