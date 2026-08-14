import axios from "axios";

// Wrapper cho provinces.open-api.vn V2
// API Documentation: https://provinces.open-api.vn/

export const vietnamAddressApi = {
  // Giữ lại API cũ của dự án: provinces.open-api.vn/api
  getProvinces: () => axios.get('https://provinces.open-api.vn/api/?depth=1'),
  getDistricts: (provinceCode) =>
    axios.get(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`),
  getWards: (districtCode) =>
    axios.get(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`),
};
