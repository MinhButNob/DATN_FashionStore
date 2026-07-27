<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal-container">
      <div class="modal-header">
        <h3>{{ isEdit ? "Cập nhật khuyến mãi" : "Thêm khuyến mãi mới" }}</h3>
        <button class="close-btn" @click="close">&times;</button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>Tên khuyến mãi *</label>
            <input
              type="text"
              v-model="form.promotionName"
              class="form-control"
              required
              maxlength="100"
            />
          </div>
          <div class="form-group">
            <label>Mô tả</label>
            <textarea
              v-model="form.promotionDescription"
              class="form-control"
              rows="2"
              maxlength="255"
            ></textarea>
          </div>
          <div class="form-group">
            <label>Phần trăm giảm (%) *</label>
            <input
              type="number"
              v-model="form.promotionDiscountPercent"
              class="form-control"
              required
              min="0"
              max="100"
              step="0.01"
            />
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label>Ngày bắt đầu *</label>
              <input
                type="datetime-local"
                v-model="form.promotionStartDate"
                class="form-control"
                required
              />
            </div>
            <div class="form-group half">
              <label>Ngày kết thúc *</label>
              <input
                type="datetime-local"
                v-model="form.promotionEndDate"
                class="form-control"
                required
              />
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-secondary" @click="close">
              Hủy
            </button>
            <button type="submit" class="btn-primary" :disabled="submitting">
              {{
                submitting ? "Đang xử lý..." : isEdit ? "Cập nhật" : "Thêm mới"
              }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { useNotification } from "../../composables/useNotification";

const props = defineProps({
  visible: Boolean,
  isEdit: Boolean,
  initialData: Object });

const emit = defineEmits(["close", "submit"]);
const { showNotification } = useNotification();

const formatDateTime = (dateStr) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  return date.toISOString().slice(0, 16);
};

const form = ref({
  promotionName: "",
  promotionDescription: "",
  promotionDiscountPercent: null,
  promotionStartDate: "",
  promotionEndDate: "" });

const submitting = ref(false);

watch(
  () => props.initialData,
  (newData) => {
    if (newData) {
      form.value = {
        promotionName: newData.promotionName || "",
        promotionDescription: newData.promotionDescription || "",
        promotionDiscountPercent: newData.promotionDiscountPercent || null,
        promotionStartDate: formatDateTime(newData.promotionStartDate),
        promotionEndDate: formatDateTime(newData.promotionEndDate) };
    }
  },
  { immediate: true },
);

watch(
  () => props.visible,
  (newVal) => {
    if (!newVal) {
      form.value = {
        promotionName: "",
        promotionDescription: "",
        promotionDiscountPercent: null,
        promotionStartDate: "",
        promotionEndDate: "" };
      submitting.value = false;
    }
  },
);

const close = () => emit("close");

const handleSubmit = async () => {
  // Trim inputs
  const nameClean = form.value.promotionName ? form.value.promotionName.trim() : "";
  const descClean = form.value.promotionDescription ? form.value.promotionDescription.trim() : "";
  
  if (!nameClean) {
    showNotification("Tên khuyến mãi không được để trống hoặc chỉ chứa khoảng trắng!", "error");
    return;
  }

  // Validate discount percentage
  const discount = parseFloat(form.value.promotionDiscountPercent);
  if (isNaN(discount) || discount <= 0 || discount > 100) {
    showNotification("Phần trăm giảm phải lớn hơn 0 và nhỏ hơn hoặc bằng 100!", "error");
    return;
  }

  // Validate dates
  if (!form.value.promotionStartDate || !form.value.promotionEndDate) {
    showNotification("Vui lòng chọn đầy đủ ngày bắt đầu và kết thúc!", "error");
    return;
  }

  const start = new Date(form.value.promotionStartDate);
  const end = new Date(form.value.promotionEndDate);
  if (end <= start) {
    showNotification("Ngày kết thúc phải sau ngày bắt đầu!", "error");
    return;
  }

  submitting.value = true;
  try {
    const submitData = {
      ...form.value,
      promotionName: nameClean,
      promotionDescription: descClean,
      promotionDiscountPercent: discount
    };
    await emit("submit", submitData);
    close();
  } catch (error) {
    console.error("Submit error:", error);
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
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
}
.modal-container {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 550px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.modal-header h3 {
  font-size: 20px;
  color: #333;
  margin: 0;
}
.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}
.modal-body {
  padding: 25px;
}
.form-group {
  margin-bottom: 15px;
}
.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #555;
}
.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}
.form-control:focus {
  outline: none;
  border-color: #e63946;
}
textarea.form-control {
  resize: vertical;
}
.form-row {
  display: flex;
  gap: 15px;
}
.form-group.half {
  flex: 1;
}
.modal-footer {
  padding: 15px 0 0;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.btn-primary,
.btn-secondary {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}
.btn-primary {
  background: #e63946;
  color: white;
}
.btn-primary:hover {
  background: #c82333;
}
.btn-secondary {
  background: #f5f5f5;
  color: #666;
}
.btn-secondary:hover {
  background: #e0e0e0;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
