import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    const token =
      localStorage.getItem("token") || localStorage.getItem("accessToken");
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



export const getCustomers = (page = 0, size = 10, keyword = '', sortBy = 'customerId', sortDir = 'asc', status = null) => {
    const params = { page, size, keyword, sortBy, sortDir }
    if (status) params.status = status
    return api.get('/admin/customers', { params })
}

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
  return api.get("/admin/promotions", {
    params: { page, size, status, keyword, sortBy, sortDir },
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
export default api;
