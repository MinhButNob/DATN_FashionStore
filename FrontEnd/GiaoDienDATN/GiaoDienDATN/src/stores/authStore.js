import { defineStore } from "pinia";
import axios from "axios";

const API = "http://localhost:8080/api/auth";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: JSON.parse(localStorage.getItem("user")) || null,

    token: localStorage.getItem("token") || null,
  }),

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
    },
  },

  actions: {
    async register(data) {
      const response = await axios.post(`${API}/register`, data);

      return response.data;
    },

    async login(email, password) {
      const response = await axios.post(`${API}/login`, {
        email,
        password,
      });

      const data = response.data;

      this.token = data.accessToken;

      this.user = data.user;

      localStorage.setItem("token", data.accessToken);

      localStorage.setItem("user", JSON.stringify(data.user));

      return data.user;
    },

    async googleLogin(idToken) {
      const response = await axios.post(`${API}/google`, {
        idToken,
      });

      const data = response.data;

      this.token = data.accessToken;

      this.user = data.user;

      localStorage.setItem("token", data.accessToken);

      localStorage.setItem("user", JSON.stringify(data.user));

      return data.user;
    },

    async facebookLogin(accessToken) {
      const response = await axios.post(`${API}/facebook`, {
        accessToken,
      });

      const data = response.data;

      this.token = data.accessToken;
      this.user = data.user;

      localStorage.setItem("token", data.accessToken);

      localStorage.setItem("user", JSON.stringify(data.user));

      return data.user;
    },

    logout() {
      this.user = null;
      this.token = null;

      localStorage.removeItem("token");

      localStorage.removeItem("user");
    },
  },
});
