package com.example.backend_fashionstore.customer.cart.service;

import com.example.backend_fashionstore.customer.cart.repository.CartRepository;
import com.example.backend_fashionstore.customer.cart.dto.response.CartItemResponseDTO;
import com.example.backend_fashionstore.customer.cart.dto.request.CartRequestDTO;
import com.example.backend_fashionstore.customer.cart.dto.response.CartResponseDTO;
import com.example.backend_fashionstore.customer.cart.repository.CartItemRepository;
import com.example.backend_fashionstore.entity.Cart;
import com.example.backend_fashionstore.entity.CartItem;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    // CartService.java - Phần addToCart
    // CartService.java - Sửa method addToCart

    public CartResponseDTO addToCart(Integer customerId, CartRequestDTO request) {
        // 🔥 Validate quantity
        Integer quantity = request.getQuantity();
        log.info("=== ADD TO CART SERVICE ===");
        log.info("CustomerId: {}", customerId);
        log.info("ProductDetailId: {}", request.getProductDetailId());
        log.info("Quantity from request: {}", quantity);

        if (quantity == null || quantity <= 0) {
            quantity = 1;
            log.warn("Quantity is null or invalid, setting to default 1");
        }

        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseGet(() -> createNewCart(customerId));

        ProductDetail productDetail = productDetailRepository.findById(request.getProductDetailId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + request.getProductDetailId()));

        // Kiểm tra số lượng tồn kho
        if (productDetail.getQuantity() < quantity) {
            throw new RuntimeException("Số lượng sản phẩm không đủ. Còn " + productDetail.getQuantity() + " sản phẩm");
        }

        // Tìm sản phẩm đã có trong giỏ
        CartItem existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductDetail().getProductDetailId().equals(request.getProductDetailId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Cập nhật số lượng
            int newQuantity = existingItem.getQuantity() + quantity;
            if (productDetail.getQuantity() < newQuantity) {
                throw new RuntimeException("Số lượng sản phẩm không đủ. Còn " + productDetail.getQuantity() + " sản phẩm");
            }
            existingItem.setQuantity(newQuantity);
            existingItem.calculateSubtotal();
            cartItemRepository.save(existingItem);
            log.info("Updated existing cart item - cartItemId: {}, newQuantity: {}",
                    existingItem.getCartItemId(), newQuantity);
        } else {
            // ✅ CÁCH 1: KHÔNG DÙNG BUILDER, dùng constructor hoặc setter
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductDetail(productDetail);
            newItem.setQuantity(quantity);  // ✅ Set quantity
            newItem.calculateSubtotal();

            // ✅ Log để debug
            log.info("New item created - cartId: {}, productDetailId: {}, quantity: {}, subtotal: {}",
                    cart.getCartId(),
                    productDetail.getProductDetailId(),
                    newItem.getQuantity(),
                    newItem.getSubtotal());

            cart.addCartItem(newItem);
            CartItem savedItem = cartItemRepository.save(newItem);
            log.info("Added new cart item - ID: {}, quantity: {}",
                    savedItem.getCartItemId(), savedItem.getQuantity());
        }

        return convertToResponse(cart);
    }

    public CartResponseDTO updateCartItem(Integer customerId, Integer cartItemId, Integer quantity) {
        // 🔥 FIX: Validate quantity khi update
        if (quantity == null) {
            throw new RuntimeException("Số lượng không hợp lệ");
        }

        log.info("Updating cart item - customerId: {}, cartItemId: {}, quantity: {}",
                customerId, cartItemId, quantity);

        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));

        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Sản phẩm không thuộc giỏ hàng này");
        }

        if (quantity <= 0) {
            // Xóa sản phẩm nếu quantity <= 0
            cart.removeCartItem(cartItem);
            cartItemRepository.delete(cartItem);
            log.info("Removed cart item - cartItemId: {}", cartItemId);
        } else {
            // Kiểm tra tồn kho
            if (cartItem.getProductDetail().getQuantity() < quantity) {
                throw new RuntimeException("Số lượng sản phẩm không đủ. Còn " +
                        cartItem.getProductDetail().getQuantity() + " sản phẩm");
            }
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
            log.info("Updated cart item quantity - cartItemId: {}, newQuantity: {}", cartItemId, quantity);
        }

        return convertToResponse(cart);
    }

    public CartResponseDTO removeFromCart(Integer customerId, Integer cartItemId) {
        log.info("Removing from cart - customerId: {}, cartItemId: {}", customerId, cartItemId);

        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));

        cart.removeCartItem(cartItem);
        cartItemRepository.delete(cartItem);

        log.info("Đã xóa sản phẩm {} khỏi giỏ hàng của customer {}", cartItemId, customerId);
        return convertToResponse(cart);
    }

    public void clearCart(Integer customerId) {
        log.info("Clearing cart - customerId: {}", customerId);

        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();

        log.info("Đã xóa toàn bộ giỏ hàng của customer {}", customerId);
    }

    private CartResponseDTO convertToResponse(Cart cart) {
        BigDecimal subtotal = cart.getCartItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = cart.getCartItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponseDTO.builder()
                .cartId(cart.getCartId())
                .customerId(cart.getCustomer().getCustomerId())
                .items(cart.getCartItems().stream()
                        .map(this::convertToItemResponse)
                        .collect(Collectors.toList()))
                .subtotal(subtotal)
                .totalQuantity(totalQuantity)
                .build();
    }

    private CartItemResponseDTO convertToItemResponse(CartItem item) {
        String imageUrl = null;
        if (item.getProductDetail().getProductImages() != null && !item.getProductDetail().getProductImages().isEmpty()) {
            imageUrl = item.getProductDetail().getProductImages().get(0).getProductImagePath();
        }

        return CartItemResponseDTO.builder()
                .cartItemId(item.getCartItemId())
                .productDetailId(item.getProductDetail().getProductDetailId())
                .productId(item.getProductDetail().getProduct().getProductId())
                .productName(item.getProductDetail().getProduct().getProductName())
                .colorName(item.getProductDetail().getColor() != null ? item.getProductDetail().getColor().getColorName() : null)
                .sizeName(item.getProductDetail().getSize() != null ? item.getProductDetail().getSize().getSizeName() : null)
                .price(item.getProductDetail().getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .imageUrl(imageUrl)
                .stock(item.getProductDetail().getQuantity())  // ✅ THÊM DÒNG NÀY - Lấy số lượng tồn kho
                .build();
    }
}