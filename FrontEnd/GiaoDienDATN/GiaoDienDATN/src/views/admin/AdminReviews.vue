<template>
  <div class="admin-reviews">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>📝 Quản lý đánh giá</h2>
        <span class="total-badge">Tổng: {{ stats.totalReviews || 0 }}</span>
      </div>
      <div class="header-actions">
        <div class="filter-group">
          <select v-model="filterStatus" class="filter-select" @change="fetchReviews">
            <option value="">Tất cả trạng thái</option>
            <option value="PENDING">⏳ Chờ duyệt</option>
            <option value="APPROVED">✅ Đã duyệt</option>
            <option value="REJECTED">❌ Từ chối</option>
          </select>
          <div class="search-box">
            <input 
              type="text" 
              v-model="keyword" 
              placeholder="Tìm kiếm đánh giá..."
              @keyup.enter="fetchReviews"
            />
            <button class="btn btn-primary btn-sm" @click="fetchReviews">
              <i class="fas fa-search"></i>
            </button>
          </div>
          <button class="btn btn-outline btn-sm" @click="resetFilters">
            <i class="fas fa-undo"></i> Đặt lại
          </button>
        </div>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="stats-grid">
      <div class="stat-card" :class="{ active: filterStatus === '' }" @click="filterStatus = ''; fetchReviews()">
        <div class="stat-icon total"><i class="fas fa-star"></i></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalReviews || 0 }}</span>
          <span class="stat-label">Tổng đánh giá</span>
        </div>
      </div>
      <div class="stat-card pending" :class="{ active: filterStatus === 'PENDING' }" @click="filterStatus = 'PENDING'; fetchReviews()">
        <div class="stat-icon pending"><i class="fas fa-clock"></i></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.pendingReviews || 0 }}</span>
          <span class="stat-label">⏳ Chờ duyệt</span>
        </div>
      </div>
      <div class="stat-card approved" :class="{ active: filterStatus === 'APPROVED' }" @click="filterStatus = 'APPROVED'; fetchReviews()">
        <div class="stat-icon approved"><i class="fas fa-check-circle"></i></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.approvedReviews || 0 }}</span>
          <span class="stat-label">✅ Đã duyệt</span>
        </div>
      </div>
      <div class="stat-card rejected" :class="{ active: filterStatus === 'REJECTED' }" @click="filterStatus = 'REJECTED'; fetchReviews()">
        <div class="stat-icon rejected"><i class="fas fa-times-circle"></i></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.rejectedReviews || 0 }}</span>
          <span class="stat-label">❌ Từ chối</span>
        </div>
      </div>
      <div class="stat-card rating">
        <div class="stat-icon rating"><i class="fas fa-star-half-alt"></i></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.averageRating?.toFixed(1) || 0 }}</span>
          <span class="stat-label">⭐ Rating trung bình</span>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <i class="fas fa-spinner fa-spin"></i>
      <p>Đang tải đánh giá...</p>
    </div>

    <!-- Reviews Table -->
    <div v-else class="table-container">
      <div v-if="reviews.length === 0" class="empty-state">
        <i class="fas fa-comment-slash"></i>
        <h3>Không có đánh giá nào</h3>
        <p>Chưa có đánh giá nào phù hợp với bộ lọc hiện tại</p>
      </div>

      <table v-else class="reviews-table">
        <thead>
          <tr>
            <th style="width: 60px">ID</th>
            <th style="width: 200px">Sản phẩm</th>
            <th style="width: 180px">Khách hàng</th>
            <th style="width: 100px">Rating</th>
            <th style="min-width: 150px">Nội dung</th>
            <th style="width: 120px">Hình ảnh</th>
            <th style="width: 100px">Trạng thái</th>
            <th style="width: 140px">Ngày tạo</th>
            <th style="width: 140px">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="review in reviews" :key="review.reviewId">
            <td class="text-center">#{{ review.reviewId }}</td>
            <td>
              <div class="product-info">
                <div class="product-thumb">
                  <img v-if="review.productImageUrl" :src="getImageUrl(review.productImageUrl)" :alt="review.productName" />
                  <i v-else class="fas fa-tshirt"></i>
                </div>
                <div class="product-name">
                  <span>{{ review.productName }}</span>
                  <small>{{ review.colorName || '' }} {{ review.sizeName || '' }}</small>
                </div>
              </div>
            </td>
            <td>
              <div class="customer-info">
                <span class="customer-name">{{ review.customerName || 'Khách hàng' }}</span>
                <small class="customer-email">{{ review.customerEmail || '' }}</small>
              </div>
            </td>
            <td>
              <div class="rating-stars">
                <i v-for="star in 5" :key="star" 
                   :class="star <= review.rating ? 'fas fa-star active' : 'far fa-star'">
                </i>
                <span class="rating-number-small">{{ review.rating }}</span>
              </div>
            </td>
            <td>
              <div class="comment-cell">
                <div class="comment-preview" :title="review.comment">
                  {{ review.comment || 'Không có nhận xét' }}
                </div>
              </div>
            </td>
            <td>
              <div class="review-images" v-if="review.images && review.images.length > 0">
                <img 
                  v-for="(img, idx) in review.images.slice(0, 3)" 
                  :key="idx"
                  :src="getImageUrl(img)"
                  class="thumbnail-img"
                  @click="viewImage(img)"
                />
                <span v-if="review.images.length > 3" class="more-images">
                  +{{ review.images.length - 3 }}
                </span>
              </div>
              <span v-else class="no-image">—</span>
            </td>
            <td>
              <span :class="['status-badge', getStatusClass(review.reviewStatus)]">
                <i :class="getStatusIcon(review.reviewStatus)"></i>
                {{ getStatusText(review.reviewStatus) }}
              </span>
            </td>
            <td>
              <div class="date-info">
                <span>{{ formatDate(review.createdAt) }}</span>
              </div>
            </td>
            <td>
              <div class="action-buttons">
                <button 
                  v-if="review.reviewStatus === 'PENDING'"
                  class="btn btn-sm btn-success"
                  @click="approveReview(review.reviewId)"
                  title="Duyệt đánh giá"
                >
                  <i class="fas fa-check"></i>
                </button>
                <button 
                  v-if="review.reviewStatus === 'PENDING'"
                  class="btn btn-sm btn-danger"
                  @click="showRejectModal(review.reviewId)"
                  title="Từ chối đánh giá"
                >
                  <i class="fas fa-times"></i>
                </button>
                <button 
                  class="btn btn-sm btn-info"
                  @click="viewDetail(review)"
                  title="Xem chi tiết"
                >
                  <i class="fas fa-eye"></i>
                </button>
                <button 
                  class="btn btn-sm btn-warning"
                  @click="deleteReview(review.reviewId)"
                  title="Xóa đánh giá"
                >
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

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
    </div>

    <!-- Detail Modal -->
    <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetail">
      <div class="modal-container modal-lg">
        <div class="modal-header">
          <h3>Chi tiết đánh giá #{{ selectedReview?.reviewId }}</h3>
          <button class="close-btn" @click="closeDetail">✕</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-section">
              <h4>Thông tin sản phẩm</h4>
              <div class="detail-row">
                <label>Tên sản phẩm:</label>
                <span>{{ selectedReview?.productName }}</span>
              </div>
              <div class="detail-row">
                <label>Phân loại:</label>
                <span>{{ selectedReview?.colorName || '' }} {{ selectedReview?.sizeName || '' }}</span>
              </div>
              <div class="detail-row">
                <label>Mã sản phẩm:</label>
                <span>#{{ selectedReview?.productDetailId }}</span>
              </div>
            </div>

            <div class="detail-section">
              <h4>Thông tin khách hàng</h4>
              <div class="detail-row">
                <label>Họ tên:</label>
                <span>{{ selectedReview?.customerName || 'Khách hàng' }}</span>
              </div>
              <div class="detail-row">
                <label>Email:</label>
                <span>{{ selectedReview?.customerEmail || '---' }}</span>
              </div>
              <div class="detail-row">
                <label>Mã đơn hàng:</label>
                <span>#{{ selectedReview?.orderId || '---' }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4>Nội dung đánh giá</h4>
            <div class="detail-row">
              <label>Rating:</label>
              <div class="rating-stars">
                <i v-for="star in 5" :key="star" 
                   :class="star <= selectedReview?.rating ? 'fas fa-star active' : 'far fa-star'">
                </i>
              </div>
            </div>
            <div class="detail-row">
              <label>Nhận xét:</label>
              <p class="comment-full">{{ selectedReview?.comment || 'Không có nhận xét' }}</p>
            </div>
          </div>

          <div class="detail-section" v-if="selectedReview?.images && selectedReview.images.length > 0">
            <h4>Hình ảnh đánh giá</h4>
            <div class="image-gallery">
              <img 
                v-for="(img, idx) in selectedReview?.images" 
                :key="idx"
                :src="getImageUrl(img)"
                class="gallery-image"
                @click="viewImage(img)"
              />
            </div>
          </div>

          <div class="detail-section">
            <h4>Thông tin khác</h4>
            <div class="detail-row">
              <label>Trạng thái:</label>
              <span :class="['status-badge', getStatusClass(selectedReview?.reviewStatus)]">
                <i :class="getStatusIcon(selectedReview?.reviewStatus)"></i>
                {{ getStatusText(selectedReview?.reviewStatus) }}
              </span>
            </div>
            <div class="detail-row">
              <label>Ngày tạo:</label>
              <span>{{ formatDate(selectedReview?.createdAt) }}</span>
            </div>
            <div class="detail-row" v-if="selectedReview?.updatedAt">
              <label>Cập nhật:</label>
              <span>{{ formatDate(selectedReview?.updatedAt) }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeDetail">Đóng</button>
          <button 
            v-if="selectedReview?.reviewStatus === 'PENDING'"
            class="btn btn-success" 
            @click="approveReview(selectedReview.reviewId)"
          >
            <i class="fas fa-check"></i> Duyệt
          </button>
          <button 
            v-if="selectedReview?.reviewStatus === 'PENDING'"
            class="btn btn-danger" 
            @click="showRejectModal(selectedReview.reviewId)"
          >
            <i class="fas fa-times"></i> Từ chối
          </button>
        </div>
      </div>
    </div>

    <!-- Reject Modal -->
    <div v-if="showRejectModalVisible" class="modal-overlay" @click.self="closeRejectModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Từ chối đánh giá</h3>
          <button class="close-btn" @click="closeRejectModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Lý do từ chối</label>
            <textarea 
              v-model="rejectReason" 
              rows="4" 
              placeholder="Nhập lý do từ chối đánh giá..."
              class="form-control"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeRejectModal">Hủy</button>
          <button class="btn btn-danger" @click="confirmReject" :disabled="rejectLoading">
            <i v-if="rejectLoading" class="fas fa-spinner fa-spin"></i>
            {{ rejectLoading ? 'Đang xử lý...' : 'Xác nhận từ chối' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Image Viewer Modal -->
    <div v-if="showImageViewer" class="modal-overlay" @click.self="closeImageViewer">
      <div class="image-viewer-container">
        <img :src="viewingImage" alt="Review image" />
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
import api from "@/api/axios";

const IMAGE_BASE = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "");

// State
const loading = ref(false)
const reviews = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const filterStatus = ref('')
const keyword = ref('')
const stats = ref({})

// Modals
const showDetailModal = ref(false)
const showRejectModalVisible = ref(false)
const showImageViewer = ref(false)
const selectedReview = ref(null)
const rejectReviewId = ref(null)
const rejectReason = ref('')
const rejectLoading = ref(false)
const viewingImage = ref('')

// Toast
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null

// Helper functions
const getImageUrl = (imagePath) => {
  if (!imagePath) return ''
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath
  }
  const cleanPath = imagePath.startsWith('/') ? imagePath : '/' + imagePath
  return IMAGE_BASE + cleanPath
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
    'PENDING': 'Chờ duyệt',
    'APPROVED': 'Đã duyệt',
    'REJECTED': 'Từ chối'
  }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    'PENDING': 'status-pending',
    'APPROVED': 'status-approved',
    'REJECTED': 'status-rejected'
  }
  return map[status] || ''
}

