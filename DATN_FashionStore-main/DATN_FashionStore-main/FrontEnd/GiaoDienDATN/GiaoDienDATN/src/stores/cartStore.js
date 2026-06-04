import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useCartStore = defineStore("cart", () => {
  // State
  const items = ref([]);
  const isCartOpen = ref(false);

  // Load from localStorage
  const loadCart = () => {
    const savedCart = localStorage.getItem("cart");
    if (savedCart) {
      items.value = JSON.parse(savedCart);
    }
  };

  // Save to localStorage
  const saveCart = () => {
    localStorage.setItem("cart", JSON.stringify(items.value));
  };

  // Initialize
  loadCart();

  // Getters
  const totalItems = computed(() => {
    return items.value.reduce((total, item) => total + item.quantity, 0);
  });

  const subtotal = computed(() => {
    return items.value.reduce(
      (total, item) => total + item.price * item.quantity,
      0,
    );
  });

  const shippingFee = computed(() => {
    return subtotal.value > 500000 ? 0 : 30000;
  });

  const total = computed(() => {
    return subtotal.value + shippingFee.value;
  });

  // Actions
  function addToCart(product, quantity = 1, size = null, color = null) {
    const existingItem = items.value.find(
      (item) =>
        item.id === product.id && item.size === size && item.color === color,
    );

    if (existingItem) {
      existingItem.quantity += quantity;
    } else {
      items.value.push({
        ...product,
        quantity,
        size,
        color,
      });
    }

    saveCart();
    isCartOpen.value = true;
  }

  function removeFromCart(productId, size = null, color = null) {
    const index = items.value.findIndex(
      (item) =>
        item.id === productId && item.size === size && item.color === color,
    );
    if (index !== -1) {
      items.value.splice(index, 1);
      saveCart();
    }
  }

  function updateQuantity(productId, quantity, size = null, color = null) {
    const item = items.value.find(
      (item) =>
        item.id === productId && item.size === size && item.color === color,
    );
    if (item) {
      item.quantity = quantity;
      saveCart();
    }
  }

  function clearCart() {
    items.value = [];
    saveCart();
  }

  function toggleCart() {
    isCartOpen.value = !isCartOpen.value;
  }

  return {
    // State
    items,
    isCartOpen,

    // Getters
    totalItems,
    subtotal,
    shippingFee,
    total,

    // Actions
    addToCart,
    removeFromCart,
    updateQuantity,
    clearCart,
    toggleCart,
  };
});
