import { defineStore } from "pinia";
import { employeeService } from "@/services/employeeService";
import { employeeApi } from "@/api/employeeApi";

export const useEmployeeStore = defineStore("employee", {
  state: () => ({
    employees: [],
    statistics: {
      totalEmployees: 0,
      activeEmployees: 0,
      lockedEmployees: 0,
      deletedEmployees: 0 },
    pagination: {
      currentPage: 0,
      pageSize: 10,
      totalElements: 0,
      totalPages: 0 },
    currentFilters: {
      keyword: "",
      status: "",
      page: 0,
      size: 10,
      sortBy: "employeeId",
      direction: "desc" },
    loading: false,
    error: null }),

  getters: {
    getEmployees: (state) => state.employees,
    getPagination: (state) => state.pagination,
    getStatistics: (state) => state.statistics,
    isLoading: (state) => state.loading },

  actions: {
    async searchEmployees({
      keyword = "",
      status = "",
      page = 0,
      size = 10,
      sortBy = "employeeId",
      direction = "desc" }) {
      this.loading = true;

      this.currentFilters = { keyword, status, page, size, sortBy, direction };

      try {
        const result = await employeeService.searchEmployees({
          keyword: keyword || null,
          status: status || null,
          page,
          size,
          sortBy,
          direction });

        if (result.success) {
          const data = result.data;
          if (data && data.content) {
            this.employees = data.content;
            this.pagination = {
              currentPage: data.number || 0,
              pageSize: data.size || 10,
              totalElements: data.totalElements || 0,
              totalPages: data.totalPages || 0 };
          } else if (Array.isArray(data)) {
            this.employees = data;
            this.pagination = {
              currentPage: 0,
              pageSize: data.length,
              totalElements: data.length,
              totalPages: 1 };
          } else {
            this.employees = [];
          }
          this.error = null;
        } else {
          this.error = result.error;
          this.employees = [];
        }
      } catch (error) {
        this.error = "Lỗi tải danh sách nhân viên";
        this.employees = [];
      } finally {
        this.loading = false;
      }
    },

    async reloadCurrentPage() {
      await this.searchEmployees(this.currentFilters);
    },

    async createEmployee(data) {
      try {
        const result = await employeeService.createEmployee(data);
        if (result.success) {
          await this.reloadCurrentPage();
          await this.loadStatistics();
          return { success: true, data: result.data };
        }
        return { success: false, error: result.error };
      } catch (error) {
        return { success: false, error: "Lỗi tạo nhân viên" };
      }
    },

    async updateEmployee(id, data) {
      try {
        const result = await employeeService.updateEmployee(id, data);
        if (result.success) {
          await this.reloadCurrentPage();
          await this.loadStatistics();
          return { success: true, data: result.data };
        }
        return { success: false, error: result.error };
      } catch (error) {
        return { success: false, error: "Lỗi cập nhật nhân viên" };
      }
    },

    async deleteEmployee(id) {
      try {
        const result = await employeeService.deleteEmployee(id);
        if (result.success) {
          await this.reloadCurrentPage();
          await this.loadStatistics();
          return { success: true };
        }
        return { success: false, error: result.error };
      } catch (error) {
        return { success: false, error: "Lỗi xoá nhân viên" };
      }
    },

    // ❌ BỎ updateEmployeeStatus - không dùng nữa

    async toggleLockEmployee(id) {
      try {
        const result = await employeeApi.toggleLock(id);
        if (result.status === 200) {
          await this.reloadCurrentPage();
          await this.loadStatistics();
          return { success: true };
        }
        return { success: false, error: result.data?.error };
      } catch (error) {
        return {
          success: false,
          error: error.response?.data?.error || "Lỗi thao tác" };
      }
    },

    async softDeleteEmployee(id) {
      try {
        const result = await employeeApi.delete(id);
        if (result.status === 200) {
          await this.reloadCurrentPage();
          await this.loadStatistics();
          return { success: true };
        }
        return { success: false, error: result.data?.error };
      } catch (error) {
        return {
          success: false,
          error: error.response?.data?.error || "Lỗi xóa nhân viên" };
      }
    },

    async restoreEmployee(id) {
      try {
        const result = await employeeApi.restore(id);
        if (result.status === 200) {
          await this.reloadCurrentPage();
          await this.loadStatistics();
          return { success: true };
        }
        return { success: false, error: result.data?.error };
      } catch (error) {
        return {
          success: false,
          error: error.response?.data?.error || "Lỗi khôi phục" };
      }
    },

    async loadStatistics() {
      try {
        const result = await employeeService.getStatistics();
        if (result.success) {
          this.statistics = {
            totalEmployees: result.data.totalEmployees || 0,
            activeEmployees: result.data.activeEmployees || 0,
            lockedEmployees: result.data.lockedEmployees || 0,
            deletedEmployees: result.data.deletedEmployees || 0 };
        }
      } catch (error) {
        console.error("Lỗi tải thống kê:", error);
      }
    } } });