const getStatusIcon = (status) => {
  const map = {
    'PENDING': 'fas fa-clock',
    'APPROVED': 'fas fa-check-circle',
    'REJECTED': 'fas fa-times-circle'
  }
  return map[status] || 'fas fa-circle'
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

// Fetch reviews
const fetchReviews = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await api.get("/admin/reviews", {
      params: {
        status: filterStatus.value || null,
        keyword: keyword.value || null,
        page: currentPage.value,
        size: 10
      } })

    if (response.data) {
      reviews.value = response.data.content || []
      totalPages.value = response.data.totalPages || 0
      currentPage.value = response.data.number || 0
    }
  } catch (error) {
    console.error('❌ Error fetching reviews:', error)
    showNotification('Không thể tải danh sách đánh giá', 'error')
  } finally {
    loading.value = false
  }
}

// Fetch statistics
const fetchStats = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await api.get(`/admin/reviews/statistics`, {})
    stats.value = response.data || {}
  } catch (error) {
    console.error('❌ Error fetching stats:', error)
  }
}

// Pagination
const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    fetchReviews()
  }
}

// Reset filters
const resetFilters = () => {
  filterStatus.value = ''
  keyword.value = ''
  currentPage.value = 0
  fetchReviews()
}

// Approve review
const approveReview = async (reviewId) => {
  if (!confirm('Bạn có chắc muốn duyệt đánh giá này?')) return

  try {
    const token = localStorage.getItem('token')
    await api.put(`/admin/reviews/${reviewId}/approve`, {}, {})
    showNotification('✅ Duyệt đánh giá thành công!', 'success')
    fetchReviews()
    fetchStats()
    if (showDetailModal.value) closeDetail()
  } catch (error) {
    console.error('❌ Error approving review:', error)
    showNotification(error.response?.data?.message || 'Không thể duyệt đánh giá', 'error')
  }
}

