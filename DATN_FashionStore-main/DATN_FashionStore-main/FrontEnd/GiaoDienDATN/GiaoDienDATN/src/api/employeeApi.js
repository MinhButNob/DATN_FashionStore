import axios from './axios.js'

// baseURL đã là http://localhost:8080/api nên chỉ cần '/employees'
const API_URL = '/employees'

export const employeeApi = {
  // ✅ THÊM method này
  getAll() {
    return axios.get(API_URL)
  },

  create(data) {
    return axios.post(API_URL, data)
  },

  update(id, data) {
    return axios.put(`${API_URL}/${id}`, data)
  },

  delete(id) {
    return axios.delete(`${API_URL}/${id}`)
  },

  getById(id) {
    return axios.get(`${API_URL}/${id}`)
  },

  search(params) {
    return axios.get(`${API_URL}/search`, { params })
  },

  getStatistics() {
    return axios.get(`${API_URL}/statistics`)
  }
}