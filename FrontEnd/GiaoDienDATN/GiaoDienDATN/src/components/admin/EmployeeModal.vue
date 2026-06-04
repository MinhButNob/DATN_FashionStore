<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h3>{{ isEdit ? 'Sửa nhân viên' : 'Thêm nhân viên' }}</h3>
        <button class="close-btn" @click="$emit('close')">&times;</button>
      </div>

      <form @submit.prevent="submit">

        <!-- ✅ CHỈ hiện username + password khi TẠO MỚI -->
        <template v-if="!isEdit">
          <div class="form-group" :class="{ 'has-error': errors.username }">
            <label>Tên đăng nhập <span class="required">*</span></label>
            <input
              v-model="formData.username"
              type="text"
              class="form-control"
              placeholder="Nhập username (dùng để đăng nhập)"
              @input="clearError('username')"
            />
            <div class="error-message" v-if="errors.username">{{ errors.username }}</div>
          </div>

          <div class="form-group" :class="{ 'has-error': errors.password }">
            <label>Mật khẩu <span class="required">*</span></label>
            <input
              v-model="formData.password"
              type="password"
              class="form-control"
              placeholder="Tối thiểu 6 ký tự"
              @input="clearError('password')"
            />
            <div class="error-message" v-if="errors.password">{{ errors.password }}</div>
          </div>
        </template>

        <!-- ✅ CHỈ hiện username + password khi SỬA (tùy chọn) -->
        <template v-if="isEdit">
          <div class="form-group">
            <label>Tên đăng nhập mới <span class="optional">(không đổi thì để trống)</span></label>
            <input
              v-model="formData.username"
              type="text"
              class="form-control"
              placeholder="Để trống nếu không đổi username"
            />
          </div>

          <div class="form-group">
            <label>Mật khẩu mới <span class="optional">(không đổi thì để trống)</span></label>
            <input
              v-model="formData.password"
              type="password"
              class="form-control"
              placeholder="Để trống nếu không đổi mật khẩu"
            />
          </div>
        </template>

        <div class="form-group" :class="{ 'has-error': errors.employeeFullname }">
          <label>Họ tên <span class="required">*</span></label>
          <input
            v-model="formData.employeeFullname"
            type="text"
            class="form-control"
            placeholder="Nhập họ tên"
            @input="clearError('employeeFullname')"
          />
          <div class="error-message" v-if="errors.employeeFullname">{{ errors.employeeFullname }}</div>
        </div>

        <div class="form-group" :class="{ 'has-error': errors.employeeGender }">
          <label>Giới tính <span class="required">*</span></label>
          <select v-model="formData.employeeGender" class="form-control" @change="clearError('employeeGender')">
            <option :value="true">Nam</option>
            <option :value="false">Nữ</option>
          </select>
          <div class="error-message" v-if="errors.employeeGender">{{ errors.employeeGender }}</div>
        </div>

        <div class="form-group" :class="{ 'has-error': errors.employeeAge }">
          <label>Tuổi <span class="required">*</span></label>
          <input
            v-model="formData.employeeAge"
            type="number"
            class="form-control"
            placeholder="Nhập tuổi"
            @input="clearError('employeeAge')"
          />
          <div class="error-message" v-if="errors.employeeAge">{{ errors.employeeAge }}</div>
        </div>

        <div class="form-group" :class="{ 'has-error': errors.employeePhone }">
          <label>Số điện thoại <span class="required">*</span></label>
          <input
            v-model="formData.employeePhone"
            type="text"
            class="form-control"
            placeholder="Nhập số điện thoại"
            @input="clearError('employeePhone')"
          />
          <div class="error-message" v-if="errors.employeePhone">{{ errors.employeePhone }}</div>
        </div>

        <div class="form-group" :class="{ 'has-error': errors.employeeEmail }">
          <label>Email <span class="required">*</span></label>
          <input
            v-model="formData.employeeEmail"
            type="email"
            class="form-control"
            placeholder="Nhập email"
            @input="clearError('employeeEmail')"
          />
          <div class="error-message" v-if="errors.employeeEmail">{{ errors.employeeEmail }}</div>
        </div>

        <div class="form-group" :class="{ 'has-error': errors.employeePosition }">
          <label>Chức vụ</label>
          <input
            v-model="formData.employeePosition"
            type="text"
            class="form-control"
            placeholder="Mặc định: Nhân viên"
            @input="clearError('employeePosition')"
          />
          <div class="error-message" v-if="errors.employeePosition">{{ errors.employeePosition }}</div>
        </div>

        <div class="form-group">
          <label>Trạng thái</label>
          <select v-model="formData.employeeStatus" class="form-control">
            <option value="ACTIVE">Đang hoạt động</option>
            <option value="INACTIVE">Không hoạt động</option>
            <option value="ON_LEAVE">Nghỉ phép</option>
            <option value="TERMINATED">Đã nghỉ việc</option>
          </select>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn-cancel" @click="$emit('close')">Hủy</button>
          <button type="submit" class="btn-submit">{{ isEdit ? 'Cập nhật' : 'Thêm mới' }}</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EmployeeModal',
  props: {
    isEdit: { type: Boolean, default: false },
    initialData: { type: Object, default: null }
  },
  data() {
    return {
      formData: {
        username: '',
        password: '',
        employeeFullname: '',
        employeeGender: true,
        employeeAge: null,
        employeePhone: '',
        employeeEmail: '',
        employeePosition: '',
        employeeStatus: 'ACTIVE'
      },
      errors: {
        username: '',
        password: '',
        employeeFullname: '',
        employeeGender: '',
        employeeAge: '',
        employeePhone: '',
        employeeEmail: '',
        employeePosition: ''
      }
    }
  },
  watch: {
    initialData: {
      handler(val) {
        if (val && this.isEdit) {
          this.formData = {
            username: '',       // ✅ Để trống khi sửa, user tự nhập nếu muốn đổi
            password: '',       // ✅ Để trống khi sửa
            employeeFullname: val.employeeFullname || '',
            employeeGender: val.employeeGender !== undefined ? val.employeeGender : true,
            employeeAge: val.employeeAge || null,
            employeePhone: val.employeePhone || '',
            employeeEmail: val.employeeEmail || '',
            employeePosition: val.employeePosition || '',
            employeeStatus: val.employeeStatus || 'ACTIVE'
          }
        } else if (!this.isEdit) {
          this.resetForm()
        }
        this.clearAllErrors()
      },
      immediate: true,
      deep: true
    }
  },
  methods: {
    resetForm() {
      this.formData = {
        username: '',
        password: '',
        employeeFullname: '',
        employeeGender: true,
        employeeAge: null,
        employeePhone: '',
        employeeEmail: '',
        employeePosition: '',
        employeeStatus: 'ACTIVE'
      }
    },
    clearError(field) { this.errors[field] = '' },
    clearAllErrors() {
      Object.keys(this.errors).forEach(k => { this.errors[k] = '' })
    },
    validate() {
      let isValid = true

      // ✅ Chỉ validate username + password khi TẠO MỚI
      if (!this.isEdit) {
        if (!this.formData.username || this.formData.username.trim().length < 3) {
          this.errors.username = 'Username phải từ 3 ký tự trở lên'
          isValid = false
        }
        if (!this.formData.password || this.formData.password.length < 6) {
          this.errors.password = 'Mật khẩu phải có ít nhất 6 ký tự'
          isValid = false
        }
      }

      if (!this.formData.employeeFullname || this.formData.employeeFullname.trim().length < 2) {
        this.errors.employeeFullname = 'Họ tên phải có ít nhất 2 ký tự'
        isValid = false
      }
      if (this.formData.employeeGender === undefined || this.formData.employeeGender === null) {
        this.errors.employeeGender = 'Giới tính không được để trống'
        isValid = false
      }
      if (!this.formData.employeeAge) {
        this.errors.employeeAge = 'Tuổi không được để trống'
        isValid = false
      } else if (this.formData.employeeAge < 18 || this.formData.employeeAge > 100) {
        this.errors.employeeAge = 'Tuổi phải từ 18 đến 100'
        isValid = false
      }
      if (!this.formData.employeePhone || !/^(0[1-9][0-9]{8,9})$/.test(this.formData.employeePhone)) {
        this.errors.employeePhone = 'Số điện thoại không hợp lệ (VD: 0912345678)'
        isValid = false
      }
      if (!this.formData.employeeEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.formData.employeeEmail)) {
        this.errors.employeeEmail = 'Email không hợp lệ'
        isValid = false
      }

      return isValid
    },
    submit() {
      this.clearAllErrors()
      if (!this.validate()) {
        const firstError = this.$el.querySelector('.has-error')
        if (firstError) firstError.scrollIntoView({ behavior: 'smooth', block: 'center' })
        return
      }

      const submitData = {
        employeeFullname: this.formData.employeeFullname.trim(),
        employeeGender: this.formData.employeeGender,
        employeeAge: Number(this.formData.employeeAge),
        employeePhone: this.formData.employeePhone.trim(),
        employeeEmail: this.formData.employeeEmail.trim(),
        // ✅ Nếu để trống thì BE sẽ tự set "Nhân viên"
        employeePosition: this.formData.employeePosition.trim() || null,
        employeeStatus: this.formData.employeeStatus
      }

      // ✅ Chỉ gửi username/password khi tạo mới HOẶC khi sửa có nhập giá trị
      if (!this.isEdit) {
        submitData.username = this.formData.username.trim()
        submitData.password = this.formData.password
      } else {
        if (this.formData.username && this.formData.username.trim()) {
          submitData.username = this.formData.username.trim()
        }
        if (this.formData.password && this.formData.password.trim()) {
          submitData.password = this.formData.password.trim()
        }
      }

      this.$emit('submit', submitData)
    }
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  border-radius: 12px;
  width: 500px;
  max-width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}
.modal-header h3 { margin: 0; font-size: 18px; }
.close-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: #6b7280; }
.form-group { padding: 12px 20px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: 500; font-size: 14px; }
.required { color: #ef4444; }
.optional { color: #9ca3af; font-size: 12px; font-weight: 400; }
.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
  box-sizing: border-box;
}
.form-control:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59,130,246,0.1);
}
.form-group.has-error .form-control {
  border-color: #ef4444;
  background-color: #fef2f2;
}
.error-message {
  color: #ef4444;
  font-size: 12px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.error-message::before { content: "⚠"; font-size: 12px; }
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
}
.btn-cancel {
  padding: 8px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.btn-cancel:hover { background: #e5e7eb; }
.btn-submit {
  padding: 8px 16px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.btn-submit:hover { background: #2563eb; }
</style>