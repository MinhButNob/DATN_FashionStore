<template>
  <div class="staff-dashboard">
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

    <div class="workspace-grid">
      <!-- Công việc cần xử lý -->
      <section class="panel">
        <div class="panel-header">
          <h3>Công việc cần xử lý</h3>
          <router-link to="/staff/orders">Xem đơn hàng</router-link>
        </div>
        <div v-if="isLoadingTasks" class="loading-state">
          <i class="fas fa-spinner fa-spin"></i> Đang tải...
        </div>
        <div v-else class="task-list">
          <div v-for="task in tasks" :key="task.id" class="task-item">
            <div class="task-icon" :style="{ color: task.color, background: task.color + '18' }">
              <i :class="task.icon"></i>
            </div>
            <div>
              <h4>{{ task.title }}</h4>
              <p>{{ task.description }}</p>
            </div>
            <span class="task-count">{{ task.count }}</span>
          </div>
        </div>
      </section>

      <!-- Truy cập nhanh -->
      <section class="panel">
        <div class="panel-header">
          <h3>Truy cập nhanh</h3>
        </div>
        <div class="quick-grid">
          <router-link v-for="item in quickActions" :key="item.path" :to="item.path" class="quick-action">
            <i :class="item.icon"></i>
            <span>{{ item.name }}</span>
          </router-link>
        </div>
      </section>
    </div>

    <!-- Đơn hàng mới hôm nay -->
    <section class="panel">
      <div class="panel-header">
        <h3>Đơn hàng mới hôm nay</h3>
        <span v-if="!isLoadingOrders" class="order-count">{{ todayOrders.length }} đơn</span>
      </div>
      <div v-if="isLoadingOrders" class="loading-state">
        <i class="fas fa-spinner fa-spin"></i> Đang tải...
      </div>
      <div v-else-if="todayOrders.length === 0" class="empty-state">
        <i class="fas fa-inbox"></i>
        <p>Chưa có đơn hàng nào hôm nay</p>
      </div>
      <div v-else class="table-container">
        <table>
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Khách hàng</th>
              <th>Tổng tiền</th>
              <th>Trạng thái</th>
              <th>Nhân viên</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in todayOrders" :key="order.id">
              <td><strong>{{ order.id }}</strong></td>
              <td>{{ order.customer }}</td>
              <td>{{ order.total }}</td>
              <td><span :class="['badge', order.badge]">{{ order.status }}</span></td>
              <td>{{ order.staff }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api/axios.js'
import { formatPrice } from '../../utils/helpers.js'
import StatsCard from '../../components/admin/StatsCard.vue'

// ── State ──────────────────────────────────────────────────────────────────
const isLoadingTasks  = ref(true)
const isLoadingOrders = ref(true)

const stats = ref([
  { title: 'Doanh thu hôm nay', value: '–',  change: '',    icon: 'fas fa-coins',         color: '#e63946' },
  { title: 'Đơn tại quầy',      value: '–',  change: '',    icon: 'fas fa-cash-register',  color: '#28a745' },
  { title: 'Đơn cần xử lý',     value: '–',  change: '',    icon: 'fas fa-box-open',       color: '#ffc107' },
  { title: 'Chat đang chờ',      value: 6,    change: '+3',  icon: 'fas fa-comments',       color: '#17a2b8' },
])

const tasks = ref([
  { id: 1, title: 'Xác nhận đơn online',   description: 'Đơn mới chưa được duyệt',          count: 0, icon: 'fas fa-clipboard-check', color: '#e63946' },
  { id: 2, title: 'Yêu cầu đổi trả',       description: 'Cần kiểm tra tình trạng sản phẩm', count: 0, icon: 'fas fa-undo-alt',         color: '#ffc107' },
  { id: 3, title: 'Tin nhắn khách hàng',   description: 'Cuộc trò chuyện chưa phản hồi',    count: 6, icon: 'fas fa-comment-dots',      color: '#17a2b8' },
])

const todayOrders = ref([])

const quickActions = [
  { name: 'Tạo hóa đơn',        path: '/staff/pos',       icon: 'fas fa-plus-circle' },
  { name: 'Tra cứu đơn',        path: '/staff/orders',    icon: 'fas fa-search'      },
  { name: 'Thêm khách',         path: '/staff/customers', icon: 'fas fa-user-plus'   },
  { name: 'Tiếp nhận đổi trả',  path: '/staff/returns',   icon: 'fas fa-receipt'     },
]

// ── Helpers ────────────────────────────────────────────────────────────────
const statusBadge = (s) => {
  const map = {
    COMPLETED:       { label: 'Hoàn tất',        badge: 'badge-success' },
    DELIVERED:       { label: 'Đã giao',          badge: 'badge-success' },
    PAID:            { label: 'Đã thanh toán',    badge: 'badge-success' },
    PROCESSING:      { label: 'Đang xử lý',       badge: 'badge-info'    },
    CONFIRMED:       { label: 'Đã xác nhận',      badge: 'badge-info'    },
    PENDING:         { label: 'Chờ xác nhận',     badge: 'badge-warning' },
    PENDING_PAYMENT: { label: 'Chờ thanh toán',   badge: 'badge-warning' },
    SHIPPING:        { label: 'Đang giao',         badge: 'badge-primary' },
    CANCELLED:       { label: 'Đã hủy',            badge: 'badge-danger'  },
    RETURNED:        { label: 'Đổi trả',           badge: 'badge-danger'  },
    REFUNDED:        { label: 'Hoàn tiền',         badge: 'badge-danger'  },
  }
  return map[s?.toUpperCase()] ?? { label: s ?? '–', badge: 'badge-secondary' }
}

const today = () => new Date().toISOString().split('T')[0]

// ── API calls ──────────────────────────────────────────────────────────────

// 1. Đơn cần xử lý (tasks) từ /api/admin/orders/status/counts
const loadOrderCounts = async () => {
  try {
    const res = await api.get('/admin/orders/status/counts')
    const d = res.data
    const needAction = (d.PENDING_PAYMENT ?? 0) + (d.CONFIRMED ?? 0) + (d.PROCESSING ?? 0)
    stats.value[2].value  = needAction
    tasks.value[0].count  = (d.PENDING_PAYMENT ?? 0) + (d.CONFIRMED ?? 0)
    tasks.value[1].count  = (d.RETURN_REQUESTED ?? 0) + (d.RETURN_PENDING ?? 0) + (d.RETURN_APPROVED ?? 0)
  } catch (e) {
    console.error('Lỗi load order counts:', e)
    stats.value[2].value = 0
  } finally {
    isLoadingTasks.value = false
  }
}

// 2. Đơn hôm nay từ /api/pos/invoices/search
//    → tự tính doanh thu + số đơn tại quầy từ dữ liệu trả về
//    (không dùng /statistics vì query đó chỉ tính COMPLETED, bỏ sót PAID)
const loadTodayOrders = async () => {
  try {
    const t = today()
    const res = await api.post('/pos/invoices/search', {
      startDate: t,
      endDate:   t,
      page: 0,
      size: 50, // lấy nhiều để tính tổng doanh thu chính xác
    })

    const content = res.data?.data?.content ?? res.data?.content ?? []

    // Tính tổng doanh thu = tổng tất cả đơn không bị hủy/hoàn tiền
    const excludedStatus = ['CANCELLED', 'REFUNDED', 'RETURNED']
    let totalRevenue = 0
    content.forEach(o => {
      if (!excludedStatus.includes(o.orderStatus?.toUpperCase())) {
        totalRevenue += Number(o.total ?? 0)
      }
    })

    stats.value[0].value  = formatPrice(totalRevenue)
    stats.value[0].change = `${content.length} đơn hôm nay`
    stats.value[1].value  = content.length

    // Chỉ hiện 10 đơn mới nhất trong bảng
    todayOrders.value = content.slice(0, 10).map(o => {
      const { label, badge } = statusBadge(o.orderStatus)
      return {
        id:       o.invoiceNumber ?? o.posOrderId,
        customer: o.customerName  ?? 'Khách lẻ',
        total:    formatPrice(o.total ?? 0),
        status:   label,
        badge,
        staff:    o.cashierName   ?? '–',
      }
    })
  } catch (e) {
    console.error('Lỗi load today orders:', e)
    stats.value[0].value = '0 ₫'
    stats.value[1].value = 0
  } finally {
    isLoadingOrders.value = false
  }
}

onMounted(() => {
  loadOrderCounts()
  loadTodayOrders()
})
</script>

<style scoped>
.staff-dashboard {
  animation: fadeIn 0.3s;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 25px;
  margin-bottom: 30px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 25px;
  margin-bottom: 30px;
}

.panel {
  background: white;
  padding: 25px;
  border-radius: 15px;
  box-shadow: var(--shadow);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.panel-header h3 {
  color: var(--dark-gray);
  font-size: 18px;
}

.panel-header a {
  color: var(--primary-red);
  text-decoration: none;
  font-weight: 500;
}

.order-count {
  font-size: 13px;
  color: var(--medium-gray);
  background: var(--light-gray);
  padding: 4px 10px;
  border-radius: 20px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 30px 0;
  color: var(--medium-gray);
  font-size: 14px;
}

.empty-state i {
  font-size: 32px;
  opacity: 0.4;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.task-item {
  display: grid;
  grid-template-columns: 46px 1fr auto;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
}

.task-icon {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.task-item h4 {
  color: var(--dark-gray);
  font-size: 15px;
  margin-bottom: 4px;
}

.task-item p {
  color: var(--medium-gray);
  font-size: 13px;
}

.task-count {
  color: var(--primary-red);
  font-size: 22px;
  font-weight: 700;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.quick-action {
  min-height: 96px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--dark-gray);
  text-decoration: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: var(--transition);
}

.quick-action i {
  color: var(--primary-red);
  font-size: 24px;
}

.quick-action:hover {
  border-color: var(--primary-red);
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}

.table-container { overflow-x: auto; }

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  text-align: left;
  padding: 12px 16px;
  font-size: 13px;
  color: var(--medium-gray);
  font-weight: 600;
  border-bottom: 2px solid var(--border-color);
  white-space: nowrap;
}

td {
  padding: 14px 16px;
  font-size: 14px;
  color: var(--dark-gray);
  border-bottom: 1px solid var(--border-color);
}

tr:last-child td { border-bottom: none; }
tr:hover td { background: var(--light-gray); }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0);    }
}

@media (max-width: 1024px) {
  .workspace-grid { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .quick-grid { grid-template-columns: 1fr; }
}
</style>