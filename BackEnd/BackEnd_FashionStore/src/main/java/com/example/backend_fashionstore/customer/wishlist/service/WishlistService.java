package com.example.backend_fashionstore.customer.wishlist.service;

import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.customer.wishlist.dto.response.WishlistItemResponseDTO;
import com.example.backend_fashionstore.customer.wishlist.dto.response.WishlistResponseDTO;
import com.example.backend_fashionstore.customer.wishlist.repository.WishlistItemRepository;
import com.example.backend_fashionstore.customer.wishlist.repository.WishlistRepository;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.entity.Product;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.entity.Wishlist;
import com.example.backend_fashionstore.entity.WishlistItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductDetailRepository productDetailRepository;

    @Transactional(readOnly = true)
    public WishlistResponseDTO getWishlist(Integer customerId) {
        Wishlist wishlist = wishlistRepository.findByCustomerIdAndDeletedAtIsNull(customerId).orElse(null);
        if (wishlist == null) {
            return WishlistResponseDTO.builder()
                    .wishlistId(null)
                    .customerId(customerId)
                    .totalItems(0)
                    .items(List.of())
                    .build();
        }
        return convertToResponse(wishlist);
    }

    // ✅ THÊM VÀO YÊU THÍCH - FIX LỖI DUPLICATE
    public WishlistResponseDTO addToWishlist(Integer customerId, Integer productDetailId) {
        if (productDetailId == null) {
            throw new RuntimeException("Thiếu thông tin sản phẩm");
        }

        // 1. Lấy hoặc tạo wishlist
        Wishlist wishlist = wishlistRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseGet(() -> createNewWishlist(customerId));

        // 2. Kiểm tra sản phẩm tồn tại
        ProductDetail productDetail = productDetailRepository.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productDetailId));

        if (isProductUnavailable(productDetail)) {
            throw new RuntimeException("Sản phẩm này không còn tồn tại hoặc đã ngừng kinh doanh");
        }

        // 3. ⭐ QUAN TRỌNG: Tìm item kể cả đã bị xóa mềm
        // Sử dụng method mới findByWishlistIdAndProductDetailId
        java.util.Optional<WishlistItem> existingItem = wishlistItemRepository
                .findByWishlistIdAndProductDetailId(wishlist.getWishlistId(), productDetailId);

        if (existingItem.isPresent()) {
            // 4. Nếu đã tồn tại (kể cả REMOVED) -> UPDATE lại thành ACTIVE
            WishlistItem item = existingItem.get();
            item.setWishlistItemStatus("ACTIVE");
            item.setWishlistItemDeletedAt(null);  // Xóa deleted_at
            item.setWishlistItemUpdatedAt(LocalDateTime.now());
            item.setWishlistItemAddedAt(LocalDateTime.now()); // Cập nhật lại thời gian thêm
            wishlistItemRepository.save(item);
            log.info("Đã kích hoạt lại wishlist item: {}", item.getWishlistItemId());
        } else {
            // 5. Nếu chưa tồn tại -> Tạo mới
            WishlistItem newItem = new WishlistItem();
            newItem.setWishlist(wishlist);
            newItem.setProductDetail(productDetail);
            newItem.setWishlistItemStatus("ACTIVE");
            newItem.setWishlistItemAddedAt(LocalDateTime.now());
            wishlistItemRepository.save(newItem);
            log.info("Đã thêm mới wishlist item: {}", newItem.getWishlistItemId());
        }

        // 6. Refresh và trả về response
        Wishlist refreshed = wishlistRepository.findByCustomerIdAndDeletedAtIsNull(customerId).orElseThrow();
        return convertToResponse(refreshed);
    }

    // ❌ XÓA KHỎI YÊU THÍCH (SOFT DELETE)
    public WishlistResponseDTO removeFromWishlist(Integer customerId, Integer productDetailId) {
        Wishlist wishlist = wishlistRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh sách yêu thích"));

        // Tìm item kể cả đã bị xóa mềm
        WishlistItem item = wishlistItemRepository
                .findByWishlistIdAndProductDetailId(wishlist.getWishlistId(), productDetailId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong danh sách yêu thích"));

        // Soft delete: set deleted_at và status = 'REMOVED'
        item.setWishlistItemDeletedAt(LocalDateTime.now());
        item.setWishlistItemStatus("REMOVED");
        item.setWishlistItemUpdatedAt(LocalDateTime.now());
        wishlistItemRepository.save(item);

        log.info("Đã xóa wishlist item: {}", item.getWishlistItemId());

        return convertToResponse(wishlist);
    }

    // ❌ XÓA HẲN (HARD DELETE) - Nếu cần xóa vĩnh viễn
    public WishlistResponseDTO hardRemoveFromWishlist(Integer customerId, Integer productDetailId) {
        Wishlist wishlist = wishlistRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh sách yêu thích"));

        WishlistItem item = wishlistItemRepository
                .findByWishlistIdAndProductDetailId(wishlist.getWishlistId(), productDetailId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong danh sách yêu thích"));

        // Xóa hẳn khỏi database
        wishlistItemRepository.delete(item);
        log.info("Đã xóa vĩnh viễn wishlist item: {}", item.getWishlistItemId());

        return convertToResponse(wishlist);
    }

    private Wishlist createNewWishlist(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + customerId));

        Wishlist wishlist = new Wishlist();
        wishlist.setCustomer(customer);
        wishlist.setWishlistName("Default");
        wishlist.setWishlistStatus("ACTIVE");
        return wishlistRepository.save(wishlist);
    }

    private boolean isProductUnavailable(ProductDetail productDetail) {
        if (productDetail == null || productDetail.getDeletedAt() != null
                || "DELETED".equals(productDetail.getStatus())) {
            return true;
        }
        Product product = productDetail.getProduct();
        return product == null || product.getDeletedAt() != null
                || "DELETED".equals(product.getStatus());
    }

    private WishlistResponseDTO convertToResponse(Wishlist wishlist) {
        List<WishlistItem> activeItems = wishlistItemRepository.findActiveByWishlistId(wishlist.getWishlistId())
                .stream()
                .filter(item -> !isProductUnavailable(item.getProductDetail()))
                .collect(Collectors.toList());

        List<WishlistItemResponseDTO> items = activeItems.stream()
                .map(this::convertToItemResponse)
                .collect(Collectors.toList());

        return WishlistResponseDTO.builder()
                .wishlistId(wishlist.getWishlistId())
                .customerId(wishlist.getCustomer().getCustomerId())
                .totalItems(items.size())
                .items(items)
                .build();
    }

    private WishlistItemResponseDTO convertToItemResponse(WishlistItem item) {
        ProductDetail detail = item.getProductDetail();

        String imageUrl = null;
        if (detail.getProductImages() != null && !detail.getProductImages().isEmpty()) {
            imageUrl = detail.getProductImages().get(0).getProductImagePath();
        }

        return WishlistItemResponseDTO.builder()
                .wishlistItemId(item.getWishlistItemId())
                .productDetailId(detail.getProductDetailId())
                .productId(detail.getProduct().getProductId())
                .productName(detail.getProduct().getProductName())
                .colorName(detail.getColor() != null ? detail.getColor().getColorName() : null)
                .sizeName(detail.getSize() != null ? detail.getSize().getSizeName() : null)
                .price(detail.getPrice())
                .discountedPrice(detail.getPrice())
                .imageUrl(imageUrl)
                .stock(detail.getQuantity())
                .addedAt(item.getWishlistItemAddedAt())
                .build();
    }
}