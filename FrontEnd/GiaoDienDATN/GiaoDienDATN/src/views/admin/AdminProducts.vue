<template>
  <div class="admin-products">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>Quản lý sản phẩm</h2>
        <p class="subtitle">Quản lý danh sách sản phẩm, biến thể màu sắc và kích thước</p>
      </div>
      <button class="btn-add" @click="openCreateModal">
        <span class="btn-icon">＋</span> Thêm sản phẩm
      </button>
    </div>

    <!-- Stats Row -->
    <div class="stats-row" v-if="statistics">
      <div class="stat-card stat-total">
        <div class="stat-icon">📦</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalProducts || 0 }}</div>
          <div class="stat-label">Tổng sản phẩm</div>
        </div>
      </div>
      <div class="stat-card stat-active">
        <div class="stat-icon">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.inStockVariants || 0 }}</div>
          <div class="stat-label">Biến thể còn hàng</div>
        </div>
      </div>
      <div class="stat-card stat-warning">
        <div class="stat-icon">⚠️</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.lowStockVariants || 0 }}</div>
          <div class="stat-label">Sắp hết hàng</div>
        </div>
      </div>
      <div class="stat-card stat-deleted">
        <div class="stat-icon">🗑️</div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.deletedProducts || 0 }}</div>
          <div class="stat-label">Đã xóa</div>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input v-model="searchKeyword" @keyup.enter="fetchProducts" placeholder="Tìm kiếm sản phẩm..."
          autocomplete="off" />
        <button v-if="searchKeyword" class="clear-search" @click="
          searchKeyword = '';
        fetchProducts();
        ">
          ✕
        </button>
      </div>
      <select v-model="selectedCategoryId" @change="fetchProducts" class="filter-select">
        <option :value="null">Tất cả danh mục</option>
        <option v-for="cat in categoryList" :key="cat.categoryId" :value="cat.categoryId">
          {{ cat.categoryName }}
        </option>
      </select>
      <select v-model="selectedStatus" @change="fetchProducts" class="filter-select">
        <option :value="null">Tất cả trạng thái</option>
        <option value="ACTIVE">Đang bán</option>
        <option value="INACTIVE">Ngừng bán</option>
        <option value="DELETED">Đã xóa</option>
      </select>
      <button class="btn-search" @click="fetchProducts">Tìm kiếm</button>
      <button v-if="searchKeyword || selectedCategoryId || selectedStatus" class="btn-reset" @click="resetFilters">
        <span class="btn-icon">↺</span> Reset
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>Đang tải dữ liệu...</span>
    </div>

    <!-- Table -->
    <div v-else class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên sản phẩm</th>
            <th>Danh mục</th>
            <th>Tổng số lượng</th>
            <th>Biến thể & Tồn kho</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in paginatedProducts" :key="product.productId" class="table-row"
            :class="{ 'row-deleted': product._deletedAt }">
            <td class="id-cell">#{{ product.productId }}</td>
            <td class="product-name" :class="{ 'deleted-text': product._deletedAt }">
              {{ product.productName }}
            </td>
            <td class="category-cell">{{ product.category?.categoryName || "-" }}</td>

            <td class="stock-cell">

              <span v-if="!product._deletedAt" class="stock-badge">

                {{ product.totalStock }}

              </span>

              <span v-else class="deleted-label">

                Đã xóa

              </span>

            </td>

            <td class="variants-cell">
              <div v-if="product.variants?.length">
                <div class="variant-summary">
                  <div class="summary-colors">
                    <span v-for="color in [...new Set(product.variants.map((v) => v.color))]" :key="color"
                      class="summary-tag">
                      {{ color }}
                    </span>
                  </div>

                  <div class="summary-sizes">
                    <span v-for="size in [...new Set(product.variants.map((v) => v.size))]" :key="size"
                      class="summary-size">
                      {{ size }}
                    </span>
                  </div>

                  <button class="btn-detail" @click="openVariantDetail(product)">
                    Xem chi tiết
                  </button>
                </div>
              </div>

              <div v-else class="no-variant">Không có biến thể</div>
            </td>

            <td class="status-cell">
              <span v-if="product._deletedAt" class="status-badge status-deleted">Đã xóa</span>
              <span v-else :class="[
                'status-badge',
                product.status === 'ACTIVE' ? 'status-active' : 'status-inactive',
              ]">{{ product.status === "ACTIVE" ? "Đang bán" : "Ngừng bán" }}</span>
            </td>

            <td class="actions">
              <div class="action-buttons">
                <template v-if="product._deletedAt">
                  <button class="action-btn restore-btn" @click="confirmRestore(product)" title="Khôi phục">
                    ↩️
                  </button>

                  <button class="action-btn view-btn" @click="viewProductDetail(product)" title="Xem chi tiết">
                    👁️
                  </button>
                </template>

                <template v-else>
                  <button class="action-btn edit-btn" @click="openEditModal(product)" title="Sửa">
                    ✏️
                  </button>

                  <button class="action-btn delete-btn" @click="confirmDelete(product)" title="Xóa">
                    🗑️
                  </button>
                </template>
              </div>
            </td>
          </tr>
          <tr v-if="allProducts.length === 0 && !loading">
            <td colspan="7" class="empty-row">
              <div class="empty-state">
                <div class="empty-icon">📭</div>
                <p>Không có sản phẩm nào</p>
                <button class="btn-retry" @click="fetchProducts">Tải lại</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 1">
      <span class="page-total">Tổng: {{ allProducts.length }} sản phẩm</span>
      <div class="page-controls">
        <button @click="goToPage(0)" :disabled="currentPage === 0">«</button>
        <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 0">
          ‹
        </button>
        <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button @click="goToPage(currentPage + 1)" :disabled="currentPage + 1 >= totalPages">
          ›
        </button>
        <button @click="goToPage(totalPages - 1)" :disabled="currentPage + 1 >= totalPages">
          »
        </button>
      </div>
    </div>

    <!-- Product Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-container modal-lg">
        <div class="modal-header">
          <h3>{{ isEdit ? "✏️ Sửa sản phẩm" : "➕ Thêm sản phẩm" }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>

        <form @submit.prevent="handleSubmit">
          <div class="modal-body">
            <div v-if="formErrors.length > 0" class="form-errors-container">
              <div class="form-errors-header">
                <span>⚠️ Vui lòng sửa các lỗi sau:</span>
              </div>
              <ul class="form-errors-list">
                <li v-for="(error, index) in formErrors" :key="index">{{ error }}</li>
              </ul>
            </div>

            <div class="form-section">
              <h4>📝 Thông tin cơ bản</h4>
              <div class="form-group" :class="{ 'has-error': validationErrors.productName }">
                <label>Tên sản phẩm <span class="required">*</span></label>
                <input v-model.trim="form.productName" type="text" placeholder="Nhập tên sản phẩm" maxlength="50"
                  @input="validateProductNameInput(); clearFieldError('productName')" @keypress="preventSpecialChars" />
                <span class="char-count">{{ form.productName ? form.productName.length : 0 }}/50</span>
                <span v-if="validationErrors.productName" class="field-error">{{
                  validationErrors.productName
                }}</span>
              </div>

              <div class="form-group" :class="{ 'has-error': validationErrors.categoryId }">
                <label>Danh mục <span class="required">*</span></label>
                <select v-model="form.categoryId" required @change="clearFieldError('categoryId')">
                  <option :value="null">Chọn danh mục</option>
                  <option v-for="cat in categoryList" :key="cat.categoryId" :value="cat.categoryId">
                    {{ cat.categoryName }}
                  </option>
                </select>
                <span v-if="validationErrors.categoryId" class="field-error">{{
                  validationErrors.categoryId
                }}</span>
              </div>

              <div class="form-group">
                <label class="checkbox-label">
                  <input type="checkbox" v-model="form.isActive" />
                  <span> Đang bán</span>
                </label>
              </div>

              <div class="form-group">
                <label>Mô tả sản phẩm</label>
                <textarea v-model="form.description" rows="4" placeholder="Nhập mô tả sản phẩm..."></textarea>
              </div>
            </div>

            <div class="form-section">
              <div class="variants-header">
                <h4>🎨 Biến thể (Màu sắc - Kích thước)</h4>
                <button type="button" class="btn-add-variant" @click="addVariant">
                  <span class="btn-icon">＋</span> Thêm biến thể
                </button>
              </div>

              <div v-if="validationErrors.variants" class="field-error variants-error">
                {{ validationErrors.variants }}
              </div>

              <div class="variants-table-wrapper">
                <table class="variants-table">
                  <thead>
                    <tr>
                      <th>Màu sắc</th>
                      <th>Kích thước</th>
                      <th>Giá gốc (VNĐ)</th>
                      <th style="min-width: 180px">Khuyến mãi riêng</th>
                      <th>Giá sau KM (VNĐ)</th>
                      <th>Số lượng</th>
                      <th>SKU</th>
                      <th style="width: 50px"></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(variant, index) in form.variants" :key="index" :class="{
                      'variant-error-row': variantErrors[index],
                      'variant-duplicate-row': variantErrors[index]?.duplicate,
                    }">
                      <td>
                        <select v-model="variant.colorId" required @change="clearVariantError(index)">
                          <option :value="null">Chọn màu</option>
                          <option v-for="color in colorList" :key="color.colorId" :value="color.colorId">
                            {{ color.colorName }}
                          </option>
                        </select>
                        <span v-if="variantErrors[index]?.color" class="field-error-small">{{ variantErrors[index].color
                        }}</span>
                      </td>
                      <td>
                        <select v-model="variant.sizeId" required @change="clearVariantError(index)">
                          <option :value="null">Chọn size</option>
                          <option v-for="size in sizeList" :key="size.sizeId" :value="size.sizeId">
                            {{ size.sizeName }}
                          </option>
                        </select>
                        <span v-if="variantErrors[index]?.size" class="field-error-small">{{ variantErrors[index].size
                        }}</span>
                      </td>
                      <td>
                        <input type="number" v-model="variant.price" placeholder="Tối thiểu 50.000đ" min="50000"
                          required @input="clearVariantError(index)" />
                        <span v-if="variantErrors[index]?.price" class="field-error-small">{{ variantErrors[index].price
                        }}</span>
                      </td>
                      <!-- ✅ CỘT KHUYẾN MÃI RIÊNG -->
                      <td>
                        <div class="variant-promotions">
                          <div class="promo-tags" v-if="variant.promotionIds && variant.promotionIds.length > 0">
                            <span v-for="promoId in variant.promotionIds" :key="promoId" class="promo-tag">
                              {{ getPromotionName(variant, promoId) }}
                              <button type="button" class="promo-tag-remove"
                                @click="removePromotionFromVariant(index, promoId)">
                                ×
                              </button>
                            </span>
                          </div>
                          <select class="promo-add-select" @change="addPromotionToVariant(index, $event)">
                            <option value="">+ Thêm KM</option>
                            <option v-for="promo in getAvailablePromotions(
                              variant.promotionIds
                            )" :key="promo.promotionId" :value="promo.promotionId">
                              {{ promo.promotionName }} (-{{
                                promo.promotionDiscountPercent
                              }}%)
                            </option>
                          </select>
                        </div>
                      </td>
                      <td>
                        <input type="text" :value="formatPrice(getVariantDiscountedPrice(variant))"
                          class="discounted-price" readonly disabled />
                      </td>
                      <td>
                        <input type="number" v-model="variant.quantity" placeholder="Tối thiểu 1" min="1" required
                          @input="clearVariantError(index)" />
                        <span v-if="variantErrors[index]?.quantity" class="field-error-small">{{
                          variantErrors[index].quantity }}</span>
                      </td>
                      <td>
                        <input type="text" v-model="variant.sku" placeholder="SKU" />
                      </td>
                      <td>
                        <button type="button" class="btn-remove-variant" @click="removeVariant(index)">
                          🗑️
                        </button>
                        <span v-if="variantErrors[index]?.duplicate" class="field-error-small duplicate-error">⚠️ {{
                          variantErrors[index].duplicate }}</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="closeModal">Hủy</button>
            <button type="submit" class="btn-save" :disabled="saving">
              <span v-if="saving" class="loading-spinner-small"></span>
              <span v-else>💾</span>
              {{ saving ? "Đang lưu..." : "Lưu sản phẩm" }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showVariantModal" class="modal-overlay" @click.self="closeVariantDetail">
      <div class="modal-container modal-lg">
        <div class="modal-header">
          <h3>Chi tiết biến thể</h3>
          <button class="close-btn" @click="closeVariantDetail">×</button>
        </div>

        <div class="modal-body">
          <table class="variants-table">
            <thead>
              <tr>
                <th>Màu</th>
                <th>Size</th>
                <th>Giá</th>
                <th>Giá khuyến mãi</th>
                <th>Số lượng</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(variant, index) in selectedVariantProduct?.variants" :key="index">
                <td>{{ variant.color }}</td>

                <td>{{ variant.size }}</td>

                <td>{{ formatPrice(variant.price) }}</td>

                <td>
                  {{ formatPrice(variant.discountedPrice || variant.price) }}
                </td>

                <td>{{ variant.quantity }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <!-- Delete Modal -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận xóa</h3>
          <button class="close-btn" @click="closeDeleteModal">✕</button>
        </div>
        <div class="modal-body">
          <p>
            Bạn có chắc muốn xóa sản phẩm <strong>{{ deleteProductName }}</strong>?
          </p>
          <p class="warning-text">
            Sau khi xóa, sản phẩm sẽ chuyển sang trạng thái đã xóa và có thể khôi phục
            lại!
          </p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeDeleteModal">Hủy</button><button class="btn-danger"
            @click="deleteProduct">Xóa</button>
        </div>
      </div>
    </div>

    <!-- Restore Modal -->
    <div v-if="showRestoreModal" class="modal-overlay" @click.self="closeRestoreModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận khôi phục</h3>
          <button class="close-btn" @click="closeRestoreModal">✕</button>
        </div>
        <div class="modal-body">
          <p>
            Bạn có chắc muốn khôi phục sản phẩm <strong>{{ restoreProductName }}</strong>?
          </p>
          <p>
            Sau khi khôi phục, sản phẩm sẽ trở lại trạng thái ban đầu và có thể chỉnh sửa.
          </p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeRestoreModal">Hủy</button><button class="btn-save"
            @click="restoreProduct">Khôi phục</button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div :class="['toast', { 'toast-error': toastType === 'error', show: showToast }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import axios from "axios";

const API_BASE = "http://localhost:8080/api";

const allProducts = ref([]);
const categoryList = ref([]);
const colorList = ref([]);
const sizeList = ref([]);
const promotionList = ref([]); // ✅ Đã thêm khai báo danh sách khuyến mãi bị thiếu
const statistics = ref({
  totalProducts: 0,
  inStockVariants: 0,
  lowStockVariants: 0,
  deletedProducts: 0,
});
const loading = ref(false);
const saving = ref(false);
const showModal = ref(false);
const showDeleteModal = ref(false);
const showRestoreModal = ref(false);
const isEdit = ref(false);
const currentPage = ref(0);
const pageSize = ref(5);
const totalPages = ref(0);
const deleteProductId = ref(null);
const deleteProductName = ref("");
const restoreProductId = ref(null);
const restoreProductName = ref("");
const searchKeyword = ref("");
const selectedCategoryId = ref(null);
const selectedStatus = ref(null);
const showToast = ref(false);
const toastMessage = ref("");
const toastType = ref("success");
let toastTimer = null;

const validationErrors = reactive({ productName: "", categoryId: "", variants: "" });
const variantErrors = reactive({});
const formErrors = ref([]);

const form = ref({
  productId: null,
  productName: "",
  categoryId: null,
  description: "",
  isActive: true,
  variants: [],
});

const showVariantModal = ref(false);
const selectedVariantProduct = ref(null);

const openVariantDetail = (product) => {
  selectedVariantProduct.value = product;
  showVariantModal.value = true;
};

const closeVariantDetail = () => {
  showVariantModal.value = false;
  selectedVariantProduct.value = null;
};

const filteredProducts = computed(() => {
  let filtered = [...allProducts.value];
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase();
    filtered = filtered.filter((p) => p.productName?.toLowerCase().includes(kw));
  }
  if (selectedCategoryId.value) {
    filtered = filtered.filter(
      (p) =>
        p.categoryId === selectedCategoryId.value ||
        p.category?.categoryId === selectedCategoryId.value
    );
  }
  if (selectedStatus.value) {
    filtered = filtered.filter((p) => p.status === selectedStatus.value);
  }
  return filtered;
});

const paginatedProducts = computed(() => {
  const start = currentPage.value * pageSize.value;
  return filteredProducts.value.slice(start, start + pageSize.value);
});

const showNotification = (message, type = "success") => {
  if (toastTimer) clearTimeout(toastTimer);
  toastMessage.value = message;
  toastType.value = type;
  showToast.value = true;
  toastTimer = setTimeout(() => {
    showToast.value = false;
  }, 3000);
};

const getColorHex = (colorName) => {
  const color = colorList.value.find((c) => c.colorName === colorName);
  return color?.hexCode || color?.colorHexCode || "#ccc";
};

const formatPrice = (price) => {
  if (!price) return "0đ";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    price
  );
};

