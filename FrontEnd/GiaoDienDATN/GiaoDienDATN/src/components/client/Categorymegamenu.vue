<template>
  <!-- QUAN TRỌNG: Component này KHÔNG có 1 thẻ bọc ngoài (root là nhiều <li>
       liền nhau - Vue 3 hỗ trợ "fragment"). Nhờ vậy khi đặt
       <CategoryMegaMenu /> ngay TRONG thẻ <ul> của ClientHeader.vue, các
       <li> danh mục lớn (Áo/Quần/Phụ kiện) sẽ nằm CÙNG HÀNG với
       Trang chủ/Sản phẩm/Khuyến mãi/Về chúng tôi thay vì xuống dòng riêng. -->
  <li
    v-for="parent in mainTree"
    :key="parent.categoryId"
    class="mega-menu-item"
    @mouseenter="openId = parent.categoryId"
    @mouseleave="openId = null"
  >
    <router-link :to="`/shop?category=${parent.categoryId}`" class="mega-menu-link">
      <i :class="iconFor(parent.categoryName)"></i>
      {{ parent.categoryName }}
      <i v-if="parent.children && parent.children.length" class="fas fa-chevron-down mega-caret"></i>
    </router-link>

    <Transition name="dropdown">
      <div
        v-if="parent.children && parent.children.length && openId === parent.categoryId"
        class="dropdown-panel"
      >
        <router-link
          v-for="child in parent.children"
          :key="child.categoryId"
          :to="`/shop?category=${parent.categoryId}&subcategory=${child.categoryId}`"
          class="dropdown-link"
        >
          {{ child.categoryName }}
        </router-link>
      </div>
    </Transition>
  </li>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { storeToRefs } from "pinia";
import { useCategoryHierarchyStore } from "@/stores/categoryHierarchyStores";

const categoryStore = useCategoryHierarchyStore();
const { activeTree: tree } = storeToRefs(categoryStore);
const openId = ref(null);

// Chỉ hiển thị đúng 3 danh mục lớn mặc định (Áo/Quần/Phụ kiện) trên menu.
// Danh mục cấp 1 khác (tạo ở "Quản lý danh mục" nhưng CHƯA gán vào 1 trong 3
// danh mục lớn ở trang "Phân cấp danh mục") sẽ KHÔNG hiện ở đây.
// Danh mục con (nếu có) vẫn hiện trong dropdown khi hover vào danh mục lớn tương ứng.
const mainTree = computed(() =>
  tree.value.filter((parent) => categoryStore.isLockedCategory(parent))
);

// Icon gợi ý theo tên danh mục lớn mặc định - có fallback icon chung
const ICON_MAP = {
  "Áo": "fas fa-tshirt",
  "Quần": "fas fa-socks",
  "Phụ kiện": "fas fa-glasses" };
function iconFor(name) {
  return ICON_MAP[name] || "fas fa-tag";
}

onMounted(async () => {
  try {
    await categoryStore.loadActiveTree();
  } catch (error) {
    console.error("Lỗi tải danh mục cho menu:", error);
  }
});
</script>

<style scoped>
.mega-menu-item {
  position: relative;
}
.mega-menu-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #555;
  font-weight: 500;
  transition: all 0.3s ease;
  padding: 10px 20px;
  border-radius: 25px;
  font-size: 14px;
}
.mega-menu-link:hover {
  color: #e63946;
  background: rgba(230, 57, 70, 0.08);
}
.mega-caret {
  font-size: 10px;
  margin-left: 2px;
}
.dropdown-panel {
  position: absolute;
  top: 100%;
  left: 0;
  background: white;
  min-width: 200px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  padding: 8px 0;
  z-index: 1001;
}
.dropdown-link {
  display: block;
  padding: 10px 18px;
  color: #34495e;
  text-decoration: none;
  font-size: 14px;
}
.dropdown-link:hover {
  background: #f5f5f5;
  color: #e63946;
}
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>