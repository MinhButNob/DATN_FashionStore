<template>
  <div class="staff-pos-wrapper">
    <div class="staff-pos">

      <!-- ===== PANEL TRÁI: Danh sách sản phẩm ===== -->
      <section class="product-panel">

        <!-- Toolbar: Tìm kiếm + Lọc danh mục -->
        <div class="toolbar">
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input v-model="keyword" type="text" placeholder="Tìm theo tên sản phẩm, SKU, phân loại..." />
          </div>
          <div class="category-tabs">
            <button
              v-for="cat in categoryList"
              :key="cat.id"
              :class="['cat-tab', { active: category === cat.id }]"
              @click="category = cat.id"
            >
              {{ cat.name }}
            </button>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="isLoadingProducts" class="loading-state">
          <i class="fas fa-spinner fa-spin"></i> Đang tải sản phẩm...
        </div>

        <!-- Lưới sản phẩm -->
        <div v-else class="product-grid">
          <div v-for="product in filteredProducts" :key="product.id" class="product-card-pos">
            
            <div class="pc-img-wrapper">
              <img v-if="product.mainImageUrl" :src="product.mainImageUrl" :alt="product.name" @error="product.mainImageUrl = null" />
              <div v-else class="no-img">
                <i :class="product.icon"></i>
              </div>
              <div v-if="product.hasPromotion" class="promo-badge-img">
                -{{ product.promotionPercent }}%
              </div>
            </div>

            <div class="pc-info-wrapper">
              <div class="pc-name-pos" :title="product.name">{{ product.name }}</div>
              <div class="pc-price-pos">{{ getVariantPrice(product) }}</div>

              <div class="attr-group" v-if="product.sizes.length > 0">
                <label>Kích cỡ:</label>
                <div class="chip-list">
                  <span 
                    v-for="sz in product.sizes" 
                    :key="sz"
                    :class="['chip', { 
                      active: product.selectedSize === sz,
                      disabled: !isSizeAvailableForColor(product, sz)
                    }]"
                    @click="selectSize(product, sz)"
                  >{{ sz }}</span>
                </div>
              </div>

              <div class="attr-group" v-if="product.colors.length > 0">
                <label>Màu sắc:</label>
                <div class="chip-list">
                  <span 
                    v-for="cl in product.colors" 
                    :key="cl"
                    :class="['chip', { 
                      active: product.selectedColor === cl,
                      disabled: !isColorAvailableForSize(product, cl)
                    }]"
                    @click="selectColor(product, cl)"
                  >{{ cl }}</span>
                </div>
              </div>

              <div class="action-row">
                <div class="stock-info" :class="{ 'out-of-stock': getVariantStock(product) <= 0 }">
                  {{ product.selectedSize && product.selectedColor ? `Tồn: ${getVariantStock(product)}` : `Tổng tồn: ${product.totalStock}` }}
                </div>
                <button 
                  class="add-btn" 
                  :disabled="!product.selectedSize || !product.selectedColor || getVariantStock(product) <= 0"
                  @click="addSelectedVariantToCart(product)"
                >
                  Thêm <i class="fas fa-shopping-cart"></i>
                </button>
              </div>
            </div>

          </div>

          <div v-if="filteredProducts.length === 0" class="empty-state">
            <i class="fas fa-search"></i>
            <p>Không tìm thấy sản phẩm phù hợp</p>
          </div>
        </div>
      </section>

      <!-- ===== PANEL PHẢI: Giỏ hàng ===== -->
      <aside class="cart-panel">
        <div class="cart-header">
          <h3><i class="fas fa-receipt"></i> Hóa đơn tại quầy</h3>
          <button class="clear-btn" @click="clearCurrentCart">
            <i class="fas fa-trash-alt"></i> Xóa giỏ
          </button>
        </div>

        <div class="customer-box">
          <i class="fas fa-user"></i>
          <input
            v-model="customerIdInput"
            type="text"
            placeholder="Mã khách hàng (Enter để gắn)"
            @keyup.enter="attachCustomer"
          />
        </div>

        <div class="cart-items">
          <div v-for="item in cart" :key="item.orderDetailId" class="cart-item">
            <div class="item-info">
              <h4>{{ item.productDetail?.product?.name || 'Sản phẩm' }}</h4>
              <p class="variant-label">
                {{ item.productDetail?.skuDetail }}
                <span class="badge">{{ item.productDetail?.size }} / {{ item.productDetail?.color }}</span>
              </p>
              <p class="price-label">{{ formatPrice(item.unitPrice) }}</p>
            </div>
            <div class="qty-control">
              <button @click="updateQty(item, item.quantity - 1)">−</button>
              <span>{{ item.quantity }}</span>
              <button @click="updateQty(item, item.quantity + 1)">+</button>
            </div>
          </div>
          <div v-if="cart.length === 0" class="empty-cart">
            <i class="fas fa-shopping-basket"></i>
            <p>Chưa có sản phẩm</p>
          </div>
        </div>

        <div class="summary">
          <div><span>Tạm tính</span><strong>{{ formatPrice(subtotal) }}</strong></div>
          <div class="voucher-input-group">
            <span>Mã giảm giá</span>
            <div class="input-btn-wrapper">
              <input v-model="voucherCodeInput" type="text" placeholder="Nhập mã voucher" @keyup.enter="applyVoucherCode" />
              <button @click="applyVoucherCode">Áp dụng</button>
            </div>
          </div>
          <div v-if="discount > 0" class="discount-display">
            <span>Được giảm:</span>
            <span class="text-success">
              -{{ formatPrice(discount) }}
              <i class="fas fa-times-circle" @click="removeVoucherCode"></i>
            </span>
          </div>
          <div class="total"><span>Thanh toán</span><strong>{{ formatPrice(total) }}</strong></div>
        </div>

        <div class="payment-methods">
          <button :class="{ active: paymentMethod === 'CASH' }" @click="paymentMethod = 'CASH'">
            <i class="fas fa-money-bill"></i> Tiền mặt
          </button>
          <button :class="{ active: paymentMethod === 'BANK_TRANSFER' }" @click="paymentMethod = 'BANK_TRANSFER'">
            <i class="fas fa-qrcode"></i> Chuyển khoản
          </button>
        </div>

        <button class="checkout-btn" :disabled="cart.length === 0" @click="checkout">
          <i class="fas fa-receipt"></i> Tạo hóa đơn &amp; In
        </button>
      </aside>

    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from "vue";
