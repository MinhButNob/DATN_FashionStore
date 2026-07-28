<template>
  <section class="banner-slider" v-if="banners.length > 0">
    <div class="slider-track" :style="trackStyle">
      <div
        v-for="banner in banners"
        :key="banner.bannerId"
        class="slide"
        @click="navigate(banner)"
      >
        <img
          :src="getImageUrl(banner.imagePath)"
          :alt="banner.title || 'Banner'"
          class="slide-img"
          loading="eager"
          draggable="false"
          @error="handleImageError"
        />
        <!-- Đã xóa overlay chứa title và button -->
      </div>
    </div>

    <!-- Dots điều hướng -->
    <div class="dots" v-if="banners.length > 1">
      <button
        v-for="(b, i) in banners"
        :key="i"
        :class="['dot', { active: i === current }]"
        @click.stop="goTo(i)"
        :aria-label="`Chuyển đến banner ${i + 1}`"
      />
    </div>

    <!-- Nút điều hướng trái/phải -->
    <button 
      class="arrow arrow-left" 
      @click.stop="prev" 
      v-if="banners.length > 1"
      aria-label="Banner trước"
    >
      ‹
    </button>
    <button 
      class="arrow arrow-right" 
      @click.stop="next" 
      v-if="banners.length > 1"
      aria-label="Banner sau"
    >
      ›
    </button>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useBannerStore } from '../../stores/bannerStore'

const router = useRouter()
const bannerStore = useBannerStore()
const current = ref(0)
let timer = null
let isTransitioning = false

const banners = computed(() => bannerStore.activeBanners)

onMounted(async () => {
  await bannerStore.fetchActiveBanners()
  startAutoPlay()
})

onUnmounted(() => {
  clearInterval(timer)
})

function startAutoPlay() {
  clearInterval(timer)
  if (banners.value.length > 1) {
    timer = setInterval(() => {
      if (!isTransitioning) {
        next()
      }
    }, 5000) // Tăng thời gian lên 5s để người dùng có thời gian nhìn
  }
}

const trackStyle = computed(() => ({
  transform: `translateX(-${current.value * 100}%)`,
  transition: isTransitioning ? 'transform 0.6s cubic-bezier(0.4, 0, 0.2, 1)' : 'none' }))

function next() {
  if (banners.value.length === 0 || isTransitioning) return
  isTransitioning = true
  current.value = (current.value + 1) % banners.value.length
  setTimeout(() => {
    isTransitioning = false
  }, 600)
}

function prev() {
  if (banners.value.length === 0 || isTransitioning) return
  isTransitioning = true
  current.value = (current.value - 1 + banners.value.length) % banners.value.length
  setTimeout(() => {
    isTransitioning = false
  }, 600)
}

function goTo(i) {
  if (i === current.value || isTransitioning) return
  isTransitioning = true
  current.value = i
  setTimeout(() => {
    isTransitioning = false
  }, 600)
  startAutoPlay()
}

function navigate(banner) {
  const url = banner.resolvedUrl
  if (!url || url === '#') return
  if (url.startsWith('http')) {
    window.open(url, '_blank')
  } else {
    router.push(url)
  }
}