// Show reject modal
const showRejectModal = (reviewId) => {
  rejectReviewId.value = reviewId
  rejectReason.value = ''
  showRejectModalVisible.value = true
}

// Close reject modal
const closeRejectModal = () => {
  showRejectModalVisible.value = false
  rejectReviewId.value = null
  rejectReason.value = ''
}

// Confirm reject
const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    showNotification('Vui lòng nhập lý do từ chối', 'error')
    return
  }

  rejectLoading.value = true
  try {
    const token = localStorage.getItem('token')
    await api.put(`/admin/reviews/${rejectReviewId.value}/reject`, {
      reason: rejectReason.value
    }, {})
    showNotification('❌ Đã từ chối đánh giá', 'success')
    closeRejectModal()
    fetchReviews()
    fetchStats()
    if (showDetailModal.value) closeDetail()
  } catch (error) {
    console.error('❌ Error rejecting review:', error)
    showNotification(error.response?.data?.message || 'Không thể từ chối đánh giá', 'error')
  } finally {
    rejectLoading.value = false
  }
}

// View detail
const viewDetail = (review) => {
  selectedReview.value = review
  showDetailModal.value = true
}

// Close detail
const closeDetail = () => {
  showDetailModal.value = false
  selectedReview.value = null
}

// Delete review
const deleteReview = async (reviewId) => {
  if (!confirm('Bạn có chắc muốn xóa đánh giá này? Hành động này không thể hoàn tác!')) return

  try {
    const token = localStorage.getItem('token')
    await api.delete(`/admin/reviews/${reviewId}`, {})
    showNotification('🗑️ Xóa đánh giá thành công!', 'success')
    fetchReviews()
    fetchStats()
  } catch (error) {
    console.error('❌ Error deleting review:', error)
    showNotification(error.response?.data?.message || 'Không thể xóa đánh giá', 'error')
  }
}

