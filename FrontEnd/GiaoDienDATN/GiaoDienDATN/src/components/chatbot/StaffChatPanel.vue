<!-- components/chatbot/StaffChatPanel.vue -->
<template>
  <div class="staff-chat-panel">
    <!-- ✅ HIỂN THỊ CHỌN LOẠI CHAT KHI CHƯA BẮT ĐẦU -->
    <div v-if="showTypeSelector" class="chat-type-selector-wrapper">
      <ChatTypeSelector 
        @chat-started="onChatStarted" 
        @close="onTypeSelectorClose"
      />
    </div>

    <!-- TRẠNG THÁI: WAITING — Đang chờ nhân viên -->
    <div v-else-if="chatStatus === 'WAITING' || chatStatus === 'IDLE'" class="staff-waiting-state">
      <div class="waiting-animation">
        <div class="waiting-dots">
          <span></span><span></span><span></span>
        </div>
        <p class="waiting-text">Đang tìm nhân viên phù hợp...</p>
        <p class="waiting-sub">Bạn có thể nhắn tin ngay, nhân viên sẽ trả lời bạn sớm nhất</p>
      </div>

      <!-- HIỂN THỊ TIN NHẮN -->
      <div v-if="messages.length > 0" class="waiting-messages">
        <div
          v-for="msg in messages"
          :key="msg.messageId || msg.id"
          class="message"
          :class="isMyMessage(msg) ? 'user' : 'staff'"
        >
          <div class="message-bubble">
            <template v-if="isImageMessage(msg.messageContent || msg.content)">
              <img
                :src="extractImageSrc(msg.messageContent || msg.content)"
                class="chat-image"
                @click="openImagePreview(extractImageSrc(msg.messageContent || msg.content))"
                alt="Ảnh"
              />
            </template>
            <template v-else-if="isReturnFormMessage(msg.messageContent || msg.content)">
              <div class="return-form-card">
                <div class="return-card-header">
                  <i class="fas fa-box-open"></i>
                  <strong>Form Yêu Cầu Hoàn Hàng</strong>
                </div>
                <p class="return-card-desc">
                  {{ extractReturnFormText(msg.messageContent || msg.content) || 'Nhấn vào nút bên dưới để chuyển sang trang Đơn hàng của tôi và gửi yêu cầu hoàn hàng.' }}
                </p>
                <button class="go-to-orders-btn" @click="goToMyOrders">
                  <i class="fas fa-external-link-alt"></i> Đến trang Đơn hàng của tôi
                </button>
              </div>
            </template>
            <p v-else>{{ msg.messageContent || msg.content }}</p>
            <span class="message-time">{{ formatTime(msg.sentDate || msg.timestamp) }}</span>
          </div>
        </div>
      </div>

      <!-- Nếu chưa có tin nhắn, hiển thị hướng dẫn -->
      <div v-else class="empty-chat-message">
        <i class="fas fa-comment-dots"></i>
        <p>Hãy nhập tin nhắn để bắt đầu trò chuyện</p>
      </div>

      <button v-if="chatStatus === 'WAITING'" @click="cancelWaiting" class="cancel-btn">
        <i class="fas fa-times"></i> Hủy yêu cầu
      </button>
    </div>

    <!-- TRẠNG THÁI: ACTIVE — Đang chat với nhân viên -->
    <div v-else-if="chatStatus === 'ACTIVE'" class="staff-chat-active">
      <!-- Header nhân viên -->
      <div class="staff-chat-header">
        <div class="staff-avatar">
          <i class="fas fa-user-tie"></i>
          <span class="online-dot"></span>
        </div>
        <div class="staff-info">
          <strong>{{ assignedEmployee || 'Nhân viên CSKH' }}</strong>
          <span class="status-text">Đang hoạt động</span>
        </div>
        <button @click="closeStaffChat" class="end-chat-btn" title="Kết thúc chat">
          <i class="fas fa-phone-slash"></i>
        </button>
      </div>

      <!-- Tin nhắn -->
      <div
        class="staff-messages"
        ref="staffMessagesContainer"
        @dragover.prevent="isDragging = true"
        @dragleave="isDragging = false"
        @drop.prevent="handleDrop"
        :class="{ 'drag-over': isDragging }"
      >
        <div
          v-for="msg in messages"
          :key="msg.messageId || msg.id"
          class="message"
          :class="isMyMessage(msg) ? 'user' : 'staff'"
        >
          <div class="message-content">
            <div class="message-bubble">
              <template v-if="isImageMessage(msg.messageContent || msg.content)">
                <img
                  :src="extractImageSrc(msg.messageContent || msg.content)"
                  class="chat-image"
                  @click="openImagePreview(extractImageSrc(msg.messageContent || msg.content))"
                  alt="Ảnh"
                />
              </template>
              <template v-else-if="isReturnFormMessage(msg.messageContent || msg.content)">
                <div class="return-form-card">
                  <div class="return-card-header">
                    <i class="fas fa-box-open"></i>
                    <strong>Form Yêu Cầu Hoàn Hàng</strong>
                  </div>
                  <p class="return-card-desc">
                    {{ extractReturnFormText(msg.messageContent || msg.content) || 'Nhấn vào nút bên dưới để chuyển sang trang Đơn hàng của tôi và gửi yêu cầu hoàn hàng.' }}
                  </p>
                  <button class="go-to-orders-btn" @click="goToMyOrders">
                    <i class="fas fa-external-link-alt"></i> Đến trang Đơn hàng của tôi
                  </button>
                </div>
              </template>
              <p v-else>{{ msg.messageContent || msg.content }}</p>
              <span class="message-time">{{ formatTime(msg.sentDate || msg.timestamp) }}</span>
            </div>
            <span v-if="!isMyMessage(msg)" class="sender-name">{{ msg.userName || assignedEmployee || 'Nhân viên' }}</span>
          </div>
        </div>
        <!-- Drag overlay -->
        <div v-if="isDragging" class="drag-overlay">
          <i class="fas fa-image"></i>
          <p>Thả ảnh vào đây để gửi</p>
        </div>
      </div>
    </div>

    <!-- TRẠNG THÁI: CONNECTING — Đang kết nối -->
    <div v-else-if="chatStatus === 'CONNECTING'" class="loading-state">
      <i class="fas fa-spinner fa-spin"></i>
      <p>Đang kết nối...</p>
    </div>

    <!-- TRẠNG THÁI: CLOSED -->
    <div v-else-if="chatStatus === 'CLOSED'" class="staff-closed-state">
      <div class="closed-content">
        <i class="fas fa-check-circle"></i>
        <h4>Cuộc trò chuyện đã kết thúc</h4>
        <p>Cảm ơn bạn đã liên hệ với chúng tôi!</p>
        <button @click="resetChat" class="new-chat-btn">
          <i class="fas fa-plus"></i> Bắt đầu cuộc trò chuyện mới
        </button>
      </div>
    </div>

    <!-- Input area (LUÔN HIỆN, kể cả khi WAITING) -->
    <div v-if="chatStatus !== 'CLOSED' && !showTypeSelector && chatStatus !== 'CONNECTING'" class="staff-input-wrapper">
      <!-- Image preview -->
      <div v-if="imagePreview" class="image-preview-bar">
        <img :src="imagePreview" class="preview-thumb" alt="preview" />
        <button class="remove-preview-btn" @click="clearImagePreview" title="Bỏ ảnh">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="staff-input-area">
        <!-- Hidden file input -->
        <input
          type="file"
          ref="fileInput"
          accept="image/*"
          style="display:none"
          @change="handleFileSelect"
        />
        <button class="attach-btn" @click="fileInput.click()" title="Gửi ảnh" :disabled="isSending">
          <i class="fas fa-image"></i>
        </button>
        <input
          v-model="currentMessage"
          @keyup.enter="sendStaffMessage"
          @paste="handlePaste"
          type="text"
          placeholder="Nhập tin nhắn hoặc dán ảnh..."
          class="staff-input"
          :disabled="isSending"
          ref="staffInput"
        />
        <button
          @click="sendStaffMessage"
          :disabled="(!currentMessage.trim() && !imagePreview) || isSending"
          class="staff-send-btn"
        >
          <i v-if="!isSending" class="fas fa-paper-plane"></i>
          <i v-else class="fas fa-spinner fa-spin"></i>
        </button>
      </div>
    </div>
  </div>

  <!-- Full-size image modal -->
  <Teleport to="body">
    <div v-if="previewModalSrc" class="image-modal-overlay" @click="closeImageModal">
      <div class="image-modal-content" @click.stop>
        <button class="image-modal-close" @click="closeImageModal">
          <i class="fas fa-times"></i>
        </button>
        <img :src="previewModalSrc" alt="Ảnh phóng to" />
      </div>
    </div>
  </Teleport>

  <!-- Orders Modal (Hoàn hàng - Giao diện y hệt OrdersPage.vue) -->
  <Teleport to="body">
    <div v-if="showOrdersModal" class="orders-page-modal-overlay" @click.self="closeOrdersModal">
      <div class="orders-page-modal-container">
        <!-- Modal Header -->
        <div class="orders-page-modal-header">
          <h2 class="page-title" style="margin: 0; font-size: 24px; color: #2c3e50;">ĐƠN HÀNG CỦA TÔI</h2>
          <button class="orders-page-modal-close" @click="closeOrdersModal">✕</button>
        </div>

        <div class="orders-page-modal-body">
          <!-- ===== ORDER TABS ===== -->
          <OrderTabs 
            ref="orderTabsRef"
            :orders="orders"
            @change-tab="onTabChange"
          />

          <!-- ===== ORDER LIST ===== -->
          <OrderList
            :orders="filteredOrders"
            :loading="ordersLoading"
            :current-page="currentPage"
            :total-pages="totalPages"
            :show-review-actions="false"
            @view-detail="viewOrderDetail"
            @cancel-order="handleCancelOrder"
            @return-order="handleReturnOrder"
            @reorder="handleReorder"
            @change-page="goToPage"
          />
        </div>
      </div>
    </div>
  </Teleport>

  <!-- Order Detail Modal -->
  <Teleport to="body">
    <div v-if="showDetailModal" class="modal-overlay" style="z-index: 100002;">
      <div class="modal-container modal-lg">
        <div class="modal-header">
          <h3>Chi tiết đơn hàng #{{ selectedOrder?.orderCode || selectedOrder?.orderId }}</h3>
          <button class="close-btn" @click="closeDetailModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="order-info-grid">
            <div class="info-section">
              <h4>Thông tin khách hàng</h4>
              <p><strong>Họ tên:</strong> {{ selectedOrder?.customer?.customerName || 'Khách hàng' }}</p>
              <p><strong>SĐT:</strong> {{ selectedOrder?.customer?.phone || '---' }}</p>
              <p><strong>Email:</strong> {{ selectedOrder?.customer?.email || '---' }}</p>
            </div>
            <div class="info-section">
              <h4>Thông tin đơn hàng</h4>
              <p><strong>Ngày đặt:</strong> {{ formatDate(selectedOrder?.orderDate) }}</p>
              <p><strong>Trạng thái:</strong> {{ getStatusText(selectedOrder?.orderStatus) }}</p>
              <p><strong>Thanh toán:</strong> {{ selectedOrder?.paymentStatus || 'UNPAID' }}</p>
            </div>
          </div>

          <!-- ===== ĐỊA CHỈ GIAO HÀNG ===== -->
          <div class="address-section">
            <h4>📍 Địa chỉ giao hàng</h4>
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
                <span class="address-value highlight">{{ getOrderFullAddress(selectedOrder?.shippingAddress) }}</span>
              </div>
            </div>
          </div>

          <div class="order-items-section">
            <h4>Sản phẩm</h4>
            <table class="items-table">
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Đơn giá</th>
                  <th>Số lượng</th>
                  <th>Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in selectedOrder?.items" :key="item.itemId">
                  <td>
                    <div class="item-info">
                      <div class="item-image">
                        <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" :alt="item.productName" />
                        <i v-else class="fas fa-tshirt"></i>
                      </div>
                      <div>
                        <div class="item-name">{{ item.productName }}</div>
                        <div class="item-variant">{{ item.sizeName || '' }} {{ item.colorName || '' }}</div>
                      </div>
                    </div>
                  </td>
                  <td>{{ formatPrice(item.unitPrice) }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ formatPrice(item.finalAmount) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="3" class="text-right">Tạm tính:</td>
                  <td>{{ formatPrice(selectedOrder?.subtotalAmount) }}</td>
                </tr>
                <tr v-if="selectedOrder?.discountAmount && selectedOrder?.discountAmount > 0" class="discount-row">
                  <td colspan="3" class="text-right">
                    <span class="voucher-label">
                      <i class="fas fa-ticket-alt"></i> Giảm giá 
                      ({{ selectedOrder?.voucher?.voucherCode || 'Voucher' }})
                    </span>
                  </td>
                  <td class="discount-amount">-{{ formatPrice(selectedOrder?.discountAmount) }}</td>
                </tr>
                <tr>
                  <td colspan="3" class="text-right">Phí vận chuyển:</td>
                  <td>{{ formatPrice(selectedOrder?.shippingFee || 0) }}</td>
                </tr>
                <tr class="total-row">
                  <td colspan="3" class="text-right">Tổng cộng:</td>
                  <td class="grand-total">{{ formatPrice(selectedOrder?.totalAmount) }}</td>
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
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeDetailModal">Đóng</button>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- Cancel Reason Modal -->
  <Teleport to="body">
    <div v-if="showCancelModal" class="modal-overlay" style="z-index: 100002;">
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
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeCancelModal">Đóng</button>
          <button class="btn btn-danger" @click="confirmCancelOrder" :disabled="cancelLoading">
            {{ cancelLoading ? 'Đang xử lý...' : 'Xác nhận hủy' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- Return Request Modal -->
  <Teleport to="body">
    <ReturnRequestModal
      :visible="showReturnModal"
      :order="selectedOrderForReturn"
      @close="closeReturnModal"
      @submitted="onReturnSubmitted"
    />
  </Teleport>

  <!-- Toast -->
  <Teleport to="body">
    <div :class="['toast', { show: showToast, 'toast-error': toastType === 'error' }]" style="z-index: 100005;">
      {{ toastMessage }}
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import chatService from '../../services/chatService'
import { orderService } from '../../services/orderService'
import { useWebSocket } from '../../composables/useWebSocket'
import ChatTypeSelector from './ChatTypeSelector.vue'
import OrderTabs from '../client/OrderTabs.vue'
import OrderList from '../client/OrderList.vue'
import ReturnRequestModal from '../client/ReturnRequestModal.vue'
import { useCartStore } from '../../stores/cartStore'

const router = useRouter()
const emit = defineEmits(['status-change'])

// ===== STATE =====
const chatStatus = ref('IDLE')
const currentChatId = ref(null)
const assignedEmployee = ref('')
const messages = ref([])
const currentMessage = ref('')
const isConnecting = ref(false)
const isSending = ref(false)
const staffTyping = ref(false)
const userId = ref(null)
const isDragging = ref(false)
const imagePreview = ref(null)
const previewModalSrc = ref(null)

// ✅ Hiển thị selector loại chat
const showTypeSelector = ref(true)

// ===== REFS =====
const staffMessagesContainer = ref(null)
const staffInput = ref(null)
const fileInput = ref(null)

// ===== WEBSOCKET SINGLETON cho Customer =====
// Dùng singleton riêng ở module level, KHÔNG tạo mới trong hàm
// để tránh nhiều kết nối WS song song
const _customerWs = useWebSocket()
let wsCleanups = []

// ===== ORDERS PAGE MODAL STATE & LOGIC (EXACTLY AS OrdersPage.vue) =====
const cartStore = useCartStore()
const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");
const ordersLoading = ref(false)
const cancelLoading = ref(false)
const orders = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const orderTabsRef = ref(null)

const activeMainTab = ref('all')
const activeSubTab = ref('all')

const showOrdersModal = ref(false)
const showDetailModal = ref(false)
const showCancelModal = ref(false)
const showReturnModal = ref(false)
const selectedOrder = ref(null)
const selectedOrderForReturn = ref(null)
const cancelOrderId = ref(null)
const cancelReason = ref('')

const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
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

const getStatusText = (status) => {
  const map = {
    'PENDING_PAYMENT': 'Chờ thanh toán',
    'CONFIRMED': 'Đã xác nhận',
    'PROCESSING': 'Đang xử lý',
    'SHIPPING': 'Đang giao hàng',
    'DELIVERED': 'Đã giao hàng',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy',
    'RETURN_REQUESTED': 'Yêu cầu hoàn hàng',
    'RETURN_APPROVED': 'Đã duyệt hoàn hàng',
    'RETURN_RECEIVED': 'Đã nhận hàng trả',
    'RETURN_PENDING': 'Đang thu hồi hàng',
    'RETURNED': 'Đã hoàn trả',
    'REJECTED': 'Từ chối hoàn trả'
  }
  return map[status] || status
}

const getOrderFullAddress = (shippingAddress) => {
  if (!shippingAddress) return '---'
  if (shippingAddress.addressLine) {
    return shippingAddress.addressLine
  }
  const parts = []
  if (shippingAddress.addressLine) parts.push(shippingAddress.addressLine)
  if (shippingAddress.ward) parts.push(shippingAddress.ward)
  if (shippingAddress.city) parts.push(shippingAddress.city)
  return parts.length > 0 ? parts.join(', ') : '---'
}

const showNotificationMsg = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 3000)
}

const filteredOrders = computed(() => {
  let result = [...orders.value]
  if (activeMainTab.value === 'orders') {
    result = result.filter(o => 
      !['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'REJECTED', 'CANCELLED'].includes(o.orderStatus)
    )
  } else if (activeMainTab.value === 'returns') {
    result = result.filter(o => 
      ['RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'REJECTED'].includes(o.orderStatus)
    )
  } else if (activeMainTab.value === 'cancelled') {
    result = result.filter(o => o.orderStatus === 'CANCELLED')
  }
  if (activeSubTab.value !== 'all' && activeMainTab.value !== 'all') {
    result = result.filter(o => o.orderStatus === activeSubTab.value)
  }
  return result
})

const onTabChange = ({ mainTab, subTab }) => {
  activeMainTab.value = mainTab
  activeSubTab.value = subTab
  currentPage.value = 0
  fetchOrders()
}

const fetchOrders = async () => {
  ordersLoading.value = true
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      showNotificationMsg('Vui lòng đăng nhập để xem đơn hàng', 'error')
      return
    }

    let statusFilter = null
    if (activeMainTab.value === 'orders') {
      if (activeSubTab.value === 'all') {
        statusFilter = 'PENDING_PAYMENT,CONFIRMED,PROCESSING,SHIPPING,DELIVERED,COMPLETED'
      } else {
        statusFilter = activeSubTab.value
      }
    } else if (activeMainTab.value === 'returns') {
      if (activeSubTab.value === 'all') {
        statusFilter = 'RETURN_REQUESTED,RETURN_APPROVED,RETURN_RECEIVED,RETURN_PENDING,RETURNED,REJECTED'
      } else {
        statusFilter = activeSubTab.value
      }
    } else if (activeMainTab.value === 'cancelled') {
      statusFilter = 'CANCELLED'
    }

    const response = await orderService.getMyOrders(statusFilter, currentPage.value, 10)
    if (response.data) {
      orders.value = response.data.content || []
      totalPages.value = response.data.totalPages || 0
      currentPage.value = response.data.number || 0
    }
  } catch (error) {
    console.error('Error fetching orders:', error)
    showNotificationMsg(error.response?.data?.message || 'Không thể tải đơn hàng', 'error')
    orders.value = []
  } finally {
    ordersLoading.value = false
  }
}

const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    fetchOrders()
  }
}

