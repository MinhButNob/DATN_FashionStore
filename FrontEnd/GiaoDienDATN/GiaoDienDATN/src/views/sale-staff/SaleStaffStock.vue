<!-- views/sale-staff/SaleStaffStock.vue -->
<template>
  <div class="sale-staff-stock">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>Quản lý tồn kho</h2>
        <p class="subtitle">Cập nhật số lượng sản phẩm trong kho</p>
      </div>
      <button class="btn-refresh" @click="refreshData">
        <span class="btn-icon">⟳</span> Làm mới
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
            <th>Biến thể & Tồn kho</th>
            <th>Giá</th>
            <th>Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in paginatedProducts" :key="product.productId" class="table-row" :class="{ 'row-deleted': product._deletedAt }">
            <td class="id-cell">#{{ product.productId }}</td>
            <td class="product-name" :class="{ 'deleted-text': product._deletedAt }">{{ product.productName }}</td>
            <td class="category-cell">{{ product.category?.categoryName || '-' }}</td>
            
            <td class="variants-cell">
              <div class="variants-list">
                <div 
                  v-for="(v, idx) in product.variants" 
                  :key="idx" 
                  class="variant-item"
                  :class="{ 
                    'low-stock': v.quantity <= 5 && v.quantity > 0,
                    'out-of-stock': v.quantity === 0,
                    'variant-deleted': product._deletedAt
                  }"
                >
                  <div class="variant-info">
                    <span class="variant-color" :style="{ backgroundColor: getColorHex(v.color) }"></span>
                    <span class="variant-name">{{ v.color || '?' }} - {{ v.size || '?' }}</span>
                  </div>
                  <div class="variant-stock-control">
                    <!-- Nút giảm -->
                    <button 
                      class="qty-btn minus" 
                      @click="decreaseStock(product, v)"
                      :disabled="v.quantity <= 0 || updating || product._deletedAt"
                      :title="product._deletedAt ? 'Sản phẩm đã xóa' : 'Giảm 1'"
                    >
                      <span class="btn-icon">−</span>
                    </button>
                    
                    <!-- Input số lượng -->
                    <input 
                      type="number" 
                      v-model.number="v.quantity" 
                      min="0"
                      class="qty-input"
                      @change="updateStock(product, v)"
                      :disabled="updating || product._deletedAt"
                    />
                    
                    <!-- Nút tăng -->
                    <button 
                      class="qty-btn plus" 
                      @click="increaseStock(product, v)"
                      :disabled="updating || product._deletedAt"
                      :title="product._deletedAt ? 'Sản phẩm đã xóa' : 'Tăng 1'"
                    >
                      <span class="btn-icon">+</span>
                    </button>
                    
                    <!-- Trạng thái tồn kho -->
                    <span 
                      class="variant-status" 
                      :class="getStockStatusClass(v.quantity)"
                    >
                      {{ getStockStatusText(v.quantity) }}
                    </span>
                  </div>
                </div>
                <div v-if="!product.variants || product.variants.length === 0" class="no-variant">
                  Không có biến thể
                </div>
              </div>
            </td>
            
            <td class="price-cell">
              <div v-if="product.variants && product.variants.length > 0">
                <span class="min-price">{{ formatPrice(getMinPrice(product.variants)) }}</span>
                <span v-if="getMinPrice(product.variants) !== getMaxPrice(product.variants)" class="price-separator"> - </span>
                <span v-if="getMinPrice(product.variants) !== getMaxPrice(product.variants)" class="max-price">{{ formatPrice(getMaxPrice(product.variants)) }}</span>
              </div>
              <span v-else class="no-price">---</span>
            </td>
            
            <td class="status-cell">
              <span v-if="product._deletedAt" class="status-badge status-deleted">
                Đã xóa
              </span>
              <span v-else :class="['status-badge', product.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
                {{ product.status === 'ACTIVE' ? 'Đang bán' : 'Ngừng bán' }}
              </span>
            </td>
          </tr>
          <tr v-if="allProducts.length === 0 && !loading">
            <td colspan="6" class="empty-row">
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

    <!-- Toast -->
    <div :class="['toast', { 'toast-error': toastType === 'error', show: showToast }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

// State
const allProducts = ref([]);
const categoryList = ref([]);
const colorList = ref([]);
const statistics = ref({
  totalProducts: 0,
  inStockVariants: 0,
  lowStockVariants: 0
});
const loading = ref(false);
const updating = ref(false);
const currentPage = ref(0);
const pageSize = ref(5);
const totalPages = ref(0);

// Filters
const searchKeyword = ref('');
const selectedCategoryId = ref(null);
const selectedStatus = ref(null);

