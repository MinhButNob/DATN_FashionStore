<!-- src/components/admin/dashboard/DashboardKPIs.vue -->
<template>
  <div class="stats-grid">
    <StatsCard
      title="Doanh thu hôm nay"
      :value="formatPrice(summary?.todayRevenue || 0)"
      icon="fas fa-wallet"
      color="#10B981"
      :loading="loading"
    />
    <StatsCard
      title="Đơn hàng hôm nay"
      :value="summary?.todayOrders || 0"
      icon="fas fa-shopping-bag"
      color="#3B82F6"
      :loading="loading"
    />
    <StatsCard
      title="Đơn chờ xác nhận"
      :value="summary?.pendingOrders || 0"
      icon="fas fa-clock"
      color="#F59E0B"
      :loading="loading"
      :isClickable="true"
      @click="navigate('PENDING')"
    />
    <StatsCard
      title="Đơn đang giao"
      :value="summary?.shippingOrders || 0"
      icon="fas fa-truck"
      color="#6366F1"
      :loading="loading"
      :isClickable="true"
      @click="navigate('SHIPPING')"
    />
    <StatsCard
      title="Hoàn thành (Tháng)"
      :value="summary?.completedOrders || 0"
      icon="fas fa-check-circle"
      color="#10B981"
      :loading="loading"
    />
    <StatsCard
      title="Khách hàng mới"
      :value="summary?.newCustomers || 0"
      icon="fas fa-user-plus"
      color="#8B5CF6"
      :loading="loading"
    />
    <StatsCard
      title="Sản phẩm sắp hết"
      :value="summary?.outOfStockProducts || 0"
      icon="fas fa-exclamation-triangle"
      color="#EF4444"
      :loading="loading"
      :isDanger="summary?.outOfStockProducts > 0"
    />
    <StatsCard
      title="Yêu cầu hoàn hàng"
      :value="summary?.returnRequests || 0"
      icon="fas fa-undo"
      color="#EF4444"
      :loading="loading"
      :isClickable="true"
      :isDanger="summary?.returnRequests > 0"
      @click="navigateReturn()"
    />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import StatsCard from './StatsCard.vue'

defineProps({
  summary: {
    type: Object,
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const router = useRouter()

const formatPrice = (value) => {
  if (!value && value !== 0) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
}

const navigate = (status) => {
  router.push({ path: '/admin/orders', query: { status } })
}

const navigateReturn = () => {
  router.push({ path: '/admin/orders', query: { status: 'RETURN_REQUESTED' } })
}
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}
</style>
