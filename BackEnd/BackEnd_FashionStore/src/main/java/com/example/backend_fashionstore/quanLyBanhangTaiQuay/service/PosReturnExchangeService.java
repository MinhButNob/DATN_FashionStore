package com.example.backend_fashionstore.quanLyBanhangTaiQuay.service;

import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.exception.BusinessException;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto.*;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.*;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.security.SecurityUtils;
import com.example.backend_fashionstore.quanlyNhanVien.service.EmployeeService;
import com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PosReturnExchangeService {

    private final PosOrderRepository posOrderRepository;
    private final PosOrderItemRepository posOrderItemRepository;
    private final PosReturnExchangeRepository returnExchangeRepository;
    private final PosReturnExchangeItemRepository returnExchangeItemRepository;
    private final ProductDetailRepository productDetailRepository;
    private final PosPaymentRepository posPaymentRepository;
    private final PosSessionRepository posSessionRepository;
    private final EmployeeService employeeService;
    private final VoucherRepository voucherRepository;
    private final DamagedProductRepository damagedProductRepository;

    private Integer getCurrentEmployeeId() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return employeeService.getEmployeeIdByUserId(userId);
    }

    public List<PosOrderResponse> findOrdersByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập số điện thoại khách hàng");
        }

        List<PosOrder> orders = posOrderRepository.findByPosOrderCustomerPhoneContaining(phone.trim());
        return orders.stream()
                .map(this::convertOrderToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PosReturnExchangeResponse createReturnExchange(PosReturnExchangeRequest request) {
        PosOrder order = posOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng"));

        if ("REFUNDED".equals(order.getPosOrderPaymentStatus()) ||
                "EXCHANGED".equals(order.getPosOrderStatus())) {
            throw new BusinessException("Đơn hàng này đã được xử lý đổi/trả trước đó");
        }

        if (!"COMPLETED".equals(order.getPosOrderStatus())) {
            throw new BusinessException("Chỉ có thể đổi/trả cho đơn hàng đã hoàn thành");
        }

        String referenceCode = generateReturnExchangeCode();

        PosReturnExchange returnExchange = PosReturnExchange.builder()
                .orderId(request.getOrderId())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .type(request.getType())
                .reason(request.getReason())
                .notes(request.getNotes())
                .status("PENDING")
                .employeeId(getCurrentEmployeeId())
                .referenceCode(referenceCode)
                .build();

        PosReturnExchange savedReturnExchange = returnExchangeRepository.save(returnExchange);

        if ("RETURN".equals(request.getType())) {
            return processReturn(savedReturnExchange, request, order);
        } else if ("EXCHANGE".equals(request.getType())) {
            return processExchange(savedReturnExchange, request, order);
        } else {
            throw new BusinessException("Loại đổi/trả không hợp lệ");
        }
    }

    /**
     * Xử lý hoàn tiền - CÓ PHÂN BỔ VOUCHER VÀ XỬ LÝ SẢN PHẨM HỎNG
     */
    @Transactional
    private PosReturnExchangeResponse processReturn(PosReturnExchange returnExchange,
                                                    PosReturnExchangeRequest request,
                                                    PosOrder order) {
        // ===== Lấy thông tin voucher nếu có =====
        BigDecimal totalDiscount = order.getPosOrderDiscountAmount();
        BigDecimal orderSubtotal = order.getPosOrderSubtotal();

        // Nếu có voucher, lấy thông tin chi tiết
        Voucher voucher = null;
        BigDecimal voucherDiscountPercent = BigDecimal.ZERO;
        BigDecimal voucherDiscountAmount = BigDecimal.ZERO;

        if (order.getVoucherId() != null) {
            voucher = voucherRepository.findById(order.getVoucherId()).orElse(null);
            if (voucher != null) {
                voucherDiscountPercent = voucher.getVoucherDiscountPercent() != null ?
                        voucher.getVoucherDiscountPercent() : BigDecimal.ZERO;
                voucherDiscountAmount = voucher.getVoucherDiscountAmount() != null ?
                        voucher.getVoucherDiscountAmount() : BigDecimal.ZERO;
            }
        }

        // ===== Lấy danh sách sản phẩm trong đơn =====
        List<PosOrderItem> allOrderItems = posOrderItemRepository.findByPosOrderItemOrderId(order.getPosOrderId());

        // ===== Tính tổng giá trị các sản phẩm trả lại =====
        List<ReturnItemDetail> returnItemDetails = new ArrayList<>();
        BigDecimal totalReturnOriginal = BigDecimal.ZERO;
        Integer totalReturnQuantity = 0;
        Integer totalOrderQuantity = 0;

        for (ReturnItemRequest itemReq : request.getReturnItems()) {
            PosOrderItem orderItem = allOrderItems.stream()
                    .filter(i -> i.getPosOrderItemId().equals(itemReq.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm trong đơn hàng"));

            BigDecimal itemUnitPrice = orderItem.getPosOrderItemUnitPrice();
            BigDecimal itemOriginalTotal = itemUnitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            totalReturnOriginal = totalReturnOriginal.add(itemOriginalTotal);
            totalReturnQuantity += itemReq.getQuantity();

            String condition = itemReq.getCondition() != null ? itemReq.getCondition() : "GOOD";

            returnItemDetails.add(new ReturnItemDetail(
                    orderItem,
                    itemReq.getQuantity(),
                    itemOriginalTotal,
                    itemUnitPrice,
                    condition
            ));
        }

        // Tính tổng số lượng sản phẩm trong đơn
        for (PosOrderItem item : allOrderItems) {
            totalOrderQuantity += item.getPosOrderItemQuantity();
        }

        // ===== TÍNH TOÁN HOÀN TIỀN THEO TỶ LỆ =====
        BigDecimal totalRefund = BigDecimal.ZERO;
        List<PosReturnExchangeItem> items = new ArrayList<>();
        List<DamagedProduct> damagedProducts = new ArrayList<>();

        boolean isReturnAllItems = (returnItemDetails.size() == allOrderItems.size()) &&
                (totalReturnQuantity.equals(totalOrderQuantity));

        for (ReturnItemDetail detail : returnItemDetails) {

            if ("GOOD".equals(detail.condition)) {
                productDetailRepository.increaseQuantity(
                        detail.orderItem.getPosOrderItemProductDetailId(),
                        detail.quantity
                );
                log.info("✅ Cộng lại kho {} sản phẩm ID: {}", detail.quantity,
                        detail.orderItem.getPosOrderItemProductDetailId());
            } else if ("DAMAGED".equals(detail.condition)) {
                DamagedProduct damagedProduct = createDamagedProduct(
                        detail.orderItem.getPosOrderItemProductDetailId(),
                        detail.quantity,
                        detail.orderItem.getPosOrderItemUnitPrice(),
                        returnExchange.getReturnExchangeId(),
                        null,
                        order.getPosOrderId(),
                        "Sản phẩm bị hỏng khi khách hàng trả lại"
                );
                damagedProducts.add(damagedProduct);
                log.info("⚠️ Thêm vào danh sách sản phẩm hỏng {} sản phẩm ID: {}", detail.quantity,
                        detail.orderItem.getPosOrderItemProductDetailId());
            }

            BigDecimal refundAmount;

            if (isReturnAllItems) {
                BigDecimal ratio = detail.originalTotal.divide(orderSubtotal, 10, RoundingMode.HALF_UP);
                BigDecimal discountedTotal = order.getPosOrderTotalAmount();
                refundAmount = discountedTotal.multiply(ratio).setScale(0, RoundingMode.HALF_UP);
                log.info("Trả toàn bộ đơn - SP: {}, Tỷ lệ: {}, Hoàn: {}",
                        detail.orderItem.getPosOrderItemId(), ratio, refundAmount);
            } else {
                BigDecimal ratio = detail.originalTotal.divide(orderSubtotal, 10, RoundingMode.HALF_UP);
                BigDecimal discountAllocated = totalDiscount.multiply(ratio).setScale(0, RoundingMode.HALF_UP);
                refundAmount = detail.originalTotal.subtract(discountAllocated);
                log.info("Trả 1 phần - SP: {}, Tỷ lệ: {}, Giảm giá phân bổ: {}, Hoàn: {}",
                        detail.orderItem.getPosOrderItemId(), ratio, discountAllocated, refundAmount);
            }

            totalRefund = totalRefund.add(refundAmount);

            PosReturnExchangeItem item = PosReturnExchangeItem.builder()
                    .returnExchangeId(returnExchange.getReturnExchangeId())
                    .orderItemId(detail.orderItem.getPosOrderItemId())
                    .productDetailId(detail.orderItem.getPosOrderItemProductDetailId())
                    .quantity(detail.quantity)
                    .unitPrice(detail.orderItem.getPosOrderItemUnitPrice())
                    .totalAmount(refundAmount)
                    .originalTotal(detail.originalTotal)
                    .discountAllocated(detail.originalTotal.subtract(refundAmount))
                    .type("RETURN")
                    .condition(detail.condition)
                    .build();

            PosReturnExchangeItem savedItem = returnExchangeItemRepository.save(item);
            items.add(savedItem);

            if ("DAMAGED".equals(detail.condition)) {
                for (DamagedProduct dp : damagedProducts) {
                    if (dp.getProductDetailId().equals(detail.orderItem.getPosOrderItemProductDetailId()) &&
                            dp.getQuantity().equals(detail.quantity)) {
                        dp.setReturnExchangeItemId(savedItem.getReturnExchangeItemId());
                        damagedProductRepository.save(dp);
                        break;
                    }
                }
            }
        }

        returnExchange.setTotalRefundAmount(totalRefund);
        returnExchange.setAdditionalPayment(BigDecimal.ZERO);
        returnExchange.setCompletedAt(LocalDateTime.now());
        returnExchange.setStatus("COMPLETED");
        returnExchangeRepository.save(returnExchange);

        order.setPosOrderStatus("REFUNDED");
        order.setPosOrderPaymentStatus("REFUNDED");
        posOrderRepository.save(order);

        return convertToResponse(returnExchange, items, order);
    }

    /**
     * Xử lý đổi sang sản phẩm mới - HỖ TRỢ NHIỀU BIẾN THỂ
     */
    @Transactional
    private PosReturnExchangeResponse processExchange(PosReturnExchange returnExchange,
                                                      PosReturnExchangeRequest request,
                                                      PosOrder oldOrder) {
        // ===== Lấy thông tin voucher nếu có =====
        BigDecimal totalDiscount = oldOrder.getPosOrderDiscountAmount();
        BigDecimal orderSubtotal = oldOrder.getPosOrderSubtotal();

        // ===== Lấy danh sách sản phẩm trong đơn =====
        List<PosOrderItem> allOrderItems = posOrderItemRepository.findByPosOrderItemOrderId(oldOrder.getPosOrderId());

        // ===== Tính tổng giá trị các sản phẩm trả lại =====
        List<ReturnItemDetail> returnItemDetails = new ArrayList<>();
        BigDecimal totalReturnOriginal = BigDecimal.ZERO;
        Integer totalReturnQuantity = 0;
        Integer totalOrderQuantity = 0;

        for (ReturnItemRequest itemReq : request.getReturnItems()) {
            PosOrderItem orderItem = allOrderItems.stream()
                    .filter(i -> i.getPosOrderItemId().equals(itemReq.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm trong đơn hàng"));

            BigDecimal itemUnitPrice = orderItem.getPosOrderItemUnitPrice();
            BigDecimal itemOriginalTotal = itemUnitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            totalReturnOriginal = totalReturnOriginal.add(itemOriginalTotal);
            totalReturnQuantity += itemReq.getQuantity();

            String condition = itemReq.getCondition() != null ? itemReq.getCondition() : "GOOD";

            returnItemDetails.add(new ReturnItemDetail(
                    orderItem,
                    itemReq.getQuantity(),
                    itemOriginalTotal,
                    itemUnitPrice,
                    condition
            ));
        }

        // Tính tổng số lượng sản phẩm trong đơn
        for (PosOrderItem item : allOrderItems) {
            totalOrderQuantity += item.getPosOrderItemQuantity();
        }

        // ===== XỬ LÝ TRẢ SẢN PHẨM CŨ =====
        BigDecimal totalRefund = BigDecimal.ZERO;
        List<PosReturnExchangeItem> items = new ArrayList<>();
        List<DamagedProduct> damagedProducts = new ArrayList<>();

        boolean isReturnAllItems = (returnItemDetails.size() == allOrderItems.size()) &&
                (totalReturnQuantity.equals(totalOrderQuantity));

        for (ReturnItemDetail detail : returnItemDetails) {

            if ("GOOD".equals(detail.condition)) {
                productDetailRepository.increaseQuantity(
                        detail.orderItem.getPosOrderItemProductDetailId(),
                        detail.quantity
                );
                log.info("✅ Cộng lại kho {} sản phẩm ID: {}", detail.quantity,
                        detail.orderItem.getPosOrderItemProductDetailId());
            } else if ("DAMAGED".equals(detail.condition)) {
                DamagedProduct damagedProduct = createDamagedProduct(
                        detail.orderItem.getPosOrderItemProductDetailId(),
                        detail.quantity,
                        detail.orderItem.getPosOrderItemUnitPrice(),
                        returnExchange.getReturnExchangeId(),
                        null,
                        oldOrder.getPosOrderId(),
                        "Sản phẩm bị hỏng khi khách hàng đổi hàng"
                );
                damagedProducts.add(damagedProduct);
                log.info("⚠️ Thêm vào danh sách sản phẩm hỏng {} sản phẩm ID: {}", detail.quantity,
                        detail.orderItem.getPosOrderItemProductDetailId());
            }

            BigDecimal refundAmount;

            if (isReturnAllItems) {
                BigDecimal ratio = detail.originalTotal.divide(orderSubtotal, 10, RoundingMode.HALF_UP);
                BigDecimal discountedTotal = oldOrder.getPosOrderTotalAmount();
                refundAmount = discountedTotal.multiply(ratio).setScale(0, RoundingMode.HALF_UP);
            } else {
                BigDecimal ratio = detail.originalTotal.divide(orderSubtotal, 10, RoundingMode.HALF_UP);
                BigDecimal discountAllocated = totalDiscount.multiply(ratio).setScale(0, RoundingMode.HALF_UP);
                refundAmount = detail.originalTotal.subtract(discountAllocated);
            }

            totalRefund = totalRefund.add(refundAmount);

            PosReturnExchangeItem item = PosReturnExchangeItem.builder()
                    .returnExchangeId(returnExchange.getReturnExchangeId())
                    .orderItemId(detail.orderItem.getPosOrderItemId())
                    .productDetailId(detail.orderItem.getPosOrderItemProductDetailId())
                    .quantity(detail.quantity)
                    .unitPrice(detail.orderItem.getPosOrderItemUnitPrice())
                    .totalAmount(refundAmount)
                    .originalTotal(detail.originalTotal)
                    .discountAllocated(detail.originalTotal.subtract(refundAmount))
                    .type("RETURN")
                    .condition(detail.condition)
                    .build();

            PosReturnExchangeItem savedItem = returnExchangeItemRepository.save(item);
            items.add(savedItem);

            if ("DAMAGED".equals(detail.condition)) {
                for (DamagedProduct dp : damagedProducts) {
                    if (dp.getProductDetailId().equals(detail.orderItem.getPosOrderItemProductDetailId()) &&
                            dp.getQuantity().equals(detail.quantity)) {
                        dp.setReturnExchangeItemId(savedItem.getReturnExchangeItemId());
                        damagedProductRepository.save(dp);
                        break;
                    }
                }
            }
        }

        // ============================================================
        // ✅ XỬ LÝ ĐỔI SANG SẢN PHẨM MỚI - HỖ TRỢ NHIỀU BIẾN THỂ
        // ============================================================
        BigDecimal totalAdditional = BigDecimal.ZERO;

        for (ExchangeItemRequest exchangeReq : request.getExchangeItems()) {
            // Kiểm tra sản phẩm mới có tồn tại không
            Integer currentStock = productDetailRepository.getQuantityById(exchangeReq.getNewProductDetailId());
            if (currentStock == null || currentStock < exchangeReq.getQuantity()) {
                throw new BusinessException("Sản phẩm mới không đủ tồn kho. ID: " + exchangeReq.getNewProductDetailId());
            }

            // Lấy thông tin sản phẩm mới để lấy giá
            ProductDetail newProduct = productDetailRepository.findById(exchangeReq.getNewProductDetailId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm mới"));

            // Trừ tồn kho sản phẩm mới
            productDetailRepository.decreaseQuantity(
                    exchangeReq.getNewProductDetailId(),
                    exchangeReq.getQuantity()
            );

            BigDecimal itemTotal = exchangeReq.getUnitPrice().multiply(BigDecimal.valueOf(exchangeReq.getQuantity()));
            totalAdditional = totalAdditional.add(itemTotal);

            // Tạo PosReturnExchangeItem cho sản phẩm đổi
            PosReturnExchangeItem exchangeItem = PosReturnExchangeItem.builder()
                    .returnExchangeId(returnExchange.getReturnExchangeId())
                    .productDetailId(exchangeReq.getOldProductDetailId() != null ?
                            exchangeReq.getOldProductDetailId() : exchangeReq.getNewProductDetailId())
                    .quantity(exchangeReq.getQuantity())
                    .unitPrice(exchangeReq.getUnitPrice())
                    .totalAmount(itemTotal)
                    .type("EXCHANGE")
                    .newProductDetailId(exchangeReq.getNewProductDetailId())
                    .newQuantity(exchangeReq.getQuantity())
                    .build();

            items.add(returnExchangeItemRepository.save(exchangeItem));

            log.info("🔄 Đổi sản phẩm: OldId={}, NewId={}, Quantity={}, Price={}",
                    exchangeReq.getOldProductDetailId(),
                    exchangeReq.getNewProductDetailId(),
                    exchangeReq.getQuantity(),
                    exchangeReq.getUnitPrice());
        }

        // ===== TÍNH TOÁN CHÊNH LỆCH =====
        BigDecimal netAmount = totalAdditional.subtract(totalRefund);
        BigDecimal refundAmount = BigDecimal.ZERO;
        BigDecimal additionalPayment = BigDecimal.ZERO;

        if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
            additionalPayment = netAmount;
        } else if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = netAmount.abs();
        }

        // ===== CẬP NHẬT PHIẾU =====
        returnExchange.setTotalRefundAmount(refundAmount);
        returnExchange.setAdditionalPayment(additionalPayment);
        returnExchange.setCompletedAt(LocalDateTime.now());
        returnExchange.setStatus("COMPLETED");
        returnExchangeRepository.save(returnExchange);

        // ===== CẬP NHẬT ĐƠN HÀNG CŨ =====
        oldOrder.setPosOrderStatus("EXCHANGED");
        posOrderRepository.save(oldOrder);

        // ===== TẠO ĐƠN HÀNG MỚI (nếu có đổi sp) =====
        if (!request.getExchangeItems().isEmpty()) {
            PosOrder newOrder = createExchangeOrder(oldOrder, request, additionalPayment, refundAmount);
            oldOrder.setExchangeOrderId(newOrder.getPosOrderId());
            posOrderRepository.save(oldOrder);
        }

        return convertToResponse(returnExchange, items, oldOrder);
    }

    /**
     * Tạo đơn hàng mới cho sản phẩm đổi
     */
    private PosOrder createExchangeOrder(PosOrder oldOrder,
                                         PosReturnExchangeRequest request,
                                         BigDecimal additionalPayment,
                                         BigDecimal refundAmount) {
        PosOrder newOrder = PosOrder.builder()
                .posOrderSessionId(oldOrder.getPosOrderSessionId())
                .posOrderCustomerId(oldOrder.getPosOrderCustomerId())
                .posOrderCustomerName(oldOrder.getPosOrderCustomerName())
                .posOrderCustomerPhone(oldOrder.getPosOrderCustomerPhone())
                .posOrderInvoiceNumber(generateInvoiceNumber())
                .posOrderType("EXCHANGE")
                .posOrderSubtotal(additionalPayment.add(refundAmount))
                .posOrderDiscountAmount(BigDecimal.ZERO)
                .posOrderTaxAmount(BigDecimal.ZERO)
                .posOrderTotalAmount(additionalPayment)
                .posOrderPaidAmount(additionalPayment)
                .posOrderStatus("COMPLETED")
                .posOrderPaymentStatus(additionalPayment.compareTo(BigDecimal.ZERO) > 0 ? "PAID" : "REFUNDED")
                .posOrderNotes("Đổi từ đơn: " + oldOrder.getPosOrderInvoiceNumber())
                .parentOrderId(oldOrder.getPosOrderId())
                .build();

        PosOrder savedNewOrder = posOrderRepository.save(newOrder);

        // Thêm sản phẩm mới vào đơn hàng mới
        for (ExchangeItemRequest itemReq : request.getExchangeItems()) {
            // Lấy thông tin sản phẩm mới
            ProductDetail newProduct = productDetailRepository.findById(itemReq.getNewProductDetailId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm mới"));

            // Lấy thông tin sản phẩm cũ (nếu có)
            String oldProductInfo = "";
            if (itemReq.getOldProductDetailId() != null) {
                ProductDetail oldProduct = productDetailRepository.findById(itemReq.getOldProductDetailId()).orElse(null);
                if (oldProduct != null && oldProduct.getProduct() != null) {
                    oldProductInfo = " (Thay thế: " + oldProduct.getProduct().getProductName() + ")";
                }
            }

            PosOrderItem newItem = PosOrderItem.builder()
                    .posOrderItemOrderId(savedNewOrder.getPosOrderId())
                    .posOrderItemProductDetailId(itemReq.getNewProductDetailId())
                    .posOrderItemQuantity(itemReq.getQuantity())
                    .posOrderItemUnitPrice(itemReq.getUnitPrice())
                    .posOrderItemDiscount(BigDecimal.ZERO)
                    .posOrderItemNotes("Đổi từ đơn hàng #" + oldOrder.getPosOrderInvoiceNumber() + oldProductInfo)
                    .build();
            posOrderItemRepository.save(newItem);

            log.info("📦 Thêm sản phẩm mới vào đơn hàng đổi: ProductId={}, Quantity={}, Price={}",
                    itemReq.getNewProductDetailId(),
                    itemReq.getQuantity(),
                    itemReq.getUnitPrice());
        }

        if (additionalPayment.compareTo(BigDecimal.ZERO) > 0) {
            PosPayment payment = PosPayment.builder()
                    .posPaymentOrderId(savedNewOrder.getPosOrderId())
                    .posPaymentMethodId(1)
                    .posPaymentAmount(additionalPayment)
                    .posPaymentStatus("COMPLETED")
                    .posPaymentProcessedAt(LocalDateTime.now())
                    .build();
            posPaymentRepository.save(payment);
            log.info("💳 Tạo thanh toán thêm cho đơn hàng đổi: {}", additionalPayment);
        }

        return savedNewOrder;
    }

    // ============================================================
    // ✅ PHƯƠNG THỨC XỬ LÝ SẢN PHẨM HỎNG
    // ============================================================

    private DamagedProduct createDamagedProduct(Integer productDetailId,
                                                Integer quantity,
                                                BigDecimal unitPrice,
                                                Integer returnExchangeId,
                                                Integer returnExchangeItemId,
                                                Integer orderId,
                                                String reason) {

        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        DamagedProduct damagedProduct = DamagedProduct.builder()
                .productDetailId(productDetailId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .returnExchangeId(returnExchangeId)
                .returnExchangeItemId(returnExchangeItemId)
                .orderId(orderId)
                .reason(reason)
                .status("PENDING")
                .notes("Sản phẩm bị hỏng khi khách hàng trả/đổi hàng")
                .build();

        return damagedProductRepository.save(damagedProduct);
    }

    public List<DamagedProduct> getAllDamagedProducts() {
        return damagedProductRepository.findAll();
    }

    public List<DamagedProduct> getDamagedProductsByStatus(String status) {
        return damagedProductRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<DamagedProduct> getPendingDamagedProducts() {
        return damagedProductRepository.findPendingDamagedProducts();
    }

    public DamagedProduct getDamagedProductById(Integer damagedProductId) {
        return damagedProductRepository.findById(damagedProductId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm hỏng với ID: " + damagedProductId));
    }

    @Transactional
    public DamagedProduct updateDamagedProductStatus(Integer damagedProductId,
                                                     String status,
                                                     String notes) {
        DamagedProduct damagedProduct = damagedProductRepository.findById(damagedProductId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm hỏng"));

        if (!isValidStatus(status)) {
            throw new BusinessException("Trạng thái không hợp lệ. Chấp nhận: PENDING, PROCESSED, DISCARDED, REPAIRED");
        }

        if (!"PENDING".equals(damagedProduct.getStatus()) && !"PENDING".equals(status)) {
            throw new BusinessException("Sản phẩm này đã được xử lý trước đó");
        }

        String oldStatus = damagedProduct.getStatus();
        damagedProduct.setStatus(status);
        damagedProduct.setProcessedAt(LocalDateTime.now());

        Integer processedBy = getCurrentEmployeeId();
        damagedProduct.setProcessedBy(processedBy);

        if (notes != null && !notes.isEmpty()) {
            String currentNotes = damagedProduct.getNotes();
            damagedProduct.setNotes(currentNotes + " | " + notes);
        }

        DamagedProduct updated = damagedProductRepository.save(damagedProduct);
        log.info("📦 Cập nhật trạng thái sản phẩm hỏng ID={}: {} -> {}",
                damagedProductId, oldStatus, status);

        return updated;
    }

    @Transactional
    public List<DamagedProduct> batchUpdateDamagedProducts(List<Integer> damagedProductIds,
                                                           String status,
                                                           String notes) {
        List<DamagedProduct> updatedProducts = new ArrayList<>();

        for (Integer id : damagedProductIds) {
            try {
                DamagedProduct updated = updateDamagedProductStatus(id, status, notes);
                updatedProducts.add(updated);
            } catch (Exception e) {
                log.error("❌ Lỗi khi cập nhật sản phẩm hỏng ID {}: {}", id, e.getMessage());
                throw new BusinessException("Lỗi khi cập nhật sản phẩm hỏng ID " + id + ": " + e.getMessage());
            }
        }

        return updatedProducts;
    }

    private boolean isValidStatus(String status) {
        return status != null && (
                "PENDING".equals(status) ||
                        "PROCESSED".equals(status) ||
                        "DISCARDED".equals(status) ||
                        "REPAIRED".equals(status)
        );
    }

    public DamagedProductStatsResponse getDamagedProductStats() {
        long pending = damagedProductRepository.countPendingDamagedProducts();
        long total = damagedProductRepository.count();

        return DamagedProductStatsResponse.builder()
                .totalDamagedProducts(total)
                .pendingDamagedProducts(pending)
                .processedDamagedProducts(total - pending)
                .build();
    }

    // ============================================================
    // QUẢN LÝ PHIẾU ĐỔI/TRẢ
    // ============================================================

    /**
     * Lấy tất cả phiếu đổi/trả
     */
    public List<PosReturnExchangeResponse> getAllReturnExchanges() {
        List<PosReturnExchange> returnExchanges = returnExchangeRepository.findAll();
        return returnExchanges.stream()
                .map(re -> {
                    List<PosReturnExchangeItem> items = returnExchangeItemRepository.findByReturnExchangeId(re.getReturnExchangeId());
                    PosOrder order = posOrderRepository.findById(re.getOrderId()).orElse(null);
                    return convertToResponse(re, items, order);
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết phiếu đổi/trả theo ID
     */
    public PosReturnExchangeResponse getReturnExchangeDetail(Integer id) {
        PosReturnExchange returnExchange = returnExchangeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu đổi/trả"));

        List<PosReturnExchangeItem> items = returnExchangeItemRepository.findByReturnExchangeId(id);
        PosOrder order = posOrderRepository.findById(returnExchange.getOrderId()).orElse(null);

        return convertToResponse(returnExchange, items, order);
    }

    // ============================================================
    // CÁC METHOD HỖ TRỢ KHÁC
    // ============================================================

    private String generateReturnExchangeCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = returnExchangeRepository.count() + 1;
        return "RE-" + datePart + "-" + String.format("%06d", count);
    }

    private String generateInvoiceNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = posOrderRepository.count() + 1;
        return "POS-EXC-" + datePart + "-" + String.format("%06d", count);
    }

    // ============================================================
    // CONVERT METHODS
    // ============================================================

    private PosOrderResponse convertOrderToResponse(PosOrder order) {
        PosOrderResponse response = new PosOrderResponse();
        response.setPosOrderId(order.getPosOrderId());
        response.setPosOrderInvoiceNumber(order.getPosOrderInvoiceNumber());
        response.setPosOrderCustomerName(order.getPosOrderCustomerName());
        response.setPosOrderCustomerPhone(order.getPosOrderCustomerPhone());
        response.setPosOrderTotalAmount(order.getPosOrderTotalAmount());
        response.setPosOrderStatus(order.getPosOrderStatus());
        response.setPosOrderPaymentStatus(order.getPosOrderPaymentStatus());
        response.setPosOrderCreatedAt(order.getPosOrderCreatedAt());
        return response;
    }

    private PosReturnExchangeResponse convertToResponse(PosReturnExchange returnExchange,
                                                        List<PosReturnExchangeItem> items,
                                                        PosOrder order) {
        PosReturnExchangeResponse response = new PosReturnExchangeResponse();
        response.setReturnExchangeId(returnExchange.getReturnExchangeId());
        response.setOrderId(returnExchange.getOrderId());
        response.setOrderInvoiceNumber(order != null ? order.getPosOrderInvoiceNumber() : "N/A");
        response.setCustomerName(returnExchange.getCustomerName());
        response.setCustomerPhone(returnExchange.getCustomerPhone());
        response.setType(returnExchange.getType());
        response.setTotalRefundAmount(returnExchange.getTotalRefundAmount());
        response.setAdditionalPayment(returnExchange.getAdditionalPayment());
        response.setReason(returnExchange.getReason());
        response.setStatus(returnExchange.getStatus());
        response.setReferenceCode(returnExchange.getReferenceCode());
        response.setNotes(returnExchange.getNotes());
        response.setCreatedAt(returnExchange.getCreatedAt());
        response.setCompletedAt(returnExchange.getCompletedAt());
        response.setItems(items.stream().map(this::convertItemToResponse).collect(Collectors.toList()));
        return response;
    }

    private ReturnExchangeItemResponse convertItemToResponse(PosReturnExchangeItem item) {
        ReturnExchangeItemResponse response = new ReturnExchangeItemResponse();
        response.setItemId(item.getReturnExchangeItemId());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setTotalAmount(item.getTotalAmount());
        response.setOriginalTotal(item.getOriginalTotal());
        response.setDiscountAllocated(item.getDiscountAllocated());
        response.setType(item.getType());
        response.setNewQuantity(item.getNewQuantity());
        response.setCondition(item.getCondition());

        ProductDetail productDetail = productDetailRepository.findById(item.getProductDetailId()).orElse(null);
        if (productDetail != null) {
            response.setProductName(productDetail.getProduct() != null ?
                    productDetail.getProduct().getProductName() : "Sản phẩm");
            response.setColorName(productDetail.getColor() != null ?
                    productDetail.getColor().getColorName() : "");
            response.setSizeName(productDetail.getSize() != null ?
                    productDetail.getSize().getSizeName() : "");
        }

        if ("EXCHANGE".equals(item.getType()) && item.getNewProductDetailId() != null) {
            ProductDetail newProduct = productDetailRepository.findById(item.getNewProductDetailId()).orElse(null);
            if (newProduct != null) {
                response.setNewProductName(newProduct.getProduct() != null ?
                        newProduct.getProduct().getProductName() : "Sản phẩm mới");
            }
        }

        return response;
    }

    // ============================================================
    // INNER CLASS
    // ============================================================

    private static class ReturnItemDetail {
        PosOrderItem orderItem;
        Integer quantity;
        BigDecimal originalTotal;
        BigDecimal unitPrice;
        BigDecimal discountAllocated = BigDecimal.ZERO;
        String condition;

        ReturnItemDetail(PosOrderItem orderItem, Integer quantity,
                         BigDecimal originalTotal, BigDecimal unitPrice,
                         String condition) {
            this.orderItem = orderItem;
            this.quantity = quantity;
            this.originalTotal = originalTotal;
            this.unitPrice = unitPrice;
            this.condition = condition;
        }
    }

    // PosReturnExchangeService.java - Thêm các phương thức sau

// ============================================================
// QUẢN LÝ TRẠNG THÁI PHIẾU ĐỔI/TRẢ
// ============================================================

    /**
     * Cập nhật trạng thái phiếu đổi/trả
     */
    @Transactional
    public PosReturnExchangeResponse updateReturnExchangeStatus(Integer id, String status) {
        PosReturnExchange returnExchange = returnExchangeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu đổi/trả"));

        // Kiểm tra trạng thái hợp lệ
        if (!isValidReturnExchangeStatus(status)) {
            throw new BusinessException("Trạng thái không hợp lệ. Chấp nhận: PENDING, COMPLETED, CANCELLED");
        }

        // Nếu đã hoàn tất hoặc đã hủy thì không cho cập nhật lại
        if ("COMPLETED".equals(returnExchange.getStatus()) || "CANCELLED".equals(returnExchange.getStatus())) {
            throw new BusinessException("Phiếu đã " + returnExchange.getStatus() + ", không thể thay đổi trạng thái");
        }

        String oldStatus = returnExchange.getStatus();
        returnExchange.setStatus(status);

        if ("COMPLETED".equals(status)) {
            returnExchange.setCompletedAt(LocalDateTime.now());
        }

        returnExchange.setUpdatedAt(LocalDateTime.now());

        PosReturnExchange updated = returnExchangeRepository.save(returnExchange);
        log.info("📋 Cập nhật trạng thái phiếu đổi/trả ID={}: {} -> {}", id, oldStatus, status);

        // Lấy thông tin để trả về response
        List<PosReturnExchangeItem> items = returnExchangeItemRepository.findByReturnExchangeId(id);
        PosOrder order = posOrderRepository.findById(returnExchange.getOrderId()).orElse(null);

        return convertToResponse(updated, items, order);
    }

    /**
     * Hủy phiếu đổi/trả
     */
    @Transactional
    public void cancelReturnExchange(Integer id) {
        PosReturnExchange returnExchange = returnExchangeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu đổi/trả"));

        if ("COMPLETED".equals(returnExchange.getStatus())) {
            throw new BusinessException("Phiếu đã hoàn tất, không thể hủy");
        }

        if ("CANCELLED".equals(returnExchange.getStatus())) {
            throw new BusinessException("Phiếu đã bị hủy trước đó");
        }

        // Cập nhật trạng thái
        returnExchange.setStatus("CANCELLED");
        returnExchange.setUpdatedAt(LocalDateTime.now());
        returnExchangeRepository.save(returnExchange);

        log.info("📋 Hủy phiếu đổi/trả ID={}", id);
    }

    /**
     * Kiểm tra trạng thái phiếu đổi/trả hợp lệ
     */
    private boolean isValidReturnExchangeStatus(String status) {
        return status != null && (
                "PENDING".equals(status) ||
                        "COMPLETED".equals(status) ||
                        "CANCELLED".equals(status)
        );
    }
}