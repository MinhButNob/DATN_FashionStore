package com.example.backend_fashionstore.quanLyHoaDonPos.service;


import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.entity.PosOrder;
import com.example.backend_fashionstore.entity.PosOrderItem;
import com.example.backend_fashionstore.entity.PosPayment;
import com.example.backend_fashionstore.entity.PosSession;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosOrderItemRepository;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosOrderRepository;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosPaymentRepository;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosSessionRepository;
import com.example.backend_fashionstore.quanLyHoaDonPos.dto.*;

import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class PosInvoiceService {

    private final PosOrderRepository posOrderRepository;
    private final PosOrderItemRepository posOrderItemRepository;
    private final PosPaymentRepository posPaymentRepository;
    private final PosSessionRepository posSessionRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductDetailRepository productDetailRepository;

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
                : LocalDateTime.now();

        String sortDir = filter.getSortDir() != null ? filter.getSortDir() : "DESC";
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "posOrderCreatedAt";
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        int page = (filter.getPage() != null && filter.getPage() >= 0) ? filter.getPage() : 0;
        int size = (filter.getSize() != null && filter.getSize() > 0) ? filter.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PosOrder> orders = posOrderRepository.searchInvoices(
                filter.getInvoiceNumber(),
                filter.getCustomerName(),
                filter.getCustomerPhone(),
                filter.getOrderStatus(),
                filter.getPaymentStatus(),
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

        // Cộng lại tồn kho
        List<PosOrderItem> items = posOrderItemRepository.findByPosOrderItemOrderId(orderId);
        for (PosOrderItem item : items) {
            productDetailRepository.increaseQuantity(
                    item.getPosOrderItemProductDetailId(),
                    item.getPosOrderItemQuantity()
            );
        }

        // Cập nhật trạng thái
        order.setPosOrderStatus("CANCELLED");
        order.setPosOrderPaymentStatus("CANCELLED");
        order.setPosOrderNotes((order.getPosOrderNotes() != null ? order.getPosOrderNotes() + " | " : "") + "Hủy đơn: " + reason);
        posOrderRepository.save(order);

        // Cập nhật lại doanh số session
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
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        Long totalOrders = posOrderRepository.getTotalOrders(start, end);
        if (totalOrders == null) totalOrders = 0L;

        Long totalCustomers = posOrderRepository.getTotalCustomers(start, end);
        if (totalCustomers == null) totalCustomers = 0L;

        BigDecimal avgOrderValue = (totalOrders != null && totalOrders > 0)
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Lấy tổng giảm giá và thuế từ repository (null-safe)
        BigDecimal totalDiscount = posOrderRepository.getTotalDiscount(start, end);
        if (totalDiscount == null) totalDiscount = BigDecimal.ZERO;

        BigDecimal totalTax = posOrderRepository.getTotalTax(start, end);
        if (totalTax == null) totalTax = BigDecimal.ZERO;

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
        Integer sessionId = order.getPosOrderSessionId();
        if (sessionId == null) return;

        posSessionRepository.findById(sessionId).ifPresent(session -> {
            // Trừ doanh thu đã hủy khỏi session (null-safe)
            BigDecimal sessionCash = session.getPosSessionCashSales() != null ? session.getPosSessionCashSales() : BigDecimal.ZERO;
            BigDecimal orderAmount = order.getPosOrderTotalAmount() != null ? order.getPosOrderTotalAmount() : BigDecimal.ZERO;
            BigDecimal newCashSales = sessionCash.subtract(orderAmount);
            if (newCashSales.compareTo(BigDecimal.ZERO) < 0) {
                newCashSales = BigDecimal.ZERO;
            }
            session.setPosSessionCashSales(newCashSales);
            posSessionRepository.save(session);
        });
    }

    private PosInvoiceDTO convertToDTO(PosOrder order) {
        PosInvoiceDTO dto = new PosInvoiceDTO();
        dto.setPosOrderId(order.getPosOrderId());
        dto.setInvoiceNumber(order.getPosOrderInvoiceNumber());
        dto.setOrderType(order.getPosOrderType());
        dto.setCustomerName(order.getPosOrderCustomerName());
        dto.setCustomerPhone(order.getPosOrderCustomerPhone());
        dto.setSubtotal(order.getPosOrderSubtotal());
        dto.setDiscount(order.getPosOrderDiscountAmount());
        dto.setTax(order.getPosOrderTaxAmount());
        dto.setTotal(order.getPosOrderTotalAmount());
        dto.setPaidAmount(order.getPosOrderPaidAmount());
        dto.setChangeAmount(order.getPosOrderChangeAmount());
        dto.setOrderStatus(order.getPosOrderStatus());
        dto.setPaymentStatus(order.getPosOrderPaymentStatus());
        dto.setCreatedAt(order.getPosOrderCreatedAt());

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

        // Lấy payments
        List<PosPayment> payments = posPaymentRepository.findByPosPaymentOrderId(order.getPosOrderId());
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

        if (payment.getPosPaymentMethodId() == 1) {
            dto.setPaymentMethod("Tiền mặt");
        } else if (payment.getPosPaymentMethodId() == 2) {
            dto.setPaymentMethod("Chuyển khoản");
        } else {
            dto.setPaymentMethod("Khác");
        }

        return dto;
    }
}