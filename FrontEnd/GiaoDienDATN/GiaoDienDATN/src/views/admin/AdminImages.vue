<template>
  <div class="admin-images">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>Quản lý ảnh sản phẩm</h2>
        <p class="subtitle">Quản lý hình ảnh cho biến thể sản phẩm (màu + size)</p>
      </div>
    </div>

    <!-- Step 1: Chọn sản phẩm -->
    <div class="product-detail-selector">
      <div class="input-group">
        <label>📦 Chọn sản phẩm:</label>
        <input 
          type="text" 
          v-model="productSearchKeyword" 
          placeholder="🔍 Tìm nhanh sản phẩm..." 
          class="product-search-input"
        />
        <select v-model="selectedProductId" @change="loadProductDetails" class="detail-select">
          <option :value="null">-- Chọn sản phẩm --</option>
          <option 
            v-for="product in filteredProductList" 
            :key="product.productId" 
            :value="product.productId"
          >
            {{ product.productName }}
          </option>
        </select>
      </div>
      
      <div class="input-group" v-if="selectedProductId && productDetailsList.length > 0">
        <label>🎨 Chọn biến thể (Màu - Size):</label>
        <select v-model="selectedProductDetailId" @change="loadImages" class="detail-select">
          <option :value="null">-- Chọn biến thể --</option>
          <option 
            v-for="detail in productDetailsList" 
            :key="detail.productDetailId" 
            :value="detail.productDetailId"
          >
            {{ detail.colorName }} - {{ detail.sizeName }} (SL: {{ detail.quantity }})
          </option>
        </select>
      </div>
      
      <div class="info-note">
        <span class="info-icon">ℹ️</span>
        Chọn sản phẩm, sau đó chọn biến thể (màu + size) để quản lý ảnh
      </div>
    </div>

    <!-- Stats Row -->
    <div class="stats-row" v-if="images.length > 0">
      <div class="stat-card stat-total">
        <div class="stat-icon">🖼️</div>
        <div class="stat-info">
          <div class="stat-value">{{ images.length }}</div>
          <div class="stat-label">Tổng số ảnh</div>
        </div>
      </div>
      <div class="stat-card stat-main">
        <div class="stat-icon">⭐</div>
        <div class="stat-info">
          <div class="stat-value">{{ mainImageCount }}</div>
          <div class="stat-label">Ảnh chính</div>
        </div>
      </div>
      <div class="stat-card stat-deleted">
        <div class="stat-icon">🗑️</div>
        <div class="stat-info">
          <div class="stat-value">{{ deletedImageCount }}</div>
          <div class="stat-label">Đã xóa</div>
        </div>
      </div>
    </div>

    <!-- Upload Area -->
    <div v-if="selectedProductDetailId" class="upload-area" @dragover.prevent @drop.prevent="handleDrop">
      <div class="upload-content" @click="triggerFileInput">
        <div class="upload-icon">☁️⬆️</div>
        <p>Kéo thả ảnh vào đây hoặc click để chọn</p>
        <span class="upload-hint">Hỗ trợ JPG, PNG, GIF, WEBP (tối đa 5MB/ảnh)</span>
        <span class="upload-hint">Có thể chọn nhiều ảnh cùng lúc</span>
      </div>
      <input
        type="file"
        ref="fileInput"
        multiple
        accept="image/*"
        style="display: none"
        @change="handleFileSelect"
      />
    </div>

    <!-- Upload Preview -->
    <div v-if="uploadPreviews.length > 0" class="upload-preview">
      <h4>📸 Ảnh sắp thêm ({{ uploadPreviews.length }})</h4>
      <div class="preview-grid">
        <div v-for="(preview, index) in uploadPreviews" :key="index" class="preview-item">
          <img :src="preview.url" />
          <button class="remove-preview" @click="removePreview(index)">
            ✕
          </button>
        </div>
      </div>
      <div class="upload-actions">
        <button class="btn-upload" @click="uploadMultipleImages" :disabled="uploading">
          <span v-if="uploading" class="loading-spinner-small"></span>
          <span v-else>📤</span>
          {{ uploading ? 'Đang tải lên...' : `Tải lên ${uploadPreviews.length} ảnh` }}
        </button>
        <button class="btn-cancel-upload" @click="clearUploadPreview">
          ✕ Hủy
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>Đang tải ảnh...</span>
    </div>

    <!-- Images Table -->
    <div v-else-if="images.length > 0" class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>STT</th>
            <th>Ảnh</th>
            <th>Đường dẫn</th>
            <th>Số thứ tự</th>
            <th>Ảnh chính</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(image, index) in paginatedImages" :key="image.productImageId" class="table-row" :class="{ 'row-deleted': image._deletedAt }">
            <td class="stt-cell">{{ getSTT(index) }}</td>
            <td>
              <div class="image-thumb">
                <img 
                  :src="getImageUrl(image.productImagePath)" 
                  :alt="'Hình ' + (index + 1)"
                  @error="handleImageError"
                  loading="lazy"
                  crossorigin="anonymous"
                />
              </div>
            </td>
            <td class="path-cell" :title="image.productImagePath">
              {{ getShortPath(image.productImagePath) }}
            </td>
            <td>
              <input 
                type="number" 
                v-model.number="image.displayOrder" 
                @change="updateDisplayOrder(image)"
                class="order-input"
                min="0"
                :disabled="image._deletedAt"
              />
            </td>
            <td>
              <label class="main-image-checkbox">
                <input 
                  type="radio" 
                  name="mainImage"
                  :checked="image.isMain === true"
                  @change="setMainImage(image)"
                  :disabled="image._deletedAt"
                />
                <span>Ảnh chính</span>
              </label>
            </td>
            <td>
              <span v-if="image._deletedAt" class="status-badge status-deleted">
                Đã xóa
              </span>
              <span v-else :class="['status-badge', image.status === 'ACTIVE' ? 'status-active' : 'status-inactive']">
                {{ image.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng' }}
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <template v-if="image._deletedAt">
                  <span class="readonly-label">Đã xóa</span>
                  <button class="action-btn restore-btn" @click="confirmRestore(image)" title="Khôi phục">
                    ↩️
                  </button>
                </template>
                <template v-else>
                  <button class="action-btn delete-btn" @click="confirmDelete(image)" title="Xóa ảnh">
                    🗑️
                  </button>
                </template>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="pagination" v-if="totalPages > 1">
        <span class="page-total">Tổng: {{ images.length }} ảnh</span>
        <div class="page-controls">
          <button @click="goToPage(0)" :disabled="currentPage === 0">«</button>
          <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 0">‹</button>
          <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button @click="goToPage(currentPage + 1)" :disabled="currentPage + 1 >= totalPages">›</button>
          <button @click="goToPage(totalPages - 1)" :disabled="currentPage + 1 >= totalPages">»</button>
        </div>
      </div>
    </div>

    <div v-else-if="selectedProductDetailId && !loading" class="empty-state">
      <div class="empty-icon">📭</div>
      <h3>Chưa có ảnh nào</h3>
      <p>Hãy tải lên ảnh cho biến thể sản phẩm này</p>
    </div>

    <!-- Delete Modal -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận xóa</h3>
          <button class="close-btn" @click="closeDeleteModal">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc chắn muốn xóa ảnh này?</p>
          <p class="warning-text">Sau khi xóa, ảnh sẽ chuyển sang trạng thái đã xóa và có thể khôi phục lại!</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeDeleteModal">Hủy</button>
          <button class="btn-danger" @click="deleteImage">Xóa</button>
        </div>
      </div>
    </div>

    <!-- Restore Modal -->
    <div v-if="showRestoreModal" class="modal-overlay" @click.self="closeRestoreModal">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3>Xác nhận khôi phục</h3>
          <button class="close-btn" @click="closeRestoreModal">✕</button>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc muốn khôi phục ảnh này?</p>
          <p>Sau khi khôi phục, ảnh sẽ trở lại trạng thái hoạt động.</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeRestoreModal">Hủy</button>
          <button class="btn-save" @click="restoreImage">Khôi phục</button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div :class="['toast', { 'toast-error': toastType === 'error', show: showToast }]">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script>
import api from "@/api/axios";

export default {
  name: 'AdminImages',
  data() {
    return {
      productList: [],
      selectedProductId: null,
      productDetailsList: [],
      selectedProductDetailId: null,
      loadingProducts: false,
      productSearchKeyword: '',
      
      allImages: [],
      images: [],
      loading: false,
      uploading: false,
      
      currentPage: 0,
      pageSize: 5,
      
      uploadPreviews: [],
      selectedFiles: [],
      
      showDeleteModal: false,
      showRestoreModal: false,
      deleteImageId: null,
      restoreImageId: null,
      
      showToast: false,
      toastMessage: '',
      toastType: 'success',
      toastTimer: null
    };
  },
  computed: {
    filteredProductList() {
      if (!this.productSearchKeyword || !this.productSearchKeyword.trim()) {
        return this.productList;
      }
      const kw = this.productSearchKeyword.trim().toLowerCase();
      return this.productList.filter(product => 
        product.productName?.toLowerCase().includes(kw)
      );
    },
    mainImageCount() {
      return this.images.filter(img => img.isMain === true && !img._deletedAt).length;
    },
    deletedImageCount() {
      return this.images.filter(img => img._deletedAt).length;
    },
    totalPages() {
      return Math.ceil(this.images.length / this.pageSize);
    },
    paginatedImages() {
      const start = this.currentPage * this.pageSize;
      const end = start + this.pageSize;
      return this.images.slice(start, end);
    }
  },
  methods: {
    showNotification(message, type = 'success') {
      if (this.toastTimer) clearTimeout(this.toastTimer);
      this.toastMessage = message;
      this.toastType = type;
      this.showToast = true;
      this.toastTimer = setTimeout(() => {
        this.showToast = false;
      }, 3000);
    },

    getSTT(index) {
      return (this.currentPage * this.pageSize) + index + 1;
    },

    getShortPath(path) {
      if (!path) return '—';
      const parts = path.split('/');
      return parts[parts.length - 1] || path;
    },

    getImageUrl(path) {
      if (!path) {
        return 'https://via.placeholder.com/80x80?text=No+Image';
      }
      
      if (path.startsWith('http://') || path.startsWith('https://')) {
        return path;
      }
      
      let cleanPath = path;
      if (cleanPath.startsWith('uploads/')) {
        return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/${cleanPath}`;
      }
      if (cleanPath.startsWith('/uploads/')) {
        return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/${cleanPath}`;
      }
      if (cleanPath.startsWith('product-images/')) {
        return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/uploads/${cleanPath}`;
      }
      if (cleanPath.startsWith('/product-images/')) {
        return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/uploads${cleanPath}`;
      }
      
      if (!cleanPath.startsWith('/')) {
        cleanPath = '/' + cleanPath;
      }
      return `${(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || "").replace(/\/api$/, "")}/uploads${cleanPath}`;
    },

    handleImageError(e) {
      console.error('❌ Lỗi tải ảnh:', e.target.src);
      const failedUrl = e.target.src;
      if (failedUrl.includes('/uploads/uploads/')) {
        const fixedUrl = failedUrl.replace('/uploads/uploads/', '/uploads/');
        e.target.src = fixedUrl;
        return;
      }
      e.target.src = 'https://via.placeholder.com/80x80?text=Error';
      e.target.style.objectFit = 'contain';
      e.target.style.backgroundColor = '#f0f0f0';
    },

    async loadProducts() {
      this.loadingProducts = true;
      try {
        const response = await api.get(`/products`, { 
          params: { page: 0, size: 100 } 
        });
        
        let products = [];
        if (response.data?.data?.content) {
          products = response.data.data.content;
        } else if (response.data?.content) {
          products = response.data.content;
        } else if (Array.isArray(response.data)) {
          products = response.data;
        }
        
        this.productList = products.map(p => ({
          productId: p.productId,
          productName: p.productName
        }));
        
        console.log('✅ Loaded products:', this.productList.length);
      } catch (error) {
        console.error('❌ Lỗi tải sản phẩm:', error);
        this.productList = [];
      } finally {
        this.loadingProducts = false;
      }
    },

    async loadProductDetails() {
      if (!this.selectedProductId) {
        this.productDetailsList = [];
        this.selectedProductDetailId = null;
        return;
      }
      
      try {
        const response = await api.get(`/products/${this.selectedProductId}`);
        const product = response.data?.data;
        
        if (product && product.variants) {
          this.productDetailsList = product.variants.map(v => ({
            productDetailId: v.productDetailId,
            colorName: v.color,
            sizeName: v.size,
            quantity: v.quantity,
            price: v.price,
            sku: v.sku
          }));
        } else {
          this.productDetailsList = [];
        }
        
        this.selectedProductDetailId = null;
        this.images = [];
        console.log('✅ Loaded variants:', this.productDetailsList.length);
      } catch (error) {
        console.error('❌ Lỗi tải biến thể:', error);
        this.productDetailsList = [];
      }
    },

    async loadImages() {
      if (!this.selectedProductDetailId) {
        this.images = [];
        return;
      }
      
      this.loading = true;
      try {
        const response = await api.get(`/product-images`, {
          params: { productDetailId: this.selectedProductDetailId, page: 0, size: 100 }
        });
        
        let allImages = [];
        if (response && response.data) {
          if (response.data.content) {
            allImages = response.data.content;
          } else if (response.data.data?.content) {
            allImages = response.data.data.content;
          } else if (Array.isArray(response.data)) {
            allImages = response.data;
          }
        }
        
        // ✅ GIỮ TẤT CẢ ẢNH (bao gồm cả đã xóa)
        this.images = allImages.map(img => ({
          ...img,
          _deletedAt: img.status === 'DELETED' ? new Date().toISOString() : null
        }));
        this.images.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        this.currentPage = 0;
        
        console.log('✅ Loaded images:', this.images.length);
      } catch (error) {
        console.error('❌ Lỗi tải ảnh:', error);
        this.showNotification('Không thể tải ảnh', 'error');
        this.images = [];
      } finally {
        this.loading = false;
      }
    },

    async updateDisplayOrder(image) {
      if (image._deletedAt) {
        this.showNotification('Không thể cập nhật ảnh đã xóa!', 'error');
        return;
      }
      
      try {
        const formData = new FormData();
        formData.append('displayOrder', image.displayOrder || 0);
        formData.append('productImageType', image.productImageType || 'gallery');
        formData.append('isMain', image.isMain || false);
        
        await api.put(`/product-images/${image.productImageId}`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        this.showNotification('Cập nhật số thứ tự thành công!', 'success');
        
        this.images.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
      } catch (error) {
        console.error('Lỗi cập nhật số thứ tự:', error);
        this.showNotification('Cập nhật thất bại!', 'error');
        await this.loadImages();
      }
    },

    triggerFileInput() {
      this.$refs.fileInput.click();
    },

    handleFileSelect(event) {
      const files = Array.from(event.target.files);
      this.addFiles(files);
    },

    handleDrop(event) {
      const files = Array.from(event.dataTransfer.files);
      this.addFiles(files);
    },

    addFiles(files) {
      const imageFiles = files.filter(f => f.type.startsWith('image/'));
      
      for (const file of imageFiles) {
        if (file.size > 5 * 1024 * 1024) {
          this.showNotification(`File ${file.name} quá lớn (tối đa 5MB)`, 'error');
          continue;
        }
        
        this.selectedFiles.push(file);
        
        const reader = new FileReader();
        reader.onload = (e) => {
          this.uploadPreviews.push({
            url: e.target.result,
            file: file
          });
        };
        reader.readAsDataURL(file);
      }
      
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = '';
      }
    },

    removePreview(index) {
      this.selectedFiles.splice(index, 1);
      this.uploadPreviews.splice(index, 1);
    },

    clearUploadPreview() {
      this.selectedFiles = [];
      this.uploadPreviews = [];
    },

    async uploadMultipleImages() {
      if (this.selectedFiles.length === 0) return;
      if (!this.selectedProductDetailId) {
        this.showNotification('Vui lòng chọn biến thể sản phẩm', 'error');
        return;
      }
      
      this.uploading = true;
      let successCount = 0;
      let errorCount = 0;
      
      for (const file of this.selectedFiles) {
        try {
          const formData = new FormData();
          formData.append('productDetailId', this.selectedProductDetailId);
          formData.append('imageFile', file);
          formData.append('productImageType', 'gallery');
          
          await api.post(`/product-images`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          });
          successCount++;
        } catch (error) {
          console.error(`Lỗi upload ${file.name}:`, error);
          errorCount++;
        }
      }
      
      if (successCount > 0) {
        this.showNotification(`Đã tải lên ${successCount} ảnh thành công${errorCount > 0 ? `, ${errorCount} ảnh thất bại` : ''}`, 'success');
        await this.loadImages();
      } else {
        this.showNotification('Không thể tải lên ảnh nào', 'error');
      }
      
      this.clearUploadPreview();
      this.uploading = false;
    },

    async setMainImage(image) {
      if (image._deletedAt) {
        this.showNotification('Không thể đặt ảnh chính cho ảnh đã xóa!', 'error');
        return;
      }
      
      try {
        const formData = new FormData();
        formData.append('isMain', true);
        await api.put(`/product-images/${image.productImageId}`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        
        this.images.forEach(img => {
          img.isMain = (img.productImageId === image.productImageId);
        });
        
        this.showNotification('Đã đặt ảnh chính thành công!', 'success');
      } catch (error) {
        console.error('Lỗi đặt ảnh chính:', error);
        this.showNotification('Không thể đặt ảnh chính!', 'error');
      }
    },

    // ✅ SOFT DELETE - Xóa mềm (giữ lại trong danh sách)
    confirmDelete(image) {
      this.deleteImageId = image.productImageId;
      this.showDeleteModal = true;
    },

    closeDeleteModal() {
      this.showDeleteModal = false;
      this.deleteImageId = null;
    },

    async deleteImage() {
      try {
        // ✅ GỌI API SOFT DELETE
        await api.delete(`/product-images/${this.deleteImageId}`);
        
        // ✅ CẬP NHẬT TRẠNG THÁI (KHÔNG XÓA KHỎI DANH SÁCH)
        const index = this.images.findIndex(img => img.productImageId === this.deleteImageId);
        if (index !== -1) {
          this.images[index] = {
            ...this.images[index],
            _deletedAt: new Date().toISOString(),
            status: 'DELETED'
          };
        }
        
        this.showNotification('Xóa ảnh thành công! Ảnh đã chuyển sang trạng thái đã xóa.', 'success');
        this.closeDeleteModal();
        // Reload lại danh sách để cập nhật thứ tự
        await this.loadImages();
      } catch (error) {
        console.error('Lỗi xóa ảnh:', error);
        this.showNotification(error.response?.data?.message || 'Không thể xóa ảnh!', 'error');
      }
    },

    // ✅ RESTORE - Khôi phục
    confirmRestore(image) {
      this.restoreImageId = image.productImageId;
      this.showRestoreModal = true;
    },

    closeRestoreModal() {
      this.showRestoreModal = false;
      this.restoreImageId = null;
    },

    async restoreImage() {
      try {
        // ✅ GỌI API RESTORE
        await api.patch(`/product-images/${this.restoreImageId}/restore`);
        
        // ✅ CẬP NHẬT TRẠNG THÁI
        const index = this.images.findIndex(img => img.productImageId === this.restoreImageId);
        if (index !== -1) {
          this.images[index] = {
            ...this.images[index],
            _deletedAt: null,
            status: 'ACTIVE'
          };
        }
        
        this.showNotification('Khôi phục ảnh thành công!', 'success');
        this.closeRestoreModal();
        await this.loadImages();
      } catch (error) {
        console.error('Lỗi khôi phục ảnh:', error);
        this.showNotification(error.response?.data?.message || 'Không thể khôi phục ảnh!', 'error');
      }
    },
    
    goToPage(page) {
      if (page >= 0 && page < this.totalPages) {
        this.currentPage = page;
      }
    }
  },
  mounted() {
    this.loadProducts();
  }
};
</script>

<style scoped>
.admin-images {
  padding: 20px;
  background-color: #f8f9fa;
  min-height: calc(100vh - 60px);
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h2 {
  margin: 0 0 5px 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #7f8c8d;
  font-size: 14px;
}

.product-detail-selector {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.input-group {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.input-group label {
  font-weight: 600;
  color: #2c3e50;
  min-width: 180px;
}

.detail-select {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  transition: border-color 0.2s;
}

.detail-select:focus {
  outline: none;
  border-color: #3498db;
}

.product-search-input {
  width: 250px;
  padding: 10px 12px;
  border: 1px solid #e0e6ed;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.product-search-input:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
}

.info-note {
  font-size: 13px;
  color: #6b7280;
  padding: 8px 12px;
  background: #f3f4f6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-icon {
  font-size: 14px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  border-left: 4px solid;
}

.stat-total { border-left-color: #3b82f6; }
.stat-main { border-left-color: #f59e0b; }
.stat-deleted { border-left-color: #ef4444; }

.stat-icon { font-size: 32px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a2e; }
.stat-label { font-size: 12px; color: #6b7280; margin-top: 4px; }

.upload-area {
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
  margin-bottom: 24px;
}

.upload-area:hover {
  border-color: #3498db;
  background: #f0f9ff;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.upload-content p {
  margin: 0;
  font-size: 16px;
  color: #2c3e50;
}

.upload-hint {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.upload-preview {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.upload-preview h4 {
  margin: 0 0 16px 0;
  color: #2c3e50;
  font-size: 16px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
}

.preview-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.preview-item img {
  width: 100%;
  height: 80px;
  object-fit: cover;
}

.remove-preview {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ef4444;
  color: white;
  border: none;
  cursor: pointer;
  font-size: 12px;
}

.upload-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-upload, .btn-cancel-upload {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-upload {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.btn-upload:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.btn-upload:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel-upload {
  background: #f1f5f9;
  color: #2c3e50;
}

.btn-cancel-upload:hover {
  background: #e2e8f0;
}

.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 14px 12px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
}

.data-table th {
  background-color: #f8fafc;
  color: #2c3e50;
  font-weight: 600;
  font-size: 14px;
}

.table-row:hover {
  background: #f8fafc;
}

.row-deleted {
  opacity: 0.6;
  background-color: #fafafa;
}

.row-deleted td {
  color: #9ca3af;
}

.stt-cell {
  font-weight: 600;
  color: #3498db;
}

.image-thumb {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.path-cell {
  font-size: 12px;
  color: #6b7280;
  word-break: break-all;
  max-width: 250px;
}

.order-input {
  width: 70px;
  padding: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  text-align: center;
  font-size: 14px;
}

.order-input:disabled {
  background: #f1f5f9;
  cursor: not-allowed;
}

.order-input:focus {
  outline: none;
  border-color: #3498db;
}

.main-image-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.main-image-checkbox input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.main-image-checkbox input:disabled {
  cursor: not-allowed;
}

.main-image-checkbox span {
  font-size: 13px;
  color: #2c3e50;
}

.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.status-active { background-color: #e8f8f5; color: #27ae60; }
.status-inactive { background-color: #fdf2e9; color: #f39c12; }
.status-deleted { background-color: #f3f4f6; color: #6b7280; }

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  background: none;
  border: none;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}

.delete-btn:hover { background-color: #fee2e2; }
.restore-btn:hover { background-color: #d1fae5; }

.restore-btn {
  background: #10b981;
  color: white;
  border-radius: 6px;
  padding: 6px 10px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.restore-btn:hover {
  background: #059669;
  transform: scale(1.05);
}

.readonly-label {
  font-size: 12px;
  color: #9ca3af;
  font-style: italic;
  padding: 4px 8px;
  background: #f3f4f6;
  border-radius: 6px;
  white-space: nowrap;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #3498db;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e2e8f0;
  border-top-color: #3498db;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.loading-spinner-small {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid white;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state h3 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 18px;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 15px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.page-total {
  font-size: 14px;
  color: #6b7280;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-controls button {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-controls button:hover:not(:disabled) {
  background: #3498db;
  color: white;
  border-color: #3498db;
}

.page-controls button:disabled {
  background: #f1f5f9;
  color: #cbd5e1;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #2c3e50;
  font-weight: 500;
  min-width: 70px;
  text-align: center;
}

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
  border-radius: 12px;
  width: 400px;
  max-width: 90%;
  animation: slideIn 0.3s;
}

.modal-container.modal-sm {
  width: 400px;
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
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 12px 12px 0 0;
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
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.warning-text {
  color: #dc3545;
  margin-top: 10px;
  font-size: 13px;
}

.btn-cancel, .btn-danger, .btn-save {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.btn-cancel {
  background: #f1f5f9;
  color: #2c3e50;
}

.btn-cancel:hover {
  background: #e2e8f0;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

.btn-save {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
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
  z-index: 9999;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.toast.toast-error {
  background: #e74c3c;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}

@media (max-width: 768px) {
  .admin-images { padding: 15px; }
  .page-header { flex-direction: column; align-items: stretch; gap: 12px; }
  .input-group { flex-direction: column; align-items: stretch; }
  .input-group label { min-width: auto; }
  .stats-row { grid-template-columns: 1fr; gap: 12px; }
  .table-container { overflow-x: auto; }
  .data-table { min-width: 650px; }
  .order-input { width: 55px; }
  .pagination { flex-direction: column; gap: 12px; }
  .upload-area { padding: 20px; }
}
</style>