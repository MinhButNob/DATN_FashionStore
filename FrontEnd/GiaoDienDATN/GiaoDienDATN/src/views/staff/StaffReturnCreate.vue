<template>
  <div class="staff-return-create">
    <div class="page-header">
      <h2><i class="fas fa-undo-alt"></i> Tạo phiếu hoàn tiền</h2>
      <button class="btn btn-secondary" @click="goBack">
        <i class="fas fa-arrow-left"></i> Quay lại
      </button>
    </div>

    <!-- Bước 1: Tìm đơn hàng theo SĐT -->
    <div class="step-section" v-if="!selectedOrder">
      <div class="search-order-box">
        <h3>Bước 1: Tìm đơn hàng</h3>
        <p class="hint">Nhập số điện thoại khách hàng để tìm đơn hàng đã mua tại quầy</p>
        <div class="search-row">
          <input 
            v-model="searchPhone" 
            type="text" 
            placeholder="Nhập số điện thoại khách hàng" 
            @keyup.enter="searchOrders"
          />
          <button class="btn btn-primary" @click="searchOrders" :disabled="loading">
            <i class="fas fa-search"></i> Tìm
          </button>
        </div>
        <div v-if="loading" class="loading-state">
          <i class="fas fa-spinner fa-spin"></i> Đang tìm kiếm...
        </div>
        <div v-else-if="orderSearchResults.length > 0" class="order-results">
          <div 
            v-for="order in orderSearchResults" 
            :key="order.posOrderId"
            class="order-result-item"
            @click="selectOrder(order)"
          >
            <div class="order-info">
              <strong>{{ order.posOrderInvoiceNumber || order.invoiceNumber }}</strong>
              <span>{{ order.posOrderCustomerName || order.customerName || 'Khách lẻ' }}</span>
              <span class="price">{{ formatPrice(order.posOrderTotalAmount || order.totalAmount) }}</span>
              <span class="order-date">{{ formatDate(order.posOrderCreatedAt || order.createdAt) }}</span>
            </div>
            <span class="badge" :class="`badge-${(order.posOrderStatus || order.orderStatus)?.toLowerCase()}`">
              {{ (order.posOrderStatus || order.orderStatus) === 'COMPLETED' ? 'Hoàn thành' : (order.posOrderStatus || order.orderStatus) }}
            </span>
          </div>
        </div>
        <div v-else-if="searched && orderSearchResults.length === 0" class="empty-state">
          <i class="fas fa-search"></i>
          <p>Không tìm thấy đơn hàng nào với SĐT: <strong>{{ searchPhone }}</strong></p>
          <p class="hint">Vui lòng kiểm tra lại số điện thoại</p>
        </div>
      </div>
    </div>

    <!-- Bước 2+3: Nhập thông tin phiếu -->
    <div class="step-section" v-else>
      <!-- Thông tin đơn hàng -->
      <div class="order-summary">
        <h3><i class="fas fa-file-invoice"></i> Đơn hàng: {{ selectedOrder.posOrderInvoiceNumber || selectedOrder.invoiceNumber }}</h3>
        <div class="summary-grid">
          <div><strong>Khách hàng:</strong> {{ selectedOrder.posOrderCustomerName || selectedOrder.customerName || 'Khách lẻ' }}</div>
          <div><strong>SĐT:</strong> {{ selectedOrder.posOrderCustomerPhone || selectedOrder.customerPhone || '---' }}</div>
          <div><strong>Tổng tiền:</strong> <span class="price">{{ formatPrice(selectedOrder.posOrderTotalAmount || selectedOrder.totalAmount) }}</span></div>
          <div><strong>Ngày mua:</strong> {{ formatDate(selectedOrder.posOrderCreatedAt || selectedOrder.createdAt) }}</div>
        </div>
      </div>

      <!-- ✅ Chỉ giữ hoàn tiền mặt -->
      <div class="return-type-select">
        <h3>Bước 2: Xác nhận hoàn tiền</h3>
        <div class="type-buttons">
          <button 
            class="type-btn active"
            disabled
          >
            <i class="fas fa-money-bill-wave"></i>
            Hoàn tiền mặt
          </button>
        </div>
      </div>

      <!-- Chọn sản phẩm trả lại -->
      <div class="return-items-section">
        <h3>Bước 3: Chọn sản phẩm trả lại</h3>
        <p class="hint" v-if="orderVoucher">
          <i class="fas fa-ticket-alt"></i> 
          Đơn hàng đã áp dụng mã giảm giá: <strong>{{ orderVoucher.voucherCode }}</strong> 
          (Giảm {{ orderVoucher.voucherDiscountPercent || 0 }}%)
        </p>
        <div v-if="orderItems.length === 0" class="empty-state">
          <i class="fas fa-box-open"></i>
          <p>Không có sản phẩm nào trong đơn hàng này</p>
        </div>
        <div class="order-items-list" v-else>
          <div 
            v-for="item in orderItems" 
            :key="item.posOrderItemId"
            class="order-item-check"
          >
            <input 
              type="checkbox" 
              :value="item.posOrderItemId"
              v-model="selectedItemIds"
              @change="updateReturnItems"
            />
            <div class="item-info">
              <div><strong>{{ item.productName || 'Sản phẩm' }}</strong></div>
              <div class="variant-info">
                <span>Size: {{ item.sizeName || item.size || 'N/A' }}</span>
                <span>Màu: {{ item.colorName || item.color || 'N/A' }}</span>
                <span>Số lượng: {{ item.quantity }}</span>
              </div>
              <!-- HIỂN THỊ GIÁ CHI TIẾT -->
              <div class="price-detail">
                <div class="price-row">
                  <span>Giá gốc:</span>
                  <span class="original-price">{{ formatPrice(item.unitPrice) }}</span>
                </div>
                <div class="price-row" v-if="item.discountPerUnit > 0">
                  <span>Giảm giá:</span>
                  <span class="discount-amount">-{{ formatPrice(item.discountPerUnit) }}</span>
                </div>
                <div class="price-row total-row">
                  <span>Giá sau giảm:</span>
                  <span class="final-price">{{ formatPrice(item.finalUnitPrice) }}</span>
                </div>
                <div class="price-row total-row" v-if="selectedItemIds.includes(item.posOrderItemId)">
                  <span>Thành tiền hoàn (x{{ returnQuantities[item.posOrderItemId] || item.quantity }}):</span>
                  <span class="refund-total">{{ formatPrice((item.finalUnitPrice || 0) * (returnQuantities[item.posOrderItemId] || item.quantity)) }}</span>
                </div>
              </div>
            </div>
            <div class="return-qty" v-if="selectedItemIds.includes(item.posOrderItemId)">
              <label>Số lượng trả:</label>
              <input 
                type="number" 
                v-model.number="returnQuantities[item.posOrderItemId]" 
                :max="item.quantity"
                min="1"
                @change="updateReturnItems"
              />
            </div>
            <!-- TÙY CHỌN TÌNH TRẠNG SẢN PHẨM -->
            <div class="condition-select" v-if="selectedItemIds.includes(item.posOrderItemId)">
              <label>Tình trạng:</label>
              <select v-model="itemConditions[item.posOrderItemId]">
                <option value="GOOD">Còn nguyên vẹn</option>
                <option value="DAMAGED">Bị hỏng/hư</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <!-- Lý do và ghi chú -->
      <div class="reason-section">
        <h3>Bước 4: Lý do và ghi chú</h3>
        <div class="form-group">
          <label>Lý do hoàn tiền <span class="required">*</span></label>
          <textarea v-model="reason" rows="3" placeholder="Nhập lý do hoàn tiền..."></textarea>
        </div>
        <div class="form-group">
          <label>Ghi chú</label>
          <textarea v-model="notes" rows="2" placeholder="Ghi chú thêm (nếu có)..."></textarea>
        </div>
      </div>

      <!-- TÓM TẮT PHIẾU -->
      <div class="summary-section">
        <h3>Tóm tắt phiếu hoàn tiền</h3>
        
        <!-- Hiển thị thông tin voucher nếu có -->
        <div class="voucher-summary" v-if="orderVoucher">
          <div class="voucher-info-box">
            <i class="fas fa-ticket-alt"></i>
            <div>
              <span>Mã: <strong>{{ orderVoucher.voucherCode }}</strong></span>
              <span>Giảm: <strong>{{ orderVoucher.voucherDiscountPercent || 0 }}%</strong></span>
              <span v-if="orderVoucher.voucherDiscountAmount">Tối đa: {{ formatPrice(orderVoucher.voucherDiscountAmount) }}</span>
            </div>
          </div>
        </div>

        <div class="summary-grid">
          <div><strong>Loại:</strong> Hoàn tiền mặt</div>
          <div><strong>Số sản phẩm trả:</strong> {{ returnItems.length }}</div>
          <div>
            <strong>Tổng giá gốc:</strong> 
            <span class="price">{{ formatPrice(totalOriginalAmount) }}</span>
          </div>
          <div v-if="totalDiscountAllocated > 0" class="discount-row">
            <strong>Giảm giá:</strong> 
            <span class="text-danger">-{{ formatPrice(totalDiscountAllocated) }}</span>
            <span class="discount-percent">({{ discountPercent }}%)</span>
          </div>
          <div class="total-refund-row">
            <strong>Tổng tiền hoàn <span class="highlight">(đã trừ giảm giá)</span>:</strong> 
            <span class="refund-amount">{{ formatPrice(totalRefund) }}</span>
          </div>
        </div>

        <!-- Chi tiết từng sản phẩm hoàn -->
        <div class="return-items-summary" v-if="returnItems.length > 0">
          <h4>Chi tiết sản phẩm hoàn:</h4>
          <div v-for="item in returnItems" :key="item.orderItemId" class="return-item-summary">
            <span class="item-name">{{ item.productName }}</span>
            <span class="item-qty">x{{ item.quantity }}</span>
            <span class="item-price">{{ formatPrice(item.finalUnitPrice) }}</span>
            <span class="item-total">{{ formatPrice((item.finalUnitPrice || 0) * item.quantity) }}</span>
            <span :class="['condition-badge', item.condition === 'GOOD' ? 'badge-success' : 'badge-danger']">
              {{ item.condition === 'GOOD' ? '✅ Nguyên' : '⚠️ Hỏng' }}
            </span>
          </div>
        </div>

        <div class="action-buttons">
          <button class="btn btn-secondary" @click="resetAll">Hủy bỏ</button>
          <button class="btn btn-primary" @click="submitReturn" :disabled="submitting || !isValid">
            <i v-if="!submitting" class="fas fa-check"></i>
            <i v-else class="fas fa-spinner fa-spin"></i>
            {{ submitting ? 'Đang xử lý...' : 'Xác nhận hoàn tiền' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useNotification } from '../../composables/useNotification';
import { formatPrice } from '../../utils/helpers';
import posReturnExchangeService from '../../services/posReturnExchangeService';
import api from '../../services/api';

const router = useRouter();
const route = useRoute();
const { showNotification } = useNotification();

// ==================== STATE ====================
const loading = ref(false);
const submitting = ref(false);
const searched = ref(false);
const searchPhone = ref("");
const orderSearchResults = ref([]);
const selectedOrder = ref(null);
const orderItems = ref([]);
const orderVoucher = ref(null);
const selectedItemIds = ref([]);
const returnQuantities = ref({});
const itemConditions = ref({});
const returnItems = ref([]);
const reason = ref("");
const notes = ref("");

// ==================== COMPUTED ====================
const totalOriginalAmount = computed(() => {
  return returnItems.value.reduce((sum, item) => sum + ((item.unitPrice || 0) * (item.quantity || 0)), 0);
});

const totalDiscountAllocated = computed(() => {
  return returnItems.value.reduce((sum, item) => sum + (((item.discountPerUnit || 0) || 0) * (item.quantity || 0)), 0);
});

const totalRefund = computed(() => {
  return returnItems.value.reduce((sum, item) => sum + (((item.finalUnitPrice || 0) || 0) * (item.quantity || 0)), 0);
});

const discountPercent = computed(() => {
  if (totalOriginalAmount.value === 0) return 0;
  return Math.round((totalDiscountAllocated.value / totalOriginalAmount.value) * 100);
});

const isValid = computed(() => {
  if (!selectedOrder.value) return false;
  if (returnItems.value.length === 0) return false;
  if (!reason.value.trim()) return false;
  
  for (const item of returnItems.value) {
    if (!itemConditions.value[item.orderItemId]) {
      return false;
    }
  }
  
  return true;
});

// ==================== METHODS ====================

// Tìm kiếm đơn hàng theo số điện thoại
const searchOrders = async () => {
  if (!searchPhone.value.trim()) {
    showNotification("Vui lòng nhập số điện thoại!", "warning");
    return;
  }
  
  loading.value = true;
  searched.value = true;
  try {
    const response = await api.get('/pos/orders/search-by-phone', {
      params: { phone: searchPhone.value.trim() }
    });
    
    let data = response?.data?.data || response?.data || response;
    
    if (data && typeof data === 'object' && !Array.isArray(data)) {
      if (data.content) data = data.content;
      else if (data.items) data = data.items;
      else data = [data];
    }
    
    orderSearchResults.value = Array.isArray(data) ? data : [];
    
    if (orderSearchResults.value.length === 0) {
      showNotification(`Không tìm thấy đơn hàng nào với SĐT: ${searchPhone.value}`, "info");
    } else {
      showNotification(`Tìm thấy ${orderSearchResults.value.length} đơn hàng!`, "success");
    }
  } catch (error) {
    console.error("Search orders error:", error);
    showNotification(error.response?.data?.message || "Không thể tìm kiếm đơn hàng", "error");
    orderSearchResults.value = [];
  } finally {
    loading.value = false;
  }
};

// Chọn đơn hàng để hoàn trả
const selectOrder = async (order) => {
  console.log("🛒 Selected order:", order);
  
  selectedOrder.value = order;
  selectedItemIds.value = [];
  returnQuantities.value = {};
  itemConditions.value = {};
  returnItems.value = [];
  reason.value = "";
  notes.value = "";
  orderVoucher.value = null;
  
  await loadOrderItems(order.posOrderId);
  await loadOrderVoucher(order.posOrderId);
  
  console.log("📦 Order items after load:", orderItems.value);
  console.log("📦 Voucher:", orderVoucher.value);
};

// Lấy danh sách sản phẩm của đơn hàng
const loadOrderItems = async (orderId) => {
  try {
    console.log("📦 Loading items for order:", orderId);
    
    const response = await api.get(`/pos/orders/${orderId}/detail`);
    console.log("📦 Full API Response:", response);
    
    let data = response?.data?.data || response?.data || response;
    console.log("📦 Parsed data:", data);
    
    let items = [];
    if (data && typeof data === 'object') {
      if (data.items && Array.isArray(data.items)) {
        items = data.items;
      } else if (Array.isArray(data)) {
        items = data;
      } else if (data.content && Array.isArray(data.content)) {
        items = data.content;
      }
    }
    
    console.log("📦 Items array length:", items.length);
    
    if (items.length > 0) {
      orderItems.value = items.map(item => {
        const unitPrice = item.unitPrice || item.posOrderItemUnitPrice || 0;
        const discountPerUnit = item.discountPerUnit || 0;
        const quantity = item.quantity || item.posOrderItemQuantity || 1;
        const productName = item.productName || 'Sản phẩm';
        const colorName = item.colorName || item.color || 'N/A';
        const sizeName = item.sizeName || item.size || 'N/A';
        
        return {
          posOrderItemId: item.posOrderItemId || item.id,
          productDetailId: item.productDetailId || item.posOrderItemProductDetailId,
          productName: productName,
          colorName: colorName,
          sizeName: sizeName,
          sku: item.sku || '',
          quantity: quantity,
          unitPrice: unitPrice,
          discount: item.discount || 0,
          total: item.total || (unitPrice * quantity),
          discountPerUnit: discountPerUnit,
          finalUnitPrice: unitPrice - discountPerUnit,
          ...item
        };
      });
      console.log("✅ Loaded items:", orderItems.value);
    } else {
      console.warn("⚠️ No items found for order:", orderId);
      orderItems.value = [];
      showNotification("Không tìm thấy sản phẩm trong đơn hàng này", "warning");
    }
  } catch (error) {
    console.error("❌ Load order items error:", error);
    showNotification(error.response?.data?.message || "Không thể tải sản phẩm", "error");
    orderItems.value = [];
  }
};

// Lấy thông tin voucher của đơn hàng
const loadOrderVoucher = async (orderId) => {
  try {
    const response = await api.get(`/pos/${orderId}/voucher`);
    const data = response?.data?.data || response?.data;
    if (data) {
      orderVoucher.value = data;
    }
  } catch (error) {
    console.warn("Không lấy được voucher:", error);
    orderVoucher.value = null;
  }
};

// Cập nhật danh sách sản phẩm cần hoàn
const updateReturnItems = () => {
  returnItems.value = selectedItemIds.value
    .filter(id => {
      const qty = returnQuantities.value[id] || 0;
      return qty > 0;
    })
    .map(id => {
      const item = orderItems.value.find(i => i.posOrderItemId === id);
      if (!item) return null;
      
      const qty = returnQuantities.value[id] || 1;
      const condition = itemConditions.value[id] || 'GOOD';
      const discountPerUnit = item.discountPerUnit || 0;
      const unitPrice = item.unitPrice || 0;
      
      return {
        orderItemId: id,
        productDetailId: item.productDetailId,
        quantity: qty,
        unitPrice: unitPrice,
        discountPerUnit: discountPerUnit,
        finalUnitPrice: unitPrice - discountPerUnit,
        productName: item.productName || 'Sản phẩm',
        sizeName: item.sizeName || item.size || 'N/A',
        colorName: item.colorName || item.color || 'N/A',
        condition: condition
      };
    })
    .filter(item => item !== null);
};

// Reset tất cả
const resetAll = () => {
  if (confirm("Bạn có chắc muốn hủy bỏ tất cả?")) {
    selectedOrder.value = null;
    searchPhone.value = "";
    orderSearchResults.value = [];
    selectedItemIds.value = [];
    returnQuantities.value = {};
    itemConditions.value = {};
    returnItems.value = [];
    reason.value = "";
    notes.value = "";
    orderVoucher.value = null;
  }
};

// Gửi yêu cầu hoàn tiền
const submitReturn = async () => {
  if (!isValid.value) return;
  
  submitting.value = true;
  try {
    const payload = {
      orderId: selectedOrder.value.posOrderId,
      customerName: selectedOrder.value.posOrderCustomerName || selectedOrder.value.customerName || 'Khách lẻ',
      customerPhone: selectedOrder.value.posOrderCustomerPhone || selectedOrder.value.customerPhone || '',
      type: "RETURN",
      reason: reason.value,
      notes: notes.value,
      returnItems: returnItems.value.map(item => ({
        orderItemId: item.orderItemId,
        productDetailId: item.productDetailId,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        discountPerUnit: item.discountPerUnit || 0,
        finalUnitPrice: item.finalUnitPrice,
        condition: item.condition || 'GOOD'
      }))
    };
    
    const response = await posReturnExchangeService.createReturnExchange(payload);
    const result = response?.data?.data || response?.data;
    
    let message = `Phiếu ${result.referenceCode} đã tạo! Hoàn ${formatPrice(result.totalRefundAmount)}`;
    if (result.totalDiscountAllocated > 0) {
      message += ` (Đã trừ giảm giá ${formatPrice(result.totalDiscountAllocated)})`;
    }
    showNotification(message, 'success');
    
    router.push('/staff/returns');
  } catch (error) {
    console.error("Submit return error:", error);
    showNotification(error.response?.data?.message || "Không thể tạo phiếu hoàn tiền", "error");
  } finally {
    submitting.value = false;
  }
};

// Quay lại
const goBack = () => {
  router.back();
};

// Format ngày tháng
const formatDate = (date) => {
  if (!date) return '';
  return new Date(date).toLocaleString('vi-VN');
};

// ==================== LIFECYCLE ====================
onMounted(async () => {
  const orderId = route.query.orderId;
  const phone = route.query.phone || '';
  
  console.log("🔍 Mounted with orderId:", orderId, "phone:", phone);
  
  if (orderId) {
    try {
      const response = await api.get(`/pos/orders/${orderId}/detail`);
      console.log("📦 Order detail response:", response);
      
      let data = response?.data?.data || response?.data || response;
      console.log("📦 Order detail data:", data);
      
      if (data) {
        if (data.order) {
          selectedOrder.value = data.order;
          console.log("✅ Selected order:", selectedOrder.value);
        }
        
        let items = [];
        if (data.items && Array.isArray(data.items)) {
          items = data.items;
        } else if (Array.isArray(data)) {
          items = data;
        }
        
        if (items.length > 0) {
          orderItems.value = items.map(item => {
            const unitPrice = item.unitPrice || item.posOrderItemUnitPrice || 0;
            const discountPerUnit = item.discountPerUnit || 0;
            const quantity = item.quantity || item.posOrderItemQuantity || 1;
            
            return {
              posOrderItemId: item.posOrderItemId || item.id,
              productDetailId: item.productDetailId || item.posOrderItemProductDetailId,
              productName: item.productName || 'Sản phẩm',
              colorName: item.colorName || item.color || 'N/A',
              sizeName: item.sizeName || item.size || 'N/A',
              sku: item.sku || '',
              quantity: quantity,
              unitPrice: unitPrice,
              discount: item.discount || 0,
              total: item.total || (unitPrice * quantity),
              discountPerUnit: discountPerUnit,
              finalUnitPrice: unitPrice - discountPerUnit,
              ...item
            };
          });
          console.log("✅ Loaded items from query:", orderItems.value);
        } else {
          console.warn("⚠️ No items found for order:", orderId);
          orderItems.value = [];
        }
        
        await loadOrderVoucher(orderId);
        
        if (phone) {
          searchPhone.value = phone;
          await searchOrders();
        }
      }
    } catch (error) {
      console.error("❌ Load order by id error:", error);
      showNotification(error.response?.data?.message || "Không thể tải đơn hàng", "error");
    }
  }
});
</script>

<style scoped>
/* ============================================================
   STAFF RETURN CREATE - COMPLETE CSS
   ============================================================ */

/* ---------- RESET & BASE ---------- */
.staff-return-create {
  animation: fadeIn 0.4s ease;
  padding: 0 0 30px 0;
}

/* ---------- PAGE HEADER ---------- */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding: 20px 24px;
  background: white;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.page-header h2 {
  color: #1a1a2e;
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header h2 i {
  color: #e63946;
}

/* ---------- BUTTONS ---------- */
.btn {
  padding: 10px 22px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.25s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #e63946, #d62828);
  color: white;
  box-shadow: 0 2px 8px rgba(230, 57, 70, 0.25);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(230, 57, 70, 0.35);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-secondary {
  background: #f0f0f2;
  color: #555;
}

.btn-secondary:hover {
  background: #e5e5e8;
  transform: translateY(-1px);
}

/* ---------- STEP SECTIONS ---------- */
.step-section {
  background: white;
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f2;
  transition: all 0.3s ease;
}

.step-section:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.step-section h3 {
  color: #1a1a2e;
  font-size: 17px;
  font-weight: 700;
  margin: 0 0 14px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.step-section h3 i {
  color: #e63946;
}

/* ---------- HINT TEXT ---------- */
.hint {
  color: #888;
  font-size: 13.5px;
  margin: 0 0 14px 0;
  padding: 10px 14px;
  background: #f8f9fb;
  border-radius: 8px;
  border-left: 3px solid #e63946;
}

.hint i {
  color: #e63946;
  margin-right: 6px;
}

/* ---------- SEARCH ORDER ---------- */
.search-order-box {
  padding: 4px 0;
}

.search-row {
  display: flex;
  gap: 12px;
}

.search-row input {
  flex: 1;
  padding: 12px 18px;
  border: 2px solid #e8e8ef;
  border-radius: 10px;
  font-size: 15px;
  outline: none;
  transition: all 0.25s ease;
  background: #fafafc;
}

.search-row input:focus {
  border-color: #e63946;
  background: white;
  box-shadow: 0 0 0 4px rgba(230, 57, 70, 0.08);
}

.search-row input::placeholder {
  color: #bbb;
}

/* ---------- LOADING & EMPTY STATE ---------- */
.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 20px;
  color: #999;
}

.loading-state i,
.empty-state i {
  font-size: 36px;
  margin-bottom: 14px;
  opacity: 0.6;
}

.loading-state i {
  color: #e63946;
}

.empty-state i {
  color: #ccc;
}

/* ---------- ORDER RESULTS ---------- */
.order-results {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 6px;
}

.order-results::-webkit-scrollbar {
  width: 6px;
}

.order-results::-webkit-scrollbar-track {
  background: #f0f0f2;
  border-radius: 10px;
}

.order-results::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 10px;
}

.order-result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border: 2px solid #e8e8ef;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  background: white;
}

.order-result-item:hover {
  border-color: #e63946;
  background: #fef5f5;
  transform: translateX(4px);
  box-shadow: 0 2px 12px rgba(230, 57, 70, 0.08);
}

.order-info {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.order-info strong {
  color: #1a1a2e;
  font-size: 15px;
  font-weight: 700;
}

.order-info .price {
  color: #e63946;
  font-weight: 700;
  font-size: 15px;
}

.order-date {
  color: #999;
  font-size: 13px;
}

/* ---------- BADGES ---------- */
.badge {
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.badge-completed {
  background: #d4edda;
  color: #155724;
}

.badge-pending {
  background: #fff3cd;
  color: #856404;
}

.badge-cancelled {
  background: #f8d7da;
  color: #721c24;
}

/* ---------- ORDER SUMMARY ---------- */
.order-summary {
  background: #f8f9fb;
  border-radius: 12px;
  padding: 18px 22px;
  margin-bottom: 22px;
  border: 1px solid #eef0f2;
}

.order-summary h3 {
  margin: 0 0 14px 0;
  font-size: 16px;
  color: #1a1a2e;
}

.order-summary h3 i {
  color: #e63946;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px 24px;
}

.summary-grid div {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #555;
}

.summary-grid strong {
  color: #1a1a2e;
  font-weight: 600;
}

.summary-grid .price {
  color: #e63946;
  font-weight: 700;
}

/* ---------- RETURN TYPE SELECT ---------- */
.return-type-select {
  margin-bottom: 24px;
}

.type-buttons {
  display: flex;
  gap: 14px;
}

.type-btn {
  flex: 1;
  padding: 16px 20px;
  border: 2.5px solid #e8e8ef;
  border-radius: 12px;
  background: white;
  cursor: not-allowed;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  transition: all 0.25s ease;
  color: #555;
  opacity: 0.8;
}

.type-btn i {
  font-size: 20px;
  opacity: 0.7;
}

.type-btn.active {
  border-color: #e63946;
  background: #fef5f5;
  color: #e63946;
  box-shadow: 0 2px 12px rgba(230, 57, 70, 0.12);
}

.type-btn.active i {
  opacity: 1;
}

/* ---------- ORDER ITEMS LIST ---------- */
.order-items-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 500px;
  overflow-y: auto;
  padding-right: 6px;
}

.order-items-list::-webkit-scrollbar {
  width: 6px;
}

.order-items-list::-webkit-scrollbar-track {
  background: #f0f0f2;
  border-radius: 10px;
}

.order-items-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 10px;
}

.order-item-check {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 18px;
  border: 2px solid #e8e8ef;
  border-radius: 12px;
  transition: all 0.25s ease;
  background: white;
  flex-wrap: wrap;
}

.order-item-check:hover {
  border-color: #d0d0d8;
}

.order-item-check input[type="checkbox"] {
  width: 20px;
  height: 20px;
  margin-top: 2px;
  cursor: pointer;
  accent-color: #e63946;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 200px;
}

.item-info > div:first-child {
  font-weight: 600;
  color: #1a1a2e;
  font-size: 15px;
  margin-bottom: 4px;
}

.variant-info {
  display: flex;
  gap: 14px;
  font-size: 13px;
  color: #777;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

/* ---------- PRICE DETAIL ---------- */
.price-detail {
  margin-top: 6px;
  padding: 8px 12px;
  background: #f8f9fb;
  border-radius: 8px;
  font-size: 13px;
  max-width: 320px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
  gap: 16px;
}

.price-row .original-price {
  color: #666;
}

.price-row .discount-amount {
  color: #dc3545;
  font-weight: 600;
}

.price-row .final-price {
  color: #e63946;
  font-weight: 700;
}

.price-row.total-row {
  border-top: 1px dashed #ddd;
  padding-top: 4px;
  margin-top: 2px;
}

.price-row .refund-total {
  color: #e63946;
  font-weight: 700;
  font-size: 14px;
}

/* ---------- RETURN QTY ---------- */
.return-qty {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.return-qty label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  white-space: nowrap;
}

.return-qty input {
  width: 64px;
  padding: 6px 10px;
  border: 2px solid #e8e8ef;
  border-radius: 8px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  outline: none;
  transition: all 0.2s ease;
}

.return-qty input:focus {
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.08);
}

/* ---------- CONDITION SELECT ---------- */
.condition-select {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.condition-select label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  white-space: nowrap;
}

.condition-select select {
  padding: 6px 12px;
  border: 2px solid #e8e8ef;
  border-radius: 8px;
  font-size: 12px;
  outline: none;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.condition-select select:focus {
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.08);
}

/* ---------- REASON SECTION ---------- */
.reason-section {
  margin-top: 6px;
}

.reason-section .form-group {
  margin-bottom: 16px;
}

.reason-section label {
  display: block;
  font-weight: 600;
  margin-bottom: 6px;
  color: #444;
  font-size: 14px;
}

.reason-section .required {
  color: #e63946;
  margin-left: 2px;
}

.reason-section textarea {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e8e8ef;
  border-radius: 10px;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
  outline: none;
  transition: all 0.25s ease;
  background: #fafafc;
}

.reason-section textarea:focus {
  border-color: #e63946;
  background: white;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.08);
}

