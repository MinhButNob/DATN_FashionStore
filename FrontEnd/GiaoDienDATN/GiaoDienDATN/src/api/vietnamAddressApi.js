import axios from "axios";

// Wrapper cho provinces.open-api.vn V2
// API Documentation: https://provinces.open-api.vn/

export const vietnamAddressApi = {
  getProvinces: () => axios.get('https://provinces.open-api.vn/api/v2/p/'),

  getWards: (provinceCode) =>
    axios.get(`https://provinces.open-api.vn/api/v2/w/?province=${provinceCode}`),
};