const viewOrderDetail = async (orderId) => {
  try {
    const response = await orderService.getOrderDetail(orderId)
    selectedOrder.value = response.data
    showDetailModal.value = true
  } catch (error) {
    console.error('Error fetching order detail:', error)
    showNotificationMsg('Không thể tải chi tiết đơn hàng', 'error')
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
}

const handleCancelOrder = (orderId) => {
  cancelOrderId.value = orderId
  cancelReason.value = ''
  showCancelModal.value = true
}

const closeCancelModal = () => {
  showCancelModal.value = false
  cancelOrderId.value = null
  cancelReason.value = ''
}

const confirmCancelOrder = async () => {
  if (!cancelReason.value.trim()) {
    showNotificationMsg('Vui lòng nhập lý do hủy đơn hàng', 'error')
    return
  }
  cancelLoading.value = true
  try {
    await orderService.cancelOrder(cancelOrderId.value, cancelReason.value)
    showNotificationMsg('Hủy đơn hàng thành công!', 'success')
    closeCancelModal()
    await fetchOrders()
  } catch (error) {
    console.error('Error canceling order:', error)
    showNotificationMsg(error.response?.data?.message || 'Không thể hủy đơn hàng', 'error')
  } finally {
    cancelLoading.value = false
  }
}

const handleReturnOrder = (order) => {
  if (!order) {
    showNotificationMsg('Không tìm thấy đơn hàng', 'error')
    return
  }
  if (order.orderStatus !== 'COMPLETED') {
    showNotificationMsg('Chỉ có thể yêu cầu hoàn hàng khi đơn hàng đã hoàn thành', 'warning')
    return
  }
  selectedOrderForReturn.value = order
  showReturnModal.value = true
}

const closeReturnModal = () => {
  showReturnModal.value = false
  selectedOrderForReturn.value = null
}

const onReturnSubmitted = (data) => {
  showNotificationMsg('Yêu cầu hoàn hàng đã được gửi!', 'success')
  closeReturnModal()
  fetchOrders()
}

const handleReorder = (order) => {
  if (order.items && order.items.length > 0) {
    order.items.forEach(item => {
      cartStore.addToCart({
        productDetailId: item.productDetailId,
        productId: item.productId,
        productName: item.productName,
        price: item.unitPrice,
        quantity: item.quantity,
        imageUrl: item.imageUrl,
        sizeName: item.sizeName,
        colorName: item.colorName
      })
    })
    showNotificationMsg('Đã thêm sản phẩm vào giỏ hàng!', 'success')
  }
}

const goToMyOrders = () => {
  showOrdersModal.value = true
  fetchOrders()
}

const closeOrdersModal = () => {
  showOrdersModal.value = false
}

// ===== WEBSOCKET =====
let wsHandle = null

// ===== CHAT TYPE SELECTOR HANDLERS =====
const onChatStarted = (chat) => {
  console.log('[StaffChatPanel] Chat started event received:', chat)
  
  if (chat && chat.chatId) {
    // ✅ Ẩn selector
    showTypeSelector.value = false
    // ✅ Set trạng thái CONNECTING
    chatStatus.value = 'CONNECTING'
    emit('status-change', 'CONNECTING')
    
    currentChatId.value = chat.chatId
    
    // ✅ Sử dụng nextTick để đảm bảo UI cập nhật
    nextTick(() => {
      // ✅ Xác định trạng thái chat
      if (chat.status === 'ACTIVE' || chat.status === 'ASSIGNED') {
        chatStatus.value = 'ACTIVE'
        assignedEmployee.value = chat.staffName || chat.employeeName || 'Nhân viên CSKH'
        loadMessages()
      } else {
        chatStatus.value = 'WAITING'
      }
      
      setupWebSocket()
      emit('status-change', chatStatus.value)
      saveChatState()
      console.log('[StaffChatPanel] Chat started, status:', chatStatus.value)
    })
    
  } else {
    console.error('[StaffChatPanel] No chat data received')
    showTypeSelector.value = true
    chatStatus.value = 'IDLE'
    emit('status-change', 'IDLE')
  }
}

const onTypeSelectorClose = () => {
  showTypeSelector.value = false
  if (!currentChatId.value) {
    chatStatus.value = 'IDLE'
    emit('status-change', 'IDLE')
  }
}

// ===== GET CURRENT USER ID =====
const getCurrentUserId = () => {
  try {
    const userObj = JSON.parse(localStorage.getItem('user') || 'null')
    let id = userObj?.userId ?? userObj?.id ?? userObj?.user_id ?? null
    if (!id) id = localStorage.getItem('userId') || localStorage.getItem('user_id')
    return id ? Number(id) : null
  } catch (e) { return null }
}

// ===== AUTO CONNECT =====
const autoConnect = async () => {
  const id = getCurrentUserId()
  if (!id) {
    console.warn('[StaffChat] No user ID found')
    return
  }
  userId.value = id

  const restored = await restoreChatState()
  if (restored) {
    showTypeSelector.value = false
    return
  }

  showTypeSelector.value = true
}

// ===== LOAD MESSAGES =====
const loadMessages = async () => {
  if (!currentChatId.value) return
  try {
    const msgs = await chatService.getMessages(currentChatId.value)
    messages.value = msgs || []
    await nextTick()
    scrollToBottom()
  } catch (e) {
    console.warn('[StaffChat] Load messages error:', e)
  }
}

// ===== IMAGE HELPERS =====
const IMAGE_PREFIX = '[IMAGE]'
const RETURN_FORM_PREFIX = '[RETURN_FORM]'

const isImageMessage = (content) => {
  return content && content.startsWith(IMAGE_PREFIX)
}

const extractImageSrc = (content) => {
  return content ? content.slice(IMAGE_PREFIX.length) : ''
}

const isReturnFormMessage = (content) => {
  return content && content.startsWith(RETURN_FORM_PREFIX)
}

const extractReturnFormText = (content) => {
  if (!content) return ''
  return content.slice(RETURN_FORM_PREFIX.length).trim()
}


const formatOrderDate = (dateStr) => {
  if (!dateStr) return '---'
  const d = new Date(dateStr)
  return d.toLocaleDateString('vi-VN')
}

const formatOrderPrice = (price) => {
  if (!price && price !== 0) return '0₫'
  return Number(price).toLocaleString('vi-VN') + '₫'
}

const openImagePreview = (src) => {
  previewModalSrc.value = src
}

const closeImageModal = () => {
  previewModalSrc.value = null
}

const clearImagePreview = () => {
  imagePreview.value = null
  if (fileInput.value) fileInput.value.value = ''
}

const processImageFile = (file) => {
  return new Promise((resolve, reject) => {
    if (!file || !file.type.startsWith('image/')) {
      reject(new Error('Not an image file'))
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const maxSize = 800
        let { width, height } = img
        if (width > maxSize || height > maxSize) {
          if (width > height) {
            height = Math.round((height * maxSize) / width)
            width = maxSize
          } else {
            width = Math.round((width * maxSize) / height)
            height = maxSize
          }
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)
        const base64 = canvas.toDataURL('image/jpeg', 0.75)
        resolve(base64)
      }
      img.onerror = reject
      img.src = e.target.result
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const handleFileSelect = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const base64 = await processImageFile(file)
    imagePreview.value = base64
  } catch (err) {
    alert('Không thể đọc file ảnh!')
  }
}

const handlePaste = async (e) => {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (file) {
        try {
          const base64 = await processImageFile(file)
          imagePreview.value = base64
        } catch (err) {
          alert('Không thể đọc ảnh từ clipboard!')
        }
      }
      break
    }
  }
}

