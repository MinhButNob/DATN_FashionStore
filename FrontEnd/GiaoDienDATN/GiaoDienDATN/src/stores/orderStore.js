import { defineStore } from "pinia";
import { ref, computed } from "vue";
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

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
      const token = localStorage.getItem("token");

      if (!token) {
        console.log("❌ No token found");
        loading.value = false;
        return;
      }

      console.log("🟡 Fetching cart from API...");
      const response = await axios.get(`${API_BASE}/cart`, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

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
            image: item.imageUrl,
          }));
        } else if (cartData.items && Array.isArray(cartData.items)) {
          items.value = cartData.items.map((item) => ({
            cartItemId: item.cartItemId,
            id: item.productDetailId,
            name: item.productName,
            price: item.unitPrice,
            quantity: item.quantity,
            size: item.sizeName,
            color: item.colorName,
            image: item.imageUrl,
          }));
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
    const token = localStorage.getItem("token");

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
        color,
      });

      const response = await axios.post(
        `${API_BASE}/cart/add`,
        {
          productDetailId: product.id,
          quantity: quantity,
          size: size,
          color: color,
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        },
      );

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
    const token = localStorage.getItem("token");

    if (!token || !cartItemId) return;

    try {
      console.log("🟡 Updating quantity:", { cartItemId, quantity });

      await axios.put(`${API_BASE}/cart/items/${cartItemId}`, null, {
        params: { quantity: quantity },
        headers: { Authorization: `Bearer ${token}` },
      });

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
    const token = localStorage.getItem("token");

    if (!token || !cartItemId) return;

    try {
      console.log("🟡 Removing from cart:", { cartItemId });

      await axios.delete(`${API_BASE}/cart/items/${cartItemId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

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
    const token = localStorage.getItem("token");

    if (!token) return;

    try {
      console.log("🟡 Clearing cart...");

      await axios.delete(`${API_BASE}/cart/clear`, {
        headers: { Authorization: `Bearer ${token}` },
      });

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
    toggleCart,
  };
});
