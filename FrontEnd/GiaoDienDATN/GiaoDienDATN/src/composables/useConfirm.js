import { ref } from 'vue'

const visible = ref(false)
const loading = ref(false)
const title = ref('Xác nhận')
const message = ref('Bạn có chắc chắn muốn thực hiện thao tác này không?')
const confirmText = ref('Xác nhận')
let resolver = null

export function useConfirm() {
  const confirm = (options = {}) => {
    if (visible.value) return Promise.resolve(false)
    title.value = options.title || 'Xác nhận'
    message.value = options.message || 'Bạn có chắc chắn muốn thực hiện thao tác này không?'
    confirmText.value = options.confirmText || 'Xác nhận'
    visible.value = true
    loading.value = false
    return new Promise((resolve) => { resolver = resolve })
  }
  const cancel = () => {
    if (loading.value) return
    visible.value = false
    resolver?.(false)
    resolver = null
  }
  const accept = () => {
    if (loading.value) return
    loading.value = true
    resolver?.(true)
    resolver = null
  }
  const close = () => {
    visible.value = false
    loading.value = false
  }
  return { visible, loading, title, message, confirmText, confirm, cancel, accept, close }
}