const handleDrop = async (e) => {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (!file || !file.type.startsWith('image/')) return
  try {
    const base64 = await processImageFile(file)
    imagePreview.value = base64
  } catch (err) {
    alert('Không thể đọc ảnh!')
  }
}

// ===== SEND MESSAGE =====
const sendStaffMessage = async () => {
  if (imagePreview.value) {
    await sendImageMessage(imagePreview.value)
    clearImagePreview()
    if (currentMessage.value.trim()) {
      await sendTextMessage(currentMessage.value.trim())
      currentMessage.value = ''
    }
    return
  }

  if (!currentMessage.value.trim()) return
  await sendTextMessage(currentMessage.value.trim())
  currentMessage.value = ''
}

const ensureChatReady = async () => {
  const id = getCurrentUserId()
  if (!id) {
    alert('Bạn cần đăng nhập để chat!')
    return false
  }
  if (!currentChatId.value) {
    showTypeSelector.value = true
    return false
  }
  return true
}

const sendTextMessage = async (msgText) => {
  const ready = await ensureChatReady()
  if (!ready) return

  const id = getCurrentUserId()
  isSending.value = true

  const tempMsg = {
    id: Date.now(),
    messageContent: msgText,
    userId: id,
    sentDate: new Date().toISOString(),
    userRole: 'CUSTOMER',
    isAi: false
  }
  messages.value.push(tempMsg)
  await nextTick()
  scrollToBottom()

  try {
    await chatService.sendMessage(currentChatId.value, id, msgText)
    const msgs = await chatService.getMessages(currentChatId.value)
    messages.value = msgs || []
  } catch (e) {
    console.error('[StaffChat] Send text error:', e)
    messages.value = messages.value.filter(m => m.id !== tempMsg.id)
    alert('Không thể gửi tin nhắn. Vui lòng thử lại!')
  } finally {
    isSending.value = false
    await nextTick()
    scrollToBottom()
  }
}

