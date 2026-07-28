// ⚠️ CẢNH BÁO: File này định nghĩa defineStore("cart", ...) TRÙNG với
// cartStore.js (cùng store id "cart", cùng tên export useCartStore).
// Pinia sẽ chỉ nhận 1 trong 2 tùy theo file nào được import sau cùng,
// gây khó debug. Khuyến nghị: kiểm tra xem trang nào đang import từ
// "orderStore" và chuyển sang import từ "cartStore", rồi XÓA file này.
// Mình vẫn sửa để dùng chung axios instance, phòng khi vẫn còn nơi
// đang import từ file này.
import { defineStore } from "pinia";
import { ref, computed } from "vue";
import api from "../api/axios";

export const useCartStore = defineStore("cart", () => {
  const items = ref([]);
  const isCartOpen = ref(false);
  const loading = ref(false);

  const saveCart = () => {
    localStorage.setItem("cart", JSON.stringify(items.value));
  };

  // ✅ fetchCart - gọi API thật
  const fetchCart = async () => {
    loading.value = true;
    try {
      const token = localStorage.getItem("accessToken");

      if (!token) {
        console.log("❌ No token found");
        loading.value = false;
        return;
      }

      console.log("🟡 Fetching cart from API...");
      const response = await api.get("/cart");

      console.log("✅ API Response:", response.data);

      // Backend trả về ApiResponse { code, message, data }
      if (response.data && response.data.code === 200 && response.data.data) {
        const cartData = response.data.data;

        if (cartData.cartItems && Array.isArray(cartData.cartItems)) {
          items.value = cartData.cartItems.map((item) => ({
            cartItemId: item.cartItemId,
            id: item.productDetailId,
            productId: item.productId,
            name: item.productName,
            price: item.unitPrice,
            quantity: item.quantity,
            size: item.sizeName,
            color: item.colorName,
            imageUrl: item.imageUrl,
            image: item.imageUrl }));
        } else if (cartData.items && Array.isArray(cartData.items)) {
          items.value = cartData.items.map((item) => ({
            cartItemId: item.cartItemId,
            id: item.productDetailId,
            name: item.productName,
            price: item.unitPrice,
            quantity: item.quantity,
            size: item.sizeName,
            color: item.colorName,
            image: item.imageUrl }));
        } else {
          items.value = [];
        }

        saveCart();
        console.log("✅ Cart loaded:", items.value.length, "items");
      } else {
        console.log("⚠️ Invalid response format:", response.data);
        items.value = [];
      }
    } catch (error) {
      console.error(
        "❌ Error fetching cart:",
        error.response?.data || error.message,
      );
      items.value = [];
    } finally {
      loading.value = false;
    }
  };

  // ✅ Add to cart - gọi API thật
  const addToCart = async (
    product,
    quantity = 1,
    size = null,
    color = null,
  ) => {
    const token = localStorage.getItem("accessToken");

    if (!token) {
      console.log("❌ Need login to add to cart");
      return;
    }

    loading.value = true;
    try {
      console.log("🟡 Adding to cart:", {
        productDetailId: product.id,
        quantity,
        size,
        color });

      const response = await api.post("/cart/add", {
        productDetailId: product.id,
        quantity: quantity,
        size: size,
        color: color });

      console.log("✅ Add to cart response:", response.data);

      if (response.data && response.data.code === 200) {
        await fetchCart(); // Refresh cart
        isCartOpen.value = true;
      }
    } catch (error) {
      console.error(
        "❌ Error adding to cart:",
        error.response?.data || error.message,
      );
    } finally {
      loading.value = false;
    }
  };

  // ✅ Update quantity
  const updateQuantity = async (
    cartItemId,
    quantity,
    productId,
    size,
    color,
  ) => {
    const token = localStorage.getItem("accessToken");

    if (!token || !cartItemId) return;

    try {
      console.log("🟡 Updating quantity:", { cartItemId, quantity });

      await api.put(`/cart/items/${cartItemId}`, null, {
        params: { quantity: quantity } });

      await fetchCart(); // Refresh cart
    } catch (error) {
      console.error(
        "❌ Error updating quantity:",
        error.response?.data || error.message,
      );
    }
  };

  // ✅ Remove from cart
  const removeFromCart = async (cartItemId, productId, size, color) => {
    const token = localStorage.getItem("accessToken");

    if (!token || !cartItemId) return;

    try {
      console.log("🟡 Removing from cart:", { cartItemId });

      await api.delete(`/cart/items/${cartItemId}`);

      await fetchCart(); // Refresh cart
    } catch (error) {
      console.error(
        "❌ Error removing from cart:",
        error.response?.data || error.message,
      );
    }
  };

  // ✅ Clear cart
  const clearCart = async () => {
    const token = localStorage.getItem("accessToken");

    if (!token) return;

    try {
      console.log("🟡 Clearing cart...");

      await api.delete("/cart/clear");

      items.value = [];
      saveCart();
    } catch (error) {
      console.error(
        "❌ Error clearing cart:",
        error.response?.data || error.message,
      );
    }
  };

  // Helper functions
  const updateQuantityItem = (item, change) => {
    const newQuantity = item.quantity + change;
    if (newQuantity >= 1 && newQuantity <= 99) {
      updateQuantity(
        item.cartItemId,
        newQuantity,
        item.id,
        item.size,
        item.color,
      );
    }
  };

  const toggleCart = () => {
    isCartOpen.value = !isCartOpen.value;
  };

  // Getters
  const totalItems = computed(() => {
    return items.value.reduce((total, item) => total + (item.quantity || 0), 0);
  });

  const subtotal = computed(() => {
    return items.value.reduce(
      (total, item) => total + (item.price || 0) * (item.quantity || 0),
      0,
    );
  });

  const shippingFee = computed(() => {
    return subtotal.value > 500000 ? 0 : 30000;
  });

  const total = computed(() => {
    return subtotal.value + shippingFee.value;
  });

  return {
    items,
    isCartOpen,
    loading,
    totalItems,
    subtotal,
    shippingFee,
    total,
    fetchCart,
    addToCart,
    removeFromCart,
    updateQuantity,
    updateQuantityItem,
    clearCart,
    toggleCart };
});