// Toast
const showToast = ref(false);
const toastMessage = ref('');
const toastType = ref('success');
let toastTimer = null;

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
  const colorMap = {
    'Đỏ': '#FF0000',
    'Đen': '#000000',
    'Trắng': '#FFFFFF',
    'Xanh': '#0000FF',
    'Xanh dương': '#0000FF',
    'Navy': '#1B2951',
    'Vàng': '#FFFF00',
    'Hồng': '#FFC0CB',
    'Xám': '#808080',
    'Nâu': '#8B4513',
    'Tím': '#800080',
    'Cam': '#FFA500',
    'Xanh lá': '#00FF00'
  };
  return colorMap[colorName] || '#CCCCCC';
};

const formatPrice = (price) => {
  if (!price) return '0đ';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const getMinPrice = (variants) => {
  if (!variants || variants.length === 0) return 0;
  return Math.min(...variants.map(v => v.price || 0));
};

const getMaxPrice = (variants) => {
  if (!variants || variants.length === 0) return 0;
  return Math.max(...variants.map(v => v.price || 0));
};

const getStockStatusClass = (quantity) => {
  if (quantity === 0) return 'out-of-stock';
  if (quantity <= 5) return 'low-stock';
  return 'in-stock';
};

const getStockStatusText = (quantity) => {
  if (quantity === 0) return 'Hết hàng';
  if (quantity <= 5) return 'Sắp hết';
  return 'Còn hàng';
};

// Computed
const filteredProducts = computed(() => {
  let filtered = [...allProducts.value];
  
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase();
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

// Fetch data
const fetchProducts = async () => {
  loading.value = true;
  try {
    const token = localStorage.getItem('token');
    const response = await axios.get(`${API_BASE}/products`, {
      params: { page: 0, size: 100 },
      headers: { Authorization: `Bearer ${token}` }
    });
    
    let products = [];
    if (response.data?.data?.content) {
      products = response.data.data.content;
    } else if (response.data?.content) {
      products = response.data.content;
    } else if (Array.isArray(response.data)) {
      products = response.data;
    }
    
    // Giữ tất cả sản phẩm (bao gồm cả đã xóa)
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
    const token = localStorage.getItem('token');
    const response = await axios.get(`${API_BASE}/products/statistics`, {
      headers: { Authorization: `Bearer ${token}` }
    });
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
    const token = localStorage.getItem('token');
    const response = await axios.get(`${API_BASE}/categories/active`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    
    let categories = [];
    if (response.data?.data) {
      categories = response.data.data;
    } else if (Array.isArray(response.data)) {
      categories = response.data;
    }
    categoryList.value = categories;
  } catch (error) {
    console.error('Lỗi tải danh mục:', error);
    categoryList.value = [];
  }
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

const refreshData = async () => {
  await fetchProducts();
  await fetchStatistics();
  showNotification('Đã làm mới dữ liệu', 'success');
};

// ===== STOCK MANAGEMENT =====
const updateStock = async (product, variant) => {
  if (product._deletedAt) {
    showNotification('Không thể cập nhật sản phẩm đã xóa!', 'error');
    return;
  }
  
  if (variant.quantity < 0) {
    variant.quantity = 0;
    showNotification('Số lượng không thể nhỏ hơn 0', 'error');
    return;
  }
  
  updating.value = true;
  try {
    const token = localStorage.getItem('token');
    const response = await axios.put(
      `${API_BASE}/sale-staff/products/stock/${variant.productDetailId}`,
      { quantity: variant.quantity },
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    const result = response.data?.data || response.data;
    variant.quantity = result.newQuantity || result.quantity || variant.quantity;
    
    // Update total stock
    product.totalStock = product.variants.reduce((sum, v) => sum + (v.quantity || 0), 0);
    
    showNotification(`Đã cập nhật số lượng cho ${variant.color} - ${variant.size}`, 'success');
  } catch (error) {
    console.error('Error updating stock:', error);
    showNotification(error.response?.data?.message || 'Cập nhật thất bại', 'error');
    await fetchProducts();
  } finally {
    updating.value = false;
  }
};

const increaseStock = async (product, variant) => {
  if (product._deletedAt) {
    showNotification('Không thể cập nhật sản phẩm đã xóa!', 'error');
    return;
  }
  
  updating.value = true;
  try {
    const token = localStorage.getItem('token');
    const response = await axios.post(
      `${API_BASE}/sale-staff/products/stock/${variant.productDetailId}/increase`,
      { quantity: 1 },
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    const result = response.data?.data || response.data;
    variant.quantity = result.newQuantity || result.quantity || (variant.quantity + 1);
    product.totalStock = product.variants.reduce((sum, v) => sum + (v.quantity || 0), 0);
    
    showNotification(`Đã tăng số lượng cho ${variant.color} - ${variant.size}`, 'success');
  } catch (error) {
    console.error('Error increasing stock:', error);
    showNotification(error.response?.data?.message || 'Tăng số lượng thất bại', 'error');
    await fetchProducts();
  } finally {
    updating.value = false;
  }
};

const decreaseStock = async (product, variant) => {
  if (product._deletedAt) {
    showNotification('Không thể cập nhật sản phẩm đã xóa!', 'error');
    return;
  }
  
  if (variant.quantity <= 0) {
    showNotification('Số lượng đã là 0, không thể giảm', 'warning');
    return;
  }
  
  updating.value = true;
  try {
    const token = localStorage.getItem('token');
    const response = await axios.post(
      `${API_BASE}/sale-staff/products/stock/${variant.productDetailId}/decrease`,
      { quantity: 1 },
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    const result = response.data?.data || response.data;
    variant.quantity = result.newQuantity || result.quantity || (variant.quantity - 1);
    product.totalStock = product.variants.reduce((sum, v) => sum + (v.quantity || 0), 0);
    
    showNotification(`Đã giảm số lượng cho ${variant.color} - ${variant.size}`, 'success');
  } catch (error) {
    console.error('Error decreasing stock:', error);
    showNotification(error.response?.data?.message || 'Giảm số lượng thất bại', 'error');
    await fetchProducts();
  } finally {
    updating.value = false;
  }
};

// Lifecycle
onMounted(async () => {
  await fetchProducts();
  await fetchStatistics();
  await fetchCategories();
});
</script>

<style scoped>
.sale-staff-stock {
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

.btn-refresh {
  padding: 10px 20px;
  background: #2c3e50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-refresh:hover {
  background: #1a252f;
  transform: translateY(-2px);
}

.btn-refresh .btn-icon {
  font-size: 18px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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
  font-size: 16px;
}

.filter-select {
  padding: 10px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  outline: none;
  background-color: white;
  cursor: pointer;
  min-width: 160px;
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

.btn-reset .btn-icon {
  font-size: 16px;
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
  min-width: 900px;
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
  max-width: 500px;
}

.variants-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.variant-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f8f9fa;
  border-left: 3px solid #28a745;
  flex-wrap: wrap;
}

.variant-item.low-stock {
  border-left-color: #f59e0b;
  background: #fffbeb;
}

.variant-item.out-of-stock {
  border-left-color: #ef4444;
  background: #fef2f2;
  opacity: 0.7;
}

.variant-item.variant-deleted {
  border-left-color: #9ca3af;
  background: #f3f4f6;
  opacity: 0.5;
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
  flex-shrink: 0;
}

.variant-name {
  font-weight: 500;
  font-size: 13px;
  color: #374151;
}

.variant-stock-control {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.qty-btn {
  width: 28px;
  height: 28px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

.qty-btn:hover:not(:disabled) {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.qty-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.qty-btn .btn-icon {
  line-height: 1;
}

.qty-input {
  width: 50px;
  height: 28px;
  text-align: center;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
}

.qty-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.qty-input:focus {
  outline: none;
  border-color: #3498db;
}

.variant-status {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.variant-status.in-stock {
  background: #d1fae5;
  color: #065f46;
}

.variant-status.low-stock {
  background: #fef3c7;
  color: #92400e;
}

.variant-status.out-of-stock {
  background: #fee2e2;
  color: #991b1b;
}

.no-variant {
  color: #9ca3af;
  font-style: italic;
  padding: 8px 0;
  text-align: center;
}

.price-cell {
  font-weight: 500;
  min-width: 120px;
}

.min-price {
  color: #e63946;
}

.max-price {
  color: #e63946;
}

.price-separator {
  color: #9ca3af;
}

.no-price {
  color: #9ca3af;
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
  .sale-staff-stock { padding: 15px; }
  .page-header { flex-direction: column; align-items: stretch; gap: 12px; }
  .btn-refresh { justify-content: center; }
  .stats-row { grid-template-columns: 1fr; gap: 12px; }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .search-box { min-width: auto; }
  .btn-search, .btn-reset { justify-content: center; }
  .table-container { overflow-x: auto; }
  .data-table { min-width: 700px; }
  .pagination { flex-direction: column; gap: 12px; }
  
  .variant-item {
    flex-direction: column;
    align-items: stretch;
    gap: 6px;
  }
  
  .variant-stock-control {
    justify-content: center;
  }
}
</style>