import { useNotification } from "../../composables/useNotification";
import { createPosOrder, printPosReceipt } from "../../services/posService";
import api from "@/api/axios.js";

const { showNotification } = useNotification();

// --- State UI ---
const keyword = ref("");
const category = ref("all");
const customerIdInput = ref("");
const voucherCodeInput = ref("");
const paymentMethod = ref("CASH");

// --- State dữ liệu ---
const currentOrder = ref(null);
const products = ref([]);
const categories = ref([]);
const isLoadingProducts = ref(false);

// Danh sách danh mục cho tab lọc
const categoryList = computed(() => [
  { id: "all", name: "Tất cả" },
  ...categories.value.map(c => ({ id: String(c.categoryId), name: c.categoryName }))
]);

// Load danh mục từ API
const loadCategories = async () => {
  try {
    const res = await api.get("/categories/active");
    if (res?.data?.success && Array.isArray(res.data.data)) {
      categories.value = res.data.data;
    } else if (res?.data?.success && Array.isArray(res.data.data?.content)) {
      categories.value = res.data.data.content;
    }
  } catch (err) {
    console.warn("Không tải được danh mục:", err);
  }
};

// Load sản phẩm
const loadProducts = async () => {
  isLoadingProducts.value = true;
  try {
    const response = await api.get("/products");

    let productArray = [];
    if (response?.data?.success && response?.data?.data?.content) {
      productArray = response.data.data.content;
    } else {
      throw new Error("Không thể parse dữ liệu sản phẩm");
    }

    if (!Array.isArray(productArray) || productArray.length === 0) {
      throw new Error("Không có sản phẩm nào được tải về.");
    }

    products.value = productArray.map(p => {
      // Get main image
      let mainImageUrl = null;
      if (p.mainImageUrl) {
        mainImageUrl = p.mainImageUrl;
      } else if (p.images && p.images.length > 0) {
        mainImageUrl = p.images[0].imageUrl;
      }

      const variants = (p.variants || []).map(v => ({
        id: v.productDetailId,
        skuDetail: v.sku || `SKU-${v.productDetailId}`,
        size: v.size || "N/A",
        color: v.color || "N/A",
        stock: v.quantity || 0,
        price: v.price || 0,
        discountedPrice: v.discountedPrice || v.price || 0
      }));

      const sizes = [...new Set(variants.map(v => v.size))].filter(s => s !== "N/A");
      const colors = [...new Set(variants.map(v => v.color))].filter(c => c !== "N/A");
      const minPrice = Math.min(...variants.map(v => v.discountedPrice || v.price));
      const maxPrice = Math.max(...variants.map(v => v.discountedPrice || v.price));
      const totalStock = variants.reduce((sum, v) => sum + v.stock, 0);

      let icon = "fas fa-box";
      const catName = (p.category?.categoryName || "").toLowerCase();
      if (catName.includes("áo") || catName.includes("polo")) icon = "fas fa-tshirt";
      else if (catName.includes("quần")) icon = "fas fa-vest";

      return {
        id: p.productId,
        name: p.productName || "Sản phẩm không tên",
        category: String(p.category?.categoryId || "all"),
        categoryName: p.category?.categoryName || "",
        icon,
        mainImageUrl,
        variants,
        sizes,
        colors,
        minPrice,
        maxPrice,
        totalStock,
        selectedSize: '',
        selectedColor: '',
        hasPromotion: !!p.promotion,
        promotionPercent: p.promotion?.promotionDiscountPercent || 0,
        status: p.status || "ACTIVE"
      };
    }).filter(p => p.status === "ACTIVE" && p.variants.length > 0);

    showNotification(`Đã tải ${products.value.length} sản phẩm`, "success");
  } catch (err) {
    console.error("Lỗi tải sản phẩm:", err);
    showNotification("Tải sản phẩm thất bại.", "error");
  } finally {
    isLoadingProducts.value = false;
  }
};