const sendImageMessage = async (base64) => {
  const ready = await ensureChatReady()
  if (!ready) return

  const id = getCurrentUserId()
  isSending.value = true
  const content = IMAGE_PREFIX + base64

  const tempMsg = {
    id: Date.now(),
    messageContent: content,
    userId: id,
    sentDate: new Date().toISOString(),
    userRole: 'CUSTOMER',
    isAi: false
  }
  messages.value.push(tempMsg)
  await nextTick()
  scrollToBottom()

  try {
    await chatService.sendMessage(currentChatId.value, id, content)
    const msgs = await chatService.getMessages(currentChatId.value)
    messages.value = msgs || []
  } catch (e) {
    console.error('[StaffChat] Send image error:', e)
    messages.value = messages.value.filter(m => m.id !== tempMsg.id)
    alert('Không thể gửi ảnh. Vui lòng thử lại!')
  } finally {
    isSending.value = false
    await nextTick()
    scrollToBottom()
  }
}

// ===== WEBSOCKET =====
const setupWebSocket = () => {
  try {
    // ✅ Dùng singleton, không tạo mới
    _customerWs.connect('/ws/chat')
    
    const id = getCurrentUserId()
    if (id) {
      // ✅ Identify là CUSTOMER
      _customerWs.identify('CUSTOMER', id)
    }

    const newMsgCleanup = _customerWs.onMessage('new_message', (data) => {
      const msgData = data.data || data
      if (msgData.chatId === currentChatId.value) {
        const msg = msgData.message
        if (msg && msg.userId !== getCurrentUserId()) {
          if (chatStatus.value === 'WAITING') {
            chatStatus.value = 'ACTIVE'
            assignedEmployee.value = msg.userName || 'Nhân viên CSKH'
            emit('status-change', 'ACTIVE')
            saveChatState()
          }
          messages.value.push(msg)
          nextTick(() => scrollToBottom())
        }
      }
    })

    const assignedCleanup = _customerWs.onMessage('chat_assigned', (data) => {
      const assignData = data.data || data
      if (assignData.chatId === currentChatId.value) {
        chatStatus.value = 'ACTIVE'
        assignedEmployee.value = assignData.staffName || assignData.employeeName || 'Nhân viên CSKH'
        emit('status-change', 'ACTIVE')
        saveChatState()
        loadMessages()
      }
    })

    const waitingCleanup = _customerWs.onMessage('chat_waiting', (data) => {
      const waitData = data.data || data
      if (waitData.chatId === currentChatId.value) {
        chatStatus.value = 'WAITING'
        emit('status-change', 'WAITING')
        saveChatState()
      }
    })

    const closedCleanup = _customerWs.onMessage('chat_closed', (data) => {
      const closeData = data.data || data
      if (closeData.chatId === currentChatId.value) {
        chatStatus.value = 'CLOSED'
        emit('status-change', 'CLOSED')
        clearChatState()
      }
    })

    // ✅ Cleanup listeners cũ trước khi thêm mới (tránh duplicate)
    wsCleanups.forEach(c => typeof c === 'function' && c())
    wsCleanups = [newMsgCleanup, assignedCleanup, waitingCleanup, closedCleanup]
    console.log('[StaffChat] WebSocket setup done, listeners registered')
    
  } catch (e) {
    console.warn('[StaffChat] WS setup error:', e)
  }
}

