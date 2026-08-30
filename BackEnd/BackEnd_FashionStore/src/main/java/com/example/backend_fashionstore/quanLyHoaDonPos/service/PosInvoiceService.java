package com.example.backend_fashionstore.quanLyHoaDonPos.service;

import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.entity.PosOrder;
import com.example.backend_fashionstore.entity.PosOrderItem;
import com.example.backend_fashionstore.entity.PosPayment;
import com.example.backend_fashionstore.entity.PosSession;
import com.example.backend_fashionstore.entity.PaymentMethod;
import com.example.backend_fashionstore.payment.repository.PaymentMethodRepository;
import com.example.backend_fashionstore.entity.Voucher;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosOrderItemRepository;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosOrderRepository;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosPaymentRepository;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosSessionRepository;
import com.example.backend_fashionstore.quanLyHoaDonPos.dto.*;
import com.example.backend_fashionstore.quanLyHoaDonPos.repository.PosInvoiceRepository;
import com.example.backend_fashionstore.quanLyVoucher.dto.VoucherDTO;
import com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PosInvoiceService {

    private final PosOrderRepository posOrderRepository;
    private final PosOrderItemRepository posOrderItemRepository;
    private final PosPaymentRepository posPaymentRepository;
    private final PosSessionRepository posSessionRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductDetailRepository productDetailRepository;
    private final PosInvoiceRepository posInvoiceRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final VoucherRepository voucherRepository; // ✅ Thêm VoucherRepository
    // =====================================================
    // TÌM KIẾM HÓA ĐƠN
    // =====================================================
    @Transactional(readOnly = true)
    public Page<PosInvoiceDTO> searchInvoices(PosInvoiceFilterDTO filter) {

        LocalDateTime startDate = filter.getStartDate() != null
                ? filter.getStartDate().atStartOfDay()
                : LocalDate.now().minusDays(30).atStartOfDay();

        LocalDateTime endDate = filter.getEndDate() != null
                ? filter.getEndDate().atTime(LocalTime.MAX)
                : LocalDate.now().atTime(LocalTime.MAX);

        String sortByColumn = "posOrderCreatedAt";
        if (filter.getSortBy() != null) {
            switch (filter.getSortBy()) {
                case "posOrderCreatedAt":
                case "createdAt":
                    sortByColumn = "posOrderCreatedAt";
                    break;
                case "invoiceNumber":
                    sortByColumn = "posOrderInvoiceNumber";
                    break;
                case "total":
                    sortByColumn = "posOrderTotalAmount";
                    break;
                default:
                    sortByColumn = filter.getSortBy();
            }
        }

        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDir() != null ? filter.getSortDir() : "DESC"), sortByColumn);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        String invoiceNumber = filter.getInvoiceNumber() != null && !filter.getInvoiceNumber().trim().isEmpty() ? filter.getInvoiceNumber().trim() : null;
        String customerName = filter.getCustomerName() != null && !filter.getCustomerName().trim().isEmpty() ? filter.getCustomerName().trim() : null;
        String customerPhone = filter.getCustomerPhone() != null && !filter.getCustomerPhone().trim().isEmpty() ? filter.getCustomerPhone().trim() : null;
        String orderStatus = filter.getOrderStatus() != null && !filter.getOrderStatus().trim().isEmpty() ? filter.getOrderStatus().trim() : null;
        String paymentStatus = filter.getPaymentStatus() != null && !filter.getPaymentStatus().trim().isEmpty() ? filter.getPaymentStatus().trim() : null;

        Page<PosOrder> orders = posOrderRepository.searchInvoices(
                invoiceNumber,
                customerName,
                customerPhone,
                orderStatus,
                paymentStatus,
                filter.getPosSessionId(),
                startDate,
                endDate,
                pageable
        );

        return orders.map(this::convertToDTO);
    }

    // =====================================================
    // LẤY CHI TIẾT HÓA ĐƠN
    // =====================================================
    @Transactional(readOnly = true)
    public PosInvoiceDTO getInvoiceDetail(Integer orderId) {
        PosOrder order = posOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        return convertToDTO(order);
    }

    @Transactional(readOnly = true)
    public PosInvoiceDTO getInvoiceByNumber(String invoiceNumber) {
        PosOrder order = posOrderRepository.findByPosOrderInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn với mã: " + invoiceNumber));
        return convertToDTO(order);
    }

    // =====================================================
    // LẤY THÔNG TIN VOUCHER CỦA ĐƠN HÀNG
    // =====================================================
    @Transactional(readOnly = true)
    public VoucherDTO getVoucherByOrderId(Integer orderId) {
        log.info("📋 Getting voucher for order: {}", orderId);

        PosOrder order = posOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Lấy voucherId từ order
        Integer voucherId = order.getVoucherId();
        if (voucherId == null) {
            throw new RuntimeException("Đơn hàng không áp dụng mã giảm giá");
        }

        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));

        return convertVoucherToDTO(voucher);
    }

    // =====================================================
    // HỦY HÓA ĐƠN
    // =====================================================
    @Transactional
    public void cancelInvoice(Integer orderId, String reason) {
        PosOrder order = posOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        if ("CANCELLED".equals(order.getPosOrderStatus())) {
            throw new RuntimeException("Hóa đơn đã bị hủy trước đó");
        }

        if ("REFUNDED".equals(order.getPosOrderStatus())) {
            throw new RuntimeException("Hóa đơn đã được hoàn tiền, không thể hủy");
        }

        List<PosOrderItem> items = posOrderItemRepository.findByPosOrderItemOrderId(orderId);
        for (PosOrderItem item : items) {
            productDetailRepository.increaseQuantity(
                    item.getPosOrderItemProductDetailId(),
                    item.getPosOrderItemQuantity()
            );
        }

        order.setPosOrderStatus("CANCELLED");
        order.setPosOrderPaymentStatus("CANCELLED");
        order.setPosOrderNotes((order.getPosOrderNotes() != null ? order.getPosOrderNotes() + " | " : "") + "Hủy đơn: " + reason);
        posOrderRepository.save(order);

        updateSessionOnCancel(order);
    }

    // =====================================================
    // THỐNG KÊ HÓA ĐƠN
    // =====================================================
    @Transactional(readOnly = true)
    public PosInvoiceStatsDTO getStatistics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        BigDecimal totalRevenue = posOrderRepository.getTotalRevenue(start, end);
        Long totalOrders = posOrderRepository.getTotalOrders(start, end);
        Long totalCustomers = posOrderRepository.getTotalCustomers(start, end);

        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        return PosInvoiceStatsDTO.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .avgOrderValue(avgOrderValue)
                .totalCustomers(totalCustomers)
                .totalDiscount(totalDiscount)
                .totalTax(totalTax)
                .fromDate(startDate != null ? startDate : LocalDate.now().minusDays(30))
                .toDate(endDate != null ? endDate : LocalDate.now())
                .build();
    }

    // =====================================================
    // IN LẠI HÓA ĐƠN
    // =====================================================
    @Transactional(readOnly = true)
    public PosInvoiceDTO getInvoiceForPrinting(Integer orderId) {
        return getInvoiceDetail(orderId);
    }

    // =====================================================
    // PRIVATE METHODS
    // =====================================================

    private void updateSessionOnCancel(PosOrder order) {
        if (order.getPosOrderSessionId() == null) return;
        PosSession session = posSessionRepository.findById(order.getPosOrderSessionId()).orElse(null);
        if (session != null) {
            BigDecimal subtotal = order.getPosOrderSubtotal() != null ? order.getPosOrderSubtotal() : BigDecimal.ZERO;
            BigDecimal discount = order.getPosOrderDiscountAmount() != null ? order.getPosOrderDiscountAmount() : BigDecimal.ZERO;
            BigDecimal tax = order.getPosOrderTaxAmount() != null ? order.getPosOrderTaxAmount() : BigDecimal.ZERO;
            BigDecimal orderTotal = subtotal.subtract(discount).add(tax);

            BigDecimal newCashSales = session.getPosSessionCashSales().subtract(orderTotal);
            if (newCashSales.compareTo(BigDecimal.ZERO) < 0) newCashSales = BigDecimal.ZERO;
            session.setPosSessionCashSales(newCashSales);
            posSessionRepository.save(session);
        }
    }

    private PosInvoiceDTO convertToDTO(PosOrder order) {
        PosInvoiceDTO dto = new PosInvoiceDTO();
        dto.setPosOrderId(order.getPosOrderId());
        dto.setInvoiceNumber(order.getPosOrderInvoiceNumber());
        dto.setOrderType(order.getPosOrderType());
        dto.setCustomerName(order.getPosOrderCustomerName());
        dto.setCustomerPhone(order.getPosOrderCustomerPhone());

        BigDecimal subtotal = order.getPosOrderSubtotal() != null ? order.getPosOrderSubtotal() : BigDecimal.ZERO;
        BigDecimal discount = order.getPosOrderDiscountAmount() != null ? order.getPosOrderDiscountAmount() : BigDecimal.ZERO;
        BigDecimal tax = order.getPosOrderTaxAmount() != null ? order.getPosOrderTaxAmount() : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount).add(tax);

        dto.setSubtotal(subtotal);
        dto.setDiscount(discount);
        dto.setTax(tax);
        dto.setTotal(total);
        dto.setPaidAmount(order.getPosOrderPaidAmount() != null ? order.getPosOrderPaidAmount() : BigDecimal.ZERO);
        dto.setChangeAmount(order.getPosOrderChangeAmount());
        dto.setOrderStatus(order.getPosOrderStatus());
        dto.setPaymentStatus(order.getPosOrderPaymentStatus());
        dto.setCreatedAt(order.getPosOrderCreatedAt());

        // ✅ Lấy voucherCode từ voucherId
        if (order.getVoucherId() != null) {
            voucherRepository.findById(order.getVoucherId()).ifPresent(voucher -> {
                dto.setVoucherCode(voucher.getVoucherCode());
            });
        }

        // Lấy thông tin nhân viên thu ngân
        if (order.getPosOrderSessionId() != null) {
            posSessionRepository.findById(order.getPosOrderSessionId()).ifPresent(session -> {
                dto.setCashierId(session.getPosSessionEmployeeId());
                employeeRepository.findById(session.getPosSessionEmployeeId()).ifPresent(emp -> {
                    dto.setCashierName(emp.getEmployeeFullname());
                });
            });
        }

        // Lấy items
        List<PosOrderItem> items = posOrderItemRepository.findByPosOrderItemOrderId(order.getPosOrderId());
        dto.setItems(items.stream().map(this::convertItemToDTO).collect(Collectors.toList()));

        // Lấy payments — chỉ lấy COMPLETED
        List<PosPayment> payments = posPaymentRepository.findByPosPaymentOrderId(order.getPosOrderId())
                .stream()
                .filter(p -> "COMPLETED".equals(p.getPosPaymentStatus()))
                .collect(Collectors.toList());
        dto.setPayments(payments.stream().map(this::convertPaymentToDTO).collect(Collectors.toList()));

        return dto;
    }

    private PosInvoiceItemDTO convertItemToDTO(PosOrderItem item) {
        PosInvoiceItemDTO dto = new PosInvoiceItemDTO();
        dto.setProductDetailId(item.getPosOrderItemProductDetailId());
        dto.setQuantity(item.getPosOrderItemQuantity());
        dto.setUnitPrice(item.getPosOrderItemUnitPrice());
        dto.setDiscount(item.getPosOrderItemDiscount());
        dto.setTotal(item.getPosOrderItemTotal());

        productDetailRepository.findByIdWithRelations(item.getPosOrderItemProductDetailId())
                .ifPresent(pd -> {
                    dto.setProductName(pd.getProduct() != null ? pd.getProduct().getProductName() : null);
                    dto.setColor(pd.getColor() != null ? pd.getColor().getColorName() : null);
                    dto.setSize(pd.getSize() != null ? pd.getSize().getSizeName() : null);
                    dto.setSku(pd.getSku());
                });

        return dto;
    }

    private PosInvoicePaymentDTO convertPaymentToDTO(PosPayment payment) {
        PosInvoicePaymentDTO dto = new PosInvoicePaymentDTO();
        dto.setAmount(payment.getPosPaymentAmount());
        dto.setReferenceNumber(payment.getPosPaymentReferenceNumber());
        dto.setProcessedAt(payment.getPosPaymentProcessedAt());

        String methodName = paymentMethodRepository.findById(Long.valueOf(payment.getPosPaymentMethodId()))
                .map(PaymentMethod::getMethodName)
                .orElse(null);
        if ("CASH".equalsIgnoreCase(methodName)) {
            dto.setPaymentMethod("Tiền mặt");
        } else if ("VNPAY".equalsIgnoreCase(methodName)) {
            dto.setPaymentMethod("VNPay");
        } else if ("BANK_TRANSFER".equalsIgnoreCase(methodName)) {
            dto.setPaymentMethod("Chuyển khoản");
        } else {
            dto.setPaymentMethod(methodName != null ? methodName : "Khác");
        }

        return dto;
    }

    // ✅ Chuyển đổi Voucher entity sang VoucherDTO
    private VoucherDTO convertVoucherToDTO(Voucher voucher) {
        VoucherDTO dto = new VoucherDTO();
        dto.setVoucherId(voucher.getVoucherId());
        dto.setVoucherCode(voucher.getVoucherCode());
        dto.setVoucherDiscountAmount(voucher.getVoucherDiscountAmount());
        dto.setVoucherDiscountPercent(voucher.getVoucherDiscountPercent());
        dto.setVoucherStartDate(voucher.getVoucherStartDate());
        dto.setVoucherEndDate(voucher.getVoucherEndDate());
        dto.setVoucherMaxUsage(voucher.getVoucherMaxUsage());
        dto.setVoucherUsedCount(voucher.getVoucherUsedCount());
        dto.setVoucherMinOrderValue(voucher.getVoucherMinOrderValue());
        dto.setVoucherStatus(voucher.getVoucherStatus());
        dto.setVoucherCreatedAt(voucher.getVoucherCreatedAt());
        dto.setVoucherUpdatedAt(voucher.getVoucherUpdatedAt());
        dto.setVoucherDeletedAt(voucher.getVoucherDeletedAt());
        return dto;
    }
}
