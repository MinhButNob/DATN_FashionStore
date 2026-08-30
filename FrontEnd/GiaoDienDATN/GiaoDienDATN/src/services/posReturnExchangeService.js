// src/services/posReturnExchangeService.js
import api from "@/api/axios";

const BASE_URL = "/pos/return-exchange";

export const posReturnExchangeService = {
  /**
   * Tìm đơn hàng theo số điện thoại
   */
  searchOrdersByPhone(phone) {
    return api.get(`${BASE_URL}/orders/search`, {
      params: { phone },
    });
  },

  /**
   * Tạo phiếu đổi/trả hàng
   */
  createReturnExchange(data) {
    return api.post(`${BASE_URL}/create`, data);
  },

  /**
   * Lấy danh sách phiếu đổi/trả
   */
  getAllReturnExchanges(params = {}) {
    return api.get(`${BASE_URL}/list`, { params });
  },

  /**
   * Lấy chi tiết phiếu đổi/trả
   */
  getReturnExchangeDetail(id) {
    return api.get(`${BASE_URL}/${id}`);
  },

  /**
   * Cập nhật trạng thái phiếu
   * Nhận 2 tham số: id và status
   */
  updateReturnExchangeStatus(id, status) {
    return api.put(`${BASE_URL}/${id}/status`, { status });
  },

  /**
   * Hủy phiếu đổi/trả
   */
  cancelReturnExchange(id) {
    return api.delete(`${BASE_URL}/${id}`);
  },

  /**
   * In phiếu đổi/trả
   */
  printReturnExchange(id) {
    return api.get(`${BASE_URL}/${id}/print`);
  },
};

export default posReturnExchangeService;
