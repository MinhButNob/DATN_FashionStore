import { defineStore } from "pinia";
import api from "../api/axios";
import { useAuthStore } from "./authStore";

export const useWishlistStore = defineStore("wishlist", {
  state: () => ({
    wishlistItems: [],
    loading: false }),

  getters: {
    isInWishlist: (state) => (productId) => {
      if (!productId) return false;
      return state.wishlistItems.some((item) => item.id === productId);
    } },

  actions: {
    async fetchWishlist() {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        this.wishlistItems = [];
        return;
      }
      this.loading = true;
      try {
        const response = await api.get("/customer/wishlist");
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
        const response = await api.post("/customer/wishlist/toggle", {
          productId });
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
    } } });