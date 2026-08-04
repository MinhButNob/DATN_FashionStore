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

// ✅ Banner có "Loại link" = Danh mục và trỏ tới danh mục "Bộ Sưu Tập Mới"
// KHÔNG được hiển thị ở slider chính này (banner đầu trang chủ) — banner đó
// chỉ dùng làm ảnh bìa riêng cho mục "Bộ Sưu Tập Mới" ở phía dưới trang chủ
// (xem HomePage.vue -> newCollectionBanner). Vì vậy cần lọc nó ra khỏi danh
// sách banner của slider chính, tránh hiển thị trùng lặp ở cả 2 nơi.
const NEW_COLLECTION_CATEGORY_NAME = 'Bộ Sưu Tập Mới'
const normalizeText = (s) => (s || '').toString().trim().toLowerCase()

// ✅ Banner đánh dấu là banner Slogan cuối trang (xem AdminBanners.vue / HomePage.vue)
// cũng không được hiển thị ở slider chính, vì nó chỉ dùng riêng cho khu vực slogan
// trước Footer ở trang chủ.
const FOOTER_BANNER_MARKER = '__FOOTER_SLOGAN_BANNER__'

const banners = computed(() =>
  (bannerStore.activeBanners || []).filter(
    (b) =>
      !(b.linkType === 'CATEGORY' && normalizeText(b.categoryName) === normalizeText(NEW_COLLECTION_CATEGORY_NAME)) &&
      b.customUrl !== FOOTER_BANNER_MARKER
  )
)

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
  width: calc(100% - 32px);
  max-width: 1440px;
  margin: 16px auto;
  overflow: hidden;
  background: #0a0a1a;
  border-radius: 16px;
  /* ✅ Shadow tinh tế hơn, không quá nặng */
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
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
  /* ✅ Tỉ lệ 3:1 - khớp với kích thước banner khuyến nghị (vd 1800x600px) */
  aspect-ratio: 3 / 1;
  background: #0a0a1a;
}

/* ✅ Style cho ảnh - render mượt, không méo/nhòe */
.slide-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
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
  width: 48px;
  height: 48px;
  font-size: 1.8rem;
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
  left: 20px;
}

.arrow-right {
  right: 20px;
}

/* ✅ Responsive - giữ tỉ lệ 3:1 nhất quán mọi kích thước màn hình */
@media (max-width: 1024px) {
  .banner-slider {
    width: calc(100% - 24px);
    margin: 12px auto;
    border-radius: 14px;
  }
  .arrow {
    width: 38px;
    height: 38px;
    font-size: 1.4rem;
  }
  .arrow-left { left: 14px; }
  .arrow-right { right: 14px; }
}

@media (max-width: 768px) {
  .banner-slider {
    width: calc(100% - 16px);
    margin: 10px auto;
    border-radius: 12px;
  }
  .dots {
    bottom: 12px;
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
    width: 32px;
    height: 32px;
    font-size: 1.1rem;
  }
  .arrow-left { left: 8px; }
  .arrow-right { right: 8px; }
}

@media (max-width: 480px) {
  .banner-slider {
    width: 100%;
    margin: 0;
    border-radius: 0;
  }
  .arrow {
    width: 28px;
    height: 28px;
    font-size: 0.95rem;
  }
  .arrow-left { left: 6px; }
  .arrow-right { right: 6px; }
  .dots {
    bottom: 8px;
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
    transform: scale(1.03);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* ✅ Placeholder cho ảnh lỗi */
.slide-placeholder {
  animation: fadeIn 0.5s ease;
}
</style>