// ✅ KHuyến mãi cho từng biến thể (Đã fix lỗi thiếu .value)
const getAvailablePromotions = (currentPromotionIds = []) => {
  return promotionList.value.filter((p) => !currentPromotionIds.includes(p.promotionId));
};

const getPromotionName = (variant, promotionId) => {
  // ✅ Ưu tiên lấy từ dữ liệu đã lưu ngay trên biến thể (không đổi dù promotion sau này hết hạn/bị tắt)
  const cached = variant?.promotionsDetail?.find((p) => p.promotionId === promotionId);
  if (cached) return `${cached.promotionName} (-${cached.promotionDiscountPercent}%)`;

  const promo = promotionList.value.find((p) => p.promotionId === promotionId);
  return promo ? `${promo.promotionName} (-${promo.promotionDiscountPercent}%)` : "";
};

const addPromotionToVariant = (variantIndex, event) => {
  const promotionId = parseInt(event.target.value);
  if (!promotionId) return;
  const variant = form.value.variants[variantIndex];
  if (!variant.promotionIds) variant.promotionIds = [];
  if (!variant.promotionsDetail) variant.promotionsDetail = [];

  if (!variant.promotionIds.includes(promotionId)) {
    variant.promotionIds.push(promotionId);

    // ✅ Lưu luôn % giảm giá + tên ngay lúc chọn, để "Giá sau KM" không phụ thuộc
    // vào việc promotion đó còn nằm trong danh sách ACTIVE tại các lần render sau hay không
    const promo = promotionList.value.find((p) => p.promotionId === promotionId);
    if (promo) {
      variant.promotionsDetail.push({
        promotionId: promo.promotionId,
        promotionName: promo.promotionName,
        promotionDiscountPercent: promo.promotionDiscountPercent,
      });
    }
  }
  event.target.value = "";
};

