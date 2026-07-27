import api from './axios.js'

// baseURL đã có sẵn nên chỉ cần '/employees'
const API_URL = '/employees'

export const employeeApi = {
  getAll() {
    return api.get(API_URL)
  },

  create(data) {
    return api.post(API_URL, data)
  },

  update(id, data) {
    return api.put(`${API_URL}/${id}`, data)
  },

  delete(id) {
    return api.delete(`${API_URL}/${id}`)
  },

  getById(id) {
    return api.get(`${API_URL}/${id}`)
  },

  search(params) {
    return api.get(`${API_URL}/search`, { params })
  },

  toggleLock(id) {
    return api.patch(`${API_URL}/${id}/toggle-lock`)
  },

  restore(id) {
    return api.patch(`${API_URL}/${id}/restore`)
  },

  getStatistics() {
    return api.get(`${API_URL}/statistics`)
  }
}