const disconnectWebSocket = () => {
  // ✅ Chỉ cleanup listeners, KHÔNG disconnect toàn bộ WS
  // (WS singleton được tái sử dụng nên không nên đóng ở đây)
  wsCleanups.forEach(cleanup => {
    if (typeof cleanup === 'function') cleanup()
  })
  wsCleanups = []
  console.log('[StaffChat] WebSocket listeners cleaned up')
}

// ===== CHAT ACTIONS =====
const cancelWaiting = async () => {
  if (!confirm('Bạn có muốn hủy yêu cầu hỗ trợ không?')) return
  resetChat()
}

const closeStaffChat = async () => {
  if (!currentChatId.value) return
  if (!confirm('Bạn có muốn kết thúc cuộc trò chuyện này không?')) return
  chatStatus.value = 'CLOSED'
  disconnectWebSocket()
  emit('status-change', 'CLOSED')
  clearChatState()
}

const resetChat = () => {
  chatStatus.value = 'IDLE'
  currentChatId.value = null
  assignedEmployee.value = ''
  messages.value = []
  currentMessage.value = ''
  disconnectWebSocket()
  clearChatState()
  emit('status-change', 'IDLE')
  showTypeSelector.value = true
}

// ===== STORAGE =====
const STORAGE_KEY = 'staff-chat-state'