// Các hàm tính toán cho giao diện Card
const getVariantPrice = (product) => {
  if (!product.selectedSize || !product.selectedColor) {
    if (product.minPrice === product.maxPrice) return formatPrice(product.minPrice);
    return `${formatPrice(product.minPrice)} - ${formatPrice(product.maxPrice)}`;
  }
  const v = product.variants.find(v => v.size === product.selectedSize && v.color === product.selectedColor);
  return v ? formatPrice(v.discountedPrice || v.price) : 'N/A';
};

const getVariantStock = (product) => {
  if (!product.selectedSize || !product.selectedColor) return product.totalStock;
  const v = product.variants.find(v => v.size === product.selectedSize && v.color === product.selectedColor);
  return v ? v.stock : 0;
};

const isSizeAvailableForColor = (product, size) => {
  if (!product.selectedColor) {
    return product.variants.some(v => v.size === size && v.stock > 0);
  }
  return product.variants.some(v => v.size === size && v.color === product.selectedColor && v.stock > 0);
};

const isColorAvailableForSize = (product, color) => {
  if (!product.selectedSize) {
    return product.variants.some(v => v.color === color && v.stock > 0);
  }
  return product.variants.some(v => v.size === product.selectedSize && v.color === color && v.stock > 0);
};

const selectSize = (product, size) => {
  if (!isSizeAvailableForColor(product, size)) return;
  if (product.selectedSize === size) {
    product.selectedSize = ''; 
  } else {
    product.selectedSize = size;
    if (product.selectedColor && !isColorAvailableForSize(product, product.selectedColor)) {
      product.selectedColor = '';
    }
  }
};

const selectColor = (product, color) => {
  if (!isColorAvailableForSize(product, color)) return;
  if (product.selectedColor === color) {
    product.selectedColor = ''; 
  } else {
    product.selectedColor = color;
    if (product.selectedSize && !isSizeAvailableForColor(product, product.selectedSize)) {
      product.selectedSize = '';
    }
  }
};

const addSelectedVariantToCart = (product) => {
  if (!product.selectedSize || !product.selectedColor) {
    showNotification("Vui lòng chọn Kích cỡ và Màu sắc!", "warning");
    return;
  }
  const v = product.variants.find(v => v.size === product.selectedSize && v.color === product.selectedColor);
  if (!v || v.stock <= 0) {
    showNotification("Phân loại này đã hết hàng!", "warning");
    return;
  }
  addVariantToCart(product, v);
  
  // Tự động reset lựa chọn để nhân viên chọn món khác dễ dàng
  product.selectedSize = '';
  product.selectedColor = '';
};


