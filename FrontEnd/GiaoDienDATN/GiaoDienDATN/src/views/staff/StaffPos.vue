<template>
  <div class="staff-pos-wrapper">
    <!-- Thanh trạng thái ca -->
    <div v-if="activeSession" class="session-status-bar">
      <div class="session-info">
        <span class="info-item"><i class="fas fa-desktop"></i> <strong>Quầy:</strong> {{ activeSession.cashRegisterId || 1 }}</span>
        <span class="info-item"><i class="fas fa-clock"></i> <strong>Bắt đầu:</strong> {{ formatTime(activeSession.startTime) }}</span>
        <span class="info-item"><i class="fas fa-money-bill-wave"></i> <strong>Tiền đầu ca:</strong> {{ formatPrice(activeSession.startBalance) }}</span>
      </div>
      <button class="btn-close-session" @click="closeSessionPrompt">
        <i class="fas fa-sign-out-alt"></i> Đóng ca
      </button>
    </div>

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
        <button v-for="product in filteredProducts" :key="product.id" class="product-card" @click="openVariantModal(product)">
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
        <div class="header-actions">
          <button class="clear-btn" @click="clearCurrentCart">Xóa giỏ hàng</button>
        </div>
      </div>

      <div class="customer-box">
        <i class="fas fa-user"></i>
        <input 
          v-model="customerIdInput" 
          type="text" 
          placeholder="Nhập mã khách hàng (Nhấn Enter)" 
          @keyup.enter="attachCustomer"
        />
      </div>

      <div class="cart-items">
        <div v-for="item in cart" :key="item.orderDetailId" class="cart-item">
          <div class="item-info">
            <h4>{{ item.productDetail?.product?.name || 'Sản phẩm' }}</h4>
            <p class="variant-label">
              Mã: {{ item.productDetail?.skuDetail }} 
              <span class="badge">Size: {{ item.productDetail?.size }} - Màu: {{ item.productDetail?.color }}</span>
            </p>
            <p class="price-label">{{ formatPrice(item.unitPrice) }}</p>
          </div>
          <div class="qty-control">
            <button @click="updateQty(item, item.quantity - 1)">-</button>
            <span>{{ item.quantity }}</span>
            <button @click="updateQty(item, item.quantity + 1)">+</button>
          </div>
        </div>
        <div v-if="cart.length === 0" class="empty-cart">Chưa có sản phẩm trong hóa đơn</div>
      </div>

      <div class="summary">
        <div><span>Tạm tính</span><strong>{{ formatPrice(subtotal) }}</strong></div>
        <div class="voucher-input-group">
          <span>Mã giảm giá</span>
          <div class="input-btn-wrapper">
            <input 
              v-model="voucherCodeInput" 
              type="text" 
              placeholder="Nhập mã Voucher" 
              @keyup.enter="applyVoucherCode"
            />
            <button @click="applyVoucherCode">Áp dụng</button>
          </div>
        </div>
        <div v-if="discount > 0" class="discount-display">
          <span>Được giảm:</span>
          <span class="text-success">-${formatPrice(discount)} <i class="fas fa-times-circle" @click="removeVoucherCode"></i></span>
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
        <i class="fas fa-receipt"></i> Tạo hóa đơn & In
      </button>
    </aside>

    <div v-if="showModal" class="modal-overlay">
      <div class="variant-modal">
        <div class="modal-header">
          <h3>Chọn thuộc tính sản phẩm</h3>
          <button class="close-modal-btn" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <h4 class="selected-p-name">{{ selectedProduct?.name }}</h4>
          <p class="selected-p-sku">Mã gốc: {{ selectedProduct?.sku }}</p>

          <div class="attribute-group">
            <label>Kích cỡ (Size):</label>
            <div class="option-buttons">
              <button 
                v-for="size in availableSizes" 
                :key="size"
                :class="{ active: selectedSize === size }"
                @click="selectedSize = size"
              >
                {{ size }}
              </button>
            </div>
          </div>

          <div class="attribute-group">
            <label>Màu sắc:</label>
            <div class="option-buttons">
              <button 
                v-for="color in availableColors" 
                :key="color"
                :class="{ active: selectedColor === color }"
                @click="selectedColor = color"
              >
                {{ color }}
              </button>
            </div>
          </div>
          
          <div v-if="matchedVariant" class="variant-info-alert">
            <span>Mã biến thể chi tiết: <b>{{ matchedVariant.skuDetail }}</b></span>
            <span>Tồn kho thực tế: <b :style="{ color: matchedVariant.stock === 0 ? '#e63946' : '#2a9d8f' }">{{ matchedVariant.stock }} sản phẩm</b></span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeModal">Hủy bỏ</button>
          <button class="btn-primary" :disabled="!matchedVariant || matchedVariant.stock <= 0" @click="confirmAddToCart">
            <i class="fas fa-shopping-cart"></i> Xác nhận thêm
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Mở ca -->
    <div v-if="showOpenSessionModal" class="modal-overlay session-modal-overlay">
      <div class="variant-modal session-modal">
        <div class="modal-header">
          <h3>Mở ca bán hàng</h3>
        </div>
        <div class="modal-body">
          <p class="session-instruction">Bạn chưa có ca bán hàng nào đang mở. Vui lòng mở ca để tiếp tục bán hàng.</p>
          <div class="attribute-group">
            <label>Tiền mặt đầu ca (VNĐ):</label>
            <input type="number" v-model="openingBalance" class="form-input" min="0" placeholder="Nhập số tiền đầu ca" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-primary w-100" @click="openSession" :disabled="isOpeningSession">
            <i class="fas fa-play-circle"></i> Bắt đầu ca
          </button>
        </div>
      </div>
    </div>
  </div>
  </div>
