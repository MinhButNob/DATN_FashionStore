// src/service/categoryhierarchyService.js
// LỚP SERVICE - xử lý logic nghiệp vụ, validate dữ liệu, xử lý lỗi.
// Gọi qua categoryHierarchyApi (KHÔNG tự gọi axios trực tiếp).
// Được dùng bởi: src/stores/categoryhierarchyStores.js
// (component KHÔNG gọi thẳng file này nữa, mà gọi qua store)

import categoryHierarchyApi from "@/api/categoryhierarchyApi";

const categoryHierarchyService = {
  /**
   * Lấy cây danh mục ĐẦY ĐỦ (ACTIVE + INACTIVE) - dùng cho trang ADMIN
   */
  async getCategoryTree() {
    try {
      const response = await categoryHierarchyApi.fetchTree();
      return response.data || [];
    } catch (error) {
      console.error("Lỗi lấy cây danh mục:", error);
      throw error;
    }
  },

  /**
   * Lấy cây danh mục CHỈ ACTIVE - dùng cho menu ngoài trang khách hàng
   */
  async getActiveCategoryTree() {
    try {
      const response = await categoryHierarchyApi.fetchActiveTree();
      return response.data || [];
    } catch (error) {
      console.error("Lỗi lấy cây danh mục active:", error);
      throw error;
    }
  },

  /**
   * Lấy danh sách danh mục cấp 1 (cha)
   */
  async getParentCategories() {
    try {
      const response = await categoryHierarchyApi.fetchParents();
      return response.data || [];
    } catch (error) {
      console.error("Lỗi lấy danh mục cấp 1:", error);
      throw error;
    }
  },

  /**
   * Lấy danh sách danh mục con theo id danh mục cha
   */
  async getChildren(parentId) {
    try {
      const response = await categoryHierarchyApi.fetchChildren(parentId);
      return response.data || [];
    } catch (error) {
      console.error(`Lỗi lấy danh mục con của cha ID ${parentId}:`, error);
      throw error;
    }
  },

  /**
   * Tạo danh mục mới (cấp 1 nếu không truyền parentCategoryId, cấp 2 nếu có)
   * Có validate cơ bản trước khi gọi API.
   */
  async createCategory(data) {
    if (!data?.categoryName?.trim()) {
      throw new Error("Tên danh mục không được để trống");
    }
    try {
      const response = await categoryHierarchyApi.create(data);
      return response.data;
    } catch (error) {
      console.error("Lỗi tạo danh mục:", error);
      throw error;
    }
  },

  /**
   * Cập nhật danh mục (bao gồm cả đổi/gỡ danh mục cha)
   */
  async updateCategory(id, data) {
    if (!id) {
      throw new Error("Thiếu ID danh mục cần cập nhật");
    }
    if (!data?.categoryName?.trim()) {
      throw new Error("Tên danh mục không được để trống");
    }
    try {
      const response = await categoryHierarchyApi.update(id, data);
      return response.data;
    } catch (error) {
      console.error(`Lỗi cập nhật danh mục ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Xóa mềm danh mục
   */
  async deleteCategory(id) {
    if (!id) {
      throw new Error("Thiếu ID danh mục cần xóa");
    }
    try {
      const response = await categoryHierarchyApi.remove(id);
      return response.data;
    } catch (error) {
      console.error(`Lỗi xóa danh mục ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Khôi phục danh mục đã xóa
   */
  async restoreCategory(id) {
    if (!id) {
      throw new Error("Thiếu ID danh mục cần khôi phục");
    }
    try {
      const response = await categoryHierarchyApi.restore(id);
      return response.data;
    } catch (error) {
      console.error(`Lỗi khôi phục danh mục ID ${id}:`, error);
      throw error;
    }
  } };

export default categoryHierarchyService;