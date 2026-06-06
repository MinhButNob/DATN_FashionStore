import { defineStore } from "pinia";
import { roleApi } from "../api/roleApi";

export const useRoleStore = defineStore("role", {
  state: () => ({
    roles: [],
    allRoles: [],
    loading: false,
    error: null,
    pagination: {
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      pageSize: 10,
    },
  }),

  actions: {
    async fetchRoles(page = 0, size = 10, status = null) {
      this.loading = true;
      try {
        const response = await roleApi.getAll(page, size, status);
        this.roles = response.data.content;
        this.pagination = {
          currentPage: response.data.number,
          totalPages: response.data.totalPages,
          totalElements: response.data.totalElements,
          pageSize: response.data.size,
        };
        this.error = null;
      } catch (error) {
        this.error =
          error.response?.data?.message || "Lỗi khi lấy danh sách vai trò";
        throw this.error;
      } finally {
        this.loading = false;
      }
    },

    async fetchAllRoles() {
      this.loading = true;
      try {
        const response = await roleApi.getAllNoParams();
        // Chỉ lấy các role chưa bị xóa (status !== 'DELETED')
        this.allRoles = response.data.filter(
          (role) => role.status !== "DELETED",
        );
        this.error = null;
      } catch (error) {
        this.error =
          error.response?.data?.message || "Lỗi khi lấy tất cả vai trò";
        throw this.error;
      } finally {
        this.loading = false;
      }
    },

    async getRoleById(id) {
      try {
        const response = await roleApi.getById(id);
        // Kiểm tra nếu role đã bị xóa
        if (response.data.status === "DELETED") {
          throw new Error("Vai trò đã bị xóa");
        }
        return response.data;
      } catch (error) {
        this.error =
          error.response?.data?.message || "Lỗi khi lấy thông tin vai trò";
        throw this.error;
      }
    },

    async createRole(roleData) {
      try {
        // Đảm bảo status không phải DELETED khi tạo mới
        const payload = {
          roleName: roleData.roleName,
          status:
            roleData.status === "DELETED"
              ? "ACTIVE"
              : roleData.status || "ACTIVE",
        };
        const response = await roleApi.create(payload);
        await this.fetchRoles(
          this.pagination.currentPage,
          this.pagination.pageSize,
        );
        return response.data;
      } catch (error) {
        this.error = error.response?.data?.message || "Lỗi khi thêm vai trò";
        throw this.error;
      }
    },

    async updateRole(id, roleData) {
      try {
        // Không cho phép cập nhật role đã bị xóa
        const currentRole = await roleApi.getById(id);
        if (currentRole.data.status === "DELETED") {
          throw new Error("Không thể cập nhật vai trò đã bị xóa");
        }

        const payload = {
          roleName: roleData.roleName,
          status: roleData.status,
        };
        const response = await roleApi.update(id, payload);
        await this.fetchRoles(
          this.pagination.currentPage,
          this.pagination.pageSize,
        );
        return response.data;
      } catch (error) {
        this.error =
          error.response?.data?.message || "Lỗi khi cập nhật vai trò";
        throw this.error;
      }
    },

    async deleteRole(id) {
      // ✅ Kiểm tra ID
      if (!id) {
        throw new Error("ID không hợp lệ");
      }

      try {
        // ✅ Gọi API xóa mềm (chuyển status thành DELETED)
        const response = await roleApi.delete(id);

        // ✅ Refresh lại danh sách
        await this.fetchRoles(
          this.pagination.currentPage,
          this.pagination.pageSize,
          this.filterStatus,
        );
        await this.fetchAllRoles();

        return response.data;
      } catch (error) {
        this.error = error.response?.data?.message || "Lỗi khi xóa vai trò";
        throw this.error;
      }
    },

    // Thêm action khôi phục role đã xóa
    async restoreRole(id) {
      try {
        const response = await roleApi.restore(id);
        await this.fetchRoles(
          this.pagination.currentPage,
          this.pagination.pageSize,
        );
        await this.fetchAllRoles();
        return response.data;
      } catch (error) {
        this.error =
          error.response?.data?.message || "Lỗi khi khôi phục vai trò";
        throw this.error;
      }
    },

    async searchRoles(keyword, page = 0, size = 10) {
      this.loading = true;
      try {
        const response = await roleApi.search(keyword, page, size);
        // Lọc bỏ các role đã bị xóa
        const filteredContent = response.data.content.filter(
          (role) => role.status !== "DELETED",
        );
        this.roles = filteredContent;
        this.pagination = {
          currentPage: response.data.number,
          totalPages: response.data.totalPages,
          totalElements: filteredContent.length,
          pageSize: response.data.size,
        };
        this.error = null;
      } catch (error) {
        this.error =
          error.response?.data?.message || "Lỗi khi tìm kiếm vai trò";
        throw this.error;
      } finally {
        this.loading = false;
      }
    },

    // Lấy danh sách role đã bị xóa
    async getDeletedRoles(page = 0, size = 10) {
      this.loading = true;
      try {
        const response = await roleApi.getByStatus("DELETED", page, size);
        this.roles = response.data.content;
        this.pagination = {
          currentPage: response.data.number,
          totalPages: response.data.totalPages,
          totalElements: response.data.totalElements,
          pageSize: response.data.size,
        };
        this.error = null;
      } catch (error) {
        this.error =
          error.response?.data?.message ||
          "Lỗi khi lấy danh sách vai trò đã xóa";
        throw this.error;
      } finally {
        this.loading = false;
      }
    },

    // Xóa cứng (nếu cần)
    async permanentDeleteRole(id) {
      try {
        await roleApi.permanentDelete(id);
        await this.fetchRoles(
          this.pagination.currentPage,
          this.pagination.pageSize,
        );
        await this.fetchAllRoles();
      } catch (error) {
        this.error =
          error.response?.data?.message || "Lỗi khi xóa vĩnh viễn vai trò";
        throw this.error;
      }
    },

    clearError() {
      this.error = null;
    },
  },

  // Thêm getters để lọc dữ liệu
  getters: {
    activeRoles: (state) => {
      return state.roles.filter((role) => role.status === "ACTIVE");
    },
    inactiveRoles: (state) => {
      return state.roles.filter((role) => role.status === "INACTIVE");
    },
    deletedRoles: (state) => {
      return state.roles.filter((role) => role.status === "DELETED");
    },
    activeCount: (state) => {
      return state.roles.filter((role) => role.status === "ACTIVE").length;
    },
    totalCount: (state) => {
      return state.pagination.totalElements;
    },
  },
});