// ✅ Hàm getImageUrl - xử lý đúng đường dẫn
function getImageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  if (path.startsWith('/uploads/')) {
    return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/${path}`
  }
  if (path.startsWith('uploads/')) {
    return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/${path}`
  }
  return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/uploads/${path}`
}

// ✅ Xử lý lỗi load ảnh
function handleImageError(event) {
  console.error('❌ Banner image load error:', event.target.src)
  const img = event.target
  img.style.display = 'none'
  // Hiển thị placeholder
  const parent = img.parentElement
  if (parent) {
    const placeholder = parent.querySelector('.slide-placeholder')
    if (!placeholder) {
      const div = document.createElement('div')
      div.className = 'slide-placeholder'
      div.innerHTML = '🖼️'
      div.style.cssText = `
        width: 100%;
        height: 100%;
        min-height: 400px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #1a1a2e, #2d2d4e);
        color: #666;
        font-size: 80px;
      `
      parent.appendChild(div)
    }
  }
}
</script>

<style scoped>
.banner-slider {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #0a0a1a;
  /* ✅ Thêm hiệu ứng shadow nhẹ */
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.slider-track {
  display: flex;
  width: 100%;
  will-change: transform;
}

.slide {
  min-width: 100%;
  position: relative;
  cursor: pointer;
  overflow: hidden;
  /* ✅ Đảm bảo ảnh chiếm toàn bộ */
  aspect-ratio: 16 / 5.5;
  background: #0a0a1a;
}

/* ✅ Style cho ảnh - sắc nét hơn */
.slide-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  /* ✅ Các thuộc tính giúp ảnh sắc nét hơn */
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
  transform: translateZ(0);
  display: block;
  /* ✅ Filter nhẹ giúp ảnh sắc nét hơn */
  filter: contrast(1.02) saturate(1.02);
  /* ✅ Scale nhẹ để tránh viền trắng khi chuyển slide */
  scale: 1.002;
}

/* ✅ Hiệu ứng zoom nhẹ khi hover */
.slide:hover .slide-img {
  transform: scale(1.03);
  transition: transform 6s ease;
}

/* Dots điều hướng */
.dots {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 10;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 20px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 0;
  outline: none;
}

.dot:hover {
  background: rgba(255, 255, 255, 0.7);
  transform: scale(1.2);
}

.dot.active {
  background: #ffffff;
  width: 28px;
  border-radius: 6px;
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.2);
}

/* Nút điều hướng */
.arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  width: 52px;
  height: 52px;
  font-size: 2rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  color: #fff;
  z-index: 10;
  line-height: 1;
  font-weight: 300;
  padding: 0;
  user-select: none;
}

.arrow:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-50%) scale(1.1);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 0 30px rgba(255, 255, 255, 0.1);
}

.arrow:active {
  transform: translateY(-50%) scale(0.95);
}

.arrow-left {
  left: 24px;
}

.arrow-right {
  right: 24px;
}

/* ✅ Responsive */
@media (max-width: 1024px) {
  .slide {
    aspect-ratio: 16 / 5;
  }
  .arrow {
    width: 44px;
    height: 44px;
    font-size: 1.6rem;
  }
  .arrow-left { left: 16px; }
  .arrow-right { right: 16px; }
}

@media (max-width: 768px) {
  .slide {
    aspect-ratio: 16 / 4.5;
    min-height: 180px;
  }
  .slide-img {
    object-fit: cover;
  }
  .dots {
    bottom: 14px;
    gap: 6px;
    padding: 6px 12px;
  }
  .dot {
    width: 8px;
    height: 8px;
  }
  .dot.active {
    width: 20px;
  }
  .arrow {
    width: 36px;
    height: 36px;
    font-size: 1.2rem;
  }
  .arrow-left { left: 10px; }
  .arrow-right { right: 10px; }
}

@media (max-width: 480px) {
  .slide {
    aspect-ratio: 16 / 4;
    min-height: 140px;
  }
  .arrow {
    width: 30px;
    height: 30px;
    font-size: 1rem;
  }
  .arrow-left { left: 6px; }
  .arrow-right { right: 6px; }
  .dots {
    bottom: 10px;
    gap: 4px;
    padding: 4px 10px;
  }
  .dot {
    width: 6px;
    height: 6px;
  }
  .dot.active {
    width: 16px;
  }
}

/* ✅ Animation cho dots */
.dot {
  position: relative;
}

.dot.active::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.15);
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.5);
    opacity: 0;
  }
}

/* ✅ Hiệu ứng fade cho ảnh khi load */
.slide-img {
  animation: fadeIn 0.8s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(1.05);
  }
  to {
    opacity: 1;
    transform: scale(1.002);
  }
}

/* ✅ Placeholder cho ảnh lỗi */
.slide-placeholder {
  animation: fadeIn 0.5s ease;
}
</style>