</template>


<script setup>
import { computed, ref, onMounted } from "vue";
import { useNotification } from "../../composables/useNotification";
import * as posService from "../../services/posService";
// HOẶC dùng alias @ nếu đã cấu hình (khuyến nghị)
import api from "@/api/axios.js";

const { showNotification } = useNotification();

// --- State cho UI ---
const keyword = ref("");
const category = ref("all");
const customerIdInput = ref("");
const voucherCodeInput = ref("");
const paymentMethod = ref("CASH");

// --- State cho Modal chọn thuộc tính ---
const showModal = ref(false);
const selectedProduct = ref(null);
const selectedSize = ref("");
const selectedColor = ref("");

// --- State cho Dữ liệu ---
const currentOrder = ref(null);
const products = ref([]);
const isLoadingProducts = ref(false);

// --- State cho Phiên bán hàng (Session) ---
const activeSession = ref(null);
const showOpenSessionModal = ref(false);
const openingBalance = ref(0);
const isOpeningSession = ref(false);

/**
 * Hàm load sản phẩm đã sửa để xử lý đúng API của bạn
 */
const loadProducts = async () => {
  isLoadingProducts.value = true;
  try {
    const response = await api.get("/products");
    console.log("Dữ liệu gốc từ API:", response.data);

    // API của bạn trả về cấu trúc: { success: true, message: "OK", data: { content: [...] } }
    let productArray = [];
    
    if (response?.data?.success && response?.data?.data?.content) {
      productArray = response.data.data.content;
      console.log("Đã parse thành công dữ liệu sản phẩm, số lượng:", productArray.length);
    } else {
      console.warn("Cấu trúc API không như mong đợi:", response.data);
      throw new Error("Không thể parse dữ liệu sản phẩm");
    }

    if (!Array.isArray(productArray) || productArray.length === 0) {
      console.warn("Mảng sản phẩm rỗng hoặc không hợp lệ.");
      throw new Error("Không có sản phẩm nào được tải về.");
    }

    // Ánh xạ dữ liệu với xử lý đúng cho variants
    products.value = productArray.map(p => {
      // Xử lý variants an toàn
 // Trong loadProducts, khi map variants:
const variants = (p.variants || []).map(v => ({
  id: v.productDetailId,  // ← QUAN TRỌNG: lưu productDetailId
  skuDetail: v.sku || `SKU-${v.productDetailId}`,
  size: v.size || "N/A",
  color: v.color || "N/A",
  stock: v.quantity || 0,
  price: v.price || 0,
  discountedPrice: v.discountedPrice || v.price || 0
}));

      // Lấy variant đầu tiên để hiển thị thông tin mặc định
      const firstVariant = variants[0];
      
      // Tính tổng tồn kho từ tất cả variants
      const totalStock = variants.reduce((sum, v) => sum + v.stock, 0);
      
      // Xác định giá hiển thị (giá đã giảm nếu có)
      const displayPrice = firstVariant?.discountedPrice || firstVariant?.price || 0;
      
      // Xác định SKU hiển thị (lấy từ variant đầu tiên)
      const displaySku = firstVariant?.skuDetail || p.productName || "N/A";
      
      // Xác định icon dựa trên category
      let icon = 'fas fa-box';
      if (p.category?.categoryName?.toLowerCase().includes('polo') || 
          p.category?.categoryName?.toLowerCase().includes('áo')) {
        icon = 'fas fa-tshirt';
      } else if (p.category?.categoryName?.toLowerCase().includes('quần')) {
        icon = 'fas fa-vest';
      }

      return {
        id: p.productId,
        name: p.productName || "Sản phẩm không tên",
        sku: displaySku,
        category: String(p.category?.categoryId || "all"),
        categoryName: p.category?.categoryName || "",
        price: displayPrice,
        stock: totalStock,
        icon: icon,
        variants: variants,
        // Thông tin bổ sung
        description: p.description || "",
        status: p.status || "ACTIVE",
        hasPromotion: p.promotion ? true : false,
        promotionName: p.promotion?.promotionName || "",
        promotionPercent: p.promotion?.promotionDiscountPercent || 0
      };
    });
    
    console.log("Sản phẩm sau khi map:", products.value);
    showNotification(`Đã tải ${products.value.length} sản phẩm thành công!`, "success");

  } catch (err) {
    console.error("Lỗi khi tải sản phẩm:", err);
    showNotification("Tải sản phẩm thất bại, đang dùng dữ liệu mẫu.", "error");
    
    // Mock data dự phòng
    products.value = [
      {
        id: 1, 
        name: "Polo Premium 2026 (Mock)", 
        sku: "POLO-NAVY-L", 
        category: "1", 
        categoryName: "Áo Polo",
        price: 350000, 
        stock: 100,
        icon: 'fas fa-tshirt',
        description: "Chất liệu vải cá sấu co giãn 4 chiều",
        status: "ACTIVE",
        hasPromotion: false,
        promotionName: "",
        promotionPercent: 0,
        variants: [
          { 
            id: 1, 
            skuDetail: "POLO-NAVY-L", 
            size: "Large", 
            color: "Xanh Navy", 
            stock: 100, 
            price: 350000,
            discountedPrice: 350000
          }
        ]
      },
      {
        id: 2, 
        name: "Áo Polo AVO (Mock)", 
        sku: "SKU-1781417182218-0", 
        category: "1", 
        categoryName: "Áo Polo",
        price: 222000, 
        stock: 200,
        icon: 'fas fa-tshirt',
        description: "",
        status: "ACTIVE",
        hasPromotion: true,
        promotionName: "Hè 2026",
        promotionPercent: 26,
        variants: [
          { 
            id: 2, 
            skuDetail: "SKU-1781417182218-0", 
            size: "Small", 
            color: "Đen", 
            stock: 100, 
            price: 300000,
            discountedPrice: 222000
          },
          { 
            id: 3, 
            skuDetail: "SKU-1781417338419-1", 
            size: "Small", 
            color: "Trắng", 
            stock: 100, 
            price: 300000,
            discountedPrice: 222000
          }
        ]
      }
    ];
  } finally {
    isLoadingProducts.value = false;
  }
};

