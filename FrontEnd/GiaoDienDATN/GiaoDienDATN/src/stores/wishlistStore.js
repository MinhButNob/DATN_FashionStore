import { defineStore } from "pinia";
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

export const useWishlistStore = defineStore("wishlist", {
  state: () => ({
    items: [],
    loading: false,
    loaded: false,
  }),

  getters: {
    totalItems: (state) => state.items.length,
    productDetailIds: (state) =>
      new Set(state.items.map((item) => item.productDetailId)),
  },

  actions: {
    getToken() {
      return (
        localStorage.getItem("token") || localStorage.getItem("accessToken")
      );
    },

    isInWishlist(productDetailId) {
      return this.items.some(
        (item) => item.productDetailId === productDetailId,
      );
    },

    async fetchWishlist() {
      const token = this.getToken();
      if (!token) {
        this.items = [];
        this.loaded = true;
        return;
      }

      this.loading = true;
      try {
        const response = await axios.get(`${API_BASE}/wishlist`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        if (response.data && response.data.data) {
          this.items = response.data.data.items || [];
        }
        this.loaded = true;
      } catch (error) {
        console.error("❌ Fetch wishlist error:", error);
      } finally {
        this.loading = false;
      }
    },

    async addToWishlist(productDetailId) {
      const token = this.getToken();
      if (!token) {
        throw new Error("Vui lòng đăng nhập để thêm vào yêu thích");
      }

      const response = await axios.post(
        `${API_BASE}/wishlist/add`,
        { productDetailId },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        },
      );

      if (response.data && response.data.data) {
        this.items = response.data.data.items || [];
      }
      return response.data;
    },

    async removeFromWishlist(productDetailId) {
      const token = this.getToken();
      if (!token) {
        throw new Error("Vui lòng đăng nhập");
      }

      const response = await axios.delete(
        `${API_BASE}/wishlist/items/${productDetailId}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      if (response.data && response.data.data) {
        this.items = response.data.data.items || [];
      }
      return response.data;
    },

    async toggleWishlist(productDetailId) {
      if (this.isInWishlist(productDetailId)) {
        await this.removeFromWishlist(productDetailId);
        return false;
      } else {
        await this.addToWishlist(productDetailId);
        return true;
      }
    },

    reset() {
      this.items = [];
      this.loaded = false;
    },
  },
});