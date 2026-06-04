<!-- Chỉ sửa phần script, giữ nguyên template và style -->
<script>
export default {
  name: 'PasswordModal',
  data() {
    return {
      form: { newPassword: '', confirmPassword: '' },
      errors: { newPassword: '', confirmPassword: '' },
      serverError: ''
    }
  },
  computed: {
    isFormValid() {
      return this.form.newPassword && this.form.confirmPassword && 
             !this.errors.newPassword && !this.errors.confirmPassword
    }
  },
  methods: {
    validatePassword() {
      if (!this.form.newPassword) {
        this.errors.newPassword = 'Mật khẩu không được để trống'
      } else if (this.form.newPassword.length < 6) {
        this.errors.newPassword = 'Mật khẩu phải có ít nhất 6 ký tự'
      } else if (this.form.newPassword.length > 50) {
        this.errors.newPassword = 'Mật khẩu không được quá 50 ký tự'
      } else {
        this.errors.newPassword = ''
        // Nếu mật khẩu mới thay đổi, kiểm tra lại xác nhận
        if (this.form.confirmPassword) {
          this.validateConfirmPassword()
        }
      }
    },
    validateConfirmPassword() {
      if (!this.form.confirmPassword) {
        this.errors.confirmPassword = 'Vui lòng xác nhận mật khẩu'
      } else if (this.form.newPassword !== this.form.confirmPassword) {
        this.errors.confirmPassword = 'Mật khẩu xác nhận không khớp'
      } else {
        this.errors.confirmPassword = ''
      }
    },
    handleSubmit() {
      this.validatePassword()
      this.validateConfirmPassword()
      if (this.isFormValid) {
        this.$emit('submit', { newPassword: this.form.newPassword })
      }
    },
    resetForm() {
      this.form = { newPassword: '', confirmPassword: '' }
      this.errors = { newPassword: '', confirmPassword: '' }
      this.serverError = ''
    }
  },
  watch: {
    // Reset form khi modal mở
    'form.newPassword'() {
      if (this.serverError) this.serverError = ''
    }
  }
}
</script>