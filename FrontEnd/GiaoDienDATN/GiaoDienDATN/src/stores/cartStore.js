// stores/cartStore.js
import { defineStore } from "pinia";
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

export const useCartStore = defineStore("cart", {
  state: () => ({
    items: [],
    loading: false,
    subtotal: 0,
    total: 0,
    totalItems: 0,
    shippingFee: 30000,
    coupon: null,
    discount: 0,
    voucherDiscount: 0,
    voucherCode: null,
  }),

  getters: {
    totalItems: (state) => {
      return state.items.reduce((sum, item) => sum + (item.quantity || 0), 0);
    },
    subtotal: (state) => {
      // ✅ QUAN TRỌNG: Tính subtotal dựa trên discountedPrice
      return state.items.reduce((sum, item) => {
        // Ưu tiên dùng discountedPrice, nếu không có thì dùng price
        const price = item.discountedPrice || item.price || 0;
        return sum + price * (item.quantity || 0);
      }, 0);
    },
    total: (state) => {
      const subtotal = state.items.reduce((sum, item) => {
        const price = item.discountedPrice || item.price || 0;
        return sum + price * (item.quantity || 0);
      }, 0);
      return (
        subtotal - (state.voucherDiscount || 0) + (state.shippingFee || 30000)
      );
    },
  },

  actions: {
    async addToCart(productOrId, quantity = 1, size, color) {
      try {
        let productDetailId;
        let productName = "";
        let price = 0;
        let discountedPrice = null;
        let image = null;
        let stock = 99;

        if (typeof productOrId === "object" && productOrId !== null) {
          productDetailId =
            productOrId.productDetailId ||
            productOrId.id ||
            productOrId.productId;
          productName =
            productOrId.productName || productOrId.name || "Sản phẩm";
          price = productOrId.price || 0;
          discountedPrice =
            productOrId.discountedPrice || productOrId.discountedPrice || null;
          image = productOrId.image || productOrId.imageUrl || null;
          stock = productOrId.stock || 99;

          if (!size && productOrId.size) size = productOrId.size;
          if (!color && productOrId.color) color = productOrId.color;
          if (productOrId.quantity) quantity = productOrId.quantity;
        } else {
          productDetailId = productOrId;
        }

        if (!productDetailId) {
          console.error("❌ Missing productDetailId");
          throw new Error("Không tìm thấy thông tin sản phẩm");
        }

        const token =
          localStorage.getItem("token") || localStorage.getItem("accessToken");

        if (!token) {
          console.log("ℹ️ Chưa đăng nhập, lưu vào localStorage");
          const cartItem = {
            id: productDetailId,
            productDetailId: productDetailId,
            productId: productDetailId,
            name: productName,
            price: price,
            discountedPrice: discountedPrice,
            quantity: quantity,
            size: size || null,
            color: color || null,
            image: image,
            imageUrl: image,
            stock: stock,
          };

          let localCart = JSON.parse(localStorage.getItem("cart") || "[]");
          const existingIndex = localCart.findIndex(
            (item) =>
              item.id === productDetailId &&
              item.size === size &&
              item.color === color,
          );

          if (existingIndex !== -1) {
            localCart[existingIndex].quantity += quantity;
          } else {
            localCart.push(cartItem);
          }

          localStorage.setItem("cart", JSON.stringify(localCart));
          await this.loadFromLocalStorage();
          return { success: true, local: true };
        }

        const response = await axios.post(
          `${API_BASE}/cart/add`,
          {
            productDetailId: productDetailId,
            quantity: quantity,
          },
          {
            headers: {
              Authorization: `Bearer ${token}`,
              "Content-Type": "application/json",
            },
          },
        );

        console.log("✅ Add to cart response:", response.data);
        await this.fetchCart();
        return response.data;
      } catch (error) {
        console.error("❌ Add to cart error:", error);
        throw error;
      }
    },

    async updateQuantity(cartItemId, quantity, productDetailId, size, color) {
      if (!cartItemId) {
        console.error("❌ Missing cartItemId");
        throw new Error("Không tìm thấy ID sản phẩm");
      }

      try {
        const token =
          localStorage.getItem("token") || localStorage.getItem("accessToken");

        const response = await axios({
          method: "put",
          url: `${API_BASE}/cart/items/${cartItemId}`,
          params: { quantity: quantity },
          headers: token
            ? {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
              }
            : {},
        });

        console.log("✅ Update response:", response.data);
        await this.fetchCart();
        return response.data;
      } catch (error) {
        console.error("❌ Update quantity error:", error);
        throw error;
      }
    },

    async removeFromCart(cartItemId, productId, size, color) {
      if (!cartItemId) {
        console.error("❌ Missing cartItemId");
        throw new Error("Không tìm thấy ID sản phẩm");
      }

      try {
        const token =
          localStorage.getItem("token") || localStorage.getItem("accessToken");

        const response = await axios.delete(
          `${API_BASE}/cart/items/${cartItemId}`,
          {
            headers: token ? { Authorization: `Bearer ${token}` } : {},
          },
        );

        console.log("✅ Remove response:", response.data);
        await this.fetchCart();
        return response.data;
      } catch (error) {
        console.error("❌ Remove from cart error:", error);
        throw error;
      }
    },

    async clearCart() {
      try {
        const token =
          localStorage.getItem("token") || localStorage.getItem("accessToken");

        if (token) {
          await axios.delete(`${API_BASE}/cart/clear`, {
            headers: token ? { Authorization: `Bearer ${token}` } : {},
          });
        }

        this.items = [];
        this.subtotal = 0;
        this.totalItems = 0;
        this.total = 0;
        this.voucherDiscount = 0;
        this.voucherCode = null;
        this.saveToLocalStorage();
      } catch (error) {
        console.error("❌ Clear cart error:", error);
        throw error;
      }
    },

    async fetchCart() {
      this.loading = true;
      try {
        const token =
          localStorage.getItem("token") || localStorage.getItem("accessToken");

        if (!token) {
          console.log("ℹ️ No token, loading from localStorage");
          this.loadFromLocalStorage();
          this.loading = false;
          return;
        }

        console.log("🔄 Fetching cart...");
        const response = await axios.get(`${API_BASE}/cart`, {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });

        console.log("✅ Cart response:", response.data);

        if (response.data && response.data.data) {
          const cartData = response.data.data;

          // ✅ Map items - QUAN TRỌNG: Lấy discountedPrice từ response
          this.items = (cartData.items || []).map((item) => ({
            ...item,
            stock: item.stock || 99,
            id: item.productId || item.id,
            productId: item.productId || item.id,
            name: item.productName || item.name,
            image: item.imageUrl || null,
            imageUrl: item.imageUrl || null,
            price: item.price || 0,
            discountedPrice: item.discountedPrice || item.price || 0, // ✅ LẤY discountedPrice
          }));

          // ✅ Tính subtotal dựa trên discountedPrice
          this.subtotal = this.items.reduce((sum, item) => {
            const price = item.discountedPrice || item.price || 0;
            return sum + price * (item.quantity || 0);
          }, 0);

          this.totalItems = cartData.totalQuantity || 0;

          this.voucherDiscount = cartData.voucherDiscount || 0;
          this.voucherCode = cartData.voucherCode || null;
          this.shippingFee = cartData.shippingFee || 30000;

          this.total = this.subtotal - this.voucherDiscount + this.shippingFee;

          console.log("📊 Cart summary:", {
            items: this.items.length,
            subtotal: this.subtotal,
            voucherDiscount: this.voucherDiscount,
            shippingFee: this.shippingFee,
            total: this.total,
          });

          this.saveToLocalStorage();
        }
      } catch (error) {
        console.error("❌ Fetch cart error:", error);
        this.loadFromLocalStorage();
      } finally {
        this.loading = false;
      }
    },

    saveToLocalStorage() {
      try {
        const data = {
          items: this.items,
          subtotal: this.subtotal,
          total: this.total,
          totalItems: this.totalItems,
          shippingFee: this.shippingFee,
          voucherDiscount: this.voucherDiscount,
          voucherCode: this.voucherCode,
        };
        localStorage.setItem("cart", JSON.stringify(data));
        console.log("💾 Cart saved to localStorage");
      } catch (e) {
        console.error("❌ Save cart error:", e);
      }
    },

    loadFromLocalStorage() {
      try {
        const data = localStorage.getItem("cart");
        if (data) {
          const parsed = JSON.parse(data);
          this.items = parsed.items || [];
          this.subtotal = parsed.subtotal || 0;
          this.total = parsed.total || 0;
          this.totalItems = parsed.totalItems || 0;
          this.shippingFee = parsed.shippingFee || 30000;
          this.voucherDiscount = parsed.voucherDiscount || 0;
          this.voucherCode = parsed.voucherCode || null;
          console.log(
            "📂 Cart loaded from localStorage:",
            this.items.length,
            "items",
          );
        }
      } catch (e) {
        console.error("❌ Load cart error:", e);
      }
    },

    syncCartAfterLogin() {
      console.log("🔄 Syncing cart after login");
      this.fetchCart();
    },
  },
});
