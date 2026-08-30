<template>
  <div class="staff-pos-wrapper">
    <div class="staff-pos">

      <!-- ===== PANEL TRÁI: Danh sách sản phẩm ===== -->
      <section class="product-panel">
        <div class="toolbar">
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input v-model="keyword" type="text" placeholder="Tìm theo tên sản phẩm, SKU, phân loại..." />
          </div>
          <div class="category-filter">
            <span class="category-filter-label"><i class="fas fa-layer-group"></i> Danh mục</span>
            <div class="category-tabs">
            <button
              v-for="cat in categoryList"
              :key="cat.id"
              :class="['cat-tab', { active: category === cat.id }]"
              @click="category = cat.id"
            >
              {{ cat.name }} <span class="cat-count">{{ cat.count }}</span>
            </button>
            </div>
          </div>
        </div>

        <div v-if="isLoadingProducts" class="loading-state">
          <i class="fas fa-spinner fa-spin"></i> Đang tải sản phẩm...
        </div>

        <div v-else class="product-grid">
          <div v-for="product in filteredProducts" :key="product.id" class="product-card-pos">
            <div class="pc-img-wrapper">
              <img v-if="product.mainImageUrl" :src="getImageUrl(product.mainImageUrl)" :alt="product.name" @error="product.mainImageUrl = null" />
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

        <!-- ===== TÌM KIẾM KHÁCH HÀNG ===== -->
        <div class="customer-search-box">
          <div class="search-row">
            <input 
              v-model="customerSearch" 
              type="text" 
              placeholder="Tìm khách hàng theo tên hoặc SĐT..."
              @input="searchCustomerList"
              @focus="loadCustomerList"
            />
            <button class="btn-sm btn-primary" @click="loadCustomerList" title="Làm mới">
              <i class="fas fa-sync"></i>
            </button>
          </div>
          <div v-if="customerList.length > 0" class="customer-list">
            <div 
              v-for="cust in customerList" 
              :key="cust.customerId"
              class="customer-item"
              @click="selectCustomer(cust)"
            >
              <span class="cust-name">{{ cust.customerName }}</span>
              <span class="cust-phone">{{ cust.phone }}</span>
              <span class="cust-id">#{{ cust.customerId }}</span>
            </div>
          </div>
        </div>

        <!-- ===== THÔNG TIN KHÁCH HÀNG ===== -->
        <div class="customer-info-box">
          <div class="customer-info-row">
            <div class="info-field">
              <label>Tên khách hàng</label>
              <input 
                v-model="customerName" 
                type="text" 
                placeholder="Nhập tên khách hàng"
                @input="handleCustomerNameChange"
              />
            </div>
            <div class="info-field">
              <label>Số điện thoại <span class="required">*</span></label>
              <input 
                v-model="customerPhone" 
                type="text" 
                placeholder="Nhập SĐT khách hàng"
                @blur="checkCustomerByPhone"
                @input="checkCustomerByPhoneRealtime"
                :class="{ 'error': phoneError }"
              />
              <span v-if="phoneError" class="error-text">{{ phoneError }}</span>
            </div>
          </div>
          <div v-if="loadingPhone" class="customer-found info">
            <i class="fas fa-spinner fa-spin"></i>
            <span>Đang kiểm tra số điện thoại...</span>
          </div>
          <div v-else-if="foundCustomer" class="customer-found success">
            <i class="fas fa-check-circle"></i>
            <span>Đã tìm thấy khách hàng: <strong>{{ foundCustomer.customerName }}</strong></span>
            <span class="customer-id">(ID: #{{ foundCustomer.customerId }})</span>
          </div>
          <div v-else-if="customerPhone && customerPhone.length >= 10 && !loadingPhone && !phoneError" class="customer-found info">
            <i class="fas fa-user-plus"></i>
            <span>Sẽ tạo khách hàng mới với SĐT: <strong>{{ customerPhone }}</strong></span>
          </div>
          <div v-else-if="customerList.length > 0 && phoneError" class="customer-found warning">
            <i class="fas fa-exclamation-triangle"></i>
            <span>{{ phoneError }}</span>
            <span class="customer-id">Vui lòng chọn từ danh sách bên trên</span>
          </div>
        </div>

        <!-- ===== GIỎ HÀNG ===== -->
        <div class="cart-items">
          <div v-for="item in cart" :key="item.orderDetailId" class="cart-item">
            <div class="item-info">
              <h4>{{ item.productDetail?.product?.name || 'Sản phẩm' }}</h4>
              <p class="variant-label">
                {{ item.productDetail?.skuDetail }}
                <span class="variant-badges">
                  <span class="variant-badge size-badge"><b>Size</b> {{ item.productDetail?.size }}</span>
                  <span class="variant-badge color-badge"><b>Màu</b> {{ item.productDetail?.color }}</span>
                </span>
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

        <!-- ===== TÓM TẮT ===== -->
        <div class="summary">
          <div><span>Tạm tính</span><strong>{{ formatPrice(subtotal) }}</strong></div>
          <div class="voucher-input-group">
            <span>Mã giảm giá</span>
            <div class="input-btn-wrapper">
              <input v-model="voucherCodeInput" type="text" placeholder="Nhập mã voucher" @keyup.enter="applyVoucherCode" />
              <button @click="applyVoucherCode">Áp dụng</button>
            </div>
          </div>
          <div v-if="discount > 0 && currentOrder?.appliedVoucher" class="discount-display">
            <span>Được giảm:</span>
            <span class="text-success">
              -{{ formatPrice(discount) }}
              <i class="fas fa-times-circle" @click="removeVoucherCode"></i>
            </span>
            <div class="voucher-info">
              <span class="voucher-code">{{ currentOrder.appliedVoucher.voucherCode }}</span>
              <span class="voucher-detail" v-if="currentOrder.appliedVoucher.voucherDiscountPercent">
                ({{ currentOrder.appliedVoucher.voucherDiscountPercent }}%)
              </span>
            </div>
          </div>
          <div class="total"><span>Thanh toán</span><strong>{{ formatPrice(total) }}</strong></div>
        </div>

        <!-- ===== PHƯƠNG THỨC THANH TOÁN ===== -->
        <div class="payment-methods">
          <button :class="{ active: paymentMethod === 'CASH' }" @click="paymentMethod = 'CASH'">
            <i class="fas fa-money-bill-wave"></i> Tiền mặt
          </button>
          <button :class="{ active: paymentMethod === 'VNPAY' }" @click="paymentMethod = 'VNPAY'" class="vnpay-btn">
            <i class="fas fa-link"></i> VNPay
          </button>
        </div>

        <!-- Cash input -->
        <div v-if="paymentMethod === 'CASH'" class="cash-input-group">
          <label>Khách đưa:</label>
          <div class="cash-input-row">
            <input v-model="cashGiven" type="number" min="0" placeholder="Nhập số tiền" />
            <span class="change-badge" v-if="cashGiven">
              Trả lại: <strong>{{ formatPrice(Math.max(0, cashGiven - total)) }}</strong>
            </span>
          </div>
          <div class="quick-cash">
            <button v-for="amt in quickAmounts" :key="amt" @click="cashGiven = amt" :class="{ sel: cashGiven === amt }">
              {{ formatPrice(amt) }}
            </button>
          </div>
        </div>

        <!-- VNPay info -->
        <div v-if="paymentMethod === 'VNPAY'" class="vnpay-info-block">
          <i class="fas fa-info-circle"></i>
          <span>Nhấn thanh toán để chuyển đến trang VNPay.</span>
        </div>

        <!-- ===== NÚT THANH TOÁN ===== -->
        <button 
          class="checkout-btn" 
          :class="{ 'vnpay-checkout': paymentMethod === 'VNPAY' }" 
          :disabled="isCheckoutDisabled"
          @click="checkout"
        >
          <span v-if="!isProcessing">
            <i :class="paymentMethod === 'VNPAY' ? 'fas fa-external-link-alt' : 'fas fa-receipt'"></i>
            {{ paymentMethod === 'VNPAY' ? 'Thanh toán qua VNPay' : 'Thanh toán & In HĐ' }}
          </span>
          <span v-else><i class="fas fa-spinner fa-spin"></i> Đang xử lý...</span>
        </button>
      </aside>

      <!-- ===== VNPAY OVERLAY ===== -->
      <transition name="modal-fade">
        <div v-if="vnpayRedirecting" class="modal-overlay">
          <div class="vnpay-redirect-card">
            <div class="vnpay-redirect-header">
              <i class="fas fa-shield-alt"></i>
              <span>Chuyển hướng VNPay</span>
            </div>
            <div class="vnpay-redirect-body">
              <div class="redirect-spinner">
                <i class="fas fa-spinner fa-spin"></i>
              </div>
              <p class="redirect-amount">{{ formatPrice(total) }}</p>
              <p class="redirect-hint">Đang kết nối đến cổng thanh toán VNPay...</p>
              <p class="redirect-sub">Trình duyệt sẽ tự chuyển hướng trong giây lát</p>
            </div>
          </div>
        </div>
      </transition>

      <!-- ===== VNPAY RESULT ===== -->
      <transition name="modal-fade">
        <div v-if="vnpayResult.show" class="modal-overlay">
          <div class="vnpay-result-card">
            <!-- Success -->
            <div v-if="vnpayResult.success" class="result-content success">
              <div class="result-icon success-icon">
                <i class="fas fa-check-circle"></i>
              </div>
              <h3>Thanh toán thành công!</h3>
              <p class="result-amount">{{ formatPrice(vnpayResult.amount) }}</p>
              <div class="result-meta">
                <div class="meta-row">
                  <span>Mã giao dịch</span>
                  <code>{{ vnpayResult.txnRef }}</code>
                </div>
                <div class="meta-row">
                  <span>Thời gian</span>
                  <strong>{{ vnpayResult.paidAt }}</strong>
                </div>
              </div>
              <button class="btn-print-receipt" @click="finishAfterVnpay">
                <i class="fas fa-receipt"></i> Đóng & In hóa đơn
              </button>
            </div>

            <!-- Failed -->
            <div v-else class="result-content failed">
              <div class="result-icon failed-icon">
                <i class="fas fa-times-circle"></i>
              </div>
              <h3>Thanh toán thất bại</h3>
              <p class="result-error-msg">{{ vnpayResult.errorMsg || 'Giao dịch không thành công hoặc đã bị hủy.' }}</p>
              <div class="failed-actions">
                <button class="btn-retry-vnpay" @click="retryVnpay">
                  <i class="fas fa-redo"></i> Thử lại
                </button>
                <button class="btn-cancel-vnpay" @click="cancelVnpayOrder">
                  <i class="fas fa-times"></i> Hủy đơn
                </button>
              </div>
            </div>
          </div>
        </div>
      </transition>

    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, reactive } from "vue";
