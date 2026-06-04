import { defineStore } from 'pinia'
import { employeeService } from '@/services/employeeService'

export const useEmployeeStore = defineStore('employee', {
  state: () => ({
    employees: [],
    statistics: {
      totalEmployees: 0,
      activeEmployees: 0
    },
    pagination: {
      currentPage: 0,
      pageSize: 10,
      totalElements: 0,
      totalPages: 0
    },
    currentFilters: {
      keyword: '',
      status: '',
      page: 0,
      size: 10,
      sortBy: 'employeeId',
      direction: 'desc'
    },
    loading: false,
    error: null
  }),

  getters: {
    getEmployees: (state) => state.employees,
    getPagination: (state) => state.pagination,
    getStatistics: (state) => state.statistics,
    isLoading: (state) => state.loading
  },

  actions: {
    async searchEmployees({ keyword = '', status = '', page = 0, size = 10, sortBy = 'employeeId', direction = 'desc' }) {
      this.loading = true
      
      // Lưu lại filters hiện tại
      this.currentFilters = { keyword, status, page, size, sortBy, direction }
      
      try {
        const result = await employeeService.searchEmployees({
          keyword: keyword || null,
          status: status || null,
          page,
          size,
          sortBy,
          direction
        })

        if (result.success) {
          const data = result.data
          if (data && data.content) {
            this.employees = data.content
            this.pagination = {
              currentPage: data.number || 0,
              pageSize: data.size || 10,
              totalElements: data.totalElements || 0,
              totalPages: data.totalPages || 0
            }
          } else if (Array.isArray(data)) {
            this.employees = data
            this.pagination = {
              currentPage: 0,
              pageSize: data.length,
              totalElements: data.length,
              totalPages: 1
            }
          } else {
            this.employees = []
          }
          this.error = null
        } else {
          this.error = result.error
          this.employees = []
        }
      } catch (error) {
        this.error = 'Lỗi tải danh sách nhân viên'
        this.employees = []
      } finally {
        this.loading = false
      }
    },

    // ✅ Thêm action để reload lại trang hiện tại
    async reloadCurrentPage() {
      await this.searchEmployees(this.currentFilters)
    },

    async createEmployee(data) {
      try {
        const result = await employeeService.createEmployee(data)
        if (result.success) {
          // Reload lại trang hiện tại sau khi tạo thành công
          await this.reloadCurrentPage()
          await this.loadStatistics()
          return { success: true, data: result.data }
        }
        return { success: false, error: result.error }
      } catch (error) {
        return { success: false, error: 'Lỗi tạo nhân viên' }
      }
    },

    async updateEmployee(id, data) {
      try {
        const result = await employeeService.updateEmployee(id, data)
        if (result.success) {
          // Reload lại trang hiện tại sau khi cập nhật thành công
          await this.reloadCurrentPage()
          await this.loadStatistics()
          return { success: true, data: result.data }
        }
        return { success: false, error: result.error }
      } catch (error) {
        return { success: false, error: 'Lỗi cập nhật nhân viên' }
      }
    },

    async deleteEmployee(id) {
      try {
        const result = await employeeService.deleteEmployee(id)
        if (result.success) {
          // Reload lại trang hiện tại sau khi xóa thành công
          await this.reloadCurrentPage()
          await this.loadStatistics()
          return { success: true }
        }
        return { success: false, error: result.error }
      } catch (error) {
        return { success: false, error: 'Lỗi xoá nhân viên' }
      }
    },

    async updateEmployeeStatus(id, status) {
      const employeeResult = await employeeService.getById(id)
      if (!employeeResult.success) {
        return { success: false, error: 'Không tìm thấy nhân viên' }
      }

      const employee = employeeResult.data
      const updateData = {
        userId: employee.userId,
        employeeFullname: employee.employeeFullname,
        employeeGender: employee.employeeGender,
        employeeAge: employee.employeeAge,
        employeePhone: employee.employeePhone,
        employeeEmail: employee.employeeEmail,
        employeePosition: employee.employeePosition,
        employeeStatus: status
      }
      return await this.updateEmployee(id, updateData)
    },

    async loadStatistics() {
      try {
        const result = await employeeService.getStatistics()
        if (result.success) {
          this.statistics = {
            totalEmployees: result.data.totalEmployees || 0,
            activeEmployees: result.data.activeEmployees || 0
          }
        }
      } catch (error) {
        console.error('Lỗi tải thống kê:', error)
      }
    }
  }
})