// View image
const viewImage = (imageUrl) => {
  viewingImage.value = getImageUrl(imageUrl)
  showImageViewer.value = true
}

// Close image viewer
const closeImageViewer = () => {
  showImageViewer.value = false
  viewingImage.value = ''
}

// Lifecycle
onMounted(() => {
  fetchReviews()
  fetchStats()
})
</script>

<style scoped>
.admin-reviews {
  padding: 20px;
  background: #f8f9fa;
  min-height: 100vh;
}

/* ===== HEADER ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 25px;
  background: white;
  padding: 20px 25px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h2 {
  margin: 0;
  font-size: 24px;
  color: #1a1a2e;
}

.total-badge {
  background: #e63946;
  color: white;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-select {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  outline: none;
}

.filter-select:focus {
  border-color: #e63946;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 0;
}

.search-box input {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 8px 0 0 8px;
  font-size: 14px;
  outline: none;
  width: 200px;
}

.search-box input:focus {
  border-color: #e63946;
}

.search-box .btn {
  border-radius: 0 8px 8px 0;
  padding: 8px 14px;
}

/* ===== STATISTICS ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 15px;
  margin-bottom: 25px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.stat-card.active {
  border-color: #e63946;
  background: #fff5f5;
}

.stat-card .stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
}

.stat-card .stat-icon.total {
  background: #6c757d;
}
.stat-card .stat-icon.pending {
  background: #ffc107;
}
.stat-card .stat-icon.approved {
  background: #28a745;
}
.stat-card .stat-icon.rejected {
  background: #dc3545;
}
.stat-card .stat-icon.rating {
  background: #e63946;
}

.stat-card .stat-info {
  display: flex;
  flex-direction: column;
}

.stat-card .stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}

.stat-card .stat-label {
  font-size: 13px;
  color: #6c757d;
}

/* ===== TABLE ===== */
.table-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
}