import { useNotification } from "../../composables/useNotification";
import { createPosOrder, printPosReceipt } from "../../services/posService";
import api from "@/api/axios.js";

const { showNotification } = useNotification();

const getImageUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  return `http://localhost:8080/api/images/${url}`;
};

// --- State UI ---
const keyword = ref("");
const category = ref("all");
const paymentMethod = ref("CASH");
const isProcessing = ref(false);
const cashGiven = ref(0);
const quickAmounts = computed(() => {
  const t = total.value;
  return [
    Math.ceil(t / 100000) * 100000,
    Math.ceil(t / 200000) * 200000,
    Math.ceil(t / 500000) * 500000,
  ].filter((v, i, a) => a.indexOf(v) === i).slice(0, 4);
});

// --- Customer state ---
const customerName = ref("");
const customerPhone = ref("");
const foundCustomer = ref(null);
const phoneError = ref("");
const loadingPhone = ref(false);
const customerSearch = ref("");
const customerList = ref([]);

// --- VNPay state ---
const vnpayRedirecting = ref(false);
const pendingVnpayOrder = reactive({
  orderId: null,
  txnRef: null,
  snapshotItems: [],
  snapshotSubtotal: 0,
  snapshotDiscount: 0,
  snapshotTotal: 0,
  snapshotCustomer: ''
});
const vnpayResult = reactive({
  show: false,
  success: false,
  txnRef: null,
  amount: 0,
  paidAt: null,
  errorMsg: null
});

// --- State dữ liệu ---
const currentOrder = ref(null);
const products = ref([]);
const categories = ref([]);
const isLoadingProducts = ref(false);

// Danh sách danh mục
const categoryList = computed(() => [
  { id: "all", name: "Tất cả", count: products.value.length },
  ...categories.value.filter(c => !['áo', 'quần', 'phụ kiện'].includes((c.categoryName || '').trim().toLowerCase())).map(c => ({
    id: String(c.categoryId),
    name: c.categoryName,
    count: products.value.filter(p => p.category === String(c.categoryId)).length
  }))
]);

