<template>
  <div class="staff-page">
    <div class="session-info-banner" v-if="currentSession">
      <i class="fas fa-cash-register"></i>
      <span>
        Quầy đang hoạt động: <strong>Mã phiên #{{ currentSession.posSessionId }}</strong> 
        (Mở lúc: {{ formatDate(currentSession.startTime) }})
      </span>
    </div>
    <div class="session-info-banner error" v-else-if="!loadingSession">
      <i class="fas fa-exclamation-triangle"></i>
      <span>Bạn chưa vào ca hoặc không có phiên/quầy nào đang mở! Vui lòng mở ca trước.</span>
    </div>

    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input 
          v-model="searchKeyword" 
          @input="debouncedSearch" 
          type="text" 
          placeholder="Tìm trong quầy: mã đơn, khách, SĐT..." 
          :disabled="!currentSession"
        />
      </div>

      <select v-model="filter.orderStatus" @change="handleFilterChange" :disabled="!currentSession">
        <option value="">Tất cả trạng thái</option>
        <option value="COMPLETED">Hoàn thành</option>
        <option value="PENDING">Chờ xử lý</option>
        <option value="CANCELLED">Đã hủy</option>
      </select>

      <select v-model="filter.paymentStatus" @change="handleFilterChange" :disabled="!currentSession">
        <option value="">Tất cả thanh toán</option>
        <option value="PAID">Đã thanh toán</option>
        <option value="UNPAID">Chưa thanh toán</option>
        <option value="CANCELLED">Đã hủy</option>
      </select>

      <button class="btn btn-primary" @click="resetFilter" :disabled="!currentSession">
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
            <th>Khách hàng</th>
            <th>Sản phẩm</th>
            <th>Tổng tiền</th>
            <th>Trạng thái đơn</th>
            <th>Thanh toán</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in invoices" :key="order.posOrderId">
            <td><strong>{{ order.invoiceNumber }}</strong></td>
            <td>
              <div class="main-text">{{ order.customerName || 'Khách lẻ' }}</div>
              <div class="sub-text">{{ order.customerPhone || '-' }}</div>
            </td>
            <td>{{ order.items?.length || 0 }} sản phẩm</td>
            <td class="price">{{ formatPrice(order.total) }}</td>
            <td>
              <select 
                v-model="order.orderStatus" 
                class="status-select" 
                :class="`status-${order.orderStatus?.toLowerCase()}`" 
                :disabled="order.orderStatus === 'CANCELLED' || order.orderStatus === 'COMPLETED'"
                @change="handleStatusChange(order)"
              >
                <option value="PENDING">Chờ xử lý</option>
                <option value="COMPLETED">Hoàn thành</option>
                <option value="CANCELLED">Đã hủy</option>
              </select>
            </td>
            <td>
              <span :class="['badge', getPaymentBadge(order.paymentStatus)]">
                {{ getPaymentMethodText(order.payments) }}
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
              </div>
            </td>
          </tr>
          <tr v-if="invoices.length === 0">
            <td colspan="7" style="text-align: center; padding: 20px; color: var(--medium-gray);">
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
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import {
  searchInvoices,
  cancelInvoice,
  printInvoiceApi,
  getInvoiceDetail
} from "../../services/posOrderService";

import { getMyActiveSession } from "../../services/posService";
import { useNotification } from '../../composables/useNotification';
import { formatPrice } from '../../utils/helpers';

const { showNotification } = useNotification();

// Quản lý thông tin ca/quầy làm việc của nhân viên
const currentSession = ref(null);
const loadingSession = ref(true);

