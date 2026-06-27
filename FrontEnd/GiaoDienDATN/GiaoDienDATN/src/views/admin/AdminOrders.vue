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

    <!-- Thống kê nhanh -->
    <div class="stats-grid" v-if="statistics">
      <div class="stat-card">
        <div class="stat-icon blue">
          <i class="fas fa-shopping-bag"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.totalOrders || 0 }}</span>
          <span class="stat-label">Tổng đơn hàng</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon yellow">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.pendingOrders || 0 }}</span>
          <span class="stat-label">Chờ xử lý</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.completedOrders || 0 }}</span>
          <span class="stat-label">Hoàn thành</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">
          <i class="fas fa-times-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ statistics.cancelledOrders || 0 }}</span>
          <span class="stat-label">Đã hủy</span>
        </div>
      </div>
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
        >
      </div>

      <select v-model="filters.status" @change="applyFilters">
        <option value="all">Tất cả trạng thái</option>
        <option value="PENDING_PAYMENT">Chờ thanh toán</option>
        <option value="CONFIRMED">Đã xác nhận</option>
        <option value="PROCESSING">Đang xử lý</option>
        <option value="SHIPPING">Đang giao</option>
        <option value="DELIVERED">Đã giao</option>
        <option value="COMPLETED">Hoàn thành</option>
        <option value="CANCELLED">Đã hủy</option>
        <option value="RETURN_REQUESTED">Yêu cầu hoàn hàng</option>
        <option value="RETURN_APPROVED">Đã duyệt hoàn hàng</option>
        <option value="RETURN_RECEIVED">Đã nhận hàng trả</option>
        <option value="REJECTED">Đã từ chối hoàn trả</option>
        <option value="RETURNED">Đã hoàn hàng</option>
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
              <div class="action-buttons">
                <!-- Nút Cập nhật trạng thái -->
                <button 
                  class="action-btn update-status-btn" 
                  @click="openUpdateStatusModal(order)"
                  :disabled="updatingOrderIds.has(order.orderId) || !canUpdateStatus(order.orderStatus)"
                  title="Cập nhật trạng thái"
                >
                  <i class="fas fa-edit"></i>
                </button>
                <button class="action-btn view-btn" @click="viewOrderDetail(order)" title="Xem chi tiết">
                  <i class="fas fa-eye"></i>
                </button>
                <!-- Nút Xem yêu cầu hoàn hàng -->
                <button 
                  v-if="['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING'].includes(order.orderStatus)" 
                  class="action-btn return-btn" 
                  @click="viewReturnRequest(order)"
                  title="Xem yêu cầu hoàn hàng"
                >
                  <i class="fas fa-undo-alt"></i>
                </button>
                <!-- Nút In hóa đơn -->
                <button 
                  class="action-btn invoice-btn" 
                  @click="viewInvoice(order)"
                  title="Xem hóa đơn"
                >
                  <i class="fas fa-file-invoice"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="orders.length === 0 && !loading">
            <td colspan="9" class="no-data">
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

    <!-- Order Detail Modal -->
    <div v-if="showDetailModal" class="modal" @click.self="closeDetailModal">
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
            </div>
            <div class="info-section">
              <h3>Địa chỉ giao hàng</h3>
              <p><strong>Người nhận:</strong> {{ selectedOrder?.shippingAddress?.fullName || '---' }}</p>
              <p><strong>SĐT:</strong> {{ selectedOrder?.shippingAddress?.phone || '---' }}</p>
              <p><strong>Địa chỉ:</strong> 
                {{ selectedOrder?.shippingAddress?.addressLine || '---' }},
                {{ selectedOrder?.shippingAddress?.ward || '' }},
                {{ selectedOrder?.shippingAddress?.district || '' }},
                {{ selectedOrder?.shippingAddress?.city || '' }}
              </p>
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
                <tr v-for="item in selectedOrder?.items" :key="item.itemId">
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
                  <td class="total-cell">{{ formatPrice(item.finalAmount) }}</td>
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
    <div v-if="showItemsModal" class="modal" @click.self="closeItemsModal">
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
              <tr v-for="item in selectedOrder?.items" :key="item.itemId">
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
                <td class="total-cell">{{ formatPrice(item.finalAmount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeItemsModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- ✅ Update Status Modal -->
    <UpdateStatusModal
      :visible="showUpdateModal"
      :order="selectedOrderForUpdate"
      @close="closeUpdateStatusModal"
      @updated="onStatusUpdated"
    />

    <!-- ✅ Return Request Detail Modal -->
    <div v-if="showReturnModal" class="modal" @click.self="closeReturnModal">
      <div class="modal-content modal-lg">
        <div class="modal-header">
          <h2>Yêu cầu hoàn hàng</h2>
          <button class="close-btn" @click="closeReturnModal">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="returnRequestDetail" class="return-detail">
            <!-- Thông tin khách hàng -->
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

            <!-- Lý do và mô tả -->
            <div class="return-reason">
              <h3>Lý do hoàn hàng</h3>
              <div class="reason-content">
                <p><strong>Lý do:</strong> {{ returnRequestDetail.reason }}</p>
                <p><strong>Mô tả:</strong> {{ returnRequestDetail.description }}</p>
                <p v-if="returnRequestDetail.adminNote"><strong>Phản hồi Admin:</strong> {{ returnRequestDetail.adminNote }}</p>
              </div>
            </div>

            <!-- Hình ảnh -->
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

            <!-- Admin Actions -->
            <div class="admin-actions">
              <h3>Xử lý yêu cầu</h3>
              
              <!-- Trường hợp PENDING - Chờ duyệt -->
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

              <!-- Trường hợp APPROVED - Đã duyệt, chờ nhận hàng -->
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

              <!-- Trường hợp RECEIVED - Đã nhận hàng, chờ hoàn tất -->
              <template v-if="returnRequestDetail.status === 'RECEIVED'">
                <div class="action-group">
                  <div class="form-group">
                    <label>Ghi chú hoàn tất (tùy chọn)</label>
                    <textarea v-model="adminNote" rows="2" placeholder="Nhập ghi chú hoàn tất hoàn hàng..." class="form-control"></textarea>
                  </div>
                  <div class="button-group">
                    <button class="btn btn-success" @click="completeReturn" :disabled="completingReturn">
                      <i v-if="completingReturn" class="fas fa-spinner fa-spin"></i>
                      {{ completingReturn ? 'Đang xử lý...' : 'Hoàn tất hoàn hàng' }}
                    </button>
                  </div>
                </div>
              </template>

              <!-- Trường hợp COMPLETED - Đã hoàn tất -->
              <template v-if="returnRequestDetail.status === 'COMPLETED'">
                <div class="status-info-box status-success">
                  <i class="fas fa-check-circle"></i>
                  <span>Đã hoàn tất hoàn hàng!</span>
                </div>
              </template>

              <!-- Trường hợp REJECTED - Từ chối -->
              <template v-if="returnRequestDetail.status === 'REJECTED'">
                <div class="status-info-box status-danger">
                  <i class="fas fa-times-circle"></i>
                  <span>Đã từ chối yêu cầu hoàn hàng. Đơn hàng đã được chuyển về trạng thái <strong>Hoàn thành</strong>.</span>
                </div>
              </template>
            </div>

            <!-- Status History -->
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

    <!-- ✅ Invoice Modal -->
    <div v-if="showInvoiceModal" class="modal" @click.self="closeInvoiceModal">
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
            <!-- Header -->
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

            <!-- Thông tin khách hàng -->
            <div class="customer-info">
              <h3>Thông tin khách hàng</h3>
              <div class="info-row">
                <span><strong>Họ tên:</strong> {{ invoiceData.customerName || 'Khách hàng' }}</span>
                <span><strong>SĐT:</strong> {{ invoiceData.customerPhone || '---' }}</span>
                <span><strong>Email:</strong> {{ invoiceData.customerEmail || '---' }}</span>
              </div>
              <div class="info-row">
                <span><strong>Địa chỉ giao hàng:</strong> 
                  {{ invoiceData.shippingFullName || '' }}
                  {{ invoiceData.shippingPhone ? '- SĐT: ' + invoiceData.shippingPhone : '' }}
                  {{ invoiceData.shippingAddress ? '- ' + invoiceData.shippingAddress : '' }}
                  {{ invoiceData.shippingWard ? ', ' + invoiceData.shippingWard : '' }}
                  {{ invoiceData.shippingDistrict ? ', ' + invoiceData.shippingDistrict : '' }}
                  {{ invoiceData.shippingCity ? ', ' + invoiceData.shippingCity : '' }}
                </span>
              </div>
            </div>

            <!-- Danh sách sản phẩm -->
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

            <!-- Ghi chú -->
            <div v-if="invoiceData.notes" class="invoice-notes">
              <h4>Ghi chú:</h4>
              <p>{{ invoiceData.notes }}</p>
            </div>

            <!-- Footer -->
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
    <div v-if="showImageViewer" class="modal" @click.self="closeImageViewer">
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
import { ref, onMounted } from 'vue'
import { adminOrderService } from '../../services/adminOrderService'
import UpdateStatusModal from '../../components/admin/UpdateStatusModal.vue'
import axios from 'axios'

// ===== STATE =====
const loading = ref(false)
const orders = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = ref(10)
const statistics = ref(null)
const updatingOrderIds = ref(new Set())

// Filters
const filters = ref({
  search: '',
  status: 'all'
})

// Modal state
const showDetailModal = ref(false)
const showItemsModal = ref(false)
const showUpdateModal = ref(false)
const showReturnModal = ref(false)
const showInvoiceModal = ref(false)
const showImageViewer = ref(false)
const selectedOrder = ref(null)
const selectedOrderForUpdate = ref(null)
const returnRequestDetail = ref(null)
const invoiceData = ref(null)
const adminNote = ref('')
const processingReturn = ref(false)
const receivingReturn = ref(false)
const completingReturn = ref(false)
const viewingImage = ref('')

// Toast
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

// BASE URL CHO ẢNH
const IMAGE_BASE = "http://localhost:8080"
const API_BASE = "http://localhost:8080/api"

// Các trạng thái Admin KHÔNG thể cập nhật
const ADMIN_CANNOT_UPDATE = ['CANCELLED', 'RETURNED', 'RETURN_REQUESTED', 'RETURN_PENDING']

// HÀM LẤY URL ẢNH
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

// ===== UTILITY FUNCTIONS =====
const showNotification = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 3000)
}

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

const canUpdateStatus = (status) => {
  return !ADMIN_CANNOT_UPDATE.includes(status)
}

// ✅ Cập nhật getStatusText - Thêm REJECTED
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
    'REJECTED': 'Đã từ chối hoàn trả',
    'PENDING': 'Chờ duyệt',
    'APPROVED': 'Đã duyệt',
    'RECEIVED': 'Đã nhận hàng',
    'COMPLETED': 'Hoàn tất'
  }
  return map[status] || status
}