const saveChatState = () => {
  if (!currentChatId.value) return
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    chatId: currentChatId.value,
    status: chatStatus.value,
    employeeName: assignedEmployee.value
  }))
}

const clearChatState = () => {
  localStorage.removeItem(STORAGE_KEY)
}

const restoreChatState = async () => {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
    if (!saved || !saved.chatId) return false

    const id = getCurrentUserId()
    if (!id) return false

    const chats = await chatService.getConversations(id)
    const found = chats.find(c => c.chatId === saved.chatId)
    if (!found) { clearChatState(); return false }

    currentChatId.value = saved.chatId
    
    if (found.status === 'ACTIVE' || found.status === 'ASSIGNED') {
      chatStatus.value = 'ACTIVE'
      assignedEmployee.value = found.staffName || found.employeeName || 'Nhân viên CSKH'
      await loadMessages()
    } else if (found.status === 'WAITING') {
      chatStatus.value = 'WAITING'
    } else {
      clearChatState()
      return false
    }
    
    setupWebSocket()
    emit('status-change', chatStatus.value)
    return true
  } catch (e) {
    clearChatState()
    return false
  }
}

// ===== HELPERS =====
const isMyMessage = (msg) => {
  const id = getCurrentUserId()
  return msg.userId === id
}

const scrollToBottom = () => {
  if (staffMessagesContainer.value) {
    staffMessagesContainer.value.scrollTop = staffMessagesContainer.value.scrollHeight
  }
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  try {
    return new Date(dateStr).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  } catch (e) { return '' }
}

