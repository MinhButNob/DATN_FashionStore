<!-- src/components/admin/dashboard/DashboardInventoryAlerts.vue -->
<template>
  <div class="card shadow-sm order-card">
    <div class="card-header">
      <h3 class="card-title text-danger"><i class="fas fa-exclamation-triangle mr-2"></i> Cảnh báo sắp hết hàng</h3>
      <button class="btn-link text-danger" @click="$router.push('/admin/products')">Nhập hàng <i class="fas fa-plus-circle ml-1"></i></button>
    </div>
    
    <div class="card-body p-0">
      <div v-if="loading" class="p-4 text-center">
        <div class="spinner-border text-danger" role="status"></div>
      </div>
      <div v-else-if="alerts.length === 0" class="p-4 text-center text-muted">
        Không có sản phẩm nào sắp hết hàng
      </div>
      <div v-else class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th class="text-center">Tồn kho</th>
              <th class="text-center">Định mức</th>
              <th class="text-center">Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in alerts" :key="item.productDetailId" class="alert-row">
              <td>
                <div class="product-info">
                  <div class="product-img">
                    <img v-if="item.imageUrl" :src="'http://localhost:8080/api/public/uploads/' + item.imageUrl" alt="img" class="img-fluid" style="border-radius: 8px; max-width: 40px; max-height: 40px;" />
                    <i v-else class="fas fa-tshirt"></i>
                  </div>
                  <div class="name-sku">
                    <span class="name">{{ item.productName }}</span>
                    <span class="sku">Size: {{ item.sizeName }} | Màu: {{ item.colorName }}</span>
                  </div>
                </div>
              </td>
              <td class="text-center font-weight-bold" :class="item.quantity === 0 ? 'text-danger' : 'text-warning'">{{ item.quantity }}</td>
              <td class="text-center text-muted">10</td>
              <td class="text-center">
                <span class="status-badge" :class="item.quantity === 0 ? 'status-out' : 'status-low'">
                  {{ item.quantity === 0 ? 'Hết hàng' : 'Sắp hết' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import DashboardService from '@/services/DashboardService'

const alerts = ref([])
const loading = ref(false)

const fetchAlerts = async () => {
  loading.value = true
  try {
    const res = await DashboardService.getInventoryAlerts(10, 5) // threshold 10, limit 5
    alerts.value = res.data
  } catch (error) {
    console.error('Lỗi khi tải cảnh báo kho:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAlerts()
})
</script>

<style scoped>
.order-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  overflow: hidden;
  height: 100%;
}

.card-header {
  padding: 20px 24px;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #ef4444 !important;
}

.btn-link {
  background: transparent;
  border: none;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  padding: 0;
}

.btn-link:hover {
  text-decoration: underline;
}

.table {
  width: 100%;
  margin-bottom: 0;
  border-collapse: collapse;
}

.table th {
  padding: 16px 24px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}

.table td {
  padding: 16px 24px;
  vertical-align: middle;
  border-bottom: 1px solid #f1f5f9;
  font-size: 14px;
}

.alert-row {
  transition: background-color 0.2s;
}

.alert-row:hover {
  background-color: #fef2f2;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-img {
  width: 40px;
  height: 40px;
  background: #f1f5f9;
  color: #94a3b8;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.name-sku {
  display: flex;
  flex-direction: column;
}

.name {
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}

.sku {
  font-size: 12px;
  color: #94a3b8;
}

.status-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}

.status-out { background: #fef2f2; color: #ef4444; }
.status-low { background: #fffbeb; color: #d97706; }
</style>
