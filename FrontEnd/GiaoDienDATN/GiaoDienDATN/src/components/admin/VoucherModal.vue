<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal-container">
      <div class="modal-header">
        <h3>{{ isEdit ? "Cập nhật voucher" : "Thêm voucher mới" }}</h3>
        <button class="close-btn" @click="close">&times;</button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>Mã voucher *</label>
            <input
              type="text"
              v-model="form.voucherCode"
              class="form-control"
              required
              maxlength="20"
              placeholder="VD: SUMMER2024"
            />
          </div>

          <div class="form-group">
            <label>Loại giảm giá *</label>
            <div class="radio-group">
              <label class="radio-label">
                <input type="radio" value="amount" v-model="discountType" />
                <span>Giảm theo số tiền</span>
              </label>
              <label class="radio-label">
                <input type="radio" value="percent" v-model="discountType" />
                <span>Giảm theo phần trăm</span>
              </label>
            </div>
          </div>

          <div class="form-group" v-if="discountType === 'amount'">
            <label>Số tiền giảm (VNĐ) *</label>
            <input
              type="number"
              v-model="form.voucherDiscountAmount"
              class="form-control"
              min="0"
              step="1000"
              placeholder="VD: 50000"
            />
          </div>

          <div class="form-group" v-if="discountType === 'percent'">
            <label>Phần trăm giảm (%) *</label>
            <input
              type="number"
              v-model="form.voucherDiscountPercent"
              class="form-control"
              min="0"
              max="100"
              step="1"
              placeholder="VD: 10"
            />
          </div>

          <div class="form-row">
            <div class="form-group half">
              <label>Ngày bắt đầu *</label>
              <input
                type="datetime-local"
                v-model="form.voucherStartDate"
                class="form-control"
                required
              />
            </div>
            <div class="form-group half">
              <label>Ngày kết thúc *</label>
              <input
                type="datetime-local"
                v-model="form.voucherEndDate"
                class="form-control"
                required
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group half">
              <label>Số lần sử dụng tối đa</label>
              <input
                type="number"
                v-model="form.voucherMaxUsage"
                class="form-control"
                min="1"
                placeholder="Không giới hạn"
              />
            </div>
            <div class="form-group half">
              <label>Giá trị đơn hàng tối thiểu (VNĐ)</label>
              <input
                type="number"
                v-model="form.voucherMinOrderValue"
                class="form-control"
                min="0"
                step="1000"
                placeholder="0"
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
import { ref, watch, computed } from "vue";

const props = defineProps({
  visible: Boolean,
  isEdit: Boolean,
  initialData: Object,
});

const emit = defineEmits(["close", "submit"]);

const formatDateTime = (dateStr) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  return date.toISOString().slice(0, 16);
};

const discountType = ref("amount");

const form = ref({
  voucherCode: "",
  voucherDiscountAmount: null,
  voucherDiscountPercent: null,
  voucherStartDate: "",
  voucherEndDate: "",
  voucherMaxUsage: null,
  voucherMinOrderValue: 0,
});

const submitting = ref(false);

watch(
  () => props.initialData,
  (newData) => {
    if (newData) {
      const hasAmount =
        newData.voucherDiscountAmount && newData.voucherDiscountAmount > 0;
      const hasPercent =
        newData.voucherDiscountPercent && newData.voucherDiscountPercent > 0;

      discountType.value = hasAmount
        ? "amount"
        : hasPercent
        ? "percent"
        : "amount";

      form.value = {
        voucherCode: newData.voucherCode || "",
        voucherDiscountAmount: newData.voucherDiscountAmount || null,
        voucherDiscountPercent: newData.voucherDiscountPercent || null,
        voucherStartDate: formatDateTime(newData.voucherStartDate),
        voucherEndDate: formatDateTime(newData.voucherEndDate),
        voucherMaxUsage: newData.voucherMaxUsage || null,
        voucherMinOrderValue: newData.voucherMinOrderValue || 0,
      };
    }
  },
  { immediate: true },
);

watch(
  () => props.visible,
  (newVal) => {
    if (!newVal) {
      form.value = {
        voucherCode: "",
        voucherDiscountAmount: null,
        voucherDiscountPercent: null,
        voucherStartDate: "",
        voucherEndDate: "",
        voucherMaxUsage: null,
        voucherMinOrderValue: 0,
      };
      discountType.value = "amount";
      submitting.value = false;
    }
  },
);

const close = () => emit("close");

const handleSubmit = async () => {
  submitting.value = true;

  const submitData = { ...form.value };

  if (discountType.value === "amount") {
    submitData.voucherDiscountPercent = null;
  } else {
    submitData.voucherDiscountAmount = null;
  }

  try {
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
.form-row {
  display: flex;
  gap: 15px;
}
.form-group.half {
  flex: 1;
}
.radio-group {
  display: flex;
  gap: 20px;
}
.radio-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
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