// Cập nhật lại số tiền giỏ hàng
const recalculateCart = () => {
  if (!currentOrder.value) return;
  let sub = 0;
  currentOrder.value.orderDetails.forEach(item => {
    sub += item.unitPrice * item.quantity;
  });
  currentOrder.value.subtotalAmount = sub;
  const discountVal = currentOrder.value.discountAmount || 0;
  currentOrder.value.totalAmount = Math.max(0, sub - discountVal);
};

// Hàm khởi tạo POS Order
const initPOSOrder = async () => {
  try {
    currentOrder.value = {
      orderId: "TEMP_" + Date.now(),
      orderDetails: [],
      subtotalAmount: 0,
      discountAmount: 0,
      totalAmount: 0,
      customerId: null,
      voucherCode: null
    };
    console.log("Tạo order mới:", currentOrder.value);
  } catch (error) {
    console.error("Lỗi khởi tạo POS Order:", error);
    showNotification("Không thể khởi tạo đơn hàng: " + error, "error");
  }
};

const checkActiveSession = async () => {
  try {
    // Lấy ca đang mở của nhân viên hiện tại (được xác định bởi backend qua token)
    const res = await posService.getMyActiveSession();
    
    let sessionObj = null;
    if (res && res.posSessionId) {
      sessionObj = res;
    } else if (res && res.data && res.data.posSessionId) {
      sessionObj = res.data;
    }

    if (sessionObj && sessionObj.posSessionId) {
      activeSession.value = sessionObj;
      showOpenSessionModal.value = false;
    } else {
      showOpenSessionModal.value = true;
    }
  } catch(error) {
    console.warn("Chưa có ca làm việc hoặc lỗi kết nối:", error);
    showOpenSessionModal.value = true;
  }
};

