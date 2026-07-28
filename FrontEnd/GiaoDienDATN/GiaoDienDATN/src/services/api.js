import axios from "axios";

// ⚠️ Trước đây file này dùng biến "VITE_API_URL" (SAI TÊN, không khớp với
// biến đã set trên Vercel là "VITE_API_BASE_URL") và fallback là chuỗi
// rỗng "" -> khi biến không tồn tại, mọi request tương đối (vd: "/chat/...")
// bị trình duyệt tự nối vào origin hiện tại (localhost:3000 lúc dev,
// hoặc domain Vercel lúc build) thay vì gọi sang backend thật.
// Đã đổi lại cho ĐÚNG và ĐỒNG BỘ với src/api/axios.js.
const api = axios.create({
  baseURL:
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Đồng bộ tên key với axios.js: ưu tiên "accessToken" (chuẩn mới),
    // vẫn đọc "token" để không phá vỡ session cũ nếu còn sót đâu đó.
    const token =
      localStorage.getItem("accessToken") || localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

// Response interceptor
api.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    if (error.response) {
      const status = error.response.status;

      if (status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");
        window.dispatchEvent(new CustomEvent("auth:unauthorized"));
      }

      if (status === 403) {
        console.error("Không có quyền truy cập");
      }
    }
    return Promise.reject(error);
  },
);
// ======================
// CUSTOMER APIS
// ======================

export const getCustomers = (
  page = 0,
  size = 10,
  keyword = "",
  sortBy = "customerId",
  sortDir = "asc",
  status = null,
) => {
  const params = { page, size, keyword, sortBy, sortDir };
  if (status) params.status = status;
  return api.get("/admin/customers", { params });
};

export const getCustomerById = (id) => {
  return api.get(`/admin/customers/${id}`);
};

export const createCustomer = (data) => {
  return api.post("/admin/customers", data);
};

export const updateCustomer = (id, data) => {
  return api.put(`/admin/customers/${id}`, data);
};

export const deleteCustomer = (id) => {
  return api.delete(`/admin/customers/${id}`);
};

export const toggleLockCustomer = (id) => {
  return api.patch(`/admin/customers/${id}/toggle-lock`);
};

export const getCustomerStatistics = () => {
  return api.get("/admin/customers/statistics");
};
// ======================
// PROMOTION APIS
// ======================

export const getPromotions = (
  page = 0,
  size = 10,
  status = "",
  keyword = "",
  sortBy = "promotionId",
  sortDir = "asc",
) => {
  const trimmedKeyword = keyword ? keyword.trim() : "";
  return api.get("/admin/promotions", {
    params: { page, size, status, keyword: trimmedKeyword, sortBy, sortDir },
  });
};

export const getDeletedPromotions = (page = 0, size = 10) => {
  return api.get("/admin/promotions/deleted", { params: { page, size } });
};

export const getPromotionById = (id) => {
  return api.get(`/admin/promotions/${id}`);
};

export const createPromotion = (data) => {
  return api.post("/admin/promotions", data);
};

export const updatePromotion = (id, data) => {
  return api.put(`/admin/promotions/${id}`, data);
};

export const softDeletePromotion = (id) => {
  return api.delete(`/admin/promotions/${id}`);
};

export const restorePromotion = (id) => {
  return api.patch(`/admin/promotions/${id}/restore`);
};

export const togglePromotionStatus = (id) => {
  return api.patch(`/admin/promotions/${id}/toggle-status`);
};

export const getPromotionStatistics = () => {
  return api.get("/admin/promotions/statistics");
};
// ======================
// VOUCHER APIS
// ======================

export const getVouchers = (
  page = 0,
  size = 10,
  status = "",
  keyword = "",
  sortBy = "voucherId",
  sortDir = "asc",
) => {
  const trimmedKeyword = keyword ? keyword.trim() : "";
  return api.get("/admin/vouchers", {
    params: { page, size, status, keyword: trimmedKeyword, sortBy, sortDir },
  });
};

export const getDeletedVouchers = (page = 0, size = 10) => {
  return api.get("/admin/vouchers/deleted", { params: { page, size } });
};

export const getVoucherById = (id) => {
  return api.get(`/admin/vouchers/${id}`);
};

export const createVoucher = (data) => {
  return api.post("/admin/vouchers", data);
};

export const updateVoucher = (id, data) => {
  return api.put(`/admin/vouchers/${id}`, data);
};

export const softDeleteVoucher = (id) => {
  return api.delete(`/admin/vouchers/${id}`);
};

export const restoreVoucher = (id) => {
  return api.patch(`/admin/vouchers/${id}/restore`);
};

export const toggleVoucherStatus = (id) => {
  return api.patch(`/admin/vouchers/${id}/toggle-status`);
};

export const getVoucherStatistics = () => {
  return api.get("/admin/vouchers/statistics");
};

export default api;