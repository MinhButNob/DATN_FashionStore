import { employeeApi } from '@/api/employeeApi'

function parseError(error) {
  const data = error.response?.data

  // ✅ THÊM dòng này để bắt lỗi 405
  if (error.response?.status === 405) {
    return `Phương thức không được hỗ trợ. Vui lòng kiểm tra lại.`
  }

  // Bắt lỗi validation từ BE trả về dạng { field: message, field: message }
  if (data && typeof data === 'object' && !data.error && !data.message) {
    const messages = Object.values(data).filter(Boolean)
    if (messages.length > 0) {
      return messages.join(' | ')
    }
  }

  // Bắt lỗi BE trả về dạng { error: "..." } hoặc { message: "..." }
  const raw = data?.error || data?.message || data || ''
  const msg = typeof raw === 'string' ? raw : JSON.stringify(raw)

  if (msg.includes('user_id_fkey') || msg.includes('user_id')) {
    return 'User ID không tồn tại trong hệ thống. Vui lòng kiểm tra lại.'
  }
  if (msg.includes('duplicate') || msg.includes('unique') || msg.includes('Duplicate')) {
    return 'Email hoặc số điện thoại đã được sử dụng.'
  }
  if (msg.includes('not-null') || msg.includes('null value')) {
    return 'Vui lòng điền đầy đủ thông tin bắt buộc.'
  }
  if (msg.includes('foreign key') || msg.includes('violates')) {
    return 'Dữ liệu liên kết không hợp lệ. Vui lòng kiểm tra lại.'
  }

  // Trả về message gốc từ BE nếu có, thay vì bỏ qua
  if (msg && msg !== '{}') return msg

  return null
}

// Phần còn lại giữ nguyên...
export const employeeService = {
  async createEmployee(data) {
    try {
      const response = await employeeApi.create(data)
      return { success: true, data: response.data }
    } catch (error) {
      console.log('Create error:', error.response?.data)
      return {
        success: false,
        error: parseError(error) || 'Thêm nhân viên thất bại. Vui lòng thử lại.'
      }
    }
  },

  async updateEmployee(id, data) {
    try {
      const response = await employeeApi.update(id, data)
      return { success: true, data: response.data }
    } catch (error) {
      console.log('Update error:', error.response?.data)
      return {
        success: false,
        error: parseError(error) || 'Cập nhật nhân viên thất bại. Vui lòng thử lại.'
      }
    }
  },

  async deleteEmployee(id) {
    try {
      const response = await employeeApi.delete(id)
      return { success: true, message: response.data?.message || 'Xoá thành công' }
    } catch (error) {
      return {
        success: false,
        error: parseError(error) || 'Xoá thất bại. Vui lòng thử lại.'
      }
    }
  },

  async getById(id) {
    try {
      const response = await employeeApi.getById(id)
      return { success: true, data: response.data }
    } catch (error) {
      return {
        success: false,
        error: parseError(error) || 'Không tìm thấy nhân viên.'
      }
    }
  },

  async searchEmployees(params) {
    try {
      const response = await employeeApi.search(params)
      return { success: true, data: response.data }
    } catch (error) {
      return {
        success: false,
        error: parseError(error) || 'Tìm kiếm thất bại.'
      }
    }
  },

  async getStatistics() {
    try {
      const response = await employeeApi.getStatistics()
      return { success: true, data: response.data }
    } catch (error) {
      return {
        success: false,
        error: parseError(error) || 'Lấy thống kê thất bại.'
      }
    }
  }
}