const openSession = async () => {
  isOpeningSession.value = true;
  try {
    const request = {
      cashRegisterId: 1, // Mặc định quầy 1
      openingBalance: openingBalance.value || 0
    };
    const res = await posService.openPosSession(request);
    
    let sessionObj = null;
    if (res && res.posSessionId) {
      sessionObj = res;
    } else if (res && res.data && res.data.posSessionId) {
      sessionObj = res.data;
    }

    if (sessionObj && sessionObj.posSessionId) {
      activeSession.value = sessionObj;
      showOpenSessionModal.value = false;
      showNotification("Mở ca thành công!", "success");
    } else {
      showNotification("Mở ca thất bại! Vui lòng thử lại.", "error");
    }
  } catch(error) {
    console.error("Lỗi mở ca:", error);
    showNotification("Lỗi: " + (error.response?.data?.message || error.message), "error");
  } finally {
    isOpeningSession.value = false;
  }
};

const closeSessionPrompt = async () => {
  if (confirm("Bạn có chắc chắn muốn đóng ca làm việc này không?")) {
    try {
      const res = await posService.closePosSession(activeSession.value.posSessionId, 1);
      
      let sessionObj = null;
      if (res && res.posSessionId) {
        sessionObj = res;
      } else if (res && res.data && res.data.posSessionId) {
        sessionObj = res.data;
      }

      if (sessionObj && sessionObj.posSessionId) {
        showNotification("Đóng ca thành công!", "success");
        activeSession.value = null;
        await clearCurrentCart();
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      } else {
        // Fallback in case of successful response but missing posSessionId
        showNotification("Đóng ca thành công!", "success");
        activeSession.value = null;
        await clearCurrentCart();
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      }
    } catch(error) {
      console.error("Lỗi đóng ca:", error);
      showNotification("Lỗi: " + (error.response?.data?.message || error.message), "error");
    }
  }
};

onMounted(() => {
  checkActiveSession();
  loadProducts();
  initPOSOrder();
});

// Xử lý bộ lọc tìm kiếm sản phẩm
const filteredProducts = computed(() => {
  const search = keyword.value.toLowerCase();
  return products.value.filter((product) => {
    const matchesCategory = category.value === "all" || product.category === category.value;
    const matchesSearch = 
      product.name.toLowerCase().includes(search) || 
      product.sku.toLowerCase().includes(search) ||
      product.categoryName.toLowerCase().includes(search);
    return matchesCategory && matchesSearch;
  });
});

// Trích xuất thuộc tính phục vụ Modal chọn Size/Màu
const availableSizes = computed(() => {
  if (!selectedProduct.value?.variants) return [];
  return [...new Set(selectedProduct.value.variants.map(v => v.size))].filter(Boolean);
});

const availableColors = computed(() => {
  if (!selectedProduct.value?.variants) return [];
  return [...new Set(selectedProduct.value.variants.map(v => v.color))].filter(Boolean);
});

const matchedVariant = computed(() => {
  if (!selectedProduct.value || !selectedSize.value || !selectedColor.value) return null;
  return selectedProduct.value.variants.find(
    v => v.size === selectedSize.value && v.color === selectedColor.value
  ) || null;
});

// Mở Modal chọn thuộc tính
const openVariantModal = (product) => {
  console.log("Mở modal cho sản phẩm:", product);
  selectedProduct.value = product;
  showModal.value = true;
  
  if (product.variants && product.variants.length > 0) {
    // Set giá trị mặc định
    const defaultVariant = product.variants[0];
    selectedSize.value = defaultVariant.size || "";
    selectedColor.value = defaultVariant.color || "";
    console.log("Set mặc định Size:", selectedSize.value, "Color:", selectedColor.value);
  }
};

const closeModal = () => {
  showModal.value = false;
  selectedProduct.value = null;
  selectedSize.value = "";
  selectedColor.value = "";
};

