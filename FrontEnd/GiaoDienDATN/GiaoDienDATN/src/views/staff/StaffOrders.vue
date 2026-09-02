<template>
  <div class="staff-page">
    <div class="session-info-banner" v-if="currentSession">
      <i class="fas fa-cash-register"></i>
      <span>
        Quầy đang hoạt động: <strong>Mã phiên #{{ currentSession.posSessionId }}</strong> 
        (Mở lúc: {{ formatDate(currentSession.startTime) }})
      </span>
    </div>

    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input 
          v-model="searchKeyword" 
          @input="debouncedSearch" 
          type="text" 
          placeholder="Tìm trong quầy: mã đơn, khách, SĐT..." 
        />
      </div>

      <select v-model="filter.orderStatus" @change="handleFilterChange">
        <option value="">Tất cả trạng thái</option>
        <option value="COMPLETED">Hoàn thành</option>
        <option value="PENDING">Chờ xử lý</option>
        <option value="CANCELLED">Đã hủy</option>
      </select>

      <select v-model="filter.paymentStatus" @change="handleFilterChange">
        <option value="">Tất cả thanh toán</option>
        <option value="PAID">Đã thanh toán</option>
        <option value="UNPAID">Chưa thanh toán</option>
        <option value="CANCELLED">Đã hủy</option>
      </select>

      <button class="btn btn-primary" @click="resetFilter">
        <i class="fas fa-undo"></i>
        Làm mới
      </button>
    </div>

    <div class="table-container">
      <div v-if="loading" class="loading-text">Đang tải dữ liệu hóa đơn của quầy...</div>
      
      <table v-else>
        <thead>
          <tr>
            <th>Mã hóa đơn</th>
            <th>Sản phẩm</th>
            <th>Tổng tiền</th>
            <th>Thanh toán</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in invoices" :key="order.posOrderId">
            <td><strong>{{ order.invoiceNumber }}</strong></td>
            <td>{{ order.items?.length || 0 }} sản phẩm</td>
            <td class="price">{{ formatPrice(order.total) }}</td>
            <td>
              <span :class="['badge', getPaymentBadge(order)]">
                {{ getPaymentMethodText(order) }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button class="action-btn view-btn" title="Xem chi tiết" @click="viewDetail(order.posOrderId)">
                  <i class="fas fa-eye"></i>
                </button>
                <button 
                  v-if="order.orderStatus !== 'CANCELLED' && order.orderStatus !== 'COMPLETED'"
                  class="action-btn edit-btn" 
                  title="Hủy hóa đơn" 
                  @click="handleCancelInvoice(order)"
                >
                  <i class="fas fa-times"></i>
                </button>
                <button class="action-btn print-btn" title="In hóa đơn" @click="printInvoice(order.posOrderId)">
                  <i class="fas fa-print"></i>
                </button>
                <!-- NÚT HOÀN HÀNG -->
                <button 
                  v-if="order.orderStatus === 'COMPLETED' && order.paymentStatus === 'PAID'"
                  class="action-btn return-btn" 
                  title="Hoàn hàng" 
                  @click="openReturnModal(order)"
                >
                  <i class="fas fa-undo-alt"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="invoices.length === 0">
            <td colspan="5" style="text-align: center; padding: 20px; color: var(--medium-gray);">
              Quầy hiện tại chưa có hóa đơn nào phù hợp.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination-bar" v-if="totalPages > 1">
      <div class="pagination-info">
        Hiển thị {{ filter.page * filter.size + 1 }} - {{ Math.min((filter.page + 1) * filter.size, totalElements) }} trong tổng số {{ totalElements }} bản ghi của quầy
      </div>
      <div class="pagination-buttons">
        <button :disabled="filter.page === 0" @click="changePage(filter.page - 1)">
          <i class="fas fa-chevron-left"></i> Trước
        </button>
        <span class="page-number">Trang {{ filter.page + 1 }} / {{ totalPages }}</span>
        <button :disabled="filter.page >= totalPages - 1" @click="changePage(filter.page + 1)">
          Sau <i class="fas fa-chevron-right"></i>
        </button>
      </div>
    </div>

    <!-- Modal chi tiết hóa đơn -->
    <transition name="modal-fade">
      <div class="modal-overlay" v-if="showDetailModal">
        <div class="modal-wrapper" @click.self="closeDetailModal">
          <div class="modal-container modal-lg">
            <div class="modal-header">
              <h3><i class="fas fa-file-invoice"></i> Chi tiết hóa đơn: {{ selectedInvoice?.posOrderInvoiceNumber || selectedInvoice?.invoiceNumber }}</h3>
              <button class="close-btn" @click="closeDetailModal">&times;</button>
            </div>
            <div class="modal-body" v-if="selectedInvoice">
              <div class="invoice-info-grid">
                <div class="info-group">
                  <label>Khách hàng:</label>
                  <div>{{ selectedInvoice.posOrderCustomerName || selectedInvoice.customerName || 'Khách lẻ' }}</div>
                </div>
                <div class="info-group">
                  <label>Số điện thoại:</label>
                  <div>{{ selectedInvoice.posOrderCustomerPhone || selectedInvoice.customerPhone || 'Không có' }}</div>
                </div>
                <div class="info-group">
                  <label>Trạng thái:</label>
                  <div>
                    <span :class="['badge', `badge-${(selectedInvoice.posOrderStatus || selectedInvoice.orderStatus)?.toLowerCase()}`]">
                      {{ (selectedInvoice.posOrderStatus || selectedInvoice.orderStatus) === 'COMPLETED' ? 'Hoàn thành' : 
                         (selectedInvoice.posOrderStatus || selectedInvoice.orderStatus) === 'PENDING' ? 'Chờ xử lý' : 'Đã hủy' }}
                    </span>
                  </div>
                </div>
                <div class="info-group">
                  <label>Thanh toán:</label>
                  <div>
                    <span :class="['badge', getPaymentBadge(selectedInvoice)]">
                      {{ getPaymentMethodText(selectedInvoice) }}
                    </span>
                  </div>
                </div>
              </div>

              <h4 class="mt-4 mb-2">Danh sách sản phẩm</h4>
              <table class="items-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th>Phân loại</th>
                    <th class="text-right">Đơn giá</th>
                    <th class="text-center">Số lượng</th>
                    <th class="text-right">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in selectedInvoice.items" :key="index">
                    <td>{{ item.productName || 'Sản phẩm' }}</td>
                    <td>{{ item.size }} - {{ item.color }}</td>
                    <td class="text-right">{{ formatPrice(item.unitPrice) }}</td>
                    <td class="text-center">{{ item.quantity }}</td>
                    <td class="text-right">{{ formatPrice(item.unitPrice * item.quantity) }}</td>
                  </tr>
                </tbody>
              </table>

              <div class="invoice-summary mt-4">
                <div class="summary-row">
                  <span>Tạm tính:</span>
                  <span>{{ formatPrice(selectedInvoice.subtotal || 0) }}</span>
                </div>
                <div class="summary-row text-success">
                  <span>Giảm giá:</span>
                  <span>{{ formatPrice(selectedInvoice.discount || 0) }}</span>
                </div>
                <div class="summary-row total-row">
                  <span>Tổng thanh toán:</span>
                  <span>{{ formatPrice(selectedInvoice.total || 0) }}</span>
                </div>
                <div class="summary-row">
                  <span>Khách đã trả:</span>
                  <span>{{ formatPrice(selectedInvoice.paidAmount || 0) }}</span>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="closeDetailModal">Đóng</button>
              <button class="btn btn-primary" @click="printInvoice(selectedInvoice?.posOrderId || selectedInvoice?.orderId)">
                <i class="fas fa-print"></i> In hóa đơn
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import {
  searchInvoices,
  cancelInvoice,
  getInvoiceDetail
} from "../../services/posOrderService";
import { getMyActiveSession, printPosReceipt } from "../../services/posService";
import { useNotification } from '../../composables/useNotification';
import { formatPrice } from '../../utils/helpers';
import { printReceiptHTML, createPrintWindow } from '../../utils/printHelper';

const router = useRouter();
const { showNotification } = useNotification();

// Quản lý thông tin ca/quầy làm việc của nhân viên
const currentSession = ref(null);
const loadingSession = ref(true);

// State quản lý danh sách hiển thị và phân trang dữ liệu
const loading = ref(false);
const invoices = ref([]);
const totalPages = ref(0);
const totalElements = ref(0);

// State cho modal chi tiết
const showDetailModal = ref(false);
const selectedInvoice = ref(null);

// Ô nhập tìm kiếm text tạm thời trên giao diện
const searchKeyword = ref("");

// Object Filter ánh xạ chính xác cấu trúc với PosInvoiceFilterDTO ở Backend
const filter = ref({
  page: 0,
  size: 10,
  invoiceNumber: "",
  customerName: "",
  customerPhone: "",
  orderStatus: "",
  paymentStatus: "",
  posSessionId: null,
  startDate: null,
  endDate: null
});

// Lấy thông tin phiên/quầy hoạt động của tài khoản hiện tại trước
const fetchCurrentSession = async () => {
  try {
    const session = await getMyActiveSession();
    console.log("SESSION ĐANG HOẠT ĐỘNG THỰC TẾ:", session);

    if (session) {
      currentSession.value = session;
      filter.value.posSessionId = session.posSessionId; 
    }
    await fetchInvoicesData();
  } catch (error) {
    console.error(error);
    await fetchInvoicesData();
  } finally {
    loadingSession.value = false;
  }
};

// Gọi API lấy danh sách hóa đơn theo các điều kiện lọc
const fetchInvoicesData = async () => {
  loading.value = true;

  try {
    console.log("FILTER =", JSON.stringify(filter.value));

    const response = await searchInvoices(filter.value);
    const targetData = response?.content ? response : response?.data;

    invoices.value = targetData?.content || [];
    totalPages.value = targetData?.totalPages || 0;
    totalElements.value = targetData?.totalElements || 0;

  } catch (error) {
    console.error("FULL ERROR =", error);
  } finally {
    loading.value = false;
  }
};

// Vừa mở trang lên: ưu tiên kiểm tra ca quầy trước rồi tự động load dữ liệu
onMounted(() => {
  fetchCurrentSession();
});

// Tách từ khóa gõ ở ô Tìm kiếm nhanh đổ vào các trường DTO tương ứng
const parseSearchKeyword = () => {
  const value = searchKeyword.value.trim();
  
  filter.value.invoiceNumber = "";
  filter.value.customerName = "";
  filter.value.customerPhone = "";

  if (!value) return;

  if (/^\d+$/.test(value)) {
    filter.value.customerPhone = value;
  } else if (value.toUpperCase().startsWith("HD") || value.includes("-")) {
    filter.value.invoiceNumber = value;
  } else {
    filter.value.customerName = value;
  }
};

let timeoutId = null;
const debouncedSearch = () => {
  clearTimeout(timeoutId);
  timeoutId = setTimeout(() => {
    parseSearchKeyword();
    filter.value.page = 0;
    fetchInvoicesData();
  }, 400);
};

const handleFilterChange = () => {
  filter.value.page = 0;
  fetchInvoicesData();
};

const changePage = (newPage) => {
  if (newPage >= 0 && newPage < totalPages.value) {
    filter.value.page = newPage;
    fetchInvoicesData();
  }
};

const resetFilter = () => {
  searchKeyword.value = "";

  filter.value = {
    page: 0,
    size: 10,
    invoiceNumber: "",
    customerName: "",
    customerPhone: "",
    orderStatus: "",
    paymentStatus: "",
    posSessionId: currentSession.value ? currentSession.value.posSessionId : null,
    startDate: null,
    endDate: null
  };

  fetchInvoicesData();
};

const handleCancelInvoice = async (order) => {
  const reason = prompt("Nhập lý do hủy hóa đơn này:");
  if (reason === null) return;
  if (!reason.trim()) {
    showNotification("Bạn bắt buộc phải nhập lý do hủy đơn!", "warning");
    return;
  }

  try {
    await cancelInvoice(order.posOrderId, reason);
    showNotification("Hủy hóa đơn thành công, số lượng sản phẩm đã hoàn trả kho quầy!", "success");
    await fetchInvoicesData();
  } catch (error) {
    console.error("Cancel order error:", error);
    showNotification(error.response?.data?.message || "Lỗi xảy ra khi thực hiện hủy đơn hàng", "error");
  }
};

const handleStatusChange = async (order) => {
  if (order.orderStatus === 'CANCELLED') {
    handleCancelInvoice(order);
    return;
  }
  showNotification(`Hệ thống ghi nhận chuyển trạng thái sang: ${order.orderStatus}`, 'info');
};

// ===== MỞ MODAL TẠO PHIẾU ĐỔI/TRẢ =====
const openReturnModal = (order) => {
  router.push({
    path: '/staff/returns/create',
    query: { 
      orderId: order.posOrderId,
      phone: order.customerPhone || ''  // ✅ Truyền SĐT sang trang tạo phiếu
    }
  });
};

const viewDetail = async (orderId) => {
  try {
    const response = await getInvoiceDetail(orderId);
    if (response && response.data) {
      selectedInvoice.value = response.data.data || response.data;
      showDetailModal.value = true;
    }
  } catch (error) {
    showNotification("Không thể tải thông tin chi tiết hóa đơn", "error");
  }
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedInvoice.value = null;
};

const printInvoice = async (orderId) => {
  const win = createPrintWindow();
  if (!win) return;
  
  try {
    const response = await getInvoiceDetail(orderId);
    const invoice = response?.data?.data || response?.data;
    if (!invoice) throw new Error("No data");

    await printPosReceipt(orderId, 1);
    printReceiptHTML(invoice, win);

    showNotification(`Đã in hóa đơn ID: ${orderId} thành công!`, 'success');
  } catch (error) {
    if (win) win.close();
    console.error("Print Invoice Error:", error);
    showNotification("Gặp lỗi trong quá trình xử lý lệnh in", "error");
  }
};

const getOrderPaymentMethod = (order) => {
  if (!order) return 'CASH';
  
  // 1. Kiểm tra mảng payments
  if (Array.isArray(order.payments) && order.payments.length > 0) {
    const methods = order.payments.map(p => (p.paymentMethod || '').toUpperCase());
    if (methods.some(m => m.includes('VNPAY'))) return 'VNPAY';
    if (methods.some(m => m.includes('CASH') || m.includes('TIEN_MAT'))) return 'CASH';
  }
  
  // 2. Kiểm tra thuộc tính phương thức thanh toán trực tiếp trên đơn hàng
  const directMethod = (order.paymentMethod || order.posOrderPaymentMethod || order.paymentMode || order.method || '').toUpperCase();
  if (directMethod.includes('VNPAY')) return 'VNPAY';
  
  return 'CASH';
};

const getPaymentBadge = (order) => {
  const method = getOrderPaymentMethod(order);
  if (method === 'VNPAY') return 'badge-pink';
  return 'badge-success'; // Tiền mặt luôn có màu xanh lá
};

const getPaymentMethodText = (order) => {
  const method = getOrderPaymentMethod(order);
  if (method === 'VNPAY') return 'VNPay';
  return 'Tiền mặt';
};

const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString('vi-VN');
};
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

