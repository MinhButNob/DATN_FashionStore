<template>
  <div class="staff-dashboard">
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
      <section class="panel">
        <div class="panel-header">
          <h3>Công việc cần xử lý</h3>
          <router-link to="/staff/orders">Xem đơn hàng</router-link>
        </div>
        <div class="task-list">
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

    <section class="panel">
      <div class="panel-header">
        <h3>Đơn hàng mới hôm nay</h3>
      </div>
      <div class="table-container">
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
import StatsCard from "../../components/admin/StatsCard.vue";

const stats = [
  { title: "Doanh thu hôm nay", value: "8.45M₫", change: "+12%", icon: "fas fa-coins", color: "#e63946" },
  { title: "Đơn tại quầy", value: 24, change: "+5", icon: "fas fa-cash-register", color: "#28a745" },
  { title: "Đơn cần xử lý", value: 9, change: "-2", icon: "fas fa-box-open", color: "#ffc107" },
  { title: "Chat đang chờ", value: 6, change: "+3", icon: "fas fa-comments", color: "#17a2b8" },
];

const tasks = [
  { id: 1, title: "Xác nhận đơn online", description: "Đơn mới chưa được duyệt", count: 5, icon: "fas fa-clipboard-check", color: "#e63946" },
  { id: 2, title: "Yêu cầu đổi trả", description: "Cần kiểm tra tình trạng sản phẩm", count: 3, icon: "fas fa-undo-alt", color: "#ffc107" },
  { id: 3, title: "Tin nhắn khách hàng", description: "Cuộc trò chuyện chưa phản hồi", count: 6, icon: "fas fa-comment-dots", color: "#17a2b8" },
];

const quickActions = [
  { name: "Tạo hóa đơn", path: "/staff/pos", icon: "fas fa-plus-circle" },
  { name: "Tra cứu đơn", path: "/staff/orders", icon: "fas fa-search" },
  { name: "Thêm khách", path: "/staff/customers", icon: "fas fa-user-plus" },
  { name: "Tiếp nhận đổi trả", path: "/staff/returns", icon: "fas fa-receipt" },
];

const todayOrders = [
  { id: "POS001", customer: "Khách lẻ", total: "698.000₫", status: "Hoàn tất", badge: "badge-success", staff: "Staff" },
  { id: "DH1024", customer: "Nguyễn Minh Anh", total: "1.248.000₫", status: "Đang xử lý", badge: "badge-info", staff: "Staff" },
  { id: "DH1025", customer: "Trần Quốc Bảo", total: "399.000₫", status: "Chờ xác nhận", badge: "badge-warning", staff: "Staff" },
];
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
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