.reviews-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.reviews-table thead th {
  background: #f8f9fa;
  padding: 12px 15px;
  text-align: left;
  font-weight: 600;
  color: #495057;
  border-bottom: 2px solid #e9ecef;
  white-space: nowrap;
}

.reviews-table tbody td {
  padding: 12px 15px;
  border-bottom: 1px solid #e9ecef;
  vertical-align: middle;
}

.reviews-table tbody tr:hover {
  background: #f8f9fa;
}

.text-center {
  text-align: center;
}

/* Product Info */
.product-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-thumb i {
  font-size: 20px;
  color: #ccc;
}

.product-name {
  display: flex;
  flex-direction: column;
}

.product-name span {
  font-weight: 500;
  color: #1a1a2e;
}

.product-name small {
  font-size: 12px;
  color: #999;
}

/* Customer Info */
.customer-info {
  display: flex;
  flex-direction: column;
}

.customer-name {
  font-weight: 500;
  color: #1a1a2e;
}

.customer-email {
  font-size: 12px;
  color: #999;
}

/* Rating Stars */
.rating-stars {
  display: flex;
  align-items: center;
  gap: 2px;
}

.rating-stars i {
  font-size: 14px;
  color: #ddd;
}

.rating-stars i.active {
  color: #f5c842;
}

.rating-number-small {
  margin-left: 5px;
  font-weight: 600;
  color: #1a1a2e;
  font-size: 13px;
}

/* Comment */
.comment-cell {
  max-width: 150px;
}

.comment-preview {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #555;
  font-size: 13px;
}

/* Review Images */
.review-images {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-wrap: wrap;
}

.thumbnail-img {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  object-fit: cover;
  cursor: pointer;
  border: 1px solid #e9ecef;
  transition: all 0.3s;
}