// State quản lý danh sách hiển thị và phân trang dữ liệu
const loading = ref(false);
const invoices = ref([]);
const totalPages = ref(0);
const totalElements = ref(0);

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
      // Đảm bảo gán chính xác ID phiên đang mở của ngày hôm nay vào bộ lọc
      filter.value.posSessionId = session.posSessionId; 
      await fetchInvoicesData();
    }
  } catch (error) {
    console.error(error);
  } finally {
    loadingSession.value = false;
  }
};
// Gọi API lấy danh sách hóa đơn theo các điều kiện lọc (Server-side Pagination)
const fetchInvoicesData = async () => {
  loading.value = true;

  try {
    console.log("FILTER =", JSON.stringify(filter.value));

    const response = await searchInvoices(filter.value);

    console.log("FULL RESPONSE =", response);

    // Xác định xem data thực sự nằm ở đâu (Phòng trường hợp axios interceptor đã bóc tách sẵn)
    // Nếu response có thuộc tính content thì lấy luôn, không thì lấy từ response.data
    const targetData = response?.content ? response : response?.data;

    console.log("TARGET DATA COMPARED =", targetData);

    // Gán dữ liệu an toàn, nếu hoàn toàn trống thì trả về giá trị mặc định tránh crash
    invoices.value = targetData?.content || [];
    totalPages.value = targetData?.totalPages || 0;
    totalElements.value = targetData?.totalElements || 0;

  } catch (error) {
    console.error("FULL ERROR =", error);
    console.error("ERROR RESPONSE =", error?.response);
    console.error("ERROR DATA =", error?.response?.data);
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
  
  // Reset trạng thái tìm kiếm text cũ trước khi gán mới
  filter.value.invoiceNumber = "";
  filter.value.customerName = "";
  filter.value.customerPhone = "";

  if (!value) return;

  // Tự động nhận diện định dạng đầu vào (Số điện thoại, Mã HD hay Tên khách)
  if (/^\d+$/.test(value)) {
    filter.value.customerPhone = value;
  } else if (value.toUpperCase().startsWith("HD") || value.includes("-")) {
    filter.value.invoiceNumber = value;
  } else {
    filter.value.customerName = value;
  }
};

// Tránh spam liên tục request lên Server khi nhân viên đang gõ chữ
let timeoutId = null;
const debouncedSearch = () => {
  clearTimeout(timeoutId);
  timeoutId = setTimeout(() => {
    parseSearchKeyword();
    filter.value.page = 0; // Đưa về trang đầu khi tìm kiếm mới
    fetchInvoicesData();
  }, 400);
};

// Gọi lại API khi thay đổi trạng thái hoặc kiểu thanh toán trên Select Box
const handleFilterChange = () => {
  filter.value.page = 0;
  fetchInvoicesData();
};

// Xử lý chuyển trang khi nhấn nút Phân trang
const changePage = (newPage) => {
  if (newPage >= 0 && newPage < totalPages.value) {
    filter.value.page = newPage;
    fetchInvoicesData();
  }
};

// Khôi phục bộ lọc về trạng thái ban đầu mặc định của quầy
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
// Thao tác hủy hóa đơn trực tiếp từ giao diện bảng
const handleCancelInvoice = async (order) => {
  const reason = prompt("Nhập lý do hủy hóa đơn này:");
  if (reason === null) return; // Nhân viên bấm nút Hủy bỏ trên prompt
  if (!reason.trim()) {
    showNotification("Bạn bắt buộc phải nhập lý do hủy đơn!", "warning");
    return;
  }

  try {
    await cancelInvoice(order.posOrderId, reason);
    showNotification("Hủy hóa đơn thành công, số lượng sản phẩm đã hoàn trả kho quầy!", "success");
    await fetchInvoicesData(); // Tải lại bảng dữ liệu mới nhất
  } catch (error) {
    console.error("Cancel order error:", error);
    showNotification(error.response?.data?.message || "Lỗi xảy ra khi thực hiện hủy đơn hàng", "error");
  }
};

// Lắng nghe sự thay đổi trạng thái đơn từ Select Box
const handleStatusChange = async (order) => {
  if (order.orderStatus === 'CANCELLED') {
    handleCancelInvoice(order);
    return;
  }
  showNotification(`Hệ thống ghi nhận chuyển trạng thái sang: ${order.orderStatus}`, 'info');
};

// Gọi API lấy dữ liệu chi tiết hóa đơn
const viewDetail = async (orderId) => {
  try {
    const response = await getInvoiceDetail(orderId);
    if (response && response.data) {
      showNotification(`Đang xem chi tiết hóa đơn: ${response.data.invoiceNumber}`, 'info');
      // Tùy biến logic: Hiển thị Modal chi tiết hoặc điều hướng router tại đây
    }
  } catch (error) {
    showNotification("Không thể tải thông tin chi tiết hóa đơn", "error");
  }
};

// Gửi lệnh in hóa đơn xuống Backend
const printInvoice = async (orderId) => {
  try {
    await printInvoiceApi(orderId);
    showNotification(`Đã gửi dữ liệu hóa đơn ID: ${orderId} tới máy in thành công!`, 'success');
  } catch (error) {
    showNotification("Gặp lỗi trong quá trình xử lý lệnh in", "error");
  }
};

// Đổi màu sắc hiển thị Badge Trạng thái thanh toán
const getPaymentBadge = (status) => {
  if (status === 'PAID') return 'badge-success';
  if (status === 'UNPAID') return 'badge-warning';
  return 'badge-danger'; // Trạng thái CANCELLED hoặc lỗi
};

// Nối chuỗi hiển thị toàn bộ các phương thức thanh toán của đơn
const getPaymentMethodText = (payments) => {
  if (!payments || payments.length === 0) return 'Chưa thanh toán';
  return payments.map(p => p.paymentMethod).join(', ');
};

// Format định dạng ngày giờ hiển thị lên banner thông tin phiên
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString('vi-VN');
};
</script>

