<template>
  <!-- Drag overlay: vô hình, fixed, theo dõi vị trí oml2d và cho phép kéo -->
  <div
    ref="dragOverlayRef"
    class="haru-drag-overlay"
    :style="overlayStyle"
    @mousedown="onDragStart"
    @touchstart.passive="onTouchStart"
    title="Kéo để di chuyển Haru"
  ></div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, nextTick, ref, computed, defineEmits, defineExpose } from 'vue'
import { loadOml2d } from 'oh-my-live2d'

const emit = defineEmits(['avatar-click', 'position-change'])

let oml2dInstance = null
let oml2dRootEl = null   // element #oml2d do thư viện tạo ra

// Vị trí hiện tại của Haru (bottom-left của oml2d container)
const haruX = ref(20)    // khoảng cách từ left (px)
const haruBottom = ref(0)
const haruWidth = ref(200)
const haruHeight = ref(300)

const dragOverlayRef = ref(null)

// Overlay style bám sát oml2d container
const overlayStyle = computed(() => ({
  left: haruX.value + 'px',
  bottom: haruBottom.value + 'px',
  width: haruWidth.value + 'px',
  height: haruHeight.value + 'px',
  cursor: isDragging.value ? 'grabbing' : 'grab',
}))

// ---- Drag state ----
const isDragging = ref(false)
let dragStartMouseX = 0
let dragStartMouseY = 0
let dragStartElLeft = 0
let dragStartElBottom = 0

// Tìm element gốc mà oh-my-live2d tạo ra
const findOml2dRoot = () => {
  return (
    document.getElementById('oml2d') ||
    document.querySelector('#oml2d-stage') ||
    document.querySelector('.oml2d-stage') ||
    document.querySelector('[id^="oml2d"]') ||
    (oml2dInstance?.canvas?.closest?.('[id]'))
  )
}

// Đọc vị trí thực tế của oml2d element để sync overlay
const syncOverlayToOml2d = () => {
  if (!oml2dRootEl) {
    oml2dRootEl = findOml2dRoot()
  }
  if (!oml2dRootEl) return

  const rect = oml2dRootEl.getBoundingClientRect()
  haruX.value = rect.left
  haruBottom.value = window.innerHeight - rect.bottom
  haruWidth.value = rect.width || 200
  haruHeight.value = rect.height || 300

  // Emit vị trí trung tâm X để HaruChatbot tính toán
  emit('position-change', {
    x: rect.left,
    centerX: rect.left + rect.width / 2,
    width: rect.width,
    bottom: haruBottom.value,
  })
}

// ---- Kéo thả ----
const onDragStart = (e) => {
  if (e.button !== 0) return // chỉ chuột trái
  e.preventDefault()
  isDragging.value = true

  if (!oml2dRootEl) oml2dRootEl = findOml2dRoot()
  if (!oml2dRootEl) return

  const rect = oml2dRootEl.getBoundingClientRect()
  dragStartMouseX = e.clientX
  dragStartMouseY = e.clientY
  dragStartElLeft = rect.left
  dragStartElBottom = window.innerHeight - rect.bottom

  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}

const onTouchStart = (e) => {
  const touch = e.touches[0]
  isDragging.value = true

  if (!oml2dRootEl) oml2dRootEl = findOml2dRoot()
  if (!oml2dRootEl) return

  const rect = oml2dRootEl.getBoundingClientRect()
  dragStartMouseX = touch.clientX
  dragStartMouseY = touch.clientY
  dragStartElLeft = rect.left
  dragStartElBottom = window.innerHeight - rect.bottom

  window.addEventListener('touchmove', onTouchMove, { passive: false })
  window.addEventListener('touchend', onTouchEnd)
}

const BOTTOM_OFFSET = -60 // phần đáy canvas ẩn dưới viewport để hiện đủ chân

const onDragMove = (e) => {
  if (!isDragging.value || !oml2dRootEl) return

  const dx = e.clientX - dragStartMouseX
  const dy = e.clientY - dragStartMouseY

  let newLeft = dragStartElLeft + dx
  let newBottom = dragStartElBottom - dy

  // Giới hạn trong viewport (cho phép bottom âm đến BOTTOM_OFFSET để chân luôn hiện)
  const maxLeft = window.innerWidth - (oml2dRootEl.offsetWidth || 200)
  newLeft = Math.max(0, Math.min(newLeft, maxLeft))
  const maxBottom = window.innerHeight - (oml2dRootEl.offsetHeight || 300)
  newBottom = Math.max(BOTTOM_OFFSET, Math.min(newBottom, maxBottom))

  // Di chuyển element của oml2d
  oml2dRootEl.style.position = 'fixed'
  oml2dRootEl.style.left = newLeft + 'px'
  oml2dRootEl.style.bottom = newBottom + 'px'
  oml2dRootEl.style.right = 'auto'
  oml2dRootEl.style.top = 'auto'
  oml2dRootEl.style.transition = 'none'
  oml2dRootEl.style.transform = ''

  // Sync overlay
  haruX.value = newLeft
  haruBottom.value = newBottom
  haruWidth.value = oml2dRootEl.offsetWidth || 200
  haruHeight.value = oml2dRootEl.offsetHeight || 300

  emit('position-change', {
    x: newLeft,
    centerX: newLeft + (oml2dRootEl.offsetWidth || 200) / 2,
    width: oml2dRootEl.offsetWidth || 200,
    bottom: newBottom,
  })
}

