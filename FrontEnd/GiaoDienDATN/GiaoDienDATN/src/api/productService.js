// src/api/productService.js
import axios from "axios";

const API_BASE = "http://localhost:8080/api/v1";
const ADMIN_API_BASE = "http://localhost:8080/api/admin";
const CUSTOMER_API_BASE = "http://localhost:8080/api/customer/home";

const productService = {
  // ==================== SẢN PHẨM ====================

  /**
   * Lấy danh sách sản phẩm (có phân trang và lọc)
   * @param {Object} params - { page, size, keyword, categoryId, status, sortBy, sortDir }
   */
  async getProducts(params = {}) {
    try {
      const defaultParams = {
        page: 0,
        size: 10,
        sortBy: "productId",
        sortDir: "DESC",
      };
      const queryParams = { ...defaultParams, ...params };
      const response = await axios.get(`${API_BASE}/products`, {
        params: queryParams,
      });
      return response;
    } catch (error) {
      console.error("Lỗi lấy danh sách sản phẩm:", error);
      throw error;
    }
  },

  /**
   * Lấy sản phẩm theo ID
   * @param {Number} id - ID sản phẩm
   */
  async getProductById(id) {
    try {
      const response = await axios.get(`${API_BASE}/products/${id}`);
      return response;
    } catch (error) {
      console.error(`Lỗi lấy sản phẩm ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Lấy thống kê sản phẩm
   */
  async getStatistics() {
    try {
      const response = await axios.get(`${API_BASE}/products/statistics`);
      return response;
    } catch (error) {
      console.error("Lỗi lấy thống kê sản phẩm:", error);
      throw error;
    }
  },

  /**
   * Tạo sản phẩm mới
   * @param {Object} data - Dữ liệu sản phẩm
   */
  async createProduct(data) {
    try {
      const response = await axios.post(`${API_BASE}/products`, data);
      return response;
    } catch (error) {
      console.error("Lỗi tạo sản phẩm:", error);
      throw error;
    }
  },

  /**
   * Cập nhật sản phẩm
   * @param {Number} id - ID sản phẩm
   * @param {Object} data - Dữ liệu cập nhật
   */
  async updateProduct(id, data) {
    try {
      const response = await axios.put(`${API_BASE}/products/${id}`, data);
      return response;
    } catch (error) {
      console.error(`Lỗi cập nhật sản phẩm ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Chuyển trạng thái sản phẩm (bật/tắt)
   * @param {Number} id - ID sản phẩm
   */
  async toggleProductStatus(id) {
    try {
      const response = await axios.patch(
        `${API_BASE}/products/${id}/toggle-status`,
      );
      return response;
    } catch (error) {
      console.error(`Lỗi chuyển trạng thái sản phẩm ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Xóa sản phẩm (xóa mềm)
   * @param {Number} id - ID sản phẩm
   */
  async deleteProduct(id) {
    try {
      const response = await axios.delete(`${API_BASE}/products/${id}`);
      return response;
    } catch (error) {
      console.error(`Lỗi xóa sản phẩm ID ${id}:`, error);
      throw error;
    }
  },

  // ==================== DANH MỤC ====================

  /**
   * Lấy tất cả danh mục
   */
  async getAllCategories() {
    try {
      const response = await axios.get(`${API_BASE}/categories`);
      return response;
    } catch (error) {
      console.error("Lỗi lấy danh sách danh mục:", error);
      throw error;
    }
  },

  /**
   * Lấy danh mục đang hoạt động (dùng cho dropdown)
   */
  async getActiveCategories() {
    try {
      const response = await axios.get(`${API_BASE}/categories/active`);
      return response;
    } catch (error) {
      console.error("Lỗi lấy danh mục hoạt động:", error);
      throw error;
    }
  },

  /**
   * Lấy danh mục theo ID
   */
  async getCategoryById(id) {
    try {
      const response = await axios.get(`${API_BASE}/categories/${id}`);
      return response;
    } catch (error) {
      console.error(`Lỗi lấy danh mục ID ${id}:`, error);
      throw error;
    }
  },

  // ==================== MÀU SẮC ====================

  /**
   * Lấy tất cả màu sắc
   */
  async getAllColors(page = 0, size = 100) {
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

  /**
   * Lấy màu đang hoạt động (dùng cho dropdown)
   */
  async getActiveColors() {
    try {
      const response = await axios.get(`${API_BASE}/colors/active`);
      return response;
    } catch (error) {
      console.error("Lỗi lấy màu hoạt động:", error);
      throw error;
    }
  },

  /**
   * Lấy màu theo ID
   */
  async getColorById(id) {
    try {
      const response = await axios.get(`${API_BASE}/colors/${id}`);
      return response;
    } catch (error) {
      console.error(`Lỗi lấy màu ID ${id}:`, error);
      throw error;
    }
  },

  // ==================== KÍCH THƯỚC ====================

  /**
   * Lấy tất cả kích thước
   */
  async getAllSizes(page = 0, size = 100) {
    try {
      const response = await axios.get(`${API_BASE}/sizes`, {
        params: { page, size },
      });
      return response;
    } catch (error) {
      console.error("Lỗi lấy danh sách kích thước:", error);
      throw error;
    }
  },

  /**
   * Lấy kích thước đang hoạt động (dùng cho dropdown)
   */
  async getActiveSizes() {
    try {
      const response = await axios.get(`${API_BASE}/sizes/active`);
      return response;
    } catch (error) {
      console.error("Lỗi lấy kích thước hoạt động:", error);
      throw error;
    }
  },

  /**
   * Lấy kích thước theo ID
   */
  async getSizeById(id) {
    try {
      const response = await axios.get(`${API_BASE}/sizes/${id}`);
      return response;
    } catch (error) {
      console.error(`Lỗi lấy kích thước ID ${id}:`, error);
      throw error;
    }
  },

  // ==================== KHUYẾN MÃI ====================

  /**
   * Lấy tất cả khuyến mãi (admin)
   */
  async getAllPromotions(params = {}) {
    try {
      const defaultParams = { page: 0, size: 100 };
      const queryParams = { ...defaultParams, ...params };
      const response = await axios.get(`${ADMIN_API_BASE}/promotions`, {
        params: queryParams,
      });
      return response;
    } catch (error) {
      console.error("Lỗi lấy danh sách khuyến mãi:", error);
      throw error;
    }
  },

  /**
   * Lấy khuyến mãi đang hoạt động (dùng cho dropdown)
   */
  async getActivePromotions() {
    try {
      const response = await axios.get(`${API_BASE}/promotions/active`);
      return response;
    } catch (error) {
      console.error("Lỗi lấy khuyến mãi hoạt động:", error);
      throw error;
    }
  },

  /**
   * Lấy khuyến mãi theo ID
   */
  async getPromotionById(id) {
    try {
      const response = await axios.get(`${ADMIN_API_BASE}/promotions/${id}`);
      return response;
    } catch (error) {
      console.error(`Lỗi lấy khuyến mãi ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Lấy khuyến mãi theo sản phẩm
   */
  async getPromotionsByProduct(productId) {
    try {
      const response = await axios.get(
        `${API_BASE}/products/${productId}/promotions`,
      );
      return response;
    } catch (error) {
      console.error(`Lỗi lấy khuyến mãi theo sản phẩm ID ${productId}:`, error);
      throw error;
    }
  },

  // ==================== CUSTOMER / HOME API (THÊM MỚI - KHÔNG ẢNH HƯỞNG CODE CŨ) ====================

  /**
   * Lấy danh sách sản phẩm trang chủ (có phân trang và lọc)
   * @param {Object} params - { page, size, keyword, categoryId, colorId, sizeId, minPrice, maxPrice, sortBy }
   */
  async getHomeProducts(params = {}) {
    try {
      const defaultParams = {
        page: 0,
        size: 12,
      };
      const queryParams = { ...defaultParams, ...params };
      const response = await axios.get(`${CUSTOMER_API_BASE}/products`, {
        params: queryParams,
      });
      return response;
    } catch (error) {
      console.error("Lỗi lấy sản phẩm trang chủ:", error);
      throw error;
    }
  },

  /**
   * Lấy sản phẩm mới nhất
   * @param {Number} page - Trang
   * @param {Number} size - Số lượng
   */
  async getLatestProducts(page = 0, size = 12) {
    try {
      const response = await axios.get(`${CUSTOMER_API_BASE}/latest-products`, {
        params: { page, size },
      });
      return response;
    } catch (error) {
      console.error("Lỗi lấy sản phẩm mới nhất:", error);
      throw error;
    }
  },

  /**
   * Lấy sản phẩm nổi bật
   * @param {Number} page - Trang
   * @param {Number} size - Số lượng
   */
  async getFeaturedProducts(page = 0, size = 8) {
    try {
      const response = await axios.get(
        `${CUSTOMER_API_BASE}/featured-products`,
        {
          params: { page, size },
        },
      );
      return response;
    } catch (error) {
      console.error("Lỗi lấy sản phẩm nổi bật:", error);
      throw error;
    }
  },

  /**
   * Lấy sản phẩm theo danh mục
   * @param {Number} categoryId - ID danh mục
   * @param {Number} page - Trang
   * @param {Number} size - Số lượng
   */
  async getProductsByCategory(categoryId, page = 0, size = 12) {
    try {
      const response = await axios.get(
        `${CUSTOMER_API_BASE}/categories/${categoryId}/products`,
        {
          params: { page, size },
        },
      );
      return response;
    } catch (error) {
      console.error(`Lỗi lấy sản phẩm theo danh mục ${categoryId}:`, error);
      throw error;
    }
  },

  /**
   * Lấy chi tiết sản phẩm cho trang customer
   * @param {Number} productId - ID sản phẩm
   */
  async getCustomerProductDetail(productId) {
    try {
      const response = await axios.get(
        `${CUSTOMER_API_BASE}/products/${productId}`,
      );
      return response;
    } catch (error) {
      console.error(`Lỗi lấy chi tiết sản phẩm ID ${productId}:`, error);
      throw error;
    }
  },

  // ==================== COMBO (Lấy tất cả dữ liệu cần thiết) ====================

  /**
   * Lấy tất cả dữ liệu cần thiết cho form sản phẩm
   * Trả về: categories, colors, sizes, promotions
   */
  async getFormData() {
    try {
      const [categories, colors, sizes, promotions] = await Promise.all([
        this.getActiveCategories(),
        this.getActiveColors(),
        this.getActiveSizes(),
        this.getActivePromotions(),
      ]);

      return {
        categories: categories.data || [],
        colors: colors.data || [],
        sizes: sizes.data || [],
        promotions: promotions.data || [],
      };
    } catch (error) {
      console.error("Lỗi lấy dữ liệu form:", error);
      throw error;
    }
  },
};

export default productService;