// ===== COMPUTED =====
// ✅ Điều kiện disable nút thanh toán
const isCheckoutDisabled = computed(() => {
  // Nếu đang xử lý thì disable
  if (isProcessing.value) return true;
  
  // Nếu giỏ hàng trống thì disable
  if (cart.value.length === 0) return true;
  
  // Nếu chưa có số điện thoại thì disable
  if (!customerPhone.value || customerPhone.value.length < 10) return true;
  
  // Nếu số điện thoại không hợp lệ thì disable
  if (!/^0[0-9]{9}$/.test(customerPhone.value)) return true;
  
  // Nếu có lỗi phone (nhiều khách hàng) thì disable
  if (phoneError.value) return true;
  
  // Nếu đang loading kiểm tra phone thì disable
  if (loadingPhone.value) return true;
  
  return false;
});

// ===== LOAD CATEGORIES =====
const loadCategories = async () => {
  try {
    const res = await api.get("/categories/active");
    if (Array.isArray(res?.data)) {
      categories.value = res.data;
    } else if (res?.data?.success && Array.isArray(res.data.data)) {
      categories.value = res.data.data;
    } else if (res?.data?.success && Array.isArray(res.data.data?.content)) {
      categories.value = res.data.data.content;
    }
  } catch (err) {
    console.warn("Không tải được danh mục:", err);
  }
};

// ===== LOAD PRODUCTS =====
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

// ===== CUSTOMER METHODS =====
const loadCustomerList = async () => {
  try {
    const response = await api.get('/pos/customers');
    customerList.value = response?.data?.data || response?.data || [];
  } catch (error) {
    console.error("Load customers error:", error);
  }
};

const searchCustomerList = async () => {
  if (!customerSearch.value || customerSearch.value.length < 2) {
    await loadCustomerList();
    return;
  }
  try {
    const response = await api.get('/pos/customers/search', {
      params: { keyword: customerSearch.value }
    });
    customerList.value = response?.data?.data || response?.data || [];
  } catch (error) {
    console.error("Search customers error:", error);
  }
};

const selectCustomer = (customer) => {
  customerName.value = customer.customerName;
  customerPhone.value = customer.phone;
  foundCustomer.value = customer;
  phoneError.value = "";
  customerList.value = [];
  customerSearch.value = "";
};

/**
 * ✅ SỬA: Kiểm tra số điện thoại khi blur
 * KHÔNG gọi API search-by-phone nữa, chỉ kiểm tra trong danh sách đã load
 */
const checkCustomerByPhone = async () => {
  phoneError.value = "";
  
  if (!customerPhone.value || customerPhone.value.length < 10) {
    foundCustomer.value = null;
    return;
  }
  
  if (!/^0[0-9]{9}$/.test(customerPhone.value)) {
    phoneError.value = "Số điện thoại không hợp lệ (phải 10 số, bắt đầu bằng 0)";
    foundCustomer.value = null;
    return;
  }
  
  loadingPhone.value = true;
  try {
    // ✅ Load danh sách khách hàng trước
    await loadCustomerList();
    
    // ✅ Tìm khách hàng trong danh sách đã load
    const exactMatches = customerList.value.filter(c => c.phone === customerPhone.value);
    
    if (exactMatches.length === 1) {
      // Chỉ có 1 khách hàng -> tự động chọn
      foundCustomer.value = exactMatches[0];
      customerName.value = exactMatches[0].customerName;
      phoneError.value = "";
      console.log("✅ Tìm thấy 1 khách hàng:", foundCustomer.value);
    } else if (exactMatches.length > 1) {
      // Nhiều khách hàng cùng SĐT -> hiển thị danh sách để chọn
      customerList.value = exactMatches;
      foundCustomer.value = null;
      phoneError.value = "Có nhiều khách hàng cùng số điện thoại, vui lòng chọn từ danh sách!";
      console.warn("⚠️ Nhiều khách hàng cùng SĐT:", exactMatches);
    } else {
      // Không có khách hàng nào khớp
      foundCustomer.value = null;
      customerList.value = [];
      phoneError.value = "";
      console.log("ℹ️ Số điện thoại chưa có trong hệ thống, sẽ tạo mới");
    }
  } catch (error) {
    console.error("Check phone error:", error);
    foundCustomer.value = null;
    customerList.value = [];
  } finally {
    loadingPhone.value = false;
  }
};

/**
 * ✅ SỬA: Kiểm tra số điện thoại real-time (có debounce)
 */
const checkCustomerByPhoneRealtime = () => {
  clearTimeout(window.phoneCheckTimeout);
  window.phoneCheckTimeout = setTimeout(() => {
    if (customerPhone.value && customerPhone.value.length >= 10) {
      if (foundCustomer.value && customerPhone.value !== foundCustomer.value.phone) {
        foundCustomer.value = null;
        customerList.value = [];
      }
      checkCustomerByPhone();
    } else {
      foundCustomer.value = null;
      customerList.value = [];
    }
  }, 500);
};

const handleCustomerNameChange = () => {
  // Cho phép thay đổi tên
};

// ===== PRODUCT METHODS =====
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
  product.selectedSize = '';
  product.selectedColor = '';
};

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

const initPOSOrder = async () => {
  currentOrder.value = {
    orderId: "TEMP_" + Date.now(),
    orderDetails: [],
    subtotalAmount: 0,
    discountAmount: 0,
    totalAmount: 0,
    customerId: null,
    voucherCode: null,
    appliedVoucher: null
  };
};

const recalculateCart = () => {
  if (!currentOrder.value) return;
  let sub = 0;
  currentOrder.value.orderDetails.forEach(item => {
    sub += item.unitPrice * item.quantity;
  });
  currentOrder.value.subtotalAmount = sub;
  
  const d = currentOrder.value.appliedVoucher;
  if (d) {
    if (d.voucherMinOrderValue && sub < d.voucherMinOrderValue) {
      showNotification("Đơn hàng không còn đủ điều kiện áp dụng mã giảm giá này!", "warning");
      currentOrder.value.voucherCode = null;
      currentOrder.value.appliedVoucher = null;
      currentOrder.value.discountAmount = 0;
      voucherCodeInput.value = "";
    } else {
      let discount = 0;
      if (d.voucherDiscountPercent && d.voucherDiscountPercent > 0) {
        discount = sub * (d.voucherDiscountPercent / 100);
        if (d.voucherDiscountAmount && d.voucherDiscountAmount > 0 && discount > d.voucherDiscountAmount) {
          discount = d.voucherDiscountAmount;
        }
      } else if (d.voucherDiscountAmount) {
        discount = d.voucherDiscountAmount;
      }
      if (discount > sub) discount = sub;
      currentOrder.value.discountAmount = discount;
    }
  }

  currentOrder.value.totalAmount = Math.max(0, sub - (currentOrder.value.discountAmount || 0));
};

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
      unitPrice: variant.discountedPrice || variant.price,
      originalPrice: variant.price,
      itemDiscount: variant.price - (variant.discountedPrice || variant.price)
    });
    showNotification(`Đã thêm ${product.name} (${variant.size} - ${variant.color})`, "success");
  }
  recalculateCart();
};