const onTouchMove = (e) => {
  e.preventDefault()
  const touch = e.touches[0]
  onDragMove({ clientX: touch.clientX, clientY: touch.clientY })
}

const onDragEnd = () => {
  isDragging.value = false
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}

const onTouchEnd = () => {
  isDragging.value = false
  window.removeEventListener('touchmove', onTouchMove)
  window.removeEventListener('touchend', onTouchEnd)
}

// ---- Init Live2D ----
const initLive2D = async () => {
  try {
    console.log('🔄 Đang tải Haru Live2D (oh-my-live2d)...')
    const modelPath = '/model/runtime/haru_greeter_t05.model3.json'

    try {
      const response = await fetch(modelPath)
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
    } catch (fetchError) {
      console.error('❌ Model not found:', fetchError)
      return
    }

    oml2dInstance = await loadOml2d({
      models: [{ path: modelPath, scale: 0.1, position: [0, 0] }],
      mobileDisplay: true,
      menus: false,
      statusBar: { disable: true },
      tips: { style: { display: 'none' } },
      stageStyle: {
        width: '280px',
        height: '420px',
      }
    })

    console.log('✅ oh-my-live2d initialized successfully')

    // Chờ DOM render xong rồi tìm element
    await nextTick()
    setTimeout(() => {
      oml2dRootEl = findOml2dRoot()
      if (oml2dRootEl) {
        // Đặt vị trí mặc định fixed bottom-left
        // bottom âm để phần padding trắng đáy canvas ẩn dưới viewport
        oml2dRootEl.style.position = 'fixed'
        oml2dRootEl.style.left = '20px'
        oml2dRootEl.style.bottom = '-60px'
        oml2dRootEl.style.right = 'auto'
        oml2dRootEl.style.top = 'auto'
        oml2dRootEl.style.overflow = 'visible'
        // Đảm bảo canvas và các phần tử con không bị clip
        oml2dRootEl.querySelectorAll('*').forEach(el => {
          el.style.overflow = 'visible'
        })
        syncOverlayToOml2d()
      }
    }, 800)

    // Click event
    if (oml2dInstance?.canvas) {
      oml2dInstance.canvas.addEventListener('click', () => emit('avatar-click'))
    } else if (typeof oml2dInstance?.onStageClick === 'function') {
      oml2dInstance.onStageClick(() => emit('avatar-click'))
    }

  } catch (error) {
    console.error('❌ Failed to load oh-my-live2d:', error.message)
  }
}

defineExpose({
  triggerMotion: (motionName = 'idle') => {
    if (!oml2dInstance?.models?.[0]) return
    const motionMap = { idle: 0, speak: 1, greeting: 3, happy: 5 }
    const index = motionMap[motionName] ?? 0
    try {
      oml2dInstance.models[0].internalModel?.motionManager?.startMotion('', index)
    } catch (e) { console.log('Motion error:', e) }
  },
  setExpression: (expression = 'normal') => {
    if (!oml2dInstance?.models?.[0]) return
    try {
      oml2dInstance.models[0].internalModel?.motionManager?.expressionManager?.setExpression(expression)
    } catch (e) { console.log('Expression error:', e) }
  },
  toggleDisplay: (show) => {
    if (!oml2dInstance) return
    const el = oml2dRootEl || findOml2dRoot()
    if (el) {
      el.style.transition = 'opacity 0.3s ease'
      el.style.opacity = show ? '1' : '0'
      el.style.pointerEvents = show ? 'auto' : 'none'
    }
    // Overlay cũng ẩn khi Haru ẩn
    if (dragOverlayRef.value) {
      dragOverlayRef.value.style.opacity = show ? '1' : '0'
      dragOverlayRef.value.style.pointerEvents = show ? 'auto' : 'none'
    }
  },
  getPosition: () => ({
    x: haruX.value,
    centerX: haruX.value + haruWidth.value / 2,
    width: haruWidth.value,
    bottom: haruBottom.value,
  })
})

onMounted(() => {
  nextTick(() => setTimeout(initLive2D, 500))
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
  window.removeEventListener('touchmove', onTouchMove)
  window.removeEventListener('touchend', onTouchEnd)
  if (oml2dInstance && typeof oml2dInstance.unmount === 'function') {
    oml2dInstance.unmount()
  }
  oml2dInstance = null
})
</script>

<style scoped>
.haru-drag-overlay {
  position: fixed;
  z-index: 10000; /* trên cả oml2d để nhận sự kiện chuột */
  background: transparent;
  /* Không block pointer events khi đang idle — chỉ nhận drag */
  pointer-events: auto;
}
</style>