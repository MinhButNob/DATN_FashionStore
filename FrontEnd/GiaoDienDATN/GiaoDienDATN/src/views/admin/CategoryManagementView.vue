<!-- ĐẶT FILE NÀY TẠI: src/views/admin/CategoryManagementView.vue -->
<!-- Gộp 2 trang có sẵn (AdminCategories.vue + Admincategoryhierarchy.vue) thành -->
<!-- 2 tab loại trừ nhau - KHÔNG sửa gì bên trong 2 file đó, chỉ bọc lại. -->
<template>
  <div class="category-management-page">
    <div class="cm-tabs-bar">
      <button
        :class="['cm-tab-btn', activeTab === 'list' ? 'cm-tab-btn--active' : '']"
        @click="activeTab = 'list'"
      >
        <i class="fas fa-tags"></i> Quản lý danh mục
      </button>
      <button
        :class="['cm-tab-btn', activeTab === 'hierarchy' ? 'cm-tab-btn--active' : '']"
        @click="activeTab = 'hierarchy'"
      >
        <i class="fas fa-sitemap"></i> Phân cấp danh mục
      </button>
    </div>

    <!-- v-if/v-else: tại 1 thời điểm chỉ 1 trang được mount, không hiện song song -->
    <AdminCategories v-if="activeTab === 'list'" />
    <AdminCategoryHierarchy v-else />
  </div>
</template>

<script>
// Import đúng 2 file gốc đã có sẵn, cùng thư mục views/admin - không đổi tên,
// không sửa nội dung bên trong 2 file này.
import AdminCategories from './AdminCategories.vue'
import AdminCategoryHierarchy from './Admincategoryhierarchy.vue'

export default {
  name: 'CategoryManagementView',
  components: { AdminCategories, AdminCategoryHierarchy },
  data() {
    return {
      activeTab: 'list' // 'list' = Quản lý danh mục | 'hierarchy' = Phân cấp danh mục
    }
  }
}
</script>

<style scoped>
.cm-tabs-bar {
  display: flex;
  gap: 4px;
  background: white;
  border-radius: 12px;
  padding: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin: 20px 20px 0;
  width: fit-content;
}

.cm-tab-btn {
  flex: none;
  padding: 12px 22px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #7f8c8d;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.cm-tab-btn:hover {
  background: #f5f6fa;
  color: #2c3e50;
}

.cm-tab-btn--active {
  background: #2c3e50;
  color: #fff;
}
</style>