<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h3>{{ isEdit ? "Sửa vai trò" : "Thêm vai trò mới" }}</h3>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <form @submit.prevent="handleSubmit" class="modal-form">
        <div class="form-group">
          <label for="roleName"
            >Tên vai trò <span class="required">*</span></label
          >
          <input
            id="roleName"
            v-model="formData.roleName"
            type="text"
            required
            placeholder="Ví dụ: ADMIN, USER..."
            :class="{ 'is-invalid': errors.roleName }"
          />
          <span class="error-msg" v-if="errors.roleName">{{
            errors.roleName
          }}</span>
        </div>

        <div class="form-group">
          <label for="status">Trạng thái</label>
          <select id="status" v-model="formData.status" class="status-select">
            <option value="ACTIVE">Hoạt động</option>
            <option value="INACTIVE">Không hoạt động</option>
          </select>
        </div>

        <div class="form-actions">
          <button type="button" class="btn-cancel" @click="$emit('close')">
            Hủy
          </button>
          <button type="submit" class="btn-submit" :disabled="isSubmitting">
            {{
              isSubmitting ? "Đang xử lý..." : isEdit ? "Cập nhật" : "Thêm mới"
            }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
export default {
  name: "RoleModal",
  props: {
    isEdit: {
      type: Boolean,
      default: false },
    initialData: {
      type: Object,
      default: () => null } },
  data() {
    return {
      formData: {
        roleName: "",
        status: "ACTIVE",  // Mặc định là "ACTIVE"
      },
      errors: {},
      isSubmitting: false };
  },
  created() {
    if (this.isEdit && this.initialData) {
      this.formData = {
        roleName: this.initialData.roleName || "",
        status: this.normalizeStatus(this.initialData.status) };
    }
  },
  methods: {
    normalizeStatus(status) {
      // Chuyển đổi các dạng status về "ACTIVE" hoặc "INACTIVE"
      if (!status) return "ACTIVE";
      if (status === true || status === "true" || status === "ACTIVE") return "ACTIVE";
      if (status === false || status === "false" || status === "INACTIVE") return "INACTIVE";
      return "ACTIVE";
    },
    validateForm() {
      this.errors = {};
      let isValid = true;

      if (!this.formData.roleName || this.formData.roleName.trim() === "") {
        this.errors.roleName = "Tên vai trò không được để trống";
        isValid = false;
      } else if (this.formData.roleName.length < 2) {
        this.errors.roleName = "Tên vai trò phải từ 2 ký tự trở lên";
        isValid = false;
      } else if (this.formData.roleName.length > 50) {
        this.errors.roleName = "Tên vai trò không được quá 50 ký tự";
        isValid = false;
      }

      return isValid;
    },
    async handleSubmit() {
      if (!this.validateForm()) return;

      this.isSubmitting = true;
      try {
        const payload = {
          roleName: this.formData.roleName.trim(),
          status: this.formData.status,  // Gửi "ACTIVE" hoặc "INACTIVE"
        };

        this.$emit("submit", payload);
      } finally {
        this.isSubmitting = false;
      }
    } } };
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background-color: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  color: #333;
  font-size: 1.25rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #e74c3c;
}

.modal-form {
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.required {
  color: #e74c3c;
}

.form-group input[type="text"] {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-group input[type="text"]:focus {
  border-color: #3498db;
  outline: none;
}

.form-group input.is-invalid {
  border-color: #e74c3c;
}

.status-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
  background-color: white;
  cursor: pointer;
  transition: border-color 0.2s;
}

.status-select:focus {
  border-color: #3498db;
  outline: none;
}

.error-msg {
  display: block;
  color: #e74c3c;
  font-size: 0.85rem;
  margin-top: 5px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.btn-cancel {
  padding: 10px 20px;
  background-color: #f1f1f1;
  color: #333;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-cancel:hover {
  background-color: #e1e1e1;
}

.btn-submit {
  padding: 10px 20px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-submit:hover:not(:disabled) {
  background-color: #2980b9;
}

.btn-submit:disabled {
  background-color: #9cb4c4;
  cursor: not-allowed;
}
</style>