.main-text { color: var(--dark-gray); font-weight: 600; }
.sub-text { color: var(--medium-gray); font-size: 12px; margin-top: 3px; }
.price { color: var(--primary-red); font-weight: 700; }

.status-select {
  padding: 8px 12px;
  border: none;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  outline: none;
}

.status-pending { background: #fff3cd; color: #856404; }
.status-completed { background: #d4edda; color: #155724; }
.status-cancelled { background: #f8d7da; color: #721c24; }

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  width: 35px;
  height: 35px;
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.view-btn { background: #17a2b8; }
.edit-btn { background: #dc3545; }
.print-btn { background: #28a745; }
.return-btn { background: #fd7e14; }
.return-btn:hover { background: #e06b0a; }

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: white;
  border-radius: 10px;
  margin-top: 15px;
  box-shadow: var(--shadow);
}

.pagination-info { font-size: 14px; color: var(--dark-gray); }
.pagination-buttons { display: flex; align-items: center; gap: 12px; }
.pagination-buttons button {
  padding: 8px 14px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
}
.pagination-buttons button:hover:not(:disabled) { background: var(--light-gray); }
.pagination-buttons button:disabled { opacity: 0.5; cursor: not-allowed; }
.page-number { font-size: 14px; font-weight: 600; }

.loading-text { text-align: center; padding: 30px; color: var(--medium-gray); font-weight: 500; }

/* Session banner */
.session-info-banner {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  padding: 12px 18px;
  border-radius: 8px;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #0050b3;
  font-size: 14px;
}

/* Modal styles */
.modal-overlay {
  position: fixed;
  z-index: 9998;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-container {
  width: 90%;
  max-width: 600px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.modal-lg { max-width: 800px; }

.modal-header {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 { margin: 0; color: #333; }

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.modal-body { padding: 20px; overflow-y: auto; }
.modal-footer {
  padding: 15px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.invoice-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.info-group label { font-weight: 600; color: #555; font-size: 0.9em; display: block; margin-bottom: 5px; }

.items-table { width: 100%; border-collapse: collapse; }
.items-table th, .items-table td { padding: 10px; border-bottom: 1px solid #eee; }
.items-table th { background: #f8f9fa; font-weight: 600; }

.text-right { text-align: right; }
.text-center { text-align: center; }

.invoice-summary {
  width: 300px;
  margin-left: auto;
  border-top: 1px solid #ddd;
  padding-top: 10px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.total-row {
  font-weight: bold;
  font-size: 1.1em;
  color: #d9534f;
}

.mt-4 { margin-top: 1.5rem; }
.mb-2 { margin-bottom: 0.5rem; }

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
.badge-pink { background: #fff1f2; color: #be123c; border: 1px solid #fecdd3; }

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>