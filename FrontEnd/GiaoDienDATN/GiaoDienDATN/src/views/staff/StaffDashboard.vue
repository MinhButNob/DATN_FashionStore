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

        <div class="task-list">
          <div 
            v-for="task in tasks" 
            :key="task.id" 
            class="task-item" 
            @click="navigateTo(task.path)"
          >
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
<<<<<<< HEAD
        <span v-if="!isLoadingOrders" class="order-count">{{ todayOrders.length }} đơn</span>
=======
        <router-link to="/staff/orders">Xem tất cả</router-link>
>>>>>>> origin/feature-ChatDungChuiNV
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

    <!-- ===== THÔNG BÁO TIN NHẮN MỚI ===== -->
    <div v-if="showNotification" class="chat-notification" @click="goToChat">
      <div class="notification-content">
        <div class="notification-icon">
          <i class="fas fa-comment-dots"></i>
        </div>
        <div class="notification-text">
          <strong>Tin nhắn mới</strong>
          <p>{{ notificationMessage }}</p>
        </div>
        <button class="notification-close" @click.stop="dismissNotification">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="notification-progress"></div>
    </div>
  </div>
</template>

<script setup>

import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useChatStore } from '../../stores/chatStore';
import { useNotificationNhanVien } from '../../composables/useNotificationNhanVien';
import StatsCard from '../../components/admin/StatsCard.vue';

const router = useRouter();
const chatStore = useChatStore();
const { unreadCount, conversations, messages } = storeToRefs(chatStore);
const { successWithIcon, showApiError } = useNotificationNhanVien();

const stats = ref([
  { title: "Doanh thu hôm nay", value: "8.45M₫", change: "+12%", icon: "fas fa-coins", color: "#e63946" },
  { title: "Đơn tại quầy", value: "24", change: "+5", icon: "fas fa-cash-register", color: "#28a745" },
  { title: "Đơn cần xử lý", value: "9", change: "-2", icon: "fas fa-box-open", color: "#ffc107" },
  { title: "Tin nhắn chưa đọc", value: "0", change: "+0", icon: "fas fa-comments", color: "#17a2b8" },
]);

const showNotification = ref(false);
const notificationMessage = ref('');
let notificationTimer = null;

const tasks = ref([
  { 
    id: 1, 
    title: "Xác nhận đơn online", 
    description: "Đơn mới chưa được duyệt", 
    count: 5, 
    icon: "fas fa-clipboard-check", 
    color: "#e63946",
    path: "/staff/orders"
  },
  { 
    id: 2, 
    title: "Yêu cầu đổi trả", 
    description: "Cần kiểm tra tình trạng sản phẩm", 
    count: 3, 
    icon: "fas fa-undo-alt", 
    color: "#ffc107",
    path: "/staff/returns"
  },
  { 
    id: 3, 
    title: "Tin nhắn khách hàng", 
    description: "Cuộc trò chuyện chưa phản hồi", 
    count: 0, 
    icon: "fas fa-comment-dots", 
    color: "#17a2b8",
    path: "/staff/chat"
  },
]);

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
const todayOrders = [
  { id: "POS001", customer: "Khách lẻ", total: "698.000₫", status: "Hoàn tất", badge: "badge-success", staff: "Staff" },
  { id: "DH1024", customer: "Nguyễn Minh Anh", total: "1.248.000₫", status: "Đang xử lý", badge: "badge-info", staff: "Staff" },
  { id: "DH1025", customer: "Trần Quốc Bảo", total: "399.000₫", status: "Chờ xác nhận", badge: "badge-warning", staff: "Staff" },
];

const navigateTo = (path) => {
  if (path) {
    router.push(path);
  }
};

const goToChat = () => {
  router.push('/staff/chat');
  dismissNotification();
};

const dismissNotification = () => {
  showNotification.value = false;
  if (notificationTimer) {
    clearTimeout(notificationTimer);
    notificationTimer = null;
  }
};

const showChatNotification = (message) => {
  notificationMessage.value = message || 'Có tin nhắn mới từ khách hàng';
  showNotification.value = true;
  
  if (notificationTimer) {
    clearTimeout(notificationTimer);
  }
  notificationTimer = setTimeout(() => {
    showNotification.value = false;
  }, 5000);
};

const updateUnreadCount = () => {
  const count = unreadCount.value || 0;
  stats.value[3].value = count.toString();
  stats.value[3].change = count > 0 ? `+${count}` : '+0';
  
  const chatTask = tasks.value.find(t => t.id === 3);
  if (chatTask) {
    chatTask.count = count;
  }
};