// ===== WATCH =====
watch(messages, () => {
  nextTick(() => scrollToBottom())
}, { deep: true })

// ===== LIFECYCLE =====
onMounted(async () => {
  console.log('[StaffChatPanel] Mounted')
  userId.value = getCurrentUserId()
  await autoConnect()
})

onBeforeUnmount(() => {
  disconnectWebSocket()
})

defineExpose({ 
  connectToStaff: autoConnect,
  resetChat
})
</script>


<style scoped>
.staff-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  font-family: 'Segoe UI', sans-serif;
}

.staff-waiting-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  gap: 16px;
  overflow-y: auto;
}

.waiting-animation {
  text-align: center;
  margin-top: 10px;
}

.waiting-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.waiting-dots span {
  width: 12px;
  height: 12px;
  background: #667eea;
  border-radius: 50%;
  animation: waiting-bounce 1.4s infinite ease-in-out both;
}
.waiting-dots span:nth-child(1) { animation-delay: -0.32s; }
.waiting-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes waiting-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.waiting-text {
  font-size: 15px;
  color: #2d3748;
  font-weight: 600;
  margin: 0 0 4px;
}

.waiting-sub {
  font-size: 13px;
  color: #718096;
  margin: 0;
}

.empty-chat-message {
  text-align: center;
  padding: 30px 20px;
  color: #a0aec0;
}

.empty-chat-message i {
  font-size: 40px;
  margin-bottom: 12px;
  display: block;
}

.empty-chat-message p {
  font-size: 14px;
  margin: 0;
}

.waiting-messages {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.cancel-btn {
  padding: 8px 20px;
  background: white;
  color: #e53e3e;
  border: 1px solid #e53e3e;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}
.cancel-btn:hover { background: #fff5f5; }

.staff-chat-active {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.staff-chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  flex-shrink: 0;
}

.staff-avatar {
  position: relative;
  width: 36px;
  height: 36px;
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.online-dot {
  position: absolute;
  bottom: 1px;
  right: 1px;
  width: 9px;
  height: 9px;
  background: #48bb78;
  border: 2px solid white;
  border-radius: 50%;
}

.staff-info { flex: 1; }
.staff-info strong { display: block; font-size: 14px; }
.staff-info .status-text { font-size: 11px; opacity: 0.85; }

.end-chat-btn {
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.3);
  color: white;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: background 0.2s;
}
.end-chat-btn:hover { background: rgba(229, 62, 62, 0.5); }

.staff-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.staff-messages::-webkit-scrollbar { width: 4px; }
.staff-messages::-webkit-scrollbar-thumb { background: #667eea; border-radius: 2px; }

.message {
  display: flex;
  flex-direction: column;
  max-width: 80%;
}
.message.user { align-self: flex-end; }
.message.staff { align-self: flex-start; }

.message-content { display: flex; flex-direction: column; gap: 2px; }
.message.user .message-content { align-items: flex-end; }
.message.staff .message-content { align-items: flex-start; }

.message-bubble {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}
.message-bubble p { margin: 0; }
.message-time { display: block; font-size: 10px; margin-top: 3px; opacity: 0.6; }

.message.user .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.staff .message-bubble {
  background: #f1f3f5;
  color: #2d3748;
  border-bottom-left-radius: 4px;
}

.sender-name {
  font-size: 10px;
  color: #a0aec0;
  margin-top: 2px;
  padding-left: 4px;
}

.typing-indicator-wrapper { align-self: flex-start; max-width: 80px; }
.typing-bubble {
  padding: 10px 16px;
  background: #f1f3f5;
  border-radius: 16px;
  border-bottom-left-radius: 4px;
}
.typing-dots {
  display: flex;
  gap: 4px;
}
.typing-dots span {
  width: 6px;
  height: 6px;
  background: #a0aec0;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}
.typing-dots span:nth-child(1) { animation-delay: -0.32s; }
.typing-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.staff-input-wrapper {
  flex-shrink: 0;
  background: white;
  border-top: 1px solid #eee;
}

.image-preview-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: #f7f8fc;
  border-bottom: 1px solid #e2e8f0;
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-6px); }
  to   { opacity: 1; transform: translateY(0); }
}

.preview-thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid #667eea;
}

.remove-preview-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  background: #e53e3e;
  color: white;
  font-size: 11px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}
