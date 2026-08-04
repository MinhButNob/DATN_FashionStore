import { defineStore } from "pinia";
import api from "../api/axios";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: JSON.parse(localStorage.getItem("user")) || null,
    token: localStorage.getItem("accessToken") || null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    userRoles: (state) => {
      if (!state.user) return [];
      const roles =
        state.user.roles || state.user.role || state.user.authorities || [];
      const normalizedRoles = Array.isArray(roles) ? roles : [roles];
      return normalizedRoles
        .map((role) => {
          if (typeof role === "string") return role;
          return role?.name || role?.roleName || role?.authority || "";
        })
        .map((role) => role.replace("ROLE_", "").toUpperCase())
        .filter(Boolean);
    },
    isAdmin() {
      return this.userRoles.includes("ADMIN");
    },
    isStaff() {
      return this.userRoles.includes("STAFF");
    },
    isCustomer() {
      return this.userRoles.includes("CUSTOMER");
    },
    primaryRole() {
      if (this.userRoles.includes("ADMIN")) return "ADMIN";
      if (this.userRoles.includes("STAFF")) return "STAFF";
      if (this.userRoles.includes("CUSTOMER")) return "CUSTOMER";
      return null;
    },
  },

  actions: {
    // ✅ SỬA: Lưu thêm username và email vào localStorage
    _setSession(data) {
      this.token = data.accessToken;
      this.user = data.user;

      // Lưu token
      localStorage.setItem("accessToken", data.accessToken);
      if (data.refreshToken) {
        localStorage.setItem("refreshToken", data.refreshToken);
      }

      // Lưu user
      localStorage.setItem("user", JSON.stringify(data.user));

      // 🔥 QUAN TRỌNG: Lưu thêm username và email để dùng cho so sánh
      const username =
        data.user?.username || data.user?.fullName || data.user?.email || "";
      const email = data.user?.email || "";
      const fullName = data.user?.fullName || data.user?.username || email;

      localStorage.setItem("username", username);
      localStorage.setItem("userName", fullName);
      localStorage.setItem("userEmail", email);
      localStorage.setItem("email", email);
      localStorage.setItem("role", this.primaryRole || "");

      console.log("✅ _setSession - Saved user info:", {
        username: localStorage.getItem("username"),
        userEmail: localStorage.getItem("userEmail"),
        role: localStorage.getItem("role"),
      });
    },

    async register(data) {
      const response = await api.post("/auth/register", data);
      return response.data;
    },

    async login(email, password) {
      const response = await api.post("/auth/login", {
        email,
        password,
      });

      const data = response.data;
      this._setSession(data);

      return data.user;
    },

    async googleLogin(idToken) {
      const response = await api.post("/auth/google", {
        idToken,
      });

      const data = response.data;
      this._setSession(data);

      return data.user;
    },

    async facebookLogin(accessToken) {
      const response = await api.post("/auth/facebook", {
        accessToken,
      });

      const data = response.data;
      this._setSession(data);

      return data.user;
    },

    logout() {
      this.user = null;
      this.token = null;

      // Xóa tất cả localStorage
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
      localStorage.removeItem("username");
      localStorage.removeItem("userName");
      localStorage.removeItem("userEmail");
      localStorage.removeItem("email");
      localStorage.removeItem("role");
      localStorage.removeItem("rememberMe");
    },
  },
});
