<template>
  <div class="staff-page">
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input v-model="search" type="text" placeholder="Tìm theo mã đổi trả, mã đơn hoặc khách hàng" />
      </div>
      <select v-model="status">
        <option value="all">Tất cả trạng thái</option>
        <option value="new">Mới tiếp nhận</option>
        <option value="checking">Đang kiểm tra</option>
        <option value="approved">Đã duyệt</option>
        <option value="rejected">Từ chối</option>
      </select>
      <button class="btn btn-primary">
        <i class="fas fa-plus"></i>
        Tạo phiếu đổi trả
      </button>
    </div>

    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>Mã phiếu</th>
            <th>Mã đơn</th>
            <th>Khách hàng</th>
            <th>Sản phẩm</th>
            <th>Lý do</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="request in filteredReturns" :key="request.id">
            <td><strong>{{ request.id }}</strong></td>
            <td>{{ request.orderId }}</td>
            <td>{{ request.customer }}</td>
            <td>{{ request.product }}</td>
            <td>{{ request.reason }}</td>
            <td>
              <select v-model="request.status" class="status-select" :class="`status-${request.status}`">
                <option value="new">Mới tiếp nhận</option>
                <option value="checking">Đang kiểm tra</option>
                <option value="approved">Đã duyệt</option>
                <option value="rejected">Từ chối</option>
              </select>
            </td>
            <td>
              <div class="action-buttons">
                <button class="action-btn view-btn" title="Xem chi tiết"><i class="fas fa-eye"></i></button>
                <button class="action-btn approve-btn" title="Duyệt"><i class="fas fa-check"></i></button>
                <button class="action-btn reject-btn" title="Từ chối"><i class="fas fa-times"></i></button>
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
const status = ref("all");
const returns = ref([
  { id: "DT001", orderId: "DH1020", customer: "Nguyễn Minh Anh", product: "Áo thun basic", reason: "Sai size", status: "new" },
  { id: "DT002", orderId: "DH1018", customer: "Trần Quốc Bảo", product: "Quần jean slim", reason: "Lỗi đường may", status: "checking" },
  { id: "DT003", orderId: "DH1014", customer: "Lê Hoài Nam", product: "Túi xách nữ", reason: "Đổi màu", status: "approved" },
]);

const filteredReturns = computed(() => {
  const keyword = search.value.toLowerCase();
  return returns.value.filter((request) => {
    const matchesSearch = [request.id, request.orderId, request.customer].some((value) => value.toLowerCase().includes(keyword));
    const matchesStatus = status.value === "all" || request.status === status.value;
    return matchesSearch && matchesStatus;
  });
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

.filters-bar select,
.status-select {
  padding: 12px 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  background: white;
}

.status-select {
  border: none;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-new { background: #fff3cd; color: #856404; }
.status-checking { background: #d1ecf1; color: #0c5460; }
.status-approved { background: #d4edda; color: #155724; }
.status-rejected { background: #f8d7da; color: #721c24; }

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
.approve-btn { background: #28a745; }
.reject-btn { background: #dc3545; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