// ===== WATCHES =====
watch(
  () => chatStore.messages,
  (newMessages, oldMessages) => {
    if (newMessages.length > oldMessages.length) {
      const lastMessage = newMessages[newMessages.length - 1];
      if (lastMessage && lastMessage.userRole === 'CUSTOMER' && !lastMessage.isFromUser) {
        const customerName = chatStore.selectedConversation?.customerName || 'Khách hàng';
        showChatNotification(`${customerName}: ${lastMessage.messageContent}`);
      }
    }
  },
  { deep: true }
);

watch(unreadCount, (newCount) => {
  updateUnreadCount();
  if (newCount > 0) {
    document.title = `(${newCount}) FASHION - Staff`;
  } else {
    document.title = 'FASHION - Staff';
  }
});

// ===== LOAD UNREAD COUNT =====
const loadUnreadCount = async () => {
  try {
    await chatStore.updateUnreadCount();
    updateUnreadCount();
  } catch (err) {
    console.error('Error loading unread count:', err);
  }
};

// ===== WEBSOCKET =====
const initWebSocket = () => {
  try {
    chatStore.initWebSocket();
  } catch (err) {
    console.error('Error initializing WebSocket:', err);
  }
};

let interval = null;
let wsInterval = null;

// ===== LIFECYCLE =====
onMounted(async () => {
  initWebSocket();
  await loadUnreadCount();
  
  interval = setInterval(loadUnreadCount, 30000);
  wsInterval = setInterval(updateUnreadCount, 5000);
  
  successWithIcon('Chào mừng bạn đến với Dashboard nhân viên', '👋');
});

onUnmounted(() => {
  if (interval) {
    clearInterval(interval);
  }
  if (wsInterval) {
    clearInterval(wsInterval);
  }
  if (notificationTimer) {
    clearTimeout(notificationTimer);
  }
  chatStore.resetState();
});
</script>

<style scoped>
.staff-dashboard {
  animation: fadeIn 0.3s;
  position: relative;
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
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.panel-header h3 {
  color: #1a1a2e;
  font-size: 18px;
  margin: 0;
}

.panel-header a {
  color: #e63946;
  text-decoration: none;
  font-weight: 500;
  font-size: 14px;
}

.panel-header a:hover {
  text-decoration: underline;
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
  border: 1px solid #eee;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.task-item:hover {
  background: #fafafa;
  border-color: #ddd;
  transform: translateX(4px);
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
  color: #1a1a2e;
  font-size: 15px;
  margin: 0 0 4px 0;
}

.task-item p {
  color: #999;
  font-size: 13px;
  margin: 0;
}

.task-count {
  color: #e63946;
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
  border: 1px solid #eee;
  border-radius: 10px;
  color: #1a1a2e;
  text-decoration: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: all 0.2s;
}

.quick-action i {
  color: #e63946;
  font-size: 24px;
}

.quick-action:hover {
  border-color: #e63946;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(230, 57, 70, 0.15);
}

.table-container {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

table th,
table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

table th {
  color: #666;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

table td {
  font-size: 14px;
}

.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.badge-success {
  background: #d4edda;
  color: #155724;
}

.badge-info {
  background: #cce5ff;
  color: #004085;
}

.badge-warning {
  background: #fff3cd;
  color: #856404;
}

/* ===== THÔNG BÁO CHAT ===== */
.chat-notification {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  max-width: 450px;
  width: 90%;
  z-index: 9999;
  cursor: pointer;
  animation: slideUpNoti 0.4s ease;
  overflow: hidden;
  border-left: 4px solid #e63946;
}

.chat-notification:hover {
  transform: translateX(-50%) scale(1.02);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
}

.notification-content {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  gap: 14px;
}

.notification-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #fce8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notification-icon i {
  font-size: 20px;
  color: #e63946;
}

.notification-text {
  flex: 1;
  min-width: 0;
}

.notification-text strong {
  display: block;
  color: #1a1a2e;
  font-size: 14px;
}

.notification-text p {
  margin: 2px 0 0;
  color: #666;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notification-close {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 6px;
  border-radius: 50%;
  transition: all 0.2s;
  flex-shrink: 0;
}

.notification-close:hover {
  background: #f5f5f5;
  color: #666;
}

.notification-progress {
  height: 3px;
  background: linear-gradient(90deg, #e63946, #ff6b6b);
  animation: progressBar 5s linear forwards;
}

@keyframes slideUpNoti {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

@keyframes progressBar {
  from {
    width: 100%;
  }
  to {
    width: 0%;
  }
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

  .quick-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
  
  .chat-notification {
    bottom: 20px;
    max-width: 95%;
  }

}
</style>