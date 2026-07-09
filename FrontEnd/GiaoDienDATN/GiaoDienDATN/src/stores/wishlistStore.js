import { defineStore } from "pinia";
import axios from "axios";
import { useAuthStore } from "./authStore";

const API_BASE = "http://localhost:8080/api/customer/wishlist";

const getAuthHeader = () => {
  const token =
    localStorage.getItem("token") || localStorage.getItem("accessToken");
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const useWishlistStore = defineStore("wishlist", {
  state: () => ({
    wishlistItems: [],
    loading: false,
  }),

  getters: {
    isInWishlist: (state) => (productId) => {
      if (!productId) return false;
      return state.wishlistItems.some((item) => item.id === productId);
    },
  },

  actions: {
    async fetchWishlist() {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        this.wishlistItems = [];
        return;
      }
      this.loading = true;
      try {
        const response = await axios.get(API_BASE, {
          headers: getAuthHeader(),
        });
        if (response.data && response.data.success) {
          this.wishlistItems = response.data.data || [];
        }
      } catch (error) {
        console.error("Error fetching wishlist:", error);
      } finally {
        this.loading = false;
      }
    },

    async toggleWishlist(productId) {
      console.log("📤 Toggling wishlist for productId:", productId);
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        window.dispatchEvent(
          new CustomEvent("open-auth-modal", { detail: "login" }),
        );
        return false;
      }
      try {
        // ✅ SỬA: Gửi productId trong body JSON
        const response = await axios.post(
          `${API_BASE}/toggle`,
          { productId }, // ← GỬI JSON BODY
          {
            headers: {
              ...getAuthHeader(),
              "Content-Type": "application/json",
            },
          },
        );
        console.log("✅ Response:", response.data);
        if (response.data && response.data.success) {
          await this.fetchWishlist();
          return true;
        }
      } catch (error) {
        console.error("❌ Error toggling wishlist:", error);
        console.error("❌ Error response:", error.response?.data);
        console.error("❌ Error status:", error.response?.status);
      }
      return false;
    },
  },
});
