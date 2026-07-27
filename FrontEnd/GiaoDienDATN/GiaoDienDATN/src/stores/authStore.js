import { defineStore } from "pinia";
import api from "../api/axios";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: JSON.parse(localStorage.getItem("user")) || null,

    token: localStorage.getItem("accessToken") || null }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    userRoles: (state) => {
      if (!state.user) return [];

      const roles = state.user.roles || state.user.role || state.user.authorities || [];
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
    } },

  actions: {
    // Lưu lại session (token + user) sau khi login/social login thành công
    _setSession(data) {
      this.token = data.accessToken;
      this.user = data.user;

      localStorage.setItem("accessToken", data.accessToken);
      if (data.refreshToken) {
        localStorage.setItem("refreshToken", data.refreshToken);
      }
      localStorage.setItem("user", JSON.stringify(data.user));
    },

    async register(data) {
      const response = await api.post("/auth/register", data);

      return response.data;
    },

    async login(email, password) {
      const response = await api.post("/auth/login", {
        email,
        password });

      const data = response.data;
      this._setSession(data);

      return data.user;
    },

    async googleLogin(idToken) {
      const response = await api.post("/auth/google", {
        idToken });

      const data = response.data;
      this._setSession(data);

      return data.user;
    },

    async facebookLogin(accessToken) {
      const response = await api.post("/auth/facebook", {
        accessToken });

      const data = response.data;
      this._setSession(data);

      return data.user;
    },

    logout() {
      this.user = null;
      this.token = null;

      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
    } } });