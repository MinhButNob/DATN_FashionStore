<template>
  <div class="staff-return-detail">
    <div class="page-header">
      <h2><i class="fas fa-file-invoice"></i> Chi tiết phiếu đổi/trả</h2>
      <button class="btn btn-secondary" @click="goBack">
        <i class="fas fa-arrow-left"></i> Quay lại
      </button>
    </div>

    <div v-if="loading" class="loading-state">
      <i class="fas fa-spinner fa-spin"></i> Đang tải...
    </div>

    <div v-else-if="returnData" class="detail-content">
      <div class="info-section">
        <div class="info-grid">
          <div><strong>Mã phiếu:</strong> {{ returnData.referenceCode }}</div>
          <div><strong>Mã đơn:</strong> {{ returnData.orderInvoiceNumber }}</div>
          <div><strong>Khách hàng:</strong> {{ returnData.customerName || 'Khách lẻ' }}</div>
          <div><strong>SĐT:</strong> {{ returnData.customerPhone || '---' }}</div>
          <div><strong>Loại:</strong> 
            <span :class="['badge', returnData.type === 'RETURN' ? 'badge-warning' : 'badge-info']">
              {{ returnData.type === 'RETURN' ? 'Hoàn tiền' : 'Đổi hàng' }}
            </span>
          </div>
          <div><strong>Trạng thái:</strong>
            <span :class="['badge', getStatusBadge(returnData.status)]">
              {{ getStatusText(returnData.status) }}
            </span>
          </div>
          <div><strong>Ngày tạo:</strong> {{ formatDate(returnData.createdAt) }}</div>
          <div v-if="returnData.completedAt"><strong>Ngày hoàn tất:</strong> {{ formatDate(returnData.completedAt) }}</div>
        </div>
      </div>

      <div class="reason-section" v-if="returnData.reason">
        <h4>Lý do</h4>
        <p>{{ returnData.reason }}</p>
      </div>

      <!-- ✅ SẢN PHẨM TRẢ LẠI - HIỂN THỊ GIẢM GIÁ -->
      <div class="items-section">
        <h4>Sản phẩm trả lại</h4>
        <table>
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th>Phân loại</th>
              <th>Đơn giá</th>
              <th>Số lượng</th>
              <th>Giá gốc</th>
              <th>Giảm giá</th>
              <th>Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in returnData.items?.filter(i => i.type === 'RETURN')" :key="item.itemId">
              <td>{{ item.productName }}</td>
              <td>{{ item.sizeName }} - {{ item.colorName }}</td>
              <td>{{ formatPrice(item.unitPrice) }}</td>
              <td>{{ item.quantity }}</td>
              <td class="price">{{ formatPrice(item.originalTotal || item.unitPrice * item.quantity) }}</td>
              <td class="text-danger">-{{ formatPrice(item.discountAllocated || 0) }}</td>
              <td class="price">{{ formatPrice(item.totalAmount) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ✅ SẢN PHẨM ĐỔI MỚI -->
      <div class="items-section" v-if="returnData.type === 'EXCHANGE'">
        <h4>Sản phẩm đổi mới</h4>
        <table>
          <thead>
            <tr>
              <th>Sản phẩm mới</th>
              <th>Phân loại</th>
              <th>Đơn giá</th>
              <th>Số lượng</th>
              <th>Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in returnData.items?.filter(i => i.type === 'EXCHANGE')" :key="item.itemId">
              <td>{{ item.newProductName || item.productName }}</td>
              <td>{{ item.sizeName }} - {{ item.colorName }}</td>
              <td>{{ formatPrice(item.unitPrice) }}</td>
              <td>{{ item.newQuantity || item.quantity }}</td>
              <td class="price">{{ formatPrice(item.totalAmount) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ✅ TÓM TẮT - HIỂN THỊ GIẢM GIÁ -->
      <div class="summary-section">
        <div class="summary-row">
          <span>Tổng giá gốc:</span>
          <span>{{ formatPrice(returnData.totalOriginalAmount || 0) }}</span>
        </div>
        <div class="summary-row text-danger" v-if="(returnData.totalDiscountAllocated || 0) > 0">
          <span>Giảm giá (Voucher):</span>
          <span>-{{ formatPrice(returnData.totalDiscountAllocated || 0) }}</span>
        </div>
        <div class="summary-row total-row">
          <span>Tổng tiền hoàn:</span>
          <span class="price">{{ formatPrice(returnData.totalRefundAmount) }}</span>
        </div>
        <div v-if="returnData.additionalPayment > 0" class="summary-row">
          <span>Khách trả thêm:</span>
          <span class="price text-danger">{{ formatPrice(returnData.additionalPayment) }}</span>
        </div>
        <div v-else-if="returnData.additionalPayment < 0" class="summary-row">
          <span>Hoàn lại khách:</span>
          <span class="price text-success">{{ formatPrice(Math.abs(returnData.additionalPayment)) }}</span>
        </div>
      </div>

      <div class="notes-section" v-if="returnData.notes">
        <h4>Ghi chú</h4>
        <p>{{ returnData.notes }}</p>
      </div>

      <div class="action-buttons" v-if="returnData.status === 'PENDING'">
        <button class="btn btn-success" @click="completeReturn">
          <i class="fas fa-check"></i> Hoàn tất
        </button>
        <button class="btn btn-danger" @click="cancelReturn">
          <i class="fas fa-times"></i> Hủy phiếu
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useNotification } from '../../composables/useNotification';
import { formatPrice } from '../../utils/helpers';
import posReturnExchangeService from '../../services/posReturnExchangeService';

const router = useRouter();
const route = useRoute();
const { showNotification } = useNotification();

const loading = ref(false);
const returnData = ref(null);

const loadDetail = async () => {
  const id = route.params.id;
  if (!id) {
    showNotification("Không tìm thấy ID phiếu", "error");
    router.back();
    return;
  }
  
  loading.value = true;
  try {
    const response = await posReturnExchangeService.getReturnExchangeDetail(id);
    returnData.value = response?.data?.data || response?.data;
  } catch (error) {
    console.error("Load detail error:", error);
    showNotification("Không thể tải chi tiết phiếu", "error");
  } finally {
    loading.value = false;
  }
};

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

const formatDate = (date) => {
  if (!date) return '';
  return new Date(date).toLocaleString('vi-VN');
};

const completeReturn = async () => {
  if (!confirm(`Xác nhận hoàn tất phiếu ${returnData.value.referenceCode}?`)) return;
  
  try {
    await posReturnExchangeService.updateReturnExchangeStatus(returnData.value.returnExchangeId, 'COMPLETED');
    showNotification('Phiếu đã được hoàn tất!', 'success');
    await loadDetail();
  } catch (error) {
    showNotification("Không thể cập nhật trạng thái", "error");
  }
};

const cancelReturn = async () => {
  if (!confirm(`Xác nhận hủy phiếu ${returnData.value.referenceCode}?`)) return;
  
  try {
    await posReturnExchangeService.cancelReturnExchange(returnData.value.returnExchangeId);
    showNotification('Phiếu đã bị hủy!', 'success');
    await loadDetail();
  } catch (error) {
    showNotification("Không thể hủy phiếu", "error");
  }
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  loadDetail();
});
</script>

<style scoped>
.staff-return-detail {
  animation: fadeIn 0.3s;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  color: #1a1a2e;
  font-size: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.loading-state i {
  font-size: 32px;
  margin-bottom: 12px;
  display: block;
}

.detail-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.info-section {
  margin-bottom: 24px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.info-grid strong {
  color: #555;
}

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

.reason-section, .items-section, .notes-section {
  margin-bottom: 24px;
}

.reason-section h4, .items-section h4, .notes-section h4 {
  color: #1a1a2e;
  margin-bottom: 12px;
}

.reason-section p, .notes-section p {
  color: #555;
  line-height: 1.6;
}

/* ✅ BẢNG SẢN PHẨM */
.items-section table {
  width: 100%;
  border-collapse: collapse;
}

.items-section th, .items-section td {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
}

.items-section th {
  background: #f8f9fb;
  font-weight: 600;
  color: #555;
}

.items-section .price {
  color: #e63946;
  font-weight: 600;
}

.text-danger {
  color: #dc3545;
}

.text-success {
  color: #28a745;
}

/* ✅ TÓM TẮT */
.summary-section {
  border-top: 1px solid #eee;
  padding-top: 16px;
  margin-top: 16px;
}

.summary-row {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  padding: 4px 0;
  font-size: 16px;
}

.summary-row .price {
  font-weight: 700;
  min-width: 120px;
  text-align: right;
}

.total-row {
  font-weight: bold;
  font-size: 1.1em;
  border-top: 2px solid #333;
  padding-top: 10px;
  margin-top: 6px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  justify-content: flex-end;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.btn-secondary {
  background: #f0f0f0;
  color: #555;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover {
  background: #218838;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>