const removePromotionFromVariant = (
  variantIndex,
  promotionId
) => {

  const variant =
    form.value.variants[variantIndex];

  variant.promotionIds =
    (variant.promotionIds || [])
      .filter(
        id => id !== promotionId
      );

  variant.promotionsDetail =
    (variant.promotionsDetail || [])
      .filter(
        p => p.promotionId !== promotionId
      );

  variant.promotionIds = [
    ...variant.promotionIds
  ];

  variant.promotionsDetail = [
    ...variant.promotionsDetail
  ];

};

const getTotalVariantDiscount = (variant) => {
  if (!variant.promotionIds || variant.promotionIds.length === 0) return 0;
  let total = 0;
  variant.promotionIds.forEach((id) => {
    // ✅ Ưu tiên % đã lưu sẵn trên biến thể; chỉ tra cứu lại promotionList nếu chưa có (trường hợp cũ)
    const cached = variant.promotionsDetail?.find((p) => p.promotionId === id);
    if (cached) {
      total += cached.promotionDiscountPercent || 0;
      return;
    }
    const promo = promotionList.value.find((p) => p.promotionId === id);
    if (promo) total += promo.promotionDiscountPercent || 0;
  });
  return Math.min(total, 100);
};

const getVariantDiscountedPrice = (variant) => {
  if (!variant.price || variant.price <= 0) return 0;
  const variantDiscount = getTotalVariantDiscount(variant);
  const totalDiscount = Math.min(variantDiscount, 100);
  if (totalDiscount === 0) return variant.price;
  return variant.price * (1 - totalDiscount / 100);
};

