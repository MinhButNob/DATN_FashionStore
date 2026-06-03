<template>
  <div class="admin-products">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-title">
        <h2>📦 Quản lý sản phẩm</h2>
        <p class="subtitle">Quản lý danh sách sản phẩm, biến thể màu sắc và kích thước</p>
      </div>
      <button class="btn-primary" @click="openCreateModal">
        <i class="fas fa-plus-circle"></i> Thêm sản phẩm
      </button>
    </div>

    <!-- Stats -->
    <div class="stats-grid" v-if="statistics">
      <div class="stats-card">
        <div class="stats-icon bg-red">
          <i class="fas fa-box"></i>
        </div>
        <div class="stats-info">
          <span class="stats-value">{{ statistics.totalProducts || 0 }}</span>
          <span class="stats-label">Tổng sản phẩm</span>
        </div>
      </div>
      <div class="stats-card">
        <div class="stats-icon bg-green">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="stats-info">
          <span class="stats-value">{{ statistics.inStockVariants || 0 }}</span>
          <span class="stats-label">Biến thể còn hàng</span>
        </div>
      </div>
      <div class="stats-card">
        <div class="stats-icon bg-yellow">
          <i class="fas fa-exclamation-triangle"></i>
        </div>
        <div class="stats-info">
          <span class="stats-value">{{ statistics.lowStockVariants || 0 }}</span>
          <span class="stats-label">Sắp hết hàng</span>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters-section">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input 
          type="text" 
          v-model="filters.keyword" 
          @keyup.enter="applyFilters" 
          placeholder="Tìm kiếm sản phẩm..." 
        />
      </div>

      <div class="filter-controls">
        <select v-model="filters.categoryId" @change="applyFilters">
          <option :value="null">Tất cả danh mục</option>
          <option v-for="cat in categoryList" :key="cat.categoryId" :value="cat.categoryId">
            {{ cat.categoryName }}
          </option>
        </select>

        <select v-model="filters.isActive" @change="applyFilters">
          <option :value="null">Tất cả trạng thái</option>
          <option :value="true">Đang bán</option>
          <option :value="false">Ngừng bán</option>
        </select>

        <button class="btn-reset" @click="resetFilters">
          <i class="fas fa-undo-alt"></i> Reset
        </button>
      </div>
    </div>

    <!-- Products Table -->
    <div class="table-container">
      <table class="product-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên sản phẩm</th>
            <th>Danh mục</th>
            <th>Biến thể</th>
            <th>Tồn kho</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody v-if="!loading">
          <tr v-for="product in products" :key="product.productId">
            <td class="id-cell">#{{ product.productId }}</td>
            <td class="product-name">{{ product.productName }}</td>
            <td>{{ product.category?.categoryName || '-' }}</td>
            <td class="variants-cell">
              <div class="variants-list">
                <div v-for="(v, idx) in product.variants" :key="idx" class="variant-item">
                  <span class="variant-color" :style="{ backgroundColor: getColorHex(v.color) }"></span>
                  <span>{{ v.color || '?' }} - {{ v.size || '?' }}</span>
                  <span class="variant-price">{{ formatPrice(v.price) }}</span>
                  <span class="variant-stock">({{ v.quantity }})</span>
                </div>
              </div>
            </td>
            <td class="stock-cell">{{ product.totalStock || 0 }}</td>
            <td>
              <span :class="['status-badge', product.isActive ? 'active' : 'inactive']">
                {{ product.isActive ? 'Đang bán' : 'Ngừng bán' }}
              </span>
            </td>
            <td class="actions">
              <button class="action-btn edit" @click="openEditModal(product)" title="Sửa">
                <i class="fas fa-edit"></i>
              </button>
              <button class="action-btn toggle" @click="toggleStatus(product)" :title="product.isActive ? 'Ngừng bán' : 'Bật bán'">
                <i :class="product.isActive ? 'fas fa-ban' : 'fas fa-play'"></i>
              </button>
              <button class="action-btn delete" @click="confirmDelete(product)" title="Xóa">
                <i class="fas fa-trash"></i>
              </button>
            </td>
          </tr>
          <tr v-if="products.length === 0">
            <td colspan="7" class="empty-row">
              <i class="fas fa-inbox"></i> Không có sản phẩm nào
            </td>
          </tr>
        </tbody>
      </table>
      
      <div v-if="loading" class="loading-overlay">
        <i class="fas fa-spinner fa-spin"></i> Đang tải...
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 1">
      <button @click="goToPage(0)" :disabled="currentPage === 0">
        <i class="fas fa-angle-double-left"></i>
      </button>
      <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 0">
        <i class="fas fa-angle-left"></i>
      </button>
      <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button @click="goToPage(currentPage + 1)" :disabled="currentPage + 1 >= totalPages">
        <i class="fas fa-angle-right"></i>
      </button>
      <button @click="goToPage(totalPages - 1)" :disabled="currentPage + 1 >= totalPages">
        <i class="fas fa-angle-double-right"></i>
      </button>
    </div>

    <!-- Product Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-container modal-lg">
        <div class="modal-header">
          <h3>{{ isEdit ? '✏️ Sửa sản phẩm' : '➕ Thêm sản phẩm' }}</h3>
          <button class="modal-close" @click="closeModal">×</button>
        </div>

        <form @submit.prevent="handleSubmit">
          <div class="modal-body">
            <!-- Thông tin cơ bản -->
            <div class="form-section">
              <h4>📝 Thông tin cơ bản</h4>
              
              <div class="form-group">
                <label>Tên sản phẩm <span class="required">*</span></label>
                <input v-model="form.productName" type="text" placeholder="Nhập tên sản phẩm" required />
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>Danh mục <span class="required">*</span></label>
                  <select v-model="form.categoryId" required>
                    <option :value="null">Chọn danh mục</option>
                    <option v-for="cat in categoryList" :key="cat.categoryId" :value="cat.categoryId">
                      {{ cat.categoryName }}
                    </option>
                  </select>
                </div>

                <div class="form-group">
                  <label>Thương hiệu</label>
                  <input v-model="form.brand" type="text" placeholder="Nhập thương hiệu" />
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>Giới tính</label>
                  <select v-model="form.gender">
                    <option value="UNISEX">👥 Unisex</option>
                    <option value="MEN">👨 Nam</option>
                    <option value="WOMEN">👩 Nữ</option>
                  </select>
                </div>

                <div class="form-group">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="form.isActive" />
                    <span>✅ Đang bán</span>
                  </label>
                </div>
              </div>

              <div class="form-group">
                <label>Mô tả sản phẩm</label>
                <textarea v-model="form.description" rows="4" placeholder="Nhập mô tả sản phẩm..."></textarea>
              </div>
            </div>

            <!-- Biến thể -->
            <div class="form-section">
              <div class="variants-header">
                <h4>🎨 Biến thể (Màu sắc - Kích thước)</h4>
                <button type="button" class="btn-add-variant" @click="addVariant">
                  <i class="fas fa-plus"></i> Thêm biến thể
                </button>
              </div>

              <div class="variants-table-wrapper">
                <table class="variants-table">
                  <thead>
                    <tr>
                      <th>Màu sắc</th>
                      <th>Kích thước</th>
                      <th>Giá (VNĐ)</th>
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
                        <input type="number" v-model="variant.price" placeholder="Giá" required />
                      </td>
                      <td>
                        <input type="number" v-model="variant.quantity" placeholder="SL" required />
                      </td>
                      <td>
                        <input type="text" v-model="variant.sku" placeholder="SKU" />
                      </td>
                      <td>
                        <button type="button" class="btn-remove-variant" @click="removeVariant(index)">
                          <i class="fas fa-trash-alt"></i>
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
              <i class="fas fa-times"></i> Hủy
            </button>
            <button type="submit" class="btn-save" :disabled="saving">
              <i v-if="saving" class="fas fa-spinner fa-spin"></i>
              <i v-else class="fas fa-save"></i>
              {{ saving ? 'Đang lưu...' : 'Lưu sản phẩm' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useProductStore } from '@/stores/productStore';
import axios from 'axios';

const productStore = useProductStore();

// State
const showModal = ref(false);
const isEdit = ref(false);
const saving = ref(false);
const form = ref({
  productName: '',
  categoryId: null,
  description: '',
  brand: '',
  gender: 'UNISEX',
  isActive: true,
  variants: []
});

// Dữ liệu từ API
const categoryList = ref([]);
const colorList = ref([]);
const sizeList = ref([]);

// Dữ liệu từ store
const products = computed(() => productStore.products);
const statistics = computed(() => productStore.statistics);
const loading = computed(() => productStore.loading);
const currentPage = computed(() => productStore.currentPage);
const totalPages = computed(() => productStore.totalPages);
const filters = productStore.filters;

const API_BASE = 'http://localhost:8080/api/v1';

// Helper functions
const getColorHex = (colorName) => {
  const color = colorList.value.find(c => c.colorName === colorName);
  return color?.hexCode || '#ccc';
};

const formatPrice = (price) => {
  if (!price) return '0đ';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

// ✅ Load categories - Gọi trực tiếp API và log chi tiết
// const loadCategories = async () => {
//   try {
//     console.log('Đang gọi API categories...');
//     const response = await axios.get(`${API_BASE}/categories`);
//     console.log('Response categories:', response);
    
//     // Lấy data từ response
//     let result = response.data;
//     console.log('result.data:', result);
    
//     // Thử các cách lấy dữ liệu
//     let categories = [];
    
//     if (result && Array.isArray(result)) {
//       categories = result;
//       console.log('Cách 1: result là mảng');
//     }
//     else if (result && result.data && Array.isArray(result.data)) {
//       categories = result.data;
//       console.log('Cách 2: result.data là mảng');
//     }
//     else if (result && result.content && Array.isArray(result.content)) {
//       categories = result.content;
//       console.log('Cách 3: result.content là mảng');
//     }
//     else if (result && result.data && result.data.content && Array.isArray(result.data.content)) {
//       categories = result.data.content;
//       console.log('Cách 4: result.data.content là mảng');
//     }
//     else if (result && result._embedded && result._embedded.categories) {
//       categories = result._embedded.categories;
//       console.log('Cách 5: result._embedded.categories là mảng');
//     }
    
//     console.log('Categories raw sau khi trích xuất:', categories);
    
//     // Lọc category ACTIVE
//     categoryList.value = categories.filter(c => c.status === 'ACTIVE');
//     console.log('✅ Categories cuối cùng:', categoryList.value);
//     console.log('✅ Số lượng categories:', categoryList.value.length);
    
//   } catch (error) {
//     console.error('❌ Lỗi load categories:', error);
//     categoryList.value = [];
//   }
// };


const loadCategories = async () => {
  try {
    const response = await categoryService.getAll();
    console.log('Categories response:', response);
    
    let allData = [];
    
    if (response.data?.data?.content) {
      allData = response.data.data.content;
    } else if (response.data?.content) {
      allData = response.data.content;
    } else if (Array.isArray(response.data)) {
      allData = response.data;
    } else if (response.data?.data && Array.isArray(response.data.data)) {
      allData = response.data.data;
    }
    
    // Nếu không có dữ liệu, dùng mock data
    if (allData.length === 0) {
      console.warn('⚠️ Không lấy được categories từ API, dùng mock data');
      allData = [
        { categoryId: 1, categoryName: "Áo", status: "ACTIVE" },
        { categoryId: 2, categoryName: "Quần", status: "ACTIVE" },
        { categoryId: 3, categoryName: "Giày", status: "ACTIVE" },
        { categoryId: 4, categoryName: "Phụ kiện", status: "ACTIVE" },
        { categoryId: 5, categoryName: "Váy", status: "INACTIVE" },
        { categoryId: 6, categoryName: "Đồ thể thao", status: "ACTIVE" }
      ];
    }
    
    // Lấy tất cả (không lọc status)
    categoryList.value = allData;
    console.log('✅ Categories loaded:', categoryList.value);
    
  } catch (error) {
    console.error('❌ Lỗi load categories:', error);
    // Mock data khi lỗi
    categoryList.value = [
      { categoryId: 1, categoryName: "Áo", status: "ACTIVE" },
      { categoryId: 2, categoryName: "Quần", status: "ACTIVE" },
      { categoryId: 3, categoryName: "Giày", status: "ACTIVE" },
      { categoryId: 4, categoryName: "Phụ kiện", status: "ACTIVE" }
    ];
  }
};

// Load colors (đang hoạt động tốt)
const loadColors = async () => {
  try {
    const response = await axios.get(`${API_BASE}/colors`);
    let result = response.data;
    let colors = [];
    
    if (result && Array.isArray(result)) colors = result;
    else if (result && result.content) colors = result.content;
    else if (result && result.data) colors = result.data;
    
    colorList.value = colors.filter(c => c.status === 'ACTIVE' || c.status === true);
    console.log('✅ Colors loaded:', colorList.value.length);
  } catch (error) {
    console.error('❌ Lỗi load colors:', error);
    colorList.value = [];
  }
};

// Load sizes
const loadSizes = async () => {
  try {
    const response = await axios.get(`${API_BASE}/sizes`);
    let result = response.data;
    let sizes = [];
    
    if (result && Array.isArray(result)) sizes = result;
    else if (result && result.content) sizes = result.content;
    else if (result && result.data) sizes = result.data;
    
    sizeList.value = sizes.filter(s => s.status === 'ACTIVE' || s.status === true);
    console.log('✅ Sizes loaded:', sizeList.value.length);
  } catch (error) {
    console.error('❌ Lỗi load sizes:', error);
    sizeList.value = [];
  }
};

// Product actions
const applyFilters = () => {
  productStore.fetchProducts();
};

const resetFilters = () => {
  productStore.resetFilters();
};

const goToPage = (page) => {
  productStore.goToPage(page);
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
    productName: '',
    categoryId: null,
    description: '',
    brand: '',
    gender: 'UNISEX',
    isActive: true,
    variants: []
  };
  showModal.value = true;
};

const openEditModal = async (product) => {
  // Đảm bảo dữ liệu đã được load
  if (categoryList.value.length === 0) {
    await loadCategories();
  }
  if (colorList.value.length === 0) {
    await loadColors();
  }
  if (sizeList.value.length === 0) {
    await loadSizes();
  }
  
  isEdit.value = true;
  
  // Tìm categoryId từ categoryName
  const foundCategory = categoryList.value.find(c => 
    c.categoryName === product.category?.categoryName || 
    c.categoryId === product.categoryId
  );
  
  // Map variants với colorId và sizeId
  const variants = product.variants?.map(v => {
    const foundColor = colorList.value.find(c => 
      c.colorName === v.color || c.colorName === v.colorName
    );
    const foundSize = sizeList.value.find(s => 
      s.sizeName === v.size || s.sizeName === v.sizeName
    );
    
    return {
      variantId: v.variantId,
      colorId: foundColor?.colorId || v.colorId || null,
      sizeId: foundSize?.sizeId || v.sizeId || null,
      price: v.price,
      quantity: v.quantity,
      sku: v.sku || ''
    };
  }) || [];
  
  form.value = {
    productId: product.productId,
    productName: product.productName,
    categoryId: foundCategory?.categoryId || product.categoryId || null,  // ✅ Set đúng categoryId
    description: product.description || '',
    brand: product.brand || '',
    gender: product.gender || 'UNISEX',
    isActive: product.isActive,
    variants: variants
  };
  
  console.log('Edit form:', {
    categoryId: form.value.categoryId,
    variants: form.value.variants
  });
  
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  isEdit.value = false;
  form.value = {
    productName: '',
    categoryId: null,
    description: '',
    brand: '',
    gender: 'UNISEX',
    isActive: true,
    variants: []
  };
};

// Submit form
const handleSubmit = async () => {
  saving.value = true;
  
  try {
    const submitData = {
      productName: form.value.productName,
      categoryId: form.value.categoryId,
      description: form.value.description,
      brand: form.value.brand,
      gender: form.value.gender,
      isActive: form.value.isActive,
      variants: form.value.variants.map((v, index) => ({
        colorId: v.colorId,
        sizeId: v.sizeId,
        price: v.price,
        quantity: v.quantity,
        sku: v.sku || `SKU-${Date.now()}-${index}`  // ✅ Tạo SKU nếu trống
      }))
    };
    
    if (isEdit.value) {
      await productStore.updateProduct(form.value.productId, submitData);
      alert('Cập nhật sản phẩm thành công!');
    } else {
      await productStore.createProduct(submitData);
      alert('Thêm sản phẩm thành công!');
    }
    
    closeModal();
    await productStore.fetchProducts();
    await productStore.fetchStatistics();
  } catch (error) {
    console.error('Lỗi lưu sản phẩm:', error);
    alert(error.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại!');
  } finally {
    saving.value = false;
  }
};

const toggleStatus = async (product) => {
  await productStore.toggleProductStatus(product.productId);
  await productStore.fetchProducts();
};

const confirmDelete = (product) => {
  if (confirm(`Xóa sản phẩm "${product.productName}"?`)) {
    deleteProduct(product.productId);
  }
};

const deleteProduct = async (id) => {
  await productStore.deleteProduct(id);
  await productStore.fetchProducts();
  await productStore.fetchStatistics();
};

const generateSku = (productName, colorName, sizeName, index) => {
  // Lấy 3 chữ cái đầu của tên sản phẩm
  const productCode = productName?.substring(0, 3).toUpperCase() || 'PRD';
  // Lấy 2 chữ cái đầu của màu
  const colorCode = colorName?.substring(0, 2).toUpperCase() || 'CL';
  // Lấy size
  const sizeCode = sizeName?.toUpperCase() || 'SZ';
  // Thêm số ngẫu nhiên
  const random = Math.floor(Math.random() * 1000);
  
  return `${productCode}-${colorCode}-${sizeCode}-${random}`;
};



// Lifecycle
onMounted(async () => {
  await productStore.fetchProducts();
  await productStore.fetchStatistics();
  await loadCategories();
  await loadColors();
  await loadSizes();
});
</script>

<style scoped>
.admin-products {
  padding: 20px;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Page Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.header-title h2 {
  font-size: 28px;
  color: #2c3e50;
  font-weight: 600;
  margin-bottom: 5px;
}

.subtitle {
  color: #7f8c8d;
  font-size: 14px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 25px;
}

.stats-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 18px;
  transition: transform 0.2s;
}

.stats-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stats-icon {
  width: 54px;
  height: 54px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.bg-red {
  background: linear-gradient(135deg, #f093fb, #f5576c);
}

.bg-green {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
}

.bg-yellow {
  background: linear-gradient(135deg, #fa709a, #fee140);
}

.stats-info {
  display: flex;
  flex-direction: column;
}

.stats-value {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
}

.stats-label {
  font-size: 13px;
  color: #7f8c8d;
}

/* Filters */
.filters-section {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 0 15px;
  transition: all 0.3s;
}

.search-box:focus-within {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.search-box i {
  color: #999;
  margin-right: 10px;
}

.search-box input {
  flex: 1;
  border: none;
  padding: 12px 0;
  outline: none;
  font-size: 14px;
}

.filter-controls {
  display: flex;
  gap: 12px;
}

.filter-controls select {
  padding: 10px 15px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  outline: none;
}

.btn-reset {
  padding: 10px 20px;
  background: #f0f0f0;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-reset:hover {
  background: #e0e0e0;
}

/* Table Container */
.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
  position: relative;
}

.product-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}

.product-table th,
.product-table td {
  padding: 15px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.product-table th {
  background: #f8f9fa;
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.product-table tbody tr:hover {
  background: #f8f9fa;
}

.id-cell {
  font-weight: 600;
  color: #667eea;
}

.product-name {
  font-weight: 500;
  color: #2c3e50;
}

/* Variants */
.variants-cell {
  max-width: 300px;
}

.variants-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.variant-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  padding: 4px 0;
}

.variant-color {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid #ddd;
}

.variant-price {
  color: #e74c3c;
  font-weight: 500;
  margin-left: auto;
}

.variant-stock {
  color: #7f8c8d;
  font-size: 11px;
}

.stock-cell {
  font-weight: 600;
  color: #2c3e50;
}

/* Status Badge */
.status-badge {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background: #e8f5e9;
  color: #4caf50;
}

.status-badge.inactive {
  background: #ffebee;
  color: #f44336;
}

/* Actions */
.actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.action-btn.edit {
  background: #2196f3;
  color: white;
}

.action-btn.edit:hover {
  background: #1976d2;
  transform: scale(1.05);
}

.action-btn.toggle {
  background: #ff9800;
  color: white;
}

.action-btn.toggle:hover {
  background: #f57c00;
  transform: scale(1.05);
}

.action-btn.delete {
  background: #f44336;
  color: white;
}

.action-btn.delete:hover {
  background: #d32f2f;
  transform: scale(1.05);
}

/* Loading */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  color: #667eea;
}

.empty-row {
  text-align: center;
  padding: 50px;
  color: #999;
}

.empty-row i {
  font-size: 48px;
  margin-bottom: 10px;
  display: block;
}

/* Pagination */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 25px;
}

.pagination button {
  width: 38px;
  height: 38px;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination button:hover:not(:disabled) {
  background: #667eea;
  border-color: #667eea;
  color: white;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  padding: 0 15px;
  font-weight: 500;
  color: #2c3e50;
}

/* Modal */
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
  width: 900px;
}

@keyframes slideIn {
  from {
    transform: translateY(-30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 12px 12px 0 0;
}

.modal-header h3 {
  font-size: 20px;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: white;
  transition: transform 0.2s;
}

.modal-close:hover {
  transform: scale(1.1);
}

.modal-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Form Styles */
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

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
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

/* Variants Table */
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
}

.variants-table input,
.variants-table select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
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

/* Modal Footer Buttons */
.btn-cancel {
  background: #95a5a6;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-cancel:hover {
  background: #7f8c8d;
}

.btn-save {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Responsive */
@media (max-width: 768px) {
  .admin-products {
    padding: 15px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .filters-section {
    flex-direction: column;
  }
  
  .filter-controls {
    flex-wrap: wrap;
  }
  
  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
  
  .modal-container {
    width: 95%;
  }
}
</style>