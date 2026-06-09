<template>
  <div class="staff-pos">
    <section class="product-panel">
      <div class="toolbar">
        <div class="search-box">
          <i class="fas fa-search"></i>
          <input v-model="keyword" type="text" placeholder="Tìm sản phẩm theo tên hoặc mã SKU" />
        </div>
        <select v-model="category">
          <option value="all">Tất cả danh mục</option>
          <option value="ao">Áo</option>
          <option value="quan">Quần</option>
          <option value="phu-kien">Phụ kiện</option>
        </select>
      </div>

      <div class="product-grid">
        <button v-for="product in filteredProducts" :key="product.id" class="product-card" @click="addToCart(product)">
          <div class="product-thumb">
            <i :class="product.icon"></i>
          </div>
          <div class="product-name">{{ product.name }}</div>
          <div class="product-meta">{{ product.sku }} · Tồn {{ product.stock }}</div>
          <div class="product-price">{{ formatPrice(product.price) }}</div>
        </button>
      </div>
    </section>

    <aside class="cart-panel">
      <div class="cart-header">
        <h3>Hóa đơn tại quầy</h3>
        <button class="clear-btn" @click="cart = []">Xóa</button>
      </div>

      <div class="customer-box">
        <i class="fas fa-user"></i>
        <input v-model="customer" type="text" placeholder="Khách lẻ hoặc số điện thoại" />
      </div>

      <div class="cart-items">
        <div v-for="item in cart" :key="item.id" class="cart-item">
          <div>
            <h4>{{ item.name }}</h4>
            <p>{{ formatPrice(item.price) }}</p>
          </div>
          <div class="qty-control">
            <button @click="decrease(item)">-</button>
            <span>{{ item.quantity }}</span>
            <button @click="item.quantity++">+</button>
          </div>
        </div>
        <div v-if="cart.length === 0" class="empty-cart">Chưa có sản phẩm trong hóa đơn</div>
      </div>

      <div class="summary">
        <div><span>Tạm tính</span><strong>{{ formatPrice(subtotal) }}</strong></div>
        <div><span>Giảm giá</span><input v-model.number="discount" type="number" min="0" /></div>
        <div class="total"><span>Thanh toán</span><strong>{{ formatPrice(total) }}</strong></div>
      </div>

      <div class="payment-methods">
        <button :class="{ active: payment === 'cash' }" @click="payment = 'cash'">
          <i class="fas fa-money-bill"></i>
          Tiền mặt
        </button>
        <button :class="{ active: payment === 'transfer' }" @click="payment = 'transfer'">
          <i class="fas fa-qrcode"></i>
          Chuyển khoản
        </button>
      </div>

      <button class="checkout-btn" :disabled="cart.length === 0" @click="checkout">
        <i class="fas fa-receipt"></i>
        Tạo hóa đơn
      </button>
    </aside>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useNotification } from "../../composables/useNotification";

const { showNotification } = useNotification();
const keyword = ref("");
const category = ref("all");
const customer = ref("");
const cart = ref([]);
const discount = ref(0);
const payment = ref("cash");

const products = [
  { id: 1, name: "Áo thun basic", sku: "AT001", category: "ao", price: 199000, stock: 42, icon: "fas fa-tshirt" },
  { id: 2, name: "Áo sơ mi nam", sku: "SM004", category: "ao", price: 329000, stock: 18, icon: "fas fa-user-tie" },
  { id: 3, name: "Quần jean slim", sku: "QJ002", category: "quan", price: 499000, stock: 26, icon: "fas fa-male" },
  { id: 4, name: "Túi xách nữ", sku: "TX008", category: "phu-kien", price: 299000, stock: 12, icon: "fas fa-shopping-bag" },
  { id: 5, name: "Đầm công sở", sku: "DM011", category: "ao", price: 459000, stock: 9, icon: "fas fa-female" },
  { id: 6, name: "Mũ lưỡi trai", sku: "MLT03", category: "phu-kien", price: 129000, stock: 33, icon: "fas fa-hat-cowboy" },
];

const filteredProducts = computed(() => {
  const search = keyword.value.toLowerCase();
  return products.filter((product) => {
    const matchesCategory = category.value === "all" || product.category === category.value;
    const matchesSearch = product.name.toLowerCase().includes(search) || product.sku.toLowerCase().includes(search);
    return matchesCategory && matchesSearch;
  });
});

const subtotal = computed(() => cart.value.reduce((sum, item) => sum + item.price * item.quantity, 0));
const total = computed(() => Math.max(subtotal.value - discount.value, 0));

const formatPrice = (value) => new Intl.NumberFormat("vi-VN").format(value || 0) + "₫";

const addToCart = (product) => {
  const existing = cart.value.find((item) => item.id === product.id);
  if (existing) {
    existing.quantity++;
  } else {
    cart.value.push({ ...product, quantity: 1 });
  }
};

const decrease = (item) => {
  if (item.quantity > 1) {
    item.quantity--;
  } else {
    cart.value = cart.value.filter((cartItem) => cartItem.id !== item.id);
  }
};

const checkout = () => {
  showNotification("Tạo hóa đơn tại quầy thành công!", "success");
  cart.value = [];
  discount.value = 0;
  customer.value = "";
};
</script>

<style scoped>
.staff-pos {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 25px;
  animation: fadeIn 0.3s;
}

.product-panel,
.cart-panel {
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
  padding: 25px;
}

.toolbar {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.search-box,
.customer-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--light-gray);
  border-radius: 8px;
  padding: 0 14px;
}

.search-box input,
.customer-box input {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  padding: 12px 0;
}

.toolbar select {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 0 12px;
  outline: none;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 15px;
}

.product-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: white;
  text-align: left;
  padding: 16px;
  cursor: pointer;
  transition: var(--transition);
}

.product-card:hover {
  border-color: var(--primary-red);
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}

.product-thumb {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: rgba(230, 57, 70, 0.1);
  color: var(--primary-red);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.product-name {
  color: var(--dark-gray);
  font-weight: 700;
  margin-bottom: 5px;
}

.product-meta {
  color: var(--medium-gray);
  font-size: 12px;
  margin-bottom: 10px;
}

.product-price {
  color: var(--primary-red);
  font-weight: 700;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.cart-header h3 {
  color: var(--dark-gray);
}

.clear-btn {
  border: none;
  background: transparent;
  color: var(--primary-red);
  cursor: pointer;
  font-weight: 600;
}

.cart-items {
  margin: 18px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 240px;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
}

.cart-item h4 {
  color: var(--dark-gray);
  font-size: 14px;
  margin-bottom: 4px;
}

.cart-item p,
.empty-cart {
  color: var(--medium-gray);
  font-size: 13px;
}

.empty-cart {
  text-align: center;
  padding: 60px 10px;
}

.qty-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qty-control button {
  width: 28px;
  height: 28px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: 6px;
  cursor: pointer;
}

.summary {
  border-top: 1px solid var(--border-color);
  padding-top: 16px;
}

.summary div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: var(--dark-gray);
}

.summary input {
  width: 120px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 8px;
  text-align: right;
}

.summary .total {
  font-size: 18px;
  color: var(--primary-red);
}

.payment-methods {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin: 18px 0;
}

.payment-methods button,
.checkout-btn {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 600;
}

.payment-methods button.active {
  border-color: var(--primary-red);
  color: var(--primary-red);
  background: rgba(230, 57, 70, 0.08);
}

.checkout-btn {
  width: 100%;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  border: none;
}

.checkout-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1100px) {
  .staff-pos {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .toolbar,
  .payment-methods {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
