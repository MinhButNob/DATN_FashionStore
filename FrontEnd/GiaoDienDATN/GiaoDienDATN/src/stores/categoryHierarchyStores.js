// src/stores/categoryHierarchyStores.js
// PINIA STORE - quản lý state (tree, loading, error) cho tính năng Phân cấp danh mục.
// Component (Admincategoryhierarchy.vue, Categorymegamenu.vue...) sẽ gọi qua STORE này.
//
// Luồng: Component -> Store -> Service -> Api -> Backend

import { defineStore } from "pinia";
import categoryHierarchyService from "@/services/categoryHierarchyService";

// Fallback client-side: nếu backend CHƯA kịp trả field "categoryIsLocked",
// vẫn nhận diện được 3 danh mục lớn mặc định qua tên (chỉ áp dụng cho
// danh mục cấp 1 - parentCategoryId null). Khi backend đã trả đúng field
// categoryIsLocked thì điều kiện fallback này không còn ảnh hưởng gì
// (item.categoryIsLocked sẽ được ưu tiên kiểm tra trước).
const DEFAULT_LOCKED_NAMES = ["Áo", "Quần", "Phụ kiện"];

export const useCategoryHierarchyStore = defineStore("categoryHierarchy", {
  state: () => ({
    tree: [],
    activeTree: [],
    parentOptions: [],
    loading: false,
    saving: false,
    error: null }),

  getters: {
    hasChildren: (state) => (categoryId) => {
      const parent = state.tree.find((p) => p.categoryId === categoryId);
      return !!(parent && parent.children && parent.children.length > 0);
    },

    // Kiểm tra 1 danh mục có phải danh mục lớn mặc định (khóa cứng) không
    isLockedCategory: () => (item) => {
      if (!item) return false;
      if (item.categoryIsLocked) return true;
      return (
        (item.parentCategoryId === null || item.parentCategoryId === undefined) &&
        DEFAULT_LOCKED_NAMES.includes(item.categoryName)
      );
    },

    totalParents: (state) => state.tree.length },

  actions: {
    async loadTree() {
      this.loading = true;
      this.error = null;
      try {
        this.tree = await categoryHierarchyService.getCategoryTree();
      } catch (error) {
        this.error = "Không tải được cây danh mục";
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async loadActiveTree() {
      this.loading = true;
      this.error = null;
      try {
        this.activeTree = await categoryHierarchyService.getActiveCategoryTree();
      } catch (error) {
        this.error = "Không tải được danh mục";
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async loadParentOptions() {
      try {
        this.parentOptions = await categoryHierarchyService.getParentCategories();
      } catch (error) {
        this.error = "Không tải được danh sách danh mục cha";
        throw error;
      }
    },

    async createCategory(payload) {
      this.saving = true;
      try {
        const result = await categoryHierarchyService.createCategory(payload);
        await this.loadTree();
        return result;
      } finally {
        this.saving = false;
      }
    },

    async updateCategory(id, payload) {
      this.saving = true;
      try {
        const result = await categoryHierarchyService.updateCategory(id, payload);
        await this.loadTree();
        return result;
      } finally {
        this.saving = false;
      }
    },

    async deleteCategory(id) {
      const result = await categoryHierarchyService.deleteCategory(id);
      await this.loadTree();
      return result;
    },

    async restoreCategory(id) {
      const result = await categoryHierarchyService.restoreCategory(id);
      await this.loadTree();
      return result;
    } } });