.remove-preview-btn:hover { background: #c53030; }

.staff-input-area {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  align-items: center;
}

.attach-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid #e2e8f0;
  background: #f7f8fc;
  color: #667eea;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}
.attach-btn:hover:not(:disabled) { background: #667eea; color: white; border-color: #667eea; }
.attach-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.staff-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}
.staff-input:focus { border-color: #667eea; }

.staff-send-btn {
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s;
  flex-shrink: 0;
}
.staff-send-btn:hover:not(:disabled) { transform: scale(1.08); }
.staff-send-btn:disabled { background: #cbd5e0; cursor: not-allowed; }

/* Chat image in bubble */
.chat-image {
  max-width: 200px;
  max-height: 200px;
  object-fit: cover;
  border-radius: 10px;
  cursor: pointer;
  display: block;
  transition: opacity 0.2s;
}
.chat-image:hover { opacity: 0.88; }

/* Drag-and-drop overlay */
.staff-messages {
  position: relative;
}
.drag-over {
  outline: 2px dashed #667eea;
  outline-offset: -4px;
}
.drag-overlay {
  position: absolute;
  inset: 0;
  background: rgba(102, 126, 234, 0.12);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #667eea;
  font-size: 14px;
  font-weight: 600;
  pointer-events: none;
  border-radius: 8px;
}
.drag-overlay i { font-size: 36px; }

/* Full-size image modal */
.image-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.8);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeInModal 0.2s ease;
}
@keyframes fadeInModal {
  from { opacity: 0; }
  to   { opacity: 1; }
}
.image-modal-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}
.image-modal-content img {
  max-width: 100%;
  max-height: 90vh;
  border-radius: 10px;
  object-fit: contain;
  display: block;
}
.image-modal-close {
  position: absolute;
  top: -14px;
  right: -14px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e53e3e;
  color: white;
  border: none;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  transition: background 0.2s;
}
.image-modal-close:hover { background: #c53030; }

.staff-closed-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.closed-content {
  text-align: center;
  padding: 20px;
}

.closed-content i {
  font-size: 48px;
  color: #48bb78;
  margin-bottom: 12px;
  display: block;
}

.closed-content h4 {
  color: #2d3748;
  margin: 0 0 8px;
  font-size: 16px;
}

.closed-content p {
  color: #718096;
  font-size: 13px;
  margin: 0 0 16px;
}

.new-chat-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 auto;
  transition: all 0.2s;
}
.new-chat-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(102,126,234,0.4); }

.chat-type-selector-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: #f8f9fa;
  overflow-y: auto;
}


.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #667eea;
  background: #f8f9fa;
}

.loading-state i {
  font-size: 32px;
}

.loading-state p {
  margin: 0;
}

/* ==================== RETURN FORM CARD STYLES ==================== */
.return-form-card {
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
  max-width: 280px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  margin: 4px 0;
}

.return-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #dd6b20;
  font-size: 14px;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #feebc8;
}

.return-card-header i {
  font-size: 16px;
}

.return-card-desc {
  font-size: 12.5px;
  color: #4a5568;
  margin: 0 0 12px 0;
  line-height: 1.45;
}

.go-to-orders-btn {
  width: 100%;
  padding: 9px 14px;
  background: linear-gradient(135deg, #dd6b20 0%, #c05621 100%);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(221, 107, 32, 0.25);
}

.go-to-orders-btn:hover {
  background: linear-gradient(135deg, #c05621 0%, #9c4221 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(221, 107, 32, 0.35);
  font-size: 14px;
  color: #718096;
}

/* ✅ Style cho chat-type-selector-wrapper */
.chat-type-selector-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: #f8f9fa;
  overflow-y: auto;
  min-height: 300px;
}

/* ==================== ORDERS PAGE MODAL ==================== */
.orders-page-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(5px);
  z-index: 100000;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.orders-page-modal-container {
  background: #f8f9fa;
  border-radius: 16px;
  width: 100%;
  max-width: 1200px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  animation: ordersModalSlideIn 0.3s ease;
}

@keyframes ordersModalSlideIn {
  from { opacity: 0; transform: translateY(-20px) scale(0.98); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.orders-page-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 30px;
  background: white;
  border-bottom: 1px solid #e9ecef;
  flex-shrink: 0;
}

.orders-page-modal-close {
  background: #f1f3f5;
  border: none;
  font-size: 20px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  color: #495057;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.orders-page-modal-close:hover {
  background: #e9ecef;
  color: #212529;
}

.orders-page-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 30px 40px;
}

/* ===== MODALS & DIALOGS (EXACTLY AS OrdersPage.vue) ===== */
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
  z-index: 100001;
  backdrop-filter: blur(5px);
}

.modal-container {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideIn 0.3s;
}

.modal-container.modal-sm {
  max-width: 450px;
}

.modal-container.modal-lg {
  max-width: 800px;
}

@keyframes slideIn {
  from { transform: translateY(-30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border-radius: 12px 12px 0 0;
  position: sticky;
  top: 0;
  z-index: 1;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: white;
}

.close-btn:hover {
  opacity: 0.8;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-radius: 0 0 12px 12px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-outline {
  background: #f0f0f0;
  color: #666;
}

.btn-outline:hover {
  background: #e0e0e0;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

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

.info-section h4 {
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

.address-section {
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #e9ecef;
}

.address-section h4 {
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

.order-items-section h4 {
  color: #2c3e50;
  margin-bottom: 15px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
}

.items-table th {
  background: #f8f9fa;
  padding: 10px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
}

.items-table td {
  padding: 10px;
  border-bottom: 1px solid #eee;
  vertical-align: middle;
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

.discount-row .discount-amount {
  color: #28a745;
  font-weight: 700;
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

.item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-info .item-image {
  width: 40px;
  height: 40px;
}

.item-info .item-image i {
  font-size: 20px;
}

.item-info .item-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.item-info .item-variant {
  font-size: 12px;
  color: #999;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 5px;
}

.required {
  color: #e63946;
}

.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}

.form-group textarea:focus {
  outline: none;
  border-color: #e63946;
}

.toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  padding: 12px 24px;
  border-radius: 8px;
  background: #27ae60;
  color: white;
  font-size: 14px;
  z-index: 100005;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}
</style>
