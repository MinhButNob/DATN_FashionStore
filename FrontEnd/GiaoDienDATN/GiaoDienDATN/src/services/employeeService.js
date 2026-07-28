import { employeeApi } from "@/api/employeeApi";

function parseError(error) {
  const data = error.response?.data;

  if (error.response?.status === 405) {
    return `Phương thức không được hỗ trợ. Vui lòng kiểm tra lại.`;
  }

  if (data && typeof data === "object" && !data.error && !data.message) {
    const messages = Object.values(data).filter(Boolean);
    if (messages.length > 0) {
      return messages.join(" | ");
    }
  }

  const raw = data?.error || data?.message || data || "";
  const msg = typeof raw === "string" ? raw : JSON.stringify(raw);

  if (msg.includes("user_id_fkey") || msg.includes("user_id")) {
    return "User ID không tồn tại trong hệ thống. Vui lòng kiểm tra lại.";
  }
  if (
    msg.includes("duplicate") ||
    msg.includes("unique") ||
    msg.includes("Duplicate")
  ) {
    return "Email hoặc số điện thoại đã được sử dụng.";
  }
  if (msg.includes("not-null") || msg.includes("null value")) {
    return "Vui lòng điền đầy đủ thông tin bắt buộc.";
  }
  if (msg.includes("foreign key") || msg.includes("violates")) {
    return "Dữ liệu liên kết không hợp lệ. Vui lòng kiểm tra lại.";
  }

  if (msg && msg !== "{}") return msg;

  return null;
}

export const employeeService = {
  async createEmployee(data) {
    try {
      const response = await employeeApi.create(data);
      return { success: true, data: response.data };
    } catch (error) {
      console.log("Create error:", error.response?.data);
      return {
        success: false,
        error:
          parseError(error) || "Thêm nhân viên thất bại. Vui lòng thử lại." };
    }
  },

  // async updateEmployee(id, data) {
  //   try {
  //     const response = await employeeApi.update(id, data);
  //     return { success: true, data: response.data };
  //   } catch (error) {
  //     console.log("Update error:", error.response?.data);
  //     return {
  //       success: false,
  //       error:
  //         parseError(error) || "Cập nhật nhân viên thất bại. Vui lòng thử lại.",
  //     };
  //   }
  // },

  async updateEmployee(id, data) {
    try {
      // Nếu có userStatus thì gọi API toggleLock riêng
      if (data.userStatus) {
        // Gọi API toggleLock nếu userStatus thay đổi
        const toggleResult = await employeeApi.toggleLock(id);
        if (toggleResult.status !== 200) {
          return {
            success: false,
            error: "Không thể cập nhật trạng thái tài khoản" };
        }
        // Xóa userStatus khỏi data để không gửi lên update thông thường
        delete data.userStatus;
      }

      const response = await employeeApi.update(id, data);
      return { success: true, data: response.data };
    } catch (error) {
      console.log("Update error:", error.response?.data);
      return {
        success: false,
        error:
          parseError(error) || "Cập nhật nhân viên thất bại. Vui lòng thử lại." };
    }
  },

  async deleteEmployee(id) {
    try {
      const response = await employeeApi.delete(id);
      return {
        success: true,
        message: response.data?.message || "Xoá thành công" };
    } catch (error) {
      return {
        success: false,
        error: parseError(error) || "Xoá thất bại. Vui lòng thử lại." };
    }
  },

  async getById(id) {
    try {
      const response = await employeeApi.getById(id);
      return { success: true, data: response.data };
    } catch (error) {
      return {
        success: false,
        error: parseError(error) || "Không tìm thấy nhân viên." };
    }
  },

  async searchEmployees(params) {
    try {
      const response = await employeeApi.search(params);
      return { success: true, data: response.data };
    } catch (error) {
      return {
        success: false,
        error: parseError(error) || "Tìm kiếm thất bại." };
    }
  },

  async getStatistics() {
    try {
      const response = await employeeApi.getStatistics();
      return { success: true, data: response.data };
    } catch (error) {
      return {
        success: false,
        error: parseError(error) || "Lấy thống kê thất bại." };
    }
  } };