// Lọc sản phẩm theo keyword + danh mục
const filteredProducts = computed(() => {
  const search = keyword.value.toLowerCase().trim();
  return products.value.filter(p => {
    const matchesCat = category.value === "all" || p.category === category.value;
    const matchesSearch =
      !search ||
      p.name.toLowerCase().includes(search) ||
      p.categoryName.toLowerCase().includes(search) ||
      p.variants.some(v =>
        v.skuDetail.toLowerCase().includes(search) ||
        v.size.toLowerCase().includes(search) ||
        v.color.toLowerCase().includes(search)
      );
    return matchesCat && matchesSearch;
  });
});

// Khởi tạo đơn hàng
const initPOSOrder = async () => {
  currentOrder.value = {
    orderId: "TEMP_" + Date.now(),
    orderDetails: [],
    subtotalAmount: 0,
    discountAmount: 0,
    totalAmount: 0,
    customerId: null,
    voucherCode: null
  };
};

// Tính lại giỏ hàng
const recalculateCart = () => {
  if (!currentOrder.value) return;
  let sub = 0;
  currentOrder.value.orderDetails.forEach(item => {
    sub += item.unitPrice * item.quantity;
  });
  currentOrder.value.subtotalAmount = sub;
  currentOrder.value.totalAmount = Math.max(0, sub - (currentOrder.value.discountAmount || 0));
};

// Thêm variant vào giỏ hàng
const addVariantToCart = (product, variant) => {
  if (!currentOrder.value) return;

  const existing = currentOrder.value.orderDetails.find(
    i => i.productDetailId === variant.id
  );

  if (existing) {
    if (existing.quantity >= variant.stock) {
      showNotification(`Chỉ còn ${variant.stock} sản phẩm trong kho!`, "warning");
      return;
    }
    existing.quantity += 1;
    showNotification(`+1 ${product.name} (${variant.size} - ${variant.color})`, "success");
  } else {
    currentOrder.value.orderDetails.push({
      orderDetailId: "ITEM_" + Date.now(),
      productDetailId: variant.id,
      productDetail: {
        id: variant.id,
        skuDetail: variant.skuDetail,
        size: variant.size,
        color: variant.color,
        product: { name: product.name }
      },
      quantity: 1,
      unitPrice: variant.discountedPrice || variant.price
    });
    showNotification(`Đã thêm ${product.name} (${variant.size} - ${variant.color})`, "success");
  }
  recalculateCart();
};

// Computed giỏ hàng
const cart = computed(() => currentOrder.value?.orderDetails || []);
const subtotal = computed(() => currentOrder.value?.subtotalAmount || 0);
const discount = computed(() => currentOrder.value?.discountAmount || 0);
const total = computed(() => currentOrder.value ? Math.max(subtotal.value - discount.value, 0) : 0);

// Format tiền tệ
const formatPrice = value =>
  new Intl.NumberFormat("vi-VN").format(Number(value) || 0) + "₫";

// Cập nhật số lượng trong giỏ
const updateQty = (item, newQty) => {
  if (!currentOrder.value) return;
  
  if (newQty <= 0) {
    currentOrder.value.orderDetails = currentOrder.value.orderDetails.filter(
      i => i.orderDetailId !== item.orderDetailId
    );
    showNotification("Đã xóa sản phẩm khỏi giỏ!", "info");
  } else {
    const found = currentOrder.value.orderDetails.find(i => i.orderDetailId === item.orderDetailId);
    if (found) found.quantity = newQty;
  }
  recalculateCart();
};

// Gắn khách hàng
const attachCustomer = () => {
  if (!currentOrder.value || !customerIdInput.value) {
    showNotification("Vui lòng nhập mã khách hàng!", "warning");
    return;
  }
  currentOrder.value.customerId = customerIdInput.value;
  showNotification("Đã gắn khách hàng vào đơn hàng!", "success");
};