.reason-section textarea::placeholder {
  color: #bbb;
}

/* ---------- VOUCHER SUMMARY ---------- */
.voucher-summary {
  margin-bottom: 18px;
}

.voucher-info-box {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff3cd;
  border: 1px solid #ffeeba;
  border-radius: 10px;
  padding: 12px 18px;
}

.voucher-info-box i {
  color: #856404;
  font-size: 22px;
}

.voucher-info-box div {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 14px;
  color: #856404;
}

.voucher-info-box div strong {
  color: #533f03;
}

/* ---------- SUMMARY SECTION ---------- */
.summary-section {
  background: #f8f9fb;
  border-radius: 14px;
  padding: 24px 28px;
  margin-top: 10px;
  border: 2px solid #eef0f2;
}

.summary-section h3 {
  margin: 0 0 18px 0;
  font-size: 18px;
  color: #1a1a2e;
}

.summary-section .summary-grid {
  margin-bottom: 6px;
}

.summary-section .summary-grid .discount-row {
  color: #dc3545;
}

.summary-section .summary-grid .total-refund-row {
  border-top: 2px solid #e63946;
  padding-top: 12px;
  margin-top: 8px;
}

.summary-section .summary-grid .total-refund-row .highlight {
  font-weight: 400;
  color: #666;
  font-size: 13px;
}

