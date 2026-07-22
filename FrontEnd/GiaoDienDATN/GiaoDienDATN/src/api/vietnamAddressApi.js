import axios from 'axios';

// Wrapper cho provinces.open-api.vn V2
// API Documentation: https://provinces.open-api.vn/

export const vietnamAddressApi = {
  /**
   * Lấy danh sách tất cả tỉnh/thành phố (V2)
   * Trả về: mảng [{ code, name, division_type, codename, phone_code }]
   */
  getProvinces: () => axios.get('https://provinces.open-api.vn/api/v2/p/'),

  /**
   * Lấy danh sách phường/xã theo mã tỉnh (V2 - bỏ qua cấp quận/huyện)
   * @param {number} provinceCode - Mã tỉnh/thành phố
   */
  getWards: (provinceCode) =>
    axios.get(`https://provinces.open-api.vn/api/v2/w/?province=${provinceCode}`),
};