// Voucher
const applyVoucherCode = () => {
  if (!currentOrder.value || !voucherCodeInput.value) {
    showNotification("Vui lòng nhập mã giảm giá!", "warning");
    return;
  }
  currentOrder.value.voucherCode = voucherCodeInput.value;
  currentOrder.value.discountAmount = 50000;
  recalculateCart();
  showNotification("Áp dụng mã giảm giá thành công!", "success");
};

const removeVoucherCode = () => {
  if (!currentOrder.value) return;
  currentOrder.value.voucherCode = null;
  currentOrder.value.discountAmount = 0;
  voucherCodeInput.value = "";
  recalculateCart();
  showNotification("Đã xóa mã giảm giá!", "success");
};

// Xóa giỏ hàng
const clearCurrentCart = async () => {
  if (!currentOrder.value) return;
  voucherCodeInput.value = "";
  customerIdInput.value = "";
  await initPOSOrder();
  showNotification("Đã xóa giỏ hàng!", "success");
};

// Thanh toán
const checkout = async () => {
  if (!currentOrder.value || cart.value.length === 0) {
    showNotification("Giỏ hàng trống!", "warning");
    return;
  }

  try {
    const request = {
      posOrderCustomerId: customerIdInput.value ? parseInt(customerIdInput.value) : null,
      posOrderCustomerName: customerIdInput.value ? null : "Khách lẻ",
      posOrderCustomerPhone: null,
      posOrderType: "AT_COUNTER",
      posOrderDiscountAmount: discount.value || 0,
      posOrderTaxAmount: 0,
      posOrderNotes: "Bán lẻ tại quầy",
      items: currentOrder.value.orderDetails.map(item => ({
        productDetailId: item.productDetailId,
        quantity: item.quantity,
        unitPrice: item.unitPrice || 0,
        discount: 0,
        notes: null
      })),
      payments: [{
        paymentMethodId: paymentMethod.value === "CASH" ? 1 : 2,
        amount: total.value || 0,
        referenceNumber: null,
        notes: null
      }]
    };

    const res = await createPosOrder(request);
    const orderId = res?.posOrderId || res?.data?.posOrderId;

    if (orderId) {
      try { await printPosReceipt(orderId, 1); } catch (e) { /* silent */ }

      showNotification("Thanh toán thành công!", "success");

      printInvoiceHandle({
        storeName: "Fashion Store",
        storeAddress: "Hệ thống thời trang nam",
        storePhone: "0123.456.789",
        orderId,
        orderDate: new Date().toLocaleString("vi-VN"),
        customerName: customerIdInput.value || "Khách lẻ",
        items: currentOrder.value.orderDetails.map(i => ({
          productDetail: i.productDetail,
          quantity: i.quantity,
          finalAmount: i.quantity * i.unitPrice
        })),
        subtotalAmount: currentOrder.value.subtotalAmount,
        discountAmount: currentOrder.value.discountAmount,
        finalAmount: total.value
      });

      voucherCodeInput.value = "";
      customerIdInput.value = "";
      await initPOSOrder();
    } else {
      showNotification("Thanh toán thất bại!", "error");
    }
  } catch (error) {
    console.error("Lỗi thanh toán:", error);
    showNotification("Thanh toán thất bại: " + (error.response?.data?.message || error.message), "error");
  }
};

