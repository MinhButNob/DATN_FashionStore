<template>
  <div class="haru-backup">
    <div class="haru-character" @click="playAnimation">
      <!-- Hair -->
      <div class="hair"></div>
      
      <!-- Face -->
      <div class="face">
        <!-- Eyes -->
        <div class="eyes">
          <div class="eye left" :class="{ blink: isBlinking }"></div>
          <div class="eye right" :class="{ blink: isBlinking }"></div>
        </div>
        
        <!-- Mouth -->
        <div class="mouth" :class="{ talk: isTalking }"></div>
        
        <!-- Blush -->
        <div class="blush left"></div>
        <div class="blush right"></div>
      </div>
      
      <!-- Body -->
      <div class="body">
        <!-- Shirt -->
        <div class="shirt"></div>
        
        <!-- Arms -->
        <div class="arm left" :class="{ wave: isWaving }"></div>
        <div class="arm right" :class="{ wave: isWaving }"></div>
      </div>
      
      <!-- Floating particles -->
      <div v-for="n in 5" :key="n" class="particle" :style="getParticleStyle(n)"></div>
    </div>
    
    <!-- Interaction hint -->
    <div class="interaction-hint">
      <i class="fas fa-hand-pointer"></i>
      Click để tương tác với Haru
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const isBlinking = ref(false)
const isTalking = ref(false)
const isWaving = ref(false)

let blinkInterval = null
let idleInterval = null

const playAnimation = () => {
  // Wave animation
  isWaving.value = true
  setTimeout(() => {
    isWaving.value = false
  }, 1000)
  
  // Talk animation
  isTalking.value = true
  setTimeout(() => {
    isTalking.value = false
  }, 2000)
}

const startIdleAnimations = () => {
  // Blinking animation
  blinkInterval = setInterval(() => {
    isBlinking.value = true
    setTimeout(() => {
      isBlinking.value = false
    }, 150)
  }, 3000 + Math.random() * 2000)
  
  // Random idle movements
  idleInterval = setInterval(() => {
    if (Math.random() < 0.3) {
      playAnimation()
    }
  }, 8000)
}

const getParticleStyle = (n) => {
  const delay = n * 0.5
  const duration = 3 + Math.random() * 2
  return {
    '--delay': `${delay}s`,
    '--duration': `${duration}s`,
    left: `${20 + Math.random() * 60}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

onMounted(() => {
  startIdleAnimations()
})

onBeforeUnmount(() => {
  if (blinkInterval) clearInterval(blinkInterval)
  if (idleInterval) clearInterval(idleInterval)
})
</script>

<style scoped>
.haru-backup {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
  border-radius: 15px;
}

.haru-character {
  position: relative;
  cursor: pointer;
  transition: transform 0.3s;
}

.haru-character:hover {
  transform: scale(1.05);
}

/* Hair */
.hair {
  width: 80px;
  height: 60px;
  background: linear-gradient(135deg, #4a5568, #2d3748);
  border-radius: 50px 50px 30px 30px;
  position: relative;
  z-index: 1;
  margin-bottom: -10px;
}

.hair::before {
  content: '';
  position: absolute;
  top: -5px;
  left: 10px;
  width: 20px;
  height: 25px;
  background: linear-gradient(135deg, #4a5568, #2d3748);
  border-radius: 15px;
  transform: rotate(-20deg);
}

.hair::after {
  content: '';
  position: absolute;
  top: -5px;
  right: 10px;
  width: 20px;
  height: 25px;
  background: linear-gradient(135deg, #4a5568, #2d3748);
  border-radius: 15px;
  transform: rotate(20deg);
}

/* Face */
.face {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #ffd6cc, #ffb3ba);
  border-radius: 50%;
  position: relative;
  z-index: 2;
}

/* Eyes */
.eyes {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 20px;
}

.eye {
  width: 8px;
  height: 8px;
  background: #2d3748;
  border-radius: 50%;
  transition: height 0.1s;
}

.eye.blink {
  height: 2px;
}

/* Mouth */
.mouth {
  position: absolute;
  top: 45px;
  left: 50%;
  transform: translateX(-50%);
  width: 12px;
  height: 6px;
  border: 2px solid #e53e3e;
  border-top: none;
  border-radius: 0 0 12px 12px;
  transition: all 0.3s;
}

.mouth.talk {
  height: 10px;
  width: 8px;
  border-radius: 50%;
  background: #e53e3e;
  border: none;
}

/* Blush */
.blush {
  position: absolute;
  top: 35px;
  width: 10px;
  height: 8px;
  background: rgba(255, 182, 193, 0.6);
  border-radius: 50%;
}

.blush.left {
  left: 5px;
}

.blush.right {
  right: 5px;
}

/* Body */
.body {
  position: relative;
  z-index: 1;
}

.shirt {
  width: 60px;
  height: 50px;
  background: linear-gradient(135deg, #38a169, #2f855a);
  border-radius: 15px 15px 5px 5px;
  position: relative;
}

.shirt::before {
  content: '💖';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 16px;
}

/* Arms */
.arm {
  position: absolute;
  top: 10px;
  width: 15px;
  height: 35px;
  background: linear-gradient(135deg, #ffd6cc, #ffb3ba);
  border-radius: 10px;
  transform-origin: top;
  transition: transform 0.5s;
}

.arm.left {
  left: -10px;
  transform: rotate(-15deg);
}

.arm.right {
  right: -10px;
  transform: rotate(15deg);
}

.arm.wave.left {
  animation: wave-left 1s ease-in-out;
}

.arm.wave.right {
  animation: wave-right 1s ease-in-out;
}

/* Animations */
@keyframes wave-left {
  0%, 100% { transform: rotate(-15deg); }
  25% { transform: rotate(-45deg); }
  50% { transform: rotate(-60deg); }
  75% { transform: rotate(-30deg); }
}

@keyframes wave-right {
  0%, 100% { transform: rotate(15deg); }
  25% { transform: rotate(45deg); }
  50% { transform: rotate(60deg); }
  75% { transform: rotate(30deg); }
}

/* Floating particles */
.particle {
  position: absolute;
  width: 6px;
  height: 6px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 50%;
  animation: float 4s infinite ease-in-out;
  pointer-events: none;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  50% {
    transform: translateY(-60px) rotate(180deg);
    opacity: 0.7;
  }
}

/* Interaction hint */
.interaction-hint {
  position: absolute;
  bottom: 15px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  text-align: center;
  display: flex;
  align-items: center;
  gap: 5px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* Responsive */
@media (max-width: 480px) {
  .haru-character {
    transform: scale(0.8);
  }
  
  .interaction-hint {
    font-size: 10px;
  }
}
</style>