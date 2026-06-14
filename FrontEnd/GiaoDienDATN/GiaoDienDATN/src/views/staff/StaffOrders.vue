<template>
  <div class="staff-page">
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input v-model="search" type="text" placeholder="Tìm theo mã đơn, khách hàng, số điện thoại" />
      </div>
      <select v-model="status">
        <option value="all">Tất cả trạng thái</option>
        <option value="pending">Chờ xác nhận</option>
        <option value="processing">Đang xử lý</option>
        <option value="shipping">Đang giao</option>
        <option value="done">Hoàn tất</option>
      </select>
      <button class="btn btn-primary">
        <i class="fas fa-plus"></i>
        Tạo đơn mới
      </button>
    </div>

    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th>Mã đơn</th>
            <th>Khách hàng</th>
            <th>Sản phẩm</th>
            <th>Tổng tiền</th>
            <th>Trạng thái</th>
            <th>Thanh toán</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in filteredOrders" :key="order.id">
            <td><strong>{{ order.id }}</strong></td>
            <td>
              <div class="main-text">{{ order.customer?.name || 'Khách lẻ' }}</div>
              <div class="sub-text">{{ order.customer?.phone || '-' }}</div>
            </td>
            <td>{{ order.items?.length || 0 }} sản phẩm</td>
            <td class="price">{{ formatPrice(order.total) }}</td>
            <td>
              <select v-model="order.status" class="status-select" :class="`status-${order.status}`" @change="updateOrderStatus(order)">
                <option value="pending">Chờ xác nhận</option>
                <option value="processing">Đang xử lý</option>
                <option value="shipping">Đang giao</option>
                <option value="done">Hoàn tất</option>
              </select>
            </td>
            <td><span :class="['badge', getPaymentBadge(order.payment?.status)]">{{ order.payment?.method || 'Thanh toán' }}</span></td>
            <td>
              <div class="action-buttons">
                <button class="action-btn view-btn" title="Xem chi tiết"><i class="fas fa-eye"></i></button>
                <button class="action-btn edit-btn" title="Cập nhật"><i class="fas fa-edit"></i></button>
                <button class="action-btn print-btn" title="In hóa đơn"><i class="fas fa-print"></i></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from "vue";
import axios from 'axios';
import { useNotification } from '../../composables/useNotification';
import { formatPrice } from '../../utils/helpers';

const { showNotification } = useNotification();

const search = ref("");
const status = ref("all");
const orders = ref([]);

const fetchOrders = async () => {
  try {
    const token = localStorage.getItem('token');
    const response = await axios.get('http://localhost:8080/api/orders', {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    
    if (response.data.success) {
      orders.value = response.data.data;
    } else {
      showNotification(response.data.message || 'Lỗi khi tải danh sách đơn hàng', 'error');
    }
  } catch (error) {
    console.error('Error fetching orders:', error);
    showNotification('Không thể kết nối đến máy chủ', 'error');
  }
};

onMounted(() => {
  fetchOrders();
});

const getPaymentBadge = (status) => {
  if (status === 'paid' || status === 'COMPLETED') return 'badge-success';
  if (status === 'pending') return 'badge-warning';
  return 'badge-info';
};

const updateOrderStatus = async (order) => {
  try {
    const token = localStorage.getItem('token');
    const response = await axios.put(`http://localhost:8080/api/orders/${order.originalId}/status`, 
      { status: order.status },
      { headers: { Authorization: `Bearer ${token}` } }
    );
    if (response.data.success) {
      showNotification('Cập nhật trạng thái thành công!', 'success');
      fetchOrders();
    } else {
      showNotification(response.data.message || 'Lỗi cập nhật trạng thái', 'error');
      fetchOrders(); // revert
    }
  } catch (error) {
    console.error('Error updating order status:', error);
    showNotification('Lỗi máy chủ', 'error');
    fetchOrders(); // revert
  }
};

const filteredOrders = computed(() => {
  const keyword = search.value.toLowerCase();
  return orders.value.filter((order) => {
    const customerName = order.customer?.name || '';
    const phone = order.customer?.phone || '';
    const matchesSearch = [order.id, customerName, phone].some((value) => value.toLowerCase().includes(keyword));
    const matchesStatus = status.value === "all" || order.status === status.value;
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

.filters-bar select {
  padding: 12px 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  background: white;
}

.main-text {
  color: var(--dark-gray);
  font-weight: 600;
}

.sub-text {
  color: var(--medium-gray);
  font-size: 12px;
  margin-top: 3px;
}

.price {
  color: var(--primary-red);
  font-weight: 700;
}

.status-select {
  padding: 8px 12px;
  border: none;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  outline: none;
}

.status-pending { background: #fff3cd; color: #856404; }
.status-processing { background: #cce5ff; color: #004085; }
.status-shipping { background: #d1ecf1; color: #0c5460; }
.status-done { background: #d4edda; color: #155724; }

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
.print-btn { background: #28a745; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
