// src/stores/productStore.js
import { defineStore } from "pinia";
import { ref, computed } from "vue";
import productService from "@/api/productService";
import categoryService from "@/api/categoryService";
import colorService from "@/api/colorService";
import sizeService from "@/api/sizeService";

export const useProductStore = defineStore("product", () => {
  // State - KHÔNG có mock data
  const products = ref([]);
  const categories = ref([]);
  const colors = ref([]);
  const sizes = ref([]);
  const statistics = ref(null);
  const loading = ref(false);
  const error = ref(null);

  // Pagination
  const currentPage = ref(0);
  const totalPages = ref(0);
  const totalElements = ref(0);
  const pageSize = ref(10);

  // Filters
  const filters = ref({
    keyword: "",
    categoryId: null,
    isActive: null,
    sortBy: "productId",
    sortDir: "DESC",
  });

  // Actions
  async function fetchProducts() {
    loading.value = true;
    error.value = null;
    try {
      const params = {
        page: currentPage.value,
        size: pageSize.value,
        sortBy: filters.value.sortBy,
        sortDir: filters.value.sortDir,
      };

      if (filters.value.keyword) params.keyword = filters.value.keyword;
      if (filters.value.categoryId)
        params.categoryId = filters.value.categoryId;
      if (filters.value.isActive !== null)
        params.isActive = filters.value.isActive;

      const response = await productService.getProducts(params);
      const data = response.data?.data || response.data;

      if (data && data.content !== undefined) {
        products.value = data.content || [];
        totalPages.value = data.totalPages || 0;
        totalElements.value = data.totalElements || 0;
        currentPage.value = data.number || 0;
      } else if (Array.isArray(data)) {
        products.value = data;
        totalPages.value = 1;
        totalElements.value = data.length;
      } else {
        products.value = [];
      }

      console.log("✅ Đã tải products từ backend:", products.value.length);
      return data;
    } catch (err) {
      error.value = err.response?.data?.message || "Không thể tải sản phẩm";
      console.error("❌ Lỗi fetchProducts:", err);
      products.value = []; // ❌ Không có mock data, chỉ là mảng rỗng
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function fetchCategories() {
    try {
      const response = await categoryService.getAll();
      const data = response.data?.data || response.data;
      categories.value = Array.isArray(data) ? data : data?.content || [];
      console.log("✅ Đã tải categories từ backend:", categories.value.length);
      return categories.value;
    } catch (err) {
      console.error("❌ Lỗi fetchCategories:", err);
      categories.value = []; // ❌ Không có mock data, chỉ là mảng rỗng
      return [];
    }
  }

  async function fetchColors() {
    try {
      const response = await colorService.getAll();
      const data = response.data?.data || response.data;
      colors.value = Array.isArray(data) ? data : data?.content || [];
      console.log("✅ Đã tải colors từ backend:", colors.value.length);
      return colors.value;
    } catch (err) {
      console.error("❌ Lỗi fetchColors:", err);
      colors.value = []; // ❌ Không có mock data, chỉ là mảng rỗng
      return [];
    }
  }

  async function fetchSizes() {
    try {
      const response = await sizeService.getAll();
      const data = response.data?.data || response.data;
      sizes.value = Array.isArray(data) ? data : data?.content || [];
      console.log("✅ Đã tải sizes từ backend:", sizes.value.length);
      return sizes.value;
    } catch (err) {
      console.error("❌ Lỗi fetchSizes:", err);
      sizes.value = []; // ❌ Không có mock data, chỉ là mảng rỗng
      return [];
    }
  }

  async function fetchStatistics() {
    try {
      const response = await productService.getStatistics();
      const data = response.data?.data || response.data;
      statistics.value = {
        totalProducts: data?.totalProducts || 0,
        inStockVariants: data?.inStockVariants || 0,
        lowStockVariants: data?.lowStockVariants || 0,
        ...data,
      };
      return statistics.value;
    } catch (err) {
      console.error("❌ Lỗi fetchStatistics:", err);
      statistics.value = {
        totalProducts: 0,
        inStockVariants: 0,
        lowStockVariants: 0,
      };
      return statistics.value;
    }
  }

  async function createProduct(productData) {
    loading.value = true;
    try {
      const response = await productService.createProduct(productData);
      await fetchProducts();
      await fetchStatistics();
      return response.data?.data || response.data;
    } catch (err) {
      error.value = err.response?.data?.message || "Không thể tạo sản phẩm";
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function updateProduct(id, productData) {
    loading.value = true;
    try {
      const response = await productService.updateProduct(id, productData);
      await fetchProducts();
      await fetchStatistics();
      return response.data?.data || response.data;
    } catch (err) {
      error.value =
        err.response?.data?.message || "Không thể cập nhật sản phẩm";
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function deleteProduct(id) {
    loading.value = true;
    try {
      await productService.deleteProduct(id);
      await fetchProducts();
      await fetchStatistics();
    } catch (err) {
      error.value = err.response?.data?.message || "Không thể xóa sản phẩm";
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function toggleProductStatus(id) {
    try {
      const response = await productService.toggleProductStatus(id);
      await fetchProducts();
      await fetchStatistics();
      return response.data?.data || response.data;
    } catch (err) {
      error.value =
        err.response?.data?.message || "Không thể thay đổi trạng thái";
      throw err;
    }
  }

  function setFilter(key, value) {
    filters.value[key] = value;
    currentPage.value = 0;
    fetchProducts();
  }

  function resetFilters() {
    filters.value = {
      keyword: "",
      categoryId: null,
      isActive: null,
      sortBy: "productId",
      sortDir: "DESC",
    };
    currentPage.value = 0;
    fetchProducts();
  }

  function goToPage(page) {
    currentPage.value = page;
    fetchProducts();
  }

  function setPageSize(size) {
    pageSize.value = size;
    currentPage.value = 0;
    fetchProducts();
  }

  return {
    // State
    products,
    categories,
    colors,
    sizes,
    statistics,
    loading,
    error,
    currentPage,
    totalPages,
    totalElements,
    pageSize,
    filters,

    // Getters
    productList: computed(() => products.value),
    categoryList: computed(() => categories.value),
    colorList: computed(() => colors.value),
    sizeList: computed(() => sizes.value),
    isLoading: computed(() => loading.value),
    hasError: computed(() => error.value),

    // Actions
    fetchProducts,
    fetchCategories,
    fetchColors,
    fetchSizes,
    fetchStatistics,
    createProduct,
    updateProduct,
    deleteProduct,
    toggleProductStatus,
    setFilter,
    resetFilters,
    goToPage,
    setPageSize,
  };
});
