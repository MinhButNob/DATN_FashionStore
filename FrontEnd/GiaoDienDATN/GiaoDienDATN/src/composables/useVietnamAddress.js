import { ref } from 'vue';
import { vietnamAddressApi } from '../api/vietnamAddressApi';

export function useVietnamAddress() {
  const provinces = ref([]);
  const districts = ref([]);
  const wards = ref([]);
  
  const loadingProvinces = ref(false);
  const loadingWards = ref(false);
  const loadingDistricts = ref(false);

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

  const fetchDistricts = async (provinceCode) => {
    districts.value = [];
    wards.value = [];
    if (!provinceCode) return;
    loadingDistricts.value = true;
    try {
      const response = await vietnamAddressApi.getDistricts(provinceCode);
      districts.value = response.data?.districts || response.data || [];
      console.log('[AddressLookup][Old API] Districts loaded:', {
        provinceCode,
        count: districts.value.length
      });
    } catch (error) {
      console.error('[AddressLookup] Error fetching districts:', { provinceCode, error });
    } finally {
      loadingDistricts.value = false;
    }
  };

  const fetchWards = async (districtCode) => {
    wards.value = [];
    if (!districtCode) {
      return;
    }
    loadingWards.value = true;
    try {
      const response = await vietnamAddressApi.getWards(districtCode);
      wards.value = response.data?.wards || response.data || [];
      console.log('[AddressLookup][Old API] Wards loaded:', {
        districtCode,
        count: wards.value.length
      });
    } catch (error) {
      console.error('[AddressLookup] Error fetching wards:', { districtCode, error });
      wards.value = [];
    } finally {
      loadingWards.value = false;
    }
  };

  return {
    provinces,
    districts,
    wards,
    loadingProvinces,
    loadingDistricts,
    loadingWards,
    fetchProvinces,
    fetchDistricts,
    fetchWards
  };
}
