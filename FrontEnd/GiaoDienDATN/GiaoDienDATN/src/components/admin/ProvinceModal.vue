<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal-container">
      <div class="modal-header">
        <h3>{{ isEdit ? '✏️ Sửa tỉnh/thành phố' : '➕ Thêm tỉnh/thành phố mới' }}</h3>
        <button class="close-btn" @click="close">✕</button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>Tên tỉnh/thành phố <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.provinceName"
              required
              placeholder="VD: Hà Nội, Hồ Chí Minh, Đà Nẵng..."
              maxlength="100"
            />
          </div>

          <div class="form-group">
            <label>Mã tỉnh/thành phố <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.provinceCode"
              required
              placeholder="VD: HN, HCM, DN..."
              maxlength="20"
            />
            <small class="form-hint">Mã duy nhất, không trùng lặp</small>
          </div>

          <div class="form-group">
            <label>Loại <span class="required">*</span></label>
            <select v-model="form.provinceType" required>
              <option value="">-- Chọn loại --</option>
              <option value="Thành phố">Thành phố</option>
              <option value="Tỉnh">Tỉnh</option>
            </select>
          </div>

          <div class="form-group">
            <label>Thứ tự hiển thị</label>
            <input
              type="number"
              v-model.number="form.displayOrder"
              min="0"
              placeholder="0"
            />
          </div>

          <div class="form-group" v-if="isEdit">
            <label>Trạng thái</label>
            <select v-model="form.status">
              <option value="ACTIVE">Hoạt động</option>
              <option value="INACTIVE">Tạm dừng</option>
            </select>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="close">Hủy</button>
            <button type="submit" class="btn-save" :disabled="submitting">
              <span v-if="submitting" class="loading-spinner-small"></span>
              <span v-else>💾</span>
              {{ submitting ? 'Đang lưu...' : (isEdit ? 'Cập nhật' : 'Thêm mới') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProvinceModal',
  props: {
    visible: Boolean,
    isEdit: Boolean,
    initialData: Object
  },
  emits: ['close', 'submit'],
  data() {
    return {
      submitting: false,
      form: {
        provinceName: '',
        provinceCode: '',
        provinceType: '',
        displayOrder: 0,
        status: 'ACTIVE'
      }
    }
  },
  watch: {
    initialData: {
      handler(val) {
        if (val) {
          this.form = {
            provinceName: val.provinceName || '',
            provinceCode: val.provinceCode || '',
            provinceType: val.provinceType || '',
            displayOrder: val.displayOrder || 0,
            status: val.status || 'ACTIVE'
          }
        }
      },
      immediate: true
    },
    visible: {
      handler(val) {
        if (!val) {
          this.submitting = false
          if (!this.isEdit) {
            this.form = {
              provinceName: '',
              provinceCode: '',
              provinceType: '',
              displayOrder: 0,
              status: 'ACTIVE'
            }
          }
        }
      }
    }
  },
  methods: {
    close() {
      this.$emit('close')
    },
    async handleSubmit() {
      // Validate
      const name = this.form.provinceName?.trim()
      const code = this.form.provinceCode?.trim().toUpperCase()
      const type = this.form.provinceType

      if (!name) {
        alert('Vui lòng nhập tên tỉnh/thành phố!')
        return
      }
      if (!code) {
        alert('Vui lòng nhập mã tỉnh/thành phố!')
        return
      }
      if (code.length > 20) {
        alert('Mã tỉnh/thành phố không được vượt quá 20 ký tự!')
        return
      }
      if (!type) {
        alert('Vui lòng chọn loại!')
        return
      }

      this.submitting = true
      try {
        const submitData = {
          provinceName: name,
          provinceCode: code,
          provinceType: type,
          displayOrder: this.form.displayOrder || 0
        }

        // Nếu là edit, thêm status
        if (this.isEdit) {
          submitData.status = this.form.status || 'ACTIVE'
        }

        await this.$emit('submit', submitData)
        this.close()
      } catch (error) {
        console.error('Submit error:', error)
        alert(error.response?.data?.message || 'Có lỗi xảy ra!')
      } finally {
        this.submitting = false
      }
    }
  }
}
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
  backdrop-filter: blur(5px);
}

.modal-container {
  background: white;
  border-radius: 12px;
  width: 500px;
  max-width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideIn 0.3s;
}

@keyframes slideIn {
  from { transform: translateY(-30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 12px 12px 0 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: white;
}

.close-btn:hover {
  opacity: 0.8;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #2c3e50;
}

.required {
  color: #e74c3c;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

.btn-cancel, .btn-save {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-cancel {
  background: #95a5a6;
  color: white;
}

.btn-cancel:hover {
  background: #7f8c8d;
}

.btn-save {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner-small {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid white;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>