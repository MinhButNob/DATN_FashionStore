import { defineStore } from "pinia";
import api from "../api/axios";
import { useAuthStore } from "./authStore";

export const useWishlistStore = defineStore("wishlist", {
  state: () => ({
    wishlistItems: [],
    loading: false
  }),

  getters: {
    items: (state) => state.wishlistItems,

    isInWishlist: (state) => (productDetailId) => {
      if (!productDetailId) return false;
      return state.wishlistItems.some((item) => item.productDetailId === productDetailId);
    }
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
        // ✅ Fixed path: /customer/wishlist → /wishlist
        const response = await api.get("/wishlist");
        if (response.data && response.data.data) {
          // Map response data to expected format
          this.wishlistItems = response.data.data.items || [];
        }
      } catch (error) {
        console.error("Error fetching wishlist:", error);
        this.wishlistItems = [];
      } finally {
        this.loading = false;
      }
    },

    async addToWishlist(productDetailId) {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        window.dispatchEvent(
          new CustomEvent("open-auth-modal", { detail: "login" })
        );
        return false;
      }

      try {
        // ✅ Fixed path: /customer/wishlist/toggle → /wishlist/add
        const response = await api.post("/wishlist/add", {
          productDetailId
        });

        if (response.data && response.data.data) {
          this.wishlistItems = response.data.data.items || [];
          return true;
        }
      } catch (error) {
        console.error("Error adding to wishlist:", error);
      }
      return false;
    },

    async removeFromWishlist(productDetailId) {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        window.dispatchEvent(
          new CustomEvent("open-auth-modal", { detail: "login" })
        );
        return false;
      }

      try {
        // ✅ Fixed path with productDetailId in URL
        const response = await api.delete(`/wishlist/items/${productDetailId}`);

        if (response.data && response.data.data) {
          this.wishlistItems = response.data.data.items || [];
          return true;
        }
      } catch (error) {
        console.error("Error removing from wishlist:", error);
      }
      return false;
    },

    async toggleWishlist(productDetailId) {
      console.log("📤 Toggling wishlist for productDetailId:", productDetailId);
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        window.dispatchEvent(
          new CustomEvent("open-auth-modal", { detail: "login" })
        );
        return false;
      }

      try {
        const isInWishlist = this.isInWishlist(productDetailId);
        
        if (isInWishlist) {
          // Remove from wishlist
          return await this.removeFromWishlist(productDetailId);
        } else {
          // Add to wishlist
          return await this.addToWishlist(productDetailId);
        }
      } catch (error) {
        console.error("❌ Error toggling wishlist:", error);
        return false;
      }
    }
  }
});