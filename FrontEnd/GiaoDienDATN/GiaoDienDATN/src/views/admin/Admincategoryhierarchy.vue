<template>
  <div class="admin-category-hierarchy">
    <div class="page-header">
      <div class="header-left">
        <h2>Phân cấp danh mục</h2>
        <p class="subtitle">
          3 danh mục lớn mặc định (Áo, Quần, Phụ kiện) là cố định, không thể sửa/xóa.
          Bạn chỉ có thể thêm/sửa/xóa danh mục con bên trong từng danh mục lớn.
        </p>
      </div>
      <!-- ĐÃ BỎ nút "Thêm danh mục cha" vì chỉ cho phép đúng 3 danh mục lớn mặc định -->
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>Đang tải dữ liệu...</span>
    </div>

    <div v-else class="tree-container">
      <div v-for="parent in mainParents" :key="parent.categoryId" class="parent-block">
        <div class="parent-row">
          <button class="toggle-btn" @click="toggleExpand(parent.categoryId)">
            {{ isExpanded(parent.categoryId) ? '▾' : '▸' }}
          </button>
          <strong class="parent-name">{{ parent.categoryName }}</strong>
          <span v-if="isLocked(parent)" class="locked-badge" title="Danh mục lớn mặc định, không thể sửa/xóa">
            🔒 Mặc định
          </span>
          <span :class="['status-badge', parent.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
            {{ parent.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng' }}
          </span>
          <div class="row-actions">
            <button class="action-btn" title="Thêm danh mục con (chọn từ Quản lý danh mục)" @click="openPickModal(parent)">➕ Con</button>
            <!-- Chỉ hiện Sửa/Xóa/Gán khi KHÔNG phải danh mục lớn mặc định -->
            <template v-if="!isLocked(parent)">
              <button class="action-btn action-assign" title="Gán vào 1 trong 3 danh mục lớn" @click="openAssignModal(parent)">🔗 Gán vào</button>
              <button class="action-btn" title="Sửa" @click="openEditModal(parent)">✏️</button>
              <button class="action-btn" title="Xóa" @click="confirmDelete(parent)">🗑️</button>
            </template>
          </div>
        </div>

        <div v-if="isExpanded(parent.categoryId)" class="children-list">
          <div v-if="!parent.children || parent.children.length === 0" class="empty-children">
            Chưa có danh mục con
          </div>
          <div v-for="child in parent.children" :key="child.categoryId" class="child-row">
            <span class="child-name">— {{ child.categoryName }}</span>
            <span :class="['status-badge', child.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
              {{ child.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng' }}
            </span>
            <div class="row-actions">
              <button class="action-btn action-detach" title="Gỡ khỏi danh mục lớn (đưa về danh mục độc lập)" @click="confirmDetach(child)">↩️ Gỡ khỏi danh mục lớn</button>
              <button class="action-btn" title="Sửa" @click="openEditModal(child)">✏️</button>
              <button class="action-btn" title="Xóa" @click="confirmDelete(child)">🗑️</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="mainParents.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>Chưa có danh mục nào</p>
      </div>
    </div>

    <!-- Modal Thêm/Sửa -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-container">
        <div class="modal-header">
          <h3>{{ isEdit ? 'Sửa danh mục' : 'Thêm danh mục con' }}</h3>
          <button class="close-btn" @click="closeModal">✕</button>
        </div>
        <form @submit.prevent="saveCategory">
          <div class="modal-body">
            <div class="form-group">
              <label>Tên danh mục <span class="required">*</span></label>
              <input v-model="form.categoryName" required placeholder="Nhập tên danh mục" maxlength="20" />
            </div>
            <div class="form-group">
              <label>Mô tả</label>
              <textarea v-model="form.categoryDescription" rows="3" placeholder="Nhập mô tả" maxlength="20"></textarea>
            </div>
            <div class="form-group">
              <label>Thuộc danh mục lớn</label>
              <select v-model="form.parentCategoryId" :disabled="isEdit && hasChildren(form.categoryId)">
                <option :value="null">-- Không thuộc danh mục lớn nào --</option>
                <option v-for="p in parentOptions" :key="p.categoryId" :value="p.categoryId">
                  {{ p.categoryName }}
                </option>
              </select>
              <small v-if="isEdit && hasChildren(form.categoryId)" class="hint-text">
                Danh mục này đang có danh mục con nên không thể trở thành danh mục con của danh mục khác.
              </small>
            </div>
            <div class="form-group">
              <label>Trạng thái</label>
              <select v-model="form.status">
                <option value="ACTIVE">Hoạt động</option>
                <option value="INACTIVE">Tạm dừng</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="closeModal">Hủy</button>
            <button type="submit" class="btn-save" :disabled="saving">
              {{ saving ? 'Đang xử lý...' : (isEdit ? 'Cập nhật' : 'Thêm mới') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal Thêm danh mục con (chọn từ danh sách đã tạo ở "Quản lý danh mục") -->
    <div v-if="showPickModal" class="modal-overlay" @click.self="closePickModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Thêm danh mục con vào "{{ pickParentTarget?.categoryName }}"</h3>
          <button class="close-btn" @click="closePickModal">✕</button>
        </div>
        <div class="modal-body">
          <p v-if="unassignedCategories.length === 0" class="hint-text">
            Không có danh mục nào đang chưa gán để thêm. Hãy tạo danh mục mới ở trang "Quản lý danh mục" trước.
          </p>
          <div v-else class="form-group">
            <label>Chọn danh mục (dữ liệu lấy từ Quản lý danh mục) <span class="required">*</span></label>
            <select v-model="pickCategoryId">
              <option :value="null">-- Chọn danh mục --</option>
              <option v-for="c in unassignedCategories" :key="c.categoryId" :value="c.categoryId">
                {{ c.categoryName }}
              </option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closePickModal">Hủy</button>
          <button class="btn-save" :disabled="!pickCategoryId || picking" @click="confirmPick">
            {{ picking ? 'Đang xử lý...' : 'Thêm vào' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Gán vào danh mục lớn -->
    <div v-if="showAssignModal" class="modal-overlay" @click.self="closeAssignModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Gán vào danh mục lớn</h3>
          <button class="close-btn" @click="closeAssignModal">✕</button>
        </div>
        <div class="modal-body">
          <p>
            Chọn 1 trong 3 danh mục lớn để đưa <strong>{{ assignTarget?.categoryName }}</strong>
            vào làm danh mục con:
          </p>
          <div class="form-group">
            <label>Danh mục lớn <span class="required">*</span></label>
            <select v-model="assignParentId">
              <option :value="null">-- Chọn danh mục lớn --</option>
              <option v-for="p in mainParents" :key="p.categoryId" :value="p.categoryId">
                {{ p.categoryName }}
              </option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeAssignModal">Hủy</button>
          <button class="btn-save" :disabled="!assignParentId || assigning" @click="confirmAssign">
            {{ assigning ? 'Đang xử lý...' : 'Gán vào' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal xác nhận Gỡ khỏi danh mục lớn -->
    <div v-if="showDetachModal" class="modal-overlay" @click.self="showDetachModal = false">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận gỡ</h3>
          <button class="close-btn" @click="showDetachModal = false">✕</button>
        </div>
        <div class="modal-body">
          <p>
            Gỡ <strong>{{ detachTarget?.categoryName }}</strong> ra khỏi danh mục lớn hiện tại?
            Danh mục này sẽ trở thành danh mục độc lập (không nằm trong 3 danh mục chính),
            dữ liệu không bị xóa.
          </p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showDetachModal = false">Hủy</button>
          <button class="btn-save" :disabled="detaching" @click="doDetach">
            {{ detaching ? 'Đang xử lý...' : 'Gỡ khỏi' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Modal -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="showDeleteModal = false">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận xóa</h3>
          <button class="close-btn" @click="showDeleteModal = false">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc muốn xóa danh mục <strong>{{ deleteTarget?.categoryName }}</strong>?</p>
          <p v-if="hasChildren(deleteTarget?.categoryId)" class="warning-text">
            Danh mục này đang có danh mục con. Vui lòng xóa/di chuyển danh mục con trước.
          </p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showDeleteModal = false">Hủy</button>
          <button class="btn-danger" :disabled="hasChildren(deleteTarget?.categoryId)" @click="doDelete">Xóa</button>
        </div>
      </div>
    </div>

    <div :class="['toast', { 'toast-error': toastType === 'error', show: showToast }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script>
import { useCategoryHierarchyStore } from "@/stores/categoryHierarchyStores";

export default {
  name: "AdminCategoryHierarchy",
  setup() {
    const categoryStore = useCategoryHierarchyStore();
    return { categoryStore };
  },
  data() {
    return {
      expandedIds: new Set(),
      showModal: false,
      isEdit: false,
      saving: false,
      form: {
        categoryId: null,
        categoryName: "",
        categoryDescription: "",
        status: "ACTIVE",
        parentCategoryId: null,
      },
      showDeleteModal: false,
      deleteTarget: null,
      // Thêm danh mục con bằng cách chọn danh mục có sẵn (lấy từ Quản lý danh mục)
      showPickModal: false,
      pickParentTarget: null,
      pickCategoryId: null,
      picking: false,
      // Gán vào danh mục lớn
      showAssignModal: false,
      assignTarget: null,
      assignParentId: null,
      assigning: false,
      // Gỡ khỏi danh mục lớn
      showDetachModal: false,
      detachTarget: null,
      detaching: false,
      showToast: false,
      toastMessage: "",
      toastType: "success",
      toastTimer: null,
    };
  },
  computed: {
    tree() {
      return this.categoryStore.tree;
    },
    loading() {
      return this.categoryStore.loading;
    },
    parentOptions() {
      // Chỉ 3 danh mục lớn mặc định (Áo/Quần/Phụ kiện) mới được chọn làm cha
      return this.mainParents.filter((p) => !this.isEdit || p.categoryId !== this.form.categoryId);
    },
    // Chỉ đúng 3 danh mục lớn mặc định (Áo/Quần/Phụ kiện) - dùng cho modal "Gán vào"
    mainParents() {
      return this.tree.filter((p) => this.isLocked(p));
    },
    // Danh mục cấp 1 nhưng KHÔNG phải 3 danh mục lớn mặc định = đã tạo ở "Quản lý danh mục"
    // và đang chưa thuộc danh mục lớn nào -> dùng cho modal "Thêm danh mục con"
    unassignedCategories() {
      return this.tree.filter((p) => !this.isLocked(p));
    },
  },
  mounted() {
    this.loadTree();
  },
  methods: {
    async loadTree() {
      try {
        await this.categoryStore.loadTree();
      } catch (error) {
        this.notify("Không tải được cây danh mục", "error");
      }
    },
    isExpanded(id) {
      return this.expandedIds.has(id);
    },
    toggleExpand(id) {
      if (this.expandedIds.has(id)) this.expandedIds.delete(id);
      else this.expandedIds.add(id);
    },
    hasChildren(categoryId) {
      return this.categoryStore.hasChildren(categoryId);
    },
    // Danh mục lớn mặc định (Áo/Quần/Phụ kiện) - khóa cứng, không sửa/xóa
    isLocked(item) {
      return this.categoryStore.isLockedCategory(item);
    },
    openCreateModal(parent) {
      // Chỉ dùng để thêm DANH MỤC CON vào 1 trong 3 danh mục lớn có sẵn.
      // Không còn tạo được danh mục lớn mới (đã bỏ nút ở giao diện).
      this.isEdit = false;
      this.form = {
        categoryId: null,
        categoryName: "",
        categoryDescription: "",
        status: "ACTIVE",
        parentCategoryId: parent ? parent.categoryId : null,
      };
      this.showModal = true;
    },
    openEditModal(item) {
      if (this.isLocked(item)) {
        this.notify("Danh mục lớn mặc định không thể chỉnh sửa", "error");
        return;
      }
      this.isEdit = true;
      this.form = {
        categoryId: item.categoryId,
        categoryName: item.categoryName,
        categoryDescription: item.categoryDescription,
        status: item.status,
        parentCategoryId: item.parentCategoryId ?? null,
      };
      this.showModal = true;
    },
    closeModal() {
      this.showModal = false;
    },
    async saveCategory() {
      this.saving = true;
      try {
        if (this.isEdit) {
          await this.categoryStore.updateCategory(this.form.categoryId, this.form);
          this.notify("Cập nhật danh mục thành công");
        } else {
          await this.categoryStore.createCategory(this.form);
          this.notify("Thêm danh mục thành công");
        }
        this.showModal = false;
      } catch (error) {
        const msg = error?.response?.data?.message || error?.message || "Có lỗi xảy ra, vui lòng thử lại";
        this.notify(msg, "error");
      } finally {
        this.saving = false;
      }
    },
    // ===== Thêm danh mục con bằng cách chọn 1 danh mục đã có sẵn (tạo ở "Quản lý danh mục") =====
    openPickModal(parent) {
      this.pickParentTarget = parent;
      this.pickCategoryId = null;
      this.showPickModal = true;
    },
    closePickModal() {
      this.showPickModal = false;
      this.pickParentTarget = null;
      this.pickCategoryId = null;
    },
    async confirmPick() {
      if (!this.pickCategoryId || !this.pickParentTarget) return;
      const selected = this.unassignedCategories.find((c) => c.categoryId === this.pickCategoryId);
      if (!selected) return;
      this.picking = true;
      try {
        await this.categoryStore.updateCategory(selected.categoryId, {
          categoryName: selected.categoryName,
          categoryDescription: selected.categoryDescription,
          status: selected.status,
          parentCategoryId: this.pickParentTarget.categoryId,
        });
        this.notify("Đã thêm danh mục con thành công");
        this.closePickModal();
      } catch (error) {
        const msg = error?.response?.data?.message || error?.message || "Không thể thêm danh mục con, vui lòng thử lại";
        this.notify(msg, "error");
      } finally {
        this.picking = false;
      }
    },
    // ===== Gán 1 danh mục độc lập (đã tạo ở "Quản lý danh mục") vào 1 trong 3 danh mục lớn =====
    openAssignModal(item) {
      if (this.isLocked(item)) return; // 3 danh mục lớn mặc định không thể trở thành con
      if (this.hasChildren(item.categoryId)) {
        this.notify("Danh mục này đang có danh mục con nên không thể gán làm con của danh mục khác", "error");
        return;
      }
      this.assignTarget = item;
      this.assignParentId = null;
      this.showAssignModal = true;
    },
    closeAssignModal() {
      this.showAssignModal = false;
      this.assignTarget = null;
      this.assignParentId = null;
    },
    async confirmAssign() {
      if (!this.assignParentId || !this.assignTarget) return;
      this.assigning = true;
      try {
        await this.categoryStore.updateCategory(this.assignTarget.categoryId, {
          categoryName: this.assignTarget.categoryName,
          categoryDescription: this.assignTarget.categoryDescription,
          status: this.assignTarget.status,
          parentCategoryId: this.assignParentId,
        });
        this.notify("Đã gán danh mục vào danh mục lớn thành công");
        this.closeAssignModal();
      } catch (error) {
        const msg = error?.response?.data?.message || error?.message || "Không thể gán danh mục, vui lòng thử lại";
        this.notify(msg, "error");
      } finally {
        this.assigning = false;
      }
    },
    // ===== Gỡ 1 danh mục con ra khỏi danh mục lớn (đưa về danh mục độc lập) =====
    confirmDetach(child) {
      this.detachTarget = child;
      this.showDetachModal = true;
    },
    async doDetach() {
      if (!this.detachTarget) return;
      this.detaching = true;
      try {
        await this.categoryStore.updateCategory(this.detachTarget.categoryId, {
          categoryName: this.detachTarget.categoryName,
          categoryDescription: this.detachTarget.categoryDescription,
          status: this.detachTarget.status,
          parentCategoryId: null,
        });
        this.notify("Đã gỡ danh mục khỏi danh mục lớn");
        this.showDetachModal = false;
      } catch (error) {
        const msg = error?.response?.data?.message || error?.message || "Không thể gỡ danh mục, vui lòng thử lại";
        this.notify(msg, "error");
      } finally {
        this.detaching = false;
      }
    },
    confirmDelete(item) {
      if (this.isLocked(item)) {
        this.notify("Danh mục lớn mặc định không thể xóa", "error");
        return;
      }
      this.deleteTarget = item;
      this.showDeleteModal = true;
    },
    async doDelete() {
      try {
        await this.categoryStore.deleteCategory(this.deleteTarget.categoryId);
        this.notify("Xóa danh mục thành công");
        this.showDeleteModal = false;
      } catch (error) {
        this.notify("Không thể xóa danh mục", "error");
      }
    },
    notify(message, type = "success") {
      this.toastMessage = message;
      this.toastType = type;
      this.showToast = true;
      clearTimeout(this.toastTimer);
      this.toastTimer = setTimeout(() => (this.showToast = false), 2500);
    },
  },
};
</script>

<style scoped>
.admin-category-hierarchy { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.subtitle { color: #7f8c8d; margin: 4px 0 0; max-width: 640px; }
.tree-container { background: white; border-radius: 12px; padding: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.parent-block { border-bottom: 1px solid #eee; }
.parent-block:last-child { border-bottom: none; }
.parent-row { display: flex; align-items: center; gap: 10px; padding: 14px 10px; }
.toggle-btn { background: none; border: none; font-size: 16px; cursor: pointer; width: 20px; }
.parent-name { flex: 1; font-size: 16px; }
.locked-badge { font-size: 12px; color: #8e44ad; background: #f3e8ff; padding: 3px 10px; border-radius: 20px; }
.row-actions { display: flex; gap: 6px; }
.action-btn { border: 1px solid #ddd; background: white; border-radius: 6px; padding: 6px 10px; cursor: pointer; font-size: 13px; }
.action-btn:hover { background: #f5f5f5; }
.action-assign { border-color: #667eea; color: #667eea; }
.action-assign:hover { background: #eef0ff; }
.action-detach { border-color: #e67e22; color: #e67e22; }
.action-detach:hover { background: #fdf1e6; }
.children-list { padding: 0 10px 12px 46px; display: flex; flex-direction: column; gap: 8px; }
.child-row { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: #fafafa; border-radius: 8px; }
.child-name { flex: 1; color: #34495e; }
.empty-children { color: #9ca3af; font-style: italic; padding: 6px 0; }
.status-badge { padding: 3px 10px; border-radius: 20px; font-size: 12px; }
.status-active { background: #d4edda; color: #155724; }
.status-inactive { background: #fff3cd; color: #856404; }
.empty-state { text-align: center; padding: 50px; color: #9ca3af; }
.empty-icon { font-size: 40px; }
.loading-state { text-align: center; padding: 60px 20px; display: flex; align-items: center; justify-content: center; gap: 12px; color: #3498db; }
.loading-spinner { width: 24px; height: 24px; border: 3px solid #e5e7eb; border-top-color: #3498db; border-radius: 50%; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; z-index: 1000; }
.modal-container { background: white; border-radius: 12px; width: 480px; max-width: 90%; }
.modal-container.modal-sm { width: 400px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: linear-gradient(135deg, #667eea, #764ba2); color: white; border-radius: 12px 12px 0 0; }
.close-btn { background: none; border: none; font-size: 22px; color: white; cursor: pointer; }
.modal-body { padding: 20px; }
.modal-footer { padding: 16px 20px; border-top: 1px solid #eee; display: flex; justify-content: flex-end; gap: 12px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: 500; }
.form-group input, .form-group select, .form-group textarea { width: 100%; padding: 10px 12px; border: 1px solid #ddd; border-radius: 8px; }
.hint-text { color: #e67e22; display: block; margin-top: 4px; }
.required { color: #e74c3c; }
.btn-cancel, .btn-save, .btn-danger { padding: 8px 20px; border: none; border-radius: 8px; cursor: pointer; }
.btn-cancel { background: #95a5a6; color: white; }
.btn-save { background: linear-gradient(135deg, #667eea, #764ba2); color: white; }
.btn-save:disabled, .btn-danger:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-danger { background: #dc3545; color: white; }
.warning-text { color: #dc3545; margin-top: 8px; }
.toast { position: fixed; bottom: 30px; right: 30px; padding: 12px 24px; border-radius: 8px; background: #27ae60; color: white; opacity: 0; transform: translateY(20px); transition: all 0.3s; }
.toast.toast-error { background: #e74c3c; }
.toast.show { opacity: 1; transform: translateY(0); }
</style>