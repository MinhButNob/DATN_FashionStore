<template>
  <Teleport to="body">
    <div v-if="visible" class="confirm-overlay">
      <div class="confirm-dialog">
        <div class="confirm-header">
          <h3>{{ title }}</h3>
          <button class="close-btn" :disabled="loading" @click="cancel">&times;</button>
        </div>
        <div class="confirm-body">
          <p>{{ message }}</p>
        </div>
        <div class="confirm-footer">
          <button class="btn-cancel" :disabled="loading" @click="cancel">Hủy</button>
          <button class="btn-confirm" :disabled="loading" @click="confirm">
            {{ loading ? 'Đang xử lý...' : confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: "Xác nhận" },
  message: { type: String, default: "Bạn có chắc chắn?" },
  confirmText: { type: String, default: "Xác nhận" },
  loading: { type: Boolean, default: false } });

const emit = defineEmits(["confirm", "cancel"]);

const confirm = () => emit("confirm");
const cancel = () => emit("cancel");
</script>

<style scoped>
.confirm-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}
.confirm-dialog {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}
.confirm-header {
  padding: 18px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.confirm-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}
.close-btn {
  background: none;
  border: none;
  font-size: 22px;
  cursor: pointer;
  color: #999;
}
.close-btn:hover {
  color: #333;
}
.confirm-body {
  padding: 25px 20px;
}
.confirm-body p {
  margin: 0;
  color: #555;
  font-size: 15px;
}
.confirm-footer {
  padding: 15px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.btn-cancel,
.btn-confirm {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.btn-cancel {
  background: #f5f5f5;
  color: #666;
}
.btn-cancel:hover {
  background: #e0e0e0;
}
.btn-confirm {
  background: #dc3545;
  color: white;
}
.btn-confirm:hover {
  background: #c82333;
}
.btn-cancel:disabled,
.btn-confirm:disabled,
.close-btn:disabled { opacity: .6; cursor: not-allowed; }
</style>
