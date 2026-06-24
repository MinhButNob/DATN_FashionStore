import { defineStore } from "pinia";
import { ref, computed } from "vue";
import bannerService from "../services/bannerService";

export const useBannerStore = defineStore("banners", () => {
  // ========== STATE ==========
  const banners = ref([]);
  const activeBanners = ref([]);
  const loading = ref(false);
  const saving = ref(false);
  const error = ref(null);

  const currentPage = ref(0);
  const totalPages = ref(1);
  const totalElements = ref(0);
  const pageSize = ref(10);
  const keyword = ref("");

  // ========== GETTERS ==========
  const activeBannerCount = computed(
    () => banners.value?.filter((b) => b.status === "ACTIVE").length || 0
  );
  const inactiveBannerCount = computed(
    () => banners.value?.filter((b) => b.status === "INACTIVE").length || 0
  );
  const totalVisibleCount = computed(
    () => banners.value?.filter((b) => b.status !== "DELETED").length || 0
  );

  // ========== ACTIONS ==========
  async function fetchActiveBanners() {
    loading.value = true;
    error.value = null;
    try {
      const res = await bannerService.getActiveBanners();
      activeBanners.value = res.data || [];
      console.log('✅ Active banners loaded:', activeBanners.value.length);
      // ✅ Log image paths để debug
      activeBanners.value.forEach(b => {
        console.log(`📸 Banner "${b.title}" image path:`, b.imagePath);
      });
    } catch (e) {
      error.value = "Không thể tải banner";
      console.error("fetchActiveBanners error:", e);
    } finally {
      loading.value = false;
    }
  }

  async function fetchBanners() {
    loading.value = true;
    error.value = null;
    try {
      const res = await bannerService.getBanners({
        keyword: keyword.value,
        page: currentPage.value,
        size: pageSize.value,
        sortBy: "displayOrder",
        sortDirection: "asc",
      });
      banners.value = res.data?.content || [];
      totalPages.value = res.data?.totalPages || 1;
      totalElements.value = res.data?.totalElements || 0;
      console.log('✅ Banners loaded:', banners.value.length);
    } catch (e) {
      error.value = "Không thể tải danh sách banner";
      console.error("fetchBanners error:", e);
    } finally {
      loading.value = false;
    }
  }

  async function createBanner(formData) {
    saving.value = true;
    error.value = null;
    try {
      console.log("📤 Creating banner with data:");
      for (let [key, value] of formData.entries()) {
        console.log(`  ${key}: ${value instanceof File ? `File: ${value.name} (${value.size} bytes)` : value}`);
      }

      const response = await bannerService.createBanner(formData);
      console.log("✅ Create success:", response.data);
      console.log("📸 Image path from server:", response.data?.imagePath);
      
      await fetchBanners();
      return { success: true, data: response.data };
    } catch (e) {
      console.error("❌ Create error:", e);
      
      let errorMessage = "Tạo banner thất bại";
      if (e.response?.data) {
        if (typeof e.response.data === "string") {
          errorMessage = e.response.data;
        } else if (e.response.data.message) {
          errorMessage = e.response.data.message;
        } else if (e.response.data.errors) {
          const errors = e.response.data.errors;
          if (typeof errors === 'object') {
            errorMessage = Object.values(errors).join(', ');
          } else if (Array.isArray(errors)) {
            errorMessage = errors.map(err => err.defaultMessage || err.message).join(', ');
          } else {
            errorMessage = errors;
          }
        }
      }
      
      if (e.response?.status === 401) {
        errorMessage = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
      } else if (e.response?.status === 403) {
        errorMessage = "Bạn không có quyền thực hiện hành động này.";
      }
      
      error.value = errorMessage;
      return {
        success: false,
        message: errorMessage,
        errors: e.response?.data?.errors,
        status: e.response?.status,
      };
    } finally {
      saving.value = false;
    }
  }

  async function updateBanner(id, formData) {
    saving.value = true;
    error.value = null;
    try {
      console.log(`📤 Updating banner ${id} with data:`);
      for (let [key, value] of formData.entries()) {
        console.log(`  ${key}: ${value instanceof File ? `File: ${value.name} (${value.size} bytes)` : value}`);
      }

      const response = await bannerService.updateBanner(id, formData);
      console.log("✅ Update success:", response.data);
      console.log("📸 Image path from server:", response.data?.imagePath);
      
      await fetchBanners();
      return { success: true, data: response.data };
    } catch (e) {
      console.error("❌ Update error:", e);
      
      let errorMessage = "Cập nhật banner thất bại";
      if (e.response?.data) {
        if (typeof e.response.data === "string") {
          errorMessage = e.response.data;
        } else if (e.response.data.message) {
          errorMessage = e.response.data.message;
        } else if (e.response.data.errors) {
          const errors = e.response.data.errors;
          if (typeof errors === 'object') {
            errorMessage = Object.values(errors).join(', ');
          } else if (Array.isArray(errors)) {
            errorMessage = errors.map(err => err.defaultMessage || err.message).join(', ');
          } else {
            errorMessage = errors;
          }
        }
      }
      
      if (e.response?.status === 401) {
        errorMessage = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
      } else if (e.response?.status === 403) {
        errorMessage = "Bạn không có quyền thực hiện hành động này.";
      }
      
      error.value = errorMessage;
      return {
        success: false,
        message: errorMessage,
        errors: e.response?.data?.errors,
        status: e.response?.status,
      };
    } finally {
      saving.value = false;
    }
  }

  async function deleteBanner(id) {
    try {
      await bannerService.deleteBanner(id);
      await fetchBanners();
      return { success: true };
    } catch (e) {
      console.error("Delete error:", e);
      let errorMessage = "Xóa banner thất bại";
      if (e.response?.data?.message) {
        errorMessage = e.response.data.message;
      }
      return { 
        success: false, 
        message: errorMessage,
        status: e.response?.status,
      };
    }
  }

  async function restoreBanner(id) {
    try {
      await bannerService.restoreBanner(id);
      await fetchBanners();
      return { success: true };
    } catch (e) {
      console.error("Restore error:", e);
      let errorMessage = "Khôi phục banner thất bại";
      if (e.response?.data?.message) {
        errorMessage = e.response.data.message;
      }
      return { 
        success: false, 
        message: errorMessage,
        status: e.response?.status,
      };
    }
  }

  function setPage(page) {
    currentPage.value = page;
    fetchBanners();
  }

  function search(kw) {
    keyword.value = kw;
    currentPage.value = 0;
    fetchBanners();
  }

  return {
    // state
    banners,
    activeBanners,
    loading,
    saving,
    error,
    currentPage,
    totalPages,
    totalElements,
    pageSize,
    keyword,
    // getters
    activeBannerCount,
    inactiveBannerCount,
    totalVisibleCount,
    // actions
    fetchActiveBanners,
    fetchBanners,
    createBanner,
    updateBanner,
    deleteBanner,
    restoreBanner,
    setPage,
    search,
  };
});