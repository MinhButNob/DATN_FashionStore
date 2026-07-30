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
        <input
          v-model="searchKeyword"
          @keyup.enter="fetchProducts"
          placeholder="Tìm kiếm sản phẩm..."
          autocomplete="off"
        />
        <button v-if="searchKeyword" class="clear-search" @click="searchKeyword = ''; fetchProducts()">✕</button>
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
            <th>Khuyến mãi</th>
            <th>Biến thể & Tồn kho</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in paginatedProducts" :key="product.productId" class="table-row" :class="{ 'row-deleted': product._deletedAt }">
            <td class="id-cell">#{{ product.productId }}</td>
            <td class="product-name" :class="{ 'deleted-text': product._deletedAt }">{{ product.productName }}</td>
            <td class="category-cell">{{ product.category?.categoryName || '-' }}</td>
            
            <td class="promotion-cell">
              <span v-if="product.promotion && !product._deletedAt" class="promotion-badge">
                🎁 {{ product.promotion.promotionName }} (-{{ product.promotion.promotionDiscountPercent }}%)
              </span>
              <span v-else-if="product._deletedAt" class="deleted-label">Đã xóa</span>
              <span v-else class="no-promotion">—</span>
            </td>
            
            <td class="variants-cell">
              <div class="variants-list">
                <div v-for="(v, idx) in product.variants" :key="idx" class="variant-item">
                  <div class="variant-info">
                    <span class="variant-color" :style="{ backgroundColor: getColorHex(v.color) }"></span>
                    <span class="variant-name">{{ v.color || '?' }} - {{ v.size || '?' }}</span>
                  </div>
                  <div class="variant-price-stock">
                    <div class="variant-prices">
                      <span class="variant-original-price" v-if="v.discountedPrice && v.discountedPrice < v.price">
                        {{ formatPrice(v.price) }}
                      </span>
                      <span class="variant-price" :class="{ 'has-discount': v.discountedPrice && v.discountedPrice < v.price }">
                        {{ formatPrice(v.discountedPrice || v.price) }}
                      </span>
                    </div>
                    <span 
                      class="variant-stock" 
                      :class="{ 
                        'stock-low': v.quantity <= 5 && v.quantity > 0,
                        'stock-out': v.quantity === 0 
                      }"
                    >
                      📦 {{ v.quantity }}
                    </span>
                  </div>
                </div>
                <div v-if="!product.variants || product.variants.length === 0" class="no-variant">
                  Không có biến thể
                </div>
              </div>
            </td>
            
            <td class="status-cell">
              <span v-if="product._deletedAt" class="status-badge status-deleted">
                Đã xóa
              </span>
              <span v-else :class="['status-badge', product.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
                {{ product.status === 'ACTIVE' ? 'Đang bán' : 'Ngừng bán' }}
              </span>
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
        <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 0">‹</button>
        <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button @click="goToPage(currentPage + 1)" :disabled="currentPage + 1 >= totalPages">›</button>
        <button @click="goToPage(totalPages - 1)" :disabled="currentPage + 1 >= totalPages">»</button>
      </div>
    </div>

    <!-- Product Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-container modal-lg">
        <div class="modal-header">
          <h3>{{ isEdit ? '✏️ Sửa sản phẩm' : '➕ Thêm sản phẩm' }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>

        <form @submit.prevent="handleSubmit">
          <div class="modal-body">
            <div class="form-section">
              <h4>📝 Thông tin cơ bản</h4>
              
              <div class="form-group">
                <label>Tên sản phẩm <span class="required">*</span></label>
                <input v-model="form.productName" type="text" placeholder="Nhập tên sản phẩm" required />
              </div>

              <div class="form-group category-search-group">
                <label>Danh mục <span class="required">*</span></label>
                <div class="category-search-box">
                  <input
                    type="text"
                    v-model="categorySearchQuery"
                    placeholder="Gõ để tìm danh mục..."
                    autocomplete="off"
                    @focus="showCategorySuggestions = true"
                    @input="showCategorySuggestions = true"
                    @blur="onCategorySearchBlur"
                  />
                  <ul v-if="showCategorySuggestions" class="category-suggestions">
                    <li v-if="filteredCategorySuggestions.length === 0" class="no-result">
                      Không tìm thấy danh mục phù hợp
                    </li>
                    <li
                      v-for="cat in filteredCategorySuggestions"
                      :key="cat.categoryId"
                      @mousedown.prevent="selectCategorySuggestion(cat)"
                    >
                      {{ cat.categoryName }}
                    </li>
                  </ul>
                </div>
              </div>

              <div class="form-group">
                <label>🎁 Khuyến mãi áp dụng</label>
                <select v-model="form.promotionId" class="promotion-select">
                  <option :value="null">-- Không áp dụng --</option>
                  <option v-for="promo in promotionList" :key="promo.promotionId" :value="promo.promotionId">
                    {{ promo.promotionName }} (-{{ promo.promotionDiscountPercent }}%)
                  </option>
                </select>
                <p class="form-hint" v-if="form.promotionId">
                  ⚡ Khuyến mãi sẽ được áp dụng cho TẤT CẢ các biến thể của sản phẩm này
                </p>
              </div>

              <div class="form-group">
                <label class="checkbox-label">
                  <input type="checkbox" v-model="form.isActive" />
                  <span>✅ Đang bán</span>
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

              <div class="variants-table-wrapper">
                <table class="variants-table">
                  <thead>
                    <tr>
                      <th>Màu sắc</th>
                      <th>Kích thước</th>
                      <th>Giá gốc (VNĐ)</th>
                      <th>Giá sau KM (VNĐ)</th>
                      <th>Số lượng</th>
                      <th>SKU</th>
                      <th style="width: 50px"></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(variant, index) in form.variants" :key="index">
                      <td>
                        <select v-model="variant.colorId" required>
                          <option :value="null">Chọn màu</option>
                          <option v-for="color in colorList" :key="color.colorId" :value="color.colorId">
                            {{ color.colorName }}
                          </option>
                        </select>
                      </td>
                      <td>
                        <select v-model="variant.sizeId" required>
                          <option :value="null">Chọn size</option>
                          <option v-for="size in sizeList" :key="size.sizeId" :value="size.sizeId">
                            {{ size.sizeName }}
                          </option>
                        </select>
                      </td>
                      <td>
                        <input type="number" v-model="variant.price" placeholder="Giá gốc" required @input="calculateAllDiscountedPrices" />
                      </td>
                      <td>
                        <input type="text" :value="formatPrice(getDiscountedPrice(variant.price))" class="discounted-price" readonly disabled />
                      </td>
                      <td>
                        <input type="number" v-model="variant.quantity" placeholder="Số lượng" required min="0" />
                      </td>
                      <td>
                        <input type="text" v-model="variant.sku" placeholder="SKU" />
                      </td>
                      <td>
                        <button type="button" class="btn-remove-variant" @click="removeVariant(index)">
                          🗑️
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="closeModal">
              Hủy
            </button>
            <button type="submit" class="btn-save" :disabled="saving">
              <span v-if="saving" class="loading-spinner-small"></span>
              <span v-else>💾</span>
              {{ saving ? 'Đang lưu...' : 'Lưu sản phẩm' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete Modal -->
    <div v-if="showDeleteModal" class="modal-overlay">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận xóa</h3>
          <button class="close-btn" @click="closeDeleteModal">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc muốn xóa sản phẩm <strong>{{ deleteProductName }}</strong>?</p>
          <p class="warning-text">Sau khi xóa, sản phẩm sẽ chuyển sang trạng thái đã xóa và có thể khôi phục lại!</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeDeleteModal">Hủy</button>
          <button class="btn-danger" @click="deleteProduct">Xóa</button>
        </div>
      </div>
    </div>

    <!-- Restore Modal -->
    <div v-if="showRestoreModal" class="modal-overlay">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận khôi phục</h3>
          <button class="close-btn" @click="closeRestoreModal">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc muốn khôi phục sản phẩm <strong>{{ restoreProductName }}</strong>?</p>
          <p>Sau khi khôi phục, sản phẩm sẽ trở lại trạng thái ban đầu và có thể chỉnh sửa.</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeRestoreModal">Hủy</button>
          <button class="btn-save" @click="restoreProduct">Khôi phục</button>
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
import { ref, computed, onMounted } from 'vue';
import api from "@/api/axios";

// State
const allProducts = ref([]);
const categoryList = ref([]);
// Tìm kiếm/gợi ý danh mục khi thêm/sửa sản phẩm (thay cho combobox)
const categorySearchQuery = ref('');
const showCategorySuggestions = ref(false);
const colorList = ref([]);
const sizeList = ref([]);
const promotionList = ref([]);
const statistics = ref({
  totalProducts: 0,
  inStockVariants: 0,
  lowStockVariants: 0,
  deletedProducts: 0
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
const deleteProductName = ref('');
const restoreProductId = ref(null);
const restoreProductName = ref('');

// Filters
const searchKeyword = ref('');
const selectedCategoryId = ref(null);
const selectedStatus = ref(null);

// Toast
const showToast = ref(false);
const toastMessage = ref('');
const toastType = ref('success');
let toastTimer = null;

// Form data
const form = ref({
  productId: null,
  productName: '',
  categoryId: null,
  description: '',
  isActive: true,
  promotionId: null,
  variants: []
});

// Computed
const filteredProducts = computed(() => {
  let filtered = [...allProducts.value];
  
  if (searchKeyword.value && searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase();
    filtered = filtered.filter(p => 
      p.productName?.toLowerCase().includes(kw)
    );
  }
  
  if (selectedCategoryId.value) {
    filtered = filtered.filter(p => 
      p.categoryId === selectedCategoryId.value || 
      p.category?.categoryId === selectedCategoryId.value
    );
  }
  
  if (selectedStatus.value) {
    filtered = filtered.filter(p => p.status === selectedStatus.value);
  }
  
  return filtered;
});

const paginatedProducts = computed(() => {
  const start = currentPage.value * pageSize.value;
  const end = start + pageSize.value;
  return filteredProducts.value.slice(start, end);
});

// Helper functions
const showNotification = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer);
  toastMessage.value = message;
  toastType.value = type;
  showToast.value = true;
  toastTimer = setTimeout(() => {
    showToast.value = false;
  }, 3000);
};

const getColorHex = (colorName) => {
  const color = colorList.value.find(c => c.colorName === colorName);
  return color?.hexCode || '#ccc';
};

const formatPrice = (price) => {
  if (!price) return '0đ';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const getCurrentPromotionDiscount = () => {
  if (!form.value.promotionId) return 0;
  const promotion = promotionList.value.find(p => p.promotionId === form.value.promotionId);
  return promotion?.promotionDiscountPercent || 0;
};

const getDiscountedPrice = (originalPrice) => {
  const discountPercent = getCurrentPromotionDiscount();
  if (!discountPercent || discountPercent === 0 || !originalPrice) return originalPrice;
  return originalPrice * (1 - discountPercent / 100);
};

const calculateAllDiscountedPrices = () => {};

// Fetch data
const fetchProducts = async () => {
  loading.value = true;
  try {
    const response = await api.get(`/products`, {
      params: { page: 0, size: 100 }
    });
    
    let products = [];
    if (response.data?.data?.content) {
      products = response.data.data.content;
    } else if (response.data?.content) {
      products = response.data.content;
    } else if (Array.isArray(response.data)) {
      products = response.data;
    }
    
    // ✅ GIỮ TẤT CẢ SẢN PHẨM (bao gồm cả đã xóa)
    allProducts.value = products.map(p => ({
      ...p,
      _deletedAt: p.status === 'DELETED' ? new Date().toISOString() : null
    }));
    totalPages.value = Math.ceil(filteredProducts.value.length / pageSize.value);
    currentPage.value = 0;
    
    console.log('✅ Products loaded:', allProducts.value.length);
  } catch (error) {
    console.error('❌ Lỗi tải sản phẩm:', error);
    showNotification('Không thể tải sản phẩm', 'error');
    allProducts.value = [];
  } finally {
    loading.value = false;
  }
};

const fetchStatistics = async () => {
  try {
    const response = await api.get(`/products/statistics`);
    const data = response.data?.data;
    if (data) {
      statistics.value = data;
    }
  } catch (error) {
    console.error('Lỗi tải thống kê:', error);
  }
};

const fetchCategories = async () => {
  try {
    const response = await api.get(`/categories`);
    let categories = [];
    
    if (response.data?.data?.content) {
      categories = response.data.data.content;
    } else if (response.data?.content) {
      categories = response.data.content;
    } else if (Array.isArray(response.data)) {
      categories = response.data;
    }
    
    // Không cho thêm sản phẩm trực tiếp vào 3 danh mục lớn mặc định
    // (Áo/Quần/Phụ kiện) - chỉ được chọn các danh mục khác (danh mục con, v.v...)
    const DEFAULT_LOCKED_NAMES = ['Áo', 'Quần', 'Phụ kiện'];
    categoryList.value = categories.filter(c => 
      c.status === 'ACTIVE' && !c.deletedAt && c.categoryStatus !== 'DELETED' &&
      !DEFAULT_LOCKED_NAMES.includes(c.categoryName)
    );
  } catch (error) {
    console.error('Lỗi tải danh mục:', error);
    categoryList.value = [];
  }
};

const fetchColors = async () => {
  try {
    const response = await api.get(`/colors`, {
      params: { page: 0, size: 100 }
    });
    
    let colors = [];
    if (response.data?.data?.content) {
      colors = response.data.data.content;
    } else if (response.data?.content) {
      colors = response.data.content;
    } else if (Array.isArray(response.data)) {
      colors = response.data;
    }
    
    colorList.value = colors.filter(c => 
      c.status === 'ACTIVE' && !c.deletedAt && c.colorStatus !== 'DELETED'
    );
  } catch (error) {
    console.error('Lỗi tải màu:', error);
    colorList.value = [];
  }
};

const fetchSizes = async () => {
  try {
    const response = await api.get(`/sizes`, {
      params: { page: 0, size: 100 }
    });
    
    let sizes = [];
    if (response.data?.data?.content) {
      sizes = response.data.data.content;
    } else if (response.data?.content) {
      sizes = response.data.content;
    } else if (Array.isArray(response.data)) {
      sizes = response.data;
    }
    
    sizeList.value = sizes.filter(s => 
      s.status === 'ACTIVE' && !s.deletedAt && s.sizeStatus !== 'DELETED'
    );
  } catch (error) {
    console.error('Lỗi tải size:', error);
    sizeList.value = [];
  }
};

const fetchPromotions = async () => {
  try {
    const token = localStorage.getItem('token');
    const response = await api.get(`/admin/promotions`, {
      params: { page: 0, size: 100 },
      headers: { 'Authorization': `Bearer ${token}` }
    });
    
    let promotions = [];
    if (response.data?.content) {
      promotions = response.data.content;
    } else if (response.data?.data?.content) {
      promotions = response.data.data.content;
    } else if (Array.isArray(response.data)) {
      promotions = response.data;
    }
    
    promotionList.value = promotions.filter(p => 
      p.promotionStatus === 'ACTIVE' && !p.promotionDeletedAt
    );
  } catch (error) {
    console.error('Lỗi tải khuyến mãi:', error);
    promotionList.value = [];
  }
};

// Gợi ý danh mục khi thêm/sửa sản phẩm (không cho gợi ý 3 danh mục lớn - đã bị loại ở fetchCategories)
const filteredCategorySuggestions = computed(() => {
  const query = categorySearchQuery.value.trim().toLowerCase();
  if (!query) {
    // Chưa gõ gì -> hiện danh sách danh mục tiêu biểu (tối đa 8 cái đầu)
    return categoryList.value.slice(0, 8);
  }
  return categoryList.value.filter(c =>
    (c.categoryName || '').toLowerCase().includes(query)
  );
});

const selectCategorySuggestion = (cat) => {
  form.value.categoryId = cat.categoryId;
  categorySearchQuery.value = cat.categoryName;
  showCategorySuggestions.value = false;
};

const onCategorySearchBlur = () => {
  // Delay nhỏ để @mousedown.prevent trên item kịp chạy trước khi đóng dropdown
  setTimeout(() => {
    showCategorySuggestions.value = false;
    // Nếu người dùng gõ nhưng không chọn danh mục nào khớp -> khôi phục lại tên đang chọn (nếu có)
    const current = categoryList.value.find(c => c.categoryId === form.value.categoryId);
    categorySearchQuery.value = current?.categoryName || '';
  }, 150);
};

// Filter actions
const resetFilters = () => {
  searchKeyword.value = '';
  selectedCategoryId.value = null;
  selectedStatus.value = null;
  fetchProducts();
};

const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page;
  }
};

// Variant functions
const addVariant = () => {
  form.value.variants.push({
    colorId: null,
    sizeId: null,
    price: 0,
    quantity: 0,
    sku: ''
  });
};

const removeVariant = (index) => {
  form.value.variants.splice(index, 1);
};

// Modal functions
const openCreateModal = () => {
  isEdit.value = false;
  form.value = {
    productId: null,
    productName: '',
    categoryId: null,
    description: '',
    isActive: true,
    promotionId: null,
    variants: []
  };
  categorySearchQuery.value = '';
  showModal.value = true;
};

const openEditModal = async (product) => {
  if (product._deletedAt) {
    showNotification('Không thể sửa sản phẩm đã xóa! Vui lòng khôi phục trước.', 'error');
    return;
  }
  
  if (categoryList.value.length === 0) await fetchCategories();
  if (colorList.value.length === 0) await fetchColors();
  if (sizeList.value.length === 0) await fetchSizes();
  if (promotionList.value.length === 0) await fetchPromotions();
  
  isEdit.value = true;
  
  let categoryId = product.categoryId || product.category?.categoryId || null;
  if (!categoryId && product.category?.categoryName) {
    const foundCategory = categoryList.value.find(c => c.categoryName === product.category.categoryName);
    categoryId = foundCategory?.categoryId ? Number(foundCategory.categoryId) : null;
  } else if (categoryId) {
    categoryId = Number(categoryId);
  }
  
  const variants = (product.variants || []).map(v => {
    let colorId = v.colorId;
    if (!colorId && v.color) {
      const color = colorList.value.find(c => c.colorName === v.color);
      colorId = color?.colorId ? Number(color.colorId) : null;
    } else if (colorId) {
      colorId = Number(colorId);
    }
    
    let sizeId = v.sizeId;
    if (!sizeId && v.size) {
      const size = sizeList.value.find(s => s.sizeName === v.size);
      sizeId = size?.sizeId ? Number(size.sizeId) : null;
    } else if (sizeId) {
      sizeId = Number(sizeId);
    }
    
    return {
      colorId: colorId,
      sizeId: sizeId,
      price: Number(v.price) || 0,
      quantity: Number(v.quantity) || 0,
      sku: v.sku || ''
    };
  });
  
  form.value = {
    productId: Number(product.productId),
    productName: product.productName,
    categoryId: categoryId,
    description: product.description || '',
    isActive: product.status === 'ACTIVE',
    promotionId: product.promotion?.promotionId || null,
    variants: variants
  };
  
  const currentCategory = categoryList.value.find(c => c.categoryId === categoryId);
  categorySearchQuery.value = currentCategory?.categoryName || product.category?.categoryName || '';
  
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  isEdit.value = false;
  form.value = {
    productId: null,
    productName: '',
    categoryId: null,
    description: '',
    isActive: true,
    promotionId: null,
    variants: []
  };
  categorySearchQuery.value = '';
  showCategorySuggestions.value = false;
};

// Submit form
const handleSubmit = async () => {
  if (!form.value.productName) {
    showNotification('Vui lòng nhập tên sản phẩm!', 'error');
    return;
  }
  if (!form.value.categoryId) {
    showNotification('Vui lòng chọn danh mục!', 'error');
    return;
  }
  if (form.value.variants.length === 0) {
    showNotification('Vui lòng thêm ít nhất 1 biến thể!', 'error');
    return;
  }
  
  for (let i = 0; i < form.value.variants.length; i++) {
    const v = form.value.variants[i];
    if (v.quantity < 0) {
      showNotification(`Số lượng biến thể ${i + 1} không được nhỏ hơn 0!`, 'error');
      return;
    }
    if (!v.colorId) {
      showNotification(`Vui lòng chọn màu cho biến thể ${i + 1}!`, 'error');
      return;
    }
    if (!v.sizeId) {
      showNotification(`Vui lòng chọn kích thước cho biến thể ${i + 1}!`, 'error');
      return;
    }
    if (!v.price || v.price <= 0) {
      showNotification(`Vui lòng nhập giá hợp lệ cho biến thể ${i + 1}!`, 'error');
      return;
    }
  }
  
  saving.value = true;
  
  try {
    const submitData = {
      productName: form.value.productName,
      categoryId: Number(form.value.categoryId),
      description: form.value.description || '',
      status: form.value.isActive ? 'ACTIVE' : 'INACTIVE',
      promotionId: form.value.promotionId ? Number(form.value.promotionId) : null,
      variants: form.value.variants.map((v, index) => ({
        colorId: Number(v.colorId),
        sizeId: Number(v.sizeId),
        price: Number(v.price),
        quantity: Number(v.quantity),
        sku: v.sku || `SKU-${Date.now()}-${index}`
      }))
    };
    
    if (isEdit.value) {
      await api.put(`/products/${form.value.productId}`, submitData);
      showNotification('Cập nhật sản phẩm thành công!');
    } else {
      await api.post(`/products`, submitData);
      showNotification('Thêm sản phẩm thành công!');
    }
    
    closeModal();
    await fetchProducts();
    await fetchStatistics();
  } catch (error) {
    console.error('Lỗi lưu sản phẩm:', error);
    showNotification(error.response?.data?.message || 'Có lỗi xảy ra!', 'error');
  } finally {
    saving.value = false;
  }
};

// ✅ SOFT DELETE - Xóa mềm (giữ lại trong danh sách)
const confirmDelete = (product) => {
  deleteProductId.value = product.productId;
  deleteProductName.value = product.productName;
  showDeleteModal.value = true;
};

const closeDeleteModal = () => {
  showDeleteModal.value = false;
  deleteProductId.value = null;
  deleteProductName.value = '';
};

const deleteProduct = async () => {
  try {
    // ✅ GỌI API SOFT DELETE
    await api.delete(`/products/${deleteProductId.value}`);
    
    // ✅ CẬP NHẬT TRẠNG THÁI (KHÔNG XÓA KHỎI DANH SÁCH)
    const index = allProducts.value.findIndex(p => p.productId === deleteProductId.value);
    if (index !== -1) {
      allProducts.value[index] = {
        ...allProducts.value[index],
        _deletedAt: new Date().toISOString(),
        status: 'DELETED'
      };
    }
    
    showNotification('Xóa sản phẩm thành công! Sản phẩm đã chuyển sang trạng thái đã xóa.', 'success');
    closeDeleteModal();
    await fetchStatistics();
  } catch (error) {
    console.error('Lỗi xóa sản phẩm:', error);
    showNotification(error.response?.data?.message || 'Không thể xóa sản phẩm!', 'error');
  }
};

// ✅ RESTORE - Khôi phục
const confirmRestore = (product) => {
  restoreProductId.value = product.productId;
  restoreProductName.value = product.productName;
  showRestoreModal.value = true;
};

const closeRestoreModal = () => {
  showRestoreModal.value = false;
  restoreProductId.value = null;
  restoreProductName.value = '';
};

const restoreProduct = async () => {
  try {
    // ✅ GỌI API RESTORE
    await api.patch(`/products/${restoreProductId.value}/restore`);
    
    // ✅ CẬP NHẬT TRẠNG THÁI
    const index = allProducts.value.findIndex(p => p.productId === restoreProductId.value);
    if (index !== -1) {
      allProducts.value[index] = {
        ...allProducts.value[index],
        _deletedAt: null,
        status: 'ACTIVE'
      };
    }
    
    showNotification('Khôi phục sản phẩm thành công!', 'success');
    closeRestoreModal();
    await fetchProducts();
    await fetchStatistics();
  } catch (error) {
    console.error('Lỗi khôi phục sản phẩm:', error);
    showNotification(error.response?.data?.message || 'Không thể khôi phục sản phẩm!', 'error');
  }
};

const viewProductDetail = (product) => {
  showNotification(`Xem chi tiết sản phẩm: ${product.productName}`, 'info');
};

// Lifecycle
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
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
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

.stat-total { border-left-color: #3b82f6; }
.stat-active { border-left-color: #10b981; }
.stat-warning { border-left-color: #f59e0b; }
.stat-deleted { border-left-color: #ef4444; }

.stat-icon { font-size: 32px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a2e; }
.stat-label { font-size: 12px; color: #6b7280; margin-top: 4px; }

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
  transition: border-color 0.2s;
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
  background-color: white;
  cursor: pointer;
}

.btn-search {
  padding: 10px 20px;
  background-color: #2c3e50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-search:hover {
  background-color: #34495e;
}

.btn-reset {
  padding: 10px 20px;
  background-color: #f1f2f6;
  color: #2c3e50;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.btn-reset:hover {
  background-color: #e0e0e0;
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

.data-table th, .data-table td {
  padding: 15px;
  text-align: left;
  border-bottom: 1px solid #f1f4f8;
}

.data-table th {
  background-color: #f8f9fa;
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

.row-deleted td {
  color: #9ca3af;
}

.row-deleted .product-name {
  color: #9ca3af;
}

.row-deleted .variant-item {
  opacity: 0.5;
}

.deleted-text {
  text-decoration: line-through;
  color: #9ca3af;
}

.deleted-label {
  color: #9ca3af;
  font-style: italic;
  font-size: 13px;
}

.id-cell {
  font-weight: 600;
  color: #3498db;
}

.product-name {
  font-weight: 500;
  color: #2c3e50;
}

.category-cell {
  color: #666;
}

.variants-cell {
  max-width: 450px;
}

.variants-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.variant-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

.variant-item:last-child {
  border-bottom: none;
}

.variant-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 110px;
}

.variant-color {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid #ddd;
}

.variant-name {
  font-weight: 500;
  font-size: 13px;
  color: #374151;
}

.variant-price-stock {
  display: flex;
  align-items: center;
  gap: 15px;
}

.variant-prices {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.variant-original-price {
  font-size: 11px;
  color: #9ca3af;
  text-decoration: line-through;
}

.variant-price {
  color: #e74c3c;
  font-weight: 600;
  font-size: 13px;
}

.variant-price.has-discount {
  color: #e74c3c;
  font-weight: 700;
}

.variant-stock {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  background-color: #e8f5e9;
  color: #2e7d32;
}

.variant-stock.stock-low {
  background-color: #fff3e0;
  color: #e65100;
}

.variant-stock.stock-out {
  background-color: #ffebee;
  color: #c62828;
}

.no-variant {
  color: #9ca3af;
  font-style: italic;
  padding: 8px 0;
  text-align: center;
}

.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.status-active { background-color: #e8f8f5; color: #27ae60; }
.status-inactive { background-color: #fdf2e9; color: #f39c12; }
.status-deleted { background-color: #f3f4f6; color: #6b7280; }

.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.action-btn {
  background: none;
  border: none;
  padding: 6px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.edit-btn:hover { background-color: #fef3c7; }
.delete-btn:hover { background-color: #fde8e8; }
.restore-btn:hover { background-color: #d1fae5; }
.view-btn:hover { background-color: #e5e7eb; }

.restore-btn {
  background: #10b981;
  color: white;
  border-radius: 6px;
  padding: 6px 10px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.restore-btn:hover {
  background: #059669;
  transform: scale(1.05);
}

.view-btn {
  background: #6b7280;
  color: white;
  border-radius: 6px;
  padding: 6px 10px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.view-btn:hover {
  background: #4b5563;
  transform: scale(1.05);
}

.promotion-cell {
  text-align: center;
}

.promotion-badge {
  display: inline-block;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.no-promotion {
  color: #9ca3af;
  font-size: 14px;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #3498db;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e5e7eb;
  border-top-color: #3498db;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
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

.empty-row {
  text-align: center;
  padding: 40px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
}

.empty-icon {
  font-size: 40px;
}

.btn-retry {
  margin-top: 10px;
  padding: 8px 16px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.btn-retry:hover {
  background: #2980b9;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 15px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.page-total {
  font-size: 14px;
  color: #6b7280;
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
  transition: all 0.2s;
}

.page-controls button:hover:not(:disabled) {
  background: #3498db;
  color: white;
  border-color: #3498db;
}

.page-controls button:disabled {
  background: #f8f9fa;
  color: #bdc3c7;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #4b5563;
  font-weight: 500;
  min-width: 70px;
  text-align: center;
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
  animation: slideIn 0.3s;
}

.modal-container.modal-lg {
  width: 1000px;
}

.modal-container.modal-sm {
  width: 400px;
}

@keyframes slideIn {
  from { transform: translateY(-30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
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

.close-btn:hover {
  opacity: 0.8;
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

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #2c3e50;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.category-search-group {
  position: relative;
}

.category-search-box {
  position: relative;
}

.category-search-box input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
}

.category-search-box input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.category-suggestions {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 20;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  max-height: 220px;
  overflow-y: auto;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.category-suggestions li {
  padding: 9px 14px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}

.category-suggestions li:hover {
  background: #f0f1ff;
  color: #667eea;
}

.category-suggestions li.no-result {
  color: #999;
  cursor: default;
}

.category-suggestions li.no-result:hover {
  background: transparent;
  color: #999;
}

.required {
  color: #e74c3c;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.checkbox-label input {
  width: auto;
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
  transition: all 0.3s;
}

.btn-add-variant:hover {
  background: #45a049;
  transform: translateY(-1px);
}

.variants-table-wrapper {
  overflow-x: auto;
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
  transition: all 0.2s;
}

.btn-remove-variant:hover {
  background: #d32f2f;
}

.promotion-select {
  min-width: 140px;
}

.form-hint {
  font-size: 12px;
  color: #f59e0b;
  margin-top: 5px;
}

.btn-cancel, .btn-save, .btn-danger {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-cancel {
  background: #95a5a6;
  color: white;
}

.btn-cancel:hover {
  background: #7f8c8d;
}

.btn-save {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
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
  font-size: 14px;
  z-index: 9999;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}

@media (max-width: 768px) {
  .admin-products { padding: 15px; }
  .page-header { flex-direction: column; align-items: stretch; gap: 12px; }
  .btn-add { justify-content: center; }
  .stats-row { grid-template-columns: 1fr; gap: 12px; }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .search-box { min-width: auto; }
  .btn-search, .btn-reset { justify-content: center; }
  .table-container { overflow-x: auto; }
  .data-table { min-width: 700px; }
  .pagination { flex-direction: column; gap: 12px; }
  .modal-container { width: 95%; }
  
  .variant-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
  
  .variant-price-stock {
    justify-content: space-between;
    width: 100%;
  }
}
</style>