const cart = computed(() => currentOrder.value?.orderDetails || []);
const subtotal = computed(() => currentOrder.value?.subtotalAmount || 0);
const discount = computed(() => currentOrder.value?.discountAmount || 0);
const total = computed(() => currentOrder.value ? Math.max(subtotal.value - discount.value, 0) : 0);

const formatPrice = value =>
  new Intl.NumberFormat("vi-VN").format(Number(value) || 0) + "₫";

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

const voucherCodeInput = ref("");

const applyVoucherCode = async () => {
  if (!currentOrder.value || !voucherCodeInput.value) {
    showNotification("Vui lòng nhập mã giảm giá!", "warning");
    return;
  }
  
  try {
    const res = await api.get('/vouchers/code/' + voucherCodeInput.value);
    const d = res?.data?.data || res?.data;
    if (!d || d.voucherStatus !== 'ACTIVE') {
      showNotification("Mã giảm giá không hợp lệ hoặc đã bị tạm dừng!", "error");
      return;
    }
    
    const subtotal = currentOrder.value.subtotalAmount;
    if (d.voucherMinOrderValue && subtotal < d.voucherMinOrderValue) {
      showNotification("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã này!", "error");
      return;
    }
    
    if (d.voucherMaxUsage && d.voucherUsedCount >= d.voucherMaxUsage) {
      showNotification("Mã giảm giá đã hết lượt sử dụng!", "error");
      return;
    }
    
    currentOrder.value.appliedVoucher = d;
    currentOrder.value.voucherCode = voucherCodeInput.value;
    recalculateCart();
    showNotification("Áp dụng mã giảm giá thành công!", "success");
  } catch (error) {
    showNotification(error.response?.data?.message || "Mã giảm giá không hợp lệ!", "error");
  }
};

const removeVoucherCode = () => {
  if (!currentOrder.value) return;
  currentOrder.value.voucherCode = null;
  currentOrder.value.appliedVoucher = null;
  currentOrder.value.discountAmount = 0;
  voucherCodeInput.value = "";
  recalculateCart();
  showNotification("Đã xóa mã giảm giá!", "success");
};

const clearCurrentCart = async () => {
  if (!currentOrder.value) return;
  voucherCodeInput.value = "";
  customerName.value = "";
  customerPhone.value = "";
  foundCustomer.value = null;
  phoneError.value = "";
  customerList.value = [];
  await initPOSOrder();
  showNotification("Đã xóa giỏ hàng!", "success");
};

// ===== CHECKOUT =====
const checkout = async () => {
  if (!currentOrder.value || cart.value.length === 0) {
    showNotification("Giỏ hàng trống!", "warning");
    return;
  }

  if (!(await window.appConfirm('Bạn có chắc chắn muốn thanh toán đơn hàng này không?'))) return;
  // ✅ KIỂM TRA SỐ ĐIỆN THOẠI TRƯỚC KHI THANH TOÁN
  if (!customerPhone.value || customerPhone.value.length < 10) {
    showNotification("Vui lòng nhập số điện thoại khách hàng!", "warning");
    return;
  }

  if (!/^0[0-9]{9}$/.test(customerPhone.value)) {
    showNotification("Số điện thoại không hợp lệ!", "warning");
    return;
  }

  // ✅ Nếu chưa kiểm tra phone, tự động kiểm tra
  if (!foundCustomer.value && customerPhone.value.length >= 10) {
    await checkCustomerByPhone();
  }

  // ✅ Nếu có lỗi phone (nhiều khách hàng) thì không cho thanh toán
  if (phoneError.value) {
    showNotification(phoneError.value, "warning");
    return;
  }

  // ✅ Nếu chưa chọn khách hàng (khi có nhiều) thì không cho thanh toán
  if (!foundCustomer.value && customerList.value.length > 0) {
    showNotification("Vui lòng chọn khách hàng từ danh sách!", "warning");
    return;
  }

  if (paymentMethod.value === 'VNPAY') {
    await checkoutVnpay();
    return;
  }

  // Tiền mặt
  isProcessing.value = true;
  try {
    const orderTotal = total.value;
    if (cashGiven.value > 0 && cashGiven.value < orderTotal) {
      showNotification(`Khách đưa thiếu ${formatPrice(orderTotal - cashGiven.value)}!`, 'warning');
      isProcessing.value = false;
      return;
    }
    
    const orderItems = currentOrder.value.orderDetails.map(item => ({
      productDetailId: item.productDetailId,
      quantity: item.quantity,
      unitPrice: item.unitPrice || 0,
      discount: 0,
      notes: null
    }));

    for (const item of orderItems) {
      if (!item.productDetailId) {
        showNotification("Sản phẩm không hợp lệ, vui lòng thêm lại!", "error");
        isProcessing.value = false;
        return;
      }
      if (item.quantity <= 0) {
        showNotification("Số lượng sản phẩm không hợp lệ!", "error");
        isProcessing.value = false;
        return;
      }
      if (item.unitPrice <= 0) {
        showNotification("Giá sản phẩm không hợp lệ!", "error");
        isProcessing.value = false;
        return;
      }
    }

    if (orderTotal <= 0) {
      showNotification("Tổng tiền đơn hàng không hợp lệ!", "error");
      isProcessing.value = false;
      return;
    }

    const request = {
      posOrderCustomerId: foundCustomer.value?.customerId || null,
      posOrderCustomerName: customerName.value || "Khách lẻ",
      posOrderCustomerPhone: customerPhone.value || null,
      posOrderType: "AT_COUNTER",
      posOrderDiscountAmount: discount.value || 0,
      voucherCode: currentOrder.value.voucherCode || null,
      posOrderTaxAmount: 0,
      posOrderNotes: "Bán lẻ tại quầy",
      items: orderItems,
      payments: [{
        paymentMethodId: 1,
        amount: orderTotal,
        referenceNumber: null,
        notes: null
      }]
    };

    console.log("📦 Order Request - Customer:", {
      id: foundCustomer.value?.customerId,
      name: customerName.value,
      phone: customerPhone.value
    });
    console.log("📦 Order Request Full:", JSON.stringify(request, null, 2));

    const res = await createPosOrder(request);
    console.log("📦 Order Response:", res);
    
    const orderId = res?.posOrderId || res?.data?.posOrderId || res?.data?.data?.posOrderId;

    if (orderId) {
      try { await printPosReceipt(orderId, 1); } catch (e) { /* silent */ }
      showNotification("Thanh toán thành công!", "success");

      if (!foundCustomer.value && customerPhone.value) {
        await loadCustomerList();
      }

      printInvoiceHandle({
        storeName: "Fashion Store",
        storeAddress: "Hệ thống thời trang nam",
        storePhone: "0123.456.789",
        orderId,
        orderDate: new Date().toLocaleString("vi-VN"),
        customerName: customerName.value || "Khách lẻ",
        customerPhone: customerPhone.value || "",
        items: currentOrder.value.orderDetails.map(i => ({
          productDetail: i.productDetail,
          quantity: i.quantity,
          finalAmount: i.quantity * i.unitPrice
        })),
        subtotalAmount: currentOrder.value.subtotalAmount,
        discountAmount: currentOrder.value.discountAmount,
        finalAmount: total.value,
        voucherCode: currentOrder.value.voucherCode || ''
      });

      voucherCodeInput.value = "";
      customerName.value = "";
      customerPhone.value = "";
      foundCustomer.value = null;
      phoneError.value = "";
      customerList.value = [];
      cashGiven.value = 0;
      await initPOSOrder();
    } else {
      showNotification("Thanh toán thất bại!", "error");
    }
  } catch (error) {
    console.error("❌ Lỗi thanh toán:", error);
    
    let errorMessage = "Thanh toán thất bại!";
    if (error.response) {
      console.error("📛 Response data:", error.response.data);
      console.error("📛 Response status:", error.response.status);
      
      if (error.response.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.response.data?.data?.message) {
        errorMessage = error.response.data.data.message;
      } else if (typeof error.response.data === 'string') {
        errorMessage = error.response.data;
      } else {
        errorMessage = `Lỗi ${error.response.status}: ${JSON.stringify(error.response.data)}`;
      }
    } else if (error.message) {
      errorMessage = error.message;
    }
    
    showNotification(errorMessage, "error");
  } finally {
    isProcessing.value = false;
  }
};

