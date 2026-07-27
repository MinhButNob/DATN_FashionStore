import axios from "axios";

// ========== CẤU HÌNH BASE URL ==========
// Khi deploy online: tạo file .env (hoặc .env.production) ở root FE với:
//   VITE_API_BASE_URL=https://api.yourdomain.com/api
// Khi chạy local mà không set biến này, sẽ tự fallback về localhost.
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

// ========== REQUEST INTERCEPTOR ==========
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Nếu gửi FormData thì xoá Content-Type để browser tự set kèm boundary
    if (config.data instanceof FormData) {
      delete config.headers["Content-Type"];
    }

    return config;
  },
  (error) => Promise.reject(error),
);

// ========== RESPONSE INTERCEPTOR (auto refresh accessToken) ==========
let isRefreshing = false;
let refreshSubscribers = [];

const onRefreshed = (newToken) => {
  refreshSubscribers.forEach((cb) => cb(newToken));
  refreshSubscribers = [];
};

const logout = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
  if (!window.location.pathname.includes("/login")) {
    window.location.href = "/login";
  }
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Lỗi network / không có response (server sập, mất mạng...)
    if (!error.response) {
      return Promise.reject(error);
    }

    const { status } = error.response;

    // Không tự refresh cho chính các request auth (login/register/refresh-token)
    const isAuthRequest = originalRequest?.url?.includes("/auth/");

    if (status === 401 && !originalRequest._retry && !isAuthRequest) {
      if (isRefreshing) {
        // Đã có 1 request khác đang refresh -> đợi kết quả rồi gọi lại request này
        return new Promise((resolve, reject) => {
          refreshSubscribers.push((newToken) => {
            if (newToken) {
              originalRequest.headers.Authorization = `Bearer ${newToken}`;
              resolve(api(originalRequest));
            } else {
              reject(error);
            }
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = localStorage.getItem("refreshToken");
        if (!refreshToken) {
          throw new Error("Không có refresh token");
        }

        const { data } = await axios.post(
          `${API_BASE_URL}/auth/refresh-token`,
          { refreshToken },
        );

        const newAccessToken = data.accessToken;
        localStorage.setItem("accessToken", newAccessToken);

        isRefreshing = false;
        onRefreshed(newAccessToken);

        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        isRefreshing = false;
        onRefreshed(null);
        logout();
        return Promise.reject(refreshError);
      }
    }

    if (status === 403) {
      logout();
    }

    return Promise.reject(error);
  },
);

export default api;