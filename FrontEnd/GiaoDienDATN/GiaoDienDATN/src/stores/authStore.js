import { defineStore } from "pinia";
import axios from "axios";

const API_URL = "http://localhost:8080/api/auth";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: JSON.parse(localStorage.getItem("user")) || null,
    token: localStorage.getItem("token") || null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
  },

  actions: {
    async login(email, password) {
      const response = await axios.post(`${API_URL}/login`, {
        email,
        password,
      });

      const data = response.data;

      // LƯU TOKEN
      this.token = data.accessToken || data.token;

      // LƯU USER
      this.user = data.user;

      // localStorage
      localStorage.setItem("token", this.token);

      localStorage.setItem("user", JSON.stringify(this.user));

      // gắn Authorization header
      axios.defaults.headers.common["Authorization"] = `Bearer ${this.token}`;

      return this.user;
    },

    async register(registerData) {
      const response = await axios.post(`${API_URL}/register`, registerData);

      return response.data;
    },

    logout() {
      this.user = null;
      this.token = null;

      localStorage.removeItem("token");
      localStorage.removeItem("user");

      delete axios.defaults.headers.common["Authorization"];
    },
  },
});
