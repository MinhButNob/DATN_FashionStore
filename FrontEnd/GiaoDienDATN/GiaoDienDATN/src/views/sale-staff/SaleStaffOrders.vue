<!-- views/sale-staff/SaleStaffOrders.vue -->
<template>
  <div class="sale-staff-orders">
    <div class="admin-header">
      <h1 class="page-title">Quản lý đơn hàng</h1>
      <div class="header-actions">
        <button class="btn-refresh" @click="fetchOrders">
          <i class="fas fa-sync-alt"></i> Làm mới
        </button>
      </div>
    </div>

    <!-- Thống kê nhanh -->
    <div class="stats-grid" v-if="statistics">
      <div class="stat-card" @click="navigateTo('orders')">
        <div class="stat-icon blue">
          <i class="fas fa-shopping-bag"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.totalOrders || 0 }}</span>
          <span class="stat-label">Tổng đơn hàng</span>
        </div>
      </div>
      <div class="stat-card" @click="navigateTo('orders?status=PENDING_PAYMENT')">
        <div class="stat-icon yellow">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.pendingOrders || 0 }}</span>
          <span class="stat-label">Chờ xử lý</span>
        </div>
      </div>
      <div class="stat-card" @click="navigateTo('orders?status=COMPLETED')">
        <div class="stat-icon green">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.completedOrders || 0 }}</span>
          <span class="stat-label">Hoàn thành</span>
        </div>
      </div>
      <div class="stat-card" @click="navigateTo('orders?status=CANCELLED')">
        <div class="stat-icon red">
          <i class="fas fa-times-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.cancelledOrders || 0 }}</span>
          <span class="stat-label">Đã hủy</span>
        </div>
      </div>
      <div class="stat-card" @click="navigateTo('orders?tab=returns')">
        <div class="stat-icon orange">
          <i class="fas fa-undo-alt"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.returnOrders || 0 }}</span>
          <span class="stat-label">Hoàn hàng</span>
        </div>
      </div>
    </div>

    <!-- ===== MAIN TABS: 3 TAB ===== -->
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
          <option value="PENDING_PAYMENT">Chờ xác nhận</option>
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
            <td>{{ order.customer?.phone || '---' }}</td>
            <td>
              <div class="product-count">{{ order.items?.length || 0 }} sản phẩm</div>
              <button class="view-items-btn" @click="viewOrderDetail(order)" title="Xem chi tiết và đổi trạng thái">
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
            <!-- ✅ CỘT XÁC NHẬN BỞI -->
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
                <button 
                  v-if="activeMainTab === 'orders'"
                  class="action-btn update-status-btn" 
                  @click="quickUpdateStatus(order)"
                  :disabled="updatingOrderIds.has(order.orderId) || !canUpdateStatus(order)"
                  :title="getUpdateStatusTooltip(order)"
                >
                  <i class="fas fa-exchange-alt"></i>
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
                  v-if="activeMainTab === 'orders' && canStaffCancel(order.orderStatus)"
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
              <p><strong>SĐT:</strong> {{ selectedOrder?.customer?.phone || '---' }}</p>
              <p><strong>Email:</strong> {{ selectedOrder?.customer?.email || '---' }}</p>
            </div>
            <div class="info-section">
              <h3>Thông tin đơn hàng</h3>
              <p><strong>Ngày đặt:</strong> {{ formatDate(selectedOrder?.orderDate) }}</p>
              <p><strong>Trạng thái:</strong> {{ getStatusText(selectedOrder?.orderStatus) }}</p>
              <p><strong>Thanh toán:</strong> {{ getPaymentStatusText(selectedOrder?.paymentStatus) }}</p>
              
              <!-- ✅ THÊM: Hiển thị người xác nhận đơn hàng -->
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
              <div class="address-row">
                <span class="address-label">🏠 Địa chỉ chi tiết:</span>
                <span class="address-value">{{ selectedOrder?.shippingAddress?.addressLine || '---' }}</span>
              </div>
              <div class="address-row" v-if="selectedOrder?.shippingAddress?.ward">
                <span class="address-label">🏘️ Phường/Xã:</span>
                <span class="address-value">{{ selectedOrder?.shippingAddress?.ward }}</span>
              </div>
              <div class="address-row" v-if="selectedOrder?.shippingAddress?.district">
                <span class="address-label">🏙️ Quận/Huyện:</span>
                <span class="address-value">{{ selectedOrder?.shippingAddress?.district }}</span>
              </div>
              <div class="address-row" v-if="selectedOrder?.shippingAddress?.city">
                <span class="address-label">🌆 Tỉnh/Thành phố:</span>
                <span class="address-value">{{ selectedOrder?.shippingAddress?.city }}</span>
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
          <button class="btn btn-primary" @click="printInvoice(selectedOrder?.orderId)">
            <i class="fas fa-print"></i> In hóa đơn
          </button>
          <button v-if="activeMainTab === 'orders' && canUpdateStatus(selectedOrder)" class="btn btn-primary" @click="openStatusFromDetail">
            <i class="fas fa-exchange-alt"></i> Đổi trạng thái
          </button>
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
    <UpdateOrderStatusModal
      :visible="showUpdateModal"
      :order="selectedOrderForUpdate"
      @close="closeUpdateStatusModal"
      @updated="onStatusUpdated"
    />

    <!-- Cancel Order Modal -->
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
      <div class="modal-content return-modal">
        <div class="modal-header">
          <div class="header-title">
            <i class="fas fa-undo-alt header-icon"></i>
            <h2>Chi tiết yêu cầu hoàn hàng</h2>
          </div>
          <div class="header-actions">
            <span class="request-status" :class="getStatusClass(returnRequestDetail?.status)">
              {{ getStatusText(returnRequestDetail?.status) }}
            </span>
            <button class="close-btn" @click="closeReturnModal">✕</button>
          </div>
        </div>

        <div class="modal-body">
          <div v-if="returnRequestDetail" class="return-detail">
            <!-- Thông tin đơn hàng -->
            <div class="return-order-info">
              <div class="order-header-info">
                <div class="order-code">
                  <span class="label">Mã đơn hàng:</span>
                  <span class="value">#{{ returnRequestDetail.orderCode || returnRequestDetail.orderId }}</span>
                </div>
                <div class="order-date">
                  <span class="label">Ngày yêu cầu:</span>
                  <span class="value">{{ formatDate(returnRequestDetail.createdAt) }}</span>
                </div>
              </div>
            </div>

            <!-- Thông tin khách hàng -->
            <div class="return-info-grid">
              <div class="info-card customer-card">
                <div class="card-header">
                  <i class="fas fa-user-circle card-icon"></i>
                  <h3>Thông tin khách hàng</h3>
                </div>
                <div class="card-body">
                  <div class="info-row">
                    <span class="label">Họ tên:</span>
                    <span class="value">{{ returnRequestDetail.customerName || 'N/A' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">SĐT:</span>
                    <span class="value">{{ returnRequestDetail.customerPhone || 'N/A' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">Email:</span>
                    <span class="value">{{ returnRequestDetail.customerEmail || 'N/A' }}</span>
                  </div>
                </div>
              </div>

              <div class="info-card order-card">
                <div class="card-header">
                  <i class="fas fa-shopping-bag card-icon"></i>
                  <h3>Thông tin đơn hàng</h3>
                </div>
                <div class="card-body">
                  <div class="info-row">
                    <span class="label">Trạng thái đơn:</span>
                    <span :class="['status-badge', getStatusClass(returnRequestDetail.orderStatus)]">
                      {{ getStatusText(returnRequestDetail.orderStatus) }}
                    </span>
                  </div>
                  <div class="info-row">
                    <span class="label">Ngày đặt:</span>
                    <span class="value">{{ formatDate(returnRequestDetail.orderDate) }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">Tổng tiền:</span>
                    <span class="value total-amount">{{ formatPrice(returnRequestDetail.totalAmount) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Thông tin sản phẩm hoàn -->
            <div class="return-items-section">
              <div class="section-header">
                <h3><i class="fas fa-boxes"></i> Sản phẩm yêu cầu hoàn</h3>
                <span class="items-count">{{ returnRequestDetail.returnItems?.length || 0 }} sản phẩm</span>
              </div>
              <div class="return-items-list">
                <div 
                  v-for="(item, index) in returnRequestDetail.returnItems" 
                  :key="index"
                  class="return-item-card"
                >
                  <div class="item-image">
                    <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" />
                    <i v-else class="fas fa-tshirt"></i>
                  </div>
                  <div class="item-details">
                    <div class="item-name">{{ item.productName }}</div>
                    <div class="item-variant">
                      {{ item.colorName || '' }} {{ item.sizeName || '' }}
                    </div>
                    <div class="item-meta">
                      <span class="item-quantity">Số lượng: <strong>{{ item.quantity }}</strong></span>
                      <span class="item-price">Đơn giá: {{ formatPrice(item.unitPrice) }}</span>
                    </div>
                  </div>
                  <div class="item-refund">
                    <span class="refund-label">Hoàn lại</span>
                    <span class="refund-amount">{{ formatPrice(item.refundAmount) }}</span>
                  </div>
                </div>
              </div>
              <div class="refund-total">
                <span class="total-label">Tổng tiền hoàn lại:</span>
                <span class="total-amount">{{ formatPrice(returnRequestDetail.totalRefundAmount) }}</span>
              </div>
            </div>

            <!-- Lý do hoàn hàng -->
            <div class="return-reason">
              <div class="section-header">
                <h3><i class="fas fa-info-circle"></i> Lý do hoàn hàng</h3>
              </div>
              <div class="reason-content">
                <div class="reason-item">
                  <span class="label">Lý do:</span>
                  <span class="value">{{ returnRequestDetail.reason }}</span>
                </div>
                <div class="reason-item">
                  <span class="label">Mô tả:</span>
                  <span class="value description">{{ returnRequestDetail.description }}</span>
                </div>
                <div v-if="returnRequestDetail.adminNote" class="reason-item admin-note">
                  <span class="label">Phản hồi Staff:</span>
                  <span class="value">{{ returnRequestDetail.adminNote }}</span>
                </div>
              </div>
            </div>

            <!-- Hình ảnh minh họa -->
            <div v-if="returnRequestDetail.images && returnRequestDetail.images.length > 0" class="return-images">
              <div class="section-header">
                <h3><i class="fas fa-images"></i> Hình ảnh minh họa</h3>
                <span class="images-count">{{ returnRequestDetail.images.length }} ảnh</span>
              </div>
              <div class="image-list">
                <div 
                  v-for="(img, index) in returnRequestDetail.images" 
                  :key="index"
                  class="image-item"
                  @click="viewImage(img)"
                >
                  <img :src="getImageUrl(img)" :alt="'Hình ảnh ' + (index + 1)" />
                  <div class="image-overlay">
                    <i class="fas fa-search-plus"></i>
                  </div>
                </div>
              </div>
            </div>

            <!-- Admin Actions -->
            <div v-if="activeMainTab === 'returns'" class="admin-actions">
              <div class="section-header">
                <h3><i class="fas fa-tools"></i> Xử lý yêu cầu</h3>
              </div>
              
              <!-- PENDING - Chờ duyệt -->
              <template v-if="returnRequestDetail.status === 'PENDING'">
                <div class="action-group">
                  <div class="form-group">
                    <label>Phản hồi (tùy chọn)</label>
                    <textarea v-model="adminNote" rows="2" placeholder="Nhập phản hồi cho khách hàng..." class="form-control"></textarea>
                  </div>
                  <div class="button-group">
                    <button class="btn btn-success" @click="processReturn('APPROVED')" :disabled="processingReturn">
                      <i v-if="processingReturn" class="fas fa-spinner fa-spin"></i>
                      {{ processingReturn ? 'Đang xử lý...' : '✅ Duyệt hoàn hàng' }}
                    </button>
                    <button class="btn btn-danger" @click="processReturn('REJECTED')" :disabled="processingReturn">
                      <i v-if="processingReturn" class="fas fa-spinner fa-spin"></i>
                      {{ processingReturn ? 'Đang xử lý...' : '❌ Từ chối' }}
                    </button>
                  </div>
                </div>
              </template>

              <!-- APPROVED - Đã duyệt, chờ nhận hàng -->
              <template v-if="returnRequestDetail.status === 'APPROVED'">
                <div class="action-group">
                  <div class="form-group">
                    <label>Ghi chú khi nhận hàng (tùy chọn)</label>
                    <textarea v-model="adminNote" rows="2" placeholder="Nhập ghi chú khi nhận hàng trả..." class="form-control"></textarea>
                  </div>
                  <div class="button-group">
                    <button class="btn btn-primary" @click="receiveReturnedItems" :disabled="receivingReturn || processingReturn">
                      <i v-if="receivingReturn" class="fas fa-spinner fa-spin"></i>
                      {{ receivingReturn ? 'Đang xử lý...' : '📦 Đã nhận hàng trả' }}
                    </button>
                    <button class="btn btn-danger" @click="processReturn('REJECTED')" :disabled="receivingReturn || processingReturn">
                      <i v-if="processingReturn" class="fas fa-spinner fa-spin"></i>
                      {{ processingReturn ? 'Đang xử lý...' : '❌ Từ chối' }}
                    </button>
                  </div>
                </div>
              </template>

              <!-- RECEIVED - Đã nhận hàng, chờ hoàn tất -->
              <template v-if="returnRequestDetail.status === 'RECEIVED'">
                <div class="action-group">
                  <div class="form-group">
                    <label>Ghi chú hoàn tất (tùy chọn)</label>
                    <textarea v-model="adminNote" rows="2" placeholder="Nhập ghi chú hoàn tất hoàn hàng..." class="form-control"></textarea>
                  </div>
                  <div class="button-group">
                    <button class="btn btn-success" @click="completeReturn" :disabled="completingReturn || processingReturn">
                      <i v-if="completingReturn" class="fas fa-spinner fa-spin"></i>
                      {{ completingReturn ? 'Đang xử lý...' : '✅ Hoàn tất hoàn hàng' }}
                    </button>
                    <button class="btn btn-danger" @click="processReturn('REJECTED')" :disabled="completingReturn || processingReturn">
                      <i v-if="processingReturn" class="fas fa-spinner fa-spin"></i>
                      {{ processingReturn ? 'Đang xử lý...' : '❌ Từ chối hoàn hàng' }}
                    </button>
                  </div>
                </div>
              </template>

              <!-- COMPLETED - Đã hoàn tất -->
              <template v-if="returnRequestDetail.status === 'COMPLETED'">
                <div class="status-info-box status-success">
                  <i class="fas fa-check-circle"></i>
                  <span>Đã hoàn tất hoàn hàng!</span>
                </div>
              </template>

              <!-- REJECTED - Từ chối -->
              <template v-if="returnRequestDetail.status === 'REJECTED'">
                <div class="status-info-box status-danger">
                  <i class="fas fa-times-circle"></i>
                  <span>Đã từ chối yêu cầu hoàn hàng.</span>
                </div>
              </template>
            </div>

            <!-- Trạng thái hiện tại -->
            <div class="return-status">
              <div class="section-header">
                <h3><i class="fas fa-clock"></i> Trạng thái hiện tại</h3>
              </div>
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
    <InvoiceModal
      :visible="showInvoiceModal"
      :order="selectedOrder"
      :invoice-data="invoiceData"
      @close="closeInvoiceModal"
      @print="printInvoice"
    />

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

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSaleStaffStore } from '../../stores/saleStaffStore'
import { 
  formatPrice, 
  formatDate, 
  getStatusText, 
  getStatusClass, 
  getPaymentStatusText, 
  getPaymentClass 
} from '../../utils/helpers'
import UpdateOrderStatusModal from '../../components/sale-staff/UpdateOrderStatusModal.vue'
import InvoiceModal from '../../components/sale-staff/InvoiceModal.vue'
import { useNotification } from '../../composables/useNotification'
import api from "@/api/axios";

const router = useRouter()
const store = useSaleStaffStore()
const { showNotification } = useNotification()

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
    { value: 'PENDING_PAYMENT', label: 'Chờ xác nhận', icon: 'fas fa-clock' },
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
const viewingImage = ref('')

// ===== TOAST =====
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

// ===== CONSTANTS =====
const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");
const CANNOT_UPDATE = ['CANCELLED', 'RETURNED', 'RETURN_REQUESTED', 'RETURN_PENDING', 'REJECTED', 'COMPLETED']

// ===== HELPERS =====
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

const showToastMessage = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 3000)
}

// ✅ Lấy email người dùng hiện tại từ localStorage
const getCurrentUserEmail = () => {
  return localStorage.getItem('userEmail') || ''
}

// ✅ Lấy username người dùng hiện tại
const getCurrentUsername = () => {
  return localStorage.getItem('username') || ''
}

// ✅ Kiểm tra xem có thể cập nhật trạng thái không
const canUpdateStatus = (order) => {
  if (!order) return false
  
  const status = order.orderStatus
  
  // Không thể cập nhật nếu là trạng thái cuối
  if (CANNOT_UPDATE.includes(status)) {
    return false
  }
  
  // ✅ Nếu đơn hàng đã được xác nhận bởi người khác và đang ở trạng thái CONFIRMED hoặc PROCESSING
  if (order.processedBy && order.processedByEmail) {
    const currentEmail = getCurrentUserEmail()
    // Chỉ người đã xác nhận mới được tiếp tục cập nhật
    if ((status === 'CONFIRMED' || status === 'PROCESSING') && 
        order.processedByEmail !== currentEmail) {
      return false
    }
  }
  
  return true
}

// ✅ Tooltip cho nút cập nhật trạng thái
const getUpdateStatusTooltip = (order) => {
  if (!order) return ''
  
  const status = order.orderStatus
  
  if (CANNOT_UPDATE.includes(status)) {
    return `Không thể cập nhật trạng thái "${getStatusText(status)}"`
  }
  
  if (order.processedBy && order.processedByEmail) {
    const currentEmail = getCurrentUserEmail()
    if ((status === 'CONFIRMED' || status === 'PROCESSING') && 
        order.processedByEmail !== currentEmail) {
      return `⚠️ Đơn hàng đã được xác nhận bởi ${order.processedBy} (${order.processedByEmail})\nChỉ người xác nhận mới được cập nhật tiếp!`
    }
  }
  
  return 'Cập nhật trạng thái đơn hàng'
}

const canStaffCancel = (status) => {
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
  if (!isReturnOrder(order?.orderStatus)) return order

  const response = await api.get(`/sale-staff/orders/return-requests/order/${order.orderId}`)
  return applyReturnItemsToOrder(order, response.data?.data || response.data)
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

// ===== NAVIGATE =====
const navigateTo = (path) => {
  router.push(`/sale-staff/${path}`)
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
    
    const token = localStorage.getItem('token')
    const response = await api.get(`/sale-staff/orders`, {
      params })
    
    console.log('📦 Orders response:', response.data)
    
    if (response.data && response.data.data) {
      const data = response.data.data
      orders.value = await enrichReturnOrders(data.content || [])
      totalPages.value = data.totalPages || 0
      totalElements.value = data.totalElements || 0
      currentPage.value = data.number || 0
    } else if (response.data && response.data.content) {
      orders.value = await enrichReturnOrders(response.data.content || [])
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
      showToastMessage(error.response?.data?.message || 'Không thể tải danh sách đơn hàng', 'error')
    }
    orders.value = []
  } finally {
    loading.value = false
  }
}

// ===== FETCH STATISTICS =====
const fetchStatistics = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(`/sale-staff/orders/statistics`, {})
    
    if (response.data && response.data.data) {
      statistics.value = response.data.data
    } else if (response.data) {
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

// ===== VIEW DETAILS =====
const viewOrderDetail = async (order) => {
  try {
    selectedOrder.value = await loadOrderForDisplay(order)
    showDetailModal.value = true
  } catch (error) {
    console.error('Không thể tải chi tiết hàng hoàn:', error)
    showToastMessage('Không thể tải chi tiết hàng hoàn', 'error')
  }
}

const viewOrderItems = async (order) => {
  try {
    selectedOrder.value = await loadOrderForDisplay(order)
    showItemsModal.value = true
  } catch (error) {
    console.error('Không thể tải sản phẩm hoàn trả:', error)
    showToastMessage('Không thể tải sản phẩm hoàn trả', 'error')
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
}

const openStatusFromDetail = () => {
  const order = selectedOrder.value
  closeDetailModal()
  if (order) openUpdateStatusModal(order)
}

const quickUpdateStatus = async (order) => {
  const nextStatus = {
    PENDING_PAYMENT: 'CONFIRMED', CONFIRMED: 'PROCESSING', PROCESSING: 'SHIPPING',
    SHIPPING: 'DELIVERED', DELIVERED: 'COMPLETED'
  }[order?.orderStatus]
  if (!nextStatus || !canUpdateStatus(order)) return

  updatingOrderIds.value.add(order.orderId)
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`/sale-staff/orders/${order.orderId}/status`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ orderStatus: nextStatus, notes: '' })
    })
    if (!response.ok) {
      const data = await response.json()
      throw new Error(data.message || 'Không thể cập nhật trạng thái đơn hàng')
    }
    showNotification(`Đã chuyển đơn hàng #${order.orderCode || order.orderId} sang ${getStatusText(nextStatus)}`, 'success')
    await fetchOrders()
    await fetchStatistics()
  } catch (error) {
    showNotification(error.message || 'Không thể cập nhật trạng thái đơn hàng', 'error')
  } finally {
    updatingOrderIds.value.delete(order.orderId)
  }
}

const closeItemsModal = () => {
  showItemsModal.value = false
  selectedOrder.value = null
}

// ===== UPDATE STATUS =====
const openUpdateStatusModal = (order) => {
  if (!canUpdateStatus(order)) {
    const tooltip = getUpdateStatusTooltip(order)
    showToastMessage(tooltip, 'warning')
    return
  }
  selectedOrderForUpdate.value = order
  showUpdateModal.value = true
}

const closeUpdateStatusModal = () => {
  showUpdateModal.value = false
  selectedOrderForUpdate.value = null
}

const onStatusUpdated = async (data) => {
  showToastMessage(`✅ Đã cập nhật trạng thái đơn hàng #${data.orderId} thành "${getStatusText(data.newStatus)}"`, 'success')
  await fetchOrders()
  await fetchStatistics()
}

// ===== INVOICE =====
const viewInvoice = async (order) => {
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(
      `/sale-staff/orders/${order.orderId}/invoice`,
      {}
    )
    
    if (response.data && response.data.data) {
      invoiceData.value = response.data.data
    } else if (response.data) {
      invoiceData.value = response.data
    }
    selectedOrder.value = order
    showInvoiceModal.value = true
  } catch (error) {
    console.error('❌ Error fetching invoice:', error)
    showToastMessage(error.response?.data?.message || 'Không thể tải hóa đơn', 'error')
  }
}

