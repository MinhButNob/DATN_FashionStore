<template>
  <div class="filter-section">
    <div class="filter-options">
      <select v-model="category" @change="updateFilters">
        <option value="all">Tất cả danh mục</option>
        <option value="men">Thời trang nam</option>
        <option value="women">Thời trang nữ</option>
        <option value="kids">Trẻ em</option>
        <option value="accessories">Phụ kiện</option>
      </select>
      
      <select v-model="sort" @change="updateFilters">
        <option value="default">Mặc định</option>
        <option value="price-asc">Giá: Thấp đến cao</option>
        <option value="price-desc">Giá: Cao đến thấp</option>
        <option value="name-asc">Tên: A-Z</option>
        <option value="name-desc">Tên: Z-A</option>
        <option value="rating">Đánh giá cao nhất</option>
        <option value="sold">Bán chạy nhất</option>
      </select>
      
      <button @click="toggleView">
        <i :class="gridView ? 'fas fa-th' : 'fas fa-list'"></i>
        {{ gridView ? 'Grid View' : 'List View' }}
      </button>
    </div>
    
    <div class="product-count">
      Hiển thị {{ productCount }} sản phẩm
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  productCount: {
    type: Number,
    default: 0
  },
  gridView: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['filter-change', 'toggle-view'])

const category = ref('all')
const sort = ref('default')
const gridViewLocal = ref(props.gridView)

// Watch for changes and emit
watch([category, sort], () => {
  emit('filter-change', {
    category: category.value,
    sort: sort.value
  })
})

const toggleView = () => {
  gridViewLocal.value = !gridViewLocal.value
  emit('toggle-view', gridViewLocal.value)
}

const updateFilters = () => {
  // Filters are automatically watched
}
</script>

<style scoped>
.filter-section {
  padding: 20px 25px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  border-radius: 10px;
  box-shadow: var(--shadow);
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.filter-options {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.filter-options select,
.filter-options button {
  padding: 10px 20px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  cursor: pointer;
  background: white;
  transition: all 0.3s;
  font-size: 14px;
  color: var(--dark-gray);
}

.filter-options select:hover,
.filter-options button:hover {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.filter-options select:focus,
.filter-options button:focus {
  border-color: var(--primary-red);
}

.filter-options button {
  background: var(--light-gray);
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.filter-options button i {
  color: var(--primary-red);
}

.product-count {
  color: var(--medium-gray);
  font-size: 15px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
    align-items: stretch;
    padding: 15px;
  }

  .filter-options {
    justify-content: center;
  }

  .filter-options select,
  .filter-options button {
    flex: 1;
    min-width: 150px;
  }
}

@media (max-width: 480px) {
  .filter-options {
    flex-direction: column;
  }

  .filter-options select,
  .filter-options button {
    width: 100%;
  }
}
</style>