// In hóa đơn
const printInvoiceHandle = (invoice) => {
  const w = window.open("", "_blank");
  w.document.write(`<html><head><title>Hóa đơn #${invoice.orderId}</title>
  <style>
    body{font-family:'Courier New',monospace;width:80mm;padding:4px;font-size:12px;line-height:1.4}
    .tc{text-align:center}.bold{font-weight:bold}.header{font-size:15px;margin-bottom:8px}
    table{width:100%;border-collapse:collapse;margin:8px 0}
    th,td{text-align:left;padding:4px 0;font-size:11px}
    .ts{border-top:1px dashed #000;padding-top:6px;margin-top:6px}
    .ts p{margin:4px 0;display:flex;justify-content:space-between}
  </style></head><body>
  <div class="tc bold header">${invoice.storeName}</div>
  <div class="tc">${invoice.storeAddress}</div>
  <div class="tc">Hotline: ${invoice.storePhone}</div>
  <p>------------------------------------------</p>
  <p><b>Mã HĐ:</b> #${invoice.orderId}</p>
  <p><b>Thời gian:</b> ${invoice.orderDate}</p>
  <p><b>Khách hàng:</b> ${invoice.customerName}</p>
  <p>------------------------------------------</p>
  <table><thead><tr><th>Mặt hàng</th><th style="text-align:center">SL</th><th style="text-align:right">T.Tiền</th></tr></thead>
  <tbody>${invoice.items.map(item => `<tr>
    <td>${item.productDetail.skuDetail}<br><small>(${item.productDetail.size}/${item.productDetail.color})</small></td>
    <td style="text-align:center">${item.quantity}</td>
    <td style="text-align:right">${new Intl.NumberFormat("vi-VN").format(item.finalAmount)}đ</td>
  </tr>`).join("")}</tbody></table>
  <div class="ts">
    <p><span>Tạm tính:</span><span>${new Intl.NumberFormat("vi-VN").format(invoice.subtotalAmount)}đ</span></p>
    <p><span>Giảm giá:</span><span>-${new Intl.NumberFormat("vi-VN").format(invoice.discountAmount)}đ</span></p>
    <p class="bold" style="font-size:13px"><span>THANH TOÁN:</span><span>${new Intl.NumberFormat("vi-VN").format(invoice.finalAmount)}đ</span></p>
  </div>
  <p>------------------------------------------</p>
  <div class="tc bold">CẢM ƠN QUÝ KHÁCH HẸN GẶP LẠI!</div>
  </body></html>`);
  w.document.close();
  w.focus();
  setTimeout(() => { w.print(); w.close(); }, 400);
};

onMounted(() => {
  loadCategories();
  loadProducts();
  initPOSOrder();
});
</script>

<style scoped>
/* ===== LAYOUT ===== */
.staff-pos-wrapper {
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.3s;
}
.staff-pos {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 20px;
  align-items: start;
}

/* ===== PRODUCT PANEL ===== */
.product-panel {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  padding: 20px;
  min-height: 80vh;
}

/* Toolbar */
.toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 18px;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f4f4f6;
  border-radius: 10px;
  padding: 0 14px;
}
.search-box i { color: #aaa; }
.search-box input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  padding: 12px 0;
  font-size: 14px;
}

/* Category tabs */
.category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.cat-tab {
  border: 1.5px solid #e1e1e6;
  background: white;
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.18s;
  color: #555;
}
.cat-tab:hover { border-color: #e63946; color: #e63946; }
.cat-tab.active {
  background: #e63946;
  border-color: #e63946;
  color: white;
  font-weight: 600;
}

/* Loading */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #aaa;
  font-size: 15px;
}
.loading-state i { margin-right: 8px; }

/* ===== PRODUCT GRID & CARDS ===== */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.product-card-pos {
  border: 1px solid #e8e8ef;
  border-radius: 12px;
  background: white;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: 0.2s;
}
.product-card-pos:hover {
  border-color: #e63946;
  box-shadow: 0 4px 16px rgba(230,57,70,0.08);
}

.pc-img-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%; /* 1:1 aspect ratio */
  background: #f4f4f6;
}
.pc-img-wrapper img {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  object-fit: cover;
}
.no-img {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  font-size: 40px; color: #ccc;
}
.promo-badge-img {
  position: absolute;
  top: 10px; right: 10px;
  background: #e63946;
  color: white;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: bold;
  box-shadow: 0 2px 8px rgba(230,57,70,0.3);
}

.pc-info-wrapper {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}
.pc-name-pos {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 40px;
}
.pc-price-pos {
  color: #e63946;
  font-weight: 700;
  font-size: 16px;
}

/* Attributes picker */
.attr-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.attr-group label {
  font-size: 12px;
  color: #555;
  font-weight: 600;
}
.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.chip {
  padding: 5px 12px;
  border: 1px solid #e1e1e6;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  background: #fafafc;
  transition: 0.15s;
  user-select: none;
}
.chip:hover:not(.disabled) {
  border-color: #aaa;
}
.chip.active {
  border-color: #e63946;
  color: #e63946;
  background: rgba(230,57,70,0.05);
  font-weight: 700;
}
.chip.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: #f0f0f0;
  color: #aaa;
  text-decoration: line-through;
}

/* Action row */
.action-row {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px dashed #eee;
}
.stock-info {
  font-size: 12px;
  color: #2a9d8f;
  font-weight: 600;
}
.stock-info.out-of-stock {
  color: #e63946;
}