const closeInvoiceModal = () => {
  showInvoiceModal.value = false
  selectedOrder.value = null
  invoiceData.value = null
}

const printInvoice = (orderId) => {
  // Implement print logic
  window.print()
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
    showToastMessage('Vui lòng nhập lý do hủy đơn hàng', 'error')
    return
  }

  cancelLoading.value = true
  try {
    const token = localStorage.getItem('token')
    const payload = {
      reason: cancelReason.value.trim()
    }

    const response = await api.put(
      `/sale-staff/orders/${cancelOrderId.value}/cancel`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )

    console.log('✅ Order cancelled:', response.data)
    showToastMessage('✅ Hủy đơn hàng thành công!', 'success')
    closeCancelModal()
    await fetchOrders()
    await fetchStatistics()
  } catch (error) {
    console.error('❌ Error canceling order:', error)
    showToastMessage(error.response?.data?.message || 'Không thể hủy đơn hàng', 'error')
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
      notes: 'Staff khôi phục đơn hàng từ trạng thái đã hủy'
    }

    const response = await api.put(
      `/sale-staff/orders/${restoreOrder.value.orderId}/status`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )

    console.log('✅ Order restored:', response.data)
    showToastMessage('✅ Khôi phục đơn hàng thành công!', 'success')
    closeRestoreModal()
    await fetchOrders()
    await fetchStatistics()
  } catch (error) {
    console.error('❌ Error restoring order:', error)
    showToastMessage(error.response?.data?.message || 'Không thể khôi phục đơn hàng', 'error')
  } finally {
    restoreLoading.value = false
  }
}

