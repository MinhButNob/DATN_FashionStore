// quanLyBanhangTaiQuay/service/PosSessionService.java
package com.example.backend_fashionstore.quanLyBanhangTaiQuay.service;

import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.exception.BusinessException;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto.*;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.*;
import com.example.backend_fashionstore.quanlyNhanVien.service.EmployeeService;
import com.example.backend_fashionstore.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosSessionService {

    private final PosSessionRepository posSessionRepository;
    private final PosOrderRepository posOrderRepository;
    private final PosPaymentRepository posPaymentRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final PosCashTransactionRepository posCashTransactionRepository;
    private final PosOrderItemRepository posOrderItemRepository;
    private final EmployeeService employeeService;

    private Integer getCurrentEmployeeId() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return employeeService.getEmployeeIdByUserId(userId);
    }

    // =====================================================
    // MỞ CA BÁN HÀNG
    // =====================================================
    @Transactional
    public PosSessionResponse openSession(PosSessionRequest request) {
        Integer employeeId = getCurrentEmployeeId();

        // Kiểm tra quầy có tồn tại không
        CashRegister cashRegister = cashRegisterRepository.findById(request.getCashRegisterId())
                .orElseThrow(() -> new BusinessException("Quầy thu ngân không tồn tại"));

        // Kiểm tra quầy có đang bị vô hiệu hóa hay không
        if ("DELETED".equals(cashRegister.getCashRegisterStatus())) {
            throw new BusinessException("Quầy thu ngân đã bị xóa hoặc không hợp lệ");
        }

        // Kiểm tra nhân viên đã có ca active chưa
        if (posSessionRepository.findActiveSessionByEmployee(employeeId).isPresent()) {
            throw new BusinessException("Bạn đã có ca bán hàng đang mở");
        }

        // Kiểm tra quầy đã có ca active chưa
        if (posSessionRepository.findByPosSessionCashRegisterIdAndPosSessionStatus(
                request.getCashRegisterId(), "ACTIVE").isPresent()) {
            throw new BusinessException("Quầy thu ngân đang có ca bán hàng mở");
        }

        // Cập nhật quầy
        cashRegister.setCashRegisterStatus("ACTIVE");
        cashRegister.setCashRegisterOpenedAt(LocalDateTime.now());
        cashRegister.setCashRegisterOpeningBalance(request.getOpeningBalance());
        cashRegister.setCashRegisterOpenedBy(employeeId);
        cashRegisterRepository.save(cashRegister);

        // Tạo session mới
        PosSession session = PosSession.builder()
                .posSessionCashRegisterId(request.getCashRegisterId())
                .posSessionEmployeeId(employeeId)
                .posSessionStartTime(LocalDateTime.now())
                .posSessionStatus("ACTIVE")
                .posSessionStartBalance(request.getOpeningBalance())
                .posSessionCashSales(BigDecimal.ZERO)
                .posSessionCardSales(BigDecimal.ZERO)
                .posSessionTransferSales(BigDecimal.ZERO)
                .build();

        PosSession savedSession = posSessionRepository.save(session);
        return convertToResponse(savedSession);
    }

    // =====================================================
    // ĐÓNG CA BÁN HÀNG
    // =====================================================
    @Transactional
    public PosSessionResponse closeSession(Integer sessionId, Integer employeeId) {
        PosSession session = posSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Phiên bán hàng không tồn tại"));

        if (!"ACTIVE".equals(session.getPosSessionStatus())) {
            throw new BusinessException("Phiên bán hàng đã được đóng");
        }

        // Tính tổng doanh thu - xử lý null
        BigDecimal totalSales = posOrderRepository.getTotalSalesBySession(sessionId);
        if (totalSales == null) totalSales = BigDecimal.ZERO;

        BigDecimal totalCashFromPayments = posPaymentRepository.getTotalPaymentBySession(sessionId);
        if (totalCashFromPayments == null) totalCashFromPayments = BigDecimal.ZERO;

        // Tính toán số dư cuối ca
        BigDecimal netCashMovement = posCashTransactionRepository.getNetCashMovement(sessionId);
        if (netCashMovement == null) netCashMovement = BigDecimal.ZERO;

        BigDecimal startBalance = session.getPosSessionStartBalance();
        if (startBalance == null) startBalance = BigDecimal.ZERO;

        BigDecimal expectedBalance = startBalance
                .add(totalCashFromPayments)
                .add(netCashMovement);

        // Đóng session
        session.setPosSessionEndTime(LocalDateTime.now());
        session.setPosSessionStatus("CLOSED");
        session.setPosSessionEndBalance(expectedBalance);
        session.setPosSessionTotalSales(totalSales);

        PosSession savedSession = posSessionRepository.save(session);
        return convertToResponse(savedSession);
    }

    // =====================================================
    // GIAO DỊCH TIỀN MẶT (NẠP / RÚT)
    // =====================================================
    @Transactional
    public PosCashTransactionResponse cashTransaction(PosCashTransactionRequest request) {
        return processCashTransaction(request);
    }

    // =====================================================
    // RÚT TIỀN MẶT TRONG CA
    // =====================================================
    @Transactional
    public PosCashTransactionResponse withdrawCash(PosCashTransactionRequest request) {
        request.setType("WITHDRAW");
        return processCashTransaction(request);
    }

    // =====================================================
    // NẠP TIỀN MẶT TRONG CA
    // =====================================================
    @Transactional
    public PosCashTransactionResponse depositCash(PosCashTransactionRequest request) {
        request.setType("DEPOSIT");
        return processCashTransaction(request);
    }

    // =====================================================
    // LẤY CA ĐANG HOẠT ĐỘNG CỦA TÔI
    // =====================================================
    public PosSessionResponse getMyActiveSession() {
        Integer employeeId = getCurrentEmployeeId();
        return posSessionRepository.findActiveSessionByEmployee(employeeId)
                .map(this::convertToResponse)
                .orElse(null);
    }

    // =====================================================
    // LẤY CA ĐANG HOẠT ĐỘNG THEO NHÂN VIÊN
    // =====================================================
    public PosSessionResponse getActiveSessionByEmployee(Integer employeeId) {
        return posSessionRepository.findActiveSessionByEmployee(employeeId)
                .map(this::convertToResponse)
                .orElse(null);
    }

    // =====================================================
    // LẤY CA ĐANG HOẠT ĐỘNG THEO QUẦY
    // =====================================================
    public PosSessionResponse getActiveSessionByCashRegister(Integer cashRegisterId) {
        return posSessionRepository.findByPosSessionCashRegisterIdAndPosSessionStatus(cashRegisterId, "ACTIVE")
                .map(this::convertToResponse)
                .orElse(null);
    }

    // =====================================================
    // LẤY DANH SÁCH CA THEO NHÂN VIÊN
    // =====================================================
    public List<PosSessionResponse> getSessionsByEmployee(Integer employeeId) {
        return posSessionRepository.findByPosSessionEmployeeIdAndPosSessionStatus(employeeId, "CLOSED")
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // LẤY TẤT CẢ CA ĐANG HOẠT ĐỘNG
    // =====================================================
    public List<PosSessionResponse> getAllActiveSessions() {
        return posSessionRepository.findAllActiveSessions()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // LẤY CHI TIẾT CA BÁN HÀNG
    // =====================================================
    public PosSessionDetailResponse getSessionDetail(Integer sessionId) {
        PosSession session = posSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Phiên bán hàng không tồn tại"));

        List<PosOrder> orders = posOrderRepository.findByPosOrderSessionId(sessionId);
        List<PosCashTransaction> cashTransactions = posCashTransactionRepository.findByCashTransactionSessionId(sessionId);

        PosSessionDetailResponse response = new PosSessionDetailResponse();
        response.setSession(convertToResponse(session));
        response.setOrders(orders.stream().map(order -> {
            PosOrderSimpleResponse simple = new PosOrderSimpleResponse();
            simple.setOrderId(order.getPosOrderId());
            simple.setInvoiceNumber(order.getPosOrderInvoiceNumber());
            simple.setTotalAmount(order.getPosOrderTotalAmount());
            simple.setCreatedAt(order.getPosOrderCreatedAt());
            return simple;
        }).collect(Collectors.toList()));

        response.setCashTransactions(cashTransactions.stream().map(transaction -> {
            PosCashTransactionResponse transResp = new PosCashTransactionResponse();
            transResp.setCashTransactionId(transaction.getCashTransactionId());
            transResp.setType(transaction.getCashTransactionType());
            transResp.setAmount(transaction.getCashTransactionAmount());
            transResp.setReason(transaction.getCashTransactionReason());
            transResp.setBeforeBalance(transaction.getCashTransactionBeforeBalance());
            transResp.setAfterBalance(transaction.getCashTransactionAfterBalance());
            transResp.setCreatedAt(transaction.getCashTransactionCreatedAt());
            return transResp;
        }).collect(Collectors.toList()));

        return response;
    }

    // =====================================================
    // PRIVATE METHODS
    // =====================================================

    private PosCashTransactionResponse processCashTransaction(PosCashTransactionRequest request) {
        PosSession session = posSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new BusinessException("Phiên bán hàng không tồn tại"));

        if (!"ACTIVE".equals(session.getPosSessionStatus())) {
            throw new BusinessException("Phiên bán hàng đã đóng, không thể thực hiện giao dịch");
        }

        String type = request.getType();
        if (type == null || (!type.equals("WITHDRAW") && !type.equals("DEPOSIT"))) {
            throw new BusinessException("Loại giao dịch không hợp lệ. Chỉ chấp nhận WITHDRAW hoặc DEPOSIT");
        }

        // Tính số dư hiện tại
        BigDecimal currentBalance = calculateCurrentBalance(session);

        BigDecimal newBalance;
        if ("WITHDRAW".equals(type)) {
            if (request.getAmount().compareTo(currentBalance) > 0) {
                throw new BusinessException("Số dư không đủ để rút. Hiện tại: " + currentBalance + "đ");
            }
            newBalance = currentBalance.subtract(request.getAmount());
        } else {
            newBalance = currentBalance.add(request.getAmount());
        }

        // Lấy employee ID từ security context
        Integer employeeId = getCurrentEmployeeId();

        PosCashTransaction transaction = PosCashTransaction.builder()
                .cashTransactionSessionId(request.getSessionId())
                .cashTransactionType(type)
                .cashTransactionAmount(request.getAmount())
                .cashTransactionReason(request.getReason())
                .cashTransactionEmployeeId(employeeId)
                .cashTransactionBeforeBalance(currentBalance)
                .cashTransactionAfterBalance(newBalance)
                .build();

        PosCashTransaction savedTransaction = posCashTransactionRepository.save(transaction);

        PosCashTransactionResponse response = new PosCashTransactionResponse();
        response.setCashTransactionId(savedTransaction.getCashTransactionId());
        response.setSessionId(savedTransaction.getCashTransactionSessionId());
        response.setType(savedTransaction.getCashTransactionType());
        response.setAmount(savedTransaction.getCashTransactionAmount());
        response.setReason(savedTransaction.getCashTransactionReason());
        response.setBeforeBalance(savedTransaction.getCashTransactionBeforeBalance());
        response.setAfterBalance(savedTransaction.getCashTransactionAfterBalance());
        response.setCreatedAt(savedTransaction.getCashTransactionCreatedAt());

        return response;
    }

//

    private BigDecimal calculateCurrentBalance(PosSession session) {
        BigDecimal totalSales = posOrderRepository.getTotalSalesBySession(session.getPosSessionId());
        if (totalSales == null) totalSales = BigDecimal.ZERO;

        BigDecimal netCashMovement = posCashTransactionRepository.getNetCashMovement(session.getPosSessionId());
        if (netCashMovement == null) netCashMovement = BigDecimal.ZERO;

        BigDecimal startBalance = session.getPosSessionStartBalance();
        if (startBalance == null) startBalance = BigDecimal.ZERO;

        return startBalance.add(totalSales).add(netCashMovement);
    }

    private PosSessionResponse convertToResponse(PosSession session) {
        PosSessionResponse response = new PosSessionResponse();
        response.setPosSessionId(session.getPosSessionId());
        response.setCashRegisterId(session.getPosSessionCashRegisterId());
        response.setEmployeeId(session.getPosSessionEmployeeId());
        response.setStartTime(session.getPosSessionStartTime());
        response.setEndTime(session.getPosSessionEndTime());
        response.setStatus(session.getPosSessionStatus());
        response.setStartBalance(session.getPosSessionStartBalance());
        response.setEndBalance(session.getPosSessionEndBalance());
        response.setCashSales(session.getPosSessionCashSales());
        response.setCardSales(session.getPosSessionCardSales());
        response.setTransferSales(session.getPosSessionTransferSales());
        response.setTotalSales(session.getPosSessionTotalSales());
        response.setNotes(session.getPosSessionNotes());
        return response;
    }
}