.add-btn {
  background: #e63946;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 8px 14px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}
.add-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(230,57,70,0.2);
}
.add-btn:active:not(:disabled) {
  transform: translateY(0);
}
.add-btn:disabled {
  background: #d1d1d6;
  cursor: not-allowed;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 20px;
  color: #bbb;
}
.empty-state i { font-size: 36px; margin-bottom: 12px; display: block; }

/* ===== CART PANEL ===== */
.cart-panel {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  padding: 20px;
  position: sticky;
  top: 20px;
}
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.cart-header h3 {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
  display: flex;
  align-items: center;
  gap: 8px;
}
.cart-header h3 i { color: #e63946; }
.clear-btn {
  border: none;
  background: transparent;
  color: #e63946;
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 5px;
}
.customer-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f4f4f6;
  border-radius: 8px;
  padding: 0 14px;
  margin-bottom: 14px;
}
.customer-box i { color: #aaa; }
.customer-box input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  padding: 10px 0;
  font-size: 13px;
}
.cart-items {
  min-height: 200px;
  max-height: 340px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
}
.cart-items::-webkit-scrollbar { width: 4px; }
.cart-items::-webkit-scrollbar-thumb { background: #e1e1e6; border-radius: 4px; }
.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #eee;
  border-radius: 10px;
  transition: 0.15s;
}
.cart-item:hover { border-color: #e63946; background: #fffafa; }
.item-info h4 { font-size: 13px; margin: 0 0 3px; color: #1a1a2e; font-weight: 600; }
.variant-label { font-size: 11px; color: #666; margin: 0 0 3px; }
.badge { background: #eef1f6; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: 600; }
.price-label { color: #e63946; font-weight: 700; margin: 0; font-size: 13px; }
.qty-control { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.qty-control button {
  width: 26px; height: 26px;
  border: 1.5px solid #e1e1e6;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  transition: 0.15s;
}
.qty-control button:hover { border-color: #e63946; color: #e63946; }
.qty-control span { font-size: 14px; font-weight: 700; min-width: 20px; text-align: center; }
.empty-cart {
  text-align: center;
  padding: 40px 10px;
  color: #ccc;
  flex: 1;
}
.empty-cart i { font-size: 30px; margin-bottom: 8px; display: block; }
.empty-cart p { margin: 0; font-size: 13px; }

/* Summary */
.summary { border-top: 1px solid #eee; padding-top: 14px; }
.summary > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
}
.voucher-input-group { flex-direction: column !important; align-items: flex-start !important; gap: 6px; }
.input-btn-wrapper { display: flex; width: 100%; gap: 8px; }
.input-btn-wrapper input { flex: 1; border: 1px solid #e1e1e6; border-radius: 6px; padding: 8px 10px; font-size: 13px; }
.input-btn-wrapper button {
  background: #2b2b2b; color: white; border: none;
  padding: 0 14px; border-radius: 6px; cursor: pointer; font-size: 13px; white-space: nowrap;
}
.discount-display {
  background: #edf7f5; padding: 8px 10px; border-radius: 6px; font-size: 13px;
}
.text-success { color: #2a9d8f; font-weight: 600; display: flex; align-items: center; gap: 6px; }
.text-success i { cursor: pointer; color: #e63946; }
.summary .total { font-size: 18px; font-weight: 700; color: #e63946; border-top: 1px dashed #eee; padding-top: 10px; margin-top: 6px; }

/* Payment */
.payment-methods { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 14px 0; }
.payment-methods button {
  border: 1.5px solid #e1e1e6;
  border-radius: 8px; padding: 11px;
  background: white; cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  font-weight: 600; font-size: 13px; transition: 0.15s;
}
.payment-methods button.active { border-color: #e63946; color: #e63946; background: rgba(230,57,70,0.05); }
.checkout-btn {
  width: 100%;
  background: linear-gradient(135deg, #e63946, #f36872);
  color: white; border: none;
  border-radius: 10px; padding: 14px;
  font-weight: 700; cursor: pointer; font-size: 15px;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  transition: 0.2s;
}
.checkout-btn:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); }
.checkout-btn:disabled { opacity: 0.45; cursor: not-allowed; }

@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
</style>