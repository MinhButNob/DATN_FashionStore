package com.example.backend_fashionstore.ai_chat.service;

import com.example.backend_fashionstore.QuanLySanPham.repository.Product.ProductRepository;
import com.example.backend_fashionstore.ai_chat.dto.ChatRequestDTO;
import com.example.backend_fashionstore.ai_chat.dto.ChatResponseDTO;
import com.example.backend_fashionstore.ai_chat.dto.ProductRecommendationDTO;
import com.example.backend_fashionstore.entity.Chat;
import com.example.backend_fashionstore.entity.Product;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.entity.Promotion;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.PromotionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIChatService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final ProductRecommendationService productRecommendationService;
    private final ChatHistoryService chatHistoryService;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public ChatResponseDTO processChat(ChatRequestDTO request) {
        String userMessage = request.getMessage();
        Integer customerId = request.getUserId().intValue();

        // 1. Get or create chat session
        Chat chat = chatHistoryService.getOrCreateActiveChat(customerId);

        // 2. Save user message
        chatHistoryService.saveUserMessage(chat, userMessage);

        // 3. Trích xuất keyword thô từ câu của user để search sản phẩm liên quan
        List<String> candidateKeywords = extractCandidateKeywords(userMessage);

        // 4. Query DB lấy sản phẩm liên quan (dùng toàn bộ keyword candidate)
        List<Product> relatedProducts = new ArrayList<>();
        for (String kw : candidateKeywords) {
            List<Product> found = productRepository.searchActiveProductsByKeyword(kw);
            for (Product p : found) {
                if (relatedProducts.stream().noneMatch(x -> x.getProductId().equals(p.getProductId()))) {
                    relatedProducts.add(p);
                }
            }
            if (relatedProducts.size() >= 10) break; // Giới hạn tối đa 10 sản phẩm đưa vào context
        }

        // Nếu không tìm thấy sản phẩm liên quan, lấy 5 sản phẩm bất kỳ để AI biết cửa hàng có gì
        if (relatedProducts.isEmpty()) {
            relatedProducts = productRepository.findAll().stream()
                    .filter(p -> "ACTIVE".equals(p.getStatus()) && p.getDeletedAt() == null)
                    .limit(5)
                    .collect(Collectors.toList());
        }

        // 5. Query DB lấy khuyến mãi đang diễn ra
        List<Promotion> activePromotions = promotionRepository.findCurrentlyActivePromotions(LocalDateTime.now());

        // 6. Build System Prompt tích hợp dữ liệu DB
        String systemPrompt = buildSystemPrompt(relatedProducts, activePromotions);

        // 7. Build Full Prompt
        String fullPrompt = systemPrompt + "\n\nKhách hàng: " + userMessage + "\nHaru:";

        // 8. Call Gemini API
        String aiResponseText = callGeminiApi(fullPrompt);

        // 9. Parse Intent từ response Gemini
        String intent = "GENERAL";
        String searchKeyword = null;
        String rawResponse = aiResponseText;

        Pattern intentPattern = Pattern.compile("\\[INTENT:([A-Z_]+)(?::([^\\]]+))?\\]");
        Matcher matcher = intentPattern.matcher(aiResponseText);

        if (matcher.find()) {
            intent = matcher.group(1);
            if (matcher.groupCount() > 1 && matcher.group(2) != null) {
                searchKeyword = matcher.group(2).trim();
            }
            // Xóa intent tag khỏi câu trả lời hiển thị
            rawResponse = aiResponseText.replace(matcher.group(0), "").trim();
        }

        // 10. Lấy ProductRecommendationDTO để trả về Frontend (dùng keyword đã parse từ intent, hoặc keyword thô)
        List<ProductRecommendationDTO> products = new ArrayList<>();
        String kwForRecommendation = (searchKeyword != null) ? searchKeyword
                : (candidateKeywords.isEmpty() ? null : candidateKeywords.get(0));

        if ("SEARCH_PRODUCT".equals(intent) && kwForRecommendation != null) {
            products = productRecommendationService.recommendProducts(kwForRecommendation);
        }

        // 11. Save AI message
        chatHistoryService.saveAIMessage(chat, rawResponse);

        return ChatResponseDTO.builder()
                .response(rawResponse)
                .intent(intent)
                .products(products)
                .build();
    }

    /**
     * Xây dựng System Prompt với dữ liệu thật từ database.
     */
    private String buildSystemPrompt(List<Product> relatedProducts, List<Promotion> activePromotions) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Bạn là Haru, nhân viên tư vấn thời trang thân thiện của Fashion Store. ")
              .append("Chỉ tư vấn dựa trên DỮ LIỆU SẢN PHẨM THỰC TẾ được cung cấp dưới đây. ")
              .append("KHÔNG được tự bịa thêm tên sản phẩm, giá, màu sắc hay size không có trong danh sách. ")
              .append("Nếu khách hỏi sản phẩm không có trong danh sách, nói thật là cửa hàng hiện chưa có. ")
              .append("Trả lời thân thiện, ngắn gọn, dùng emoji phù hợp. ")
              .append("Ở CUỐI câu trả lời, LUÔN thêm một trong các tag intent sau:\n")
              .append("  - [INTENT:SEARCH_PRODUCT:từ khóa] nếu khách muốn tìm sản phẩm\n")
              .append("  - [INTENT:PROMOTION] nếu khách hỏi về khuyến mãi\n")
              .append("  - [INTENT:ORDER] nếu khách muốn đặt hàng\n")
              .append("  - [INTENT:SUPPORT] nếu khách cần hỗ trợ\n")
              .append("  - [INTENT:GREETING] nếu khách chào hỏi\n")
              .append("  - [INTENT:GENERAL] cho các trường hợp khác\n\n");

        // ---- DANH SÁCH SẢN PHẨM THỰC TẾ ----
        prompt.append("===== SẢN PHẨM CỬA HÀNG (DỮ LIỆU THỰC TỪ DATABASE) =====\n");
        if (relatedProducts.isEmpty()) {
            prompt.append("(Hiện không có sản phẩm nào khớp yêu cầu)\n");
        } else {
            for (Product p : relatedProducts) {
                prompt.append("- Tên: ").append(p.getProductName());
                if (p.getCategory() != null) {
                    prompt.append(" | Danh mục: ").append(p.getCategory().getCategoryName());
                }
                // Lấy thông tin từ các ProductDetail
                if (p.getProductDetails() != null && !p.getProductDetails().isEmpty()) {
                    List<ProductDetail> activeDetails = p.getProductDetails().stream()
                            .filter(d -> "ACTIVE".equals(d.getStatus()) && d.getDeletedAt() == null)
                            .collect(Collectors.toList());

                    if (!activeDetails.isEmpty()) {
                        // Giá min - max
                        BigDecimal minPrice = activeDetails.stream()
                                .map(ProductDetail::getPrice)
                                .filter(Objects::nonNull)
                                .min(BigDecimal::compareTo).orElse(null);
                        BigDecimal maxPrice = activeDetails.stream()
                                .map(ProductDetail::getPrice)
                                .filter(Objects::nonNull)
                                .max(BigDecimal::compareTo).orElse(null);

                        if (minPrice != null) {
                            if (minPrice.compareTo(maxPrice) == 0) {
                                prompt.append(" | Giá: ").append(formatPrice(minPrice)).append("đ");
                            } else {
                                prompt.append(" | Giá: ").append(formatPrice(minPrice))
                                      .append(" - ").append(formatPrice(maxPrice)).append("đ");
                            }
                        }

                        // Màu sắc có sẵn
                        String colors = activeDetails.stream()
                                .filter(d -> d.getColor() != null)
                                .map(d -> d.getColor().getColorName())
                                .distinct()
                                .collect(Collectors.joining(", "));
                        if (!colors.isEmpty()) {
                            prompt.append(" | Màu: ").append(colors);
                        }

                        // Size có sẵn
                        String sizes = activeDetails.stream()
                                .filter(d -> d.getSize() != null)
                                .map(d -> d.getSize().getSizeName())
                                .distinct()
                                .collect(Collectors.joining(", "));
                        if (!sizes.isEmpty()) {
                            prompt.append(" | Size: ").append(sizes);
                        }

                        // Tổng tồn kho
                        int totalQty = activeDetails.stream()
                                .mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0)
                                .sum();
                        prompt.append(" | Tồn kho: ").append(totalQty);
                    }
                }
                prompt.append("\n");
            }
        }

        // ---- KHUYẾN MÃI ĐANG DIỄN RA ----
        prompt.append("\n===== KHUYẾN MÃI ĐANG DIỄN RA =====\n");
        if (activePromotions.isEmpty()) {
            prompt.append("(Hiện không có chương trình khuyến mãi nào)\n");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Promotion promo : activePromotions) {
                prompt.append("- ").append(promo.getPromotionName());
                if (promo.getPromotionDiscountPercent() != null &&
                        promo.getPromotionDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    prompt.append(" | Giảm: ").append(promo.getPromotionDiscountPercent()).append("%");
                }
                if (promo.getPromotionDescription() != null && !promo.getPromotionDescription().isBlank()) {
                    prompt.append(" | Mô tả: ").append(promo.getPromotionDescription());
                }
                if (promo.getPromotionEndDate() != null) {
                    prompt.append(" | Đến hết: ").append(promo.getPromotionEndDate().format(fmt));
                }
                prompt.append("\n");
            }
        }
        prompt.append("=============================================\n");

        return prompt.toString();
    }

    /**
     * Trích xuất các từ khóa tiềm năng từ câu hỏi của user.
     * Ví dụ: "Bạn có áo thun trắng size M không?" → ["áo thun trắng", "áo thun", "áo"]
     */
    private List<String> extractCandidateKeywords(String message) {
        List<String> keywords = new ArrayList<>();
        if (message == null || message.trim().isEmpty()) return keywords;

        // Lấy toàn bộ câu (không quá 50 ký tự) để search rộng trước
        String cleaned = message.trim().toLowerCase();

        // Loại bỏ các từ hỏi không liên quan đến sản phẩm
        String[] stopWords = {"bạn", "có", "không", "nào", "và", "hoặc", "hay", "thì", "là", "tôi",
                "mình", "cho", "với", "ở", "đây", "kia", "muốn", "cần", "tìm", "xem",
                "ơi", "ạ", "nhé", "nha", "thôi", "vậy", "thế", "ừ", "ok", "được"};

        String[] words = cleaned.split("\\s+");
        List<String> meaningful = new ArrayList<>();
        for (String word : words) {
            boolean isStop = false;
            for (String stop : stopWords) {
                if (word.equals(stop)) { isStop = true; break; }
            }
            if (!isStop && word.length() > 1) {
                meaningful.add(word);
            }
        }

        // Tạo phrase ngắn từ các từ có nghĩa (2-3 từ liền nhau)
        for (int len = Math.min(3, meaningful.size()); len >= 1; len--) {
            for (int i = 0; i <= meaningful.size() - len; i++) {
                String phrase = String.join(" ", meaningful.subList(i, i + len));
                if (!keywords.contains(phrase)) {
                    keywords.add(phrase);
                }
            }
        }

        return keywords.stream().limit(8).collect(Collectors.toList());
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "N/A";
        // Format: 199000 -> 199.000
        return String.format("%,.0f", price.doubleValue()).replace(",", ".");
    }

    private String callGeminiApi(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> parts = new HashMap<>();
            parts.put("parts", Collections.singletonList(textPart));

            Map<String, Object> body = new HashMap<>();
            body.put("contents", Collections.singletonList(parts));

            // Cấu hình safety settings để không bị chặn do content (tiếng Việt hay bị)
            List<Map<String, Object>> safetySettings = new ArrayList<>();
            for (String category : List.of("HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH",
                    "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT")) {
                Map<String, Object> setting = new HashMap<>();
                setting.put("category", category);
                setting.put("threshold", "BLOCK_ONLY_HIGH");
                safetySettings.add(setting);
            }
            body.put("safetySettings", safetySettings);

            // Giới hạn output token để response nhanh hơn
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("maxOutputTokens", 800);
            generationConfig.put("temperature", 0.7);
            body.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String url = GEMINI_URL + geminiApiKey;

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(responseEntity.getBody(), Map.class);
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    if (content != null) {
                        List<Map<String, Object>> partsRes = (List<Map<String, Object>>) content.get("parts");
                        if (partsRes != null && !partsRes.isEmpty()) {
                            return (String) partsRes.get(0).get("text");
                        }
                    }
                }
            }
            return "Xin lỗi, hiện tại Haru đang gặp chút trục trặc. Bạn thử lại sau nhé! 😅";
        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, Haru không thể kết nối được với máy chủ AI lúc này 😢";
        }
    }
}
