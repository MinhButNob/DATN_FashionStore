<template>
  <div class="banner-slider">
    <div 
      v-for="(slide, index) in slides" 
      :key="index"
      class="slide"
      :class="{ active: currentSlide === index }"
      :style="{ background: slide.background }"
    >
      <div class="slide-content">
        <h2>{{ slide.title }}</h2>
        <p>{{ slide.description }}</p>
        <button class="btn btn-primary" @click="handleSlideClick(slide)">
          {{ slide.buttonText }} <i class="fas fa-arrow-right"></i>
        </button>
      </div>
      <div class="slide-image">
        <i :class="['fas', slide.icon]"></i>
      </div>
    </div>

    <div class="slider-dots">
      <span 
        v-for="(slide, index) in slides" 
        :key="index"
        class="dot"
        :class="{ active: currentSlide === index }"
        @click="goToSlide(index)"
      ></span>
    </div>

    <button class="slider-arrow prev" @click="prevSlide">
      <i class="fas fa-chevron-left"></i>
    </button>
    <button class="slider-arrow next" @click="nextSlide">
      <i class="fas fa-chevron-right"></i>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const currentSlide = ref(0)
let interval = null

const slides = [
  {
    title: 'SUMMER SALE 2024',
    description: 'Giảm giá lên đến 50% cho tất cả sản phẩm mùa hè',
    buttonText: 'Mua ngay',
    icon: 'fa-tshirt',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    link: '/shop?tag=sale'
  },
  {
    title: 'NEW COLLECTION',
    description: 'Bộ sưu tập thu đông 2024 với những thiết kế mới nhất',
    buttonText: 'Khám phá',
    icon: 'fa-vest',
    background: 'linear-gradient(135deg, #e63946 0%, #c82333 100%)',
    link: '/shop?tag=new'
  },
  {
    title: 'ACCESSORIES',
    description: 'Phụ kiện thời trang cao cấp - Hoàn hảo mọi phong cách',
    buttonText: 'Xem ngay',
    icon: 'fa-hat-cowboy',
    background: 'linear-gradient(135deg, #2c3e50 0%, #3498db 100%)',
    link: '/shop?category=accessories'
  }
]

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % slides.length
}

const prevSlide = () => {
  currentSlide.value = (currentSlide.value - 1 + slides.length) % slides.length
}

const goToSlide = (index) => {
  currentSlide.value = index
}

const handleSlideClick = (slide) => {
  router.push(slide.link)
}

// Auto slide
const startAutoSlide = () => {
  interval = setInterval(nextSlide, 5000)
}

const stopAutoSlide = () => {
  if (interval) {
    clearInterval(interval)
  }
}

onMounted(() => {
  startAutoSlide()
})

onUnmounted(() => {
  stopAutoSlide()
})
</script>

<style scoped>
.banner-slider {
  margin: 20px 5%;
  border-radius: 20px;
  overflow: hidden;
  position: relative;
  height: 400px;
  box-shadow: var(--shadow);
}

.slide {
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.5s ease-in-out;
  display: flex;
  align-items: center;
  padding: 0 50px;
  color: white;
  pointer-events: none;
}

.slide.active {
  opacity: 1;
  pointer-events: all;
}

.slide-content {
  flex: 1;
  z-index: 2;
}

.slide-content h2 {
  font-size: 48px;
  margin-bottom: 20px;
  animation: slideInLeft 1s;
}

.slide-content p {
  font-size: 18px;
  margin-bottom: 30px;
  opacity: 0.9;
  animation: slideInLeft 1s 0.2s backwards;
}

.slide-content button {
  background: white;
  color: var(--dark-gray);
  border: none;
  padding: 15px 40px;
  border-radius: 30px;
  font-weight: bold;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
  animation: slideInLeft 1s 0.4s backwards;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.slide-content button:hover {
  background: var(--primary-red);
  color: white;
  transform: translateY(-3px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.2);
}

.slide-image {
  flex: 1;
  text-align: center;
  z-index: 2;
}

.slide-image i {
  font-size: 250px;
  color: rgba(255,255,255,0.3);
  animation: float 3s ease-in-out infinite;
}

/* Slider Dots */
.slider-dots {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 3;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  cursor: pointer;
  transition: all 0.3s;
}

.dot:hover {
  background: rgba(255,255,255,0.8);
}

.dot.active {
  background: white;
  transform: scale(1.3);
}

/* Slider Arrows */
.slider-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: rgba(255,255,255,0.3);
  border: none;
  border-radius: 50%;
  color: white;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(5px);
}

.slider-arrow:hover {
  background: rgba(255,255,255,0.5);
  transform: translateY(-50%) scale(1.1);
}

.slider-arrow.prev {
  left: 20px;
}

.slider-arrow.next {
  right: 20px;
}

/* Animations */
@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-50px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .banner-slider {
    height: 500px;
    margin: 10px 3%;
  }

  .slide {
    flex-direction: column;
    text-align: center;
    padding: 20px;
  }

  .slide-content h2 {
    font-size: 32px;
  }

  .slide-content p {
    font-size: 16px;
  }

  .slide-image i {
    font-size: 150px;
  }

  .slider-arrow {
    width: 35px;
    height: 35px;
    font-size: 14px;
  }

  .slider-arrow.prev {
    left: 10px;
  }

  .slider-arrow.next {
    right: 10px;
  }
}

@media (max-width: 480px) {
  .banner-slider {
    height: 450px;
  }

  .slide-content h2 {
    font-size: 24px;
  }

  .slide-content p {
    font-size: 14px;
  }

  .slide-content button {
    padding: 12px 30px;
    font-size: 14px;
  }

  .slide-image i {
    font-size: 100px;
  }

  .slider-dots {
    bottom: 10px;
  }

  .dot {
    width: 8px;
    height: 8px;
  }
}
</style>