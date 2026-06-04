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
    isAdmin: (state) => {
      if (!state.user) return false;
      const role = state.user.role || state.user.roles;
      if (Array.isArray(role)) {
        return role.some(r => r === 'ADMIN' || r === 'admin' || r.name === 'ADMIN');
      }
      return role === 'ADMIN' || role === 'admin' || role === 'staff' || role === 'STAFF';
    }
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