const clearFieldError = (field) => {
  validationErrors[field] = "";
};

// ✅ Chặn ký tự đặc biệt khi gõ
const preventSpecialChars = (e) => {
  const char = String.fromCharCode(e.keyCode || e.which);
  const pattern = /^[a-zA-Z0-9\sÀ-ỹà-ỹđĐ\p{L}\p{N}]+$/u;
  if (!pattern.test(char)) {
    e.preventDefault();
  }
};

// ✅ Validate và xóa ký tự đặc biệt khi paste/input
const validateProductNameInput = () => {

  form.value.productName =
    (form.value.productName || '');

  const sanitized =
    form.value.productName.replace(
      /[^a-zA-Z0-9\sÀ-ỹà-ỹđĐ\p{L}\p{N}]/gu,
      ''
    );

  form.value.productName = sanitized;

  const value =
    sanitized.trim();
    validationErrors.productName = '';
    if (!value) {
      validationErrors.productName =
      'Tên sản phẩm không được để trống';
      return;
  }
  if (value.length < 5) {
    validationErrors.productName =
      'Tên sản phẩm phải từ 5 ký tự trở lên';
      return;
  }

  if (value.length > 50) {
    validationErrors.productName =
      'Tên sản phẩm tối đa 50 ký tự';
      return;
  }

};

const clearVariantError = (index) => {
  if (variantErrors[index]) delete variantErrors[index];
};
const resetValidation = () => {
  validationErrors.productName = "";
  validationErrors.categoryId = "";
  validationErrors.variants = "";
  formErrors.value = [];
  Object.keys(variantErrors).forEach((key) => delete variantErrors[key]);
};

