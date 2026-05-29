import { defineStore } from "pinia";
import authApi from "../api/authApi";

export const useAuthStore = defineStore("auth", {
  state: () => {
    // Try to load user from local storage safely
    let storedUser = null;
    try {
      const userStr = localStorage.getItem("user");
      if (userStr) {
        storedUser = JSON.parse(userStr);
      }
    } catch (e) {
      console.error("Failed to parse user from local storage", e);
    }
    
    return {
      user: storedUser,
      token: localStorage.getItem("accessToken") || null,
    };
  },

  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => {
      if (!state.user || !state.user.roles) return false;
      return state.user.roles.includes("ROLE_ADMIN") || state.user.roles.includes("ROLE_STAFF");
    }
  },

  actions: {
    // LOGIN
    async login(email, password) {
      const response = await authApi.login({
        email,
        password,
      });

      const token = response.data.accessToken;
      const user = response.data.user;

      localStorage.setItem("accessToken", token);
      localStorage.setItem("user", JSON.stringify(user));

      this.token = token;
      this.user = user;

      return this.user;
    },

    // REGISTER
    async register(data) {
      return await authApi.register(data);
    },

    // GOOGLE LOGIN
    async googleLogin(idToken) {
      const response = await authApi.googleLogin(idToken);

      const token = response.data.accessToken;
      const user = response.data.user;

      localStorage.setItem("accessToken", token);
      localStorage.setItem("user", JSON.stringify(user));

      this.token = token;
      this.user = user;

      return this.user;
    },

    // FACEBOOK LOGIN
    async facebookLogin(accessToken) {
      const response = await authApi.facebookLogin(accessToken);

      const token = response.data.accessToken;
      const user = response.data.user;

      localStorage.setItem("accessToken", token);
      localStorage.setItem("user", JSON.stringify(user));

      this.token = token;
      this.user = user;

      return this.user;
    },

    // LOGOUT
    logout() {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("user");
      this.user = null;
      this.token = null;
    },
  },
});