// ===== VNPAY FLOW =====
const checkoutVnpay = async () => {
  isProcessing.value = true;
  try {
    const orderItems = currentOrder.value.orderDetails.map(item => ({
      productDetailId: item.productDetailId,
      quantity: item.quantity,
      unitPrice: item.unitPrice || 0,
      discount: 0,
      notes: null
    }));

    for (const item of orderItems) {
      if (!item.productDetailId) {
        showNotification("Sản phẩm không hợp lệ, vui lòng thêm lại!", "error");
        isProcessing.value = false;
        return;
      }
      if (item.quantity <= 0) {
        showNotification("Số lượng sản phẩm không hợp lệ!", "error");
        isProcessing.value = false;
        return;
      }
    }

    const returnUrl = window.location.origin + window.location.pathname + '?vnpay_return=1';
    const snapshotTotal = total.value;
    
    if (snapshotTotal <= 0) {
      showNotification("Tổng tiền đơn hàng không hợp lệ!", "error");
      isProcessing.value = false;
      return;
    }
    
    const request = {
      posOrderCustomerId: foundCustomer.value?.customerId || null,
      posOrderCustomerName: customerName.value || "Khách lẻ",
      posOrderCustomerPhone: customerPhone.value || null,
      posOrderType: "AT_COUNTER",
      posOrderDiscountAmount: discount.value || 0,
      voucherCode: currentOrder.value.voucherCode || null,
      posOrderTaxAmount: 0,
      posOrderNotes: "VNPay tại quầy",
      items: orderItems,
      payments: []
    };

    console.log("📦 VNPay Order Request:", JSON.stringify(request, null, 2));

    const res = await createPosOrder(request);
    console.log("📦 VNPay Order Response:", res);
    
    const orderId = res?.posOrderId || res?.data?.posOrderId || res?.data?.data?.posOrderId;
    if (!orderId) {
      throw new Error("Không tạo được đơn hàng");
    }

    const vnpRes = await api.post(`/pos/orders/${orderId}/vnpay/create-payment`, {
      paymentMethodId: 2,
      amount: snapshotTotal,
      returnUrl
    });

    const vnpData = vnpRes.data?.data || vnpRes.data;
    const paymentUrl = vnpData?.paymentUrl;
    const txnRef = vnpData?.txnRef;

    if (!paymentUrl) throw new Error("Backend không trả về paymentUrl");

    const snapshot = {
      orderId,
      txnRef,
      customerName: customerName.value || "Khách lẻ",
      items: currentOrder.value.orderDetails.map(i => ({
        productDetail: i.productDetail,
        quantity: i.quantity,
        finalAmount: i.quantity * i.unitPrice
      })),
      subtotalAmount: currentOrder.value.subtotalAmount,
      discountAmount: currentOrder.value.discountAmount,
      finalAmount: snapshotTotal
    };
    sessionStorage.setItem('pos_vnpay_pending', JSON.stringify(snapshot));

    vnpayRedirecting.value = true;
    setTimeout(() => {
      window.location.href = paymentUrl;
    }, 800);

  } catch (err) {
    console.error("❌ Lỗi thanh toán VNPay:", err);
    
    let errorMessage = "Lỗi tạo thanh toán VNPay!";
    if (err.response) {
      console.error("📛 Response data:", err.response.data);
      if (err.response.data?.message) {
        errorMessage = err.response.data.message;
      } else if (err.response.data?.data?.message) {
        errorMessage = err.response.data.data.message;
      } else {
        errorMessage = `Lỗi ${err.response.status}: ${JSON.stringify(err.response.data)}`;
      }
    } else if (err.message) {
      errorMessage = err.message;
    }
    
    showNotification(errorMessage, "error");
    isProcessing.value = false;
  }
};