const mergeDuplicateVariants = () => {
  const seen = new Map();
  let mergedCount = 0;
  for (let i = 0; i < form.value.variants.length; i++) {
    const v = form.value.variants[i];
    if (!v.colorId || !v.sizeId) continue;
    const key = `${v.colorId}-${v.sizeId}`;
    if (seen.has(key)) {
      const firstIndex = seen.get(key);
      const firstVariant = form.value.variants[firstIndex];
      firstVariant.quantity =
        (Number(firstVariant.quantity) || 0) + (Number(v.quantity) || 0);
      if (Number(v.price) < Number(firstVariant.price))
        firstVariant.price = Number(v.price);
      if (v.promotionIds && v.promotionIds.length > 0) {
        if (!firstVariant.promotionIds) firstVariant.promotionIds = [];
        if (!firstVariant.promotionsDetail) firstVariant.promotionsDetail = [];
        v.promotionIds.forEach((id) => {
          if (!firstVariant.promotionIds.includes(id)) firstVariant.promotionIds.push(id);
        });
        (v.promotionsDetail || []).forEach((p) => {
          if (!firstVariant.promotionsDetail.some((fp) => fp.promotionId === p.promotionId)) {
            firstVariant.promotionsDetail.push(p);
          }
        });
      }
      v._duplicate = true;
      mergedCount++;
    } else {
      if (!v.promotionIds) v.promotionIds = [];
      if (!v.promotionsDetail) v.promotionsDetail = [];
      seen.set(key, i);
    }
  }
  for (let i = form.value.variants.length - 1; i >= 0; i--) {
    if (form.value.variants[i]._duplicate) form.value.variants.splice(i, 1);
  }
  if (mergedCount > 0)
    showNotification(`✅ Đã tự động gộp ${mergedCount} biến thể trùng lặp!`, "info");
};

const validateForm = () => {
  let isValid = true;
  formErrors.value = [];
  resetValidation();
  if (!form.value.productName || form.value.productName.trim() === "") {
    validationErrors.productName = "Tên sản phẩm không được để trống";
    formErrors.value.push("Tên sản phẩm không được để trống");
    isValid = false;
  } else if (/[^a-zA-Z0-9\sÀ-ỹà-ỹđĐ\p{L}\p{N}]/u.test(form.value.productName.trim())) {
    validationErrors.productName = "Tên sản phẩm không được chứa ký tự đặc biệt";
    formErrors.value.push("Tên sản phẩm không được chứa ký tự đặc biệt");
    isValid = false;
  } else if (form.value.productName.trim().length < 5) {
    validationErrors.productName = "Tên sản phẩm phải từ 5 ký tự trở lên";
    formErrors.value.push("Tên sản phẩm phải từ 5 ký tự trở lên");
    isValid = false;
  } else if (form.value.productName.trim().length > 50) {
    validationErrors.productName = "Tên sản phẩm không được vượt quá 50 ký tự";
    formErrors.value.push("Tên sản phẩm không được vượt quá 50 ký tự");
    isValid = false;
  }
  if (!form.value.categoryId) {
    validationErrors.categoryId = "Vui lòng chọn danh mục";
    formErrors.value.push("Vui lòng chọn danh mục");
    isValid = false;
  }
  if (form.value.variants.length === 0) {
    validationErrors.variants = "Phải có ít nhất một biến thể";
    formErrors.value.push("Phải có ít nhất một biến thể");
    isValid = false;
  } else {
    for (let i = 0; i < form.value.variants.length; i++) {
      const v = form.value.variants[i];
      if (!v.colorId) {
        if (!variantErrors[i]) variantErrors[i] = {};
        variantErrors[i].color = "Chọn màu";
        formErrors.value.push(`Biến thể ${i + 1}: Chưa chọn màu sắc`);
        isValid = false;
      }
      if (!v.sizeId) {
        if (!variantErrors[i]) variantErrors[i] = {};
        variantErrors[i].size = "Chọn size";
        formErrors.value.push(`Biến thể ${i + 1}: Chưa chọn kích thước`);
        isValid = false;
      }
      if (!v.price || v.price < 50000) {
        if (!variantErrors[i]) variantErrors[i] = {};
        variantErrors[i].price = "Giá phải từ 50.000đ";
        formErrors.value.push(`Biến thể ${i + 1}: Giá phải từ 50.000đ trở lên`);
        isValid = false;
      }
      if (!v.quantity || v.quantity < 1) {
        if (!variantErrors[i]) variantErrors[i] = {};
        variantErrors[i].quantity = "SL tối thiểu là 1";
        formErrors.value.push(`Biến thể ${i + 1}: Số lượng tối thiểu là 1`);
        isValid = false;
      }
    }
  }
  return isValid;
};

const fetchProducts = async () => {
  loading.value = true;
  try {
    const response = await axios.get(`${API_BASE}/products`, {
      params: { page: 0, size: 100 },
    });
    let products = [];
    if (response.data?.data?.content) products = response.data.data.content;
    else if (response.data?.content) products = response.data.content;
    else if (Array.isArray(response.data)) products = response.data;
    allProducts.value = products.map((p) => ({
      ...p,
      _deletedAt: p.status === "DELETED" ? new Date().toISOString() : null,
    }));
    totalPages.value = Math.ceil(filteredProducts.value.length / pageSize.value);
    currentPage.value = 0;
  } catch (error) {
    showNotification("Không thể tải sản phẩm", "error");
    allProducts.value = [];
  } finally {
    loading.value = false;
  }
};

const fetchStatistics = async () => {
  try {
    const response = await axios.get(`${API_BASE}/products/statistics`);
    if (response.data?.data) statistics.value = response.data.data;
  } catch (error) { }
};

const fetchCategories = async () => {
  try {
    const response = await axios.get(`${API_BASE}/categories`);
    let categories = [];
    if (response.data?.data?.content) categories = response.data.data.content;
    else if (response.data?.content) categories = response.data.content;
    else if (Array.isArray(response.data)) categories = response.data;
    categoryList.value = categories.filter(
      (c) => c.status === "ACTIVE" && !c.deletedAt && c.categoryStatus !== "DELETED"
    );
  } catch (error) {
    categoryList.value = [];
  }
};

