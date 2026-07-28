import { ref } from 'vue';
import { vietnamAddressApi } from '../api/vietnamAddressApi';

export function useVietnamAddress() {
  const provinces = ref([]);
  const wards = ref([]);
  
  const loadingProvinces = ref(false);
  const loadingWards = ref(false);

  // Load danh sách tỉnh/thành phố
  const fetchProvinces = async () => {
    loadingProvinces.value = true;
    try {
      const response = await vietnamAddressApi.getProvinces();
      provinces.value = response.data || [];
    } catch (error) {
      console.error('Error fetching provinces:', error);
      provinces.value = [];
    } finally {
      loadingProvinces.value = false;
    }
  };

  // Load danh sách phường/xã dựa trên provinceCode
  const fetchWards = async (provinceCode) => {
    if (!provinceCode) {
      wards.value = [];
      return;
    }
    loadingWards.value = true;
    try {
      const response = await vietnamAddressApi.getWards(provinceCode);
      // Data trả về từ api v2 có dạng mảng, hoặc object chứa mảng. 
      // API documentation: return array trực tiếp hoặc response.data.wards (nếu có format khác)
      // Dựa theo api v2 w/?province= thì nó trả về mảng trực tiếp.
      wards.value = response.data?.wards || response.data || [];
    } catch (error) {
      console.error('Error fetching wards:', error);
      wards.value = [];
    } finally {
      loadingWards.value = false;
    }
  };

  return {
    provinces,
    wards,
    loadingProvinces,
    loadingWards,
    fetchProvinces,
    fetchWards
  };
}