// ===== VNPAY CALLBACK =====
const handleVnpayReturn = async () => {
  const params = new URLSearchParams(window.location.search);
  if (!params.get('vnpay_return')) return;

  window.history.replaceState({}, '', window.location.pathname);

  const raw = sessionStorage.getItem('pos_vnpay_pending');
  if (!raw) return;
  const snapshot = JSON.parse(raw);
  sessionStorage.removeItem('pos_vnpay_pending');

  const responseCode = params.get('vnp_ResponseCode');
  const txnRef = params.get('vnp_TxnRef') || snapshot.txnRef;
  const paidAt = params.get('vnp_PayDate')
    ? formatVnpDate(params.get('vnp_PayDate'))
    : new Date().toLocaleString('vi-VN');

  let confirmSuccess = false;
  try {
    const confirmPayload = {};
    params.forEach((v, k) => {
      if (k.startsWith('vnp_')) {
        confirmPayload[k] = v;
      }
    });
    await api.post(`/pos/orders/${snapshot.orderId}/vnpay/confirm`, confirmPayload);
    confirmSuccess = true;
  } catch (e) {
    console.warn('Confirm VNPay lỗi:', e);
    showNotification("Lỗi đồng bộ thanh toán với máy chủ.", "error");
  }

  if (responseCode === '00' && confirmSuccess) {
    vnpayResult.show = true;
    vnpayResult.success = true;
    vnpayResult.txnRef = txnRef;
    vnpayResult.amount = snapshot.finalAmount;
    vnpayResult.paidAt = paidAt;
    vnpayResult.errorMsg = null;
    Object.assign(pendingVnpayOrder, snapshot);
  } else {
    vnpayResult.show = true;
    vnpayResult.success = false;
    vnpayResult.txnRef = txnRef;
    vnpayResult.errorMsg = vnpayErrorMsg(responseCode);
    Object.assign(pendingVnpayOrder, snapshot);
    await restoreCartFromSnapshot(snapshot);
  }
};

const formatVnpDate = (str) => {
  if (!str || str.length < 14) return str;
  return `${str.slice(6,8)}/${str.slice(4,6)}/${str.slice(0,4)} ${str.slice(8,10)}:${str.slice(10,12)}:${str.slice(12,14)}`;
};

const vnpayErrorMsg = (code) => {
  const msgs = {
    '07': 'Trừ tiền thành công nhưng giao dịch bị nghi ngờ gian lận.',
    '09': 'Thẻ/Tài khoản chưa đăng ký dịch vụ Internet Banking.',
    '10': 'Xác thực thẻ/tài khoản quá 3 lần sai. Tài khoản bị khóa.',
    '11': 'Đã hết hạn chờ thanh toán.',
    '12': 'Thẻ/Tài khoản bị khóa.',
    '13': 'Mã OTP nhập sai.',
    '24': 'Khách hàng hủy giao dịch.',
    '51': 'Tài khoản không đủ số dư.',
    '65': 'Vượt hạn mức giao dịch trong ngày.',
    '75': 'Ngân hàng đang bảo trì.',
    '79': 'Sai mật khẩu quá số lần quy định.'
  };
  return msgs[code] || `Giao dịch thất bại (mã: ${code}).`;
};

const restoreCartFromSnapshot = async (snapshot) => {
  showNotification("Đơn VNPay thất bại. Vui lòng thử lại.", "warning");
};

const finishAfterVnpay = async () => {
  const snap = pendingVnpayOrder;
  vnpayResult.show = false;

  try { await printPosReceipt(snap.orderId, 1); } catch { /* silent */ }

  printInvoiceHandle({
    storeName: "Fashion Store",
    storeAddress: "Hệ thống thời trang nam",
    storePhone: "0123.456.789",
    orderId: snap.orderId,
    orderDate: new Date().toLocaleString("vi-VN"),
    customerName: snap.customerName,
    items: snap.items,
    subtotalAmount: snap.subtotalAmount,
    discountAmount: snap.discountAmount,
    finalAmount: snap.finalAmount
  });

  showNotification("Đã hoàn tất đơn VNPay!", "success");
  voucherCodeInput.value = "";
  customerName.value = "";
  customerPhone.value = "";
  foundCustomer.value = null;
  phoneError.value = "";
  customerList.value = [];
  cashGiven.value = 0;
  await initPOSOrder();
};

const retryVnpay = async () => {
  vnpayResult.show = false;
  paymentMethod.value = 'VNPAY';
  showNotification("Vui lòng thêm lại sản phẩm và thử thanh toán VNPay.", "info");
};

const cancelVnpayOrder = () => {
  vnpayResult.show = false;
  showNotification("Đã hủy thanh toán VNPay.", "info");
};