const fetchColors = async () => {
  try {
    const response = await axios.get(`${API_BASE}/colors`, {
      params: { page: 0, size: 100 },
    });
    let colors = [];
    if (response.data?.data?.content) colors = response.data.data.content;
    else if (response.data?.content) colors = response.data.content;
    else if (Array.isArray(response.data)) colors = response.data;
    colorList.value = colors.filter(
      (c) => c.status === "ACTIVE" && !c.deletedAt && c.colorStatus !== "DELETED"
    );
  } catch (error) {
    colorList.value = [];
  }
};

const fetchSizes = async () => {
  try {
    const response = await axios.get(`${API_BASE}/sizes`, {
      params: { page: 0, size: 100 },
    });
    let sizes = [];
    if (response.data?.data?.content) sizes = response.data.data.content;
    else if (response.data?.content) sizes = response.data.content;
    else if (Array.isArray(response.data)) sizes = response.data;
    sizeList.value = sizes.filter(
      (s) => s.status === "ACTIVE" && !s.deletedAt && s.sizeStatus !== "DELETED"
    );
  } catch (error) {
    sizeList.value = [];
  }
};

const fetchPromotions = async () => {
  try {
    const token = localStorage.getItem("token");
    const response = await axios.get(`${API_BASE}/admin/promotions`, {
      params: { page: 0, size: 100 },
      headers: { Authorization: `Bearer ${token}` },
    });
    let promotions = [];
    if (response.data?.content) promotions = response.data.content;
    else if (response.data?.data?.content) promotions = response.data.data.content;
    else if (Array.isArray(response.data)) promotions = response.data;

    // ✅ Chỉ giữ lại khuyến mãi thực sự còn hiệu lực (đúng ngày + đang ACTIVE),
    // để tránh chọn được ở form nhưng backend lại âm thầm từ chối áp dụng
    const now = new Date();
    promotionList.value = promotions.filter((p) => {
      if (p.promotionStatus !== "ACTIVE" || p.promotionDeletedAt) return false;
      const start = p.promotionStartDate ? new Date(p.promotionStartDate) : null;
      const end = p.promotionEndDate ? new Date(p.promotionEndDate) : null;
      if (start && now < start) return false;
      if (end && now > end) return false;
      return true;
    });
  } catch (error) {
    promotionList.value = [];
  }
};

const resetFilters = () => {
  searchKeyword.value = "";
  selectedCategoryId.value = null;
  selectedStatus.value = null;
  fetchProducts();
};
const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) currentPage.value = page;
};

const addVariant = () => {
  form.value.variants.push({
    colorId: null,
    sizeId: null,
    price: 0,
    quantity: 1,
    sku: "",
    promotionIds: [],
    promotionsDetail: [],
  });
};

const removeVariant = (index) => {
  form.value.variants.splice(index, 1);
  if (variantErrors[index]) delete variantErrors[index];
};

const openCreateModal = () => {
  isEdit.value = false;
  form.value = {
    productId: null,
    productName: "",
    categoryId: null,
    description: "",
    isActive: true,
    promotionId: null,
    variants: [],
  };
  resetValidation();
  showModal.value = true;
};

const openEditModal = async (product) => {
  if (product._deletedAt) {
    showNotification("Không thể sửa sản phẩm đã xóa!", "error");
    return;
  }
  if (categoryList.value.length === 0) await fetchCategories();
  if (colorList.value.length === 0) await fetchColors();
  if (sizeList.value.length === 0) await fetchSizes();
  if (promotionList.value.length === 0) await fetchPromotions();
  isEdit.value = true;
  let categoryId = product.categoryId || product.category?.categoryId || null;
  if (!categoryId && product.category?.categoryName) {
    const foundCategory = categoryList.value.find(
      (c) => c.categoryName === product.category.categoryName
    );
    categoryId = foundCategory?.categoryId ? Number(foundCategory.categoryId) : null;
  } else if (categoryId) categoryId = Number(categoryId);
  const variants = (product.variants || []).map((v) => {
    let colorId = v.colorId;
    if (!colorId && v.color) {
      const color = colorList.value.find((c) => c.colorName === v.color);
      colorId = color?.colorId ? Number(color.colorId) : null;
    } else if (colorId) colorId = Number(colorId);
    let sizeId = v.sizeId;
    if (!sizeId && v.size) {
      const size = sizeList.value.find((s) => s.sizeName === v.size);
      sizeId = size?.sizeId ? Number(size.sizeId) : null;
    } else if (sizeId) sizeId = Number(sizeId);
    return {
      colorId,
      sizeId,
      price: Number(v.price) || 0,
      quantity: Number(v.quantity) || 0,
      sku: v.sku || "",
      promotionIds: v.promotions
        ? v.promotions.map((p) => p.promotionId)
        : v.promotionIds || [],
      // ✅ Lưu luôn % giảm giá + tên tại thời điểm load, không phụ thuộc tra cứu lại promotionList sau này
      promotionsDetail: v.promotions
        ? v.promotions.map((p) => ({
          promotionId: p.promotionId,
          promotionName: p.promotionName,
          promotionDiscountPercent: p.promotionDiscountPercent,
        }))
        : v.promotionsDetail || [],
    };
  });
  form.value = {
    productId: Number(product.productId),
    productName: product.productName,
    categoryId,
    description: product.description || "",
    isActive: product.status === "ACTIVE",
    promotionId: product.promotion?.promotionId || null,
    variants,
  };
  resetValidation();
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  isEdit.value = false;
  form.value = {
    productId: null,
    productName: "",
    categoryId: null,
    description: "",
    isActive: true,
    promotionId: null,
    variants: [],
  };
  resetValidation();
};

