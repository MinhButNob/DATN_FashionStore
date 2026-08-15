package com.example.backend_fashionstore.QuanLyDonHang.service;

import com.example.backend_fashionstore.QuanLyDonHang.dto.request.*;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.ReturnInspectionResponseDTO;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.customer.order.repository.*;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.*;
import com.example.backend_fashionstore.security.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderDetailRepository;

@Service
@RequiredArgsConstructor
public class ReturnInspectionServiceImpl implements ReturnInspectionService {
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final ProductDetailRepository productDetailRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final ObjectMapper objectMapper;
    private final OrderDetailRepository orderDetailRepository;

    @Override
    @Transactional
    public ReturnInspectionResponseDTO inspect(Long returnRequestId, ReturnInspectionRequestDTO request) {
        ReturnRequest rr = returnRequestRepository.findForInspection(returnRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn hàng"));
        Order order = orderRepository.findById(rr.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (order.getOrderStatus() != OrderStatus.RETURN_RECEIVED) {
            throw new IllegalStateException("Chỉ được kiểm tra khi đơn hàng ở trạng thái RETURN_RECEIVED");
        }
        if (!"RECEIVED".equals(rr.getStatus()) && !"RETURN_RECEIVED".equals(rr.getStatus())) {
            throw new IllegalStateException("Yêu cầu hoàn hàng chưa ở trạng thái đã nhận hàng");
        }
        if (rr.getInspectionResult() != null && !rr.getInspectionResult().isBlank()) {
            throw new IllegalStateException("Yêu cầu hoàn hàng đã được kiểm tra trước đó");
        }

        List<Integer> ids = parseIds(rr.getProductDetailIds());
        Map<Integer, Integer> quantities = parseQuantities(rr.getQuantities());
        BigDecimal refundForRequest = BigDecimal.ZERO;
        if (ids.size() != new HashSet<>(ids).size()) {
            throw new IllegalArgumentException("Danh sách sản phẩm hoàn bị trùng");
        }
        Map<Integer, ReturnInspectionItemDTO> submitted = request.getItems().stream().collect(Collectors.toMap(
                ReturnInspectionItemDTO::getProductDetailId, Function.identity(), (a, b) -> {
                    throw new IllegalArgumentException("Không được gửi trùng sản phẩm");
                }));
        if (!submitted.keySet().equals(new HashSet<>(ids))) {
            throw new IllegalArgumentException("Danh sách sản phẩm kiểm tra không khớp yêu cầu hoàn hàng");
        }

        Map<String, ReturnInspectionResponseDTO.ReturnInspectionResultItem> result = new LinkedHashMap<>();
        for (Integer id : ids) {
            ReturnInspectionItemDTO item = submitted.get(id);
            Integer quantity = quantities.get(id);
            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Số lượng hoàn không hợp lệ cho sản phẩm " + id);
            }
            if (item.getAction() == ReturnInspectionAction.DISCARD
                    && (item.getDiscardReason() == null || item.getDiscardReason().isBlank())) {
                throw new IllegalArgumentException("Phải nhập lý do loại bỏ cho sản phẩm " + id);
            }
            ProductDetail pd = productDetailRepository.findByIdWithRelations(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sản phẩm " + id));
            if (item.getAction() == ReturnInspectionAction.RESTOCK) {
                pd.setQuantity((pd.getQuantity() == null ? 0 : pd.getQuantity()) + quantity);
                productDetailRepository.save(pd);
            }
            OrderDetail detail = orderDetailRepository.findByOrder_OrderId(order.getOrderId()).stream()
                    .filter(d -> d.getProductDetail().getProductDetailId().equals(id)).findFirst().orElse(null);
            if (detail != null) {
                BigDecimal unitPaid = detail.getFinalAmount().divide(BigDecimal.valueOf(detail.getQuantity()), 2, java.math.RoundingMode.HALF_UP);
                refundForRequest = refundForRequest.add(unitPaid.multiply(BigDecimal.valueOf(quantity)));
            }
            String image = pd.getProductImages() != null && !pd.getProductImages().isEmpty()
                    ? pd.getProductImages().get(0).getProductImagePath() : null;
            ReturnInspectionResponseDTO.ReturnInspectionResultItem stored =
                    ReturnInspectionResponseDTO.ReturnInspectionResultItem.builder()
                            .productDetailId(id).productName(pd.getProduct() == null ? null : pd.getProduct().getProductName())
                            .colorName(pd.getColor() == null ? null : pd.getColor().getColorName())
                            .sizeName(pd.getSize() == null ? null : pd.getSize().getSizeName())
                            .imageUrl(image).quantity(quantity).action(item.getAction())
                            .discardReason(item.getDiscardReason()).build();
            result.put(String.valueOf(id), stored);
        }

        LocalDateTime now = LocalDateTime.now();
        try {
            rr.setInspectionResult(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể lưu kết quả kiểm tra", e);
        }
        rr.setInspectionNote(request.getInspectionNote());
        rr.setInspectedBy(SecurityUtils.getCurrentUserId());
        rr.setInspectedAt(now);
        rr.setStatus("COMPLETED");
        rr.setUpdatedAt(now);
        returnRequestRepository.save(rr);

        orderDetailRepository.findByOrder_OrderId(order.getOrderId()).forEach(detail -> {
            if (ids.contains(detail.getProductDetail().getProductDetailId())) {
                detail.setStatus(OrderItemStatus.RETURNED);
                orderDetailRepository.save(detail);
            }
        });
        List<OrderDetail> allDetails = orderDetailRepository.findByOrder_OrderId(order.getOrderId());
        boolean allReturned = allDetails.stream().allMatch(d -> d.getStatus() == OrderItemStatus.RETURNED);
        boolean anyReturned = allDetails.stream().anyMatch(d -> d.getStatus() == OrderItemStatus.RETURNED);
        order.setOrderStatus(allReturned ? OrderStatus.RETURNED :
                (anyReturned ? OrderStatus.PARTIALLY_RETURNED : OrderStatus.COMPLETED));
        order.setRefundAmount((order.getRefundAmount() == null ? BigDecimal.ZERO : order.getRefundAmount()).add(refundForRequest));
        order.setUpdatedAt(now);
        orderRepository.save(order);
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus(OrderStatus.RETURNED.name());
        history.setNote("Đã kiểm tra hàng hoàn trả");
        history.setTimestamp(now);
        orderHistoryRepository.save(history);
        return ReturnInspectionResponseDTO.builder().returnRequestId(rr.getReturnRequestId()).orderId(rr.getOrderId())
                .orderStatus(order.getOrderStatus().name()).inspectionResult(result)
                .inspectionNote(rr.getInspectionNote()).inspectedBy(rr.getInspectedBy())
                .inspectedAt(rr.getInspectedAt()).items(new ArrayList<>(result.values())).build();
    }

    private List<Integer> parseIds(String json) {
        try { return objectMapper.readValue(json, new TypeReference<List<Integer>>() {}); }
        catch (Exception e) { throw new IllegalArgumentException("productDetailIds không hợp lệ"); }
    }
    private Map<Integer, Integer> parseQuantities(String json) {
        try { return objectMapper.readValue(json, new TypeReference<Map<Integer, Integer>>() {}); }
        catch (Exception e) { throw new IllegalArgumentException("quantities không hợp lệ"); }
    }
}
