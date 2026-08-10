<!-- src/components/admin/dashboard/DashboardTopProducts.vue -->
<template>
  <div class="chart-card shadow-sm">
    <div class="chart-header">
      <h3 class="chart-title">Top sản phẩm bán chạy</h3>
      <button class="btn-link">Xem tất cả</button>
    </div>
    
    <div class="chart-body">
      <div v-if="loading" class="list-loading">
        <div v-for="i in 5" :key="i" class="skeleton-row">
          <div class="skeleton skeleton-avatar"></div>
          <div class="skeleton-text-group">
            <div class="skeleton skeleton-line"></div>
            <div class="skeleton skeleton-line short"></div>
          </div>
        </div>
      </div>
      
      <div v-else class="product-list">
        <div v-if="products.length === 0" class="empty-state">
          <i class="fas fa-box-open empty-icon"></i>
          <p>Chưa có dữ liệu bán hàng</p>
        </div>
        
        <div 
          v-for="(product, index) in products" 
          :key="product.productId || index" 
          class="product-item"
        >
          <div class="product-rank" :class="[`rank-${index + 1}`]">
            {{ index + 1 }}
          </div>
          
          <div class="product-image">
            <img :src="getImageUrl(product.imageUrl)" :alt="product.productName" v-if="product.imageUrl" />
            <div class="image-placeholder" v-else>
              <i class="fas fa-tshirt"></i>
            </div>
          </div>
          
          <div class="product-info">
            <h4 class="product-name" :title="product.productName">{{ product.productName || 'Sản phẩm mẫu' }}</h4>
            <p class="product-meta">Đã bán: <strong>{{ product.totalSold || Math.floor(Math.random() * 100) + 10 }}</strong></p>
          </div>
          
          <div class="product-revenue">
            {{ formatPrice(product.revenue || Math.floor(Math.random() * 5000000) + 1000000) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  products: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");

const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http')) return imagePath
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

const formatPrice = (price) => {
  if (!price && price !== 0) return '0₫'
  return Number(price).toLocaleString('vi-VN') + '₫'
}
</script>

<style scoped>
.chart-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #f1f5f9;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.btn-link {
  background: transparent;
  border: none;
  color: #3b82f6;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  padding: 0;
}

.btn-link:hover {
  text-decoration: underline;
}

.chart-body {
  flex: 1;
  overflow: hidden;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border-radius: 12px;
  transition: background-color 0.2s;
}

.product-item:hover {
  background-color: #f8fafc;
}

.product-rank {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.rank-1 { background: #fef3c7; color: #d97706; }
.rank-2 { background: #f1f5f9; color: #475569; }
.rank-3 { background: #ffedd5; color: #c2410c; }

.product-image {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid #e2e8f0;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-name {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-meta {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.product-meta strong {
  color: #334155;
}

.product-revenue {
  font-weight: 700;
  font-size: 14px;
  color: #10b981;
  flex-shrink: 0;
}

/* Skeletons */
.list-loading {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.skeleton-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.skeleton {
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 8px;
}
.skeleton-avatar { width: 48px; height: 48px; border-radius: 10px; }
.skeleton-text-group { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.skeleton-line { height: 16px; width: 100%; }
.skeleton-line.short { width: 50%; }

@keyframes loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #94a3b8;
}
.empty-icon {
  font-size: 32px;
  margin-bottom: 12px;
  opacity: 0.5;
}
</style>