const handleSubmit = async () => {
  mergeDuplicateVariants();
  form.value.productName =
    form.value.productName.trim();
  if (!validateForm()) {
    showNotification("Vui lòng kiểm tra lại thông tin!", "error");
    return;
  }
  saving.value = true;
  try {
    const submitData = {
      productName: form.value.productName.trim(),
      categoryId: Number(form.value.categoryId),
      description: form.value.description || "",
      status: form.value.isActive ? "ACTIVE" : "INACTIVE",

      promotionId: form.value.promotionId
        ? Number(form.value.promotionId)
        : null,

      variants: form.value.variants.map((v, index) => ({
        productName: form.value.productName.trim(),

        colorId: Number(v.colorId),

        sizeId: Number(v.sizeId),

        price: Number(v.price),

        quantity: Number(v.quantity),

        sku: v.sku || `SKU-${Date.now()}-${index}`,

        promotionIds: Array.isArray(v.promotionIds)
          ? [...v.promotionIds]
          : []
      }))
    };

    console.log(submitData);
    if (isEdit.value) {
      await axios.put(`${API_BASE}/products/${form.value.productId}`, submitData);
      showNotification("Cập nhật sản phẩm thành công!");
    } else {
      await axios.post(`${API_BASE}/products`, submitData);
      showNotification("Thêm sản phẩm thành công!");
    }
    closeModal();
    await fetchProducts();
    await fetchStatistics();
  } catch (error) {
    if (error.response?.data?.errors)
      formErrors.value = Object.values(error.response.data.errors).flat();
    showNotification(error.response?.data?.message || "Có lỗi xảy ra!", "error");
  } finally {
    saving.value = false;
  }

};


const confirmDelete = (product) => {
  deleteProductId.value = product.productId;
  deleteProductName.value = product.productName;
  showDeleteModal.value = true;
};
const closeDeleteModal = () => {
  showDeleteModal.value = false;
  deleteProductId.value = null;
  deleteProductName.value = "";
};
const deleteProduct = async () => {
  try {
    await axios.delete(`${API_BASE}/products/${deleteProductId.value}`);
    const index = allProducts.value.findIndex(
      (p) => p.productId === deleteProductId.value
    );
    if (index !== -1)
      allProducts.value[index] = {
        ...allProducts.value[index],
        _deletedAt: new Date().toISOString(),
        status: "DELETED",
      };
    showNotification("Xóa sản phẩm thành công!", "success");
    closeDeleteModal();
    await fetchStatistics();
  } catch (error) {
    showNotification(error.response?.data?.message || "Không thể xóa sản phẩm!", "error");
  }
};

const confirmRestore = (product) => {
  restoreProductId.value = product.productId;
  restoreProductName.value = product.productName;
  showRestoreModal.value = true;
};
const closeRestoreModal = () => {
  showRestoreModal.value = false;
  restoreProductId.value = null;
  restoreProductName.value = "";
};
const restoreProduct = async () => {
  try {
    await axios.patch(`${API_BASE}/products/${restoreProductId.value}/restore`);
    const index = allProducts.value.findIndex(
      (p) => p.productId === restoreProductId.value
    );
    if (index !== -1)
      allProducts.value[index] = {
        ...allProducts.value[index],
        _deletedAt: null,
        status: "ACTIVE",
      };
    showNotification("Khôi phục sản phẩm thành công!", "success");
    closeRestoreModal();
    await fetchProducts();
    await fetchStatistics();
  } catch (error) {
    showNotification(
      error.response?.data?.message || "Không thể khôi phục sản phẩm!",
      "error"
    );
  }
};

const viewProductDetail = (product) => {
  showNotification(`Xem chi tiết sản phẩm: ${product.productName}`, "info");
};

onMounted(async () => {
  await fetchProducts();
  await fetchStatistics();
  await fetchCategories();
  await fetchColors();
  await fetchSizes();
  await fetchPromotions();
});
</script>


<style scoped>
.admin-products {
  padding: 20px;
  background-color: #f8f9fa;
  min-height: calc(100vh - 60px);
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h2 {
  margin: 0 0 5px 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #7f8c8d;
  font-size: 14px;
}

.btn-add {
  background-color: #3498db;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-add:hover {
  background-color: #2980b9;
  transform: translateY(-2px);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  border-left: 4px solid;
}

.stat-total {
  border-left-color: #3b82f6;
}

.stat-active {
  border-left-color: #10b981;
}

.stat-warning {
  border-left-color: #f59e0b;
}

.stat-deleted {
  border-left-color: #ef4444;
}

.stat-icon {
  font-size: 32px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.filter-bar {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  background: white;
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  min-width: 250px;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #95a5a6;
}

.search-box input {
  width: 100%;
  padding: 10px 35px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  outline: none;
  font-size: 14px;
}

.search-box input:focus {
  border-color: #3498db;
}

.clear-search {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  color: #95a5a6;
  cursor: pointer;
}

.filter-select {
  padding: 10px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  outline: none;
  background: white;
  cursor: pointer;
}

.btn-search {
  padding: 10px 20px;
  background: #2c3e50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.btn-reset {
  padding: 10px 20px;
  background: #f1f2f6;
  color: #2c3e50;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;
}

.data-table th,
.data-table td {
  padding: 15px;
  text-align: left;
  border-bottom: 1px solid #f1f4f8;
}

.data-table th {
  background: #f8f9fa;
  color: #2c3e50;
  font-weight: 600;
  font-size: 14px;
  text-transform: uppercase;
}

.table-row:hover {
  background: #f9fafb;
}

.row-deleted {
  opacity: 0.6;
  background-color: #fafafa;
}

.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.status-active {
  background-color: #e8f8f5;
  color: #27ae60;
}

.status-inactive {
  background-color: #fdf2e9;
  color: #f39c12;
}

.status-deleted {
  background-color: #f3f4f6;
  color: #6b7280;
}

.promotion-badge {
  display: inline-block;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.variant-promo-indicator {
  background: #667eea;
  color: white;
  padding: 1px 5px;
  border-radius: 10px;
  font-size: 10px;
  margin-left: 4px;
}

.variant-promotions {
  min-width: 180px;
}

.promo-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 5px;
}

.promo-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 10px;
  white-space: nowrap;
}

.promo-tag-remove {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 13px;
  padding: 0;
  line-height: 1;
  opacity: 0.8;
}

.promo-tag-remove:hover {
  opacity: 1;
}

.promo-add-select {
  width: 100%;
  padding: 4px 6px;
  border: 1px dashed #667eea;
  border-radius: 4px;
  font-size: 11px;
  background: #f8f9ff;
  cursor: pointer;
  color: #667eea;
}

.form-errors-container {
  background: #fff5f5;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 20px;
}

.form-errors-header {
  font-weight: 600;
  color: #dc2626;
  margin-bottom: 8px;
  font-size: 14px;
}

.form-errors-list {
  margin: 0;
  padding-left: 20px;
}

.form-errors-list li {
  color: #dc2626;
  font-size: 13px;
  margin-bottom: 4px;
}

.form-group {
  margin-bottom: 20px;
  position: relative;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #2c3e50;
}

input[type="checkbox"] {
  width: auto !important;
  height: auto;
}

.form-group input:not([type="checkbox"]),
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}