// ===== RETURN REQUEST =====
const viewReturnRequest = async (order) => {
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(`/sale-staff/orders/return-requests/order/${order.orderId}`, {})
    
    if (response.data && response.data.data) {
      returnRequestDetail.value = response.data.data
    } else if (response.data) {
      returnRequestDetail.value = response.data
    }
    adminNote.value = ''
    showReturnModal.value = true
  } catch (error) {
    console.error('❌ Error fetching return request:', error)
    showToastMessage(error.response?.data?.message || 'Không thể tải yêu cầu hoàn hàng', 'error')
  }
}

const closeReturnModal = () => {
  showReturnModal.value = false
  returnRequestDetail.value = null
  adminNote.value = ''
  processingReturn.value = false
  receivingReturn.value = false
  completingReturn.value = false
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
      `/sale-staff/orders/return-requests/${returnRequestDetail.value.orderId}/process`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )
    
    console.log('✅ Return request processed:', response.data)
    
    if (response.data && response.data.data) {
      returnRequestDetail.value = response.data.data
    } else if (response.data) {
      returnRequestDetail.value = response.data
    }
    
    if (status === 'APPROVED') {
      showToastMessage('✅ Đã duyệt yêu cầu hoàn hàng thành công!', 'success')
    } else {
      showToastMessage('❌ Đã từ chối yêu cầu hoàn hàng.', 'info')
    }
    
    await fetchOrders()
    await fetchStatistics()
  } catch (error) {
    console.error('❌ Error processing return request:', error)
    showToastMessage(error.response?.data?.message || `Không thể ${actionText} yêu cầu hoàn hàng`, 'error')
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
      `/sale-staff/orders/return-requests/${returnRequestDetail.value.orderId}/receive`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )
    
    console.log('✅ Returned items received:', response.data)
    showToastMessage('✅ Đã xác nhận nhận hàng trả thành công!', 'success')
    
    await fetchOrders()
    await fetchStatistics()
    closeReturnModal()
  } catch (error) {
    console.error('❌ Error receiving returned items:', error)
    showToastMessage(error.response?.data?.message || 'Không thể xác nhận nhận hàng trả', 'error')
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
      `/sale-staff/orders/${returnRequestDetail.value.orderId}/status`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )
    
    console.log('✅ Return completed:', response.data)
    showToastMessage('✅ Đã hoàn tất hoàn hàng thành công!', 'success')
    
    await fetchOrders()
    await fetchStatistics()
    closeReturnModal()
  } catch (error) {
    console.error('❌ Error completing return:', error)
    showToastMessage(error.response?.data?.message || 'Không thể hoàn tất hoàn hàng', 'error')
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

// ===== WATCH =====
watch([activeMainTab, activeSubTab], () => {
  fetchOrders()
})

// ===== LIFECYCLE =====
onMounted(() => {
  console.log('🚀 SaleStaffOrders mounted')
  fetchOrders()
  fetchStatistics()
})
</script>

<style scoped>
/* ===== RESET & CONTAINER ===== */
.sale-staff-orders {
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
  gap: 20px;
  margin-bottom: 25px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 15px;
  transition: all 0.3s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
}

.stat-icon.blue { background: #3498db; }
.stat-icon.yellow { background: #f39c12; }
.stat-icon.green { background: #2ecc71; }
.stat-icon.red { background: #e74c3c; }
.stat-icon.orange { background: #e67e22; }

.stat-info {
  flex: 1;
}

.stat-number {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
}

.stat-label {
  font-size: 13px;
  color: #999;
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

@keyframes slideUp {
  from { opacity: 0; transform: translateY(50px); }
  to { opacity: 1; transform: translateY(0); }
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
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #e9ecef;
}

.address-section h3 {
  margin-top: 0;
  margin-bottom: 12px;
  color: #2c3e50;
  font-size: 15px;
}

.address-detail-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.address-row {
  display: flex;
  align-items: flex-start;
  padding: 4px 0;
  font-size: 14px;
}

.address-row .address-label {
  font-weight: 600;
  color: #2c3e50;
  min-width: 120px;
  flex-shrink: 0;
}

.address-row .address-value {
  color: #444;
  word-break: break-word;
}

.address-row.full-address-row {
  background: #e8f0fe;
  padding: 8px 12px;
  border-radius: 6px;
  margin-top: 4px;
}

.address-row.full-address-row .address-label {
  color: #1a3a6a;
}

.address-row.full-address-row .address-value.highlight {
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
  margin-top: 0;
  color: #2c3e50;
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

.text-right {
  text-align: right;
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

/* ===== RETURN MODAL ===== */
.return-modal .modal-header {
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  color: white;
  border-radius: 15px 15px 0 0;
  padding: 20px 25px;
}

.return-modal .modal-header .header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.return-modal .modal-header .header-title .header-icon {
  font-size: 24px;
  color: #ffc107;
}

.return-modal .modal-header .header-title h2 {
  color: white;
  margin: 0;
  font-size: 20px;
}

.return-modal .modal-header .header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.return-modal .modal-header .request-status {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.return-modal .modal-header .request-status.status-pending {
  background: #fff3cd;
  color: #856404;
}
.return-modal .modal-header .request-status.status-approved {
  background: #d4edda;
  color: #155724;
}
.return-modal .modal-header .request-status.status-rejected {
  background: #f8d7da;
  color: #721c24;
}
.return-modal .modal-header .request-status.status-return-received {
  background: #cce5ff;
  color: #004085;
}
.return-modal .modal-header .request-status.status-completed {
  background: #d4edda;
  color: #155724;
}

.return-modal .close-btn {
  color: rgba(255, 255, 255, 0.7);
}

.return-modal .close-btn:hover {
  color: white;
  transform: rotate(90deg);
}

.return-order-info {
  background: #f8f9fa;
  padding: 15px 20px;
  border-radius: 10px;
  margin-bottom: 20px;
  border-left: 4px solid #e63946;
}

.return-order-info .order-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.return-order-info .order-header-info .label {
  font-weight: 600;
  color: #555;
  margin-right: 8px;
}

.return-order-info .order-header-info .value {
  color: #2c3e50;
  font-weight: 500;
}

.return-order-info .order-code .value {
  font-size: 16px;
  color: #e63946;
  font-weight: 700;
}

.return-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 25px;
}

.info-card {
  background: #f8f9fa;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e9ecef;
  transition: all 0.3s;
}

.info-card:hover {
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.info-card .card-header {
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-card .card-header .card-icon {
  font-size: 18px;
  color: #e63946;
}

.info-card .card-header h3 {
  margin: 0;
  font-size: 15px;
  color: #2c3e50;
}

.info-card .card-body {
  padding: 15px 16px;
}

.info-card .card-body .info-row {
  display: flex;
  align-items: baseline;
  padding: 5px 0;
  font-size: 14px;
}

.info-card .card-body .info-row .label {
  font-weight: 600;
  color: #666;
  min-width: 80px;
  flex-shrink: 0;
}

.info-card .card-body .info-row .value {
  color: #2c3e50;
}

.info-card .card-body .info-row .total-amount {
  font-weight: 700;
  color: #e63946;
  font-size: 16px;
}

.return-items-section {
  margin-bottom: 25px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-header .items-count,
.section-header .images-count {
  font-size: 13px;
  color: #999;
  background: #f1f3f5;
  padding: 2px 12px;
  border-radius: 20px;
}

.return-items-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.return-item-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px 15px;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  transition: all 0.3s;
}

.return-item-card:hover {
  border-color: #e63946;
  box-shadow: 0 2px 8px rgba(230, 57, 70, 0.08);
}

.return-item-card .item-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.return-item-card .item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.return-item-card .item-image i {
  font-size: 28px;
  color: #ccc;
}

.return-item-card .item-details {
  flex: 1;
}

.return-item-card .item-details .item-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 15px;
}

.return-item-card .item-details .item-variant {
  font-size: 13px;
  color: #999;
  margin: 2px 0;
}

.return-item-card .item-details .item-meta {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #666;
  margin-top: 4px;
}

.return-item-card .item-details .item-meta strong {
  color: #2c3e50;
}

.return-item-card .item-refund {
  text-align: right;
  padding-left: 15px;
  border-left: 1px solid #e9ecef;
  flex-shrink: 0;
}

.return-item-card .item-refund .refund-label {
  display: block;
  font-size: 12px;
  color: #999;
}

.return-item-card .item-refund .refund-amount {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: #28a745;
}

.refund-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 15px;
  padding: 12px 20px;
  background: #e8f5e9;
  border-radius: 10px;
  margin-top: 15px;
  border: 1px solid #c8e6c9;
}

.refund-total .total-label {
  font-weight: 600;
  color: #2e7d32;
  font-size: 15px;
}

.refund-total .total-amount {
  font-size: 20px;
  font-weight: 700;
  color: #e63946;
}

.return-reason {
  background: #f8f9fa;
  padding: 15px 20px;
  border-radius: 10px;
  margin-bottom: 20px;
  border: 1px solid #e9ecef;
}

.reason-content .reason-item {
  display: flex;
  padding: 6px 0;
  font-size: 14px;
}

.reason-content .reason-item .label {
  font-weight: 600;
  color: #666;
  min-width: 80px;
  flex-shrink: 0;
}

.reason-content .reason-item .value {
  color: #2c3e50;
}

.reason-content .reason-item .value.description {
  line-height: 1.6;
}

.reason-content .reason-item.admin-note {
  background: #e3f2fd;
  padding: 8px 12px;
  border-radius: 6px;
  margin-top: 5px;
}

.reason-content .reason-item.admin-note .value {
  color: #0d47a1;
}

.return-images {
  margin-bottom: 20px;
}

.return-images .image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.return-images .image-item {
  width: 120px;
  height: 120px;
  border-radius: 10px;
  overflow: hidden;
  border: 2px solid #e9ecef;
  cursor: pointer;
  position: relative;
  transition: all 0.3s;
}

.return-images .image-item:hover {
  border-color: #e63946;
  transform: scale(1.03);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.return-images .image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.return-images .image-item .image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s;
}

.return-images .image-item:hover .image-overlay {
  opacity: 1;
}

.return-images .image-item .image-overlay i {
  color: white;
  font-size: 28px;
}

.admin-actions {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  border: 1px solid #e9ecef;
}

.admin-actions .action-group {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.admin-actions .form-group label {
  display: block;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 6px;
  font-size: 14px;
}

.admin-actions .form-group .form-control {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  transition: all 0.3s;
  resize: vertical;
}

.admin-actions .form-group .form-control:focus {
  outline: none;
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.admin-actions .button-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-actions .button-group .btn {
  min-width: 140px;
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.admin-actions .button-group .btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.admin-actions .button-group .btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #1e7e34;
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #c82333;
  box-shadow: 0 4px 12px rgba(220, 53, 69, 0.3);
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c62828;
  box-shadow: 0 4px 12px rgba(230, 57, 70, 0.3);
}

.status-info-box {
  padding: 15px 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 500;
  font-size: 15px;
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

.status-info-box i {
  font-size: 22px;
}

.return-status {
  background: #f8f9fa;
  padding: 15px 20px;
  border-radius: 10px;
  border: 1px solid #e9ecef;
}

.return-status .status-info {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.return-status .status-info .update-time {
  font-size: 13px;
  color: #999;
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

/* ===== ANIMATIONS ===== */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== RESPONSIVE ===== */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
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
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .stat-card {
    padding: 15px;
  }

  .stat-number {
    font-size: 20px;
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

  .order-info-grid {
    grid-template-columns: 1fr;
  }
  
  .admin-actions .button-group {
    flex-direction: column;
  }
  
  .admin-actions .button-group .btn {
    width: 100%;
  }
  
  .return-images .return-image {
    width: 80px;
    height: 80px;
  }
  
  .return-item-card {
    flex-wrap: wrap;
  }
  
  .return-item-card .item-refund {
    width: 100%;
    text-align: left;
    padding-left: 0;
    border-left: none;
    padding-top: 8px;
    border-top: 1px solid #e9ecef;
  }
  
  .return-item-card .item-refund .refund-label,
  .return-item-card .item-refund .refund-amount {
    display: inline;
  }
  
  .return-item-card .item-refund .refund-amount {
    margin-left: 8px;
  }
  
  .refund-total {
    flex-direction: column;
    text-align: center;
    padding: 15px;
  }
  
  .processed-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
    align-items: center;
  }
  
  .return-modal .modal-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .return-modal .modal-header .header-actions {
    width: 100%;
    justify-content: space-between;
  }
  
  .return-modal .modal-header .header-title h2 {
    font-size: 17px;
  }
  
  .return-images .image-item {
    width: 60px;
    height: 60px;
  }
  
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
  
  .processed-by-cell {
    font-size: 10px;
  }
}
</style>