// Thêm biến thể vào giỏ hàng

// Thêm biến thể vào giỏ hàng
const confirmAddToCart = async () => {
  if (!currentOrder.value || !matchedVariant.value) {
    showNotification("Vui lòng chọn đầy đủ thuộc tính sản phẩm!", "warning");
    return;
  }
  
  try {
    const variant = matchedVariant.value;
    const existing = currentOrder.value.orderDetails.find(
      i => i.productDetailId === variant.id  // ← SỬA: so sánh theo id
    );

    if (existing) {
      existing.quantity += 1;
    } else {
      currentOrder.value.orderDetails.push({
        orderDetailId: "ITEM_" + Date.now(),
        productDetailId: variant.id,  // ← THÊM: id của product_detail
        productDetail: {
          id: variant.id,
          skuDetail: variant.skuDetail,
          size: variant.size,
          color: variant.color,
          product: { name: selectedProduct.value.name }
        },
        quantity: 1,
        unitPrice: variant.discountedPrice || variant.price
      });
    }
    recalculateCart();
    showNotification(`Đã thêm ${selectedProduct.value.name} (${variant.size}/${variant.color}) vào giỏ hàng!`, "success");
    closeModal();
  } catch (error) {
    console.error("Lỗi thêm vào giỏ hàng:", error);
    showNotification("Không thể thêm vào giỏ hàng: " + error, "error");
  }
};

// Computed cho giỏ hàng
const cart = computed(() => currentOrder.value?.orderDetails || []);
const subtotal = computed(() => currentOrder.value?.subtotalAmount || 0);
const discount = computed(() => currentOrder.value?.discountAmount || 0);
const total = computed(() => currentOrder.value ? Math.max(subtotal.value - discount.value, 0) : 0);

// Format tiền tệ
const formatPrice = (value) => {
  const numValue = Number(value) || 0;
  return new Intl.NumberFormat("vi-VN").format(numValue) + "₫";
};

// Format thời gian
const formatTime = (timeStr) => {
  if (!timeStr) return "--:--";
  try {
    const date = new Date(timeStr);
    return date.toLocaleString("vi-VN");
  } catch(e) {
    return timeStr;
  }
};

// Cập nhật số lượng
const updateQty = async (item, newQty) => {
  if (!currentOrder.value) return;
  
  if (newQty <= 0) {
    currentOrder.value.orderDetails = currentOrder.value.orderDetails.filter(
      i => i.orderDetailId !== item.orderDetailId
    );
    showNotification("Đã xóa sản phẩm khỏi giỏ hàng!", "info");
  } else {
    const existing = currentOrder.value.orderDetails.find(
      i => i.orderDetailId === item.orderDetailId
    );
    if (existing) {
      existing.quantity = newQty;
    }
  }
  recalculateCart();
};

// Gắn khách hàng
const attachCustomer = async () => {
  if (!currentOrder.value || !customerIdInput.value) {
    showNotification("Vui lòng nhập mã khách hàng!", "warning");
    return;
  }
  
  currentOrder.value.customerId = customerIdInput.value;
  showNotification("Đã gắn thông tin thành viên vào đơn hàng!", "success");
};

// Áp dụng voucher
const applyVoucherCode = async () => {
  if (!currentOrder.value || !voucherCodeInput.value) {
    showNotification("Vui lòng nhập mã giảm giá!", "warning");
    return;
  }
  
  // Tạm thời áp dụng giảm giá mẫu do không có API quản lý giỏ hàng
  currentOrder.value.voucherCode = voucherCodeInput.value;
  currentOrder.value.discountAmount = 50000; // Giảm mẫu 50k
  recalculateCart();
  showNotification("Áp dụng mã giảm giá thành công!", "success");
};

// Xóa voucher
const removeVoucherCode = async () => {
  if (!currentOrder.value) return;
  
  currentOrder.value.voucherCode = null;
  currentOrder.value.discountAmount = 0;
  voucherCodeInput.value = "";
  recalculateCart();
  showNotification("Đã hủy bỏ áp dụng mã giảm giá!", "success");
};

// Xóa giỏ hàng hiện tại
const clearCurrentCart = async () => {
  if (!currentOrder.value) return;
  
  voucherCodeInput.value = "";
  customerIdInput.value = "";
  await initPOSOrder();
  showNotification("Đã hủy hóa đơn chờ hiện tại!", "success");
};