.form-group.has-error input,
.form-group.has-error select {
  border-color: #dc3545;
}

.char-count {
  position: absolute;
  right: 12px;
  top: 38px;
  font-size: 11px;
  color: #9ca3af;
}

.field-error {
  display: block;
  color: #dc3545;
  font-size: 12px;
  margin-top: 5px;
  font-weight: 500;
}

.field-error-small {
  display: block;
  color: #dc3545;
  font-size: 11px;
  margin-top: 3px;
}

.required {
  color: #e74c3c;
}

.variants-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.btn-add-variant {
  background: #4caf50;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.variants-table {
  width: 100%;
  border-collapse: collapse;
}

.variants-table th,
.variants-table td {
  padding: 10px;
  border: 1px solid #eee;
  text-align: left;
}

.variants-table th {
  background: #f8f9fa;
  font-weight: 600;
  color: #2c3e50;
  font-size: 13px;
}

.variants-table input,
.variants-table select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
}

.discounted-price {
  background-color: #e8f5e9;
  color: #2e7d32;
  font-weight: 600;
  text-align: right;
}

.btn-remove-variant {
  background: #f44336;
  color: white;
  border: none;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.variant-error-row td {
  background-color: #fff5f5;
}

.variant-duplicate-row td {
  background-color: #fff3cd !important;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.modal-container {
  background: white;
  border-radius: 12px;
  width: 900px;
  max-width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-container.modal-lg {
  width: 1000px;
}

.modal-container.modal-sm {
  width: 400px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 12px 12px 0 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: white;
}

.modal-body {
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-section {
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.form-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.form-section h4 {
  margin-bottom: 20px;
  color: #2c3e50;
  font-size: 18px;
}

.btn-cancel,
.btn-save,
.btn-danger {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-cancel {
  background: #95a5a6;
  color: white;
}

.btn-save {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.warning-text {
  color: #dc3545;
  margin-top: 10px;
  font-size: 13px;
}

.toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  padding: 12px 24px;
  border-radius: 8px;
  background: #27ae60;
  color: white;
  z-index: 9999;
  opacity: 0;
  transition: all 0.3s;
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 15px;
  background: white;
  border-radius: 12px;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-controls button {
  padding: 5px 12px;
  border: 1px solid #e0e6ed;
  background: white;
  border-radius: 6px;
  cursor: pointer;
}

.page-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e5e7eb;
  border-top-color: #3498db;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.loading-spinner-small {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid white;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  margin-right: 6px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.promotion-select {
  min-width: 140px;
}

.form-hint {
  font-size: 12px;
  color: #f59e0b;
  margin-top: 5px;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-direction: column;
  }

  .modal-container {
    width: 95%;
  }
}

/* ===== ACTION BUTTONS ===== */

.actions {
  text-align: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.action-btn {
  width: 34px;
  height: 34px;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 0;

  border: none !important;
  outline: none !important;
  box-shadow: none !important;

  background: transparent;

  border-radius: 6px;

  cursor: pointer;

  font-size: 16px;

  transition: all 0.2s ease;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.action-btn:focus,
.action-btn:focus-visible,
.action-btn:active {
  outline: none !important;
  border: none !important;
  box-shadow: none !important;
}

.edit-btn {
  color: #2563eb;
}

.edit-btn:hover {
  background: #dbeafe;
}

.delete-btn {
  color: #dc2626;
}

.delete-btn:hover {
  background: #fee2e2;
}

.restore-btn {
  color: #16a34a;
}

.restore-btn:hover {
  background: #dcfce7;
}

.view-btn {
  color: #4b5563;
}

.view-btn:hover {
  background: #f3f4f6;
}

.variant-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-colors,
.summary-sizes {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.summary-tag {
  padding: 3px 8px;
  border-radius: 12px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 11px;
  font-weight: 600;
}

.summary-size {
  padding: 3px 8px;
  border-radius: 12px;
  background: #f3f4f6;
  font-size: 11px;
}

.btn-detail {
  border: none;
  border-radius: 6px;
  padding: 5px 10px;
  background: #667eea;
  color: #fff;
  cursor: pointer;
  font-size: 12px;
  width: max-content;
}

.btn-detail:hover {
  opacity: 0.9;
}

.stock-badge {
  display: inline-flex;
  align-items: center;

  padding: 6px 12px;

  border-radius: 20px;

  background: #eff6ff;

  color: #2563eb;

  font-weight: 600;
}

.stock-cell {
  min-width: 120px;
}
</style>