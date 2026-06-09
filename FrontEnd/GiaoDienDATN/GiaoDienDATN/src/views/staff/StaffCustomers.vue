<template>
  <div class="staff-page">
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input v-model="search" type="text" placeholder="Tìm khách hàng theo tên, SĐT hoặc email" />
      </div>
      <button class="btn btn-primary">
        <i class="fas fa-user-plus"></i>
        Thêm khách hàng
      </button>
    </div>

    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>Khách hàng</th>
            <th>Số điện thoại</th>
            <th>Email</th>
            <th>Hạng</th>
            <th>Đơn gần nhất</th>
            <th>Tổng chi tiêu</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="customer in filteredCustomers" :key="customer.id">
            <td>
              <div class="customer-cell">
                <div class="avatar">{{ customer.name.charAt(0) }}</div>
                <div>
                  <div class="main-text">{{ customer.name }}</div>
                  <div class="sub-text">#{{ customer.id }}</div>
                </div>
              </div>
            </td>
            <td>{{ customer.phone }}</td>
            <td>{{ customer.email }}</td>
            <td><span :class="['badge', customer.badge]">{{ customer.level }}</span></td>
            <td>{{ customer.lastOrder }}</td>
            <td class="price">{{ customer.spent }}</td>
            <td>
              <div class="action-buttons">
                <button class="action-btn view-btn" title="Lịch sử mua"><i class="fas fa-history"></i></button>
                <button class="action-btn edit-btn" title="Cập nhật"><i class="fas fa-edit"></i></button>
                <button class="action-btn chat-btn" title="Chat"><i class="fas fa-comment"></i></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";

const search = ref("");
const customers = [
  { id: "KH001", name: "Nguyễn Minh Anh", phone: "0901122334", email: "minhanh@email.com", level: "VIP", badge: "badge-success", lastOrder: "DH1024", spent: "8.420.000₫" },
  { id: "KH002", name: "Trần Quốc Bảo", phone: "0919988776", email: "quocbao@email.com", level: "Thân thiết", badge: "badge-info", lastOrder: "DH1025", spent: "3.120.000₫" },
  { id: "KH003", name: "Lê Hoài Nam", phone: "0987654321", email: "hoainam@email.com", level: "Mới", badge: "badge-warning", lastOrder: "DH1026", spent: "499.000₫" },
];

const filteredCustomers = computed(() => {
  const keyword = search.value.toLowerCase();
  return customers.filter((customer) =>
    [customer.name, customer.phone, customer.email].some((value) => value.toLowerCase().includes(keyword))
  );
});
</script>

<style scoped>
.staff-page {
  animation: fadeIn 0.3s;
}

.filters-bar {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: var(--shadow);
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.search-box {
  flex: 1;
  min-width: 260px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--light-gray);
  border-radius: 8px;
  padding: 0 14px;
}

.search-box input {
  border: none;
  background: transparent;
  outline: none;
  padding: 12px 0;
  width: 100%;
}

.customer-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.main-text {
  color: var(--dark-gray);
  font-weight: 600;
}

.sub-text {
  color: var(--medium-gray);
  font-size: 12px;
}

.price {
  color: var(--primary-red);
  font-weight: 700;
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
  color: white;
  cursor: pointer;
}

.view-btn { background: #17a2b8; }
.edit-btn { background: #ffc107; color: var(--dark-gray); }
.chat-btn { background: #28a745; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
