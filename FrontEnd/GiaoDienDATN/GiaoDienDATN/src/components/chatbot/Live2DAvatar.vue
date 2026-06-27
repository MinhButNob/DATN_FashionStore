<template>
  <div class="live2d-anchor"></div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, nextTick, defineEmits, defineExpose } from 'vue'
import { loadOml2d } from 'oh-my-live2d'

const emit = defineEmits(['avatar-click'])
let oml2dInstance = null

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
      models: [
        {
          path: modelPath,
          scale: 0.1, 
          position: [0, 50], 
        }
      ],
      mobileDisplay: true,
      menus: false, // Tắt floating menu mặc định
      statusBar: {
        disable: true // Tắt thanh trạng thái báo load thành công (chữ Trung Quốc)
      },
      tips: {
        style: {
          display: 'none' // Ẩn hoàn toàn khung bong bóng chat mặc định của thư viện
        }
      }
    })
    
    console.log('✅ oh-my-live2d initialized successfully')

    // Lắng nghe sự kiện click vào Haru
    if (oml2dInstance?.canvas) {
      oml2dInstance.canvas.addEventListener('click', () => {
        emit('avatar-click')
      })
    } else if (typeof oml2dInstance?.onStageClick === 'function') {
      oml2dInstance.onStageClick(() => {
        emit('avatar-click')
      })
    }
    
  } catch (error) {
    console.error('❌ Failed to load oh-my-live2d:', error.message)
  }
}

defineExpose({
  triggerMotion: (motionName = 'idle') => {
    if (!oml2dInstance || !oml2dInstance.models || !oml2dInstance.models[0]) return
    const motionMap = { 'idle': 0, 'speak': 1, 'greeting': 3, 'happy': 5 }
    const index = motionMap[motionName] !== undefined ? motionMap[motionName] : 0
    try {
      const internalModel = oml2dInstance.models[0].internalModel
      if (internalModel && internalModel.motionManager) {
        internalModel.motionManager.startMotion('', index)
      }
    } catch (e) {
      console.log('Motion error:', e)
    }
  },
  setExpression: (expression = 'normal') => {
    if (!oml2dInstance || !oml2dInstance.models || !oml2dInstance.models[0]) return
    try {
      const internalModel = oml2dInstance.models[0].internalModel
      if (internalModel && internalModel.motionManager && internalModel.motionManager.expressionManager) {
        internalModel.motionManager.expressionManager.setExpression(expression)
      }
    } catch (e) {
      console.log('Expression error:', e)
    }
  },
  toggleDisplay: (show) => {
    if (!oml2dInstance) return
    // oh-my-live2d thường tạo thẻ div#oml2d ở thẳng ngoài body (đôi khi dùng shadow DOM)
    // Dùng opacity và pointerEvents an toàn hơn display: none (display: none dễ làm hỏng WebGL context/kích thước)
    const oml2dRoot = document.getElementById('oml2d') || document.querySelector('#oml2d-stage') || document.querySelector('.oml2d-stage')
    
    const applyVisibility = (el) => {
      el.style.transition = 'opacity 0.3s ease'
      el.style.opacity = show ? '1' : '0'
      el.style.pointerEvents = show ? 'auto' : 'none'
    }

    if (oml2dRoot) {
      applyVisibility(oml2dRoot)
    } else if (oml2dInstance.canvas) {
      // Dự phòng nếu không tìm thấy thẻ bọc
      let parent = oml2dInstance.canvas.parentElement
      if (parent && parent.tagName !== 'BODY') {
        let root = parent
        while (root.parentElement && root.parentElement.tagName !== 'BODY' && root.parentElement.tagName !== 'HTML') {
          if (root.host) { // Nếu dính shadow root
            root = root.host
          } else {
            root = root.parentElement
          }
        }
        if (root.style) applyVisibility(root)
      } else {
        applyVisibility(oml2dInstance.canvas)
      }
    }
  }
})

onMounted(() => {
  nextTick(() => {
    setTimeout(initLive2D, 500)
  })
})

onBeforeUnmount(() => {
  if (oml2dInstance && typeof oml2dInstance.unmount === 'function') {
    oml2dInstance.unmount()
  }
  oml2dInstance = null
})
</script>