// Thanh toán

// Thanh toán
const checkout = async () => {
  if (!currentOrder.value || cart.value.length === 0) {
    showNotification("Giỏ hàng trống, không thể thanh toán!", "warning");
    return;
  }
  
  try {
    let sessionId = activeSession.value?.posSessionId;
    if (!sessionId) {
      showNotification("Bạn chưa mở ca bán hàng!", "warning");
      return;
    }

    const request = {
      posSessionId: sessionId,
      posOrderCustomerId: customerIdInput.value ? parseInt(customerIdInput.value) : null,
      posOrderCustomerName: customerIdInput.value ? null : "Khách lẻ",
      posOrderCustomerPhone: null,
      posOrderType: "AT_COUNTER",
      posOrderDiscountAmount: discount.value || 0,
      posOrderTaxAmount: 0,
      posOrderNotes: "Bán lẻ tại quầy",
      items: currentOrder.value.orderDetails.map(item => ({
         productDetailId: item.productDetailId,  // ← SỬA: cần productDetailId, không phải skuDetail
         quantity: item.quantity,
         unitPrice: item.unitPrice || 0,
         discount: 0
      })),
      payments: [
        {
           paymentMethodId: paymentMethod.value === "CASH" ? 1 : 2,  // ← SỬA: paymentMethodId
           amount: total.value || 0  // ← SỬA: amount (không phải posPaymentAmount)
        }
      ]
    };

    console.log("Request thanh toán:", request);  // Debug

    const res = await posService.createPosOrder(request);
    
    let orderObj = null;
    if (res && res.posOrderId) {
      orderObj = res;
    } else if (res && res.data && res.data.data && res.data.data.posOrderId) {
      orderObj = res.data.data;
    } else if (res && res.data && res.data.posOrderId) {
      orderObj = res.data;
    } else {
      orderObj = res.data || res;
    }

    showNotification("Thanh toán thành công!", "success");

    // Lấy thông tin order code
    const invoiceNumber = orderObj?.posOrderInvoiceNumber || "HD-" + Date.now();
    const orderDate = orderObj?.posOrderCreatedAt ? formatTime(orderObj.posOrderCreatedAt) : formatTime(new Date());

    // In hóa đơn
    const invoiceData = {
      orderId: invoiceNumber,
      orderDate: orderDate,
      customerName: customerIdInput.value ? ("KH: " + customerIdInput.value) : "Khách lẻ",
      storeName: "FASHION STORE",
      storeAddress: "123 Đường Fashion, Quận 1, TP.HCM",
      storePhone: "1900 1234",
      items: currentOrder.value.orderDetails.map(item => ({
        productDetail: {
          skuDetail: item.productDetail?.skuDetail || "SKU",
          size: item.productDetail?.size || "N/A",
          color: item.productDetail?.color || "N/A"
        },
        quantity: item.quantity,
        finalAmount: (item.unitPrice || 0) * item.quantity
      })),
      subtotalAmount: subtotal.value,
      discountAmount: discount.value,
      finalAmount: total.value
    };

    printInvoiceHandle(invoiceData);

    // Reset giỏ hàng
    await clearCurrentCart();

  } catch (error) {
    console.error("Lỗi thanh toán:", error);
    showNotification("Thanh toán thất bại: " + (error.response?.data?.message || error.message), "error");
  }
};


