<!-- views/admin/AdminOrders.vue -->
<template>
  <div class="admin-orders">
    <div class="admin-header">
      <h1 class="page-title">Quản lý đơn hàng</h1>
      <div class="header-actions">
        <button class="btn-refresh" @click="fetchOrders">
          <i class="fas fa-sync-alt"></i> Làm mới
        </button>
      </div>
    </div>

    <!-- ===== THỐNG KÊ NÂNG CAO ===== -->
    <div class="stats-grid" v-if="statistics">
      <!-- Cột 1: Tổng đơn hàng -->
      <div class="stat-card">
        <div class="stat-icon blue">
          <i class="fas fa-shopping-bag"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.totalOrders || 0 }}</span>
          <span class="stat-label">Tổng đơn hàng</span>
        </div>
      </div>

      <!-- Cột 2: Chờ xử lý -->
      <div class="stat-card">
        <div class="stat-icon yellow">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.pendingOrders || 0 }}</span>
          <span class="stat-label">Chờ xử lý</span>
        </div>
      </div>

      <!-- Cột 3: Hoàn thành -->
      <div class="stat-card">
        <div class="stat-icon green">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.completedOrders || 0 }}</span>
          <span class="stat-label">Hoàn thành</span>
        </div>
      </div>

      <!-- ===== THỐNG KÊ DOANH THU ===== -->
      <div class="stat-card stat-card-highlight">
        <div class="stat-icon purple">
          <i class="fas fa-chart-line"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number stat-revenue">{{ formatPrice(statistics.totalRevenue) }}</span>
          <span class="stat-label">💰 Tổng doanh thu</span>
        </div>
      </div>

      <div class="stat-card stat-card-highlight">
        <div class="stat-icon teal">
          <i class="fas fa-laptop"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number stat-revenue">{{ formatPrice(statistics.onlineRevenue) }}</span>
          <span class="stat-label">🛒 Doanh thu online</span>
        </div>
      </div>

      <div class="stat-card stat-card-highlight">
        <div class="stat-icon orange">
          <i class="fas fa-undo-alt"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number stat-return">{{ formatPrice(statistics.returnRefundAmount) }}</span>
          <span class="stat-label">↩️ Tiền hoàn trả</span>
        </div>
      </div>

      <div class="stat-card stat-card-highlight">
        <div class="stat-icon success">
          <i class="fas fa-hand-holding-usd"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number stat-net">{{ formatPrice(statistics.netRevenue) }}</span>
          <span class="stat-label">📊 Doanh thu thực tế</span>
        </div>
      </div>

      <!-- ===== THỐNG KÊ HOÀN HÀNG ===== -->
      <div class="stat-card stat-card-return">
        <div class="stat-icon return">
          <i class="fas fa-clipboard-list"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.returnRequested || 0 }}</span>
          <span class="stat-label">📋 Yêu cầu hoàn</span>
        </div>
      </div>

      <div class="stat-card stat-card-return">
        <div class="stat-icon return-approved">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.returnApproved || 0 }}</span>
          <span class="stat-label">✅ Đã duyệt</span>
        </div>
      </div>

      <div class="stat-card stat-card-return">
        <div class="stat-icon return-received">
          <i class="fas fa-box"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.returnReceived || 0 }}</span>
          <span class="stat-label">📦 Đã nhận hàng</span>
        </div>
      </div>

      <div class="stat-card stat-card-return">
        <div class="stat-icon return-completed">
          <i class="fas fa-check-double"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.returnCompleted || 0 }}</span>
          <span class="stat-label">✅ Hoàn thành</span>
        </div>
      </div>

      <div class="stat-card stat-card-return">
        <div class="stat-icon return-rejected">
          <i class="fas fa-times-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.returnRejected || 0 }}</span>
          <span class="stat-label">❌ Từ chối</span>
        </div>
      </div>

      <!-- ===== THỐNG KÊ ĐÃ HỦY ===== -->
      <div class="stat-card stat-card-cancelled">
        <div class="stat-icon cancelled">
          <i class="fas fa-user-times"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.cancelledByCustomer || 0 }}</span>
          <span class="stat-label">👤 Khách hủy</span>
        </div>
      </div>

      <div class="stat-card stat-card-cancelled">
        <div class="stat-icon cancelled-admin">
          <i class="fas fa-user-shield"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.cancelledByAdmin || 0 }}</span>
          <span class="stat-label">🛡️ Admin hủy</span>
        </div>
      </div>

      <div class="stat-card stat-card-cancelled">
        <div class="stat-icon cancelled-system">
          <i class="fas fa-robot"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.cancelledBySystem || 0 }}</span>
          <span class="stat-label">🤖 Hệ thống hủy</span>
        </div>
      </div>
    </div>

    <!-- ===== MAIN TABS ===== -->
    <div class="main-tabs">
      <button
        v-for="mainTab in mainTabs"
        :key="mainTab.value"
        class="main-tab-btn"
        :class="{ active: activeMainTab === mainTab.value }"
        @click="changeMainTab(mainTab.value)"
      >
        <i :class="mainTab.icon"></i>
        {{ mainTab.label }}
        <span class="tab-badge" :class="mainTab.badgeClass">
          {{ getMainTabCount(mainTab.value) }}
        </span>
      </button>
    </div>

    <!-- ===== SUB TABS ===== -->
    <div class="sub-tabs" v-if="subTabs.length > 0">
      <button
        v-for="subTab in subTabs"
        :key="subTab.value"
        class="sub-tab-btn"
        :class="{ active: activeSubTab === subTab.value }"
        @click="changeSubTab(subTab.value)"
      >
        <i :class="subTab.icon"></i>
        {{ subTab.label }}
        <span class="tab-count">{{ getSubTabCount(subTab.value) }}</span>
      </button>
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          v-model="filters.search"
          placeholder="Tìm kiếm theo mã đơn, tên khách hàng..."
          @input="applyFilters"
        />
      </div>

      <select v-model="filters.status" @change="applyFilters">
        <option value="all">Tất cả trạng thái</option>
        <!-- Đơn hàng -->
        <template v-if="activeMainTab === 'orders'">
          <option value="PENDING_PAYMENT">Chờ thanh toán</option>
          <option value="CONFIRMED">Đã xác nhận</option>
          <option value="PROCESSING">Đang xử lý</option>
          <option value="SHIPPING">Đang giao</option>
          <option value="DELIVERED">Đã giao</option>
          <option value="COMPLETED">Hoàn thành</option>
        </template>
        <!-- Hoàn hàng -->
        <template v-if="activeMainTab === 'returns'">
          <option value="RETURN_REQUESTED">Yêu cầu hoàn hàng</option>
          <option value="RETURN_APPROVED">Đã duyệt hoàn hàng</option>
          <option value="RETURN_RECEIVED">Đã nhận hàng trả</option>
          <option value="RETURN_PENDING">Đang thu hồi hàng</option>
          <option value="RETURNED">Đã hoàn hàng</option>
          <option value="REJECTED">Từ chối hoàn trả</option>
        </template>
        <!-- Hủy đơn -->
        <template v-if="activeMainTab === 'cancelled'">
          <option value="CANCELLED">Đã hủy</option>
        </template>
      </select>

      <button class="btn-search" @click="fetchOrders">
        <i class="fas fa-search"></i> Tìm kiếm
      </button>
      <button class="btn-reset" @click="resetFilters">
        <i class="fas fa-sync-alt"></i> Làm mới
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Đang tải đơn hàng...</p>
    </div>

    <!-- Orders Table -->
    <div v-else class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>Mã đơn</th>
            <th>Khách hàng</th>
            <th>SĐT</th>
            <th>Sản phẩm</th>
            <th>Tổng tiền</th>
            <th>Ngày đặt</th>
            <th>Trạng thái</th>
            <th>Thanh toán</th>
            <th>Xác nhận bởi</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.orderId">
            <td>
              <strong>#{{ order.orderCode || order.orderId }}</strong>
            </td>
            <td>
              <div class="customer-info">
                <div class="customer-name">{{ order.customer?.customerName || 'Khách vãng lai' }}</div>
              </div>
            </td>
            <td>
              <span class="phone-number">{{ order.shippingAddress?.phone || order.customer?.phone || '---' }}</span>
            </td>
            <td>
              <div class="product-count">{{ order.items?.length || 0 }} sản phẩm</div>
              <button class="view-items-btn" @click="viewOrderItems(order)" title="Xem chi tiết">
                <i class="fas fa-eye"></i>
              </button>
            </td>
            <td>
              <span class="order-total">{{ formatPrice(order.totalAmount) }}</span>
            </td>
            <td>{{ formatDate(order.orderDate) }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(order.orderStatus)]">
                {{ getStatusText(order.orderStatus) }}
              </span>
            </td>
            <td>
              <span class="payment-status" :class="getPaymentClass(order.paymentStatus)">
                {{ getPaymentStatusText(order.paymentStatus) }}
              </span>
            </td>
            <td>
              <div v-if="order.processedBy" class="processed-by-cell">
                <span class="processed-name">{{ order.processedBy }}</span>
                <span class="processed-email">{{ order.processedByEmail || '' }}</span>
                <span class="processed-role">{{ order.processedByRole || '' }}</span>
                <span class="processed-time">{{ formatDate(order.processedAt) }}</span>
              </div>
              <span v-else class="not-processed">---</span>
            </td>
            <td>
              <div class="action-buttons">
                <!-- ✅ Nút cập nhật trạng thái - CHỈ HIỂN THỊ nếu là người xác nhận hoặc đơn chưa được xác nhận -->
                <button 
                  v-if="activeMainTab === 'orders' && shouldShowUpdateButton(order)"
                  class="action-btn update-status-btn" 
                  @click="openUpdateStatusModal(order)"
                  :disabled="updatingOrderIds.has(order.orderId) || !canUpdateStatus(order)"
                  :title="getUpdateStatusTooltip(order)"
                >
                  <i class="fas fa-edit"></i>
                </button>
                
                <button class="action-btn view-btn" @click="viewOrderDetail(order)" title="Xem chi tiết">
                  <i class="fas fa-eye"></i>
                </button>
                
                <button 
                  v-if="activeMainTab === 'returns' && isReturnOrder(order.orderStatus)"
                  class="action-btn return-detail-btn" 
                  @click="viewReturnRequest(order)"
                  title="Xem chi tiết hoàn hàng"
                >
                  <i class="fas fa-undo-alt"></i>
                </button>
                
                <button 
                  class="action-btn invoice-btn" 
                  @click="viewInvoice(order)"
                  title="Xem hóa đơn"
                >
                  <i class="fas fa-file-invoice"></i>
                </button>
                
                <button 
                  v-if="activeMainTab === 'orders' && canAdminCancel(order.orderStatus)"
                  class="action-btn cancel-btn" 
                  @click="openCancelOrderModal(order)"
                  title="Hủy đơn hàng"
                >
                  <i class="fas fa-times"></i>
                </button>

                <button 
                  v-if="activeMainTab === 'cancelled' && order.orderStatus === 'CANCELLED'"
                  class="action-btn restore-order-btn" 
                  @click="openRestoreOrderModal(order)"
                  title="Khôi phục đơn hàng"
                >
                  <i class="fas fa-undo"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="orders.length === 0 && !loading">
            <td colspan="10" class="no-data">
              <i class="fas fa-box-open"></i>
              <p>Không tìm thấy đơn hàng nào</p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 1">
      <button class="page-btn" :disabled="currentPage === 0" @click="goToPage(currentPage - 1)">
        <i class="fas fa-chevron-left"></i>
      </button>
      <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button class="page-btn" :disabled="currentPage === totalPages - 1" @click="goToPage(currentPage + 1)">
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- ===== MODALS ===== -->
    <!-- Order Detail Modal -->
    <div v-if="showDetailModal" class="modal-overlay">
      <div class="modal-content modal-lg">
        <div class="modal-header">
          <h2>Chi tiết đơn hàng #{{ selectedOrder?.orderCode || selectedOrder?.orderId }}</h2>
          <button class="close-btn" @click="closeDetailModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="order-info-grid">
            <div class="info-section">
              <h3>Thông tin khách hàng</h3>
              <p><strong>Họ tên:</strong> {{ selectedOrder?.customer?.customerName || 'Khách hàng' }}</p>
              <p><strong>SĐT:</strong> {{ selectedOrder?.shippingAddress?.phone || selectedOrder?.customer?.phone || '---' }}</p>
              <p><strong>Email:</strong> {{ selectedOrder?.customer?.email || '---' }}</p>
            </div>
            <div class="info-section">
              <h3>Thông tin đơn hàng</h3>
              <p><strong>Ngày đặt:</strong> {{ formatDate(selectedOrder?.orderDate) }}</p>
              <p><strong>Trạng thái:</strong> {{ getStatusText(selectedOrder?.orderStatus) }}</p>
              <p><strong>Thanh toán:</strong> {{ getPaymentStatusText(selectedOrder?.paymentStatus) }}</p>
              
              <div v-if="selectedOrder?.processedBy" class="processed-info">
                <p class="processed-label">
                  <i class="fas fa-check-circle" style="color: #28a745;"></i>
                  <strong>Đã xác nhận bởi:</strong>
                </p>
                <div class="processed-detail">
                  <div><strong>Tên:</strong> {{ selectedOrder.processedBy }}</div>
                  <div><strong>Email:</strong> {{ selectedOrder.processedByEmail || 'N/A' }}</div>
                  <div><strong>Vai trò:</strong> {{ selectedOrder.processedByRole || 'N/A' }}</div>
                  <div><strong>Thời gian:</strong> {{ formatDate(selectedOrder.processedAt) }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Địa chỉ giao hàng -->
          <div class="address-section">
            <h3>📍 Địa chỉ giao hàng</h3>
            <div class="address-detail-card">
              <div class="address-row">
                <span class="address-label">👤 Người nhận:</span>
                <span class="address-value">{{ selectedOrder?.shippingAddress?.fullName || '---' }}</span>
              </div>
              <div class="address-row">
                <span class="address-label">📞 Số điện thoại:</span>
                <span class="address-value">{{ selectedOrder?.shippingAddress?.phone || '---' }}</span>
              </div>
              <div class="address-row full-address-row">
                <span class="address-label">📦 Địa chỉ đầy đủ:</span>
                <span class="address-value highlight">{{ selectedOrder?.shippingAddress?.fullAddress || getFullAddress(selectedOrder?.shippingAddress) }}</span>
              </div>
            </div>
          </div>

          <div class="order-items-section">
            <h3>Sản phẩm</h3>
            <table class="items-table">
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th class="price-cell">Đơn giá</th>
                  <th class="quantity-cell">Số lượng</th>
                  <th class="total-cell">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in selectedOrder?.items" :key="item.itemId || item.productDetailId">
                  <td>
                    <div class="item-info">
                      <div class="item-image">
                        <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" @error="handleImageError" />
                        <i v-else class="fas fa-tshirt"></i>
                      </div>
                      <div>
                        <div class="item-name">{{ item.productName }}</div>
                        <div class="item-variant">{{ item.sizeName || '' }} {{ item.colorName || '' }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="price-cell">{{ formatPrice(item.unitPrice) }}</td>
                  <td class="quantity-cell">{{ item.quantity }}</td>
                  <td class="total-cell">{{ formatPrice(item.finalAmount ?? item.refundAmount) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="3" class="text-right">Tạm tính:</td>
                  <td class="text-right">{{ formatPrice(selectedOrder?.subtotalAmount) }}</td>
                </tr>
                <tr v-if="selectedOrder?.discountAmount && selectedOrder?.discountAmount > 0" class="discount-row">
                  <td colspan="3" class="text-right">
                    <span class="voucher-label">
                      <i class="fas fa-ticket-alt"></i> Giảm giá 
                      ({{ selectedOrder?.voucher?.voucherCode || 'Voucher' }})
                    </span>
                  </td>
                  <td class="text-right discount-amount">-{{ formatPrice(selectedOrder?.discountAmount) }}</td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right">Phí vận chuyển:</td>
                  <td class="text-right">{{ formatPrice(selectedOrder?.shippingFee || 0) }}</td>
                </tr>
                <tr class="total-row">
                  <td colspan="3" class="text-right">Tổng cộng:</td>
                  <td class="text-right grand-total">{{ formatPrice(selectedOrder?.totalAmount) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div v-if="selectedOrder?.voucher" class="voucher-info-section">
            <h4><i class="fas fa-ticket-alt"></i> Mã giảm giá đã áp dụng</h4>
            <div class="voucher-detail">
              <span class="voucher-code">{{ selectedOrder.voucher.voucherCode }}</span>
              <span class="voucher-savings">Tiết kiệm: {{ formatPrice(selectedOrder.discountAmount || 0) }}</span>
            </div>
          </div>

          <div v-if="selectedOrder?.notes" class="order-notes">
            <h3>Ghi chú</h3>
            <p>{{ selectedOrder.notes }}</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeDetailModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Order Items Modal -->
    <div v-if="showItemsModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Sản phẩm trong đơn hàng #{{ selectedOrder?.orderCode || selectedOrder?.orderId }}</h2>
          <button class="close-btn" @click="closeItemsModal">✕</button>
        </div>
        <div class="modal-body">
          <table class="items-table">
            <thead>
              <tr>
                <th>Sản phẩm</th>
                <th class="price-cell">Đơn giá</th>
                <th class="quantity-cell">Số lượng</th>
                <th class="total-cell">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in selectedOrder?.items" :key="item.itemId || item.productDetailId">
                <td>
                  <div class="item-info">
                    <div class="item-image">
                      <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" @error="handleImageError" />
                      <i v-else class="fas fa-tshirt"></i>
                    </div>
                    <div>
                      <div class="item-name">{{ item.productName }}</div>
                      <div class="item-variant">{{ item.sizeName || '' }} {{ item.colorName || '' }}</div>
                    </div>
                  </div>
                </td>
                <td class="price-cell">{{ formatPrice(item.unitPrice) }}</td>
                <td class="quantity-cell">{{ item.quantity }}</td>
                <td class="total-cell">{{ formatPrice(item.finalAmount ?? item.refundAmount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeItemsModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Update Status Modal -->
    <UpdateStatusModal
      :visible="showUpdateModal"
      :order="selectedOrderForUpdate"
      @close="closeUpdateStatusModal"
      @updated="onStatusUpdated"
    />

    <!-- Admin Cancel Order Modal -->
    <div v-if="showCancelModal" class="modal-overlay">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Hủy đơn hàng</h3>
          <button class="close-btn" @click="closeCancelModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Lý do hủy <span class="required">*</span></label>
            <textarea v-model="cancelReason" rows="3" placeholder="Nhập lý do hủy đơn hàng..."></textarea>
          </div>
          <div class="warning-text">
            <i class="fas fa-exclamation-triangle"></i>
            <span>Hành động này sẽ hủy đơn hàng và hoàn lại số lượng sản phẩm vào kho (nếu đã trừ).</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeCancelModal">Đóng</button>
          <button class="btn btn-danger" @click="confirmCancelOrder" :disabled="cancelLoading">
            {{ cancelLoading ? 'Đang xử lý...' : 'Xác nhận hủy' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Restore Order Modal -->
    <div v-if="showRestoreModal" class="modal-overlay">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Khôi phục đơn hàng</h3>
          <button class="close-btn" @click="closeRestoreModal">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc muốn khôi phục đơn hàng <strong>#{{ restoreOrder?.orderCode || restoreOrder?.orderId }}</strong>?</p>
          <p class="warning-text success-text">
            <i class="fas fa-info-circle"></i>
            Sau khi khôi phục, đơn hàng sẽ chuyển về trạng thái <strong>PENDING_PAYMENT</strong> và có thể xử lý lại.
          </p>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeRestoreModal">Hủy</button>
          <button class="btn btn-success" @click="confirmRestoreOrder" :disabled="restoreLoading">
            {{ restoreLoading ? 'Đang xử lý...' : 'Khôi phục' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Return Request Detail Modal -->
    <div v-if="showReturnModal" class="modal-overlay">
      <div class="modal-content modal-lg">
        <div class="modal-header">
          <h2>Chi tiết yêu cầu hoàn hàng</h2>
          <button class="close-btn" @click="closeReturnModal">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="returnRequestDetail" class="return-detail">
            <div class="return-info-grid">
              <div class="info-section">
                <h3>Thông tin khách hàng</h3>
                <p><strong>Khách hàng:</strong> {{ returnRequestDetail.customerName || 'N/A' }}</p>
                <p><strong>SĐT:</strong> {{ returnRequestDetail.customerPhone || 'N/A' }}</p>
                <p><strong>Email:</strong> {{ returnRequestDetail.customerEmail || 'N/A' }}</p>
              </div>
              <div class="info-section">
                <h3>Thông tin đơn hàng</h3>
                <p><strong>Mã đơn:</strong> #{{ returnRequestDetail.orderCode || returnRequestDetail.orderId }}</p>
                <p><strong>Trạng thái:</strong> 
                  <span :class="['status-badge', getStatusClass(returnRequestDetail.status)]">
                    {{ getStatusText(returnRequestDetail.status) }}
                  </span>
                </p>
                <p><strong>Ngày yêu cầu:</strong> {{ formatDate(returnRequestDetail.createdAt) }}</p>
              </div>
            </div>

            <div class="return-reason">
              <h3>Lý do hoàn hàng</h3>
              <div class="reason-content">
                <p><strong>Lý do:</strong> {{ returnRequestDetail.reason }}</p>
                <p><strong>Mô tả:</strong> {{ returnRequestDetail.description }}</p>
                <p v-if="returnRequestDetail.adminNote"><strong>Phản hồi Admin:</strong> {{ returnRequestDetail.adminNote }}</p>
              </div>
            </div>

            <div v-if="returnRequestDetail.returnItems?.length" class="return-items-section">
              <h3>Sản phẩm yêu cầu hoàn</h3>
              <div v-for="item in returnRequestDetail.returnItems" :key="item.productDetailId" class="inspection-result-row">
                <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" class="inspection-image" @error="handleImageError" />
                <strong>{{ item.productName }}</strong>
                <span>{{ item.colorName || '---' }} · {{ item.sizeName || '---' }}</span>
                <span>SL hoàn: {{ item.quantity }}</span>
              </div>
            </div>

            <div v-if="returnRequestDetail.images && returnRequestDetail.images.length > 0" class="return-images">
              <h3>Hình ảnh minh họa</h3>
              <div class="image-list">
                <img 
                  v-for="(img, index) in returnRequestDetail.images" 
                  :key="index"
                  :src="getImageUrl(img)"
                  class="return-image"
                  @click="viewImage(img)"
                />
              </div>
            </div>

            <div v-if="activeMainTab === 'returns'" class="admin-actions">
              <h3>Xử lý yêu cầu</h3>
              
              <template v-if="returnRequestDetail.status === 'PENDING'">
                <div class="action-group">
                  <div class="form-group">
                    <label>Phản hồi (tùy chọn)</label>
                    <textarea v-model="adminNote" rows="2" placeholder="Nhập phản hồi cho khách hàng..." class="form-control"></textarea>
                  </div>
                  <div class="button-group">
                    <button class="btn btn-success" @click="processReturn('APPROVED')" :disabled="processingReturn">
                      <i v-if="processingReturn" class="fas fa-spinner fa-spin"></i>
                      {{ processingReturn ? 'Đang xử lý...' : 'Duyệt hoàn hàng' }}
                    </button>
                    <button class="btn btn-danger" @click="processReturn('REJECTED')" :disabled="processingReturn">
                      <i v-if="processingReturn" class="fas fa-spinner fa-spin"></i>
                      {{ processingReturn ? 'Đang xử lý...' : 'Từ chối' }}
                    </button>
                  </div>
                </div>
              </template>

              <template v-if="returnRequestDetail.status === 'APPROVED'">
                <div class="action-group">
                  <div class="form-group">
                    <label>Ghi chú khi nhận hàng (tùy chọn)</label>
                    <textarea v-model="adminNote" rows="2" placeholder="Nhập ghi chú khi nhận hàng trả..." class="form-control"></textarea>
                  </div>
                  <div class="button-group">
                    <button class="btn btn-primary" @click="receiveReturnedItems" :disabled="receivingReturn">
                      <i v-if="receivingReturn" class="fas fa-spinner fa-spin"></i>
                      {{ receivingReturn ? 'Đang xử lý...' : 'Đã nhận hàng trả' }}
                    </button>
                  </div>
                </div>
              </template>

              <template v-if="returnRequestDetail.status === 'RECEIVED'">
                <div class="action-group">
                  <h3>Kiểm tra hàng hoàn trả</h3>
                  <div v-for="item in inspectionItems" :key="item.productDetailId" class="inspection-item">
                    <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" class="inspection-image" @error="handleImageError" />
                    <div class="inspection-info">
                      <strong>{{ item.productName || `Sản phẩm #${item.productDetailId}` }}</strong>
                      <div>Màu: {{ item.colorName || '---' }} · Size: {{ item.sizeName || '---' }}</div>
                      <div>Số lượng hoàn: <strong>{{ item.quantity }}</strong></div>
                    </div>
                    <div class="inspection-actions">
                      <label><input type="radio" :name="`inspection-${item.productDetailId}`" value="RESTOCK" v-model="item.action" /> Nhập lại kho</label>
                      <label><input type="radio" :name="`inspection-${item.productDetailId}`" value="DISCARD" v-model="item.action" /> Loại bỏ</label>
                      <textarea v-if="item.action === 'DISCARD'" v-model="item.discardReason" rows="2" class="form-control" placeholder="Lý do loại bỏ"></textarea>
                    </div>
                  </div>
                  <textarea v-model="inspectionNote" rows="2" placeholder="Ghi chú kiểm tra (tùy chọn)" class="form-control"></textarea>
                  <div class="button-group">
                    <button class="btn btn-success" @click="inspectReturn" :disabled="inspectionLoading">
                      <i v-if="inspectionLoading" class="fas fa-spinner fa-spin"></i>
                      {{ inspectionLoading ? 'Đang kiểm tra...' : 'Xác nhận kết quả kiểm tra' }}
                    </button>
                  </div>
                </div>
              </template>

              <template v-if="returnRequestDetail.status === 'COMPLETED'">
                <div class="status-info-box status-success">
                  <i class="fas fa-check-circle"></i>
                  <span>Đã hoàn tất hoàn hàng!</span>
                </div>
                <div v-if="inspectionResults.length" class="inspection-results">
                  <h3>Kết quả kiểm tra</h3>
                  <div v-for="item in inspectionResults" :key="item.productDetailId" class="inspection-result-row">
                    <strong>{{ item.productName || `Sản phẩm #${item.productDetailId}` }}</strong>
                    <span :class="item.action === 'RESTOCK' ? 'text-success' : 'text-danger'">{{ item.action }}</span>
                    <span>SL: {{ item.quantity }}</span>
                    <span v-if="item.discardReason">Lý do: {{ item.discardReason }}</span>
                  </div>
                </div>
              </template>

              <template v-if="returnRequestDetail.status === 'REJECTED'">
                <div class="status-info-box status-danger">
                  <i class="fas fa-times-circle"></i>
                  <span>Đã từ chối yêu cầu hoàn hàng.</span>
                </div>
              </template>
            </div>

            <div class="return-status">
              <h3>Trạng thái hiện tại</h3>
              <div class="status-info">
                <span :class="['status-badge', getStatusClass(returnRequestDetail.status)]">
                  {{ getStatusText(returnRequestDetail.status) }}
                </span>
                <span v-if="returnRequestDetail.updatedAt" class="update-time">
                  Cập nhật: {{ formatDate(returnRequestDetail.updatedAt) }}
                </span>
              </div>
            </div>
          </div>
          <div v-else class="loading-state">
            <div class="spinner"></div>
            <p>Đang tải thông tin...</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeReturnModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Invoice Modal -->
    <div v-if="showInvoiceModal" class="modal-overlay">
      <div class="modal-content modal-lg">
        <div class="modal-header">
          <h2>Hóa đơn #{{ invoiceData?.orderCode || invoiceData?.orderId }}</h2>
          <div class="header-actions">
            <button class="btn btn-primary btn-sm" @click="printInvoice">
              <i class="fas fa-print"></i> In hóa đơn
            </button>
            <button class="close-btn" @click="closeInvoiceModal">✕</button>
          </div>
        </div>
        <div class="modal-body" id="invoice-content">
          <div v-if="invoiceData" class="invoice-container">
            <div class="invoice-header">
              <div class="company-info">
                <h1>FASHION STORE</h1>
                <p>Địa chỉ: 123 Nguyễn Huệ, Quận 1, TP.HCM</p>
                <p>Điện thoại: (028) 1234 5678</p>
                <p>Email: info@fashionstore.com</p>
              </div>
              <div class="invoice-title">
                <h2>HÓA ĐƠN BÁN HÀNG</h2>
                <p>Mã đơn: #{{ invoiceData.orderCode }}</p>
                <p>Ngày: {{ formatDate(invoiceData.orderDate) }}</p>
                <p>Trạng thái: {{ getStatusText(invoiceData.orderStatus) }}</p>
              </div>
            </div>

            <div class="customer-info">
              <h3>Thông tin khách hàng</h3>
              <div class="info-row">
                <span><strong>Họ tên:</strong> {{ invoiceData.customerName || 'Khách hàng' }}</span>
                <span><strong>SĐT:</strong> {{ invoiceData.customerPhone || '---' }}</span>
                <span><strong>Email:</strong> {{ invoiceData.customerEmail || '---' }}</span>
              </div>
              <div class="info-row">
                <span><strong>Địa chỉ giao hàng:</strong> 
                  {{ getInvoiceFullAddress(invoiceData) }}
                </span>
              </div>
            </div>

            <div class="invoice-items">
              <h3>Chi tiết sản phẩm</h3>
              <table class="invoice-table">
                <thead>
                  <tr>
                    <th style="width: 40%;">Sản phẩm</th>
                    <th style="width: 20%;">Đơn giá</th>
                    <th style="width: 15%;">Số lượng</th>
                    <th style="width: 25%;">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in invoiceData.items" :key="index">
                    <td>
                      <div class="product-info">
                        <div class="product-image">
                          <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" />
                          <i v-else class="fas fa-tshirt"></i>
                        </div>
                        <div>
                          <div class="product-name">{{ item.productName }}</div>
                          <div class="product-variant">{{ item.colorName || '' }} {{ item.sizeName || '' }}</div>
                        </div>
                      </div>
                    </td>
                    <td class="text-right">{{ formatPrice(item.unitPrice) }}</td>
                    <td class="text-center">{{ item.quantity }}</td>
                    <td class="text-right">{{ formatPrice(item.finalAmount) }}</td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <td colspan="3" class="text-right"><strong>Tạm tính:</strong></td>
                    <td class="text-right">{{ formatPrice(invoiceData.subtotalAmount) }}</td>
                  </tr>
                  <tr v-if="invoiceData.discountAmount && invoiceData.discountAmount > 0">
                    <td colspan="3" class="text-right">
                      <strong>Giảm giá</strong>
                      <span v-if="invoiceData.voucherCode" class="voucher-code">({{ invoiceData.voucherCode }})</span>
                    </td>
                    <td class="text-right discount-amount">-{{ formatPrice(invoiceData.discountAmount) }}</td>
                  </tr>
                  <tr>
                    <td colspan="3" class="text-right"><strong>Phí vận chuyển:</strong></td>
                    <td class="text-right">{{ formatPrice(invoiceData.shippingFee || 0) }}</td>
                  </tr>
                  <tr class="total-row">
                    <td colspan="3" class="text-right"><strong>Tổng cộng:</strong></td>
                    <td class="text-right grand-total">{{ formatPrice(invoiceData.totalAmount) }}</td>
                  </tr>
                  <tr v-if="invoiceData.paymentStatus === 'PAID'">
                    <td colspan="4" class="text-right payment-status-paid">
                      <i class="fas fa-check-circle"></i> Đã thanh toán
                    </td>
                  </tr>
                  <tr v-else>
                    <td colspan="4" class="text-right payment-status-unpaid">
                      <i class="fas fa-clock"></i> Chưa thanh toán
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>

            <div v-if="invoiceData.notes" class="invoice-notes">
              <h4>Ghi chú:</h4>
              <p>{{ invoiceData.notes }}</p>
            </div>

            <div class="invoice-footer">
              <p>Cảm ơn quý khách đã mua hàng tại Fashion Store!</p>
            </div>
          </div>
          <div v-else class="loading-state">
            <div class="spinner"></div>
            <p>Đang tải hóa đơn...</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-primary" @click="printInvoice">
            <i class="fas fa-print"></i> In hóa đơn
          </button>
          <button class="btn btn-outline" @click="closeInvoiceModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Image Viewer Modal -->
    <div v-if="showImageViewer" class="modal-overlay">
      <div class="image-viewer-container">
        <img :src="viewingImage" alt="Return image" />
        <button class="close-btn" @click="closeImageViewer">✕</button>
      </div>
    </div>

    <!-- Toast Notification -->
    <div :class="['toast', { show: showToast, 'toast-error': toastType === 'error' }]">
      <i :class="toastType === 'error' ? 'fas fa-exclamation-circle' : 'fas fa-check-circle'"></i>
      {{ toastMessage }}
    </div>
  </div>
</template>

<!-- views/admin/AdminOrders.vue - Phần script đã sửa hoàn chỉnh -->
<!-- views/admin/AdminOrders.vue - Phần script đã sửa hoàn chỉnh -->
<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { adminOrderService } from '../../services/adminOrderService'
import UpdateStatusModal from '../../components/admin/UpdateStatusModal.vue'
import api from "@/api/axios";

// ===== STATE =====
const loading = ref(false)
const orders = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = ref(10)
const statistics = ref(null)
const updatingOrderIds = ref(new Set())

// ===== MAIN TABS =====
const activeMainTab = ref('orders')
const mainTabs = [
  { value: 'orders', label: 'Đơn hàng', icon: 'fas fa-shopping-bag', badgeClass: 'badge-blue' },
  { value: 'returns', label: 'Hoàn hàng', icon: 'fas fa-undo-alt', badgeClass: 'badge-orange' },
  { value: 'cancelled', label: 'Đã hủy', icon: 'fas fa-times-circle', badgeClass: 'badge-red' },
]

// ===== SUB TABS =====
const activeSubTab = ref('all')

const subTabsMap = {
  orders: [
    { value: 'all', label: 'Tất cả', icon: 'fas fa-list' },
    { value: 'PENDING_PAYMENT', label: 'Chờ thanh toán', icon: 'fas fa-clock' },
    { value: 'CONFIRMED', label: 'Đã xác nhận', icon: 'fas fa-check' },
    { value: 'PROCESSING', label: 'Đang xử lý', icon: 'fas fa-spinner' },
    { value: 'SHIPPING', label: 'Đang giao', icon: 'fas fa-truck' },
    { value: 'DELIVERED', label: 'Đã giao', icon: 'fas fa-box' },
    { value: 'COMPLETED', label: 'Hoàn thành', icon: 'fas fa-check-circle' },
  ],
  returns: [
    { value: 'all', label: 'Tất cả', icon: 'fas fa-list' },
    { value: 'RETURN_REQUESTED', label: 'Yêu cầu hoàn hàng', icon: 'fas fa-clock' },
    { value: 'RETURN_APPROVED', label: 'Đã duyệt', icon: 'fas fa-check' },
    { value: 'RETURN_RECEIVED', label: 'Đã nhận hàng trả', icon: 'fas fa-box' },
    { value: 'RETURN_PENDING', label: 'Đang thu hồi', icon: 'fas fa-spinner' },
    { value: 'RETURNED', label: 'Đã hoàn hàng', icon: 'fas fa-check-circle' },
    { value: 'REJECTED', label: 'Từ chối', icon: 'fas fa-times-circle' },
  ],
  cancelled: [
    { value: 'all', label: 'Tất cả', icon: 'fas fa-list' },
    { value: 'CANCELLED', label: 'Đã hủy', icon: 'fas fa-times-circle' },
  ]
}

const subTabs = computed(() => {
  return subTabsMap[activeMainTab.value] || []
})

// ===== FILTERS =====
const filters = ref({
  search: '',
  status: 'all'
})

// ===== MODAL STATE =====
const showDetailModal = ref(false)
const showItemsModal = ref(false)
const showUpdateModal = ref(false)
const showReturnModal = ref(false)
const showInvoiceModal = ref(false)
const showCancelModal = ref(false)
const showRestoreModal = ref(false)
const showImageViewer = ref(false)
const selectedOrder = ref(null)
const selectedOrderForUpdate = ref(null)
const returnRequestDetail = ref(null)
const invoiceData = ref(null)
const cancelOrderId = ref(null)
const cancelReason = ref('')
const cancelLoading = ref(false)
const restoreOrder = ref(null)
const restoreLoading = ref(false)
const adminNote = ref('')
const processingReturn = ref(false)
const receivingReturn = ref(false)
const completingReturn = ref(false)
const inspectionLoading = ref(false)
const inspectionItems = ref([])
const inspectionNote = ref('')
const viewingImage = ref('')

const inspectionResults = computed(() => {
  const value = returnRequestDetail.value?.inspectionResult
  if (!value) return []
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return Object.values(parsed || {})
  } catch {
    return []
  }
})

// ===== TOAST =====
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

// ===== CONSTANTS =====
const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");
const ADMIN_CANNOT_UPDATE = ['CANCELLED', 'RETURNED', 'RETURN_REQUESTED', 'RETURN_PENDING', 'REJECTED', 'COMPLETED']

// ===== HELPER FUNCTIONS =====
const formatPrice = (price) => {
  if (!price && price !== 0) return '0₫'
  const num = Number(price)
  if (isNaN(num)) return '0₫'
  return num.toLocaleString('vi-VN') + '₫'
}

const formatDate = (dateString) => {
  if (!dateString) return '---'
  const date = new Date(dateString)
  if (isNaN(date.getTime())) return '---'
  return date.toLocaleDateString('vi-VN') + ' ' + 
         date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
}

const handleImageError = (event) => {
  event.target.style.display = 'none'
}

const getFullAddress = (shippingAddress) => {
  if (!shippingAddress) return '---'
  const parts = []
  if (shippingAddress.addressLine) parts.push(shippingAddress.addressLine)
  if (shippingAddress.ward) parts.push(shippingAddress.ward)
  if (shippingAddress.district) parts.push(shippingAddress.district)
  if (shippingAddress.city) parts.push(shippingAddress.city)
  if (shippingAddress.fullAddress) {
    return shippingAddress.fullAddress
  }
  return parts.length > 0 ? parts.join(', ') : '---'
}

const getInvoiceFullAddress = (invoice) => {
  if (!invoice) return '---'
  const parts = []
  if (invoice.shippingAddress) parts.push(invoice.shippingAddress)
  if (invoice.shippingWard) parts.push(invoice.shippingWard)
  if (invoice.shippingDistrict) parts.push(invoice.shippingDistrict)
  if (invoice.shippingCity) parts.push(invoice.shippingCity)
  if (invoice.shippingFullAddress) {
    return invoice.shippingFullAddress
  }
  const address = parts.length > 0 ? parts.join(', ') : '---'
  const name = invoice.shippingFullName || ''
  const phone = invoice.shippingPhone ? '- SĐT: ' + invoice.shippingPhone : ''
  return [name, phone, address].filter(Boolean).join(' ')
}

const getStatusText = (status) => {
  const map = {
    'PENDING_PAYMENT': 'Chờ thanh toán',
    'CONFIRMED': 'Đã xác nhận',
    'PROCESSING': 'Đang xử lý',
    'SHIPPING': 'Đang giao',
    'DELIVERED': 'Đã giao',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy',
    'RETURN_REQUESTED': 'Yêu cầu hoàn hàng',
    'RETURN_APPROVED': 'Đã duyệt hoàn hàng',
    'RETURN_RECEIVED': 'Đã nhận hàng trả',
    'RETURN_PENDING': 'Đang thu hồi hàng',
    'RETURNED': 'Đã hoàn hàng',
    'REJECTED': 'Từ chối hoàn trả',
    'PENDING': 'Chờ duyệt',
    'APPROVED': 'Đã duyệt',
    'RECEIVED': 'Đã nhận hàng' 
  }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    'PENDING_PAYMENT': 'status-pending',
    'CONFIRMED': 'status-confirmed',
    'PROCESSING': 'status-processing',
    'SHIPPING': 'status-shipping',
    'DELIVERED': 'status-delivered',
    'COMPLETED': 'status-completed',
    'CANCELLED': 'status-cancelled',
    'RETURN_REQUESTED': 'status-return-requested',
    'RETURN_APPROVED': 'status-return-approved',
    'RETURN_RECEIVED': 'status-return-received',
    'RETURN_PENDING': 'status-return-pending',
    'RETURNED': 'status-returned',
    'REJECTED': 'status-rejected' 
  }
  return map[status] || ''
}

const getPaymentStatusText = (status) => {
  const map = {
    'PAID': 'Đã thanh toán',
    'UNPAID': 'Chưa thanh toán',
    'SUCCESS': 'Đã thanh toán',
    'PENDING': 'Chờ thanh toán',
    'FAILED': 'Thanh toán thất bại',
    'CANCELLED': 'Đã hủy thanh toán',
    'REFUNDED': 'Đã hoàn tiền'
  }
  return map[status] || status
}

const getPaymentClass = (status) => {
  const map = {
    'PAID': 'payment-paid',
    'UNPAID': 'payment-unpaid',
    'SUCCESS': 'payment-paid',
    'PENDING': 'payment-pending',
    'FAILED': 'payment-failed',
    'CANCELLED': 'payment-cancelled',
    'REFUNDED': 'payment-refunded'
  }
  return map[status] || ''
}

// ✅ Lấy thông tin người dùng hiện tại
const getCurrentUser = () => {
  return localStorage.getItem('username') || 
         localStorage.getItem('userName') || 
         ''
}

const getCurrentUserEmail = () => {
  return localStorage.getItem('userEmail') || 
         localStorage.getItem('email') || 
         ''
}

// ✅ Kiểm tra xem đơn hàng đã được xác nhận bởi người khác chưa - SO SÁNH CẢ USERNAME VÀ EMAIL
const isOrderConfirmedByOther = (order) => {
  if (!order || !order.processedBy) {
    return false
  }
  
  const currentUser = getCurrentUser()
  const currentEmail = getCurrentUserEmail()
  const status = order.orderStatus
  
  // Chỉ chặn khi đơn hàng ở trạng thái CONFIRMED hoặc PROCESSING
  if (status !== 'CONFIRMED' && status !== 'PROCESSING') {
    return false
  }
  
  // 🔥 QUAN TRỌNG: So sánh KHÔNG phân biệt chữ hoa/thường
  const processedByLower = (order.processedBy || '').toLowerCase().trim()
  const processedByEmailLower = (order.processedByEmail || '').toLowerCase().trim()
  const currentUserLower = (currentUser || '').toLowerCase().trim()
  const currentEmailLower = (currentEmail || '').toLowerCase().trim()
  
  // Kiểm tra xem có phải cùng người không
  const isSameUser = processedByLower === currentUserLower || 
                     processedByEmailLower === currentEmailLower ||
                     processedByLower === currentEmailLower ||
                     processedByEmailLower === currentUserLower
  
  // Nếu không cùng người -> là người khác
  return !isSameUser
}

// ✅ Kiểm tra xem có nên hiển thị nút cập nhật không
const shouldShowUpdateButton = (order) => {
  if (!order) return false
  
  const status = order.orderStatus
  
  // Không hiển thị nếu là trạng thái cuối
  if (ADMIN_CANNOT_UPDATE.includes(status)) {
    return false
  }
  
  // Nếu đơn hàng chưa được xác nhận -> hiển thị nút
  if (!order.processedBy) {
    return true
  }
  
  // Nếu đơn hàng đã được xác nhận -> kiểm tra
  const currentUser = getCurrentUser()
  const currentEmail = getCurrentUserEmail()
  
  const processedByLower = (order.processedBy || '').toLowerCase().trim()
  const processedByEmailLower = (order.processedByEmail || '').toLowerCase().trim()
  const currentUserLower = (currentUser || '').toLowerCase().trim()
  const currentEmailLower = (currentEmail || '').toLowerCase().trim()
  
  // CHỈ hiển thị nếu là người xác nhận
  const isSameUser = processedByLower === currentUserLower || 
                     processedByEmailLower === currentEmailLower ||
                     processedByLower === currentEmailLower ||
                     processedByEmailLower === currentUserLower
  
  return isSameUser
}

// ✅ Kiểm tra xem có thể cập nhật trạng thái không
const canUpdateStatus = (order) => {
  if (!order) return false
  
  const status = order.orderStatus
  
  if (ADMIN_CANNOT_UPDATE.includes(status)) {
    return false
  }
  
  if (order.processedBy) {
    const currentUser = getCurrentUser()
    const currentEmail = getCurrentUserEmail()
    
    const processedByLower = (order.processedBy || '').toLowerCase().trim()
    const processedByEmailLower = (order.processedByEmail || '').toLowerCase().trim()
    const currentUserLower = (currentUser || '').toLowerCase().trim()
    const currentEmailLower = (currentEmail || '').toLowerCase().trim()
    
    const isSameUser = processedByLower === currentUserLower || 
                       processedByEmailLower === currentEmailLower ||
                       processedByLower === currentEmailLower ||
                       processedByEmailLower === currentUserLower
    
    return isSameUser
  }
  
  return true
}

// ✅ Tooltip cho nút cập nhật trạng thái
const getUpdateStatusTooltip = (order) => {
  if (!order) return ''
  
  const status = order.orderStatus
  
  if (ADMIN_CANNOT_UPDATE.includes(status)) {
    return `Không thể cập nhật trạng thái "${getStatusText(status)}"`
  }
  
  if (order.processedBy) {
    const currentUser = getCurrentUser()
    const currentEmail = getCurrentUserEmail()
    
    const processedByLower = (order.processedBy || '').toLowerCase().trim()
    const processedByEmailLower = (order.processedByEmail || '').toLowerCase().trim()
    const currentUserLower = (currentUser || '').toLowerCase().trim()
    const currentEmailLower = (currentEmail || '').toLowerCase().trim()
    
    const isSameUser = processedByLower === currentUserLower || 
                       processedByEmailLower === currentEmailLower ||
                       processedByLower === currentEmailLower ||
                       processedByEmailLower === currentUserLower
    
    if ((status === 'CONFIRMED' || status === 'PROCESSING') && !isSameUser) {
      return `⚠️ Đơn hàng đã được xác nhận bởi ${order.processedBy || order.processedByEmail}\nChỉ người xác nhận mới được cập nhật tiếp!`
    }
  }
  
  return 'Cập nhật trạng thái đơn hàng'
}

const showNotification = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 3000)
}

const canAdminCancel = (status) => {
  return status !== 'COMPLETED' && status !== 'CANCELLED' && status !== 'RETURNED'
}

const isReturnOrder = (status) => {
  return ['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'REJECTED'].includes(status)
}

const applyReturnItemsToOrder = (order, returnRequest) => {
  const items = (returnRequest.returnItems || []).map((item) => {
    const refundAmount = Number(item.refundAmount ?? (Number(item.unitPrice || 0) * Number(item.quantity || 0)))
    return {
      ...item,
      itemId: item.productDetailId,
      finalAmount: refundAmount
    }
  })
  const returnAmount = items.reduce((total, item) => total + Number(item.finalAmount || 0), 0)

  return {
    ...order,
    items,
    subtotalAmount: returnAmount,
    discountAmount: 0,
    shippingFee: 0,
    totalAmount: returnAmount,
    voucher: null
  }
}

const loadOrderForDisplay = async (order) => {
  if (!isReturnOrder(order?.orderStatus)) {
    return order
  }

  const response = await adminOrderService.getReturnRequestByOrderId(order.orderId)
  return applyReturnItemsToOrder(order, response.data)
}

const enrichReturnOrders = async (orderList) => {
  if (activeMainTab.value !== 'returns') return orderList

  return Promise.all(orderList.map(async (order) => {
    try {
      return await loadOrderForDisplay(order)
    } catch (error) {
      console.error(`Không thể tải sản phẩm hoàn trả của đơn ${order.orderId}:`, error)
      return order
    }
  }))
}

const getMainTabCount = (tab) => {
  if (tab === 'orders') {
    return orders.value.filter(o => 
      !['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'REJECTED', 'CANCELLED'].includes(o.orderStatus)
    ).length
  }
  if (tab === 'returns') {
    return orders.value.filter(o => 
      ['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'REJECTED'].includes(o.orderStatus)
    ).length
  }
  if (tab === 'cancelled') {
    return orders.value.filter(o => o.orderStatus === 'CANCELLED').length
  }
  return 0
}

const getSubTabCount = (status) => {
  if (status === 'all') {
    return getMainTabCount(activeMainTab.value)
  }
  return orders.value.filter(o => o.orderStatus === status).length
}

// ===== FETCH ORDERS =====
const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      _t: Date.now()
    }
    
    let statusFilters = []
    
    if (activeMainTab.value === 'orders') {
      const normalStatuses = ['PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'DELIVERED', 'COMPLETED']
      if (activeSubTab.value === 'all') {
        statusFilters = normalStatuses
      } else {
        statusFilters = [activeSubTab.value]
      }
    } else if (activeMainTab.value === 'returns') {
      const returnStatuses = ['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'REJECTED']
      if (activeSubTab.value === 'all') {
        statusFilters = returnStatuses
      } else {
        statusFilters = [activeSubTab.value]
      }
    } else if (activeMainTab.value === 'cancelled') {
      if (activeSubTab.value === 'all' || activeSubTab.value === 'CANCELLED') {
        statusFilters = ['CANCELLED']
      } else {
        statusFilters = [activeSubTab.value]
      }
    }
    
    if (filters.value.status !== 'all') {
      if (statusFilters.length === 0) {
        statusFilters = [filters.value.status]
      } else if (!statusFilters.includes(filters.value.status)) {
        statusFilters = [filters.value.status]
      } else {
        statusFilters = [filters.value.status]
      }
    }
    
    if (statusFilters.length > 0) {
      params.status = statusFilters.join(',')
    }
    
    if (filters.value.search) {
      params.keyword = filters.value.search
    }
    
    console.log('📤 Fetching orders with params:', params)
    
    const response = await adminOrderService.getAllOrders(params)
    
    console.log('📦 Orders response:', response.data)
    
    if (response.data && response.data.content) {
      orders.value = await enrichReturnOrders(response.data.content)
      totalPages.value = response.data.totalPages || 0
      totalElements.value = response.data.totalElements || 0
      currentPage.value = response.data.number || 0
    } else if (Array.isArray(response.data)) {
      orders.value = await enrichReturnOrders(response.data)
      totalPages.value = 1
    } else {
      orders.value = []
    }
  } catch (error) {
    console.error('❌ Error fetching orders:', error)
    if (error.code !== 'ERR_NETWORK') {
      showNotification(error.response?.data?.message || 'Không thể tải danh sách đơn hàng', 'error')
    }
    orders.value = []
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const response = await adminOrderService.getStatistics()
    if (response.data) {
      statistics.value = response.data
    }
  } catch (error) {
    console.error('❌ Error fetching statistics:', error)
  }
}

// ===== TAB CHANGE =====
const changeMainTab = (tab) => {
  activeMainTab.value = tab
  activeSubTab.value = 'all'
  filters.value.status = 'all'
  currentPage.value = 0
  fetchOrders()
}

const changeSubTab = (tab) => {
  activeSubTab.value = tab
  filters.value.status = 'all'
  currentPage.value = 0
  fetchOrders()
}

// ===== FILTERS =====
const applyFilters = () => {
  currentPage.value = 0
  fetchOrders()
}

const resetFilters = () => {
  filters.value = {
    search: '',
    status: 'all'
  }
  activeSubTab.value = 'all'
  currentPage.value = 0
  fetchOrders()
}

// ===== PAGINATION =====
const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    fetchOrders()
  }
}

// ===== UPDATE STATUS =====
const openUpdateStatusModal = (order) => {
  if (!canUpdateStatus(order)) {
    const tooltip = getUpdateStatusTooltip(order)
    showNotification(tooltip, 'warning')
    return
  }
  selectedOrderForUpdate.value = order
  showUpdateModal.value = true
}

const closeUpdateStatusModal = () => {
  showUpdateModal.value = false
  selectedOrderForUpdate.value = null
}

// ✅ SỬA: Hàm xử lý sau khi cập nhật trạng thái - THÊM THÔNG BÁO CHO DELIVERED
const onStatusUpdated = async (data) => {
  // Kiểm tra nếu đơn hàng được chuyển sang DELIVERED
  if (data.newStatus === 'DELIVERED') {
    showNotification(
      `✅ Đơn hàng #${data.orderId} đã được giao thành công. Thanh toán đã được tự động cập nhật thành THÀNH CÔNG!`,
      'success'
    )
  } else {
    showNotification(
      `✅ Đã cập nhật trạng thái đơn hàng #${data.orderId} thành "${getStatusText(data.newStatus)}"`,
      'success'
    )
  }
  
  // Refresh lại dữ liệu để cập nhật UI
  await fetchOrders()
  await fetchStatistics()
}

// ===== VIEW DETAILS =====
const viewOrderDetail = async (order) => {
  try {
    const response = await adminOrderService.getOrderDetail(order.orderId)
    selectedOrder.value = await loadOrderForDisplay(response.data)
    showDetailModal.value = true
  } catch (error) {
    console.error('❌ Error fetching order detail:', error)
    showNotification('Không thể tải chi tiết đơn hàng', 'error')
  }
}

const viewOrderItems = async (order) => {
  try {
    selectedOrder.value = await loadOrderForDisplay(order)
    showItemsModal.value = true
  } catch (error) {
    console.error('Không thể tải sản phẩm hoàn trả:', error)
    showNotification('Không thể tải sản phẩm hoàn trả', 'error')
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
}

const closeItemsModal = () => {
  showItemsModal.value = false
  selectedOrder.value = null
}

// ===== INVOICE =====
const viewInvoice = async (order) => {
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(
      `/admin/orders/${order.orderId}/invoice`,
      {}
    )
    
    if (response.data) {
      invoiceData.value = response.data
      showInvoiceModal.value = true
    }
  } catch (error) {
    console.error('❌ Error fetching invoice:', error)
    showNotification(error.response?.data?.message || 'Không thể tải hóa đơn', 'error')
  }
}

const closeInvoiceModal = () => {
  showInvoiceModal.value = false
  invoiceData.value = null
}

const printInvoice = () => {
  const content = document.getElementById('invoice-content')
  if (!content) {
    showNotification('Không tìm thấy nội dung hóa đơn', 'error')
    return
  }
  
  const printWindow = window.open('', '_blank', 'width=1000,height=800')
  if (!printWindow) {
    showNotification('Vui lòng cho phép popup để in hóa đơn', 'warning')
    return
  }
  
  printWindow.document.write(`<!DOCTYPE html><html><head><title>Hóa đơn #${invoiceData.value?.orderCode}</title><style>
    * { box-sizing: border-box; }
    body { font-family: 'Times New Roman', Arial, sans-serif; padding: 40px; max-width: 800px; margin: 0 auto; background: white; color: #333; }
    .invoice-header { display: flex; justify-content: space-between; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 3px solid #e63946; }
    .company-info h1 { color: #e63946; margin: 0; font-size: 28px; }
    .company-info p { margin: 5px 0; color: #666; font-size: 13px; }
    .invoice-title { text-align: right; }
    .invoice-title h2 { color: #333; margin: 0; font-size: 22px; }
    .invoice-title p { margin: 5px 0; color: #666; font-size: 14px; }
    .customer-info { background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
    .customer-info h3 { margin-top: 0; color: #333; font-size: 16px; margin-bottom: 10px; }
    .info-row { margin: 5px 0; font-size: 14px; }
    .invoice-items h3 { font-size: 16px; color: #333; margin-bottom: 15px; }
    .invoice-table { width: 100%; border-collapse: collapse; margin: 15px 0; }
    .invoice-table th { background: #f8f9fa; padding: 10px; text-align: left; border-bottom: 2px solid #dee2e6; font-weight: 600; font-size: 14px; }
    .invoice-table td { padding: 10px; border-bottom: 1px solid #dee2e6; vertical-align: middle; font-size: 14px; }
    .text-right { text-align: right; }
    .text-center { text-align: center; }
    .total-row { font-weight: bold; background: #fafafa; }
    .grand-total { font-size: 18px; color: #e63946; }
    .discount-amount { color: #28a745; font-weight: bold; }
    .payment-status-paid { color: #28a745; font-weight: bold; padding: 8px; background: #d4edda; border-radius: 4px; display: inline-block; margin-top: 10px; }
    .payment-status-unpaid { color: #dc3545; font-weight: bold; padding: 8px; background: #f8d7da; border-radius: 4px; display: inline-block; margin-top: 10px; }
    .product-info { display: flex; align-items: center; gap: 10px; }
    .product-image { width: 40px; height: 40px; background: #f5f5f5; border-radius: 5px; display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0; }
    .product-image img { width: 100%; height: 100%; object-fit: cover; }
    .product-image i { font-size: 20px; color: #ccc; }
    .product-name { font-weight: 600; }
    .product-variant { font-size: 12px; color: #999; }
    .voucher-code { color: #28a745; font-weight: 600; background: #e8f5e9; padding: 2px 8px; border-radius: 4px; }
    .invoice-footer { margin-top: 30px; text-align: center; color: #666; border-top: 1px solid #eee; padding-top: 20px; font-style: italic; }
    .invoice-notes { background: #f8f9fa; padding: 15px; border-radius: 8px; margin: 15px 0; }
    .invoice-notes h4 { margin: 0 0 5px 0; color: #333; font-size: 14px; }
    .invoice-notes p { margin: 0; color: #666; }
    @media print { body { padding: 20px; } .no-print { display: none; } .btn { display: none; } }
  </style></head><body>${content.innerHTML}<div style="margin-top:20px;text-align:center;color:#999;font-size:11px;border-top:1px solid #eee;padding-top:10px;">Hóa đơn được tạo tự động từ hệ thống Fashion Store</div></body></html>`)
  
  printWindow.document.close()
  setTimeout(() => {
    printWindow.focus()
    printWindow.print()
  }, 500)
}

// ===== CANCEL ORDER =====
const openCancelOrderModal = (order) => {
  cancelOrderId.value = order.orderId
  cancelReason.value = ''
  showCancelModal.value = true
}

const closeCancelModal = () => {
  showCancelModal.value = false
  cancelOrderId.value = null
  cancelReason.value = ''
  cancelLoading.value = false
}

const confirmCancelOrder = async () => {
  if (!cancelReason.value.trim()) {
    showNotification('Vui lòng nhập lý do hủy đơn hàng', 'error')
    return
  }

  cancelLoading.value = true
  try {
    const token = localStorage.getItem('token')
    const payload = {
      reason: cancelReason.value.trim()
    }

    const response = await api.put(
      `/admin/orders/${cancelOrderId.value}/cancel`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )

    console.log('✅ Order cancelled:', response.data)
    showNotification('✅ Hủy đơn hàng thành công!', 'success')
    closeCancelModal()
    await fetchOrders()
    await fetchStatistics()
  } catch (error) {
    console.error('❌ Error canceling order:', error)
    showNotification(error.response?.data?.message || 'Không thể hủy đơn hàng', 'error')
  } finally {
    cancelLoading.value = false
  }
}

// ===== RESTORE ORDER =====
const openRestoreOrderModal = (order) => {
  restoreOrder.value = order
  showRestoreModal.value = true
}

const closeRestoreModal = () => {
  showRestoreModal.value = false
  restoreOrder.value = null
  restoreLoading.value = false
}

const confirmRestoreOrder = async () => {
  if (!restoreOrder.value) return

  restoreLoading.value = true
  try {
    const token = localStorage.getItem('token')
    
    const payload = {
      orderStatus: 'PENDING_PAYMENT',
      notes: 'Admin khôi phục đơn hàng từ trạng thái đã hủy'
    }

    const response = await api.put(
      `/admin/orders/${restoreOrder.value.orderId}/status`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )

    console.log('✅ Order restored:', response.data)
    showNotification('✅ Khôi phục đơn hàng thành công!', 'success')
    closeRestoreModal()
    await fetchOrders()
    await fetchStatistics()
  } catch (error) {
    console.error('❌ Error restoring order:', error)
    showNotification(error.response?.data?.message || 'Không thể khôi phục đơn hàng', 'error')
  } finally {
    restoreLoading.value = false
  }
}

// ===== RETURN REQUEST =====
const viewReturnRequest = async (order) => {
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(`/admin/orders/return-requests/order/${order.orderId}`, {})
    
    if (response.data) {
      returnRequestDetail.value = response.data
      const quantities = response.data.quantities || {}
      const ids = response.data.productDetailIds || []
      const orderItems = response.data.returnItems || order.items || []
      inspectionItems.value = ids.map((productDetailId) => {
        const source = orderItems.find(item => Number(item.productDetailId) === Number(productDetailId)) || {}
        return {
          productDetailId,
          productName: source.productName,
          colorName: source.colorName,
          sizeName: source.sizeName,
          imageUrl: source.imageUrl,
          quantity: Number(quantities[productDetailId] ?? quantities[String(productDetailId)] ?? 0),
          action: 'RESTOCK',
          discardReason: ''
        }
      })
      inspectionNote.value = response.data.inspectionNote || ''
      adminNote.value = ''
      showReturnModal.value = true
    } else {
      showNotification('Không tìm thấy yêu cầu hoàn hàng', 'warning')
    }
  } catch (error) {
    console.error('❌ Error fetching return request:', error)
    showNotification(error.response?.data?.message || 'Không thể tải yêu cầu hoàn hàng', 'error')
  }
}

const closeReturnModal = () => {
  showReturnModal.value = false
  returnRequestDetail.value = null
  adminNote.value = ''
  processingReturn.value = false
  receivingReturn.value = false
  completingReturn.value = false
  inspectionLoading.value = false
  inspectionItems.value = []
  inspectionNote.value = ''
}

const inspectReturn = async () => {
  if (!returnRequestDetail.value || !inspectionItems.value.length) return
  const invalid = inspectionItems.value.find(item => item.action === 'DISCARD' && !item.discardReason?.trim())
  if (invalid) {
    showNotification(`Vui lòng nhập lý do loại bỏ cho sản phẩm ${invalid.productName || invalid.productDetailId}`, 'warning')
    return
  }
  if (!confirm('Xác nhận kết quả kiểm tra hàng hoàn trả?')) return
  inspectionLoading.value = true
  try {
    await adminOrderService.inspectReturn(returnRequestDetail.value.returnRequestId, {
      inspectionNote: inspectionNote.value.trim() || null,
      items: inspectionItems.value.map(({ productDetailId, action, discardReason }) => ({
        productDetailId,
        action,
        discardReason: action === 'DISCARD' ? discardReason.trim() : null
      }))
    })
    showNotification('✅ Đã kiểm tra hàng hoàn trả và cập nhật tồn kho!', 'success')
    await fetchOrders()
    await fetchStatistics()
    closeReturnModal()
  } catch (error) {
    showNotification(error.response?.data?.message || 'Không thể kiểm tra hàng hoàn trả', 'error')
  } finally {
    inspectionLoading.value = false
  }
}

const processReturn = async (status) => {
  if (!returnRequestDetail.value) return
  
  const actionText = status === 'APPROVED' ? 'duyệt' : 'từ chối'
  if (!confirm(`Bạn có chắc muốn ${actionText} yêu cầu hoàn hàng này?`)) return
  
  processingReturn.value = true
  
  try {
    const token = localStorage.getItem('token')
    const payload = {
      status: status,
      adminNote: adminNote.value.trim() || null
    }
    
    const response = await api.put(
      `/admin/orders/return-requests/${returnRequestDetail.value.orderId}/process`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )
    
    console.log('✅ Return request processed:', response.data)
    returnRequestDetail.value = response.data
    
    if (status === 'APPROVED') {
      showNotification('✅ Đã duyệt yêu cầu hoàn hàng thành công!', 'success')
    } else {
      showNotification('❌ Đã từ chối yêu cầu hoàn hàng.', 'info')
    }
    
    await fetchOrders()
    await fetchStatistics()
  } catch (error) {
    console.error('❌ Error processing return request:', error)
    showNotification(error.response?.data?.message || `Không thể ${actionText} yêu cầu hoàn hàng`, 'error')
  } finally {
    processingReturn.value = false
  }
}

const receiveReturnedItems = async () => {
  if (!returnRequestDetail.value) return
  
  if (!confirm('Bạn đã kiểm tra và nhận đủ hàng trả từ khách hàng?')) return
  
  receivingReturn.value = true
  
  try {
    const token = localStorage.getItem('token')
    const payload = {
      note: adminNote.value.trim() || null
    }
    
    const response = await api.put(
      `/admin/orders/return-requests/${returnRequestDetail.value.orderId}/receive`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )
    
    console.log('✅ Returned items received:', response.data)
    showNotification('✅ Đã xác nhận nhận hàng trả thành công!', 'success')
    
    await fetchOrders()
    await fetchStatistics()
    closeReturnModal()
  } catch (error) {
    console.error('❌ Error receiving returned items:', error)
    showNotification(error.response?.data?.message || 'Không thể xác nhận nhận hàng trả', 'error')
  } finally {
    receivingReturn.value = false
  }
}

const completeReturn = async () => {
  if (!returnRequestDetail.value) return
  
  if (!confirm('Bạn có chắc muốn hoàn tất hoàn hàng cho đơn hàng này?')) return
  
  completingReturn.value = true
  
  try {
    const token = localStorage.getItem('token')
    
    const payload = {
      orderStatus: 'RETURNED',
      notes: adminNote.value.trim() || 'Hoàn tất hoàn hàng'
    }
    
    const response = await api.put(
      `/admin/orders/${returnRequestDetail.value.orderId}/status`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )
    
    console.log('✅ Return completed:', response.data)
    showNotification('✅ Đã hoàn tất hoàn hàng thành công!', 'success')
    
    await fetchOrders()
    await fetchStatistics()
    closeReturnModal()
  } catch (error) {
    console.error('❌ Error completing return:', error)
    showNotification(error.response?.data?.message || 'Không thể hoàn tất hoàn hàng', 'error')
  } finally {
    completingReturn.value = false
  }
}

// ===== IMAGE VIEWER =====
const viewImage = (imageUrl) => {
  viewingImage.value = getImageUrl(imageUrl)
  showImageViewer.value = true
}

const closeImageViewer = () => {
  showImageViewer.value = false
  viewingImage.value = ''
}

// ===== LIFECYCLE =====
onMounted(() => {
  console.log('🚀 AdminOrders mounted')
  console.log('🔍 Current user:', getCurrentUser())
  console.log('🔍 Current user email:', getCurrentUserEmail())
  fetchOrders()
  fetchStatistics()
})

watch([activeMainTab, activeSubTab], () => {
  fetchOrders()
})
</script>

<style scoped>
/* ===== RESET & CONTAINER ===== */
.admin-orders {
  padding: 20px;
  animation: fadeIn 0.3s;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.page-title {
  font-size: 28px;
  color: #2c3e50;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.btn-refresh {
  padding: 10px 20px;
  background: #2c3e50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-refresh:hover {
  background: #1a252f;
}

/* ===== STATS ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 15px;
  margin-bottom: 25px;
}

.stat-card {
  background: white;
  padding: 18px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 15px;
  transition: all 0.3s;
  border-left: 4px solid transparent;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
}

.stat-card-highlight {
  border-left-color: #6c5ce7;
  background: linear-gradient(135deg, #ffffff 0%, #f8f0ff 100%);
}

.stat-card-return {
  border-left-color: #f39c12;
  background: linear-gradient(135deg, #ffffff 0%, #fff8e1 100%);
}

.stat-card-cancelled {
  border-left-color: #e74c3c;
  background: linear-gradient(135deg, #ffffff 0%, #fde8e8 100%);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  flex-shrink: 0;
}

.stat-icon.blue { background: #3498db; }
.stat-icon.yellow { background: #f39c12; }
.stat-icon.green { background: #2ecc71; }
.stat-icon.purple { background: #6c5ce7; }
.stat-icon.teal { background: #00b894; }
.stat-icon.orange { background: #e17055; }
.stat-icon.success { background: #00b894; }
.stat-icon.return { background: #fdcb6e; color: #2d3436; }
.stat-icon.return-approved { background: #00b894; }
.stat-icon.return-received { background: #0984e3; }
.stat-icon.return-completed { background: #00b894; }
.stat-icon.return-rejected { background: #e74c3c; }
.stat-icon.cancelled { background: #fd79a8; }
.stat-icon.cancelled-admin { background: #6c5ce7; }
.stat-icon.cancelled-system { background: #636e72; }

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-number {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.2;
}

.stat-number.stat-revenue {
  color: #6c5ce7;
}

.stat-number.stat-return {
  color: #e17055;
}

.stat-number.stat-net {
  color: #00b894;
}

.stat-label {
  font-size: 12px;
  color: #999;
  display: block;
  margin-top: 2px;
}

/* ===== MAIN TABS ===== */
.main-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  background: white;
  padding: 10px 15px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.main-tab-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  font-size: 15px;
  color: #666;
  background: transparent;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.main-tab-btn:hover {
  background: #f0f0f0;
}

.main-tab-btn.active {
  background: #e63946;
  color: white;
  box-shadow: 0 4px 12px rgba(230, 57, 70, 0.3);
}

.main-tab-btn .tab-badge {
  padding: 1px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-left: 4px;
}

.main-tab-btn .tab-badge.badge-blue {
  background: rgba(52, 152, 219, 0.2);
  color: #2980b9;
}

.main-tab-btn .tab-badge.badge-orange {
  background: rgba(230, 126, 34, 0.2);
  color: #d35400;
}

.main-tab-btn .tab-badge.badge-red {
  background: rgba(231, 76, 60, 0.2);
  color: #c0392b;
}

.main-tab-btn.active .tab-badge {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

/* ===== SUB TABS ===== */
.sub-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 15px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 10px;
  flex-wrap: wrap;
  border: 1px solid #e9ecef;
}

.sub-tab-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  font-size: 13px;
  color: #666;
  background: transparent;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sub-tab-btn:hover {
  background: #e9ecef;
}

.sub-tab-btn.active {
  background: #2c3e50;
  color: white;
}

.sub-tab-btn .tab-count {
  font-size: 11px;
  background: rgba(0, 0, 0, 0.08);
  padding: 0 8px;
  border-radius: 12px;
  font-weight: 600;
}

.sub-tab-btn.active .tab-count {
  background: rgba(255, 255, 255, 0.2);
}

/* ===== FILTERS ===== */
.filters-bar {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  align-items: center;
}

.search-box {
  flex: 1;
  min-width: 250px;
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 0 15px;
}

.search-box i {
  color: #999;
}

.search-box input {
  border: none;
  background: none;
  padding: 12px;
  width: 100%;
  outline: none;
}

.filters-bar select {
  padding: 12px 15px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  outline: none;
  min-width: 180px;
  cursor: pointer;
  background: white;
}

.btn-search, .btn-reset {
  padding: 12px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-search {
  background: #e63946;
  color: white;
}

.btn-search:hover {
  background: #c82333;
}

.btn-reset {
  background: #f0f0f0;
  color: #666;
}

.btn-reset:hover {
  background: #e0e0e0;
}

/* ===== TABLE ===== */
.table-container {
  background: white;
  border-radius: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
  margin-bottom: 20px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1100px;
}

.data-table th {
  background: #f8f9fa;
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
  font-size: 13px;
}

.data-table td {
  padding: 12px 15px;
  border-bottom: 1px solid #eee;
  color: #666;
  font-size: 14px;
  vertical-align: middle;
}

.data-table tr:hover {
  background: #fafafa;
}

.customer-name {
  font-weight: 600;
  color: #2c3e50;
}

.phone-number {
  font-weight: 500;
  color: #2c3e50;
}

.product-count {
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 5px;
}

.view-items-btn {
  background: none;
  border: none;
  color: #e63946;
  cursor: pointer;
  font-size: 14px;
  padding: 5px;
  transition: all 0.3s;
}

.view-items-btn:hover {
  transform: scale(1.1);
}

.order-total {
  font-weight: 700;
  color: #e63946;
}

/* ===== PROCESSED BY COLUMN ===== */
.processed-by-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
}

.processed-by-cell .processed-name {
  font-weight: 600;
  color: #2c3e50;
}

.processed-by-cell .processed-email {
  color: #6c5ce7;
  font-size: 11px;
}

.processed-by-cell .processed-role {
  color: #999;
  font-size: 10px;
  background: #f0f0f0;
  padding: 0 8px;
  border-radius: 10px;
  display: inline-block;
  width: fit-content;
}

.processed-by-cell .processed-time {
  color: #aaa;
  font-size: 10px;
}

.not-processed {
  color: #ccc;
  font-size: 14px;
}

/* ===== STATUS BADGE ===== */
.status-badge {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.status-pending {
  background: #fff3cd;
  color: #856404;
}
.status-badge.status-confirmed {
  background: #cce5ff;
  color: #004085;
}
.status-badge.status-processing {
  background: #cce5ff;
  color: #004085;
}
.status-badge.status-shipping {
  background: #d1ecf1;
  color: #0c5460;
}
.status-badge.status-delivered {
  background: #d4edda;
  color: #155724;
}
.status-badge.status-completed {
  background: #d4edda;
  color: #155724;
}
.status-badge.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}
.status-badge.status-return-requested {
  background: #fff3cd;
  color: #856404;
}
.status-badge.status-return-approved {
  background: #cce5ff;
  color: #004085;
}
.status-badge.status-return-received {
  background: #cce5ff;
  color: #004085;
}
.status-badge.status-return-pending {
  background: #d1ecf1;
  color: #0c5460;
}
.status-badge.status-returned {
  background: #d4edda;
  color: #155724;
}
.status-badge.status-rejected {
  background: #f8d7da;
  color: #721c24;
}

.payment-status {
  display: inline-block;
  padding: 5px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.payment-status.payment-paid {
  background: #d4edda;
  color: #155724;
}

.payment-status.payment-unpaid {
  background: #fff3cd;
  color: #856404;
}

/* ===== ACTION BUTTONS ===== */
.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.action-btn {
  width: 35px;
  height: 35px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.update-status-btn {
  background: #e63946;
  color: white;
}

.update-status-btn:hover:not(:disabled) {
  background: #c82333;
  transform: translateY(-2px);
}

.update-status-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.view-btn {
  background: #17a2b8;
  color: white;
}

.view-btn:hover {
  background: #138496;
  transform: translateY(-2px);
}

.return-detail-btn {
  background: #ffc107;
  color: #333;
}

.return-detail-btn:hover {
  background: #e0a800;
  transform: translateY(-2px);
}

.invoice-btn {
  background: #6c5ce7;
  color: white;
}

.invoice-btn:hover {
  background: #5a4bd1;
  transform: translateY(-2px);
}

.cancel-btn {
  background: #dc3545;
  color: white;
}

.cancel-btn:hover {
  background: #c82333;
  transform: translateY(-2px);
}

.restore-order-btn {
  background: #28a745;
  color: white;
}

.restore-order-btn:hover {
  background: #1e7e34;
  transform: translateY(-2px);
}

.no-data {
  text-align: center;
  padding: 40px;
  color: #999;
}

.no-data i {
  font-size: 40px;
  display: block;
  margin-bottom: 10px;
  color: #ddd;
}

/* ===== PAGINATION ===== */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  background: #e63946;
  color: white;
  border-color: #e63946;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #666;
}

/* ===== MODAL ===== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.modal-content {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s;
}

.modal-content.modal-lg {
  max-width: 1000px;
}

.modal-container {
  background: white;
  border-radius: 15px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s;
}

.modal-container.modal-sm {
  max-width: 450px;
}

.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  background: white;
  border-radius: 15px 15px 0 0;
  z-index: 1;
}

.modal-header h2, .modal-header h3 {
  font-size: 20px;
  color: #2c3e50;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  transition: all 0.3s;
}

.close-btn:hover {
  color: #e63946;
  transform: rotate(90deg);
}

.modal-body {
  padding: 25px;
}

.modal-footer {
  padding: 20px 25px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* ===== PROCESSED INFO IN MODAL ===== */
.processed-info {
  margin-top: 10px;
  padding: 12px 14px;
  background: #e8f5e9;
  border-radius: 8px;
  border-left: 4px solid #28a745;
}

.processed-label {
  font-weight: 600;
  color: #2e7d32;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.processed-detail {
  font-size: 13px;
  color: #333;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
}

.processed-detail div {
  padding: 2px 0;
}

/* ===== ADDRESS SECTION ===== */
.address-section {
  margin-top: 10px;
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #e9ecef;
}

.address-section h3 {
  margin-top: 0;
  color: #2c3e50;
  font-size: 16px;
  margin-bottom: 15px;
}

.address-detail-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.address-row {
  display: flex;
  align-items: flex-start;
  padding: 6px 0;
  border-bottom: 1px solid #f1f3f5;
}

.address-row:last-child {
  border-bottom: none;
}

.address-row .address-label {
  font-weight: 600;
  color: #2c3e50;
  min-width: 140px;
  flex-shrink: 0;
  font-size: 14px;
}

.address-row .address-value {
  color: #444;
  font-size: 14px;
  word-break: break-word;
}

.address-row .address-value.highlight {
  color: #1a3a6a;
  font-weight: 500;
}

.full-address-row {
  background: #e8f0fe;
  padding: 10px 14px;
  border-radius: 6px;
  margin-top: 4px;
}

.full-address-row .address-label {
  color: #1a3a6a;
}

.full-address-row .address-value {
  color: #1a3a6a;
  font-weight: 500;
}

/* ===== ORDER INFO GRID ===== */
.order-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.info-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
}

.info-section h3 {
  color: #2c3e50;
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 15px;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.info-section p {
  margin: 6px 0;
  font-size: 14px;
  color: #555;
}

/* ===== ORDER ITEMS ===== */
.order-items-section {
  margin-top: 20px;
}

.order-items-section h3 {
  color: #2c3e50;
  margin-bottom: 15px;
  font-size: 16px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
}

.items-table th {
  background: #f8f9fa;
  padding: 10px;
  text-align: left;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
  font-size: 13px;
}

.items-table td {
  padding: 10px;
  border-bottom: 1px solid #eee;
  vertical-align: middle;
  font-size: 14px;
}

.items-table tfoot td {
  border-bottom: none;
  padding: 8px 10px;
}

.price-cell, .total-cell {
  text-align: right;
}

.quantity-cell {
  text-align: center;
}

.text-right {
  text-align: right;
}

.text-center {
  text-align: center;
}

.total-row {
  font-weight: 700;
}

.grand-total {
  font-size: 18px;
  color: #e63946;
}

.discount-row td {
  color: #28a745;
  font-weight: 600;
}

.discount-row .voucher-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.discount-amount {
  color: #28a745;
  font-weight: 700;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-image {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-image i {
  font-size: 24px;
  color: #ccc;
}

.item-name {
  font-weight: 600;
  color: #2c3e50;
}

.item-variant {
  font-size: 12px;
  color: #999;
}

/* ===== VOUCHER ===== */
.voucher-info-section {
  background: #e8f5e9;
  padding: 15px 20px;
  border-radius: 8px;
  margin-top: 20px;
  border-left: 4px solid #2e7d32;
}

.voucher-info-section h4 {
  color: #2e7d32;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.voucher-info-section .voucher-detail {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.voucher-info-section .voucher-code {
  font-weight: 700;
  color: #2e7d32;
  background: white;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 16px;
}

.voucher-info-section .voucher-savings {
  color: #e63946;
  font-weight: 700;
  font-size: 15px;
}

/* ===== ORDER NOTES ===== */
.order-notes {
  margin-top: 20px;
  padding: 15px;
  background: #fff8e1;
  border-radius: 8px;
  border-left: 4px solid #f9a825;
}

.order-notes h3 {
  margin-top: 0;
  font-size: 14px;
  color: #2c3e50;
}

.order-notes p {
  margin: 0;
  color: #555;
}

/* ===== RETURN REQUEST ===== */
.return-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.return-reason {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.return-reason h3 {
  margin-top: 0;
  color: #2c3e50;
  font-size: 15px;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.return-reason .reason-content p {
  margin: 6px 0;
  color: #555;
}

.return-images {
  margin-bottom: 20px;
}

.return-images h3 {
  color: #2c3e50;
  font-size: 15px;
  margin-bottom: 10px;
}

.return-images .image-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.return-images .return-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.return-images .return-image:hover {
  border-color: #e63946;
  transform: scale(1.05);
}

.admin-actions {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.admin-actions h3 {
  margin-top: 0;
  color: #2c3e50;
  font-size: 15px;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.admin-actions .action-group {
  margin-top: 10px;
}

.admin-actions .form-group {
  margin-bottom: 10px;
}

.admin-actions .form-group label {
  display: block;
  font-weight: 500;
  margin-bottom: 5px;
  font-size: 13px;
}

.admin-actions .form-control {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
}

.admin-actions .button-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.status-info-box {
  padding: 12px 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-info-box.status-success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-info-box.status-danger {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.return-status h3 {
  color: #2c3e50;
  font-size: 15px;
  margin-bottom: 10px;
}

.return-status .status-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.return-status .update-time {
  font-size: 13px;
  color: #999;
}

/* ===== INVOICE ===== */
.invoice-container {
  padding: 10px 0;
}

.invoice-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 3px solid #e63946;
}

.company-info h1 {
  color: #e63946;
  margin: 0;
  font-size: 28px;
}

.company-info p {
  margin: 5px 0;
  color: #666;
  font-size: 13px;
}

.invoice-title {
  text-align: right;
}

.invoice-title h2 {
  color: #333;
  margin: 0;
  font-size: 22px;
}

.invoice-title p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.invoice-table {
  width: 100%;
  border-collapse: collapse;
  margin: 15px 0;
}

.invoice-table th {
  background: #f8f9fa;
  padding: 10px;
  text-align: left;
  border-bottom: 2px solid #dee2e6;
  font-weight: 600;
  font-size: 14px;
}

.invoice-table td {
  padding: 10px;
  border-bottom: 1px solid #dee2e6;
  vertical-align: middle;
  font-size: 14px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-image {
  width: 40px;
  height: 40px;
  background: #f5f5f5;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-image i {
  font-size: 20px;
  color: #ccc;
}

.product-name {
  font-weight: 600;
}

.product-variant {
  font-size: 12px;
  color: #999;
}

.voucher-code {
  color: #28a745;
  font-weight: 600;
  background: #e8f5e9;
  padding: 2px 8px;
  border-radius: 4px;
}

.invoice-footer {
  margin-top: 30px;
  text-align: center;
  color: #666;
  border-top: 1px solid #eee;
  padding-top: 20px;
  font-style: italic;
}

.invoice-notes {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin: 15px 0;
}

.invoice-notes h4 {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 14px;
}

.invoice-notes p {
  margin: 0;
  color: #666;
}

.payment-status-paid {
  color: #28a745;
  font-weight: bold;
  padding: 8px;
  background: #d4edda;
  border-radius: 4px;
  display: inline-block;
  margin-top: 10px;
}

.payment-status-unpaid {
  color: #dc3545;
  font-weight: bold;
  padding: 8px;
  background: #f8d7da;
  border-radius: 4px;
  display: inline-block;
  margin-top: 10px;
}

/* ===== IMAGE VIEWER ===== */
.image-viewer-container {
  background: white;
  border-radius: 15px;
  padding: 20px;
  max-width: 90vw;
  max-height: 90vh;
  position: relative;
}

.image-viewer-container img {
  max-width: 100%;
  max-height: 80vh;
  border-radius: 8px;
}

.image-viewer-container .close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.image-viewer-container .close-btn:hover {
  background: rgba(0, 0, 0, 0.8);
  transform: rotate(90deg);
}

/* ===== TOAST ===== */
.toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  padding: 12px 24px;
  border-radius: 10px;
  background: #27ae60;
  color: white;
  font-size: 14px;
  z-index: 9999;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.4s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 10px;
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}

/* ===== LOADING ===== */
.loading-state {
  text-align: center;
  padding: 40px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #e63946;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ===== BUTTONS ===== */
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover {
  background: #c82333;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover {
  background: #1e7e34;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

.btn-outline {
  background: transparent;
  color: #666;
  border: 1px solid #ddd;
}

.btn-outline:hover {
  background: #f5f5f5;
}

.btn-sm {
  padding: 6px 14px;
  font-size: 13px;
}

/* ===== WARNING TEXT ===== */
.warning-text {
  margin-top: 15px;
  padding: 12px 16px;
  background: #fff3cd;
  border-radius: 8px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: #856404;
  font-size: 14px;
}

.warning-text i {
  margin-top: 3px;
  font-size: 18px;
}

.warning-text.success-text {
  background: #d4edda;
  color: #155724;
}

.required {
  color: #e63946;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  font-weight: 500;
  margin-bottom: 5px;
  font-size: 14px;
}

.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
}

.form-group textarea:focus {
  outline: none;
  border-color: #e63946;
}

/* ===== ANIMATIONS ===== */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(50px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== RESPONSIVE ===== */
@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .order-info-grid {
    grid-template-columns: 1fr;
  }
  
  .return-info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .admin-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .page-title {
    font-size: 24px;
  }

  .filters-bar {
    flex-direction: column;
  }

  .filters-bar select,
  .btn-search,
  .btn-reset {
    width: 100%;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .stat-card {
    padding: 14px 16px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }

  .stat-number {
    font-size: 18px;
  }
  
  .main-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
  }
  
  .main-tab-btn {
    white-space: nowrap;
    font-size: 13px;
    padding: 8px 16px;
  }
  
  .sub-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
  }
  
  .sub-tab-btn {
    white-space: nowrap;
    font-size: 12px;
    padding: 4px 12px;
  }

  .address-row {
    flex-direction: column;
    padding: 4px 0;
  }
  
  .address-row .address-label {
    min-width: auto;
    font-size: 12px;
  }
  
  .address-row .address-value {
    font-size: 13px;
    padding-left: 10px;
  }
  
  .full-address-row .address-value {
    padding-left: 0;
  }

  .modal-content,
  .modal-container {
    width: 95%;
    max-height: 95vh;
  }

  .action-buttons {
    flex-wrap: wrap;
  }
  
  .processed-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
  
  .stat-card {
    padding: 12px 14px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .stat-icon {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }
  
  .stat-number {
    font-size: 16px;
  }
  
  .stat-label {
    font-size: 11px;
  }

  .modal-header h2 {
    font-size: 18px;
  }
  
  .modal-body {
    padding: 15px;
  }

  .items-table th,
  .items-table td {
    padding: 6px 8px;
    font-size: 12px;
  }
  
  .item-info {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .item-image {
    width: 40px;
    height: 40px;
  }
  
  .processed-by-cell {
    font-size: 10px;
  }
}
</style>
