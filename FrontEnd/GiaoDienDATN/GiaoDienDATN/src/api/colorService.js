// src/api/colorService.js
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

// ✅ CÁCH 1: Export default object
const colorService = {
  async getColors(page = 0, size = 100) {
    try {
      const response = await axios.get(`${API_BASE}/colors`, {
        params: { page, size },
      });
      return response;
    } catch (error) {
      console.error("Lỗi lấy danh sách màu:", error);
      throw error;
    }
  },

  async getActiveColors() {
    try {
      const response = await axios.get(`${API_BASE}/colors/active`);
      return response;
    } catch (error) {
      console.error("Lỗi lấy màu hoạt động:", error);
      throw error;
    }
  },

  async getColorById(id) {
    try {
      const response = await axios.get(`${API_BASE}/colors/${id}`);
      return response;
    } catch (error) {
      console.error(`Lỗi lấy màu ID ${id}:`, error);
      throw error;
    }
  },

  async createColor(data) {
    try {
      const response = await axios.post(`${API_BASE}/colors`, data);
      return response;
    } catch (error) {
      console.error("Lỗi tạo màu:", error);
      throw error;
    }
  },

  async updateColor(id, data) {
    try {
      const response = await axios.put(`${API_BASE}/colors/${id}`, data);
      return response;
    } catch (error) {
      console.error(`Lỗi cập nhật màu ID ${id}:`, error);
      throw error;
    }
  },

  async deleteColor(id) {
    try {
      const response = await axios.delete(`${API_BASE}/colors/${id}`);
      return response;
    } catch (error) {
      console.error(`Lỗi xóa màu ID ${id}:`, error);
      throw error;
    }
  },

  async restoreColor(id) {
    try {
      const response = await axios.patch(`${API_BASE}/colors/${id}/restore`);
      return response;
    } catch (error) {
      console.error(`Lỗi khôi phục màu ID ${id}:`, error);
      throw error;
    }
  },

  async searchColors(keyword, page = 0, size = 10) {
    try {
      const response = await axios.get(`${API_BASE}/colors/search`, {
        params: { keyword, page, size },
      });
      return response;
    } catch (error) {
      console.error("Lỗi tìm kiếm màu:", error);
      throw error;
    }
  },

  async countColors() {
    try {
      const response = await axios.get(`${API_BASE}/colors/count`);
      return response;
    } catch (error) {
      console.error("Lỗi đếm màu:", error);
      throw error;
    }
  },
};

export default colorService;