// In hóa đơn
const printInvoiceHandle = (invoice) => {
  const printWindow = window.open("", "_blank");
  printWindow.document.write(`
    <html>
      <head>
        <title>In hóa đơn #${invoice.orderId}</title>
        <style>
          body { font-family: 'Courier New', monospace; width: 80mm; padding: 4px; font-size: 12px; line-height: 1.4;}
          .text-center { text-align: center; }
          .bold { font-weight: bold; }
          .header { font-size: 15px; margin-bottom: 8px; }
          table { width: 100%; border-collapse: collapse; margin: 8px 0; }
          th, td { text-align: left; padding: 4px 0; font-size: 11px; }
          .total-section { border-top: 1px dashed #000; padding-top: 6px; margin-top: 6px; }
          .total-section p { margin: 4px 0; display: flex; justify-content: space-between; }
        </style>
      </head>
      <body>
        <div class="text-center bold header">${invoice.storeName}</div>
        <div class="text-center">${invoice.storeAddress}</div>
        <div class="text-center">Hotline: ${invoice.storePhone}</div>
        <p>------------------------------------------</p>
        <p><b>Mã HĐ:</b> #${invoice.orderId}</p>
        <p><b>Thời gian:</b> ${invoice.orderDate}</p>
        <p><b>Khách hàng:</b> ${invoice.customerName}</p>
        <p>------------------------------------------</p>
        <table>
          <thead>
            <tr><th>Mặt hàng (SKU)</th><th style="text-align:center">SL</th><th style="text-align:right">T.Tiền</th></tr>
          </thead>
          <tbody>
            ${invoice.items.map(item => `
              <tr>
                <td>${item.productDetail.skuDetail}<br><small>(${item.productDetail.size}/${item.productDetail.color})</small></td>
                <td style="text-align:center">${item.quantity}</td>
                <td style="text-align:right">${new Intl.NumberFormat("vi-VN").format(item.finalAmount)}đ</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
        <div class="total-section">
          <p><span>Tạm tính:</span> <span>${new Intl.NumberFormat("vi-VN").format(invoice.subtotalAmount)}đ</span></p>
          <p><span>Giảm giá:</span> <span>-${new Intl.NumberFormat("vi-VN").format(invoice.discountAmount)}đ</span></p>
          <p class="bold" style="font-size: 13px;"><span>THÀNH TOÁN:</span> <span>${new Intl.NumberFormat("vi-VN").format(invoice.finalAmount)}đ</span></p>
        </div>
        <p>------------------------------------------</p>
        <div class="text-center bold">CẢM ƠN QUÝ KHÁCH HẸN GẶP LẠI!</div>
      </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.focus();
  setTimeout(() => {
    printWindow.print();
    printWindow.close();
  }, 400);
};
</script>



<style scoped>
.staff-pos-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
  animation: fadeIn 0.3s;
}
.session-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 12px 25px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.session-info {
  display: flex;
  gap: 25px;
  color: #2b2b2b;
  font-size: 14px;
}
.session-info .info-item i {
  color: #2a9d8f;
  margin-right: 6px;
}
.btn-close-session {
  background: #e63946;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  transition: 0.2s;
}
.btn-close-session:hover {
  background: #d62828;
}
.staff-pos {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 25px;
}
.product-panel, .cart-panel {
  background: white; border-radius: 15px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 25px;
}
.toolbar { display: flex; gap: 15px; margin-bottom: 20px; }
.search-box, .customer-box {
  flex: 1; display: flex; align-items: center; gap: 10px;
  background: #f4f4f6; border-radius: 8px; padding: 0 14px;
}
.search-box input, .customer-box input {
  width: 100%; border: none; background: transparent; outline: none; padding: 12px 0;
}
.toolbar select { border: 1px solid #e1e1e6; border-radius: 8px; padding: 0 12px; outline: none; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 15px; }
.product-card {
  border: 1px solid #e1e1e6; border-radius: 12px; background: white; text-align: left; padding: 16px; cursor: pointer; transition: 0.2s;
}
.product-card:hover { border-color: #e63946; transform: translateY(-2px); }
.product-thumb {
  width: 46px; height: 46px; border-radius: 50%; background: rgba(230, 57, 70, 0.1);
  color: #e63946; display: flex; align-items: center; justify-content: center; margin-bottom: 12px;
}
.product-name { color: #2b2b2b; font-weight: 700; margin-bottom: 5px; }
.product-meta { color: #888; font-size: 12px; margin-bottom: 10px; }
.product-price { color: #e63946; font-weight: 700; }
.cart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.clear-btn { border: none; background: transparent; color: #e63946; cursor: pointer; font-weight: 600; }
.cart-items { margin: 18px 0; display: flex; flex-direction: column; gap: 12px; min-height: 240px; max-height: 400px; overflow-y: auto; }
.cart-item { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px; border: 1px solid #e1e1e6; border-radius: 10px; }
.item-info h4 { font-size: 14px; margin: 0 0 4px 0; color: #2b2b2b; }
.variant-label { font-size: 11px; color: #666; margin: 0 0 4px 0; }
.badge { background: #eef1f6; padding: 2px 6px; border-radius: 4px; font-weight: 500; }
.price-label { color: #e63946; font-weight: 600; margin: 0; font-size: 13px; }
.qty-control { display: flex; align-items: center; gap: 8px; }
.qty-control button { width: 28px; height: 28px; border: 1px solid #e1e1e6; background: white; border-radius: 6px; cursor: pointer; }
.empty-cart { text-align: center; padding: 60px 10px; color: #888; }
.summary { border-top: 1px solid #e1e1e6; padding-top: 16px; }
.summary div { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.voucher-input-group { display: flex; flex-direction: column; align-items: flex-start !important; gap: 6px; }
.input-btn-wrapper { display: flex; width: 100%; gap: 8px; }
.input-btn-wrapper input { flex: 1; border: 1px solid #e1e1e6; border-radius: 6px; padding: 8px; }
.input-btn-wrapper button { background: #2b2b2b; color: white; border: none; padding: 0 14px; border-radius: 6px; cursor: pointer; font-size: 13px; }
.discount-display { display: flex; justify-content: space-between; font-size: 13px; background: #edf7f5; padding: 8px; border-radius: 6px; }
.text-success { color: #2a9d8f; font-weight: 600; display: flex; align-items: center; gap: 6px; }
.text-success i { cursor: pointer; color: #e63946; }
.summary .total { font-size: 19px; color: #e63946; border-top: 1px dashed #e1e1e6; padding-top: 12px; margin-top: 8px; }
.payment-methods { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 18px 0; }
.payment-methods button {
  border: 1px solid #e1e1e6; border-radius: 8px; padding: 12px; background: white; cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px; font-weight: 600;
}
.payment-methods button.active { border-color: #e63946; color: #e63946; background: rgba(230, 57, 70, 0.06); }
.checkout-btn {
  width: 100%; background: linear-gradient(135deg, #e63946, #f36872); color: white; border: none;
  border-radius: 8px; padding: 14px; font-weight: bold; cursor: pointer; font-size: 15px;
}
.checkout-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* MODAL OVERLAY */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.45);
  display: flex; align-items: center; justify-content: center; z-index: 9999;
}
.variant-modal {
  background: white; border-radius: 12px; width: 440px; box-shadow: 0 8px 30px rgba(0,0,0,0.15); overflow: hidden;
}
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #e1e1e6; }
.close-modal-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: #888; }
.modal-body { padding: 20px; }
.selected-p-name { font-size: 16px; margin: 0 0 4px 0; color: #2b2b2b; }
.selected-p-sku { font-size: 12px; color: #888; margin: 0 0 16px 0; }
.attribute-group { margin-bottom: 16px; }
.attribute-group label { display: block; font-weight: 600; margin-bottom: 8px; font-size: 13px; }
.option-buttons { display: flex; gap: 8px; flex-wrap: wrap; }
.option-buttons button { padding: 6px 14px; border: 1px solid #e1e1e6; background: white; border-radius: 6px; cursor: pointer; font-size: 13px; }
.option-buttons button.active { border-color: #e63946; color: #e63946; background: rgba(230, 57, 70, 0.06); }
.variant-info-alert { background: #f8f9fa; padding: 12px; border-radius: 8px; margin-top: 15px; display: flex; flex-direction: column; gap: 4px; font-size: 12px; }
.modal-footer { padding: 14px 20px; background: #f8f9fa; border-top: 1px solid #e1e1e6; display: flex; justify-content: flex-end; gap: 10px; }
.modal-footer button { padding: 8px 16px; border-radius: 6px; cursor: pointer; font-weight: 600; border: none; font-size: 13px; }
.btn-secondary { background: #e4e4e7; color: #2b2b2b; }
.btn-primary { background: #e63946; color: white; }
.btn-primary:disabled { background: #ccc; cursor: not-allowed; }

.header-actions { display: flex; gap: 10px; align-items: center; }
.close-session-btn { border: 1px solid #e63946; background: white; color: #e63946; padding: 4px 10px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 13px; }
.close-session-btn:hover { background: #e63946; color: white; }
.session-badge { font-size: 11px; color: #2a9d8f; font-weight: normal; background: #e6f7f4; padding: 2px 6px; border-radius: 4px; margin-left: 8px; }
.form-input { width: 100%; border: 1px solid #e1e1e6; border-radius: 6px; padding: 10px; font-size: 14px; outline: none; margin-top: 6px; }
.session-instruction { font-size: 14px; color: #666; margin-bottom: 20px; line-height: 1.5; }
.w-100 { width: 100%; justify-content: center; }
.session-modal-overlay { background: rgba(0,0,0,0.7) !important; backdrop-filter: blur(4px); }

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
</style>