// ✅ Cập nhật getStatusClass - Thêm REJECTED
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
    'REJECTED': 'status-rejected',
    'PENDING': 'status-pending',
    'APPROVED': 'status-approved',
    'RECEIVED': 'status-return-received'
  }
  return map[status] || ''
}

const getPaymentStatusText = (status) => {
  const map = {
    'PAID': 'Đã thanh toán',
    'UNPAID': 'Chưa thanh toán'
  }
  return map[status] || status
}

const getPaymentClass = (status) => {
  const map = {
    'PAID': 'payment-paid',
    'UNPAID': 'payment-unpaid'
  }
  return map[status] || ''
}

// ===== API CALLS =====
const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      _t: Date.now() // Thêm timestamp để tránh cache
    }
    
    if (filters.value.search) {
      params.keyword = filters.value.search
    }
    if (filters.value.status !== 'all') {
      params.status = filters.value.status
    }
    
    console.log('📤 Fetching orders with params:', params)
    
    const response = await adminOrderService.getAllOrders(params)
    
    console.log('📦 Orders response:', response.data)
    
    if (response.data && response.data.content) {
      orders.value = response.data.content
      totalPages.value = response.data.totalPages || 0
      totalElements.value = response.data.totalElements || 0
      currentPage.value = response.data.number || 0
    } else if (Array.isArray(response.data)) {
      orders.value = response.data
      totalPages.value = 1
    } else {
      orders.value = []
    }
  } catch (error) {
    console.error('❌ Error fetching orders:', error)
    showNotification(error.response?.data?.message || 'Không thể tải danh sách đơn hàng', 'error')
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

// ===== UPDATE STATUS =====
const openUpdateStatusModal = (order) => {
  if (!canUpdateStatus(order.orderStatus)) {
    showNotification('Đơn hàng đã ở trạng thái cuối, không thể cập nhật', 'warning')
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
  showNotification(`✅ Đã cập nhật trạng thái đơn hàng #${data.orderId} thành "${getStatusText(data.newStatus)}"`, 'success')
  await fetchOrders()
  await fetchStatistics()
}

// ===== VIEW DETAILS =====
const viewOrderDetail = async (order) => {
  try {
    const response = await adminOrderService.getOrderDetail(order.orderId)
    selectedOrder.value = response.data
    showDetailModal.value = true
  } catch (error) {
    console.error('❌ Error fetching order detail:', error)
    showNotification('Không thể tải chi tiết đơn hàng', 'error')
  }
}

const viewOrderItems = (order) => {
  selectedOrder.value = order
  showItemsModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
}

const closeItemsModal = () => {
  showItemsModal.value = false
  selectedOrder.value = null
}

// ===== INVOICE FUNCTIONS =====
const viewInvoice = async (order) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(
      `${API_BASE}/admin/orders/${order.orderId}/invoice`,
      {
        headers: {
          Authorization: `Bearer ${token}`
        }
      }
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
  
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>Hóa đơn #${invoiceData.value?.orderCode}</title>
      <style>
        * { box-sizing: border-box; }
        body { 
          font-family: 'Times New Roman', Arial, sans-serif; 
          padding: 40px; 
          max-width: 800px; 
          margin: 0 auto;
          background: white;
          color: #333;
        }
        .invoice-header { display: flex; justify-content: space-between; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 3px solid #e63946; }
        .company-info h1 { color: #e63946; margin: 0; font-size: 28px; }
        .company-info p { margin: 5px 0; color: #666; font-size: 13px; }
        .invoice-title { text-align: right; }
        .invoice-title h2 { color: #333; margin: 0; font-size: 22px; }
        .invoice-title p { margin: 5px 0; color: #666; font-size: 14px; }
        .customer-info { background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
        .customer-info h3 { margin-top: 0; color: #333; font-size: 16px; margin-bottom: 10px; }
        .info-row { margin: 5px 0; font-size: 14px; }
        .info-row span { margin-right: 20px; }
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
        @media print { 
          body { padding: 20px; } 
          .no-print { display: none; } 
          .btn { display: none; }
        }
      </style>
    </head>
    <body>
      ${content.innerHTML}
      <div style="margin-top: 20px; text-align: center; color: #999; font-size: 11px; border-top: 1px solid #eee; padding-top: 10px;">
        Hóa đơn được tạo tự động từ hệ thống Fashion Store
      </div>
    </body>
    </html>
  `)
  
  printWindow.document.close()
  setTimeout(() => {
    printWindow.focus()
    printWindow.print()
  }, 500)
}

// ===== RETURN REQUEST FUNCTIONS =====
const viewReturnRequest = async (order) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`${API_BASE}/admin/orders/return-requests/order/${order.orderId}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    
    if (response.data) {
      returnRequestDetail.value = response.data
      adminNote.value = ''
      showReturnModal.value = true
    } else {
      showNotification('Không tìm thấy yêu cầu hoàn hàng', 'warning')
    }
  } catch (error) {
    console.error('❌ Error fetching return request:', error)
    console.error('❌ Response:', error.response?.data)
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
    
    console.log('📤 Processing return request:', payload)
    console.log('📤 Order ID:', returnRequestDetail.value.orderId)
    
    const response = await axios.put(
      `${API_BASE}/admin/orders/return-requests/${returnRequestDetail.value.orderId}/process`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    )
    
    console.log('✅ Return request processed:', response.data)
    
    // Cập nhật lại thông tin chi tiết
    returnRequestDetail.value = response.data
    
    // Hiển thị thông báo khác nhau
    if (status === 'APPROVED') {
      showNotification('✅ Đã duyệt yêu cầu hoàn hàng thành công!', 'success')
    } else {
      showNotification('❌ Đã từ chối yêu cầu hoàn hàng. Đơn hàng đã được chuyển về trạng thái "Hoàn thành".', 'info')
    }
    
    // Refresh danh sách
    await fetchOrders()
    await fetchStatistics()
    
    // Không đóng modal để admin thấy kết quả
    // closeReturnModal()
  } catch (error) {
    console.error('❌ Error processing return request:', error)
    console.error('❌ Response:', error.response?.data)
    showNotification(error.response?.data?.message || `Không thể ${actionText} yêu cầu hoàn hàng`, 'error')
  } finally {
    processingReturn.value = false
  }
}

// Xác nhận đã nhận hàng trả
const receiveReturnedItems = async () => {
  if (!returnRequestDetail.value) return
  
  if (!confirm('Bạn đã kiểm tra và nhận đủ hàng trả từ khách hàng?')) return
  
  receivingReturn.value = true
  
  try {
    const token = localStorage.getItem('token')
    const payload = {
      note: adminNote.value.trim() || null
    }
    
    console.log('📤 Receiving returned items:', payload)
    
    const response = await axios.put(
      `${API_BASE}/admin/orders/return-requests/${returnRequestDetail.value.orderId}/receive`,
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
    console.error('❌ Response:', error.response?.data)
    showNotification(error.response?.data?.message || 'Không thể xác nhận nhận hàng trả', 'error')
  } finally {
    receivingReturn.value = false
  }
}

// Hoàn tất hoàn hàng
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
    
    console.log('📤 Completing return for order:', returnRequestDetail.value.orderId)
    console.log('📤 Payload:', payload)
    
    const response = await axios.put(
      `${API_BASE}/admin/orders/${returnRequestDetail.value.orderId}/status`,
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
    console.error('❌ Response:', error.response?.data)
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

// ===== PAGINATION =====
const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    fetchOrders()
  }
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
  currentPage.value = 0
  fetchOrders()
}

// ===== LIFECYCLE =====
onMounted(() => {
  console.log('🚀 AdminOrders mounted')
  fetchOrders()
  fetchStatistics()
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
  grid-template-columns: repeat(4, 1fr);
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
  min-width: 150px;
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

/* ===== LOADING ===== */
.loading-state {
  text-align: center;
  padding: 60px;
  background: white;
  border-radius: 15px;
}

.spinner {
  width: 50px;
  height: 50px;
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
  min-width: 900px;
}

.data-table th {
  background: #f8f9fa;
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
}

.data-table td {
  padding: 15px;
  border-bottom: 1px solid #eee;
  color: #666;
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
/* ✅ Thêm style cho REJECTED */
.status-badge.status-rejected {
  background: #f8d7da;
  color: #721c24;
}
.status-badge.status-approved {
  background: #d4edda;
  color: #155724;
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
  gap: 8px;
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
}

.view-btn {
  background: #17a2b8;
  color: white;
}

.view-btn:hover {
  background: #138496;
  transform: translateY(-2px);
}

.return-btn {
  background: #ffc107;
  color: #333;
}

.return-btn:hover {
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
.modal {
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

.modal-header h2 {
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

/* ===== ORDER DETAIL ===== */
.order-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.info-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
}

.info-section h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 10px;
  padding-bottom: 5px;
  border-bottom: 1px solid #eee;
}

.info-section p {
  margin-bottom: 5px;
  font-size: 14px;
  color: #666;
}

.order-items-section {
  margin-bottom: 30px;
}

.order-items-section h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 15px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
}

.items-table th {
  background: #f8f9fa;
  padding: 12px;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
}

.items-table td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  vertical-align: middle;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-image {
  width: 40px;
  height: 40px;
  background: #f5f5f5;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-image i {
  font-size: 20px;
  color: #ccc;
}

.item-name {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 3px;
}

.item-variant {
  font-size: 12px;
  color: #999;
}

.price-cell, .quantity-cell, .total-cell {
  text-align: right;
}

.text-right {
  text-align: right;
}

.grand-total {
  font-size: 18px;
  font-weight: 700;
}

.total-row {
  font-weight: 700;
  background: #fafafa;
}

.discount-row td {
  color: #28a745;
  font-weight: 600;
}

.discount-amount {
  color: #28a745;
  font-weight: 700;
}

.voucher-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

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

.order-notes {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-top: 20px;
}

.order-notes h3 {
  font-size: 14px;
  color: #2c3e50;
  margin-bottom: 8px;
}

.order-notes p {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

/* ===== RETURN DETAIL ===== */
.return-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.return-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.return-reason {
  background: #f8f9fa;
  padding: 15px 20px;
  border-radius: 8px;
}

.return-reason h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.return-reason .reason-content p {
  margin-bottom: 8px;
  font-size: 14px;
  color: #555;
}

.return-images h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.return-images .image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.return-images .return-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #e9ecef;
  transition: all 0.3s;
}

.return-images .return-image:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.admin-actions {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
}

.admin-actions h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 15px;
}

.admin-actions .action-group {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.admin-actions .form-group {
  margin-bottom: 0;
}

.admin-actions .form-group label {
  display: block;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 6px;
}

.admin-actions .form-control {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  transition: all 0.3s;
}

.admin-actions .form-control:focus {
  outline: none;
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.admin-actions .button-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.admin-actions .button-group .btn {
  min-width: 120px;
}

.status-info-box {
  padding: 12px 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
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
  font-size: 20px;
}

.return-status {
  background: #f8f9fa;
  padding: 15px 20px;
  border-radius: 8px;
}

.return-status h3 {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.return-status .status-info {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.return-status .update-time {
  font-size: 13px;
  color: #999;
}

/* ===== INVOICE ===== */
.invoice-container {
  padding: 10px;
}

.invoice-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e63946;
}

.company-info h1 {
  color: #e63946;
  margin: 0;
  font-size: 28px;
}

.company-info p {
  margin: 5px 0;
  color: #666;
}

.invoice-title {
  text-align: right;
}

.invoice-title h2 {
  color: #333;
  margin: 0;
  font-size: 24px;
}

.invoice-title p {
  margin: 5px 0;
  color: #666;
}

.customer-info {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.customer-info h3 {
  margin-top: 0;
  color: #333;
  font-size: 16px;
  margin-bottom: 10px;
}

.info-row {
  margin: 5px 0;
  font-size: 14px;
}

.invoice-items h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 15px;
}

.invoice-table {
  width: 100%;
  border-collapse: collapse;
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

.invoice-footer {
  margin-top: 30px;
  text-align: center;
  color: #666;
  border-top: 1px solid #eee;
  padding-top: 20px;
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
}

.invoice-notes p {
  margin: 0;
  color: #666;
}

.payment-status-paid {
  color: #28a745;
  font-weight: bold;
  padding: 5px 10px;
  background: #d4edda;
  border-radius: 4px;
  display: inline-block;
  margin-top: 10px;
}

.payment-status-unpaid {
  color: #dc3545;
  font-weight: bold;
  padding: 5px 10px;
  background: #f8d7da;
  border-radius: 4px;
  display: inline-block;
  margin-top: 10px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-actions .btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

/* ===== IMAGE VIEWER ===== */
.image-viewer-container {
  max-width: 90vw;
  max-height: 90vh;
  position: relative;
}

.image-viewer-container img {
  max-width: 100%;
  max-height: 85vh;
  object-fit: contain;
  border-radius: 8px;
}

.image-viewer-container .close-btn {
  position: absolute;
  top: -40px;
  right: -40px;
  background: white;
  color: #333;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.image-viewer-container .close-btn:hover {
  transform: rotate(90deg);
  background: #e63946;
  color: white;
}

/* ===== BUTTONS ===== */
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
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
  background: none;
  border: 1px solid #ddd;
  color: #666;
}

.btn-outline:hover {
  border-color: #e63946;
  color: #e63946;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

@keyframes slideUp {
  from { opacity: 0; transform: translateY(50px); }
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

  .order-info-grid {
    grid-template-columns: 1fr;
  }

  .modal-content {
    width: 95%;
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
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
    align-items: center;
  }
}
</style>