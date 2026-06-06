import axios from "./axios";

const API_URL = "/sizes";

export const sizeApi = {
  getAll(page = 0, size = 10, status = null) {
    let url = `${API_URL}?page=${page}&size=${size}`;
    if (status) {
      url += `&status=${status}`;
    }
    return axios.get(url);
  },

  getById(id) {
    return axios.get(`${API_URL}/${id}`);
  },

  create(data) {
    return axios.post(API_URL, data);
  },

  update(id, data) {
    return axios.put(`${API_URL}/${id}`, data);
  },

  delete(id) {
    return axios.delete(`${API_URL}/${id}`);
  },

  restore(id) {
    return axios.patch(`${API_URL}/${id}/restore`);
  },

  search(keyword, page = 0, size = 10) {
    return axios.get(`${API_URL}/search`, {
      params: { keyword, page, size },
    });
  },

  count() {
    return axios.get(`${API_URL}/count`);
  },
};