.thumbnail-img:hover {
  transform: scale(1.1);
}

.more-images {
  font-size: 12px;
  color: #999;
  font-weight: 600;
}

.no-image {
  color: #ccc;
}

/* Status Badge */
.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.status-badge.status-pending {
  background: #fff3cd;
  color: #856404;
}

.status-badge.status-approved {
  background: #d4edda;
  color: #155724;
}

.status-badge.status-rejected {
  background: #f8d7da;
  color: #721c24;
}

/* Date Info */
.date-info {
  font-size: 12px;
  color: #999;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-sm {
  padding: 5px 10px;
  font-size: 12px;
  min-width: 30px;
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
  background: #bd2130;
}

.btn-info {
  background: #17a2b8;
  color: white;
}

.btn-info:hover {
  background: #138496;
}

.btn-warning {
  background: #ffc107;
  color: #333;
}

.btn-warning:hover {
  background: #e0a800;
}

.btn-outline {
  background: transparent;
  border: 1px solid #ddd;
  color: #666;
}

.btn-outline:hover {
  border-color: #e63946;
  color: #e63946;
}

.btn-primary {
  background: #e63946;
  color: white;
}

.btn-primary:hover {
  background: #c62828;
}

/* ===== PAGINATION ===== */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #e9ecef;
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

.modal-container {
  background: white;
  border-radius: 16px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideIn 0.3s;
}

.modal-container.modal-sm {
  max-width: 450px;
  width: 90%;
}

.modal-container.modal-lg {
  max-width: 700px;
  width: 90%;
}

@keyframes slideIn {
  from { transform: translateY(-30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: linear-gradient(135deg, #e63946, #ff6b6b);
  color: white;
  border-radius: 16px 16px 0 0;
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
  transition: all 0.3s;
}

.close-btn:hover {
  transform: rotate(90deg);
}

.modal-body {
  padding: 24px;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Detail Grid */
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  color: #1a1a2e;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e63946;
  font-size: 15px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 6px 0;
  font-size: 14px;
}

.detail-row label {
  font-weight: 600;
  color: #666;
  min-width: 100px;
  flex-shrink: 0;
}

.detail-row span {
  color: #1a1a2e;
}

.comment-full {
  color: #555;
  line-height: 1.6;
  margin: 0;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 8px;
}

/* Image Gallery */
.image-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.gallery-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e9ecef;
}

.gallery-image:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* Image Viewer */
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

/* Form Group */
.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 6px;
}

.form-control {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  transition: all 0.3s;
}

.form-control:focus {
  outline: none;
  border-color: #e63946;
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

/* ===== LOADING & EMPTY ===== */
.loading-state {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 12px;
}

.loading-state i {
  font-size: 50px;
  color: #e63946;
  margin-bottom: 15px;
}

.loading-state p {
  color: #666;
  font-size: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-state i {
  font-size: 60px;
  color: #ddd;
  margin-bottom: 15px;
}

.empty-state h3 {
  color: #1a1a2e;
  margin-bottom: 8px;
}

.empty-state p {
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
  max-width: 400px;
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}

/* ===== RESPONSIVE ===== */
@media (max-width: 1024px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-left {
    justify-content: center;
  }

  .filter-group {
    flex-direction: column;
    width: 100%;
  }

  .filter-select {
    width: 100%;
  }

  .search-box {
    width: 100%;
  }

  .search-box input {
    flex: 1;
    width: auto;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .reviews-table {
    font-size: 12px;
  }

  .reviews-table thead th,
  .reviews-table tbody td {
    padding: 8px 10px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 3px;
  }

  .modal-container.modal-lg {
    max-width: 95%;
    margin: 10px;
  }

  .image-viewer-container .close-btn {
    top: 10px;
    right: 10px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .modal-footer {
    flex-direction: column;
  }

  .modal-footer .btn {
    width: 100%;
  }
}
</style>