.summary-section .summary-grid .refund-amount {
  color: #e63946;
  font-weight: 800;
  font-size: 20px;
}

.discount-percent {
  font-size: 12px;
  color: #856404;
  background: #fff3cd;
  padding: 2px 10px;
  border-radius: 20px;
  margin-left: 6px;
  font-weight: 600;
}

.text-danger {
  color: #dc3545;
}

.text-success {
  color: #28a745;
}

/* ---------- RETURN ITEMS SUMMARY ---------- */
.return-items-summary {
  margin-top: 18px;
  padding: 16px 18px;
  background: white;
  border-radius: 10px;
  border: 1px solid #eef0f2;
}

.return-items-summary h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #555;
  font-weight: 600;
}

.return-item-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f2;
  font-size: 13px;
  flex-wrap: wrap;
}

.return-item-summary:last-child {
  border-bottom: none;
}

.return-item-summary .item-name {
  flex: 1;
  font-weight: 500;
  color: #1a1a2e;
}

.return-item-summary .item-qty {
  color: #666;
  font-weight: 500;
}

.return-item-summary .item-price {
  color: #666;
}

.return-item-summary .item-total {
  color: #e63946;
  font-weight: 700;
  min-width: 80px;
  text-align: right;
}

.condition-badge {
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.condition-badge.badge-success {
  background: #d4edda;
  color: #155724;
}

.condition-badge.badge-danger {
  background: #f8d7da;
  color: #721c24;
}

/* ---------- ACTION BUTTONS ---------- */
.action-buttons {
  display: flex;
  gap: 14px;
  justify-content: flex-end;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 2px solid #eef0f2;
}

/* ---------- ANIMATIONS ---------- */
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

/* ---------- RESPONSIVE ---------- */
@media (max-width: 1024px) {
  .step-section {
    padding: 22px 24px;
  }
}

@media (max-width: 768px) {
  .step-section {
    padding: 18px 16px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 14px;
    text-align: center;
    padding: 16px 20px;
  }
  
  .page-header h2 {
    font-size: 19px;
  }
  
  .type-buttons {
    flex-direction: column;
  }
  
  .search-row {
    flex-direction: column;
  }
  
  .order-item-check {
    padding: 14px 16px;
  }
  
  .order-result-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .order-info {
    gap: 10px;
  }
  
  .summary-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .action-buttons .btn {
    width: 100%;
    justify-content: center;
  }
  
  .voucher-info-box {
    flex-direction: column;
    text-align: center;
  }
  
  .voucher-info-box div {
    justify-content: center;
    gap: 8px;
    font-size: 12px;
  }
  
  .return-item-summary {
    flex-wrap: wrap;
    gap: 6px 12px;
  }
  
  .return-item-summary .item-total {
    min-width: 60px;
  }
}

@media (max-width: 480px) {
  .step-section {
    padding: 14px 12px;
  }
  
  .page-header h2 {
    font-size: 17px;
  }
  
  .order-item-check {
    padding: 12px 14px;
  }
  
  .variant-info {
    gap: 8px;
    font-size: 12px;
  }
  
  .price-detail {
    font-size: 12px;
    padding: 6px 10px;
  }
}
</style>