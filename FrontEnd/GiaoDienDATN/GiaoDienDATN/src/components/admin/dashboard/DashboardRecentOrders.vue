<!-- src/components/admin/dashboard/DashboardRecentOrders.vue -->
<template>
  <div class="card shadow-sm order-card">
    <div class="card-header">
      <h3 class="card-title">Đơn hàng mới nhất</h3>
      <button class="btn-link" @click="$router.push('/admin/orders')">Xem toàn bộ <i class="fas fa-arrow-right ml-1"></i></button>
    </div>
    
    <div class="card-body p-0">
      <div v-if="loading" class="p-4 text-center">
        <div class="spinner-border text-primary" role="status"></div>
      </div>
      <div v-else-if="orders.length === 0" class="p-4 text-center text-muted">
        Chưa có đơn hàng nào
      </div>
      <div v-else class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Khách hàng</th>
              <th class="text-right">Tổng tiền</th>
              <th class="text-center">Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="order.orderId" class="order-row" @click="$router.push(`/admin/orders/${order.orderId}`)">
              <td class="font-weight-bold text-primary">#{{ order.orderId }}</td>
              <td>
                <div class="customer-info">
                  <div class="avatar">{{ order.customer?.customerName ? order.customer.customerName.charAt(0) : 'U' }}</div>
                  <div class="name-time">
                    <span class="name">{{ order.customer?.customerName || 'Khách vãng lai' }}</span>
                    <span class="time">{{ formatDate(order.createdAt) }}</span>
                  </div>
                </div>
              </td>
              <td class="text-right font-weight-bold text-success">{{ formatPrice(order.totalAmount) }}</td>
              <td class="text-center">
                <span class="status-badge" :class="getStatusClass(order.orderStatus)">{{ getStatusText(order.orderStatus) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  orders: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const router = useRouter()

const formatPrice = (price) => {
  if (!price && price !== 0) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    return new Intl.DateTimeFormat('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(date)
  } catch (e) {
    return dateString
  }
}

const getStatusClass = (status) => {
  switch (status) {
    case 'PENDING_PAYMENT':
    case 'CONFIRMED':
    case 'PROCESSING':
      return 'status-pending'
    case 'SHIPPING':
      return 'status-shipping'
    case 'DELIVERED':
    case 'COMPLETED':
      return 'status-completed'
    case 'CANCELLED':
    case 'REJECTED':
      return 'status-cancelled'
    default:
      return 'status-pending'
  }
}

const getStatusText = (status) => {
  const statusMap = {
    'PENDING_PAYMENT': 'Chờ thanh toán',
    'CONFIRMED': 'Đã xác nhận',
    'PROCESSING': 'Đang xử lý',
    'SHIPPING': 'Đang giao',
    'DELIVERED': 'Đã giao',
    'COMPLETED': 'Hoàn thành',
    'RETURN_REQUESTED': 'Yêu cầu hoàn hàng',
    'RETURN_APPROVED': 'Đã duyệt hoàn',
    'RETURN_RECEIVED': 'Đã nhận hoàn',
    'RETURN_PENDING': 'Đang thu hồi',
    'RETURNED': 'Đã hoàn hàng',
    'REJECTED': 'Từ chối hoàn',
    'CANCELLED': 'Đã hủy'
  }
  return statusMap[status] || status
}
</script>

<style scoped>
.order-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  overflow: hidden;
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
  color: #0f172a;
}

.btn-link {
  background: transparent;
  border: none;
  color: #3b82f6;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  padding: 0;
}

.btn-link:hover {
  color: #2563eb;
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
  color: #334155;
  font-size: 14px;
}

.order-row {
  transition: background-color 0.2s;
  cursor: pointer;
}

.order-row:hover {
  background-color: #f8fafc;
}

.customer-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 36px;
  height: 36px;
  background: #e0e7ff;
  color: #4f46e5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}

.name-time {
  display: flex;
  flex-direction: column;
}

.name {
  font-weight: 600;
  color: #1e293b;
}

.time {
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

.status-pending { background: #fffbeb; color: #b45309; }
.status-shipping { background: #eff6ff; color: #1d4ed8; }
.status-completed { background: #ecfdf5; color: #047857; }
.status-cancelled { background: #fef2f2; color: #b91c1c; }
</style>