<style scoped>
/* Thêm CSS cho phần Banner quầy POS ở trên cùng */
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

.session-info-banner.error {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #a8071a;
}

.session-info-banner i {
  font-size: 16px;
}

/* Các thuộc tính CSS cũ của bạn giữ nguyên bên dưới */
.staff-page { animation: fadeIn 0.3s; }
.filters-bar { background: white; padding: 20px; border-radius: 10px; box-shadow: var(--shadow); display: flex; gap: 15px; flex-wrap: wrap; margin-bottom: 20px; }
.search-box { flex: 1; min-width: 260px; display: flex; align-items: center; gap: 10px; background: var(--light-gray); border-radius: 8px; padding: 0 14px; }
.search-box input { border: none; background: transparent; outline: none; padding: 12px 0; width: 100%; }
.filters-bar select { padding: 12px 15px; border: 1px solid var(--border-color); border-radius: 8px; outline: none; background: white; }
.main-text { color: var(--dark-gray); font-weight: 600; }
.sub-text { color: var(--medium-gray); font-size: 12px; margin-top: 3px; }
.price { color: var(--primary-red); font-weight: 700; }
.status-select { padding: 8px 12px; border: none; border-radius: 20px; font-size: 12px; font-weight: 600; outline: none; }
.status-pending { background: #fff3cd; color: #856404; }
.status-completed { background: #d4edda; color: #155724; }
.status-cancelled { background: #f8d7da; color: #721c24; }
.action-buttons { display: flex; gap: 8px; }
.action-btn { width: 35px; height: 35px; border: none; border-radius: 8px; color: white; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.view-btn { background: #17a2b8; }
.edit-btn { background: #dc3545; }
.print-btn { background: #28a745; }
.pagination-bar { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; background: white; border-radius: 10px; margin-top: 15px; box-shadow: var(--shadow); }
.pagination-info { font-size: 14px; color: var(--dark-gray); }
.pagination-buttons { display: flex; align-items: center; gap: 12px; }
.pagination-buttons button { padding: 8px 14px; border: 1px solid var(--border-color); background: white; border-radius: 6px; cursor: pointer; transition: 0.2s; }
.pagination-buttons button:hover:not(:disabled) { background: var(--light-gray); }
.pagination-buttons button:disabled { opacity: 0.5; cursor: not-allowed; }
.page-number { font-size: 14px; font-weight: 600; }
.loading-text { text-align: center; padding: 30px; color: var(--medium-gray); font-weight: 500; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
</style>