// stores/cartStore.js
import { defineStore } from "pinia";
import api from "../api/axios";

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
    voucherId: null, // ✅ THÊM voucherId
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
    // ✅ Thêm getter để kiểm tra có voucher không
    hasVoucher: (state) => {
      return !!state.voucherCode && !!state.voucherId;
    },
    // ✅ Thêm getter để lấy finalTotal
    finalTotal: (state) => {
      const subtotal = state.items.reduce((sum, item) => {
        const price = item.discountedPrice || item.price || 0;
        return sum + price * (item.quantity || 0);
      }, 0);
      return (
        subtotal - (state.voucherDiscount || 0) + (state.shippingFee || 30000)
      );
    } },

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

        const token = localStorage.getItem("accessToken");

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
            stock: stock };

          let localData = localStorage.getItem("cart");
          let localCart = [];
          if (localData) {
            try {
              let parsed = JSON.parse(localData);
              localCart = Array.isArray(parsed) ? parsed : (parsed.items || []);
            } catch(e) {}
          }

          const existingIndex = localCart.findIndex(
            (item) =>
              (item.productDetailId === productDetailId || item.id === productDetailId) &&
              item.size === size &&
              item.color === color,
          );

          if (existingIndex !== -1) {
            localCart[existingIndex].quantity += quantity;
          } else {
            localCart.push(cartItem);
          }

          this.items = localCart;
          this.totalItems = localCart.length;
          
          let newSubtotal = 0;
          localCart.forEach(item => {
            const price = item.discountedPrice || item.price || 0;
            newSubtotal += price * item.quantity;
          });
          this.subtotal = newSubtotal;
          this.total = this.subtotal - this.voucherDiscount + this.shippingFee;
          
          this.saveToLocalStorage();
          return { success: true, local: true };
        }

        const response = await api.post("/cart/add", {
          productDetailId: productDetailId,
          quantity: quantity });

        console.log("✅ Add to cart response:", response.data);
        await this.fetchCart();
        return response.data;
      } catch (error) {
        console.error("❌ Add to cart error:", error);
        throw error;
      }
    },

    async updateQuantity(cartItemId, quantity, productDetailId, size, color) {
      const token = localStorage.getItem("accessToken");
      if (!token) {
        try {
          let localData = localStorage.getItem("cart");
          if (localData) {
            let parsed = JSON.parse(localData);
            let localCart = Array.isArray(parsed) ? parsed : (parsed.items || []);
            const index = localCart.findIndex(item => 
              (item.productDetailId === productDetailId || item.id === productDetailId) && 
              item.size === size && 
              item.color === color
            );
            if (index !== -1) {
              localCart[index].quantity = quantity;
              this.items = localCart;
              this.totalItems = localCart.reduce((sum, item) => sum + item.quantity, 0);
              let newSubtotal = 0;
              localCart.forEach(item => {
                const price = item.discountedPrice || item.price || 0;
                newSubtotal += price * item.quantity;
              });
              this.subtotal = newSubtotal;
              this.total = this.subtotal - (this.voucherDiscount || 0) + this.shippingFee;
              this.saveToLocalStorage();
              return { success: true, local: true };
            }
          }
        } catch (e) {
          console.error("❌ Local update error:", e);
        }
        return { success: false };
      }

      if (!cartItemId) {
        console.error("❌ Missing cartItemId");
        throw new Error("Không tìm thấy ID sản phẩm");
      }

      try {
        const response = await api.put(
          `/cart/items/${cartItemId}`,
          null,
          { params: { quantity: quantity } },
        );

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
        const response = await api.delete(`/cart/items/${cartItemId}`);

        console.log("✅ Remove response:", response.data);
        await this.fetchCart();
        return response.data;
      } catch (error) {
        console.error("❌ Remove from cart error:", error);
        throw error;
      }
    },

    // ✅ THÊM ACTION ÁP DỤNG VOUCHER
    async applyVoucher(voucherCode) {
      try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
          throw new Error("Vui lòng đăng nhập để áp dụng voucher");
        }

        const response = await api.post(
          `/cart/apply-voucher?voucherCode=${voucherCode}`,
          {},
        );

        console.log("✅ Apply voucher response:", response.data);

        if (response.data && response.data.data) {
          const cartData = response.data.data;

          // ✅ Cập nhật voucherId
          this.voucherId = cartData.voucherId || null;
          this.voucherCode = cartData.voucherCode || null;
          this.voucherDiscount = cartData.voucherDiscount || 0;

          // ✅ Cập nhật lại giỏ hàng
          this.items = (cartData.items || []).map((item) => ({
            ...item,
            stock: item.stock || 99,
            id: item.productId || item.id,
            productId: item.productId || item.id,
            name: item.productName || item.name,
            image: item.imageUrl || null,
            imageUrl: item.imageUrl || null,
            price: item.price || 0,
            discountedPrice: item.discountedPrice || item.price || 0 }));

          this.subtotal = this.items.reduce((sum, item) => {
            const price = item.discountedPrice || item.price || 0;
            return sum + price * (item.quantity || 0);
          }, 0);

          this.totalItems = cartData.totalQuantity || 0;
          this.shippingFee = cartData.shippingFee || 30000;
          this.total = this.subtotal - this.voucherDiscount + this.shippingFee;

          // ✅ Lưu vào localStorage
          this.saveToLocalStorage();
        }

        return response.data;
      } catch (error) {
        console.error("❌ Apply voucher error:", error);
        throw error;
      }
    },

    // ✅ THÊM ACTION XÓA VOUCHER
    async removeVoucher() {
      try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
          throw new Error("Vui lòng đăng nhập");
        }

        const response = await api.delete("/cart/remove-voucher");

        console.log("✅ Remove voucher response:", response.data);

        if (response.data && response.data.data) {
          const cartData = response.data.data;

          // ✅ Xóa voucher
          this.voucherId = null;
          this.voucherCode = null;
          this.voucherDiscount = 0;

          // ✅ Cập nhật lại giỏ hàng
          this.items = (cartData.items || []).map((item) => ({
            ...item,
            stock: item.stock || 99,
            id: item.productId || item.id,
            productId: item.productId || item.id,
            name: item.productName || item.name,
            image: item.imageUrl || null,
            imageUrl: item.imageUrl || null,
            price: item.price || 0,
            discountedPrice: item.discountedPrice || item.price || 0 }));

          this.subtotal = this.items.reduce((sum, item) => {
            const price = item.discountedPrice || item.price || 0;
            return sum + price * (item.quantity || 0);
          }, 0);

          this.totalItems = cartData.totalQuantity || 0;
          this.shippingFee = cartData.shippingFee || 30000;
          this.total = this.subtotal - this.voucherDiscount + this.shippingFee;

          // ✅ Lưu vào localStorage
          this.saveToLocalStorage();
        }

        return response.data;
      } catch (error) {
        console.error("❌ Remove voucher error:", error);
        throw error;
      }
    },

    async clearCart() {
      try {
        const token = localStorage.getItem("accessToken");

        if (token) {
          await api.delete("/cart/clear");
        }

        this.items = [];
        this.subtotal = 0;
        this.totalItems = 0;
        this.total = 0;
        this.voucherDiscount = 0;
        this.voucherCode = null;
        this.voucherId = null; // ✅ Xóa voucherId
        this.saveToLocalStorage();
      } catch (error) {
        console.error("❌ Clear cart error:", error);
        throw error;
      }
    },

    async fetchCart() {
      this.loading = true;
      try {
        const token = localStorage.getItem("accessToken");

        if (!token) {
          console.log("ℹ️ No token, loading from localStorage");
          this.loadFromLocalStorage();
          this.loading = false;
          return;
        }

        console.log("🔄 Fetching cart...");
        const response = await api.get("/cart");

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
            discountedPrice: item.discountedPrice || item.price || 0 }));

          // ✅ Tính subtotal dựa trên discountedPrice
          this.subtotal = this.items.reduce((sum, item) => {
            const price = item.discountedPrice || item.price || 0;
            return sum + price * (item.quantity || 0);
          }, 0);

          this.totalItems = cartData.totalQuantity || 0;

          // ✅ Lấy thông tin voucher - QUAN TRỌNG: Lấy voucherId
          this.voucherDiscount = cartData.voucherDiscount || 0;
          this.voucherCode = cartData.voucherCode || null;
          this.voucherId = cartData.voucherId || null; // ✅ THÊM
          this.shippingFee = cartData.shippingFee || 30000;

          this.total = this.subtotal - this.voucherDiscount + this.shippingFee;

          console.log("📊 Cart summary:", {
            items: this.items.length,
            subtotal: this.subtotal,
            voucherDiscount: this.voucherDiscount,
            voucherCode: this.voucherCode,
            voucherId: this.voucherId,
            shippingFee: this.shippingFee,
            total: this.total });

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
          voucherId: this.voucherId, // ✅ THÊM voucherId
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
          this.voucherId = parsed.voucherId || null; // ✅ THÊM voucherId
          console.log(
            "📂 Cart loaded from localStorage:",
            this.items.length,
            "items",
            "voucherId:",
            this.voucherId,
          );
        }
      } catch (e) {
        console.error("❌ Load cart error:", e);
      }
    },

    async syncCartAfterLogin() {
      console.log("🔄 Syncing cart after login");
      try {
        const localData = localStorage.getItem("cart");
        if (localData) {
          const parsed = JSON.parse(localData);
          const localItems = Array.isArray(parsed) ? parsed : (parsed.items || []);
          
          if (localItems.length > 0) {
            console.log("🔄 Merging local cart items to backend...");
            for (const item of localItems) {
              try {
                await api.post("/cart/add", {
                  productDetailId: item.productDetailId || item.id,
                  quantity: item.quantity
                });
              } catch (e) {
                console.error("❌ Failed to sync item to backend:", item, e);
              }
            }
            localStorage.removeItem("cart"); // Clear after merging
          }
        }
      } catch (e) {
        console.error("❌ Error syncing local cart:", e);
      }
      
      await this.fetchCart();
    } } });