<template>
  <div class="admin-dashboard">
    <h1 class="page-title">Dashboard</h1>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <StatsCard
        v-for="stat in stats"
        :key="stat.title"
        :title="stat.title"
        :value="stat.value"
        :change="stat.change"
        :icon="stat.icon"
        :color="stat.color"
      />
    </div>

    <!-- Charts -->
    <div class="charts-row">
      <div class="chart-card">
        <div class="chart-header">
          <h3>Doanh thu theo tháng</h3>
          <select v-model="selectedYear">
            <option value="2024">2024</option>
            <option value="2023">2023</option>
          </select>
        </div>
        <RevenueChart />
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <h3>Top sản phẩm bán chạy</h3>
        </div>
        <div class="top-products">
          <div
            v-for="(product, index) in topProducts"
            :key="product.id"
            class="top-product-item"
          >
            <div class="product-rank" :class="{ 'rank-1': index === 0 }">
              {{ index + 1 }}
            </div>
            <div class="product-info">
              <h4>{{ product.name }}</h4>
              <p>Đã bán: {{ product.sold }}</p>
            </div>
            <div class="product-price">{{ formatPrice(product.price) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Orders -->
    <div class="recent-orders">
      <div class="section-header">
        <h3>Đơn hàng gần đây</h3>
        <router-link to="/admin/orders" class="view-all">
          Xem tất cả <i class="fas fa-arrow-right"></i>
        </router-link>
      </div>

      <DataTable
        :columns="orderColumns"
        :data="recentOrders"
      >
        <template #cell-status="{ row }">
          <span :class="['badge', `badge-${getOrderStatusClass(row.status)}`]">
            {{ getOrderStatusText(row.status) }}
          </span>
        </template>

        <template #cell-actions="{ row }">
          <div class="action-buttons">
            <button class="action-btn view-btn" @click="viewOrder(row.id)">
              <i class="fas fa-eye"></i>
            </button>
          </div>
        </template>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useProductStore } from '../../stores/productStore'
import { formatPrice, getOrderStatusText, getOrderStatusClass } from '../../utils/helpers'
import StatsCard from '../../components/admin/StatsCard.vue'
import RevenueChart from '../../components/admin/RevenueChart.vue'
import DataTable from '../../components/admin/DataTable.vue'

const router = useRouter()
const productStore = useProductStore()

const selectedYear = ref('2024')

const stats = [
  {
    title: 'Tổng sản phẩm',
    value: productStore.products.length,
    change: '+12%',
    icon: 'fas fa-tshirt',
    color: '#e63946'
  },
  {
    title: 'Tổng đơn hàng',
    value: 156,
    change: '+8%',
    icon: 'fas fa-shopping-cart',
    color: '#28a745'
  },
  {
    title: 'Doanh thu',
    value: '45.6M₫',
    change: '+15%',
    icon: 'fas fa-dollar-sign',
    color: '#ffc107'
  },
  {
    title: 'Khách hàng',
    value: 892,
    change: '+25',
    icon: 'fas fa-users',
    color: '#17a2b8'
  }
]

const topProducts = computed(() => {
  return [...productStore.products]
    .sort((a, b) => b.sold - a.sold)
    .slice(0, 5)
})

const orderColumns = [
  { key: 'id', label: 'Mã đơn' },
  { key: 'customer', label: 'Khách hàng' },
  { key: 'total', label: 'Tổng tiền', format: (value) => formatPrice(value) },
  { key: 'date', label: 'Ngày đặt' },
  { key: 'status', label: 'Trạng thái' },
  { key: 'actions', label: 'Thao tác' }
]

const recentOrders = [
  { id: 'DH001', customer: 'Nguyễn Văn A', total: 398000, date: '15/03/2024', status: 'delivered' },
  { id: 'DH002', customer: 'Trần Thị B', total: 399000, date: '16/03/2024', status: 'processing' },
  { id: 'DH003', customer: 'Lê Văn C', total: 798000, date: '16/03/2024', status: 'shipped' },
  { id: 'DH004', customer: 'Phạm Thị D', total: 599000, date: '17/03/2024', status: 'pending' },
  { id: 'DH005', customer: 'Hoàng Văn E', total: 447000, date: '17/03/2024', status: 'cancelled' }
]

const viewOrder = (id) => {
  router.push(`/admin/orders/${id}`)
}
</script>

<style scoped>
.admin-dashboard {
  animation: fadeIn 0.3s;
}

.page-title {
  font-size: 28px;
  color: var(--dark-gray);
  margin-bottom: 30px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 25px;
  margin-bottom: 30px;
}

.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 25px;
  margin-bottom: 30px;
}

.chart-card {
  background: white;
  padding: 25px;
  border-radius: 15px;
  box-shadow: var(--shadow);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 {
  color: var(--dark-gray);
  font-size: 18px;
}

.chart-header select {
  padding: 8px 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  cursor: pointer;
}

.top-products {
  margin-top: 10px;
}

.top-product-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.top-product-item:last-child {
  border-bottom: none;
}

.product-rank {
  width: 30px;
  height: 30px;
  background: var(--light-gray);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--medium-gray);
}

.product-rank.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffed4e);
  color: var(--dark-gray);
}

.product-info {
  flex: 1;
}

.product-info h4 {
  font-size: 15px;
  color: var(--dark-gray);
  margin-bottom: 3px;
}

.product-info p {
  font-size: 13px;
  color: var(--medium-gray);
}

.product-price {
  font-weight: 700;
  color: var(--primary-red);
}

.recent-orders {
  background: white;
  padding: 25px;
  border-radius: 15px;
  box-shadow: var(--shadow);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  color: var(--dark-gray);
  font-size: 18px;
}

.view-all {
  color: var(--primary-red);
  text-decoration: none;
  font-weight: 500;
  transition: var(--transition);
  display: flex;
  align-items: center;
  gap: 5px;
}

.view-all:hover {
  gap: 10px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 35px;
  height: 35px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}

.view-btn {
  background: #17a2b8;
  color: white;
}

.view-btn:hover {
  background: #138496;
  transform: translateY(-2px);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1024px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .page-title {
    font-size: 24px;
  }
}
</style>