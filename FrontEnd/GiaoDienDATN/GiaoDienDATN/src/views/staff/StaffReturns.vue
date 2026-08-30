<template>
  <div class="staff-page">
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input v-model="search" type="text" placeholder="Tìm theo mã phiếu, mã đơn, SĐT hoặc khách hàng" />
      </div>
      <select v-model="statusFilter">
        <option value="all">Tất cả trạng thái</option>
        <option value="PENDING">Chờ xử lý</option>
        <option value="COMPLETED">Đã hoàn tất</option>
        <option value="CANCELLED">Đã hủy</option>
      </select>
      <select v-model="typeFilter">
        <option value="all">Tất cả loại</option>
        <option value="RETURN">Hoàn tiền</option>
      </select>
      <button class="btn btn-primary" @click="goToCreate">
        <i class="fas fa-plus"></i>
        Tạo phiếu hoàn tiền
      </button>
    </div>

    <div class="table-container">
      <div v-if="loading" class="loading-text">Đang tải dữ liệu...</div>
      
      <table v-else>
        <thead>
          <tr>
            <th>Mã phiếu</th>
            <th>Mã đơn</th>
            <th>Khách hàng</th>
            <th>SĐT</th>
            <th>Loại</th>
            <th>Số tiền hoàn</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredReturns" :key="item.returnExchangeId">
            <td><strong>{{ item.referenceCode }}</strong></td>
            <td>{{ item.orderInvoiceNumber }}</td>
            <td>{{ item.customerName || 'Khách lẻ' }}</td>
            <td>{{ item.customerPhone || '---' }}</td>
            <td>
              <span class="badge badge-warning">
                Hoàn tiền
              </span>
            </td>
            <td class="price">{{ formatPrice(item.totalRefundAmount) }}</td>
            <td>
              <span :class="['badge', getStatusBadge(item.status)]">
                {{ getStatusText(item.status) }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button class="action-btn view-btn" title="Xem chi tiết" @click="viewDetail(item.returnExchangeId)">
                  <i class="fas fa-eye"></i>
                </button>
                <button 
                  v-if="item.status === 'PENDING'"
                  class="action-btn approve-btn" 
                  title="Hoàn tất" 
                  @click="completeReturn(item)"
                >
                  <i class="fas fa-check"></i>
                </button>
                <button 
                  v-if="item.status === 'PENDING'"
                  class="action-btn reject-btn" 
                  title="Hủy" 
                  @click="cancelReturn(item)"
                >
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredReturns.length === 0">
            <td colspan="8" style="text-align: center; padding: 20px; color: var(--medium-gray);">
              Không có phiếu hoàn tiền nào.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useNotification } from '../../composables/useNotification';
import { formatPrice } from '../../utils/helpers';
import posReturnExchangeService from '../../services/posReturnExchangeService';

const router = useRouter();
const { showNotification } = useNotification();

const search = ref("");
const statusFilter = ref("all");
const typeFilter = ref("all");
const loading = ref(false);
const returnList = ref([]);

const loadReturnList = async () => {
  loading.value = true;
  try {
    const response = await posReturnExchangeService.getAllReturnExchanges();
    returnList.value = response?.data?.data || response?.data || [];
  } catch (error) {
    console.error("Load return list error:", error);
    showNotification("Không thể tải danh sách phiếu hoàn tiền", "error");
  } finally {
    loading.value = false;
  }
};

const filteredReturns = computed(() => {
  const keyword = search.value.toLowerCase().trim();
  return returnList.value.filter(item => {
    const matchesSearch = !keyword || 
      item.referenceCode?.toLowerCase().includes(keyword) ||
      item.orderInvoiceNumber?.toLowerCase().includes(keyword) ||
      item.customerName?.toLowerCase().includes(keyword) ||
      item.customerPhone?.includes(keyword);
    
    const matchesStatus = statusFilter.value === "all" || item.status === statusFilter.value;
    const matchesType = typeFilter.value === "all" || item.type === typeFilter.value;
    
    return matchesSearch && matchesStatus && matchesType;
  });
});

const getStatusBadge = (status) => {
  const map = {
    'PENDING': 'badge-warning',
    'COMPLETED': 'badge-success',
    'CANCELLED': 'badge-danger'
  };
  return map[status] || 'badge-secondary';
};

const getStatusText = (status) => {
  const map = {
    'PENDING': 'Chờ xử lý',
    'COMPLETED': 'Đã hoàn tất',
    'CANCELLED': 'Đã hủy'
  };
  return map[status] || status;
};

const goToCreate = () => {
  router.push('/staff/returns/create');
};

const viewDetail = (id) => {
  router.push(`/staff/returns/${id}`);
};

const completeReturn = async (item) => {
  if (!confirm(`Xác nhận hoàn tất phiếu ${item.referenceCode}?`)) return;
  
  try {
    await posReturnExchangeService.updateReturnExchangeStatus(item.returnExchangeId, 'COMPLETED');
    showNotification(`Phiếu ${item.referenceCode} đã hoàn tất!`, 'success');
    await loadReturnList();
  } catch (error) {
    showNotification("Không thể cập nhật trạng thái", "error");
  }
};

const cancelReturn = async (item) => {
  if (!confirm(`Xác nhận hủy phiếu ${item.referenceCode}?`)) return;
  
  try {
    await posReturnExchangeService.cancelReturnExchange(item.returnExchangeId);
    showNotification(`Phiếu ${item.referenceCode} đã bị hủy!`, 'success');
    await loadReturnList();
  } catch (error) {
    showNotification("Không thể hủy phiếu", "error");
  }
};

onMounted(() => {
  loadReturnList();
});
</script>

<style scoped>
.staff-page { animation: fadeIn 0.3s; }

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

.price { color: var(--primary-red); font-weight: 700; }

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

.badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.badge-success { background: #d4edda; color: #155724; }
.badge-warning { background: #fff3cd; color: #856404; }
.badge-danger { background: #f8d7da; color: #721c24; }
.badge-info { background: #d1ecf1; color: #0c5460; }
.badge-secondary { background: #e2e3e5; color: #383d41; }

.loading-text { text-align: center; padding: 30px; color: var(--medium-gray); font-weight: 500; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>