// ===== PRINT INVOICE =====
const printInvoiceHandle = (invoice) => {
  const w = window.open("", "_blank");
  
  let discountHtml = '';
  if (invoice.discountAmount > 0) {
    const voucherText = invoice.voucherCode ? `Mã: ${invoice.voucherCode}` : '';
    discountHtml = `
      <div class="discount-row">
        <p><span>Giảm giá ${voucherText}</span><span>-${new Intl.NumberFormat("vi-VN").format(invoice.discountAmount)}đ</span></p>
      </div>
    `;
  }

  w.document.write(`<html><head><title>Hóa đơn #${invoice.orderId}</title>
  <style>
    body{font-family:'Courier New',monospace;width:80mm;padding:4px;font-size:12px;line-height:1.4}
    .tc{text-align:center}.bold{font-weight:bold}.header{font-size:15px;margin-bottom:8px}
    table{width:100%;border-collapse:collapse;margin:8px 0}
    th,td{text-align:left;padding:4px 0;font-size:11px}
    .ts{border-top:1px dashed #000;padding-top:6px;margin-top:6px}
    .ts p{margin:4px 0;display:flex;justify-content:space-between}
    .discount-row{color:#dc3545;border-top:1px dashed #000;padding-top:6px;margin-top:6px}
    .discount-row p{margin:4px 0;display:flex;justify-content:space-between}
    .total-row{font-weight:bold;font-size:14px;border-top:1px solid #000;padding-top:6px;margin-top:6px}
    .total-row p{margin:4px 0;display:flex;justify-content:space-between}
    .customer-info{margin:4px 0;font-size:11px}
    .customer-info p{margin:2px 0}
  </style></head><body>
  <div class="tc bold header">${invoice.storeName}</div>
  <div class="tc">${invoice.storeAddress}</div>
  <div class="tc">Hotline: ${invoice.storePhone}</div>
  <p>------------------------------------------</p>
  <p><b>Mã HĐ:</b> #${invoice.orderId}</p>
  <p><b>Thời gian:</b> ${invoice.orderDate}</p>
  <div class="customer-info">
    <p><b>Khách hàng:</b> ${invoice.customerName}</p>
    <p><b>SĐT:</b> ${invoice.customerPhone || 'Không có'}</p>
  </div>
  <p>------------------------------------------</p>
  <table><thead><tr><th>Mặt hàng</th><th style="text-align:center">SL</th><th style="text-align:right">T.Tiền</th></tr></thead>
  <tbody>${invoice.items.map(item => `<tr>
    <td>${item.productDetail.skuDetail}<br><small>(${item.productDetail.size}/${item.productDetail.color})</small></td>
    <td style="text-align:center">${item.quantity}</td>
    <td style="text-align:right">${new Intl.NumberFormat("vi-VN").format(item.finalAmount)}đ</td>
  </tr>`).join("")}</tbody></table>
  <div class="ts">
    <p><span>Tạm tính:</span><span>${new Intl.NumberFormat("vi-VN").format(invoice.subtotalAmount)}đ</span></p>
    ${discountHtml}
    <div class="total-row">
      <p><span>THANH TOÁN:</span><span>${new Intl.NumberFormat("vi-VN").format(invoice.finalAmount)}đ</span></p>
    </div>
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
  loadCustomerList();
  handleVnpayReturn();
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

.category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.category-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.category-filter-label {
  color: #666;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}
.category-filter-label i { color: #e63946; margin-right: 4px; }
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
.cat-count {
  display: inline-flex;
  min-width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  margin-left: 4px;
  padding: 0 5px;
  border-radius: 10px;
  background: #f1f1f4;
  color: #777;
  font-size: 11px;
}
.cat-tab.active .cat-count { background: rgba(255,255,255,.22); color: #fff; }

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #aaa;
  font-size: 15px;
}
.loading-state i { margin-right: 8px; }

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
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
  padding-top: 68%;
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
  padding: 9px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.pc-name-pos {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a2e;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 34px;
}
.pc-price-pos {
  color: #e63946;
  font-weight: 700;
  font-size: 14px;
}

.attr-group {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  align-items: center;
  gap: 5px;
}
.attr-group label {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 2px 6px;
  border-radius: 5px;
  font-size: 10px;
  color: #be123c;
  background: #fff1f2;
  border: 1px solid #fecdd3;
  font-weight: 600;
}
.attr-group:nth-of-type(2) label { color: #1d4ed8; background: #eff6ff; border-color: #bfdbfe; }
.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.chip {
  padding: 4px 8px;
  border: 1px solid #e1e1e6;
  border-radius: 6px;
  font-size: 11px;
  cursor: pointer;
  background: #fafafc;
  transition: 0.15s;
  user-select: none;
}
.chip:hover:not(.disabled) { border-color: #aaa; }
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

.action-row {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 7px;
  border-top: 1px dashed #eee;
}
.stock-info {
  font-size: 11px;
  color: #2a9d8f;
  font-weight: 600;
}
.stock-info.out-of-stock { color: #e63946; }

.add-btn {
  background: #e63946;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 6px 10px;
  font-weight: 600;
  font-size: 12px;
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
.add-btn:disabled { background: #d1d1d6; cursor: not-allowed; }

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

/* ===== CUSTOMER SEARCH ===== */
.customer-search-box {
  margin-bottom: 10px;
}
.customer-search-box .search-row {
  display: flex;
  gap: 8px;
}
.customer-search-box .search-row input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
}
.customer-search-box .search-row input:focus {
  border-color: #e63946;
}
.btn-sm {
  padding: 8px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.btn-sm.btn-primary {
  background: #e63946;
  color: white;
}
.btn-sm.btn-primary:hover {
  background: #c62828;
}
.customer-list {
  max-height: 150px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 6px;
  margin-top: 6px;
}
.customer-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.15s;
}
.customer-item:hover {
  background: #f0f7ff;
}
.customer-item .cust-name {
  font-weight: 600;
}
.customer-item .cust-phone {
  color: #666;
  font-size: 13px;
}
.customer-item .cust-id {
  font-size: 11px;
  color: #999;
  background: #f0f0f2;
  padding: 0 8px;
  border-radius: 4px;
}

/* ===== CUSTOMER INFO BOX ===== */
.customer-info-box {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.customer-info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.info-field label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  margin-bottom: 4px;
}
.info-field label .required {
  color: #e63946;
}
.info-field input {
  width: 100%;
  padding: 8px 12px;
  border: 1.5px solid #e1e1e6;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}
.info-field input:focus {
  border-color: #e63946;
}
.info-field input.error {
  border-color: #dc3545;
}
.error-text {
  color: #dc3545;
  font-size: 11px;
  margin-top: 4px;
  display: block;
}
.customer-found {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.customer-found.success {
  background: #d4edda;
  color: #155724;
}
.customer-found.info {
  background: #cce5ff;
  color: #004085;
}
.customer-found.warning {
  background: #fff3cd;
  color: #856404;
}
.customer-found i {
  font-size: 16px;
}
.customer-found.info i.fa-spinner {
  animation: spin 1s linear infinite;
}

.customer-id {
  font-size: 12px;
  color: #6c757d;
  margin-left: 6px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== CART ITEMS ===== */
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
.variant-badges { display: inline-flex; gap: 5px; margin-left: 4px; vertical-align: middle; }
.variant-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 7px;
  border-radius: 5px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1;
}
.variant-badge b { font-size: 9px; text-transform: uppercase; letter-spacing: .2px; }
.size-badge { background: #fff1f2; color: #be123c; border: 1px solid #fecdd3; }
.color-badge { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.price-label { color: #e63946; font-weight: 700; margin: 0; font-size: 13px; }
.qty-control { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.qty-control button {
  width: 26px; height: 26px;
  border: 1.5px solid #e1e1e6;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px; font-weight: 700;
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

/* ===== SUMMARY ===== */
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
  background: #edf7f5; 
  padding: 8px 10px; 
  border-radius: 6px; 
  font-size: 13px;
  flex-direction: column !important;
  align-items: flex-start !important;
}
.text-success { 
  color: #2a9d8f; 
  font-weight: 600; 
  display: flex; 
  align-items: center; 
  gap: 6px;
  width: 100%;
}
.text-success i { cursor: pointer; color: #e63946; }
.voucher-info {
  display: flex;
  gap: 6px;
  font-size: 11px;
  color: #2a9d8f;
  margin-top: 4px;
}
.voucher-code {
  background: #d4edda;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.voucher-detail {
  color: #155724;
}
.summary .total { 
  font-size: 18px; 
  font-weight: 700; 
  color: #e63946; 
  border-top: 1px dashed #eee; 
  padding-top: 10px; 
  margin-top: 6px; 
}

/* ===== PAYMENT METHODS ===== */
.payment-methods { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 14px 0; }
.payment-methods button {
  border: 1.5px solid #e1e1e6;
  border-radius: 8px; padding: 10px 6px;
  background: white; cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 6px;
  font-weight: 600; font-size: 12px; transition: 0.15s;
}
.payment-methods button.active { border-color: #e63946; color: #e63946; background: rgba(230,57,70,0.05); }
.payment-methods button.vnpay-btn.active { border-color: #0064c8; color: #0064c8; background: rgba(0,100,200,0.06); }

/* ===== CASH INPUT ===== */
.cash-input-group { background: #f8f9fb; border-radius: 10px; padding: 12px 14px; margin-bottom: 12px; }
.cash-input-group label { font-size: 12px; font-weight: 600; color: #666; margin-bottom: 6px; display: block; }
.cash-input-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.cash-input-row input {
  flex: 1; border: 1.5px solid #e1e1e6; border-radius: 8px;
  padding: 8px 12px; font-size: 14px; outline: none;
}
.cash-input-row input:focus { border-color: #e63946; }
.change-badge { font-size: 12px; color: #28a745; white-space: nowrap; }
.change-badge strong { font-size: 13px; }
.quick-cash { display: flex; gap: 6px; flex-wrap: wrap; }
.quick-cash button {
  border: 1.5px solid #e1e1e6; background: white; border-radius: 6px;
  padding: 4px 10px; font-size: 11px; font-weight: 600; cursor: pointer;
  transition: 0.15s; color: #444;
}
.quick-cash button:hover, .quick-cash button.sel { border-color: #e63946; color: #e63946; background: rgba(230,57,70,0.06); }

/* ===== VNPAY ===== */
.vnpay-info-block {
  background: #f0f6ff;
  border: 1px solid #cce0ff;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 12px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #0064c8;
  line-height: 1.5;
}
.vnpay-info-block i { margin-top: 2px; flex-shrink: 0; }

/* ===== NÚT THANH TOÁN ===== */
.checkout-btn {
  width: 100%;
  background: linear-gradient(135deg, #e63946, #f36872);
  color: white;
  border: none;
  border-radius: 10px;
  padding: 14px;
  font-weight: 700;
  cursor: pointer;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: 0.2s;
}
.checkout-btn.vnpay-checkout {
  background: linear-gradient(135deg, #0064c8, #0084ff);
}
.checkout-btn:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
}
.checkout-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}

/* ===== MODAL OVERLAY ===== */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.6);
  display: flex; align-items: center; justify-content: center;
  z-index: 9999; backdrop-filter: blur(6px);
}

.vnpay-redirect-card {
  background: white;
  border-radius: 20px;
  width: 360px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(0,0,0,0.3);
  animation: modalPop 0.3s cubic-bezier(.34,1.56,.64,1);
}
.vnpay-redirect-header {
  background: linear-gradient(135deg, #0064c8, #0084ff);
  padding: 18px 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
  font-size: 16px;
  font-weight: 700;
}
.vnpay-redirect-header i { font-size: 22px; }
.vnpay-redirect-body {
  padding: 32px 24px;
  text-align: center;
}
.redirect-spinner { font-size: 44px; color: #0064c8; margin-bottom: 16px; }
.redirect-amount { font-size: 28px; font-weight: 800; color: #1a1a2e; margin: 0 0 12px; }
.redirect-hint { font-size: 14px; color: #555; margin: 0 0 6px; }
.redirect-sub { font-size: 12px; color: #aaa; margin: 0; }

/* ===== VNPAY RESULT ===== */
.vnpay-result-card {
  background: white;
  border-radius: 20px;
  width: 400px;
  max-width: 95vw;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(0,0,0,0.3);
  animation: modalPop 0.3s cubic-bezier(.34,1.56,.64,1);
}
.result-content {
  padding: 36px 28px;
  text-align: center;
}
.result-icon { font-size: 64px; margin-bottom: 14px; animation: popIn 0.4s cubic-bezier(.34,1.56,.64,1); }
.success-icon { color: #28a745; }
.failed-icon { color: #e63946; }

.result-content h3 { font-size: 20px; font-weight: 800; color: #1a1a2e; margin: 0 0 8px; }
.result-amount { font-size: 28px; font-weight: 800; color: #0064c8; margin: 0 0 20px; }
.result-error-msg { font-size: 14px; color: #666; margin: 0 0 24px; line-height: 1.5; }

.result-meta {
  background: #f8f9fb;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 22px;
  text-align: left;
}
.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 4px 0;
}
.meta-row span { color: #888; }
.meta-row code { background: #e8f5e9; color: #28a745; border-radius: 4px; padding: 2px 8px; font-size: 12px; }
.meta-row strong { color: #1a1a2e; font-weight: 600; }

.btn-print-receipt {
  width: 100%;
  background: linear-gradient(135deg, #e63946, #f36872);
  color: white; border: none;
  border-radius: 10px; padding: 14px;
  font-size: 15px; font-weight: 700;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  transition: 0.2s;
}
.btn-print-receipt:hover { opacity: 0.9; transform: translateY(-1px); }

.failed-actions { display: flex; flex-direction: column; gap: 10px; }
.btn-retry-vnpay {
  background: linear-gradient(135deg, #0064c8, #0084ff);
  color: white; border: none;
  border-radius: 10px; padding: 13px;
  font-size: 14px; font-weight: 700;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  transition: 0.15s;
}
.btn-retry-vnpay:hover { opacity: 0.9; }
.btn-cancel-vnpay {
  background: white; border: 1.5px solid #e1e1e6; color: #666;
  border-radius: 10px; padding: 11px;
  font-size: 14px; font-weight: 600; cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  transition: 0.15s;
}
.btn-cancel-vnpay:hover { border-color: #aaa; }

@media (max-width: 1350px) {
  .staff-pos { grid-template-columns: minmax(0, 1fr) 360px; gap: 14px; }
  .product-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 1050px) {
  .product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 760px) {
  .staff-pos { display: flex; flex-direction: column; }
  .product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .cart-panel { width: 100%; }
}

/* Animations */
@keyframes modalPop { from { opacity:0; transform: scale(0.88) translateY(20px); } to { opacity:1; transform: none; } }
@keyframes popIn { from { transform: scale(0); opacity: 0; } to { transform: scale(1); opacity: 1; } }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
@media (max-width: 768px) {
  .staff-pos {
    grid-template-columns: 1fr;
  }
  .customer-info-row {
    grid-template-columns: 1fr;
  }
}
</style>
