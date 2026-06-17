package com.example.backend_fashionstore.customer.cart.service;

import com.example.backend_fashionstore.customer.cart.repository.CartRepository;
import com.example.backend_fashionstore.customer.cart.dto.response.CartItemResponseDTO;
import com.example.backend_fashionstore.customer.cart.dto.request.CartRequestDTO;
import com.example.backend_fashionstore.customer.cart.dto.response.CartResponseDTO;
import com.example.backend_fashionstore.customer.cart.repository.CartItemRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.ProductPromotionRepository;
import com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ProductPromotionRepository productPromotionRepository;
    private final VoucherRepository voucherRepository;  // ✅ THÊM

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");

    @Transactional(readOnly = true)
    public CartResponseDTO getCart(Integer customerId) {
        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseGet(() -> createNewCart(customerId));
        return convertToResponse(cart);
    }

    private Cart createNewCart(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + customerId));

        Cart cart = Cart.builder()
                .customer(customer)
                .build();

        return cartRepository.save(cart);
    }

    public CartResponseDTO addToCart(Integer customerId, CartRequestDTO request) {
        Integer quantity = request.getQuantity();
        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }

        log.info("Adding to cart - customerId: {}, productDetailId: {}, quantity: {}",
                customerId, request.getProductDetailId(), quantity);

        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseGet(() -> createNewCart(customerId));

        ProductDetail productDetail = productDetailRepository.findById(request.getProductDetailId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + request.getProductDetailId()));

        if (productDetail.getQuantity() < quantity) {
            throw new RuntimeException("Số lượng sản phẩm không đủ. Còn " + productDetail.getQuantity() + " sản phẩm");
        }

        // Kiểm tra promotion
        BigDecimal discountedPrice = getDiscountedPrice(productDetail);

        CartItem existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductDetail().getProductDetailId().equals(request.getProductDetailId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + quantity;
            if (productDetail.getQuantity() < newQuantity) {
                throw new RuntimeException("Số lượng sản phẩm không đủ. Còn " + productDetail.getQuantity() + " sản phẩm");
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setDiscountedPrice(discountedPrice);  // ✅ Lưu giá đã giảm
            existingItem.calculateSubtotal();
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductDetail(productDetail);
            newItem.setQuantity(quantity);
            newItem.setDiscountedPrice(discountedPrice);  // ✅ Lưu giá đã giảm
            newItem.calculateSubtotal();
            cart.addCartItem(newItem);
            cartItemRepository.save(newItem);
        }

        return convertToResponse(cart);
    }

    public CartResponseDTO updateCartItem(Integer customerId, Integer cartItemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng không hợp lệ");
        }

        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));

        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Sản phẩm không thuộc giỏ hàng này");
        }

        if (cartItem.getProductDetail().getQuantity() < quantity) {
            throw new RuntimeException("Số lượng sản phẩm không đủ. Còn " +
                    cartItem.getProductDetail().getQuantity() + " sản phẩm");
        }

        cartItem.setQuantity(quantity);
        cartItem.calculateSubtotal();
        cartItemRepository.save(cartItem);

        return convertToResponse(cart);
    }

    public CartResponseDTO removeFromCart(Integer customerId, Integer cartItemId) {
        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));

        cart.removeCartItem(cartItem);
        cartItemRepository.delete(cartItem);

        return convertToResponse(cart);
    }

    public void clearCart(Integer customerId) {
        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
    }

    // ✅ Lấy giá đã giảm từ promotion
    private BigDecimal getDiscountedPrice(ProductDetail productDetail) {
        List<ProductPromotion> promotions = productPromotionRepository
                .findByProductDetailId(productDetail.getProductDetailId());

        if (promotions == null || promotions.isEmpty()) {
            return productDetail.getPrice();
        }

        Promotion promotion = promotions.get(0).getPromotion();
        if (promotion == null || promotion.getPromotionDeletedAt() != null ||
                !"ACTIVE".equals(promotion.getPromotionStatus())) {
            return productDetail.getPrice();
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getPromotionStartDate()) ||
                now.isAfter(promotion.getPromotionEndDate())) {
            return productDetail.getPrice();
        }

        BigDecimal discountPercent = promotion.getPromotionDiscountPercent();
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return productDetail.getPrice();
        }

        BigDecimal discount = productDetail.getPrice()
                .multiply(discountPercent.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        return productDetail.getPrice().subtract(discount)
                .setScale(0, RoundingMode.HALF_UP);
    }

    // ✅ Áp dụng voucher
    public CartResponseDTO applyVoucher(Integer customerId, String voucherCode) {
        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Mã voucher không tồn tại"));

        // Kiểm tra voucher hợp lệ
        if (!"ACTIVE".equals(voucher.getVoucherStatus()) || voucher.getVoucherDeletedAt() != null) {
            throw new RuntimeException("Voucher đã bị vô hiệu hóa");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getVoucherStartDate()) || now.isAfter(voucher.getVoucherEndDate())) {
            throw new RuntimeException("Voucher đã hết hạn hoặc chưa đến thời gian sử dụng");
        }

        if (voucher.getVoucherUsedCount() >= voucher.getVoucherMaxUsage()) {
            throw new RuntimeException("Voucher đã được sử dụng hết số lần");
        }

        // Tính tổng giỏ hàng (sau promotion)
        BigDecimal cartTotal = calculateCartTotal(cart);

        // Kiểm tra giá trị đơn hàng tối thiểu
        if (voucher.getVoucherMinOrderValue() != null &&
                cartTotal.compareTo(voucher.getVoucherMinOrderValue()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để sử dụng voucher: " +
                    voucher.getVoucherMinOrderValue());
        }

        // Tính discount từ voucher
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (voucher.getVoucherDiscountAmount() != null &&
                voucher.getVoucherDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            voucherDiscount = voucher.getVoucherDiscountAmount().min(cartTotal);
        } else if (voucher.getVoucherDiscountPercent() != null &&
                voucher.getVoucherDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            voucherDiscount = cartTotal.multiply(voucher.getVoucherDiscountPercent())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }

        // Lưu voucher vào cart
        cart.setVoucher(voucher);
        cart.setVoucherDiscount(voucherDiscount);
        cartRepository.save(cart);

        return convertToResponse(cart);
    }

    // ✅ Xóa voucher
    public CartResponseDTO removeVoucher(Integer customerId) {
        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        cart.setVoucher(null);
        cart.setVoucherDiscount(BigDecimal.ZERO);
        cartRepository.save(cart);

        return convertToResponse(cart);
    }

    // ✅ Tính tổng giỏ hàng (sau promotion)
    private BigDecimal calculateCartTotal(Cart cart) {
        return cart.getCartItems().stream()
                .map(item -> {
                    BigDecimal price = item.getDiscountedPrice() != null ?
                            item.getDiscountedPrice() : item.getProductDetail().getPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartResponseDTO convertToResponse(Cart cart) {
        BigDecimal subtotal = cart.getCartItems().stream()
                .map(item -> {
                    BigDecimal price = item.getDiscountedPrice() != null ?
                            item.getDiscountedPrice() : item.getProductDetail().getPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = cart.getCartItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        // Tính voucher discount
        BigDecimal voucherDiscount = cart.getVoucherDiscount() != null ?
                cart.getVoucherDiscount() : BigDecimal.ZERO;

        // Tính tổng sau khi trừ voucher
        BigDecimal total = subtotal.subtract(voucherDiscount).add(DEFAULT_SHIPPING_FEE);

        return CartResponseDTO.builder()
                .cartId(cart.getCartId())
                .customerId(cart.getCustomer().getCustomerId())
                .items(cart.getCartItems().stream()
                        .map(this::convertToItemResponse)
                        .collect(Collectors.toList()))
                .subtotal(subtotal)
                .totalQuantity(totalQuantity)
                .voucherDiscount(voucherDiscount)
                .shippingFee(DEFAULT_SHIPPING_FEE)
                .total(total)
                .voucherCode(cart.getVoucher() != null ? cart.getVoucher().getVoucherCode() : null)
                .build();
    }

    private CartItemResponseDTO convertToItemResponse(CartItem item) {
        String imageUrl = null;
        if (item.getProductDetail().getProductImages() != null &&
                !item.getProductDetail().getProductImages().isEmpty()) {
            imageUrl = item.getProductDetail().getProductImages().get(0).getProductImagePath();
        }

        BigDecimal originalPrice = item.getProductDetail().getPrice();
        BigDecimal discountedPrice = item.getDiscountedPrice() != null ?
                item.getDiscountedPrice() : originalPrice;

        return CartItemResponseDTO.builder()
                .cartItemId(item.getCartItemId())
                .productDetailId(item.getProductDetail().getProductDetailId())
                .productId(item.getProductDetail().getProduct().getProductId())
                .productName(item.getProductDetail().getProduct().getProductName())
                .colorName(item.getProductDetail().getColor() != null ?
                        item.getProductDetail().getColor().getColorName() : null)
                .sizeName(item.getProductDetail().getSize() != null ?
                        item.getProductDetail().getSize().getSizeName() : null)
                .price(originalPrice)
                .discountedPrice(discountedPrice)  // ✅ Thêm giá đã giảm
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .imageUrl(imageUrl)
                .stock(item.getProductDetail().getQuantity())
                .build();
    }
}