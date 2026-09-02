package com.example.backend_fashionstore.shipping.service;

import com.example.backend_fashionstore.customer.cart.repository.CartRepository;
import com.example.backend_fashionstore.entity.Cart;
import com.example.backend_fashionstore.entity.CartItem;
import com.example.backend_fashionstore.shipping.dto.ShippingCalculateRequest;
import com.example.backend_fashionstore.shipping.dto.ShippingCalculateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {
    private static final int LEGACY_PRODUCT_WEIGHT_GRAM = 100;

    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper;

    @Value("${ghtk.api.url}") private String ghtkApiUrl;
    @Value("${ghtk.token}") private String ghtkToken;
    @Value("${ghtk.pick.province}") private String pickProvince;
    @Value("${ghtk.pick.district}") private String pickDistrict;
    @Value("${ghtk.pick.ward:}") private String pickWard;
    @Value("${ghtk.pick.address:}") private String pickAddress;

    @Override
    public ShippingCalculateResponse calculate(Integer customerId, ShippingCalculateRequest request) {
        validateAddress(request);
        int weight = calculateCartWeight(customerId);
        BigDecimal fee = BigDecimal.ZERO;
        log.info("Free shipping: customerAddress={}, weight={}g, orderValue={}, shippingFee={}",
                request.getProvince() + ", " + request.getDistrict() + ", " + request.getWard(),
                weight, request.getOrderValue(), fee);
        return ShippingCalculateResponse.builder().shippingFee(fee).weight(weight).build();
    }

    public int calculateCartWeight(Integer customerId) {
        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId).orElse(null);
        if (cart == null || cart.getCartItems() == null) return 0;
        return Math.max(1, cart.getCartItems().stream().mapToInt(this::itemWeight).sum());
    }

    private int itemWeight(CartItem item) {
        Double productWeight = item.getProductDetail().getProduct().getWeight();
        int grams = productWeight == null || productWeight <= 0
                ? LEGACY_PRODUCT_WEIGHT_GRAM
                : (int) Math.ceil(productWeight);
        return grams * item.getQuantity();
    }

    public BigDecimal requestGhtk(ShippingCalculateRequest request, int weight) {
        GhtkAddressFormatter.FormattedAddress formatted = GhtkAddressFormatter.format(
                request.getProvince(), request.getDistrict(), request.getWard());
        String ghtkProvince = formatted.province();
        String ghtkDistrict = formatted.district();
        String ghtkWard = formatted.ward();
        log.info("Shipping Address: province={}, district={}, ward={}",
                request.getProvince(), request.getDistrict(), request.getWard());
        log.info("GHTK Address: province={}, district={}, ward={}",
                ghtkProvince, ghtkDistrict, ghtkWard);
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(ghtkApiUrl)
                .path("/services/shipment/fee")
                .queryParam("pick_province", GhtkAddressFormatter.formatProvince(pickProvince))
                .queryParam("pick_district", pickDistrict == null ? "" : pickDistrict.trim())
                .queryParam("pick_ward", GhtkAddressFormatter.formatWard(pickWard))
                .queryParam("address", request.getAddress())
                .queryParam("province", ghtkProvince)
                .queryParam("district", ghtkDistrict)
                .queryParam("ward", ghtkWard)
                .queryParam("weight", weight)
                .queryParam("value", request.getOrderValue().setScale(0, RoundingMode.HALF_UP));

        if (isConfiguredPickupAddress(pickAddress)) {
            urlBuilder.queryParam("pick_address", pickAddress.trim());
        } else {
            log.warn("GHTK pickup address is missing or still a placeholder; omit pick_address from fee request");
        }

        String url = urlBuilder.build().encode().toUriString();
        log.debug("GHTK fee URL (without token): {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghtkToken);
        log.info("Calling GHTK fee API: province={}, district={}, ward={}, weight={}g, value={}",
                request.getProvince(), request.getDistrict(), request.getWard(), weight, request.getOrderValue());
        ResponseEntity<String> response;
        try {
            response = new RestTemplate().exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
        } catch (RestClientException ex) {
            log.error("GHTK fee API request failed: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Không kết nối được GHTK: " + ex.getMessage(), ex);
        }
        JsonNode root;
        try {
            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new IllegalStateException("GHTK trả về phản hồi rỗng");
            }
            root = objectMapper.readTree(response.getBody());
            if (root == null || root.isMissingNode()) {
                throw new IllegalStateException("GHTK trả về JSON không hợp lệ");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Không đọc được phản hồi phí vận chuyển GHTK", e);
        }
        if (!response.getStatusCode().is2xxSuccessful() || !root.path("success").asBoolean(false)) {
            log.error("GHTK fee API rejected request: status={}, body={}", response.getStatusCode(), response.getBody());
            String providerMessage = root.path("message").asText("");
            throw new IllegalStateException("GHTK không hỗ trợ địa chỉ giao hàng hoặc yêu cầu không hợp lệ: "
                    + providerMessage
                    + " [province=" + ghtkProvince
                    + ", district=" + ghtkDistrict
                    + ", ward=" + ghtkWard + "]");
        }
        JsonNode feeNode = root.path("fee").path("fee");
        if (feeNode.isNumber()) return feeNode.decimalValue();
        if (feeNode.isTextual()) {
            try {
                return new BigDecimal(feeNode.asText().replace(",", "").trim());
            } catch (NumberFormatException ignored) {
                // fall through to a useful error below
            }
        }
        throw new IllegalStateException("GHTK không trả về phí vận chuyển: " + feeNode);
    }

    private void validateAddress(ShippingCalculateRequest request) {
        if (request == null || request.getProvince() == null || request.getProvince().isBlank()) {
            throw new IllegalArgumentException("Tỉnh/thành phố giao hàng không hợp lệ");
        }
        if (request.getDistrict() == null || request.getDistrict().isBlank()) {
            throw new IllegalArgumentException("Quận/huyện giao hàng không hợp lệ");
        }
        if (request.getWard() == null || request.getWard().isBlank()) {
            throw new IllegalArgumentException("Phường/xã giao hàng không hợp lệ");
        }
        if (request.getOrderValue() == null) {
            throw new IllegalArgumentException("Giá trị đơn hàng không được để trống");
        }
    }

    private boolean isConfiguredPickupAddress(String value) {
        if (value == null || value.isBlank()) return false;
        String normalized = value.trim().toLowerCase();
        return !normalized.equals("123 duong abc")
                && !normalized.equals("dia chi shop")
                && !normalized.equals("địa chỉ shop");
    }

}
